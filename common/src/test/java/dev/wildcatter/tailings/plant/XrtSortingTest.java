package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import org.junit.jupiter.api.Test;

/** The X-ray sorter's sorting maths (docs/multiblocks/xrt-ore-sorter.md, L8). */
class XrtSortingTest {
	private static final double EPS = 1e-9;

	@Test
	void effectiveAtomicNumbersAreTheTextbookOnes() {
		// quartz about 11.6, galena about 77, native gold 79 (the photoelectric power-law mean, exponent 2.94)
		assertEquals(11.6, XrtSorting.zeff(XrtSorting.GANGUE), 0.1);
		assertEquals(77.2, XrtSorting.zeff(Mineral.GALENA), 0.3);
		assertEquals(79, XrtSorting.zeff(Mineral.GOLD), EPS);
		assertEquals(6, XrtSorting.zeff(Mineral.GRAPHITE), EPS);
		for (Mineral mineral : Mineral.values()) {
			assertTrue(XrtSorting.zeff(mineral) > 5 && XrtSorting.zeff(mineral) < 93, mineral + " has a formula of real elements");
		}
	}

	@Test
	void countryRockReadsUnderTheDefaultAndHeavyOresOver() {
		int step = XrtSorting.DEFAULT_STEP;
		for (Rock rock : Rock.values()) {
			if (rock.kind() == Rock.Kind.COUNTRY) {
				assertFalse(XrtSorting.over(rock, step), rock + " reads " + XrtSorting.pieceZeff(rock));
				assertTrue(XrtSorting.pieceZeff(rock) > 11.5, rock + " reads over LOW: " + XrtSorting.pieceZeff(rock));
			}
		}
		for (Rock rock : new Rock[] {Rock.GALENA, Rock.SPHALERITE, Rock.CASSITERITE, Rock.SCHEELITE, Rock.CHROMITE, Rock.PITCHBLENDE, Rock.IRON, Rock.COPPER, Rock.GOLD}) {
			assertTrue(XrtSorting.over(rock, step), rock + " reads " + XrtSorting.pieceZeff(rock));
		}
		// what it cannot see: a few grams of platinum a tonne, and the light ores under quartz
		assertFalse(XrtSorting.over(Rock.PLATINUM, step));
		assertFalse(XrtSorting.over(Rock.GRAPHITE, step));
		assertFalse(XrtSorting.over(Rock.MAGNESITE, step));
	}

	@Test
	void theSplitHasTheSpecsErrors() {
		int step = XrtSorting.DEFAULT_STEP;
		assertEquals(0.97, XrtSorting.productShare(Rock.GALENA, step), EPS);
		assertEquals(0.05, XrtSorting.productShare(Rock.ANDESITE, step), EPS);
	}

	@Test
	void lowTakesNearlyEverythingAndHighDropsLowGradeOre() {
		assertEquals(0.97, XrtSorting.productShare(Rock.ANDESITE, 0), EPS);
		assertEquals(0.97, XrtSorting.productShare(Rock.DEEPSLATE, 0), EPS);
		int high = XrtSorting.THRESHOLDS.length - 1;
		assertEquals(0.05, XrtSorting.productShare(Rock.MOLYBDENITE, high), EPS);
		assertEquals(0.05, XrtSorting.productShare(Rock.COPPER, high), EPS);
		assertEquals(0.97, XrtSorting.productShare(Rock.GALENA, high), EPS);
		assertEquals(0.97, XrtSorting.productShare(Rock.SPHALERITE, high), EPS);
	}

	@Test
	void theRatingAndSizes() {
		assertEquals(250_000.0 / 72_000, XrtSorting.kgPerTick(0), EPS);
		assertEquals(125_000.0 / 72_000, XrtSorting.kgPerTick(1), EPS);
		assertTrue(XrtSorting.sorts(SizeClass.CRUSHED) && XrtSorting.sorts(SizeClass.GRAVEL));
		assertFalse(XrtSorting.sorts(SizeClass.SAND) || XrtSorting.sorts(SizeClass.FINES) || XrtSorting.sorts(null));
	}

	@Test
	void theThresholdLoopIsFourMilliampsAStep() {
		for (int step = 0; step < XrtSorting.THRESHOLDS.length; step++) {
			assertEquals(step, XrtSorting.stepOf((XrtSorting.maOf(step) - 4) / 16));
		}
		assertEquals(4, XrtSorting.maOf(0), EPS);
		assertEquals(20, XrtSorting.maOf(4), EPS);
		assertEquals(2, XrtSorting.stepOf(0.55));
	}

	@Test
	void theStateCountsTonnesAndKeepsItsStep() {
		XrtSorting.State state = new XrtSorting.State();
		assertEquals(XrtSorting.DEFAULT_STEP, state.step());
		state.nextStep();
		state.sent(2500, 999);
		assertEquals(2, state.productPulses());
		assertEquals(0, state.wastePulses());
		XrtSorting.State back = new XrtSorting.State();
		back.load(state.save());
		assertEquals(state.step(), back.step());
		assertEquals(2500, back.productKg(), EPS);
		for (int i = 0; i < 10; i++) {
			back.nextStep();
		}
		assertEquals(state.step(), back.step(), "five steps, then the five in reverse, round");
		assertEquals(state.reverse(), back.reverse());
		XrtSorting.State blank = new XrtSorting.State();
		blank.load("");
		assertEquals(XrtSorting.DEFAULT_STEP, blank.step());
	}

	@Test
	void reverseKeepsWhatReadsUnderTheThreshold() {
		// Tim, 2026-11-10 (l8-xrt.md #5): graphite reads under quartz, so only reverse can upgrade it
		int step = XrtSorting.DEFAULT_STEP;
		assertEquals(0.05, XrtSorting.productShare(Rock.GRAPHITE, step), EPS);
		assertEquals(0.97, XrtSorting.productShare(Rock.GRAPHITE, step, true), EPS);
		assertEquals(0.97, XrtSorting.productShare(Rock.GALENA, step), EPS);
		assertEquals(0.05, XrtSorting.productShare(Rock.GALENA, step, true), EPS);
		XrtSorting.State state = new XrtSorting.State();
		assertFalse(state.reverse(), "a new sorter sorts as built");
		for (int i = state.step(); i < XrtSorting.THRESHOLDS.length; i++) {
			state.nextStep();
		}
		assertTrue(state.reverse() && state.step() == 0, "past HIGH the screen goes to LOW in reverse");
		XrtSorting.State back = new XrtSorting.State();
		back.load(state.save());
		assertTrue(back.reverse(), "reverse is saved");
		back.load("1|0|0");
		assertFalse(back.reverse(), "a sorter saved before it loads as built");
		assertTrue(XrtSorting.describe(0, true).contains("REVERSE"));
	}
}
