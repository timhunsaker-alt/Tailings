package dev.wildcatter.tailings.haul;

/**
 * The battery locomotive's numbers (docs/multiblocks/battery-locomotive.md, L2): the 8 t class on a lead-acid traction battery, its
 * charge in usable watt-hours, and the charger. Plain Java. A figure marked (proposed) or (unverified) is so marked in the spec.
 */
public final class Battery {
	/** Two 22 kW traction motors (a supplier's listing for the 8 t class, unverified). */
	public static final double TRACTION_WATTS = 2 * 22_000;
	/** The 132 V, 440 Ah traction battery (unverified): about 58 kWh. */
	public static final double BATTERY_VOLTS = 132;
	public static final double BATTERY_AMP_HOURS = 440;
	public static final double NAMEPLATE_WH = BATTERY_VOLTS * BATTERY_AMP_HOURS;
	/** Usable: 80 % of the nameplate (real: a deeper discharge shortens a lead-acid battery's life). About 46 kWh. */
	public static final double USABLE_SHARE = 0.8;
	public static final double USABLE_WH = NAMEPLATE_WH * USABLE_SHARE;
	/** Under this share of the usable charge it reads LOW BATTERY. */
	public static final double LOW_SHARE = 0.2;
	/** The motors and their drive take their output over this from the battery (proposed). */
	public static final double DRIVE_EFFICIENCY = 0.85;
	/** A new battery, crafted or on a new locomotive, holds this share of its usable charge (proposed; as a new pit machine's 10 % diesel). */
	public static final double NEW_SHARE = 0.1;

	/** The charger: 20 kW continuous, 80 % of a three-phase 30 A circuit at 480 V (proposed). */
	public static final double CHARGER_WATTS = 20_000;
	/** Its rectifier's power factor (proposed). */
	public static final double CHARGER_POWER_FACTOR = 0.95;
	/** Of what it draws, this goes into the battery: 10 % is lost in the charger and the battery (proposed). */
	public static final double CHARGE_EFFICIENCY = 0.9;
	/** A battery locomotive stopped with its cab this close to a charger charges where it stands (proposed). */
	public static final double CHARGE_REACH = 3.0;

	private Battery() {
	}

	/**
	 * Watt-hours the battery gives for {@code seconds} of the motors putting out {@code motorWatts} (holding the grade, rolling and
	 * speeding up): their output over the drive's 85 %. Nothing going down the decline: the rack brake holds it, with no regeneration.
	 */
	public static double drawWh(double motorWatts, double seconds) {
		return Math.max(0, motorWatts) / DRIVE_EFFICIENCY * seconds / 3600;
	}

	/** Watt-hours a charger drawing {@code watts} from the line puts into the battery in {@code seconds}. */
	public static double chargeWh(double watts, double seconds) {
		return Math.max(0, watts) * CHARGE_EFFICIENCY * seconds / 3600;
	}

	/** Watt-hours to lift {@code kg} up {@code metres} of height along {@code run} metres of track, with rolling resistance. */
	public static double climbWh(double kg, double metres, double run) {
		return drawWh(kg * Haul.GRAVITY * (metres + Haul.ROLLING * run), 1);
	}

	/** Hours a charger takes to fill a flat battery. */
	public static double hoursToCharge() {
		return USABLE_WH / (CHARGER_WATTS * CHARGE_EFFICIENCY);
	}

	/** The share of its usable charge {@code wh} is, 0 to 1. */
	public static double share(double wh) {
		return Math.max(0, Math.min(1, wh / USABLE_WH));
	}

	public static boolean low(double wh) {
		return share(wh) < LOW_SHARE;
	}

	/** At the floor: it has nothing more to give. */
	public static boolean flat(double wh) {
		return wh <= 1e-6;
	}

	/** The charge as 4-20 mA over empty to full (the charger's PLC point). */
	public static double chargeMa(double wh) {
		return 4 + 16 * share(wh);
	}
}
