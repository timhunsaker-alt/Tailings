package dev.wildcatter.tailings.mine;

/** The four horizontal drives. Across is the player's left-to-right when looking along the heading. */
public enum Heading {
	NORTH(0, -1, 1, 0),
	SOUTH(0, 1, -1, 0),
	EAST(1, 0, 0, -1),
	WEST(-1, 0, 0, 1);

	private final int forwardX;
	private final int forwardZ;
	private final int acrossX;
	private final int acrossZ;

	Heading(int forwardX, int forwardZ, int acrossX, int acrossZ) {
		this.forwardX = forwardX;
		this.forwardZ = forwardZ;
		this.acrossX = acrossX;
		this.acrossZ = acrossZ;
	}

	public int forwardX() {
		return forwardX;
	}

	public int forwardZ() {
		return forwardZ;
	}

	public int acrossX() {
		return acrossX;
	}

	public int acrossZ() {
		return acrossZ;
	}

	public Heading turn() {
		return switch (this) {
			case NORTH -> EAST;
			case EAST -> SOUTH;
			case SOUTH -> WEST;
			case WEST -> NORTH;
		};
	}
}
