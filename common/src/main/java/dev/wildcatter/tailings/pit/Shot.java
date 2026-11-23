package dev.wildcatter.tailings.pit;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.BlastholeBlock;
import dev.wildcatter.tailings.block.BlastholeBlockEntity;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A shot fired (docs/multiblocks/open-pit-p3-blasting.md, The blast): its logged holes fire in delay order, a tick each 50 ms. Each hole
 * breaks its own box, burden deep toward its free face, spacing wide, collar to bench floor: hard rock and ore to muck holding 3 quarters
 * of what it was, the 4th quarter of every block spilled off the free face as the swell (4 for 3) by the settle rule. Soft ground,
 * muck, fluid and anything unbreakable are left. A hole under 2 m of stemming blows out, breaking only the lower half of its box and
 * throwing flyrock; a short burden or an overcharge throws some forward. Players and mobs within 50 m are hurt by the spec's line. No
 * vanilla explosion: no crater, no drops.
 */
public final class Shot {
	/** One hole's box: the way it breaks (null for a box cut), how far its free face is, and its cells. */
	public record Box(BlockPos collar, @Nullable Direction face, int faceAt, int x0, int x1, int y0, int y1, int z0, int z1) {
		public AABB aabb() {
			return new AABB(x0, y0, z0, x1 + 1, y1 + 1, z1 + 1);
		}

		public boolean contains(BlockPos p) {
			return p.getX() >= x0 && p.getX() <= x1 && p.getY() >= y0 && p.getY() <= y1 && p.getZ() >= z0 && p.getZ() <= z1;
		}
	}

	/** A hole as the shot has it: its collar and delay. */
	public record Hole(BlockPos collar, int delayMs) {
	}

	private record Flight(int landTick, int x, int z, String id) {
	}

	/**
	 * A fragment in the air: where it left, its speed (blocks a tick), the shot tick it left and the tick it lands (traced through the
	 * world as it was thrown), and the rock it is drawn as.
	 */
	private record Fragment(Vec3 from, Vec3 v, int launch, int land, BlockState state) {
		Vec3 at(int t) {
			return dev.wildcatter.tailings.entity.FlyrockEntity.position(from, v, Math.max(0, Math.min(t, land) - launch));
		}
	}

	private final ServerLevel level;
	private final List<Hole> holes;
	private final Set<BlockPos> broken = new HashSet<>();
	private final Set<BlockPos> fired = new HashSet<>();
	private final Map<String, Integer> swell = new LinkedHashMap<>();
	private final List<Flight> flights = new ArrayList<>();
	private final List<Fragment> fragments = new ArrayList<>();
	/** Who a fragment has been aimed at this shot: one strike each, however many holes reach them. */
	private final Set<java.util.UUID> aimedAt = new HashSet<>();
	private int thrown;
	private int strikes;
	private double reach;
	/** The broken muck slumping to its angle of repose, after the last hole: the cells left to look at in this pass, and the pass. */
	private @Nullable List<BlockPos> slumping;
	private int slumpAt;
	private int slumpPass;
	private int slumpMoved;
	private int slumped;
	private boolean slumpDone;
	private int tick;
	private int next;
	private int bank;
	private int misfires;
	private int blowouts;
	private int flyrock;
	private double kgFired;
	private boolean harmed;
	private @Nullable BlockPos lastSpill;
	private final List<String> notes = new ArrayList<>();

	public Shot(ServerLevel level, List<Hole> holes) {
		this.level = level;
		this.holes = new ArrayList<>(holes);
		this.holes.sort(Comparator.comparingInt(Hole::delayMs));
	}

	public int bank() {
		return bank;
	}

	public int misfires() {
		return misfires;
	}

	public int blowouts() {
		return blowouts;
	}

	public int flyrock() {
		return flyrock;
	}

	/** Fragments thrown, every firing hole's, drawn and flown. */
	public int fragmentsThrown() {
		return thrown;
	}

	/** Fragments that struck a player or mob. */
	public int strikes() {
		return strikes;
	}

	/** The farthest any fired hole could throw, m (at most 100). */
	public double reach() {
		return reach;
	}

	/** Muck blocks that rolled off the broken face as it slumped. */
	public int slumped() {
		return slumped;
	}

	public List<String> notes() {
		return notes;
	}

	public double tonnes() {
		return bank * PitMass.ROCK_KG / 1000.0;
	}

	public boolean done() {
		return next >= holes.size() && flights.isEmpty() && fragments.isEmpty() && slumpDone;
	}

