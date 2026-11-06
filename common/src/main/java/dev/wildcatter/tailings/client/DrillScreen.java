package dev.wildcatter.tailings.client;

import dev.wildcatter.tailings.block.DrillMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** The cab. Buttons send {@link DrillMenu#clickMenuButton} ids. The readout is the synced program. */
public class DrillScreen extends AbstractContainerScreen<DrillMenu> {
	public DrillScreen(DrillMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 234);
	}

	@Override
	protected void init() {
		super.init();
		int x = leftPos + 8;
		int y = topPos + 76;
		add(x, y, 22, "Y-", 0);
		add(x + 24, y, 22, "Y+", 1);
		add(x + 52, y, 24, "L-", 3);
		add(x + 78, y, 24, "L+", 4);
		add(x + 112, y, 32, "Ride", dev.wildcatter.tailings.block.TbmCore.BUTTON_RIDE);
		add(x + 148, y, 20, "Go", 10);
		int y2 = topPos + 58;
		add(x, y2, 28, "ore", 6);
		add(x + 30, y2, 36, "cable", 7);
		add(x + 68, y2, 36, "liq", 8);
		add(x + 106, y2, 28, "out", 9);
	}

	private void add(int x, int y, int width, String label, int id) {
		addRenderableWidget(Button.builder(Component.literal(label), button -> {
			if (minecraft != null && minecraft.gameMode != null) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
			}
		}).bounds(x, y, width, 16).build());
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
		super.extractContents(graphics, mouseX, mouseY, partial);
		int line = topPos + 18;
		for (String row : menu.readout().split("\n")) {
			graphics.text(font, Component.literal(row), leftPos + 8, line, 0xFF404040);
			line += 10;
		}
	}
}
