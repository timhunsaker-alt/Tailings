package dev.wildcatter.tailings.fabric.gametest;

import dev.wildcatter.control.ControlNet;
import dev.wildcatter.control.PlcCabinetBlockEntity;
import dev.wildcatter.control.PlcModuleItem;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.entity.UltraHaulTruckEntity;
import dev.wildcatter.tailings.fabric.gametest.Trailer.Cam;
import dev.wildcatter.tailings.plant.Machine;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestServerContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.fabric.api.client.gametest.v1.screenshot.TestScreenshotOptions;
import net.minecraft.client.CloudStatus;
import net.minecraft.client.GraphicsPreset;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ParticleStatus;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.AABB;

/**
 * The PLC trailer's Tailings shots (TAILINGS_ONLY=plcTrailer): real Tailings machines on Wildcatter's control cable, run by a real
 * program in a real cabinet, on the trailer's generated world at golden hour. PLC_TRAILER_SHOTS=1,3 runs only those shots; stills go
 * to PLC_TRAILER_OUT, films to {@code fabric/build/scene-videos/plcTrailer_*.mov}.
 * <ol>
 * <li>{@code belts}: the pit line's conveyor start-up, horn and amber first, then the belts from the last to the first, the gyratory
 * and its feeder, green with the line up;</li>
 * <li>{@code plant}: the same crushing and flotation plant running, the cabinet and its stack light in front;</li>
 * <li>{@code tbm}: the TBM boring on its cable junction's GO from a cabinet on the cut's bench.</li>
 * </ol>
 * The cabinet, its 24 V supply, the control cable to every machine and the landings are the real ones (the walk from the cabinet is
 * checked). The machines' motors have no grid in the scene, so the test runs each one while the PLC's own output for it is on: the
 * order and timing on screen are the program's.
 */
final class PlcTrailer {
	private static final String SEED = System.getenv().getOrDefault("TAILINGS_TRAILER_SEED", "tailings");
	private static final Path OUT = Path.of(System.getenv().getOrDefault("PLC_TRAILER_OUT", "plc-trailer")).toAbsolutePath();
	private static final int W = 1920, H = 1080;

	private final ClientGameTestContext context;
	private TestServerContext server;
	/** Each machine the PLC runs, by the output that runs it. */
	private final Map<BlockPos, String> runs = new LinkedHashMap<>();
	private BlockPos cabinet;
	private Runnable each;

	PlcTrailer(ClientGameTestContext context) {
		this.context = context;
	}

	private static void log(String line) {
		System.out.println("[tailings-plc-trailer] " + line);
	}

	private static boolean wanted(int shot) {
		String only = System.getenv("PLC_TRAILER_SHOTS");
		return only == null || only.isBlank() || java.util.Arrays.asList(only.split(",")).contains(Integer.toString(shot));
	}

