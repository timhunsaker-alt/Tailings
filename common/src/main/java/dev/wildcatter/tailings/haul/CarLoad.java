package dev.wildcatter.tailings.haul;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** One Granby car: 3 t, any mix of rock, counted in kilograms. */
public final class CarLoad {
	private final Map<String, Integer> kilograms = new LinkedHashMap<>();

	public int total() {
		int sum = 0;
		for (int kg : kilograms.values()) {
			sum += kg;
		}
		return sum;
	}

	public int room() {
		return Haul.CAR_ITEMS - total();
	}

	/** Adds what fits and returns the kilograms that would not fit. */
	public int add(String itemId, int kilograms) {
		if (itemId == null || itemId.isEmpty() || kilograms <= 0) {
			return Math.max(0, kilograms);
		}
		int accepted = Math.min(kilograms, room());
		if (accepted > 0) {
			this.kilograms.merge(itemId, accepted, Integer::sum);
		}
		return kilograms - accepted;
	}

	public Map<String, Integer> contents() {
		return Collections.unmodifiableMap(kilograms);
	}

	public void clear() {
		kilograms.clear();
	}

	/** Takes up to {@code count} items out, the first loaded first, and returns them. */
	public Map<String, Integer> take(int count) {
		Map<String, Integer> out = new LinkedHashMap<>();
		var it = kilograms.entrySet().iterator();
		while (count > 0 && it.hasNext()) {
			Map.Entry<String, Integer> e = it.next();
			int n = Math.min(count, e.getValue());
			out.put(e.getKey(), n);
			count -= n;
			if (n == e.getValue()) {
				it.remove();
			} else {
				e.setValue(e.getValue() - n);
			}
		}
		return out;
	}

	public String save() {
		StringBuilder out = new StringBuilder();
		for (Map.Entry<String, Integer> entry : kilograms.entrySet()) {
			if (!out.isEmpty()) {
				out.append(',');
			}
			out.append(entry.getValue()).append('=').append(entry.getKey());
		}
		return out.toString();
	}

	public void load(String saved) {
		kilograms.clear();
		if (saved == null || saved.isEmpty()) {
			return;
		}
		for (String part : saved.split(",")) {
			int mark = part.indexOf('=');
			if (mark <= 0) {
				continue;
			}
			try {
				add(part.substring(mark + 1), Integer.parseInt(part.substring(0, mark)));
			} catch (NumberFormatException ignored) {
				// A corrupt entry is skipped.
			}
		}
	}
}
