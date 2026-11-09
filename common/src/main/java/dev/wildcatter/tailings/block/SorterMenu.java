package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.haul.Sorter;
import dev.wildcatter.tailings.plant.Machine;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The ore sorter's screen (dump-station.md): its four sides, each showing the rock or ore type it takes, and which side takes the
 * waste. A side is set by putting a rock or ore item on its slot (the item stays with the player: the slot shows its type, it does not
 * hold it) and cleared with an empty hand; a button makes a side the waste side. Types with no side go out of the waste side.
 */
public class SorterMenu extends AbstractContainerMenu {
	/** Button ids: 0-3 make that side (north, east, south, west) the waste side; 4-7 clear it. */
	public static final int WASTE = 0;
	public static final int CLEAR = 4;
	/** The slots' places on the panel, north, east, south, west, round the sorter in the middle. */
	public static final int[][] AT = {{80, 28}, {116, 52}, {80, 76}, {44, 52}};

	private final Container shown;
	private final ContainerData data;
	private final @Nullable PlantBlockEntity sorter;

	/** The client's copy: the server sends it the slots and the waste side. */
	public SorterMenu(int id, Inventory player) {
		this(id, player, null);
	}

	/** The server's, on the sorter. */
	public SorterMenu(int id, Inventory player, @Nullable PlantBlockEntity sorter) {
		super(DrillMenus.SORTER, id);
		this.sorter = sorter;
		this.shown = new SimpleContainer(Sorter.SIDES);
		this.data = sorter == null ? new SimpleContainerData(1) : new ContainerData() {
			@Override
			public int get(int index) {
				return sorter.sorter().waste() + 1;
			}

			@Override
			public void set(int index, int value) {
			}

			@Override
			public int getCount() {
				return 1;
			}
		};
		for (int side = 0; side < Sorter.SIDES; side++) {
			addSlot(new Shown(shown, side, AT[side][0], AT[side][1]));
		}
		addStandardInventorySlots(player, 8, 116);
		addDataSlots(data);
		show();
	}

	/** The side set as waste, 0 north to 3 west, or -1. */
	public int waste() {
		return data.get(0) - 1;
	}

	/** Each side's slot shows the type it takes. */
	private void show() {
		if (sorter == null) {
			return;
		}
		for (int side = 0; side < Sorter.SIDES; side++) {
			String id = sorter.sorter().product(side);
			ItemStack want = id == null ? ItemStack.EMPTY : Feeds.stack(id, 1);
			if (!ItemStack.isSameItemSameComponents(shown.getItem(side), want)) {
				shown.setItem(side, want);
			}
		}
	}

	@Override
	public void broadcastChanges() {
		show();
		super.broadcastChanges();
	}

	/** A click on a side's slot sets it to the type in hand, or clears it with an empty hand; the item stays in hand. */
	@Override
	public void clicked(int slotIndex, int buttonNum, ContainerInput input, Player player) {
		if (slotIndex < 0 || slotIndex >= Sorter.SIDES) {
			super.clicked(slotIndex, buttonNum, input, player);
			return;
		}
		if (sorter == null) {
			return;
		}
		ItemStack carried = getCarried();
		if (carried.isEmpty()) {
			sorter.clearSide(slotIndex);
		} else if (Feeds.parse(carried) != null) {
			sorter.setSide(slotIndex, BuiltInRegistries.ITEM.getKey(carried.getItem()).toString(), carried.getHoverName().getString());
		}
		show();
	}

	@Override
	public boolean clickMenuButton(Player player, int id) {
		if (sorter == null || id < 0 || id >= CLEAR + Sorter.SIDES) {
			return false;
		}
		if (id < CLEAR) {
			sorter.setWaste(id - WASTE);
		} else {
			sorter.clearSide(id - CLEAR);
		}
		show();
		return true;
	}

	/** A shift-click on a rock in the inventory sets the first side with nothing set to it. */
	@Override
	public ItemStack quickMoveStack(Player player, int index) {
		if (sorter == null || index < Sorter.SIDES || index >= slots.size()) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = slots.get(index).getItem();
		if (Feeds.parse(stack) == null) {
			return ItemStack.EMPTY;
		}
		for (int side = 0; side < Sorter.SIDES; side++) {
			if (sorter.sorter().product(side) == null && sorter.sorter().waste() != side) {
				sorter.setSide(side, BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getHoverName().getString());
				show();
				break;
			}
		}
		return ItemStack.EMPTY;
	}

	@Override
	public boolean stillValid(Player player) {
		return sorter != null && !sorter.isRemoved() && sorter.machine() == Machine.ORE_SORTER
				&& player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(sorter.getBlockPos())) < 10 * 10;
	}

	/** A slot that shows a type and holds nothing a player can take. */
	private static final class Shown extends Slot {
		private Shown(Container container, int index, int x, int y) {
			super(container, index, x, y);
		}

		@Override
		public boolean mayPlace(ItemStack stack) {
			return false;
		}

		@Override
		public boolean mayPickup(Player player) {
			return false;
		}
	}
}
