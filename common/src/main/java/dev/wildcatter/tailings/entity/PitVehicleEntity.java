package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.block.ChunkRunner;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.haul.TrainHarm;
import dev.wildcatter.tailings.pit.PitDrive;
import dev.wildcatter.tailings.pit.PitLoad;
import dev.wildcatter.tailings.platform.Platform;
import java.util.Map;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A rubber-tyred pit machine a player drives (docs/multiblocks/open-pit.md): the haul truck and the wheel loader. Right-click to sit
 * in its cab; the movement keys drive it on the server from the rider's keys, by its engine's power ({@link PitDrive}); jump works
 * its body or bucket; sneak gets out. It is at its real size, its box turning with it, and it steps up one block at a time.
 */
public abstract class PitVehicleEntity extends Entity implements SoundingEntity {
	private static final EntityDataAccessor<Float> DATA_SPEED = SynchedEntityData.defineId(PitVehicleEntity.class, EntityDataSerializers.FLOAT);
	/** The truck's body or the loader's bucket arm, 0 down to 1 up. */
	private static final EntityDataAccessor<Float> DATA_LIFT = SynchedEntityData.defineId(PitVehicleEntity.class, EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Boolean> DATA_ENGINE = SynchedEntityData.defineId(PitVehicleEntity.class, EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Boolean> DATA_LOADED = SynchedEntityData.defineId(PitVehicleEntity.class, EntityDataSerializers.BOOLEAN);
	/** Its trailing cable's switch house and outlet, for drawing the cable (P5a). */
	private static final EntityDataAccessor<java.util.Optional<net.minecraft.core.BlockPos>> DATA_CABLE = SynchedEntityData.defineId(PitVehicleEntity.class,
			EntityDataSerializers.OPTIONAL_BLOCK_POS);
	private static final EntityDataAccessor<Integer> DATA_CABLE_OUTLET = SynchedEntityData.defineId(PitVehicleEntity.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> DATA_ELECTRIC = SynchedEntityData.defineId(PitVehicleEntity.class, EntityDataSerializers.BOOLEAN);
	protected static final double TICK = 1.0 / 20;

	protected final PitLoad load;
	private double speed;
	/** The grade under it, averaged over the last 10 m it moved: plus is uphill in the way it faces. */
	private double slope;
	private double fall;
	private boolean lastJump;
	private int refresh;
	protected String status = "standing";
	/** A game test's keys, when no player drives. */
	private Input testInput = Input.EMPTY;
	/** Diesel in its tank, litres (open-pit.md, P2a). */
	private double fuel;
	private boolean engineOn;
	/** On the client: how far its wheels have rolled, in metres, now and a tick ago, for drawing them turning. */
	private double rolled, rolledBefore;
	/** Its trailing cable from a pit switch house, or null (open-pit-p5-big-machines.md, P5a). */
	protected @Nullable CableLink cable;
	/** What its electric drives draw this tick, watts. */
	private double demand;

	protected PitVehicleEntity(EntityType<?> type, Level level) {
		super(type, level);
		this.load = new PitLoad(spec().ratingKg());
		this.fuel = tank() * dev.wildcatter.tailings.pit.PitFuel.NEW_FILL;
	}

	/** Its fuel tank, litres. */
	public abstract double tank();

	/** How far its wheels have rolled, between ticks, on the client. */
	public float rolled(float partialTick) {
		return (float) (rolledBefore + (rolled - rolledBefore) * partialTick);
	}

	public double fuel() {
		return fuel;
	}

	public void addFuel(double litres) {
		fuel = Math.max(0, Math.min(tank(), fuel + litres));
	}

	/** Test hook. */
	public void setFuel(double litres) {
		fuel = Math.max(0, Math.min(tank(), litres));
	}

	/** Dry: out of diesel, or, running on its cable, no power at its end of the cable. */
	public boolean outOfFuel() {
		return electric() ? cable == null || !cable.live() : fuel <= 0;
	}

	/** Why it will not run when {@link #outOfFuel}: out of fuel, or what its cable says. */
	public String noPower() {
		if (!electric()) {
			return "out of fuel";
		}
		return cable == null ? "NO CABLE: couple a trailing cable from a pit switch house" : cable.why();
	}

	// ---- the trailing cable (P5a) -------------------------------------------------------------------------------------------------

	/** It runs on its trailing cable now, not diesel. */
	public boolean electric() {
		return false;
	}

	/** It has a cable reel: a trailing cable may be coupled to it. */
	public boolean takesCable() {
		return false;
	}

	/** Its drives' peak draw, watts, which its outlet's breaker is set from. */
	public double peakWatts() {
		return spec().watts();
	}

	public @Nullable CableLink cable() {
		return cable;
	}

	/** What its drives draw now, watts (0 on diesel). */
	public double demand() {
		return demand;
	}

	/** A trailing cable coupled to it, or taken off (null). */
	public void setCable(@Nullable CableLink link) {
		cable = link;
		syncCable();
	}

	protected void syncCable() {
		entityData.set(DATA_CABLE, cable == null ? java.util.Optional.empty() : java.util.Optional.of(cable.house()));
		entityData.set(DATA_CABLE_OUTLET, cable == null ? 0 : cable.outlet());
		entityData.set(DATA_ELECTRIC, electric());
	}

	/** On the client too: the switch house its cable runs from, and the outlet. */
	public java.util.Optional<net.minecraft.core.BlockPos> cableHouse() {
		return entityData.get(DATA_CABLE);
	}

	public int cableOutlet() {
		return entityData.get(DATA_CABLE_OUTLET);
	}

	/** It reached the end of its cable: what it was doing stops (the drill's pattern). */
	protected void cableEnd() {
	}

	/** Whether its engine ran this tick. */
	public boolean engineOn() {
		return engineOn;
	}

	/** What drives it when no player is in the cab: a self-driving route, or null to stand. */
	public record Drive(double throttle, double steer, boolean jump) {
		public static final Drive STAND = new Drive(0, 0, false);
	}

	/** Its autopilot's controls this tick, or null when it has none (the default). */
	protected @Nullable Drive autopilot(ServerLevel level) {
		return null;
	}

	/** It runs by itself: its engine idles and its chunks stay loaded. */
	public boolean selfRunning() {
		return false;
	}

	/** Its engine idles while it stands: by default whenever it runs by itself; a truck parked by dispatch shuts it off. */
	protected boolean engineIdles() {
		return selfRunning();
	}

	/** Extra engine power for its body hoist or bucket, as a fraction of rated, beyond driving. */
	protected double workFraction() {
		return 0;
	}

	/** Clicked with the survey book: take its route, or what the book sets. */
	protected void onSurveyBook(ServerLevel level, Player player, ItemStack book) {
		player.sendOverlayMessage(Component.literal(getType().getDescription().getString() + ": the survey book has nothing to set here"));
	}

	/** A player sat in the cab: a self-driving machine drops off its route. */
	protected void takenOver() {
	}

	/** Its route and stop for the readout, or empty. */
	protected String routeInfo() {
		return "";
	}

	public abstract PitDrive.Spec spec();

	protected abstract Item item();

	/** The jump key, pressed once: the truck tips, the loader digs or dumps. */
	protected abstract void jump(ServerLevel level, @Nullable Player driver);

	/** It may not drive: the truck's body is up, the loader is in a pass. */
	public abstract boolean busy();

	/** The jump key pressed once, with nobody in the cab: for game tests. */
	public void pressJump(ServerLevel level) {
		if (electric() && outOfFuel()) {
			status = noPower();   // as its jump key: no power on its cable, no work
			return;
		}
		jump(level, null);
	}

	/** Where its cab seat is, from its centre on the ground: across (plus is to its right), up, ahead. */
	protected abstract Vec3 seat();

	/** Each server tick, after it has moved. */
	protected void work(ServerLevel level) {
	}

	/** Each server tick before it moves, with its speed for this tick set: the dozer's blade works the ground in front here. */
	protected void beforeMove(ServerLevel level) {
	}

	/** A tick it runs itself instead (a truck going over a crest); true when it did. */
	protected boolean ownTick(ServerLevel level) {
		return false;
	}

	/** The fastest it may go now, m/s (the dozer pushing a load); its top speed otherwise. */
	protected double speedCap() {
		return Double.MAX_VALUE;
	}

	/** Its speed, set by what it struck (the dozer's blade at hard rock). */
	protected void stop() {
		speed = 0;
	}

	public PitLoad load() {
		return load;
	}

	public double speed() {
		return speed;
	}

	public double slope() {
		return slope;
	}

	public String status() {
		return status;
	}

	void setStatus(String status) {
		this.status = status;
	}

	protected boolean engineSynced() {
		return entityData.get(DATA_ENGINE);
	}

	protected float syncedSpeed() {
		return entityData.get(DATA_SPEED);
	}

	public float lift() {
		return entityData.get(DATA_LIFT);
	}

	protected void setLift(float lift) {
		entityData.set(DATA_LIFT, lift);
	}

	public boolean loaded() {
		return entityData.get(DATA_LOADED);
	}

	/** After its load changes. */
	public void refreshLoad() {
		entityData.set(DATA_LOADED, load.total() > 0);
	}

	public double massKg() {
		return spec().emptyKg() + load.total();
	}

	/** Keys for a game test to drive it with. */
	public void testDrive(Input input) {
		this.testInput = input;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_SPEED, 0F);
		builder.define(DATA_LIFT, 0F);
		builder.define(DATA_LOADED, false);
		builder.define(DATA_ENGINE, false);
		builder.define(DATA_CABLE, java.util.Optional.empty());
		builder.define(DATA_CABLE_OUTLET, 0);
		builder.define(DATA_ELECTRIC, false);
	}

	// ---- its frame: ahead and to its right ---------------------------------------------------------------------------------------

	protected Vec3 ahead() {
		double yaw = Math.toRadians(getYRot());
		return new Vec3(-Math.sin(yaw), 0, Math.cos(yaw));
	}

	protected Vec3 right() {
		double yaw = Math.toRadians(getYRot());
		return new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
	}

	/** A point given from its centre on the ground: across (plus right), up, ahead. */
	public Vec3 local(double across, double up, double forward) {
		return position().add(right().scale(across)).add(ahead().scale(forward)).add(0, up, 0);
	}

	// ---- its box -----------------------------------------------------------------------------------------------------------------

	/** Its box runs its real length and width, turned with it. */
	@Override
	protected AABB makeBoundingBox(Vec3 at) {
		return boxAt(at, getYRot());
	}

	protected AABB boxAt(Vec3 at, float yawDegrees) {
		PitDrive.Spec spec = spec();
		double half = spec.length() / 2, side = spec.width() / 2;
		double yaw = Math.toRadians(yawDegrees), sin = Math.abs(Math.sin(yaw)), cos = Math.abs(Math.cos(yaw));
		double x = sin * half + cos * side, z = cos * half + sin * side;
		return new AABB(at.x - x, at.y, at.z - z, at.x + x, at.y + spec.height(), at.z + z);
	}

	@Override
	public void setYRot(float yaw) {
		boolean turned = yaw != getYRot();
		super.setYRot(yaw);
		if (turned) {
			setBoundingBox(makeBoundingBox(position()));
		}
	}

	@Override
	public float maxUpStep() {
		return 1.0F;
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	@Override
	public boolean canBeCollidedWith(@Nullable Entity other) {
		return true;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	// ---- driving -----------------------------------------------------------------------------------------------------------------

	protected Input keys() {
		return getFirstPassenger() instanceof ServerPlayer driver ? driver.getLastClientInput() : testInput;
	}

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel server)) {
			SoundingEntity.Hook.start.accept(this);
			rolledBefore = rolled;
			rolled += entityData.get(DATA_SPEED) * TICK;
			return;
		}
		if (ownTick(server)) {
			return;
		}
		Vec3 start = position();
		boolean driven = getFirstPassenger() instanceof ServerPlayer;
		Drive drive = null;
		if (driven) {
			if (selfRunning()) {
				takenOver();
			}
		} else {
			drive = autopilot(server);
		}
		Input keys = keys();
		boolean jumpNow = drive != null ? drive.jump() : keys.jump() && !lastJump;
		lastJump = keys.jump();
		boolean dry = outOfFuel();
		if (jumpNow) {
			if (dry) {
				status = noPower();
				if (getFirstPassenger() instanceof Player p) {
					p.sendOverlayMessage(Component.literal(getType().getDescription().getString() + ": " + status));
				}
			} else {
				jump(server, getFirstPassenger() instanceof Player p ? p : null);
			}
		}
		double throttle = busy() || dry ? 0 : drive != null ? drive.throttle() : keys.forward() ? 1 : keys.backward() ? -1 : 0;
		double speedBefore = speed;
		speed = busy() ? 0 : PitDrive.next(spec(), massKg(), speed, throttle, slope, TICK);
		speed = Math.max(-speedCap(), Math.min(speedCap(), speed));
		double steer = drive != null ? drive.steer() : keys.left() ? 1 : keys.right() ? -1 : 0;
		if (Math.abs(speed) > 0.05 && steer != 0) {
			float turned = getYRot() - (float) Math.toDegrees(steer * speed / spec().turnRadius() * TICK);
			if (server.noCollision(this, boxAt(position(), turned).deflate(0.05))) {
				setYRot(turned);
			}
		}
		beforeMove(server);
		fall = onGround() ? -0.04 : Math.max(-3.0, fall - 0.08);
		Vec3 before = position();
		Vec3 want = ahead().scale(speed * TICK);
		move(MoverType.SELF, new Vec3(want.x, fall, want.z));
		setDeltaMovement(Vec3.ZERO);
		double moved = Math.hypot(getX() - before.x, getZ() - before.z);
		if (horizontalCollision && moved < want.horizontalDistance() * 0.3) {
			speed = 0;   // into something it cannot climb
		}
		if (moved > 0.001) {
			double sample = (getY() - before.y) * Math.signum(speed == 0 ? 1 : speed);
			slope += (sample - slope * moved) / 10.0;
			slope = Math.max(-0.5, Math.min(0.5, slope));
		}
		entityData.set(DATA_SPEED, (float) speed);
		entityData.set(DATA_ENGINE, engineOn);
		if (Math.abs(speed) > 1.0) {
			strike(server);
		}
		work(server);
		if (cable != null) {
			pullCable(server, start);
		}
		burn(driven, throttle, speedBefore);
		if (cable != null) {
			cable.answer(server, getUUID(), electric() ? demand : 0, peakWatts());
		}
		if (entityData.get(DATA_ELECTRIC) != electric()) {
			entityData.set(DATA_ELECTRIC, electric());
		}
		if (refresh-- <= 0 && (isVehicle() || selfRunning() || Math.abs(speed) > 0.05 || busy())) {
			refresh = ChunkRunner.REFRESH_TICKS;
			ChunkRunner.refresh(server, blockPosition());
			ChunkRunner.refresh(server, net.minecraft.core.BlockPos.containing(local(0, 0, spec().length())));
		}
	}

	/**
	 * Diesel by the engine's work (open-pit.md, P2a): off when nobody drives it, it runs no route and does no work. On its cable the
	 * same work is what its drives draw from the switch house, and no diesel burns.
	 */
	private void burn(boolean driven, double throttle, double before) {
		double extra = workFraction();
		boolean moving = Math.abs(speed) > 0.05 || Math.abs(before) > 0.05 || throttle != 0;
		engineOn = !outOfFuel() && (driven || engineIdles() || moving || extra > 0);
		demand = 0;
		if (!engineOn) {
			return;
		}
		double watts = moving ? dev.wildcatter.tailings.pit.PitFuel.drivingWatts(spec(), massKg(), before, speed, slope, TICK)
				: spec().watts() * dev.wildcatter.tailings.pit.PitFuel.IDLE;
		watts = Math.min(spec().watts(), watts + extra * spec().watts());
		if (electric()) {
			demand = electricWatts(watts);
			return;
		}
		fuel = Math.max(0, fuel - dev.wildcatter.tailings.pit.PitFuel.litres(watts, TICK));
	}

	/** What its drives draw from the cable for {@code engineWatts} of work: the same work by default (the rope shovel draws by its cycle). */
	protected double electricWatts(double engineWatts) {
		return engineWatts;
	}

	/** Its cable pays out or in as it moves; at its end it is put back and stops, CABLE END; torn out, its outlet trips. */
	private void pullCable(ServerLevel server, Vec3 start) {
		CableLink.Move move = cable.move(server, start, position());
		if (move == CableLink.Move.END) {
			setPos(start);
			stop();
			entityData.set(DATA_SPEED, 0F);
			status = cable.why();
			cableEnd();
			if (getFirstPassenger() instanceof Player p) {
				p.sendOverlayMessage(Component.literal(getType().getDescription().getString() + ": " + status));
			}
		} else if (move == CableLink.Move.TORN) {
			var house = cable.house(server);
			if (house != null) {
				house.torn(cable.outlet(), "the cable was torn out");
			}
			setCable(null);
			status = "its trailing cable was torn out";
		}
	}

	/** What it hits while moving is hurt (config/tailings-train.properties, truck_damage). */
	private void strike(ServerLevel server) {
		float amount = TrainHarm.truckDamageFor(server.getServer(), Platform.INSTANCE.configDir().resolve("tailings-train.properties"));
		if (amount <= 0) {
			return;
		}
		for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(0.2), e -> !hasPassenger(e))) {
			living.hurtServer(server, server.damageSources().generic(), amount);
		}
	}

	// ---- the cab -----------------------------------------------------------------------------------------------------------------

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		return getPassengers().isEmpty();
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction move) {
		Vec3 seat = seat();
		Vec3 at = local(seat.x, seat.y, seat.z);
		move.accept(passenger, at.x, at.y, at.z);
	}

	/** Out of the cab door, on the ground beside it. */
	@Override
	public Vec3 getDismountLocationForPassenger(LivingEntity passenger) {
		Vec3 seat = seat();
		double side = Math.signum(seat.x == 0 ? -1 : seat.x) * (spec().width() / 2 + 1.0);
		Vec3 out = local(side, 0, seat.z);
		if (level().noCollision(passenger, passenger.getBoundingBox().move(out.subtract(passenger.position())))) {
			return out;
		}
		return super.getDismountLocationForPassenger(passenger);
	}

	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		ItemStack held = player.getItemInHand(hand);
		if (held.getItem() instanceof dev.wildcatter.tailings.item.SurveyBookItem && level() instanceof ServerLevel server) {
			onSurveyBook(server, player, held);
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown() || isVehicle()) {
			player.sendOverlayMessage(readout());
			return InteractionResult.SUCCESS;
		}
		player.startRiding(this);
		return InteractionResult.SUCCESS;
	}

	public Component readout() {
		String route = routeInfo();
		return Component.literal(getType().getDescription().getString() + ": " + status + "; " + load.tonnes() + "; "
				+ String.format(java.util.Locale.ROOT, "%.0f km/h; ", Math.abs(speed) * 3.6) + powerInfo() + (route.isEmpty() ? "" : "; " + route));
	}

	/** Its fuel, or its cable and what it draws. */
	public String powerInfo() {
		if (electric()) {
			return cable == null ? "on its cable: NO CABLE" : String.format(java.util.Locale.ROOT, "on its cable, %.0f kW; ", demand / 1000) + cable.describe();
		}
		return String.format(java.util.Locale.ROOT, "fuel %.0f of %.0f L", fuel, tank()) + (cable == null ? "" : "; " + cable.describe());
	}

	/** Only a sneaking hit with an empty hand breaks it, into its item and its load. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
			for (Map.Entry<String, Integer> e : load.wholeBlocks().entrySet()) {
				for (int left = e.getValue(); left > 0; left -= 64) {
					spawnAtLocation(level, Feeds.stack(e.getKey(), Math.min(64, left)));
				}
			}
			load.clear();
			dropCable(level);
			spawnAtLocation(level, new ItemStack(item()));
			ejectPassengers();
			discard();
			return true;
		}
		return false;
	}

	/** Its trailing cable comes off its outlet and back as items, a 400 m reel each. */
	public void dropCable(ServerLevel level) {
		if (cable == null) {
			return;
		}
		var house = cable.house(level);
		if (house != null) {
			house.uncouple(cable.outlet());
		}
		int items = (int) Math.max(1, Math.round(cable.metres() / dev.wildcatter.tailings.pit.PitPower.CABLE_M));
		spawnAtLocation(level, new ItemStack(dev.wildcatter.tailings.TailingsItems.TRAILING_CABLE, items));
		setCable(null);
	}

	// ---- sound -------------------------------------------------------------------------------------------------------------------

	@Override
	public net.minecraft.sounds.@Nullable SoundEvent loop() {
		if (!isVehicle() && !entityData.get(DATA_ENGINE) && Math.abs(entityData.get(DATA_SPEED)) < 0.05F && lift() == 0) {
			return null;   // parked, engine off
		}
		if (entityData.get(DATA_ELECTRIC)) {
			return dev.wildcatter.WildcatterSounds.MOTOR_HUM;   // on its cable: its drives' motors, no diesel
		}
		return Math.abs(entityData.get(DATA_SPEED)) > 0.3F ? dev.wildcatter.WildcatterSounds.DIESEL_LOAD : dev.wildcatter.WildcatterSounds.DIESEL_IDLE;
	}

	@Override
	public float soundPitch() {
		return 0.8F + 0.03F * Math.abs(entityData.get(DATA_SPEED));
	}

	@Override
	public float soundVolume() {
		return 1.0F;
	}

	// ---- saving ------------------------------------------------------------------------------------------------------------------

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		load.load(input.getStringOr("load", ""));
		speed = input.getDoubleOr("speed", 0);
		slope = input.getDoubleOr("slope", 0);
		status = input.getStringOr("status", "standing");
		setLift((float) input.getDoubleOr("lift", 0));
		fuel = input.getDoubleOr("fuel", tank() * dev.wildcatter.tailings.pit.PitFuel.NEW_FILL);
		cable = CableLink.load(input);
		syncCable();
		refreshLoad();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putString("load", load.save());
		output.putDouble("speed", speed);
		output.putDouble("slope", slope);
		output.putString("status", status);
		output.putDouble("lift", lift());
		output.putDouble("fuel", fuel);
		if (cable != null) {
			cable.save(output);
		}
	}
}
