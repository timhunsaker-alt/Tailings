package dev.wildcatter.tailings.plant;

import dev.wildcatter.tailings.rock.Rock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;

/**
 * Kilograms of reagent sitting in the conditioning tank. One item dropped in adds one kilogram, and the tank holds back an item
 * while any part of its kilogram is left, so the items in the tank are always {@link #items} of each reagent.
 */
public final class ReagentStock {
	private final Map<Reagent, BigDecimal> kilograms = new EnumMap<>(Reagent.class);

	public BigDecimal kilograms(Reagent reagent) {
		return kilograms.getOrDefault(reagent, BigDecimal.ZERO);
	}

	public void addKilogram(Reagent reagent) {
		kilograms.put(reagent, kilograms(reagent).add(BigDecimal.ONE));
	}

	/** The items of this reagent the tank should hold: one for each kilogram or part of one left. */
	public int items(Reagent reagent) {
		return kilograms(reagent).setScale(0, RoundingMode.CEILING).intValueExact();
	}

	/** True if it holds the dose for {@code tonnes} of feed solids (copper sulfate only with sphalerite in the feed). */
	public boolean covers(double tonnes, boolean sphalerite) {
		return missing(tonnes, sphalerite).isEmpty();
	}

	/** The reagents short for {@code tonnes} of feed, by name; empty when it has them all. */
	public String missing(double tonnes, boolean sphalerite) {
		StringBuilder out = new StringBuilder();
		for (Reagent reagent : Reagent.values()) {
			if (reagent.requiredFor(sphalerite) && kilograms(reagent).compareTo(reagent.kilogramsFor(tonnes)) < 0) {
				out.append(out.length() > 0 ? ", " : "").append(reagent.displayName());
			}
		}
		return out.toString();
	}

	/** Doses {@code tonnes} of feed solids. The caller has already checked {@link #covers(double, boolean)}. */
	public void consume(double tonnes, boolean sphalerite) {
		for (Reagent reagent : Reagent.values()) {
			if (!reagent.requiredFor(sphalerite)) {
				continue;
			}
			BigDecimal left = kilograms(reagent).subtract(reagent.kilogramsFor(tonnes));
			if (left.signum() <= 0) {
				kilograms.remove(reagent);
			} else {
				kilograms.put(reagent, left);
			}
		}
	}

	public boolean covers(Rock rock) {
		for (Map.Entry<Reagent, BigDecimal> need : Reagent.dose(rock).entrySet()) {
			if (kilograms(need.getKey()).compareTo(need.getValue()) < 0) {
				return false;
			}
		}
		return true;
	}

	/** Subtracts one block's dose. The caller has already checked {@link #covers}. */
	public void consume(Rock rock) {
		for (Map.Entry<Reagent, BigDecimal> need : Reagent.dose(rock).entrySet()) {
			BigDecimal left = kilograms(need.getKey()).subtract(need.getValue());
			if (left.signum() <= 0) {
				kilograms.remove(need.getKey());
			} else {
				kilograms.put(need.getKey(), left);
			}
		}
	}

	public String missing(Rock rock) {
		StringBuilder out = new StringBuilder();
		for (Map.Entry<Reagent, BigDecimal> need : Reagent.dose(rock).entrySet()) {
			if (kilograms(need.getKey()).compareTo(need.getValue()) < 0) {
				if (out.length() > 0) {
					out.append(", ");
				}
				out.append(need.getKey().displayName());
			}
		}
		return out.toString();
	}

	public String save() {
		StringBuilder out = new StringBuilder();
		for (Map.Entry<Reagent, BigDecimal> entry : kilograms.entrySet()) {
			if (out.length() > 0) {
				out.append(',');
			}
			out.append(entry.getKey().name()).append('=').append(entry.getValue().toPlainString());
		}
		return out.toString();
	}

	public void load(String text) {
		kilograms.clear();
		if (text == null || text.isBlank()) {
			return;
		}
		for (String part : text.split(",")) {
			int cut = part.indexOf('=');
			kilograms.put(Reagent.valueOf(part.substring(0, cut)), new BigDecimal(part.substring(cut + 1)));
		}
	}
}
