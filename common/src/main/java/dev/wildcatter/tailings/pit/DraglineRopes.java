package dev.wildcatter.tailings.pit;

import org.jspecify.annotations.Nullable;

/**
 * The dragline's levers (docs/multiblocks/dragline-hand-control.md, Part 1): where its bucket hangs on its two ropes, how fast swing,
 * hoist and drag move, their limits, and how its generator sets share 6 MW between them. Plain Java, unit-tested.
 *
 * <p>Everything is in the boom's plane: {@code out} metres from the tub's centre and {@code up} metres over its floor (minus is down in
 * the cut). The hoist rope runs from the boom point, the drag rope from the fairlead at the house's front; the bucket hangs where they
 * meet. With the hoist slacker than that, the bucket rests on the ground at the length its drag rope allows.
 */
public final class DraglineRopes {
	/** The boom point: 75 m at 32 degrees from its foot 8.5 m ahead of the centre (dragline.md, As built, 7): 72 m out, 44 m up. */
	public static final double POINT_OUT = 72, POINT_UP = 44;
	/** The drag rope's fairlead at the house's front, a metre up. */
	public static final double FAIR_OUT = 8.5, FAIR_UP = 1;
	/** The hoist's limit switch stops the bucket this far under the point (no two-blocking). */
	public static final double HOIST_LIMIT = 4;
	/** The drag stops the bucket this far from the centre (it would hit the tub). */
	public static final double DRAG_LIMIT_OUT = 15;
	/** Its reach and its dig depth under the tub, as the job (dragline.md). */
	public static final double REACH = Dragline.REACH_M, DEPTH = Dragline.DIG_DEPTH_M;

	/** Swing: up to 9 degrees a second (1.5 rpm), speeding up and braking at 1 degree a second squared (proposed). */
	public static final double SWING_MAX = 9, SWING_ACCEL = 1;
	/** Rope speeds, m/s (proposed): hoist empty and loaded, drag free and through the bank (25 bank blocks in 15 s over 5 m). */
	public static final double HOIST_EMPTY = 2.5, HOIST_LOADED = 1.5, DRAG_FREE = 2.5, DRAG_DIG = 1.0 / 3;

	/** What each motion draws at full speed, and the generator sets' most (spec). */
	public static final double DIG_WATTS = 4_800_000, HOIST_LOADED_WATTS = 4_800_000, LIGHT_WATTS = 1_200_000, SWING_WATTS = 1_200_000;
	public static final double CAP_WATTS = Dragline.PEAK_WATTS, STAND_WATTS = Dragline.STANDING_WATTS;

	/** Where the bucket is, and whether it rests on the ground. */
	public record Bucket(double out, double up, boolean grounded) {
	}

	private DraglineRopes() {
	}

	public static double hoistLength(double out, double up) {
		return Math.hypot(POINT_OUT - out, POINT_UP - up);
	}

	public static double dragLength(double out, double up) {
		return Math.hypot(out - FAIR_OUT, up - FAIR_UP);
	}

	/**
	 * The bucket on a hoist rope {@code hoist} long and a drag rope {@code drag} long, the ground {@code ground} up under it: where the
	 * ropes meet (the lower meeting), or, if that is under the ground, on the ground where its drag rope reaches. Null if the ropes
	 * cannot meet.
	 */
	public static @Nullable Bucket hang(double hoist, double drag, double ground) {
		double dx = POINT_OUT - FAIR_OUT, dy = POINT_UP - FAIR_UP;
		double d = Math.hypot(dx, dy);
		if (d > hoist + drag + 1e-9 || d < Math.abs(hoist - drag) - 1e-9) {
			return onGround(drag, ground, hoist);
		}
		double a = (drag * drag - hoist * hoist + d * d) / (2 * d);
		double h = Math.sqrt(Math.max(0, drag * drag - a * a));
		double bx = FAIR_OUT + a * dx / d, by = FAIR_UP + a * dy / d;
		// the two meetings either side of the line fairlead-point; the lower one is where a bucket hangs
		double x1 = bx - h * dy / d, y1 = by + h * dx / d;
		double x2 = bx + h * dy / d, y2 = by - h * dx / d;
		double out = y1 < y2 ? x1 : x2, up = Math.min(y1, y2);
		if (up < ground) {
			return onGround(drag, ground, hoist);
		}
		return new Bucket(out, up, false);
	}

