package dev.wildcatter.tailings.mine;

/** What one advance spent. A null lining array means the ring was crafted segments; otherwise each cell is a stone block. */
public final class Spent {
	private final Ground[] floor;
	private final Ground[] lining;

	public Spent(Ground[] floor, Ground[] lining) {
		this.floor = floor;
		this.lining = lining;
	}

	public Ground[] floor() {
		return floor;
	}

	public Ground[] lining() {
		return lining;
	}

	public boolean segmentRing() {
		return lining == null;
	}
}
