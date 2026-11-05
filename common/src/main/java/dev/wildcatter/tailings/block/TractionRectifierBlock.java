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
}
