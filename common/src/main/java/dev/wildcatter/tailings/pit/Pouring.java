package dev.wildcatter.tailings.pit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The pour service (docs/multiblocks/open-pit.md, Pouring): one machine's loose material on its way into the world, from a truck's
 * body, a dozer's blade or a dragline's bucket, and the streams drawn into a hopper or a truck's body. A machine starts a pour with
 * where the load leaves it, where it lands, where it is let go for the settle rule, how fast it leaves and how long the pile takes
 * to set; then ticks it, saves it with itself, and on being removed for good finishes it at once. Pours started one after another
 * hold each other's cells and set in order, so a dozer's run of small spills builds one pile as an instant tip would.
 */
public final class Pouring {
	private final List<Pour.Flow> flows = new ArrayList<>();
	private boolean changed;

	/** A pour onto the ground, as a machine asks for it. */
	public record Spec(BlockPos start, Vec3 lip, Vec3 land, int window, int setting, float wide) {
	}

	public boolean empty() {
		return flows.isEmpty();
	}

	public int size() {
		return flows.size();
	}

	public List<Pour.Flow> flows() {
		return Collections.unmodifiableList(flows);
	}

	/** Whether what the client is sent should be sent again; asking clears it. */
	public boolean takeChanged() {
		boolean was = changed;
		changed = false;
		return was;
	}

	/** Blocks of it that have left their source. */
	public int released(long now) {
		int n = 0;
		for (Pour.Flow f : flows) {
			if (f.launch() <= now) {
				n++;
			}
		}
		return n;
	}

	/** Kilograms not yet let go. */
	public long waitingKg(long now) {
		long kg = 0;
		for (Pour.Flow f : flows) {
			if (f.launch() > now) {
				kg += f.kg();
			}
		}
		return kg;
	}

	private Set<BlockPos> held() {
		Set<BlockPos> held = new HashSet<>();
		for (Pour.Flow f : flows) {
			if (!f.drawnOnly()) {
				held.add(f.at());
			}
		}
		return held;
	}

	private long lastSet() {
		long last = 0;
		for (Pour.Flow f : flows) {
			if (!f.drawnOnly()) {
				last = Math.max(last, f.set());
			}
		}
		return last;
	}

	/**
	 * Pours {@code load} onto the ground: block by block as an instant tip would ({@link Loose#next}), each resting where the settle rule
	 * puts it from {@code spec.start}, the cells still on their way (this pour's and any before it) held as taken. What finds no room
	 * stays in {@code load}. The blocks leave over {@code spec.window} ticks, run from where they land to their cells, and set rim
	 * first, a layer at a time from the bottom, over {@code spec.setting} ticks, never before a block set before them. Returns the
	 * blocks it will set.
	 */
	public int pour(ServerLevel level, PitLoad load, Spec spec) {
		long now = level.getGameTime();
		Set<BlockPos> held = held();
		List<Loose.Piece> pieces = new ArrayList<>();
		List<BlockPos> cells = new ArrayList<>();
		for (Loose.Piece piece = Loose.next(load); piece != null; piece = Loose.next(load)) {
			BlockPos rest = Loose.rest(level, spec.start(), held);
			if (rest == null) {
				piece.putBack(load);
				break;
			}
			held.add(rest);
			pieces.add(piece);
			cells.add(rest);
		}
		int n = pieces.size();
		if (n == 0) {
			return 0;
		}
		Vec3 lip = spec.lip(), land = spec.land();
		int fall = Pour.fall(lip.y - land.y);
		long[] launch = new long[n], arrive = new long[n];
		int[] run = new int[n];
		for (int i = 0; i < n; i++) {
			BlockPos at = cells.get(i);
			launch[i] = now + (long) i * spec.window() / n;
			run[i] = Pour.run(Math.hypot(at.getX() + 0.5 - land.x, at.getZ() + 0.5 - land.z) + Math.max(0, land.y - at.getY()) * 0.5);
			arrive[i] = launch[i] + fall + run[i];
		}
		// rim first, a layer at a time: the lowest first, and in a layer the farthest from where it was let go first; so every block
		// sets on one that has set below it, and the sheet thins from its rim to the middle of the pile
		Integer[] order = new Integer[n];
		for (int i = 0; i < n; i++) {
			order[i] = i;
		}
		double sx = spec.start().getX() + 0.5, sz = spec.start().getZ() + 0.5;
		java.util.Arrays.sort(order, java.util.Comparator.<Integer>comparingInt(i -> cells.get(i).getY())
				.thenComparing(i -> -Math.hypot(cells.get(i).getX() + 0.5 - sx, cells.get(i).getZ() + 0.5 - sz)));
		double gap = (double) spec.setting() / n;
		double last = lastSet();
		for (int k = 0; k < n; k++) {
			int i = order[k];
			Loose.Piece piece = pieces.get(i);
			double set = Math.max(arrive[i] + Pour.SET, last + (k == 0 && last == 0 ? 0 : gap));
			last = set;
			flows.add(new Pour.Flow(cells.get(i), piece.state(), piece.quarters(), piece.taken(), launch[i], fall, run[i], (long) Math.ceil(set), Pour.GROUND, lip, land,
					spec.wide()));
		}
		changed = true;
		return n;
	}

