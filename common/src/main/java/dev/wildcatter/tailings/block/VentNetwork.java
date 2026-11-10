package dev.wildcatter.tailings.block;

import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.mine.Airflow;
import dev.wildcatter.tailings.mine.VentRules;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.platform.Platform;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

/**
 * The drive's air in the world (docs/multiblocks/ventilation.md): each ventilation fan reads its duct line once a second and says here
 * where it ends and the air it gives there. The bore is fresh from the portal to the duct's end plus 30 m, and beside the duct; a TBM's
 * back-up, a player or a gas detector asks {@link #airAt}. Players in a drive's stale air are counted here each second, and suffer only
 * where {@code config/tailings-ventilation.properties} turns stale air on ({@link VentRules}).
 */
public final class VentNetwork {
	/** A fan's line as last read: where it ends, its length, its air, and when it was read. */
	public record Run(BlockPos fan, @Nullable BlockPos end, Airflow.Result air, long tick, Set<Long> cells) {
	}

	/** The air at a place: the best line's air there (0 with none), and that line. */
	public record Air(double flow, @Nullable Run run) {
		public boolean fresh() {
			return flow + 1e-9 >= Airflow.NEED;
		}
	}

	/** Longest line a fan reads: 10 km of duct. */
	private static final int MOST_CELLS = 10_000;
	/** A line not read for this long is gone, unless its fan's chunk is not loaded. */
	private static final long STALE_TICKS = 60;
	private static final Map<Level, Map<Long, Run>> RUNS = new WeakHashMap<>();
	private static final Map<Level, Map<Long, Long>> CELLS = new WeakHashMap<>();
	/** Seconds each player has spent in stale air, and in fresh air since. Not saved. */
	private static final Map<UUID, int[]> PLAYERS = new HashMap<>();
	/** Players a game test makes feel stale air whatever the config says. */
	public static final Set<UUID> TEST_STALE = new HashSet<>();

	private VentNetwork() {
	}

	// ---- a fan reads its line ---------------------------------------------------------------------------------------------------

	/**
	 * Follows the duct out of the fan's south face, through any booster fans in it, to its far end, and works out the air. A cell in a
	 * chunk that is not loaded is taken as it was last read. At a branch the farther end is the line's end; the rest is not counted.
	 */
	public static void read(ServerLevel level, PlantBlockEntity fan, VentState state) {
		Set<Long> wasCells = new HashSet<>(state.cells);
		Map<Long, Long> wasBoosters = new HashMap<>(state.boosterCells);
		state.cells.clear();
		state.boosterCells.clear();
		state.fault = "";
		Map<Long, Integer> distance = new HashMap<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		for (dev.wildcatter.block.Lines.Face face : fan.side(Direction.SOUTH)) {
			BlockPos next = face.cell().relative(face.side());
			if (duct(level, next, wasCells) && distance.putIfAbsent(next.asLong(), 1) == null) {
				queue.add(next);
			}
		}
		List<Airflow.Fan> fans = new ArrayList<>();
		fans.add(Airflow.Fan.main(fan.running() ? fan.strength() : 0));
		Set<Long> boostersSeen = new HashSet<>();
		BlockPos far = null;
		int farthest = 0;
		while (!queue.isEmpty() && distance.size() < MOST_CELLS) {
			BlockPos at = queue.poll();
			int d = distance.get(at.asLong());
			state.cells.add(at.asLong());
			if (d > farthest) {
				farthest = d;
				far = at;
			}
			for (Direction side : Direction.values()) {
				BlockPos next = at.relative(side);
				if (distance.containsKey(next.asLong())) {
					continue;
				}
				if (duct(level, next, wasCells)) {
					distance.put(next.asLong(), d + 1);
					queue.add(next);
					continue;
				}
				Long boosterMain = booster(level, next, wasBoosters);
				if (boosterMain == null || !boostersSeen.add(boosterMain)) {
					continue;
				}
				// a booster: in at its north face, out of its south; turned the other way it blocks the line
				BlockPos main = BlockPos.of(boosterMain);
				PlantBlockEntity part = level.isLoaded(main) && BigParts.entity(level, main) instanceof PlantBlockEntity b ? b : null;
				List<dev.wildcatter.block.Lines.Face> inlet = part == null ? List.of() : part.side(Direction.NORTH);
				List<dev.wildcatter.block.Lines.Face> outlet = part == null ? List.of() : part.side(Direction.SOUTH);
				boolean intoInlet = part == null || inlet.stream().anyMatch(f -> f.cell().relative(f.side()).equals(at));
				if (!intoInlet) {
					state.fault = "a booster fan faces against the air at " + main.toShortString();
					continue;
				}
				double speed = part == null ? state.boosterSpeed.getOrDefault(boosterMain, 0.0) : part.running() ? part.strength() : 0;
				state.boosterSpeed.put(boosterMain, speed);
				fans.add(Airflow.Fan.booster(speed));
				int length = Machine.BOOSTER_FAN.depth();
				if (part != null) {
					for (BlockPos cell : part.cells()) {
						state.boosterCells.put(cell.asLong(), boosterMain);
						distance.put(cell.asLong(), d + length);
					}
				} else {
					wasBoosters.forEach((cell, m) -> {
						if (m.equals(boosterMain)) {
							state.boosterCells.put(cell, m);
							distance.put(cell, d + length);
						}
					});
				}
				for (dev.wildcatter.block.Lines.Face face : outlet) {
					BlockPos out = face.cell().relative(face.side());
					if (!distance.containsKey(out.asLong()) && duct(level, out, wasCells)) {
						distance.put(out.asLong(), d + length + 1);
						queue.add(out);
					}
				}
			}
		}
		state.end = far;
		state.air = Airflow.solve(farthest + state.extraMetres, fans.toArray(new Airflow.Fan[0]));
		register(level, new Run(fan.getBlockPos(), far, state.air, level.getGameTime(), Set.copyOf(state.cells)));
	}

