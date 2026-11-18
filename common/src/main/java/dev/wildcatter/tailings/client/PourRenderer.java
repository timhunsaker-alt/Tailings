package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.pit.Pour;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a haul truck's load pouring out (docs/multiblocks/open-pit.md, Pouring): each block of the load a lump of its own texture,
 * dark and wet, that slides off the tail, drops, runs over the ground and the pile to its cell, lies there thickening and drying,
 * and is gone when the server sets the real block in its place. Nothing here is in the world: it is drawn from the times the
 * server sent.
 */
public final class PourRenderer {
	/**
	 * One lump to draw: its middle's foot from the truck, its half width, height and half length, its heading, shade and sprite; a
	 * mound while it moves, a flat sheet once it lies at its cell.
	 */
	public record Lump(float x, float y, float z, float hw, float h, float hl, float yaw, float shade, TextureAtlasSprite sprite, boolean sheet) {
	}

	private static final Lump[] NONE = new Lump[0];

	private PourRenderer() {
	}

	/** The lumps of a truck's pour at this frame, from where the truck is drawn ({@code ox, oy, oz}). */
	public static Lump[] lumps(HaulTruckEntity truck, float partialTick, double ox, double oy, double oz) {
		Pour.Shown shown = truck.shownPour();
		if (shown.globs().isEmpty()) {
			return NONE;
		}
		Level level = truck.level();
		double now = level.getGameTime() + partialTick;
		Vec3 lip = shown.lip(), land = shown.land();
		List<Lump> out = new ArrayList<>();
		int i = 0;
		for (Pour.Glob g : shown.globs()) {
			i++;
			double dt = now - g.launch();
			if (dt < 0) {
				continue;
			}
			BlockState state = Block.stateById(g.state());
			TextureAtlasSprite sprite = Minecraft.getInstance().getModelManager().getBlockStateModelSet().getParticleMaterial(state).sprite();
			// a little to either side, by its number, so the lumps do not all run on one line
			double jx = ((i * 37 % 11) - 5) * 0.06, jz = ((i * 53 % 13) - 6) * 0.05;
			double landY = g.into() ? g.y() + 1.0 : land.y;
			double lx = g.into() ? lip.x + jx : land.x + jx, lz = g.into() ? lip.z + jz : land.z + jz;
			if (dt < g.fall()) {
				// off the tail and down, faster as it falls, stretched as it goes
				double s = dt / g.fall();
				double x = lip.x + (lx - lip.x) * s, z = lip.z + (lz - lip.z) * s, y = lip.y + (landY - lip.y) * s * s;
				out.add(new Lump((float) (x - ox), (float) (y - oy - 0.3), (float) (z - oz), 0.42F, 0.75F + 0.3F * (float) s, 0.42F, 0, 0.6F, sprite, false));
				continue;
			}
			if (g.into()) {
				// into the pocket: it spreads and sinks into what is there
				double s = (dt - g.fall()) / 6.0;
				if (s < 1) {
					out.add(new Lump((float) (lx - ox), (float) (landY - oy), (float) (lz - oz), 0.5F + 0.3F * (float) s, 0.4F * (float) (1 - s), 0.5F + 0.3F * (float) s, 0,
							0.6F, sprite, false));
				}
				continue;
			}
			double cx = g.x() + 0.5, cz = g.z() + 0.5;
			double arrive = g.launch() + g.fall() + g.run();
			if (now < arrive) {
				// running over the ground and the pile, a long low lump
				double s = (dt - g.fall()) / Math.max(1, g.run());
				double x = lx + (cx - lx) * s, z = lz + (cz - lz) * s;
				double ground = GantryRenderer.floorAt(level, x, z, Math.max(landY, g.y()) + 1.0);
				double y = s < 0.75 ? ground : ground + (g.y() - ground) * (s - 0.75) / 0.25;
				float yaw = (float) Math.atan2(cx - lx, cz - lz);
				out.add(new Lump((float) (x - ox), (float) (y - oy), (float) (z - oz), 0.55F, 0.45F, 0.75F, yaw, 0.58F, sprite, false));
				continue;
			}
			BlockPos cell = new BlockPos(g.x(), g.y(), g.z());
			if (now >= g.set() && (now > g.set() + 4 || !level.getBlockState(cell).isAir())) {
				continue;   // set: the real block is there
			}
			// at its cell: a wet sheet that thickens and dries, setting from the last SET ticks
			double p = Math.max(0, Math.min(1, (now - (g.set() - Pour.SET)) / Pour.SET));
			float h = (float) (0.35 + 0.65 * p);
			float shade = (float) (0.58 + 0.42 * p * p);
			// a little wider than its cell while wet, so the sheet runs on into its neighbours; its cell's size as it sets
			float hw = (float) (0.57 - 0.07 * p);
			out.add(new Lump((float) (cx - ox), (float) (g.y() - oy), (float) (cz - oz), hw, h, hw, 0, shade, sprite, true));
		}
		return out.toArray(NONE);
	}

