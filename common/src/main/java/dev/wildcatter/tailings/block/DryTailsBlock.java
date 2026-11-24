package dev.wildcatter.tailings.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The tailings filter's cake as a placed block (open-pit-p4-dry-stack.md): 1 m³ at 1.8 t, falling like sand, LOOSE as it is placed and
 * COMPACTED once a dozer has driven over it (Tim, 2026-11-23, "Compaction"). Loose cake standing on loose cake at the top of an edge
 * with a drop of 2 m or more beside it slides off the edge and comes to rest below by the settle rule; anything else stands.
 */
public class DryTailsBlock extends net.minecraft.world.level.block.ColoredFallingBlock {
	public static final BooleanProperty COMPACTED = BooleanProperty.create("compacted");
	private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

	public DryTailsBlock(Properties properties) {
		super(new net.minecraft.util.ColorRGBA(0x9a8f7f), properties);
		registerDefaultState(stateDefinition.any().setValue(COMPACTED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(COMPACTED);
	}

	public static boolean loose(BlockState state) {
		return state.getBlock() instanceof DryTailsBlock && !state.getValue(COMPACTED);
	}

	public static boolean compacted(BlockState state) {
		return state.getBlock() instanceof DryTailsBlock && state.getValue(COMPACTED);
	}

	/** Compacts loose cake at {@code pos} (a dozer's pass); true if it was loose. */
	public static boolean compact(ServerLevel level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		if (!loose(state)) {
			return false;
		}
		level.setBlock(pos, state.setValue(COMPACTED, true), Block.UPDATE_ALL);
		return true;
	}

	/** The side loose cake at {@code pos} would slide off to, or null when it stands. */
	public static Direction slideSide(ServerLevel level, BlockPos pos, BlockState state) {
		if (!loose(state) || !loose(level.getBlockState(pos.below()))) {
			return null;
		}
		int start = Math.floorMod(pos.getX() * 31 + pos.getZ() * 17, 4);
		for (int i = 0; i < 4; i++) {
			Direction d = SIDES[(start + i) % 4];
			BlockPos side = pos.relative(d);
			if (FallingBlock.isFree(level.getBlockState(side)) && FallingBlock.isFree(level.getBlockState(side.below()))) {
				return d;
			}
		}
		return null;
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		super.tick(state, level, pos, random);
		if (level.getBlockState(pos) != state) {
			return;   // it fell
		}
		Direction side = slideSide(level, pos, state);
		if (side != null) {
			// it slumps: off the edge, settling below at the cake's angle (37 degrees)
			level.removeBlock(pos, false);
			if (dev.wildcatter.tailings.pit.Loose.block(level, pos.relative(side), state) == null) {
				level.setBlock(pos, state, Block.UPDATE_ALL);
			}
		}
	}
}
