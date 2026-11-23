package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.wildcatter.tailings.block.TbmMachine;
import dev.wildcatter.tailings.entity.TbmEntity;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;

/**
 * Draws the running machine, every part's cells where they stand in it, and the cutterhead's wheel on the face in front of its shield:
 * a rim, six spokes, the disc cutters along them and the muck buckets round the rim, all turning together at {@link #CUTTERHEAD_RPM}
 * while it cuts. The head's model behind the wheel is a plain face plate, so nothing on the face stands still while the wheel turns.
 */
public class TbmRenderer extends PitchedCellsRenderer<TbmEntity> {
	/** The cutterhead's speed, rpm: an 8.02 m main-beam machine's head turns 0 to 6.87 rpm (its spec table, WTC 2023). */
	public static final float CUTTERHEAD_RPM = 6;
	/** The wheel's centre on the face, across and up from the cutterhead's main cell corner, and its radius, m. */
	private static final float CX = 4f, CY = 4f, R = 3.8f;
	/** How far in front of the shield the wheel stands (the face is at 0, ahead negative). */
	private static final float FRONT = -0.12f;
	private static final int SPOKES = 6;
	private static final int BUCKETS = 8;

	private final java.util.Map<Integer, Float> lastTurn = new java.util.HashMap<>();

	public TbmRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	/**
	 * The machine's cells. The cutterhead and the tail shield are drawn round in their own models (an 8 m circle in level strips,
	 * tools/models/tbm.py), so the head reads round running or set down as blocks, and their corner cells are empty.
	 */
	@Override
	protected List<TbmMachine.Cell> cells(TbmEntity entity) {
		return TbmMachine.cells(entity.facing());
	}

	@Override
	protected Direction facing(TbmEntity entity) {
		return entity.facing();
	}

	@Override
	protected float pitch(TbmEntity entity) {
		return entity.pitch();
	}

	@Override
	public void extractRenderState(TbmEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		// the wheel is always drawn on the round face; it turns while it cuts, and stands where it stopped
		if (entity.cutting()) {
			lastTurn.put(entity.getId(), (entity.tickCount + partialTick) / 20f);
		}
		state.working = lastTurn.getOrDefault(entity.getId(), 0f);
	}

	@Override
	protected void submitMore(State state, PoseStack pose, SubmitNodeCollector collector, int light, boolean pitched) {
		if (!pitched || Float.isNaN(state.working)) {
			return;
		}
		Direction facing = state.facing;
		double turn = state.working * CUTTERHEAD_RPM / 60.0 * Math.PI * 2;
		TextureAtlasSprite steel = sprite("wildcatter", "block/mat/mat_cast_iron"), disc = sprite("wildcatter", "block/mat/mat_stainless"),
				hub = sprite("wildcatter", "block/mat/mat_paint_crane_yellow"), bucket = sprite("wildcatter", "block/mat/mat_oil_dark");
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (at, v) -> {
			// the rim: 24 straight pieces round the wheel
			for (int k = 0; k < 24; k++) {
				double a0 = turn + k * Math.PI / 12, a1 = a0 + Math.PI / 12;
				line(at, v, steel, a0, R, a1, R, 0.34f, light, facing);
			}
			for (int s = 0; s < SPOKES; s++) {
				double a = turn + s * Math.PI * 2 / SPOKES;
				line(at, v, steel, a, 0.5, a, R, 0.42f, light, facing);
				// the disc cutters along the spoke, staggered spoke to spoke so their tracks cover the face
				for (float r = 0.9f + (s % 2) * 0.3f; r < R - 0.2f; r += 0.6f) {
					double side = a + 0.22 / r;
					line(at, v, disc, side, r - 0.12, side, r + 0.12, 0.2f, light, facing, FRONT - 0.18f);
				}
			}
			// the muck buckets round the rim, scooping the cut rock up to the muck ring as the wheel turns
			for (int k = 0; k < BUCKETS; k++) {
				double a = turn + (k + 0.5) * Math.PI * 2 / BUCKETS;
				line(at, v, bucket, a, R - 0.55, a, R - 0.05, 0.5f, light, facing);
			}
			// the hub
			for (int k = 0; k < 8; k++) {
				double a0 = turn + k * Math.PI / 4, a1 = a0 + Math.PI / 4;
				line(at, v, hub, a0, 0.55, a1, 0.55, 0.5f, light, facing);
			}
		});
	}

	private static void line(PoseStack.Pose at, com.mojang.blaze3d.vertex.VertexConsumer v, TextureAtlasSprite s, double a0, double r0, double a1, double r1,
			float thick, int light, Direction facing) {
		line(at, v, s, a0, r0, a1, r1, thick, light, facing, FRONT);
	}

	/** A rod on the face from (angle a0, radius r0) to (a1, r1) about the wheel's centre, {@code along} in front of the shield. */
	private static void line(PoseStack.Pose at, com.mojang.blaze3d.vertex.VertexConsumer v, TextureAtlasSprite s, double a0, double r0, double a1, double r1,
			float thick, int light, Direction facing, float along) {
		float[] p = point(facing, CX + (float) (Math.cos(a0) * r0), CY + (float) (Math.sin(a0) * r0), along);
		float[] q = point(facing, CX + (float) (Math.cos(a1) * r1), CY + (float) (Math.sin(a1) * r1), along);
		GantryRenderer.rod(at, v, s, p[0], p[1], p[2], q[0], q[1], q[2], thick, light);
	}

	/** A point of the cutterhead's frame (across, up, along back from the face) from the entity's corner, turned to its facing. */
	private static float[] point(Direction facing, float across, float up, float along) {
		BlockPos ax = TbmMachine.offset(facing, 1, 0, 0), az = TbmMachine.offset(facing, 0, 0, 1);
		float x = across - 0.5f, z = along - 0.5f;
		return new float[] {x * ax.getX() + z * az.getX() + 0.5f, up, x * ax.getZ() + z * az.getZ() + 0.5f};
	}

	private static TextureAtlasSprite sprite(String ns, String path) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Identifier.fromNamespaceAndPath(ns, path));
	}
}
