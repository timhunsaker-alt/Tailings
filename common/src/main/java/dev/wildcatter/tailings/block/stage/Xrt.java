package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.plant.XrtSorting;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * The X-ray transmission ore sorter and its compressor (docs/multiblocks/xrt-ore-sorter.md, L8). Crushed rock or gravel comes in by a
 * belt over its north (feed) end, a hopper or a hand; each piece is read and the ones over the threshold are blown by the ejectors off
 * its south (discharge) end onto the product belt; the rest drop out of its east face onto the waste belt. With no air it ejects
 * nothing, so it stops rather than send ore to waste.
 */
final class Xrt {
	private Xrt() {
	}

	static final Stage SORTER = new Stage() {
		@Override
		public boolean accepts(Rock rock, @Nullable SizeClass size) {
			return XrtSorting.sorts(size);
		}

		/** Fed by a belt over its north (feed) end, where the vibrating feeder spreads the rock onto its belt. */
		@Override
		public boolean feeds(PlantBlockEntity part, int[] at, Direction localSide) {
			return Stage.feedEndTop(part, at, localSide);
		}

		@Override
		public void tick(PlantBlockEntity part) {
			if (part.hold().isEmpty()) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			PlantBlockEntity compressor = part.touching(Machine.SORTER_COMPRESSOR);
			if (compressor == null) {
				part.say("NO AIR: needs a Sorter Air Compressor touching it");
				return;
			}
			if (!compressor.running()) {
				part.say("NO AIR: the compressor is not running");
				return;
			}
			PlantBlockEntity.Sink product = part.sink(part.side(Direction.SOUTH));
			if (product == null) {
				part.say(Crushing.NOWHERE + " (a belt on its south face for the product)");
				return;
			}
			PlantBlockEntity.Sink waste = part.sink(part.side(Direction.EAST));
			if (waste == null) {
				part.say(Crushing.NOWHERE + " (a belt on its east face for the waste)");
				return;
			}
			int step = part.xrtStep();
			RockLoad toProduct = new RockLoad();
			RockLoad toWaste = new RockLoad();
			split(part.hold(), step, toProduct, toWaste);
			double total = part.hold().total();
			double productShare = toProduct.total() / total;
			double gravel = part.hold().total((rock, size) -> size == SizeClass.GRAVEL) / total;
			double max = XrtSorting.kgPerTick(gravel) * part.strength();
			if (productShare > 1e-9) {
				max = Math.min(max, product.room(toProduct) / productShare);
			}
			if (productShare < 1 - 1e-9) {
				max = Math.min(max, waste.room(toWaste) / (1 - productShare));
			}
			if (max < 1e-6) {
				part.say("FULL: its belts are full");
				return;
			}
			RockLoad taken = part.hold().take(max);
			toProduct.clear();
			toWaste.clear();
			split(taken, step, toProduct, toWaste);
			if (!toProduct.isEmpty()) {
				product.put(toProduct);
			}
			if (!toWaste.isEmpty()) {
				waste.put(toWaste);
			}
			part.xrt().sent(toProduct.total(), toWaste.total());
			part.say("sorting " + Crushing.main(taken));
			part.setChanged();
		}
	};

	/** Each rock's kg split two ways at {@code step}, keeping its size. */
	static void split(RockLoad load, int step, RockLoad toProduct, RockLoad toWaste) {
		load.forEach((rock, size, kg) -> {
			double share = XrtSorting.productShare(rock, step);
			toProduct.add(rock, size, kg * share);
			toWaste.add(rock, size, kg * (1 - share));
		});
	}

	/**
	 * The compressor runs while it touches a sorter that has rock to sort or is sorting (on a thin feed the sorter's hold empties every
	 * tick); the sorter reads it. A started motor runs on through gaps, as every plant motor does.
	 */
	static final Stage COMPRESSOR = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			PlantBlockEntity sorter = part.touching(Machine.XRT_SORTER);
			if (sorter == null) {
				part.say("NOT BUILT: needs an X-Ray Ore Sorter touching it");
				return;
			}
			if (sorter.hold().isEmpty() && !sorter.status().startsWith("sorting")) {
				part.say("idle: the sorter has no feed");
				return;
			}
			part.want();
			part.say(part.running() ? "air to the ejectors at 7 bar" : "waiting for its motor");
		}
	};
}
