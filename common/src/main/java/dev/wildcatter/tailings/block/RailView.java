package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.haul.Track;
import dev.wildcatter.tailings.mine.Heading;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** The mine rails of a level, as the plain-Java {@link Track} sees them. */
public final class RailView implements Track.View {
	private final Level level;

	public RailView(Level level) {
		this.level = level;
	}

	@Override
	public Track.Cell at(int x, int y, int z) {
		BlockPos pos = new BlockPos(x, y, z);
		if (!level.isLoaded(pos)) {
			return null;
		}
		BlockState state = level.getBlockState(pos);
		if (state.getBlock() instanceof MineSwitchBlock) {
			return new Track.Cell(x, y, z, Track.Kind.SWITCH, heading(state.getValue(MineSwitchBlock.FACING)), state.getValue(MineSwitchBlock.THROWN), true);
		}
		if (state.getBlock() instanceof MineRailBlock) {
			Track.Kind kind = switch (state.getValue(MineRailBlock.GRADE)) {
				case FLAT -> Track.Kind.FLAT;
				case LOWER -> Track.Kind.LOWER;
				case UPPER -> Track.Kind.UPPER;
				case STEEP -> Track.Kind.STEEP;
			};
			return new Track.Cell(x, y, z, kind, heading(state.getValue(MineRailBlock.FACING)), false, state.getValue(MineRailBlock.RACK));
		}
		return null;
	}

	public Track.Cell at(BlockPos pos) {
		return at(pos.getX(), pos.getY(), pos.getZ());
	}

	public static Heading heading(Direction direction) {
		return switch (direction) {
			case EAST -> Heading.EAST;
			case SOUTH -> Heading.SOUTH;
			case WEST -> Heading.WEST;
			default -> Heading.NORTH;
		};
	}

	public static Direction direction(Heading heading) {
		return switch (heading) {
			case NORTH -> Direction.NORTH;
			case SOUTH -> Direction.SOUTH;
			case EAST -> Direction.EAST;
			case WEST -> Direction.WEST;
		};
	}

	public static BlockPos pos(Track.Cell cell) {
		return new BlockPos(cell.x(), cell.y(), cell.z());
	}
}
