package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.control.ControlDevice;
import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.plant.XrtSorting;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Server scenes for the X-ray ore sorter (docs/multiblocks/xrt-ore-sorter.md, L8; decisions in docs/questions/l8-xrt.md): galena ore
 * and andesite split two ways at the default threshold with the spec's errors, fed by a belt over its feed end; LOW and HIGH; no air;
 * the PLC's threshold and its tonne pulses; what it refuses.
 */
final class XrtGameTests {
	static final String[] SCENES = {"xrt_split", "xrt_threshold", "xrt_no_air", "xrt_plc", "xrt_refuses"};
	static final Set<String> LONG = Set.of("xrt_split", "xrt_threshold", "xrt_plc");

	private XrtGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("xrt_split", XrtGameTests::split);
		out.accept("xrt_threshold", XrtGameTests::threshold);
		out.accept("xrt_no_air", XrtGameTests::noAir);
		out.accept("xrt_plc", XrtGameTests::plc);
		out.accept("xrt_refuses", XrtGameTests::refuses);
	}

	private static BlockState belt(Direction.Axis axis) {
		return TailingsBlocks.BELT_CONVEYOR.defaultBlockState().setValue(BeltConveyorBlock.AXIS, axis);
	}

	/** The sorter's parts and what reached each belt. */
	record Line(PlantBlockEntity sorter, PlantBlockEntity compressor, PlantBlockEntity product, PlantBlockEntity waste, RockLoad toProduct, RockLoad toWaste) {
		String says() {
			return sorter.inspect().getString() + " | " + compressor.status() + " | product belt " + product.status() + " | waste belt " + waste.status();
		}
	}

	/**
	 * The sorter at (2, y, 2), facing north: x 2-5, z 2-9. Its compressor touches its west side; a belt off its south face takes the
	 * product, one off its east face the waste. Each tick the scene lifts what reached either belt and counts it.
	 */
	private static Line line(GameTestHelper helper, int y, boolean airOn) {
		PlantBlockEntity sorter = TailingsGameTests.plant(helper, Machine.XRT_SORTER, new BlockPos(2, y, 2));
		PlantBlockEntity compressor = TailingsGameTests.plant(helper, Machine.SORTER_COMPRESSOR, new BlockPos(0, y, 2));
		for (int z = 10; z <= 12; z++) {
			helper.setBlock(new BlockPos(3, y, z), belt(Direction.Axis.Z));
		}
		PlantBlockEntity product = TailingsGameTests.plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(4, y, 12));
		for (int x = 6; x <= 8; x++) {
			helper.setBlock(new BlockPos(x, y, 6), belt(Direction.Axis.X));
		}
		PlantBlockEntity waste = TailingsGameTests.plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(9, y, 6));
		sorter.forceRunning();
		product.forceRunning();
		waste.forceRunning();
		if (airOn) {
			compressor.forceRunning();
		}
		Line line = new Line(sorter, compressor, product, waste, new RockLoad(), new RockLoad());
		helper.onEachTick(() -> {
			line.toProduct().add(product.hold().take(Double.MAX_VALUE));
			line.toWaste().add(waste.hold().take(Double.MAX_VALUE));
		});
		return line;
	}

	private static RockLoad load(Rock rock, double kg) {
		RockLoad load = new RockLoad();
		load.add(rock, SizeClass.CRUSHED, kg);
		return load;
	}

	private static boolean near(double got, double want) {
		return Math.abs(got - want) < 1;
	}

	/**
	 * xrt_split: 1 t of crushed galena ore and 1 t of crushed andesite come in on a belt over the sorter's feed end. At the default
	 * threshold 97 % of the galena is blown to product and 95 % of the andesite drops to waste; the product and waste keep their size.
	 */
	private static void split(GameTestHelper helper) {
		int y = 4;
		Line line = line(helper, y, true);
		// the feed belt over its north (feed) end, its head end over the sorter
		for (int x = 0; x <= 4; x++) {
			helper.setBlock(new BlockPos(x, y + 5, 3), belt(Direction.Axis.X));
		}
		PlantBlockEntity feed = TailingsGameTests.plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(5, y + 5, 2));
		feed.forceRunning();
		helper.startSequence()
				.thenExecuteAfter(20, () -> {
					RockLoad mixed = load(Rock.GALENA, 1000);
					mixed.add(load(Rock.ANDESITE, 1000));
					feed.receive(mixed);
				})
				.thenWaitUntil(() -> helper.assertTrue(near(line.toProduct().total() + line.toWaste().total(), 2000),
						Component.literal("both tonnes should be sorted: " + line.says() + " | feed " + feed.status())))
				.thenExecute(() -> {
					RockLoad p = line.toProduct(), w = line.toWaste();
					String s = String.format(" (product %.1f kg galena, %.1f andesite; waste %.1f galena, %.1f andesite) ", p.kg(Rock.GALENA, SizeClass.CRUSHED),
							p.kg(Rock.ANDESITE, SizeClass.CRUSHED), w.kg(Rock.GALENA, SizeClass.CRUSHED), w.kg(Rock.ANDESITE, SizeClass.CRUSHED)) + line.says();
					helper.assertTrue(near(p.kg(Rock.GALENA, SizeClass.CRUSHED), 970) && near(w.kg(Rock.GALENA, SizeClass.CRUSHED), 30),
							Component.literal("97 % of the galena ore to product, 3 % to waste" + s));
					helper.assertTrue(near(w.kg(Rock.ANDESITE, SizeClass.CRUSHED), 950) && near(p.kg(Rock.ANDESITE, SizeClass.CRUSHED), 50),
							Component.literal("95 % of the andesite to waste, 5 % with the product" + s));
					helper.assertTrue(near(line.sorter().xrt().productKg(), 1020) && near(line.sorter().xrt().wasteKg(), 980), Component.literal("its tallies" + s));
					helper.assertTrue(line.sorter().readPulses(7) == 1 && line.sorter().readPulses(8) == 0, Component.literal("a pulse a whole tonne" + s));
				})
				.thenSucceed();
	}

	/**
	 * xrt_threshold: at LOW even andesite reads over the threshold and nearly all of it goes to product; at HIGH the low-grade
	 * molybdenum ore (3 % molybdenite) drops to waste while the galena ore is still product. A sneaking click steps the screen.
	 */
	private static void threshold(GameTestHelper helper) {
		Line line = line(helper, 4, true);
		PlantBlockEntity sorter = line.sorter();
		sorter.xrt().setStep(0);
		helper.startSequence()
				.thenExecute(() -> sorter.receive(load(Rock.ANDESITE, 500)))
				.thenWaitUntil(() -> helper.assertTrue(near(line.toProduct().total() + line.toWaste().total(), 500), Component.literal("sorted at LOW: " + line.says())))
				.thenExecute(() -> {
					helper.assertTrue(near(line.toProduct().kg(Rock.ANDESITE, SizeClass.CRUSHED), 485), Component.literal("at LOW 97 % of even andesite is product: " + line.says()));
					line.toProduct().clear();
					line.toWaste().clear();
					for (int i = 0; i < 4; i++) {
						sorter.stepThreshold();
					}
					helper.assertTrue(sorter.xrtStep() == 4 && sorter.status().contains("HIGH"), Component.literal("four clicks from LOW is HIGH: " + sorter.status()));
					RockLoad two = load(Rock.MOLYBDENITE, 500);
					two.add(load(Rock.GALENA, 500));
					sorter.receive(two);
				})
				.thenWaitUntil(() -> helper.assertTrue(near(line.toProduct().total() + line.toWaste().total(), 1000), Component.literal("sorted at HIGH: " + line.says())))
				.thenExecute(() -> {
					helper.assertTrue(near(line.toWaste().kg(Rock.MOLYBDENITE, SizeClass.CRUSHED), 475), Component.literal("at HIGH the low-grade molybdenum ore is waste: " + line.says()));
					helper.assertTrue(near(line.toProduct().kg(Rock.GALENA, SizeClass.CRUSHED), 485), Component.literal("and the galena ore still product: " + line.says()));
					sorter.stepThreshold();
					helper.assertTrue(sorter.xrtStep() == 0, Component.literal("one more click goes round to LOW"));
				})
				.thenSucceed();
	}

	/** xrt_no_air: with its compressor unpowered, and then with none, the sorter reads NO AIR and keeps its rock: none is lost to waste. */
	private static void noAir(GameTestHelper helper) {
		Line line = line(helper, 4, false);
		line.sorter().receive(load(Rock.GALENA, 500));
		helper.startSequence()
				.thenExecuteAfter(40, () -> {
					helper.assertTrue(line.sorter().status().contains("NO AIR") && line.sorter().status().contains("not running"), Component.literal("an unpowered compressor: " + line.says()));
					helper.assertTrue(near(line.sorter().hold().total(), 500) && line.toWaste().isEmpty() && line.toProduct().isEmpty(), Component.literal("nothing sorted, nothing lost: " + line.says()));
					helper.assertTrue(line.compressor().status().contains("waiting for its motor"), Component.literal("the compressor wants to run: " + line.compressor().status()));
					helper.destroyBlock(new BlockPos(0, 4, 2));
				})
				.thenExecuteAfter(10, () -> {
					helper.assertTrue(line.sorter().status().contains("NO AIR") && line.sorter().status().contains("needs a Sorter Air Compressor"),
							Component.literal("no compressor at all: " + line.sorter().status()));
					helper.assertTrue(near(line.sorter().hold().total(), 500), Component.literal("still holds its rock"));
				})
				.thenSucceed();
	}

	/**
	 * xrt_plc: in AUTO the PLC's THRESHOLD loop at 20 mA sets HIGH over the screen's step; its PRODUCT counter counts a pulse a tonne of
	 * galena ore; the PLC quiet, the screen's step is back.
	 */
	private static void plc(GameTestHelper helper) {
		Line line = line(helper, 4, true);
		PlantBlockEntity sorter = line.sorter();
		helper.assertTrue(sorter.points().get(6).name().startsWith("THRESHOLD") && sorter.points().get(6).signal() == ControlDevice.Signal.MA && sorter.points().get(6).output(),
				Component.literal("point 6 is the threshold, an analog output of the PLC: " + sorter.points()));
		helper.assertTrue(sorter.points().get(7).signal() == ControlDevice.Signal.PULSE && sorter.points().get(8).name().startsWith("WASTE"),
				Component.literal("7 and 8 count product and waste: " + sorter.points()));
		BlockPos at = helper.absolutePos(new BlockPos(2, 4, 2));
		sorter.cycleHoa(at);
		sorter.cycleHoa(at);   // HAND, OFF, AUTO
		boolean[] plc = {true};
		helper.onEachTick(() -> {
			long now = helper.getLevel().getGameTime();
			sorter.commandPoint(0, true, now);
			if (plc[0]) {
				sorter.commandMa(6, XrtSorting.maOf(4), now);
			}
		});
		helper.startSequence()
				.thenExecuteAfter(2, () -> {
					helper.assertTrue(sorter.xrtStep() == 4 && sorter.xrt().step() == XrtSorting.DEFAULT_STEP, Component.literal("the PLC sets HIGH; the screen keeps its own: " + sorter.inspect().getString()));
					helper.assertTrue(sorter.inspect().getString().contains("(PLC)"), Component.literal("it reads that the PLC sets it"));
					sorter.receive(load(Rock.GALENA, 2100));
				})
				.thenWaitUntil(() -> helper.assertTrue(sorter.hold().isEmpty() && sorter.readPulses(7) == 2 && sorter.readPulses(8) == 0,
						Component.literal("2.04 t of product is two pulses: " + line.says())))
				.thenExecute(() -> plc[0] = false)
				.thenExecuteAfter(15, () -> helper.assertTrue(sorter.xrtStep() == XrtSorting.DEFAULT_STEP, Component.literal("the PLC quiet, the screen's step again: " + sorter.xrtStep())))
				.thenSucceed();
	}

	/** xrt_refuses: sand and fines are too fine to sort; crushed rock and gravel go in by hand; the part reads NO FEED empty. */
	private static void refuses(GameTestHelper helper) {
		Line line = line(helper, 4, true);
		PlantBlockEntity sorter = line.sorter();
		helper.assertTrue(!sorter.offer(new ItemStack(BuiltInRegistries.ITEM.getValue(Tailings.id("sand_granite")))), Component.literal("sand is under its 10 mm"));
		helper.assertTrue(!sorter.accepts(Rock.GRANITE, SizeClass.FINES) && !sorter.accepts(Rock.GRANITE, null), Component.literal("nor fines, nor unbroken rock"));
		helper.startSequence()
				.thenExecuteAfter(5, () -> helper.assertTrue(sorter.status().equals("NO FEED"), Component.literal("empty, it reads NO FEED: " + sorter.status())))
				.thenExecute(() -> helper.assertTrue(sorter.offer(new ItemStack(BuiltInRegistries.ITEM.getValue(Tailings.id("gravel_granite")))),
						Component.literal("gravel goes in by hand")))
				.thenWaitUntil(() -> helper.assertTrue(line.toWaste().kg(Rock.GRANITE, SizeClass.GRAVEL) > 100 && sorter.status().startsWith("sorting Granite"),
						Component.literal("granite gravel sorts to waste, as gravel: " + line.says())))
				.thenExecute(() -> {
					double w = line.toWaste().kg(Rock.GRANITE, SizeClass.GRAVEL), p = line.toProduct().kg(Rock.GRANITE, SizeClass.GRAVEL);
					helper.assertTrue(Math.abs(p / (p + w) - 0.05) < 0.01, Component.literal("5 % of it with the product, got " + p / (p + w)));
				})
				.thenSucceed();
	}
}
