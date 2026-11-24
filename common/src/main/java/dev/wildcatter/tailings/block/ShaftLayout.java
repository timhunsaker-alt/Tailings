package dev.wildcatter.tailings.block;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.shaft.Hoisting;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Where a shaft's pieces stand (docs/multiblocks/shaft-and-hoist.md, "How it runs"), as the hoist house finds them: the headframe
 * ahead of it within the rope's reach, the shaft under the headframe, the measuring flask beside the shaft bottom, and the headframe
 * bin touching the headframe. The headframe's own frame faces north from its main cell (x east, z south): the shaft is the 5 x 5 in
 * its middle, x and z 2 to 6; skip compartment 0 is x 2-3, compartment 1 x 5-6, both z 3-4; its ropes lead off to the south, where the
 * hoist house stands.
 */
public final class ShaftLayout {
	/** The shaft's cells in the headframe's own frame, and the skips' compartments. */
	public static final int HOLE_FROM = 2, HOLE_TO = 6;
	/** Compartments 0 and 1 are the skips', 2 the cage's, beside them on the shaft's south side (heavy-iron-fixes). */
	private static final int[] COMPARTMENT_X = {2, 5, 3};
	private static final int[] COMPARTMENT_Z = {3, 3, 5};
	public static final int CAGE = 2;

	/** A headframe's geometry, from its main cell and facing. */
	public record Frame(BlockPos main, Direction facing) {
		public static Frame of(PlantBlockEntity headframe) {
			return new Frame(headframe.getBlockPos(), headframe.getBlockState().getValue(BigPartBlock.FACING));
		}

		public int collarY() {
			return main.getY();
		}

		/** A cell in the headframe's own frame. */
		public BlockPos cell(int x, int y, int z) {
			return main.offset(TbmMachine.offset(facing, x, y, z));
		}

		/** A point in the headframe's model, metres from its main cell's corner, in the world. */
		public Vec3 point(double x, double y, double z) {
			BlockPos ax = TbmMachine.offset(facing, 1, 0, 0), az = TbmMachine.offset(facing, 0, 0, 1);
			double px = x - 0.5, pz = z - 0.5;
			return new Vec3(main.getX() + 0.5 + px * ax.getX() + pz * az.getX(), main.getY() + y, main.getZ() + 0.5 + px * ax.getZ() + pz * az.getZ());
		}

		/** The corner of a skip's main cell in compartment {@code which}, its bottom at {@code y}. */
		public Vec3 skipCorner(int which, double y) {
			BlockPos at = cell(COMPARTMENT_X[which], 0, COMPARTMENT_Z[which]);
			return new Vec3(at.getX(), y, at.getZ());
		}

		/** The cells of a compartment's column at height {@code y}. */
		public List<BlockPos> compartment(int which, int y) {
			List<BlockPos> out = new ArrayList<>();
			for (int dx = 0; dx < 2; dx++) {
				for (int dz = 0; dz < 2; dz++) {
					out.add(cell(COMPARTMENT_X[which] + dx, y - collarY(), COMPARTMENT_Z[which] + dz));
				}
			}
			return out;
		}

		/** The shaft, from the deepest pocket the hoist looks for up to the top of the headframe. */
		public AABB shaftBox() {
			BlockPos a = cell(HOLE_FROM, -Hoisting.MAX_DEPTH - 1, HOLE_FROM), b = cell(HOLE_TO, Machine.HEADFRAME.height(), HOLE_TO);
			return new AABB(Math.min(a.getX(), b.getX()), a.getY(), Math.min(a.getZ(), b.getZ()), Math.max(a.getX(), b.getX()) + 1, b.getY() + 1,
					Math.max(a.getZ(), b.getZ()) + 1);
		}

		/** The cells round the shaft at height {@code y}: where a flask stands beside it. */
		public List<BlockPos> ring(int y) {
			List<BlockPos> out = new ArrayList<>();
			for (int x = HOLE_FROM - 1; x <= HOLE_TO + 1; x++) {
				for (int z = HOLE_FROM - 1; z <= HOLE_TO + 1; z++) {
					if (x == HOLE_FROM - 1 || x == HOLE_TO + 1 || z == HOLE_FROM - 1 || z == HOLE_TO + 1) {
						out.add(cell(x, y - collarY(), z));
					}
				}
			}
			return out;
		}

