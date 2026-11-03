package dev.wildcatter.tailings.block;

import net.minecraft.util.StringRepresentable;

/** Lower is 0 to 0.5, upper is 0.5 to 1, steep is a full block. Two halves are one block of rise. */
public enum SlopeHalf implements StringRepresentable {
	LOWER("lower"),
	UPPER("upper"),
	STEEP("steep");

	private final String name;

	SlopeHalf(String name) {
		this.name = name;
	}

	@Override
	public String getSerializedName() {
		return name;
	}
}
