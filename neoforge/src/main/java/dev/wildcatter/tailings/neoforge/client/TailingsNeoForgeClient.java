package dev.wildcatter.tailings.neoforge.client;

import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.DrillMenus;
import dev.wildcatter.tailings.client.DrillScreen;
import dev.wildcatter.tailings.client.MineCarModel;
import dev.wildcatter.tailings.client.MineLocomotiveModel;
import dev.wildcatter.tailings.client.MineVehicleRenderer;
import net.minecraft.client.renderer.entity.NoopRenderer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The NeoForge client side. */
public final class TailingsNeoForgeClient {
	private TailingsNeoForgeClient() {
	}

	public static void init(IEventBus modBus) {
		dev.wildcatter.tailings.entity.SoundingEntity.Hook.start = dev.wildcatter.tailings.client.EntitySound::start;
		modBus.addListener(RegisterMenuScreensEvent.class, event -> {
			event.register(DrillMenus.DRILL, DrillScreen::new);
			event.register(dev.wildcatter.tailings.block.PitPanelMenu.TYPE, dev.wildcatter.tailings.client.PitPanelScreen::new);
			event.register(DrillMenus.SORTER, dev.wildcatter.tailings.client.SorterScreen::new);
		});
		modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, event -> {
			event.registerLayerDefinition(MineLocomotiveModel.LAYER, MineLocomotiveModel::createBodyLayer);
			event.registerLayerDefinition(MineCarModel.LAYER, MineCarModel::createBodyLayer);
			event.registerLayerDefinition(dev.wildcatter.tailings.client.BatteryLocomotiveModel.LAYER, dev.wildcatter.tailings.client.BatteryLocomotiveModel::createBodyLayer);
			event.registerLayerDefinition(dev.wildcatter.tailings.client.BottomDumpCarModel.LAYER, dev.wildcatter.tailings.client.BottomDumpCarModel::createBodyLayer);
			event.registerLayerDefinition(dev.wildcatter.tailings.client.HaulTruckModel.LAYER, dev.wildcatter.tailings.client.HaulTruckModel::createBodyLayer);
			event.registerLayerDefinition(dev.wildcatter.tailings.client.WheelLoaderModel.LAYER, dev.wildcatter.tailings.client.WheelLoaderModel::createBodyLayer);
			event.registerLayerDefinition(dev.wildcatter.tailings.client.BlastholeDrillModel.LAYER, dev.wildcatter.tailings.client.BlastholeDrillModel::createBodyLayer);
			event.registerLayerDefinition(dev.wildcatter.tailings.client.EmulsionTruckModel.LAYER, dev.wildcatter.tailings.client.EmulsionTruckModel::createBodyLayer);
			event.registerLayerDefinition(dev.wildcatter.tailings.client.TrackDozerModel.LAYER, dev.wildcatter.tailings.client.TrackDozerModel::createBodyLayer);
			event.registerLayerDefinition(dev.wildcatter.tailings.client.UltraHaulTruckModel.LAYER, dev.wildcatter.tailings.client.UltraHaulTruckModel::createBodyLayer);
			event.registerLayerDefinition(dev.wildcatter.tailings.client.RopeShovelModel.LAYER, dev.wildcatter.tailings.client.RopeShovelModel::createBodyLayer);
			event.registerLayerDefinition(dev.wildcatter.tailings.client.DraglineModel.LAYER, dev.wildcatter.tailings.client.DraglineModel::createBodyLayer);
			event.registerLayerDefinition(dev.wildcatter.tailings.client.HydraulicShovelModel.LAYER, dev.wildcatter.tailings.client.HydraulicShovelModel::createBodyLayer);
		});
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
			event.registerEntityRenderer(TailingsEntities.DRILL, NoopRenderer::new);
			event.registerEntityRenderer(TailingsEntities.TRAIN, NoopRenderer::new);
			event.registerEntityRenderer(TailingsEntities.LOCOMOTIVE, MineVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.CAR, MineVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.BATTERY_LOCOMOTIVE, MineVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.BOTTOM_DUMP_CAR, MineVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.TBM, dev.wildcatter.tailings.client.TbmRenderer::new);
			event.registerEntityRenderer(TailingsEntities.PUMP_GANTRY, dev.wildcatter.tailings.client.GantryRenderer::new);
			event.registerEntityRenderer(TailingsEntities.REEL_GANTRY, dev.wildcatter.tailings.client.GantryRenderer::new);
			event.registerEntityRenderer(TailingsEntities.CONVEYOR_GANTRY, dev.wildcatter.tailings.client.GantryRenderer::new);
			event.registerEntityRenderer(TailingsEntities.SKIP, dev.wildcatter.tailings.client.SkipRenderer::new);
			event.registerEntityRenderer(TailingsEntities.HAUL_TRUCK, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.WHEEL_LOADER, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.BLASTHOLE_DRILL, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.EMULSION_TRUCK, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.TRACK_DOZER, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.ULTRA_HAUL_TRUCK, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.ROPE_SHOVEL, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.DRAGLINE, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.HYDRAULIC_SHOVEL, dev.wildcatter.tailings.client.PitVehicleRenderer::new);
			event.registerBlockEntityRenderer(dev.wildcatter.tailings.TailingsBlocks.BELT, context -> new dev.wildcatter.tailings.client.BeltRenderer());
		});
		modBus.addListener(FMLClientSetupEvent.class, event -> Tailings.LOGGER.info("Tailings client ready"));
	}
}
