package dev.wildcatter.tailings.haul;

import dev.wildcatter.tailings.mine.Heading;

/**
 * Where a train runs, worked out from the rails themselves (docs/multiblocks/haulage.md). Each rail cell has a centreline from the
 * middle of the edge a train enters by, through the cell's centre, to the middle of the edge it leaves by, at the rail surface's
 * height. From a cell, the next is the rail beside it, level or a block up or down, whose surface meets it: so the flat drive, the
 * 1-in-2 decline and the joins between them run both ways. Plain Java: the blocks come in through {@link View}.
 */
public final class Track {
	private Track() {
	}

	public enum Kind {
		FLAT,
		/** The lower half of a 1-in-2 pair: 0 at the downhill edge to 0.5 at the uphill edge. */
		LOWER,
		/** The upper half: 0.5 to 1. */
		UPPER,
		/** 1 in 1: 0 to 1. */
		STEEP,
		/** A turnout, flat; thrown, it sends a train entering at its toe out of its clockwise side. */
		SWITCH
	}

	/**
	 * One rail cell. {@code facing} is the downhill way on a graded rail, the rail's way on a flat one, and the toe-to-heel way of a
	 * switch.
	 */
	public record Cell(int x, int y, int z, Kind kind, Heading facing, boolean thrown, boolean rack) {
		public boolean graded() {
			return kind == Kind.LOWER || kind == Kind.UPPER || kind == Kind.STEEP;
		}

		public boolean same(Cell other) {
			return other != null && x == other.x && y == other.y && z == other.z;
		}
	}

	/** The rail at a position, or null for none. */
	public interface View {
		Cell at(int x, int y, int z);
	}

	/** A cell as a train runs over it: in moving one way, out moving another. */
	public record Step(Cell cell, Heading in, Heading out) {
		public double length() {
			double[] a = point(this, 0), c = point(this, 0.5), b = point(this, 1);
			return dist(a, c) + dist(c, b);
		}
	}

	public static Heading opposite(Heading h) {
		return h.turn().turn();
	}

	public static Heading counterClockwise(Heading h) {
		return h.turn().turn().turn();
	}

	private static boolean sameAxis(Heading a, Heading b) {
		return a == b || a == opposite(b);
	}

	/** The way a train moving {@code travel} leaves {@code cell}, or null if it cannot run that way through it. */
	public static Heading exit(Cell cell, Heading travel) {
		if (cell.kind() != Kind.SWITCH) {
			return sameAxis(cell.facing(), travel) ? travel : null;
		}
		Heading toe = cell.facing();
		if (travel == toe) {
			return cell.thrown() ? toe.turn() : toe;
		}
		if (travel == opposite(toe)) {
			return travel;
		}
		// in off the clockwise branch, out of the toe
		return travel == counterClockwise(toe) ? opposite(toe) : null;
	}

	/** Rail height above the bottom of the cell at a point (fractions 0-1 across the block). */
	public static double height(Cell cell, double fx, double fz) {
		if (!cell.graded()) {
			return 1.0;
		}
		Heading down = cell.facing();
		// 0 on the downhill edge, 1 on the uphill edge
		double along = switch (down) {
			case NORTH -> fz;
			case SOUTH -> 1 - fz;
			case EAST -> 1 - fx;
			case WEST -> fx;
		};
		along = Math.min(1, Math.max(0, along));
		return switch (cell.kind()) {
			case LOWER -> along * 0.5;
			case UPPER -> 0.5 + along * 0.5;
			case STEEP -> along;
			default -> 1.0;
		};
	}

	/** The world height of the middle of {@code cell}'s edge on side {@code side}. */
	public static double edgeHeight(Cell cell, Heading side) {
		return cell.y() + height(cell, 0.5 + 0.5 * side.forwardX(), 0.5 + 0.5 * side.forwardZ());
	}

	/** The next step after leaving {@code from} by {@code out}: the rail beside it whose surface meets it; null at the end of track. */
	public static Step next(View view, Step from) {
		Heading out = from.out();
		Cell c = from.cell();
		double meet = edgeHeight(c, out);
		for (int dy : new int[] {0, -1, 1}) {
			Cell n = view.at(c.x() + out.forwardX(), c.y() + dy, c.z() + out.forwardZ());
			if (n == null) {
				continue;
			}
			Heading leave = exit(n, out);
			if (leave != null && Math.abs(edgeHeight(n, opposite(out)) - meet) < 0.01) {
				return new Step(n, out, leave);
			}
		}
		return null;
	}

	/** The step before {@code to}: the rail it came from, for a train backing up or a path laid back from a cell. */
	public static Step previous(View view, Step to) {
		Step back = next(view, reverse(to));
		return back == null ? null : reverse(back);
	}

	/** The same cell run the other way. */
	public static Step reverse(Step step) {
		return new Step(step.cell(), opposite(step.out()), opposite(step.in()));
	}

	/** The first step on {@code cell} for a train facing {@code travel}; null if the rail does not run that way. */
	public static Step start(Cell cell, Heading travel) {
		if (cell.kind() == Kind.SWITCH) {
			Heading toe = cell.facing();
			Heading t = sameAxis(toe, travel) ? travel : toe;
			return new Step(cell, t, exit(cell, t));
		}
		if (!sameAxis(cell.facing(), travel)) {
			return null;
		}
		return new Step(cell, travel, travel);
	}

	/**
	 * A point on the step's centreline, {@code t} from 0 (the entry edge) to 1 (the exit edge), as {x, y, z} in the world: the entry
	 * edge's middle, the centre, the exit edge's middle.
	 */
	public static double[] point(Step step, double t) {
		Cell c = step.cell();
		Heading from = opposite(step.in());
		double fx, fz;
		if (t <= 0.5) {
			double k = t / 0.5;
			fx = 0.5 + 0.5 * from.forwardX() * (1 - k);
			fz = 0.5 + 0.5 * from.forwardZ() * (1 - k);
		} else {
			double k = (t - 0.5) / 0.5;
			fx = 0.5 + 0.5 * step.out().forwardX() * k;
			fz = 0.5 + 0.5 * step.out().forwardZ() * k;
		}
		return new double[] {c.x() + fx, c.y() + height(c, fx, fz), c.z() + fz};
	}

	/** The share of gravity along the rail for a train running this step: positive uphill, negative downhill. */
	public static double sine(Step step) {
		Cell c = step.cell();
		if (!c.graded()) {
			return 0;
		}
		double rise = c.kind() == Kind.STEEP ? 1 : 0.5;
		double s = rise / Math.sqrt(1 + rise * rise);
		return step.in() == c.facing() ? -s : s;
	}

	static double dist(double[] a, double[] b) {
		double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}
}
