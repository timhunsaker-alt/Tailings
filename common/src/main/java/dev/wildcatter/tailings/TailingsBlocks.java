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
	public static final Block THICKENER = plant(Machine.THICKENER, MapColor.TERRACOTTA_BROWN);
	public static final Block FILTER = plant(Machine.FILTER, MapColor.TERRACOTTA_WHITE);
	public static final Block TAILINGS_POND = plant(Machine.TAILINGS_POND, MapColor.CLAY);
	public static final Block DUMP_RAMP = plant(Machine.DUMP_RAMP, MapColor.METAL);
	public static final Block DUMP_POCKET = plant(Machine.DUMP_POCKET, MapColor.STONE);
	public static final Block APRON_FEEDER = plant(Machine.APRON_FEEDER, MapColor.COLOR_GRAY);
	public static final Block ORE_SORTER = plant(Machine.ORE_SORTER, MapColor.COLOR_GRAY);
	public static final Block ORE_BIN = plant(Machine.ORE_BIN, MapColor.METAL);
	public static final BlockEntityType<BeltBlockEntity> BELT = new BlockEntityType<>(BeltBlockEntity::new, Set.of(BELT_CONVEYOR));
	public static final BlockEntityType<PlantBlockEntity> PLANT = new BlockEntityType<>(PlantBlockEntity::new,
			Set.of(GRIZZLY, JAW, CONE, SCREEN, CONVEYOR_DRIVE, BALL_MILL, CYCLONE, SPIRAL_BANK, CONDITIONER, FLOTATION_BANK, BLOWER, THICKENER, FILTER, TAILINGS_POND,
					DUMP_RAMP, APRON_FEEDER, ORE_SORTER, ORE_BIN));
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
	public static final BlockEntityType<DumpBlockEntity> DUMP = new BlockEntityType<>(DumpBlockEntity::new, Set.of(DUMP_POCKET));
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
			case FLOTATION_BANK -> FLOTATION_BANK;
			case BLOWER -> BLOWER;
			case SPIRAL_BANK -> SPIRAL_BANK;
			case THICKENER -> THICKENER;
			case FILTER -> FILTER;
			case TAILINGS_POND -> TAILINGS_POND;
			case DUMP_RAMP -> DUMP_RAMP;
			case DUMP_POCKET -> DUMP_POCKET;
			case APRON_FEEDER -> APRON_FEEDER;
			case ORE_SORTER -> ORE_SORTER;
			case ORE_BIN -> ORE_BIN;
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
