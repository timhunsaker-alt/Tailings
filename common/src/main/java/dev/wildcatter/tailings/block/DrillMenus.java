package dev.wildcatter.tailings.block;

import net.minecraft.world.inventory.MenuType;

/** Set by each loader while registries are open. The cab screen and the ore sorter's screen look these up. */
public final class DrillMenus {
	public static MenuType<DrillMenu> DRILL;
	public static MenuType<SorterMenu> SORTER;

	private DrillMenus() {
	}
}
