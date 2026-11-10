package dev.wildcatter.tailings.paste;

/**
 * The paste fill figures (docs/multiblocks/paste-backfill.md, L5): the paste, its binder, the filter, mixer and piston pump, the fill
 * line's pressure and curing. Plain Java, unit-tested in {@code PasteFillTest}.
 *
 * <p>Real cemented paste fill is thickened and filtered tails at 70-85 % solids with 3-7 % binder, pumped through steel pipe into a
 * mined-out void behind bulkheads, where it sets to a weak rock of about 0.5-1 MPa at 28 days.
 */
public final class PasteFill {
	/** The paste's solids by mass (proposed; real 70-85 %). */
	public static final double PASTE_SOLIDS = 0.78;
	/** A cubic metre of paste weighs 2.0 t (proposed): one placed block. */
	public static final double PASTE_KG_PER_M3 = 2_000;
	/** Binder by mass of the tails' dry solids: 5 % cement (proposed; real 3-7 %). */
	public static final double BINDER = 0.05;
	/** One `cement` item is a 50 kg bag (proposed; real bags are 25 to 50 kg). At 5 % that is a bag a tonne of tails solids. */
	public static final double CEMENT_BAG_KG = 50;
	/** The mixer's cement hopper holds 40 bags, 2 t (proposed). */
	public static final double CEMENT_HOLD_KG = 40 * CEMENT_BAG_KG;
	/** A vacuum disc filter's cake: about 80 % solids (proposed; real 75-85 %). */
	public static final double CAKE_SOLIDS = 0.80;
	/** Thickened tails, as the tailings thickener puts them on the pipe (open-pit-p4-dry-stack.md): 60 % solids. */
	public static final double UNDERFLOW_SOLIDS = 0.60;
	/** The paste carries its binder in its assay, as thickened tails carry their solids: Wildcatter 0.5.1 has no product for paste. */
	public static final String BINDER_KEY = "tailings:binder";

	/** The disc filter is sized for the plant's tails: 50 t/h of solids (proposed). */
	public static final double FILTER_KG_PER_HOUR = 50_000;
	/** The filter's cake waits for the mixer in its discharge chute: up to 5 t of solids (proposed). */
	public static final double CAKE_HOLD_KG = 5_000;
	/** The piston pump: 35 m3/h at 8 MPa (proposed). The mixer is sized to the pump. */
	public static final double PUMP_M3_PER_HOUR = 35;
	public static final double PUMP_MAX_PA = 8_000_000;
	/** The pump's hydraulic power pack at 70 % from flow to motor (proposed). */
	public static final double PUMP_EFFICIENCY = 0.70;
	/** Its motor: 35 m3/h x 8 MPa / 0.7 is 111 kW, the next standard frame down being 110 kW. */
	public static final int PUMP_WATTS = 110_000;
	/** Wall friction in the fill line at the pump's rate: 8 kPa a metre (proposed: so 8 MPa is reached at 1 km, as the spec has it). */
	public static final double FRICTION_PA_PER_M = 8_000;
	/** The weight of the paste in a vertical line: 2.0 t/m3 x 9.81, 19.6 kPa a metre of fall. */
	public static final double HEAD_PA_PER_M = PASTE_KG_PER_M3 * 9.81;
	/** The longest line the pump will trace: a little past where it reads LINE PRESSURE on the flat. */
	public static final int MOST_LINE_PIPES = 1_500;

	/**
	 * The fill's game pace (Tim, 2026-11-10, l5-paste.md #19): each placed block of paste fill stands for this many cubic metres of
	 * the paste the line delivers, so a void fills ten times as fast as the real 34 m3 an hour: a 360 m3 drive branch in about an
	 * hour of play, not eleven. Everything up the line (the filter, the mixer, the pump, the cement) runs at its real rate; the cure
	 * stays one game day.
	 */
	public static final int FILL_SCALE = 10;

	/** The largest void an outlet fills, cells (proposed, as the lined cell's basin search). */
	public static final int MOST_CELLS = 20_000;
	/** Wet paste sets in one game day, 20 minutes (scaled from 28 days to design strength), in this many steps. */
	public static final int CURE_TICKS = 24_000;
	public static final int CURE_STEPS = 8;

	private PasteFill() {
	}

	/** Kg of tails solids in a m3 of paste: 2.0 t at 78 % solids, less the binder's share (about 1.49 t). */
	public static double tailsSolidsPerM3() {
		return PASTE_KG_PER_M3 * PASTE_SOLIDS / (1 + BINDER);
	}

