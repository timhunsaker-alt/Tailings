package dev.wildcatter.tailings.pit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Loose material pouring out of a machine (docs/multiblocks/open-pit.md, Pouring): a truck's body, a dozer's blade, a dragline's
 * bucket. Each block leaves its source in turn, drops to the ground, runs over the ground and the pile to the cell the settle rule
 * gave it, and sets there. Which cell each block rests in is worked out when the pour starts, the cells still on their way held as
 * taken, so the pile is the one an instant tip would leave. {@link Pouring} runs it; this is its data and timing.
 */
public final class Pour {
	/** The body's lift at which a truck's load starts to slide out, and by which it is all out (real bodies shed it from a third up). */
	public static final float FROM = 0.35F, TO = 0.65F;
	/** How fast the load runs over the ground, blocks a tick: about 2.4 m/s, a thick flow, wet concrete rather than water. */
	public static final double SPEED = 0.12;
	/** The longest a block runs, ticks. */
	public static final int MOST_RUN = 70;
	/** Ticks the film over a block thins before it sets. */
	public static final int SET = 16;
	/** Ticks over which a truck's pour sets, rim first (3 s). */
	public static final int SETTING = 60;

	/** A flow onto the ground, that sets as a block. */
	public static final int GROUND = 0;
	/** A flow into a hopper or pocket, which has already counted it: drawn only. */
	public static final int POCKET = 1;
	/** A flow into a truck's body, which has already been loaded: drawn only, from the bucket down onto the load. */
	public static final int BODY = 2;

	/**
	 * One block of the pour: where it rests (or, drawn only, where it lands), the block it sets as, its piece of the load, its times
	 * (let go at {@code launch}, falling for {@code fall} ticks, running for {@code run}, set at {@code set}), its kind, where it
	 * leaves its source ({@code lip}) and lands ({@code land}), and how wide the falling stream is, metres.
	 */
	public record Flow(BlockPos at, BlockState state, Map<String, Integer> quarters, Map<String, Long> taken, long launch, int fall, int run, long set,
			int kind, Vec3 lip, Vec3 land, float wide) {
		/** When it reaches its cell. */
		public long arrive() {
			return launch + fall + run;
		}

		public boolean drawnOnly() {
			return kind != GROUND;
		}

		public long kg() {
			long n = 0;
			for (long kg : taken.values()) {
				n += kg;
			}
			return n;
		}

		public Loose.Piece piece() {
			return new Loose.Piece(state, quarters, taken);
		}
	}

	private Pour() {
	}

	/** Ticks to drop {@code h} blocks. */
	public static int fall(double h) {
		return Math.max(4, (int) Math.round(5 * Math.sqrt(Math.max(0, h))));
	}

	/** Ticks to run {@code d} blocks over the ground. */
	public static int run(double d) {
		return Math.min(MOST_RUN, (int) Math.round(d / SPEED));
	}

	/** How far a pile of {@code blocks} at 3 in 4 reaches from where it is let go, and a little more: about (4 n / pi)^(1/3). */
	public static double reach(int blocks) {
		return 1.15 * Math.cbrt(4.0 * Math.max(1, blocks) / Math.PI);
	}

	// ---- saved with the machine --------------------------------------------------------------------------------------------------

	public static String save(List<Flow> flows) {
		StringBuilder out = new StringBuilder();
		for (Flow f : flows) {
			out.append(out.isEmpty() ? "" : ";").append(f.at.getX()).append(',').append(f.at.getY()).append(',').append(f.at.getZ()).append(',')
					.append(BuiltInRegistries.BLOCK.getKey(f.state.getBlock())).append(',').append(f.launch).append(',').append(f.fall).append(',')
					.append(f.run).append(',').append(f.set).append(',').append(f.kind).append(',').append(map(f.quarters)).append(',')
					.append(map(f.taken)).append(',').append(vec(f.lip)).append(',').append(vec(f.land)).append(',').append(num(f.wide));
		}
		return out.toString();
	}

