package dev.wildcatter.tailings;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

/**
 * {@code /tailings rule}: lists the world's rules ({@link OptInRules}); {@code /tailings rule <name>} reads one; {@code /tailings rule
 * <name> <true|false>} sets one, saved with the world (an operator's command, as {@code /gamerule} is).
 */
public final class TailingsCommands {
	private TailingsCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(Tailings.MOD_ID).then(Commands.literal("rule")
				.executes(c -> list(c.getSource()))
				.then(Commands.argument("name", StringArgumentType.word())
						.suggests((c, builder) -> SharedSuggestionProvider.suggest(OptInRules.NEW_WORLD.keySet(), builder))
						.executes(c -> show(c.getSource(), StringArgumentType.getString(c, "name")))
						.then(Commands.argument("value", BoolArgumentType.bool())
								.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
								.executes(c -> set(c.getSource(), StringArgumentType.getString(c, "name"), BoolArgumentType.getBool(c, "value")))))));
	}

	private static int list(CommandSourceStack source) {
		MinecraftServer server = source.getServer();
		Map<String, Boolean> rules = OptInRules.all(server);
		source.sendSuccess(() -> Component.literal("Tailings rules (" + (OptInRules.newWorld(server) ? "a new world" : "a world from before them") + "):"), false);
		rules.forEach((key, value) -> source.sendSuccess(() -> Component.literal("  " + key + " = " + value + ": " + OptInRules.meaning(key)), false));
		return rules.size();
	}

	private static int show(CommandSourceStack source, String key) {
		if (!OptInRules.NEW_WORLD.containsKey(key)) {
			return unknown(source, key);
		}
		boolean value = OptInRules.on(source.getServer(), key);
		source.sendSuccess(() -> Component.literal(key + " = " + value + ": " + OptInRules.meaning(key)), false);
		return value ? 1 : 0;
	}

	private static int set(CommandSourceStack source, String key, boolean value) {
		if (!OptInRules.NEW_WORLD.containsKey(key)) {
			return unknown(source, key);
		}
		OptInRules.set(source.getServer(), key, value);
		source.sendSuccess(() -> Component.literal("Tailings rule " + key + " is now " + value), true);
		return 1;
	}

	private static int unknown(CommandSourceStack source, String key) {
		source.sendFailure(Component.literal("No Tailings rule " + key + "; the rules are " + String.join(", ", OptInRules.NEW_WORLD.keySet())));
		return 0;
	}
}
