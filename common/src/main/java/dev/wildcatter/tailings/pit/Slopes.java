package dev.wildcatter.tailings.pit;

import com.mojang.serialization.Codec;
import dev.wildcatter.tailings.OptInRules;
import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.MuckBlockEntity;
import dev.wildcatter.tailings.mine.Settle;
import dev.wildcatter.tailings.mine.Slope;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The dragline's highwalls and spoil piles, and their failures (docs/multiblocks/dragline-hand-control.md, Part 2; world rule
 * {@code slope_failures}). Each strip a dragline digs is kept here from its first lift, in 10 m sections along it, so its highwall and its
 * spoil are still checked after it walks on, is set down or is DONE (until 7 game days after DONE). Each section is checked every 10 s
 * while its chunk is loaded: its height, slope and ground from the world, water and blast damage, by {@link Slope}; a slope going bad
 * creeps (cracks on the bench, prisms moving), then fails on its clock, and its wedge runs into the cut as a fast pour.
 *
 * <p>Saved with the server's data, one record for every dimension (each strip names its own).
 */
public final class Slopes extends SavedData {
	/** A section is 10 m along its strip. */
	public static final int SECTION_M = 10;
	/** Checked every 10 s. */
	public static final int CHECK_TICKS = 200;
	/** A new crack block a minute while a slope cracks. */
	public static final int CRACK_TICKS = 1200;
	/** The bench top this far behind the crest is the crest's height. */
	public static final int BENCH_BACK = 3;
	/** A wedge may reach this far behind the dragline's walk line (its strip's frame starts there). */
	public static final int WEDGE_BEHIND = 40;
	/** Water this far behind the failure plane's top wets the wedge. */
	public static final int WATER_BEHIND = 8;
	/** Rain on a section for 10 min wets it, and it stays wet a game day after (proposed). */
	public static final long RAIN_TICKS = 12_000, WET_TICKS = 24_000;
	/** A slide runs at 6 m/s (proposed) and sets over 2 s. */
	public static final int SLIDE_WINDOW = 40, SLIDE_SETTING = 40;
	/** A player or mob caught in a slide (as a truck's hit). */
	public static final float SLIDE_DAMAGE = 8;
	/** A strip is forgotten this long after its job is DONE. */
	public static final long FORGET_TICKS = 7 * 24_000;
	/** The station reads prisms this far off. */
	public static final double STATION_M = 500;
	private static final int[] CRACK_ORDER = {4, 5, 3, 6, 2, 7, 1, 8, 0, 9};
	/** A section's slide reaches this far into the sections either side, its wedge thinning to nothing (spec: the ends taper 2 m). */
	public static final int TAPER_M = 2;
	/** How far through its clock a failing highwall's bench slab moves a metre toward the face, opening its crack (proposed). */
	static final double[] CREEP_STEPS = {0.35, 0.6, 0.8};
	/** A slope cracking or failing is checked this much more often at the world's time scale, down to once a second. */
	static final int FAST_CHECK_TICKS = 20;

	/** One slope's state: a highwall's or a spoil pile's. */
	public static final class Part {
		Slope.Stage stage = Slope.Stage.STABLE;
		double fs = Double.POSITIVE_INFINITY;
		long failAt = -1;
		long clock;
		double mm;
		int cracks;
		long rainSince = -1;
		long wetUntil = -1;
		long lastCrack = -CRACK_TICKS;
		double height;
		double beta;
		/** How many times its bench slab has moved toward the face while failing. */
		int steps;

		public Slope.Stage stage() {
			return stage;
		}

		public double fs() {
			return fs;
		}

		public long failAt() {
			return failAt;
		}

		public int cracks() {
			return cracks;
		}

		public double mm() {
			return mm;
		}

		public double height() {
			return height;
		}

		public double beta() {
			return beta;
		}

		public int steps() {
			return steps;
		}

		boolean cracking() {
			return stage != Slope.Stage.SLID && Slope.cracks(fs);
		}
	}

	public static final class Section {
		final Part wall = new Part(), spoil = new Part();
		boolean damaged;
		long shakeUntil = -1;
		boolean cast;

		public Part wall() {
			return wall;
		}

		public Part spoil() {
			return spoil;
		}

		public boolean damaged() {
			return damaged;
		}
	}

	/** One dragline strip, in the frame its dragline had when it was lifted. */
	public static final class Strip {
		final long id;
		final String dim;
		final double ox, oz;
		final int fx, fz, sx, sz;
		final int floorY;
		int width, length, batter;
		UUID dragline;
		long doneAt = -1;
		final Map<Integer, Section> sections = new TreeMap<>();

		Strip(long id, String dim, double ox, double oz, int fx, int fz, int sx, int sz, int floorY, int width, int length, int batter, UUID dragline) {
			this.id = id;
			this.dim = dim;
			this.ox = ox;
			this.oz = oz;
			this.fx = fx;
			this.fz = fz;
			this.sx = sx;
			this.sz = sz;
			this.floorY = floorY;
			this.width = width;
			this.length = length;
			this.batter = batter;
			this.dragline = dragline;
		}

		public long id() {
			return id;
		}

		public int floorY() {
			return floorY;
		}

		public Section section(int i) {
			return sections.computeIfAbsent(i, k -> new Section());
		}

		public int sections() {
			return (length + SECTION_M - 1) / SECTION_M;
		}

		/** The world column of the cell {@code a} across and {@code j} along (whole metres from the strip's origin). */
		int x(int a, int j) {
			return (int) Math.floor(ox + sx * (a + 0.5) + fx * (j + 0.5));
		}

		int z(int a, int j) {
			return (int) Math.floor(oz + sz * (a + 0.5) + fz * (j + 0.5));
		}

		/** A world point in the strip's frame: {across, along}. */
		double[] frame(double x, double z) {
			double dx = x - ox, dz = z - oz;
			return new double[] {dx * sx + dz * sz, dx * fx + dz * fz};
		}

		/** Where a section's crest is, its middle, at the crest's height. */
		BlockPos crest(int i) {
			int a = (int) Math.floor(Dragline.standoff()) - 1;
			int j = i * SECTION_M + SECTION_M / 2;
			return new BlockPos(x(a, j), floorY, z(a, j));
		}
	}

	private static final Codec<Slopes> CODEC = Codec.STRING.xmap(Slopes::parse, Slopes::text);
	public static final SavedDataType<Slopes> TYPE = new SavedDataType<>(Tailings.id("slopes"), Slopes::new, CODEC, DataFixTypes.LEVEL);

	private final Map<Long, Strip> strips = new LinkedHashMap<>();
	private final Map<String, Set<BlockPos>> prisms = new HashMap<>();
	/** Each dimension's slides on their way, and what found no room. */
	private final Map<String, Pouring> pours = new HashMap<>();
	private long nextId = 1;

	public Slopes() {
	}

	public static Slopes get(MinecraftServer server) {
		return server.getDataStorage().computeIfAbsent(TYPE);
	}

