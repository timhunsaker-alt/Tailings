package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.wildcatter.tailings.rock.Mineral;
import org.junit.jupiter.api.Test;

class FullRecoveryRatesTest {
	/** Of a block: 75 + 10 + 10 for what floats, 50 + 45 for what does not, and each pays an even 4 items at the top. */
	@Test
	void theLadderComesTo95PercentAndPays4() {
		double floats = 0.75 + 0.25 * FullRecoveryRates.SCAVENGER;
		double left = 1 - floats;
		double leached = left * FullRecoveryRates.leachShare(Mineral.COPPER);
		assertEquals(0.95, floats + leached, 1e-9);
		assertEquals(4, floats * 4 + leached * 4 * FullRecoveryRates.topRungPay(Mineral.COPPER), 1e-9);
		double heavy = 0.5 * FullRecoveryRates.leachShare(Mineral.CASSITERITE);
		assertEquals(0.95, 0.5 + heavy, 1e-9);
		assertEquals(4, 2 + heavy * 4 * FullRecoveryRates.topRungPay(Mineral.CASSITERITE), 1e-9);
		assertEquals(0, FullRecoveryRates.scavengerShare(Mineral.CASSITERITE));
	}
}
