package dev.wildcatter.tailings.pit;

import net.minecraft.world.phys.Vec3;

/**
 * How a slide's ground runs (dragline-hand-control.md, The slide): each block lets go, speeds up down the failure plane at about half
 * of gravity, and runs at the slide's 6 m/s (proposed; real slides run at 1-10 m/s) down the face and out over the cut's floor to its
 * cell. The server times each block's arrival by it and the client draws each block where it is by the same rule.
 */
public final class SlideMotion {
	/** The run, blocks a tick: 6 m/s. */
	public static final double SPEED = 0.3;
	/** Speeding up, blocks a tick a tick: about 5 m/s^2, gravity down a steep plane less its friction. */
	public static final double ACCEL = 0.0125;
	/** The distance it takes to reach its run, blocks. */
	private static final double RAMP = SPEED * SPEED / (2 * ACCEL);
	private static final double RAMP_TICKS = SPEED / ACCEL;

	private SlideMotion() {
	}

	public static double distance(Vec3 a, Vec3 b) {
		return a.distanceTo(b);
	}

	/** Ticks to run {@code d} blocks from standing. */
	public static int ticks(double d) {
		return (int) Math.ceil(seconds(d));
	}

	private static double seconds(double d) {
		if (d <= 0) {
			return 0;
		}
		if (d <= RAMP) {
			return Math.sqrt(2 * d / ACCEL);
		}
		return RAMP_TICKS + (d - RAMP) / SPEED;
	}

	/** Blocks run {@code t} ticks after letting go. */
	public static double run(double t) {
		if (t <= 0) {
			return 0;
		}
		if (t <= RAMP_TICKS) {
			return 0.5 * ACCEL * t * t;
		}
		return RAMP + (t - RAMP_TICKS) * SPEED;
	}

	/** Where a block is {@code t} ticks after letting go from {@code from}, past {@code via}, to {@code to}; at {@code to} once there. */
	public static Vec3 at(Vec3 from, Vec3 via, Vec3 to, double t) {
		double d = run(t);
		double first = from.distanceTo(via);
		if (d <= first) {
			return first < 1e-6 ? via : from.lerp(via, d / first);
		}
		double second = via.distanceTo(to);
		double s = second < 1e-6 ? 1 : Math.min(1, (d - first) / second);
		return via.lerp(to, s);
	}
}