	private static String dim(ServerLevel level) {
		return level.dimension().identifier().toString();
	}

	public static boolean on(ServerLevel level) {
		return OptInRules.on(level.getServer(), OptInRules.SLOPE_FAILURES);
	}

	// ---- what the machines tell it ------------------------------------------------------------------------------------------------

	/**
	 * A dragline lifted to work a strip from where it stands: its strip is kept from now. Returns the strip's id; the same strip again if
	 * it is lifted where it was lifted before.
	 */
	public static long register(ServerLevel level, UUID dragline, double ox, double oz, int fx, int fz, int sx, int sz, int floorY, int width, int length, int batter) {
		Slopes s = get(level.getServer());
		String dim = dim(level);
		for (Strip strip : s.strips.values()) {
			if (strip.dragline.equals(dragline) && strip.dim.equals(dim) && Math.abs(strip.ox - ox) < 0.01 && Math.abs(strip.oz - oz) < 0.01) {
				strip.width = width;
				strip.length = length;
				strip.batter = batter;
				strip.doneAt = -1;
				s.setDirty();
				return strip.id;
			}
		}
		long id = s.nextId++;
		s.strips.put(id, new Strip(id, dim, ox, oz, fx, fz, sx, sz, floorY, width, length, batter, dragline));
		s.setDirty();
		return id;
	}

	/** Its job changed while it runs. */
	public static void job(ServerLevel level, long id, int width, int length, int batter) {
		Strip strip = get(level.getServer()).strips.get(id);
		if (strip != null) {
			strip.width = width;
			strip.length = length;
			strip.batter = batter;
		}
	}

	/** A cast landed at {@code at}: the section beyond is spoil. */
	public static void cast(ServerLevel level, long id, BlockPos at) {
		Slopes s = get(level.getServer());
		Strip strip = s.strips.get(id);
		if (strip == null) {
			return;
		}
		double[] p = strip.frame(at.getX() + 0.5, at.getZ() + 0.5);
		int i = (int) Math.floor(p[1] / SECTION_M);
		if (i >= 0 && i < strip.sections()) {
			strip.section(i).cast = true;
			s.setDirty();
		}
	}

	public static void done(ServerLevel level, long id) {
		Slopes s = get(level.getServer());
		Strip strip = s.strips.get(id);
		if (strip != null && strip.doneAt < 0) {
			strip.doneAt = level.getGameTime();
			s.setDirty();
		}
	}

	public static @Nullable Strip strip(MinecraftServer server, long id) {
		return get(server).strips.get(id);
	}

	/** A hole fired at {@code hole}: a crest within 15 m is damaged for good; within 100 m it is shaken for a game hour. */
	public static void blasted(ServerLevel level, BlockPos hole) {
		Slopes s = get(level.getServer());
		String dim = dim(level);
		for (Strip strip : s.strips.values()) {
			if (!strip.dim.equals(dim)) {
				continue;
			}
			for (int i = 0; i < strip.sections(); i++) {
				BlockPos c = strip.crest(i);
				double d = Math.hypot(c.getX() - hole.getX(), c.getZ() - hole.getZ());
				if (d <= Slope.BLAST_DAMAGE_M + SECTION_M / 2.0) {
					strip.section(i).damaged = true;
					s.setDirty();
				}
				if (d <= Slope.BLAST_SHAKE_M) {
					strip.section(i).shakeUntil = level.getGameTime() + Slope.SHAKE_TICKS;
				}
			}
		}
	}

	/** True when a section of the strip within {@code reach} of {@code x, z} has cracks (its highwall or its spoil). */
	public static boolean cracksNear(ServerLevel level, long id, double x, double z, double reach) {
		Strip strip = get(level.getServer()).strips.get(id);
		if (strip == null || !on(level)) {
			return false;
		}
		for (Map.Entry<Integer, Section> e : strip.sections.entrySet()) {
			BlockPos c = strip.crest(e.getKey());
			if (Math.hypot(c.getX() + 0.5 - x, c.getZ() + 0.5 - z) <= reach && (e.getValue().wall.cracking() || e.getValue().spoil.cracking())) {
				return true;
			}
		}
		return false;
	}

	// ---- prisms ------------------------------------------------------------------------------------------------------------------

	public static void addPrism(ServerLevel level, BlockPos at) {
		Slopes s = get(level.getServer());
		s.prisms.computeIfAbsent(dim(level), k -> new HashSet<>()).add(at.immutable());
		s.setDirty();
	}

	/** The prisms of this dimension that still stand. */
	public static List<BlockPos> prisms(ServerLevel level) {
		Slopes s = get(level.getServer());
		Set<BlockPos> set = s.prisms.getOrDefault(dim(level), Set.of());
		List<BlockPos> out = new ArrayList<>();
		boolean gone = false;
		for (BlockPos p : List.copyOf(set)) {
			if (!level.isLoaded(p)) {
				out.add(p);
			} else if (level.getBlockState(p).is(TailingsBlocks.SLOPE_PRISM)) {
				out.add(p);
			} else {
				set.remove(p);
				gone = true;
			}
		}
		if (gone) {
			s.setDirty();
		}
		return out;
	}

	/** What a prism reads: its section's slope, the way a crew sees it. */
	public record Reading(boolean onSlope, String which, double mmPerHour, double mm, boolean failing, long ticksLeft, Slope.Stage stage, int cracks) {
		public String trend() {
			return Slope.trend(mmPerHour, failing);
		}

		public String describe() {
			if (!onSlope) {
				return "NOT ON A SLOPE: place it on a dragline's highwall bench or spoil";
			}
			return String.format(Locale.ROOT, "%s: moved %.0f mm, %.1f mm/h, %s%s", which, mm, mmPerHour, trend(),
					failing ? String.format(Locale.ROOT, "; slide in about %s", time(ticksLeft)) : "");
		}
	}

	static String time(long ticks) {
		long s = Math.max(0, ticks / 20);
		return s >= 3600 ? String.format(Locale.ROOT, "%.1f h", s / 3600.0) : s >= 60 ? String.format(Locale.ROOT, "%d min", s / 60) : s + " s";
	}

	public static Reading read(ServerLevel level, BlockPos prism) {
		Slopes s = get(level.getServer());
		String dim = dim(level);
		long now = level.getGameTime();
		for (Strip strip : s.strips.values()) {
			if (!strip.dim.equals(dim)) {
				continue;
			}
			double[] p = strip.frame(prism.getX() + 0.5, prism.getZ() + 0.5);
			int i = (int) Math.floor(p[1] / SECTION_M);
			if (i < 0 || i >= strip.sections() || p[0] < 0 || p[0] > Dragline.REACH_M + 2) {
				continue;
			}
			boolean wall = p[0] < Dragline.standoff() + strip.width / 2.0;
			Section sec = strip.section(i);
			Part part = wall ? sec.wall : sec.spoil;
			boolean failing = on(level) && part.stage == Slope.Stage.FAILING && part.failAt >= 0;
			long left = failing ? part.failAt - now : -1;
			double v = on(level) ? Slope.velocity(part.fs, left, part.clock) : 0.05;
			return new Reading(true, (wall ? "highwall" : "spoil") + " section " + (i + 1), v, part.mm, failing, left, part.stage, part.cracks);
		}
		return new Reading(false, "", 0, 0, false, -1, Slope.Stage.STABLE, 0);
	}

