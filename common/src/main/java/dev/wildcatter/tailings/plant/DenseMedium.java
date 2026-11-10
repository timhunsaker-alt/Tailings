package dev.wildcatter.tailings.plant;

import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.EnumMap;
import java.util.Map;

/**
 * Dense-medium separation's rules (docs/multiblocks/dense-medium-separation.md, L7), plain Java. Gravel is mixed with a slurry of
 * fine ferrosilicon in water at a set density; a piece lighter than the medium floats and leaves the plant as aggregate, a heavier
 * piece sinks and goes on to the cone. A piece's density is its rock's: the mixing rule over the minerals the block holds (the plant
 * counts a block as 2.7 t) at their textbook specific gravities, the rest of the block its host rock. The game takes the block's mean
 * (liberation at gravel size is not modelled), so in practice DMS rejects country rock and keeps the ores. A real DMS cyclone is sharp:
 * 2 % of what should sink floats and 2 % of what should float sinks (proposed).
 */
public final class DenseMedium {
	private DenseMedium() {
	}

	/** The medium's density range on the module's setting, and its default (dense-medium-separation.md, proposed). */
	public static final double SG_LOW = 2.60, SG_HIGH = 3.30, SG_DEFAULT = 2.75;
	/** One step of the setting: a sneaking click on the module moves it this much, wrapping at the top (proposed). */
	public static final double SG_STEP = 0.05;
	/** The share of each side that reports to the wrong product (proposed: real DMS cyclones are sharp). */
	public static final double MISPLACED = 0.02;
	/** The module's rate: one 610 mm DMS cyclone, about 60 t/h of feed (unverified), more than the plant's share of gravel. */
	public static final double TONNES_PER_HOUR = 60;
	public static final double KG_PER_TICK = TONNES_PER_HOUR * 1000 / 72_000;
	/** Ferrosilicon lost to the products and the magnetic separator's tails: 0.3 kg a tonne of feed (proposed; real 0.1-0.5). */
	public static final double FESI_KG_PER_TONNE = 0.3;
	/** One `ferrosilicon` item: a 25 kg bag of milled 15 % Si ferrosilicon (proposed). */
	public static final double FESI_BAG_KG = 25;
	/** The medium recovery's make-up hopper: 1 t of ferrosilicon, 40 bags (proposed). */
	public static final double FESI_HOLD_KG = 1000;
	/** Ferrosilicon's own density, for the medium's makeup (milled FeSi 15 %, textbook). */
	public static final double FESI_SG = 6.8;
	/** Floats leave as vanilla gravel, one block for each 1.9 t (open-pit.md's loose gravel). */
	public static final double FLOATS_KG_PER_GRAVEL = 1900;
	/** The deslime screen washes off the -1 mm still on the gravel, back to the sand belt (proposed: a few per cent). */
	public static final double DESLIME_FINES = 0.03;
	/** Make-up water at the module for the drain-and-rinse sprays, net of what the medium recovery returns: 0.15 L a kg (proposed). */
	public static final double WATER_PER_KG = 0.15;
	/** The module's own water, 10 s of its use at full rate. */
	public static final double WATER_HOLD = 10 * TONNES_PER_HOUR / 3.6 * WATER_PER_KG;
	/** The deslime and drain-and-rinse screens each pass the module's rate. */
	public static final double SCREEN_KG_PER_TICK = KG_PER_TICK;

	/** Textbook specific gravities of the mineral each assay entry stands for (Wills, Dana): the carrier of a vanilla metal. */
	private static final Map<Mineral, Double> SG = new EnumMap<>(Mineral.class);
	/** kg of carrier mineral a kg in the assay stands for: the vanilla metals are counted as metal. */
	private static final Map<Mineral, Double> CARRIER = new EnumMap<>(Mineral.class);
	/** The host rock's density: the country rock itself, or what an ore block's mineral sits in. */
	private static final Map<Rock, Double> HOST = new EnumMap<>(Rock.class);
	/** An ore's host, unless written: quartz and feldspar. */
	public static final double HOST_DEFAULT = 2.65;

	static {
		// the vanilla metals as their usual minerals: hematite (70 % Fe), chalcopyrite (34.6 % Cu), native gold
		mineral(Mineral.IRON, 5.26, 1 / 0.70);
		mineral(Mineral.COPPER, 4.20, 1 / 0.346);
		mineral(Mineral.GOLD, 19.3, 1);
		mineral(Mineral.ZIRCON, 4.65, 1);
		mineral(Mineral.ILMENITE, 4.72, 1);
		mineral(Mineral.RUTILE, 4.25, 1);
		mineral(Mineral.MONAZITE, 5.15, 1);
		mineral(Mineral.URANINITE, 8.7, 1);
		mineral(Mineral.PITCHBLENDE, 7.0, 1);
		mineral(Mineral.SULFUR, 2.07, 1);
		mineral(Mineral.HALITE, 2.17, 1);
		mineral(Mineral.SYLVITE, 1.99, 1);
		mineral(Mineral.FLUORITE, 3.18, 1);
		mineral(Mineral.BAUXITE, 2.45, 1);
		mineral(Mineral.GALENA, 7.58, 1);
		mineral(Mineral.SPHALERITE, 4.05, 1);
		mineral(Mineral.CASSITERITE, 6.99, 1);
		mineral(Mineral.PENTLANDITE, 4.8, 1);
		mineral(Mineral.CHROMITE, 4.6, 1);
		mineral(Mineral.PLATINUM, 21.45, 1);
		mineral(Mineral.MOLYBDENITE, 4.73, 1);
		mineral(Mineral.MAGNESITE, 3.0, 1);
		mineral(Mineral.DOLOMITE, 2.85, 1);
		mineral(Mineral.GRAPHITE, 2.2, 1);
		mineral(Mineral.BORAX, 1.71, 1);
		mineral(Mineral.SPODUMENE, 3.15, 1);
		mineral(Mineral.BARITE, 4.48, 1);
		mineral(Mineral.BENTONITE, 2.35, 1);
		mineral(Mineral.ACANTHITE, 7.2, 1);
		mineral(Mineral.SCHEELITE, 6.1, 1);
		mineral(Mineral.COBALTITE, 6.3, 1);
		mineral(Mineral.PYROLUSITE, 4.75, 1);
		mineral(Mineral.PHOSPHATE, 3.2, 1);
		// country rocks (textbook ranges, the middle taken; unverified): stone and cobble as a granodiorite
		HOST.put(Rock.COBBLESTONE, 2.70);
		HOST.put(Rock.STONE, 2.70);
		HOST.put(Rock.GRANITE, 2.65);
		HOST.put(Rock.DIORITE, 2.85);
		HOST.put(Rock.ANDESITE, 2.65);
		HOST.put(Rock.DEEPSLATE, 2.80);
		HOST.put(Rock.TUFF, 2.40);
		HOST.put(Rock.SANDSTONE, 2.35);
		// ores whose host is not quartz and feldspar: the platinum reef's pyroxenite, the nickel ore's norite
		HOST.put(Rock.PLATINUM, 3.20);
		HOST.put(Rock.PENTLANDITE, 3.00);
		for (Mineral mineral : Mineral.values()) {
			if (!SG.containsKey(mineral)) {
				throw new IllegalStateException(mineral + " has no specific gravity");
			}
		}
	}

