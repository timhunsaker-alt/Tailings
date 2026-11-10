package dev.wildcatter.tailings.block.stage;

import dev.wildcatter.block.Lines;
import dev.wildcatter.block.Slurry;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Assays;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PitLine;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.jspecify.annotations.Nullable;

/**
 * The pit line's grinding (docs/multiblocks/sag-mill-line.md, P7b; decisions in docs/questions/p7-pit-line.md): an SABC circuit at
 * 1,000 t/h. The SAG mill grinds the stockpile's crushed rock, its trommel's undersize into the cyclone feed pump's box it touches and
 * its pebbles out of its east face onto a belt to the pebble crusher, which sends them back crushed. The pump sends the SAG's undersize
 * and the large ball mill's discharge to the large cyclone cluster, which splits at 150 µm at a 250 % circulating load: fines slurry out
 * on pipe, the underflow into the ball mill it touches. As the small plant's mill and cyclones, from the SAG on it is slurry with its assay.
 */
public final class SagLineStages {
	private SagLineStages() {
	}

	/** The pump box's SAG undersize, dry kg: 10 s of the cyclone feed. */
	static final double PUMP_BACK_HOLD = PitLine.SLURRY_HOLD_SECONDS * PitLine.CYCLONE_FEED_TPH / 3.6;
	/** The pump's outlet, wet kg: 10 s of the cyclone feed at its density. */
	static final double PUMP_OUT_HOLD = PUMP_BACK_HOLD / PitLine.cycloneFeedSolids();
	/** The ball mill's underflow waiting, dry kg, and its discharge, wet kg: 10 s each. */
	static final double BALL_BACK_HOLD = PitLine.SLURRY_HOLD_SECONDS * PitLine.BALL_MILL_TPH / 3.6;
	static final double BALL_OUT_HOLD = BALL_BACK_HOLD / PitLine.MILL_SOLIDS;
	/** The cluster's overflow, wet kg: 10 s of the line's fines slurry. */
	static final double CLUSTER_OUT_HOLD = PitLine.SLURRY_HOLD_SECONDS * PitLine.KG_PER_SECOND / PitLine.OVERFLOW_SOLIDS;
	/** The pump and the cluster are rated 20 % over the design cyclone feed: twelve cyclones with ten running (proposed). */
	static final double CYCLONE_RATED_TPH = PitLine.CYCLONE_FEED_TPH * 1.2;
	/** The SAG's water, 10 s of its use at its rate. */
	static final double SAG_WATER = PitLine.SLURRY_HOLD_SECONDS * PitLine.KG_PER_SECOND * PlantRates.waterPerKg(PitLine.SAG_SOLIDS);
	/** The pump box's water, 10 s of its use. */
	static final double PUMP_WATER = PitLine.SLURRY_HOLD_SECONDS * PitLine.CYCLONE_FEED_TPH / 3.6 * PitLine.pumpWaterPerKg(PitLine.MILL_SOLIDS);
	/** The cyclones' feed pressure at their design feed, kPa (proposed: 660 mm cyclones run at 70-100 kPa); it goes with the flow squared. */
	public static final double DESIGN_PRESSURE_KPA = 90;
	public static final double PRESSURE_SPAN_KPA = 200;

	/** The cyclones' feed pressure for a feed of {@code tph} of solids. */
	public static double pressureKpa(double tph) {
		double r = tph / PitLine.CYCLONE_FEED_TPH;
		return DESIGN_PRESSURE_KPA * r * r;
	}

	/** True if the SAG's pebble belt (beyond its east face) delivers into a pebble crusher that is crushing, not bypassed. */
	static boolean pebbleCrusherRunning(PlantBlockEntity sag) {
		for (Lines.Face face : sag.side(Direction.EAST)) {
			BlockPos at = face.pipe();
			if (sag.getLevel().getBlockState(at).getBlock() instanceof BeltConveyorBlock) {
				PlantBlockEntity head = PitLineStages.headOf(sag.getLevel(), at);
				return head != null && head.machine() == Machine.PEBBLE_CRUSHER && head.running();
			}
		}
		return false;
	}

