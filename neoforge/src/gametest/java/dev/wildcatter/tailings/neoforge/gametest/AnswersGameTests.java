package dev.wildcatter.tailings.neoforge.gametest;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.wildcatter.tailings.OptInRules;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.block.HandMillBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.rock.Rock;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.PermissionSet;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Server scenes for Tim's answers of 2026-11-10 (docs/questions/answers-2026-11-10.md) that have no wave of their own: the per-world
 * rules and their command (#5, #23), and ore blocks only as plant feed (#3). The other answers' scenes sit with their wave's:
 * {@code vent_new_world}, {@code paste_pace}, {@code dry_mill_all_hmc}, {@code dms_floats_belt}, {@code xrt_reverse} and
 * {@code gaps_press_cycle_bit}. This class also says which scenes run in a world from before the rules, and which turn rules on and
 * off ({@link RulesEnvironment}).
 */
final class AnswersGameTests {
	static final String[] SCENES = {"rules_new_world", "rules_existing_world", "rules_command", "raw_ore_refused"};
	static final Set<String> LONG = Set.of();
	/** Scenes of a world saved before the rules: their batch runs with every rule off. */
	static final Set<String> EXISTING_WORLD = Set.of("rules_existing_world", "vent_default_off", "dry_mill_ladder");
	/** Scenes that turn a rule off and on and set it back: their batch runs apart from every other scene. */
	static final Set<String> RULE_SCENES = Set.of("rules_command", "gaps_headframe_hopper", "gaps_paste_wall_breach", "gaps_paste_catch_up");

	private AnswersGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("rules_new_world", AnswersGameTests::newWorld);
		out.accept("rules_existing_world", AnswersGameTests::existingWorld);
		out.accept("rules_command", AnswersGameTests::command);
		out.accept("raw_ore_refused", AnswersGameTests::rawOre);
	}

	static int ticks(String scene, int otherwise) {
		return otherwise;
	}

	/**
	 * rules_new_world: the test server's world is made fresh, so its rules were made for a new world when the server started: every
	 * rule on but {@code paste_wall_breach}, which is off even in a new world.
	 */
	private static void newWorld(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		helper.assertTrue(OptInRules.newWorld(server), Component.literal("the test world is a new world"));
		Map<String, Boolean> rules = OptInRules.all(server);
		helper.assertTrue(rules.keySet().equals(OptInRules.NEW_WORLD.keySet()) && rules.size() == 6, Component.literal("six rules: " + rules));
		rules.forEach((key, value) -> helper.assertTrue(value == !key.equals(OptInRules.PASTE_WALL_BREACH),
				Component.literal(key + " should be " + !key.equals(OptInRules.PASTE_WALL_BREACH) + " in a new world: " + rules)));
		helper.succeed();
	}

	/**
	 * rules_existing_world: a world is new only while its game time is 0 and it holds no Tailings data, when its rules are first made;
	 * a world from before them (this scene's batch) has every rule off.
	 */
	private static void existingWorld(GameTestHelper helper) {
		helper.assertTrue(OptInRules.isNew(0, false), Component.literal("game time 0 and no Tailings data: new"));
		helper.assertTrue(!OptInRules.isNew(1, false) && !OptInRules.isNew(72_000, false), Component.literal("a world that has run a tick: saved before"));
		helper.assertTrue(!OptInRules.isNew(0, true), Component.literal("Tailings data already saved: saved before"));
		Map<String, Boolean> rules = OptInRules.all(helper.getLevel().getServer());
		helper.assertTrue(!rules.containsValue(true), Component.literal("a world from before the rules has them all off: " + rules));
		helper.succeed();
	}

	private static int run(CommandSourceStack source, String command) {
		try {
			return source.getServer().getCommands().getDispatcher().execute(command, source);
		} catch (CommandSyntaxException ex) {
			return -1;
		}
	}

	/**
	 * rules_command: {@code /tailings rule} lists the six; an operator sets one off and on again by command; a player with no permission
	 * may read a rule but not set it; an unknown rule is refused.
	 */
	private static void command(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		CommandSourceStack op = server.createCommandSourceStack().withSuppressedOutput();
		CommandSourceStack player = op.withPermission(PermissionSet.NO_PERMISSIONS);
		boolean was = OptInRules.on(server, OptInRules.HEADFRAME_HOPPER);
		helper.assertTrue(run(op, "tailings rule") == 6, Component.literal("the list names six rules"));
		helper.assertTrue(run(op, "tailings rule headframe_hopper false") == 1 && !OptInRules.on(server, OptInRules.HEADFRAME_HOPPER), Component.literal("an operator sets it off"));
		helper.assertTrue(run(player, "tailings rule headframe_hopper true") == -1 && !OptInRules.on(server, OptInRules.HEADFRAME_HOPPER),
				Component.literal("a player with no permission may not set it"));
		helper.assertTrue(run(player, "tailings rule headframe_hopper") == 0, Component.literal("but may read it: off"));
		helper.assertTrue(run(op, "tailings rule headframe_hopper true") == 1 && OptInRules.on(server, OptInRules.HEADFRAME_HOPPER), Component.literal("and on again"));
		helper.assertTrue(run(op, "tailings rule no_such_rule true") == 0, Component.literal("an unknown rule is refused"));
		OptInRules.set(server, OptInRules.HEADFRAME_HOPPER, was);
		helper.succeed();
	}

	/**
	 * raw_ore_refused (Tim, 2026-11-10, specs-tailings.md X-1): only ore blocks feed the plant. Raw iron, copper and gold and an ore's
	 * mined drop (Wildcatter's galena) are no feed: the jaw and the stamp mill refuse them; the ore blocks go in.
	 */
	private static void rawOre(GameTestHelper helper) {
		for (ItemStack raw : new ItemStack[] {new ItemStack(Items.RAW_IRON), new ItemStack(Items.RAW_COPPER), new ItemStack(Items.RAW_GOLD), Feeds.stack("wildcatter:galena", 1)}) {
			helper.assertTrue(!raw.isEmpty() && Feeds.parse(raw) == null, Component.literal(raw + " should be no plant feed"));
		}
		helper.assertTrue(Feeds.parse(new ItemStack(Items.IRON_ORE)).rock() == Rock.IRON && Feeds.parse(new ItemStack(Items.DEEPSLATE_GOLD_ORE)).rock() == Rock.GOLD
				&& Feeds.parse(Feeds.stack("wildcatter:galena_ore", 1)).rock() == Rock.GALENA, Component.literal("the ore blocks are feed"));
		PlantBlockEntity jaw = TailingsGameTests.plant(helper, Machine.JAW, new BlockPos(0, 2, 0));
		helper.assertTrue(!jaw.offer(new ItemStack(Items.RAW_IRON, 4)), Component.literal("the jaw refuses raw iron"));
		helper.assertTrue(jaw.offer(new ItemStack(Items.IRON_ORE)), Component.literal("and takes iron ore"));
		helper.setBlock(8, 0, 0, Blocks.STONE);
		helper.setBlock(8, 1, 0, TailingsBlocks.STAMP_MILL);
		HandMillBlockEntity mill = helper.getBlockEntity(new BlockPos(8, 1, 0), HandMillBlockEntity.class);
		helper.assertTrue(!mill.offer(Feeds.stack("wildcatter:galena", 1)) && !mill.offer(new ItemStack(Items.RAW_COPPER)), Component.literal("the stamp mill refuses mined drops"));
		helper.succeed();
	}
}
