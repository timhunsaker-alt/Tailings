package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Machine;
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
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(PlantBlockEntity plant, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(plant, state, crumbling);
		Machine machine = plant.machine();
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
			default -> new int[] {1, 0, 2};
		};
		if (plant.getLevel() == null) {
			state.light = state.lightCoords;
			return;
		}
		BlockPos there = cell(plant.getBlockPos(), state.turns, at);
		int in = LightCoordsUtil.getLightCoords(plant.getLevel(), there), over = LightCoordsUtil.getLightCoords(plant.getLevel(), there.above());
		state.light = Math.max(in & 0xFFFF, over & 0xFFFF) | Math.max(in >>> 16, over >>> 16) << 16;
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
		return new AABB(plant.getBlockPos()).inflate(13, 0, 13).expandTowards(0, 14, 0);
	}

	@Override
	public int getViewDistance() {
		return 128;
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
		int light = state.light;
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
