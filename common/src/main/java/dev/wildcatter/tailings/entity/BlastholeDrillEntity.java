package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.BlastholeBlock;
import dev.wildcatter.tailings.block.BlastholeBlockEntity;
import dev.wildcatter.tailings.block.PitPanelMenu;
import dev.wildcatter.tailings.pit.Blasting;
import dev.wildcatter.tailings.pit.DrillPattern;
import dev.wildcatter.tailings.pit.PitDrive;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The rotary blasthole drill (docs/multiblocks/open-pit-p3-blasting.md): a crawler drill with its mast at its back end, drilling 270 mm
 * holes 1 m every 10 s (15 s in deepslate) after 20 s setting up on its jacks, tramming at 1.5 km/h. Its panel (sneak-right-click) sets
 * a pattern, and GO walks it row by row, a serpentine: turn on the spot toward the next hole, tram, turn back square to the pattern,
 * jacks down, drill. A drilled hole is a column of {@code blasthole} blocks, each remembering its rock.
 *
 * <p>As built it is one machine, not four placed parts (docs/questions/p3-blasting.md). The pattern runs square to the world's grid: GO
 * squares the drill to the nearest of the four ways. Its back faces the free face; the rows run on ahead of it.
 */
public class BlastholeDrillEntity extends PitVehicleEntity implements PitPanelMenu.Panel {
	/** The hole is under the mast's foot, 1.5 m in from its back end. */
	public static final double HOLE_BACK = 6.0;

	enum Phase {
		IDLE, PIVOT_OUT, TRAM, PIVOT_BACK, SETUP, DRILL
	}

	private final int[] settings = new int[PitPanelMenu.Kind.DRILL.settings().length];
	private boolean running;
	private Phase phase = Phase.IDLE;
	/** The pattern: where its first hole is, and the way it faces (the rows run on that way; the free face is behind). */
	private @Nullable BlockPos origin;
	private Direction facing = Direction.SOUTH;
	private int next;
	/** The hole being drilled: its collar, how deep it is so far, ticks into the metre, and set-up ticks left. */
	private @Nullable BlockPos collar;
	private int metres;
	private int ticks;
	private int depth;
	private boolean single;
	private int holesDrilled;
	private int divisor = 1;

	public BlastholeDrillEntity(EntityType<?> type, Level level) {
		super(type, level);
		for (int i = 0; i < settings.length; i++) {
			settings[i] = PitPanelMenu.Kind.DRILL.settings()[i].initial();
		}
		status = "parked";
	}

	@Override
	public PitDrive.Spec spec() {
		return PitDrive.DRILL;
	}

	@Override
	protected Item item() {
		return TailingsItems.BLASTHOLE_DRILL;
	}

	@Override
	public double tank() {
		return Blasting.DRILL_TANK_L;
	}

	@Override
	protected Vec3 seat() {
		return new Vec3(-2.5, 4.2, 5.5);
	}

	@Override
	public boolean busy() {
		return phase != Phase.IDLE;
	}

	@Override
	protected double workFraction() {
		return switch (phase) {
			case DRILL -> Blasting.DRILLING;
			case PIVOT_OUT, TRAM, PIVOT_BACK, SETUP -> Blasting.TRAMMING;
			default -> 0;
		};
	}

	/** A game test's faster drilling, tramming and set-up. */
	public void fast(int divisor) {
		this.divisor = Math.max(1, divisor);
	}

	public int holesDrilled() {
		return holesDrilled;
	}

	public boolean running() {
		return running;
	}

	public void setting(String label, int value) {
		PitPanelMenu.Setting[] all = PitPanelMenu.Kind.DRILL.settings();
		for (int i = 0; i < all.length; i++) {
			if (all[i].label().startsWith(label)) {
				settings[i] = all[i].clamp(value);
			}
		}
	}

	private int rows() {
		return settings[0];
	}

	private int cols() {
		return settings[1];
	}

