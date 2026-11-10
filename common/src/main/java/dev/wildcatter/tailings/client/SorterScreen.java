package dev.wildcatter.tailings.client;

import dev.wildcatter.tailings.block.SorterMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The ore sorter's screen: its four sides round the sorter, each slot showing the type that side takes. Put a rock or ore item on a
 * slot to set the side, click it with an empty hand to clear it; a side's Waste button sends every type with no side out of it.
 */
public class SorterScreen extends AbstractContainerScreen<SorterMenu> {
	private static final String[] NAMES = {"North", "East", "South", "West"};
	private final List<Button> waste = new ArrayList<>();

	public SorterScreen(SorterMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 198);
		inventoryLabelY = 105;
		titleLabelY = 5;
	}

	@Override
	protected void init() {
		super.init();
		waste.clear();
		for (int side = 0; side < NAMES.length; side++) {
			int id = SorterMenu.WASTE + side;
			int x = leftPos + SorterMenu.AT[side][0] + (side == 3 ? -34 : 20);
			int y = topPos + SorterMenu.AT[side][1] + 2;
			waste.add(addRenderableWidget(Button.builder(Component.literal("W"), button -> press(id)).bounds(x, y, 14, 12).build()));
		}
	}

	private void press(int id) {
		if (minecraft != null && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
		}
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
		// the sorter in the middle, its sides round it
		int cx = leftPos + 80, cy = topPos + 52;
		graphics.fill(cx - 8, cy - 4, cx + 24, cy + 20, 0xFF5A6470);
		for (Slot slot : menu.slots) {
			graphics.fill(leftPos + slot.x - 1, topPos + slot.y - 1, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFF373737);
			graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 17, topPos + slot.y + 17, 0xFFFFFFFF);
			graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xFF8B8B8B);
		}
		for (int side = 0; side < waste.size(); side++) {
			boolean on = menu.waste() == side;
			waste.get(side).setMessage(on ? Component.literal("W").withStyle(ChatFormatting.GOLD) : Component.literal("W"));
			waste.get(side).setTooltip(Tooltip.create(Component.literal(on ? NAMES[side] + " takes the waste: every type with no side of its own"
					: "Make the " + NAMES[side].toLowerCase(java.util.Locale.ROOT) + " side the waste side")));
		}
		super.extractContents(graphics, mouseX, mouseY, partial);
		for (int side = 0; side < NAMES.length; side++) {
			Slot slot = menu.slots.get(side);
			String label = menu.waste() == side ? NAMES[side] + ": waste" : NAMES[side];
			int x = leftPos + slot.x + 8 - font.width(label) / 2;
			int y = side == 2 ? topPos + slot.y + 18 : topPos + slot.y - 10;
			graphics.text(font, Component.literal(label).getVisualOrderText(), x, y, menu.waste() == side ? 0xFF8A5A00 : 0xFF404040, false);
		}
	}
}
