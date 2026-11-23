package dev.wildcatter.tailings.mine;

/**
 * The round bore and the 6-wide floor for one block of advance (heavy-iron-fixes, 2026-11-22). The face is cut 8 x 8 for the machine's
 * square parts to pass, but the bore is round: an 8 m circle, block by block, its three cells at each corner outside it backfilled
 * behind the erector with the ground they held (or grouted, where they were open), as a real shield's annulus is grouted behind its
 * segments. The ring is the circle's edge: 18 lining segments, the floor row's six cells left for the floor pieces.
 */
public final class DriveCourse {
	public static final int FACE = 8;
	public static final int CLEAR = 6;
	public static final int SEGMENTS_PER_RING = 18;
	/** The bore's radius, cells from its centre: a cell whose centre is farther out is outside the round bore. */
	public static final double BORE_RADIUS = 4.2;
	/** The corner cells outside the round bore, three at each corner. */
	public static final int CORNERS = 12;
	public static final int RAILS_PER_ADVANCE = 2;
	public static final int FLOOR_PIECES = 6;
	public static final int GRAVEL_ON_FLAT = 2;
	public static final int PROBE_BLOCKS = 3;
	/** One block per 20 seconds in stone. Deepslate is slower. */
	public static final int STONE_TICKS = 400;
	public static final int DEEPSLATE_TICKS = 600;
	/**
	 * Where the machine places its ring and its floor, in slices behind the face once it has moved on (tunnel-boring-machine.md):
	 * the ring just behind the segment erector's tail, the floor and track just behind the track layer.
	 */
	public static final int RING_BEHIND = 4;
	public static final int FLOOR_BEHIND = 12;

	private DriveCourse() {
	}

	/** Floor voxel Y after {@code advance} steps. Even and odd steps of a 1-in-2 pair share a voxel; the shape changes. */
	public static int floorY(int startFloorY, int advance, Grade grade) {
		// the slices behind the first face are where the machine was built, on the level
		return startFloorY - grade.dropBlocks(Math.max(0, advance));
	}

	/** True while the drill is still dropping toward {@code targetY}. */
	public static boolean declining(int floorY, int targetY) {
		return floorY > targetY;
	}

	/**
	 * Columns 1 to 6. On a 1-in-2 decline, even advances are the upper half and a stair; odd advances are the lower
	 * half and a landing, so both have dropped one block over the pair. (Decided with Tim, R6: 1 in 2 only.)
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
		boolean upper = Math.floorMod(advance, 2) == 0;
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

	/** True for the corner cells of the 8 x 8 face outside the round bore: cut for the machine to pass, backfilled behind it. */
	public static boolean isOutside(int across, int up) {
		if (across < 0 || across >= FACE || up < 0 || up >= FACE) {
			return false;
		}
		double dx = across - (FACE - 1) / 2.0, dy = up - (FACE - 1) / 2.0;
		return dx * dx + dy * dy > BORE_RADIUS * BORE_RADIUS;
	}

	/**
	 * The ring: the cells of the round bore on its edge (on the 8 x 8's edge, or beside a corner cell outside it), but for the floor row's
	 * cells, which are the floor pieces' (the invert's corners are filled by the floor's own slope or rock).
	 */
	public static boolean isLining(int across, int up) {
		if (across < 0 || across >= FACE || up < 0 || up >= FACE || isOutside(across, up) || isFloor(across, up)) {
			return false;
		}
		if (up == 0 || up == FACE - 1 || across == 0 || across == FACE - 1) {
			return true;
		}
		return isOutside(across - 1, up) || isOutside(across + 1, up) || isOutside(across, up - 1) || isOutside(across, up + 1);
	}

	/**
	 * For the ring's two upper corner segments (a lining cell with outside cells beside it and above it): -1 or 1, the step across toward
	 * its outer side; else 0. Those are drawn as rounded segments, so the bore reads round-cornered from inside, not stepped.
	 */
	public static int roundedCorner(int across, int up) {
		if (!isLining(across, up) || up < FACE / 2 || !isOutside(across, up + 1)) {
			return 0;
		}
		return isOutside(across - 1, up) ? -1 : isOutside(across + 1, up) ? 1 : 0;
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
