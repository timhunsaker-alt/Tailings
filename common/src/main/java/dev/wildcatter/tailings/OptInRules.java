package dev.wildcatter.tailings;

import dev.wildcatter.tailings.haul.TrainHarm;
import dev.wildcatter.tailings.mine.VentRules;
import dev.wildcatter.tailings.platform.Platform;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import org.jspecify.annotations.Nullable;

/**
 * Fixes that change what an existing world does, off until Tim decides (docs/questions/gaps2.md, #4 to #6), from
 * {@code config/tailings-rules.properties}, read once per server run as {@link VentRules} reads ventilation's file:
 * <ul>
 * <li>{@code headframe_hopper=true}: a headframe bin with no belt under it feeds a hopper or chest under it;</li>
 * <li>{@code paste_wall_breach=true}: a solid wall removed beside wet paste lets it spill, as a broken bulkhead does;</li>
 * <li>{@code paste_unloaded_cure=true}: paste makes up the time its chunk was unloaded.</li>
 * </ul>
 * Missing file or key: off.
 */
public final class OptInRules {
	public static final String HEADFRAME_HOPPER = "headframe_hopper";
	public static final String PASTE_WALL_BREACH = "paste_wall_breach";
	public static final String PASTE_UNLOADED_CURE = "paste_unloaded_cure";
	public static final String FILE = "tailings-rules.properties";

	private static Object readFor;
	private static List<String> lines = List.of();
	/** A game test's setting, over the file's, until it sets it back to null. */
	private static final Map<String, Boolean> TEST = new HashMap<>();

	private OptInRules() {
	}

	/** Whether {@code key} is on for this server run. */
	public static synchronized boolean on(@Nullable MinecraftServer server, String key) {
		Boolean test = TEST.get(key);
		if (test != null) {
			return test;
		}
		if (server == null) {
			return false;
		}
		if (readFor != server) {
			lines = TrainHarm.read(Platform.INSTANCE.configDir().resolve(FILE));
			readFor = server;
		}
		return VentRules.flag(lines, key);
	}

	/** Test hook: turns {@code key} on or off over the file; null hands it back to the file. */
	public static synchronized void setTest(String key, @Nullable Boolean value) {
		if (value == null) {
			TEST.remove(key);
		} else {
			TEST.put(key, value);
		}
	}
}
