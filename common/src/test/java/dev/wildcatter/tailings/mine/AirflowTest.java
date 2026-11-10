package dev.wildcatter.tailings.mine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The drive's ventilation numbers (docs/multiblocks/ventilation.md, "How it runs"). */
class AirflowTest {
	@Test
	void theBoreNeedsTenPointEightCubicMetresASecond() {
		assertEquals(36, Airflow.BORE_AREA, 1e-9);
		assertEquals(10.8, Airflow.NEED, 1e-9);
	}

	@Test
	void theDuctLosesAboutSixtyFivePascalsAHundredMetres() {
		// at 13-15 m³/s a 1.4 m duct carries 8.5-9.7 m/s and loses about 65 Pa a 100 m (spec)
		assertEquals(8.4, 13 / Airflow.ductArea(), 0.1);
		assertEquals(9.7, 15 / Airflow.ductArea(), 0.1);
		double at14 = Airflow.frictionPa(100, 14);
		assertTrue(at14 > 55 && at14 < 75, "about 65 Pa a 100 m at 14 m³/s, got " + at14);
	}

	@Test
	void itLeaksOnePercentAHundredMetres() {
		assertEquals(0.99, Airflow.kept(100), 1e-9);
		assertEquals(Math.pow(0.99, 30), Airflow.kept(3_000), 1e-9);
		assertTrue(Airflow.effectiveMetres(3_000) < 3_000 && Airflow.effectiveMetres(3_000) > 2_000);
	}

	@Test
	void theFanFollowsItsCurve() {
		Airflow.Fan fan = Airflow.Fan.main(1);
		assertEquals(3_000, fan.pressure(0), 1e-9);
		assertEquals(0, fan.pressure(25), 1e-9);
		assertEquals(1_920, fan.pressure(15), 1e-9);
		// half speed: a quarter of the pressure (the fan laws)
		assertEquals(750, Airflow.Fan.main(0.5).pressure(0), 1e-9);
	}

	@Test
	void twoHundredMetresOfDuctIsPlenty() {
		Airflow.Result air = Airflow.solve(200, Airflow.Fan.main(1));
		assertTrue(air.enough(), "200 m should carry 10.8 m³/s, got " + air.endFlow());
		assertTrue(air.fanFlow() < Airflow.FAN_FREE && air.fanFlow() > 20, "the fan near free delivery, got " + air.fanFlow());
	}

	@Test
	void oneFanReachesAboutThreeKilometres() {
		assertTrue(Airflow.solve(3_000, Airflow.Fan.main(1)).enough(), "3 km on one fan");
		Airflow.Result far = Airflow.solve(3_500, Airflow.Fan.main(1));
		assertFalse(far.enough(), "3.5 km on one fan should fall short, got " + far.endFlow());
		// at about 3 km the fan works near its 15 m³/s duty point
		assertEquals(15, Airflow.solve(3_000, Airflow.Fan.main(1)).fanFlow(), 0.5);
	}

	@Test
	void aBoosterCarriesItOn() {
		Airflow.Result boosted = Airflow.solve(3_500, Airflow.Fan.main(1), Airflow.Fan.booster(1));
		assertTrue(boosted.enough(), "3.5 km with a booster, got " + boosted.endFlow());
		assertTrue(boosted.pressurePa() > Airflow.FAN_SHUTOFF_PA * 0.5);
		// a booster that is not turning adds nothing
		assertEquals(Airflow.solve(3_500, Airflow.Fan.main(1)).endFlow(), Airflow.solve(3_500, Airflow.Fan.main(1), Airflow.Fan.booster(0)).endFlow(), 1e-9);
	}

	@Test
	void noFanNoAir() {
		assertEquals(0, Airflow.solve(100, Airflow.Fan.main(0)).endFlow(), 1e-9);
		assertEquals(0, Airflow.solve(100).endFlow(), 1e-9);
		assertTrue(Airflow.describe(0).startsWith("POOR AIR"));
		assertTrue(Airflow.describe(12).startsWith("air 0.33"));
	}

	@Test
	void theRulesAreOffUnlessTheConfigTurnsThemOn() {
		assertFalse(VentRules.flag(java.util.List.of(), "interlock"));
		assertFalse(VentRules.flag(java.util.List.of("stale_air=true"), "interlock"));
		assertTrue(VentRules.flag(java.util.List.of(" interlock=true "), "interlock"));
		assertFalse(VentRules.flag(java.util.List.of("interlock=yes"), "interlock"));
	}
}
