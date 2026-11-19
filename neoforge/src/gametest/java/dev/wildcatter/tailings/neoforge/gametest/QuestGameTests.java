package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.block.StorageTankBlockEntity;
import dev.wildcatter.block.ZirconFeed;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.quest.QuestBook;
import dev.wildcatter.quest.QuestData;
import dev.wildcatter.quest.QuestRules;
import dev.wildcatter.quest.QuestTracker;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.BlastholeBlockEntity;
import dev.wildcatter.tailings.block.BlastingMachineBlockEntity;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.RopeShovelBlockEntity;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.entity.BlastholeDrillEntity;
import dev.wildcatter.tailings.entity.DraglineEntity;
import dev.wildcatter.tailings.entity.RopeShovelEntity;
import dev.wildcatter.tailings.item.TrailingCableItem;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.quest.Milestones;
import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
 * <li>{@code quest_crusher_milestone}, {@code quest_flotation_milestone}, {@code quest_blast_milestone}, {@code quest_dragline_milestone},
 * {@code quest_gyratory_milestone}, {@code quest_shovel_milestone}, {@code quest_leach_milestone}, {@code quest_pit_energised_milestone}:
 * each machine, working, gives its milestone to a player standing near it;</li>
 * <li>{@code quest_zircon_feed}: Wildcatter's chlorinator takes Tailings' zircon concentrate as 56.25 kg of zircon each.</li>
 * </ul>
 */
final class QuestGameTests {
	static final String[] SCENES = {"quests_valid", "quest_crusher_milestone", "quest_flotation_milestone", "quest_blast_milestone",
			"quest_dragline_milestone", "quest_zircon_feed", "quest_gyratory_milestone", "quest_shovel_milestone", "quest_leach_milestone",
			"quest_pit_energised_milestone", "quest_stamp_mill_milestone"};
	/** Scenes over a wide area: their chunks are loaded by {@link #reach}. */
	static final Set<String> WIDE = Set.of("quest_blast_milestone", "quest_dragline_milestone", "quest_gyratory_milestone", "quest_shovel_milestone",
			"quest_pit_energised_milestone");
	private static final Map<String, Integer> TICKS = Map.of("quest_blast_milestone", 1200, "quest_dragline_milestone", 1200, "quest_gyratory_milestone", 800,
			"quest_shovel_milestone", 1600, "quest_pit_energised_milestone", 1200);
	private static final Map<String, Integer> PADDING = Map.of("quest_blast_milestone", 100, "quest_dragline_milestone", 160, "quest_gyratory_milestone", 50,
			"quest_shovel_milestone", 120, "quest_pit_energised_milestone", 50);

