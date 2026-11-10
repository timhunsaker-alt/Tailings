package dev.wildcatter.tailings.pit;

/**
 * What lies behind a DUMP POINT stake (docs/multiblocks/open-pit-p4-dry-stack.md, Waste dumps and Berms): a crest, a drop of 3 m or
 * more within 10 m; and a berm, a ridge at least mid-axle high across the truck's tail within 3 m. Plain Java: the world is a
 * {@link Ground}, and the line is walked in half metres.
 */
public final class Crest {
	/** The world as a crest sees it. */
	public interface Ground {
		/** A block a truck stands on or stops against. */
		boolean solid(int x, int y, int z);
	}

	/** What is behind the stake. */
	public enum Kind {
		/** No drop of 3 m within 10 m: a paddock dump, tipped on the flat. */
		PADDOCK,
		/** A crest with a berm in front of it: back to the berm and tip over it. */
		BERM,
		/** A crest with no berm: a self-driving truck will not back to it. */
		NO_BERM
	}

	/** The finding: its kind, and metres from the stake to the crest and to the berm (NaN where there is none). */
	public record Look(Kind kind, double crestAt, double bermAt) {
	}

	private Crest() {
	}

	/** A drop of {@link DryStack#CREST_DROP} or more under the column at (x, z), from a road whose first air is {@code y}. */
	public static boolean drop(Ground ground, int x, int y, int z) {
		for (int down = 1; down <= DryStack.CREST_DROP; down++) {
			if (ground.solid(x, y - down, z)) {
				return false;
			}
		}
		return true;
	}

	/** A ridge {@code high} blocks tall standing on the road at (x, z). */
	public static boolean ridge(Ground ground, int x, int y, int z, int high) {
		for (int up = 0; up < high; up++) {
			if (!ground.solid(x, y + up, z)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Looks behind the stake at (sx, y, sz) (the centre of its cell, y the road's first air) along the unit way (dx, dz), across a
	 * truck {@code width} wide, for a berm {@code bermHigh} blocks tall. A crest counts on the centre line or either side of the tail;
	 * a berm only where it spans the tail (its centre and both sides 1 m in from its corners).
	 */
	public static Look look(Ground ground, double sx, int y, double sz, double dx, double dz, double width, int bermHigh) {
		double rx = -dz, rz = dx;
		double side = Math.max(0, width / 2 - 1);
		double[] across = {0, -side, side};
		double crest = Double.NaN;
		for (double s = 0; s <= DryStack.CREST_SEARCH && Double.isNaN(crest); s += 0.5) {
			for (double t : across) {
				int x = (int) Math.floor(sx + dx * s + rx * t), z = (int) Math.floor(sz + dz * s + rz * t);
				if (drop(ground, x, y, z)) {
					crest = s;
					break;
				}
			}
		}
		if (Double.isNaN(crest)) {
			return new Look(Kind.PADDOCK, Double.NaN, Double.NaN);
		}
		double berm = Double.NaN;
		for (double s = 0; s <= Math.min(crest, DryStack.BERM_SEARCH) && Double.isNaN(berm); s += 0.5) {
			boolean spans = true;
			for (double t : across) {
				int x = (int) Math.floor(sx + dx * s + rx * t), z = (int) Math.floor(sz + dz * s + rz * t);
				spans &= ridge(ground, x, y, z, bermHigh);
			}
			if (spans) {
				berm = s;
			}
		}
		return new Look(Double.isNaN(berm) ? Kind.NO_BERM : Kind.BERM, crest, berm);
	}
}
