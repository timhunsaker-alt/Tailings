package dev.wildcatter.tailings.pit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

/** The blasthole drill's four parts (open-pit-p3-blasting.md, As built: four parts): where they stand and where the machine is. */
class BlastholeDrillLayoutTest {
	@Test
	void thePartsStandAsTheLiftedDrill() {
		assertFalse(BlastholeDrillLayout.overlaps(), "no two parts share a cell");
		// the deck sits on the crawlers, the mast and the dust collector on the deck's back end
		assertEquals(2, BlastholeDrillLayout.Part.DECK.offset()[1]);
		assertEquals(6, BlastholeDrillLayout.Part.MAST.offset()[1]);
		assertEquals(6, BlastholeDrillLayout.Part.DUST_COLLECTOR.offset()[1]);
		// 26 m to the mast top, as the lifted drill (PitDrive.DRILL)
		assertEquals(PitDrive.DRILL.height(), BlastholeDrillLayout.Part.MAST.offset()[1] + BlastholeDrillLayout.Part.MAST.machine().height(), 1e-9);
		// the deck is the machine's 7 x 15 m, its middle the centre
		var deck = BlastholeDrillLayout.Part.DECK;
		assertEquals(PitDrive.DRILL.width(), deck.machine().width(), 1e-9);
		assertEquals(PitDrive.DRILL.length(), deck.machine().depth(), 1e-9);
		assertEquals(BlastholeDrillLayout.CENTRE_X, deck.offset()[0] + deck.machine().width() / 2.0, 1e-9);
		assertEquals(BlastholeDrillLayout.CENTRE_Z, deck.offset()[2] + deck.machine().depth() / 2.0, 1e-9);
		assertEquals(140 + 420 + 180 + 18, BlastholeDrillLayout.cells());
	}

	@Test
	void theHoleIsUnderTheMastAndClearOfTheCrawlers() {
		double holeZ = BlastholeDrillLayout.CENTRE_Z + BlastholeDrillLayout.HOLE_BACK;   // the back is south, facing north
		var mast = BlastholeDrillLayout.Part.MAST;
		int[] m = mast.offset();
		assertEquals(true, holeZ > m[2] && holeZ < m[2] + mast.machine().depth(), "the hole is under the mast's foot");
		assertEquals(true, BlastholeDrillLayout.CENTRE_X > m[0] && BlastholeDrillLayout.CENTRE_X < m[0] + mast.machine().width(), "across, too");
		var crawlers = BlastholeDrillLayout.Part.CRAWLERS;
		assertEquals(true, holeZ > crawlers.machine().depth(), "the bit goes down behind the crawlers");
	}

	@Test
	void settingDownFindsTheCrawlersItWasLiftedFrom() {
		for (int turns = 0; turns < 4; turns++) {
			double[] c = BlastholeDrillLayout.centreFor(100, -40, turns);
			assertArrayEquals(new int[] {100, -40}, BlastholeDrillLayout.mainFor(c[0], c[1], turns), "facing " + turns);
			assertEquals(turns, BlastholeDrillLayout.turns(BlastholeDrillLayout.yaw(turns)));
		}
		double[] north = BlastholeDrillLayout.centreFor(0, 0, 0);
		assertArrayEquals(new double[] {3.5, 5.5}, north, 1e-9);
		// a quarter turn clockwise (facing east): the deck's middle swings to the crawlers' west
		assertArrayEquals(new int[] {-4, 2, 0}, BlastholeDrillLayout.turned(new int[] {0, 2, 4}, 1));
	}
}
