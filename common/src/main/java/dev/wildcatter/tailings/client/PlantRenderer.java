package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.ShaftLayout;
import dev.wildcatter.tailings.shaft.ShaftGeometry;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.pit.DryStack;
import dev.wildcatter.tailings.plant.PlantMotion;
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
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The moving parts of the crushers and apron feeders, drawn apart from their still block models (tools/models: crushing.py,
 * pitline.py, dump.py leave them out) and moved while the part's motor runs ({@link PlantBlockEntity#moving()}, sent from the
 * server). Each is drawn in its part's own frame (facing north, x east, y up, z south, metres from the main cell's north-west bottom
 * corner, the same boxes the model tool drew) and turned with the part. They turn by game time
 * ({@link PlantBlockEntity#turned}), stop where they stand and start from there.
 * Speeds are {@link PlantMotion}'s.
 * <ul>
 * <li>jaw crusher (C100 class): the flywheels and eccentric shaft turn at 260 rpm, its real speed; the swing jaw's top rides the
 * eccentric's 16 mm circle and its bottom swings the 32 mm stroke at the discharge on the toggle;</li>
 * <li>gyratory crusher (42-65 class): the eccentric at 150 rpm, its real speed; the mantle's head gyrates with it in the bowl (by
 * the little it does that high up, near the spider); the drive coupling is shown turning with the eccentric, a quarter of its real
 * 600 rpm, which strobes;</li>
 * <li>apron feeders (the primary, the reclaim and the dump's): the pans travel toward the head sprockets at 0.1 m/s;</li>
 * <li>the SAG mill and the large ball mill: the shell between its heads turns at 75 % of critical speed, its liner bolt rows showing
 * the turn;</li>
 * <li>the tank and column cells: a froth over the slurry, its bubbles crowding out from the middle to the launder at the lip;</li>
 * <li>the flotation bank: the same froth on each of its eight 20 m3 cells (flotation.py's two rows of four);</li>
 * <li>the scavenger and cleaner banks: the same froth on each of their four cells (recovery.py);</li>
 * <li>the X-ray sorter: pieces ride its belt at 3 m/s through the X-ray cabinet; off the head pulley the ejectors' air blows the ones
 * read as ore on to the product chute (south), with a puff at the manifold, and the rest drop to the waste chute (east), in the share
 * the sorter is sending each way.</li>
 * <li>the tailings filter: its 78 plates, moving tail and ram rod, drawn closed through filling, squeezing and blowing; in the cycle's
 * last 7.5 minutes the tail draws back half a metre and the plates are shifted one at a time from the tail end to the head, each
 * cake dropping through the floor into the trough as its chamber opens, the last as the cycle ends and the game counts the cake into
 * the bin; the press closes again over the next cycle's first 20 s. Timed by the cycle the server sends
 * ({@link PlantBlockEntity#pressSeconds}).</li>
 * </ul>
 */
public class PlantRenderer implements BlockEntityRenderer<PlantBlockEntity, PlantRenderer.State> {
	private static final int SPROCKET_TEETH = PlantMotion.SPROCKET_TEETH;
	private static final PlantMotion.Apron PRIMARY_APRON = PlantMotion.PRIMARY_APRON, RECLAIM_APRON = PlantMotion.RECLAIM_APRON, DUMP_APRON = PlantMotion.DUMP_APRON;

	public static class State extends BlockEntityRenderState {
		@Nullable Machine machine;
		int turns;
		/** The shaft's angle, degrees. */
		float angle;
		/** The light where the moving parts are. */
		int light;
		/** An X-ray sorter's product share. */
		float share = 0.5f;
		/** A tailings filter's seconds into its cycle (-1: not known), and whether it has ever dropped a cake. */
		float press = -1;
		boolean pressed;
		/** The hoist's ropes (films-hi07): each skip's height (west, east compartment), for the drums and head sheaves; NaN for none. */
		float rope0 = Float.NaN, rope1 = Float.NaN;
		/** The cage's height, for its winder's drum and its sheave (cage-winder.md); NaN for none. */
		float ropeCage = Float.NaN;
		/**
		 * Each drum's rope (fix3-hi07), in the part's own frame: {its end on the drum x, y, z; m wound on; its sheave's x along the drum};
		 * null for none. The hoist house's west and east drums, and the cage winder's one drum.
		 */
		float @Nullable [] coil0, coil1, coilCage;
		/** A raise borer's pilot bit and reamer face, metres below its base (NaN: not in the hole), and their light. */
		float pilot = Float.NaN, reamer = Float.NaN;
		int deepLight;
		/** A measuring flask's fill (0 to 1) and its commonest rock's block (heavy-iron ep 7 fix). */
		float flaskFill;
		String flaskRock = "";
		/** A load-out bin's level, 0 to 1 (Tim, 2026-11-23). */
		float binFill;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PlantBlockEntity plant, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(plant, state, crumbling);
		Machine machine = plant.machine();
		if (machine == Machine.TAILINGS_FILTER) {
			state.machine = machine;
			state.turns = BigParts.turns(plant.getBlockState());
			state.press = plant.getLevel() == null ? -1 : (float) plant.pressSeconds(plant.getLevel().getGameTime() + partialTick);
			state.pressed = plant.cycles() > 0;
			state.light = plant.getLevel() == null ? state.lightCoords : lightAt(plant, state.turns, new int[] {3, 5, 12});
			return;
		}
		if (machine == Machine.HEADFRAME || machine == Machine.HOIST_HOUSE || machine == Machine.CAGE_WINDER) {
			state.machine = machine;
			state.turns = BigParts.turns(plant.getBlockState());
			ropes(plant, state, partialTick);
			state.light = plant.getLevel() == null ? state.lightCoords
					: lightAt(plant, state.turns, machine == Machine.HEADFRAME ? new int[] {4, 27, 5} : machine == Machine.CAGE_WINDER ? new int[] {3, 3, 1} : new int[] {3, 3, 2});
			return;
		}
		if (machine == Machine.RAISE_BORER) {
			state.machine = machine;
			state.turns = BigParts.turns(plant.getBlockState());
			if (plant.getLevel() == null) {
				state.light = state.deepLight = state.lightCoords;
				state.pilot = state.reamer = Float.NaN;
				return;
			}
			double now = plant.getLevel().getGameTime() + partialTick;
			state.angle = (float) plant.turned(now, RAISE_RPM);
			double[] tools = plant.raiseTools(now);
			state.pilot = (float) tools[0];
			state.reamer = (float) tools[1];
			state.light = lightAt(plant, state.turns, new int[] {2, 2, 3});
			// lit as the open cell under the bit (the drive, as it breaks through) or under the reamer, not the rock round the string
			float deep = Float.isNaN(state.reamer) ? state.pilot + 1 : state.reamer + 1.5f;
			state.deepLight = Float.isNaN(deep) ? state.light : lightAt(plant, state.turns, new int[] {2, -(int) Math.ceil(deep), 3});
			return;
		}
		if (machine == Machine.LOADOUT_BIN) {
			state.machine = machine;
			state.turns = BigParts.turns(plant.getBlockState());
			state.binFill = plant.binFill();
			state.light = plant.getLevel() == null ? state.lightCoords : Math.max(lightAt(plant, state.turns, new int[] {5, 7, 5}), lightAt(plant, state.turns, new int[] {-1, 4, 5}));
			return;
		}
		if (machine == Machine.MEASURING_FLASK) {
			state.machine = machine;
			state.turns = BigParts.turns(plant.getBlockState());
			state.flaskFill = plant.flaskFill();
			state.flaskRock = plant.flaskRock();
			state.light = plant.getLevel() == null ? state.lightCoords
					: Math.max(lightAt(plant, state.turns, new int[] {-1, 1, 0}), Math.max(lightAt(plant, state.turns, new int[] {2, 1, 1}), lightAt(plant, state.turns, new int[] {1, 1, 2})));
			return;
		}
		float rpm = PlantMotion.rpm(machine);
		if (rpm <= 0) {
			state.machine = null;
			return;
		}
		state.machine = machine;
		state.turns = BigParts.turns(plant.getBlockState());
		state.angle = plant.getLevel() == null ? 0 : (float) plant.turned(plant.getLevel().getGameTime() + partialTick, rpm);
		state.share = plant.xrtShare();
		// lit as the cell the moving parts sit in, not the main cell under them
		int[] at = switch (machine) {
			case JAW -> new int[] {1, 2, 2};
			case GYRATORY_CRUSHER -> new int[] {3, 9, 3};
			case PRIMARY_APRON_FEEDER -> new int[] {1, 1, 6};
			case RECLAIM_FEEDER -> new int[] {1, 1, 3};
			case SAG_MILL -> new int[] {5, 10, 6};
			case BALL_MILL_LARGE -> new int[] {4, 8, 7};
			case TANK_CELL -> new int[] {3, 6, 3};
			case COLUMN_CELL -> new int[] {2, 13, 2};
			case FLOTATION_BANK -> new int[] {3, 3, 6};
			case SCAVENGER_BANK -> new int[] {2, 3, 4};
			case CLEANER_BANK -> new int[] {2, 2, 3};
			case XRT_SORTER -> new int[] {2, 3, 7};
			case VENT_FAN, BOOSTER_FAN -> new int[] {1, 1, 0};
			default -> new int[] {1, 0, 2};
		};
		if (plant.getLevel() == null) {
			state.light = state.lightCoords;
			return;
		}
		state.light = lightAt(plant, state.turns, at);
	}

	/** The light at a cell in the part's frame, or the one over it if brighter. */
	private static int lightAt(PlantBlockEntity plant, int turns, int[] at) {
		BlockPos there = cell(plant.getBlockPos(), turns, at);
		int in = LightCoordsUtil.getLightCoords(plant.getLevel(), there), over = LightCoordsUtil.getLightCoords(plant.getLevel(), there.above());
		return Math.max(in & 0xFFFF, over & 0xFFFF) | Math.max(in >>> 16, over >>> 16) << 16;
	}

	/** A cell in the part's own frame, in the world. */
	private static BlockPos cell(BlockPos main, int turns, int[] local) {
		int x = local[0], z = local[2];
		for (int i = 0; i < turns; i++) {
			int t = x;
			x = -z;
			z = t;
		}
		return main.offset(x, local[1], z);
	}

	@Override
	public boolean shouldRenderOffScreen() {
		return true;
	}

	/** NeoForge culls by this box: the whole of the largest part here, round the main cell whichever way it faces. */
	public AABB getRenderBoundingBox(PlantBlockEntity plant) {
		int reach = plant.machine() == Machine.TAILINGS_FILTER ? 25 : 13;
		AABB box = new AABB(plant.getBlockPos()).inflate(reach, 0, reach).expandTowards(0, plant.machine() == Machine.HEADFRAME ? 30 : 14, 0);
		// a raise borer's string and reamer reach down its hole
		return plant.machine() == Machine.RAISE_BORER ? box.expandTowards(0, -dev.wildcatter.tailings.shaft.RaiseBoring.MAX_DEPTH, 0) : box;
	}

	@Override
	public int getViewDistance() {
		// a raise borer's reamer is seen from the drive far under it (films-hi07)
		return 224;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.machine == null) {
			return;
		}
		pose.pushPose();
		// the part's frame turned with it about the main cell's centre, as its blockstate turns each piece's model
		pose.translate(0.5f, 0, 0.5f);
		pose.rotateDegrees(Axis.YP, -90f * state.turns);
		pose.translate(-0.5f, 0, -0.5f);
		Machine machine = state.machine;
		float angle = state.angle;
		float press = state.press;
		boolean pressed = state.pressed;
		int light = state.light;
		float[] coil0 = state.coil0, coil1 = state.coil1, coilCage = state.coilCage;
		float rope0 = state.rope0, rope1 = state.rope1, ropeCage = state.ropeCage, pilot = state.pilot, reamer = state.reamer;
		int deepLight = state.deepLight;
		float flaskFill = state.flaskFill;
		String flaskRock = state.flaskRock;
		float binFill = state.binFill;
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (p, v) -> {
			PoseStack at = new PoseStack();
			at.last().pose().set(p.pose());
			at.last().normal().set(p.normal());
			switch (machine) {
				case JAW -> jaw(at, v, angle, light);
				case GYRATORY_CRUSHER -> gyratory(at, v, angle, light);
				case PRIMARY_APRON_FEEDER -> apron(at.last(), v, PRIMARY_APRON, angle, light);
				case RECLAIM_FEEDER -> apron(at.last(), v, RECLAIM_APRON, angle, light);
				case APRON_FEEDER -> apron(at.last(), v, DUMP_APRON, angle, light);
					case SAG_MILL -> shell(at, v, angle, 5.5f, 5.9f, 4.85f, 4.55f, 8.3f, 8.7f, 8.85f, light);
					case BALL_MILL_LARGE -> shell(at, v, angle, 4.5f, 4.8f, 3.65f, 3.75f, 11.4f, 11.8f, 12.25f, light);
					case TANK_CELL -> froth(at.last(), v, angle, 3.5f, 3.5f, 3.15f, 5.65f, light);
					case COLUMN_CELL -> froth(at.last(), v, angle, 2.0f, 2.0f, 1.85f, 12.75f, light);
					case FLOTATION_BANK -> bankFroth(at.last(), v, angle, light);
					case SCAVENGER_BANK -> scavengerFroth(at.last(), v, angle, light);
					case CLEANER_BANK -> cleanerFroth(at.last(), v, angle, light);
					case XRT_SORTER -> xrt(at.last(), v, angle, state.share, light);
					case TAILINGS_FILTER -> press(at.last(), v, press, pressed, light);
					case HEADFRAME -> {
						sheaves(at, v, rope0, rope1, light);
						cageSheave(at, v, ropeCage, light);
					}
					case HOIST_HOUSE -> drums(at, v, coil0, coil1, light);
					case CAGE_WINDER -> cageDrum(at, v, coilCage, light);
					case VENT_FAN -> impeller(at, v, angle, 1.5f, 1.5f, 0.75f, 0.85f, 8, 0.3f, light);
					case BOOSTER_FAN -> impeller(at, v, angle, 1.0f, 1.0f, 0.38f, 0.65f, 6, 0.24f, light);
					case RAISE_BORER -> raiseTools(at, v, angle, pilot, reamer, light, deepLight);
					case MEASURING_FLASK -> flask(at.last(), v, flaskFill, flaskRock, light);
					case LOADOUT_BIN -> bin(at.last(), v, binFill, light);
				default -> {
				}
			}
		});
		pose.popPose();
	}

	// ---- the parts ------------------------------------------------------------------------------------------------------------------

	/** The jaw's eccentric throw (radius) at the pitman's top, and half its stroke at the discharge, m. */
	private static final float JAW_THROW = 0.016f;

	private static void jaw(PoseStack at, VertexConsumer v, float angle, int light) {
		// the flywheels and the eccentric shaft, about the shaft's centre (y 3.1, z 2.3)
		at.pushPose();
		at.translate(0, 3.1f, 2.3f);
		at.rotateDegrees(Axis.XP, -angle);
		at.translate(0, -3.1f, -2.3f);
		PoseStack.Pose p = at.last();
		for (float[] x : new float[][] {{0.25f, 0.55f}, {2.45f, 2.75f}}) {
			cylinderX(p, v, mat("iron"), x[0], x[1], 3.1f, 2.3f, 0.85f, 16, light);
			// the counterweight on its rim, so the wheel reads as turning
			boolean west = x[0] < 1;
			float face = west ? x[0] - 0.03f : x[1];
			box(p, v, mat("blue"), face, 3.1f + 0.4f, 2.3f - 0.22f, face + 0.03f, 3.1f + 0.8f, 2.3f + 0.22f, light);
			box(p, v, mat("steel"), face, 3.1f - 0.05f, 2.3f - 0.05f, face + 0.03f, 3.1f + 0.4f, 2.3f + 0.05f, light);
		}
		cylinderX(p, v, mat("steel"), 0.55f, 2.45f, 3.1f, 2.3f, 0.12f, 8, light);
		at.popPose();
		// the swing jaw: its top on the eccentric's circle, its bottom swinging on the toggle
		double r = Math.toRadians(angle);
		float topZ = (float) (JAW_THROW * Math.sin(r)), topY = (float) (JAW_THROW * Math.cos(r));
		float lowZ = (float) (JAW_THROW * Math.sin(r));
		float[][] c = new float[8][];
		for (int i = 0; i < 8; i++) {
			boolean east = (i & 4) != 0, south = (i & 2) != 0, top = (i & 1) != 0;
			float x = east ? 2.1f : 0.9f;
			float y = top ? 3.7f + topY : 1.2f + topY;
			float z = (south ? 2.1f : 1.7f) + (top ? topZ : lowZ);
			c[i] = new float[] {x, y, z};
		}
		hexa(at.last(), v, mat("iron"), c, light);
	}

	/** The 42-65's eccentric throw (radius) at the bottom of the mantle, m, and how far below the spider's pivot it is and the head is. */
	private static final float GYRATORY_THROW = 0.019f, MANTLE_DEPTH = 7.6f, HEAD_DEPTH = 0.6f;

	private static void gyratory(PoseStack at, VertexConsumer v, float angle, int light) {
		// the mantle's head, gyrating about the bowl's centre with the eccentric (its throw scaled up the shaft to the pivot)
		double r = Math.toRadians(angle);
		float e = GYRATORY_THROW * HEAD_DEPTH / MANTLE_DEPTH;
		float dx = (float) (e * Math.cos(r)), dz = (float) (e * Math.sin(r));
		disc(at.last(), v, mat("iron"), 3.0f + dx, 3.0f + dz, 0.9f, 9.0f, 9.4f, light);
		// the drive coupling on the countershaft, out of the west side
		at.pushPose();
		at.translate(0, 2.4f, 3.0f);
		at.rotateDegrees(Axis.XP, angle);
		at.translate(0, -2.4f, -3.0f);
		PoseStack.Pose p = at.last();
		octoX(p, v, mat("blue"), 0.0f, 0.3f, 2.4f, 3.0f, 0.45f, light);
		for (int k = 0; k < 2; k++) {
			float s = k == 0 ? 1 : -1;
			box(p, v, mat("steel"), -0.03f, 2.4f + s * 0.3f - 0.05f, 3.0f - 0.05f, 0.0f, 2.4f + s * 0.3f + 0.05f, 3.0f + 0.05f, light);   // its bolts
		}
		at.popPose();
	}

	/** The pans, moved along by the head sprocket's turn; a pan past the head end comes back round the tail. */
	private static void apron(PoseStack.Pose p, VertexConsumer v, PlantMotion.Apron a, float angle, int light) {
		float run = a.pitch() * a.count();
		float travel = (angle / 360f) * SPROCKET_TEETH * a.pitch();
		TextureAtlasSprite tex = mat(a.tex());
		for (int k = 0; k < a.count(); k++) {
			float z = a.z0() + ((k * a.pitch() + travel) % run + run) % run;
			float end = z + a.length();
			box(p, v, tex, a.x0(), a.y0(), z, a.x1(), a.y1(), Math.min(end, a.end()), light);
			if (end > a.end()) {
				box(p, v, tex, a.x0(), a.y0(), a.z0(), a.x1(), a.y1(), a.z0() + (end - a.end()), light);
			}
		}
	}

	/**
	 * A mill's shell between its head flanges (z0 to z1, the girth gear's z gz0 to gz1 left to the model), turning about its axis along
	 * z at (cx, cy): a 16-sided shell just over the model's octagon, eight rows of liner bolt heads along it, so the turn shows.
	 */
	private static void shell(PoseStack at, VertexConsumer v, float angle, float cx, float cy, float r, float z0, float gz0, float gz1, float z1, int light) {
		at.pushPose();
		at.translate(cx, cy, 0);
		at.rotateDegrees(Axis.ZP, angle);
		PoseStack.Pose p = at.last();
		float rr = r * 1.01f;
		for (float[] span : new float[][] {{z0, gz0}, {gz1, z1}}) {
			cylinderZ(p, v, mat("green"), span[0], span[1], rr, 16, light);
			for (int k = 0; k < 8; k++) {
				PoseStack row = new PoseStack();
				row.last().pose().set(p.pose());
				row.last().normal().set(p.normal());
				row.rotateDegrees(Axis.ZP, k * 45f + 11.25f);
				for (float z = span[0] + 0.2f; z < span[1] - 0.2f; z += 0.45f) {
					box(row.last(), v, mat("steel"), -0.14f, rr - 0.01f, z, 0.14f, rr + 0.12f, z + 0.28f, light);
				}
			}
		}
		at.popPose();
	}

	/** A round part along z about the pose's origin: {@code sides / 2} slabs each turned about the axis. */
	private static void cylinderZ(PoseStack.Pose base, VertexConsumer v, TextureAtlasSprite s, float z0, float z1, float r, int sides, int light) {
		float w = (float) (2 * r * Math.tan(Math.PI / sides));
		for (int k = 0; k < sides / 2; k++) {
			PoseStack at = new PoseStack();
			at.last().pose().set(base.pose());
			at.last().normal().set(base.normal());
			at.rotateDegrees(Axis.ZP, k * 360f / sides);
			box(at.last(), v, s, -w / 2, -r, z0, w / 2, r, z1, light);
		}
	}

	/** A fixed pseudo-random 0 to 1 for bubble or piece {@code i}, draw {@code k}. */
	private static float hash(int i, int k) {
		int h = i * 0x9E3779B1 + k * 0x85EBCA77;
		h ^= h >>> 15;
		h *= 0x2C1B3C6D;
		h ^= h >>> 12;
		return (h & 0xFFFF) / 65536f;
	}

	private static final int BUBBLES = 70;

	/**
	 * A cell's froth: a grey mineral froth just over the model's slurry (at y), and bubbles riding out across it from the middle to the
	 * launder at the lip, growing as they go, one cycle a turn.
	 */
	private static void froth(PoseStack.Pose p, VertexConsumer v, float angle, float cx, float cz, float r, float y, int light) {
		TextureAtlasSprite base = vanilla("light_gray_concrete_powder"), bubble = vanilla("white_concrete_powder");
		float step = 0.25f;
		for (float z = cz - r; z < cz + r - 1e-4f; z += step) {
			float z2 = Math.min(z + step, cz + r), d = Math.abs((z + z2) / 2 - cz);
			float h = (float) Math.sqrt(Math.max(0, r * r - d * d));
			box(p, v, base, cx - h, y, z, cx + h, y + 0.03f, z2, light);
		}
		float turn = angle / 360f;
		for (int i = 0; i < BUBBLES; i++) {
			float f = ((turn + hash(i, 0)) % 1f + 1f) % 1f;
			double a = hash(i, 1) * Math.PI * 2;
			float rad = 0.15f * r + 0.8f * r * f;
			float size = (0.10f + 0.12f * hash(i, 2)) * (0.6f + 0.8f * f) * Math.min(1f, r / 2.5f + 0.3f);
			float bx = cx + (float) Math.cos(a) * rad, bz = cz + (float) Math.sin(a) * rad;
			box(p, v, bubble, bx - size / 2, y + 0.02f, bz - size / 2, bx + size / 2, y + 0.02f + size * 0.6f, bz + size / 2, light);
		}
	}

	/** The bank's eight cells (flotation.py: rows at x 1.75 and 5.25, cells every 3.25 m from z 1.65, 1.6 m across), each its own froth. */
	private static void bankFroth(PoseStack.Pose p, VertexConsumer v, float angle, int light) {
		for (float cx : new float[] {1.75f, 5.25f}) {
			for (int i = 0; i < 4; i++) {
				froth(p, v, angle + 90 * i + (cx > 3 ? 45 : 0), cx, 1.65f + 3.25f * i, 1.5f, 3.06f, light);
			}
		}
	}

	/** The scavenger bank's four 20 m3 cells (recovery.py: in a row at x 2.5, z 1.2 to 7.8, 1.05 m across, slurry at 2.85 m). */
	private static void scavengerFroth(PoseStack.Pose p, VertexConsumer v, float angle, int light) {
		float[] zs = {1.2f, 3.4f, 5.6f, 7.8f};
		for (int i = 0; i < zs.length; i++) {
			froth(p, v, angle + 90 * i, 2.5f, zs[i], 0.98f, 2.86f, light);
		}
	}

	/** The cleaner bank's four 3 m3 cells (recovery.py: x 1.1 and 2.9, z 1.5 and 4.5, 0.8 m across, slurry at 1.85 m). */
	private static void cleanerFroth(PoseStack.Pose p, VertexConsumer v, float angle, int light) {
		for (float cx : new float[] {1.1f, 2.9f}) {
			for (float cz : new float[] {1.5f, 4.5f}) {
				froth(p, v, angle + 90 * cz + (cx > 2 ? 45 : 0), cx, cz, 0.74f, 1.86f, light);
			}
		}
	}

	/** Pieces on the sorter's belt, one every half metre. */
	private static final float PIECE_PITCH = 0.5f;
	private static final float BELT_TAIL = 1.0f, BELT_HEAD = 7.0f, BELT_TOP = 2.25f;

	/**
	 * The X-ray sorter's pieces: each rides the belt from its tail (z 1) to its head (z 7) at 3 m/s, then flies: one read as ore is
	 * blown on by the ejectors' air over the manifold toward the product chute out of the south face, a puff at the manifold as it
	 * goes; one read as waste drops off the head and falls away east into the waste chute. {@code share} of them are ore.
	 */
	private static void xrt(PoseStack.Pose p, VertexConsumer v, float angle, float share, int light) {
		float loop = PlantMotion.XRT_LOOP_METRES;
		float travel = angle / 360f * loop;
		int count = Math.round(loop / PIECE_PITCH);
		TextureAtlasSprite ore = mat("brass"), waste = vanilla("andesite"), air = vanilla("white_concrete");
		double speed = PlantMotion.XRT_BELT_SPEED;
		for (int k = 0; k < count; k++) {
			float along = ((k * PIECE_PITCH + travel) % loop + loop) % loop;   // metres from the tail
			float size = 0.11f + 0.07f * hash(k, 3);
			float x = 1.5f + 1.0f * hash(k, 4);
			boolean isOre = hash(k, 5) < share;
			float z = BELT_TAIL + along;
			if (z <= BELT_HEAD) {
				box(p, v, isOre ? ore : waste, x - size / 2, BELT_TOP, z - size / 2, x + size / 2, BELT_TOP + size * 0.8f, z + size / 2, light);
				continue;
			}
			float t = (float) ((z - BELT_HEAD) / speed);   // seconds since it left the head pulley
			float fx, fy, fz;
			if (isOre) {
				fz = BELT_HEAD + (float) speed * t * 1.15f;
				fy = BELT_TOP + 0.6f * t - 4.9f * t * t;
				fx = x;
				if (t < 0.12f) {
					float puff = 0.25f + 1.5f * t;
					box(p, v, air, x - puff / 2, 1.95f, 7.35f - puff / 3, x + puff / 2, 1.95f + puff * 0.6f, 7.35f + puff * 0.8f, light);
				}
			} else {
				fz = BELT_HEAD + 0.35f * t;
				fy = BELT_TOP - 4.9f * t * t;
				fx = x + 2.6f * t;
			}
			if (fy < 0.4f || fz > 8.6f || fx > 4.6f) {
				continue;
			}
			box(p, v, isOre ? ore : waste, fx - size / 2, fy, fz - size / 2, fx + size / 2, fy + size * 0.8f, fz + size / 2, light);
		}
	}

	// ---- the tailings filter's plates (tools/models/pit.py leaves them out) ------------------------------------------------------------

	private static final int PLATES = 78;
	/** The first plate's head-side face, the plates' pitch and thickness (the cake chambers between them 5 cm), m along z. */
	private static final float PLATE_Z0 = 2.1f, PLATE_PITCH = 0.25f, PLATE_T = 0.2f;
	/** The tail's closed head-side face; how far it draws back, and each shifted plate goes; the cylinder's end stand. */
	private static final float TAIL_Z = 21.8f, OPEN = 0.5f, STAND_Z = 23.2f;
	/** A cake stands in its opened chamber this long before it falls. */
	private static final float CAKE_HANG = 1.0f;
	/**
	 * Seconds: opening starts when blowing ends; the tail draws back over 20 s; then the plates are shifted one at a time, 2.5 s each,
	 * the last (by the head) starting so its cake falls just before the cycle ends, when the game counts the whole cake into the bin.
	 */
	private static final float OPEN_AT = DryStack.FILL_SECONDS + DryStack.SQUEEZE_SECONDS + DryStack.BLOW_SECONDS, TAIL_MOVE = 20f, SHIFT_MOVE = 2.5f;
	private static final float LAST_SHIFT = DryStack.CYCLE_SECONDS - CAKE_HANG - 0.6f;
	private static final float SHIFT_EVERY = (LAST_SHIFT - OPEN_AT - TAIL_MOVE) / (PLATES - 1);

	private static float smooth(float t) {
		t = Math.max(0, Math.min(1, t));
		return t * t * (3 - 2 * t);
	}

	/** When plate k (0 at the head) starts its shift, s into the cycle: from the tail end to the head. */
	private static float shiftAt(int k) {
		return OPEN_AT + TAIL_MOVE + (PLATES - 1 - k) * SHIFT_EVERY;
	}

	/** Where the cake trough's bottom is under z (pit.py: stepping down from each end to the chute). */
	private static float troughAt(float z) {
		if (z < 5.0f || z >= 19.5f) {
			return 2.6f;
		}
		if (z < 7.5f || z >= 16.5f) {
			return 1.8f;
		}
		if (z < 9.6f || z >= 14.4f) {
			return 1.0f;
		}
		return 0.6f;
	}

	/**
	 * The press at {@code s} seconds into its cycle (-1: closed): the plates, the moving tail and the ram's rod, and the cakes standing
	 * in their opened chambers or falling into the trough.
	 */
	private static void press(PoseStack.Pose p, VertexConsumer v, float s, boolean pressed, int light) {
		float tail = 0;
		float[] off = new float[PLATES];
		if (s >= OPEN_AT) {
			tail = OPEN * smooth((s - OPEN_AT) / TAIL_MOVE);
			for (int k = 0; k < PLATES; k++) {
				off[k] = OPEN * smooth((s - shiftAt(k)) / SHIFT_MOVE);
			}
		} else if (s >= 0 && s < TAIL_MOVE && pressed) {
			// closing: the ram pushes the tail and the shifted plates back up to the head
			tail = OPEN * (1 - smooth(s / TAIL_MOVE));
			java.util.Arrays.fill(off, tail);
		}
		TextureAtlasSprite plate = mat("polymer"), blue = mat("blue"), steel = mat("steel");
		for (int k = 0; k < PLATES; k++) {
			float z = PLATE_Z0 + k * PLATE_PITCH + off[k];
			box(p, v, plate, 1.75f, 4.1f, z, 4.25f, 5.8f, z + PLATE_T, light);
			box(p, v, steel, 0.9f, 5.55f, z + 0.04f, 1.75f, 5.75f, z + PLATE_T - 0.04f, light);   // its arms on the side beams
			box(p, v, steel, 4.25f, 5.55f, z + 0.04f, 5.1f, 5.75f, z + PLATE_T - 0.04f, light);
		}
		box(p, v, blue, 0.9f, 4.0f, TAIL_Z + tail, 5.1f, 5.9f, TAIL_Z + 0.8f + tail, light);
		PoseStack rod = new PoseStack();
		rod.last().pose().set(p.pose());
		rod.last().normal().set(p.normal());
		rod.translate(3.0f, 5.0f, 0);
		cylinderZ(rod.last(), v, steel, TAIL_Z + 0.8f + tail, STAND_Z, 0.12f, 8, light);   // the ram's rod
		if (s < OPEN_AT) {
			return;
		}
		TextureAtlasSprite cake = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Identifier.fromNamespaceAndPath("tailings", "block/dry_tails"));
		for (int k = 0; k <= PLATES; k++) {
			// chamber k: between plate k-1 (or the head) and plate k (or the tail), opened as plate k (or the tail) moves off
			float opened = k == PLATES ? OPEN_AT : shiftAt(k);
			float t = s - opened;
			if (t < 0 || t > CAKE_HANG + 1.2f) {
				continue;
			}
			float z = k == 0 ? 2.05f : k == PLATES ? PLATE_Z0 + (PLATES - 1) * PLATE_PITCH + PLATE_T + 0.07f : PLATE_Z0 + k * PLATE_PITCH - 0.025f;
			if (t < CAKE_HANG) {
				box(p, v, cake, 1.95f, 4.15f, z - 0.04f, 4.05f, 5.75f, z + 0.04f, light);   // standing in its opened chamber
				continue;
			}
			for (int j = 0; j < 5; j++) {
				// as it falls it breaks into lumps, the lowest first, each tumbling a little out of the chamber's plane; clear of the plates
				// (under their bottom edge, 4.1 m) the lumps open out to half a metre and more, so they read from the press's side
				float fall = Math.max(0, t - CAKE_HANG - 0.05f * (j % 3));
				float drop = 4.9f * fall * fall;
				float grow = Math.min(1, fall / 0.15f);
				float wide = 0.3f + 0.16f * hash(k, j + 13);
				float x = 1.95f + wide + (2.1f - 2 * wide) * hash(k, j + 3);
				float y0 = 4.15f + 0.3f * j - drop, y1 = y0 + 0.4f + 0.2f * hash(k, j + 21);
				if (y0 < troughAt(z) + 0.2f) {
					continue;
				}
				float clear = Math.max(0, Math.min(1, (4.1f - y1) / 0.6f));
				float half = 0.04f + 0.13f * grow * (0.7f + 0.6f * hash(k, j + 7)) + 0.16f * clear;
				float dz = (OPEN / 2 + (hash(k, j + 31) - 0.5f) * 0.12f) * grow;   // into the middle of the opened gap
				box(p, v, cake, x - wide, y0, z + dz - half, x + wide, y1, z + dz + half, light);
			}
		}
		// the cake gathers in the trough under each chamber that has dropped, till the game counts the whole batch into the bin at the
		// cycle's end (the trough is then empty again)
		for (int k = 0; k <= PLATES; k++) {
			float opened = k == PLATES ? OPEN_AT : shiftAt(k);
			float landed = s - opened - CAKE_HANG - 0.7f;
			if (landed < 0) {
				continue;
			}
			float z = PLATE_Z0 + Math.min(PLATES - 1, k) * PLATE_PITCH;
			float floor = troughAt(z);
			float h = (0.35f + 0.25f * hash(k, 41)) * Math.min(1, landed / 0.4f);
			float w = 0.55f + 0.25f * hash(k, 43);
			box(p, v, cake, 3.0f - w, floor, z - 0.02f, 3.0f + w, floor + h, z + PLATE_PITCH + 0.02f, light);
		}
	}

	// ---- the load-out bin's level (Tim, 2026-11-23) --------------------------------------------------------------------------------

	/** The bin's inside (pit.py: walls 0.3 m thick round 10 x 10, from 2 m up to 7) and its level's range: empty at 2.1, full at 6.6. */
	private static final float BIN_IN0 = 0.3f, BIN_IN1 = 9.7f, BIN_EMPTY = 2.1f, BIN_FULL = 6.6f;
	/** The sight windows, on each wall: across 6.4 to 7.8 m, up 2.2 to 6.8. */
	private static final float BWIN_A = 6.4f, BWIN_B = 7.8f, BWIN_Y0 = 2.2f, BWIN_Y1 = 6.8f;

	/**
	 * The dry tails in the bin at their level, heaped a little under the press's chute in the middle, and a sight window on each wall:
	 * dark glass with the cake risen to the level, a frame, and a yellow mark at each 100 t.
	 */
	private static void bin(PoseStack.Pose p, VertexConsumer v, float fill, int light) {
		TextureAtlasSprite cake = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Identifier.fromNamespaceAndPath("tailings", "block/dry_tails"));
		TextureAtlasSprite dark = vanilla("black_concrete"), yellow = vanilla("yellow_concrete"), frame = mat("iron");
		fill = Math.max(0, Math.min(1, fill));
		float top = BIN_EMPTY + (BIN_FULL - BIN_EMPTY) * fill;
		if (fill > 0.004f) {
			box(p, v, cake, BIN_IN0, BIN_EMPTY - 0.1f, BIN_IN0, BIN_IN1, top, BIN_IN1, light);
			float heap = Math.min(0.35f, 0.05f + fill);
			box(p, v, cake, 3.0f, top, 3.0f, 7.0f, Math.min(BIN_FULL + 0.3f, top + heap), 7.0f, light);
		}
		float wTop = BWIN_Y0 + (BWIN_Y1 - BWIN_Y0) * fill;
		for (int side = 0; side < 4; side++) {
			binBox(p, v, dark, side, BWIN_A, BWIN_B, BWIN_Y0, BWIN_Y1, 0.005f, light);
			if (fill > 0.004f) {
				binBox(p, v, cake, side, BWIN_A, BWIN_B, BWIN_Y0, wTop, 0.012f, light);
			}
			binBox(p, v, frame, side, BWIN_A - 0.08f, BWIN_A, BWIN_Y0 - 0.08f, BWIN_Y1 + 0.08f, 0.03f, light);
			binBox(p, v, frame, side, BWIN_B, BWIN_B + 0.08f, BWIN_Y0 - 0.08f, BWIN_Y1 + 0.08f, 0.03f, light);
			binBox(p, v, frame, side, BWIN_A, BWIN_B, BWIN_Y0 - 0.08f, BWIN_Y0, 0.03f, light);
			binBox(p, v, frame, side, BWIN_A, BWIN_B, BWIN_Y1, BWIN_Y1 + 0.08f, 0.03f, light);
			for (int q = 1; q <= 3; q++) {
				float y = BWIN_Y0 + (BWIN_Y1 - BWIN_Y0) * q / 4f;
				binBox(p, v, yellow, side, BWIN_A - 0.15f, BWIN_A + 0.25f, y - 0.03f, y + 0.03f, 0.035f, light);   // 100, 200, 300 t
			}
		}
	}

	/** A thin box on one wall of the bin (0 north, 1 south, 2 west, 3 east), across {@code a} to {@code b}, {@code out} proud of it. */
	private static void binBox(PoseStack.Pose p, VertexConsumer v, TextureAtlasSprite s, int side, float a, float b, float y0, float y1, float out, int light) {
		float t = 0.004f;
		switch (side) {
			case 0 -> box(p, v, s, a, y0, -out - t, b, y1, -out, light);
			case 1 -> box(p, v, s, 10 - b, y0, 10 + out, 10 - a, y1, 10 + out + t, light);
			case 2 -> box(p, v, s, -out - t, y0, 10 - b, -out, y1, 10 - a, light);
			default -> box(p, v, s, 10 + out, y0, a, 10 + out + t, y1, b, light);
		}
	}

	// ---- the shaft and the fans (films-hi07) -----------------------------------------------------------------------------------------

	/** A raise borer's drill string, rpm (proposed: real raise borers turn the reamer at a few to about 20 rpm). */
	static final float RAISE_RPM = 8;
	/** The head sheaves (2.9 m): radius, m, as shaft.py draws them (the drums' are in ShaftGeometry). */
	private static final float SHEAVE_R = 1.4f;

	/**
	 * Each skip's height on the client, for the drums and sheaves to turn with its rope: the skips hung within this part's reach, west
	 * or east compartment by where they hang across its frame (the hoist house stands in line behind the headframe). The cage's, for
	 * its own winder's drum (the cage whose rope leads to it) and its sheave in the headframe (cage-winder.md).
	 */
	private static void ropes(PlantBlockEntity plant, State state, float partialTick) {
		state.rope0 = state.rope1 = state.ropeCage = Float.NaN;
		state.coil0 = state.coil1 = state.coilCage = null;
		if (plant.getLevel() == null) {
			return;
		}
		BlockPos main = plant.getBlockPos();
		ShaftLayout.Frame own = new ShaftLayout.Frame(main, plant.getBlockState().getValue(dev.wildcatter.block.BigPartBlock.FACING));
		boolean frame = plant.machine() == Machine.HEADFRAME;
		AABB around = frame ? new AABB(main).inflate(10, 0, 10).expandTowards(0, -dev.wildcatter.tailings.shaft.Hoisting.MAX_DEPTH - 10, 0).expandTowards(0, 32, 0)
				: new AABB(main).inflate(dev.wildcatter.tailings.shaft.Hoisting.ROPE_REACH + 12, 0, dev.wildcatter.tailings.shaft.Hoisting.ROPE_REACH + 12)
						.expandTowards(0, -dev.wildcatter.tailings.shaft.Hoisting.MAX_DEPTH - 10, 0).expandTowards(0, 40, 0);
		for (dev.wildcatter.tailings.entity.ShaftSkipEntity skip : plant.getLevel().getEntitiesOfClass(dev.wildcatter.tailings.entity.ShaftSkipEntity.class, around)) {
			if (skip.cage()) {
				// the headframe's: the cage in its shaft; the cage winder's: the cage whose rope leads to its drum
				boolean mine = frame || plant.machine() == Machine.CAGE_WINDER && skip.drum() != null && skip.drum().distManhattan(main) <= 12;
				if (mine) {
					state.ropeCage = (float) net.minecraft.util.Mth.lerp(partialTick, skip.yo, skip.getY());
					if (plant.machine() == Machine.CAGE_WINDER) {
						state.coilCage = coil(own, skip);
					}
				}
				continue;
			}
			if (plant.machine() == Machine.CAGE_WINDER) {
				continue;
			}
			if (!frame && (skip.drum() == null || skip.drum().distManhattan(main) > 24)) {
				continue;
			}
			// the skip's centre across the part's frame (x east when it faces north)
			double cx = skip.getBoundingBox().getCenter().x - main.getX(), cz = skip.getBoundingBox().getCenter().z - main.getZ();
			double across = switch (Math.floorMod(state.turns, 4)) {
				case 1 -> cz;
				case 2 -> 1 - cx;
				case 3 -> 1 - cz;
				default -> cx;
			};
			float y = (float) (net.minecraft.util.Mth.lerp(partialTick, skip.yo, skip.getY()));
			if (across < 4.5) {
				state.rope0 = y;
			} else {
				state.rope1 = y;
			}
			if (!frame) {
				// each rope on its own drum, by where its end lies along the drums (fix3-hi07)
				float[] c = coil(own, skip);
				if (c != null && c[0] < (ShaftGeometry.HOIST_DRUMS[0][1] + ShaftGeometry.HOIST_DRUMS[1][0]) / 2) {
					state.coil0 = c;
				} else if (c != null) {
					state.coil1 = c;
				}
			}
		}
	}

	/** A conveyance's rope on this part's drum, in its own frame: {end x, y, z; wound; sheave x}; null for none. */
	private static float @Nullable [] coil(ShaftLayout.Frame own, dev.wildcatter.tailings.entity.ShaftSkipEntity skip) {
		Vec3 end = skip.ropeEnd();
		if (end == null) {
			return null;
		}
		double[] e = own.local(end), sheave = own.local(skip.getBoundingBox().getCenter());
		return new float[] {(float) e[0], (float) e[1], (float) e[2], skip.wound(), (float) sheave[0]};
	}

	/**
	 * A rope's coil on a drum from {@code x0} to {@code x1}, centre ({@code cy}, {@code cz}), radius {@code r} (fix3-hi07): a helix from the
	 * flange farther from its sheave to where it leaves at its end, laid at {@link ShaftGeometry#PITCH} a wrap, the whole length wound on.
	 */
	private static void coilOn(PoseStack at, VertexConsumer v, float[] c, float x0, float x1, float cy, float cz, float r, int light) {
		// a light steel grey, so the wraps read on the dark barrel (fix3-hi07)
		TextureAtlasSprite rope = vanilla("light_gray_concrete");
		double rr = r + ShaftGeometry.ROPE_R;
		double[] k = ShaftGeometry.coil(x0, x1, c[4], c[3], r);
		double start = k[0], dir = k[1], front = k[2];
		boolean over = c[1] > cy;
		double alpha = Math.atan2(c[1] - cy, c[2] - cz);
		double len = Math.max(0, c[3]);
		int n = Math.max(1, (int) Math.ceil(len / (2 * Math.PI * rr / 20)));
		double[] prev = null;
		for (int i = 0; i <= n; i++) {
			double sLen = len * i / n;
			double a = alpha + (over ? -1 : 1) * sLen / rr;
			double x = front - dir * Math.min(Math.abs(front - start), ShaftGeometry.PITCH * sLen / (2 * Math.PI * rr));
			double[] pt = {x, cy + rr * Math.sin(a), cz + rr * Math.cos(a)};
			if (prev != null) {
				GantryRenderer.rod(at.last(), v, rope, (float) prev[0], (float) prev[1], (float) prev[2], (float) pt[0], (float) pt[1], (float) pt[2], 0.1f, light);
			}
			prev = pt;
		}
	}

	/** The two head sheaves' six spokes and hubs (shaft.py: x 3 and 6, centre 27.5 m up, 5.5 m south), turned as far as each rope moved. */
	private static void sheaves(PoseStack at, VertexConsumer v, float rope0, float rope1, int light) {
		TextureAtlasSprite blue = mat("blue"), iron = mat("iron");
		float[] xs = {3.0f, 6.0f};
		float[] ropes = {rope0, rope1};
		for (int k = 0; k < 2; k++) {
			float turn = Float.isNaN(ropes[k]) ? 0 : (float) Math.toDegrees(ropes[k] / SHEAVE_R);
			for (int b = 0; b < 3; b++) {
				PoseStack p = new PoseStack();
				p.last().pose().set(at.last().pose());
				p.last().normal().set(at.last().normal());
				p.translate(0, 27.5f, 5.5f);
				p.rotateDegrees(Axis.XP, turn + b * 60f);
				box(p.last(), v, blue, xs[k] - 0.04f, -0.06f, -1.38f, xs[k] + 0.04f, 0.06f, 1.38f, light);
				if (b == 0) {
					box(p.last(), v, iron, xs[k] - 0.12f, -0.2f, -0.2f, xs[k] + 0.12f, 0.2f, 0.2f, light);
				}
			}
		}
	}

	/**
	 * The hoist's two drums (shaft.py: x 0.8-4.3 and 5.3-8.8, centre 3.4 m up, 3 m south), each with its own rope (fix3-hi07): the rope's
	 * coil drawn from the flange farther from its sheave to where it leaves for the sheave, and four spokes and a small yellow mark on
	 * each flange, turned by the rope wound on. One shaft: the west rope winds over its drum's top and the east one under its bottom, so
	 * one pays out as the other takes in and both drums turn together.
	 */
	private static void drums(PoseStack at, VertexConsumer v, float @Nullable [] coil0, float @Nullable [] coil1, int light) {
		TextureAtlasSprite blue = mat("blue"), mark = vanilla("yellow_concrete");
		float cy = (float) ShaftGeometry.HOIST_DRUM_Y, cz = (float) ShaftGeometry.HOIST_DRUM_Z;
		float[][] coils = {coil0, coil1};
		// the shaft's turn: from whichever rope is known, over (west) and under (east) giving the same turn
		float r = (float) ShaftGeometry.HOIST_DRUM_R;
		float turn = coil0 != null ? turnOf(coil0, cy, r) : coil1 != null ? turnOf(coil1, cy, r) : 0;
		for (int k = 0; k < 2; k++) {
			float x0 = (float) ShaftGeometry.HOIST_DRUMS[k][0], x1 = (float) ShaftGeometry.HOIST_DRUMS[k][1];
			if (coils[k] != null) {
				coilOn(at, v, coils[k], x0, x1, cy, cz, r, light);
			}
			for (int b = 0; b < 4; b++) {
				PoseStack p = new PoseStack();
				p.last().pose().set(at.last().pose());
				p.last().normal().set(at.last().normal());
				p.translate(0, cy, cz);
				p.rotateDegrees(Axis.XP, turn + b * 90f);
				box(p.last(), v, blue, x0 - 0.14f, 0.25f, -0.12f, x0 - 0.1f, 1.65f, 0.12f, light);
				box(p.last(), v, blue, x1 + 0.1f, 0.25f, -0.12f, x1 + 0.14f, 1.65f, 0.12f, light);
				if (b == 0) {
					box(p.last(), v, mark, x0 - 0.16f, 1.3f, -0.15f, x0 - 0.1f, 1.62f, 0.15f, light);
					box(p.last(), v, mark, x1 + 0.1f, 1.3f, -0.15f, x1 + 0.16f, 1.62f, 0.15f, light);
				}
			}
		}
	}

	/** A drum's turn about its axis (Axis.XP), degrees, from its rope: deg(wound / r), one way for a rope over its top, the other under. */
	private static float turnOf(float[] coil, float cy, float r) {
		float deg = (float) Math.toDegrees(coil[3] / (r + ShaftGeometry.ROPE_R));
		return coil[1] > cy ? deg : -deg;
	}

	/** The cage sheave (shaft.py: 2.5 m, x 4, its axle 26.95 m up and 7.25 m south): six spokes and a hub, turned as far as the cage moved. */
	private static void cageSheave(PoseStack at, VertexConsumer v, float rope, int light) {
		if (Float.isNaN(rope)) {
			return;
		}
		TextureAtlasSprite blue = mat("blue"), iron = mat("iron");
		float r = (float) (dev.wildcatter.tailings.shaft.Hoisting.CAGE_SHEAVE_DIAMETER / 2);
		float turn = (float) Math.toDegrees(rope / r);
		for (int b = 0; b < 3; b++) {
			PoseStack p = new PoseStack();
			p.last().pose().set(at.last().pose());
			p.last().normal().set(at.last().normal());
			p.translate(0, (float) ShaftLayout.CAGE_SHEAVE_Y, (float) ShaftLayout.CAGE_SHEAVE_Z);
			p.rotateDegrees(Axis.XP, turn + b * 60f);
			float x = (float) ShaftLayout.CAGE_SHEAVE_X;
			box(p.last(), v, blue, x - 0.04f, -0.06f, -(r - 0.08f), x + 0.04f, 0.06f, r - 0.08f, light);
			if (b == 0) {
				box(p.last(), v, iron, x - 0.12f, -0.18f, -0.18f, x + 0.12f, 0.18f, 0.18f, light);
				// a yellow mark on one spoke, so its turn reads from the ground
				box(p.last(), v, vanilla("yellow_concrete"), x - 0.07f, -0.09f, r - 0.6f, x + 0.07f, 0.09f, r - 0.1f, light);
			}
		}
	}

	/**
	 * The cage winder's drum (shaft.py: 2.5 m, x 2-5, its centre 2.25 m up and 2.5 m south): its rope's coil from the flange farther from
	 * the cage sheave to where it leaves over the drum's top (fix3-hi07), four spokes on each flange and a yellow mark on each, turned
	 * by the rope wound on.
	 */
	private static void cageDrum(PoseStack at, VertexConsumer v, float @Nullable [] coil, int light) {
		TextureAtlasSprite blue = mat("blue"), mark = vanilla("yellow_concrete");
		float r = (float) ShaftGeometry.CAGE_DRUM_R, cy = (float) ShaftGeometry.CAGE_DRUM_Y, cz = (float) ShaftGeometry.CAGE_DRUM_Z;
		float x0 = (float) ShaftGeometry.CAGE_DRUM[0], x1 = (float) ShaftGeometry.CAGE_DRUM[1];
		float turn = coil == null ? 0 : turnOf(coil, cy, r);
		if (coil != null) {
			coilOn(at, v, coil, x0, x1, cy, cz, r, light);
		}
		for (int b = 0; b < 4; b++) {
			PoseStack p = new PoseStack();
			p.last().pose().set(at.last().pose());
			p.last().normal().set(at.last().normal());
			p.translate(0, cy, cz);
			p.rotateDegrees(Axis.XP, turn + b * 90f);
			box(p.last(), v, blue, x0 - 0.14f, 0.2f, -0.1f, x0 - 0.1f, r + 0.15f, 0.1f, light);
			box(p.last(), v, blue, x1 + 0.1f, 0.2f, -0.1f, x1 + 0.14f, r + 0.15f, 0.1f, light);
			if (b == 0) {
				box(p.last(), v, mark, x0 - 0.16f, r - 0.1f, -0.2f, x0 - 0.1f, r + 0.22f, 0.2f, light);
				box(p.last(), v, mark, x1 + 0.1f, r - 0.1f, -0.2f, x1 + 0.16f, r + 0.22f, 0.2f, light);
			}
		}
	}

	/** An axial fan's impeller: {@code blades} pitched blades and a hub, round the z axis at (cx, cy), {@code z} south of its north face. */
	private static void impeller(PoseStack at, VertexConsumer v, float angle, float cx, float cy, float z, float r, int blades, float width, int light) {
		TextureAtlasSprite iron = mat("iron"), steel = mat("steel");
		for (int b = 0; b < blades; b++) {
			PoseStack p = new PoseStack();
			p.last().pose().set(at.last().pose());
			p.last().normal().set(at.last().normal());
			p.translate(cx, cy, z);
			p.rotateDegrees(Axis.ZP, angle + b * 360f / blades);
			p.rotateDegrees(Axis.YP, 30);
			box(p.last(), v, iron, -width / 2, r * 0.22f, -0.025f, width / 2, r, 0.025f, light);
		}
		PoseStack p = new PoseStack();
		p.last().pose().set(at.last().pose());
		p.last().normal().set(at.last().normal());
		p.translate(cx, cy, z);
		p.rotateDegrees(Axis.ZP, angle);
		box(p.last(), v, steel, -r * 0.22f, -r * 0.22f, -0.09f, r * 0.22f, r * 0.22f, 0.09f, light);
		p.rotateDegrees(Axis.ZP, 45);
		box(p.last(), v, steel, -r * 0.22f, -r * 0.22f, -0.09f, r * 0.22f, r * 0.22f, 0.09f, light);
	}

	/**
	 * A raise borer's tools in the hole under its drive head (shaft.py: the rod at x 2.5, z 3.5): while it drills the pilot, the drill
	 * string of 1.5 m rods down to its bit; while it reams, the string down to the 5 m reamer head, its cutters on top, rising up the
	 * raise as it cuts. The string and the reamer turn with the drive head.
	 */
	private static void raiseTools(PoseStack at, VertexConsumer v, float angle, float pilot, float reamer, int light, int deepLight) {
		TextureAtlasSprite steel = mat("steel"), iron = mat("iron"), paint = mat("blue"), black = vanilla("black_concrete");
		float bottom = !Float.isNaN(reamer) ? reamer : pilot;
		PoseStack p = new PoseStack();
		p.last().pose().set(at.last().pose());
		p.last().normal().set(at.last().normal());
		p.translate(2.5f, 0, 3.5f);
		p.rotateDegrees(Axis.YP, angle);
		// the rotary spindle under the drive head, its four lugs showing the turn
		box(p.last(), v, iron, -0.35f, 2.3f, -0.08f, 0.35f, 2.6f, 0.08f, light);
		box(p.last(), v, iron, -0.08f, 2.3f, -0.35f, 0.08f, 2.6f, 0.35f, light);
		if (Float.isNaN(bottom)) {
			return;
		}
		// the string: a rod a 1.5 m length, a tool joint at each, a painted stripe down one side
		box(p.last(), v, steel, -0.14f, -bottom, -0.14f, 0.14f, 0, 0.14f, deepLight);
		box(p.last(), v, black, 0.14f, -bottom, -0.03f, 0.16f, 0, 0.03f, deepLight);
		for (float y = 1.5f; y < bottom; y += 1.5f) {
			box(p.last(), v, iron, -0.18f, -y - 0.12f, -0.18f, 0.18f, -y + 0.12f, 0.18f, deepLight);
		}
		if (Float.isNaN(reamer)) {
			// the pilot bit: a 300 mm tricone
			box(p.last(), v, iron, -0.15f, -pilot - 0.05f, -0.15f, 0.15f, -pilot + 0.35f, 0.15f, deepLight);
			return;
		}
		// the reamer head under its cutting face: a body, a round plate 4.8 m across, and its cutters in rows on top, turning. Seen
		// from the drive under it, so its underside is lit as if from below (lit for the camera, as the raise is), and three yellow
		// arms under the plate show it turning (heavy-iron ep 7 fix: from below it read as a dim disc)
		UNDERLIT = true;
		try {
			reamer(p, v, reamer, steel, iron, paint, deepLight);
		} finally {
			UNDERLIT = false;
		}
	}

	/** Downward faces lit as if they faced up: a part seen only from below (the reamer from the drive). Render thread only. */
	private static boolean UNDERLIT;

	private static void reamer(PoseStack p, VertexConsumer v, float reamer, TextureAtlasSprite steel, TextureAtlasSprite iron, TextureAtlasSprite paint, int deepLight) {
		TextureAtlasSprite yellow = vanilla("yellow_concrete");
		float face = -reamer;
		box(p.last(), v, paint, -0.6f, face - 1.4f, -0.6f, 0.6f, face - 0.3f, 0.6f, deepLight);
		for (int k = 0; k < 4; k++) {
			PoseStack q = new PoseStack();
			q.last().pose().set(p.last().pose());
			q.last().normal().set(p.last().normal());
			q.rotateDegrees(Axis.YP, k * 22.5f);
			box(q.last(), v, paint, -2.4f, face - 0.45f, -0.95f, 2.4f, face - 0.2f, 0.95f, deepLight);
			box(q.last(), v, paint, -0.95f, face - 0.45f, -2.4f, 0.95f, face - 0.2f, 2.4f, deepLight);
		}
		for (int k = 0; k < 8; k++) {
			PoseStack q = new PoseStack();
			q.last().pose().set(p.last().pose());
			q.last().normal().set(p.last().normal());
			q.rotateDegrees(Axis.YP, k * 45f + (k % 2) * 12f);
			for (float r = 0.4f + (k % 2) * 0.5f; r < 2.3f; r += 1.0f) {
				box(q.last(), v, iron, r - 0.18f, face - 0.2f, -0.13f, r + 0.18f, face, 0.13f, deepLight);
			}
			// the gauge cutters round the rim, standing out past the plate's edge
			box(q.last(), v, steel, 2.3f, face - 0.4f, -0.16f, 2.48f, face, 0.16f, deepLight);
		}
		// the stabiliser arms under the plate, out from the body to the rim: three, so the turn reads
		for (int k = 0; k < 3; k++) {
			PoseStack q = new PoseStack();
			q.last().pose().set(p.last().pose());
			q.last().normal().set(p.last().normal());
			q.rotateDegrees(Axis.YP, k * 120f);
			box(q.last(), v, yellow, 0.55f, face - 0.75f, -0.16f, 2.3f, face - 0.45f, 0.16f, deepLight);
		}
		// the stem's nut under the body, yellow on one side
		box(p.last(), v, iron, -0.35f, face - 1.7f, -0.35f, 0.35f, face - 1.4f, 0.35f, deepLight);
		box(p.last(), v, yellow, 0.35f, face - 1.7f, -0.12f, 0.6f, face - 1.4f, 0.12f, deepLight);
	}

	/** Where a measuring flask's body is in its frame (shaft.py: x and z 0.15 to 1.85, y 1.2 to 2.6), and its sight windows. */
	private static final float FLASK_LO = 0.15f, FLASK_HI = 1.85f, WIN_Y0 = 1.3f, WIN_Y1 = 2.5f, WIN_A = 0.45f, WIN_B = 1.55f;

	/**
	 * A measuring flask's sight windows (heavy-iron ep 7 fix: its fill showed only on the readout): on its west, east and south faces
	 * (the north one carries its load cell readout), a dark window with the flask's rock risen to its fill, a yellow mark at its 3 t.
	 */
	private static void flask(PoseStack.Pose p, VertexConsumer v, float fill, String rockId, int light) {
		TextureAtlasSprite dark = vanilla("black_concrete"), yellow = vanilla("yellow_concrete"), frame = mat("iron"), rock = blockSprite(rockId);
		float top = WIN_Y0 + (WIN_Y1 - WIN_Y0) * Math.max(0, Math.min(1, fill));
		for (int side = 0; side < 3; side++) {
			// each window as boxes standing proud of the face: d0 the face, out the way out
			for (float[] layer : new float[][] {{0.005f, 0}, {0.012f, 1}, {0.03f, 2}}) {
				float d = layer[0];
				int what = (int) layer[1];
				if (what == 1 && fill <= 0.005f) {
					continue;
				}
				float y0 = what == 2 ? WIN_Y0 - 0.06f : WIN_Y0, y1 = what == 0 ? WIN_Y1 : what == 1 ? top : WIN_Y1 + 0.06f;
				if (what == 2) {
					// the frame: four bars, and the yellow 3 t mark across the top
					flaskBox(p, v, frame, side, WIN_A - 0.06f, WIN_A, y0, y1, d, light);
					flaskBox(p, v, frame, side, WIN_B, WIN_B + 0.06f, y0, y1, d, light);
					flaskBox(p, v, frame, side, WIN_A, WIN_B, y0, WIN_Y0, d, light);
					flaskBox(p, v, yellow, side, WIN_A, WIN_B, WIN_Y1 - 0.03f, WIN_Y1 + 0.06f, d, light);
				} else {
					flaskBox(p, v, what == 0 ? dark : rock, side, WIN_A, WIN_B, y0, y1, d, light);
				}
			}
		}
	}

	/** A thin box on one face of the flask (0 west, 1 east, 2 south), across {@code a} to {@code b}, standing {@code out} proud of it. */
	private static void flaskBox(PoseStack.Pose p, VertexConsumer v, TextureAtlasSprite s, int side, float a, float b, float y0, float y1, float out, int light) {
		float t = 0.004f;
		switch (side) {
			case 0 -> box(p, v, s, FLASK_LO - out - t, y0, a, FLASK_LO - out, y1, b, light);
			case 1 -> box(p, v, s, FLASK_HI + out, y0, a, FLASK_HI + out + t, y1, b, light);
			default -> box(p, v, s, a, y0, FLASK_HI + out, b, y1, FLASK_HI + out + t, light);
		}
	}

	/** A block's own texture by its id, or cobblestone. */
	private static TextureAtlasSprite blockSprite(String blockId) {
		Identifier id = blockId == null || blockId.isEmpty() ? null : Identifier.tryParse(blockId);
		var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		if (id != null) {
			TextureAtlasSprite s = atlas.getSprite(Identifier.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath()));
			if (!s.contents().name().getPath().equals("missingno")) {
				return s;
			}
		}
		return atlas.getSprite(Identifier.withDefaultNamespace("block/cobblestone"));
	}

	private static TextureAtlasSprite vanilla(String name) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Identifier.withDefaultNamespace("block/" + name));
	}

	// ---- drawing, as the model tool draws: boxes, octagons and many-sided round parts ----------------------------------------------

	private static TextureAtlasSprite mat(String name) {
		String file = switch (name) {
			case "iron" -> "mat_cast_iron";
			case "blue" -> "mat_paint_ansi70";
			case "green" -> "mat_paint_pad_green";
			case "brass" -> "mat_brass";
			case "polymer" -> "mat_polymer_grey";
			default -> "mat_stainless";
		};
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Identifier.fromNamespaceAndPath("wildcatter", "block/mat/" + file));
	}

	/** A round part along x: {@code sides / 2} slabs each turned about the axis, as mlib's cylinder. */
	private static void cylinderX(PoseStack.Pose base, VertexConsumer v, TextureAtlasSprite s, float x0, float x1, float cy, float cz, float r, int sides, int light) {
		float w = (float) (2 * r * Math.tan(Math.PI / sides));
		for (int k = 0; k < sides / 2; k++) {
			PoseStack at = new PoseStack();
			at.last().pose().set(base.pose());
			at.last().normal().set(base.normal());
			at.translate(0, cy, cz);
			at.rotateDegrees(Axis.XP, k * 360f / sides);
			box(at.last(), v, s, x0, -w / 2, -r, x1, w / 2, r, light);
		}
	}

	/** A round body along x as pitline.py's octo: three boxes. */
	private static void octoX(PoseStack.Pose p, VertexConsumer v, TextureAtlasSprite s, float x0, float x1, float y, float z, float r, int light) {
		float a = r * 0.92f, b = r * 0.38f, c = r * 0.65f;
		box(p, v, s, x0, y - b, z - a, x1, y + b, z + a, light);
		box(p, v, s, x0, y - a, z - b, x1, y + a, z + b, light);
		box(p, v, s, x0, y - c, z - c, x1, y + c, z + c, light);
	}

	/** A round lid as tlib's disc: three flat boxes. */
	private static void disc(PoseStack.Pose p, VertexConsumer v, TextureAtlasSprite s, float cx, float cz, float r, float y0, float y1, int light) {
		float a = r * 0.92f, b = r * 0.38f, c = r * 0.65f;
		box(p, v, s, cx - b, y0, cz - a, cx + b, y1, cz + a, light);
		box(p, v, s, cx - a, y0, cz - b, cx + a, y1, cz + b, light);
		box(p, v, s, cx - c, y0, cz - c, cx + c, y1, cz + c, light);
	}

	static void box(PoseStack.Pose p, VertexConsumer v, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
		if (x1 - x0 < 1e-4f || y1 - y0 < 1e-4f || z1 - z0 < 1e-4f) {
			return;
		}
		float[][] c = new float[8][];
		for (int i = 0; i < 8; i++) {
			c[i] = new float[] {(i & 4) != 0 ? x1 : x0, (i & 1) != 0 ? y1 : y0, (i & 2) != 0 ? z1 : z0};
		}
		hexa(p, v, s, c, light);
	}

	/** Six faces between eight corners, indexed east (4) + south (2) + top (1). */
	private static void hexa(PoseStack.Pose p, VertexConsumer v, TextureAtlasSprite s, float[][] c, int light) {
		quad(p, v, s, c[1], c[3], c[7], c[5], new float[] {0, 1, 0}, light);
		quad(p, v, s, c[0], c[4], c[6], c[2], new float[] {0, -1, 0}, light);
		quad(p, v, s, c[0], c[1], c[5], c[4], new float[] {0, 0, -1}, light);
		quad(p, v, s, c[2], c[6], c[7], c[3], new float[] {0, 0, 1}, light);
		quad(p, v, s, c[0], c[2], c[3], c[1], new float[] {-1, 0, 0}, light);
		quad(p, v, s, c[4], c[5], c[7], c[6], new float[] {1, 0, 0}, light);
	}

	/** One face wound to face {@code out}, textured as a block model is: a sixteenth of the sprite a sixteenth of a block. */
	private static void quad(PoseStack.Pose p, VertexConsumer v, TextureAtlasSprite s, float[] a, float[] b, float[] c, float[] d, float[] out, int light) {
		float[] e1 = sub(b, a), e2 = sub(d, a);
		float[] n = {e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]};
		float len = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
		if (len < 1e-9f) {
			return;
		}
		float[][] q = {a, b, c, d};
		if (n[0] * out[0] + n[1] * out[1] + n[2] * out[2] < 0) {
			q = new float[][] {a, d, c, b};
			n = new float[] {-n[0], -n[1], -n[2]};
		}
		if (UNDERLIT && n[1] < 0) {
			n = new float[] {0, len, 0};
		}
		float la = Math.min(1, norm(sub(q[1], q[0]))), lb = Math.min(1, norm(sub(q[3], q[0])));
		float[][] uv = {{0, 0}, {la, 0}, {la, lb}, {0, lb}};
		for (int k = 0; k < 4; k++) {
			v.addVertex(p, q[k][0], q[k][1], q[k][2]).setColor(0xFFFFFFFF).setUv(s.getU(uv[k][0]), s.getV(uv[k][1]))
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, n[0] / len, n[1] / len, n[2] / len);
		}
	}

	private static float[] sub(float[] a, float[] b) {
		return new float[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
	}

	private static float norm(float[] a) {
		return (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
	}
}