		/** {@code pos} in the headframe's own frame, {x, y, z}. */
		public int[] local(BlockPos pos) {
			int dx = pos.getX() - main.getX(), dz = pos.getZ() - main.getZ();
			for (int i = 0; i < dev.wildcatter.tailings.block.DewateringPumpBlock.turns(facing); i++) {
				int t = dx;
				dx = dz;
				dz = -t;
			}
			return new int[] {dx, pos.getY() - main.getY(), dz};
		}

		/** A world point in this frame: {x, y, z} metres from the main cell's corner (the inverse of {@link #point}). */
		public double[] local(Vec3 world) {
			BlockPos ax = TbmMachine.offset(facing, 1, 0, 0), az = TbmMachine.offset(facing, 0, 0, 1);
			double wx = world.x - main.getX() - 0.5, wz = world.z - main.getZ() - 0.5;
			return new double[] {wx * ax.getX() + wz * ax.getZ() + 0.5, world.y - main.getY(), wx * az.getX() + wz * az.getZ() + 0.5};
		}

		/** The top of the head sheaves, where the ropes leave them. */
		public double sheaveTop() {
			return collarY() + Hoisting.SHEAVE_HEIGHT + SHEAVE_RADIUS;
		}

		/** The top of compartment {@code which}'s head sheave, where its rope leaves it for the drum (shaft.py: x 3 and 6, 5.5 m south). */
		public Vec3 sheaveTopPoint(int which) {
			return point(COMPARTMENT_X[which] + 1.0, Hoisting.SHEAVE_HEIGHT + SHEAVE_RADIUS, SHEAVE_Z);
		}

		/** The top of the cage sheave. */
		public Vec3 cageSheaveTopPoint() {
			return point(CAGE_SHEAVE_X, CAGE_SHEAVE_Y + Hoisting.CAGE_SHEAVE_DIAMETER / 2, CAGE_SHEAVE_Z);
		}
	}

	/** The head sheaves' axle, m south of the headframe's main cell's corner (shaft.py). */
	public static final double SHEAVE_Z = 5.5;

	/** The head sheaves' radius, m (3 m sheaves, as the drums; proposed). */
	public static final double SHEAVE_RADIUS = 1.5;

	public final @Nullable Frame frame;
	public final @Nullable PlantBlockEntity headframe;
	public final @Nullable PlantBlockEntity bin;
	public final @Nullable PlantBlockEntity flask;
	/** The loading stop: the flask's bottom, where the skip's bottom stands to load. */
	public final int bottomY;
	public final double travel;
	/** Where the ropes come off the drums, for drawing them. */
	public final @Nullable BlockPos drum;
	/** The hoist house's (or the cage winder's) own frame, where its drums are (fix3-hi07: each rope to its own drum). */
	public @Nullable Frame drums;
	/** What is missing, for NOT BUILT; null when the shaft is whole. */
	public final @Nullable String problem;

	private ShaftLayout(@Nullable Frame frame, @Nullable PlantBlockEntity headframe, @Nullable PlantBlockEntity bin, @Nullable PlantBlockEntity flask, int bottomY,
			@Nullable BlockPos drum, @Nullable String problem) {
		this.frame = frame;
		this.headframe = headframe;
		this.bin = bin;
		this.flask = flask;
		this.bottomY = bottomY;
		this.travel = frame == null ? 0 : frame.collarY() + Hoisting.DUMP_HEIGHT - bottomY;
		this.drum = drum;
		this.problem = problem;
	}

	private static ShaftLayout missing(String problem) {
		return new ShaftLayout(null, null, null, null, 0, null, problem);
	}

	/** True while every piece it found still stands. */
	public boolean stillStands() {
		return (headframe == null || !headframe.isRemoved()) && (bin == null || !bin.isRemoved()) && (flask == null || !flask.isRemoved());
	}

