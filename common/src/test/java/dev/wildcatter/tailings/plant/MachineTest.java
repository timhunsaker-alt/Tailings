package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Every part is the size its approved spec gives (docs/multiblocks), and none is a one-cell part, which the 0.2.0 Wildcatter jar cannot hold. */
class MachineTest {
	/** W x H x L from the specs' parts tables. */
	private static final Map<Machine, int[]> SPEC = Map.ofEntries(
			Map.entry(Machine.GRIZZLY, new int[] {3, 2, 4}),
			Map.entry(Machine.JAW, new int[] {3, 4, 4}),
			Map.entry(Machine.CONE, new int[] {3, 4, 3}),
			Map.entry(Machine.SCREEN, new int[] {3, 3, 6}),
			Map.entry(Machine.CONVEYOR_DRIVE, new int[] {1, 1, 2}),
			Map.entry(Machine.BALL_MILL, new int[] {4, 4, 6}),
			Map.entry(Machine.CYCLONE, new int[] {2, 4, 2}),
			Map.entry(Machine.CONDITIONER, new int[] {3, 4, 3}),
			Map.entry(Machine.FLOTATION_BANK, new int[] {7, 4, 13}),
			Map.entry(Machine.BLOWER, new int[] {2, 2, 3}),
			Map.entry(Machine.THICKENER, new int[] {8, 3, 8}),
			Map.entry(Machine.FILTER, new int[] {3, 3, 5}),
			Map.entry(Machine.DUMP_RAMP, new int[] {1, 1, 4}),
			Map.entry(Machine.DUMP_POCKET, new int[] {4, 3, 3}),
			Map.entry(Machine.APRON_FEEDER, new int[] {2, 1, 4}),
			Map.entry(Machine.ORE_SORTER, new int[] {3, 3, 3}),
			Map.entry(Machine.ORE_BIN, new int[] {3, 6, 3}));

	@Test
	void everyPartIsItsSpecSize() {
		assertEquals(Machine.values().length, SPEC.size());
		for (Machine machine : Machine.values()) {
			int[] size = SPEC.get(machine);
			assertEquals(size[0], machine.width(), machine + " width");
			assertEquals(size[1], machine.height(), machine + " height");
			assertEquals(size[2], machine.depth(), machine + " length");
			assertTrue(machine.cells() >= 2, machine + " must have at least 2 cells");
		}
	}

	@Test
	void motorsAreTheSpecRatings() {
		assertEquals(110_000, Machine.JAW.watts());
		assertEquals(600_000, Machine.BALL_MILL.watts());
		assertEquals(Machine.Supply.MEDIUM, Machine.BALL_MILL.supply());
		assertEquals(360_000, Machine.FLOTATION_BANK.watts());
		assertEquals(0, Machine.GRIZZLY.watts());
		assertEquals(96, Machine.BALL_MILL.cells());
		assertEquals(192, Machine.THICKENER.cells());
	}
}
