package dev.wildcatter.tailings.plant;

import dev.wildcatter.tailings.rock.Mineral;

/**
 * The copper smelter line's numbers (docs/copper-smelter.md): a small flash furnace on air, a Peirce-Smith converter, an anode furnace,
 * a casting wheel and an electrorefining tankhouse, at 6 t/h of concentrate, 1.68 t/h of copper. The line carries copper only, as the
 * plant's concentrate assay does; sulfur, flux, slag and fuel come from a real concentrate of 28 % Cu and 32 % S. Masses in kg, energy
 * in joules. Plain Java, so it is unit-tested without Minecraft.
 */
public final class Smelter {
	private Smelter() {
	}

	/** The real concentrate the line's figures take: 28 % Cu, 32 % S (proposed; real 20-35 % Cu). */
	public static final double CONC_CU = 0.28;
	public static final double CONC_S = 0.32;
	/** The flash furnace's matte: 55 % Cu, 22 % S (proposed: air flash furnaces made 45-60 % matte). */
	public static final double MATTE_CU = 0.55;
	public static final double MATTE_S = 0.22;
	/** Of the copper the furnace smelts, the share its discard slag carries away (proposed: 1.2 % Cu in 0.6 t a t of concentrate). */
	public static final double FURNACE_SLAG_LOSS = 0.025;
	/** Tonnes of smelting slag a tonne of concentrate (proposed). */
	public static final double SLAG_PER_CONC = 0.6;
	/** Of the copper the converter blows, the share its slag carries back to the furnace (proposed: converter slag 4-8 % Cu). */
	public static final double CONVERTER_SLAG_RETURN = 0.04;
	/** Silica flux: 0.15 t a t of concentrate in the furnace, 0.15 kg a kg of copper in the converter (proposed). */
	public static final double FURNACE_FLUX_PER_CONC = 0.15;
	public static final double CONVERTER_FLUX_PER_CU = 0.15;
	/** A block of silica sand, 1.6 t (dry sand, 1.6 t/m3), and four held at a furnace or converter. */
	public static final double SAND_BLOCK_KG = 1600;
	public static final double FLUX_HOLD_KG = 4 * SAND_BLOCK_KG;
	/** Gas: 0.30 GJ a t of concentrate to dry it, 1.0 GJ a t for the air flash furnace's burners, 1.1 GJ a t of copper to fire refine (proposed). */
	public static final double DRYER_J_PER_CONC_KG = 0.30e6;
	public static final double FURNACE_J_PER_CONC_KG = 1.0e6;
	public static final double ANODE_FURNACE_J_PER_CU_KG = 1.1e6;

	/** The line's rating: 1,680 kg of copper an hour, 6 t/h of concentrate. */
	public static final double CU_KGH = 1680;
	public static final double CU_KG_PER_TICK = CU_KGH / 3600 / 20;
	/** The casting wheel's rate, 25 t/h. */
	public static final double WHEEL_KGH = 25_000;

	/** The tankhouse: 72 cells in series at 20.5 kA and 0.30 V, 96 % current efficiency, Faraday's 1.186 g of copper an ampere-hour. */
	public static final int CELLS = 72;
	public static final double CELL_KA = 20.5;
	public static final double CELL_VOLTS = 0.30;
	public static final double CURRENT_EFFICIENCY = 0.96;
	public static final double G_PER_AH = 63.546 / (2 * 26.801);

	/** Copper held: the dryer's bin, the matte at the converter, the blister at the anode furnace, the wheel's tundish, the tankhouse's anodes. */
	public static final double DRYER_HOLD_KG = 3000;
	public static final double MATTE_HOLD_KG = 8000;
	public static final double BLISTER_HOLD_KG = 6000;
	public static final double ANODE_COPPER_HOLD_KG = 6000;
	public static final double WHEEL_HOLD_KG = 1200;
	public static final int TANKHOUSE_ANODES = 24;

	/** The copper's own name in an assay, as the filter's cake has it. */
	public static final String COPPER = Mineral.COPPER.mineralItemId();

	/** A copper anode: 400 kg, the copper of one ore block (14 ingots); real anodes are 350-400 kg. */
	public static double anodeKg() {
		return Mineral.COPPER.kgPerUnit() * 14;
	}

	/** A copper cathode: two ingots' copper, 57.1 kg; real cathodes are 40-60 kg. */
	public static double cathodeKg() {
		return Mineral.COPPER.kgPerUnit() * 2;
	}

	/** kg of concentrate a kg of copper. */
	public static double concPerCu() {
		return 1 / CONC_CU;
	}

	/** kg of sulfur the concentrate brings a kg of copper. */
	public static double sulfurPerCu() {
		return CONC_S / CONC_CU;
	}

	/** kg of sulfur left in the matte a kg of copper: what the converter blows off. */
	public static double converterSulfurPerCu() {
		return MATTE_S / MATTE_CU;
	}

	/** kg of sulfur the flash furnace sends to the acid plant a kg of copper smelted. */
	public static double furnaceSulfurPerCu() {
		return sulfurPerCu() - converterSulfurPerCu();
	}

	/** kg of silica sand the furnace takes a kg of copper. */
	public static double furnaceFluxPerCu() {
		return FURNACE_FLUX_PER_CONC * concPerCu();
	}

	/** kg of smelting slag a kg of copper. */
	public static double slagPerCu() {
		return SLAG_PER_CONC * concPerCu();
	}

	public static double dryerJoulesPerCu() {
		return DRYER_J_PER_CONC_KG * concPerCu();
	}

	public static double furnaceJoulesPerCu() {
		return FURNACE_J_PER_CONC_KG * concPerCu();
	}

	/** The tankhouse's copper rate at its rating, kg an hour: 72 cells at 20.5 kA. */
	public static double tankhouseKgh() {
		return CELLS * CELL_KA * 1000 * G_PER_AH * CURRENT_EFFICIENCY / 1000;
	}

	/** The tankhouse's DC volts and watts at its rating. */
	public static double tankhouseVolts() {
		return CELLS * CELL_VOLTS;
	}

	public static double tankhouseDcWatts() {
		return tankhouseVolts() * CELL_KA * 1000;
	}

	/** Of the copper in the concentrate, the share the line pays as cathode: all but the smelting slag's. */
	public static double recovery() {
		return 1 - FURNACE_SLAG_LOSS;
	}

	/** The filter's direct payout in a world with the copper_shortcut rule: a sold concentrate's payable copper, less charges (proposed). */
	public static final double SHORTCUT_PAYABLE = 0.85;
}
