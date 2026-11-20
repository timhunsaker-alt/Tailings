package dev.wildcatter.tailings.pit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The dragline's levers (dragline-hand-control.md, Part 1): its bucket on two ropes, the limits, the swing and the power share. */
class DraglineRopesTest {
	@Test
	void theBucketHangsWhereItsTwoRopesMeet() {
		double out = 50, up = 10;
		DraglineRopes.Bucket b = DraglineRopes.hang(DraglineRopes.hoistLength(out, up), DraglineRopes.dragLength(out, up), -20);
		assertNotNull(b);
		assertEquals(out, b.out(), 1e-6);
		assertEquals(up, b.up(), 1e-6);
		assertFalse(b.grounded());
	}

	@Test
	void loweredOntoTheGroundItRestsWhereItsDragReaches() {
		double out = 50, up = 10;
		DraglineRopes.Bucket b = DraglineRopes.hang(DraglineRopes.hoistLength(out, up) + 30, DraglineRopes.dragLength(out, up), 0);
		assertNotNull(b);
		assertTrue(b.grounded());
		assertEquals(0, b.up(), 1e-9);
		assertEquals(DraglineRopes.dragLength(out, up), DraglineRopes.dragLength(b.out(), 0), 1e-6, "on the ground at its drag rope's length");
	}

	@Test
	void hoistingRaisesItAndDraggingBringsItIn() {
		DraglineRopes.Bucket b = new DraglineRopes.Bucket(50, 5, false);
		DraglineRopes.Bucket up = DraglineRopes.move(b, -2, 0, -30);
		assertTrue(up.up() > b.up(), "hoisting raises it: " + up);
		DraglineRopes.Bucket in = DraglineRopes.move(b, 0, -2, -30);
		assertTrue(in.out() < b.out(), "dragging brings it in: " + in);
		DraglineRopes.Bucket slack = DraglineRopes.move(new DraglineRopes.Bucket(50, 0, true), 3, 0, 0);
		assertEquals(50, slack.out(), 1e-6, "on the ground, paying out the hoist moves nothing");
	}

	@Test
	void theLimitsStopIt() {
		DraglineRopes.Bucket top = new DraglineRopes.Bucket(60, DraglineRopes.POINT_UP - DraglineRopes.HOIST_LIMIT - 0.1, false);
		DraglineRopes.Bucket next = DraglineRopes.move(top, -2, 0, -30);
		assertTrue(next.up() <= DraglineRopes.POINT_UP - DraglineRopes.HOIST_LIMIT + 1e-6, "the hoist limit: " + next);
		DraglineRopes.Bucket near = new DraglineRopes.Bucket(15.2, -5, false);
		DraglineRopes.Bucket closer = DraglineRopes.move(near, 0, -3, -30);
		assertTrue(closer.out() >= DraglineRopes.DRAG_LIMIT_OUT - 1e-6, "the drag limit: " + closer);
		assertFalse(DraglineRopes.allowed(66, 0), "past its 65 m reach");
		assertFalse(DraglineRopes.allowed(40, -36), "below its 35 m dig depth");
	}

	@Test
	void theHouseCoastsFortyDegreesFromFullSpeed() {
		assertEquals(40.5, DraglineRopes.coast(DraglineRopes.SWING_MAX), 0.01);
		double rate = 0, moved = 0;
		for (int i = 0; i < 400; i++) {
			double[] s = DraglineRopes.swing(rate, 1, 0.05, 1);
			moved += s[0];
			rate = s[1];
		}
		assertEquals(DraglineRopes.SWING_MAX, rate, 1e-9, "full speed after 9 s");
		double coast = 0;
		for (int i = 0; i < 400 && rate != 0; i++) {
			double[] s = DraglineRopes.swing(rate, 0, 0.05, 1);
			coast += s[0];
			rate = s[1];
		}
		assertEquals(40.5, coast, 0.6, "it coasts about 40 degrees with no key");
		double[] braking = DraglineRopes.swing(5, -1, 0.05, 1);
		assertEquals(5 - 0.1, braking[1], 1e-9, "the other key brakes twice as hard");
	}

	@Test
	void ninetyDegreesTakeAboutNineteenSeconds() {
		double rate = 0, angle = 0;
		int ticks = 0;
		while (angle < 90 && ticks < 2000) {
			int key = angle + DraglineRopes.coast(rate) >= 90 ? 0 : 1;
			double[] s = DraglineRopes.swing(rate, key, 0.05, 1);
			angle += s[0];
			rate = s[1];
			ticks++;
		}
		assertEquals(19, ticks / 20.0, 1.0, "a 90 degree swing, braking on time: " + ticks / 20.0 + " s");
	}

	@Test
	void theSetsShareSixMegawatts() {
		double asked = DraglineRopes.asked(true, true, -1, true, true);
		assertEquals(10.8e6, asked, 1);
		assertEquals(6e6 / 10.8e6, DraglineRopes.share(asked), 1e-9, "every motion slows in proportion");
		assertEquals(6e6, DraglineRopes.draw(asked), 1, "never more than 6 MW: the breaker never trips by the levers");
		assertEquals(1, DraglineRopes.share(DraglineRopes.asked(true, true, 0, false, false)), 1e-9, "dragging alone at 4.8 MW is not slowed");
		assertEquals(0.3e6, DraglineRopes.draw(0), 1, "0.3 MW standing");
	}

	@Test
	void aFullBucketIsFiveMetresOfDragInFifteenSeconds() {
		assertEquals(15, 5 / DraglineRopes.DRAG_DIG, 1e-9);
	}

	@Test
	void theHighwallIsLaidBackAtItsBatter() {
		assertEquals(0, Dragline.batterOffset(Dragline.BATTER_VERTICAL, 10));
		assertEquals(0, Dragline.batterOffset(Dragline.BATTER_2_IN_1, 2));
		assertEquals(4, Dragline.batterOffset(Dragline.BATTER_2_IN_1, 10));
		assertEquals(9, Dragline.batterOffset(Dragline.BATTER_1_IN_1, 10));
		assertEquals(18, Dragline.batterOffset(Dragline.BATTER_1_IN_2, 10));
		Dragline.Job job = new Dragline.Job();
		assertEquals(Dragline.BATTER_2_IN_1, job.batter(true), "auto: 2 in 1 with slope failures on");
		assertEquals(Dragline.BATTER_VERTICAL, job.batter(false), "auto: vertical with them off");
		assertTrue(job.stopOnCracks(), "Stop on cracks on by default");
		double first = Dragline.standoff() + 0.5;
		assertTrue(Dragline.inStrip(first, 1, 30, 10, Dragline.BATTER_2_IN_1, 2));
		assertFalse(Dragline.inStrip(first, 1, 30, 10, Dragline.BATTER_2_IN_1, 3), "the face leans into the strip");
	}
}
