package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.block.Slurry;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Assays;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * The wet end of the crushing circuit (docs/multiblocks/crushing-circuit.md): the ball mill and its cyclone cluster, in closed
 * circuit at a 250 % circulating load. From here on the ore is slurry on Wildcatter's steel pipe, carrying its assay.
 */
final class Grinding {
	private Grinding() {
	}

	/** Solids through the mill each tick at full power: new feed plus the circulating load. */
	static final double MILL_KG_PER_TICK = PlantRates.KG_PER_TICK * (1 + PlantRates.CIRCULATING_LOAD);
	/** The mill's own water, 10 s of its use (proposed). */
	static final double MILL_WATER = 10 * PlantRates.KG_PER_SECOND * PlantRates.waterPerKg(PlantRates.MILL_SOLIDS);
	/** Water the cyclones add to bring the overflow to 35 % solids, per kg of overflow solids. */
	static final double CYCLONE_WATER_PER_KG = PlantRates.waterPerKg(PlantRates.OVERFLOW_SOLIDS) - PlantRates.waterPerKg(PlantRates.MILL_SOLIDS);
	static final double CYCLONE_WATER = 10 * PlantRates.KG_PER_SECOND * CYCLONE_WATER_PER_KG;
	/** Slurry held at the mill's outlet and the cyclones' overflow: a few seconds of flow. */
	static final double DISCHARGE_HOLD = PlantRates.SLURRY_HOLD_SECONDS * 20 * MILL_KG_PER_TICK / PlantRates.MILL_SOLIDS;
	static final double OVERFLOW_HOLD = PlantRates.SLURRY_HOLD_SECONDS * PlantRates.KG_PER_SECOND / PlantRates.OVERFLOW_SOLIDS;
	/** The underflow waiting in the mill, dry kg. */
	static final double UNDERFLOW_HOLD = PlantRates.SLURRY_HOLD_SECONDS * 20 * PlantRates.KG_PER_TICK * PlantRates.CIRCULATING_LOAD;

	/** The ball mill: sand in by belt, water off a pump, the cyclones' underflow first; mill discharge out on pipe. */
	static final Stage MILL = new Stage() {
		@Override
		public boolean accepts(Rock rock, @Nullable SizeClass size) {
			return size == SizeClass.SAND;
		}

		@Override
		public double waterHold() {
			return MILL_WATER;
		}

		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return at[2] == part.machine().depth() - 1 && at[1] == 0 && localSide == Direction.SOUTH ? Product.MILL_DISCHARGE : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			double returned = part.back().held(Product.MILL_DISCHARGE);
			if (part.hold().isEmpty() && returned < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double room = (DISCHARGE_HOLD - part.out().held(Product.MILL_DISCHARGE)) * PlantRates.MILL_SOLIDS;
			if (room < 1e-6) {
				part.say(Crushing.NOWHERE + " (mill discharge on the pipe at its south end)");
				return;
			}
			double budget = Math.min(MILL_KG_PER_TICK * part.strength(), room);
			double fromBack = Math.min(returned, budget);
			SlurryAssay backAssay = part.back().assayOf(Product.MILL_DISCHARGE);
			part.back().give(Product.MILL_DISCHARGE, fromBack);
			double waterPerKg = PlantRates.waterPerKg(PlantRates.MILL_SOLIDS);
			double fresh = Math.min(Math.min(part.hold().total(), budget - fromBack), part.water() / waterPerKg);
			RockLoad sand = part.hold().take(fresh);
			part.useWater(sand.total() * waterPerKg);
			double solids = fromBack + sand.total();
			if (solids < 1e-6) {
				part.say(part.water() < 1 ? "NO WATER" : "NO FEED");
				return;
			}
			SlurryAssay dry = SlurryAssay.blend(backAssay, fromBack, Assays.dry(sand), sand.total());
			part.out().offer(Product.MILL_DISCHARGE, solids / PlantRates.MILL_SOLIDS, Assays.wet(dry, PlantRates.MILL_SOLIDS));
			part.say(part.water() < 1 && !part.hold().isEmpty() ? "NO WATER" : "grinding " + Crushing.main(sand));
			part.setChanged();
		}
	};

	/**
	 * The cyclone cluster: mill discharge in on pipe at its north face, water off a pump; it splits at 150 micrometres, fines slurry
	 * out on pipe at its south face, the underflow back into the ball mill it touches.
	 */
	static final Stage CYCLONE = new Stage() {
		@Override
		public double waterHold() {
			return CYCLONE_WATER;
		}

		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return at[2] == part.machine().depth() - 1 && localSide == Direction.SOUTH ? Product.FINES_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			PlantBlockEntity mill = part.touching(Machine.BALL_MILL);
			if (mill == null) {
				part.say("NOT BUILT: needs a Ball Mill touching it, for the underflow");
				return;
			}
			var inlet = part.faces(at -> at[2] == 0, Direction.NORTH);
			double waiting = Slurry.available(part.getLevel(), inlet, Product.MILL_DISCHARGE, part);
			if (waiting < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double overShare = 1 / (1 + PlantRates.CIRCULATING_LOAD);
			// the most solids the overflow pipe, the water and the mill's room for underflow allow
			double maxSolids = MILL_KG_PER_TICK * part.strength();
			maxSolids = Math.min(maxSolids, (OVERFLOW_HOLD - part.out().held(Product.FINES_SLURRY)) * PlantRates.OVERFLOW_SOLIDS / overShare);
			maxSolids = Math.min(maxSolids, part.water() / CYCLONE_WATER_PER_KG / overShare);
			maxSolids = Math.min(maxSolids, (UNDERFLOW_HOLD - mill.back().held(Product.MILL_DISCHARGE)) / (1 - overShare));
			if (maxSolids < 1e-6) {
				part.say(part.water() < 1 ? "NO WATER" : Crushing.NOWHERE + " (fines slurry on the pipe at its south face)");
				return;
			}
			Slurry.Taken taken = Slurry.take(part.getLevel(), inlet, Product.MILL_DISCHARGE, maxSolids / PlantRates.MILL_SOLIDS, part);
			double solids = taken.kg() * PlantRates.MILL_SOLIDS;
			if (solids < 1e-6) {
				part.say("NO FEED");
				return;
			}
			SlurryAssay dry = unwet(taken.assay(), PlantRates.MILL_SOLIDS);
			double over = solids * overShare;
			part.useWater(over * CYCLONE_WATER_PER_KG);
			part.out().offer(Product.FINES_SLURRY, over / PlantRates.OVERFLOW_SOLIDS, Assays.wet(dry, PlantRates.OVERFLOW_SOLIDS));
			mill.back().offer(Product.MILL_DISCHARGE, solids - over, dry);
			mill.setChanged();
			part.say("classifying at 150 micrometres");
			part.setChanged();
		}
	};

	/** A wet assay back to the dry kg per kg of solids. */
	static SlurryAssay unwet(SlurryAssay wet, double solids) {
		Map<String, Double> shares = new HashMap<>();
		for (String m : wet.minerals()) {
			shares.put(m, wet.get(m) / solids);
		}
		return SlurryAssay.of(shares);
	}
}
