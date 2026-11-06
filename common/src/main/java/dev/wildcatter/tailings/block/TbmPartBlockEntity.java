package dev.wildcatter.tailings.block;

import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.tailings.TailingsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The main cell of a TBM part with no state of its own: the cutterhead, the segment erector, the track layer. The cab works them. */
public class TbmPartBlockEntity extends BlockEntity implements BigPartEntity, Inspectable {
	private boolean grown;

	public TbmPartBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.TBM_PART, pos, state);
	}

	@Override
	public Component inspect() {
		return Component.literal(getBlockState().getBlock().getName().getString() + ": part of the tunnel boring machine; its cab is at the main beam's tail");
	}

	@Override
	public boolean grown() {
		return grown;
	}

	@Override
	public void markGrown() {
		grown = true;
		setChanged();
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("grown", grown);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
	}
}
