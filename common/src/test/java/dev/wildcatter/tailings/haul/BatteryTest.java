package dev.wildcatter.tailings.haul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The figures in docs/multiblocks/battery-locomotive.md (L2). */
class BatteryTest {
	private static final double UP_ONE_IN_TWO = 0.5 / Math.sqrt(1.25);

	@Test
	void theBatteryIs58KilowattHoursAnd46Usable() {
		assertEquals(58_080, Battery.NAMEPLATE_WH, 1e-6, "132 V x 440 Ah");
		assertEquals(46_464, Battery.USABLE_WH, 1e-6, "80 % of it");
		assertEquals(44_000, Battery.TRACTION_WATTS, 1e-6, "2 x 22 kW");
	}

	@Test
	void speedFollowsItsFortyFourKilowatts() {
		assertEquals(0.20, Traction.limit(48_000, UP_ONE_IN_TWO, Battery.TRACTION_WATTS), 0.01, "loaded, 10 cars");
		assertEquals(0.55, Traction.limit(18_000, UP_ONE_IN_TWO, Battery.TRACTION_WATTS), 0.01, "empty, 10 cars");
		assertEquals(1.2, Traction.limit(8_000, UP_ONE_IN_TWO, Battery.TRACTION_WATTS), 0.05, "alone");
		assertEquals(Haul.MAX_SPEED, Traction.limit(48_000, 0, Battery.TRACTION_WATTS), 1e-9, "the flat is capped");
	}

	@Test
	void climbingAndRollingUseWhatTheSpecSays() {
		// 100 m of height up the 1-in-2 decline is about 224 m of track
		double run = 100 * Math.sqrt(5);
		assertEquals(16_000, Battery.climbWh(48_000, 100, run), 500, "a loaded train up 100 m of height: about 16 kWh");
		assertEquals(8_000, Battery.climbWh(48_000, 50, run / 2), 250, "and up 50 m about 8 kWh");
		// on the flat at 3 m/s a loaded train's motors give about 14 kW: over the 85 % drive, about 1.5 kWh a km
		double perKm = Battery.drawWh(Traction.watts(48_000, 0, 3), 1000 / 3.0);
		assertEquals(1_540, perKm, 30);
	}

	@Test
	void aFlatBatteryChargesInAboutTwoAndAHalfHours() {
		assertEquals(2.6, Battery.hoursToCharge(), 0.05);
		assertEquals(5.0, Battery.chargeWh(20_000, 1), 1e-9, "18 kW into the battery: 5 Wh a second");
	}

	@Test
	void itReadsLowUnderAFifthAndStopsAtTheFloor() {
		assertTrue(Battery.low(0.19 * Battery.USABLE_WH));
		assertFalse(Battery.low(0.21 * Battery.USABLE_WH));
		assertTrue(Battery.flat(0));
		assertFalse(Battery.flat(1));
		assertEquals(4, Battery.chargeMa(0), 1e-9);
		assertEquals(20, Battery.chargeMa(Battery.USABLE_WH), 1e-9);
		assertEquals(0, Battery.drawWh(-5_000, 10), 1e-9, "no regeneration down the decline");
	}
}
