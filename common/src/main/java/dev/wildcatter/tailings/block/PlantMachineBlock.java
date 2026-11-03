package dev.wildcatter.tailings.block;

import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.tailings.plant.Machine;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * One true-size machine. Piece 0 is the main cell and holds the block entity; the other cells are the same block.
 * Breaking any cell drops the one item. Terminals are on the main cell, where the cable box is.
 */
public class PlantMachineBlock extends BaseEntityBlock {
	public static final IntegerProperty PIECE = IntegerProperty.create("piece", 0, 191);
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

	private final Machine machine;
	private final Footprint footprint;

	public PlantMachineBlock(Properties properties, Machine machine) {
		super(properties);
		this.machine = machine;
		this.footprint = Footprint.box(machine.width(), machine.height(), machine.depth(), 0, 0);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PIECE, 0));
	}

	public Machine machine() {
		return machine;
	}

	public static int turns(Direction facing) {
		return switch (facing) {
			case EAST -> 1;
			case SOUTH -> 2;
			case WEST -> 3;
			default -> 0;
		};
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PIECE);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection().getOpposite();
		BlockState state = defaultBlockState().setValue(FACING, facing).setValue(PIECE, 0);
		return fits(context.getLevel(), context.getClickedPos(), state) ? state : null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
		fill(level, pos, state);
	}

	public boolean fits(Level level, BlockPos main, BlockState state) {
		int turns = turns(state.getValue(FACING));
		for (int piece = 0; piece < footprint.pieces(); piece++) {
			int[] at = footprint.offset(piece, turns);
			BlockPos pos = main.offset(at[0], at[1], at[2]);
			if (!level.getBlockState(pos).canBeReplaced()) {
				return false;
			}
		}
		return true;
	}

	public void fill(Level level, BlockPos main, BlockState state) {
		int turns = turns(state.getValue(FACING));
		Direction facing = state.getValue(FACING);
		for (int piece = 1; piece < footprint.pieces(); piece++) {
			int[] at = footprint.offset(piece, turns);
			level.setBlock(main.offset(at[0], at[1], at[2]), defaultBlockState().setValue(FACING, facing).setValue(PIECE, piece), 3);
		}
	}

	public BlockPos mainOf(BlockPos pos, BlockState state) {
		int piece = state.getValue(PIECE);
		int[] at = footprint.offset(piece, turns(state.getValue(FACING)));
		return pos.offset(-at[0], -at[1], -at[2]);
	}

	public void dismantle(Level level, BlockPos pos, BlockState state, boolean drop) {
		BlockPos main = mainOf(pos, state);
		BlockState mainState = level.getBlockState(main);
		if (mainState.getBlock() != this) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			return;
		}
		int turns = turns(mainState.getValue(FACING));
		for (int piece = 0; piece < footprint.pieces(); piece++) {
			int[] at = footprint.offset(piece, turns);
			BlockPos cell = main.offset(at[0], at[1], at[2]);
			if (level.getBlockState(cell).getBlock() == this) {
				level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
			}
		}
		if (drop && !level.isClientSide()) {
			Block.popResource(level, main, new ItemStack(this));
		}
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		dismantle(level, pos, state, !player.hasInfiniteMaterials());
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PIECE) == 0 ? new PlantBlockEntity(pos, state) : null;
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || state.getValue(PIECE) != 0) {
			return null;
		}
		return createTickerHelper(type, dev.wildcatter.tailings.TailingsBlocks.PLANT, PlantBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
		BlockPos main = mainOf(pos, state);
		if (level.getBlockEntity(main) instanceof PlantBlockEntity plant && plant.offer(stack)) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		BlockPos main = mainOf(pos, state);
		if (level.getBlockEntity(main) instanceof PlantBlockEntity plant && plant.give(player)) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	public List<BlockPos> cells(BlockPos main, Direction facing) {
		int turns = turns(facing);
		return java.util.stream.IntStream.range(0, footprint.pieces())
				.mapToObj(piece -> {
					int[] at = footprint.offset(piece, turns);
					return main.offset(at[0], at[1], at[2]);
				})
				.toList();
	}
}
