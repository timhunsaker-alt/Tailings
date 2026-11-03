package dev.wildcatter.tailings.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Places cars behind the locomotive along a sampled rail path. Centers are {@link #APART} blocks apart, measured
 * along the path so a slope shortens the horizontal gap and lifts the cars.
 */
public final class ConsistSpacing {
	/** Twenty pixels, the length of a car body. */
	public static final double APART = 20.0 / 16.0;

	private ConsistSpacing() {
	}

	public record Node(double x, double y, double z) {
		public double distance(Node other) {
			double dx = x - other.x;
			double dy = y - other.y;
			double dz = z - other.z;
			return Math.sqrt(dx * dx + dy * dy + dz * dz);
		}
	}

	public record Placement(double x, double y, double z, float yaw, float pitch) {
	}

	/**
	 * {@code backward} starts at the locomotive and walks away from the direction of travel. Returns one placement
	 * per car, the first a car-length behind the locomotive.
	 */
	public static List<Placement> carsBehind(List<Node> backward, int count) {
		if (backward.size() < 2 || count <= 0) {
			return List.of();
		}
		double[] dist = new double[backward.size()];
		for (int i = 1; i < backward.size(); i++) {
			dist[i] = dist[i - 1] + backward.get(i - 1).distance(backward.get(i));
		}
		double total = dist[dist.length - 1];
		Node last = backward.get(backward.size() - 1);
		Node beforeLast = backward.get(backward.size() - 2);
		double tail = Math.max(1.0e-6, beforeLast.distance(last));
		double ux = (last.x() - beforeLast.x()) / tail;
		double uy = (last.y() - beforeLast.y()) / tail;
		double uz = (last.z() - beforeLast.z()) / tail;
		List<Placement> placed = new ArrayList<>();
		for (int car = 1; car <= count; car++) {
			double target = car * APART;
			Node at;
			Node ahead;
			if (target <= total) {
				int index = 1;
				while (index < dist.length && dist[index] < target) {
					index++;
				}
				double span = dist[index] - dist[index - 1];
				double blend = span < 1.0e-8 ? 0 : (target - dist[index - 1]) / span;
				Node from = backward.get(index - 1);
				Node to = backward.get(index);
				at = lerp(from, to, blend);
				ahead = from;
			} else {
				double extra = target - total;
				at = new Node(last.x() + ux * extra, last.y() + uy * extra, last.z() + uz * extra);
				ahead = last;
			}
			placed.add(new Placement(at.x(), at.y(), at.z(), yaw(ahead.x() - at.x(), ahead.z() - at.z()), pitch(ahead, at)));
		}
		return placed;
	}

	public static float pitch(Node ahead, Node here) {
		double dx = ahead.x() - here.x();
		double dy = ahead.y() - here.y();
		double dz = ahead.z() - here.z();
		double horizontal = Math.sqrt(dx * dx + dz * dz);
		if (horizontal < 1.0e-6) {
			return 0F;
		}
		return (float) Math.toDegrees(Math.atan2(dy, horizontal));
	}

	/** Minecraft yaw: 0 looks south, 180 looks north. {@code dx}/{@code dz} point in the direction of travel. */
	public static float yaw(double dx, double dz) {
		float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
		// atan2(-0, -1) is -180, which is the same heading as north.
		if (yaw <= -180F) {
			yaw += 360F;
		}
		return yaw;
	}

	private static Node lerp(Node from, Node to, double blend) {
		return new Node(from.x() + (to.x() - from.x()) * blend, from.y() + (to.y() - from.y()) * blend, from.z() + (to.z() - from.z()) * blend);
	}
}
