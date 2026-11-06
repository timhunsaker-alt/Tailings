package dev.wildcatter.tailings.plant;

import dev.wildcatter.tailings.rock.Mineral;

/**
 * Phase 4's shares (docs/multiblocks/full-recovery.md). Each is a share of what reaches that step, so a plant stopping at any rung pays
 * that rung, and the whole line comes to the ladder's 95 % of what the block holds: 75 + 10 + 10 for what floats, 50 + 45 for what does
 * not. The top rung's even 4x is VISION.md's written exception. Plain Java.
 */
public final class FullRecoveryRates {
	private FullRecoveryRates() {
	}

	/** Of the 25 % the bank leaves, the scavengers take 10. */
	public static final double SCAVENGER = 10.0 / 25.0;
	/** Of the 15 % the scavengers leave, the leach takes 10. */
	public static final double LEACH_FLOATS = 10.0 / 15.0;
	/** Of the 50 % the spirals leave, the leach takes 45 (Tim, 2026-11-06: what does not float reaches 4x too). */
	public static final double LEACH_HEAVIES = 45.0 / 50.0;
	/** 20 kg of sulfuric acid a tonne of tails at 1.84 kg/L (proposed). */
	public static final double ACID_LITRES_PER_TONNE = 20 / 1.84;
	/** The strongest the liquor's mineral is carried, kg a kg (proposed, a game figure: the liquor's volume does not matter here). */
	public static final double LIQUOR_STRENGTH = 0.30;

	public static double scavengerShare(Mineral mineral) {
		return mineral.floats() ? SCAVENGER : 0;
	}

	public static double leachShare(Mineral mineral) {
		return mineral.floats() ? LEACH_FLOATS : mineral.spirals() ? LEACH_HEAVIES : 0;
	}

	public static double acidLitres(double slurryKg) {
		return slurryKg * PlantRates.OVERFLOW_SOLIDS / 1000 * ACID_LITRES_PER_TONNE;
	}

	/**
	 * What the electrowinning bank pays per kg won, so a full line comes to an even 4 items (VISION.md's written exception). What floats
	 * has 3.4 items in hand when the leach adds 0.4: paying that at 1.5 makes 4. What does not float has 2 when the leach adds 1.8.
	 */
	public static double topRungPay(Mineral mineral) {
		return mineral.floats() ? 0.6 / 0.4 : 2.0 / 1.8;
	}
}
