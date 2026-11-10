package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.block.Lines;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.DenseMedium;
import dev.wildcatter.tailings.plant.DmsState;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.Locale;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Dense-medium separation (docs/multiblocks/dense-medium-separation.md, L7) on the double-deck screen's gravel face: the deslime
 * screen, the DMS cyclone module, its two drain-and-rinse screens and the medium recovery. Gravel from a belt over the deslime
 * screen's north end loses its -1 mm to a belt on the screen's east face and goes into the module touching it. The module splits it
 * by each rock's density against the medium's: the floats into the drain-and-rinse screen on its north side, which pays them out as
 * vanilla gravel for a hopper under it; the sinks into the one on its south side, which puts them on a belt off its south end, on to
 * the cone. The medium recovery touching the module makes up the ferrosilicon it loses.
 */
public final class DenseMediumStages {
	private DenseMediumStages() {
	}

	/** The part of kind {@code machine} beyond {@code part}'s faces on {@code localSide} (its own frame), or null. */
	public static @Nullable PlantBlockEntity touchingOn(PlantBlockEntity part, Machine machine, Direction localSide) {
		for (Lines.Face face : part.side(localSide)) {
			if (BigParts.entity(part.getLevel(), face.pipe()) instanceof PlantBlockEntity there && there != part && there.machine() == machine) {
				return there;
			}
		}
		return null;
	}

	/** The module's floats screen: the drain-and-rinse screen on its north side. */
	public static @Nullable PlantBlockEntity floatsScreen(PlantBlockEntity module) {
		return touchingOn(module, Machine.DRAIN_RINSE_SCREEN, Direction.NORTH);
	}

	/** The module's sinks screen: the drain-and-rinse screen on its south side. */
	public static @Nullable PlantBlockEntity sinksScreen(PlantBlockEntity module) {
		return touchingOn(module, Machine.DRAIN_RINSE_SCREEN, Direction.SOUTH);
	}

	private static String sg(double value) {
		return String.format(Locale.ROOT, "%.2f", value);
	}

	/** The deslime screen: gravel in by a belt over its north end; its -1 mm out of its east face onto a belt; the rest into the module. */
	static final Stage DESLIME = new Stage() {
		@Override
		public boolean accepts(Rock rock, @Nullable SizeClass size) {
			return size == SizeClass.GRAVEL;
		}

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
			PlantBlockEntity module = part.touching(Machine.DMS_MODULE);
			if (module == null) {
				part.say("NOT BUILT: needs a DMS Cyclone Module touching it");
				return;
			}
			PlantBlockEntity.Sink fines = part.sink(part.side(Direction.EAST));
			if (fines == null) {
				part.say(Crushing.NOWHERE + " (a belt on its east face for the -1 mm, back to the sand belt)");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			RockLoad probe = new RockLoad();
			part.hold().forEach((rock, size, kg) -> probe.add(rock, SizeClass.SAND, kg));
			double max = DenseMedium.SCREEN_KG_PER_TICK * part.strength() * part.fast();
			max = Math.min(max, fines.room(probe) / DenseMedium.DESLIME_FINES);
			max = Math.min(max, module.room() / (1 - DenseMedium.DESLIME_FINES));
			if (max < 1e-6) {
				part.say("FULL: the module or the fines belt is full");
				return;
			}
			RockLoad taken = part.hold().take(max);
			RockLoad sand = new RockLoad();
			RockLoad gravel = new RockLoad();
			taken.forEach((rock, size, kg) -> {
				sand.add(rock, SizeClass.SAND, kg * DenseMedium.DESLIME_FINES);
				gravel.add(rock, SizeClass.GRAVEL, kg * (1 - DenseMedium.DESLIME_FINES));
			});
			fines.put(sand);
			module.receive(gravel);
			part.say("washing the fines off " + Crushing.main(taken));
			part.setChanged();
		}
	};