	/** Kg of cement in a m3 of paste (about 74 kg: a bag and a half). */
	public static double cementPerM3() {
		return tailsSolidsPerM3() * BINDER;
	}

	/** Water a kg of solids carries at {@code solids} by mass. */
	public static double waterPerKgSolids(double solids) {
		return (1 - solids) / solids;
	}

	/** The disc filter's filtrate, litres a kg of solids, from thickened tails to cake: about 0.42. */
	public static double filtratePerKgSolids() {
		return waterPerKgSolids(UNDERFLOW_SOLIDS) - waterPerKgSolids(CAKE_SOLIDS);
	}

	/**
	 * Kg of thickened tails the mixer adds to a kg of cake solids to bring the paste, cement included, down to 78 % solids. With cake
	 * solids c, trim t and binder 5 % of the tails solids, (1 + b)(c + u t) is 78 % of that plus the water w c + (1 - u) t, where w is
	 * the cake's water a kg of solids and u the trim's solids: about 0.21 kg.
	 */
	public static double trimPerKgCake() {
		double s = PASTE_SOLIDS, b = 1 + BINDER, u = UNDERFLOW_SOLIDS, w = waterPerKgSolids(CAKE_SOLIDS);
		// b c + b u t = s (b c + b u t + w c + (1 - u) t)  =>  t (b u (1 - s) - s (1 - u)) = c (s w - b (1 - s))
		return (s * w - b * (1 - s)) / (b * u * (1 - s) - s * (1 - u));
	}

	/** The solids of a paste mixed from {@code cakeSolids} kg of cake, {@code trimKg} of thickened tails and its binder. */
	public static double mixedSolids(double cakeSolids, double trimKg) {
		double tails = cakeSolids + trimKg * UNDERFLOW_SOLIDS;
		double water = cakeSolids * waterPerKgSolids(CAKE_SOLIDS) + trimKg * (1 - UNDERFLOW_SOLIDS);
		double solids = tails * (1 + BINDER);
		return solids / (solids + water);
	}

	/** Kg of cake solids that make {@code pasteKg} of paste. */
	public static double cakeFor(double pasteKg) {
		double tails = pasteKg * PASTE_SOLIDS / (1 + BINDER);
		return tails / (1 + UNDERFLOW_SOLIDS * trimPerKgCake());
	}

	/** Kg of delivered paste that makes one placed block of paste fill: a cubic metre's 2.0 t over {@link #FILL_SCALE}, 200 kg. */
	public static double kgPerBlock() {
		return PASTE_KG_PER_M3 / FILL_SCALE;
	}

	/** Hours for the pump at its rating to fill {@code m3} cubic metres of void (placed blocks). */
	public static double hoursToFill(double m3) {
		return m3 * kgPerBlock() / (PUMP_M3_PER_HOUR * PASTE_KG_PER_M3);
	}

	/** The pump's full rate, kg of paste a tick. */
	public static double pumpKgPerTick() {
		return PUMP_M3_PER_HOUR * PASTE_KG_PER_M3 / 72_000;
	}

	/** The filter's full rate, kg of solids a tick. */
	public static double filterKgPerTick() {
		return FILTER_KG_PER_HOUR / 72_000;
	}

	/** The pump's hydraulic power at its rating, W, over its pack's efficiency: what its motor must give. */
	public static double pumpShaftWatts() {
		return PUMP_M3_PER_HOUR / 3600 * PUMP_MAX_PA / PUMP_EFFICIENCY;
	}

	/**
	 * The pump's discharge pressure, Pa, to push paste through {@code pipes} metres of line to an outlet {@code drop} metres below it
	 * (negative: above): friction along the line less the paste's weight in the fall. A line that falls far enough runs on its own
	 * weight (a gravity-fed fill line), so it never reads below 0.
	 */
	public static double linePressure(int pipes, double drop) {
		return Math.max(0, pipes * FRICTION_PA_PER_M - drop * HEAD_PA_PER_M);
	}

	/** True if the pump can push through this line: no more than its 8 MPa. */
	public static boolean pumpable(int pipes, double drop) {
		return linePressure(pipes, drop) <= PUMP_MAX_PA;
	}

	/** Ticks between curing steps: a step every 2.5 minutes, the eighth sets the paste. */
	public static int cureStepTicks() {
		return CURE_TICKS / CURE_STEPS;
	}

	/** Bags of cement for {@code tailsSolidsKg} kg of tails solids. */
	public static double bagsFor(double tailsSolidsKg) {
		return tailsSolidsKg * BINDER / CEMENT_BAG_KG;
	}
}
