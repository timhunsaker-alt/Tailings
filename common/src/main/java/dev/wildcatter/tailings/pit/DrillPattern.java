package dev.wildcatter.tailings.pit;

import java.util.ArrayList;
import java.util.List;

/**
 * A drill pattern (docs/multiblocks/open-pit-p3-blasting.md, Running a pattern): rows x columns of holes, burden apart row to row and
 * spacing apart in a row, staggered (each other row offset half a spacing) or square. Row 1 is the front row, at the free face, and
 * the rows run back from it. The drill walks it row by row, a serpentine.
 *
 * <p>The pattern runs square to the world's grid (as built): {@code (backX, backZ)} is the unit step from the face back into the bench
 * and {@code (acrossX, acrossZ)} the unit step along a row.
 */
public final class DrillPattern {
	/** One hole: its row and place in the row (from 0, as the logger counts them) and its column of blocks. */
	public record Hole(int row, int col, int x, int z) {
	}

	private DrillPattern() {
	}

	public static List<Hole> holes(int originX, int originZ, int backX, int backZ, int acrossX, int acrossZ, int rows, int cols, int burden, int spacing,
			boolean staggered) {
		List<Hole> out = new ArrayList<>();
		for (int r = 0; r < rows; r++) {
			int shift = staggered && r % 2 == 1 ? spacing / 2 : 0;
			for (int i = 0; i < cols; i++) {
				int c = r % 2 == 0 ? i : cols - 1 - i;   // the serpentine: every other row walked back
				int along = c * spacing + shift;
				out.add(new Hole(r, c, originX + backX * r * burden + acrossX * along, originZ + backZ * r * burden + acrossZ * along));
			}
		}
		return out;
	}

	/** Time to drill a pattern's hole of {@code depth} after tramming {@code metres}: depth x rate + set-up + tramming, in ticks. */
	public static int holeTicks(int depth, double metres) {
		return depth * Blasting.TICKS_PER_M + Blasting.SETUP_TICKS + (int) Math.ceil(metres / Blasting.TRAM_MPS * 20);
	}
}
