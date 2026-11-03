package dev.wildcatter.tailings;

import dev.wildcatter.tailings.block.HandMillBlock;
import dev.wildcatter.tailings.block.HandMillBlock.Role;
import dev.wildcatter.tailings.block.HandMillBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.PlantMachineBlock;
import dev.wildcatter.tailings.plant.Machine;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The starter machines, and the true-size crushing circuit and flotation bank. */
public final class TailingsBlocks {
	public static final Block STAMP_MILL = mill("stamp_mill", Role.STAMP_MILL, MapColor.STONE);
	public static final Block HAND_SCREEN = mill("hand_screen", Role.HAND_SCREEN, MapColor.WOOD);
	public static final Block SHAKING_TABLE = mill("shaking_table", Role.SHAKING_TABLE, MapColor.METAL);
	public static final Block MAGNETIC_SEPARATOR = mill("magnetic_separator", Role.MAGNETIC_SEPARATOR, MapColor.COLOR_GRAY);
	public static final BlockEntityType<HandMillBlockEntity> MILL = new BlockEntityType<>(HandMillBlockEntity::new,
			Set.of(STAMP_MILL, HAND_SCREEN, SHAKING_TABLE, MAGNETIC_SEPARATOR));
	public static final Block GRIZZLY = plant(Machine.GRIZZLY, MapColor.STONE);
	public static final Block JAW = plant(Machine.JAW, MapColor.COLOR_GRAY);
	public static final Block CONE = plant(Machine.CONE, MapColor.COLOR_GRAY);
	public static final Block SCREEN = plant(Machine.SCREEN, MapColor.METAL);
	public static final Block BELT = plant(Machine.BELT, MapColor.COLOR_BLACK);
	public static final Block BALL_MILL = plant(Machine.BALL_MILL, MapColor.COLOR_GRAY);
	public static final Block CYCLONE = plant(Machine.CYCLONE, MapColor.COLOR_ORANGE);
	public static final Block CONDITIONER = plant(Machine.CONDITIONER, MapColor.COLOR_LIGHT_GRAY);
	public static final Block CELL = plant(Machine.CELL, MapColor.COLOR_LIGHT_BLUE);
	public static final Block BLOWER = plant(Machine.BLOWER, MapColor.COLOR_BLUE);
	public static final Block THICKENER = plant(Machine.THICKENER, MapColor.TERRACOTTA_BROWN);
	public static final Block FILTER = plant(Machine.FILTER, MapColor.TERRACOTTA_WHITE);
	public static final BlockEntityType<PlantBlockEntity> PLANT = new BlockEntityType<>(PlantBlockEntity::new,
			Set.of(GRIZZLY, JAW, CONE, SCREEN, BELT, BALL_MILL, CYCLONE, CONDITIONER, CELL, BLOWER, THICKENER, FILTER));

	private TailingsBlocks() {
	}

	public static void register() {
		add("stamp_mill", STAMP_MILL);
		add("hand_screen", HAND_SCREEN);
		add("shaking_table", SHAKING_TABLE);
		add("magnetic_separator", MAGNETIC_SEPARATOR);
		for (Machine machine : Machine.values()) {
			add(machine.id(), plantBlock(machine));
		}
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("hand_mill"), MILL);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("plant"), PLANT);
	}

	public static Block plantBlock(Machine machine) {
		return switch (machine) {
			case GRIZZLY -> GRIZZLY;
			case JAW -> JAW;
			case CONE -> CONE;
			case SCREEN -> SCREEN;
			case BELT -> BELT;
			case BALL_MILL -> BALL_MILL;
			case CYCLONE -> CYCLONE;
			case CONDITIONER -> CONDITIONER;
			case CELL -> CELL;
			case BLOWER -> BLOWER;
			case THICKENER -> THICKENER;
			case FILTER -> FILTER;
		};
	}

	private static Block mill(String name, Role role, MapColor color) {
		return block(name, props -> new HandMillBlock(props.mapColor(color).strength(2.5F, 6.0F).sound(SoundType.METAL)
				.requiresCorrectToolForDrops(), role));
	}

	private static Block plant(Machine machine, MapColor color) {
		return block(machine.id(), props -> new PlantMachineBlock(props.mapColor(color).strength(3.5F, 8.0F).sound(SoundType.METAL)
				.requiresCorrectToolForDrops(), machine));
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Tailings.id(name));
		return factory.apply(BlockBehaviour.Properties.of().setId(key));
	}

	private static void add(String name, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, Tailings.id(name), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Tailings.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix()));
	}
}
