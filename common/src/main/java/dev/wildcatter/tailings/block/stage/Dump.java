package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.haul.Sorter;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantRates;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * The dump station past the pocket (docs/multiblocks/dump-station.md): the apron feeder draws rock off the pocket's bottom onto the
 * sorter, one stack a second, and the sensor sorter sends each rock and ore type out of the side its screen sets, into the ore bin
 * touching that side.
 */
final class Dump {
	private Dump() {
	}

	/** What the sorter holds on its belt, waiting to be sorted: two stacks. */
	static final int SORTER_HOLD = 2 * Haul.STACK;

	/** The pocket the part touches, or null. */
	static @Nullable DumpBlockEntity pocket(PlantBlockEntity part) {
		List<BlockPos> mine = part.cells();
		for (BlockPos cell : mine) {
			for (Direction side : Direction.values()) {
				if (!mine.contains(cell.relative(side)) && BigParts.entity(part.getLevel(), cell.relative(side)) instanceof DumpBlockEntity pocket) {
					return pocket;
				}
			}
		}
		return null;
	}

	static int count(Map<String, Integer> items) {
		int n = 0;
		for (int c : items.values()) {
			n += c;
		}
		return n;
	}

	static final Stage FEEDER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			DumpBlockEntity pocket = pocket(part);
			if (pocket == null) {
				part.say("NOT BUILT: needs a Dump Pocket touching it");
				return;
			}
			PlantBlockEntity sorter = part.touching(Machine.ORE_SORTER);
			if (sorter == null) {
				part.say("NOT BUILT: needs an Ore Sorter at its tail end");
				return;
			}
			if (pocket.pocket() == 0) {
				part.say("NO FEED: the pocket is empty");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			int room = SORTER_HOLD - count(sorter.binned());
			if (room <= 0) {
				part.say("FULL: the sorter is full");
				return;
			}
			if (part.getLevel().getGameTime() % 20 != 0) {
				return;
			}
			int max = Math.min(room, (int) Math.round(Haul.STACK * part.strength()));
			pocket.draw(max).forEach((id, n) -> sorter.binned().merge(id, n, Integer::sum));
			sorter.setChanged();
			part.say("feeding the sorter, " + pocket.pocket() + " items in the pocket");
		}
	};

	static final Stage SORTER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			Map<String, Integer> waiting = part.binned();
			if (waiting.isEmpty()) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			if (part.getLevel().getGameTime() % 20 != 0) {
				return;
			}
			Sorter screen = part.sorter();
			int budget = (int) Math.round(Haul.STACK * part.strength());
			List<String> stuck = new ArrayList<>();
			Iterator<Map.Entry<String, Integer>> it = waiting.entrySet().iterator();
			while (it.hasNext() && budget > 0) {
				Map.Entry<String, Integer> e = it.next();
				int side = screen.route(e.getKey());
				if (side < 0) {
					stuck.add(name(e.getKey()) + " (no side set, and no waste side)");
					continue;
				}
				PlantBlockEntity bin = bin(part, Direction.from2DDataValue((side + 2) % 4));
				if (bin == null) {
					stuck.add(name(e.getKey()) + " (no Ore Bin on that side: NOT BUILT)");
					continue;
				}
				int fits = Math.min(Math.min(e.getValue(), budget), PlantRates.BIN_ITEMS - bin.binCount());
				if (fits <= 0) {
					stuck.add(name(e.getKey()));
					continue;
				}
				bin.binned().merge(e.getKey(), fits, Integer::sum);
				bin.setChanged();
				budget -= fits;
				if (fits == e.getValue()) {
					it.remove();
				} else {
					e.setValue(e.getValue() - fits);
				}
			}
			part.setChanged();
			part.say(stuck.isEmpty() ? "sorting" : "FULL: " + String.join(", ", stuck) + " waits");
		}
	};

	/** The ore bin touching the sorter on {@code side}, or null. */
	static @Nullable PlantBlockEntity bin(PlantBlockEntity sorter, Direction side) {
		List<BlockPos> mine = sorter.cells();
		for (BlockPos cell : mine) {
			BlockPos next = cell.relative(side);
			if (!mine.contains(next) && BigParts.entity(sorter.getLevel(), next) instanceof PlantBlockEntity bin && bin.machine() == Machine.ORE_BIN) {
				return bin;
			}
		}
		return null;
	}

	static String name(String itemId) {
		return itemId.substring(itemId.indexOf(':') + 1).replace('_', ' ');
	}

	/** The sorter's side numbers, 0 north to 3 west, from a world side. */
	static int side(Direction direction) {
		return (direction.get2DDataValue() + 2) % 4;
	}
}
