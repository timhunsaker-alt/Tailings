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
			event.register(Registries.MENU, helper -> {
				if (DrillMenus.DRILL == null) {
					MenuType<DrillMenu> menu = IMenuTypeExtension.create((id, inventory, data) -> new DrillMenu(id, inventory, data.readBlockPos()));
					DrillMenus.DRILL = menu;
					helper.register(Tailings.id("tunnel_drill"), menu);
				}
				if (DrillMenus.SORTER == null) {
					MenuType<dev.wildcatter.tailings.block.SorterMenu> sorter = new MenuType<>(dev.wildcatter.tailings.block.SorterMenu::new,
							net.minecraft.world.flag.FeatureFlags.VANILLA_SET);
					DrillMenus.SORTER = sorter;
					helper.register(Tailings.id("ore_sorter"), sorter);
				}
			});
		});
		// the drive's air: players in stale air, once a second (ventilation.md)
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.LevelTickEvent.Post.class, event -> {
			if (event.getLevel() instanceof net.minecraft.server.level.ServerLevel level) {
				dev.wildcatter.tailings.block.VentNetwork.tickPlayers(level);
			}
		});
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			dev.wildcatter.tailings.neoforge.client.TailingsNeoForgeClient.init(modBus);
		}
	}
}
