package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.tailings.OptInRules;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.block.SurveyStationBlockEntity;
import dev.wildcatter.tailings.entity.DraglineEntity;
import dev.wildcatter.tailings.mine.Slope;
import dev.wildcatter.tailings.pit.Dragline;
import dev.wildcatter.tailings.pit.DraglineRopes;
import dev.wildcatter.tailings.pit.Slopes;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/**
 * The dragline by hand, and its highwall and spoil failures (docs/multiblocks/dragline-hand-control.md, Wave B).
 *
 * <p>The hand scenes stand the lifted dragline as {@link DraglineGameTests} does (its tub's floor at y 10, facing south, +z), and put
 * its bucket ahead of it, 45 m out over a bed of sand. The slope scenes build a highwall by hand along x 32, a strip registered in the
 * slope record from a dragline centre at x 20.5, z 30.5 facing +z with its strip to +x (as the dragline scenes' strip), and run with
 * the world rule {@code slope_failures} on (their own batch, {@link #RULE_ON}).
 */
final class DraglineHandGameTests {
	static final String[] HAND = {"dragline_hand_dig", "dragline_hand_limits", "dragline_hand_give_back", "dragline_hand_plc_stop", "slope_rule_off"};
	static final String[] SLOPE = {"dragline_batter", "slope_highwall_creep", "slope_highwall_slide", "slope_water", "slope_blast", "slope_buttress", "slope_spoil",
			"slope_dragline_wrecked", "slope_dragline_ground_gone", "slope_station_alarm", "slope_saved"};
	static final String[] SCENES = java.util.stream.Stream.of(HAND, SLOPE).flatMap(java.util.Arrays::stream).toArray(String[]::new);
	/** The slope scenes run in a batch with slope_failures on. */
	static final Set<String> RULE_ON = Set.of(SLOPE);
	static final Map<String, Integer> TICKS = Map.of("slope_highwall_slide", 3600, "slope_spoil", 4800, "slope_dragline_wrecked", 1200, "slope_dragline_ground_gone", 1200,
			"dragline_batter", 4000, "dragline_hand_dig", 1600, "dragline_hand_give_back", 2400, "dragline_hand_limits", 1600);

	private static final double CX = 20.5, CZ = 30.5;

	private DraglineHandGameTests() {
	}

	static void register(java.util.function.BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("dragline_hand_dig", DraglineHandGameTests::handDig);
		out.accept("dragline_hand_limits", DraglineHandGameTests::handLimits);
		out.accept("dragline_hand_give_back", DraglineHandGameTests::handGiveBack);
		out.accept("dragline_hand_plc_stop", DraglineHandGameTests::handPlcStop);
		out.accept("slope_rule_off", DraglineHandGameTests::ruleOff);
		out.accept("dragline_batter", DraglineHandGameTests::batter);
		out.accept("slope_highwall_creep", DraglineHandGameTests::creep);
		out.accept("slope_highwall_slide", DraglineHandGameTests::slide);
		out.accept("slope_water", DraglineHandGameTests::water);
		out.accept("slope_blast", DraglineHandGameTests::blast);
		out.accept("slope_buttress", DraglineHandGameTests::buttress);
		out.accept("slope_spoil", DraglineHandGameTests::spoil);
		out.accept("slope_dragline_wrecked", DraglineHandGameTests::wrecked);
		out.accept("slope_dragline_ground_gone", DraglineHandGameTests::groundGone);
		out.accept("slope_station_alarm", DraglineHandGameTests::stationAlarm);
		out.accept("slope_saved", DraglineHandGameTests::saved);
	}

	static int ticks(String scene, int otherwise) {
		return TICKS.getOrDefault(scene, java.util.Arrays.asList(SCENES).contains(scene) ? 1200 : otherwise);
	}

	static int[] reach(String scene) {
		return new int[] {-1, 6, -1, 7};
	}

	private static Input keys(boolean forward, boolean back, boolean left, boolean right, boolean jump, boolean sprint) {
		return new Input(forward, back, left, right, jump, false, sprint);
	}

	private static final Input NONE = Input.EMPTY;
	private static final Input LOWER = keys(false, false, false, false, false, true);
	private static final Input DRAG = keys(true, false, false, false, false, false);
	private static final Input HOIST = keys(false, false, false, false, true, false);
	private static final Input PAY_OUT = keys(false, true, false, false, false, false);
	private static final Input HOIST_PAY_OUT = keys(false, true, false, false, true, false);
	private static final Input SWING_RIGHT = keys(false, false, false, true, false, false);

	private static void log(String scene, String what) {
		System.out.printf(Locale.ROOT, "[tailings-test] %s: %s%n", scene, what);
	}

