package dev.wildcatter.tailings.neoforge.gametest;

import dev.wildcatter.block.BigPartBlock;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.Tailings;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.DewateringPumpBlock;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.block.TbmBackup;
import dev.wildcatter.tailings.block.TbmCab;
import dev.wildcatter.tailings.block.TbmCore;
import dev.wildcatter.tailings.block.TbmMachine;
import dev.wildcatter.tailings.entity.GantryEntity;
import dev.wildcatter.tailings.mine.TbmGantry;
import dev.wildcatter.tailings.entity.TbmEntity;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.Feeds;
import dev.wildcatter.tailings.block.ChunkRunner;
import dev.wildcatter.tailings.block.MineRailBlock;
import dev.wildcatter.tailings.block.MineSwitchBlock;
import dev.wildcatter.tailings.block.RailBlockEntity;
import dev.wildcatter.tailings.block.RectifierBlockEntity;
import dev.wildcatter.tailings.block.HandMillBlockEntity;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.SignalBlockEntity;
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
import dev.wildcatter.tailings.block.FloorLook;
import java.util.List;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
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
	private static final String[] SCENES = {"plc_switch", "plc_signal", "plc_plant", "plc_tbm_go", "selective_float", "selective_no_reagent", "no_activation_tank", "bulk_loses_zinc", "bulk_one_mineral", "registers", "tails_to_cell", "cell_breached", "stamp", "magnet", "jaw_size", "crushing_circuit", "bin_feeds", "belt_drive", "flotation", "flotation_stops", "spiral_bank", "full_recovery", "leach_no_acid", "byproducts", "heap_leach", "dump_station", "dump_waits", "drill", "tbm_built", "drill_retired", "tbm_lift", "tbm_glide", "tbm_setdown", "tbm_park", "tbm_dismantle", "tbm_seat", "tbm_gantries", "tbm_muck", "tbm_train_loads", "tbm_cable_end", "tbm_gantry_missing", "tbm_uncoupled", "tbm_gantries_decline", "tbm_train_stops", "tbm_train_turns", "plant_sound", "drill_pause", "haul", "models", "train_runs", "train_turnout", "train_decline", "train_derail", "train_harm", "train_migrate", "traction_dc", "traction_trip", "traction_far", "traction_unfed", "train_tickets", "drill_blocked", "plant_drops", "part_break", "not_built", "terminal"};

	/** Scenes where trains run the length of a track at their real speed. */
	private static final java.util.Set<String> LONG = java.util.Set.of("plc_switch", "haul", "train_runs", "train_turnout", "train_decline", "train_derail", "train_harm",
			"dump_station", "dump_waits", "traction_dc", "tbm_train_stops", "tbm_train_turns", "byproducts", "selective_float", "no_activation_tank", "bulk_loses_zinc", "bulk_one_mineral");

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
			helper.register(id("tbm_built"), loaded(TailingsGameTests::tbmBuilt));
			helper.register(id("drill_retired"), loaded(TailingsGameTests::drillRetired));
			helper.register(id("tbm_lift"), loaded(TailingsGameTests::tbmLift));
			helper.register(id("tbm_glide"), loaded(TailingsGameTests::tbmGlide));
			helper.register(id("tbm_setdown"), loaded(TailingsGameTests::tbmSetDown));
			helper.register(id("tbm_park"), loaded(TailingsGameTests::tbmPark));
			helper.register(id("tbm_dismantle"), loaded(TailingsGameTests::tbmDismantle));
			helper.register(id("tbm_seat"), loaded(TailingsGameTests::tbmSeat));
			helper.register(id("tbm_gantries"), loaded(TailingsGameTests::tbmGantries));
			helper.register(id("spiral_bank"), loaded(TailingsGameTests::spiralBank));
			helper.register(id("full_recovery"), loaded(TailingsGameTests::fullRecovery));
			helper.register(id("selective_float"), loaded(TailingsGameTests::selectiveFloat));
			helper.register(id("plc_switch"), loaded(TailingsGameTests::plcSwitch));
			helper.register(id("plc_signal"), loaded(TailingsGameTests::plcSignal));
			helper.register(id("plc_plant"), loaded(TailingsGameTests::plcPlant));
			helper.register(id("plc_tbm_go"), loaded(TailingsGameTests::plcTbmGo));
			helper.register(id("selective_no_reagent"), loaded(TailingsGameTests::selectiveNoReagent));
			helper.register(id("no_activation_tank"), loaded(TailingsGameTests::noActivationTank));
			helper.register(id("bulk_loses_zinc"), loaded(TailingsGameTests::bulkLosesZinc));
			helper.register(id("bulk_one_mineral"), loaded(TailingsGameTests::bulkOneMineral));
			helper.register(id("tails_to_cell"), loaded(TailingsGameTests::tailsToCell));
			helper.register(id("cell_breached"), loaded(TailingsGameTests::cellBreached));
			helper.register(id("leach_no_acid"), loaded(TailingsGameTests::leachNoAcid));
			helper.register(id("byproducts"), loaded(TailingsGameTests::byproducts));
			helper.register(id("heap_leach"), loaded(TailingsGameTests::heapLeach));
			helper.register(id("tbm_muck"), loaded(TailingsGameTests::tbmMuck));
			helper.register(id("tbm_train_loads"), loaded(TailingsGameTests::tbmTrainLoads));
			helper.register(id("tbm_cable_end"), loaded(TailingsGameTests::tbmCableEnd));
			helper.register(id("tbm_gantry_missing"), loaded(TailingsGameTests::tbmGantryMissing));
			helper.register(id("tbm_uncoupled"), loaded(TailingsGameTests::tbmUncoupled));
			helper.register(id("tbm_gantries_decline"), loaded(TailingsGameTests::tbmGantriesDecline));
			helper.register(id("tbm_train_stops"), loaded(TailingsGameTests::tbmTrainStops));
			helper.register(id("tbm_train_turns"), loaded(TailingsGameTests::tbmTrainTurns));
			helper.register(id("plant_sound"), loaded(TailingsGameTests::plantSound));
			helper.register(id("haul"), loaded(TailingsGameTests::haul));
			helper.register(id("models"), loaded(TailingsGameTests::models));
			helper.register(id("train_runs"), loaded(TailingsGameTests::trainRuns));
			helper.register(id("train_turnout"), loaded(TailingsGameTests::trainTurnout));
			helper.register(id("train_decline"), loaded(TailingsGameTests::trainDecline));
			helper.register(id("train_derail"), loaded(TailingsGameTests::trainDerail));
			helper.register(id("train_harm"), loaded(TailingsGameTests::trainHarm));
			helper.register(id("train_migrate"), loaded(TailingsGameTests::trainMigrate));
			helper.register(id("traction_dc"), loaded(TailingsGameTests::tractionDc));
			helper.register(id("traction_trip"), loaded(TailingsGameTests::tractionTrip));
			helper.register(id("traction_far"), loaded(TailingsGameTests::tractionFar));
			helper.register(id("traction_unfed"), loaded(TailingsGameTests::tractionUnfed));
			helper.register(id("train_tickets"), loaded(TailingsGameTests::trainTickets));
			helper.register(id("drill_blocked"), loaded(TailingsGameTests::drillBlocked));
			helper.register(id("plant_drops"), loaded(TailingsGameTests::plantDrops));
			helper.register(id("part_break"), loaded(TailingsGameTests::partBreak));
			helper.register(id("not_built"), loaded(TailingsGameTests::notBuilt));
			helper.register(id("terminal"), loaded(TailingsGameTests::terminal));
			helper.register(id("pit_settle"), loaded(PitGameTests::settle));
			helper.register(id("pit_truck_tip"), loaded(PitGameTests::truckTip));
			helper.register(id("pit_truck_body"), loaded(PitGameTests::truckBody));
			helper.register(id("pit_hopper"), loaded(PitGameTests::hopper));
			helper.register(id("pit_truck_drive"), loaded(PitGameTests::truckDrive));
			helper.register(id("pit_loader_soft"), loaded(PitGameTests::loaderSoft));
			helper.register(id("pit_loader_hard"), loaded(PitGameTests::loaderHard));
			helper.register(id("pit_loader_muck"), loaded(PitGameTests::loaderMuck));
			helper.register(id("pit_survey"), wide("pit_survey", PitHaulGameTests::survey));
			helper.register(id("pit_loader_auto"), wide("pit_loader_auto", PitHaulGameTests::loaderAuto));
			helper.register(id("pit_fuel_burn"), wide("pit_fuel_burn", PitHaulGameTests::fuelBurn));
			helper.register(id("pit_fuel_bay"), wide("pit_fuel_bay", PitHaulGameTests::fuelBay));
			helper.register(id("pit_out_of_fuel"), wide("pit_out_of_fuel", PitHaulGameTests::outOfFuel));
			helper.register(id("pit_truck_obstacle"), wide("pit_truck_obstacle", PitHaulGameTests::truckObstacle));
			helper.register(id("pit_truck_follow"), wide("pit_truck_follow", PitHaulGameTests::truckFollow));
			helper.register(id("pit_one_way"), wide("pit_one_way", PitHaulGameTests::oneWay));
			helper.register(id("pit_truck_haul"), wide("pit_truck_haul", PitHaulGameTests::truckHaul));
			helper.register(id("pit_dispatch"), wide("pit_dispatch", PitDispatchGameTests::dispatch));
			helper.register(id("pit_dispatch_alarms"), wide("pit_dispatch_alarms", PitDispatchGameTests::alarms));
			helper.register(id("pit_dispatch_fallback"), wide("pit_dispatch_fallback", PitDispatchGameTests::fallback));
			helper.register(id("pit_drill_pattern"), wide("pit_drill_pattern", PitBlastGameTests::drillPattern));
			helper.register(id("pit_drill_short"), wide("pit_drill_short", PitBlastGameTests::drillShort));
			helper.register(id("pit_charge"), wide("pit_charge", PitBlastGameTests::charge));
			helper.register(id("pit_prime_stem"), wide("pit_prime_stem", PitBlastGameTests::primeStem));
			helper.register(id("pit_blast"), wide("pit_blast", PitBlastGameTests::blast));
			helper.register(id("pit_blast_blowout"), wide("pit_blast_blowout", PitBlastGameTests::blowout));
			helper.register(id("pit_blast_boxcut"), wide("pit_blast_boxcut", PitBlastGameTests::boxCut));
			helper.register(id("pit_misfire"), wide("pit_misfire", PitBlastGameTests::misfire));
			helper.register(id("pit_blast_guard"), wide("pit_blast_guard", PitBlastGameTests::guard));
			helper.register(id("pit_blast_plc"), wide("pit_blast_plc", PitBlastGameTests::plc));
		}));
		modBus.addListener(RegisterGameTestsEvent.class, event -> {
			Holder<TestEnvironmentDefinition<?>> plain = event.registerEnvironment(id("plain"));
			// TAILINGS_ONLY=pit_blast,pit_charge runs only those scenes (a wave's own, while it is built); unset, all of them
			String only = System.getenv("TAILINGS_ONLY");
			java.util.Set<String> chosen = only == null || only.isBlank() ? null : java.util.Set.of(only.split(","));
			for (String scene : java.util.stream.Stream.of(SCENES, PitGameTests.SCENES, PitHaulGameTests.SCENES, PitDispatchGameTests.SCENES, PitBlastGameTests.SCENES).flatMap(java.util.Arrays::stream).toList()) {
				if (chosen != null && !chosen.contains(scene)) {
					continue;
				}
				event.registerTest(id(scene), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(scene)),
						// the plant's parts are true size: padding keeps each scene clear of the next
						new TestData<>(plain, net.minecraft.world.level.Level.OVERWORLD, Identifier.withDefaultNamespace("empty"), PitHaulGameTests.ticks(scene, LONG.contains(scene) || PitGameTests.LONG.contains(scene) ? 1600 : 400), 1, true,
								net.minecraft.world.level.block.Rotation.NONE, false, 1, 1, false, PitHaulGameTests.padding(scene, 40))));
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

	/** As {@link #loaded}, over the chunks a long pit scene reaches. */
	private static Consumer<GameTestHelper> wide(String name, Consumer<GameTestHelper> scene) {
		int[] reach = PitHaulGameTests.reach(name);
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

	private static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath("tailings_gametest", path);
	}

	private static void registers(GameTestHelper helper) {
		for (String sound : new String[] {"jaw_crusher", "cone_crusher", "vibrating_screen", "ball_mill", "belt_conveyor", "apron_feeder", "slurry_rush", "agitator", "pressure_filter",
				"ore_sorter", "tbm_cutter", "mine_locomotive", "muck_tip", "muck_load", "ring_set", "car_couple"}) {
			helper.assertTrue(BuiltInRegistries.SOUND_EVENT.containsKey(Tailings.id(sound)), Component.literal("the sound " + sound + " should be registered"));
		}
		helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Tailings.id("stamp_mill")), Component.literal("stamp mill missing"));
		helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Tailings.id("crushed_granite")), Component.literal("crushed granite missing"));
		helper.assertTrue(BuiltInRegistries.ITEM.containsKey(Tailings.id("sand_iron")), Component.literal("iron sand missing"));
		helper.assertTrue(BuiltInRegistries.CREATIVE_MODE_TAB.containsKey(Tailings.id("tailings")), Component.literal("creative tab missing"));
		helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Tailings.id("tunnel_drill")), Component.literal("tunnel drill missing"));
		helper.assertTrue(BuiltInRegistries.BLOCK.containsKey(Tailings.id("mine_rail")), Component.literal("mine rail missing"));
		helper.assertTrue(BuiltInRegistries.MENU.containsKey(Tailings.id("tunnel_drill")), Component.literal("drill menu missing"));
		// a tag naming a block that does not exist fails to load, and takes vanilla's mining rules with it
		helper.assertTrue(Blocks.STONE.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE), Component.literal("the pickaxe tag should load"));
		helper.assertTrue(Blocks.IRON_ORE.defaultBlockState().is(net.minecraft.tags.BlockTags.NEEDS_STONE_TOOL), Component.literal("the stone tool tag should load"));
		for (Block block : BuiltInRegistries.BLOCK) {
			if (BuiltInRegistries.BLOCK.getKey(block).getNamespace().equals(Tailings.MOD_ID) && block.defaultBlockState().requiresCorrectToolForDrops()) {
				helper.assertTrue(block.defaultBlockState().is(net.minecraft.tags.BlockTags.MINEABLE_WITH_PICKAXE),
						Component.literal(block + " needs the right tool, so it must be in the pickaxe tag"));
			}
		}
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

	/** Phase 4 (full-recovery.md): a block's last 25 % of copper through scavengers, regrind, cleaners, leach, wash and electrowinning. */
	private static void fullRecovery(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity source = plant(helper, Machine.FLOTATION_BANK, new BlockPos(3, y, 0));
		source.out().offer(Product.TAILS_SLURRY, 100, dev.wildcatter.fluid.SlurryAssay.of(java.util.Map.of("minecraft:copper_ingot", 1.0 / 100)));
		pipe(helper, new BlockPos(3, y, 13));
		PlantBlockEntity scavenger = plant(helper, Machine.SCAVENGER_BANK, new BlockPos(3, y, 14));
		PlantBlockEntity blower = plant(helper, Machine.BLOWER, new BlockPos(1, y, 15));
		pipe(helper, new BlockPos(7, y, 23), new BlockPos(8, y, 23), new BlockPos(9, y, 23), new BlockPos(10, y, 23), new BlockPos(11, y, 23), new BlockPos(12, y, 23));
		PlantBlockEntity regrind = plant(helper, Machine.REGRIND_MILL, new BlockPos(12, y, 24));
		pipe(helper, new BlockPos(12, y, 29));
		PlantBlockEntity cleaner = plant(helper, Machine.CLEANER_BANK, new BlockPos(12, y, 30));
		pipe(helper, new BlockPos(12, y, 36));
		PlantBlockEntity thickener = plant(helper, Machine.THICKENER, new BlockPos(12, y, 37));
		pipe(helper, new BlockPos(12, y, 45));
		PlantBlockEntity filter = plant(helper, Machine.FILTER, new BlockPos(12, y, 46));
		pipe(helper, new BlockPos(3, y, 23));
		PlantBlockEntity leach = plant(helper, Machine.LEACH_TANK, new BlockPos(1, y, 24));
		var tankBlock = BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("wildcatter", "stock_tank"));
		var tank = part(helper, tankBlock, new BlockPos(7, y, 25), dev.wildcatter.block.StorageTankBlockEntity.class);
		tank.testFill(Product.SULFURIC_ACID, 5000);
		pipe(helper, new BlockPos(6, y, 26));
		pipe(helper, new BlockPos(3, y, 29));
		PlantBlockEntity ccd = plant(helper, Machine.CCD_THICKENER, new BlockPos(0, y, 30));
		pipe(helper, new BlockPos(11, y, 42));
		PlantBlockEntity ew = plant(helper, Machine.ELECTROWINNING_BANK, new BlockPos(7, y, 43));
		pipe(helper, new BlockPos(0, y, 42));
		PlantBlockEntity pond = plant(helper, Machine.TAILINGS_POND, new BlockPos(0, y, 43));
		for (PlantBlockEntity part : new PlantBlockEntity[] {scavenger, blower, regrind, cleaner, thickener, filter, leach, ccd, ew}) {
			part.forceRunning();
		}
		var nugget = Feeds.stack(dev.wildcatter.tailings.rock.Mineral.COPPER.nuggetId(), 1);
		helper.succeedWhen(() -> {
			int fromFilter = filter.countIn(new int[] {1, 2}, nugget), fromCells = ew.countIn(new int[] {1, 2}, nugget);
			String all = scavenger.status() + " | " + regrind.status() + " | " + cleaner.status() + " | " + thickener.status() + " | " + filter.status() + " | " + leach.status() + " | " + ccd.status() + " | " + ew.status() + " | " + pond.status();
			helper.assertTrue(!blower.status().contains("NOT BUILT"), Component.literal("a blower should serve the scavenger bank it touches: " + blower.status()));
			helper.assertTrue(fromFilter == 3, Component.literal("the scavengers' 0.4 of an ingot should pay 3 nuggets at the filter, got " + fromFilter + ": " + all));
			helper.assertTrue(fromCells == 5, Component.literal("the leach's 0.6 of an ingot should pay 5 nuggets at the cells, got " + fromCells + ": " + all));
		});
	}

	/** A little over four nuggets' worth of a mineral, in kg, so a 75 % bank pays three. */
	private static double fourNuggets(dev.wildcatter.tailings.rock.Mineral mineral) {
		return 4.04 * mineral.nuggetAmount().doubleValue();
	}

	private static int nuggets(PlantBlockEntity filter, dev.wildcatter.tailings.rock.Mineral mineral) {
		return filter.countIn(new int[] {1, 2}, Feeds.stack(mineral.nuggetId(), 1));
	}

	/** Fines on one tank, one bank, thickener, filter and a pond for the tails: {@code {tank, filter, pond}}. */
	private static PlantBlockEntity[] oneBank(GameTestHelper helper, java.util.Map<String, Double> kg, Reagent... reagents) {
		int y = 4;
		double total = 1000;
		java.util.Map<String, Double> shares = new java.util.HashMap<>();
		kg.forEach((id, m) -> shares.put(id, m / total));
		fines(helper, new BlockPos(0, y, 0), total, shares);
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
		reagents(helper, tank, reagents);
		return new PlantBlockEntity[] {tank, filter, pond};
	}

	/** selective-flotation.md: lead off the first bank, zinc off the second, each through its own filter at the full rung. */
	private static void selectiveFloat(GameTestHelper helper) {
		int y = 4;
		var galena = dev.wildcatter.tailings.rock.Mineral.GALENA;
		var sphalerite = dev.wildcatter.tailings.rock.Mineral.SPHALERITE;
		double total = 1000;
		fines(helper, new BlockPos(0, y, 0), total, java.util.Map.of(galena.mineralItemId(), fourNuggets(galena) / total, sphalerite.mineralItemId(), fourNuggets(sphalerite) / total));
		pipe(helper, new BlockPos(0, y, 2), new BlockPos(1, y, 2));
		PlantBlockEntity tank = plant(helper, Machine.CONDITIONER, new BlockPos(0, y, 3));
		tank.toggleSelective();
		pipe(helper, new BlockPos(0, y, 6));
		PlantBlockEntity lead = plant(helper, Machine.FLOTATION_BANK, new BlockPos(0, y, 7));
		PlantBlockEntity blowerOne = plant(helper, Machine.BLOWER, new BlockPos(7, y, 7));
		pipe(helper, new BlockPos(6, y, 20), new BlockPos(7, y, 20), new BlockPos(8, y, 20), new BlockPos(9, y, 20));
		PlantBlockEntity thickenerOne = plant(helper, Machine.THICKENER, new BlockPos(9, y, 21));
		pipe(helper, new BlockPos(9, y, 29));
		PlantBlockEntity filterOne = plant(helper, Machine.FILTER, new BlockPos(9, y, 30));
		pipe(helper, new BlockPos(0, y, 20));
		PlantBlockEntity activation = plant(helper, Machine.ACTIVATION_TANK, new BlockPos(0, y, 21));
		pipe(helper, new BlockPos(0, y, 24));
		PlantBlockEntity zinc = plant(helper, Machine.FLOTATION_BANK, new BlockPos(0, y, 25));
		PlantBlockEntity blowerTwo = plant(helper, Machine.BLOWER, new BlockPos(7, y, 25));
		pipe(helper, new BlockPos(6, y, 38));
		PlantBlockEntity thickenerTwo = plant(helper, Machine.THICKENER, new BlockPos(6, y, 39));
		pipe(helper, new BlockPos(6, y, 47));
		PlantBlockEntity filterTwo = plant(helper, Machine.FILTER, new BlockPos(6, y, 48));
		pipe(helper, new BlockPos(0, y, 38));
		PlantBlockEntity pond = plant(helper, Machine.TAILINGS_POND, new BlockPos(0, y, 39));
		for (PlantBlockEntity part : new PlantBlockEntity[] {tank, lead, blowerOne, thickenerOne, filterOne, activation, zinc, blowerTwo, thickenerTwo, filterTwo}) {
			part.forceRunning();
		}
		reagents(helper, tank, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC, Reagent.ZINC_SULFATE);
		reagents(helper, activation, Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC, Reagent.COPPER_SULFATE);
		helper.succeedWhen(() -> {
			String all = tank.status() + " | " + lead.status() + " | " + activation.status() + " | " + zinc.status() + " | " + filterOne.status() + " | " + filterTwo.status();
			helper.assertTrue(nuggets(filterOne, galena) == 3, Component.literal("the first filter should pay 3 lead nuggets, got " + nuggets(filterOne, galena) + ": " + all));
			helper.assertTrue(nuggets(filterTwo, sphalerite) == 3, Component.literal("the second filter should pay 3 zinc nuggets, got " + nuggets(filterTwo, sphalerite) + ": " + all));
			helper.assertTrue(nuggets(filterOne, sphalerite) == 0, Component.literal("no zinc through the lead filter: " + all));
			helper.assertTrue(nuggets(filterTwo, galena) == 0, Component.literal("no lead floats a second time: " + all));
		});
	}

	/** A selective tank short of zinc sulfate stops and names it; it does not fall back to bulk. */
	private static void selectiveNoReagent(GameTestHelper helper) {
		var sphalerite = dev.wildcatter.tailings.rock.Mineral.SPHALERITE;
		PlantBlockEntity[] line = oneBank(helper, java.util.Map.of(sphalerite.mineralItemId(), fourNuggets(sphalerite)), Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC, Reagent.COPPER_SULFATE);
		line[0].toggleSelective();
		helper.succeedWhen(() -> helper.assertTrue(line[0].status().contains("NO REAGENT: Zinc Sulfate"), Component.literal("got " + line[0].status())));
	}

	/** A selective tank's tails with no activation tank: the lead pays and the held-down zinc goes to the tails. */
	private static void noActivationTank(GameTestHelper helper) {
		var galena = dev.wildcatter.tailings.rock.Mineral.GALENA;
		var sphalerite = dev.wildcatter.tailings.rock.Mineral.SPHALERITE;
		double zinc = fourNuggets(sphalerite);
		PlantBlockEntity[] line = oneBank(helper, java.util.Map.of(galena.mineralItemId(), fourNuggets(galena), sphalerite.mineralItemId(), zinc), Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC, Reagent.ZINC_SULFATE);
		line[0].toggleSelective();
		helper.succeedWhen(() -> {
			double down = line[2].back().assayOf(Product.TAILS_SLURRY).kg(dev.wildcatter.tailings.plant.Selective.DEPRESSED, line[2].back().held(Product.TAILS_SLURRY));
			helper.assertTrue(nuggets(line[1], galena) == 3, Component.literal("the filter should pay 3 lead nuggets, got " + nuggets(line[1], galena) + ": " + line[0].status()));
			helper.assertTrue(nuggets(line[1], sphalerite) == 0, Component.literal("no zinc should reach the filter"));
			helper.assertTrue(Math.abs(down - zinc) < 1, Component.literal("all " + zinc + " kg of zinc should be in the tails, got " + down));
		});
	}

	/** A plain bank on feed holding both: lead pays at the rung, the zinc it floated is gone, and only its tails go on. */
	private static void bulkLosesZinc(GameTestHelper helper) {
		var galena = dev.wildcatter.tailings.rock.Mineral.GALENA;
		var sphalerite = dev.wildcatter.tailings.rock.Mineral.SPHALERITE;
		double zinc = fourNuggets(sphalerite);
		PlantBlockEntity[] line = oneBank(helper, java.util.Map.of(galena.mineralItemId(), fourNuggets(galena), sphalerite.mineralItemId(), zinc), Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC, Reagent.COPPER_SULFATE);
		helper.succeedWhen(() -> {
			double left = line[2].back().assayOf(Product.TAILS_SLURRY).kg(sphalerite.mineralItemId(), line[2].back().held(Product.TAILS_SLURRY));
			helper.assertTrue(nuggets(line[1], galena) == 3, Component.literal("the filter should pay 3 lead nuggets, got " + nuggets(line[1], galena) + ": " + line[0].status()));
			helper.assertTrue(nuggets(line[1], sphalerite) == 0, Component.literal("the bulk cake's zinc should be lost"));
			helper.assertTrue(Math.abs(left - zinc * 0.25) < 1, Component.literal("a quarter of the zinc should go on in the tails, got " + left));
		});
	}

	/** Feed holding only sphalerite is not bulk, and pays as it always did. */
	private static void bulkOneMineral(GameTestHelper helper) {
		var sphalerite = dev.wildcatter.tailings.rock.Mineral.SPHALERITE;
		PlantBlockEntity[] line = oneBank(helper, java.util.Map.of(sphalerite.mineralItemId(), fourNuggets(sphalerite)), Reagent.LIME, Reagent.XANTHATE, Reagent.MIBC, Reagent.COPPER_SULFATE);
		helper.succeedWhen(() -> helper.assertTrue(nuggets(line[1], sphalerite) == 3, Component.literal("the filter should pay 3 zinc nuggets, got " + nuggets(line[1], sphalerite) + ": " + line[0].status())));
	}

	/** A lined cell of Wildcatter's, 5 x 5 inside and 2 high, its tower at (3, 5, 18), with a bank's tails coming in over the rim. */
	private static PlantBlockEntity cellOnPipe(GameTestHelper helper) {
		int y = 4;
		Block liner = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "cell_liner"));
		for (int x = 0; x <= 6; x++) {
			for (int z = 15; z <= 21; z++) {
				helper.setBlock(new BlockPos(x, y, z), liner);
				if (x == 0 || x == 6 || z == 15 || z == 21) {
					helper.setBlock(new BlockPos(x, y + 1, z), liner);
					helper.setBlock(new BlockPos(x, y + 2, z), liner);
				}
			}
		}
		helper.setBlock(new BlockPos(3, y + 1, 18), BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "decant_tower")));
		PlantBlockEntity source = plant(helper, Machine.FLOTATION_BANK, new BlockPos(0, y, 0));
		source.out().offer(Product.TAILS_SLURRY, 100, dev.wildcatter.fluid.SlurryAssay.EMPTY);
		pipe(helper, new BlockPos(0, y, 13), new BlockPos(0, y, 14), new BlockPos(0, y + 1, 14), new BlockPos(0, y + 2, 14), new BlockPos(0, y + 3, 14), new BlockPos(1, y + 3, 14), new BlockPos(2, y + 3, 14),
				new BlockPos(3, y + 3, 14), new BlockPos(3, y + 3, 15), new BlockPos(3, y + 3, 16), new BlockPos(3, y + 3, 17), new BlockPos(3, y + 3, 18), new BlockPos(3, y + 2, 18));
		return source;
	}

	/** Phase 4d: tails settle in the lined cell over a real pipe, and the decant's water comes back to the mill on its own pipe. */
	private static void tailsToCell(GameTestHelper helper) {
		int y = 4;
		cellOnPipe(helper);
		Block water = BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("wildcatter", "pipe"));
		for (BlockPos at : new BlockPos[] {new BlockPos(3, y + 1, 19), new BlockPos(3, y + 2, 19), new BlockPos(3, y + 3, 19), new BlockPos(3, y + 3, 20), new BlockPos(3, y + 3, 21), new BlockPos(3, y + 3, 22)}) {
			helper.setBlock(at, water);
		}
		PlantBlockEntity mill = plant(helper, Machine.BALL_MILL, new BlockPos(0, y, 23));
		helper.succeedWhen(() -> {
			var tower = helper.getBlockEntity(new BlockPos(3, y + 1, 18), dev.wildcatter.block.DecantTowerBlockEntity.class);
			helper.assertTrue(Math.abs(tower.settledKg() - 40) < 1e-3, Component.literal("100 kg of slurry at 40 % solids should settle 40 kg, got " + tower.settledKg() + ": " + tower.status()));
			helper.assertTrue(mill.water() > 0, Component.literal("the decant should send water back to the mill: " + mill.status()));
		});
	}

	/** Phase 4d: a cell with a hole in its floor seeps and takes no tails, so they stay on the bank's outlet. */
	private static void cellBreached(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity source = cellOnPipe(helper);
		helper.setBlock(new BlockPos(1, y, 16), Blocks.DIRT);
		helper.startSequence().thenExecuteAfter(100, () -> {
			var tower = helper.getBlockEntity(new BlockPos(3, y + 1, 18), dev.wildcatter.block.DecantTowerBlockEntity.class);
			helper.assertTrue(tower.status().contains("BREACHED"), Component.literal("it should read BREACHED: " + tower.status()));
			helper.assertTrue(tower.settledKg() == 0, Component.literal("and take nothing, got " + tower.settledKg()));
			helper.assertTrue(source.out().held(Product.TAILS_SLURRY) == 100, Component.literal("the tails should wait at the bank, got " + source.out().held(Product.TAILS_SLURRY)));
		}).thenSucceed();
	}

	/** Phase 4c: lead goes in and comes back, with the silver it carried; the working time is 20 s an item. */
	private static void byproducts(GameTestHelper helper) {
		PlantBlockEntity plant = plant(helper, Machine.BYPRODUCT_PLANT, new BlockPos(0, 4, 0));
		plant.forceRunning();
		ItemStack lead = Feeds.stack("wildcatter:lead_ingot", 1);
		for (int i = 0; i < 2; i++) {
			helper.assertTrue(plant.offer(lead.copy()), Component.literal("it should take lead"));
		}
		helper.succeedWhen(() -> {
			helper.assertTrue(plant.countIn(new int[] {1, 2}, lead) == 2, Component.literal("both ingots should come back: " + plant.status()));
			helper.assertTrue(plant.byproducts().getOrDefault("wildcatter:silver_ingot", 0.0) > 0 || plant.countIn(new int[] {1, 2}, Feeds.stack("wildcatter:silver_ingot", 1)) > 0, Component.literal("and with silver"));
		});
	}

	/** Phase 4b: a heap of granite under acid: pregnant liquor to the cells, which pay its traces in nuggets. */
	private static void heapLeach(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity heap = plant(helper, Machine.HEAP_PAD, new BlockPos(0, y, 0));
		var tank = part(helper, BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.fromNamespaceAndPath("wildcatter", "stock_tank")), new BlockPos(11, y, 3), dev.wildcatter.block.StorageTankBlockEntity.class);
		tank.testFill(Product.SULFURIC_ACID, 5000);
		pipe(helper, new BlockPos(10, y, 4));
		pipe(helper, new BlockPos(0, y, 10));
		PlantBlockEntity ew = plant(helper, Machine.ELECTROWINNING_BANK, new BlockPos(0, y, 11));
		heap.forceRunning();
		ew.forceRunning();
		helper.assertTrue(heap.offer(new ItemStack(Blocks.GRANITE)), Component.literal("the heap should take granite"));
		helper.succeedWhen(() -> {
			helper.assertTrue(heap.status().contains("leaching"), Component.literal("the heap should be leaching: " + heap.status()));
			helper.assertTrue(ew.status().contains("plating"), Component.literal("and the cells plating: " + ew.status()));
		});
	}

	/** With no acid on its pipe the leach tank stops and says so. */
	private static void leachNoAcid(GameTestHelper helper) {
		int y = 4;
		PlantBlockEntity source = plant(helper, Machine.FLOTATION_BANK, new BlockPos(0, y, 0));
		source.out().offer(Product.TAILS_SLURRY, 100, dev.wildcatter.fluid.SlurryAssay.of(java.util.Map.of("minecraft:copper_ingot", 1.0 / 100)));
		pipe(helper, new BlockPos(0, y, 13));
		PlantBlockEntity leach = plant(helper, Machine.LEACH_TANK, new BlockPos(0, y, 14));
		leach.forceRunning();
		helper.succeedWhen(() -> helper.assertTrue(leach.status().contains("NO ACID"), Component.literal("it should read NO ACID, got " + leach.status())));
	}

	/** The spirals take half the cassiterite to concentrate and the filter pays it; the galena, which floats, goes on untouched (gravity-circuit.md). */
	private static void spiralBank(GameTestHelper helper) {
		int y = 4;
		var tin = dev.wildcatter.tailings.rock.Mineral.CASSITERITE;
		var lead = dev.wildcatter.tailings.rock.Mineral.GALENA;
		double tinKg = tin.itemAmount().doubleValue() * 2;
		fines(helper, new BlockPos(0, y, 0), 400, java.util.Map.of(tin.mineralItemId(), tinKg / 400, lead.mineralItemId(), 1.0 / 400));
		pipe(helper, new BlockPos(0, y, 2), new BlockPos(1, y, 2));
		PlantBlockEntity spirals = plant(helper, Machine.SPIRAL_BANK, new BlockPos(0, y, 3));
		pipe(helper, new BlockPos(0, y, 9));
		PlantBlockEntity pond = plant(helper, Machine.TAILINGS_POND, new BlockPos(0, y, 10));
		pipe(helper, new BlockPos(3, y, 9));
		pipe(helper, new BlockPos(7, y, 9), new BlockPos(6, y, 9), new BlockPos(5, y, 9), new BlockPos(4, y, 9));
		PlantBlockEntity thickener = plant(helper, Machine.THICKENER, new BlockPos(7, y, 10));
		pipe(helper, new BlockPos(7, y, 18));
		PlantBlockEntity filter = plant(helper, Machine.FILTER, new BlockPos(7, y, 19));
		for (PlantBlockEntity part : new PlantBlockEntity[] {spirals, thickener, filter}) {
			part.forceRunning();
		}
		helper.succeedWhen(() -> {
			int paid = filter.countIn(new int[] {1, 2}, Feeds.stack(tin.phaseOneProductId(), 1));
			helper.assertTrue(paid == 1, Component.literal("two items' worth of tin in should pay one (the ladder's 50 %), got " + paid + " (" + spirals.inspect().getString() + " | " + thickener.inspect().getString() + " | " + filter.inspect().getString() + " | " + pond.inspect().getString()
					+ " | out " + filter.getItem(1) + " " + filter.getItem(2) + ")"));
			helper.assertTrue(filter.countIn(new int[] {1, 2}, Feeds.stack(lead.nuggetId(), 1)) == 0, Component.literal("and no galena"));
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








	/** The rectifier (forced on) beside a north-running track at x 3 from z 0 to {@code last}, and the track fed. */
	private static void track(GameTestHelper helper, int y, int last) {
		RectifierBlockEntity box = part(helper, TailingsBlocks.TRACTION_RECTIFIER, new BlockPos(1, y, 2), RectifierBlockEntity.class);
		box.forceRunning();
		BlockState rail = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = 0; z <= last; z++) {
			helper.setBlock(new BlockPos(3, y, z), rail);
		}
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
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 8), Heading.NORTH);
		MineVehicleEntity car = car(helper, new BlockPos(3, y, 11), Heading.NORTH);
		helper.assertTrue(car.locomotiveId() != null && loco.carIds().size() == 1, Component.literal("the car should couple to the locomotive ahead"));
		car.load().add("minecraft:cobblestone", 2);
		car.refreshLoad();
		helper.succeedWhen(() -> {
			helper.assertTrue(fed.powered() && fed.conductorLive() && fed.volts() > 240, Component.literal("the rectifier should feed the track joined to it, "
					+ fed.volts() + " V"));
			helper.assertTrue(relZ(helper, loco) < 4, Component.literal("the locomotive should glide north, at z " + relZ(helper, loco) + " (" + loco.status() + ")"));
			helper.assertTrue(Math.abs(relZ(helper, car) - relZ(helper, loco) - Haul.carOffset(0)) < 0.05, Component.literal("the car should keep its place behind"));
			helper.assertTrue(car.load().total() == 2, Component.literal("the car keeps its load"));
		});
	}

	/** A locomotive and three cars run as four entities to the end of the track, wait, and come back. */
	private static void trainRuns(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 20);
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 10), Heading.NORTH);
		List<MineVehicleEntity> cars = List.of(car(helper, new BlockPos(3, y, 13), Heading.NORTH), car(helper, new BlockPos(3, y, 15), Heading.NORTH),
				car(helper, new BlockPos(3, y, 18), Heading.NORTH));
		helper.assertTrue(loco.carIds().size() == 3, Component.literal("three cars should couple, got " + loco.carIds().size()));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(relZ(helper, loco) < 2.0 && loco.speed() < 0.01,
						Component.literal("the train should run to the end of the track, at z " + relZ(helper, loco) + " (" + loco.status() + ")")))
				.thenExecute(() -> {
					int count = helper.getLevel().getEntitiesOfClass(MineVehicleEntity.class, new AABB(helper.absolutePos(new BlockPos(3, y, 0))).inflate(2, 2, 25)).size();
					helper.assertTrue(count == 4, Component.literal("four vehicle entities, got " + count));
					for (int i = 0; i < cars.size(); i++) {
						double gap = relZ(helper, cars.get(i)) - (i == 0 ? relZ(helper, loco) : relZ(helper, cars.get(i - 1)));
						helper.assertTrue(Math.abs(gap - (i == 0 ? Haul.carOffset(0) : Haul.CAR_LENGTH + Haul.COUPLING)) < 0.05, Component.literal("car " + i + " should keep its spacing, gap " + gap));
					}
				})
				.thenWaitUntil(() -> helper.assertTrue(loco.direction() < 0 && relZ(helper, loco) > 3,
						Component.literal("after its wait the train should come back, at z " + relZ(helper, loco) + " (" + loco.status() + ")")))
				.thenSucceed();
	}

	/** Through a thrown switch the locomotive turns east, and its cars follow it round. */
	private static void trainTurnout(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 12);
		helper.setBlock(new BlockPos(3, y, 1), TailingsBlocks.MINE_SWITCH.defaultBlockState().setValue(MineSwitchBlock.FACING, Direction.NORTH)
				.setValue(MineSwitchBlock.THROWN, true));
		for (int x = 4; x <= 12; x++) {
			helper.setBlock(new BlockPos(x, y, 1), TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.EAST));
		}
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 5), Heading.NORTH);
		MineVehicleEntity first = car(helper, new BlockPos(3, y, 8), Heading.NORTH);
		MineVehicleEntity second = car(helper, new BlockPos(3, y, 10), Heading.NORTH);
		helper.succeedWhen(() -> {
			helper.assertTrue(relX(helper, loco) > 7, Component.literal("the locomotive should take the branch east, at " + loco.position() + " (" + loco.status() + ")"));
			helper.assertTrue(relX(helper, second) > 4 && Math.abs(relZ(helper, second) - 1.5) < 0.05,
					Component.literal("the cars should follow it round, the second at " + relX(helper, second) + ", " + relZ(helper, second)));
			helper.assertTrue(Math.abs(relX(helper, loco) - relX(helper, first) - Haul.carOffset(0)) < 0.05, Component.literal("and keep their spacing"));
		});
	}

	/** A PLC throws a turnout under a running train's path (Wildcatter's plc.md): the train sees it, and takes the branch. */
	private static void plcSwitch(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 12);
		BlockPos switchAt = new BlockPos(3, y, 1);
		helper.setBlock(switchAt, TailingsBlocks.MINE_SWITCH.defaultBlockState().setValue(MineSwitchBlock.FACING, Direction.NORTH));
		for (int x = 4; x <= 12; x++) {
			helper.setBlock(new BlockPos(x, y, 1), TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.EAST));
		}
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 9), Heading.NORTH);
		RailBlockEntity rail = (RailBlockEntity) helper.getBlockEntity(switchAt, RailBlockEntity.class);
		helper.assertTrue(rail.points().size() == 3, Component.literal("a switch has its signals"));
		helper.assertTrue(((RailBlockEntity) helper.getBlockEntity(new BlockPos(3, y, 5), RailBlockEntity.class)).points().isEmpty(), Component.literal("a plain rail has none, so it is no device"));
		rail.commandPoint(0, true, helper.getLevel().getGameTime());
		helper.assertFalse(helper.getBlockState(switchAt).getValue(MineSwitchBlock.THROWN), Component.literal("in HAND a PLC's command does nothing"));
		rail.cycleHoa(helper.absolutePos(switchAt));
		rail.cycleHoa(helper.absolutePos(switchAt));
		helper.runAfterDelay(30, () -> rail.commandPoint(0, true, helper.getLevel().getGameTime()));
		helper.succeedWhen(() -> helper.assertTrue(relX(helper, loco) > 6, Component.literal("thrown under its path, the train should take the branch, at " + loco.position())));
	}

	/** A PLC holds a train at a signal or lets it go, and cannot let two into a section. */
	private static void plcSignal(GameTestHelper helper) {
		BlockPos at = new BlockPos(1, 6, 1);
		helper.setBlock(at, TailingsBlocks.BLOCK_SIGNAL.defaultBlockState());
		SignalBlockEntity signal = (SignalBlockEntity) helper.getBlockEntity(at, SignalBlockEntity.class);
		long now = helper.getLevel().getGameTime();
		signal.cycleHoa(helper.absolutePos(at));
		signal.cycleHoa(helper.absolutePos(at));
		signal.commandPoint(0, true, now);
		helper.assertFalse(signal.enter("one"), Component.literal("held, no train may enter"));
		signal.commandPoint(0, false, now);
		helper.assertTrue(signal.enter("one"), Component.literal("released, it may"));
		helper.assertTrue(signal.readPoint(1), Component.literal("and the PLC reads it occupied"));
		helper.assertFalse(signal.enter("two"), Component.literal("one train a section beats any PLC: there is no way to command a second in"));
		signal.commandPoint(0, true, now);
		helper.assertTrue(signal.enter("one"), Component.literal("a train already in stays in"));
		signal.leave("one");
		helper.succeed();
	}

	/** A plant part's signals: in HAND as ever, held in OFF and AUTO with no command; a part with no motor has no signals; a sorter's gates send its product to waste. */
	private static void plcPlant(GameTestHelper helper) {
		PlantBlockEntity drive = plant(helper, Machine.CONVEYOR_DRIVE, new BlockPos(1, 6, 1));
		PlantBlockEntity grizzly = plant(helper, Machine.GRIZZLY, new BlockPos(6, 6, 1));
		PlantBlockEntity sorter = plant(helper, Machine.ORE_SORTER, new BlockPos(12, 6, 1));
		helper.assertTrue(drive.points().size() == 6, Component.literal("a part with a motor has its six signals: " + drive.points()));
		helper.assertTrue(grizzly.points().isEmpty(), Component.literal("a part with no motor has none"));
		helper.assertTrue(sorter.points().size() == 10, Component.literal("a sorter adds a gate a side: " + sorter.points()));
		BlockPos cell = helper.absolutePos(new BlockPos(1, 6, 1));
		helper.assertFalse(drive.readPoint(3), Component.literal("a new part is in HAND"));
		drive.cycleHoa(cell);
		drive.cycleHoa(cell);
		helper.assertTrue(drive.readPoint(3), Component.literal("two steps of the key is AUTO"));
		BlockPos sorterCell = helper.absolutePos(new BlockPos(12, 6, 1));
		sorter.cycleHoa(sorterCell);
		sorter.cycleHoa(sorterCell);
		sorter.sorter().set(1, "minecraft:copper_ore");
		sorter.sorter().setWaste(2);
		helper.assertTrue(sorter.sorter().route("minecraft:copper_ore") == 1, Component.literal("its gate open, the ore goes by its side"));
		sorter.commandPoint(7, true, helper.getLevel().getGameTime());
		helper.assertTrue(sorter.sorter().route("minecraft:copper_ore") == 2, Component.literal("its gate shut by the PLC, it goes to waste"));
		sorter.commandPoint(7, false, helper.getLevel().getGameTime());
		helper.assertTrue(sorter.sorter().route("minecraft:copper_ore") == 1, Component.literal("and back"));
		helper.succeed();
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
		MineVehicleEntity car = car(helper, new BlockPos(3, y, 18), Heading.NORTH);
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
		track(helper, y, 20);
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
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 9), Heading.NORTH);
		MineVehicleEntity first = car(helper, new BlockPos(3, y, 12), Heading.NORTH);
		MineVehicleEntity second = car(helper, new BlockPos(3, y, 14), Heading.NORTH);
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
		track(helper, y, 8);
		DumpBlockEntity pocket = part(helper, TailingsBlocks.DUMP_POCKET, new BlockPos(2, y - 3, 3), DumpBlockEntity.class);
		part(helper, TailingsBlocks.DUMP_RAMP, new BlockPos(4, y, 3), PlantBlockEntity.class);
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 1), Heading.NORTH);
		MineVehicleEntity car = car(helper, new BlockPos(3, y, 4), Heading.NORTH);
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

	/**
	 * A 14 t train (the locomotive, three cars, one of them full) climbs the 1-in-2 rack decline on the rectifier's 250 V
	 * (haulage.md, check 2): the rectifier's DC amps rise while it climbs, and it climbs at about 1.2 m/s, power over force.
	 */
	private static void tractionDc(GameTestHelper helper) {
		int top = 64;
		RectifierBlockEntity box = part(helper, TailingsBlocks.TRACTION_RECTIFIER, new BlockPos(1, top - 3, 2), RectifierBlockEntity.class);
		box.forceRunning();
		BlockState flat = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = 0; z <= 10; z++) {
			helper.setBlock(new BlockPos(3, top - 3, z), flat);
		}
		for (int k = 0; k < 3; k++) {
			helper.setBlock(new BlockPos(3, top - k, 16 - 2 * k), flat.setValue(MineRailBlock.GRADE, RailGrade.UPPER).setValue(MineRailBlock.RACK, true));
			helper.setBlock(new BlockPos(3, top - k, 15 - 2 * k), flat.setValue(MineRailBlock.GRADE, RailGrade.LOWER).setValue(MineRailBlock.RACK, true));
		}
		for (int z = 17; z <= 26; z++) {
			helper.setBlock(new BlockPos(3, top, z), flat);
		}
		MineVehicleEntity loco = loco(helper, new BlockPos(3, top - 3, 10), Heading.SOUTH);
		MineVehicleEntity full = car(helper, new BlockPos(3, top - 3, 7), Heading.SOUTH);
		car(helper, new BlockPos(3, top - 3, 5), Heading.SOUTH);
		car(helper, new BlockPos(3, top - 3, 2), Heading.SOUTH);
		full.load().add("minecraft:cobblestone", Haul.CAR_ITEMS);
		helper.assertTrue(loco.carIds().size() == 3, Component.literal("three cars should couple, got " + loco.carIds().size()));
		double[] seen = {0, 0, 0};   // fastest on the upper grade, slowest there, most amps
		helper.onEachTick(() -> {
			double z = relZ(helper, loco);
			if (z > 13.5 && z < 16.5) {
				seen[0] = Math.max(seen[0], loco.speed());
				seen[1] = seen[1] == 0 ? loco.speed() : Math.min(seen[1], loco.speed());
				seen[2] = Math.max(seen[2], box.amps());
			}
		});
		helper.succeedWhen(() -> {
			helper.assertTrue(relZ(helper, loco) > 18, Component.literal("the train should climb to the top, at " + loco.position() + " (" + loco.status() + ")"));
			helper.assertTrue(Math.abs(seen[0] - 1.18) < 0.08 && seen[1] > 1.0, Component.literal("it should climb at about 1.2 m/s, between "
					+ seen[1] + " and " + seen[0]));
			helper.assertTrue(seen[2] > 250, Component.literal("the rectifier's amps should rise while it climbs, most " + seen[2]));
		});
	}

	/** Twice the rectifier's rating held on the track trips its DC breaker after a second; the track goes dead; a reset feeds it. */
	private static void tractionTrip(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 6);
		RectifierBlockEntity box = helper.getBlockEntity(new BlockPos(1, y, 2), RectifierBlockEntity.class);
		RailBlockEntity rail = helper.getBlockEntity(new BlockPos(3, y, 5), RailBlockEntity.class);
		boolean[] loading = {true};
		helper.onEachTick(() -> {
			if (loading[0]) {
				rail.draw(2 * Haul.RECTIFIER_WATTS);
			}
		});
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(box.tripped(), Component.literal("the DC breaker should trip, " + box.amps() + " A")))
				.thenExecute(() -> loading[0] = false)
				.thenWaitUntil(() -> helper.assertFalse(rail.powered(), Component.literal("the track should go dead")))
				.thenExecute(() -> helper.assertTrue(box.reset(), Component.literal("a click resets it")))
				.thenWaitUntil(() -> helper.assertTrue(rail.powered() && rail.volts() > 240, Component.literal("the track is fed again")))
				.thenSucceed();
	}

	/** Track with no rectifier solves, dead (its insulators' leak keeps it from floating), and a rectifier placed on it feeds it. */
	private static void tractionUnfed(GameTestHelper helper) {
		int y = 60;
		BlockState north = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = 0; z <= 6; z++) {
			helper.setBlock(new BlockPos(3, y, z), north);
		}
		RailBlockEntity rail = helper.getBlockEntity(new BlockPos(3, y, 5), RailBlockEntity.class);
		helper.startSequence()
				.thenExecuteAfter(30, () -> {
					helper.assertFalse(rail.powered(), Component.literal("unfed track is dead"));
					helper.assertTrue(rail.volts() == 0 && rail.solved(), Component.literal("and it solves at 0 V, got " + rail.volts() + " V, solved " + rail.solved()));
					part(helper, TailingsBlocks.TRACTION_RECTIFIER, new BlockPos(1, y, 2), RectifierBlockEntity.class).forceRunning();
				})
				.thenWaitUntil(() -> helper.assertTrue(rail.powered() && rail.volts() > 240, Component.literal("fed once a rectifier stands on it")))
				.thenSucceed();
	}

	/** Along 120 m of conductor rail the volts fall under a 300 kW load at its far end: 0.02 ohm/km. */
	private static void tractionFar(GameTestHelper helper) {
		int y = 60;
		ChunkPos origin = ChunkPos.containing(helper.absolutePos(BlockPos.ZERO));
		for (int dz = 2; dz <= 9; dz++) {
			for (int dx = -1; dx <= 1; dx++) {
				helper.getLevel().setChunkForced(origin.x() + dx, origin.z() + dz, true);
			}
		}
		track(helper, y, 120);
		RailBlockEntity near = helper.getBlockEntity(new BlockPos(3, y, 3), RailBlockEntity.class);
		RailBlockEntity far = helper.getBlockEntity(new BlockPos(3, y, 120), RailBlockEntity.class);
		helper.onEachTick(() -> far.draw(Haul.RECTIFIER_WATTS));
		helper.succeedWhen(() -> {
			helper.assertTrue(far.powered(), Component.literal("the far end should be fed, " + far.volts() + " V"));
			double drop = near.volts() - far.volts();
			helper.assertTrue(drop > 1.5 && drop < 5, Component.literal("about 1,200 A over 117 m of rail should drop 2.8 V, got " + drop));
		});
	}

	/**
	 * The TBM built as blocks facing north (tunnel-boring-machine.md): the cutterhead's main cell at {@code c}, the segment erector
	 * behind it, the main beam down the middle with its cab at the tail, the track layer under the beam's rear, the dewatering pump
	 * against the beam's tail, a cable junction behind, and a stone face 8 x 8 by {@code depth} ahead with stone under it. Returns
	 * the cab, stocked and forced on, programmed to drive flat.
	 */
	private static DrillBlockEntity tbm(GameTestHelper helper, BlockPos c, int depth, boolean withTrackLayer) {
		for (int z = 1; z <= depth; z++) {
			for (int x = 0; x < 8; x++) {
				for (int y = -1; y < 8; y++) {
					helper.setBlock(c.offset(x, y, -z), Blocks.STONE);
				}
			}
		}
		part(helper, TailingsBlocks.CUTTERHEAD, c, null);
		part(helper, TailingsBlocks.SEGMENT_ERECTOR, c.offset(0, 0, 1), null);
		DrillBlockEntity cab = part(helper, TailingsBlocks.MAIN_BEAM, c.offset(3, 3, 3), DrillBlockEntity.class);
		if (withTrackLayer) {
			part(helper, TailingsBlocks.TRACK_LAYER, c.offset(1, 2, 8), null);
		}
		helper.setBlock(c.offset(-2, 0, 14), TailingsBlocks.CABLE_JUNCTION);
		cab.forceRunning();
		cab.program().setTargetY(helper.absolutePos(c).getY() + 1);
		cab.program().setFlatLength(32);
		cab.setItem(DrillBlockEntity.SLOT_RAIL, new ItemStack(TailingsBlocks.MINE_RAIL, 64));
		cab.setItem(DrillBlockEntity.SLOT_GRAVEL, new ItemStack(Items.GRAVEL, 64));
		cab.setItem(DrillBlockEntity.SLOT_COBBLE, new ItemStack(Items.COBBLESTONE, 64));
		cab.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
		BlockState rail = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = 11; z <= 36; z++) {
			helper.setBlock(c.offset(2, 1, z), rail);
			helper.setBlock(c.offset(5, 1, z), rail);
		}
		for (TbmGantry kind : TbmGantry.values()) {
			GantryEntity gantry = GantryEntity.place(helper.getLevel(), helper.absolutePos(c.offset(2, 1, kind.row())), kind, Direction.NORTH);
			helper.assertTrue(gantry != null, Component.literal("a gantry should go on the rail"));
			String said = gantry.couple();
			helper.assertTrue(said.equals("coupled"), Component.literal(kind.displayName() + " should couple, said: " + said));
		}
		return cab;
	}

	private static GantryEntity gantry(GameTestHelper helper, TbmCab cab, TbmGantry kind) {
		return (GantryEntity) helper.getLevel().getEntity(cab.core().gantry(kind));
	}

	/** The running machine near {@code c}, or null. */
	private static TbmEntity machine(GameTestHelper helper, BlockPos c) {
		List<TbmEntity> found = helper.getLevel().getEntitiesOfClass(TbmEntity.class, new AABB(helper.absolutePos(c)).inflate(20));
		return found.isEmpty() ? null : found.get(0);
	}

	/** Starts the built machine at {@code ticks} ticks a block. */
	private static void go(DrillBlockEntity cab, int ticks) {
		cab.core().setTestTicks(ticks);
		cab.press(10, null);
	}

	/** Keeps the running machine's segments up: a ring is 28 of them, and a stack is 64. */
	private static void feed(GameTestHelper helper, BlockPos c) {
		helper.onEachTick(() -> {
			TbmEntity machine = machine(helper, c);
			if (machine != null) {
				machine.setItem(DrillBlockEntity.SLOT_SEGMENTS, new ItemStack(TailingsBlocks.LINING, 64));
			}
		});
	}

	private static int ringAt(GameTestHelper helper, BlockPos c, int z) {
		int lined = 0;
		for (int x = 0; x < 8; x++) {
			for (int y = 0; y < 8; y++) {
				if (helper.getBlockState(c.offset(x, y, z)).getBlock() == TailingsBlocks.LINING) {
					lined++;
				}
			}
		}
		return lined;
	}

	/**
	 * One advance and stop: the machine lifts, cuts the face to muck, glides a block on and sets back down as blocks, a ring of 28
	 * segments behind the segment erector and the floor and twin track, on ballast, behind the track layer.
	 */
	private static void drill(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 4, true);
		go(cab, 10);
		helper.succeedWhen(() -> {
			TbmEntity machine = machine(helper, c);
			if (machine != null && machine.core().drill().advanceCount() >= 1) {
				machine.core().setRunning(false);
			}
			helper.assertTrue(machine == null, Component.literal("it should set down"));
			helper.assertTrue(helper.getBlockState(c.offset(0, 0, -1)).getBlock() == TailingsBlocks.CUTTERHEAD
					&& helper.getBlockState(c.offset(3, 3, 2)).getBlock() == TailingsBlocks.MAIN_BEAM, Component.literal("every part should be one block on"));
			int lined = ringAt(helper, c, 2);
			helper.assertTrue(lined == 28, Component.literal("a ring of 28 segments behind the erector, got " + lined));
			BlockState rail = helper.getBlockState(c.offset(2, 1, 10));
			helper.assertTrue(rail.getBlock() == TailingsBlocks.MINE_RAIL && rail.getValue(MineRailBlock.LOOK) == FloorLook.BALLAST,
					Component.literal("track on ballast behind the track layer, got " + rail));
			helper.assertTrue(helper.getBlockState(c.offset(5, 1, 10)).getBlock() == TailingsBlocks.MINE_RAIL, Component.literal("the second track"));
			DrillBlockEntity now = helper.getBlockEntity(c.offset(3, 3, 2), DrillBlockEntity.class);
			int muck = 0;
			for (int slot = DrillBlockEntity.INPUTS; slot < DrillBlockEntity.SLOTS; slot++) {
				muck += now.getItem(slot).getCount();
			}
			helper.assertTrue(muck == 60, Component.literal("64 cut, 4 kept for the floor: 60 muck, got " + muck));
			helper.assertTrue(now.getItem(DrillBlockEntity.SLOT_GRAVEL).getCount() == 62 && now.getItem(DrillBlockEntity.SLOT_SEGMENTS).getCount() == 36,
					Component.literal("ballast under both rails and a crafted ring spent"));
			helper.assertFalse(now.running(), Component.literal("and it stays stopped"));
		});
	}

	/** Started, every cell of the machine becomes air and one entity carries its cab, slots and all. */
	private static void tbmLift(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 4, true);
		go(cab, 200);
		helper.succeedWhen(() -> {
			for (TbmMachine.Cell cell : TbmMachine.cells(Direction.NORTH)) {
				helper.assertTrue(helper.getBlockState(c.offset(cell.x(), cell.y(), cell.z())).isAir(), Component.literal("cell should be air: " + cell));
			}
			List<TbmEntity> machines = helper.getLevel().getEntitiesOfClass(TbmEntity.class, new AABB(helper.absolutePos(c)).inflate(20));
			helper.assertTrue(machines.size() == 1, Component.literal("one machine, got " + machines.size()));
			helper.assertTrue(machines.get(0).getItem(DrillBlockEntity.SLOT_RAIL).getCount() > 0, Component.literal("the cab's slots go with it"));
			helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new AABB(helper.absolutePos(c)).inflate(20)).isEmpty(),
					Component.literal("and nothing drops"));
		});
	}

	/** It glides, not hops: part-way through an advance it stands between two blocks; four advances line four rings and lay four rows. */
	private static void tbmGlide(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 6, true);
		feed(helper, c);
		go(cab, 10);
		double start = helper.absolutePos(c).getZ();
		boolean[] between = {false};
		helper.succeedWhen(() -> {
			TbmEntity machine = machine(helper, c);
			helper.assertTrue(machine != null, Component.literal("it should be running"));
			between[0] |= machine.getZ() != Math.floor(machine.getZ()) && machine.getZ() < start;
			helper.assertTrue(machine.core().drill().advanceCount() > 4, Component.literal("four advances lined, at " + machine.core().readout()));
			helper.assertTrue(between[0], Component.literal("it should pass between whole blocks"));
			for (int i = 0; i < 4; i++) {
				int lined = ringAt(helper, c, 2 - i);
				helper.assertTrue(lined == 28, Component.literal("ring " + i + " should have 28 segments, got " + lined));
				helper.assertTrue(helper.getBlockState(c.offset(2, 1, 10 - i)).getBlock() == TailingsBlocks.MINE_RAIL
						&& helper.getBlockState(c.offset(5, 1, 10 - i)).getBlock() == TailingsBlocks.MINE_RAIL, Component.literal("both tracks at row " + i));
			}
		});
	}

	/** Stopped on the flat with something in its cells it says where and stays; cleared, it sets down. */
	private static void tbmSetDown(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 4, true);
		go(cab, 40);
		BlockPos wall = c.offset(3, 3, 5);
		helper.runAfterDelay(10, () -> {
			machine(helper, c).core().setRunning(false);
			helper.setBlock(wall, Blocks.OBSIDIAN);
		});
		helper.runAfterDelay(90, () -> {
			TbmEntity machine = machine(helper, c);
			helper.assertTrue(machine != null && machine.core().note().contains("can't set down"), Component.literal("it should say why it stays"));
			helper.assertTrue(machine.core().note().contains(String.valueOf(helper.absolutePos(wall).getZ())), Component.literal("and where: " + machine.core().note()));
			helper.setBlock(wall, Blocks.AIR);
		});
		helper.runAfterDelay(92, () -> helper.succeedWhen(() -> {
			helper.assertTrue(machine(helper, c) == null, Component.literal("cleared, it sets down"));
			helper.assertTrue(helper.getBlockState(c.offset(0, 0, -1)).getBlock() == TailingsBlocks.CUTTERHEAD, Component.literal("the parts are blocks again"));
		}));
	}

	/** Stopped on the decline it parks as it is, pitched, its cab still working, and goes on when started. */
	private static void tbmPark(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 8, true);
		for (int z = 1; z <= 8; z++) {
			for (int x = 0; x < 8; x++) {
				for (int y = -8; y < -1; y++) {
					helper.setBlock(c.offset(x, y, -z), Blocks.STONE);
				}
			}
		}
		cab.program().setTargetY(helper.absolutePos(c).getY() - 6);
		feed(helper, c);
		go(cab, 5);
		int[] phase = {0};
		int[] at = {0};
		helper.succeedWhen(() -> {
			TbmEntity machine = machine(helper, c);
			helper.assertTrue(machine != null, Component.literal("it should stay an entity on the decline"));
			if (phase[0] == 0) {
				helper.assertTrue(machine.core().drill().advanceCount() >= 5, Component.literal("driving down"));
				machine.core().setRunning(false);
				at[0] = machine.core().drill().advanceCount();
				phase[0] = 1;
			}
			if (phase[0] == 1) {
				helper.assertTrue(machine.core().waitTicks() == 0 && !machine.core().note().isEmpty(), Component.literal("parked, saying so"));
				helper.assertTrue(machine.pitch() > 5, Component.literal("pitched down the grade, got " + machine.pitch()));
				var player = helper.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
				player.setPos(machine.position());
				helper.assertTrue(machine.getItem(DrillBlockEntity.SLOT_RAIL).getCount() > 0 && machine.stillValid(player), Component.literal("the cab still works"));
				machine.press(10, null);
				phase[0] = 2;
			}
			helper.assertTrue(machine.core().running() && machine.core().drill().advanceCount() > at[0], Component.literal("started, it goes on"));
		});
	}

	/** A sneaking click on the stopped machine drops every part and every slot. */
	private static void tbmDismantle(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 4, true);
		go(cab, 200);
		helper.runAfterDelay(5, () -> {
			TbmEntity machine = machine(helper, c);
			machine.core().setRunning(false);
			machine.dismantle(helper.getLevel());
			helper.assertTrue(machine.isRemoved(), Component.literal("the machine should go"));
			BlockPos at = helper.relativePos(machine.blockPosition());
			for (var item : List.of(TailingsBlocks.CUTTERHEAD.asItem(), TailingsBlocks.SEGMENT_ERECTOR.asItem(), TailingsBlocks.MAIN_BEAM.asItem(),
					TailingsBlocks.TRACK_LAYER.asItem(), TailingsBlocks.MINE_RAIL.asItem(), Items.GRAVEL)) {
				helper.assertItemEntityPresent(item, at, 4);
			}
			helper.succeed();
		});
	}

	/** The Ride button seats a player in the cab, where it rides with the machine, up on the main beam's tail. */
	private static void tbmSeat(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 4, true);
		go(cab, 200);
		helper.runAfterDelay(5, () -> {
			TbmEntity machine = machine(helper, c);
			var rider = helper.spawn(net.minecraft.world.entity.EntityTypes.PIG, c.offset(3, 8, 10));
			rider.startRiding(machine, true, false);
			helper.assertTrue(rider.getVehicle() == machine, Component.literal("it should take a rider"));
			helper.succeedWhen(() -> helper.assertTrue(Math.abs(rider.getY() - (machine.getY() + 5)) < 0.01 && Math.abs(rider.getZ() - (machine.getZ() + 10.5)) < 0.01
					&& Math.abs(rider.getX() - (machine.getX() + 4)) < 0.01, Component.literal("at the seat, got " + rider.position() + " from " + machine.position())));
		});
	}

	/** Each gantry is set at its place behind the running machine, in order, on the track it lays; a train stops short of the last. */
	private static void tbmGantries(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 6, true);
		feed(helper, c);
		go(cab, 10);
		helper.succeedWhen(() -> {
			TbmEntity machine = machine(helper, c);
			helper.assertTrue(machine != null && machine.core().drill().advanceCount() > 4, Component.literal("four advances on"));
			for (TbmGantry kind : TbmGantry.values()) {
				GantryEntity gantry = gantry(helper, machine, kind);
				helper.assertTrue(gantry.coupled() && Math.abs(gantry.getZ() - (machine.getZ() + kind.row())) < 1e-6 && gantry.getX() == machine.getX() + 1
						&& gantry.getY() == machine.getY() + 1, Component.literal(kind.displayName() + " should keep its place, at " + gantry.position() + " from " + machine.position()));
			}
			BlockPos under = helper.absolutePos(c.offset(2, 1, 20));
			helper.assertTrue(GantryEntity.standingAt(helper.getLevel(), under), Component.literal("a train should find a gantry over the track"));
		});
	}

	/** A PLC's GO through the cable junction starts the machine as its cab's own Go does, gantries and all: nobody has pressed anything. */
	private static void plcTbmGo(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 6, true);
		feed(helper, c);
		cab.core().setTestTicks(10);
		BlockPos junctionAt = helper.absolutePos(c.offset(-2, 0, 14));
		var junction = (dev.wildcatter.tailings.block.CableJunctionBlockEntity) helper.getLevel().getBlockEntity(junctionAt);
		junction.cycleHoa(junctionAt);
		junction.cycleHoa(junctionAt);   // HAND, OFF, AUTO
		helper.onEachTick(() -> junction.commandPoint(0, true, helper.getLevel().getGameTime()));
		helper.succeedWhen(() -> {
			TbmEntity machine = machine(helper, c);
			helper.assertTrue(machine != null && machine.core().drill().advanceCount() > 2, Component.literal("a GO from the junction should drive it: " + cab.core().readout() + " / " + cab.core().note()));
			helper.assertTrue(junction.readPoint(1), Component.literal("and the junction should say RUNNING"));
		});
	}

	/** A car standing just behind the conveyor's tail takes the machine's muck, and the conveyor draws its power. */
	private static void tbmMuck(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 4, true);
		go(cab, 200);
		helper.runAfterDelay(20, () -> {
			TbmEntity machine = machine(helper, c);
			helper.assertTrue(machine.core().drill().advanceCount() == 1 && machine.core().hasMuck(), Component.literal("it should have cut a face: " + machine.core().readout()));
			// the machine has moved a block: its loading point with it
			var car = MineVehicleEntity.placeCar(helper.getLevel(), helper.absolutePos(c.offset(2, 1, 27)), dev.wildcatter.tailings.mine.Heading.NORTH);
			helper.succeedWhen(() -> {
				helper.assertTrue(car.load().total() > 0, Component.literal("the car should take muck"));
				helper.assertTrue(machine.core().drawWatts() == TbmCore.BEAM_WATTS + TbmCore.CONVEYOR_WATTS, Component.literal("the conveyor draws while it moves muck, got " + machine.core().drawWatts()));
			});
		});
	}

	/** A real train run up behind the conveyor stops short of it with its first car under the chute, at real length, and loads. */
	private static void tbmTrainLoads(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 5);
		DrillBlockEntity cab = tbm(helper, c, 4, true);
		BlockState rail = TailingsBlocks.MINE_RAIL.defaultBlockState().setValue(MineRailBlock.FACING, Direction.NORTH);
		for (int z = 11; z <= 40; z++) {
			helper.setBlock(c.offset(2, 1, z), rail);
		}
		part(helper, TailingsBlocks.TRACTION_RECTIFIER, c.offset(0, 1, 36), RectifierBlockEntity.class).forceRunning();
		go(cab, 200);
		helper.runAfterDelay(20, () -> {
			MineVehicleEntity loco = loco(helper, c.offset(2, 1, 33), Heading.NORTH);
			MineVehicleEntity first = car(helper, c.offset(2, 1, 36), Heading.NORTH);
			MineVehicleEntity second = car(helper, c.offset(2, 1, 38), Heading.NORTH);
			helper.succeedWhen(() -> {
				helper.assertTrue(first.load().total() > 0, Component.literal("the first car should take muck behind the conveyor, train at z " + relZ(helper, loco) + " (" + loco.status() + ")"));
				helper.assertTrue(second.locomotiveId() != null, Component.literal("and the second should stay coupled"));
			});
		});
	}

	/** The cable all paid out, it stops and says so. */
	private static void tbmCableEnd(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 4, true);
		feed(helper, c);
		cab.core().setTestCable(2);
		go(cab, 5);
		helper.succeedWhen(() -> {
			helper.assertTrue(machine(helper, c) == null, Component.literal("on the flat it sets down"));
			DrillBlockEntity now = helper.getBlockEntity(c.offset(3, 3, 1), DrillBlockEntity.class);
			helper.assertTrue(now.status() == DrillCodes.STOP_CABLE && now.drill().advanceCount() == 2 && now.core().note().contains("cable"),
					Component.literal("it should read cable end, got " + now.core().readout() + " / " + now.core().note()));
		});
	}

	/** A gantry broken stops the machine, naming it; put back and coupled, the machine goes on. */
	private static void tbmGantryMissing(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 6, true);
		feed(helper, c);
		go(cab, 5);
		helper.runAfterDelay(10, () -> {
			TbmEntity machine = machine(helper, c);
			gantry(helper, machine, TbmGantry.REEL).discard();
		});
		int[] at = {0};
		helper.runAfterDelay(30, () -> {
			TbmEntity machine = machine(helper, c);
			helper.assertTrue(machine.core().status() == DrillCodes.NOT_BUILT && machine.core().note().contains("Reel Gantry"),
					Component.literal("it should name the reel gantry, got " + machine.core().note()));
			at[0] = machine.core().drill().advanceCount();
		});
		helper.runAfterDelay(50, () -> {
			TbmEntity machine = machine(helper, c);
			helper.assertTrue(machine.core().drill().advanceCount() == at[0], Component.literal("and it should not cut"));
			Vec3 slot = TbmBackup.slot(machine, TbmGantry.REEL);
			GantryEntity reel = GantryEntity.place(helper.getLevel(), BlockPos.containing(slot.x + 1, slot.y, slot.z), TbmGantry.REEL, Direction.NORTH);
			helper.assertTrue(reel != null && reel.couple().equals("coupled"), Component.literal("a new reel gantry should couple"));
			helper.succeedWhen(() -> helper.assertTrue(machine.core().drill().advanceCount() > at[0] && machine.core().status() == DrillCodes.DRIVING, Component.literal("and it goes on")));
		});
	}

	/** The machine will not start without its gantries, in order. */
	private static void tbmUncoupled(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 2, true);
		helper.runAfterDelay(5, () -> {
			GantryEntity reel = gantry2(helper, c, TbmGantry.REEL);
			TbmBackup.release(helper.getLevel(), cab.core());
			helper.assertTrue(reel.couple().contains("Pump Gantry"), Component.literal("gantries couple in order"));
			cab.press(10, null);
			helper.succeedWhen(() -> {
				helper.assertTrue(cab.status() == DrillCodes.NOT_BUILT && cab.core().note().contains("Pump Gantry"), Component.literal("it should ask for the pump gantry, got " + cab.core().note()));
				helper.assertTrue(machine(helper, c) == null, Component.literal("and not lift"));
			});
		});
	}

	private static GantryEntity gantry2(GameTestHelper helper, BlockPos c, TbmGantry kind) {
		return helper.getLevel().getEntitiesOfClass(GantryEntity.class, new AABB(helper.absolutePos(c)).inflate(20), e -> e.kind() == kind).get(0);
	}

	/** Far enough down the decline that the gantries are on the grade too: pitched, at the floor's height, and the machine still driving. */
	private static void tbmGantriesDecline(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 19, true);
		for (int z = 1; z <= 19; z++) {
			for (int x = 0; x < 8; x++) {
				for (int y = -12; y < -1; y++) {
					helper.setBlock(c.offset(x, y, -z), Blocks.STONE);
				}
			}
		}
		cab.program().setTargetY(helper.absolutePos(c).getY() - 10);
		feed(helper, c);
		go(cab, 2);
		helper.succeedWhen(() -> {
			TbmEntity machine = machine(helper, c);
			helper.assertTrue(machine != null, Component.literal("it should be driving"));
			helper.assertTrue(machine.core().status() == DrillCodes.DRIVING, Component.literal("it should keep driving, got " + machine.core().readout() + " / " + machine.core().note()));
			helper.assertTrue(machine.core().drill().advanceCount() >= 18, Component.literal("eighteen blocks down, at " + machine.core().readout()));
			GantryEntity pump = gantry(helper, machine, TbmGantry.PUMP);
			helper.assertTrue(pump.pitch() > 5, Component.literal("the pump gantry should be on the grade, pitch " + pump.pitch()));
			double floor = machine.core().drill().floorExact(machine.machineSlice() - TbmGantry.PUMP.row());
			helper.assertTrue(Math.abs(pump.getY() - floor) < 0.01, Component.literal("and on the floor: " + pump.getY() + " against " + floor));
			helper.assertTrue(pump.getY() > machine.getY() + 1, Component.literal("and above the machine's nose"));
		});
	}

	/** A train stops short of a gantry over the track, and turns back there as at the end of the line. */
	private static void tbmTrainStops(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 24);
		GantryEntity gantry = GantryEntity.place(helper.getLevel(), helper.absolutePos(new BlockPos(3, y, 2)), TbmGantry.CONVEYOR, Direction.NORTH);
		helper.assertTrue(gantry != null, Component.literal("the gantry should go on the rail"));
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 18), Heading.NORTH);
		car(helper, new BlockPos(3, y, 21), Heading.NORTH);
		helper.onEachTick(() -> helper.assertTrue(relZ(helper, loco) > 10, Component.literal("it should not run under the gantry, at z " + relZ(helper, loco))));
		helper.startSequence()
				.thenWaitUntil(() -> helper.assertTrue(loco.direction() < 0 && relZ(helper, loco) > 13,
						Component.literal("it should turn back from the gantry, at z " + relZ(helper, loco) + " (" + loco.status() + ")")))
				.thenSucceed();
	}

	/** A plant part sounds only with its motor running and work to do: a forced jaw with no rock is quiet, and sounds once fed. */
	private static void plantSound(GameTestHelper helper) {
		PlantBlockEntity jaw = plant(helper, Machine.JAW, new BlockPos(0, 4, 0));
		jaw.forceRunning();
		helper.startSequence()
				.thenIdle(5)
				.thenExecute(() -> {
					helper.assertTrue(jaw.loop() == null, Component.literal("a part with no work should be quiet"));
					helper.assertTrue(jaw.offer(new ItemStack(Blocks.COPPER_ORE)), Component.literal("the jaw should take ore"));
				})
				.thenWaitUntil(() -> helper.assertTrue(jaw.loop() == dev.wildcatter.tailings.TailingsSounds.JAW_CRUSHER, Component.literal("a working jaw should sound (" + jaw.status() + ")")))
				.thenSucceed();
	}

	/** Behind a gantry moving at the machine's real rate, one block in 20 seconds, the train still turns back. */
	private static void tbmTrainTurns(GameTestHelper helper) {
		int y = 60;
		track(helper, y, 40);
		GantryEntity gantry = GantryEntity.place(helper.getLevel(), helper.absolutePos(new BlockPos(3, y, 10)), TbmGantry.CONVEYOR, Direction.NORTH);
		helper.assertTrue(gantry != null, Component.literal("the gantry should go on the rail"));
		MineVehicleEntity loco = loco(helper, new BlockPos(3, y, 26), Heading.NORTH);
		car(helper, new BlockPos(3, y, 29), Heading.NORTH);
		helper.onEachTick(() -> gantry.setAt(gantry.position().add(0, 0, -0.0025), Direction.NORTH, 0));
		helper.succeedWhen(() -> helper.assertTrue(loco.direction() < 0, Component.literal("it should turn back from the moving gantry, at z " + relZ(helper, loco) + " (" + loco.status() + ")")));
	}

	/** A machine with no track layer stays blocks, reads NOT BUILT and names it. */
	private static void tbmBuilt(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 2, false);
		cab.press(10, null);
		helper.succeedWhen(() -> {
			helper.assertTrue(cab.status() == DrillCodes.NOT_BUILT && cab.core().note().contains("Track Layer"),
					Component.literal("it should name the missing track layer, got " + cab.core().readout()));
			helper.assertTrue(machine(helper, c) == null && helper.getBlockState(c).getBlock() == TailingsBlocks.CUTTERHEAD, Component.literal("and it should not lift"));
		});
	}

	/** With its muck buffer full the machine pauses, and goes on once a car takes the muck. */
	private static void drillPause(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 4, true);
		for (int slot = DrillBlockEntity.INPUTS; slot < DrillBlockEntity.SLOTS; slot++) {
			cab.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
		}
		go(cab, 10);
		helper.runAfterDelay(10, () -> {
			TbmEntity machine = machine(helper, c);
			helper.assertTrue(machine.core().status() == DrillCodes.STOP_BUFFER, Component.literal("a full buffer should pause it, got " + machine.core().readout()));
			helper.assertTrue(machine.core().drill().advanceCount() == 0 && machine.core().running(), Component.literal("waiting, not cutting"));
			for (int slot = DrillBlockEntity.INPUTS; slot < DrillBlockEntity.SLOTS; slot++) {
				machine.setItem(slot, ItemStack.EMPTY);
			}
			helper.succeedWhen(() -> helper.assertTrue(machine.core().drill().advanceCount() >= 1, Component.literal("with room it goes on")));
		});
	}

	/** Something solid in the machine's way stops it as blocked, uncut; cleared and started, it goes on. */
	private static void drillBlocked(GameTestHelper helper) {
		BlockPos c = new BlockPos(4, 40, 20);
		DrillBlockEntity cab = tbm(helper, c, 4, true);
		BlockPos wall = c.offset(1, 2, 7);   // just ahead of the track layer, inside the bore
		helper.setBlock(wall, Blocks.OBSIDIAN);
		go(cab, 10);
		helper.runAfterDelay(30, () -> {
			helper.assertTrue(machine(helper, c) == null, Component.literal("on the flat it sets back down"));
			DrillBlockEntity now = helper.getBlockEntity(c.offset(3, 3, 3), DrillBlockEntity.class);
			helper.assertTrue(now.status() == DrillCodes.STOP_BLOCKED && now.drill().advanceCount() == 0,
					Component.literal("a solid block in the way should stop it as blocked, got " + now.core().readout()));
			helper.assertTrue(helper.getBlockState(c.offset(0, 0, -1)).getBlock() == Blocks.STONE, Component.literal("uncut"));
			helper.assertTrue(helper.getBlockState(wall).getBlock() == Blocks.OBSIDIAN, Component.literal("the obsidian should not be overwritten"));
			helper.setBlock(wall, Blocks.AIR);
			now.press(10, null);
			helper.succeedWhen(() -> {
				TbmEntity machine = machine(helper, c);
				helper.assertTrue(machine != null && machine.core().drill().advanceCount() >= 1, Component.literal("cleared, it cuts"));
			});
		});
	}

	/** The one-block cab of a world saved before R6 drops its stock and a main beam item, and goes. */
	private static void drillRetired(GameTestHelper helper) {
		BlockPos at = new BlockPos(2, 40, 2);
		helper.setBlock(at, TailingsBlocks.TUNNEL_DRILL.defaultBlockState().setValue(TunnelDrillBlock.FACING, Direction.NORTH));
		helper.getBlockEntity(at, DrillBlockEntity.class).setItem(DrillBlockEntity.SLOT_RAIL, new ItemStack(TailingsBlocks.MINE_RAIL, 5));
		helper.succeedWhen(() -> {
			helper.assertTrue(helper.getBlockState(at).isAir(), Component.literal("the old cab should go"));
			helper.assertItemEntityPresent(TailingsBlocks.MAIN_BEAM.asItem(), at, 3);
			helper.assertItemEntityPresent(TailingsBlocks.MINE_RAIL.asItem(), at, 3);
		});
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
