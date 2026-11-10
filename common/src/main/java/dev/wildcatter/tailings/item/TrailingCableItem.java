package dev.wildcatter.tailings.item;

import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.block.PlantMachineBlock;
import dev.wildcatter.tailings.entity.CableLink;
import dev.wildcatter.tailings.entity.PitVehicleEntity;
import dev.wildcatter.tailings.pit.PitPower;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * A 400 m reel of medium-voltage mining cable (docs/multiblocks/open-pit-p5-big-machines.md, Trailing cables). Right-click a pit
 * switch house to take its first free outlet, then the machine's cable reel (the machine) to couple it; another reel clicked on a
 * coupled machine adds 400 m. Sneak-right-click a coupled machine with one to take its cable off: the reels come back.
 */
public class TrailingCableItem extends Item {
	public TrailingCableItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		BlockState state = context.getLevel().getBlockState(context.getClickedPos());
		if (!(state.getBlock() instanceof PlantMachineBlock)) {
			return InteractionResult.PASS;
		}
		BlockPos main = PlantMachineBlock.mainOf(context.getClickedPos(), state);
		if (context.getLevel().getBlockEntity(main) instanceof dev.wildcatter.tailings.block.RopeShovelBlockEntity crawlers) {
			// the rope shovel standing as its parts: its reel is in the crawlers
			if (context.getLevel() instanceof ServerLevel server && context.getPlayer() != null) {
				long[] from = from(context.getItemInHand());
				String why = crawlers.cable() == null && from == null ? "click a pit switch house first, to take one of its outlets"
						: crawlers.couple(server, from == null ? BlockPos.ZERO : BlockPos.of(from[0]), from == null ? 0 : (int) from[1]);
				if (why == null) {
					context.getItemInHand().remove(DataComponents.CUSTOM_DATA);
					if (!context.getPlayer().getAbilities().instabuild) {
						context.getItemInHand().shrink(1);
					}
				}
				context.getPlayer().sendOverlayMessage(Component.literal("Rope Shovel: " + (why == null ? crawlers.cable().describe() : why)));
			}
			return InteractionResult.SUCCESS;
		}
		if (!(context.getLevel().getBlockEntity(main) instanceof PitSwitchHouseBlockEntity house)) {
			return InteractionResult.PASS;
		}
		if (context.getLevel() instanceof ServerLevel && context.getPlayer() != null) {
			context.getPlayer().sendOverlayMessage(Component.literal("Trailing Cable: " + pickUp(context.getItemInHand(), house)));
		}
		return InteractionResult.SUCCESS;
	}

	/** Takes the house's first free outlet onto the reel in hand; what to tell the player. Public for tests. */
	public static String pickUp(ItemStack stack, PitSwitchHouseBlockEntity house) {
		int outlet = house.freeOutlet();
		if (outlet < 0) {
			return "every outlet of this switch house has a cable on it";
		}
		CompoundTag tag = new CompoundTag();
		tag.putLong("house", house.getBlockPos().asLong());
		tag.putInt("outlet", outlet);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		return "outlet " + (outlet + 1) + " taken: now click the machine's cable reel";
	}

	/** The house and outlet this reel was taken from, or null. */
	public static long @Nullable [] from(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return null;
		}
		CompoundTag tag = data.copyTag();
		return tag.contains("house") ? new long[] {tag.getLongOr("house", 0), tag.getIntOr("outlet", 0)} : null;
	}

	/**
	 * The reel in hand clicked on a machine: couples it to the outlet it was taken from, adds its 400 m to the machine's cable, or (with
	 * sneak) takes the machine's cable off. What to tell the player. Public for tests.
	 */
	public static String onMachine(ServerLevel level, Player player, ItemStack stack, PitVehicleEntity machine) {
		if (!machine.takesCable()) {
			return "it has no cable reel";
		}
		CableLink link = machine.cable();
		if (player != null && player.isShiftKeyDown() && link != null) {
			machine.dropCable(level);
			return "trailing cable taken off";
		}
		if (link != null) {
			link.addMetres(PitPower.CABLE_M);
			if (player == null || !player.getAbilities().instabuild) {
				stack.shrink(1);
			}
			return String.format(Locale.ROOT, "another 400 m on its reel: %.0f m of cable", link.metres());
		}
		String why = couple(level, stack, machine);
		if (why == null && (player == null || !player.getAbilities().instabuild)) {
			stack.shrink(1);
		}
		return why == null ? "coupled: " + machine.cable().describe() : why;
	}

	/** Couples the machine to the outlet on the reel; why not, or null. Public for tests. */
	public static @Nullable String couple(ServerLevel level, ItemStack stack, PitVehicleEntity machine) {
		long[] from = from(stack);
		if (from == null) {
			return "click a pit switch house first, to take one of its outlets";
		}
		BlockPos at = BlockPos.of(from[0]);
		int outlet = (int) from[1];
		if (!(level.getBlockEntity(at) instanceof PitSwitchHouseBlockEntity house)) {
			return "the switch house it was taken from is gone";
		}
		CableLink link = new CableLink(at, outlet, PitPower.CABLE_M, 0);
		var coupler = link.coupler(level);
		double straight = Math.hypot(machine.getX() - coupler.x, machine.getZ() - coupler.z);
		if (straight > PitPower.CABLE_M) {
			return String.format(Locale.ROOT, "the machine is %.0f m from the switch house: a reel is 400 m", straight);
		}
		String why = house.couple(outlet, machine.getUUID(), machine.getType().getDescription().getString(), PitPower.CABLE_M);
		if (why != null) {
			return why;
		}
		link.setPaid(straight);
		machine.setCable(link);
		stack.remove(DataComponents.CUSTOM_DATA);
		return null;
	}
}