	/** The bucket on the ground where its drag rope reaches, if its hoist is long enough to let it lie there. */
	private static @Nullable Bucket onGround(double drag, double ground, double hoist) {
		double rise = ground - FAIR_UP;
		if (drag < Math.abs(rise)) {
			return null;
		}
		double out = FAIR_OUT + Math.sqrt(drag * drag - rise * rise);
		if (hoistLength(out, ground) > hoist + 1e-6) {
			return null;   // the hoist will not let it down that far
		}
		return new Bucket(out, ground, true);
	}

	/** True where the bucket may be: inside its limits. */
	public static boolean allowed(double out, double up) {
		return out >= DRAG_LIMIT_OUT - 1e-6 && out <= REACH + 1e-6 && up <= POINT_UP - HOIST_LIMIT + 1e-6 && up >= -DEPTH - 1e-6;
	}

	/**
	 * The bucket after the hoist pays out {@code dHoist} metres (minus winds in) and the drag {@code dDrag} (minus winds in), the ground
	 * {@code ground} up under it. A move that would pass a limit is refused, each rope apart: so at the hoist limit the drag still works.
	 */
	public static Bucket move(Bucket from, double dHoist, double dDrag, double ground) {
		double hoist = hoistLength(from.out(), from.up()), drag = dragLength(from.out(), from.up());
		if (from.grounded() && dHoist > 0) {
			dHoist = 0;   // lying on the ground the hoist is slack already: paying it out moves nothing
		}
		Bucket both = hang(hoist + dHoist, drag + dDrag, ground);
		if (both != null && allowed(both.out(), both.up())) {
			return both;
		}
		Bucket h = dHoist == 0 ? null : hang(hoist + dHoist, drag, ground);
		if (h != null && allowed(h.out(), h.up())) {
			return h;
		}
		Bucket g = dDrag == 0 ? null : hang(hoist, drag + dDrag, ground);
		if (g != null && allowed(g.out(), g.up())) {
			return g;
		}
		Bucket still = hang(hoist, drag, ground);
		return still != null ? still : from;
	}

	/**
	 * The swing after one step of {@code dt} seconds: {@code rate} degrees a second now, {@code key} -1 (left), 0 or 1 (right), its
	 * speeds scaled by {@code scale} (the power share). Pressing the way it turns speeds it up; the other way brakes twice as hard; no
	 * key brakes it to a stop at its acceleration, so it coasts. Returns {angle moved, new rate}.
	 */
	public static double[] swing(double rate, int key, double dt, double scale) {
		double accel = SWING_ACCEL * scale;
		double next;
		if (key == 0) {
			next = Math.abs(rate) <= accel * dt ? 0 : rate - Math.signum(rate) * accel * dt;
		} else if (rate * key < 0) {
			next = rate + key * 2 * accel * dt;
			if (next * key > 0) {
				next = 0;   // braked to a stop this step; it speeds up the other way from the next
			}
		} else {
			next = Math.min(SWING_MAX * scale, Math.abs(rate + key * accel * dt)) * key;
		}
		return new double[] {(rate + next) / 2 * dt, next};
	}

	/** How far a swing at {@code rate} coasts to a stop with no key, degrees: 40 from full speed. */
	public static double coast(double rate) {
		return rate * rate / (2 * SWING_ACCEL);
	}

	/**
	 * What the motions asked for draw together, watts, before the share: dragging through the bank, hoisting (loaded or not), any other
	 * rope moving, and the swing speeding up.
	 */
	public static double asked(boolean digging, boolean dragging, int hoist, boolean loaded, boolean swingPushing) {
		double w = digging ? DIG_WATTS : dragging ? LIGHT_WATTS : 0;
		if (hoist < 0) {
			w += loaded ? HOIST_LOADED_WATTS : LIGHT_WATTS;
		} else if (hoist > 0) {
			w += LIGHT_WATTS;
		}
		return w + (swingPushing ? SWING_WATTS : 0);
	}

	/** The share: every motion slows in proportion when they ask more than the sets give. */
	public static double share(double asked) {
		return asked <= CAP_WATTS ? 1 : CAP_WATTS / asked;
	}

	/** What it draws for {@code asked}: never more than the sets' 6 MW, never less than standing. */
	public static double draw(double asked) {
		return Math.max(STAND_WATTS, Math.min(CAP_WATTS, asked));
	}
}