	void run() {
		context.runOnClient(client -> {
			client.options.renderDistance().set(20);
			client.options.graphicsPreset().set(GraphicsPreset.FANCY);
			client.options.particles().set(ParticleStatus.ALL);
			client.options.cloudStatus().set(CloudStatus.FANCY);
			client.options.entityDistanceScaling().set(3.0);
			client.options.fov().set(70);
			client.options.gamma().set(1.0);
			client.options.chatVisibility().set(net.minecraft.world.entity.player.ChatVisiblity.HIDDEN);
		});
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false).adjustSettings(s -> {
			s.setSeed(SEED);
			s.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE);
		}).create()) {
			server = world.getServer();
			context.waitTicks(40);
			server.runCommand("gamerule advance_time false");
			server.runCommand("gamerule advance_weather false");
			server.runCommand("weather clear");
			server.runCommand("difficulty peaceful");
			server.runCommand("gamemode spectator @p");
			server.runCommand("tp @p 0 160 0");
			context.runOnClient(client -> {
				if (!client.gui.hud.isHidden()) {
					client.gui.hud.toggle();
				}
			});
			context.waitTicks(20);
			List<String> failed = new ArrayList<>();
			if (wanted(1) || wanted(2)) {
				try {
					plantSite();
				} catch (RuntimeException | AssertionError ex) {
					log("plant failed: " + ex);
					ex.printStackTrace();
					failed.add("1,2");
				}
			}
			if (wanted(3)) {
				try {
					tbmSite();
				} catch (RuntimeException | AssertionError ex) {
					log("tbm failed: " + ex);
					ex.printStackTrace();
					failed.add("3");
				}
			}
			log("done; failed shots " + failed);
		}
	}

	// ---- the camera, every tick keeping the machines on their PLC outputs ----

	private void put(Cam c) {
		double dx = c.tx() - c.x(), dy = c.ty() - c.y() - 1.62, dz = c.tz() - c.z();
		double yaw = Math.toDegrees(Math.atan2(-dx, dz));
		double pitch = -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
		server.runCommand(String.format(Locale.ROOT, "tp @p %.3f %.3f %.3f %.2f %.2f", c.x(), c.y(), c.z(), yaw, pitch));
	}

	/** The machines follow the PLC's outputs for them: each runs while its output is on. */
	private void follow() {
		if (runs.isEmpty() || cabinet == null) {
			return;
		}
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			if (!(level.getBlockEntity(cabinet) instanceof PlcCabinetBlockEntity box)) {
				return;
			}
			for (Map.Entry<BlockPos, String> run : runs.entrySet()) {
				if (level.getBlockEntity(run.getKey()) instanceof PlantBlockEntity part) {
					if (Boolean.TRUE.equals(box.cpu().read(run.getValue()))) {
						part.forceRunning();
					} else {
						part.stopForced();
					}
				}
			}
		});
	}

	private void ticks(int n) {
		for (int t = 0; t < n; t++) {
			follow();
			context.waitTick();
			if (each != null && t % 20 == 0) {
				each.run();
			}
		}
	}

	private void settle(int ticks) {
		ticks(ticks);
		try {
			context.waitFor(client -> client.levelRenderer.hasRenderedAllSections(), 400);
		} catch (RuntimeException | AssertionError ex) {
			log("sections still compiling: " + ex.getMessage());
		}
		ticks(10);
	}

	private void still(String name) {
		server.runCommand("kill @e[type=item]");
		Path at = context.takeScreenshot(TestScreenshotOptions.of(name).withSize(W, H).withDestinationDir(OUT).disableCounterPrefix());
		log("still " + at);
	}

	/** A shot: the start still, then the filmed move from a to b (eased) over {@code length} ticks; {@code at} runs on its tick. */
	private void shot(String id, Cam a, Cam b, int length, Map<Integer, Runnable> at) {
		put(a);
		settle(60);
		still(id + "_a");
		Filming film = context.computeOnClient(client -> Filming.start(client, "plcTrailer_" + id));
		for (int t = 0; t <= length; t++) {
			double s = (double) t / length;
			put(a.lerp(b, s * s * (3 - 2 * s)));
			if (at.containsKey(t)) {
				at.get(t).run();
			}
			follow();
			context.waitTick();
			if (t % 20 == 0) {
				server.runCommand("kill @e[type=item]");
				if (each != null) {
					each.run();
				}
			}
		}
		film.stop();
		still(id + "_b");
	}

	// ---- the control side: a cabinet with its own small steam set for 24 V, cable to every device ----

	@SuppressWarnings({"unchecked", "rawtypes"})
	static BlockState facing(Block block, Direction way) {
		BlockState state = block.defaultBlockState();
		Property<?> p = block.getStateDefinition().getProperty("facing");
		return p != null && p.getPossibleValues().contains(way) ? state.setValue((Property) p, (Comparable) way) : state;
	}

	static Block wc(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse("wildcatter:" + id));
	}

	/** Ground the cable may be buried in. */
	private static boolean ground(BlockState s) {
		return s.is(Blocks.STONE) || s.is(Blocks.DIRT) || s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.GRAVEL) || s.is(Blocks.ANDESITE) || s.is(Blocks.COARSE_DIRT)
				|| s.is(Blocks.GRANITE) || s.is(Blocks.DIORITE) || s.is(Blocks.DEEPSLATE) || s.is(Blocks.TUFF) || s.is(Blocks.SAND) || s.is(Blocks.CLAY)
				|| s.is(Blocks.SMOOTH_STONE);
	}

	/**
	 * The cabinet on the ground at {@code at} (its cell, on a plinth), a junction box beside it on {@code -along} with the horn and, on a
	 * second box over it, the stack light, both facing {@code front}. Its 24 V comes up a cable from the playthrough's small steam set
	 * buried 5 m under the box: junction box, supply, hot wire, steam turbine, boiler on a campfire, the neutral behind on {@code back}.
	 * Returns the surface box; the buried run's cable cells go into {@code laid}.
	 */
	static BlockPos rig(ServerLevel level, BlockPos at, Direction along, Direction front, BlockPos[] devices, Set<BlockPos> laid) {
		Direction back = front.getOpposite();
		BlockPos box = at.relative(along, -1);
		BlockPos low = box.below(5);
		BlockPos supply = low.relative(along, -1);
		BlockPos hot = low.relative(along, -2);
		BlockPos turbine = low.relative(along, -3);
		BlockPos boiler = low.relative(along, -4);
		for (BlockPos p : new BlockPos[] {at, box}) {
			level.setBlock(p.below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
		}
		// the buried room: stone round the set, air where it stands
		for (int i = -1; i <= 5; i++) {
			for (int j = -2; j <= 2; j++) {
				for (int dy = -2; dy <= 1; dy++) {
					BlockPos p = low.relative(along, -i).relative(front, j).above(dy);
					level.setBlock(p, (dy >= 0 && j == 0 || dy == 0 && j == -1) && i >= 0 && i <= 4 ? Blocks.AIR.defaultBlockState() : Blocks.STONE.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
		}
		level.setBlock(boiler.below(), Blocks.CAMPFIRE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(boiler, wc("boiler").defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(turbine, wc("steam_turbine").defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(hot, wc("plastic_wire_hot").defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(supply, wc("control_supply").defaultBlockState(), Block.UPDATE_ALL);
		for (BlockPos p : new BlockPos[] {turbine, hot, supply}) {
			level.setBlock(p.relative(back), wc("plastic_wire_neutral").defaultBlockState(), Block.UPDATE_ALL);
		}
		level.setBlock(low, wc("junction_box").defaultBlockState(), Block.UPDATE_ALL);
		for (int dy = 1; dy <= 4; dy++) {
			level.setBlock(low.above(dy), wc("control_cable").defaultBlockState(), Block.UPDATE_ALL);
			laid.add(low.above(dy));
		}
		level.setBlock(box, wc("junction_box").defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(at, facing(wc("plc_cabinet"), front), Block.UPDATE_ALL);
		BigParts.grow(level, at, level.getBlockState(at));
		((dev.wildcatter.block.BoilerBlockEntity) level.getBlockEntity(boiler)).addWater(4000);
		BlockPos upper = box.above();
		level.setBlock(upper, wc("junction_box").defaultBlockState(), Block.UPDATE_ALL);
		devices[0] = box.relative(front);
		devices[1] = upper.relative(front);
		level.setBlock(devices[0].below(), Blocks.SMOOTH_STONE.defaultBlockState(), Block.UPDATE_ALL);
		level.setBlock(devices[0], facing(wc("horn"), front), Block.UPDATE_CLIENTS);
		level.setBlock(devices[1], facing(wc("stack_light"), front), Block.UPDATE_CLIENTS);
		return box;
	}

	/**
	 * Cable from the device at {@code device} to the nearest cable already laid (or {@code net}'s cells): buried where it can be,
	 * through air where it must. Cells touching more than two things become junction boxes afterwards ({@link #joints}).
	 */
	static void route(ServerLevel level, BlockPos device, Set<BlockPos> net, Set<BlockPos> laid, int g, BlockPos lo, BlockPos hi) {
		List<BlockPos> cells = BigParts.cellsAt(level, device);
		if (cells.isEmpty()) {
			cells = List.of(device);
		}
		Set<BlockPos> own = new HashSet<>(cells);
		Map<BlockPos, Integer> dist = new HashMap<>();
		Map<BlockPos, BlockPos> from = new HashMap<>();
		PriorityQueue<Object[]> queue = new PriorityQueue<>((a, b) -> Integer.compare((int) a[1], (int) b[1]));
		for (BlockPos cell : cells) {
			for (Direction d : Direction.values()) {
				BlockPos n = cell.relative(d);
				if (!own.contains(n) && passable(level, n, net, laid) && !dist.containsKey(n)) {
					dist.put(n, cost(level, n, g));
					from.put(n, null);
					queue.add(new Object[] {n, cost(level, n, g)});
				}
			}
		}
		int seen = 0;
		while (!queue.isEmpty() && seen++ < 400_000) {
			Object[] top = queue.poll();
			BlockPos at = (BlockPos) top[0];
			int d0 = (int) top[1];
			if (d0 > dist.get(at)) {
				continue;
			}
			for (Direction d : Direction.values()) {
				if (net.contains(at.relative(d)) || laid.contains(at.relative(d))) {
					for (BlockPos step = at; step != null; step = from.get(step)) {
						level.setBlock(step, wc("control_cable").defaultBlockState(), Block.UPDATE_ALL);
						laid.add(step.immutable());
					}
					return;
				}
			}
			for (Direction d : Direction.values()) {
				BlockPos n = at.relative(d);
				if (n.getX() < lo.getX() || n.getY() < lo.getY() || n.getZ() < lo.getZ() || n.getX() > hi.getX() || n.getY() > hi.getY() || n.getZ() > hi.getZ()) {
					continue;
				}
				if (own.contains(n) || !passable(level, n, net, laid)) {
					continue;
				}
				int nd = d0 + cost(level, n, g);
				if (nd < dist.getOrDefault(n, Integer.MAX_VALUE)) {
					dist.put(n, nd);
					from.put(n, at);
					queue.add(new Object[] {n, nd});
				}
			}
		}
		throw new AssertionError("no way for control cable from " + device.toShortString());
	}

	private static boolean passable(ServerLevel level, BlockPos at, Set<BlockPos> net, Set<BlockPos> laid) {
		if (net.contains(at) || laid.contains(at)) {
			return false;
		}
		BlockState s = level.getBlockState(at);
		if (BigParts.entity(level, at) != null || level.getBlockEntity(at) != null) {
			return false;
		}
		return s.isAir() || ground(s) && !level.getBlockState(at.above()).is(TailingsBlocks.MINE_RAIL);
	}

	/** Buried deep is cheapest, just under the grass next, through the open air dearest. */
	private static int cost(ServerLevel level, BlockPos at, int g) {
		BlockState s = level.getBlockState(at);
		if (s.isAir()) {
			// in the open, a cable keeps to the belt or machine beside it, as on its tray, rather than hanging alone
			for (Direction d : Direction.values()) {
				BlockState by = level.getBlockState(at.relative(d));
				if (d != Direction.UP && (by.is(TailingsBlocks.HEAVY_BELT_CONVEYOR) || by.is(TailingsBlocks.BELT_CONVEYOR))) {
					return 2;
				}
			}
			return 40;
		}
		return at.getY() <= g - 2 ? 1 : 3;
	}

	/** A cable cell touching more than two things is a bad joint: a junction box there, as the playthrough's trunk does. */
	static void joints(ServerLevel level, Set<BlockPos> laid) {
		for (BlockPos at : laid) {
			int touching = 0;
			for (Direction side : Direction.values()) {
				if (ControlNet.joins(level, at.relative(side))) {
					touching++;
				}
			}
			if (touching > 2) {
				level.setBlock(at, wc("junction_box").defaultBlockState(), Block.UPDATE_ALL);
			}
		}
	}

	static void auto(ServerLevel level, BlockPos at) {
		var device = ControlNet.deviceAt(level, at);
		if (device == null) {
			throw new AssertionError("no device at " + at.toShortString());
		}
		device.cycleHoa(at);
		device.cycleHoa(at);
	}

	/** Output k of the DO16s in slots from {@code firstSlot}. */
	private static String out(int k, int firstSlot) {
		return "%QX" + ((firstSlot + k / 16) * PlcCabinetBlockEntity.BYTES_PER_SLOT + k % 16 / 8) + "." + (k % 8);
	}

	static String land(PlcCabinetBlockEntity box, BlockPos at, int point, String address) {
		String refused = box.land(at, point, address);
		if (refused != null) {
			throw new AssertionError(address + " on " + at.toShortString() + ": " + refused);
		}
		return address;
	}

	private void checkNet(ServerLevel level, BlockPos cab, List<BlockPos> all) {
		var found = ControlNet.walk(level, BigParts.cellsAt(level, cab));
		List<BlockPos> missed = new ArrayList<>();
		for (BlockPos at : all) {
			if (!found.devices().contains(ControlNet.mainOf(level, at))) {
				missed.add(at);
			}
		}
		log("the cabinet's walk reaches " + found.devices().size() + " devices; missed " + missed + (found.badJoint() != null ? ", bad joint at " + found.badJoint().toShortString() : ""));
		if (!missed.isEmpty()) {
			throw new AssertionError("missed " + missed);
		}
	}

	private String read(String what) {
		return server.computeOnServer(srv -> {
			var be = srv.overworld().getBlockEntity(cabinet);
			return be instanceof dev.wildcatter.energy.Inspectable i ? i.inspect().getString() : String.valueOf(be);
		});
	}

	// ---- 1 and 2: the pit line and flotation under one program ----

	private void plantSite() {
		BlockPos[] at = new BlockPos[1];
		Object[] v = new Object[4];
		PlantBlockEntity[][] groups = new PlantBlockEntity[5][];
		BlockPos[] sound = new BlockPos[2];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, -400, 300, 110, 170);
			int g = s.getY();
			BlockPos o = new BlockPos(s.getX() + 40, g, s.getZ() + 60);
			Trailer.pad(level, o.getX() - 45, o.getZ() - 60, o.getX() + 70, o.getZ() + 110, g, 16, 45);
			// the trucks' bench and the gyratory in it, as the first trailer's crusher shot
			Trailer.hill(level, o.getX() - 40, o.getZ() - 60, o.getX() + 40, o.getZ() - 1, g, (x, z) -> {
				int dx = Math.max(Math.max(o.getX() - 2 - x, x - (o.getX() + 15)), 0);
				int dz = Math.max(z - (o.getZ() - 3), 0);
				int north = z - (o.getZ() - 60);
				return Math.min(26 - Math.max(dx, dz) * 2, north);
			}, true);
			Trailer.fillBox(level, o, Blocks.STONE, -2, 15, 0, 25, -40, -3);
			Trailer.fillBox(level, o, Blocks.STONE, -2, 15, 0, 21, -2, -1);
			Trailer.fillBox(level, o, Blocks.GRAVEL, -2, 15, 25, 25, -40, -3);
			PlantBlockEntity feeder = Trailer.plant(level, Machine.PRIMARY_APRON_FEEDER, o.offset(5, 0, 3));
			Trailer.plant(level, Machine.SURGE_POCKET, o.offset(3, 2, 1));
			PlantBlockEntity crusher = Trailer.plant(level, Machine.GYRATORY_CRUSHER, o.offset(4, 8, 2));
			Trailer.part(level, TailingsBlocks.GYRATORY_POCKET, o.offset(0, 18, 0));
			Trailer.plant(level, Machine.ROCK_BREAKER, o.offset(12, 26, 2));
			Trailer.part(level, TailingsBlocks.TRUCK_DUMP_WALL, o.offset(1, 22, -2));
			// belt 1 south off the feeder, climbing 1 in 3 to a transfer 10 m up; belt 2 west from under its head, on up to the stacker
			int by = 0;
			for (int z = 15; z <= 45; z++) {
				if (z > 15 && (z - 15) % 3 == 0) {
					by++;
				}
				level.setBlock(o.offset(6, by, z), Trailer.heavyBelt(Direction.Axis.Z), Block.UPDATE_ALL);
			}
			PlantBlockEntity driveA = Trailer.plant(level, Machine.HEAVY_CONVEYOR_DRIVE, o.offset(7, by, 43));
			int y2 = by - 1;
			for (int x = 6; x >= -18; x--) {
				level.setBlock(o.offset(x, y2 + (6 - x) / 3, 45), Trailer.heavyBelt(Direction.Axis.X), Block.UPDATE_ALL);
			}
			int top = y2 + 8;
			PlantBlockEntity driveB = Trailer.plant(level, Machine.HEAVY_CONVEYOR_DRIVE, o.offset(-18, top, 46));
			Trailer.plant(level, Machine.STOCKPILE_HEAD, o.offset(-21, top - 4, 44));
			BlockPos tip = o.offset(-20, 40, 47);
			for (int i = 0; i < 500; i++) {
				dev.wildcatter.tailings.pit.Loose.muck(level, tip, TailingsBlocks.STOCKPILE_ROCK.defaultBlockState(), Map.of("minecraft:copper_ore", 3));
			}
			var truck = TailingsEntities.ULTRA_HAUL_TRUCK.create(level, EntitySpawnReason.COMMAND);
			truck.setPos(o.getX() + 7.0, o.getY() + 26, o.getZ() - 3.0 - truck.spec().length() / 2);
			truck.setYRot(180);
			truck.setFuel(truck.tank());
			truck.load().add("minecraft:copper_ore", 363_000);
			truck.refreshLoad();
			level.addFreshEntity(truck);
			// the flotation row east of belt 1, as p7_flotation lays it
			BlockPos f = o.offset(24, 0, 4);
			Block tubing = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "air_tubing"));
			PlantBlockEntity source = Trailer.plant(level, Machine.CYCLONE, f);
			Trailer.pipe(level, f.offset(0, 0, 2), f.offset(1, 0, 2));
			PlantBlockEntity tank = Trailer.plant(level, Machine.CONDITIONING_TANK_LARGE, f.offset(0, 0, 3));
			Trailer.pipe(level, f.offset(3, 0, 11));
			PlantBlockEntity[] cells = new PlantBlockEntity[6];
			for (int k = 0; k < cells.length; k++) {
				int z = 12 + 8 * k;
				cells[k] = Trailer.plant(level, Machine.TANK_CELL, f.offset(0, 0, z));
				Trailer.pipe(level, f.offset(3, 0, z + 7));
				for (int dz = 0; dz < 8; dz++) {
					if (z + dz <= 58) {
						Trailer.pipe(level, f.offset(7, 0, z + dz));
					}
					level.setBlock(f.offset(3, 7, z + dz), tubing.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
			PlantBlockEntity blower = Trailer.plant(level, Machine.FLOTATION_BLOWER_LARGE, f.offset(9, 0, 12));
			for (int x = 4; x <= 10; x++) {
				level.setBlock(f.offset(x, 7, 13), tubing.defaultBlockState(), Block.UPDATE_ALL);
			}
			for (int dy = 4; dy <= 6; dy++) {
				level.setBlock(f.offset(10, dy, 13), tubing.defaultBlockState(), Block.UPDATE_ALL);
			}
			for (String reagent : new String[] {"lime", "xanthate", "mibc"}) {
				ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(dev.wildcatter.tailings.Tailings.id(reagent)), 64);
				while (!stack.isEmpty() && tank.offer(stack)) {
					// a stack of each into the hopper
				}
			}
			// the start-up order, last machine first: flotation, belt 2, belt 1, the gyratory, its feeder
			List<PlantBlockEntity> flot = new ArrayList<>(List.of(blower, tank));
			flot.addAll(List.of(cells));
			groups[0] = flot.toArray(new PlantBlockEntity[0]);
			groups[1] = new PlantBlockEntity[] {driveB};
			groups[2] = new PlantBlockEntity[] {driveA};
			groups[3] = new PlantBlockEntity[] {crusher};
			groups[4] = new PlantBlockEntity[] {feeder};
			// the cabinet on the ground south of the transfer, its door and lamps to the south-east
			cabinet = o.offset(13, 1, 44);
			BlockPos[] devices = new BlockPos[2];
			Set<BlockPos> laid = new HashSet<>();
			BlockPos box = rig(level, cabinet, Direction.EAST, Direction.SOUTH, devices, laid);
			sound[0] = devices[0];
			sound[1] = devices[1];
			Set<BlockPos> net = new HashSet<>(List.of(box, box.above()));
			BlockPos lo = o.offset(-45, -8, -45), hi = o.offset(70, 30, 100);
			List<BlockPos> all = new ArrayList<>();
			for (PlantBlockEntity[] group : groups) {
				for (PlantBlockEntity part : group) {
					route(level, part.getBlockPos(), net, laid, g, lo, hi);
					all.add(part.getBlockPos());
				}
			}
			joints(level, laid);
			all.addAll(List.of(devices));
			log("staged: " + laid.size() + " cells of control cable to " + all.size() + " devices");
			checkNet(level, cabinet, all);
			v[0] = truck;
			v[1] = source;
			v[2] = cells;
			at[0] = o;
		});
		BlockPos o = at[0];
		// the modules, the landings, every machine to AUTO, and the program
		String program = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var box = (PlcCabinetBlockEntity) level.getBlockEntity(cabinet);
			box.seat(PlcModuleItem.Type.CPU_SMALL);
			box.seat(PlcModuleItem.Type.DO16);
			box.seat(PlcModuleItem.Type.DO16);
			StringBuilder vars = new StringBuilder("PROGRAM LINE\nVAR\n");
			int[] k = {0};
			java.util.function.BiFunction<BlockPos, Integer, String> landOut = (pos, point) -> land(box, pos, point, out(k[0]++, 0));
			vars.append("\thorn AT ").append(landOut.apply(sound[0], 0)).append(" : BOOL;\n");
			vars.append("\tred AT ").append(landOut.apply(sound[1], 0)).append(" : BOOL;\n");
			vars.append("\tamber AT ").append(landOut.apply(sound[1], 1)).append(" : BOOL;\n");
			vars.append("\tgreen AT ").append(landOut.apply(sound[1], 2)).append(" : BOOL;\n");
			StringBuilder code = new StringBuilder();
			String[] names = {"flotation", "belt2", "belt1", "crusher", "feeder"};
			for (int gi = 0; gi < groups.length; gi++) {
				for (int i = 0; i < groups[gi].length; i++) {
					PlantBlockEntity part = groups[gi][i];
					String name = names[gi] + (groups[gi].length > 1 ? "_" + i : "");
					String address = landOut.apply(part.getBlockPos(), 0);
					runs.put(part.getBlockPos(), address);
					vars.append("\t").append(name).append(" AT ").append(address).append(" : BOOL;\n");
					code.append(name).append(" := seq > ").append(gi + 1).append(";\n");
					auto(level, part.getBlockPos());
				}
			}
			vars.append("\tstart : BOOL;\n\tstop : BOOL;\n\tseq : INT;\n\tpt : TIME;\n\tstep : TON;\nEND_VAR\n");
			int last = groups.length + 1;
			String body = "(* conveyor start-up: horn and amber, then the last machine first, each once the one after it runs *)\n"
					+ "IF stop THEN\n\tseq := 0;\n\tstop := FALSE;\nEND_IF;\n"
					+ "IF start AND seq = 0 THEN\n\tseq := 1;\nEND_IF;\nstart := FALSE;\n"
					+ "IF seq = 1 THEN\n\tpt := T#2s;\nELSE\n\tpt := T#1500ms;\nEND_IF;\n"
					+ "step(IN := seq > 0 AND seq < " + last + " AND NOT step.Q, PT := pt);\n"
					+ "IF step.Q THEN\n\tseq := seq + 1;\nEND_IF;\n"
					+ "horn := seq = 1;\namber := seq > 0 AND seq < " + last + ";\ngreen := seq >= " + last + ";\nred := seq = 0;\n"
					+ code + "END_PROGRAM\n";
			String text = vars + body;
			var errors = box.download(text);
			if (!errors.isEmpty()) {
				throw new AssertionError("the program should compile: " + errors + "\n" + text);
			}
			box.cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN);
			return text;
		});
		log("the line's program:\n" + program);
		String copper = dev.wildcatter.tailings.rock.Mineral.COPPER.mineralItemId();
		each = () -> server.runOnServer(srv -> {
			PlantBlockEntity source = (PlantBlockEntity) v[1];
			double short_ = 3000 - source.out().held(dev.wildcatter.fluid.Product.FINES_SLURRY);
			if (short_ > 0) {
				source.out().offer(dev.wildcatter.fluid.Product.FINES_SLURRY, short_, dev.wildcatter.fluid.SlurryAssay.of(Map.of(copper, 0.004)));
			}
			for (PlantBlockEntity cell : (PlantBlockEntity[]) v[2]) {
				cell.out().give(dev.wildcatter.fluid.Product.TAILS_SLURRY, Double.MAX_VALUE);
				cell.out().give(dev.wildcatter.fluid.Product.CONCENTRATE_SLURRY, Double.MAX_VALUE);
			}
		});
		Runnable say = () -> server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			StringBuilder line = new StringBuilder("line: seq " + ((PlcCabinetBlockEntity) level.getBlockEntity(cabinet)).cpu().read("LINE.seq"));
			for (PlantBlockEntity[] group : groups) {
				line.append("; ").append(group[0].machine().name()).append(" ").append(group[0].running() ? "running" : "stopped").append(" (").append(group[0].status()).append(")");
			}
			log(line.toString());
		});
		Runnable start = () -> server.runOnServer(srv -> ((PlcCabinetBlockEntity) srv.overworld().getBlockEntity(cabinet)).cpu().write("LINE.start", true));
		Runnable stop = () -> server.runOnServer(srv -> ((PlcCabinetBlockEntity) srv.overworld().getBlockEntity(cabinet)).cpu().write("LINE.stop", true));
		server.runCommand("time set 12600");
		Cam view = Cam.of(o, 15, 2, 53, 2, 9, 28);
		put(view);
		settle(100);
		log("cabinet: " + read("cabinet"));
		// a first start, then the truck tips and the line runs loaded
		start.run();
		ticks(200);
		say.run();
		server.runOnServer(srv -> ((UltraHaulTruckEntity) v[0]).testDrive(new Input(false, false, false, false, true, false, false)));
		ticks(10);
		server.runOnServer(srv -> ((UltraHaulTruckEntity) v[0]).testDrive(Input.EMPTY));
		for (int i = 0; i < 4; i++) {
			ticks(200);
			say.run();
		}
		log("cabinet: " + read("cabinet"));
		if (wanted(1)) {
			// the line stopped where it stood, loaded; then the start-up, filmed
			stop.run();
			ticks(40);
			say.run();
			shot("01_belts", Cam.of(o, 13.6, 1.4, 48.5, 12.4, 3.2, 45), Cam.of(o, 3.5, 15, 54, 6, 3, 18), 210, Map.of(10, start, 100, say, 200, say));
			ticks(100);
		}
		if (wanted(2)) {
			server.runCommand("time set 12600");
			// low behind the cabinet and its lamps, the flotation row and the gyratory beyond; a slow dolly east
			shot("02_plant", Cam.of(o, 10, 1.2, 52, 24, 6, 20), Cam.of(o, 15, 1.4, 51, 26, 6, 18), 180, Map.of(90, say));
		}
		each = null;
		runs.clear();
	}

	// ---- 3: the TBM on its junction's GO ----

	private void tbmSite() {
		BlockPos[] at = new BlockPos[2];
		BlockPos[] devices = new BlockPos[2];
		server.runOnServer(srv -> {
			ServerLevel level = srv.overworld();
			BlockPos s = Trailer.site(level, 400, -200, 90, 120);
			int g = s.getY();
			BlockPos c = new BlockPos(s.getX() + 40, g, s.getZ() + 80);
			Trailer.pad(level, c.getX() - 40, c.getZ() - 80, c.getX() + 50, c.getZ() + 45, g, 14, 40);
			Trailer.hill(level, c.getX() - 40, c.getZ() - 80, c.getX() + 6, c.getZ() - 1, g, (x, z) -> {
				int fromSouth = c.getZ() - 1 - z;
				int fromWest = x - (c.getX() - 40);
				int fromNorth = z - (c.getZ() - 80);
				int h = Math.min(16, Math.min(6 + fromSouth * 3, Math.min(fromWest, fromNorth)));
				if (x >= c.getX() - 2) {
					h = Math.max(h, Math.min(12, 4 + fromSouth * 4));
				}
				return h;
			});
			for (int x = c.getX() + 7; x <= c.getX() + 30; x++) {
				for (int z = c.getZ() - 70; z <= c.getZ() + 2; z++) {
					level.setBlock(new BlockPos(x, g - 1, z), Blocks.GRAVEL.defaultBlockState(), Block.UPDATE_CLIENTS);
				}
			}
			Trailer.tbm(level, c, 30);
			// the cabinet on the cut's bench, its door and lamps to the east
			cabinet = c.offset(14, 0, -2);
			Set<BlockPos> laid = new HashSet<>();
			BlockPos box = rig(level, cabinet, Direction.SOUTH, Direction.EAST, devices, laid);
			BlockPos junction = c.offset(-2, 0, 14);
			Set<BlockPos> net = new HashSet<>(List.of(box, box.above()));
			route(level, junction, net, laid, g, c.offset(-20, -8, -40), c.offset(40, 20, 40));
			joints(level, laid);
			log("staged: " + laid.size() + " cells of control cable to the junction");
			checkNet(level, cabinet, List.of(junction, devices[0], devices[1]));
			at[0] = c;
			at[1] = junction;
		});
		BlockPos c = at[0];
		BlockPos junction = at[1];
		String program = server.computeOnServer(srv -> {
			ServerLevel level = srv.overworld();
			var box = (PlcCabinetBlockEntity) level.getBlockEntity(cabinet);
			box.seat(PlcModuleItem.Type.CPU_SMALL);
			box.seat(PlcModuleItem.Type.DO16);
			box.seat(PlcModuleItem.Type.DI16);
			log("junction points: " + ControlNet.deviceAt(level, junction).points());
			String text = "PROGRAM DRIVE\nVAR\n"
					+ "\tgo AT " + land(box, junction, 0, "%QX0.0") + " : BOOL;\n"
					+ "\thorn AT " + land(box, devices[0], 0, "%QX0.1") + " : BOOL;\n"
					+ "\tred AT " + land(box, devices[1], 0, "%QX0.2") + " : BOOL;\n"
					+ "\tamber AT " + land(box, devices[1], 1, "%QX0.3") + " : BOOL;\n"
					+ "\tgreen AT " + land(box, devices[1], 2, "%QX0.4") + " : BOOL;\n"
					+ "\trunning AT " + land(box, junction, 1, "%IX16.0") + " : BOOL;\n"
					+ "\tstart : BOOL;\n\twarn : TON;\nEND_VAR\n"
					+ "(* the horn before the machine moves, then GO; green while it bores *)\n"
					+ "warn(IN := start, PT := T#2s);\n"
					+ "horn := start AND NOT warn.Q;\n"
					+ "go := warn.Q;\n"
					+ "amber := start AND NOT running;\n"
					+ "green := running;\n"
					+ "red := NOT start;\n"
					+ "END_PROGRAM\n";
			auto(level, junction);
			var errors = box.download(text);
			if (!errors.isEmpty()) {
				throw new AssertionError("the program should compile: " + errors + "\n" + text);
			}
			box.cpu().setMode(dev.wildcatter.plc.Cpu.Mode.RUN);
			return text;
		});
		log("the drive's program:\n" + program);
		server.runCommand("time set 12600");
		put(Cam.of(c, 26, 8, 6, 3, 3, -6));
		settle(80);
		log("cabinet: " + read("cabinet"));
		server.runOnServer(srv -> ((PlcCabinetBlockEntity) srv.overworld().getBlockEntity(cabinet)).cpu().write("DRIVE.start", true));
		each = () -> server.runOnServer(srv -> {
			Trailer.feedTbm(srv.overworld(), c);
			Trailer.cutAway(srv.overworld(), c);
		});
		double front = c.getZ();
		for (int s = 0; s < 150 && c.getZ() - front < 9; s++) {
			ticks(20);
			front = server.computeOnServer(srv -> Trailer.tbmFront(srv.overworld(), c));
			if (s % 10 == 0) {
				server.runOnServer(srv -> {
					var cpu = ((PlcCabinetBlockEntity) srv.overworld().getBlockEntity(cabinet)).cpu();
					log("drive: go " + cpu.read("DRIVE.go") + ", running " + cpu.read("DRIVE.running"));
					for (TbmEntity machine : srv.overworld().getEntitiesOfClass(TbmEntity.class, new AABB(c).inflate(90))) {
						log("tbm at " + machine.position() + ": " + machine.core().readout());
					}
					if (srv.overworld().getBlockEntity(c.offset(3, 3, 3)) instanceof DrillBlockEntity cab) {
						log("cab standing: " + cab.core().readout());
					}
				});
			}
		}
		int f = (int) Math.round(front) - c.getZ();
		log("tbm: front at " + f + "; cabinet: " + read("cabinet"));
		// over the cabinet and its green lamp on the bench, toward the machine boring into the face
		shot("03_tbm", Cam.of(c, 21, 1.8, 3, 2, 5, -5), Cam.of(c, 21, 2.8, -2, 2, 4.5, -9), 180, Map.of());
		each = null;
	}
}
