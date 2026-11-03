package dev.wildcatter.tailings.mine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/** The cab's slots, plus rock held back from the cut to make floor pieces. */
public final class DrillStock {
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
	 * What is missing for one advance. {@code incomingFloorRock} is suitable rock in the face about to be cut, already
	 * capped at the number of rock pieces. Cobble covers both the shortfall and the cave fill; it is not counted twice.
	 */
	public String missing(FloorPiece[] floor, int caveCobble, int lavaBlocks, int incomingFloorRock) {
		if (segments < DriveCourse.SEGMENTS_PER_RING) {
			return "lining segments";
		}
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
		int shortfall = Math.max(0, rockNeed - floorRock.size() - Math.max(0, incomingFloorRock));
		if (cobble < shortfall) {
			return "floor rock";
		}
		if (cobble < shortfall + Math.max(0, caveCobble)) {
			return "cobble";
		}
		if (water < lavaBlocks) {
			return "quench water";
		}
		return null;
	}

	/** Spends one advance and returns the rock each floor column was cut from. Gravel ballast reports {@link Ground#GRAVEL}. */
	public Ground[] consume(FloorPiece[] floor, int caveCobble, int lavaBlocks, List<Ground> mined) {
		segments -= DriveCourse.SEGMENTS_PER_RING;
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
		cobble -= fromCobble + caveCobble;
		water -= lavaBlocks;
		return used;
	}
}
