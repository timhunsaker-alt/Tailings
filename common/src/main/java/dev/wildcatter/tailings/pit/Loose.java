package dev.wildcatter.tailings.pit;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.mine.Settle;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Loose blocks let go in the world: muck, a tipped load, settling by the rule in {@link Settle} (docs/multiblocks/open-pit.md). They
 * are placed where they rest at once, with no falling entities.
 */
public final class Loose {
	private Loose() {
	}

	static Settle.Ground ground(ServerLevel level) {
		return new Settle.Ground() {
			private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

			@Override
			public boolean free(int x, int y, int z) {
				if (y < level.getMinY() || y > level.getMaxY()) {
					return false;
				}
				BlockState state = level.getBlockState(at.set(x, y, z));
				return FallingBlock.isFree(state) && !state.hasBlockEntity();
			}

			@Override
			public int floor() {
				return level.getMinY();
			}
		};
	}

	/** The first free cell at or above {@code start}, up to 8 up; null if there is none. */
	private static @Nullable BlockPos opening(ServerLevel level, BlockPos start) {
		Settle.Ground ground = ground(level);
		for (int up = 0; up <= 8; up++) {
			if (ground.free(start.getX(), start.getY() + up, start.getZ())) {
				return start.above(up);
			}
		}
		return null;
	}

	/** Lets one block of muck go at {@code start}; returns where it rested, or null when there was no room. */
	public static @Nullable BlockPos muck(ServerLevel level, BlockPos start, Map<String, Integer> quarters) {
		return muck(level, start, TailingsBlocks.MUCK.defaultBlockState(), quarters);
	}

	/**
	 * Lets one block of {@code state}, a muck block or a stockpile block (primary-gyratory-stockpile.md, P7a), go at {@code start} holding
	 * {@code quarters}; returns where it rested, or null when there was no room.
	 */
	public static @Nullable BlockPos muck(ServerLevel level, BlockPos start, BlockState state, Map<String, Integer> quarters) {
		BlockPos rest = rest(level, start);
		if (rest != null) {
			level.setBlock(rest, state, Block.UPDATE_ALL);
			if (level.getBlockEntity(rest) instanceof MuckBlockEntity muck) {
				muck.set(quarters);
			}
		}
		return rest;
	}

	/**
	 * Where a loose block standing at {@code pos} would roll to by the settle rule, were it let go: its own cell when it stands, as at
	 * 37 degrees or less (the stockpile's slopes rolling in over a reclaim feeder, P7a).
	 */
	public static BlockPos wouldRoll(ServerLevel level, BlockPos pos) {
		Settle.Ground world = ground(level);
		Settle.Ground without = new Settle.Ground() {
			@Override
			public boolean free(int x, int y, int z) {
				return (x == pos.getX() && y == pos.getY() && z == pos.getZ()) || world.free(x, y, z);
			}

			@Override
			public int floor() {
				return world.floor();
			}
		};
		Settle.Cell cell = Settle.rest(without, pos.getX(), pos.getY(), pos.getZ(), new java.util.Random(level.getRandom().nextLong()));
		return cell == null ? pos : new BlockPos(cell.x(), cell.y(), cell.z());
	}

	/** Lets one block of {@code state} go at {@code start}. */
	public static @Nullable BlockPos block(ServerLevel level, BlockPos start, BlockState state) {
		BlockPos rest = rest(level, start);
		if (rest != null) {
			level.setBlock(rest, state, Block.UPDATE_ALL);
		}
		return rest;
	}

	private static @Nullable BlockPos rest(ServerLevel level, BlockPos start) {
		BlockPos open = opening(level, start);
		if (open == null) {
			return null;
		}
		Settle.Cell cell = Settle.rest(ground(level), open.getX(), open.getY(), open.getZ(), new java.util.Random(level.getRandom().nextLong()));
		return cell == null ? null : new BlockPos(cell.x(), cell.y(), cell.z());
	}

	/**
	 * Tips {@code load} out at {@code start}: soil lands as its own blocks, rock and ore as muck, four quarters of muck for each bank
	 * block (the same swell as a blast). What is under a whole block or quarter, or finds no room, stays in the load. Returns the
	 * blocks placed.
	 */
	public static int tip(ServerLevel level, BlockPos start, PitLoad load) {
		int placed = 0;
		Map<String, Integer> filling = new LinkedHashMap<>();
		int inFilling = 0;
		for (Map.Entry<String, Long> e : Map.copyOf(load.contents()).entrySet()) {
			String id = e.getKey();
			long kg = e.getValue();
			if (PitMass.ownBlock(id)) {
				BlockState state = stateOf(id);
				// to the nearest whole block: what is left over spills, and the body comes away empty (2026-11-09, docs/questions)
				int whole = (int) Math.round((double) kg / PitMass.kg(id));
				boolean all = true;
				for (int i = 0; i < whole; i++) {
					if (block(level, start, state) == null) {
						all = false;
						break;
					}
					load.take(id, PitMass.kg(id));
					placed++;
				}
				if (all) {
					load.take(id, Long.MAX_VALUE);
				}
				continue;
			}
			// to the nearest quarter of a bank block; the rest spills (2026-11-09)
			int quarters = (int) Math.round(kg * 4.0 / PitMass.kg(id));
			for (int q = 0; q < quarters; q++) {
				filling.merge(id, 1, Integer::sum);
				inFilling++;
				load.take(id, PitMass.kg(id) / 4);
				if (inFilling == PitMass.MUCK_QUARTERS) {
					if (muck(level, start, filling) == null) {
						filling.forEach((back, n) -> load.add(back, (long) n * PitMass.kg(back) / 4));
						return placed;
					}
					placed++;
					filling.clear();
					inFilling = 0;
				}
			}
			load.take(id, Long.MAX_VALUE);
		}
		if (inFilling > 0) {
			if (muck(level, start, filling) == null) {
				filling.forEach((back, n) -> load.add(back, (long) n * PitMass.kg(back) / 4));
			} else {
				placed++;
			}
		}
		return placed;
	}

	private static BlockState stateOf(String id) {
		Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
		return block == null || block == Blocks.AIR ? Blocks.DIRT.defaultBlockState() : block.defaultBlockState();
	}
}
