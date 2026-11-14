package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.block.ZirconFeed;
import dev.wildcatter.quest.QuestBook;
import dev.wildcatter.quest.QuestData;
import dev.wildcatter.quest.QuestRules;
import dev.wildcatter.quest.QuestTracker;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.BlastholeBlockEntity;
import dev.wildcatter.tailings.block.BlastingMachineBlockEntity;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.entity.DraglineEntity;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.quest.Milestones;
import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Tailings' chapter of Wildcatter's quest book (data/tailings/wildcatter_quests/open_pit.json; Wildcatter docs/quests.md):
 * <ul>
 * <li>{@code quests_valid}: every chapter the server has, Tailings' with Wildcatter's, passes Wildcatter's check, and Tailings' chapter and
 * its quests are there;</li>
 * <li>{@code quest_crusher_milestone}, {@code quest_flotation_milestone}, {@code quest_blast_milestone}, {@code quest_dragline_milestone}:
 * each machine, working, gives its milestone to a player standing near it;</li>
 * <li>{@code quest_zircon_feed}: Wildcatter's chlorinator takes Tailings' zircon concentrate as 56.25 kg of zircon each.</li>
 * </ul>
 */
final class QuestGameTests {
	static final String[] SCENES = {"quests_valid", "quest_crusher_milestone", "quest_flotation_milestone", "quest_blast_milestone",
			"quest_dragline_milestone", "quest_zircon_feed"};
	/** Scenes over a wide area: their chunks are loaded by {@link #reach}. */
	static final Set<String> WIDE = Set.of("quest_blast_milestone", "quest_dragline_milestone");
	private static final Map<String, Integer> TICKS = Map.of("quest_blast_milestone", 1200, "quest_dragline_milestone", 1200);
	private static final Map<String, Integer> PADDING = Map.of("quest_blast_milestone", 100, "quest_dragline_milestone", 160);

