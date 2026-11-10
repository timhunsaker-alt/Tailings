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
		TRACK_DOZER = vehicle("track_dozer", () -> TailingsEntities.TRACK_DOZER);
		ResourceKey<Item> book = ResourceKey.create(Registries.ITEM, Tailings.id("survey_book"));
		SURVEY_BOOK = Registry.register(BuiltInRegistries.ITEM, book, new dev.wildcatter.tailings.item.SurveyBookItem(new Item.Properties().setId(book).stacksTo(1)));
		for (TbmGantry gantry : TbmGantry.values()) {
			GANTRIES.put(gantry, item(gantry.id()));
		}
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
