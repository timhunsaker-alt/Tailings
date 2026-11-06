package dev.wildcatter.tailings.block;

import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.mine.TbmPart;
import dev.wildcatter.tailings.platform.Platform;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * One part of the tunnel boring machine at true size (docs/multiblocks/tunnel-boring-machine.md). The main beam's main cell holds
 * the cab ({@link DrillBlockEntity}); a click anywhere on the main beam opens the cab's screen.
 */
public class TbmPartBlock extends TailingsPartBlock {
	private final TbmPart part;

	public TbmPartBlock(Properties properties, TbmPart part) {
		super(properties, Footprint.box(part.width(), part.height(), part.length(), 0, 0), part.id());
		this.part = part;
	}

	public TbmPart part() {
		return part;
	}

	public static BlockPos mainOf(BlockPos pos, BlockState state) {
		return main(pos, state);
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return part == TbmPart.MAIN_BEAM ? new DrillBlockEntity(pos, state) : new TbmPartBlockEntity(pos, state);
	}

	@Override
	protected <T extends BlockEntity> @Nullable BlockEntityTicker<T> mainTicker(Level level, BlockState state, BlockEntityType<T> type) {
		if (level.isClientSide() || part != TbmPart.MAIN_BEAM) {
			return null;
		}
		return createTickerHelper(type, TailingsBlocks.DRILL, DrillBlockEntity::tick);
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (part == TbmPart.MAIN_BEAM && level.getBlockEntity(main(pos, state)) instanceof DrillBlockEntity cab) {
			if (!level.isClientSide()) {
				if (player.isShiftKeyDown()) {
					cab.dismantle();
				} else {
					Platform.INSTANCE.openDrill(player, cab);
				}
			}
			return InteractionResult.SUCCESS;
		}
		return super.useWithoutItem(state, level, pos, player, hit);
	}
}
