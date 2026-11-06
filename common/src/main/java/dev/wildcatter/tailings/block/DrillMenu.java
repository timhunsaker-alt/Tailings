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
	private final Container container;
	private final ContainerData data;
	private final @Nullable DrillBlockEntity drill;

	public DrillMenu(int id, Inventory player, BlockPos pos) {
		this(id, player, find(player, pos));
	}

	private DrillMenu(int id, Inventory player, @Nullable DrillBlockEntity drill) {
		super(DrillMenus.DRILL, id);
		this.drill = drill;
		this.container = drill == null ? new SimpleContainer(DrillBlockEntity.SLOTS) : drill;
		boolean client = player.player.level().isClientSide();
		this.data = client || drill == null ? new SimpleContainerData(12) : new Live(drill);
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

	private static @Nullable DrillBlockEntity find(Inventory player, BlockPos pos) {
		if (pos != null && player.player.level().getBlockEntity(pos) instanceof DrillBlockEntity drill) {
			return drill;
		}
		return null;
	}

	public String readout() {
		Heading heading = Heading.values()[Math.floorMod(data.get(1), Heading.values().length)];
		int flags = data.get(4);
		return "Y " + data.get(0) + "  " + heading.name().toLowerCase(java.util.Locale.ROOT) + "  flat " + data.get(2) + "  1 in 2"
				+ "\n" + DrillCodes.phrase(data.get(8), data.get(11))
				+ "\n" + data.get(5) + " blocks, floor Y " + data.get(6) + ", cable " + data.get(7)
				+ ", pumped " + data.get(9) + " m3, quench " + data.get(10)
				+ "\nore " + bit(flags, 0) + "  cable " + bit(flags, 1) + "  liquid " + bit(flags, 2) + "  empty " + bit(flags, 3)
				+ (bit(flags, 4).equals("on") ? "  running" : "  stopped");
	}

	private static String bit(int flags, int bit) {
		return (flags & (1 << bit)) != 0 ? "on" : "off";
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (drill != null) {
			drill.press(id);
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

	private static final class StockSlot extends Slot {
		private StockSlot(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return container instanceof DrillBlockEntity drill ? drill.canPlaceItem(index, stack) : true;
		}
	}

	/** Server-side view of the cab. The client keeps a {@link SimpleContainerData} copy. */
	private static final class Live implements ContainerData {
		private final DrillBlockEntity drill;

		private Live(DrillBlockEntity drill) {
			this.drill = drill;
		}

		@Override
		public int get(int index) {
			return switch (index) {
				case 0 -> drill.program().targetY();
				case 1 -> drill.program().heading().ordinal();
				case 2 -> drill.program().flatLength();
				case 3 -> 0;
				case 4 -> flags();
				case 5 -> drill.drill().advanceCount();
				case 6 -> drill.drill().floorY();
				case 7 -> drill.core().cableLeft();
				case 8 -> drill.status();
				case 9 -> drill.drill().pumped();
				case 10 -> drill.tank();
				case 11 -> drill.lack();
				default -> 0;
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
			return 12;
		}
	}
}
