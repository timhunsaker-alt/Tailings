package dev.wildcatter.tailings.plant;

/**
 * The plant's rates and holds (docs/multiblocks/crushing-circuit.md, "Size: 50 t/h"). A figure marked (proposed) here is marked so in
 * the spec, or is a game choice listed in ROADMAP Known gaps. Plain Java.
 */
public final class PlantRates {
	private PlantRates() {
	}

	/** The plant's rate: 50 t/h of rock, a small underground mine. */
	public static final double TONNES_PER_HOUR = 50;
	public static final double KG_PER_SECOND = TONNES_PER_HOUR * 1000 / 3600;
	public static final double KG_PER_TICK = KG_PER_SECOND / 20;
	/** One block or rock item: a cubic metre at 2.7 t. */
	public static final double BLOCK_KG = 2700;

	/** A dry part holds one block of rock (proposed), so a whole block can be handed in. */
	public static final double DRY_HOLD_KG = BLOCK_KG;
	/** A belt holds about 30 s of its run's flow (proposed). */
	public static final double BELT_HOLD_KG = 30 * KG_PER_SECOND;
	/** A troughed belt runs at 2 m/s. */
	public static final double BELT_METRES_PER_SECOND = 2;
	/** One conveyor drive (7.5 kW) moves this many metres of belt; a longer run needs more drives on it (proposed). */
	public static final int METRES_PER_DRIVE = 30;
	/** An ore bin holds 8,192 items (decided as the game size). */
	public static final int BIN_ITEMS = 8192;

	/** Of unbroken rock, the share under 150 mm that falls through the grizzly's bars past the jaw (proposed). */
	public static final double GRIZZLY_UNDERSIZE = 0.30;
	/** Of the jaw's crushed rock, the share the screen passes as sand under 6 mm (proposed). */
	public static final double SCREEN_SAND_FROM_CRUSHED = 0.15;
	/** Of the cone's gravel, the share the screen passes as sand (proposed); the rest goes round again. */
	public static final double SCREEN_SAND_FROM_GRAVEL = 0.40;
	/** The screen and cone see the circulating oversize as well as new feed: their capacity over the plant rate (proposed). */
	public static final double SCREEN_CAPACITY = 3;
	public static final double CONE_CAPACITY = 2.5;

	/** The cyclones' circulating load, 250 %: the mill grinds 3.5 kg for each kg of new feed. */
	public static final double CIRCULATING_LOAD = 2.5;
	/** The mill discharges at 70 % solids (proposed, typical). */
	public static final double MILL_SOLIDS = 0.70;
	/** The overflow, flotation feed, at 35 % solids (proposed, typical). */
	public static final double OVERFLOW_SOLIDS = 0.35;
	/** A slurry part holds about 10 s of its flow (proposed: "a few seconds"). */
	public static final double SLURRY_HOLD_SECONDS = 10;

	/** Litres of water that go with a kg of solids in slurry at {@code solids} (by mass). */
	public static double waterPerKg(double solids) {
		return (1 - solids) / solids;
	}

	/** The drives a run of {@code metres} of belt needs: 7.5 kW for each 30 m, rounded up. */
	public static int drivesFor(int metres) {
		return Math.max(1, (metres + METRES_PER_DRIVE - 1) / METRES_PER_DRIVE);
	}

	/** Bond's law, W = 10 Wi (1/sqrt(P80) - 1/sqrt(F80)) kWh/t, sizes in micrometres. */
	public static double bondKwhPerTonne(double workIndex, double p80, double f80) {
		return 10 * workIndex * (1 / Math.sqrt(p80) - 1 / Math.sqrt(f80));
	}
}
