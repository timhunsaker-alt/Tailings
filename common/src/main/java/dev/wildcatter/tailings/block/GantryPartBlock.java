package dev.wildcatter.tailings.block;

import dev.wildcatter.multiblock.Footprint;
import dev.wildcatter.tailings.mine.TbmGantry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** A gantry's looks: never placed in the world (it has no item of its own), its states are the models the gantry entity draws. */
public class GantryPartBlock extends TailingsPartBlock {
	public GantryPartBlock(Properties properties, TbmGantry gantry) {
		super(properties, Footprint.box(TbmGantry.WIDTH, TbmGantry.HEIGHT, gantry.length(), 0, 0), gantry.id());
	}

	@Override
	protected BlockEntity createMain(BlockPos pos, BlockState state) {
		return new TbmPartBlockEntity(pos, state);
	}
}
