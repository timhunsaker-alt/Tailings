package dev.wildcatter.tailings.plant;

import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.BiPredicate;
import org.jspecify.annotations.Nullable;

/**
 * Dry rock in kg, by rock and size class (docs/multiblocks/crushing-circuit.md: "the dry stages carry kg of each rock and size
 * class"). What a belt run or a dry part holds. A null size is unbroken run-of-mine rock. Plain Java.
 */
public final class RockLoad {
	private final Map<Rock, double[]> kg = new EnumMap<>(Rock.class);

	private static int slot(@Nullable SizeClass size) {
		return size == null ? 0 : size.ordinal() + 1;
	}

	private static @Nullable SizeClass size(int slot) {
		return slot == 0 ? null : SizeClass.values()[slot - 1];
	}

	public double kg(Rock rock, @Nullable SizeClass size) {
		double[] at = kg.get(rock);
		return at == null ? 0 : at[slot(size)];
	}

	public double total() {
		double sum = 0;
		for (double[] at : kg.values()) {
			for (double v : at) {
				sum += v;
			}
		}
		return sum;
	}

	public boolean isEmpty() {
		return total() < 1e-6;
	}

	public void add(Rock rock, @Nullable SizeClass size, double amount) {
		if (amount <= 0) {
			return;
		}
		kg.computeIfAbsent(rock, r -> new double[SizeClass.values().length + 1])[slot(size)] += amount;
	}

	public void add(RockLoad other) {
		other.forEach((rock, size, amount) -> add(rock, size, amount));
	}

	/** Each rock and size it holds, with its kg. */
	public interface Visitor {
		void visit(Rock rock, @Nullable SizeClass size, double kg);
	}

	public void forEach(Visitor visitor) {
		for (Map.Entry<Rock, double[]> e : kg.entrySet()) {
			for (int i = 0; i < e.getValue().length; i++) {
				if (e.getValue()[i] > 1e-9) {
					visitor.visit(e.getKey(), size(i), e.getValue()[i]);
				}
			}
		}
	}

	/** The kg of what {@code which} accepts. */
	public double total(BiPredicate<Rock, @Nullable SizeClass> which) {
		double[] sum = {0};
		forEach((rock, size, amount) -> {
			if (which.test(rock, size)) {
				sum[0] += amount;
			}
		});
		return sum[0];
	}

	/** Takes up to {@code max} kg of what {@code which} accepts, in proportion to what is held. */
	public RockLoad take(double max, BiPredicate<Rock, @Nullable SizeClass> which) {
		RockLoad out = new RockLoad();
		double there = total(which);
		if (there <= 0 || max <= 0) {
			return out;
		}
		double share = Math.min(1, max / there);
		for (Map.Entry<Rock, double[]> e : kg.entrySet()) {
			for (int i = 0; i < e.getValue().length; i++) {
				double v = e.getValue()[i];
				if (v > 0 && which.test(e.getKey(), size(i))) {
					double t = v * share;
					e.getValue()[i] = v - t < 1e-9 ? 0 : v - t;
					out.add(e.getKey(), size(i), t);
				}
			}
		}
		kg.values().removeIf(a -> {
			for (double v : a) {
				if (v > 0) {
					return false;
				}
			}
			return true;
		});
		return out;
	}

	public RockLoad take(double max) {
		return take(max, (rock, size) -> true);
	}

	/** The rock most of the load is, or null when empty: what a belt draws. */
	public @Nullable Rock main() {
		Rock best = null;
		double most = 0;
		for (Map.Entry<Rock, double[]> e : kg.entrySet()) {
			double sum = 0;
			for (double v : e.getValue()) {
				sum += v;
			}
			if (sum > most) {
				most = sum;
				best = e.getKey();
			}
		}
		return best;
	}

	public void clear() {
		kg.clear();
	}

	/** Saved as {@code ROCK:slot=kg,...}. */
	public String save() {
		StringBuilder out = new StringBuilder();
		forEach((rock, size, amount) -> out.append(out.length() == 0 ? "" : ",").append(rock.name()).append(':').append(slot(size))
				.append('=').append(amount));
		return out.toString();
	}

	public void load(String text) {
		kg.clear();
		if (text == null || text.isBlank()) {
			return;
		}
		for (String part : text.split(",")) {
			try {
				int colon = part.indexOf(':'), eq = part.indexOf('=');
				Rock rock = Rock.valueOf(part.substring(0, colon));
				int slot = Integer.parseInt(part.substring(colon + 1, eq));
				add(rock, size(slot), Double.parseDouble(part.substring(eq + 1)));
			} catch (RuntimeException ex) {
				// left out
			}
		}
	}
}
