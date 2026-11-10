package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryHold;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlcScale;
import java.util.List;

/**
 * A slurry part's level and density gauges (docs/questions/plc-13i.md). They read its feed tank, the inlet buffer a part draws its
 * feed into off the pipe; the mill, the cyclones and the heap pad have none, and read their discharge sump; the paste mixer reads its
 * paste hopper. A part backed up by a full
 * outlet stops drawing down its feed, so its feed tank fills: the level shows it.
 */
public final class SlurryGauge {
	private SlurryGauge() {
	}

	public static final List<Product> SLURRIES = List.of(Product.LEACH_SLURRY, Product.PREGNANT_LIQUOR, Product.MILL_DISCHARGE, Product.FINES_SLURRY, Product.CONDITIONED_SLURRY,
			Product.CONCENTRATE_SLURRY, Product.THICKENED_CONCENTRATE, Product.TAILS_SLURRY);

	/** The wet kg its gauged tank holds when full, the hold the stage itself works to; 0 for a part that holds no slurry. */
	public static double hold(Machine machine) {
		return switch (machine) {
			case BALL_MILL -> Grinding.DISCHARGE_HOLD;
			case CYCLONE -> Grinding.OVERFLOW_HOLD;
			case SPIRAL_BANK, SCAVENGER_SPIRAL_BANK, CONDITIONER, ACTIVATION_TANK, FLOTATION_BANK, THICKENER, FILTER, SCAVENGER_BANK, REGRIND_MILL, CLEANER_BANK, LEACH_TANK, CCD_THICKENER,
					ELECTROWINNING_BANK, HEAP_PAD -> Flotation.HOLD;
			// the paste filter's feed of thickened tails (paste-backfill.md, L5; docs/questions/gaps2.md #3)
			case PASTE_FILTER -> Flotation.HOLD;
			// the mixer's paste hopper, what the pump draws on (Tim, 2026-11-10, gaps2.md #3)
			case PASTE_MIXER -> Paste.PASTE_HOLD;
			default -> 0;
		};
	}

	/** True for a part with slurry to gauge. */
	public static boolean gauged(Machine machine) {
		return hold(machine) > 0;
	}

	private static SlurryHold tank(PlantBlockEntity part) {
		return switch (part.machine()) {
			case BALL_MILL, CYCLONE, HEAP_PAD, PASTE_MIXER -> part.out();
			default -> part.back();
		};
	}

	/** Wet kg in its gauged tank. */
	public static double heldKg(PlantBlockEntity part) {
		SlurryHold tank = tank(part);
		double kg = 0;
		for (Product product : SLURRIES) {
			kg += tank.held(product);
		}
		return kg;
	}

	/** Its level, 0 to 1 of its tank (more when over-full). */
	public static double level(PlantBlockEntity part) {
		double hold = hold(part.machine());
		return hold <= 0 ? 0 : heldKg(part) / hold;
	}

	/**
	 * A product's density as its tank holds it: the solids its assay is marked with (thickened tails and paste carry
	 * {@code tailings:solids}, as Wildcatter 0.5.1 has no product for them), else the product's own.
	 */
	private static double density(Product product, SlurryHold tank) {
		double marked = tank.assayOf(product).get(dev.wildcatter.tailings.pit.DryStack.SOLIDS_KEY);
		return marked > 0 ? PlcScale.density(marked) : PlcScale.density(product);
	}

	/** The density of what its tank holds, kg/L; an empty tank reads as water. */
	public static double density(PlantBlockEntity part) {
		SlurryHold tank = tank(part);
		double kg = 0, litres = 0;
		for (Product product : SLURRIES) {
			double held = tank.held(product);
			kg += held;
			litres += held / density(product, tank);
		}
		return litres <= 1e-9 ? PlcScale.DENSITY_LOW : kg / litres;
	}
}
