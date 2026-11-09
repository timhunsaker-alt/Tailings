package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcatter.tailings.block.TbmMachine;
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
import net.minecraft.world.entity.Entity;

/**
 * Draws an entity made of cells: every cell's own block model where it stands, the whole turned about the entity's corner to the grade,
 * the way Wildcatter's cranes draw a part in flight. Its work lights keep it lit underground.
 */
public abstract class PitchedCellsRenderer<T extends Entity> extends EntityRenderer<T, PitchedCellsRenderer.State> {
	private static final BlockDisplayContext CONTEXT = BlockDisplayContext.create();
	private static final int WORK_LIGHT = 12;

	private final BlockModelResolver resolver;

	protected PitchedCellsRenderer(EntityRendererProvider.Context context) {
		super(context);
		resolver = context.getBlockModelResolver();
	}

	public static class State extends EntityRenderState {
		Direction facing = Direction.NORTH;
		float pitch;
		final List<BlockModelRenderState> models = new ArrayList<>();
		List<TbmMachine.Cell> cells = List.of();
		/** The reel gantry's: its trailing cable as points from the entity's corner, {x, y, z} each, and how far the reel has turned. */
		float[] cable = new float[0];
		float reelTurn = Float.NaN;
	}

	/** More to draw, turned to the grade with the cells ({@code pitched}) or as the world stands. */
	protected void submitMore(State state, PoseStack pose, SubmitNodeCollector collector, int light, boolean pitched) {
	}

	protected abstract List<TbmMachine.Cell> cells(T entity);

	protected abstract Direction facing(T entity);

	protected abstract float pitch(T entity);

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public boolean shouldRender(T entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
		return frustum.isVisible(entity.getBoundingBox().inflate(2));
	}

	@Override
	public void extractRenderState(T entity, State state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.shadowRadius = 0;
		state.pitch = pitch(entity);
		state.facing = facing(entity);
		List<TbmMachine.Cell> cells = cells(entity);
		if (cells != state.cells) {
			state.cells = cells;
			state.models.clear();
			for (TbmMachine.Cell cell : cells) {
				BlockModelRenderState model = new BlockModelRenderState();
				resolver.update(model, cell.state(), CONTEXT);
				state.models.add(model);
			}
		}
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		int light = LightCoordsUtil.pack(Math.max(WORK_LIGHT, LightCoordsUtil.block(state.lightCoords)), LightCoordsUtil.sky(state.lightCoords));
		submitMore(state, pose, collector, light, false);
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
		submitMore(state, pose, collector, light, true);
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}
}
