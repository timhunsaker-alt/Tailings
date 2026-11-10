package dev.wildcatter.tailings.neoforge.gametest;

import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.floor;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.part;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.relZ;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.stake;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.vehicle;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.where;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.HaulDispatchBlockEntity;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.pit.Dispatch;
import dev.wildcatter.tailings.pit.StopKind;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.decoration.ArmorStand;

/**
 * The open pit's P2b (docs/multiblocks/open-pit.md): the haul dispatch office on the PLC. The scenes stand in for the PLC by writing
 * the office's RUN every tick, as a scanning PLC's output module does, and stop writing it to be a PLC that has stopped.
 */
final class PitDispatchGameTests {
	static final String[] SCENES = {"pit_dispatch", "pit_dispatch_alarms", "pit_dispatch_fallback"};
	static final Map<String, Integer> TICKS = Map.of("pit_dispatch", 7200, "pit_dispatch_alarms", 2400, "pit_dispatch_fallback", 4000);
	static final Map<String, Integer> PADDING = Map.of("pit_dispatch", 180, "pit_dispatch_fallback", 150);

	private PitDispatchGameTests() {
	}

	static int[] reach(String scene) {
		return switch (scene) {
			case "pit_dispatch" -> new int[] {-3, 5, -1, 11};
			case "pit_dispatch_fallback" -> new int[] {-3, 5, -1, 9};
			case "pit_dispatch_alarms" -> new int[] {-1, 3, -1, 5};
			default -> java.util.Arrays.asList(PitDryStackGameTests.SCENES).contains(scene) ? PitDryStackGameTests.reach(scene) : PitBlastGameTests.reach(scene);
		};
	}

	private static int point(int route, int kind) {
		return route * Dispatch.POINTS_PER_ROUTE + kind;
	}

	private static HaulDispatchBlockEntity office(GameTestHelper helper, BlockPos at, boolean auto) {
		HaulDispatchBlockEntity office = part(helper, TailingsBlocks.HAUL_DISPATCH, at, HaulDispatchBlockEntity.class);
		office.forcePowered(true);
		if (auto) {
			office.cycleHoa(helper.absolutePos(at));   // HAND to OFF
			office.cycleHoa(helper.absolutePos(at));   // to AUTO
		}
		return office;
	}

