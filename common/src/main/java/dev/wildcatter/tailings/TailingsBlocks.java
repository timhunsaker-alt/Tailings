package dev.wildcatter.tailings;

import dev.wildcatter.tailings.block.HandMillBlock;
import dev.wildcatter.tailings.block.HandMillBlock.Role;
import dev.wildcatter.tailings.block.HandMillBlockEntity;
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

/** The starter machines. The true-size crushing circuit is a later phase. */
public final class TailingsBlocks {
	public static final Block STAMP_MILL = mill("stamp_mill", Role.STAMP_MILL, MapColor.STONE);
	public static final Block HAND_SCREEN = mill("hand_screen", Role.HAND_SCREEN, MapColor.WOOD);
	public static final Block SHAKING_TABLE = mill("shaking_table", Role.SHAKING_TABLE, MapColor.METAL);
	public static final Block MAGNETIC_SEPARATOR = mill("magnetic_separator", Role.MAGNETIC_SEPARATOR, MapColor.COLOR_GRAY);
	public static final BlockEntityType<HandMillBlockEntity> MILL = new BlockEntityType<>(HandMillBlockEntity::new,
			Set.of(STAMP_MILL, HAND_SCREEN, SHAKING_TABLE, MAGNETIC_SEPARATOR));

	private TailingsBlocks() {
	}

	public static void register() {
		add("stamp_mill", STAMP_MILL);
		add("hand_screen", HAND_SCREEN);
		add("shaking_table", SHAKING_TABLE);
		add("magnetic_separator", MAGNETIC_SEPARATOR);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("hand_mill"), MILL);
	}

	private static Block mill(String name, Role role, MapColor color) {
		return block(name, props -> new HandMillBlock(props.mapColor(color).strength(2.5F, 6.0F).sound(SoundType.METAL)
				.requiresCorrectToolForDrops(), role));
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