	/**
	 * The SAG mill: crushed rock (and the pebble crusher's gravel) in by a heavy belt over its feed end, water off a water pipe. It grinds
	 * at 1,000 t/h times its pinions' output, less 15 % with no pebble crusher crushing its pebbles, and less while its charge is under its
	 * normal 83 t; 20 % of the crushed rock it grinds leaves as pebbles. Over its 35 % filling (about 120 t) it reads OVERLOAD.
	 */
	static final Stage SAG = new Stage() {
		@Override
		public boolean accepts(Rock rock, @Nullable SizeClass size) {
			return size == SizeClass.CRUSHED || size == SizeClass.GRAVEL;
		}

		@Override
		public boolean feeds(PlantBlockEntity part, int[] at, Direction localSide) {
			return Stage.feedEndTop(part, at, localSide);
		}

		@Override
		public double holdKg() {
			return PitLine.SAG_MOST_CHARGE_KG;
		}

		@Override
		public double waterHold() {
			return SAG_WATER;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			PlantBlockEntity pump = part.touching(Machine.CYCLONE_FEED_PUMP);
			if (pump == null) {
				part.say("NOT BUILT: needs a Cyclone Feed Pump touching it, for the trommel's undersize");
				return;
			}
			if (part.hold().isEmpty()) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motors");
				return;
			}
			if (part.scanDue() || !(part.run instanceof Boolean)) {
				part.run = pebbleCrusherRunning(part);
			}
			boolean pebbleCrusher = (Boolean) part.run;
			double charge = part.hold().total();
			double crushedShare = part.hold().total((r, s) -> s == SizeClass.CRUSHED) / charge;
			double pebbleShare = PitLine.PEBBLE_SHARE * crushedShare;
			double grind = PitLine.sagGrindKg(charge, part.strength(), pebbleCrusher) * part.fast();
			PlantBlockEntity.Sink pebbles = part.sink(part.side(Direction.EAST));
			if (pebbles == null && pebbleShare > 0) {
				part.say(Crushing.NOWHERE + " (a heavy belt on its east face for the pebbles)");
				return;
			}
			if (pebbles != null && pebbleShare > 0) {
				RockLoad probe = new RockLoad();
				part.hold().forEach((rock, size, kg) -> probe.add(rock, SizeClass.CRUSHED, kg));
				grind = Math.min(grind, pebbles.room(probe) / pebbleShare);
			}
			grind = Math.min(grind, (PUMP_BACK_HOLD - pump.back().held(Product.MILL_DISCHARGE)) / (1 - pebbleShare));
			double waterPerKg = PlantRates.waterPerKg(PitLine.SAG_SOLIDS);
			grind = Math.min(grind, part.water() / waterPerKg / (1 - pebbleShare));
			if (grind < 1e-6) {
				part.say(part.water() < 1 ? "NO WATER" : Crushing.NOWHERE + " (the pump box or the pebble belt is full)");
				return;
			}
			RockLoad taken = part.hold().take(grind);
			RockLoad under = new RockLoad();
			RockLoad pebble = new RockLoad();
			taken.forEach((rock, size, kg) -> {
				double out = size == SizeClass.CRUSHED ? kg * PitLine.PEBBLE_SHARE : 0;
				pebble.add(rock, SizeClass.CRUSHED, out);
				under.add(rock, SizeClass.SAND, kg - out);
			});
			if (!pebble.isEmpty() && pebble.total() > 1e-9) {
				pebbles.put(pebble);
			}
			part.useWater(under.total() * waterPerKg);
			pump.back().offer(Product.MILL_DISCHARGE, under.total(), Assays.dry(under));
			pump.setChanged();
			double tonnes = part.hold().total() / 1000;
			String grinding = String.format("grinding %s, charge %.0f t (%.0f %% filling)%s", Crushing.main(taken), tonnes, filling(part.hold().total()) * 100,
					pebbleCrusher ? "" : ", 15 % slower: no pebble crusher crushing its pebbles");
			part.say(part.hold().total() > PitLine.SAG_OVERLOAD_KG ? "OVERLOAD: " + grinding : grinding);
			part.setChanged();
		}
	};

	/** The SAG's filling, balls and rock, for a charge of {@code kg} of rock. */
	public static double filling(double kg) {
		return PitLine.SAG_BALLS + (PitLine.SAG_FILLING - PitLine.SAG_BALLS) * kg / PitLine.SAG_NORMAL_CHARGE_KG;
	}

	/**
	 * The pebble crusher: the SAG's pebbles in over its top, crushed to gravel at a 12 mm closed side and out of its south end onto a belt
	 * back to the SAG. With its motor stopped they go round it through the bypass chute, uncrushed: BYPASSED.
	 */
	static final Stage PEBBLE = new Stage() {
		@Override
		public boolean accepts(Rock rock, @Nullable SizeClass size) {
			return size == SizeClass.CRUSHED || size == SizeClass.GRAVEL;
		}

		@Override
		public boolean feeds(PlantBlockEntity part, int[] at, Direction localSide) {
			return Stage.top(part, at, localSide);
		}

		@Override
		public double holdKg() {
			return 2 * PlantRates.BLOCK_KG;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			PlantBlockEntity.Sink out = PitLineStages.discharge(part);
			if (out == null) {
				part.say(Crushing.NOWHERE + " (a belt at its south end, back to the SAG)");
				return;
			}
			if (part.hold().isEmpty()) {
				part.say("NO FEED");
				return;
			}
			part.want();
			boolean crushing = part.running();
			RockLoad probe = new RockLoad();
			part.hold().forEach((rock, size, kg) -> probe.add(rock, crushing ? SizeClass.GRAVEL : size, kg));
			double max = Math.min(PitLine.kgPerTick(PitLine.PEBBLE_CRUSHER_TPH) * (crushing ? part.strength() : 1) * part.fast(), out.room(probe));
			if (max < 1e-6) {
				part.say("FULL: its belt is full");
				return;
			}
			RockLoad taken = part.hold().take(max);
			RockLoad made = new RockLoad();
			taken.forEach((rock, size, kg) -> made.add(rock, crushing ? SizeClass.GRAVEL : size, kg));
			out.put(made);
			part.say(crushing ? "crushing pebbles of " + Crushing.main(taken) : "BYPASSED: its motor is not running; the pebbles go round it uncrushed");
			part.setChanged();
		}
	};

	/** The large ball mill: the cyclones' underflow in (by touching), ground and out on pipe at the south face of its last cells. */
	static final Stage BALL = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return at[2] == part.machine().depth() - 1 && at[1] == 0 && localSide == Direction.SOUTH ? Product.MILL_DISCHARGE : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			double returned = part.back().held(Product.MILL_DISCHARGE);
			if (returned < 1e-6) {
				part.say("NO FEED: the cyclones' underflow comes in by the cluster touching it");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motors");
				return;
			}
			double room = (BALL_OUT_HOLD - part.out().held(Product.MILL_DISCHARGE)) * PitLine.MILL_SOLIDS;
			double kg = Math.min(Math.min(PitLine.kgPerTick(PitLine.BALL_MILL_TPH) * part.strength() * part.fast(), room), returned);
			if (kg < 1e-6) {
				part.say(Crushing.NOWHERE + " (mill discharge on the pipe at its south end)");
				return;
			}
			SlurryAssay assay = part.back().assayOf(Product.MILL_DISCHARGE);
			part.back().give(Product.MILL_DISCHARGE, kg);
			part.out().offer(Product.MILL_DISCHARGE, kg / PitLine.MILL_SOLIDS, Assays.wet(assay, PitLine.MILL_SOLIDS));
			part.say(String.format("grinding the underflow, %.0f t waiting", part.back().held(Product.MILL_DISCHARGE) / 1000));
			part.setChanged();
		}
	};

	/**
	 * The cyclone feed pump: the SAG's undersize in its box (by touching), the ball mill's discharge in on pipe at its north face or either
	 * side, water
	 * off a water pipe to bring it to the cyclone feed's density; out on pipe at its south face.
	 */
	static final Stage PUMP = new Stage() {
		@Override
		public double waterHold() {
			return PUMP_WATER;
		}

		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return at[2] == part.machine().depth() - 1 && localSide == Direction.SOUTH ? Product.MILL_DISCHARGE : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			double sag = part.back().held(Product.MILL_DISCHARGE);
			// the ball mill's discharge on pipe at its north face, or either side where the SAG stands against its north face
			List<Lines.Face> inlet = new java.util.ArrayList<>(part.faces(at -> at[2] == 0, Direction.NORTH));
			inlet.addAll(part.side(Direction.EAST));
			inlet.addAll(part.side(Direction.WEST));
			double piped = Slurry.available(part.getLevel(), inlet, Product.MILL_DISCHARGE, part);
			if (sag < 1e-6 && piped < 1e-6) {
				part.say("NO FEED");
				return;
			}
			part.want();
			if (!part.running()) {
				part.say("waiting for its motor");
				return;
			}
			double feedSolids = PitLine.cycloneFeedSolids();
			double budget = PitLine.kgPerTick(CYCLONE_RATED_TPH) * part.strength() * part.fast();
			budget = Math.min(budget, (PUMP_OUT_HOLD - part.out().held(Product.MILL_DISCHARGE)) * feedSolids);
			double sagWater = PitLine.pumpWaterPerKg(PitLine.SAG_SOLIDS), millWater = PitLine.pumpWaterPerKg(PitLine.MILL_SOLIDS);
			double a = Math.min(sag, budget);
			double b = Math.min(piped * PitLine.MILL_SOLIDS, budget - a);
			double water = a * sagWater + b * millWater;
			if (water > part.water() && water > 0) {
				double scale = part.water() / water;
				a *= scale;
				b *= scale;
			}
			if (a + b < 1e-6) {
				part.say(part.water() < 1 ? "NO WATER" : Crushing.NOWHERE + " (the cyclone feed on the pipe at its south face)");
				return;
			}
			SlurryAssay fromSag = part.back().assayOf(Product.MILL_DISCHARGE);
			part.back().give(Product.MILL_DISCHARGE, a);
			SlurryAssay fromMill = SlurryAssay.EMPTY;
			double gotB = 0;
			if (b > 1e-9) {
				Slurry.Taken taken = Slurry.take(part.getLevel(), inlet, Product.MILL_DISCHARGE, b / PitLine.MILL_SOLIDS, part);
				gotB = taken.kg() * PitLine.MILL_SOLIDS;
				fromMill = Grinding.unwet(taken.assay(), PitLine.MILL_SOLIDS);
			}
			part.useWater(a * sagWater + gotB * millWater);
			SlurryAssay dry = SlurryAssay.blend(fromSag, a, fromMill, gotB);
			part.out().offer(Product.MILL_DISCHARGE, (a + gotB) / feedSolids, Assays.wet(dry, feedSolids));
			part.say(String.format("pumping %.0f t/h of cyclone feed at %.0f %% solids", (a + gotB) * 20 * 3.6 / part.fast(), feedSolids * 100));
			part.setChanged();
		}
	};

	/**
	 * The large cyclone cluster: the cyclone feed in on pipe at its north face; it splits at 150 µm, a share 1 / 3.5 of the solids to
	 * the overflow at 35 % solids, fines slurry out on pipe at its south face, the rest into the ball mill it touches at the mill's 70 %.
	 */
	static final Stage CLUSTER = new Stage() {
		@Override
		public @Nullable Product outlet(PlantBlockEntity part, int[] at, Direction localSide) {
			return at[2] == part.machine().depth() - 1 && localSide == Direction.SOUTH ? Product.FINES_SLURRY : null;
		}

		@Override
		public void tick(PlantBlockEntity part) {
			if (part.getLevel().getGameTime() % 20 == 0) {
				part.pit().second();
			}
			PlantBlockEntity mill = part.touching(Machine.BALL_MILL_LARGE);
			if (mill == null) {
				part.say("NOT BUILT: needs a Large Ball Mill touching it, for the underflow");
				return;
			}
			List<Lines.Face> inlet = part.faces(at -> at[2] == 0, Direction.NORTH);
			if (Slurry.available(part.getLevel(), inlet, Product.MILL_DISCHARGE, part) < 1e-6) {
				part.say("NO FEED");
				return;
			}
			double feedSolids = PitLine.cycloneFeedSolids();
			double overShare = 1 / (1 + PitLine.CIRCULATING_LOAD);
			double max = PitLine.kgPerTick(CYCLONE_RATED_TPH) * part.fast();
			max = Math.min(max, (CLUSTER_OUT_HOLD - part.out().held(Product.FINES_SLURRY)) * PitLine.OVERFLOW_SOLIDS / overShare);
			max = Math.min(max, (BALL_BACK_HOLD - mill.back().held(Product.MILL_DISCHARGE)) / (1 - overShare));
			if (max < 1e-6) {
				part.say(Crushing.NOWHERE + " (fines slurry on the pipe at its south face, or the ball mill is full)");
				return;
			}
			Slurry.Taken taken = Slurry.take(part.getLevel(), inlet, Product.MILL_DISCHARGE, max / feedSolids, part);
			double solids = taken.kg() * feedSolids;
			if (solids < 1e-6) {
				part.say("NO FEED");
				return;
			}
			SlurryAssay dry = Grinding.unwet(taken.assay(), feedSolids);
			double over = solids * overShare;
			part.out().offer(Product.FINES_SLURRY, over / PitLine.OVERFLOW_SOLIDS, Assays.wet(dry, PitLine.OVERFLOW_SOLIDS));
			mill.back().offer(Product.MILL_DISCHARGE, solids - over, dry);
			mill.setChanged();
			part.pit().weigh(solids / part.fast());
			part.say(String.format("classifying at 150 micrometres, %.0f kPa", pressureKpa(part.pit().rateTph())));
			part.setChanged();
		}
	};
}
