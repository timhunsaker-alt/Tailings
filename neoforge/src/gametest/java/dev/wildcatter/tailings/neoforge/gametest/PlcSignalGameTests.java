package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.fluid.Product;
import dev.wildcatter.tailings.block.CableJunctionBlockEntity;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.mine.DrillCodes;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlcScale;
import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.rock.Rock;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Server scenes for the last of Wildcatter 13i's signals (docs/questions/plc-13i.md): the sorter's routing by rock code, the TBM's
 * course on its cable junction, and a slurry part's level, flow and density. Each drives a device as a PLC's modules do: a command
 * every tick, as its scan sends one.
 */
final class PlcSignalGameTests {
	static final String[] SCENES = {"plc_sorter_route", "plc_tbm_course", "plc_slurry"};
	static final Set<String> LONG = Set.of("plc_slurry", "plc_sorter_route");
	/** The sorter's points: a rock code a side, north to west, past its four gates. */
	private static final int CODES = 10;

	private PlcSignalGameTests() {
	}

	static void register(BiConsumer<String, Consumer<GameTestHelper>> out) {
		out.accept("plc_sorter_route", PlcSignalGameTests::sorterRoute);
		out.accept("plc_tbm_course", PlcSignalGameTests::tbmCourse);
		out.accept("plc_slurry", PlcSignalGameTests::slurry);
	}

	private static void auto(dev.wildcatter.control.ControlDevice device, BlockPos cell) {
		device.cycleHoa(cell);
		device.cycleHoa(cell);   // HAND, OFF, AUTO
	}

	/**
	 * The screen sends copper ore east and the waste south. In AUTO the PLC says the west side takes copper ore (code 10, 6.5 mA) and the
	 * east nothing of its own (code 0, 4 mA): the copper goes west. The PLC goes quiet and the sides go back to the screen's: copper east.
	 */
	private static void sorterRoute(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity sorter = TailingsGameTests.plant(helper, Machine.ORE_SORTER, new BlockPos(4, y, 4));
		PlantBlockEntity east = TailingsGameTests.plant(helper, Machine.ORE_BIN, new BlockPos(7, y, 4));
		PlantBlockEntity south = TailingsGameTests.plant(helper, Machine.ORE_BIN, new BlockPos(4, y, 7));
		PlantBlockEntity west = TailingsGameTests.plant(helper, Machine.ORE_BIN, new BlockPos(1, y, 4));
		sorter.forceRunning();
		helper.assertTrue(sorter.setSide(Direction.EAST, new ItemStack(Blocks.COPPER_ORE)), Component.literal("the screen sends copper ore east"));
		helper.assertTrue(sorter.setWasteSide(Direction.SOUTH), Component.literal("and the waste south"));
		helper.assertTrue(sorter.points().get(CODES + 3).name().startsWith("WEST TAKES") && sorter.points().get(CODES + 3).signal() == dev.wildcatter.control.ControlDevice.Signal.MA,
				Component.literal("the sorter's last point is the west side's rock code: " + sorter.points()));
		auto(sorter, helper.absolutePos(new BlockPos(4, y, 4)));
		boolean[] plc = {true};
		helper.onEachTick(() -> {
			if (plc[0]) {
				long now = helper.getLevel().getGameTime();
				sorter.commandMa(CODES + 1, PlcScale.codeMa(0), now);
				sorter.commandMa(CODES + 3, PlcScale.codeMa(PlcScale.codeOf(Rock.COPPER)), now);
			}
		});
		helper.assertTrue(PlcScale.codeOf(Rock.COPPER) == 10 && PlcScale.codeMa(10) == 6.5, Component.literal("copper ore is code 10, 6.5 mA"));
		helper.startSequence()
				.thenExecuteAfter(2, () -> {
					helper.assertTrue(sorter.sorter().route("minecraft:copper_ore") == 3, Component.literal("the PLC sends copper ore west, got side " + sorter.sorter().route("minecraft:copper_ore")));
					helper.assertTrue("minecraft:copper_ore".equals(sorter.sorter().product(1)), Component.literal("the screen's setting is kept"));
					sorter.binned().put("minecraft:copper_ore", 32);
					sorter.binned().put("minecraft:cobblestone", 32);
				})
				.thenWaitUntil(() -> helper.assertTrue(west.binned().getOrDefault("minecraft:copper_ore", 0) == 32 && south.binned().getOrDefault("minecraft:cobblestone", 0) == 32
						&& east.binned().isEmpty(), Component.literal("copper west, cobble to waste, nothing east (sorter " + sorter.status() + "; " + sorter.inspect().getString() + ")")))
				.thenExecute(() -> plc[0] = false)
				.thenExecuteAfter(15, () -> {
					helper.assertTrue(!sorter.sorter().chosen(3) && sorter.sorter().route("minecraft:copper_ore") == 1,
							Component.literal("the PLC quiet, the sides are the screen's again: copper east, got side " + sorter.sorter().route("minecraft:copper_ore")));
					sorter.binned().put("minecraft:copper_ore", 16);
				})
				.thenWaitUntil(() -> helper.assertTrue(east.binned().getOrDefault("minecraft:copper_ore", 0) == 16, Component.literal("copper east, sorter " + sorter.status())))
				.thenSucceed();
	}

