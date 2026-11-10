package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.paste.PasteFill;
import dev.wildcatter.tailings.mine.Settle;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A fill bulkhead (docs/multiblocks/paste-backfill.md, L5): a cubic metre of a shotcrete barricade; a wall of them closes a void's
 * openings for paste fill. To the void it is a wall like any other block. Broken while wet paste is behind it, the paste runs out of
 * the gap by the settle rule ({@link #spill}), down to the gap's floor; set paste stays.
 */
public class FillBulkheadBlock extends Block {
	public FillBulkheadBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (!level.getBlockState(pos).is(this)) {
			spill(level, pos);
		}
	}

	/**
	 * The wet paste behind a gap at {@code gap} runs out of it: every wet cell joined to the gap no lower than the gap (all of it, for a
	 * gap under the paste), highest first, is let go outside the gap and settles there by the settle rule. It stops when the paste finds
	 * no more room outside. Returns the cubic metres that ran out.
	 */
	public static int spill(ServerLevel level, BlockPos gap) {
		if (!FallingBlock.isFree(level.getBlockState(gap))) {
			return 0;
		}
		// where the paste comes from, and so which way is out
		Direction from = null;
		for (Direction d : new Direction[] {Direction.UP, Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
			if (PasteFillBlock.wet(level.getBlockState(gap.relative(d)))) {
				from = d;
				break;
			}
		}
		if (from == null) {
			return 0;
		}
		BlockPos out = from == Direction.UP ? gap : gap.relative(from.getOpposite());
		boolean under = from == Direction.UP;
		// the wet paste joined to the gap
		List<BlockPos> wet = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (Direction d : Direction.values()) {
			BlockPos next = gap.relative(d);
			if (PasteFillBlock.wet(level.getBlockState(next)) && seen.add(next)) {
				queue.add(next);
			}
		}
		while (!queue.isEmpty() && wet.size() < PasteFill.MOST_CELLS) {
			BlockPos cell = queue.poll();
			if (under || cell.getY() >= gap.getY()) {
				wet.add(cell);
			}
			for (Direction d : Direction.values()) {
				BlockPos next = cell.relative(d);
				if (!seen.contains(next) && level.isLoaded(next) && PasteFillBlock.wet(level.getBlockState(next))) {
					seen.add(next);
					queue.add(next);
				}
			}
		}
		wet.sort(Comparator.comparingInt((BlockPos p) -> -p.getY()));
		int ran = 0;
		for (BlockPos cell : wet) {
			BlockState paste = level.getBlockState(cell);
			if (!PasteFillBlock.wet(paste)) {
				continue;
			}
			level.setBlock(cell, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
			if (settle(level, out, paste) == null) {
				level.setBlock(cell, paste, Block.UPDATE_ALL);   // no more room outside: the rest stays behind the gap
				break;
			}
			ran++;
		}
		return ran;
	}

	/**
	 * The settle rule's ground for paste: free where a loose block is free and there is no paste. Wet paste has no collision, so the
	 * rule's own test would let a block fall into it and replace it.
	 */
	private static Settle.Ground ground(ServerLevel level) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		return new Settle.Ground() {
			@Override
			public boolean free(int x, int y, int z) {
				if (y < level.getMinY() || y > level.getMaxY()) {
					return false;
				}
				BlockState state = level.getBlockState(at.set(x, y, z));
				return FallingBlock.isFree(state) && !state.hasBlockEntity() && !(state.getBlock() instanceof PasteFillBlock);
			}

			@Override
			public int floor() {
				return level.getMinY();
			}
		};
	}

	/** Lets one block of paste go at {@code start} (or the first free cell up to 8 above it); where it rested, or null. */
	private static BlockPos settle(ServerLevel level, BlockPos start, BlockState paste) {
		Settle.Ground ground = ground(level);
		for (int up = 0; up <= 8; up++) {
			if (ground.free(start.getX(), start.getY() + up, start.getZ())) {
				Settle.Cell cell = Settle.rest(ground, start.getX(), start.getY() + up, start.getZ(), new java.util.Random(level.getRandom().nextLong()));
				if (cell == null) {
					return null;
				}
				BlockPos rest = new BlockPos(cell.x(), cell.y(), cell.z());
				level.setBlock(rest, paste, Block.UPDATE_ALL);
				return rest;
			}
		}
		return null;
	}

	/** True for a fill bulkhead. */
	public static boolean is(BlockState state) {
		return state.is(TailingsBlocks.FILL_BULKHEAD);
	}
}
