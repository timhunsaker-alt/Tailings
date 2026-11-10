package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.pit.Blasting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A small settings screen for the blasting machines (open-pit-p3-blasting.md): the drill's pattern, the emulsion truck's stemming and
 * the logger's delays; and the pit switch house's outlets (P5a). Each setting has a minus and a plus button; each panel a few action buttons; and a status line. Everything goes
 * over a menu's data slots and button clicks, so it needs no packets of its own.
 */
public class PitPanelMenu extends AbstractContainerMenu {
	/** Set by each loader while registries are open. */
	public static @Nullable MenuType<PitPanelMenu> TYPE;
	public static final int SETTINGS = 8;
	public static final int STATUS_CHARS = 160;
	/** A setting's minus button is 2 i, its plus 2 i + 1; an action's button is ACTION + a. */
	public static final int ACTION = 50;

	/** One panel's settings and actions. */
	public enum Kind {
		DRILL("Blasthole Drill", new Setting[] {
				new Setting("Rows", 1, Blasting.ROWS_MAX, 1, 3),
				new Setting("Columns", 1, Blasting.COLUMNS_MAX, 1, 3),
				new Setting("Burden m", Blasting.BURDEN_MIN, Blasting.BURDEN_MAX, 1, Blasting.BURDEN),
				new Setting("Spacing m", Blasting.SPACING_MIN, Blasting.SPACING_MAX, 1, Blasting.SPACING),
				new Setting("Bench m", Blasting.BENCH_MIN, Blasting.BENCH_MAX, 1, Blasting.BENCH),
				new Setting("Subdrill m", Blasting.SUBDRILL_MIN, Blasting.SUBDRILL_MAX, 1, Blasting.SUBDRILL),
				new Setting("Staggered", 0, 1, 1, 1),
				new Setting("Electric", 0, 1, 1, 0)}, new String[] {"GO", "STOP", "Drill one"}),
		TRUCK("Emulsion Truck", new Setting[] {
				new Setting("Stemming m", Blasting.STEMMING_MIN, Blasting.STEMMING_MAX, 1, Blasting.STEMMING)}, new String[] {"Charge all in reach", "Stop"}),
		LOGGER("Blast Logger", new Setting[] {
				new Setting("Row delay ms", 0, 500, 1, Blasting.ROW_DELAY_MS),
				new Setting("Hole delay ms", 0, 200, 1, Blasting.HOLE_DELAY_MS)}, new String[] {"Clear"}),
		/** The pit switch house (P5a): each outlet open (0) or closed (1), and a reset for trips. */
		SWITCH_HOUSE("Pit Switch House", new Setting[] {
				new Setting("Outlet 1", 0, 1, 1, 0),
				new Setting("Outlet 2", 0, 1, 1, 0),
				new Setting("Outlet 3", 0, 1, 1, 0),
				new Setting("Outlet 4", 0, 1, 1, 0)}, new String[] {"Reset trips"}),
		/** The hydraulic shovel (P5c): diesel (0) or its electric variant on a trailing cable (1); AUTO here. */
		HYD_SHOVEL("Hydraulic Shovel", new Setting[] {
				new Setting("Electric", 0, 1, 1, 0)}, new String[] {"AUTO on / off"}),
		/** The rope shovel (P5d): RUN lifts it off its parts and starts it, STOP sets it down; AUTO loads the trucks that come. */
		ROPE_SHOVEL("Rope Shovel", new Setting[0], new String[] {"RUN", "STOP", "AUTO on / off"}),
		/**
		 * The walking dragline (dragline.md, P6): its job, the strip's width and length, the seam's depth under its tub and the side it
		 * casts to (0 left, 1 right of its walk); RUN lifts it off its parts or starts it again, STOP stops it and sets it down if it can.
		 */
		DRAGLINE("Dragline", new Setting[] {
				new Setting("Strip width m", dev.wildcatter.tailings.pit.Dragline.WIDTH_MIN, dev.wildcatter.tailings.pit.Dragline.WIDTH_MAX, 5, dev.wildcatter.tailings.pit.Dragline.WIDTH),
				new Setting("Strip length m", dev.wildcatter.tailings.pit.Dragline.LENGTH_MIN, dev.wildcatter.tailings.pit.Dragline.LENGTH_MAX, 10, dev.wildcatter.tailings.pit.Dragline.LENGTH),
				new Setting("Seam depth m", dev.wildcatter.tailings.pit.Dragline.SEAM_MIN, dev.wildcatter.tailings.pit.Dragline.SEAM_MAX, 1, dev.wildcatter.tailings.pit.Dragline.SEAM),
				new Setting("Cast right", 0, 1, 1, 1)}, new String[] {"RUN", "STOP"});

		private final String title;
		private final Setting[] settings;
		private final String[] actions;

		Kind(String title, Setting[] settings, String[] actions) {
			this.title = title;
			this.settings = settings;
			this.actions = actions;
		}

		public String title() {
			return title;
		}

		public Setting[] settings() {
			return settings;
		}

		public String[] actions() {
			return actions;
		}
	}

	public record Setting(String label, int min, int max, int step, int initial) {
		public int clamp(int v) {
			return Math.max(min, Math.min(max, v));
		}
	}

	/** What a panel works on: a drill, a truck, a logger in a player's hand. */
	public interface Panel {
		Kind kind();

		int setting(int i);

		void set(int i, int value, Player player);

		void action(int a, Player player);

		String status();

		boolean stillValid(Player player);
	}

	private final ContainerData data;
	private final @Nullable Panel panel;

	/** The client's copy. */
	public PitPanelMenu(int id, Inventory inventory) {
		this(id, inventory, null);
	}

	public PitPanelMenu(int id, Inventory inventory, @Nullable Panel panel) {
		super(TYPE, id);
		this.panel = panel;
		this.data = panel == null ? new SimpleContainerData(1 + SETTINGS + STATUS_CHARS) : new Live(panel);
		addDataSlots(data);
	}

	public Kind kind() {
		return Kind.values()[Math.floorMod(data.get(0), Kind.values().length)];
	}

	public int setting(int i) {
		return data.get(1 + i);
	}

	public String status() {
		StringBuilder line = new StringBuilder();
		for (int i = 0; i < STATUS_CHARS && data.get(1 + SETTINGS + i) != 0; i++) {
			line.append((char) data.get(1 + SETTINGS + i));
		}
		return line.toString();
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (panel == null) {
			return true;
		}
		if (id >= ACTION) {
			if (id - ACTION < panel.kind().actions().length) {
				panel.action(id - ACTION, player);
			}
			return true;
		}
		int i = id / 2;
		Setting[] settings = panel.kind().settings();
		if (i < settings.length) {
			Setting s = settings[i];
			panel.set(i, s.clamp(panel.setting(i) + (id % 2 == 0 ? -s.step() : s.step())), player);
		}
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return panel != null && panel.stillValid(player);
	}

	private static final class Live implements ContainerData {
		private final Panel panel;

		private Live(Panel panel) {
			this.panel = panel;
		}

		@Override
		public int get(int index) {
			if (index == 0) {
				return panel.kind().ordinal();
			}
			if (index <= SETTINGS) {
				return index - 1 < panel.kind().settings().length ? panel.setting(index - 1) : 0;
			}
			String line = panel.status();
			int at = index - 1 - SETTINGS;
			return at < line.length() ? line.charAt(at) : 0;
		}

		@Override
		public void set(int index, int value) {
		}

		@Override
		public int getCount() {
			return 1 + SETTINGS + STATUS_CHARS;
		}
	}
}
