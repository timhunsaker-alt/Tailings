package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.item.HeavyMineralConcentrateItem;
import dev.wildcatter.tailings.plant.DryMill;
import dev.wildcatter.tailings.plant.FullRecoveryRates;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Server scenes for the dry mill (docs/multiblocks/dry-mill.md, L6; decisions in docs/questions/l6-dry-mill.md): a bag of HMC split
 * four ways by the real properties; the whole ladder from fines through spirals, scavenger spirals, filter, dryer and separators to
 * 4x, with nothing left for the leach; a separator refusing wet HMC; a filter with no dryer bagging the scavengers' share; a dryer
 * with no gas.
 */
final class DryMillGameTests {
	static final String[] SCENES = {"dry_mill_split", "dry_mill_ladder", "dry_mill_wet", "dry_mill_no_dryer", "dry_mill_no_gas", "dry_mill_parts"};
	static final Set<String> LONG = Set.of("dry_mill_split", "dry_mill_ladder", "dry_mill_no_dryer");
	/** The separators' four output stacks. */
	private static final int[] FOUR = dev.wildcatter.tailings.block.stage.DryMillStages.OUTPUT_SLOTS;

	private DryMillGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("dry_mill_split", DryMillGameTests::split);
		out.accept("dry_mill_ladder", DryMillGameTests::ladder);
		out.accept("dry_mill_wet", DryMillGameTests::wet);
		out.accept("dry_mill_no_dryer", DryMillGameTests::noDryer);
		out.accept("dry_mill_no_gas", DryMillGameTests::noGas);
		out.accept("dry_mill_parts", DryMillGameTests::parts);
	}

	private static int nuggets(PlantBlockEntity part, int[] slots, Mineral mineral) {
		return part.countIn(slots, Feeds.stack(mineral.nuggetId(), 1)) + 9 * part.countIn(slots, Feeds.stack(mineral.phaseOneProductId(), 1));
	}

	/** A stock tank of pipeline gas whose pipe meets the east side of a dryer placed at {@code dryer}. */
	private static void gas(GameTestHelper helper, BlockPos dryer) {
		var tank = TailingsGameTests.part(helper, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "stock_tank")), dryer.offset(4, 0, 1),
				dev.wildcatter.block.StorageTankBlockEntity.class);
		tank.testFill(Product.PIPELINE_GAS, 5000);
		TailingsGameTests.pipe(helper, dryer.offset(3, 0, 2));
	}

	/** The dryer at {@code at} and the three separators behind it, each touching the last, all forced on and fast. */
	private static PlantBlockEntity[] line(GameTestHelper helper, BlockPos at) {
		PlantBlockEntity dryer = TailingsGameTests.plant(helper, Machine.FLUID_BED_DRYER, at);
		PlantBlockEntity rer = TailingsGameTests.plant(helper, Machine.RER_SEPARATOR, at.offset(0, 0, 6));
		PlantBlockEntity ht = TailingsGameTests.plant(helper, Machine.HT_ROLL_SEPARATOR, at.offset(0, 0, 9));
		PlantBlockEntity irm = TailingsGameTests.plant(helper, Machine.IRM_SEPARATOR, at.offset(0, 0, 13));
		for (PlantBlockEntity part : new PlantBlockEntity[] {dryer, rer, ht, irm}) {
			part.forceRunning();
			part.fastCycle(100);
		}
		return new PlantBlockEntity[] {dryer, rer, ht, irm};
	}

	private static String all(PlantBlockEntity... parts) {
		StringBuilder out = new StringBuilder();
		for (PlantBlockEntity part : parts) {
			out.append(" | ").append(part.inspect().getString());
		}
		return out.toString();
	}

	/** dry_mill_split: one bag of mixed HMC, dried on gas, comes out at four separators, each its own minerals. */
	private static void split(GameTestHelper helper) {
		PlantBlockEntity[] p = line(helper, new BlockPos(0, 4, 0));
		gas(helper, new BlockPos(0, 4, 0));
		Map<Mineral, Integer> bag = new java.util.EnumMap<>(Mineral.class);
		bag.put(Mineral.ILMENITE, 50);
		bag.put(Mineral.ZIRCON, 30);
		bag.put(Mineral.RUTILE, 6);
		bag.put(Mineral.MONAZITE, 3);
		bag.put(Mineral.CASSITERITE, 10);
		helper.assertTrue(p[0].offer(HeavyMineralConcentrateItem.of(TailingsItems.HEAVY_MINERAL_CONCENTRATE, bag)), Component.literal("the dryer should take a bag of HMC"));
		helper.succeedWhen(() -> {
			String s = all(p);
			helper.assertTrue(nuggets(p[1], FOUR, Mineral.ILMENITE) == 4, Component.literal("the roll magnet should pay 4 ilmenite nuggets (50 kg x 1.11)" + s));
			helper.assertTrue(nuggets(p[2], FOUR, Mineral.RUTILE) == 5 && nuggets(p[2], FOUR, Mineral.CASSITERITE) == 1,
					Component.literal("the high-tension roll should pay 5 rutile and 1 cassiterite nugget" + s));
			helper.assertTrue(nuggets(p[3], FOUR, Mineral.MONAZITE) == 4 && nuggets(p[3], FOUR, Mineral.ZIRCON) == 5, Component.literal("the induced roll should pay 4 monazite, 5 zircon" + s));
			helper.assertTrue(nuggets(p[1], FOUR, Mineral.ZIRCON) + nuggets(p[1], FOUR, Mineral.RUTILE) + nuggets(p[2], FOUR, Mineral.ILMENITE) + nuggets(p[2], FOUR, Mineral.ZIRCON)
					+ nuggets(p[3], FOUR, Mineral.ILMENITE) + nuggets(p[3], FOUR, Mineral.RUTILE) == 0, Component.literal("no mineral at the wrong separator" + s));
			helper.assertTrue(p[0].inspect().getString().contains("m3 of gas burnt"), Component.literal("the dryer should burn gas" + s));
		});
	}

	/**
	 * dry_mill_ladder: fines holding 4 nuggets' worth of zircon and of ilmenite, and a little galena, through the spiral bank, the
	 * scavenger spirals, the thickener and filter, the dryer touching the filter and the separators: 2 nuggets of each at the filter
	 * (50 %), 2 more at the separators (45 % paid at 1.11), 4 in all, the 4x; the scavenger spirals' tails carry none of either under
	 * its own name, so a leach after them finds none; the galena goes on untouched.
	 */
	private static void ladder(GameTestHelper helper) {
		int y = 4;
		var zircon = Mineral.ZIRCON;
		var ilmenite = Mineral.ILMENITE;
		double total = 600;
		double zr = 4 * zircon.nuggetAmount().doubleValue(), il = 4 * ilmenite.nuggetAmount().doubleValue();
		TailingsGameTests.fines(helper, new BlockPos(0, y, 0), total, Map.of(zircon.mineralItemId(), zr / total, ilmenite.mineralItemId(), il / total, Mineral.GALENA.mineralItemId(), 1.0 / total));
		TailingsGameTests.pipe(helper, new BlockPos(0, y, 2), new BlockPos(1, y, 2));
		PlantBlockEntity spirals = TailingsGameTests.plant(helper, Machine.SPIRAL_BANK, new BlockPos(0, y, 3));
		TailingsGameTests.pipe(helper, new BlockPos(0, y, 9), new BlockPos(0, y, 10));
		PlantBlockEntity scavenger = TailingsGameTests.plant(helper, Machine.SCAVENGER_SPIRAL_BANK, new BlockPos(0, y, 11));
		// both concentrates on one line to the thickener, as the spec draws it
		TailingsGameTests.pipe(helper, new BlockPos(3, y, 9), new BlockPos(4, y, 9), new BlockPos(5, y, 9), new BlockPos(6, y, 9));
		TailingsGameTests.pipe(helper, new BlockPos(3, y, 17), new BlockPos(4, y, 17));
		for (int z = 10; z <= 16; z++) {
			TailingsGameTests.pipe(helper, new BlockPos(4, y, z));
		}
		PlantBlockEntity thickener = TailingsGameTests.plant(helper, Machine.THICKENER, new BlockPos(6, y, 10));
		TailingsGameTests.pipe(helper, new BlockPos(6, y, 18));
		PlantBlockEntity filter = TailingsGameTests.plant(helper, Machine.FILTER, new BlockPos(6, y, 19));
		PlantBlockEntity[] p = line(helper, new BlockPos(9, y, 19));
		gas(helper, new BlockPos(9, y, 19));
		for (PlantBlockEntity part : new PlantBlockEntity[] {spirals, scavenger, thickener, filter}) {
			part.forceRunning();
		}
		int[] two = {1, 2};
		helper.succeedWhen(() -> {
			String s = all(spirals, scavenger, thickener, filter) + all(p);
			helper.assertTrue(nuggets(filter, two, zircon) == 2 && nuggets(filter, two, ilmenite) == 2, Component.literal("the filter should pay the spirals' 50 %: 2 nuggets of each" + s));
			helper.assertTrue(nuggets(p[3], FOUR, zircon) == 2, Component.literal("the dry mill should pay 2 zircon nuggets" + s));
			helper.assertTrue(nuggets(p[1], FOUR, ilmenite) == 2, Component.literal("the dry mill should pay 2 ilmenite nuggets" + s));
			SlurryAssay tails = scavenger.out().assayOf(Product.FINES_SLURRY);
			double left = scavenger.out().held(Product.FINES_SLURRY);
			helper.assertTrue(tails.get(zircon.mineralItemId()) == 0 && tails.get(ilmenite.mineralItemId()) == 0, Component.literal("no zircon or ilmenite for the leach: " + tails.describe()));
			helper.assertTrue(Math.abs(tails.kg(DryMill.scavengedKey(zircon), left) - zr * 0.05) < 1e-3, Component.literal("5 % of the zircon lost to the tails, got " + tails.kg(DryMill.scavengedKey(zircon), left)));
			for (String id : tails.minerals()) {
				var mineral = dev.wildcatter.tailings.plant.Assays.mineral(id);
				helper.assertTrue(mineral == null || mineral == Mineral.GALENA || FullRecoveryRates.leachShare(mineral) == 0, Component.literal("the leach would take " + id));
			}
			helper.assertTrue(Math.abs(tails.kg(Mineral.GALENA.mineralItemId(), left) - 1) < 1e-6, Component.literal("the galena goes on untouched" + s));
		});
	}

	/** dry_mill_wet: a bag of moist HMC into the roll magnet: refused, WET. */
	private static void wet(GameTestHelper helper) {
		PlantBlockEntity rer = TailingsGameTests.plant(helper, Machine.RER_SEPARATOR, new BlockPos(0, 4, 0));
		rer.forceRunning();
		ItemStack bag = HeavyMineralConcentrateItem.of(TailingsItems.HEAVY_MINERAL_CONCENTRATE, Map.of(Mineral.ILMENITE, 100));
		helper.assertFalse(rer.offer(bag), Component.literal("the magnet should refuse moist HMC"));
		helper.assertTrue(rer.status().startsWith("WET"), Component.literal("and read WET, got " + rer.status()));
		helper.assertTrue(bag.getCount() == 1, Component.literal("and keep none of it"));
		helper.succeed();
	}

	/** dry_mill_no_dryer: with no dryer the filter bags the scavengers' share, 100 kg a bag, its assay kept; it pays none of it. */
	private static void noDryer(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity source = TailingsGameTests.plant(helper, Machine.THICKENER, new BlockPos(0, y, 0));
		double kg = 300;
		source.out().offer(Product.THICKENED_CONCENTRATE, kg, SlurryAssay.of(Map.of(DryMill.hmcKey(Mineral.ZIRCON), 120.0 / kg, DryMill.hmcKey(Mineral.ILMENITE), 30.0 / kg)));
		TailingsGameTests.pipe(helper, new BlockPos(0, y, 8));
		PlantBlockEntity filter = TailingsGameTests.plant(helper, Machine.FILTER, new BlockPos(0, y, 9));
		filter.forceRunning();
		helper.succeedWhen(() -> {
			ItemStack out = filter.getItem(1);
			helper.assertTrue(out.getItem() == TailingsItems.HEAVY_MINERAL_CONCENTRATE && out.getCount() == 1, Component.literal("one 100 kg bag of HMC, got " + out + all(filter)));
			var in = HeavyMineralConcentrateItem.contents(out);
			helper.assertTrue(in.kg(Mineral.ZIRCON) == 80 && in.kg(Mineral.ILMENITE) == 20, Component.literal("its assay kept: 80 kg zircon, 20 ilmenite, got " + in.describe()));
			helper.assertTrue(Math.abs(filter.hmc().total() - 50) < 1e-6, Component.literal("50 kg waits for the next bag, got " + filter.hmc().total()));
			helper.assertTrue(filter.getItem(2).isEmpty() && nuggets(filter, new int[] {1, 2}, Mineral.ZIRCON) == 0, Component.literal("and no zircon paid at the filter"));
		});
	}

	/** dry_mill_no_gas: a dryer with HMC and no gas on its pipe stops, NO GAS. */
	private static void noGas(GameTestHelper helper) {
		PlantBlockEntity[] p = line(helper, new BlockPos(0, 4, 0));
		p[0].offer(HeavyMineralConcentrateItem.of(TailingsItems.HEAVY_MINERAL_CONCENTRATE, Map.of(Mineral.ZIRCON, 100)));
		helper.succeedWhen(() -> {
			helper.assertTrue(p[0].status().startsWith("NO GAS"), Component.literal("it should read NO GAS, got " + p[0].status()));
			helper.assertTrue(p[1].hmc().isEmpty(), Component.literal("and pass nothing on"));
		});
	}

	/** dry_mill_parts: each part fills its spec's cells and has a motor on the world's industrial tier. */
	private static void parts(GameTestHelper helper) {
		int x = 0;
		for (Machine machine : new Machine[] {Machine.SCAVENGER_SPIRAL_BANK, Machine.FLUID_BED_DRYER, Machine.RER_SEPARATOR, Machine.HT_ROLL_SEPARATOR, Machine.IRM_SEPARATOR}) {
			BlockPos at = new BlockPos(x, 4, 0);
			PlantBlockEntity part = TailingsGameTests.plant(helper, machine, at);
			int cells = part.cells().size();
			helper.assertTrue(cells == machine.cells(), Component.literal(machine + " should fill " + machine.cells() + " cells, got " + cells));
			helper.assertTrue(part.inspect().getString().contains("kW"), Component.literal(machine + " should read its motor"));
			helper.assertTrue(!part.points().isEmpty() && part.points().get(0).name().equals("RUN"), Component.literal(machine + " should have its PLC points"));
			x += machine.width() + 2;
		}
		helper.succeed();
	}
}
