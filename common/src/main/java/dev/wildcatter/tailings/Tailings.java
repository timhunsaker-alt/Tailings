package dev.wildcatter.tailings;

import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod, the same on every loader. Each loader's entry point calls {@link #register(String)} while the game is starting.
 * Plants, blocks and items are not registered in Phase 0.
 */
public final class Tailings {
	public static final String MOD_ID = "tailings";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private Tailings() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** {@code requiredMod} is Wildcatter's mod id, read off the Wildcatter jar on the compile classpath. */
	public static void register(String requiredMod) {
		LOGGER.info("Tailings loaded; requires {}", requiredMod);
	}
}
