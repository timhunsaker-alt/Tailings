package dev.wildcatter.tailings.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The dry stack's figures (docs/multiblocks/open-pit-p4-dry-stack.md): water, the press's batch, the load-out, berms, crests, grades. */
class DryStackTest {
	@Test
	void theThickenerSendsBackAboutOnePointTwoLitresAKg() {
		assertEquals(1.19, DryStack.overflowPerKgSolids(), 0.01);
		// 50 t/h of solids: about 16.5 L/s (the spec)
		assertEquals(16.5, DryStack.overflowPerKgSolids() * 50_000 / 3600, 0.2);
	}

	@Test
	void theFiltrateIsAboutHalfALitreAKg() {
		assertEquals(0.49, DryStack.filtratePerKgSolids(), 0.01);
	}

	@Test
	void aCycleIsFifteenTonnesInFifteenMinutes() {
		assertEquals(15_000, DryStack.BATCH_KG, 1e-9);
		assertEquals(8.33, DryStack.blocks(DryStack.BATCH_KG), 0.01);
		assertEquals("filling", DryStack.phase(0));
		assertEquals("squeezing", DryStack.phase(200));
		assertEquals("blowing", DryStack.phase(400));
		assertEquals("opening and dropping the cake", DryStack.phase(800));
	}

	@Test
	void theLoadOutFillsATruckInUnderAMinute() {
		assertEquals(45.5, DryStack.fillSeconds(91_000), 1e-9);
		assertEquals(181.5, DryStack.fillSeconds(363_000), 1e-9);
		// a 91 t truck is about 50 blocks of dry tails, the 400 t bin 222
		assertEquals(50.6, DryStack.blocks(91_000), 0.1);
		assertEquals(PitMass.DRY_TAILS_KG, PitMass.kg(PitMass.DRY_TAILS));
		assertTrue(PitMass.ownBlock(PitMass.DRY_TAILS) && PitMass.soft(PitMass.DRY_TAILS) && !PitMass.soil(PitMass.DRY_TAILS));
	}

	@Test
	void bermsAreMidAxleHigh() {
		assertEquals(1, DryStack.bermBlocks(DryStack.TRUCK_WHEEL_RADIUS));   // 2.7 m wheel
		assertEquals(2, DryStack.bermBlocks(2.0));                           // the ultra's 4 m wheel
	}

	@Test
	void gradesStepABlockEachRun() {
		assertEquals(0, DryStack.Grade.LEVEL.stepsAfter(100));
		assertEquals(2, DryStack.Grade.TEN.stepsAfter(20));
		assertEquals(1, DryStack.Grade.EIGHT.stepsAfter(20));
		assertEquals(0, DryStack.Grade.FIVE.stepsAfter(19.9));
	}

	/** A flat road at y 10 running +z, with a crest {@code crestZ} out and a ridge {@code high} at {@code bermZ}. */
	private static Crest.Ground road(int crestZ, int bermZ, int high) {
		Set<Long> ridge = new HashSet<>();
		return (x, y, z) -> {
			if (y < 10) {
				return z < crestZ || y < 2;   // the dump's floor far below
			}
			return z == bermZ && y < 10 + high && !ridge.contains((long) x);
		};
	}

	@Test
	void aCrestWithABermIsFound() {
		Crest.Look look = Crest.look(road(5, 3, 1), 0.5, 10, 0.5, 0, 1, 6.2, 1);
		assertEquals(Crest.Kind.BERM, look.kind());
		assertEquals(3, look.bermAt(), 0.51);
		assertEquals(5, look.crestAt(), 0.51);
	}

	@Test
	void aCrestWithNoBermIsNoBerm() {
		assertEquals(Crest.Kind.NO_BERM, Crest.look(road(5, -100, 1), 0.5, 10, 0.5, 0, 1, 6.2, 1).kind());
		// too low for the truck: a 1-block ridge is no berm for a 2-block truck
		assertEquals(Crest.Kind.NO_BERM, Crest.look(road(5, 3, 1), 0.5, 10, 0.5, 0, 1, 6.2, 2).kind());
	}

	@Test
	void noDropIsAPaddock() {
		assertEquals(Crest.Kind.PADDOCK, Crest.look(road(30, -100, 1), 0.5, 10, 0.5, 0, 1, 6.2, 1).kind());
		assertFalse(Crest.drop(road(30, -100, 1), 0, 10, 12));
		assertTrue(Crest.drop(road(5, -100, 1), 0, 10, 6));
	}
}
