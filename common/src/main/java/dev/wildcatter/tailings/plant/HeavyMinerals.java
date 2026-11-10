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
	/**
	 * What the separators pay for each mineral's kg, as kg won (l6-dry-mill.md #3): the scavenger spirals' share at the top rung's 1.11 a
	 * kg ({@link #DEFAULT}), the spiral bank's own share at 1.0 when the world's {@code dry_mill_all_hmc} rule sends it here too.
	 */
	private final Map<Mineral, Double> pay = new EnumMap<>(Mineral.class);

	/** What a kg of HMC pays by default: the scavenger spirals' share, at the top rung's rate. */
	public static double defaultRate(Mineral mineral) {
		return FullRecoveryRates.topRungPay(mineral);
	}

	public double kg(Mineral mineral) {
		return kg.getOrDefault(mineral, 0.0);
	}

	/** The kg a separator pays for all of {@code mineral} here. */
	public double pay(Mineral mineral) {
		return pay.getOrDefault(mineral, 0.0);
	}

	/** What a kg of {@code mineral} here pays, on average. */
	public double rate(Mineral mineral) {
		double there = kg(mineral);
		return there > SLACK ? pay(mineral) / there : defaultRate(mineral);
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

	/** Adds {@code amount} kg of the scavenger spirals' HMC, paid at the default rate. */
	public void add(Mineral mineral, double amount) {
		add(mineral, amount, amount * defaultRate(mineral));
	}

	/** Adds {@code amount} kg that the separators will pay as {@code payKg} kg won. */
	public void add(Mineral mineral, double amount, double payKg) {
		if (amount > 0) {
			kg.merge(mineral, amount, Double::sum);
			pay.merge(mineral, Math.max(0, payKg), Double::sum);
		}
	}

	public void add(HeavyMinerals other) {
		other.kg.forEach((mineral, amount) -> add(mineral, amount, other.pay(mineral)));
	}

	/** Takes out {@code amount} kg of {@code mineral}, or what there is, with its share of the pay. */
	public double remove(Mineral mineral, double amount) {
		return removeWithPay(mineral, amount)[0];
	}

	/** As {@link #remove}: the kg taken and the pay it carries. */
	private double[] removeWithPay(Mineral mineral, double amount) {
		double there = kg(mineral);
		double took = Math.min(there, Math.max(0, amount));
		double paid = there > 0 ? pay(mineral) * took / there : 0;
		if (there - took <= SLACK) {
			kg.remove(mineral);
			paid = pay(mineral);
			pay.remove(mineral);
		} else {
			kg.put(mineral, there - took);
			pay.put(mineral, pay(mineral) - paid);
		}
		return new double[] {took, paid};
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
			double[] got = removeWithPay(mineral, share >= 1 ? kg(mineral) : kg(mineral) * share);
			out.add(mineral, got[0], got[1]);
		}
		return out;
	}

	/** Moves all of {@code mineral} from {@code from} into this, with its pay. */
	public void addAll(HeavyMinerals from, Mineral mineral) {
		add(mineral, from.kg(mineral), from.pay(mineral));
	}

	public Iterable<Mineral> minerals() {
		return java.util.List.copyOf(kg.keySet());
	}

	public Map<Mineral, Double> asMap() {
		return java.util.Collections.unmodifiableMap(kg);
	}

	public void clear() {
		kg.clear();
		pay.clear();
	}

	/** True when some mineral here pays other than the default rate (the spiral bank's share is in it). */
	public boolean mixedRates() {
		for (Mineral mineral : kg.keySet()) {
			if (Math.abs(rate(mineral) - defaultRate(mineral)) > 1e-6) {
				return true;
			}
		}
		return false;
	}

	/** {@code ZIRCON=12.5,ILMENITE=3.0}; a mineral not at the default rate carries its pay, {@code ZIRCON=12.5@13.0}. */
	public String save() {
		StringBuilder out = new StringBuilder();
		kg.forEach((m, v) -> {
			out.append(out.length() == 0 ? "" : ",").append(m.name()).append('=').append(v);
			if (Math.abs(rate(m) - defaultRate(m)) > 1e-9) {
				out.append('@').append(pay(m));
			}
		});
		return out.toString();
	}

	public void load(String text) {
		clear();
		if (text == null || text.isBlank()) {
			return;
		}
		for (String part : text.split(",")) {
			try {
				int eq = part.indexOf('=');
				int at = part.indexOf('@');
				Mineral mineral = Mineral.valueOf(part.substring(0, eq));
				if (at < 0) {
					add(mineral, Double.parseDouble(part.substring(eq + 1)));
				} else {
					add(mineral, Double.parseDouble(part.substring(eq + 1, at)), Double.parseDouble(part.substring(at + 1)));
				}
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
