package dev.wildcatter.tailings.mine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** A failing slope's clock and its prisms (dragline-hand-control.md, Stages; Tim 2026-11-19: a game day at 0.95 to 2 min at 0.7). */
class SlopeClockTest {
	@Test
	void aGameDayAtNinetyFiveDownToTwoMinutesAtSeventy() {
		assertEquals(24_000, Slope.clockTicks(0.95, 1));
		assertEquals(2_400, Slope.clockTicks(0.71, 1), "never under two minutes from 0.7 up");
		assertEquals(200, Slope.clockTicks(0.69, 1), "under 0.7 within 10 s");
		assertEquals(2_458, Slope.clockTicks(0.78, 1), 1, "the 9 m dirt wall's two minutes");
		assertEquals(-1, Slope.clockTicks(1.0, 1), "no clock at 1 and over");
	}

	@Test
	void theClockOnlyShortensAsItFallsAndFollowsTheTimeScale() {
		long last = Long.MAX_VALUE;
		for (double fs = 0.99; fs >= 0.7; fs -= 0.01) {
			long t = Slope.clockTicks(fs, 1);
			assertTrue(t <= last, "lower falls sooner at " + fs);
			last = t;
		}
		assertEquals(2_400, Slope.clockTicks(0.95, 10), "at 10:1 a game day is 2.4 minutes");
	}

	@Test
	void prismsAccelerateToTheSlide() {
		assertTrue(Slope.velocity(1.5, -1, 0) < 0.1);
		assertEquals(2.05, Slope.velocity(1.0, -1, 0), 1e-9);
		assertEquals(2, Slope.velocity(0.9, 1000, 1000), 1e-9, "2 mm/h when its clock starts");
		assertEquals(4, Slope.velocity(0.9, 500, 1000), 1e-9, "1/velocity halves at half time");
		assertEquals(20, Slope.velocity(0.9, 100, 1000), 1e-9);
		assertEquals("ACCELERATING", Slope.trend(4, true));
		assertEquals("STEADY", Slope.trend(0.05, false));
	}
}
