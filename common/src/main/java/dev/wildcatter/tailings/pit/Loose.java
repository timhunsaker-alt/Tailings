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
		return ground(level, java.util.Set.of());
	}

	/** The ground, with {@code held} cells counted as taken: where a pour's blocks are still on their way (docs/multiblocks/open-pit.md, Pouring). */
	static Settle.Ground ground(ServerLevel level, java.util.Set<BlockPos> held) {
		return new Settle.Ground() {
			private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();

			@Override
			public boolean free(int x, int y, int z) {
				if (y < level.getMinY() || y > level.getMaxY()) {
					return false;
				}
				at.set(x, y, z);
				if (!held.isEmpty() && held.contains(at)) {
					return false;
				}
				BlockState state = level.getBlockState(at);
				return FallingBlock.isFree(state) && !state.hasBlockEntity();
			}

			@Override
			public int floor() {
				return level.getMinY();
			}
		};
	}

	/** The first free cell at or above {@code start}, up to 8 up; null if there is none. */
	private static @Nullable BlockPos opening(Settle.Ground ground, BlockPos start) {
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
	 * Where a stockpile block standing on the pile's top at {@code pos} would roll to as a reclaim feeder draws the pile down (P7a), by
	 * the settle rule's draw-down table ({@link Settle#DRAW_DOWN_RISE}): its own cell when it stands.
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
		Settle.Cell cell = Settle.rest(without, pos.getX(), pos.getY(), pos.getZ(), new java.util.Random(level.getRandom().nextLong()), Settle.DRAW_DOWN_RISE);
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
		return rest(level, start, java.util.Set.of());
	}

	/** Where a block let go at {@code start} rests by the settle rule, the {@code held} cells counted as taken; null with no room. */
	public static @Nullable BlockPos rest(ServerLevel level, BlockPos start, java.util.Set<BlockPos> held) {
		Settle.Ground ground = ground(level, held);
		BlockPos open = opening(ground, start);
		if (open == null) {
			return null;
		}
		Settle.Cell cell = Settle.rest(ground, open.getX(), open.getY(), open.getZ(), new java.util.Random(level.getRandom().nextLong()));
		return cell == null ? null : new BlockPos(cell.x(), cell.y(), cell.z());
	}

	/**
	 * One block of a tipped load: a soil (or dry tails) block, or a muck block of up to {@link PitMass#MUCK_QUARTERS} quarters, with
	 * what was taken from the load for it, item by item, so it can go back if it finds no room.
	 */
	public record Piece(BlockState state, Map<String, Integer> quarters, Map<String, Long> taken) {
		/** Puts what it took back in {@code load}. */
		public void putBack(PitLoad load) {
			taken.forEach(load::add);
		}

		/** Places it at {@code at}. */
		public void place(ServerLevel level, BlockPos at) {
			level.setBlock(at, state, Block.UPDATE_ALL);
			if (!quarters.isEmpty() && level.getBlockEntity(at) instanceof MuckBlockEntity muck) {
				muck.set(quarters);
			}
		}
	}

	/**
	 * Takes the next block's worth out of {@code load}, in the load's order: soil lands as its own blocks, rock and ore as muck, four
	 * quarters of muck for each bank block (the same swell as a blast), a muck block taking quarters across items until it is full.
	 * What rounds to under a whole block of soil, or under a quarter, spills, so the body comes away empty (2026-11-09,
	 * docs/questions). Null when nothing is left that makes a block.
	 */
	public static @Nullable Piece next(PitLoad load) {
		Map<String, Integer> filling = new LinkedHashMap<>();
		Map<String, Long> taken = new LinkedHashMap<>();
		int inFilling = 0;
		for (String id : java.util.List.copyOf(load.contents().keySet())) {
			long kg = load.kg(id);
			int blockKg = PitMass.kg(id);
			if (PitMass.ownBlock(id)) {
				if (inFilling > 0) {
					continue;   // a muck block being filled takes only rock and ore
				}
				// to the nearest whole block: what is left over spills
				if (Math.round((double) kg / blockKg) < 1) {
					load.take(id, Long.MAX_VALUE);
					continue;
				}
				long take = Math.min(kg, blockKg);
				load.take(id, take);
				return new Piece(stateOf(id), Map.of(), Map.of(id, take));
			}
			// to the nearest quarter of a bank block; the rest spills (2026-11-09)
			while (inFilling < PitMass.MUCK_QUARTERS && load.kg(id) > 0 && Math.round(load.kg(id) * 4.0 / blockKg) >= 1) {
				long take = Math.min(load.kg(id), blockKg / 4);
				load.take(id, take);
				taken.merge(id, take, Long::sum);
				filling.merge(id, 1, Integer::sum);
				inFilling++;
			}
			if (inFilling == PitMass.MUCK_QUARTERS) {
				break;   // what is left of this item goes in the next block
			}
			load.take(id, Long.MAX_VALUE);   // under a quarter left: it spills
		}
		return inFilling == 0 ? null : new Piece(TailingsBlocks.MUCK.defaultBlockState(), filling, taken);
	}

	/** How many blocks {@code load} would tip to, all finding room. */
	public static int blocks(PitLoad load) {
		PitLoad copy = new PitLoad(Long.MAX_VALUE);
		load.contents().forEach(copy::add);
		int n = 0;
		while (next(copy) != null) {
			n++;
		}
		return n;
	}

	/**
	 * Tips {@code load} out at {@code start}, all at once: soil lands as its own blocks, rock and ore as muck ({@link #next}). What is
	 * under a whole block or quarter spills; what finds no room stays in the load. Returns the blocks placed.
	 */
	public static int tip(ServerLevel level, BlockPos start, PitLoad load) {
		int placed = 0;
		for (Piece piece = next(load); piece != null; piece = next(load)) {
			BlockPos rest = rest(level, start);
			if (rest == null) {
				piece.putBack(load);
				return placed;
			}
			piece.place(level, rest);
			placed++;
		}
		return placed;
	}

	/** What a tipped item lands as: muck for rock and ore, its own block for soil (and dry tails). */
	public static BlockState landsAs(String id) {
		return PitMass.ownBlock(id) ? stateOf(id) : TailingsBlocks.MUCK.defaultBlockState();
	}

	static BlockState stateOf(String id) {
		Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
		return block == null || block == Blocks.AIR ? Blocks.DIRT.defaultBlockState() : block.defaultBlockState();
	}
}
