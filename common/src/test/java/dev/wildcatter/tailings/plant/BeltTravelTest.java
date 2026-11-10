package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.tailings.rock.Mineral;
import org.junit.jupiter.api.Test;

/** A belt run's load rides to its head end at the belt's speed; a filter pays a vanilla metal's kg as ingots. */
class BeltTravelTest {
	@Test
	void rockReachesTheHeadEndAfterTheRunsLengthOverItsSpeed() {
		BeltTravel belt = new BeltTravel();
		belt.add(100, 10);
		int ticks = 0;
		while (belt.ready() <= 0) {
			belt.advance(PlantRates.BELT_METRES_PER_SECOND / 20);
			ticks++;
		}
		assertEquals(100, ticks, 1, "10 m at 2 m/s is 5 s, 100 ticks");
		assertEquals(5, BeltTravel.transitSeconds(10, PlantRates.BELT_METRES_PER_SECOND), 1e-9);
		assertEquals(100, belt.take(1000), 1e-9);
		assertTrue(belt.isEmpty());
	}

	@Test
	void rockDroppedNearTheHeadArrivesSoonerAndTheLoadIsDrawnWhereItRides() {
		BeltTravel belt = new BeltTravel();
		belt.add(50, 20);
		belt.add(30, 2.5);
		assertEquals(30, belt.between(2, 3), 1e-9);
		assertEquals(50, belt.between(19, 21), 1e-9);
		belt.advance(2.5);
		assertEquals(30, belt.ready(), 1e-9);
		assertEquals(30, belt.take(1000), 1e-9, "only what has arrived is delivered");
		assertEquals(50, belt.total(), 1e-9);
	}

	@Test
	void theDrivesLoadIsTheTruth() {
		BeltTravel belt = new BeltTravel();
		belt.add(10, 5);
		belt.match(25);
		assertEquals(15, belt.ready(), 1e-9, "rock put on outside the run is at the head end");
		belt.match(5);
		assertEquals(5, belt.total(), 1e-9);
		BeltTravel back = new BeltTravel();
		back.load(belt.save());
		assertEquals(belt.total(), back.total(), 1e-9);
	}

	@Test
	void aFilterCountsAVanillaMetalsKgAsIngots() {
		FilterCake cake = new FilterCake();
		cake.addKg(Mineral.COPPER, 3 * Mineral.INGOT_KG);
		assertEquals(3, cake.held(Mineral.COPPER), 1e-9);
		cake.addKg(Mineral.GALENA, 12);
		assertEquals(12, cake.held(Mineral.GALENA), 1e-9);
	}

	@Test
	void rockFedOnEveryTickStillRidesOn() {
		BeltTravel belt = new BeltTravel();
		int ticks = 0;
		while (belt.ready() <= 0 && ticks < 1000) {
			belt.add(1, 7);
			belt.advance(PlantRates.BELT_METRES_PER_SECOND / 20);
			ticks++;
		}
		assertTrue(ticks <= 71, "a 7 m run fed every tick delivers in about 3.5 s, took " + ticks + " ticks");
	}
}
