package dev.wildcatter.tailings.mine;

/**
 * The drive's forcing ventilation as numbers (docs/multiblocks/ventilation.md): a fan at the portal blowing through a flexible duct to
 * near the face, boosters in the line, the duct's friction and leakage. Plain Java, unit-tested.
 *
 * <p>The duct is one line. Its friction is Darcy-Weisbach on a 1.4 m round duct with a friction factor of 0.02 (proposed); it leaks 1 %
 * of the air in it every 100 m (proposed, good duct), so the air the fan pushes falls off along it. Each fan follows a square-law curve
 * from its shut-off pressure to its free delivery, scaled by the fan laws for its motor's speed. All the fans in a line carry the same
 * air (the leakage before a booster is left out of the booster's share).
 */
public final class Airflow {
	/** The bore's clear section: 6 x 6 m. */
	public static final double BORE_AREA = DriveCourse.CLEAR * DriveCourse.CLEAR;
	/** Air speed the game asks for across the bore where people work (proposed; Queensland's roadway minimum is 0.3 m/s). */
	public static final double NEED_VELOCITY = 0.3;
	/** 0.3 m/s across 36 m²: 10.8 m³/s. */
	public static final double NEED = NEED_VELOCITY * BORE_AREA;
	/** Fresh air reaches this far past the duct's end, to the back-up's tail where people work (proposed). */
	public static final double REACH_PAST_END = 30;
	/** The bore beside the duct counts as fresh this far from it (half the 8 m face, proposed). */
	public static final double BESIDE_DUCT = 5;

	/** Lay-flat duct: 1.4 m (proposed from practice, unverified). */
	public static final double DUCT_DIAMETER = 1.4;
	/** Darcy friction factor of the duct (proposed). */
	public static final double FRICTION_FACTOR = 0.02;
	/** Air at the portal, kg/m³. */
	public static final double AIR_DENSITY = 1.2;
	/** The share of its air the duct keeps over 100 m (1 % leaks, proposed). */
	public static final double KEPT_PER_100M = 0.99;

	/** The main fan: 3 kPa shut off, 25 m³/s in free air; 15 m³/s at 1.9 kPa (proposed). 75 kW on its motor. */
	public static final double FAN_SHUTOFF_PA = 3_000;
	public static final double FAN_FREE = 25;
	/** The in-line booster: adds up to 1.5 kPa (proposed), free delivery as the main fan's. 30 kW. */
	public static final double BOOSTER_SHUTOFF_PA = 1_500;
	public static final double BOOSTER_FREE = 25;
	/** The cassette on the reel gantry holds 100 m of duct (proposed). */
	public static final int CASSETTE_METRES = 100;

	private Airflow() {
	}

	/** The duct's cross-section, m². */
	public static double ductArea() {
		return Math.PI * DUCT_DIAMETER * DUCT_DIAMETER / 4;
	}

	/** Pressure lost a metre of duct for each (m³/s)²: f / D x rho / (2 A²). */
	public static double resistancePerMetre() {
		double area = ductArea();
		return FRICTION_FACTOR / DUCT_DIAMETER * AIR_DENSITY / (2 * area * area);
	}

	/** Pa lost over {@code metres} of duct carrying {@code flow} m³/s, without leakage. */
	public static double frictionPa(double metres, double flow) {
		return resistancePerMetre() * metres * flow * flow;
	}

	/** The share of the air put in at one end that comes out at the other, {@code metres} on. */
	public static double kept(double metres) {
		return Math.pow(KEPT_PER_100M, Math.max(0, metres) / 100);
	}

	/**
	 * The length of tight duct with the same friction as {@code metres} of leaky duct carrying the fan's air: the air falls off along
	 * it, so the far end costs less pressure. The integral of kept(x)² over the length.
	 */
	public static double effectiveMetres(double metres) {
		if (metres <= 0) {
			return 0;
		}
		double k = -2 * Math.log(KEPT_PER_100M) / 100;
		return (1 - Math.exp(-k * metres)) / k;
	}

	/** One fan in the line: its shut-off pressure and free delivery, and its speed as a share of rated (its motor's output). */
	public record Fan(double shutoffPa, double free, double speed) {
		public static Fan main(double speed) {
			return new Fan(FAN_SHUTOFF_PA, FAN_FREE, speed);
		}

		public static Fan booster(double speed) {
			return new Fan(BOOSTER_SHUTOFF_PA, BOOSTER_FREE, speed);
		}

		/** Pa it adds at {@code flow}: P = Ps s² - (Ps / Qf²) Q², the fan laws at speed s. */
		public double pressure(double flow) {
			double s = Math.max(0, Math.min(1, speed));
			return shutoffPa * s * s - shutoffPa / (free * free) * flow * flow;
		}

		double slope() {
			return shutoffPa / (free * free);
		}

		double head() {
			double s = Math.max(0, Math.min(1, speed));
			return shutoffPa * s * s;
		}
	}

	/** What a line of duct gives: the air the fans push, the air at its end, and the fans' pressure. */
	public record Result(double fanFlow, double endFlow, double pressurePa, double metres) {
		/** Air speed across the bore at the duct's end. */
		public double velocity() {
			return endFlow / BORE_AREA;
		}

		public boolean enough() {
			return endFlow >= NEED - 1e-9;
		}
	}

	/**
	 * The fans in series on {@code metres} of duct: Q² = sum of heads / (R x effective length + sum of slopes), and the air at the far end
	 * is what is kept of it. No fan turning, no air.
	 */
	public static Result solve(double metres, Fan... fans) {
		double head = 0;
		double slope = 0;
		for (Fan fan : fans) {
			if (fan.speed() > 0) {
				head += fan.head();
				slope += fan.slope();
			}
		}
		if (head <= 0) {
			return new Result(0, 0, 0, metres);
		}
		double system = resistancePerMetre() * effectiveMetres(metres);
		double flow = Math.sqrt(head / (system + slope));
		return new Result(flow, flow * kept(metres), system * flow * flow, metres);
	}

	/** "POOR AIR: 0.21 m/s, needs 0.30" or "air 0.34 m/s (12.2 m³/s)". */
	public static String describe(double endFlow) {
		double velocity = endFlow / BORE_AREA;
		if (endFlow + 1e-9 < NEED) {
			return String.format(java.util.Locale.ROOT, "POOR AIR: %.2f m/s (%.1f m3/s), needs %.2f", velocity, endFlow, NEED_VELOCITY);
		}
		return String.format(java.util.Locale.ROOT, "air %.2f m/s (%.1f m3/s)", velocity, endFlow);
	}
}
