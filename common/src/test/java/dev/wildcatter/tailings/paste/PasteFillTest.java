package dev.wildcatter.tailings.paste;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The paste fill's figures (paste-backfill.md, "Size", "Parts", "How it runs"). */
class PasteFillTest {
	@Test
	void aCubicMetreOfPasteHoldsAboutOneAndAHalfTonnesOfTailsAndABagAndAHalfOfCement() {
		// 2.0 t at 78 % solids is 1.56 t of solids, cement included: 1.49 t of tails and 74 kg of cement
		assertEquals(1_560, PasteFill.tailsSolidsPerM3() + PasteFill.cementPerM3(), 0.5);
		assertEquals(1_486, PasteFill.tailsSolidsPerM3(), 1);
		assertEquals(74.3, PasteFill.cementPerM3(), 0.1);
		// at 5 %, a 50 kg bag a tonne of tails solids
		assertEquals(1.0, PasteFill.bagsFor(1_000), 1e-9);
	}

	@Test
	void allThePlantsTailsMakeAbout32CubicMetresAnHourAndThePumpTakes35() {
		double m3 = dev.wildcatter.tailings.plant.PlantRates.TONNES_PER_HOUR * 1000 / PasteFill.tailsSolidsPerM3();
		assertEquals(33.6, m3, 0.1);
		assertTrue(PasteFill.PUMP_M3_PER_HOUR >= m3 * 0.95, "the pump takes about the plant's tails");
		// 2.5 t of cement an hour at the full rate
		assertEquals(2_500, dev.wildcatter.tailings.plant.PlantRates.TONNES_PER_HOUR * 1000 * PasteFill.BINDER, 1e-6);
	}

	@Test
	void theMixerTrimsTheCakeTo78Percent() {
		double trim = PasteFill.trimPerKgCake();
		assertEquals(0.208, trim, 0.001);
		assertEquals(PasteFill.PASTE_SOLIDS, PasteFill.mixedSolids(1, trim), 1e-9);
		// without trim the cake alone, with its binder, is stiffer than the paste
		assertTrue(PasteFill.mixedSolids(1, 0) > PasteFill.PASTE_SOLIDS);
		// the cake for a m3 is what is left of its tails after the trim's solids
		double cake = PasteFill.cakeFor(PasteFill.PASTE_KG_PER_M3);
		assertEquals(PasteFill.tailsSolidsPerM3(), cake * (1 + trim * PasteFill.UNDERFLOW_SOLIDS), 1e-6);
		double paste = cake / PasteFill.CAKE_SOLIDS + cake * trim + PasteFill.tailsSolidsPerM3() * PasteFill.BINDER;
		assertEquals(PasteFill.PASTE_KG_PER_M3, paste, 1e-6);
	}

	@Test
	void theFilterTakesAbout0Point42LitresOfFiltrateAKilogram() {
		assertEquals(0.4167, PasteFill.filtratePerKgSolids(), 1e-4);
	}

	@Test
	void thePumpNeedsAbout111KilowattsAndHas110() {
		assertEquals(111_111, PasteFill.pumpShaftWatts(), 1);
		assertEquals(110_000, PasteFill.PUMP_WATTS);
		assertEquals(0.972, PasteFill.pumpKgPerTick(), 0.001);
	}

	@Test
	void theLineReadsLinePressurePastAKilometreOnTheFlat() {
		assertEquals(8_000_000, PasteFill.linePressure(1000, 0), 1e-6);
		assertTrue(PasteFill.pumpable(1000, 0));
		assertFalse(PasteFill.pumpable(1001, 0));
		// a fall helps: 100 m down pays for 245 m of pipe
		assertEquals(8_000_000 - 1_962_000, PasteFill.linePressure(1000, 100), 1e-6);
		assertTrue(PasteFill.pumpable(1200, 100));
		// a line that falls far enough runs on the paste's weight
		assertEquals(0, PasteFill.linePressure(300, 200), 1e-9);
		assertTrue(PasteFill.MOST_LINE_PIPES > 1000);
	}

	@Test
	void pasteSetsInOneGameDay() {
		assertEquals(24_000, PasteFill.cureStepTicks() * PasteFill.CURE_STEPS);
		assertEquals(3_000, PasteFill.cureStepTicks());
	}

	@Test
	void aDriveBranchFillsInAboutAnHour() {
		// Tim, 2026-11-10 (l5-paste.md #19): ten times the real pace; the cure stays a game day
		assertEquals(200, PasteFill.kgPerBlock(), 1e-9);
		assertEquals(1.03, PasteFill.hoursToFill(360), 0.01);
		assertEquals(24_000, PasteFill.CURE_TICKS);
	}
}
