package dev.wildcatter.tailings.pit;

/**
 * A trailing cable's heights along its run, a point a metre, over the ground it lies on (open-pit-p5-big-machines.md, P5a; Heavy Iron
 * fix of 2026-11-23). On the flat it lies on the ground. Where the ground drops away by more than {@link #STEP} in a metre (off a crest,
 * down a bench face) it does not run straight down the face: pulled toward its machine, it bends over the lip and hangs clear of the
 * face in a long curve that touches down on the floor beyond the toe, steep at the lip and flat where it lands, as a hanging cable does.
 * A face it climbs (the machine above the switch house) is the same curve the other way. Plain Java, used by the client's
 * {@code PitVehicleRenderer} and checked by the NeoForge scene {@code pit_cable_drape}.
 */
public final class CableDrape {
	/** A step of more than this, metres in one metre along, is a face the cable hangs over. */
	public static final double STEP = 1.5;
	/** How far out from the lip it touches down: this much run for every metre of drop, plus {@link #TOUCH}, at least {@link #MIN_RUN}. */
	public static final double RUN_PER_DROP = 0.8;
	public static final int TOUCH = 2, MIN_RUN = 3;

	private CableDrape() {
	}

	/** The cable's height at each point from the ground's top at each point (same length; the ground itself where it lies on it). */
	public static double[] drape(double[] ground) {
		int n = ground.length;
		double[] out = ground.clone();
		int i = 0;
		while (i < n - 1) {
			if (ground[i] - ground[i + 1] > STEP) {
				// a face going down from the lip at i, past every steep metre after it (a stepped face)
				int foot = i + 1;
				while (foot < n - 1 && ground[foot] - ground[foot + 1] > STEP) {
					foot++;
				}
				int run = Math.max(MIN_RUN, (int) Math.round((ground[i] - ground[foot]) * RUN_PER_DROP) + TOUCH);
				hang(out, ground, i, Math.min(n - 1, i + Math.max(run, foot - i + 1)));
				i = foot;
			} else if (ground[i + 1] - ground[i] > STEP) {
				// a face going up to its lip: the same curve, hanging back from the lip toward where it is
				int lip = i + 1;
				while (lip < n - 1 && ground[lip + 1] - ground[lip] > STEP) {
					lip++;
				}
				int run = Math.max(MIN_RUN, (int) Math.round((ground[lip] - ground[i]) * RUN_PER_DROP) + TOUCH);
				hang(out, ground, lip, Math.max(0, lip - Math.max(run, lip - i + 1)));
				i = lip;
			} else {
				i++;
			}
		}
		return out;
	}

	/**
	 * Hangs the cable from the lip at {@code lip} to where it touches down at {@code touch} (either side): a parabola steepest at the lip
	 * and flat at the touch-down, never under the ground and never lower than a drape already laid there.
	 */
	private static void hang(double[] out, double[] ground, int lip, int touch) {
		int len = Math.abs(touch - lip), dir = touch > lip ? 1 : -1;
		if (len < 2) {
			return;
		}
		double top = ground[lip], low = ground[touch];
		for (int k = 1; k < len; k++) {
			double left = 1 - (double) k / len;
			int at = lip + dir * k;
			out[at] = Math.max(out[at], Math.max(ground[at], low + (top - low) * left * left));
		}
	}

	/** The steepest rise or fall between neighbouring points, metres a metre. */
	public static double steepest(double[] heights) {
		double most = 0;
		for (int i = 0; i + 1 < heights.length; i++) {
			most = Math.max(most, Math.abs(heights[i + 1] - heights[i]));
		}
		return most;
	}
}
