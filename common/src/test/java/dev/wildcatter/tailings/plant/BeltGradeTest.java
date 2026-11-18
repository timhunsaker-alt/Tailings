package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A belt run on a grade is one straight belt between its steps, and every block meets the next (crushing-circuit.md). */
class BeltGradeTest {
	/** A run with {@code flat} level blocks, then steps of {@code way} every {@code every} blocks, {@code steps} times, then {@code flat}. */
	static int[] run(int flat, int every, int steps, int way) {
		int[] levels = new int[flat * 2 + every * steps];
		int level = 0;
		for (int i = 0; i < levels.length; i++) {
			if (i >= flat && i < flat + every * steps && (i - flat) % every == 0) {
				level += way;
			}
			levels[i] = level;
		}
		return levels;
	}

	static void meets(int[] levels, double[][] p) {
		for (int i = 0; i + 1 < levels.length; i++) {
			assertEquals(levels[i] + p[i][2], levels[i + 1] + p[i + 1][0], 1e-9, "block " + i + " meets block " + (i + 1));
		}
		assertEquals(BeltGrade.FLAT, p[0][0], 1e-9, "the tail end is flat");
		assertEquals(BeltGrade.FLAT, p[levels.length - 1][2], 1e-9, "the head end is flat");
	}

	@Test
	void everyGradeIsOneStraightBelt() {
		for (int every = 1; every <= 4; every++) {
			for (int way : new int[] {1, -1}) {
				int[] levels = run(6, every, 5, way);
				double[][] p = BeltGrade.profile(levels);
				meets(levels, p);
				// inside the grade (from the first step to the last) each half block climbs 1/every/2
				for (int i = 6; i < 6 + every * 4; i++) {
					assertEquals(way / (2.0 * every), p[i][1] - p[i][0], 1e-9, "1 in " + every + " block " + i);
					assertEquals(way / (2.0 * every), p[i][2] - p[i][1], 1e-9, "1 in " + every + " block " + i);
					assertTrue(BeltGrade.sloped(p[i]));
				}
				assertFalse(BeltGrade.sloped(p[0]), "the flat stays flat");
				assertFalse(BeltGrade.sloped(p[levels.length - 1]));
			}
		}
	}

	@Test
	void unevenStepsPeaksAndShortRunsStillMeet() {
		int[][] runs = {{0, 0, 1, 1, 1, 2, 3, 3, 4, 4, 4, 4, 4, 4, 5, 5}, {0, 1, 0}, {0, 0, 1, 1, 0, 0}, {3, 2, 1, 2, 3}, {0, 1}, {0}, {0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 1}};
		for (int[] levels : runs) {
			double[][] p = BeltGrade.profile(levels);
			meets(levels, p);
			for (double[] h : p) {
				for (double v : h) {
					assertTrue(v >= 0.25 - 1e-9 && v <= 1.25 + 1e-9, "the belt stays within a quarter block of its cell: " + v);
				}
			}
		}
	}
}
