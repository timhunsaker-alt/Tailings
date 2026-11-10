package dev.wildcatter.tailings.block;

import dev.wildcatter.multiblock.Footprint;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A skip's looks (docs/multiblocks/shaft-and-hoist.md, L4): never placed in the world (it has no item of its own; the skip item hangs
 * a {@link dev.wildcatter.tailings.entity.ShaftSkipEntity} in a headframe), its states are the models the skip entity draws.
 */
public class SkipPartBlock extends TailingsPartBlock {
	public static final int WIDTH = 2, HEIGHT = 4;

	public SkipPartBlock(Properties properties) {
		super(properties, Footprint.box(WIDTH, HEIGHT, WIDTH, 0, 0), "skip");
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new TbmPartBlockEntity(pos, state);
	}
}
