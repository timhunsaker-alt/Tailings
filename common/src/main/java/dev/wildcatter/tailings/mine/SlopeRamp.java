package dev.wildcatter.tailings.mine;

/**
 * A smooth ramp as eight thin boxes, the way framed-slope mods approximate a wedge. A 1-in-2 half rises 8 pixels over
 * 16. The downhill end is the low end. Two halves in a row meet in the middle and drop one block over the pair.
 */
public final class SlopeRamp {
	public static final int SLICES = 8;

	private SlopeRamp() {
	}

	/** Pixel boxes {@code x0,y0,z0,x1,y1,z1}. {@code steep} is the 1-in-1 wedge. Otherwise {@code upper} picks the half. */
	public static int[][] boxes(boolean upper, boolean steep, Heading downhill) {
		int[][] north = steep ? rise(0, 2) : rise(upper ? 8 : 0, 1);
		return switch (downhill) {
			case SOUTH -> mirrorZ(north);
			case EAST -> yawEast(north);
			case WEST -> yawWest(north);
			case NORTH -> north;
		};
	}

	/** Low at north (z = 0), high at south. Each slice is 2 pixels of run. */
	private static int[][] rise(int base, int step) {
		int[][] boxes = new int[SLICES][];
		for (int i = 0; i < SLICES; i++) {
			int z0 = i * 2;
			boxes[i] = new int[] {0, 0, z0, 16, base + (i + 1) * step, z0 + 2};
		}
		return boxes;
	}

	private static int[][] mirrorZ(int[][] boxes) {
		int[][] out = new int[boxes.length][];
		for (int i = 0; i < boxes.length; i++) {
			int[] box = boxes[i];
			out[i] = new int[] {box[0], box[1], 16 - box[5], box[3], box[4], 16 - box[2]};
		}
		return out;
	}

	/** North's high side (+z) turns to the west, so the downhill end is east. */
	private static int[][] yawEast(int[][] boxes) {
		int[][] out = new int[boxes.length][];
		for (int i = 0; i < boxes.length; i++) {
			int[] box = boxes[i];
			out[i] = new int[] {16 - box[5], box[1], box[0], 16 - box[2], box[4], box[3]};
		}
		return out;
	}

	private static int[][] yawWest(int[][] boxes) {
		int[][] out = new int[boxes.length][];
		for (int i = 0; i < boxes.length; i++) {
			int[] box = boxes[i];
			out[i] = new int[] {box[2], box[1], 16 - box[3], box[5], box[4], 16 - box[0]};
		}
		return out;
	}
}
