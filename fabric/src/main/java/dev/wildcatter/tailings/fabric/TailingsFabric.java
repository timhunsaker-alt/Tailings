package dev.wildcatter.tailings.fabric;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.block.DrillMenu;
import dev.wildcatter.tailings.block.DrillMenus;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

/** The Fabric entry point. Wildcatter is a hard dependency and a compile dependency. */
public final class TailingsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Tailings.register(Wildcatter.MOD_ID);
		MenuType<DrillMenu> menu = new ExtendedMenuType<>((id, inventory, pos) -> new DrillMenu(id, inventory, pos), BlockPos.STREAM_CODEC);
		DrillMenus.DRILL = Registry.register(BuiltInRegistries.MENU, Tailings.id("tunnel_drill"), menu);
		DrillMenus.SORTER = Registry.register(BuiltInRegistries.MENU, Tailings.id("ore_sorter"),
				new MenuType<>(dev.wildcatter.tailings.block.SorterMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Tailings.id("tailings"), FabricCreativeModeTab.builder()
				.title(Tailings.tabTitle())
				.icon(Tailings::tabIcon)
				.displayItems((parameters, output) -> Tailings.creativeTab(output::accept))
				.build());
	}
}