	private int burden() {
		return settings[2];
	}

	private int spacing() {
		return settings[3];
	}

	private int bench() {
		return settings[4];
	}

	private int subdrill() {
		return settings[5];
	}

	private boolean staggered() {
		return settings[6] == 1;
	}

	/** The block column under the mast's foot. */
	public BlockPos holeColumn() {
		Vec3 at = local(0, 0, -HOLE_BACK);
		return BlockPos.containing(at.x, getY() + 0.5, at.z);
	}

	// ---- the pattern -------------------------------------------------------------------------------------------------------------

	public List<DrillPattern.Hole> plan() {
		if (origin == null) {
			return List.of();
		}
		Direction across = facing.getClockWise();   // along a row: to its right, as the machine sees it
		return DrillPattern.holes(origin.getX(), origin.getZ(), facing.getStepX(), facing.getStepZ(), across.getStepX(), across.getStepZ(), rows(), cols(), burden(),
				spacing(), staggered());
	}

	/** GO: squares itself to the grid and starts the pattern at the hole under its mast. Public for tests. */
	public String go() {
		if (phase != Phase.IDLE && running) {
			return status;
		}
		float squared = Math.round(getYRot() / 90F) * 90F;
		if (!(level() instanceof ServerLevel server) || !server.noCollision(this, boxAt(position(), squared).deflate(0.05))) {
			return say("BLOCKED: no room to square up to the pattern");
		}
		setYRot(squared);
		facing = Direction.fromYRot(squared);
		origin = holeColumn();
		next = 0;
		running = true;
		single = false;
		phase = Phase.PIVOT_OUT;
		return say(String.format(Locale.ROOT, "GO: %d x %d holes, %d x %d m%s, %d m deep", rows(), cols(), burden(), spacing(), staggered() ? " staggered" : "",
				Blasting.depth(bench(), subdrill())));
	}

	public void stop(String why) {
		running = false;
		phase = Phase.IDLE;
		setLift(0);
		say(why);
	}

	private String say(String what) {
		status = what;
		if (getFirstPassenger() instanceof Player driver) {
			driver.sendOverlayMessage(Component.literal("Blasthole Drill: " + what));
		}
		return what;
	}

	/** Jump: drills one hole where it stands, square to nothing (it is not part of a pattern). */
	@Override
	protected void jump(ServerLevel level, @Nullable Player driver) {
		if (phase != Phase.IDLE) {
			return;
		}
		single = true;
		running = false;
		phase = Phase.SETUP;
		ticks = Blasting.SETUP_TICKS / divisor;
		say("jacks down: setting up");
	}

	@Override
	protected void work(ServerLevel level) {
		if (phase == Phase.IDLE) {
			return;
		}
		if (outOfFuel()) {
			say("OUT OF FUEL");
			return;
		}
		switch (phase) {
			case PIVOT_OUT -> pivotOut();
			case TRAM -> tram();
			case PIVOT_BACK -> pivotBack();
			case SETUP -> setUp(level);
			case DRILL -> drill(level);
			default -> {
			}
		}
	}

	/** The centre it must stand at for its mast to be over {@code hole}, square to the pattern. */
	private Vec3 standFor(DrillPattern.Hole hole) {
		return new Vec3(hole.x() + 0.5 + facing.getStepX() * HOLE_BACK, getY(), hole.z() + 0.5 + facing.getStepZ() * HOLE_BACK);
	}

	private DrillPattern.@Nullable Hole target() {
		List<DrillPattern.Hole> plan = plan();
		return next < plan.size() ? plan.get(next) : null;
	}