	/**
	 * The PLC sets the course before its GO: a flat drive of 3 m (the cab's screen said 32). The course it started on holds while it drives,
	 * though the PLC asks for 400; it stops at its 3 m, and reads back what it holds.
	 */
	private static void tbmCourse(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = TailingsGameTests.tbm(helper, c, 6, true);
		TailingsGameTests.feed(helper, c);
		cab.core().setTestTicks(5);
		int floor = helper.absolutePos(c).getY() + 1;
		BlockPos junctionAt = helper.absolutePos(c.offset(-2, 0, 14));
		CableJunctionBlockEntity junction = (CableJunctionBlockEntity) helper.getLevel().getBlockEntity(junctionAt);
		helper.assertTrue(junction.points().size() == 10 && junction.points().get(6).name().startsWith("TARGET Y") && junction.points().get(7).name().startsWith("FLAT LENGTH"),
				Component.literal("the junction takes a target Y and a flat length: " + junction.points()));
		auto(junction, junctionAt);
		double targetMa = 4 + 16 * PlcScale.targetFraction(floor);
		double[] mostSet = {0};
		helper.onEachTick(() -> {
			long now = helper.getLevel().getGameTime();
			TbmEntity machine = TailingsGameTests.machine(helper, c);
			boolean started = machine != null && machine.core().drill().advanceCount() >= 1;
			junction.commandMa(6, targetMa, now);
			junction.commandMa(7, started ? 20 : 4 + 16 * 3 / 400.0, now);
			junction.commandPoint(0, true, now);
			if (machine != null && machine.core().running()) {
				mostSet[0] = Math.max(mostSet[0], junction.readMa(9));
			}
		});
		helper.succeedWhen(() -> {
			DrillBlockEntity now = null;
			for (int z = 3; z >= -6 && now == null; z--) {
				now = helper.getLevel().getBlockEntity(helper.absolutePos(c.offset(3, 3, z))) instanceof DrillBlockEntity d ? d : null;
			}
			helper.assertTrue(now != null && TailingsGameTests.machine(helper, c) == null && now.status() == DrillCodes.STOP_TARGET && now.drill().advanceCount() == 3,
					Component.literal("it should drive its 3 m and stop at its target, got " + (now == null ? "no cab" : now.core().readout() + " / " + now.core().note() + ", "
							+ now.drill().advanceCount() + " advanced")));
			helper.assertTrue(now.program().targetY() == floor, Component.literal("its target is the PLC's, " + floor + ", got " + now.program().targetY()));
			helper.assertTrue(mostSet[0] > 4 && mostSet[0] < 4.2, Component.literal("while it drove, FLAT LENGTH SET read its 3 m (4.12 mA), most " + mostSet[0]));
			helper.assertTrue(Math.abs(junction.readMa(9) - 20) < 1e-6, Component.literal("stopped, it takes the PLC's new 400 m, reading " + junction.readMa(9)));
			helper.assertTrue(Math.abs(junction.readMa(8) - targetMa) < 0.05, Component.literal("TARGET Y SET reads its target, " + junction.readMa(8)));
		});
	}

	/**
	 * Fines slurry off a cyclone's overflow into a conditioning tank with nothing downstream. The cyclone's FLOW reads the tank drawing it
	 * at the plant's 31 L/s, the tank's DENSITY its 35 % solids (1.28 kg/L); when the tank's outlet is full its feed tank backs up, LEVEL
	 * reads full and the cyclone's flow stops.
	 */
	private static void slurry(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity cyclone = TailingsGameTests.fines(helper, new BlockPos(0, y, 0), 2000, Map.of("minecraft:copper_ingot", 0.01));
		TailingsGameTests.pipe(helper, new BlockPos(0, y, 2), new BlockPos(1, y, 2));
		PlantBlockEntity tank = TailingsGameTests.plant(helper, Machine.CONDITIONER, new BlockPos(0, y, 3));
		tank.forceRunning();
		TailingsGameTests.reagents(helper, tank, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC);
		helper.assertTrue(tank.points().size() == 9 && tank.points().get(6).name().startsWith("LEVEL") && tank.points().get(7).name().startsWith("FLOW")
				&& tank.points().get(8).name().startsWith("DENSITY"), Component.literal("a slurry part reads level, flow and density: " + tank.points()));
		PlantBlockEntity drive = TailingsGameTests.plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(8, y, 0));
		helper.assertTrue(drive.points().size() == 6, Component.literal("a dry part does not"));
		double density = PlcScale.density(Product.FINES_SLURRY);
		double densityMa = 4 + 16 * (density - 1);
		boolean[] steady = {false};
		helper.onEachTick(() -> {
			double flow = (cyclone.readMa(7) - 4) / 16 * PlcScale.FLOW_HIGH;
			if (flow > 28 && flow < 34 && "conditioning".equals(tank.status())) {
				steady[0] = true;
			}
		});
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(steady[0], Component.literal("the cyclone's FLOW should read the tank drawing 31 L/s, reads " + cyclone.flow() + " L/s (" + tank.status() + ")")))
				.thenExecute(() -> {
					helper.assertTrue(Math.abs(tank.readMa(8) - densityMa) < 0.05, Component.literal("the tank's DENSITY reads 1.28 kg/L, " + densityMa + " mA, got " + tank.readMa(8)));
					helper.assertTrue(Math.abs(cyclone.readMa(8) - densityMa) < 0.05, Component.literal("and the cyclone's, got " + cyclone.readMa(8)));
					helper.assertTrue(Math.abs(tank.measure(dev.wildcatter.control.Measurable.Quantity.DENSITY) - density) < 0.01, Component.literal("a transmitter reads it too"));
				})
				.thenWaitUntil(() -> helper.assertTrue(tank.readMa(6) > 19.8 && cyclone.readMa(7) < 4.01,
						Component.literal("its outlet full, the tank backs up to full and the flow stops: LEVEL " + tank.readMa(6) + " mA, FLOW " + cyclone.readMa(7) + " mA (" + tank.status() + ")")))
				.thenSucceed();
	}
}
