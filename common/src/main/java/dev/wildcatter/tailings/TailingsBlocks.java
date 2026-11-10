package dev.wildcatter.tailings;

import dev.wildcatter.tailings.block.GantryPartBlock;
import dev.wildcatter.tailings.block.BlockSignalBlock;
import dev.wildcatter.tailings.block.DewateringPumpBlock;
import dev.wildcatter.tailings.block.DumpBlockEntity;
import dev.wildcatter.tailings.block.BeltBlockEntity;
import dev.wildcatter.tailings.block.BeltConveyorBlock;
import dev.wildcatter.tailings.block.DrillBlockEntity;
import dev.wildcatter.tailings.mine.TbmGantry;
import dev.wildcatter.tailings.mine.TbmPart;
import dev.wildcatter.tailings.block.CableJunctionBlockEntity;
import dev.wildcatter.tailings.block.CableJunctionBlock;
import dev.wildcatter.tailings.block.TbmPartBlockEntity;
import dev.wildcatter.tailings.block.TbmPartBlock;
import dev.wildcatter.tailings.block.HandMillBlock;
import dev.wildcatter.tailings.block.HandMillBlock.Role;
import dev.wildcatter.tailings.block.HandMillBlockEntity;
import dev.wildcatter.tailings.block.LiningBlock;
import dev.wildcatter.tailings.block.MineFullBlock;
import dev.wildcatter.tailings.block.MineLightBlock;
import dev.wildcatter.tailings.block.MineRailBlock;
import dev.wildcatter.tailings.block.MineSwitchBlock;
import dev.wildcatter.tailings.block.MineStairBlock;
import dev.wildcatter.tailings.block.PlantBlockEntity;
import dev.wildcatter.tailings.block.PlantMachineBlock;
import dev.wildcatter.tailings.block.PumpBlockEntity;
import dev.wildcatter.tailings.block.RailBlockEntity;
import dev.wildcatter.tailings.block.RectifierBlockEntity;
import dev.wildcatter.tailings.block.SignalBlockEntity;
import dev.wildcatter.tailings.block.TractionRectifierBlock;
import dev.wildcatter.tailings.block.SlopeBlock;
import dev.wildcatter.tailings.block.TunnelDrillBlock;
import dev.wildcatter.tailings.plant.Machine;
import java.util.Set;
import java.util.function.Function;
import dev.wildcatter.item.BigPartItem;
import dev.wildcatter.multiblock.BigPart;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

