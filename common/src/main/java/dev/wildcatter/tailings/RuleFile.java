package dev.wildcatter.tailings;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * The old rules files, {@code config/tailings-rules.properties} and {@code config/tailings-ventilation.properties}, which
 * {@link OptInRules} reads once, when a world's rules are first made. Plain Java.
 */
public final class RuleFile {
	private RuleFile() {
	}

	/** A key's value: true or false as written (anything but {@code true} is false), null when it is not there. */
	public static @Nullable Boolean value(List<String> lines, String key) {
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.startsWith(key + "=")) {
				return Boolean.parseBoolean(trimmed.substring(key.length() + 1).trim());
			}
		}
		return null;
	}
}
