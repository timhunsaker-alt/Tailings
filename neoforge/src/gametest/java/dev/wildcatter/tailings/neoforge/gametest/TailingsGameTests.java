package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
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
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Entity;
import dev.wildcatter.tailings.mine.Heading;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.rock.SizeClass;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.fluid.Product;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.RegisterEvent;

/** Headless scenes for the starter line. {@code ./gradlew :neoforge:runGameTestServer} */
@Mod("tailings_gametest")
public class TailingsGameTests {
	private static final String[] SCENES = {"registers", "stamp", "magnet", "jaw_size", "crushing_circuit", "bin_feeds", "belt_drive", "flotation", "flotation_stops", "dump_station", "dump_waits", "drill", "drill_pause", "haul", "models", "train_runs", "train_turnout", "train_decline", "train_derail", "train_harm", "train_migrate", "train_tickets", "drill_blocked", "plant_drops", "part_break", "not_built", "terminal"};

	/** Scenes where trains run the length of a track at their real speed. */
	private static final java.util.Set<String> LONG = java.util.Set.of("haul", "train_runs", "train_turnout", "train_decline", "train_derail", "train_harm",
			"dump_station", "dump_waits");

	public TailingsGameTests(IEventBus modBus) {
		modBus.addListener(RegisterEvent.class, event -> event.register(Registries.TEST_FUNCTION, helper -> {
			helper.register(id("registers"), loaded(TailingsGameTests::registers));
			helper.register(id("stamp"), loaded(TailingsGameTests::stamp));
			helper.register(id("magnet"), loaded(TailingsGameTests::magnet));
			helper.register(id("jaw_size"), loaded(TailingsGameTests::jawSize));
			helper.register(id("crushing_circuit"), loaded(TailingsGameTests::crushingCircuit));
			helper.register(id("bin_feeds"), loaded(TailingsGameTests::binFeeds));
			helper.register(id("belt_drive"), loaded(TailingsGameTests::beltDrive));
			helper.register(id("flotation"), loaded(TailingsGameTests::flotation));
			helper.register(id("flotation_stops"), loaded(TailingsGameTests::flotationStops));
			helper.register(id("dump_station"), loaded(TailingsGameTests::dumpStation));
			helper.register(id("dump_waits"), loaded(TailingsGameTests::dumpWaits));
			helper.register(id("drill"), loaded(TailingsGameTests::drill));
			helper.register(id("drill_pause"), loaded(TailingsGameTests::drillPause));
			helper.register(id("haul"), loaded(TailingsGameTests::haul));
			helper.register(id("models"), loaded(TailingsGameTests::models));
			helper.register(id("train_runs"), loaded(TailingsGameTests::trainRuns));
			helper.register(id("train_turnout"), loaded(TailingsGameTests::trainTurnout));
			helper.register(id("train_decline"), loaded(TailingsGameTests::trainDecline));
			helper.register(id("train_derail"), loaded(TailingsGameTests::trainDerail));
			helper.register(id("train_harm"), loaded(TailingsGameTests::trainHarm));
			helper.register(id("train_migrate"), loaded(TailingsGameTests::trainMigrate));
			helper.register(id("train_tickets"), loaded(TailingsGameTests::trainTickets));
			helper.register(id("drill_blocked"), loaded(TailingsGameTests::drillBlocked));
			helper.register(id("plant_drops"), loaded(TailingsGameTests::plantDrops));
			helper.register(id("part_break"), loaded(TailingsGameTests::partBreak));
			helper.register(id("not_built"), loaded(TailingsGameTests::notBuilt));
			helper.register(id("terminal"), loaded(TailingsGameTests::terminal));
		}));
		modBus.addListener(RegisterGameTestsEvent.class, event -> {
			Holder<TestEnvironmentDefinition<?>> plain = event.registerEnvironment(id("plain"));
			for (String scene : SCENES) {
				event.registerTest(id(scene), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(scene)),
						// the plant's parts are true size: padding keeps each scene clear of the next
						new TestData<>(plain, net.minecraft.world.level.Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), LONG.contains(scene) ? 1600 : 400, 1, true,
								net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, 40)));
			}
		});
	}

	/**
	 * Runs a scene with the chunks around it loaded and ticking: the padding puts each scene on its own ground, and a true-size plant
	 * reaches past the chunk the test starts in.
	 */
	private static Consumer<GameTestHelper> loaded(Consumer<GameTestHelper> scene) {
		return helper -> {
			ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
			for (int dx = -1; dx <= 2; dx++) {
				for (int dz = -1; dz <= 2; dz++) {
					helper.getLevel().setChunkForced(origin.x() + dx, origin.z() + dz, true);
				}
			}
			scene.accept(helper);
		};
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

	/** Places a whole part as a player would: its main cell, then the rest grown into place. */
	private static <T extends net.minecraft.world.level.block.entity.BlockEntity> T part(GameTestHelper helper, Block block, BlockPos pos, Class<T> entity) {
		BlockState state = block.defaultBlockState().setValue(BigPartBlock.FACING, Direction.NORTH);
		helper.setBlock(pos, state);
		BigParts.grow(helper.getLevel(), helper.absolutePos(pos), helper.getLevel().getBlockState(helper.absolutePos(pos)));
		return entity == null ? null : helper.getBlockEntity(pos, entity);
	}

	private static PlantBlockEntity plant(GameTestHelper helper, Machine machine, BlockPos pos) {
		return part(helper, TailingsBlocks.plantBlock(machine), pos, PlantBlockEntity.class);
	}

	/** The jaw is one item and 3 x 4 x 4 cells. */
	private static void jawSize(GameTestHelper helper) {
		PlantMachineBlock jaw = (PlantMachineBlock) TailingsBlocks.JAW;
		BlockPos pos = new BlockPos(0, 2, 0);
		plant(helper, Machine.JAW, pos);
		int cells = 0;
		for (BlockPos cell : BigParts.cells(helper.absolutePos(pos), helper.getLevel().getBlockState(helper.absolutePos(pos)))) {
			if (helper.getLevel().getBlockState(cell).getBlock() == jaw) {
				cells++;
			}
		}
		helper.assertTrue(cells == Machine.JAW.cells(), Component.literal("jaw should fill " + Machine.JAW.cells() + " cells, got " + cells));
		helper.succeed();
	}

	private static BlockState belt(Direction.Axis axis) {
		return TailingsBlocks.BELT_CONVEYOR.defaultBlockState().setValue(BeltConveyorBlock.AXIS, axis);
	}

	private static void pipe(GameTestHelper helper, BlockPos... at) {
		Block steel = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "gas_line"));
		for (BlockPos pos : at) {
			helper.setBlock(pos, steel);
		}
	}

	/**
	 * The crushing circuit at 50 t/h (docs/multiblocks/crushing-circuit.md, game test checks 1-3): copper ore on the grizzly, the jaw
	 * under it, a belt climbing to the screen, the screen's oversize into the cone and its sand by belt onto the ball mill, mill
	 * discharge by steel pipe to the cyclones, and fines slurry at the cyclones carrying the copper ore's mineral.
	 */
	private static void crushingCircuit(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity jaw = plant(helper, Machine.JAW, new BlockPos(0, y, 0));
		PlantBlockEntity grizzly = plant(helper, Machine.GRIZZLY, new BlockPos(0, y + 4, 0));
		// the jaw's discharge belt, east along its south face and up two blocks to under the screen
		for (BlockPos at : new BlockPos[] {new BlockPos(0, y, 4), new BlockPos(1, y, 4), new BlockPos(2, y, 4), new BlockPos(3, y, 4),
				new BlockPos(4, y + 1, 4), new BlockPos(5, y + 2, 4)}) {
			helper.setBlock(at, belt(Direction.Axis.X));
		}
		PlantBlockEntity jawDrive = plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(6, y + 2, 4));
		PlantBlockEntity screen = plant(helper, Machine.SCREEN, new BlockPos(3, y + 3, 0));
		PlantBlockEntity cone = plant(helper, Machine.CONE, new BlockPos(3, y + 3, 6));
		// the sand belt, off the screen's east face and over the mill
		for (int x = 6; x <= 8; x++) {
			helper.setBlock(new BlockPos(x, y + 3, 1), belt(Direction.Axis.X));
		}
		PlantBlockEntity sandDrive = plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(9, y + 3, 1));
		PlantBlockEntity mill = plant(helper, Machine.BALL_MILL, new BlockPos(7, y - 1, 0));
		PlantBlockEntity cyclone = plant(helper, Machine.CYCLONE, new BlockPos(11, y - 1, 2));
		pipe(helper, new BlockPos(10, y - 1, 6), new BlockPos(11, y - 1, 6), new BlockPos(12, y - 1, 6), new BlockPos(13, y - 1, 6),
				new BlockPos(13, y - 1, 5), new BlockPos(13, y - 1, 4), new BlockPos(13, y - 1, 3), new BlockPos(13, y - 1, 2),
				new BlockPos(13, y - 1, 1), new BlockPos(12, y - 1, 1));
		for (PlantBlockEntity part : new PlantBlockEntity[] {jaw, screen, cone, jawDrive, sandDrive, mill, cyclone}) {
			part.forceRunning();
		}
		helper.onEachTick(() -> {
			mill.addWater(1000);
			cyclone.addWater(1000);
		});
		helper.assertTrue(grizzly.offer(new ItemStack(Blocks.COPPER_ORE)), Component.literal("the grizzly should take copper ore"));
		helper.succeedWhen(() -> {
			double fines = cyclone.out().held(Product.FINES_SLURRY);
			double copper = cyclone.out().assayOf(Product.FINES_SLURRY).get("minecraft:copper_ingot");
			helper.assertTrue(cone.hold().total() > 0, Component.literal("the cone should get the screen's oversize (" + screen.status() + ")"));
			helper.assertTrue(fines > 0 && copper > 0, Component.literal("fines slurry with copper should leave the cyclones, got " + fines
					+ " kg (jaw " + jaw.status() + "; screen " + screen.status() + "; mill " + mill.status() + "; cyclone " + cyclone.status() + ")"));
		});
	}

	/** An ore bin feeds the belt under it, and the belt drops the rock onto the grizzly under its head end. */
	private static void binFeeds(GameTestHelper helper) {
		int y = 6;
		PlantBlockEntity bin = plant(helper, Machine.ORE_BIN, new BlockPos(0, y + 1, 0));
		for (int x = 0; x <= 3; x++) {
			helper.setBlock(new BlockPos(x, y, 1), belt(Direction.Axis.X));
		}
		PlantBlockEntity drive = plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(4, y, 1));
		PlantBlockEntity grizzly = plant(helper, Machine.GRIZZLY, new BlockPos(3, y - 2, 0));
		drive.forceRunning();
		helper.assertTrue(bin.offer(new ItemStack(Blocks.COBBLESTONE, 2)), Component.literal("the bin should take cobble"));
		helper.succeedWhen(() -> helper.assertTrue(grizzly.hold().total() > 0,
				Component.literal("cobble should reach the grizzly (bin " + bin.status() + "; drive " + drive.status() + ")")));
	}

	/** A belt moves only with enough running drive, and stops taking when full (FULL). */
	private static void beltDrive(GameTestHelper helper) {
		int y = 6;
		// a 31 m run with one drive needs two
		for (int x = 0; x < 31; x++) {
			helper.setBlock(new BlockPos(x, y, 0), belt(Direction.Axis.X));
		}
		PlantBlockEntity longDrive = plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(31, y, 0));
		longDrive.forceRunning();
		// a drive with no power
		for (int x = 0; x < 3; x++) {
			helper.setBlock(new BlockPos(x, y, 4), belt(Direction.Axis.X));
		}
		PlantBlockEntity dead = plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(3, y, 4));
		// a full sand belt onto a full, stopped mill: the screen feeding it reads FULL
		PlantBlockEntity screen = plant(helper, Machine.SCREEN, new BlockPos(0, y, 10));
		for (int x = 3; x <= 5; x++) {
			helper.setBlock(new BlockPos(x, y + 1, 11), belt(Direction.Axis.X));
		}
		PlantBlockEntity sandDrive = plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(6, y + 1, 11));
		PlantBlockEntity mill = plant(helper, Machine.BALL_MILL, new BlockPos(4, y - 3, 10));
		PlantBlockEntity cone = plant(helper, Machine.CONE, new BlockPos(0, y, 16));
		screen.forceRunning();
		sandDrive.forceRunning();
		cone.forceRunning();
		RockLoad fullMill = new RockLoad();
		fullMill.add(Rock.GRANITE, SizeClass.SAND, PlantRates.DRY_HOLD_KG);
		mill.receive(fullMill);
		RockLoad fullBelt = new RockLoad();
		fullBelt.add(Rock.GRANITE, SizeClass.SAND, PlantRates.BELT_HOLD_KG);
		sandDrive.receive(fullBelt);
		helper.assertTrue(screen.offer(new ItemStack(BuiltInRegistries.ITEM.getValue(Tailings.id("crushed_granite")))),
				Component.literal("the screen should take crushed granite"));
		helper.succeedWhen(() -> {
			helper.assertTrue(longDrive.status().startsWith("NO DRIVE") && longDrive.status().contains("2 drives"),
					Component.literal("a 31 m run should want 2 drives, got " + longDrive.status()));
			helper.assertTrue(dead.status().startsWith("NO DRIVE"), Component.literal("an unpowered drive should read NO DRIVE, got " + dead.status()));
			helper.assertTrue(screen.status().startsWith("FULL"), Component.literal("the screen should read FULL, got " + screen.status()));
		});
	}

	/** A stand-in for the crushing circuit's overflow: cyclones holding fines slurry of this assay at their south face. */
	private static PlantBlockEntity fines(GameTestHelper helper, BlockPos pos, double kg, java.util.Map<String, Double> assay) {
		PlantBlockEntity source = plant(helper, Machine.CYCLONE, pos);
		source.out().offer(Product.FINES_SLURRY, kg, dev.wildcatter.fluid.SlurryAssay.of(assay));
		return source;
	}

	private static void reagents(GameTestHelper helper, PlantBlockEntity tank, Reagent... which) {
		for (Reagent reagent : which) {
			helper.assertTrue(tank.offer(new ItemStack(BuiltInRegistries.ITEM.getValue(Tailings.id(reagent.id())))),
					Component.literal("the tank should take " + reagent.displayName()));
		}
	}

	/**
	 * The flotation circuit on steel pipe (docs/multiblocks/flotation-bank.md, game test checks 1, 3 and 4): fines slurry holding one
	 * block of copper ore's copper, conditioned, floated with the blower running, thickened and filtered to 3 copper ingots; the
	 * tails, with the rest of the copper, to the tailings pond.
	 */
	private static void flotation(GameTestHelper helper) {
		int y = 4;
		double copper = Rock.COPPER.contained(dev.wildcatter.tailings.rock.Mineral.COPPER).doubleValue();
		fines(helper, new BlockPos(0, y, 0), 100, java.util.Map.of("minecraft:copper_ingot", copper / 100));
		pipe(helper, new BlockPos(0, y, 2), new BlockPos(1, y, 2));
		PlantBlockEntity tank = plant(helper, Machine.CONDITIONER, new BlockPos(0, y, 3));
		pipe(helper, new BlockPos(0, y, 6));
		PlantBlockEntity bank = plant(helper, Machine.FLOTATION_BANK, new BlockPos(0, y, 7));
		PlantBlockEntity blower = plant(helper, Machine.BLOWER, new BlockPos(7, y, 7));
		pipe(helper, new BlockPos(6, y, 20));
		PlantBlockEntity thickener = plant(helper, Machine.THICKENER, new BlockPos(6, y, 21));
		pipe(helper, new BlockPos(6, y, 29));
		PlantBlockEntity filter = plant(helper, Machine.FILTER, new BlockPos(6, y, 30));
		pipe(helper, new BlockPos(0, y, 20), new BlockPos(0, y, 21));
		PlantBlockEntity pond = plant(helper, Machine.TAILINGS_POND, new BlockPos(0, y, 22));
		for (PlantBlockEntity part : new PlantBlockEntity[] {tank, bank, blower, thickener, filter}) {
			part.forceRunning();
		}
		reagents(helper, tank, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC);
		helper.succeedWhen(() -> {
			int ingots = filter.countIn(new int[] {1, 2}, new ItemStack(Items.COPPER_INGOT));
			double tailCopper = pond.back().assayOf(Product.TAILS_SLURRY).get("minecraft:copper_ingot") * pond.back().held(Product.TAILS_SLURRY);
			helper.assertTrue(ingots == 3, Component.literal("the filter should pay 3 copper ingots, got " + ingots + " (tank " + tank.status()
					+ "; bank " + bank.status() + "; thickener " + thickener.status() + "; filter " + filter.status() + ")"));
			helper.assertTrue(Math.abs(tailCopper - copper * 0.25) < 1e-3, Component.literal("the tails should carry the rest of the copper, got "
					+ tailCopper + " (pond " + pond.status() + ")"));
		});
	}

	/**
	 * What stops the flotation parts (flotation-bank.md, checks 2 and 5): a bank whose blower is off reads NO AIR; a sphalerite feed
	 * without copper sulfate reads NO REAGENT (Copper Sulfate); a full pond reads FULL, and a bank whose tails have nowhere to go
	 * stops.
	 */
	private static void flotationStops(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity still = plant(helper, Machine.FLOTATION_BANK, new BlockPos(0, y, 0));
		plant(helper, Machine.BLOWER, new BlockPos(7, y, 0));
		still.forceRunning();
		still.back().offer(Product.CONDITIONED_SLURRY, 50, dev.wildcatter.fluid.SlurryAssay.EMPTY);

		PlantBlockEntity tank = plant(helper, Machine.CONDITIONER, new BlockPos(0, y, 15));
		tank.forceRunning();
		tank.back().offer(Product.FINES_SLURRY, 50, dev.wildcatter.fluid.SlurryAssay.of(java.util.Map.of("wildcatter:sphalerite", 0.01)));
		reagents(helper, tank, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC);

		PlantBlockEntity choked = plant(helper, Machine.FLOTATION_BANK, new BlockPos(10, y, 0));
		PlantBlockEntity air = plant(helper, Machine.BLOWER, new BlockPos(17, y, 0));
		choked.forceRunning();
		air.forceRunning();
		choked.back().offer(Product.CONDITIONED_SLURRY, 50, dev.wildcatter.fluid.SlurryAssay.EMPTY);
		choked.out().offer(Product.TAILS_SLURRY, 1_000_000, dev.wildcatter.fluid.SlurryAssay.EMPTY);

		PlantBlockEntity pond = plant(helper, Machine.TAILINGS_POND, new BlockPos(10, y, 15));
		pond.back().offer(Product.TAILS_SLURRY, 3_400_000, dev.wildcatter.fluid.SlurryAssay.EMPTY);
		helper.succeedWhen(() -> {
			helper.assertTrue(still.status().startsWith("NO AIR"), Component.literal("the bank should read NO AIR, got " + still.status()));
			helper.assertTrue(tank.status().equals("NO REAGENT: Copper Sulfate"), Component.literal("the tank should want copper sulfate, got " + tank.status()));
			helper.assertTrue(choked.status().contains("nowhere to go"), Component.literal("the choked bank should stop, got " + choked.status()));
			helper.assertTrue(pond.status().startsWith("FULL"), Component.literal("the pond should read FULL, got " + pond.status()));
		});
	}

	/** Breaking any piece of the ball mill takes all 96 cells and drops the one item. */
	private static void partBreak(GameTestHelper helper) {
		BlockPos main = new BlockPos(2, 30, 2);
		plant(helper, Machine.BALL_MILL, main);
		BlockPos piece = main.offset(2, 3, 4);
		helper.assertTrue(helper.getBlockState(piece).getBlock() == TailingsBlocks.BALL_MILL, Component.literal("the mill should fill its far corner"));
		helper.startSequence()
				.thenExecute(() -> helper.getLevel().destroyBlock(helper.absolutePos(piece), true))
				.thenExecuteAfter(5, () -> {
					for (int x = 0; x < 4; x++) {
						for (int yy = 0; yy < 4; yy++) {
							for (int z = 0; z < 6; z++) {
								helper.assertTrue(helper.getBlockState(main.offset(x, yy, z)).getBlock() != TailingsBlocks.BALL_MILL,
										Component.literal("every cell should go, " + main.offset(x, yy, z) + " stayed"));
							}
						}
					}
					helper.assertItemEntityCountIs(TailingsBlocks.BALL_MILL.asItem(), main, 8.0, 1);
				})
				.thenSucceed();
	}

	/** A grizzly with no jaw under it, and cyclones with no mill beside them, read what they need. */
	private static void notBuilt(GameTestHelper helper) {
		PlantBlockEntity grizzly = plant(helper, Machine.GRIZZLY, new BlockPos(0, 40, 0));
		PlantBlockEntity cyclone = plant(helper, Machine.CYCLONE, new BlockPos(6, 40, 0));
		cyclone.forceRunning();
		helper.assertTrue(grizzly.offer(new ItemStack(Blocks.COBBLESTONE)), Component.literal("the grizzly should take cobble"));
		helper.succeedWhen(() -> {
			helper.assertTrue(grizzly.status().startsWith("NOT BUILT") && grizzly.status().contains("Jaw Crusher"),
					Component.literal("the grizzly should name the jaw, got " + grizzly.status()));
			helper.assertTrue(cyclone.status().startsWith("NOT BUILT") && cyclone.status().contains("Ball Mill"),
					Component.literal("the cyclones should name the mill, got " + cyclone.status()));
		});
	}

	/** The jaw's motor box is the west face of its main cell, turned with the part, and nowhere else. */
	private static void terminal(GameTestHelper helper) {
		BlockPos at = new BlockPos(4, 50, 4);
		BlockState state = TailingsBlocks.JAW.defaultBlockState().setValue(BigPartBlock.FACING, Direction.EAST);
		helper.setBlock(at, state);
		BigParts.grow(helper.getLevel(), helper.absolutePos(at), helper.getLevel().getBlockState(helper.absolutePos(at)));
		PlantBlockEntity jaw = helper.getBlockEntity(at, PlantBlockEntity.class);
		BlockPos main = helper.absolutePos(at);
		// facing east, the drawing's west face is north
		helper.assertTrue(jaw.hasPortAt(main, Direction.NORTH, dev.wildcatter.circuit.Conductor.A, dev.wildcatter.energy.Voltage.INDUSTRIAL),
				Component.literal("the motor box should be on the main cell's turned west face"));
		helper.assertFalse(jaw.hasPortAt(main, Direction.WEST, dev.wildcatter.circuit.Conductor.A, dev.wildcatter.energy.Voltage.INDUSTRIAL),
				Component.literal("no box on another face"));
		helper.assertFalse(jaw.hasPortAt(main.above(), Direction.NORTH, dev.wildcatter.circuit.Conductor.A, dev.wildcatter.energy.Voltage.INDUSTRIAL),
				Component.literal("no box on another cell"));
		helper.assertFalse(jaw.hasPortAt(main, Direction.NORTH, dev.wildcatter.circuit.Conductor.A, dev.wildcatter.energy.Voltage.MEDIUM),
				Component.literal("the jaw is a 480 V motor"));
		helper.succeed();
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






	/** The rectifier (forced on) beside a north-running track at x 3 from z 0 to {@code last}, and the track fed. */
	private static void track(GameTestHelper helper, int y, int last) {
		RectifierBlockEntity box = part(helper, TailingsBlocks.TRACTION_RECTIFIER, new BlockPos(1, y, 2), RectifierBlockEntity.class);
		box.forceRunning();
		BlockState rail = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = 0; z <= last; z++) {
			helper.setBlock(new BlockPos(3, y, z), rail);
		}
		box.spread();
	}

	private static MineVehicleEntity loco(GameTestHelper helper, BlockPos at, Heading heading) {
		MineVehicleEntity loco = MineVehicleEntity.placeLocomotive(helper.getLevel(), helper.absolutePos(at), heading);
		helper.assertTrue(loco != null, Component.literal("a locomotive should go on the rail at " + at));
		return loco;
	}

	private static MineVehicleEntity car(GameTestHelper helper, BlockPos at, Heading heading) {
		MineVehicleEntity car = MineVehicleEntity.placeCar(helper.getLevel(), helper.absolutePos(at), heading);
		helper.assertTrue(car != null, Component.literal("a car should go on the rail at " + at));
		return car;
	}

	/** Relative z of an entity in the test. */
	private static double relZ(GameTestHelper helper, Entity e) {
		return e.getZ() - helper.absolutePos(BlockPos.ZERO).getZ();
	}

	private static double relX(GameTestHelper helper, Entity e) {
		return e.getX() - helper.absolutePos(BlockPos.ZERO).getX();
	}

	/** The rectifier feeds the rail, and a locomotive with a loaded car glides along it, the car keeping its place. */
	private static void haul(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 12);
		RailBlockEntity fed = helper.getBlockEntity(new BlockPos(3, y, 9), RailBlockEntity.class);
		helper.assertTrue(fed.powered() && fed.conductorLive(), Component.literal("the rectifier should feed the track joined to it"));
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 8), Heading.NORTH);
		MineVehicleEntity car = car(helper, new BlockPos(3, y, 9), Heading.NORTH);
		helper.assertTrue(car.locomotiveId() != null && loco.carIds().size() == 1, Component.literal("the car should couple to the locomotive ahead"));
		car.load().add("minecraft:cobblestone", 2);
		car.refreshLoad();
		helper.succeedWhen(() -> {
			helper.assertTrue(relZ(helper, loco) < 4, Component.literal("the locomotive should glide north, at z " + relZ(helper, loco) + " (" + loco.status() + ")"));
			helper.assertTrue(Math.abs(relZ(helper, car) - relZ(helper, loco) - Haul.SPACING) < 0.05, Component.literal("the car should keep its place behind"));
			helper.assertTrue(car.load().total() == 2, Component.literal("the car keeps its load"));
		});
	}

	/** A locomotive and three cars run as four entities to the end of the track, wait, and come back. */
	private static void trainRuns(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 20);
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 12), Heading.NORTH);
		List<MineVehicleEntity> cars = List.of(car(helper, new BlockPos(3, y, 13), Heading.NORTH), car(helper, new BlockPos(3, y, 14), Heading.NORTH),
				car(helper, new BlockPos(3, y, 15), Heading.NORTH));
		helper.assertTrue(loco.carIds().size() == 3, Component.literal("three cars should couple, got " + loco.carIds().size()));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, loco) < 1.0 && loco.speed() < 0.01,
						Component.literal("the train should run to the end of the track, at z " + relZ(helper, loco) + " (" + loco.status() + ")")))
				.thenExecute(() -> {
					int count = helper.getLevel().getEntitiesOfClass(MineVehicleEntity.class, new AABB(helper.absolutePos(new BlockPos(3, y, 0))).inflate(2, 2, 25)).size();
					helper.assertTrue(count == 4, Component.literal("four vehicle entities, got " + count));
					for (int i = 0; i < cars.size(); i++) {
						double gap = relZ(helper, cars.get(i)) - (i == 0 ? relZ(helper, loco) : relZ(helper, cars.get(i - 1)));
						helper.assertTrue(Math.abs(gap - Haul.SPACING) < 0.05, Component.literal("car " + i + " should keep its spacing, gap " + gap));
					}
				})
				.thenWaitUntil(() -> helper.assertTrue(loco.direction() < 0 && relZ(helper, loco) > 3,
						Component.literal("after its wait the train should come back, at z " + relZ(helper, loco) + " (" + loco.status() + ")")))
				.thenSucceed();
	}

	/** Through a thrown switch the locomotive turns east, and its cars follow it round. */
	private static void trainTurnout(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 9);
		helper.setBlock(new BlockPos(3, y, 1), TailingsBlocks.MINE_SWITCH.defaultBlockState().setValue(MineSwitchBlock.FACING, Direction.NORTH)
				.setValue(MineSwitchBlock.THROWN, true));
		for (int x = 4; x <= 12; x++) {
			helper.setBlock(new BlockPos(x, y, 1), TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.EAST));
		}
		helper.getBlockEntity(new BlockPos(1, y, 2), RectifierBlockEntity.class).spread();
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 5), Heading.NORTH);
		MineVehicleEntity first = car(helper, new BlockPos(3, y, 6), Heading.NORTH);
		MineVehicleEntity second = car(helper, new BlockPos(3, y, 7), Heading.NORTH);
		helper.succeedWhen(() -> {
			helper.assertTrue(relX(helper, loco) > 7, Component.literal("the locomotive should take the branch east, at " + loco.position() + " (" + loco.status() + ")"));
			helper.assertTrue(relX(helper, second) > 4 && Math.abs(relZ(helper, second) - 1.5) < 0.05,
					Component.literal("the cars should follow it round, the second at " + relX(helper, second) + ", " + relZ(helper, second)));
			helper.assertTrue(Math.abs(relX(helper, loco) - relX(helper, first) - Haul.SPACING) < 0.05, Component.literal("and keep their spacing"));
		});
	}

	/** Down the 1-in-2 rack decline at the rack brake's 1 m/s, and back up it at power over force. */
	private static void trainDecline(GameTestHelper helper) {
		int top = 64;
		RectifierBlockEntity box = part(helper, TailingsBlocks.TRACTION_RECTIFIER, new BlockPos(1, top, 12), RectifierBlockEntity.class);
		box.forceRunning();
		BlockState flat = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = 11; z <= 14; z++) {
			helper.setBlock(new BlockPos(3, top, z), flat);
		}
		for (int k = 0; k < 3; k++) {
			helper.setBlock(new BlockPos(3, top - k, 10 - 2 * k), flat.setValue(MineRailBlock.GRADE, RailGrade.UPPER).setValue(MineRailBlock.RACK, true));
			helper.setBlock(new BlockPos(3, top - k, 9 - 2 * k), flat.setValue(MineRailBlock.GRADE, RailGrade.LOWER).setValue(MineRailBlock.RACK, true));
		}
		for (int z = 0; z <= 4; z++) {
			helper.setBlock(new BlockPos(3, top - 3, z), flat);
		}
		box.spread();
		MineVehicleEntity loco = loco(helper, new BlockPos(3, top, 13), Heading.NORTH);
		double[] most = {0, 0};   // fastest down the grade, fastest up it
		helper.onEachTick(() -> {
			double z = relZ(helper, loco);
			if (z > 5.2 && z < 10.5) {   // the locomotive on the grade, not the flat at either end
				int way = loco.direction() > 0 ? 0 : 1;
				most[way] = Math.max(most[way], loco.speed());
			}
		});
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(loco.getY() < helper.absolutePos(BlockPos.ZERO).getY() + top - 1.9 && relZ(helper, loco) < 3,
						Component.literal("the locomotive should run down to the bottom, at " + loco.position() + " (" + loco.status() + ")")))
				.thenWaitUntil(() -> helper.assertTrue(loco.direction() < 0 && relZ(helper, loco) > 11.5,
						Component.literal("and back up to the top, at " + loco.position() + " (" + loco.status() + ")")))
				.thenExecute(() -> {
					helper.assertTrue(most[0] <= Haul.DOWNHILL_SPEED + 1e-6, Component.literal("down the grade at most 1 m/s, got " + most[0]));
					helper.assertTrue(most[1] > 1 && most[1] <= 2.07, Component.literal("up the grade at power over force (2.06 m/s alone), got " + most[1]));
				})
				.thenSucceed();
	}

	/** Track broken under a running train derails it, and the car on the gap drops its load. */
	private static void trainDerail(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 20);
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 15), Heading.NORTH);
		MineVehicleEntity car = car(helper, new BlockPos(3, y, 16), Heading.NORTH);
		car.load().add("minecraft:cobblestone", 30);
		car.refreshLoad();
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, loco) < 10, Component.literal("the train should run")))
				.thenExecute(() -> helper.setBlock(BlockPos.containing(relX(helper, car), y, relZ(helper, car)), Blocks.AIR))
				.thenExecuteAfter(2, () -> {
					helper.assertTrue(loco.derailed() && loco.status().startsWith("DERAILED"), Component.literal("the train should derail, got " + loco.status()));
					helper.assertTrue(car.load().total() == 0, Component.literal("the car on the gap should drop its load"));
					int dropped = 0;
					for (var item : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, car.getBoundingBox().inflate(4),
							e -> e.getItem().is(Items.COBBLESTONE))) {
						dropped += item.getItem().getCount();
					}
					helper.assertTrue(dropped == 30, Component.literal("its 30 cobble should lie by the track, got " + dropped));
				})
				.thenSucceed();
	}

	/** A running train hurts a mob on the track. */
	private static void trainHarm(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 20);
		loco(helper, new BlockPos(3, y, 18), Heading.NORTH);
		net.minecraft.world.entity.animal.pig.Pig pig = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, new BlockPos(3, y + 1, 10));
		pig.setNoAi(true);
		float health = pig.getHealth();
		helper.succeedWhen(() -> helper.assertTrue(pig.getHealth() < health || !pig.isAlive(), Component.literal("the train should hurt the pig")));
	}

	/** A world saved before R5 kept its train on the rails: it comes back as a locomotive and a coupled car, load and all. */
	private static void trainMigrate(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 10);
		helper.getBlockEntity(new BlockPos(3, y, 5), RailBlockEntity.class).setOldTrain("LOCO", 0, "");
		helper.getBlockEntity(new BlockPos(3, y, 6), RailBlockEntity.class).setOldTrain("CAR", 1, "7=minecraft:iron_ore");
		helper.succeedWhen(() -> {
			List<MineVehicleEntity> found = helper.getLevel().getEntitiesOfClass(MineVehicleEntity.class, new AABB(helper.absolutePos(new BlockPos(3, y, 5))).inflate(2, 2, 12));
			MineVehicleEntity loco = found.stream().filter(MineVehicleEntity::locomotive).findFirst().orElse(null);
			MineVehicleEntity car = found.stream().filter(e -> !e.locomotive()).findFirst().orElse(null);
			helper.assertTrue(loco != null && car != null, Component.literal("the old train should become a locomotive and a car, got " + found.size()));
			helper.assertTrue(car.locomotiveId() != null && car.load().total() == 7, Component.literal("the car should couple and keep its 7 iron ore"));
		});
	}

	/**
	 * The dump station (docs/multiblocks/dump-station.md): two cars, copper ore and cobble, tip on the move over the ramp and pocket;
	 * the apron feeder draws the pocket to the sorter, which sends copper ore to its bin and cobble to the waste bin. With the copper
	 * bin full, copper waits and the sorter reads FULL for it.
	 */
	private static void dumpStation(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 15);
		DumpBlockEntity pocket = part(helper, TailingsBlocks.DUMP_POCKET, new BlockPos(2, y - 3, 6), DumpBlockEntity.class);
		part(helper, TailingsBlocks.DUMP_RAMP, new BlockPos(4, y, 6), PlantBlockEntity.class);
		PlantBlockEntity feeder = plant(helper, Machine.APRON_FEEDER, new BlockPos(2, y - 3, 9));
		PlantBlockEntity sorter = plant(helper, Machine.ORE_SORTER, new BlockPos(1, y - 3, 13));
		PlantBlockEntity copperBin = plant(helper, Machine.ORE_BIN, new BlockPos(4, y - 3, 13));
		PlantBlockEntity wasteBin = plant(helper, Machine.ORE_BIN, new BlockPos(1, y - 3, 16));
		feeder.forceRunning();
		sorter.forceRunning();
		helper.assertTrue(sorter.setSide(Direction.EAST, new ItemStack(Blocks.COPPER_ORE)), Component.literal("the sorter's east side should take copper ore"));
		helper.assertTrue(sorter.setWasteSide(Direction.SOUTH), Component.literal("the sorter's south side should take the waste"));
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 11), Heading.NORTH);
		MineVehicleEntity first = car(helper, new BlockPos(3, y, 12), Heading.NORTH);
		MineVehicleEntity second = car(helper, new BlockPos(3, y, 13), Heading.NORTH);
		first.load().add("minecraft:copper_ore", 64);
		second.load().add("minecraft:cobblestone", 64);
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(first.load().total() == 0 && second.load().total() == 0,
						Component.literal("both cars should tip on the move (" + loco.status() + ", locomotive at z " + relZ(helper, loco) + ")")))
				.thenWaitUntil(() -> helper.assertTrue(copperBin.binned().getOrDefault("minecraft:copper_ore", 0) == 64
						&& wasteBin.binned().getOrDefault("minecraft:cobblestone", 0) == 64,
						Component.literal("copper ore to its bin and cobble to waste (feeder " + feeder.status() + "; sorter " + sorter.status() + ")")))
				.thenExecute(() -> {
					copperBin.binned().put("minecraft:copper_ore", PlantRates.BIN_ITEMS);
					helper.assertTrue(pocket.offer(java.util.Map.of("minecraft:copper_ore", 64)), Component.literal("the pocket should take more copper"));
				})
				.thenWaitUntil(() -> helper.assertTrue(sorter.status().startsWith("FULL") && sorter.status().contains("copper ore"),
						Component.literal("the sorter should read FULL for copper, got " + sorter.status())))
				.thenSucceed();
	}

	/** A loaded car over a full pocket does not tip, and the train waits until the pocket has room. */
	private static void dumpWaits(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 4);
		DumpBlockEntity pocket = part(helper, TailingsBlocks.DUMP_POCKET, new BlockPos(2, y - 3, 0), DumpBlockEntity.class);
		part(helper, TailingsBlocks.DUMP_RAMP, new BlockPos(4, y, 0), PlantBlockEntity.class);
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 1), Heading.NORTH);
		MineVehicleEntity car = car(helper, new BlockPos(3, y, 2), Heading.NORTH);
		car.load().add("minecraft:cobblestone", 64);
		helper.assertTrue(pocket.offer(java.util.Map.of("minecraft:cobblestone", Haul.POCKET_ITEMS)), Component.literal("fill the pocket"));
		double start = relZ(helper, loco);
		helper.startSequence()
				.thenExecuteAfter(40, () -> {
					helper.assertTrue(Math.abs(relZ(helper, loco) - start) < 0.01 && loco.status().contains("dump"),
							Component.literal("the train should wait at a full pocket, got " + loco.status()));
					helper.assertTrue(car.load().total() == 64, Component.literal("the car should not tip into a full pocket"));
					pocket.draw(64);
				})
				.thenWaitUntil(() -> helper.assertTrue(car.load().total() == 0 && pocket.pocket() == Haul.POCKET_ITEMS,
						Component.literal("with room, the car tips")))
				.thenSucceed();
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
		MineVehicleEntity[] trainLoco = new MineVehicleEntity[1];
		helper.startSequence()
				.thenExecute(() -> trainLoco[0] = loco(helper, rail, Heading.NORTH))
				.thenExecuteAfter(5, () -> {
					helper.assertTrue(hasTicket(level, abs, ChunkRunner.RUNNER), Component.literal("a running train should hold a runner ticket"));
					// take the locomotive off
					trainLoco[0].discard();
				})
				// wait out the timeout, then allow until the end of the test: a slow server purges stale tickets a little late
				.thenIdle((int) ChunkRunner.TIMEOUT_TICKS)
				.thenWaitUntil(() -> helper.assertFalse(hasTicket(level, abs, ChunkRunner.RUNNER), Component.literal("the runner ticket should time out once the train is gone")))
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
		BlockPos pos = new BlockPos(1, y, 1);
		PlantBlockEntity tank = plant(helper, Machine.CONDITIONER, pos);
		var lime = BuiltInRegistries.ITEM.getValue(Tailings.id(Reagent.LIME.id()));
		for (int i = 0; i < 3; i++) {
			helper.assertTrue(tank.offer(new ItemStack(lime)), Component.literal("the tank should take lime"));
		}
		helper.getLevel().removeBlock(helper.absolutePos(pos), false);
		int dropped = 0;
		for (var item : helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
				new net.minecraft.world.phys.AABB(helper.absolutePos(pos)).inflate(6), e -> e.getItem().is(lime))) {
			dropped += item.getItem().getCount();
		}
		helper.assertTrue(dropped == 3, Component.literal("the tank's 3 lime should drop, got " + dropped));
		helper.succeed();
	}
}
