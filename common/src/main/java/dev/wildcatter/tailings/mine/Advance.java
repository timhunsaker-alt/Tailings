package dev.wildcatter.tailings.mine;

import java.util.LinkedHashMap;
import java.util.Map;

/** One block of drive, or the reason the drill stopped before cutting it. */
public final class Advance {
	public enum Stop {
		NONE,
		TARGET_REACHED,
		ORE,
		CABLE,
		LIQUID,
		EMPTY,
		BUFFER_FULL,
		BLOCKED
	}

	private final Stop stop;
	private final String detail;
	private final Map<Ground, Integer> muck = new LinkedHashMap<>();
	private final Map<String, Integer> loads = new LinkedHashMap<>();
	private final int pumpedWater;
	private final int quenchedLava;
	private boolean placed;
	private FloorPiece[] floor = new FloorPiece[0];
	private Ground[] floorRock = new Ground[0];
	private Ground[] lining;

	private Advance(Stop stop, String detail, int pumpedWater, int quenchedLava) {
		this.stop = stop;
		this.detail = detail;
		this.pumpedWater = pumpedWater;
		this.quenchedLava = quenchedLava;
	}

	public static Advance stopped(Stop stop, String detail) {
		return new Advance(stop, detail, 0, 0);
	}

	public static Advance drove(int pumpedWater, int quenchedLava) {
		return new Advance(Stop.NONE, "", pumpedWater, quenchedLava);
	}

	public Stop stop() {
		return stop;
	}

	public String detail() {
		return detail;
	}

	public Map<Ground, Integer> muck() {
		return muck;
	}

	public void addMuck(Ground ground, String itemId, int n) {
		if (n > 0 && ground.isMuck()) {
			muck.merge(ground, n, Integer::sum);
			loads.merge(itemId == null || itemId.isEmpty() ? ground.name().toLowerCase(java.util.Locale.ROOT) : itemId, n, Integer::sum);
		}
	}

	public Map<String, Integer> loads() {
		return loads;
	}

	public FloorPiece[] floor() {
		return floor;
	}

	public Ground[] floorRock() {
		return floorRock;
	}

	public void recordFloor(FloorPiece[] floor, Ground[] rock) {
		this.floor = floor.clone();
		this.floorRock = rock.clone();
	}

	/** Null when the ring was crafted segments. */
	public void recordLining(Ground[] lining) {
		this.lining = lining == null ? null : lining.clone();
	}

	public Ground[] lining() {
		return lining;
	}

	public boolean segmentRing() {
		return lining == null;
	}

	public int pumpedWater() {
		return pumpedWater;
	}

	public int quenchedLava() {
		return quenchedLava;
	}

	public boolean placed() {
		return placed;
	}

	public void markPlaced() {
		placed = true;
	}
}
