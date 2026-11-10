package dev.wildcatter.tailings.paste;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The void a fill outlet fills (docs/multiblocks/paste-backfill.md, "The void"): every cell it reaches through open cells (air or
 * water) and paste, held in by everything else (rock, fill bulkheads, any block). Plain Java, unit-tested in {@code FillVoidTest}.
 *
 * <p>It is NOT ENCLOSED when it reaches open sky, ground not loaded, or more than {@link PasteFill#MOST_CELLS} cells. The cells it fills
 * are its open ones no higher than the outlet, lowest first, and on one level the nearest to the outlet first.
 */
public final class FillVoid {
	/** What a cell is, to the scan. */
	public enum Cell {
		/** Rock, a bulkhead, any block: the void's wall. */
		WALL,
		/** Air or water under cover: paste can go here. */
		OPEN,
		/** Air or water open to the sky: the void is not closed. */
		SKY,
		/** Wet paste. */
		WET,
		/** Set paste. */
		SET,
		/** Ground not loaded: the scan cannot know. */
		UNKNOWN
	}

	public interface Ground {
		Cell at(int x, int y, int z);
	}

	public enum Status {
		ENCLOSED,
		/** The void reaches open sky. */
		OPEN_TO_SKY,
		/** More than {@link PasteFill#MOST_CELLS} cells. */
		TOO_BIG,
		/** It reaches ground that is not loaded. */
		UNLOADED,
		/** The outlet has no open cell or paste beside it. */
		NO_VOID
	}

	/**
	 * A scan: its status; the void's cells, open, wet and set; and, when enclosed, the cells to fill, packed by {@link #pack}, in the
	 * order to fill them.
	 */
	public record Result(Status status, int cells, int open, int wet, int set, long[] toFill) {
		public boolean enclosed() {
			return status == Status.ENCLOSED;
		}

		/** Enclosed and nothing left to fill to the outlet's level. */
		public boolean full() {
			return enclosed() && toFill.length == 0;
		}
	}

	private static final int[][] SIDES = {{0, -1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}};

	private FillVoid() {
	}

	/** One cell as a long: 26 bits of x and z, 12 of y (offset by 2048), as the game's own packing. */
	public static long pack(int x, int y, int z) {
		return ((long) (x & 0x3FFFFFF) << 38) | ((long) (z & 0x3FFFFFF) << 12) | ((y + 2048) & 0xFFF);
	}

	public static int x(long packed) {
		return (int) (packed >> 38);
	}

	public static int y(long packed) {
		return (int) (packed & 0xFFF) - 2048;
	}

	public static int z(long packed) {
		return (int) (packed << 26 >> 38);
	}

	/** Scans the void beside an outlet at {@code ox, oy, oz}. */
	public static Result scan(Ground ground, int ox, int oy, int oz) {
		Set<Long> seen = new HashSet<>();
		ArrayDeque<long[]> queue = new ArrayDeque<>();
		List<long[]> fill = new ArrayList<>();   // {packed, y, order}
		int open = 0, wet = 0, set = 0;
		Status status = Status.ENCLOSED;
		seen.add(pack(ox, oy, oz));
		for (int[] s : SIDES) {
			int x = ox + s[0], y = oy + s[1], z = oz + s[2];
			if (ground.at(x, y, z) != Cell.WALL && seen.add(pack(x, y, z))) {
				queue.add(new long[] {x, y, z});
			}
		}
		if (queue.isEmpty()) {
			return new Result(Status.NO_VOID, 0, 0, 0, 0, new long[0]);
		}
		long order = 0;
		while (!queue.isEmpty()) {
			long[] c = queue.poll();
			int x = (int) c[0], y = (int) c[1], z = (int) c[2];
			Cell cell = ground.at(x, y, z);
			switch (cell) {
				case SKY -> status = Status.OPEN_TO_SKY;
				case UNKNOWN -> status = Status.UNLOADED;
				case OPEN -> {
					open++;
					if (y <= oy) {
						fill.add(new long[] {pack(x, y, z), y, order++});
					}
				}
				case WET -> wet++;
				case SET -> set++;
				default -> {
				}
			}
			if (status != Status.ENCLOSED) {
				break;
			}
			if (open + wet + set > PasteFill.MOST_CELLS) {
				status = Status.TOO_BIG;
				break;
			}
			for (int[] s : SIDES) {
				int nx = x + s[0], ny = y + s[1], nz = z + s[2];
				long key = pack(nx, ny, nz);
				if (!seen.contains(key) && ground.at(nx, ny, nz) != Cell.WALL) {
					seen.add(key);
					queue.add(new long[] {nx, ny, nz});
				}
			}
		}
		int cells = open + wet + set;
		if (status != Status.ENCLOSED) {
			return new Result(status, cells, open, wet, set, new long[0]);
		}
		fill.sort(Comparator.<long[]>comparingLong(f -> f[1]).thenComparingLong(f -> f[2]));
		long[] toFill = new long[fill.size()];
		for (int i = 0; i < toFill.length; i++) {
			toFill[i] = fill.get(i)[0];
		}
		return new Result(status, cells, open, wet, set, toFill);
	}

	/** The outlet's readout for a scan. */
	public static String describe(Result r) {
		return switch (r.status()) {
			case OPEN_TO_SKY -> "NOT ENCLOSED: the void reaches open sky";
			case TOO_BIG -> "NOT ENCLOSED: the void is over " + PasteFill.MOST_CELLS + " m3 (close it with fill bulkheads)";
			case UNLOADED -> "NOT ENCLOSED: the void runs into ground that is not loaded";
			case NO_VOID -> "NO VOID: nothing open beside the outlet";
			case ENCLOSED -> r.full() ? String.format(java.util.Locale.ROOT, "FULL: %d m3 void, %d m3 wet, %d m3 set", r.cells(), r.wet(), r.set())
					: String.format(java.util.Locale.ROOT, "%d m3 void: %d m3 open, %d m3 wet, %d m3 set; %d m3 to fill to the outlet", r.cells(), r.open(), r.wet(), r.set(), r.toFill().length);
		};
	}
}
