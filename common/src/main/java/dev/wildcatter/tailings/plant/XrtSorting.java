package dev.wildcatter.tailings.plant;

import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * The X-ray transmission ore sorter's rules (docs/multiblocks/xrt-ore-sorter.md, L8), plain Java. A real XRT sorter shoots each piece
 * on its belt with X-rays at two energies; how much of each gets through gives the piece's mean atomic number (its "atomic density"),
 * which tells heavy-element ore from light silicate rock. Here each piece is the rock it came from: its <b>effective atomic number</b>
 * is worked out from the real chemical formula of each mineral it holds, at the game grade, and the rest of its 2.7 t taken as quartz.
 * <ul>
 * <li>Each formula's effective atomic number is the power-law mean, Z<sub>eff</sub> = (Σ f<sub>i</sub> Z<sub>i</sub><sup>2.94</sup>)<sup>1/2.94</sup>,
 * f<sub>i</sub> each element's share of the electrons (the photoelectric rule of Mayneord and Spiers): quartz 11.6, galena 77.</li>
 * <li>A piece mixes its minerals the same way, each weighted by its electrons (its kg times its electrons a kg).</li>
 * <li>Over the threshold a piece is blown off the belt to <b>product</b>; under it, it drops to <b>waste</b>. Real sorters misplace a
 * few pieces each way: 3 % of the pieces over the threshold drop to waste and 5 % of those under it are blown with the product.</li>
 * </ul>
 * Country rock reads 11.6-12.0 (its trace minerals barely lift it). Light ores (graphite, magnesite, borax, bauxite) read under quartz:
 * this sorter, which blows the heavy pieces, cannot upgrade them (l8-xrt.md).
 */
public final class XrtSorting {
	private XrtSorting() {
	}

	/** The power of Z in the photoelectric mean (Mayneord and Spiers, 2.94). */
	public static final double POWER = 2.94;
	/** The five threshold steps on its screen, as effective atomic numbers: LOW passes everything, HIGH only the rich heavy ores (proposed). */
	public static final double[] THRESHOLDS = {11.5, 12.0, 13.0, 15.0, 20.0};
	/** The step a new sorter starts at: just over the heaviest country rock, deepslate (11.96), so every rock goes to waste and every heavy ore to product. */
	public static final int DEFAULT_STEP = 1;
	/** Of the pieces over the threshold, the share that drops to waste anyway (xrt-ore-sorter.md, proposed). */
	public static final double ORE_TO_WASTE = 0.03;
	/** Of the pieces under the threshold, the share blown with the product anyway (proposed). */
	public static final double WASTE_TO_PRODUCT = 0.05;
	/** The 1.2 m sorter's rating on the game's crushed class (40-150 mm): 250 t/h (proposed; makers rate it up to 250 t/h at 50-125 mm). */
	public static final double TONNES_PER_HOUR = 250;
	public static final double KG_PER_TICK = TONNES_PER_HOUR * 1000 / 72_000;
	/** On gravel (6-40 mm) it treats half the tonnage: smaller pieces, the same belt area (proposed; a maker's table halves it at 25-50 mm). */
	public static final double GRAVEL_CAPACITY = 0.5;
	/** The game's block, 2.7 t: what a piece's minerals are a share of. */
	private static final double PIECE_KG = PlantRates.BLOCK_KG;
	/** What the rest of a piece is: quartz, the common silicate (feldspars read about the same). */
	public static final String GANGUE = "SiO2";

	/** Each element the formulas use: its atomic number and its standard atomic weight. */
	private static final Map<String, double[]> ELEMENTS = Map.ofEntries(
			Map.entry("H", new double[] {1, 1.008}), Map.entry("Li", new double[] {3, 6.94}), Map.entry("B", new double[] {5, 10.81}),
			Map.entry("C", new double[] {6, 12.011}), Map.entry("O", new double[] {8, 15.999}), Map.entry("F", new double[] {9, 18.998}),
			Map.entry("Na", new double[] {11, 22.990}), Map.entry("Mg", new double[] {12, 24.305}), Map.entry("Al", new double[] {13, 26.982}),
			Map.entry("Si", new double[] {14, 28.085}), Map.entry("P", new double[] {15, 30.974}), Map.entry("S", new double[] {16, 32.06}),
			Map.entry("Cl", new double[] {17, 35.45}), Map.entry("K", new double[] {19, 39.098}), Map.entry("Ca", new double[] {20, 40.078}),
			Map.entry("Ti", new double[] {22, 47.867}), Map.entry("Cr", new double[] {24, 51.996}), Map.entry("Mn", new double[] {25, 54.938}),
			Map.entry("Fe", new double[] {26, 55.845}), Map.entry("Co", new double[] {27, 58.933}), Map.entry("Ni", new double[] {28, 58.693}),
			Map.entry("Cu", new double[] {29, 63.546}), Map.entry("Zn", new double[] {30, 65.38}), Map.entry("As", new double[] {33, 74.922}),
			Map.entry("Zr", new double[] {40, 91.224}), Map.entry("Mo", new double[] {42, 95.95}), Map.entry("Ag", new double[] {47, 107.87}),
			Map.entry("Sn", new double[] {50, 118.71}), Map.entry("Ba", new double[] {56, 137.33}), Map.entry("Ce", new double[] {58, 140.12}),
			Map.entry("W", new double[] {74, 183.84}), Map.entry("Pt", new double[] {78, 195.08}), Map.entry("Au", new double[] {79, 196.97}),
			Map.entry("Pb", new double[] {82, 207.2}), Map.entry("U", new double[] {92, 238.03}));

