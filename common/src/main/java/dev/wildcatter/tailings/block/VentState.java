package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.mine.Airflow;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A ventilation fan's duct line as it last read it (docs/multiblocks/ventilation.md): the duct cells, the boosters in it, where it ends
 * and the air it gives. Saved with the fan, so a line running through chunks that are not loaded is still known: nothing can change a
 * duct in a chunk that is not loaded.
 */
public final class VentState {
	/** Every duct cell of the line, as {@link BlockPos#asLong}. */
	final Set<Long> cells = new HashSet<>();
	/** Each booster cell in the line, to its main cell; and each booster's speed when last read. */
	final Map<Long, Long> boosterCells = new HashMap<>();
	final Map<Long, Double> boosterSpeed = new HashMap<>();
	@Nullable BlockPos end;
	Airflow.Result air = Airflow.solve(0);
	/** What was wrong in the line, or empty. */
	String fault = "";
	/** For the tests: this many metres of duct more than are laid (3.5 km will not fit a test's ground). Not saved. */
	int extraMetres;

	public Airflow.Result air() {
		return air;
	}

	public @Nullable BlockPos end() {
		return end;
	}

	public int ductCells() {
		return cells.size();
	}

	public String fault() {
		return fault;
	}

	public void setExtraMetres(int metres) {
		extraMetres = Math.max(0, metres);
	}

	void save(ValueOutput output) {
		output.putIntArray("duct", pack(cells));
		output.putIntArray("ductBoosterCells", pack(boosterCells.keySet()));
		output.putIntArray("ductBoosterMains", pack(boosterCells.values()));
		output.putLong("ductEnd", end == null ? Long.MIN_VALUE : end.asLong());
	}

	void load(ValueInput input) {
		cells.clear();
		boosterCells.clear();
		cells.addAll(unpack(input.getIntArray("duct").orElse(new int[0])));
		java.util.List<Long> at = unpack(input.getIntArray("ductBoosterCells").orElse(new int[0]));
		java.util.List<Long> main = unpack(input.getIntArray("ductBoosterMains").orElse(new int[0]));
		for (int i = 0; i < Math.min(at.size(), main.size()); i++) {
			boosterCells.put(at.get(i), main.get(i));
		}
		long saved = input.getLongOr("ductEnd", Long.MIN_VALUE);
		end = saved == Long.MIN_VALUE ? null : BlockPos.of(saved);
	}

	/** Positions as x, y, z triples, in order. */
	private static int[] pack(java.util.Collection<Long> positions) {
		int[] out = new int[positions.size() * 3];
		int i = 0;
		for (long p : positions) {
			BlockPos pos = BlockPos.of(p);
			out[i++] = pos.getX();
			out[i++] = pos.getY();
			out[i++] = pos.getZ();
		}
		return out;
	}

	private static java.util.List<Long> unpack(int[] packed) {
		java.util.List<Long> out = new java.util.ArrayList<>();
		for (int i = 0; i + 2 < packed.length; i += 3) {
			out.add(BlockPos.asLong(packed[i], packed[i + 1], packed[i + 2]));
		}
		return out;
	}
}
