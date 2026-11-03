package dev.wildcatter.tailings.platform;

import dev.wildcatter.tailings.block.DrillBlockEntity;
import java.nio.file.Path;
import java.util.ServiceLoader;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
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

	/**
	 * Ticket-based chunk load. NeoForge files a {@code TicketController} ticket owned by {@code owner}. Fabric uses
	 * the level's force-load ticket, which is the same idea without a mod ticket controller.
	 */
	void forceChunk(ServerLevel level, UUID owner, int chunkX, int chunkZ, boolean add);
}
