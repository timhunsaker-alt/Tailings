package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcatter.tailings.block.ShaftLayout;
import dev.wildcatter.tailings.block.SkipPartBlock;
import dev.wildcatter.tailings.block.TbmMachine;
import dev.wildcatter.tailings.entity.ShaftSkipEntity;
import dev.wildcatter.tailings.shaft.ShaftGeometry;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * Draws a shaft skip (shaft-and-hoist.md, L4): its cells' own models where it hangs, its load of rock showing over its rim while it
 * is loaded, and its rope up the shaft to the head sheave and back down to the hoist's drum.
 */
public class SkipRenderer extends PitchedCellsRenderer<ShaftSkipEntity> {
	private static final float ROPE_THICK = 0.08f;

	public SkipRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	public static class SkipState extends State {
		boolean loaded;
		/** The shaft's cage, drawn as its steel frame, not a skip's cells. */
		boolean cage;
		/** The rope's points from the skip's corner: off its bail, up to the sheave, and back to the drum; empty for none. */
		float[] rope = new float[0];
		float centreX, centreZ;
		/** Its pour (films-hi07): the load still to pour (0: door shut). */
		float pour;
		/**
		 * The stream's centre line from its corner (fix3-hi07: ShaftLayout.pourPath, over the door and the chute and into the bin), the
		 * lip's index in it, the headframe's x axis across the stream, and the open door's hinge and lip from its corner.
		 */
		float[] pourPath;
		int lip;
		float acrossX, acrossZ;
		float[] door;
	}

	@Override
	public State createRenderState() {
		return new SkipState();
	}

	@Override
	protected List<TbmMachine.Cell> cells(ShaftSkipEntity entity) {
		return entity.cage() ? List.of() : TbmMachine.skipCells(entity.facing());
	}

	@Override
	protected Direction facing(ShaftSkipEntity entity) {
		return entity.facing();
	}

	@Override
	protected float pitch(ShaftSkipEntity entity) {
		return 0;
	}

	/** Its rope reaches far up the shaft: it is drawn while any of it is in view. */
	@Override
	public boolean shouldRender(ShaftSkipEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
		AABB box = entity.getBoundingBox();
		if (entity.sheaveY() > ShaftSkipEntity.NO_ROPE + 1) {
			box = box.minmax(new AABB(box.minX, entity.sheaveY(), box.minZ, box.maxX, entity.sheaveY() + 1, box.maxZ));
			BlockPos drum = entity.drum();
			if (drum != null) {
				box = box.minmax(new AABB(drum));
			}
		}
		net.minecraft.world.phys.Vec3 mouth = entity.pourTo();
		if (mouth != null && entity.pour() > 0) {
			box = box.minmax(new AABB(mouth, mouth).inflate(1));
		}
		return frustum.isVisible(box.inflate(2));
	}

