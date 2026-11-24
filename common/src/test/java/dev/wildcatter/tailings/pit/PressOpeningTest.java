package dev.wildcatter.tailings.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The press's opening as drawn (fix3-hi04): a wide gap at each plate, the cake falling the full height under the pack in time. */
class PressOpeningTest {
	@Test
	void eachShiftedPlateOpensTheFullGap() {
		for (int k = 1; k < PressOpening.PLATES; k++) {
			float s = PressOpening.shiftAt(k) + PressOpening.SHIFT_MOVE;
			float gap = (PressOpening.PLATE_Z0 + k * PressOpening.PLATE_PITCH + PressOpening.plate(k, s, true))
					- (PressOpening.PLATE_Z0 + (k - 1) * PressOpening.PLATE_PITCH + PressOpening.PLATE_T + PressOpening.plate(k - 1, s, true));
			assertTrue(gap >= PressOpening.GAP, "chamber " + k + " opens " + gap + " m");
		}
		assertEquals(0.8f, PressOpening.GAP, 1e-6);
	}

	@Test
	void theTailOpensTheGapShortOfTheStand() {
		assertTrue(PressOpening.TAIL_Z + PressOpening.TAIL_T + PressOpening.GAP <= PressOpening.STAND_Z + 1e-5f);
		assertEquals(PressOpening.GAP, PressOpening.tail(PressOpening.OPEN_AT + PressOpening.TAIL_MOVE, true), 1e-6);
		assertEquals(0, PressOpening.tail(PressOpening.TAIL_MOVE, true), 1e-6);
	}

	@Test
	void everyCakeFallsTheFullHeightAndLandsBeforeTheNextOpens() {
		for (int k = 0; k <= PressOpening.PLATES; k++) {
			// an open fall of at least 1.5 m under the plates, over the pans' skirts
			float pan = PressOpening.panAt(PressOpening.chamberZ(k));
			assertTrue(PressOpening.PLATE_BOTTOM - (pan + PressOpening.SKIRT) >= 1.5f, "chamber " + k + " over a pan at " + pan);
			if (k > 0 && k < PressOpening.PLATES) {
				assertTrue(PressOpening.openedAt(k) + PressOpening.allLandedAfter(k) < PressOpening.openedAt(k - 1), "chamber " + k);
			}
		}
	}

	@Test
	void theLastCakeLandsJustBeforeTheGameCountsIt() {
		float last = PressOpening.openedAt(0) + PressOpening.allLandedAfter(0);
		assertTrue(last < DryStack.CYCLE_SECONDS && last > DryStack.CYCLE_SECONDS - 0.5f, "last slab lands at " + last);
		assertEquals(5.6f, PressOpening.SHIFT_EVERY, 0.1f);
	}
}
