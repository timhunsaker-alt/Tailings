package dev.wildcatter.tailings.recovery;

import java.math.BigDecimal;

/**
 * One way of taking mineral out of a block. The fractions are the ladder in {@code docs/VISION.md}: plain mining 25%,
 * gravity or magnetic concentration 50%, flotation 75%, full recovery 95%. A later route replaces the earlier one.
 * The fractions are not added.
 */
public enum RecoveryRoute {
	PLAIN_MINING(25, 1),
	GRAVITY_OR_MAGNETIC(50, 2),
	FLOTATION(75, 3),
	/**
	 * Labeled 4x of plain mining. 95 / 25 is 3.8; the even 4x is the written exception in VISION.md, and the mass
	 * returned is still 95% of what the block holds, not 100%.
	 */
	FULL_RECOVERY(95, 4);

	private final int percent;
	private final int multipleOfPlainMining;

	RecoveryRoute(int percent, int multipleOfPlainMining) {
		this.percent = percent;
		this.multipleOfPlainMining = multipleOfPlainMining;
	}

	/** Percent of the mineral the block holds. */
	public int percent() {
		return percent;
	}

	public BigDecimal fraction() {
		return BigDecimal.valueOf(percent).movePointLeft(2);
	}

	/** Multiple of plain mining, as played. See {@link #FULL_RECOVERY}. */
	public int multipleOfPlainMining() {
		return multipleOfPlainMining;
	}
}
