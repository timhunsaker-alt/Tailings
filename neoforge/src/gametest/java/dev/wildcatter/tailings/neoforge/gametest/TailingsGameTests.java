package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.HandMillBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.PlantMachineBlock;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantPlan;
import dev.wildcatter.tailings.plant.Reagent;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Headless scenes for the starter line. {@code ./gradlew :neoforge:runGameTestServer} */
@Mod("tailings_gametest")
public class TailingsGameTests {
	private static final String[] SCENES = {"registers", "stamp", "magnet", "jaw_size", "crushing", "flotation"};

	public TailingsGameTests(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, event -> event.register(Registries.TEST_FUNCTION, helper -> {
			helper.register(id("registers"), (Consumer<GameTestHelper>) TailingsGameTests::registers);
			helper.register(id("stamp"), (Consumer<GameTestHelper>) TailingsGameTests::stamp);
			helper.register(id("magnet"), (Consumer<GameTestHelper>) TailingsGameTests::magnet);
			helper.register(id("jaw_size"), (Consumer<GameTestHelper>) TailingsGameTests::jawSize);
			helper.register(id("crushing"), (Consumer<GameTestHelper>) TailingsGameTests::crushing);
			helper.register(id("flotation"), (Consumer<GameTestHelper>) TailingsGameTests::flotation);
		}));
		modBus.addListener(RegisterGameTestsEvent.class, event -> {
			Holder<TestEnvironmentDefinition<?>> plain = event.registerEnvironment(id("plain"));
			for (String scene : SCENES) {
				event.registerTest(id(scene), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(scene)),
						new TestData<>(plain, Identifier.withDefaultNamespace("empty"), 400, 1, true)));
			}
		});
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("tailings_gametest", path);
	}

	private static void registers(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Tailings.id("stamp_mill")), Component.literal("stamp mill missing"));
		helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Tailings.id("crushed_granite")), Component.literal("crushed granite missing"));
		helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Tailings.id("sand_iron")), Component.literal("iron sand missing"));
		helper.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(Tailings.id("tailings")), Component.literal("creative tab missing"));
		helper.succeed();
	}

	private static void stamp(GameTestHelper helper) {
		helper.setBlock(0, 0, 0, Blocks.STONE);
		helper.setBlock(0, 1, 0, TailingsBlocks.STAMP_MILL);
		HandMillBlockEntity mill = helper.getBlockEntity(new BlockPos(0, 1, 0), HandMillBlockEntity.class);
		helper.assertTrue(mill.offer(new ItemStack(Blocks.COBBLESTONE)), Component.literal("the stamp mill should take cobble"));
		helper.succeedWhen(() -> helper.assertTrue(mill.output(0).is(BuiltInRegistries.ITEM.getValue(Tailings.id("crushed_cobblestone"))),
				Component.literal("stamp mill should crush cobble, got " + mill.output(0))));
	}

	private static void magnet(GameTestHelper helper) {
		helper.setBlock(0, 0, 0, Blocks.STONE);
		helper.setBlock(0, 1, 0, TailingsBlocks.MAGNETIC_SEPARATOR);
		HandMillBlockEntity mill = helper.getBlockEntity(new BlockPos(0, 1, 0), HandMillBlockEntity.class);
		ItemStack sand = new ItemStack(BuiltInRegistries.ITEM.getValue(Tailings.id("sand_iron")));
		helper.assertTrue(mill.offer(sand), Component.literal("the separator should take iron sand"));
		helper.succeedWhen(() -> {
			ItemStack out = mill.output(0);
			helper.assertTrue(out.is(Items.IRON_INGOT) && out.getCount() == 2, Component.literal("magnetic separation should pay 2 iron ingots, got " + out));
		});
	}

	private static void erect(GameTestHelper helper, java.util.List<PlantPlan.Station> plan, int y) {
		for (PlantPlan.Station station : plan) {
			PlantMachineBlock block = (PlantMachineBlock) TailingsBlocks.plantBlock(station.machine());
			BlockPos pos = new BlockPos(station.x(), y, 0);
			BlockState state = block.defaultBlockState().setValue(PlantMachineBlock.FACING, Direction.NORTH).setValue(PlantMachineBlock.PIECE, 0);
			helper.setBlock(pos, state);
		}
	}

	private static PlantBlockEntity plant(GameTestHelper helper, java.util.List<PlantPlan.Station> plan, Machine machine, int y) {
		for (PlantPlan.Station station : plan) {
			if (station.machine() == machine) {
				return helper.getBlockEntity(new BlockPos(station.x(), y, 0), PlantBlockEntity.class);
			}
		}
		throw new IllegalStateException(machine.toString());
	}

	/** The jaw is one item and 3 x 4 x 4 cells. */
	private static void jawSize(GameTestHelper helper) {
		PlantMachineBlock jaw = (PlantMachineBlock) TailingsBlocks.JAW;
		BlockPos pos = new BlockPos(0, 2, 0);
		BlockState state = jaw.defaultBlockState().setValue(PlantMachineBlock.FACING, Direction.NORTH).setValue(PlantMachineBlock.PIECE, 0);
		helper.setBlock(pos, state);
		jaw.fill(helper.getLevel(), helper.absolutePos(pos), state);
		int cells = 0;
		for (BlockPos cell : jaw.cells(helper.absolutePos(pos), Direction.NORTH)) {
			if (helper.getLevel().getBlockState(cell).getBlock() == jaw) {
				cells++;
			}
		}
		helper.assertTrue(cells == Machine.JAW.cells(), Component.literal("jaw should fill " + Machine.JAW.cells() + " cells, got " + cells));
		helper.succeed();
	}

	private static void crushing(GameTestHelper helper) {
		int y = 6;
		erect(helper, PlantPlan.CRUSHING, y);
		PlantBlockEntity jaw = plant(helper, PlantPlan.CRUSHING, Machine.JAW, y);
		jaw.refresh();
		jaw.forceRunning();
		helper.assertTrue(plant(helper, PlantPlan.CRUSHING, Machine.GRIZZLY, y).offer(new ItemStack(Blocks.COBBLESTONE)),
				Component.literal("the grizzly should take cobble"));
		helper.succeedWhen(() -> {
			ItemStack out = plant(helper, PlantPlan.CRUSHING, Machine.CYCLONE, y).product();
			helper.assertTrue(jaw.formed() && out.is(BuiltInRegistries.ITEM.getValue(Tailings.id("fines_cobblestone"))),
					Component.literal("the circuit should make fines, got " + out + " (" + jaw.status() + ")"));
		});
	}

	private static void flotation(GameTestHelper helper) {
		int y = 16;
		erect(helper, PlantPlan.FLOTATION, y);
		PlantBlockEntity tank = plant(helper, PlantPlan.FLOTATION, Machine.CONDITIONER, y);
		tank.forceRunning();
		ItemStack fines = new ItemStack(BuiltInRegistries.ITEM.getValue(Tailings.id("fines_copper")));
		helper.assertTrue(tank.offer(fines), Component.literal("the tank should take copper fines"));
		for (Reagent reagent : new Reagent[] {Reagent.LIME, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC}) {
			helper.assertTrue(tank.offer(new ItemStack(BuiltInRegistries.ITEM.getValue(Tailings.id(reagent.id())))),
					Component.literal("the tank should take " + reagent.displayName()));
		}
		helper.succeedWhen(() -> {
			ItemStack out = plant(helper, PlantPlan.FLOTATION, Machine.FILTER, 16).product();
			helper.assertTrue(tank.formed() && out.is(Items.COPPER_INGOT) && out.getCount() == 3,
					Component.literal("flotation should pay 3 copper ingots, got " + out + " formed=" + tank.formed()));
		});
	}
}