	private QuestGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("quests_valid", QuestGameTests::questsValid);
		out.accept("quest_crusher_milestone", QuestGameTests::crusher);
		out.accept("quest_flotation_milestone", QuestGameTests::flotation);
		out.accept("quest_blast_milestone", QuestGameTests::blast);
		out.accept("quest_dragline_milestone", QuestGameTests::dragline);
		out.accept("quest_zircon_feed", QuestGameTests::zirconFeed);
		out.accept("quest_gyratory_milestone", QuestGameTests::gyratory);
		out.accept("quest_shovel_milestone", QuestGameTests::shovel);
		out.accept("quest_leach_milestone", QuestGameTests::leach);
		out.accept("quest_pit_energised_milestone", QuestGameTests::pitEnergised);
		out.accept("quest_stamp_mill_milestone", QuestGameTests::stampMill);
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
			case "quest_gyratory_milestone" -> new int[] {-1, 3, -1, 3};
			case "quest_shovel_milestone" -> new int[] {-1, 5, -1, 6};
			case "quest_pit_energised_milestone" -> new int[] {-1, 4, -1, 4};
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
	static ServerPlayer player(GameTestHelper helper, int x, int y, int z) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		BlockPos at = helper.absolutePos(new BlockPos(x, y, z));
		player.teleportTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
		player.getInventory().clearContent();
		return player;
	}

	static void leave(GameTestHelper helper, ServerPlayer player) {
		helper.getLevel().getServer().getPlayerList().remove(player);
	}

	static boolean reached(GameTestHelper helper, ServerPlayer player, Identifier key) {
		return QuestData.get(helper.getLevel().getServer()).of(player.getStringUUID()).milestones.contains(key.toString());
	}

	// ---- scenes ------------------------------------------------------------------------------------------------------------------

	/** Every chapter passes Wildcatter's check with Tailings loaded; Tailings' chapter has its quests and its eleven milestones. */
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
				Milestones.DRAGLINE_RUNNING.toString(), Milestones.GYRATORY_RUNNING.toString(), Milestones.SHOVEL_RUNNING.toString(),
				Milestones.LEACH_RUNNING.toString(), Milestones.PIT_ENERGISED.toString(), Milestones.STAMP_MILL_RUNNING.toString(),
				Milestones.DRY_MILL_RUNNING.toString(), Milestones.DMS_RUNNING.toString());
		check(helper, keys.equals(fired), "the chapter's milestones should be the eleven Tailings fires: " + keys);
		log("quests_valid", "ok: " + book.chapters().size() + " chapters, " + book.quests().size() + " quests pass; Tailings' chapter has "
				+ chapter.quests().size() + " quests");
		helper.succeed();
	}

	/** The stamp mill crushing a copper ore block gives a player beside it {@code tailings:stamp_mill_running} (2026-11-19). */
	private static void stampMill(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 2, 2);
		helper.setBlock(at, TailingsBlocks.STAMP_MILL);
		ServerPlayer player = player(helper, 4, 2, 4);
		check(helper, !reached(helper, player, Milestones.STAMP_MILL_RUNNING), "nothing is reached before it stamps");
		var mill = helper.getBlockEntity(at, dev.wildcatter.tailings.block.HandMillBlockEntity.class);
		check(helper, mill.offer(new ItemStack(Blocks.COPPER_ORE)), "the stamp mill should take copper ore");
		helper.succeedWhen(() -> {
			check(helper, reached(helper, player, Milestones.STAMP_MILL_RUNNING), "the stamping mill should give stamp_mill_running");
			leave(helper, player);
		});
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
		double copper = Rock.COPPER.contained(Mineral.COPPER).doubleValue() * Mineral.COPPER.kgPerUnit();
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

	/** The gyratory crushing granite from its dump pocket into the surge pocket gives a player on the ground {@code tailings:gyratory_running}. */
	private static void gyratory(GameTestHelper helper) {
		PitLineGameTests.Primary p = PitLineGameTests.primary(helper);
		ServerPlayer player = player(helper, 20, 2, 20);
		check(helper, p.pocket().offer(Map.of("minecraft:granite", 4)), "the pocket should take four bank blocks of granite");
		check(helper, !reached(helper, player, Milestones.GYRATORY_RUNNING), "nothing is reached before the gyratory runs");
		p.crusher().forceRunning();
		helper.succeedWhen(() -> {
			check(helper, reached(helper, player, Milestones.GYRATORY_RUNNING), "the crushing gyratory should give gyratory_running (" + p.crusher().status() + ")");
			log("quest_gyratory_milestone", "ok: " + p.crusher().status());
			leave(helper, player);
		});
	}

	/** The rope shovel, lifted on its trailing cable, digging the muck face in AUTO gives a player on the floor {@code tailings:shovel_running}. */
	private static void shovel(GameTestHelper helper) {
		PitHaulGameTests.floor(helper, 0, 75, 1, 1, 0, 90);
		BlockPos at = new BlockPos(20, 2, 40);
		RopeShovelBlockEntity crawlers = PitBigGameTests.ropeShovel(helper, at, null);
		PitSwitchHouseBlockEntity house = PitBigGameTests.house(helper, new BlockPos(62, 2, 75));
		ItemStack reel = new ItemStack(TailingsItems.TRAILING_CABLE);
		TrailingCableItem.pickUp(reel, house);
		long[] from = TrailingCableItem.from(reel);
		String why = crawlers.couple(helper.getLevel(), BlockPos.of(from[0]), (int) from[1]);
		check(helper, why == null, "the crawlers should take the cable: " + why);
		UUID name = crawlers.machine();
		ServerPlayer player = player(helper, 45, 2, 60);
		RopeShovelEntity[] shovel = new RopeShovelEntity[1];
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					check(helper, house.close(0) == null, "its outlet should close on the standing machine's cable");
					String said = crawlers.run(helper.getLevel());
					check(helper, said.startsWith("lifted"), "RUN should lift it: " + said);
					check(helper, helper.getLevel().getEntity(name) instanceof RopeShovelEntity, "one machine, by the crawlers' name");
					shovel[0] = (RopeShovelEntity) helper.getLevel().getEntity(name);
					check(helper, !reached(helper, player, Milestones.SHOVEL_RUNNING), "nothing is reached before it digs: " + shovel[0].status());
					PitBigGameTests.muck(helper, 22, 31, 2, 14, 35, 38);
					shovel[0].fastPasses(4);
					shovel[0].setAutoHere();
				})
				.thenWaitUntil(() -> check(helper, reached(helper, player, Milestones.SHOVEL_RUNNING), "the digging shovel should give shovel_running: " + shovel[0].status()))
				.thenExecute(() -> {
					log("quest_shovel_milestone", "ok: " + shovel[0].status());
					shovel[0].setAuto(helper.getLevel(), List.of());
					shovel[0].discard();
					leave(helper, player);
				})
				.thenSucceed();
	}

	/** A leach tank taking flotation tails and sulfuric acid from a stock tank gives a player beside it {@code tailings:leach_running}. */
	private static void leach(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity source = TailingsGameTests.plant(helper, Machine.FLOTATION_BANK, new BlockPos(0, y, 0));
		source.out().offer(Product.TAILS_SLURRY, 200, SlurryAssay.of(Map.of("minecraft:copper_ingot", Mineral.COPPER.kgPerUnit() / 200)));
		TailingsGameTests.pipe(helper, new BlockPos(0, y, 13));
		PlantBlockEntity leach = TailingsGameTests.plant(helper, Machine.LEACH_TANK, new BlockPos(0, y, 14));
		var acid = TailingsGameTests.part(helper, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "stock_tank")), new BlockPos(6, y, 15),
				StorageTankBlockEntity.class);
		acid.testFill(Product.SULFURIC_ACID, 5000);
		TailingsGameTests.pipe(helper, new BlockPos(5, y, 16));
		ServerPlayer player = player(helper, 10, y, 20);
		check(helper, !reached(helper, player, Milestones.LEACH_RUNNING), "nothing is reached before the tank leaches");
		leach.forceRunning();
		helper.succeedWhen(() -> {
			check(helper, leach.status().contains("leaching") && reached(helper, player, Milestones.LEACH_RUNNING), "the leaching tank should give leach_running ("
					+ leach.status() + ")");
			log("quest_leach_milestone", "ok: " + leach.status());
			leave(helper, player);
		});
	}

	/**
	 * A switch house with an electric drill on outlet 1's trailing cable gives a player near it {@code tailings:pit_energised} once the outlet
	 * closes and the cable is live, not before.
	 */
	private static void pitEnergised(GameTestHelper helper) {
		PitHaulGameTests.floor(helper, 0, 40, 1, 1, 0, 40);
		PitSwitchHouseBlockEntity house = PitBigGameTests.house(helper, new BlockPos(4, 2, 4));
		BlastholeDrillEntity drill = PitBigGameTests.electricDrill(helper, 20.5, 2, 26.5, 0);
		String why = PitBigGameTests.couple(helper, house, drill);
		check(helper, why == null, "the drill should couple: " + why);
		ServerPlayer player = player(helper, 12, 2, 20);
		helper.startSequence()
				.thenIdle(40)
				.thenExecute(() -> {
					check(helper, !reached(helper, player, Milestones.PIT_ENERGISED), "a coupled cable on an open outlet is not energised: " + house.state(0));
					check(helper, house.close(0) == null, "outlet 1 should close on the drill's cable");
				})
				.thenWaitUntil(() -> check(helper, house.live(0) && reached(helper, player, Milestones.PIT_ENERGISED), "the live cable should give pit_energised: "
						+ drill.powerInfo()))
				.thenExecute(() -> {
					log("quest_pit_energised_milestone", "ok: outlet 1 " + house.state(0) + ", " + Math.round(house.volts(0)) + " V at the drill");
					drill.discard();
					leave(helper, player);
				})
				.thenSucceed();
	}
}
