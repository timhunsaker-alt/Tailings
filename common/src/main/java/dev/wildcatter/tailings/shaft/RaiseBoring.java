package dev.wildcatter.tailings.shaft;

import java.util.Locale;

/**
 * Raise boring (docs/multiblocks/shaft-and-hoist.md, "Sinking it: raise boring", L4b): a raise borer on a pad over a drive drills a
 * pilot hole down to it, then pulls a reamer head back up to the shaft's full size, its cuttings falling into the drive. The figures and
 * the borer's progress, plain Java and unit-tested ({@code RaiseBoringTest}). A figure marked proposed is the spec's, chosen while Tim was
 * away (shaft-and-hoist.md, Decision L4-1).
 */
public final class RaiseBoring {
	/** The borer's motor, W (the spec's 400 kW at the medium-voltage tier, proposed). */
	public static final int MOTOR_WATTS = 400_000;
	/** The reamed shaft, cells a side: a 5 m reamer (proposed; real raise borers ream 1-6 m), the headframe's 5 x 5 shaft. */
	public static final int SHAFT = 5;
	/** The pilot hole's bore, mm (drawn thin; real pilots are about 230-350 mm, unverified). */
	public static final int PILOT_MM = 300;
	/** The pilot: a metre every 10 s; the ream: a metre every 60 s (scaled, proposed; real reaming makes a few metres a day). */
	public static final double PILOT_SECONDS_PER_M = 10;
	public static final double REAM_SECONDS_PER_M = 60;
	/** The deepest drive it looks for, m (proposed), and the rock it wants over the drive's roof (the spec's at least 3 cells). */
	public static final int MAX_DEPTH = 200;
	public static final int MIN_COVER = 3;

	private RaiseBoring() {
	}

	/** How long a raise {@code metres} deep takes, s: its pilot to the drive and its ream back up. */
	public static double hours(int metres) {
		return metres * (PILOT_SECONDS_PER_M + REAM_SECONDS_PER_M) / 3600.0;
	}

	public enum Phase {
		/** Looking down for a drive. */
		SITING,
		PILOT,
		REAM,
		DONE
	}

	/** One borer's progress, saved on its part. */
	public static final class Progress {
		private Phase phase = Phase.SITING;
		/** Metres of pilot drilled; the drive's depth below the pad (the air cell broken into), and the layer reamed up to. */
		private int pilot;
		private int drive;
		private int reamed;
		/** Seconds into the metre being drilled or reamed. */
		private double into;

		public Phase phase() {
			return phase;
		}

		public int pilot() {
			return pilot;
		}

		public int drive() {
			return drive;
		}

		/** Metres of raise reamed up from the drive's roof. */
		public int reamed() {
			return reamed;
		}

		/** The raise's length from the drive's roof to under the collar slab, m: {@code drive - 2} (the pad's own metre left). */
		public int raise() {
			return Math.max(0, drive - 2);
		}

		/** A drive found {@code depth} m below the pad (the first air cell under rock): starts the pilot. */
		public void found(int depth) {
			if (phase == Phase.SITING) {
				drive = depth;
				pilot = 0;
				into = 0;
				phase = Phase.PILOT;
			}
		}

		/**
		 * Runs on {@code dt} s at {@code strength} of the motor; returns how many metres finished in this step (a pilot metre or a
		 * reamed layer), 0 or 1.
		 */
		public int run(double dt, double strength) {
			if (strength <= 0 || phase == Phase.SITING || phase == Phase.DONE) {
				return 0;
			}
			into += dt * Math.min(1, strength);
			double per = phase == Phase.PILOT ? PILOT_SECONDS_PER_M : REAM_SECONDS_PER_M;
			if (into < per) {
				return 0;
			}
			into -= per;
			if (phase == Phase.PILOT) {
				pilot++;
				if (pilot >= drive) {
					phase = raise() > 0 ? Phase.REAM : Phase.DONE;   // broken through: the reamer head goes on in the drive
					into = 0;
				}
			} else {
				reamed++;
				if (reamed >= raise()) {
					phase = Phase.DONE;
				}
			}
			return 1;
		}

		public String readout() {
			return switch (phase) {
				case SITING -> "NO DRIVE BELOW: no drive under at least " + MIN_COVER + " m of rock within " + MAX_DEPTH + " m";
				case PILOT -> String.format(Locale.ROOT, "PILOT %d of %d m (%d mm, a metre in %.0f s)", pilot, drive, PILOT_MM, PILOT_SECONDS_PER_M);
				case REAM -> String.format(Locale.ROOT, "REAMING %d of %d m (%d m across, a metre in %.0f s)", reamed, raise(), SHAFT, REAM_SECONDS_PER_M);
				case DONE -> String.format(Locale.ROOT, "DONE: a %d m raise %d m deep; move the borer and take out the collar slab", SHAFT, raise());
			};
		}

		public String save() {
			return phase.name() + ";" + pilot + ";" + drive + ";" + reamed + ";" + into;
		}

		public void load(String text) {
			String[] p = text == null ? new String[0] : text.split(";");
			if (p.length < 5) {
				return;
			}
			try {
				phase = Phase.valueOf(p[0]);
				pilot = Integer.parseInt(p[1]);
				drive = Integer.parseInt(p[2]);
				reamed = Integer.parseInt(p[3]);
				into = Double.parseDouble(p[4]);
			} catch (RuntimeException ex) {
				phase = Phase.SITING;
			}
		}
	}
}
