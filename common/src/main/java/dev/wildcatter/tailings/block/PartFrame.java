package dev.wildcatter.tailings.block;

import dev.wildcatter.block.Lines;
import dev.wildcatter.multiblock.BigParts;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A part's own frame (each part drawn facing north, main cell at the north-west bottom corner; x east, y up, z south), turned the
 * way the part faces, as the specs' "Nozzles and faces" are written. The same arithmetic as Wildcatter's lease parts.
 */
public final class PartFrame {
	private PartFrame() {
	}

	/** {@code cell} in the part's own frame, {x, y, z}. */
	public static int[] local(BlockPos main, BlockState state, BlockPos cell) {
		int dx = cell.getX() - main.getX(), dz = cell.getZ() - main.getZ();
		for (int i = 0; i < BigParts.turns(state); i++) {
			int t = dx;
			dx = dz;
			dz = -t;
		}
		return new int[] {dx, cell.getY() - main.getY(), dz};
	}

	/** A side written in the part's own frame, turned the way the part faces. */
	public static Direction world(BlockState state, Direction local) {
		Direction out = local;
		for (int i = 0; local.getAxis().isHorizontal() && i < BigParts.turns(state); i++) {
			out = out.getClockWise();
		}
		return out;
	}

	/** The faces of the cells that match {@code where} (own frame) on the side {@code localSide}. */
	public static List<Lines.Face> faces(BlockPos main, BlockState state, Predicate<int[]> where, Direction localSide) {
		List<Lines.Face> out = new ArrayList<>();
		Direction side = world(state, localSide);
		for (BlockPos cell : BigParts.cells(main, state)) {
			if (where.test(local(main, state, cell))) {
				out.add(new Lines.Face(cell, side));
			}
		}
		return out;
	}

	/** Every outward face of the part on the side {@code localSide}: the cells with nothing of the part beyond them. */
	public static List<Lines.Face> side(BlockPos main, BlockState state, Direction localSide) {
		List<BlockPos> cells = BigParts.cells(main, state);
		Direction side = world(state, localSide);
		List<Lines.Face> out = new ArrayList<>();
		for (BlockPos cell : cells) {
			if (!cells.contains(cell.relative(side))) {
				out.add(new Lines.Face(cell, side));
			}
		}
		return out;
	}

	/** Every outward face of the part, all sides. */
	public static List<Lines.Face> all(BlockPos main, BlockState state) {
		return Lines.faces(BigParts.cells(main, state));
	}
}
