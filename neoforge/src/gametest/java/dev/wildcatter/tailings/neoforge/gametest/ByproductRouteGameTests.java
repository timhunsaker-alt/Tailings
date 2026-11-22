package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.block.PlantBlockEntity;
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
import net.minecraft.world.item.ItemStack;

/**
 * The by-product plant's feed from the concentrator (docs/questions/copper-smelter.md, items 1-3; the rev2 audit's possible bug): a
 * pressure filter fed thickened concentrate carrying one item's worth of pentlandite, or of molybdenite, packs it into an item the
 * by-product plant takes, and the plant pays the cobalt, or the rhenium, it carries. Scheelite and chromite pay a concentrate that
 * crafts into the Wildcatter mineral its plants take.
 */
final class ByproductRouteGameTests {
	static final String[] SCENES = {"byproduct_pentlandite", "byproduct_molybdenite", "filter_scheelite", "byproduct_hopper"};
	static final Set<String> LONG = Set.of("byproduct_pentlandite", "byproduct_molybdenite", "filter_scheelite", "byproduct_hopper");
	private static final int[] OUT = {1, 2};

	private ByproductRouteGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("byproduct_pentlandite", helper -> route(helper, Mineral.PENTLANDITE, "tailings:cobalt"));
		out.accept("byproduct_molybdenite", helper -> route(helper, Mineral.MOLYBDENITE, "tailings:rhenium"));
		out.accept("filter_scheelite", ByproductRouteGameTests::scheelite);
		out.accept("byproduct_hopper", ByproductRouteGameTests::hopper);
	}

	/**
	 * byproduct_hopper: a hopper on the by-product plant's top feeds it lead ingots, as a hand does (Rock to Metal episode 7 found only a
	 * hand could); it gives them back with their silver; a hopper under it takes them out; a cobblestone in the hopper stays there.
	 */
	private static void hopper(GameTestHelper helper) {
		PlantBlockEntity plant = TailingsGameTests.plant(helper, Machine.BYPRODUCT_PLANT, new BlockPos(0, 4, 0));
		plant.forceRunning();
		BlockPos top = new BlockPos(2, 4 + Machine.BYPRODUCT_PLANT.height(), 2);
		helper.setBlock(top, net.minecraft.world.level.block.Blocks.HOPPER);
		var hopper = helper.getBlockEntity(top, net.minecraft.world.level.block.entity.HopperBlockEntity.class);
		hopper.setItem(0, Feeds.stack("wildcatter:lead_ingot", 3));
		hopper.setItem(1, new ItemStack(net.minecraft.world.item.Items.COBBLESTONE, 1));
		helper.succeedWhen(() -> {
			int back = plant.countIn(OUT, Feeds.stack("wildcatter:lead_ingot", 1));
			double silver = plant.byproducts().getOrDefault("wildcatter:silver_ingot", 0.0) + 10 * plant.countIn(OUT, Feeds.stack("wildcatter:silver_ingot", 1));
			helper.assertTrue(hopper.getItem(0).isEmpty(), Component.literal("the hopper should feed its lead ingots in, still holds " + hopper.getItem(0) + "; " + outputs(plant)));
			helper.assertTrue(back >= 1 && silver > 0, Component.literal("and the plant give them back with silver: " + outputs(plant)));
			helper.assertTrue(hopper.getItem(1).is(net.minecraft.world.item.Items.COBBLESTONE), Component.literal("cobblestone is not its feed"));
		});
	}

	/** A filter with {@code kg} of thickened concentrate on its inlet, carrying one item's worth of {@code mineral}. */
	private static PlantBlockEntity filter(GameTestHelper helper, BlockPos at, Mineral mineral) {
		PlantBlockEntity filter = TailingsGameTests.plant(helper, Machine.FILTER, at);
		double kg = 1000;
		filter.back().offer(Product.THICKENED_CONCENTRATE, kg, SlurryAssay.of(Map.of(mineral.mineralItemId(), mineral.itemAmount().doubleValue() / kg)));
		filter.forceRunning();
		return filter;
	}

	private static String outputs(PlantBlockEntity part) {
		return part.getItem(1) + " " + part.getItem(2) + " (" + part.status() + ")";
	}

	/**
	 * byproduct_pentlandite, byproduct_molybdenite: the filter's packed item goes, as a hopper would carry it, into the by-product plant,
	 * which must take it and pay what it carries.
	 */
	private static void route(GameTestHelper helper, Mineral mineral, String byproduct) {
		PlantBlockEntity filter = filter(helper, new BlockPos(0, 4, 0), mineral);
		PlantBlockEntity plant = TailingsGameTests.plant(helper, Machine.BYPRODUCT_PLANT, new BlockPos(0, 4, 8));
		plant.forceRunning();
		ItemStack packed = Feeds.stack(mineral.phaseOneProductId(), 1);
		boolean[] handed = {false};
		helper.onEachTick(() -> {
			if (!handed[0] && !packed.isEmpty() && filter.countIn(OUT, packed) > 0 && plant.offer(packed.copy())) {
				filter.removeFrom(OUT, packed, 1);
				handed[0] = true;
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(!packed.isEmpty(), Component.literal("the filter packs " + mineral + " into " + mineral.phaseOneProductId() + ", which is not a registered item; the filter holds "
					+ outputs(filter)));
			helper.assertTrue(handed[0], Component.literal("the by-product plant should take the filter's " + mineral.phaseOneProductId() + "; the filter holds " + outputs(filter)));
			helper.assertTrue(plant.countIn(OUT, packed) == 1, Component.literal("and give it back: " + outputs(plant)));
			double paid = plant.byproducts().getOrDefault(byproduct, 0.0) + 10 * plant.countIn(OUT, Feeds.stack(byproduct, 1));
			double expected = mineral.itemAmount().doubleValue() * dev.wildcatter.tailings.plant.ByProducts.of(mineral).get(0).grade();
			helper.assertTrue(Math.abs(paid - expected) < 1e-3, Component.literal("with " + expected + " kg of " + byproduct + ", got " + paid));
		});
	}

	/**
	 * filter_scheelite: the filter pays scheelite as a concentrate that crafts one for one into Wildcatter's scheelite, the mineral its
	 * roaster's feed names; chromite's concentrate crafts into the chromite Wildcatter's chromate kiln takes.
	 */
	private static void scheelite(GameTestHelper helper) {
		PlantBlockEntity filter = filter(helper, new BlockPos(0, 4, 0), Mineral.SCHEELITE);
		helper.succeedWhen(() -> {
			ItemStack paid = Feeds.stack(Mineral.SCHEELITE.phaseOneProductId(), 1);
			helper.assertTrue(filter.countIn(OUT, paid) == 1, Component.literal("the filter should pay one scheelite concentrate: " + outputs(filter)));
			for (Mineral mineral : new Mineral[] {Mineral.SCHEELITE, Mineral.CHROMITE, Mineral.PENTLANDITE, Mineral.MOLYBDENITE}) {
				ItemStack made = crafted(helper, Feeds.stack(mineral.concentrateId(), 1));
				helper.assertTrue(BuiltInRegistries.ITEM.getKey(made.getItem()).toString().equals(mineral.mineralItemId()) && made.getCount() == 1,
						Component.literal(mineral.concentrateId() + " should craft into one " + mineral.mineralItemId() + ", got " + made));
			}
		});
	}

	/** What one item alone in a crafting grid makes. */
	private static ItemStack crafted(GameTestHelper helper, ItemStack one) {
		var level = helper.getLevel();
		var input = net.minecraft.world.item.crafting.CraftingInput.of(1, 1, java.util.List.of(one));
		return level.getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level)
				.map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
	}
}
