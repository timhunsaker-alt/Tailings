package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.mine.SlopeRamp;
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
		return shape(state.getValue(GRADE), state.getValue(FACING));
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state.getValue(GRADE), state.getValue(FACING));
	}

	private static VoxelShape shape(RailGrade grade, Direction facing) {
		if (grade == RailGrade.FLAT) {
			return Shapes.block();
		}
		boolean steep = grade == RailGrade.STEEP;
		boolean upper = grade == RailGrade.UPPER;
		VoxelShape shape = Shapes.empty();
		for (int[] box : SlopeRamp.boxes(upper, steep, Heading.valueOf(facing.name()))) {
			shape = Shapes.or(shape, box(box[0], box[1], box[2], box[3], box[4], box[5]));
		}
		return shape;
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
		return placeVehicle(stack, level, pos, player);
	}

	/**
	 * A locomotive or car item on a mine rail or switch puts that vehicle on it, facing along the rail the way the player looks
	 * (docs/multiblocks/haulage.md); a car couples to a train whose tail is within reach behind it.
	 */
	static InteractionResult placeVehicle(ItemStack stack, Level level, BlockPos pos, Player player) {
		for (dev.wildcatter.tailings.mine.TbmGantry gantry : dev.wildcatter.tailings.mine.TbmGantry.values()) {
			if (stack.getItem() == TailingsItems.gantry(gantry)) {
				if (!(level instanceof net.minecraft.server.level.ServerLevel server)) {
					return InteractionResult.SUCCESS;
				}
				if (dev.wildcatter.tailings.entity.GantryEntity.place(server, pos, gantry, player.getDirection()) == null) {
					return InteractionResult.PASS;
				}
				if (!player.hasInfiniteMaterials()) {
					stack.shrink(1);
				}
				return InteractionResult.SUCCESS;
			}
		}
		boolean loco = stack.getItem() == TailingsItems.LOCOMOTIVE;
		if (!loco && stack.getItem() != TailingsItems.CAR) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof net.minecraft.server.level.ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		dev.wildcatter.tailings.mine.Heading heading = RailView.heading(player.getDirection());
		Object placed = loco ? dev.wildcatter.tailings.entity.MineVehicleEntity.placeLocomotive(server, pos, heading)
				: dev.wildcatter.tailings.entity.MineVehicleEntity.placeCar(server, pos, heading);
		if (placed == null) {
			return InteractionResult.PASS;
		}
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}
}
