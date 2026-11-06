package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcatter.tailings.block.TbmMachine;
import dev.wildcatter.tailings.entity.TbmEntity;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;

/**
 * Draws the running machine: every cell's own block model where it stands in the machine, the whole turned about the cutterhead to the
 * grade, the way Wildcatter's cranes draw a part in flight. Its work lights keep it lit underground.
 */
public class TbmRenderer extends EntityRenderer<TbmEntity, TbmRenderer.State> {
	private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();
	private static final int WORK_LIGHT = 12;

	private final BlockModelResolver resolver;

	public TbmRenderer(EntityRendererProvider.Context context) {
		super(context);
		resolver = context.getBlockModelResolver();
	}

	public static class State extends EntityRenderState {
		Direction facing;
		float pitch;
		final List<BlockModelRenderState> models = new ArrayList<>();
		List<TbmMachine.Cell> cells = List.of();
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRender(TbmEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
		return frustum.isVisible(entity.getBoundingBox().inflate(2));
	}

	@Override
	public void extractRenderState(TbmEntity entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.shadowRadius = 0;
		state.pitch = entity.pitch();
		Direction facing = entity.facing();
		if (facing != state.facing) {
			state.facing = facing;
			state.cells = TbmMachine.cells(facing);
			state.models.clear();
			for (TbmMachine.Cell cell : state.cells) {
				BlockModelRenderState model = new BlockModelRenderState();
				resolver.update(model, cell.state(), CONTEXT);
				state.models.add(model);
			}
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = LightCoordsUtil.pack(Math.max(WORK_LIGHT, LightCoordsUtil.block(state.lightCoords)), LightCoordsUtil.sky(state.lightCoords));
		pose.pushPose();
		// its rear rises: about the axis across the heading
		switch (state.facing) {
			case NORTH -> pose.rotateDegrees(Axis.XP, -state.pitch);
			case SOUTH -> pose.rotateDegrees(Axis.XP, state.pitch);
			case EAST -> pose.rotateDegrees(Axis.ZP, -state.pitch);
			default -> pose.rotateDegrees(Axis.ZP, state.pitch);
		}
		for (int i = 0; i < state.models.size(); i++) {
			TbmMachine.Cell cell = state.cells.get(i);
			pose.pushPose();
			pose.translate(cell.x(), cell.y(), cell.z());
			state.models.get(i).submit(pose, collector, light, OverlayTexture.NO_OVERLAY, 0);
			pose.popPose();
		}
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
