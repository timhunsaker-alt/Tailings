package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Which item is which rock, at which size. Unbroken feed has a null size. Only ore blocks feed the plant (Silk Touch, the TBM, a blast):
 * raw ore items and an ore's mined drop are the quarter already won, so the plant refuses them (specs-tailings.md X-1).
 */
public final class Feeds {
	public record Charge(Rock rock, @Nullable SizeClass size) {
	}

	private static final Map<String, Rock> COUNTRY = new HashMap<>();

	static {
		for (Rock rock : Rock.values()) {
			if (rock.kind() == Rock.Kind.COUNTRY) {
				COUNTRY.put(rock.id(), rock);
			}
		}
	}

	private Feeds() {
	}

	public static @Nullable Charge parse(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
		if (id == null || "air".equals(id.getPath())) {
			return null;
		}
		String full = id.toString();
		if ("tailings".equals(id.getNamespace())) {
			for (SizeClass size : SizeClass.values()) {
				String prefix = size.path() + "_";
				if (id.getPath().startsWith(prefix)) {
					Rock rock = rock(id.getPath().substring(prefix.length()));
					return rock == null ? null : new Charge(rock, size);
				}
			}
			return null;
		}
		if ("wildcatter".equals(id.getNamespace())) {
			// ore blocks only (Tim, 2026-11-10; specs-tailings.md X-1): an ore's mined drop (wildcatter:galena, what Fortune multiplies) is
			// the quarter already won, so it is no plant feed
			String path = id.getPath();
			if (path.endsWith("_ore")) {
				Rock rock = rock(path.substring(0, path.length() - 4));
				return rock == null || rock.kind() != Rock.Kind.ORE ? null : new Charge(rock, null);
			}
			return null;
		}
		Rock vanilla = vanillaMetal(id.getPath());
		if (vanilla != null) {
			return new Charge(vanilla, null);
		}
		Rock country = COUNTRY.get(id.getPath());
		if (country != null && "minecraft".equals(id.getNamespace())) {
			return new Charge(country, null);
		}
		return null;
	}

	/** The item of a whole, unbroken rock: its block. */
	public static String blockId(Rock rock) {
		return switch (rock.kind()) {
			case ORE -> "wildcatter:" + rock.id() + "_ore";
			case VANILLA -> "minecraft:" + rock.id() + "_ore";
			case COUNTRY -> "minecraft:" + rock.id();
		};
	}

	public static ItemStack stack(String itemId, int count) {
		Identifier id = Identifier.parse(itemId);
		Item item = BuiltInRegistries.ITEM.getValue(id);
		if (item == null || item == Items.AIR) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(item, count);
	}

	/** A vanilla metal's ore block; raw iron, copper and gold are no plant feed (Tim, 2026-11-10; specs-tailings.md X-1). */
	private static @Nullable Rock vanillaMetal(String path) {
		if (path.equals("iron_ore") || path.equals("deepslate_iron_ore")) {
			return Rock.IRON;
		}
		if (path.equals("copper_ore") || path.equals("deepslate_copper_ore")) {
			return Rock.COPPER;
		}
		if (path.equals("gold_ore") || path.equals("deepslate_gold_ore")) {
			return Rock.GOLD;
		}
		return null;
	}

	private static @Nullable Rock rock(String id) {
		for (Rock rock : Rock.values()) {
			if (rock.id().equals(id)) {
				return rock;
			}
		}
		return null;
	}
}
