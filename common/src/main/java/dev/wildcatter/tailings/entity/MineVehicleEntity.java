package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.ChunkRunner;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.block.RailBlockEntity;
import dev.wildcatter.tailings.block.RailView;
import dev.wildcatter.tailings.block.SignalBlockEntity;
import dev.wildcatter.tailings.haul.CarLoad;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.haul.Track;
import dev.wildcatter.tailings.haul.Traction;
import dev.wildcatter.tailings.haul.TrainHarm;
import dev.wildcatter.tailings.haul.TrainPath;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.platform.Platform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A mine locomotive or a mine car (docs/multiblocks/haulage.md): every vehicle is an entity, and the train glides along the rails.
 * The locomotive drives: it holds the train's path ({@link TrainPath}), its speed and direction, and its cars by UUID, and each
 * tick it runs the train along the track and sets every car at its place behind it. Nothing of the train is stored on the rails.
 */
public class MineVehicleEntity extends Entity {
	private static final EntityDataAccessor<Boolean> DATA_LOADED = SynchedEntityData.defineId(MineVehicleEntity.class, EntityDataSerializers.BOOLEAN);
	private static final double TICK = 1.0 / 20;
	/** Cells each train covered on its last tick, per level: two trains never share one. */
	private static final Map<Level, Map<Long, Claim>> CLAIMS = new WeakHashMap<>();

	private record Claim(UUID train, long tick) {
	}

	// the locomotive's train
	private @Nullable TrainPath path;
	/** Where the locomotive's centre is on the path. */
	private double front;
	private double speed;
	/** 1 runs locomotive-first, -1 backs the train up. */
	private int direction = 1;
	private boolean running = true;
	private boolean derailed;
	private final List<UUID> cars = new ArrayList<>();
	private final Set<BlockPos> sections = new HashSet<>();
	private int endWait;
	private int refresh;
	private int coupleScan;
	private String status = "standing";
	private @Nullable BlockPos lamp;
	/** How far the track ran ahead last tick, to tell track broken ahead from the track's end. */
	private double reachedAhead = -1;

	// a car's
	private final CarLoad load = new CarLoad();
	private @Nullable UUID locomotiveId;

	public MineVehicleEntity(EntityType<?> type, Level level) {
		super(type, level);
		setNoGravity(true);
	}

	public boolean locomotive() {
		return getType() == TailingsEntities.LOCOMOTIVE;
	}

	public CarLoad load() {
		return load;
	}

	/** After its load changes: tells the client whether to draw the load. */
	public void refreshLoad() {
		entityData.set(DATA_LOADED, load.total() > 0);
	}

	public boolean loaded() {
		return entityData.get(DATA_LOADED);
	}

	public String status() {
		return status;
	}

	public double speed() {
		return speed;
	}

	public int direction() {
		return direction;
	}

	public boolean derailed() {
		return derailed;
	}

	public List<UUID> carIds() {
		return cars;
	}

	public @Nullable UUID locomotiveId() {
		return locomotiveId;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		builder.define(DATA_LOADED, false);
	}

	// ---- placing ---------------------------------------------------------------------------------------------------------------

	/** Puts a locomotive on the rail at {@code pos}, facing {@code heading} along it (or the rail's own way). */
	public static @Nullable MineVehicleEntity placeLocomotive(ServerLevel level, BlockPos pos, Heading heading) {
		Track.Cell cell = new RailView(level).at(pos);
		if (cell == null || claimed(level, pos, null)) {
			return null;
		}
		Track.Step step = Track.start(cell, heading);
		if (step == null) {
			step = Track.start(cell, cell.facing());
		}
		MineVehicleEntity loco = new MineVehicleEntity(TailingsEntities.LOCOMOTIVE, level);
		loco.path = new TrainPath(step);
		loco.front = step.length() / 2;
		loco.place();
		level.addFreshEntity(loco);
		return loco;
	}

	/** Puts a car on the rail at {@code pos}; it couples to a train whose tail is within reach behind it. */
	public static @Nullable MineVehicleEntity placeCar(ServerLevel level, BlockPos pos, Heading heading) {
		Track.Cell cell = new RailView(level).at(pos);
		if (cell == null || claimed(level, pos, null)) {
			return null;
		}
		Track.Step step = Track.start(cell, heading);
		if (step == null) {
			step = Track.start(cell, cell.facing());
		}
		MineVehicleEntity car = new MineVehicleEntity(TailingsEntities.CAR, level);
		double[] at = Track.point(step, 0.5);
		float[] attitude = new TrainPath(step).attitude(step.length() / 2);
		car.setAt(at[0], at[1], at[2], attitude[0], attitude[1]);
		level.addFreshEntity(car);
		car.coupleToNearest();
		return car;
	}

