package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.FuelBayBlockEntity;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.block.SurveyStakeBlock;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.entity.PitVehicleEntity;
import dev.wildcatter.tailings.entity.WheelLoaderEntity;
import dev.wildcatter.tailings.item.SurveyBookItem;
import dev.wildcatter.tailings.pit.StopKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The open pit's second wave, P2a (docs/multiblocks/open-pit.md): survey stakes and the book, self-driving trucks on their routes, the
 * self-running loader, diesel and the fuel bay.
 */
final class PitHaulGameTests {
	static final String[] SCENES = {"pit_survey", "pit_loader_auto", "pit_fuel_burn", "pit_fuel_bay", "pit_out_of_fuel", "pit_truck_obstacle",
			"pit_truck_follow", "pit_one_way", "pit_truck_haul"};
	/** Scenes whose trucks run long routes at their real speed, and the room each needs. */
	static final Map<String, Integer> TICKS = Map.of("pit_truck_obstacle", 1600, "pit_truck_follow", 1600, "pit_one_way", 2400, "pit_truck_haul", 7200,
			"pit_loader_auto", 1600, "pit_fuel_burn", 1600);
	static final Map<String, Integer> PADDING = Map.of("pit_truck_follow", 150, "pit_one_way", 190, "pit_truck_haul", 130);

	private static final Input FORWARD = new Input(true, false, false, false, false, false, false);
	private static final Input JUMP = new Input(false, false, false, false, true, false, false);

	private PitHaulGameTests() {
	}

	static int ticks(String scene, int otherwise) {
		return TICKS.getOrDefault(scene, PitDispatchGameTests.TICKS.getOrDefault(scene, PitDryStackGameTests.TICKS.getOrDefault(scene, PitBlastGameTests.TICKS.getOrDefault(scene, otherwise))));
	}

	static int padding(String scene, int otherwise) {
		return PADDING.getOrDefault(scene, PitDispatchGameTests.PADDING.getOrDefault(scene, PitDryStackGameTests.PADDING.getOrDefault(scene, PitBlastGameTests.PADDING.getOrDefault(scene, otherwise))));
	}

	/** Chunks the long scenes reach, from the test's origin, in chunks. */
	static int[] reach(String scene) {
		return switch (scene) {
			case "pit_truck_follow" -> new int[] {-1, 3, -1, 9};
			case "pit_one_way" -> new int[] {-1, 3, -1, 12};
			case "pit_truck_haul" -> new int[] {-2, 8, -1, 5};
			default -> PitDispatchGameTests.reach(scene);
		};
	}

	// ---- building ---------------------------------------------------------------------------------------------------------------

