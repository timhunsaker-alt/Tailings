package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.Tailings;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;

/**
 * The chunk ticket a running drill or train keeps (docs/multiblocks/tunnel-drill-haulage.md). It loads and ticks like a forced
 * chunk, is saved with the world, and times out {@link #TIMEOUT_TICKS} after its last refresh, so nothing ever removes it by
 * hand: a machine that stops or is gone simply stops refreshing it. Two machines in one chunk share it.
 */
public final class ChunkRunner {
	public static final long TIMEOUT_TICKS = 200;
	/** How often a running machine refreshes its chunks. */
	public static final int REFRESH_TICKS = 20;
	public static final TicketType RUNNER = new TicketType(TIMEOUT_TICKS,
			TicketType.FLAG_PERSIST | TicketType.FLAG_LOADING | TicketType.FLAG_SIMULATION | TicketType.FLAG_KEEP_DIMENSION_ACTIVE);

	private ChunkRunner() {
	}

	public static void register() {
		Registry.register(BuiltInRegistries.TICKET_TYPE, Tailings.id("runner"), RUNNER);
	}

	public static void refresh(ServerLevel level, BlockPos pos) {
		level.getChunkSource().addTicket(new Ticket(RUNNER, ChunkMap.FORCED_TICKET_LEVEL), ChunkPos.containing(pos));
	}
}
