package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.DewateringPumpBlock;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.ChunkRunner;
import dev.wildcatter.tailings.block.MineRailBlock;
import dev.wildcatter.tailings.block.MineSwitchBlock;
import dev.wildcatter.tailings.block.RailBlockEntity;
import dev.wildcatter.tailings.block.RectifierBlockEntity;
import dev.wildcatter.tailings.block.HandMillBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.PlantMachineBlock;
import dev.wildcatter.tailings.client.MineCarModel;
import dev.wildcatter.tailings.client.MineLocomotiveModel;
import dev.wildcatter.tailings.client.RailSurface;
import dev.wildcatter.tailings.block.RailGrade;
import dev.wildcatter.tailings.block.SlopeBlock;
import dev.wildcatter.tailings.block.SlopeHalf;
import dev.wildcatter.tailings.block.TunnelDrillBlock;
import dev.wildcatter.tailings.mine.Advance;
import dev.wildcatter.tailings.mine.DriveCourse;
import dev.wildcatter.tailings.mine.DrillCodes;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantPlan;
import dev.wildcatter.tailings.plant.Reagent;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.Ticket;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.TicketStorage;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Headless scenes for the starter line. {@code ./gradlew :neoforge:runGameTestServer} */
@Mod("tailings_gametest")
public class TailingsGameTests {
	private static final String[] SCENES = {"registers", "stamp", "magnet", "jaw_size", "crushing", "flotation", "drill", "drill_pause", "haul", "models", "haul_turnout", "train_tickets", "drill_blocked", "plant_drops"};

