package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.haul.BlockSection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** One block section. A second train cannot enter until the first has left. */
public class SignalBlockEntity extends BlockEntity {
	private final BlockSection section = new BlockSection();
	private static final String SELF = "section";

	public SignalBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.SIGNAL, pos, state);
	}

	public boolean enter(String train) {
		boolean ok = section.enter(SELF, train);
		if (ok) {
			setChanged();
		}
		return ok;
	}

	public void leave(String train) {
		section.leave(SELF, train);
		setChanged();
	}

	public String occupant() {
		return section.occupant(SELF);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		String who = section.occupant(SELF);
		output.putString("occupant", who == null ? "" : who);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		String who = input.getStringOr("occupant", "");
		if (!who.isEmpty()) {
			section.enter(SELF, who);
		}
	}
}
