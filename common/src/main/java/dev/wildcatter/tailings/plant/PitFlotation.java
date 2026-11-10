package dev.wildcatter.tailings.plant;

import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.recovery.RecoveryRoute;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.HashMap;
import java.util.Map;

/**
 * The pit line's flotation (docs/multiblocks/pit-flotation.md, P7c; decisions in docs/questions/p7-pit-line.md), plain Java. The row of
 * tank cells floats the same 75 % of every floating mineral as the small bank (the flotation rung), split over its eight cells, most in
 * the first. A mineral that has passed cells travels in the tails under a marked name, {@code tailings:rowN/<id>} after N cells, so the
 * next cell floats its own share of what reaches it and no more, however the row's pipes are joined; a ninth cell floats nothing. The
 * concentrate carries the plain names, as the bank's does, so the existing thickener and filter pay it by their unchanged rule.
 */
public final class PitFlotation {
	private PitFlotation() {
	}

	/** Each cell's share of the block's floating mineral, % (proposed): five roughers, three scavengers. They add to the rung's 75 %. */
	public static final int[] SHARES_PERCENT = {35, 15, 8, 5, 4, 3, 3, 2};
	public static final int CELLS = SHARES_PERCENT.length;
	private static final String ROW = "tailings:row";
	private static final String SPHALERITE = Mineral.SPHALERITE.mineralItemId();
	private static final String GALENA = Mineral.GALENA.mineralItemId();

	/** The rougher concentrate leaves the cells' launders at 20 % solids (proposed); the column cleaners take it to the bank's 30 %. */
	public static final double ROUGHER_SOLIDS = 0.20;
	public static final double CLEANER_SOLIDS = 0.30;

	/** The row and the tank it is fed from, rated 20 % over the line's 1,000 t/h of fines slurry at 35 %, wet kg a tick (as the cyclones). */
	public static final double ROW_KG_PER_TICK = PitLine.kgPerTick(PitLine.TONNES_PER_HOUR) / PitLine.OVERFLOW_SOLIDS * 1.2;
	/** Each part's inlet and outlet hold 10 s of that, as the small plant's parts hold 10 s of theirs. */
	public static final double ROW_HOLD = PitLine.SLURRY_HOLD_SECONDS * 20 * ROW_KG_PER_TICK;
	/** One column cell cleans up to 30 t/h of concentrate solids (proposed: about 2.4 t/h a m² of its 12.6 m² of froth). */
	public static final double COLUMN_SOLIDS_TPH = 30;
	public static final double COLUMN_KG_PER_TICK = PitLine.kgPerTick(COLUMN_SOLIDS_TPH) / ROUGHER_SOLIDS;
	public static final double COLUMN_HOLD = PitLine.SLURRY_HOLD_SECONDS * 20 * COLUMN_KG_PER_TICK;
	/** One large blower's air serves ten cells on its air line, the row's eight and two columns (proposed). */
	public static final int CELLS_PER_BLOWER = 10;

	/** The large thickener: the line's tails, rated as the row; its underflow at 60 % solids, 10 s of it held for the line out. */
	public static final double THICKENER_KG_PER_TICK = ROW_KG_PER_TICK;
	public static final double THICKENER_UNDERFLOW_HOLD = PitLine.SLURRY_HOLD_SECONDS * 20 * PitLine.kgPerTick(PitLine.TONNES_PER_HOUR) / 0.60;

	/** The rung the row pays: the flotation route's 75 %. */
	public static double rowRecovery() {
		int sum = 0;
		for (int share : SHARES_PERCENT) {
			sum += share;
		}
		return sum / 100.0;
	}

	/** The share of the block's floating mineral the first {@code cells} cells of a row float, 0 to 0.75. */
	public static double floatedBy(int cells) {
		int sum = 0;
		for (int i = 0; i < Math.min(cells, CELLS); i++) {
			sum += SHARES_PERCENT[i];
		}
		return sum / 100.0;
	}

	/** What a cell floats of the floating mineral that reaches it having passed {@code passed} cells: its share over what is left. */
	public static double cellFraction(int passed) {
		if (passed < 0 || passed >= CELLS) {
			return 0;
		}
		return SHARES_PERCENT[passed] / 100.0 / (1 - floatedBy(passed));
	}

