package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.Feeds.Charge;
import dev.wildcatter.tailings.block.HandMillBlock.Role;
import dev.wildcatter.tailings.rock.Milling;
import dev.wildcatter.tailings.rock.NuggetBank;
import dev.wildcatter.tailings.rock.Separator;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.util.Prediction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * One input slot on top and output slots a hopper can pull from the other faces. A hand crank, so it draws no power.
 * One item every two seconds.
 */
public class HandMillBlockEntity extends BlockEntity implements WorldlyContainer {
	private static final int PERIOD = 40;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(9, ItemStack.EMPTY);
	private final NuggetBank bank = new NuggetBank();
	private int wait;

	public HandMillBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.MILL, pos, state);
	}

	public Role role() {
		return ((HandMillBlock) getBlockState().getBlock()).role();
	}

	public static void tick(Level level, BlockPos pos, BlockState state, HandMillBlockEntity mill) {
		mill.serverTick();
	}

	private void serverTick() {
		if (wait < PERIOD) {
			wait++;
			return;
		}
		if (tryRun()) {
			wait = 0;
		}
	}

	/** Puts one of {@code stack} into the input, if this machine takes it. */
	public boolean offer(ItemStack stack) {
		if (!takes(stack) || !slots.get(0).isEmpty()) {
			return false;
		}
		slots.set(0, stack.split(1));
		setChanged();
		return true;
	}

	/** Gives the player one output stack. */
	public boolean give(Player player) {
		for (int slot = 1; slot < slots.size(); slot++) {
			ItemStack stack = slots.get(slot);
			if (!stack.isEmpty()) {
				player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
				slots.set(slot, ItemStack.EMPTY);
				setChanged();
				return true;
			}
		}
		return false;
	}

	public ItemStack input() {
		return slots.get(0);
	}

	public ItemStack output(int slot) {
		return slots.get(slot + 1);
	}

	private boolean tryRun() {
		ItemStack in = slots.get(0);
		Charge charge = Feeds.parse(in);
		if (charge == null) {
			return false;
		}
		Role role = role();
		Separator separator = role.separator();
		SizeClass next = role.next(charge.size());
		if (separator == null) {
			if (next == null) {
				return false;
			}
			ItemStack out = Feeds.stack(next.itemId(charge.rock()), 1);
			if (out.isEmpty() || !roomFor(out)) {
				return false;
			}
			in.shrink(1);
			add(out);
			setChanged();
			return true;
		}
		if (!Milling.acceptsConcentrate(charge.size(), separator, charge.rock())) {
			return false;
		}
		NuggetBank updated = copyBank();
		List<Milling.Yield> yields = Milling.concentrate(charge.rock(), separator, updated);
		if (!fits(yields)) {
			return false;
		}
		in.shrink(1);
		for (Milling.Yield yield : yields) {
			add(Feeds.stack(yield.itemId(), yield.count()));
		}
		bank.load(updated.save());
		setChanged();
		return true;
	}

	private NuggetBank copyBank() {
		NuggetBank copy = new NuggetBank();
		copy.load(bank.save());
		return copy;
	}

	private boolean fits(List<Milling.Yield> yields) {
		for (Milling.Yield yield : yields) {
			ItemStack stack = Feeds.stack(yield.itemId(), yield.count());
			if (stack.isEmpty() || !roomFor(stack)) {
				return false;
			}
		}
		return true;
	}

	private boolean roomFor(ItemStack stack) {
		int left = stack.getCount();
		for (int slot = 1; slot < slots.size(); slot++) {
			ItemStack there = slots.get(slot);
			if (there.isEmpty()) {
				return true;
			}
			if (ItemStack.isSameItemSameComponents(there, stack)) {
				left -= there.getMaxStackSize() - there.getCount();
				if (left <= 0) {
					return true;
				}
			}
		}
		return false;
	}

	private void add(ItemStack stack) {
		for (int slot = 1; slot < slots.size() && !stack.isEmpty(); slot++) {
			ItemStack there = slots.get(slot);
			if (there.isEmpty()) {
				slots.set(slot, stack);
				return;
			}
			if (ItemStack.isSameItemSameComponents(there, stack)) {
				int move = Math.min(stack.getCount(), there.getMaxStackSize() - there.getCount());
				there.grow(move);
				stack.shrink(move);
			}
		}
	}

	public boolean takes(ItemStack stack) {
		Charge charge = Feeds.parse(stack);
		if (charge == null) {
			return false;
		}
		Role role = role();
		if (role.separator() == null) {
			return role.next(charge.size()) != null;
		}
		return Milling.acceptsConcentrate(charge.size(), role.separator(), charge.rock());
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putInt("wait", wait);
		output.putString("bank", bank.save());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		wait = input.getIntOr("wait", 0);
		bank.load(input.getStringOr("bank", ""));
	}

	@Override
	public int getContainerSize() {
		return slots.size();
	}

	@Override
	public boolean isEmpty() {
		return slots.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return slots.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack out = ContainerHelper.removeItem(slots, slot, count);
		setChanged();
		return out;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(slots, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		slots.set(slot, stack);
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return false;
	}

	@Override
	public void clearContent() {
		slots.clear();
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		if (side == Direction.UP) {
			return new int[] {0};
		}
		return new int[] {1, 2, 3, 4, 5, 6, 7, 8};
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return slot == 0 && takes(stack);
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return slot == 0 && side == Direction.UP && takes(stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return slot > 0 && side != Direction.UP;
	}
}
