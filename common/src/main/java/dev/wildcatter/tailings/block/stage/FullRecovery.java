package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.block.Lines;
import dev.wildcatter.block.FluidNode;
import dev.wildcatter.block.Slurry;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Assays;
import dev.wildcatter.tailings.plant.FullRecoveryRates;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Phase 4 (docs/multiblocks/full-recovery.md): scavenging, regrinding and cleaning the bank's tails, leaching what is left, washing
 * the leach, and winning the metal from the pregnant liquor. Each part follows the flotation parts' way of working.
 */
final class FullRecovery {
	private FullRecovery() {
	}

	/** Splits {@code kg} of feed by a share per mineral: the taken share to {@code solids}-percent product, the rest stays in the rest. */
	record Split(double taken, SlurryAssay takenAssay, double rest, SlurryAssay restAssay) {
	}

	static Split split(SlurryAssay feed, double kg, Function<Mineral, Double> share, double solids) {
		Map<String, Double> got = new HashMap<>();
		Map<String, Double> left = new HashMap<>();
		double mass = 0;
		for (String id : feed.minerals()) {
			double there = feed.get(id) * kg;
			Mineral mineral = Assays.mineral(id);
			double take = mineral == null ? 0 : there * share.apply(mineral);
			got.put(id, take);
			left.put(id, there - take);
			mass += take;
		}
		double takenKg = Math.min(kg, mass / solids);
		double restKg = kg - takenKg;
		got.replaceAll((id, m) -> takenKg > 0 ? m / takenKg : 0);
		left.replaceAll((id, m) -> restKg > 0 ? m / restKg : 0);
		return new Split(takenKg, SlurryAssay.of(got), restKg, SlurryAssay.of(left));
	}

