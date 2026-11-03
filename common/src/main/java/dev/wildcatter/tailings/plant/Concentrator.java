package dev.wildcatter.tailings.plant;

import dev.wildcatter.tailings.recovery.Recovery;
import dev.wildcatter.tailings.recovery.RecoveryRoute;
import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Milling;
import dev.wildcatter.tailings.rock.NuggetBank;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * What the powered circuit and the flotation bank do to one block. Crushing changes size and pays nothing. A full
 * rougher-scavenger-cleaner bank pays the 75% route, and never more than the block holds.
 */
public final class Concentrator {
	private Concentrator() {
	}

	/** Jaw, cone and screen steps. Null when that machine does not take this size. */
	public static SizeClass crush(Machine machine, SizeClass size) {
		return switch (machine) {
			case JAW -> size == null ? SizeClass.CRUSHED : null;
			case CONE -> size == SizeClass.CRUSHED ? SizeClass.GRAVEL : null;
			case SCREEN -> size == SizeClass.CRUSHED ? SizeClass.GRAVEL : size == SizeClass.GRAVEL ? SizeClass.SAND : null;
			case BALL_MILL -> size == SizeClass.SAND ? SizeClass.FINES : null;
			default -> null;
		};
	}

	/**
	 * Pays {@link RecoveryRoute#FLOTATION} for every mineral in the rock that floats. Country rock adds nuggets to
	 * {@code bank}. An ore or a vanilla metal emits three products, the 3x of one plain smelt.
	 */
	public static List<Milling.Yield> floatBank(Rock rock, NuggetBank bank) {
		List<Milling.Yield> yields = new ArrayList<>();
		for (Mineral mineral : rock.minerals()) {
			if (!mineral.floats()) {
				continue;
			}
			BigDecimal contained = rock.contained(mineral);
			BigDecimal recovered = Recovery.recoveredKilograms(contained, RecoveryRoute.FLOTATION);
			if (recovered.compareTo(contained) > 0) {
				throw new IllegalStateException(mineral + " flotation returned more than the block holds");
			}
			if (rock.kind() == Rock.Kind.COUNTRY) {
				int nuggets = bank.deposit(mineral, recovered);
				if (nuggets > 0) {
					yields.add(new Milling.Yield(mineral.nuggetId(), nuggets));
				}
			} else {
				int count = RecoveryRoute.FLOTATION.multipleOfPlainMining();
				BigDecimal paid = mineral.itemAmount().multiply(BigDecimal.valueOf(count));
				if (paid.compareTo(contained) > 0) {
					throw new IllegalStateException(mineral + " would pay more than the block holds");
				}
				yields.add(new Milling.Yield(mineral.phaseOneProductId(), count));
			}
		}
		return yields;
	}
}
