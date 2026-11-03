package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.rock.Milling;
import dev.wildcatter.tailings.rock.Separator;
import dev.wildcatter.tailings.rock.SizeClass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/** A hand-fed starter machine: stamp mill, screen, shaking table, or magnetic separator. */
public class HandMillBlock extends BaseEntityBlock {
	public enum Role {
		STAMP_MILL,
		HAND_SCREEN,
		SHAKING_TABLE,
		MAGNETIC_SEPARATOR;

		public @Nullable SizeClass next(@Nullable SizeClass size) {
			return switch (this) {
				case STAMP_MILL -> Milling.stamp(size);
				case HAND_SCREEN -> Milling.screen(size);
				case SHAKING_TABLE, MAGNETIC_SEPARATOR -> size == SizeClass.SAND ? SizeClass.SAND : null;
			};
		}

		public @Nullable Separator separator() {
			return switch (this) {
				case SHAKING_TABLE -> Separator.GRAVITY;
				case MAGNETIC_SEPARATOR -> Separator.MAGNETIC;
				default -> null;
			};
		}
	}

	private final Role role;

	public HandMillBlock(Properties properties, Role role) {
		super(properties);
		this.role = role;
	}

	public Role role() {
		return role;
	}

	@Override
	public RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new HandMillBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, TailingsBlocks.MILL, HandMillBlockEntity::tick);
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof HandMillBlockEntity mill && mill.offer(stack)) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.TRY_WITH_EMPTY_HAND;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (level.getBlockEntity(pos) instanceof HandMillBlockEntity mill && mill.give(player)) {
			return InteractionResult.SUCCESS;
		}
		return InteractionResult.PASS;
	}
}
