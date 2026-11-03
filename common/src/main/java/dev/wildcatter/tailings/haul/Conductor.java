package dev.wildcatter.tailings.haul;

/**
 * The conductor rail is live only while a locomotive covers it and the rectifier is feeding that rail.
 * A car, a player, or a dead rectifier leaves it cold.
 */
public final class Conductor {
	private Conductor() {
	}

	public static boolean live(boolean fedByRectifier, boolean locomotiveHere) {
		return fedByRectifier && locomotiveHere;
	}
}
