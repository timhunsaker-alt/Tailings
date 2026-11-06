package dev.wildcatter.tailings.mine;

/**
 * The back-up gantries (docs/multiblocks/tunnel-boring-machine.md), in their order behind the machine. Each straddles the twin track, 6
 * x 4 by its length, and its place is {@code row} blocks back from the cutterhead's main cell: the first of them just behind the last
 * track laid, so its wheels are always on rail. The rows behind the last are the loading point. Plain Java.
 */
public enum TbmGantry {
	PUMP("pump_gantry", "Pump Gantry", 3, 12),
	REEL("reel_gantry", "Reel Gantry", 4, 15),
	CONVEYOR("conveyor_gantry", "Conveyor Gantry", 8, 19);

	public static final int WIDTH = 6;
	public static final int HEIGHT = 4;
	/** The first row behind the conveyor's tail, and how many rows the conveyor's chute reaches: a train's first few cars. */
	public static final int LOADING_ROW = 27;
	public static final int LOADING_ROWS = 5;
	/** Across the gantry's frame: where its wheels run on the twin track, columns 2 and 5 of the bore. */
	public static final int LEFT_RAIL = 1;
	public static final int RIGHT_RAIL = 4;

	private final String id;
	private final String name;
	private final int length;
	private final int row;

	TbmGantry(String id, String name, int length, int row) {
		this.id = id;
		this.name = name;
		this.length = length;
		this.row = row;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return name;
	}

	public int length() {
		return length;
	}

	public int row() {
		return row;
	}
}
