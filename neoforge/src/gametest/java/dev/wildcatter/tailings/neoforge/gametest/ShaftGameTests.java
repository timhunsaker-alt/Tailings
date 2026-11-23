package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.MineRailBlock;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.RectifierBlockEntity;
import dev.wildcatter.tailings.block.ShaftLayout;
import dev.wildcatter.tailings.block.stage.Shaft;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.entity.ShaftSkipEntity;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.shaft.Hoisting;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server scenes for the shaft and hoist (docs/multiblocks/shaft-and-hoist.md, L4; decided on recommendation while Tim was away,
 * docs/questions/l4-shaft.md): the headframe over its shaft, the hoist house behind it, the measuring flask at the bottom fed by the
 * bottom-dump cars' pocket (L3), and the skips in balance between them.
 */
final class ShaftGameTests {
	static final String[] SCENES = {"shaft_hoist", "shaft_not_built", "shaft_flask_feed", "shaft_bin_full", "shaft_power", "shaft_cage", "shaft_raise_bore", "shaft_cage_button"};
	static final Set<String> LONG = Set.of("shaft_hoist", "shaft_not_built", "shaft_flask_feed", "shaft_bin_full", "shaft_power", "shaft_cage", "shaft_raise_bore",
			"shaft_cage_button");
	private static final Map<String, Integer> TICKS = Map.of("shaft_hoist", 3400);
	/** The scenes' collar: the headframe's main cell, its shaft going down from here. */
	private static final int COLLAR = 155;

