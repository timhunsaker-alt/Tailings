package dev.wildcatter.tailings.item;

import dev.wildcatter.tailings.block.VentNetwork;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * A personal gas detector (ventilation.md, L1): held in a drive it reads the air where the player stands each second, fresh or STALE,
 * and RADON in tuff or sandstone; a click reads it once. It only reads: it changes nothing.
 */
public class GasDetectorItem extends Item {
	public GasDetectorItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level instanceof ServerLevel server) {
			player.sendOverlayMessage(VentNetwork.detector(server, player.blockPosition()));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal("Reads a drive's air: it needs 0.30 m/s across the bore"));
	}
}
