package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Selective lead-zinc flotation pays both at the rung; a plain bank on feed holding both loses the zinc it floats. */
class SelectiveTest {
	private static final String LEAD = Mineral.GALENA.mineralItemId();
	private static final String ZINC = Mineral.SPHALERITE.mineralItemId();
	private static final String COPPER = Mineral.COPPER.mineralItemId();
	private static final SlurryAssay BOTH = SlurryAssay.of(Map.of(LEAD, 0.01, ZINC, 0.02));

	private static Selective.Split bank(SlurryAssay feed) {
		return Selective.bank(feed, 100, 0.75, 0.30);
	}

	private static double conc(Selective.Split split, String id) {
		return split.concentrateAssay().kg(id, split.concentrate());
	}

	private static double tails(Selective.Split split, String id) {
		return split.tailsAssay().kg(id, split.tails());
	}

	@Test
	void aPlainBankLosesTheZincItFloats() {
		Selective.Split split = bank(BOTH);
		assertEquals(0.75, conc(split, LEAD), 1e-9);
		assertEquals(0, conc(split, ZINC), 1e-9);
		assertEquals(0.25, tails(split, LEAD), 1e-9);
		assertEquals(0.5, tails(split, ZINC), 1e-9, "the rest goes on, for the scavengers and the leach");
	}

	@Test
	void feedWithOnlyOneOfTheTwoPaysAsBefore() {
		assertEquals(1.5, conc(bank(SlurryAssay.of(Map.of(ZINC, 0.02))), ZINC), 1e-9);
		assertEquals(0.75, conc(bank(SlurryAssay.of(Map.of(LEAD, 0.01))), LEAD), 1e-9);
	}

	@Test
	void selectiveFloatsLeadThenZincEachAtTheRung() {
		Selective.Split lead = bank(Selective.depress(BOTH));
		assertEquals(0.75, conc(lead, LEAD), 1e-9);
		assertEquals(0, conc(lead, ZINC), 1e-9);
		assertEquals(0, conc(lead, Selective.DEPRESSED), 1e-9);

		Selective.Split zinc = Selective.bank(Selective.activate(lead.tailsAssay()), lead.tails(), 0.75, 0.30);
		assertEquals(1.5, conc(zinc, ZINC), 1e-9);
		assertEquals(0, conc(zinc, LEAD), 1e-9, "the lead the first bank left does not float again");
		assertEquals(0.25, tails(zinc, LEAD), 1e-9, "and the scavengers still see it by name");
		assertEquals(0.5, tails(zinc, ZINC), 1e-9);
	}

	@Test
	void theFirstBankStillFloatsEverythingElse() {
		Selective.Split lead = bank(Selective.depress(SlurryAssay.of(Map.of(LEAD, 0.01, ZINC, 0.02, COPPER, 0.01))));
		assertEquals(0.75, conc(lead, COPPER), 1e-9);
	}

	@Test
	void withNoActivationTankTheZincStaysDown() {
		Selective.Split lead = bank(Selective.depress(BOTH));
		assertEquals(2, tails(lead, Selective.DEPRESSED), 1e-9);
		assertEquals(0, tails(lead, ZINC), 1e-9);
	}

	@Test
	void eachTankDosesWhatItNeeds() {
		assertTrue(Selective.conditioning(true, true).contains(Reagent.ZINC_SULFATE));
		assertFalse(Selective.conditioning(true, true).contains(Reagent.COPPER_SULFATE));
		assertTrue(Selective.conditioning(false, true).contains(Reagent.COPPER_SULFATE));
		assertFalse(Selective.conditioning(true, false).contains(Reagent.ZINC_SULFATE));
		assertTrue(Selective.activating(true).contains(Reagent.COPPER_SULFATE));
		assertFalse(Selective.activating(false).contains(Reagent.COPPER_SULFATE));
	}
}
