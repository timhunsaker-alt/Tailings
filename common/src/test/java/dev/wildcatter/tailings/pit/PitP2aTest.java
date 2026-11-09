package dev.wildcatter.tailings.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Fuel, the leg survey and the autopilot (docs/multiblocks/open-pit.md, P2a). */
class PitP2aTest {
	@Test
	void flatOutTheTruckBurnsAbout200LitresAnHour() {
		double perHour = PitFuel.litres(PitDrive.TRUCK.watts(), 3600);
		assertEquals(201, perHour, 1);
	}

	@Test
	void drivingPowerIsTheWheelsOverTheDriveHeldBetweenIdleAndRated() {
		// 161 t loaded at 3.24 m/s up 10 %: about all the engine has (the spec's 12 km/h)
		double climbing = PitFuel.drivingWatts(PitDrive.TRUCK, 161_000, 3.24, 3.24, 0.10, 0.05);
		assertEquals(PitDrive.TRUCK.watts(), climbing, 30_000);
		// coasting down: idle
		double coasting = PitFuel.drivingWatts(PitDrive.TRUCK, 70_000, 5, 4.9, -0.05, 0.05);
		assertEquals(PitDrive.TRUCK.watts() * PitFuel.IDLE, coasting, 1);
	}

	private static Survey.Ground blocks(Set<String> solid) {
		return (x, y, z) -> solid.contains(x + "," + y + "," + z);
	}

	@Test
	void anOpenFlatLegIsClearAndTwoWay() {
		Survey.Leg leg = Survey.leg(blocks(Set.of()), 0, 60, 0, 0, 60, 40, 6.2, 5.2);
		assertNull(leg.problem());
		assertFalse(leg.oneWay());
		assertEquals(0, leg.grade(), 1e-9);
	}

	@Test
	void aLegOver10PercentIsSteep() {
		Survey.Leg ok = Survey.leg(blocks(Set.of()), 0, 60, 0, 0, 64, 40, 6.2, 5.2);
		assertNull(ok.problem(), "10 % is the most");
		Survey.Leg steep = Survey.leg(blocks(Set.of()), 0, 60, 0, 0, 66, 40, 6.2, 5.2);
		assertNotNull(steep.problem());
		assertTrue(steep.problem().contains("15 %"), steep.problem());
	}

	@Test
	void aBlockInTheTrucksBoxBlocksTheLegAndOneBesideItMakesItOneWay() {
		Survey.Leg blocked = Survey.leg(blocks(Set.of("1,62,20")), 0, 60, 0, 0, 60, 40, 6.2, 5.2);
		assertNotNull(blocked.blockedAt());
		assertTrue(blocked.problem().startsWith("blocked at 1, 62, 20"), blocked.problem());
		// a wall 6 m to the side: clear for one truck, not for three widths
		Set<String> wall = new HashSet<>();
		for (int z = 0; z <= 40; z++) {
			wall.add("6," + 61 + "," + z);
		}
		Survey.Leg narrow = Survey.leg(blocks(wall), 0, 60, 0, 0, 60, 40, 6.2, 5.2);
		assertNull(narrow.problem());
		assertTrue(narrow.oneWay());
	}

	@Test
	void itSteersItsNoseForwardAndItsTailInReverse() {
		// facing +z (yaw 0), the target off to -x (its right is -x): forward it turns right, its yaw growing
		double forward = Autopilot.steer(0, 0, 0, -10, 10, false, 5, 12.5);
		double rate = -forward * 5 / 12.5;
		assertTrue(rate > 0, "its yaw should grow toward 45, steer " + forward);
		assertEquals(45, Autopilot.yawToward(0, 0, -10, 10), 1e-9);
		// reversing to a point behind and to the left (+x, -z): the tail swings to it, so the yaw grows toward 45 too
		double back = Autopilot.steer(0, 0, 0, 10, -10, true, -2, 12.5);
		double backRate = -back * -2 / 12.5;
		assertTrue(backRate > 0, "reversing its yaw should grow, steer " + back);
		// on line: no steer
		assertEquals(0, Autopilot.steer(0, 0, 0, 0, 30, false, 5, 12.5), 1e-9);
	}

	@Test
	void itPlansToStopInTheDistanceItHas() {
		double v = Autopilot.stoppingSpeed(20);
		assertEquals(20, Autopilot.stoppingDistance(v), 1e-9);
		assertEquals(1, Autopilot.throttle(0, 10, 1));
		assertEquals(-1, Autopilot.throttle(10, 0, 1), "brake to stop");
		assertEquals(0, Autopilot.throttle(0, 0, 1), "stand");
		assertEquals(-1, Autopilot.throttle(0, 3, -1), "reverse");
		assertEquals(1, Autopilot.throttle(-3, 0, -1), "brake reversing");
	}
}
