package dev.wildcatter.tailings.mine;

/** How fast the floor drops. One in two is the default; one in one is the steep option. */
public enum Grade {
	/** One block of drop over two of run, half a block each advance. */
	ONE_IN_TWO,
	/** One block of drop every advance. Brings back a steep rack. */
	ONE_IN_ONE;

	public int dropBlocks(int advance) {
		return this == ONE_IN_ONE ? advance : advance / 2;
	}
}
