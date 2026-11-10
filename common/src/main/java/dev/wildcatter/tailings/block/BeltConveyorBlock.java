package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.plant.Machine;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One metre of a belt conveyor run (docs/multiblocks/crushing-circuit.md): a 600 mm troughed belt on idlers, laid block by block
 * along one horizontal axis like a pipe run, or a block up or down for a grade. It has no motor of its own: the {@link
 * Machine#CONVEYOR_DRIVE} at the run's head end drives it and holds its load, and a run with no drive carries nothing.
 */
public class BeltConveyorBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	/** The frame and belt: 0.8 m wide, its belt 0.75 m up. */
	private static final VoxelShape X = Shapes.or(Block.box(0, 0, 1.6, 16, 12, 14.4));
	private static final VoxelShape Z = Shapes.or(Block.box(1.6, 0, 0, 14.4, 12, 16));
	/** A run is never followed past this many blocks. */
	public static final int MAX_RUN = 512;

	public BeltConveyorBlock(Properties properties) {
		super(properties.noOcclusion());
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS);
	}

	/** It runs the way the player faces. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis());
	}

	/** The drive that drives a run of this belt. */
	public Machine drive() {
		return Machine.CONVEYOR_DRIVE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AXIS) == Direction.Axis.X ? X : Z;
	}

	/**
	 * The run {@code start} belongs to, in order from one end to the other: belts of the same kind and axis end to end, level or one
	 * block up or down (a grade). Belts side by side are separate runs, and so are a 600 mm belt and a heavy belt end to end.
	 */
	public static List<BlockPos> run(BlockGetter level, BlockPos start) {
		BlockState first = level.getBlockState(start);
		if (!(first.getBlock() instanceof BeltConveyorBlock)) {
			return List.of();
		}
		Direction.Axis axis = first.getValue(AXIS);
		Direction forward = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		List<BlockPos> back = walk(level, start, forward.getOpposite(), axis, first.getBlock());
		List<BlockPos> run = new ArrayList<>();
		for (int i = back.size() - 1; i >= 0; i--) {
			run.add(back.get(i));
		}
		run.add(start);
		run.addAll(walk(level, start, forward, axis, first.getBlock()));
		return run;
	}

	private static List<BlockPos> walk(BlockGetter level, BlockPos start, Direction way, Direction.Axis axis, Block kind) {
		List<BlockPos> out = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		seen.add(start);
		BlockPos at = start;
		while (out.size() < MAX_RUN) {
			BlockPos next = null;
			for (int dy : new int[] {0, 1, -1}) {
				BlockPos maybe = at.relative(way).above(dy);
				BlockState there = level.getBlockState(maybe);
				if (there.getBlock() == kind && there.getValue(AXIS) == axis && seen.add(maybe)) {
					next = maybe;
					break;
				}
			}
			if (next == null) {
				break;
			}
			out.add(next);
			at = next;
		}
		return out;
	}

	/** The way a run moves at its {@code end} block: out of the run along its axis, or, for a one-block run, away from {@code from}. */
	public static Direction outward(BlockGetter level, List<BlockPos> run, BlockPos end, BlockPos from) {
		if (run.size() > 1) {
			BlockPos other = end.equals(run.get(0)) ? run.get(1) : run.get(run.size() - 2);
			int dx = end.getX() - other.getX(), dz = end.getZ() - other.getZ();
			return dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
		}
		Direction.Axis axis = level.getBlockState(end).getValue(AXIS);
		Direction plus = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		return end.relative(plus).equals(from) ? plus.getOpposite() : plus;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BeltBlockEntity(pos, state);
	}
}
