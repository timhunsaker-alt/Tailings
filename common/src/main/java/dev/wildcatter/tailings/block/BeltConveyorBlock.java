package dev.wildcatter.tailings.block;

import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.plant.Machine;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One metre of a belt conveyor run (docs/multiblocks/crushing-circuit.md): a 600 mm troughed belt on idlers, laid block by block
 * along one horizontal axis like a pipe run. It has no motor of its own: a {@link Machine#CONVEYOR_DRIVE} touching the run drives
 * it, and a run with no drive carries nothing.
 */
public class BeltConveyorBlock extends Block {
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

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(AXIS) == Direction.Axis.X ? X : Z;
	}

	/** The run {@code start} belongs to: every belt block joined to it, end to end along their axis or side by side. */
	public static List<BlockPos> run(BlockGetter level, BlockPos start) {
		List<BlockPos> run = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		queue.add(start);
		seen.add(start);
		while (!queue.isEmpty() && run.size() < MAX_RUN) {
			BlockPos pos = queue.removeFirst();
			run.add(pos);
			for (Direction side : Direction.values()) {
				BlockPos next = pos.relative(side);
				if (seen.add(next) && level.getBlockState(next).getBlock() instanceof BeltConveyorBlock) {
					queue.add(next);
				}
			}
		}
		return run;
	}

	/** A conveyor drive touches some block of the run. */
	public static boolean driven(BlockGetter level, List<BlockPos> run) {
		for (BlockPos pos : run) {
			for (Direction side : Direction.values()) {
				BlockState there = level.getBlockState(pos.relative(side));
				if (there.getBlock() instanceof PlantMachineBlock block && block.machine() == Machine.CONVEYOR_DRIVE
						&& BigParts.entity(level, pos.relative(side)) != null) {
					return true;
				}
			}
		}
		return false;
	}
}