	/**
	 * A stream drawn only, of {@code kg} already counted where it went: into a hopper's pocket at {@code at} ({@link Pour#POCKET}) or
	 * onto a truck's load ({@link Pour#BODY}), {@code shares} lumps of it over {@code window} ticks.
	 */
	public void stream(ServerLevel level, int kind, BlockPos at, BlockState state, long kg, int shares, Vec3 lip, Vec3 land, int window, float wide) {
		long now = level.getGameTime();
		int fall = Pour.fall(lip.y - land.y);
		for (int i = 0; i < shares; i++) {
			long launch = now + (long) i * window / shares;
			long share = kg / shares + (i < kg % shares ? 1 : 0);
			flows.add(new Pour.Flow(at, state, Map.of(), Map.of("pour", share), launch, fall, 0, launch + fall, kind, lip, land, wide));
		}
		changed = true;
	}

	/** One tick: dust where blocks leave and land, and the blocks whose time has come set. Returns true while any is leaving. */
	public boolean tick(ServerLevel level, PitLoad back) {
		long now = level.getGameTime();
		boolean leaving = false;
		for (Pour.Flow f : flows) {
			if (f.launch() == now) {
				leaving = true;
				level.sendParticles(new BlockParticleOption(ParticleTypes.FALLING_DUST, f.state()), f.lip().x, f.lip().y, f.lip().z, 1, f.wide() * 0.3, 0.1, 0.3, 0.0);
			}
			if (f.launch() + f.fall() == now && f.kind() != Pour.BODY) {
				level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, f.state()), f.land().x, f.land().y + 0.2, f.land().z, 4, 0.6, 0.15, 0.6, 0.1);
			}
		}
		int setNow = 0;
		boolean waiting = false;   // ground flows set in order: once one waits, those after it wait too
		for (Iterator<Pour.Flow> it = flows.iterator(); it.hasNext();) {
			Pour.Flow f = it.next();
			if (f.set() > now || (waiting && !f.drawnOnly())) {
				waiting |= !f.drawnOnly();
				continue;
			}
			it.remove();
			changed = true;
			if (!f.drawnOnly()) {
				BlockPos at = set(level, f, back);
				if (at != null && setNow++ % 3 == 0) {
					level.playSound(null, at, f.state().getSoundType().getPlaceSound(), SoundSource.BLOCKS, 0.6F, 0.7F);
				}
			}
		}
		return leaving;
	}

	/** Every block still on its way sets now, in order; what finds no room goes into {@code back}. */
	public void finish(ServerLevel level, PitLoad back) {
		while (!flows.isEmpty()) {
			Pour.Flow f = flows.remove(0);
			if (!f.drawnOnly()) {
				set(level, f, back);
			}
		}
		changed = true;
	}

	/**
	 * One block sets at its cell. If the cell was taken while it was on its way, or its ground went, it settles again from there; with
	 * no room at all, it goes back into {@code back}.
	 */
	private @Nullable BlockPos set(ServerLevel level, Pour.Flow f, PitLoad back) {
		BlockPos at = f.at();
		BlockState there = level.getBlockState(at);
		if (!FallingBlock.isFree(there) || there.hasBlockEntity() || FallingBlock.isFree(level.getBlockState(at.below()))) {
			at = Loose.rest(level, at, held());
		}
		if (at == null) {
			f.piece().putBack(back);
			return null;
		}
		f.piece().place(level, at);
		return at;
	}

	public String save() {
		return Pour.save(flows);
	}

	public void load(String saved) {
		flows.clear();
		flows.addAll(Pour.load(saved));
		changed = true;
	}

	/** What the client is sent to draw. */
	public String show() {
		return Pour.show(flows);
	}
}
