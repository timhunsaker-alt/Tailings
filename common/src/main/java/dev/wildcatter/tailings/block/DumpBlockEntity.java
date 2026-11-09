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
	/** Rock on the rail pocket's 300 mm grizzly bars, too big to pass, by item: it waits to be broken or lifted off. */
	private final Map<String, Integer> oversize = new LinkedHashMap<>();
	/** Items' worth of oversize owed, under a whole item. */
	private double oversizeDue;
	/** The pit hopper's part blocks: what a truck tipped under a whole block of each ore, kept for the next load (Tim, 2026-11-09). */
	private final dev.wildcatter.tailings.pit.PitLoad part = new dev.wildcatter.tailings.pit.PitLoad(Long.MAX_VALUE);
	private boolean grown;

	/** The truck dump's hopper (docs/multiblocks/open-pit.md): 400 t. */
	public static final int PIT_HOPPER_KG = 400_000;

	public DumpBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.DUMP, pos, state);
	}

	/** The truck dump's hopper rather than the rail dump pocket. */
	public boolean pit() {
		return getBlockState().getBlock() instanceof PlantMachineBlock block && block.machine() == dev.wildcatter.tailings.plant.Machine.PIT_HOPPER;
	}

	/** Items it holds when full: a rail pocket one 10-car train, the pit hopper 400 t of bank blocks. */
	public int capacity() {
		return pit() ? PIT_HOPPER_KG / dev.wildcatter.tailings.pit.PitMass.ROCK_KG : Haul.POCKET_ITEMS;
	}

	/**
	 * A truck tips its load in (open-pit.md, Truck dump), all or none, and comes away empty: its rock and ore go in as whole bank
	 * blocks, and what is under a whole block of each is kept here and made up by the next load (decided with Tim, 2026-11-09).
	 * Returns null when it took the load, or why not: soil goes to a dump, never to the plant (decided with Tim, 2026-11-09).
	 */
	public @org.jspecify.annotations.Nullable String tip(dev.wildcatter.tailings.pit.PitLoad load) {
		if (load.hasSoil()) {
			return "not rock: tip it on a dump";
		}
		dev.wildcatter.tailings.pit.PitLoad all = new dev.wildcatter.tailings.pit.PitLoad(Long.MAX_VALUE);
		part.contents().forEach(all::add);
		load.contents().forEach(all::add);
		Map<String, Integer> whole = all.wholeBlocks();
		int sum = 0;
		for (int n : whole.values()) {
			sum += n;
		}
		if (pocket() + sum > capacity()) {
			return "FULL";
		}
		whole.forEach((id, n) -> {
			held.merge(id, n, Integer::sum);
			all.take(id, (long) n * dev.wildcatter.tailings.pit.PitMass.kg(id));
		});
		part.clear();
		all.contents().forEach(part::add);
		load.clear();
		setChanged();
		return null;
	}

	/** Kilograms kept under a whole block, by item. */
	public Map<String, Long> part() {
		return part.contents();
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

	/**
	 * The pocket a car on the rail at {@code rail} tips into: a dump pocket under the rail, with a dump ramp beside it
	 * (docs/multiblocks/dump-station.md). Null when there is none.
	 */
	public static @org.jspecify.annotations.Nullable DumpBlockEntity under(net.minecraft.world.level.Level level, BlockPos rail) {
		boolean ramp = false;
		for (net.minecraft.core.Direction side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
			if (level.getBlockState(rail.relative(side)).getBlock() instanceof PlantMachineBlock block
					&& block.machine() == dev.wildcatter.tailings.plant.Machine.DUMP_RAMP) {
				ramp = true;
			}
		}
		if (!ramp) {
			return null;
		}
		BlockState below = level.getBlockState(rail.below());
		if (below.getBlock() instanceof PlantMachineBlock block && block.machine() == dev.wildcatter.tailings.plant.Machine.DUMP_POCKET
				&& level.getBlockEntity(PlantMachineBlock.mainOf(rail.below(), below)) instanceof DumpBlockEntity pocket) {
			return pocket;
		}
		return null;
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

	/** Takes a car's whole load if it fits and the grizzly is not choked; otherwise none of it. Its oversize stays on the bars. */
	public boolean offer(Map<String, Integer> load) {
		if (load == null || load.isEmpty()) {
			return true;
		}
		int sum = 0;
		for (int n : load.values()) {
			sum += Math.max(0, n);
		}
		if (!takes(sum)) {
			return false;
		}
		load.forEach((id, n) -> {
			if (n > 0) {
				held.merge(id, n, Integer::sum);
			}
		});
		if (!pit()) {
			grizzly(load, sum);
		}
		setChanged();
		return true;
	}

	/** True when a car's load of {@code items} would tip now: there is room, and the grizzly is not choked with oversize. */
	public boolean takes(int items) {
		return !choked() && pocket() + items <= capacity();
	}

	/** The share of a load that is over the bars' 300 mm stays on them, taken from the load's commonest rock (dump-station.md). */
	private void grizzly(Map<String, Integer> load, int sum) {
		oversizeDue += sum * Haul.OVERSIZE_SHARE;
		while (oversizeDue >= 1 - 1e-9) {
			String most = null;
			for (Map.Entry<String, Integer> e : load.entrySet()) {
				if (held.getOrDefault(e.getKey(), 0) > 0 && (most == null || e.getValue() > load.get(most))) {
					most = e.getKey();
				}
			}
			if (most == null) {
				break;
			}
			oversizeDue -= 1;
			held.merge(most, -1, Integer::sum);
			held.remove(most, 0);
			oversize.merge(most, 1, Integer::sum);
		}
	}

	/** Items of oversize on the grizzly's bars. */
	public int oversize() {
		int n = 0;
		for (int c : oversize.values()) {
			n += c;
		}
		return n;
	}

	/** Too much oversize on the bars: no car tips until it is cleared. */
	public boolean choked() {
		return oversize() >= Haul.GRIZZLY_CHOKE;
	}

	/** Breaks one piece of oversize through the bars into the pocket, as a rock breaker does; the item it was, or null with none. */
	public @org.jspecify.annotations.Nullable String breakOne() {
		Iterator<Map.Entry<String, Integer>> it = oversize.entrySet().iterator();
		if (!it.hasNext()) {
			return null;
		}
		Map.Entry<String, Integer> e = it.next();
		String id = e.getKey();
		if (e.getValue() <= 1) {
			it.remove();
		} else {
			e.setValue(e.getValue() - 1);
		}
		held.merge(id, 1, Integer::sum);
		setChanged();
		return id;
	}

	/** Lifts all the oversize off the bars, returned to whoever cleared it. */
	public Map<String, Integer> liftOff() {
		Map<String, Integer> out = new LinkedHashMap<>(oversize);
		oversize.clear();
		setChanged();
		return out;
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
		if (pit()) {
			return Component.literal("Pit Hopper: holds " + pocket() * dev.wildcatter.tailings.pit.PitMass.ROCK_KG / 1000 + " t of " + PIT_HOPPER_KG / 1000 + " t"
					+ (pocket() >= capacity() ? " (FULL: the next truck waits)" : ""));
		}
		String bars = oversize() == 0 ? "" : "; " + oversize() + " oversize on the grizzly" + (choked() ? " (CHOKED: break it with a pickaxe, or sneak-click to lift it off)" : "");
		return Component.literal("Dump Pocket: holds " + pocket() + " of " + Haul.POCKET_ITEMS + " items" + (pocket() >= Haul.POCKET_ITEMS ? " (FULL: the next car waits)" : "") + bars);
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
			oversize.forEach((id, n) -> {
				for (int left = n; left > 0; left -= 64) {
					Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), Feeds.stack(id, Math.min(64, left)));
				}
			});
			oversize.clear();
		}
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
		StringBuilder out = new StringBuilder();
		held.forEach((id, n) -> out.append(out.length() == 0 ? "" : ",").append(id).append('=').append(n));
		output.putString("held", out.toString());
		output.putString("part", part.save());
		StringBuilder bars = new StringBuilder();
		oversize.forEach((id, n) -> bars.append(bars.length() == 0 ? "" : ",").append(id).append('=').append(n));
		output.putString("oversize", bars.toString());
		output.putDouble("oversizeDue", oversizeDue);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		held.clear();
		oversize.clear();
		oversizeDue = input.getDoubleOr("oversizeDue", 0);
		for (String one : input.getStringOr("oversize", "").split(",")) {
			int eq = one.lastIndexOf('=');
			if (eq > 0) {
				try {
					oversize.put(one.substring(0, eq), Integer.parseInt(one.substring(eq + 1)));
				} catch (NumberFormatException ex) {
					// left out
				}
			}
		}
		part.load(input.getStringOr("part", ""));
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
