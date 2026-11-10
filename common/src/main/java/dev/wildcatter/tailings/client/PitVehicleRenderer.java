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
	private static final Identifier TRUCK_TEXTURE = Tailings.id("textures/entity/haul_truck.png");
	private static final Identifier LOADER_TEXTURE = Tailings.id("textures/entity/wheel_loader.png");
	private static final Identifier DRILL_TEXTURE = Tailings.id("textures/entity/blasthole_drill_placeholder.png");
	private static final Identifier EMULSION_TEXTURE = Tailings.id("textures/entity/emulsion_truck_placeholder.png");
	private static final Identifier DOZER_TEXTURE = Tailings.id("textures/entity/track_dozer.png");
	private static final Identifier ULTRA_TEXTURE = Tailings.id("textures/entity/ultra_haul_truck.png");
	private static final Identifier HYD_TEXTURE = Tailings.id("textures/entity/hydraulic_shovel.png");

	private final HaulTruckModel truck;
	private final WheelLoaderModel loader;
	private final BlastholeDrillModel drill;
	private final EmulsionTruckModel emulsion;
	private final TrackDozerModel dozer;
	private final UltraHaulTruckModel ultra;
	private final HydraulicShovelModel hydraulic;

	public PitVehicleRenderer(EntityRendererProvider.Context context) {
		super(context);
		truck = new HaulTruckModel(context.bakeLayer(HaulTruckModel.LAYER));
		loader = new WheelLoaderModel(context.bakeLayer(WheelLoaderModel.LAYER));
		drill = new BlastholeDrillModel(context.bakeLayer(BlastholeDrillModel.LAYER));
		emulsion = new EmulsionTruckModel(context.bakeLayer(EmulsionTruckModel.LAYER));
		dozer = new TrackDozerModel(context.bakeLayer(TrackDozerModel.LAYER));
		ultra = new UltraHaulTruckModel(context.bakeLayer(UltraHaulTruckModel.LAYER));
		hydraulic = new HydraulicShovelModel(context.bakeLayer(HydraulicShovelModel.LAYER));
	}

	@Override
	public PitState createRenderState() {
		return new PitState();
	}

	@Override
	public boolean shouldRender(PitVehicleEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
		// its trailing cable runs far from it, to the switch house: drawn while the machine is tracked at all
		return entity.cableHouse().isPresent() || frustum.isVisible(entity.getBoundingBox().inflate(4.0));   // the drill's box runs to its mast top
	}

	@Override
	public void extractRenderState(PitVehicleEntity entity, PitState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.shadowRadius = 2.5F;
		state.truck = entity instanceof HaulTruckEntity;
		state.kind = entity instanceof dev.wildcatter.tailings.entity.HydraulicShovelEntity ? 6 : entity instanceof dev.wildcatter.tailings.entity.UltraHaulTruckEntity ? 5 : entity instanceof dev.wildcatter.tailings.entity.BlastholeDrillEntity ? 2 : entity instanceof dev.wildcatter.tailings.entity.EmulsionTruckEntity ? 3
				: entity instanceof dev.wildcatter.tailings.entity.TrackDozerEntity ? 4 : state.truck ? 0 : 1;
		state.loaded = entity.loaded();
		state.lift = entity.lift();
		state.yaw = entity.getYRot(partialTick);
		state.roll = entity.rolled(partialTick);
		state.swing = entity instanceof dev.wildcatter.tailings.entity.PitShovelEntity shovel ? shovel.swing() : 0;
		state.cable = entity.cableHouse().map(house -> cable(entity, house, partialTick)).orElse(NO_CABLE);
	}

	private static final float[] NO_CABLE = new float[0];
	/** The most metres of trailing cable drawn: four reels. */
	private static final int MOST_METRES = 1600;
	private static final float CABLE_THICK = 0.12f;

	/**
	 * Its trailing cable's points, from the entity: off the switch house's coupler, down to the ground, then a point every metre along the
	 * straight line to the machine, lying on whatever ground is there, and up to its reel at its back (P5a; the path it has driven is
	 * counted, not drawn).
	 */
	private static float[] cable(PitVehicleEntity entity, net.minecraft.core.BlockPos house, float partialTick) {
		net.minecraft.world.level.Level level = entity.level();
		net.minecraft.world.level.block.state.BlockState hs = level.getBlockState(house);
		if (!(hs.getBlock() instanceof dev.wildcatter.tailings.block.PlantMachineBlock)) {
			return NO_CABLE;
		}
		double ox = entity.getX(partialTick), oy = entity.getY(partialTick), oz = entity.getZ(partialTick);
		net.minecraft.world.phys.Vec3 from = dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity.coupler(house, hs, entity.cableOutlet());
		double yaw = Math.toRadians(entity.getYRot(partialTick));
		double back = entity.spec().length() / 2 - 0.5;
		// its reel at its back, low down: behind the centre along its heading
		double rx = ox + Math.sin(yaw) * back, rz = oz - Math.cos(yaw) * back;
		double dx = rx - from.x, dz = rz - from.z, run = Math.hypot(dx, dz);
		int metres = (int) Math.min(MOST_METRES, Math.floor(run));
		float[] out = new float[3 * (metres + 4)];
		int n = 0;
		out[n++] = (float) (from.x - ox);
		out[n++] = (float) (from.y - oy);
		out[n++] = (float) (from.z - oz);
		double floor = from.y;
		for (int m = 1; m <= metres; m++) {
			double wx = from.x + dx / run * m, wz = from.z + dz / run * m;
			floor = GantryRenderer.floorAt(level, wx, wz, floor);
			out[n++] = (float) (wx - ox);
			out[n++] = (float) (floor + 0.06 - oy);
			out[n++] = (float) (wz - oz);
		}
		out[n++] = (float) (rx - ox);
		out[n++] = 1.0f;
		out[n++] = (float) (rz - oz);
		return java.util.Arrays.copyOf(out, n);
	}

	@Override
	public void submit(PitState state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		pose.pushPose();
		pose.rotateDegrees(Axis.YP, 180.0F - state.yaw);
		pose.scale(-1.0F, -1.0F, 1.0F);
		EntityModel<PitState> model = switch (state.kind) {
			case 0 -> truck;
			case 2 -> drill;
			case 3 -> emulsion;
			case 4 -> dozer;
			case 5 -> ultra;
			case 6 -> hydraulic;
			default -> loader;
		};
		Identifier texture = switch (state.kind) {
			case 0 -> TRUCK_TEXTURE;
			case 2 -> DRILL_TEXTURE;
			case 3 -> EMULSION_TEXTURE;
			case 4 -> DOZER_TEXTURE;
			case 5 -> ULTRA_TEXTURE;
			case 6 -> HYD_TEXTURE;
			default -> LOADER_TEXTURE;
		};
		collector.submitModel(model, state, pose, texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		pose.popPose();
		float[] p = state.cable;
		if (p.length >= 6) {
			net.minecraft.client.renderer.texture.TextureAtlasSprite sprite = net.minecraft.client.Minecraft.getInstance().getAtlasManager()
					.getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(Identifier.withDefaultNamespace("block/black_concrete"));
			int light = state.lightCoords;
			collector.submitCustomGeometry(pose, net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS),
					(at, v) -> {
						for (int i = 0; i + 5 < p.length; i += 3) {
							GantryRenderer.rod(at, v, sprite, p[i], p[i + 1], p[i + 2], p[i + 3], p[i + 4], p[i + 5], CABLE_THICK, light);
						}
					});
		}
		super.submit(state, pose, collector, camera);
	}

	public static class PitState extends EntityRenderState {
		public boolean truck = true;
		/** 0 the haul truck, 1 the wheel loader, 2 the blasthole drill, 3 the emulsion truck, 4 the track dozer, 5 the ultra truck, 6 the hydraulic shovel. */
		public int kind;
		public boolean loaded;
		public float lift;
		public float yaw;
		/** Metres its wheels have rolled. */
		public float roll;
		/** A shovel's house swung from its tracks, degrees, plus to its right. */
		public float swing;
		/** Its trailing cable's points from it, x y z in turn; empty with none. */
		public float[] cable = NO_CABLE;
	}
}
