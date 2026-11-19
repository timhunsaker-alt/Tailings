package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildcatter.tailings.entity.PitVehicleEntity;
import dev.wildcatter.tailings.pit.Pour;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Draws a machine's load pouring out (docs/multiblocks/open-pit.md, Pouring) as one thick wet sheet: a stream off its source (a
 * truck's tail lip, a dozer's blade, a dragline's bucket),
 * and a smooth surface over every column the load lies on or runs over, its height shared between neighbours and slumping to the
 * ground at its edges, dark and glossy in the load's own texture. Over a cell about to set the sheet thins to the cell's top, and
 * when the server sets the real block the column is dry. Nothing here is in the world: it is drawn from the times the server sent.
 */
public final class PourRenderer {
	/** A quad of the sheet: four vertices of x, y, z (from the truck), u, v, nx, ny, nz and shade, and the atlas it is drawn from. */
	public record Quad(float[] v, net.minecraft.resources.Identifier atlas) {
	}

	public static final Quad[] NONE = new Quad[0];
	/** How thick the wet sheet stands over a cell still running, and over a cell it has run past. */
	private static final double FRONT = 0.55, FILM = 0.2;
	/** The film over a cell that has not started to set. */
	private static final double OVER = 0.2;
	private static final double STREAM = 0.4;

	private PourRenderer() {
	}

	/** One column of the sheet: its surface, its sprite, and how far it has dried (0 wet, 1 set). */
	private static final class Col {
		double surf = Double.NEGATIVE_INFINITY;
		TextureAtlasSprite sprite;
		double dry;

		void wet(double s, TextureAtlasSprite sp, double d) {
			if (s > surf) {
				surf = s;
				sprite = sp;
				dry = d;
			}
		}
	}

	/** The sheet and streams of a machine's pour at this frame, from where the machine is drawn ({@code ox, oy, oz}). */
	public static Quad[] mesh(PitVehicleEntity truck, float partialTick, double ox, double oy, double oz) {
		Pour.Shown shown = truck.shownPour();
		if (shown.globs().isEmpty()) {
			return NONE;
		}
		Level level = truck.level();
		double now = level.getGameTime() + partialTick;
		Map<Long, Col> cols = new HashMap<>();
		Map<Long, Double> tops = new HashMap<>();
		int highest = Integer.MIN_VALUE;
		for (Pour.Glob g : shown.globs()) {
			highest = Math.max(highest, Math.max(g.y() + 1, (int) Math.floor(g.land().y)));
		}
		final int from = highest + 3;
		java.util.function.BiFunction<Integer, Integer, Double> top = (x, z) -> tops.computeIfAbsent(key(x, z), k -> ground(level, x, z, from));
		// one stream from each source that has load falling from it now
		Map<String, Object[]> streams = new java.util.LinkedHashMap<>();
		for (Pour.Glob g : shown.globs()) {
			double dt = now - g.launch();
			if (dt < 0) {
				continue;
			}
			TextureAtlasSprite sprite = sprite(Block.stateById(g.state()));
			Vec3 lip = g.lip(), land = g.land();
			double landY = land.y, lx = land.x, lz = land.z;
			if (dt < g.fall() + 2) {
				streams.put(lip + "|" + land, new Object[] {lip, land, g.wide(), sprite});
			}
			if (dt < g.fall() || g.kind() == Pour.BODY) {
				continue;   // falling, or onto a truck's load, which the truck draws
			}
			if (g.kind() == Pour.POCKET) {
				// a pool where it lands in the pocket, sinking in
				double s = (dt - g.fall()) / 10.0;
				if (s < 1) {
					int x = (int) Math.floor(lx), z = (int) Math.floor(lz);
					cols.computeIfAbsent(key(x, z), k -> new Col()).wet(top.apply(x, z) + 0.35 * (1 - s), sprite, 0);
				}
				continue;
			}
			double cx = g.x() + 0.5, cz = g.z() + 0.5;
			double arrive = g.launch() + g.fall() + g.run();
			if (now < arrive) {
				// running out over the ground and the pile: a thick front and a film behind it
				double s = (dt - g.fall()) / Math.max(1, g.run());
				double run = Math.hypot(cx - lx, cz - lz);
				for (double d = 0; d <= run * s + 1e-6; d += 0.5) {
					double f = run < 1e-6 ? 0 : d / run;
					int x = (int) Math.floor(lx + (cx - lx) * f), z = (int) Math.floor(lz + (cz - lz) * f);
					boolean front = d + 0.5 > run * s;
					cols.computeIfAbsent(key(x, z), k -> new Col()).wet(top.apply(x, z) + (front ? FRONT : FILM), sprite, 0);
				}
				continue;
			}
			if (now >= g.set() && (now > g.set() + 4 || !level.getBlockState(new BlockPos(g.x(), g.y(), g.z())).isAir())) {
				continue;   // set: the real block is there
			}
			// lying at its cell, under a film that thins to the cell's top as it sets
			double p = Math.max(0, Math.min(1, (now - (g.set() - Pour.SET)) / Pour.SET));
			cols.computeIfAbsent(key(g.x(), g.z()), k -> new Col()).wet(g.y() + 1.0 + OVER * (1 - p), sprite, p);
		}
		List<Quad> out = new ArrayList<>();
		Vec3 eye = Minecraft.getInstance().gameRenderer.mainCamera().position();
		sheet(cols, top, out, ox, oy, oz, eye);
		for (Object[] st : streams.values()) {
			Vec3 lip = (Vec3) st[0], to = (Vec3) st[1];
			stream(out, (TextureAtlasSprite) st[3], (float) st[2], truck.getYRot(partialTick), lip.x - ox, lip.y - oy, lip.z - oz, to.x - ox, to.y - oy, to.z - oz);
		}
		return out.toArray(NONE);
	}

