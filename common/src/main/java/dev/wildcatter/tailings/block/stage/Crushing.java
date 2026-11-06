package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The dry end of the crushing circuit (docs/multiblocks/crushing-circuit.md): the grizzly, the jaw, the double-deck screen and the
 * cone. Each works at its share of 50 t/h times its motor's output, and only as fast as its product has somewhere to go.
 */
final class Crushing {
	private Crushing() {
	}

	static final String NOWHERE = "its output has nowhere to go";

	/** A static bar grizzly over the jaw: +150 mm drops into the jaw under it, -150 mm out of its east face onto a belt. */
	static final Stage GRIZZLY = new Stage() {
		@Override
		public boolean accepts(Rock rock, @Nullable SizeClass size) {
			return size == null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			if (part.hold().isEmpty()) {
				part.say("NO FEED");
				return;
			}
			PlantBlockEntity jaw = part.under(Machine.JAW);
			if (jaw == null) {
				part.say("NOT BUILT: needs a Jaw Crusher under it");
				return;
			}
			PlantBlockEntity.Sink bypass = part.sink(part.side(Direction.EAST));
			if (bypass == null) {
				part.say(NOWHERE + " (a belt on its east face for the -150 mm)");
				return;
			}
			double max = PlantRates.KG_PER_TICK;
			RockLoad probe = new RockLoad();
			part.hold().forEach((rock, size, kg) -> probe.add(rock, SizeClass.CRUSHED, kg));
			max = Math.min(max, bypass.room(probe) / PlantRates.GRIZZLY_UNDERSIZE);
			max = Math.min(max, jaw.room() / (1 - PlantRates.GRIZZLY_UNDERSIZE));
			if (max < 1e-6) {
				part.say("FULL: the jaw or its bypass belt is full");
				return;
			}
			RockLoad taken = part.hold().take(max);
			RockLoad under = new RockLoad();
			RockLoad over = new RockLoad();
			taken.forEach((rock, size, kg) -> {
				under.add(rock, SizeClass.CRUSHED, kg * PlantRates.GRIZZLY_UNDERSIZE);
				over.add(rock, null, kg * (1 - PlantRates.GRIZZLY_UNDERSIZE));
			});
			bypass.put(under);
			jaw.receive(over);
			part.say("screening " + main(taken));
			part.setChanged();
		}
	};

	/** The jaw: unbroken rock to crushed (-150 mm), out of its south face onto a belt. */
	static final Stage JAW = simple(1, size -> size == null ? SizeClass.CRUSHED : null, Direction.SOUTH);

	/** The cone: the screen's oversize to gravel (-40 mm), out of its south face onto a belt back to the screen. */
	static final Stage CONE = simple(PlantRates.CONE_CAPACITY,
			size -> size == SizeClass.CRUSHED || size == SizeClass.GRAVEL ? SizeClass.GRAVEL : null, Direction.SOUTH);

