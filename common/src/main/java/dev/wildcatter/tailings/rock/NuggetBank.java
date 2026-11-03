package dev.wildcatter.tailings.rock;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

/** Running recovered mineral that has not yet made a whole nugget. */
public final class NuggetBank {
	private final Map<Mineral, BigDecimal> held = new EnumMap<>(Mineral.class);

	public BigDecimal held(Mineral mineral) {
		return held.getOrDefault(mineral, BigDecimal.ZERO);
	}

	/** Adds recovered mineral and returns how many nuggets that completes. */
	public int deposit(Mineral mineral, BigDecimal recovered) {
		if (recovered.signum() < 0) {
			throw new IllegalArgumentException("recovered mineral cannot be negative");
		}
		BigDecimal nugget = mineral.nuggetAmount();
		BigDecimal total = held(mineral).add(recovered);
		int count = 0;
		while (total.compareTo(nugget) >= 0) {
			total = total.subtract(nugget);
			count++;
		}
		if (total.signum() == 0) {
			held.remove(mineral);
		} else {
			held.put(mineral, total);
		}
		return count;
	}

	public String save() {
		StringBuilder out = new StringBuilder();
		for (Map.Entry<Mineral, BigDecimal> entry : held.entrySet()) {
			if (out.length() > 0) {
				out.append(',');
			}
			out.append(entry.getKey().name()).append('=').append(entry.getValue().toPlainString());
		}
		return out.toString();
	}

	public void load(String text) {
		held.clear();
		if (text == null || text.isBlank()) {
			return;
		}
		for (String part : text.split(",")) {
			int cut = part.indexOf('=');
			held.put(Mineral.valueOf(part.substring(0, cut)), new BigDecimal(part.substring(cut + 1)));
		}
	}
}
