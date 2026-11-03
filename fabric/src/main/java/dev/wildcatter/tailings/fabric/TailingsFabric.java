package dev.wildcatter.tailings.fabric;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.tailings.Tailings;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

/** The Fabric entry point. Wildcatter is a hard dependency and a compile dependency. */
public final class TailingsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Tailings.register(Wildcatter.MOD_ID);
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Tailings.id("tailings"), FabricCreativeModeTab.builder()
				.title(Tailings.tabTitle())
				.icon(Tailings::tabIcon)
				.displayItems((parameters, output) -> Tailings.creativeTab(output::accept))
				.build());
	}
}
