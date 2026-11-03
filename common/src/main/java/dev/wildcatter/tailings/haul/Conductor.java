package dev.wildcatter.tailings.haul;

/**
 * A fed section of conductor is live along its whole length, so lights and signals can draw from it.
 * It stays walk-safe: nothing on the rail is shocked. A dead rectifier leaves the section cold.
 */
public final class Conductor {
	private Conductor() {
	}

	public static boolean live(boolean fedByRectifier) {
		return fedByRectifier;
	}
}
