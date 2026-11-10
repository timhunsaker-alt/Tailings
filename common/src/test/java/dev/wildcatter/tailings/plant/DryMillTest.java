package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.recovery.RecoveryRoute;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The dry mill's separation maths (docs/multiblocks/dry-mill.md, L6). */
class DryMillTest {
	private static final double EPS = 1e-9;

	@Test
	void eachMineralReportsToItsRealSeparator() {
		assertEquals(Machine.RER_SEPARATOR, DryMill.separator(Mineral.ILMENITE));
		assertEquals(Machine.HT_ROLL_SEPARATOR, DryMill.separator(Mineral.RUTILE));
		assertEquals(Machine.HT_ROLL_SEPARATOR, DryMill.separator(Mineral.CASSITERITE));
		assertEquals(Machine.IRM_SEPARATOR, DryMill.separator(Mineral.MONAZITE));
		assertEquals(Machine.IRM_SEPARATOR, DryMill.separator(Mineral.ZIRCON));
		assertNull(DryMill.separator(Mineral.IRON));
		assertTrue(!DryMill.takes(Mineral.CHROMITE) && !DryMill.takes(Mineral.GALENA));
		assertEquals(Machine.RER_SEPARATOR, DryMill.next(Machine.FLUID_BED_DRYER));
		assertNull(DryMill.next(Machine.IRM_SEPARATOR));
	}

	@Test
	void theLineSplitsAMixedConcentrateIntoFour() {
		HeavyMinerals hmc = new HeavyMinerals();
		hmc.add(Mineral.ILMENITE, 55);
		hmc.add(Mineral.ZIRCON, 25);
		hmc.add(Mineral.RUTILE, 5);
		hmc.add(Mineral.MONAZITE, 3);
		DryMill.Pass rer = DryMill.separate(Machine.RER_SEPARATOR, hmc);
		assertEquals(55, rer.product().kg(Mineral.ILMENITE), EPS);
		assertEquals(33, rer.rest().total(), EPS);
		DryMill.Pass ht = DryMill.separate(Machine.HT_ROLL_SEPARATOR, rer.rest());
		assertEquals(5, ht.product().total(), EPS);
		DryMill.Pass irm = DryMill.separate(Machine.IRM_SEPARATOR, ht.rest());
		assertEquals(3, irm.product().kg(Mineral.MONAZITE), EPS);
		assertEquals(25, irm.product().kg(Mineral.ZIRCON), EPS);
		assertTrue(irm.rest().isEmpty());
	}

	/** Spirals 50 %, scavenger spirals 45 % of the block, the dry mill paying it at 1.11: the even 4x, never over the 95 % mass. */
	@Test
	void theLadderComesToFourAndNoMore() {
		double block = 225;   // a heavy mineral sand block's zircon, kg
		double spirals = block * RecoveryRoute.GRAVITY_OR_MAGNETIC.fraction().doubleValue();
		double left = block - spirals;
		SlurryAssay feed = SlurryAssay.of(Map.of(Mineral.ZIRCON.mineralItemId(), left / 1000, Mineral.GALENA.mineralItemId(), 1.0 / 1000));
		DryMill.Split split = DryMill.scavenge(feed, 1000, 0.30);
		double hmc = DryMill.hmcIn(split.concentrateAssay(), split.concentrate()).kg(Mineral.ZIRCON);
		assertEquals(block * 0.45, hmc, 1e-6);
		assertEquals((spirals + hmc) / block, 0.95, 1e-9);
		double items = (spirals + hmc * DryMill.pay(Mineral.ZIRCON)) / Mineral.ZIRCON.itemAmount().doubleValue();
		assertEquals(4, items, 1e-6);
		// what the scavenger spirals leave is marked: the leach and a second set of spirals see no zircon
		assertEquals(0, split.restAssay().get(Mineral.ZIRCON.mineralItemId()), EPS);
		assertEquals(block * 0.05, split.restAssay().kg(DryMill.scavengedKey(Mineral.ZIRCON), split.rest()), 1e-6);
		assertEquals(1, split.restAssay().kg(Mineral.GALENA.mineralItemId(), split.rest()), 1e-6);
		DryMill.Split again = DryMill.scavenge(split.restAssay(), split.rest(), 0.30);
		assertEquals(0, again.concentrate(), EPS);
	}

	@Test
	void bagsAreWholeKgAndLoseNothing() {
		HeavyMinerals store = new HeavyMinerals();
		store.add(Mineral.ZIRCON, 70.4);
		store.add(Mineral.ILMENITE, 40.3);
		store.add(Mineral.MONAZITE, 0.5);
		double before = store.total();
		Map<Mineral, Integer> bag = DryMill.bag(store);
		int inBag = bag.values().stream().mapToInt(Integer::intValue).sum();
		assertTrue(inBag <= DryMill.ITEM_KG && inBag >= DryMill.ITEM_KG - 3, "a bag is about 100 kg, got " + inBag);
		assertEquals(before, inBag + store.total(), 1e-9);
		assertNull(DryMill.bag(store));
	}

	@Test
	void theDryerBurnsRealGas() {
		// 0.3 GJ a tonne on 38 MJ/m3 pipeline gas: about 7.9 m3 a tonne, 16 m3 an hour at 2 t/h
		assertEquals(7.89, DryMill.gasM3(1000, 38e6), 0.01);
		assertEquals(2000, DryMill.KG_PER_TICK * 72_000, 1e-9);
	}

	@Test
	void hmcKeysRoundTrip() {
		for (Mineral mineral : Mineral.values()) {
			assertEquals(DryMill.takes(mineral) ? mineral : null, DryMill.fromHmcKey(DryMill.hmcKey(mineral)));
		}
		assertNull(DryMill.fromHmcKey(Mineral.ZIRCON.mineralItemId()));
		assertNull(DryMill.fromHmcKey(DryMill.scavengedKey(Mineral.ZIRCON)));
	}
}
