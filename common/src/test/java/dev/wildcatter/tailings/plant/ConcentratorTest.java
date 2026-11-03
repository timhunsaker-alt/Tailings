package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.tailings.recovery.Recovery;
import dev.wildcatter.tailings.recovery.RecoveryRoute;
import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Milling;
import dev.wildcatter.tailings.rock.NuggetBank;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class ConcentratorTest {
	@Test
	void circuitsAreTrueSizeAndDoNotOverlap() {
		assertFalse(PlantPlan.overlaps(PlantPlan.CRUSHING));
		assertFalse(PlantPlan.overlaps(PlantPlan.FLOTATION));
		assertEquals(4, PlantPlan.count(PlantPlan.FLOTATION, 'R'));
		assertEquals(2, PlantPlan.count(PlantPlan.FLOTATION, 'V'));
		assertEquals(2, PlantPlan.count(PlantPlan.FLOTATION, 'L'));
		assertEquals(110_000, Machine.JAW.watts());
		assertEquals(600_000, Machine.BALL_MILL.watts());
		assertEquals(Machine.Supply.MEDIUM, Machine.BALL_MILL.supply());
		assertEquals(0, Machine.GRIZZLY.watts());
		assertEquals(96, Machine.BALL_MILL.cells());
		assertEquals(192, Machine.THICKENER.cells());
		assertTrue(PlantPlan.row(PlantPlan.CRUSHING).indexOf('J') > 0);
		assertTrue(PlantPlan.row(PlantPlan.FLOTATION).startsWith("K"));
	}

	@Test
	void crushingLiberatesAndPaysNothing() {
		assertEquals(SizeClass.CRUSHED, Concentrator.crush(Machine.JAW, null));
		assertEquals(SizeClass.GRAVEL, Concentrator.crush(Machine.CONE, SizeClass.CRUSHED));
		assertEquals(SizeClass.SAND, Concentrator.crush(Machine.SCREEN, SizeClass.GRAVEL));
		assertEquals(SizeClass.FINES, Concentrator.crush(Machine.BALL_MILL, SizeClass.SAND));
		assertNull(Concentrator.crush(Machine.BALL_MILL, SizeClass.GRAVEL));
		assertNull(Concentrator.crush(Machine.JAW, SizeClass.CRUSHED));
		assertEquals(0.90, PlantPlan.FINES_LIBERATION);
		assertEquals(0.60, PlantPlan.SAND_LIBERATION);
	}

	@Test
	void flotationPaysThreeAndStaysInsideTheBlock() {
		List<Milling.Yield> copper = Concentrator.floatBank(Rock.COPPER, new NuggetBank());
		List<Milling.Yield> gold = Concentrator.floatBank(Rock.GOLD, new NuggetBank());
		List<Milling.Yield> galena = Concentrator.floatBank(Rock.GALENA, new NuggetBank());
		assertEquals(List.of(new Milling.Yield("minecraft:copper_ingot", 3)), copper);
		assertEquals(List.of(new Milling.Yield("minecraft:gold_ingot", 3)), gold);
		assertEquals(List.of(new Milling.Yield("wildcatter:lead_ingot", 3)), galena);
		assertTrue(Concentrator.floatBank(Rock.IRON, new NuggetBank()).isEmpty());
		assertTrue(Concentrator.floatBank(Rock.CASSITERITE, new NuggetBank()).isEmpty());
		for (Rock rock : List.of(Rock.COPPER, Rock.GOLD, Rock.GALENA, Rock.SPHALERITE, Rock.PENTLANDITE, Rock.MOLYBDENITE, Rock.COBALTITE, Rock.ACANTHITE, Rock.PLATINUM)) {
			for (Mineral mineral : rock.minerals()) {
				if (!mineral.floats()) {
					continue;
				}
				BigDecimal contained = rock.contained(mineral);
				BigDecimal recovered = Recovery.recoveredKilograms(contained, RecoveryRoute.FLOTATION);
				int count = RecoveryRoute.FLOTATION.multipleOfPlainMining();
				assertEquals(3, count);
				assertTrue(recovered.compareTo(contained) <= 0);
				if (rock.kind() != Rock.Kind.COUNTRY) {
					assertTrue(mineral.itemAmount().multiply(BigDecimal.valueOf(count)).compareTo(contained) <= 0);
				}
			}
		}
	}

	@Test
	void countrySulfidesBecomeNuggetsInsideTheWrittenMass() {
		NuggetBank bank = new NuggetBank();
		BigDecimal recovered = Recovery.recoveredKilograms(Rock.COBBLESTONE.contained(Mineral.GALENA), RecoveryRoute.FLOTATION);
		int nuggets = 0;
		for (int i = 0; i < 100; i++) {
			for (Milling.Yield yield : Concentrator.floatBank(Rock.COBBLESTONE, bank)) {
				if (yield.itemId().equals(Mineral.GALENA.nuggetId())) {
					nuggets += yield.count();
				}
			}
		}
		BigDecimal paid = Mineral.GALENA.nuggetAmount().multiply(BigDecimal.valueOf(nuggets)).add(bank.held(Mineral.GALENA));
		assertEquals(0, paid.compareTo(recovered.multiply(BigDecimal.valueOf(100))));
		assertTrue(nuggets > 0);
	}

	@Test
	void reagentsMatchTheWrittenDosesAndSphaleriteNeedsCopperSulfate() {
		assertEquals(0, new BigDecimal("1.35").compareTo(Reagent.LIME.kilogramsPerBlock()));
		assertEquals(0, new BigDecimal("0.135").compareTo(Reagent.XANTHATE.kilogramsPerBlock()));
		assertFalse(Reagent.COPPER_SULFATE.requiredFor(Rock.COPPER));
		assertTrue(Reagent.COPPER_SULFATE.requiredFor(Rock.SPHALERITE));
		assertTrue(Reagent.LIME.requiredFor(Rock.COPPER));
		ReagentStock stock = new ReagentStock();
		assertFalse(stock.covers(Rock.COPPER));
		stock.addKilogram(Reagent.LIME);
		stock.addKilogram(Reagent.LIME);
		stock.addKilogram(Reagent.XANTHATE);
		stock.addKilogram(Reagent.MIBC);
		assertTrue(stock.covers(Rock.COPPER));
		assertFalse(stock.covers(Rock.SPHALERITE));
		stock.consume(Rock.COPPER);
		assertTrue(stock.kilograms(Reagent.LIME).compareTo(new BigDecimal("1.35")) < 0);
	}
}
