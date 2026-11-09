package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.block.FuelBayBlockEntity;
import dev.wildcatter.tailings.block.HaulDispatchBlockEntity;
import dev.wildcatter.tailings.block.PlantMachineBlock;
import dev.wildcatter.tailings.block.SurveyStakeBlock;
import dev.wildcatter.tailings.item.SurveyBookItem;
import dev.wildcatter.tailings.pit.Autopilot;
import dev.wildcatter.tailings.pit.PitFuel;
import dev.wildcatter.tailings.pit.StopKind;
import dev.wildcatter.tailings.pit.Survey;
import dev.wildcatter.tailings.plant.Machine;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A haul truck's surveyed route and where it is on it (docs/multiblocks/open-pit.md, P2a). The route runs out and back between its end
 * stakes; the truck backs into each end from the stake before it, fills or tips there, and drives back. It keeps right on two-way
 * legs, waits at a one-way leg for an oncoming truck, keeps 50 m from a truck ahead, stops for anything in its way, and stops at a
 * FUEL stake when it is low.
 */
final class TruckRoute {
	enum Phase {
		DRIVE, TURNING, BACKING, FILLING, TIPPING, FUELING, WAITING, HALTED, PARKED,
		/** P4: driving through a load-out to its gate, out the far side, and round a balloon loop back onto the line. */
		THROUGH, EXITING, LOOP_OUT, LOOP_BACK,
		/** P4: stopped before a DUMP POINT whose crest has no berm. */
		NO_BERM
	}

	/** Through a load-out it keeps to 15 km/h (proposed), and drives out 15 m past the gate before it loops round. */
	static final double THROUGH_SPEED = 15 / 3.6;
	static final double EXIT_PAST = 15;

	/** Trucks on one-way legs: leg key to truck to (way along the leg, game time seen). */
	private static final Map<ServerLevel, Map<String, Map<UUID, long[]>>> ONE_WAY = new WeakHashMap<>();
	/** Leaves a part-filled truck's LOAD stop after 60 s with nothing more loaded (approved). */
	static final int FILL_PATIENCE = 60 * 20;
	/** Waits at a fuel bay that gives nothing for 2 minutes, then goes on. */
	static final int FUEL_PATIENCE = 120 * 20;
	/** Stopped for an obstacle longer than 30 s, it reads BLOCKED (spec). */
	static final int BLOCKED_TICKS = 30 * 20;
	/** Turning round at a turn stake: aligned within 15 degrees, at 2 m/s, for at most a minute. */
	static final double ALIGNED_DEGREES = 15;
	static final double TURNING_SPEED = 2.0;
	static final int TURNING_TICKS = 60 * 20;
	/** At a DUMP POCKET it backs up to this far past the stake, until the bumper stops it. */
	static final double BUMPER_SEARCH = 3;
	/** Backing in at an angle, a truck whose tail passes the end within this of the line stops there (P2b: it circled before). */
	static final double PAST_END_ACROSS = 4;
	/** A truck waits this far short of a one-way leg while a truck comes along it the other way (proposed). */
	static final double ONE_WAY_HOLD = 40;

	final List<BlockPos> stakes = new ArrayList<>();
	/** The stake it is heading for, and which way along the list it goes. */
	int at;
	int way = 1;
	Phase phase = Phase.DRIVE;
	private final List<Survey.Leg> legs = new ArrayList<>();
	private int waited;
	private int stalled;
	private long lastLoad = -1;
	private double lastFuel;
	String problem;
	/** Dispatch (open-pit.md, P2b): its route's office, if one has it, as last looked up; whether it is to run, and why not. */
	private HaulDispatchBlockEntity.@Nullable Link dispatch;
	private boolean run = true;
	private String parkWhy = "";
	private long nextLook;

	boolean active() {
		return !stakes.isEmpty();
	}

	/** Parked by dispatch: its engine is off. */
	boolean parked() {
		return phase == Phase.PARKED;
	}

	void clear() {
		stakes.clear();
		legs.clear();
		phase = Phase.DRIVE;
		problem = null;
		bermLook = null;
		noBermFor = 0;
		dispatch = null;
		run = true;
		nextLook = 0;
	}

	StopKind kind(ServerLevel level, int index) {
		return SurveyStakeBlock.kind(level.getBlockState(stakes.get(index)));
	}

	private boolean end(int index) {
		return index == 0 || index == stakes.size() - 1;
	}

