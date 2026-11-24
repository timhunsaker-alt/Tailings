package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.pit.Loose;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * Muck: broken rock (docs/multiblocks/open-pit.md). It falls like gravel and settles by the settle rule, carrying what it holds; a
 * bucket digs it as it lies. Broken by hand it drops the rock it holds, a quarter at a time by chance.
 * <p>
 * A block on a pile's surface (nothing over it, {@link #UP}) is drawn rounded off toward its open sides ({@link #NORTH} ...: air or
 * anything with no collision beside it), so a pile slumped to 37 degrees reads as a pile, not a staircase of cubes (Heavy Iron fix of
 * 2026-11-23; tools/models/muck_shape.py writes the models). Only the drawing and the light change: its collision stays a whole cube,
 * so the settle rule, the buckets and the wheels work in cells as before.
 */
public class MuckBlock extends BaseEntityBlock {
	public static final BooleanProperty UP = BlockStateProperties.UP, NORTH = BlockStateProperties.NORTH, EAST = BlockStateProperties.EAST,
			SOUTH = BlockStateProperties.SOUTH, WEST = BlockStateProperties.WEST;
	/**
	 * A surface column's lowest height, pixels, by how many open sides it touches: none, one (an edge's middle), two (a corner). The
	 * models draw each at one of a few heights from this up, picked per block, so a pile's step edges break up (muck_shape.py).
	 */
	public static final int[] HEIGHTS = {16, 5, 1};
	private static final int[] EDGES = {0, 5, 11, 16};

	public MuckBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(UP, false).setValue(NORTH, false).setValue(EAST, false).setValue(SOUTH, false).setValue(WEST, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(UP, NORTH, EAST, SOUTH, WEST);
	}

	/** Whether the cell beside or over a muck block is open: not muck, and air, replaceable or with no collision. */
	private static boolean open(BlockGetter level, BlockPos at) {
		BlockState there = level.getBlockState(at);
		if (there.getBlock() instanceof MuckBlock) {
			return false;
		}
		return there.isAir() || there.canBeReplaced() || there.getCollisionShape(level, at).isEmpty();
	}

	/** {@code state} as it stands at {@code pos}: on the surface or not, and open to which sides. */
	public static BlockState shaped(BlockGetter level, BlockPos pos, BlockState state) {
		if (!(state.getBlock() instanceof MuckBlock)) {
			return state;
		}
		boolean up = open(level, pos.above());
		return state.setValue(UP, up).setValue(NORTH, up && open(level, pos.north())).setValue(EAST, up && open(level, pos.east()))
				.setValue(SOUTH, up && open(level, pos.south())).setValue(WEST, up && open(level, pos.west()));
	}

	/** The boxes a block is drawn as: a whole cube under something, else the 3 x 3 columns, lowered toward its open sides. */
	public static VoxelShape shapeOf(BlockState state) {
		if (!state.getValue(UP)) {
			return Shapes.block();
		}
		VoxelShape shape = Shapes.empty();
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 3; j++) {
				int open = (i == 0 && state.getValue(WEST) ? 1 : 0) + (i == 2 && state.getValue(EAST) ? 1 : 0) + (j == 0 && state.getValue(NORTH) ? 1 : 0)
						+ (j == 2 && state.getValue(SOUTH) ? 1 : 0);
				shape = Shapes.or(shape, Block.box(EDGES[i], 0, EDGES[j], EDGES[i + 1], HEIGHTS[open], EDGES[j + 1]));
			}
		}
		return shape.optimize();
	}

	/** Shapes a block that stays where it is to its neighbours, without telling them (they do not depend on it). */
	protected static void reshape(ServerLevel level, BlockPos pos, BlockState state) {
		BlockState shaped = shaped(level, pos, state);
		if (shaped != state) {
			level.setBlock(pos, shaped, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
		}
	}

	@Override
	protected VoxelShape getOcclusionShape(BlockState state) {
		return shapeOf(state);
	}

	@Override
	protected boolean useShapeForLightOcclusion(BlockState state) {
		return true;
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return shaped(context.getLevel(), context.getClickedPos(), defaultBlockState());
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new MuckBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		// placed by a pour, a shot or a scene as the plain block: its tick shapes it to where it stands (its neighbours shape themselves)
		level.scheduleTick(pos, this, 2);
	}

	@Override
	protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction toNeighbour,
			BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		ticks.scheduleTick(pos, this, 2);
		return shaped(level, pos, super.updateShape(state, level, ticks, pos, toNeighbour, neighbourPos, neighbourState, random));
	}

	/** The ground went from under it: it settles again, by the rule, with what it holds. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!FallingBlock.isFree(level.getBlockState(pos.below())) || pos.getY() <= level.getMinY()) {
			reshape(level, pos, state);
			return;
		}
		Map<String, Integer> held = level.getBlockEntity(pos) instanceof MuckBlockEntity muck ? Map.copyOf(muck.quarters()) : Map.of();
		level.removeBlock(pos, false);
		Loose.muck(level, pos, held);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof MuckBlockEntity muck) {
			player.sendOverlayMessage(muck.inspect());
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void playerDestroy(ServerLevel server, ServerPlayer player, BlockPos pos, BlockState state, @Nullable BlockEntity entity, ItemStack tool) {
		super.playerDestroy(server, player, pos, state, entity, tool);
		if (entity instanceof MuckBlockEntity muck) {
			muck.quarters().forEach((id, q) -> {
				int whole = q / 4 + (server.getRandom().nextInt(4) < q % 4 ? 1 : 0);
				if (whole > 0) {
					Block.popResource(server, pos, Feeds.stack(id, whole));
				}
			});
		}
	}
}
