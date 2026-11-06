package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.mine.Ground;
import net.minecraft.util.StringRepresentable;

/** The rock a floor piece was cut from. The model uses that rock's texture. */
public enum FloorLook implements StringRepresentable {
	STONE("stone"),
	COBBLE("cobble"),
	DEEPSLATE("deepslate"),
	GRANITE("granite"),
	DIORITE("diorite"),
	ANDESITE("andesite"),
	TUFF("tuff"),
	SANDSTONE("sandstone"),
	/** Gravel ballast: the bed under a mine rail (track only; decided with Tim, R6, flat and decline). */
	BALLAST("ballast");

	private final String name;

	FloorLook(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}

	public static FloorLook from(Ground ground) {
		return switch (ground) {
			case COBBLE -> COBBLE;
			case DEEPSLATE -> DEEPSLATE;
			case GRANITE -> GRANITE;
			case DIORITE -> DIORITE;
			case ANDESITE -> ANDESITE;
			case TUFF -> TUFF;
			case SANDSTONE -> SANDSTONE;
			default -> STONE;
		};
	}
}