	/** A standalone car couples to the nearest train whose next car place is within reach. */
	public boolean coupleToNearest() {
		if (locomotive() || locomotiveId != null || !(level() instanceof ServerLevel server)) {
			return false;
		}
		for (MineVehicleEntity loco : server.getEntitiesOfClass(MineVehicleEntity.class, getBoundingBox().inflate(Haul.SPACING * (Haul.MAX_CARS + 2)),
				e -> e.locomotive() && e.isAlive())) {
			if (loco.take(this, Haul.COUPLE_REACH)) {
				return true;
			}
		}
		return false;
	}

	/** Takes {@code car} as its next car if the car stands within {@code reach} of that place on the track behind the train. */
	private boolean take(MineVehicleEntity car, double reach) {
		if (path == null || cars.size() >= Haul.MAX_CARS || car.locomotiveId != null) {
			return false;
		}
		double slot = front - (cars.size() + 1) * Haul.SPACING;
		RailView view = new RailView(level());
		for (int i = 0; i < 8 && slot - Haul.HALF_LENGTH < 0; i++) {
			double moved = path.extendBack(view);
			if (moved == 0) {
				break;
			}
			front += moved;
			slot += moved;
		}
		if (slot - Haul.HALF_LENGTH < 0) {
			return false;
		}
		double[] at = path.point(slot);
		if (car.distanceToSqr(at[0], at[1], at[2]) > reach * reach) {
			return false;
		}
		cars.add(car.getUUID());
		car.locomotiveId = getUUID();
		place();
		return true;
	}

