package dev.wildcatter.tailings.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A slide's run (dragline-hand-control.md, The slide): up to 6 m/s, the server times arrivals and the client draws by this one rule. */
class SlideMotionTest {
	@Test
	void itSpeedsUpThenRunsAtSixMetresASecond() {
		double v = (SlideMotion.run(100) - SlideMotion.run(80)) / 20 * 20;
		assertEquals(6.0, v, 1e-9);
		assertTrue(SlideMotion.run(10) < 10 * SlideMotion.SPEED, "still speeding up after half a second");
	}

	@Test
	void thirtyMetresTakeAboutSixSeconds() {
		int t = SlideMotion.ticks(30);
		assertTrue(t >= 100 && t <= 125, "30 m in " + t + " ticks");
		assertEquals(30, SlideMotion.run(t), 0.31);
	}
}
