package dev.wildcatter.tailings.item;

import dev.wildcatter.tailings.haul.Battery;
import java.util.Locale;
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
 * A battery locomotive's traction battery box (docs/multiblocks/battery-locomotive.md, L2): 132 V, 440 Ah of lead-acid cells in a
 * steel box. It keeps its charge as item data, in usable watt-hours; one with none (a new one) holds {@link Battery#NEW_SHARE}.
 */
public class LocoBatteryItem extends Item {
	private static final String CHARGE = "charge_wh";

	public LocoBatteryItem(Properties properties) {
		super(properties);
	}

	/** Usable watt-hours the battery in {@code stack} holds. */
	public static double charge(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? Battery.NEW_SHARE * Battery.USABLE_WH : Math.max(0, Math.min(Battery.USABLE_WH, data.copyTag().getDoubleOr(CHARGE, 0)));
	}

	public static void setCharge(ItemStack stack, double wh) {
		CompoundTag tag = new CompoundTag();
		tag.putDouble(CHARGE, Math.max(0, Math.min(Battery.USABLE_WH, wh)));
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	/** One battery holding {@code wh}. */
	public static ItemStack of(Item item, double wh) {
		ItemStack stack = new ItemStack(item);
		setCharge(stack, wh);
		return stack;
	}

	/** How a charge reads: kWh of the usable, the share, and LOW or FLAT. */
	public static String describe(double wh) {
		String state = Battery.flat(wh) ? " (FLAT)" : Battery.low(wh) ? " (LOW BATTERY)" : "";
		return String.format(Locale.ROOT, "%.1f of %.1f kWh, %.0f %%%s", wh / 1000, Battery.USABLE_WH / 1000, 100 * Battery.share(wh), state);
	}

	@Override
	public boolean isBarVisible(ItemStack stack) {
		return true;
	}

	@Override
	public int getBarWidth(ItemStack stack) {
		return (int) Math.round(MAX_BAR_WIDTH * Battery.share(charge(stack)));
	}

	@Override
	public int getBarColor(ItemStack stack) {
		return Battery.low(charge(stack)) ? 0xD04030 : 0x40B040;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal("Traction battery, 132 V 440 Ah: " + describe(charge(stack))));
	}
}
