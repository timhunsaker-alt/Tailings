package dev.wildcatter.tailings.entity;

import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.PlantMachineBlock;
import dev.wildcatter.tailings.pit.Loose;
import dev.wildcatter.tailings.pit.PitDrive;
import dev.wildcatter.tailings.pit.PitLoad;
import dev.wildcatter.tailings.pit.Pour;
import dev.wildcatter.tailings.plant.Machine;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The 100 t class haul truck (docs/multiblocks/open-pit.md, Haul trucks): 91 t in its body, counted in kilograms. Standing, jump
 * raises the body; from about a third up the load pours out of its tail ({@link Pour}): into a pit hopper under its tail, or onto
 * the ground behind it, where it runs out and sets block by block by the settle rule. At the top the body comes down again.
 */
public class HaulTruckEntity extends PitVehicleEntity {
	/** The body goes up, or down, in 13 s (proposed). */
	public static final int RAISE_TICKS = 260;

	/** 1 raising, -1 lowering, 0 still. */
	private int raising;
	/** Its surveyed route when it drives itself (open-pit.md, P2a). */
	private final TruckRoute route = new TruckRoute();
	private long[] savedRoute;
	private int savedAt, savedWay;
	private String savedPhase = "DRIVE";

	/** The pour on the client: what to draw of it (open-pit.md, Pouring). */
	private static final net.minecraft.network.syncher.EntityDataAccessor<String> DATA_POUR = net.minecraft.network.syncher.SynchedEntityData.defineId(
			HaulTruckEntity.class, net.minecraft.network.syncher.EntityDataSerializers.STRING);
	/** How full the body looks while it pours, 0 to 1; below 0 when not pouring (then it looks full while it holds anything). */
	private static final net.minecraft.network.syncher.EntityDataAccessor<Float> DATA_FILL = net.minecraft.network.syncher.SynchedEntityData.defineId(
			HaulTruckEntity.class, net.minecraft.network.syncher.EntityDataSerializers.FLOAT);

	/** The load on its way out: each block, in the order it sets. */
	private final java.util.List<Pour.Flow> pour = new java.util.ArrayList<>();
	/** This lift's pour has started. */
	private boolean poured;
	/** What the body held when the pour started, for how full it looks. */
	private long pourKg;
	private long nextRumble;
	/** The client's last read of {@link #DATA_POUR}. */
	private String shownFrom = "";
	private Pour.Shown shown = Pour.Shown.NONE;

	public HaulTruckEntity(EntityType<?> type, Level level) {
		super(type, level);
	}

