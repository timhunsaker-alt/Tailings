package dev.wildcatter.tailings.fabric;

import dev.wildcatter.Wildcatter;
import dev.wildcatter.tailings.Tailings;
import net.fabricmc.api.ModInitializer;

/** The Fabric entry point. Wildcatter is a hard dependency and a compile dependency. */
public final class TailingsFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		Tailings.register(Wildcatter.MOD_ID);
	}
}
