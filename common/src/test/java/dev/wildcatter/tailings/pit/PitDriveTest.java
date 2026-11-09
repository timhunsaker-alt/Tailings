package dev.wildcatter.tailings.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The haul truck's speed against its spec (docs/multiblocks/open-pit.md, Haul trucks). */
class PitDriveTest {
	private static double settle(PitDrive.Spec spec, double mass, double throttle, double slope) {
		double v = 0;
		for (int tick = 0; tick < 20 * 120; tick++) {
			v = PitDrive.next(spec, mass, v, throttle, slope, 0.05);
		}
		return v;
	}

	@Test
	void onTheFlatItReachesItsTopSpeed() {
		assertEquals(60 / 3.6, settle(PitDrive.TRUCK, 70_000, 1, 0), 0.01);
	}

	@Test
	void loadedUpATenPercentRoadItClimbsAtAboutTwelveKmH() {
		double slope = Math.sin(Math.atan(0.10));
		double v = settle(PitDrive.TRUCK, 70_000 + 91_000, 1, slope);
		// P η / (m g (sin θ + 0.02)) = 612 kW / (161 t × 9.81 × 0.1195) = 3.24 m/s, 11.7 km/h
		assertEquals(3.24, v, 0.05);
	}

	@Test
	void downASteepRoadTheRetarderHoldsIt() {
		double slope = -Math.sin(Math.atan(0.10));
		double v = settle(PitDrive.TRUCK, 161_000, 1, slope);
		assertEquals(15 / 3.6, v, 0.01);
	}

	@Test
	void standingWithNoThrottleItHoldsOnAGrade() {
		assertEquals(0, PitDrive.next(PitDrive.TRUCK, 161_000, 0, 0, 0.1, 0.05));
	}

	@Test
	void backWhileRollingBrakes() {
		double v = PitDrive.next(PitDrive.TRUCK, 161_000, 10, -1, 0, 0.05);
		assertEquals(10 - 3.0 * 0.05, v, 1e-9);
	}

	@Test
	void itReversesSlowly() {
		assertTrue(settle(PitDrive.TRUCK, 70_000, -1, 0) >= -3.0 - 1e-9);
		assertEquals(-3.0, settle(PitDrive.TRUCK, 70_000, -1, 0), 0.01);
	}
}