	@Override
	protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_POUR, "");
		builder.define(DATA_FILL, -1F);
	}

	/** The pour as the client draws it. */
	public Pour.Shown shownPour() {
		String sent = entityData.get(DATA_POUR);
		if (!sent.equals(shownFrom)) {
			shownFrom = sent;
			shown = Pour.shown(sent);
		}
		return shown;
	}

	/** How full the body looks, 0 to 1. */
	public float fill() {
		float f = entityData.get(DATA_FILL);
		return f >= 0 ? f : loaded() ? 1F : 0F;
	}

	/** Blocks of its load still on their way out (tests). */
	public int pouring() {
		return pour.size();
	}

	/** Where its body's tail lip is: back and up from its centre on the ground, at the body's hinge. */
	protected double[] hinge() {
		return new double[] {4.63, 2.0};
	}

	/** The load leaves the body here: the tail lip, a little behind and below the hinge. */
	public Vec3 lip() {
		double[] h = hinge();
		return local(0, h[1] - 0.2 - 0.4 * lift(), -(h[0] + 0.5));
	}

	/** Where a tip on the ground is let go, as the settle rule sees it: 1.5 m behind the tail, 1 m up. */
	public BlockPos tipStart() {
		return BlockPos.containing(local(0, 1, -spec().length() / 2 - 1.5));
	}

	@Override
	public PitDrive.Spec spec() {
		return PitDrive.TRUCK;
	}

	@Override
	protected Item item() {
		return TailingsItems.HAUL_TRUCK;
	}

	@Override
	public double tank() {
		return dev.wildcatter.tailings.pit.PitFuel.TRUCK_TANK_L;
	}

	@Override
	protected double workFraction() {
		return raising != 0 ? dev.wildcatter.tailings.pit.PitFuel.HOIST : 0;
	}

	@Override
	public boolean selfRunning() {
		return route.active();
	}

	/** Parked by its dispatch office, its engine is off (open-pit.md, P2b). */
	@Override
	protected boolean engineIdles() {
		return route.active() && !route.parked();
	}

	/** Parked by its dispatch office, for tests. */
	public boolean parked() {
		return route.parked();
	}

	/** Its route's stakes, for tests. */
	public java.util.List<BlockPos> routeStakes() {
		return java.util.List.copyOf(route.stakes);
	}

	public boolean blocked() {
		return route.blocked();
	}

	@Override
	protected @Nullable Drive autopilot(ServerLevel level) {
		if (savedRoute != null) {
			route.load(level, this, savedRoute, savedAt, savedWay, savedPhase);
			savedRoute = null;
		}
		return route.active() ? route.drive(level, this) : null;
	}

	/** Gives it the survey book's route; why not, or null. Public for tests. */
	public @Nullable String takeRoute(ServerLevel level, java.util.List<BlockPos> stakes) {
		String why = route.set(level, this, stakes);
		if (why == null) {
			setStatus("given a route of " + stakes.size() + " stakes");
		}
		return why;
	}

	@Override
	protected void onSurveyBook(ServerLevel level, Player player, net.minecraft.world.item.ItemStack book) {
		java.util.List<BlockPos> stakes = dev.wildcatter.tailings.item.SurveyBookItem.route(book);
		if (stakes.isEmpty()) {
			route.clear();
			say(player, "off its route: drive it by hand");
			return;
		}
		String why = takeRoute(level, stakes);
		say(player, why == null ? "driving itself on a route of " + stakes.size() + " stakes" : why);
	}

	@Override
	protected void takenOver() {
		route.clear();
		status = "driven by hand";
	}

	@Override
	protected String routeInfo() {
		return level() instanceof ServerLevel server ? route.describe(server) : "";
	}

	/** The middle of its body, where a load-out's gate fills it: about 1 m behind its centre. */
	public Vec3 bodyCentre() {
		return local(0, 0, -1.0);
	}

	@Override
	public boolean busy() {
		return raising != 0 || lift() > 0 || !pour.isEmpty();
	}

	/** Its body goes up, or down, in this many ticks. */
	protected int raiseTicks() {
		return RAISE_TICKS;
	}

	/** A shovel has told it it is loaded (its room under half a bucket): it leaves its LOAD stake (P5c). */
	private boolean loadedOut;

	/** Told by the shovel loading it that it is loaded. */
	public void loadedOut() {
		loadedOut = true;
	}

	/** Whether a shovel has said so since it last asked; asking clears it. */
	boolean takeLoadedOut() {
		boolean was = loadedOut;
		loadedOut = false;
		return was;
	}

	/** Its wheels' radius, metres: a berm at a dump point is mid-axle high, a block a metre of it (P4). */
	public double wheelRadius() {
		return dev.wildcatter.tailings.pit.DryStack.TRUCK_WHEEL_RADIUS;
	}

	/** Its body's top above the ground, metres: a loader that cannot reach it cannot load it. */
	public double loadingHeight() {
		return 4.2;
	}

	@Override
	protected Vec3 seat() {
		return new Vec3(-1.9, 3.1, 3.4);   // the cab, on the deck over the left front wheel
	}

	@Override
	protected void jump(ServerLevel level, @Nullable Player driver) {
		if (raising != 0) {
			return;
		}
		if (Math.abs(speed()) > 0.1) {
			say(driver, "stop to tip");
			return;
		}
		if (load.empty()) {
			say(driver, "empty");
			return;
		}
		raising = 1;
		status = "tipping";
	}

	private void say(@Nullable Player driver, String what) {
		status = what;
		if (driver != null) {
			driver.sendOverlayMessage(Component.literal(getType().getDescription().getString() + ": " + what));
		}
	}

	@Override
	protected void work(ServerLevel level) {
		if (raising != 0) {
			raise(level);
		}
		if (!pour.isEmpty()) {
			pourTick(level);
		}
	}

	/** The body goes up or down a tick's worth; the load starts to pour a third of the way up. */
	private void raise(ServerLevel level) {
		float lift = Math.max(0, Math.min(1, lift() + raising / (float) raiseTicks()));
		if (raising > 0 && lift >= 1 && released(level) < pour.size()) {
			lift = 1;   // held up until the last of the load has slid out
		}
		setLift(lift);
		if (raising > 0 && !poured && lift >= Pour.FROM) {
			startPour(level);
		}
		if (raising > 0 && lift >= 1 && released(level) >= pour.size()) {
			raising = -1;
		} else if (raising < 0 && lift <= 0) {
			raising = 0;
			poured = false;
		}
	}

	/** How many blocks of the pour have left the body. */
	private int released(ServerLevel level) {
		long now = level.getGameTime();
		int n = 0;
		for (Pour.Flow f : pour) {
			if (f.launch() <= now) {
				n++;
			}
		}
		return n;
	}

	/** Tips its whole load at once: the pour started and finished in the same tick. Public for tests and set-piece scenes. */
	public void tip(ServerLevel level) {
		startPour(level);
		finishPour(level, load);
	}

	/**
	 * The load starts to pour out (open-pit.md, Pouring). Into a hopper under its tail it goes in at once, as the hopper counts it,
	 * and is drawn falling in over the pour; refused, it all stays in the body. Onto the ground each block's resting cell is found now
	 * by the settle rule, the cells still on their way held as taken, and what finds no room stays in the body. Its dispatch office
	 * counts what left the body.
	 */
	private void startPour(ServerLevel level) {
		poured = true;
		finishPour(level, load);   // anything left of an earlier pour sets first
		Player driver = getFirstPassenger() instanceof Player p ? p : null;
		long before = load.total();
		long now = level.getGameTime();
		int window = Math.max(1, Math.round((Pour.TO - Pour.FROM) * raiseTicks()));
		Vec3 lip = lip();
		DumpBlockEntity hopper = hopperBehind(level);
		Vec3 land;
		if (hopper != null) {
			String refused = hopper.tip(load);
			refreshLoad();
			if (refused != null) {
				say(driver, "can't tip: " + refused);
				return;
			}
			say(driver, "tipped into the hopper");
			long kg = before - load.total();
			// drawn as one lump a bank block, falling into the pocket under the tail
			int n = (int) Math.max(4, Math.min(160, Math.round(kg / (double) dev.wildcatter.tailings.pit.PitMass.ROCK_KG)));
			BlockPos into = floorUnder(level, BlockPos.containing(lip.x, lip.y, lip.z), 14);
			land = new Vec3(lip.x, into.getY() + 1.0, lip.z);
			int fall = Pour.fall(lip.y - land.y);
			BlockState rock = dev.wildcatter.tailings.TailingsBlocks.MUCK.defaultBlockState();
			for (int i = 0; i < n; i++) {
				long launch = now + (long) i * window / n;
				long share = kg / n + (i < kg % n ? 1 : 0);
				pour.add(new Pour.Flow(into, rock, java.util.Map.of(), java.util.Map.of("pour", share), launch, fall, 0, launch + fall, true));
			}
		} else {
			BlockPos start = tipStart();
			BlockPos ground = floorUnder(level, start, 24);
			land = new Vec3(start.getX() + 0.5, ground.getY() + 1.0, start.getZ() + 0.5);
			java.util.Set<BlockPos> held = new java.util.HashSet<>();
			java.util.List<Loose.Piece> pieces = new java.util.ArrayList<>();
			java.util.List<BlockPos> cells = new java.util.ArrayList<>();
			for (Loose.Piece piece = Loose.next(load); piece != null; piece = Loose.next(load)) {
				BlockPos rest = Loose.rest(level, start, held);
				if (rest == null) {
					piece.putBack(load);
					break;
				}
				held.add(rest);
				pieces.add(piece);
				cells.add(rest);
			}
			refreshLoad();
			int n = pieces.size();
			int fall = Pour.fall(lip.y - land.y);
			long lastSet = 0;
			for (int i = 0; i < n; i++) {
				BlockPos at = cells.get(i);
				Loose.Piece piece = pieces.get(i);
				long launch = now + (long) i * window / Math.max(1, n);
				int run = Pour.run(Math.hypot(at.getX() + 0.5 - land.x, at.getZ() + 0.5 - land.z) + Math.max(0, land.y - at.getY()) * 0.5);
				// it sets after the block before it, so it always sets on ground that stands
				long set = Math.max(launch + fall + run + Pour.SET, lastSet);
				lastSet = set;
				pour.add(new Pour.Flow(at, piece.state(), piece.quarters(), piece.taken(), launch, fall, run, set, false));
			}
			say(driver, "tipped " + n + " blocks" + (load.empty() ? "" : ", some left in the body"));
		}
		pourKg = Math.max(1, before);
		level.playSound(null, getX(), getY(), getZ(), dev.wildcatter.tailings.TailingsSounds.MUCK_TIP, net.minecraft.sounds.SoundSource.BLOCKS, 3F, 0.8F);
		nextRumble = now + 50;
		String sent = Pour.show(lip, land, pour);
		entityData.set(DATA_POUR, sent.length() < 30_000 ? sent : Pour.show(lip, land, pour.subList(0, Math.min(pour.size(), 500))));
		entityData.set(DATA_FILL, 1F);
		long tipped = before - load.total();
		if (tipped > 0 && route.active()) {
			route.tipped(level, this, tipped);   // its dispatch office counts the load
		}
	}

	/** The first block down from {@code from} that is not free ground, or {@code from} {@code most} blocks down. */
	private static BlockPos floorUnder(ServerLevel level, BlockPos from, int most) {
		BlockPos.MutableBlockPos at = from.mutable();
		for (int i = 0; i < most && at.getY() > level.getMinY(); i++) {
			at.move(0, -1, 0);
			BlockState state = level.getBlockState(at);
			if (!net.minecraft.world.level.block.FallingBlock.isFree(state) || state.hasBlockEntity()) {
				return at.immutable();
			}
		}
		return at.immutable();
	}

	/** Each tick of a pour: blocks leave the tail, land, and set where they rest, in order. */
	private void pourTick(ServerLevel level) {
		long now = level.getGameTime();
		Pour.Shown drawn = shownPour();
		long inBody = load.total();
		boolean leaving = false;
		for (Pour.Flow f : pour) {
			if (f.launch() > now) {
				inBody += f.kg();
				continue;
			}
			if (f.launch() == now) {
				leaving = true;
			}
			if (f.launch() + f.fall() == now) {
				Vec3 land = f.into() ? new Vec3(drawn.lip().x, f.at().getY() + 1.0, drawn.lip().z) : drawn.land();
				level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, f.state()), land.x, land.y + 0.2,
						land.z, 6, 0.6, 0.15, 0.6, 0.1);
			}
		}
		if (leaving) {
			Vec3 lip = drawn.lip();
			level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(net.minecraft.core.particles.ParticleTypes.FALLING_DUST, pour.get(0).state()), lip.x, lip.y,
					lip.z, 3, 1.0, 0.1, 0.3, 0.0);
			if (now >= nextRumble) {
				nextRumble = now + 50;
				level.playSound(null, getX(), getY(), getZ(), dev.wildcatter.tailings.TailingsSounds.MUCK_TIP, net.minecraft.sounds.SoundSource.BLOCKS, 2F, 0.7F);
			}
		}
		int setNow = 0;
		while (!pour.isEmpty() && pour.get(0).set() <= now) {
			Pour.Flow f = pour.remove(0);
			if (!f.into()) {
				set(level, f, load);
				if (setNow++ % 3 == 0) {
					level.playSound(null, f.at(), f.state().getSoundType().getPlaceSound(), net.minecraft.sounds.SoundSource.BLOCKS, 0.6F, 0.7F);
				}
			}
		}
		if (pour.isEmpty()) {
			entityData.set(DATA_POUR, "");
			entityData.set(DATA_FILL, -1F);
			refreshLoad();
		} else {
			entityData.set(DATA_FILL, Math.min(1F, (float) inBody / pourKg));
		}
	}

	/**
	 * One block of the pour sets at its cell. If the cell was taken while it was on its way, it settles again from there; with no
	 * room at all, it goes back into {@code back}.
	 */
	private void set(ServerLevel level, Pour.Flow f, PitLoad back) {
		BlockPos at = f.at();
		java.util.Set<BlockPos> held = new java.util.HashSet<>();
		for (Pour.Flow o : pour) {
			if (!o.into()) {
				held.add(o.at());
			}
		}
		BlockState there = level.getBlockState(at);
		if (!net.minecraft.world.level.block.FallingBlock.isFree(there) || there.hasBlockEntity() || net.minecraft.world.level.block.FallingBlock.isFree(level.getBlockState(at.below()))) {
			at = Loose.rest(level, at, held);
		}
		if (at == null) {
			f.piece().putBack(back);
			return;
		}
		f.piece().place(level, at);
	}

	/** Every block still on its way sets now, in order: the truck was removed, wrecked, or tipped all at once. */
	private void finishPour(ServerLevel level, PitLoad back) {
		while (!pour.isEmpty()) {
			Pour.Flow f = pour.remove(0);
			if (!f.into()) {
				set(level, f, back);
			}
		}
		entityData.set(DATA_POUR, "");
		entityData.set(DATA_FILL, -1F);
		refreshLoad();
	}

	/** Removed for good (broken, killed, gone to another dimension) mid-pour, its load still on its way sets at once; unloaded, it is saved. */
	@Override
	public void remove(RemovalReason reason) {
		if (!reason.shouldSave() && !pour.isEmpty() && level() instanceof ServerLevel server) {
			PitLoad back = new PitLoad(Long.MAX_VALUE);
			finishPour(server, back);
			for (java.util.Map.Entry<String, Integer> e : back.wholeBlocks().entrySet()) {
				for (int left = e.getValue(); left > 0; left -= 64) {
					spawnAtLocation(server, dev.wildcatter.tailings.block.Feeds.stack(e.getKey(), Math.min(64, left)));
				}
			}
		}
		super.remove(reason);
	}

	/** A pit hopper, or a gyratory's dump pocket (P7a), under the body's tail: up to 3 m behind it, across its width, down to 8 m. */
	public @Nullable DumpBlockEntity hopperBehind(Level level) {
		for (double back = 0; back <= 3; back += 1) {
			for (double across = -Math.floor(spec().width() / 3); across <= Math.floor(spec().width() / 3); across += 1) {
				Vec3 at = local(across, 0, -spec().length() / 2 - back);
				BlockPos column = BlockPos.containing(at);
				for (int down = 0; down <= 8; down++) {
					BlockPos pos = column.below(down);
					BlockState state = level.getBlockState(pos);
					if (state.getBlock() instanceof PlantMachineBlock block && (block.machine() == Machine.PIT_HOPPER || block.machine() == Machine.GYRATORY_POCKET)
							&& level.getBlockEntity(PlantMachineBlock.mainOf(pos, state)) instanceof DumpBlockEntity hopper) {
						return hopper;
					}
				}
			}
		}
		return null;
	}

	// ---- the crest (open-pit-p4-dry-stack.md, Berms) ----------------------------------------------------------------------------

	/** The ground as a crest or berm sees it: what has a collision box (a survey stake has none). */
	public static dev.wildcatter.tailings.pit.Crest.Ground crestGround(Level level) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		return (x, y, z) -> {
			BlockState state = level.getBlockState(at.set(x, y, z));
			return !state.getCollisionShape(level, at).isEmpty();
		};
	}

	/** Falling over a crest: metres a tick, and how far it has fallen; it is wrecked where it lands. */
	private double overFall;
	private double fallen;
	private boolean goingOver;

	/** True once it has gone over the crest (tests). */
	public boolean goingOver() {
		return goingOver;
	}

	/** Its rear axle, about 2.5 m in from its tail. */
	private Vec3 rearAxle() {
		return local(0, 0, -spec().length() / 2 + 2.5);
	}

	/**
	 * Backing toward a crest: a berm at the crest stops it, as a bumper does (it does not step up onto a ridge with a drop behind it);
	 * with no berm, its rear axle over a drop of 3 m or more, it goes over (P4-4). A self-driving truck never backs to an unbermed crest.
	 */
	@Override
	protected void beforeMove(ServerLevel level) {
		if (speed() >= -0.05) {
			bermBehind = false;
			return;
		}
		dev.wildcatter.tailings.pit.Crest.Ground ground = crestGround(level);
		int y = (int) Math.floor(getY() + 0.01);
		Vec3 axle = rearAxle();
		if (!route.active() && dev.wildcatter.tailings.pit.Crest.drop(ground, (int) Math.floor(axle.x), y, (int) Math.floor(axle.z))) {
			goingOver = true;
			overFall = 0;
			fallen = 0;
			// it tips back over the edge: its centre goes out past its rear axle, over the drop
			Vec3 out = ahead().scale(-(spec().length() / 2 - 1.5));
			setPos(getX() + out.x, getY(), getZ() + out.z);
			status = "over the crest";
			if (getFirstPassenger() instanceof Player p) {
				p.sendOverlayMessage(Component.literal("Haul Truck: over the crest!"));
			}
			return;
		}
		// a ridge just behind its tail with a drop behind that: a berm
		bermBehind = false;
		for (double back = 0.5; back <= 2.0 && !bermBehind; back += 0.5) {
			Vec3 at = local(0, 0, -spec().length() / 2 - back);
			int x = (int) Math.floor(at.x), z = (int) Math.floor(at.z);
			if (ground.solid(x, y, z) && !ground.solid(x, y + 1, z)) {
				for (double beyond = 1; beyond <= 4 && !bermBehind; beyond += 1) {
					Vec3 b = local(0, 0, -spec().length() / 2 - back - beyond);
					bermBehind = dev.wildcatter.tailings.pit.Crest.drop(ground, (int) Math.floor(b.x), y, (int) Math.floor(b.z));
				}
			}
		}
	}

	private boolean bermBehind;

	@Override
	public float maxUpStep() {
		return bermBehind ? 0F : super.maxUpStep();
	}

	@Override
	protected boolean ownTick(ServerLevel level) {
		if (!goingOver) {
			return false;
		}
		// it tips back over the edge and falls: it drifts back as it goes, and is wrecked where it lands
		overFall = Math.min(3.0, overFall + 0.08);
		Vec3 back = ahead().scale(-0.15);
		double drop = overFall;
		BlockPos below = BlockPos.containing(getX(), getY() - drop, getZ());
		boolean landed = false;
		for (int y = (int) Math.floor(getY()) - 1; y >= below.getY(); y--) {
			if (crestGround(level).solid(below.getX(), y, below.getZ()) || y <= level.getMinY()) {
				drop = getY() - (y + 1);
				landed = true;
				break;
			}
		}
		setPos(getX() + back.x, getY() - Math.max(0, drop), getZ() + back.z);
		fallen += Math.max(0, drop);
		if (landed) {
			wreck(level);
		}
		return true;
	}

	/** Wrecked at the bottom: it breaks into its item and its load, tipped where it lands, and its driver takes the fall. Public for tests. */
	public void wreck(ServerLevel level) {
		finishPour(level, load);
		Loose.tip(level, blockPosition().above(), load);
		refreshLoad();
		spawnAtLocation(level, new net.minecraft.world.item.ItemStack(item()));
		java.util.List<net.minecraft.world.entity.Entity> riders = java.util.List.copyOf(getPassengers());
		ejectPassengers();
		for (net.minecraft.world.entity.Entity rider : riders) {
			float hurt = (float) Math.max(0, fallen - 3);
			if (hurt > 0) {
				rider.hurtServer(level, level.damageSources().fall(), hurt);
			}
		}
		level.playSound(null, getX(), getY(), getZ(), dev.wildcatter.tailings.TailingsSounds.MUCK_TIP, net.minecraft.sounds.SoundSource.BLOCKS, 4F, 0.5F);
		discard();
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		raising = input.getIntOr("raising", 0);
		poured = input.getBooleanOr("poured", false);
		pourKg = input.getLongOr("pour_kg", 1);
		pour.clear();
		pour.addAll(Pour.load(input.getStringOr("pour", "")));   // what was still on its way sets on its first tick back, if its time has come
		entityData.set(DATA_POUR, pour.isEmpty() ? "" : input.getStringOr("pour_shown", ""));
		String saved = input.getStringOr("route", "");
		long[] stakes = saved.isEmpty() ? new long[0] : java.util.Arrays.stream(saved.split(",")).mapToLong(Long::parseLong).toArray();
		if (stakes.length > 0) {
			savedRoute = stakes;   // set again on its first tick, when the level can survey it
			savedAt = input.getIntOr("route_at", 0);
			savedWay = input.getIntOr("route_way", 1);
			savedPhase = input.getStringOr("route_phase", "DRIVE");
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putInt("raising", raising);
		output.putBoolean("poured", poured);
		output.putLong("pour_kg", pourKg);
		if (!pour.isEmpty()) {
			output.putString("pour", Pour.save(pour));
			output.putString("pour_shown", entityData.get(DATA_POUR));
		}
		if (route.active()) {
			output.putString("route", java.util.Arrays.stream(route.save()).mapToObj(Long::toString).collect(java.util.stream.Collectors.joining(",")));
			output.putInt("route_at", route.at);
			output.putInt("route_way", route.way);
			output.putString("route_phase", route.phase.name());
		}
	}
}
