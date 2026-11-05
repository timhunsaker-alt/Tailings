package dev.wildcatter.tailings.plant;

/**
 * The plant's pace and liberation. Since R3 a line is not a fixed layout: its parts are joined by touching or by belt runs
 * (docs/multiblocks/crushing-circuit.md), and the jaw or the conditioning tank finds them.
 */
public final class PlantPlan {
	/** One block of feed per second while the motors run, until R4 runs the plant at the real 50 t/h. */
	public static final int CYCLE_TICKS = 20;

	/** Fines are 90% liberated. The game still pays the even 75% ladder, not 90% times the real flotation recovery. */
	public static final double FINES_LIBERATION = 0.90;
	public static final double SAND_LIBERATION = 0.60;

	private PlantPlan() {
	}
}
