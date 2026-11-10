package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * What one kind of plant part does each tick (docs/multiblocks/crushing-circuit.md, "How it runs"). There is no controller: each
 * part runs its own motor while it has work and somewhere to put its product. Stages keep no state; the part's block entity holds it.
 */
public interface Stage {
	void tick(PlantBlockEntity part);

	/** True if this part takes this rock at this size (a null size is unbroken rock) from a belt, a hand or a hopper. */
	default boolean accepts(Rock rock, @Nullable SizeClass size) {
		return false;
	}

	/**
	 * True if rock from a belt's head end or a part's output face may come in at this face: its feed face as the spec draws it
	 * (crushing-circuit.md, "Nozzles and faces"). {@code at} is the cell in the part's own frame and {@code localSide} the face, turned
	 * with the part. A part with no feed face takes rock only by hand, hopper, or from the part it is built into (the jaw from its grizzly).
	 */
	default boolean feeds(PlantBlockEntity part, int[] at, Direction localSide) {
		return false;
	}

	/** A conveyor drive's metres from {@code belt} to its run's head end, where rock dropped on there starts its ride; 0 for other parts. */
	default double dropAt(PlantBlockEntity part, net.minecraft.core.BlockPos belt) {
		return 0;
	}

	/** The top of the cells at the north (feed) end: the first third of the part's length, at least one row. */
	static boolean feedEndTop(PlantBlockEntity part, int[] at, Direction localSide) {
		return localSide == Direction.UP && at[1] == part.machine().height() - 1 && at[2] < Math.max(1, part.machine().depth() / 3);
	}

	/** The top of any cell. */
	static boolean top(PlantBlockEntity part, int[] at, Direction localSide) {
		return localSide == Direction.UP && at[1] == part.machine().height() - 1;
	}

	/** How much dry rock it holds, kg. */
	default double holdKg() {
		return PlantRates.DRY_HOLD_KG;
	}

	/** The product a cell's side puts on steel pipe, in the part's own frame ({x, y, z}); null for none. */
	default @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
		return null;
	}

	/** Litres of water it holds; 0 for a part that takes none. */
	default double waterHold() {
		return 0;
	}

	/** The stage for a part. */
	static Stage of(Machine machine) {
		return switch (machine) {
			case GRIZZLY -> Crushing.GRIZZLY;
			case JAW -> Crushing.JAW;
			case SCREEN -> Crushing.SCREEN;
			case CONE -> Crushing.CONE;
			case BALL_MILL -> Grinding.MILL;
			case CYCLONE -> Grinding.CYCLONE;
			case CONVEYOR_DRIVE -> Belts.DRIVE;
			case ORE_BIN -> Belts.BIN;
			case SPIRAL_BANK -> Gravity.SPIRALS;
			case SCAVENGER_BANK -> FullRecovery.SCAVENGER;
			case REGRIND_MILL -> FullRecovery.REGRIND;
			case CLEANER_BANK -> FullRecovery.CLEANER;
			case LEACH_TANK -> FullRecovery.LEACH;
			case CCD_THICKENER -> FullRecovery.WASH;
			case ELECTROWINNING_BANK -> FullRecovery.ELECTROWIN;
			case BYPRODUCT_PLANT -> FullRecovery.BYPRODUCTS;
			case HEAP_PAD -> FullRecovery.HEAP;
			case CONDITIONER -> Flotation.CONDITIONER;
			case ACTIVATION_TANK -> Flotation.ACTIVATION;
			case FLOTATION_BANK -> Flotation.BANK;
			case BLOWER -> Flotation.BLOWER;
			case THICKENER -> Flotation.THICKENER;
			case FILTER -> Flotation.FILTER;
			case TAILINGS_POND -> Flotation.POND;
			case APRON_FEEDER -> Dump.FEEDER;
			case ORE_SORTER -> Dump.SORTER;
			case TAILINGS_THICKENER -> DryStackStages.THICKENER;
			case TAILINGS_FILTER -> DryStackStages.FILTER;
			case LOADOUT_BIN -> DryStackStages.BIN;
			case LOADOUT_FRAME -> DryStackStages.FRAME;
			case HOIST_HOUSE -> Shaft.HOIST;
			case MEASURING_FLASK -> Shaft.FLASK;
			case HEADFRAME_BIN -> Shaft.BIN;
			default -> Idle.INSTANCE;
		};
	}

	/** A part with nothing of its own to do yet. */
	final class Idle implements Stage {
		static final Idle INSTANCE = new Idle();

		@Override
		public void tick(PlantBlockEntity part) {
		}
	}
}
