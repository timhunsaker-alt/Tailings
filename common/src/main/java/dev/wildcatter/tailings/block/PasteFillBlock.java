package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.paste.PasteFill;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Placed paste fill (docs/multiblocks/paste-backfill.md, L5): a cubic metre of cemented paste, 2.0 t. It is wet when the outlet places
 * it: soft, nothing to stand on, slowing what wades in it. It sets in one game day ({@link PasteFill#CURE_TICKS}), in eight steps of
 * 2.5 minutes kept in its {@link #CURE} (a block moved by a spill keeps its age), to a weak rock that can be mined. Either way it drops
 * nothing: its mass is tails, as settled tails are.
 */
public class PasteFillBlock extends Block {
	/** 0 to 7 wet, curing; {@link #SET} set. */
	public static final IntegerProperty CURE = IntegerProperty.create("cure", 0, PasteFill.CURE_STEPS);
	public static final int SET = PasteFill.CURE_STEPS;

	public PasteFillBlock(Properties properties) {
		super(properties);
		registerDefaultState(stateDefinition.any().setValue(CURE, 0));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(CURE);
	}

	public static boolean wet(BlockState state) {
		return state.getBlock() instanceof PasteFillBlock && state.getValue(CURE) < SET;
	}

	public static boolean set(BlockState state) {
		return state.getBlock() instanceof PasteFillBlock && state.getValue(CURE) >= SET;
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return state.getValue(CURE) < SET ? Shapes.empty() : Shapes.block();
	}

	@Override
	protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean past) {
		if (state.getValue(CURE) < SET) {
			entity.makeStuckInBlock(state, new Vec3(0.5, 0.4, 0.5));   // wading in wet paste
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		if (state.getValue(CURE) < SET && !oldState.is(this)) {
			level.scheduleTick(pos, this, PasteFill.cureStepTicks());
		}
	}

	/**
	 * Another 2.5 minutes cured; the eighth step sets it. With {@code paste_unloaded_cure} on, time its chunk spent unloaded is made up
	 * here, in whole steps ({@link dev.wildcatter.tailings.paste.PasteClock}).
	 */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int cure = state.getValue(CURE);
		if (cure >= SET) {
			return;
		}
		// the time its chunk was unloaded, made up only while the world's paste_unloaded_cure rule is on (on in new worlds; docs/questions/gaps2.md #6)
		int missed = dev.wildcatter.tailings.OptInRules.on(level.getServer(), dev.wildcatter.tailings.OptInRules.PASTE_UNLOADED_CURE)
				? dev.wildcatter.tailings.paste.PasteClock.get(level).missedSteps(net.minecraft.world.level.ChunkPos.containing(pos), level.getGameTime())
				: 0;
		int next = Math.min(SET, cure + 1 + missed);
		level.setBlock(pos, state.setValue(CURE, next), Block.UPDATE_ALL);
		if (next < SET) {
			level.scheduleTick(pos, this, PasteFill.cureStepTicks());
		}
	}

	// ---- a wall breached beside wet paste (docs/questions/gaps2.md) ------------------------------------------------------------------
	//
	// A neighbour's change reaches wet paste twice, in this order: neighborChanged names the block that was there, then updateShape
	// where it was and what is there now. A wall (a solid block that is not paste, a bulkhead or the outlet) gone and open ground in its
	// place is a breach: the paste spills out of it as out of a broken bulkhead. Only while the world's paste_wall_breach rule
	// is on (off by default, even in new worlds; docs/questions/gaps2.md #5).

	private static final java.util.Map<Level, java.util.Map<Long, Long>> WALL_GONE = new java.util.WeakHashMap<>();

	/** True for a block whose going opens a wall of the void: a solid block, not paste, a bulkhead or the outlet (a torch is not a wall). */
	private static boolean wall(Block block) {
		BlockState was = block.defaultBlockState();
		return !net.minecraft.world.level.block.FallingBlock.isFree(was) && was.isSolid() && !(block instanceof PasteFillBlock) && !(block instanceof FillBulkheadBlock)
				&& !(block instanceof FillOutletBlock);
	}

	@Override
	protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, net.minecraft.world.level.redstone.@org.jspecify.annotations.Nullable Orientation orientation,
			boolean movedByPiston) {
		super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
		if (level instanceof ServerLevel server && wet(state) && wall(neighborBlock)
				&& dev.wildcatter.tailings.OptInRules.on(server.getServer(), dev.wildcatter.tailings.OptInRules.PASTE_WALL_BREACH)) {
			java.util.Map<Long, Long> gone = WALL_GONE.computeIfAbsent(level, l -> new java.util.HashMap<>());
			if (gone.size() > 64) {
				long now = level.getGameTime();
				gone.values().removeIf(t -> t != now);
			}
			gone.put(pos.asLong(), level.getGameTime());
		}
	}

	@Override
	protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
			net.minecraft.core.Direction toNeighbour, BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
		if (level instanceof ServerLevel server && wet(state)) {
			java.util.Map<Long, Long> gone = WALL_GONE.get(server);
			Long at = gone == null ? null : gone.remove(pos.asLong());
			if (at != null && at == server.getGameTime() && net.minecraft.world.level.block.FallingBlock.isFree(neighbourState) && !(neighbourState.getBlock() instanceof PasteFillBlock)) {
				breach(server, neighbourPos.immutable());
			}
		}
		return super.updateShape(state, level, ticks, pos, toNeighbour, neighbourPos, neighbourState, random);
	}

	/** The paste runs out of the gap once the block update that opened it is done, as out of a broken bulkhead. */
	private static void breach(ServerLevel level, BlockPos gap) {
		net.minecraft.server.MinecraftServer server = level.getServer();
		server.schedule(new net.minecraft.server.TickTask(server.getTickCount(), () -> {
			if (level.isLoaded(gap)) {
				FillBulkheadBlock.spill(level, gap);
			}
		}));
	}
}
