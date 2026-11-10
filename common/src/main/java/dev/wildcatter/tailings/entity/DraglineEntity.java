package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.ChunkRunner;
import dev.wildcatter.tailings.block.DraglineBlockEntity;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.block.PitPanelMenu;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.pit.Dragline;
import dev.wildcatter.tailings.pit.DraglineLayout;
import dev.wildcatter.tailings.pit.Loose;
import dev.wildcatter.tailings.pit.PitDrive;
import dev.wildcatter.tailings.pit.PitMass;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The walking dragline, lifted off its parts and working its strip (docs/multiblocks/dragline.md, P6). It stands on the highwall's top,
 * back from the strip's edge by its tub's radius plus 5 m, and digs the overburden off the seam: each 55 s cycle drags a 33 m³ bucket
 * (25 bank blocks, up to 55 t) out of the strip within its 65 m reach, from the top down, hoists and swings it out beyond the strip's
 * far edge and casts it, where it lands by the settle rule. With nothing left in reach above the seam it walks 2 m along the strip, a
 * step a minute; at the strip's end it reads DONE. Hard overburden it cannot dig: BLOCKED, blast it first. It never digs the seam.
 *
 * <p>It runs on its trailing cable at the world's medium voltage: 6 MW while the bucket fills, 1.2 MW the rest of the cycle, 2.5 MW
 * walking (about 2.5 MW on average, spec). Its panel: RUN, STOP (it finishes its cycle, then sets its parts down where it stands if
 * their cells are free) and its job. Its PLC points are on its switch house outlet, over its cable's control pair.
 */
public class DraglineEntity extends PitVehicleEntity implements PitPanelMenu.Panel {
	private static final EntityDataAccessor<Float> DATA_SWING = SynchedEntityData.defineId(DraglineEntity.class, EntityDataSerializers.FLOAT);
	/** The bucket, metres out from its centre and up from its tub's floor (minus is down in the cut). */
	private static final EntityDataAccessor<Float> DATA_BUCKET_OUT = SynchedEntityData.defineId(DraglineEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_BUCKET_UP = SynchedEntityData.defineId(DraglineEntity.class, EntityDataSerializers.FLOAT);
	/** How far through a step its shoes are, 0 to 1; 0 standing. */
	private static final EntityDataAccessor<Float> DATA_WALK = SynchedEntityData.defineId(DraglineEntity.class, EntityDataSerializers.FLOAT);

	/** Where its bucket is parked while it walks or stands, out and up (proposed: hung clear of the ground under the boom). */
	public static final float PARK_OUT = 45F, PARK_UP = 3F;

	public enum Mode {
		RUNNING, STOPPED, DONE
	}

	private final Dragline.Job job = new Dragline.Job();
	private Mode mode = Mode.STOPPED;
	private boolean stopRequested;
	private boolean setDownWhenStopped;
	/** The strip's origin: its centre where it started, and the way it walks. */
	private double originX, originZ;
	/** Ticks into its cycle (scaled by {@link #divisor}), or -1 between cycles; the same for a step. */
	private int cycleTick = -1;
	private int walkTick = -1;
	private double walkFromX, walkFromZ;
	private int retryIn;
	private boolean dumped;
	private int bucketQuarters;
	private double digSwing, digOut, digUp;
	private double dumpSwing, dumpOut, dumpUp;
	private @Nullable BlockPos dumpAt;
	private long cycles;
	private long bankQuarters;
	private int steps;
	private int divisor = 1;

	public DraglineEntity(EntityType<?> type, Level level) {
		super(type, level);
		status = "STOPPED";
	}

	@Override
	public PitDrive.Spec spec() {
		return PitDrive.DRAGLINE;
	}

	/** It is not placed from an item: it is built from its parts. Broken by hand, it gives back its tub and every other part. */
	@Override
	protected Item item() {
		return TailingsBlocks.DRAGLINE_TUB.asItem();
	}

	@Override
	public double tank() {
		return 1;
	}

	@Override
	public boolean electric() {
		return true;
	}

	@Override
	public boolean takesCable() {
		return true;
	}

	@Override
	public double peakWatts() {
		return Dragline.PEAK_WATTS;
	}

	/** It never drives: it walks by its own steps, and its cab runs the set job. */
	@Override
	public boolean busy() {
		return true;
	}

	@Override
	protected void jump(ServerLevel level, @Nullable Player driver) {
	}

	@Override
	public boolean selfRunning() {
		return mode == Mode.RUNNING;
	}

	@Override
	protected Vec3 seat() {
		return new Vec3(-10.5, 9.5, 7.5);   // the cab at the house's front left corner
	}

	public Dragline.Job job() {
		return job;
	}

	public Mode mode() {
		return mode;
	}

	public long cycles() {
		return cycles;
	}

	/** Bank m³ it has dug and cast, ever. */
	public double bankM3() {
		return bankQuarters / 4.0;
	}

	public int steps() {
		return steps;
	}

	/** Metres it has walked along its strip from where it started. */
	public double walked() {
		Vec3 f = ahead();
		return (getX() - originX) * f.x + (getZ() - originZ) * f.z;
	}

	public boolean walking() {
		return walkTick >= 0;
	}

	/** Test and film hook: its cycles and steps run this many times faster. */
	public void fastCycles(int divisor) {
		this.divisor = Math.max(1, divisor);
	}

	public float swing() {
		return entityData.get(DATA_SWING);
	}

	public float bucketOut() {
		return entityData.get(DATA_BUCKET_OUT);
	}

	public float bucketUp() {
		return entityData.get(DATA_BUCKET_UP);
	}

	public float walkPhase() {
		return entityData.get(DATA_WALK);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SWING, 0F);
		builder.define(DATA_BUCKET_OUT, PARK_OUT);
		builder.define(DATA_BUCKET_UP, PARK_UP);
		builder.define(DATA_WALK, 0F);
	}

