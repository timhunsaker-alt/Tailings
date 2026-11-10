package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.entity.UltraHaulTruckEntity;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PitLine;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server scenes for the pit line (docs/multiblocks/primary-gyratory-stockpile.md, P7a; decisions in docs/questions/p7-pit-line.md): an
 * ultra truck tips into the gyratory's pocket and the gyratory crushes at 2,400 t/h into the surge pocket, soil refused; a boulder
 * bridges the bowl until the rock breaker breaks it; the stockpile head builds a cone at the settle rule's 37 degrees; three reclaim
 * feeders draw a pile down to its dead stockpile; the belt weigher's setpoint shared by the feeders; a heavy belt's drives and grade;
 * the primary line from the surge pocket up the belt onto the pile; every part its spec size with its PLC points.
 */
final class PitLineGameTests {
	static final String[] SCENES = {"p7_gyratory_tip", "p7_gyratory_bridge", "p7_stockpile_cone", "p7_stockpile_live", "p7_stockpile_one", "p7_reclaim_rate", "p7_heavy_belt",
			"p7_primary_line", "p7a_parts", "p7_sag_grind", "p7_sag_overload", "p7_pebble_off", "p7_cyclones", "p7b_parts"};
	static final Map<String, Integer> TICKS = Map.of("p7_gyratory_tip", 800, "p7_gyratory_bridge", 1600, "p7_stockpile_cone", 3000, "p7_stockpile_live", 9000, "p7_stockpile_one", 9000,
			"p7_reclaim_rate", 1200, "p7_heavy_belt", 1200, "p7_primary_line", 1600, "p7a_parts", 200);
	static final Map<String, Integer> MORE_TICKS = Map.of("p7_sag_grind", 1600, "p7_sag_overload", 800, "p7_pebble_off", 600, "p7_cyclones", 400, "p7b_parts", 200);
	static final Map<String, Integer> PADDING = Map.of("p7_heavy_belt", 180, "p7_stockpile_cone", 60, "p7_stockpile_live", 60);
	/** Chunks each scene reaches from its origin: {x from, x to, z from, z to}. */
	private static final Map<String, int[]> REACH = Map.of("p7_heavy_belt", new int[] {-1, 2, -1, 11}, "p7_gyratory_tip", new int[] {-1, 3, -1, 4},
			"p7_primary_line", new int[] {-1, 3, -1, 4});
	static final Set<String> ALL = Set.of(SCENES);

