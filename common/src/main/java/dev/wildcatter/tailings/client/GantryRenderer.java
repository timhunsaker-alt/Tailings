package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildcatter.tailings.block.TbmMachine;
import dev.wildcatter.tailings.entity.GantryEntity;
import dev.wildcatter.tailings.mine.TbmGantry;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Draws a back-up gantry, its frame's cells as they stand. The reel gantry also draws its trailing cable, from the reel down to the
 * tunnel floor beside the left rail and back along the bore to the cable junction, a segment a metre lying on whatever floor is
 * there; and spokes on the reel's flanges that turn as the cable pays out (the reel itself is in the frame's model).
 */
public class GantryRenderer extends PitchedCellsRenderer<GantryEntity> {
	/** The reel's drum, in the model's metres (tools/models/tbm.py): its axle along x at y 2.5, z 1.5; the cable wound to 0.95 m. */
	private static final float REEL_Y = 2.5f, REEL_Z = 1.5f, REEL_R = 0.95f, FLANGE_R = 1.15f;
	private static final float[] FLANGES = {0.45f, 3.3f};
	/** The cable lies on the floor in the gantry's first column, beside its left rail. */
	private static final float CABLE_ACROSS = 0.35f;
	private static final float CABLE_THICK = 0.09f;
	/** Drawn no further back than this, in metres. */
	private static final int MOST_METRES = 160;

	public GantryRenderer(EntityRendererProvider.Context context) {
		super(context);
	}

	@Override
	protected List<TbmMachine.Cell> cells(GantryEntity entity) {
		return TbmMachine.gantryCells(entity.kind(), entity.facing());
	}

	@Override
	protected Direction facing(GantryEntity entity) {
		return entity.facing();
	}

	@Override
	protected float pitch(GantryEntity entity) {
		return entity.pitch();
	}

	/** The reel's cable runs far behind it: it is drawn while the gantry is near enough to be tracked at all. */
	@Override
	public boolean shouldRender(GantryEntity entity, net.minecraft.client.renderer.culling.Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
		return entity.kind() == TbmGantry.REEL && entity.cableJunction() != null || super.shouldRender(entity, frustum, cameraX, cameraY, cameraZ, partialTick);
	}

	@Override
	public void extractRenderState(GantryEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		if (entity.kind() != TbmGantry.REEL) {
			state.cable = new float[0];
			state.reelTurn = Float.NaN;
			return;
		}
		state.reelTurn = entity.paidOut() / REEL_R;
		BlockPos junction = entity.cableJunction();
		state.cable = junction == null ? new float[0] : cable(entity, junction, partialTick);
	}

	/** The cable's points from the gantry's corner: off the reel, down behind the gantry, then a metre at a time along the floor to the junction. */
	private static float[] cable(GantryEntity entity, BlockPos junction, float partialTick) {
		Direction facing = entity.facing();
		Level level = entity.level();
		double ox = entity.getX(), oy = entity.getY(), oz = entity.getZ();
		int length = entity.kind().length();
		float[] out = new float[3 * (MOST_METRES + 4)];
		int n = 0;
		n = put(out, n, model(facing, 1.0f, REEL_Y - REEL_R, REEL_Z));
		n = put(out, n, model(facing, CABLE_ACROSS, 0.9f, length + 0.2f));
		double floor = oy + 1;
		double alongJunction = junction.getX() * facing.getStepX() + junction.getZ() * facing.getStepZ();
		for (int metre = 1; metre <= MOST_METRES; metre++) {
			float[] at = model(facing, CABLE_ACROSS, 0, length + metre);
			double wx = ox + at[0], wz = oz + at[2];
			if ((wx * facing.getStepX() + wz * facing.getStepZ()) <= alongJunction + 0.5) {
				break;
			}
			floor = floorAt(level, wx, wz, floor);
			out[n++] = at[0];
			out[n++] = (float) (floor + 0.04 - oy);
			out[n++] = at[2];
		}
		if (n / 3 < MOST_METRES + 2) {
			out[n++] = (float) (junction.getX() + 0.5 - ox);
			out[n++] = (float) (junction.getY() + 0.5 - oy);
			out[n++] = (float) (junction.getZ() + 0.5 - oz);
		}
		return java.util.Arrays.copyOf(out, n);
	}

	private static int put(float[] out, int n, float[] p) {
		out[n] = p[0];
		out[n + 1] = p[1];
		out[n + 2] = p[2];
		return n + 3;
	}

