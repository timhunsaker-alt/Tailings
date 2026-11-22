package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.tailings.rock.Mineral;
import org.junit.jupiter.api.Test;

/** The copper smelter line's numbers (docs/copper-smelter.md). */
class SmelterTest {
	@Test
	void theTankhouseMakesTheLinesRating() {
		// 72 cells at 20.5 kA, 96 % current efficiency, 1.186 g an ampere-hour: 1.68 t/h
		assertEquals(1.186, Smelter.G_PER_AH, 0.001);
		assertEquals(Smelter.CU_KGH, Smelter.tankhouseKgh(), 2);
		assertEquals(21.6, Smelter.tankhouseVolts(), 1e-9);
		assertEquals(443_000, Smelter.tankhouseDcWatts(), 1000);
	}

	@Test
	void theSulfurSplitsBetweenFurnaceAndConverter() {
		assertEquals(0.32 / 0.28, Smelter.sulfurPerCu(), 1e-12);
		assertEquals(0.4, Smelter.converterSulfurPerCu(), 1e-12);
		assertEquals(Smelter.sulfurPerCu(), Smelter.furnaceSulfurPerCu() + Smelter.converterSulfurPerCu(), 1e-12);
		// at the rating: about 0.53 kg/s of sulfur, under the 0.57 kg/s of Wildcatter's 150 t/d acid plant
		double kgs = Smelter.CU_KGH * Smelter.sulfurPerCu() / 3600;
		assertEquals(0.533, kgs, 0.001);
		assertTrue(kgs < dev.wildcatter.fluid.AcidPlant.ratedSulfurKgs());
	}

	@Test
	void anodesAndCathodesAreWholeIngots() {
		assertEquals(400, Smelter.anodeKg(), 1e-9);
		assertEquals(14 * Mineral.COPPER.kgPerUnit(), Smelter.anodeKg(), 1e-9);
		assertEquals(2 * Mineral.COPPER.kgPerUnit(), Smelter.cathodeKg(), 1e-9);
		assertEquals(7, Smelter.anodeKg() / Smelter.cathodeKg(), 1e-9);
	}

	@Test
	void theLinePaysMoreThanTheShortcut() {
		// per ore block through flotation, 300 kg of copper: 10.24 ingots as cathode against the shortcut's 8.9
		double block = 0.75 * 400;
		double line = block * Smelter.recovery() / Mineral.COPPER.kgPerUnit();
		double shortcut = block * Smelter.SHORTCUT_PAYABLE / Mineral.COPPER.kgPerUnit();
		assertEquals(10.24, line, 0.01);
		assertEquals(8.93, shortcut, 0.01);
		assertTrue(line > shortcut * 1.1);
	}

	@Test
	void theStateSavesAndLoads() {
		SmelterState s = new SmelterState();
		s.add(123.5);
		s.plate(7);
		s.count(10, 1, 20, 3);
		s.made(0);
		SmelterState t = new SmelterState();
		t.load(s.save());
		assertEquals(123.5, t.copper(), 1e-6);
		assertEquals(7, t.plated(), 1e-6);
		assertEquals(1, t.lostKg(), 1e-6);
		assertEquals(1, t.madeCount());
	}
}