	/** Every strip's sections within 200 m of {@code at}, with their factors of safety: the operator's command. */
	public static List<String> describe(ServerLevel level, Vec3 at) {
		List<String> out = new ArrayList<>();
		Slopes s = get(level.getServer());
		out.add("slope_failures is " + (on(level) ? "on" : "off"));
		for (Strip strip : s.strips.values()) {
			if (!strip.dim.equals(dim(level))) {
				continue;
			}
			for (Map.Entry<Integer, Section> e : strip.sections.entrySet()) {
				BlockPos c = strip.crest(e.getKey());
				if (Math.hypot(c.getX() - at.x, c.getZ() - at.z) > 200) {
					continue;
				}
				Section x = e.getValue();
				out.add(String.format(Locale.ROOT, "strip %d section %d at %s: highwall %s FS %.2f (%.0f m at %.0f deg)%s; spoil %s FS %.2f", strip.id, e.getKey() + 1,
						c.toShortString(), x.wall.stage, x.wall.fs, x.wall.height, x.wall.beta, x.damaged ? ", blast damaged" : "", x.spoil.stage, x.spoil.fs));
			}
		}
		return out;
	}

	// ---- the check ---------------------------------------------------------------------------------------------------------------

	/** Each level tick: sections due their check are checked, and slides on their way move on and set. */
	public static void tick(ServerLevel level) {
		Slopes s = get(level.getServer());
		Pouring pour = s.pours.get(dim(level));
		if (!s.runouts.isEmpty()) {
			s.runouts(level);
		}
		if (pour != null && !pour.empty()) {
			PitLoad back = new PitLoad(Long.MAX_VALUE);
			pour.tick(level, back);
			s.setDirty();
			if (!back.empty()) {
				Tailings.LOGGER.warn("Tailings: slide blocks found no room: {}", back.contents());
			}
		}
		if (s.strips.isEmpty() || !on(level)) {
			return;
		}
		long now = level.getGameTime();
		String dim = dim(level);
		List<Long> forget = new ArrayList<>();
		for (Strip strip : List.copyOf(s.strips.values())) {
			if (!strip.dim.equals(dim)) {
				continue;
			}
			if (strip.doneAt >= 0 && now - strip.doneAt > FORGET_TICKS) {
				forget.add(strip.id);
				continue;
			}
			int scale = dev.wildcatter.TimeScale.scale();
			for (int i = 0; i < strip.sections(); i++) {
				Section sec = strip.sections.get(i);
				boolean moving = sec != null && (sec.wall.cracking() || sec.spoil.cracking() || sec.wall.failAt >= 0 || sec.spoil.failAt >= 0);
				int every = moving ? Math.max(FAST_CHECK_TICKS, CHECK_TICKS / Math.max(1, scale)) : CHECK_TICKS;
				if ((now + i * 7 + strip.id * 13) % every != 0) {
					continue;
				}
				BlockPos c = strip.crest(i);
				if (!level.isLoaded(c)) {
					continue;
				}
				s.check(level, strip, i);
			}
		}
		forget.forEach(s.strips::remove);
		if (!forget.isEmpty()) {
			s.setDirty();
		}
	}

	/** Checks one section now (tests call it to skip the wait). */
	public static void checkNow(ServerLevel level, long id, int section) {
		Slopes s = get(level.getServer());
		Strip strip = s.strips.get(id);
		if (strip != null) {
			s.check(level, strip, section);
		}
	}

