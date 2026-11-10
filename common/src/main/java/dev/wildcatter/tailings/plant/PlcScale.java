package dev.wildcatter.tailings.plant;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.tailings.rock.Rock;
import org.jspecify.annotations.Nullable;

/**
 * How Tailings' word signals sit on a 4-20 mA loop (docs/questions/plc-13i.md): the sorter's rock codes, the TBM's course, and a
 * slurry part's level, flow and density. Plain Java.
 */
public final class PlcScale {
	private PlcScale() {
	}

	// ---- the sorter's rock codes ---------------------------------------------------------------------------------------------------

	/** A rock code is a step of a quarter milliamp: code n at 4 + n / 4 mA, so 64 codes fit a loop and the next rock never moves the others. */
	public static final double CODE_MA = 0.25;

	/** The code a loop carries: the nearest step, never under 0. */
	public static int code(double milliamps) {
		return Math.max(0, (int) Math.round((milliamps - 4) / CODE_MA));
	}

	/** The loop current that carries {@code code}. */
	public static double codeMa(int code) {
		return 4 + code * CODE_MA;
	}

	/** Code 1 is the first rock in {@link Rock}'s list, in its order (new rocks are added at its end); 0 and codes past the list are none. */
	public static @Nullable Rock rock(int code) {
		return code >= 1 && code <= Rock.values().length ? Rock.values()[code - 1] : null;
	}

	public static int codeOf(Rock rock) {
		return rock.ordinal() + 1;
	}

	// ---- the TBM's course ----------------------------------------------------------------------------------------------------------

	/** TARGET Y spans the overworld's height, -64 at 4 mA to 320 at 20 mA: 24 blocks a milliamp. */
	public static final int TARGET_Y_LOW = -64;
	public static final int TARGET_Y_HIGH = 320;
	/** FLAT LENGTH spans 0 to 400 m, as CABLE LEFT does. */
	public static final int FLAT_HIGH = 400;

	/** The target Y a loop at {@code fraction} of its span (0 at 4 mA, 1 at 20) asks for. */
	public static int targetY(double fraction) {
		return (int) Math.round(TARGET_Y_LOW + clamp(fraction) * (TARGET_Y_HIGH - TARGET_Y_LOW));
	}

	public static double targetFraction(int y) {
		return (y - TARGET_Y_LOW) / (double) (TARGET_Y_HIGH - TARGET_Y_LOW);
	}

	public static int flatLength(double fraction) {
		return (int) Math.round(clamp(fraction) * FLAT_HIGH);
	}

	/**
	 * The target Y a PLC may set without moving floor already cut, since the floor of every slice is worked out from the target: before
	 * the first advance, any; while the drive still declines, no higher than the floor it stands on (it levels out there); once it has
	 * levelled out, the target it has. {@code low} is the lowest floor the world allows.
	 */
	public static int allowedTarget(int wanted, int advanced, boolean declining, int floorY, int target, int low) {
		int y = Math.max(low, wanted);
		if (advanced <= 0) {
			return y;
		}
		return declining ? Math.min(y, floorY) : target;
	}

	// ---- slurry --------------------------------------------------------------------------------------------------------------------

	/** The solids are rock at 2.7 t/m3, as a block is (PlantRates.BLOCK_KG). */
	public static final double SOLIDS_SG = PlantRates.BLOCK_KG / 1000;
	/** DENSITY spans 1.0 kg/L (water) at 4 mA to 2.0 at 20 mA: the mill's 70 % discharge, the densest, is 1.79. */
	public static final double DENSITY_LOW = 1.0;
	public static final double DENSITY_HIGH = 2.0;
	/** FLOW spans 0 to 60 L/s: the flotation feed is 31 L/s (flotation-bank.md), the mill's discharge with its circulating load 39. */
	public static final double FLOW_HIGH = 60;

	/** Each product's solids by mass, from the specs; pregnant liquor is a solution, read as water. */
	public static double solids(Product product) {
		return switch (product) {
			case MILL_DISCHARGE -> PlantRates.MILL_SOLIDS;
			case FINES_SLURRY, CONDITIONED_SLURRY, TAILS_SLURRY, LEACH_SLURRY -> PlantRates.OVERFLOW_SOLIDS;
			case CONCENTRATE_SLURRY -> 0.30;
			case THICKENED_CONCENTRATE -> 0.60;
			default -> 0;
		};
	}

	/** Slurry's density in kg/L at {@code solids} by mass: 1 / (s / 2.7 + (1 - s)). */
	public static double density(double solids) {
		return 1 / (solids / SOLIDS_SG + (1 - solids));
	}

	public static double density(Product product) {
		return density(solids(product));
	}

	/** The loop current for {@code value} over {@code low} to {@code high}, held to 3.8-20.5 mA as a transmitter's over and under range is (NE 43). */
	public static double ma(double value, double low, double high) {
		return Math.max(3.8, Math.min(20.5, 4 + 16 * (value - low) / (high - low)));
	}

	private static double clamp(double fraction) {
		return Math.max(0, Math.min(1, fraction));
	}
}
