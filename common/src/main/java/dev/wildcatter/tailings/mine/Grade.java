package dev.wildcatter.tailings.mine;

/** How fast the floor drops: one in two (decided with Tim, R6: the steep option is gone; an old program's one in one reads as one in two). */
public enum Grade {
	/** One block of drop over two of run, half a block each advance. */
	ONE_IN_TWO,
	/** One block of drop every advance. Brings back a steep rack. */
	ONE_IN_ONE;

	public int dropBlocks(int advance) {
		return advance / 2;
	}
}
