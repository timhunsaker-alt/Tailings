package dev.wildcatter.tailings.item;

import dev.wildcatter.tailings.plant.DryMill;
import dev.wildcatter.tailings.plant.HeavyMinerals;
import dev.wildcatter.tailings.rock.Mineral;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * A bag of moist heavy mineral concentrate (docs/multiblocks/dry-mill.md, L6-2): the scavenger spirals' share, as the pressure filter
 * drops it when no dryer takes it. It keeps its assay as item data, whole kg of each mineral, so bags of one feed stack. Fed to a
 * fluid-bed dryer by hopper or by hand; a separator refuses it, WET.
 */
public class HeavyMineralConcentrateItem extends Item {
	private static final String KG = "kg";

	public HeavyMineralConcentrateItem(Properties properties) {
		super(properties);
	}

	/** One bag holding these kg. */
	public static ItemStack of(Item item, Map<Mineral, Integer> kg) {
		ItemStack stack = new ItemStack(item);
		CompoundTag minerals = new CompoundTag();
		kg.forEach((mineral, amount) -> minerals.putInt(mineral.name(), amount));
		CompoundTag tag = new CompoundTag();
		tag.put(KG, minerals);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return stack;
	}

	/** What one bag in {@code stack} holds; empty for a bag with no assay (one from the creative tab). */
	public static HeavyMinerals contents(ItemStack stack) {
		HeavyMinerals out = new HeavyMinerals();
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return out;
		}
		CompoundTag minerals = data.copyTag().getCompoundOrEmpty(KG);
		for (String key : minerals.keySet()) {
			try {
				Mineral mineral = Mineral.valueOf(key);
				if (DryMill.takes(mineral)) {
					out.add(mineral, minerals.getIntOr(key, 0));
				}
			} catch (IllegalArgumentException ex) {
				// left out
			}
		}
		return out;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		HeavyMinerals in = contents(stack);
		tooltip.accept(Component.literal(in.isEmpty() ? "Moist, no assay" : String.format(java.util.Locale.ROOT, "Moist, %.0f kg: %s", in.total(), in.describe())));
	}
}