	/** The name a mineral travels under after {@code passed} cells. */
	public static String marked(int passed, String id) {
		return passed <= 0 ? id : ROW + passed + "/" + id;
	}

	/** How many cells the mineral named {@code id} has passed (0 for a plain name). */
	public static int passed(String id) {
		if (!id.startsWith(ROW)) {
			return 0;
		}
		int slash = id.indexOf('/');
		try {
			return slash < 0 ? 0 : Integer.parseInt(id.substring(ROW.length(), slash));
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	/** The mineral's own name, the mark taken off. */
	public static String plain(String id) {
		return passed(id) > 0 ? id.substring(id.indexOf('/') + 1) : id;
	}

	/** An assay with the row's marks taken off: what leaves the row for the thickener and the lined cell. */
	public static SlurryAssay plain(SlurryAssay assay) {
		Map<String, Double> out = new HashMap<>();
		for (String id : assay.minerals()) {
			out.merge(plain(id), assay.get(id), Double::sum);
		}
		return SlurryAssay.of(out);
	}

	/**
	 * One tank cell's split of {@code kg} of its feed: each floating mineral's share for this cell to a rougher concentrate at 20 % solids,
	 * the rest on in the tails marked as having passed one more cell. As the bank: sphalerite floated from feed that also holds galena is
	 * a bulk cake's and is lost, and the bank's zinc concentrate keeps its own name.
	 */
	public static Selective.Split cell(SlurryAssay feed, double kg) {
		boolean bulk = false, galena = false, sphalerite = false;
		for (String id : feed.minerals()) {
			galena |= plain(id).equals(GALENA);
			sphalerite |= plain(id).equals(SPHALERITE);
		}
		bulk = galena && sphalerite;
		Map<String, Double> conc = new HashMap<>();
		Map<String, Double> tail = new HashMap<>();
		double solids = 0;
		for (String id : feed.minerals()) {
			double there = feed.get(id) * kg;
			int passed = passed(id);
			String base = plain(id);
			Mineral mineral = Assays.mineral(base);
			boolean floats = mineral != null && mineral.floats() && passed < CELLS;
			double got = floats ? there * cellFraction(passed) : 0;
			tail.merge(floats ? marked(passed + 1, base) : id, there - got, Double::sum);
			if (got > 0 && !(bulk && base.equals(SPHALERITE))) {
				conc.merge(base.equals(SPHALERITE) ? Selective.ZINC_CONCENTRATE : base, got, Double::sum);
				solids += got;
			}
		}
		double concKg = Math.min(kg, solids / ROUGHER_SOLIDS);
		double tailKg = kg - concKg;
		conc.replaceAll((id, m) -> concKg > 0 ? m / concKg : 0);
		tail.replaceAll((id, m) -> tailKg > 0 ? m / tailKg : 0);
		return new Selective.Split(concKg, SlurryAssay.of(conc), tailKg, SlurryAssay.of(tail));
	}

	/** The solids of a concentrate slurry: its minerals are all of its solids. */
	public static double concentrateSolids(SlurryAssay concentrate) {
		double sum = 0;
		for (String id : concentrate.minerals()) {
			sum += concentrate.get(id);
		}
		return sum;
	}

	/**
	 * A column cleaner's split of {@code kg} of concentrate slurry: all its mineral to the cleaned concentrate at the bank's 30 % (the
	 * game pays at the filter, so the cleaners are a step without a rung of their own); its tails are the water it gave up, which carries
	 * no mineral back to cell 1. Concentrate already at 30 % or more passes as it is.
	 */
	public static Selective.Split column(SlurryAssay concentrate, double kg) {
		double solids = concentrateSolids(concentrate);
		if (solids >= CLEANER_SOLIDS - 1e-9 || solids <= 0) {
			return new Selective.Split(kg, concentrate, 0, SlurryAssay.EMPTY);
		}
		double cleanKg = kg * solids / CLEANER_SOLIDS;
		double ratio = kg / cleanKg;
		Map<String, Double> out = new HashMap<>();
		for (String id : concentrate.minerals()) {
			out.put(id, concentrate.get(id) * ratio);
		}
		return new Selective.Split(cleanKg, SlurryAssay.of(out), kg - cleanKg, SlurryAssay.EMPTY);
	}

	/** The flotation route's rung, which the row's shares add up to. */
	public static double rung() {
		return RecoveryRoute.FLOTATION.fraction().doubleValue();
	}
}
