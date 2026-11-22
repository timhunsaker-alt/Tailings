package dev.wildcatter.tailings.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.data.AtlasIds;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The oversize on a rail dump pocket's 300 mm grizzly bars (dump-station.md, Oversize): each piece the bars hold back is drawn as a
 * boulder lying on them, on the side away from the track where the cars tip, in the rock it is, heaped up as they gather. 32 pieces
 * choke the grizzly; a pickaxe breaks them through one by one and they go. Drawn in the pocket's own frame (facing north: 4 wide, 3
 * high, 3 long; the track over x 1, the ramp over x 2), turned with it.
 */
public class DumpRenderer implements BlockEntityRenderer<DumpBlockEntity, DumpRenderer.State> {
	public static class State extends BlockEntityRenderState {
		final List<String> pieces = new ArrayList<>();
		int turns;
		int light;
	}

	@Override
	public State createRenderState() {
		return new State();
	}

	@Override
	public void extractRenderState(DumpBlockEntity pocket, State state, float partialTick, Vec3 cameraPos, ModelFeatureRenderer.@Nullable CrumblingOverlay crumbling) {
		BlockEntityRenderState.extractBase(pocket, state, crumbling);
		state.pieces.clear();
		if (pocket.pit()) {
			return;
		}
		state.pieces.addAll(pocket.barsDrawn());
		state.turns = BigParts.turns(pocket.getBlockState());
		state.light = pocket.getLevel() == null ? state.lightCoords : LightCoordsUtil.getLightCoords(pocket.getLevel(), pocket.getBlockPos().above(3));
	}

	@Override
	public int getViewDistance() {
		return 96;
	}

	@Override
	public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
		if (state.pieces.isEmpty()) {
			return;
		}
		List<String> pieces = List.copyOf(state.pieces);
		int light = state.light;
		pose.pushPose();
		pose.translate(0.5f, 0, 0.5f);
		pose.rotateDegrees(Axis.YP, -90f * state.turns);
		pose.translate(-0.5f, 0, -0.5f);
		collector.submitCustomGeometry(pose, RenderTypes.entityCutout(TextureAtlas.LOCATION_BLOCKS), (p, v) -> {
			for (int i = 0; i < pieces.size(); i++) {
				// two across the free cell beside the track, six along the pocket, then the next layer up, each a little turned
				int layer = i / 12, k = i % 12;
				int across = k % 2, along = k / 2;
				long h = (i * 0x9E3779B97F4A7C15L) >>> 40;
				float jx = ((h & 0xFF) / 255f - 0.5f) * 0.12f, jz = (((h >> 8) & 0xFF) / 255f - 0.5f) * 0.12f;
				float size = 0.36f + ((h >> 16) & 0xFF) / 255f * 0.14f;
				float cx = 0.27f + across * 0.46f + jx + (layer % 2) * 0.1f, cz = 0.27f + along * 0.49f + jz + (layer % 2) * 0.2f;
				float y0 = 3.0f + layer * 0.32f;
				PoseStack at = new PoseStack();
				at.last().pose().set(p.pose());
				at.last().normal().set(p.normal());
				at.translate(cx, y0 + size / 2, cz);
				at.rotateDegrees(Axis.YP, ((h >> 24) & 0xFF) / 255f * 90f);
				at.rotateDegrees(Axis.XP, (((h >> 32) & 0xFF) / 255f - 0.5f) * 30f);
				float a = size / 2;
				PlantRenderer.box(at.last(), v, sprite(pieces.get(i)), -a, -a, -a * 0.85f, a, a * 0.8f, a * 0.9f, light);
			}
		});
		pose.popPose();
	}

	/** The piece's own rock: its block's texture, or cobblestone for a rock with no block of its own. */
	private static TextureAtlasSprite sprite(String itemId) {
		Identifier id = Identifier.tryParse(itemId);
		var atlas = Minecraft.getInstance().getAtlasManager().getAtlasOrThrow(AtlasIds.BLOCKS);
		if (id != null) {
			TextureAtlasSprite s = atlas.getSprite(Identifier.fromNamespaceAndPath(id.getNamespace(), "block/" + id.getPath()));
			if (!s.contents().name().getPath().equals("missingno")) {
				return s;
			}
		}
		return atlas.getSprite(Identifier.withDefaultNamespace("block/cobblestone"));
	}
}
