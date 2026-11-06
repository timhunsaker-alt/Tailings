package dev.wildcatter.tailings.fabric.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.RectifierBlockEntity;
import dev.wildcatter.tailings.entity.GantryEntity;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.mine.TbmGantry;
import dev.wildcatter.tailings.plant.Machine;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The mine played from nothing, in a real generated survival world: every part crafted through its real recipe and placed through
 * the real placement code with a survival player's hand, every motor wired to a generator that really runs, nothing force-run. The
 * machine bores real rock; what it finds goes by train to the dump, through the crushing circuit and the flotation bank to ingots.
 *
 * What is still staged, and logged as such: digging the site's pit, the gas and diesel in the generators, the water, and the raw
 * materials (the bill they add up to is written to playthrough-bill.txt).
 */
final class Playthrough {
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_SEED", "tailings");
	/** Vanilla items are gathered or crafted the vanilla way, which vanilla tests. */
	private static final String VANILLA = "minecraft";
	private static final int DROP = 25, FLAT = 20;

	private final ClientGameTestContext context;
	private final Map<String, JsonObject> recipes = new HashMap<>();
	private final Map<String, Boolean> proved = new HashMap<>();
	private final Map<String, Integer> raw = new TreeMap<>();
	private final Map<String, Integer> crafted = new TreeMap<>();
	private final List<String> notCraftable = new ArrayList<>();

	private BlockPos c;
	private BlockPos o;
	private int gy;
	private int ytop;
	private BlockPos beam;
	private final Map<String, BlockPos> parts = new java.util.LinkedHashMap<>();
	private final List<BlockPos> panels = new ArrayList<>();
	private final MineVehicleEntity[] train = new MineVehicleEntity[3];
	private String oreId = "minecraft:copper_ore";
	private dev.wildcatter.energy.ConductorSize size;

	Playthrough(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[tailings-play] " + line);
	}

	private static void require(boolean ok, String message) {
		if (!ok) {
			throw new AssertionError(message);
		}
	}

