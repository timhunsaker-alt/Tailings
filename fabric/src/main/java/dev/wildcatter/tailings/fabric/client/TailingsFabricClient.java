package dev.wildcatter.tailings.fabric.client;

import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.DrillMenus;
import dev.wildcatter.tailings.client.BeltRenderer;
import dev.wildcatter.tailings.client.DrillScreen;
import dev.wildcatter.tailings.client.MineCarModel;
import dev.wildcatter.tailings.client.MineLocomotiveModel;
import dev.wildcatter.tailings.client.MineVehicleRenderer;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.NoopRenderer;

/** The Fabric client entry point. */
public final class TailingsFabricClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		MenuScreens.register(DrillMenus.DRILL, DrillScreen::new);
		ModelLayerRegistry.registerModelLayer(MineLocomotiveModel.LAYER, MineLocomotiveModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(MineCarModel.LAYER, MineCarModel::createBodyLayer);
		EntityRendererRegistry.register(TailingsEntities.DRILL, NoopRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.TRAIN, NoopRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.LOCOMOTIVE, MineVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.CAR, MineVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.TBM, dev.wildcatter.tailings.client.TbmRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.PUMP_GANTRY, dev.wildcatter.tailings.client.GantryRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.REEL_GANTRY, dev.wildcatter.tailings.client.GantryRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.CONVEYOR_GANTRY, dev.wildcatter.tailings.client.GantryRenderer::new);
		BlockEntityRenderers.register(TailingsBlocks.BELT, context -> new BeltRenderer());
		Tailings.LOGGER.info("Tailings client ready");
	}
}