	private static long key(int x, int z) {
		return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
	}

	private static TextureAtlasSprite sprite(BlockState state) {
		return Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();
	}

	/** The top of what stands in a column, from {@code from} down. */
	private static double ground(Level level, int x, int z, int from) {
		BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
		for (int y = from; y > from - 24; y--) {
			at.set(x, y, z);
			VoxelShape shape = level.getBlockState(at).getCollisionShape(level, at);
			if (!shape.isEmpty()) {
				return y + shape.max(Direction.Axis.Y);
			}
		}
		return from - 24;
	}

	/**
	 * The sheet over the wet columns: each column four quads, its middle at its surface, its corners and edge middles shared with its
	 * neighbours; where a neighbour is dry the sheet comes down to that neighbour's ground (or stays level against a higher one), so
	 * the front is a slumped, rounded edge. Normals from the heights either side, so it shades smoothly.
	 */
	private static void sheet(Map<Long, Col> cols, java.util.function.BiFunction<Integer, Integer, Double> top, List<Quad> out, double ox, double oy, double oz,
			Vec3 eye) {
		if (cols.isEmpty()) {
			return;
		}
		Map<Long, Double> lattice = new HashMap<>();
		// a half-cell lattice: (2x, 2z) is a column's corner, (2x + 1, 2z + 1) its middle
		java.util.function.BiFunction<Integer, Integer, Double> h = (i, j) -> lattice.computeIfAbsent(key(i, j), k -> height(cols, top, i, j));
		for (Map.Entry<Long, Col> e : cols.entrySet()) {
			int x = (int) (e.getKey() >> 32), z = (int) (long) e.getKey();
			Col c = e.getValue();
			int[][] quads = {{0, 0, 1, 0, 1, 1, 0, 1}, {1, 0, 2, 0, 2, 1, 1, 1}, {1, 1, 2, 1, 2, 2, 1, 2}, {0, 1, 1, 1, 1, 2, 0, 2}};
			for (int[] q : quads) {
				float[] v = new float[4 * 9];
				int[] order = {3, 2, 1, 0};
				for (int n = 0; n < 4; n++) {
					int k = order[n];
					int i = 2 * x + q[k * 2], j = 2 * z + q[k * 2 + 1];
					double y = h.apply(i, j);
					double dx = h.apply(i + 1, j) - h.apply(i - 1, j), dz = h.apply(i, j + 1) - h.apply(i, j - 1);
					double nx = -dx, ny = 1.0, nz = -dz, len = Math.sqrt(nx * nx + ny * ny + nz * nz);
					nx /= len;
					ny /= len;
					nz /= len;
					double wx = i / 2.0, wz = j / 2.0;
					int b = n * 9;
					v[b] = (float) (wx - ox);
					v[b + 1] = (float) (y - oy);
					v[b + 2] = (float) (wz - oz);
					v[b + 3] = c.sprite.getU((float) (q[k * 2] / 2.0));
					v[b + 4] = c.sprite.getV((float) (q[k * 2 + 1] / 2.0));
					v[b + 5] = (float) nx;
					v[b + 6] = (float) ny;
					v[b + 7] = (float) nz;
					v[b + 8] = (float) shade(c.dry, nx, ny, nz, wx, y, wz, eye);
				}
				out.add(new Quad(v, c.sprite.atlasLocation()));
			}
		}
	}