	private static boolean duct(Level level, BlockPos pos, Set<Long> was) {
		if (!level.isLoaded(pos)) {
			return was.contains(pos.asLong());
		}
		return level.getBlockState(pos).getBlock() instanceof VentDuctBlock;
	}

	private static @Nullable Long booster(Level level, BlockPos pos, Map<Long, Long> was) {
		if (!level.isLoaded(pos)) {
			return was.get(pos.asLong());
		}
		if (level.getBlockState(pos).getBlock() instanceof PlantMachineBlock part && part.machine() == Machine.BOOSTER_FAN) {
			return PlantMachineBlock.mainOf(pos, level.getBlockState(pos)).asLong();
		}
		return null;
	}

	private static void register(ServerLevel level, Run run) {
		Map<Long, Run> runs = RUNS.computeIfAbsent(level, l -> new HashMap<>());
		Map<Long, Long> cells = CELLS.computeIfAbsent(level, l -> new HashMap<>());
		Run old = runs.put(run.fan().asLong(), run);
		long fan = run.fan().asLong();
		if (old != null) {
			for (long cell : old.cells()) {
				cells.remove(cell, fan);
			}
		}
		for (long cell : run.cells()) {
			cells.put(cell, fan);
		}
	}

	/** The fan is gone: its line gives no air. */
	public static void forget(Level level, BlockPos fan) {
		Map<Long, Run> runs = RUNS.get(level);
		Run old = runs == null ? null : runs.remove(fan.asLong());
		Map<Long, Long> cells = CELLS.get(level);
		if (old != null && cells != null) {
			for (long cell : old.cells()) {
				cells.remove(cell, fan.asLong());
			}
		}
	}

	private static boolean live(Level level, Run run) {
		return level.getGameTime() - run.tick() <= STALE_TICKS || !level.isLoaded(run.fan());
	}

	// ---- the air at a place -----------------------------------------------------------------------------------------------------

	/** The air at {@code pos}: the best line whose end is within 30 m, or whose duct is within 5 m (the bore beside it). */
	public static Air airAt(Level level, BlockPos pos) {
		Map<Long, Run> runs = RUNS.get(level);
		if (runs == null || runs.isEmpty()) {
			return new Air(0, null);
		}
		Run best = null;
		double reach = Airflow.REACH_PAST_END * Airflow.REACH_PAST_END;
		for (Run run : runs.values()) {
			if (run.end() != null && run.end().distSqr(pos) <= reach && live(level, run) && (best == null || run.air().endFlow() > best.air().endFlow())) {
				best = run;
			}
		}
		Map<Long, Long> cells = CELLS.get(level);
		int r = (int) Airflow.BESIDE_DUCT;
		if (cells != null && !cells.isEmpty()) {
			for (int dx = -r; dx <= r; dx++) {
				for (int dy = -r; dy <= r; dy++) {
					for (int dz = -r; dz <= r; dz++) {
						Long fan = cells.get(BlockPos.asLong(pos.getX() + dx, pos.getY() + dy, pos.getZ() + dz));
						Run run = fan == null ? null : runs.get(fan);
						if (run != null && live(level, run) && (best == null || run.air().endFlow() > best.air().endFlow())) {
							best = run;
						}
					}
				}
			}
		}
		return best == null ? new Air(0, null) : new Air(best.air().endFlow(), best);
	}

	/** What a click on a duct says: the line it is in and its air. */
	public static String describeDuct(ServerLevel level, BlockPos pos) {
		Map<Long, Long> cells = CELLS.get(level);
		Long fan = cells == null ? null : cells.get(pos.asLong());
		Run run = fan == null || RUNS.get(level) == null ? null : RUNS.get(level).get(fan);
		if (run == null || !live(level, run)) {
			return "Ventilation duct (1.4 m): no fan blowing into it";
		}
		return String.format(java.util.Locale.ROOT, "Ventilation duct (1.4 m): %.0f m from the fan at %s; the fan pushes %.1f m3/s at %.0f Pa, %.1f m3/s at its end",
				run.air().metres(), run.fan().toShortString(), run.air().fanFlow(), run.air().pressurePa(), run.air().endFlow());
	}

