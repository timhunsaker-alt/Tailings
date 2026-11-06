package dev.wildcatter.tailings.block;

import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.haul.Haul;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The dump pocket (docs/multiblocks/dump-station.md): a concrete pocket under the track that takes each tipping car's whole load, up
 * to one 10-car train. The apron feeder touching it draws the rock out to the sorter. When it cannot take a car's load, that car
 * does not tip and the train waits (decided with Tim, 2026-11-05).
 */
public class DumpBlockEntity extends BlockEntity implements BigPartEntity, Inspectable {
	private final Map<String, Integer> held = new LinkedHashMap<>();
	private boolean grown;

	public DumpBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.DUMP, pos, state);
	}

	public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, DumpBlockEntity dump) {
		// the apron feeder draws from it
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	/** The items the pocket holds. */
	public int pocket() {
		int n = 0;
		for (int c : held.values()) {
			n += c;
		}
		return n;
	}

	public int held(String itemId) {
		return held.getOrDefault(itemId, 0);
	}

	/** Takes a car's whole load if it fits; otherwise none of it. */
	public boolean offer(Map<String, Integer> load) {
		if (load == null || load.isEmpty()) {
			return true;
		}
		int sum = 0;
		for (int n : load.values()) {
			sum += Math.max(0, n);
		}
		if (pocket() + sum > Haul.POCKET_ITEMS) {
			return false;
		}
		load.forEach((id, n) -> {
			if (n > 0) {
				held.merge(id, n, Integer::sum);
			}
		});
		setChanged();
		return true;
	}

	/** Draws up to {@code max} items off the pocket's bottom, oldest type first. */
	public Map<String, Integer> draw(int max) {
		Map<String, Integer> out = new LinkedHashMap<>();
		Iterator<Map.Entry<String, Integer>> it = held.entrySet().iterator();
		while (max > 0 && it.hasNext()) {
			Map.Entry<String, Integer> e = it.next();
			int take = Math.min(max, e.getValue());
			out.put(e.getKey(), take);
			max -= take;
			if (take == e.getValue()) {
				it.remove();
			} else {
				e.setValue(e.getValue() - take);
			}
		}
		if (!out.isEmpty()) {
			setChanged();
		}
		return out;
	}

	/** Puts back what the sorter could not take. */
	public void putBack(Map<String, Integer> items) {
		Map<String, Integer> first = new LinkedHashMap<>(items);
		held.forEach((id, n) -> first.merge(id, n, Integer::sum));
		held.clear();
		held.putAll(first);
		setChanged();
	}

	@Override
	public Component inspect() {
		return Component.literal("Dump Pocket: holds " + pocket() + " of " + Haul.POCKET_ITEMS + " items" + (pocket() >= Haul.POCKET_ITEMS ? " (FULL: the next car waits)" : ""));
	}

	/** Breaking the pocket drops what it holds. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null) {
			held.forEach((id, n) -> {
				for (int left = n; left > 0; left -= 64) {
					Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), Feeds.stack(id, Math.min(64, left)));
				}
			});
			held.clear();
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		StringBuilder out = new StringBuilder();
		held.forEach((id, n) -> out.append(out.length() == 0 ? "" : ",").append(id).append('=').append(n));
		output.putString("held", out.toString());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		held.clear();
		// a pocket saved before R4c sorted into nine slots: its items come back into the pocket
		net.minecraft.core.NonNullList<net.minecraft.world.item.ItemStack> old = net.minecraft.core.NonNullList.withSize(9, net.minecraft.world.item.ItemStack.EMPTY);
		net.minecraft.world.ContainerHelper.loadAllItems(input, old);
		for (net.minecraft.world.item.ItemStack stack : old) {
			if (!stack.isEmpty()) {
				held.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), Integer::sum);
			}
		}
		for (String part : input.getStringOr("held", "").split(",")) {
			int eq = part.lastIndexOf('=');
			if (eq > 0) {
				try {
					held.put(part.substring(0, eq), Integer.parseInt(part.substring(eq + 1)));
				} catch (NumberFormatException ex) {
					// left out
				}
			}
		}
	}
}
