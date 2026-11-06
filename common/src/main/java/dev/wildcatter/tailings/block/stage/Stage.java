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
			case CONDITIONER -> Flotation.CONDITIONER;
			case FLOTATION_BANK -> Flotation.BANK;
			case BLOWER -> Flotation.BLOWER;
			case THICKENER -> Flotation.THICKENER;
			case FILTER -> Flotation.FILTER;
			case TAILINGS_POND -> Flotation.POND;
			case APRON_FEEDER -> Dump.FEEDER;
			case ORE_SORTER -> Dump.SORTER;
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
