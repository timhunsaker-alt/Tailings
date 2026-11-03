package dev.wildcatter.tailings.block;

import net.minecraft.util.StringRepresentable;

/** How the mine rail sits on its bed. Flat is the level drive. The others carry the rack. */
public enum RailGrade implements StringRepresentable {
	FLAT("flat"),
	LOWER("lower"),
	UPPER("upper"),
	STEEP("steep");

	private final String name;

	RailGrade(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
