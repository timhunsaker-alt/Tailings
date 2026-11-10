package dev.wildcatter.tailings.pit;

import dev.wildcatter.tailings.plant.Machine;

/**
 * Where the walking dragline's parts stand to be built (docs/multiblocks/dragline.md), each part's main cell from the tub's main cell,
 * in the tub's own frame (facing north: x east, y up, z south; the machine's front is north), as the rope shovel's
 * ({@link RopeShovelLayout}). The tub on the ground; a walking shoe each side of it; the house on the tub (its revolving frame on the
 * tub's roller path, a metre up), overhanging the shoes; the mast on the house's roof at its front; and laid on the ground ahead, as a
 * boom is before it is raised, the boom's five sections foot to point in a line from the house's front, and the bucket beyond its point.
 * Plain Java.
 */
public final class DraglineLayout {
	public enum Part {
		TUB(Machine.DRAGLINE_TUB, 0, 0, 0),
		SHOE_LEFT(Machine.DRAGLINE_SHOE, -3, 0, 1),
		SHOE_RIGHT(Machine.DRAGLINE_SHOE, 15, 0, 1),
		HOUSE(Machine.DRAGLINE_HOUSE, -4, 3, -1),
		MAST(Machine.DRAGLINE_MAST, 5, 15, 0),
		BOOM_FOOT(Machine.DRAGLINE_BOOM, 6, 0, -16),
		BOOM_2(Machine.DRAGLINE_BOOM, 6, 0, -31),
		BOOM_3(Machine.DRAGLINE_BOOM, 6, 0, -46),
		BOOM_4(Machine.DRAGLINE_BOOM, 6, 0, -61),
		BOOM_POINT(Machine.DRAGLINE_BOOM, 6, 0, -76),
		BUCKET(Machine.DRAGLINE_BUCKET, 6, 0, -82);

		private final Machine machine;
		private final int x, y, z;

		Part(Machine machine, int x, int y, int z) {
			this.machine = machine;
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public Machine machine() {
			return machine;
		}

		/** Its main cell from the tub's main cell, facing north, {x, y, z}. */
		public int[] offset() {
			return new int[] {x, y, z};
		}

		/** What the NOT BUILT readout calls it: a shoe by its side, a boom section by its place in the line. */
		public String describe() {
			return switch (this) {
				case SHOE_LEFT -> "left Dragline Walking Shoe";
				case SHOE_RIGHT -> "right Dragline Walking Shoe";
				case BOOM_FOOT -> "Dragline Boom Section at the boom's foot";
				case BOOM_2 -> "second Dragline Boom Section";
				case BOOM_3 -> "third Dragline Boom Section";
				case BOOM_4 -> "fourth Dragline Boom Section";
				case BOOM_POINT -> "Dragline Boom Section at the boom's point";
				default -> machine.displayName();
			};
		}
	}

	/** The boom is laid out as five sections of 15 m (two of the spec's 7.5 m sections each: a cell is a whole metre). */
	public static final int BOOM_SECTIONS = 5;
	/** The machine's centre, where it turns and swings: the middle of the tub, metres from its main cell's north-west corner. */
	public static final double CENTRE_X = 7.5;
	public static final double CENTRE_Z = 7.5;

	private DraglineLayout() {
	}

	/** {@code offset} turned {@code quarterTurns} clockwise from north, as Wildcatter's big parts turn their cells. */
	public static int[] turned(int[] offset, int quarterTurns) {
		return RopeShovelLayout.turned(offset, quarterTurns);
	}

	/** Its yaw, in Minecraft's degrees, for a machine built facing {@code quarterTurns} from north. */
	public static float yaw(int quarterTurns) {
		return RopeShovelLayout.yaw(quarterTurns);
	}

	/** The quarter turns from north nearest {@code yaw}. */
	public static int turns(float yaw) {
		return RopeShovelLayout.turns(yaw);
	}

	/** Every cell its parts fill. */
	public static int cells() {
		int n = 0;
		for (Part part : Part.values()) {
			n += part.machine().cells();
		}
		return n;
	}
}