	// ---- saving ----------------------------------------------------------------------------------------------------------------

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		load.load(input.getStringOr("load", ""));
		String loco = input.getStringOr("locomotive", "");
		locomotiveId = loco.isEmpty() ? null : UUID.fromString(loco);
		path = TrainPath.load(input.getStringOr("path", ""));
		front = input.getDoubleOr("front", 0);
		speed = input.getDoubleOr("speed", 0);
		direction = input.getIntOr("direction", 1);
		running = input.getBooleanOr("running", true);
		derailed = input.getBooleanOr("derailed", false);
		status = input.getStringOr("status", "standing");
		cars.clear();
		for (String id : input.getStringOr("cars", "").split(",")) {
			if (!id.isEmpty()) {
				cars.add(UUID.fromString(id));
			}
		}
		refreshLoad();
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		output.putString("load", load.save());
		output.putString("locomotive", locomotiveId == null ? "" : locomotiveId.toString());
		if (path != null) {
			output.putString("path", path.save());
		}
		output.putDouble("front", front);
		output.putDouble("speed", speed);
		output.putInt("direction", direction);
		output.putBoolean("running", running);
		output.putBoolean("derailed", derailed);
		output.putString("status", status);
		StringBuilder ids = new StringBuilder();
		for (UUID id : cars) {
			ids.append(ids.length() == 0 ? "" : ",").append(id);
		}
		output.putString("cars", ids.toString());
	}

	// ---- running ---------------------------------------------------------------------------------------------------------------

	@Override
	public void tick() {
		super.tick();
		if (!(level() instanceof ServerLevel server)) {
			return;
		}
		if (!locomotive()) {
			if (locomotiveId != null && !(server.getEntity(locomotiveId) instanceof MineVehicleEntity loco && loco.cars.contains(getUUID()))) {
				locomotiveId = null;   // its locomotive is gone: it stands where it is
			}
			return;
		}
		if (path == null) {
			discard();
			return;
		}
		List<MineVehicleEntity> train = members(server);
		if (++coupleScan >= 20) {
			coupleScan = 0;
			for (MineVehicleEntity car : server.getEntitiesOfClass(MineVehicleEntity.class, getBoundingBox().inflate(Haul.SPACING * (Haul.MAX_CARS + 2)),
					e -> !e.locomotive() && e.locomotiveId == null && e.isAlive())) {
				if (take(car, 0.75)) {
					train = members(server);
				}
			}
		}
		RailView view = new RailView(server);
		run(server, view, train);
		place(train);
		claim(server, train);
		for (MineVehicleEntity car : train) {
			if (car != this) {
				tipAndLoad(server, car);
			}
		}
		if (speed > 0.05) {
			strike(server, train);
		}
		if (refresh-- <= 0) {
			refresh = ChunkRunner.REFRESH_TICKS;
			for (MineVehicleEntity v : train) {
				ChunkRunner.refresh(server, v.blockPosition());
			}
			double[] ahead = path.point(direction > 0 ? path.length() : 0);
			ChunkRunner.refresh(server, BlockPos.containing(ahead[0], ahead[1], ahead[2]));
		}
		headlight(server);
	}

	/** The locomotive and its cars, in order, that are loaded; a car gone for good leaves the train. */
	private List<MineVehicleEntity> members(ServerLevel server) {
		List<MineVehicleEntity> out = new ArrayList<>();
		out.add(this);
		for (int i = 0; i < cars.size(); i++) {
			if (server.getEntity(cars.get(i)) instanceof MineVehicleEntity car && car.isAlive()) {
				out.add(car);
				continue;
			}
			double[] at = path.point(carPlace(i));
			if (server.isPositionEntityTicking(BlockPos.containing(at[0], at[1], at[2]))) {
				cars.remove(i--);   // its chunk is loaded and it is not there: it was broken
			}
		}
		return out;
	}

	private double carPlace(int index) {
		return front - (index + 1) * Haul.SPACING;
	}

	private double tailEnd() {
		return front - cars.size() * Haul.SPACING - Haul.HALF_LENGTH;
	}

	private double frontEnd() {
		return front + Haul.HALF_LENGTH;
	}

	/** One tick of the train's run along the track. */
	private void run(ServerLevel server, RailView view, List<MineVehicleEntity> train) {
		// the track under and around the train still stands?
		int broken = path.broken(view);
		if (broken >= 0) {
			double from = path.start(broken), to = from + path.steps().get(broken).length();
			double reach = Traction.brakingDistance(speed);
			// under the train, or ahead of it closer than it can stop
			double dangerFrom = tailEnd() - (direction < 0 ? reach : 0), dangerTo = frontEnd() + (direction > 0 ? reach : 0);
			if (to > dangerFrom && from < dangerTo) {
				derail(server, train, from, to);
				return;
			}
			if (from >= frontEnd()) {
				path.trimAfter(from - 1e-6);
			} else {
				double moved = path.trimBefore(to + 1e-6);
				front -= moved;
			}
		}
		if (derailed) {
			if (path.broken(view) < 0) {
				derailed = false;
				status = "back on the rail";
			} else {
				return;
			}
		}
		// lay the track ahead and behind as far as it can stop in, and a little more
		double look = Traction.brakingDistance(Haul.MAX_SPEED) + 2;
		for (int i = 0; i < 64 && path.length() - frontEnd() < look; i++) {
			if (!path.extendFront(view)) {
				break;
			}
		}
		for (int i = 0; i < 64 && tailEnd() < look; i++) {
			double moved = path.extendBack(view);
			if (moved == 0) {
				break;
			}
			front += moved;
		}
		double ahead = direction > 0 ? path.length() - frontEnd() : tailEnd();
		// track that vanished ahead closer than it could stop: a derailment, not the end of the line
		if (reachedAhead >= 0 && ahead < reachedAhead - 1.01 && ahead < Traction.brakingDistance(speed) - 0.05 && speed > 0.1) {
			reachedAhead = -1;
			double gap = direction > 0 ? path.length() : 0;
			derail(server, train, gap - 1, gap + 1);
			return;
		}
		reachedAhead = ahead;

		Track.Step under = path.stepAt(front);
		RailBlockEntity rail = server.getBlockEntity(RailView.pos(under.cell())) instanceof RailBlockEntity r ? r : null;
		boolean fed = rail != null && rail.conductorLive();
		double sine = Track.sine(under) * direction;
		int items = 0;
		for (MineVehicleEntity v : train) {
			items += v.load.total();
		}
		double mass = Traction.mass(train.size() - 1, items);
		double target;
		String why;
		if (!running) {
			target = 0;
			why = "held";
		} else if (!fed) {
			target = 0;
			why = "NO POWER: its rail is not fed";
		} else if (under.cell().graded() && !under.cell().rack()) {
			target = 0;
			why = "NO RACK on the grade";
		} else {
			target = Traction.limit(mass, sine, Haul.TRACTION_WATTS);
			why = String.format("running %.1f m/s", speed);
		}
		double stop = ahead;
		String held = hold(server, view, train);
		if (held != null) {
			stop = Math.min(stop, 0);
			why = held;
		}
		double signal = signalAhead(server);
		if (signal >= 0 && signal < stop) {
			stop = signal;
			why = "waiting at a signal";
		}
		double other = otherTrainAhead(server);
		if (other >= 0 && other < stop) {
			stop = other;
			why = "a train ahead";
		}
		target = Math.min(target, Traction.stoppable(stop));
		target = Math.min(target, slowAhead());
		if (endWait > 0) {
			target = 0;
			if (--endWait == 0) {
				direction = -direction;
				reachedAhead = -1;
				why = "reversing";
			} else {
				why = "at the end of the track";
			}
		} else if (running && fed && ahead < 0.02 && speed < 0.01 && held == null && (signal < 0 || signal > ahead) && (other < 0 || other > ahead)) {
			endWait = Haul.END_WAIT_TICKS;
			why = "at the end of the track";
		}
		speed = Traction.approach(speed, target, TICK);
		double move = Math.min(speed * TICK, Math.max(0, stop));
		if (move < speed * TICK) {
			speed = move / TICK;   // held short by what is ahead
		}
		front += move * direction;
		enterSections(server);
		// drop the track well behind
		if (direction > 0) {
			front -= path.trimBefore(tailEnd() - 2);
		} else {
			path.trimAfter(frontEnd() + 2);
		}
		status = why;
	}

	/**
	 * The fastest it may go now and still be down to the rack brake's 1 m/s where the track ahead turns downhill: a train slows
	 * before the decline, not on it.
	 */
	private double slowAhead() {
		double end = direction > 0 ? frontEnd() : tailEnd();
		double limit = Haul.MAX_SPEED;
		List<Track.Step> steps = path.steps();
		for (int i = 0; i < steps.size(); i++) {
			Track.Step step = direction > 0 ? steps.get(i) : Track.reverse(steps.get(i));
			double start = path.start(i), stop = start + steps.get(i).length();
			double distance = direction > 0 ? start - end : end - stop;
			if (distance >= -1e-6 && Track.sine(step) < -1e-9) {
				limit = Math.min(limit, Math.sqrt(Haul.DOWNHILL_SPEED * Haul.DOWNHILL_SPEED + 2 * Haul.ACCELERATION * Math.max(0, distance)));
			}
		}
		return limit;
	}

	/** A reason the train must hold where it is (a car at the drill or the dump), or null. */
	private @Nullable String hold(ServerLevel server, RailView view, List<MineVehicleEntity> train) {
		for (int i = 1; i < train.size(); i++) {
			MineVehicleEntity car = train.get(i);
			BlockPos rail = RailView.pos(path.stepAt(carPlace(i - 1)).cell());
			DumpBlockEntity pocket = DumpBlockEntity.under(server, rail);
			if (pocket != null && car.load.total() > 0 && pocket.pocket() + car.load.total() > Haul.POCKET_ITEMS) {
				return "waiting at the dump: the pocket is full";
			}
			DrillBlockEntity drill = DrillBlockEntity.loadingAt(server, rail);
			if (drill != null && car.load.room() > 0 && drill.hasMuck()) {
				return "loading at the drill";
			}
		}
		return null;
	}

	/** The distance to the next signal section it may not enter, or -1. */
	private double signalAhead(ServerLevel server) {
		double end = direction > 0 ? frontEnd() : tailEnd();
		List<Track.Step> steps = path.steps();
		for (int i = 0; i < steps.size(); i++) {
			double start = path.start(i), stop = start + steps.get(i).length();
			boolean beyond = direction > 0 ? start >= end - 1e-6 : stop <= end + 1e-6;
			if (!beyond) {
				continue;
			}
			if (server.getBlockEntity(RailView.pos(steps.get(i).cell())) instanceof RailBlockEntity rail && !rail.signal().equals(BlockPos.ZERO)
					&& !sections.contains(rail.signal()) && server.getBlockEntity(rail.signal()) instanceof SignalBlockEntity signal) {
				String holder = signal.occupant();
				if (holder != null && !holder.isEmpty() && !holder.equals(getUUID().toString())) {
					return direction > 0 ? start - end : end - stop;
				}
			}
		}
		return -1;
	}

	/** Enters the signal sections its cells reach, and leaves the ones it has left. */
	private void enterSections(ServerLevel server) {
		Set<BlockPos> now = new HashSet<>();
		for (Track.Step step : covered()) {
			if (server.getBlockEntity(RailView.pos(step.cell())) instanceof RailBlockEntity rail && !rail.signal().equals(BlockPos.ZERO)) {
				now.add(rail.signal());
			}
		}
		for (BlockPos at : now) {
			if (!sections.contains(at) && server.getBlockEntity(at) instanceof SignalBlockEntity signal) {
				signal.enter(getUUID().toString());
			}
		}
		for (BlockPos at : sections) {
			if (!now.contains(at) && server.getBlockEntity(at) instanceof SignalBlockEntity signal) {
				signal.leave(getUUID().toString());
			}
		}
		sections.clear();
		sections.addAll(now);
	}

	/** The steps the train stands on. */
	private List<Track.Step> covered() {
		List<Track.Step> out = new ArrayList<>();
		int from = path.index(Math.max(0, tailEnd())), to = path.index(Math.min(path.length(), frontEnd()));
		for (int i = from; i <= to; i++) {
			out.add(path.steps().get(i));
		}
		return out;
	}

	/** The distance to the first cell ahead another train holds, or -1. */
	private double otherTrainAhead(ServerLevel server) {
		Map<Long, Claim> claims = CLAIMS.get(server);
		if (claims == null) {
			return -1;
		}
		double end = direction > 0 ? frontEnd() : tailEnd();
		List<Track.Step> steps = path.steps();
		double best = -1;
		for (int i = 0; i < steps.size(); i++) {
			double start = path.start(i), stop = start + steps.get(i).length();
			boolean beyond = direction > 0 ? start >= end - 1e-6 : stop <= end + 1e-6;
			Claim claim = claims.get(RailView.pos(steps.get(i).cell()).asLong());
			if (beyond && claim != null && !claim.train().equals(getUUID()) && server.getGameTime() - claim.tick() <= 2) {
				double d = direction > 0 ? start - end : end - stop;
				best = best < 0 ? d : Math.min(best, d);
			}
		}
		return best;
	}

	private static boolean claimed(ServerLevel level, BlockPos pos, @Nullable UUID except) {
		Map<Long, Claim> claims = CLAIMS.get(level);
		Claim claim = claims == null ? null : claims.get(pos.asLong());
		return claim != null && !claim.train().equals(except) && level.getGameTime() - claim.tick() <= 2;
	}

	private void claim(ServerLevel server, List<MineVehicleEntity> train) {
		Map<Long, Claim> claims = CLAIMS.computeIfAbsent(server, l -> new HashMap<>());
		claims.values().removeIf(c -> c.train().equals(getUUID()) || server.getGameTime() - c.tick() > 2);
		for (Track.Step step : covered()) {
			claims.put(RailView.pos(step.cell()).asLong(), new Claim(getUUID(), server.getGameTime()));
		}
	}

	/** Sets the locomotive and every car at its place on the path. */
	private void place() {
		if (level() instanceof ServerLevel server) {
			place(members(server));
		}
	}

	private void place(List<MineVehicleEntity> train) {
		for (int i = 0; i < train.size(); i++) {
			double s = i == 0 ? front : carPlace(i - 1);
			double[] at = path.point(s);
			float[] attitude = path.attitude(s);
			train.get(i).setAt(at[0], at[1], at[2], attitude[0], attitude[1]);
		}
	}

	private void setAt(double x, double y, double z, float yaw, float pitch) {
		setPos(x, y, z);
		setYRot(yaw);
		setXRot(pitch);
	}

	/** The track went from under the train: it stops, and the cars at the gap drop their loads. */
	private void derail(ServerLevel server, List<MineVehicleEntity> train, double from, double to) {
		derailed = true;
		speed = 0;
		status = "DERAILED: put the track back under it";
		for (int i = 1; i < train.size(); i++) {
			double s = carPlace(i - 1);
			if (s + Haul.HALF_LENGTH >= from - Haul.SPACING && s - Haul.HALF_LENGTH <= to + Haul.SPACING) {
				train.get(i).dropLoad(server);
			}
		}
	}

	private void dropLoad(ServerLevel server) {
		for (Map.Entry<String, Integer> e : load.contents().entrySet()) {
			for (int left = e.getValue(); left > 0; left -= 64) {
				spawnAtLocation(server, Feeds.stack(e.getKey(), Math.min(64, left)));
			}
		}
		load.clear();
		refreshLoad();
	}

	/** A car over a dump pocket beside a ramp tips on the move (the drill loads the car standing at its loading point itself). */
	private void tipAndLoad(ServerLevel server, MineVehicleEntity car) {
		int index = cars.indexOf(car.getUUID());
		BlockPos rail = RailView.pos(path.stepAt(carPlace(index)).cell());
		DumpBlockEntity pocket = DumpBlockEntity.under(server, rail);
		if (pocket != null && car.load.total() > 0 && pocket.offer(car.load.contents())) {
			car.load.clear();
			car.refreshLoad();
		}
	}

	private void strike(ServerLevel server, List<MineVehicleEntity> train) {
		float amount = TrainHarm.damageFor(server.getServer(), Platform.INSTANCE.configDir().resolve("tailings-train.properties"));
		if (amount <= 0) {
			return;
		}
		for (MineVehicleEntity v : train) {
			AABB box = v.getBoundingBox().inflate(0.1);
			for (LivingEntity living : server.getEntities(EntityTypeTest.forClass(LivingEntity.class), box, e -> true)) {
				living.hurtServer(server, server.damageSources().generic(), amount);
			}
		}
	}

	/** Moves the light block to the air above the locomotive, only when it has moved a block. */
	private void headlight(ServerLevel server) {
		BlockPos above = blockPosition().above();
		if (above.equals(lamp) && server.getBlockState(lamp).getBlock() == Blocks.LIGHT) {
			return;
		}
		clearLamp(server);
		if (server.getBlockState(above).isAir()) {
			server.setBlock(above, Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15), 3);
			lamp = above;
		}
	}

	private void clearLamp(ServerLevel server) {
		if (lamp != null && server.getBlockState(lamp).getBlock() == Blocks.LIGHT) {
			server.removeBlock(lamp, false);
		}
		lamp = null;
	}

	// ---- hands -----------------------------------------------------------------------------------------------------------------

	/** The locomotive: a click starts or holds it, a sneaking click reverses it. A car: a sneaking click couples it. */
	@Override
	public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {
		if (level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (locomotive()) {
			if (player.isShiftKeyDown()) {
				direction = -direction;
				reachedAhead = -1;
				endWait = 0;
				player.sendOverlayMessage(Component.literal("Locomotive: reversed, " + (direction > 0 ? "locomotive first" : "backing up")));
			} else {
				running = !running;
				player.sendOverlayMessage(Component.literal("Locomotive: " + (running ? "running" : "held") + "; " + status));
			}
			return InteractionResult.SUCCESS;
		}
		if (player.isShiftKeyDown() && player.getItemInHand(hand).isEmpty()) {
			boolean joined = coupleToNearest();
			player.sendOverlayMessage(Component.literal(joined ? "Car: coupled" : locomotiveId != null ? "Car: already coupled" : "Car: no train within reach"));
			return InteractionResult.SUCCESS;
		}
		player.sendOverlayMessage(Component.literal("Car: " + load.total() + " of " + Haul.CAR_ITEMS + " items"));
		return InteractionResult.SUCCESS;
	}

	@Override
	public boolean isPickable() {
		return !isRemoved();
	}

	/** Only a sneaking hit with an empty hand breaks a vehicle, into its item and its load. */
	@Override
	public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
		if (source.getEntity() instanceof Player player && player.isShiftKeyDown() && player.getMainHandItem().isEmpty()) {
			dropLoad(level);
			spawnAtLocation(level, new ItemStack(locomotive() ? TailingsItems.LOCOMOTIVE : TailingsItems.CAR));
			discard();
			return true;
		}
		return false;
	}

	@Override
	public void remove(RemovalReason reason) {
		if (level() instanceof ServerLevel server) {
			clearLamp(server);
			if (locomotive()) {
				for (BlockPos at : sections) {
					if (server.getBlockEntity(at) instanceof SignalBlockEntity signal) {
						signal.leave(getUUID().toString());
					}
				}
				Map<Long, Claim> claims = CLAIMS.get(server);
				if (claims != null) {
					claims.values().removeIf(c -> c.train().equals(getUUID()));
				}
			}
		}
		super.remove(reason);
	}
}
