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

/** Which item is which rock, at which size. Unbroken feed has a null size. */
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
			String path = id.getPath();
			if (path.endsWith("_ore")) {
				Rock rock = rock(path.substring(0, path.length() - 4));
				return rock == null || rock.kind() != Rock.Kind.ORE ? null : new Charge(rock, null);
			}
			Rock rock = rock(path);
			return rock == null || rock.kind() != Rock.Kind.ORE ? null : new Charge(rock, SizeClass.SAND);
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

	public static ItemStack stack(String itemId, int count) {
		Identifier id = Identifier.parse(itemId);
		Item item = BuiltInRegistries.ITEM.getValue(id);
		if (item == null || item == Items.AIR) {
			return ItemStack.EMPTY;
		}
		return new ItemStack(item, count);
	}

	private static @Nullable Rock vanillaMetal(String path) {
		if (path.equals("iron_ore") || path.equals("deepslate_iron_ore") || path.equals("raw_iron")) {
			return Rock.IRON;
		}
		if (path.equals("copper_ore") || path.equals("deepslate_copper_ore") || path.equals("raw_copper")) {
			return Rock.COPPER;
		}
		if (path.equals("gold_ore") || path.equals("deepslate_gold_ore") || path.equals("raw_gold")) {
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
