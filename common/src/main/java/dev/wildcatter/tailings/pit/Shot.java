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

	private final ServerLevel level;
	private final List<Hole> holes;
	private final Set<BlockPos> broken = new HashSet<>();
	private final Set<BlockPos> fired = new HashSet<>();
	private final Map<String, Integer> swell = new LinkedHashMap<>();
	private final List<Flight> flights = new ArrayList<>();
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

	public List<String> notes() {
		return notes;
	}

	public double tonnes() {
		return bank * PitMass.ROCK_KG / 1000.0;
	}

	public boolean done() {
		return next >= holes.size() && flights.isEmpty();
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
			for (Entity e : level.getEntities((Entity) null, box.aabb().inflate(0, 1, 0), e -> !(e instanceof LivingEntity))) {
				String ns = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).getNamespace();
				if (ns.equals("tailings") || ns.equals("wildcatter")) {
					return "MACHINE IN THE SHOT: " + e.getType().getDescription().getString() + ": move it";
				}
			}
		}
		return null;
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
		BlockPos spill = box.face() == null ? collar.above() : collar.relative(box.face(), hole.burden()).above();
		lastSpill = spill;
		spillWhole(spill);
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
		for (int i = 0; i < throwN; i++) {
			throwRock(collar, toward, rock);
		}
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
	private void throwRock(BlockPos from, @Nullable Direction toward, String rock) {
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
		flights.add(new Flight(tick + Blasting.flightTicks(range), x, z, id));
		flyrock++;
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
