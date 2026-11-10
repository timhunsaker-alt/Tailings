package dev.wildcatter.tailings.plant;

import dev.wildcatter.tailings.rock.Mineral;
import java.util.EnumMap;
import java.util.Map;

/**
 * A heap of heavy mineral concentrate (docs/multiblocks/dry-mill.md): kg of each mineral, the gangue left out as the plant's concentrate
 * leaves it out. The filter's store, the dryer's feed bin and each separator's feed are one of these. Plain Java.
 */
public final class HeavyMinerals {
	/** Rounding slack: what is left under this many kg is gone. */
	private static final double SLACK = 1e-9;

	private final Map<Mineral, Double> kg = new EnumMap<>(Mineral.class);

	public double kg(Mineral mineral) {
		return kg.getOrDefault(mineral, 0.0);
	}

	public double total() {
		double sum = 0;
		for (double v : kg.values()) {
			sum += v;
		}
		return sum;
	}

	public boolean isEmpty() {
		return total() <= SLACK;
	}

	public void add(Mineral mineral, double amount) {
		if (amount > 0) {
			kg.merge(mineral, amount, Double::sum);
		}
	}

	public void add(HeavyMinerals other) {
		other.kg.forEach(this::add);
	}

	/** Takes out {@code amount} kg of {@code mineral}, or what there is. */
	public double remove(Mineral mineral, double amount) {
		double there = kg(mineral);
		double took = Math.min(there, Math.max(0, amount));
		if (there - took <= SLACK) {
			kg.remove(mineral);
		} else {
			kg.put(mineral, there - took);
		}
		return took;
	}

	/** Takes {@code amount} kg of the whole, each mineral in its share: what a feeder draws off a blended bin. */
	public HeavyMinerals take(double amount) {
		HeavyMinerals out = new HeavyMinerals();
		double total = total();
		if (total <= SLACK || amount <= 0) {
			return out;
		}
		double share = Math.min(1, amount / total);
		for (Mineral mineral : java.util.List.copyOf(kg.keySet())) {
			out.add(mineral, remove(mineral, share >= 1 ? kg(mineral) : kg(mineral) * share));
		}
		return out;
	}

	public Iterable<Mineral> minerals() {
		return java.util.List.copyOf(kg.keySet());
	}

	public Map<Mineral, Double> asMap() {
		return java.util.Collections.unmodifiableMap(kg);
	}

	public void clear() {
		kg.clear();
	}

	/** {@code ZIRCON=12.5,ILMENITE=3.0}. */
	public String save() {
		StringBuilder out = new StringBuilder();
		kg.forEach((m, v) -> out.append(out.length() == 0 ? "" : ",").append(m.name()).append('=').append(v));
		return out.toString();
	}

	public void load(String text) {
		kg.clear();
		if (text == null || text.isBlank()) {
			return;
		}
		for (String part : text.split(",")) {
			try {
				int eq = part.indexOf('=');
				add(Mineral.valueOf(part.substring(0, eq)), Double.parseDouble(part.substring(eq + 1)));
			} catch (RuntimeException ex) {
				// left out
			}
		}
	}

	/** {@code 56 kg zircon, 24 kg ilmenite}, richest first. */
	public String describe() {
		if (isEmpty()) {
			return "nothing";
		}
		StringBuilder out = new StringBuilder();
		kg.entrySet().stream().sorted(Map.Entry.<Mineral, Double>comparingByValue().reversed()).forEach(e -> out.append(out.length() == 0 ? "" : ", ")
				.append(String.format(java.util.Locale.ROOT, e.getValue() >= 10 ? "%.0f kg %s" : "%.2f kg %s", e.getValue(), e.getKey().name().toLowerCase(java.util.Locale.ROOT))));
		return out.toString();
	}
}
