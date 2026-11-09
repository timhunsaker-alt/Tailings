package dev.wildcatter.tailings.pit;

/**
 * How a self-driving truck steers and sets its speed (docs/multiblocks/open-pit.md, P2a). Plain Java, in the truck's own terms: yaw in
 * Minecraft degrees (0 faces +z, 90 faces -x), speed in m/s with minus for reverse.
 */
public final class Autopilot {
	/** Speed for a turn of more than 30 degrees at a stake (approved). */
	public static final double TURN_SPEED = 15 / 3.6;
	public static final double SHARP_TURN_DEGREES = 30;
	/** The gap it keeps to a truck ahead on its route (spec). */
	public static final double FOLLOW_GAP = 50;
	/** It stops for anything within its stopping distance plus 5 m ahead (approved). */
	public static final double OBSTACLE_MARGIN = 5;
	/** On a two-way leg it keeps 4 m right of the stake line (approved). */
	public static final double LANE_OFFSET = 4;
	/** It plans to brake at half the brakes' rate, to stop smoothly. */
	public static final double PLAN_DECEL = PitDrive.BRAKE / 2;

	private Autopilot() {
	}

	/** The yaw that faces from (fromX, fromZ) toward (toX, toZ). */
	public static double yawToward(double fromX, double fromZ, double toX, double toZ) {
		return Math.toDegrees(Math.atan2(-(toX - fromX), toZ - fromZ));
	}

	/** {@code a - b} wrapped to -180..180. */
	public static double wrap(double degrees) {
		double d = degrees % 360;
		if (d > 180) {
			d -= 360;
		} else if (d < -180) {
			d += 360;
		}
		return d;
	}

	/**
	 * The steer, -1 to 1, as the truck's tick reads it (its yaw changes by -steer · speed / radius): forward it points its nose at the
	 * target; in reverse it points its tail at it.
	 */
	public static double steer(double yaw, double x, double z, double targetX, double targetZ, boolean reverse, double speed, double turnRadius) {
		double want = reverse ? yawToward(targetX, targetZ, x, z) : yawToward(x, z, targetX, targetZ);
		double error = Math.toRadians(wrap(want - yaw));
		double v = Math.abs(speed) < 0.5 ? (reverse ? -0.5 : 0.5) : speed;
		// turn the error out over about a second: yaw rate = error, and yaw rate = -steer · v / R
		double steer = -error * turnRadius / v;
		return Math.max(-1, Math.min(1, steer));
	}

	/** The fastest it may go with {@code distance} metres left to stop in. */
	public static double stoppingSpeed(double distance) {
		return distance <= 0 ? 0 : Math.sqrt(2 * PLAN_DECEL * distance);
	}

	/** Its stopping distance from {@code speed}, braking as planned. */
	public static double stoppingDistance(double speed) {
		return speed * speed / (2 * PLAN_DECEL);
	}

	/**
	 * The throttle, 1 drive, -1 brake (or reverse), 0 coast, to bring {@code speed} toward {@code target} going {@code way} (1 forward,
	 * -1 back).
	 */
	public static double throttle(double speed, double target, int way) {
		double along = speed * way;
		if (target <= 0.05) {
			return along > 0.1 ? -way : 0;
		}
		if (along < target - 0.3) {
			return way;
		}
		if (along > target + 0.7) {
			return -way;
		}
		return 0;
	}
}
