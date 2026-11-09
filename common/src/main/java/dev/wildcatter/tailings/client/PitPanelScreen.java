package dev.wildcatter.tailings.client;

import dev.wildcatter.tailings.block.PitPanelMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;

/** The settings screen of a drill, an emulsion truck or a logger: each setting with minus and plus, the actions, the status. */
public class PitPanelScreen extends AbstractContainerScreen<PitPanelMenu> {
	private static final int ROW = 18;
	private final List<Button> made = new ArrayList<>();
	private PitPanelMenu.Kind built;

	public PitPanelScreen(PitPanelMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 196, 200);
		inventoryLabelY = -10000;
		titleLabelY = 6;
	}

	@Override
	protected void init() {
		super.init();
		build();
	}

	/** The buttons follow the panel's kind, which comes with the first data the server sends. */
	private void build() {
		made.forEach(this::removeWidget);
		made.clear();
		built = menu.kind();
		PitPanelMenu.Setting[] settings = built.settings();
		int y = topPos + 20;
		for (int i = 0; i < settings.length; i++) {
			add(leftPos + 120, y, 20, "-", 2 * i);
			add(leftPos + 144, y, 20, "+", 2 * i + 1);
			y += ROW;
		}
		int x = leftPos + 8;
		for (int a = 0; a < built.actions().length; a++) {
			int w = font.width(built.actions()[a]) + 10;
			if (x + w > leftPos + imageWidth - 8) {
				x = leftPos + 8;
				y += ROW;
			}
			add(x, y, w, built.actions()[a], PitPanelMenu.ACTION + a);
			x += w + 4;
		}
	}

	private void add(int x, int y, int width, String label, int id) {
		made.add(addRenderableWidget(Button.builder(Component.literal(label), button -> {
			if (minecraft != null && minecraft.gameMode != null) {
				minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
			}
		}).bounds(x, y, width, 16).build()));
	}

	@Override
	public void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partial) {
		if (built != menu.kind()) {
			build();
		}
		graphics.fill(leftPos, topPos, leftPos + imageWidth, topPos + imageHeight, 0xFFC6C6C6);
		super.extractContents(graphics, mouseX, mouseY, partial);
		graphics.text(font, Component.literal(built.title()), leftPos + 8, topPos + 6, 0xFF404040, false);
		PitPanelMenu.Setting[] settings = built.settings();
		int y = topPos + 24;
		for (int i = 0; i < settings.length; i++) {
			int v = menu.setting(i);
			String value = settings[i].max() == 1 && settings[i].min() == 0 ? (v == 1 ? "yes" : "no") : Integer.toString(v);
			graphics.text(font, Component.literal(settings[i].label() + ": " + value), leftPos + 8, y, 0xFF404040, false);
			y += ROW;
		}
		int line = topPos + 24 + settings.length * ROW + ROW + 4;
		for (FormattedCharSequence wrapped : font.split(Component.literal(menu.status()), imageWidth - 16)) {
			graphics.text(font, wrapped, leftPos + 8, line, 0xFF202020, false);
			line += 9;
		}
	}
}
