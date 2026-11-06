package dev.wildcatter.tailings.haul;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import dev.wildcatter.tailings.mine.Heading;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Where a train runs: the flat, a turnout, and the 1-in-2 decline both ways. */
class TrackTest {
	/** Rails laid in a map, as a level would hold them. */
	private static final class Rails implements Track.View {
		final Map<String, Track.Cell> cells = new HashMap<>();

		Rails lay(int x, int y, int z, Track.Kind kind, Heading facing, boolean thrown) {
			cells.put(x + "," + y + "," + z, new Track.Cell(x, y, z, kind, facing, thrown, true));
			return this;
		}

		@Override
		public Track.Cell at(int x, int y, int z) {
			return cells.get(x + "," + y + "," + z);
		}
	}

	@Test
	void theFlatRunsBothWaysAndEnds() {
		Rails rails = new Rails();
		for (int z = 0; z < 3; z++) {
			rails.lay(0, 0, z, Track.Kind.FLAT, Heading.NORTH, false);
		}
		Track.Step start = Track.start(rails.at(0, 0, 2), Heading.NORTH);
		Track.Step next = Track.next(rails, start);
		assertEquals(1, next.cell().z());
		assertNull(Track.next(rails, Track.next(rails, next)), "the end of the track");
		assertEquals(2, Track.previous(rails, next).cell().z());
		assertNull(Track.start(rails.at(0, 0, 0), Heading.EAST), "a rail does not run across itself");
		assertEquals(1.0, start.length(), 1e-9);
	}

	@Test
	void aThrownSwitchSendsTheTrainClockwiseAndBackOutOfItsToe() {
		Rails rails = new Rails();
		rails.lay(0, 0, 1, Track.Kind.FLAT, Heading.NORTH, false);
		rails.lay(0, 0, 0, Track.Kind.SWITCH, Heading.NORTH, true);
		rails.lay(1, 0, 0, Track.Kind.FLAT, Heading.EAST, false);
		Track.Step in = Track.start(rails.at(0, 0, 1), Heading.NORTH);
		Track.Step sw = Track.next(rails, in);
		assertEquals(Heading.EAST, sw.out(), "thrown: out of the clockwise side");
		assertEquals(1, Track.next(rails, sw).cell().x());
		assertEquals(Track.Kind.SWITCH, Track.previous(rails, Track.next(rails, sw)).cell().kind(), "and back the way it came");
		assertEquals(Heading.SOUTH, Track.exit(rails.at(0, 0, 0), Heading.WEST), "from the branch, out of the toe");
	}

	@Test
	void theDeclineRunsDownAndBackUp() {
		// downhill to the north: flat at y 3, then pairs of upper and lower halves stepping down a block every two cells
		Rails rails = new Rails();
		rails.lay(0, 3, 5, Track.Kind.FLAT, Heading.NORTH, false);
		rails.lay(0, 3, 4, Track.Kind.UPPER, Heading.NORTH, false);
		rails.lay(0, 3, 3, Track.Kind.LOWER, Heading.NORTH, false);
		rails.lay(0, 2, 2, Track.Kind.UPPER, Heading.NORTH, false);
		rails.lay(0, 2, 1, Track.Kind.LOWER, Heading.NORTH, false);
		rails.lay(0, 1, 0, Track.Kind.FLAT, Heading.NORTH, false);
		Track.Step step = Track.start(rails.at(0, 3, 5), Heading.NORTH);
		int[] ys = {3, 3, 2, 2, 1};
		for (int y : ys) {
			step = Track.next(rails, step);
			assertNotNull(step);
			assertEquals(y, step.cell().y());
		}
		assertEquals(0, step.cell().z());
		assertEquals(-0.5 / Math.sqrt(1.25), Track.sine(Track.previous(rails, step)), 1e-9, "running down");
		Track.Step up = Track.reverse(Track.previous(rails, step));
		assertEquals(0.5 / Math.sqrt(1.25), Track.sine(up), 1e-9, "running up");
		Track.Step back = Track.reverse(step);
		for (int i = 0; i < 5; i++) {
			back = Track.next(rails, back);
			assertNotNull(back, "back up the decline, step " + i);
		}
		assertEquals(5, back.cell().z());
		double[] top = Track.point(Track.start(rails.at(0, 3, 4), Heading.NORTH), 0);
		assertEquals(4.0, top[1], 1e-9, "the decline meets the flat at its rail height");
	}

	@Test
	void aPathLaysAheadAndBehindAndSaves() {
		Rails rails = new Rails();
		for (int z = 0; z < 10; z++) {
			rails.lay(0, 0, z, Track.Kind.FLAT, Heading.NORTH, false);
		}
		TrainPath path = new TrainPath(Track.start(rails.at(0, 0, 5), Heading.NORTH));
		while (path.extendFront(rails)) {
		}
		assertEquals(6, path.steps().size());
		assertEquals(1.0, path.extendBack(rails), 1e-9);
		double[] p = path.point(0.5);
		assertEquals(6.5, p[2], 1e-9, "the step laid behind is now first");
		assertEquals(180, Math.abs(path.attitude(3)[0]), 1e-3, "running north");
		TrainPath back = TrainPath.load(path.save());
		assertEquals(path.length(), back.length(), 1e-9);
		rails.cells.remove("0,0,3");
		assertEquals(3, path.broken(rails) >= 0 ? 6 - path.steps().get(path.broken(rails)).cell().z() : -1);
		assertEquals(2.0, path.trimBefore(2.5), 1e-9);
	}
}
