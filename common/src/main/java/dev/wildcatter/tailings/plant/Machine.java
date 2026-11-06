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
	CONDITIONER("conditioning_tank", "Conditioning Tank", 3, 4, 3, 15_000, Supply.INDUSTRIAL),
	/** Eight 20 m3 cells, 8 x 45 kW of agitators on one motor box. */
	FLOTATION_BANK("flotation_bank", "Flotation Bank", 7, 4, 13, 360_000, Supply.INDUSTRIAL),
	BLOWER("air_blower", "Air Blower", 2, 2, 3, 75_000, Supply.INDUSTRIAL),
	THICKENER("concentrate_thickener", "Concentrate Thickener", 8, 3, 8, 5_500, Supply.INDUSTRIAL),
	FILTER("pressure_filter", "Pressure Filter", 3, 3, 5, 15_000, Supply.INDUSTRIAL),
	/** An earthen pond for the final tails until Phase 4d's lined cell (decided with Tim, 2026-11-05). A placeholder. */
	TAILINGS_POND("tailings_pond_placeholder", "Tailings Pond (placeholder)", 6, 2, 6, 0, Supply.NONE),
	DUMP_RAMP("dump_ramp", "Dump Ramp", 1, 1, 4, 0, Supply.NONE),
	DUMP_POCKET("dump_pocket", "Dump Pocket", 4, 3, 3, 0, Supply.NONE),
	APRON_FEEDER("apron_feeder", "Apron Feeder", 2, 1, 4, 11_000, Supply.INDUSTRIAL),
	ORE_SORTER("ore_sorter", "Ore Sorter", 3, 3, 3, 40_000, Supply.INDUSTRIAL),
	ORE_BIN("ore_bin", "Ore Bin", 3, 6, 3, 0, Supply.NONE);

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

	/** The dump station's feeder, sorter and ramp: placed and powered now, joined to the process in R4c. */
	public boolean waitsForR4() {
		return this == DUMP_RAMP || this == APRON_FEEDER || this == ORE_SORTER;
	}
}
