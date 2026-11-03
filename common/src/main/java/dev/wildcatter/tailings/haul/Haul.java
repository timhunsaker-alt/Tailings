package dev.wildcatter.tailings.haul;

/**
 * Capacities and timings. A real Granby car is about 3 t. The game car holds nine stacks (576 items) so one advance
 * of the drill, up to 64 blocks, fits in a single car with room, and a 10-car train clears a long cut before the dump.
 */
public final class Haul {
	public static final int REAL_CAR_KG = 3_000;
	public static final int CAR_STACKS = 9;
	public static final int STACK = 64;
	public static final int CAR_ITEMS = CAR_STACKS * STACK;
	public static final int MAX_CARS = 10;
	/** One 10-car train. A second train waits rather than tipping into a full pocket. */
	public static final int POCKET_ITEMS = MAX_CARS * CAR_ITEMS;
	public static final int BIN_ITEMS = 8_192;
	/** How many items the drill hands a waiting car in one tick, one face of muck. */
	public static final int TRANSFER = 64;
	/** About 3 m/s on the flat: one block every 7 ticks. The decline is half that. */
	public static final int FLAT_TICKS = 7;
	public static final int DECLINE_TICKS = 14;
	public static final int RECTIFIER_WATTS = 300_000;

	private Haul() {
	}
}
