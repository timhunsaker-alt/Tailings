package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetPartBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.BatteryChargerBlockEntity;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.MineRailBlock;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.RailBlockEntity;
import dev.wildcatter.tailings.block.RailGrade;
import dev.wildcatter.tailings.block.RectifierBlockEntity;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.haul.Battery;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.mine.Heading;
import java.util.ArrayList;
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
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server scenes for the later mine-train waves (decided on recommendation while Tim was away, docs/questions/l2-l3-trains.md): the
 * battery locomotive and its charger (docs/multiblocks/battery-locomotive.md, L2) and the bottom-dump cars over a track grizzly
 * (docs/multiblocks/bottom-dump-cars.md, L3).
 */
final class TrainsGameTests {
	static final String[] SCENES = {"battery_loco_run", "battery_loco_climb", "battery_loco_flat", "battery_charge", "bottom_dump_on_move",
			"bottom_dump_full", "bottom_dump_mixed", "train_models_l2_l3"};
	static final Set<String> LONG = Set.of("battery_loco_run", "battery_loco_climb", "battery_loco_flat", "battery_charge", "bottom_dump_on_move",
			"bottom_dump_full", "bottom_dump_mixed");
	private static final Map<String, Integer> TICKS = Map.of("battery_loco_run", 2400, "battery_loco_climb", 2400);
	private static final Map<String, Integer> PADDING = Map.of("battery_loco_run", 100, "battery_loco_climb", 80);
	private static final int Y = 60;
	/** Ticks for the chunks a long scene forces to come in before its vehicles go on the track. */
	private static final int SETTLE = 20;

