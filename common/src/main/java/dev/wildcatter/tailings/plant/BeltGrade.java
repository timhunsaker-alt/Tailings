package dev.wildcatter.tailings.plant;

import java.util.ArrayList;
import java.util.List;

/**
 * How a belt run on a grade is drawn (docs/multiblocks/crushing-circuit.md, "A belt on a grade"): one straight belt between its steps,
 * not a staircase of flat metres. A run is laid level or a block up or down at each step; this takes the level of each block along
 * the run and gives each block the height of its belt surface at its start, middle and end, above its own floor, so that every block
 * meets the next. Plain Java.
 * <ul>
 * <li>A <b>step</b> is where the run goes a block up or down. The belt passes through the middle of each step, half a block above the
 * lower flat (a flat belt's surface is {@link #FLAT}).</li>
 * <li>Steps the same way no more than {@link #MOST_SPACING} blocks apart are one <b>grade</b>: the belt runs straight from step to
 * step, so steps every 1, 2, 3 or 4 blocks are a 1-in-1, 1-in-2, 1-in-3 or 1-in-4 grade. Uneven spacing gives a slight bend at a
 * step, never a gap.</li>
 * <li>At each end the grade runs on half its spacing (a lone step, one block) into the flat, so it meets the flat at the flat's
 * height. It never reaches past the run's end, or past halfway to the next step of another grade: a peak or a valley keeps a
 * bend.</li>
 * </ul>
 */
public final class BeltGrade {
	/** A flat belt's surface above its block's floor, in blocks (the belt block's shape, 0.75 m). */
	public static final double FLAT = 0.75;
	/** Steps the same way at most this many blocks apart are one grade (1 in 4, about 14°; real belts climb up to about 18°). */
	public static final int MOST_SPACING = 4;
	/** How far a lone step's ramp reaches into the flat either side, in blocks: a 1-in-2 ramp over two blocks. */
	private static final double LONE_REACH = 1.0;

	private BeltGrade() {
	}

	/**
	 * Each block's belt surface, in run order: {start, middle, end} along the run, in blocks above the block's floor. Between those
	 * three it is straight. {@code levels} is each block's y, in run order; neighbours differ by at most one.
	 */
	public static double[][] profile(int[] levels) {
		int n = levels.length;
		List<double[]> steps = new ArrayList<>();   // {boundary, sign}: boundary b lies between block b-1 and block b
		for (int b = 1; b < n; b++) {
			int d = levels[b] - levels[b - 1];
			if (d != 0) {
				steps.add(new double[] {b, Math.signum(d)});
			}
		}
		List<double[][]> grades = new ArrayList<>();   // each grade's points {x, height}, in order along the run
		int i = 0;
		while (i < steps.size()) {
			int j = i;
			while (j + 1 < steps.size() && steps.get(j + 1)[1] == steps.get(i)[1] && steps.get(j + 1)[0] - steps.get(j)[0] <= MOST_SPACING) {
				j++;
			}
			int first = (int) steps.get(i)[0], last = (int) steps.get(j)[0];
			double back = j > i ? (steps.get(i + 1)[0] - first) / 2 : LONE_REACH;
			double ahead = j > i ? (last - steps.get(j - 1)[0]) / 2 : LONE_REACH;
			back = Math.min(back, i > 0 ? (first - steps.get(i - 1)[0]) / 2 : first);
			ahead = Math.min(ahead, j + 1 < steps.size() ? (steps.get(j + 1)[0] - last) / 2 : n - last);
			double[][] points = new double[j - i + 3][];
			points[0] = new double[] {first - back, levels[first - 1] + FLAT};
			for (int k = i; k <= j; k++) {
				int b = (int) steps.get(k)[0];
				points[k - i + 1] = new double[] {b, Math.min(levels[b - 1], levels[b]) + FLAT + 0.5};
			}
			points[j - i + 2] = new double[] {last + ahead, levels[last] + FLAT};
			grades.add(points);
			i = j + 1;
		}
		double[][] out = new double[n][];
		for (int b = 0; b < n; b++) {
			out[b] = new double[] {height(grades, levels[b], b) - levels[b], height(grades, levels[b], b + 0.5) - levels[b], height(grades, levels[b], b + 1) - levels[b]};
		}
		return out;
	}

	/** The belt's height at {@code x} blocks along the run, on a block at {@code level}: on a grade, or flat. */
	private static double height(List<double[][]> grades, int level, double x) {
		for (double[][] points : grades) {
			if (x < points[0][0] || x > points[points.length - 1][0]) {
				continue;
			}
			for (int k = 0; k + 1 < points.length; k++) {
				double[] a = points[k], b = points[k + 1];
				if (x >= a[0] && x <= b[0]) {
					return b[0] - a[0] < 1e-9 ? a[1] : a[1] + (b[1] - a[1]) * (x - a[0]) / (b[0] - a[0]);
				}
			}
		}
		return level + FLAT;
	}

	/** Whether a block's belt is not flat. */
	public static boolean sloped(double[] profile) {
		for (double h : profile) {
			if (Math.abs(h - FLAT) > 1e-6) {
				return true;
			}
		}
		return false;
	}

	/** The belt's surface at {@code u} (0 to 1) along a block with this profile. */
	public static double surface(double h0, double hm, double h1, double u) {
		return u <= 0.5 ? h0 + (hm - h0) * u * 2 : hm + (h1 - hm) * (u * 2 - 1);
	}
}
