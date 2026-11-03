package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.HandMillBlockEntity;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Headless scenes for the starter line. {@code ./gradlew :neoforge:runGameTestServer} */
@Mod("tailings_gametest")
public class TailingsGameTests {
	private static final String[] SCENES = {"registers", "stamp", "magnet"};

	public TailingsGameTests(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, event -> event.register(Registries.TEST_FUNCTION, helper -> {
			helper.register(id("registers"), (Consumer<GameTestHelper>) TailingsGameTests::registers);
			helper.register(id("stamp"), (Consumer<GameTestHelper>) TailingsGameTests::stamp);
			helper.register(id("magnet"), (Consumer<GameTestHelper>) TailingsGameTests::magnet);
		}));
		modBus.addListener(RegisterGameTestsEvent.class, event -> {
			Holder<TestEnvironmentDefinition<?>> plain = event.registerEnvironment(id("plain"));
			for (String scene : SCENES) {
				event.registerTest(id(scene), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(scene)),
						new TestData<>(plain, Identifier.withDefaultNamespace("empty"), 200, 1, true)));
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
}
