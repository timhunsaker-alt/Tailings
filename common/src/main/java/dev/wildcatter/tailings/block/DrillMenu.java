package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.mine.DrillCodes;
import dev.wildcatter.tailings.mine.Heading;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** The cab screen: six stock slots (rail, gravel, cobble, lining, water, cable), the muck buffer, and the program buttons. */
public class DrillMenu extends AbstractContainerMenu {
	private static final int NUMBERS = 13;
	private static final int STATUS_CHARS = 96;
	private final Container container;
	private final ContainerData data;
	/** The cab it shows: the main beam's block entity, or the running machine. It follows the machine as it lifts and sets down. */
	private @Nullable TbmCab drill;

	/** The client's copy: the server sends it the slots and data. */
	public DrillMenu(int id, Inventory player, BlockPos pos) {
		this(id, player, (TbmCab) null);
	}

	/** The server's, on the cab, wherever the machine is. */
	public DrillMenu(int id, Inventory player, @Nullable TbmCab drill) {
		super(DrillMenus.DRILL, id);
		this.drill = drill;
		this.container = drill == null ? new SimpleContainer(DrillBlockEntity.SLOTS) : new Follow();
		this.data = drill == null ? new SimpleContainerData(NUMBERS + STATUS_CHARS) : new Live(() -> this.drill.core());
		for (int i = 0; i < DrillBlockEntity.INPUTS; i++) {
			addSlot(new StockSlot(container, i, 8 + i * 18, 96));
		}
		// the Ride button's seat comes with the contraption (R6b)
		for (int i = 0; i < DrillBlockEntity.MUCK_SLOTS; i++) {
			addSlot(new Slot(container, DrillBlockEntity.INPUTS + i, 8 + (i % 9) * 18, 116 + (i / 9) * 18) {
				@Override
				public boolean mayPlace(ItemStack stack) {
					return false;
				}
			});
		}
		addStandardInventorySlots(player, 8, 158);
		addDataSlots(data);
	}

	public String readout() {
		Heading heading = Heading.values()[Math.floorMod(data.get(1), Heading.values().length)];
		return "Y " + data.get(0) + "  " + heading.name().toLowerCase(java.util.Locale.ROOT) + "  flat " + data.get(2) + "  " + (on(4) ? "running" : "stopped")
				+ "\n" + statusLine()
				+ "\n" + data.get(5) + " blocks" + (data.get(5) > 0 ? ", floor Y " + data.get(6) : "") + ", cable " + data.get(7)
				+ "\npumped " + data.get(9) + " m3, quench " + data.get(10)
				+ "\n" + airLine();
	}

	/** The air at the back-up, in tenths of m3/s (-1 before the first reading), and the cassette's duct (ventilation.md). */
	private String airLine() {
		int tenths = (short) data.get(3);
		double flow = tenths / 10.0;
		double velocity = flow / dev.wildcatter.tailings.mine.Airflow.BORE_AREA;
		String reading = tenths < 0 ? "air: no reading" : String.format(java.util.Locale.ROOT, "air %.2f/%.2f m/s%s", velocity, dev.wildcatter.tailings.mine.Airflow.NEED_VELOCITY,
				flow + 1e-9 < dev.wildcatter.tailings.mine.Airflow.NEED ? " POOR" : "");
		return reading + ", duct " + data.get(12) + " m";
	}

	/** The status as the server words it, sent a character to a data slot, since that is what a menu syncs. */
	private String statusLine() {
		StringBuilder line = new StringBuilder();
		for (int i = 0; i < STATUS_CHARS && data.get(NUMBERS + i) != 0; i++) {
			line.append((char) data.get(NUMBERS + i));
		}
		return line.isEmpty() ? DrillCodes.phrase(data.get(8), data.get(11)) : line.toString();
	}

	/** One of the program's flags: 0 ore, 1 cable, 2 liquid, 3 empty, 4 running. */
	public boolean on(int bit) {
		return (data.get(4) & (1 << bit)) != 0;
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (drill != null) {
			drill.press(id, player);
		}
		return true;
	}

	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		Slot slot = slots.get(index);
		if (slot == null || !slot.hasItem()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slot.getItem();
		ItemStack copy = stack.copy();
		if (index < DrillBlockEntity.SLOTS) {
			if (!moveItemStackTo(stack, DrillBlockEntity.SLOTS, slots.size(), true)) {
				return ItemStack.EMPTY;
			}
		} else {
			int into = inputFor(stack);
			if (into < 0 || !moveItemStackTo(stack, into, into + 1, false)) {
				return ItemStack.EMPTY;
			}
		}
		if (stack.isEmpty()) {
			slot.set(ItemStack.EMPTY);
		} else {
			slot.setChanged();
		}
		return copy;
	}

