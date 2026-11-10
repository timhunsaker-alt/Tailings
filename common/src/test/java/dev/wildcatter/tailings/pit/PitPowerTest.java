package dev.wildcatter.tailings.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The pit switch house's and the trailing cables' numbers (docs/multiblocks/open-pit-p5-big-machines.md, P5a). */
class PitPowerTest {
	/** 4.16 kV, the NA standard's medium voltage, for the arithmetic only: the game reads it from the world's standard. */
	private static final double KV416 = 4_160;

	@Test
	void theDraglinesPeakFitsAnOutlet() {
		double amps = PitPower.amps(6_000_000, KV416, 1.0);
		assertEquals(833, amps, 1, "the spec: the dragline's 6 MW peak is about 830 A at 4.16 kV");
		assertTrue(amps < PitPower.OUTLET_AMPS);
	}

	@Test
	void anOutletTripsAtOneAndAQuarterTimesItsPeak() {
		double peak = PitPower.amps(PitPower.ROPE_HOIST_WATTS, KV416, PitPower.POWER_FACTOR);
		assertEquals(511, peak, 1, "the rope shovel's 3.5 MW hoist at 0.95");
		assertEquals(1.25 * peak, PitPower.tripAmps(PitPower.ROPE_HOIST_WATTS, KV416), 1e-6);
		assertEquals(PitPower.OUTLET_AMPS, PitPower.tripAmps(20_000_000, KV416), 1e-6, "never over the outlet's 1,200 A");
		assertEquals(2_500, PitPower.busTripAmps(), 1e-9);
	}

	@Test
	void aBreakerTripsAfterASecondOver() {
		double over = 0;
		for (int i = 0; i < 3; i++) {
			over = PitPower.over(over, 700, 640, 0.25);
		}
		assertFalse(PitPower.trips(over), "0.75 s over is not yet a trip");
		over = PitPower.over(over, 700, 640, 0.25);
		assertTrue(PitPower.trips(over));
		assertEquals(0, PitPower.over(0.9, 600, 640, 0.25), "under the pickup it starts again");
	}

	@Test
	void aCableIsATenthOfAnOhmAKilometre() {
		assertEquals(0.04, PitPower.cableOhms(400), 1e-9);
		// the rope shovel's hoist on four reels: about 3 % drop
		double amps = PitPower.amps(PitPower.ROPE_HOIST_WATTS, KV416, PitPower.POWER_FACTOR);
		double drop = Math.sqrt(3) * amps * PitPower.cableOhms(1_600) * PitPower.POWER_FACTOR;
		assertEquals(0.034, drop / KV416, 0.005);
	}

	@Test
	void theCablePaysOutAlongThePath() {
		// straight away from the house: what it moved
		assertEquals(110, PitPower.payOut(100, 100, 110, 10), 1e-9);
		// across, at the same distance: still what it moved
		assertEquals(105, PitPower.payOut(100, 100, 100, 5), 1e-9);
		// back toward the house: reeled in
		assertEquals(95, PitPower.payOut(100, 100, 95, 5), 1e-9);
		// never less than the straight line
		assertEquals(98, PitPower.payOut(100, 100, 98, 10), 1e-9);
	}

	@Test
	void theRopeShovelAveragesAboutOneAndAHalfMegawatts() {
		assertEquals(3_500_000, PitPower.ropeShovelWatts(5), 1e-9);
		assertEquals(600_000, PitPower.ropeShovelWatts(20), 1e-9);
		assertEquals(1_594_000, PitPower.ropeShovelAverage(35), 1_000);
	}

	@Test
	void lowVoltageIsUnderNinetyPercent() {
		assertTrue(PitPower.enough(0.9 * KV416, KV416));
		assertFalse(PitPower.enough(0.89 * KV416, KV416));
	}
}
