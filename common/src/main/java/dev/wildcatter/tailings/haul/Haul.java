package dev.wildcatter.tailings.haul;

/** Capacities and timings from the haulage spec. One item in a car is 1,000 kg, so a 3 t car holds three. */
public final class Haul {
	public static final int CAR_KG = 3_000;
	public static final int ITEM_KG = 1_000;
	public static final int MAX_CARS = 10;
	/** One 10-car train. A second train waits rather than tipping into a full pocket. */
	public static final int POCKET_KG = 30_000;
	public static final int BIN_KG = 64_000;
	/** About 3 m/s on the flat: one block every 7 ticks. The decline is half that. */
	public static final int FLAT_TICKS = 7;
	public static final int DECLINE_TICKS = 14;
	public static final int RECTIFIER_WATTS = 300_000;

	private Haul() {
	}
}
