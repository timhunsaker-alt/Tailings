package dev.wildcatter.tailings.block.stage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.plant.Assays;
import dev.wildcatter.tailings.plant.FilterCake;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.plant.ReagentStock;
import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The flotation circuit's arithmetic (docs/multiblocks/flotation-bank.md): the bank's split, the filter's cake and the reagents. */
class FlotationTest {
	private static final String COPPER = Mineral.COPPER.mineralItemId();
	private static final String IRON = Mineral.IRON.mineralItemId();

	@Test
	void theBankFloatsThreeQuartersOfWhatFloatsAndKeepsTheMass() {
		SlurryAssay feed = SlurryAssay.of(Map.of(COPPER, 0.001, IRON, 0.002));
		double kg = 10_000;
		dev.wildcatter.tailings.plant.Selective.Split split = Flotation.split(feed, kg);
		assertEquals(kg, split.concentrate() + split.tails(), 1e-6, "no slurry is made or lost");
		double copperIn = 0.001 * kg;
		double copperConc = split.concentrateAssay().get(COPPER) * split.concentrate();
		double copperTail = split.tailsAssay().get(COPPER) * split.tails();
		assertEquals(0.75 * copperIn, copperConc, 1e-9, "the 75 % ladder");
		assertEquals(copperIn, copperConc + copperTail, 1e-9, "never more than the feed");
		assertEquals(0, split.concentrateAssay().get(IRON), "magnetite does not float");
		assertEquals(0.002 * kg, split.tailsAssay().get(IRON) * split.tails(), 1e-9);
		assertEquals(copperConc / Flotation.CONCENTRATE_SOLIDS, split.concentrate(), 1e-9, "a 30 % solids concentrate");
	}

	@Test
	void aBlockOfCopperOrePaysThreeIngotsWorthOfNuggets() {
		FilterCake cake = new FilterCake();
		double copper = Rock.COPPER.contained(Mineral.COPPER).doubleValue() * 0.75;
		// it arrives in many small parts, as it does off the pipe
		for (int i = 0; i < 997; i++) {
			cake.add(Mineral.COPPER, copper / 997);
		}
		int nuggets = 0;
		for (int i = 0; i < 5; i++) {
			nuggets += cake.takeNuggets(Mineral.COPPER);
		}
		assertEquals(27, nuggets, "27 nuggets pack into 3 ingots, the 3x step");
		assertEquals(0, cake.held(Mineral.COPPER), 1e-6);
	}

	@Test
	void countryRockPaysNuggetsAndKeepsWhatIsLeft() {
		FilterCake cake = new FilterCake();
		double galena = Rock.COBBLESTONE.contained(Mineral.GALENA).doubleValue() * 0.75;
		int nuggets = 0;
		for (int block = 0; block < 100; block++) {
			cake.add(Mineral.GALENA, galena);
			nuggets += cake.takeNuggets(Mineral.GALENA);
		}
		double paid = nuggets * Mineral.GALENA.nuggetAmount().doubleValue() + cake.held(Mineral.GALENA);
		assertEquals(100 * galena, paid, 1e-6);
		assertTrue(nuggets > 0);
		FilterCake back = new FilterCake();
		back.load(cake.save());
		assertEquals(cake.held(Mineral.GALENA), back.held(Mineral.GALENA), 1e-12);
		cake.putBack(Mineral.GALENA, 2);
		assertEquals(back.held(Mineral.GALENA) + 2 * Mineral.GALENA.nuggetAmount().doubleValue(), cake.held(Mineral.GALENA), 1e-9);
	}

	@Test
	void reagentsAreDosedByTheTonneAndSphaleriteNeedsCopperSulfate() {
		assertEquals(0, new BigDecimal("1.35").compareTo(Reagent.LIME.kilogramsPerBlock()));
		assertEquals(0, new BigDecimal("0.5").compareTo(Reagent.LIME.kilogramsFor(1)));
		ReagentStock stock = new ReagentStock();
		assertFalse(stock.covers(1, false));
		for (Reagent r : new Reagent[] {Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC}) {
			stock.addKilogram(r);
		}
		assertTrue(stock.covers(1, false));
		assertFalse(stock.covers(1, true));
		assertEquals("Copper Sulfate", stock.missing(1, true));
		stock.consume(1, false);
		assertEquals(0, new BigDecimal("0.5").compareTo(stock.kilograms(Reagent.LIME)));
		assertTrue(Assays.dry(Rock.SPHALERITE).get(Mineral.SPHALERITE.mineralItemId()) > 0, "the tank finds sphalerite in the assay");
		assertEquals(PlantRates.KG_PER_TICK / PlantRates.OVERFLOW_SOLIDS, Flotation.FEED_PER_TICK, 1e-12);
	}
}