	static void floor(GameTestHelper helper, int x0, int x1, int y0, int y1, int z0, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = y0; y <= y1; y++) {
					helper.setBlock(x, y, z, Blocks.STONE);
				}
			}
		}
	}

	static BlockPos stake(GameTestHelper helper, int x, int y, int z, StopKind kind) {
		helper.setBlock(new BlockPos(x, y, z), TailingsBlocks.SURVEY_STAKE.defaultBlockState().setValue(SurveyStakeBlock.KIND, kind));
		return helper.absolutePos(new BlockPos(x, y, z));
	}

	static <T extends PitVehicleEntity> T vehicle(GameTestHelper helper, EntityType<T> type, double x, int y, double z, float yaw) {
		T v = type.create(helper.getLevel(), EntitySpawnReason.COMMAND);
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		BlockPos at = helper.absolutePos(new BlockPos(0, y, 0));
		v.setPos(origin.getX() + x, at.getY(), origin.getZ() + z);
		v.setYRot(yaw);
		helper.getLevel().addFreshEntity(v);
		return v;
	}

	static <T extends net.minecraft.world.level.block.entity.BlockEntity> T part(GameTestHelper helper, Block block, BlockPos pos, Class<T> entity) {
		BlockState state = block.defaultBlockState().setValue(BigPartBlock.FACING, Direction.NORTH);
		helper.setBlock(pos, state);
		BigParts.grow(helper.getLevel(), helper.absolutePos(pos), helper.getLevel().getBlockState(helper.absolutePos(pos)));
		return entity == null ? null : helper.getBlockEntity(pos, entity);
	}

	private static void muck(GameTestHelper helper, int x0, int x1, int y0, int y1, int z0, int z1, String id) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = y0; y <= y1; y++) {
					BlockPos pos = new BlockPos(x, y, z);
					helper.setBlock(pos, TailingsBlocks.MUCK);
					helper.getBlockEntity(pos, MuckBlockEntity.class).set(Map.of(id, 3));
				}
			}
		}
	}

	static double relX(GameTestHelper helper, PitVehicleEntity v) {
		return v.getX() - helper.absolutePos(BlockPos.ZERO).getX();
	}

	static double relZ(GameTestHelper helper, PitVehicleEntity v) {
		return v.getZ() - helper.absolutePos(BlockPos.ZERO).getZ();
	}

	static String where(GameTestHelper helper, PitVehicleEntity v) {
		return String.format(java.util.Locale.ROOT, "at %.1f, %.1f, speed %.1f, %s", relX(helper, v), relZ(helper, v), v.speed(), v.status());
	}

	// ---- the scenes -------------------------------------------------------------------------------------------------------------

	/** A route of four stakes: a clear two-way leg, a 20 % leg and a blocked leg are written on the book. */
	static void survey(GameTestHelper helper) {
		floor(helper, 0, 30, 56, 58, 0, 39);
		ServerLevel level = helper.getLevel();
		BlockPos a = stake(helper, 5, 59, 2, StopKind.LOAD);
		BlockPos b = stake(helper, 5, 59, 20, StopKind.WAYPOINT);
		BlockPos c = stake(helper, 5, 62, 35, StopKind.WAYPOINT);
		BlockPos d = stake(helper, 25, 62, 35, StopKind.DUMP_POINT);
		helper.setBlock(15, 63, 35, Blocks.STONE);   // a rock on the third leg
		helper.setBlock(15, 64, 35, Blocks.STONE);
		ItemStack book = new ItemStack(TailingsItems.SURVEY_BOOK);
		for (BlockPos pos : List.of(a, b, c, d)) {
			SurveyBookItem.addStake(level, book, pos);
		}
		helper.assertTrue(SurveyBookItem.route(book).equals(List.of(a, b, c, d)), Component.literal("the book should hold the four stakes in order, got " + SurveyBookItem.route(book)));
		List<String> problems = SurveyBookItem.problems(book);
		helper.assertTrue(problems.size() == 2 && problems.get(0).equals("leg 2: grade 20 % (most 10 %)") && problems.get(1).startsWith("leg 3: blocked at"),
				Component.literal("legs 2 and 3 should be written up, got " + problems));
		helper.assertFalse(SurveyBookItem.survey(level, a, b).oneWay(), Component.literal("the first leg is open ground: two-way"));
		// a truck given the route stops at the steep leg and reads it
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 5.5, 59, 8, 0);
		truck.load().add("minecraft:sand", 30_000);   // loaded, it heads for the dump end, over the steep leg
		truck.refreshLoad();
		helper.assertTrue(truck.takeRoute(level, SurveyBookItem.route(book)) == null, Component.literal("the route should be taken"));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.status().startsWith("leg 2: grade 20 %"), Component.literal("the truck should stop for the steep leg: " + where(helper, truck))))
				.thenSucceed();
	}

	/** Set to a LOAD stake, the loader digs, holds its full bucket, and fills a truck that pulls in. */
	static void loaderAuto(GameTestHelper helper) {
		floor(helper, 0, 30, 56, 58, 0, 30);
		muck(helper, 4, 8, 59, 61, 19, 21, "minecraft:cobblestone");
		ServerLevel level = helper.getLevel();
		BlockPos load = stake(helper, 14, 59, 10, StopKind.LOAD);
		WheelLoaderEntity loader = vehicle(helper, TailingsEntities.WHEEL_LOADER, 6.5, 59, 10.5, 0);
		loader.fastPasses(10);
		helper.assertTrue(loader.setAuto(level, List.of(load)) == null, Component.literal("a LOAD stake within 16 m should set it working"));
		helper.assertTrue(loader.setAuto(level, List.of(stake(helper, 29, 59, 29, StopKind.LOAD))) != null, Component.literal("a LOAD stake 25 m off should be refused"));
		loader.setAuto(level, List.of(load));
		HaulTruckEntity[] truck = new HaulTruckEntity[1];
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(!loader.busy() && !loader.bucket().empty() && loader.status().startsWith("bucket full"),
						Component.literal("it should dig and hold the full bucket: " + loader.status())))
				.thenExecute(() -> {
					helper.assertTrue(loader.fuel() < loader.tank() * 0.10, Component.literal("digging should burn diesel: " + loader.fuel()));
					truck[0] = vehicle(helper, TailingsEntities.HAUL_TRUCK, 13.5, 59, 17, 0);
				})
				.thenWaitUntil(() -> helper.assertTrue(truck[0].load().total() >= 40_000, Component.literal("it should fill the truck that pulled in: " + truck[0].load().tonnes() + "; " + loader.status())))
				.thenSucceed();
	}

	/** Diesel by the engine's work: about 200 L/h flat out, nothing parked. */
	static void fuelBurn(GameTestHelper helper) {
		floor(helper, 0, 30, 56, 58, 0, 39);
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 15.5, 59, 6, 0);
		truck.setFuel(500);
		double[] start = new double[1];
		helper.startSequence()
				.thenIdle(100)
				.thenExecute(() -> helper.assertTrue(truck.fuel() == 500, Component.literal("parked with nobody in it, its engine is off: " + truck.fuel())))
				.thenExecute(() -> truck.testDrive(FORWARD))
				.thenIdle(100)
				.thenExecute(() -> {
					truck.testDrive(Input.EMPTY);
					double burnt = 500 - truck.fuel();
					start[0] = burnt;
					// 5 s of a 765 kW engine is at most 0.28 L at 36 MJ/L and 38 %
					helper.assertTrue(burnt > 0.05 && burnt <= 0.29, Component.literal("5 s driving should burn up to 0.28 L, burnt " + burnt));
				})
				.thenWaitUntil(() -> helper.assertTrue(truck.speed() == 0, Component.literal("it should roll to a stop")))
				.thenExecute(() -> start[0] = truck.fuel())
				.thenIdle(200)
				.thenExecute(() -> helper.assertTrue(truck.fuel() == start[0], Component.literal("stopped with nobody in it, it burns nothing: " + (start[0] - truck.fuel()))))
				.thenSucceed();
	}

	/** The fuel bay fills a stopped truck within 8 m at 600 L a minute, and not one 20 m off. */
	static void fuelBay(GameTestHelper helper) {
		floor(helper, 0, 30, 56, 58, 0, 39);
		FuelBayBlockEntity bay = part(helper, TailingsBlocks.FUEL_BAY, new BlockPos(10, 59, 10), FuelBayBlockEntity.class);
		bay.setLitres(2_000);
		HaulTruckEntity near = vehicle(helper, TailingsEntities.HAUL_TRUCK, 18.5, 59, 11, 0);
		HaulTruckEntity far = vehicle(helper, TailingsEntities.HAUL_TRUCK, 18.5, 59, 34, 0);
		near.setFuel(100);
		far.setFuel(100);
		helper.startSequence()
				.thenIdle(100)
				.thenExecute(() -> {
					helper.assertTrue(near.fuel() > 140 && near.fuel() <= 150.01, Component.literal("5 s at 10 L/s should give about 50 L, it has " + near.fuel()));
					helper.assertTrue(Math.abs(bay.litres() + near.fuel() - 2_100) < 0.01, Component.literal("what the truck took comes out of the day tank: " + bay.litres()));
					helper.assertTrue(far.fuel() == 100, Component.literal("a truck 20 m off is out of the hose's reach: " + far.fuel()));
				})
				.thenSucceed();
	}

	/** Out of diesel it rolls to a stop and will not tip. */
	static void outOfFuel(GameTestHelper helper) {
		floor(helper, 0, 30, 56, 58, 0, 39);
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 15.5, 59, 6, 0);
		truck.load().add("minecraft:cobblestone", 27_000);
		truck.setFuel(0.02);
		truck.testDrive(FORWARD);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.outOfFuel() && truck.speed() == 0, Component.literal("it should run dry and stop: " + where(helper, truck))))
				.thenExecute(() -> truck.testDrive(JUMP))
				.thenIdle(5)
				.thenExecute(() -> {
					truck.testDrive(Input.EMPTY);
					helper.assertTrue(truck.status().equals("out of fuel") && truck.lift() == 0, Component.literal("it should not tip dry: " + truck.status()));
				})
				.thenSucceed();
	}

	/** A self-driving truck stops for a stand in its lane, reads BLOCKED after 30 s, and goes on when it is gone. */
	static void truckObstacle(GameTestHelper helper) {
		floor(helper, 0, 40, 56, 58, 0, 39);
		ServerLevel level = helper.getLevel();
		BlockPos l = stake(helper, 20, 59, 2, StopKind.LOAD);
		BlockPos w = stake(helper, 20, 59, 26, StopKind.WAYPOINT);
		BlockPos d = stake(helper, 20, 59, 38, StopKind.DUMP_POINT);
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 16.5, 59, 7, 0);
		truck.load().add("minecraft:sand", 30_000);
		truck.refreshLoad();
		ArmorStand stand = helper.spawn(net.minecraft.world.entity.EntityTypes.ARMOR_STAND, new BlockPos(16, 59, 19));
		helper.assertTrue(truck.takeRoute(level, List.of(l, w, d)) == null, Component.literal("the route should be taken"));
		helper.startSequence()
				.thenIdle(200)
				.thenExecute(() -> helper.assertTrue(truck.speed() == 0 && relZ(helper, truck) + 5 < 19.5 && relZ(helper, truck) > 7.5,
						Component.literal("it should drive up and stop short of the stand: " + where(helper, truck))))
				.thenWaitUntil(() -> helper.assertTrue(truck.blocked(), Component.literal("stopped 30 s, it should read BLOCKED: " + where(helper, truck))))
				.thenExecute(stand::discard)
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, truck) > 20, Component.literal("with the way clear it should go on: " + where(helper, truck))))
				.thenSucceed();
	}

	/** It keeps 50 m from a truck standing ahead in its lane. */
	static void truckFollow(GameTestHelper helper) {
		floor(helper, 0, 40, 56, 58, 0, 140);
		ServerLevel level = helper.getLevel();
		BlockPos l = stake(helper, 20, 59, 0, StopKind.LOAD);
		BlockPos w = stake(helper, 20, 59, 125, StopKind.WAYPOINT);
		BlockPos d = stake(helper, 20, 59, 138, StopKind.DUMP_POINT);
		HaulTruckEntity lead = vehicle(helper, TailingsEntities.HAUL_TRUCK, 16.5, 59, 100, 0);
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 16.5, 59, 6, 0);
		truck.load().add("minecraft:sand", 30_000);
		truck.refreshLoad();
		helper.assertTrue(truck.takeRoute(level, List.of(l, w, d)) == null, Component.literal("the route should be taken"));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, truck) > 20 && truck.speed() == 0, Component.literal("it should drive up behind the standing truck and stop: " + where(helper, truck))))
				.thenExecute(() -> {
					double gap = (relZ(helper, lead) - 5) - (relZ(helper, truck) + 5);
					helper.assertTrue(gap >= 45 && gap <= 58, Component.literal("it should keep about 50 m, the gap is " + gap));
				})
				.thenSucceed();
	}

	/** A truck waits short of a one-way leg while another comes along it, then takes it. */
	static void oneWay(GameTestHelper helper) {
		floor(helper, 0, 40, 56, 58, 0, 180);
		for (int z = 33; z <= 67; z++) {
			for (int y = 59; y <= 60; y++) {
				helper.setBlock(13, y, z, Blocks.STONE);
				helper.setBlock(28, y, z, Blocks.STONE);
			}
		}
		ServerLevel level = helper.getLevel();
		List<BlockPos> route = new ArrayList<>();
		route.add(stake(helper, 20, 59, 0, StopKind.LOAD));
		route.add(stake(helper, 20, 59, 30, StopKind.WAYPOINT));
		route.add(stake(helper, 20, 59, 70, StopKind.WAYPOINT));
		route.add(stake(helper, 20, 59, 150, StopKind.WAYPOINT));
		route.add(stake(helper, 20, 59, 170, StopKind.DUMP_POINT));
		helper.assertTrue(SurveyBookItem.survey(level, route.get(1), route.get(2)).oneWay(), Component.literal("the walled leg should be one-way"));
		helper.assertFalse(SurveyBookItem.survey(level, route.get(2), route.get(3)).oneWay(), Component.literal("the open leg should be two-way"));
		HaulTruckEntity up = vehicle(helper, TailingsEntities.HAUL_TRUCK, 20.5, 59, 38, 0);
		up.load().add("minecraft:sand", 30_000);
		up.refreshLoad();
		HaulTruckEntity down = vehicle(helper, TailingsEntities.HAUL_TRUCK, 24.5, 59, 140, 180);
		helper.assertTrue(up.takeRoute(level, route) == null && down.takeRoute(level, route) == null, Component.literal("both should take the route"));
		boolean[] waited = new boolean[1];
		helper.startSequence()
				.thenWaitUntil(() -> {
					if (down.status().startsWith("one-way") && down.speed() == 0 && relZ(helper, down) > 100) {
						waited[0] = true;
					}
					helper.assertTrue(relZ(helper, up) > 75, Component.literal("the loaded truck should clear the one-way leg: " + where(helper, up) + "; the other " + where(helper, down)));
				})
				.thenExecute(() -> helper.assertTrue(waited[0], Component.literal("the empty truck should have waited 40 m short of the one-way leg: " + where(helper, down))))
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, down) < 60, Component.literal("then it should take the leg: " + where(helper, down))))
				.thenSucceed();
	}

	/**
	 * The spec's haul: a truck on a 7-stake route turns round and backs to a self-running loader, is filled, climbs a 10 % ramp,
	 * turns round and backs to the truck dump's bumper, tips into the hopper, and comes back.
	 */
	static void truckHaul(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		// the dump's pad, its top at 58, and the truck dump: the hopper in the cut, its wall and bumper
		floor(helper, -10, 60, 52, 58, 12, 75);
		DumpBlockEntity hopper = part(helper, TailingsBlocks.PIT_HOPPER, new BlockPos(2, 53, 4), DumpBlockEntity.class);
		part(helper, TailingsBlocks.TRUCK_DUMP_WALL, new BlockPos(1, 56, 10), null);
		// the ramp: down a block every 10 m to the loading floor, its top at 55
		for (int x = 61; x <= 125; x++) {
			int top = x <= 70 ? 57 : x <= 80 ? 56 : 55;
			for (int z = (x <= 90 ? 12 : -5); z <= 75; z++) {
				for (int y = 52; y <= top; y++) {
					helper.setBlock(x, y, z, Blocks.STONE);
				}
			}
		}
		// the face: 60 blocks of broken rock in front of the loader's bucket
		muck(helper, 98, 102, 56, 58, 18, 21, "minecraft:cobblestone");
		List<BlockPos> route = new ArrayList<>();
		route.add(stake(helper, 93, 56, 15, StopKind.LOAD));
		route.add(stake(helper, 93, 56, 58, StopKind.WAYPOINT));
		route.add(stake(helper, 88, 56, 52, StopKind.WAYPOINT));
		route.add(stake(helper, 55, 59, 52, StopKind.WAYPOINT));
		route.add(stake(helper, 40, 59, 42, StopKind.WAYPOINT));
		route.add(stake(helper, 14, 59, 42, StopKind.WAYPOINT));
		route.add(stake(helper, 7, 59, 13, StopKind.DUMP_POCKET));
		ItemStack book = new ItemStack(TailingsItems.SURVEY_BOOK);
		for (BlockPos pos : route) {
			SurveyBookItem.addStake(level, book, pos);
		}
		helper.assertTrue(SurveyBookItem.problems(book).isEmpty(), Component.literal("the haul road should survey clean: " + SurveyBookItem.problems(book)));
		WheelLoaderEntity loader = vehicle(helper, TailingsEntities.WHEEL_LOADER, 100.5, 56, 28, 180);
		loader.fastPasses(10);
		loader.setFuel(loader.tank());
		helper.assertTrue(loader.setAuto(level, route) == null, Component.literal("the loader should take its LOAD stake"));
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 93.5, 56, 62, 0);
		truck.setFuel(truck.tank());
		helper.assertTrue(truck.takeRoute(level, SurveyBookItem.route(book)) == null, Component.literal("the truck should take the route"));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(truck.load().total() > 0, Component.literal("it should back to the loader and be loaded: " + where(helper, truck) + "; loader "
						+ loader.status() + ", bucket " + loader.bucket().tonnes() + ", truck in reach " + (loader.truckInReach(level) != null) + ", fuel " + loader.fuel())))
				.thenWaitUntil(() -> helper.assertTrue(truck.load().room() == 0 && relX(helper, truck) < 80,
						Component.literal("full, it should climb the ramp: " + where(helper, truck) + "; " + truck.load().tonnes())))
				.thenWaitUntil(() -> helper.assertTrue(hopper.pocket() > 0 || truck.lift() > 0, Component.literal("it should back to the bumper and tip: " + where(helper, truck))))
				.thenExecute(() -> System.out.println("[tailings-test] pit_truck_haul: tipping " + where(helper, truck) + ", yaw " + truck.getYRot() + ", hopper behind " + (truck.hopperBehind(level) != null)))
				.thenWaitUntil(() -> helper.assertTrue(truck.load().empty() && hopper.pocket() >= 33 && relX(helper, truck) > 30,
						Component.literal("empty, it should come back toward the loader: " + where(helper, truck) + "; hopper " + hopper.pocket() + "; loader " + loader.status() + ", bucket " + loader.bucket().tonnes())))
				.thenSucceed();
	}
}
