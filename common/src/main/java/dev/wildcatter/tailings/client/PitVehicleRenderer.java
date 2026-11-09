package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.entity.PitVehicleEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/**
 * Draws a haul truck or wheel loader at its real size where its entity is, turned to its heading, with the truck's body tipped or the
 * loader's arms lifted as the entity says (open-pit.md). The model's ground is its root, so nothing is offset.
 */
public class PitVehicleRenderer extends EntityRenderer<PitVehicleEntity, PitVehicleRenderer.PitState> {
	private static final Identifier TRUCK_TEXTURE = Tailings.id("textures/entity/haul_truck_placeholder.png");
	private static final Identifier LOADER_TEXTURE = Tailings.id("textures/entity/wheel_loader_placeholder.png");

	private final HaulTruckModel truck;
	private final WheelLoaderModel loader;

	public PitVehicleRenderer(EntityRendererProvider.Context context) {
		super(context);
		truck = new HaulTruckModel(context.bakeLayer(HaulTruckModel.LAYER));
		loader = new WheelLoaderModel(context.bakeLayer(WheelLoaderModel.LAYER));
	}

	@Override
	public PitState createRenderState() {
		return new PitState();
	}

	@Override
	public boolean shouldRender(PitVehicleEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
		return frustum.isVisible(entity.getBoundingBox().inflate(4.0));
	}

	@Override
	public void extractRenderState(PitVehicleEntity entity, PitState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.shadowRadius = 2.5F;
		state.truck = entity instanceof HaulTruckEntity;
		state.loaded = entity.loaded();
		state.lift = entity.lift();
		state.yaw = entity.getYRot(partialTick);
		state.roll = entity.rolled(partialTick);
	}

	@Override
	public void submit(PitState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, 180.0F - state.yaw);
		pose.scale(-1.0F, -1.0F, 1.0F);
		EntityModel<PitState> model = state.truck ? truck : loader;
		collector.submitModel(model, state, pose, state.truck ? TRUCK_TEXTURE : LOADER_TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}

	public static class PitState extends EntityRenderState {
		public boolean truck = true;
		public boolean loaded;
		public float lift;
		public float yaw;
		/** Metres its wheels have rolled. */
		public float roll;
	}
}
