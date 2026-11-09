package dev.wildcatter.tailings.pit;

/**
 * The bench, the hole and the blast in numbers (docs/multiblocks/open-pit-p3-blasting.md): a 270 mm hole drilled 16 m on a 14 m bench,
 * 7 x 8 m staggered, 4 m of stemming over a 12 m column of gassed emulsion at 1.2 g/cm³. Plain Java, unit-tested ({@code BlastingTest}).
 */
public final class Blasting {
	/** The game's only bit, 270 mm (the 271 class). */
	public static final double HOLE_DIAMETER_M = 0.270;
	/** One pass of the 271 class; deeper needs rod changes, left out. */
	public static final double SINGLE_PASS_M = 16.8;
	/** Gassed emulsion in the hole, and the matrix before gassing, kg/m³ (proposed; the matrix figure unverified). */
	public static final double GASSED_DENSITY = 1_200;
	public static final double MATRIX_DENSITY = 1_350;
	/** A metre of 270 mm hole holds 68.7 kg at 1.2 g/cm³: the spec's 69, rounded as it is. */
	public static final int KG_PER_M = (int) Math.round(Math.PI * (HOLE_DIAMETER_M / 2) * (HOLE_DIAMETER_M / 2) * GASSED_DENSITY);
	/** Rock and ore, t a bank m³ ({@code PlantRates.BLOCK_KG}). */
	public static final double ROCK_T_PER_M3 = PitMass.ROCK_KG / 1000.0;

	/** The defaults (decision P3-3) and the screen's limits (proposed). */
	public static final int BENCH = 14, BENCH_MIN = 6, BENCH_MAX = 15;
	public static final int SUBDRILL = 2, SUBDRILL_MIN = 0, SUBDRILL_MAX = 3;
	public static final int BURDEN = 7, BURDEN_MIN = 4, BURDEN_MAX = 10;
	public static final int SPACING = 8, SPACING_MIN = 4, SPACING_MAX = 12;
	public static final int STEMMING = 4, STEMMING_MIN = 2, STEMMING_MAX = 8;
	public static final int ROWS_MAX = 10, COLUMNS_MAX = 20;

	/** The drill: 1 m every 10 s in stone, 15 s in deepslate, 20 s to set up at each hole (scaled, approved); trams at 1.5 km/h. */
	public static final int TICKS_PER_M = 200;
	public static final int TICKS_PER_M_DEEPSLATE = 300;
	public static final int SETUP_TICKS = 400;
	public static final double TRAM_MPS = 1.5 / 3.6;
	/** It turns on the spot at 9 degrees a second (proposed: a crawler counter-rotating its tracks). */
	public static final double PIVOT_DEGREES_PER_TICK = 9.0 / 20;
	/** Its fuel: drilling at 70 % of rated, tramming at 40 % (proposed); the tank 1,500 L. */
	public static final double DRILLING = 0.70, TRAMMING = 0.40;
	public static final double DRILL_TANK_L = 1_500;

	/** Cuttings: the hole's volume less 25 % lost to the dust collector, bulked 1.0. Gravel: 1 block = 1 m³ less the same 25 %. */
	public static final double CUTTINGS_KEPT = 0.75;

	/** The emulsion truck: 12 t of matrix, 20 kg/s (scaled 4x), a 12 m hose, a 400 L tank, 60 s to gas (all proposed). */
	public static final double TRUCK_MATRIX_KG = 12_000;
	public static final double PUMP_KG_PER_TICK = 20.0 / 20;
	public static final double HOSE_REACH = 12;
	public static final double TRUCK_TANK_L = 400;
	public static final int SLEEP_TICKS = 60 * 20;
	/** The pump's share of the engine while pumping (proposed: the hydraulic drive to the matrix pump). */
	public static final double PUMPING = 0.10;
	/** The fill point: 1 t a minute into a truck stopped within 6 m (proposed). */
	public static final double FILL_KG_PER_TICK = 1_000.0 / 60 / 20;
	public static final double FILL_REACH = 6;