	/** A lattice point's height: a column's middle is its surface; a corner or edge middle is shared by the columns around it. */
	private static double height(Map<Long, Col> cols, java.util.function.BiFunction<Integer, Integer, Double> top, int i, int j) {
		boolean midX = (i & 1) == 1, midZ = (j & 1) == 1;
		int[] xs = midX ? new int[] {(i - 1) >> 1} : new int[] {(i >> 1) - 1, i >> 1};
		int[] zs = midZ ? new int[] {(j - 1) >> 1} : new int[] {(j >> 1) - 1, j >> 1};
		double wetSum = 0, wetMax = Double.NEGATIVE_INFINITY;
		int wet = 0;
		double dryLow = Double.POSITIVE_INFINITY;
		for (int x : xs) {
			for (int z : zs) {
				Col c = cols.get(key(x, z));
				if (c != null) {
					wetSum += c.surf;
					wetMax = Math.max(wetMax, c.surf);
					wet++;
				} else {
					dryLow = Math.min(dryLow, top.apply(x, z) + 0.02);
				}
			}
		}
		if (wet == 0) {
			return dryLow;
		}
		double mean = wetSum / wet;
		if (wet == xs.length * zs.length) {
			return mean;
		}
		// at the edge of the sheet: down to the dry ground beside it, or level with the sheet against a higher dry column
		return dryLow < mean ? dryLow : mean;
	}

	/** Wet load is dark and glossy: a dark base, a highlight where the surface mirrors the sky toward the eye; drier as it sets. */
	private static double shade(double dry, double nx, double ny, double nz, double x, double y, double z, Vec3 eye) {
		double base = 0.5 + 0.4 * dry;
		if (eye == null) {
			return base;
		}
		double vx = eye.x - x, vy = eye.y - y, vz = eye.z - z, vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
		double lx = 0.3, ly = 1.0, lz = 0.25, ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
		double hx = vx / vl + lx / ll, hy = vy / vl + ly / ll, hz = vz / vl + lz / ll, hl = Math.sqrt(hx * hx + hy * hy + hz * hz);
		double spec = Math.pow(Math.max(0, (nx * hx + ny * hy + nz * hz) / hl), 18) * 0.5 * (1 - dry);
		return Math.min(1, base + spec);
	}

