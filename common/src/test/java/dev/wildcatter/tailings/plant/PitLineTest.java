package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The pit line's plain Java (primary-gyratory-stockpile.md, sag-mill-line.md; decisions in docs/questions/p7-pit-line.md). */
class PitLineTest {
	@Test
	void aThousandTonnesAnHourIsABlockEveryTenSeconds() {
		assertEquals(277.8, PitLine.KG_PER_SECOND, 0.1);
		assertEquals(9.7, PlantRates.BLOCK_KG / PitLine.KG_PER_SECOND, 0.05);
		// a 363 t ultra load keeps the line full for about 22 minutes
		assertEquals(22, 363_000 / PitLine.KG_PER_SECOND / 60, 0.5);
	}

	@Test
	void theGyratoryIsBiggerThanTheLine() {
		assertTrue(PitLine.GYRATORY_TPH >= 2354 && PitLine.GYRATORY_TPH <= 3362, "inside the 42-65's range at 152 mm");
		// fed the line's rate it stands idle more than half the time, as real primaries do
		assertEquals(0.42, PitLine.TONNES_PER_HOUR / PitLine.GYRATORY_TPH, 0.01);
	}

	@Test
	void theStockpileConveyorNeedsTwoDrives() {
		// 150 m long and 30 m up at 3,000 t/h: 375 kW on the flat and 272 kW of lift
		assertEquals(375_000, PitLine.heavyBeltWatts(150, 0, 3000), 1);
		assertEquals(272_500, PitLine.heavyBeltWatts(150, 30, 3000) - 375_000, 500);
		assertEquals(2, PitLine.heavyDrivesFor(150, 30));
		assertEquals(1, PitLine.heavyDrivesFor(30, 0));
		assertEquals(1, PitLine.heavyDrivesFor(1, 0));
		// a metre of full belt carries about 208 kg; a 150 m run holds over 31 t
		assertEquals(208.3, PitLine.HEAVY_KG_PER_METRE, 0.1);
		assertEquals(31_250, PitLine.heavyBeltHoldKg(150), 1);
		assertEquals(30 * PitLine.KG_PER_SECOND, PitLine.heavyBeltHoldKg(5), 1e-6);
	}

	@Test
	void aThirtyMetreConeHoldsFourDays() {
		assertEquals(40, PitLine.coneRadius(30), 1e-9);
		assertEquals(50_265, PitLine.coneVolume(30), 1);
		assertEquals(2025, PitLine.STOCKPILE_BLOCK_KG, 1e-9);
		double tonnes = PitLine.coneTonnes(30);
		assertEquals(101_800, tonnes, 100);
		assertEquals(4.2, tonnes / (PitLine.TONNES_PER_HOUR * 24), 0.05);
	}

	@Test
	void feedersShareTheSetpoint() {
		assertEquals(500, PitLine.feederShareTph(1000, 2), 1e-9);
		assertEquals(500, PitLine.feederShareTph(1000, 1), 1e-9, "one feeder runs at its own 500 t/h");
		assertEquals(333.3, PitLine.feederShareTph(1000, 3), 0.1);
		assertEquals(25, PitLine.feederShareTph(50, 2), 1e-9, "fed to the small plant at 50 t/h");
		assertEquals(0, PitLine.feederShareTph(1000, 0), 1e-9);
		assertEquals(1250, PitLine.nextSetpoint(1000), 1e-9);
		assertEquals(50, PitLine.nextSetpoint(1500), 1e-9);
	}

	@Test
	void everyTwoHundredthHardBlockIsABoulder() {
		PitState state = new PitState();
		int boulders = 0;
		for (int i = 0; i < 1000; i++) {
			boulders += state.countBlock(true) ? 1 : 0;
		}
		assertEquals(5, boulders);
		assertFalse(state.countBlock(false), "soft rock is never a boulder");
		// the breaker clears it in 30 s
		state.setSinceBoulder(PitLine.BOULDER_EVERY - 1);
		assertTrue(state.countBlock(true));
		assertTrue(state.bridged());
		int ticks = 0;
		while (!state.breakTick()) {
			ticks++;
		}
		assertEquals(PitLine.BREAK_SECONDS * 20 - 1, ticks);
		assertFalse(state.bridged());
	}

	@Test
	void theWeigherHoldsBackOnOverload() {
		PitState weigher = new PitState();
		assertEquals(1000, weigher.setpoint(100), 1e-9);
		weigher.sawOverload(100);
		assertEquals(900, weigher.setpoint(110), 1e-9);
		assertEquals(1000, weigher.setpoint(200), 1e-9, "it comes back once the mill has cleared");
		weigher.weigh(250_000);
		assertEquals(2, weigher.pulses());
		PitState saved = new PitState();
		weigher.setSetpoint(750);
		saved.load(weigher.save());
		assertEquals(750, saved.ownSetpoint(), 1e-9);
		assertEquals(250_000, saved.totalKg(), 1e-9);
	}

