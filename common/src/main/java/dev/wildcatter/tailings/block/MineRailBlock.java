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
	/**
	 * Laid on a track grizzly (bottom-dump-cars.md): no ballast bed under it, only the rails, the conductor and the ties, so the bars
	 * and the pocket under them show and a bottom-dump car's load is seen to fall through. Set from the block under it.
	 */
	public static final BooleanProperty BRIDGE = BooleanProperty.create("bridge");

	public MineRailBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(GRADE, RailGrade.FLAT)
				.setValue(LOOK, FloorLook.STONE).setValue(RACK, false).setValue(BRIDGE, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, GRADE, LOOK, RACK, BRIDGE);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state);
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return shape(state);
	}

	/** Over a track grizzly: the rails and the conductor only, open between them. */
	private static final VoxelShape BRIDGE_X = Shapes.or(box(0, 13, 2, 16, 16, 4), box(0, 13, 7, 16, 16, 9), box(0, 13, 12, 16, 16, 14));
	private static final VoxelShape BRIDGE_Z = Shapes.or(box(2, 13, 0, 4, 16, 16), box(7, 13, 0, 9, 16, 16), box(12, 13, 0, 14, 16, 16));

	private static VoxelShape shape(BlockState state) {
		if (state.getValue(BRIDGE) && state.getValue(GRADE) == RailGrade.FLAT) {
			return state.getValue(FACING).getAxis() == Direction.Axis.X ? BRIDGE_X : BRIDGE_Z;
		}
		return shape(state.getValue(GRADE), state.getValue(FACING));
	}

	/** True when the block under a rail is a track grizzly: the rail is laid as a bridge over its bars. */
	public static boolean onGrizzly(BlockState below) {
		return below.getBlock() instanceof PlantMachineBlock block && block.machine() == dev.wildcatter.tailings.plant.Machine.TRACK_GRIZZLY;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(net.minecraft.world.item.context.BlockPlaceContext context) {
		BlockState state = super.getStateForPlacement(context);
		return state == null ? null : state.setValue(BRIDGE, onGrizzly(context.getLevel().getBlockState(context.getClickedPos().below())));
	}

	@Override
	protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
			Direction toNeighbour, BlockPos neighbourPos, BlockState neighbourState, net.minecraft.util.RandomSource random) {
		if (toNeighbour == Direction.DOWN) {
			state = state.setValue(BRIDGE, onGrizzly(neighbourState));
		}
		return super.updateShape(state, level, ticks, pos, toNeighbour, neighbourPos, neighbourState, random);
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
		net.minecraft.world.entity.EntityType<dev.wildcatter.tailings.entity.MineVehicleEntity> type = stack.getItem() == TailingsItems.LOCOMOTIVE ? dev.wildcatter.tailings.TailingsEntities.LOCOMOTIVE
				: stack.getItem() == TailingsItems.BATTERY_LOCOMOTIVE ? dev.wildcatter.tailings.TailingsEntities.BATTERY_LOCOMOTIVE
				: stack.getItem() == TailingsItems.CAR ? dev.wildcatter.tailings.TailingsEntities.CAR
				: stack.getItem() == TailingsItems.BOTTOM_DUMP_CAR ? dev.wildcatter.tailings.TailingsEntities.BOTTOM_DUMP_CAR : null;
		if (type == null) {
			return InteractionResult.PASS;
		}
		if (!(level instanceof net.minecraft.server.level.ServerLevel server)) {
			return InteractionResult.SUCCESS;
		}
		dev.wildcatter.tailings.mine.Heading heading = RailView.heading(player.getDirection());
		boolean loco = type == dev.wildcatter.tailings.TailingsEntities.LOCOMOTIVE || type == dev.wildcatter.tailings.TailingsEntities.BATTERY_LOCOMOTIVE;
		Object placed = loco ? dev.wildcatter.tailings.entity.MineVehicleEntity.placeLocomotive(server, pos, heading, type)
				: dev.wildcatter.tailings.entity.MineVehicleEntity.placeCar(server, pos, heading, type);
		if (placed == null) {
			return InteractionResult.PASS;
		}
		if (!player.hasInfiniteMaterials()) {
			stack.shrink(1);
		}
		return InteractionResult.SUCCESS;
	}
}
