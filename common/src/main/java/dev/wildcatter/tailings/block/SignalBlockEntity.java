package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.haul.BlockSection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** One block section. A second train cannot enter until the first has left. */
public class SignalBlockEntity extends BlockEntity implements dev.wildcatter.control.ControlDevice {
	private final dev.wildcatter.control.Remote remote = new dev.wildcatter.control.Remote(1);

	@Override
	public String deviceName() {
		return "Block signal";
	}

	@Override
	public java.util.List<Point> points() {
		return java.util.List.of(new Point("HOLD", true), new Point("OCCUPIED", false), new Point("AUTO", false));
	}

	@Override
	public boolean readPoint(int point) {
		return point == 1 ? occupant() != null && !occupant().isEmpty() : point == 2 && remote.auto();
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == 0) {
			remote.command(0, on, gameTime);
		}
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		setChanged();
		return remote.cycle();
	}

	/** A PLC may hold a train at this signal or let it go. It can only add a stop: one train a section is still the section's to say. */
	private boolean held() {
		return level != null && remote.on(0, level.getGameTime());
	}

	private final BlockSection section = new BlockSection();
	private static final String SELF = "section";

	public SignalBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.SIGNAL, pos, state);
	}

	public boolean enter(String train) {
		if (held() && !train.equals(occupant())) {
			return false;
		}
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
		remote.save(output);
		String who = section.occupant(SELF);
		output.putString("occupant", who == null ? "" : who);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		remote.load(input);
		String who = input.getStringOr("occupant", "");
		if (!who.isEmpty()) {
			section.enter(SELF, who);
		}
	}
}
