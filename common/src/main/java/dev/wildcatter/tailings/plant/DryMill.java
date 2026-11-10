package dev.wildcatter.tailings.plant;

import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * The dry mill's rules (docs/multiblocks/dry-mill.md, L6), plain Java. The scavenger spiral bank takes another 45 % of what each block
 * held of the heavy minerals off the spirals' tails; what it takes travels in the slurry's assay under its own name, {@code
 * tailings:hmc_zircon}, so the filter keeps it apart as heavy mineral concentrate (HMC) instead of paying it; what it leaves travels as
 * {@code tailings:scavenged_zircon}, so neither the leach nor a second set of spirals takes it again. The dryer and the three
 * separators pay that HMC at the top rung's rate, by the minerals' real properties:
 * <ul>
 * <li>ilmenite is magnetic: the rare-earth roll magnet takes it;</li>
 * <li>rutile and cassiterite conduct: the high-tension roll takes them;</li>
 * <li>monazite is weakly magnetic: the induced-roll magnet takes it at high intensity;</li>
 * <li>zircon is neither: it is what the induced-roll magnet leaves.</li>
 * </ul>
 */
public final class DryMill {
	private DryMill() {
	}

	/** Of the 50 % the spirals leave, the scavenger spirals take 45 (so 50 + 45 = 95 %, the ladder's top rung; proposed). */
	public static final double SCAVENGER_SHARE = 45.0 / 50.0;
	/** The dryer and each separator: 2 t/h of HMC (dry-mill.md, proposed; a 1.5 m roll at 1-4 t/h a metre). */
	public static final double KG_PER_TICK = 2000.0 / 72_000;
	/** A fluid-bed dryer's fuel: about 0.3 GJ a tonne of HMC (proposed), so 300 kJ a kg. */
	public static final double DRYER_JOULES_PER_KG = 0.3e9 / 1000;
	/** The dryer's feed bin and each separator's feed hopper: one tonne of HMC (proposed). */
	public static final double HOLD_KG = 1000;
	/** One `heavy_mineral_concentrate` item: a 100 kg bag of moist HMC as the filter drops it (proposed). */
	public static final double ITEM_KG = 100;

	private static final String HMC = "tailings:hmc_";
	private static final String SCAVENGED = "tailings:scavenged_";

	/** The minerals the dry mill splits: the heavy minerals of mineral sands, and cassiterite, which goes with the conductors. */
	public static boolean takes(Mineral mineral) {
		return switch (mineral) {
			case ILMENITE, RUTILE, ZIRCON, MONAZITE, CASSITERITE -> true;
			default -> false;
		};
	}

	/** The separator a mineral reports to as product. */
	public static @Nullable Machine separator(Mineral mineral) {
		return switch (mineral) {
			case ILMENITE -> Machine.RER_SEPARATOR;
			case RUTILE, CASSITERITE -> Machine.HT_ROLL_SEPARATOR;
			case MONAZITE, ZIRCON -> Machine.IRM_SEPARATOR;
			default -> null;
		};
	}

	/** The separator after {@code machine} in the line, which takes what it rejects; null after the last. */
	public static @Nullable Machine next(Machine machine) {
		return switch (machine) {
			case FLUID_BED_DRYER -> Machine.RER_SEPARATOR;
			case RER_SEPARATOR -> Machine.HT_ROLL_SEPARATOR;
			case HT_ROLL_SEPARATOR -> Machine.IRM_SEPARATOR;
			default -> null;
		};
	}

	/** True for the three separators. */
	public static boolean separates(Machine machine) {
		return machine == Machine.RER_SEPARATOR || machine == Machine.HT_ROLL_SEPARATOR || machine == Machine.IRM_SEPARATOR;
	}

	/** What a separator pays a kg won: the top rung's rate for what does not float (1.11), so 45 % pays as 50 % and the line makes 4x. */
	public static double pay(Mineral mineral) {
		return FullRecoveryRates.topRungPay(mineral);
	}

	/** m³ of natural gas to dry {@code kg} of HMC, at {@code joulesPerM3} (Wildcatter's heating value for pipeline gas). */
	public static double gasM3(double kg, double joulesPerM3) {
		return kg * DRYER_JOULES_PER_KG / joulesPerM3;
	}

