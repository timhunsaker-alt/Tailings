package dev.wildcatter.tailings.fabric.client;

import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.block.DrillMenus;
import dev.wildcatter.tailings.client.DrillScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

/** The Fabric client entry point. Phase 0 has no client renderers. */
public final class TailingsFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(DrillMenus.DRILL, DrillScreen::new);
		Tailings.LOGGER.info("Tailings client ready");
	}
}
