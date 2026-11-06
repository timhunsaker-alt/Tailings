package dev.wildcatter.tailings.haul;

/**
 * How fast a train runs (docs/multiblocks/haulage.md, "Speed follows power"): the force to hold it on the grade is
 * F = m g (sin θ + rolling), and its speed is the power over that force, capped at 3 m/s on the flat; down the decline the rack
 * brake holds it to 1 m/s. It speeds up and slows down at 0.3 m/s². Plain Java.
 */
public final class Traction {
	private Traction() {
	}

	/** The speed limit for {@code kg} on a grade of {@code sine} (positive uphill) with {@code watts} at the wheels. */
	public static double limit(double kg, double sine, double watts) {
		if (sine < -1e-9) {
			return Haul.DOWNHILL_SPEED;
		}
		double force = kg * Haul.GRAVITY * (sine + Haul.ROLLING);
		return Math.min(Haul.MAX_SPEED, watts / force);
	}

	/** The power to hold {@code speed} on that grade (none going down; the rack brake holds it). */
	public static double watts(double kg, double sine, double speed) {
		return Math.max(0, kg * Haul.GRAVITY * (sine + Haul.ROLLING) * speed);
	}

	/** The speed after {@code seconds}, moving toward {@code target} at the train's acceleration. */
	public static double approach(double speed, double target, double seconds) {
		double step = Haul.ACCELERATION * seconds;
		return speed < target ? Math.min(target, speed + step) : Math.max(target, speed - step);
	}

	/** The fastest it can go and still stop within {@code metres}. */
	public static double stoppable(double metres) {
		return Math.sqrt(2 * Haul.ACCELERATION * Math.max(0, metres));
	}

	/** How far it runs before it stops from {@code speed}. */
	public static double brakingDistance(double speed) {
		return speed * speed / (2 * Haul.ACCELERATION);
	}

	/** A train's mass: the locomotive, its cars and their loads (a full car is 3 t of rock). */
	public static double mass(int cars, int loadedItems) {
		return Haul.LOCO_KG + cars * Haul.CAR_EMPTY_KG + (double) loadedItems / Haul.CAR_ITEMS * Haul.REAL_CAR_KG;
	}
}