	void run() {
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(SEED);
			s.setGameMode(WorldCreationUiState.SelectedGameMode.SURVIVAL);
		}).create()) {
			TestServerContext server = world.getServer();
			context.waitTicks(40);
			server.runCommand("time set noon");
			server.runCommand("gamerule advance_time false");
			// off the ground before the pit opens under spawn: a player left standing there falls in and dies, behind a locked death screen
			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p 34 120 40");
			context.waitTicks(10);
			server.runOnServer(srv -> require(srv.getPlayerList().getPlayers().get(0).isAlive(), "the player should be alive"));
			server.runOnServer(this::site);
			server.runOnServer(this::readRecipes);
			context.waitTicks(100);
			server.runOnServer(this::dig);
			Filming film = context.computeOnClient(client -> Filming.start(client, "playthrough"));
			server.runCommand("gamemode spectator @p");
			camera(server, 34, 22, 40, 90, 25);
			server.runCommand("tp @p " + (c.getX() + 8) + " " + gy + " " + (c.getZ() + 48));
			server.runCommand("gamemode survival @p");
			// the plant runs while its chunks are loaded: here a forced area stands in for the player staying near, or a chunk loader
			server.runCommand("forceload add " + (c.getX() - 24) + " " + (c.getZ() - 8) + " " + (c.getX() + 46) + " " + (c.getZ() + 92));
			for (int wait = 0; wait < 120 && !server.computeOnServer(srv -> srv.overworld().isPositionEntityTicking(c.offset(2, 1, 12)) && srv.overworld().isPositionEntityTicking(c.offset(30, 1, 85))); wait++) {
				context.waitTicks(10);
			}
			require(server.computeOnServer(srv -> srv.overworld().isPositionEntityTicking(c.offset(2, 1, 12))), "the site's chunks should load");
			server.runOnServer(srv -> log("simulation distance " + srv.getPlayerList().getSimulationDistance() + ", view " + srv.getPlayerList().getViewDistance()));
			build(server);
			server.runCommand("gamemode spectator @p");
			camera(server, 34, 22, 40, 90, 25);
			context.takeScreenshot("tailings-play-built");
			power(server);
			play(server);
			film.stop();
			writeBill();
		}
	}

	private void camera(TestServerContext server, int x, int up, int z, int yaw, int pitch) {
		server.runCommand("tp @p " + (c.getX() + x) + " " + (c.getY() + up) + " " + (c.getZ() + z) + " " + yaw + " " + pitch);
		context.waitTicks(20);
	}

	// ---- the site ------------------------------------------------------------------------------------------------------------

	/** Finds rock at spawn, digs the pit and starts the machine under ground level. */
	private void site(MinecraftServer srv) {
		ServerLevel level = srv.overworld();
		BlockPos spawn = BlockPos.ZERO;   // the seed's own ground at the world's centre: the same site every run
		int top = Integer.MAX_VALUE;
		int water = 0;
		for (int x = -40; x <= 40; x += 8) {
			for (int z = -70; z <= 100; z += 8) {
				int h = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, spawn.getX() + x, spawn.getZ() + z);
				level.getChunk(spawn.getX() + x >> 4, spawn.getZ() + z >> 4);
				h = level.getHeight(Heightmap.Types.OCEAN_FLOOR, spawn.getX() + x, spawn.getZ() + z);
				top = Math.min(top, h);
				if (level.getFluidState(new BlockPos(spawn.getX() + x, h + 1, spawn.getZ() + z)).is(net.minecraft.tags.FluidTags.WATER)) {
					water++;
				}
			}
		}
		c = new BlockPos(spawn.getX(), top - 7, spawn.getZ());
		beam = c.offset(3, 3, 3);
		o = c.offset(13, -2, 50);
		gy = c.getY() - 3;
		ytop = o.getY() + 11;
		log("seed " + SEED + ", spawn " + spawn.toShortString() + ", lowest ground " + top + ", " + water + " sample points in water, machine at " + c.toShortString());
		dig(srv);
		// the ore the bore will meet decides what the sorter's ore side is set to
		Map<String, Integer> ores = new TreeMap<>();
		for (int ahead = 1; ahead <= DROP * 2 + FLAT; ahead++) {
			int floor = c.getY() + 1 - Math.min(DROP, ahead / 2);
			for (int x = 0; x < 8; x++) {
				for (int yy = -1; yy < 8; yy++) {
					BlockPos at = new BlockPos(c.getX() + x, floor + yy, c.getZ() - ahead);
					String id = BuiltInRegistries.BLOCK.getKey(level.getBlockState(at).getBlock()).toString();
					if (id.endsWith("_ore")) {
						ores.merge(id, 1, Integer::sum);
					}
				}
			}
		}
		log("ore in the bore's path: " + ores);
		for (String ore : List.of("minecraft:copper_ore", "minecraft:deepslate_copper_ore", "minecraft:iron_ore", "minecraft:deepslate_iron_ore", "minecraft:gold_ore")) {
			if (ores.getOrDefault(ore, 0) > ores.getOrDefault(oreId, 0)) {
				oreId = ore;
			}
		}
		require(ores.getOrDefault(oreId, 0) > 0, "no ladder ore in the bore's path on this seed: " + ores);
		log("the sorter's ore side: " + oreId);
	}

	/** The pit, dug or dug out again after whatever sand fell into it. Digging it by hand is left out. */
	private void dig(MinecraftServer srv) {
		ServerLevel level = srv.overworld();
		BlockState air = Blocks.AIR.defaultBlockState();
		for (int x = -24; x <= 46; x++) {
			for (int z = 0; z <= 92; z++) {
				level.setBlock(new BlockPos(c.getX() + x, gy - 1, c.getZ() + z), Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
				for (int yy = gy; yy < level.getHeight(); yy++) {
					BlockPos at = new BlockPos(c.getX() + x, yy, c.getZ() + z);
					if (!level.getBlockState(at).isAir()) {
						level.setBlock(at, air, Block.UPDATE_CLIENTS);
					}
				}
			}
		}
	}

	// ---- crafting ------------------------------------------------------------------------------------------------------------

	private void readRecipes(MinecraftServer srv) {
		srv.getResourceManager().listResources("recipe", id -> id.getPath().endsWith(".json")).forEach((id, resource) -> {
			if (id.getNamespace().equals(VANILLA)) {
				return;
			}
			try (Reader reader = new InputStreamReader(resource.open())) {
				JsonObject recipe = JsonParser.parseReader(reader).getAsJsonObject();
				String type = recipe.has("type") ? recipe.get("type").getAsString() : "";
				if (recipe.has("result") && (type.endsWith("crafting_shaped") || type.endsWith("crafting_shapeless"))) {
					JsonElement result = recipe.get("result");
					String out = result.isJsonObject() ? result.getAsJsonObject().get("id").getAsString() : result.getAsString();
					// a conductor's recipes that join two of itself into the next size are no way to make the first one
					String needs = (recipe.has("key") ? recipe.get("key") : recipe.get("ingredients")).toString();
					if (!needs.contains("\"" + out + "\"")) {
						recipes.merge(out, recipe, (old, now) -> id.getPath().contains(out.substring(out.indexOf(':') + 1)) ? now : old);
					}
				}
			} catch (IOException | RuntimeException e) {
				log("could not read recipe " + id + ": " + e);
			}
		});
		log("read " + recipes.size() + " mod crafting recipes");
	}

	private static Item item(String id) {
		return BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
	}

	/** One item that an ingredient of a recipe's JSON takes. */
	private static ItemStack ingredient(JsonElement element) {
		if (element.isJsonArray()) {
			JsonArray any = element.getAsJsonArray();
			return ingredient(any.get(0));
		}
		if (element.isJsonObject()) {
			JsonObject object = element.getAsJsonObject();
			if (object.has("components") && object.has("base")) {   // a sized wire
				ItemStack sized = ingredient(object.get("base")).copy();
				object.getAsJsonObject("components").asMap().forEach((name, value) -> setComponent(sized, name, value));
				return sized;
			}
			for (String field : List.of("item", "items", "tag", "ingredient", "base")) {
				if (object.has(field)) {
					JsonElement inner = object.get(field);
					return ingredient(field.equals("tag") ? new com.google.gson.JsonPrimitive("#" + inner.getAsString()) : inner);
				}
			}
			throw new IllegalArgumentException("an ingredient this scene can't read: " + object);
		}
		String id = element.getAsString();
		if (id.startsWith("#")) {
			TagKey<Item> tag = TagKey.create(Registries.ITEM, Identifier.parse(id.replace("#", "")));
			for (var holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
				return new ItemStack(holder.value());
			}
			return ItemStack.EMPTY;
		}
		return new ItemStack(item(id));
	}

	@SuppressWarnings("unchecked")
	private static <T> void setComponent(ItemStack stack, String name, JsonElement value) {
		var type = (net.minecraft.core.component.DataComponentType<T>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.parse(name));
		stack.set(type, type.codecOrThrow().parse(com.mojang.serialization.JsonOps.INSTANCE, value).getOrThrow());
	}

	/** Puts {@code count} of {@code id} on the bill, crafting what the mods make through the game's own recipe matching. */
	private void need(MinecraftServer srv, String id, int count, int depth) {
		if (id.startsWith(VANILLA + ":") || depth > 12) {
			raw.merge(id, count, Integer::sum);
			return;
		}
		JsonObject recipe = recipes.get(id);
		if (recipe == null) {
			raw.merge(id, count, Integer::sum);
			if (!notCraftable.contains(id)) {
				notCraftable.add(id);
			}
			return;
		}
		crafted.merge(id, count, Integer::sum);
		List<ItemStack> grid = new ArrayList<>();
		int width = 3, height = 3;
		if (recipe.has("pattern")) {
			JsonArray rows = recipe.getAsJsonArray("pattern");
			height = rows.size();
			width = rows.get(0).getAsString().length();
			JsonObject key = recipe.getAsJsonObject("key");
			for (JsonElement row : rows) {
				for (char ch : row.getAsString().toCharArray()) {
					grid.add(ch == ' ' ? ItemStack.EMPTY : ingredient(key.get(String.valueOf(ch))));
				}
			}
		} else {
			JsonArray list = recipe.getAsJsonArray("ingredients");
			for (JsonElement one : list) {
				grid.add(ingredient(one));
			}
			while (grid.size() < 9) {
				grid.add(ItemStack.EMPTY);
			}
		}
		JsonElement result = recipe.get("result");
		int makes = result.isJsonObject() && result.getAsJsonObject().has("count") ? result.getAsJsonObject().get("count").getAsInt() : 1;
		if (proved.putIfAbsent(id, true) == null) {
			CraftingInput input = CraftingInput.of(width, height, grid);
			Optional<?> match = srv.getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, srv.overworld());
			require(match.isPresent(), "the game's crafting table should take the recipe for " + id + " (" + grid + ")");
		}
		int times = (count + makes - 1) / makes;
		Map<String, Integer> inputs = new HashMap<>();
		for (ItemStack stack : grid) {
			if (!stack.isEmpty()) {
				inputs.merge(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), 1, Integer::sum);
			}
		}
		inputs.forEach((in, n) -> need(srv, in, n * times, depth + 1));
	}

	private void writeBill() {
		StringBuilder out = new StringBuilder("The mine's bill of materials, from the playthrough\n\nRaw materials:\n");
		raw.forEach((id, n) -> out.append(String.format("%8d  %s%n", n, id)));
		out.append("\nCrafted:\n");
		crafted.forEach((id, n) -> out.append(String.format("%8d  %s%n", n, id)));
		out.append("\nMod items on the bill with no crafting recipe:\n");
		notCraftable.forEach(id -> out.append("  ").append(id).append('\n'));
		try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(Path.of("playthrough-bill.txt")))) {
			writer.print(out);
		} catch (IOException e) {
			throw new IllegalStateException(e);
		}
		log("no crafting recipe: " + notCraftable);
		log(out.toString().replace('\n', '|'));
	}

	// ---- placing with a survival hand -----------------------------------------------------------------------------------------

	private static final int SOUTH = 0, WEST = 90, NORTH = 180, EAST = -90;

	/** Crafts one of {@code id} and places it at {@code at} through the game's own item use, the player looking along {@code yaw}. */
	private void place(MinecraftServer srv, String id, BlockPos at, int yaw) {
		need(srv, id, 1, 0);
		ServerLevel level = srv.overworld();
		ServerPlayer player = srv.getPlayerList().getPlayers().get(0);
		player.setYRot(yaw);
		player.setYHeadRot(yaw);
		player.setXRot(0);
		ItemStack stack = new ItemStack(item(id));
		if (size != null) {
			stack.set(dev.wildcatter.WildcatterComponents.CONDUCTOR_SIZE, size);
		}
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(at), Direction.UP, at, false);
		var result = player.gameMode.useItemOn(player, level, stack, InteractionHand.MAIN_HAND, hit);
		require(result.consumesAction(), "placing " + id + " at " + at.toShortString() + " should work, got " + result + "; there: " + level.getBlockState(at));
		require(stack.isEmpty(), "placing " + id + " should take it from a survival hand");
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
	}

	/** A part at its main cell, facing north. */
	private void part(MinecraftServer srv, String name, Machine machine, BlockPos at) {
		String id = "tailings:" + machine.id();
		place(srv, id, at, SOUTH);
		ServerLevel level = srv.overworld();
		require(level.getBlockEntity(at) instanceof PlantBlockEntity, machine.id() + " should have its main cell at " + at.toShortString() + ", there: " + level.getBlockState(at));
		parts.put(name, at);
	}

	private void bigPart(MinecraftServer srv, Block block, BlockPos at) {
		String id = BuiltInRegistries.BLOCK.getKey(block).toString();
		place(srv, id, at, SOUTH);
		require(srv.overworld().getBlockState(at).getBlock() == block, id + " should be at " + at.toShortString() + ", there: " + srv.overworld().getBlockState(at));
	}

	/** A straight run of belt along a list of cells: {@code axis} X or Z. */
	private void belts(MinecraftServer srv, Direction.Axis axis, int[]... cells) {
		for (int[] at : cells) {
			place(srv, "tailings:belt_conveyor", o.offset(at[0], at[1], at[2]), axis == Direction.Axis.X ? EAST : SOUTH);
		}
	}

	/** Cable or pipe along straight legs between {@code points}, skipping cells already holding the same block. */
	private void line(MinecraftServer srv, String id, BlockPos... points) {
		ServerLevel level = srv.overworld();
		Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
		for (int i = 0; i < points.length; i++) {
			BlockPos from = i == 0 ? points[0] : points[i - 1], to = points[i];
			BlockPos at = from;
			while (true) {
				BlockState there = level.getBlockState(at);
				boolean joined = there.getBlock() == block || BuiltInRegistries.BLOCK.getKey(there.getBlock()).getPath().matches("busway_industrial|cable_industrial") && id.matches(".*(busway|cable)_industrial");
				if (!joined) {
					require(there.canBeReplaced(), id + " at " + at.toShortString() + " is blocked by " + there);
					place(srv, id, at, SOUTH);
				}
				if (at.equals(to)) {
					break;
				}
				at = at.offset(Integer.signum(to.getX() - at.getX()), Integer.signum(to.getY() - at.getY()), Integer.signum(to.getZ() - at.getZ()));
			}
		}
	}

	private BlockPos at(int x, int y, int z) {
		return o.offset(x, y, z);
	}

	private void build(TestServerContext server) {
		server.runOnServer(srv -> {
			mine(srv);
			log("the machine and the track are placed");
		});
		context.waitTicks(40);
		gantries(server);
		server.runOnServer(srv -> {
			dump(srv);
			crushing(srv);
			flotation(srv);
			log(parts.size() + " plant parts placed");
		});
		context.waitTicks(40);
		server.runOnServer(this::wiring);
		context.waitTicks(40);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			PlantBlockEntity sorter = (PlantBlockEntity) level.getBlockEntity(parts.get("sorter"));
			ServerPlayer player = srv.getPlayerList().getPlayers().get(0);
			// clicking the sorter's faces, as a player sets it: its east side with the ore, its south side empty-handed and sneaking
			BlockPos cell = parts.get("sorter").offset(2, 1, 1);
			ItemStack ore = new ItemStack(item(oreId));
			player.setItemInHand(InteractionHand.MAIN_HAND, ore);
			player.gameMode.useItemOn(player, level, ore, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(cell), Direction.EAST, cell, false));
			player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
			player.setShiftKeyDown(true);
			BlockPos south = parts.get("sorter").offset(1, 1, 2);
			player.gameMode.useItemOn(player, level, ItemStack.EMPTY, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(south), Direction.SOUTH, south, false));
			player.setShiftKeyDown(false);
			log("sorter: " + sorter.inspect().getString());
		});
	}

	private void mine(MinecraftServer srv) {
		bigPart(srv, TailingsBlocks.CUTTERHEAD, c);
		bigPart(srv, TailingsBlocks.SEGMENT_ERECTOR, c.offset(0, 0, 1));
		bigPart(srv, TailingsBlocks.MAIN_BEAM, beam);
		bigPart(srv, TailingsBlocks.TRACK_LAYER, c.offset(1, 2, 8));
		place(srv, "tailings:cable_junction", c.offset(-2, 0, 14), SOUTH);
		for (int z = 11; z <= 40; z++) {
			place(srv, "tailings:mine_rail", c.offset(5, 1, z), NORTH);
		}
		for (int z = 11; z <= 50; z++) {
			place(srv, "tailings:mine_rail", c.offset(2, 1, z), NORTH);
		}
		DrillBlockEntity cab = (DrillBlockEntity) srv.overworld().getBlockEntity(beam);
		cab.program().setTargetY(c.getY() + 1 - DROP);
		cab.program().setFlatLength(FLAT);
		stock(srv, cab);
	}

	/** An item that goes on a rail, used on it from a survival hand. */
	private void onRail(MinecraftServer srv, String id, int row) {
		need(srv, id, 1, 0);
		ServerPlayer player = srv.getPlayerList().getPlayers().get(0);
		BlockPos rail = c.offset(2, 1, row);
		ItemStack stack = new ItemStack(item(id));
		player.setYRot(NORTH);
		player.setItemInHand(InteractionHand.MAIN_HAND, stack);
		player.gameMode.useItemOn(player, srv.overworld(), stack, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(rail), Direction.UP, rail, false));
		player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
		require(stack.isEmpty(), id + " should go on the rail at row " + row + " from a survival hand");
	}

	private void gantries(TestServerContext server) {
		for (TbmGantry kind : TbmGantry.values()) {
			server.runOnServer(srv -> onRail(srv, "tailings:" + kind.id(), kind.row()));
			context.waitTicks(10);
			server.runOnServer(srv -> {
				List<GantryEntity> placed = srv.overworld().getEntitiesOfClass(GantryEntity.class, new AABB(c).inflate(60), g -> g.kind() == kind);
				require(!placed.isEmpty(), kind.displayName() + " should stand on the rail");
				log(kind.displayName() + ": " + placed.get(0).couple());
			});
		}
		for (Object[] each : new Object[][] {{"tailings:mine_locomotive", 42, 0}, {"tailings:mine_car", 45, 1}, {"tailings:mine_car", 47, 2}}) {
			server.runOnServer(srv -> onRail(srv, (String) each[0], (Integer) each[1]));
			context.waitTicks(10);
			server.runOnServer(srv -> {
				List<MineVehicleEntity> there = srv.overworld().getEntitiesOfClass(MineVehicleEntity.class, new AABB(c.offset(2, 1, (Integer) each[1])).inflate(1, 2, 2),
						v -> !java.util.Arrays.asList(train).contains(v));
				require(!there.isEmpty(), each[0] + " should stand on the rail at row " + each[1]);
				train[(Integer) each[2]] = there.get(0);
			});
		}
		server.runOnServer(srv -> require(train[0].carIds().size() == 2, "both cars should couple, got " + train[0].carIds().size()));
	}

	/** What a player tops the cab's slots up with from their pack: rail, gravel, cobble and cable. */
	private void stock(MinecraftServer srv, net.minecraft.world.Container cab) {
		top(srv, cab, DrillBlockEntity.SLOT_RAIL, "tailings:mine_rail");
		top(srv, cab, DrillBlockEntity.SLOT_GRAVEL, "minecraft:gravel");
		top(srv, cab, DrillBlockEntity.SLOT_COBBLE, "minecraft:cobblestone");
	}

	private void top(MinecraftServer srv, net.minecraft.world.Container cab, int slot, String id) {
		ItemStack now = cab.getItem(slot);
		int add = 64 - now.getCount();
		if (add >= 16) {
			need(srv, id, add, 0);
			cab.setItem(slot, new ItemStack(item(id), 64));
		}
	}

	private void dump(MinecraftServer srv) {
		BlockPos rail = c.offset(2, 1, 44);
		bigPart(srv, TailingsBlocks.DUMP_POCKET, rail.offset(-1, -3, 0));
		bigPart(srv, TailingsBlocks.DUMP_RAMP, rail.offset(1, 0, 0));
		part(srv, "feeder", Machine.APRON_FEEDER, rail.offset(-1, -3, 3));
		part(srv, "sorter", Machine.ORE_SORTER, rail.offset(-2, -3, 7));
		part(srv, "oreBin", Machine.ORE_BIN, rail.offset(1, -3, 7));
		part(srv, "wasteBin", Machine.ORE_BIN, rail.offset(-2, -3, 10));
		bigPart(srv, TailingsBlocks.TRACTION_RECTIFIER, c.offset(0, 1, 36));
	}

	private void crushing(MinecraftServer srv) {
		part(srv, "jaw", Machine.JAW, at(0, 0, 0));
		part(srv, "grizzly", Machine.GRIZZLY, at(0, 4, 0));
		part(srv, "screen", Machine.SCREEN, at(6, 3, 0));
		part(srv, "cone", Machine.CONE, at(6, 3, 6));
		part(srv, "mill", Machine.BALL_MILL, at(11, -1, 0));
		part(srv, "cyclone", Machine.CYCLONE, at(9, -1, 2));
		// the ore bin's belt: under the bin, up and over the grizzly
		belts(srv, Direction.Axis.X, new int[][] {{-9, -1, 2}, {-8, -1, 2}, {-7, 0, 2}, {-6, 1, 2}, {-5, 2, 2}, {-4, 3, 2}, {-3, 4, 2}, {-2, 5, 2}, {-1, 6, 2}, {0, 6, 2}, {1, 6, 2}});
		part(srv, "binDrive", Machine.CONVEYOR_DRIVE, at(2, 6, 1));
		// the grizzly's undersize to the screen
		belts(srv, Direction.Axis.X, new int[][] {{3, 4, 1}, {4, 4, 1}, {5, 4, 1}});
		part(srv, "bypassDrive", Machine.CONVEYOR_DRIVE, at(5, 4, -1));
		// the jaw's product up under the screen
		belts(srv, Direction.Axis.X, new int[][] {{0, 0, 4}, {1, 0, 4}, {2, 0, 4}, {3, 0, 4}, {4, 1, 4}, {5, 2, 4}, {6, 2, 4}, {7, 2, 4}});
		part(srv, "jawDrive", Machine.CONVEYOR_DRIVE, at(8, 2, 3));
		// sand off the screen's east face into the mill's feed end
		belts(srv, Direction.Axis.X, new int[][] {{9, 3, 1}, {10, 3, 1}, {11, 3, 1}});
		part(srv, "sandDrive", Machine.CONVEYOR_DRIVE, at(12, 3, 0));
		// the cone's product round to the top of the screen: three runs, as belts run straight
		belts(srv, Direction.Axis.X, new int[][] {{7, 3, 9}, {8, 3, 9}});
		part(srv, "coneDrive", Machine.CONVEYOR_DRIVE, at(8, 3, 10));
		belts(srv, Direction.Axis.Z, new int[][] {{9, 3, 9}, {9, 4, 8}, {9, 5, 7}, {9, 6, 6}, {9, 6, 5}, {9, 6, 4}});
		part(srv, "returnDrive", Machine.CONVEYOR_DRIVE, at(9, 7, 4));
		belts(srv, Direction.Axis.X, new int[][] {{9, 6, 3}, {8, 6, 3}});
		part(srv, "topDrive", Machine.CONVEYOR_DRIVE, at(7, 6, 2));
		// mill discharge round to the cyclone's north face
		String pipe = "wildcatter:gas_line";
		line(srv, pipe, at(11, -1, 6), at(7, -1, 6), at(7, -1, 1), at(9, -1, 1));
		// the fines from the cyclone's south face over to the conditioner
		line(srv, pipe, at(10, 2, 4), at(10, 2, 7), at(16, 2, 7), at(16, -1, 7));
		line(srv, pipe, at(17, -1, 7));
	}

	private void flotation(MinecraftServer srv) {
		String pipe = "wildcatter:gas_line";
		part(srv, "tank", Machine.CONDITIONER, at(16, -1, 8));
		line(srv, pipe, at(16, -1, 11));
		part(srv, "bank", Machine.FLOTATION_BANK, at(16, -1, 12));
		part(srv, "blower", Machine.BLOWER, at(14, -1, 13));
		line(srv, pipe, at(22, -1, 25));
		part(srv, "thickener", Machine.THICKENER, at(22, -1, 26));
		line(srv, pipe, at(22, -1, 34));
		part(srv, "filter", Machine.FILTER, at(22, -1, 35));
		line(srv, pipe, at(16, -1, 25), at(16, -1, 26));
		part(srv, "pond", Machine.TAILINGS_POND, at(16, -1, 27));
	}

	// ---- power ----------------------------------------------------------------------------------------------------------------

	private BlockPos skid(MinecraftServer srv, BlockPos panel, boolean gas) {
		for (String id : gas
				? List.of("wildcatter:genset_base_frame", "wildcatter:radiator", "wildcatter:genset_gas_engine", "wildcatter:exhaust_silencer_large", "wildcatter:genset_control_panel")
				: List.of("wildcatter:fuel_tank", "wildcatter:radiator", "wildcatter:genset_diesel_engine", "wildcatter:exhaust_stack", "wildcatter:genset_control_panel")) {
			need(srv, id, 1, 0);
		}
		List<BlockPos> overwrote = GensetSkid.build(srv.overworld(), panel, Direction.NORTH, gas);
		if (!overwrote.isEmpty()) {
			log("the genset at " + panel.toShortString() + " overwrote " + overwrote.size() + " blocks");
		}
		panels.add(panel);
		return panel;
	}

	/** A unit substation run backward east of {@code from}: 480 V in, 4.16 kV out. Returns the cell its 4.16 kV side feeds. */
	private BlockPos stepUp(MinecraftServer srv, BlockPos from) {
		Block sub = BuiltInRegistries.BLOCK.getValue(Identifier.parse("wildcatter:unit_substation"));
		need(srv, "wildcatter:unit_substation", 1, 0);
		BlockPos first = from.relative(Direction.EAST);
		BlockState state = sub.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		BlockPos main = first.relative(Direction.EAST, 3);
		srv.overworld().setBlock(main, state, Block.UPDATE_ALL);
		BigParts.grow(srv.overworld(), main, state);
		return from.relative(Direction.EAST, 5);
	}

	private void wiring(MinecraftServer srv) {
		String mv = "wildcatter:cable_mv", lv = "wildcatter:cable_industrial", bus = "wildcatter:busway_industrial";
		int py = gy + 1;
		// The machine's 1 MW is too much for one set (it trips on frequency), for one 1 MVA unit substation (112 %) and for 800 A of
		// busway (it burns through): two gas sets, each through its own substation, joined on the 4.16 kV side.
		BlockPos mine = skid(srv, c.offset(-12, py - c.getY(), 10), true);
		BlockPos mineTwo = skid(srv, c.offset(-12, py - c.getY(), 6), true);
		BlockPos mineOut = stepUp(srv, mine);
		BlockPos mineTwoOut = stepUp(srv, mineTwo);
		BlockPos terminal = c.offset(-3, 0, 14);
		line(srv, mv, mineTwoOut, mineOut, new BlockPos(terminal.getX(), py, mineOut.getZ()), new BlockPos(terminal.getX(), py, terminal.getZ()), terminal);
		// the traction rectifier: a 500 kW diesel set; one train draws about 110 A of its rating
		BlockPos traction = skid(srv, c.offset(-12, py - c.getY(), 30), false);
		size = dev.wildcatter.energy.ConductorSize.AWG_4_0;
		line(srv, lv, traction.east(), new BlockPos(c.getX() - 1, py, traction.getZ()), c.offset(-1, 1, traction.getZ() - c.getZ()), c.offset(-1, 1, 36));
		size = null;
		// the mill: a 1 MW gas set stepped up to 4.16 kV
		BlockPos millSet = skid(srv, at(9, py - o.getY(), -14), true);
		BlockPos millOut = stepUp(srv, millSet);
		line(srv, mv, millOut, new BlockPos(millOut.getX(), o.getY() + 5, millOut.getZ()), new BlockPos(at(10, 0, 0).getX(), o.getY() + 5, millOut.getZ()), at(10, 5, 0), at(10, -1, 0));
		// the crushers and the dump: a 1 MW gas set, 800 A busway overhead along z = o - 3, a drop to each motor box
		BlockPos crushers = skid(srv, c.offset(-12, py - c.getY(), 22), true);
		int trunkA = o.getZ() - 3;
		BlockPos up = new BlockPos(crushers.getX() + 1, ytop, crushers.getZ());
		line(srv, bus, crushers.east(), up, new BlockPos(up.getX(), ytop, trunkA), new BlockPos(at(14, 0, 0).getX(), ytop, trunkA));
		size = dev.wildcatter.energy.ConductorSize.AWG_4_0;
		drop(srv, lv, at(-1, 0, 0), ytop, trunkA);    // the jaw, 165 A
		drop(srv, lv, at(5, 3, 6), ytop, trunkA);     // the cone, 200 A
		size = null;
		for (BlockPos box : new BlockPos[] {parts.get("feeder").west(), parts.get("sorter").west(), at(1, 6, 1), at(4, 4, -1), at(7, 3, 10), at(8, 7, 4), at(6, 6, 2), at(11, 3, 0)}) {
			drop(srv, lv, box, ytop, trunkA);
		}
		// three drops that can't come straight down
		line(srv, lv, at(5, 3, 0), at(4, 3, 0), at(4, 4, 0));
		line(srv, lv, at(7, 2, 3), at(5, 2, 3), new BlockPos(at(5, 0, 3).getX(), ytop, at(0, 0, 3).getZ()), new BlockPos(at(5, 0, 0).getX(), ytop, trunkA));
		line(srv, lv, at(8, -1, 2), at(8, 0, 2), at(4, 0, 2), new BlockPos(at(4, 0, 0).getX(), ytop, at(0, 0, 2).getZ()), new BlockPos(at(4, 0, 0).getX(), ytop, trunkA));
		// the flotation plant: its own 1 MW gas set and busway
		BlockPos floats = skid(srv, at(31, py - o.getY(), 5), true);
		int high = ytop + 2, trunkB = o.getZ() + 5;
		BlockPos rise = new BlockPos(floats.getX() + 1, high, floats.getZ());
		line(srv, bus, floats.east(), rise, new BlockPos(at(13, 0, 0).getX(), high, trunkB));
		line(srv, bus, at(15, -1, 12), new BlockPos(at(15, 0, 0).getX(), high, at(0, 0, 12).getZ()), new BlockPos(at(15, 0, 0).getX(), high, trunkB));   // the bank, 530 A
		size = dev.wildcatter.energy.ConductorSize.AWG_4_0;
		drop(srv, lv, at(13, -1, 13), high, trunkB);   // the blower, 113 A
		size = null;
		for (BlockPos box : new BlockPos[] {at(15, -1, 8), at(21, -1, 26), at(21, -1, 35)}) {
			drop(srv, lv, box, high, trunkB);
		}
		log("wired: six generators, conductors sized to their amps");
	}

	private void drop(MinecraftServer srv, String lv, BlockPos box, int top, int trunkZ) {
		line(srv, lv, box, new BlockPos(box.getX(), top, box.getZ()), new BlockPos(box.getX(), top, trunkZ));
	}

	private String read(MinecraftServer srv, BlockPos at) {
		return BigParts.entity(srv.overworld(), at) instanceof dev.wildcatter.energy.Inspectable i ? i.inspect().getString() : "(no readout at " + at.toShortString() + ")";
	}

	private void power(TestServerContext server) {
		context.waitTicks(40);
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			for (BlockPos panel : panels) {
				GensetControlPanelBlockEntity box = (GensetControlPanelBlockEntity) level.getBlockEntity(panel);
				if (level.getBlockEntity(GensetSkid.base(panel, Direction.NORTH, false)) instanceof GensetPartBlockEntity tank) {
					tank.setFuel(2000);
				}
				box.setGasForTest(200);
				box.setWantRun(true);
			}
			log("staged: gas and diesel put in the generators; each started from its panel");
		});
		context.waitTicks(300);
		server.runOnServer(srv -> {
			for (BlockPos panel : panels) {
				log("generator " + panel.toShortString() + ": " + read(srv, panel));
			}
		});
		context.takeScreenshot("tailings-play-powered");
	}

	// ---- playing ---------------------------------------------------------------------------------------------------------------

	private void play(TestServerContext server) {
		int rate = Integer.parseInt(System.getenv().getOrDefault("TAILINGS_RATE", "100"));
		server.runOnServer(srv -> {
			DrillBlockEntity cab = (DrillBlockEntity) srv.overworld().getBlockEntity(beam);
			cab.press(6, null);   // the screen's "stop on ore" off: this drive means to go through it
			cab.press(10, null);
			log("pressed Go: " + cab.core().readout() + " / " + cab.core().note());
		});
		server.runCommand("tick rate " + rate);
		int minutes = Integer.parseInt(System.getenv().getOrDefault("TAILINGS_GAME_MINUTES", "50"));
		boolean[] done = {false};
		for (int t = 0; t < minutes * 2 && !done[0]; t++) {
			long from = server.computeOnServer(srv -> srv.overworld().getGameTime());
			while (server.computeOnServer(srv -> srv.overworld().getGameTime()) - from < 600) {   // 30 game seconds
				context.waitTicks(10);
				server.runOnServer(srv -> {
					for (String name : List.of("mill", "cyclone")) {   // staged: the mill only holds 3 s of its water
						((PlantBlockEntity) srv.overworld().getBlockEntity(parts.get(name))).addWater(100_000);
					}
				});
			}
			int half = t;
			server.runOnServer(srv -> {
				ServerLevel level = srv.overworld();
				for (BlockPos panel : panels) {
					GensetControlPanelBlockEntity box = (GensetControlPanelBlockEntity) level.getBlockEntity(panel);
					box.setGasForTest(200);
				}
				for (String name : List.of("mill", "cyclone")) {
					((PlantBlockEntity) level.getBlockEntity(parts.get(name))).addWater(100_000);
				}
				PlantBlockEntity tank = (PlantBlockEntity) level.getBlockEntity(parts.get("tank"));
				for (String reagent : List.of("tailings:lime", "tailings:xanthate", "tailings:mibc")) {
					ItemStack stack = new ItemStack(item(reagent));
					if (tank.offer(stack)) {
						need(srv, reagent, 1, 0);
					}
				}
				TbmEntity machine = level.getEntitiesOfClass(TbmEntity.class, new AABB(c).inflate(120)).stream().findFirst().orElse(null);
				String tbm;
				if (machine != null) {
					stock(srv, machine);
					tbm = machine.core().readout() + " / " + machine.core().note() + " at " + machine.blockPosition().toShortString();
				} else if (level.getBlockEntity(beam) instanceof DrillBlockEntity cab) {
					stock(srv, cab);
					tbm = "(blocks) " + cab.core().readout() + " / " + cab.core().note();
				} else {
					tbm = "(machine not found)";
				}
				PlantBlockEntity filter = (PlantBlockEntity) level.getBlockEntity(parts.get("filter"));
				int ingots = 0;
				for (Item ingot : List.of(Items.COPPER_INGOT, Items.IRON_INGOT, Items.GOLD_INGOT)) {
					ingots += filter.countIn(new int[] {1, 2}, new ItemStack(ingot));
				}
				MineVehicleEntity loco = train[0];
				require(srv.getPlayerList().getPlayers().get(0).isAlive(), "the camera player should stay alive");
				log(String.format("mean tick %.1f ms", srv.getAverageTickTimeNanos() / 1e6));
				log(String.format("%.1f min | TBM %s | train z %.0f %s, cars %d %d | filter paid %d", (half + 1) / 2.0, tbm, loco.getZ() - c.getZ(), loco.status(),
						level.getEntity(loco.carIds().get(0)) instanceof MineVehicleEntity a ? a.load().total() : -1,
						level.getEntity(loco.carIds().get(1)) instanceof MineVehicleEntity b ? b.load().total() : -1, ingots));
				if (half % 4 == 0) {
					parts.forEach((name, at) -> log("  " + name + ": " + read(srv, at)));
					panels.forEach(panel -> log("  generator " + panel.toShortString() + ": " + read(srv, panel)));
					log("  rectifier: " + read(srv, c.offset(0, 1, 36)));
					log("  junction: " + read(srv, c.offset(-2, 0, 14)));
					log("  mine substation: " + read(srv, c.offset(-10, gy + 1 - c.getY(), 10)));
				}
				if (ingots >= 3) {
					done[0] = true;
				}
			});
			if (t % 8 == 3) {
				context.takeScreenshot("tailings-play-" + t);
			}
		}
		server.runCommand("tick rate 20");
		context.waitTicks(40);
		context.takeScreenshot("tailings-play-end");
		require(done[0], "the mine should pay ingots within " + minutes + " game minutes");
		log("PAID: real rock to ingots");
	}
}
