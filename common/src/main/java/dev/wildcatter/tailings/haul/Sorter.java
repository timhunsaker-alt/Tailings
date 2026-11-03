package dev.wildcatter.tailings.haul;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * The dump pocket and the sensor sorter. The pocket holds one train. Each assigned product has a bin; anything else
 * is waste. A full bin or a full pocket refuses the tip, and the train waits.
 */
public final class Sorter {
	private final Set<String> products;
	private final Map<String, Integer> bins = new LinkedHashMap<>();
	private int waste;
	private int pocket;

	public Sorter(Set<String> products) {
		this.products = Set.copyOf(products);
	}

	public void restore(Map<String, Integer> bins, int waste, int pocket) {
		this.bins.clear();
		if (bins != null) {
			this.bins.putAll(bins);
		}
		this.waste = Math.max(0, waste);
		this.pocket = Math.max(0, pocket);
	}

	public int pocket() {
		return pocket;
	}

	public int waste() {
		return waste;
	}

	public int bin(String itemId) {
		return bins.getOrDefault(itemId, 0);
	}

	public Map<String, Integer> bins() {
		return Collections.unmodifiableMap(bins);
	}

	/** Tips one car, or the whole consist, if the pocket and the bins can take every kilogram. */
	public boolean tip(Map<String, Integer> load) {
		if (load == null || load.isEmpty()) {
			return true;
		}
		int sum = 0;
		for (int kg : load.values()) {
			if (kg > 0) {
				sum += kg;
			}
		}
		if (pocket + sum > Haul.POCKET_KG) {
			return false;
		}
		Map<String, Integer> nextBins = new LinkedHashMap<>(bins);
		int nextWaste = waste;
		for (Map.Entry<String, Integer> entry : load.entrySet()) {
			if (entry.getValue() <= 0) {
				continue;
			}
			if (products.contains(entry.getKey())) {
				int filled = nextBins.getOrDefault(entry.getKey(), 0) + entry.getValue();
				if (filled > Haul.BIN_KG) {
					return false;
				}
				nextBins.put(entry.getKey(), filled);
			} else {
				nextWaste += entry.getValue();
				if (nextWaste > Haul.BIN_KG) {
					return false;
				}
			}
		}
		bins.clear();
		bins.putAll(nextBins);
		waste = nextWaste;
		pocket += sum;
		return true;
	}

	/** The apron has cleared the pocket into the bins. The kilograms stay in the bins. */
	public void clearPocket() {
		pocket = 0;
	}
}
