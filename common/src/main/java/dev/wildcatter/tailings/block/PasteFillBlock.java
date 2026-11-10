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

	/** Another 2.5 minutes cured; the eighth step sets it. */
	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		int cure = state.getValue(CURE);
		if (cure >= SET) {
			return;
		}
		level.setBlock(pos, state.setValue(CURE, cure + 1), Block.UPDATE_ALL);
		if (cure + 1 < SET) {
			level.scheduleTick(pos, this, PasteFill.cureStepTicks());
		}
	}
}
