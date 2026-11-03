package dev.wildcatter.tailings.fabric;

import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.DrillMenu;
import dev.wildcatter.tailings.platform.Platform;
import java.nio.file.Path;
import net.fabricmc.fabric.api.menu.v1.ExtendedMenuProvider;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class FabricPlatform implements Platform {
	@Override
	public String loader() {
		return "Fabric";
	}

	@Override
	public Path configDir() {
		return FabricLoader.getInstance().getConfigDir();
	}

	@Override
	public boolean isModLoaded(String modId) {
		return FabricLoader.getInstance().isModLoaded(modId);
	}

	@Override
	public void openDrill(Player player, DrillBlockEntity drill) {
		if (!(player instanceof ServerPlayer)) {
			return;
		}
		player.openMenu(new ExtendedMenuProvider<BlockPos>() {
			@Override
			public BlockPos getScreenOpeningData(ServerPlayer server) {
				return drill.getBlockPos();
			}

			@Override
			public Component getDisplayName() {
				return Component.translatable("block.tailings.tunnel_drill");
			}

			@Override
			public AbstractContainerMenu createMenu(int id, Inventory inventory, Player opener) {
				return new DrillMenu(id, inventory, drill.getBlockPos());
			}
		});
	}
}