	public String summary() {
		return String.format(Locale.ROOT, "FIRED: %d holes, %.0f t broken, %d misfires%s", holes.size() - misfires, tonnes(), misfires,
				blowouts > 0 ? ", " + blowouts + " blowouts" : "");
	}

	// ---- planning: the boxes, as if every hole fires ---------------------------------------------------------------------------

	/** Every hole's box, worked out in delay order with the holes before it broken: what the guard checks. */
	public static List<Box> plan(ServerLevel level, List<Hole> holes) {
		List<Hole> sorted = new ArrayList<>(holes);
		sorted.sort(Comparator.comparingInt(Hole::delayMs));
		Set<BlockPos> open = new HashSet<>();
		List<Box> out = new ArrayList<>();
		for (Hole h : sorted) {
			if (!(level.getBlockEntity(h.collar()) instanceof BlastholeBlockEntity hole)) {
				continue;
			}
			Box box = box(level, hole, open, false);
			out.add(box);
			for (BlockPos p : BlockPos.betweenClosed(box.x0, box.y0, box.z0, box.x1, box.y1, box.z1)) {
				open.add(p.immutable());
			}
		}
		return out;
	}

	/**
	 * A hole's box. Its free face is the side, of four, where open ground (air, fluid, or rock this shot has already broken) stands at
	 * least half the bench high within burden + 2 m: the pattern's own face when it has one there, else the nearest such side; with
	 * none it is a box cut, centred on the hole.
	 */
	static Box box(ServerLevel level, BlastholeBlockEntity hole, Set<BlockPos> open, boolean blowout) {
		BlockPos c = hole.collar();
		int bench = hole.bench(), burden = hole.burden(), spacing = hole.spacing();
		int y1 = c.getY(), y0 = c.getY() - bench + 1;
		if (blowout) {
			y1 = y0 + bench / 2 - 1;   // it vents up the collar: the lower half breaks, the top stands
		}
		Direction face = null;
		int faceAt = Integer.MAX_VALUE;
		List<Direction> ways = new ArrayList<>(Direction.Plane.HORIZONTAL.stream().toList());
		if (hole.face() != null) {
			ways.remove(hole.face());
			ways.add(0, hole.face());   // the pattern's own face wins a tie
		}
		for (Direction d : ways) {
			if (face != null && face == hole.face()) {
				break;   // the pattern's own face, when it has one in reach, is the way it breaks: a neighbour broken first does not turn it
			}
			for (int k = 1; k <= burden + 2 && k < faceAt; k++) {
				int count = 0;
				for (int y = c.getY() - bench + 1; y <= c.getY(); y++) {
					BlockPos p = new BlockPos(c.getX() + d.getStepX() * k, y, c.getZ() + d.getStepZ() * k);
					BlockState s = level.getBlockState(p);
					if (open.contains(p) || s.isAir() || !s.getFluidState().isEmpty()) {
						count++;
					}
				}
				if (count * 2 >= bench) {
					face = d;
					faceAt = k;
					break;
				}
			}
		}
		int lo = -spacing / 2, hi = spacing - spacing / 2 - 1;
		int a0, a1;
		Direction along = face != null ? face : hole.face() != null ? hole.face() : Direction.NORTH;
		if (face != null) {
			a0 = 0;
			a1 = burden - 1;
		} else {
			a0 = -burden / 2;
			a1 = burden - burden / 2 - 1;
			faceAt = -1;
		}
		Direction across = along.getClockWise();
		int[] xs = {c.getX() + along.getStepX() * a0 + across.getStepX() * lo, c.getX() + along.getStepX() * a1 + across.getStepX() * hi};
		int[] zs = {c.getZ() + along.getStepZ() * a0 + across.getStepZ() * lo, c.getZ() + along.getStepZ() * a1 + across.getStepZ() * hi};
		return new Box(c, face, faceAt, Math.min(xs[0], xs[1]), Math.max(xs[0], xs[1]), y0, y1, Math.min(zs[0], zs[1]), Math.max(zs[0], zs[1]));
	}

