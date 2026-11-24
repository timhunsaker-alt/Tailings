package dev.wildcatter.tailings.shaft;

import java.util.ArrayList;
import java.util.List;

/**
 * Where the rock and the ropes run, in the parts' own frames (x east, y up, z south, metres from the main cell's corner, as
 * tools/models/shaft.py draws them). Plain Java, unit-tested; the server's particles, the renderers and the tests all use it
 * (fix3-hi07: the pour ran under the chute, and both ropes ended in the gap between the drums).
 */
public final class ShaftGeometry {
	private ShaftGeometry() {
	}

	// ---- the pour at the dump ---------------------------------------------------------------------------------------------------

	/** The dump chute in the headframe (shaft.py stepped(2.0, 7.0, 17.2, 15.6, 3.0, 0.0, 4, 0.25)): its steps' tops, south to north. */
	public static final double CHUTE_SOUTH = 3.0, CHUTE_LIP = 0.0;
	private static final int CHUTE_STEPS = 4;
	private static final double CHUTE_Y_START = 17.2, CHUTE_Y_END = 15.6, CHUTE_THICK = 0.25;
	/** The scroll that catches the door's roller: z 3.0 to 3.2, its top 17.6 m up (shaft.py). */
	public static final double SCROLL_Z0 = 3.0, SCROLL_Z1 = 3.2, SCROLL_TOP = 17.6;
	/** The skip's bottom door, open: hinged at its south edge under the skip's floor, its lip resting just over the scroll. */
	public static final double DOOR_HINGE_Z = 4.9, DOOR_HINGE_Y = Hoisting.DUMP_HEIGHT, DOOR_LIP_Z = 3.1, DOOR_LIP_Y = 17.68;
	/** The stream of rock: a sheet this thick, m, its centre half that over what it slides on. */
	public static final double STREAM_THICK = 0.3;

	/** The top of the chute (or the scroll at its head) at {@code z}; minus infinity off its ends. */
	public static double chuteTop(double z) {
		double top = Double.NEGATIVE_INFINITY;
		double len = (CHUTE_SOUTH - CHUTE_LIP) / CHUTE_STEPS;
		for (int i = 0; i < CHUTE_STEPS; i++) {
			double za = CHUTE_SOUTH - len * i, zb = CHUTE_SOUTH - len * (i + 1);
			if (z <= za + 1e-9 && z >= zb - 1e-9) {
				top = Math.max(top, CHUTE_Y_START + (CHUTE_Y_END - CHUTE_Y_START) * (i + 0.5) / CHUTE_STEPS + CHUTE_THICK);
			}
		}
		if (z >= SCROLL_Z0 - 1e-9 && z <= SCROLL_Z1 + 1e-9) {
			top = Math.max(top, SCROLL_TOP);
		}
		return top;
	}

	/** The line over the steps' noses (their north edges), where the rock slides down the chute: y at {@code z}. */
	public static double noseLine(double z) {
		double len = (CHUTE_SOUTH - CHUTE_LIP) / CHUTE_STEPS;
		double lipTop = CHUTE_Y_START + (CHUTE_Y_END - CHUTE_Y_START) * (CHUTE_STEPS - 0.5) / CHUTE_STEPS + CHUTE_THICK;
		double rise = (CHUTE_Y_END - CHUTE_Y_START) / CHUTE_STEPS * -1 / len;
		return lipTop + rise * (z - CHUTE_LIP);
	}

	/** The open door's top at {@code z}, between its hinge and its lip. */
	public static double doorTop(double z) {
		return DOOR_LIP_Y + (DOOR_HINGE_Y - DOOR_LIP_Y) * (z - DOOR_LIP_Z) / (DOOR_HINGE_Z - DOOR_LIP_Z);
	}

	/** The parabola's segments from the lip to the bin. */
	public static final int FALL_SEGMENTS = 14;

	/**
	 * The stream's centre line, in the headframe's frame, out of a skip whose centre is {@code skipX} at the dump: out of the skip over
	 * its open door, down the chute over the steps' noses, off the lip at z 0, and in a parabola into the bin's mouth at
	 * ({@code binX}, {@code binY}, {@code binZ}). The fall leaves the lip at the chute's slope and lands in the middle of the mouth.
	 * Points {x, y, z}; the first {@link #lipIndex()} + 1 lie on the skip, door and chute.
	 */
	public static List<double[]> pourPath(double skipX, double binX, double binY, double binZ) {
		double half = STREAM_THICK / 2;
		List<double[]> out = new ArrayList<>();
		out.add(new double[] {skipX, doorTop(4.5) + half, 4.5});
		out.add(new double[] {skipX, DOOR_LIP_Y + half, DOOR_LIP_Z});
		out.add(new double[] {skipX, noseLine(CHUTE_SOUTH) + half, CHUTE_SOUTH});
		out.add(new double[] {skipX, noseLine(CHUTE_LIP) + half, CHUTE_LIP});
		double lx = skipX, ly = noseLine(CHUTE_LIP) + half, lz = CHUTE_LIP;
		double dx = binX - lx, dz = binZ - lz, d = Math.sqrt(dx * dx + dz * dz);
		double slope = (CHUTE_Y_START - CHUTE_Y_END) / (CHUTE_SOUTH - CHUTE_LIP);
		if (d < 1e-6) {
			out.add(new double[] {binX, binY, binZ});
			return out;
		}
		// y = ly - s d t - k (d t)^2, through the mouth at t = 1; k >= 0 (falling), else a straight line
		double k = (ly - binY - slope * d) / (d * d);
		double s = slope;
		if (k < 0) {
			k = 0;
			s = (ly - binY) / d;
		}
		for (int i = 1; i <= FALL_SEGMENTS; i++) {
			double t = (double) i / FALL_SEGMENTS, h = d * t;
			out.add(new double[] {lx + dx * t, ly - s * h - k * h * h, lz + dz * t});
		}
		return out;
	}