	@Override
	public void extractRenderState(ShaftSkipEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		SkipState skip = (SkipState) state;
		skip.loaded = entity.loaded();
		skip.cage = entity.cage();
		skip.pour = entity.pour();
		double ox = entity.getX(), oy = entity.getY(), oz = entity.getZ();
		net.minecraft.world.phys.Vec3 mouth = entity.pourTo();
		ShaftLayout.Frame frame = new ShaftLayout.Frame(entity.frameMain(), entity.facing());
		BlockPos ax = TbmMachine.offset(entity.facing(), 1, 0, 0);
		skip.acrossX = ax.getX();
		skip.acrossZ = ax.getZ();
		if (mouth != null && skip.pour > 0 && !entity.cage()) {
			List<net.minecraft.world.phys.Vec3> path = ShaftLayout.pourPath(frame, entity.syncedCompartment(), mouth);
			skip.pourPath = new float[path.size() * 3];
			for (int i = 0; i < path.size(); i++) {
				skip.pourPath[3 * i] = (float) (path.get(i).x - ox);
				skip.pourPath[3 * i + 1] = (float) (path.get(i).y - oy);
				skip.pourPath[3 * i + 2] = (float) (path.get(i).z - oz);
			}
			skip.lip = ShaftGeometry.lipIndex();
			double sx = entity.syncedCompartment() == 0 ? 3.0 : 6.0;
			net.minecraft.world.phys.Vec3 hinge = frame.point(sx, ShaftGeometry.DOOR_HINGE_Y, ShaftGeometry.DOOR_HINGE_Z),
					lip = frame.point(sx, ShaftGeometry.DOOR_LIP_Y, ShaftGeometry.DOOR_LIP_Z);
			skip.door = new float[] {(float) (hinge.x - ox), (float) (hinge.y - oy), (float) (hinge.z - oz), (float) (lip.x - ox), (float) (lip.y - oy), (float) (lip.z - oz)};
		} else {
			skip.pourPath = null;
			skip.door = null;
		}
		float bail = skip.cage ? dev.wildcatter.tailings.shaft.Hoisting.CAGE_HEIGHT : SkipPartBlock.HEIGHT;
		BlockPos far = TbmMachine.offset(entity.facing(), SkipPartBlock.WIDTH - 1, 0, SkipPartBlock.WIDTH - 1);
		skip.centreX = (Math.min(0, far.getX()) + Math.max(0, far.getX()) + 1) / 2f;
		skip.centreZ = (Math.min(0, far.getZ()) + Math.max(0, far.getZ()) + 1) / 2f;
		if (entity.sheaveY() <= ShaftSkipEntity.NO_ROPE + 1) {
			skip.rope = new float[0];
			return;
		}
		float top = (float) (entity.sheaveY() - oy);
		// the rope up its compartment's centre to the north side of its sheave, over its top, and down to the true tangent on its own
		// drum (fix3-hi07; cage-winder.md): the sheave stands its radius south of the rope
		BlockPos south = TbmMachine.offset(entity.facing(), 0, 0, 1);
		float r = (float) (skip.cage ? dev.wildcatter.tailings.shaft.Hoisting.CAGE_SHEAVE_DIAMETER / 2 : ShaftLayout.SHEAVE_RADIUS), sx = south.getX(), sz = south.getZ();
		float cx = skip.centreX + sx * r, cz = skip.centreZ + sz * r, cy = top - r;
		net.minecraft.world.phys.Vec3 end = entity.ropeEnd();
		BlockPos drum = entity.drum();
		float[] pts = new float[3 * 8];
		int n = 0;
		pts[n++] = skip.centreX;
		pts[n++] = bail;
		pts[n++] = skip.centreZ;
		for (int k = 0; k <= 5; k++) {
			double a = Math.toRadians(90.0 * k / 5);
			pts[n++] = (float) (cx - sx * r * Math.cos(a));
			pts[n++] = (float) (cy + r * Math.sin(a));
			pts[n++] = (float) (cz - sz * r * Math.cos(a));
		}
		if (end != null) {
			pts[n++] = (float) (end.x - ox);
			pts[n++] = (float) (end.y - oy);
			pts[n++] = (float) (end.z - oz);
		} else if (drum != null) {
			pts[n++] = (float) (drum.getX() + 0.5 - ox);
			pts[n++] = (float) (drum.getY() + 0.5 - oy);
			pts[n++] = (float) (drum.getZ() + 0.5 - oz);
		} else {
			pts = java.util.Arrays.copyOf(pts, n);
		}
		skip.rope = pts;
	}

