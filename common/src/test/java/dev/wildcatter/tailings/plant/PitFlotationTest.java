package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The pit line's flotation split (pit-flotation.md, P7c; decisions in docs/questions/p7-pit-line.md). */
class PitFlotationTest {
	private static final String COPPER = Mineral.COPPER.mineralItemId();
	private static final String QUARTZ_LIKE = "minecraft:quartz";

	@Test
	void theRowPaysTheFlotationRung() {
		assertEquals(PitFlotation.rung(), PitFlotation.rowRecovery(), 1e-12);
		assertEquals(0.75, PitFlotation.floatedBy(8), 1e-12);
		assertEquals(0.63, PitFlotation.floatedBy(4), 1e-12);
		assertEquals(0.75, PitFlotation.floatedBy(9), 1e-12);
	}

	/** Copper through eight cells in turn: 35, 15, 8, 5, 4, 3, 3, 2 % of the block's copper, 75 % in all, the rest in the tails. */
	@Test
	void eightCellsSplitTheCopper() {
		double feedKg = 1000, copper = 10;
		SlurryAssay feed = SlurryAssay.of(Map.of(COPPER, copper / feedKg, QUARTZ_LIKE, 0.3));
		double kg = feedKg, floated = 0;
		for (int cell = 0; cell < 8; cell++) {
			Selective.Split split = PitFlotation.cell(feed, kg);
			double got = split.concentrateAssay().kg(COPPER, split.concentrate());
			assertEquals(copper * PitFlotation.SHARES_PERCENT[cell] / 100.0, got, 1e-9, "cell " + (cell + 1));
			assertEquals(PitFlotation.ROUGHER_SOLIDS, PitFlotation.concentrateSolids(split.concentrateAssay()), 1e-9);
			floated += got;
			feed = split.tailsAssay();
			kg = split.tails();
		}
		assertEquals(copper * 0.75, floated, 1e-9);
		// what is left travels marked as having passed the whole row; a ninth cell floats none of it
		assertEquals(copper * 0.25, feed.kg(PitFlotation.marked(8, COPPER), kg), 1e-9);
		Selective.Split ninth = PitFlotation.cell(feed, kg);
		assertEquals(0, ninth.concentrate(), 1e-12);
		assertEquals(copper * 0.25, PitFlotation.plain(ninth.tailsAssay()).kg(COPPER, ninth.tails()), 1e-9);
		// the gangue goes through untouched
		assertEquals(0.3 * feedKg, PitFlotation.plain(ninth.tailsAssay()).kg(QUARTZ_LIKE, ninth.tails()), 1e-6);
	}

	@Test
	void aShortRowPaysWhatItsCellsFloat() {
		SlurryAssay feed = SlurryAssay.of(Map.of(COPPER, 0.01));
		double kg = 1000, floated = 0;
		for (int cell = 0; cell < 4; cell++) {
			Selective.Split split = PitFlotation.cell(feed, kg);
			floated += split.concentrateAssay().kg(COPPER, split.concentrate());
			feed = split.tailsAssay();
			kg = split.tails();
		}
		assertEquals(10 * 0.63, floated, 1e-9);
	}

	@Test
	void marksReadBack() {
		assertEquals(0, PitFlotation.passed(COPPER));
		assertEquals(3, PitFlotation.passed(PitFlotation.marked(3, COPPER)));
		assertEquals(COPPER, PitFlotation.plain(PitFlotation.marked(3, COPPER)));
		assertEquals(COPPER, PitFlotation.marked(0, COPPER));
		// the spent and held-down names of the selective circuit are not the row's marks
		assertEquals(Selective.DEPRESSED, PitFlotation.plain(Selective.DEPRESSED));
	}

	/** Sphalerite floats as the bank's zinc concentrate, and is lost from a bulk feed that also holds galena, as at the bank. */
	@Test
	void zincAsTheBank() {
		String zinc = Mineral.SPHALERITE.mineralItemId(), lead = Mineral.GALENA.mineralItemId();
		Selective.Split alone = PitFlotation.cell(SlurryAssay.of(Map.of(zinc, 0.01)), 1000);
		assertEquals(3.5, alone.concentrateAssay().kg(Selective.ZINC_CONCENTRATE, alone.concentrate()), 1e-9);
		Selective.Split bulk = PitFlotation.cell(SlurryAssay.of(Map.of(zinc, 0.01, lead, 0.01)), 1000);
		assertEquals(0, bulk.concentrateAssay().kg(Selective.ZINC_CONCENTRATE, bulk.concentrate()), 1e-12);
		assertEquals(3.5, bulk.concentrateAssay().kg(lead, bulk.concentrate()), 1e-9);
		assertEquals(6.5, bulk.tailsAssay().kg(PitFlotation.marked(1, zinc), bulk.tails()), 1e-9);
	}

	/** The columns take the rougher concentrate from 20 % to the bank's 30 %: every kg of mineral on, the water given up as tails. */
	@Test
	void theColumnsClean() {
		Selective.Split rougher = PitFlotation.cell(SlurryAssay.of(Map.of(COPPER, 0.01)), 1000);
		Selective.Split clean = PitFlotation.column(rougher.concentrateAssay(), rougher.concentrate());
		assertEquals(3.5, clean.concentrateAssay().kg(COPPER, clean.concentrate()), 1e-9);
		assertEquals(0.30, PitFlotation.concentrateSolids(clean.concentrateAssay()), 1e-9);
		assertEquals(rougher.concentrate(), clean.concentrate() + clean.tails(), 1e-9);
		assertTrue(clean.tailsAssay().isEmpty());
		Selective.Split again = PitFlotation.column(clean.concentrateAssay(), clean.concentrate());
		assertEquals(0, again.tails(), 1e-12);
	}

	@Test
	void theRowsRates() {
		// 1,000 t/h of fines slurry at 35 % is about 39.7 kg a tick; the row is rated 20 % over it
		assertEquals(47.6, PitFlotation.ROW_KG_PER_TICK, 0.1);
		// eight 160 m3 cells at 85 % live hold about 29 minutes of the line's 2,230 m3/h
		assertEquals(29, 8 * 160 * 0.85 / 2230 * 60, 0.5);
	}
}
