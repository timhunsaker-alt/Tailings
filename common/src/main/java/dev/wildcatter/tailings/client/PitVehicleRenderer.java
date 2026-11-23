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
	private static final Identifier DRILL_TEXTURE = Tailings.id("textures/entity/blasthole_drill.png");
	private static final Identifier EMULSION_TEXTURE = Tailings.id("textures/entity/emulsion_truck.png");
	private static final Identifier DOZER_TEXTURE = Tailings.id("textures/entity/track_dozer.png");
	private static final Identifier ULTRA_TEXTURE = Tailings.id("textures/entity/ultra_haul_truck.png");
	private static final Identifier HYD_TEXTURE = Tailings.id("textures/entity/hydraulic_shovel.png");
	private static final Identifier ROPE_TEXTURE = Tailings.id("textures/entity/rope_shovel.png");
	private static final Identifier DRAGLINE_TEXTURE = Tailings.id("textures/entity/dragline.png");

	private final HaulTruckModel truck;
	private final WheelLoaderModel loader;
	private final BlastholeDrillModel drill;
	private final EmulsionTruckModel emulsion;
	private final TrackDozerModel dozer;
	private final UltraHaulTruckModel ultra;
	private final HydraulicShovelModel hydraulic;
	private final RopeShovelModel rope;
	private final DraglineModel dragline;

	public PitVehicleRenderer(EntityRendererProvider.Context context) {
		super(context);
		truck = new HaulTruckModel(context.bakeLayer(HaulTruckModel.LAYER));
		loader = new WheelLoaderModel(context.bakeLayer(WheelLoaderModel.LAYER));
		drill = new BlastholeDrillModel(context.bakeLayer(BlastholeDrillModel.LAYER));
		emulsion = new EmulsionTruckModel(context.bakeLayer(EmulsionTruckModel.LAYER));
		dozer = new TrackDozerModel(context.bakeLayer(TrackDozerModel.LAYER));
		ultra = new UltraHaulTruckModel(context.bakeLayer(UltraHaulTruckModel.LAYER));
		hydraulic = new HydraulicShovelModel(context.bakeLayer(HydraulicShovelModel.LAYER));
		rope = new RopeShovelModel(context.bakeLayer(RopeShovelModel.LAYER));
		dragline = new DraglineModel(context.bakeLayer(DraglineModel.LAYER));
	}

	@Override
	public PitState createRenderState() {
		return new PitState();
	}

	@Override
	public boolean shouldRender(PitVehicleEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ, float partialTick) {
		// its trailing cable runs far from it, to the switch house: drawn while the machine is tracked at all
		if (entity instanceof dev.wildcatter.tailings.entity.DraglineEntity) {
			return entity.cableHouse().isPresent() || frustum.isVisible(entity.getBoundingBox().inflate(80.0, 40.0, 80.0));   // its boom reaches 72 m out and 45 m up
		}
		if (!entity.shownPour().globs().isEmpty()) {
			return frustum.isVisible(entity.getBoundingBox().inflate(24.0));   // its load runs out behind it as it pours
		}
		return entity.cableHouse().isPresent() || frustum.isVisible(entity.getBoundingBox().inflate(4.0));   // the drill's box runs to its mast top
	}

	@Override
	public void extractRenderState(PitVehicleEntity entity, PitState state, float partialTick) {
		super.extractRenderState(entity, state, partialTick);
		state.shadowRadius = 2.5F;
		state.truck = entity instanceof HaulTruckEntity;
		state.kind = entity instanceof dev.wildcatter.tailings.entity.DraglineEntity ? 8 : entity instanceof dev.wildcatter.tailings.entity.RopeShovelEntity ? 7 : entity instanceof dev.wildcatter.tailings.entity.HydraulicShovelEntity ? 6 : entity instanceof dev.wildcatter.tailings.entity.UltraHaulTruckEntity ? 5 : entity instanceof dev.wildcatter.tailings.entity.BlastholeDrillEntity ? 2 : entity instanceof dev.wildcatter.tailings.entity.EmulsionTruckEntity ? 3
				: entity instanceof dev.wildcatter.tailings.entity.TrackDozerEntity ? 4 : state.truck ? 0 : 1;
		state.loaded = entity.loaded();
		state.fill = entity instanceof HaulTruckEntity t ? t.fill() : state.loaded ? 1F : 0F;
		// from where the truck is drawn, which is where the pose stands
		state.pour = PourRenderer.mesh(entity, partialTick, state.x, state.y, state.z);
		state.lift = entity.lift();
		state.spin = entity.tickCount + partialTick;
		state.yaw = entity.getYRot(partialTick);
		state.roll = entity.rolled(partialTick);
		state.swing = entity instanceof dev.wildcatter.tailings.entity.PitShovelEntity shovel ? shovel.swing() : 0;
		state.cycle = entity instanceof dev.wildcatter.tailings.entity.PitShovelEntity shovel ? shovel.cycle() : 0;
		if (entity instanceof dev.wildcatter.tailings.entity.DraglineEntity d) {
			state.swing = d.swing();
			state.bucketOut = d.bucketOut();
			state.bucketUp = d.bucketUp();
			state.walk = d.walkPhase();
			state.shadowRadius = 9F;
			float[] tip = d.tipped(partialTick);
			state.tip = tip[0];
			state.tipX = tip[1];
			state.tipZ = tip[2];
			state.tipDrop = tip[3];
			state.tipShift = tip[4];
			state.tipPivot = tip[5];
			if (state.tip != 0) {
				state.shadowRadius = 0;
			}
		}
		state.groundPitch = state.groundRoll = state.groundDy = 0;
		if (state.kind == 4) {
			ground(entity, state, partialTick);
		}
		state.cable = entity.cableHouse().map(house -> cable(entity, house, partialTick)).orElse(NO_CABLE);
		state.ropes = state.kind == 7 ? ropes(state.yaw, state.swing, state.cycle) : state.kind == 8 ? draglineRopes(state) : NO_CABLE;
	}

	/**
	 * The rope shovel's ropes, from the entity, as pairs of points: two hoist ropes from the boom's point sheaves down to the dipper's
	 * bail, and two suspension ropes from the gantry's head to the boom point (open-pit-p5-big-machines.md, P5d).
	 */
	private static float[] ropes(float yaw, float swing, float cycle) {
		float[] pose = RopeShovelModel.pose(cycle);
		double up = RopeShovelModel.BOOM_ANGLE, a = pose[0];
		double[] foot = {0, 5.0, -6.5}, saddle = {0, RopeShovelModel.SADDLE_UP, -RopeShovelModel.SADDLE_OUT};
		double[] point = {0, foot[1] + 20.8 * Math.sin(up), foot[2] - 20.8 * Math.cos(up)};
		// the bail on the dipper, out along the handle by its crowd
		double d = RopeShovelModel.BAIL_ALONG + pose[1], h = RopeShovelModel.BAIL_UP;
		double[] bail = {0, saddle[1] + h * Math.cos(a) - d * Math.sin(a), saddle[2] - d * Math.cos(a) - h * Math.sin(a)};
		double[] head = {0, 16.5, -3.1};
		float[] out = new float[24];
		int n = 0;
		for (double side : new double[] {-0.6, 0.6}) {
			n = put(out, n, yaw, swing, side, point[1], point[2]);
			n = put(out, n, yaw, swing, side, bail[1], bail[2]);
		}
		for (double side : new double[] {-0.9, 0.9}) {
			n = put(out, n, yaw, swing, side * 2.8, head[1], head[2]);
			n = put(out, n, yaw, swing, side, point[1], point[2]);
		}
		return java.util.Arrays.copyOf(out, n);
	}

	/**
	 * The dragline's ropes, as pairs of points (dragline.md, P6): two hoist ropes from the boom point's sheaves down to the bucket's
	 * spreader bar, two drag ropes from the fairlead at the house's front out to the bucket's mouth, and two suspension ropes from the
	 * mast head to the boom point.
	 */
	private static float[] draglineRopes(PitState state) {
		double up = DraglineModel.BOOM_ANGLE;
		double[] foot = {0, 4.4, -8.5};
		double[] point = {0, foot[1] + 76.0 * Math.sin(up), foot[2] - 76.0 * Math.cos(up)};
		double bx = 0, by = state.bucketUp, bz = -state.bucketOut;
		float[] out = new float[36];
		int n = 0;
		for (double side : new double[] {-1.0, 1.0}) {
			n = put(out, n, state.yaw, state.swing, side * 0.6, point[1], point[2]);
			n = put(out, n, state.yaw, state.swing, bx + side * 2.3, by + 3.9, bz - 0.1);
		}
		for (double side : new double[] {-0.5, 0.5}) {
			n = put(out, n, state.yaw, state.swing, side, 4.0, -9.0);
			n = put(out, n, state.yaw, state.swing, bx + side * 3, by + 0.5, bz + 3.0);
		}
		for (double side : new double[] {-1.0, 1.0}) {
			n = put(out, n, state.yaw, state.swing, side * 1.2, 34.0, -6.5);
			n = put(out, n, state.yaw, state.swing, side * 0.9, point[1] + 0.5, point[2] + 1.0);
		}
		return java.util.Arrays.copyOf(out, n);
	}

	/** A point of the machine (x to its right, y up, z back, from its centre on the ground), its house swung, from the entity. */
	private static int put(float[] out, int n, float yaw, float swing, double x, double y, double z) {
		double s = Math.toRadians(swing), r = Math.toRadians(yaw);
		double xs = x * Math.cos(s) - z * Math.sin(s), zs = x * Math.sin(s) + z * Math.cos(s);
		double rx = -Math.cos(r), rz = -Math.sin(r), ax = -Math.sin(r), az = Math.cos(r);
		out[n] = (float) (xs * rx - zs * ax);
		out[n + 1] = (float) y;
		out[n + 2] = (float) (xs * rz - zs * az);
		return n + 3;
	}

	/** Each tracked machine's pitch, roll and drop as last drawn, eased toward the ground's from frame to frame. */
	private final java.util.Map<Integer, float[]> eased = new java.util.HashMap<>();

	/** The track dozer's tracks: 5.2 m long on the ground, their centres 3.3 m apart (TrackDozerModel). */
	private static final double TRACK_HALF = 2.6, TRACK_GAUGE = 3.3;

	/**
	 * The track dozer stands on the ground under its tracks, not level (heavy-iron-fixes): the ground under the front and rear of its
	 * tracks pitches it, under its left and right tracks rolls it, each from the highest ground there (a track bridges a hollow and rests
	 * on the highest points). Eased, so a block edge passing under it tips it over a few frames, as a track walks over it.
	 */
	private void ground(PitVehicleEntity entity, PitState state, float partialTick) {
		net.minecraft.world.level.Level level = entity.level();
		double ox = entity.getX(partialTick), oy = entity.getY(partialTick), oz = entity.getZ(partialTick);
		double yaw = Math.toRadians(state.yaw);
		double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = -Math.cos(yaw), rz = -Math.sin(yaw);
		double front = -1e9, rear = -1e9, left = -1e9, right = -1e9;
		for (double side : new double[] {-TRACK_GAUGE / 2, TRACK_GAUGE / 2}) {
			for (int k = 0; k <= 8; k++) {
				double along = -TRACK_HALF + k * TRACK_HALF / 4;
				double h = GantryRenderer.floorAt(level, ox + fx * along + rx * side, oz + fz * along + rz * side, oy) - oy;
				if (along >= TRACK_HALF / 2) {
					front = Math.max(front, h);
				}
				if (along <= -TRACK_HALF / 2) {
					rear = Math.max(rear, h);
				}
				if (side > 0) {
					right = Math.max(right, h);
				} else {
					left = Math.max(left, h);
				}
			}
		}
		float pitch = (float) Math.toDegrees(Math.atan2(front - rear, TRACK_HALF * 1.5));
		float roll = (float) Math.toDegrees(Math.atan2(left - right, TRACK_GAUGE));
		float dy = (float) Math.max(-1.5, Math.min(1.5, (front + rear) / 2));
		pitch = Math.max(-35, Math.min(35, pitch));
		roll = Math.max(-25, Math.min(25, roll));
		if (eased.size() > 512) {
			eased.clear();
		}
		float[] e = eased.computeIfAbsent(entity.getId(), id -> new float[] {Float.NaN, 0, 0});
		if (Float.isNaN(e[0])) {
			e[0] = pitch;
			e[1] = roll;
			e[2] = dy;
		} else {
			e[0] += (pitch - e[0]) * 0.15F;
			e[1] += (roll - e[1]) * 0.15F;
			e[2] += (dy - e[2]) * 0.15F;
		}
		state.groundPitch = e[0];
		state.groundRoll = e[1];
		state.groundDy = e[2];
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
		if (state.tip != 0) {
			// a dragline going down with a slide: it tips over its tub's edge toward the cut, sliding out and down with the ground
			pose.translate(state.tipX * state.tipShift, -state.tipDrop, state.tipZ * state.tipShift);
			pose.translate(state.tipX * state.tipPivot, 0, state.tipZ * state.tipPivot);
			pose.rotateDegrees(Axis.of(new org.joml.Vector3f(state.tipZ, 0, -state.tipX)), state.tip);
			pose.translate(-state.tipX * state.tipPivot, 0, -state.tipZ * state.tipPivot);
		}
		pose.pushPose();
		if (state.groundPitch != 0 || state.groundRoll != 0 || state.groundDy != 0) {
			// on the ground under its tracks: nose up about its right, right side down about its forward
			double yaw = Math.toRadians(state.yaw);
			pose.translate(0, state.groundDy, 0);
			pose.rotateDegrees(Axis.of(new org.joml.Vector3f((float) -Math.cos(yaw), 0, (float) -Math.sin(yaw))), state.groundPitch);
			pose.rotateDegrees(Axis.of(new org.joml.Vector3f((float) -Math.sin(yaw), 0, (float) Math.cos(yaw))), state.groundRoll);
		}
		pose.rotateDegrees(Axis.YP, 180.0F - state.yaw);
		pose.scale(-1.0F, -1.0F, 1.0F);
		EntityModel<PitState> model = switch (state.kind) {
			case 0 -> truck;
			case 2 -> drill;
			case 3 -> emulsion;
			case 4 -> dozer;
			case 5 -> ultra;
			case 6 -> hydraulic;
			case 7 -> rope;
			case 8 -> dragline;
			default -> loader;
		};
		Identifier texture = switch (state.kind) {
			case 0 -> TRUCK_TEXTURE;
			case 2 -> DRILL_TEXTURE;
			case 3 -> EMULSION_TEXTURE;
			case 4 -> DOZER_TEXTURE;
			case 5 -> ULTRA_TEXTURE;
			case 6 -> HYD_TEXTURE;
			case 7 -> ROPE_TEXTURE;
			case 8 -> DRAGLINE_TEXTURE;
			default -> LOADER_TEXTURE;
		};
		collector.submitModel(model, state, pose, texture, state.lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
		pose.popPose();
		PourRenderer.submit(state.pour, pose, collector, state.lightCoords);
		float[] r = state.ropes;
		if (r.length >= 6) {
			net.minecraft.client.renderer.texture.TextureAtlasSprite steel = net.minecraft.client.Minecraft.getInstance().getAtlasManager()
					.getAtlasOrThrow(net.minecraft.data.AtlasIds.BLOCKS).getSprite(Identifier.withDefaultNamespace("block/gray_concrete"));
			int light = state.lightCoords;
			collector.submitCustomGeometry(pose, net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS),
					(at, v) -> {
						for (int i = 0; i + 5 < r.length; i += 6) {
							GantryRenderer.rod(at, v, steel, r[i], r[i + 1], r[i + 2], r[i + 3], r[i + 4], r[i + 5], 0.09f, light);
						}
					});
		}
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
		pose.popPose();
		super.submit(state, pose, collector, camera);
	}

	public static class PitState extends EntityRenderState {
		public boolean truck = true;
		/** 0 the haul truck, 1 the wheel loader, 2 the blasthole drill, 3 the emulsion truck, 4 the track dozer, 5 the ultra truck, 6 the hydraulic shovel, 7 the rope shovel, 8 the dragline. */
		public int kind;
		public boolean loaded;
		/** How full a truck's body looks, 0 to 1, as its load pours out. */
		public float fill;
		/** A truck's load on its way out, to draw (open-pit.md, Pouring). */
		public PourRenderer.Quad[] pour = PourRenderer.NONE;
		public float lift;
		/** Ticks it has lived, for parts that turn while it works (the drill's rotary head). */
		public float spin;
		public float yaw;
		/** Metres its wheels have rolled. */
		public float roll;
		/** A shovel's house swung from its tracks, degrees, plus to its right. */
		public float swing;
		/** A shovel's pass: 0 to 1 through the dig, 1 to 2 through the swing, dump and back (the rope shovel's handle and door follow it). */
		public float cycle;
		/** Its trailing cable's points from it, x y z in turn; empty with none. */
		public float[] cable = NO_CABLE;
		/** The dragline's bucket, metres out and up from its centre, and how far through a step its shoes are. */
		public float bucketOut;
		public float bucketUp;
		public float walk;
		/** The rope shovel's or dragline's ropes, pairs of points from it; empty for any other machine. */
		public float[] ropes = NO_CABLE;
		/** A dragline going down with a slide: degrees tipped, toward x z (a unit vector), metres dropped and slid out, and its pivot's distance out. */
		public float tip, tipX, tipZ, tipDrop, tipShift, tipPivot;
		/** The track dozer on the ground under its tracks: degrees nose up, degrees right side down, metres up from its entity. */
		public float groundPitch, groundRoll, groundDy;
	}
}