	/** Each mineral's real formula (hydrates and solid solutions written out whole): what the X-rays see. */
	public static String formula(Mineral mineral) {
		return switch (mineral) {
			case IRON -> "Fe2O3";               // hematite, the iron ore mineral
			case COPPER -> "CuFeS2";            // chalcopyrite
			case GOLD -> "Au";                  // native gold
			case ZIRCON -> "ZrSiO4";
			case ILMENITE -> "FeTiO3";
			case RUTILE -> "TiO2";
			case MONAZITE -> "CePO4";
			case URANINITE, PITCHBLENDE -> "UO2";
			case SULFUR -> "S";
			case HALITE -> "NaCl";
			case SYLVITE -> "KCl";
			case FLUORITE -> "CaF2";
			case BAUXITE -> "AlO3H3";            // gibbsite, Al(OH)3
			case GALENA -> "PbS";
			case SPHALERITE -> "ZnS";
			case CASSITERITE -> "SnO2";
			case PENTLANDITE -> "Fe9Ni9S16";     // (Fe,Ni)9S8, iron and nickel half and half
			case CHROMITE -> "FeCr2O4";
			case PLATINUM -> "Pt";
			case MOLYBDENITE -> "MoS2";
			case MAGNESITE -> "MgCO3";
			case DOLOMITE -> "CaMgC2O6";         // CaMg(CO3)2
			case GRAPHITE -> "C";
			case BORAX -> "Na2B4O17H20";         // Na2B4O7.10H2O
			case SPODUMENE -> "LiAlSi2O6";
			case BARITE -> "BaSO4";
			case BENTONITE -> "Al2Si4O12H2";     // montmorillonite, its interlayer water and cations left out
			case ACANTHITE -> "Ag2S";
			case SCHEELITE -> "CaWO4";
			case COBALTITE -> "CoAsS";
			case PYROLUSITE -> "MnO2";
			case PHOSPHATE -> "Ca5P3O12F";       // fluorapatite, Ca5(PO4)3F
		};
	}

	private static final Pattern TERM = Pattern.compile("([A-Z][a-z]?)(\\d*)");

	/** A formula's electrons a gram, and the electron-weighted mean of Z^2.94 over its electrons. */
	record Mix(double electronsPerGram, double meanZp) {
	}

	static Mix mix(String formula) {
		double electrons = 0, weighted = 0, mass = 0;
		Matcher m = TERM.matcher(formula);
		while (m.find()) {
			double[] element = ELEMENTS.get(m.group(1));
			if (element == null) {
				throw new IllegalArgumentException("no element " + m.group(1) + " in " + formula);
			}
			int n = m.group(2).isEmpty() ? 1 : Integer.parseInt(m.group(2));
			electrons += element[0] * n;
			weighted += element[0] * n * Math.pow(element[0], POWER);
			mass += element[1] * n;
		}
		return new Mix(electrons / mass, weighted / electrons);
	}

	/** A formula's effective atomic number: quartz 11.6, galena 77.2. */
	public static double zeff(String formula) {
		return Math.pow(mix(formula).meanZp(), 1 / POWER);
	}

	public static double zeff(Mineral mineral) {
		return zeff(formula(mineral));
	}

	private static final Map<Rock, Double> PIECE = new EnumMap<>(Rock.class);

	/** The effective atomic number the sorter reads for a piece of {@code rock}: its minerals at the game grade, the rest quartz. */
	public static double pieceZeff(Rock rock) {
		return PIECE.computeIfAbsent(rock, XrtSorting::reckon);
	}

	private static double reckon(Rock rock) {
		double electrons = 0, weighted = 0, used = 0;
		for (Mineral mineral : rock.minerals()) {
			double kg = Math.min(rock.contained(mineral).doubleValue() * mineral.kgPerUnit(), PIECE_KG - used);
			used += kg;
			Mix mix = mix(formula(mineral));
			electrons += kg * mix.electronsPerGram();
			weighted += kg * mix.electronsPerGram() * mix.meanZp();
		}
		Mix gangue = mix(GANGUE);
		double rest = PIECE_KG - used;
		electrons += rest * gangue.electronsPerGram();
		weighted += rest * gangue.electronsPerGram() * gangue.meanZp();
		return Math.pow(weighted / electrons, 1 / POWER);
	}

	/** True if a piece of {@code rock} reads over the threshold at {@code step}: it is ejected as product (but for the error). */
	public static boolean over(Rock rock, int step) {
		return pieceZeff(rock) > THRESHOLDS[clamp(step)];
	}

