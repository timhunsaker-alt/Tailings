package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.block.Lines;
import dev.wildcatter.block.Slurry;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Assays;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.recovery.RecoveryRoute;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The flotation circuit (docs/multiblocks/flotation-bank.md): the conditioning tank, the bank of eight cells with its blower, the
 * concentrate thickener, the pressure filter, and the tailings pond for the final tail. Each part draws its feed off steel pipe into
 * a few seconds' buffer, so it knows the feed's assay before it works it, and works it only while its product has room.
 */
final class Flotation {
	private Flotation() {
	}

	/** Fines slurry at 35 % solids, at 50 t/h of solids: about 2 kg a tick. */
	static final double FEED_PER_TICK = PlantRates.KG_PER_TICK / PlantRates.OVERFLOW_SOLIDS;
	/** A few seconds of flow, held at each inlet and outlet. */
	static final double HOLD = PlantRates.SLURRY_HOLD_SECONDS * 20 * FEED_PER_TICK;
	/** The bank's concentrate slurry, 30 % solids (proposed). */
	static final double CONCENTRATE_SOLIDS = 0.30;
	/** The thickener's underflow, 60 % solids (proposed in the spec). */
	static final double THICKENED_SOLIDS = 0.60;
	/** The pond holds about a day of the plant's tails (proposed): 3,400 t. */
	static final double POND_KG = 3_400_000;
	/** The filter's two output stacks. */
	static final int[] OUTPUT_SLOTS = {1, 2};

	/** The inlet: the north face of the part's front cells. */
	static List<Lines.Face> inlet(PlantBlockEntity part) {
		return part.faces(at -> at[2] == 0, Direction.NORTH);
	}

	/** Draws {@code product} off the pipe at the inlet into the part's buffer, up to {@link #HOLD}. */
	static void draw(PlantBlockEntity part, Product product) {
		double room = HOLD - part.back().held(product);
		if (room > 1e-6) {
			Slurry.Taken got = Slurry.take(part.getLevel(), inlet(part), product, Math.min(room, 4 * FEED_PER_TICK), part);
			if (got.kg() > 0) {
				part.back().offer(product, got.kg(), got.assay());
				part.setChanged();
			}
		}
	}

	/** Takes up to {@code max} kg of {@code product} out of the buffer, with its assay. */
	static Slurry.Taken use(PlantBlockEntity part, Product product, double max) {
		SlurryAssay assay = part.back().assayOf(product);
		double kg = part.back().give(product, max);
		return new Slurry.Taken(kg, assay);
	}

	static double outRoom(PlantBlockEntity part, Product product) {
		return Math.max(0, HOLD - part.out().held(product));
	}

	/** The south face of the back cells, for a cell at local x (or any x when {@code x} is negative). */
	static boolean backFace(PlantBlockEntity part, int[] at, Direction localSide, int x) {
		return localSide == Direction.SOUTH && at[2] == part.machine().depth() - 1 && at[1] == 0 && (x < 0 || at[0] == x);
	}

