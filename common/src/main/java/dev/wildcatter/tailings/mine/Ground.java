package dev.wildcatter.tailings.mine;

/** What a cell of ground is, for the drill. Liquids are cleared before the cut; ore can stop the program. */
public enum Ground {
	AIR,
	WATER,
	FLOWING_WATER,
	LAVA,
	FLOWING_LAVA,
	STONE,
	COBBLE,
	DEEPSLATE,
	GRANITE,
	DIORITE,
	ANDESITE,
	TUFF,
	SANDSTONE,
	GRAVEL,
	SAND,
	DIRT,
	OBSIDIAN,
	ORE,
	OTHER,
	UNKNOWN_LIQUID,
	BEDROCK,
	LINING,
	FLOOR;

	public boolean isLiquid() {
		return this == WATER || this == FLOWING_WATER || this == LAVA || this == FLOWING_LAVA || this == UNKNOWN_LIQUID;
	}

	public boolean isLava() {
		return this == LAVA || this == FLOWING_LAVA;
	}

	public boolean isWater() {
		return this == WATER || this == FLOWING_WATER;
	}

	/** Rock the track layer may cut into a floor piece. Ore, gravel, sand and dirt go out as muck. */
	public boolean isFloorRock() {
		return switch (this) {
			case STONE, COBBLE, DEEPSLATE, GRANITE, DIORITE, ANDESITE, TUFF, SANDSTONE -> true;
			default -> false;
		};
	}

	/** Rock the cars can take. Air, liquids, bedrock and blocks the drill itself placed are not muck. */
	public boolean isMuck() {
		return this != AIR && !isLiquid() && this != BEDROCK && this != LINING && this != FLOOR;
	}
}
