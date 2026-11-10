package dev.wildcatter.tailings.plant;

import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Selective lead-zinc flotation (docs/multiblocks/selective-flotation.md), plain Java. A slurry's assay is keyed by strings, so the
 * sphalerite a selective tank holds down travels as its own entry, and what the first bank left of everything else travels marked
 * "spent" past the second bank, so that no block pays over its rung.
 */
public final class Selective {
	/** Sphalerite held down by zinc sulfate: it does not float until an activation tank frees it. */
	public static final String DEPRESSED = "tailings:depressed_sphalerite";
	/**
	 * Sphalerite a bank floated as a zinc concentrate. It is kept apart from the scavengers' sphalerite, so that a thickener or filter
	 * that blends it with lead concentrate makes a bulk cake that loses it, as a bank fed both does (plant-gaps decision 6).
	 */
	public static final String ZINC_CONCENTRATE = "tailings:floated_sphalerite";
	private static final String SPENT = "tailings:spent/";
	private static final String SPHALERITE = Mineral.SPHALERITE.mineralItemId();
	private static final String GALENA = Mineral.GALENA.mineralItemId();

	private Selective() {
	}

	/** What {@code kg} of conditioned slurry makes in a bank. */
	public record Split(double concentrate, SlurryAssay concentrateAssay, double tails, SlurryAssay tailsAssay) {
	}

	/** The conditioning tank's dose: zinc sulfate in place of copper sulfate when it runs selectively, either only with sphalerite in the feed. */
	public static Set<Reagent> conditioning(boolean selective, boolean sphalerite) {
		Set<Reagent> dose = EnumSet.of(Reagent.XANTHATE, Reagent.MIBC, Reagent.LIME);
		if (sphalerite) {
			dose.add(selective ? Reagent.ZINC_SULFATE : Reagent.COPPER_SULFATE);
		}
		return dose;
	}

	/** The activation tank's dose: copper sulfate only when there is held-down sphalerite to free. */
	public static Set<Reagent> activating(boolean depressed) {
		Set<Reagent> dose = EnumSet.of(Reagent.XANTHATE, Reagent.MIBC, Reagent.LIME);
		if (depressed) {
			dose.add(Reagent.COPPER_SULFATE);
		}
		return dose;
	}

	/** A selective tank's product: its sphalerite held down. */
	public static SlurryAssay depress(SlurryAssay feed) {
		Map<String, Double> out = new HashMap<>();
		for (String id : feed.minerals()) {
			out.merge(id.equals(SPHALERITE) ? DEPRESSED : id, feed.get(id), Double::sum);
		}
		return SlurryAssay.of(out);
	}

	/** An activation tank's product: the sphalerite freed, and what the first bank left of everything else marked as spent. */
	public static SlurryAssay activate(SlurryAssay tails) {
		Map<String, Double> out = new HashMap<>();
		for (String id : tails.minerals()) {
			Mineral mineral = Assays.mineral(id);
			String to = id.equals(DEPRESSED) ? SPHALERITE : mineral != null && mineral.floats() && mineral != Mineral.SPHALERITE ? SPENT + id : id;
			out.merge(to, tails.get(id), Double::sum);
		}
		return SlurryAssay.of(out);
	}

	/**
	 * A bank's split: each floating mineral's recovered share to concentrate, the rest of the feed to tails, spent minerals going back to
	 * their own names for the scavengers. Feed holding galena and sphalerite together makes a bulk cake, which smelts to lead with its
	 * zinc lost to slag (Tim, 2026-11-06), so the sphalerite the bank floats is gone and only its tails carry on.
	 */
	public static Split bank(SlurryAssay feed, double kg, double recovery, double concentrateSolids) {
		boolean bulk = feed.get(GALENA) > 0 && feed.get(SPHALERITE) > 0;
		Map<String, Double> conc = new HashMap<>();
		Map<String, Double> tail = new HashMap<>();
		double solids = 0;
		for (String id : feed.minerals()) {
			double there = feed.get(id) * kg;
			Mineral mineral = Assays.mineral(id);
			double got = mineral != null && mineral.floats() ? there * recovery : 0;
			tail.merge(id.startsWith(SPENT) ? id.substring(SPENT.length()) : id, there - got, Double::sum);
			if (!(bulk && id.equals(SPHALERITE))) {
				conc.merge(id.equals(SPHALERITE) ? ZINC_CONCENTRATE : id, got, Double::sum);
				solids += got;
			}
		}
		double concKg = Math.min(kg, solids / concentrateSolids);
		double tailKg = kg - concKg;
		conc.replaceAll((id, m) -> concKg > 0 ? m / concKg : 0);
		tail.replaceAll((id, m) -> tailKg > 0 ? m / tailKg : 0);
		return new Split(concKg, SlurryAssay.of(conc), tailKg, SlurryAssay.of(tail));
	}

	/**
	 * What a filter's cake is paid from (selective-flotation.md, plant-gaps decision 6): a bank's zinc concentrate counts as sphalerite,
	 * unless the concentrate also holds galena. Then lead and zinc concentrate were piped into one thickener or filter, and the bulk cake
	 * smelts to lead with the bank's zinc lost to slag, as on a bank fed both. The scavengers' sphalerite is not a bank's and pays as before.
	 */
	public static SlurryAssay cake(SlurryAssay concentrate) {
		if (concentrate.get(ZINC_CONCENTRATE) <= 0) {
			return concentrate;
		}
		boolean bulk = concentrate.get(GALENA) > 0;
		Map<String, Double> out = new HashMap<>();
		for (String id : concentrate.minerals()) {
			if (!id.equals(ZINC_CONCENTRATE)) {
				out.merge(id, concentrate.get(id), Double::sum);
			} else if (!bulk) {
				out.merge(SPHALERITE, concentrate.get(id), Double::sum);
			}
		}
		return SlurryAssay.of(out);
	}

	/** True when a concentrate blends a bank's zinc concentrate with galena: the cake is bulk and the bank's zinc is lost. */
	public static boolean mixed(SlurryAssay concentrate) {
		return concentrate.get(ZINC_CONCENTRATE) > 0 && concentrate.get(GALENA) > 0;
	}
}
