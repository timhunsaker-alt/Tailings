package dev.wildcatter.tailings.recovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecoveryTest {
	private static final List<BigDecimal> MASSES = List.of(
			new BigDecimal("0"),
			new BigDecimal("0.001"),
			new BigDecimal("1"),
			new BigDecimal("8"),
			new BigDecimal("2700"),
			new BigDecimal("1000000"));

	@Test
	void ladderIsTwentyFiveFiftySeventyFiveNinetyFive() {
		assertEquals(25, RecoveryRoute.PLAIN_MINING.percent());
		assertEquals(50, RecoveryRoute.GRAVITY_OR_MAGNETIC.percent());
		assertEquals(75, RecoveryRoute.FLOTATION.percent());
		assertEquals(95, RecoveryRoute.FULL_RECOVERY.percent());
		assertEquals(new BigDecimal("0.25"), RecoveryRoute.PLAIN_MINING.fraction());
		assertEquals(new BigDecimal("0.50"), RecoveryRoute.GRAVITY_OR_MAGNETIC.fraction());
		assertEquals(new BigDecimal("0.75"), RecoveryRoute.FLOTATION.fraction());
		assertEquals(new BigDecimal("0.95"), RecoveryRoute.FULL_RECOVERY.fraction());
		assertEquals(1, RecoveryRoute.PLAIN_MINING.multipleOfPlainMining());
		assertEquals(2, RecoveryRoute.GRAVITY_OR_MAGNETIC.multipleOfPlainMining());
		assertEquals(3, RecoveryRoute.FLOTATION.multipleOfPlainMining());
		assertEquals(4, RecoveryRoute.FULL_RECOVERY.multipleOfPlainMining());
	}

	@Test
	void noRouteReturnsMoreMineralThanTheBlockHolds() {
		for (BigDecimal mass : MASSES) {
			BlockMineral block = new BlockMineral("cassiterite", mass);
			for (RecoveryRoute route : RecoveryRoute.values()) {
				BigDecimal recovered = block.recoveredKilograms(route);
				assertTrue(recovered.signum() >= 0, route + " returned a negative mass");
				assertTrue(recovered.compareTo(mass) <= 0, route + " returned " + recovered + " kg from " + mass + " kg");
				assertEquals(0, recovered.compareTo(Recovery.recoveredKilograms(mass, route)));
			}
		}
	}

	@Test
	void twoAndThreeTimesPlainMiningAreExact() {
		for (BigDecimal mass : MASSES) {
			BigDecimal plain = Recovery.recoveredKilograms(mass, RecoveryRoute.PLAIN_MINING);
			assertEquals(0, Recovery.recoveredKilograms(mass, RecoveryRoute.GRAVITY_OR_MAGNETIC).compareTo(plain.multiply(BigDecimal.valueOf(2))));
			assertEquals(0, Recovery.recoveredKilograms(mass, RecoveryRoute.FLOTATION).compareTo(plain.multiply(BigDecimal.valueOf(3))));
		}
	}

	@Test
	void fullRecoveryIsTheWrittenFourTimesException() {
		BigDecimal mass = new BigDecimal("8");
		BigDecimal plain = Recovery.recoveredKilograms(mass, RecoveryRoute.PLAIN_MINING);
		BigDecimal fourTimesPlain = plain.multiply(BigDecimal.valueOf(4));
		BigDecimal full = Recovery.recoveredKilograms(mass, RecoveryRoute.FULL_RECOVERY);
		assertEquals(0, fourTimesPlain.compareTo(mass), "4 x the 25% baseline is the whole block");
		assertTrue(full.compareTo(mass) < 0, "95% stays inside the block");
		assertTrue(full.compareTo(fourTimesPlain) < 0, "the played 4x returns 95%, not 100%");
		assertEquals(0, full.compareTo(new BigDecimal("7.6")));
	}

	@Test
	void routesAreNotAddedTogether() {
		BigDecimal mass = new BigDecimal("100");
		BigDecimal full = Recovery.recoveredKilograms(mass, RecoveryRoute.FULL_RECOVERY);
		int summed = 0;
		for (RecoveryRoute route : RecoveryRoute.values()) {
			summed += route.percent();
		}
		assertTrue(summed > 100);
		assertEquals(0, full.compareTo(new BigDecimal("95")));
	}

	@Test
	void laterRoutesRecoverAtLeastAsMuchAsEarlierOnes() {
		for (BigDecimal mass : MASSES) {
			BigDecimal previous = BigDecimal.ZERO;
			for (RecoveryRoute route : RecoveryRoute.values()) {
				BigDecimal recovered = Recovery.recoveredKilograms(mass, route);
				assertTrue(recovered.compareTo(previous) >= 0, route + " recovered less than the route before it");
				previous = recovered;
			}
		}
	}

	@Test
	void negativeMassIsRejected() {
		assertThrows(IllegalArgumentException.class, () -> Recovery.recoveredKilograms(new BigDecimal("-1"), RecoveryRoute.PLAIN_MINING));
		assertThrows(IllegalArgumentException.class, () -> new BlockMineral("galena", new BigDecimal("-0.01")));
		assertThrows(IllegalArgumentException.class, () -> new BlockMineral(" ", new BigDecimal("1")));
		assertThrows(IllegalArgumentException.class, () -> Recovery.recoveredKilograms(null, RecoveryRoute.PLAIN_MINING));
		assertThrows(IllegalArgumentException.class, () -> Recovery.recoveredKilograms(BigDecimal.ONE, null));
	}
}
