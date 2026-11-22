package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.block.GensetControlPanelBlockEntity;
import dev.wildcatter.block.GensetSkid;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.TailingsPartBlock;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.Smelter;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * smelter_line (docs/copper-smelter.md, "Game tests"): thickened concentrate carrying 1,200 kg of copper on a pressure filter's inlet,
 * through the concentrate dryer, the flash furnace, the Peirce-Smith converter, the anode furnace and the casting wheel; the anodes
 * carried to the tankhouse as its crane would; cathodes out, and one melted to two copper ingots in a blast furnace. Every motor is on
 * Wildcatter generator sets: the four low-voltage parts on one 1 MW gas set by busway, the converter and the tankhouse each on their own
 * set stepped up to medium voltage through a unit substation run backward. Both furnaces' off-gas is counted at their outlets (smelter_acid pipes it to a real acid plant). The
 * copper balances: cathodes, anodes, what each part holds and the slag's copper make the 1,200 kg fed.
 */
final class SmelterLineGameTests {
	static final String[] SCENES = {"smelter_line"};
	private static final int FAST = 100;
	private static final double COPPER = 1200;
	private static final String BUS = "wildcatter:busway_industrial", MV = "wildcatter:cable_mv";

	private SmelterLineGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("smelter_line", helper -> {
			ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
			for (int dx = -2; dx <= 4; dx++) {
				for (int dz = -2; dz <= 6; dz++) {
					helper.getLevel().setChunkForced(origin.x() + dx, origin.z() + dz, true);
				}
			}
			line(helper);
		});
	}

	static int ticks(String scene, int otherwise) {
		return scene.equals("smelter_line") ? 8000 : otherwise;
	}

	static int padding(String scene, int otherwise) {
		return scene.equals("smelter_line") ? 120 : otherwise;
	}

	private static PlantBlockEntity part(GameTestHelper helper, Machine machine, BlockPos at) {
		PlantBlockEntity p = TailingsGameTests.plant(helper, machine, at);
		p.fastCycle(FAST);
		return p;
	}

	private static Block wc(String id) {
		return BuiltInRegistries.BLOCK.getValue(Identifier.parse(id));
	}

	/** A 1 MW gas set at {@code panel} (relative), its skid west of it, on a stone floor; its absolute panel. */
	private static BlockPos set(GameTestHelper helper, BlockPos panel) {
		for (int x = -9; x <= 6; x++) {
			for (int z = -5; z <= 9; z++) {
				helper.setBlock(panel.offset(x, -1, z), Blocks.STONE);
			}
		}
		BlockPos abs = helper.absolutePos(panel);
		GensetSkid.build(helper.getLevel(), abs, Direction.NORTH, true);
		return abs;
	}

	/** A unit substation run backward east of a set's panel: the industrial voltage in, the medium voltage out. Its MV cell, absolute. */
	private static BlockPos stepUp(GameTestHelper helper, BlockPos panel) {
		BlockPos main = panel.east(4);
		helper.setBlock(main, wc("wildcatter:unit_substation").defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.SOUTH));
		BigParts.grow(helper.getLevel(), helper.absolutePos(main), helper.getLevel().getBlockState(helper.absolutePos(main)));
		return helper.absolutePos(panel.east(5));
	}

	private static BlockPos box(PlantBlockEntity part) {
		return part.getBlockPos().relative(TailingsPartBlock.boxFace(part.getBlockState()));
	}

	private static void line(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		int y = 4;
		PlantBlockEntity filter = part(helper, Machine.FILTER, new BlockPos(2, y, 0));
		filter.forceRunning();   // the concentrator's filter is not this test's: its own scenes power it
		PlantBlockEntity dryer = part(helper, Machine.CONCENTRATE_DRYER, new BlockPos(2, y, 5));
		PlantBlockEntity furnace = part(helper, Machine.FLASH_FURNACE, new BlockPos(2, y, 21));
		PlantBlockEntity conv = part(helper, Machine.COPPER_CONVERTER, new BlockPos(2, y, 37));
		PlantBlockEntity af = part(helper, Machine.ANODE_FURNACE, new BlockPos(2, y, 46));
		PlantBlockEntity wheel = part(helper, Machine.ANODE_CASTING_WHEEL, new BlockPos(2, y, 55));
		PlantBlockEntity tank = part(helper, Machine.REFINERY_TANKHOUSE, new BlockPos(14, y, 40));
		tank.fastCycle(400);
		// pipeline gas to the dryer, the furnace's burners and the anode furnace, each at the east face of its front cells
		SmelterGameTests.gas(helper, new BlockPos(2, y, 5), 4);
		SmelterGameTests.gas(helper, new BlockPos(2, y, 21), 7);
		SmelterGameTests.gas(helper, new BlockPos(2, y, 46), 5);
		// the off-gas, counted at each outlet as an acid plant's converter would take it (smelter_acid pipes it into a real one: at the
		// scene's hundredfold pace one acid plant could not take it all)
		double[] offGas = {0, 0};
		for (int i = 0; i < 2; i++) {
			furnace.offer(new ItemStack(Items.SAND));
			conv.offer(new ItemStack(Items.SAND));
		}
		SmelterGameTests.concentrate(filter, COPPER);
		// a blast furnace for the cathodes, east of the tankhouse's front
		BlockPos blast = new BlockPos(26, y, 40);
		helper.setBlock(blast, Blocks.BLAST_FURNACE);
		// the power: one gas set on busway to the low-voltage parts, one stepped up for the converter, one for the tankhouse
		List<BlockPos> panels = new ArrayList<>();
		BlockPos a = set(helper, new BlockPos(-4, y, 10));
		panels.add(a);
		BlockPos b = set(helper, new BlockPos(-8, y, 32));
		panels.add(b);
		BlockPos mvB = stepUp(helper, new BlockPos(-8, y, 32));
		BlockPos c = set(helper, new BlockPos(34, y, 28));
		panels.add(c);
		BlockPos mvC = stepUp(helper, new BlockPos(34, y, 28));
		Set<BlockPos> boxes = new HashSet<>();
		for (PlantBlockEntity p : new PlantBlockEntity[] {dryer, furnace, conv, af, wheel, tank}) {
			boxes.add(box(p));
		}
		BlockPos lo = helper.absolutePos(new BlockPos(-20, y - 1, -6)), hi = helper.absolutePos(new BlockPos(45, y + 8, 70));
		wire(level, BUS, a.east(), boxes, lo, hi, box(dryer), box(furnace), box(af), box(wheel));
		wire(level, MV, mvB, boxes, lo, hi, box(conv));
		wire(level, MV, mvC, boxes, lo, hi, box(tank));
		helper.runAfterDelay(30, () -> panels.forEach(p -> ((GensetControlPanelBlockEntity) level.getBlockEntity(p)).setWantRun(true)));
		boolean[] melted = {false};
		helper.onEachTick(() -> {
			offGas[0] += furnace.out().give(Product.BURNER_GAS, Double.MAX_VALUE);
			offGas[1] += conv.out().give(Product.BURNER_GAS, Double.MAX_VALUE);
			for (BlockPos p : panels) {
				if (level.getBlockEntity(p) instanceof GensetControlPanelBlockEntity set) {
					set.setGasForTest(200);
				}
			}
			// the crane: anodes from the wheel to the tankhouse's hopper
			for (int slot : SmelterStages_OUT) {
				ItemStack s = wheel.getItem(slot);
				if (!s.isEmpty() && tank.offer(s)) {
					wheel.setChanged();
				}
			}
			// the first cathode into the blast furnace, with coal
			if (!melted[0] && tank.countIn(SmelterStages_OUT, new ItemStack(TailingsItems.COPPER_CATHODE)) > 0 && level.getBlockEntity(helper.absolutePos(blast)) instanceof Container bf) {
				tank.removeFrom(SmelterStages_OUT, new ItemStack(TailingsItems.COPPER_CATHODE), 1);
				bf.setItem(0, new ItemStack(TailingsItems.COPPER_CATHODE));
				bf.setItem(1, new ItemStack(Items.COAL, 4));
				melted[0] = true;
			}
		});
		helper.succeedWhen(() -> {
			String s = " | " + dryer.inspect().getString() + " | " + furnace.inspect().getString() + " | " + conv.inspect().getString() + " | " + af.inspect().getString() + " | "
					+ wheel.inspect().getString() + " | " + tank.inspect().getString();
			for (BlockPos p : panels) {
				GensetControlPanelBlockEntity set = (GensetControlPanelBlockEntity) level.getBlockEntity(p);
				helper.assertTrue(set.breakerClosed(), Component.literal("every set on its bus: " + set.inspect().getString()));
			}
			helper.assertTrue(wheel.smelter().madeCount() >= 2, Component.literal("two anodes cast on power" + s));
			int cathodes = tank.countIn(SmelterStages_OUT, new ItemStack(TailingsItems.COPPER_CATHODE)) + (melted[0] ? 1 : 0);
			helper.assertTrue(cathodes >= 14, Component.literal("14 cathodes from two anodes, got " + cathodes + s));
			helper.assertTrue(level.getBlockEntity(helper.absolutePos(blast)) instanceof Container bf && bf.getItem(2).is(Items.COPPER_INGOT) && bf.getItem(2).getCount() == 2,
					Component.literal("the blast furnace melts a cathode to two copper ingots"));
			double sulfur = (furnace.smelter().sulfurKg() + conv.smelter().sulfurKg()) * dev.wildcatter.fluid.AcidPlant.gasPerSulfur();
			helper.assertTrue(offGas[0] > 0 && offGas[1] > 0 && Math.abs(offGas[0] + offGas[1] - sulfur) < 1e-3,
					Component.literal("both furnaces' sulfur as off-gas: " + offGas[0] + " + " + offGas[1] + " kg, " + sulfur + " counted"));
			double held = 0;
			for (PlantBlockEntity p : new PlantBlockEntity[] {dryer, furnace, conv, af, wheel, tank}) {
				held += p.smelter().copper() + p.smelter().plated();
			}
			int anodesAround = wheel.countIn(SmelterStages_OUT, new ItemStack(TailingsItems.COPPER_ANODE));
			double accounted = held + anodesAround * Smelter.anodeKg() + cathodes * Smelter.cathodeKg() + furnace.smelter().lostKg();
			helper.assertTrue(Math.abs(accounted - COPPER) < 1e-3, Component.literal("the copper balances: " + accounted + " of " + COPPER + s));
			System.out.println("[tailings-test] smelter_line: " + cathodes + " cathodes, " + furnace.smelter().lostKg() + " kg of copper in slag, off-gas " + offGas[0] + " + " + offGas[1] + " kg" + s);
		});
	}

	private static final int[] SmelterStages_OUT = dev.wildcatter.tailings.block.stage.SmelterStages.OUTPUT_SLOTS;

	// ---- cable runs, as the client films lay them (Rtm05Scenes.route): through free cells, fewest turns, clear of other runs ----------

	private static void wire(ServerLevel level, String id, BlockPos from, Set<BlockPos> boxes, BlockPos lo, BlockPos hi, BlockPos... to) {
		Set<BlockPos> net = new HashSet<>();
		BlockPos start = from;
		for (BlockPos goal : to) {
			Set<BlockPos> goals = Set.of(goal);
			List<BlockPos> run = route(level, id, net.isEmpty() ? start : null, net, goals, boxes, lo, hi);
			for (BlockPos cell : run) {
				if (level.getBlockState(cell).getBlock() != wc(id)) {
					level.setBlock(cell, wc(id).defaultBlockState(), Block.UPDATE_ALL);
					level.updateNeighborsAt(cell, wc(id), null);
				}
			}
			net.addAll(run);
		}
	}

	private static boolean free(BlockState s) {
		return s.canBeReplaced();
	}

	private static boolean conductor(Block b) {
		return b == wc(BUS) || b == wc(MV) || b == wc("wildcatter:cable_industrial");
	}

	/** A run from {@code from} (or from any cell of {@code net}) to a goal, through free cells inside lo..hi. */
	private static List<BlockPos> route(ServerLevel level, String id, BlockPos from, Set<BlockPos> net, Set<BlockPos> goals, Set<BlockPos> boxes, BlockPos lo, BlockPos hi) {
		int nx = hi.getX() - lo.getX() + 1, ny = hi.getY() - lo.getY() + 1, nz = hi.getZ() - lo.getZ() + 1;
		int n = nx * ny * nz;
		byte[] ok = new byte[n];
		int[] dist = new int[n * 6];
		java.util.Arrays.fill(dist, Integer.MAX_VALUE);
		int[] prev = new int[n * 6];
		PriorityQueue<long[]> pq = new PriorityQueue<>((p, q) -> Long.compare(p[0], q[0]));
		Direction[] dirs = Direction.values();
		java.util.function.IntFunction<BlockPos> pos = i -> new BlockPos(lo.getX() + i % nx, lo.getY() + (i / nx) % ny, lo.getZ() + i / (nx * ny));
		java.util.function.ToIntFunction<BlockPos> idx = p -> (p.getX() - lo.getX()) + nx * ((p.getY() - lo.getY()) + ny * (p.getZ() - lo.getZ()));
		java.util.function.Predicate<BlockPos> inside = p -> p.getX() >= lo.getX() && p.getX() <= hi.getX() && p.getY() >= lo.getY() && p.getY() <= hi.getY() && p.getZ() >= lo.getZ()
				&& p.getZ() <= hi.getZ();
		Block kind = wc(id);
		java.util.function.IntPredicate passable = i -> {
			if (ok[i] == 0) {
				BlockPos p = pos.apply(i);
				boolean good = free(level.getBlockState(p)) && !(boxes.contains(p) && !goals.contains(p));
				if (good) {
					for (Direction d : dirs) {
						BlockPos q = p.relative(d);
						Block b = level.getBlockState(q).getBlock();
						if ((b == kind || conductor(b)) && !net.contains(q) && !goals.contains(q) && !q.equals(from)) {
							good = false;
							break;
						}
					}
				}
				ok[i] = (byte) (good ? 1 : 2);
			}
			return ok[i] == 1;
		};
		List<BlockPos> starts = new ArrayList<>();
		if (from != null) {
			starts.add(from);
		} else {
			starts.addAll(net);
		}
		for (BlockPos st : starts) {
			int s0 = idx.applyAsInt(st);
			for (int d = 0; d < 6; d++) {
				dist[s0 * 6 + d] = 0;
				prev[s0 * 6 + d] = -1;
				pq.add(new long[] {0, s0 * 6L + d});
			}
		}
		int found = -1;
		while (!pq.isEmpty()) {
			long[] e = pq.poll();
			int s = (int) e[1];
			if (e[0] > dist[s]) {
				continue;
			}
			int cell = s / 6, dir = s % 6;
			BlockPos p = pos.apply(cell);
			if (goals.contains(p)) {
				found = s;
				break;
			}
			for (int d = 0; d < 6; d++) {
				BlockPos q = p.relative(dirs[d]);
				if (!inside.test(q)) {
					continue;
				}
				int c2 = idx.applyAsInt(q);
				if (!goals.contains(q) && !passable.test(c2)) {
					continue;
				}
				int cost = 10 + (d == dir || prev[s] == -1 ? 0 : 40);
				int nd = dist[s] + cost;
				int s2 = c2 * 6 + d;
				if (nd < dist[s2]) {
					dist[s2] = nd;
					prev[s2] = s;
					pq.add(new long[] {nd, s2});
				}
			}
		}
		if (found < 0) {
			throw new AssertionError("no free route for " + id + " to " + goals);
		}
		ArrayDeque<BlockPos> out = new ArrayDeque<>();
		for (int s = found; s >= 0; s = prev[s]) {
			out.addFirst(pos.apply(s / 6));
		}
		return new ArrayList<>(out);
	}
}