	private void pivotOut() {
		DrillPattern.Hole hole = target();
		if (hole == null) {
			stop("DONE: " + holesDrilled + " holes");
			return;
		}
		Vec3 to = standFor(hole).subtract(position());
		if (Math.hypot(to.x, to.z) < 0.05) {
			phase = Phase.PIVOT_BACK;
			return;
		}
		// it goes forward or backward, whichever needs the shorter turn
		float ahead = (float) Math.toDegrees(Math.atan2(-to.x, to.z));
		float want = Math.abs(net.minecraft.util.Mth.wrapDegrees(ahead - getYRot())) <= 90 ? ahead : ahead + 180;
		if (turnToward(want)) {
			phase = Phase.TRAM;
		}
	}

	private void pivotBack() {
		if (turnToward(facing.toYRot())) {
			phase = Phase.SETUP;
			ticks = Blasting.SETUP_TICKS / divisor;
			say("jacks down: setting up at hole " + (next + 1) + " of " + plan().size());
		}
	}

	/** Turns on the spot toward {@code yaw}; true when there. */
	private boolean turnToward(float yaw) {
		float diff = net.minecraft.util.Mth.wrapDegrees(yaw - getYRot());
		float step = (float) Blasting.PIVOT_DEGREES_PER_TICK * divisor;
		float turned = Math.abs(diff) <= step ? getYRot() + diff : getYRot() + Math.signum(diff) * step;
		if (!level().noCollision(this, boxAt(position(), turned).deflate(0.05).move(0, 0.3, 0))) {
			stop("BLOCKED: no room to turn");
			return false;
		}
		setYRot(turned);
		status = "turning";
		return Math.abs(diff) <= step;
	}

	private void tram() {
		DrillPattern.Hole hole = target();
		if (hole == null) {
			phase = Phase.PIVOT_OUT;
			return;
		}
		Vec3 to = standFor(hole).subtract(position());
		double left = Math.hypot(to.x, to.z);
		double step = Blasting.TRAM_MPS * TICK * divisor;
		if (left <= step) {
			Vec3 before = position();
			move(MoverType.SELF, new Vec3(to.x, 0, to.z));
			if (Math.hypot(getX() - before.x - to.x, getZ() - before.z - to.z) > 0.05) {
				stop("BLOCKED: something in the way of tramming");
				return;
			}
			phase = Phase.PIVOT_BACK;
			return;
		}
		Vec3 way = new Vec3(to.x / left * step, 0, to.z / left * step);
		Vec3 before = position();
		move(MoverType.SELF, way);
		double moved = Math.hypot(getX() - before.x, getZ() - before.z);
		if (horizontalCollision && moved < step * 0.3) {
			stop("BLOCKED: something in the way of tramming");
			return;
		}
		status = String.format(Locale.ROOT, "tramming to hole %d: %.0f m", next + 1, left);
	}

	/** Jacks down: the ground must be level, the collar solid and not broken. */
	private void setUp(ServerLevel level) {
		if (ticks == Blasting.SETUP_TICKS / divisor) {
			String why = checkGround(level);
			if (why != null) {
				if (why.startsWith("drilled")) {
					holeDone();
					return;
				}
				stop(why);
				return;
			}
		}
		if (--ticks > 0) {
			return;
		}
		BlockPos column = holeColumn();
		int top = groundTop(level, column.getX(), column.getZ());
		collar = new BlockPos(column.getX(), top, column.getZ());
		metres = 0;
		depth = Blasting.depth(bench(), subdrill());
		ticks = 0;
		phase = Phase.DRILL;
		say("drilling hole " + (single ? 1 : next + 1));
	}

