package dev.wildcatter.tailings.platform;

import dev.wildcatter.tailings.block.DrillBlockEntity;
import java.nio.file.Path;
import java.util.ServiceLoader;
import net.minecraft.world.entity.player.Player;

/** What common code needs from the mod loader it runs on. Each loader's jar provides one, found by {@link ServiceLoader}. */
public interface Platform {
	Platform INSTANCE = ServiceLoader.load(Platform.class, Platform.class.getClassLoader()).findFirst()
			.orElseThrow(() -> new IllegalStateException("no Tailings platform: run the Fabric or NeoForge jar"));

	/** The loader's name, for logs. */
	String loader();

	/** Where config files go. */
	Path configDir();

	boolean isModLoaded(String modId);

	/** Opens the tunnel drill's cab. Each loader sends the block position to its own menu type. */
	void openDrill(Player player, DrillBlockEntity drill);
}
