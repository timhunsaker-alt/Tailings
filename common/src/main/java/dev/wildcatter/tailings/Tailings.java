package dev.wildcatter.tailings;

import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod, the same on every loader. Each loader's entry point calls {@link #register(String)} while the registries are open.
 */
public final class Tailings {
	public static final String MOD_ID = "tailings";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private Tailings() {
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	/** {@code requiredMod} is Wildcatter's mod id, read off the Wildcatter jar on the compile classpath. */
	public static void register(String requiredMod) {
		TailingsBlocks.register();
		TailingsItems.register();
		LOGGER.info("Tailings loaded; requires {}", requiredMod);
	}

	public static Component tabTitle() {
		return Component.translatable("itemGroup.tailings");
	}

	public static ItemStack tabIcon() {
		return new ItemStack(TailingsBlocks.STAMP_MILL);
	}

	public static void creativeTab(Consumer<ItemStack> output) {
		output.accept(new ItemStack(TailingsBlocks.STAMP_MILL));
		output.accept(new ItemStack(TailingsBlocks.HAND_SCREEN));
		output.accept(new ItemStack(TailingsBlocks.SHAKING_TABLE));
		output.accept(new ItemStack(TailingsBlocks.MAGNETIC_SEPARATOR));
		for (dev.wildcatter.tailings.plant.Machine machine : dev.wildcatter.tailings.plant.Machine.values()) {
			output.accept(new ItemStack(TailingsBlocks.plantBlock(machine)));
		}
		output.accept(new ItemStack(TailingsBlocks.TUNNEL_DRILL));
		output.accept(new ItemStack(TailingsBlocks.DEWATERING_PUMP));
		output.accept(new ItemStack(TailingsBlocks.LINING));
		output.accept(new ItemStack(TailingsBlocks.MINE_RAIL));
		output.accept(new ItemStack(TailingsBlocks.TRACTION_RECTIFIER));
		output.accept(new ItemStack(TailingsBlocks.DUMP_STATION));
		output.accept(new ItemStack(TailingsBlocks.BLOCK_SIGNAL));
		output.accept(new ItemStack(TailingsItems.LOCOMOTIVE));
		output.accept(new ItemStack(TailingsItems.CAR));
		for (ItemStack stack : TailingsItems.REAGENTS.stream().map(ItemStack::new).toList()) {
			output.accept(stack);
		}
		for (ItemStack stack : TailingsItems.SIZED.stream().map(ItemStack::new).toList()) {
			output.accept(stack);
		}
		for (ItemStack stack : TailingsItems.NUGGETS.stream().map(ItemStack::new).toList()) {
			output.accept(stack);
		}
		for (ItemStack stack : TailingsItems.MINERALS.stream().map(ItemStack::new).toList()) {
			output.accept(stack);
		}
		for (ItemStack stack : TailingsItems.CONCENTRATES.stream().map(ItemStack::new).toList()) {
			output.accept(stack);
		}
	}
}
