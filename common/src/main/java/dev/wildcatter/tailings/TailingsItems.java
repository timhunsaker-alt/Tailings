package dev.wildcatter.tailings;

import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.rock.Mineral;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
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
		LOCOMOTIVE = item("mine_locomotive");
		CAR = item("mine_car");
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

	private static Item item(String path) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Tailings.id(path));
		Item item = new Item(new Item.Properties().setId(key));
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}
}
