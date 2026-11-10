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
		dev.wildcatter.tailings.entity.SoundingEntity.Hook.start = dev.wildcatter.tailings.client.EntitySound::start;
		MenuScreens.register(DrillMenus.DRILL, DrillScreen::new);
		MenuScreens.register(dev.wildcatter.tailings.block.PitPanelMenu.TYPE, dev.wildcatter.tailings.client.PitPanelScreen::new);
		MenuScreens.register(DrillMenus.SORTER, dev.wildcatter.tailings.client.SorterScreen::new);
		ModelLayerRegistry.registerModelLayer(MineLocomotiveModel.LAYER, MineLocomotiveModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(MineCarModel.LAYER, MineCarModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.wildcatter.tailings.client.BatteryLocomotiveModel.LAYER, dev.wildcatter.tailings.client.BatteryLocomotiveModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.wildcatter.tailings.client.BottomDumpCarModel.LAYER, dev.wildcatter.tailings.client.BottomDumpCarModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.wildcatter.tailings.client.HaulTruckModel.LAYER, dev.wildcatter.tailings.client.HaulTruckModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.wildcatter.tailings.client.WheelLoaderModel.LAYER, dev.wildcatter.tailings.client.WheelLoaderModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.wildcatter.tailings.client.BlastholeDrillModel.LAYER, dev.wildcatter.tailings.client.BlastholeDrillModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.wildcatter.tailings.client.EmulsionTruckModel.LAYER, dev.wildcatter.tailings.client.EmulsionTruckModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.wildcatter.tailings.client.TrackDozerModel.LAYER, dev.wildcatter.tailings.client.TrackDozerModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.wildcatter.tailings.client.UltraHaulTruckModel.LAYER, dev.wildcatter.tailings.client.UltraHaulTruckModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.wildcatter.tailings.client.RopeShovelModel.LAYER, dev.wildcatter.tailings.client.RopeShovelModel::createBodyLayer);
		ModelLayerRegistry.registerModelLayer(dev.wildcatter.tailings.client.HydraulicShovelModel.LAYER, dev.wildcatter.tailings.client.HydraulicShovelModel::createBodyLayer);
		EntityRendererRegistry.register(TailingsEntities.DRILL, NoopRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.TRAIN, NoopRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.LOCOMOTIVE, MineVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.CAR, MineVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.BATTERY_LOCOMOTIVE, MineVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.BOTTOM_DUMP_CAR, MineVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.TBM, dev.wildcatter.tailings.client.TbmRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.PUMP_GANTRY, dev.wildcatter.tailings.client.GantryRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.REEL_GANTRY, dev.wildcatter.tailings.client.GantryRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.CONVEYOR_GANTRY, dev.wildcatter.tailings.client.GantryRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.SKIP, dev.wildcatter.tailings.client.SkipRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.HAUL_TRUCK, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.WHEEL_LOADER, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.BLASTHOLE_DRILL, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.EMULSION_TRUCK, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.TRACK_DOZER, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.ULTRA_HAUL_TRUCK, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.ROPE_SHOVEL, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
		EntityRendererRegistry.register(TailingsEntities.HYDRAULIC_SHOVEL, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
		BlockEntityRenderers.register(TailingsBlocks.BELT, context -> new BeltRenderer());
		Tailings.LOGGER.info("Tailings client ready");
	}
}
