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

	/** Flat track is mine rail on gravel ballast. The decline cuts its rail bed from rock, not gravel. */
	public boolean usesGravel() {
		return this == GRAVEL || this == RAIL_FLAT;
	}
}