	/** Lifted from its parts with its cable and job: its strip starts where it stands, and it goes to work. */
	public void start() {
		originX = getX();
		originZ = getZ();
		mode = Mode.RUNNING;
		status = "lifted off its parts: DIGGING; " + job.describe() + (cable == null ? "; NO CABLE" : "; " + cable.describe());
	}

	// ---- its draw -----------------------------------------------------------------------------------------------------------------

	/** What it is doing now, for its draw. */
	public Dragline.Phase phase() {
		if (walkTick >= 0) {
			return Dragline.Phase.WALK;
		}
		return cycleTick >= 0 ? Dragline.phase(cycleTick) : Dragline.Phase.IDLE;
	}

	@Override
	protected double electricWatts(double engineWatts) {
		return Dragline.watts(phase());
	}

	// ---- work ---------------------------------------------------------------------------------------------------------------------

	@Override
	protected void work(ServerLevel level) {
		plc(level);
		doWork(level);
		if (cable != null && cable.house(level) instanceof PitSwitchHouseBlockEntity house) {
			house.reportPlc(cable.outlet(), getUUID(), mode == Mode.RUNNING, mode == Mode.DONE, cycles);
		}
	}

	private void doWork(ServerLevel level) {
		if (mode != Mode.RUNNING) {
			return;
		}
		if (outOfFuel()) {
			status = noPower();   // the cycle holds where it is until its power comes back
			return;
		}
		if (walkTick >= 0) {
			stepWalk(level);
		} else if (cycleTick >= 0) {
			stepCycle(level);
		} else if (stopRequested) {
			stopNow(level);
		} else if (retryIn > 0) {
			retryIn -= divisor;
		} else {
			startCycle(level);
		}
	}

	/** Its points on its switch house outlet: the PLC's RUN in AUTO (it answers RUNNING, DONE and its cycles after its work). */
	private void plc(ServerLevel level) {
		if (cable == null) {
			return;
		}
		PitSwitchHouseBlockEntity house = cable.house(level);
		if (house == null) {
			return;
		}
		Boolean run = house.runCommand(cable.outlet());
		if (run != null) {
			if (run && mode == Mode.STOPPED) {
				resume("started by the PLC");
			} else if (!run && mode == Mode.RUNNING && !stopRequested) {
				stopRequested = true;
				setDownWhenStopped = false;
				status = "STOPPING by the PLC: finishing its cycle";
			}
		}
	}

	/** One strip cell the bucket can take, and what it is. */
	private record Cell(BlockPos pos, double across, double along, boolean muck) {
	}

