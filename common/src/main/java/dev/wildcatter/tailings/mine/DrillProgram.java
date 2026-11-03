package dev.wildcatter.tailings.mine;

/**
 * What the cab screen sets. The drill declines to {@code targetY}, then drives flat for {@code flatLength} blocks.
 */
public final class DrillProgram {
	private int targetY;
	private Heading heading;
	private int flatLength;
	private Grade grade;
	private boolean stopOnOre = true;
	private boolean stopOnCable = true;
	private boolean stopOnLiquid = true;
	private boolean stopOnEmpty = true;
	private int cableBlocks = 256;

	public DrillProgram(int targetY, Heading heading, int flatLength) {
		this.targetY = targetY;
		this.heading = heading;
		this.flatLength = Math.max(0, flatLength);
		this.grade = Grade.ONE_IN_TWO;
	}

	public int targetY() {
		return targetY;
	}

	public void setTargetY(int targetY) {
		this.targetY = targetY;
	}

	public Heading heading() {
		return heading;
	}

	public void setHeading(Heading heading) {
		this.heading = heading;
	}

	public int flatLength() {
		return flatLength;
	}

	public void setFlatLength(int flatLength) {
		this.flatLength = Math.max(0, flatLength);
	}

	public Grade grade() {
		return grade;
	}

	public void setGrade(Grade grade) {
		this.grade = grade;
	}

	public boolean stopOnOre() {
		return stopOnOre;
	}

	public void setStopOnOre(boolean stopOnOre) {
		this.stopOnOre = stopOnOre;
	}

	public boolean stopOnCable() {
		return stopOnCable;
	}

	public void setStopOnCable(boolean stopOnCable) {
		this.stopOnCable = stopOnCable;
	}

	public boolean stopOnLiquid() {
		return stopOnLiquid;
	}

	public void setStopOnLiquid(boolean stopOnLiquid) {
		this.stopOnLiquid = stopOnLiquid;
	}

	public boolean stopOnEmpty() {
		return stopOnEmpty;
	}

	public void setStopOnEmpty(boolean stopOnEmpty) {
		this.stopOnEmpty = stopOnEmpty;
	}

	public int cableBlocks() {
		return cableBlocks;
	}

	public void setCableBlocks(int cableBlocks) {
		this.cableBlocks = Math.max(0, cableBlocks);
	}
}
