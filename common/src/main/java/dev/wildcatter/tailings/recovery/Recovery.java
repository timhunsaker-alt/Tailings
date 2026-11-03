package dev.wildcatter.tailings.recovery;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Mineral returned from a block. The contained mass is an input (Wildcatter's game grade, or a written list for
 * ordinary rock). This class only applies a route's recovery, and it will not return more than the block holds.
 */
public final class Recovery {
	private static final int SCALE = 8;

	private Recovery() {
	}

	public static BigDecimal recoveredKilograms(BigDecimal containedKilograms, RecoveryRoute route) {
		if (containedKilograms == null) {
			throw new IllegalArgumentException("contained mineral mass is required");
		}
		if (route == null) {
			throw new IllegalArgumentException("recovery route is required");
		}
		if (containedKilograms.signum() < 0) {
			throw new IllegalArgumentException("a block cannot hold a negative mineral mass");
		}
		BigDecimal recovered = containedKilograms
				.multiply(BigDecimal.valueOf(route.percent()))
				.divide(BigDecimal.valueOf(100), SCALE, RoundingMode.DOWN);
		if (recovered.compareTo(containedKilograms) > 0) {
			throw new IllegalStateException(route + " returned more mineral than the block holds");
		}
		return recovered;
	}
}
