package dev.wildcatter.tailings.pit;

import dev.wildcatter.tailings.plant.Machine;

/**
 * Where the large electric rotary blasthole drill's four big parts stand (docs/multiblocks/open-pit-p3-blasting.md, As built: four
 * parts), each part's main cell from the crawlers' main cell, in the crawlers' own frame (facing north: x east, y up, z south; the
 * machine's front, its cab end, is north). The crawlers stand on the ground, the deck on them overhanging 2 m ahead and 3 m behind, the
 * mast standing on the deck's back end over the hole and the dust collector beside the mast's foot, as the lifted drill stands. Plain
 * Java.
 */
public final class BlastholeDrillLayout {
	public enum Part {
		CRAWLERS(Machine.DRILL_CRAWLERS, 0, 0, 0),
		DECK(Machine.DRILL_DECK, 0, 2, -2),
		MAST(Machine.DRILL_MAST, 2, 6, 10),
		DUST_COLLECTOR(Machine.DRILL_DUST_COLLECTOR, 0, 6, 10);

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

	/**
	 * The machine's centre, where the lifted drill stands and turns: the middle of the deck, metres from the crawlers' main cell's
	 * north-west corner. The hole is {@code BlastholeDrillEntity.HOLE_BACK} (6 m) behind it, under the mast: at 3.5, 11.5.
	 */
	public static final double CENTRE_X = 3.5;
	public static final double CENTRE_Z = 5.5;
	/** The hole under the mast's foot, metres behind the centre (the lifted drill's {@code HOLE_BACK}). */
	public static final double HOLE_BACK = 6.0;

	private BlastholeDrillLayout() {
	}

	/** {@code offset} turned {@code quarterTurns} clockwise from north, as Wildcatter's big parts turn their cells. */
	public static int[] turned(int[] offset, int quarterTurns) {
		return RopeShovelLayout.turned(offset, quarterTurns);
	}

	/** Its yaw, in Minecraft's degrees, for a drill built facing {@code quarterTurns} from north (its front to the north at 0). */
	public static float yaw(int quarterTurns) {
		return RopeShovelLayout.yaw(quarterTurns);
	}

	/** The quarter turns from north nearest {@code yaw}. */
	public static int turns(float yaw) {
		return RopeShovelLayout.turns(yaw);
	}

	/**
	 * The crawlers' main cell for a drill whose centre stands at {@code x, z}, facing {@code quarterTurns}: {x, z} of the block. The
	 * centre is turned about the main cell's middle, as the parts' cells are.
	 */
	public static int[] mainFor(double x, double z, int quarterTurns) {
		double qx = CENTRE_X - 0.5, qz = CENTRE_Z - 0.5;
		for (int t = 0; t < Math.floorMod(quarterTurns, 4); t++) {
			double tmp = qx;
			qx = -qz;
			qz = tmp;
		}
		return new int[] {(int) Math.round(x - 0.5 - qx), (int) Math.round(z - 0.5 - qz)};
	}

	/** The drill's centre for crawlers whose main cell is at {@code x, z}, facing {@code quarterTurns}: {x, z} in metres. */
	public static double[] centreFor(int x, int z, int quarterTurns) {
		double qx = CENTRE_X - 0.5, qz = CENTRE_Z - 0.5;
		for (int t = 0; t < Math.floorMod(quarterTurns, 4); t++) {
			double tmp = qx;
			qx = -qz;
			qz = tmp;
		}
		return new double[] {x + 0.5 + qx, z + 0.5 + qz};
	}

	/** Every cell the four parts fill. */
	public static int cells() {
		int n = 0;
		for (Part part : Part.values()) {
			n += part.machine().cells();
		}
		return n;
	}

	/** True if no two parts share a cell, facing north: each part's box, from its offset and size. */
	public static boolean overlaps() {
		Part[] all = Part.values();
		for (int i = 0; i < all.length; i++) {
			for (int j = i + 1; j < all.length; j++) {
				if (boxesMeet(all[i], all[j])) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean boxesMeet(Part a, Part b) {
		return a.x < b.x + b.machine.width() && b.x < a.x + a.machine.width() && a.y < b.y + b.machine.height() && b.y < a.y + a.machine.height()
				&& a.z < b.z + b.machine.depth() && b.z < a.z + a.machine.depth();
	}
}
