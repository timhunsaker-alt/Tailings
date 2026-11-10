package dev.wildcatter.tailings.pit;

import dev.wildcatter.tailings.plant.Machine;

/**
 * Where the electric rope shovel's six big parts stand (docs/multiblocks/open-pit-p5-big-machines.md, P5d), each part's main cell
 * from the crawlers' main cell, in the crawlers' own frame (facing north: x east, y up, z south; the machine's front is north). The
 * crawlers and the house stand as they work, the house on the crawlers with its rear overhanging, the gantry on the house's roof; the
 * boom, the dipper handle and the dipper lie on the ground ahead, as a real boom is laid out before it is raised. Plain Java.
 */
public final class RopeShovelLayout {
	public enum Part {
		CRAWLERS(Machine.SHOVEL_CRAWLERS, 0, 0, 0),
		HOUSE(Machine.SHOVEL_HOUSE, 1, 3, -1),
		GANTRY(Machine.SHOVEL_GANTRY, 4, 12, 1),
		BOOM(Machine.SHOVEL_BOOM, 6, 0, -21),
		HANDLE(Machine.SHOVEL_HANDLE, 10, 0, -16),
		DIPPER(Machine.SHOVEL_DIPPER, 5, 0, -27);

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

		/** Its main cell from the crawlers' main cell, facing north, {x, y, z}. */
		public int[] offset() {
			return new int[] {x, y, z};
		}
	}

	/** The machine's centre, where it turns and swings: the middle of the crawlers, metres from their main cell's north-west corner. */
	public static final double CENTRE_X = 7.0;
	public static final double CENTRE_Z = 6.0;

	private RopeShovelLayout() {
	}

	/** {@code offset} turned {@code quarterTurns} clockwise from north, as Wildcatter's big parts turn their cells. */
	public static int[] turned(int[] offset, int quarterTurns) {
		int dx = offset[0], dz = offset[2];
		for (int i = 0; i < Math.floorMod(quarterTurns, 4); i++) {
			int t = dx;
			dx = -dz;
			dz = t;
		}
		return new int[] {dx, offset[1], dz};
	}

	/** Its yaw, in Minecraft's degrees, for a machine built facing {@code quarterTurns} from north (its front to the north at 0). */
	public static float yaw(int quarterTurns) {
		return 180F + 90F * Math.floorMod(quarterTurns, 4);
	}

	/** The quarter turns from north nearest {@code yaw}. */
	public static int turns(float yaw) {
		return Math.floorMod(Math.round((yaw - 180F) / 90F), 4);
	}

	/** Every cell the six parts fill, from the crawlers' main cell, facing north: for checking the ground before setting down. */
	public static int cells() {
		int n = 0;
		for (Part part : Part.values()) {
			n += part.machine().cells();
		}
		return n;
	}
}
