package dev.wildcatter.tailings.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * One block of 1-block-gauge mine rail. The conductor is part of the block and is dead unless a locomotive covers it.
 * The rack flag is set on the decline.
 */
public class MineRailBlock extends Block {
	public static final EnumProperty<Direction> FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<RailGrade> GRADE = EnumProperty.create("grade", RailGrade.class);
	public static final EnumProperty<FloorLook> LOOK = EnumProperty.create("look", FloorLook.class);
	public static final BooleanProperty RACK = BooleanProperty.create("rack");

	public MineRailBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GRADE, RailGrade.FLAT)
				.setValue(LOOK, FloorLook.STONE).setValue(RACK, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, GRADE, LOOK, RACK);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state.getValue(GRADE));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state.getValue(GRADE));
	}

	private static VoxelShape shape(RailGrade grade) {
		return switch (grade) {
			case LOWER -> box(0, 0, 0, 16, 8, 16);
			case UPPER -> box(0, 8, 0, 16, 16, 16);
			case FLAT, STEEP -> Shapes.block();
		};
	}
}
