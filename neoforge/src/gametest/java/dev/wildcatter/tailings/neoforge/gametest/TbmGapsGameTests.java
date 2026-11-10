package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.CableJunctionBlockEntity;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.DrillMenu;
import dev.wildcatter.tailings.block.MineRailBlock;
import dev.wildcatter.tailings.block.RailBlockEntity;
import dev.wildcatter.tailings.block.RectifierBlockEntity;
import dev.wildcatter.tailings.entity.GantryEntity;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.mine.DrillCodes;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.mine.TbmGantry;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Server scenes for the TBM's gaps closed while Tim was away (docs/questions/tbm-gaps.md): gantries on their own wheels, every car of
 * a train loaded, the quench tank on the pump gantry, the cab's screen across the lift, the reel gantry's transformer on the circuit,
 * the real recipes, the warning for track too long to solve, and busway straight onto the rectifier.
 */
final class TbmGapsGameTests {
	static final String[] SCENES = {"tbm_gantry_derail", "tbm_train_fills", "tbm_quench_gantry", "tbm_screen_follows", "tbm_transformer",
			"tbm_recipes", "track_too_long", "rectifier_busway"};
	static final Set<String> LONG = Set.of("tbm_train_fills", "tbm_transformer", "rectifier_busway", "track_too_long");
	private static final Map<String, Integer> PADDING = Map.of("tbm_train_fills", 110);

