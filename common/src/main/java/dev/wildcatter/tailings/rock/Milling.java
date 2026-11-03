package dev.wildcatter.tailings.rock;

import dev.wildcatter.tailings.recovery.Recovery;
import dev.wildcatter.tailings.recovery.RecoveryRoute;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * The starter line. The stamp mill and the hand screen only change size: they pay nothing. The shaking table and the
 * magnetic separator pay {@link RecoveryRoute#GRAVITY_OR_MAGNETIC} and nothing over what the rock holds.
 */
public final class Milling {
	private Milling() {
	}

	/** One product id and a count. */
	public record Yield(String itemId, int count) {
	}

	/** Unbroken rock in, crushed rock out. Null when the feed is already crushed. */
	public static SizeClass stamp(SizeClass size) {
		return size == null ? SizeClass.CRUSHED : null;
	}

	/** Crushed rock to gravel, gravel to sand. Sand does not become fines here. */
	public static SizeClass screen(SizeClass size) {
		if (size == SizeClass.CRUSHED) {
			return SizeClass.GRAVEL;
		}
		if (size == SizeClass.GRAVEL) {
			return SizeClass.SAND;
		}
		return null;
	}

	public static boolean acceptsConcentrate(SizeClass size, Separator separator, Rock rock) {
		return size == SizeClass.SAND && rock.pays(separator);
	}

	/**
	 * Pays the 50% route for one sand-sized charge of {@code rock}. Country rock adds to {@code bank} and may emit
	 * nuggets. An ore or a vanilla metal emits two products, the 2x of one plain smelt, and does not touch the bank.
	 */
	public static List<Yield> concentrate(Rock rock, Separator separator, NuggetBank bank) {
		if (!rock.pays(separator)) {
			throw new IllegalArgumentException(rock + " does not pay on " + separator);
		}
		List<Yield> yields = new ArrayList<>();
		if (rock.kind() == Rock.Kind.COUNTRY) {
			for (Mineral mineral : rock.minerals()) {
				if (mineral.separator() != separator) {
					continue;
				}
				BigDecimal contained = rock.contained(mineral);
				BigDecimal recovered = Recovery.recoveredKilograms(contained, RecoveryRoute.GRAVITY_OR_MAGNETIC);
				int nuggets = bank.deposit(mineral, recovered);
				if (nuggets > 0) {
					yields.add(new Yield(mineral.nuggetId(), nuggets));
				}
			}
			return yields;
		}
		int count = RecoveryRoute.GRAVITY_OR_MAGNETIC.multipleOfPlainMining();
		for (Mineral mineral : rock.minerals()) {
			if (mineral.separator() != separator) {
				continue;
			}
			BigDecimal contained = rock.contained(mineral);
			BigDecimal recovered = Recovery.recoveredKilograms(contained, RecoveryRoute.GRAVITY_OR_MAGNETIC);
			BigDecimal paid = mineral.itemAmount().multiply(BigDecimal.valueOf(count));
			if (paid.compareTo(contained) > 0 || recovered.compareTo(contained) > 0) {
				throw new IllegalStateException(mineral + " would pay more than the block holds");
			}
			yields.add(new Yield(mineral.phaseOneProductId(), count));
		}
		return yields;
	}
}
