package dev.wildcatter.tailings.pit;

import java.util.Map;
import java.util.Set;

/**
 * What a block of each thing a pit moves weighs, and which of it a bucket digs as it stands (docs/multiblocks/open-pit.md, Rock,
 * broken rock and dug ground). Rock and ore are 2.7 t a block, as the plant counts them ({@code PlantRates.BLOCK_KG}); soil is
 * lighter, at bank density (proposed).
 */
public final class PitMass {
	/** A bank block of rock or ore, as the plant counts it. */
	public static final int ROCK_KG = 2_700;
	/** A muck block holds three quarters of a bank block: 3 blocks of rock break to 4 of muck. */
	public static final int MUCK_QUARTERS = 3;

	/** Soil: overburden that goes to dumps, never to the plant (decided with Tim, 2026-11-09). Kilograms a block. */
	private static final Map<String, Integer> SOIL = Map.of(
			"minecraft:dirt", 1_600,
			"minecraft:coarse_dirt", 1_600,
			"minecraft:rooted_dirt", 1_600,
			"minecraft:sand", 1_600,
			"minecraft:red_sand", 1_600,
			"minecraft:gravel", 1_900,
			"minecraft:clay", 2_000,
			"minecraft:mud", 1_800);

	/** Wildcatter's soft ores: a bucket digs them as they stand, and they go to the plant. */
	private static final Set<String> SOFT_ORES = Set.of(
			"wildcatter:bauxite_ore",
			"wildcatter:phosphate_ore",
			"wildcatter:heavy_mineral_ore",
			"wildcatter:bentonite_ore",
			"wildcatter:borax_ore");

	private PitMass() {
	}

	public static boolean soil(String itemId) {
		return SOIL.containsKey(itemId);
	}

	/** A bucket digs it where it stands, without a blast. */
	public static boolean soft(String itemId) {
		return SOIL.containsKey(itemId) || SOFT_ORES.contains(itemId);
	}

	/** Kilograms in one block of it. */
	public static int kg(String itemId) {
		return SOIL.getOrDefault(itemId, ROCK_KG);
	}

	/** What a dug block counts as: a grass block is its dirt, as when it is broken. */
	public static String dugAs(String blockId) {
		return switch (blockId) {
			case "minecraft:grass_block", "minecraft:podzol", "minecraft:mycelium", "minecraft:dirt_path", "minecraft:farmland" -> "minecraft:dirt";
			default -> blockId;
		};
	}
}
