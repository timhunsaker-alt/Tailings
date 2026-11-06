package dev.wildcatter.tailings.client;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.DrillMenu;
import dev.wildcatter.tailings.block.TbmCore;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** The cab. Buttons send {@link DrillMenu#clickMenuButton} ids. The readout is the synced program. */
public class DrillScreen extends AbstractContainerScreen<DrillMenu> {
	private static final int TEXT_WIDTH = 160;
	private final List<Toggle> toggles = new ArrayList<>();

	private record Toggle(Button button, String label, String help, int bit) {
	}

	public DrillScreen(DrillMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 234);
		// the readout and the slots fill the panel; the hotbar and inventory explain themselves
		inventoryLabelY = -10000;
		titleLabelY = 5;
	}

	@Override
	protected void init() {
		super.init();
		int x = leftPos + 8;
		int y = topPos + 79;
		toggles.clear();
		add(x, y, 22, "Y-", 0);
		add(x + 24, y, 22, "Y+", 1);
		add(x + 52, y, 24, "L-", 3);
		add(x + 78, y, 24, "L+", 4);
		add(x + 112, y, 32, "Ride", dev.wildcatter.tailings.block.TbmCore.BUTTON_RIDE);
		add(x + 148, y, 20, "Go", 10);
		int y2 = topPos + 62;
		toggle(x, y2, 28, "ore", "Stop when the probe finds ore", 6, 0);
		toggle(x + 30, y2, 36, "cable", "Stop at the end of the cable", 7, 1);
		toggle(x + 68, y2, 36, "liq", "Stop at a liquid it cannot handle", 8, 2);
		toggle(x + 106, y2, 28, "out", "Stop when a stock slot is empty", 9, 3);
	}

	private void toggle(int x, int y, int width, String label, String help, int id, int bit) {
		toggles.add(new Toggle(add(x, y, width, label, id), label, help, bit));
	}

	private Button add(int x, int y, int width, String label, int id) {
		return addRenderableWidget(Button.builder(Component.literal(label), button -> {
			if (minecraft != null && minecraft.gameMode != null) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
			}
		}).bounds(x, y, width, 16).build());
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
		for (Slot slot : menu.slots) {
			slotBack(graphics, leftPos + slot.x, topPos + slot.y);
		}
		for (Toggle toggle : toggles) {
			boolean on = menu.on(toggle.bit());
			toggle.button().setMessage(on ? Component.literal(toggle.label()).withStyle(ChatFormatting.GREEN)
					: Component.literal(toggle.label()).withStyle(ChatFormatting.RED, ChatFormatting.STRIKETHROUGH));
			toggle.button().setTooltip(Tooltip.create(Component.literal(toggle.help() + (on ? ": on" : ": off"))));
		}
		super.extractContents(graphics, mouseX, mouseY, partial);
		for (int i = 0; i < DrillBlockEntity.INPUTS; i++) {
			Slot slot = menu.slots.get(i);
			if (!slot.hasItem()) {
				graphics.fakeItem(ghost(i), leftPos + slot.x, topPos + slot.y);
				graphics.fill(leftPos + slot.x, topPos + slot.y, leftPos + slot.x + 16, topPos + slot.y + 16, 0xA08B8B8B);
			}
		}
		int line = topPos + 15;
		for (String row : menu.readout().split("\n")) {
			for (FormattedCharSequence wrapped : font.split(Component.literal(row), TEXT_WIDTH)) {
				graphics.text(font, wrapped, leftPos + 8, line, 0xFF404040, false);
				line += 9;
			}
		}
	}

	private static void slotBack(GuiGraphicsExtractor graphics, int x, int y) {
		graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF373737);
		graphics.fill(x, y, x + 17, y + 17, 0xFFFFFFFF);
		graphics.fill(x, y, x + 16, y + 16, 0xFF8B8B8B);
	}

	/** What an empty stock slot takes. */
	private static ItemStack ghost(int slot) {
		if (slot == DrillBlockEntity.SLOT_RAIL) {
			return new ItemStack(TailingsBlocks.MINE_RAIL);
		}
		if (slot == DrillBlockEntity.SLOT_GRAVEL) {
			return new ItemStack(Items.GRAVEL);
		}
		if (slot == DrillBlockEntity.SLOT_COBBLE) {
			return new ItemStack(Items.COBBLESTONE);
		}
		if (slot == DrillBlockEntity.SLOT_SEGMENTS) {
			return new ItemStack(TailingsBlocks.LINING);
		}
		if (slot == DrillBlockEntity.SLOT_WATER) {
			return new ItemStack(Items.WATER_BUCKET);
		}
		return new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(TbmCore.CABLE_ID)));
	}
}
