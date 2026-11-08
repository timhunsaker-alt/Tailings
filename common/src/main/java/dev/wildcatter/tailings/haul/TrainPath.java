package dev.wildcatter.tailings.haul;

import dev.wildcatter.tailings.mine.Heading;
import java.util.ArrayList;
import java.util.List;

/**
 * The stretch of track a train covers, as the steps it runs over, forward being the locomotive's end. A place on it is a distance
 * {@code s} from the start of its first step. The train lays it ahead as it goes, back behind it when it reverses, and drops what
 * it has left well behind. Plain Java.
 */
public final class TrainPath {
	private final List<Track.Step> steps = new ArrayList<>();
	private final List<Double> starts = new ArrayList<>();
	private double length;

	public TrainPath(Track.Step first) {
		add(first);
	}

	private TrainPath() {
	}

	private void add(Track.Step step) {
		starts.add(length);
		steps.add(step);
		length += step.length();
	}

	private void restart() {
		starts.clear();
		length = 0;
		List<Track.Step> all = new ArrayList<>(steps);
		steps.clear();
		for (Track.Step s : all) {
			add(s);
		}
	}

	public double length() {
		return length;
	}

	public List<Track.Step> steps() {
		return steps;
	}

	public Track.Step first() {
		return steps.get(0);
	}

	public Track.Step last() {
		return steps.get(steps.size() - 1);
	}

	/** Lays the next step ahead of the last; false at the end of track. */
	public boolean extendFront(Track.View view) {
		Track.Step next = Track.next(view, last());
		if (next == null) {
			return false;
		}
		add(next);
		return true;
	}

	/** Lays the step behind the first; returns how far every place on the path moved (0 at the end of track). */
	public double extendBack(Track.View view) {
		Track.Step back = Track.previous(view, first());
		if (back == null) {
			return 0;
		}
		steps.add(0, back);
		restart();
		return back.length();
	}

	/** Drops the steps that end before {@code s}; returns how far every place on the path moved back. */
	public double trimBefore(double s) {
		double moved = 0;
		while (steps.size() > 1 && starts.get(1) <= s) {
			double gone = steps.get(0).length();
			steps.remove(0);
			restart();
			s -= gone;
			moved += gone;
		}
		return moved;
	}

	/** Drops the steps that start after {@code s}, the track laid ahead of the train that it has not reached. */
	public void trimAfter(double s) {
		while (steps.size() > 1 && starts.get(steps.size() - 1) > s) {
			Track.Step gone = steps.remove(steps.size() - 1);
			starts.remove(starts.size() - 1);
			length -= gone.length();
		}
	}

	/** The index of the step under {@code s}. */
	public int index(double s) {
		int i = 0;
		while (i + 1 < steps.size() && starts.get(i + 1) <= s) {
			i++;
		}
		return i;
	}

	public Track.Step stepAt(double s) {
		return steps.get(index(s));
	}

	/** The point at {@code s}, {x, y, z}, held to the path's ends. */
	public double[] point(double s) {
		s = Math.max(0, Math.min(length, s));
		int i = index(s);
		Track.Step step = steps.get(i);
		double t = step.length() < 1e-9 ? 0 : (s - starts.get(i)) / step.length();
		return Track.point(step, Math.min(1, t));
	}

	/** Yaw (Minecraft degrees, 0 south) and pitch (degrees, nose up negative) of a vehicle centred at {@code s}. */
	public float[] attitude(double s) {
		double[] a = point(s - 0.3), b = point(s + 0.3);
		double dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
		double flat = Math.sqrt(dx * dx + dz * dz);
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		float pitch = (float) -Math.toDegrees(Math.atan2(dy, Math.max(1e-6, flat)));
		return new float[] {yaw, pitch};
	}

	/** Index of the first step whose rail is gone or changed, or -1 if the whole path still stands. */
	public int broken(Track.View view) {
		for (int i = 0; i < steps.size(); i++) {
			Track.Cell was = steps.get(i).cell();
			Track.Cell now = view.at(was.x(), was.y(), was.z());
			if (now == null || now.kind() != was.kind() || now.facing() != was.facing() || now.thrown() != was.thrown()) {
				return i;
			}
		}
		return -1;
	}

	public double start(int index) {
		return starts.get(index);
	}

	/** Saved as {@code x,y,z,KIND,FACING,thrown,rack,IN,OUT} per step, steps split by {@code ;}. */
	public String save() {
		StringBuilder out = new StringBuilder();
		for (Track.Step s : steps) {
			Track.Cell c = s.cell();
			out.append(out.length() == 0 ? "" : ";").append(c.x()).append(',').append(c.y()).append(',').append(c.z()).append(',')
					.append(c.kind()).append(',').append(c.facing()).append(',').append(c.thrown()).append(',').append(c.rack()).append(',')
					.append(s.in()).append(',').append(s.out());
		}
		return out.toString();
	}

	/** The path {@link #save} wrote, or null if it does not read. */
	public static TrainPath load(String text) {
		if (text == null || text.isBlank()) {
			return null;
		}
		TrainPath path = new TrainPath();
		try {
			for (String part : text.split(";")) {
				String[] f = part.split(",");
				Track.Cell c = new Track.Cell(Integer.parseInt(f[0]), Integer.parseInt(f[1]), Integer.parseInt(f[2]), Track.Kind.valueOf(f[3]),
						Heading.valueOf(f[4]), Boolean.parseBoolean(f[5]), Boolean.parseBoolean(f[6]));
				path.add(new Track.Step(c, Heading.valueOf(f[7]), Heading.valueOf(f[8])));
			}
		} catch (RuntimeException ex) {
			return null;
		}
		return path.steps.isEmpty() ? null : path;
	}
}
