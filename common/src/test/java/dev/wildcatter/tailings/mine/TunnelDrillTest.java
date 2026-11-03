package dev.wildcatter.tailings.mine;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TunnelDrillTest {
	@Test
	void ringIsTwentyEightAndTheFloorPatternAlternates() {
		assertEquals(28, DriveCourse.liningCount());
		assertArrayEquals(slope(true), DriveCourse.floor(0, Grade.ONE_IN_TWO, true));
		assertArrayEquals(slope(false), DriveCourse.floor(1, Grade.ONE_IN_TWO, true));
		assertArrayEquals(slope(true), DriveCourse.floor(2, Grade.ONE_IN_TWO, true));
		assertArrayEquals(new FloorPiece[] {
				FloorPiece.FLAT_ROCK, FloorPiece.RAIL_FLAT, FloorPiece.LANDING,
				FloorPiece.LANDING, FloorPiece.RAIL_FLAT, FloorPiece.FLAT_ROCK
		}, DriveCourse.floor(0, Grade.ONE_IN_TWO, false));
		assertArrayEquals(new FloorPiece[] {
				FloorPiece.SLOPE_STEEP, FloorPiece.RAIL_STEEP, FloorPiece.STAIR,
				FloorPiece.STAIR, FloorPiece.RAIL_STEEP, FloorPiece.SLOPE_STEEP
		}, DriveCourse.floor(3, Grade.ONE_IN_ONE, true));
		assertEquals(0, Grade.ONE_IN_TWO.dropBlocks(1));
		assertEquals(1, Grade.ONE_IN_TWO.dropBlocks(2));
		assertEquals(4, Grade.ONE_IN_ONE.dropBlocks(4));
	}

	@Test
	void oneAdvanceLinesTheRingAndLaysTheUpperSlope() {
		TableGround ground = stoneBox();
		TunnelDrill drill = ready(10, 6, Grade.ONE_IN_TWO);
		Advance drove = drill.step(ground);
		assertEquals(Advance.Stop.NONE, drove.stop());
		assertEquals(28, ground.count(Ground.LINING));
		assertEquals(58, drove.muck().get(Ground.STONE));
		assertArrayEquals(slope(true), drove.floor());
		assertEquals(FloorPiece.SLOPE_UPPER, ground.floorPiece(1, 10, 0));
		assertEquals(FloorPiece.RAIL_UPPER, ground.floorPiece(2, 10, 0));
		assertEquals(FloorPiece.STAIR, ground.floorPiece(3, 10, 0));
		assertEquals(FloorPiece.STAIR, ground.floorPiece(4, 10, 0));
		assertEquals(Ground.STONE, ground.floorRock(1, 10, 0));
		assertEquals(72, drill.stock().segments());
		assertEquals(98, drill.stock().rail());
		assertEquals(100, drill.stock().cobble());
		assertEquals(0, drill.stock().floorRock());
		assertEquals(1, drill.advanceCount());
		assertEquals(10, drill.floorY());
	}

	@Test
	void secondAdvanceIsTheLowerHalfAndALanding() {
		TableGround ground = stoneBox();
		fill(ground, 1);
		TunnelDrill drill = ready(10, 6, Grade.ONE_IN_TWO);
		drill.step(ground);
		Advance second = drill.step(ground);
		assertArrayEquals(slope(false), second.floor());
		assertEquals(FloorPiece.LANDING, ground.floorPiece(drill.worldX(3, -1), drill.worldY(1, -1), drill.worldZ(3, -1)));
		assertEquals(9, DriveCourse.floorY(10, 2, Grade.ONE_IN_TWO));
		assertEquals(9, drill.floorY());
	}

	@Test
	void flatDriveUsesGravelBallastAndNotSlopes() {
		TunnelDrill drill = ready(4, 4, Grade.ONE_IN_TWO);
		drill.program().setFlatLength(2);
		TableGround ground = new TableGround();
		fillAt(ground, drill, 0);
		Advance drove = drill.step(ground);
		assertEquals(FloorPiece.RAIL_FLAT, drove.floor()[1]);
		assertEquals(FloorPiece.LANDING, drove.floor()[2]);
		assertEquals(Ground.GRAVEL, drove.floorRock()[1]);
		assertEquals(98, drill.stock().gravel());
		assertEquals(98, drill.stock().rail());
		assertEquals(1, drill.flatDriven());
		fillAt(ground, drill, 0);
		drill.step(ground);
		assertEquals(Advance.Stop.TARGET_REACHED, drill.step(ground).stop());
	}

	@Test
	void waterIsVoidedAndLavaWithoutWaterStops() {
		TableGround wet = stoneBox();
		wet.set(0, 10, 0, Ground.WATER);
		TunnelDrill drill = ready(10, 4, Grade.ONE_IN_TWO);
		int px = drill.worldX(1, 2);
		int py = drill.worldY(3, 2);
		int pz = drill.worldZ(1, 2);
		wet.set(px, py, pz, Ground.FLOWING_WATER);
		Advance drove = drill.step(wet);
		assertEquals(1, drove.pumpedWater());
		assertEquals(Ground.LINING, wet.get(0, 10, 0));
		assertEquals(Ground.AIR, wet.get(px, py, pz));

		TableGround lava = stoneBox();
		lava.set(2, 11, 0, Ground.LAVA);
		TunnelDrill dry = ready(10, 4, Grade.ONE_IN_TWO);
		dry.stock().setWater(0);
		Advance stopped = dry.step(lava);
		assertEquals(Advance.Stop.LIQUID, stopped.stop());
		assertEquals(Ground.LAVA, lava.get(2, 11, 0));
		assertEquals(0, dry.advanceCount());

		dry.resetStop();
		dry.stock().setWater(1);
		Advance quenched = dry.step(lava);
		assertEquals(Advance.Stop.NONE, quenched.stop());
		assertEquals(1, quenched.muck().get(Ground.OBSIDIAN));
		assertEquals(0, dry.stock().water());
	}

	@Test
	void oreAheadAndAnEmptySlotStopBeforeTheCut() {
		TableGround ground = stoneBox();
		ground.set(4, 11, -3, Ground.ORE);
		TunnelDrill drill = ready(10, 4, Grade.ONE_IN_TWO);
		assertEquals(Advance.Stop.ORE, drill.step(ground).stop());
		assertEquals(Ground.STONE, ground.get(0, 10, 0));
		drill.resetStop();
		drill.program().setStopOnOre(false);
		assertEquals(Advance.Stop.NONE, drill.step(ground).stop());

		TunnelDrill bare = ready(10, 4, Grade.ONE_IN_TWO);
		bare.stock().setSegments(0);
		bare.stock().setCobble(0);
		TableGround air = new TableGround();
		Advance empty = bare.step(air);
		assertEquals(Advance.Stop.EMPTY, empty.stop());
		assertEquals(0, bare.advanceCount());
	}

	@Test
	void aCaveUnderTheInvertTakesCobbleAndAirDoesNotBecomeFloorRock() {
		TableGround ground = new TableGround();
		TunnelDrill drill = ready(10, 0, Grade.ONE_IN_TWO);
		drill.stock().setCobble(3);
		assertEquals(Advance.Stop.EMPTY, drill.step(ground).stop());
		drill.resetStop();
		drill.stock().setCobble(20);
		Advance drove = drill.step(ground);
		assertEquals(Advance.Stop.NONE, drove.stop());
		assertEquals(6, ground.count(Ground.COBBLE));
		assertEquals(Ground.COBBLE, drove.floorRock()[0]);
		assertEquals(8, drill.stock().cobble());
	}

	@Test
	void withoutSegmentsTheRingIsCutFromStone() {
		TableGround ground = stoneBox();
		TunnelDrill drill = ready(10, 6, Grade.ONE_IN_TWO);
		drill.stock().setSegments(0);
		Advance drove = drill.step(ground);
		assertEquals(Advance.Stop.NONE, drove.stop());
		assertEquals(0, ground.count(Ground.LINING));
		assertEquals(Ground.STONE, ground.get(0, 9, 0));
		assertEquals(0, drill.stock().segments());
		assertEquals(100, drill.stock().cobble());
	}

	@Test
	void pumpedWaterFillsTheTankAndTheRestIsVoided() {
		TableGround ground = stoneBox();
		ground.set(0, 10, 0, Ground.WATER);
		ground.set(1, 11, 0, Ground.WATER);
		TunnelDrill drill = ready(10, 4, Grade.ONE_IN_TWO);
		drill.stock().setWater(DrillStock.TANK_MAX - 1);
		drill.step(ground);
		assertEquals(DrillStock.TANK_MAX, drill.stock().water());

		TableGround lava = stoneBox();
		lava.set(2, 11, 0, Ground.LAVA);
		lava.set(3, 11, 0, Ground.WATER);
		TunnelDrill dry = ready(10, 4, Grade.ONE_IN_TWO);
		dry.stock().setWater(0);
		Advance quenched = dry.step(lava);
		assertEquals(Advance.Stop.NONE, quenched.stop());
		assertEquals(1, quenched.muck().get(Ground.OBSIDIAN));
		assertEquals(0, dry.stock().water());
	}

	@Test
	void theRampRisesTowardTheUphillEndAndTheMachineStepsForward() {
		int[][] lower = SlopeRamp.boxes(false, false, Heading.NORTH);
		int[][] upper = SlopeRamp.boxes(true, false, Heading.NORTH);
		assertEquals(8, lower.length);
		assertEquals(1, lower[0][4]);
		assertEquals(8, lower[7][4]);
		assertEquals(9, upper[0][4]);
		assertEquals(16, upper[7][4]);
		int[][] east = SlopeRamp.boxes(false, false, Heading.EAST);
		assertEquals(8, east[7][4]);
		assertTrue(east[7][0] < lower[7][2]);
		int[] moved = DrillContraption.translate(10, 40, 12, Heading.NORTH, true);
		assertArrayEquals(new int[] {10, 41, 11}, moved);
		assertTrue(DrillContraption.ridingInBore(41, 40));
		int[] again = DrillContraption.translate(moved[0], moved[1], moved[2], Heading.NORTH, false);
		assertArrayEquals(new int[] {10, 41, 10}, again);
	}

	@Test
	void deepslateAsksForTheSlowerAdvance() {
		TableGround ground = stoneBox();
		TunnelDrill drill = ready(10, 4, Grade.ONE_IN_TWO);
		assertEquals(DriveCourse.STONE_TICKS, drill.ticksFor(ground));
		ground.set(2, 12, 0, Ground.DEEPSLATE);
		assertEquals(DriveCourse.DEEPSLATE_TICKS, drill.ticksFor(ground));
	}

	private static FloorPiece[] slope(boolean upper) {
		FloorPiece slope = upper ? FloorPiece.SLOPE_UPPER : FloorPiece.SLOPE_LOWER;
		FloorPiece rail = upper ? FloorPiece.RAIL_UPPER : FloorPiece.RAIL_LOWER;
		FloorPiece walk = upper ? FloorPiece.STAIR : FloorPiece.LANDING;
		return new FloorPiece[] {slope, rail, walk, walk, rail, slope};
	}

	private static TunnelDrill ready(int floorY, int targetY, Grade grade) {
		DrillProgram program = new DrillProgram(targetY, Heading.NORTH, 0);
		program.setGrade(grade);
		program.setCableBlocks(1000);
		DrillStock stock = new DrillStock();
		stock.setRail(100);
		stock.setGravel(100);
		stock.setCobble(100);
		stock.setSegments(100);
		stock.setWater(10);
		return new TunnelDrill(program, stock, 0, floorY, 0);
	}

	private static TableGround stoneBox() {
		TableGround ground = new TableGround();
		fill(ground, 0);
		return ground;
	}

	private static void fill(TableGround ground, int ahead) {
		for (int up = 0; up < DriveCourse.FACE; up++) {
			for (int across = 0; across < DriveCourse.FACE; across++) {
				ground.set(across, 10 - 1 + up, -ahead, Ground.STONE);
			}
		}
		for (int across = 1; across <= DriveCourse.CLEAR; across++) {
			ground.set(across, 8, -ahead, Ground.STONE);
		}
	}

	private static void fillAt(TableGround ground, TunnelDrill drill, int ahead) {
		for (int up = 0; up < DriveCourse.FACE; up++) {
			for (int across = 0; across < DriveCourse.FACE; across++) {
				ground.set(drill.worldX(across, ahead), drill.worldY(up, ahead), drill.worldZ(across, ahead), Ground.STONE);
			}
		}
	}

}
