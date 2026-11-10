package dev.wildcatter.tailings;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.wildcatter.tailings.haul.TrainHarm;
import dev.wildcatter.tailings.platform.Platform;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

/**
 * Tailings' per-world rules (docs/questions/answers-2026-11-10.md #23): the fixes and hazards that change what an existing world does.
 * They are saved with the world ({@code data/tailings/rules.dat}), on in a new world and off in a world saved before them, and changed
 * by {@code /tailings rule <name> <true|false>} (an operator's command):
 * <ul>
 * <li>{@code ventilation_interlock}: a TBM with no duct air at its back-up for 60 s stops, NO VENTILATION (l1-ventilation.md #1);</li>
 * <li>{@code stale_air}: a player in a drive's stale air gets Mining Fatigue, then Weakness (l1-ventilation.md #1);</li>
 * <li>{@code headframe_hopper}: a headframe bin with no belt under it feeds a hopper or chest under it (gaps2.md #4);</li>
 * <li>{@code paste_unloaded_cure}: paste makes up the time its chunk was unloaded (gaps2.md #6);</li>
 * <li>{@code dry_mill_all_hmc}: a filter with a dryer touching it sends all its heavy mineral concentrate to the dry mill
 * (l6-dry-mill.md #3);</li>
 * <li>{@code paste_wall_breach}: a solid wall removed beside wet paste spills it, as a broken bulkhead does (gaps2.md #5). Off even in a
 * new world.</li>
 * </ul>
 * A world is new when it has no rules saved yet, its game time is still 0 and no Tailings data is saved in it: the rules are made when
 * the server starts, before the first tick. A world with rules saved by a later version keeps what it saved; a rule it never saved (one
 * added after the world was made) is off, as for any existing world. The old {@code config/tailings-rules.properties} and {@code
 * config/tailings-ventilation.properties} are read once, when a world's rules are first made: a key either file sets explicitly is taken
 * over that world's default.
 */
public final class OptInRules extends SavedData {
	public static final String VENTILATION_INTERLOCK = "ventilation_interlock";
	public static final String STALE_AIR = "stale_air";
	public static final String HEADFRAME_HOPPER = "headframe_hopper";
	public static final String PASTE_UNLOADED_CURE = "paste_unloaded_cure";
	public static final String DRY_MILL_ALL_HMC = "dry_mill_all_hmc";
	public static final String PASTE_WALL_BREACH = "paste_wall_breach";
	/** The old files, read once when a world's rules are made. */
	public static final String FILE = "tailings-rules.properties";
	public static final String VENT_FILE = "tailings-ventilation.properties";
	/** The rules' saved format. */
	public static final int VERSION = 1;

	/** Every rule, in the order {@code /tailings rule} lists them, with its value in a new world. */
	public static final Map<String, Boolean> NEW_WORLD;

	static {
		Map<String, Boolean> rules = new LinkedHashMap<>();
		rules.put(VENTILATION_INTERLOCK, true);
		rules.put(STALE_AIR, true);
		rules.put(HEADFRAME_HOPPER, true);
		rules.put(PASTE_UNLOADED_CURE, true);
		rules.put(DRY_MILL_ALL_HMC, true);
		rules.put(PASTE_WALL_BREACH, false);
		NEW_WORLD = java.util.Collections.unmodifiableMap(rules);
	}

	/** What each rule means, for the command's list. */
	public static String meaning(String key) {
		return switch (key) {
			case VENTILATION_INTERLOCK -> "a TBM with no duct air at its back-up for 60 s stops, NO VENTILATION";
			case STALE_AIR -> "a player in a drive's stale air gets Mining Fatigue, then Weakness";
			case HEADFRAME_HOPPER -> "a headframe bin with no belt under it feeds a hopper or chest under it";
			case PASTE_UNLOADED_CURE -> "paste makes up the time its chunk was unloaded";
			case DRY_MILL_ALL_HMC -> "a filter with a dryer touching it sends all its heavy mineral concentrate to the dry mill";
			case PASTE_WALL_BREACH -> "a solid wall removed beside wet paste spills it, as a broken bulkhead does";
			default -> "";
		};
	}

	private record Saved(int version, boolean newWorld, Map<String, Boolean> rules) {
	}

	private static final Codec<Saved> SAVED = RecordCodecBuilder.create(i -> i.group(
			Codec.INT.fieldOf("version").forGetter(Saved::version),
			Codec.BOOL.optionalFieldOf("new_world", false).forGetter(Saved::newWorld),
			Codec.unboundedMap(Codec.STRING, Codec.BOOL).fieldOf("rules").forGetter(Saved::rules)).apply(i, Saved::new));
	public static final Codec<OptInRules> CODEC = SAVED.xmap(OptInRules::new, OptInRules::save);
	public static final SavedDataType<OptInRules> TYPE = new SavedDataType<>(Tailings.id("rules"), OptInRules::new, CODEC, DataFixTypes.LEVEL);

