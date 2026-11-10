package dev.wildcatter.tailings.mine;

/** Status numbers the cab screen shows. Shared so the block and the menu agree. */
public final class DrillCodes {
	public static final int IDLE = 0;
	public static final int NEED_PUMP = 1;
	public static final int NEED_POWER = 2;
	public static final int DRIVING = 3;
	public static final int STOP_TARGET = 4;
	public static final int STOP_ORE = 5;
	public static final int STOP_CABLE = 6;
	public static final int STOP_LIQUID = 7;
	public static final int STOP_EMPTY = 8;
	public static final int STOP_BUFFER = 9;
	public static final int STOP_BLOCKED = 10;
	public static final int NOT_BUILT = 11;
	/** No duct air at the back-up for 60 s, where the ventilation interlock is on (ventilation.md, L1). */
	public static final int NO_VENTILATION = 12;

	public static final int LACK_NONE = 0;
	public static final int LACK_SEGMENTS = 1;
	public static final int LACK_RAIL = 2;
	public static final int LACK_GRAVEL = 3;
	public static final int LACK_FLOOR = 4;
	public static final int LACK_COBBLE = 5;
	public static final int LACK_WATER = 6;

	private DrillCodes() {
	}

	public static int fromStop(Advance.Stop stop) {
		return switch (stop) {
			case TARGET_REACHED -> STOP_TARGET;
			case ORE -> STOP_ORE;
			case CABLE -> STOP_CABLE;
			case LIQUID -> STOP_LIQUID;
			case EMPTY -> STOP_EMPTY;
			case BUFFER_FULL -> STOP_BUFFER;
			case BLOCKED -> STOP_BLOCKED;
			case NONE -> DRIVING;
		};
	}

	public static int lack(String detail) {
		if (detail == null) {
			return LACK_NONE;
		}
		if (detail.contains("lining")) {
			return LACK_SEGMENTS;
		}
		if (detail.contains("mine rail")) {
			return LACK_RAIL;
		}
		if (detail.contains("gravel")) {
			return LACK_GRAVEL;
		}
		if (detail.contains("floor rock")) {
			return LACK_FLOOR;
		}
		if (detail.contains("cobble")) {
			return LACK_COBBLE;
		}
		if (detail.contains("water") || detail.contains("quench")) {
			return LACK_WATER;
		}
		return LACK_NONE;
	}

	public static String phrase(int status, int lack) {
		String lackText = switch (lack) {
			case LACK_SEGMENTS -> "lining segments";
			case LACK_RAIL -> "mine rail";
			case LACK_GRAVEL -> "gravel";
			case LACK_FLOOR -> "floor rock";
			case LACK_COBBLE -> "cobble";
			case LACK_WATER -> "quench water";
			default -> "";
		};
		String base = switch (status) {
			case NEED_PUMP -> "needs the dewatering pump behind the main beam";
			case NOT_BUILT -> "NOT BUILT";
			case NEED_POWER -> "waiting for power";
			case DRIVING -> "driving";
			case STOP_TARGET -> "target reached";
			case STOP_ORE -> "stopped: ore ahead";
			case STOP_CABLE -> "stopped: trailing cable ended";
			case STOP_LIQUID -> "stopped: liquid it cannot clear";
			case STOP_EMPTY -> "stopped: out of stock";
			case STOP_BUFFER -> "paused: muck buffer full";
			case STOP_BLOCKED -> "stopped: blocked";
			case NO_VENTILATION -> "NO VENTILATION";
			default -> "idle";
		};
		return lackText.isEmpty() ? base : base + " (" + lackText + ")";
	}
}
