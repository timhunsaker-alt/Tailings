package dev.wildcatter.tailings.plant;

import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import java.util.HashMap;
import java.util.Map;

/**
 * What a rock's slurry carries, for Wildcatter's assayed slurry: the kg of each of the rock's minerals in a kg of it, keyed by the
 * mineral's item id. The crushing circuit liberates and recovers nothing, so the assay is the whole mineral the rock holds
 * ({@link Rock#contained} over a 2.7 t block). Plain Java.
 */
public final class Assays {
	private Assays() {
	}

	/** The kg of each mineral in a kg of {@code rock}, dry. */
	public static SlurryAssay dry(Rock rock) {
		Map<String, Double> shares = new HashMap<>();
		for (Mineral mineral : rock.minerals()) {
			shares.merge(mineral.mineralItemId(), rock.contained(mineral).doubleValue() / PlantRates.BLOCK_KG, Double::sum);
		}
		return SlurryAssay.of(shares);
	}

	/** {@code kg} of each rock in {@code load} blended, dry. */
	public static SlurryAssay dry(RockLoad load) {
		SlurryAssay[] out = {SlurryAssay.EMPTY};
		double[] held = {0};
		load.forEach((rock, size, kg) -> {
			out[0] = SlurryAssay.blend(out[0], held[0], dry(rock), kg);
			held[0] += kg;
		});
		return out[0];
	}

	/** A dry assay diluted to slurry at {@code solids} (by mass). */
	public static SlurryAssay wet(SlurryAssay dry, double solids) {
		Map<String, Double> shares = new HashMap<>();
		for (String m : dry.minerals()) {
			shares.put(m, dry.get(m) * solids);
		}
		return SlurryAssay.of(shares);
	}

	/** The mineral, by its item id, or null for one Tailings does not know. */
	public static Mineral mineral(String itemId) {
		for (Mineral mineral : Mineral.values()) {
			if (mineral.mineralItemId().equals(itemId)) {
				return mineral;
			}
		}
		return null;
	}
}
