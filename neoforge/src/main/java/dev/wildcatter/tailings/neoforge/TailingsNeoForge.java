package dev.wildcatter.tailings.neoforge;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.tailings.Tailings;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The NeoForge entry point. Wildcatter is a hard dependency and a compile dependency. */
@Mod(Tailings.MOD_ID)
public final class TailingsNeoForge {
	private static boolean registered;

	public TailingsNeoForge(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, event -> {
			if (!registered) {
				registered = true;
				Tailings.register(Wildcatter.MOD_ID);
				Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Tailings.id("tailings"), CreativeModeTab.builder()
						.title(Tailings.tabTitle())
						.icon(Tailings::tabIcon)
						.displayItems((parameters, output) -> Tailings.creativeTab(output::accept))
						.build());
			}
		});
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			dev.wildcatter.tailings.neoforge.client.TailingsNeoForgeClient.init(modBus);
		}
	}
}
