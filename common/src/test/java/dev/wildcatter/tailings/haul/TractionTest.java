package dev.wildcatter.tailings.haul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The speeds in docs/multiblocks/haulage.md, "Speed follows power". */
class TractionTest {
	private static final double UP_ONE_IN_TWO = 0.5 / Math.sqrt(1.25);

	@Test
	void upTheDeclineSpeedIsPowerOverForce() {
		assertEquals(0.34, Traction.limit(48_000, UP_ONE_IN_TWO, Haul.TRACTION_WATTS), 0.01, "loaded, 10 cars");
		assertEquals(0.92, Traction.limit(18_000, UP_ONE_IN_TWO, Haul.TRACTION_WATTS), 0.01, "empty, 10 cars");
		assertEquals(2.06, Traction.limit(8_000, UP_ONE_IN_TWO, Haul.TRACTION_WATTS), 0.01, "the locomotive alone");
		assertEquals(1.2, Traction.limit(14_000, UP_ONE_IN_TWO, Haul.TRACTION_WATTS), 0.05, "the game test's 14 t");
	}

	@Test
	void theFlatIsCappedAndTheDeclineHeldByTheRack() {
		assertEquals(Haul.MAX_SPEED, Traction.limit(48_000, 0, Haul.TRACTION_WATTS));
		assertEquals(Haul.DOWNHILL_SPEED, Traction.limit(48_000, -UP_ONE_IN_TWO, Haul.TRACTION_WATTS));
		assertEquals(14_000, Traction.watts(48_000, 0, 3), 200, "a loaded train draws about 14 kW to roll at 3 m/s");
	}

	@Test
	void itSpeedsUpAndStopsAtThreeTenthsOfAMetrePerSecondSquared() {
		assertEquals(0.3, Traction.approach(0, 3, 1), 1e-9);
		assertEquals(2.7, Traction.approach(3, 0, 1), 1e-9);
		assertEquals(15, Traction.brakingDistance(3), 1e-9);
		assertEquals(3, Traction.stoppable(15), 1e-9);
	}

	@Test
	void aTrainsMassCountsItsLoads() {
		assertEquals(48_000, Traction.mass(10, 10 * Haul.CAR_ITEMS), 1e-6);
		assertEquals(18_000, Traction.mass(10, 0), 1e-6);
		assertTrue(Traction.mass(3, Haul.CAR_ITEMS) > Traction.mass(3, 0));
	}

	@Test
	void theRectifierIsSizedFromItsRating() {
		assertEquals(1_200, Haul.RECTIFIER_AMPS, 1e-9, "300 kW at 250 V");
		assertEquals(0.0042, Haul.RECTIFIER_OHMS, 0.0001, "a 2 % drop at its rating");
		assertEquals(5.0, Haul.RECTIFIER_OHMS * Haul.RECTIFIER_AMPS, 1e-9);
		assertEquals(1_500, Haul.TRIP_SHARE * Haul.RECTIFIER_AMPS, 1e-9, "it trips at 1,500 A held for a second");
		assertEquals(296, Haul.TRACTION_WATTS / Haul.TRACTION_VOLTS, 1, "a locomotive's 74 kW is 296 A");
		assertEquals(0.002, Haul.CONDUCTOR_OHMS_PER_METRE * 100, 1e-12, "100 m of conductor rail and return");
	}
}
