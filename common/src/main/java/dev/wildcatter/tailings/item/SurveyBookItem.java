package dev.wildcatter.tailings.item;

import dev.wildcatter.tailings.block.SurveyStakeBlock;
import dev.wildcatter.tailings.pit.PitDrive;
import dev.wildcatter.tailings.pit.Survey;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The survey book (docs/multiblocks/open-pit.md, P2a): click survey stakes in order to make a haul route; each click surveys the leg
 * from the stake before and writes what is wrong. Click a haul truck to give it the route, or a wheel loader to set it to the route's
 * LOAD stake. Sneak-use in the air clears it.
 */
public class SurveyBookItem extends Item {
	public static final int MOST_STAKES = 32;

	public SurveyBookItem(Properties properties) {
		super(properties);
	}

	/** The route's stakes, in order. */
	public static List<BlockPos> route(ItemStack stack) {
		List<BlockPos> out = new ArrayList<>();
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data == null) {
			return out;
		}
		for (long packed : data.copyTag().getLongArray("route").orElse(new long[0])) {
			out.add(BlockPos.of(packed));
		}
		return out;
	}

	public static List<String> problems(ItemStack stack) {
		List<String> out = new ArrayList<>();
		CustomData data = stack.get(DataComponents.CUSTOM_DATA);
		if (data != null) {
			ListTag list = data.copyTag().getListOrEmpty("problems");
			for (int i = 0; i < list.size(); i++) {
				out.add(list.getStringOr(i, ""));
			}
		}
		return out;
	}

	private static void write(ItemStack stack, List<BlockPos> route, List<String> problems) {
		CompoundTag tag = new CompoundTag();
		tag.putLongArray("route", route.stream().mapToLong(BlockPos::asLong).toArray());
		ListTag list = new ListTag();
		problems.forEach(p -> list.add(StringTag.valueOf(p)));
		tag.put("problems", list);
		stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
	}

	/** A block a truck cannot drive through: anything with a collision box, a stake aside. */
	public static Survey.Ground ground(Level level) {
		return (x, y, z) -> {
			BlockPos pos = new BlockPos(x, y, z);
			BlockState state = level.getBlockState(pos);
			return !(state.getBlock() instanceof SurveyStakeBlock) && !state.getCollisionShape(level, pos).isEmpty();
		};
	}

	/** Surveys a leg for the 100 t truck. */
	public static Survey.Leg survey(Level level, BlockPos from, BlockPos to) {
		return Survey.leg(ground(level), from.getX(), from.getY(), from.getZ(), to.getX(), to.getY(), to.getZ(), PitDrive.TRUCK.width(), PitDrive.TRUCK.height());
	}

	/** Adds a stake to the book's route, surveying the leg to it; what to tell the player. Public for tests. */
	public static String addStake(Level level, ItemStack stack, BlockPos pos) {
		List<BlockPos> route = route(stack);
		List<String> problems = problems(stack);
		if (!route.isEmpty() && route.get(route.size() - 1).equals(pos)) {
			return "that stake is already the route's last";
		}
		if (route.size() >= MOST_STAKES) {
			return "a route holds " + MOST_STAKES + " stakes";
		}
		String said;
		if (route.isEmpty()) {
			said = "route started at " + pos.toShortString();
		} else {
			BlockPos from = route.get(route.size() - 1);
			Survey.Leg leg = survey(level, from, pos);
			String problem = leg.problem();
			said = String.format(java.util.Locale.ROOT, "leg %d: %.0f m, grade %.1f %%%s%s", route.size(), Math.hypot(pos.getX() - from.getX(), pos.getZ() - from.getZ()),
					leg.grade() * 100, leg.oneWay() ? ", one-way" : ", two-way", problem == null ? "" : "; " + problem);
			if (problem != null) {
				problems.add("leg " + route.size() + ": " + problem);
			}
		}
		route.add(pos);
		write(stack, route, problems);
		return said;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		if (!(level.getBlockState(pos).getBlock() instanceof SurveyStakeBlock)) {
			return InteractionResult.PASS;
		}
		if (!level.isClientSide() && context.getPlayer() != null) {
			context.getPlayer().sendSystemMessage(Component.literal("Survey Book: " + addStake(level, context.getItemInHand(), pos)));
		}
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player.isShiftKeyDown()) {
			if (!level.isClientSide()) {
				player.getItemInHand(hand).remove(DataComponents.CUSTOM_DATA);
				player.sendOverlayMessage(Component.literal("Survey Book: cleared"));
			}
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
		List<BlockPos> route = route(stack);
		if (route.isEmpty()) {
			tooltip.accept(Component.literal("No route: click survey stakes in order"));
			return;
		}
		tooltip.accept(Component.literal(route.size() + " stakes, " + route.get(0).toShortString() + " to " + route.get(route.size() - 1).toShortString()));
		for (String problem : problems(stack)) {
			tooltip.accept(Component.literal(problem));
		}
	}
}