	/** The share of {@code rock}'s kg that the sorter blows to product at {@code step}: 97 % over the threshold, 5 % under it. */
	public static double productShare(Rock rock, int step) {
		return productShare(rock, step, false);
	}

	/**
	 * As {@link #productShare(Rock, int)}; in reverse the ejectors blow the pieces that read under the threshold, for the light ores
	 * (graphite, magnesite, borax, bauxite) that read under quartz: 97 % of a rock under it to product, 5 % of one over it (Tim,
	 * 2026-11-10, l8-xrt.md #5). The errors are the ejectors', so they follow the stream blown.
	 */
	public static double productShare(Rock rock, int step, boolean reverse) {
		boolean kept = over(rock, step) != reverse;
		return kept ? 1 - ORE_TO_WASTE : WASTE_TO_PRODUCT;
	}

	/** True for what the sorter takes: crushed rock and gravel, inside its 10-125 mm range as the game's classes go; sand and fines not. */
	public static boolean sorts(@Nullable SizeClass size) {
		return size == SizeClass.CRUSHED || size == SizeClass.GRAVEL;
	}

	/** How many kg it treats a tick at full motor, on a load {@code gravelShare} of which is gravel (gravel at half the rate). */
	public static double kgPerTick(double gravelShare) {
		double share = Math.max(0, Math.min(1, gravelShare));
		return KG_PER_TICK / ((1 - share) + share / GRAVEL_CAPACITY);
	}

	public static int clamp(int step) {
		return Math.max(0, Math.min(THRESHOLDS.length - 1, step));
	}

	/** The step a THRESHOLD loop asks for: 4 mA the first, 20 mA the fifth, 4 mA a step. */
	public static int stepOf(double fraction) {
		return clamp((int) Math.round(fraction * (THRESHOLDS.length - 1)));
	}

	/** The loop current that asks for {@code step}. */
	public static double maOf(int step) {
		return 4 + 16.0 * clamp(step) / (THRESHOLDS.length - 1);
	}

	/** "threshold 2 of 5 (Zeff 12.0)". */
	public static String describe(int step) {
		return describe(step, false);
	}

	/** "threshold 2 of 5 (Zeff 12.0)", and in reverse ", REVERSE: keeps what reads under it". */
	public static String describe(int step, boolean reverse) {
		int s = clamp(step);
		String name = s == 0 ? " LOW" : s == THRESHOLDS.length - 1 ? " HIGH" : "";
		return String.format(Locale.ROOT, "threshold %d of %d%s (Zeff %.1f)%s", s + 1, THRESHOLDS.length, name, THRESHOLDS[s], reverse ? ", REVERSE: keeps what reads under it" : "");
	}

	/**
	 * A sorter's own state: the step its screen is set to, and the tonnes it has sent each way (its PRODUCT and WASTE pulse counts, one
	 * pulse a tonne, as a belt scale's totaliser gives them).
	 */
	public static final class State {
		private int step = DEFAULT_STEP;
		/** Reverse mode: the ejectors blow what reads under the threshold (off, as built, in a new sorter). */
		private boolean reverse;
		private double productKg;
		private double wasteKg;

		public int step() {
			return step;
		}

		public boolean reverse() {
			return reverse;
		}

		public void setReverse(boolean reverse) {
			this.reverse = reverse;
		}

		/** The screen's next step: LOW to HIGH, then LOW to HIGH again in reverse, then round. */
		public int nextStep() {
			if (step + 1 < THRESHOLDS.length) {
				step++;
			} else {
				step = 0;
				reverse = !reverse;
			}
			return step;
		}

		public void setStep(int step) {
			this.step = clamp(step);
		}

		public void sent(double toProduct, double toWaste) {
			productKg += Math.max(0, toProduct);
			wasteKg += Math.max(0, toWaste);
		}

		public double productKg() {
			return productKg;
		}

		public double wasteKg() {
			return wasteKg;
		}

		/** A pulse a whole tonne. */
		public long productPulses() {
			return (long) Math.floor(productKg / 1000 + 1e-9);
		}

		public long wastePulses() {
			return (long) Math.floor(wasteKg / 1000 + 1e-9);
		}

		/** Saved as {@code step|productKg|wasteKg}, and {@code |R} in reverse. */
		public String save() {
			return step + "|" + productKg + "|" + wasteKg + (reverse ? "|R" : "");
		}

		public void load(@Nullable String text) {
			step = DEFAULT_STEP;
			reverse = false;
			productKg = 0;
			wasteKg = 0;
			if (text == null || text.isBlank()) {
				return;
			}
			String[] part = text.split("\\|");
			try {
				step = clamp(Integer.parseInt(part[0]));
				productKg = part.length > 1 ? Double.parseDouble(part[1]) : 0;
				wasteKg = part.length > 2 ? Double.parseDouble(part[2]) : 0;
				reverse = part.length > 3 && "R".equals(part[3]);
			} catch (NumberFormatException ex) {
				step = DEFAULT_STEP;
			}
		}
	}
}
