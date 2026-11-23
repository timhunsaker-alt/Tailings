package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcatter.tailings.block.SkipPartBlock;
import dev.wildcatter.tailings.block.TbmMachine;
import dev.wildcatter.tailings.entity.ShaftSkipEntity;
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
		return frustum.isVisible(box.inflate(2));
	}

	@Override
	public void extractRenderState(ShaftSkipEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		SkipState skip = (SkipState) state;
		skip.loaded = entity.loaded();
		skip.cage = entity.cage();
		float bail = skip.cage ? dev.wildcatter.tailings.shaft.Hoisting.CAGE_HEIGHT : SkipPartBlock.HEIGHT;
		BlockPos far = TbmMachine.offset(entity.facing(), SkipPartBlock.WIDTH - 1, 0, SkipPartBlock.WIDTH - 1);
		skip.centreX = (Math.min(0, far.getX()) + Math.max(0, far.getX()) + 1) / 2f;
		skip.centreZ = (Math.min(0, far.getZ()) + Math.max(0, far.getZ()) + 1) / 2f;
		if (entity.sheaveY() <= ShaftSkipEntity.NO_ROPE + 1) {
			skip.rope = new float[0];
			return;
		}
		double ox = entity.getX(), oy = entity.getY(), oz = entity.getZ();
		float top = (float) (entity.sheaveY() - oy);
		BlockPos drum = entity.drum();
		skip.rope = drum == null ? new float[] {skip.centreX, bail, skip.centreZ, skip.centreX, top, skip.centreZ}
				: new float[] {skip.centreX, bail, skip.centreZ, skip.centreX, top, skip.centreZ,
						(float) (drum.getX() + 0.5 - ox), (float) (drum.getY() + 0.5 - oy), (float) (drum.getZ() + 0.5 - oz)};
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
				// the rock heaped in the skip, just under its rim: one rod along x as wide as the body
				TextureAtlasSprite gravel = sprite("block/gravel");
				collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (at, v) -> GantryRenderer.rod(at, v, gravel,
						skip.centreX - 0.8f, 3.15f, skip.centreZ, skip.centreX + 0.8f, 3.15f, skip.centreZ, 1.5f, light));
			}
			return;
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
