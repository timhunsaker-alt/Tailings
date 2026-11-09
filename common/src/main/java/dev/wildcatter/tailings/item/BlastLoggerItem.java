package dev.wildcatter.tailings.item;

import dev.wildcatter.tailings.block.BlastholeBlock;
import dev.wildcatter.tailings.block.BlastholeBlockEntity;
import dev.wildcatter.tailings.block.PitPanelMenu;
import dev.wildcatter.tailings.pit.Blasting;
import dev.wildcatter.tailings.pit.Shot;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The blast logger (open-pit-p3-blasting.md, Tying in and firing): a hand-held logger that reads each hole's detonator and writes its
 * delay, row x row delay + hole in row x hole delay (65 and 25 ms by default). It holds up to 200 holes. Click a primed or charged
 * hole's collar to log it; click the blasting machine to load the shot; sneak-use in the air for its settings.
 */
public class BlastLoggerItem extends Item {
	public BlastLoggerItem(Properties properties) {
		super(properties);
	}

	private static CompoundTag tag(ItemStack stack) {
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		return data == null ? new CompoundTag() : data.copyTag();
	}

	public static int rowMs(ItemStack stack) {
		return tag(stack).getIntOr("row_ms", Blasting.ROW_DELAY_MS);
	}

	public static int holeMs(ItemStack stack) {
		return tag(stack).getIntOr("hole_ms", Blasting.HOLE_DELAY_MS);
	}

	public static List<Shot.Hole> holes(ItemStack stack) {
		CompoundTag tag = tag(stack);
		long[] at = tag.getLongArray("holes").orElse(new long[0]);
		int[] delays = tag.getIntArray("delays").orElse(new int[0]);
		List<Shot.Hole> out = new ArrayList<>();
		for (int i = 0; i < at.length; i++) {
			out.add(new Shot.Hole(BlockPos.of(at[i]), i < delays.length ? delays[i] : 0));
		}
		return out;
	}

	private static void write(ItemStack stack, List<Shot.Hole> holes, int rowMs, int holeMs) {
		CompoundTag tag = new CompoundTag();
		tag.putLongArray("holes", holes.stream().mapToLong(h -> h.collar().asLong()).toArray());
		tag.putIntArray("delays", holes.stream().mapToInt(Shot.Hole::delayMs).toArray());
		tag.putInt("row_ms", rowMs);
		tag.putInt("hole_ms", holeMs);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	public static void setDelays(ItemStack stack, int rowMs, int holeMs) {
		write(stack, holes(stack), rowMs, holeMs);
	}

	public static void clear(ItemStack stack) {
		write(stack, List.of(), rowMs(stack), holeMs(stack));
	}

	/** Logs a hole: gives it its delay and adds it to the shot. Public for tests. */
	public static String log(Level level, ItemStack stack, BlockPos pos) {
		if (!(level.getBlockEntity(pos) instanceof BlastholeBlockEntity hole) || !level.getBlockState(pos).getValue(BlastholeBlock.COLLAR)) {
			return "not a hole's collar";
		}
		if (!hole.primed() && hole.chargeKg() <= 0 && !hole.booster() && hole.detonator() == 0) {
			return "nothing to log: prime it first";
		}
		if (hole.misfired()) {
			return "MISFIRED: re-prime it, or wash it out";
		}
		List<Shot.Hole> holes = holes(stack);
		holes.removeIf(h -> h.collar().equals(pos));
		if (holes.size() >= Blasting.LOGGER_HOLES) {
			return "the logger holds " + Blasting.LOGGER_HOLES + " holes";
		}
		int delay = Blasting.delayMs(hole.row(), hole.col(), rowMs(stack), holeMs(stack));
		hole.log(delay);
		holes.add(new Shot.Hole(pos.immutable(), delay));
		write(stack, holes, rowMs(stack), holeMs(stack));
		return "logged " + pos.toShortString() + ": row " + (hole.row() + 1) + ", hole " + (hole.col() + 1) + ", " + delay + " ms"
				+ (hole.detonator() == 0 ? "; NO DETONATOR answers" : "") + "; " + holes.size() + " holes";
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!(level.getBlockState(pos).getBlock() instanceof BlastholeBlock)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide() && context.getPlayer() != null) {
			context.getPlayer().sendOverlayMessage(Component.literal("Blast Logger: " + log(level, context.getItemInHand(), pos)));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!level.isClientSide()) {
			if (player.isShiftKeyDown()) {
				player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new PitPanelMenu(id, inventory, new Panel(player, hand)), getName(player.getItemInHand(hand))));
			} else {
				ItemStack stack = player.getItemInHand(hand);
				player.sendOverlayMessage(Component.literal("Blast Logger: " + holes(stack).size() + " holes; row " + rowMs(stack) + " ms, hole " + holeMs(stack) + " ms"));
			}
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		tooltip.accept(Component.literal(holes(stack).size() + " holes logged; row " + rowMs(stack) + " ms, hole " + holeMs(stack) + " ms"));
	}

	/** The logger's settings, on the stack in the player's hand. */
	private record Panel(Player player, InteractionHand hand) implements PitPanelMenu.Panel {
		private ItemStack stack() {
			return player.getItemInHand(hand);
		}

		@Override
		public PitPanelMenu.Kind kind() {
			return PitPanelMenu.Kind.LOGGER;
		}

		@Override
		public int setting(int i) {
			return i == 0 ? rowMs(stack()) : holeMs(stack());
		}

		@Override
		public void set(int i, int value, Player p) {
			setDelays(stack(), i == 0 ? value : rowMs(stack()), i == 1 ? value : holeMs(stack()));
		}

		@Override
		public void action(int a, Player p) {
			clear(stack());
		}

		@Override
		public String status() {
			return holes(stack()).size() + " holes logged. New delays apply to holes logged from now on.";
		}

		@Override
		public boolean stillValid(Player p) {
			return stack().getItem() instanceof BlastLoggerItem;
		}
	}
}