	/** Anything that must not be in a shot: a machine's block entity, or a Tailings or Wildcatter vehicle. Null when clear. */
	public static @Nullable String machineIn(ServerLevel level, List<Box> boxes) {
		for (Box box : boxes) {
			for (BlockPos p : BlockPos.betweenClosed(box.x0, box.y0, box.z0, box.x1, box.y1, box.z1)) {
				BlockState s = level.getBlockState(p);
				if (s.hasBlockEntity() && !(s.getBlock() instanceof BlastholeBlock) && s.getBlock() != TailingsBlocks.MUCK) {
					return "MACHINE IN THE SHOT: " + s.getBlock().getName().getString() + " at " + p.toShortString() + ": move it";
				}
			}
			for (Entity e : level.getEntities((Entity) null, box.aabb().inflate(0, 1, 0), e -> !(e instanceof LivingEntity)
					&& !(e instanceof dev.wildcatter.tailings.entity.FlyrockEntity) && !(e instanceof dev.wildcatter.tailings.entity.SlideEntity))) {
				String ns = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace();
				if (ns.equals("tailings") || ns.equals("wildcatter")) {
					return "MACHINE IN THE SHOT: " + e.getType().getDescription().getString() + ": move it";
				}
			}
		}
		return null;
	}

	/**
	 * How far the shot's flyrock can fly, m, as planned: for each hole that will fire, the most of its cratering (its stemming) and its
	 * face burst (the burden to its face), each held to 100 m. The danger zone is this, or the 50 m of the blast's own harm.
	 */
	public static double plannedReach(ServerLevel level, List<Hole> holes) {
		double r = 0;
		for (Box b : plan(level, holes)) {
			if (!(level.getBlockEntity(b.collar()) instanceof BlastholeBlockEntity hole) || fires(hole) != null) {
				continue;
			}
			double m = Blasting.kgPerM(hole.chargeKg(), Blasting.effectiveKg(hole.chargeKg(), hole.chargedTick(), level.getGameTime()));
			r = Math.max(r, Blasting.flyrockReach(Blasting.crateringM(m, hole.stemming())));
			if (b.face() != null) {
				r = Math.max(r, Blasting.flyrockReach(Blasting.faceBurstM(m, Math.max(0.5, b.faceAt() - 0.5))));
			}
		}
		return r;
	}

	/** The danger zone's radius round the holes, m: the flyrock's reach or the blast's 50 m, whichever is farther. */
	public static double dangerZone(ServerLevel level, List<Hole> holes) {
		return Math.max(Blasting.HARM_RADIUS, plannedReach(level, holes));
	}

	// ---- firing ------------------------------------------------------------------------------------------------------------------

	/** One tick of the shot. */
	public void tick() {
		int ms = tick * Blasting.MS_PER_TICK;
		if (tick == 0) {
			start();
		}
		while (next < holes.size() && holes.get(next).delayMs() <= ms) {
			fire(holes.get(next++));
		}
		if (next >= holes.size() && !swell.isEmpty()) {
			spillRest();
		}
		for (int i = flights.size() - 1; i >= 0; i--) {
			if (flights.get(i).landTick() <= tick) {
				land(flights.remove(i));
			}
		}
		flyFragments();
		if (next >= holes.size() && swell.isEmpty() && !slumpDone) {
			slump();
		}
		tick++;
	}

