package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.haul.Sorter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The dump pocket and sorter. Slot 0 is waste. Slots 1 to 8 are product bins: a bin keeps the item it already holds.
 * The pocket buffers one 30 t train, then the next train waits.
 */
public class DumpBlockEntity extends BlockEntity implements WorldlyContainer {
	public static final int SLOTS = 9;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
	private int pocket;
	private int apron;

	public DumpBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.DUMP, pos, state);
	}

	public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, DumpBlockEntity dump) {
		if (dump.pocket <= 0) {
			return;
		}
		dump.apron++;
		if (dump.apron >= 100) {
			dump.apron = 0;
			dump.pocket = Math.max(0, dump.pocket - Haul.ITEM_KG);
			dump.setChanged();
		}
	}

	public int pocket() {
		return pocket;
	}

	/** Tips a train's kilograms. One item is 1,000 kg. Refuses the whole tip if the pocket or a bin cannot take it. */
	public boolean offer(Map<String, Integer> load) {
		if (load == null || load.isEmpty()) {
			return true;
		}
		Sorter sorter = snapshot();
		if (!sorter.tip(load)) {
			return false;
		}
		for (Map.Entry<String, Integer> entry : load.entrySet()) {
			int items = entry.getValue() / Haul.ITEM_KG;
			if (items <= 0) {
				continue;
			}
			insert(entry.getKey(), items);
		}
		pocket = sorter.pocket();
		setChanged();
		return true;
	}

	private Sorter snapshot() {
		Set<String> products = new LinkedHashSet<>();
		Map<String, Integer> bins = new LinkedHashMap<>();
		int waste = 0;
		for (int slot = 0; slot < SLOTS; slot++) {
			ItemStack stack = slots.get(slot);
			if (stack.isEmpty()) {
				continue;
			}
			String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			int kg = stack.getCount() * Haul.ITEM_KG;
			if (slot == 0) {
				waste += kg;
			} else {
				products.add(id);
				bins.merge(id, kg, Integer::sum);
			}
		}
		Sorter sorter = new Sorter(products);
		sorter.restore(bins, waste, pocket);
		return sorter;
	}

	private void insert(String id, int items) {
		Item item = BuiltInRegistries.ITEM.getValue(Identifier.parse(id));
		if (item == null || item == Items.AIR) {
			return;
		}
		int slot = slotFor(item);
		if (slot < 0) {
			return;
		}
		ItemStack there = slots.get(slot);
		if (there.isEmpty()) {
			slots.set(slot, new ItemStack(item, Math.min(items, item.getDefaultMaxStackSize())));
		} else {
			there.grow(Math.min(items, there.getMaxStackSize() - there.getCount()));
		}
	}

	private int slotFor(Item item) {
		for (int slot = 1; slot < SLOTS; slot++) {
			ItemStack there = slots.get(slot);
			if (there.getItem() == item && there.getCount() < there.getMaxStackSize()) {
				return slot;
			}
		}
		ItemStack waste = slots.get(0);
		if (waste.isEmpty() || (waste.getItem() == item && waste.getCount() < waste.getMaxStackSize())) {
			return 0;
		}
		for (int slot = 1; slot < SLOTS; slot++) {
			if (slots.get(slot).isEmpty()) {
				return slot;
			}
		}
		return -1;
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		ContainerHelper.saveAllItems(output, slots);
		output.putInt("pocket", pocket);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		slots.clear();
		ContainerHelper.loadAllItems(input, slots);
		pocket = input.getIntOr("pocket", 0);
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
		return Container.stillValidBlockEntity(this, player);
	}

	@Override
	public void clearContent() {
		slots.clear();
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		int[] slots = new int[SLOTS];
		for (int i = 0; i < SLOTS; i++) {
			slots[i] = i;
		}
		return slots;
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		return false;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return false;
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return true;
	}
}
