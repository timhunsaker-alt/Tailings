package dev.wildcatter.tailings.fabric.client;

import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.DrillMenus;
import dev.wildcatter.tailings.client.DrillScreen;
import dev.wildcatter.tailings.client.MineCarModel;
import dev.wildcatter.tailings.client.MineLocomotiveModel;
import dev.wildcatter.tailings.client.MineTrainRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.NoopRenderer;

/** The Fabric client entry point. */
public final class TailingsFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(DrillMenus.DRILL, DrillScreen::new);
		ModelLayerRegistry.registerModelLayer(MineLocomotiveModel.LAYER, MineLocomotiveModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(MineCarModel.LAYER, MineCarModel::createBodyLayer);
		EntityRendererRegistry.register(TailingsEntities.DRILL, NoopRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.TRAIN, MineTrainRenderer::new);
		Tailings.LOGGER.info("Tailings client ready");
	}
}
