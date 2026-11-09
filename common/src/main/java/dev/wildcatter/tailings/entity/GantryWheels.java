package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.haul.Track;
import dev.wildcatter.tailings.haul.TrainPath;
import dev.wildcatter.tailings.mine.Heading;
import org.jspecify.annotations.Nullable;

/**
 * A back-up gantry's wheels on the twin track (docs/multiblocks/tunnel-boring-machine.md): a {@link TrainPath} along each of its two
 * rails, laid from the rails themselves as a train's is. The machine tows the gantry by saying how far along the bore its front is
 * to stand; the wheels find where the rails put it, its height and its pitch. A rail gone or changed under it, or missing ahead of it
 * or behind it, takes it off the track. Places along the bore are measured horizontally along the heading. Plain Java but for
 * nothing: the rails come in through {@link Track.View}.
 */
final class GantryWheels {
	/** The two rails, left then right as the machine faces. */
	private final TrainPath[] rails = new TrainPath[2];

	boolean railed() {
		return rails[0] != null && rails[1] != null;
	}

	void derail() {
		rails[0] = null;
		rails[1] = null;
	}

	/** Lays both paths from the rail cells under the gantry's front row; false if either is not a rail running the heading's way. */
	boolean rail(Track.View view, int[] left, int[] right, Heading heading) {
		TrainPath a = start(view, left, heading), b = start(view, right, heading);
		if (a == null || b == null) {
			derail();
			return false;
		}
		rails[0] = a;
		rails[1] = b;
		return true;
	}

	private static @Nullable TrainPath start(Track.View view, int[] at, Heading heading) {
		Track.Cell cell = view.at(at[0], at[1], at[2]);
		Track.Step step = cell == null ? null : Track.start(cell, heading);
		return step == null || step.out() != heading ? null : new TrainPath(step);
	}

	/**
	 * Runs both paths to a gantry {@code length} rows long whose front edge is at {@code along}, laying track ahead and behind as it
	 * needs it and dropping what it has left. Returns the left rail's point at the front edge and its height at the rear edge, {x, y,
	 * z, rearY}; or null, and the wheels off the track, if either rail does not carry it there.
	 */
	double @Nullable [] follow(Track.View view, double along, int length, Heading heading) {
		if (!railed()) {
			return null;
		}
		double[] front = null;
		double rearY = 0;
		for (TrainPath path : rails) {
			if (!cover(view, path, along - length, along, heading)) {
				derail();
				return null;
			}
			double sFront = sAt(path, along, heading), sRear = sAt(path, along - length, heading);
			if (front == null) {
				front = path.point(sFront);
				rearY = path.point(sRear)[1];
			}
			path.trimBefore(sRear - 2);
		}
		return new double[] {front[0], front[1], front[2], rearY};
	}

	/** True once the path runs from {@code from} to {@code to} along the heading, straight, with every rail of it still there. */
	private static boolean cover(Track.View view, TrainPath path, double from, double to, Heading heading) {
		int broken = path.broken(view);
		if (broken >= 0) {
			double a = along(Track.point(path.steps().get(broken), 0), heading), b = along(Track.point(path.steps().get(broken), 1), heading);
			if (Math.max(a, b) > from + 1e-6 && Math.min(a, b) < to - 1e-6) {
				return false;   // the rail under it went
			}
			double s = path.start(broken);
			if (Math.min(a, b) >= to - 1e-6) {
				path.trimAfter(s - 1e-6);
			} else {
				path.trimBefore(s + path.steps().get(broken).length() + 1e-6);
			}
			if (path.broken(view) >= 0) {
				return false;
			}
		}
		for (int i = 0; i < 64 && along(path.point(path.length()), heading) < to - 1e-6; i++) {
			if (!path.extendFront(view) || path.last().out() != heading) {
				return false;
			}
		}
		for (int i = 0; i < 64 && along(path.point(0), heading) > from + 1e-6; i++) {
			if (path.extendBack(view) == 0 || path.first().in() != heading) {
				return false;
			}
		}
		return along(path.point(path.length()), heading) >= to - 1e-6 && along(path.point(0), heading) <= from + 1e-6;
	}

	/** The place on the path whose point is {@code along} the bore: the path runs straight along the heading. */
	static double sAt(TrainPath path, double along, Heading heading) {
		double lo = 0, hi = path.length();
		for (int i = 0; i < 50; i++) {
			double mid = (lo + hi) / 2;
			if (along(path.point(mid), heading) < along) {
				lo = mid;
			} else {
				hi = mid;
			}
		}
		return (lo + hi) / 2;
	}

	static double along(double[] point, Heading heading) {
		return point[0] * heading.forwardX() + point[2] * heading.forwardZ();
	}

	String save() {
		return railed() ? rails[0].save() + "|" + rails[1].save() : "";
	}

	void load(String text) {
		derail();
		String[] two = text == null ? new String[0] : text.split("\\|");
		if (two.length == 2) {
			rails[0] = TrainPath.load(two[0]);
			rails[1] = TrainPath.load(two[1]);
			if (!railed()) {
				derail();
			}
		}
	}
}