	private TbmGapsGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("tbm_gantry_derail", TbmGapsGameTests::gantryDerail);
		out.accept("tbm_train_fills", TbmGapsGameTests::trainFills);
		out.accept("tbm_quench_gantry", TbmGapsGameTests::quenchGantry);
		out.accept("tbm_screen_follows", TbmGapsGameTests::screenFollows);
		out.accept("tbm_transformer", TbmGapsGameTests::transformer);
		out.accept("tbm_recipes", TbmGapsGameTests::recipes);
		out.accept("track_too_long", TbmGapsGameTests::trackTooLong);
		out.accept("rectifier_busway", TbmGapsGameTests::rectifierBusway);
	}

	static int padding(String scene, int otherwise) {
		return PADDING.getOrDefault(scene, otherwise);
	}

	// ---- the gantries ride the track ----------------------------------------------------------------------------------------------

	/**
	 * Each gantry runs on its own wheels on the twin track; a rail taken from under the conveyor gantry takes it off the track, and the
	 * machine stops, naming it; the rail put back, the gantry goes back on and the machine goes on.
	 */
	private static void gantryDerail(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = TailingsGameTests.tbm(helper, c, 10, true);
		TailingsGameTests.feed(helper, c);
		TailingsGameTests.go(cab, 10);
		BlockPos[] taken = {null};
		BlockState[] was = {null};
		int[] at = {0};
		helper.runAfterDelay(25, () -> {
			TbmEntity machine = TailingsGameTests.machine(helper, c);
			helper.assertTrue(machine != null && machine.core().drill().advanceCount() >= 2, Component.literal("it should be driving"));
			for (TbmGantry kind : TbmGantry.values()) {
				helper.assertTrue(TailingsGameTests.gantry(helper, machine, kind).onTrack(), Component.literal(kind.displayName() + " should be on its wheels"));
			}
			GantryEntity conveyor = TailingsGameTests.gantry(helper, machine, TbmGantry.CONVEYOR);
			// its right rail, half way along it
			taken[0] = BlockPos.containing(conveyor.getX() + TbmGantry.RIGHT_RAIL + 0.5, conveyor.getY() + 0.5, conveyor.getZ() + 4.5);
			was[0] = helper.getLevel().getBlockState(taken[0]);
			helper.assertTrue(was[0].getBlock() == TailingsBlocks.MINE_RAIL, Component.literal("a rail under the conveyor gantry, got " + was[0]));
			helper.getLevel().setBlock(taken[0], Blocks.AIR.defaultBlockState(), 3);
		});
		helper.runAfterDelay(55, () -> {
			TbmEntity machine = TailingsGameTests.machine(helper, c);
			helper.assertTrue(!TailingsGameTests.gantry(helper, machine, TbmGantry.CONVEYOR).onTrack(), Component.literal("the conveyor gantry should be off the track"));
			helper.assertTrue(machine.core().status() == DrillCodes.NOT_BUILT && machine.core().note().contains("track under the Conveyor Gantry"),
					Component.literal("the machine should name the track under the conveyor gantry, got " + machine.core().note()));
			at[0] = machine.core().drill().advanceCount();
		});
		helper.runAfterDelay(80, () -> {
			TbmEntity machine = TailingsGameTests.machine(helper, c);
			helper.assertTrue(machine.core().drill().advanceCount() == at[0], Component.literal("and not cut"));
			helper.getLevel().setBlock(taken[0], was[0], 3);
			helper.succeedWhen(() -> {
				helper.assertTrue(TailingsGameTests.gantry(helper, machine, TbmGantry.CONVEYOR).onTrack(), Component.literal("back on its wheels"));
				helper.assertTrue(machine.core().drill().advanceCount() > at[0] && machine.core().status() == DrillCodes.DRIVING,
						Component.literal("and the machine goes on, at " + machine.core().readout()));
			});
		});
	}

	// ---- every car loads ----------------------------------------------------------------------------------------------------------

	/**
	 * A six-car train whose first five cars are full: its last car, further back than the old loading point reached with the train at
	 * the conveyor, still loads, as the train indexes forward through the loading point.
	 */
	private static void trainFills(GameTestHelper helper) {
		ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
		for (int dx = -1; dx <= 2; dx++) {
			for (int dz = -1; dz <= 7; dz++) {
				helper.getLevel().setChunkForced(origin.x() + dx, origin.z() + dz, true);
			}
		}
		BlockPos c = new BlockPos(4, 40, 5);
		DrillBlockEntity cab = TailingsGameTests.tbm(helper, c, 4, true);
		BlockState rail = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = 11; z <= 84; z++) {
			helper.setBlock(c.offset(2, 1, z), rail);
		}
		TailingsGameTests.part(helper, TailingsBlocks.TRACTION_RECTIFIER, c.offset(0, 1, 80), RectifierBlockEntity.class).forceRunning();
		TailingsGameTests.go(cab, 200);
		helper.runAfterDelay(20, () -> {
			MineVehicleEntity loco = TailingsGameTests.loco(helper, c.offset(2, 1, 60), Heading.NORTH);
			MineVehicleEntity[] cars = new MineVehicleEntity[6];
			int[] at = {63, 65, 68, 70, 73, 75};
			for (int i = 0; i < cars.length; i++) {
				cars[i] = TailingsGameTests.car(helper, c.offset(2, 1, at[i]), Heading.NORTH);
				if (i < cars.length - 1) {
					cars[i].load().add("minecraft:cobblestone", Haul.CAR_ITEMS);
					cars[i].refreshLoad();
				}
			}
			helper.succeedWhen(() -> {
				helper.assertTrue(loco.carIds().size() == 6, Component.literal("six cars coupled, got " + loco.carIds().size()));
				helper.assertTrue(cars[5].load().total() > 0, Component.literal("the last car should load too, train at z " + (loco.getZ() - helper.absolutePos(c).getZ())
						+ " (" + loco.status() + "), the old reach ended at row " + (TbmGantry.LOADING_ROW + 13)));
			});
		});
	}

	// ---- the quench tank on the pump gantry ---------------------------------------------------------------------------------------

	/** A bucket in the cab's water slot fills the pump gantry's tank; the pump gantry broken, its water goes with it. */
	private static void quenchGantry(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = TailingsGameTests.tbm(helper, c, 2, true);
		cab.setItem(DrillBlockEntity.SLOT_WATER, new ItemStack(Items.WATER_BUCKET));
		helper.runAfterDelay(5, () -> {
			GantryEntity pump = TailingsGameTests.gantry(helper, cab, TbmGantry.PUMP);
			helper.assertTrue(pump.water() == 1 && cab.core().tank() == 1, Component.literal("the bucket should go to the pump gantry's tank, got " + pump.water()));
			helper.assertTrue(cab.getItem(DrillBlockEntity.SLOT_WATER).is(Items.BUCKET), Component.literal("and leave an empty bucket"));
			pump.setWater(40);
		});
		helper.runAfterDelay(8, () -> {
			helper.assertTrue(cab.core().tank() == 40, Component.literal("the cab reads the pump gantry's tank, got " + cab.core().tank()));
			TailingsGameTests.gantry(helper, cab, TbmGantry.PUMP).discard();
			cab.setItem(DrillBlockEntity.SLOT_WATER, new ItemStack(Items.WATER_BUCKET));
		});
		helper.runAfterDelay(10, () -> helper.succeedWhen(() -> {
			helper.assertTrue(cab.core().tank() == 0 && cab.core().gantry(TbmGantry.PUMP) == null, Component.literal("its water should go with it, got " + cab.core().tank()));
			helper.assertTrue(cab.getItem(DrillBlockEntity.SLOT_WATER).is(Items.WATER_BUCKET), Component.literal("and no tank takes a bucket"));
		}));
	}

	// ---- the cab's screen stays open ----------------------------------------------------------------------------------------------

	/** A screen open on the cab stays open as the machine lifts, now on the running machine, and back on the cab when it sets down. */
	private static void screenFollows(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = TailingsGameTests.tbm(helper, c, 4, true);
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos stand = helper.absolutePos(c.offset(3, 8, 8));
		player.setPos(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5);
		DrillMenu menu = new DrillMenu(7, player.getInventory(), cab);
		player.containerMenu = menu;
		TailingsGameTests.go(cab, 10);
		int[] phase = {0};
		helper.succeedWhen(() -> {
			if (phase[0] == 0) {
				TbmEntity machine = TailingsGameTests.machine(helper, c);
				helper.assertTrue(machine != null, Component.literal("it should lift"));
				helper.assertTrue(player.containerMenu == menu && menu.cab() == machine && menu.stillValid(player),
						Component.literal("the screen should now be on the running machine, on " + menu.cab()));
				helper.assertTrue(menu.getSlot(DrillBlockEntity.SLOT_RAIL).getItem().is(TailingsBlocks.MINE_RAIL.asItem()), Component.literal("showing its slots"));
				machine.core().setRunning(false);
				phase[0] = 1;
			}
			helper.assertTrue(TailingsGameTests.machine(helper, c) == null, Component.literal("it should set down"));
			helper.assertTrue(menu.cab() instanceof DrillBlockEntity && menu.stillValid(player), Component.literal("and the screen go back to the cab, on " + menu.cab()));
		});
	}

	// ---- the reel gantry's transformer --------------------------------------------------------------------------------------------

	/** A 500 kW diesel set at 480 V with its panel at {@code panel} (facing north, its skid to the west), fuelled and started. */
	static GensetControlPanelBlockEntity genset(GameTestHelper helper, BlockPos panel) {
		for (int x = -7; x <= 18; x++) {
			for (int z = -4; z <= 8; z++) {
				helper.setBlock(panel.offset(x, -1, z), Blocks.STONE);
			}
		}
		GensetSkid.build(helper.getLevel(), helper.absolutePos(panel), Direction.NORTH, false);
		return (GensetControlPanelBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(panel));
	}

	static void start(GameTestHelper helper, BlockPos panel) {
		helper.runAfterDelay(30, () -> {
			if (helper.getLevel().getBlockEntity(GensetSkid.base(helper.absolutePos(panel), Direction.NORTH, false)) instanceof GensetPartBlockEntity tank) {
				tank.setFuel(2000);
			}
			((GensetControlPanelBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(panel))).setWantRun(true);
		});
	}

	static void put(GameTestHelper helper, String id, BlockPos at) {
		helper.setBlock(at, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", id)));
	}

	/**
	 * The junction fed at 4.16 kV from a real set through a unit substation run backward: the pump and conveyor's 82.5 kW flows through
	 * the reel gantry's transformer, at about 480 V on its secondary and the turns ratio's current on its primary.
	 */
	private static void transformer(GameTestHelper helper) {
		BlockPos panel = new BlockPos(8, 2, 3);
		GensetControlPanelBlockEntity set = genset(helper, panel);
		Block sub = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "unit_substation"));
		BlockState state = sub.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH);
		BlockPos main = panel.east(4);
		helper.setBlock(main, state);
		BigParts.grow(helper.getLevel(), helper.absolutePos(main), helper.getLevel().getBlockState(helper.absolutePos(main)));
		put(helper, "cable_mv", panel.east(5));
		put(helper, "cable_mv", panel.east(6));
		helper.setBlock(panel.east(7), TailingsBlocks.CABLE_JUNCTION);
		CableJunctionBlockEntity junction = helper.getBlockEntity(panel.east(7), CableJunctionBlockEntity.class);
		start(helper, panel);
		double aux = dev.wildcatter.tailings.block.TbmCore.PUMP_WATTS + dev.wildcatter.tailings.block.TbmCore.CONVEYOR_WATTS;
		helper.onEachTick(() -> junction.draw(0, aux));
		helper.runAfterDelay(400, () -> helper.succeedWhen(() -> {
			helper.assertTrue(set.breakerClosed(), Component.literal("the set should be on with its breaker closed: " + set.inspect().getString()));
			helper.assertTrue(junction.live(), Component.literal("4.16 kV at the junction: " + junction.inspect().getString()));
			double lv = Voltage.INDUSTRIAL.lineToLine();
			helper.assertTrue(junction.auxLive() && Math.abs(junction.transformerVolts() - lv) < lv * 0.08,
					Component.literal("its secondary near " + Math.round(lv) + " V: " + junction.inspect().getString()));
			helper.assertTrue(Math.abs(junction.auxWatts() - aux) < aux * 0.1, Component.literal("82.5 kW through it: " + junction.inspect().getString()));
			double ratio = Voltage.MEDIUM.lineToLine() / Voltage.INDUSTRIAL.lineToLine();
			helper.assertTrue(junction.auxAmps() > 80 && Math.abs(junction.transformerPrimaryAmps() * ratio - junction.auxAmps()) < junction.auxAmps() * 0.05,
					Component.literal("the secondary's amps over the turns ratio on its primary: " + junction.transformerPrimaryAmps() + " A and " + junction.auxAmps() + " A"));
		}));
	}

	// ---- recipes --------------------------------------------------------------------------------------------------------------------

	/** Every part of the machine and its gantries has a real recipe from Wildcatter's parts: steel, windings, cable, its drive. */
	private static void recipes(GameTestHelper helper) {
		var recipes = helper.getLevel().getServer().getRecipeManager();
		for (String part : List.of("cutterhead", "segment_erector", "main_beam", "track_layer", "cable_junction", "pump_gantry", "reel_gantry", "conveyor_gantry")) {
			var found = recipes.byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, Tailings.id(part)));
			helper.assertTrue(found.isPresent(), Component.literal("a recipe for " + part));
			var recipe = (net.minecraft.world.item.crafting.ShapedRecipe) found.get().value();
			boolean steel = false;
			for (var ingredient : recipe.getIngredients()) {
				steel |= ingredient.isPresent() && ingredient.get().test(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("wildcatter", "steel_ingot"))));
			}
			helper.assertTrue(steel, Component.literal(part + " should be steel, not the old iron placeholder"));
		}
		helper.succeed();
	}

	// ---- track too long to solve --------------------------------------------------------------------------------------------------

	/** More than Wildcatter's 4,000 nodes of rail on one rectifier: the track is dead, and the rectifier and a train say why. */
	private static void trackTooLong(GameTestHelper helper) {
		BlockState rail = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		// a block of joined rail, 16 x 16 x 16: every rail is a node, and they touch on every face
		for (int x = 0; x < 16; x++) {
			for (int y = 0; y < 16; y++) {
				for (int z = 0; z < 16; z++) {
					helper.getLevel().setBlock(helper.absolutePos(new BlockPos(4 + x, 2 + y, 4 + z)), rail, Block.UPDATE_CLIENTS);
				}
			}
		}
		RectifierBlockEntity box = TailingsGameTests.part(helper, TailingsBlocks.TRACTION_RECTIFIER, new BlockPos(2, 2, 4), RectifierBlockEntity.class);
		box.forceRunning();
		helper.succeedWhen(() -> {
			RailBlockEntity some = helper.getBlockEntity(new BlockPos(10, 17, 10), RailBlockEntity.class);
			helper.assertTrue(box.tooBig() && some.tooBig(), Component.literal("the circuit should be too big to solve"));
			helper.assertTrue(!some.conductorLive(), Component.literal("and the track dead"));
			helper.assertTrue(box.inspect().getString().contains("TRACK TOO LONG"), Component.literal("the rectifier should say so: " + box.inspect().getString()));
		});
	}

	// ---- busway straight onto the rectifier ---------------------------------------------------------------------------------------

	/**
	 * The R10 fault, re-tested on Wildcatter 0.5.1: a 500 kW set with 480 V busway run straight onto the traction rectifier's motor box
	 * closes its breaker, the rectifier feeds its track, and a train's 74 kW on the track comes through.
	 */
	private static void rectifierBusway(GameTestHelper helper) {
		BlockPos panel = new BlockPos(8, 2, 3);
		GensetControlPanelBlockEntity set = genset(helper, panel);
		for (int i = 1; i <= 6; i++) {
			put(helper, "busway_industrial", panel.east(i));
		}
		RectifierBlockEntity box = TailingsGameTests.part(helper, TailingsBlocks.TRACTION_RECTIFIER, panel.east(7), RectifierBlockEntity.class);
		BlockState rail = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = -2; z <= 6; z++) {
			helper.setBlock(new BlockPos(panel.getX() + 9, panel.getY(), z + panel.getZ()), rail);
		}
		start(helper, panel);
		BlockPos railAt = new BlockPos(panel.getX() + 9, panel.getY(), panel.getZ() + 4);
		long from = helper.getLevel().getGameTime();
		helper.onEachTick(() -> {
			if (helper.getLevel().getGameTime() - from > 330) {
				helper.getBlockEntity(railAt, RailBlockEntity.class).draw(Haul.TRACTION_WATTS);
			}
		});
		helper.runAfterDelay(500, () -> helper.succeedWhen(() -> {
			helper.assertTrue(set.run() == GensetControlPanelBlockEntity.Run.RUNNING && set.breakerClosed(),
					Component.literal("the set should close its breaker onto the busway: " + set.inspect().getString()));
			helper.assertTrue(box.powered() && box.amps() > 150, Component.literal("the rectifier fed and carrying the train: " + box.inspect().getString()));
			helper.assertTrue(helper.getBlockEntity(railAt, RailBlockEntity.class).conductorLive(), Component.literal("and its track live"));
		}));
	}
}