	private PitLineGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("p7_gyratory_tip", wide("p7_gyratory_tip", PitLineGameTests::gyratoryTip));
		out.accept("p7_gyratory_bridge", wide("p7_gyratory_bridge", PitLineGameTests::gyratoryBridge));
		out.accept("p7_stockpile_cone", wide("p7_stockpile_cone", PitLineGameTests::stockpileCone));
		out.accept("p7_stockpile_live", wide("p7_stockpile_live", PitLineGameTests::stockpileLive));
		out.accept("p7_stockpile_one", wide("p7_stockpile_one", PitLineGameTests::stockpileOne));
		out.accept("p7_reclaim_rate", wide("p7_reclaim_rate", PitLineGameTests::reclaimRate));
		out.accept("p7_heavy_belt", wide("p7_heavy_belt", PitLineGameTests::heavyBelt));
		out.accept("p7_primary_line", wide("p7_primary_line", PitLineGameTests::primaryLine));
		out.accept("p7a_parts", wide("p7a_parts", PitLineGameTests::parts));
		out.accept("p7_sag_grind", wide("p7_sag_grind", PitLineGameTests::sagGrind));
		out.accept("p7_sag_overload", wide("p7_sag_overload", PitLineGameTests::sagOverload));
		out.accept("p7_pebble_off", wide("p7_pebble_off", PitLineGameTests::pebbleOff));
		out.accept("p7_cyclones", wide("p7_cyclones", PitLineGameTests::cyclones));
		out.accept("p7b_parts", wide("p7b_parts", PitLineGameTests::partsB));
	}

	static int ticks(String scene, int otherwise) {
		return TICKS.getOrDefault(scene, MORE_TICKS.getOrDefault(scene, otherwise));
	}

	static int padding(String scene, int otherwise) {
		return PADDING.getOrDefault(scene, ALL.contains(scene) ? 50 : otherwise);
	}

	private static Consumer<GameTestHelper> wide(String name, Consumer<GameTestHelper> scene) {
		int[] reach = REACH.getOrDefault(name, new int[] {-1, 3, -1, 3});
		return helper -> {
			ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
			for (int dx = reach[0]; dx <= reach[1]; dx++) {
				for (int dz = reach[2]; dz <= reach[3]; dz++) {
					helper.getLevel().setChunkForced(origin.x() + dx, origin.z() + dz, true);
				}
			}
			scene.accept(helper);
		};
	}

	// ---- building ---------------------------------------------------------------------------------------------------------------

	static PlantBlockEntity plant(GameTestHelper helper, Machine machine, BlockPos pos) {
		return TailingsGameTests.plant(helper, machine, pos);
	}

	static DumpBlockEntity pocket(GameTestHelper helper, BlockPos pos) {
		return TailingsGameTests.part(helper, TailingsBlocks.GYRATORY_POCKET, pos, DumpBlockEntity.class);
	}

	static void fill(GameTestHelper helper, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = y0; y <= y1; y++) {
					helper.setBlock(x, y, z, block);
				}
			}
		}
	}

	static BlockState heavy(Direction.Axis axis) {
		return TailingsBlocks.HEAVY_BELT_CONVEYOR.defaultBlockState().setValue(BeltConveyorBlock.AXIS, axis);
	}

	/** A heavy belt along z at {@code x, y} from {@code z0} to {@code z1}. */
	static void heavyRun(GameTestHelper helper, int x, int y, int z0, int z1) {
		for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++) {
			helper.setBlock(new BlockPos(x, y, z), heavy(Direction.Axis.Z));
		}
	}

	/** The primary stack, from the top: the dump pocket at y 26, the gyratory under it, the surge pocket, the primary apron feeder. */
	record Primary(DumpBlockEntity pocket, PlantBlockEntity crusher, PlantBlockEntity surge, PlantBlockEntity feeder, PlantBlockEntity breaker) {
	}

	static Primary primary(GameTestHelper helper) {
		PlantBlockEntity feeder = plant(helper, Machine.PRIMARY_APRON_FEEDER, new BlockPos(5, 8, 3));
		PlantBlockEntity surge = plant(helper, Machine.SURGE_POCKET, new BlockPos(3, 10, 1));
		PlantBlockEntity crusher = plant(helper, Machine.GYRATORY_CRUSHER, new BlockPos(4, 16, 2));
		DumpBlockEntity pocket = pocket(helper, new BlockPos(0, 26, 0));
		PlantBlockEntity breaker = plant(helper, Machine.ROCK_BREAKER, new BlockPos(0, 34, 2));
		return new Primary(pocket, crusher, surge, feeder, breaker);
	}

	private static String describe(PlantBlockEntity... parts) {
		StringBuilder out = new StringBuilder();
		for (PlantBlockEntity part : parts) {
			out.append(" | ").append(part.inspect().getString());
		}
		return out.toString();
	}

	private static int count(GameTestHelper helper, Block block, int x0, int x1, int y0, int y1, int z0, int z1) {
		int n = 0;
		for (int x = x0; x <= x1; x++) {
			for (int z = z0; z <= z1; z++) {
				for (int y = y0; y <= y1; y++) {
					if (helper.getBlockState(new BlockPos(x, y, z)).getBlock() == block) {
						n++;
					}
				}
			}
		}
		return n;
	}

	/** The first y at or above {@code y} in a column that is not a stockpile block. */
	private static int top(GameTestHelper helper, int x, int z, int y) {
		while (helper.getBlockState(new BlockPos(x, y, z)).getBlock() == TailingsBlocks.STOCKPILE_ROCK) {
			y++;
		}
		return y;
	}

	private static RockLoad crushed(Rock rock, double kg) {
		RockLoad load = new RockLoad();
		load.add(rock, SizeClass.CRUSHED, kg);
		return load;
	}

	// ---- the scenes -----------------------------------------------------------------------------------------------------------

	/**
	 * p7_gyratory_tip: an ultra truck backed to the pocket's rim tips 363 t of granite into it, 134 bank blocks, the 1.1 t under a block
	 * kept for the next load; a load of sand is refused. The gyratory crushes it into the surge pocket at 2,400 t/h, as crushed rock.
	 */
	private static void gyratoryTip(GameTestHelper helper) {
		Primary p = primary(helper);
		fill(helper, Blocks.STONE, 0, 13, 33, 33, 10, 30);
		UltraHaulTruckEntity truck = TailingsEntities.ULTRA_HAUL_TRUCK.create(helper.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
		BlockPos origin = helper.absolutePos(BlockPos.ZERO);
		// facing south, its tail over the pocket's south wall
		truck.setPos(origin.getX() + 7.0, origin.getY() + 34, origin.getZ() + 9.0 + truck.spec().length() / 2);
		truck.setYRot(0);
		helper.getLevel().addFreshEntity(truck);
		truck.load().add("minecraft:granite", 363_000);
		truck.refreshLoad();
		helper.assertTrue(truck.hopperBehind(helper.getLevel()) == p.pocket(), Component.literal("the gyratory's pocket should be under the ultra's tail"));
		truck.tip(helper.getLevel());
		helper.assertTrue(p.pocket().pocket() == 134, Component.literal("363 t of granite is 134 bank blocks in the pocket, got " + p.pocket().pocket()));
		helper.assertTrue(p.pocket().part().getOrDefault("minecraft:granite", 0L) == 1_200, Component.literal("the pocket keeps the 1.2 t under a block, got " + p.pocket().part()));
		helper.assertTrue(truck.load().empty(), Component.literal("the ultra comes away empty"));
		truck.load().add("minecraft:sand", 160_000);
		truck.tip(helper.getLevel());
		helper.assertTrue(truck.status().contains("not rock") && p.pocket().pocket() == 134, Component.literal("the pocket refuses soil, got " + truck.status()));
		truck.load().clear();
		helper.assertTrue(p.crusher().status().startsWith("NO FEED") || p.crusher().status().equals("idle") || p.crusher().hold().total() > 0,
				Component.literal("not yet running: " + p.crusher().status()));
		p.crusher().forceRunning();
		double[] start = {-1};
		long[] at = {0};
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(p.surge().hold().total() > 0, Component.literal("the gyratory should crush into the surge pocket" + describe(p.crusher(), p.surge()))))
				.thenExecute(() -> {
					start[0] = p.surge().hold().total();
					at[0] = helper.getLevel().getGameTime();
				})
				.thenExecuteAfter(200, () -> {
					double kg = p.surge().hold().total() - start[0];
					double tph = kg / (helper.getLevel().getGameTime() - at[0]) * 20 * 3.6;
					helper.assertTrue(Math.abs(tph - PitLine.GYRATORY_TPH) < 30, Component.literal("it should crush at 2,400 t/h, got " + tph + describe(p.crusher())));
					helper.assertTrue(p.surge().hold().kg(Rock.GRANITE, SizeClass.CRUSHED) > 0 && p.surge().hold().kg(Rock.GRANITE, null) == 0,
							Component.literal("the surge pocket holds crushed rock"));
					helper.assertTrue(p.crusher().status().startsWith("crushing Granite"), Component.literal("its status: " + p.crusher().status()));
				})
				.thenSucceed();
	}

	/**
	 * p7_gyratory_bridge: the 200th hard bank block is a boulder that bridges the bowl. The gyratory stops, BRIDGED; with the rock
	 * breaker's motor off it stays bridged; running, the breaker clears it in 30 s and the crusher goes on.
	 */
	private static void gyratoryBridge(GameTestHelper helper) {
		Primary p = primary(helper);
		helper.assertTrue(p.pocket().offer(Map.of("minecraft:granite", 4)), Component.literal("the pocket takes four blocks"));
		p.crusher().pit().setSinceBoulder(PitLine.BOULDER_EVERY - 1);
		p.crusher().forceRunning();
		long[] started = {0};
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(p.crusher().pit().bridged() && p.crusher().status().startsWith("BRIDGED"),
						Component.literal("the boulder should bridge the bowl" + describe(p.crusher()))))
				.thenExecuteAfter(100, () -> {
					helper.assertTrue(p.crusher().pit().bridged() && p.surge().hold().isEmpty(), Component.literal("with the breaker off it stays bridged" + describe(p.crusher(), p.breaker())));
					helper.assertTrue(p.crusher().readPoint(6), Component.literal("BRIDGED on the PLC"));
					p.breaker().forceRunning();
					started[0] = helper.getLevel().getGameTime();
				})
				.thenWaitUntil(() -> helper.assertTrue(!p.crusher().pit().bridged(), Component.literal("the breaker should clear it" + describe(p.breaker()))))
				.thenExecute(() -> {
					long took = helper.getLevel().getGameTime() - started[0];
					helper.assertTrue(took >= PitLine.BREAK_SECONDS * 20 - 2 && took <= PitLine.BREAK_SECONDS * 20 + 5, Component.literal("30 s to break it, took " + took + " ticks"));
				})
				.thenWaitUntil(() -> helper.assertTrue(p.surge().hold().total() > 2000, Component.literal("the crusher goes on" + describe(p.crusher()))))
				.thenSucceed();
	}

	/**
	 * p7_stockpile_cone: 2,000 blocks dropped by the stockpile head stand as a cone at the settle rule's 37 degrees: no step of 2 next door
	 * and no rise over 3 in 4 columns, about 10 m high.
	 */
	private static void stockpileCone(GameTestHelper helper) {
		fill(helper, Blocks.STONE, 0, 44, 9, 9, 0, 44);
		PlantBlockEntity head = plant(helper, Machine.STOCKPILE_HEAD, new BlockPos(21, 24, 19));
		int blocks = 2000;
		helper.onEachTick(() -> {
			if (head.pit().dropped() + head.hold().total() / PitLine.STOCKPILE_BLOCK_KG < blocks) {
				head.receive(crushed(Rock.GRANITE, PitLine.STOCKPILE_BLOCK_KG));
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(head.pit().dropped() == blocks, Component.literal("the head drops " + blocks + " blocks, so far " + head.pit().dropped() + "; " + head.status()));
			int stood = count(helper, TailingsBlocks.STOCKPILE_ROCK, 0, 44, 10, 23, 0, 44);
			helper.assertTrue(stood == blocks, Component.literal("all of them stand in the pile, " + stood));
			for (int x = 1; x <= 43; x++) {
				for (int z = 1; z <= 43; z++) {
					int h = top(helper, x, z, 10);
					helper.assertTrue(Math.abs(h - top(helper, x + 1, z, 10)) <= 1 && Math.abs(h - top(helper, x, z + 1, 10)) <= 1,
							Component.literal("a step of 2 or more at " + x + ", " + z));
					if (x + 4 <= 44) {
						helper.assertTrue(Math.abs(h - top(helper, x + 4, z, 10)) <= 3, Component.literal("steeper than 3 in 4 at " + x + ", " + z));
					}
				}
			}
			int high = top(helper, 22, 22, 10) - 10;
			helper.assertTrue(high >= 9 && high <= 12, Component.literal("a 2,000 block cone at 37 degrees stands about 10 m, got " + high));
			helper.assertTrue(Math.abs(head.pit().stackedKg() - blocks * PitLine.STOCKPILE_BLOCK_KG) < 1, Component.literal("it counts the tonnes stacked"));
		});
	}

	/** The reclaim tunnel: stone ground to y 9 with a 2 m wide tunnel at x 19-20, y 5-7, three feeders under openings in its roof. */
	record Tunnel(PlantBlockEntity[] feeders, PlantBlockEntity drive, PlantBlockEntity weigher) {
	}

	static Tunnel tunnel(GameTestHelper helper, int[] feederZ, boolean weigher, boolean openings) {
		fill(helper, Blocks.STONE, 0, 40, 4, 9, 0, 40);
		fill(helper, Blocks.AIR, 17, 21, 5, 7, 5, 36);
		PlantBlockEntity[] feeders = new PlantBlockEntity[feederZ.length];
		for (int i = 0; i < feederZ.length; i++) {
			feeders[i] = plant(helper, Machine.RECLAIM_FEEDER, new BlockPos(19, 6, feederZ[i]));
			if (openings) {
				fill(helper, Blocks.AIR, 19, 20, 8, 9, feederZ[i], feederZ[i]);   // the opening in the tunnel's roof over its feed end
			}
		}
		heavyRun(helper, 19, 5, 8, 36);
		PlantBlockEntity drive = plant(helper, Machine.HEAVY_CONVEYOR_DRIVE, new BlockPos(17, 5, 6));
		helper.setBlock(new BlockPos(19, 5, 7), Blocks.CHEST);
		PlantBlockEntity scale = weigher ? plant(helper, Machine.BELT_WEIGHER, new BlockPos(20, 5, 30)) : null;
		drive.forceRunning();
		for (PlantBlockEntity feeder : feeders) {
			feeder.forceRunning();
		}
		return new Tunnel(feeders, drive, scale);
	}

	/**
	 * p7_stockpile_live: a pile of 2,000 blocks, about 10 m high, over three feeders in a line along the tunnel. They draw until each reads NO FEED: the pile
	 * drains as cones over the openings, and what stands outside them is the dead stockpile. The live share falls out of the settle rule's
	 * draw-down table: the spec's 25-35 % with three (Tim, 2026-11-10, p7-pit-line.md #11; about 33 %, 41 % before the draw-down stood
	 * steeper than the pile).
	 */
	private static void stockpileLive(GameTestHelper helper) {
		drawDown(helper, new int[] {12, 18, 24}, 0.25, 0.35);
	}

	/**
	 * p7_stockpile_one: the same pile over one feeder under its middle: a real conical pile with one feeder is 20-30 % live; about 20 % here
	 * (26 % before the draw-down stood steeper than the pile).
	 */
	private static void stockpileOne(GameTestHelper helper) {
		drawDown(helper, new int[] {18}, 0.17, 0.30);
	}

	/** Builds a 2,000 block pile over feeders at {@code feederZ} and draws it until each reads NO FEED; the live share must be in range. */
	private static void drawDown(GameTestHelper helper, int[] feederZ, double low, double high) {
		Tunnel t = tunnel(helper, feederZ, false, true);
		t.drive().fastCycle(20);
		for (PlantBlockEntity feeder : t.feeders()) {
			feeder.fastCycle(20);
		}
		BlockPos tip = helper.absolutePos(new BlockPos(20, 30, 18));
		int pile = 0;
		for (int i = 0; i < 2000; i++) {
			if (dev.wildcatter.tailings.pit.Loose.muck(helper.getLevel(), tip, TailingsBlocks.STOCKPILE_ROCK.defaultBlockState(), Map.of("minecraft:granite", 3)) != null) {
				pile++;
			}
		}
		int before = count(helper, TailingsBlocks.STOCKPILE_ROCK, 0, 40, 8, 30, 0, 40);
		helper.assertTrue(before == pile && pile == 2000, Component.literal("2,000 blocks stand in the pile, got " + before));
		int[] quiet = {0};
		helper.succeedWhen(() -> {
			boolean drained = true;
			for (PlantBlockEntity feeder : t.feeders()) {
				drained &= feeder.status().startsWith("NO FEED");
			}
			quiet[0] = drained ? quiet[0] + 1 : 0;
			helper.assertTrue(quiet[0] > 100, Component.literal("the feeders draw until NO FEED" + describe(t.feeders())));
			int after = count(helper, TailingsBlocks.STOCKPILE_ROCK, 0, 40, 8, 30, 0, 40);
			double drawn = 0;
			for (PlantBlockEntity feeder : t.feeders()) {
				drawn += feeder.pit().drawnKg();
			}
			double live = (before - after) / (double) before;
			System.out.printf("p7 draw-down with %d feeders: %d of %d blocks drawn, %.1f %% live%n", feederZ.length, before - after, before, live * 100);
			helper.assertTrue(Math.abs(drawn - (before - after) * PitLine.STOCKPILE_BLOCK_KG) < PitLine.STOCKPILE_BLOCK_KG * 3.5,
					Component.literal("the feeders drew what left the pile: " + drawn + " kg, " + (before - after) + " blocks"));
			helper.assertTrue(live >= low && live <= high, Component.literal(low * 100 + " to " + high * 100 + " % of the pile is live, got " + live + " (" + after + " of " + before + " left)"));
		});
	}

	/**
	 * p7_reclaim_rate: two feeders feeding the belt past a belt weigher at its default 1,000 t/h each run at 500 t/h, and the weigher
	 * reads 1,000 t/h; with one feeder out of rock the other runs at its own 500 t/h and the weigher reads 500.
	 */
	private static void reclaimRate(GameTestHelper helper) {
		Tunnel t = tunnel(helper, new int[] {12, 18}, true, false);
		boolean[] both = {true};
		helper.onEachTick(() -> {
			for (int i = 0; i < t.feeders().length; i++) {
				PlantBlockEntity feeder = t.feeders()[i];
				if ((i == 0 || both[0]) && feeder.hold().total() < 2 * PitLine.STOCKPILE_BLOCK_KG - 100) {
					feeder.hold().add(Rock.COPPER, SizeClass.CRUSHED, 100);
				}
			}
		});
		helper.startSequence()
				.thenExecuteAfter(200, () -> {
					double rate = t.weigher().pit().rateTph();
					helper.assertTrue(Math.abs(rate - 1000) < 15, Component.literal("the weigher reads 1,000 t/h, got " + rate + describe(t.weigher())));
					for (PlantBlockEntity feeder : t.feeders()) {
						helper.assertTrue(Math.abs(feeder.feederTph() - 500) < 5, Component.literal("each feeder runs at 500 t/h" + describe(feeder)));
					}
					double ma = t.weigher().readMa(0);
					helper.assertTrue(Math.abs(ma - (4 + 16 * 1000 / PitLine.WEIGHER_SPAN_TPH)) < 0.2, Component.literal("RATE on the PLC reads 12 mA, got " + ma));
					both[0] = false;
					t.feeders()[1].hold().clear();
				})
				.thenExecuteAfter(300, () -> {
					double rate = t.weigher().pit().rateTph();
					helper.assertTrue(Math.abs(rate - 500) < 15, Component.literal("with one feeder it reads 500 t/h, got " + rate + describe(t.weigher(), t.feeders()[0], t.feeders()[1])));
					helper.assertTrue(t.feeders()[1].status().startsWith("NO FEED"), Component.literal("the other is out of rock: " + t.feeders()[1].status()));
					t.weigher().stepSetpoint();
					helper.assertTrue(Math.abs(t.weigher().setpointTph() - 1250) < 1e-6, Component.literal("a sneaking click steps the setpoint to 1,250 t/h"));
					helper.assertTrue(t.weigher().pit().totalKg() > 0 && t.weigher().points().size() == 3, Component.literal("RATE, SETPOINT and TOTAL on the PLC"));
				})
				.thenSucceed();
	}

	/**
	 * p7_heavy_belt: a flat 150 m heavy run takes 375 kW, so two 355 kW drives; with one it reads NO DRIVE, with two it carries its load
	 * to the chest at its head end. A run climbing two metres in three is too steep for crushed rock.
	 */
	private static void heavyBelt(GameTestHelper helper) {
		heavyRun(helper, 4, 5, 0, 149);
		PlantBlockEntity head = plant(helper, Machine.HEAVY_CONVEYOR_DRIVE, new BlockPos(2, 5, 147));
		helper.setBlock(new BlockPos(4, 5, 150), Blocks.CHEST);
		head.forceRunning();
		head.receive(crushed(Rock.GRANITE, 5400));
		// a steep run: up 1 at z 2, and again at z 3
		helper.setBlock(new BlockPos(10, 5, 0), heavy(Direction.Axis.Z));
		helper.setBlock(new BlockPos(10, 5, 1), heavy(Direction.Axis.Z));
		helper.setBlock(new BlockPos(10, 6, 2), heavy(Direction.Axis.Z));
		helper.setBlock(new BlockPos(10, 7, 3), heavy(Direction.Axis.Z));
		helper.setBlock(new BlockPos(10, 7, 4), heavy(Direction.Axis.Z));
		PlantBlockEntity steep = plant(helper, Machine.HEAVY_CONVEYOR_DRIVE, new BlockPos(11, 7, 2));
		steep.forceRunning();
		helper.startSequence()
				.thenExecuteAfter(40, () -> {
					helper.assertTrue(head.status().startsWith("NO DRIVE") && head.status().contains("needs 2 drives"),
							Component.literal("one drive cannot move 150 m: " + head.status()));
					helper.assertTrue(steep.status().startsWith("TOO STEEP"), Component.literal("2 in 3 is too steep: " + steep.status()));
					PlantBlockEntity second = plant(helper, Machine.HEAVY_CONVEYOR_DRIVE, new BlockPos(5, 5, 60));
					second.forceRunning();
				})
				.thenWaitUntil(() -> {
					net.minecraft.world.Container chest = (net.minecraft.world.Container) helper.getBlockEntity(new BlockPos(4, 5, 150), net.minecraft.world.level.block.entity.ChestBlockEntity.class);
					int crushed = 0;
					for (int i = 0; i < chest.getContainerSize(); i++) {
						crushed += chest.getItem(i).getCount();
					}
					helper.assertTrue(crushed == 2, Component.literal("two drives carry the 5.4 t to the chest at its head end: " + crushed + "; " + head.status()));
				})
				.thenSucceed();
	}

	/**
	 * p7_primary_line: the surge pocket's crushed rock goes out by the primary apron feeder onto a heavy belt rising 1 in 4 to the
	 * stockpile head, which drops it onto the ground as stockpile blocks.
	 */
	private static void primaryLine(GameTestHelper helper) {
		Primary p = primary(helper);
		fill(helper, Blocks.STONE, 0, 30, 3, 3, 15, 60);
		// the belt from under the feeder's head end (z 14) at y 7, up 1 in 4 to y 13, then on to the head
		int y = 7;
		for (int z = 14; z <= 40; z++) {
			if (z > 16 && z <= 40 && (z - 16) % 4 == 0 && y < 13) {
				y++;
			}
			helper.setBlock(new BlockPos(6, y, z), heavy(Direction.Axis.Z));
		}
		final int topY = y;
		PlantBlockEntity drive = plant(helper, Machine.HEAVY_CONVEYOR_DRIVE, new BlockPos(7, topY, 38));
		PlantBlockEntity head = plant(helper, Machine.STOCKPILE_HEAD, new BlockPos(5, topY - 4, 41));
		p.surge().receive(crushed(Rock.COPPER, 30_000));
		p.feeder().forceRunning();
		drive.forceRunning();
		helper.succeedWhen(() -> {
			int pile = count(helper, TailingsBlocks.STOCKPILE_ROCK, 0, 30, 4, 20, 30, 60);
			helper.assertTrue(pile >= 10, Component.literal("the head stacks the feeder's rock: " + pile + " blocks" + describe(p.feeder(), drive, head)));
			helper.assertTrue(head.pit().stackedKg() >= 10 * PitLine.STOCKPILE_BLOCK_KG, Component.literal("it counts what it stacked"));
		});
	}

	/** p7a_parts: every P7a part is its spec size and one item; the gyratory and the weigher have their PLC points. */
	private static void parts(GameTestHelper helper) {
		Machine[] machines = {Machine.GYRATORY_CRUSHER, Machine.ROCK_BREAKER, Machine.SURGE_POCKET, Machine.PRIMARY_APRON_FEEDER, Machine.HEAVY_CONVEYOR_DRIVE,
				Machine.STOCKPILE_HEAD, Machine.RECLAIM_FEEDER, Machine.BELT_WEIGHER};
		int x = 0;
		for (Machine machine : machines) {
			BlockPos pos = new BlockPos(x, 2, 0);
			plant(helper, machine, pos);
			int cells = 0;
			BlockState state = helper.getLevel().getBlockState(helper.absolutePos(pos));
			for (BlockPos cell : BigParts.cells(helper.absolutePos(pos), state)) {
				if (helper.getLevel().getBlockState(cell).getBlock() == TailingsBlocks.plantBlock(machine)) {
					cells++;
				}
			}
			helper.assertTrue(cells == machine.cells(), Component.literal(machine + " should fill " + machine.cells() + " cells, got " + cells));
			x += machine.width() + 1;
		}
		pocket(helper, new BlockPos(x, 2, 0));
		helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(x + 13, 9, 9))).getBlock() == TailingsBlocks.GYRATORY_POCKET,
				Component.literal("the pocket fills its 14 x 8 x 10 cells"));
		PlantBlockEntity crusher = helper.getBlockEntity(new BlockPos(0, 2, 0), PlantBlockEntity.class);
		helper.assertTrue(crusher.points().stream().anyMatch(point -> point.name().equals("BRIDGED")), Component.literal("the gyratory has BRIDGED on the PLC"));
		helper.assertTrue(crusher.inspect().getString().contains("NOT BUILT") || crusher.status().equals("idle"), Component.literal("alone it is not built: " + crusher.status()));
		helper.assertTrue(Machine.GYRATORY_CRUSHER.supply() == Machine.Supply.MEDIUM, Component.literal("the gyratory's motor is on the medium-voltage tier"));
		helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(new BlockPos(0, 2, 0))).getValue(BigPartBlock.FACING) == Direction.NORTH, Component.literal("facing north"));
		helper.succeed();
	}

	// ---- P7b: the SAG mill line ---------------------------------------------------------------------------------------------------

	/** The SAG line: the SAG at the origin, its pump box touching its south end, the cluster beside the large ball mill, piped. */
	record Grinding(PlantBlockEntity sag, PlantBlockEntity pump, PlantBlockEntity cluster, PlantBlockEntity ball, PlantBlockEntity pebbles, PlantBlockEntity pebbleDrive,
			PlantBlockEntity returnDrive) {
		PlantBlockEntity[] all() {
			return new PlantBlockEntity[] {sag, pump, cluster, ball, pebbles, pebbleDrive, returnDrive};
		}

		/** Water off the pipes, as the plant's pumps keep the SAG's and the pump box's levels. */
		void water() {
			sag.addWater(100_000);
			pump.addWater(100_000);
		}

		/** The overflow drawn off, as flotation would; true if what it held carried an assay. */
		boolean drain() {
			boolean assayed = cluster.out().held(dev.wildcatter.fluid.Product.FINES_SLURRY) > 0 && !cluster.out().assayOf(dev.wildcatter.fluid.Product.FINES_SLURRY).isEmpty();
			cluster.out().give(dev.wildcatter.fluid.Product.FINES_SLURRY, Double.MAX_VALUE);
			return assayed;
		}
	}

	static Grinding grinding(GameTestHelper helper) {
		PlantBlockEntity sag = plant(helper, Machine.SAG_MILL, new BlockPos(0, 5, 0));
		PlantBlockEntity pump = plant(helper, Machine.CYCLONE_FEED_PUMP, new BlockPos(3, 5, 14));
		PlantBlockEntity cluster = plant(helper, Machine.CYCLONE_CLUSTER_LARGE, new BlockPos(4, 5, 22));
		PlantBlockEntity ball = plant(helper, Machine.BALL_MILL_LARGE, new BlockPos(10, 5, 20));
		// the cyclone feed: the pump's south face to the cluster's north face
		TailingsGameTests.pipe(helper, new BlockPos(4, 5, 19), new BlockPos(4, 5, 20), new BlockPos(4, 5, 21));
		// the ball mill's discharge: out under its south end, back under the cluster to the pump's east side
		TailingsGameTests.pipe(helper, new BlockPos(10, 5, 36), new BlockPos(10, 4, 36), new BlockPos(9, 4, 36), new BlockPos(8, 4, 36), new BlockPos(7, 4, 36));
		for (int z = 35; z >= 16; z--) {
			TailingsGameTests.pipe(helper, new BlockPos(7, 4, z));
		}
		TailingsGameTests.pipe(helper, new BlockPos(7, 5, 16));
		// the pebble belt off the SAG's east face to the pebble crusher, and its return belt to a chest
		for (int x = 11; x <= 16; x++) {
			helper.setBlock(new BlockPos(x, 5, 10), heavy(Direction.Axis.X));
		}
		PlantBlockEntity pebbleDrive = plant(helper, Machine.HEAVY_CONVEYOR_DRIVE, new BlockPos(15, 6, 8));
		PlantBlockEntity pebbles = plant(helper, Machine.PEBBLE_CRUSHER, new BlockPos(17, 0, 9));
		heavyRun(helper, 18, -1, 12, 18);
		PlantBlockEntity returnDrive = plant(helper, Machine.HEAVY_CONVEYOR_DRIVE, new BlockPos(19, -1, 16));
		helper.setBlock(new BlockPos(18, -1, 19), Blocks.CHEST);
		Grinding g = new Grinding(sag, pump, cluster, ball, pebbles, pebbleDrive, returnDrive);
		for (PlantBlockEntity part : g.all()) {
			part.forceRunning();
		}
		g.water();
		return g;
	}

	/**
	 * p7_sag_grind: the SAG at its normal charge of copper ore grinds at 1,000 t/h; its undersize goes through the pump box, the cluster
	 * and the large ball mill, and the cluster's overflow is fines slurry carrying the ore's assay; 20 % of what it grinds goes out as
	 * pebbles to the pebble crusher, which sends them on crushed to gravel.
	 */
	private static void sagGrind(GameTestHelper helper) {
		Grinding g = grinding(helper);
		g.sag().receive(crushed(Rock.COPPER, PitLine.SAG_NORMAL_CHARGE_KG));
		double[] added = {0};
		boolean[] assayed = {false};
		helper.onEachTick(() -> {
			g.water();
			assayed[0] |= g.drain();
			double short_ = PitLine.SAG_NORMAL_CHARGE_KG - g.sag().hold().total();
			if (short_ > 0) {
				g.sag().receive(crushed(Rock.COPPER, short_));
				added[0] += short_;
			}
		});
		long[] at = {0};
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(g.cluster().pit().totalKg() > 100,
						Component.literal("the cluster should classify the SAG's undersize" + describe(g.all()))))
				.thenExecute(() -> {
					helper.assertTrue(assayed[0], Component.literal("its overflow is fines slurry carrying the copper ore's assay"));
					helper.assertTrue(g.ball().status().startsWith("grinding the underflow"), Component.literal("the ball mill grinds the underflow: " + g.ball().status()));
					added[0] = 0;
					at[0] = helper.getLevel().getGameTime();
				})
				.thenExecuteAfter(100, () -> {
					double tph = added[0] / (helper.getLevel().getGameTime() - at[0]) * 20 * 3.6;
					helper.assertTrue(Math.abs(tph - PitLine.TONNES_PER_HOUR) < 20, Component.literal("the SAG grinds 1,000 t/h at its normal charge, got " + tph + describe(g.sag())));
					helper.assertTrue(g.pebbles().status().startsWith("crushing pebbles"), Component.literal("the pebble crusher crushes the pebbles: " + g.pebbles().status()));
					helper.assertTrue(g.returnDrive().hold().kg(Rock.COPPER, SizeClass.GRAVEL) > 0, Component.literal("its gravel goes out on its belt" + describe(g.returnDrive())));
					helper.assertTrue(!g.sag().status().contains("slower"), Component.literal("with the pebble crusher crushing it runs at full rate: " + g.sag().status()));
				})
				.thenSucceed();
	}

	/**
	 * p7_sag_overload: fed past its 35 % filling the SAG reads OVERLOAD, and the belt weigher on its feed belt holds its setpoint 10 %
	 * back, 900 t/h; with the charge back down it comes back to 1,000.
	 */
	private static void sagOverload(GameTestHelper helper) {
		Grinding g = grinding(helper);
		for (int z = -6; z <= 1; z++) {
			helper.setBlock(new BlockPos(5, 16, z), heavy(Direction.Axis.Z));
		}
		PlantBlockEntity feedDrive = plant(helper, Machine.HEAVY_CONVEYOR_DRIVE, new BlockPos(6, 16, -1));
		PlantBlockEntity weigher = plant(helper, Machine.BELT_WEIGHER, new BlockPos(3, 16, -5));
		feedDrive.forceRunning();
		g.sag().receive(crushed(Rock.GRANITE, PitLine.SAG_OVERLOAD_KG + 5000));
		helper.onEachTick(() -> {
			g.water();
			g.drain();
		});
		helper.startSequence()
				.thenWaitUntil(() -> {
					helper.assertTrue(g.sag().status().startsWith("OVERLOAD"), Component.literal("the SAG reads OVERLOAD: " + g.sag().status()));
					helper.assertTrue(Math.abs(weigher.setpointTph() - 900) < 1e-6, Component.literal("the weigher holds 10 % back: " + weigher.setpointTph() + describe(weigher)));
				})
				.thenExecute(() -> {
					double ma = g.sag().readMa(6);
					helper.assertTrue(ma > 4 + 16 * PitLine.SAG_OVERLOAD_KG / PitLine.SAG_MOST_CHARGE_KG - 0.1, Component.literal("WEIGHT on the PLC reads its charge, " + ma + " mA"));
					g.sag().hold().take(g.sag().hold().total() - 60_000);
				})
				.thenWaitUntil(() -> helper.assertTrue(Math.abs(weigher.setpointTph() - 1000) < 1e-6 && !g.sag().status().startsWith("OVERLOAD"),
						Component.literal("cleared, the setpoint comes back: " + weigher.setpointTph() + "; " + g.sag().status())))
				.thenSucceed();
	}

	/** p7_pebble_off: with the pebble crusher's motor off the pebbles go round it uncrushed and the SAG grinds 15 % slower. */
	private static void pebbleOff(GameTestHelper helper) {
		Grinding g = grinding(helper);
		g.pebbles().stopForced();
		g.sag().receive(crushed(Rock.GRANITE, PitLine.SAG_NORMAL_CHARGE_KG));
		double[] added = {0};
		helper.onEachTick(() -> {
			g.water();
			g.drain();
			double short_ = PitLine.SAG_NORMAL_CHARGE_KG - g.sag().hold().total((r, sz) -> true);
			if (short_ > 0) {
				g.sag().receive(crushed(Rock.GRANITE, short_));
				added[0] += short_;
			}
		});
		long[] at = {0};
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(g.pebbles().status().startsWith("BYPASSED"), Component.literal("the pebbles go round it: " + g.pebbles().status())))
				.thenExecute(() -> {
					added[0] = 0;
					at[0] = helper.getLevel().getGameTime();
				})
				.thenExecuteAfter(100, () -> {
					double tph = added[0] / (helper.getLevel().getGameTime() - at[0]) * 20 * 3.6;
					helper.assertTrue(Math.abs(tph - 850) < 20, Component.literal("the SAG grinds 15 % slower, 850 t/h, got " + tph + describe(g.sag())));
					helper.assertTrue(g.sag().status().contains("15 % slower"), Component.literal("and says why: " + g.sag().status()));
					helper.assertTrue(g.returnDrive().hold().kg(Rock.GRANITE, SizeClass.CRUSHED) > 0 && g.returnDrive().hold().kg(Rock.GRANITE, SizeClass.GRAVEL) == 0,
							Component.literal("its pebbles go back uncrushed" + describe(g.returnDrive())));
				})
				.thenSucceed();
	}

	/**
	 * p7_cyclones: 1,000 kg of the SAG's undersize through the pump box and the cluster, with the ball mill stopped: 286 kg of solids
	 * (1 in 3.5) to the overflow at 35 % solids, 816 kg of fines slurry, and 714 kg of underflow into the ball mill; the pump box adds
	 * the water to make the 54 % cyclone feed, and the cluster's PRESSURE reads on the PLC.
	 */
	private static void cyclones(GameTestHelper helper) {
		Grinding g = grinding(helper);
		g.ball().stopForced();
		RockLoad under = new RockLoad();
		under.add(Rock.COPPER, SizeClass.SAND, 1000);
		g.pump().back().offer(dev.wildcatter.fluid.Product.MILL_DISCHARGE, 1000, dev.wildcatter.tailings.plant.Assays.dry(under));
		double water = g.pump().water();
		double[] pressure = {0};
		helper.onEachTick(() -> pressure[0] = Math.max(pressure[0], g.cluster().readMa(0)));
		helper.succeedWhen(() -> {
			double fines = g.cluster().out().held(dev.wildcatter.fluid.Product.FINES_SLURRY);
			double back = g.ball().back().held(dev.wildcatter.fluid.Product.MILL_DISCHARGE);
			helper.assertTrue(Math.abs(fines - 1000 / 3.5 / 0.35) < 1, Component.literal("816 kg of fines slurry at 35 %, got " + fines + describe(g.pump(), g.cluster())));
			helper.assertTrue(Math.abs(back - 1000 * 2.5 / 3.5) < 1, Component.literal("714 kg of underflow into the ball mill, got " + back));
			double used = water - g.pump().water();
			helper.assertTrue(Math.abs(used - 1000 * PitLine.pumpWaterPerKg(PitLine.SAG_SOLIDS)) < 1, Component.literal("the pump box adds " + 1000 * PitLine.pumpWaterPerKg(PitLine.SAG_SOLIDS) + " L, used " + used));
			helper.assertTrue(pressure[0] > 4.1, Component.literal("the cluster's PRESSURE reads while it classifies, got " + pressure[0] + " mA"));
			helper.assertTrue(g.cluster().points().size() == 1, Component.literal("the cluster has its PRESSURE point"));
		});
	}

	/** p7b_parts: the P7b parts are their spec sizes; each big mill has two motor boxes on its west face, at its main cell and 4 m south. */
	private static void partsB(GameTestHelper helper) {
		Machine[] machines = {Machine.SAG_MILL, Machine.PEBBLE_CRUSHER, Machine.BALL_MILL_LARGE, Machine.CYCLONE_FEED_PUMP, Machine.CYCLONE_CLUSTER_LARGE};
		int x = 0;
		for (Machine machine : machines) {
			BlockPos pos = new BlockPos(x, 2, 0);
			PlantBlockEntity part = plant(helper, machine, pos);
			int cells = 0;
			BlockState state = helper.getLevel().getBlockState(helper.absolutePos(pos));
			for (BlockPos cell : BigParts.cells(helper.absolutePos(pos), state)) {
				if (helper.getLevel().getBlockState(cell).getBlock() == TailingsBlocks.plantBlock(machine)) {
					cells++;
				}
			}
			helper.assertTrue(cells == machine.cells(), Component.literal(machine + " should fill " + machine.cells() + " cells, got " + cells));
			if (machine.motors() == 2) {
				BlockPos main = helper.absolutePos(pos);
				Direction box = dev.wildcatter.tailings.block.TailingsPartBlock.boxFace(state);
				var voltage = dev.wildcatter.energy.Voltage.MEDIUM;
				helper.assertTrue(part.hasPortAt(main, box, dev.wildcatter.circuit.Conductor.A, voltage), Component.literal(machine + ": the first pinion's box at its main cell"));
				helper.assertTrue(part.hasPortAt(main.south(4), box, dev.wildcatter.circuit.Conductor.A, voltage), Component.literal(machine + ": the second pinion's box 4 m south"));
				helper.assertTrue(!part.hasPortAt(main.south(2), box, dev.wildcatter.circuit.Conductor.A, voltage), Component.literal(machine + ": no box between them"));
				helper.assertTrue(part.points().stream().anyMatch(p -> p.name().startsWith("WEIGHT")), Component.literal(machine + " has WEIGHT on the PLC"));
				helper.assertTrue(part.inspect().getString().contains("2 x "), Component.literal(machine + " reads its two motors: " + part.inspect().getString()));
			}
			x += machine.width() + 1;
		}
		helper.succeed();
	}
}
