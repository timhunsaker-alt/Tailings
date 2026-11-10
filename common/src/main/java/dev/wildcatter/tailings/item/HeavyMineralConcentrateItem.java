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
 * A bag of moist heavy mineral concentrate (docs/multiblocks/dry-mill.md, L6-2): the scavenger spirals' share (and, with the world's
 * {@code dry_mill_all_hmc} rule, the spiral bank's), as the pressure filter drops it when no dryer takes it. It keeps its assay as item data, whole kg of each mineral, so bags of one feed stack. Fed to a
 * fluid-bed dryer by hopper or by hand; a separator refuses it, WET.
 */
public class HeavyMineralConcentrateItem extends Item {
	private static final String KG = "kg";
	/** A mineral's pay a kg, in thousandths, where it is not the default: a bag with the spiral bank's share in it (l6-dry-mill.md #3). */
	private static final String RATE = "rate";

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

	/**
	 * One bag of {@code bag}, whole kg of each mineral; a mineral that pays other than the default carries its rate, rounded to a
	 * thousandth so that bags of one feed still stack.
	 */
	public static ItemStack of(Item item, HeavyMinerals bag) {
		Map<Mineral, Integer> kg = new java.util.EnumMap<>(Mineral.class);
		CompoundTag rates = new CompoundTag();
		for (Mineral mineral : bag.minerals()) {
			kg.put(mineral, (int) Math.round(bag.kg(mineral)));
			if (Math.abs(bag.rate(mineral) - HeavyMinerals.defaultRate(mineral)) > 1e-6) {
				rates.putInt(mineral.name(), (int) Math.round(bag.rate(mineral) * 1000));
			}
		}
		ItemStack stack = of(item, kg);
		if (!rates.isEmpty()) {
			CompoundTag tag = stack.get(DataComponents.CUSTOM_DATA).copyTag();
			tag.put(RATE, rates);
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
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
		CompoundTag rates = data.copyTag().getCompoundOrEmpty(RATE);
		for (String key : minerals.keySet()) {
			try {
				Mineral mineral = Mineral.valueOf(key);
				if (DryMill.takes(mineral)) {
					int kg = minerals.getIntOr(key, 0);
					int rate = rates.getIntOr(key, 0);
					out.add(mineral, kg, rate > 0 ? kg * rate / 1000.0 : kg * HeavyMinerals.defaultRate(mineral));
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
