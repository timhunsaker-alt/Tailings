package dev.wildcatter.tailings.block;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.mine.TbmPart;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The machine's layout (docs/multiblocks/tunnel-boring-machine.md): every cell of its parts, as block states, from the cutterhead's
 * main cell. The cab checks it standing, the contraption lifts and sets down by it, and the renderer draws it.
 */
public final class TbmMachine {
	/** One cell: its state, and where it is from the cutterhead's main cell. */
	public record Cell(BlockState state, int x, int y, int z) {
	}

	/** The old dewatering pump's main cell, against the main beam's tail, in the cutterhead's frame. */
	private static final int[] PUMP = {3, 3, 11};
	private static final Map<Direction, List<Cell>> CELLS = new EnumMap<>(Direction.class);

	private TbmMachine() {
	}

	/** A place in the cutterhead's own frame (x across, y up, z back along the machine), turned the way the machine faces. */
	public static BlockPos offset(Direction facing, int x, int y, int z) {
		int dx = x, dz = z;
		for (int i = 0; i < DewateringPumpBlock.turns(facing); i++) {
			int t = dx;
			dx = -dz;
			dz = t;
		}
		return new BlockPos(dx, y, dz);
	}

	/** A part's main cell, from the cutterhead's. */
	public static BlockPos main(TbmPart part, BlockPos cutter, Direction facing) {
		return cutter.offset(offset(facing, part.across(), part.up(), TbmPart.CUTTERHEAD.ahead() - part.ahead()));
	}

	public static BlockPos cutterFromBeam(BlockPos beam, Direction facing) {
		BlockPos from = offset(facing, TbmPart.MAIN_BEAM.across(), TbmPart.MAIN_BEAM.up(), TbmPart.CUTTERHEAD.ahead() - TbmPart.MAIN_BEAM.ahead());
		return beam.subtract(from);
	}

	public static BlockPos pump(BlockPos cutter, Direction facing) {
		return cutter.offset(offset(facing, PUMP[0], PUMP[1], PUMP[2]));
	}

	/** Every cell of the machine facing {@code facing}: its four parts and the old pump. */
	public static synchronized List<Cell> cells(Direction facing) {
		return CELLS.computeIfAbsent(facing, TbmMachine::build);
	}

	private static List<Cell> build(Direction facing) {
		List<Cell> out = new ArrayList<>();
		for (TbmPart part : TbmPart.values()) {
			BigPartBlock block = (BigPartBlock) TailingsBlocks.tbmBlock(part);
			BlockPos main = main(part, BlockPos.ZERO, facing);
			BlockState state = block.defaultBlockState().setValue(BigPartBlock.FACING, facing);
			for (int piece = 0; piece < block.footprint().pieces(); piece++) {
				int[] at = block.footprint().offset(piece, DewateringPumpBlock.turns(facing));
				out.add(new Cell(state.setValue(block.piece(), piece), main.getX() + at[0], main.getY() + at[1], main.getZ() + at[2]));
			}
		}
		DewateringPumpBlock pump = (DewateringPumpBlock) TailingsBlocks.DEWATERING_PUMP;
		BlockPos main = pump(BlockPos.ZERO, facing);
		for (int piece = 0; piece < 8; piece++) {
			int[] at = dev.wildcatter.multiblock.Footprint.box(2, 2, 2, 0, 0).offset(piece, DewateringPumpBlock.turns(facing));
			out.add(new Cell(pump.defaultBlockState().setValue(DewateringPumpBlock.FACING, facing).setValue(DewateringPumpBlock.PIECE, piece),
					main.getX() + at[0], main.getY() + at[1], main.getZ() + at[2]));
		}
		return out;
	}
}