	private TrainsGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("battery_loco_run", TrainsGameTests::batteryRun);
		out.accept("battery_loco_climb", TrainsGameTests::batteryClimb);
		out.accept("battery_loco_flat", TrainsGameTests::batteryFlat);
		out.accept("battery_charge", TrainsGameTests::batteryCharge);
		out.accept("bottom_dump_on_move", TrainsGameTests::bottomDumpOnMove);
		out.accept("bottom_dump_full", TrainsGameTests::bottomDumpFull);
		out.accept("bottom_dump_mixed", TrainsGameTests::bottomDumpMixed);
		out.accept("train_models_l2_l3", TrainsGameTests::models);
	}

	static int ticks(String scene, int otherwise) {
		return TICKS.getOrDefault(scene, otherwise);
	}

	static int padding(String scene, int otherwise) {
		return PADDING.getOrDefault(scene, otherwise);
	}

	// ---- building ---------------------------------------------------------------------------------------------------------------

	private static void force(GameTestHelper helper, int dzFrom, int dzTo) {
		ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
		for (int dx = -1; dx <= 2; dx++) {
			for (int dz = dzFrom; dz <= dzTo; dz++) {
				helper.getLevel().setChunkForced(origin.x() + dx, origin.z() + dz, true);
			}
		}
	}

	private static BlockState rail() {
		return TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
	}

	/** A north-running track at x 3 from z {@code from} to {@code to}, at height {@code y}. */
	private static void track(GameTestHelper helper, int y, int from, int to) {
		for (int z = from; z <= to; z++) {
			helper.setBlock(new BlockPos(3, y, z), rail());
		}
	}

	/** A rectifier, forced on, beside the track at z 2: the track is fed. */
	private static void feed(GameTestHelper helper, int y) {
		TailingsGameTests.part(helper, TailingsBlocks.TRACTION_RECTIFIER, new BlockPos(1, y, 2), RectifierBlockEntity.class).forceRunning();
	}

	/** A part placed facing {@code facing}, its cells grown as a player's would be. */
	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> T part(GameTestHelper helper, Block block, BlockPos pos, Direction facing, Class<T> type) {
		helper.setBlock(pos, block.defaultBlockState().setValue(BigPartBlock.FACING, facing));
		BigParts.grow(helper.getLevel(), helper.absolutePos(pos), helper.getLevel().getBlockState(helper.absolutePos(pos)));
		return type == null ? null : helper.getBlockEntity(pos, type);
	}

	/**
	 * A bottom-dump station on the north-running track at x 3 (bottom-dump-cars.md): the track grizzly under the rails from z {@code z}
	 * to {@code z + 3}, the cam rail beside it on the east, and the dump pocket under the grizzly, turned so its 4 m run along the track.
	 */
	private static DumpBlockEntity grizzlyStation(GameTestHelper helper, int y, int z) {
		part(helper, TailingsBlocks.TRACK_GRIZZLY, new BlockPos(3, y - 1, z), Direction.NORTH, PlantBlockEntity.class);
		part(helper, TailingsBlocks.DUMP_CAM_RAIL, new BlockPos(4, y, z), Direction.NORTH, PlantBlockEntity.class);
		DumpBlockEntity pocket = part(helper, TailingsBlocks.DUMP_POCKET, new BlockPos(4, y - 4, z), Direction.EAST, DumpBlockEntity.class);
		for (int k = 0; k < 4; k++) {
			helper.setBlock(new BlockPos(3, y, z + k), rail());
		}
		return pocket;
	}

	private static MineVehicleEntity loco(GameTestHelper helper, BlockPos at, Heading heading, EntityType<MineVehicleEntity> type) {
		MineVehicleEntity loco = MineVehicleEntity.placeLocomotive(helper.getLevel(), helper.absolutePos(at), heading, type);
		helper.assertTrue(loco != null, Component.literal("a locomotive should go on the rail at " + at));
		return loco;
	}

	private static MineVehicleEntity car(GameTestHelper helper, BlockPos at, Heading heading, EntityType<MineVehicleEntity> type) {
		MineVehicleEntity car = MineVehicleEntity.placeCar(helper.getLevel(), helper.absolutePos(at), heading, type);
		helper.assertTrue(car != null, Component.literal("a car should go on the rail at " + at));
		return car;
	}

	/** {@code count} cars behind a locomotive whose centre is at z {@code locoZ} + 0.5, at their coupled places (going +z). */
	private static List<MineVehicleEntity> cars(GameTestHelper helper, int y, int locoZ, int count, int sign, Heading heading, EntityType<MineVehicleEntity> type) {
		List<MineVehicleEntity> out = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			double slot = locoZ + 0.5 + sign * Haul.carOffset(i);
			out.add(car(helper, new BlockPos(3, y, (int) Math.floor(slot)), heading, type));
		}
		return out;
	}

	private static double relZ(GameTestHelper helper, MineVehicleEntity e) {
		return e.getZ() - helper.absolutePos(BlockPos.ZERO).getZ();
	}

	private static double relY(GameTestHelper helper, MineVehicleEntity e) {
		return e.getY() - helper.absolutePos(BlockPos.ZERO).getY();
	}

	// ---- L2: the battery locomotive ---------------------------------------------------------------------------------------------

	/**
	 * A battery locomotive pulls 10 loaded cars (48 t) on track with no rectifier to the dump (battery-locomotive.md, battery_loco_run):
	 * every car tips, the track stays dead, and the battery pays for the run.
	 */
	private static void batteryRun(GameTestHelper helper) {
		force(helper, -1, 5);
		track(helper, Y, 2, 70);
		DumpBlockEntity pocket = TailingsGameTests.part(helper, TailingsBlocks.DUMP_POCKET, new BlockPos(2, Y - 3, 30), DumpBlockEntity.class);
		TailingsGameTests.part(helper, TailingsBlocks.DUMP_RAMP, new BlockPos(4, Y, 30), PlantBlockEntity.class);
		List<MineVehicleEntity> cars = new ArrayList<>();
		MineVehicleEntity[] train = new MineVehicleEntity[1];
		RailBlockEntity rail = helper.getBlockEntity(new BlockPos(3, Y, 20), RailBlockEntity.class);
		int[] lifted = {0};
		// a rock breaker keeps the pocket's bars clear: 5,760 items leave about 58 pieces of oversize, more than chokes it
		helper.onEachTick(() -> lifted[0] += pocket.liftOff().values().stream().mapToInt(Integer::intValue).sum());
		helper.startSequence().thenIdle(SETTLE).thenExecute(() -> {
			// the vehicles once the forced chunks are in
			train[0] = loco(helper, new BlockPos(3, Y, 40), Heading.NORTH, TailingsEntities.BATTERY_LOCOMOTIVE);
			train[0].setBatteryWh(Battery.USABLE_WH);
			cars.addAll(cars(helper, Y, 40, Haul.MAX_CARS, 1, Heading.NORTH, TailingsEntities.CAR));
			helper.assertTrue(train[0].carIds().size() == Haul.MAX_CARS, Component.literal("ten cars should couple, got " + train[0].carIds().size()));
			for (MineVehicleEntity car : cars) {
				car.load().add("minecraft:cobblestone", Haul.CAR_ITEMS);
				car.refreshLoad();
			}
		}).thenWaitUntil(() -> {
			MineVehicleEntity loco = train[0];
			int left = cars.stream().mapToInt(c -> c.load().total()).sum();
			helper.assertTrue(left == 0, Component.literal("every car should tip on unfed track, " + left + " items left (" + loco.status() + ", at z " + relZ(helper, loco) + ")"));
			helper.assertTrue(pocket.pocket() + lifted[0] == Haul.MAX_CARS * Haul.CAR_ITEMS, Component.literal("the pocket should take all ten loads"));
			helper.assertFalse(rail.conductorLive(), Component.literal("the track has no rectifier: it is dead"));
			double used = Battery.USABLE_WH - loco.batteryWh();
			helper.assertTrue(used > 1 && used < 400, Component.literal("the run should cost the battery a few hundred Wh at most (about 1.5 kWh a km), " + used + " Wh"));
		}).thenSucceed();
	}

	/**
	 * Five metres of the 1-in-2 rack decline, heading south up it: rails for the bottom flat at {@code top - 5} up to z 33, the grade
	 * from z 34 to 43, and the top flat at {@code top} from z 44 to 50.
	 */
	private static void decline(GameTestHelper helper, int top) {
		force(helper, -1, 5);
		BlockState flat = rail().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = 2; z <= 33; z++) {
			helper.setBlock(new BlockPos(3, top - 5, z), flat);
		}
		for (int k = 0; k < 5; k++) {
			helper.setBlock(new BlockPos(3, top - k, 43 - 2 * k), flat.setValue(MineRailBlock.GRADE, RailGrade.UPPER).setValue(MineRailBlock.RACK, true));
			helper.setBlock(new BlockPos(3, top - k, 42 - 2 * k), flat.setValue(MineRailBlock.GRADE, RailGrade.LOWER).setValue(MineRailBlock.RACK, true));
		}
		for (int z = 44; z <= 50; z++) {
			helper.setBlock(new BlockPos(3, top, z), flat);
		}
	}

	/**
	 * A loaded 10-car train (48 t) climbs the decline on its battery (battery_loco_climb): about 0.20 m/s, power over force at 44 kW,
	 * and about 8 kWh for each 50 m of height (here 3 m of it is measured and scaled up).
	 */
	private static void batteryClimb(GameTestHelper helper) {
		int top = 64, bottom = top - 5;
		decline(helper, top);
		MineVehicleEntity[] train = new MineVehicleEntity[1];
		double[] seen = {-1, -1, 0, 9, 0};   // charge at 0.75 m up, at 3.75 m up, fastest and slowest between, the base
		helper.onEachTick(() -> {
			MineVehicleEntity loco = train[0];
			if (loco == null) {
				return;
			}
			double base = seen[4];
			double up = relY(helper, loco) - base;
			if (up >= 0.75 && seen[0] < 0) {
				seen[0] = loco.batteryWh();
			}
			if (up >= 3.75 && seen[1] < 0) {
				seen[1] = loco.batteryWh();
			}
			if (up > 0.75 && up < 3.75 && seen[1] < 0) {
				seen[2] = Math.max(seen[2], loco.speed());
				seen[3] = Math.min(seen[3], loco.speed());
			}
		});
		helper.startSequence().thenIdle(SETTLE).thenExecute(() -> {
			// on the foot of the grade, so it starts the climb from rest; the cars on the flat behind it
			MineVehicleEntity loco = loco(helper, new BlockPos(3, bottom + 1, 34), Heading.SOUTH, TailingsEntities.BATTERY_LOCOMOTIVE);
			loco.setBatteryWh(Battery.USABLE_WH);
			seen[4] = relY(helper, loco);
			List<MineVehicleEntity> cars = cars(helper, bottom, 34, Haul.MAX_CARS, -1, Heading.SOUTH, TailingsEntities.CAR);
			helper.assertTrue(loco.carIds().size() == Haul.MAX_CARS, Component.literal("ten cars should couple, got " + loco.carIds().size()));
			for (MineVehicleEntity car : cars) {
				car.load().add("minecraft:cobblestone", Haul.CAR_ITEMS);
				car.refreshLoad();
			}
			train[0] = loco;
		}).thenWaitUntil(() -> {
			MineVehicleEntity loco = train[0];
			helper.assertTrue(seen[1] >= 0, Component.literal("the train should climb 3.75 m, at " + loco.position() + " (" + loco.status() + ")"));
			helper.assertTrue(Math.abs(seen[2] - 0.20) < 0.03 && seen[3] > 0.15, Component.literal("it should climb at about 0.20 m/s, between " + seen[3] + " and " + seen[2]));
			double per50 = (seen[0] - seen[1]) / 3.0 * 50;
			helper.assertTrue(per50 > 7_000 && per50 < 9_000, Component.literal("about 8 kWh for 50 m of height, got " + Math.round(per50) + " Wh"));
		}).thenSucceed();
	}

	/** Run to its floor on the decline, it stops FLAT and holds on the rack (battery_loco_flat). */
	private static void batteryFlat(GameTestHelper helper) {
		int top = 64, bottom = top - 5;
		decline(helper, top);
		MineVehicleEntity loco = loco(helper, new BlockPos(3, bottom + 1, 34), Heading.SOUTH, TailingsEntities.BATTERY_LOCOMOTIVE);
		loco.setBatteryWh(50);   // about 4 s of its 52 kW
		double base = relY(helper, loco);
		double[] stopped = {Double.NaN, 0};
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(loco.status().startsWith("FLAT") && loco.speed() == 0,
						Component.literal("it should stop FLAT, got " + loco.status() + " at " + loco.speed() + " m/s")))
				.thenExecute(() -> {
					stopped[0] = relY(helper, loco);
					stopped[1] = relZ(helper, loco);
					double up = stopped[0] - base;
					helper.assertTrue(up > 0.3 && up < 4, Component.literal("it should be on the grade, " + up + " m up"));
					helper.assertTrue(loco.batteryWh() == 0, Component.literal("at its floor, got " + loco.batteryWh() + " Wh"));
				})
				.thenExecuteAfter(60, () -> helper.assertTrue(Math.abs(relY(helper, loco) - stopped[0]) < 1e-6 && Math.abs(relZ(helper, loco) - stopped[1]) < 1e-6,
						Component.literal("braked on the rack, it should not roll back")))
				.thenSucceed();
	}

	/** A 500 kW set at the industrial tier, its control panel at {@code panel} facing north, its skid to the west (as TbmGapsGameTests). */
	private static GensetControlPanelBlockEntity genset(GameTestHelper helper, BlockPos panel) {
		for (int x = -7; x <= 18; x++) {
			for (int z = -4; z <= 8; z++) {
				helper.setBlock(panel.offset(x, -1, z), net.minecraft.world.level.block.Blocks.STONE);
			}
		}
		GensetSkid.build(helper.getLevel(), helper.absolutePos(panel), Direction.NORTH, false);
		return (GensetControlPanelBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(panel));
	}

	private static void start(GameTestHelper helper, BlockPos panel) {
		if (helper.getLevel().getBlockEntity(GensetSkid.base(helper.absolutePos(panel), Direction.NORTH, false)) instanceof GensetPartBlockEntity tank) {
			tank.setFuel(2000);
		}
		((GensetControlPanelBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(panel))).setWantRun(true);
	}

	private static Block wildcatter(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", id));
	}

	/**
	 * The charger on the world's three-phase 30 A receptacle (battery_charge): dead, it charges nothing; fed from a set, it draws its
	 * 20 kW and puts 18 kW into the battery it holds; then it charges a battery locomotive standing beside it.
	 */
	private static void batteryCharge(GameTestHelper helper) {
		BlockPos panel = new BlockPos(8, 2, 3);
		genset(helper, panel);
		// the world's three-phase 30 A receptacle on the industrial tier: NEMA L22-30 at 480 V, IEC 60309 red 32 A at 400 V
		boolean na = Voltage.INDUSTRIAL.lineToLine() > 440;
		helper.setBlock(panel.east(3), wildcatter(na ? "receptacle_nema_l22_30" : "receptacle_iec_60309_red_32"));
		BatteryChargerBlockEntity charger = part(helper, TailingsBlocks.BATTERY_CHARGER, panel.east(4), Direction.NORTH, BatteryChargerBlockEntity.class);
		charger.setHeldWh(0);
		double[] mark = {0};
		helper.startSequence()
				.thenExecuteAfter(60, () -> {
					helper.assertTrue(charger.heldWh() == 0, Component.literal("on a dead receptacle it charges nothing, got " + charger.heldWh() + " Wh"));
					helper.assertTrue(charger.status().startsWith("NO POWER"), Component.literal("and says so: " + charger.status()));
					helper.assertFalse(charger.readPoint(BatteryChargerBlockEntity.CHARGING), Component.literal("CHARGING off"));
					helper.setBlock(panel.east(1), wildcatter("cable_industrial"));
					helper.setBlock(panel.east(2), wildcatter("cable_industrial"));
					start(helper, panel);
				})
				.thenWaitUntil(() -> helper.assertTrue(Math.abs(charger.lineWatts() - Battery.CHARGER_WATTS) < Battery.CHARGER_WATTS * 0.05,
						Component.literal("fed, it should draw its 20 kW: " + charger.inspect().getString())))
				.thenExecute(() -> mark[0] = charger.heldWh())
				.thenExecuteAfter(100, () -> {
					double rate = (charger.heldWh() - mark[0]) * 3600 / 5;
					helper.assertTrue(Math.abs(rate - 18_000) < 1_000, Component.literal("18 kW into the battery, got " + Math.round(rate) + " W"));
					helper.assertTrue(charger.readPoint(BatteryChargerBlockEntity.CHARGING), Component.literal("CHARGING on"));
					double ma = charger.readMa(BatteryChargerBlockEntity.CHARGE);
					helper.assertTrue(ma > 4 && ma < 4.1, Component.literal("a nearly flat battery reads just over 4 mA, got " + ma));
					// its own battery out; a battery locomotive with a flat battery stands beside it on a branch with no rectifier
					charger.setHeldWh(-1);
					for (int x = 4; x <= 10; x++) {
						helper.setBlock(panel.offset(x, 0, 1), TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.EAST));
					}
					MineVehicleEntity loco = loco(helper, panel.offset(6, 0, 1), Heading.EAST, TailingsEntities.BATTERY_LOCOMOTIVE);
					loco.setBatteryWh(0);
					mark[0] = 0;
				})
				.thenWaitUntil(() -> {
					MineVehicleEntity loco = helper.getLevel().getEntitiesOfClass(MineVehicleEntity.class, new net.minecraft.world.phys.AABB(helper.absolutePos(panel.offset(6, 0, 1))).inflate(4),
							MineVehicleEntity::battery).stream().findFirst().orElse(null);
					helper.assertTrue(loco != null && loco.batteryWh() > 5, Component.literal("the locomotive standing within 3 m should charge where it stands: " + charger.status()));
				})
				.thenSucceed();
	}

	/** The battery locomotive's model has its battery box and headlight and no collector shoe; the bottom-dump car's doors swing down. */
	private static void models(GameTestHelper helper) {
		var loco = dev.wildcatter.tailings.client.BatteryLocomotiveModel.createBodyLayer().bakeRoot();
		helper.assertTrue(loco.hasChild(dev.wildcatter.tailings.client.BatteryLocomotiveModel.BATTERY) && loco.hasChild(dev.wildcatter.tailings.client.BatteryLocomotiveModel.HEADLIGHT),
				Component.literal("a battery box and a headlight"));
		helper.assertFalse(loco.hasChild("shoe"), Component.literal("no collector shoe"));
		var car = new dev.wildcatter.tailings.client.BottomDumpCarModel(dev.wildcatter.tailings.client.BottomDumpCarModel.createBodyLayer().bakeRoot());
		car.doors(true);
		helper.assertTrue(car.root().getChild(dev.wildcatter.tailings.client.BottomDumpCarModel.DOOR_LEFT).zRot > 0.5
				&& car.root().getChild(dev.wildcatter.tailings.client.BottomDumpCarModel.DOOR_RIGHT).zRot < -0.5, Component.literal("both doors swing open"));
		car.doors(false);
		helper.assertTrue(car.root().getChild(dev.wildcatter.tailings.client.BottomDumpCarModel.DOOR_LEFT).zRot == 0F, Component.literal("and shut"));
		helper.assertFalse(car.root().getChild(dev.wildcatter.tailings.client.BottomDumpCarModel.LOAD).isEmpty(), Component.literal("it has a load cube"));
		helper.succeed();
	}

	// ---- L3: bottom-dump cars -----------------------------------------------------------------------------------------------------

	/** A train of bottom-dump cars drops every car's load into the pocket under a track grizzly, on the move (bottom_dump_on_move). */
	private static void bottomDumpOnMove(GameTestHelper helper) {
		// far enough from the end of the track that the train is not braking for it over the grizzly
		force(helper, -1, 3);
		track(helper, Y, 0, 50);
		feed(helper, Y);
		DumpBlockEntity pocket = grizzlyStation(helper, Y, 22);
		MineVehicleEntity loco = loco(helper, new BlockPos(3, Y, 40), Heading.NORTH, TailingsEntities.LOCOMOTIVE);
		List<MineVehicleEntity> cars = cars(helper, Y, 40, 3, 1, Heading.NORTH, TailingsEntities.BOTTOM_DUMP_CAR);
		for (MineVehicleEntity car : cars) {
			car.load().add("minecraft:cobblestone", 200);
			car.refreshLoad();
		}
		double[] slowest = {9};
		boolean[] opened = {false};
		helper.onEachTick(() -> {
			for (MineVehicleEntity car : cars) {
				double z = relZ(helper, car);
				if (z > 22 && z < 26) {
					slowest[0] = Math.min(slowest[0], loco.speed());
				}
				opened[0] |= car.doorsOpen();
			}
		});
		helper.succeedWhen(() -> {
			int left = cars.stream().mapToInt(c -> c.load().total()).sum();
			helper.assertTrue(left == 0, Component.literal("every car should drop its load over the grizzly, " + left + " left (" + loco.status() + ")"));
			helper.assertTrue(pocket.pocket() + pocket.oversize() == 600, Component.literal("into the pocket under it, got " + pocket.pocket()));
			helper.assertTrue(slowest[0] > 2.0, Component.literal("without stopping: slowest " + slowest[0] + " m/s over the grizzly"));
			helper.assertTrue(opened[0], Component.literal("the cam rail should open the doors"));
			helper.assertTrue(cars.stream().noneMatch(c -> relZ(helper, c) < 21.5 && c.doorsOpen()), Component.literal("and the closing ramp shut them past it"));
		});
	}

	/** With the pocket full the train stops with the car over the grizzly, doors shut, and goes on when the feeder makes room (bottom_dump_full). */
	private static void bottomDumpFull(GameTestHelper helper) {
		track(helper, Y, 0, 20);
		feed(helper, Y);
		DumpBlockEntity pocket = grizzlyStation(helper, Y, 10);
		helper.assertTrue(pocket.offer(Map.of("minecraft:cobblestone", Haul.POCKET_ITEMS)), Component.literal("fill the pocket"));
		pocket.liftOff();   // only the full pocket holds the car
		MineVehicleEntity loco = loco(helper, new BlockPos(3, Y, 7), Heading.NORTH, TailingsEntities.LOCOMOTIVE);
		MineVehicleEntity car = cars(helper, Y, 7, 1, 1, Heading.NORTH, TailingsEntities.BOTTOM_DUMP_CAR).get(0);
		car.load().add("minecraft:copper_ore", 64);
		car.refreshLoad();
		double start = relZ(helper, loco);
		helper.startSequence()
				.thenExecuteAfter(40, () -> {
					helper.assertTrue(Math.abs(relZ(helper, loco) - start) < 0.01 && loco.status().contains("dump"),
							Component.literal("the train should wait with the car over the grizzly, got " + loco.status()));
					helper.assertTrue(car.load().total() == 64 && !car.doorsOpen(), Component.literal("its doors shut, its load in it"));
					pocket.draw(64);   // the apron feeder draws a stack out
				})
				.thenWaitUntil(() -> helper.assertTrue(car.load().total() == 0 && pocket.held("minecraft:copper_ore") > 0,
						Component.literal("with room, the car drops its load")))
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, loco) < start - 1, Component.literal("and the train goes on, at z " + relZ(helper, loco))))
				.thenSucceed();
	}

	/**
	 * A mixed train dumps each car at its own station (bottom_dump_mixed): its Granby car crosses the track grizzly loaded and tips at
	 * the side-dump ramp; its bottom-dump car drops its load over the grizzly and passes the ramp without tipping.
	 */
	private static void bottomDumpMixed(GameTestHelper helper) {
		force(helper, -1, 3);
		track(helper, Y, 0, 45);
		feed(helper, Y);
		DumpBlockEntity grizzly = grizzlyStation(helper, Y, 18);
		DumpBlockEntity side = TailingsGameTests.part(helper, TailingsBlocks.DUMP_POCKET, new BlockPos(2, Y - 3, 4), DumpBlockEntity.class);
		TailingsGameTests.part(helper, TailingsBlocks.DUMP_RAMP, new BlockPos(4, Y, 4), PlantBlockEntity.class);
		MineVehicleEntity loco = loco(helper, new BlockPos(3, Y, 32), Heading.NORTH, TailingsEntities.LOCOMOTIVE);
		MineVehicleEntity granby = car(helper, new BlockPos(3, Y, 35), Heading.NORTH, TailingsEntities.CAR);
		MineVehicleEntity bottom = car(helper, new BlockPos(3, Y, 38), Heading.NORTH, TailingsEntities.BOTTOM_DUMP_CAR);
		helper.assertTrue(loco.carIds().size() == 2, Component.literal("both cars should couple"));
		granby.load().add("minecraft:copper_ore", 64);
		granby.refreshLoad();
		bottom.load().add("minecraft:cobblestone", 64);
		bottom.refreshLoad();
		helper.succeedWhen(() -> {
			helper.assertTrue(granby.load().total() == 0 && bottom.load().total() == 0, Component.literal("both cars should dump (" + loco.status() + ")"));
			helper.assertTrue(grizzly.held("minecraft:cobblestone") == 64 && grizzly.held("minecraft:copper_ore") == 0,
					Component.literal("the grizzly takes only the bottom-dump car's cobble"));
			helper.assertTrue(side.held("minecraft:copper_ore") + side.oversize() == 64 && side.held("minecraft:cobblestone") == 0,
					Component.literal("the ramp's pocket only the Granby car's copper"));
		});
	}
}
