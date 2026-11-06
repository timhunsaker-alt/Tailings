package dev.wildcatter.tailings.haul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HaulTest {
	@Test
	void aCarHoldsNineStacksOfAnyMix() {
		CarLoad car = new CarLoad();
		assertEquals(0, car.add("minecraft:cobblestone", 64));
		assertEquals(0, car.add("minecraft:iron_ore", Haul.CAR_ITEMS - 64));
		assertEquals(10, car.add("minecraft:gravel", 10));
		assertEquals(Haul.CAR_ITEMS, car.total());
		assertEquals(64, car.contents().get("minecraft:cobblestone"));
		car.load(car.save());
		assertEquals(Haul.CAR_ITEMS, car.total());
	}

	@Test
	void aFedSectionIsLiveAlongItsLength() {
		assertTrue(Conductor.live(true));
		assertFalse(Conductor.live(false));
	}

	@Test
	void trainDamageCanBeTurnedOff() {
		assertTrue(dev.wildcatter.tailings.haul.TrainHarm.enabled(java.util.List.of()));
		assertFalse(dev.wildcatter.tailings.haul.TrainHarm.enabled(java.util.List.of("damage=false")));
		assertEquals(8.0F, dev.wildcatter.tailings.haul.TrainHarm.amount(java.util.List.of("amount=8")));
	}

	@Test
	void aBlockSectionHoldsOneTrain() {
		BlockSection section = new BlockSection();
		assertTrue(section.enter("north-1", "alpha"));
		assertTrue(section.enter("north-1", "alpha"));
		assertFalse(section.enter("north-1", "beta"));
		section.leave("north-1", "alpha");
		assertTrue(section.enter("north-1", "beta"));
		assertTrue(section.enter("", "beta"));
	}

	@Test
	void theSorterSendsEachTypeOutOfItsSideAndTheRestToWaste() {
		Sorter sorter = new Sorter();
		assertEquals(-1, sorter.route("minecraft:cobblestone"), "nothing set: nowhere to go");
		sorter.set(1, "minecraft:copper_ore");
		sorter.setWaste(2);
		assertEquals(1, sorter.route("minecraft:copper_ore"));
		assertEquals(2, sorter.route("minecraft:cobblestone"));
		sorter.set(3, "minecraft:copper_ore");
		assertEquals(3, sorter.route("minecraft:copper_ore"), "a type has one side");
		assertEquals(null, sorter.product(1));
		sorter.set(2, "minecraft:iron_ore");
		assertEquals(-1, sorter.waste(), "a side set to a type is no longer waste");
		Sorter back = new Sorter();
		back.load(sorter.save());
		assertEquals(sorter.save(), back.save());
		back.load("junk");
		assertEquals(-1, back.waste());
		assertEquals(Haul.MAX_CARS * Haul.CAR_ITEMS, Haul.POCKET_ITEMS, "the pocket holds one 10-car train");
	}
}