	/** The top of the floor under {@code x, z}, looking from a little above where the last metre's was: the bore climbs behind on the decline. */
	static double floorAt(Level level, double x, double z, double last) {
		int top = (int) Math.floor(last) + 2;
		for (int y = top; y > top - 6; y--) {
			BlockPos pos = BlockPos.containing(x, y, z);
			BlockState state = level.getBlockState(pos);
			VoxelShape shape = state.getCollisionShape(level, pos);
			if (!shape.isEmpty()) {
				return y + shape.max(Direction.Axis.Y);
			}
		}
		return last;
	}

	/** A point of the frame's model (metres across, up and along, from its main cell's corner) from the entity's corner, turned to its facing. */
	private static float[] model(Direction facing, float across, float up, float along) {
		BlockPos ax = TbmMachine.offset(facing, 1, 0, 0), az = TbmMachine.offset(facing, 0, 0, 1);
		float x = across - 0.5f, z = along - 0.5f;
		return new float[] {x * ax.getX() + z * az.getX() + 0.5f, up, x * ax.getZ() + z * az.getZ() + 0.5f};
	}

	@Override
	protected void submitMore(State state, PoseStack pose, SubmitNodeCollector collector, int light, boolean pitched) {
		if (Float.isNaN(state.reelTurn)) {
			return;
		}
		TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS)
				.getSprite(Identifier.withDefaultNamespace("block/black_concrete"));
		if (!pitched) {
			float[] p = state.cable;
			if (p.length < 6) {
				return;
			}
			collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (at, v) -> {
				for (int i = 0; i + 5 < p.length; i += 3) {
					rod(at, v, sprite, p[i], p[i + 1], p[i + 2], p[i + 3], p[i + 4], p[i + 5], CABLE_THICK, light);
				}
			});
			return;
		}
		// two bars across each flange, turning with the reel
		Direction facing = state.facing;
		float turn = state.reelTurn;
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (at, v) -> {
			for (float x : FLANGES) {
				for (int bar = 0; bar < 2; bar++) {
					double a = turn + bar * Math.PI / 2;
					float dy = (float) (Math.cos(a) * FLANGE_R), dz = (float) (Math.sin(a) * FLANGE_R);
					float[] from = model(facing, x, REEL_Y - dy, REEL_Z - dz), to = model(facing, x, REEL_Y + dy, REEL_Z + dz);
					rod(at, v, sprite, from[0], from[1], from[2], to[0], to[1], to[2], 0.12f, light);
				}
			}
		});
	}

	/** A square rod from one point to another. */
	static void rod(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1,
			float thick, int light) {
		float dx = x1 - x0, dy = y1 - y0, dz = z1 - z0;
		float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
		if (len < 1e-4f) {
			return;
		}
		dx /= len;
		dy /= len;
		dz /= len;
		// two sides square to it: across it in the level if it can, else along x
		float ux = -dz, uy = 0, uz = dx;
		float ul = (float) Math.sqrt(ux * ux + uz * uz);
		if (ul < 1e-3f) {
			ux = 1;
			uz = 0;
			ul = 1;
		}
		ux /= ul;
		uz /= ul;
		float vx = dy * uz - dz * uy, vy = dz * ux - dx * uz, vz = dx * uy - dy * ux;
		float h = thick / 2;
		float[][] corners = {{-h, -h}, {h, -h}, {h, h}, {-h, h}};
		for (int k = 0; k < 4; k++) {
			float[] c = corners[k], d = corners[(k + 1) % 4];
			float ax = x0 + ux * c[0] + vx * c[1], ay = y0 + uy * c[0] + vy * c[1], az = z0 + uz * c[0] + vz * c[1];
			float bx = x0 + ux * d[0] + vx * d[1], by = y0 + uy * d[0] + vy * d[1], bz = z0 + uz * d[0] + vz * d[1];
			float nx = ux * (c[0] + d[0]) + vx * (c[1] + d[1]), ny = uy * (c[0] + d[0]) + vy * (c[1] + d[1]), nz = uz * (c[0] + d[0]) + vz * (c[1] + d[1]);
			vertex(pose, v, s, ax, ay, az, 0, 0, light, nx, ny, nz);
			vertex(pose, v, s, bx, by, bz, 0.25f, 0, light, nx, ny, nz);
			vertex(pose, v, s, bx + dx * len, by + dy * len, bz + dz * len, 0.25f, 0.25f, light, nx, ny, nz);
			vertex(pose, v, s, ax + dx * len, ay + dy * len, az + dz * len, 0, 0.25f, light, nx, ny, nz);
		}
	}

	private static void vertex(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite s, float x, float y, float z, float u, float w, int light,
			float nx, float ny, float nz) {
		v.addVertex(pose, x, y, z).setColor(0xFFFFFFFF).setUv(s.getU(u), s.getV(w)).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
				.setNormal(pose, nx, ny, nz);
	}
}
