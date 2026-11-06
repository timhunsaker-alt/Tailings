package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.block.Slurry;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Assays;
import dev.wildcatter.tailings.recovery.RecoveryRoute;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * The spiral bank (docs/multiblocks/gravity-circuit.md): fines slurry in, the heavy minerals that do not float to concentrate at the
 * ladder's 50 %, and the rest on as fines slurry to flotation.
 */
final class Gravity {
	private Gravity() {
	}

	/** What {@code kg} of fines slurry assayed {@code feed} makes on the spirals. */
	record Split(double concentrate, SlurryAssay concentrateAssay, double rest, SlurryAssay restAssay) {
	}

	static Split split(SlurryAssay feed, double kg) {
		double recovery = RecoveryRoute.GRAVITY_OR_MAGNETIC.fraction().doubleValue();
		Map<String, Double> conc = new HashMap<>();
		Map<String, Double> rest = new HashMap<>();
		double solids = 0;
		for (String id : feed.minerals()) {
			double there = feed.get(id) * kg;
			Mineral mineral = Assays.mineral(id);
			double got = mineral != null && mineral.spirals() ? there * recovery : 0;
			conc.put(id, got);
			rest.put(id, there - got);
			solids += got;
		}
		double concKg = Math.min(kg, solids / Flotation.CONCENTRATE_SOLIDS);
		double restKg = kg - concKg;
		conc.replaceAll((id, m) -> concKg > 0 ? m / concKg : 0);
		rest.replaceAll((id, m) -> restKg > 0 ? m / restKg : 0);
		return new Split(concKg, SlurryAssay.of(conc), restKg, SlurryAssay.of(rest));
	}

	static final Stage SPIRALS = new Stage() {
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
			Split per = split(part.back().assayOf(Product.FINES_SLURRY), 1);
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
			Split split = split(taken.assay(), taken.kg());
			part.out().offer(Product.CONCENTRATE_SLURRY, split.concentrate(), split.concentrateAssay());
			part.out().offer(Product.FINES_SLURRY, split.rest(), split.restAssay());
			part.say("on the spirals");
			part.setChanged();
		}
	};
}
