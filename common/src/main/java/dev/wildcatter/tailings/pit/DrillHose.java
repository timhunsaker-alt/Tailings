package dev.wildcatter.tailings.pit;

/**
 * The blasthole drill's main air hose (open-pit-p3-blasting.md; Heavy Iron fix of 2026-11-23): the compressor blows air down the rods to
 * lift the cuttings out of the hole, through a hose to the rotary head. It runs up the front of the mast's left leg from the compressor
 * to a clamp at {@link #CLAMP_UP}, then hangs in a loop down and back up into the head's gooseneck. The hose is {@link #LOOP} long from
 * the clamp, so its loop hangs lower as the head comes down the mast, as a real drill's does, never below {@link #LOWEST} (the
 * compressor's top). Points are metres from the machine's centre on the ground: x to its right, y up, z back (the mast at z 4.8-6.6),
 * as {@code BlastholeDrillModel} is drawn. Plain Java, drawn by {@code PitVehicleRenderer}, checked by the scene {@code pit_drill_hose}.
 */
public final class DrillHose {
	/** The head's top at the top of the mast, metres, and how far it comes down (16 m, the hole's depth). */
	public static final double HEAD_TOP = 24.5, HEAD_TRAVEL = 16.0;
	/** The clamp on the mast, metres up, the loop's length from it to the head, and the lowest the loop may hang (on the compressor). */
	public static final double CLAMP_UP = 14.0, LOOP = 16.0, LOWEST = 4.6;
	/** The loop's two legs in front of the mast (x), the plane it hangs in (z), and its bend's radius. */
	public static final double FEED_X = -1.0, HEAD_X = -0.4, FRONT_Z = 4.55, BEND = 0.3;
	/** Where the hose goes into the head: its front face, a little under its top. */
	public static final double INTO_HEAD_Z = 5.4, UNDER_TOP = 0.35;

	private DrillHose() {
	}

	/** The head's top, metres up, at {@code lift} (0 at the top of the mast, 1 at the bottom of a 16 m hole). */
	public static double headTop(double lift) {
		return HEAD_TOP - HEAD_TRAVEL * Math.max(0, Math.min(1, lift));
	}

	/** The bottom of the loop, metres up: the clamp and the head share its length, held off the compressor. */
	public static double loopBottom(double lift) {
		double into = headTop(lift) - UNDER_TOP;
		return Math.max(LOWEST, (CLAMP_UP + into - LOOP) / 2);
	}

	/** The hose's points in order, x y z each: from the compressor up the mast to the clamp, down and round its loop, up into the head. */
	public static double[] points(double lift) {
		double into = headTop(lift) - UNDER_TOP, bottom = loopBottom(lift);
		java.util.List<double[]> p = new java.util.ArrayList<>();
		// off the compressor's top, up the front of the mast's leg to the clamp
		p.add(new double[] {FEED_X, 4.05, 4.2});
		p.add(new double[] {FEED_X, 4.4, FRONT_Z});
		for (double y = 5.5; y < CLAMP_UP; y += 1.5) {
			p.add(new double[] {FEED_X, y, FRONT_Z});
		}
		p.add(new double[] {FEED_X, CLAMP_UP, FRONT_Z});
		// down from the clamp to the loop's bend
		double bendY = bottom + BEND;
		for (double y = CLAMP_UP - 1; y > bendY; y -= 1) {
			p.add(new double[] {FEED_X, y, FRONT_Z});
		}
		// round the bend: a half circle from the feed's leg to the head's
		double cx = (FEED_X + HEAD_X) / 2, r = Math.abs(HEAD_X - FEED_X) / 2;
		for (int k = 0; k <= 8; k++) {
			double a = Math.PI * k / 8;
			p.add(new double[] {cx - r * Math.cos(a), bendY - BEND * Math.sin(a), FRONT_Z - 0.05 * Math.sin(a)});
		}
		// up to the head and into its gooseneck
		for (double y = bendY + 1; y < into - 0.6; y += 1) {
			p.add(new double[] {HEAD_X, y, FRONT_Z});
		}
		p.add(new double[] {HEAD_X, into - 0.3, FRONT_Z + 0.15});
		p.add(new double[] {HEAD_X, into, INTO_HEAD_Z});
		double[] out = new double[p.size() * 3];
		for (int i = 0; i < p.size(); i++) {
			System.arraycopy(p.get(i), 0, out, i * 3, 3);
		}
		return out;
	}

	/** The hose's length from the clamp to the head along its points (it should stay about {@link #LOOP} while the loop hangs free). */
	public static double loopLength(double[] points) {
		double len = 0;
		boolean past = false;
		for (int i = 0; i + 5 < points.length; i += 3) {
			if (!past && points[i + 1] >= CLAMP_UP - 1e-9 && points[i] == FEED_X) {
				past = true;
			}
			if (past) {
				len += Math.sqrt(Math.pow(points[i + 3] - points[i], 2) + Math.pow(points[i + 4] - points[i + 1], 2) + Math.pow(points[i + 5] - points[i + 2], 2));
			}
		}
		return len;
	}
}
