package dev.wildcatter.tailings.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The walking dragline's numbers and its strip (docs/multiblocks/dragline.md, P6). */
class DraglineTest {
	/** 4.16 kV, the NA standard's medium voltage, for the arithmetic only: the game reads it from the world's standard. */
	private static final double KV416 = 4_160;

	@Test
	void itsCycleIsFiftyFiveSeconds() {
		assertEquals(55 * 20, Dragline.CYCLE_TICKS);
		assertEquals(65.5, Dragline.cyclesPerHour(), 0.1, "about 65 cycles an hour (spec)");
		assertEquals(1_636, Dragline.bankPerHour(), 1, "about 1,600 m³ of bank an hour (spec)");
	}

	@Test
	void itDrawsTwoAndAHalfMegawattsOnAverageAndSixAtItsPeak() {
		assertEquals(2.51e6, Dragline.cycleAverageWatts(), 0.01e6, "about 2.5 MW average (spec)");
		assertEquals(6e6, Dragline.watts(Dragline.Phase.FILL));
		assertEquals(Dragline.PEAK_WATTS, Dragline.watts(Dragline.phase(0)));
		assertEquals(Dragline.REST_WATTS, Dragline.watts(Dragline.phase(Dragline.FILL_TICKS)));
		assertEquals(Dragline.Phase.DUMP, Dragline.phase(Dragline.FILL_TICKS + Dragline.SWING_OUT_TICKS));
		assertEquals(Dragline.Phase.SWING_BACK, Dragline.phase(Dragline.CYCLE_TICKS - 1));
		assertEquals(Dragline.Phase.IDLE, Dragline.phase(Dragline.CYCLE_TICKS));
	}

	@Test
	void itsPeakFitsAnOutletAndTripsAtOneAndAQuarter() {
		double peak = PitPower.amps(Dragline.PEAK_WATTS, KV416, PitPower.POWER_FACTOR);
		assertEquals(877, peak, 1, "6 MW at 4.16 kV and 0.95");
		assertEquals(1.25 * peak, PitPower.tripAmps(Dragline.PEAK_WATTS, KV416), 1e-6, "under the outlet's 1,200 A");
	}

	@Test
	void itStandsBackAndReachesItsStrip() {
		assertEquals(12.5, Dragline.standoff(), 1e-9, "its tub's radius plus 5 m");
		assertEquals(42.5, Dragline.farEdge(30), 1e-9);
		assertEquals(49.2, Dragline.reachAlongFarEdge(30), 0.1);
		assertTrue(Dragline.fits(Dragline.WIDTH_MAX), "the widest strip still leaves room to cast");
		assertFalse(Dragline.fits(50));
		assertTrue(Dragline.inStrip(12.5, 0, 30, 100));
		assertFalse(Dragline.inStrip(12.4, 10, 30, 100), "the highwall it stands on");
		assertTrue(Dragline.inStrip(13, -1.6e-15, 30, 100), "its first row, facing north (sin 180 degrees is not quite 0)");
		assertFalse(Dragline.inStrip(42.5, 10, 30, 100), "the spoil side");
		assertFalse(Dragline.inStrip(20, 100, 30, 100), "past the strip's end");
		assertTrue(Dragline.inReach(39, 52));
		assertFalse(Dragline.inReach(40, 52));
	}

	@Test
	void itWalksOnUntilTheStripsEndIsInReach() {
		assertTrue(Dragline.walkOn(0, 30, 100));
		assertTrue(Dragline.walkOn(50, 30, 100));
		assertFalse(Dragline.walkOn(51, 30, 100), "51 + 49.2 m reaches the end");
		assertFalse(Dragline.walkOn(0, 30, 40), "a short strip is all in reach");
	}

	@Test
	void aStepIsTwoMetresAMinute() {
		assertEquals(0.12, Dragline.walkKmh(), 1e-9, "real about 0.2 km/h");
		assertEquals(0, Dragline.stepMoved(0));
		assertEquals(0, Dragline.stepMoved(Dragline.STEP_TICKS / 4));
		assertEquals(0.5, Dragline.stepMoved(Dragline.STEP_TICKS / 2), 1e-9);
		assertEquals(1, Dragline.stepMoved(Dragline.STEP_TICKS));
	}

	@Test
	void theBucketFillsToTwentyFiveBankBlocksOrFiftyFiveTonnes() {
		assertTrue(Dragline.fits(0, 0, 1_600, 4));
		assertTrue(Dragline.fits(24 * 1_600, 96, 1_600, 4), "the 25th block of sand");
		assertFalse(Dragline.fits(25 * 1_600, 100, 1_600, 4), "a 26th block of sand: past 25 m³ of bank");
		assertFalse(Dragline.fits(54_500, 80, 675, 1), "a quarter of rock past 55 t");
	}

	@Test
	void itSwingsToItsCastSide() {
		assertEquals(90, Dragline.swingTo(30, 0, true), 1e-9);
		assertEquals(-90, Dragline.swingTo(30, 0, false), 1e-9);
		assertEquals(45, Dragline.swingTo(20, 20, true), 1e-9);
	}

	@Test
	void aJobHoldsToItsLimits() {
		Dragline.Job job = new Dragline.Job();
		assertEquals(Dragline.WIDTH, job.get(0));
		job.set(0, 100);
		job.set(2, 0);
		job.set(3, 0);
		assertEquals(Dragline.WIDTH_MAX, job.width());
		assertEquals(Dragline.SEAM_MIN, job.seam());
		assertFalse(job.castRight());
	}

	@Test
	void itsPartsAreTheSpecsSizes() {
		assertEquals(5 * 15, Dragline.BOOM_M, 1e-9, "five 15 m boom sections make the 75 m boom");
		assertEquals(450 + 5_760 + 2 * 126 + 480 + 5 * 240 + 120, DraglineLayout.cells());
	}
}
