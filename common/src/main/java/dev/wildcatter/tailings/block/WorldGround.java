package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.mine.FloorPiece;
import dev.wildcatter.tailings.mine.Ground;
import dev.wildcatter.tailings.mine.GroundGrid;
import dev.wildcatter.tailings.mine.Heading;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/** The drill's view of one level. Placed lining and floor are the mod's own blocks. */
public final class WorldGround implements GroundGrid {
	private final Level level;
	private final Heading heading;

	public WorldGround(Level level, Heading heading) {
		this.level = level;
		this.heading = heading;
	}

	@Override
	public Ground get(int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		BlockState state = level.getBlockState(pos);
		FluidState fluid = state.getFluidState();
		if (!fluid.isEmpty()) {
			if (fluid.getType() == Fluids.WATER || fluid.getType() == Fluids.FLOWING_WATER) {
				return fluid.isSource() ? Ground.WATER : Ground.FLOWING_WATER;
			}
			if (fluid.getType() == Fluids.LAVA || fluid.getType() == Fluids.FLOWING_LAVA) {
				return fluid.isSource() ? Ground.LAVA : Ground.FLOWING_LAVA;
			}
			return Ground.UNKNOWN_LIQUID;
		}
		Block block = state.getBlock();
		if (block == Blocks.AIR || block == Blocks.CAVE_AIR || block == Blocks.VOID_AIR) {
			return Ground.AIR;
		}
		if (state.getDestroySpeed(level, pos) < 0) {
			return Ground.BEDROCK;
		}
		if (block == TailingsBlocks.LINING) {
			return Ground.LINING;
		}
		if (block == TailingsBlocks.SLOPE || block == TailingsBlocks.MINE_STAIR || block == TailingsBlocks.MINE_LANDING
				|| block == TailingsBlocks.MINE_FLOOR || block == TailingsBlocks.MINE_RAIL) {
			return Ground.FLOOR;
		}
		if (state.is(BlockTags.ORES, ignored -> true) || BuiltInRegistries.BLOCK.getKey(block).getPath().contains("ore")) {
			return Ground.ORE;
		}
		if (block == Blocks.STONE) {
			return Ground.STONE;
		}
		if (block == Blocks.COBBLESTONE || block == Blocks.COBBLED_DEEPSLATE) {
			return block == Blocks.COBBLED_DEEPSLATE ? Ground.DEEPSLATE : Ground.COBBLE;
		}
		if (block == Blocks.DEEPSLATE) {
			return Ground.DEEPSLATE;
		}
		if (block == Blocks.GRANITE) {
			return Ground.GRANITE;
		}
		if (block == Blocks.DIORITE) {
			return Ground.DIORITE;
		}
		if (block == Blocks.ANDESITE) {
			return Ground.ANDESITE;
		}
		if (block == Blocks.TUFF) {
			return Ground.TUFF;
		}
		if (block == Blocks.SANDSTONE) {
			return Ground.SANDSTONE;
		}
		if (block == Blocks.GRAVEL) {
			return Ground.GRAVEL;
		}
		if (block == Blocks.SAND || block == Blocks.RED_SAND) {
			return Ground.SAND;
		}
		if (block == Blocks.DIRT || block == Blocks.COARSE_DIRT || block == Blocks.ROOTED_DIRT || block == Blocks.MUD) {
			return Ground.DIRT;
		}
		if (block == Blocks.OBSIDIAN || block == Blocks.CRYING_OBSIDIAN) {
			return Ground.OBSIDIAN;
		}
		return Ground.OTHER;
	}

	@Override
	public String dropId(int x, int y, int z) {
		BlockState state = level.getBlockState(new BlockPos(x, y, z));
		Block block = state.getBlock();
		if (block == Blocks.STONE) {
			return "minecraft:cobblestone";
		}
		if (block == Blocks.DEEPSLATE) {
			return "minecraft:cobbled_deepslate";
		}
		String id = BuiltInRegistries.ITEM.getKey(block.asItem()).toString();
		return "minecraft:air".equals(id) ? "minecraft:cobblestone" : id;
	}

	@Override
	public void set(int x, int y, int z, Ground ground) {
		BlockState state = switch (ground) {
			case OBSIDIAN -> Blocks.OBSIDIAN.defaultBlockState();
			case COBBLE -> Blocks.COBBLESTONE.defaultBlockState();
			case LINING -> TailingsBlocks.LINING.defaultBlockState();
			default -> Blocks.AIR.defaultBlockState();
		};
		level.setBlock(new BlockPos(x, y, z), state, 3);
	}

	@Override
	public void placeLining(int x, int y, int z) {
		level.setBlock(new BlockPos(x, y, z), TailingsBlocks.LINING.defaultBlockState(), 3);
	}

	@Override
	public void placeCave(int x, int y, int z) {
		level.setBlock(new BlockPos(x, y, z), Blocks.COBBLESTONE.defaultBlockState(), 3);
	}

	@Override
	public void placeFloor(int x, int y, int z, FloorPiece piece, Ground rock) {
		Direction facing = Direction.valueOf(heading.name());
		FloorLook look = FloorLook.from(rock);
		BlockState state = switch (piece) {
			case SLOPE_LOWER -> slope(look, SlopeHalf.LOWER, facing);
			case SLOPE_UPPER -> slope(look, SlopeHalf.UPPER, facing);
			case SLOPE_STEEP -> slope(look, SlopeHalf.STEEP, facing);
			case RAIL_LOWER -> rail(look, RailGrade.LOWER, true, facing);
			case RAIL_UPPER -> rail(look, RailGrade.UPPER, true, facing);
			case RAIL_STEEP -> rail(look, RailGrade.STEEP, true, facing);
			case RAIL_FLAT -> rail(FloorLook.STONE, RailGrade.FLAT, false, facing);
			case STAIR -> TailingsBlocks.MINE_STAIR.defaultBlockState().setValue(MineStairBlock.FACING, facing).setValue(MineStairBlock.LOOK, look);
			case LANDING -> TailingsBlocks.MINE_LANDING.defaultBlockState().setValue(MineFullBlock.LOOK, look);
			case FLAT_ROCK -> TailingsBlocks.MINE_FLOOR.defaultBlockState().setValue(MineFullBlock.LOOK, look);
			case GRAVEL -> Blocks.GRAVEL.defaultBlockState();
		};
		level.setBlock(new BlockPos(x, y, z), state, 3);
	}

	private static BlockState slope(FloorLook look, SlopeHalf half, Direction facing) {
		return TailingsBlocks.SLOPE.defaultBlockState().setValue(SlopeBlock.FACING, facing).setValue(SlopeBlock.HALF, half)
				.setValue(SlopeBlock.LOOK, look);
	}

	private static BlockState rail(FloorLook look, RailGrade grade, boolean rack, Direction facing) {
		return TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, facing).setValue(MineRailBlock.GRADE, grade)
				.setValue(MineRailBlock.LOOK, look).setValue(MineRailBlock.RACK, rack);
	}
}
