package dev.wildcatter.tailings.haul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class HaulTest {
	@Test
	void aCarHoldsThreeTonnesOfAnyMix() {
		CarLoad car = new CarLoad();
		assertEquals(0, car.add("minecraft:cobblestone", 1_000));
		assertEquals(0, car.add("minecraft:iron_ore", 2_000));
		assertEquals(500, car.add("minecraft:gravel", 500));
		assertEquals(3_000, car.total());
		assertEquals(1_000, car.contents().get("minecraft:cobblestone"));
		car.load(car.save());
		assertEquals(3_000, car.total());
	}

	@Test
	void theEleventhCarDoesNotCouple() {
		Train train = new Train("t1");
		for (int i = 0; i < Haul.MAX_CARS; i++) {
			assertTrue(train.couple(new CarLoad()));
		}
		assertFalse(train.couple(new CarLoad()));
		assertEquals(10, train.cars());
	}

	@Test
	void theConductorIsLiveOnlyUnderALocomotive() {
		assertTrue(Conductor.live(true, true));
		assertFalse(Conductor.live(true, false));
		assertFalse(Conductor.live(false, true));
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
	void declineNeedsTheRackAndADeadRailStopsTheTrain() {
		assertEquals(Motion.Halt.NONE, Motion.halt(true, false, false, true, true));
		assertEquals(Motion.Halt.UNPOWERED, Motion.halt(false, false, false, true, true));
		assertEquals(Motion.Halt.NO_RACK, Motion.halt(true, true, false, true, true));
		assertEquals(Motion.Halt.NONE, Motion.halt(true, true, true, true, true));
		assertEquals(Motion.Halt.SIGNAL, Motion.halt(true, false, false, false, true));
		assertEquals(Motion.Halt.POCKET, Motion.halt(true, false, false, true, false));
		assertEquals(Haul.FLAT_TICKS, Motion.ticks(false));
		assertEquals(Haul.DECLINE_TICKS, Motion.ticks(true));
	}

	@Test
	void thePocketBuffersOneTrainAndSortsTheRestToWaste() {
		Sorter sorter = new Sorter(Set.of("minecraft:iron_ore"));
		assertTrue(sorter.tip(Map.of("minecraft:iron_ore", 2_000, "minecraft:cobblestone", 1_000)));
		assertEquals(2_000, sorter.bin("minecraft:iron_ore"));
		assertEquals(1_000, sorter.waste());
		assertEquals(3_000, sorter.pocket());
		assertTrue(sorter.tip(Map.of("minecraft:cobblestone", Haul.POCKET_KG - 3_000)));
		assertFalse(sorter.tip(Map.of("minecraft:cobblestone", 1_000)));
		sorter.clearPocket();
		assertTrue(sorter.tip(Map.of("minecraft:cobblestone", 1_000)));
	}
}
