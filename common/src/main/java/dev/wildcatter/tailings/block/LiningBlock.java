package dev.wildcatter.tailings.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A solid, non-waterloggable ring segment. Liquids stay outside the drive.
 * <p>
 * The ring's two upper corner segments ({@link #CORNER}) are drawn rounded: a quarter-round fillet of the bore's inside, from the wall's
 * top to the crown's edge, the segment filling the corner toward {@link #FACING} and up. The bore then reads as a round-cornered tunnel
 * from inside instead of a stepped octagon (fix-weak-spots-hi05). Placed set: {@code corner=false} is the plain full segment.
 */
public class LiningBlock extends Block {
	public static final BooleanProperty CORNER = BooleanProperty.create("corner");
	public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
	/** The fillet's strips, as boxes in pixels (along the axis, up, toward the solid side), facing north. */
	private static final VoxelShape[] FILLET = new VoxelShape[4];

	static {
		// a quarter circle of radius 16 px about the cell's inner lower edge; the segment is what lies outside it, in 4 px strips (the model draws 2 px strips)
		for (int i = 0; i < 4; i++) {
			Direction to = Direction.from2DDataValue(i);
			VoxelShape shape = Shapes.empty();
			for (int s = 0; s < 4; s++) {
				double y0 = s * 4, y1 = y0 + 4;
				double reach = 16 - Math.sqrt(256 - y1 * y1);   // how far the solid reaches from the outer side at the strip's top
				shape = Shapes.or(shape, box(to, reach, y0, y1));
			}
			FILLET[i] = shape.optimize();
		}
	}

	public LiningBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(CORNER, false).setValue(FACING, Direction.NORTH));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CORNER, FACING);
	}

	/** A rounded corner segment whose solid corner lies toward {@code toward} (horizontal) and up. */
	public BlockState corner(Direction toward) {
		return defaultBlockState().setValue(CORNER, true).setValue(FACING, toward);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(CORNER) ? FILLET[state.getValue(FACING).get2DDataValue()] : Shapes.block();
	}

	/** A strip from the {@code to} side, {@code reach} px into the cell, between heights y0 and y1, full along the other axis. */
	private static VoxelShape box(Direction to, double reach, double y0, double y1) {
		return switch (to) {
			case NORTH -> Block.box(0, y0, 0, 16, y1, reach);
			case SOUTH -> Block.box(0, y0, 16 - reach, 16, y1, 16);
			case WEST -> Block.box(0, y0, 0, reach, y1, 16);
			default -> Block.box(16 - reach, y0, 0, 16, y1, 16);
		};
	}
}