	/** The spec's scene: a PLC turns a route's RUN on and off; the truck runs, tips (LOADS pulses), parks at the PARK stake with its engine off, and goes back to work. */
	static void dispatch(GameTestHelper helper) {
		floor(helper, -25, 65, 56, 58, 0, 165);
		ServerLevel level = helper.getLevel();
		List<BlockPos> route = List.of(stake(helper, 20, 59, 0, StopKind.LOAD), stake(helper, 20, 59, 30, StopKind.WAYPOINT), stake(helper, 20, 59, 70, StopKind.PARK),
				stake(helper, 20, 59, 110, StopKind.WAYPOINT), stake(helper, 20, 59, 140, StopKind.DUMP_POINT));
		HaulDispatchBlockEntity office = office(helper, new BlockPos(50, 59, 4), true);
		helper.assertTrue(office.addRoute(route).startsWith("route 1"), Component.literal("the book's route should be route 1"));
		helper.assertTrue(office.points().size() == 43, Component.literal("8 routes of 5 points and the fleet's 3: " + office.points().size()));
		boolean[] plcRun = {true};
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 16.5, 59, 40, 0);
		helper.onEachTick(() -> office.commandPoint(point(0, Dispatch.RUN), plcRun[0], level.getGameTime()));
		truck.setFuel(truck.tank());
		truck.load().add("minecraft:sand", 30_000);
		truck.refreshLoad();
		helper.assertTrue(truck.takeRoute(level, route) == null, Component.literal("the truck should take the route"));
		double[] fuel = new double[1];
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(office.readPulses(point(0, Dispatch.LOADS)) == 1, Component.literal("RUN on, it should haul to the dump point and tip: one pulse on LOADS; " + where(helper, truck))))
				.thenExecute(() -> {
					helper.assertTrue(office.readPoint(point(0, Dispatch.RUNNING)), Component.literal("the route reads RUNNING"));
					helper.assertTrue(Math.abs(office.readMa(point(0, Dispatch.TRUCKS)) - 5) < 1e-6, Component.literal("one truck is 5 mA: " + office.readMa(point(0, Dispatch.TRUCKS))));
					double ma = office.readMa(point(0, Dispatch.TONNES));
					helper.assertTrue(ma > 4.1 && ma <= Dispatch.tonnesMa(30) + 1e-6, Component.literal("what it tipped, up to 30 t, in the hour on TONNES: " + ma));
					plcRun[0] = false;   // the PLC turns RUN off
				})
				.thenWaitUntil(() -> helper.assertTrue(truck.parked(), Component.literal("RUN off, it should drive back to the PARK stake and park: " + where(helper, truck))))
				.thenExecute(() -> {
					helper.assertTrue(Math.abs(relZ(helper, truck) - 70) < 7, Component.literal("it should park at the PARK stake: " + where(helper, truck)));
					helper.assertTrue(truck.status().startsWith("parked at stake 3 (PARK): RUN off"), Component.literal("it reads why: " + truck.status()));
				})
				.thenWaitUntil(() -> helper.assertFalse(office.readPoint(point(0, Dispatch.RUNNING)), Component.literal("parked, the route is not RUNNING")))
				.thenWaitUntil(() -> helper.assertTrue(truck.speed() == 0, Component.literal("it rolls to a stand: " + where(helper, truck))))
				.thenIdle(2)
				.thenExecute(() -> fuel[0] = truck.fuel())
				.thenIdle(60)
				.thenExecute(() -> {
					helper.assertTrue(!truck.engineOn() && truck.fuel() == fuel[0], Component.literal("parked, its engine is off and burns nothing: " + (fuel[0] - truck.fuel())));
					helper.assertTrue(Math.abs(office.readMa(point(0, Dispatch.TRUCKS)) - 5) < 1e-6, Component.literal("still one truck on the route"));
					plcRun[0] = true;   // and on again
				})
				.thenWaitUntil(() -> helper.assertTrue(!truck.parked() && relZ(helper, truck) < 55, Component.literal("RUN on, it should go back to work: " + where(helper, truck))))
				.thenExecute(() -> helper.assertTrue(office.readPoint(point(0, Dispatch.RUNNING)), Component.literal("and the route reads RUNNING")))
				.thenSucceed();
	}

	/** FUEL LOW for a truck under 15 %; BLOCKED for one stopped by an obstacle over 30 s, clearing when it goes on. */
	static void alarms(GameTestHelper helper) {
		floor(helper, 0, 45, 56, 58, 0, 75);
		ServerLevel level = helper.getLevel();
		List<BlockPos> route = List.of(stake(helper, 20, 59, 2, StopKind.LOAD), stake(helper, 20, 59, 26, StopKind.WAYPOINT), stake(helper, 20, 59, 38, StopKind.DUMP_POINT));
		HaulDispatchBlockEntity office = office(helper, new BlockPos(38, 59, 62), false);   // HAND: its routes run with no PLC
		office.addRoute(route);
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 16.5, 59, 7, 0);
		truck.load().add("minecraft:sand", 30_000);
		truck.refreshLoad();
		truck.setFuel(truck.tank() * 0.5);
		ArmorStand stand = helper.spawn(net.minecraft.world.entity.EntityTypes.ARMOR_STAND, new BlockPos(16, 59, 19));
		helper.assertTrue(truck.takeRoute(level, route) == null, Component.literal("the route should be taken"));
		helper.startSequence()
				.thenIdle(60)
				.thenExecute(() -> {
					helper.assertFalse(office.readPoint(Dispatch.FUEL_LOW), Component.literal("half a tank is not FUEL LOW"));
					helper.assertTrue(office.readPoint(point(0, Dispatch.RUNNING)), Component.literal("in HAND the route runs: " + where(helper, truck)));
					truck.setFuel(truck.tank() * 0.10);
				})
				.thenWaitUntil(() -> helper.assertTrue(office.readPoint(Dispatch.FUEL_LOW), Component.literal("under 15 % should read FUEL LOW")))
				.thenExecute(() -> helper.assertFalse(office.readPoint(Dispatch.BLOCKED), Component.literal("not BLOCKED yet: " + where(helper, truck))))
				.thenWaitUntil(() -> helper.assertTrue(office.readPoint(Dispatch.BLOCKED), Component.literal("stopped for the stand 30 s, the fleet reads BLOCKED: " + where(helper, truck))))
				.thenExecute(stand::discard)
				.thenWaitUntil(() -> helper.assertFalse(office.readPoint(Dispatch.BLOCKED), Component.literal("the way clear, BLOCKED should clear: " + where(helper, truck))))
				.thenSucceed();
	}

	/**
	 * The fallbacks: in AUTO with the PLC's RUN no longer coming, the route parks (with no PARK stake, at the next stake); back in HAND
	 * it runs; with no power the radio is dead and the truck stops at its next stake, its loops reading a failed 0 mA.
	 */
	static void fallback(GameTestHelper helper) {
		floor(helper, -25, 65, 56, 58, 0, 135);
		ServerLevel level = helper.getLevel();
		List<BlockPos> route = List.of(stake(helper, 20, 59, 0, StopKind.LOAD), stake(helper, 20, 59, 40, StopKind.WAYPOINT), stake(helper, 20, 59, 80, StopKind.WAYPOINT),
				stake(helper, 20, 59, 120, StopKind.DUMP_POINT));
		BlockPos officeAt = new BlockPos(50, 59, 4);
		HaulDispatchBlockEntity office = office(helper, officeAt, true);
		office.addRoute(route);
		boolean[] plc = {true};
		helper.onEachTick(() -> {
			if (plc[0]) {
				office.commandPoint(point(0, Dispatch.RUN), true, level.getGameTime());
			}
		});
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 16.5, 59, 6, 0);
		truck.setFuel(truck.tank());
		truck.load().add("minecraft:sand", 30_000);
		truck.refreshLoad();
		helper.assertTrue(truck.takeRoute(level, route) == null, Component.literal("the route should be taken"));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, truck) > 14, Component.literal("RUN on, it should drive: " + where(helper, truck))))
				.thenExecute(() -> plc[0] = false)   // the PLC stops scanning: no more commands
				.thenWaitUntil(() -> helper.assertTrue(truck.parked(), Component.literal("with no RUN coming, the route should park: " + where(helper, truck))))
				.thenExecute(() -> {
					double z = relZ(helper, truck);
					helper.assertTrue(Math.abs(z - 40) < 7 || Math.abs(z - 80) < 7, Component.literal("with no PARK stake it stops at its next stake: " + where(helper, truck)));
					helper.assertTrue(truck.status().endsWith("no RUN from the PLC"), Component.literal("it reads why: " + truck.status()));
					office.cycleHoa(helper.absolutePos(officeAt));   // AUTO to HAND
				})
				.thenWaitUntil(() -> helper.assertTrue(!truck.parked() && Math.abs(truck.speed()) > 1, Component.literal("in HAND it should run again: " + where(helper, truck))))
				.thenExecute(() -> office.forcePowered(false))
				.thenWaitUntil(() -> helper.assertTrue(truck.parked(), Component.literal("with the office's power off, it should stop at its next stake: " + where(helper, truck))))
				.thenExecute(() -> {
					helper.assertTrue(truck.status().contains("no word from dispatch: the office has no power"), Component.literal("it reads why: " + truck.status()));
					helper.assertTrue(office.readMa(point(0, Dispatch.TRUCKS)) == 0, Component.literal("unpowered, its loops read 0 mA, a failed loop"));
					office.forcePowered(true);
				})
				.thenWaitUntil(() -> helper.assertFalse(truck.parked(), Component.literal("power back, it should go on: " + where(helper, truck))))
				.thenSucceed();
	}
}
