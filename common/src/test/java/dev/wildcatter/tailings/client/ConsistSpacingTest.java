package dev.wildcatter.tailings.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.wildcatter.tailings.client.ConsistSpacing.Node;
import dev.wildcatter.tailings.client.ConsistSpacing.Placement;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Cars are 20 pixels apart along the rail path, including the rise of a slope. */
class ConsistSpacingTest {
	@Test
	void carsSitTwentyPixelsApartOnTheFlat() {
		List<Node> path = line(0);
		List<Placement> cars = ConsistSpacing.carsBehind(path, 2);
		assertEquals(2, cars.size());
		assertEquals(ConsistSpacing.APART, cars.get(0).z(), 1.0e-6);
		assertEquals(2 * ConsistSpacing.APART, cars.get(1).z(), 1.0e-6);
		assertEquals(20, cars.get(0).y(), 1.0e-6);
		assertEquals(180F, cars.get(0).yaw(), 0.01F);
		assertEquals(0F, cars.get(0).pitch(), 0.01F);
	}

	@Test
	void carsFollowAOneInOneSlope() {
		List<Node> path = line(1);
		Placement car = ConsistSpacing.carsBehind(path, 1).get(0);
		double along = ConsistSpacing.APART / Math.sqrt(2);
		assertEquals(along, car.z(), 1.0e-4);
		assertEquals(20 + along, car.y(), 1.0e-4);
		assertEquals(-45F, car.pitch(), 0.05F);
		assertEquals(180F, car.yaw(), 0.01F);
	}

	/** Samples walking south, away from a northbound locomotive, rising {@code slope} blocks per block. */
	private static List<Node> line(double slope) {
		List<Node> path = new ArrayList<>();
		for (int pixel = 0; pixel <= 16 * 16; pixel++) {
			double run = pixel / 16.0;
			path.add(new Node(0, 20 + run * slope, run));
		}
		return path;
	}
}
