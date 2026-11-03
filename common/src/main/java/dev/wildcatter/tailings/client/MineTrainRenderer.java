package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.block.MineRailBlock;
import dev.wildcatter.tailings.block.MineSwitchBlock;
import dev.wildcatter.tailings.block.RailBlockEntity;
import dev.wildcatter.tailings.block.RailGrade;
import dev.wildcatter.tailings.client.ConsistSpacing.Node;
import dev.wildcatter.tailings.client.ConsistSpacing.Placement;
import dev.wildcatter.tailings.entity.MineTrainEntity;
import java.util.ArrayList;
import java.util.List;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Draws the locomotive and the cars stored on the rails behind it. Cars sit 20 pixels apart along the rail, including
 * the slope. The headlight cube is submitted again with the eyes render type so it is full bright.
 */
public class MineTrainRenderer extends EntityRenderer<MineTrainEntity, MineTrainRenderer.TrainState> {
	private static final Identifier LOCOMOTIVE_TEXTURE = Tailings.id("textures/entity/mine_locomotive.png");
	private static final Identifier CAR_TEXTURE = Tailings.id("textures/entity/mine_car.png");
	private static final int PATH_BLOCKS = 16;
	/** The model root is 24 pixels above the wheel bottoms. */
	private static final float MODEL_ROOT = 1.5F;

	private final EntityModelSet models;
	private final MineLocomotiveModel locomotive;
	private final MineLocomotiveModel headlight;

	public MineTrainRenderer(EntityRendererProvider.Context context) {
		super(context);
		models = context.getModelSet();
		locomotive = new MineLocomotiveModel(context.bakeLayer(MineLocomotiveModel.LAYER));
		headlight = new MineLocomotiveModel(context.bakeLayer(MineLocomotiveModel.LAYER));
		ModelPart lamp = headlight.root().getChild(MineLocomotiveModel.HEADLIGHT);
		for (ModelPart part : headlight.allParts()) {
			if (part != headlight.root() && part != lamp) {
				part.visible = false;
			}
		}
	}

	@Override
	public TrainState createRenderState() {
		return new TrainState();
	}

	@Override
	public boolean shouldRender(MineTrainEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
		return frustum.isVisible(entity.getBoundingBox().inflate(14.0, 2.0, 14.0));
	}

	@Override
	public void extractRenderState(MineTrainEntity entity, TrainState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.shadowRadius = 0.5F;
		state.locomotive = entity.locomotive();
		state.cars.clear();
		Level level = entity.level();
		BlockPos railPos = BlockPos.containing(entity.getX(), entity.getY(), entity.getZ());
		BlockState here = level.getBlockState(railPos);
		Direction travel = travel(here);
		double surface = surfaceAt(level, entity.getX(), entity.getY() + 1.0, entity.getZ(), entity.getY() + 1.0);
		state.offsetY = surface - entity.getY();
		Node loco = new Node(entity.getX(), surface, entity.getZ());
		double aheadX = entity.getX() + travel.getStepX() * 0.25;
		double aheadZ = entity.getZ() + travel.getStepZ() * 0.25;
		Node ahead = new Node(aheadX, surfaceAt(level, aheadX, surface, aheadZ, surface), aheadZ);
		state.yaw = ConsistSpacing.yaw(travel.getStepX(), travel.getStepZ());
		state.pitch = ConsistSpacing.pitch(ahead, loco);
		if (!state.locomotive) {
			state.loaded = loaded(level, railPos);
			return;
		}
		List<RailBlockEntity> consist = level.getBlockEntity(railPos) instanceof RailBlockEntity track ? track.consist() : List.of();
		List<Boolean> loads = new ArrayList<>();
		for (int i = 1; i < consist.size(); i++) {
			RailBlockEntity part = consist.get(i);
			if (part.kind() != RailBlockEntity.Kind.CAR) {
				continue;
			}
			loads.add(part.load().total() > 0);
		}
		if (loads.isEmpty()) {
			return;
		}
		List<Placement> placed = ConsistSpacing.carsBehind(pathBehind(level, loco, travel.getOpposite()), loads.size());
		for (int i = 0; i < placed.size(); i++) {
			Placement spot = placed.get(i);
			CarState drawn = new CarState();
			drawn.dx = spot.x() - entity.getX();
			drawn.dy = spot.y() - entity.getY();
			drawn.dz = spot.z() - entity.getZ();
			drawn.yaw = spot.yaw();
			drawn.pitch = spot.pitch();
			drawn.loaded = loads.get(i);
			drawn.light = LightCoordsUtil.getLightCoords(level, BlockPos.containing(spot.x(), spot.y(), spot.z()));
			state.cars.add(drawn);
		}
	}

