package dev.wildcatter.tailings.pit;

import org.jspecify.annotations.Nullable;

/**
 * Surveys a haul-road leg between two stakes (docs/multiblocks/open-pit.md, P2a): its grade, whether a truck's box is clear along it,
 * and whether it is wide enough for two-way traffic. Plain Java: the world is a {@link Ground}.
 */
public final class Survey {
	/** Steepest a haul road may be (spec: real haul roads 8-10 %). */
	public static final double MOST_GRADE = 0.10;
	/** Two-way traffic needs 3 truck widths (spec, the SME rule). */
	public static final double TWO_WAY_WIDTHS = 3;

	public interface Ground {
		/** A block a truck cannot drive through. */
		boolean solid(int x, int y, int z);
	}

	/** A leg's survey: its grade (rise over run), the first blocked cell or null, and whether it is one-way. */
	public record Leg(double grade, int @Nullable [] blockedAt, boolean oneWay) {
		public boolean steep() {
			return Math.abs(grade) > MOST_GRADE + 1e-9;
		}

		/** What is wrong with it for a truck, or null. */
		public @Nullable String problem() {
			if (steep()) {
				return String.format(java.util.Locale.ROOT, "grade %.0f %% (most 10 %%)", Math.abs(grade) * 100);
			}
			if (blockedAt != null) {
				return "blocked at " + blockedAt[0] + ", " + blockedAt[1] + ", " + blockedAt[2];
			}
			return null;
		}
	}

	private Survey() {
	}

	/**
	 * Surveys from the stake at (ax, ay, az) to the one at (bx, by, bz); a stake stands on the road, so its y is the road's first air.
	 * A truck {@code width} wide and {@code height} high must fit along the line; two-way needs {@code TWO_WAY_WIDTHS} widths clear.
	 */
	public static Leg leg(Ground ground, int ax, int ay, int az, int bx, int by, int bz, double width, double height) {
		double dx = bx - ax, dz = bz - az, run = Math.hypot(dx, dz);
		double grade = run < 1e-6 ? 0 : (by - ay) / run;
		if (run < 1e-6) {
			return new Leg(0, null, false);
		}
		double ux = dx / run, uz = dz / run;   // along
		double rx = -uz, rz = ux;              // across
		int[] blocked = null;
		boolean oneWay = false;
		int tall = (int) Math.ceil(height);
		for (double s = 0; s <= run && blocked == null; s += 0.5) {
			double cx = ax + 0.5 + ux * s, cz = az + 0.5 + uz * s;
			int floor = (int) Math.floor(ay + (by - ay) * (s / run));
			// the truck's own box: it climbs a block at a time, so the cell at road level may be a step
			for (double t = -width / 2; t <= width / 2 && blocked == null; t += 0.5) {
				int x = (int) Math.floor(cx + rx * t), z = (int) Math.floor(cz + rz * t);
				for (int y = floor + 1; y <= floor + tall; y++) {
					if (ground.solid(x, y, z)) {
						blocked = new int[] {x, y, z};
						break;
					}
				}
			}
			// two-way: three widths clear at the truck's height above the road
			if (!oneWay) {
				double half = width * TWO_WAY_WIDTHS / 2;
				for (double t = -half; t <= half; t += 0.5) {
					int x = (int) Math.floor(cx + rx * t), z = (int) Math.floor(cz + rz * t);
					if (ground.solid(x, floor + 1, z) || ground.solid(x, floor + 2, z)) {
						oneWay = true;
						break;
					}
				}
			}
		}
		return new Leg(grade, blocked, oneWay);
	}
}
