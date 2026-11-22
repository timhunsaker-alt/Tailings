package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.Smelter;
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
import net.minecraft.world.item.Items;

/**
 * Server scenes for the copper smelter line (docs/copper-smelter.md; decisions in docs/questions/copper-smelter.md): each unit on its
 * own, forced on and fast, fed what it takes; the filter's shortcut and its copper sent to a dryer; the whole line from thickened
 * concentrate to cathodes and ingots on generator sets is {@link SmelterLineGameTests}.
 */
final class SmelterGameTests {
	static final String[] SCENES = {"smelter_shortcut", "smelter_dryer", "smelter_furnace", "smelter_furnace_stops", "smelter_converter", "smelter_anodes",
			"smelter_tankhouse", "smelter_acid"};
	static final Set<String> LONG = Set.of(SCENES);
	private static final int[] OUT = {1, 2};
	/** The scenes' speed-up: the line's 1.68 t/h, 50 times over. */
	static final int FAST = 50;

	private SmelterGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("smelter_shortcut", SmelterGameTests::shortcut);
		out.accept("smelter_dryer", SmelterGameTests::dryer);
		out.accept("smelter_furnace", SmelterGameTests::furnace);
		out.accept("smelter_furnace_stops", SmelterGameTests::furnaceStops);
		out.accept("smelter_converter", SmelterGameTests::converter);
		out.accept("smelter_anodes", SmelterGameTests::anodes);
		out.accept("smelter_tankhouse", SmelterGameTests::tankhouse);
		out.accept("smelter_acid", SmelterGameTests::acid);
	}

	static PlantBlockEntity part(GameTestHelper helper, Machine machine, BlockPos at) {
		PlantBlockEntity p = TailingsGameTests.plant(helper, machine, at);
		p.forceRunning();
		p.fastCycle(FAST);
		return p;
	}

	/** A stock tank of pipeline gas whose pipe meets the east face of the front cells of a part of {@code width} at {@code at}. */
	static void gas(GameTestHelper helper, BlockPos at, int width) {
		var tank = TailingsGameTests.part(helper, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "stock_tank")), at.offset(width + 1, 0, 0),
				dev.wildcatter.block.StorageTankBlockEntity.class);
		tank.testFill(Product.PIPELINE_GAS, 50_000);
		TailingsGameTests.pipe(helper, at.offset(width, 0, 0));
	}

	/** Thickened concentrate on a filter's inlet carrying {@code copperKg} of copper. */
	static void concentrate(PlantBlockEntity filter, double copperKg) {
		double kg = Math.max(1000, copperKg * 2);   // a share of the slurry, never more than all of it
		filter.back().offer(Product.THICKENED_CONCENTRATE, kg, SlurryAssay.of(Map.of(Smelter.COPPER, copperKg / kg)));
	}

	private static String all(PlantBlockEntity... parts) {
		StringBuilder out = new StringBuilder();
		for (PlantBlockEntity p : parts) {
			out.append(" | ").append(p.inspect().getString());
		}
		return out.toString();
	}

	private static void check(GameTestHelper helper, boolean ok, String why) {
		helper.assertTrue(ok, Component.literal(why));
	}

	/** Copper paid by a filter, in nuggets (an ingot is nine). */
	private static int nuggets(PlantBlockEntity filter) {
		return filter.countIn(OUT, Feeds.stack(Mineral.COPPER.nuggetId(), 1)) + 9 * filter.countIn(OUT, new ItemStack(Items.COPPER_INGOT));
	}

	/**
	 * smelter_shortcut: in a new world (the copper_shortcut rule on) a filter alone pays 85 % of its copper as nuggets and ingots, the
	 * shortcut; a filter with a concentrate dryer touching it pays none and sends it all to the dryer.
	 */
	private static void shortcut(GameTestHelper helper) {
		double nugget = Mineral.COPPER.nuggetAmount().doubleValue() * Mineral.COPPER.kgPerUnit();
		PlantBlockEntity alone = part(helper, Machine.FILTER, new BlockPos(0, 4, 0));
		concentrate(alone, 20 * nugget);
		PlantBlockEntity fed = part(helper, Machine.FILTER, new BlockPos(12, 4, 0));
		PlantBlockEntity dryer = TailingsGameTests.plant(helper, Machine.CONCENTRATE_DRYER, new BlockPos(12, 4, 5));
		concentrate(fed, 20 * nugget);
		boolean[] said = {false};
		helper.onEachTick(() -> said[0] |= alone.status().contains("shortcut"));
		helper.succeedWhen(() -> {
			check(helper, alone.back().held(Product.THICKENED_CONCENTRATE) < 1e-6 && fed.back().held(Product.THICKENED_CONCENTRATE) < 1e-6, "both filter their feed" + all(alone, fed));
			check(helper, nuggets(alone) == 17, "20 nuggets' copper pays 17 at 85 %, got " + nuggets(alone) + all(alone));
			check(helper, said[0], "and says, filtering, it is the shortcut: " + alone.status());
			check(helper, nuggets(fed) == 0, "the filter with a dryer pays no copper, got " + nuggets(fed));
			check(helper, Math.abs(dryer.smelter().copper() - 20 * nugget) < 1e-3, "it all goes to the dryer: " + dryer.smelter().copper() + " of " + 20 * nugget);
		});
	}

	/** smelter_dryer: the dryer dries 300 kg of copper's concentrate on gas into the furnace's bin, and reads NO GAS without gas. */
	private static void dryer(GameTestHelper helper) {
		PlantBlockEntity dryer = part(helper, Machine.CONCENTRATE_DRYER, new BlockPos(2, 4, 0));
		PlantBlockEntity furnace = TailingsGameTests.plant(helper, Machine.FLASH_FURNACE, new BlockPos(2, 4, 16));
		gas(helper, new BlockPos(2, 4, 0), 4);
		dryer.smelter().add(300);
		PlantBlockEntity dry = part(helper, Machine.CONCENTRATE_DRYER, new BlockPos(20, 4, 0));
		TailingsGameTests.plant(helper, Machine.FLASH_FURNACE, new BlockPos(20, 4, 16));
		dry.smelter().add(300);
		helper.succeedWhen(() -> {
			check(helper, Math.abs(furnace.smelter().copper() - 300) < 1e-6, "300 kg of copper's concentrate dried into the furnace's bin: " + all(dryer, furnace));
			check(helper, dryer.inspect().getString().contains("m3 of gas burnt"), "on gas: " + dryer.inspect().getString());
			check(helper, dry.status().startsWith("NO GAS"), "with no gas it reads NO GAS: " + dry.status());
		});
	}

	/**
	 * smelter_furnace: 400 kg of copper's dry concentrate, silica sand by hand and gas: 97.5 % of it to the converter as matte, 2.5 % in
	 * 0.86 t of slag, the concentrate's sulfur less the matte's to the off-gas, as Wildcatter burner gas; the sand used at 0.15 t a t of
	 * concentrate.
	 */
	private static void furnace(GameTestHelper helper) {
		PlantBlockEntity f = part(helper, Machine.FLASH_FURNACE, new BlockPos(2, 4, 0));
		PlantBlockEntity conv = TailingsGameTests.plant(helper, Machine.COPPER_CONVERTER, new BlockPos(2, 4, 16));
		gas(helper, new BlockPos(2, 4, 0), 7);
		check(helper, f.offer(new ItemStack(Items.SAND)), "it takes silica sand");
		check(helper, !f.offer(new ItemStack(Items.COPPER_INGOT)), "and nothing else by hand");
		f.smelter().add(400);
		double[] gasKg = {0};
		helper.onEachTick(() -> gasKg[0] += f.out().give(Product.BURNER_GAS, Double.MAX_VALUE));
		helper.succeedWhen(() -> {
			String s = all(f, conv);
			check(helper, Math.abs(conv.smelter().copper() - 390) < 1e-6, "390 kg of copper in matte at the converter" + s);
			check(helper, Math.abs(f.smelter().lostKg() - 10) < 1e-6 && Math.abs(f.smelter().slagKg() - 400 * Smelter.slagPerCu()) < 1e-3, "10 kg of copper in the slag" + s);
			double sulfur = 400 * (Smelter.sulfurPerCu() - Smelter.converterSulfurPerCu());
			check(helper, Math.abs(f.smelter().sulfurKg() - sulfur) < 1e-3, "its sulfur to the gas: " + sulfur + s);
			check(helper, Math.abs(gasKg[0] - sulfur * dev.wildcatter.fluid.AcidPlant.gasPerSulfur()) < 1e-3, "as burner gas on its outlet: " + gasKg[0]);
			check(helper, Math.abs(f.dryKg() - (Smelter.SAND_BLOCK_KG - 400 * Smelter.furnaceFluxPerCu())) < 1e-3, "its flux used: " + f.dryKg());
		});
	}

	/** smelter_furnace_stops: no sand reads NO FLUX; with sand and no pipe for its off-gas it fills its outlet and stops: OFF-GAS HAS NOWHERE TO GO. */
	private static void furnaceStops(GameTestHelper helper) {
		PlantBlockEntity noFlux = part(helper, Machine.FLASH_FURNACE, new BlockPos(2, 4, 0));
		TailingsGameTests.plant(helper, Machine.COPPER_CONVERTER, new BlockPos(2, 4, 16));
		gas(helper, new BlockPos(2, 4, 0), 7);
		noFlux.smelter().add(400);
		PlantBlockEntity noVent = part(helper, Machine.FLASH_FURNACE, new BlockPos(24, 4, 0));
		TailingsGameTests.plant(helper, Machine.COPPER_CONVERTER, new BlockPos(24, 4, 16));
		gas(helper, new BlockPos(24, 4, 0), 7);
		noVent.offer(new ItemStack(Items.SAND, 4));
		noVent.smelter().add(4000);
		helper.succeedWhen(() -> {
			check(helper, noFlux.status().startsWith("NO FLUX"), "no sand: NO FLUX, got " + noFlux.status());
			check(helper, noVent.status().startsWith("OFF-GAS HAS NOWHERE TO GO") && noVent.readPoint(6), "no pipe for its gas: it stops, got " + noVent.status());
			check(helper, noVent.smelter().copper() > 1000, "with most of its feed unsmelted: " + noVent.smelter().copper());
		});
	}

	/** smelter_converter: 500 kg of copper in matte blown with sand: 480 kg of blister to the anode furnace, 20 kg back to the flash furnace in its slag. */
	private static void converter(GameTestHelper helper) {
		PlantBlockEntity f = TailingsGameTests.plant(helper, Machine.FLASH_FURNACE, new BlockPos(2, 4, 0));
		PlantBlockEntity c = part(helper, Machine.COPPER_CONVERTER, new BlockPos(2, 4, 16));
		PlantBlockEntity af = TailingsGameTests.plant(helper, Machine.ANODE_FURNACE, new BlockPos(2, 4, 25));
		c.offer(new ItemStack(Items.SAND));
		c.smelter().add(500);
		double[] gasKg = {0};
		helper.onEachTick(() -> gasKg[0] += c.out().give(Product.BURNER_GAS, Double.MAX_VALUE));
		helper.succeedWhen(() -> {
			String s = all(f, c, af);
			check(helper, Math.abs(af.smelter().copper() - 480) < 1e-6, "480 kg of blister at the anode furnace" + s);
			check(helper, Math.abs(f.smelter().copper() - 20) < 1e-6, "20 kg back in the flash furnace with the slag" + s);
			check(helper, Math.abs(gasKg[0] - 500 * Smelter.converterSulfurPerCu() * dev.wildcatter.fluid.AcidPlant.gasPerSulfur()) < 1e-3, "the matte's sulfur as burner gas: " + gasKg[0]);
		});
	}

	/** smelter_anodes: 820 kg of blister fire refined on gas and cast: two 400 kg anodes out of the wheel, 20 kg waiting for the next mould. */
	private static void anodes(GameTestHelper helper) {
		PlantBlockEntity af = part(helper, Machine.ANODE_FURNACE, new BlockPos(2, 4, 0));
		PlantBlockEntity wheel = part(helper, Machine.ANODE_CASTING_WHEEL, new BlockPos(2, 4, 9));
		gas(helper, new BlockPos(2, 4, 0), 5);
		af.smelter().add(820);
		helper.succeedWhen(() -> {
			String s = all(af, wheel);
			check(helper, wheel.countIn(OUT, new ItemStack(TailingsItems.COPPER_ANODE)) == 2 && wheel.smelter().madeCount() == 2, "two anodes cast" + s);
			check(helper, Math.abs(wheel.smelter().copper() + wheel.smelter().plated() - 20) < 1e-6, "20 kg left for the next" + s);
			check(helper, Math.abs(Smelter.anodeKg() - 400) < 1e-9, "an anode is 400 kg: " + Smelter.anodeKg());
		});
	}

	/**
	 * smelter_tankhouse: on a Wildcatter rectifier set against it (on a gas set), two anodes by hand and one by a hopper on top refine to twenty-one 57.1 kg cathodes; it takes anodes only; a blast furnace melts a cathode to
	 * two copper ingots.
	 */
	private static void tankhouse(GameTestHelper helper) {
		PlantBlockEntity t = part(helper, Machine.REFINERY_TANKHOUSE, new BlockPos(2, 4, 0));
		t.fastCycle(400);
		// its rectifier against its north end, on a gas set stepped up to the medium tier (copper-smelter.md #11)
		var rectifier = SmelterLineGameTests.rectifier(helper, new BlockPos(2, 4, -4));
		BlockPos panel = new BlockPos(-6, 4, -10);
		BlockPos set = SmelterLineGameTests.set(helper, panel);
		BlockPos mv = SmelterLineGameTests.stepUp(helper, panel);
		SmelterLineGameTests.wire(helper.getLevel(), "wildcatter:cable_mv", mv, java.util.Set.of(), helper.absolutePos(new BlockPos(-15, 3, -15)), helper.absolutePos(new BlockPos(14, 10, 26)),
				SmelterLineGameTests.rbox(helper, new BlockPos(2, 4, -4)));
		helper.runAfterDelay(30, () -> ((dev.wildcatter.block.GensetControlPanelBlockEntity) helper.getLevel().getBlockEntity(set)).setWantRun(true));
		helper.onEachTick(() -> ((dev.wildcatter.block.GensetControlPanelBlockEntity) helper.getLevel().getBlockEntity(set)).setGasForTest(200));
		double[] ka = {0};
		helper.onEachTick(() -> {
			if (t.status().startsWith("refining") && rectifier.dc().amps() > 1) {
				ka[0] = rectifier.dc().amps() / 1000;   // the last reading while refining (closing onto 150 V overshoots at first)
			}
		});
		check(helper, t.offer(new ItemStack(TailingsItems.COPPER_ANODE, 2)) && t.offer(new ItemStack(TailingsItems.COPPER_ANODE)), "it takes anodes");
		check(helper, !t.offer(new ItemStack(Items.COPPER_INGOT)), "and nothing else");
		// a third anode from a hopper on its top
		BlockPos hop = new BlockPos(3, 4 + 3, 1);
		helper.setBlock(hop, net.minecraft.world.level.block.Blocks.HOPPER);
		((net.minecraft.world.Container) helper.getBlockEntity(hop, net.minecraft.world.level.block.entity.HopperBlockEntity.class)).setItem(0, new ItemStack(TailingsItems.COPPER_ANODE));
		var level = helper.getLevel();
		var input = new net.minecraft.world.item.crafting.SingleRecipeInput(new ItemStack(TailingsItems.COPPER_CATHODE));
		ItemStack melted = level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.BLASTING, input, level)
				.map(h -> h.value().assemble(input)).orElse(ItemStack.EMPTY);
		check(helper, melted.is(Items.COPPER_INGOT) && melted.getCount() == 2, "a blast furnace melts a cathode to two copper ingots, got " + melted);
		helper.succeedWhen(() -> {
			int cathodes = t.countIn(OUT, new ItemStack(TailingsItems.COPPER_CATHODE));
			check(helper, cathodes == 21 && t.smelter().copper() < 1e-6, "three anodes (two by hand, one by hopper) make 21 cathodes, got " + cathodes + all(t));
			check(helper, t.status().startsWith("NO ANODES"), "then it waits for anodes: " + t.status());
			check(helper, Math.abs(ka[0] - Smelter.CELL_KA) < 0.2, "its rectifier held the cells' 20.5 kA: " + ka[0] + " kA; " + rectifier.inspect().getString());
		});
	}

	/**
	 * smelter_acid: the flash furnace at its own pace, its off-gas piped from the east face of its back cell's top layer down to the
	 * north face of a Wildcatter acid plant converter, a final absorber against it kept in water: the converter takes the smelter's burner
	 * gas and makes acid.
	 */
	private static void acid(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity f = TailingsGameTests.plant(helper, Machine.FLASH_FURNACE, new BlockPos(2, y, 0));
		f.forceRunning();
		TailingsGameTests.plant(helper, Machine.COPPER_CONVERTER, new BlockPos(2, y, 16));
		gas(helper, new BlockPos(2, y, 0), 7);
		f.offer(new ItemStack(Items.SAND));
		f.smelter().add(1000);
		for (int up = 0; up <= 11; up++) {
			TailingsGameTests.pipe(helper, new BlockPos(9, y + up, 15));
		}
		TailingsGameTests.pipe(helper, new BlockPos(9, y, 16), new BlockPos(10, y, 16));
		var converter = TailingsGameTests.part(helper, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "converter")), new BlockPos(10, y, 17),
				dev.wildcatter.block.ConverterBlockEntity.class);
		var absorber = TailingsGameTests.part(helper, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "final_absorber")), new BlockPos(15, y, 17),
				dev.wildcatter.block.FinalAbsorberBlockEntity.class);
		helper.onEachTick(() -> absorber.addWater(400));
		helper.succeedWhen(() -> {
			check(helper, f.smelter().sulfurKg() > 0, "the furnace smelts: " + f.inspect().getString());
			check(helper, converter.inspect().getString().contains("% converted"), "the acid plant's converter makes acid from its gas: " + converter.inspect().getString() + " | "
					+ absorber.inspect().getString() + " | " + f.inspect().getString());
		});
	}
}
