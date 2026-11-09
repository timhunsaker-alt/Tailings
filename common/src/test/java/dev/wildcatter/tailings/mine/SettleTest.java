package dev.wildcatter.tailings.mine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** The settle rule on a flat floor at y 0 (docs/multiblocks/open-pit.md). */
class SettleTest {
	/** Solid below y 0 and wherever a block was placed. */
	private static final class Floor implements Settle.Ground {
		final Set<Settle.Cell> placed = new HashSet<>();
		final Set<Settle.Cell> holes = new HashSet<>();

		@Override
		public boolean free(int x, int y, int z) {
			if (placed.contains(new Settle.Cell(x, y, z))) {
				return false;
			}
			if (y >= 0) {
				return true;
			}
			return holes.contains(new Settle.Cell(x, y, z));
		}

		@Override
		public int floor() {
			return -64;
		}


		int height(int x, int z) {
			int h = 0;
			while (!free(x, h, z)) {
				h++;
			}
			return h;
		}

		Settle.Cell drop(int x, int y, int z, Random random) {
			Settle.Cell at = Settle.rest(this, x, y, z, random);
			placed.add(at);
			return at;
		}
	}

	@Test
	void aBlockFallsToTheFloor() {
		Floor floor = new Floor();
		assertEquals(new Settle.Cell(0, 0, 0), floor.drop(0, 20, 0, new Random(1)));
	}

	@Test
	void aSolidStartIsRefused() {
		Floor floor = new Floor();
		assertNull(Settle.rest(floor, 0, -1, 0, new Random(1)));
	}

	@Test
	void aSecondBlockRollsOffAStackOfTwo() {
		Floor floor = new Floor();
		Random random = new Random(3);
		floor.drop(0, 10, 0, random);
		floor.drop(0, 10, 0, random);
		Settle.Cell third = floor.drop(0, 10, 0, random);
		// two high would leave a step of 2 to the floor beside it, so the third block cannot stay on top
		assertTrue(third.y() <= 1, "rested at " + third);
	}

	@Test
	void aPileNeverStandsSteeperThanOneInOne() {
		Floor floor = new Floor();
		Random random = new Random(7);
		for (int i = 0; i < 400; i++) {
			assertNotNull(floor.drop(0, 40, 0, random));
		}
		for (int x = -15; x <= 15; x++) {
			for (int z = -15; z <= 15; z++) {
				int h = floor.height(x, z);
				assertTrue(Math.abs(h - floor.height(x + 1, z)) <= 1 && Math.abs(h - floor.height(x, z + 1)) <= 1,
						"a step of 2 or more at " + x + ", " + z);
			}
		}
	}

	@Test
	void aPileStandsNearRealBrokenRock() {
		Floor floor = new Floor();
		Random random = new Random(11);
		int n = 2_000;
		for (int i = 0; i < n; i++) {
			floor.drop(0, 80, 0, random);
		}
		int peak = floor.height(0, 0);
		// along the four axes from the peak to the pile's toe: a pile at 1 in 1 reaches as far out as it is high
		double reach = 0;
		for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
			int r = 0;
			while (floor.height(d[0] * (r + 1), d[1] * (r + 1)) > 0) {
				r++;
			}
			reach += (r + 1) / 4.0;
		}
		double radius = reach;
		double degrees = Math.toDegrees(Math.atan(peak / radius));
		System.out.println("[settle] " + n + " blocks: peak " + peak + ", radius " + String.format("%.1f", radius) + ", slope "
				+ String.format("%.1f", degrees) + " degrees");
		assertTrue(degrees > 33 && degrees < 41, "slope " + degrees);
	}

	@Test
	void aHoleBesideTheTipFillsFirst() {
		Floor floor = new Floor();
		for (int x = 2; x <= 4; x++) {
			for (int z = -1; z <= 1; z++) {
				floor.holes.add(new Settle.Cell(x, -1, z));
				floor.holes.add(new Settle.Cell(x, -2, z));
			}
		}
		Random random = new Random(5);
		for (int i = 0; i < 40; i++) {
			floor.drop(0, 10, 0, random);
		}
		for (int x = 2; x <= 4; x++) {
			for (int z = -1; z <= 1; z++) {
				assertTrue(floor.placed.contains(new Settle.Cell(x, -2, z)), "the hole's floor at " + x + ", " + z + " is still open");
			}
		}
	}
}
