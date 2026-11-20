package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcatter.tailings.entity.SlideEntity;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Draws a slide's ground on its way ({@link SlideEntity}): every block in its own model, where it stood until it lets go, then tumbling
 * as it runs down the face and out over the cut, and lying at its cell until the real block is set there.
 */
public class SlideRenderer extends EntityRenderer<SlideEntity, SlideRenderer.State> {
	private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();
	/** How far a block turns over as it runs, degrees a block: a flow tumbles its lumps. */
	private static final double TUMBLE = 9;

	private final BlockModelResolver resolver;
	private final Map<BlockState, BlockModelRenderState> models = new HashMap<>();

	public SlideRenderer(EntityRendererProvider.Context context) {
		super(context);
		resolver = context.getBlockModelResolver();
	}

	public static class State extends EntityRenderState {
		final List<BlockModelRenderState> drawn = new ArrayList<>();
		/** Each drawn block: x, y, z of its corner from the entity, and its turn about the axis across its run: ax, az, degrees. */
		final List<float[]> at = new ArrayList<>();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRender(SlideEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
		return true;   // its blocks reach tens of metres from it
	}

	private BlockModelRenderState model(BlockState state) {
		return models.computeIfAbsent(state, s -> {
			BlockModelRenderState m = new BlockModelRenderState();
			resolver.update(m, s, CONTEXT);
			return m;
		});
	}

	@Override
	public void extractRenderState(SlideEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.shadowRadius = 0;
		state.drawn.clear();
		state.at.clear();
		double now = entity.level().getGameTime() + partialTick;
		double ex = state.x, ey = state.y, ez = state.z;
		for (SlideEntity.Way w : entity.ways()) {
			if (now >= w.arrive() && (now > w.arrive() + 60 || !entity.level().getBlockState(w.to()).isAir())) {
				continue;   // set: the real block is there
			}
			Vec3 p = w.at(now);
			float ax = 0, az = 0, deg = 0;
			if (now > w.launch() && now < w.arrive()) {
				// turned over about the axis across its way, as far as it has run; each block a little differently
				Vec3 dir = (p.distanceToSqr(w.via()) > 0.01 ? w.via().subtract(Vec3.atCenterOf(w.from())) : Vec3.atCenterOf(w.to()).subtract(w.via()));
				double len = Math.hypot(dir.x, dir.z);
				if (len > 1e-3) {
					ax = (float) (dir.z / len);
					az = (float) (-dir.x / len);
				} else {
					ax = 1;
				}
				double run = dev.wildcatter.tailings.pit.SlideMotion.run(now - w.launch());
				int seed = w.from().hashCode();
				deg = (float) (run * TUMBLE * (0.6 + 0.8 * ((seed >>> 8) & 0xFF) / 255.0));
			}
			state.drawn.add(model(w.state()));
			state.at.add(new float[] {(float) (p.x - ex), (float) (p.y - ey), (float) (p.z - ez), ax, az, deg});
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = state.lightCoords;
		for (int i = 0; i < state.drawn.size(); i++) {
			float[] a = state.at.get(i);
			pose.pushPose();
			pose.translate(a[0], a[1], a[2]);
			if (a[5] != 0) {
				pose.rotateDegrees(Axis.of(new org.joml.Vector3f(a[3], 0, a[4])), a[5]);
			}
			pose.translate(-0.5, -0.5, -0.5);
			state.drawn.get(i).submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
		super.submit(state, pose, collector, camera);
	}
}