	/** Finds the shaft a hoist house works. */
	public static ShaftLayout find(PlantBlockEntity hoist) {
		Level level = hoist.getLevel();
		List<BlockPos> cells = hoist.cells();
		int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, minZ = Integer.MAX_VALUE, maxZ = Integer.MIN_VALUE, base = hoist.getBlockPos().getY();
		for (BlockPos c : cells) {
			minX = Math.min(minX, c.getX());
			maxX = Math.max(maxX, c.getX());
			minZ = Math.min(minZ, c.getZ());
			maxZ = Math.max(maxZ, c.getZ());
		}
		int cx = (minX + maxX) / 2, cz = (minZ + maxZ) / 2;
		PlantBlockEntity headframe = null;
		Direction toward = null;
		search:
		for (Direction d : Direction.Plane.HORIZONTAL) {
			int edge = switch (d) {
				case NORTH -> minZ;
				case SOUTH -> maxZ;
				case WEST -> minX;
				default -> maxX;
			};
			for (int k = 1; k <= Hoisting.ROPE_REACH; k++) {
				int x = d.getAxis() == Direction.Axis.X ? edge + d.getStepX() * k : cx;
				int z = d.getAxis() == Direction.Axis.Z ? edge + d.getStepZ() * k : cz;
				BlockPos at = new BlockPos(x, base, z);
				if (!level.isLoaded(at)) {
					break;
				}
				for (int dy = 0; dy < Machine.HOIST_HOUSE.height(); dy++) {
					if (BigParts.entity(level, at.above(dy)) instanceof PlantBlockEntity part && part.machine() == Machine.HEADFRAME) {
						headframe = part;
						toward = d;
						break search;
					}
				}
			}
		}
		if (headframe == null) {
			return missing("needs a Headframe in line with its drums, within " + Hoisting.ROPE_REACH + " m");
		}
		Frame frame = Frame.of(headframe);
		ShaftLayout found = findIn(level, hoist, frame, headframe, toward, cx, cz, minX, maxX, minZ, maxZ, base);
		found.drums = Frame.of(hoist);
		return found;
	}

	private static ShaftLayout findIn(Level level, PlantBlockEntity hoist, Frame frame, PlantBlockEntity headframe, Direction toward, int cx, int cz, int minX, int maxX,
			int minZ, int maxZ, int base) {
		int[] at = frame.local(new BlockPos(cx, base, cz));
		BlockPos drum = new BlockPos(toward.getAxis() == Direction.Axis.X ? (toward.getStepX() > 0 ? maxX : minX) : cx, base + 4,
				toward.getAxis() == Direction.Axis.Z ? (toward.getStepZ() > 0 ? maxZ : minZ) : cz);
		if (at[2] <= HOLE_TO + 2 || at[0] < HOLE_FROM - 2 || at[0] > HOLE_TO + 2) {
			return new ShaftLayout(frame, headframe, null, null, 0, drum, "needs to stand behind the Headframe, on its rope side, in line with the shaft");
		}
		PlantBlockEntity bin = headframe.touching(Machine.HEADFRAME_BIN);
		if (bin == null) {
			return new ShaftLayout(frame, headframe, null, null, 0, drum, "needs a Headframe Bin touching the headframe, for the skips to dump into");
		}
		return bottom(level, frame, headframe, bin, drum, new int[] {0, 1}, "the skips need their compartments clear down to the flask");
	}

	/** The measuring flask beside the shaft bottom, and the compartments {@code which} clear from it up to the collar. */
	private static ShaftLayout bottom(Level level, Frame frame, PlantBlockEntity headframe, @Nullable PlantBlockEntity bin, BlockPos drum, int[] which, String blocked) {
		PlantBlockEntity flask = null;
		for (int y = frame.collarY() - 1; y >= frame.collarY() - Hoisting.MAX_DEPTH && flask == null && y >= level.getMinY(); y--) {
			for (BlockPos ring : frame.ring(y)) {
				if (level.isLoaded(ring) && BigParts.entity(level, ring) instanceof PlantBlockEntity part && part.machine() == Machine.MEASURING_FLASK) {
					flask = part;
					break;
				}
			}
		}
		if (flask == null) {
			return new ShaftLayout(frame, headframe, bin, null, 0, drum, "needs a Measuring Flask beside the shaft bottom, within " + Hoisting.MAX_DEPTH + " m of the collar");
		}
		int bottom = flask.getBlockPos().getY();
		for (int y = bottom; y < frame.collarY(); y++) {
			for (int w : which) {
				for (BlockPos c : frame.compartment(w, y)) {
					if (!level.getBlockState(c).getCollisionShape(level, c).isEmpty()) {
						return new ShaftLayout(frame, headframe, bin, flask, bottom, drum, "the shaft is blocked at y " + y + ": " + blocked);
					}
				}
			}
		}
		return new ShaftLayout(frame, headframe, bin, flask, bottom, drum, null);
	}

