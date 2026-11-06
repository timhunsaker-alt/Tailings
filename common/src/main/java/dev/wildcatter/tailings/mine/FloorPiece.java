package dev.wildcatter.tailings.mine;

/** One cell of the drive floor, columns 1 to 6 west to east when looking along the heading. */
public enum FloorPiece {
	SLOPE_LOWER,
	SLOPE_UPPER,
	SLOPE_STEEP,
	RAIL_LOWER,
	RAIL_UPPER,
	RAIL_STEEP,
	RAIL_FLAT,
	STAIR,
	LANDING,
	FLAT_ROCK,
	GRAVEL;

	public boolean isRail() {
		return this == RAIL_LOWER || this == RAIL_UPPER || this == RAIL_STEEP || this == RAIL_FLAT;
	}

	/** Track is mine rail on gravel ballast, flat and decline alike (decided with Tim, R6). */
	public boolean usesGravel() {
		return this == GRAVEL || isRail();
	}
}
