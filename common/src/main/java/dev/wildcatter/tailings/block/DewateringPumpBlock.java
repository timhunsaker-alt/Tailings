package dev.wildcatter.tailings.block;

import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.tailings.TailingsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.jspecify.annotations.Nullable;

/** A 2 x 2 x 2 pump. The main cell, the one with the block entity, sits directly behind the cab. */
public class DewateringPumpBlock extends BaseEntityBlock {
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty PIECE = IntegerProperty.create("piece", 0, 7);
	private final Footprint footprint = Footprint.box(2, 2, 2, 0, 0);

	public DewateringPumpBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PIECE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, PIECE);
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
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
	public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction facing = context.getHorizontalDirection();
		BlockState state = defaultBlockState().setValue(FACING, facing).setValue(PIECE, 0);
		return fits(context.getLevel(), context.getClickedPos(), state) ? state : null;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		int turns = turns(state.getValue(FACING));
		Direction facing = state.getValue(FACING);
		for (int piece = 1; piece < footprint.pieces(); piece++) {
			int[] at = footprint.offset(piece, turns);
			level.setBlock(pos.offset(at[0], at[1], at[2]), defaultBlockState().setValue(FACING, facing).setValue(PIECE, piece), 3);
		}
	}

	private boolean fits(Level level, BlockPos main, BlockState state) {
		int turns = turns(state.getValue(FACING));
		for (int piece = 0; piece < footprint.pieces(); piece++) {
			int[] at = footprint.offset(piece, turns);
			if (!level.getBlockState(main.offset(at[0], at[1], at[2])).canBeReplaced()) {
				return false;
			}
		}
		return true;
	}

	public java.util.List<BlockPos> cells(BlockPos main, Direction facing) {
		int turns = turns(facing);
		java.util.ArrayList<BlockPos> cells = new java.util.ArrayList<>();
		for (int piece = 0; piece < footprint.pieces(); piece++) {
			int[] at = footprint.offset(piece, turns);
			cells.add(main.offset(at[0], at[1], at[2]));
		}
		return cells;
	}

	public BlockPos mainOf(BlockPos pos, BlockState state) {
		int[] at = footprint.offset(state.getValue(PIECE), turns(state.getValue(FACING)));
		return pos.offset(-at[0], -at[1], -at[2]);
	}

	@Override
	public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
		BlockPos main = mainOf(pos, state);
		BlockState mainState = level.getBlockState(main);
		if (mainState.getBlock() != this) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
			return super.playerWillDestroy(level, pos, state, player);
		}
		int turns = turns(mainState.getValue(FACING));
		for (int piece = 0; piece < footprint.pieces(); piece++) {
			int[] at = footprint.offset(piece, turns);
			BlockPos cell = main.offset(at[0], at[1], at[2]);
			if (level.getBlockState(cell).getBlock() == this) {
				level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
			}
		}
		if (!player.hasInfiniteMaterials() && !level.isClientSide()) {
			Block.popResource(level, main, new ItemStack(this));
		}
		return super.playerWillDestroy(level, pos, state, player);
	}

	@Override
	public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return state.getValue(PIECE) == 0 ? new PumpBlockEntity(pos, state) : null;
	}
}
