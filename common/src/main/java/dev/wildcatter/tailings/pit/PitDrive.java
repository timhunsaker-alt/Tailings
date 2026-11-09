package dev.wildcatter.tailings.pit;

/**
 * How a haul truck or loader speeds up, climbs and stops (docs/multiblocks/open-pit.md, Haul trucks): its engine's power at the
 * wheels against its weight on the grade, v = P η / (m g (sin θ + rolling)), held to its top speed, and a retarder that holds a
 * loaded truck to 15 km/h down a steep grade.
 */
public final class PitDrive {
	public static final double G = 9.81;
	/** Engine power that reaches the wheels (proposed). */
	public static final double ETA = 0.8;
	/** Rolling resistance on a haul road, as a fraction of weight (proposed). */
	public static final double ROLLING = 0.02;
	/** The most a vehicle speeds up, and how hard it brakes, in m/s² (proposed). */
	public static final double MOST_ACCEL = 1.5;
	public static final double BRAKE = 3.0;
	/** What a rolling vehicle loses with its foot off (engine braking and drag), m/s² (proposed). */
	public static final double COAST = 0.3;
	/** The retarder holds it to 15 km/h on any grade steeper than 3 % down (proposed). */
	public static final double RETARDER_SPEED = 15 / 3.6;
	public static final double RETARDER_GRADE = -0.03;

	/**
	 * A vehicle's figures: size in metres, mass empty and its rating in kg, engine watts, top speeds in m/s, the radius it turns
	 * on in metres.
	 */
	public record Spec(double length, double width, double height, long emptyKg, long ratingKg, double watts, double topSpeed,
			double reverseSpeed, double turnRadius) {
	}

	/**
	 * The 100 t class haul truck: 91 t payload, 10.0 x 6.2 x 5.2 m, about 70 t empty, 765 kW, 60 km/h (checked 2026-11-09). It turns
	 * on 12.5 m (proposed: a 25 m turning circle).
	 */
	public static final Spec TRUCK = new Spec(10.0, 6.2, 5.2, 70_000, 91_000, 765_000, 60 / 3.6, 3.0, 12.5);
	/**
	 * The wheel loader: a 12 m³ bucket of 20.4 t, 607 kW, about 106 t at work (checked 2026-11-09); 15 x 4.5 x 6 m with its bucket
	 * down and 20 km/h (proposed). It bends in the middle and turns on 9 m (proposed).
	 */
	public static final Spec LOADER = new Spec(15.0, 4.5, 6.0, 106_000, 20_400, 607_000, 20 / 3.6, 3.0, 9.0);
	/**
	 * The rotary blasthole drill (open-pit-p3-blasting.md): about 7 x 15 m on its crawlers and 26 m to the mast top (unverified), a
	 * 600 kW diesel (unverified), tramming at 1.5 km/h (proposed); about 110 t (proposed). By hand it steers on 5 m.
	 */
	public static final Spec DRILL = new Spec(15.0, 7.0, 26.0, 110_000, 1, 600_000, 1.5 / 3.6, 1.5 / 3.6, 5.0);
	/** The emulsion truck: 8.5 x 2.5 x 3.8 m, about 300 kW, 12 t of matrix, about 14 t empty and 40 km/h (proposed). */
	public static final Spec EMULSION_TRUCK = new Spec(8.5, 2.5, 3.8, 14_000, 12_000, 300_000, 40 / 3.6, 3.0, 8.0);

	private PitDrive() {
	}

	/**
	 * Its speed after {@code dt} seconds. {@code throttle} is 1 forward, -1 back, 0 off; {@code slope} is the grade it is on (the
	 * sine, plus uphill in the way it faces); {@code massKg} with its load.
	 */
	public static double next(Spec spec, double massKg, double speed, double throttle, double slope, double dt) {
		double a;
		boolean forwardWanted = throttle > 0, backWanted = throttle < 0;
		if ((forwardWanted && speed < -0.05) || (backWanted && speed > 0.05)) {
			a = -Math.signum(speed) * BRAKE;
		} else if (throttle != 0) {
			double way = Math.signum(throttle);
			double drive = Math.min(spec.watts() * ETA / Math.max(Math.abs(speed), 1.0), massKg * G * 0.5);
			// against it: the grade (uphill in the way it faces is against forward) and rolling
			double against = massKg * G * (slope * way + ROLLING);
			a = way * Math.min(MOST_ACCEL, (drive - against) / massKg);
		} else {
			if (Math.abs(speed) < 0.1) {
				return 0;   // the brakes hold it standing, on any grade
			}
			a = -Math.signum(speed) * (COAST + G * ROLLING) - G * slope;
		}
		double v = speed + a * dt;
		if (throttle == 0 && Math.signum(v) != Math.signum(speed)) {
			v = 0;
		}
		double top = spec.topSpeed();
		if (slope * Math.signum(v) < RETARDER_GRADE) {
			top = Math.min(top, RETARDER_SPEED);
		}
		return Math.max(-spec.reverseSpeed(), Math.min(top, v));
	}
}
