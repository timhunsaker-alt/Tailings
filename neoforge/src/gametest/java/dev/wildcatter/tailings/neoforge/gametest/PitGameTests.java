package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.entity.PitVehicleEntity;
import dev.wildcatter.tailings.entity.WheelLoaderEntity;
import dev.wildcatter.tailings.pit.Loose;
import dev.wildcatter.tailings.plant.Machine;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The open pit's first wave, P1 (docs/multiblocks/open-pit.md): loose ground, the haul truck, the truck dump and the wheel loader. */
final class PitGameTests {
	static final String[] SCENES = {"pit_settle", "pit_truck_tip", "pit_truck_body", "pit_hopper", "pit_truck_drive", "pit_loader_soft",
			"pit_loader_hard", "pit_loader_muck"};
	static final java.util.Set<String> LONG = java.util.Set.of("pit_truck_body", "pit_truck_drive", "pit_loader_soft", "pit_hopper");

	private static final Input FORWARD = new Input(true, false, false, false, false, false, false);
	private static final Input BACK = new Input(false, true, false, false, false, false, false);

	private PitGameTests() {
	}

	/** Solid stone from y0 to y1 over x0..x1, z0..z1. */
	private static void floor(GameTestHelper helper, int x0, int x1, int y0, int y1, int z0, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = y0; y <= y1; y++) {
					helper.setBlock(x, y, z, Blocks.STONE);
				}
			}
		}
	}

	private static <T extends PitVehicleEntity> T vehicle(GameTestHelper helper, net.minecraft.world.entity.EntityType<T> type, double x, int y, double z, float yaw) {
		T v = type.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		BlockPos at = helper.absolutePos(new BlockPos(0, y, 0));
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		v.setPos(origin.getX() + x, at.getY(), origin.getZ() + z);
		v.setYRot(yaw);
		helper.getLevel().addFreshEntity(v);
		return v;
	}

	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> T part(GameTestHelper helper, Block block, BlockPos pos, Class<T> entity) {
		BlockState state = block.defaultBlockState().setValue(BigPartBlock.FACING, Direction.NORTH);
		helper.setBlock(pos, state);
		BigParts.grow(helper.getLevel(), helper.absolutePos(pos), helper.getLevel().getBlockState(helper.absolutePos(pos)));
		return entity == null ? null : helper.getBlockEntity(pos, entity);
	}

	private static int count(GameTestHelper helper, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		int n = 0;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = y0; y <= y1; y++) {
					if (helper.getBlockState(new BlockPos(x, y, z)).getBlock() == block) {
						n++;
					}
				}
			}
		}
		return n;
	}

	/** The height of loose ground in a column: the first air at or above y. */
	private static int top(GameTestHelper helper, int x, int z, int y) {
		while (!helper.getBlockState(new BlockPos(x, y, z)).isAir()) {
			y++;
		}
		return y;
	}

	/** 200 blocks of muck let go at one point stand no steeper than 1 in 1 and fill a hole beside the tip first. */
	static void settle(GameTestHelper helper) {
		floor(helper, 0, 30, 54, 59, 0, 30);
		for (int x = 18; x <= 20; x++) {
			for (int z = 14; z <= 16; z++) {
				helper.setBlock(x, 59, z, Blocks.AIR);
				helper.setBlock(x, 58, z, Blocks.AIR);
			}
		}
		ServerLevel level = helper.getLevel();
		BlockPos tip = helper.absolutePos(new BlockPos(15, 72, 15));
		int placed = 0;
		for (int i = 0; i < 200; i++) {
			if (Loose.muck(level, tip, Map.of("minecraft:cobblestone", 3)) != null) {
				placed++;
			}
		}
		helper.assertTrue(placed == 200, Component.literal("all 200 blocks should find a place, " + placed + " did"));
		helper.assertTrue(count(helper, TailingsBlocks.MUCK, 0, 30, 55, 80, 0, 30) == 200, Component.literal("200 muck blocks should stand in the pile"));
		for (int x = 18; x <= 20; x++) {
			for (int z = 14; z <= 16; z++) {
				helper.assertTrue(helper.getBlockState(new BlockPos(x, 58, z)).getBlock() == TailingsBlocks.MUCK,
						Component.literal("the hole beside the tip should fill first, but " + x + ", 58, " + z + " is open"));
			}
		}
		for (int x = 2; x <= 28; x++) {
			for (int z = 2; z <= 28; z++) {
				int h = top(helper, x, z, 58);
				helper.assertTrue(Math.abs(h - top(helper, x + 1, z, 58)) <= 1 && Math.abs(h - top(helper, x, z + 1, 58)) <= 1,
						Component.literal("a step of 2 or more in the pile at " + x + ", " + z));
			}
		}
		helper.succeed();
	}

	/** A truck tipped on the ground: its sand lands as sand, its cobble as muck at 4 quarters a bank block. */
	static void truckTip(GameTestHelper helper) {
		floor(helper, 0, 30, 58, 59, 0, 30);
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 15.5, 60, 18.5, 0);
		truck.load().add("minecraft:sand", 30_000);
		truck.load().add("minecraft:cobblestone", 27_000);
		truck.tip(helper.getLevel());
		int sand = count(helper, Blocks.SAND, 0, 30, 60, 75, 0, 30);
		int muck = count(helper, TailingsBlocks.MUCK, 0, 30, 60, 75, 0, 30);
		helper.assertTrue(sand == 18, Component.literal("30 t of sand at 1.6 t a block is 18 blocks, got " + sand));
		// 27 t of cobble is 10 bank blocks, 40 quarters: 13 full muck blocks and one with a quarter
		helper.assertTrue(muck == 14, Component.literal("27 t of cobble should land as 14 muck blocks, got " + muck));
		helper.assertTrue(truck.load().kg("minecraft:sand") == 1_200 && truck.load().kg("minecraft:cobblestone") == 0,
				Component.literal("what is under a whole block stays in the body: " + truck.load().save()));
		helper.succeed();
	}

	/** Jump raises the body over 13 s, the load goes out at the top, and the body comes down. */
	static void truckBody(GameTestHelper helper) {
		floor(helper, 0, 30, 58, 59, 0, 30);
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 15.5, 60, 18.5, 0);
		truck.load().add("minecraft:gravel", 19_000);
		truck.refreshLoad();
		truck.pressJump(helper.getLevel());
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.lift() > 0.5F && truck.busy(), Component.literal("the body should be going up")))
				.thenWaitUntil(() -> helper.assertTrue(truck.load().empty(), Component.literal("the load should go at the top (" + truck.status() + ")")))
				.thenWaitUntil(() -> helper.assertTrue(truck.lift() == 0 && !truck.busy(), Component.literal("the body should come back down")))
				.thenExecute(() -> helper.assertTrue(count(helper, Blocks.GRAVEL, 0, 30, 60, 75, 0, 30) == 10, Component.literal("19 t of gravel is 10 blocks")))
				.thenSucceed();
	}

	/**
	 * The truck dump: a truck backs to the bumper, tips its rock into the hopper, and the apron feeder draws it out to the sorter's
	 * bin. Soil is refused, and a full hopper reads FULL.
	 */
	static void hopper(GameTestHelper helper) {
		floor(helper, 0, 14, 57, 58, 12, 34);
		DumpBlockEntity hopper = part(helper, TailingsBlocks.PIT_HOPPER, new BlockPos(2, 53, 4), DumpBlockEntity.class);
		part(helper, TailingsBlocks.TRUCK_DUMP_WALL, new BlockPos(1, 56, 10), PlantBlockEntity.class);
		PlantBlockEntity feeder = part(helper, TailingsBlocks.plantBlock(Machine.APRON_FEEDER), new BlockPos(5, 53, 0), PlantBlockEntity.class);
		PlantBlockEntity sorter = part(helper, TailingsBlocks.plantBlock(Machine.ORE_SORTER), new BlockPos(4, 53, -3), PlantBlockEntity.class);
		PlantBlockEntity bin = part(helper, TailingsBlocks.plantBlock(Machine.ORE_BIN), new BlockPos(4, 53, -6), PlantBlockEntity.class);
		helper.assertTrue(sorter.setWasteSide(Direction.NORTH), Component.literal("the sorter's north side should take the waste"));
		// backing up from the pad: the bumper stops the wheels with the body over the hopper
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 7.0, 59, 20.0, 0);
		truck.load().add("minecraft:cobblestone", 55_000);   // 20 bank blocks and 1 t
		truck.refreshLoad();
		truck.testDrive(BACK);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.speed() == 0 && relZ(helper, truck) < 17.5,
						Component.literal("the truck should back up to the bumper and stop, at z " + relZ(helper, truck))))
				.thenExecute(() -> {
					truck.testDrive(Input.EMPTY);
					helper.assertTrue(relZ(helper, truck) > 15.9, Component.literal("the bumper should stop the truck, at z " + relZ(helper, truck)));
					helper.assertTrue(truck.hopperBehind(helper.getLevel()) == hopper, Component.literal("the hopper should be under the truck's tail"));
					truck.tip(helper.getLevel());
					helper.assertTrue(hopper.pocket() == 20, Component.literal("55 t of cobble is 20 bank blocks in the hopper, got " + hopper.pocket()));
					helper.assertTrue(truck.load().empty() && !truck.loaded(), Component.literal("the truck should come away empty: " + truck.load().save()));
					helper.assertTrue(hopper.part().getOrDefault("minecraft:cobblestone", 0L) == 1_000, Component.literal("the hopper keeps the 1 t under a block, got " + hopper.part()));
					truck.load().add("minecraft:cobblestone", 1_700);
					truck.tip(helper.getLevel());
					helper.assertTrue(hopper.pocket() == 21 && hopper.part().isEmpty(), Component.literal("the next 1.7 t makes up a block: " + hopper.pocket() + " blocks, " + hopper.part()));
					truck.load().add("minecraft:sand", 16_000);
					truck.tip(helper.getLevel());
					helper.assertTrue(truck.status().contains("not rock") && hopper.pocket() == 21,
							Component.literal("the hopper should refuse soil, got " + truck.status()));
					truck.load().clear();
					helper.assertTrue(hopper.offer(Map.of("minecraft:cobblestone", hopper.capacity() - hopper.pocket() - 5)), Component.literal("fill the hopper"));
					truck.load().add("minecraft:cobblestone", 27_000);
					truck.tip(helper.getLevel());
					helper.assertTrue(truck.status().contains("FULL"), Component.literal("a full hopper reads FULL, got " + truck.status()));
					hopper.draw(hopper.pocket() - 20);
					feeder.forceRunning();
					sorter.forceRunning();
				})
				.thenWaitUntil(() -> helper.assertTrue(bin.binned().getOrDefault("minecraft:cobblestone", 0) >= 20,
						Component.literal("the feeder should draw the hopper's cobble to the bin (feeder " + feeder.status() + "; sorter " + sorter.status() + ")")))
				.thenSucceed();
	}

	private static double relZ(GameTestHelper helper, PitVehicleEntity v) {
		return v.getZ() - helper.absolutePos(BlockPos.ZERO).getZ();
	}

	/** It drives by its engine's power, climbs a 1-block step and stops against a 2-block wall. */
	static void truckDrive(GameTestHelper helper) {
		floor(helper, 0, 12, 58, 59, 0, 90);
		for (int x = 0; x <= 12; x++) {
			for (int z = 40; z <= 90; z++) {
				helper.setBlock(x, 60, z, Blocks.STONE);   // a step up at z 40
			}
			helper.setBlock(x, 61, 70, Blocks.STONE);       // and a wall 2 high at z 70
			helper.setBlock(x, 62, 70, Blocks.STONE);
		}
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 6.5, 60, 8.0, 0);
		double startY = truck.getY();
		truck.testDrive(FORWARD);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.getY() >= startY + 0.99, Component.literal("the truck should climb the step, at z " + relZ(helper, truck))))
				.thenWaitUntil(() -> helper.assertTrue(truck.speed() == 0 && relZ(helper, truck) > 55,
						Component.literal("the truck should stop at the 2-high wall, at z " + relZ(helper, truck) + " doing " + truck.speed())))
				.thenExecute(() -> helper.assertTrue(relZ(helper, truck) < 65.1, Component.literal("it must not pass the wall, at z " + relZ(helper, truck))))
				.thenSucceed();
	}

	/** Five passes of sand fill a truck to its 91 t. */
	static void loaderSoft(GameTestHelper helper) {
		floor(helper, 0, 20, 57, 58, 0, 34);
		for (int x = 0; x <= 6; x++) {
			for (int z = 20; z <= 28; z++) {
				for (int y = 59; y <= 64; y++) {
					helper.setBlock(x, y, z, Blocks.SAND);
				}
			}
		}
		WheelLoaderEntity loader = vehicle(helper, TailingsEntities.WHEEL_LOADER, 3.5, 59, 12.3, 0);
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 10.0, 59, 16.0, 0);
		loader.fastPasses(25);
		int[] passes = {0};
		helper.onEachTick(() -> {
			if (loader.busy() || truck.load().room() == 0) {
				return;
			}
			if (loader.bucket().empty()) {
				loader.pressJump(helper.getLevel());
				passes[0]++;
			} else {
				loader.pressJump(helper.getLevel());
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(truck.load().room() == 0, Component.literal("the truck should fill: " + truck.load().tonnes() + " after " + passes[0]
					+ " passes (" + loader.status() + ")"));
			helper.assertTrue(passes[0] == 5, Component.literal("a 20.4 t bucket fills a 91 t truck in 5 passes, took " + passes[0]));
		});
	}

	/** Standing rock it cannot dig. */
	static void loaderHard(GameTestHelper helper) {
		floor(helper, 0, 20, 57, 58, 0, 34);
		floor(helper, 0, 8, 59, 64, 20, 26);
		WheelLoaderEntity loader = vehicle(helper, TailingsEntities.WHEEL_LOADER, 3.5, 59, 12.3, 0);
		loader.pressJump(helper.getLevel());
		helper.assertTrue(loader.status().equals("hard rock: blast it first") && loader.bucket().empty(),
				Component.literal("a stone face should read hard rock, got " + loader.status()));
		helper.succeed();
	}

	/** It digs muck, taking what each block holds. */
	static void loaderMuck(GameTestHelper helper) {
		floor(helper, 0, 20, 57, 58, 0, 34);
		ServerLevel level = helper.getLevel();
		for (int i = 0; i < 10; i++) {
			BlockPos at = helper.absolutePos(new BlockPos(2 + i % 5, 59 + i / 5, 20));
			level.setBlock(at, TailingsBlocks.MUCK.defaultBlockState(), Block.UPDATE_ALL);
			((MuckBlockEntity) level.getBlockEntity(at)).set(Map.of("minecraft:cobblestone", 3));
		}
		WheelLoaderEntity loader = vehicle(helper, TailingsEntities.WHEEL_LOADER, 4.5, 59, 12.3, 0);
		loader.pressJump(level);
		// ten blocks of three quarters of 2.7 t
		helper.assertTrue(loader.bucket().kg("minecraft:cobblestone") == 20_250,
				Component.literal("the bucket should hold 20.25 t of cobble, got " + loader.bucket().save() + " (" + loader.status() + ")"));
		helper.assertTrue(count(helper, TailingsBlocks.MUCK, 0, 10, 59, 62, 18, 22) == 0, Component.literal("the muck should be dug out"));
		helper.succeed();
	}
}
