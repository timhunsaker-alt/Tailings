package dev.wildcatter.tailings.mine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** The cab's slots, plus rock held back from the cut to make floor pieces. */
public final class DrillStock {
	public static final int TANK_MAX = 256;
	private int rail;
	private int gravel;
	private int cobble;
	private int segments;
	private int water;
	private final Deque<Ground> floorRock = new ArrayDeque<>();

	public int rail() {
		return rail;
	}

	public int gravel() {
		return gravel;
	}

	public int cobble() {
		return cobble;
	}

	public int segments() {
		return segments;
	}

	public int water() {
		return water;
	}

	public int floorRock() {
		return floorRock.size();
	}

	public void setRail(int n) {
		rail = n;
	}

	public void setGravel(int n) {
		gravel = n;
	}

	public void setCobble(int n) {
		cobble = n;
	}

	public void setSegments(int n) {
		segments = n;
	}

	public void setWater(int n) {
		water = n;
	}

	public void addRail(int n) {
		rail += n;
	}

	public void addGravel(int n) {
		gravel += n;
	}

	public void addCobble(int n) {
		cobble += n;
	}

	public void addSegments(int n) {
		segments += n;
	}

	public void addWater(int n) {
		water += n;
	}

	/** Pumped source blocks fill the quench tank. Returns how many were voided because the tank was full. */
	public int acceptWater(int sources) {
		int space = Math.max(0, TANK_MAX - water);
		int kept = Math.min(Math.max(0, sources), space);
		water += kept;
		return Math.max(0, sources) - kept;
	}

	public void addFloorRock(Ground ground) {
		if (ground != null && ground.isFloorRock()) {
			floorRock.add(ground);
		}
	}

	public String floorRockSave() {
		StringBuilder out = new StringBuilder();
		for (Ground ground : floorRock) {
			if (!out.isEmpty()) {
				out.append(',');
			}
			out.append(ground.name());
		}
		return out.toString();
	}

	public void loadFloorRock(String saved) {
		floorRock.clear();
		if (saved == null || saved.isEmpty()) {
			return;
		}
		for (String name : saved.split(",")) {
			try {
				addFloorRock(Ground.valueOf(name));
			} catch (IllegalArgumentException ignored) {
				// A renamed ground value is dropped rather than crashing the load.
			}
		}
	}

	/**
	 * What is missing for one advance. Crafted segments are optional: without a full ring the lining is cut from mined
	 * rock and cobble. {@code incomingFloorRock} is suitable rock in the face, already capped. {@code incomingWater}
	 * is pumped water that will fit in the tank before the quench is paid.
	 */
	public String missing(FloorPiece[] floor, int caveCobble, int lavaBlocks, int incomingFloorRock, int incomingWater) {
		if (rail < DriveCourse.rails(floor)) {
			return "mine rail";
		}
		int gravelNeed = 0;
		int rockNeed = 0;
		for (FloorPiece piece : floor) {
			if (piece.usesGravel()) {
				gravelNeed++;
			} else {
				rockNeed++;
			}
		}
		if (gravel < gravelNeed) {
			return "gravel";
		}
		int liningStone = segments >= DriveCourse.SEGMENTS_PER_RING ? 0 : DriveCourse.SEGMENTS_PER_RING;
		int shortfall = Math.max(0, rockNeed + liningStone - floorRock.size() - Math.max(0, incomingFloorRock));
		if (cobble < shortfall) {
			return liningStone > 0 && floorRock.size() + incomingFloorRock < rockNeed ? "floor rock" : "lining stone";
		}
		if (cobble < shortfall + Math.max(0, caveCobble)) {
			return "cobble";
		}
		if (water + Math.max(0, incomingWater) < lavaBlocks) {
			return "quench water";
		}
		return null;
	}

	/**
	 * Spends one advance. The lining array is null when crafted segments were used; otherwise each entry is the stone
	 * block for that cell. Floor gravel ballast reports {@link Ground#GRAVEL}.
	 */
	public Spent consume(FloorPiece[] floor, int caveCobble, int lavaBlocks, List<Ground> mined) {
		boolean segments = this.segments >= DriveCourse.SEGMENTS_PER_RING;
		if (segments) {
			this.segments -= DriveCourse.SEGMENTS_PER_RING;
		}
		rail -= DriveCourse.rails(floor);
		int gravelNeed = 0;
		for (FloorPiece piece : floor) {
			if (piece.usesGravel()) {
				gravelNeed++;
			}
		}
		gravel -= gravelNeed;
		if (mined != null) {
			for (Ground ground : mined) {
				addFloorRock(ground);
			}
		}
		Ground[] used = new Ground[floor.length];
		int fromCobble = 0;
		for (int i = 0; i < floor.length; i++) {
			if (floor[i].usesGravel()) {
				used[i] = Ground.GRAVEL;
			} else if (!floorRock.isEmpty()) {
				used[i] = floorRock.removeFirst();
			} else {
				used[i] = Ground.COBBLE;
				fromCobble++;
			}
		}
		Ground[] lining = null;
		if (!segments) {
			lining = new Ground[DriveCourse.SEGMENTS_PER_RING];
			for (int i = 0; i < lining.length; i++) {
				if (!floorRock.isEmpty()) {
					lining[i] = floorRock.removeFirst();
				} else {
					lining[i] = Ground.COBBLE;
					fromCobble++;
				}
			}
		}
		cobble -= fromCobble + caveCobble;
		water -= lavaBlocks;
		return new Spent(used, lining);
	}
}
