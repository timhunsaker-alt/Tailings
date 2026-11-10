package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import org.junit.jupiter.api.Test;

/** Dense-medium separation's maths (docs/multiblocks/dense-medium-separation.md, L7). */
class DenseMediumTest {
	private static final double EPS = 1e-9;

	@Test
	void countryRockIsItsHostAndOreIsHeavier() {
		assertEquals(2.65, DenseMedium.pieceSg(Rock.GRANITE), 0.01, "granite's traces barely move it");
		// the game's copper ore: 400 kg of copper as chalcopyrite is 43 % of the block, about 3.15
		assertEquals(3.15, DenseMedium.pieceSg(Rock.COPPER), 0.01);
		// galena ore, 24 % galena in quartz
		assertEquals(3.14, DenseMedium.pieceSg(Rock.GALENA), 0.01);
		// sphalerite ore, 83 % sphalerite
		assertEquals(3.72, DenseMedium.pieceSg(Rock.SPHALERITE), 0.01);
		for (Rock rock : Rock.values()) {
			double sg = DenseMedium.pieceSg(rock);
			assertTrue(sg > 1.5 && sg < 8, rock + " at SG " + sg);
		}
	}

	@Test
	void atTheDefaultCountryRockFloatsAndTheMetalOresSink() {
		double medium = DenseMedium.SG_DEFAULT;
		for (Rock rock : new Rock[] {Rock.GRANITE, Rock.ANDESITE, Rock.STONE, Rock.COBBLESTONE, Rock.TUFF, Rock.SANDSTONE}) {
			assertFalse(DenseMedium.sinks(rock, medium), rock + " should float at 2.75");
		}
		for (Rock rock : new Rock[] {Rock.IRON, Rock.COPPER, Rock.GOLD, Rock.GALENA, Rock.SPHALERITE, Rock.CASSITERITE, Rock.CHROMITE, Rock.BARITE, Rock.FLUORITE, Rock.SPODUMENE,
				Rock.PLATINUM, Rock.PENTLANDITE, Rock.SCHEELITE}) {
			assertTrue(DenseMedium.sinks(rock, medium), rock + " should sink at 2.75, SG " + DenseMedium.pieceSg(rock));
		}
		// the denser country rocks sink with the ore at the default: the trade the setting is for
		assertTrue(DenseMedium.sinks(Rock.DEEPSLATE, medium));
		assertTrue(DenseMedium.sinks(Rock.DIORITE, medium));
		// and the light industrial minerals float whatever the setting: DMS is the wrong machine for them
		for (Rock rock : new Rock[] {Rock.HALITE, Rock.SYLVITE, Rock.BORAX, Rock.SULFUR, Rock.GRAPHITE, Rock.BAUXITE, Rock.BENTONITE}) {
			assertFalse(DenseMedium.sinks(rock, DenseMedium.SG_LOW), rock + " should float even at 2.6");
		}
	}

	@Test
	void theSettingTradesRecoveryForRejection() {
		// at 2.6 every hard country rock sinks; only the porous tuff and sandstone float
		for (Rock rock : new Rock[] {Rock.GRANITE, Rock.ANDESITE, Rock.DEEPSLATE, Rock.DIORITE, Rock.COPPER}) {
			assertTrue(DenseMedium.sinks(rock, 2.6), rock + " at 2.6");
		}
		// at 2.9 deepslate and diorite float too, but so does the tin ore (13 % cassiterite, about 2.88)
		assertFalse(DenseMedium.sinks(Rock.DEEPSLATE, 2.9));
		assertFalse(DenseMedium.sinks(Rock.DIORITE, 2.9));
		assertFalse(DenseMedium.sinks(Rock.CASSITERITE, 2.9));
		assertTrue(DenseMedium.sinks(Rock.COPPER, 2.9));
		// at 3.3 low-grade ore floats with the waste; the rich sphalerite ore still sinks
		assertFalse(DenseMedium.sinks(Rock.COPPER, 3.3));
		assertFalse(DenseMedium.sinks(Rock.GALENA, 3.3));
		assertTrue(DenseMedium.sinks(Rock.SPHALERITE, 3.3));
	}

	@Test
	void twoPerCentGoesTheWrongWayEachWay() {
		RockLoad feed = new RockLoad();
		feed.add(Rock.GRANITE, SizeClass.GRAVEL, 1000);
		feed.add(Rock.COPPER, SizeClass.GRAVEL, 1000);
		DenseMedium.Split split = DenseMedium.split(feed, DenseMedium.SG_DEFAULT);
		assertEquals(980, split.floats().kg(Rock.GRANITE, SizeClass.GRAVEL), EPS);
		assertEquals(20, split.sinks().kg(Rock.GRANITE, SizeClass.GRAVEL), EPS);
		assertEquals(980, split.sinks().kg(Rock.COPPER, SizeClass.GRAVEL), EPS);
		assertEquals(20, split.floats().kg(Rock.COPPER, SizeClass.GRAVEL), EPS);
		assertEquals(2000, split.sinks().total() + split.floats().total(), EPS, "nothing made or lost");
		assertEquals(0.5, DenseMedium.sinkShare(feed, DenseMedium.SG_DEFAULT), EPS);
	}

	@Test
	void theMediumAndItsLosses() {
		assertEquals(18, DenseMedium.fesiLost(60_000), EPS, "0.3 kg a tonne: an hour at 60 t/h");
		assertEquals(0.30, DenseMedium.fesiVolumeShare(2.75), 0.005, "a 2.75 medium is about 30 % ferrosilicon by volume");
		assertEquals(2, DenseMedium.gravelBlocks(3800));
		assertEquals(1, DenseMedium.gravelBlocks(3799));
		assertEquals(2.80, DenseMedium.step(2.75), EPS);
		assertEquals(DenseMedium.SG_LOW, DenseMedium.step(DenseMedium.SG_HIGH), EPS, "the setting wraps");
		assertEquals(DenseMedium.SG_HIGH, DenseMedium.clamp(4), EPS);
		assertEquals(60.0 * 1000 / 72_000, DenseMedium.KG_PER_TICK, EPS);
	}

	@Test
	void theStateCountsATonneAPulse() {
		DmsState state = new DmsState();
		assertEquals(DenseMedium.SG_DEFAULT, state.medium(), EPS);
		state.count(1000, 600, 0.3, 100);
		assertFalse(state.pulsing(100));
		state.count(1000, 600, 0.3, 200);
		assertTrue(state.pulsing(200));
		assertFalse(state.pulsing(200 + DmsState.PULSE_TICKS));
		state.step();
		DmsState loaded = new DmsState();
		loaded.load(state.save());
		assertEquals(2.80, loaded.medium(), EPS);
		assertEquals(1200, loaded.floatsKg(), 1e-3);
		assertEquals(0.6, loaded.fesiKg(), 1e-6);
	}
}