	// ---- P7b: the SAG mill line ------------------------------------------------------------------------------------------------

	@Test
	void bondsLawSizesTheBallMill() {
		// from the SAG's 2 mm transfer size to 150 µm at Wi 15: 8.9 kWh/t, 8.9 MW at 1,000 t/h
		assertEquals(8.9, PitLine.ballMillKwhPerTonne(), 0.05);
		assertTrue(PitLine.ballMillKwhPerTonne() * PitLine.TONNES_PER_HOUR * 1000 < 2 * PitLine.BALL_MILL_MOTOR_WATTS, "the 10.5 MW mill covers it");
		// scaled from an 18 x 28 ft mill at 4.1 MW by D^2.5 L: about 10.8 MW for 24 x 36 ft, the spec's "about 10.5"
		double scaled = PitLine.scaledMillWatts(4_100_000, 18, 28, 24, 36);
		assertEquals(10_800_000, scaled, 100_000);
		assertEquals(1.0, scaled / (2 * PitLine.BALL_MILL_MOTOR_WATTS), 0.04);
		// the SAG's 8.2 MW at 1,000 t/h is 8.2 kWh/t, inside real SAG mills' 5-12; about 17 MW of grinding drawn, 18.7 installed
		assertEquals(8.2, 2.0 * PitLine.SAG_MOTOR_WATTS / 1000 / PitLine.TONNES_PER_HOUR, 1e-9);
		assertEquals(18.7, (2.0 * PitLine.SAG_MOTOR_WATTS + 2.0 * PitLine.BALL_MILL_MOTOR_WATTS) / 1e6, 0.01);
	}

	@Test
	void theSagChargeAndItsOverload() {
		assertEquals(326, PitLine.SAG_INSIDE_M3, 1);
		assertEquals(83_400, PitLine.SAG_NORMAL_CHARGE_KG, 500);
		assertEquals(119_900, PitLine.SAG_OVERLOAD_KG, 1000);
		assertTrue(PitLine.SAG_MOST_CHARGE_KG > PitLine.SAG_OVERLOAD_KG);
		// at its normal charge it grinds its full rate; with no pebble crusher 15 % less; half charged, half
		assertEquals(PitLine.KG_PER_TICK, PitLine.sagGrindKg(PitLine.SAG_NORMAL_CHARGE_KG, 1, true), 1e-9);
		assertEquals(0.85 * PitLine.KG_PER_TICK, PitLine.sagGrindKg(PitLine.SAG_NORMAL_CHARGE_KG, 1, false), 1e-9);
		assertEquals(0.5 * PitLine.KG_PER_TICK, PitLine.sagGrindKg(PitLine.SAG_NORMAL_CHARGE_KG / 2, 1, true), 1e-9);
		assertEquals(PitLine.KG_PER_TICK, PitLine.sagGrindKg(PitLine.SAG_OVERLOAD_KG, 1, true), 1e-9, "overfed, it grinds no faster");
		assertTrue(PitLine.sagGrindKg(5, 1, true) <= 5, "never more than it holds");
	}

	@Test
	void theCycloneCircuitsWaterBalances() {
		// a 54 % cyclone feed splits into 70 % underflow and 35 % overflow with no water of its own
		assertEquals(0.544, PitLine.cycloneFeedSolids(), 0.001);
		double feedWater = (1 + PitLine.CIRCULATING_LOAD) * PlantRates.waterPerKg(PitLine.cycloneFeedSolids());
		double outWater = PitLine.CIRCULATING_LOAD * PlantRates.waterPerKg(PitLine.MILL_SOLIDS) + PlantRates.waterPerKg(PitLine.OVERFLOW_SOLIDS);
		assertEquals(outWater, feedWater, 1e-9);
		// all the line's fresh water leaves in the overflow: 516 L/s; the overflow is 0.62 m3/s of slurry
		assertEquals(516, PitLine.freshWaterLitresPerSecond(), 1);
		assertEquals(0.62, PitLine.overflowM3PerSecond(), 0.005);
		double sagWater = PlantRates.waterPerKg(PitLine.SAG_SOLIDS) + PitLine.pumpWaterPerKg(PitLine.SAG_SOLIDS);
		assertEquals(PlantRates.waterPerKg(PitLine.cycloneFeedSolids()), sagWater, 1e-9, "the SAG's water and the pump box's make the cyclone feed");
		assertEquals(3500, PitLine.CYCLONE_FEED_TPH, 1e-9);
		assertEquals(2500, PitLine.BALL_MILL_TPH, 1e-9);
	}
}
