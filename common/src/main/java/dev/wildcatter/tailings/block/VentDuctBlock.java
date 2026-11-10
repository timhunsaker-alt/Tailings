package dev.wildcatter.tailings.block;

import dev.wildcatter.block.WireShape;
import dev.wildcatter.tailings.plant.Machine;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A metre of 1.4 m lay-flat ventilation duct (docs/multiblocks/ventilation.md), drawn round and laid like pipe: it grows an arm toward
 * every duct, ventilation fan and booster fan beside it. It carries nothing itself; a fan reads the whole joined line
 * ({@link VentNetwork}). A click with an empty hand reads the air in it.
 */
public class VentDuctBlock extends Block {
	/** Drawn a metre round (the real 1.4 m does not fit a block); its shape the same, a little inside the block. */
	private static final VoxelShape CORE = Block.box(1, 1, 1, 15, 15, 15);
	private static final Map<Direction, VoxelShape> ARM_SHAPES = new EnumMap<>(Direction.class);

	static {
		ARM_SHAPES.put(Direction.NORTH, Block.box(1, 1, 0, 15, 15, 1));
		ARM_SHAPES.put(Direction.SOUTH, Block.box(1, 1, 15, 15, 15, 16));
		ARM_SHAPES.put(Direction.WEST, Block.box(0, 1, 1, 1, 15, 15));
		ARM_SHAPES.put(Direction.EAST, Block.box(15, 1, 1, 16, 15, 15));
		ARM_SHAPES.put(Direction.DOWN, Block.box(1, 0, 1, 15, 1, 15));
		ARM_SHAPES.put(Direction.UP, Block.box(1, 15, 1, 15, 16, 15));
	}

	public VentDuctBlock(Properties properties) {
		super(properties);
		registerDefaultState(WireShape.noArms(stateDefinition.any()));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		WireShape.ARMS.values().forEach(builder::add);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		return withArms(defaultBlockState(), context.getLevel(), context.getClickedPos());
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, @Nullable Orientation orientation, boolean movedByPiston) {
		BlockState updated = withArms(state, level, pos);
		if (updated != state) {
			level.setBlock(pos, updated, 2);
		}
	}

	/** A neighbour placed or broken: the arm toward it follows. */
	@Override
	protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction side,
			BlockPos neighborPos, BlockState neighbor, net.minecraft.util.RandomSource random) {
		return state.setValue(WireShape.ARMS.get(side), joins(neighbor));
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		neighborChanged(state, level, pos, this, null, movedByPiston);
	}

	/** Duct joins duct and the two fans. */
	public static boolean joins(BlockState neighbour) {
		Block block = neighbour.getBlock();
		return block instanceof VentDuctBlock
				|| block instanceof PlantMachineBlock part && (part.machine() == Machine.VENT_FAN || part.machine() == Machine.BOOSTER_FAN);
	}

	public static BlockState withArms(BlockState state, BlockGetter level, BlockPos pos) {
		for (Direction side : Direction.values()) {
			state = state.setValue(WireShape.ARMS.get(side), joins(level.getBlockState(pos.relative(side))));
		}
		return state;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape shape = CORE;
		for (Direction side : Direction.values()) {
			if (state.getValue(WireShape.ARMS.get(side))) {
				shape = Shapes.or(shape, ARM_SHAPES.get(side));
			}
		}
		return shape;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server) {
			player.sendOverlayMessage(Component.literal(VentNetwork.describeDuct(server, pos)));
		}
		return InteractionResult.SUCCESS;
	}
}
