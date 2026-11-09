package dev.wildcatter.tailings.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The bench, the hole and the blast's numbers (docs/multiblocks/open-pit-p3-blasting.md). */
class BlastingTest {
	@Test
	void theDefaultHoleIsTheSpecs() {
		assertEquals(69, Blasting.KG_PER_M, "a 270 mm hole at 1.2 g/cm³ holds 69 kg a metre");
		int depth = Blasting.depth(Blasting.BENCH, Blasting.SUBDRILL);
		assertEquals(16, depth);
		int column = Blasting.column(depth, Blasting.STEMMING);
		assertEquals(12, column);
		assertEquals(828, Blasting.chargeKg(column));
		double tonnes = Blasting.boxTonnes(Blasting.BURDEN, Blasting.SPACING, Blasting.BENCH);
		assertEquals(2_116.8, tonnes, 0.01, "7 x 8 x 14 m at 2.7 t/m³");
		assertEquals(0.39, Blasting.powderFactor(828, tonnes), 0.005);
	}

	@Test
	void depthIsOnePass() {
		assertEquals(16, Blasting.depth(15, 3), "18 m is over the 16.8 m pass");
		assertEquals(8, Blasting.depth(6, 2));
	}

	@Test
	void cuttingsAndGravel() {
		assertEquals(0.92, Blasting.holeVolume(16), 0.005, "a 16 m hole is 0.92 m³");
		assertEquals(13.1, Blasting.gravelStemming(), 0.1, "a block of gravel stems 13 m of hole");
		assertTrue(Blasting.cuttingsStemming(16) >= Blasting.STEMMING_MAX, "a hole's own cuttings stem it to any setting");
	}

	@Test
	void harmFollowsTheSpecsLine() {
		assertEquals(30, Blasting.harm(0), 1e-9);
		assertEquals(18, Blasting.harm(20), 1e-9);
		assertEquals(6, Blasting.harm(40), 1e-9);
		assertEquals(0, Blasting.harm(50), 1e-9);
		assertEquals(0, Blasting.harm(80), 1e-9);
	}

	@Test
	void delaysAndSleep() {
		assertEquals(0, Blasting.delayMs(0, 0, 65, 25));
		assertEquals(65 * 2 + 25 * 3, Blasting.delayMs(2, 3, 65, 25));
		assertEquals(15_000, Blasting.delayMs(300, 0, 65, 25), "held to the detonator's range");
		assertEquals(414, Blasting.effectiveKg(828, 0, 100), 1e-9, "fired before it gasses it counts half");
		assertEquals(828, Blasting.effectiveKg(828, 0, Blasting.SLEEP_TICKS), 1e-9);
	}

	@Test
	void outcomes() {
		double t = Blasting.boxTonnes(7, 8, 14);
		assertEquals(Blasting.Outcome.GOOD, Blasting.outcome(true, 828, t, 4));
		assertEquals(Blasting.Outcome.BLOWOUT, Blasting.outcome(true, 828, t, 1.5));
		assertEquals(Blasting.Outcome.UNDERCHARGED, Blasting.outcome(true, 300, t, 4), "300 kg is 0.14 kg/t");
		assertEquals(Blasting.Outcome.NO_PRIMER, Blasting.outcome(false, 828, t, 4));
		assertEquals(Blasting.Outcome.OVERCHARGED, Blasting.outcome(true, 2_200, t, 4));
	}

	@Test
	void swellIsFourForThree() {
		assertEquals(0, Blasting.swell(2));
		assertEquals(1, Blasting.swell(3));
		assertEquals(261, Blasting.swell(784));
	}

	@Test
	void flyrockFlies() {
		int t = Blasting.flightTicks(100);
		assertTrue(t > 80 && t < 100, "100 m on a 45 degree arc is about 4.5 s, got " + t);
	}

	@Test
	void aStaggeredPatternIsASerpentine() {
		// rows run north (back = 0, -1); along a row east (1, 0)
		List<DrillPattern.Hole> holes = DrillPattern.holes(0, 0, 0, -1, 1, 0, 3, 3, 7, 8, true);
		assertEquals(9, holes.size());
		assertEquals(new DrillPattern.Hole(0, 0, 0, 0), holes.get(0));
		assertEquals(new DrillPattern.Hole(0, 2, 16, 0), holes.get(2));
		// row 2 is walked back, offset half a spacing
		assertEquals(new DrillPattern.Hole(1, 2, 20, -7), holes.get(3));
		assertEquals(new DrillPattern.Hole(1, 0, 4, -7), holes.get(5));
		assertEquals(new DrillPattern.Hole(2, 0, 0, -14), holes.get(6));
		Set<Long> spots = new HashSet<>();
		holes.forEach(h -> spots.add(((long) h.x() << 32) ^ (h.z() & 0xffffffffL)));
		assertEquals(9, spots.size(), "no two holes in one place");
		for (int i = 1; i < holes.size(); i++) {
			DrillPattern.Hole a = holes.get(i - 1), b = holes.get(i);
			assertTrue(Math.hypot(a.x() - b.x(), a.z() - b.z()) <= 8.1, "each hole is near the last: " + a + " to " + b);
		}
	}

	@Test
	void aDefaultHoleTakesAboutThreeMinutesTwenty() {
		int ticks = DrillPattern.holeTicks(16, 8);
		assertEquals(200, ticks / 20.0, 1.5, "16 x 10 s + 20 s + 8 m at 1.5 km/h");
	}
}
