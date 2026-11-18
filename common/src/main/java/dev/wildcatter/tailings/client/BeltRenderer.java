package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.wildcatter.tailings.block.BeltBlockEntity;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.HeavyBeltBlock;
import dev.wildcatter.tailings.plant.BeltGrade;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.rock.Rock;
import java.util.List;
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
import net.minecraft.core.Direction;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * The lumps a belt carries (docs/multiblocks/crushing-circuit.md: "it shows the lumps it carries"): small cubes in the run's main
 * rock's texture, more as the run fills, sliding toward its head end at 2 m/s while the drive runs. On a grade ({@link BeltGrade})
 * it also draws the belt itself, its frame and its legs, following the grade's heights, and the lumps ride up or down it.
 */
public class BeltRenderer implements BlockEntityRenderer<BeltBlockEntity, BeltRenderer.State> {
	/** The belt's surface, 0.75 m up, as the belt block's shape. */
	private static final float SURFACE = 12 / 16f;
	private static final float LUMP = 0.16f;
	private static final int MOST = 4;

	public static class State extends BlockEntityRenderState {
		String rock = "";
		float fill;
		Direction toward;
		float phase;
		boolean sloped;
		boolean heavy;
		Direction.Axis axis = Direction.Axis.Z;
		final float[] grade = new float[3];
		int drop;
		/** Whether this metre carries a trestle down to the ground: every third block along the run. */
		boolean trestle;
	}

	/**
	 * One part of a belt's frame as drawn on a grade, in the block model's metres (tools/models/belt.py and pitline.py): across the
	 * belt from {@code v0} to {@code v1}, from {@code d0} to {@code d1} above the belt's surface (or, for a leg, from the ground up to
	 * {@code d1} above it, {@code u0} to {@code u1} along the block).
	 */
	private record Part(String tex, float v0, float v1, float d0, float d1, boolean leg, float u0, float u1) {
		static Part run(String tex, float v0, float v1, float d0, float d1) {
			return new Part(tex, v0, v1, d0, d1, false, 0, 1);
		}

		static Part leg(float v0, float v1, float u0, float u1, float top) {
			return new Part(GALV, v0, v1, 0, top, true, u0, u1);
		}
	}

