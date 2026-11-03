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

	private TrainHarm() {
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
