package dev.wildcatter.tailings.paste;

import com.mojang.serialization.Codec;
import dev.wildcatter.tailings.Tailings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Paste curing while its chunk is unloaded (docs/questions/gaps2.md): the game holds a scheduled tick's time left while its chunk is
 * not loaded, so paste would wait with it. Each dimension keeps, for each chunk that has had wet paste, the game time its paste last
 * stepped. A chunk's wet paste steps at least once every {@link PasteFill#cureStepTicks()} while it is loaded, so a longer gap is time
 * it spent unloaded; every wet block in the chunk gets that time's whole steps on its first step back (within one step of the
 * chunk's return). A chunk with no record yet (paste placed before this, or in a fresh world) gets nothing until it has one.
 */
public final class PasteClock extends SavedData {
	/** Leeway over a step before a gap counts as unloaded time, ticks. */
	static final long SLACK = 100;

	public static final Codec<PasteClock> CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG.listOf()).xmap(PasteClock::new, PasteClock::save);
	public static final SavedDataType<PasteClock> TYPE = new SavedDataType<>(Tailings.id("paste_clock"), PasteClock::new, CODEC, DataFixTypes.LEVEL);

	/** By chunk: the last step's game time, the unloaded ticks to make up, and when the making up began. */
	private final Map<Long, long[]> chunks = new HashMap<>();

	public PasteClock() {
	}

	private PasteClock(Map<String, List<Long>> saved) {
		saved.forEach((key, values) -> {
			if (values.size() == 3) {
				chunks.put(Long.parseLong(key), new long[] {values.get(0), values.get(1), values.get(2)});
			}
		});
	}

	private Map<String, List<Long>> save() {
		Map<String, List<Long>> out = new HashMap<>();
		chunks.forEach((key, v) -> out.put(Long.toString(key), List.of(v[0], v[1], v[2])));
		return out;
	}

	public static PasteClock get(ServerLevel level) {
		return level.getDataStorage().computeIfAbsent(TYPE);
	}

	/**
	 * A wet block in {@code chunk} steps at {@code now}: the extra steps it is owed for time its chunk spent unloaded (0 almost always).
	 */
	public int missedSteps(ChunkPos chunk, long now) {
		long step = PasteFill.cureStepTicks();
		long[] at = chunks.get(chunk.pack());
		if (at == null) {
			chunks.put(chunk.pack(), new long[] {now, 0, 0});
			setDirty();
			return 0;
		}
		long gap = now - at[0];
		if (gap > step + SLACK) {
			at[1] = gap - step;
			at[2] = now;
		}
		at[0] = now;
		setDirty();
		return at[1] > 0 && now < at[2] + step ? (int) Math.min(PasteFill.CURE_STEPS, at[1] / step) : 0;
	}

	/** Test hook: as if the chunk's paste last stepped {@code ticks} ago. */
	public void backdate(ChunkPos chunk, long now, long ticks) {
		chunks.put(chunk.pack(), new long[] {now - ticks, 0, 0});
		setDirty();
	}
}
