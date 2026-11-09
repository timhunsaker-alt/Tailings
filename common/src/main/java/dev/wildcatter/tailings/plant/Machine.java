package dev.wildcatter.tailings.plant;

/**
 * One true-size part of the plant or the dump station (docs/multiblocks/crushing-circuit.md, flotation-bank.md, dump-station.md).
 * Sizes are the spec's cells, W x H x L (1 block = 1 m). Watts are the real motor; 0 is unpowered steel. {@link Supply#MEDIUM} is
 * the 4.16 kV tier and {@link Supply#INDUSTRIAL} the 480 V tier, read from Wildcatter's tiers, never as a number.
 */
public enum Machine {
	GRIZZLY("grizzly", "Grizzly", 3, 2, 4, 0, Supply.NONE),
	JAW("jaw_crusher", "Jaw Crusher", 3, 4, 4, 110_000, Supply.INDUSTRIAL),
	CONE("cone_crusher", "Cone Crusher", 3, 4, 3, 132_000, Supply.INDUSTRIAL),
	SCREEN("double_deck_screen", "Double-Deck Screen", 3, 3, 6, 22_000, Supply.INDUSTRIAL),
	/** The head drive of a belt run; R3 rates it for one 30 m run, R4 sizes it by the run's length. */
	CONVEYOR_DRIVE("conveyor_drive", "Conveyor Drive", 1, 1, 2, 7_500, Supply.INDUSTRIAL),
	BALL_MILL("ball_mill", "Ball Mill", 4, 4, 6, 600_000, Supply.MEDIUM),
	CYCLONE("cyclone_cluster", "Cyclone Cluster", 2, 4, 2, 45_000, Supply.INDUSTRIAL),
	/** The 2x route at plant scale: 24 twin-start spirals on the cyclones' overflow, with a 37 kW feed pump (gravity-circuit.md). */
	SPIRAL_BANK("spiral_bank", "Spiral Bank", 4, 5, 6, 37_000, Supply.INDUSTRIAL),
	CONDITIONER("conditioning_tank", "Conditioning Tank", 3, 4, 3, 15_000, Supply.INDUSTRIAL),
	ACTIVATION_TANK("activation_tank", "Activation Tank", 3, 4, 3, 15_000, Supply.INDUSTRIAL),
	/** Eight 20 m3 cells, 8 x 45 kW of agitators on one motor box. */
	FLOTATION_BANK("flotation_bank", "Flotation Bank", 7, 4, 13, 360_000, Supply.INDUSTRIAL),
	BLOWER("air_blower", "Air Blower", 2, 2, 3, 75_000, Supply.INDUSTRIAL),
	THICKENER("concentrate_thickener", "Concentrate Thickener", 8, 3, 8, 5_500, Supply.INDUSTRIAL),
	FILTER("pressure_filter", "Pressure Filter", 3, 3, 5, 15_000, Supply.INDUSTRIAL),
	/** Phase 4 (full-recovery.md): four 20 m3 cells on the bank's tails, 4 x 45 kW. */
	SCAVENGER_BANK("scavenger_bank", "Scavenger Bank", 5, 4, 9, 180_000, Supply.INDUSTRIAL),
	REGRIND_MILL("regrind_mill", "Regrind Mill", 3, 3, 5, 150_000, Supply.INDUSTRIAL),
	CLEANER_BANK("cleaner_bank", "Cleaner Bank", 4, 3, 6, 44_000, Supply.INDUSTRIAL),
	LEACH_TANK("leach_tank", "Leach Tank", 5, 6, 5, 30_000, Supply.INDUSTRIAL),
	CCD_THICKENER("ccd_thickener", "CCD Thickener", 12, 4, 12, 7_500, Supply.INDUSTRIAL),
	ELECTROWINNING_BANK("electrowinning_bank", "Electrowinning Bank", 5, 3, 9, 400_000, Supply.INDUSTRIAL),
	/** Phase 4c: roasting, leaching and cementing out what concentrates carry (proposed: 250 kW). */
	BYPRODUCT_PLANT("byproduct_plant", "By-Product Plant", 5, 5, 8, 250_000, Supply.INDUSTRIAL),
	/** Phase 4b: a lined heap of waste rock under acid sprays, at a tenth of the plant's rate. Its pump is 15 kW (proposed). */
	HEAP_PAD("heap_pad", "Heap Leach Pad", 10, 3, 10, 15_000, Supply.INDUSTRIAL),
	/** The earthen pond from before Wildcatter's lined cell (Phase 4d): a legacy part, with no recipe and out of the creative tab; an old plant's pond goes on working. */
	TAILINGS_POND("tailings_pond_placeholder", "Tailings Pond (legacy)", 6, 2, 6, 0, Supply.NONE),
	DUMP_RAMP("dump_ramp", "Dump Ramp", 1, 1, 4, 0, Supply.NONE),
	DUMP_POCKET("dump_pocket", "Dump Pocket", 4, 3, 3, 0, Supply.NONE),
	APRON_FEEDER("apron_feeder", "Apron Feeder", 2, 1, 4, 11_000, Supply.INDUSTRIAL),
	ORE_SORTER("ore_sorter", "Ore Sorter", 3, 3, 3, 40_000, Supply.INDUSTRIAL),
	ORE_BIN("ore_bin", "Ore Bin", 3, 6, 3, 0, Supply.NONE),
	/** The truck dump's concrete retaining wall: 3 m of wall below the pad and its 1.5 m bumper above, in two rows (open-pit.md). */
	TRUCK_DUMP_WALL("truck_dump_wall", "Truck Dump Wall", 12, 5, 2, 0, Supply.NONE),
	/** The truck dump's 400 t hopper, drawn by an apron feeder touching it (open-pit.md). */
	PIT_HOPPER("pit_hopper", "Pit Hopper", 10, 6, 6, 0, Supply.NONE),
	/** The pit's diesel dispenser island and its 2,000 L day tank, fed by pipe (open-pit.md, P2a). */
	FUEL_BAY("fuel_bay", "Fuel Bay", 4, 3, 2, 0, Supply.NONE),
	/** The pit's haul dispatch office (open-pit.md, P2b): a small building on the control cable that runs the fleet by radio. Its load is on the low tier, not a motor. */
	HAUL_DISPATCH("haul_dispatch", "Haul Dispatch Office", 3, 3, 3, 0, Supply.NONE),
	/** The emulsion truck's filling point (open-pit-p3-blasting.md, P3): a standpipe and hose on Wildcatter pipe from the matrix tank. */
	MATRIX_FILL_POINT("matrix_fill_point", "Matrix Fill Point", 1, 2, 1, 0, Supply.NONE);

	public enum Supply {
		NONE,
		INDUSTRIAL,
		MEDIUM
	}

	private final String id;
	private final String name;
	private final int width;
	private final int height;
	private final int depth;
	private final int watts;
	private final Supply supply;

	Machine(String id, String name, int width, int height, int depth, int watts, Supply supply) {
		this.id = id;
		this.name = name;
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.watts = watts;
		this.supply = supply;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return name;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public int depth() {
		return depth;
	}

	public int watts() {
		return watts;
	}

	public Supply supply() {
		return supply;
	}

	public int cells() {
		return width * height * depth;
	}
}
