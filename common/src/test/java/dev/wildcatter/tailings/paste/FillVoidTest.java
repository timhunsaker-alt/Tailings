package dev.wildcatter.tailings.paste;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** The void scan (paste-backfill.md, "The void"). */
class FillVoidTest {
	/** Rock everywhere but the cells set; above {@code sky} the open cells see the sky. */
	private static final class World implements FillVoid.Ground {
		final Map<Long, FillVoid.Cell> cells = new HashMap<>();
		int sky = Integer.MAX_VALUE;

		void box(int x0, int y0, int z0, int x1, int y1, int z1, FillVoid.Cell cell) {
			for (int x = x0; x <= x1; x++) {
				for (int y = y0; y <= y1; y++) {
					for (int z = z0; z <= z1; z++) {
						cells.put(FillVoid.pack(x, y, z), cell);
					}
				}
			}
		}

		@Override
		public FillVoid.Cell at(int x, int y, int z) {
			FillVoid.Cell c = cells.getOrDefault(FillVoid.pack(x, y, z), FillVoid.Cell.WALL);
			return c == FillVoid.Cell.OPEN && y >= sky ? FillVoid.Cell.SKY : c;
		}
	}

	@Test
	void packingRoundTrips() {
		for (int[] p : new int[][] {{0, 0, 0}, {-30_000_000, -64, 29_999_999}, {12345, 319, -54321}, {-1, -1, -1}}) {
			long packed = FillVoid.pack(p[0], p[1], p[2]);
			assertEquals(p[0], FillVoid.x(packed));
			assertEquals(p[1], FillVoid.y(packed));
			assertEquals(p[2], FillVoid.z(packed));
		}
	}

	@Test
	void aClosedDriveBranchFillsFromTheBottomUp() {
		// a 10 x 6 x 6 branch, the outlet in its top layer at one end
		World w = new World();
		w.box(0, 0, 0, 9, 5, 5, FillVoid.Cell.OPEN);
		w.cells.put(FillVoid.pack(0, 5, 0), FillVoid.Cell.WALL);   // the outlet's own cell
		FillVoid.Result r = FillVoid.scan(w, 0, 5, 0);
		assertTrue(r.enclosed());
		assertEquals(359, r.cells());
		assertEquals(359, r.toFill().length);
		int lastY = Integer.MIN_VALUE;
		for (long cell : r.toFill()) {
			assertTrue(FillVoid.y(cell) >= lastY, "lowest first");
			lastY = FillVoid.y(cell);
		}
		// the nearest first on the floor
		assertEquals(0, FillVoid.y(r.toFill()[0]));
		assertEquals(0, FillVoid.x(r.toFill()[0]));
		assertEquals(0, FillVoid.z(r.toFill()[0]));
	}

	@Test
	void itFillsOnlyToTheOutletsLevel() {
		World w = new World();
		w.box(0, 0, 0, 4, 4, 4, FillVoid.Cell.OPEN);
		w.cells.put(FillVoid.pack(2, 2, 2), FillVoid.Cell.WALL);
		FillVoid.Result r = FillVoid.scan(w, 2, 2, 2);
		assertEquals(124, r.cells());
		assertEquals(74, r.toFill().length);   // the bottom three layers, less the outlet's cell
	}

	@Test
	void pasteCountsInTheVoidButIsNotFilledAgain() {
		World w = new World();
		w.box(0, 0, 0, 3, 3, 3, FillVoid.Cell.OPEN);
		w.box(0, 0, 0, 3, 0, 3, FillVoid.Cell.SET);
		w.box(0, 1, 0, 3, 1, 3, FillVoid.Cell.WET);
		w.cells.put(FillVoid.pack(0, 3, 0), FillVoid.Cell.WALL);
		FillVoid.Result r = FillVoid.scan(w, 0, 3, 0);
		assertEquals(63, r.cells());
		assertEquals(16, r.set());
		assertEquals(16, r.wet());
		assertEquals(31, r.toFill().length);
		assertEquals(2, FillVoid.y(r.toFill()[0]));
	}

	@Test
	void aFullVoidReadsFull() {
		World w = new World();
		w.box(0, 0, 0, 2, 1, 2, FillVoid.Cell.WET);
		w.cells.put(FillVoid.pack(1, 1, 1), FillVoid.Cell.WALL);
		FillVoid.Result r = FillVoid.scan(w, 1, 1, 1);
		assertTrue(r.full());
		assertTrue(FillVoid.describe(r).startsWith("FULL"));
	}

	@Test
	void aVoidOpenToTheSkyIsNotEnclosed() {
		World w = new World();
		w.box(0, 0, 0, 3, 3, 3, FillVoid.Cell.OPEN);
		w.box(1, 4, 1, 1, 10, 1, FillVoid.Cell.OPEN);   // a raise up to the surface
		w.sky = 8;
		w.cells.put(FillVoid.pack(0, 3, 0), FillVoid.Cell.WALL);
		FillVoid.Result r = FillVoid.scan(w, 0, 3, 0);
		assertEquals(FillVoid.Status.OPEN_TO_SKY, r.status());
		assertFalse(r.enclosed());
		assertEquals(0, r.toFill().length);
		assertTrue(FillVoid.describe(r).startsWith("NOT ENCLOSED"));
	}

	@Test
	void aVoidOver20000CellsIsNotEnclosed() {
		World w = new World();
		w.box(0, 0, 0, 39, 9, 50, FillVoid.Cell.OPEN);   // 20,400 cells
		w.cells.put(FillVoid.pack(0, 9, 0), FillVoid.Cell.WALL);
		assertEquals(FillVoid.Status.TOO_BIG, FillVoid.scan(w, 0, 9, 0).status());
		w.box(0, 0, 50, 39, 9, 50, FillVoid.Cell.WALL);   // a bulkhead across it: 20,000 less the outlet
		assertEquals(FillVoid.Status.ENCLOSED, FillVoid.scan(w, 0, 9, 0).status());
	}

	@Test
	void unloadedGroundIsNotEnclosed() {
		World w = new World();
		w.box(0, 0, 0, 3, 3, 3, FillVoid.Cell.OPEN);
		w.cells.put(FillVoid.pack(3, 0, 3), FillVoid.Cell.UNKNOWN);
		w.cells.put(FillVoid.pack(0, 3, 0), FillVoid.Cell.WALL);
		assertEquals(FillVoid.Status.UNLOADED, FillVoid.scan(w, 0, 3, 0).status());
	}

	@Test
	void anOutletInRockHasNoVoid() {
		assertEquals(FillVoid.Status.NO_VOID, FillVoid.scan(new World(), 0, 0, 0).status());
	}
}
