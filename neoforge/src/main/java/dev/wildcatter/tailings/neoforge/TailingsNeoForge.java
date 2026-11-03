package dev.wildcatter.tailings.neoforge;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.tailings.Tailings;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/** The NeoForge entry point. Wildcatter is a hard dependency and a compile dependency. */
@Mod(Tailings.MOD_ID)
public final class TailingsNeoForge {
	public TailingsNeoForge(IEventBus modBus) {
		Tailings.register(Wildcatter.MOD_ID);
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			dev.wildcatter.tailings.neoforge.client.TailingsNeoForgeClient.init(modBus);
		}
	}
}
