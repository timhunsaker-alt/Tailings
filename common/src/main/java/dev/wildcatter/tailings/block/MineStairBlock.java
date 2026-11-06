package dev.wildcatter.tailings.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A walkway stair. The high side is uphill, opposite the bore's heading. Not waterloggable. */
public class MineStairBlock extends Block {
	public static final EnumProperty<Direction> FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<FloorLook> LOOK = EnumProperty.create("look", FloorLook.class, look -> look != FloorLook.BALLAST);
	private static final VoxelShape LOWER = box(0, 0, 0, 16, 8, 16);

	public MineStairBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LOOK, FloorLook.STONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, LOOK);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state.getValue(FACING));
	}

	private static VoxelShape shape(Direction downhill) {
		VoxelShape high = switch (downhill) {
			case SOUTH -> box(0, 8, 0, 16, 16, 8);
			case WEST -> box(8, 8, 0, 16, 16, 16);
			case EAST -> box(0, 8, 0, 8, 16, 16);
			default -> box(0, 8, 8, 16, 16, 16);
		};
		return Shapes.or(LOWER, high);
	}
}
