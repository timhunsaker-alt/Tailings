package dev.wildcatter.tailings.shaft;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import org.junit.jupiter.api.Test;

/** The hoist's figures (shaft-and-hoist.md, "The hoist") and its winder's skips in balance. */
class HoistingTest {
	@Test
	void theLiftNeedsAbout350KilowattsAndTheMotorIs450() {
		assertEquals(346_000, Hoisting.liftWatts(), 1_000);
		assertTrue(Hoisting.MOTOR_WATTS > Hoisting.liftWatts());
	}

	@Test
	void a150MetreWindIsAbout28Seconds() {
		// 13.3 s up to 10 m/s and 13.3 s braking (66.7 m each), 16.7 m at full speed
		assertEquals(28.33, Hoisting.windSeconds(150), 0.01);
		assertEquals(38.33, Hoisting.cycleSeconds(150), 0.01);
		assertEquals(282, Hoisting.tonnesPerHour(150), 1);
		// five times the plant's 50 t/h and more
		assertTrue(Hoisting.tonnesPerHour(150) > 5 * dev.wildcatter.tailings.plant.PlantRates.TONNES_PER_HOUR);
	}

	@Test
	void aShortWindNeverReachesFullSpeed() {
		// 40 m: a triangle, peak sqrt(0.75 x 40) = 5.5 m/s
		assertEquals(2 * Math.sqrt(40 / 0.75), Hoisting.windSeconds(40), 1e-9);
	}

	@Test
	void theWinderTakesAboutTheWindTimeAndArrivesExactly() {
		Winder w = new Winder();
		w.setTravel(150);
		w.start();
		double t = 0, fastest = 0;
		while (!w.wind(0.05, 1)) {
			t += 0.05;
			fastest = Math.max(fastest, w.speed());
			assertTrue(t < 60, "it should arrive");
		}
		assertEquals(Hoisting.windSeconds(150), t, 1.0);
		assertEquals(Hoisting.ROPE_SPEED, fastest, 1e-9);
		assertEquals(150, w.height(0), 1e-9);
		assertEquals(0, w.height(1), 1e-9);
		assertEquals(0, w.top());
		assertEquals(Hoisting.REST_SECONDS, w.rest(), 1e-9);
	}

	@Test
	void theSkipsAreInBalance() {
		Winder w = new Winder();
		w.setTravel(100);
		w.start();
		for (int i = 0; i < 200; i++) {
			w.wind(0.05, 1);
			assertEquals(100, w.height(0) + w.height(1), 1e-9);
		}
	}

	@Test
	void noPowerSetsTheBrakes() {
		Winder w = new Winder();
		w.setTravel(100);
		w.start();
		for (int i = 0; i < 100; i++) {
			w.wind(0.05, 1);
		}
		double at = w.position();
		assertFalse(w.wind(0.05, 0));
		assertEquals(0, w.speed());
		assertEquals(at, w.position(), 1e-12);
	}

	@Test
	void loadsGoUpAndAreDumpedAndSaved() {
		Winder w = new Winder();
		w.setTravel(50);
		RockLoad flask = new RockLoad();
		flask.add(Rock.GRANITE, null, Hoisting.SKIP_KG);
		w.loadBottom(flask);
		assertEquals(Hoisting.SKIP_KG, w.load(0).total(), 1e-9);
		w.start();
		while (!w.wind(0.05, 1)) {
			// winding
		}
		Winder copy = new Winder();
		copy.load(w.save());
		assertEquals(Hoisting.SKIP_KG, copy.load(copy.top()).total(), 1e-9);
		assertEquals(50, copy.height(0), 1e-9);
		RockLoad out = copy.dump();
		assertEquals(Hoisting.SKIP_KG, out.kg(Rock.GRANITE, null), 1e-9);
		assertEquals(1, copy.dumps());
		assertTrue(copy.load(0).isEmpty());
	}

	@Test
	void theCageRidesAtFourMetresASecond() {
		// 40 m: 5.3 s up to 4 m/s over 10.7 m, the same down, 18.7 m at speed: about 15.3 s
		assertEquals(15.33, Hoisting.cageSeconds(40), 0.05);
		CageWinder w = new CageWinder();
		w.setTravel(40);
		assertTrue(w.up(), "a cage first found stands where it was hung, at the collar");
		assertTrue(w.call(false) && w.ringing());
		double t = 0, fastest = 0;
		while (!w.wind(0.05, 1) && t < 120) {
			t += 0.05;
			fastest = Math.max(fastest, w.speed());
		}
		assertEquals(Hoisting.CAGE_BELL_SECONDS + Hoisting.cageSeconds(40), t, 0.6, "the bell, then the trip");
		assertEquals(Hoisting.CAGE_SPEED, fastest, 1e-9);
		assertTrue(w.atLanding() && !w.up() && w.trips() == 1);
		CageWinder back = new CageWinder();
		back.load(w.save());
		assertEquals(1, back.trips());
		assertEquals(0, back.at(), 1e-9);
		// 40 m paid out over the 2.5 m drum: 5.1 turns
		assertEquals(40 / (Math.PI * 2.5), back.drumDegrees() / 360, 1e-9);
	}