	public static List<Flow> load(String saved) {
		List<Flow> out = new ArrayList<>();
		if (saved == null || saved.isEmpty()) {
			return out;
		}
		for (String part : saved.split(";")) {
			String[] p = part.split(",", -1);
			if (p.length < 18) {
				continue;
			}
			try {
				Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(p[3]));
				BlockState state = (block == null || block == Blocks.AIR ? Blocks.DIRT : block).defaultBlockState();
				Map<String, Integer> quarters = new LinkedHashMap<>();
				unmap(p[9]).forEach((id, n) -> quarters.put(id, n.intValue()));
				out.add(new Flow(new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])), state, quarters, unmap(p[10]),
						Long.parseLong(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]), Long.parseLong(p[7]), Integer.parseInt(p[8]), vec(p, 11), vec(p, 14),
						Float.parseFloat(p[17])));
			} catch (RuntimeException ex) {
				// left out
			}
		}
		return out;
	}

	private static String num(double d) {
		return String.format(Locale.ROOT, "%.2f", d);
	}

	private static String vec(Vec3 v) {
		return num(v.x) + "," + num(v.y) + "," + num(v.z);
	}

	private static Vec3 vec(String[] p, int at) {
		return new Vec3(Double.parseDouble(p[at]), Double.parseDouble(p[at + 1]), Double.parseDouble(p[at + 2]));
	}

	private static String map(Map<String, ? extends Number> m) {
		StringBuilder out = new StringBuilder();
		m.forEach((id, n) -> out.append(out.isEmpty() ? "" : "+").append(id).append('=').append(n));
		return out.toString();
	}

	private static Map<String, Long> unmap(String s) {
		Map<String, Long> out = new LinkedHashMap<>();
		if (!s.isEmpty()) {
			for (String e : s.split("\\+")) {
				int eq = e.lastIndexOf('=');
				out.put(e.substring(0, eq), Long.parseLong(e.substring(eq + 1)));
			}
		}
		return out;
	}

	// ---- sent to the client, to draw ---------------------------------------------------------------------------------------------

	/** What the client draws of one block of the pour. */
	public record Glob(int x, int y, int z, int state, long launch, int fall, int run, long set, int kind, Vec3 lip, Vec3 land, float wide) {
		public boolean drawnOnly() {
			return kind != GROUND;
		}
	}

	/** What the client draws: each block's way. */
	public record Shown(List<Glob> globs) {
		public static final Shown NONE = new Shown(List.of());
	}

	/** The pour as the client is sent it; held under the 32,767 characters a synced string may be. */
	public static String show(List<Flow> flows) {
		StringBuilder out = new StringBuilder();
		for (Flow f : flows) {
			StringBuilder one = new StringBuilder();
			one.append(f.at.getX()).append(',').append(f.at.getY()).append(',').append(f.at.getZ()).append(',').append(Block.getId(f.state)).append(',')
					.append(f.launch).append(',').append(f.fall).append(',').append(f.run).append(',').append(f.set).append(',').append(f.kind).append(',')
					.append(vec(f.lip)).append(',').append(vec(f.land)).append(',').append(num(f.wide));
			if (out.length() + one.length() + 1 > 30_000) {
				break;
			}
			out.append(out.isEmpty() ? "" : ";").append(one);
		}
		return out.toString();
	}

	public static Shown shown(String sent) {
		if (sent == null || sent.isEmpty()) {
			return Shown.NONE;
		}
		try {
			List<Glob> globs = new ArrayList<>();
			for (String part : sent.split(";")) {
				String[] p = part.split(",");
				globs.add(new Glob(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), Long.parseLong(p[4]),
						Integer.parseInt(p[5]), Integer.parseInt(p[6]), Long.parseLong(p[7]), Integer.parseInt(p[8]), vec(p, 9), vec(p, 12), Float.parseFloat(p[15])));
			}
			return new Shown(globs);
		} catch (RuntimeException ex) {
			return Shown.NONE;
		}
	}
}
