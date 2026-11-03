package dev.wildcatter.tailings.recovery;

import java.math.BigDecimal;

/** The mineral mass one block holds, and what one recovery route returns from it. */
public final class BlockMineral {
	private final String mineral;
	private final BigDecimal containedKilograms;

	public BlockMineral(String mineral, BigDecimal containedKilograms) {
		if (mineral == null || mineral.isBlank()) {
			throw new IllegalArgumentException("mineral name is required");
		}
		if (containedKilograms == null || containedKilograms.signum() < 0) {
			throw new IllegalArgumentException("contained mineral mass must be zero or more kilograms");
		}
		this.mineral = mineral;
		this.containedKilograms = containedKilograms;
	}

	public String mineral() {
		return mineral;
	}

	public BigDecimal containedKilograms() {
		return containedKilograms;
	}

	public BigDecimal recoveredKilograms(RecoveryRoute route) {
		return Recovery.recoveredKilograms(containedKilograms, route);
	}
}