	// ---- the cage winder (cage-winder.md) ---------------------------------------------------------------------------------------

	/** How far east or west of the shaft's centre line a cage winder may stand, m: in or beside the hoist house's line. */
	public static final int CAGE_WINDER_ASIDE = 20;
	/** The cage winder's drum in its own frame (facing north from its main cell): its axis x 2 to 5, its centre 2.25 m up, 2.5 m south. */
	public static final double CAGE_DRUM_X = 3.5, CAGE_DRUM_Y = 2.25, CAGE_DRUM_Z = 2.5;
	/** The cage sheave in the headframe's frame: over the cage's compartment, its axle 26.95 m up and 7.25 m south (shaft.py). */
	public static final double CAGE_SHEAVE_X = 4.0, CAGE_SHEAVE_Y = 26.95, CAGE_SHEAVE_Z = 7.25;

	/** The top of the cage sheave, where the cage's rope leaves it. */
	public static double cageSheaveTop(Frame frame) {
		return frame.collarY() + CAGE_SHEAVE_Y + Hoisting.CAGE_SHEAVE_DIAMETER / 2;
	}

	/**
	 * Finds the shaft a cage winder works: the nearest headframe whose rope side it stands on, within the rope's reach and
	 * {@link #CAGE_WINDER_ASIDE} of its line ({@code known} first, if it still stands there); the flask's landing, and the cage's
	 * compartment clear. No bin is needed.
	 */
	public static ShaftLayout findCage(PlantBlockEntity winder, @Nullable PlantBlockEntity known) {
		Level level = winder.getLevel();
		BlockPos main = winder.getBlockPos();
		Direction facing = winder.getBlockState().getValue(BigPartBlock.FACING);
		BlockPos drumCell = main.offset(TbmMachine.offset(facing, (int) Math.floor(CAGE_DRUM_X), (int) Math.floor(CAGE_DRUM_Y + Hoisting.CAGE_DRUM_DIAMETER / 2),
				(int) Math.floor(CAGE_DRUM_Z)));
		PlantBlockEntity headframe = known != null && !known.isRemoved() && serves(known, drumCell) ? known : null;
		if (headframe == null) {
			int reach = Hoisting.ROPE_REACH + 16;
			double best = Double.MAX_VALUE;
			for (int cx = (main.getX() - reach) >> 4; cx <= (main.getX() + reach) >> 4; cx++) {
				for (int cz = (main.getZ() - reach) >> 4; cz <= (main.getZ() + reach) >> 4; cz++) {
					if (!level.hasChunk(cx, cz)) {
						continue;
					}
					for (net.minecraft.world.level.block.entity.BlockEntity be : level.getChunk(cx, cz).getBlockEntities().values()) {
						if (be instanceof PlantBlockEntity part && part.machine() == Machine.HEADFRAME && !part.isRemoved() && serves(part, drumCell)) {
							double d = part.getBlockPos().distSqr(main);
							if (d < best) {
								best = d;
								headframe = part;
							}
						}
					}
				}
			}
		}
		if (headframe == null) {
			return new ShaftLayout(null, null, null, null, 0, drumCell, "needs a Headframe within " + Hoisting.ROPE_REACH + " m, standing behind it on its rope side, within "
					+ CAGE_WINDER_ASIDE + " m of its line");
		}
		Frame frame = Frame.of(headframe);
		ShaftLayout found = bottom(level, frame, headframe, null, drumCell, new int[] {CAGE}, "the cage needs its compartment clear down to the flask");
		found.drums = Frame.of(winder);
		return found;
	}

	/** A headframe a cage winder's drum at {@code drum} can serve: behind it on its rope side, in reach and near its line. */
	private static boolean serves(PlantBlockEntity headframe, BlockPos drum) {
		Frame frame = Frame.of(headframe);
		int[] at = frame.local(drum);
		return at[2] > HOLE_TO + 2 && at[2] - HOLE_TO <= Hoisting.ROPE_REACH && Math.abs(at[0] - CAGE_SHEAVE_X) <= CAGE_WINDER_ASIDE
				&& Math.abs(at[1]) <= Machine.HOIST_HOUSE.height();
	}