	public TailingsGameTests(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, event -> event.register(Registries.TEST_FUNCTION, helper -> {
			helper.register(id("registers"), (Consumer<GameTestHelper>) TailingsGameTests::registers);
			helper.register(id("stamp"), (Consumer<GameTestHelper>) TailingsGameTests::stamp);
			helper.register(id("magnet"), (Consumer<GameTestHelper>) TailingsGameTests::magnet);
			helper.register(id("jaw_size"), (Consumer<GameTestHelper>) TailingsGameTests::jawSize);
			helper.register(id("crushing"), (Consumer<GameTestHelper>) TailingsGameTests::crushing);
			helper.register(id("flotation"), (Consumer<GameTestHelper>) TailingsGameTests::flotation);
			helper.register(id("drill"), (Consumer<GameTestHelper>) TailingsGameTests::drill);
			helper.register(id("drill_pause"), (Consumer<GameTestHelper>) TailingsGameTests::drillPause);
			helper.register(id("haul"), (Consumer<GameTestHelper>) TailingsGameTests::haul);
			helper.register(id("models"), (Consumer<GameTestHelper>) TailingsGameTests::models);
			helper.register(id("haul_turnout"), (Consumer<GameTestHelper>) TailingsGameTests::haulTurnout);
			helper.register(id("train_tickets"), (Consumer<GameTestHelper>) TailingsGameTests::trainTickets);
			helper.register(id("drill_blocked"), (Consumer<GameTestHelper>) TailingsGameTests::drillBlocked);
			helper.register(id("plant_drops"), (Consumer<GameTestHelper>) TailingsGameTests::plantDrops);
		}));
		modBus.addListener(RegisterGameTestsEvent.class, event -> {
			Holder<TestEnvironmentDefinition<?>> plain = event.registerEnvironment(id("plain"));
			for (String scene : SCENES) {
				event.registerTest(id(scene), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(scene)),
						new TestData<>(plain, Identifier.withDefaultNamespace("empty"), 400, 1, true)));
			}
		});
	}

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("tailings_gametest", path);
	}

	private static void registers(GameTestHelper helper) {
		helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Tailings.id("stamp_mill")), Component.literal("stamp mill missing"));
		helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Tailings.id("crushed_granite")), Component.literal("crushed granite missing"));
		helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Tailings.id("sand_iron")), Component.literal("iron sand missing"));
		helper.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(Tailings.id("tailings")), Component.literal("creative tab missing"));
		helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Tailings.id("tunnel_drill")), Component.literal("tunnel drill missing"));
		helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Tailings.id("mine_rail")), Component.literal("mine rail missing"));
		helper.assertTrue(BuiltInRegistries.MENU.containsKey(Tailings.id("tunnel_drill")), Component.literal("drill menu missing"));
		helper.succeed();
	}

	private static void stamp(GameTestHelper helper) {
		helper.setBlock(0, 0, 0, Blocks.STONE);
		helper.setBlock(0, 1, 0, TailingsBlocks.STAMP_MILL);
		HandMillBlockEntity mill = helper.getBlockEntity(new BlockPos(0, 1, 0), HandMillBlockEntity.class);
		helper.assertTrue(mill.offer(new ItemStack(Blocks.COBBLESTONE)), Component.literal("the stamp mill should take cobble"));
		helper.succeedWhen(() -> helper.assertTrue(mill.output(0).is(BuiltInRegistries.ITEM.getValue(Tailings.id("crushed_cobblestone"))),
				Component.literal("stamp mill should crush cobble, got " + mill.output(0))));
	}

	private static void magnet(GameTestHelper helper) {
		helper.setBlock(0, 0, 0, Blocks.STONE);
		helper.setBlock(0, 1, 0, TailingsBlocks.MAGNETIC_SEPARATOR);
		HandMillBlockEntity mill = helper.getBlockEntity(new BlockPos(0, 1, 0), HandMillBlockEntity.class);
		ItemStack sand = new ItemStack(BuiltInRegistries.ITEM.getValue(Tailings.id("sand_iron")));
		helper.assertTrue(mill.offer(sand), Component.literal("the separator should take iron sand"));
		helper.succeedWhen(() -> {
			ItemStack out = mill.output(0);
			helper.assertTrue(out.is(Items.IRON_INGOT) && out.getCount() == 2, Component.literal("magnetic separation should pay 2 iron ingots, got " + out));
		});
	}

	private static void erect(GameTestHelper helper, java.util.List<PlantPlan.Station> plan, int y) {
		for (PlantPlan.Station station : plan) {
			PlantMachineBlock block = (PlantMachineBlock) TailingsBlocks.plantBlock(station.machine());
			BlockPos pos = new BlockPos(station.x(), y, 0);
			BlockState state = block.defaultBlockState().setValue(PlantMachineBlock.FACING, Direction.NORTH).setValue(PlantMachineBlock.PIECE, 0);
			helper.setBlock(pos, state);
		}
	}

	private static PlantBlockEntity plant(GameTestHelper helper, java.util.List<PlantPlan.Station> plan, Machine machine, int y) {
		for (PlantPlan.Station station : plan) {
			if (station.machine() == machine) {
				return helper.getBlockEntity(new BlockPos(station.x(), y, 0), PlantBlockEntity.class);
			}
		}
		throw new IllegalStateException(machine.toString());
	}

	/** The jaw is one item and 3 x 4 x 4 cells. */
	private static void jawSize(GameTestHelper helper) {
		PlantMachineBlock jaw = (PlantMachineBlock) TailingsBlocks.JAW;
		BlockPos pos = new BlockPos(0, 2, 0);
		BlockState state = jaw.defaultBlockState().setValue(PlantMachineBlock.FACING, Direction.NORTH).setValue(PlantMachineBlock.PIECE, 0);
		helper.setBlock(pos, state);
		jaw.fill(helper.getLevel(), helper.absolutePos(pos), state);
		int cells = 0;
		for (BlockPos cell : jaw.cells(helper.absolutePos(pos), Direction.NORTH)) {
			if (helper.getLevel().getBlockState(cell).getBlock() == jaw) {
				cells++;
			}
		}
		helper.assertTrue(cells == Machine.JAW.cells(), Component.literal("jaw should fill " + Machine.JAW.cells() + " cells, got " + cells));
		helper.succeed();
	}

	private static void crushing(GameTestHelper helper) {
		int y = 6;
		erect(helper, PlantPlan.CRUSHING, y);
		PlantBlockEntity jaw = plant(helper, PlantPlan.CRUSHING, Machine.JAW, y);
		jaw.refresh();
		jaw.forceRunning();
		helper.assertTrue(plant(helper, PlantPlan.CRUSHING, Machine.GRIZZLY, y).offer(new ItemStack(Blocks.COBBLESTONE)),
				Component.literal("the grizzly should take cobble"));
		helper.succeedWhen(() -> {
			ItemStack out = plant(helper, PlantPlan.CRUSHING, Machine.CYCLONE, y).product();
			helper.assertTrue(jaw.formed() && out.is(BuiltInRegistries.ITEM.getValue(Tailings.id("fines_cobblestone"))),
					Component.literal("the circuit should make fines, got " + out + " (" + jaw.status() + ")"));
		});
	}

	private static void flotation(GameTestHelper helper) {
		int y = 16;
		erect(helper, PlantPlan.FLOTATION, y);
		PlantBlockEntity tank = plant(helper, PlantPlan.FLOTATION, Machine.CONDITIONER, y);
		tank.forceRunning();
		ItemStack fines = new ItemStack(BuiltInRegistries.ITEM.getValue(Tailings.id("fines_copper")));
		helper.assertTrue(tank.offer(fines), Component.literal("the tank should take copper fines"));
		for (Reagent reagent : new Reagent[] {Reagent.LIME, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC}) {
			helper.assertTrue(tank.offer(new ItemStack(BuiltInRegistries.ITEM.getValue(Tailings.id(reagent.id())))),
					Component.literal("the tank should take " + reagent.displayName()));
		}
		helper.succeedWhen(() -> {
			ItemStack out = plant(helper, PlantPlan.FLOTATION, Machine.FILTER, 16).product();
			helper.assertTrue(tank.formed() && out.is(Items.COPPER_INGOT) && out.getCount() == 3,
					Component.literal("flotation should pay 3 copper ingots, got " + out + " formed=" + tank.formed()));
		});
	}

	/** One advance of the TBM: 28 lining segments and the upper half of the 1-in-2 floor. */
	private static void drill(GameTestHelper helper) {
		BlockPos cab = new BlockPos(10, 40, 12);
		helper.setBlock(cab, TailingsBlocks.TUNNEL_DRILL.defaultBlockState().setValue(TunnelDrillBlock.FACING, Direction.NORTH));
		helper.setBlock(cab.south(), TailingsBlocks.DEWATERING_PUMP.defaultBlockState().setValue(DewateringPumpBlock.FACING, Direction.NORTH)
				.setValue(DewateringPumpBlock.PIECE, 0));
		DrillBlockEntity drill = helper.getBlockEntity(cab, DrillBlockEntity.class);
		drill.forceRunning();
		drill.program().setTargetY(30);
		drill.program().setFlatLength(0);
		drill.setItem(DrillBlockEntity.SLOT_RAIL, new ItemStack(TailingsBlocks.MINE_RAIL, 64));
		drill.setItem(DrillBlockEntity.SLOT_GRAVEL, new ItemStack(Items.GRAVEL, 64));
		drill.setItem(DrillBlockEntity.SLOT_COBBLE, new ItemStack(Items.COBBLESTONE, 64));
		drill.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
		BlockPos at = helper.absolutePos(cab);
		drill.drill().placeOriginFrom(at.getX(), at.getY(), at.getZ());
		ServerLevel level = helper.getLevel();
		for (int up = 0; up < DriveCourse.FACE; up++) {
			for (int across = 0; across < DriveCourse.FACE; across++) {
				level.setBlock(drill.cell(across, up, 0), Blocks.STONE.defaultBlockState(), 3);
			}
		}
		for (int across = 1; across <= DriveCourse.CLEAR; across++) {
			level.setBlock(drill.cell(across, 0, 0).below(), Blocks.STONE.defaultBlockState(), 3);
		}
		Advance drove = drill.stepNow();
		helper.assertTrue(drove.placed(), Component.literal("the drill should advance (" + drove.stop() + " " + drove.detail() + ")"));
		int lining = 0;
		for (int up = 0; up < DriveCourse.FACE; up++) {
			for (int across = 0; across < DriveCourse.FACE; across++) {
				if (level.getBlockState(drill.cell(across, up, -1)).getBlock() == TailingsBlocks.LINING) {
					lining++;
				}
			}
		}
		helper.assertTrue(lining == DriveCourse.SEGMENTS_PER_RING, Component.literal("expected 28 lining segments, got " + lining));
		BlockState slope = level.getBlockState(drill.cell(1, 1, -1));
		BlockState rail = level.getBlockState(drill.cell(2, 1, -1));
		helper.assertTrue(slope.getBlock() == TailingsBlocks.SLOPE && slope.getValue(SlopeBlock.HALF) == SlopeHalf.UPPER,
				Component.literal("column 1 should be an upper slope, got " + slope));
		helper.assertTrue(rail.getBlock() == TailingsBlocks.MINE_RAIL && rail.getValue(MineRailBlock.GRADE) == RailGrade.UPPER && rail.getValue(MineRailBlock.RACK),
				Component.literal("column 2 should be rack rail on the upper half, got " + rail));
		helper.assertTrue(level.getBlockState(drill.cell(3, 1, -1)).getBlock() == TailingsBlocks.MINE_STAIR, Component.literal("column 3 should be a stair"));
		helper.assertTrue(level.getBlockState(drill.cell(4, 1, -1)).getBlock() == TailingsBlocks.MINE_STAIR, Component.literal("column 4 should be a stair"));
		helper.assertTrue(level.getBlockState(drill.cell(5, 1, -1)).getBlock() == TailingsBlocks.MINE_RAIL, Component.literal("column 5 should be rail"));
		helper.assertTrue(level.getBlockState(drill.cell(6, 1, -1)).getBlock() == TailingsBlocks.SLOPE, Component.literal("column 6 should be a slope"));
		DrillBlockEntity now = helper.getBlockEntity(cab.north().above(), DrillBlockEntity.class);
		ItemStack muck = now.getItem(DrillBlockEntity.INPUTS);
		helper.assertTrue(muck.is(Items.COBBLESTONE) && muck.getCount() == 58, Component.literal("stone should come out as 58 cobble, got " + muck));
		helper.assertTrue(now.getItem(DrillBlockEntity.SLOT_SEGMENTS).getCount() == 36, Component.literal("a ring spends 28 segments"));
		helper.assertTrue(now.getItem(DrillBlockEntity.SLOT_RAIL).getCount() == 62, Component.literal("an advance spends 2 rails"));
		helper.assertTrue(helper.getBlockState(cab).getBlock() != TailingsBlocks.TUNNEL_DRILL, Component.literal("the cab should leave its starting block"));
		helper.assertTrue(helper.getBlockState(cab.north().above()).getBlock() == TailingsBlocks.TUNNEL_DRILL,
				Component.literal("the cab should ride forward into the bore"));
		helper.succeed();
	}

	/** A full muck buffer pauses the advance. Emptying it lets the machine go on. */
	private static void drillPause(GameTestHelper helper) {
		BlockPos cab = new BlockPos(10, 70, 12);
		helper.setBlock(cab, TailingsBlocks.TUNNEL_DRILL.defaultBlockState().setValue(TunnelDrillBlock.FACING, Direction.NORTH));
		helper.setBlock(cab.south(), TailingsBlocks.DEWATERING_PUMP.defaultBlockState().setValue(DewateringPumpBlock.FACING, Direction.NORTH)
				.setValue(DewateringPumpBlock.PIECE, 0));
		DrillBlockEntity drill = helper.getBlockEntity(cab, DrillBlockEntity.class);
		drill.forceRunning();
		drill.program().setTargetY(60);
		drill.setItem(DrillBlockEntity.SLOT_RAIL, new ItemStack(TailingsBlocks.MINE_RAIL, 64));
		drill.setItem(DrillBlockEntity.SLOT_GRAVEL, new ItemStack(Items.GRAVEL, 64));
		drill.setItem(DrillBlockEntity.SLOT_COBBLE, new ItemStack(Items.COBBLESTONE, 64));
		drill.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
		for (int slot = DrillBlockEntity.INPUTS; slot < DrillBlockEntity.SLOTS; slot++) {
			drill.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
		}
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(cab);
		drill.drill().placeOriginFrom(at.getX(), at.getY(), at.getZ());
		for (int up = 0; up < DriveCourse.FACE; up++) {
			for (int across = 0; across < DriveCourse.FACE; across++) {
				level.setBlock(drill.cell(across, up, 0), Blocks.STONE.defaultBlockState(), 3);
			}
		}
		Advance paused = drill.stepNow();
		helper.assertTrue(paused.stop() == Advance.Stop.BUFFER_FULL, Component.literal("a full buffer should pause the drill, got " + paused.stop()));
		helper.assertTrue(helper.getBlockState(cab).getBlock() == TailingsBlocks.TUNNEL_DRILL, Component.literal("a pause should leave the cab where it is"));
		for (int slot = DrillBlockEntity.INPUTS; slot < DrillBlockEntity.SLOTS; slot++) {
			drill.setItem(slot, ItemStack.EMPTY);
		}
		Advance drove = drill.stepNow();
		helper.assertTrue(drove.placed(), Component.literal("emptying the buffer should let the drill resume"));
		helper.assertTrue(helper.getBlockState(cab.north().above()).getBlock() == TailingsBlocks.TUNNEL_DRILL, Component.literal("the resumed drill should move"));
		helper.succeed();
	}

	/** The rectifier feeds the rail, the train moves one block, and the dump splits the car into cobble. */
	private static void haul(GameTestHelper helper) {
		BlockPos rectifier = new BlockPos(4, 60, 2);
		helper.setBlock(rectifier, TailingsBlocks.TRACTION_RECTIFIER.defaultBlockState());
		RectifierBlockEntity box = helper.getBlockEntity(rectifier, RectifierBlockEntity.class);
		box.forceRunning();
		BlockState rail = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = 0; z <= 3; z++) {
			helper.setBlock(new BlockPos(3, 60, z), rail);
		}
		box.spread();
		RailBlockEntity loco = helper.getBlockEntity(new BlockPos(3, 60, 2), RailBlockEntity.class);
		RailBlockEntity car = helper.getBlockEntity(new BlockPos(3, 60, 3), RailBlockEntity.class);
		helper.assertTrue(loco.powered() && car.powered(), Component.literal("the rectifier should feed the rails it touches"));
		helper.assertTrue(loco.placeLoco(), Component.literal("the rail should take a locomotive"));
		helper.assertTrue(car.placeCar(), Component.literal("the car should couple to the locomotive ahead"));
		helper.assertTrue(loco.conductorLive(), Component.literal("the conductor is live under the locomotive"));
		car.load().add("minecraft:cobblestone", 2);
		helper.assertTrue(loco.stepOnce(), Component.literal("the train should take the next powered rail"));
		RailBlockEntity moved = helper.getBlockEntity(new BlockPos(3, 60, 1), RailBlockEntity.class);
		RailBlockEntity followed = helper.getBlockEntity(new BlockPos(3, 60, 2), RailBlockEntity.class);
		helper.assertTrue(moved.kind() == RailBlockEntity.Kind.LOCO && moved.conductorLive(), Component.literal("the locomotive should have moved and stayed live"));
		helper.assertTrue(followed.kind() == RailBlockEntity.Kind.CAR && followed.load().total() == 2, Component.literal("the car should follow with its load"));
		helper.setBlock(new BlockPos(3, 60, 0), TailingsBlocks.DUMP_STATION.defaultBlockState());
		helper.assertTrue(moved.stepOnce(), Component.literal("the train should tip into the dump"));
		DumpBlockEntity dump = helper.getBlockEntity(new BlockPos(3, 60, 0), DumpBlockEntity.class);
		ItemStack waste = dump.getItem(0);
		helper.assertTrue(waste.getItem() == Items.COBBLESTONE && waste.getCount() == 2, Component.literal("the sorter should put cobble in the waste bin, got " + waste));
		helper.assertTrue(followed.load().total() == 0, Component.literal("the car should be empty after the tip"));
		helper.succeed();
	}

	/** The entity layers bake, and the rail surface meets across a 1-in-2 pair. */
	private static void models(GameTestHelper helper) {
		var loco = MineLocomotiveModel.createBodyLayer().bakeRoot();
		helper.assertFalse(loco.getChild(MineLocomotiveModel.HEADLIGHT).isEmpty(), Component.literal("the locomotive should have a headlight cube"));
		helper.assertTrue(loco.hasChild("cab") && loco.hasChild("wheel_fl"), Component.literal("the locomotive layer should keep its body parts"));
		var car = MineCarModel.createBodyLayer().bakeRoot();
		helper.assertFalse(car.getChild(MineCarModel.LOAD).isEmpty(), Component.literal("the car should have a load cube"));
		car.getChild(MineCarModel.LOAD).visible = false;
		helper.assertFalse(car.getChild(MineCarModel.LOAD).visible, Component.literal("the load cube should be hideable"));
		helper.assertTrue(Math.abs(RailSurface.blocksAboveBottom(RailGrade.LOWER, 1) - 0.5) < 1.0e-6, Component.literal("lower rail should end at half a block"));
		helper.assertTrue(Math.abs(RailSurface.blocksAboveBottom(RailGrade.UPPER, 0) - 0.5) < 1.0e-6, Component.literal("upper rail should start at half a block"));
		helper.assertTrue(Math.abs(RailSurface.alongDownhill(Direction.NORTH, 4, 8, 4.5, 8)) < 1.0e-6, Component.literal("north downhill starts at the north edge"));
		helper.succeed();
	}

	/** A locomotive and two cars run through a thrown switch onto a branch; the consist stays whole and the track behind clears. */
	private static void haulTurnout(GameTestHelper helper) {
		int y = 60;
		BlockPos rectifier = new BlockPos(2, y, 3);
		helper.setBlock(rectifier, TailingsBlocks.TRACTION_RECTIFIER.defaultBlockState());
		RectifierBlockEntity box = helper.getBlockEntity(rectifier, RectifierBlockEntity.class);
		box.forceRunning();
		BlockState north = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		BlockState east = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.EAST);
		for (int z = 2; z <= 5; z++) {
			helper.setBlock(new BlockPos(3, y, z), north);
		}
		helper.setBlock(new BlockPos(3, y, 1), TailingsBlocks.MINE_SWITCH.defaultBlockState().setValue(MineSwitchBlock.FACING, Direction.NORTH)
				.setValue(MineSwitchBlock.THROWN, true));
		for (int x = 4; x <= 6; x++) {
			helper.setBlock(new BlockPos(x, y, 1), east);
		}
		box.spread();
		RailBlockEntity loco = helper.getBlockEntity(new BlockPos(3, y, 3), RailBlockEntity.class);
		helper.assertTrue(loco.placeLoco(), Component.literal("the rail should take a locomotive"));
		helper.assertTrue(helper.getBlockEntity(new BlockPos(3, y, 4), RailBlockEntity.class).placeCar(), Component.literal("first car should couple"));
		helper.assertTrue(helper.getBlockEntity(new BlockPos(3, y, 5), RailBlockEntity.class).placeCar(), Component.literal("second car should couple"));
		BlockPos at = new BlockPos(3, y, 3);
		BlockPos[] path = {new BlockPos(3, y, 2), new BlockPos(3, y, 1), new BlockPos(4, y, 1), new BlockPos(5, y, 1)};
		for (BlockPos next : path) {
			RailBlockEntity head = helper.getBlockEntity(at, RailBlockEntity.class);
			var dest = helper.getLevel().getBlockEntity(helper.absolutePos(next));
			helper.assertTrue(head.stepOnce(), Component.literal("the train should step from " + at + " to " + dest + " (powered "
					+ head.powered() + "/" + (dest instanceof RailBlockEntity r ? r.powered() + " " + r.kind() : "-") + ")"));
			at = next;
			RailBlockEntity now = helper.getBlockEntity(at, RailBlockEntity.class);
			helper.assertTrue(now.kind() == RailBlockEntity.Kind.LOCO, Component.literal("the locomotive should be at " + at));
			helper.assertTrue(now.consist().size() == 3, Component.literal("the consist should stay three long at " + at + ", got " + now.consist().size()));
		}
		helper.assertTrue(helper.getBlockEntity(new BlockPos(4, y, 1), RailBlockEntity.class).kind() == RailBlockEntity.Kind.CAR
				&& helper.getBlockEntity(new BlockPos(3, y, 1), RailBlockEntity.class).kind() == RailBlockEntity.Kind.CAR,
				Component.literal("both cars should follow the locomotive through the turnout"));
		for (int z = 2; z <= 5; z++) {
			helper.assertTrue(helper.getBlockEntity(new BlockPos(3, y, z), RailBlockEntity.class).kind() == RailBlockEntity.Kind.NONE,
					Component.literal("the main line behind the switch should be clear at z " + z));
		}
		helper.succeed();
	}

	private static boolean hasTicket(ServerLevel level, BlockPos pos, TicketType type) {
		TicketStorage tickets = level.getDataStorage().computeIfAbsent(TicketStorage.TYPE);
		for (Ticket ticket : tickets.getTickets(ChunkPos.containing(pos).pack())) {
			if (ticket.getType() == type) {
				return true;
			}
		}
		return false;
	}

	/** A running train holds a runner ticket on its chunk, and the ticket times out once the train is gone. */
	private static void trainTickets(GameTestHelper helper) {
		int y = 60;
		BlockState north = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = 0; z <= 2; z++) {
			helper.setBlock(new BlockPos(3, y, z), north);
		}
		BlockPos rail = new BlockPos(3, y, 2);
		BlockPos abs = helper.absolutePos(rail);
		ServerLevel level = helper.getLevel();
		helper.startSequence()
				.thenExecute(() -> helper.assertTrue(helper.getBlockEntity(rail, RailBlockEntity.class).placeLoco(), Component.literal("the rail should take a locomotive")))
				.thenExecuteAfter(5, () -> {
					helper.assertTrue(hasTicket(level, abs, ChunkRunner.RUNNER), Component.literal("a running train should hold a runner ticket"));
					// take the locomotive off: its entity goes on its next tick
					helper.setBlock(rail, Blocks.AIR);
				})
				.thenExecuteAfter((int) ChunkRunner.TIMEOUT_TICKS + ChunkRunner.REFRESH_TICKS + 10,
						() -> helper.assertFalse(hasTicket(level, abs, ChunkRunner.RUNNER), Component.literal("the runner ticket should time out once the train is gone")))
				.thenSucceed();
	}

	/** Something solid where the cab would move stops the drill as blocked and overwrites nothing; clearing it lets the cab follow. */
	private static void drillBlocked(GameTestHelper helper) {
		BlockPos cab = new BlockPos(10, 100, 12);
		helper.setBlock(cab, TailingsBlocks.TUNNEL_DRILL.defaultBlockState().setValue(TunnelDrillBlock.FACING, Direction.NORTH));
		helper.setBlock(cab.south(), TailingsBlocks.DEWATERING_PUMP.defaultBlockState().setValue(DewateringPumpBlock.FACING, Direction.NORTH)
				.setValue(DewateringPumpBlock.PIECE, 0));
		DrillBlockEntity drill = helper.getBlockEntity(cab, DrillBlockEntity.class);
		drill.forceRunning();
		drill.program().setTargetY(90);
		drill.setItem(DrillBlockEntity.SLOT_RAIL, new ItemStack(TailingsBlocks.MINE_RAIL, 64));
		drill.setItem(DrillBlockEntity.SLOT_GRAVEL, new ItemStack(Items.GRAVEL, 64));
		drill.setItem(DrillBlockEntity.SLOT_COBBLE, new ItemStack(Items.COBBLESTONE, 64));
		drill.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
		ServerLevel level = helper.getLevel();
		BlockPos at = helper.absolutePos(cab);
		drill.drill().placeOriginFrom(at.getX(), at.getY(), at.getZ());
		for (int up = 0; up < DriveCourse.FACE; up++) {
			for (int across = 0; across < DriveCourse.FACE; across++) {
				level.setBlock(drill.cell(across, up, 0), Blocks.STONE.defaultBlockState(), 3);
			}
		}
		// the pump's upper cells move into the cell above the cab
		BlockPos wall = cab.above();
		helper.setBlock(wall, Blocks.OBSIDIAN);
		Advance stopped = drill.stepNow();
		helper.assertTrue(stopped.stop() == Advance.Stop.BLOCKED && drill.status() == DrillCodes.STOP_BLOCKED,
				Component.literal("a solid block in the way should stop the drill as blocked, got " + stopped.stop()));
		helper.assertTrue(helper.getBlockState(cab).getBlock() == TailingsBlocks.TUNNEL_DRILL, Component.literal("the cab should not move"));
		helper.assertTrue(helper.getBlockState(wall).getBlock() == Blocks.OBSIDIAN, Component.literal("the obsidian should not be overwritten"));
		helper.setBlock(wall, Blocks.AIR);
		drill.stepNow();
		helper.assertTrue(helper.getBlockState(cab.north().above()).getBlock() == TailingsBlocks.TUNNEL_DRILL,
				Component.literal("with the way clear the cab should follow the face"));
		helper.succeed();
	}

	/** A plant machine removed by anything but a player (here a plain removal, as an explosion does) still drops what it holds. */
	private static void plantDrops(GameTestHelper helper) {
		int y = 30;
		PlantMachineBlock block = (PlantMachineBlock) TailingsBlocks.plantBlock(Machine.CONDITIONER);
		BlockPos pos = new BlockPos(1, y, 1);
		helper.setBlock(pos, block.defaultBlockState().setValue(PlantMachineBlock.FACING, Direction.NORTH).setValue(PlantMachineBlock.PIECE, 0));
		PlantBlockEntity tank = helper.getBlockEntity(pos, PlantBlockEntity.class);
		var lime = BuiltInRegistries.ITEM.getValue(Tailings.id(Reagent.LIME.id()));
		for (int i = 0; i < 3; i++) {
			helper.assertTrue(tank.offer(new ItemStack(lime)), Component.literal("the tank should take lime"));
		}
		helper.getLevel().removeBlock(helper.absolutePos(pos), false);
		helper.assertItemEntityCountIs(lime, pos, 3.0, 3);
		helper.succeed();
	}
}
