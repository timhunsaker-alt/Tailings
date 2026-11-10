package dev.wildcatter.tailings.shaft;

/**
 * The shaft and hoist's figures (docs/multiblocks/shaft-and-hoist.md, L4). Plain Java, unit-tested. A figure marked proposed is a
 * textbook or scaled value chosen while Tim was away (docs/questions/l4-shaft.md); the sources are listed in the spec.
 */
public final class Hoisting {
	private Hoisting() {
	}

	/** A skip's payload: 3 t of rock (proposed; a small-mine bottom-dump skip). */
	public static final double SKIP_KG = 3_000;
	/** The measuring flask fills to one skip's payload, so every skip goes up with the same load. */
	public static final double FLASK_KG = SKIP_KG;
	/** The flask's fill from its surge, kg a second (proposed: 3 t in 10 s, the loading time). */
	public static final double FLASK_FILL_KG_PER_SECOND = 300;
	/** The headframe bin: 100 t of steel bin under the skip dump (proposed). */
	public static final double BIN_KG = 100_000;

	/** Full rope speed, m/s (proposed; real small shafts wind at 8 to 12 m/s). */
	public static final double ROPE_SPEED = 10;
	/** Accelerating and braking, m/s² (proposed; real drum hoists 0.6 to 1.0). */
	public static final double ACCEL = 0.75;
	/** The creep into the dump scroll and the loading stop, m/s (proposed): the last metres are run slowly, as real hoists do. */
	public static final double CREEP = 0.5;
	/**
	 * The rest at each end of a wind, s (proposed): with the skips in balance, one skip loads at the bottom while the other dumps at the
	 * top, in the same 10 s.
	 */
	public static final double REST_SECONDS = 10;

	/** How far above the collar the skip's bottom stands when it dumps, m (proposed: the scroll in a 30 m headframe). */
	public static final int DUMP_HEIGHT = 18;
	/** The sheaves' centres above the collar, m: the head sheaves at the top of the headframe. */
	public static final double SHEAVE_HEIGHT = 27.5;
	/** The farthest the hoist house may stand from the headframe, m (proposed): the rope's free span from drum to sheave. */
	public static final int ROPE_REACH = 60;
	/** The deepest loading pocket the hoist looks for below the collar, m (proposed). */
	public static final int MAX_DEPTH = 200;

	/** The hoist's motor, W (proposed): the payload's lift, the losses and the acceleration. */
	public static final int MOTOR_WATTS = 450_000;
	/** The drive train's efficiency (proposed). */
	public static final double EFFICIENCY = 0.85;
	/** g, m/s². */
	public static final double G = 9.81;

	/** The power to lift one payload at full rope speed with the skips in balance: only the payload and the losses, W. */
	public static double liftWatts() {
		return SKIP_KG * G * ROPE_SPEED / EFFICIENCY;
	}

	/**
	 * The time of one wind over {@code travel} metres, s: up to rope speed, at it, and braked, on a trapezoid (or a triangle when the
	 * travel is too short to reach full speed). The creep of the last metres is left out; it adds well under a second.
	 */
	public static double windSeconds(double travel) {
		double toFull = ROPE_SPEED / ACCEL;
		double accelMetres = ROPE_SPEED * toFull / 2;
		if (travel <= 2 * accelMetres) {
			return 2 * Math.sqrt(travel / ACCEL);
		}
		return 2 * toFull + (travel - 2 * accelMetres) / ROPE_SPEED;
	}

	/** One cycle: a wind and a rest, s. Each cycle brings one skip to the top. */
	public static double cycleSeconds(double travel) {
		return windSeconds(travel) + REST_SECONDS;
	}

	/** What the hoist lifts, t/h, over {@code travel} metres. */
	public static double tonnesPerHour(double travel) {
		return SKIP_KG / 1000 * 3600 / cycleSeconds(travel);
	}
}
