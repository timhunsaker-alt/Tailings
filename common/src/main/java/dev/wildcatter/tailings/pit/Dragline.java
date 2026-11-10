package dev.wildcatter.tailings.pit;

/**
 * The walking dragline's figures and its strip (docs/multiblocks/dragline.md): a mid-size machine of the 30-60 m³ class, a 33 m³ bucket
 * on a 75 m boom at 32 degrees, on a trailing cable at the world's medium voltage. Plain Java: no volts here, the caller reads them
 * from Wildcatter's standard.
 *
 * <p>The strip is given in the frame the machine had when it started: {@code along} its walk from where it started, {@code across}
 * out to its cast side from its walk line. The strip runs from {@link #standoff()} out across its width, from 0 along its length; the
 * spoil goes beyond the strip's far edge, into the last strip's hole, or on flat ground for the first ("box") cut.
 */
public final class Dragline {
	/** The bucket: 33 m³ heaped, about 55 t of rock a pass (spec). */
	public static final double BUCKET_M3 = 33;
	public static final long BUCKET_KG = 55_000;
	/** A 33 m³ bucket of loose rock is about 25 m³ of bank (spec): it fills to 25 bank blocks, counted in quarters, or to 55 t. */
	public static final int BUCKET_BANK_QUARTERS = 25 * 4;
	/** The boom, 75 m of lattice at 32 degrees (spec). */
	public static final double BOOM_M = 75;
	public static final double BOOM_DEGREES = 32;
	/** It digs and casts to about 65 m out from its centre (spec), its boom point about 42 m up. */
	public static final double REACH_M = 65;
	public static final double POINT_UP_M = 42;
	/** It digs down to 35 m below its tub (spec, unverified), and up to 10 m above it (proposed: a bench a little higher than its own). */
	public static final int DIG_DEPTH_M = 35;
	public static final int DIG_ABOVE_M = 10;
	/** The tub, 15 m across; it stands back from the strip's edge by its radius plus 5 m (spec, proposed). */
	public static final double TUB_M = 15;
	public static final double EDGE_M = 5;
	/** The spoil lands at least this far beyond the strip's far edge, so its pile does not run back into the cut (proposed). */
	public static final double SPOIL_CLEAR_M = 6;
	/** Operating mass, about 1,750 t (spec). */
	public static final long MASS_KG = 1_750_000;

	/** Its job's settings (spec, its screen): the strip's width and length, the seam's depth under the tub, and the cast side. */
	public static final int WIDTH_MIN = 10, WIDTH_MAX = 40, WIDTH = 30;
	public static final int LENGTH_MIN = 10, LENGTH_MAX = 400, LENGTH = 100;
	public static final int SEAM_MIN = 1, SEAM_MAX = DIG_DEPTH_M, SEAM = 10;

	/**
	 * A 55 s cycle (spec; real 50-60 s): dragging the bucket in to fill it 15 s, hoisting and swinging to the spoil 17 s, dumping 3 s,
	 * swinging back and lowering 20 s (the split proposed).
	 */
	public static final int FILL_TICKS = 300, SWING_OUT_TICKS = 340, DUMP_TICKS = 60, SWING_BACK_TICKS = 400;
	public static final int CYCLE_TICKS = FILL_TICKS + SWING_OUT_TICKS + DUMP_TICKS + SWING_BACK_TICKS;

	/**
	 * Its draw (spec: about 2.5 MW average, 6 MW peak, proposed): the drag and hoist motors at 6 MW while the bucket fills and comes out
	 * of the cut, 1.2 MW through the rest of the cycle; 2.5 MW walking (its walking cams run off the same generators, proposed); 0.3 MW
	 * standing with its sets turning (proposed).
	 */
	public static final double PEAK_WATTS = 6_000_000;
	public static final double REST_WATTS = 1_200_000;
	public static final double WALK_WATTS = 2_500_000;
	public static final double STANDING_WATTS = 300_000;

	/** Walking: one step of 2 m a minute (spec; real about 0.2 km/h); the tub moves in the middle half of the step (proposed). */
	public static final double STEP_M = 2;
	public static final int STEP_TICKS = 1200;

	/** The phases of its work. */
	public enum Phase {
		FILL, SWING_OUT, DUMP, SWING_BACK, WALK, IDLE
	}

	/** Its job as its screen sets it: settings 0 width, 1 length, 2 seam depth, 3 cast right (1) or left (0). */
	public static final class Job {
		private int width = WIDTH, length = LENGTH, seam = SEAM;
		private boolean castRight = true;

		public int width() {
			return width;
		}

		public int length() {
			return length;
		}

		/** The seam's top lies this many metres under its tub's floor: it digs down to here and no further. */
		public int seam() {
			return seam;
		}

		public boolean castRight() {
			return castRight;
		}

		public int get(int i) {
			return switch (i) {
				case 0 -> width;
				case 1 -> length;
				case 2 -> seam;
				case 3 -> castRight ? 1 : 0;
				default -> 0;
			};
		}

