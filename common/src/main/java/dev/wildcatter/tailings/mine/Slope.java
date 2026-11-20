package dev.wildcatter.tailings.mine;

import java.util.Map;

/**
 * Whether a highwall or a spoil pile stands (docs/multiblocks/dragline-hand-control.md, Part 2). Plain Java, unit-tested.
 *
 * <p><b>A highwall</b> by Culmann's planar wedge (1866; Taylor 1948, Das): a slope at angle beta in ground of cohesion c, friction angle
 * phi and unit weight gamma stands to a critical height H_c = (4c/gamma) sin(beta) cos(phi) / (1 - cos(beta - phi)), failing on a plane
 * through its toe at (beta + phi)/2. Its factor of safety here is H_c over its height (a simplification: it reads the same at 1 and moves
 * the same way). Water lowers phi as atan(0.7 tan phi) (about a pore pressure ratio of 0.3); blast damage halves c.
 *
 * <p><b>A spoil pile</b> stands at the settle rule's 37 degrees, its angle of repose, and never fails on a firm dry floor; over a weak
 * floor or with water at its toe it stands to 24 m, with both to 12 m (proposed).
 *
 * <p><b>Stages:</b> 1.3 and over STABLE; to 1.0 CREEP (cracks under 1.15); under 1.0 FAILING on a clock, a game day at 0.95 down to two
 * minutes at 0.7; then it slides.
 */
public final class Slope {
	/** The ground's strength (proposed: textbook mid-range values): cohesion kPa, friction angle degrees, unit weight kN/m³. */
	public enum Ground {
		SAND(5, 32, 17), GRAVEL(2, 38, 19), DIRT(20, 25, 18), CLAY(30, 18, 19), SPOIL(5, 37, 18), ROCK(200, 35, 26);

		public final double c, phi, gamma;

		Ground(double c, double phi, double gamma) {
			this.c = c;
			this.phi = phi;
			this.gamma = gamma;
		}

		/** A block's ground by its id: sands, gravel, clay and mud, the soils, muck as spoil, anything else as rock. */
		public static Ground of(String blockId, boolean soft) {
			String path = blockId.substring(blockId.indexOf(':') + 1);
			if (path.equals("muck")) {
				return SPOIL;
			}
			if (path.equals("sand") || path.equals("red_sand") || path.equals("dry_tails") || path.endsWith("_sand")) {
				return SAND;
			}
			if (path.equals("gravel")) {
				return GRAVEL;
			}
			if (path.equals("clay") || path.equals("mud") || path.equals("packed_mud")) {
				return CLAY;
			}
			if (soft || path.contains("dirt") || path.equals("grass_block") || path.equals("podzol") || path.equals("mycelium") || path.equals("farmland")
					|| path.equals("dirt_path") || path.equals("moss_block")) {
				return DIRT;
			}
			return ROCK;
		}
	}

	/** The ground in a wedge, by count: its strength averaged. */
	public record Mix(double c, double phi, double gamma) {
		public static Mix of(Map<Ground, Integer> counts, boolean wet, boolean blasted) {
			double n = 0, c = 0, tan = 0, gamma = 0;
			for (Map.Entry<Ground, Integer> e : counts.entrySet()) {
				int k = e.getValue();
				n += k;
				c += e.getKey().c * k;
				tan += Math.tan(Math.toRadians(e.getKey().phi)) * k;
				gamma += e.getKey().gamma * k;
			}
			if (n == 0) {
				return new Mix(0, 0, 0);
			}
			double t = tan / n * (wet ? WET_FRICTION : 1);
			return new Mix(c / n * (blasted ? BLAST_COHESION : 1), Math.toDegrees(Math.atan(t)), gamma / n);
		}

		public boolean empty() {
			return gamma == 0;
		}
	}

	/** Wet ground's friction, as a share of dry (proposed: a pore pressure ratio of about 0.3). */
	public static final double WET_FRICTION = 0.7;
	/** A blast's damage zone keeps this much of its cohesion (proposed). */
	public static final double BLAST_COHESION = 0.5;
	/** A blast within this many metres of a crest damages it; within the next, it shakes it (proposed). */
	public static final double BLAST_DAMAGE_M = 15, BLAST_SHAKE_M = 100;
	/** A shake takes this off a factor of safety for a game hour (proposed). */
	public static final double SHAKE_FS = 0.05;
	public static final long SHAKE_TICKS = 1000;

	/** The spoil's limit heights, m (proposed): over a weak floor or with a wet toe; with both. */
	public static final double SPOIL_ONE = 24, SPOIL_BOTH = 12;

	/** The stage boundaries. */
	public static final double STABLE_FS = 1.3, CRACK_FS = 1.15;