	private static void mineral(Mineral mineral, double sg, double carrier) {
		SG.put(mineral, sg);
		CARRIER.put(mineral, carrier);
	}

	/** A mineral's specific gravity (its carrier's, for a vanilla metal). */
	public static double sg(Mineral mineral) {
		return SG.get(mineral);
	}

	/** A rock's host density. */
	public static double host(Rock rock) {
		return HOST.getOrDefault(rock, HOST_DEFAULT);
	}

	/** The share of a 2.7 t block that is {@code mineral}'s carrier mineral. */
	public static double massShare(Rock rock, Mineral mineral) {
		return rock.contained(mineral).doubleValue() * mineral.kgPerUnit() * CARRIER.get(mineral) / PlantRates.BLOCK_KG;
	}

	/**
	 * A piece of {@code rock}'s density: the mixing rule, 1 / SG = sum of (mass share / SG of each part), the host rock taking what the
	 * minerals leave. A 10 % sphalerite ore in quartz is about 2.75; the game's copper ore (43 % chalcopyrite) about 3.15.
	 */
	public static double pieceSg(Rock rock) {
		double minerals = 0, inverse = 0;
		for (Mineral mineral : rock.minerals()) {
			double share = massShare(rock, mineral);
			minerals += share;
			inverse += share / sg(mineral);
		}
		if (minerals > 1) {
			// never more mineral than block: scale down to the whole block
			inverse /= minerals;
			minerals = 1;
		}
		return 1 / (inverse + (1 - minerals) / host(rock));
	}

	/** True if a piece of {@code rock} sinks in a medium of {@code medium}: heavier pieces, and those at the cut, sink. */
	public static boolean sinks(Rock rock, double medium) {
		return pieceSg(rock) >= medium;
	}

	/** The share of {@code rock} that reports to the sinks at {@code medium}: 98 % of what sinks, 2 % of what floats. */
	public static double sinkShare(Rock rock, double medium) {
		return sinks(rock, medium) ? 1 - MISPLACED : MISPLACED;
	}

	/** The medium's density held within its range. */
	public static double clamp(double medium) {
		return Math.max(SG_LOW, Math.min(SG_HIGH, medium));
	}

	/** The next setting up from {@code medium}, wrapping from the top back to the bottom. */
	public static double step(double medium) {
		double next = Math.round((medium + SG_STEP) * 100) / 100.0;
		return next > SG_HIGH + 1e-9 ? SG_LOW : next;
	}

	/** What one pass does to a load of gravel: the sinks, still gravel with their rock, and the floats, which leave as aggregate. */
	public record Split(RockLoad sinks, RockLoad floats) {
	}

	public static Split split(RockLoad feed, double medium) {
		RockLoad sinks = new RockLoad();
		RockLoad floats = new RockLoad();
		feed.forEach((rock, size, kg) -> {
			double share = sinkShare(rock, medium);
			sinks.add(rock, SizeClass.GRAVEL, kg * share);
			floats.add(rock, SizeClass.GRAVEL, kg * (1 - share));
		});
		return new Split(sinks, floats);
	}

	/** The share of a load that sinks at {@code medium}. */
	public static double sinkShare(RockLoad feed, double medium) {
		double total = feed.total();
		if (total <= 0) {
			return 1;
		}
		double[] sinks = {0};
		feed.forEach((rock, size, kg) -> sinks[0] += kg * sinkShare(rock, medium));
		return sinks[0] / total;
	}

	/** kg of ferrosilicon lost treating {@code kg} of feed. */
	public static double fesiLost(double kg) {
		return kg / 1000 * FESI_KG_PER_TONNE;
	}

	/** Vanilla gravel blocks from {@code kg} of floats, whole blocks only. */
	public static int gravelBlocks(double kg) {
		return (int) Math.floor(kg / FLOATS_KG_PER_GRAVEL + 1e-9);
	}

	/** The volume share of ferrosilicon in water that makes a medium of {@code medium}: (SG - 1) / (6.8 - 1). */
	public static double fesiVolumeShare(double medium) {
		return (medium - 1) / (FESI_SG - 1);
	}
}
