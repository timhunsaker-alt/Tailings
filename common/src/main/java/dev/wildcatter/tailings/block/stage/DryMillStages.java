package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.block.FluidNode;
import dev.wildcatter.block.Lines;
import dev.wildcatter.block.Slurry;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.item.HeavyMineralConcentrateItem;
import dev.wildcatter.tailings.plant.DryMill;
import dev.wildcatter.tailings.plant.HeavyMinerals;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * The dry mill (docs/multiblocks/dry-mill.md, L6): the scavenger spiral bank on the spirals' tails, the fluid-bed dryer and the three
 * separators. The dryer and separators hand the dry HMC to the next part touching them, as a bucket elevator between real machines
 * would; each separator pays its own minerals at the top rung's rate into its four output stacks.
 */
public final class DryMillStages {
	private DryMillStages() {
	}

	/** A separator's output stacks: a hopper under it or a hand takes them. */
	public static final int[] OUTPUT_SLOTS = {1, 2, 3, 4};
	/** The gases a dryer's burner fires on, as Wildcatter's gas burners do: any natural gas on its pipe. */
	private static final List<Product> GASES = List.of(Product.PIPELINE_GAS, Product.DRIED_GAS, Product.GAS, Product.COMPRESSED_GAS);

	/** The scavenger spirals: the spirals' tails in, another 45 % of each heavy mineral to HMC, the rest on as fines slurry to flotation. */
	static final Stage SCAVENGER_SPIRALS = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			if (Flotation.backFace(part, at, localSide, part.machine().width() - 1)) {
				return Product.CONCENTRATE_SLURRY;
			}
			return Flotation.backFace(part, at, localSide, 0) ? Product.FINES_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			Flotation.draw(part, Product.FINES_SLURRY);
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
			DryMill.Split per = DryMill.scavenge(part.back().assayOf(Product.FINES_SLURRY), 1, Flotation.CONCENTRATE_SOLIDS);
			double max = Math.min(Flotation.FEED_PER_TICK * part.strength(), waiting);
			if (per.concentrate() > 0) {
				max = Math.min(max, Flotation.outRoom(part, Product.CONCENTRATE_SLURRY) / per.concentrate());
			}
			if (per.rest() > 0) {
				max = Math.min(max, Flotation.outRoom(part, Product.FINES_SLURRY) / per.rest());
			}
			if (max < 1e-6) {
				part.say(Crushing.NOWHERE + " (" + (Flotation.outRoom(part, Product.FINES_SLURRY) < 1e-6 ? "the fines" : "the concentrate") + " pipe is full)");
				return;
			}
			Slurry.Taken taken = Flotation.use(part, Product.FINES_SLURRY, max);
			DryMill.Split split = DryMill.scavenge(taken.assay(), taken.kg(), Flotation.CONCENTRATE_SOLIDS);
			part.out().offer(Product.CONCENTRATE_SLURRY, split.concentrate(), split.concentrateAssay());
			part.out().offer(Product.FINES_SLURRY, split.rest(), split.restAssay());
			part.say("scavenging on the spirals");
			part.setChanged();
		}
	};

	/**
	 * The pressure filter's HMC: into a fluid-bed dryer touching the filter while its bin has room, else bagged 100 kg at a time into the
	 * filter's output, its assay kept (L6-2). A filter with no HMC (every plant without scavenger spirals) does nothing here.
	 */
	static void passHmc(PlantBlockEntity filter) {
		HeavyMinerals store = filter.hmc();
		if (store.isEmpty()) {
			return;
		}
		PlantBlockEntity dryer = filter.touching(Machine.FLUID_BED_DRYER);
		if (dryer != null) {
			double room = DryMill.HOLD_KG - dryer.hmc().total();
			if (room > 1e-9) {
				dryer.hmc().add(store.take(room));
				dryer.setChanged();
				filter.setChanged();
			}
		}
		while (store.total() >= DryMill.ITEM_KG - 1e-6) {
			HeavyMinerals before = new HeavyMinerals();
			before.add(store);
			Map<Mineral, Integer> bag = DryMill.bag(store);
			if (bag == null) {
				break;
			}
			if (!filter.emitTo(Flotation.OUTPUT_SLOTS, HeavyMineralConcentrateItem.of(TailingsItems.HEAVY_MINERAL_CONCENTRATE, bag))) {
				store.clear();
				store.add(before);
				break;
			}
			filter.setChanged();
		}
	}

	/** The fluid-bed dryer: moist HMC in by hopper, by hand or from the filter it touches; dried on gas and passed to the roll magnet. */
	static final Stage DRYER = new Stage() {
		@Override
		public void tick(PlantBlockEntity part) {
			if (part.hmc().isEmpty()) {
				part.say("NO FEED: heavy mineral concentrate from the filter or by hopper");
				return;
			}
			part.want();
			PlantBlockEntity next = part.touching(Machine.RER_SEPARATOR);
			if (next == null) {
				part.say(Crushing.NOWHERE + " (needs a Rare-Earth Roll Magnet touching it)");
				return;
			}
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double max = Math.min(Math.min(DryMill.KG_PER_TICK * part.strength() * part.fast(), part.hmc().total()), DryMill.HOLD_KG - next.hmc().total());
			if (max < 1e-9) {
				part.say(Crushing.NOWHERE + " (the roll magnet's hopper is full)");
				return;
			}
			double wanted = DryMill.gasM3(max, dev.wildcatter.Tuning.GAS_JOULES_PER_M3);
			double got = 0;
			List<Lines.Face> east = part.side(Direction.EAST);
			for (Product gas : GASES) {
				if (got < wanted) {
					got += Lines.pull(part.getLevel(), east, gas, wanted - got, FluidNode.Pull.MACHINE, part);
				}
			}
			if (got < wanted * 0.5) {
				part.say("NO GAS: pipeline gas on the pipe at its east side");
				return;
			}
			next.hmc().add(part.hmc().take(max * Math.min(1, got / wanted)));
			part.gasBurnt(got);
			next.setChanged();
			part.setChanged();
			part.say("drying at 120 °C");
		}
	};

	/** A separator: dry HMC from the part before it; its own minerals paid by the nuggets rule, the rest to the next separator it touches. */
	private static Stage separator(Machine self, String doing) {
		return new Stage() {
			@Override
			public void tick(PlantBlockEntity part) {
				Flotation.pay(part, OUTPUT_SLOTS);
				if (part.hmc().isEmpty()) {
					part.say(full(part) ? "FULL: take the products" : "NO FEED");
					return;
				}
				part.want();
				if (!part.running()) {
					part.say("waiting for its motor");
					return;
				}
				if (full(part)) {
					part.say("FULL: take the products");
					return;
				}
				double max = Math.min(DryMill.KG_PER_TICK * part.strength() * part.fast(), part.hmc().total());
				double rejected = DryMill.separate(self, part.hmc()).rest().total() / part.hmc().total();
				Machine nextKind = DryMill.next(self);
				PlantBlockEntity next = nextKind == null ? null : part.touching(nextKind);
				if (rejected > 1e-9) {
					if (next == null) {
						part.say(Crushing.NOWHERE + " (needs " + (nextKind == null ? "somewhere" : "a " + nextKind.displayName()) + " touching it for what it rejects)");
						return;
					}
					max = Math.min(max, (DryMill.HOLD_KG - next.hmc().total()) / rejected);
				}
				if (max < 1e-9) {
					part.say(Crushing.NOWHERE + " (the next separator's hopper is full)");
					return;
				}
				DryMill.Pass pass = DryMill.separate(self, part.hmc().take(max));
				for (Mineral mineral : pass.product().minerals()) {
					part.cake().addKg(mineral, pass.product().kg(mineral) * DryMill.pay(mineral));
				}
				if (next != null && !pass.rest().isEmpty()) {
					next.hmc().add(pass.rest());
					next.setChanged();
				}
				part.say(doing);
				part.setChanged();
			}
		};
	}

	private static boolean full(PlantBlockEntity part) {
		for (Mineral mineral : part.cake().minerals()) {
			if (part.cake().held(mineral) >= mineral.nuggetAmount().doubleValue()) {
				return true;
			}
		}
		return false;
	}

	static final Stage RER = separator(Machine.RER_SEPARATOR, "taking the ilmenite on the magnet");
	static final Stage HT = separator(Machine.HT_ROLL_SEPARATOR, "taking the conductors on the high-tension roll");
	static final Stage IRM = separator(Machine.IRM_SEPARATOR, "taking the monazite; the zircon passes");
}
