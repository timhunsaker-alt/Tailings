package dev.wildcatter.tailings.mine;

import java.util.ArrayList;
import java.util.List;
import java.util.random.RandomGenerator;

/**
 * Where a loose block comes to rest (docs/multiblocks/open-pit.md, The settle rule): muck from a blast, a tipped truck load, a
 * dragline's cast. It falls straight down to the first solid block. It then rolls on toward any of its four sides where resting
 * would leave the ground too steep, and falls again:
 * <ul>
 * <li>a step of 2 to the next column (a block world's 1 in 1, 45 degrees, the steepest it can stand);</li>
 * <li>or 4 up in a run of 4 columns: so a long slope stands at 3 in 4, <b>37 degrees</b>, real broken rock's angle of repose
 * (a written exception, decided with Tim 2026-11-09; it replaced a 1-in-3 chance to creep on, which measured 43 degrees).</li>
 * </ul>
 * It goes the steepest way, a tie by chance, and stops after 64 rolls.
 */
public final class Settle {
	/** The most cells a block falls before the rule gives up on finding a floor. */
	public static final int MOST_FALL = 384;
	public static final int MOST_ROLLS = 64;
	/** The most a column may stand above the one {@code k} away, for k = 1 to 4: 1 in 1 near, 3 in 4 over a run. */
	private static final int[] MOST_RISE = {0, 1, 2, 3, 3};

	/** The world as the rule sees it. */
	public interface Ground {
		/** True where a loose block can be: air, a fluid or a plant. */
		boolean free(int x, int y, int z);

		/** The lowest y a block may rest at. */
		int floor();
	}

	public record Cell(int x, int y, int z) {
	}

	private static final int[][] SIDES = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

	private Settle() {
	}

	/**
	 * Where a block let go at {@code x, y, z} rests, or null when that cell is not free. The caller places it there; the next block
	 * then sees it.
	 */
	public static Cell rest(Ground ground, int x, int y, int z, RandomGenerator random) {
		if (!ground.free(x, y, z)) {
			return null;
		}
		y = fall(ground, x, y, z);
		int fromX = Integer.MIN_VALUE, fromZ = Integer.MIN_VALUE;
		for (int roll = 0; roll < MOST_ROLLS; roll++) {
			List<Cell> ways = new ArrayList<>();
			int worst = 0;
			for (int[] side : SIDES) {
				int nx = x + side[0], nz = z + side[1];
				if ((nx == fromX && nz == fromZ) || !ground.free(nx, y, nz)) {
					continue;
				}
				int first = fall(ground, nx, y, nz);
				// resting here, this column's top is y + 1: how far past the steepest it may stand is it, looking k columns out?
				int excess = 0;
				for (int k = 1; k < MOST_RISE.length; k++) {
					int kx = x + side[0] * k, kz = z + side[1] * k;
					if (!ground.free(kx, y, kz)) {
						break;
					}
					int top = k == 1 ? first : fall(ground, kx, y, kz);
					excess = Math.max(excess, (y + 1) - top - MOST_RISE[k]);
				}
				if (excess <= 0) {
					continue;
				}
				if (excess > worst) {
					ways.clear();
					worst = excess;
				}
				if (excess == worst) {
					ways.add(new Cell(nx, first, nz));
				}
			}
			if (ways.isEmpty()) {
				break;
			}
			Cell next = ways.get(random.nextInt(ways.size()));
			fromX = x;
			fromZ = z;
			x = next.x();
			y = next.y();
			z = next.z();
		}
		return new Cell(x, y, z);
	}

	/** The y a block falls to from {@code y} in its column. */
	static int fall(Ground ground, int x, int y, int z) {
		int floor = ground.floor();
		for (int i = 0; i < MOST_FALL && y > floor && ground.free(x, y - 1, z); i++) {
			y--;
		}
		return y;
	}
}