/** The starter machines; the true-size crushing circuit, flotation bank and dump station; the mine. */
public final class TailingsBlocks {
	public static final Block STAMP_MILL = mill("stamp_mill", Role.STAMP_MILL, MapColor.STONE);
	public static final Block HAND_SCREEN = mill("hand_screen", Role.HAND_SCREEN, MapColor.WOOD);
	public static final Block SHAKING_TABLE = mill("shaking_table", Role.SHAKING_TABLE, MapColor.METAL);
	public static final Block MAGNETIC_SEPARATOR = mill("magnetic_separator", Role.MAGNETIC_SEPARATOR, MapColor.COLOR_GRAY);
	public static final BlockEntityType<HandMillBlockEntity> MILL = new BlockEntityType<>(HandMillBlockEntity::new,
			Set.of(STAMP_MILL, HAND_SCREEN, SHAKING_TABLE, MAGNETIC_SEPARATOR));
	public static final Block GRIZZLY = plant(Machine.GRIZZLY, MapColor.STONE);
	public static final Block JAW = plant(Machine.JAW, MapColor.COLOR_GRAY);
	public static final Block CONE = plant(Machine.CONE, MapColor.COLOR_GRAY);
	public static final Block SCREEN = plant(Machine.SCREEN, MapColor.METAL);
	public static final Block CONVEYOR_DRIVE = plant(Machine.CONVEYOR_DRIVE, MapColor.COLOR_GRAY);
	public static final Block BELT_CONVEYOR = block("belt_conveyor", props -> new BeltConveyorBlock(props.mapColor(MapColor.COLOR_BLACK).strength(2.0F, 6.0F)
			.sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block BALL_MILL = plant(Machine.BALL_MILL, MapColor.COLOR_GRAY);
	public static final Block CYCLONE = plant(Machine.CYCLONE, MapColor.COLOR_ORANGE);
	public static final Block CONDITIONER = plant(Machine.CONDITIONER, MapColor.COLOR_LIGHT_GRAY);
	public static final Block FLOTATION_BANK = plant(Machine.FLOTATION_BANK, MapColor.COLOR_LIGHT_BLUE);
	public static final Block BLOWER = plant(Machine.BLOWER, MapColor.COLOR_BLUE);
	public static final Block SPIRAL_BANK = plant(Machine.SPIRAL_BANK, MapColor.METAL);
	public static final Block BYPRODUCT_PLANT = plant(Machine.BYPRODUCT_PLANT, MapColor.COLOR_GRAY);
	public static final Block HEAP_PAD = plant(Machine.HEAP_PAD, MapColor.TERRACOTTA_BROWN);
	public static final Block ACTIVATION_TANK = plant(Machine.ACTIVATION_TANK, MapColor.COLOR_LIGHT_GRAY);
	public static final Block SCAVENGER_BANK = plant(Machine.SCAVENGER_BANK, MapColor.COLOR_GRAY);
	public static final Block REGRIND_MILL = plant(Machine.REGRIND_MILL, MapColor.METAL);
	public static final Block CLEANER_BANK = plant(Machine.CLEANER_BANK, MapColor.COLOR_GRAY);
	public static final Block LEACH_TANK = plant(Machine.LEACH_TANK, MapColor.COLOR_GRAY);
	public static final Block CCD_THICKENER = plant(Machine.CCD_THICKENER, MapColor.STONE);
	public static final Block ELECTROWINNING_BANK = plant(Machine.ELECTROWINNING_BANK, MapColor.COLOR_ORANGE);
	public static final Block THICKENER = plant(Machine.THICKENER, MapColor.TERRACOTTA_BROWN);
	public static final Block FILTER = plant(Machine.FILTER, MapColor.TERRACOTTA_WHITE);
	public static final Block TAILINGS_POND = plant(Machine.TAILINGS_POND, MapColor.CLAY);
	public static final Block DUMP_RAMP = plant(Machine.DUMP_RAMP, MapColor.METAL);
	public static final Block DUMP_POCKET = plant(Machine.DUMP_POCKET, MapColor.STONE);
	public static final Block APRON_FEEDER = plant(Machine.APRON_FEEDER, MapColor.COLOR_GRAY);
	public static final Block ORE_SORTER = plant(Machine.ORE_SORTER, MapColor.COLOR_GRAY);
	public static final Block ORE_BIN = plant(Machine.ORE_BIN, MapColor.METAL);
	public static final Block TRUCK_DUMP_WALL = plant(Machine.TRUCK_DUMP_WALL, MapColor.STONE);
	public static final Block PIT_HOPPER = plant(Machine.PIT_HOPPER, MapColor.METAL);
	public static final Block GYRATORY_CRUSHER = plant(Machine.GYRATORY_CRUSHER, MapColor.COLOR_GRAY);
	public static final Block GYRATORY_POCKET = plant(Machine.GYRATORY_POCKET, MapColor.STONE);
	public static final Block ROCK_BREAKER = plant(Machine.ROCK_BREAKER, MapColor.COLOR_YELLOW);
	public static final Block SURGE_POCKET = plant(Machine.SURGE_POCKET, MapColor.METAL);
	public static final Block PRIMARY_APRON_FEEDER = plant(Machine.PRIMARY_APRON_FEEDER, MapColor.COLOR_GRAY);
	public static final Block HEAVY_CONVEYOR_DRIVE = plant(Machine.HEAVY_CONVEYOR_DRIVE, MapColor.COLOR_GRAY);
	public static final Block STOCKPILE_HEAD = plant(Machine.STOCKPILE_HEAD, MapColor.METAL);
	public static final Block RECLAIM_FEEDER = plant(Machine.RECLAIM_FEEDER, MapColor.COLOR_GRAY);
	public static final Block BELT_WEIGHER = plant(Machine.BELT_WEIGHER, MapColor.METAL);
	public static final Block SAG_MILL = plant(Machine.SAG_MILL, MapColor.COLOR_GRAY);
	public static final Block PEBBLE_CRUSHER = plant(Machine.PEBBLE_CRUSHER, MapColor.COLOR_GRAY);
	public static final Block BALL_MILL_LARGE = plant(Machine.BALL_MILL_LARGE, MapColor.COLOR_GRAY);
	public static final Block CYCLONE_FEED_PUMP = plant(Machine.CYCLONE_FEED_PUMP, MapColor.COLOR_BLUE);
	public static final Block CYCLONE_CLUSTER_LARGE = plant(Machine.CYCLONE_CLUSTER_LARGE, MapColor.COLOR_ORANGE);
	public static final Block FUEL_BAY = plant(Machine.FUEL_BAY, MapColor.COLOR_YELLOW);
	public static final Block HAUL_DISPATCH = plant(Machine.HAUL_DISPATCH, MapColor.TERRACOTTA_WHITE);
	public static final Block MATRIX_FILL_POINT = plant(Machine.MATRIX_FILL_POINT, MapColor.COLOR_ORANGE);
	public static final Block TAILINGS_THICKENER = plant(Machine.TAILINGS_THICKENER, MapColor.STONE);
	public static final Block TAILINGS_FILTER = plant(Machine.TAILINGS_FILTER, MapColor.TERRACOTTA_WHITE);
	public static final Block LOADOUT_FRAME = plant(Machine.LOADOUT_FRAME, MapColor.COLOR_GRAY);
	public static final Block LOADOUT_BIN = plant(Machine.LOADOUT_BIN, MapColor.METAL);
	public static final Block PIT_SWITCH_HOUSE = plant(Machine.PIT_SWITCH_HOUSE, MapColor.COLOR_GRAY);
	public static final Block SHOVEL_CRAWLERS = plant(Machine.SHOVEL_CRAWLERS, MapColor.COLOR_GRAY);
	public static final Block SHOVEL_HOUSE = plant(Machine.SHOVEL_HOUSE, MapColor.COLOR_YELLOW);
	public static final Block SHOVEL_GANTRY = plant(Machine.SHOVEL_GANTRY, MapColor.COLOR_YELLOW);
	public static final Block SHOVEL_BOOM = plant(Machine.SHOVEL_BOOM, MapColor.COLOR_YELLOW);
	public static final Block SHOVEL_HANDLE = plant(Machine.SHOVEL_HANDLE, MapColor.COLOR_YELLOW);
	public static final Block SHOVEL_DIPPER = plant(Machine.SHOVEL_DIPPER, MapColor.METAL);
	public static final Block TRACK_GRIZZLY = plant(Machine.TRACK_GRIZZLY, MapColor.METAL);
	public static final Block DUMP_CAM_RAIL = plant(Machine.DUMP_CAM_RAIL, MapColor.METAL);
	public static final Block BATTERY_CHARGER = plant(Machine.BATTERY_CHARGER, MapColor.COLOR_GRAY);
	public static final Block VENT_FAN = plant(Machine.VENT_FAN, MapColor.COLOR_YELLOW);
	public static final Block BOOSTER_FAN = plant(Machine.BOOSTER_FAN, MapColor.COLOR_YELLOW);
	/** Lay-flat ventilation duct (ventilation.md, L1): a block a metre, laid like pipe, joining fans and the TBM's cassette. */
	public static final Block VENT_DUCT = block("vent_duct", props -> new dev.wildcatter.tailings.block.VentDuctBlock(props.mapColor(MapColor.COLOR_YELLOW)
			.strength(0.8F, 1.0F).sound(SoundType.WOOL).noOcclusion()));
	public static final Block HEADFRAME = plant(Machine.HEADFRAME, MapColor.COLOR_GRAY);
	public static final Block HEADFRAME_BIN = plant(Machine.HEADFRAME_BIN, MapColor.METAL);
	public static final Block HOIST_HOUSE = plant(Machine.HOIST_HOUSE, MapColor.COLOR_GRAY);
	public static final Block MEASURING_FLASK = plant(Machine.MEASURING_FLASK, MapColor.METAL);
	public static final Block PASTE_FILTER = plant(Machine.PASTE_FILTER, MapColor.COLOR_GRAY);
	public static final Block PASTE_MIXER = plant(Machine.PASTE_MIXER, MapColor.COLOR_GRAY);
	public static final Block PASTE_PUMP = plant(Machine.PASTE_PUMP, MapColor.COLOR_YELLOW);
	/** A shotcrete fill bulkhead (paste-backfill.md, L5): a metre of the barricade that closes a void's opening for paste fill. */
	public static final Block FILL_BULKHEAD = block("fill_bulkhead", props -> new dev.wildcatter.tailings.block.FillBulkheadBlock(props.mapColor(MapColor.STONE)
			.strength(2.0F, 6.0F).sound(SoundType.STONE).requiresCorrectToolForDrops()));
	/** The open end of the paste fill line inside the void (L5). */
	public static final Block FILL_OUTLET = block("fill_outlet", props -> new dev.wildcatter.tailings.block.FillOutletBlock(props.mapColor(MapColor.METAL)
			.strength(3.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
	public static final BlockEntityType<dev.wildcatter.tailings.block.FillOutletBlockEntity> FILL_OUTLET_ENTITY = new BlockEntityType<>(dev.wildcatter.tailings.block.FillOutletBlockEntity::new, Set.of(FILL_OUTLET));
	/** Placed paste fill (L5): wet when placed, set after a game day. A cubic metre of tails and cement: it drops nothing. */
	public static final Block PASTE_FILL = block("paste_fill", props -> new dev.wildcatter.tailings.block.PasteFillBlock(props.mapColor(MapColor.TERRACOTTA_LIGHT_GRAY)
			.strength(1.0F, 3.0F).sound(SoundType.MUD).noLootTable().isSuffocating((state, getter, pos) -> false)));
	public static final Block SCAVENGER_SPIRAL_BANK = plant(Machine.SCAVENGER_SPIRAL_BANK, MapColor.METAL);
	public static final Block FLUID_BED_DRYER = plant(Machine.FLUID_BED_DRYER, MapColor.COLOR_GRAY);
	public static final Block RER_SEPARATOR = plant(Machine.RER_SEPARATOR, MapColor.COLOR_BLUE);
	public static final Block HT_ROLL_SEPARATOR = plant(Machine.HT_ROLL_SEPARATOR, MapColor.COLOR_LIGHT_GRAY);
	public static final Block IRM_SEPARATOR = plant(Machine.IRM_SEPARATOR, MapColor.COLOR_BLUE);
	public static final Block DESLIME_SCREEN = plant(Machine.DESLIME_SCREEN, MapColor.METAL);
	public static final Block DMS_MODULE = plant(Machine.DMS_MODULE, MapColor.COLOR_GRAY);
	public static final Block DRAIN_RINSE_SCREEN = plant(Machine.DRAIN_RINSE_SCREEN, MapColor.METAL);
	public static final Block MEDIUM_RECOVERY = plant(Machine.MEDIUM_RECOVERY, MapColor.COLOR_BLUE);
	public static final Block XRT_SORTER = plant(Machine.XRT_SORTER, MapColor.COLOR_YELLOW);
	public static final Block SORTER_COMPRESSOR = plant(Machine.SORTER_COMPRESSOR, MapColor.COLOR_BLUE);
	/** A skip's looks (shaft-and-hoist.md, L4), drawn by its entity: the block itself has no item and is never placed. */
	public static final Block SKIP = block("skip", props -> new dev.wildcatter.tailings.block.SkipPartBlock(props.mapColor(MapColor.COLOR_GRAY).strength(3.5F, 8.0F)
			.sound(SoundType.METAL).noOcclusion().noLootTable()));
	public static final BlockEntityType<dev.wildcatter.tailings.block.BatteryChargerBlockEntity> BATTERY_CHARGER_ENTITY = new BlockEntityType<>(dev.wildcatter.tailings.block.BatteryChargerBlockEntity::new, Set.of(BATTERY_CHARGER));
	/** The tailings filter's cake as a placed block (open-pit-p4-dry-stack.md): 1 m³ at 1.8 t, falling like sand. */
	public static final Block DRY_TAILS = block("dry_tails", props -> new net.minecraft.world.level.block.ColoredFallingBlock(new net.minecraft.util.ColorRGBA(0x9a8f7f),
			props.mapColor(MapColor.TERRACOTTA_LIGHT_GRAY).strength(0.6F).sound(SoundType.SAND)));
	/** A haul road's survey stake (open-pit.md, P2a): no collision, so trucks drive over it. */
	public static final Block SURVEY_STAKE = block("survey_stake", props -> new dev.wildcatter.tailings.block.SurveyStakeBlock(props.mapColor(MapColor.COLOR_ORANGE)
			.strength(0.5F, 1.0F).sound(SoundType.WOOD).noCollision().noOcclusion()));
	public static final BlockEntityType<dev.wildcatter.tailings.block.FuelBayBlockEntity> FUEL_BAY_ENTITY = new BlockEntityType<>(dev.wildcatter.tailings.block.FuelBayBlockEntity::new, Set.of(FUEL_BAY));
	public static final BlockEntityType<dev.wildcatter.tailings.block.HaulDispatchBlockEntity> HAUL_DISPATCH_ENTITY = new BlockEntityType<>(dev.wildcatter.tailings.block.HaulDispatchBlockEntity::new, Set.of(HAUL_DISPATCH));
	public static final BlockEntityType<dev.wildcatter.tailings.block.RopeShovelBlockEntity> ROPE_SHOVEL_ENTITY = new BlockEntityType<>(dev.wildcatter.tailings.block.RopeShovelBlockEntity::new, Set.of(SHOVEL_CRAWLERS));
	public static final BlockEntityType<dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity> PIT_SWITCH_HOUSE_ENTITY = new BlockEntityType<>(dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity::new, Set.of(PIT_SWITCH_HOUSE));
	public static final BlockEntityType<dev.wildcatter.tailings.block.MatrixFillPointBlockEntity> MATRIX_FILL_POINT_ENTITY = new BlockEntityType<>(dev.wildcatter.tailings.block.MatrixFillPointBlockEntity::new, Set.of(MATRIX_FILL_POINT));
	/** A blasthole (open-pit-p3-blasting.md): stone-hard, each block remembering the rock it replaced; it drops that rock, not itself. */
	public static final Block BLASTHOLE = block("blasthole", props -> new dev.wildcatter.tailings.block.BlastholeBlock(props.mapColor(MapColor.STONE).strength(1.5F, 6.0F)
			.sound(SoundType.STONE).noLootTable()));
	public static final BlockEntityType<dev.wildcatter.tailings.block.BlastholeBlockEntity> BLASTHOLE_ENTITY = new BlockEntityType<>(dev.wildcatter.tailings.block.BlastholeBlockEntity::new, Set.of(BLASTHOLE));
	/** The blasting machine (open-pit-p3-blasting.md, Tying in and firing): a box with a key switch, ARM and FIRE, on the lead line. */
	public static final Block BLASTING_MACHINE = block("blasting_machine", props -> new dev.wildcatter.tailings.block.BlastingMachineBlock(props.mapColor(MapColor.COLOR_RED)
			.strength(2.0F, 6.0F).sound(SoundType.METAL).requiresCorrectToolForDrops().noOcclusion()));
	public static final BlockEntityType<dev.wildcatter.tailings.block.BlastingMachineBlockEntity> BLASTING_MACHINE_ENTITY = new BlockEntityType<>(dev.wildcatter.tailings.block.BlastingMachineBlockEntity::new, Set.of(BLASTING_MACHINE));
	/** Broken rock (open-pit.md): falls and settles, holding what it was. Its drops are its contents, not a loot table. */
	public static final Block MUCK = block("muck", props -> new dev.wildcatter.tailings.block.MuckBlock(props.mapColor(MapColor.STONE).strength(0.8F, 1.0F)
			.sound(SoundType.GRAVEL).noLootTable()));
	/** The coarse-ore stockpile's crushed rock (primary-gyratory-stockpile.md, P7a): muck that also rolls in over a reclaim feeder. */
	public static final Block STOCKPILE_ROCK = block("stockpile_rock", props -> new dev.wildcatter.tailings.block.StockpileRockBlock(props.mapColor(MapColor.STONE).strength(0.8F, 1.0F)
			.sound(SoundType.GRAVEL).noLootTable()));
	public static final BlockEntityType<dev.wildcatter.tailings.block.MuckBlockEntity> MUCK_ENTITY = new BlockEntityType<>(dev.wildcatter.tailings.block.MuckBlockEntity::new, Set.of(MUCK, STOCKPILE_ROCK));
	/** The pit line's 1,400 mm belt (P7a), laid as the 600 mm belt is; a run is all one kind. */
	public static final Block HEAVY_BELT_CONVEYOR = block("heavy_belt_conveyor", props -> new dev.wildcatter.tailings.block.HeavyBeltBlock(props.mapColor(MapColor.COLOR_BLACK).strength(2.5F, 6.0F)
			.sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final BlockEntityType<BeltBlockEntity> BELT = new BlockEntityType<>(BeltBlockEntity::new, Set.of(BELT_CONVEYOR, HEAVY_BELT_CONVEYOR));
	public static final BlockEntityType<PlantBlockEntity> PLANT = new BlockEntityType<>(PlantBlockEntity::new,
			Set.of(ACTIVATION_TANK, GRIZZLY, JAW, CONE, SCREEN, CONVEYOR_DRIVE, BALL_MILL, CYCLONE, SPIRAL_BANK, BYPRODUCT_PLANT, HEAP_PAD, SCAVENGER_BANK, REGRIND_MILL, CLEANER_BANK, LEACH_TANK, CCD_THICKENER, ELECTROWINNING_BANK, CONDITIONER, FLOTATION_BANK, BLOWER, THICKENER, FILTER, TAILINGS_POND,
					DUMP_RAMP, APRON_FEEDER, ORE_SORTER, ORE_BIN, TRUCK_DUMP_WALL, TAILINGS_THICKENER, TAILINGS_FILTER, LOADOUT_FRAME, LOADOUT_BIN,
					SHOVEL_HOUSE, SHOVEL_GANTRY, SHOVEL_BOOM, SHOVEL_HANDLE, SHOVEL_DIPPER,
					TRACK_GRIZZLY, DUMP_CAM_RAIL, VENT_FAN, BOOSTER_FAN, HEADFRAME, HEADFRAME_BIN, HOIST_HOUSE, MEASURING_FLASK,
					PASTE_FILTER, PASTE_MIXER, PASTE_PUMP, SCAVENGER_SPIRAL_BANK, FLUID_BED_DRYER, RER_SEPARATOR, HT_ROLL_SEPARATOR, IRM_SEPARATOR,
					DESLIME_SCREEN, DMS_MODULE, DRAIN_RINSE_SCREEN, MEDIUM_RECOVERY, XRT_SORTER, SORTER_COMPRESSOR,
					GYRATORY_CRUSHER, ROCK_BREAKER, SURGE_POCKET, PRIMARY_APRON_FEEDER, HEAVY_CONVEYOR_DRIVE, STOCKPILE_HEAD, RECLAIM_FEEDER, BELT_WEIGHER,
					SAG_MILL, PEBBLE_CRUSHER, BALL_MILL_LARGE, CYCLONE_FEED_PUMP, CYCLONE_CLUSTER_LARGE));
	public static final Block LINING = course("lining", props -> new LiningBlock(props.mapColor(MapColor.COLOR_GRAY).strength(3.0F, 9.0F).sound(SoundType.STONE)));
	public static final Block SLOPE = course("slope", props -> new SlopeBlock(props.mapColor(MapColor.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE)
			.noOcclusion().isSuffocating((state, getter, pos) -> false)));
	public static final Block MINE_STAIR = course("mine_stair", props -> new MineStairBlock(props.mapColor(MapColor.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE)
			.noOcclusion().isSuffocating((state, getter, pos) -> false)));
	public static final Block MINE_LANDING = course("mine_landing", props -> new MineFullBlock(props.mapColor(MapColor.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE)));
	public static final Block MINE_FLOOR = course("mine_floor", props -> new MineFullBlock(props.mapColor(MapColor.STONE).strength(2.0F, 6.0F).sound(SoundType.STONE)));
	public static final Block MINE_RAIL = course("mine_rail", props -> new MineRailBlock(props.mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL)
			.noOcclusion().isSuffocating((state, getter, pos) -> false)));
	public static final Block TUNNEL_DRILL = block("tunnel_drill", props -> new TunnelDrillBlock(props.mapColor(MapColor.COLOR_ORANGE).strength(3.5F, 8.0F)
			.sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block DEWATERING_PUMP = block("dewatering_pump", props -> new DewateringPumpBlock(props.mapColor(MapColor.COLOR_LIGHT_BLUE)
			.strength(3.5F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final Block CUTTERHEAD = tbm(TbmPart.CUTTERHEAD, MapColor.COLOR_YELLOW);
	public static final Block SEGMENT_ERECTOR = tbm(TbmPart.SEGMENT_ERECTOR, MapColor.COLOR_YELLOW);
	public static final Block MAIN_BEAM = tbm(TbmPart.MAIN_BEAM, MapColor.COLOR_YELLOW);
	public static final Block TRACK_LAYER = tbm(TbmPart.TRACK_LAYER, MapColor.COLOR_GRAY);
	public static final Block PUMP_GANTRY = gantry(TbmGantry.PUMP);
	public static final Block REEL_GANTRY = gantry(TbmGantry.REEL);
	public static final Block CONVEYOR_GANTRY = gantry(TbmGantry.CONVEYOR);
	public static final Block CABLE_JUNCTION = block("cable_junction", props -> new CableJunctionBlock(props.mapColor(MapColor.COLOR_GRAY).strength(3.0F, 6.0F)
			.sound(SoundType.METAL).requiresCorrectToolForDrops()));
	/** The cab: at the main beam's tail; the one-block cab of a world saved before R6 too, which retires itself. */
	public static final BlockEntityType<DrillBlockEntity> DRILL = new BlockEntityType<>(DrillBlockEntity::new, Set.of(TUNNEL_DRILL, MAIN_BEAM));
	public static final BlockEntityType<TbmPartBlockEntity> TBM_PART = new BlockEntityType<>(TbmPartBlockEntity::new, Set.of(CUTTERHEAD, SEGMENT_ERECTOR, TRACK_LAYER));
	public static final BlockEntityType<CableJunctionBlockEntity> CABLE_JUNCTION_ENTITY = new BlockEntityType<>(CableJunctionBlockEntity::new, Set.of(CABLE_JUNCTION));
	public static final BlockEntityType<PumpBlockEntity> PUMP = new BlockEntityType<>(PumpBlockEntity::new, Set.of(DEWATERING_PUMP));
	public static final Block MINE_SWITCH = course("mine_switch", props -> new MineSwitchBlock(props.mapColor(MapColor.METAL).strength(2.0F, 6.0F).sound(SoundType.METAL)
			.noOcclusion().isSuffocating((state, getter, pos) -> false)));
	public static final Block MINE_LIGHT = block("mine_light", props -> new MineLightBlock(props.mapColor(MapColor.COLOR_YELLOW).strength(1.5F, 3.0F)
			.sound(SoundType.GLASS).lightLevel(state -> state.getValue(MineLightBlock.LIT) ? 15 : 0)));
	public static final BlockEntityType<RailBlockEntity> RAIL = new BlockEntityType<>(RailBlockEntity::new, Set.of(MINE_RAIL, MINE_SWITCH));
	public static final Block BLOCK_SIGNAL = block("block_signal", props -> new BlockSignalBlock(props.mapColor(MapColor.COLOR_RED).strength(2.0F, 6.0F)
			.sound(SoundType.METAL).requiresCorrectToolForDrops().lightLevel(state -> state.getValue(BlockSignalBlock.LIT) ? 12 : 0)));
	public static final Block TRACTION_RECTIFIER = block("traction_rectifier", props -> new TractionRectifierBlock(props.mapColor(MapColor.COLOR_YELLOW)
			.strength(3.5F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));
	public static final BlockEntityType<DumpBlockEntity> DUMP = new BlockEntityType<>(DumpBlockEntity::new, Set.of(DUMP_POCKET, PIT_HOPPER, GYRATORY_POCKET));
	public static final BlockEntityType<SignalBlockEntity> SIGNAL = new BlockEntityType<>(SignalBlockEntity::new, Set.of(BLOCK_SIGNAL));
	public static final BlockEntityType<RectifierBlockEntity> RECTIFIER = new BlockEntityType<>(RectifierBlockEntity::new, Set.of(TRACTION_RECTIFIER));

	private TailingsBlocks() {
	}

	public static void register() {
		add("stamp_mill", STAMP_MILL);
		add("hand_screen", HAND_SCREEN);
		add("shaking_table", SHAKING_TABLE);
		add("magnetic_separator", MAGNETIC_SEPARATOR);
		for (Machine machine : Machine.values()) {
			add(machine.id(), plantBlock(machine));
		}
		add("lining", LINING);
		add("slope", SLOPE);
		add("mine_stair", MINE_STAIR);
		add("mine_landing", MINE_LANDING);
		add("mine_floor", MINE_FLOOR);
		add("mine_rail", MINE_RAIL);
		add("mine_switch", MINE_SWITCH);
		add("mine_light", MINE_LIGHT);
		add("tunnel_drill", TUNNEL_DRILL);
		add("dewatering_pump", DEWATERING_PUMP);
		for (TbmPart part : TbmPart.values()) {
			add(part.id(), tbmBlock(part));
		}
		for (TbmGantry gantry : TbmGantry.values()) {
			Registry.register(BuiltInRegistries.BLOCK, Tailings.id(gantry.id()), gantryBlock(gantry));
		}
		add("cable_junction", CABLE_JUNCTION);
		Registry.register(BuiltInRegistries.BLOCK, Tailings.id("skip"), SKIP);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("tbm_part"), TBM_PART);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("cable_junction"), CABLE_JUNCTION_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("hand_mill"), MILL);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("plant"), PLANT);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("tunnel_drill"), DRILL);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("dewatering_pump"), PUMP);
		add("belt_conveyor", BELT_CONVEYOR);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("belt_conveyor"), BELT);
		legacy("flotation_cell");
		legacy("dump_station");
		add("block_signal", BLOCK_SIGNAL);
		add("traction_rectifier", TRACTION_RECTIFIER);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("mine_rail"), RAIL);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("dump_station"), DUMP);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("block_signal"), SIGNAL);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("traction_rectifier"), RECTIFIER);
		add("muck", MUCK);
		add("stockpile_rock", STOCKPILE_ROCK);
		add("heavy_belt_conveyor", HEAVY_BELT_CONVEYOR);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("muck"), MUCK_ENTITY);
		add("survey_stake", SURVEY_STAKE);
		add("dry_tails", DRY_TAILS);
		add("vent_duct", VENT_DUCT);
		add("fill_bulkhead", FILL_BULKHEAD);
		add("fill_outlet", FILL_OUTLET);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("fill_outlet"), FILL_OUTLET_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK, Tailings.id("paste_fill"), PASTE_FILL);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("fuel_bay"), FUEL_BAY_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("haul_dispatch"), HAUL_DISPATCH_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("matrix_fill_point"), MATRIX_FILL_POINT_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("pit_switch_house"), PIT_SWITCH_HOUSE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("shovel_crawlers"), ROPE_SHOVEL_ENTITY);
		add("blasthole", BLASTHOLE);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("blasthole"), BLASTHOLE_ENTITY);
		add("blasting_machine", BLASTING_MACHINE);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("blasting_machine"), BLASTING_MACHINE_ENTITY);
		Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Tailings.id("battery_charger"), BATTERY_CHARGER_ENTITY);
	}

	public static Block plantBlock(Machine machine) {
		return switch (machine) {
			case GRIZZLY -> GRIZZLY;
			case JAW -> JAW;
			case CONE -> CONE;
			case SCREEN -> SCREEN;
			case CONVEYOR_DRIVE -> CONVEYOR_DRIVE;
			case BALL_MILL -> BALL_MILL;
			case CYCLONE -> CYCLONE;
			case CONDITIONER -> CONDITIONER;
			case ACTIVATION_TANK -> ACTIVATION_TANK;
			case FLOTATION_BANK -> FLOTATION_BANK;
			case BLOWER -> BLOWER;
			case SPIRAL_BANK -> SPIRAL_BANK;
			case BYPRODUCT_PLANT -> BYPRODUCT_PLANT;
			case HEAP_PAD -> HEAP_PAD;
			case SCAVENGER_BANK -> SCAVENGER_BANK;
			case REGRIND_MILL -> REGRIND_MILL;
			case CLEANER_BANK -> CLEANER_BANK;
			case LEACH_TANK -> LEACH_TANK;
			case CCD_THICKENER -> CCD_THICKENER;
			case ELECTROWINNING_BANK -> ELECTROWINNING_BANK;
			case THICKENER -> THICKENER;
			case FILTER -> FILTER;
			case TAILINGS_POND -> TAILINGS_POND;
			case DUMP_RAMP -> DUMP_RAMP;
			case DUMP_POCKET -> DUMP_POCKET;
			case APRON_FEEDER -> APRON_FEEDER;
			case ORE_SORTER -> ORE_SORTER;
			case ORE_BIN -> ORE_BIN;
			case TRUCK_DUMP_WALL -> TRUCK_DUMP_WALL;
			case PIT_HOPPER -> PIT_HOPPER;
			case FUEL_BAY -> FUEL_BAY;
			case HAUL_DISPATCH -> HAUL_DISPATCH;
			case MATRIX_FILL_POINT -> MATRIX_FILL_POINT;
			case TAILINGS_THICKENER -> TAILINGS_THICKENER;
			case TAILINGS_FILTER -> TAILINGS_FILTER;
			case LOADOUT_FRAME -> LOADOUT_FRAME;
			case LOADOUT_BIN -> LOADOUT_BIN;
			case PIT_SWITCH_HOUSE -> PIT_SWITCH_HOUSE;
			case SHOVEL_CRAWLERS -> SHOVEL_CRAWLERS;
			case SHOVEL_HOUSE -> SHOVEL_HOUSE;
			case SHOVEL_GANTRY -> SHOVEL_GANTRY;
			case SHOVEL_BOOM -> SHOVEL_BOOM;
			case SHOVEL_HANDLE -> SHOVEL_HANDLE;
			case SHOVEL_DIPPER -> SHOVEL_DIPPER;
			case TRACK_GRIZZLY -> TRACK_GRIZZLY;
			case DUMP_CAM_RAIL -> DUMP_CAM_RAIL;
			case BATTERY_CHARGER -> BATTERY_CHARGER;
			case VENT_FAN -> VENT_FAN;
			case BOOSTER_FAN -> BOOSTER_FAN;
			case HEADFRAME -> HEADFRAME;
			case HEADFRAME_BIN -> HEADFRAME_BIN;
			case HOIST_HOUSE -> HOIST_HOUSE;
			case MEASURING_FLASK -> MEASURING_FLASK;
			case PASTE_FILTER -> PASTE_FILTER;
			case PASTE_MIXER -> PASTE_MIXER;
			case PASTE_PUMP -> PASTE_PUMP;
			case SCAVENGER_SPIRAL_BANK -> SCAVENGER_SPIRAL_BANK;
			case FLUID_BED_DRYER -> FLUID_BED_DRYER;
			case RER_SEPARATOR -> RER_SEPARATOR;
			case HT_ROLL_SEPARATOR -> HT_ROLL_SEPARATOR;
			case IRM_SEPARATOR -> IRM_SEPARATOR;
			case DESLIME_SCREEN -> DESLIME_SCREEN;
			case DMS_MODULE -> DMS_MODULE;
			case DRAIN_RINSE_SCREEN -> DRAIN_RINSE_SCREEN;
			case MEDIUM_RECOVERY -> MEDIUM_RECOVERY;
			case XRT_SORTER -> XRT_SORTER;
			case SORTER_COMPRESSOR -> SORTER_COMPRESSOR;
			case GYRATORY_CRUSHER -> GYRATORY_CRUSHER;
			case GYRATORY_POCKET -> GYRATORY_POCKET;
			case ROCK_BREAKER -> ROCK_BREAKER;
			case SURGE_POCKET -> SURGE_POCKET;
			case PRIMARY_APRON_FEEDER -> PRIMARY_APRON_FEEDER;
			case HEAVY_CONVEYOR_DRIVE -> HEAVY_CONVEYOR_DRIVE;
			case STOCKPILE_HEAD -> STOCKPILE_HEAD;
			case RECLAIM_FEEDER -> RECLAIM_FEEDER;
			case BELT_WEIGHER -> BELT_WEIGHER;
			case SAG_MILL -> SAG_MILL;
			case PEBBLE_CRUSHER -> PEBBLE_CRUSHER;
			case BALL_MILL_LARGE -> BALL_MILL_LARGE;
			case CYCLONE_FEED_PUMP -> CYCLONE_FEED_PUMP;
			case CYCLONE_CLUSTER_LARGE -> CYCLONE_CLUSTER_LARGE;
		};
	}

	private static Block mill(String name, Role role, MapColor color) {
		return block(name, props -> new HandMillBlock(props.mapColor(color).strength(2.5F, 6.0F).sound(SoundType.METAL)
				.requiresCorrectToolForDrops(), role));
	}

	private static Block plant(Machine machine, MapColor color) {
		return block(machine.id(), props -> new PlantMachineBlock(props.mapColor(color).strength(3.5F, 8.0F).sound(SoundType.METAL)
				.requiresCorrectToolForDrops(), machine));
	}

	private static Block course(String name, Function<BlockBehaviour.Properties, Block> factory) {
		return block(name, props -> factory.apply(props.requiresCorrectToolForDrops()));
	}

	private static Block tbm(TbmPart part, MapColor color) {
		return block(part.id(), props -> new TbmPartBlock(props.mapColor(color).strength(3.5F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
				.noOcclusion(), part));
	}

	/** A gantry's looks, drawn by its entity: the block itself has no item and is never placed. */
	private static Block gantry(TbmGantry gantry) {
		return block(gantry.id(), props -> new GantryPartBlock(props.mapColor(MapColor.COLOR_GRAY).strength(3.5F, 8.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()
				.noOcclusion().noLootTable(), gantry));
	}

	public static Block gantryBlock(TbmGantry gantry) {
		return switch (gantry) {
			case PUMP -> PUMP_GANTRY;
			case REEL -> REEL_GANTRY;
			case CONVEYOR -> CONVEYOR_GANTRY;
		};
	}

	public static Block tbmBlock(TbmPart part) {
		return switch (part) {
			case CUTTERHEAD -> CUTTERHEAD;
			case SEGMENT_ERECTOR -> SEGMENT_ERECTOR;
			case MAIN_BEAM -> MAIN_BEAM;
			case TRACK_LAYER -> TRACK_LAYER;
		};
	}

	private static Block block(String name, Function<BlockBehaviour.Properties, Block> factory) {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, Tailings.id(name));
		return factory.apply(BlockBehaviour.Properties.of().setId(key));
	}

	/** A true-size part's item is Wildcatter's {@link BigPartItem}, which also gets the ghost outline. */
	private static void add(String name, Block block) {
		Registry.register(BuiltInRegistries.BLOCK, Tailings.id(name), block);
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Tailings.id(name));
		Item.Properties properties = new Item.Properties().setId(itemKey).useBlockDescriptionPrefix();
		Registry.register(BuiltInRegistries.ITEM, itemKey, block instanceof BigPart ? new BigPartItem(block, properties) : new BlockItem(block, properties));
	}

	/**
	 * An item whose block went in R3 (the eight-cell bank and the one-block dump station replaced it), kept for one release so an
	 * old inventory keeps it: it crafts back into the iron it was made of.
	 */
	private static void legacy(String name) {
		ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, Tailings.id(name));
		Registry.register(BuiltInRegistries.ITEM, itemKey, new Item(new Item.Properties().setId(itemKey)));
	}
}
