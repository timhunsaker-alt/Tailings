package dev.wildcatter.tailings.plant;

import dev.wildcatter.tailings.rock.Mineral;
import java.util.List;
import java.util.Map;

/**
 * Phase 4c (docs/multiblocks/full-recovery.md): what a concentrate's mineral carries. Grades are real concentrates' scaled by ten, as
 * Wildcatter's ores.md scales its grades, except monazite, which is itself the rare earth ore. (All proposed.) Plain Java.
 */
public final class ByProducts {
	private ByProducts() {
	}

	/** One by-product: its item, and the kg of it a kg of the mineral carries. */
	public record Share(String itemId, double grade) {
	}

	/** One by-product item is this many kg (proposed, the size of a Wildcatter mineral's game item). */
	public static final double ITEM_KG = 10;

	private static final Map<Mineral, List<Share>> TABLE = Map.of(
			Mineral.SPHALERITE, List.of(new Share("tailings:cadmium", 0.03), new Share("tailings:indium", 0.001)),
			Mineral.GALENA, List.of(new Share("wildcatter:silver_ingot", 0.05)),
			Mineral.MOLYBDENITE, List.of(new Share("tailings:rhenium", 0.005)),
			Mineral.PENTLANDITE, List.of(new Share("tailings:cobalt", 0.30)),
			Mineral.MONAZITE, List.of(new Share("tailings:rare_earth_oxide", 0.60)));

	public static List<Share> of(Mineral mineral) {
		return TABLE.getOrDefault(mineral, List.of());
	}

	/** The mineral whose paid item this is, or null if it carries nothing. */
	public static Mineral byItem(String itemId) {
		for (Mineral mineral : TABLE.keySet()) {
			if (mineral.phaseOneProductId().equals(itemId)) {
				return mineral;
			}
		}
		return null;
	}
}