	@Override
	public void submit(TrainState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.locomotive) {
			pose.pushPose();
			pose.translate(0.0, state.offsetY, 0.0);
			poseVehicle(pose, state.yaw, state.pitch);
			submitModel(collector, locomotive, state, pose, LOCOMOTIVE_TEXTURE, state.lightCoords);
			collector.order(1).submitModel(headlight, state, pose, RenderTypes.eyes(LOCOMOTIVE_TEXTURE), LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
			pose.popPose();
			for (CarState drawn : state.cars) {
				MineCarModel model = freshCar(drawn.loaded);
				pose.pushPose();
				pose.translate(drawn.dx, drawn.dy, drawn.dz);
				poseVehicle(pose, drawn.yaw, drawn.pitch);
				submitModel(collector, model, state, pose, CAR_TEXTURE, drawn.light);
				pose.popPose();
			}
		} else {
			MineCarModel model = freshCar(state.loaded);
			pose.pushPose();
			pose.translate(0.0, state.offsetY, 0.0);
			poseVehicle(pose, state.yaw, state.pitch);
			submitModel(collector, model, state, pose, CAR_TEXTURE, state.lightCoords);
			pose.popPose();
		}
		super.submit(state, pose, collector, camera);
	}

	private MineCarModel freshCar(boolean loaded) {
		MineCarModel model = new MineCarModel(models.bakeLayer(MineCarModel.LAYER));
		model.root().getChild(MineCarModel.LOAD).visible = loaded;
		return model;
	}

	private static void poseVehicle(PoseStack pose, float yaw, float pitch) {
		pose.rotateDegrees(Axis.YP, 180.0F - yaw);
		pose.rotateDegrees(Axis.XP, pitch);
		pose.scale(-1.0F, -1.0F, 1.0F);
		pose.translate(0.0F, -MODEL_ROOT, 0.0F);
	}

	private static void submitModel(SubmitNodeCollector collector, Model<EntityRenderState> model, TrainState state, PoseStack pose, Identifier texture, int light) {
		collector.submitModel(model, state, pose, texture, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
	}

	private static List<Node> pathBehind(Level level, Node loco, Direction backward) {
		List<Node> nodes = new ArrayList<>();
		nodes.add(loco);
		double x = loco.x();
		double y = loco.y();
		double z = loco.z();
		double stepX = backward.getStepX() / 16.0;
		double stepZ = backward.getStepZ() / 16.0;
		for (int pixel = 0; pixel < PATH_BLOCKS * 16; pixel++) {
			x += stepX;
			z += stepZ;
			y = surfaceAt(level, x, y, z, y);
			nodes.add(new Node(x, y, z));
		}
		return nodes;
	}

	private static double surfaceAt(Level level, double x, double nearY, double z, double fallback) {
		int blockX = (int) Math.floor(x);
		int blockZ = (int) Math.floor(z);
		int base = (int) Math.floor(nearY);
		for (int dy = 1; dy >= -1; dy--) {
			BlockPos pos = new BlockPos(blockX, base + dy, blockZ);
			BlockState state = level.getBlockState(pos);
			if (state.getBlock() instanceof MineSwitchBlock) {
				return pos.getY() + 1.0;
			}
			if (state.getBlock() instanceof MineRailBlock) {
				RailGrade grade = state.getValue(MineRailBlock.GRADE);
				Direction downhill = state.getValue(MineRailBlock.FACING);
				double along = RailSurface.alongDownhill(downhill, pos.getX(), pos.getZ(), x, z);
				return pos.getY() + RailSurface.blocksAboveBottom(grade, along);
			}
		}
		return fallback;
	}

	private static Direction travel(BlockState state) {
		Direction facing = Direction.SOUTH;
		if (state.hasProperty(MineRailBlock.FACING)) {
			facing = state.getValue(MineRailBlock.FACING);
		}
		if (state.hasProperty(MineSwitchBlock.THROWN) && Boolean.TRUE.equals(state.getValue(MineSwitchBlock.THROWN))) {
			return facing.getClockWise();
		}
		return facing;
	}

	private static boolean loaded(Level level, BlockPos pos) {
		return level.getBlockEntity(pos) instanceof RailBlockEntity rail && rail.load().total() > 0;
	}

	public static class TrainState extends EntityRenderState {
		public boolean locomotive = true;
		public boolean loaded;
		public float yaw;
		public float pitch;
		public double offsetY = 1.0;
		public final List<CarState> cars = new ArrayList<>();
	}

	public static class CarState {
		public double dx;
		public double dy;
		public double dz;
		public float yaw;
		public float pitch;
		public boolean loaded;
		public int light;
	}
}