	private int floorY() {
		return (int) Math.floor(getY() + 0.5);
	}

	/** Its cast side's unit vector: to its right, or its left. */
	private Vec3 castSide() {
		return job.castRight() ? right() : right().scale(-1);
	}

	/** A world column's centre in the strip's frame: {across, along}. */
	private double[] frame(double x, double z) {
		Vec3 f = ahead(), s = castSide();
		double dx = x - originX, dz = z - originZ;
		return new double[] {dx * s.x + dz * s.z, dx * f.x + dz * f.z};
	}

	/** A new cycle: fills the bucket from the strip in reach, top down, and picks where to cast it; or walks on, or is DONE. */
	private void startCycle(ServerLevel level) {
		if (!load.empty()) {
			// what found no room last cast: cast it again, somewhere else
			if (pickDump(level, frame(getX(), getZ())[1])) {
				cycleTick = Dragline.FILL_TICKS;
				dumped = false;
				status = "DIGGING: casting what was left in the bucket";
			} else {
				blocked("no room to cast within its reach beyond the strip");
			}
			return;
		}
		int floor = floorY();
		int top = floor + Dragline.DIG_ABOVE_M - 1, bottom = floor - job.seam();
		double[] me = frame(getX(), getZ());
		List<Cell> cells = new ArrayList<>();
		boolean hard = false;
		int r = (int) Math.ceil(Dragline.REACH_M);
		int cx = (int) Math.floor(getX()), cz = (int) Math.floor(getZ());
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int x = cx - r; x <= cx + r; x++) {
			for (int z = cz - r; z <= cz + r; z++) {
				double wx = x + 0.5, wz = z + 0.5;
				if (!Dragline.inReach(wx - getX(), wz - getZ())) {
					continue;
				}
				double[] p = frame(wx, wz);
				if (!Dragline.inStrip(p[0], p[1], job.width(), job.length())) {
					continue;
				}
				for (int y = top; y >= bottom; y--) {
					BlockState state = level.getBlockState(at.set(x, y, z));
					if (state.isAir() || !state.getFluidState().isEmpty()) {
						continue;
					}
					if (state.getBlock() == TailingsBlocks.MUCK) {
						cells.add(new Cell(at.immutable(), p[0], p[1], true));
						continue;
					}
					if (state.hasBlockEntity()) {
						break;   // never another machine's block
					}
					String id = PitMass.dugAs(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
					if (ore(id)) {
						break;   // the seam: loaders and trucks take it
					}
					if (PitMass.soft(id) || state.canBeReplaced()) {
						cells.add(new Cell(at.immutable(), p[0], p[1], false));
					} else {
						hard = true;   // standing rock: nothing under it can be reached
						break;
					}
				}
			}
		}
		if (cells.isEmpty()) {
			if (hard) {
				blocked("hard overburden: blast it first");
			} else if (Dragline.walkOn(me[1], job.width(), job.length())) {
				startWalk(level);
			} else {
				mode = Mode.DONE;
				status = String.format(Locale.ROOT, "DONE: the strip is uncovered to the seam, %d m down; %d cycles, %,.0f bank m³", job.seam(), cycles, bankM3());
			}
			return;
		}
		// the topmost first, nearest the strip's start and its walk line; then the cells of that layer nearest it, and down
		Cell first = cells.stream().max(Comparator.<Cell>comparingInt(c -> c.pos().getY()).thenComparing(c -> -c.along()).thenComparing(c -> -c.across())).get();
		cells.sort(Comparator.<Cell>comparingInt(c -> -c.pos().getY()).thenComparingDouble(c -> first.pos().distSqr(c.pos())));
		if (!pickDump(level, first.along())) {
			blocked("no room to cast within its reach beyond the strip");
			return;
		}
		fill(level, cells);
		if (load.empty() && bucketQuarters == 0) {
			blocked("nothing the bucket could take");
			return;
		}
		Vec3 to = new Vec3(first.pos().getX() + 0.5 - getX(), 0, first.pos().getZ() + 0.5 - getZ());
		digSwing = Dragline.swingTo(first.across() - me[0], first.along() - me[1], job.castRight());
		digOut = to.horizontalDistance();
		digUp = first.pos().getY() + 0.5 - floor;
		cycleTick = 0;
		dumped = false;
		retryIn = 0;
		status = String.format(Locale.ROOT, "DIGGING: %s, %d quarters of bank from %d m down", load.tonnes(), bucketQuarters, Math.max(0, floor - first.pos().getY()));
		ChunkRunner.refresh(level, first.pos());
		level.playSound(null, first.pos(), dev.wildcatter.tailings.TailingsSounds.MUCK_LOAD, net.minecraft.sounds.SoundSource.BLOCKS, 3F, 0.7F);
	}

