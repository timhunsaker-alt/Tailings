package dev.wildcatter.tailings.neoforge.gametest;

import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.floor;
import static dev.wildcatter.tailings.neoforge.gametest.PitHaulGameTests.vehicle;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsEntities;
import dev.wildcatter.tailings.block.FuelBayBlockEntity;
import dev.wildcatter.tailings.block.PasteFillBlock;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.stage.SlurryGauge;
import dev.wildcatter.tailings.entity.HaulTruckEntity;
import dev.wildcatter.tailings.OptInRules;
import dev.wildcatter.tailings.paste.PasteClock;
import dev.wildcatter.tailings.paste.PasteFill;
import dev.wildcatter.tailings.pit.DryStack;
import dev.wildcatter.tailings.pit.Loose;
import dev.wildcatter.tailings.pit.PitMass;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlcScale;
import dev.wildcatter.tailings.rock.Rock;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server scenes for the known gaps closed in the gaps2 wave (docs/questions/gaps2.md): the fuel bay filled by pipe, the press's and
 * load-out's own PLC points, the paste filter's and mixer's slurry gauges, the headframe bin into a hopper, a stone wall breached
 * beside wet paste, paste making up the time its chunk was unloaded, and muck tipped onto wet paste.
 */
final class Gaps2GameTests {
	static final String[] SCENES = {"gaps_fuel_bay_pipe", "gaps_press_points", "gaps_loadout_points", "gaps_paste_gauges", "gaps_headframe_hopper",
			"gaps_paste_wall_breach", "gaps_paste_catch_up", "gaps_muck_on_paste"};
	static final Set<String> LONG = Set.of("gaps_press_points");
	private static final Map<String, Integer> TICKS = Map.of("gaps_press_points", 1600, "gaps_loadout_points", 1600);

