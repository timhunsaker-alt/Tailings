package dev.wildcatter.tailings;

import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import dev.wildcatter.tailings.mine.TbmGantry;
import java.util.EnumMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

/** Sized rock, nuggets, and the concentrates the 50% machines put out. */
public final class TailingsItems {
	public static final List<Item> SIZED = new ArrayList<>();
	public static final List<Item> NUGGETS = new ArrayList<>();
	public static final List<Item> MINERALS = new ArrayList<>();
	public static final List<Item> CONCENTRATES = new ArrayList<>();
	public static final List<Item> REAGENTS = new ArrayList<>();
	public static Item LOCOMOTIVE;
	public static Item CAR;
	public static Item BATTERY_LOCOMOTIVE;
	public static Item BOTTOM_DUMP_CAR;
	public static Item LOCO_BATTERY;
	public static Item HAUL_TRUCK;
	public static Item WHEEL_LOADER;
	public static Item TRACK_DOZER;
	public static Item SURVEY_BOOK;
	/** Open pit P3 (open-pit-p3-blasting.md). */
	public static Item BLASTHOLE_DRILL;
	public static Item EMULSION_TRUCK;
	public static Item CAST_BOOSTER;
	public static Item DETONATOR;
	public static Item BLAST_LOGGER;
	public static Item BLASTING_KEY;
	public static Item TRAILING_CABLE;
	public static Item ULTRA_HAUL_TRUCK;
	public static Item HYDRAULIC_SHOVEL;
	/** 100 m of lay-flat duct for the TBM's reel gantry, and the gas detector (ventilation.md, L1). */
	public static Item DUCT_CASSETTE;
	public static Item GAS_DETECTOR;
	/** A shaft skip (shaft-and-hoist.md, L4): clicked on a headframe, it hangs on the next free rope. */
	public static Item SKIP;
	/** A 50 kg bag of cement (paste-backfill.md, L5): the paste mixer's binder, a placeholder until a cement kiln exists. */
	public static Item CEMENT;
	/** A 25 kg bag of milled 15 % Si ferrosilicon, the dense medium's make-up (dense-medium-separation.md, L7; a placeholder recipe). */
	public static Item FERROSILICON;
	/** A 100 kg bag of moist heavy mineral concentrate, its assay kept (dry-mill.md, L6). */
	public static Item HEAVY_MINERAL_CONCENTRATE;
	public static final Map<TbmGantry, Item> GANTRIES = new EnumMap<>(TbmGantry.class);

	private TailingsItems() {
	}