	private static int inputFor(ItemStack stack) {
		if (stack.getItem() == TailingsBlocks.MINE_RAIL.asItem()) {
			return DrillBlockEntity.SLOT_RAIL;
		}
		if (stack.getItem() == Items.GRAVEL) {
			return DrillBlockEntity.SLOT_GRAVEL;
		}
		if (stack.getItem() == Items.COBBLESTONE) {
			return DrillBlockEntity.SLOT_COBBLE;
		}
		if (stack.getItem() == TailingsBlocks.LINING.asItem()) {
			return DrillBlockEntity.SLOT_SEGMENTS;
		}
		if (stack.getItem() == Items.WATER_BUCKET) {
			return DrillBlockEntity.SLOT_WATER;
		}
		if (TbmCore.CABLE_ID.equals(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())) {
			return DrillBlockEntity.SLOT_CABLE;
		}
		return -1;
	}

	@Override
	public boolean stillValid(Player player) {
		return drill != null && drill.stillValid(player);
	}

	/**
	 * The machine lifted into its contraption, or set down as blocks again: every screen open on {@code from} now shows {@code to},
	 * so the cab's screen stays open across the change.
	 */
	public static void rebind(net.minecraft.server.level.ServerLevel level, TbmCab from, TbmCab to) {
		for (net.minecraft.server.level.ServerPlayer player : level.players()) {
			if (player.containerMenu instanceof DrillMenu menu && menu.drill == from) {
				menu.drill = to;
			}
		}
	}

	/** The slots of whichever cab the screen shows now. */
	private final class Follow implements Container {
		private TbmCab cab() {
			return java.util.Objects.requireNonNull(drill);
		}

		@Override
		public int getContainerSize() {
			return cab().getContainerSize();
		}

		@Override
		public boolean isEmpty() {
			return cab().isEmpty();
		}

		@Override
		public ItemStack getItem(int slot) {
			return cab().getItem(slot);
		}

		@Override
		public ItemStack removeItem(int slot, int count) {
			return cab().removeItem(slot, count);
		}

		@Override
		public ItemStack removeItemNoUpdate(int slot) {
			return cab().removeItemNoUpdate(slot);
		}

		@Override
		public void setItem(int slot, ItemStack stack) {
			cab().setItem(slot, stack);
		}

		@Override
		public void setChanged() {
			cab().setChanged();
		}

		@Override
		public boolean stillValid(Player player) {
			return cab().stillValid(player);
		}

		@Override
		public boolean canPlaceItem(int slot, ItemStack stack) {
			return cab().canPlaceItem(slot, stack);
		}

		@Override
		public void clearContent() {
			cab().clearContent();
		}
	}

	/** The cab it shows now, for tests. */
	public @Nullable TbmCab cab() {
		return drill;
	}

	private static final class StockSlot extends Slot {
		private StockSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return TbmCore.canPlace(index, stack);
		}
	}

	/** Server-side view of the cab. The client keeps a {@link SimpleContainerData} copy. */
	private static final class Live implements ContainerData {
		private final java.util.function.Supplier<TbmCore> core;
		private TbmCore drill;

		private Live(java.util.function.Supplier<TbmCore> core) {
			this.core = core;
		}

		@Override
		public int get(int index) {
			drill = core.get();
			return switch (index) {
				case 0 -> drill.program().targetY();
				case 1 -> drill.program().heading().ordinal();
				case 2 -> drill.program().flatLength();
				case 3 -> drill.air() < 0 ? -1 : (int) Math.min(Short.MAX_VALUE, Math.round(drill.air() * 10));
				case 4 -> flags();
				case 5 -> drill.drill().advanceCount();
				case 6 -> drill.drill().floorY();
				case 7 -> drill.cableLeft();
				case 8 -> drill.status();
				case 9 -> drill.drill().pumped();
				case 10 -> drill.tank();
				case 11 -> drill.lack();
				case 12 -> drill.ductLeft();
				default -> {
					String line = drill.statusLine();
					int at = index - NUMBERS;
					yield at >= 0 && at < line.length() ? line.charAt(at) : 0;
				}
			};
		}

		private int flags() {
			int flags = 0;
			if (drill.program().stopOnOre()) {
				flags |= 1;
			}
			if (drill.program().stopOnCable()) {
				flags |= 2;
			}
			if (drill.program().stopOnLiquid()) {
				flags |= 4;
			}
			if (drill.program().stopOnEmpty()) {
				flags |= 8;
			}
			if (drill.running()) {
				flags |= 16;
			}
			return flags;
		}

		@Override
		public void set(int index, int value) {
		}

		@Override
		public int getCount() {
			return NUMBERS + STATUS_CHARS;
		}
	}
}
