package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.haul.CarLoad;
import dev.wildcatter.tailings.haul.Conductor;
import dev.wildcatter.tailings.haul.Haul;
import dev.wildcatter.tailings.haul.Motion;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** The train, if any, that is sitting on this rail, and whether the rectifier is feeding it. */
public class RailBlockEntity extends BlockEntity {
	public enum Kind {
		NONE,
		LOCO,
		CAR
	}

	private Kind kind = Kind.NONE;
	private String trainId = "";
	private int index;
	private final CarLoad load = new CarLoad();
	private boolean powered;
	private int wait;
	private BlockPos signal = BlockPos.ZERO;

	public RailBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.RAIL, pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, RailBlockEntity rail) {
		// The locomotive entity drives the train, so a loaded chunk without the entity does not double-step it.
	}

	public Kind kind() {
		return kind;
	}

	public String trainId() {
		return trainId;
	}

	public CarLoad load() {
		return load;
	}

	public boolean powered() {
		return powered;
	}

	public void setPowered(boolean powered) {
		this.powered = powered;
		setChanged();
	}

	public boolean conductorLive() {
		return Conductor.live(powered);
	}

	public void setSignal(BlockPos signal) {
		this.signal = signal == null ? BlockPos.ZERO : signal;
		setChanged();
	}

	public boolean placeLoco() {
		if (kind != Kind.NONE || level == null) {
			return false;
		}
		kind = Kind.LOCO;
		trainId = "train-" + worldPosition.asLong();
		index = 0;
		wait = 0;
		setChanged();
		if (level instanceof net.minecraft.server.level.ServerLevel server) {
			dev.wildcatter.tailings.entity.MineTrainEntity train = new dev.wildcatter.tailings.entity.MineTrainEntity(level, true);
			train.setPos(worldPosition.getX() + 0.5, worldPosition.getY(), worldPosition.getZ() + 0.5);
			train.attach(worldPosition);
			server.addFreshEntity(train);
		}
		return true;
	}

	/** A car joins the train on the rail ahead of it, up to 10 cars. */
	public boolean placeCar() {
		if (kind != Kind.NONE || level == null) {
			return false;
		}
		RailBlockEntity ahead = neighbor(facing());
		if (ahead == null || ahead.trainId.isEmpty()) {
			kind = Kind.CAR;
			trainId = "car-" + worldPosition.asLong();
			index = 1;
			setChanged();
			return true;
		}
		if (ahead.consist().size() - 1 >= Haul.MAX_CARS) {
			return false;
		}
		kind = Kind.CAR;
		trainId = ahead.trainId;
		index = ahead.index + 1;
		setChanged();
		return true;
	}

	/** Sneak-use couples this car to the train one block ahead. */
	public boolean joinAhead() {
		if (kind != Kind.CAR || level == null) {
			return false;
		}
		RailBlockEntity ahead = neighbor(facing());
		if (ahead == null || ahead.trainId.isEmpty() || ahead.trainId.equals(trainId)) {
			return false;
		}
		if (ahead.consist().size() - 1 >= Haul.MAX_CARS) {
			return false;
		}
		trainId = ahead.trainId;
		index = ahead.index + 1;
		setChanged();
		return true;
	}

	/** Moves the train one block, or tips it into a dump. Returns false when it stays. */
	public boolean stepOnce() {
		if (level == null || kind != Kind.LOCO) {
			return false;
		}
		Direction facing = travel();
		BlockPos next = worldPosition.relative(facing);
		boolean decline = decline();
		BlockEntity ahead = level.getBlockEntity(next);
		if (ahead instanceof DumpBlockEntity dump) {
			if (!powered || Motion.halt(true, decline, rack(), true, true) != Motion.Halt.NONE) {
				return false;
			}
			if (!dump.offer(consistLoad())) {
				return false;
			}
			for (RailBlockEntity car : consist()) {
				if (car.kind == Kind.CAR) {
					car.load.clear();
					car.setChanged();
				}
			}
			return true;
		}
		RailBlockEntity dest = ahead instanceof RailBlockEntity rail ? rail : null;
		boolean blocked = dest == null || (!dest.trainId.isEmpty() && !dest.trainId.equals(trainId));
		boolean sectionClear = !blocked;
		if (dest != null && !dest.signal.equals(BlockPos.ZERO) && level.getBlockEntity(dest.signal) instanceof SignalBlockEntity signalBe) {
			sectionClear = signalBe.enter(trainId);
		}
		boolean fed = powered && dest != null && dest.powered;
		if (dest == null || Motion.halt(fed, decline, rack(), sectionClear, true) != Motion.Halt.NONE) {
			return false;
		}
		shift(dest, facing);
		return true;
	}

	private void shift(RailBlockEntity dest, Direction facing) {
		String id = trainId;
		List<RailBlockEntity> consist = consist();
		List<Kind> kinds = new ArrayList<>();
		List<String> loads = new ArrayList<>();
		List<Integer> indexes = new ArrayList<>();
		for (RailBlockEntity rail : consist) {
			kinds.add(rail.kind);
			loads.add(rail.load.save());
			indexes.add(rail.index);
		}
		BlockPos previousSignal = signal;
		List<BlockPos> targets = new ArrayList<>();
		targets.add(dest.getBlockPos());
		for (int i = 0; i < consist.size() - 1; i++) {
			targets.add(consist.get(i).getBlockPos());
		}
		for (RailBlockEntity rail : consist) {
			rail.clearVehicle();
		}
		for (int i = 0; i < targets.size(); i++) {
			if (level.getBlockEntity(targets.get(i)) instanceof RailBlockEntity rail) {
				rail.kind = kinds.get(i);
				rail.trainId = id;
				rail.index = indexes.get(i);
				rail.load.load(loads.get(i));
				if (rail.kind == Kind.LOCO) {
					rail.wait = Motion.ticks(decline());
				}
				rail.setChanged();
			}
		}
		if (!previousSignal.equals(BlockPos.ZERO) && level.getBlockEntity(previousSignal) instanceof SignalBlockEntity signalBe) {
			boolean stillThere = false;
			for (BlockPos target : targets) {
				if (level.getBlockEntity(target) instanceof RailBlockEntity rail && previousSignal.equals(rail.signal)) {
					stillThere = true;
				}
			}
			if (!stillThere) {
				signalBe.leave(trainId);
			}
		}
	}

	public List<RailBlockEntity> consist() {
		List<RailBlockEntity> found = new ArrayList<>();
		if (trainId.isEmpty() || level == null) {
			return found;
		}
		RailBlockEntity loco = this;
		BlockPos cursor = worldPosition;
		Direction facing = facing();
		while (level.getBlockEntity(cursor) instanceof RailBlockEntity rail && trainId.equals(rail.trainId)) {
			if (rail.kind == Kind.LOCO) {
				loco = rail;
				break;
			}
			cursor = cursor.relative(facing);
		}
		found.add(loco);
		cursor = loco.worldPosition.relative(loco.facing().getOpposite());
		while (found.size() <= Haul.MAX_CARS && level.getBlockEntity(cursor) instanceof RailBlockEntity rail && trainId.equals(rail.trainId)) {
			found.add(rail);
			cursor = cursor.relative(loco.facing().getOpposite());
		}
		return found;
	}

	private java.util.Map<String, Integer> consistLoad() {
		java.util.Map<String, Integer> sum = new java.util.LinkedHashMap<>();
		for (RailBlockEntity rail : consist()) {
			for (var entry : rail.load.contents().entrySet()) {
				sum.merge(entry.getKey(), entry.getValue(), Integer::sum);
			}
		}
		return sum;
	}

	private void clearVehicle() {
		kind = Kind.NONE;
		trainId = "";
		index = 0;
		load.clear();
		wait = 0;
		setChanged();
	}

	private RailBlockEntity neighbor(Direction facing) {
		if (level == null) {
			return null;
		}
		BlockEntity entity = level.getBlockEntity(worldPosition.relative(facing));
		return entity instanceof RailBlockEntity rail ? rail : null;
	}

	private Direction facing() {
		if (getBlockState().hasProperty(MineSwitchBlock.FACING)) {
			return getBlockState().getValue(MineSwitchBlock.FACING);
		}
		return getBlockState().getValue(MineRailBlock.FACING);
	}

	/** Straight along the rail, or out the turnout when the switch is thrown. */
	private Direction travel() {
		Direction facing = facing();
		if (getBlockState().hasProperty(MineSwitchBlock.THROWN) && Boolean.TRUE.equals(getBlockState().getValue(MineSwitchBlock.THROWN))) {
			return facing.getClockWise();
		}
		return facing;
	}

	private boolean decline() {
		return getBlockState().hasProperty(MineRailBlock.GRADE) && getBlockState().getValue(MineRailBlock.GRADE) != RailGrade.FLAT;
	}

	private boolean rack() {
		return !getBlockState().hasProperty(MineRailBlock.RACK) || getBlockState().getValue(MineRailBlock.RACK);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putString("kind", kind.name());
		output.putString("train", trainId);
		output.putInt("index", index);
		output.putString("load", load.save());
		output.putBoolean("powered", powered);
		output.putInt("wait", wait);
		output.putLong("signal", signal.asLong());
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		try {
			kind = Kind.valueOf(input.getStringOr("kind", "NONE"));
		} catch (IllegalArgumentException ex) {
			kind = Kind.NONE;
		}
		trainId = input.getStringOr("train", "");
		index = input.getIntOr("index", 0);
		load.load(input.getStringOr("load", ""));
		powered = input.getBooleanOr("powered", false);
		wait = input.getIntOr("wait", 0);
		signal = BlockPos.of(input.getLongOr("signal", 0));
	}
}