	public static void register() {
		for (Rock rock : Rock.values()) {
			for (SizeClass size : SizeClass.values()) {
				SIZED.add(item(size.path() + "_" + rock.id()));
			}
		}
		for (Reagent reagent : Reagent.values()) {
			REAGENTS.add(item(reagent.id()));
		}
		for (String byProduct : java.util.List.of("cadmium", "indium", "rhenium", "cobalt", "rare_earth_oxide")) {
			item(byProduct);
		}
		LOCOMOTIVE = item("mine_locomotive");
		CAR = item("mine_car");
		BATTERY_LOCOMOTIVE = item("battery_locomotive");
		BOTTOM_DUMP_CAR = item("bottom_dump_car");
		ResourceKey<Item> battery = ResourceKey.create(Registries.ITEM, Tailings.id("loco_battery"));
		LOCO_BATTERY = Registry.register(BuiltInRegistries.ITEM, battery, new dev.wildcatter.tailings.item.LocoBatteryItem(new Item.Properties().setId(battery).stacksTo(1)));
		HAUL_TRUCK = vehicle("haul_truck", () -> TailingsEntities.HAUL_TRUCK);
		WHEEL_LOADER = vehicle("wheel_loader", () -> TailingsEntities.WHEEL_LOADER);
		BLASTHOLE_DRILL = vehicle("blasthole_drill", () -> TailingsEntities.BLASTHOLE_DRILL);
		EMULSION_TRUCK = vehicle("emulsion_truck", () -> TailingsEntities.EMULSION_TRUCK);
		CAST_BOOSTER = item("cast_booster");
		DETONATOR = item("detonator");
		BLASTING_KEY = Registry.register(BuiltInRegistries.ITEM, ResourceKey.create(Registries.ITEM, Tailings.id("blasting_key")),
				new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Tailings.id("blasting_key"))).stacksTo(1)));
		ResourceKey<Item> logger = ResourceKey.create(Registries.ITEM, Tailings.id("blast_logger"));
		BLAST_LOGGER = Registry.register(BuiltInRegistries.ITEM, logger, new dev.wildcatter.tailings.item.BlastLoggerItem(new Item.Properties().setId(logger).stacksTo(1)));
		TRACK_DOZER = vehicle("track_dozer", () -> TailingsEntities.TRACK_DOZER);
		ULTRA_HAUL_TRUCK = vehicle("ultra_haul_truck", () -> TailingsEntities.ULTRA_HAUL_TRUCK);
		HYDRAULIC_SHOVEL = vehicle("hydraulic_shovel", () -> TailingsEntities.HYDRAULIC_SHOVEL);
		ResourceKey<Item> cable = ResourceKey.create(Registries.ITEM, Tailings.id("trailing_cable"));
		TRAILING_CABLE = Registry.register(BuiltInRegistries.ITEM, cable, new dev.wildcatter.tailings.item.TrailingCableItem(new Item.Properties().setId(cable).stacksTo(16)));
		ResourceKey<Item> book = ResourceKey.create(Registries.ITEM, Tailings.id("survey_book"));
		SURVEY_BOOK = Registry.register(BuiltInRegistries.ITEM, book, new dev.wildcatter.tailings.item.SurveyBookItem(new Item.Properties().setId(book).stacksTo(1)));
		for (TbmGantry gantry : TbmGantry.values()) {
			GANTRIES.put(gantry, item(gantry.id()));
		}
		ResourceKey<Item> cassette = ResourceKey.create(Registries.ITEM, Tailings.id("vent_duct_cassette"));
		DUCT_CASSETTE = Registry.register(BuiltInRegistries.ITEM, cassette, new Item(new Item.Properties().setId(cassette).stacksTo(16)));
		ResourceKey<Item> detector = ResourceKey.create(Registries.ITEM, Tailings.id("gas_detector"));
		GAS_DETECTOR = Registry.register(BuiltInRegistries.ITEM, detector, new dev.wildcatter.tailings.item.GasDetectorItem(new Item.Properties().setId(detector).stacksTo(1)));
		ResourceKey<Item> skip = ResourceKey.create(Registries.ITEM, Tailings.id("skip"));
		SKIP = Registry.register(BuiltInRegistries.ITEM, skip, new Item(new Item.Properties().setId(skip).stacksTo(1)));
		CEMENT = item("cement");
		FERROSILICON = item("ferrosilicon");
		ResourceKey<Item> hmc = ResourceKey.create(Registries.ITEM, Tailings.id("heavy_mineral_concentrate"));
		HEAVY_MINERAL_CONCENTRATE = Registry.register(BuiltInRegistries.ITEM, hmc, new dev.wildcatter.tailings.item.HeavyMineralConcentrateItem(new Item.Properties().setId(hmc)));
		Set<String> seen = new LinkedHashSet<>();
		for (Mineral mineral : Mineral.values()) {
			if (!mineral.vanillaNugget()) {
				registerOnce(seen, NUGGETS, mineral.nuggetId());
			}
			if (mineral.mineralItemId().startsWith("tailings:")) {
				registerOnce(seen, MINERALS, mineral.mineralItemId());
			}
			if (mineral.separator() != null && mineral.smeltedId() == null) {
				registerOnce(seen, CONCENTRATES, mineral.concentrateId());
			}
		}
	}

	private static void registerOnce(Set<String> seen, List<Item> into, String id) {
		if (seen.add(id)) {
			into.add(item(Identifier.parse(id).getPath()));
		}
	}

	public static Item gantry(TbmGantry gantry) {
		return GANTRIES.get(gantry);
	}

	private static Item vehicle(String path, java.util.function.Supplier<net.minecraft.world.entity.EntityType<? extends dev.wildcatter.tailings.entity.PitVehicleEntity>> type) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Tailings.id(path));
		Item item = new dev.wildcatter.tailings.item.PitVehicleItem(new Item.Properties().setId(key).stacksTo(1), type);
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}

	private static Item item(String path) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Tailings.id(path));
		Item item = new Item(new Item.Properties().setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}
}
