package dev.wildcatter.tailings.neoforge.gametest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import dev.wildcatter.tailings.OptInRules;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;

/**
 * A test environment that sets Tailings' world rules ({@link OptInRules}) for its batch and puts them back after: the scenes of a world
 * saved before the rules run with them off. Scenes in one batch run side by side, so a rule a scene depends on is set for the whole
 * batch, never in the middle of a scene other scenes share.
 */
record RulesEnvironment(Map<String, Boolean> rules) implements TestEnvironmentDefinition<Map<String, Boolean>> {
	static final MapCodec<RulesEnvironment> CODEC = Codec.unboundedMap(Codec.STRING, Codec.BOOL).fieldOf("rules").xmap(RulesEnvironment::new, RulesEnvironment::rules);

	/** Every rule off: a world saved before them. */
	static RulesEnvironment existingWorld() {
		Map<String, Boolean> off = new LinkedHashMap<>();
		OptInRules.NEW_WORLD.keySet().forEach(key -> off.put(key, false));
		return new RulesEnvironment(off);
	}

	@Override
	public Map<String, Boolean> setup(ServerLevel level) {
		Map<String, Boolean> before = OptInRules.all(level.getServer());
		rules.forEach((key, value) -> OptInRules.set(level.getServer(), key, value));
		return before;
	}

	@Override
	public void teardown(ServerLevel level, Map<String, Boolean> before) {
		before.forEach((key, value) -> OptInRules.set(level.getServer(), key, value));
	}

	@Override
	public MapCodec<? extends TestEnvironmentDefinition<Map<String, Boolean>>> codec() {
		return CODEC;
	}
}
