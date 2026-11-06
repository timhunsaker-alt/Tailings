package dev.wildcatter.tailings.plant;

import dev.wildcatter.tailings.rock.Mineral;
import java.util.EnumMap;
import java.util.Map;

/**
 * The pressure filter's cake (docs/multiblocks/flotation-bank.md): the mineral it has filtered and not yet paid, by mineral. It pays
 * a nugget for each nugget's worth (the nuggets rule, crushing-circuit.md); the filter packs each nine nuggets into the item the
 * ladder pays. Masses are in each mineral's own unit ({@link Mineral#unit()}). Plain Java.
 */
public final class FilterCake {
	/** Rounding slack, so a block's worth that went through many blends still pays its last nugget. */
	private static final double SLACK = 1e-6;

	private final Map<Mineral, Double> held = new EnumMap<>(Mineral.class);

	public double held(Mineral mineral) {
		return held.getOrDefault(mineral, 0.0);
	}

	public void add(Mineral mineral, double amount) {
		if (amount > 0) {
			held.merge(mineral, amount, Double::sum);
		}
	}

	/** The whole nuggets of {@code mineral} the cake holds, taken out of it. */
	public int takeNuggets(Mineral mineral) {
		double nugget = mineral.nuggetAmount().doubleValue();
		int count = (int) Math.floor(held(mineral) / nugget + SLACK);
		if (count > 0) {
			double left = held(mineral) - count * nugget;
			if (left <= nugget * SLACK) {
				held.remove(mineral);
			} else {
				held.put(mineral, left);
			}
		}
		return count;
	}

	/** Gives back nuggets that did not fit in the filter's output. */
	public void putBack(Mineral mineral, int nuggets) {
		add(mineral, nuggets * mineral.nuggetAmount().doubleValue());
	}

	public Iterable<Mineral> minerals() {
		return java.util.List.copyOf(held.keySet());
	}

	public String save() {
		StringBuilder out = new StringBuilder();
		held.forEach((m, v) -> out.append(out.length() == 0 ? "" : ",").append(m.name()).append('=').append(v));
		return out.toString();
	}

	public void load(String text) {
		held.clear();
		if (text == null || text.isBlank()) {
			return;
		}
		for (String part : text.split(",")) {
			try {
				int eq = part.indexOf('=');
				held.put(Mineral.valueOf(part.substring(0, eq)), Double.parseDouble(part.substring(eq + 1)));
			} catch (RuntimeException ex) {
				// left out
			}
		}
	}
}
