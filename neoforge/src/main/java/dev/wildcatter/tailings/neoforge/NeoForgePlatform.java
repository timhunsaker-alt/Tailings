package dev.wildcatter.tailings.neoforge;

import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.DrillMenu;
import dev.wildcatter.tailings.platform.Platform;
import java.nio.file.Path;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.extensions.IPlayerExtension;

public final class NeoForgePlatform implements Platform {
	@Override
	public String loader() {
		return "NeoForge";
	}

	@Override
	public Path configDir() {
		return FMLPaths.CONFIGDIR.get();
	}

	@Override
	public boolean isModLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}

	@Override
	public void openDrill(Player player, DrillBlockEntity drill) {
		if (!(player instanceof ServerPlayer server)) {
			return;
		}
		((IPlayerExtension) server).openMenu(new SimpleMenuProvider((id, inventory, opener) -> new DrillMenu(id, inventory, drill.getBlockPos()),
				Component.translatable("block.tailings.tunnel_drill")), drill.getBlockPos());
	}
}