	/** Takes a route from the survey book; why not, or null. */
	@Nullable String set(ServerLevel level, HaulTruckEntity truck, List<BlockPos> route) {
		if (route.size() < 3) {
			return "a route needs at least 3 stakes: the truck backs into each end from the stake before it";
		}
		for (BlockPos pos : route) {
			if (!(level.getBlockState(pos).getBlock() instanceof SurveyStakeBlock)) {
				return "no survey stake at " + pos.toShortString();
			}
		}
		StopKind first = SurveyStakeBlock.kind(level.getBlockState(route.get(0)));
		StopKind last = SurveyStakeBlock.kind(level.getBlockState(route.get(route.size() - 1)));
		if (!((first.fills() && last.tips()) || (first.tips() && last.fills()))) {
			return "a route's ends must be a LOAD stake and a DUMP POCKET or DUMP POINT stake (it has " + first.label() + " and " + last.label() + ")";
		}
		clear();
		stakes.addAll(route);
		for (int i = 0; i + 1 < route.size(); i++) {
			legs.add(SurveyBookItem.survey(level, route.get(i), route.get(i + 1)));
		}
		ends(level);
		// start at the nearest stake, heading for the end it wants: the dump end loaded, the load end empty
		int nearest = 0;
		double best = Double.MAX_VALUE;
		for (int i = 0; i < stakes.size(); i++) {
			double d = flat(truck.position(), centre(stakes.get(i)));
			if (d < best) {
				best = d;
				nearest = i;
			}
		}
		boolean wantDump = truck.load().total() > 0;
		int target = wantDump == first.tips() ? 0 : stakes.size() - 1;
		way = target == 0 ? -1 : 1;
		at = end(nearest) && nearest != target ? nearest + way : nearest;
		phase = Phase.DRIVE;
		problem = null;
		return null;
	}

