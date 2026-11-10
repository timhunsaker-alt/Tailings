package dev.wildcatter.tailings.pit;

/**
 * The pit's power (docs/multiblocks/open-pit-p5-big-machines.md, P5a): the pit switch house on the world's medium voltage, its four
 * breaker outlets, and the trailing cables that run from them to the electric machines. Plain Java: the volts are passed in, read
 * from Wildcatter's standard by the caller, never written here.
 */
public final class PitPower {
	/** Four outlets, each a breaker, a ground-check relay and a trailing-cable coupler (spec). */
	public static final int OUTLETS = 4;
	/** Each outlet's rating, 1,200 A (proposed: the dragline's 6 MW peak is about 830 A at 4.16 kV); the house's bus 2,000 A (proposed). */
	public static final double OUTLET_AMPS = 1_200;
	public static final double BUS_AMPS = 2_000;
	/** An outlet's breaker trips at 1.25 x its machine's peak held for 1 s; the incoming breaker at 1.25 x the bus (spec). */
	public static final double TRIP_FACTOR = 1.25;
	public static final double TRIP_SECONDS = 1.0;
	/** One trailing cable item: 400 m of medium-voltage mining cable on a reel (open-pit.md; real reels 300-500 m). */
	public static final double CABLE_M = 400;
	/** About 0.1 ohm a km a phase (proposed: a 4/0 AWG class mining cable, unverified). */
	public static final double CABLE_OHMS_PER_KM = 0.1;
	/** Under 90 % of the nominal volts at the machine it stops: LOW VOLTAGE (proposed, P5-5). */
	public static final double LOW_VOLTAGE = 0.90;
	/** The big machines' drives are variable-frequency: their front ends draw at 0.95 (proposed, as the TBM's). */
	public static final double POWER_FACTOR = 0.95;
	/** A machine that moves more than this between two ticks has torn its cable out: it breaks, and its outlet trips. */
	public static final double TORN_M = 8;

	/** The electric drill: the same 600 kW as its diesel (proposed). */
	public static final double DRILL_WATTS = 600_000;
	/** The hydraulic shovel's electric variant, 860 kW (spec sheet). */
	public static final double HYD_SHOVEL_ELECTRIC_WATTS = 860_000;
	/** The rope shovel: 3.5 MW during the hoist (12 s of each cycle), 0.6 MW the rest; about 1.5 MW on average (proposed). */
	public static final double ROPE_HOIST_WATTS = 3_500_000;
	public static final double ROPE_REST_WATTS = 600_000;

	private PitPower() {
	}

	/** Line current of a balanced three-phase load of {@code watts} at {@code lineVolts} and {@code powerFactor}. */
	public static double amps(double watts, double lineVolts, double powerFactor) {
		return lineVolts <= 0 ? 0 : Math.max(0, watts) / (Math.sqrt(3) * lineVolts * powerFactor);
	}

	/** Where an outlet's breaker trips: 1.25 x its machine's peak current at the nominal volts, never over the outlet's rating. */
	public static double tripAmps(double peakWatts, double nominalLineVolts) {
		return Math.min(OUTLET_AMPS, TRIP_FACTOR * amps(peakWatts, nominalLineVolts, POWER_FACTOR));
	}

	/** The incoming breaker's pickup. */
	public static double busTripAmps() {
		return TRIP_FACTOR * BUS_AMPS;
	}

	/** One phase of a cable {@code metres} long, in ohms. The whole cable carries the current, laid out or still on its reel. */
	public static double cableOhms(double metres) {
		return CABLE_OHMS_PER_KM * Math.max(0, metres) / 1000.0;
	}

	/**
	 * The cable paid out after a move: it lies along the machine's track, so moving away from the switch house (or across) pays out
	 * what it moved; moving back toward it reels in what it moved, never to less than the straight line to the house.
	 */
	public static double payOut(double paid, double distanceBefore, double distanceAfter, double moved) {
		if (moved <= 0) {
			return Math.max(paid, distanceAfter);
		}
		double next = distanceAfter >= distanceBefore ? paid + moved : paid - moved;
		return Math.max(distanceAfter, next);
	}

	/** Whether {@code lineVolts} at the machine are enough to run on. */
	public static boolean enough(double lineVolts, double nominalLineVolts) {
		return lineVolts >= nominalLineVolts * LOW_VOLTAGE;
	}

	/**
	 * A breaker's definite-time trip: the seconds over its pickup so far, after another {@code seconds} at {@code amps}. Under the
	 * pickup it starts again from nothing.
	 */
	public static double over(double sofar, double amps, double pickup, double seconds) {
		return amps > pickup ? sofar + seconds : 0;
	}

	/** True once it has been over its pickup for the trip time. */
	public static boolean trips(double secondsOver) {
		return secondsOver >= TRIP_SECONDS - 1e-9;
	}

	/** The rope shovel's draw a moment into its 35 s cycle: the hoist's 3.5 MW for its first 12 s, 0.6 MW the rest (proposed). */
	public static double ropeShovelWatts(double secondsIntoCycle) {
		return secondsIntoCycle < 12 ? ROPE_HOIST_WATTS : ROPE_REST_WATTS;
	}

	/** Its average over a cycle of {@code cycleSeconds}. */
	public static double ropeShovelAverage(double cycleSeconds) {
		return (ROPE_HOIST_WATTS * 12 + ROPE_REST_WATTS * (cycleSeconds - 12)) / cycleSeconds;
	}

	/** The 4-20 mA of an outlet's AMPS over its 1,200 A. */
	public static double ampsMa(double amps) {
		return 4 + 16 * Math.min(1.03, Math.max(0, amps) / OUTLET_AMPS);
	}
}
