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

	/**
	 * The rail dump pocket under a rail of this section, beside its ramp, or null: a loaded train waits at this signal while the pocket
	 * cannot take its load. It follows the section's rails from the signal, up to 32 of them.
	 */
	public @org.jspecify.annotations.Nullable DumpBlockEntity dumpPocket() {
		if (level == null) {
			return null;
		}
		java.util.ArrayDeque<BlockPos> queue = new java.util.ArrayDeque<>();
		java.util.Set<BlockPos> seen = new java.util.HashSet<>();
		for (net.minecraft.core.Direction side : net.minecraft.core.Direction.values()) {
			queue.add(worldPosition.relative(side));
		}
		int rails = 0;
		while (!queue.isEmpty() && rails < 32) {
			BlockPos at = queue.poll();
			if (!seen.add(at) || !(level.getBlockEntity(at) instanceof RailBlockEntity rail) || !rail.signal().equals(worldPosition)) {
				continue;
			}
			rails++;
			DumpBlockEntity pocket = DumpBlockEntity.under(level, at);
			if (pocket != null && !pocket.pit()) {
				return pocket;
			}
			for (net.minecraft.core.Direction side : net.minecraft.core.Direction.Plane.HORIZONTAL) {
				for (int dy = -1; dy <= 1; dy++) {
					queue.add(at.relative(side).above(dy));
				}
			}
		}
		return null;
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
