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
	/** The tailings filter's cake (open-pit-p4-dry-stack.md): a placed block is 1 m³ at 1.8 t (open-pit.md, proposed). */
	public static final String DRY_TAILS = "tailings:dry_tails";
	public static final int DRY_TAILS_KG = 1_800;

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

	/** Tipped on the ground it lands as its own block, not as muck: soil and dry tails. Neither goes to the plant. */
	public static boolean ownBlock(String itemId) {
		return SOIL.containsKey(itemId) || DRY_TAILS.equals(itemId);
	}

	/** A bucket digs it where it stands, without a blast; a dozer's blade pushes it. */
	public static boolean soft(String itemId) {
		return SOIL.containsKey(itemId) || SOFT_ORES.contains(itemId) || DRY_TAILS.equals(itemId);
	}

	/** Kilograms in one block of it. */
	public static int kg(String itemId) {
		return DRY_TAILS.equals(itemId) ? DRY_TAILS_KG : SOIL.getOrDefault(itemId, ROCK_KG);
	}

	/** What a dug block counts as: a grass block is its dirt, as when it is broken. */
	public static String dugAs(String blockId) {
		return switch (blockId) {
			case "minecraft:grass_block", "minecraft:podzol", "minecraft:mycelium", "minecraft:dirt_path", "minecraft:farmland" -> "minecraft:dirt";
			default -> blockId;
		};
	}
}