	private final Map<String, Boolean> rules = new LinkedHashMap<>();
	private int version = VERSION;
	private boolean newWorld;

	/** Only for the saved-data type: {@link #get} makes a world's rules. */
	public OptInRules() {
	}

	private OptInRules(Saved saved) {
		version = saved.version();
		newWorld = saved.newWorld();
		rules.putAll(saved.rules());
	}

	private Saved save() {
		return new Saved(version, newWorld, Map.copyOf(rules));
	}

	/** Whether {@code key} is on in the server's world; off with no server. */
	public static boolean on(@Nullable MinecraftServer server, String key) {
		return server != null && get(server).rules.getOrDefault(key, false);
	}

	/** Sets {@code key} in the server's world, saved with it. */
	public static void set(MinecraftServer server, String key, boolean value) {
		OptInRules rules = get(server);
		rules.rules.put(key, value);
		rules.setDirty();
	}

	/** Each rule and its value in the server's world, in {@link #NEW_WORLD}'s order. */
	public static Map<String, Boolean> all(MinecraftServer server) {
		OptInRules rules = get(server);
		Map<String, Boolean> out = new LinkedHashMap<>();
		NEW_WORLD.keySet().forEach(key -> out.put(key, rules.rules.getOrDefault(key, false)));
		return out;
	}

	/** True when the server's world was made with these rules (a new world), false for one saved before them. */
	public static boolean newWorld(MinecraftServer server) {
		return get(server).newWorld;
	}

	/** The server's world's rules, made the first time it is asked (at server start, before the first tick). */
	public static synchronized OptInRules get(MinecraftServer server) {
		OptInRules rules = server.getDataStorage().get(TYPE);
		if (rules == null) {
			rules = make(server);
			server.getDataStorage().set(TYPE, rules);
		}
		return rules;
	}

	/** Called at server start, so a world's rules are made before its game time moves. */
	public static void serverStarted(MinecraftServer server) {
		get(server);
	}

	private static OptInRules make(MinecraftServer server) {
		long time = server.overworld().getGameTime();
		boolean data = tailingsData(server);
		OptInRules made = new OptInRules();
		made.newWorld = isNew(time, data);
		NEW_WORLD.forEach((key, value) -> made.rules.put(key, made.newWorld && value));
		Path config = Platform.INSTANCE.configDir();
		List<String> old = TrainHarm.read(config.resolve(FILE));
		List<String> vent = TrainHarm.read(config.resolve(VENT_FILE));
		StringBuilder fromFiles = new StringBuilder();
		for (String key : new String[] {HEADFRAME_HOPPER, PASTE_WALL_BREACH, PASTE_UNLOADED_CURE}) {
			fromFile(made, old, key, key, fromFiles);
		}
		fromFile(made, vent, "interlock", VENTILATION_INTERLOCK, fromFiles);
		fromFile(made, vent, "stale_air", STALE_AIR, fromFiles);
		made.setDirty();
		Tailings.LOGGER.info("Tailings: world rules made for {} world (game time {}, Tailings data {}){}: {}", made.newWorld ? "a new" : "an existing", time,
				data ? "found" : "none", fromFiles.length() == 0 ? "" : ", from the old config files " + fromFiles, made.rules);
		return made;
	}

	/** A world with no rules saved is new while its game time is still 0 and it holds no Tailings data; otherwise it was saved before. */
	public static boolean isNew(long gameTime, boolean tailingsData) {
		return gameTime == 0 && !tailingsData;
	}

	private static void fromFile(OptInRules made, List<String> lines, String fileKey, String rule, StringBuilder note) {
		Boolean value = RuleFile.value(lines, fileKey);
		if (value != null) {
			made.rules.put(rule, value);
			note.append(note.length() == 0 ? "" : ", ").append(rule).append('=').append(value);
		}
	}

	/** Tailings data saved in the world before its rules were: the server's {@code data/tailings} or a dimension's. */
	private static boolean tailingsData(MinecraftServer server) {
		Path root = server.getWorldPath(LevelResource.ROOT);
		if (Files.isDirectory(server.getWorldPath(LevelResource.DATA).resolve(Tailings.MOD_ID))) {
			return true;
		}
		for (ServerLevel level : server.getAllLevels()) {
			if (Files.isDirectory(DimensionType.getStorageFolder(level.dimension(), root).resolve("data").resolve(Tailings.MOD_ID))) {
				return true;
			}
		}
		return false;
	}
}
