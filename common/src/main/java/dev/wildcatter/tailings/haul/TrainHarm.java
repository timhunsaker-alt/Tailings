package dev.wildcatter.tailings.haul;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Whether a train hurts what it hits. {@code config/tailings-train.properties} may set {@code damage=false} or
 * {@code amount=8}. Missing file means trains do 8 damage.
 */
public final class TrainHarm {
	public static final float DEFAULT_AMOUNT = 8.0F;
	private static Object readFor;
	private static boolean cachedEnabled = true;
	private static float cachedAmount = DEFAULT_AMOUNT;
	private static float cachedTruck = DEFAULT_AMOUNT;

	private TrainHarm() {
	}

	/** The settings, read from {@code file} once per server run: {@code server} is whatever identifies the run. */
	public static synchronized float damageFor(Object server, Path file) {
		if (readFor != server) {
			List<String> lines = read(file);
			cachedEnabled = enabled(lines);
			cachedAmount = amount(lines);
			cachedTruck = truckAmount(lines);
			readFor = server;
		}
		return cachedEnabled ? cachedAmount : 0;
	}

	/** What a moving haul truck or loader does to what it hits (open-pit.md): {@code truck_damage=8}; {@code damage=false} turns both off. */
	public static synchronized float truckDamageFor(Object server, Path file) {
		damageFor(server, file);
		return cachedEnabled ? cachedTruck : 0;
	}

	public static float truckAmount(List<String> lines) {
		try {
			return Float.parseFloat(value(lines, "truck_damage", Float.toString(DEFAULT_AMOUNT)));
		} catch (NumberFormatException ex) {
			return DEFAULT_AMOUNT;
		}
	}

	public static boolean enabled(List<String> lines) {
		return Boolean.parseBoolean(value(lines, "damage", "true"));
	}

	public static float amount(List<String> lines) {
		try {
			return Float.parseFloat(value(lines, "amount", Float.toString(DEFAULT_AMOUNT)));
		} catch (NumberFormatException ex) {
			return DEFAULT_AMOUNT;
		}
	}

	public static List<String> read(Path file) {
		if (file == null || !Files.exists(file)) {
			return List.of();
		}
		try {
			return Files.readAllLines(file);
		} catch (IOException ex) {
			return List.of();
		}
	}

	private static String value(List<String> lines, String key, String fallback) {
		if (lines == null) {
			return fallback;
		}
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.startsWith(key + "=")) {
				return trimmed.substring(key.length() + 1).trim();
			}
		}
		return fallback;
	}
}
