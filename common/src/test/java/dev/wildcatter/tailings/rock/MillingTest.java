package dev.wildcatter.tailings.rock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.tailings.recovery.Recovery;
import dev.wildcatter.tailings.recovery.RecoveryRoute;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MillingTest {
	@Test
	void sizeLadderStopsBeforeFines() {
		assertEquals(SizeClass.CRUSHED, Milling.stamp(null));
		assertNull(Milling.stamp(SizeClass.CRUSHED));
		assertEquals(SizeClass.GRAVEL, Milling.screen(SizeClass.CRUSHED));
		assertEquals(SizeClass.SAND, Milling.screen(SizeClass.GRAVEL));
		assertNull(Milling.screen(SizeClass.SAND));
		assertNull(Milling.screen(SizeClass.FINES));
		assertEquals("tailings:crushed_granite", SizeClass.CRUSHED.itemId(Rock.GRANITE));
		assertEquals("tailings:fines_sandstone", SizeClass.FINES.itemId(Rock.SANDSTONE));
	}

	@Test
	void everyRockHasAMineralListAndTheTwentySevenOresAreHere() {
		assertEquals(27, Rock.wildcatterOres().size());
		for (Rock rock : Rock.values()) {
			assertFalse(rock.minerals().isEmpty(), rock.name());
			for (Mineral mineral : rock.minerals()) {
				assertTrue(rock.contained(mineral).signum() > 0);
			}
		}
		assertEquals(0, new BigDecimal("8").compareTo(Rock.GRANITE.contained(Mineral.ZIRCON)));
		assertEquals(0, new BigDecimal("344.5").compareTo(Mineral.CASSITERITE.blockAmount()));
		assertEquals(0, new BigDecimal("225").compareTo(Mineral.ZIRCON.blockAmount()));
	}

	@Test
	void crusherAndScreenPayNothing() {
		assertFalse(Milling.acceptsConcentrate(null, Separator.GRAVITY, Rock.CASSITERITE));
		assertFalse(Milling.acceptsConcentrate(SizeClass.CRUSHED, Separator.GRAVITY, Rock.CASSITERITE));
		assertFalse(Milling.acceptsConcentrate(SizeClass.GRAVEL, Separator.MAGNETIC, Rock.IRON));
		assertTrue(Milling.acceptsConcentrate(SizeClass.SAND, Separator.GRAVITY, Rock.CASSITERITE));
		assertFalse(Milling.acceptsConcentrate(SizeClass.SAND, Separator.MAGNETIC, Rock.CASSITERITE));
		assertTrue(Milling.acceptsConcentrate(SizeClass.SAND, Separator.MAGNETIC, Rock.IRON));
		assertFalse(Milling.acceptsConcentrate(SizeClass.SAND, Separator.GRAVITY, Rock.IRON));
	}

	@Test
	void vanillaMetalsAreOneIngotPlainAndTwoAtGravity() {
		assertEquals(1, RecoveryRoute.PLAIN_MINING.multipleOfPlainMining());
		assertEquals(2, RecoveryRoute.GRAVITY_OR_MAGNETIC.multipleOfPlainMining());
		NuggetBank bank = new NuggetBank();
		List<Milling.Yield> iron = Milling.concentrate(Rock.IRON, Separator.MAGNETIC, bank);
		List<Milling.Yield> copper = Milling.concentrate(Rock.COPPER, Separator.GRAVITY, bank);
		List<Milling.Yield> gold = Milling.concentrate(Rock.GOLD, Separator.GRAVITY, bank);
		assertEquals(List.of(new Milling.Yield("minecraft:iron_ingot", 2)), iron);
		assertEquals(List.of(new Milling.Yield("minecraft:copper_ingot", 2)), copper);
		assertEquals(List.of(new Milling.Yield("minecraft:gold_ingot", 2)), gold);
		assertTrue(bank.save().isEmpty());
		for (Rock rock : List.of(Rock.IRON, Rock.COPPER, Rock.GOLD)) {
			Mineral metal = rock.minerals().get(0);
			assertEquals(0, new BigDecimal("4").compareTo(rock.contained(metal)));
			assertTrue(new BigDecimal("2").compareTo(rock.contained(metal)) < 0);
		}
	}

	@Test
	void wildcatterGravityOresPayTwoAndNeverMoreThanTheBlock() {
		for (Rock rock : List.of(Rock.CASSITERITE, Rock.CHROMITE, Rock.SCHEELITE, Rock.ACANTHITE, Rock.HEAVY_MINERAL)) {
			List<Milling.Yield> yields = Milling.concentrate(rock, Separator.GRAVITY, new NuggetBank());
			assertFalse(yields.isEmpty(), rock.name());
			for (Mineral mineral : rock.minerals()) {
				if (mineral.separator() != Separator.GRAVITY) {
					continue;
				}
				BigDecimal contained = rock.contained(mineral);
				BigDecimal recovered = Recovery.recoveredKilograms(contained, RecoveryRoute.GRAVITY_OR_MAGNETIC);
				BigDecimal paid = mineral.itemAmount().multiply(BigDecimal.valueOf(2));
				assertTrue(recovered.compareTo(contained) <= 0);
				assertTrue(paid.compareTo(contained) <= 0, mineral + " paid " + paid + " from " + contained);
			}
		}
		assertEquals("wildcatter:tin_ingot", Mineral.CASSITERITE.phaseOneProductId());
		assertEquals(2, Milling.concentrate(Rock.CASSITERITE, Separator.GRAVITY, new NuggetBank()).get(0).count());
		assertFalse(Rock.GALENA.pays(Separator.GRAVITY));
		assertFalse(Rock.SPHALERITE.pays(Separator.GRAVITY));
	}

	@Test
	void graniteZirconMatchesTheSpecAndNineNuggetsMakeOneItem() {
		BigDecimal contained = Rock.GRANITE.contained(Mineral.ZIRCON);
		BigDecimal recovered = Recovery.recoveredKilograms(contained, RecoveryRoute.GRAVITY_OR_MAGNETIC);
		assertEquals(0, new BigDecimal("4").compareTo(recovered));
		assertTrue(recovered.compareTo(contained) < 0);
		NuggetBank bank = new NuggetBank();
		int nuggets = 0;
		BigDecimal paid = BigDecimal.ZERO;
		for (int i = 0; i < 2; i++) {
			for (Milling.Yield yield : Milling.concentrate(Rock.GRANITE, Separator.GRAVITY, bank)) {
				if (yield.itemId().equals(Mineral.ZIRCON.nuggetId())) {
					nuggets += yield.count();
				}
			}
			paid = paid.add(recovered);
		}
		assertEquals(1, nuggets);
		BigDecimal stillHeld = bank.held(Mineral.ZIRCON);
		BigDecimal accounted = Mineral.ZIRCON.nuggetAmount().multiply(BigDecimal.valueOf(nuggets)).add(stillHeld);
		assertEquals(0, accounted.compareTo(paid));
		assertEquals(0, Mineral.ZIRCON.nuggetAmount().multiply(BigDecimal.valueOf(9)).compareTo(Mineral.ZIRCON.itemAmount()));
		assertEquals("wildcatter:cassiterite", Mineral.CASSITERITE.mineralItemId());
		assertEquals("minecraft:iron_nugget", Mineral.IRON.nuggetId());
		assertEquals("tailings:copper_nugget", Mineral.COPPER.nuggetId());
		assertFalse(Mineral.IRON.vanillaNugget() && Mineral.COPPER.vanillaNugget());
		assertTrue(Mineral.IRON.vanillaNugget());
		assertTrue(Mineral.GOLD.vanillaNugget());
	}

	@Test
	void noRouteFromOrdinaryRockReturnsMoreThanTheRockHolds() {
		NuggetBank bank = new NuggetBank();
		int blocks = 40;
		for (int i = 0; i < blocks; i++) {
			Milling.concentrate(Rock.COBBLESTONE, Separator.MAGNETIC, bank);
		}
		BigDecimal recoveredEach = Recovery.recoveredKilograms(Rock.COBBLESTONE.contained(Mineral.IRON), RecoveryRoute.GRAVITY_OR_MAGNETIC);
		BigDecimal recovered = recoveredEach.multiply(BigDecimal.valueOf(blocks));
		BigDecimal contained = Rock.COBBLESTONE.contained(Mineral.IRON).multiply(BigDecimal.valueOf(blocks));
		assertTrue(recovered.compareTo(contained) <= 0);
		assertTrue(bank.held(Mineral.IRON).compareTo(Mineral.IRON.nuggetAmount()) < 0);
		assertTrue(bank.held(Mineral.IRON).compareTo(recovered) <= 0);
	}

	@Test
	void nuggetBankRoundTrips() {
		NuggetBank bank = new NuggetBank();
		bank.deposit(Mineral.ZIRCON, new BigDecimal("1.5"));
		NuggetBank loaded = new NuggetBank();
		loaded.load(bank.save());
		assertEquals(0, bank.held(Mineral.ZIRCON).compareTo(loaded.held(Mineral.ZIRCON)));
	}
}
