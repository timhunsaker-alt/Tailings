package dev.wildcatter.tailings.neoforge.client;

import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.block.DrillMenus;
import dev.wildcatter.tailings.client.DrillScreen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The NeoForge client side. Phase 0 has no client renderers. */
public final class TailingsNeoForgeClient {
	private TailingsNeoForgeClient() {
	}

	public static void init(IEventBus modBus) {
		modBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(DrillMenus.DRILL, DrillScreen::new));
		modBus.addListener(FMLClientSetupEvent.class, event -> Tailings.LOGGER.info("Tailings client ready"));
	}
}
