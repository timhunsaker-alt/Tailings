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
		modBus.addListener(RegisterMenuScreensEvent.class, event -> event.register(DrillMenus.DRILL, DrillScreen::new));
		modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, event -> {
			event.registerLayerDefinition(MineLocomotiveModel.LAYER, MineLocomotiveModel::createBodyLayer);
			event.registerLayerDefinition(MineCarModel.LAYER, MineCarModel::createBodyLayer);
		});
		modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
			event.registerEntityRenderer(TailingsEntities.DRILL, NoopRenderer::new);
			event.registerEntityRenderer(TailingsEntities.TRAIN, NoopRenderer::new);
			event.registerEntityRenderer(TailingsEntities.LOCOMOTIVE, MineVehicleRenderer::new);
			event.registerEntityRenderer(TailingsEntities.CAR, MineVehicleRenderer::new);
			event.registerBlockEntityRenderer(dev.wildcatter.tailings.TailingsBlocks.BELT, context -> new dev.wildcatter.tailings.client.BeltRenderer());
		});
		modBus.addListener(FMLClientSetupEvent.class, event -> Tailings.LOGGER.info("Tailings client ready"));
	}
}
