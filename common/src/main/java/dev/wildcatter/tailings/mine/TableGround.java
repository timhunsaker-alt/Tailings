package dev.wildcatter.tailings.mine;

import java.util.HashMap;
import java.util.Map;

/** A ground grid for tests and for anything that is not a world. Unset cells are air. */
public final class TableGround implements GroundGrid {
	private record Key(int x, int y, int z) {
	}

	private final Map<Key, Ground> cells = new HashMap<>();
	private final Map<Key, FloorPiece> floors = new HashMap<>();
	private final Map<Key, Ground> rocks = new HashMap<>();

	@Override
	public Ground get(int x, int y, int z) {
		return cells.getOrDefault(new Key(x, y, z), Ground.AIR);
	}

	@Override
	public void set(int x, int y, int z, Ground ground) {
		Key key = new Key(x, y, z);
		if (ground == null || ground == Ground.AIR) {
			cells.remove(key);
		} else {
			cells.put(key, ground);
		}
	}

	@Override
	public void placeFloor(int x, int y, int z, FloorPiece piece, Ground rock) {
		Key key = new Key(x, y, z);
		cells.put(key, Ground.FLOOR);
		floors.put(key, piece);
		rocks.put(key, rock);
	}

	@Override
	public FloorPiece floorPiece(int x, int y, int z) {
		return floors.get(new Key(x, y, z));
	}

	@Override
	public Ground floorRock(int x, int y, int z) {
		return rocks.get(new Key(x, y, z));
	}

	public int count(Ground ground) {
		int n = 0;
		for (Ground cell : cells.values()) {
			if (cell == ground) {
				n++;
			}
		}
		return n;
	}
}