	private Gaps2GameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("gaps_fuel_bay_pipe", Gaps2GameTests::fuelBayPipe);
		out.accept("gaps_press_points", Gaps2GameTests::pressPoints);
		out.accept("gaps_loadout_points", Gaps2GameTests::loadoutPoints);
		out.accept("gaps_paste_gauges", Gaps2GameTests::pasteGauges);
		out.accept("gaps_headframe_hopper", Gaps2GameTests::headframeHopper);
		out.accept("gaps_paste_wall_breach", Gaps2GameTests::wallBreach);
		out.accept("gaps_paste_catch_up", Gaps2GameTests::catchUp);
		out.accept("gaps_muck_on_paste", Gaps2GameTests::muckOnPaste);
	}

	static int ticks(String scene, int otherwise) {
		return TICKS.getOrDefault(scene, otherwise);
	}

	// ---- building ---------------------------------------------------------------------------------------------------------------

	private static PlantBlockEntity plant(GameTestHelper helper, Machine machine, BlockPos pos) {
		return TailingsGameTests.plant(helper, machine, pos);
	}

	private static void fill(GameTestHelper helper, int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
		for (int x = x0; x <= x1; x++) {
			for (int y = y0; y <= y1; y++) {
				for (int z = z0; z <= z1; z++) {
					helper.setBlock(new BlockPos(x, y, z), state);
				}
			}
		}
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

	/** Thickened tails at 60 % solids, as the tailings thickener puts them on the pipe. */
	private static SlurryAssay thickened() {
		return SlurryAssay.of(Map.of(DryStack.SOLIDS_KEY, DryStack.UNDERFLOW_SOLIDS));
	}

	// ---- the scenes -----------------------------------------------------------------------------------------------------------

	/**
	 * The fuel bay's pipe intake (gaps_fuel_bay_pipe, P2a): a stock tank of diesel on Wildcatter's steel pipe to the bay's east side; the
	 * day tank fills from it, once a second, and the stock tank goes down by as much.
	 */
	private static void fuelBayPipe(GameTestHelper helper) {
		floor(helper, 0, 12, 0, 0, 0, 6);
		FuelBayBlockEntity bay = TailingsGameTests.part(helper, TailingsBlocks.FUEL_BAY, new BlockPos(1, 1, 1), FuelBayBlockEntity.class);
		var tank = TailingsGameTests.part(helper, BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "stock_tank")), new BlockPos(6, 1, 1),
				dev.wildcatter.block.StorageTankBlockEntity.class);
		tank.testFill(Product.DIESEL, 5_000);
		TailingsGameTests.pipe(helper, new BlockPos(5, 1, 1));
		helper.assertTrue(bay.litres() == 0, Component.literal("the day tank starts empty"));
		helper.succeedWhen(() -> {
			helper.assertTrue(bay.litres() > 100, Component.literal("the day tank should fill by pipe: " + bay.inspect().getString() + " | " + tank.inspect().getString()));
			helper.assertTrue(Math.abs(bay.litres() + tank.amount() - 5_000) < 1, Component.literal("what the bay took left the stock tank: " + bay.litres() + " + " + tank.amount()));
		});
	}

	/**
	 * The tailings filter's own points (gaps_press_points, P4): CYCLE counts a pulse a cake drop, and FULL is on while a press with
	 * nowhere to drop its cake holds it. Cycles run 60 times fast.
	 */
	private static void pressPoints(GameTestHelper helper) {
		floor(helper, 0, 40, 0, 0, 0, 30);
		plant(helper, Machine.LOADOUT_BIN, new BlockPos(0, 1, 0));
		PlantBlockEntity filter = plant(helper, Machine.TAILINGS_FILTER, new BlockPos(2, 8, 0));
		filter.forceRunning();
		filter.fastCycle(60);
		filter.back().offer(Product.TAILS_SLURRY, 26_000, thickened());
		PlantBlockEntity full = plant(helper, Machine.TAILINGS_FILTER, new BlockPos(24, 1, 0));
		full.forceRunning();
		full.fastCycle(60);
		full.back().offer(Product.TAILS_SLURRY, 26_000, thickened());
		helper.assertTrue(filter.points().size() == 8, Component.literal("the press has the motor's six points, CYCLE and FULL: " + filter.points().size()));
		helper.assertTrue(filter.readPulses(6) == 0 && !filter.readPoint(7), Component.literal("no drops yet, not full"));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(filter.cycles() == 1, Component.literal("one cycle should drop: " + filter.status())))
				.thenExecute(() -> {
					helper.assertTrue(filter.readPulses(6) == 1, Component.literal("CYCLE: a pulse for the drop: " + filter.readPulses(6)));
					helper.assertTrue(!filter.readPoint(7), Component.literal("its cake went into the bin: not FULL"));
				})
				.thenWaitUntil(() -> helper.assertTrue(full.status().startsWith("FULL"), Component.literal("nowhere to drop, it reads FULL: " + full.status())))
				.thenExecute(() -> helper.assertTrue(full.readPoint(7) && full.readPulses(6) == 1, Component.literal("FULL on, and its drop counted: " + full.readPulses(6))))
				.thenSucceed();
	}

	/**
	 * The load-out's own points (gaps_loadout_points, P4), on its frame: LEVEL, the bin's 400 t over 4-20 mA; LOADING while the gate
	 * fills a truck; FULL at the bin's FULL.
	 */
	private static void loadoutPoints(GameTestHelper helper) {
		floor(helper, 0, 20, 0, 0, 0, 24);
		PlantBlockEntity frame = plant(helper, Machine.LOADOUT_FRAME, new BlockPos(0, 1, 0));
		PlantBlockEntity bin = plant(helper, Machine.LOADOUT_BIN, new BlockPos(1, 11, 1));
		frame.forceRunning();
		bin.setDryKg(100_000);
		helper.assertTrue(frame.points().size() == 9, Component.literal("the frame has the motor's six points, LEVEL, FULL and LOADING: " + frame.points().size()));
		double level = frame.readMa(6);
		helper.assertTrue(Math.abs(level - 8) < 0.01, Component.literal("100 of 400 t reads 8 mA: " + level));
		helper.assertTrue(!frame.readPoint(7) && !frame.readPoint(8), Component.literal("not full, not loading"));
		HaulTruckEntity truck = vehicle(helper, TailingsEntities.HAUL_TRUCK, 6.0, 1, 7.0, 0);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(frame.readPoint(8), Component.literal("LOADING while the gate fills the truck: " + frame.status())))
				.thenWaitUntil(() -> helper.assertTrue(truck.load().room() == 0, Component.literal("the truck fills: " + frame.status())))
				.thenIdle(2)
				.thenExecute(() -> {
					helper.assertTrue(!frame.readPoint(8), Component.literal("LOADING off once the truck is full: " + frame.status()));
					helper.assertTrue(frame.readMa(6) < level, Component.literal("the level falls as it loads: " + frame.readMa(6)));
					bin.setDryKg(DryStack.BIN_KG);
					helper.assertTrue(frame.readPoint(7) && Math.abs(frame.readMa(6) - 20) < 0.01, Component.literal("a full bin: FULL on, LEVEL 20 mA: " + frame.readMa(6)));
				})
				.thenSucceed();
	}

	/**
	 * The paste filter's and mixer's slurry gauges (gaps_paste_gauges, L5): LEVEL, FLOW and DENSITY as every slurry part has, reading
	 * their feed tanks of thickened tails at 60 % solids (1.61 kg/L, from the solids the assay carries).
	 */
	private static void pasteGauges(GameTestHelper helper) {
		floor(helper, 0, 10, 0, 0, 0, 8);
		PlantBlockEntity filter = plant(helper, Machine.PASTE_FILTER, new BlockPos(0, 1, 0));
		PlantBlockEntity mixer = plant(helper, Machine.PASTE_MIXER, new BlockPos(5, 1, 0));
		double hold = SlurryGauge.hold(Machine.PASTE_FILTER);
		filter.back().offer(Product.TAILS_SLURRY, hold / 2, thickened());
		mixer.back().offer(Product.TAILS_SLURRY, hold / 4, thickened());
		double sg = PlcScale.density(DryStack.UNDERFLOW_SOLIDS);
		helper.startSequence()
				.thenIdle(5)
				.thenExecute(() -> {
					for (PlantBlockEntity part : new PlantBlockEntity[] {filter, mixer}) {
						helper.assertTrue(part.points().size() == 9, Component.literal(part.deviceName() + " has the motor's six points, LEVEL, FLOW and DENSITY: " + part.points().size()));
						helper.assertTrue(part.points().get(6).name().startsWith("LEVEL") && part.points().get(8).name().startsWith("DENSITY"), Component.literal("in the slurry parts' order"));
						helper.assertTrue(Math.abs(part.readMa(8) - PlcScale.ma(sg, PlcScale.DENSITY_LOW, PlcScale.DENSITY_HIGH)) < 0.01,
								Component.literal(part.deviceName() + " DENSITY should read 60 % solids, " + sg + " kg/L: " + part.readMa(8) + " mA; " + part.status()));
					}
					helper.assertTrue(Math.abs(filter.readMa(6) - 12) < 0.05, Component.literal("the filter's tank half full: 12 mA, " + filter.readMa(6) + "; " + filter.status()));
					helper.assertTrue(Math.abs(mixer.readMa(6) - 8) < 0.05, Component.literal("the mixer's trim a quarter: 8 mA, " + mixer.readMa(6) + "; " + mixer.status()));
					helper.assertTrue(filter.inspect().getString().contains("slurry level"), Component.literal("its readout gives the gauges: " + filter.inspect().getString()));
				})
				.thenSucceed();
	}

	/**
	 * The headframe bin into a hopper under it (gaps_headframe_hopper, L4): with no belt under it, it puts each whole block's worth of
	 * one rock (2.7 t) into the hopper as that rock's item, at the belt's rate; less than a block stays in the bin. Only with
	 * {@code headframe_hopper} on (off by default): the scene checks it off, turns it on, and hands it back to the config file.
	 */
	private static void headframeHopper(GameTestHelper helper) {
		floor(helper, 0, 8, 0, 0, 0, 8);
		BlockPos hopperAt = new BlockPos(3, 1, 3);
		helper.setBlock(hopperAt, Blocks.HOPPER);
		PlantBlockEntity bin = plant(helper, Machine.HEADFRAME_BIN, new BlockPos(2, 2, 2));
		bin.hold().add(Rock.GRANITE, null, 2 * 2700 + 1000);
		bin.fastCycle(20);   // an item each 65 s at the belt's rate: 20 times fast
		HopperBlockEntity hopper = helper.getBlockEntity(hopperAt, HopperBlockEntity.class);
		java.util.function.IntSupplier granite = () -> {
			int n = 0;
			for (int slot = 0; slot < hopper.getContainerSize(); slot++) {
				if (hopper.getItem(slot).is(Items.GRANITE)) {
					n += hopper.getItem(slot).getCount();
				}
			}
			return n;
		};
		OptInRules.setTest(OptInRules.HEADFRAME_HOPPER, false);
		helper.startSequence()
				.thenIdle(80)
				.thenExecute(() -> {
					helper.assertTrue(granite.getAsInt() == 0 && bin.status().contains("(a belt under it)"), Component.literal("off (the default), a hopper under it takes nothing: "
							+ granite.getAsInt() + "; " + bin.status()));
					OptInRules.setTest(OptInRules.HEADFRAME_HOPPER, true);
				})
				.thenWaitUntil(() -> {
					helper.assertTrue(granite.getAsInt() == 2, Component.literal("on, two blocks of granite into the hopper: " + granite.getAsInt() + "; " + bin.status()));
					helper.assertTrue(Math.abs(bin.hold().total() - 1000) < 1, Component.literal("a tonne, under a block, stays in the bin: " + bin.hold().total()));
				})
				.thenExecute(() -> OptInRules.setTest(OptInRules.HEADFRAME_HOPPER, null))
				.thenSucceed();
	}

	/**
	 * A stone wall breached beside wet paste (gaps_paste_wall_breach, L5): a stone changed for cobblestone lets nothing out; a stone
	 * mined out of the wall lets the wet paste at and above the gap run out of it, as a broken bulkhead does; set paste stays. Only with
	 * {@code paste_wall_breach} on (off by default): a stone mined out with it off lets nothing out.
	 */
	private static void wallBreach(GameTestHelper helper) {
		fill(helper, -8, 1, -14, 34, 1, 18, Blocks.STONE.defaultBlockState());   // the drive floor round the branch
		fill(helper, 0, 1, 0, 7, 6, 5, Blocks.STONE.defaultBlockState());
		fill(helper, 1, 2, 1, 6, 2, 4, TailingsBlocks.PASTE_FILL.defaultBlockState().setValue(PasteFillBlock.CURE, PasteFillBlock.SET));
		fill(helper, 1, 3, 1, 6, 5, 4, TailingsBlocks.PASTE_FILL.defaultBlockState());
		helper.setBlock(new BlockPos(1, 5, 1), Blocks.AIR);   // the void's last open cell
		OptInRules.setTest(OptInRules.PASTE_WALL_BREACH, false);
		helper.startSequence()
				.thenExecuteAfter(5, () -> helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(7, 4, 4)), false))
				.thenExecuteAfter(10, () -> {
					helper.assertTrue(count(helper, 1, 3, 1, 6, 5, 4, PasteFillBlock::wet) == 71, Component.literal("off (the default), a breach lets nothing out: "
							+ count(helper, 1, 3, 1, 6, 5, 4, PasteFillBlock::wet)));
					OptInRules.setTest(OptInRules.PASTE_WALL_BREACH, true);
					helper.setBlock(new BlockPos(7, 4, 3), Blocks.COBBLESTONE);
				})
				.thenExecuteAfter(10, () -> {
					helper.assertTrue(count(helper, 1, 3, 1, 6, 5, 4, PasteFillBlock::wet) == 71, Component.literal("a wall changed for a wall lets nothing out: "
							+ count(helper, 1, 3, 1, 6, 5, 4, PasteFillBlock::wet)));
					helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(7, 3, 2)), false);
				})
				.thenExecuteAfter(40, () -> {
					int inside = count(helper, 1, 3, 1, 6, 5, 4, PasteFillBlock::wet);
					int outside = count(helper, -8, 2, -14, 34, 12, 18, PasteFillBlock::wet) - inside;
					int set = count(helper, 1, 2, 1, 6, 2, 4, PasteFillBlock::set);
					helper.assertTrue(outside >= 40, Component.literal("the wet paste should run out of the gap in the stone: " + outside + " m3 outside, " + inside + " left inside"));
					helper.assertTrue(inside + outside == 71, Component.literal("no paste is lost or made: " + (inside + outside)));
					helper.assertTrue(set == 24, Component.literal("set paste stays: " + set));
					OptInRules.setTest(OptInRules.PASTE_WALL_BREACH, null);
				})
				.thenSucceed();
	}

	/**
	 * Paste makes up the time its chunk was unloaded (gaps_paste_catch_up, L5): with its chunk's paste last stepped five steps ago, the
	 * next step is the missed four and its own, and so for each wet block in the chunk within a step; a chunk with no record yet (an
	 * existing world's paste) steps once. Only with {@code paste_unloaded_cure} on (off by default): off, it steps once.
	 */
	private static void catchUp(GameTestHelper helper) {
		fill(helper, 0, 1, 0, 6, 1, 6, Blocks.STONE.defaultBlockState());
		BlockPos a = new BlockPos(2, 2, 2), b = new BlockPos(3, 2, 2);
		helper.setBlock(a, TailingsBlocks.PASTE_FILL);
		helper.setBlock(b, TailingsBlocks.PASTE_FILL);
		var level = helper.getLevel();
		BlockPos absA = helper.absolutePos(a), absB = helper.absolutePos(b);
		ChunkPos chunk = ChunkPos.containing(absA);
		helper.assertTrue(chunk.equals(ChunkPos.containing(absB)), Component.literal("both in one chunk"));
		long now = level.getGameTime();
		long step = PasteFill.cureStepTicks();
		helper.assertTrue(new PasteClock().missedSteps(chunk, now) == 0, Component.literal("a chunk with no record makes up nothing"));
		OptInRules.setTest(OptInRules.PASTE_UNLOADED_CURE, false);
		PasteClock.get(level).backdate(chunk, now, 5 * step);
		level.getBlockState(absA).tick(level, absA, level.getRandom());
		helper.assertTrue(level.getBlockState(absA).getValue(PasteFillBlock.CURE) == 1, Component.literal("off (the default), one step only: " + level.getBlockState(absA)));
		helper.setBlock(a, TailingsBlocks.PASTE_FILL);
		OptInRules.setTest(OptInRules.PASTE_UNLOADED_CURE, true);
		PasteClock.get(level).backdate(chunk, now, 5 * step);
		level.getBlockState(absA).tick(level, absA, level.getRandom());
		helper.assertTrue(level.getBlockState(absA).getValue(PasteFillBlock.CURE) == 5, Component.literal("four steps missed and its own: " + level.getBlockState(absA)));
		level.getBlockState(absB).tick(level, absB, level.getRandom());
		helper.assertTrue(level.getBlockState(absB).getValue(PasteFillBlock.CURE) == 5, Component.literal("the chunk's other wet paste makes them up too: " + level.getBlockState(absB)));
		PasteClock.get(level).backdate(chunk, now, 20 * step);
		level.getBlockState(absB).tick(level, absB, level.getRandom());
		helper.assertTrue(PasteFillBlock.set(level.getBlockState(absB)), Component.literal("a long time unloaded, it sets: " + level.getBlockState(absB)));
		OptInRules.setTest(OptInRules.PASTE_UNLOADED_CURE, null);
		helper.succeed();
	}

	/**
	 * Muck tipped onto wet paste (gaps_muck_on_paste, L5): it rests on the paste rather than replacing it, and stays there when its
	 * scheduled settle comes. This was already so (the game's free test is not about collision, and paste is not replaceable); the
	 * Known gap said otherwise, and this scene keeps it so.
	 */
	private static void muckOnPaste(GameTestHelper helper) {
		fill(helper, 0, 1, 0, 8, 1, 8, Blocks.STONE.defaultBlockState());
		fill(helper, 1, 2, 1, 7, 2, 7, TailingsBlocks.PASTE_FILL.defaultBlockState());
		BlockState wet = TailingsBlocks.PASTE_FILL.defaultBlockState();
		boolean free = FallingBlock.isFree(wet);
		BlockPos rest = Loose.muck(helper.getLevel(), helper.absolutePos(new BlockPos(4, 6, 4)), Map.of("minecraft:granite", PitMass.MUCK_QUARTERS));
		helper.startSequence()
				.thenIdle(10)
				.thenExecute(() -> {
					helper.assertTrue(rest != null && rest.equals(helper.absolutePos(new BlockPos(4, 3, 4))),
							Component.literal("the muck should rest on the paste: " + rest + " (the game's free test on wet paste: " + free + ")"));
					helper.assertTrue(count(helper, 1, 2, 1, 7, 2, 7, PasteFillBlock::wet) == 49, Component.literal("the paste under it is all still there"));
					helper.assertTrue(helper.getBlockState(new BlockPos(4, 3, 4)).is(TailingsBlocks.MUCK), Component.literal("and the muck stays on it: " + helper.getBlockState(new BlockPos(4, 3, 4))));
				})
				.thenSucceed();
	}
}
