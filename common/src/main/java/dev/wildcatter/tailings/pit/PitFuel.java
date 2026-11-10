package dev.wildcatter.tailings.pit;

/**
 * A pit machine's diesel (docs/multiblocks/open-pit.md, P2a): it burns by the engine's work, at diesel's 36 MJ a litre (Wildcatter's
 * {@code LIQUID_FUEL_JOULES_PER_L}) and an engine efficiency of 0.38 (approved 2026-11-09). Flat out the 765 kW truck burns about
 * 200 L an hour; parked with its engine off, nothing.
 */
public final class PitFuel {
	public static final double JOULES_PER_L = 36.0e6;
	public static final double ENGINE_EFFICIENCY = 0.38;
	/** Idle with a driver in, or running a route: 3 % of rated power (approved). */
	public static final double IDLE = 0.03;
	/** The truck's body hoist while it moves: 30 % (approved). */
	public static final double HOIST = 0.30;
	/** The loader digging or dumping a pass: 60 % (approved). */
	public static final double PASS = 0.60;
	/** Tanks: the truck 1,100 L (spec), the loader 1,500 L (approved). */
	public static final double TRUCK_TANK_L = 1_100;
	public static final double LOADER_TANK_L = 1_500;
	/** The ultra truck's 3,785 L (spec sheet, decision P5-3). */
	public static final double ULTRA_TANK_L = 3_785;
	/** The dozer: about 1,200 L (unverified), dozing at 80 % of its rated power (proposed). */
	public static final double DOZER_TANK_L = 1_200;
	public static final double DOZING = 0.80;
	/** A new machine comes with a tenth of its tank (approved). */
	public static final double NEW_FILL = 0.10;
	/** A self-driving truck under this goes to fuel (spec). */
	public static final double LOW = 0.15;
	/** The fuel bay's nozzle: 600 L a minute (approved). */
	public static final double BAY_LITRES_PER_SECOND = 600.0 / 60.0;
	/** The fuel bay's day tank, 2,000 L, and how far its hose reaches, 8 m (approved). */
	public static final double BAY_TANK_L = 2_000;
	public static final double BAY_REACH = 8.0;

	private PitFuel() {
	}

	/** Litres the engine burns making {@code watts} for {@code seconds}. */
	public static double litres(double watts, double seconds) {
		return Math.max(0, watts) * seconds / (JOULES_PER_L * ENGINE_EFFICIENCY);
	}

	/**
	 * The engine's output while driving: the power at the wheels, m (a + g (grade + rolling)) v, over the drive's 0.8, never less
	 * than idle and never more than rated. Coasting and braking it idles.
	 */
	public static double drivingWatts(PitDrive.Spec spec, double massKg, double speedBefore, double speedAfter, double slope, double dt) {
		double v = (Math.abs(speedBefore) + Math.abs(speedAfter)) / 2;
		double accel = (Math.abs(speedAfter) - Math.abs(speedBefore)) / dt;
		double way = Math.signum(speedAfter == 0 ? speedBefore : speedAfter);
		double wheels = massKg * (accel + PitDrive.G * (slope * way + PitDrive.ROLLING)) * v;
		double engine = wheels / PitDrive.ETA;
		return Math.max(spec.watts() * IDLE, Math.min(spec.watts(), engine));
	}
}
