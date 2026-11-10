package dev.wildcatter.tailings.neoforge.gametest;

import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.floor;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.part;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.DraglineBlockEntity;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.entity.DraglineEntity;
import dev.wildcatter.tailings.item.TrailingCableItem;
import dev.wildcatter.tailings.pit.Dragline;
import dev.wildcatter.tailings.pit.DraglineLayout;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * The walking dragline's scenes (docs/multiblocks/dragline.md, P6): built and checked from its parts, lifted into one machine and set
 * down again; one cycle; a strip uncovered to its seam; walking on and DONE at the strip's end; hard overburden; its PLC points.
 *
 * <p>The strip scenes stand the lifted machine on a stone bench, its tub's floor at y 10, facing south (+z), casting to its left (+x):
 * the strip runs from x 33 (its walk line at x 20.5 plus 12.5 m) and from z 30, and the spoil goes beyond it on the bench.
 */
final class DraglineGameTests {
	static final String[] SCENES = {"dragline_built", "dragline_cycle", "dragline_strip", "dragline_walk", "dragline_hard", "dragline_plc"};
	static final Map<String, Integer> TICKS = Map.of("dragline_built", 600, "dragline_cycle", 1200, "dragline_strip", 4000, "dragline_walk", 2400,
			"dragline_hard", 1600, "dragline_plc", 1600);
	static final Map<String, Integer> PADDING = Map.of("dragline_built", 160, "dragline_cycle", 160, "dragline_strip", 160, "dragline_walk", 160,
			"dragline_hard", 160, "dragline_plc", 160);

	private static final int BENCH = 10;
	private static final double CX = 20.5, CZ = 30.5;

	private DraglineGameTests() {
	}

	static int[] reach(String scene) {
		return switch (scene) {
			case "dragline_built" -> new int[] {-1, 3, -1, 7};
			case "dragline_walk" -> new int[] {-1, 6, -1, 7};
			case "dragline_cycle", "dragline_strip", "dragline_hard", "dragline_plc" -> new int[] {-1, 6, -1, 5};
			default -> new int[] {-1, 2, -1, 2};
		};
	}

	// ---- building ---------------------------------------------------------------------------------------------------------------

	/** The dragline's parts, facing north, the tub's main cell at {@code at}, all but {@code leaveOut}. */
	static DraglineBlockEntity parts(GameTestHelper helper, BlockPos at, DraglineLayout.Part leaveOut) {
		for (DraglineLayout.Part part : DraglineLayout.Part.values()) {
			if (part != leaveOut) {
				int[] o = part.offset();
				part(helper, TailingsBlocks.plantBlock(part.machine()), at.offset(o[0], o[1], o[2]), null);
			}
		}
		return helper.getBlockEntity(at, DraglineBlockEntity.class);
	}

	/** A stone bench, its top at y 9, over {@code x1} by {@code z1}. */
	private static void bench(GameTestHelper helper, int x1, int z1) {
		floor(helper, 0, x1, 1, BENCH - 1, 0, z1);
	}

	/** The strip: {@code top} layers of {@code over} down from the bench's top, over {@code seam} layers of bauxite. */
	private static void strip(GameTestHelper helper, int width, int length, int top, Block over, int seam) {
		for (int x = 33; x < 33 + width; x++) {
			for (int z = 30; z < 30 + length; z++) {
				for (int k = 0; k < top; k++) {
					helper.setBlock(x, BENCH - 1 - k, z, over);
				}
				for (int k = 0; k < seam; k++) {
					helper.setBlock(x, BENCH - 1 - top - k, z, bauxite());
				}
			}
		}
	}