	public static String hmcKey(Mineral mineral) {
		return HMC + mineral.name().toLowerCase(Locale.ROOT);
	}

	public static String scavengedKey(Mineral mineral) {
		return SCAVENGED + mineral.name().toLowerCase(Locale.ROOT);
	}

	/** The mineral an assay's HMC entry carries, or null for any other entry. */
	public static @Nullable Mineral fromHmcKey(String key) {
		if (!key.startsWith(HMC)) {
			return null;
		}
		try {
			Mineral mineral = Mineral.valueOf(key.substring(HMC.length()).toUpperCase(Locale.ROOT));
			return takes(mineral) ? mineral : null;
		} catch (IllegalArgumentException ex) {
			return null;
		}
	}

	/** What {@code kg} of fines slurry makes on the scavenger spirals. */
	public record Split(double concentrate, SlurryAssay concentrateAssay, double rest, SlurryAssay restAssay) {
	}

	/**
	 * The scavenger spirals' split: of each heavy mineral, {@link #SCAVENGER_SHARE} to HMC in a {@code solids}-percent concentrate slurry
	 * and the rest marked scavenged; every other entry passes on as it came.
	 */
	public static Split scavenge(SlurryAssay feed, double kg, double solids) {
		Map<String, Double> conc = new HashMap<>();
		Map<String, Double> rest = new HashMap<>();
		double taken = 0;
		for (String id : feed.minerals()) {
			double there = feed.get(id) * kg;
			Mineral mineral = Assays.mineral(id);
			if (mineral != null && takes(mineral)) {
				double got = there * SCAVENGER_SHARE;
				conc.merge(hmcKey(mineral), got, Double::sum);
				rest.merge(scavengedKey(mineral), there - got, Double::sum);
				taken += got;
			} else {
				rest.merge(id, there, Double::sum);
			}
		}
		double concKg = Math.min(kg, taken / solids);
		double restKg = kg - concKg;
		conc.replaceAll((id, m) -> concKg > 0 ? m / concKg : 0);
		rest.replaceAll((id, m) -> restKg > 0 ? m / restKg : 0);
		return new Split(concKg, SlurryAssay.of(conc), restKg, SlurryAssay.of(rest));
	}

	/** The HMC in {@code kg} of a concentrate assayed {@code assay}, by mineral. */
	public static HeavyMinerals hmcIn(SlurryAssay assay, double kg) {
		HeavyMinerals out = new HeavyMinerals();
		for (String id : assay.minerals()) {
			Mineral mineral = fromHmcKey(id);
			if (mineral != null) {
				out.add(mineral, assay.get(id) * kg);
			}
		}
		return out;
	}

	/** What a separator's pass makes: its product by mineral, and the rest on to the next separator. */
	public record Pass(HeavyMinerals product, HeavyMinerals rest) {
	}

	/** {@code feed} through {@code separator}: the minerals that report to it are its product, all else the rest. */
	public static Pass separate(Machine separator, HeavyMinerals feed) {
		HeavyMinerals product = new HeavyMinerals();
		HeavyMinerals rest = new HeavyMinerals();
		for (Mineral mineral : feed.minerals()) {
			(separator(mineral) == separator ? product : rest).add(mineral, feed.kg(mineral));
		}
		return new Pass(product, rest);
	}

	/**
	 * One bag of HMC off {@code store}, whole kg of each mineral so that bags of one feed stack, the fractions left in the store; null
	 * while it holds under a bag.
	 */
	public static @Nullable Map<Mineral, Integer> bag(HeavyMinerals store) {
		double total = store.total();
		if (total < ITEM_KG - 1e-6) {
			return null;
		}
		Map<Mineral, Integer> bag = new java.util.EnumMap<>(Mineral.class);
		for (Mineral mineral : store.minerals()) {
			int whole = (int) Math.floor(store.kg(mineral) * ITEM_KG / total + 1e-9);
			if (whole > 0) {
				bag.put(mineral, whole);
			}
		}
		if (bag.isEmpty()) {
			return null;
		}
		bag.forEach((mineral, kg) -> store.remove(mineral, kg));
		return bag;
	}
}