	/** A Wildcatter, vanilla or other ore block: the seam, which the dragline leaves (spec). */
	private static boolean ore(String id) {
		return id.endsWith("_ore") || id.equals("minecraft:ancient_debris");
	}

	/** Fills the bucket from the cells in order: 25 bank blocks or 55 t, whichever comes first; muck by the quarter. */
	private void fill(ServerLevel level, List<Cell> cells) {
		long quarterKg = PitMass.ROCK_KG / 4;
		for (Cell c : cells) {
			if (bucketQuarters >= Dragline.BUCKET_BANK_QUARTERS || load.total() >= Dragline.BUCKET_KG) {
				break;
			}
			BlockState state = level.getBlockState(c.pos());
			if (c.muck()) {
				if (level.getBlockEntity(c.pos()) instanceof MuckBlockEntity muck) {
					long before = load.total();
					long room = Math.min(Dragline.BUCKET_KG - before, (long) (Dragline.BUCKET_BANK_QUARTERS - bucketQuarters) * quarterKg);
					PitShovelEntity.takeQuarters(level, c.pos(), muck, load, before + room);
					bucketQuarters += (int) Math.round((load.total() - before) / (double) quarterKg);
				}
				continue;
			}
			String id = PitMass.dugAs(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
			if (PitMass.soft(id)) {
				long kg = PitMass.kg(id);
				if (Dragline.fits(load.total(), bucketQuarters, kg, 4)) {
					load.add(id, kg);
					bucketQuarters += 4;
					level.removeBlock(c.pos(), false);
				}
			} else if (state.canBeReplaced()) {
				level.destroyBlock(c.pos(), false);   // grass, flowers, snow: stripped with the topsoil
			}
		}
		refreshLoad();
	}

	/**
	 * Where to cast: the lowest ground beyond the strip's far edge within its reach (the last strip's hole, or flat ground for the box
	 * cut), counting a column 4 m farther out as a metre lower, so the pile stands highest away from the cut and its toe stays out of it
	 * (proposed); nearest along to where it digs on a tie. False if there is nowhere.
	 */
	private boolean pickDump(ServerLevel level, double digAlong) {
		double[] me = frame(getX(), getZ());
		int floor = floorY();
		int highest = floor + (int) Dragline.POINT_UP_M - 8;
		BlockPos best = null;
		double bestAlong = 0, bestAcross = 0;
		double bestY = Double.MAX_VALUE;
		int r = (int) Math.ceil(Dragline.REACH_M);
		int cx = (int) Math.floor(getX()), cz = (int) Math.floor(getZ());
		for (int x = cx - r; x <= cx + r; x++) {
			for (int z = cz - r; z <= cz + r; z++) {
				double wx = x + 0.5, wz = z + 0.5;
				double dx = wx - getX(), dz = wz - getZ();
				if (dx * dx + dz * dz > (Dragline.REACH_M - 1) * (Dragline.REACH_M - 1)) {
					continue;
				}
				double[] p = frame(wx, wz);
				if (p[0] < Dragline.spoilFrom(job.width()) || p[1] < -20 || p[1] > job.length() + 20) {
					continue;
				}
				int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
				if (y > highest || y <= level.getMinY()) {
					continue;
				}
				BlockState under = level.getBlockState(new BlockPos(x, y - 1, z));
				if (under.hasBlockEntity() && under.getBlock() != TailingsBlocks.MUCK) {
					continue;   // never on a machine
				}
				double low = y - (p[0] - Dragline.spoilFrom(job.width())) / 4.0;
				boolean better = best == null || low < bestY - 1e-9
						|| (Math.abs(low - bestY) < 1e-9 && Math.abs(p[1] - digAlong) < Math.abs(bestAlong - digAlong) - 1e-9);
				if (better) {
					best = new BlockPos(x, y, z);
					bestY = low;
					bestAlong = p[1];
					bestAcross = p[0];
				}
			}
		}
		if (best == null) {
			return false;
		}
		dumpAt = best;
		dumpSwing = Dragline.swingTo(bestAcross - me[0], bestAlong - me[1], job.castRight());
		dumpOut = Math.hypot(best.getX() + 0.5 - getX(), best.getZ() + 0.5 - getZ());
		dumpUp = best.getY() - floor + 4;
		ChunkRunner.refresh(level, best);
		return true;
	}

	/** A tick of its cycle: the bucket drags in, hoists and swings out, casts, and swings back. */
	private void stepCycle(ServerLevel level) {
		cycleTick += divisor;
		Dragline.Phase phase = Dragline.phase(Math.min(cycleTick, Dragline.CYCLE_TICKS - 1));
		double f;
		switch (phase) {
			case FILL -> {
				f = cycleTick / (double) Dragline.FILL_TICKS;
				// dragged in toward the machine as it fills, about 10 m (proposed)
				pose(digSwing, digOut - 10 * f, digUp);
			}
			case SWING_OUT -> {
				f = (cycleTick - Dragline.FILL_TICKS) / (double) Dragline.SWING_OUT_TICKS;
				pose(lerp(digSwing, dumpSwing, f), lerp(digOut - 10, dumpOut, f), lerp(digUp, dumpUp, Math.min(1, f * 1.5)));
				status = "DIGGING: hoisting and swinging to the spoil; " + load.tonnes();
			}
			case DUMP -> {
				pose(dumpSwing, dumpOut, dumpUp);
				if (!dumped) {
					dumped = true;
					cast(level);
				}
			}
			default -> {
				f = (cycleTick - Dragline.FILL_TICKS - Dragline.SWING_OUT_TICKS - Dragline.DUMP_TICKS) / (double) Dragline.SWING_BACK_TICKS;
				pose(lerp(dumpSwing, digSwing, f), lerp(dumpOut, digOut, f), lerp(dumpUp, digUp, f));
			}
		}
		if (cycleTick >= Dragline.CYCLE_TICKS) {
			cycleTick = -1;
		}
	}

	/** Casts the bucket at its spoil point: it lands by the settle rule. */
	private void cast(ServerLevel level) {
		if (dumpAt == null) {
			return;
		}
		int quarters = bucketQuarters;
		Loose.tip(level, dumpAt, load);
		refreshLoad();
		if (load.empty()) {
			cycles++;
			bankQuarters += quarters;
			bucketQuarters = 0;
			status = String.format(Locale.ROOT, "DIGGING: cycle %d cast at %s; %,.0f bank m³ moved", cycles, dumpAt.toShortString(), bankM3());
		} else {
			status = "DIGGING: some spoil found no room at " + dumpAt.toShortString() + ": casting it again";
		}
		level.playSound(null, dumpAt, dev.wildcatter.tailings.TailingsSounds.MUCK_TIP, net.minecraft.sounds.SoundSource.BLOCKS, 4F, 0.7F);
	}

	private void pose(double swing, double out, double up) {
		entityData.set(DATA_SWING, (float) swing);
		entityData.set(DATA_BUCKET_OUT, (float) out);
		entityData.set(DATA_BUCKET_UP, (float) up);
	}

	private static double lerp(double a, double b, double f) {
		return a + (b - a) * Math.max(0, Math.min(1, f));
	}

	private void blocked(String why) {
		status = "BLOCKED: " + why;
		retryIn = 200;
	}

	// ---- walking ------------------------------------------------------------------------------------------------------------------

	/** Starts a 2 m step along its strip, if the way ahead is clear and there is ground under it. */
	private void startWalk(ServerLevel level) {
		Vec3 to = position().add(ahead().scale(Dragline.STEP_M));
		if (!level.noCollision(this, boxAt(to, getYRot()).deflate(0.05))) {
			blocked("the way ahead is blocked");
			return;
		}
		BlockPos under = BlockPos.containing(to.x, floorY() - 1, to.z);
		if (level.getBlockState(under).getCollisionShape(level, under).isEmpty()) {
			blocked("no ground ahead at " + under.toShortString());
			return;
		}
		walkTick = 0;
		walkFromX = getX();
		walkFromZ = getZ();
		status = "WALKING: a 2 m step along the strip";
	}

	private void stepWalk(ServerLevel level) {
		// the boom swings round to point along its walk, and the bucket is carried clear of the ground
		pose(swing() * 0.9, bucketOut() + (PARK_OUT - bucketOut()) * 0.1, bucketUp() + (PARK_UP - bucketUp()) * 0.1);
		walkTick = Math.min(Dragline.STEP_TICKS, walkTick + divisor);
		entityData.set(DATA_WALK, walkTick / (float) Dragline.STEP_TICKS);
		double moved = Dragline.stepMoved(walkTick) * Dragline.STEP_M;
		Vec3 f = ahead();
		setPos(walkFromX + f.x * moved, getY(), walkFromZ + f.z * moved);
		if (walkTick >= Dragline.STEP_TICKS) {
			walkTick = -1;
			steps++;
			entityData.set(DATA_WALK, 0F);
			status = String.format(Locale.ROOT, "WALKING: step %d done, %.0f m along the strip", steps, walked());
		}
	}

	/** At the end of its cable the step is refused: it stands where it is, CABLE END, and tries again every 10 s. */
	@Override
	protected void cableEnd() {
		retryIn = 200;
		if (walkTick >= 0) {
			// it goes on with the step from here when it can
			walkTick = Math.max(0, walkTick - divisor);
		}
	}

	// ---- RUN and STOP -------------------------------------------------------------------------------------------------------------

	private void resume(String why) {
		if (mode == Mode.DONE) {
			return;
		}
		mode = Mode.RUNNING;
		stopRequested = false;
		retryIn = 0;
		status = "DIGGING: " + why;
	}

	/** STOP: it finishes its cycle or step, stops, and sets its parts down where it stands if it can. */
	public String stop(ServerLevel level, boolean setDown) {
		if (mode != Mode.RUNNING || (cycleTick < 0 && walkTick < 0)) {
			mode = mode == Mode.DONE ? Mode.DONE : Mode.STOPPED;
			stopRequested = false;
			return setDown ? stopAndSetDown(level) : (status = "STOPPED");
		}
		stopRequested = true;
		setDownWhenStopped = setDown;
		return status = "STOPPING: finishing its " + (walkTick >= 0 ? "step" : "cycle");
	}

	private void stopNow(ServerLevel level) {
		stopRequested = false;
		mode = Mode.STOPPED;
		status = "STOPPED";
		if (setDownWhenStopped) {
			stopAndSetDown(level);
		}
	}

	/** Its parts go back down as blocks where it stands, square to its walk, if their cells are free and its bucket is empty; why not, or what happened. */
	public String stopAndSetDown(ServerLevel level) {
		if (mode == Mode.RUNNING) {
			mode = Mode.STOPPED;
		}
		if (!load.empty()) {
			return status = "STOPPED: its bucket still holds " + load.tonnes() + ": RUN to cast it first";
		}
		int turns = DraglineLayout.turns(getYRot());
		double qx = DraglineLayout.CENTRE_X - 0.5, qz = DraglineLayout.CENTRE_Z - 0.5;
		for (int t = 0; t < turns; t++) {
			double tmp = qx;
			qx = -qz;
			qz = tmp;
		}
		BlockPos main = new BlockPos((int) Math.round(getX() - 0.5 - qx), floorY(), (int) Math.round(getZ() - 0.5 - qz));
		String why = DraglineBlockEntity.setDown(level, main, turns, getUUID(), cable, job);
		if (why != null) {
			return status = "STOPPED: " + why;
		}
		cable = null;
		ejectPassengers();
		discard();
		return "set down as its parts";
	}

	/** Broken by hand (sneak, empty hand) it gives back every part, its cable and what is in its bucket. */
	@Override
	public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
			for (DraglineLayout.Part part : DraglineLayout.Part.values()) {
				if (part != DraglineLayout.Part.TUB) {
					spawnAtLocation(level, new ItemStack(TailingsBlocks.plantBlock(part.machine())));
				}
			}
		}
		return super.hurtServer(level, source, amount);
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
		return PitPanelMenu.Kind.DRAGLINE;
	}

	@Override
	public int setting(int i) {
		return job.get(i);
	}

	@Override
	public void set(int i, int value, Player player) {
		job.set(i, value);
		say(player, job.describe());
	}

	@Override
	public void action(int a, Player player) {
		if (!(level() instanceof ServerLevel server)) {
			return;
		}
		switch (a) {
			case 0 -> {
				resume("started from its cab");
				say(player, mode == Mode.DONE ? status : "RUN: " + status);
			}
			case 1 -> say(player, stop(server, true));
			default -> {
			}
		}
	}

	private void say(@Nullable Player player, String what) {
		if (player != null) {
			player.sendOverlayMessage(Component.literal("Dragline: " + what));
		}
	}

	@Override
	public String status() {
		return String.format(Locale.ROOT, "%s. Bucket %s; %d cycles, %,.0f bank m³; walked %.0f m; %s", status, load.tonnes(), cycles, bankM3(), walked(), powerInfo());
	}

	@Override
	public boolean stillValid(Player player) {
		return isAlive() && player.distanceToSqr(this) < 40 * 40;
	}

	@Override
	public Component readout() {
		return Component.literal("Dragline: " + status() + "; " + job.describe());
	}

	// ---- saving ------------------------------------------------------------------------------------------------------------------

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		DraglineBlockEntity.loadJob(input, job);
		mode = Mode.valueOf(input.getStringOr("mode", Mode.STOPPED.name()));
		stopRequested = input.getBooleanOr("stop_requested", false);
		setDownWhenStopped = input.getBooleanOr("set_down", false);
		originX = input.getDoubleOr("origin_x", getX());
		originZ = input.getDoubleOr("origin_z", getZ());
		cycleTick = input.getIntOr("cycle_tick", -1);
		walkTick = input.getIntOr("walk_tick", -1);
		walkFromX = input.getDoubleOr("walk_from_x", getX());
		walkFromZ = input.getDoubleOr("walk_from_z", getZ());
		retryIn = input.getIntOr("retry_in", 0);
		dumped = input.getBooleanOr("dumped", false);
		bucketQuarters = input.getIntOr("bucket_quarters", 0);
		digSwing = input.getDoubleOr("dig_swing", 0);
		digOut = input.getDoubleOr("dig_out", PARK_OUT);
		digUp = input.getDoubleOr("dig_up", 0);
		dumpSwing = input.getDoubleOr("dump_swing", 0);
		dumpOut = input.getDoubleOr("dump_out", PARK_OUT);
		dumpUp = input.getDoubleOr("dump_up", 0);
		long d = input.getLongOr("dump_at", Long.MIN_VALUE);
		dumpAt = d == Long.MIN_VALUE ? null : BlockPos.of(d);
		cycles = input.getLongOr("cycles", 0);
		bankQuarters = input.getLongOr("bank_quarters", 0);
		steps = input.getIntOr("steps", 0);
		divisor = Math.max(1, input.getIntOr("fast", 1));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		DraglineBlockEntity.saveJob(output, job);
		output.putString("mode", mode.name());
		output.putBoolean("stop_requested", stopRequested);
		output.putBoolean("set_down", setDownWhenStopped);
		output.putDouble("origin_x", originX);
		output.putDouble("origin_z", originZ);
		output.putInt("cycle_tick", cycleTick);
		output.putInt("walk_tick", walkTick);
		output.putDouble("walk_from_x", walkFromX);
		output.putDouble("walk_from_z", walkFromZ);
		output.putInt("retry_in", retryIn);
		output.putBoolean("dumped", dumped);
		output.putInt("bucket_quarters", bucketQuarters);
		output.putDouble("dig_swing", digSwing);
		output.putDouble("dig_out", digOut);
		output.putDouble("dig_up", digUp);
		output.putDouble("dump_swing", dumpSwing);
		output.putDouble("dump_out", dumpOut);
		output.putDouble("dump_up", dumpUp);
		if (dumpAt != null) {
			output.putLong("dump_at", dumpAt.asLong());
		}
		output.putLong("cycles", cycles);
		output.putLong("bank_quarters", bankQuarters);
		output.putInt("steps", steps);
		output.putInt("fast", divisor);
	}
}
