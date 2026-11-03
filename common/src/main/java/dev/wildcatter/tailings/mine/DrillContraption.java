package dev.wildcatter.tailings.mine;

/**
 * The TBM moves with the face, the way a contraption does: the cab is the anchor, and each advance shifts that anchor
 * one block along the heading. The first shift also lifts it into the clear bore so it does not sit on the floor it
 * just laid.
 */
public final class DrillContraption {
	private DrillContraption() {
	}

	public static boolean ridingInBore(int blockY, int floorY) {
		return blockY > floorY;
	}

	public static int[] translate(int x, int y, int z, Heading heading, boolean lift) {
		return new int[] {x + heading.forwardX(), y + (lift ? 1 : 0), z + heading.forwardZ()};
	}
}