		public void set(int i, int value) {
			switch (i) {
				case 0 -> width = Math.max(WIDTH_MIN, Math.min(WIDTH_MAX, value));
				case 1 -> length = Math.max(LENGTH_MIN, Math.min(LENGTH_MAX, value));
				case 2 -> seam = Math.max(SEAM_MIN, Math.min(SEAM_MAX, value));
				case 3 -> castRight = value != 0;
				default -> {
				}
			}
		}

		public void copy(Job other) {
			for (int i = 0; i < 4; i++) {
				set(i, other.get(i));
			}
		}

		public String describe() {
			return String.format(java.util.Locale.ROOT, "strip %d x %d m to the seam %d m down, casting %s", width, length, seam, castRight ? "right" : "left");
		}
	}

	private Dragline() {
	}

	/** Its tub's radius plus 5 m: how far its walk line stands back from the strip's near edge. */
	public static double standoff() {
		return TUB_M / 2 + EDGE_M;
	}

	/** The strip's far edge, metres out from its walk line. */
	public static double farEdge(int width) {
		return standoff() + width;
	}

	/** How far along, either way, its reach covers the strip's far edge; 0 if the strip is too wide to reach at all. */
	public static double reachAlongFarEdge(int width) {
		double far = farEdge(width);
		return far >= REACH_M ? 0 : Math.sqrt(REACH_M * REACH_M - far * far);
	}

	/** The nearest the spoil lands to its walk line. */
	public static double spoilFrom(int width) {
		return farEdge(width) + SPOIL_CLEAR_M;
	}

	/** True if a strip this wide leaves room to cast beyond it within its reach. */
	public static boolean fits(int width) {
		return spoilFrom(width) + 1 <= REACH_M;
	}

	/** True if a cell centred {@code across} out and {@code along} is in the strip. */
	public static boolean inStrip(double across, double along, int width, int length) {
		return across >= standoff() - 1e-6 && across < farEdge(width) - 1e-6 && along >= -1e-6 && along < length - 1e-6;
	}

	/** True if a point {@code dx, dz} metres from its centre is within its reach. */
	public static boolean inReach(double dx, double dz) {
		return dx * dx + dz * dz <= REACH_M * REACH_M;
	}

	/** True when, with nothing left in its reach, it should walk on: some of the strip lies beyond its reach ahead. */
	public static boolean walkOn(double centreAlong, int width, int length) {
		return centreAlong + reachAlongFarEdge(width) < length;
	}

	/** The phase {@code tick} ticks into a cycle. */
	public static Phase phase(int tick) {
		if (tick < FILL_TICKS) {
			return Phase.FILL;
		}
		if (tick < FILL_TICKS + SWING_OUT_TICKS) {
			return Phase.SWING_OUT;
		}
		if (tick < FILL_TICKS + SWING_OUT_TICKS + DUMP_TICKS) {
			return Phase.DUMP;
		}
		return tick < CYCLE_TICKS ? Phase.SWING_BACK : Phase.IDLE;
	}

	/** What it draws in each phase, watts. */
	public static double watts(Phase phase) {
		return switch (phase) {
			case FILL -> PEAK_WATTS;
			case SWING_OUT, DUMP, SWING_BACK -> REST_WATTS;
			case WALK -> WALK_WATTS;
			case IDLE -> STANDING_WATTS;
		};
	}

	/** Its average draw over a cycle, watts: about 2.5 MW. */
	public static double cycleAverageWatts() {
		return (PEAK_WATTS * FILL_TICKS + REST_WATTS * (CYCLE_TICKS - FILL_TICKS)) / CYCLE_TICKS;
	}

	/** Cycles an hour at its 55 s cycle: about 65. */
	public static double cyclesPerHour() {
		return 3600.0 * 20 / CYCLE_TICKS;
	}

	/** Bank m³ an hour, 25 a cycle: about 1,600 (spec). */
	public static double bankPerHour() {
		return cyclesPerHour() * BUCKET_BANK_QUARTERS / 4.0;
	}

	/** Its walking speed, km/h: 2 m a minute is 0.12 km/h. */
	public static double walkKmh() {
		return STEP_M / (STEP_TICKS / 20.0) * 3.6;
	}

	/** How far through a step the tub has moved, 0 to 1, {@code tick} ticks into it: still while the shoes come down, then lifted and moved, then set down. */
	public static double stepMoved(int tick) {
		double f = tick / (double) STEP_TICKS;
		return f < 0.25 ? 0 : f > 0.75 ? 1 : (f - 0.25) / 0.5;
	}

	/** The bucket's room left for a block of {@code kg} that is {@code quarters} of a bank block: whether it still fits. */
	public static boolean fits(long loadKg, int loadQuarters, long kg, int quarters) {
		return loadKg + kg <= BUCKET_KG && loadQuarters + quarters <= BUCKET_BANK_QUARTERS;
	}

	/** The angle from its heading to a point {@code across} out on its cast side and {@code along} ahead, degrees, plus to its right. */
	public static double swingTo(double across, double along, boolean castRight) {
		double degrees = Math.toDegrees(Math.atan2(across, along));
		return castRight ? degrees : -degrees;
	}
}