	private ShaftGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("shaft_hoist", ShaftGameTests::hoist);
		out.accept("shaft_not_built", ShaftGameTests::notBuilt);
		out.accept("shaft_flask_feed", ShaftGameTests::flaskFeed);
		out.accept("shaft_bin_full", ShaftGameTests::binFull);
		out.accept("shaft_power", ShaftGameTests::power);
		out.accept("shaft_cage", ShaftGameTests::cage);
		out.accept("shaft_raise_bore", ShaftGameTests::raiseBore);
		out.accept("shaft_cage_button", ShaftGameTests::cageButton);
	}

	static int ticks(String scene, int otherwise) {
		return TICKS.getOrDefault(scene, otherwise);
	}

	// ---- building ---------------------------------------------------------------------------------------------------------------

	/** A part facing north, its cells grown as a player's would be. */
	private static PlantBlockEntity part(GameTestHelper helper, Machine machine, BlockPos pos) {
		return TailingsGameTests.part(helper, TailingsBlocks.plantBlock(machine), pos, PlantBlockEntity.class);
	}

	/** The pieces of one shaft. */
	record Built(PlantBlockEntity headframe, PlantBlockEntity hoist, PlantBlockEntity bin, PlantBlockEntity flask) {
	}

	/**
	 * A shaft {@code depth} m deep (collar to the flask's bottom): the headframe at (0, COLLAR, 0) over the shaft at x and z 2 to 6, its
	 * bin touching its north side, the hoist house behind it to the south (z 20 to 31), and the flask beside the shaft bottom at x 7.
	 */
	static Built build(GameTestHelper helper, int depth) {
		PlantBlockEntity headframe = part(helper, Machine.HEADFRAME, new BlockPos(0, COLLAR, 0));
		PlantBlockEntity bin = part(helper, Machine.HEADFRAME_BIN, new BlockPos(2, COLLAR, -4));
		PlantBlockEntity hoist = part(helper, Machine.HOIST_HOUSE, new BlockPos(0, COLLAR, 20));
		PlantBlockEntity flask = part(helper, Machine.MEASURING_FLASK, new BlockPos(7, COLLAR - depth, 4));
		hoist.forceRunning();
		return new Built(headframe, hoist, bin, flask);
	}

	static ShaftSkipEntity hang(GameTestHelper helper, PlantBlockEntity headframe) {
		ShaftSkipEntity skip = ShaftSkipEntity.hang(helper.getLevel(), ShaftLayout.Frame.of(headframe));
		helper.assertTrue(skip != null, Component.literal("a skip should hang in the headframe"));
		return skip;
	}

	/** Keeps the flask's surge topped up, as a feeder from a pocket would. */
	static void feed(GameTestHelper helper, PlantBlockEntity flask) {
		helper.onEachTick(() -> {
			if (flask.made().isEmpty()) {
				flask.made().add(Rock.GRANITE, null, 2700);
			}
		});
	}

	// ---- the scenes -----------------------------------------------------------------------------------------------------------

	/**
	 * Two skips in balance lift granite 150 m (168 m of wind to the dump) into the headframe bin (shaft_hoist): each skip dumps 3 t, and
	 * the time between dumps is the wind and the rest, about 40 s, so about 270 t/h.
	 */
	private static void hoist(GameTestHelper helper) {
		Built b = build(helper, 150);
		ShaftSkipEntity west = hang(helper, b.headframe());
		ShaftSkipEntity east = hang(helper, b.headframe());
		feed(helper, b.flask());
		long[] dumped = new long[4];
		int[] seen = {0};
		double[] fastest = {0};
		boolean[] wound = {false};
		helper.onEachTick(() -> {
			wound[0] |= b.hoist().loop() == dev.wildcatter.tailings.TailingsSounds.MINE_HOIST;
			int dumps = b.hoist().winder().dumps();
			if (dumps > seen[0] && dumps <= 3) {
				dumped[dumps] = helper.getTick();
			}
			seen[0] = dumps;
			fastest[0] = Math.max(fastest[0], b.hoist().winder().speed());
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(seen[0] >= 3, Component.literal("three skips should dump, got " + seen[0] + " (" + b.hoist().status() + ")"));
			double travel = b.hoist().winder().travel();
			helper.assertTrue(Math.abs(travel - (150 + Hoisting.DUMP_HEIGHT)) < 1e-6, Component.literal("it winds 168 m, got " + travel));
			double cycle = (dumped[3] - dumped[1]) / 2.0 / 20.0;
			double expected = Hoisting.cycleSeconds(travel);
			helper.assertTrue(Math.abs(cycle - expected) < 0.05 * expected,
					Component.literal(String.format("a cycle should take about %.1f s, took %.1f s", expected, cycle)));
			double tph = Hoisting.SKIP_KG / 1000 * 3600 / cycle;
			dev.wildcatter.tailings.Tailings.LOGGER.info("shaft_hoist: {} m, a cycle of {} s (expected {} s), {} t/h", travel, cycle, expected, tph);
			helper.assertTrue(tph > 225, Component.literal(String.format("at least the spec's 225 t/h, got %.0f", tph)));
			helper.assertTrue(Math.abs(fastest[0] - Hoisting.ROPE_SPEED) < 1e-6, Component.literal("full rope speed: " + fastest[0]));
			helper.assertTrue(wound[0], Component.literal("winding, the hoist sounds its own loop"));
			helper.assertTrue(b.bin().hold().kg(Rock.GRANITE, null) >= 3 * Hoisting.SKIP_KG - 1, Component.literal("the bin holds the granite: " + b.bin().hold().total()));
			// in balance: one skip's height above the loading stop and the other's add up to the wind
			double ws = west.getY() - (helper.absolutePos(BlockPos.ZERO).getY() + COLLAR - 150), es = east.getY() - (helper.absolutePos(BlockPos.ZERO).getY() + COLLAR - 150);
			helper.assertTrue(Math.abs(ws + es - travel) < 1e-3, Component.literal("the skips in balance: " + ws + " + " + es));
		});
	}

	/**
	 * The cage (shaft_cage, heavy-iron-fixes): hung beside the skips at the collar, it takes a stack of materials on its deck and a
	 * rider; the rider's bell rings 5 s and it winds 40 m to the bottom landing at the man-riding 4 m/s, the skips standing; a call from
	 * the collar brings it back up empty.
	 */
	private static void cage(GameTestHelper helper) {
		Built b = build(helper, 40);
		hang(helper, b.headframe());
		hang(helper, b.headframe());
		ShaftSkipEntity cage = ShaftSkipEntity.hangCage(helper.getLevel(), ShaftLayout.Frame.of(b.headframe()));
		helper.assertTrue(cage != null && cage.cage(), Component.literal("the cage should hang beside the skips"));
		helper.assertTrue(ShaftSkipEntity.hangCage(helper.getLevel(), ShaftLayout.Frame.of(b.headframe())) == null, Component.literal("one cage a headframe"));
		int collarY = helper.absolutePos(BlockPos.ZERO).getY() + COLLAR;
		net.minecraft.world.entity.player.Player rider = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
		rider.setPos(cage.getX() + 1, cage.getY() + 0.1, cage.getZ() + 1);
		rider.setShiftKeyDown(true);
		rider.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RAIL, 64));
		cage.interact(rider, net.minecraft.world.InteractionHand.MAIN_HAND, cage.position());
		helper.assertTrue(cage.deck().size() == 1 && cage.loaded(), Component.literal("a stack of rail on its deck"));
		rider.setShiftKeyDown(false);
		cage.interact(rider, net.minecraft.world.InteractionHand.MAIN_HAND, cage.position());
		helper.assertTrue(cage.hasPassenger(rider), Component.literal("the rider boards"));
		long[] start = {helper.getTick()}, arrived = {-1};
		double[] fastest = {0};
		helper.onEachTick(() -> {
			fastest[0] = Math.max(fastest[0], b.hoist().winder().cageSpeed());
			if (arrived[0] < 0 && b.hoist().winder().cageTrips() == 1) {
				arrived[0] = helper.getTick();
			}
		});
		double trip = Hoisting.CAGE_BELL_SECONDS + Hoisting.cageSeconds(40);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(arrived[0] >= 0, Component.literal("the cage should reach the bottom: " + b.hoist().status())))
				.thenExecute(() -> {
					double took = (arrived[0] - start[0]) / 20.0;
					helper.assertTrue(Math.abs(took - trip) < 1.0, Component.literal(String.format("the bell and the trip should take about %.1f s, took %.1f s", trip, took)));
					helper.assertTrue(Math.abs(fastest[0] - Hoisting.CAGE_SPEED) < 1e-6, Component.literal("at the man-riding 4 m/s: " + fastest[0]));
					helper.assertTrue(Math.abs(cage.getY() - (collarY - 40)) < 1e-3, Component.literal("its floor at the bottom landing: " + cage.getY()));
					helper.assertTrue(cage.hasPassenger(rider) && cage.deck().size() == 1, Component.literal("with its rider and its rail"));
					helper.assertTrue(b.hoist().winder().dumps() == 0 && !b.hoist().winder().winding(), Component.literal("the skips stood"));
					rider.stopRiding();
					cage.call(true);
				})
				.thenWaitUntil(() -> helper.assertTrue(b.hoist().winder().cageTrips() == 2 && Math.abs(cage.getY() - collarY) < 1e-3,
						Component.literal("called from the collar, it comes up: " + cage.getY() + "; " + b.hoist().status())))
				.thenSucceed();
	}

	/**
	 * The landings' call buttons (shaft_cage_button, films-hi07): a stone button on a block beside the measuring flask calls the cage
	 * down to the bottom landing, one beside the headframe at the collar calls it back up.
	 */
	private static void cageButton(GameTestHelper helper) {
		Built b = build(helper, 60);
		hang(helper, b.headframe());
		hang(helper, b.headframe());
		ShaftSkipEntity cage = ShaftSkipEntity.hangCage(helper.getLevel(), ShaftLayout.Frame.of(b.headframe()));
		helper.assertTrue(cage != null, Component.literal("the cage should hang"));
		int collarY = helper.absolutePos(BlockPos.ZERO).getY() + COLLAR;
		// 60 m: deeper than the 40 m a call from the collar once reached
		BlockPos bottomButton = new BlockPos(10, COLLAR - 59, 4), collarButton = new BlockPos(4, COLLAR + 1, 10);
		helper.setBlock(new BlockPos(9, COLLAR - 59, 4), net.minecraft.world.level.block.Blocks.STONE_BRICKS);
		helper.setBlock(bottomButton, net.minecraft.world.level.block.Blocks.STONE_BUTTON.defaultBlockState()
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL)
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.EAST));
		helper.setBlock(new BlockPos(4, COLLAR + 1, 9), net.minecraft.world.level.block.Blocks.STONE_BRICKS);
		helper.setBlock(collarButton, net.minecraft.world.level.block.Blocks.STONE_BUTTON.defaultBlockState()
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.ATTACH_FACE, net.minecraft.world.level.block.state.properties.AttachFace.WALL)
				.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
		Runnable[] press = new Runnable[2];
		BlockPos[] buttons = {bottomButton, collarButton};
		for (int i = 0; i < 2; i++) {
			BlockPos at = helper.absolutePos(buttons[i]);
			press[i] = () -> {
				net.minecraft.world.level.block.state.BlockState s = helper.getLevel().getBlockState(at);
				((net.minecraft.world.level.block.ButtonBlock) s.getBlock()).press(s, helper.getLevel(), at, null);
			};
		}
		helper.startSequence()
				.thenIdle(20)
				.thenExecute(press[0])
				.thenWaitUntil(() -> helper.assertTrue(b.hoist().winder().cageTrips() == 1 && Math.abs(cage.getY() - (collarY - 60)) < 1e-3,
						Component.literal("the bottom landing's button brings it down: " + cage.getY() + "; " + b.hoist().status())))
				.thenIdle(40)
				.thenExecute(press[1])
				.thenWaitUntil(() -> helper.assertTrue(b.hoist().winder().cageTrips() == 2 && Math.abs(cage.getY() - collarY) < 1e-3,
						Component.literal("the collar's button brings it up: " + cage.getY() + "; " + b.hoist().status())))
				.thenSucceed();
	}

	/**
	 * The raise borer (shaft_raise_bore, L4b): on a pad over a drive 20 m down, it drills its pilot and reams a 5 x 5 raise from the
	 * drive's roof to under the collar slab, its rock falling into the drive as cobblestone; the walls and the slab stand. At 50 times
	 * the real rate (a pilot metre in 0.2 s, a reamed metre in 1.2 s).
	 */
	private static void raiseBore(GameTestHelper helper) {
		int top = COLLAR - 1, driveY = COLLAR - 20;
		for (int x = -3; x <= 8; x++) {
			for (int z = -3; z <= 9; z++) {
				for (int y = COLLAR - 24; y <= top; y++) {
					boolean drive = y >= driveY - 2 && y <= driveY;
					helper.setBlock(new BlockPos(x, y, z), drive ? Blocks.AIR : Blocks.STONE);
				}
			}
		}
		PlantBlockEntity borer = part(helper, Machine.RAISE_BORER, new BlockPos(0, COLLAR, 0));
		helper.assertTrue(dev.wildcatter.tailings.block.stage.Shaft.findDrive(borer) == 20, Component.literal("the drive 20 m down: " + dev.wildcatter.tailings.block.stage.Shaft.findDrive(borer)));
		borer.forceRunning();
		borer.fastCycle(50);
		helper.succeedWhen(() -> {
			var p = borer.raise();
			helper.assertTrue(p.phase() == dev.wildcatter.tailings.shaft.RaiseBoring.Phase.DONE, Component.literal("it should finish: " + borer.status()));
			helper.assertTrue(p.pilot() == 20 && p.reamed() == 18, Component.literal("a 20 m pilot and an 18 m ream: " + p.save()));
			int open = 0, wall = 0;
			for (int y = driveY + 1; y <= COLLAR - 2; y++) {
				for (int x = 0; x <= 4; x++) {
					for (int z = 1; z <= 5; z++) {
						open += helper.getBlockState(new BlockPos(x, y, z)).isAir() ? 1 : 0;
					}
				}
				wall += helper.getBlockState(new BlockPos(-1, y, 3)).is(Blocks.STONE) && helper.getBlockState(new BlockPos(5, y, 3)).is(Blocks.STONE) ? 1 : 0;
			}
			helper.assertTrue(open == 25 * 18, Component.literal("the 5 x 5 raise open from the drive's roof up: " + open + " of " + 25 * 18));
			helper.assertTrue(wall == 18, Component.literal("its walls stand"));
			helper.assertTrue(helper.getBlockState(new BlockPos(2, COLLAR - 1, 3)).is(Blocks.STONE), Component.literal("the collar slab under the borer stands"));
			int cobble = 0;
			for (var item : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
					new net.minecraft.world.phys.AABB(helper.absolutePos(new BlockPos(-3, driveY - 3, -3))).expandTowards(12, 5, 13))) {
				if (item.getItem().is(net.minecraft.world.item.Items.COBBLESTONE)) {
					cobble += item.getItem().getCount();
				}
			}
			helper.assertTrue(cobble == 25 * 18, Component.literal("its cuttings in the drive: " + cobble + " cobblestone"));
		});
	}

	/** A missing skip reads NOT BUILT, and with it hung the hoist winds (shaft_not_built); a missing flask is named too. */
	private static void notBuilt(GameTestHelper helper) {
		Built b = build(helper, 20);
		hang(helper, b.headframe());
		feed(helper, b.flask());
		helper.startSequence()
				.thenExecuteAfter(25, () -> helper.assertTrue(b.hoist().status().startsWith("NOT BUILT") && b.hoist().status().contains("skip"),
						Component.literal("one skip: NOT BUILT naming the skip, got " + b.hoist().status())))
				.thenExecute(() -> hang(helper, b.headframe()))
				.thenWaitUntil(() -> helper.assertTrue(b.hoist().winder().winding(), Component.literal("with both, it winds: " + b.hoist().status())))
				.thenExecute(() -> helper.setBlock(b.flask().getBlockPos().subtract(helper.absolutePos(BlockPos.ZERO)), Blocks.AIR.defaultBlockState()))
				.thenWaitUntil(() -> helper.assertTrue(b.hoist().status().contains("Measuring Flask") && b.hoist().winder().speed() == 0,
						Component.literal("the flask gone: NOT BUILT naming it, got " + b.hoist().status())))
				.thenExecute(() -> part(helper, Machine.MEASURING_FLASK, new BlockPos(7, COLLAR - 20, 4)))
				.thenExecute(() -> helper.setBlock(new BlockPos(5, COLLAR - 10, 3), Blocks.STONE.defaultBlockState()))
				.thenWaitUntil(() -> helper.assertTrue(b.hoist().status().contains("blocked"), Component.literal("a block in a compartment: blocked, got " + b.hoist().status())))
				.thenSucceed();
	}

	/**
	 * The bottom-dump cars (L3) feed the flask (shaft_flask_feed): a car drops its load over a track grizzly into the pocket, the apron
	 * feeder draws it onto the flask, and the flask measures 3 t for the next skip.
	 */
	private static void flaskFeed(GameTestHelper helper) {
		int y = 20, z = 10;
		for (int k = 0; k <= 24; k++) {
			helper.setBlock(new BlockPos(3, y, k), TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH));
		}
		TailingsGameTests.part(helper, TailingsBlocks.TRACTION_RECTIFIER, new BlockPos(1, y, 2), RectifierBlockEntity.class).forceRunning();
		grown(helper, TailingsBlocks.TRACK_GRIZZLY, new BlockPos(3, y - 1, z), Direction.NORTH);
		grown(helper, TailingsBlocks.DUMP_CAM_RAIL, new BlockPos(4, y, z), Direction.NORTH);
		grown(helper, TailingsBlocks.DUMP_POCKET, new BlockPos(4, y - 4, z), Direction.EAST);
		DumpBlockEntity pocket = helper.getBlockEntity(new BlockPos(4, y - 4, z), DumpBlockEntity.class);
		PlantBlockEntity feeder = part(helper, Machine.APRON_FEEDER, new BlockPos(2, y - 4, z + 4));
		PlantBlockEntity flask = part(helper, Machine.MEASURING_FLASK, new BlockPos(2, y - 4, z + 8));
		feeder.forceRunning();
		MineVehicleEntity loco = MineVehicleEntity.placeLocomotive(helper.getLevel(), helper.absolutePos(new BlockPos(3, y, 20)), Heading.NORTH, TailingsEntities.LOCOMOTIVE);
		MineVehicleEntity car = MineVehicleEntity.placeCar(helper.getLevel(), helper.absolutePos(new BlockPos(3, y, (int) Math.floor(20.5 + Haul.carOffset(0)))), Heading.NORTH,
				TailingsEntities.BOTTOM_DUMP_CAR);
		helper.assertTrue(loco != null && car != null, Component.literal("the train should go on the track"));
		car.load().add("minecraft:granite", 8);
		car.refreshLoad();
		helper.succeedWhen(() -> {
			helper.assertTrue(car.load().total() == 0, Component.literal("the car drops its load over the grizzly"));
			helper.assertTrue(flask.hold().total() >= Hoisting.FLASK_KG - 1e-6, Component.literal("the flask measures 3 t, holds " + flask.hold().total() + " (" + feeder.status() + ")"));
			helper.assertTrue(flask.hold().kg(Rock.GRANITE, null) > 0, Component.literal("of the car's granite"));
			helper.assertTrue(pocket.pocket() + pocket.oversize() < 8, Component.literal("drawn from the pocket"));
		});
	}

	/** A full headframe bin holds the loaded skip at the top: BIN FULL, and it goes on once a belt draws the bin down (shaft_bin_full). */
	private static void binFull(GameTestHelper helper) {
		Built b = build(helper, 20);
		hang(helper, b.headframe());
		hang(helper, b.headframe());
		feed(helper, b.flask());
		b.bin().hold().add(Rock.GRANITE, null, Hoisting.BIN_KG - 1000);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(b.hoist().status().startsWith("BIN FULL"), Component.literal("BIN FULL, got " + b.hoist().status())))
				.thenExecute(() -> helper.assertTrue(b.hoist().winder().dumps() == 0, Component.literal("nothing dumped")))
				.thenExecute(() -> b.bin().hold().take(10_000))
				.thenWaitUntil(() -> helper.assertTrue(b.hoist().winder().dumps() == 1, Component.literal("with room, it dumps")))
				.thenExecute(() -> helper.assertTrue(Shaft.dumpedRecently(b.hoist()) && b.hoist().readPoint(6), Component.literal("SKIP DUMPED pulses")))
				.thenSucceed();
	}

	/** With no power the brakes hold the skips where they stand, and they wind on when it comes back (shaft_power). */
	private static void power(GameTestHelper helper) {
		Built b = build(helper, 40);
		hang(helper, b.headframe());
		hang(helper, b.headframe());
		feed(helper, b.flask());
		double[] at = {0};
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(b.hoist().winder().winding() && b.hoist().winder().speed() > 3, Component.literal("winding")))
				.thenExecute(() -> {
					b.hoist().stopForced();
					at[0] = b.hoist().winder().position();
				})
				.thenExecuteAfter(40, () -> {
					helper.assertTrue(b.hoist().winder().speed() == 0 && Math.abs(b.hoist().winder().position() - at[0]) < 1e-6,
							Component.literal("the brakes hold it at " + at[0] + ", now " + b.hoist().winder().position()));
					helper.assertTrue(b.hoist().status().startsWith("STOPPED"), Component.literal("STOPPED, got " + b.hoist().status()));
					b.hoist().forceRunning();
				})
				.thenWaitUntil(() -> helper.assertTrue(b.hoist().winder().dumps() >= 1, Component.literal("power back, it winds on and dumps")))
				.thenSucceed();
	}

	private static void grown(GameTestHelper helper, Block block, BlockPos pos, Direction facing) {
		BlockState state = block.defaultBlockState().setValue(BigPartBlock.FACING, facing);
		helper.setBlock(pos, state);
		BigParts.grow(helper.getLevel(), helper.absolutePos(pos), helper.getLevel().getBlockState(helper.absolutePos(pos)));
	}

	static void forceChunks(GameTestHelper helper) {
		ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
		for (int dx = -1; dx <= 2; dx++) {
			for (int dz = -1; dz <= 2; dz++) {
				helper.getLevel().setChunkForced(origin.x() + dx, origin.z() + dz, true);
			}
		}
	}
}
