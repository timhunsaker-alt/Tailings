package dev.wildcatter.tailings.haul;

/**
 * The haulage tuning class (docs/multiblocks/haulage.md): every rating and capacity in one place. A figure marked (proposed) is so
 * marked in the spec. A real Granby car is about 3 t; the game car holds nine stacks (576 items), so one advance of the drill fits
 * in a single car with room, and a 10-car train clears a long cut before the dump.
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

	/** The 8 t mine locomotive (unverified). */
	public static final double LOCO_KG = 8_000;
	/** A Granby car, about 1 t empty (proposed), 3 t of rock full. */
	public static final double CAR_EMPTY_KG = 1_000;
	/** Two 37 kW traction motors (unverified). */
	public static final double TRACTION_WATTS = 2 * 37_000;
	/** Top speed on the flat. */
	public static final double MAX_SPEED = 3.0;
	/** Down the decline the rack brake holds it to this. */
	public static final double DOWNHILL_SPEED = 1.0;
	/** Starting and stopping, m/s² (proposed). */
	public static final double ACCELERATION = 0.3;
	/** Rolling resistance, as a share of weight. */
	public static final double ROLLING = 0.01;
	public static final double GRAVITY = 9.81;
	/** Vehicle centres along the track: the models' length, 20 px, until the art pass draws them at their real length. */
	public static final double SPACING = 20.0 / 16.0;
	/** Half a vehicle, from its centre to its end. */
	public static final double HALF_LENGTH = SPACING / 2;
	/** A car couples to a train whose tail is this close behind it (proposed). */
	public static final double COUPLE_REACH = 3.0;
	/** At the end of the track a train waits this long, then reverses (proposed). */
	public static final int END_WAIT_TICKS = 100;

	/** The traction rectifier (unverified): 300 kW, 250 V DC to the conductor rail. */
	public static final int RECTIFIER_WATTS = 300_000;
	public static final double TRACTION_VOLTS = 250.0;

	private Haul() {
	}
}
