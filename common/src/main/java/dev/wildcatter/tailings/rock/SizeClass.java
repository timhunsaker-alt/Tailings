package dev.wildcatter.tailings.rock;

/**
 * The ladder of sizes in {@code docs/multiblocks/crushing-circuit.md}. Fines are the ball-mill product and are not made
 * by the starter machines.
 */
public enum SizeClass {
	CRUSHED("crushed", "Crushed"),
	GRAVEL("gravel", "Gravel"),
	SAND("sand", "Sand"),
	FINES("fines", "Fines");

	private final String path;
	private final String label;

	SizeClass(String path, String label) {
		this.path = path;
		this.label = label;
	}

	public String path() {
		return path;
	}

	public String label() {
		return label;
	}

	/** {@code tailings:crushed_granite} and the same shape for the other sizes. */
	public String itemId(Rock rock) {
		return "tailings:" + path + "_" + rock.id();
	}
}
