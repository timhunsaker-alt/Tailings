package dev.wildcatter.tailings.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.tailings.haul.Track;
import dev.wildcatter.tailings.mine.Heading;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** A gantry's wheels follow the twin track the machine lays, and come off it where a rail is gone. */
class GantryWheelsTest {
	private final Map<String, Track.Cell> rails = new HashMap<>();
	private final Track.View view = (x, y, z) -> rails.get(x + "," + y + "," + z);

	/** Two flat rails running north at x 2 and x 5, from z {@code from} to {@code to}. */
	private void lay(int from, int to) {
		for (int z = from; z <= to; z++) {
			for (int x : new int[] {2, 5}) {
				rails.put(x + "," + 0 + "," + z, new Track.Cell(x, 0, z, Track.Kind.FLAT, Heading.NORTH, false, false));
			}
		}
	}

	@Test
	void followsTheTrackAsItIsTowed() {
		lay(0, 20);
		GantryWheels wheels = new GantryWheels();
		assertTrue(wheels.rail(view, new int[] {2, 0, 10}, new int[] {5, 0, 10}, Heading.NORTH));
		// north is -z: the front edge of a gantry whose front row is z 10 is at z 10, along -10
		for (double front = 10; front >= 4; front -= 0.25) {
			double[] at = wheels.follow(view, -front, 4, Heading.NORTH);
			assertNotNull(at, "on the track at " + front);
			assertEquals(front, at[2], 1e-6);
			assertEquals(2.5, at[0], 1e-9);
			assertEquals(1.0, at[1], 1e-9);   // a flat rail's surface is the top of its cell
			assertEquals(at[1], at[3], 1e-9);
		}
	}

	@Test
	void comesOffWhereARailIsGoneAndNotOtherwise() {
		lay(0, 20);
		GantryWheels wheels = new GantryWheels();
		wheels.rail(view, new int[] {2, 0, 10}, new int[] {5, 0, 10}, Heading.NORTH);
		assertNotNull(wheels.follow(view, -10, 4, Heading.NORTH));
		rails.remove("5,0,18");   // well behind it
		assertNotNull(wheels.follow(view, -10, 4, Heading.NORTH));
		rails.remove("5,0,12");   // under its right wheels
		assertNull(wheels.follow(view, -10, 4, Heading.NORTH));
		assertFalse(wheels.railed());
	}

	@Test
	void stopsAtTheEndOfTheTrack() {
		lay(8, 20);
		GantryWheels wheels = new GantryWheels();
		wheels.rail(view, new int[] {2, 0, 10}, new int[] {5, 0, 10}, Heading.NORTH);
		assertNotNull(wheels.follow(view, -8, 4, Heading.NORTH));
		assertNull(wheels.follow(view, -7.5, 4, Heading.NORTH), "no rail ahead of it");
	}

	@Test
	void savesAndLoads() {
		lay(0, 20);
		GantryWheels wheels = new GantryWheels();
		wheels.rail(view, new int[] {2, 0, 10}, new int[] {5, 0, 10}, Heading.NORTH);
		wheels.follow(view, -10, 4, Heading.NORTH);
		GantryWheels again = new GantryWheels();
		again.load(wheels.save());
		assertTrue(again.railed());
		assertNotNull(again.follow(view, -9, 4, Heading.NORTH));
	}
}