	/** A part that draws one product and passes all of it on as another, for as long as its motor and outlet allow. */
	private static Stage pass(Product in, Product out, double rate, String doing) {
		return new Stage() {
			@Override
			public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
				return Flotation.backFace(part, at, localSide, -1) ? out : null;
			}

			@Override
			public void tick(PlantBlockEntity part) {
				Flotation.draw(part, in);
				double waiting = part.back().held(in);
				if (waiting < 1e-6) {
					part.say("NO FEED");
					return;
				}
				part.want();
				if (!part.running()) {
					part.say("waiting for its motor");
					return;
				}
				double max = Math.min(Math.min(rate * part.strength(), waiting), Flotation.outRoom(part, out));
				if (max < 1e-6) {
					part.say(Crushing.NOWHERE + " (" + out.label() + " on the pipe at its south side)");
					return;
				}
				Slurry.Taken taken = Flotation.use(part, in, max);
				part.out().offer(out, taken.kg(), taken.assay());
				part.say(doing);
				part.setChanged();
			}
		};
	}

	/** The scavengers: the bank's tails in, what still floats on as a poor concentrate, the rest as tails. Air off a blower it touches. */
	static final Stage SCAVENGER = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			if (Flotation.backFace(part, at, localSide, part.machine().width() - 1)) {
				return Product.CONCENTRATE_SLURRY;
			}
			return Flotation.backFace(part, at, localSide, 0) ? Product.TAILS_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			Flotation.draw(part, Product.TAILS_SLURRY);
			double waiting = part.back().held(Product.TAILS_SLURRY);
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
			Split per = split(part.back().assayOf(Product.TAILS_SLURRY), 1, FullRecoveryRates::scavengerShare, Flotation.CONCENTRATE_SOLIDS);
			double max = Math.min(Flotation.FEED_PER_TICK * part.strength(), waiting);
			if (per.taken() > 0) {
				max = Math.min(max, Flotation.outRoom(part, Product.CONCENTRATE_SLURRY) / per.taken());
			}
			if (per.rest() > 0) {
				max = Math.min(max, Flotation.outRoom(part, Product.TAILS_SLURRY) / per.rest());
			}
			if (max < 1e-6) {
				part.say(Crushing.NOWHERE + " (a full pipe at its south side)");
				return;
			}
			Slurry.Taken taken = Flotation.use(part, Product.TAILS_SLURRY, max);
			Split split = split(taken.assay(), taken.kg(), FullRecoveryRates::scavengerShare, Flotation.CONCENTRATE_SOLIDS);
			part.out().offer(Product.CONCENTRATE_SLURRY, split.taken(), split.takenAssay());
			part.out().offer(Product.TAILS_SLURRY, split.rest(), split.restAssay());
			part.say("scavenging");
			part.setChanged();
		}
	};

	/** The regrind mill frees what the scavengers floated; its product is conditioned slurry for the cleaners. */
	static final Stage REGRIND = pass(Product.CONCENTRATE_SLURRY, Product.CONDITIONED_SLURRY, Flotation.FEED_PER_TICK, "regrinding");
	/** The cleaners: all of the reground slurry on as concentrate for the thickener. Their own tails are left out. */
	static final Stage CLEANER = pass(Product.CONDITIONED_SLURRY, Product.CONCENTRATE_SLURRY, Flotation.FEED_PER_TICK, "cleaning");

	/** The leach tank: tails and acid in, leach slurry out. NO ACID stops it. */
	static final Stage LEACH = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return Flotation.backFace(part, at, localSide, -1) ? Product.LEACH_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			Flotation.draw(part, Product.TAILS_SLURRY);
			double waiting = part.back().held(Product.TAILS_SLURRY);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double max = Math.min(Math.min(Flotation.FEED_PER_TICK * part.strength(), waiting), Flotation.outRoom(part, Product.LEACH_SLURRY));
			if (max < 1e-6) {
				part.say(Crushing.NOWHERE + " (leach slurry on the pipe at its south side)");
				return;
			}
			double wanted = FullRecoveryRates.acidLitres(max);
			Level level = part.getLevel();
			List<Lines.Face> east = part.side(Direction.EAST);
			double got = Lines.pull(level, east, Product.SULFURIC_ACID, wanted, FluidNode.Pull.MACHINE, part);
			if (got < wanted * 0.5) {
				part.say("NO ACID: sulfuric acid on the pipe at its east side");
				return;
			}
			max *= Math.min(1, got / wanted);
			Slurry.Taken taken = Flotation.use(part, Product.TAILS_SLURRY, max);
			part.out().offer(Product.LEACH_SLURRY, taken.kg(), taken.assay());
			part.say("leaching");
			part.setChanged();
		}
	};

	/** The CCD thickener: leach slurry in; what dissolved to pregnant liquor at its east corner, the washed solids to tails at its west. */
	static final Stage WASH = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			if (Flotation.backFace(part, at, localSide, part.machine().width() - 1)) {
				return Product.PREGNANT_LIQUOR;
			}
			return Flotation.backFace(part, at, localSide, 0) ? Product.TAILS_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			Flotation.draw(part, Product.LEACH_SLURRY);
			double waiting = part.back().held(Product.LEACH_SLURRY);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			Split per = split(part.back().assayOf(Product.LEACH_SLURRY), 1, FullRecoveryRates::leachShare, FullRecoveryRates.LIQUOR_STRENGTH);
			double max = Math.min(Flotation.FEED_PER_TICK * part.strength(), waiting);
			if (per.taken() > 0) {
				max = Math.min(max, Flotation.outRoom(part, Product.PREGNANT_LIQUOR) / per.taken());
			}
			if (per.rest() > 0) {
				max = Math.min(max, Flotation.outRoom(part, Product.TAILS_SLURRY) / per.rest());
			}
			if (max < 1e-6) {
				part.say(Crushing.NOWHERE + " (a full pipe at its south side)");
				return;
			}
			Slurry.Taken taken = Flotation.use(part, Product.LEACH_SLURRY, max);
			Split split = split(taken.assay(), taken.kg(), FullRecoveryRates::leachShare, FullRecoveryRates.LIQUOR_STRENGTH);
			part.out().offer(Product.PREGNANT_LIQUOR, split.taken(), split.takenAssay());
			part.out().offer(Product.TAILS_SLURRY, split.rest(), split.restAssay());
			part.say("washing");
			part.setChanged();
		}
	};

	/** The electrowinning bank: pregnant liquor in, metal paid by the nuggets rule, as the filter pays. */
	static final Stage ELECTROWIN = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			Flotation.pay(part);
			Flotation.draw(part, Product.PREGNANT_LIQUOR);
			double waiting = part.back().held(Product.PREGNANT_LIQUOR);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			Slurry.Taken taken = Flotation.use(part, Product.PREGNANT_LIQUOR, Math.min(Flotation.FEED_PER_TICK * part.strength(), waiting));
			for (String id : taken.assay().minerals()) {
				Mineral mineral = Assays.mineral(id);
				if (mineral != null) {
					part.cake().addKg(mineral, taken.assay().get(id) * taken.kg() * FullRecoveryRates.topRungPay(mineral));
				}
			}
			part.say("plating");
			part.setChanged();
		}
	};

	/** Ticks to work one item of concentrate (proposed): 20 s. */
	static final int BYPRODUCT_TICKS = 400;
	private static final int[] OUTPUTS = Flotation.OUTPUT_SLOTS;

	/** The by-product plant: takes a concentrate's item, gives it back, and pays what it carries. */
	static final Stage BYPRODUCTS = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			net.minecraft.world.item.ItemStack feed = part.getItem(0);
			if (feed.isEmpty()) {
				part.say("NO FEED: put in zinc, lead, molybdenite, pentlandite or monazite");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			if (part.working(1) < BYPRODUCT_TICKS) {
				part.say("recovering by-products");
				return;
			}
			String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(feed.getItem()).toString();
			Mineral mineral = dev.wildcatter.tailings.plant.ByProducts.byItem(id);
			net.minecraft.world.item.ItemStack back = feed.copyWithCount(1);
			if (mineral == null || !part.emitTo(OUTPUTS, back)) {
				part.say("FULL: take what it has made");
				part.working(-1);
				return;
			}
			feed.shrink(1);
			part.working(-1);
			for (dev.wildcatter.tailings.plant.ByProducts.Share share : dev.wildcatter.tailings.plant.ByProducts.of(mineral)) {
				double kg = part.byproducts().getOrDefault(share.itemId(), 0.0) + mineral.itemAmount().doubleValue() * share.grade();
				while (kg >= dev.wildcatter.tailings.plant.ByProducts.ITEM_KG - 1e-6
						&& part.emitTo(OUTPUTS, dev.wildcatter.tailings.block.Feeds.stack(share.itemId(), 1))) {
					kg -= dev.wildcatter.tailings.plant.ByProducts.ITEM_KG;
				}
				part.byproducts().put(share.itemId(), kg);
			}
			part.setChanged();
		}
	};

	/** The heap's rate: a tenth of the plant's (proposed), a heap being slow. */
	static final double HEAP_KG_PER_TICK = PlantRates.KG_PER_TICK / 10;

	/** The heap pad: waste rock by belt, acid from its east side, the rock's trace minerals to pregnant liquor at the ladder's 50 %. */
	static final Stage HEAP = new Stage() {
		@Override
		public boolean accepts(dev.wildcatter.tailings.rock.Rock rock, dev.wildcatter.tailings.rock.@Nullable SizeClass size) {
			return size == null;
		}

		@Override
		public double holdKg() {
			return 100 * PlantRates.BLOCK_KG;
		}

		/** Waste rock by a belt over it, tipped onto the pad. */
		@Override
		public boolean feeds(PlantBlockEntity part, int[] at, Direction localSide) {
			return Stage.top(part, at, localSide);
		}

		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return Flotation.backFace(part, at, localSide, -1) ? Product.PREGNANT_LIQUOR : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			if (part.hold().isEmpty()) {
				part.say("NO FEED: waste rock by belt");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double max = Math.min(HEAP_KG_PER_TICK * part.strength(), part.hold().total());
			double wanted = FullRecoveryRates.ACID_LITRES_PER_TONNE * max / 1000;
			double got = Lines.pull(part.getLevel(), part.side(Direction.EAST), Product.SULFURIC_ACID, wanted, FluidNode.Pull.MACHINE, part);
			if (got < wanted * 0.5) {
				part.say("NO ACID: sulfuric acid on the pipe at its east side");
				return;
			}
			if (Flotation.outRoom(part, Product.PREGNANT_LIQUOR) < 1) {
				part.say(Crushing.NOWHERE + " (pregnant liquor on the pipe at its south side)");
				return;
			}
			dev.wildcatter.tailings.plant.RockLoad taken = part.hold().take(max);
			SlurryAssay dry = Assays.dry(taken);
			Map<String, Double> shares = new HashMap<>();
			for (String id : dry.minerals()) {
				Mineral mineral = Assays.mineral(id);
				// the cells pay the top rung's rate on what they plate; the heap's ladder is 50 %, so this is taken back here
				shares.put(id, mineral == null ? 0 : dry.get(id) * dev.wildcatter.tailings.recovery.RecoveryRoute.GRAVITY_OR_MAGNETIC.fraction().doubleValue() / FullRecoveryRates.topRungPay(mineral));
			}
			part.out().offer(Product.PREGNANT_LIQUOR, taken.total(), SlurryAssay.of(shares));
			part.say("leaching the heap");
			part.setChanged();
		}
	};
}