	private static final String GALV = "mat_galvanized", RUBBER = "mat_rubber", STEEL = "mat_stainless";
	/** The 600 mm belt: belt.py's boxes, measured from the belt's surface (0.75 m on the flat). */
	private static final List<Part> BELT = List.of(
			Part.leg(0.1f, 0.16f, 0.1f, 0.18f, -0.1f), Part.leg(0.1f, 0.16f, 0.82f, 0.9f, -0.1f),
			Part.leg(0.84f, 0.9f, 0.1f, 0.18f, -0.1f), Part.leg(0.84f, 0.9f, 0.82f, 0.9f, -0.1f),
			Part.run(GALV, 0.1f, 0.16f, -0.2f, -0.1f), Part.run(GALV, 0.84f, 0.9f, -0.2f, -0.1f),             // the stringers
			new Part(STEEL, 0.16f, 0.84f, -0.18f, -0.08f, false, 0.45f, 0.55f),                              // a carrying idler set
			Part.run(RUBBER, 0.2f, 0.8f, -0.08f, -0.03f),                                                      // the belt's flat
			Part.run(RUBBER, 0.12f, 0.2f, -0.03f, 0.05f), Part.run(RUBBER, 0.8f, 0.88f, -0.03f, 0.05f),       // its troughed edges
			Part.run(RUBBER, 0.2f, 0.8f, -0.6f, -0.57f));                                                      // the return belt
	/** The heavy belt: pitline.py's boxes, measured the same way. */
	private static final List<Part> HEAVY = List.of(
			Part.leg(0f, 0.08f, 0.1f, 0.2f, -0.05f), Part.leg(0f, 0.08f, 0.8f, 0.9f, -0.05f),
			Part.leg(0.92f, 1f, 0.1f, 0.2f, -0.05f), Part.leg(0.92f, 1f, 0.8f, 0.9f, -0.05f),
			Part.run(GALV, 0f, 0.08f, -0.15f, -0.05f), Part.run(GALV, 0.92f, 1f, -0.15f, -0.05f),
			new Part(STEEL, 0.1f, 0.9f, -0.15f, -0.03f, false, 0.44f, 0.56f),
			Part.run(RUBBER, 0.18f, 0.82f, -0.03f, 0.03f),
			Part.run(RUBBER, 0.08f, 0.18f, 0.01f, 0.11f), Part.run(RUBBER, 0.82f, 0.92f, 0.01f, 0.11f),
			Part.run(RUBBER, 0.1f, 0.9f, -0.6f, -0.56f));
	/** Over open ground a trestle every this many metres carries the belt down to the ground. */
	private static final int TRESTLE_EVERY = 3;
	/** A belt's legs reach down through at most this many open blocks to the ground. */
	private static final int MOST_DROP = 24;

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(BeltBlockEntity belt, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.CrumblingOverlay breakProgress) {
		BlockEntityRenderState.extractBase(belt, state, breakProgress);
		state.rock = belt.rock();
		state.fill = belt.fill();
		state.toward = belt.toward();
		double ticks = belt.getLevel() == null ? 0 : belt.getLevel().getGameTime() + partialTick;
		state.phase = belt.moving() ? (float) ((ticks * PlantRates.BELT_METRES_PER_SECOND / 20) % 1.0) : 0;
		BlockState block = belt.getBlockState();
		state.sloped = block.getBlock() instanceof BeltConveyorBlock && block.getValue(BeltConveyorBlock.SLOPED);
		state.heavy = block.getBlock() instanceof HeavyBeltBlock;
		state.axis = block.getBlock() instanceof BeltConveyorBlock ? block.getValue(BeltConveyorBlock.AXIS) : Direction.Axis.Z;
		System.arraycopy(belt.grade(), 0, state.grade, 0, 3);
		Level level = belt.getLevel();
		state.drop = level == null ? 0 : belt.drop(level.getGameTime(), () -> drop(level, belt.getBlockPos()));
		BlockPos at = belt.getBlockPos();
		state.trestle = Math.floorMod(state.axis == Direction.Axis.X ? at.getX() : at.getZ(), TRESTLE_EVERY) == 0;
	}

	/** Open blocks under a belt, down to what its legs stand on: anything solid, or another belt. */
	private static int drop(Level level, BlockPos pos) {
		BlockPos.MutableBlockPos at = pos.mutable();
		for (int n = 0; n < MOST_DROP; n++) {
			at.move(Direction.DOWN);
			BlockState below = level.getBlockState(at);
			if (below.getBlock() instanceof BeltConveyorBlock || !below.getCollisionShape(level, at).isEmpty() || !below.getFluidState().isEmpty()) {
				return n;
			}
		}
		return 0;
	}

	/** Drawn as far as the chunks are: a long incline seen from across the plant is still a belt. */
	@Override
	public int getViewDistance() {
		return 160;
	}

