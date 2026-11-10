package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.pit.PitLoad;
import dev.wildcatter.tailings.pit.PitMass;
import dev.wildcatter.tailings.pit.StopKind;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A crawler shovel that loads trucks (docs/multiblocks/open-pit-p5-big-machines.md, P5c and P5d): the hydraulic front shovel and the
 * electric rope shovel. A pass digs its bucket full from the face in front (soft ground or muck, top down; standing rock is "hard rock:
 * blast it first"; a misfired hole stops it), then swings up to 90 degrees to a stopped truck whose body is near its dump arc, either
 * side, and dumps. Muck comes out by the quarter, so a bucket fills to its rating. The last pass into a truck is a part bucket of what
 * the truck has room for; a truck with under half a bucket of room is loaded and is sent on its way.
 *
 * <p>In AUTO (a survey book's route with a LOAD stake near it) it digs whenever its bucket is empty and loads the trucks that pull in
 * on either side: it fills one, then turns to the other side's while the first leaves (double-side loading).
 */
public abstract class PitShovelEntity extends PitVehicleEntity {
	/** How far its house is swung from its tracks' heading, degrees (plus to its right), for drawing it. */
	private static final EntityDataAccessor<Float> DATA_SWING = SynchedEntityData.defineId(PitShovelEntity.class, EntityDataSerializers.FLOAT);
	/** It swings no further than this to a truck (spec). */
	public static final double MOST_SWING = 90;

	private int passLeft;
	private boolean dumping;
	private double swingTo;
	private int divisor = 1;
	private @Nullable BlockPos autoStake;
	private @Nullable UUID loading;
	private int lastSide;
	private int passesThisTruck;
	private long passes;
	private long tonnesKg;
	private int retryIn;

	protected PitShovelEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	// ---- its figures --------------------------------------------------------------------------------------------------------------

	/** The face a pass digs: cells either side of its centre line, metres up from its floor, metres in. */
	protected abstract int reachAcross();

	protected abstract int reachUp();

	protected abstract int reachIn();

	/** How far ahead of its centre the face starts, metres. */
	protected abstract double faceAhead();

	/** The radius of its dump arc from its centre, metres; and how near a truck's body must be to it. */
	protected abstract double dumpRadius();

	protected abstract double truckReach();

	/** Its highest dump, metres: a truck's body must be under it. */
	protected abstract double dumpHeight();

	/** A pass: digging, then swinging, dumping and swinging back, in ticks. */
	protected abstract int digTicks();

	protected abstract int dumpTicks();

	/** How near its LOAD stake it works by itself, metres. */
	protected double autoReach() {
		return 30;
	}

	public long bucketKg() {
		return spec().ratingKg();
	}

	// ---- for tests and readouts ---------------------------------------------------------------------------------------------------

	public void fastPasses(int divisor) {
		this.divisor = Math.max(1, divisor);
	}

	public PitLoad bucket() {
		return load;
	}

	/** Passes it has dumped into trucks, ever. */
	public long passes() {
		return passes;
	}

	/** Passes into the truck it is loading, or that it last loaded. */
	public int passesThisTruck() {
		return passesThisTruck;
	}

	public long tonnesKg() {
		return tonnesKg;
	}

	public float swing() {
		return entityData.get(DATA_SWING);
	}

	/** True while it digs (the hoist part of its cycle). */
	public boolean digging() {
		return passLeft > 0 && !dumping;
	}

	public @Nullable UUID loadingTruck() {
		return loading;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_SWING, 0F);
	}

	@Override
	public boolean busy() {
		return passLeft > 0;
	}

	@Override
	protected double workFraction() {
		return passLeft > 0 ? dev.wildcatter.tailings.pit.PitFuel.PASS : 0;
	}

	@Override
	public boolean selfRunning() {
		return autoStake != null;
	}

	// ---- AUTO ---------------------------------------------------------------------------------------------------------------------

	/** Sets it to work by itself at the route's LOAD stake near it; why not, or null. Public for tests. */
	public @Nullable String setAuto(ServerLevel level, java.util.List<BlockPos> stakes) {
		if (stakes.isEmpty()) {
			autoStake = null;
			status = "driven by hand";
			return null;
		}
		for (BlockPos stake : stakes) {
			if (dev.wildcatter.tailings.block.SurveyStakeBlock.kind(level.getBlockState(stake)) == StopKind.LOAD
					&& Math.hypot(stake.getX() + 0.5 - getX(), stake.getZ() + 0.5 - getZ()) <= autoReach()) {
				autoStake = stake.immutable();
				status = "working by itself at the LOAD stake " + stake.toShortString();
				return null;
			}
		}
		return "no LOAD stake on the route within " + (int) autoReach() + " m";
	}

	/** In AUTO without a route: works at the trucks that come to it (tests, and a shovel set going by hand). */
	public void setAutoHere() {
		autoStake = blockPosition();
		status = "working by itself";
	}

	@Override
	protected void onSurveyBook(ServerLevel level, Player player, net.minecraft.world.item.ItemStack book) {
		String why = setAuto(level, dev.wildcatter.tailings.item.SurveyBookItem.route(book));
		say(player, why == null ? status : why);
	}

	@Override
	protected void takenOver() {
		autoStake = null;
		status = "driven by hand";
	}

	@Override
	protected String routeInfo() {
		return (autoStake == null ? "" : "AUTO; ") + String.format(Locale.ROOT, "%d passes, %,.0f t loaded", passes, tonnesKg / 1000.0);
	}

	private void workAlone(ServerLevel level) {
		if (autoStake == null || passLeft > 0 || isVehicle()) {
			return;
		}
		if (outOfFuel()) {
			status = noPower();
			return;
		}
		HaulTruckEntity truck = chooseTruck(level);
		if (load.empty()) {
			if (retryIn-- > 0) {
				return;
			}
			// the last pass into a truck is a part bucket: what it has room for
			long most = truck == null ? bucketKg() : Math.min(bucketKg(), truck.load().room());
			dig(level, null, most);
			if (load.empty() && !status.startsWith("hard rock") && !status.startsWith("misfire")) {
				status = "face worked out: nothing left in reach";
				retryIn = 200;
			} else if (load.empty()) {
				retryIn = 200;
			}
			return;
		}
		if (truck == null) {
			status = "bucket full: WAITING FOR TRUCK";
			return;
		}
		startDump(truck);
	}

	/**
	 * The truck to load: the one it is loading while it stands in reach and has room; else a waiting truck on the other side from the
	 * last it loaded, else any waiting truck. A truck with under half a bucket of room is loaded: it is told so, and goes.
	 */
	private @Nullable HaulTruckEntity chooseTruck(ServerLevel level) {
		HaulTruckEntity best = null;
		for (HaulTruckEntity truck : trucksInReach(level)) {
			if (truck.getUUID().equals(loading)) {
				if (truck.load().room() < bucketKg() / 2) {
					truck.loadedOut();   // loaded: away it goes
					lastSide = side(truck);
					loading = null;
					continue;
				}
				return truck;
			}
		}
		for (HaulTruckEntity truck : trucksInReach(level)) {
			if (truck.load().room() < bucketKg() / 2 || truck.busy() || Math.abs(truck.speed()) > 0.05) {
				continue;
			}
			if (best == null || (side(truck) != lastSide && side(best) == lastSide)) {
				best = truck;
			}
		}
		if (best != null && !best.getUUID().equals(loading)) {
			loading = best.getUUID();
			passesThisTruck = 0;
		}
		return best;
	}

	/** -1 a truck to its left, 1 to its right. */
	private int side(HaulTruckEntity truck) {
		return angleTo(truck.position()) < 0 ? -1 : 1;
	}

	/** The angle from its heading to {@code at}, degrees, plus to its right. */
	private double angleTo(Vec3 at) {
		Vec3 to = at.subtract(position());
		Vec3 ahead = ahead(), right = right();
		return Math.toDegrees(Math.atan2(to.x * right.x + to.z * right.z, to.x * ahead.x + to.z * ahead.z));
	}

	/** Stopped trucks whose body is within its reach of its dump arc (up to 90 degrees either side), and under its dump height. */
	public java.util.List<HaulTruckEntity> trucksInReach(Level level) {
		java.util.List<HaulTruckEntity> out = new java.util.ArrayList<>();
		AABB around = getBoundingBox().inflate(dumpRadius() + truckReach() + 10, 4, dumpRadius() + truckReach() + 10);
		for (HaulTruckEntity truck : level.getEntitiesOfClass(HaulTruckEntity.class, around)) {
			if (truck.loadingHeight() > dumpHeight() + 1e-9) {
				continue;
			}
			AABB body = truck.getBoundingBox().inflate(truckReach(), 2, truckReach());
			for (double a = -MOST_SWING; a <= MOST_SWING; a += 10) {
				double r = Math.toRadians(a);
				Vec3 point = local(Math.sin(r) * dumpRadius(), 1, Math.cos(r) * dumpRadius());
				if (body.contains(point)) {
					out.add(truck);
					break;
				}
			}
		}
		return out;
	}

	// ---- driving by hand ----------------------------------------------------------------------------------------------------------

	@Override
	protected void jump(ServerLevel level, @Nullable Player driver) {
		if (passLeft > 0) {
			return;
		}
		if (load.empty()) {
			HaulTruckEntity truck = chooseTruck(level);
			dig(level, driver, truck == null ? bucketKg() : Math.min(bucketKg(), Math.max(truck.load().room(), 1)));
			return;
		}
		HaulTruckEntity truck = chooseTruck(level);
		if (truck == null) {
			say(driver, trucksInReach(level).isEmpty() ? "no haul truck under its arc" : "no truck in reach with room");
			return;
		}
		startDump(truck);
	}

	private void startDump(HaulTruckEntity truck) {
		dumping = true;
		passLeft = dumpTicks() / divisor;
		swingTo = Math.max(-MOST_SWING, Math.min(MOST_SWING, angleTo(truck.position())));
		loading = truck.getUUID();
		status = "LOADING: swinging to the truck";
	}

	protected void say(@Nullable Player driver, String what) {
		status = what;
		if (driver != null) {
			driver.sendOverlayMessage(Component.literal(getType().getDescription().getString() + ": " + what));
		}
	}

	/** One pass: fills the bucket from the face in front, up to {@code mostKg}. Public for tests. */
	public void dig(ServerLevel level, @Nullable Player driver, long mostKg) {
		long room = Math.min(bucketKg(), Math.max(0, mostKg));
		Vec3 lip = local(0, 0, faceAhead());
		int floor = (int) Math.floor(getY() + 0.5);
		Vec3 in = ahead(), across = right();
		for (int up = reachUp() - 1; up >= 0; up--) {
			for (int depth = 0; depth < reachIn(); depth++) {
				for (int side = -reachAcross(); side < reachAcross(); side++) {
					BlockPos pos = cell(lip, in, across, depth, side, floor + up);
					if (level.getBlockEntity(pos) instanceof dev.wildcatter.tailings.block.BlastholeBlockEntity hole && hole.collarEntity() != null && hole.collarEntity().misfired()) {
						say(driver, "misfire in the muck: stop");
						return;
					}
				}
			}
		}
		boolean hard = false;
		outer:
		for (int up = reachUp() - 1; up >= 0; up--) {
			for (int depth = 0; depth < reachIn(); depth++) {
				for (int side = -reachAcross(); side < reachAcross(); side++) {
					if (load.total() >= room) {
						break outer;
					}
					BlockPos pos = cell(lip, in, across, depth, side, floor + up);
					BlockState state = level.getBlockState(pos);
					if (state.isAir() || !state.getFluidState().isEmpty()) {
						continue;
					}
					if (state.getBlock() == TailingsBlocks.MUCK && level.getBlockEntity(pos) instanceof MuckBlockEntity muck) {
						takeQuarters(level, pos, muck, room);
						continue;
					}
					String id = PitMass.dugAs(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
					if (PitMass.soft(id)) {
						if (load.total() + PitMass.kg(id) > room) {
							continue;
						}
						load.add(id, PitMass.kg(id));
						level.removeBlock(pos, false);
					} else if (state.canBeReplaced()) {
						level.destroyBlock(pos, false);
					} else {
						hard = true;
					}
				}
			}
		}
		refreshLoad();
		if (load.empty()) {
			say(driver, hard ? "hard rock: blast it first" : "FACE OUT OF REACH: nothing in reach of the bucket");
			return;
		}
		dumping = false;
		passLeft = digTicks() / divisor;
		say(driver, "DIGGING: " + load.tonnes());
		level.playSound(null, lip.x, lip.y, lip.z, dev.wildcatter.tailings.TailingsSounds.MUCK_LOAD, net.minecraft.sounds.SoundSource.BLOCKS, 2.5F, 0.8F);
	}

	private static BlockPos cell(Vec3 lip, Vec3 in, Vec3 across, int depth, int side, int y) {
		return BlockPos.containing(lip.x + in.x * (depth + 0.5) + across.x * (side + 0.5), y, lip.z + in.z * (depth + 0.5) + across.z * (side + 0.5));
	}

	/** Takes what fits of a muck block, a quarter at a time; what is left stays as a smaller block. */
	private void takeQuarters(ServerLevel level, BlockPos pos, MuckBlockEntity muck, long room) {
		Map<String, Integer> left = new LinkedHashMap<>(muck.quarters());
		for (Map.Entry<String, Integer> e : left.entrySet()) {
			long quarter = PitMass.kg(e.getKey()) / 4;
			int take = (int) Math.min(e.getValue(), Math.max(0, (room - load.total()) / Math.max(1, quarter)));
			if (take > 0) {
				load.add(e.getKey(), take * quarter);
				e.setValue(e.getValue() - take);
			}
		}
		left.values().removeIf(q -> q <= 0);
		if (left.isEmpty()) {
			level.removeBlock(pos, false);
		} else {
			muck.set(left);
		}
	}

	@Override
	protected void work(ServerLevel level) {
		workAlone(level);
		if (passLeft <= 0) {
			float swung = swing();
			if (swung != 0) {
				entityData.set(DATA_SWING, Math.abs(swung) < 3 ? 0F : swung - Math.signum(swung) * 3F);
			}
			return;
		}
		passLeft--;
		int length = Math.max(1, (dumping ? dumpTicks() : digTicks()) / divisor);
		float done = 1 - passLeft / (float) length;
		if (dumping) {
			// swing out over the first 40 %, dump, swing back over the last 40 %
			double out = done < 0.4 ? done / 0.4 : done > 0.6 ? (1 - done) / 0.4 : 1;
			entityData.set(DATA_SWING, (float) (swingTo * out));
			setLift((float) Math.min(1, out + 0.2));
		} else {
			setLift(done);
		}
		if (passLeft == 0 && dumping) {
			dumpInto(level);
			setLift(0);
		}
	}

	private void dumpInto(ServerLevel level) {
		Player driver = getFirstPassenger() instanceof Player p ? p : null;
		HaulTruckEntity truck = null;
		for (HaulTruckEntity t : trucksInReach(level)) {
			if (t.getUUID().equals(loading)) {
				truck = t;
			}
		}
		if (truck == null) {
			say(driver, "the truck moved away");
			return;
		}
		long before = truck.load().total();
		load.pourInto(truck.load());
		truck.refreshLoad();
		refreshLoad();
		long moved = truck.load().total() - before;
		if (moved > 0) {
			passes++;
			passesThisTruck++;
			tonnesKg += moved;
			afterPass(moved);
		}
		say(driver, String.format(Locale.ROOT, "LOADING: pass %d, %.1f t; the truck holds %s", passesThisTruck, moved / 1000.0, truck.load().tonnes()));
		level.playSound(null, truck.getX(), truck.getY(), truck.getZ(), dev.wildcatter.tailings.TailingsSounds.MUCK_TIP, net.minecraft.sounds.SoundSource.BLOCKS, 3F, 0.9F);
		if (truck.load().room() < bucketKg() / 2) {
			truck.loadedOut();
			lastSide = side(truck);
			loading = null;
			say(driver, String.format(Locale.ROOT, "truck loaded in %d passes: %s", passesThisTruck, truck.load().tonnes()));
		}
	}

	/** A pass has gone into a truck (the rope shovel counts its PASSES for its PLC). */
	protected void afterPass(long kg) {
	}

	// ---- saving ------------------------------------------------------------------------------------------------------------------

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		passLeft = input.getIntOr("pass_left", 0);
		dumping = input.getBooleanOr("dumping", false);
		swingTo = input.getDoubleOr("swing_to", 0);
		long stake = input.getLongOr("auto_stake", Long.MIN_VALUE);
		autoStake = stake == Long.MIN_VALUE ? null : BlockPos.of(stake);
		String truck = input.getStringOr("loading", "");
		loading = truck.isEmpty() ? null : UUID.fromString(truck);
		lastSide = input.getIntOr("last_side", 0);
		passesThisTruck = input.getIntOr("passes_truck", 0);
		passes = input.getLongOr("passes", 0);
		tonnesKg = input.getLongOr("tonnes_kg", 0);
		divisor = Math.max(1, input.getIntOr("fast", 1));
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("pass_left", passLeft);
		output.putBoolean("dumping", dumping);
		output.putDouble("swing_to", swingTo);
		if (autoStake != null) {
			output.putLong("auto_stake", autoStake.asLong());
		}
		output.putString("loading", loading == null ? "" : loading.toString());
		output.putInt("last_side", lastSide);
		output.putInt("passes_truck", passesThisTruck);
		output.putLong("passes", passes);
		output.putLong("tonnes_kg", tonnesKg);
		output.putInt("fast", divisor);
	}
}
