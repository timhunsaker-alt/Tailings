package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.pit.StopKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A survey stake (docs/multiblocks/open-pit.md, P2a): a marker post on a haul road. Right-click with an empty hand steps what it marks;
 * sneak-right-click reads it. Trucks drive over it: it has no collision.
 */
public class SurveyStakeBlock extends Block {
	public static final EnumProperty<Direction> FACING = net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING;
	public static final EnumProperty<StopKind> KIND = EnumProperty.create("kind", StopKind.class);
	private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 16, 10);

	public SurveyStakeBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(KIND, StopKind.WAYPOINT));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, KIND);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	public static StopKind kind(BlockState state) {
		return state.getBlock() instanceof SurveyStakeBlock ? state.getValue(KIND) : StopKind.WAYPOINT;
	}

	/** The survey book links stakes: let the item have the click. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.getItem() instanceof dev.wildcatter.tailings.item.SurveyBookItem) {
			return InteractionResult.PASS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide()) {
			if (player.isShiftKeyDown()) {
				player.sendOverlayMessage(Component.literal("Survey Stake: " + state.getValue(KIND).label() + " at " + pos.toShortString()));
			} else {
				BlockState next = state.setValue(KIND, state.getValue(KIND).next());
				level.setBlock(pos, next, Block.UPDATE_ALL);
				player.sendOverlayMessage(Component.literal("Survey Stake: " + next.getValue(KIND).label()));
			}
		}
		return InteractionResult.SUCCESS;
	}
}
