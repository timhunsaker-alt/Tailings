package dev.wildcatter.tailings.neoforge.client;

import dev.wildcatter.tailings.Tailings;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/** The NeoForge client side. Phase 0 has no client renderers. */
public final class TailingsNeoForgeClient {
	private TailingsNeoForgeClient() {
	}

	public static void init(IEventBus modBus) {
		modBus.addListener(FMLClientSetupEvent.class, event -> Tailings.LOGGER.info("Tailings client ready"));
	}
}
