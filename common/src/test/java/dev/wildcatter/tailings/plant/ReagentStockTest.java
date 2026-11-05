package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.tailings.rock.Rock;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** The tank holds one item per kilogram, or part of one, still to dose: spent reagent never piles up in its slots. */
class ReagentStockTest {
	@Test
	void aDoseSpendsWholeItemsAndKeepsThePart() {
		ReagentStock stock = new ReagentStock();
		stock.addKilogram(Reagent.LIME);
		stock.addKilogram(Reagent.LIME);
		assertEquals(2, stock.items(Reagent.LIME));
		// 1.35 kg of lime a block: 0.65 kg left, still one item
		stock.addKilogram(Reagent.XANTHATE);
		stock.addKilogram(Reagent.MIBC);
		stock.consume(Rock.COPPER);
		assertEquals(0, new BigDecimal("0.65").compareTo(stock.kilograms(Reagent.LIME)));
		assertEquals(1, stock.items(Reagent.LIME));
		assertEquals(1, stock.items(Reagent.XANTHATE));
	}

	@Test
	void threeHundredBlocksNeverJam() {
		ReagentStock stock = new ReagentStock();
		int[] held = new int[Reagent.values().length];
		for (int block = 0; block < 300; block++) {
			// top up as the plant would, one item at a time while the tank has room (4 slots of 64)
			for (Reagent reagent : Reagent.values()) {
				while (held[reagent.ordinal()] < 64 && !stock.covers(Rock.SPHALERITE)) {
					stock.addKilogram(reagent);
					held[reagent.ordinal()]++;
				}
			}
			assertTrue(stock.covers(Rock.SPHALERITE), "block " + block);
			stock.consume(Rock.SPHALERITE);
			for (Reagent reagent : Reagent.values()) {
				held[reagent.ordinal()] = stock.items(reagent);
			}
		}
	}

	@Test
	void saveAndLoadKeepTheStock() {
		ReagentStock stock = new ReagentStock();
		for (int i = 0; i < 3; i++) {
			stock.addKilogram(Reagent.LIME);
			stock.addKilogram(Reagent.XANTHATE);
			stock.addKilogram(Reagent.MIBC);
		}
		stock.consume(Rock.COPPER);
		ReagentStock back = new ReagentStock();
		back.load(stock.save());
		for (Reagent reagent : Reagent.values()) {
			assertEquals(0, stock.kilograms(reagent).compareTo(back.kilograms(reagent)), reagent.name());
			assertEquals(stock.items(reagent), back.items(reagent));
		}
	}
}