	@Test
	void theCageWinderLiftsItsCageOnItsOwnMotor() {
		// single drum, nothing balancing it: the cage, its rated 1 t and 135 m of 2.7 kg/m rope at 4 m/s, accelerating, at 85 %: 142 kW
		assertEquals(142.3, Hoisting.cageLiftWatts(120) / 1000, 0.5);
		assertTrue(Hoisting.cageLiftWatts(120) < Hoisting.CAGE_MOTOR_WATTS, "the 160 kW motor lifts it from 120 m");
		// MDG 33's single-drum personnel winder: 4 m/s; the Handbook's economic drum speed over 120 m, 0.405 √H: 4.4 m/s
		assertTrue(Hoisting.CAGE_SPEED <= 0.405 * Math.sqrt(120));
	}

	@Test
	void aSkipWinderSavedWithACageStillLoads() {
		Winder w = new Winder();
		w.setTravel(100);
		w.start();
		w.wind(1, 1);
		Winder back = new Winder();
		back.load(w.save() + ";40.0;100.0;0.0;0.0;0;3");   // the cage's state as the hoist once saved it, after the loads
		assertEquals(w.position(), back.position(), 1e-9);
		assertEquals(w.travel(), back.travel(), 1e-9);
	}

	// ---- fix3-hi07: the pour over the chute, each rope on its own drum ---------------------------------------------------------------

	/** Every point along the path, every 2 cm, and the chute's top under it: the sheet's underside never below the chute or the scroll. */
	@Test
	void thePourRunsOverTheChuteAndIntoTheBin() {
		// ep 7's yard: the bin's mouth 2 m north of the headframe, x 4, its rim 5 m up; a skip in each compartment (x 3 and 6)
		for (double skipX : new double[] {3.0, 6.0}) {
			java.util.List<double[]> path = ShaftGeometry.pourPath(skipX, 4.0, 5.0, -2.0);
			double lowest = Double.POSITIVE_INFINITY;
			for (int i = 0; i + 1 < path.size(); i++) {
				double[] a = path.get(i), b = path.get(i + 1);
				for (double t = 0; t <= 1; t += 0.01) {
					double z = a[2] + (b[2] - a[2]) * t, y = a[1] + (b[1] - a[1]) * t;
					double under = y - ShaftGeometry.STREAM_THICK / 2 - ShaftGeometry.chuteTop(z);
					lowest = Math.min(lowest, under);
				}
			}
			assertTrue(lowest >= -1e-9, "the stream lies on the chute, never under it: " + lowest);
			// it lies on the chute, not floating over it: at each step's nose the sheet touches
			for (double z = 0; z <= 2.25 + 1e-9; z += 0.75) {
				assertEquals(ShaftGeometry.chuteTop(z + 1e-6), ShaftGeometry.noseLine(z), 1e-9, "the nose at z " + z);
			}
			// it leaves the skip under its floor and over the scroll, and starts down from the lip at z 0
			assertTrue(path.get(0)[1] > ShaftGeometry.SCROLL_TOP && path.get(0)[2] > ShaftGeometry.SCROLL_Z1);
			double[] lip = path.get(ShaftGeometry.lipIndex());
			assertEquals(ShaftGeometry.CHUTE_LIP, lip[2], 1e-9);
			assertEquals(16.05 + ShaftGeometry.STREAM_THICK / 2, lip[1], 1e-9);
			// after the lip it only falls and only goes north, and it ends in the middle of the bin's mouth
			for (int i = ShaftGeometry.lipIndex() + 1; i < path.size(); i++) {
				assertTrue(path.get(i)[1] < path.get(i - 1)[1] && path.get(i)[2] <= 1e-9);
			}
			double[] end = path.get(path.size() - 1);
			assertEquals(4.0, end[0], 1e-9);
			assertEquals(5.0, end[1], 1e-9);
			assertEquals(-2.0, end[2], 1e-9);
			// the door plate's lip clears the scroll
			assertTrue(ShaftGeometry.DOOR_LIP_Y > ShaftGeometry.SCROLL_TOP && ShaftGeometry.DOOR_LIP_Z > ShaftGeometry.SCROLL_Z0 && ShaftGeometry.DOOR_LIP_Z < ShaftGeometry.SCROLL_Z1);
			// a particle thrown off the lip at lipVelocity, under 0.04 blocks a tick squared, lands in the mouth
			double[] v = ShaftGeometry.lipVelocity(lip, end, 0.04);
			double x = lip[0], y = lip[1], z = lip[2], vy = v[1];
			while (y > end[1]) {
				x += v[0];
				z += v[2];
				y += vy;
				vy -= 0.04;
			}
			assertEquals(end[0], x, 0.6);
			assertEquals(end[2], z, 0.6);
		}
	}

