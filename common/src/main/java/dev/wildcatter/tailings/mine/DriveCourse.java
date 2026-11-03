package dev.wildcatter.tailings.mine;

/**
 * The 8 x 8 ring and the 6-wide floor for one block of advance. The outer ring is 28 lining segments: the perimeter
 * of an 8 x 8, with the floor row's middle six cells left for the floor pieces.
 */
public final class DriveCourse {
	public static final int FACE = 8;
	public static final int CLEAR = 6;
	public static final int SEGMENTS_PER_RING = 28;
	public static final int RAILS_PER_ADVANCE = 2;
	public static final int FLOOR_PIECES = 6;
	public static final int GRAVEL_ON_FLAT = 2;
	public static final int PROBE_BLOCKS = 3;
	/** One block per 20 seconds in stone. Deepslate is slower. */
	public static final int STONE_TICKS = 400;
	public static final int DEEPSLATE_TICKS = 600;

	private DriveCourse() {
	}

	/** Floor voxel Y after {@code advance} steps. Even and odd steps of a 1-in-2 pair share a voxel; the shape changes. */
	public static int floorY(int startFloorY, int advance, Grade grade) {
		return startFloorY - grade.dropBlocks(advance);
	}

	/** True while the drill is still dropping toward {@code targetY}. */
	public static boolean declining(int floorY, int targetY) {
		return floorY > targetY;
	}

	/**
	 * Columns 1 to 6. On a 1-in-2 decline, even advances are the upper half and a stair; odd advances are the lower
	 * half and a landing, so both have dropped one block over the pair. A 1-in-1 advance is a steep slope and a stair.
	 */
	public static FloorPiece[] floor(int advance, Grade grade, boolean decline) {
		FloorPiece[] columns = new FloorPiece[CLEAR];
		if (!decline) {
			columns[0] = FloorPiece.FLAT_ROCK;
			columns[1] = FloorPiece.RAIL_FLAT;
			columns[2] = FloorPiece.LANDING;
			columns[3] = FloorPiece.LANDING;
			columns[4] = FloorPiece.RAIL_FLAT;
			columns[5] = FloorPiece.FLAT_ROCK;
			return columns;
		}
		if (grade == Grade.ONE_IN_ONE) {
			columns[0] = FloorPiece.SLOPE_STEEP;
			columns[1] = FloorPiece.RAIL_STEEP;
			columns[2] = FloorPiece.STAIR;
			columns[3] = FloorPiece.STAIR;
			columns[4] = FloorPiece.RAIL_STEEP;
			columns[5] = FloorPiece.SLOPE_STEEP;
			return columns;
		}
		boolean upper = advance % 2 == 0;
		FloorPiece slope = upper ? FloorPiece.SLOPE_UPPER : FloorPiece.SLOPE_LOWER;
		FloorPiece rail = upper ? FloorPiece.RAIL_UPPER : FloorPiece.RAIL_LOWER;
		FloorPiece walk = upper ? FloorPiece.STAIR : FloorPiece.LANDING;
		columns[0] = slope;
		columns[1] = rail;
		columns[2] = walk;
		columns[3] = walk;
		columns[4] = rail;
		columns[5] = slope;
		return columns;
	}

	/** Lining cells of one ring, as (across, up) with up 0 the invert and up 7 the crown. */
	public static int liningCount() {
		int n = 0;
		for (int up = 0; up < FACE; up++) {
			for (int across = 0; across < FACE; across++) {
				if (isLining(across, up)) {
					n++;
				}
			}
		}
		return n;
	}

	public static boolean isLining(int across, int up) {
		if (across < 0 || across >= FACE || up < 0 || up >= FACE) {
			return false;
		}
		if (up == 0 || up == FACE - 1) {
			return true;
		}
		return across == 0 || across == FACE - 1;
	}

	public static boolean isFloor(int across, int up) {
		return up == 1 && across >= 1 && across <= CLEAR;
	}

	/** How many rock blocks the floor consumes. Gravel ballast is not rock. */
	public static int rockPieces(FloorPiece[] floor) {
		int n = 0;
		for (FloorPiece piece : floor) {
			if (!piece.usesGravel()) {
				n++;
			}
		}
		return n;
	}

	public static int rails(FloorPiece[] floor) {
		int n = 0;
		for (FloorPiece piece : floor) {
			if (piece.isRail()) {
				n++;
			}
		}
		return n;
	}
}