	/** The blast's limits (proposed). */
	public static final double UNDERCHARGED_KG_PER_T = 0.15;
	public static final double OVERCHARGED_KG_PER_T = 1.0;
	public static final double BLOWOUT_STEMMING_M = 2.0;
	public static final int BLOWOUT_FLYROCK = 6, SHORT_BURDEN_FLYROCK = 3, OVERCHARGE_FLYROCK = 3;
	public static final double FLYROCK_RANGE = 100, FLYROCK_DAMAGE = 10;
	public static final double HARM_RADIUS = 50, HARM_AT_HOLE = 30;
	/** The firing point: at least 50 m from every hole, within 500 m (the lead line). */
	public static final double FIRING_MIN = 50, FIRING_MAX = 500;
	public static final int SIREN_TICKS = 10 * 20, ARMED_TICKS = 30 * 20, MISFIRE_WAIT_TICKS = 30 * 20;
	public static final int ROW_DELAY_MS = 65, HOLE_DELAY_MS = 25, DELAY_MAX_MS = 15_000;
	public static final int LOGGER_HOLES = 200;
	/** Each tick of the shot is 50 ms. */
	public static final int MS_PER_TICK = 50;

	private Blasting() {
	}

	/** Hole depth: bench plus subdrill, at most one pass. */
	public static int depth(int bench, int subdrill) {
		return (int) Math.min(Math.floor(SINGLE_PASS_M), bench + subdrill);
	}

	/** The column charged in a hole of {@code depth} with {@code stemming} metres left at the top. */
	public static int column(int depth, int stemming) {
		return Math.max(0, depth - stemming);
	}

	public static int chargeKg(int column) {
		return column * KG_PER_M;
	}

	/** One hole's block: burden x spacing x bench, bank tonnes. */
	public static double boxTonnes(int burden, int spacing, int bench) {
		return burden * spacing * bench * ROCK_T_PER_M3;
	}

	public static double powderFactor(double kg, double tonnes) {
		return tonnes <= 0 ? 0 : kg / tonnes;
	}

	/** A hole's volume, m³. */
	public static double holeVolume(double metres) {
		return Math.PI * (HOLE_DIAMETER_M / 2) * (HOLE_DIAMETER_M / 2) * metres;
	}

	/** How much stemming a hole's own cuttings make, metres of hole. */
	public static double cuttingsStemming(double depth) {
		return depth * CUTTINGS_KEPT;
	}

	/** How much hole one block of gravel stems: 13 m. */
	public static double gravelStemming() {
		return CUTTINGS_KEPT / holeVolume(1);
	}

	/** A hole's delay: row x row delay + hole in row x hole delay, held to the detonator's 15,000 ms. */
	public static int delayMs(int row, int hole, int rowMs, int holeMs) {
		return Math.max(0, Math.min(DELAY_MAX_MS, row * rowMs + hole * holeMs));
	}

	/** What the charge counts when fired: half until the matrix has gassed (decision P3-10). */
	public static double effectiveKg(double kg, long chargedTick, long nowTick) {
		return nowTick - chargedTick < SLEEP_TICKS ? kg / 2 : kg;
	}

	/** Harm to a player or mob {@code r} metres from the nearest firing hole: 30 x (1 - r / 50), 0 beyond. */
	public static double harm(double r) {
		return r >= HARM_RADIUS ? 0 : HARM_AT_HOLE * (1 - Math.max(0, r) / HARM_RADIUS);
	}

	/** How a hole fires. */
	public enum Outcome {
		GOOD, BLOWOUT, OVERCHARGED, UNDERCHARGED, NO_PRIMER
	}

	/** A hole's outcome from its primer, charge (kg as it counts), box and stemming. Undercharged and no primer are misfires. */
	public static Outcome outcome(boolean primed, double kg, double boxTonnes, double stemming) {
		if (!primed) {
			return Outcome.NO_PRIMER;
		}
		double pf = powderFactor(kg, boxTonnes);
		if (pf < UNDERCHARGED_KG_PER_T) {
			return Outcome.UNDERCHARGED;
		}
		if (stemming < BLOWOUT_STEMMING_M) {
			return Outcome.BLOWOUT;
		}
		return pf > OVERCHARGED_KG_PER_T ? Outcome.OVERCHARGED : Outcome.GOOD;
	}

	/** Every 3 bank blocks break to 4 of muck: the extra blocks a shot of {@code bank} blocks spills. */
	public static int swell(int bank) {
		return bank / 3;
	}

	/** Flyrock's flight on a 45 degree arc to {@code range} metres: ticks in the air. */
	public static int flightTicks(double range) {
		double v = Math.sqrt(PitDrive.G * range);
		return (int) Math.ceil(Math.sqrt(2) * v / PitDrive.G * 20);
	}
}