	static final Stage CONDITIONER = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return backFace(part, at, localSide, -1) ? Product.CONDITIONED_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			draw(part, Product.FINES_SLURRY);
			double waiting = part.back().held(Product.FINES_SLURRY);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double max = Math.min(Math.min(FEED_PER_TICK * part.strength(), waiting), outRoom(part, Product.CONDITIONED_SLURRY));
			if (max < 1e-6) {
				part.say(Crushing.NOWHERE + " (conditioned slurry on the pipe at its south side)");
				return;
			}
			SlurryAssay assay = part.back().assayOf(Product.FINES_SLURRY);
			boolean sphalerite = assay.get(Mineral.SPHALERITE.mineralItemId()) > 0;
			double tonnes = max * PlantRates.OVERFLOW_SOLIDS / 1000;
			if (!part.reagents().covers(tonnes, sphalerite)) {
				part.say("NO REAGENT: " + part.reagents().missing(tonnes, sphalerite));
				return;
			}
			part.reagents().consume(tonnes, sphalerite);
			part.trimReagents();
			Slurry.Taken taken = use(part, Product.FINES_SLURRY, max);
			part.out().offer(Product.CONDITIONED_SLURRY, taken.kg(), taken.assay());
			part.say("conditioning");
			part.setChanged();
		}
	};

	/** The bank: rougher, scavenger and cleaner cells; the floating minerals at the 75 % ladder to concentrate, the rest to tails. */
	static final Stage BANK = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			if (backFace(part, at, localSide, part.machine().width() - 1)) {
				return Product.CONCENTRATE_SLURRY;
			}
			return backFace(part, at, localSide, 0) ? Product.TAILS_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			draw(part, Product.CONDITIONED_SLURRY);
			double waiting = part.back().held(Product.CONDITIONED_SLURRY);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			PlantBlockEntity blower = part.touching(Machine.BLOWER);
			if (blower == null) {
				part.say("NOT BUILT: needs an Air Blower touching it");
				return;
			}
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			if (!blower.running()) {
				part.say("NO AIR: the blower is not running");
				return;
			}
			SlurryAssay feed = part.back().assayOf(Product.CONDITIONED_SLURRY);
			Split per = split(feed, 1);
			double max = Math.min(FEED_PER_TICK * part.strength(), waiting);
			if (per.concentrate() > 0) {
				max = Math.min(max, outRoom(part, Product.CONCENTRATE_SLURRY) / per.concentrate());
			}
			if (per.tails() > 0) {
				max = Math.min(max, outRoom(part, Product.TAILS_SLURRY) / per.tails());
			}
			if (max < 1e-6) {
				part.say(Crushing.NOWHERE + " (" + (outRoom(part, Product.TAILS_SLURRY) < 1e-6 ? "the tails" : "the concentrate") + " pipe is full)");
				return;
			}
			Slurry.Taken taken = use(part, Product.CONDITIONED_SLURRY, max);
			Split split = split(taken.assay(), taken.kg());
			part.out().offer(Product.CONCENTRATE_SLURRY, split.concentrate(), split.concentrateAssay());
			part.out().offer(Product.TAILS_SLURRY, split.tails(), split.tailsAssay());
			part.say("floating");
			part.setChanged();
		}
	};

	/** What {@code kg} of conditioned slurry assayed {@code feed} makes in the bank. */
	record Split(double concentrate, SlurryAssay concentrateAssay, double tails, SlurryAssay tailsAssay) {
	}

	/** The bank's split: each floating mineral's recovered share to a 30 % solids concentrate, the rest of the feed to tails. */
	static Split split(SlurryAssay feed, double kg) {
		double recovery = RecoveryRoute.FLOTATION.fraction().doubleValue();
		Map<String, Double> conc = new HashMap<>();
		Map<String, Double> tail = new HashMap<>();
		double solids = 0;
		for (String id : feed.minerals()) {
			double there = feed.get(id) * kg;
			Mineral mineral = Assays.mineral(id);
			double got = mineral != null && mineral.floats() ? there * recovery : 0;
			conc.put(id, got);
			tail.put(id, there - got);
			solids += got;
		}
		double concKg = Math.min(kg, solids / CONCENTRATE_SOLIDS);
		double tailKg = kg - concKg;
		conc.replaceAll((id, m) -> concKg > 0 ? m / concKg : 0);
		tail.replaceAll((id, m) -> tailKg > 0 ? m / tailKg : 0);
		return new Split(concKg, SlurryAssay.of(conc), tailKg, SlurryAssay.of(tail));
	}

	/** The blower runs while it touches a bank that has feed; the bank reads it. */
	static final Stage BLOWER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			if (part.touching(Machine.FLOTATION_BANK) == null && part.touching(Machine.SCAVENGER_BANK) == null) {
				part.say("NOT BUILT: needs a Flotation Bank or Scavenger Bank touching it");
				return;
			}
			part.want();
			part.say(part.running() ? "blowing air to the bank" : "waiting for its motor");
		}
	};

	/** The thickener: concentrate slurry in, thickened concentrate at 60 % solids out; the overflow water is left out. */
	static final Stage THICKENER = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return backFace(part, at, localSide, -1) ? Product.THICKENED_CONCENTRATE : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			draw(part, Product.CONCENTRATE_SLURRY);
			double waiting = part.back().held(Product.CONCENTRATE_SLURRY);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double ratio = CONCENTRATE_SOLIDS / THICKENED_SOLIDS;
			double max = Math.min(Math.min(FEED_PER_TICK * part.strength(), waiting), outRoom(part, Product.THICKENED_CONCENTRATE) / ratio);
			if (max < 1e-6) {
				part.say(Crushing.NOWHERE + " (thickened concentrate on the pipe at its south side)");
				return;
			}
			Slurry.Taken taken = use(part, Product.CONCENTRATE_SLURRY, max);
			Map<String, Double> shares = new HashMap<>();
			for (String id : taken.assay().minerals()) {
				shares.put(id, taken.assay().get(id) / ratio);
			}
			part.out().offer(Product.THICKENED_CONCENTRATE, taken.kg() * ratio, SlurryAssay.of(shares));
			part.say("thickening");
			part.setChanged();
		}
	};

	/** The pressure filter: thickened concentrate in; its cake paid as nuggets, nine packed into the ladder's item. */
	static final Stage FILTER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			pay(part);
			draw(part, Product.THICKENED_CONCENTRATE);
			double waiting = part.back().held(Product.THICKENED_CONCENTRATE);
			if (waiting < 1e-6) {
				part.say(full(part) ? "FULL: take the concentrate" : "NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			if (full(part)) {
				part.say("FULL: take the concentrate");
				return;
			}
			Slurry.Taken taken = use(part, Product.THICKENED_CONCENTRATE, Math.min(FEED_PER_TICK * part.strength(), waiting));
			for (String id : taken.assay().minerals()) {
				Mineral mineral = Assays.mineral(id);
				if (mineral != null) {
					part.cake().add(mineral, taken.assay().get(id) * taken.kg());
				}
			}
			part.say("filtering");
			part.setChanged();
		}
	};

	/** Pays each whole nugget into the output, then packs nine of a mineral's nuggets into its item. */
	static void pay(PlantBlockEntity part) {
		for (Mineral mineral : part.cake().minerals()) {
			int nuggets = part.cake().takeNuggets(mineral);
			int left = nuggets;
			while (left > 0) {
				ItemStack one = Feeds.stack(mineral.nuggetId(), 1);
				if (one.isEmpty() || !part.emitTo(OUTPUT_SLOTS, one)) {
					break;
				}
				left--;
				pack(part, mineral);
			}
			if (left > 0) {
				part.cake().putBack(mineral, left);
			}
		}
	}

	/** Nine of a mineral's nuggets in the output become one of the item the ladder pays for it. */
	private static void pack(PlantBlockEntity part, Mineral mineral) {
		ItemStack nugget = Feeds.stack(mineral.nuggetId(), 1);
		ItemStack item = Feeds.stack(mineral.phaseOneProductId(), 1);
		if (item.isEmpty() || part.countIn(OUTPUT_SLOTS, nugget) < 9) {
			return;
		}
		part.removeFrom(OUTPUT_SLOTS, nugget, 9);
		if (!part.emitTo(OUTPUT_SLOTS, item)) {
			part.emitTo(OUTPUT_SLOTS, Feeds.stack(mineral.nuggetId(), 9));
		}
	}

	/** True when the cake has a nugget waiting that the output cannot take. */
	private static boolean full(PlantBlockEntity part) {
		for (Mineral mineral : part.cake().minerals()) {
			if (part.cake().held(mineral) >= mineral.nuggetAmount().doubleValue()) {
				return true;
			}
		}
		return false;
	}

	/** The tailings pond: takes tails slurry off any pipe that touches it, until it is full. */
	static final Stage POND = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			double held = part.back().held(Product.TAILS_SLURRY);
			double room = POND_KG - held;
			if (room < 1) {
				part.say(String.format("FULL: %.0f t of tails", held / 1000));
				return;
			}
			List<Lines.Face> faces = part.side(Direction.NORTH);
			faces = new java.util.ArrayList<>(faces);
			for (Direction side : Direction.Plane.HORIZONTAL) {
				if (side != Direction.NORTH) {
					faces.addAll(part.side(side));
				}
			}
			Slurry.Taken got = Slurry.take(part.getLevel(), faces, Product.TAILS_SLURRY, Math.min(room, 10 * FEED_PER_TICK), part);
			if (got.kg() > 0) {
				part.back().offer(Product.TAILS_SLURRY, got.kg(), got.assay());
				part.setChanged();
			}
			part.say(String.format("holds %.1f of %.0f t of tails", part.back().held(Product.TAILS_SLURRY) / 1000, POND_KG / 1000));
		}
	};
}
