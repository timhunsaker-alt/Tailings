package dev.wildcatter.tailings.haul;

/** Why a train does or does not take the next block. */
public final class Motion {
	public enum Halt {
		NONE,
		UNPOWERED,
		NO_RACK,
		SIGNAL,
		POCKET
	}

	private Motion() {
	}

	public static int ticks(boolean decline) {
		return decline ? Haul.DECLINE_TICKS : Haul.FLAT_TICKS;
	}

	/**
	 * {@code decline} is a rail that is not flat. It must have its rack. {@code fed} is the rectifier reaching the
	 * rail under the locomotive. {@code sectionClear} is the next block signal. {@code pocketReady} is the dump.
	 */
	public static Halt halt(boolean fed, boolean decline, boolean rack, boolean sectionClear, boolean pocketReady) {
		if (!fed) {
			return Halt.UNPOWERED;
		}
		if (decline && !rack) {
			return Halt.NO_RACK;
		}
		if (!sectionClear) {
			return Halt.SIGNAL;
		}
		if (!pocketReady) {
			return Halt.POCKET;
		}
		return Halt.NONE;
	}
}
