package dev.wildcatter.tailings.pit;

import java.util.Locale;

/**
 * The dry-stack figures (docs/multiblocks/open-pit-p4-dry-stack.md): the tailings thickener, the filter press's batch cycle, the truck
 * load-out, berms at a dump's crest, and the dozer's cut to grade. Plain Java, unit-tested.
 */
public final class DryStack {
	/** The plant's tails, 35 % solids by mass (the flotation feed's). */
	public static final double FEED_SOLIDS = 0.35;
	/** The thickener's underflow, 60 % solids (proposed, the concentrate thickener's). */
	public static final double UNDERFLOW_SOLIDS = 0.60;
	/** The filter's cake at 15 % moisture: 85 % solids (open-pit.md; real 12-20 %). */
	public static final double CAKE_SOLIDS = 0.85;
	/**
	 * A thickened slurry's solids, carried in its assay as its own entry, as {@code Selective.DEPRESSED} is: Wildcatter 0.5.1 has no
	 * product for thickened tails, so the underflow goes on the pipe as tails slurry marked with what it is.
	 */
	public static final String SOLIDS_KEY = "tailings:solids";

	/** The press: 60 t/h of solids (open-pit.md), in a 15-minute cycle (a 2.5 m press maker's sizing example). */
	public static final double FILTER_KG_PER_HOUR = 60_000;
	public static final int CYCLE_SECONDS = 900;
	/** Fill 2, squeeze 3.5, blow 2 minutes; the rest opening the plates and dropping the cake. */
	public static final int FILL_SECONDS = 120;
	public static final int SQUEEZE_SECONDS = 210;
	public static final int BLOW_SECONDS = 120;
	/** 15 t of solids a cycle: 60 t/h for 15 minutes. */
	public static final double BATCH_KG = FILTER_KG_PER_HOUR * CYCLE_SECONDS / 3600;

	/** The load-out: a 400 t bin, filling a truck under its gate at 2 t/s (proposed), a truck's body centre within 2 m of the gate. */
	public static final double BIN_KG = 400_000;
	public static final double LOADOUT_KG_PER_SECOND = 2_000;
	public static final double GATE_REACH = 2.0;
	/** A self-driving truck at a LOADOUT leaves after 60 s with nothing more loaded (as at LOAD). */
	public static final int LOADOUT_PATIENCE_SECONDS = 60;

	/** A crest is a drop of 3 m or more within 10 m behind a DUMP POINT stake; a berm counts within 3 m; a truck stops 1 m short. */
	public static final int CREST_DROP = 3;
	public static final double CREST_SEARCH = 10;
	public static final double BERM_SEARCH = 3;
	public static final double BERM_STOP = 1;

	private DryStack() {
	}

	/** Litres of water a kg of solids carries in a slurry or cake of {@code solids} by mass. */
	public static double waterPerKgSolids(double solids) {
		return (1 - solids) / solids;
	}

	/** The thickener's overflow water, litres a kg of tails solids: about 1.2 (the spec). */
	public static double overflowPerKgSolids() {
		return waterPerKgSolids(FEED_SOLIDS) - waterPerKgSolids(UNDERFLOW_SOLIDS);
	}

	/** The press's filtrate, litres a kg of solids: about 0.49 (the spec). */
	public static double filtratePerKgSolids() {
		return waterPerKgSolids(UNDERFLOW_SOLIDS) - waterPerKgSolids(CAKE_SOLIDS);
	}

	/** Where in its cycle the press is, {@code seconds} in. */
	public static String phase(double seconds) {
		if (seconds < FILL_SECONDS) {
			return "filling";
		}
		if (seconds < FILL_SECONDS + SQUEEZE_SECONDS) {
			return "squeezing";
		}
		if (seconds < FILL_SECONDS + SQUEEZE_SECONDS + BLOW_SECONDS) {
			return "blowing";
		}
		return "opening and dropping the cake";
	}

	/** Seconds the load-out takes to fill {@code kg} into a truck. */
	public static double fillSeconds(double kg) {
		return kg / LOADOUT_KG_PER_SECOND;
	}

	/** Placed blocks of dry tails in {@code kg}. */
	public static double blocks(double kg) {
		return kg / PitMass.DRY_TAILS_KG;
	}

	/**
	 * A berm's height for a truck, in whole blocks: mid-axle height, the wheel's radius (US 30 CFR 56.9300(b)), rounded down to whole
	 * blocks (proposed), at least one.
	 */
	public static int bermBlocks(double wheelRadius) {
		return Math.max(1, (int) Math.floor(wheelRadius + 1e-9));
	}

	/** The 100 t truck's wheel, about 2.7 m across. */
	public static final double TRUCK_WHEEL_RADIUS = 1.35;

	/** The dozer's cut to grade: level, 5, 8 or 10 % (proposed: a block down each 20, 12 or 10 m; level never steps). */
	public enum Grade {
		LEVEL("level", 0),
		FIVE("5 %", 20),
		EIGHT("8 %", 12),
		TEN("10 %", 10);

		private final String label;
		private final int run;

		Grade(String label, int run) {
			this.label = label;
			this.run = run;
		}

		public String label() {
			return label;
		}

		/** Metres driven for each block it steps down; 0 never. */
		public int run() {
			return run;
		}

		public Grade next() {
			return values()[(ordinal() + 1) % values().length];
		}

		/** Blocks down below the first cut after {@code metres} of cutting. */
		public int stepsAfter(double metres) {
			return run == 0 ? 0 : (int) Math.floor(metres / run + 1e-9);
		}
	}

	/** Tonnes to one decimal, for readouts. */
	public static String tonnes(double kg) {
		return String.format(Locale.ROOT, "%.1f t", kg / 1000);
	}
}
