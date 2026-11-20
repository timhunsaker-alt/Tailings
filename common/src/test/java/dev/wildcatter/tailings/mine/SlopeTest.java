package dev.wildcatter.tailings.mine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Whether a highwall or a spoil pile stands (dragline-hand-control.md, Part 2): the spec's table, water, blast damage, the spoil. */
class SlopeTest {
	private static double hc(Slope.Ground g, double beta) {
		return Slope.criticalHeight(Slope.Mix.of(Map.of(g, 1), false, false), beta);
	}

	@Test
	void theSpecsTable() {
		double two = Slope.angle(2, 1), one = 45;
		assertEquals(2.1, hc(Slope.Ground.SAND, 90), 0.1);
		assertEquals(6.1, hc(Slope.Ground.SAND, two), 0.1);
		assertEquals(27.5, hc(Slope.Ground.SAND, one), 0.2);
		assertEquals(7.0, hc(Slope.Ground.DIRT, 90), 0.1);
		assertEquals(16.7, hc(Slope.Ground.DIRT, two), 0.1);
		assertEquals(47.2, hc(Slope.Ground.DIRT, one), 0.2);
		assertEquals(8.7, hc(Slope.Ground.CLAY, 90), 0.1);
		assertEquals(0.9, hc(Slope.Ground.GRAVEL, 90), 0.1);
		assertEquals(2.2, hc(Slope.Ground.SPOIL, 90), 0.1);
		assertEquals(59.1, hc(Slope.Ground.ROCK, 90), 0.2);
		assertTrue(Double.isInfinite(hc(Slope.Ground.DIRT, 25)), "no steeper than its friction angle, it stands at any height");
	}

	@Test
	void waterAndBlastDamageLowerIt() {
		double two = Slope.angle(2, 1);
		assertEquals(12.7, Slope.criticalHeight(Slope.Mix.of(Map.of(Slope.Ground.DIRT, 1), true, false), two), 0.1, "a wet dirt wall 2 in 1 stands 13 m, not 17");
		assertEquals(29.6, Slope.criticalHeight(Slope.Mix.of(Map.of(Slope.Ground.ROCK, 1), false, true), 90), 0.2, "a blasted rock wall stands 30 m, not 59");
	}

	@Test
	void theScenesFigures() {
		Slope.Mix dirt = Slope.Mix.of(Map.of(Slope.Ground.DIRT, 1), false, false);
		assertEquals(0.78, Slope.highwallFs(dirt, 9, 90, 0), 0.01, "slope_highwall_slide: 9 m vertical dirt");
		assertEquals(1.28, Slope.highwallFs(dirt, 13, Slope.angle(2, 1), 0), 0.01, "slope_water, dry");
		assertEquals(0.98, Slope.highwallFs(Slope.Mix.of(Map.of(Slope.Ground.DIRT, 1), true, false), 13, Slope.angle(2, 1), 0), 0.01, "slope_water, wet");
		Slope.Mix rock = Slope.Mix.of(Map.of(Slope.Ground.ROCK, 1), false, false);
		assertTrue(Slope.highwallFs(rock, 40, 90, 0) > 1.3, "40 m vertical rock stands");
		assertTrue(Slope.highwallFs(Slope.Mix.of(Map.of(Slope.Ground.ROCK, 1), false, true), 40, 90, 0) < 1, "blasted, it fails");
		assertEquals(Slope.highwallFs(dirt, 13, 63.4, 0) - 0.05, Slope.highwallFs(dirt, 13, 63.4, Slope.SHAKE_FS), 1e-9, "a shake");
	}

	@Test
	void theFailurePlaneAndItsCracks() {
		assertEquals(57.5, Slope.planeDegrees(90, 25), 1e-9);
		assertEquals(12.7, Slope.crackBehind(20, 90, 57.5), 0.1, "a 20 m vertical dirt wall cracks 12.7 m behind its crest: under the tub");
	}

	@Test
	void theMixAveragesItsGround() {
		Slope.Mix m = Slope.Mix.of(Map.of(Slope.Ground.DIRT, 1, Slope.Ground.ROCK, 1), false, false);
		assertEquals(110, m.c(), 1e-9);
		assertEquals(22, m.gamma(), 1e-9);
		assertTrue(m.phi() > 25 && m.phi() < 35);
		assertTrue(Slope.Mix.of(Map.of(), false, false).empty());
		assertEquals(Slope.Ground.SAND, Slope.Ground.of("minecraft:red_sand", true));
		assertEquals(Slope.Ground.DIRT, Slope.Ground.of("minecraft:grass_block", true));
		assertEquals(Slope.Ground.CLAY, Slope.Ground.of("minecraft:mud", true));
		assertEquals(Slope.Ground.SPOIL, Slope.Ground.of("tailings:muck", false));
		assertEquals(Slope.Ground.ROCK, Slope.Ground.of("minecraft:stone", false));
	}

	@Test
	void spoilStandsOnAFirmDryFloor() {
		assertTrue(Double.isInfinite(Slope.spoilFs(60, false, false)));
		assertEquals(24 / 15.0, Slope.spoilFs(15, true, false), 1e-9);
		assertEquals(0.8, Slope.spoilFs(15, true, true), 1e-9, "slope_spoil: 15 m on wet clay fails");
	}

	@Test
	void theStages() {
		assertEquals(Slope.Stage.STABLE, Slope.stage(1.3));
		assertEquals(Slope.Stage.CREEP, Slope.stage(1.1));
		assertEquals(Slope.Stage.FAILING, Slope.stage(0.99));
		assertTrue(Slope.cracks(1.1));
		assertFalse(Slope.cracks(1.2));
	}
}