	/** Harm, the sound, the dust: once, as the first hole goes. */
	private void start() {
		List<Vec3> firing = new ArrayList<>();
		for (Hole h : holes) {
			if (level.getBlockEntity(h.collar()) instanceof BlastholeBlockEntity hole && fires(hole) == null) {
				firing.add(Vec3.atCenterOf(h.collar()));
			}
		}
		if (firing.isEmpty()) {
			return;
		}
		Vec3 centre = Vec3.ZERO;
		for (Vec3 v : firing) {
			centre = centre.add(v);
		}
		centre = centre.scale(1.0 / firing.size());
		level.playSound(null, centre.x, centre.y, centre.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 16F, 0.6F);
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, centre.x, centre.y + 4, centre.z, 120, 12, 6, 12, 0.02);
		AABB reach = new AABB(centre, centre).inflate(Blasting.HARM_RADIUS + 60, 80, Blasting.HARM_RADIUS + 60);
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, reach)) {
			double r = Double.MAX_VALUE;
			for (Vec3 v : firing) {
				r = Math.min(r, living.position().distanceTo(v));
			}
			double harm = Blasting.harm(r);
			if (harm > 0) {
				living.hurtServer(level, level.damageSources().explosion(null, null), (float) harm);
			}
		}
		harmed = true;
	}

	/** Why a hole will not fire, or null when it will. */
	public static @Nullable String fires(BlastholeBlockEntity hole) {
		if (hole.misfired()) {
			return "misfired already";
		}
		if (hole.detonator() == 0) {
			return "no detonator: it does not answer";
		}
		if (!hole.booster()) {
			return "no booster";
		}
		if (Blasting.powderFactor(hole.chargeKg(), hole.boxTonnes()) < Blasting.UNDERCHARGED_KG_PER_T) {
			return String.format(Locale.ROOT, "UNDERCHARGED: %.0f kg is %.2f kg/t", hole.chargeKg(), Blasting.powderFactor(hole.chargeKg(), hole.boxTonnes()));
		}
		return null;
	}

	private void fire(Hole h) {
		if (!(level.getBlockEntity(h.collar()) instanceof BlastholeBlockEntity hole)) {
			return;
		}
		double kg = Blasting.effectiveKg(hole.chargeKg(), hole.chargedTick(), level.getGameTime());
		Blasting.Outcome outcome = hole.detonator() == 0 ? Blasting.Outcome.NO_PRIMER
				: Blasting.outcome(hole.primed(), kg, hole.boxTonnes(), hole.stemming());
		if (outcome == Blasting.Outcome.NO_PRIMER || outcome == Blasting.Outcome.UNDERCHARGED) {
			String why = outcome == Blasting.Outcome.UNDERCHARGED ? String.format(Locale.ROOT, "UNDERCHARGED (%.2f kg/t)", Blasting.powderFactor(kg, hole.boxTonnes()))
					: hole.detonator() == 0 ? "no detonator answered" : "no booster";
			hole.misfire(why);
			misfires++;
			notes.add(h.collar().toShortString() + " misfired: " + why);
			return;
		}
		fired.add(h.collar());
		Slopes.blasted(level, h.collar());   // a highwall near the hole is damaged, one farther off shaken (dragline-hand-control.md)
		kgFired += kg;
		boolean blowout = outcome == Blasting.Outcome.BLOWOUT;
		Box box = box(level, hole, broken, blowout);
		int depth = hole.depth();
		BlockPos collar = h.collar();
		String rock = hole.replaced();
		// the hole's own column below the bench floor (the subdrill) is fractured, not broken: it is rock again
		for (int m = 0; m < depth; m++) {
			BlockPos p = collar.below(m);
			if (!box.contains(p) && level.getBlockEntity(p) instanceof BlastholeBlockEntity part) {
				level.setBlock(p, part.replacedState(), Block.UPDATE_CLIENTS);
			}
		}
		for (BlockPos p : BlockPos.betweenClosed(box.x0, box.y0, box.z0, box.x1, box.y1, box.z1)) {
			breakAt(p.immutable());
		}
		int throwN = 0;
		Direction toward = box.face();
		if (blowout) {
			blowouts++;
			throwN = Blasting.BLOWOUT_FLYROCK;
			toward = null;
			notes.add(collar.toShortString() + " blew out: stemming " + String.format(Locale.ROOT, "%.1f m", hole.stemming()));
		} else if (box.face() != null && box.faceAt() - 1 < hole.burden() / 2.0) {
			throwN = Blasting.SHORT_BURDEN_FLYROCK;
		} else if (outcome == Blasting.Outcome.OVERCHARGED) {
			throwN = Blasting.OVERCHARGE_FLYROCK;
		}
		List<dev.wildcatter.tailings.entity.FlyrockEntity.Arc> arcs = new ArrayList<>();
		for (int i = 0; i < throwN; i++) {
			throwRock(collar, toward, rock, arcs);
		}
		throwFragments(hole, box, kg, rock, arcs);   // off the face as it stood, before the swell spills down it
		dev.wildcatter.tailings.entity.FlyrockEntity.show(level, Vec3.atCenterOf(collar.above()), arcs);
		BlockPos spill = box.face() == null ? collar.above() : collar.relative(box.face(), hole.burden()).above();
		lastSpill = spill;
		spillWhole(spill);
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, collar.getX() + 0.5, collar.getY() + 2, collar.getZ() + 0.5, 30, 3, 3, 3, 0.03);
	}

	/** One block of the box: hard rock and ore to muck; the rest left. */
	private void breakAt(BlockPos p) {
		if (broken.contains(p)) {
			return;
		}
		BlockState s = level.getBlockState(p);
		String id;
		if (s.getBlock() instanceof BlastholeBlock) {
			if (!(level.getBlockEntity(p) instanceof BlastholeBlockEntity part) || !fired.contains(part.collar())) {
				return;   // another hole's column: it breaks when that hole fires, and a misfire stands in the muck
			}
			id = part.replaced();
		} else {
			// rock: a block a pickaxe digs, with a collision box and no block entity; never planks, glass or a machine
			if (s.isAir() || !s.getFluidState().isEmpty() || s.hasBlockEntity() || !s.is(BlockTags.MINEABLE_WITH_PICKAXE)
					|| s.getCollisionShape(level, p).isEmpty() || s.getDestroySpeed(level, p) < 0
					|| s.is(Blocks.OBSIDIAN) || s.is(Blocks.CRYING_OBSIDIAN) || s.is(Blocks.REINFORCED_DEEPSLATE)) {
				return;
			}
			id = PitMass.dugAs(BuiltInRegistries.BLOCK.getKey(s.getBlock()).toString());
			if (PitMass.soft(id)) {
				return;   // already diggable
			}
		}
		level.setBlock(p, TailingsBlocks.MUCK.defaultBlockState(), Block.UPDATE_CLIENTS);
		if (level.getBlockEntity(p) instanceof dev.wildcatter.tailings.block.MuckBlockEntity muck) {
			muck.set(Map.of(id, PitMass.MUCK_QUARTERS));
		}
		broken.add(p);
		bank++;
		swell.merge(id, 1, Integer::sum);
	}

	/** The swell, three quarters to a muck block, let go off the face. */
	private void spillWhole(BlockPos at) {
		while (quarters() >= PitMass.MUCK_QUARTERS) {
			Map<String, Integer> block = takeQuarters(PitMass.MUCK_QUARTERS);
			if (Loose.muck(level, at, block) == null) {
				block.forEach((id, q) -> swell.merge(id, q, Integer::sum));
				notes.add("no room to spill the swell at " + at.toShortString());
				return;
			}
		}
	}

	/** At the end, what is under a whole muck block goes as a part block, so nothing is lost. */
	private void spillRest() {
		if (lastSpill == null) {
			swell.clear();
			return;
		}
		spillWhole(lastSpill);
		if (quarters() > 0) {
			Map<String, Integer> rest = takeQuarters(quarters());
			if (Loose.muck(level, lastSpill, rest) == null) {
				notes.add("no room for the last of the swell");
			}
		}
		swell.clear();
	}

	private int quarters() {
		int n = 0;
		for (int q : swell.values()) {
			n += q;
		}
		return n;
	}

	private Map<String, Integer> takeQuarters(int n) {
		Map<String, Integer> out = new LinkedHashMap<>();
		var it = swell.entrySet().iterator();
		while (n > 0 && it.hasNext()) {
			var e = it.next();
			int take = Math.min(n, e.getValue());
			out.merge(e.getKey(), take, Integer::sum);
			n -= take;
			if (take == e.getValue()) {
				it.remove();
			} else {
				e.setValue(e.getValue() - take);
			}
		}
		return out;
	}

	/** Flyrock: a quarter of the hole's rock on a ballistic arc, landing up to 100 m off; forward, or any way from a blowout. */
	private void throwRock(BlockPos from, @Nullable Direction toward, String rock, List<dev.wildcatter.tailings.entity.FlyrockEntity.Arc> arcs) {
		String id = rock;
		if (swell.getOrDefault(rock, 0) > 0) {
			swell.merge(rock, -1, Integer::sum);
			swell.remove(rock, 0);
		} else if (!swell.isEmpty()) {
			id = swell.keySet().iterator().next();
			swell.merge(id, -1, Integer::sum);
			swell.remove(id, 0);
		}
		double angle = toward == null ? level.getRandom().nextDouble() * Math.PI * 2
				: Math.atan2(toward.getStepZ(), toward.getStepX()) + (level.getRandom().nextDouble() - 0.5) * Math.toRadians(60);
		double range = 40 + level.getRandom().nextDouble() * (Blasting.FLYROCK_RANGE - 40);
		int x = (int) Math.floor(from.getX() + 0.5 + Math.cos(angle) * range);
		int z = (int) Math.floor(from.getZ() + 0.5 + Math.sin(angle) * range);
		int flight = Blasting.flightTicks(range);
		// drawn: a whole block on the arc to where it will land, when the ground there is loaded to aim at
		Vec3 start = Vec3.atCenterOf(from.above());
		double speed = Blasting.launchSpeed(range);
		if (level.hasChunkAt(new BlockPos(x, from.getY(), z))) {
			double dy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 0.5 - start.y;
			double up = Blasting.aim(speed, range, dy, false);
			if (!Double.isNaN(up)) {
				flight = Math.max(1, (int) Math.round(range / (speed * Math.cos(up)) * 20));
				double vh = speed * Math.cos(up) / 20, vy = speed * Math.sin(up) / 20;
				arcs.add(new dev.wildcatter.tailings.entity.FlyrockEntity.Arc(start, new Vec3(Math.cos(angle) * vh, vy, Math.sin(angle) * vh),
						level.getGameTime(), level.getGameTime() + flight, (float) Blasting.BLOCK_FLYROCK_M, rockState(id)));
			}
		}
		flights.add(new Flight(tick + flight, x, z, id));
		flyrock++;
	}

	/** The block a piece of {@code id} is drawn as: broken stone as cobblestone, the rest as itself. */
	private static BlockState rockState(String id) {
		Block block = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse(id));
		if (block == Blocks.STONE || block == Blocks.AIR) {
			return Blocks.COBBLESTONE.defaultBlockState();
		}
		if (block == Blocks.DEEPSLATE) {
			return Blocks.COBBLED_DEEPSLATE.defaultBlockState();
		}
		return block.defaultBlockState();
	}

	// ---- fragments: what every firing hole throws, drawn and flown (open-pit-p3-blasting.md, Flyrock) ----------------------------

	/**
	 * Every hole that fires throws fragments: off its free face, as far as Richards and Moore's face burst for its charge a metre and the
	 * burden standing in front of it; and up out of its collar, as far as their cratering for its stemming; each held to 100 m. Anyone
	 * standing in the open within a reach gets one fragment aimed at them a shot; cover over the head (a roof, a shelter) stops it.
	 */
	private void throwFragments(BlastholeBlockEntity hole, Box box, double kg, String rock, List<dev.wildcatter.tailings.entity.FlyrockEntity.Arc> arcs) {
		BlockPos collar = hole.collar();
		double m = Blasting.kgPerM(hole.chargeKg(), kg);
		BlockState state = rockState(rock);
		var random = level.getRandom();
		// the face burst: only off a real face, open air in front, not into the muck of the row ahead
		Vec3 faceAt = null;
		double faceReach = 0;
		Direction face = box.face();
		if (face != null) {
			int mid = collar.getY() - hole.bench() / 2;
			BlockPos front = new BlockPos(collar.getX() + face.getStepX() * box.faceAt(), mid, collar.getZ() + face.getStepZ() * box.faceAt());
			if (!broken.contains(front) && level.getBlockState(front).isAir()) {
				faceReach = Blasting.flyrockReach(Blasting.faceBurstM(m, Math.max(0.5, box.faceAt() - 0.5)));
				faceAt = new Vec3(collar.getX() + 0.5 + face.getStepX() * (box.faceAt() - 0.5), mid + 0.5, collar.getZ() + 0.5 + face.getStepZ() * (box.faceAt() - 0.5));
			}
		}
		double collarReach = Blasting.flyrockReach(Blasting.crateringM(m, hole.stemming()));
		Vec3 top = new Vec3(collar.getX() + 0.5, collar.getY() + 1.2, collar.getZ() + 0.5);
		reach = Math.max(reach, Math.max(faceReach, collarReach));
		if (faceAt != null && faceReach > 0) {
			double vMax = Blasting.launchSpeed(faceReach);
			double yaw0 = Math.atan2(face.getStepZ(), face.getStepX());
			for (int i = 0; i < Blasting.FACE_FRAGMENTS; i++) {
				double yaw = yaw0 + (random.nextDouble() - 0.5) * Math.toRadians(70);
				double up = Math.toRadians(5 + random.nextDouble() * 35);
				double speed = vMax * Math.sqrt(0.3 + 0.7 * random.nextDouble());
				Direction across = face.getClockWise();
				double side = (random.nextDouble() - 0.5) * hole.spacing();
				Vec3 from = faceAt.add(across.getStepX() * side, (random.nextDouble() - 0.5) * hole.bench() * 0.6, across.getStepZ() * side);
				launch(from, yaw, up, speed, state, arcs, random);
			}
		}
		if (collarReach > 0) {
			double vMax = Blasting.launchSpeed(collarReach);
			for (int i = 0; i < Blasting.COLLAR_FRAGMENTS; i++) {
				double yaw = random.nextDouble() * Math.PI * 2;
				double up = Math.toRadians(50 + random.nextDouble() * 30);
				double speed = vMax * Math.sqrt(0.2 + 0.8 * random.nextDouble());
				launch(top, yaw, up, speed, state, arcs, random);
			}
		}
		// anyone in the open within reach: one fragment aimed at them a shot
		AABB around = new AABB(collar).inflate(Blasting.FLYROCK_RANGE + 2, 60, Blasting.FLYROCK_RANGE + 2);
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, around, e -> e.isAlive() && !e.isSpectator())) {
			if (aimedAt.contains(living.getUUID()) || !level.canSeeSky(BlockPos.containing(living.getEyePosition()))) {
				continue;
			}
			Vec3 target = living.getBoundingBox().getCenter();
			Vec3 from = null;
			double speed = 0;
			boolean high = false;
			if (faceAt != null && faceReach > 0) {
				double dx = target.x - faceAt.x, dz = target.z - faceAt.z;
				double d = Math.hypot(dx, dz);
				if (d > 0.5 && (dx * face.getStepX() + dz * face.getStepZ()) / d >= 0.5
						&& Blasting.reaches(Blasting.launchSpeed(faceReach), d, target.y - faceAt.y)) {
					from = faceAt;
					speed = Blasting.launchSpeed(faceReach);
				}
			}
			if (from == null && collarReach > 0) {
				double d = Math.hypot(target.x - top.x, target.z - top.z);
				if (Blasting.reaches(Blasting.launchSpeed(collarReach), d, target.y - top.y)) {
					from = top;
					speed = Blasting.launchSpeed(collarReach);
					high = true;
				}
			}
			if (from == null) {
				continue;
			}
			double d = Math.hypot(target.x - from.x, target.z - from.z);
			double up = Blasting.aim(speed, Math.max(d, 0.5), target.y - from.y, high);
			if (Double.isNaN(up)) {
				continue;
			}
			aimedAt.add(living.getUUID());
			launch(from, Math.atan2(target.z - from.z, target.x - from.x), up, speed, state, arcs, random);
		}
	}

	/** Puts one fragment in the air: traced through the world as it stands to where it lands, drawn, flown. */
	private void launch(Vec3 from, double yaw, double up, double speed, BlockState state, List<dev.wildcatter.tailings.entity.FlyrockEntity.Arc> arcs,
			net.minecraft.util.RandomSource random) {
		double vh = speed * Math.cos(up) / 20;
		Vec3 v = new Vec3(Math.cos(yaw) * vh, speed * Math.sin(up) / 20, Math.sin(yaw) * vh);
		int land = 400;
		Vec3 a = from;
		for (int t = 1; t <= 400; t++) {
			Vec3 b = dev.wildcatter.tailings.entity.FlyrockEntity.position(from, v, t);
			if (!level.hasChunkAt(BlockPos.containing(b)) || b.y < level.getMinY()) {
				land = t;
				break;
			}
			if (t > 2 && level.clip(new net.minecraft.world.level.ClipContext(a, b, net.minecraft.world.level.ClipContext.Block.COLLIDER,
					net.minecraft.world.level.ClipContext.Fluid.ANY, net.minecraft.world.phys.shapes.CollisionContext.empty())).getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
				land = t;
				break;
			}
			a = b;
		}
		fragments.add(new Fragment(from, v, tick, tick + land, state));
		arcs.add(new dev.wildcatter.tailings.entity.FlyrockEntity.Arc(from, v, level.getGameTime(), level.getGameTime() + land,
				(float) (Blasting.FRAGMENT_MIN_M + random.nextDouble() * (Blasting.FRAGMENT_MAX_M - Blasting.FRAGMENT_MIN_M)), state));
		thrown++;
	}

	/** Each fragment in the air goes on a tick: what it passes through is struck (10, as flyrock), and it stops there or on the ground. */
	private void flyFragments() {
		for (int i = fragments.size() - 1; i >= 0; i--) {
			Fragment f = fragments.get(i);
			if (tick <= f.launch()) {
				continue;
			}
			Vec3 a = f.at(tick - 1), b = f.at(tick);
			AABB path = new AABB(a, b).inflate(0.4);
			LivingEntity hit = null;
			for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, path, e -> e.isAlive() && !e.isSpectator())) {
				if (living.getBoundingBox().inflate(0.3).clip(a, b).isPresent() || living.getBoundingBox().inflate(0.3).contains(b)) {
					hit = living;
					break;
				}
			}
			if (hit != null) {
				hit.setInvulnerableTime(0);   // a second stone is a second blow, whatever hit them a moment ago
				hit.hurtServer(level, new net.minecraft.world.damagesource.DamageSource(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getOrThrow(net.minecraft.world.damagesource.DamageTypes.FALLING_BLOCK)), (float) Blasting.FLYROCK_DAMAGE);
				strikes++;
				fragments.remove(i);
				continue;
			}
			if (tick >= f.land()) {
				fragments.remove(i);
				if (level.hasChunkAt(BlockPos.containing(b))) {
					level.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, f.state()), b.x, b.y + 0.2, b.z, 8, 0.3, 0.2, 0.3, 0.1);
					if (level.getRandom().nextInt(3) == 0) {
						level.playSound(null, b.x, b.y, b.z, SoundEvents.STONE_FALL, SoundSource.BLOCKS, 1.5F, 0.7F + level.getRandom().nextFloat() * 0.3F);
					}
				}
			}
		}
	}

	// ---- the slump: broken rock does not stand as a wall (open-pit-p3-blasting.md, The muck pile) ----------------------------------

	/** Most cells looked at a tick, and most passes, as the broken face slumps. */
	private static final int SLUMP_PER_TICK = 4000, SLUMP_PASSES = 40;

	/**
	 * After the last hole, the broken rock rolls off the face it stood as, by the settle rule, top down, until it stands at its 37 degree
	 * angle of repose: a muck pile, not a wall of muck. A few thousand cells a tick.
	 */
	private void slump() {
		if (slumping == null) {
			slumping = new ArrayList<>(broken);
			slumping.sort((a, b) -> Integer.compare(b.getY(), a.getY()));
			slumpAt = 0;
			slumpMoved = 0;
		}
		int budget = SLUMP_PER_TICK;
		while (budget-- > 0 && slumpAt < slumping.size()) {
			BlockPos p = slumping.get(slumpAt++);
			if (level.getBlockState(p).getBlock() != TailingsBlocks.MUCK || !exposed(p)) {
				continue;
			}
			BlockPos to = Loose.slumpTo(level, p);
			if (to.equals(p)) {
				continue;
			}
			Map<String, Integer> held = level.getBlockEntity(p) instanceof dev.wildcatter.tailings.block.MuckBlockEntity muck ? Map.copyOf(muck.quarters()) : Map.of();
			level.removeBlock(p, false);
			level.setBlock(to, TailingsBlocks.MUCK.defaultBlockState(), Block.UPDATE_ALL);
			if (level.getBlockEntity(to) instanceof dev.wildcatter.tailings.block.MuckBlockEntity muck) {
				muck.set(held);
			}
			slumpMoved++;
			slumped++;
		}
		if (slumpAt >= slumping.size()) {
			slumpPass++;
			if (slumpMoved == 0 || slumpPass >= SLUMP_PASSES) {
				slumpDone = true;
				slumping = null;
			} else {
				slumpAt = 0;
				slumpMoved = 0;
				// the next pass looks at what is muck now in the broken cells (the rest rolled off and rests by the rule already)
			}
		}
	}

	/** A cell with open ground beside or over it: only those can roll. */
	private boolean exposed(BlockPos p) {
		for (Direction d : Direction.values()) {
			if (d != Direction.DOWN && FallingBlock.isFree(level.getBlockState(p.relative(d)))) {
				return true;
			}
		}
		return false;
	}

	private void land(Flight f) {
		BlockPos column = new BlockPos(f.x(), level.getMinY(), f.z());
		if (!level.hasChunkAt(column)) {
			notes.add("flyrock landed in an unloaded chunk at " + f.x() + ", " + f.z() + " and is lost");
			return;
		}
		int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING, f.x(), f.z());
		BlockPos below = new BlockPos(f.x(), top - 1, f.z());
		BlockState hit = level.getBlockState(below);
		// it breaks glass and leaves it lands on, nothing else
		while (top > level.getMinY() && fragile(hit)) {
			level.destroyBlock(below, false);
			top--;
			below = below.below();
			hit = level.getBlockState(below);
		}
		BlockPos at = new BlockPos(f.x(), top, f.z());
		for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, new AABB(at).inflate(1.0, 2.0, 1.0))) {
			living.hurtServer(level, level.damageSources().generic(), (float) Blasting.FLYROCK_DAMAGE);
		}
		Loose.muck(level, at, Map.of(f.id(), 1));
	}

	/** Glass, panes and leaves: what flyrock breaks. */
	private static boolean fragile(BlockState s) {
		return s.is(BlockTags.LEAVES) || s.is(BlockTags.IMPERMEABLE) || BuiltInRegistries.BLOCK.getKey(s.getBlock()).getPath().endsWith("glass_pane");
	}

	/** Dust over the shot, for a while after. */
	public void dust(int ticksSince) {
		if (lastSpill != null && ticksSince % 20 == 0) {
			level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, lastSpill.getX() + 0.5, lastSpill.getY() + 3, lastSpill.getZ() + 0.5, 40, 10, 4, 10, 0.01);
		}
	}

	public double kgFired() {
		return kgFired;
	}

	public boolean harmed() {
		return harmed;
	}
}
