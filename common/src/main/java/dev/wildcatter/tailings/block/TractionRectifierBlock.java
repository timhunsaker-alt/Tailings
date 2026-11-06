package dev.wildcatter.tailings.block;

import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.tailings.TailingsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** The 2 x 3 x 2 traction rectifier (docs/multiblocks/haulage.md): it feeds every mine rail and switch its cells touch, and the track joined to them. */
public class TractionRectifierBlock extends TailingsPartBlock {
	public static final Footprint FOOTPRINT = Footprint.box(2, 3, 2, 0, 0);

	public TractionRectifierBlock(Properties properties) {
		super(properties, FOOTPRINT, "traction_rectifier");
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new RectifierBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		return level.isClientSide() ? null : createTickerHelper(type, TailingsBlocks.RECTIFIER, RectifierBlockEntity::tick);
	}

	/** A click on a tripped rectifier closes its DC breaker again; otherwise it reads out. */
	@Override
	protected net.minecraft.world.InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, net.minecraft.world.entity.player.Player player,
			net.minecraft.world.phys.BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(main(pos, state)) instanceof RectifierBlockEntity rectifier && rectifier.reset()) {
			player.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Traction rectifier: DC breaker reset"));
			return net.minecraft.world.InteractionResult.SUCCESS;
		}
		return super.useWithoutItem(state, level, pos, player, hit);
	}
}
