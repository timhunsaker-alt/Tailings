package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.Tailings;
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
	/** Going down with a slide: the tick it started to tip (-1 standing), toward which way (degrees, world yaw) and how far down it lands. */
	private static final EntityDataAccessor<Integer> DATA_TIP_START = SynchedEntityData.defineId(DraglineEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Float> DATA_TIP_YAW = SynchedEntityData.defineId(DraglineEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> DATA_TIP_DEPTH = SynchedEntityData.defineId(DraglineEntity.class, EntityDataSerializers.FLOAT);
	/** It tips over and falls into the cut in 3 s, to 70 degrees, then breaks into its parts where it lands. */
	public static final int TIP_TICKS = 60;
	private static final float TIP_MOST = 70F;

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

	// ---- by hand (dragline-hand-control.md, Part 1) ----
	/** A player has its levers: the job waits, and the keys move swing, drag and hoist. */
	private boolean hand;
	/** The swing's speed, degrees a second, plus to its right. */
	private double swingRate;
	/** After handing back with sprint, the keys do nothing until all are let go. */
	private boolean latch;
	private int sprintTicks;
	/** The layer the bucket bit into when this pass began, or MIN_VALUE between passes; the last metre it cut. */
	private int biteY = Integer.MIN_VALUE;
	private int lastCut = Integer.MIN_VALUE;
	private boolean dumpLatch;
	/** The PLC's RUN is off: the brakes are set. */
	private boolean plcHold;
	private double handWatts;
	private long handCasts;
	private boolean testSeated;
	private int sayIn;
	/** A slide took ground from under its tub (Part 2). */
	private boolean groundGone;
	/** A slide's run-out has buried its bucket lying in the cut: it must be dragged out before it hoists or swings. */
	private boolean bucketBuried;
	/** Its strip in the slope record, or -1. */
	private long stripId = -1;

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
		Vec3 f = square(ahead());
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
		builder.define(DATA_TIP_START, -1);
		builder.define(DATA_TIP_YAW, 0F);
		builder.define(DATA_TIP_DEPTH, 0F);
	}

	/** Its tip as drawn: degrees, toward x and z, metres dropped, metres slid out, and the pivot's distance out (the tub's edge). */
	public float[] tipped(float partialTick) {
		int start = entityData.get(DATA_TIP_START);
		if (start < 0) {
			return new float[6];
		}
		double t = Math.max(0, Math.min(1, ((int) level().getGameTime() - start + partialTick) / TIP_TICKS));
		double s = t * t;   // it falls: slow over the edge, fast at the end
		double yaw = Math.toRadians(entityData.get(DATA_TIP_YAW));
		float depth = entityData.get(DATA_TIP_DEPTH);
		return new float[] {(float) (TIP_MOST * s), (float) -Math.sin(yaw), (float) Math.cos(yaw), (float) (depth * s), (float) (0.6 * depth * s), (float) (Dragline.TUB_M / 2)};
	}

	/** True while it is tipping into the cut (tests). */
	public boolean tipping() {
		return entityData.get(DATA_TIP_START) >= 0;
	}

	@Override
	protected boolean ownTick(ServerLevel level) {
		int start = entityData.get(DATA_TIP_START);
		if (start < 0) {
			return false;
		}
		long t = level.getGameTime() - start;
		if (t % 10 == 0) {
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 2, getZ(), 12, 6, 1, 6, 0.01);
		}
		if (t >= TIP_TICKS) {
			breakApart(level);
		}
		return true;
	}

	/** Lifted from its parts with its cable and job: its strip starts where it stands, and it goes to work. */
	public void start() {
		originX = getX();
		originZ = getZ();
		mode = Mode.RUNNING;
		register();
		status = "lifted off its parts: DIGGING; " + job.describe() + (cable == null ? "; NO CABLE" : "; " + cable.describe());
	}

	// ---- its draw -----------------------------------------------------------------------------------------------------------------

	/** What it is doing now, for its draw. */
	/** Where its next cast goes, or null (tests and films). */
	public @Nullable BlockPos dumpAt() {
		return dumpAt;
	}

	public Dragline.Phase phase() {
		if (walkTick >= 0) {
			return Dragline.Phase.WALK;
		}
		return cycleTick >= 0 ? Dragline.phase(cycleTick) : Dragline.Phase.IDLE;
	}

	@Override
	protected double electricWatts(double engineWatts) {
		if (hand) {
			return handWatts;
		}
		return groundGone && mode == Mode.RUNNING ? Dragline.STANDING_WATTS : Dragline.watts(phase());
	}

	// ---- work ---------------------------------------------------------------------------------------------------------------------

	@Override
	protected void work(ServerLevel level) {
		plc(level);
		levers(level);
		if (!hand) {
			doWork(level);
		}
		if (cable != null && cable.house(level) instanceof PitSwitchHouseBlockEntity house) {
			house.reportPlc(cable.outlet(), getUUID(), mode == Mode.RUNNING, mode == Mode.DONE, cycles, hand);
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
		if (groundGone) {
			if (level.getGameTime() % 200 == 0) {
				groundCheck(level);
			}
			return;
		}
		if (walkTick >= 0 || cycleTick >= 0) {
			dev.wildcatter.tailings.quest.Milestones.fire(level, blockPosition(), dev.wildcatter.tailings.quest.Milestones.DRAGLINE_RUNNING);
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
		if (hand) {
			// a stop always wins: in HAND the PLC's RUN off sets the brakes; on does not move the levers
			plcHold = run != null && !run;
			return;
		}
		plcHold = false;
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

	/** Its strip goes in the slope record, so its highwall and spoil are watched (Part 2). */
	private void register() {
		if (level() instanceof ServerLevel server) {
			Vec3 f = square(ahead()), sd = square(castSide());
			stripId = dev.wildcatter.tailings.pit.Slopes.register(server, getUUID(), originX, originZ, (int) f.x, (int) f.z, (int) sd.x, (int) sd.z, floorY(), job.width(), job.length(),
					batter(server));
		}
	}

	/** The highwall's batter it digs: auto by the world rule. */
	private int batter(ServerLevel level) {
		return job.batter(dev.wildcatter.tailings.pit.Slopes.on(level));
	}

	public long stripId() {
		return stripId;
	}

	public boolean hand() {
		return hand;
	}

	public boolean groundGone() {
		return groundGone;
	}

	public boolean bucketBuried() {
		return bucketBuried;
	}

	/** The cell the bucket lies in, if it is on the ground (or in it); null while it hangs clear. */
	private @Nullable BlockPos bucketCell(ServerLevel level) {
		Vec3 at = bucketAt(swing(), bucketOut(), bucketUp());
		if (bucketUp() > groundAt(level, at) + 1.5) {
			return null;
		}
		return BlockPos.containing(at.x, at.y + 0.5, at.z);
	}

	/**
	 * A slide's ground is coming to rest in {@code cells}: lying on the ground under them, the bucket is buried. In HAND it must be
	 * dragged out (W); in AUTO the job's next bucket is the ground over it, dragged out at its rate.
	 */
	public void slideOver(ServerLevel level, java.util.Set<BlockPos> cells) {
		if (bucketBuried || tipping() || wrecked) {
			return;
		}
		BlockPos b = bucketCell(level);
		if (b == null || !(cells.contains(b) || cells.contains(b.above()) || cells.contains(b.above(2)))) {
			return;
		}
		bucketBuried = true;
		status = "BUCKET BURIED: a slide ran over its bucket; drag it out";
		tell(status);
		if (!hand && cycleTick >= 0 && walkTick < 0) {
			cycleTick = -1;   // the job stops its cycle; its next bucket starts from the buried one
		}
	}

	/** In HAND: the bucket is free once nothing stands over it. */
	private void unburyCheck(ServerLevel level) {
		BlockPos b = bucketCell(level);
		if (b == null || (net.minecraft.world.level.block.FallingBlock.isFree(level.getBlockState(b.above())) && net.minecraft.world.level.block.FallingBlock.isFree(level.getBlockState(b.above(2))))) {
			bucketBuried = false;
			status = "HAND: the bucket is out of the slide";
			tell(status);
		}
	}

	public long handCasts() {
		return handCasts;
	}

	public double swingRate() {
		return swingRate;
	}

	public double handWatts() {
		return handWatts;
	}

	private int floorY() {
		return (int) Math.floor(getY() + 0.5);
	}

	private static Vec3 square(Vec3 v) {
		return new Vec3(Math.round(v.x), 0, Math.round(v.z));
	}

	/** Its cast side's unit vector: to its right, or its left. */
	private Vec3 castSide() {
		return job.castRight() ? right() : right().scale(-1);
	}

	/**
	 * A world column's centre in the strip's frame: {across, along}. It always faces square to the grid, so its axes are rounded to whole
	 * ones (sin 180 degrees is 1.2e-16, which put the strip's first row a hair behind its start, facing north).
	 */
	private double[] frame(double x, double z) {
		Vec3 f = square(ahead()), s = square(castSide());
		double dx = x - originX, dz = z - originZ;
		return new double[] {dx * s.x + dz * s.z, dx * f.x + dz * f.z};
	}

	/** A new cycle: fills the bucket from the strip in reach, top down, and picks where to cast it; or walks on, or is DONE. */
	private void startCycle(ServerLevel level) {
		boolean buried = bucketBuried;
		if (buried) {
			bucketBuried = false;   // its next bucket is the slid ground over it, from the top down: it drags the bucket out
		}
		if (!load.empty() && !buried) {
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
		if (job.stopOnCracks() && stripId >= 0 && dev.wildcatter.tailings.pit.Slopes.cracksNear(level, stripId, getX(), getZ(), Dragline.REACH_M)) {
			status = "WARNING: CRACKS in its highwall or spoil within its reach: it stands (Stop on cracks); survey it, or set Stop on cracks off to dig on";
			retryIn = 200;
			return;
		}
		int floor = floorY();
		int top = floor + Dragline.DIG_ABOVE_M - 1, bottom = floor - job.seam();
		int batter = batter(level);
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
					if (!Dragline.inStrip(p[0], p[1], job.width(), job.length(), batter, floor - y)) {
						break;   // the highwall's face, laid back at its batter: deeper cells are further into the strip
					}
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
				if (stripId >= 0) {
					dev.wildcatter.tailings.pit.Slopes.done(level, stripId);
				}
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
		status = buried ? String.format(Locale.ROOT, "DIGGING: dragging its buried bucket out of the slide, %s", load.tonnes())
				: String.format(Locale.ROOT, "DIGGING: %s, %d quarters of bank from %d m down", load.tonnes(), bucketQuarters, Math.max(0, floor - first.pos().getY()));
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
		// the bucket opens 4 m over the spoil: a heavy, short pour, falling from height (open-pit.md, Pouring)
		Vec3 lip = new Vec3(dumpAt.getX() + 0.5, dumpAt.getY() + 3.5, dumpAt.getZ() + 0.5);
		Vec3 land = new Vec3(lip.x, groundUnder(level, lip, 40), lip.z);
		// out of the bucket while it hangs there: 1 s, or its dump if that is shorter
		pour(level, load, new dev.wildcatter.tailings.pit.Pouring.Spec(dumpAt, lip, land, Math.max(3, Math.min(20, Dragline.DUMP_TICKS / divisor)), 30, 3.5F));
		refreshLoad();
		if (stripId >= 0) {
			dev.wildcatter.tailings.pit.Slopes.cast(level, stripId, dumpAt);
		}
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
		Vec3 to = position().add(square(ahead()).scale(Dragline.STEP_M));
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
		Vec3 f = square(ahead());
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

	// ---- by hand (dragline-hand-control.md, Part 1) ----------------------------------------------------------------------------------

	private String lastReadout = "";

	/** Its HAND readout as last shown on its operator's action bar (films). */
	public String handReadoutText() {
		return lastReadout;
	}

	/** Where its operator's eyes are in the cab, turned with the house (films). */
	public Vec3 cabEye() {
		double r = Math.toRadians(swing());
		Vec3 seat = seat();
		double x = seat.x * Math.cos(r) + seat.z * Math.sin(r), z = -seat.x * Math.sin(r) + seat.z * Math.cos(r);
		return local(x, seat.y + 1.6, z);
	}

	/** Test hook: a bucket of {@code blocks} bank blocks of {@code id}, as if dragged full. */
	public void testFill(String id, int blocks) {
		load.add(id, (long) PitMass.kg(id) * blocks);
		bucketQuarters = 4 * blocks;
		refreshLoad();
	}

	/** Test hook: an empty bucket. */
	public void emptyBucket() {
		load.clear();
		bucketQuarters = 0;
		refreshLoad();
	}

	/** Test hook: puts the bucket at a pose, as a player would have swung, dragged and hoisted it. */
	public void handPose(double swingDeg, double out, double up) {
		pose(swingDeg, out, up);
	}

	/** Test hook: a player sits in its cab, so the test's keys ({@link #testDrive}) are an operator's. */
	public void testSeat(boolean seated) {
		testSeated = seated;
	}

	private boolean seated() {
		return getFirstPassenger() instanceof net.minecraft.server.level.ServerPlayer || testSeated;
	}

	private static boolean anyKey(net.minecraft.world.entity.player.Input k) {
		return k.forward() || k.backward() || k.left() || k.right() || k.jump() || k.sprint();
	}

	/** Its levers, each tick: a seated player takes it into HAND by touching a key; getting out (or sprint held 2 s alone) hands it back. */
	private void levers(ServerLevel level) {
		net.minecraft.world.entity.player.Input k = keys();
		boolean seated = seated();
		if (!anyKey(k)) {
			latch = false;
		}
		if (mode != Mode.RUNNING) {
			hand = false;
			if (seated && anyKey(k) && sayIn-- <= 0) {
				sayIn = 40;
				tell("the levers do nothing: " + status);
			}
			return;
		}
		if (!hand) {
			if (!seated || !anyKey(k) || latch || groundGone) {
				return;
			}
			if (walkTick >= 0) {
				if (sayIn-- <= 0) {
					sayIn = 40;
					tell("WALKING: the levers take it after the step");
				}
				return;
			}
			takeOver();
		}
		if (!seated) {
			handBack(level, "its operator got out: AUTO");
			return;
		}
		if (outOfFuel()) {
			handWatts = 0;
			if (sayIn-- <= 0) {
				sayIn = 40;
				tell(noPower());
			}
			return;
		}
		// jump and sprint together (hoist up and down at once, which means nothing) held 2 s hand it back
		boolean chord = k.sprint() && k.jump() && !k.forward() && !k.backward() && !k.left() && !k.right();
		sprintTicks = chord ? sprintTicks + 1 : 0;
		if (sprintTicks >= 40) {
			sprintTicks = 0;
			latch = true;
			handBack(level, "handed back to AUTO from the cab");
			tell("AUTO: the job carries on");
			return;
		}
		moveByHand(level, k);
	}

	private void takeOver() {
		hand = true;
		cycleTick = -1;   // the job stops planning; nothing is undone: the bucket stays where it is with what it holds
		biteY = Integer.MIN_VALUE;
		lastCut = Integer.MIN_VALUE;
		swingRate = 0;
		status = "HAND: its operator has the levers";
		tell("HAND: W/S drag, A/D swing, jump hoist up, sprint lower; S hoisted and loaded dumps. Jump and sprint 2 s, or get out, for AUTO");
	}

	/** Back to its job, carrying on from where the bucket is: loaded, it casts; empty, it swings back to dig. */
	private void handBack(ServerLevel level, String why) {
		hand = false;
		swingRate = 0;
		handWatts = 0;
		biteY = Integer.MIN_VALUE;
		retryIn = 0;
		status = "DIGGING: " + why;
		if (!load.empty()) {
			if (pickDump(level, frame(getX(), getZ())[1])) {
				digSwing = swing();
				digOut = bucketOut() + 10;   // the swing out lerps from 10 m in from its dig point: so from where the bucket is
				digUp = bucketUp();
				cycleTick = Dragline.FILL_TICKS;
				dumped = false;
			}
			return;
		}
		dumpSwing = swing();
		dumpOut = bucketOut();
		dumpUp = bucketUp();
		cycleTick = Dragline.FILL_TICKS + Dragline.SWING_OUT_TICKS + Dragline.DUMP_TICKS;
		dumped = true;
	}

	private void tell(String what) {
		if (getFirstPassenger() instanceof Player p) {
			p.sendOverlayMessage(Component.literal("Dragline: " + what));
		}
	}

	/** The bucket's world position: its out and up turned by the swing about the tub's centre. */
	public Vec3 bucketAt(double swingDeg, double out, double up) {
		double r = Math.toRadians(swingDeg);
		return local(out * Math.sin(r), 0, out * Math.cos(r)).add(0, floorY() - getY() + up, 0);
	}

	/** The ground's top under the bucket, metres over the tub's floor. */
	private double groundAt(ServerLevel level, Vec3 at) {
		return groundUnder(level, new Vec3(at.x, Math.max(at.y, floorY() - Dragline.DIG_DEPTH_M) + 1.0, at.z), 120) - floorY();
	}

	private void moveByHand(ServerLevel level, net.minecraft.world.entity.player.Input k) {
		double dt = 0.05 * divisor;
		int key = k.left() ? -1 : k.right() ? 1 : 0;
		int hoist = k.jump() ? -1 : k.sprint() ? 1 : 0;
		int drag = k.forward() ? -1 : k.backward() ? 1 : 0;
		boolean loaded = !load.empty();
		Vec3 at = bucketAt(swing(), bucketOut(), bucketUp());
		double ground = groundAt(level, at);
		boolean grounded = bucketUp() <= ground + 0.05;
		boolean full = bucketQuarters >= Dragline.BUCKET_BANK_QUARTERS || load.total() >= Dragline.BUCKET_KG;
		boolean digging = drag < 0 && grounded && hoist >= 0 && !full;
		if (!grounded || drag >= 0) {
			biteY = Integer.MIN_VALUE;
		}
		if (plcHold) {
			key = 0;
			hoist = 0;
			drag = 0;
			digging = false;
			if (sayIn-- <= 0) {
				sayIn = 40;
				tell("stopped by the plant: its RUN is off");
			}
		}
		if (bucketBuried) {
			// buried: it neither hoists nor swings until it is dragged out from under the slid ground
			if ((key != 0 || hoist < 0) && sayIn-- <= 0) {
				sayIn = 40;
				tell("BUCKET BURIED: drag it out (W) before it hoists or swings");
			}
			key = 0;
			hoist = Math.max(0, hoist);
			swingRate = 0;
			if (drag < 0 && grounded) {
				digging = true;   // dragged through the slid ground, full or not, it comes out
			}
		}
		// dump: the drag paid out with the bucket hoisted clear and loaded
		if (drag > 0 && loaded && bucketUp() > ground + 1 && !dumpLatch) {
			dumpLatch = true;
			dumpByHand(level, at, ground);
			return;
		}
		if (drag <= 0) {
			dumpLatch = false;
		}
		boolean pushing = key != 0 && swingRate * key >= 0 && Math.abs(swingRate) < dev.wildcatter.tailings.pit.DraglineRopes.SWING_MAX;
		double asked = dev.wildcatter.tailings.pit.DraglineRopes.asked(digging, drag != 0, hoist, loaded, pushing);
		double share = dev.wildcatter.tailings.pit.DraglineRopes.share(asked);
		handWatts = dev.wildcatter.tailings.pit.DraglineRopes.draw(asked);
		// the swing, with its inertia; into a block it stops
		double before = swingRate;
		double[] sw = dev.wildcatter.tailings.pit.DraglineRopes.swing(swingRate, key, dt, share);
		double nextSwing = swing() + sw[0];
		Vec3 swung = bucketAt(nextSwing, bucketOut(), bucketUp());
		BlockPos cell = BlockPos.containing(swung.x, swung.y + 1.5, swung.z);
		if (sw[0] != 0 && !level.getBlockState(cell).getCollisionShape(level, cell).isEmpty()) {
			swingRate = 0;
			nextSwing = swing();
		} else {
			swingRate = sw[1];
		}
		if (key == 0 && before != 0 && swingRate == 0) {
			level.playSound(null, blockPosition(), dev.wildcatter.tailings.TailingsSounds.DRAGLINE_SWING_BRAKE, net.minecraft.sounds.SoundSource.BLOCKS, 2F, 1F);
		}
		// the ropes
		double hoistSpeed = loaded ? dev.wildcatter.tailings.pit.DraglineRopes.HOIST_LOADED : dev.wildcatter.tailings.pit.DraglineRopes.HOIST_EMPTY;
		double dragSpeed = digging ? dev.wildcatter.tailings.pit.DraglineRopes.DRAG_DIG : dev.wildcatter.tailings.pit.DraglineRopes.DRAG_FREE;
		dev.wildcatter.tailings.pit.DraglineRopes.Bucket from = new dev.wildcatter.tailings.pit.DraglineRopes.Bucket(bucketOut(), bucketUp(), grounded);
		if (digging && biteY == Integer.MIN_VALUE) {
			biteY = (int) Math.floor(floorY() + ground) - 1;
		}
		// digging, it runs along the floor it bit to, whatever the uncut ground under its next step
		double along = digging ? biteY - floorY() : ground;
		dev.wildcatter.tailings.pit.DraglineRopes.Bucket to = dev.wildcatter.tailings.pit.DraglineRopes.move(from, hoist * hoistSpeed * share * dt, drag * dragSpeed * share * dt, along);
		if (digging) {
			int metre = (int) Math.floor(to.out());
			if (metre != lastCut) {
				lastCut = metre;
				cut(level, nextSwing, to.out());
			}
			// it sits in its cut: the ground under it is the floor it bit to
			to = new dev.wildcatter.tailings.pit.DraglineRopes.Bucket(to.out(), biteY - floorY(), true);
		}
		pose(nextSwing, to.out(), to.up());
		if (bucketBuried) {
			unburyCheck(level);
		}
		if (sayIn-- <= 0) {
			sayIn = 10;
			lastReadout = handReadout(level, hoist * hoistSpeed * share, drag * dragSpeed * share);
			tell(lastReadout);
		}
	}

	/** Cuts the row 5 wide across the bucket's path at {@code out}, from its bite layer up to its 4 m height. */
	private void cut(ServerLevel level, double swingDeg, double out) {
		double r = Math.toRadians(swingDeg);
		Vec3 along = local(Math.sin(r), 0, Math.cos(r)).subtract(position());
		Vec3 side = new Vec3(along.z, 0, -along.x);
		Vec3 c = bucketAt(swingDeg, out, 0);
		long quarterKg = PitMass.ROCK_KG / 4;
		boolean hard = false, ore = false;
		for (int w = -2; w <= 2; w++) {
			for (int y = biteY + 3; y >= biteY; y--) {
				if (bucketQuarters >= Dragline.BUCKET_BANK_QUARTERS || load.total() >= Dragline.BUCKET_KG) {
					break;
				}
				BlockPos p = BlockPos.containing(c.x + side.x * w, y, c.z + side.z * w);
				BlockState state = level.getBlockState(p);
				if (state.isAir() || !state.getFluidState().isEmpty()) {
					continue;
				}
				if (state.getBlock() == TailingsBlocks.MUCK && level.getBlockEntity(p) instanceof MuckBlockEntity muck) {
					long before = load.total();
					long room = Math.min(Dragline.BUCKET_KG - before, (long) (Dragline.BUCKET_BANK_QUARTERS - bucketQuarters) * quarterKg);
					PitShovelEntity.takeQuarters(level, p, muck, load, before + room);
					bucketQuarters += (int) Math.round((load.total() - before) / (double) quarterKg);
					continue;
				}
				if (state.hasBlockEntity()) {
					continue;   // never a machine's block
				}
				if (state.is(TailingsBlocks.TENSION_CRACK)) {
					load.add("minecraft:dirt", PitMass.kg("minecraft:dirt"));
					bucketQuarters += 4;
					level.removeBlock(p, false);
					continue;
				}
				String id = PitMass.dugAs(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
				if (ore(id)) {
					ore = true;
					continue;   // the seam is left for loaders and trucks
				}
				if (PitMass.soft(id)) {
					long kg = PitMass.kg(id);
					if (Dragline.fits(load.total(), bucketQuarters, kg, 4)) {
						load.add(id, kg);
						bucketQuarters += 4;
						level.removeBlock(p, false);
					}
				} else if (state.canBeReplaced()) {
					level.destroyBlock(p, false);
				} else {
					hard = true;
				}
			}
		}
		refreshLoad();
		if (hard || ore) {
			status = hard ? "HAND: the bucket skids on rock: hard, blast it first" : "HAND: the bucket rides over the seam: it is left for loaders and trucks";
			tell(status);
		}
	}

	/** Dumps the bucket where it hangs: it pours down and sets by the settle rule. */
	private void dumpByHand(ServerLevel level, Vec3 at, double ground) {
		int quarters = bucketQuarters;
		// over a truck's body it loads the truck, to its mass, as a shovel does; the rest spills
		HaulTruckEntity truck = null;
		for (HaulTruckEntity t : level.getEntitiesOfClass(HaulTruckEntity.class, new net.minecraft.world.phys.AABB(at.x - 8, at.y - 30, at.z - 8, at.x + 8, at.y + 2, at.z + 8))) {
			net.minecraft.world.phys.AABB box = t.getBoundingBox();
			if (at.x >= box.minX && at.x <= box.maxX && at.z >= box.minZ && at.z <= box.maxZ && at.y >= box.minY && t.load().room() > 0) {
				truck = t;
			}
		}
		if (truck != null) {
			long before = truck.load().total();
			String first = load.contents().isEmpty() ? "minecraft:dirt" : load.contents().keySet().iterator().next();
			load.pourInto(truck.load());
			truck.refreshLoad();
			refreshLoad();
			long moved = truck.load().total() - before;
			streamInto(level, truck, first, moved);   // drawn only: the body is loaded
			tell(String.format(Locale.ROOT, "dumped into the truck: %.1f t; it holds %s", moved / 1000.0, truck.load().tonnes()));
			if (load.empty()) {
				handCast(level, truck.blockPosition(), quarters);
				status = String.format(Locale.ROOT, "HAND: cast %d by hand into a truck, %.1f t", handCasts, moved / 1000.0);
				level.playSound(null, truck.blockPosition(), dev.wildcatter.tailings.TailingsSounds.MUCK_TIP, net.minecraft.sounds.SoundSource.BLOCKS, 4F, 0.8F);
				return;
			}
			ground = groundAt(level, at);   // what the truck could not take spills beside it
		}
		BlockPos start = BlockPos.containing(at.x, floorY() + ground, at.z);
		Vec3 lip = new Vec3(at.x, at.y + 0.5, at.z);
		Vec3 land = new Vec3(at.x, floorY() + ground, at.z);
		pour(level, load, new dev.wildcatter.tailings.pit.Pouring.Spec(start, lip, land, Math.max(3, Math.min(20, Dragline.DUMP_TICKS / divisor)), 30, 3.5F));
		refreshLoad();
		if (load.empty()) {
			handCast(level, start, quarters);
			status = String.format(Locale.ROOT, "HAND: cast %d by hand at %s", handCasts, start.toShortString());
		} else {
			status = "HAND: some of the load found no room under the bucket";
		}
		level.playSound(null, start, dev.wildcatter.tailings.TailingsSounds.MUCK_TIP, net.minecraft.sounds.SoundSource.BLOCKS, 4F, 0.7F);
	}

	/** A bucket cast by hand: a cycle, its bank, the milestone, and the strip's spoil marked if it landed there. */
	private void handCast(ServerLevel level, BlockPos at, int quarters) {
		cycles++;
		handCasts++;
		bankQuarters += quarters;
		bucketQuarters = 0;
		dev.wildcatter.tailings.quest.Milestones.fire(level, at, dev.wildcatter.tailings.quest.Milestones.DRAGLINE_HAND_CAST);
		if (stripId >= 0) {
			dev.wildcatter.tailings.pit.Slopes.cast(level, stripId, at);
		}
	}

	private String handReadout(ServerLevel level, double hoistSpeed, double dragSpeed) {
		String slope = stripId >= 0 && dev.wildcatter.tailings.pit.Slopes.cracksNear(level, stripId, getX(), getZ(), Dragline.REACH_M) ? " | CRACKS" : "";
		int pct = (int) Math.round(100.0 * Math.max(bucketQuarters / (double) Dragline.BUCKET_BANK_QUARTERS, load.total() / (double) Dragline.BUCKET_KG));
		// short enough to read whole on a phone: swing, the bucket's reach and height, its load, the ropes, the draw
		return String.format(Locale.ROOT, "HAND | swing %+.0f° | %.0f m out %+.0f m | %.0f t %d%% | hoist %+.1f drag %+.1f m/s | %.0f kW%s%s", swing(), bucketOut(), bucketUp(),
				load.total() / 1000.0, pct, -hoistSpeed, -dragSpeed, handWatts / 1000, slope, bucketBuried ? " | BURIED" : "");
	}

	// ---- a slide (dragline-hand-control.md, Part 2) ---------------------------------------------------------------------------------

	/**
	 * After a slide near it: with a third or more of its tub's ground gone it goes down with the slide and is wrecked; with some, it
	 * stops, GROUND GONE, until the ground under its tub is made up.
	 */
	public void groundCheck(ServerLevel level) {
		int floor = floorY();
		int gone = 0, all = 0;
		double goneX = 0, goneZ = 0;
		double r = Dragline.TUB_M / 2;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int dx = (int) -Math.ceil(r); dx <= Math.ceil(r); dx++) {
			for (int dz = (int) -Math.ceil(r); dz <= Math.ceil(r); dz++) {
				if (dx * dx + dz * dz > r * r) {
					continue;
				}
				all++;
				at.set((int) Math.floor(getX()) + dx, floor - 1, (int) Math.floor(getZ()) + dz);
				if (net.minecraft.world.level.block.FallingBlock.isFree(level.getBlockState(at))) {
					gone++;
					goneX += dx;
					goneZ += dz;
				}
			}
		}
		if (all > 0 && gone * 3 >= all) {
			wreck(level, goneX, goneZ);
		} else if (gone > 0) {
			groundGone = true;
			hand = false;
			cycleTick = -1;
			walkTick = -1;
			status = String.format(Locale.ROOT, "GROUND GONE: a slide took the ground from under %d of its tub's %d cells; make it up, then RUN", gone, all);
		} else if (groundGone) {
			groundGone = false;
			status = "STOPPED: the ground under its tub is made up: RUN";
			mode = Mode.STOPPED;
		}
	}

	/** Wrecked by a slide: it tips toward the cut beside it (ahead if none is found) and falls into it, then breaks into its parts. */
	public void wreck(ServerLevel level) {
		Vec3 a = ahead();
		wreck(level, a.x, a.z);
	}

	/**
	 * Wrecked by a slide, the ground gone toward {@code dx, dz}: it tips over its tub's edge that way and falls into the cut over 3 s,
	 * its operator thrown clear and hurt; then it breaks where it lands into every part, its cable and its bucket's load. Nothing is
	 * lost.
	 */
	public void wreck(ServerLevel level, double dx, double dz) {
		if (tipping() || wrecked) {
			return;
		}
		double len = Math.hypot(dx, dz);
		if (len < 1e-6) {
			Vec3 a = ahead();
			dx = a.x;
			dz = a.z;
			len = 1;
		}
		dx /= len;
		dz /= len;
		// how far down it lands: the ground a tub's width out that way
		double out = Dragline.TUB_M;
		double landY = groundUnder(level, new Vec3(getX() + dx * out, getY() + 1, getZ() + dz * out), 80);
		float depth = (float) Math.max(2, getY() - landY);
		hand = false;
		cycleTick = -1;
		walkTick = -1;
		mode = Mode.STOPPED;
		status = "WRECKED: going down with the slide";
		entityData.set(DATA_TIP_YAW, (float) Math.toDegrees(Math.atan2(-dx, dz)));
		entityData.set(DATA_TIP_DEPTH, depth);
		entityData.set(DATA_TIP_START, (int) level.getGameTime());
		for (net.minecraft.world.entity.Entity rider : java.util.List.copyOf(getPassengers())) {
			if (rider instanceof net.minecraft.world.entity.LivingEntity living) {
				living.stopRiding();
				living.hurtServer(level, level.damageSources().fallingBlock(null), dev.wildcatter.tailings.pit.Slopes.SLIDE_DAMAGE);
			}
		}
		level.playSound(null, blockPosition(), dev.wildcatter.tailings.TailingsSounds.HIGHWALL_SLIDE, net.minecraft.sounds.SoundSource.BLOCKS, 12F, 0.6F);
		Tailings.LOGGER.info("Tailings: a dragline at {} is going down with a slide, {} m into the cut", blockPosition().toShortString(), Math.round(depth));
	}

	/** At the bottom: it breaks where it landed into every part, its cable and its bucket's load. */
	private void breakApart(ServerLevel level) {
		float[] tip = tipped(0);
		double x = getX() + tip[1] * (tip[4] + Dragline.TUB_M / 2), z = getZ() + tip[2] * (tip[4] + Dragline.TUB_M / 2);
		double landY = groundUnder(level, new Vec3(x, getY() + 1, z), 80);
		setPos(x, landY, z);
		for (DraglineLayout.Part part : DraglineLayout.Part.values()) {
			spawnAtLocation(level, new ItemStack(TailingsBlocks.plantBlock(part.machine())));
		}
		for (java.util.Map.Entry<String, Integer> e : load.wholeBlocks().entrySet()) {
			for (int left = e.getValue(); left > 0; left -= 64) {
				spawnAtLocation(level, dev.wildcatter.tailings.block.Feeds.stack(e.getKey(), Math.min(64, left)));
			}
		}
		load.clear();
		dropCable(level);
		Tailings.LOGGER.info("Tailings: a dragline went down with a slide at {} and was wrecked into its parts", blockPosition().toShortString());
		level.playSound(null, blockPosition(), dev.wildcatter.tailings.TailingsSounds.MUCK_TIP, net.minecraft.sounds.SoundSource.BLOCKS, 12F, 0.4F);
		level.sendParticles(net.minecraft.core.particles.ParticleTypes.CAMPFIRE_COSY_SMOKE, getX(), getY() + 2, getZ(), 60, 10, 3, 10, 0.02);
		wrecked = true;
		discard();
	}

	private boolean wrecked;

	/** True once a slide has wrecked it (tests). */
	public boolean wrecked() {
		return wrecked;
	}

	// ---- RUN and STOP -------------------------------------------------------------------------------------------------------------

	private void resume(String why) {
		if (mode == Mode.DONE) {
			return;
		}
		if (groundGone) {
			status = "GROUND GONE: a slide took the ground from under its tub; make it up, then RUN";
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
		if (stripId >= 0 && level() instanceof ServerLevel server) {
			dev.wildcatter.tailings.pit.Slopes.job(server, stripId, job.width(), job.length(), batter(server));
		}
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
		hand = input.getBooleanOr("hand", false);
		swingRate = input.getDoubleOr("swing_rate", 0);
		handCasts = input.getLongOr("hand_casts", 0);
		groundGone = input.getBooleanOr("ground_gone", false);
		bucketBuried = input.getBooleanOr("bucket_buried", false);
		stripId = input.getLongOr("strip_id", -1);
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
		output.putBoolean("hand", hand);
		output.putDouble("swing_rate", swingRate);
		output.putLong("hand_casts", handCasts);
		output.putBoolean("ground_gone", groundGone);
		output.putBoolean("bucket_buried", bucketBuried);
		output.putLong("strip_id", stripId);
	}
}