	/** The double-deck screen: sand (-6 mm) out of its east face (or its hopper), the oversize out of its south face to the cone. */
	static final Stage SCREEN = new Stage() {
		@Override
		public boolean accepts(Rock rock, @Nullable SizeClass size) {
			return size == SizeClass.CRUSHED || size == SizeClass.GRAVEL;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			payHopper(part);
			if (part.hold().isEmpty()) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			PlantBlockEntity.Sink oversize = part.sink(part.side(Direction.SOUTH));
			if (oversize == null) {
				part.say(NOWHERE + " (a belt on its south face for the oversize)");
				return;
			}
			PlantBlockEntity.Sink sand = part.sink(part.side(Direction.EAST));
			double sandShare = sandShare(part.hold());
			RockLoad sandProbe = new RockLoad();
			RockLoad overProbe = new RockLoad();
			part.hold().forEach((rock, size, kg) -> {
				sandProbe.add(rock, SizeClass.SAND, kg);
				overProbe.add(rock, size, kg);
			});
			double max = PlantRates.KG_PER_TICK * PlantRates.SCREEN_CAPACITY * part.strength();
			max = Math.min(max, oversize.room(overProbe) / Math.max(1e-6, 1 - sandShare));
			if (sand != null) {
				max = Math.min(max, sand.room(sandProbe) / Math.max(1e-6, sandShare));
			} else if (part.made().total() >= PlantRates.BLOCK_KG * 2) {
				max = 0;
			}
			if (max < 1e-6) {
				part.say("FULL: " + (sand == null ? "take the sand from its hopper" : "its belts are full"));
				return;
			}
			RockLoad taken = part.hold().take(max);
			RockLoad toSand = new RockLoad();
			RockLoad toCone = new RockLoad();
			taken.forEach((rock, size, kg) -> {
				double share = size == SizeClass.GRAVEL ? PlantRates.SCREEN_SAND_FROM_GRAVEL : PlantRates.SCREEN_SAND_FROM_CRUSHED;
				toSand.add(rock, SizeClass.SAND, kg * share);
				toCone.add(rock, size, kg * (1 - share));
			});
			oversize.put(toCone);
			if (sand != null) {
				sand.put(toSand);
			} else {
				part.made().add(toSand);
			}
			part.say("screening " + main(taken));
			part.setChanged();
		}
	};

	/** The share of what the screen holds that passes 6 mm. */
	private static double sandShare(RockLoad load) {
		double total = load.total();
		if (total <= 0) {
			return PlantRates.SCREEN_SAND_FROM_CRUSHED;
		}
		double gravel = load.total((rock, size) -> size == SizeClass.GRAVEL);
		return (gravel * PlantRates.SCREEN_SAND_FROM_GRAVEL + (total - gravel) * PlantRates.SCREEN_SAND_FROM_CRUSHED) / total;
	}

	/** With no belt on its sand face, the screen bags a block's worth of each rock's sand as an item for the hopper under it. */
	private static void payHopper(PlantBlockEntity part) {
		Rock[] ready = {null};
		part.made().forEach((rock, size, kg) -> {
			if (ready[0] == null && kg >= PlantRates.BLOCK_KG) {
				ready[0] = rock;
			}
		});
		if (ready[0] == null) {
			return;
		}
		ItemStack sand = Feeds.stack(SizeClass.SAND.itemId(ready[0]), 1);
		if (part.emit(sand)) {
			Rock rock = ready[0];
			part.made().take(PlantRates.BLOCK_KG, (r, s) -> r == rock);
		}
	}

	/** A crusher: takes what {@code to} maps, makes that size, puts it out of {@code out} onto a belt. */
	private static Stage simple(double capacity, java.util.function.Function<@Nullable SizeClass, @Nullable SizeClass> to, Direction out) {
		return new Stage() {
			@Override
			public boolean accepts(Rock rock, @Nullable SizeClass size) {
				return to.apply(size) != null;
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
				PlantBlockEntity.Sink sink = part.sink(part.side(out));
				if (sink == null) {
					part.say(NOWHERE + " (a belt on its " + out.getName() + " face)");
					return;
				}
				RockLoad probe = new RockLoad();
				part.hold().forEach((rock, size, kg) -> probe.add(rock, to.apply(size), kg));
				double max = Math.min(PlantRates.KG_PER_TICK * capacity * part.strength(), sink.room(probe));
				if (max < 1e-6) {
					part.say("FULL: its belt is full");
					return;
				}
				RockLoad taken = part.hold().take(max);
				RockLoad made = new RockLoad();
				taken.forEach((rock, size, kg) -> made.add(rock, to.apply(size), kg));
				sink.put(made);
				part.say("crushing " + main(taken));
				part.setChanged();
			}
		};
	}

	static String main(RockLoad load) {
		Rock rock = load.main();
		return rock == null ? "rock" : rock.displayName();
	}
}
