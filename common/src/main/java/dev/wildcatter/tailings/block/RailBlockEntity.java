package dev.wildcatter.tailings.block;

import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.haul.CarLoad;
import dev.wildcatter.tailings.haul.Conductor;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * One rail or switch: whether the rectifier feeds it, and the block signal whose section it is in. The trains themselves are
 * entities (docs/multiblocks/haulage.md); a rail saved before R5 that still holds a train turns it into its entities on its first
 * tick.
 */
public class RailBlockEntity extends BlockEntity {
	private boolean powered;
	private BlockPos signal = BlockPos.ZERO;
	/** A train saved on the rail before R5: "LOCO" or "CAR", its id, its place and its load. Cleared once it is an entity. */
	private String oldKind = "NONE";
	private String oldTrain = "";
	private int oldIndex;
	private final CarLoad oldLoad = new CarLoad();

	public RailBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.RAIL, pos, state);
	}

	public static void tick(Level level, BlockPos pos, BlockState state, RailBlockEntity rail) {
		if (!"NONE".equals(rail.oldKind) && level instanceof ServerLevel server) {
			rail.migrate(server);
		}
	}

	/** An old locomotive becomes its entity facing the way the old train ran; an old car becomes a car, and couples to it. */
	private void migrate(ServerLevel server) {
		Direction travel = travel();
		if ("LOCO".equals(oldKind)) {
			MineVehicleEntity.placeLocomotive(server, worldPosition, RailView.heading(travel));
		} else if ("CAR".equals(oldKind)) {
			MineVehicleEntity car = MineVehicleEntity.placeCar(server, worldPosition, RailView.heading(travel));
			if (car != null) {
				car.load().load(oldLoad.save());
				car.refreshLoad();
			}
		}
		oldKind = "NONE";
		oldTrain = "";
		oldLoad.clear();
		setChanged();
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

	public BlockPos signal() {
		return signal;
	}

	public void setSignal(BlockPos signal) {
		this.signal = signal == null ? BlockPos.ZERO : signal;
		setChanged();
	}

	/** The way the old block-stepped trains ran over this rail: its facing, or out of a thrown switch's branch. */
	private Direction travel() {
		BlockState state = getBlockState();
		if (state.hasProperty(MineSwitchBlock.THROWN)) {
			Direction facing = state.getValue(MineSwitchBlock.FACING);
			return Boolean.TRUE.equals(state.getValue(MineSwitchBlock.THROWN)) ? facing.getClockWise() : facing;
		}
		return state.getValue(MineRailBlock.FACING);
	}

	@Override
	protected void saveAdditional(ValueOutput output) {
		super.saveAdditional(output);
		output.putBoolean("powered", powered);
		output.putLong("signal", signal.asLong());
		if (!"NONE".equals(oldKind)) {
			output.putString("kind", oldKind);
			output.putString("train", oldTrain);
			output.putInt("index", oldIndex);
			output.putString("load", oldLoad.save());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		powered = input.getBooleanOr("powered", false);
		signal = BlockPos.of(input.getLongOr("signal", 0));
		oldKind = input.getStringOr("kind", "NONE");
		oldTrain = input.getStringOr("train", "");
		oldIndex = input.getIntOr("index", 0);
		oldLoad.load(input.getStringOr("load", ""));
	}

	/** For a game test: a rail as a world saved before R5 holds a train. */
	public void setOldTrain(String kind, int index, String load) {
		oldKind = kind;
		oldTrain = "old";
		oldIndex = index;
		oldLoad.load(load);
		setChanged();
	}
}