	/** A column's top: the first solid block down from {@code fromY}, plus one; water, plants and survey prisms are not ground. */
	static int top(ServerLevel level, int x, int z, int fromY, int most) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos(x, fromY, z);
		for (int i = 0; i < most; i++) {
			BlockState state = level.getBlockState(at);
			if (!state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced() && !state.is(TailingsBlocks.SLOPE_PRISM)) {
				return at.getY() + 1;
			}
			at.move(0, -1, 0);
		}
		return at.getY() + 1;
	}

	/** The tops of the cells across the strip at {@code j} along, from 0 to {@code to} across. */
	private int[] profile(ServerLevel level, Strip strip, int j, int to) {
		int[] h = new int[to + 1];
		int from = strip.floorY + Dragline.DIG_ABOVE_M + 4;
		for (int a = 0; a <= to; a++) {
			h[a] = top(level, strip.x(a, j), strip.z(a, j), from, Dragline.DIG_ABOVE_M + Dragline.DIG_DEPTH_M + 30);
		}
		return h;
	}

	/** A highwall's shape on one line across: its crest and toe columns and heights; null where there is no wall. */
	record Wall(int crestCol, int crestY, int toeCol, int toeY) {
		double height() {
			return crestY - toeY;
		}

		double beta() {
			int n = toeCol - crestCol - 1;
			return Slope.angle(height(), n == 0 ? 0 : n + 1);
		}

		/** Where the failure plane at {@code theta} degrees from the toe is, across, at height {@code y}. */
		double planeAcross(double y, double theta) {
			return toeCol - (y - toeY) / Math.tan(Math.toRadians(theta));
		}
	}

	static @Nullable Wall wall(int[] h, int benchCol) {
		if (benchCol < 0 || benchCol >= h.length - 1) {
			return null;
		}
		int crestY = h[benchCol];
		int crest = benchCol;
		while (crest + 1 < h.length) {
			if (h[crest + 1] >= crestY) {
				crest++;
				continue;
			}
			// an open tension crack on the bench, up to 3 m wide, with the bench standing again beyond it: still the bench
			int k = 2;
			while (k <= 4 && crest + k < h.length && h[crest + k] < crestY && h[crest + k - 1] >= crestY - 3) {
				k++;
			}
			if (k <= 4 && crest + k < h.length && h[crest + k] >= crestY && h[crest + k - 1] >= crestY - 3) {
				crest += k;
				continue;
			}
			break;
		}
		if (crest + 1 >= h.length) {
			return null;
		}
		int floor = Integer.MAX_VALUE;
		for (int a = crest + 1; a < h.length; a++) {
			floor = Math.min(floor, h[a]);
		}
		// down the face to the cut's floor: a 1 in 2 face has steps two columns wide
		int toe = crest + 1;
		while (toe + 1 < h.length && h[toe] > floor && h[toe + 1] <= h[toe]) {
			toe++;
		}
		if (crestY - h[toe] < 2) {
			return null;
		}
		return new Wall(crest, crestY, toe, h[toe]);
	}

	private void check(ServerLevel level, Strip strip, int i) {
		Section sec = strip.section(i);
		long now = level.getGameTime();
		int scale = dev.wildcatter.TimeScale.scale();
		checkWall(level, strip, i, sec, now, scale);
		if (sec.cast) {
			checkSpoil(level, strip, i, sec, now, scale);
		}
		setDirty();
	}

	private static boolean wet(ServerLevel level, Part part, BlockPos at, long now) {
		if (level.isRainingAt(at.above())) {
			if (part.rainSince < 0) {
				part.rainSince = now;
			}
			if (now - part.rainSince >= RAIN_TICKS) {
				part.wetUntil = now + WET_TICKS;
			}
		} else {
			part.rainSince = -1;
		}
		return now < part.wetUntil;
	}

	private void checkWall(ServerLevel level, Strip strip, int i, Section sec, long now, int scale) {
		int to = (int) Math.ceil(Dragline.farEdge(strip.width)) + 2;
		int bench = (int) Math.floor(Dragline.standoff()) - BENCH_BACK;
		int mid = i * SECTION_M + SECTION_M / 2;
		Wall w = wall(profile(level, strip, mid, to), bench);
		Part part = sec.wall;
		if (w == null) {
			part.fs = Double.POSITIVE_INFINITY;
			part.height = 0;
			part.stage = Slope.Stage.STABLE;
			part.failAt = -1;
			return;
		}
		// the ground in the wedge, on three lines along the section; the plane first at a dirt's friction, then at the mix's own
		double firstTheta = Slope.planeDegrees(w.beta(), Slope.Ground.DIRT.phi);
		Map<Slope.Ground, Integer> counts = wedgeGround(level, strip, i, w, firstTheta);
		boolean water = water(level, strip, mid, w, firstTheta);
		boolean wet = water | wet(level, part, strip.crest(i), now);
		Slope.Mix mix = Slope.Mix.of(counts, wet, sec.damaged);
		double shake = now < sec.shakeUntil ? Slope.SHAKE_FS : 0;
		double fs = Slope.highwallFs(mix, w.height(), w.beta(), shake);
		part.height = w.height();
		part.beta = w.beta();
		advance(level, strip, i, part, fs, now, scale);
		if (part.stage == Slope.Stage.FAILING && part.failAt >= 0 && now >= part.failAt) {
			slideWall(level, strip, i, Slope.planeDegrees(w.beta(), mix.phi()));
			part.stage = Slope.Stage.SLID;
			part.failAt = -1;
			part.cracks = 0;
			part.steps = 0;
			return;
		}
		if (part.cracking()) {
			double theta = Slope.planeDegrees(w.beta(), mix.phi());
			int behind = Math.max(1, (int) Math.floor(Slope.crackBehind(w.height(), w.beta(), theta)));
			int crackCol = Math.max(-WEDGE_BEHIND, w.crestCol() - behind);
			crack(level, strip, i, part, crackCol, w.crestY(), now, scale);
			ravel(level, strip, i, w);
			if (part.stage == Slope.Stage.FAILING && part.failAt >= 0 && part.clock > 0) {
				double through = 1 - (part.failAt - now) / (double) part.clock;
				if (part.steps < CREEP_STEPS.length && through >= CREEP_STEPS[part.steps]) {
					creepStep(level, strip, i, w, crackCol);
					part.steps++;
				}
			}
		}
	}

	/**
	 * A failing highwall moves: the slab of bench between its crack and its crest slides a metre toward the face, with whatever stands
	 * on it (its prisms), over about half a second; the crack gapes a metre wider behind it, and the block at the crest goes down the
	 * face. Nothing is lost: each block is let go and set again through the slide's pour.
	 */
	private void creepStep(ServerLevel level, Strip strip, int i, Wall w, int crackCol) {
		Pouring pour = pour(level);
		long now = level.getGameTime();
		List<Pouring.Slid> drawn = new ArrayList<>();
		for (int k = 0; k < SECTION_M; k++) {
			int j = i * SECTION_M + k;
			int[] h = profile(level, strip, j, w.toeCol() + 2);
			Wall line = wall(h, (int) Math.floor(Dragline.standoff()) - BENCH_BACK);
			if (line == null) {
				continue;
			}
			int top = line.crestY() - 1;
			int from = Math.max(crackCol + 1, -WEDGE_BEHIND + 1);
			// the crest's top block, and what stands on it, go down the face
			PitLoad ravel = new PitLoad(Long.MAX_VALUE);
			List<Pouring.SlideFrom> ravelFrom = new ArrayList<>();
			Vec3 foot = new Vec3(strip.x(line.toeCol(), j) + 0.5, line.toeY() + 0.5, strip.z(line.toeCol(), j) + 0.5);
			for (int y = top; y <= top + 1; y++) {
				BlockPos at = new BlockPos(strip.x(line.crestCol(), j), y, strip.z(line.crestCol(), j));
				if (level.getBlockState(at).is(TailingsBlocks.SLOPE_PRISM)) {
					continue;   // a prism at the very crest rides on; the scene's crews set them back from it
				}
				if (take(level, at, ravel)) {
					ravelFrom.add(new Pouring.SlideFrom(at, foot, now));
				}
			}
			// the rest of the slab, a metre on, from the crest end back
			for (int a = line.crestCol() - 1; a >= from; a--) {
				for (int y = top; y <= top + 1; y++) {
					BlockPos at = new BlockPos(strip.x(a, j), y, strip.z(a, j));
					BlockPos to = new BlockPos(strip.x(a + 1, j), y, strip.z(a + 1, j));
					boolean prism = level.getBlockState(at).is(TailingsBlocks.SLOPE_PRISM);
					Pouring.Slid moved = pour.shift(level, at, to, now + (line.crestCol() - a) / 3);
					if (moved != null) {
						drawn.add(moved);
						if (prism) {
							movePrism(level, at, to);
						}
					}
				}
			}
			if (!ravel.empty()) {
				// it bounces off the foot of the face and comes to rest a few metres out, clear of the toe
				BlockPos start = new BlockPos(strip.x(line.toeCol() + 3, j), line.toeY(), strip.z(line.toeCol() + 3, j));
				drawn.addAll(pour.slide(level, ravel, start, Settle.MOST_RISE_37, ravelFrom));
				dropLeft(level, ravel, Vec3.atCenterOf(start));
			}
		}
		if (!drawn.isEmpty()) {
			dev.wildcatter.tailings.entity.SlideEntity.show(level, drawn);
		}
		BlockPos c = strip.crest(i);
		level.playSound(null, c, dev.wildcatter.tailings.TailingsSounds.HIGHWALL_CREAK, SoundSource.BLOCKS, 4F, 0.6F);
		level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.DIRT.defaultBlockState()), c.getX() + 0.5, c.getY() + 0.5, c.getZ() + 0.5, 30,
				SECTION_M / 3.0, 0.3, SECTION_M / 3.0, 0);
		Tailings.LOGGER.info("Tailings: highwall section {} of strip {} moved a metre toward its face", i + 1, strip.id);
		setDirty();
	}

	/** A prism carried on a moving slab: the record follows it to its new cell. */
	private void movePrism(ServerLevel level, BlockPos from, BlockPos to) {
		Set<BlockPos> set = prisms.get(dim(level));
		if (set != null && set.remove(from)) {
			set.add(to.immutable());
		}
	}

	private static Slope.Ground ground(BlockState state) {
		String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
		return Slope.Ground.of(id, PitMass.soft(PitMass.dugAs(id)));
	}

	private static boolean solid(BlockState state) {
		return !state.isAir() && state.getFluidState().isEmpty() && !state.canBeReplaced();
	}

	private Map<Slope.Ground, Integer> wedgeGround(ServerLevel level, Strip strip, int i, Wall w, double theta) {
		Map<Slope.Ground, Integer> counts = new EnumMap<>(Slope.Ground.class);
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int j : new int[] {i * SECTION_M + 1, i * SECTION_M + SECTION_M / 2, i * SECTION_M + SECTION_M - 2}) {
			for (int y = w.toeY(); y < w.crestY(); y++) {
				int from = (int) Math.floor(w.planeAcross(y + 0.5, theta));
				for (int a = Math.max(-WEDGE_BEHIND, from); a < w.toeCol(); a++) {
					if (a + 0.5 <= w.planeAcross(y + 0.5, theta)) {
						continue;
					}
					BlockState state = level.getBlockState(at.set(strip.x(a, j), y, strip.z(a, j)));
					if (solid(state)) {
						counts.merge(ground(state), 1, Integer::sum);
					}
				}
			}
		}
		return counts;
	}

	private boolean water(ServerLevel level, Strip strip, int j, Wall w, double theta) {
		int from = Math.max(-WEDGE_BEHIND, (int) Math.floor(w.planeAcross(w.crestY(), theta)) - WATER_BEHIND);
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int a = from; a <= w.toeCol(); a++) {
			for (int y = w.toeY(); y <= w.crestY(); y++) {
				if (level.getFluidState(at.set(strip.x(a, j), y, strip.z(a, j))).is(net.minecraft.tags.FluidTags.WATER)) {
					return true;
				}
			}
		}
		return false;
	}

	/** A slope's stage, its clock and its prisms' movement, from its new factor of safety. */
	private void advance(ServerLevel level, Strip strip, int i, Part part, double fs, long now, int scale) {
		part.fs = fs;
		Slope.Stage stage = Slope.stage(fs);
		if (stage == Slope.Stage.FAILING) {
			long clock = Slope.clockTicks(fs, scale);
			if (part.failAt < 0) {
				part.failAt = now + clock;
				part.clock = clock;
			} else if (now + clock < part.failAt) {
				part.failAt = now + clock;
			}
		} else {
			part.failAt = -1;
			part.steps = 0;
		}
		part.stage = stage;
		long left = part.failAt < 0 ? -1 : part.failAt - now;
		part.mm += Slope.velocity(fs, left, part.clock) * CHECK_TICKS / 72_000.0;
	}

	/** A crack block more along the line where the failure plane meets the bench, a minute at a time; on rock, dust only. */
	private void crack(ServerLevel level, Strip strip, int i, Part part, int a, int crestY, long now, int scale) {
		// a block a minute (at the world's time scale); failing, as fast as its clock runs out
		int due = part.cracks < CRACK_ORDER.length && now - part.lastCrack >= CRACK_TICKS / Math.max(1, scale) ? part.cracks + 1 : part.cracks;
		if (part.stage == Slope.Stage.FAILING && part.failAt >= 0 && part.clock > 0) {
			due = Math.max(due, (int) Math.ceil(CRACK_ORDER.length * Math.min(1, 1 - (part.failAt - now) / (double) part.clock)));
		}
		while (part.cracks < Math.min(CRACK_ORDER.length, due)) {
			int j = i * SECTION_M + CRACK_ORDER[part.cracks];
			BlockPos at = new BlockPos(strip.x(a, j), crestY - 1, strip.z(a, j));
			BlockState state = level.getBlockState(at);
			Slope.Ground g = ground(state);
			if (solid(state) && !state.hasBlockEntity() && g != Slope.Ground.ROCK && g != Slope.Ground.SPOIL) {
				level.setBlock(at, TailingsBlocks.TENSION_CRACK.defaultBlockState(), Block.UPDATE_ALL);
			}
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state.isAir() ? Blocks.DIRT.defaultBlockState() : state), at.getX() + 0.5, at.getY() + 1.1, at.getZ() + 0.5,
					8, 0.4, 0.05, 0.4, 0.02);
			part.cracks++;
			part.lastCrack = now;
		}
		BlockPos c = strip.crest(i);
		level.playSound(null, c, dev.wildcatter.tailings.TailingsSounds.HIGHWALL_CREAK, SoundSource.BLOCKS, 2.5F, 0.8F + level.getRandom().nextFloat() * 0.3F);
	}

	/** Loose ground ravels off the face: dust and grit down the wall. */
	private void ravel(ServerLevel level, Strip strip, int i, Wall w) {
		int j = i * SECTION_M + level.getRandom().nextInt(SECTION_M);
		int a = w.crestCol() + 1;
		double x = strip.x(a, j) + 0.5, z = strip.z(a, j) + 0.5;
		level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.DIRT.defaultBlockState()), x, w.crestY() - 0.5, z, 12, 0.4, w.height() / 3, 0.4, 0);
		level.playSound(null, BlockPos.containing(x, w.crestY() - 1, z), dev.wildcatter.tailings.TailingsSounds.RAVEL, SoundSource.BLOCKS, 1.5F, 1F);
	}

	private void checkSpoil(ServerLevel level, Strip strip, int i, Section sec, long now, int scale) {
		int from = (int) Math.floor(Dragline.standoff());
		int to = (int) Math.ceil(Dragline.REACH_M);
		int mid = i * SECTION_M + SECTION_M / 2;
		int[] h = profile(level, strip, mid, to);
		Part part = sec.spoil;
		Pile pile = pile(h, from, (int) Math.floor(Dragline.farEdge(strip.width)));
		if (pile == null) {
			part.fs = Double.POSITIVE_INFINITY;
			part.stage = Slope.Stage.STABLE;
			part.failAt = -1;
			return;
		}
		boolean weak = false, wetToe = false;
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int a = Math.max(0, pile.toeCol() - 2); a <= Math.min(to, pile.toeCol() + 10); a++) {
			for (int y : new int[] {pile.toeY() - 1, pile.base() - 1}) {
				BlockState state = level.getBlockState(at.set(strip.x(a, mid), y, strip.z(a, mid)));
				if (state.is(Blocks.CLAY) || state.is(Blocks.MUD)) {
					weak = true;
				}
			}
		}
		for (int a = Math.max(0, pile.toeCol() - 3); a <= Math.min(to, pile.toeCol() + 2); a++) {
			for (int y = pile.base() - 1; y <= pile.toeY() + 2; y++) {
				if (level.getFluidState(at.set(strip.x(a, mid), y, strip.z(a, mid))).is(net.minecraft.tags.FluidTags.WATER)) {
					wetToe = true;
				}
			}
		}
		wetToe |= wet(level, part, new BlockPos(strip.x(pile.crestCol(), mid), pile.crestY(), strip.z(pile.crestCol(), mid)), now);
		double height = pile.crestY() - pile.base();
		double fs = Slope.spoilFs(height, weak, wetToe);
		part.height = height;
		advance(level, strip, i, part, fs, now, scale);
		if (part.stage == Slope.Stage.FAILING && part.failAt >= 0 && now >= part.failAt) {
			slideSpoil(level, strip, i);
			part.stage = Slope.Stage.SLID;
			part.failAt = -1;
			part.cracks = 0;
			part.steps = 0;
		} else if (part.cracking()) {
			double x = strip.x(pile.crestCol(), mid) + 0.5, z = strip.z(pile.crestCol(), mid) + 0.5;
			level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, Blocks.DIRT.defaultBlockState()), x, pile.crestY() + 0.2, z, 16, 3, 0.1, 3, 0);
			part.cracks = Math.min(CRACK_ORDER.length, part.cracks + 1);
			level.playSound(null, BlockPos.containing(x, pile.crestY(), z), dev.wildcatter.tailings.TailingsSounds.HIGHWALL_CREAK, SoundSource.BLOCKS, 2F, 0.9F);
		}
	}

	/** A spoil pile's shape on one line: its crest, its toe toward the strip and the lowest ground there. */
	record Pile(int crestCol, int crestY, int toeCol, int toeY, int base) {
	}

	static @Nullable Pile pile(int[] h, int from, int farEdge) {
		int crest = -1;
		for (int a = Math.max(from, farEdge); a < h.length; a++) {
			if (crest < 0 || h[a] > h[crest]) {
				crest = a;
			}
		}
		if (crest < 0) {
			return null;
		}
		int base = Integer.MAX_VALUE;
		for (int a = from; a <= crest; a++) {
			base = Math.min(base, h[a]);
		}
		// down its face to the ground at its toe: a 37 degree face has steps of one and two columns
		int toe = crest;
		while (toe - 1 >= from && h[toe] > base && h[toe - 1] <= h[toe]) {
			toe--;
		}
		if (h[crest] - base < 3 || toe == crest) {
			return null;
		}
		return new Pile(crest, h[crest], toe, h[toe], base);
	}

	// ---- the slide ---------------------------------------------------------------------------------------------------------------

	private Pouring pour(ServerLevel level) {
		return pours.computeIfAbsent(dim(level), k -> new Pouring());
	}

	/** One block of a slide into {@code load}, its cell left empty: soil as itself, rock and ore as bank (they land as muck), muck by its quarters. */
	private static boolean take(ServerLevel level, BlockPos at, PitLoad load) {
		BlockState state = level.getBlockState(at);
		if (!solid(state)) {
			return false;
		}
		if (state.is(TailingsBlocks.MUCK) && level.getBlockEntity(at) instanceof MuckBlockEntity muck) {
			muck.kilograms().forEach(load::add);
		} else if (state.hasBlockEntity()) {
			return false;   // never a machine's block
		} else {
			String id = PitMass.dugAs(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString());
			if (state.is(TailingsBlocks.TENSION_CRACK)) {
				id = "minecraft:dirt";
			}
			load.add(id, PitMass.kg(id));
		}
		level.setBlock(at, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
		return true;
	}

	/** How deep the wedge reaches on line {@code k} of a section (0 to 9; -2, -1 and 10, 11 are the tapers into its neighbours). */
	static double taper(int k) {
		if (k < 0) {
			return (k + TAPER_M + 1) / (TAPER_M + 1.0);
		}
		if (k >= SECTION_M) {
			return (SECTION_M + TAPER_M - k) / (TAPER_M + 1.0);
		}
		return 1;
	}

	/**
	 * When a block of the wedge lets go: the toe and the face first, the top and the back of the wedge after, and the middle of the
	 * section before its ends, so the slide starts at its foot and middle and pulls the rest after it, within about 1.5 s.
	 */
	private static long launch(long now, double up, double back, int k) {
		return now + Math.round(0.45 * up + 0.3 * back + 1.2 * Math.abs(k - (SECTION_M - 1) / 2.0));
	}

	/**
	 * The highwall's wedge in section {@code i} runs into the cut, line by line along the section and 2 m into each neighbour, thinning:
	 * each block slides from where it stood down the failure plane and out over the cut's floor to its cell, at the slide's run.
	 */
	public void slideWall(ServerLevel level, Strip strip, int i, double theta) {
		int to = (int) Math.ceil(Dragline.farEdge(strip.width)) + 2;
		int bench = (int) Math.floor(Dragline.standoff()) - BENCH_BACK;
		Pouring pour = pour(level);
		long now = level.getGameTime();
		double tan = Math.tan(Math.toRadians(theta));
		AABB caught = null;
		int moved = 0;
		List<Pouring.Slid> slid = new ArrayList<>();
		for (int k = -TAPER_M; k < SECTION_M + TAPER_M; k++) {
			int j = i * SECTION_M + k;
			Wall w = wall(profile(level, strip, j, to), bench);
			if (w == null) {
				continue;
			}
			double depth = taper(k);
			PitLoad load = new PitLoad(Long.MAX_VALUE);
			List<Pouring.SlideFrom> from = new ArrayList<>();
			for (int y = w.crestY() - 1; y >= w.toeY(); y--) {
				double reach = depth * (w.toeCol() - w.planeAcross(y + 0.5, theta));
				for (int a = Math.max(-WEDGE_BEHIND, (int) Math.floor(w.toeCol() - reach)); a < w.toeCol(); a++) {
					if (w.toeCol() - (a + 0.5) >= reach) {
						continue;
					}
					BlockPos at = new BlockPos(strip.x(a, j), y, strip.z(a, j));
					if (take(level, at, load)) {
						moved++;
						// down the plane to the foot of the face, then out over the floor
						double viaY = Math.max(w.toeY() + 0.5, y + 0.5 - (w.toeCol() - (a + 0.5)) * tan);
						Vec3 via = new Vec3(strip.x(w.toeCol(), j) + 0.5, viaY, strip.z(w.toeCol(), j) + 0.5);
						from.add(new Pouring.SlideFrom(at, via, launch(now, y - w.toeY(), w.toeCol() - 1 - a, k)));
					}
				}
			}
			if (from.isEmpty()) {
				continue;
			}
			BlockPos start = new BlockPos(strip.x(w.toeCol() - 1, j), w.toeY() + (int) (w.height() / 2), strip.z(w.toeCol() - 1, j));
			Vec3 land = new Vec3(strip.x(w.toeCol(), j) + 0.5, w.toeY(), strip.z(w.toeCol(), j) + 0.5);
			slid.addAll(pour.slide(level, load, start, Settle.MOST_RISE_37, from));
			dropLeft(level, load, land);
			AABB box = new AABB(strip.x((int) Math.floor(w.planeAcross(w.crestY(), theta)) - 1, j), w.toeY() - 1, strip.z((int) Math.floor(w.planeAcross(w.crestY(), theta)) - 1, j),
					strip.x(w.toeCol() + (int) w.height(), j) + 1, w.crestY() + 3, strip.z(w.toeCol() + (int) w.height(), j) + 1);
			caught = caught == null ? box : caught.minmax(box);
		}
		Tailings.LOGGER.info("Tailings: highwall section {} of strip {} slid: {} blocks", i + 1, strip.id, moved);
		afterSlide(level, strip, i, caught, slid);
	}

	/** The spoil pile's face in section {@code i} slumps to about 1 in 2, by the wet settle rule, and 2 m into each neighbour, thinning. */
	public void slideSpoil(ServerLevel level, Strip strip, int i) {
		int from0 = (int) Math.floor(Dragline.standoff());
		int to = (int) Math.ceil(Dragline.REACH_M);
		Pouring pour = pour(level);
		long now = level.getGameTime();
		AABB caught = null;
		int moved = 0;
		List<Pouring.Slid> slid = new ArrayList<>();
		for (int k = -TAPER_M; k < SECTION_M + TAPER_M; k++) {
			int j = i * SECTION_M + k;
			int[] h = profile(level, strip, j, to);
			Pile p = pile(h, from0, (int) Math.floor(Dragline.farEdge(strip.width)));
			if (p == null) {
				continue;
			}
			double depth = taper(k);
			PitLoad load = new PitLoad(Long.MAX_VALUE);
			List<Pouring.SlideFrom> from = new ArrayList<>();
			int topA = p.toeCol();
			for (int a = p.toeCol() + 1; a <= to; a++) {
				double plane = p.toeY() + (a - p.toeCol()) / 2.0;
				double line = h[a] - depth * (h[a] - plane);   // the taper takes less off the top
				for (int y = h[a] - 1; y >= Math.ceil(line); y--) {
					BlockPos at = new BlockPos(strip.x(a, j), y, strip.z(a, j));
					if (take(level, at, load)) {
						moved++;
						topA = a;
						double viaY = Math.max(p.toeY() + 0.5, y + 0.5 - (a - p.toeCol()) * 0.5);
						Vec3 via = new Vec3(strip.x(p.toeCol(), j) + 0.5, viaY, strip.z(p.toeCol(), j) + 0.5);
						from.add(new Pouring.SlideFrom(at, via, launch(now, y - p.toeY(), a - p.toeCol(), k)));
					}
				}
			}
			if (from.isEmpty()) {
				continue;
			}
			int a0 = p.toeCol() + Math.max(1, (topA - p.toeCol()) / 3);
			BlockPos start = new BlockPos(strip.x(a0, j), (int) Math.ceil(p.toeY() + (a0 - p.toeCol()) / 2.0) + 1, strip.z(a0, j));
			Vec3 land = new Vec3(strip.x(p.toeCol(), j) + 0.5, p.toeY(), strip.z(p.toeCol(), j) + 0.5);
			slid.addAll(pour.slide(level, load, start, Settle.WET_RISE, from));
			dropLeft(level, load, land);
			AABB box = new AABB(strip.x(p.toeCol() - (int) (p.crestY() - p.base()), j), p.base() - 1, strip.z(p.toeCol() - (int) (p.crestY() - p.base()), j),
					strip.x(topA + 1, j) + 1, p.crestY() + 3, strip.z(topA + 1, j) + 1);
			caught = caught == null ? box : caught.minmax(box);
		}
		Tailings.LOGGER.info("Tailings: spoil section {} of strip {} slid: {} blocks", i + 1, strip.id, moved);
		afterSlide(level, strip, i, caught, slid);
	}

	/** What found no room in the cut drops as items where the slide lands: nothing is lost. */
	private static void dropLeft(ServerLevel level, PitLoad load, Vec3 at) {
		for (Map.Entry<String, Integer> e : load.wholeBlocks().entrySet()) {
			for (int left = e.getValue(); left > 0; left -= 64) {
				net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(level, at.x, at.y + 1, at.z,
						dev.wildcatter.tailings.block.Feeds.stack(e.getKey(), Math.min(64, left)));
				level.addFreshEntity(item);
			}
		}
	}

	/**
	 * A slide's run-out on its way: its blocks' cells and when each arrives, so the machines in its way are caught as it reaches them (a
	 * truck buried or pushed and BLOCKED, a dragline's bucket buried). Kept while it runs, not saved.
	 */
	private static final class Runout {
		final String dim;
		final List<Pouring.Slid> slid;
		final long[] arrive;
		final AABB box;
		final long end;
		final Set<UUID> caught = new HashSet<>();

		Runout(String dim, List<Pouring.Slid> slid, AABB box) {
			this.dim = dim;
			this.slid = slid;
			this.arrive = new long[slid.size()];
			long last = 0;
			for (int n = 0; n < slid.size(); n++) {
				Pouring.Slid s = slid.get(n);
				arrive[n] = s.launch() + SlideMotion.ticks(Vec3.atCenterOf(s.from()).distanceTo(s.via()) + s.via().distanceTo(Vec3.atCenterOf(s.to())));
				last = Math.max(last, arrive[n]);
			}
			this.box = box;
			this.end = last + 40;
		}
	}

	private final List<Runout> runouts = new ArrayList<>();

	/** Every other tick: the machines each slide's run-out has reached are caught. */
	private void runouts(ServerLevel level) {
		long now = level.getGameTime();
		String dim = dim(level);
		for (java.util.Iterator<Runout> it = runouts.iterator(); it.hasNext();) {
			Runout r = it.next();
			if (!r.dim.equals(dim)) {
				continue;
			}
			if (now > r.end) {
				it.remove();
			}
			if (now % 2 != 0) {
				continue;
			}
			Set<BlockPos> arrived = new HashSet<>();
			for (int n = 0; n < r.slid.size(); n++) {
				if (r.arrive[n] <= now) {
					arrived.add(r.slid.get(n).to());
				}
			}
			for (dev.wildcatter.tailings.entity.PitVehicleEntity v : level.getEntitiesOfClass(dev.wildcatter.tailings.entity.PitVehicleEntity.class, r.box)) {
				if (v instanceof dev.wildcatter.tailings.entity.DraglineEntity d) {
					d.slideOver(level, arrived);
					continue;
				}
				if (r.caught.contains(v.getUUID())) {
					continue;
				}
				AABB b = v.getBoundingBox().inflate(0.4, 0, 0.4);
				boolean hit = false;
				int top = Integer.MIN_VALUE;
				double px = 0, pz = 0;
				for (int n = 0; n < r.slid.size(); n++) {
					Pouring.Slid s = r.slid.get(n);
					BlockPos c = s.to();
					if (c.getX() + 1 <= b.minX || c.getX() >= b.maxX || c.getZ() + 1 <= b.minZ || c.getZ() >= b.maxZ || c.getY() + 1 < b.minY - 0.5) {
						continue;
					}
					top = Math.max(top, c.getY() + 1);
					if (r.arrive[n] <= now) {
						hit = true;
					}
					px += c.getX() + 0.5 - s.via().x;
					pz += c.getZ() + 0.5 - s.via().z;
				}
				if (!hit) {
					continue;
				}
				r.caught.add(v.getUUID());
				boolean buried = top - b.minY >= v.getBbHeight() / 2;
				double pl = Math.hypot(px, pz);
				Vec3 push = pl < 1e-6 ? Vec3.ZERO : new Vec3(px / pl * 2, 0, pz / pl * 2);
				v.caughtBySlide(level, push, top, buried);
			}
		}
	}

	/** The rumble, dust, and what the slide catches: players and mobs hurt, the dragline's ground checked, its blocks drawn on their way. */
	private void afterSlide(ServerLevel level, Strip strip, int i, @Nullable AABB caught, List<Pouring.Slid> slid) {
		if (!slid.isEmpty()) {
			dev.wildcatter.tailings.entity.SlideEntity.show(level, slid);
			if (caught != null) {
				runouts.add(new Runout(dim(level), slid, caught.inflate(4, 2, 4)));
			}
		}
		BlockPos c = strip.crest(i);
		level.playSound(null, c, dev.wildcatter.tailings.TailingsSounds.HIGHWALL_SLIDE, SoundSource.BLOCKS, 12F, 0.8F);
		level.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, c.getX() + 0.5, c.getY() + 1, c.getZ() + 0.5, 30, 6, 2, 6, 0.01);
		if (caught != null) {
			for (LivingEntity living : level.getEntitiesOfClass(LivingEntity.class, caught)) {
				living.hurtServer(level, level.damageSources().fallingBlock(null), SLIDE_DAMAGE);
			}
		}
		for (dev.wildcatter.tailings.entity.DraglineEntity d : level.getEntitiesOfClass(dev.wildcatter.tailings.entity.DraglineEntity.class, new AABB(c).inflate(120))) {
			d.groundCheck(level);
		}
		setDirty();
	}

	/** Blocks of slides still on their way in this dimension (tests). */
	public static int pending(ServerLevel level) {
		Pouring pour = get(level.getServer()).pours.get(dim(level));
		return pour == null ? 0 : pour.size();
	}

	/** Test hook: this data written and read back, as a save and load would. */
	public static Slopes roundTrip(MinecraftServer server) {
		return parse(get(server).text());
	}

	public @Nullable Strip stripOf(long id) {
		return strips.get(id);
	}

	// ---- saving ------------------------------------------------------------------------------------------------------------------

	private String text() {
		StringBuilder out = new StringBuilder("v1\n").append(nextId).append('\n');
		for (Strip s : strips.values()) {
			out.append("S;").append(s.id).append(';').append(s.dim).append(';').append(s.ox).append(';').append(s.oz).append(';').append(s.fx).append(';').append(s.fz)
					.append(';').append(s.sx).append(';').append(s.sz).append(';').append(s.floorY).append(';').append(s.width).append(';').append(s.length).append(';')
					.append(s.batter).append(';').append(s.dragline).append(';').append(s.doneAt).append('\n');
			for (Map.Entry<Integer, Section> e : s.sections.entrySet()) {
				Section x = e.getValue();
				out.append("C;").append(e.getKey()).append(';').append(x.damaged).append(';').append(x.shakeUntil).append(';').append(x.cast).append(';').append(part(x.wall))
						.append(';').append(part(x.spoil)).append('\n');
			}
		}
		prisms.forEach((dim, set) -> set.forEach(p -> out.append("P;").append(dim).append(';').append(p.asLong()).append('\n')));
		pours.forEach((dim, pour) -> {
			if (!pour.empty()) {
				out.append("O;").append(dim).append(';').append(java.util.Base64.getEncoder().encodeToString(pour.save().getBytes(java.nio.charset.StandardCharsets.UTF_8))).append('\n');
			}
		});
		return out.toString();
	}

	private static String part(Part p) {
		return p.stage.name() + ',' + p.fs + ',' + p.failAt + ',' + p.clock + ',' + p.mm + ',' + p.cracks + ',' + p.rainSince + ',' + p.wetUntil + ',' + p.lastCrack + ',' + p.steps;
	}

	private static void part(Part p, String text) {
		String[] f = text.split(",");
		p.stage = Slope.Stage.valueOf(f[0]);
		p.fs = Double.parseDouble(f[1]);
		p.failAt = Long.parseLong(f[2]);
		p.clock = Long.parseLong(f[3]);
		p.mm = Double.parseDouble(f[4]);
		p.cracks = Integer.parseInt(f[5]);
		p.rainSince = Long.parseLong(f[6]);
		p.wetUntil = Long.parseLong(f[7]);
		p.lastCrack = Long.parseLong(f[8]);
		p.steps = f.length > 9 ? Integer.parseInt(f[9]) : 0;
	}

	private static Slopes parse(String text) {
		Slopes s = new Slopes();
		Strip strip = null;
		for (String line : text.split("\n")) {
			try {
				String[] f = line.split(";");
				switch (f[0]) {
					case "S" -> {
						strip = new Strip(Long.parseLong(f[1]), f[2], Double.parseDouble(f[3]), Double.parseDouble(f[4]), Integer.parseInt(f[5]), Integer.parseInt(f[6]),
								Integer.parseInt(f[7]), Integer.parseInt(f[8]), Integer.parseInt(f[9]), Integer.parseInt(f[10]), Integer.parseInt(f[11]), Integer.parseInt(f[12]),
								UUID.fromString(f[13]));
						strip.doneAt = Long.parseLong(f[14]);
						s.strips.put(strip.id, strip);
					}
					case "C" -> {
						if (strip != null) {
							Section x = strip.section(Integer.parseInt(f[1]));
							x.damaged = Boolean.parseBoolean(f[2]);
							x.shakeUntil = Long.parseLong(f[3]);
							x.cast = Boolean.parseBoolean(f[4]);
							part(x.wall, f[5]);
							part(x.spoil, f[6]);
						}
					}
					case "P" -> s.prisms.computeIfAbsent(f[1], k -> new HashSet<>()).add(BlockPos.of(Long.parseLong(f[2])));
					case "O" -> s.pours.computeIfAbsent(f[1], k -> new Pouring()).load(new String(java.util.Base64.getDecoder().decode(f[2]), java.nio.charset.StandardCharsets.UTF_8));
					default -> {
						if (!line.startsWith("v") && !line.isBlank() && strip == null) {
							s.nextId = Math.max(s.nextId, Long.parseLong(line.trim()));
						}
					}
				}
			} catch (RuntimeException e) {
				Tailings.LOGGER.warn("Tailings: a saved slope line could not be read and was dropped: {}", line);
			}
		}
		return s;
	}
}
