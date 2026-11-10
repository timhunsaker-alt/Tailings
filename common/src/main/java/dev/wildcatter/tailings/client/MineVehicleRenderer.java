package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;

/**
 * Draws one mine locomotive or car where its entity is, on the rail surface, turned and pitched as the track runs. The locomotive's
 * headlight cube is submitted again with the eyes render type so it is full bright.
 */
public class MineVehicleRenderer extends EntityRenderer<MineVehicleEntity, MineVehicleRenderer.VehicleState> {
	private static final Identifier LOCOMOTIVE_TEXTURE = Tailings.id("textures/entity/mine_locomotive.png");
	private static final Identifier CAR_TEXTURE = Tailings.id("textures/entity/mine_car.png");
	/** The model root is 24 pixels above the wheel bottoms. */
	private static final float MODEL_ROOT = 1.5F;

	private final MineLocomotiveModel locomotive;
	private final MineLocomotiveModel headlight;
	private final MineCarModel car;
	private final MineCarModel emptyCar;

	public MineVehicleRenderer(EntityRendererProvider.Context context) {
		super(context);
		EntityModelSet models = context.getModelSet();
		locomotive = new MineLocomotiveModel(context.bakeLayer(MineLocomotiveModel.LAYER));
		headlight = new MineLocomotiveModel(context.bakeLayer(MineLocomotiveModel.LAYER));
		ModelPart lamp = headlight.root().getChild(MineLocomotiveModel.HEADLIGHT);
		for (ModelPart part : headlight.allParts()) {
			if (part != headlight.root() && part != lamp) {
				part.visible = false;
			}
		}
		car = new MineCarModel(models.bakeLayer(MineCarModel.LAYER));
		emptyCar = new MineCarModel(models.bakeLayer(MineCarModel.LAYER));
		emptyCar.root().getChild(MineCarModel.LOAD).visible = false;
	}

	@Override
	public VehicleState createRenderState() {
		return new VehicleState();
	}

	@Override
	public boolean shouldRender(MineVehicleEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
		return frustum.isVisible(entity.getBoundingBox().inflate(1.0));
	}

	@Override
	public void extractRenderState(MineVehicleEntity entity, VehicleState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.shadowRadius = 0.5F;
		state.locomotive = entity.locomotive();
		state.loaded = entity.loaded();
		state.yaw = entity.getYRot(partialTick);
		state.pitch = -entity.getXRot(partialTick);
	}

	@Override
	public void submit(VehicleState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, 180.0F - state.yaw);
		pose.rotateDegrees(Axis.XP, state.pitch);
		pose.scale(-1.0F, -1.0F, 1.0F);
		pose.translate(0.0F, -MODEL_ROOT, 0.0F);
		if (state.locomotive) {
			submitModel(collector, locomotive, state, pose, LOCOMOTIVE_TEXTURE, state.lightCoords);
			collector.order(1).submitModel(headlight, state, pose, RenderTypes.eyes(LOCOMOTIVE_TEXTURE), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
		} else {
			submitModel(collector, state.loaded ? car : emptyCar, state, pose, CAR_TEXTURE, state.lightCoords);
		}
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}

	private static void submitModel(SubmitNodeCollector collector, Model<EntityRenderState> model, VehicleState state, PoseStack pose, Identifier texture, int light) {
		collector.submitModel(model, state, pose, texture, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
	}

	public static class VehicleState extends EntityRenderState {
		public boolean locomotive = true;
		public boolean loaded;
		public float yaw;
		public float pitch;
	}
}
