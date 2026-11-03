package dev.wildcatter.tailings.neoforge;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.block.DrillMenu;
import dev.wildcatter.tailings.block.DrillMenus;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** The NeoForge entry point. Wildcatter is a hard dependency and a compile dependency. */
@Mod(Tailings.MOD_ID)
public final class TailingsNeoForge {
	private static boolean registered;

	public TailingsNeoForge(IEventBus modBus) {
		modBus.addListener(RegisterTicketControllersEvent.class, event -> event.register(NeoForgePlatform.CHUNKS));
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
			event.register(Registries.MENU, helper -> {
				if (DrillMenus.DRILL == null) {
					MenuType<DrillMenu> menu = IMenuTypeExtension.create((id, inventory, data) -> new DrillMenu(id, inventory, data.readBlockPos()));
					DrillMenus.DRILL = menu;
					helper.register(Tailings.id("tunnel_drill"), menu);
				}
			});
		});
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			dev.wildcatter.tailings.neoforge.client.TailingsNeoForgeClient.init(modBus);
		}
	}
}
