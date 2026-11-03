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

/** A floor wedge cut from mined rock. Not waterloggable, so a broken ring is the only way liquid gets in. */
public class SlopeBlock extends Block {
	public static final EnumProperty<Direction> FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<SlopeHalf> HALF = EnumProperty.create("half", SlopeHalf.class);
	public static final EnumProperty<FloorLook> LOOK = EnumProperty.create("look", FloorLook.class);

	public SlopeBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(HALF, SlopeHalf.LOWER).setValue(LOOK, FloorLook.STONE));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, HALF, LOOK);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state.getValue(HALF));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state.getValue(HALF));
	}

	private static VoxelShape shape(SlopeHalf half) {
		return switch (half) {
			case UPPER -> box(0, 8, 0, 16, 16, 16);
			case STEEP -> Shapes.block();
			case LOWER -> box(0, 0, 0, 16, 8, 16);
		};
	}
}
