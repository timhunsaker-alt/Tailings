package dev.wildcatter.tailings.mine;

/**
 * The parts of the tunnel boring machine (docs/multiblocks/tunnel-boring-machine.md), each a true-size big part, with where its
 * main cell sits in the bore. The bore frame: {@code across} 0-7 left to right looking along the heading, {@code up} 0-7 from the
 * invert, {@code ahead} from the face about to be cut (0) back along the machine. A part's own frame is drawn facing the heading,
 * main cell at its front-left-bottom corner, W across, H up, L back. Plain Java.
 */
public enum TbmPart {
	/** A disc-cutter head in its shield, cutting the 8 x 8 face. */
	CUTTERHEAD("cutterhead", "Cutterhead", 8, 8, 1, 0, 0, -1),
	/** The tail shield and ring erector: a ring of 28 segments at its tail for each block of advance. */
	SEGMENT_ERECTOR("segment_erector", "Segment Erector", 8, 8, 2, 0, 0, -2),
	/** The thrust cylinders, grippers and cutterhead drive, the operator cab at its tail; down the middle of the bore. */
	MAIN_BEAM("main_beam", "Main Beam", 2, 2, 8, 3, 3, -4),
	/** Under the rear of the main beam, a block above the floor: it lays the floor and the twin track behind it. */
	TRACK_LAYER("track_layer", "Track Layer", 6, 1, 3, 1, 2, -9);

	private final String id;
	private final String name;
	private final int width;
	private final int height;
	private final int length;
	private final int across;
	private final int up;
	private final int ahead;

	TbmPart(String id, String name, int width, int height, int length, int across, int up, int ahead) {
		this.id = id;
		this.name = name;
		this.width = width;
		this.height = height;
		this.length = length;
		this.across = across;
		this.up = up;
		this.ahead = ahead;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return name;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public int length() {
		return length;
	}

	/** Its main cell in the bore frame, before the machine has cut anything. */
	public int across() {
		return across;
	}

	public int up() {
		return up;
	}

	public int ahead() {
		return ahead;
	}

	/** The cab is the main beam's last cell: across, up and ahead in the bore frame. */
	public static int[] cab() {
		return new int[] {MAIN_BEAM.across, MAIN_BEAM.up, MAIN_BEAM.ahead - MAIN_BEAM.length + 1};
	}
}