	private static int count(GameTestHelper helper, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		int n = 0;
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					if (helper.getBlockState(new BlockPos(x, y, z)).is(block)) {
						n++;
					}
				}
			}
		}
		return n;
	}

	private static void fill(GameTestHelper helper, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					helper.setBlock(x, y, z, block);
				}
			}
		}
	}

	// ---- by hand -------------------------------------------------------------------------------------------------------------------

	/** A running dragline on its outlet, a strip of sand to its left, and a bed of sand 45 m ahead where its bucket is put. */
	private static DraglineEntity handScene(GameTestHelper helper, PitSwitchHouseBlockEntity[] houseOut, int fast) {
		DraglineGameTests.bench(helper, 90, 100);
		DraglineGameTests.strip(helper, 10, 10, 2, Blocks.SAND, 1);
		fill(helper, Blocks.SAND, 15, 26, DraglineGameTests.BENCH - 1, DraglineGameTests.BENCH - 1, 55, 80);
		PitSwitchHouseBlockEntity house = DraglineGameTests.house(helper);
		houseOut[0] = house;
		return DraglineGameTests.dragline(helper, house, 10, 10, 2, fast);
	}

	/** Puts it in HAND: seated, a key touched, and the bucket hung 45 m ahead, 3 m over the sand bed. */
	private static void takeOver(DraglineEntity d) {
		d.testSeat(true);
		d.testDrive(HOIST);
	}

	/**
	 * A key takes it into HAND mid-cycle; sprint lowers the bucket onto the sand, W drags it through the bank: 25 bank blocks in a full
	 * bucket at 4.8 MW, the outlet's MACHINE HAND on; jump hoists it clear and S dumps it where it hangs, set by the settle rule, the same
	 * sand: a cycle and a hand cast counted.
	 */
	static void handDig(GameTestHelper helper) {
		PitSwitchHouseBlockEntity[] h = new PitSwitchHouseBlockEntity[1];
		DraglineEntity d = handScene(helper, h, 4);
		int bed = count(helper, Blocks.SAND, 0, 90, 0, 30, 45, 100);
		double[] digWatts = {0};
		helper.onEachTick(() -> {
			if (d.hand() && d.load().total() > 0 && d.load().total() < Dragline.BUCKET_KG && d.handWatts() > digWatts[0]) {
				digWatts[0] = d.handWatts();
			}
		});
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> helper.assertTrue(h[0].close(0) == null, Component.literal("its outlet should close")))
				.thenIdle(5)
				.thenExecute(() -> takeOver(d))
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertTrue(d.hand() && d.status().startsWith("HAND"), Component.literal("a key takes it into HAND: " + d.status()));
					helper.assertTrue(h[0].readPoint(PitSwitchHouseBlockEntity.HAND_POINTS), Component.literal("MACHINE HAND on its outlet"));
					d.emptyBucket();
					d.handPose(0, 45, 3);
					d.testDrive(LOWER);
				})
				.thenWaitUntil(() -> helper.assertTrue(d.bucketUp() <= 0.06, Component.literal("lowered onto the sand: " + d.bucketUp())))
				.thenExecute(() -> d.testDrive(DRAG))
				.thenWaitUntil(() -> helper.assertTrue(d.load().total() >= 25 * 1_600L || d.status().contains("skids"), Component.literal("dragging fills it: " + d.load().tonnes())))
				.thenExecute(() -> {
					int dug = bed - count(helper, Blocks.SAND, 0, 90, 0, 30, 45, 100);
					log("dragline_hand_dig", String.format(Locale.ROOT, "dug %d blocks, %s, at %.0f kW; bucket %.1f m out, %.1f m", dug, d.load().tonnes(), digWatts[0] / 1000,
							d.bucketOut(), d.bucketUp()));
					helper.assertTrue(dug == 25, Component.literal("a full bucket is 25 bank blocks of sand: " + dug));
					helper.assertTrue(Math.abs(digWatts[0] - DraglineRopes.DIG_WATTS) < 1, Component.literal("4.8 MW dragging through the bank: " + digWatts[0]));
					d.testDrive(HOIST);
				})
				.thenWaitUntil(() -> helper.assertTrue(d.bucketUp() > 3, Component.literal("hoisted clear: " + d.bucketUp())))
				.thenExecute(() -> d.testDrive(PAY_OUT))
				.thenWaitUntil(() -> helper.assertTrue(d.handCasts() == 1 && d.pouring() == 0, Component.literal("dumped and set: " + d.status())))
				.thenExecute(() -> {
					int after = count(helper, Blocks.SAND, 0, 90, 0, 30, 45, 100);
					log("dragline_hand_dig", "cast by hand, " + after + " of " + bed + " sand blocks; " + d.status());
					helper.assertTrue(after == bed && d.load().empty() && d.cycles() >= 1, Component.literal("all 25 back on the ground: " + after + " of " + bed));
				})
				.thenSucceed();
	}

	/** The hoist stops 4 m under the boom point, the drag 15 m from the centre; the house coasts about 40 degrees from full swing. */
	static void handLimits(GameTestHelper helper) {
		PitSwitchHouseBlockEntity[] h = new PitSwitchHouseBlockEntity[1];
		DraglineEntity d = handScene(helper, h, 1);
		double[] from = {0};
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> helper.assertTrue(h[0].close(0) == null, Component.literal("its outlet should close")))
				.thenIdle(5)
				.thenExecute(() -> takeOver(d))
				.thenIdle(2)
				.thenExecute(() -> {
					d.handPose(0, 45, 3);
					d.testDrive(HOIST_PAY_OUT);   // hoisting with the drag held, the bucket only swings up under the fairlead: pay the drag out with it
				})
				.thenIdle(400)
				.thenExecute(() -> {
					helper.assertTrue(d.bucketUp() <= DraglineRopes.POINT_UP - DraglineRopes.HOIST_LIMIT + 0.01 && d.bucketUp() > 30,
							Component.literal("the hoist limit: " + d.bucketUp()));
					d.handPose(0, 20, 20);
					d.testDrive(DRAG);
				})
				.thenIdle(200)
				.thenExecute(() -> {
					helper.assertTrue(d.bucketOut() >= DraglineRopes.DRAG_LIMIT_OUT - 0.01, Component.literal("the drag limit: " + d.bucketOut()));
					d.handPose(0, 40, 20);
					d.testDrive(SWING_RIGHT);
				})
				.thenIdle(200)
				.thenExecute(() -> {
					helper.assertTrue(Math.abs(d.swingRate() - DraglineRopes.SWING_MAX) < 1e-6, Component.literal("full swing after 9 s: " + d.swingRate()));
					from[0] = d.swing();
					d.testDrive(NONE);
				})
				.thenWaitUntil(() -> helper.assertTrue(d.swingRate() == 0, Component.literal("it coasts to a stop: " + d.swingRate())))
				.thenExecute(() -> {
					double coast = d.swing() - from[0];
					log("dragline_hand_limits", String.format(Locale.ROOT, "hoist limit %.1f m up; drag limit %.1f m out; coasted %.1f degrees", DraglineRopes.POINT_UP - DraglineRopes.HOIST_LIMIT,
							DraglineRopes.DRAG_LIMIT_OUT, coast));
					helper.assertTrue(Math.abs(coast - 40.5) < 1, Component.literal("about 40 degrees: " + coast));
				})
				.thenSucceed();
	}

	/** A full bucket when its operator gets out: the job casts it beyond the strip, then digs on. */
	static void handGiveBack(GameTestHelper helper) {
		PitSwitchHouseBlockEntity[] h = new PitSwitchHouseBlockEntity[1];
		DraglineEntity d = handScene(helper, h, 4);
		long[] before = {0};
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> helper.assertTrue(h[0].close(0) == null, Component.literal("its outlet should close")))
				.thenIdle(5)
				.thenExecute(() -> takeOver(d))
				.thenIdle(2)
				.thenExecute(() -> {
					d.emptyBucket();
					d.handPose(0, 45, 3);
					d.testDrive(LOWER);
				})
				.thenWaitUntil(() -> helper.assertTrue(d.bucketUp() <= 0.06, Component.literal("on the sand")))
				.thenExecute(() -> d.testDrive(DRAG))
				.thenWaitUntil(() -> helper.assertTrue(d.load().total() >= 25 * 1_600L, Component.literal("full: " + d.load().tonnes())))
				.thenExecute(() -> {
					before[0] = d.cycles();
					d.testDrive(NONE);
					d.testSeat(false);
				})
				.thenIdle(2)
				.thenExecute(() -> helper.assertTrue(!d.hand(), Component.literal("out of the cab: AUTO: " + d.status())))
				.thenWaitUntil(() -> helper.assertTrue(d.cycles() > before[0] && d.load().empty(), Component.literal("the job casts what the operator dug: " + d.status())))
				.thenExecute(() -> log("dragline_hand_give_back", "handed back full; cast by the job at " + d.dumpAt() + "; " + d.status()))
				.thenSucceed();
	}

	/** In HAND the PLC's RUN off sets the brakes: the hoist does not move; MACHINE HAND reads on. */
	static void handPlcStop(GameTestHelper helper) {
		PitSwitchHouseBlockEntity[] h = new PitSwitchHouseBlockEntity[1];
		DraglineEntity d = handScene(helper, h, 1);
		PitSwitchHouseBlockEntity house = h[0];
		int base = PitSwitchHouseBlockEntity.MACHINE_POINTS;
		boolean[] run = {true};
		helper.onEachTick(() -> {
			long now = helper.getLevel().getGameTime();
			house.commandPoint(0, true, now);
			house.commandPoint(base, run[0], now);
		});
		house.cycleHoa(null);
		house.cycleHoa(null);   // AUTO
		double[] up = {0};
		helper.startSequence()
				.thenIdle(10)
				.thenExecute(() -> takeOver(d))
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertTrue(d.hand() && house.readPoint(PitSwitchHouseBlockEntity.HAND_POINTS), Component.literal("HAND, and MACHINE HAND on"));
					d.handPose(0, 45, 3);
					run[0] = false;
				})
				.thenIdle(5)
				.thenExecute(() -> up[0] = d.bucketUp())
				.thenIdle(40)
				.thenExecute(() -> {
					log("dragline_hand_plc_stop", "RUN off in HAND: bucket " + up[0] + " -> " + d.bucketUp());
					helper.assertTrue(Math.abs(d.bucketUp() - up[0]) < 1e-6 && d.hand(), Component.literal("the brakes hold: " + up[0] + " -> " + d.bucketUp()));
					run[0] = true;
				})
				.thenIdle(20)
				.thenExecute(() -> helper.assertTrue(d.bucketUp() > up[0], Component.literal("RUN on again, the hoist works: " + d.bucketUp())))
				.thenSucceed();
	}

	// ---- slopes ----------------------------------------------------------------------------------------------------------------------

	/** The scenes' strip in the slope record: from a dragline centre at x 20.5, z 30.5 facing +z, its tub's floor at {@code crest}. */
	private static long strip(GameTestHelper helper, int crest, int width, int length) {
		BlockPos o = helper.absolutePos(BlockPos.ZERO);
		return Slopes.register(helper.getLevel(), UUID.randomUUID(), o.getX() + CX, o.getZ() + CZ, 0, 1, 1, 0, o.getY() + crest, width, length, Dragline.BATTER_VERTICAL);
	}

	/**
	 * A highwall along x 32: stone up to {@code toe} (its top), {@code mat} from there to {@code crest} behind it (x 0-32), and the face
	 * laid back at {@code batter} into the strip, over z 20-45.
	 */
	private static void wall(GameTestHelper helper, Block mat, int toe, int crest, int batter) {
		fill(helper, Blocks.STONE, 0, 90, 1, 1, 0, 65);   // a floor wide enough that nothing rolls off it
		fill(helper, Blocks.STONE, 0, 70, 1, toe - 1, 20, 45);
		fill(helper, mat, 0, 32, toe, crest - 1, 20, 45);
		for (int d = 1; d <= crest - toe; d++) {
			int off = Dragline.batterOffset(batter, d);
			if (off > 0) {
				fill(helper, mat, 33, 32 + off, crest - d, crest - d, 20, 45);
			}
		}
	}

	private static Slopes.Part part(GameTestHelper helper, long id) {
		return Slopes.strip(helper.getLevel().getServer(), id).section(0).wall();
	}

	/** Slope failures off: a 10 m vertical sand highwall stands; a prism on it reads STEADY. */
	static void ruleOff(GameTestHelper helper) {
		wall(helper, Blocks.SAND, 3, 13, Dragline.BATTER_VERTICAL);
		long id = strip(helper, 13, 30, 10);
		helper.setBlock(30, 13, 35, TailingsBlocks.SLOPE_PRISM);
		int sand = count(helper, Blocks.SAND, 0, 70, 1, 20, 20, 45);
		helper.startSequence()
				.thenIdle(450)
				.thenExecute(() -> {
					Slopes.Reading r = Slopes.read(helper.getLevel(), helper.absolutePos(new BlockPos(30, 13, 35)));
					log("slope_rule_off", "rule " + OptInRules.on(helper.getLevel().getServer(), OptInRules.SLOPE_FAILURES) + "; " + r.describe());
					helper.assertTrue(!OptInRules.on(helper.getLevel().getServer(), OptInRules.SLOPE_FAILURES), Component.literal("off in this batch"));
					helper.assertTrue(count(helper, Blocks.SAND, 0, 70, 1, 20, 20, 45) == sand, Component.literal("the wall stands"));
					helper.assertTrue(r.onSlope() && r.trend().equals("STEADY"), Component.literal("its prism reads STEADY: " + r.describe()));
					helper.assertTrue(part(helper, id).stage() == Slope.Stage.STABLE, Component.literal("never checked"));
				})
				.thenSucceed();
	}

	/** With slope failures on, the job's auto highwall is 2 in 1: the strip's first column dug 2 m, the next 4 m, the third to the seam. */
	static void batter(GameTestHelper helper) {
		DraglineGameTests.bench(helper, 90, 70);
		DraglineGameTests.strip(helper, 10, 10, 5, Blocks.SAND, 2);
		PitSwitchHouseBlockEntity house = DraglineGameTests.house(helper);
		DraglineEntity d = DraglineGameTests.dragline(helper, house, 10, 10, 5, 55);
		int b = DraglineGameTests.BENCH;
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> helper.assertTrue(house.close(0) == null, Component.literal("its outlet should close")))
				.thenWaitUntil(() -> helper.assertTrue(d.mode() == DraglineEntity.Mode.DONE, Component.literal("DONE: " + d.status())))
				.thenExecute(() -> {
					int first = count(helper, Blocks.SAND, 33, 33, b - 5, b - 1, 30, 39), second = count(helper, Blocks.SAND, 34, 34, b - 5, b - 1, 30, 39),
							third = count(helper, Blocks.SAND, 35, 35, b - 5, b - 1, 30, 39);
					log("dragline_batter", "sand left in the first three columns: " + first + ", " + second + ", " + third + "; " + d.status());
					helper.assertTrue(first == 30 && second == 10 && third == 0, Component.literal("laid back 2 in 1: " + first + ", " + second + ", " + third));
				})
				.thenSucceed();
	}

	/** Dirt laid back 2 in 1, 16 m high: CREEP; a tension crack opens on the bench where the failure plane meets it; its prism creeps. */
	static void creep(GameTestHelper helper) {
		wall(helper, Blocks.DIRT, 3, 19, Dragline.BATTER_2_IN_1);
		long id = strip(helper, 19, 30, 10);
		helper.setBlock(30, 19, 35, TailingsBlocks.SLOPE_PRISM);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> Slopes.checkNow(helper.getLevel(), id, 0))
				.thenExecute(() -> {
					Slopes.Part p = part(helper, id);
					int cracks = count(helper, TailingsBlocks.TENSION_CRACK, 0, 32, 18, 18, 20, 45);
					Slopes.Reading r = Slopes.read(helper.getLevel(), helper.absolutePos(new BlockPos(30, 19, 35)));
					log("slope_highwall_creep", String.format(Locale.ROOT, "FS %.2f, %s, %.0f m at %.0f deg; %d crack; %s", p.fs(), p.stage(), p.height(), p.beta(), cracks, r.describe()));
					helper.assertTrue(p.stage() == Slope.Stage.CREEP && p.fs() < Slope.CRACK_FS, Component.literal("CREEP with cracks: " + p.fs()));
					helper.assertTrue(Math.abs(p.height() - 16) < 0.01 && Math.abs(p.beta() - Slope.angle(2, 1)) < 1, Component.literal("16 m at 2 in 1: " + p.height() + ", " + p.beta()));
					helper.assertTrue(cracks == 1, Component.literal("a crack: " + cracks));
					helper.assertTrue(r.trend().equals("CREEPING"), Component.literal("its prism creeps: " + r.describe()));
				})
				.thenSucceed();
	}

	private static int earth(GameTestHelper helper) {
		return count(helper, Blocks.DIRT, -10, 90, 1, 45, -5, 70) + count(helper, TailingsBlocks.TENSION_CRACK, -10, 90, 1, 45, -5, 70);
	}

	private static int itemBlocks(GameTestHelper helper) {
		int n = 0;
		for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(new BlockPos(-20, 0, 0))).expandTowards(130, 60, 70))) {
			n += item.getItem().getCount();
		}
		return n;
	}

	/** 9 m of vertical dirt: FS 0.78, FAILING; it slides on its clock (about 2 min), every block of its wedge in the cut, none lost. */
	static void slide(GameTestHelper helper) {
		wall(helper, Blocks.DIRT, 3, 12, Dragline.BATTER_VERTICAL);
		long id = strip(helper, 12, 30, 10);
		int before = earth(helper);
		long[] started = {0};
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					Slopes.checkNow(helper.getLevel(), id, 0);
					Slopes.Part p = part(helper, id);
					started[0] = helper.getLevel().getGameTime();
					log("slope_highwall_slide", String.format(Locale.ROOT, "FS %.2f, %s; slides in %d ticks", p.fs(), p.stage(), p.failAt() - started[0]));
					helper.assertTrue(p.stage() == Slope.Stage.FAILING && Math.abs(p.fs() - 0.78) < 0.02, Component.literal("FAILING at 0.78: " + p.fs()));
					helper.assertTrue(p.failAt() - started[0] >= 2400 && p.failAt() - started[0] <= 2500, Component.literal("about 2 min: " + (p.failAt() - started[0])));
				})
				.thenWaitUntil(() -> helper.assertTrue(part(helper, id).stage() == Slope.Stage.SLID, Component.literal("slid: " + part(helper, id).stage())))
				.thenWaitUntil(() -> helper.assertTrue(Slopes.pending(helper.getLevel()) == 0, Component.literal("set: " + Slopes.pending(helper.getLevel()))))
				.thenExecute(() -> {
					int after = earth(helper), items = itemBlocks(helper);
					int inCut = count(helper, Blocks.DIRT, 33, 70, 3, 12, 20, 45);
					log("slope_highwall_slide", String.format(Locale.ROOT, "slid after %d ticks: %d dirt before, %d after, %d as items; %d in the cut", helper.getLevel().getGameTime() - started[0],
							before, after, items, inCut));
					helper.assertTrue(after + items == before, Component.literal("none lost: " + before + " -> " + after + " + " + items));
					helper.assertTrue(inCut > 50, Component.literal("the wedge ran into the cut: " + inCut));
				})
				.thenSucceed();
	}

	/** A pond behind the crest turns a 14 m dirt wall 2 in 1 from CREEP (1.19) to FAILING (0.91); drained, its clock stops. */
	static void water(GameTestHelper helper) {
		wall(helper, Blocks.DIRT, 3, 17, Dragline.BATTER_2_IN_1);
		long id = strip(helper, 17, 30, 10);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					Slopes.checkNow(helper.getLevel(), id, 0);
					helper.assertTrue(part(helper, id).stage() == Slope.Stage.CREEP && Math.abs(part(helper, id).fs() - 1.19) < 0.02, Component.literal("dry: " + part(helper, id).fs()));
					fill(helper, Blocks.WATER, 22, 24, 16, 16, 33, 37);
				})
				.thenIdle(2)
				.thenExecute(() -> {
					Slopes.checkNow(helper.getLevel(), id, 0);
					Slopes.Part p = part(helper, id);
					log("slope_water", String.format(Locale.ROOT, "with the pond FS %.2f, %s", p.fs(), p.stage()));
					helper.assertTrue(p.stage() == Slope.Stage.FAILING && Math.abs(p.fs() - 0.91) < 0.02 && p.failAt() > 0, Component.literal("wet: " + p.fs()));
					fill(helper, Blocks.DIRT, 22, 24, 16, 16, 33, 37);
				})
				.thenIdle(2)
				.thenExecute(() -> {
					Slopes.checkNow(helper.getLevel(), id, 0);
					Slopes.Part p = part(helper, id);
					helper.assertTrue(p.stage() == Slope.Stage.CREEP && p.failAt() < 0, Component.literal("drained, the clock stops: " + p.fs() + ", " + p.failAt()));
				})
				.thenSucceed();
	}

	/** A 40 m vertical rock wall stands; a blast 10 m behind its crest halves its cohesion and it fails. */
	static void blast(GameTestHelper helper) {
		wall(helper, Blocks.STONE, 3, 43, Dragline.BATTER_VERTICAL);
		long id = strip(helper, 43, 30, 10);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					Slopes.checkNow(helper.getLevel(), id, 0);
					helper.assertTrue(part(helper, id).stage() == Slope.Stage.STABLE, Component.literal("rock stands: " + part(helper, id).fs()));
					Slopes.blasted(helper.getLevel(), helper.absolutePos(new BlockPos(22, 43, 35)));
					Slopes.checkNow(helper.getLevel(), id, 0);
					Slopes.Part p = part(helper, id);
					log("slope_blast", String.format(Locale.ROOT, "after the blast FS %.2f, %s", p.fs(), p.stage()));
					helper.assertTrue(Slopes.strip(helper.getLevel().getServer(), id).section(0).damaged() && p.stage() == Slope.Stage.FAILING,
							Component.literal("blast damaged, it fails: " + p.fs()));
				})
				.thenSucceed();
	}

	/** Spoil against the toe of a failing 9 m dirt wall lowers its height: the clock stops. */
	static void buttress(GameTestHelper helper) {
		wall(helper, Blocks.DIRT, 3, 12, Dragline.BATTER_VERTICAL);
		long id = strip(helper, 12, 30, 10);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					Slopes.checkNow(helper.getLevel(), id, 0);
					helper.assertTrue(part(helper, id).stage() == Slope.Stage.FAILING, Component.literal("failing"));
					fill(helper, Blocks.DIRT, 33, 34, 3, 7, 20, 45);   // a buttress 5 m high at its toe
				})
				.thenIdle(2)
				.thenExecute(() -> {
					Slopes.checkNow(helper.getLevel(), id, 0);
					Slopes.Part p = part(helper, id);
					log("slope_buttress", String.format(Locale.ROOT, "buttressed: %.0f m, FS %.2f, %s", p.height(), p.fs(), p.stage()));
					helper.assertTrue(p.stage() != Slope.Stage.FAILING && p.failAt() < 0, Component.literal("the clock stops: " + p.fs()));
				})
				.thenSucceed();
	}

	/** A 15 m spoil pile on clay with water at its toe slides to a flatter face; none of it lost. */
	static void spoil(GameTestHelper helper) {
		fill(helper, Blocks.STONE, 0, 90, 1, 1, 0, 65);
		fill(helper, Blocks.CLAY, 30, 90, 2, 2, 0, 65);
		fill(helper, Blocks.STONE, 0, 29, 2, 2, 0, 65);
		for (int x = 44; x <= 64; x++) {
			int k = Math.min(15, (int) Math.ceil((x - 43) * 0.75));
			fill(helper, Blocks.DIRT, x, x, 3, 2 + k, 20, 45);
		}
		fill(helper, Blocks.WATER, 40, 43, 3, 3, 30, 40);
		long id = strip(helper, 13, 10, 10);
		Slopes.cast(helper.getLevel(), id, helper.absolutePos(new BlockPos(50, 10, 35)));
		int before = earth(helper);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					Slopes.checkNow(helper.getLevel(), id, 0);
					Slopes.Part p = Slopes.strip(helper.getLevel().getServer(), id).section(0).spoil();
					log("slope_spoil", String.format(Locale.ROOT, "spoil %.0f m, FS %.2f, %s", p.height(), p.fs(), p.stage()));
					helper.assertTrue(p.stage() == Slope.Stage.FAILING && Math.abs(p.fs() - 0.8) < 0.01, Component.literal("15 m on wet clay: " + p.fs()));
				})
				.thenWaitUntil(() -> helper.assertTrue(Slopes.strip(helper.getLevel().getServer(), id).section(0).spoil().stage() == Slope.Stage.SLID, Component.literal("slid")))
				.thenWaitUntil(() -> helper.assertTrue(Slopes.pending(helper.getLevel()) == 0, Component.literal("set: " + Slopes.pending(helper.getLevel()))))
				.thenExecute(() -> {
					int top = 0;
					for (int y = 40; y >= 3 && top == 0; y--) {
						if (count(helper, Blocks.DIRT, 44, 70, y, y, 31, 40) > 0) {
							top = y;
						}
					}
					int after = earth(helper), items = itemBlocks(helper);
					log("slope_spoil", "slid: its top now y " + top + " (was 17); " + before + " dirt, " + after + " after, " + items + " as items");
					helper.assertTrue(top < 17, Component.literal("its crest came down: " + top));
					helper.assertTrue(after + items == before, Component.literal("none lost: " + before + " -> " + after + " + " + items));
				})
				.thenSucceed();
	}

	/**
	 * A dragline standing on dirt by a 30 m vertical highwall: the failure plane meets its bench 19 m behind the crest, past the tub's
	 * centre, and with a third of its tub's ground gone it is wrecked. (Its strip starts at its centre, so a section's slide reaches only
	 * the half of its tub ahead of that.)
	 */
	static void wrecked(GameTestHelper helper) {
		wall(helper, Blocks.DIRT, 3, 33, Dragline.BATTER_VERTICAL);
		fill(helper, Blocks.DIRT, 0, 32, 3, 32, 10, 19);
		DraglineEntity d = standing(helper, 33);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					Slopes.checkNow(helper.getLevel(), d.stripId(), 0);
					helper.assertTrue(part(helper, d.stripId()).stage() == Slope.Stage.FAILING, Component.literal("failing: " + part(helper, d.stripId()).fs()));
				})
				.thenWaitUntil(() -> helper.assertTrue(d.isRemoved(), Component.literal("it goes down with the slide: " + d.status())))
				.thenExecute(() -> {
					int houses = 0;
					for (ItemEntity item : helper.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(helper.absolutePos(BlockPos.ZERO)).inflate(120))) {
						if (item.getItem().is(TailingsBlocks.DRAGLINE_HOUSE.asItem())) {
							houses += item.getItem().getCount();
						}
					}
					log("slope_dragline_wrecked", "wrecked " + d.wrecked() + "; house items " + houses);
					helper.assertTrue(d.wrecked() && houses == 1, Component.literal("wrecked into its parts: " + houses + " house"));
				})
				.thenSucceed();
	}

	/** A dragline beside a 13 m vertical dirt wall: the slide takes a little of its tub's ground: GROUND GONE; made up, RUN works. */
	static void groundGone(GameTestHelper helper) {
		wall(helper, Blocks.DIRT, 10, 23, Dragline.BATTER_VERTICAL);
		fill(helper, Blocks.DIRT, 0, 32, 10, 22, 10, 19);
		DraglineEntity d = standing(helper, 23);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> Slopes.checkNow(helper.getLevel(), d.stripId(), 0))
				.thenWaitUntil(() -> helper.assertTrue(d.groundGone(), Component.literal("GROUND GONE: " + d.status() + "; " + part(helper, d.stripId()).stage())))
				.thenExecute(() -> {
					log("slope_dragline_ground_gone", d.status());
					helper.assertTrue(d.isAlive() && d.status().startsWith("GROUND GONE"), Component.literal(d.status()));
					for (int x = 10; x <= 31; x++) {
						for (int z = 20; z <= 41; z++) {
							if (helper.getBlockState(new BlockPos(x, 22, z)).isAir()) {
								helper.setBlock(x, 22, z, Blocks.DIRT);
							}
						}
					}
					d.groundCheck(helper.getLevel());
					helper.assertTrue(!d.groundGone() && d.mode() == DraglineEntity.Mode.STOPPED, Component.literal("made up: " + d.status()));
					d.action(0, null);
					helper.assertTrue(d.mode() == DraglineEntity.Mode.RUNNING, Component.literal("RUN: " + d.status()));
				})
				.thenSucceed();
	}

	/** A dragline lifted with no cable, its tub's floor at {@code floor}, its strip registered as RUN does: it stands in its job. */
	private static DraglineEntity standing(GameTestHelper helper, int floor) {
		DraglineEntity d = TailingsEntities.DRAGLINE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
		BlockPos o = helper.absolutePos(BlockPos.ZERO);
		d.setPos(o.getX() + CX, o.getY() + floor, o.getZ() + CZ);
		d.setYRot(0);
		d.job().set(0, 30);
		d.job().set(1, 10);
		d.job().set(2, 1);
		d.job().set(3, 0);
		helper.getLevel().addFreshEntity(d);
		d.start();
		return d;
	}

	/** A survey station reads three prisms on a failing wall: ALARM over its set speed, WORST SPEED and PRISMS on its loops. */
	static void stationAlarm(GameTestHelper helper) {
		wall(helper, Blocks.DIRT, 3, 12, Dragline.BATTER_VERTICAL);
		long id = strip(helper, 12, 30, 10);
		for (int z : new int[] {33, 36, 39}) {
			helper.setBlock(28, 12, z, TailingsBlocks.SLOPE_PRISM);
		}
		helper.setBlock(20, 12, 50, TailingsBlocks.SURVEY_STATION);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					Slopes.checkNow(helper.getLevel(), id, 0);
					SurveyStationBlockEntity station = helper.getBlockEntity(new BlockPos(20, 12, 50), SurveyStationBlockEntity.class);
					station.alarmAt(1);
					station.read(helper.getLevel());
					station.lines().forEach(line -> log("slope_station_alarm", line));
					helper.assertTrue(station.seen() >= 3 && station.lines().stream().filter(l -> l.contains("ACCELERATING")).count() == 3,
							Component.literal("its three prisms in sight, accelerating: " + station.seen()));
					helper.assertTrue(station.readPoint(0) && station.alarm(), Component.literal("ALARM: worst " + station.worst()));
					helper.assertTrue(station.readMa(1) > 4.5 && station.readMa(2) >= 4 + 16 * 3 / 20.0 - 1e-6, Component.literal("its loops: " + station.readMa(1) + ", " + station.readMa(2)));
					helper.assertTrue(station.lines().stream().anyMatch(l -> l.contains("ACCELERATING") && l.contains("slide in about")), Component.literal("a predicted slide time"));
				})
				.thenSucceed();
	}

	/** A failing section's clock, its blast damage and its prisms survive a save and a load. */
	static void saved(GameTestHelper helper) {
		wall(helper, Blocks.DIRT, 3, 12, Dragline.BATTER_VERTICAL);
		long id = strip(helper, 12, 30, 10);
		helper.setBlock(28, 12, 35, TailingsBlocks.SLOPE_PRISM);
		helper.startSequence()
				.thenIdle(2)
				.thenExecute(() -> {
					Slopes.blasted(helper.getLevel(), helper.absolutePos(new BlockPos(25, 12, 35)));
					Slopes.checkNow(helper.getLevel(), id, 0);
					Slopes.Part p = part(helper, id);
					Slopes back = Slopes.roundTrip(helper.getLevel().getServer());
					Slopes.Strip s = back.stripOf(id);
					helper.assertTrue(s != null, Component.literal("the strip comes back"));
					Slopes.Part q = s.section(0).wall();
					log("slope_saved", "fail at " + p.failAt() + " -> " + q.failAt() + "; damaged " + s.section(0).damaged());
					helper.assertTrue(q.failAt() == p.failAt() && q.stage() == p.stage() && s.section(0).damaged() && Math.abs(q.mm() - p.mm()) < 1e-9,
							Component.literal("its clock, stage, damage and movement"));
				})
				.thenSucceed();
	}

	/** Every rule as a new world has it, and slope_failures on. */
	static Map<String, Boolean> ruleOn() {
		Map<String, Boolean> rules = new LinkedHashMap<>(OptInRules.NEW_WORLD);
		rules.put(OptInRules.SLOPE_FAILURES, true);
		return rules;
	}
}
