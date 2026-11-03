package dev.wildcatter.tailings.fabric.client;

import dev.wildcatter.tailings.Tailings;
import net.fabricmc.api.ClientModInitializer;

/** The Fabric client entry point. Phase 0 has no client renderers. */
public final class TailingsFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		Tailings.LOGGER.info("Tailings client ready");
	}
}
