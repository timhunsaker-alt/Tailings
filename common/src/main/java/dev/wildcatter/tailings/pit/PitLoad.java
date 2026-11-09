package dev.wildcatter.tailings.pit;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * What a haul truck's body or a loader's bucket holds, counted in kilograms of each item (docs/multiblocks/open-pit.md: real
 * tonnes, decided with Tim 2026-11-09). It fills by mass to its rating.
 */
public final class PitLoad {
	private final Map<String, Long> kilograms = new LinkedHashMap<>();
	private final long rating;

	public PitLoad(long ratingKg) {
		this.rating = ratingKg;
	}

	public long rating() {
		return rating;
	}

	public long total() {
		long sum = 0;
		for (long kg : kilograms.values()) {
			sum += kg;
		}
		return sum;
	}

	public long room() {
		return Math.max(0, rating - total());
	}

	public boolean empty() {
		return kilograms.isEmpty();
	}

	public long kg(String itemId) {
		return kilograms.getOrDefault(itemId, 0L);
	}

	/** Adds it all, even past the rating: the caller checks {@link #room()} first. */
	public void add(String itemId, long kg) {
		if (itemId != null && !itemId.isEmpty() && kg > 0) {
			kilograms.merge(itemId, kg, Long::sum);
		}
	}

	/** Moves what fits into {@code into}, item by item in order. */
	public void pourInto(PitLoad into) {
		var it = kilograms.entrySet().iterator();
		while (it.hasNext() && into.room() > 0) {
			Map.Entry<String, Long> e = it.next();
			long move = Math.min(e.getValue(), into.room());
			into.add(e.getKey(), move);
			if (move == e.getValue()) {
				it.remove();
			} else {
				e.setValue(e.getValue() - move);
			}
		}
	}

	/** Takes {@code kg} of {@code itemId} out. */
	public void take(String itemId, long kg) {
		Long have = kilograms.get(itemId);
		if (have == null) {
			return;
		}
		if (kg >= have) {
			kilograms.remove(itemId);
		} else {
			kilograms.put(itemId, have - kg);
		}
	}

	public boolean hasSoil() {
		for (String id : kilograms.keySet()) {
			if (PitMass.soil(id)) {
				return true;
			}
		}
		return false;
	}

	/** Whole blocks of each item it holds; what is left under a whole block stays. */
	public Map<String, Integer> wholeBlocks() {
		Map<String, Integer> out = new LinkedHashMap<>();
		kilograms.forEach((id, kg) -> {
			int n = (int) (kg / PitMass.kg(id));
			if (n > 0) {
				out.put(id, n);
			}
		});
		return out;
	}

	public Map<String, Long> contents() {
		return Collections.unmodifiableMap(kilograms);
	}

	public void clear() {
		kilograms.clear();
	}

	/** Tonnes, to one decimal, for readouts. */
	public String tonnes() {
		return String.format(java.util.Locale.ROOT, "%.1f t of %.0f t", total() / 1000.0, rating / 1000.0);
	}

	public String save() {
		StringBuilder out = new StringBuilder();
		kilograms.forEach((id, kg) -> out.append(out.isEmpty() ? "" : ",").append(id).append('=').append(kg));
		return out.toString();
	}

	public void load(String saved) {
		kilograms.clear();
		if (saved == null || saved.isEmpty()) {
			return;
		}
		for (String part : saved.split(",")) {
			int eq = part.lastIndexOf('=');
			if (eq > 0) {
				try {
					kilograms.put(part.substring(0, eq), Long.parseLong(part.substring(eq + 1)));
				} catch (NumberFormatException ex) {
					// left out
				}
			}
		}
	}
}
