package dev.wildcatter.tailings.client;

import dev.wildcatter.tailings.block.RailGrade;
import net.minecraft.core.Direction;

/** Height of the rail surface above the bottom of its block. {@code alongDownhill} is 0 at the low edge and 1 at the high edge. */
public final class RailSurface {
	private RailSurface() {
	}

	public static double blocksAboveBottom(RailGrade grade, double alongDownhill) {
		double t = Math.min(1.0, Math.max(0.0, alongDownhill));
		return switch (grade) {
			case FLAT -> 1.0;
			case LOWER -> t * 0.5;
			case UPPER -> 0.5 + t * 0.5;
			case STEEP -> t;
		};
	}

	/** 0 on the downhill face of the block, 1 on the uphill face. */
	public static double alongDownhill(Direction downhill, int blockX, int blockZ, double x, double z) {
		return switch (downhill) {
			case NORTH -> z - blockZ;
			case SOUTH -> blockZ + 1.0 - z;
			case EAST -> blockX + 1.0 - x;
			case WEST -> x - blockX;
			default -> 0.5;
		};
	}
}