	/**
	 * ep 7's hoist house 20 m behind the headframe, facing the same way: the sheaves' tops at x 3 and 6, 29 m up and 5.5 m south of the
	 * headframe's corner are, in the hoist house's frame, 14.5 m north of it. Each rope meets its own drum at a true tangent, on the
	 * drum's barrel and never in the gap between the drums; the west over the top, the east under the bottom.
	 */
	@Test
	void eachRopeMeetsItsOwnDrumAtATrueTangent() {
		double r = ShaftGeometry.HOIST_DRUM_R, rr = r + ShaftGeometry.ROPE_R, cy = ShaftGeometry.HOIST_DRUM_Y, cz = ShaftGeometry.HOIST_DRUM_Z;
		double travel = 138;
		for (double h = 0; h <= travel; h += 6) {
			for (int which = 0; which < 2; which++) {
				double sx = which == 0 ? 3.0 : 6.0, sy = Hoisting.SHEAVE_HEIGHT + 1.5, sz = 5.5 - 20;
				double[] d = ShaftGeometry.HOIST_DRUMS[which];
				double wound = ShaftGeometry.wound(which == 0 ? h : travel - h, r);
				double[] e = ShaftGeometry.ropeEnd(d[0], d[1], cy, cz, r, sx, sy, sz, wound, which == 0);
				assertTrue(e != null);
				assertTrue(e[0] > d[0] && e[0] < d[1], "on its own drum's barrel: x " + e[0] + " of " + d[0] + "-" + d[1]);
				assertFalse(e[0] > ShaftGeometry.HOIST_DRUMS[0][1] && e[0] < ShaftGeometry.HOIST_DRUMS[1][0], "not in the gap between the drums");
				assertEquals(rr, Math.hypot(e[1] - cy, e[2] - cz), 1e-9, "on the barrel");
				// a true tangent: the rope from the sheave is square to the drum's radius there
				double ru = e[2] - cz, rv = e[1] - cy, pu = sz - e[2], pv = sy - e[1];
				assertEquals(0, (ru * pu + rv * pv) / Math.hypot(pu, pv), 1e-9);
				assertTrue(which == 0 ? e[1] > cy + 0.5 : e[1] < cy - 0.5, which == 0 ? "the west rope over the top" : "the east rope under the bottom");
				// the coil starts at the flange farther from its sheave and fits on the drum
				double[] c = ShaftGeometry.coil(d[0], d[1], sx, wound, r);
				assertEquals(which == 0 ? d[0] : d[1], c[0], 0.1);
				assertTrue(Math.abs(c[2] - c[0]) < d[1] - d[0] - 0.1, "the coil fits");
			}
		}
		// on one shaft one rope pays out as the other takes in: the wound lengths add up to the same at every height
		assertEquals(ShaftGeometry.wound(0, r) + ShaftGeometry.wound(travel, r), ShaftGeometry.wound(40, r) + ShaftGeometry.wound(travel - 40, r), 1e-9);
	}

	/** The cage's rope meets the cage winder's 2.5 m drum over its top at a true tangent (the winder 11 m east, 20 m south of the headframe). */
	@Test
	void theCageRopeMeetsItsOwnDrum() {
		double r = ShaftGeometry.CAGE_DRUM_R, cy = ShaftGeometry.CAGE_DRUM_Y, cz = ShaftGeometry.CAGE_DRUM_Z;
		double sx = 4.0 - 11, sy = 26.95 + Hoisting.CAGE_SHEAVE_DIAMETER / 2, sz = 7.25 - 20;
		for (double h = 0; h <= 120; h += 10) {
			double wound = ShaftGeometry.wound(h, r);
			double[] e = ShaftGeometry.ropeEnd(ShaftGeometry.CAGE_DRUM[0], ShaftGeometry.CAGE_DRUM[1], cy, cz, r, sx, sy, sz, wound, true);
			assertTrue(e[0] > ShaftGeometry.CAGE_DRUM[0] && e[0] < ShaftGeometry.CAGE_DRUM[1], "on the drum: x " + e[0]);
			assertEquals(r + ShaftGeometry.ROPE_R, Math.hypot(e[1] - cy, e[2] - cz), 1e-9);
			assertTrue(e[1] > cy + 0.5, "over its top");
			double ru = e[2] - cz, rv = e[1] - cy, pu = sz - e[2], pv = sy - e[1];
			assertEquals(0, (ru * pu + rv * pv) / Math.hypot(pu, pv), 1e-9);
		}
	}
}