	// ---- where the rock and the ropes run (fix3-hi07; the figures in shaft.ShaftGeometry) ------------------------------------------

	/** The middle of a headframe bin's mouth, at its rim (shaft.py: the bin 4 x 4, its rim 5 m up). */
	public static Vec3 binMouth(PlantBlockEntity bin) {
		return Frame.of(bin).point(2.0, 5.0, 2.0);
	}

	/**
	 * The pour's centre line in the world, from the skip in compartment {@code which} at the dump down the chute and into the bin's mouth
	 * at {@code mouth}: {@link dev.wildcatter.tailings.shaft.ShaftGeometry#pourPath}, set in the headframe. The renderer draws it,
	 * the server's particles leave its lip, and the tests check it clears the chute.
	 */
	public static List<Vec3> pourPath(Frame frame, int which, Vec3 mouth) {
		double[] m = frame.local(mouth);
		List<Vec3> out = new ArrayList<>();
		for (double[] p : dev.wildcatter.tailings.shaft.ShaftGeometry.pourPath(COMPARTMENT_X[which] + 1.0, m[0], m[1], m[2])) {
			out.add(frame.point(p[0], p[1], p[2]));
		}
		return out;
	}

	/** Which of the hoist house's two drums compartment {@code which}'s rope winds on: the one nearer its sheave, the other rope the other. */
	public int drumFor(int which) {
		if (frame == null || drums == null) {
			return which;
		}
		double s0 = drums.local(frame.sheaveTopPoint(0))[0];
		double[][] d = dev.wildcatter.tailings.shaft.ShaftGeometry.HOIST_DRUMS;
		int nearer0 = Math.abs(s0 - (d[0][0] + d[0][1]) / 2) <= Math.abs(s0 - (d[1][0] + d[1][1]) / 2) ? 0 : 1;
		return which == 0 ? nearer0 : 1 - nearer0;
	}

	/**
	 * Where compartment {@code which}'s rope meets its own drum in the world, with {@code wound} m on it: the true tangent from its
	 * sheave's top, at the coil's front. The west compartment's rope winds over its drum's top and the east one's under its bottom, so
	 * on one shaft one pays out while the other takes in. Null without a hoist house.
	 */
	public @Nullable Vec3 ropeEnd(int which, double wound) {
		if (frame == null || drums == null) {
			return null;
		}
		double[] s = drums.local(frame.sheaveTopPoint(which));
		double[] d = dev.wildcatter.tailings.shaft.ShaftGeometry.HOIST_DRUMS[drumFor(which)];
		double[] e = dev.wildcatter.tailings.shaft.ShaftGeometry.ropeEnd(d[0], d[1], dev.wildcatter.tailings.shaft.ShaftGeometry.HOIST_DRUM_Y,
				dev.wildcatter.tailings.shaft.ShaftGeometry.HOIST_DRUM_Z, dev.wildcatter.tailings.shaft.ShaftGeometry.HOIST_DRUM_R, s[0], s[1], s[2], wound, which == 0);
		return e == null ? null : drums.point(e[0], e[1], e[2]);
	}

	/** Where the cage's rope meets the cage winder's drum, over its top, with {@code wound} m on it. Null without a cage winder. */
	public @Nullable Vec3 cageRopeEnd(double wound) {
		if (frame == null || drums == null) {
			return null;
		}
		double[] s = drums.local(frame.cageSheaveTopPoint());
		double[] d = dev.wildcatter.tailings.shaft.ShaftGeometry.CAGE_DRUM;
		double[] e = dev.wildcatter.tailings.shaft.ShaftGeometry.ropeEnd(d[0], d[1], dev.wildcatter.tailings.shaft.ShaftGeometry.CAGE_DRUM_Y,
				dev.wildcatter.tailings.shaft.ShaftGeometry.CAGE_DRUM_Z, dev.wildcatter.tailings.shaft.ShaftGeometry.CAGE_DRUM_R, s[0], s[1], s[2], wound, true);
		return e == null ? null : drums.point(e[0], e[1], e[2]);
	}
}