	/** Why it cannot set up here, or null. */
	private @Nullable String checkGround(ServerLevel level) {
		int lo = Integer.MAX_VALUE, hi = Integer.MIN_VALUE;
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sz = -1; sz <= 1; sz += 2) {
				Vec3 corner = local(sx * 3.0, 0, sz * 5.0);
				int top = groundTop(level, (int) Math.floor(corner.x), (int) Math.floor(corner.z));
				lo = Math.min(lo, top);
				hi = Math.max(hi, top);
			}
		}
		if (hi - lo > 1) {
			return "NOT LEVEL: the ground under the crawlers is " + (hi - lo) + " blocks out";
		}
		BlockPos column = holeColumn();
		int top = groundTop(level, column.getX(), column.getZ());
		BlockState collarState = level.getBlockState(new BlockPos(column.getX(), top, column.getZ()));
		if (collarState.getBlock() == TailingsBlocks.MUCK) {
			return "not on broken ground: drill the bench's solid floor";
		}
		if (collarState.getBlock() instanceof BlastholeBlock) {
			return "drilled already";
		}
		if (collarState.isAir() || !collarState.getFluidState().isEmpty()) {
			return "no ground under the mast";
		}
		return null;
	}

	/** The top solid block in a column, from just over the crawlers' level down 3. */
	private int groundTop(ServerLevel level, int x, int z) {
		int y0 = (int) Math.floor(getY() + 0.01);
		for (int y = y0; y >= y0 - 3; y--) {
			BlockState state = level.getBlockState(new BlockPos(x, y, z));
			if (!state.isAir() && (!state.getCollisionShape(level, new BlockPos(x, y, z)).isEmpty() || !state.getFluidState().isEmpty())) {
				return y;
			}
		}
		return y0 - 1;
	}

	private void drill(ServerLevel level) {
		if (collar == null) {
			stop("lost its hole");
			return;
		}
		BlockPos at = collar.below(metres);
		BlockState state = level.getBlockState(at);
		String why = refuses(level, at, state);
		if (why != null) {
			finishHole(level, true, why);
			return;
		}
		int perMetre = (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath().contains("deepslate") ? Blasting.TICKS_PER_M_DEEPSLATE : Blasting.TICKS_PER_M) / divisor;
		setLift((metres + ticks / (float) Math.max(1, perMetre)) / depth);
		if (++ticks < perMetre) {
			return;
		}
		ticks = 0;
		String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
		level.setBlock(at, TailingsBlocks.BLASTHOLE.defaultBlockState().setValue(BlastholeBlock.COLLAR, metres == 0), Block.UPDATE_ALL);
		if (level.getBlockEntity(at) instanceof BlastholeBlockEntity hole) {
			hole.drilled(id, collar);
			if (metres == 0) {
				DrillPattern.Hole h = single ? null : target();
				hole.pattern(h == null ? 0 : h.row(), h == null ? 0 : h.col(), single ? null : facing.getOpposite(), burden(), spacing(), subdrill());
			}
		}
		metres++;
		if (metres >= depth) {
			finishHole(level, false, id);
		}
	}

	/** Why the bit stops at this block, or null: a cave, water, lava, bedrock or a machine. */
	private static @Nullable String refuses(ServerLevel level, BlockPos at, BlockState state) {
		if (state.isAir()) {
			return "a cave";
		}
		if (!state.getFluidState().isEmpty()) {
			return "water or lava";
		}
		if (state.getDestroySpeed(level, at) < 0) {
			return "bedrock";
		}
		if (state.hasBlockEntity()) {
			return "a machine";
		}
		return null;
	}

	private void finishHole(ServerLevel level, boolean stoppedShort, String what) {
		setLift(0);
		BlastholeBlockEntity hole = collar != null && level.getBlockEntity(collar) instanceof BlastholeBlockEntity h ? h : null;
		if (hole != null) {
			String toeRock = metres > 0 && level.getBlockEntity(collar.below(metres - 1)) instanceof BlastholeBlockEntity t ? t.replaced() : what;
			hole.finish(metres, stoppedShort, toeRock);
		}
		holesDrilled++;
		say(stoppedShort ? "hole SHORT at " + metres + " m: " + what : "hole drilled: " + metres + " m");
		holeDone();
	}

	private void holeDone() {
		if (single) {
			single = false;
			phase = Phase.IDLE;
			return;
		}
		next++;
		phase = Phase.PIVOT_OUT;
	}

	// ---- the panel ---------------------------------------------------------------------------------------------------------------

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
			if (!level().isClientSide()) {
				player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PitPanelMenu(id, inventory, this), getType().getDescription()));
			}
			return InteractionResult.SUCCESS;
		}
		return super.interact(player, hand, location);
	}

	@Override
	public PitPanelMenu.Kind kind() {
		return PitPanelMenu.Kind.DRILL;
	}

	@Override
	public int setting(int i) {
		return settings[i];
	}

	@Override
	public void set(int i, int value, Player player) {
		if (running) {
			say("stop it before changing the pattern");
			return;
		}
		settings[i] = value;
	}

	@Override
	public void action(int a, Player player) {
		switch (a) {
			case 0 -> go();
			case 1 -> stop("stopped");
			case 2 -> {
				if (level() instanceof ServerLevel server) {
					jump(server, player);
				}
			}
			default -> {
			}
		}
	}

	@Override
	public String status() {
		return String.format(Locale.ROOT, "%s. Holes drilled %d; depth %d m; fuel %.0f of %.0f L", status, holesDrilled, Blasting.depth(bench(), subdrill()), fuel(), tank());
	}

	@Override
	public boolean stillValid(Player player) {
		return isAlive() && player.distanceToSqr(this) < 32 * 32;
	}

	@Override
	public Component readout() {
		return Component.literal("Blasthole Drill: " + status());
	}

	// ---- sound -------------------------------------------------------------------------------------------------------------------

	/** While drilling, the rotary head (the TBM's cutter sound stands in for it); otherwise its diesel. */
	@Override
	public net.minecraft.sounds.@Nullable SoundEvent loop() {
		return lift() > 0 ? dev.wildcatter.tailings.TailingsSounds.TBM_CUTTER : super.loop();
	}

	// ---- saving ------------------------------------------------------------------------------------------------------------------

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		for (int i = 0; i < settings.length; i++) {
			settings[i] = PitPanelMenu.Kind.DRILL.settings()[i].clamp(input.getIntOr("setting" + i, settings[i]));
		}
		running = input.getBooleanOr("running", false);
		long o = input.getLongOr("origin", Long.MIN_VALUE);
		origin = o == Long.MIN_VALUE ? null : BlockPos.of(o);
		facing = Direction.from2DDataValue(input.getIntOr("facing", 0));
		next = input.getIntOr("next", 0);
		holesDrilled = input.getIntOr("holes", 0);
		divisor = Math.max(1, input.getIntOr("fast", 1));
		single = input.getBooleanOr("single", false);
		// it goes on where it was: a hole half drilled when its chunk unloaded is finished, from the metre it had reached
		phase = Phase.values()[Math.floorMod(input.getIntOr("phase", (running ? Phase.PIVOT_OUT : Phase.IDLE).ordinal()), Phase.values().length)];
		long c = input.getLongOr("collar", Long.MIN_VALUE);
		collar = c == Long.MIN_VALUE ? null : BlockPos.of(c);
		metres = input.getIntOr("metres", 0);
		depth = input.getIntOr("depth", 0);
		ticks = input.getIntOr("ticks", 0);
		if (phase == Phase.DRILL && collar == null) {
			phase = running ? Phase.PIVOT_OUT : Phase.IDLE;
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		for (int i = 0; i < settings.length; i++) {
			output.putInt("setting" + i, settings[i]);
		}
		output.putBoolean("running", running);
		if (origin != null) {
			output.putLong("origin", origin.asLong());
		}
		output.putInt("facing", facing.get2DDataValue());
		output.putInt("next", next);
		output.putInt("holes", holesDrilled);
		output.putInt("fast", divisor);
		output.putBoolean("single", single);
		output.putInt("phase", phase.ordinal());
		if (collar != null) {
			output.putLong("collar", collar.asLong());
		}
		output.putInt("metres", metres);
		output.putInt("depth", depth);
		output.putInt("ticks", ticks);
	}
}