	private static Vec3 centre(BlockPos pos) {
		return new Vec3(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
	}

	private static double flat(Vec3 a, Vec3 b) {
		return Math.hypot(a.x - b.x, a.z - b.z);
	}

	/** The leg between stakes i and i + 1. */
	private Survey.Leg leg(int i) {
		return legs.get(Math.max(0, Math.min(legs.size() - 1, i)));
	}

	/** The leg it is on, heading for {@code at} going {@code way}. */
	private int currentLeg() {
		return way > 0 ? at - 1 : at;
	}

	private String legKey(int leg) {
		BlockPos a = stakes.get(leg), b = stakes.get(leg + 1);
		return a.asLong() + ">" + b.asLong();
	}

	/** Where it steers for the stake it is heading to: on a two-way leg, 4 m right of the line. */
	private Vec3 aim(boolean reverse) {
		Vec3 target = centre(stakes.get(at));
		int from = at - way;
		if (reverse || from < 0 || from >= stakes.size() || leg(currentLeg()).oneWay() || singleFile(currentLeg())) {
			return target;
		}
		Vec3 start = centre(stakes.get(from));
		double dx = target.x - start.x, dz = target.z - start.z, run = Math.hypot(dx, dz);
		if (run < 1e-6) {
			return target;
		}
		// its right, going from start to target: yaw from the heading, right() = (-cos, -sin)
		double yaw = Math.toRadians(Autopilot.yawToward(start.x, start.z, target.x, target.z));
		return target.add(-Math.cos(yaw) * Autopilot.LANE_OFFSET, 0, -Math.sin(yaw) * Autopilot.LANE_OFFSET);
	}

	/** Load-out ends: the leg into a load-out is single file, on the line through its lane, both ways (P4). */
	private boolean firstLoadout, lastLoadout;

	private boolean singleFile(int leg) {
		return (leg == 0 && firstLoadout) || (leg == legs.size() - 1 && lastLoadout);
	}

	private void ends(ServerLevel level) {
		firstLoadout = kind(level, 0) == StopKind.LOADOUT;
		lastLoadout = kind(level, stakes.size() - 1) == StopKind.LOADOUT;
	}

	/** Steering looks this far ahead along its lane (proposed), so it is in its lane soon after a junction. */
	static final double LOOK_AHEAD = 15;

	/** The point it steers for: on its lane, 15 m on from where it is, short of the stake's lane point. */
	private Vec3 lookAhead(HaulTruckEntity truck, Vec3 laneEnd) {
		int from = at - way;
		if (from < 0 || from >= stakes.size()) {
			return laneEnd;
		}
		Vec3 offset = laneEnd.subtract(centre(stakes.get(at)));
		Vec3 start = centre(stakes.get(from)).add(offset);
		double dx = laneEnd.x - start.x, dz = laneEnd.z - start.z, run = Math.hypot(dx, dz);
		if (run < 1e-6) {
			return laneEnd;
		}
		double ux = dx / run, uz = dz / run;
		double t = (truck.getX() - start.x) * ux + (truck.getZ() - start.z) * uz;
		double ahead = Math.max(0, Math.min(run, t + LOOK_AHEAD));
		return new Vec3(start.x + ux * ahead, laneEnd.y, start.z + uz * ahead);
	}

	/** Its route's PARK stake, or -1. */
	private int parkStake(ServerLevel level) {
		for (int i = 0; i < stakes.size(); i++) {
			if (kind(level, i) == StopKind.PARK) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * Once a second it finds its route's dispatch office and reports to it (P2b): whether it works, is low on fuel or blocked. A
	 * route no office has runs as in P2a. An office whose radio does not reach it (no power, over 2 km) stops it at its next stake.
	 */
	private void listen(ServerLevel level, HaulTruckEntity truck) {
		long now = level.getGameTime();
		if (now < nextLook) {
			return;
		}
		nextLook = now + 20;
		dispatch = HaulDispatchBlockEntity.find(level, stakes, truck.position());
		if (dispatch == null) {
			run = true;
			return;
		}
		if (!dispatch.contact()) {
			run = false;
			parkWhy = dispatch.noContact();
			return;
		}
		HaulDispatchBlockEntity office = dispatch.office();
		int route = dispatch.route();
		run = office.runs(route);
		parkWhy = run ? "" : office.parkedWhy(route);
		boolean working = run && phase != Phase.PARKED && phase != Phase.HALTED && !truck.outOfFuel();
		office.report(route, truck.getUUID(), working, truck.fuel() < PitFuel.LOW * truck.tank(), blocked());
	}

	/** It tipped a load: its office counts it. */
	void tipped(ServerLevel level, HaulTruckEntity truck, long kg) {
		nextLook = 0;
		listen(level, truck);
		if (dispatch != null && dispatch.contact()) {
			dispatch.office().tipped(dispatch.route(), kg);
		}
	}

	/** Whether it is to stop at the stake it is heading for: told to stop, at the route's PARK stake, or the next stake if it has none or no radio. */
	private boolean parkAt(ServerLevel level) {
		if (run) {
			return false;
		}
		int park = dispatch != null && dispatch.contact() ? parkStake(level) : -1;
		return park < 0 || park == at;
	}

	/** This tick's controls. */
	PitVehicleEntity.Drive drive(ServerLevel level, HaulTruckEntity truck) {
		listen(level, truck);
		if (phase == Phase.PARKED) {
			if (run) {
				phase = Phase.DRIVE;   // back to work: on from the stake it parked at
			} else {
				truck.setStatus("parked at stake " + (at + 1) + " (" + kind(level, at).label() + "): " + parkWhy);
				return PitVehicleEntity.Drive.STAND;
			}
		}
		if (truck.outOfFuel()) {
			truck.setStatus("out of fuel");
			return PitVehicleEntity.Drive.STAND;
		}
		switch (phase) {
			case HALTED -> {
				truck.setStatus(problem);
				return PitVehicleEntity.Drive.STAND;
			}
			case FILLING -> {
				return fill(level, truck);
			}
			case TIPPING -> {
				return tip(level, truck);
			}
			case FUELING -> {
				return fuel(level, truck);
			}
			case WAITING -> {
				return waitOneWay(level, truck);
			}
			case TURNING -> {
				return turn(level, truck);
			}
			case BACKING -> {
				return back(level, truck);
			}
			case THROUGH -> {
				return through(level, truck);
			}
			case EXITING -> {
				return exit(level, truck);
			}
			case LOOP_OUT, LOOP_BACK -> {
				return loop(level, truck);
			}
			case NO_BERM -> {
				return noBerm(level, truck);
			}
			default -> {
				return forward(level, truck);
			}
		}
	}

	private PitVehicleEntity.Drive forward(ServerLevel level, HaulTruckEntity truck) {
		if (end(at) && kind(level, at) == StopKind.LOADOUT) {
			phase = Phase.THROUGH;   // a load-out is driven through forward (P4-3)
			return through(level, truck);
		}
		if (end(at)) {
			phase = Phase.TURNING;   // given its route beside an end: turn round and back into it
			waited = 0;
			return PitVehicleEntity.Drive.STAND;
		}
		int leg = currentLeg();
		String legProblem = leg(leg).problem();
		if (legProblem != null) {
			phase = Phase.HALTED;
			problem = "leg " + (leg + 1) + ": " + legProblem;
			truck.setStatus(problem);
			return PitVehicleEntity.Drive.STAND;
		}
		if (leg(leg).oneWay()) {
			mark(level, truck, leg);
		}
		Vec3 aim = aim(false);
		double distance = flat(truck.position(), aim);
		int next = at + way;
		boolean turnBeforeEnd = end(next) && kind(level, next) != StopKind.LOADOUT;
		boolean fuelStop = kind(level, at) == StopKind.FUEL && truck.fuel() < PitFuel.LOW * truck.tank();
		boolean parkStop = parkAt(level);
		boolean stopHere = turnBeforeEnd || fuelStop || parkStop;
		double target = truck.spec().topSpeed();
		if (stopHere) {
			target = Math.min(target, Autopilot.stoppingSpeed(distance - 0.5));
		} else if (turnAngle(at) > Autopilot.SHARP_TURN_DEGREES) {
			target = Math.min(target, Math.sqrt(Autopilot.TURN_SPEED * Autopilot.TURN_SPEED + 2 * Autopilot.PLAN_DECEL * Math.max(0, distance - 3)));
		}
		// a one-way leg next, with a truck coming along it: wait 40 m short, clear of the junction
		int nextLeg = way > 0 ? at : at - 1;
		boolean hold = !end(at) && nextLeg >= 0 && nextLeg < legs.size() && leg(nextLeg).oneWay() && oncoming(level, truck, nextLeg);
		if (hold) {
			target = Math.min(target, Autopilot.stoppingSpeed(distance - ONE_WAY_HOLD));
		}
		Vec3 steerFor = lookAhead(truck, aim);
		// obstacles only as far as the stake: past it, the next leg's way is looked along when it gets there
		target = Math.min(target, clearAhead(level, truck, false, distance + 5, steerFor));
		if (hold && distance < ONE_WAY_HOLD + 6) {
			truck.setStatus("one-way: waiting for a truck coming the other way");
			return new PitVehicleEntity.Drive(Autopilot.throttle(truck.speed(), 0, 1), 0, false);
		}
		// passed it: the stake is behind it and close beside, which a truck that cannot turn tight enough to reach it does
		Vec3 to = aim.subtract(truck.position());
		Vec3 nose = truck.local(0, 0, 1).subtract(truck.position());
		boolean behind = to.x * nose.x + to.z * nose.z < 0 && distance < (stopHere ? 8 : truck.spec().turnRadius() + 2);
		boolean arrived = stopHere ? (distance < 2.5 || behind) && Math.abs(truck.speed()) < 0.3 : distance < 4 || behind;
		if (stopHere && behind && Math.abs(truck.speed()) >= 0.3) {
			return new PitVehicleEntity.Drive(Autopilot.throttle(truck.speed(), 0, 1), 0, false);
		}
		if (arrived) {
			if (parkStop) {
				phase = Phase.PARKED;   // it stays heading for this stake, so it goes on from here
				blockedFor = 0;
				truck.setStatus("parked at stake " + (at + 1) + " (" + kind(level, at).label() + "): " + parkWhy);
				return PitVehicleEntity.Drive.STAND;
			}
			return arrive(level, truck, fuelStop, turnBeforeEnd);
		}
		if (!run) {
			truck.setStatus((parkStop ? "to park at stake " : "to stake ") + (at + 1) + ", then PARK: " + parkWhy);
		} else {
			truck.setStatus(truck.load().total() > 0 ? "hauling to stake " + (at + 1) : "to stake " + (at + 1) + " empty");
		}
		double steer = Autopilot.steer(truck.getYRot(), truck.getX(), truck.getZ(), steerFor.x, steerFor.z, false, truck.speed(), truck.spec().turnRadius());
		return new PitVehicleEntity.Drive(Autopilot.throttle(truck.speed(), target, 1), steer, false);
	}

	private PitVehicleEntity.Drive arrive(ServerLevel level, HaulTruckEntity truck, boolean fuelStop, boolean turnBeforeEnd) {
		if (fuelStop) {
			phase = Phase.FUELING;
			waited = 0;
			lastFuel = truck.fuel();
			return PitVehicleEntity.Drive.STAND;
		}
		at += way;
		if (turnBeforeEnd) {
			phase = Phase.TURNING;
			waited = 0;
			if (kind(level, at) == StopKind.DUMP_POINT && look(level, truck).kind() == dev.wildcatter.tailings.pit.Crest.Kind.NO_BERM) {
				phase = Phase.NO_BERM;   // it will not back to a crest with no berm (P4-4)
			}
			return PitVehicleEntity.Drive.STAND;
		}
		return PitVehicleEntity.Drive.STAND;
	}

	/** The unit way from the turn stake to the end stake it backs into. */
	private Vec3 inward() {
		Vec3 stake = centre(stakes.get(at));
		int from = at - way;
		if (from < 0 || from >= stakes.size()) {
			return new Vec3(0, 0, 1);
		}
		Vec3 start = centre(stakes.get(from));
		Vec3 along = new Vec3(stake.x - start.x, 0, stake.z - start.z);
		return along.length() < 1e-6 ? new Vec3(0, 0, 1) : along.normalize();
	}

	/** What is behind the DUMP POINT it is heading for: a paddock, a bermed crest, or a crest with no berm. */
	dev.wildcatter.tailings.pit.Crest.Look look(ServerLevel level, HaulTruckEntity truck) {
		BlockPos stake = stakes.get(at);
		Vec3 in = inward();
		return dev.wildcatter.tailings.pit.Crest.look(HaulTruckEntity.crestGround(level), stake.getX() + 0.5, stake.getY(), stake.getZ() + 0.5, in.x, in.z,
				truck.spec().width(), dev.wildcatter.tailings.pit.DryStack.bermBlocks(dev.wildcatter.tailings.pit.DryStack.TRUCK_WHEEL_RADIUS));
	}

	private int noBermFor;

	/** Waits at the stake before a DUMP POINT with no berm, reading NO BERM, BLOCKED in dispatch after 30 s; goes on once a berm is up. */
	private PitVehicleEntity.Drive noBerm(ServerLevel level, HaulTruckEntity truck) {
		if (++noBermFor % 20 == 0 && look(level, truck).kind() != dev.wildcatter.tailings.pit.Crest.Kind.NO_BERM) {
			phase = Phase.TURNING;
			noBermFor = 0;
			waited = 0;
			return PitVehicleEntity.Drive.STAND;
		}
		truck.setStatus("NO BERM at the DUMP POINT (stake " + (at + 1) + "): push one up along the crest");
		return new PitVehicleEntity.Drive(Autopilot.throttle(truck.speed(), 0, 1), 0, false);
	}

	/**
	 * The load-out's gate: the stake stands at the mouth of its lane (the frame's cells fill the lane), so the gate is that of the
	 * load-out frame within 16 m on along the line; with none, the stake itself.
	 */
	private Vec3 gate(ServerLevel level) {
		BlockPos stake = stakes.get(at);
		Vec3 in = inward();
		for (int k = 0; k <= 16; k++) {
			for (int up = 0; up <= 1; up++) {
				BlockPos pos = BlockPos.containing(stake.getX() + 0.5 + in.x * k, stake.getY() + up, stake.getZ() + 0.5 + in.z * k);
				net.minecraft.world.level.block.state.BlockState state = level.getBlockState(pos);
				if (state.getBlock() instanceof PlantMachineBlock block && block.machine() == Machine.LOADOUT_FRAME
						&& level.getBlockEntity(PlantMachineBlock.mainOf(pos, state)) instanceof dev.wildcatter.tailings.block.PlantBlockEntity frame) {
					Vec3 g = frame.loadoutGate();
					// on the line, abreast of the gate
					Vec3 s0 = centre(stake);
					double t = (g.x - s0.x) * in.x + (g.z - s0.z) * in.z;
					return s0.add(in.scale(t));
				}
			}
		}
		return centre(stake);
	}

	/** Drives forward along the line through the load-out's lane and stops with its body under the gate. */
	private PitVehicleEntity.Drive through(ServerLevel level, HaulTruckEntity truck) {
		Vec3 stake = gate(level);
		Vec3 in = inward();
		Vec3 body = truck.bodyCentre();
		double toGo = (stake.x - body.x) * in.x + (stake.z - body.z) * in.z;
		if (toGo < 0.4 && Math.abs(truck.speed()) < 0.1) {
			phase = Phase.FILLING;
			lastLoad = truck.load().total();
			waited = 0;
			return PitVehicleEntity.Drive.STAND;
		}
		double target = toGo < 0.4 ? 0 : Math.min(THROUGH_SPEED, Math.max(0.8, Autopilot.stoppingSpeed(toGo - 0.5)));
		target = Math.min(target, clearAhead(level, truck, false, Math.max(0, toGo) + 5, null));
		truck.setStatus("driving through the load-out to its gate");
		double t = (truck.getX() - stake.x) * in.x + (truck.getZ() - stake.z) * in.z;
		Vec3 aim = stake.add(in.scale(t + 8));   // a point on the line 8 m ahead of it, so it runs straight through the lane
		double steer = Autopilot.steer(truck.getYRot(), truck.getX(), truck.getZ(), aim.x, aim.z, false, truck.speed(), truck.spec().turnRadius());
		return new PitVehicleEntity.Drive(Autopilot.throttle(truck.speed(), target, 1), steer, false);
	}

	/** Filled at the load-out, it drives on out of the far side of the lane. */
	private PitVehicleEntity.Drive exit(ServerLevel level, HaulTruckEntity truck) {
		Vec3 stake = gate(level);
		Vec3 in = inward();
		double past = (truck.getX() - stake.x) * in.x + (truck.getZ() - stake.z) * in.z;
		if (past >= EXIT_PAST) {
			phase = Phase.LOOP_OUT;
			loopFrom = Autopilot.yawToward(0, 0, in.x, in.z);
			return new PitVehicleEntity.Drive(Autopilot.throttle(truck.speed(), TURNING_SPEED, 1), 0, false);
		}
		truck.setStatus("loaded: out of the load-out");
		Vec3 aim = stake.add(in.scale(past + 8));
		double target = Math.min(THROUGH_SPEED, clearAhead(level, truck, false, 12, null));
		double steer = Autopilot.steer(truck.getYRot(), truck.getX(), truck.getZ(), aim.x, aim.z, false, truck.speed(), truck.spec().turnRadius());
		return new PitVehicleEntity.Drive(Autopilot.throttle(truck.speed(), target, 1), steer, false);
	}

	private double loopFrom;

	/**
	 * The balloon loop past a load-out (proposed): a quarter turn one way, then three quarters the other at full lock, which brings it
	 * back onto the line it came out on, heading back, about 25 m further out.
	 */
	private PitVehicleEntity.Drive loop(ServerLevel level, HaulTruckEntity truck) {
		double turned = Autopilot.wrap(truck.getYRot() - loopFrom);
		double target = Math.min(TURNING_SPEED, clearAhead(level, truck, false, 12, null));
		truck.setStatus("looping round at the load-out");
		if (++waited > TURNING_TICKS * 3) {
			phase = Phase.HALTED;
			problem = "can't loop round past the load-out: give it 40 m of open ground beyond the gate";
			return PitVehicleEntity.Drive.STAND;
		}
		if (phase == Phase.LOOP_OUT) {
			// yaw rate = -steer · v / R: steer -1 turns its yaw up
			if (turned >= 90) {
				phase = Phase.LOOP_BACK;
			}
			return new PitVehicleEntity.Drive(Autopilot.throttle(truck.speed(), target, 1), -1, false);
		}
		// back round the other way until it heads back along the line
		if (Math.abs(Autopilot.wrap(turned - 180)) < 4) {
			waited = 0;
			leaveEnd();
			return PitVehicleEntity.Drive.STAND;
		}
		return new PitVehicleEntity.Drive(Autopilot.throttle(truck.speed(), target, 1), 1, false);
	}

	/**
	 * Turns round in the open ground at the turn stake until its nose points away from the end stake, as a truck swings round on a
	 * dump's turning area before it backs up; then stops, to back in.
	 */
	private PitVehicleEntity.Drive turn(ServerLevel level, HaulTruckEntity truck) {
		Vec3 end = centre(stakes.get(at));
		double want = Autopilot.yawToward(end.x, end.z, truck.getX(), truck.getZ());
		double error = Autopilot.wrap(want - truck.getYRot());
		if (Math.abs(error) < ALIGNED_DEGREES) {
			if (Math.abs(truck.speed()) < 0.05) {
				phase = Phase.BACKING;
				stalled = 0;
				return PitVehicleEntity.Drive.STAND;
			}
			return new PitVehicleEntity.Drive(Autopilot.throttle(truck.speed(), 0, 1), 0, false);
		}
		if (++waited > TURNING_TICKS) {
			phase = Phase.HALTED;
			problem = "can't turn round at stake " + (at - way + 1) + ": give it 25 m of open ground";
			return PitVehicleEntity.Drive.STAND;
		}
		truck.setStatus("turning round to back in to " + kind(level, at).label());
		double target = Math.min(TURNING_SPEED, clearAhead(level, truck, false, 12, null));
		// yaw rate = -steer · v / R, so a full lock the error's way
		return new PitVehicleEntity.Drive(Autopilot.throttle(truck.speed(), target, 1), -Math.signum(error), false);
	}

	/** Backs into the end stake: its tail to the stake, or until a bumper stops it. */
	private PitVehicleEntity.Drive back(ServerLevel level, HaulTruckEntity truck) {
		Vec3 stake = centre(stakes.get(at));
		Vec3 end = stake;
		Vec3 tail = truck.local(0, 0, -truck.spec().length() / 2);
		int turnStake = at - way;
		if (kind(level, at) == StopKind.DUMP_POINT) {
			if (bermLook == null) {
				bermLook = look(level, truck);
			}
			if (bermLook.kind() == dev.wildcatter.tailings.pit.Crest.Kind.BERM) {
				// its tail stops 1 m short of the berm, as at a bumper (P4-4)
				stake = stake.add(inward().scale(bermLook.bermAt() - dev.wildcatter.tailings.pit.DryStack.BERM_STOP));
				end = stake;
			}
		}
		if (kind(level, at) == StopKind.DUMP_POCKET && turnStake >= 0 && turnStake < stakes.size()) {
			// at a truck dump it backs on until the bumper stops it, up to 3 m past the stake
			Vec3 from = centre(stakes.get(turnStake));
			Vec3 along = new Vec3(stake.x - from.x, 0, stake.z - from.z);
			if (along.length() > 1e-6) {
				end = stake.add(along.normalize().scale(BUMPER_SEARCH));
			}
		}
		double distance = flat(tail, end);
		double fromStake = flat(tail, stake);
		// its tail past the end along the line, close beside it: it came in at an angle and missed the stake; it stops there
		boolean past = false;
		int fromIndex = at - way;
		if (fromIndex >= 0 && fromIndex < stakes.size()) {
			Vec3 start = centre(stakes.get(fromIndex));
			double dx = end.x - start.x, dz = end.z - start.z, run = Math.hypot(dx, dz);
			if (run > 1e-6) {
				double along = ((tail.x - start.x) * dx + (tail.z - start.z) * dz) / run;
				double across = Math.abs((tail.x - start.x) * dz - (tail.z - start.z) * dx) / run;
				past = along >= run - 0.3 && across < PAST_END_ACROSS;
			}
		}
		double target = past ? 0 : Math.min(truck.spec().reverseSpeed(), Autopilot.stoppingSpeed(distance - 0.3));
		target = Math.min(target, clearAhead(level, truck, true, distance + 2, null));
		boolean stopped = Math.abs(truck.speed()) < 0.05;
		stalled = stopped && target > 0.3 ? stalled + 1 : 0;
		if (stalled > BLOCKED_TICKS && fromStake >= 5) {
			phase = Phase.HALTED;
			problem = String.format(java.util.Locale.ROOT, "can't back in to %s: something in its way %.0f m short", kind(level, at).label(), distance);
			return PitVehicleEntity.Drive.STAND;
		}
		// at the stake, or stopped against a bumper close to it
		if ((distance < 1.5 && stopped) || (past && stopped) || (stalled > 40 && fromStake < 5)) {
			StopKind kind = kind(level, at);
			if (kind.fills()) {
				phase = Phase.FILLING;
				lastLoad = truck.load().total();
				waited = 0;
			} else {
				phase = Phase.TIPPING;
				waited = 0;
				return new PitVehicleEntity.Drive(0, 0, true);
			}
			return PitVehicleEntity.Drive.STAND;
		}
		truck.setStatus("backing in to " + kind(level, at).label());
		// along the line from the turn stake to the end, 6 m on from its tail, so it backs in straight
		Vec3 aim = end;
		int from = at - way;
		if (from >= 0 && from < stakes.size()) {
			Vec3 start = centre(stakes.get(from));
			double dx = end.x - start.x, dz = end.z - start.z, run = Math.hypot(dx, dz);
			if (run > 1e-6) {
				double ux = dx / run, uz = dz / run;
				double t = (tail.x - start.x) * ux + (tail.z - start.z) * uz;
				double on = Math.max(0, Math.min(run + 6, t + 6));
				aim = new Vec3(start.x + ux * on, end.y, start.z + uz * on);
			}
		}
		double steer = Autopilot.steer(truck.getYRot(), truck.getX(), truck.getZ(), aim.x, aim.z, true, truck.speed(), truck.spec().turnRadius());
		return new PitVehicleEntity.Drive(Autopilot.throttle(truck.speed(), target, -1), steer, false);
	}

	private PitVehicleEntity.Drive fill(ServerLevel level, HaulTruckEntity truck) {
		long now = truck.load().total();
		if (now != lastLoad) {
			lastLoad = now;
			waited = 0;
		}
		waited++;
		if (truck.load().room() <= 0 || (now > 0 && waited > FILL_PATIENCE) || !run) {
			if (kind(level, at) == StopKind.LOADOUT) {
				phase = Phase.EXITING;   // out of the far side, not back the way it came
				waited = 0;
				return PitVehicleEntity.Drive.STAND;
			}
			leaveEnd();
			return PitVehicleEntity.Drive.STAND;
		}
		truck.setStatus(now > 0 ? "loading: " + truck.load().tonnes() : "waiting to load");
		return PitVehicleEntity.Drive.STAND;
	}

	private PitVehicleEntity.Drive tip(ServerLevel level, HaulTruckEntity truck) {
		waited++;
		if (!truck.busy() && waited > 5) {
			String said = truck.status();
			if (said.contains("FULL")) {
				// the hopper is full: wait, and try again every 10 s (spec)
				return new PitVehicleEntity.Drive(0, 0, waited % 200 == 0);
			}
			if (said.startsWith("can't tip")) {
				phase = Phase.HALTED;
				problem = said;
				return PitVehicleEntity.Drive.STAND;
			}
			leaveEnd();
		}
		return PitVehicleEntity.Drive.STAND;
	}

	/** Off the end stop, back along the route. */
	private void leaveEnd() {
		way = -way;
		at += way;
		phase = Phase.DRIVE;
	}

	private PitVehicleEntity.Drive fuel(ServerLevel level, HaulTruckEntity truck) {
		waited++;
		if (truck.fuel() > lastFuel + 0.01) {
			waited = 0;
		}
		lastFuel = truck.fuel();
		if (truck.fuel() >= truck.tank() * 0.95 || waited > FUEL_PATIENCE) {
			if (waited > FUEL_PATIENCE) {
				truck.setStatus("no fuel at the bay");
			}
			phase = Phase.DRIVE;
			at += way;
			return PitVehicleEntity.Drive.STAND;
		}
		truck.setStatus(bayNear(level, truck) ? String.format(java.util.Locale.ROOT, "fuelling: %.0f of %.0f L", truck.fuel(), truck.tank()) : "at FUEL: no fuel bay in reach");
		return PitVehicleEntity.Drive.STAND;
	}

	private static boolean bayNear(ServerLevel level, HaulTruckEntity truck) {
		BlockPos centre = truck.blockPosition();
		for (BlockPos pos : BlockPos.betweenClosed(centre.offset(-14, -3, -14), centre.offset(14, 3, 14))) {
			if (level.getBlockState(pos).getBlock() instanceof PlantMachineBlock block && block.machine() == Machine.FUEL_BAY
					&& level.getBlockEntity(PlantMachineBlock.mainOf(pos, level.getBlockState(pos))) instanceof FuelBayBlockEntity bay && bay.reaches(truck)) {
				return true;
			}
		}
		return false;
	}

	private PitVehicleEntity.Drive waitOneWay(ServerLevel level, HaulTruckEntity truck) {
		if (oncoming(level, truck, currentLeg())) {
			truck.setStatus("one-way: waiting for a truck coming the other way");
			return PitVehicleEntity.Drive.STAND;
		}
		phase = Phase.DRIVE;
		return PitVehicleEntity.Drive.STAND;
	}

	private void mark(ServerLevel level, HaulTruckEntity truck, int leg) {
		ONE_WAY.computeIfAbsent(level, l -> new HashMap<>()).computeIfAbsent(legKey(leg), k -> new HashMap<>())
				.put(truck.getUUID(), new long[] {way, level.getGameTime()});
	}

	/** A truck on the leg, seen in the last 2 s, going the other way. */
	private boolean oncoming(ServerLevel level, HaulTruckEntity truck, int leg) {
		Map<UUID, long[]> on = ONE_WAY.getOrDefault(level, Map.of()).get(legKey(leg));
		if (on == null) {
			return false;
		}
		long now = level.getGameTime();
		on.values().removeIf(seen -> now - seen[1] > 40);
		for (Map.Entry<UUID, long[]> e : on.entrySet()) {
			if (!e.getKey().equals(truck.getUUID()) && e.getValue()[0] != way) {
				return true;
			}
		}
		return false;
	}

	/** The turn at stake i between the legs either side, in degrees. */
	private double turnAngle(int i) {
		if (i <= 0 || i >= stakes.size() - 1) {
			return 0;
		}
		Vec3 a = centre(stakes.get(i - 1)), b = centre(stakes.get(i)), c = centre(stakes.get(i + 1));
		double in = Autopilot.yawToward(a.x, a.z, b.x, b.z), out = Autopilot.yawToward(b.x, b.z, c.x, c.z);
		return Math.abs(Autopilot.wrap(out - in));
	}

	/**
	 * The fastest it may go for what is ahead of it (behind it, reversing): it stops for any player, mob or machine within its
	 * stopping distance plus 5 m and its width, and keeps 50 m from a truck ahead in its lane.
	 */
	private double clearAhead(ServerLevel level, HaulTruckEntity truck, boolean reverse, double most, @Nullable Vec3 aim) {
		double v = Math.abs(truck.speed());
		double look = Math.min(most, Autopilot.stoppingDistance(v) + Autopilot.OBSTACLE_MARGIN);
		double half = truck.spec().length() / 2;
		double sign = reverse ? -1 : 1;
		double width = truck.spec().width();
		double limit = Double.MAX_VALUE;
		boolean obstacle = false;
		Vec3 fwd = truck.local(0, 0, 1).subtract(truck.position()).scale(sign);
		if (aim != null && flat(truck.position(), aim) > 1) {
			// along the path it is steering for, not straight off its nose
			Vec3 toward = aim.subtract(truck.position());
			fwd = new Vec3(toward.x, 0, toward.z).normalize();
		}
		Vec3 right = new Vec3(-fwd.z, 0, fwd.x);
		AABB search = truck.getBoundingBox().inflate(Autopilot.FOLLOW_GAP + 10, 4, Autopilot.FOLLOW_GAP + 10);
		for (Entity e : level.getEntities(truck, search, e -> e instanceof LivingEntity || e instanceof PitVehicleEntity || e instanceof MineVehicleEntity)) {
			if (truck.hasPassenger(e) || e.isSpectator()) {
				continue;
			}
			Vec3 d = e.position().subtract(truck.position());
			double along = d.x * fwd.x + d.z * fwd.z - half;
			double across = Math.abs(d.x * right.x + d.z * right.z);
			double theirHalfWidth = e instanceof PitVehicleEntity p ? p.spec().width() / 2 : e.getBbWidth() / 2;
			double theirHalfLength = e instanceof PitVehicleEntity p ? p.spec().length() / 2 : e.getBbWidth() / 2;
			along -= theirHalfLength;
			if (across > width / 2 + theirHalfWidth || along < -1) {
				continue;
			}
			if (along <= look) {
				obstacle = true;
			}
			boolean sameWay = e instanceof PitVehicleEntity p && (p.local(0, 0, 1).subtract(p.position()).dot(fwd) > 0.5);
			if (!reverse && e instanceof HaulTruckEntity && sameWay && along < Autopilot.FOLLOW_GAP + 30) {
				limit = Math.min(limit, Autopilot.stoppingSpeed(along - Autopilot.FOLLOW_GAP));
			}
		}
		if (obstacle) {
			stalledOn(truck);
			return 0;
		}
		blockedFor = 0;
		return limit;
	}

	private int blockedFor;
	private dev.wildcatter.tailings.pit.Crest.@Nullable Look bermLook;

	private void stalledOn(HaulTruckEntity truck) {
		blockedFor++;
		if (blockedFor > BLOCKED_TICKS) {
			truck.setStatus("BLOCKED: something in its way");
		}
	}

	boolean blocked() {
		return blockedFor > BLOCKED_TICKS || (phase == Phase.NO_BERM && noBermFor > BLOCKED_TICKS);
	}

	String describe(ServerLevel level) {
		if (!active()) {
			return "";
		}
		String office = dispatch == null ? "" : "; dispatch route " + (dispatch.route() + 1) + (dispatch.contact() ? (run ? ", RUN" : ", PARK") : ", no radio");
		return "route of " + stakes.size() + " stakes, to stake " + (at + 1) + " (" + kind(level, at).label() + ")" + office;
	}

	long[] save() {
		long[] out = new long[stakes.size()];
		for (int i = 0; i < out.length; i++) {
			out[i] = stakes.get(i).asLong();
		}
		return out;
	}

	void load(ServerLevel level, HaulTruckEntity truck, long[] saved, int at, int way, String phase) {
		clear();
		List<BlockPos> route = new ArrayList<>();
		for (long l : saved) {
			route.add(BlockPos.of(l));
		}
		if (route.size() >= 3) {
			stakes.addAll(route);
			for (int i = 0; i + 1 < route.size(); i++) {
				legs.add(SurveyBookItem.survey(level, route.get(i), route.get(i + 1)));
			}
			ends(level);
			this.at = Math.max(0, Math.min(stakes.size() - 1, at));
			this.way = way >= 0 ? 1 : -1;
			try {
				this.phase = Phase.valueOf(phase);
			} catch (IllegalArgumentException ex) {
				this.phase = Phase.DRIVE;
			}
		}
	}
}