	private static Block bauxite() {
		return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.parse("wildcatter:bauxite_ore"));
	}

	/** A switch house behind it on the bench, fed by the test's ideal supply. */
	private static PitSwitchHouseBlockEntity house(GameTestHelper helper) {
		return PitBigGameTests.house(helper, new BlockPos(4, BENCH, 60));
	}

	/** The dragline lifted and on outlet 1 of {@code house}, its job set, running its cycles {@code fast} times faster. */
	private static DraglineEntity dragline(GameTestHelper helper, PitSwitchHouseBlockEntity house, int width, int length, int seam, int fast) {
		DraglineEntity d = TailingsEntities.DRAGLINE.create(helper.getLevel(), EntitySpawnReason.COMMAND);
		BlockPos origin = helper.absolutePos(BlockPos.ZERO), at = helper.absolutePos(new BlockPos(0, BENCH, 0));
		d.setPos(origin.getX() + CX, at.getY(), origin.getZ() + CZ);
		d.setYRot(0);
		d.job().set(0, width);
		d.job().set(1, length);
		d.job().set(2, seam);
		d.job().set(3, 0);   // casting to its left: +x, facing +z
		d.fastCycles(fast);
		helper.getLevel().addFreshEntity(d);
		String why = PitBigGameTests.couple(helper, house, d);
		helper.assertTrue(why == null, Component.literal("the dragline should couple: " + why));
		d.start();   // as RUN on its tub does: its strip starts where it stands
		return d;
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

	// ---- scenes ------------------------------------------------------------------------------------------------------------------

	/**
	 * Built without its right shoe it reads NOT BUILT naming the shoe; with it, NO CABLE. Coupled, RUN lifts it into one machine by the
	 * tub's name, its parts' cells air; STOP sets its parts back down where they were, its cable and job still on.
	 */
	static void built(GameTestHelper helper) {
		floor(helper, 0, 40, 1, 1, 0, 112);
		BlockPos at = new BlockPos(10, 2, 92);
		DraglineBlockEntity tub = parts(helper, at, DraglineLayout.Part.SHOE_RIGHT);
		String said = tub.run(helper.getLevel());
		helper.assertTrue(said.startsWith("NOT BUILT") && said.contains("right Dragline Walking Shoe"), Component.literal("it should name the missing shoe: " + said));
		int[] o = DraglineLayout.Part.SHOE_RIGHT.offset();
		part(helper, TailingsBlocks.DRAGLINE_SHOE, at.offset(o[0], o[1], o[2]), null);
		helper.assertTrue(tub.check() == null, Component.literal("built: " + tub.check()));
		said = tub.run(helper.getLevel());
		helper.assertTrue(said.startsWith("NO CABLE"), Component.literal("no cable, no lift: " + said));
		PitSwitchHouseBlockEntity house = PitBigGameTests.house(helper, new BlockPos(34, 2, 104));
		ItemStack reel = new ItemStack(TailingsItems.TRAILING_CABLE);
		TrailingCableItem.pickUp(reel, house);
		long[] from = TrailingCableItem.from(reel);
		String why = tub.couple(helper.getLevel(), BlockPos.of(from[0]), (int) from[1]);
		helper.assertTrue(why == null, Component.literal("the tub should take the cable: " + why));
		tub.job().set(0, 10);
		UUID name = tub.machine();
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> {
					helper.assertTrue(house.close(0) == null, Component.literal("its outlet should close on the standing machine's cable"));
					String lifted = tub.run(helper.getLevel());
					helper.assertTrue(lifted.startsWith("lifted"), Component.literal("RUN should lift it: " + lifted));
					helper.assertTrue(helper.getLevel().getEntity(name) instanceof DraglineEntity, Component.literal("one machine, by the tub's name"));
					helper.assertTrue(helper.getBlockState(at).isAir() && helper.getBlockState(at.offset(6, 0, -76)).isAir(), Component.literal("its parts' cells are air"));
				})
				.thenIdle(5)
				.thenExecute(() -> {
					DraglineEntity d = (DraglineEntity) helper.getLevel().getEntity(name);
					String down = d.stop(helper.getLevel(), true);
					System.out.printf(Locale.ROOT, "[tailings-test] dragline_built: lifted and %s%n", down);
					helper.assertTrue(down.startsWith("set down"), Component.literal("STOP should set it down: " + down + "; " + d.status()));
					DraglineBlockEntity back = helper.getBlockEntity(at, DraglineBlockEntity.class);
					helper.assertTrue(back.check() == null && back.cable() != null && back.machine().equals(name) && back.job().width() == 10,
							Component.literal("its parts back where they were, its cable and job on: " + back.status()));
				})
				.thenSucceed();
	}

	/**
	 * One cycle takes 25 blocks of sand from the top of the strip and casts them beyond it, settled on the bench; its outlet carries
	 * 6 MW while the bucket fills and 1.2 MW the rest of the cycle.
	 */
	static void cycle(GameTestHelper helper) {
		bench(helper, 90, 70);
		strip(helper, 30, 10, 5, Blocks.SAND, 2);
		PitSwitchHouseBlockEntity house = house(helper);
		DraglineEntity d = dragline(helper, house, 30, 10, 5, 4);
		int before = count(helper, Blocks.SAND, 33, 62, 5, 9, 30, 39);
		double[] most = {0}, rest = {0};
		helper.onEachTick(() -> {
			if (house.state(0) == PitSwitchHouseBlockEntity.State.CLOSED && d.isAlive()) {
				most[0] = Math.max(most[0], house.watts(0));
				if (d.phase() == Dragline.Phase.SWING_OUT && house.watts(0) > 1000) {
					rest[0] = house.watts(0);
				}
			}
		});
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> helper.assertTrue(house.close(0) == null, Component.literal("its outlet should close")))
				.thenWaitUntil(() -> helper.assertTrue(d.cycles() >= 1, Component.literal("one cycle: " + d.status())))
				.thenExecute(() -> {
					d.stop(helper.getLevel(), false);
					int dug = before - count(helper, Blocks.SAND, 33, 62, 5, 9, 30, 39);
					int cast = count(helper, Blocks.SAND, 63, 90, BENCH, BENCH + 20, 0, 70);
					int floating = 0;
					for (int x = 63; x <= 90; x++) {
						for (int y = BENCH; y <= BENCH + 20; y++) {
							for (int z = 0; z <= 70; z++) {
								if (helper.getBlockState(new BlockPos(x, y, z)).is(Blocks.SAND) && helper.getBlockState(new BlockPos(x, y - 1, z)).isAir()) {
									floating++;
								}
							}
						}
					}
					System.out.printf(Locale.ROOT, "[tailings-test] dragline_cycle: dug %d blocks, cast %d beyond the strip (%d floating); outlet peak %.0f kW, the rest %.0f kW; %s%n",
							dug, cast, floating, most[0] / 1000, rest[0] / 1000, d.status());
					helper.assertTrue(dug == 25, Component.literal("a bucket is 25 bank blocks of sand: " + dug));
					helper.assertTrue(cast == 25, Component.literal("all 25 cast beyond the strip: " + cast));
					helper.assertTrue(floating == 0, Component.literal("the spoil settled: " + floating + " floating"));
					helper.assertTrue(count(helper, Blocks.SAND, 33, 62, 5, 8, 30, 39) == 1200, Component.literal("from the top layer down"));
					helper.assertTrue(Math.abs(most[0] - 6_000_000) < 600_000, Component.literal("6 MW while it fills: " + most[0]));
					helper.assertTrue(Math.abs(rest[0] - 1_200_000) < 200_000, Component.literal("1.2 MW the rest of its cycle: " + rest[0]));
				})
				.thenSucceed();
	}

	/** A 30 x 10 m strip 5 m deep over a bauxite seam is uncovered to the seam's top, the seam untouched, and it reads DONE. */
	static void stripScene(GameTestHelper helper) {
		bench(helper, 90, 70);
		strip(helper, 30, 10, 5, Blocks.SAND, 2);
		PitSwitchHouseBlockEntity house = house(helper);
		int seam = count(helper, bauxite(), 33, 62, 3, 4, 30, 39);
		DraglineEntity d = dragline(helper, house, 30, 10, 5, 55);
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> helper.assertTrue(house.close(0) == null, Component.literal("its outlet should close")))
				.thenWaitUntil(() -> helper.assertTrue(d.mode() == DraglineEntity.Mode.DONE, Component.literal("DONE: " + d.status())))
				.thenExecute(() -> {
					int left = 0;
					for (int x = 33; x <= 62; x++) {
						for (int y = 5; y <= 9; y++) {
							for (int z = 30; z <= 39; z++) {
								if (!helper.getBlockState(new BlockPos(x, y, z)).isAir()) {
									left++;
								}
							}
						}
					}
					int after = count(helper, bauxite(), 33, 62, 3, 4, 30, 39);
					System.out.printf(Locale.ROOT, "[tailings-test] dragline_strip: %d cycles, %.0f bank m3; %d blocks left over the seam; seam %d of %d; %s%n",
							d.cycles(), d.bankM3(), left, after, seam, d.status());
					helper.assertTrue(left == 0, Component.literal("the strip uncovered to the seam's top: " + left + " left"));
					helper.assertTrue(after == seam && seam == 600, Component.literal("the seam untouched: " + after + " of " + seam));
					helper.assertTrue(d.bankM3() == 1500, Component.literal("1,500 m³ of bank moved: " + d.bankM3()));
				})
				.thenSucceed();
	}

	/**
	 * A 10 m strip 70 m long, 3 m to the seam, uncovered already in its middle: it digs the near end, then with nothing left in its reach
	 * walks on 2 m at a step until the far end is in reach, digs it, and reads DONE with the whole strip uncovered.
	 */
	static void walk(GameTestHelper helper) {
		bench(helper, 90, 112);
		// along = z - 30: the near end, along 0-11, and the far end, along 64-69, 3 m of sand; between, dug to the seam
		for (int x = 33; x <= 42; x++) {
			for (int z = 30; z <= 99; z++) {
				boolean over = z <= 41 || z >= 94;
				for (int yy = BENCH - 3; yy <= BENCH - 1; yy++) {
					helper.setBlock(x, yy, z, over ? Blocks.SAND : Blocks.AIR);
				}
				helper.setBlock(x, BENCH - 4, z, bauxite());
			}
		}
		PitSwitchHouseBlockEntity house = house(helper);
		DraglineEntity d = dragline(helper, house, 10, 70, 3, 20);
		double[] firstWalk = {-1};
		helper.onEachTick(() -> {
			if (firstWalk[0] < 0 && d.walking()) {
				firstWalk[0] = d.cycles();
			}
		});
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> helper.assertTrue(house.close(0) == null, Component.literal("its outlet should close")))
				.thenWaitUntil(() -> helper.assertTrue(d.mode() == DraglineEntity.Mode.DONE, Component.literal("DONE: " + d.status())))
				.thenExecute(() -> {
					StringBuilder where = new StringBuilder();
					int left = 0;
					for (int x = 33; x <= 42; x++) {
						for (int z = 30; z <= 99; z++) {
							for (int yy = BENCH - 3; yy <= BENCH - 1; yy++) {
								if (!helper.getBlockState(new BlockPos(x, yy, z)).isAir()) {
									if (left++ < 6) {
										where.append(' ').append(x).append(',').append(yy).append(',').append(z);
									}
								}
							}
						}
					}
					System.out.printf(Locale.ROOT, "[tailings-test] dragline_walk: %d steps, walked %.1f m, %d cycles, %.0f bank m3; %d left%s; first step after %.0f cycles; %s%n",
							d.steps(), d.walked(), d.cycles(), d.bankM3(), left, where, firstWalk[0], d.status());
					helper.assertTrue(firstWalk[0] > 0, Component.literal("it digs the near end first, then walks"));
					helper.assertTrue(d.steps() >= 2 && Math.abs(d.walked() - 2.0 * d.steps()) < 0.01, Component.literal("2 m a step: " + d.walked() + " in " + d.steps()));
					helper.assertTrue(left == 0 && d.bankM3() == 540, Component.literal("the whole strip uncovered: " + left + " left," + where));
				})
				.thenSucceed();
	}

	/** Stone overburden it cannot dig: BLOCKED, blast it first; blasted (muck), it digs it. */
	static void hard(GameTestHelper helper) {
		bench(helper, 90, 70);
		PitSwitchHouseBlockEntity house = house(helper);
		DraglineEntity d = dragline(helper, house, 10, 10, 3, 10);
		helper.startSequence()
				.thenIdle(3)
				.thenExecute(() -> helper.assertTrue(house.close(0) == null, Component.literal("its outlet should close")))
				.thenWaitUntil(() -> helper.assertTrue(d.status().startsWith("BLOCKED: hard overburden"), Component.literal("BLOCKED: " + d.status())))
				.thenExecute(() -> {
					helper.assertTrue(d.cycles() == 0 && count(helper, Blocks.STONE, 33, 42, 7, 9, 30, 39) == 300, Component.literal("nothing dug from the rock"));
					// the top 2 m blasted
					PitBigGameTests.muck(helper, 33, 42, 8, 9, 30, 39);
				})
				.thenWaitUntil(() -> helper.assertTrue(count(helper, TailingsBlocks.MUCK, 33, 42, 8, 9, 30, 39) == 0 && d.status().startsWith("BLOCKED"),
						Component.literal("it digs the muck, then the rock under it blocks it: " + d.status())))
				.thenExecute(() -> System.out.printf(Locale.ROOT, "[tailings-test] dragline_hard: %d cycles of muck, %.1f bank m3; %s%n", d.cycles(), d.bankM3(), d.status()))
				.thenSucceed();
	}

	/**
	 * Its PLC points on its switch house outlet: in AUTO, RUN on keeps it digging and it answers RUNNING and a pulse a cycle; RUN off
	 * stops it at the end of its cycle; DONE when its strip is uncovered.
	 */
	static void plc(GameTestHelper helper) {
		bench(helper, 90, 70);
		strip(helper, 10, 10, 2, Blocks.SAND, 1);
		PitSwitchHouseBlockEntity house = house(helper);
		DraglineEntity d = dragline(helper, house, 10, 10, 2, 20);
		int base = PitSwitchHouseBlockEntity.MACHINE_POINTS;   // outlet 1's machine: RUN, RUNNING, DONE, CYCLES, POWER
		boolean[] run = {true};
		helper.onEachTick(() -> {
			long now = helper.getLevel().getGameTime();
			house.commandPoint(0, true, now);   // outlet 1 CLOSE
			house.commandPoint(base, run[0], now);
		});
		house.cycleHoa(null);
		house.cycleHoa(null);   // HAND, OFF, AUTO
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(house.readPulses(base + 3) >= 2, Component.literal("a pulse a cycle: " + house.readPulses(base + 3) + "; " + d.status())))
				.thenExecute(() -> {
					helper.assertTrue(house.readPoint(base + 1), Component.literal("RUNNING"));
					helper.assertTrue(house.readMa(base + 4) > 4.5, Component.literal("its POWER loop: " + house.readMa(base + 4)));
					run[0] = false;
				})
				.thenWaitUntil(() -> helper.assertTrue(d.mode() == DraglineEntity.Mode.STOPPED, Component.literal("RUN off stops it: " + d.status())))
				.thenExecute(() -> {
					helper.assertTrue(!house.readPoint(base + 1), Component.literal("not RUNNING"));
					run[0] = true;
				})
				.thenWaitUntil(() -> helper.assertTrue(house.readPoint(base + 2), Component.literal("DONE: " + d.status())))
				.thenExecute(() -> System.out.printf(Locale.ROOT, "[tailings-test] dragline_plc: %d cycle pulses; DONE; %s%n", house.readPulses(base + 3), d.status()))
				.thenSucceed();
	}
}
