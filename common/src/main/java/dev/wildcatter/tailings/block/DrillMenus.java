package dev.wildcatter.tailings.block;

import net.minecraft.world.inventory.MenuType;

/** Set by each loader while registries are open. The cab screen looks this up. */
public final class DrillMenus {
	public static MenuType<DrillMenu> DRILL;

	private DrillMenus() {
	}
}
