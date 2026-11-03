package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * One block of 1-block-gauge mine rail. The conductor is part of the block and is live only under a locomotive
 * while the traction rectifier is feeding the rail. The rack flag is set on the decline. Walking it does not shock.
 */
public class MineRailBlock extends BaseEntityBlock {
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
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
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

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new RailBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide()) {
			return null;
		}
		return createTickerHelper(type, TailingsBlocks.RAIL, RailBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (!(level.getBlockEntity(pos) instanceof RailBlockEntity rail)) {
			return InteractionResult.PASS;
		}
		boolean placed = false;
		if (stack.getItem() == TailingsItems.LOCOMOTIVE) {
			placed = rail.placeLoco();
		} else if (stack.getItem() == TailingsItems.CAR) {
			placed = rail.placeCar();
		}
		if (!placed) {
			return InteractionResult.PASS;
		}
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (player.isShiftKeyDown() && level.getBlockEntity(pos) instanceof RailBlockEntity rail && rail.joinAhead()) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
}