	@Override
	protected void submitMore(State state, PoseStack pose, SubmitNodeCollector collector, int light, boolean pitched) {
		SkipState skip = (SkipState) state;
		if (pitched && skip.cage) {
			cage(skip, pose, collector, light);
			return;
		}
		if (pitched) {
			if (skip.loaded) {
				// the rock heaped in the skip, just under its rim: one rod along x as wide as the body; it sinks as the skip pours
				TextureAtlasSprite gravel = sprite("block/gravel");
				float top = skip.pour > 0 ? 0.9f + 2.25f * skip.pour : 3.15f;
				collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (at, v) -> GantryRenderer.rod(at, v, gravel,
						skip.centreX - 0.8f, top, skip.centreZ, skip.centreX + 0.8f, top, skip.centreZ, 1.5f, light));
			}
			return;
		}
		if (skip.pour > 0 && skip.pourPath != null && skip.door != null) {
			// its bottom door swung open onto the head of the chute, clear over the scroll, and the stream of rock as a flat sheet over the
			// door, down the chute's steps and off its lip in a parabola into the bin, thinning as it empties (fix3-hi07)
			TextureAtlasSprite gravel = sprite("block/gravel"), cobble = sprite("block/cobblestone"), steel = sprite("block/iron_block");
			float[] path = skip.pourPath, d = skip.door;
			float ax = skip.acrossX, az = skip.acrossZ, width = 0.5f + 1.0f * skip.pour;
			int lip = skip.lip;
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (at, v) -> {
				// the door: a plate of rods from its hinge to its lip, 1.8 m wide
				for (float o = -0.85f; o <= 0.86f; o += 0.17f) {
					GantryRenderer.rod(at, v, steel, d[0] + ax * o, d[1] - 0.03f, d[2] + az * o, d[3] + ax * o, d[4] - 0.03f, d[5] + az * o, 0.06f, light);
				}
				int rods = 5;
				for (int i = 0; i + 1 < path.length / 3; i++) {
					// across the chute the sheet keeps its width; in the fall it draws in toward the bin's mouth
					float w0 = i < lip ? width : width * (1 - 0.5f * (i - lip) / (path.length / 3f - 1 - lip));
					float w1 = i + 1 <= lip ? width : width * (1 - 0.5f * (i + 1 - lip) / (path.length / 3f - 1 - lip));
					for (int k = 0; k < rods; k++) {
						float f = (k - (rods - 1) / 2f) / (rods - 1);
						TextureAtlasSprite s = k % 2 == 0 ? gravel : cobble;
						GantryRenderer.rod(at, v, s, path[3 * i] + ax * f * w0, path[3 * i + 1], path[3 * i + 2] + az * f * w0,
								path[3 * i + 3] + ax * f * w1, path[3 * i + 4], path[3 * i + 5] + az * f * w1, (float) ShaftGeometry.STREAM_THICK, light);
					}
				}
			});
		}
		float[] p = skip.rope;
		if (p.length < 6) {
			return;
		}
		TextureAtlasSprite rope = sprite("block/black_concrete");
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (at, v) -> {
			for (int i = 0; i + 5 < p.length; i += 3) {
				GantryRenderer.rod(at, v, rope, p[i], p[i + 1], p[i + 2], p[i + 3], p[i + 4], p[i + 5], ROPE_THICK, light);
			}
		});
	}

	/**
	 * The cage: a steel deck and roof on four corner posts, mesh sides of vertical bars on three sides and an open gate on its fourth,
	 * the bail over its roof; a crate on its deck when it carries materials. 2 x 3 x 2 m.
	 */
	private static void cage(SkipState s, PoseStack pose, SubmitNodeCollector collector, int light) {
		TextureAtlasSprite steel = sprite("block/iron_block"), mesh = sprite("block/gray_concrete"), crate = sprite("block/barrel_side"),
				paint = sprite("block/yellow_concrete");
		float x0 = s.centreX - 1, x1 = s.centreX + 1, z0 = s.centreZ - 1, z1 = s.centreZ + 1, h = dev.wildcatter.tailings.shaft.Hoisting.CAGE_HEIGHT;
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (at, v) -> {
			// deck and roof: plates across
			for (float z = z0 + 0.12f; z < z1; z += 0.25f) {
				GantryRenderer.rod(at, v, steel, x0 + 0.05f, 0.06f, z, x1 - 0.05f, 0.06f, z, 0.26f, light);
				GantryRenderer.rod(at, v, steel, x0 + 0.05f, h - 0.06f, z, x1 - 0.05f, h - 0.06f, z, 0.26f, light);
			}
			// corner posts, painted
			for (float[] c : new float[][] {{x0, z0}, {x1, z0}, {x0, z1}, {x1, z1}}) {
				float cx = c[0] + (c[0] == x0 ? 0.06f : -0.06f), cz = c[1] + (c[1] == z0 ? 0.06f : -0.06f);
				GantryRenderer.rod(at, v, paint, cx, 0, cz, cx, h, cz, 0.12f, light);
			}
			// mesh sides on three sides; the gate side (its z1 face) open, with a top rail
			for (float t = 0.25f; t < 2f; t += 0.25f) {
				GantryRenderer.rod(at, v, mesh, x0 + t, 0.1f, z0 + 0.04f, x0 + t, h - 0.1f, z0 + 0.04f, 0.04f, light);
				GantryRenderer.rod(at, v, mesh, x0 + 0.04f, 0.1f, z0 + t, x0 + 0.04f, h - 0.1f, z0 + t, 0.04f, light);
				GantryRenderer.rod(at, v, mesh, x1 - 0.04f, 0.1f, z0 + t, x1 - 0.04f, h - 0.1f, z0 + t, 0.04f, light);
			}
			for (float y = 0.6f; y < h; y += 0.6f) {
				GantryRenderer.rod(at, v, paint, x0, y, z0 + 0.04f, x1, y, z0 + 0.04f, 0.06f, light);
				GantryRenderer.rod(at, v, paint, x0 + 0.04f, y, z0, x0 + 0.04f, y, z1, 0.06f, light);
				GantryRenderer.rod(at, v, paint, x1 - 0.04f, y, z0, x1 - 0.04f, y, z1, 0.06f, light);
			}
			GantryRenderer.rod(at, v, paint, x0, h - 0.3f, z1 - 0.04f, x1, h - 0.3f, z1 - 0.04f, 0.08f, light);
			// the bail: a crosshead over the roof the rope hangs from
			GantryRenderer.rod(at, v, steel, s.centreX - 0.7f, h + 0.15f, s.centreZ, s.centreX + 0.7f, h + 0.15f, s.centreZ, 0.2f, light);
			if (s.loaded) {
				GantryRenderer.rod(at, v, crate, s.centreX - 0.35f, 0.12f, s.centreZ - 0.45f, s.centreX - 0.35f, 0.92f, s.centreZ - 0.45f, 0.8f, light);
			}
		});
	}

	private static TextureAtlasSprite sprite(String path) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Identifier.withDefaultNamespace(path));
	}
}