	/** The index in {@link #pourPath} of the chute's lip, where the rock leaves the chute and falls free. */
	public static int lipIndex() {
		return 3;
	}

	/**
	 * The rock's speed off the lip at {@code lip} for a particle to follow the parabola to {@code end} (both {x, y, z}, in any frame
	 * with y up): {x, y, z} in blocks a tick, under a particle's gravity of {@code g} blocks a tick squared.
	 */
	public static double[] lipVelocity(double[] lip, double[] end, double g) {
		double dx = end[0] - lip[0], dz = end[2] - lip[2], d = Math.sqrt(dx * dx + dz * dz), drop = lip[1] - end[1];
		double slope = (CHUTE_Y_START - CHUTE_Y_END) / (CHUTE_SOUTH - CHUTE_LIP);
		double fallBy = drop - slope * d;
		if (d < 1e-6 || fallBy <= 0) {
			return new double[] {0, -0.2, 0};
		}
		// d / v ticks to cover d; in that time it falls slope d + g/2 (d/v)^2
		double v = d * Math.sqrt(g / 2 / fallBy);
		return new double[] {v * dx / d, -slope * v, v * dz / d};
	}

	// ---- the ropes on the drums ---------------------------------------------------------------------------------------------------

	/**
	 * The hoist house's two drums (shaft.py): along x, 0.8 to 4.3 and 5.3 to 8.8; their centre 3.4 m up and 3 m south; 3 m across. The
	 * radius here is the barrel's outer face, where the rope lies: shaft.py's ring of plates 0.2 thick centred on 1.4 m.
	 */
	public static final double[][] HOIST_DRUMS = {{0.8, 4.3}, {5.3, 8.8}};
	public static final double HOIST_DRUM_Y = 3.4, HOIST_DRUM_Z = 3.0, HOIST_DRUM_R = 1.4 + 0.1;
	/** The cage winder's drum (shaft.py): x 2 to 5, its centre 2.25 m up and 2.5 m south; 2.5 m across, its plates 0.18 thick. */
	public static final double[] CAGE_DRUM = {2.0, 5.0};
	public static final double CAGE_DRUM_Y = 2.25, CAGE_DRUM_Z = 2.5, CAGE_DRUM_R = Hoisting.CAGE_DRUM_DIAMETER / 2 + 0.09;
	/** The rope's own radius, m (drawn 10 cm thick): its centre lies this far outside the barrel, clear of its 16 plates' corners. */
	public static final double ROPE_R = 0.06;
	/** The coil's pitch, m a wrap (the rope laid beside itself), and the dead wraps always left on a drum. */
	public static final double PITCH = 0.09, DEAD_WRAPS = 3;
	/** Room between the coil and a flange, m. */
	private static final double FLANGE_GAP = 0.05;

	/** The rope wound on a drum of radius {@code r} with its conveyance {@code height} m above its bottom stop: the dead wraps and the rest. */
	public static double wound(double height, double r) {
		return DEAD_WRAPS * 2 * Math.PI * (r + ROPE_R) + Math.max(0, height);
	}

	/**
	 * Where a rope's coil lies along a drum from {@code x0} to {@code x1} whose sheave stands at {@code sheaveX} along its axis: it
	 * starts at the flange farther from the sheave and lays toward the other. {start flange's inner face, direction (+1 or -1), the
	 * coil's front, where the rope leaves it}.
	 */
	public static double[] coil(double x0, double x1, double sheaveX, double wound, double r) {
		boolean fromWest = Math.abs(sheaveX - x0) >= Math.abs(sheaveX - x1);
		double start = fromWest ? x0 + FLANGE_GAP : x1 - FLANGE_GAP, dir = fromWest ? 1 : -1;
		double wraps = wound / (2 * Math.PI * (r + ROPE_R));
		double front = start + dir * Math.min(Math.abs(x1 - x0) - 2 * FLANGE_GAP, wraps * PITCH);
		return new double[] {start, dir, front};
	}

	/**
	 * The point where a rope from {@code (pu, pv)} touches a circle of radius {@code r} at {@code (cu, cv)}, both in the drum's cross
	 * section (u across it, v up): over its top or under its bottom. Null when the point is inside the circle.
	 */
	public static double[] tangent(double pu, double pv, double cu, double cv, double r, boolean over) {
		double du = pu - cu, dv = pv - cv, d = Math.sqrt(du * du + dv * dv);
		if (d <= r) {
			return null;
		}
		double toP = Math.atan2(dv, du), off = Math.acos(r / d);
		double a1 = toP + off, a2 = toP - off;
		double v1 = cv + r * Math.sin(a1), v2 = cv + r * Math.sin(a2);
		double a = over == (v1 >= v2) ? a1 : a2;
		return new double[] {cu + r * Math.cos(a), cv + r * Math.sin(a)};
	}

	/**
	 * A rope's end on its drum, in the drum's part's frame, from the sheave's top at ({@code sx}, {@code sy}, {@code sz}) in the same
	 * frame: the coil's front along the axis, the tangent point in the cross section. {x, y, z}; null for a sheave inside the drum.
	 */
	public static double[] ropeEnd(double x0, double x1, double cy, double cz, double r, double sx, double sy, double sz, double wound, boolean over) {
		double rr = r + ROPE_R;
		double[] t = tangent(sz, sy, cz, cy, rr, over);
		if (t == null) {
			return null;
		}
		double[] c = coil(x0, x1, sx, wound, r);
		return new double[] {c[2], t[1], t[0]};
	}
}