	@Override
	public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.sloped || state.drop > 0 && state.trestle) {
			collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, v) -> frame(pose, v, state));
		}
		if (state.rock.isEmpty() || state.fill <= 0 || state.toward == null) {
			return;
		}
		TextureAtlasSprite sprite = sprite(state.rock);
		int lumps = Math.max(1, Math.round(state.fill * MOST));
		float dx = state.toward.getStepX(), dz = state.toward.getStepZ();
		collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (pose, v) -> {
			for (int i = 0; i < lumps; i++) {
				float along = ((i + 0.5f) / lumps + state.phase) % 1f - 0.5f;   // -0.5 to 0.5 along the belt
				float across = ((i * 37) % 5 - 2) * 0.06f;                    // a little scatter across it
				float cx = 0.5f + along * dx + across * dz, cz = 0.5f + along * dz + across * dx;
				float s = LUMP * (0.8f + 0.1f * (i % 3));
				if (!state.sloped) {
					box(pose, v, sprite, cx - s / 2, SURFACE, cz - s / 2, cx + s / 2, SURFACE + s * 0.8f, cz + s / 2, state.lightCoords);
					continue;
				}
				// on a grade the lump sits on the belt under it, tilted with it
				boolean alongX = state.axis == Direction.Axis.X;
				float u = alongX ? cx : cz, w = alongX ? cz : cx;
				float u0 = u - s / 2, u1 = u + s / 2;
				float h0 = surface(state, u0), h1 = surface(state, u1);
				slab(pose, v, sprite, alongX, u0, u1, w - s / 2, w + s / 2, h0, h1, h0 + s * 0.8f, h1 + s * 0.8f, state.lightCoords);
			}
		});
	}

	private static float surface(State state, float u) {
		return (float) BeltGrade.surface(state.grade[0], state.grade[1], state.grade[2], Math.max(0, Math.min(1, u)));
	}

	/** The belt, its frame and legs on a grade, each part in two straight halves (the grade may bend at the block's middle). */
	private static void frame(PoseStack.Pose pose, VertexConsumer v, State state) {
		boolean alongX = state.axis == Direction.Axis.X;
		if (state.drop > 0 && state.trestle) {
			// the trestle's cross ties, one every three metres down
			Part first = (state.heavy ? HEAVY : BELT).get(0);
			float lo = state.heavy ? 0 : 0.1f, hi = state.heavy ? 1 : 0.9f;
			for (int k = 0; k * 3 < state.drop; k++) {
				float y = -k * 3 - 0.1f;
				slab(pose, v, material(GALV), alongX, first.u0() + 0.01f, first.u1() - 0.01f, lo, hi, y - 0.06f, y - 0.06f, y, y, state.lightCoords);
			}
		}
		for (Part p : state.heavy ? HEAVY : BELT) {
			TextureAtlasSprite tex = material(p.tex());
			if (p.leg() && state.drop > 0 && state.trestle && p.u0() < 0.5f) {
				// over open ground a trestle every few metres goes on down to it, so a raised belt stands on something
				slab(pose, v, tex, alongX, p.u0(), p.u1(), p.v0(), p.v1(), -state.drop, -state.drop, 0, 0, state.lightCoords);
			}
			if (!state.sloped) {
				continue;
			}
			if (p.leg()) {
				float top = surface(state, (p.u0() + p.u1()) / 2) + p.d1();
				slab(pose, v, tex, alongX, p.u0(), p.u1(), p.v0(), p.v1(), 0, 0, top, top, state.lightCoords);
				continue;
			}
			float[] cuts = p.u0() < 0.5f && p.u1() > 0.5f ? new float[] {p.u0(), 0.5f, p.u1()} : new float[] {p.u0(), p.u1()};
			for (int k = 0; k + 1 < cuts.length; k++) {
				float a = cuts[k], b = cuts[k + 1], ha = surface(state, a), hb = surface(state, b);
				// straight with the belt: at the foot of a grade the return belt dips into the ground under it, as a real one runs into its pit
				slab(pose, v, tex, alongX, a, b, p.v0(), p.v1(), ha + p.d0(), hb + p.d0(), ha + p.d1(), hb + p.d1(), state.lightCoords);
			}
		}
	}

	private static TextureAtlasSprite material(String name) {
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(Identifier.fromNamespaceAndPath("wildcatter", "block/mat/" + name));
	}

	/**
	 * A box with upright sides whose bottom and top slope along the block: {@code u0} to {@code u1} along it, {@code w0} to {@code w1}
	 * across it, its bottom from {@code b0} to {@code b1} and its top from {@code t0} to {@code t1}, in blocks above the floor.
	 */
	private static void slab(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite s, boolean alongX, float u0, float u1, float w0, float w1, float b0, float b1, float t0, float t1, int light) {
		float[][] c = new float[8][];   // index: along (0, 1) * 4 + across (0, 1) * 2 + up (0, 1)
		for (int i = 0; i < 8; i++) {
			float u = (i & 4) == 0 ? u0 : u1, w = (i & 2) == 0 ? w0 : w1;
			float y = (i & 1) == 0 ? ((i & 4) == 0 ? b0 : b1) : ((i & 4) == 0 ? t0 : t1);
			c[i] = alongX ? new float[] {u, y, w} : new float[] {w, y, u};
		}
		float[] along = alongX ? new float[] {1, 0, 0} : new float[] {0, 0, 1}, across = alongX ? new float[] {0, 0, 1} : new float[] {1, 0, 0};
		float[] up = {0, 1, 0};
		quad(pose, v, s, c[1], c[5], c[7], c[3], up, light);                   // top
		quad(pose, v, s, c[0], c[2], c[6], c[4], neg(up), light);              // bottom
		quad(pose, v, s, c[0], c[4], c[5], c[1], neg(across), light);          // the near side
		quad(pose, v, s, c[2], c[3], c[7], c[6], across, light);               // the far side
		quad(pose, v, s, c[0], c[1], c[3], c[2], neg(along), light);           // the start end
		quad(pose, v, s, c[4], c[6], c[7], c[5], along, light);                // the far end
	}

	private static float[] neg(float[] a) {
		return new float[] {-a[0], -a[1], -a[2]};
	}

	/** One face, wound to face {@code out}, its normal square to it, textured by its two widest spans. */
	private static void quad(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite s, float[] a, float[] b, float[] c, float[] d, float[] out, int light) {
		float[] e1 = sub(b, a), e2 = sub(d, a);
		float[] n = cross(e1, e2);
		float len = (float) Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
		if (len < 1e-7f) {
			return;
		}
		float[][] q = {a, b, c, d};
		if (n[0] * out[0] + n[1] * out[1] + n[2] * out[2] < 0) {
			q = new float[][] {a, d, c, b};
			n = neg(n);
		}
		// texture: block-model style, a sixteenth of the sprite a sixteenth of a block, along the face's two edges from its first corner
		float[] ea = sub(q[1], q[0]), eb = sub(q[3], q[0]);
		float la = norm(ea), lb = norm(eb);
		float[][] uv = {{0, 0}, {Math.min(1, la), 0}, {Math.min(1, la), Math.min(1, lb)}, {0, Math.min(1, lb)}};
		for (int k = 0; k < 4; k++) {
			v.addVertex(pose, q[k][0], q[k][1], q[k][2]).setColor(0xFFFFFFFF).setUv(s.getU(uv[k][0]), s.getV(uv[k][1]))
					.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, n[0] / len, n[1] / len, n[2] / len);
		}
	}

	private static float[] sub(float[] a, float[] b) {
		return new float[] {a[0] - b[0], a[1] - b[1], a[2] - b[2]};
	}

	private static float[] cross(float[] a, float[] b) {
		return new float[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
	}

	private static float norm(float[] a) {
		return (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]);
	}

	/** The rock's own block texture. */
	private static TextureAtlasSprite sprite(String rockName) {
		Identifier id;
		try {
			Rock rock = Rock.valueOf(rockName);
			id = switch (rock.kind()) {
				case COUNTRY -> Identifier.withDefaultNamespace("block/" + rock.id());
				case VANILLA -> Identifier.withDefaultNamespace("block/" + rock.id() + "_ore");
				case ORE -> Identifier.fromNamespaceAndPath("wildcatter", "block/ore/" + rock.id() + "_ore");
			};
		} catch (IllegalArgumentException ex) {
			id = Identifier.withDefaultNamespace("block/cobblestone");
		}
		return Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS).getSprite(id);
	}

	private static void box(PoseStack.Pose pose, VertexConsumer v, TextureAtlasSprite s, float x0, float y0, float z0, float x1, float y1, float z1, int light) {
		float[][] faces = {
				{x0, y0, z0, x1, y0, z0, x1, y1, z0, x0, y1, z0, 0, 0, -1},
				{x1, y0, z1, x0, y0, z1, x0, y1, z1, x1, y1, z1, 0, 0, 1},
				{x0, y0, z1, x0, y0, z0, x0, y1, z0, x0, y1, z1, -1, 0, 0},
				{x1, y0, z0, x1, y0, z1, x1, y1, z1, x1, y1, z0, 1, 0, 0},
				{x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0}};
		float[][] uv = {{0, 0.25f}, {0.25f, 0.25f}, {0.25f, 0}, {0, 0}};
		for (float[] f : faces) {
			for (int k = 3; k >= 0; k--) {
				v.addVertex(pose, f[k * 3], f[k * 3 + 1], f[k * 3 + 2]).setColor(0xFFFFFFFF).setUv(s.getU(uv[k][0]), s.getV(uv[k][1]))
						.setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, f[12], f[13], f[14]);
			}
		}
	}
}
