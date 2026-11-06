package dev.wildcatter.tailings.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/** The operator cab, wherever the machine is: the main beam's block entity while it stands as blocks, the contraption while it runs. Its slots are the {@link TbmCore}'s. */
public interface TbmCab extends Container {
	TbmCore core();

	/** A screen button. */
	void press(int button, Player player);

	BlockPos menuPos();

	@Override
	default int getContainerSize() {
		return TbmCore.SLOTS;
	}

	@Override
	default boolean isEmpty() {
		return core().slots().stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	default ItemStack getItem(int slot) {
		return core().slots().get(slot);
	}

	@Override
	default ItemStack removeItem(int slot, int count) {
		ItemStack out = ContainerHelper.removeItem(core().slots(), slot, count);
		setChanged();
		return out;
	}

	@Override
	default ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(core().slots(), slot);
	}

	@Override
	default void setItem(int slot, ItemStack stack) {
		core().slots().set(slot, stack);
		setChanged();
	}

	@Override
	default boolean canPlaceItem(int slot, ItemStack stack) {
		return TbmCore.canPlace(slot, stack);
	}

	@Override
	default void clearContent() {
		core().slots().clear();
	}
}
