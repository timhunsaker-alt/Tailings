package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.tailings.haul.Sorter;
import dev.wildcatter.tailings.rock.Rock;
import org.junit.jupiter.api.Test;

class PlcScaleTest {
	@Test
	void rockCodesAreQuarterMilliampSteps() {
		assertEquals(0, PlcScale.code(4));
		assertEquals(0, PlcScale.code(3.7));
		assertEquals(10, PlcScale.code(6.5));
		assertEquals(10, PlcScale.code(6.6));   // a tenth of a milliamp off still reads its code
		assertEquals(64, PlcScale.code(20));
		assertNull(PlcScale.rock(0));
		assertNull(PlcScale.rock(Rock.values().length + 1));
		// the codes are written down in the docs: these must not move
		assertEquals(Rock.COBBLESTONE, PlcScale.rock(1));
		assertEquals(Rock.IRON, PlcScale.rock(9));
		assertEquals(Rock.COPPER, PlcScale.rock(10));
		assertEquals(Rock.GOLD, PlcScale.rock(11));
		assertEquals(Rock.GALENA, PlcScale.rock(20));
		assertEquals(Rock.SPHALERITE, PlcScale.rock(21));
		for (Rock rock : Rock.values()) {
			assertEquals(rock, PlcScale.rock(PlcScale.code(PlcScale.codeMa(PlcScale.codeOf(rock)))));
		}
		assertTrue(Rock.values().length <= 64, "the codes fit a loop");
	}

	@Test
	void courseSpansTheWorldAndTheCable() {
		assertEquals(-64, PlcScale.targetY(0));
		assertEquals(320, PlcScale.targetY(1));
		assertEquals(0, PlcScale.targetY(PlcScale.targetFraction(0)));
		assertEquals(40, PlcScale.targetY(PlcScale.targetFraction(40)));
		assertEquals(0, PlcScale.flatLength(0));
		assertEquals(200, PlcScale.flatLength(0.5));
		assertEquals(400, PlcScale.flatLength(1.2));
	}

	@Test
	void targetNeverMovesFloorAlreadyCut() {
		// not started: anything the world allows
		assertEquals(10, PlcScale.allowedTarget(10, 0, true, 60, 30, -63));
		assertEquals(-63, PlcScale.allowedTarget(-64, 0, true, 60, 30, -63));
		// still declining at floor 50: deeper is fine, higher levels out where it is
		assertEquals(20, PlcScale.allowedTarget(20, 20, true, 50, 30, -63));
		assertEquals(50, PlcScale.allowedTarget(70, 20, true, 50, 30, -63));
		// levelled out: the target holds
		assertEquals(30, PlcScale.allowedTarget(10, 80, false, 30, 30, -63));
	}

	@Test
	void slurryDensityFromItsSolids() {
		assertEquals(1.0, PlcScale.density(0), 1e-9);
		assertEquals(1.28, PlcScale.density(Product.FINES_SLURRY), 0.01);
		assertEquals(1.79, PlcScale.density(Product.MILL_DISCHARGE), 0.01);
		assertEquals(1.61, PlcScale.density(Product.THICKENED_CONCENTRATE), 0.01);
		assertEquals(1.0, PlcScale.density(Product.PREGNANT_LIQUOR), 1e-9);
		assertTrue(PlcScale.density(Product.MILL_DISCHARGE) < PlcScale.DENSITY_HIGH);
		assertEquals(4, PlcScale.ma(1.0, PlcScale.DENSITY_LOW, PlcScale.DENSITY_HIGH), 1e-9);
		assertEquals(20.5, PlcScale.ma(3, 0, 1), 1e-9);
		assertEquals(3.8, PlcScale.ma(-1, 0, 1), 1e-9);
	}

	@Test
	void aChosenSideOverridesItsScreenWhileChosen() {
		Sorter sorter = new Sorter();
		sorter.set(0, "minecraft:copper_ore");
		sorter.setWaste(3);
		assertEquals(0, sorter.route("minecraft:copper_ore"));
		sorter.choose(1, "minecraft:copper_ore");
		sorter.choose(0, null);
		assertEquals(1, sorter.route("minecraft:copper_ore"));
		assertEquals("minecraft:copper_ore", sorter.product(0), "the screen's setting is kept");
		sorter.setGate(1, true);
		assertEquals(3, sorter.route("minecraft:copper_ore"));
		sorter.setGate(1, false);
		sorter.release(0);
		sorter.release(1);
		assertEquals(0, sorter.route("minecraft:copper_ore"));
	}
}
