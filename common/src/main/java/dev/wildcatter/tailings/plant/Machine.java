package dev.wildcatter.tailings.plant;

/**
 * One true-size part. Sizes are the spec's cells (1 block = 1 m). Watts are the real motor; 0 is unpowered steel.
 * {@link Supply#MEDIUM} is 4.16 kV. The rest of the motors are 480 V industrial.
 */
public enum Machine {
	GRIZZLY("grizzly", "Grizzly", 3, 2, 4, 0, Supply.NONE),
	JAW("jaw_crusher", "Jaw Crusher", 3, 4, 4, 110_000, Supply.INDUSTRIAL),
	CONE("cone_crusher", "Cone Crusher", 3, 4, 3, 132_000, Supply.INDUSTRIAL),
	SCREEN("double_deck_screen", "Double-Deck Screen", 3, 3, 6, 22_000, Supply.INDUSTRIAL),
	BELT("belt_conveyor", "Belt Conveyor", 1, 1, 6, 7_500, Supply.INDUSTRIAL),
	BALL_MILL("ball_mill", "Ball Mill", 4, 4, 6, 600_000, Supply.MEDIUM),
	CYCLONE("cyclone_cluster", "Cyclone Cluster", 2, 4, 2, 45_000, Supply.INDUSTRIAL),
	CONDITIONER("conditioning_tank", "Conditioning Tank", 3, 4, 3, 15_000, Supply.INDUSTRIAL),
	CELL("flotation_cell", "Flotation Cell", 3, 4, 3, 45_000, Supply.INDUSTRIAL),
	BLOWER("air_blower", "Air Blower", 2, 2, 3, 75_000, Supply.INDUSTRIAL),
	THICKENER("concentrate_thickener", "Concentrate Thickener", 8, 3, 8, 5_500, Supply.INDUSTRIAL),
	FILTER("pressure_filter", "Pressure Filter", 3, 3, 5, 15_000, Supply.INDUSTRIAL);

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