	static void submit(Lump[] lumps, PoseStack pose, SubmitNodeCollector collector, int light) {
		if (lumps.length == 0) {
			return;
		}
		// each sprite drawn from its own atlas
		java.util.Set<net.minecraft.resources.Identifier> atlases = new java.util.LinkedHashSet<>();
		for (Lump l : lumps) {
			atlases.add(l.sprite().atlasLocation());
		}
		for (net.minecraft.resources.Identifier atlas : atlases) {
			collector.submitCustomGeometry(pose, net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(atlas), (at, v) -> {
				for (Lump l : lumps) {
					if (l.sprite().atlasLocation().equals(atlas)) {
						if (l.sheet()) {
							box(at, v, l, l.hw(), l.h(), l.hl(), 0, light);
						} else {
							// a mound: a broad low base and a narrower crown
							box(at, v, l, l.hw(), l.h() * 0.62F, l.hl(), 0, light);
							box(at, v, l, l.hw() * 0.72F, l.h() * 0.38F, l.hl() * 0.78F, l.h() * 0.62F, light);
						}
					}
				}
			});
		}
	}

	/** A box turned to the lump's heading, {@code up} above its foot. */
	private static void box(PoseStack.Pose pose, VertexConsumer v, Lump l, float hw, float h, float hl, float up, int light) {
		float c = (float) Math.cos(l.yaw()), s = (float) Math.sin(l.yaw());
		// across is (c, -s), along is (s, c)
		float[][] corners = new float[8][];
		for (int k = 0; k < 8; k++) {
			float a = (k & 1) == 0 ? -hw : hw, b = (k & 2) == 0 ? -hl : hl, y = (k & 4) == 0 ? up : up + h;
			corners[k] = new float[] {l.x() + a * c + b * s, l.y() + y, l.z() - a * s + b * c};
		}
		int shade = Math.round(255 * l.shade());
		int color = 0xFF000000 | shade << 16 | shade << 8 | shade;
		TextureAtlasSprite sp = l.sprite();
		float u = Math.min(1F, 2 * hw), w = Math.min(1F, 2 * hl), t = Math.min(1F, h);
		face(pose, v, sp, corners[4], corners[5], corners[7], corners[6], 0, 1, 0, u, w, color, light);    // top
		face(pose, v, sp, corners[2], corners[3], corners[1], corners[0], 0, -1, 0, u, w, color, light);   // bottom
		face(pose, v, sp, corners[0], corners[1], corners[5], corners[4], -s, 0, -c, u, t, color, light);  // back
		face(pose, v, sp, corners[3], corners[2], corners[6], corners[7], s, 0, c, u, t, color, light);    // front
		face(pose, v, sp, corners[2], corners[0], corners[4], corners[6], -c, 0, s, w, t, color, light);   // one side
		face(pose, v, sp, corners[1], corners[3], corners[7], corners[5], c, 0, -s, w, t, color, light);   // the other
	}

	private static void face(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite sp, float[] a, float[] b, float[] c, float[] d, float nx, float ny, float nz,
			float u, float w, int color, int light) {
		// wound to face out: the entity render types cull back faces
		vertex(pose, v, sp, d, 0, 0, nx, ny, nz, color, light);
		vertex(pose, v, sp, c, u, 0, nx, ny, nz, color, light);
		vertex(pose, v, sp, b, u, w, nx, ny, nz, color, light);
		vertex(pose, v, sp, a, 0, w, nx, ny, nz, color, light);
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite sp, float[] p, float u, float w, float nx, float ny, float nz, int color, int light) {
		v.addVertex(pose, p[0], p[1], p[2]).setColor(color).setUv(sp.getU(u), sp.getV(w)).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
				.setNormal(pose, nx, ny, nz);
	}
}
