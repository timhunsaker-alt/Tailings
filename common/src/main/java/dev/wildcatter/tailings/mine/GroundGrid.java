package dev.wildcatter.tailings.mine;

/** The ground the drill reads and writes. The world adapter and the tests both implement it. */
public interface GroundGrid {
	Ground get(int x, int y, int z);

	void set(int x, int y, int z, Ground ground);

	/** Item id of whatever {@link #get} currently is, captured before the cell is cleared. */
	default String dropId(int x, int y, int z) {
		return get(x, y, z).name().toLowerCase(java.util.Locale.ROOT);
	}

	default void placeLining(int x, int y, int z) {
		placeLining(x, y, z, null);
	}

	/** {@code material} null is a crafted segment. Otherwise the ring cell is that stone block. */
	default void placeLining(int x, int y, int z, Ground material) {
		set(x, y, z, material == null ? Ground.LINING : material);
	}

	/**
	 * A crafted segment at one of the ring's two upper corners, drawn rounded, its solid corner toward ({@code towardX},
	 * {@code towardZ}) and up. The tests keep it a plain segment.
	 */
	default void placeLiningCorner(int x, int y, int z, int towardX, int towardZ) {
		placeLining(x, y, z, null);
	}

	/**
	 * The cove at one of the bore's lower corners, in the open cell over a flat floor beside a wall, its solid toward ({@code towardX},
	 * {@code towardZ}) and down. Drawn finish from no stock: a world places it only beside a crafted segment; elsewhere nothing.
	 */
	default void placeLiningFoot(int x, int y, int z, int towardX, int towardZ) {
	}

	default void placeFloor(int x, int y, int z, FloorPiece piece, Ground rock) {
		set(x, y, z, Ground.FLOOR);
	}

	default void placeCave(int x, int y, int z) {
		set(x, y, z, Ground.COBBLE);
	}

	default FloorPiece floorPiece(int x, int y, int z) {
		return null;
	}

	default Ground floorRock(int x, int y, int z) {
		return null;
	}
}
