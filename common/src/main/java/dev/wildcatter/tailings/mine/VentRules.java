package dev.wildcatter.tailings.mine;

import dev.wildcatter.tailings.haul.TrainHarm;
import java.nio.file.Path;
import java.util.List;

/**
 * What ventilation may do to play, from {@code config/tailings-ventilation.properties} (docs/multiblocks/ventilation.md). Both rules
 * are off unless the file turns them on, so a world saved before L1 runs as it did (docs/questions/l1-ventilation.md):
 * <ul>
 * <li>{@code interlock=true}: a TBM with no duct air at its back-up for 60 s stops, NO VENTILATION;</li>
 * <li>{@code stale_air=true}: a player in a drive's stale air gets Mining Fatigue, then Weakness.</li>
 * </ul>
 * Off, both are read out only: the cab, the junction's PLC point and the gas detector still say POOR AIR.
 */
public final class VentRules {
	private static Object readFor;
	private static boolean interlock;
	private static boolean staleAir;

	private VentRules() {
	}

	/** Reads the file once per server run: {@code server} is whatever identifies the run. */
	private static synchronized void read(Object server, Path file) {
		if (readFor != server) {
			List<String> lines = TrainHarm.read(file);
			interlock = flag(lines, "interlock");
			staleAir = flag(lines, "stale_air");
			readFor = server;
		}
	}

	public static boolean interlock(Object server, Path file) {
		read(server, file);
		return interlock;
	}

	public static boolean staleAir(Object server, Path file) {
		read(server, file);
		return staleAir;
	}

	/** A key set to true; anything else, or no file, is false. */
	public static boolean flag(List<String> lines, String key) {
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.startsWith(key + "=")) {
				return Boolean.parseBoolean(trimmed.substring(key.length() + 1).trim());
			}
		}
		return false;
	}
}
