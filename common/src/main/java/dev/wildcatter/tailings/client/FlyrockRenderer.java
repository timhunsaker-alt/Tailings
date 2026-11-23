package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcatter.tailings.entity.FlyrockEntity;
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
 * Draws a firing hole's flyrock and fragments ({@link FlyrockEntity}): each piece a small block of its rock, tumbling along its arc
 * from the hole until it lands.
 */
public class FlyrockRenderer extends EntityRenderer<FlyrockEntity, FlyrockRenderer.State> {
	private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();

	private final BlockModelResolver resolver;
	private final Map<BlockState, BlockModelRenderState> models = new HashMap<>();

	public FlyrockRenderer(EntityRendererProvider.Context context) {
		super(context);
		resolver = context.getBlockModelResolver();
	}

	public static class State extends EntityRenderState {
		final List<BlockModelRenderState> drawn = new ArrayList<>();
		/** Each piece: x, y, z of its centre from the entity, its size, its turn about two axes (degrees), its light. */
		final List<float[]> at = new ArrayList<>();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRender(FlyrockEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
		return true;   // its pieces fly up to 100 m from it
	}

	private BlockModelRenderState model(BlockState state) {
		return models.computeIfAbsent(state, s -> {
			BlockModelRenderState m = new BlockModelRenderState();
			resolver.update(m, s, CONTEXT);
			return m;
		});
	}

	@Override
	public void extractRenderState(FlyrockEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.shadowRadius = 0;
		state.drawn.clear();
		state.at.clear();
		double now = entity.level().getGameTime() + partialTick;
		int i = 0;
		for (FlyrockEntity.Arc a : entity.arcs()) {
			i++;
			if (now < a.launch() || now > a.land()) {
				continue;
			}
			Vec3 p = a.at(now);
			double t = now - a.launch();
			// each piece tumbles at its own rate, about two axes, as a thrown stone does
			int seed = a.from().hashCode() * 31 + i;
			float spinA = (float) (t * (14 + (seed & 15))), spinB = (float) (t * (9 + ((seed >>> 4) & 15)));
			state.drawn.add(model(a.state()));
			state.at.add(new float[] {(float) (p.x - state.x), (float) (p.y - state.y), (float) (p.z - state.z), a.size(), spinA, spinB,
					light(entity.level(), p)});
		}
	}

	private static float light(net.minecraft.world.level.Level level, Vec3 p) {
		net.minecraft.core.BlockPos at = net.minecraft.core.BlockPos.containing(p);
		int sky = level.getBrightness(net.minecraft.world.level.LightLayer.SKY, at);
		int block = level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, at);
		return Float.intBitsToFloat(net.minecraft.util.LightCoordsUtil.pack(block, sky));
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		for (int i = 0; i < state.drawn.size(); i++) {
			float[] a = state.at.get(i);
			int light = Float.floatToRawIntBits(a[6]);
			pose.pushPose();
			pose.translate(a[0], a[1], a[2]);
			pose.rotateDegrees(Axis.XP, a[4]);
			pose.rotateDegrees(Axis.ZP, a[5]);
			pose.scale(a[3], a[3] * 0.8F, a[3] * 0.9F);   // a stone is not a cube
			pose.translate(-0.5, -0.5, -0.5);
			state.drawn.get(i).submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
		super.submit(state, pose, collector, camera);
	}
}
