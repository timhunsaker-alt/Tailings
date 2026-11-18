package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.plant.BeltGrade;
import dev.wildcatter.tailings.plant.Machine;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * One metre of a belt conveyor run (docs/multiblocks/crushing-circuit.md): a 600 mm troughed belt on idlers, laid block by block
 * along one horizontal axis like a pipe run, or a block up or down for a grade. It has no motor of its own: the {@link
 * Machine#CONVEYOR_DRIVE} at the run's head end drives it and holds its load, and a run with no drive carries nothing.
 */
public class BeltConveyorBlock extends Block implements EntityBlock {
	public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
	/**
	 * On a grade ({@link BeltGrade}): its block entity holds the belt's heights, the belt renderer draws it, and the block model is
	 * empty. A flat block keeps its baked model.
	 */
	public static final BooleanProperty SLOPED = BooleanProperty.create("sloped");
	/**
	 * Over open ground (nothing solid right under it): its block model is empty and the belt renderer draws it with no legs of its
	 * own, carried by a trestle every third metre down to the ground. A belt on the ground keeps its legs.
	 */
	public static final BooleanProperty RAISED = BooleanProperty.create("raised");
	/** The frame and belt: 0.8 m wide, its belt 0.75 m up. */
	private static final VoxelShape X = Shapes.or(Block.box(0, 0, 1.6, 16, 12, 14.4));
	private static final VoxelShape Z = Shapes.or(Block.box(1.6, 0, 0, 14.4, 12, 16));
	/** A run is never followed past this many blocks. */
	public static final int MAX_RUN = 512;

	public BeltConveyorBlock(Properties properties) {
		// its shape follows its run's grade, held by its block entity: never cached by state
		super(properties.noOcclusion().dynamicShape());
		registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Z).setValue(SLOPED, false).setValue(RAISED, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(AXIS, SLOPED, RAISED);
	}

	/** It runs the way the player faces. */
	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		BlockPos below = context.getClickedPos().below();
		return defaultBlockState().setValue(AXIS, context.getHorizontalDirection().getAxis())
				.setValue(RAISED, open(context.getLevel(), below, context.getLevel().getBlockState(below)));
	}

	/** Whether a belt over {@code below} stands over open ground: nothing there it could stand on. */
	public static boolean open(BlockGetter level, BlockPos at, BlockState below) {
		return !(below.getBlock() instanceof BeltConveyorBlock) && below.getCollisionShape(level, at).isEmpty() && below.getFluidState().isEmpty();
	}

	/** The block under it placed or taken away: it stands on it, or goes over to its trestles. */
	@Override
	protected BlockState updateShape(BlockState state, net.minecraft.world.level.LevelReader level, net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos, Direction side,
			BlockPos neighborPos, BlockState neighbor, RandomSource random) {
		return side == Direction.DOWN ? state.setValue(RAISED, open(level, neighborPos, neighbor)) : state;
	}

	/** The drive that drives a run of this belt. */
	public Machine drive() {
		return Machine.CONVEYOR_DRIVE;
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		VoxelShape sloped = slopedShape(state, level, pos);
		return sloped != null ? sloped : state.getValue(AXIS) == Direction.Axis.X ? X : Z;
	}

	/** Across the run, the shape's sides, in blocks; and the top of the shape above the belt's surface. */
	protected double[] shapeAcross() {
		return new double[] {0.1, 0.9, 0};
	}

	/**
	 * On a grade, a shape that follows the belt: four slices along the run, each up to the belt's surface over it, so a player walks
	 * up the incline and the outline follows it. Null when flat or not yet known.
	 */
	protected @Nullable VoxelShape slopedShape(BlockState state, BlockGetter level, BlockPos pos) {
		if (!state.getValue(SLOPED) || !(level.getBlockEntity(pos) instanceof BeltBlockEntity belt)) {
			return null;
		}
		VoxelShape made = belt.shape();
		if (made != null) {
			return made;
		}
		float[] g = belt.grade();
		double[] across = shapeAcross();
		boolean alongX = state.getValue(AXIS) == Direction.Axis.X;
		made = Shapes.empty();
		for (int k = 0; k < 4; k++) {
			double u0 = k / 4.0, u1 = (k + 1) / 4.0;
			double top = Math.max(0.0625, BeltGrade.surface(g[0], g[1], g[2], (u0 + u1) / 2) + across[2]);
			made = Shapes.or(made, alongX ? Shapes.box(u0, 0, across[0], u1, top, across[1]) : Shapes.box(across[0], 0, u0, across[1], top, u1));
		}
		made = made.optimize();
		belt.shape(made);
		return made;
	}

	/** A belt placed: its run is graded again next tick, when its block entity is there. */
	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
		super.onPlace(state, level, pos, oldState, movedByPiston);
		if (!oldState.is(state.getBlock()) && !level.isClientSide()) {
			level.scheduleTick(pos, this, 1);
		}
	}

	/** Placed by a player: graded at once (its block entity is there by now). */
	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
		super.setPlacedBy(level, pos, state, by, stack);
		if (!level.isClientSide()) {
			regrade(level, pos);
		}
	}

	@Override
	protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		regrade(level, pos);
	}

	/** A belt taken out: the runs either side of it are graded again. */
	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
		super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
		if (level.getBlockState(pos).is(state.getBlock())) {
			return;
		}
		Direction.Axis axis = state.getValue(AXIS);
		for (Direction way : new Direction[] {Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE), Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE)}) {
			for (int dy = -1; dy <= 1; dy++) {
				BlockPos near = pos.relative(way).above(dy);
				BlockState there = level.getBlockState(near);
				if (there.is(state.getBlock()) && there.getValue(AXIS) == axis) {
					regrade(level, near);
				}
			}
		}
	}

	/** Grades {@code run} again if any of it was saved before belts were graded (a drive's scan asks). */
	public static void gradeIfOld(Level level, List<BlockPos> run) {
		for (BlockPos p : run) {
			if (level.getBlockEntity(p) instanceof BeltBlockEntity belt && belt.ungraded()) {
				regrade(level, p);
				return;
			}
		}
	}

	/**
	 * Works out where each block of {@code at}'s run sits on its grade ({@link BeltGrade}) and sets it: the heights into each block
	 * entity, {@link #SLOPED} into each state.
	 */
	public static void regrade(Level level, BlockPos at) {
		List<BlockPos> run = run(level, at);
		if (run.isEmpty()) {
			return;
		}
		int[] levels = new int[run.size()];
		for (int i = 0; i < levels.length; i++) {
			levels[i] = run.get(i).getY();
		}
		double[][] profile = BeltGrade.profile(levels);
		for (int i = 0; i < levels.length; i++) {
			BlockPos p = run.get(i);
			BlockState state = level.getBlockState(p);
			boolean sloped = BeltGrade.sloped(profile[i]);
			if (level.getBlockEntity(p) instanceof BeltBlockEntity belt) {
				belt.setGrade(profile[i]);
				belt.graded();
			}
			boolean raised = open(level, p.below(), level.getBlockState(p.below()));
			if (state.getValue(SLOPED) != sloped || state.getValue(RAISED) != raised) {
				level.setBlock(p, state.setValue(SLOPED, sloped).setValue(RAISED, raised), Block.UPDATE_CLIENTS);
			}
		}
	}

	/**
	 * The run {@code start} belongs to, in order from one end to the other: belts of the same kind and axis end to end, level or one
	 * block up or down (a grade). Belts side by side are separate runs, and so are a 600 mm belt and a heavy belt end to end.
	 */
	public static List<BlockPos> run(BlockGetter level, BlockPos start) {
		BlockState first = level.getBlockState(start);
		if (!(first.getBlock() instanceof BeltConveyorBlock)) {
			return List.of();
		}
		Direction.Axis axis = first.getValue(AXIS);
		Direction forward = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		List<BlockPos> back = walk(level, start, forward.getOpposite(), axis, first.getBlock());
		List<BlockPos> run = new ArrayList<>();
		for (int i = back.size() - 1; i >= 0; i--) {
			run.add(back.get(i));
		}
		run.add(start);
		run.addAll(walk(level, start, forward, axis, first.getBlock()));
		return run;
	}

	private static List<BlockPos> walk(BlockGetter level, BlockPos start, Direction way, Direction.Axis axis, Block kind) {
		List<BlockPos> out = new ArrayList<>();
		Set<BlockPos> seen = new HashSet<>();
		seen.add(start);
		BlockPos at = start;
		while (out.size() < MAX_RUN) {
			BlockPos next = null;
			for (int dy : new int[] {0, 1, -1}) {
				BlockPos maybe = at.relative(way).above(dy);
				BlockState there = level.getBlockState(maybe);
				if (there.getBlock() == kind && there.getValue(AXIS) == axis && seen.add(maybe)) {
					next = maybe;
					break;
				}
			}
			if (next == null) {
				break;
			}
			out.add(next);
			at = next;
		}
		return out;
	}

	/** The way a run moves at its {@code end} block: out of the run along its axis, or, for a one-block run, away from {@code from}. */
	public static Direction outward(BlockGetter level, List<BlockPos> run, BlockPos end, BlockPos from) {
		if (run.size() > 1) {
			BlockPos other = end.equals(run.get(0)) ? run.get(1) : run.get(run.size() - 2);
			int dx = end.getX() - other.getX(), dz = end.getZ() - other.getZ();
			return dx > 0 ? Direction.EAST : dx < 0 ? Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH;
		}
		Direction.Axis axis = level.getBlockState(end).getValue(AXIS);
		Direction plus = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
		return end.relative(plus).equals(from) ? plus.getOpposite() : plus;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new BeltBlockEntity(pos, state);
	}
}