	// ---- a drive, and what it holds ---------------------------------------------------------------------------------------------

	/** True underground in a drive the TBM cut: on its floor, walkway or track, out of sight of the sky. */
	public static boolean inDrive(Level level, BlockPos feet) {
		if (level.canSeeSky(feet)) {
			return false;
		}
		for (int down = 0; down <= 2; down++) {
			Block block = level.getBlockState(feet.below(down)).getBlock();
			if (block == TailingsBlocks.MINE_FLOOR || block == TailingsBlocks.MINE_LANDING || block == TailingsBlocks.MINE_STAIR || block == TailingsBlocks.SLOPE
					|| block == TailingsBlocks.MINE_RAIL || block == TailingsBlocks.MINE_SWITCH) {
				return true;
			}
		}
		return false;
	}

	/** True where the rock round {@code pos} is tuff or sandstone, which hold uranium (Rock): stale air there reads RADON. */
	public static boolean radonRock(Level level, BlockPos pos) {
		for (int dx = -4; dx <= 4; dx += 2) {
			for (int dy = -4; dy <= 6; dy += 2) {
				for (int dz = -4; dz <= 4; dz += 2) {
					Block block = level.getBlockState(pos.offset(dx, dy, dz)).getBlock();
					if (block == Blocks.TUFF || block == Blocks.SANDSTONE || block == Blocks.RED_SANDSTONE || block == Blocks.SMOOTH_SANDSTONE
							|| block == Blocks.CUT_SANDSTONE || block == Blocks.CHISELED_SANDSTONE) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** What a gas detector reads at {@code pos}. */
	public static Component detector(ServerLevel level, BlockPos pos) {
		if (!inDrive(level, pos)) {
			return Component.literal("Gas detector: not in a drive (open air)");
		}
		Air air = airAt(level, pos);
		String reading = air.fresh() ? "fresh, " + Airflow.describe(air.flow()) : "STALE AIR, " + Airflow.describe(air.flow());
		if (!air.fresh() && radonRock(level, pos)) {
			reading += "; RADON (tuff or sandstone)";
		}
		return Component.literal("Gas detector: " + reading);
	}

	// ---- players --------------------------------------------------------------------------------------------------------------

	/**
	 * Once a second: each player in a drive's stale air counts it, and one holding a gas detector sees the reading. Where the config turns
	 * stale air on (or a test does for its player), 30 s of it gives Mining Fatigue I and 2 minutes Weakness I; 5 s of fresh air clears
	 * them (within the spec's 10 s).
	 */
	public static void tickPlayers(ServerLevel level) {
		if (level.getGameTime() % 20 != 0) {
			return;
		}
		boolean rule = VentRules.staleAir(level.getServer(), Platform.INSTANCE.configDir().resolve("tailings-ventilation.properties"));
		for (ServerPlayer player : level.players()) {
			if (player.isSpectator()) {
				continue;
			}
			BlockPos feet = player.blockPosition();
			boolean drive = inDrive(level, feet);
			boolean stale = drive && !airAt(level, feet).fresh();
			int[] counts = PLAYERS.computeIfAbsent(player.getUUID(), id -> new int[2]);
			if (stale) {
				counts[0]++;
				counts[1] = 0;
			} else {
				counts[1]++;
			}
			if (drive && (player.getMainHandItem().getItem() instanceof dev.wildcatter.tailings.item.GasDetectorItem
					|| player.getOffhandItem().getItem() instanceof dev.wildcatter.tailings.item.GasDetectorItem)) {
				player.sendOverlayMessage(detector(level, feet));
			}
			if (!rule && !TEST_STALE.contains(player.getUUID())) {
				continue;
			}
			if (stale && counts[0] >= 30) {
				give(player, MobEffects.MINING_FATIGUE);
			}
			if (stale && counts[0] >= 120) {
				give(player, MobEffects.WEAKNESS);
			}
			if (!stale && counts[1] >= 5 && counts[0] > 0) {
				counts[0] = 0;
				clear(player, MobEffects.MINING_FATIGUE);
				clear(player, MobEffects.WEAKNESS);
			}
		}
	}

	/** Seconds the player has been in stale air (0 in fresh). */
	public static int staleSeconds(ServerPlayer player) {
		int[] counts = PLAYERS.get(player.getUUID());
		return counts == null ? 0 : counts[0];
	}

	private static final int EFFECT_TICKS = 100;

	private static void give(ServerPlayer player, Holder<MobEffect> effect) {
		player.addEffect(new MobEffectInstance(effect, EFFECT_TICKS, 0, true, false, true));
	}

	/** Takes off only the effect stale air gave: a short, ambient one; a potion's or a beacon's is left. */
	private static void clear(ServerPlayer player, Holder<MobEffect> effect) {
		MobEffectInstance on = player.getEffect(effect);
		if (on != null && on.isAmbient() && on.getAmplifier() == 0 && on.getDuration() <= EFFECT_TICKS) {
			player.removeEffect(effect);
		}
	}
}
