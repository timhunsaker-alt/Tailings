package dev.wildcatter.tailings.plant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Every part is the size its approved spec gives (docs/multiblocks), and none is a one-cell part, which Wildcatter jars before 0.3.0 cannot hold. */
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
			Map.entry(Machine.SPIRAL_BANK, new int[] {4, 5, 6}),
			Map.entry(Machine.BYPRODUCT_PLANT, new int[] {5, 5, 8}),
			Map.entry(Machine.HEAP_PAD, new int[] {10, 3, 10}),
			Map.entry(Machine.SCAVENGER_BANK, new int[] {5, 4, 9}),
			Map.entry(Machine.ACTIVATION_TANK, new int[] {3, 4, 3}),
			Map.entry(Machine.REGRIND_MILL, new int[] {3, 3, 5}),
			Map.entry(Machine.CLEANER_BANK, new int[] {4, 3, 6}),
			Map.entry(Machine.LEACH_TANK, new int[] {5, 6, 5}),
			Map.entry(Machine.CCD_THICKENER, new int[] {12, 4, 12}),
			Map.entry(Machine.ELECTROWINNING_BANK, new int[] {5, 3, 9}),
			Map.entry(Machine.CONDITIONER, new int[] {3, 4, 3}),
			Map.entry(Machine.TAILINGS_POND, new int[] {6, 2, 6}),
			Map.entry(Machine.FLOTATION_BANK, new int[] {7, 4, 13}),
			Map.entry(Machine.BLOWER, new int[] {2, 2, 3}),
			Map.entry(Machine.THICKENER, new int[] {8, 3, 8}),
			Map.entry(Machine.FILTER, new int[] {3, 3, 5}),
			Map.entry(Machine.DUMP_RAMP, new int[] {1, 1, 4}),
			Map.entry(Machine.DUMP_POCKET, new int[] {4, 3, 3}),
			Map.entry(Machine.APRON_FEEDER, new int[] {2, 1, 4}),
			Map.entry(Machine.ORE_SORTER, new int[] {3, 3, 3}),
			Map.entry(Machine.ORE_BIN, new int[] {3, 6, 3}),
			Map.entry(Machine.TRUCK_DUMP_WALL, new int[] {12, 5, 2}),
			Map.entry(Machine.PIT_HOPPER, new int[] {10, 6, 6}),
			Map.entry(Machine.FUEL_BAY, new int[] {4, 3, 2}),
			Map.entry(Machine.HAUL_DISPATCH, new int[] {3, 3, 3}),
			Map.entry(Machine.TAILINGS_THICKENER, new int[] {15, 5, 15}),
			Map.entry(Machine.TAILINGS_FILTER, new int[] {6, 6, 24}),
			Map.entry(Machine.LOADOUT_FRAME, new int[] {12, 10, 12}),
			Map.entry(Machine.LOADOUT_BIN, new int[] {10, 7, 10}),
			Map.entry(Machine.TRACK_GRIZZLY, new int[] {1, 1, 4}),
			Map.entry(Machine.DUMP_CAM_RAIL, new int[] {1, 1, 4}),
			Map.entry(Machine.BATTERY_CHARGER, new int[] {2, 2, 1}),
			Map.entry(Machine.VENT_FAN, new int[] {3, 3, 5}),
			Map.entry(Machine.BOOSTER_FAN, new int[] {2, 2, 3}),
			Map.entry(Machine.HEADFRAME, new int[] {9, 30, 9}),
			Map.entry(Machine.HEADFRAME_BIN, new int[] {4, 5, 4}),
			Map.entry(Machine.HOIST_HOUSE, new int[] {10, 8, 12}),
			Map.entry(Machine.MEASURING_FLASK, new int[] {2, 3, 2}),
			Map.entry(Machine.PASTE_FILTER, new int[] {5, 5, 8}),
			Map.entry(Machine.PASTE_MIXER, new int[] {3, 3, 5}),
			Map.entry(Machine.PASTE_PUMP, new int[] {3, 3, 6}),
			Map.entry(Machine.SCAVENGER_SPIRAL_BANK, new int[] {4, 5, 6}),
			Map.entry(Machine.FLUID_BED_DRYER, new int[] {3, 4, 6}),
			Map.entry(Machine.RER_SEPARATOR, new int[] {2, 3, 3}),
			Map.entry(Machine.HT_ROLL_SEPARATOR, new int[] {2, 3, 4}),
			Map.entry(Machine.IRM_SEPARATOR, new int[] {2, 3, 3}),
			Map.entry(Machine.DESLIME_SCREEN, new int[] {2, 2, 4}),
			Map.entry(Machine.DMS_MODULE, new int[] {3, 6, 4}),
			Map.entry(Machine.DRAIN_RINSE_SCREEN, new int[] {2, 2, 5}),
			Map.entry(Machine.MEDIUM_RECOVERY, new int[] {2, 3, 3}),
			Map.entry(Machine.XRT_SORTER, new int[] {4, 5, 8}),
			Map.entry(Machine.SORTER_COMPRESSOR, new int[] {2, 2, 3}));

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