	private QuestGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("quests_valid", QuestGameTests::questsValid);
		out.accept("quest_crusher_milestone", QuestGameTests::crusher);
		out.accept("quest_flotation_milestone", QuestGameTests::flotation);
		out.accept("quest_blast_milestone", QuestGameTests::blast);
		out.accept("quest_dragline_milestone", QuestGameTests::dragline);
		out.accept("quest_zircon_feed", QuestGameTests::zirconFeed);
	}

	static int ticks(String scene, int otherwise) {
		return TICKS.getOrDefault(scene, otherwise);
	}

	static int padding(String scene, int otherwise) {
		return PADDING.getOrDefault(scene, otherwise);
	}

	static int[] reach(String scene) {
		return switch (scene) {
			case "quest_blast_milestone" -> new int[] {-1, 4, -1, 6};
			case "quest_dragline_milestone" -> new int[] {-1, 6, -1, 5};
			default -> new int[] {-1, 2, -1, 2};
		};
	}

	// ---- helpers -----------------------------------------------------------------------------------------------------------------

	private static void check(GameTestHelper helper, boolean ok, String why) {
		helper.assertTrue(ok, Component.literal(why));
	}

	private static void log(String scene, String what) {
		System.out.printf(Locale.ROOT, "[tailings-test] %s: %s%n", scene, what);
	}

	/** A stand-in player at {@code (x, y, z)} of the test, its inventory empty. */
	private static ServerPlayer player(GameTestHelper helper, int x, int y, int z) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos at = helper.absolutePos(new BlockPos(x, y, z));
		player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		player.getInventory().clearContent();
		return player;
	}

	private static void leave(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	private static boolean reached(GameTestHelper helper, ServerPlayer player, Identifier key) {
		return QuestData.get(helper.getLevel().getServer()).of(player.getStringUUID()).milestones.contains(key.toString());
	}

	// ---- scenes ------------------------------------------------------------------------------------------------------------------

	/** Every chapter passes Wildcatter's check with Tailings loaded; Tailings' chapter has its quests and its four milestones. */
	private static void questsValid(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		List<String> problems = QuestRules.problems(server);
		check(helper, problems.isEmpty(), "every chapter should pass the check: " + problems);
		QuestBook book = QuestTracker.book(server);
		QuestBook.Chapter chapter = book.chapter(Identifier.parse("tailings:open_pit"));
		check(helper, chapter != null, "Tailings' chapter tailings:open_pit should load; chapters: " + book.chapters().stream().map(QuestBook.Chapter::id).toList());
		check(helper, chapter.order() >= 100, "an add-on chapter is ordered 100 and up: " + chapter.order());
		for (String quest : new String[] {"stamp_mill", "crusher_line", "flotation", "leach", "zircon", "ferrosilicon", "gyratory", "cabled_pit", "blasting",
				"rope_shovel", "dragline"}) {
			check(helper, book.quest(Identifier.fromNamespaceAndPath("tailings", quest)) != null, "the quest tailings:" + quest + " should load");
		}
		java.util.Set<String> keys = new java.util.HashSet<>();
		for (QuestBook.Quest q : chapter.quests()) {
			for (QuestBook.Task t : q.tasks()) {
				if (t.kind() == QuestBook.Kind.MILESTONE) {
					keys.add(t.target());
				}
			}
		}
		Set<String> fired = Set.of(Milestones.CRUSHER_RUNNING.toString(), Milestones.FLOTATION_RUNNING.toString(), Milestones.BLAST_FIRED.toString(),
				Milestones.DRAGLINE_RUNNING.toString());
		check(helper, keys.equals(fired), "the chapter's milestones should be the four Tailings fires: " + keys);
		log("quests_valid", "ok: " + book.chapters().size() + " chapters, " + book.quests().size() + " quests pass; Tailings' chapter has "
				+ chapter.quests().size() + " quests");
		helper.succeed();
	}

	/** A jaw crusher on 480 V crushing copper ore onto its belt gives a player 6 m off {@code tailings:crusher_running}. */
	private static void crusher(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity jaw = TailingsGameTests.plant(helper, Machine.JAW, new BlockPos(0, y, 0));
		for (int x = 0; x <= 3; x++) {
			helper.setBlock(new BlockPos(x, y, 4), TailingsBlocks.BELT_CONVEYOR.defaultBlockState().setValue(BeltConveyorBlock.AXIS, Direction.Axis.X));
		}
		PlantBlockEntity drive = TailingsGameTests.plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(4, y, 4));
		ServerPlayer player = player(helper, 2, y, 10);
		check(helper, !reached(helper, player, Milestones.CRUSHER_RUNNING), "nothing is reached before the jaw runs");
		jaw.forceRunning();
		drive.forceRunning();
		check(helper, jaw.offer(new ItemStack(Blocks.COPPER_ORE)), "the jaw should take copper ore");
		helper.succeedWhen(() -> {
			check(helper, reached(helper, player, Milestones.CRUSHER_RUNNING), "the crushing jaw should give crusher_running (jaw " + jaw.status() + ")");
			log("quest_crusher_milestone", "ok: " + jaw.status());
			leave(helper, player);
		});
	}

	/** A flotation bank floating conditioned slurry, its blower running, gives a player beside it {@code tailings:flotation_running}. */
	private static void flotation(GameTestHelper helper) {
		int y = 4;
		double copper = Rock.COPPER.contained(Mineral.COPPER).doubleValue() * Mineral.INGOT_KG;
		TailingsGameTests.fines(helper, new BlockPos(0, y, 0), 1200, Map.of("minecraft:copper_ingot", copper / 1200));
		TailingsGameTests.pipe(helper, new BlockPos(0, y, 2), new BlockPos(1, y, 2));
		PlantBlockEntity tank = TailingsGameTests.plant(helper, Machine.CONDITIONER, new BlockPos(0, y, 3));
		TailingsGameTests.pipe(helper, new BlockPos(0, y, 6));
		PlantBlockEntity bank = TailingsGameTests.plant(helper, Machine.FLOTATION_BANK, new BlockPos(0, y, 7));
		PlantBlockEntity blower = TailingsGameTests.plant(helper, Machine.BLOWER, new BlockPos(7, y, 7));
		ServerPlayer player = player(helper, 10, y, 14);
		for (PlantBlockEntity part : new PlantBlockEntity[] {tank, bank, blower}) {
			part.forceRunning();
		}
		TailingsGameTests.reagents(helper, tank, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC);
		helper.succeedWhen(() -> {
			check(helper, reached(helper, player, Milestones.FLOTATION_RUNNING), "the floating bank should give flotation_running (tank " + tank.status()
					+ "; bank " + bank.status() + ")");
			log("quest_flotation_milestone", "ok: " + bank.status());
			leave(helper, player);
		});
	}

	/** One charged, stemmed hole fired from the blasting machine gives a player at the firing point {@code tailings:blast_fired}. */
	private static void blast(GameTestHelper helper) {
		PitBlastGameTests.fill(helper, Blocks.STONE, 0, 40, 40, 59, 0, 40);
		BlastholeBlockEntity hole = PitBlastGameTests.hole(helper, 20, 59, 20, 16, 0, 0, Direction.NORTH);
		PitBlastGameTests.load(helper, hole, true, true, 828, 4);
		BlastingMachineBlockEntity machine = PitBlastGameTests.machine(helper, 20, 60, 80);
		helper.setBlock(new BlockPos(22, 59, 82), Blocks.STONE);
		ServerPlayer player = player(helper, 22, 60, 82);
		machine.load(PitBlastGameTests.logged(helper, List.of(hole)));
		machine.setKey(true);
		String armed = machine.arm();
		check(helper, armed.startsWith("ARMED"), "the machine should arm: " + armed);
		String fired = machine.fire();
		if (fired.startsWith("PLAYER IN DANGER ZONE")) {
			fired = machine.fire();
		}
		check(helper, fired.startsWith("SIREN"), "FIRE should sound the siren: " + fired);
		check(helper, !reached(helper, player, Milestones.BLAST_FIRED), "nothing is reached before the shot");
		machine.sirenNow();
		helper.startSequence()
				.thenWaitUntil(() -> check(helper, !machine.firing() && machine.lastShot() != null, "the shot should fire: " + machine.status()))
				.thenExecute(() -> {
					check(helper, reached(helper, player, Milestones.BLAST_FIRED), "the fired shot should give blast_fired (" + machine.status() + ")");
					log("quest_blast_milestone", "ok: " + machine.status());
					leave(helper, player);
				})
				.thenSucceed();
	}

	/** The walking dragline digging its strip on its trailing cable gives a player on the bench {@code tailings:dragline_running}. */
	private static void dragline(GameTestHelper helper) {
		DraglineGameTests.bench(helper, 90, 70);
		DraglineGameTests.strip(helper, 30, 10, 5, Blocks.SAND, 2);
		PitSwitchHouseBlockEntity house = DraglineGameTests.house(helper);
		DraglineEntity d = DraglineGameTests.dragline(helper, house, 30, 10, 5, 4);
		ServerPlayer player = player(helper, 10, DraglineGameTests.BENCH, 50);
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					check(helper, !reached(helper, player, Milestones.DRAGLINE_RUNNING), "nothing is reached before its outlet closes: " + d.status());
					check(helper, house.close(0) == null, "its outlet should close");
				})
				.thenWaitUntil(() -> check(helper, reached(helper, player, Milestones.DRAGLINE_RUNNING), "the digging dragline should give dragline_running: " + d.status()))
				.thenExecute(() -> {
					log("quest_dragline_milestone", "ok: " + d.status());
					d.stop(helper.getLevel(), false);
					leave(helper, player);
				})
				.thenSucceed();
	}

	/** Tailings' zircon concentrate is in {@code #wildcatter:zircon_feed} at 56.25 kg of zircon an item; a zircon nugget is not. */
	private static void zirconFeed(GameTestHelper helper) {
		MinecraftServer server = helper.getLevel().getServer();
		ItemStack concentrate = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("tailings:zircon_concentrate")));
		ItemStack nugget = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("tailings:zircon_nugget")));
		check(helper, ZirconFeed.is(concentrate), "zircon concentrate should be zircon feed");
		double kg = ZirconFeed.kg(server, concentrate);
		check(helper, Math.abs(kg - 56.25) < 1e-9, "an item of zircon concentrate should count 56.25 kg of zircon, got " + kg);
		check(helper, !ZirconFeed.is(nugget), "a zircon nugget is not chlorinator feed (nine craft into zircon)");
		log("quest_zircon_feed", "ok: " + kg + " kg");
		helper.succeed();
	}
}