	/**
	 * The DMS cyclone module: deslimed gravel from the screen touching it (or by hand or hopper), water off a water pipe, ferrosilicon
	 * from the medium recovery touching it. Each rock floats or sinks by its density against the medium's.
	 */
	static final Stage MODULE = new Stage() {
		@Override
		public boolean accepts(Rock rock, @Nullable SizeClass size) {
			return size == SizeClass.GRAVEL;
		}

		@Override
		public double waterHold() {
			return DenseMedium.WATER_HOLD;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			DmsState dms = part.dms();
			if (dms == null) {
				return;
			}
			if (part.hold().isEmpty()) {
				part.say("NO FEED: gravel from a deslime screen touching it");
				return;
			}
			PlantBlockEntity floats = floatsScreen(part);
			PlantBlockEntity sinks = sinksScreen(part);
			if (floats == null || sinks == null) {
				part.say("NOT BUILT: needs a Drain-and-Rinse Screen on its " + (floats == null ? "north side, for the floats" : "south side, for the sinks"));
				return;
			}
			PlantBlockEntity recovery = part.touching(Machine.MEDIUM_RECOVERY);
			if (recovery == null) {
				part.say("NOT BUILT: needs a Medium Recovery touching it");
				return;
			}
			if (part.water() < 1) {
				part.say("NO WATER");
				return;
			}
			if (recovery.dryKg() <= 1e-9) {
				part.say("NO MEDIUM: ferrosilicon in the medium recovery's hopper");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			if (!recovery.running()) {
				part.say("waiting for the medium recovery's motor");
				return;
			}
			double medium = dms.medium();
			double sinkShare = DenseMedium.sinkShare(part.hold(), medium);
			double max = Math.min(DenseMedium.KG_PER_TICK * part.strength() * part.fast(), part.hold().total());
			max = Math.min(max, part.water() / DenseMedium.WATER_PER_KG);
			max = Math.min(max, recovery.dryKg() / (DenseMedium.FESI_KG_PER_TONNE / 1000));
			if (sinkShare > 1e-9) {
				max = Math.min(max, sinks.room() / sinkShare);
			}
			if (sinkShare < 1 - 1e-9) {
				max = Math.min(max, floats.room() / (1 - sinkShare));
			}
			if (max < 1e-6) {
				part.say("FULL: a drain-and-rinse screen is full");
				return;
			}
			RockLoad taken = part.hold().take(max);
			DenseMedium.Split split = DenseMedium.split(taken, medium);
			if (!split.sinks().isEmpty()) {
				sinks.receive(split.sinks());
			}
			if (!split.floats().isEmpty()) {
				floats.receive(split.floats());
			}
			double lost = DenseMedium.fesiLost(taken.total());
			recovery.setDryKg(recovery.dryKg() - lost);
			part.useWater(taken.total() * DenseMedium.WATER_PER_KG);
			dms.count(taken.total(), split.floats().total(), lost, part.getLevel().getGameTime());
			Rock main = taken.main();
			part.say(main == null ? "separating at SG " + sg(medium)
					: "separating at SG " + sg(medium) + ": " + main.displayName() + " (SG " + sg(DenseMedium.pieceSg(main)) + ") " + (DenseMedium.sinks(main, medium) ? "sinks" : "floats"));
			part.setChanged();
		}
	};

	/**
	 * A drain-and-rinse screen: on the module's north side it drains the floats and pays them out as vanilla gravel, 1.9 t a block, for
	 * a hopper under it or a hand; on its south side it drains the sinks onto a belt off its own south end.
	 */
	static final Stage DRAIN_RINSE = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			PlantBlockEntity module = part.touching(Machine.DMS_MODULE);
			boolean floats = module != null && floatsScreen(module) == part;
			if (floats) {
				payGravel(part);
			}
			if (part.hold().isEmpty()) {
				part.say(module == null ? "NOT BUILT: needs a DMS Cyclone Module touching it" : floats && full(part) ? "FULL: take the gravel" : "NO FEED");
				return;
			}
			PlantBlockEntity.Sink belt = floats ? null : part.sink(part.side(Direction.SOUTH));
			if (!floats && belt == null) {
				part.say(Crushing.NOWHERE + " (a belt on its south end for the sinks, to the cone)");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double max = DenseMedium.SCREEN_KG_PER_TICK * part.strength() * part.fast();
			if (floats) {
				if (full(part)) {
					part.say("FULL: take the gravel");
					return;
				}
			} else {
				max = Math.min(max, belt.room(part.hold()));
			}
			if (max < 1e-6) {
				part.say("FULL: its belt is full");
				return;
			}
			RockLoad taken = part.hold().take(max);
			if (floats) {
				part.made().add(taken);
				part.say("draining the floats: " + Crushing.main(taken) + " out as gravel");
			} else {
				belt.put(taken);
				part.say("draining the sinks: " + Crushing.main(taken) + " on to the cone");
			}
			part.setChanged();
		}
	};

	/** Two blocks' worth of floats wait and its product slot holds no more: it stops. */
	private static boolean full(PlantBlockEntity part) {
		return part.made().total() >= 2 * DenseMedium.FLOATS_KG_PER_GRAVEL;
	}

	/** The floats screen's drained floats, whole blocks of vanilla gravel into its product slot while it has room. */
	private static void payGravel(PlantBlockEntity part) {
		while (part.made().total() >= DenseMedium.FLOATS_KG_PER_GRAVEL - 1e-6 && part.emit(new ItemStack(Items.GRAVEL))) {
			part.made().take(DenseMedium.FLOATS_KG_PER_GRAVEL);
			part.setChanged();
		}
	}

	/** The medium recovery: ferrosilicon in by hopper or hand; it runs while the module touching it has gravel to treat. */
	static final Stage RECOVERY = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			PlantBlockEntity module = part.touching(Machine.DMS_MODULE);
			if (module == null) {
				part.say("NOT BUILT: needs a DMS Cyclone Module touching it");
				return;
			}
			if (part.dryKg() <= 1e-9) {
				part.say("NO MEDIUM: ferrosilicon by hopper or by hand");
				return;
			}
			if (module.hold().isEmpty()) {
				part.say(String.format(Locale.ROOT, "idle: %.1f kg of ferrosilicon", part.dryKg()));
				return;
			}
			part.want();
			part.say(part.running() ? String.format(Locale.ROOT, "recovering the medium: %.1f kg of ferrosilicon left", part.dryKg()) : "waiting for its motor");
		}
	};
}