	/**
	 * The stream off the lip: a flat, rounded ribbon of the load as wide as most of the body, from the lip down to where it lands,
	 * narrowing as it falls.
	 */
	private static void stream(List<Quad> out, TextureAtlasSprite sp, float across, float yaw, double x0, double y0, double z0, double x1, double y1, double z1) {
		int sides = 12, rings = 6;
		// across the way it falls, or across the machine when it falls straight down
		double hx = x1 - x0, hz = z1 - z0, hl = Math.hypot(hx, hz);
		double turn = Math.toRadians(yaw);
		double ax = hl > 0.3 ? hx / hl : -Math.sin(turn), az = hl > 0.3 ? hz / hl : Math.cos(turn);
		double rx = -az, rz = ax;
		for (int r = 0; r < rings; r++) {
			double t0 = (double) r / rings, t1 = (double) (r + 1) / rings;
			double r0 = STREAM * (1.1 - 0.35 * t0), r1 = STREAM * (1.1 - 0.35 * t1);
			// it falls in a curve: out from the lip, then down
			double[] a = {x0 + (x1 - x0) * Math.sqrt(t0), y0 + (y1 - y0) * t0 * t0, z0 + (z1 - z0) * Math.sqrt(t0)};
			double[] b = {x0 + (x1 - x0) * Math.sqrt(t1), y0 + (y1 - y0) * t1 * t1, z0 + (z1 - z0) * Math.sqrt(t1)};
			for (int s = 0; s < sides; s++) {
				double p0 = 2 * Math.PI * s / sides, p1 = 2 * Math.PI * (s + 1) / sides;
				float[] v = new float[4 * 9];
				double[][] pts = {{p0, 0}, {p1, 0}, {p1, 1}, {p0, 1}};
				for (int n = 0; n < 4; n++) {
					double ang = pts[n][0];
					boolean lower = pts[n][1] > 0;
					double[] c = lower ? b : a;
					double rad = lower ? r1 : r0;
					// an ellipse: wide across the tail, thin along it
					double wide = Math.max(rad, across / 2.0 * rad / STREAM), thin = rad;
					double ox2 = rx * Math.cos(ang) * wide + ax * Math.sin(ang) * thin, oz2 = rz * Math.cos(ang) * wide + az * Math.sin(ang) * thin;
					double nx = rx * Math.cos(ang) * thin + ax * Math.sin(ang) * wide, nz = rz * Math.cos(ang) * thin + az * Math.sin(ang) * wide;
					double nl = Math.max(1e-6, Math.hypot(nx, nz));
					nx /= nl;
					nz /= nl;
					int k = n * 9;
					v[k] = (float) (c[0] + ox2);
					v[k + 1] = (float) c[1];
					v[k + 2] = (float) (c[2] + oz2);
					v[k + 3] = sp.getU((float) (s / (double) sides));
					v[k + 4] = sp.getV((float) (lower ? t1 : t0));
					v[k + 5] = (float) nx;
					v[k + 6] = 0;
					v[k + 7] = (float) nz;
					v[k + 8] = (float) (0.55 + 0.25 * Math.max(0, nx * 0.3 + nz * 0.25));
				}
				out.add(new Quad(v, sp.atlasLocation()));
			}
		}
	}

	static void submit(Quad[] quads, PoseStack pose, SubmitNodeCollector collector, int light) {
		if (quads.length == 0) {
			return;
		}
		java.util.Set<net.minecraft.resources.Identifier> atlases = new java.util.LinkedHashSet<>();
		for (Quad q : quads) {
			atlases.add(q.atlas());
		}
		for (net.minecraft.resources.Identifier atlas : atlases) {
			// no culling: the sheet is seen from above and the stream from every side
			collector.submitCustomGeometry(pose, net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(atlas), (at, v) -> {
				for (Quad q : quads) {
					if (q.atlas().equals(atlas)) {
						for (int n = 0; n < 4; n++) {
							vertex(at, v, q.v(), n * 9, light);
						}
					}
				}
			});
		}
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer v, float[] q, int b, int light) {
		int shade = Math.max(0, Math.min(255, Math.round(255 * q[b + 8])));
		int color = 0xFF000000 | shade << 16 | shade << 8 | shade;
		v.addVertex(pose, q[b], q[b + 1], q[b + 2]).setColor(color).setUv(q[b + 3], q[b + 4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
				.setNormal(pose, q[b + 5], q[b + 6], q[b + 7]);
	}
}
