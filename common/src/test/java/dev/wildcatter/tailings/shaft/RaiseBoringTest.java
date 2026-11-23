package dev.wildcatter.tailings.shaft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Raise boring's figures and progress (docs/multiblocks/shaft-and-hoist.md, L4b). */
class RaiseBoringTest {
	@Test
	void theSpecsRates() {
		// a 150 m shaft: 25 min of pilot, 2.5 h of reaming
		assertEquals(150 * 10 / 60.0, 150 * RaiseBoring.PILOT_SECONDS_PER_M / 60, 1e-9);
		assertEquals(2.5, 150 * RaiseBoring.REAM_SECONDS_PER_M / 3600, 1e-9);
		assertEquals(150 * 70 / 3600.0, RaiseBoring.hours(150), 1e-9);
	}

	@Test
	void pilotDownThenReamUp() {
		RaiseBoring.Progress p = new RaiseBoring.Progress();
		assertEquals(RaiseBoring.Phase.SITING, p.phase());
		assertEquals(0, p.run(100, 1), "nothing to drill until a drive is found");
		p.found(20);
		int metres = 0;
		double t = 0;
		while (p.phase() == RaiseBoring.Phase.PILOT) {
			metres += p.run(0.05, 1);
			t += 0.05;
		}
		assertEquals(20, metres);
		assertEquals(200, t, 0.1, "20 m of pilot at 10 s a metre");
		assertEquals(RaiseBoring.Phase.REAM, p.phase());
		t = 0;
		while (p.phase() == RaiseBoring.Phase.REAM) {
			p.run(0.05, 1);
			t += 0.05;
		}
		assertEquals(18 * 60, t, 0.1, "18 m reamed at 60 s a metre, the collar slab and the drive's own height left");
		assertEquals(18, p.reamed());
		assertTrue(p.readout().startsWith("DONE"));
		RaiseBoring.Progress back = new RaiseBoring.Progress();
		back.load(p.save());
		assertEquals(RaiseBoring.Phase.DONE, back.phase());
		assertEquals(20, back.drive());
	}

	@Test
	void halfPowerHalfSpeed() {
		RaiseBoring.Progress p = new RaiseBoring.Progress();
		p.found(10);
		double t = 0;
		while (p.pilot() < 1) {
			p.run(0.05, 0.5);
			t += 0.05;
		}
		assertEquals(20, t, 0.1);
	}
}
