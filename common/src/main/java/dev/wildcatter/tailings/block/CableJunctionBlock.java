package dev.wildcatter.tailings.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The TBM's cable junction (docs/multiblocks/tunnel-boring-machine.md, proposed): a terminal box where the machine was started. A
 * Wildcatter 4.16 kV cable lands on any face, and the trailing cable to the machine leaves it.
 */
public class CableJunctionBlock extends BaseEntityBlock {
	public CableJunctionBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		return new CableJunctionBlockEntity(pos, state);
	}

	@Override
	protected RenderShape getRenderShape(BlockState state) {
		return RenderShape.MODEL;
	}

	@Override
	protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
		if (!level.isClientSide() && level.getBlockEntity(pos) instanceof CableJunctionBlockEntity junction) {
			player.sendOverlayMessage(junction.inspect());
		}
		return InteractionResult.SUCCESS;
	}
}
