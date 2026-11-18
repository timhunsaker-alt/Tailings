package dev.wildcatter.tailings.pit;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A haul truck's load pouring out of its tail (docs/multiblocks/open-pit.md, Pouring): each block of the load leaves the body in
 * turn as the body rises, drops off the tail, runs over the ground and the pile to the cell the settle rule gave it, and sets there.
 * Which cell each block rests in is worked out when the pour starts, the cells still on their way held as taken, so the pile is
 * the one an instant tip would leave. The blocks set in that same order, so each one sets on ground that already stands.
 */
public final class Pour {
	/** The body's lift at which the load starts to slide out, and by which it is all out (real bodies shed it from about a third up). */
	public static final float FROM = 0.35F, TO = 0.65F;
	/** How fast the load runs over the ground, blocks a tick: about 1.8 m/s, a thick flow, wet concrete rather than water. */
	public static final double SPEED = 0.09;
	/** The longest a block runs, ticks. */
	public static final int MOST_RUN = 100;
	/** Ticks a block lies wet at its cell, thickening, before it sets. */
	public static final int SET = 16;

	/**
	 * One block of the pour: where it rests (or, into a hopper, where it lands), the block it sets as, its piece of the load, and its
	 * times: let go at {@code launch}, off the tail for {@code fall} ticks, running for {@code run}, set at {@code set}.
	 */
	public record Flow(BlockPos at, BlockState state, Map<String, Integer> quarters, Map<String, Long> taken, long launch, int fall, int run, long set,
			boolean into) {
		/** When it reaches its cell. */
		public long arrive() {
			return launch + fall + run;
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

	/** Ticks to drop {@code h} blocks off the tail. */
	public static int fall(double h) {
		return Math.max(4, (int) Math.round(5 * Math.sqrt(Math.max(0, h))));
	}

	/** Ticks to run {@code d} blocks over the ground. */
	public static int run(double d) {
		return Math.min(MOST_RUN, (int) Math.round(d / SPEED));
	}

	// ---- saved with the truck ----------------------------------------------------------------------------------------------------

	public static String save(List<Flow> flows) {
		StringBuilder out = new StringBuilder();
		for (Flow f : flows) {
			out.append(out.isEmpty() ? "" : ";").append(f.at.getX()).append(',').append(f.at.getY()).append(',').append(f.at.getZ()).append(',')
					.append(BuiltInRegistries.BLOCK.getKey(f.state.getBlock())).append(',').append(f.launch).append(',').append(f.fall).append(',')
					.append(f.run).append(',').append(f.set).append(',').append(f.into ? 1 : 0).append(',').append(map(f.quarters)).append(',')
					.append(map(f.taken));
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
			if (p.length < 11) {
				continue;
			}
			try {
				Block block = BuiltInRegistries.BLOCK.getValue(Identifier.parse(p[3]));
				BlockState state = (block == null || block == Blocks.AIR ? Blocks.DIRT : block).defaultBlockState();
				Map<String, Integer> quarters = new LinkedHashMap<>();
				unmap(p[9]).forEach((id, n) -> quarters.put(id, n.intValue()));
				out.add(new Flow(new BlockPos(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2])), state, quarters, unmap(p[10]),
						Long.parseLong(p[4]), Integer.parseInt(p[5]), Integer.parseInt(p[6]), Long.parseLong(p[7]), "1".equals(p[8])));
			} catch (RuntimeException ex) {
				// left out
			}
		}
		return out;
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
	public record Glob(int x, int y, int z, int state, long launch, int fall, int run, long set, boolean into) {
	}

	/** What the client draws: where the load leaves the tail and lands, and each block's way. */
	public record Shown(Vec3 lip, Vec3 land, List<Glob> globs) {
		public static final Shown NONE = new Shown(Vec3.ZERO, Vec3.ZERO, List.of());
	}

	public static String show(Vec3 lip, Vec3 land, List<Flow> flows) {
		if (flows.isEmpty()) {
			return "";
		}
		StringBuilder out = new StringBuilder();
		out.append(String.format(java.util.Locale.ROOT, "%.2f,%.2f,%.2f,%.2f,%.2f,%.2f", lip.x, lip.y, lip.z, land.x, land.y, land.z));
		for (Flow f : flows) {
			out.append(';').append(f.at.getX()).append(',').append(f.at.getY()).append(',').append(f.at.getZ()).append(',').append(Block.getId(f.state))
					.append(',').append(f.launch).append(',').append(f.fall).append(',').append(f.run).append(',').append(f.set).append(',')
					.append(f.into ? 1 : 0);
		}
		return out.toString();
	}

	public static Shown shown(String sent) {
		if (sent == null || sent.isEmpty()) {
			return Shown.NONE;
		}
		try {
			String[] parts = sent.split(";");
			String[] h = parts[0].split(",");
			Vec3 lip = new Vec3(Double.parseDouble(h[0]), Double.parseDouble(h[1]), Double.parseDouble(h[2]));
			Vec3 land = new Vec3(Double.parseDouble(h[3]), Double.parseDouble(h[4]), Double.parseDouble(h[5]));
			List<Glob> globs = new ArrayList<>();
			for (int i = 1; i < parts.length; i++) {
				String[] p = parts[i].split(",");
				globs.add(new Glob(Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]), Long.parseLong(p[4]),
						Integer.parseInt(p[5]), Integer.parseInt(p[6]), Long.parseLong(p[7]), "1".equals(p[8])));
			}
			return new Shown(lip, land, globs);
		} catch (RuntimeException ex) {
			return Shown.NONE;
		}
	}
}
