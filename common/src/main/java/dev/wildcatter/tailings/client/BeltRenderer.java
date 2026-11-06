package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildcatter.tailings.block.BeltBlockEntity;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.rock.Rock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * The lumps a belt carries (docs/multiblocks/crushing-circuit.md: "it shows the lumps it carries"): small cubes in the run's main
 * rock's texture, more as the run fills, sliding toward its head end at 2 m/s while the drive runs.
 */
public class BeltRenderer implements BlockEntityRenderer<BeltBlockEntity, BeltRenderer.State> {
	/** The belt's surface, 0.75 m up, as the belt block's shape. */
	private static final float SURFACE = 12 / 16f;
	private static final float LUMP = 0.16f;
	private static final int MOST = 4;

	public static class State extends BlockEntityRenderState {
		String rock = "";
		float fill;
		Direction toward;
		float phase;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(BeltBlockEntity belt, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderState.extractBase(belt, state, breakProgress);
		state.rock = belt.rock();
		state.fill = belt.fill();
		state.toward = belt.toward();
		double ticks = belt.getLevel() == null ? 0 : belt.getLevel().getGameTime() + partialTick;
		state.phase = belt.moving() ? (float) ((ticks * PlantRates.BELT_METRES_PER_SECOND / 20) % 1.0) : 0;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.rock.isEmpty() || state.fill <= 0 || state.toward == null) {
			return;
		}
		TextureAtlasSprite sprite = sprite(state.rock);
		int lumps = Math.max(1, Math.round(state.fill * MOST));
		float dx = state.toward.getStepX(), dz = state.toward.getStepZ();
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, v) -> {
			for (int i = 0; i < lumps; i++) {
				float along = ((i + 0.5f) / lumps + state.phase) % 1f - 0.5f;   // -0.5 to 0.5 along the belt
				float across = ((i * 37) % 5 - 2) * 0.06f;                    // a little scatter across it
				float cx = 0.5f + along * dx + across * dz, cz = 0.5f + along * dz + across * dx;
				float s = LUMP * (0.8f + 0.1f * (i % 3));
				box(pose, v, sprite, cx - s / 2, SURFACE, cz - s / 2, cx + s / 2, SURFACE + s * 0.8f, cz + s / 2, state.lightCoords);
			}
		});
	}

	/** The rock's own block texture. */
	private static TextureAtlasSprite sprite(String rockName) {
		Identifier id;
		try {
			Rock rock = Rock.valueOf(rockName);
			id = switch (rock.kind()) {
				case COUNTRY -> Identifier.withDefaultNamespace("block/" + rock.id());
				case VANILLA -> Identifier.withDefaultNamespace("block/" + rock.id() + "_ore");
				case ORE -> Identifier.fromNamespaceAndPath("wildcatter", "block/ore/" + rock.id() + "_ore");
			};
		} catch (IllegalArgumentException ex) {
			id = Identifier.withDefaultNamespace("block/cobblestone");
		}
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
	}

	private static void box(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
		float[][] faces = {
				{x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1},
				{x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0, 0, 1},
				{x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0},
				{x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0},
				{x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0}};
		float[][] uv = {{0, 0.25f}, {0.25f, 0.25f}, {0.25f, 0}, {0, 0}};
		for (float[] f : faces) {
			for (int k = 3; k >= 0; k--) {
				v.addVertex(pose, f[k * 3], f[k * 3 + 1], f[k * 3 + 2]).setColor(0xFFFFFFFF).setUv(s.getU(uv[k][0]), s.getV(uv[k][1]))
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, f[12], f[13], f[14]);
			}
		}
	}
}
