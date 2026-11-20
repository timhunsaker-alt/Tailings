package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.pit.Slopes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A survey prism on a short pole (dragline-hand-control.md, Warnings): placed on a dragline's highwall bench or spoil, it belongs to the
 * section under it. The survey book reads it (sneak-click with an empty hand does too); a survey station reads every prism it can see.
 * A crew reads how far and how fast it moves, never the slope's factor of safety.
 */
public class SlopePrismBlock extends Block {
	private static final VoxelShape SHAPE = Block.box(6, 0, 6, 10, 14, 10);

	public SlopePrismBlock(Properties properties) {
		super(properties);
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return SHAPE;
	}

	@Override
	public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
		super.setPlacedBy(level, pos, state, placer, stack);
		if (level instanceof ServerLevel server) {
			Slopes.addPrism(server, pos);
		}
	}

	@Override
	protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean moved) {
		super.onPlace(state, level, pos, old, moved);
		if (level instanceof ServerLevel server && !old.is(state.getBlock())) {
			Slopes.addPrism(server, pos);
		}
	}

	/** The survey book reads it: let the item have the click. */
	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (stack.getItem() instanceof dev.wildcatter.tailings.item.SurveyBookItem) {
			return InteractionResult.PASS;
		}
		return super.useItemOn(stack, state, level, pos, player, hand, hit);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level instanceof ServerLevel server) {
			player.sendOverlayMessage(read(server, pos));
		}
		return InteractionResult.SUCCESS;
	}

	public static Component read(ServerLevel level, BlockPos pos) {
		return Component.literal("Slope Prism at " + pos.toShortString() + ": " + Slopes.read(level, pos).describe());
	}
}
