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
}
