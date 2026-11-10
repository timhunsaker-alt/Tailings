package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.block.FillOutletBlockEntity;
import dev.wildcatter.tailings.block.PasteFillBlock;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.paste.FillVoid;
import dev.wildcatter.tailings.paste.PasteFill;
import dev.wildcatter.tailings.pit.DryStack;
import dev.wildcatter.tailings.plant.Machine;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server scenes for paste backfill (docs/multiblocks/paste-backfill.md, L5; decided on recommendation while Tim was away,
 * docs/questions/l5-paste.md): a closed void filling from the bottom up, NOT ENCLOSED, curing, a bulkhead failing, the mixer without
 * cement, the whole plant from thickened tails to placed paste, and line pressure.
 */
final class PasteGameTests {
	static final String[] SCENES = {"paste_fill_void", "paste_not_enclosed", "paste_cure", "paste_bulkhead_fail", "paste_no_cement", "paste_plant", "paste_line_pressure", "paste_pace"};
	static final Set<String> LONG = Set.of("paste_fill_void", "paste_plant");
	private static final Map<String, Integer> TICKS = Map.of("paste_plant", 2400);

	private PasteGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("paste_fill_void", PasteGameTests::fillVoid);
		out.accept("paste_not_enclosed", PasteGameTests::notEnclosed);
		out.accept("paste_cure", PasteGameTests::cure);
		out.accept("paste_bulkhead_fail", PasteGameTests::bulkheadFail);
		out.accept("paste_no_cement", PasteGameTests::noCement);
		out.accept("paste_plant", PasteGameTests::plant);
		out.accept("paste_line_pressure", PasteGameTests::linePressure);
		out.accept("paste_pace", PasteGameTests::pace);
	}

	static int ticks(String scene, int otherwise) {
		return TICKS.getOrDefault(scene, otherwise);
	}

	// ---- building ---------------------------------------------------------------------------------------------------------------

	private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					helper.setBlock(new BlockPos(x, y, z), state);
				}
			}
		}
	}

	/** A drive branch: stone round an air space x0..x1, y0..y1, z0..z1, its roof only if {@code roof}. */
	private static void branch(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, boolean roof) {
		fill(helper, x0 - 1, y0 - 1, z0 - 1, x1 + 1, roof ? y1 + 1 : y1, z1 + 1, Blocks.STONE.defaultBlockState());
		fill(helper, x0, y0, z0, x1, y1 + (roof ? 0 : 1), z1, Blocks.AIR.defaultBlockState());
	}

	private static int count(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, java.util.function.Predicate<BlockState> what) {
		int n = 0;
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					if (what.test(helper.getBlockState(new BlockPos(x, y, z)))) {
						n++;
					}
				}
			}
		}
		return n;
	}

	private static FillOutletBlockEntity outlet(GameTestHelper helper, BlockPos at) {
		helper.setBlock(at, TailingsBlocks.FILL_OUTLET);
		return helper.getBlockEntity(at, FillOutletBlockEntity.class);
	}

	private static PlantBlockEntity part(GameTestHelper helper, Machine machine, BlockPos pos) {
		return TailingsGameTests.plant(helper, machine, pos);
	}

	private static SlurryAssay thickened() {
		return SlurryAssay.of(Map.of(DryStack.SOLIDS_KEY, DryStack.UNDERFLOW_SOLIDS));
	}

	private static SlurryAssay paste() {
		return SlurryAssay.of(Map.of(DryStack.SOLIDS_KEY, PasteFill.PASTE_SOLIDS, PasteFill.BINDER_KEY, PasteFill.BINDER));
	}

	// ---- the scenes -----------------------------------------------------------------------------------------------------------

	/**
	 * A bulkheaded 10 x 6 x 6 drive branch fills from the bottom up and reads FULL (paste_fill_void): paste is handed to the outlet a
	 * block's worth a tick; no layer takes paste before the one under it is full.
	 */
	private static void fillVoid(GameTestHelper helper) {
		branch(helper, 1, 2, 1, 10, 7, 6, true);
		fill(helper, 11, 2, 1, 11, 7, 6, TailingsBlocks.FILL_BULKHEAD.defaultBlockState());   // the bulkhead across its mouth
		FillOutletBlockEntity outlet = outlet(helper, new BlockPos(1, 7, 1));
		boolean[] order = {true};
		helper.onEachTick(() -> {
			outlet.receive(PasteFill.kgPerBlock());
			for (int y = 3; y <= 7; y++) {
				int above = count(helper, 1, y, 1, 10, y, 6, PasteFillBlock::wet);
				int below = count(helper, 1, y - 1, 1, 10, y - 1, 6, PasteFillBlock::wet);
				if (above > 0 && below < 60) {
					order[0] = false;
				}
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(order[0], Component.literal("paste should fill a layer only when the one under it is full"));
			helper.assertTrue(outlet.filled() == 359, Component.literal("all 359 m3 but the outlet's own cell should fill: " + outlet.filled() + "; " + outlet.status()));
			helper.assertTrue(outlet.scan() != null && outlet.scan().full(), Component.literal("the outlet should read FULL: " + outlet.status()));
			helper.assertTrue(outlet.status().startsWith("FULL"), Component.literal(outlet.status()));
			helper.assertTrue(!outlet.accepting() && outlet.receive(1000) == 0, Component.literal("a full void takes no more paste"));
		});
	}

	/**
	 * An outlet in a void open to the sky reads NOT ENCLOSED (paste_not_enclosed), and so does one behind a bulkhead wall with a gap in
	 * it; neither takes paste.
	 */
	private static void notEnclosed(GameTestHelper helper) {
		branch(helper, 1, 2, 1, 5, 4, 4, false);
		FillOutletBlockEntity open = outlet(helper, new BlockPos(1, 4, 1));
		branch(helper, 10, 2, 1, 14, 4, 4, true);
		fill(helper, 15, 2, 1, 15, 4, 4, TailingsBlocks.FILL_BULKHEAD.defaultBlockState());
		helper.setBlock(new BlockPos(15, 3, 2), Blocks.AIR);   // a bulkhead missing
		FillOutletBlockEntity gap = outlet(helper, new BlockPos(10, 4, 1));
		helper.succeedWhen(() -> {
			for (FillOutletBlockEntity outlet : new FillOutletBlockEntity[] {open, gap}) {
				helper.assertTrue(outlet.scan() != null && outlet.scan().status() == FillVoid.Status.OPEN_TO_SKY, Component.literal("should read open to the sky: " + outlet.status()));
				helper.assertTrue(outlet.status().startsWith("NOT ENCLOSED"), Component.literal(outlet.status()));
				helper.assertTrue(outlet.receive(2000) == 0, Component.literal("an open void takes no paste"));
			}
		});
	}

	/** Wet paste is soft and sets after its cure, eight steps of 2.5 minutes, one game day (paste_cure). */
	private static void cure(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 2, 2);
		helper.setBlock(at.below(), Blocks.STONE);
		helper.setBlock(at, TailingsBlocks.PASTE_FILL);
		BlockPos abs = helper.absolutePos(at);
		var level = helper.getLevel();
		helper.assertTrue(level.getBlockTicks().hasScheduledTick(abs, TailingsBlocks.PASTE_FILL), Component.literal("wet paste should be curing (a tick scheduled)"));
		helper.assertTrue(level.getBlockState(abs).getCollisionShape(level, abs).isEmpty(), Component.literal("wet paste is nothing to stand on"));
		for (int step = 1; step <= PasteFill.CURE_STEPS; step++) {
			BlockState state = level.getBlockState(abs);
			helper.assertTrue(PasteFillBlock.wet(state), Component.literal("still wet before step " + step));
			state.tick(level, abs, level.getRandom());   // what the scheduled tick does each 2.5 minutes
		}
		BlockState set = level.getBlockState(abs);
		helper.assertTrue(PasteFillBlock.set(set), Component.literal("paste should set after " + PasteFill.CURE_STEPS + " steps: " + set));
		helper.assertTrue(!set.getCollisionShape(level, abs).isEmpty(), Component.literal("set paste is solid"));
		helper.assertTrue(PasteFill.cureStepTicks() * PasteFill.CURE_STEPS == 24_000, Component.literal("the cure is one game day"));
		helper.succeed();
	}

	/**
	 * Breaking a bulkhead with wet paste behind it lets the paste run out of the gap (paste_bulkhead_fail): the wet paste at and above
	 * the gap runs out onto the drive floor outside; the set layer stays; the void reads NOT ENCLOSED.
	 */
	private static void bulkheadFail(GameTestHelper helper) {
		fill(helper, -8, 1, -14, 34, 1, 18, Blocks.STONE.defaultBlockState());   // the drive floor round the branch
		branch(helper, 1, 2, 1, 6, 5, 4, true);
		fill(helper, 7, 2, 1, 7, 5, 4, TailingsBlocks.FILL_BULKHEAD.defaultBlockState());
		fill(helper, 1, 2, 1, 6, 2, 4, TailingsBlocks.PASTE_FILL.defaultBlockState().setValue(PasteFillBlock.CURE, PasteFillBlock.SET));
		fill(helper, 1, 3, 1, 6, 5, 4, TailingsBlocks.PASTE_FILL.defaultBlockState());
		helper.setBlock(new BlockPos(1, 5, 1), Blocks.AIR);
		FillOutletBlockEntity outlet = outlet(helper, new BlockPos(1, 5, 1));
		helper.startSequence()
				.thenExecuteAfter(5, () -> helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(7, 3, 2)), false))
				.thenExecuteAfter(110, () -> {
					int inside = count(helper, 1, 3, 1, 6, 5, 4, PasteFillBlock::wet);
					int outside = count(helper, -8, 2, -14, 34, 12, 18, PasteFillBlock::wet) - inside;
					int set = count(helper, 1, 2, 1, 6, 2, 4, PasteFillBlock::set);
					helper.assertTrue(outside >= 60, Component.literal("the wet paste should run out of the gap: " + outside + " m3 outside, " + inside + " left inside"));
					helper.assertTrue(inside + outside == 71, Component.literal("no paste is lost or made: " + (inside + outside)));
					helper.assertTrue(set == 24, Component.literal("set paste stays: " + set));
					helper.assertTrue(outlet.status().startsWith("NOT ENCLOSED"), Component.literal("the void is no longer closed: " + outlet.status()));
				})
				.thenSucceed();
	}

	/** The paste mixer stops NO CEMENT, and mixes once cement is in its hopper (paste_no_cement). */
	private static void noCement(GameTestHelper helper) {
		fill(helper, 0, 0, 0, 8, 0, 8, Blocks.STONE.defaultBlockState());
		PlantBlockEntity filter = part(helper, Machine.PASTE_FILTER, new BlockPos(0, 1, 0));
		PlantBlockEntity mixer = part(helper, Machine.PASTE_MIXER, new BlockPos(5, 1, 0));
		mixer.forceRunning();
		filter.setDryKg(2_000);
		mixer.back().offer(Product.TAILS_SLURRY, 300, thickened());
		helper.startSequence()
				.thenExecuteAfter(10, () -> {
					helper.assertTrue(mixer.status().startsWith("NO CEMENT"), Component.literal("the mixer should stop NO CEMENT: " + mixer.status()));
					helper.assertTrue(mixer.out().held(Product.TAILS_SLURRY) == 0, Component.literal("no paste without cement"));
					ItemStack bags = new ItemStack(TailingsItems.CEMENT, 4);
					helper.assertTrue(mixer.offer(bags) && mixer.offer(bags), Component.literal("the hopper takes cement"));
					helper.assertTrue(mixer.dryKg() == 2 * PasteFill.CEMENT_BAG_KG, Component.literal("two 50 kg bags: " + mixer.dryKg()));
				})
				.thenExecuteAfter(20, () -> {
					helper.assertTrue(mixer.status().startsWith("mixing"), Component.literal("with cement it mixes: " + mixer.status()));
					double paste = mixer.out().held(Product.TAILS_SLURRY);
					helper.assertTrue(paste > 0, Component.literal("paste on its outlet"));
					SlurryAssay assay = mixer.out().assayOf(Product.TAILS_SLURRY);
					helper.assertTrue(Math.abs(assay.get(PasteFill.BINDER_KEY) - PasteFill.BINDER) < 1e-9 && Math.abs(assay.get(DryStack.SOLIDS_KEY) - PasteFill.PASTE_SOLIDS) < 1e-9,
							Component.literal("paste at 78 % solids and 5 % binder: " + assay.describe()));
					double used = 2 * PasteFill.CEMENT_BAG_KG - mixer.dryKg();
					double tails = paste * PasteFill.PASTE_SOLIDS / (1 + PasteFill.BINDER);
					helper.assertTrue(Math.abs(used - tails * PasteFill.BINDER) < 0.01, Component.literal("cement at 5 % of the tails: " + used + " kg for " + tails));
				})
				.thenSucceed();
	}

	/**
	 * The whole plant (paste_plant): thickened tails into the disc filter, its cake into the mixer touching it with a trim of thickened
	 * tails and cement, the paste down a steel pipe to the pump and on to a fill outlet in a closed void, where it lands as wet paste on
	 * the void's floor. The pump reads its flow and pressure on its PLC points. Rates sped up 40 times.
	 */
	private static void plant(GameTestHelper helper) {
		fill(helper, 0, 0, 0, 10, 0, 13, Blocks.STONE.defaultBlockState());
		PlantBlockEntity filter = part(helper, Machine.PASTE_FILTER, new BlockPos(0, 1, 0));
		PlantBlockEntity mixer = part(helper, Machine.PASTE_MIXER, new BlockPos(5, 1, 0));
		PlantBlockEntity pump = part(helper, Machine.PASTE_PUMP, new BlockPos(5, 1, 7));
		for (PlantBlockEntity p : new PlantBlockEntity[] {filter, mixer, pump}) {
			p.forceRunning();
			p.fastCycle(40);
		}
		helper.assertTrue(mixer.offer(new ItemStack(TailingsItems.CEMENT, 4)), Component.literal("cement in the mixer's hopper"));
		mixer.offer(new ItemStack(TailingsItems.CEMENT, 4));
		TailingsGameTests.pipe(helper, new BlockPos(6, 1, 5), new BlockPos(6, 1, 6));   // mixer to pump
		branch(helper, 5, 2, 16, 8, 4, 19, true);   // the void
		TailingsGameTests.pipe(helper, new BlockPos(6, 1, 13), new BlockPos(6, 2, 13), new BlockPos(6, 3, 13), new BlockPos(6, 3, 14), new BlockPos(6, 3, 15));
		FillOutletBlockEntity outlet = outlet(helper, new BlockPos(6, 3, 16));
		double[] flow = {0}, pressure = {0};
		helper.onEachTick(() -> {
			if (filter.back().held(Product.TAILS_SLURRY) < 200) {
				filter.back().offer(Product.TAILS_SLURRY, 200, thickened());
			}
			if (mixer.back().held(Product.TAILS_SLURRY) < 200) {
				mixer.back().offer(Product.TAILS_SLURRY, 200, thickened());
			}
			flow[0] = Math.max(flow[0], pump.readMa(6));
			pressure[0] = Math.max(pressure[0], pump.readMa(7));
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(filter.waterOut() + filter.waterReturned() + filter.waterLost() > 0, Component.literal("the filter should press out filtrate: " + filter.status()));
			helper.assertTrue(outlet.filled() >= 1, Component.literal("paste should reach the void: " + filter.status() + " / " + mixer.status() + " / " + pump.status() + " / " + outlet.status()));
			helper.assertTrue(count(helper, 5, 2, 16, 8, 2, 19, PasteFillBlock::wet) >= 1, Component.literal("wet paste on the void's floor"));
			helper.assertTrue(pump.status().startsWith("pumping"), Component.literal(pump.status()));
			helper.assertTrue(flow[0] > 4.5 && pressure[0] > 4.0, Component.literal("FLOW and PRESSURE should read: " + flow[0] + " mA, " + pressure[0] + " mA"));
			helper.assertTrue(pump.points().size() == 8, Component.literal("the pump has the motor's six points, FLOW and PRESSURE"));
		});
	}

	/**
	 * The fill's pace (paste_pace; Tim, 2026-11-10, l5-paste.md #19): a cubic metre of the line's paste, 2.0 t, fills ten blocks of the
	 * void, so the pump's 35 m3/h fills a 360 m3 drive branch in about an hour, not eleven; each block still takes a game day to set.
	 */
	private static void pace(GameTestHelper helper) {
		branch(helper, 1, 2, 1, 10, 7, 6, true);
		fill(helper, 11, 2, 1, 11, 7, 6, TailingsBlocks.FILL_BULKHEAD.defaultBlockState());
		FillOutletBlockEntity outlet = outlet(helper, new BlockPos(1, 7, 1));
		helper.assertTrue(Math.abs(PasteFill.hoursToFill(360) - 1.03) < 0.01, Component.literal("360 m3 in about an hour at the pump's rating: " + PasteFill.hoursToFill(360) + " h"));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(outlet.accepting(), Component.literal("the outlet reads its closed void: " + outlet.status())))
				.thenExecute(() -> helper.assertTrue(outlet.receive(PasteFill.PASTE_KG_PER_M3) == PasteFill.PASTE_KG_PER_M3, Component.literal("it takes a cubic metre of paste")))
				.thenExecuteAfter(40, () -> {
					helper.assertTrue(outlet.filled() == PasteFill.FILL_SCALE, Component.literal("a cubic metre of paste fills ten blocks: " + outlet.filled() + "; " + outlet.status()));
					int wet = count(helper, 1, 2, 1, 10, 7, 6, PasteFillBlock::wet);
					helper.assertTrue(wet == PasteFill.FILL_SCALE, Component.literal("ten wet blocks on the void's floor: " + wet));
					helper.assertTrue(PasteFill.cureStepTicks() * PasteFill.CURE_STEPS == 24_000, Component.literal("the cure stays a game day"));
				})
				.thenSucceed();
	}

	/** Over 1 km of fill line (1,100 m of pipe) the pump reads LINE PRESSURE and pushes nothing (paste_line_pressure). */
	private static void linePressure(GameTestHelper helper) {
		fill(helper, 0, 0, 0, 10, 0, 16, Blocks.STONE.defaultBlockState());
		PlantBlockEntity pump = part(helper, Machine.PASTE_PUMP, new BlockPos(0, 1, 0));
		pump.forceRunning();
		pump.back().offer(Product.TAILS_SLURRY, 1_000, paste());
		Block steel = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("wildcatter", "gas_line"));
		fill(helper, 0, 1, 6, 10, 10, 15, steel.defaultBlockState());   // 1,100 m of line, coiled
		FillOutletBlockEntity outlet = outlet(helper, new BlockPos(5, 11, 10));
		helper.succeedWhen(() -> {
			helper.assertTrue(pump.status().startsWith("LINE PRESSURE"), Component.literal("the pump should read LINE PRESSURE: " + pump.status()));
			helper.assertTrue(outlet.pasteKg() == 0 && pump.back().held(Product.TAILS_SLURRY) == 1_000, Component.literal("nothing pumped"));
		});
	}
}