	/** The clock: a game day at FS 0.95, two minutes at 0.7, 10 s under that (proposed). */
	public static final long DAY_TICKS = 24_000, LEAST_TICKS = 2_400, AT_ONCE_TICKS = 200;

	public enum Stage {
		STABLE, CREEP, FAILING, SLID
	}

	private Slope() {
	}

	/** Culmann's critical height, m; infinite for a slope no steeper than its friction angle, or with no ground. */
	public static double criticalHeight(double c, double phiDeg, double gamma, double betaDeg) {
		if (gamma <= 0 || betaDeg <= phiDeg + 1e-9) {
			return Double.POSITIVE_INFINITY;
		}
		double b = Math.toRadians(betaDeg), p = Math.toRadians(phiDeg);
		return 4 * c / gamma * Math.sin(b) * Math.cos(p) / (1 - Math.cos(b - p));
	}

	public static double criticalHeight(Mix mix, double betaDeg) {
		return criticalHeight(mix.c(), mix.phi(), mix.gamma(), betaDeg);
	}

	/** A highwall's factor of safety: its critical height over its height, less any shake. */
	public static double highwallFs(Mix mix, double heightM, double betaDeg, double shake) {
		if (mix.empty() || heightM <= 0) {
			return Double.POSITIVE_INFINITY;
		}
		return criticalHeight(mix, betaDeg) / heightM - shake;
	}

	/** The failure plane's angle, degrees: through the toe at (beta + phi)/2. */
	public static double planeDegrees(double betaDeg, double phiDeg) {
		return (betaDeg + phiDeg) / 2;
	}

	/** How far behind the crest the failure plane meets the bench top, m: where the tension cracks open. */
	public static double crackBehind(double heightM, double betaDeg, double thetaDeg) {
		double cotT = 1 / Math.tan(Math.toRadians(thetaDeg));
		double cotB = betaDeg >= 89.999 ? 0 : 1 / Math.tan(Math.toRadians(betaDeg));
		return Math.max(0, heightM * (cotT - cotB));
	}

	/** A slope's angle from its height and the run of its face, degrees: a run of 0 is vertical. */
	public static double angle(double heightM, double runM) {
		return runM <= 0 ? 90 : Math.toDegrees(Math.atan2(heightM, runM));
	}

	/** A spoil pile's factor of safety: its limit height over its height; infinite on a firm dry floor. */
	public static double spoilFs(double heightM, boolean weakFloor, boolean wetToe) {
		if ((!weakFloor && !wetToe) || heightM <= 0) {
			return Double.POSITIVE_INFINITY;
		}
		return (weakFloor && wetToe ? SPOIL_BOTH : SPOIL_ONE) / heightM;
	}

	public static Stage stage(double fs) {
		return fs >= STABLE_FS ? Stage.STABLE : fs >= 1 ? Stage.CREEP : Stage.FAILING;
	}

	public static boolean cracks(double fs) {
		return fs < CRACK_FS;
	}

	/**
	 * Ticks from now to the slide at a factor of safety {@code fs} under 1, at a time scale of {@code scale}:1 (the clock is a wait, so
	 * it follows Wildcatter's time scale, as the paste cure does): a game day at 0.95, as the square of (fs - 0.7)/0.25 down to two
	 * minutes; within 10 s under 0.7. -1 at 1 and over.
	 */
	public static long clockTicks(double fs, int scale) {
		if (fs >= 1) {
			return -1;
		}
		if (fs < 0.7) {
			return AT_ONCE_TICKS;
		}
		double f = (fs - 0.7) / 0.25;
		long t = Math.round(DAY_TICKS * f * f / Math.max(1, scale));
		return Math.max(LEAST_TICKS / Math.max(1, scale), t);
	}

	/**
	 * How fast a prism on it moves, mm an hour: under 0.1 standing; creeping, up to 2 at FS 1.0; failing, 1/velocity falling in a
	 * straight line to zero at the slide (Fukuzono 1985), from 2 mm/h when its clock started.
	 */
	public static double velocity(double fs, long ticksLeft, long clockTicks) {
		if (fs >= STABLE_FS) {
			return 0.05;
		}
		if (fs >= 1) {
			return 0.05 + 2 * (STABLE_FS - fs) / (STABLE_FS - 1);
		}
		if (clockTicks <= 0) {
			return 2;
		}
		double left = Math.max(1, ticksLeft) / (double) clockTicks;
		return Math.min(1000, 2 / Math.min(1, left));
	}

	/** What a crew reads from a prism's speed: never the factor of safety. */
	public static String trend(double mmPerHour, boolean failing) {
		return failing ? "ACCELERATING" : mmPerHour > 0.2 ? "CREEPING" : "STEADY";
	}
}
