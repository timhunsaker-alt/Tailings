package dev.wildcatter.tailings.block;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.haul.Haul;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
 * One rail or switch, and its length of conductor rail on Wildcatter's DC solver: its volts, whether that feeds it, the
 * locomotive drawing from it, and the block signal whose section it is in. The trains themselves are
 * entities (docs/multiblocks/haulage.md); a rail saved before R5 that still holds a train turns it into its entities on its first
 * tick.
 */
public class RailBlockEntity extends BlockEntity implements CircuitPart {
	private boolean powered;
	/** Its node on Wildcatter's DC circuit, the conductor rail's volts there, and the locomotive drawing from it. */
	private int node = -1;
	private double volts;
	private double demand;
	private long demandUntil;
	private boolean solved;
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

	/** The conductor rail's volts here at the last solve. */
	public double volts() {
		return volts;
	}

	/** True once a solve has reached this rail. */
	public boolean solved() {
		return solved;
	}

	/** A locomotive on this rail draws {@code watts} through its shoe, for the next half second. */
	public void draw(double watts) {
		demand = Math.max(0, watts);
		demandUntil = level == null ? 0 : level.getGameTime() + 10;
	}

	// ---- the conductor rail on Wildcatter's DC solver (docs/multiblocks/haulage.md) -----------------------------------------------

	@Override
	public boolean hasPort(Direction face, dev.wildcatter.circuit.Conductor role, Voltage voltage) {
		return false;
	}

	/** The rails a block up or down the grade beside it: joined by a span, as their faces do not touch. */
	@Override
	public Collection<BlockPos> links() {
		List<BlockPos> out = new ArrayList<>();
		if (level == null) {
			return out;
		}
		for (Direction side : Direction.Plane.HORIZONTAL) {
			for (int dy : new int[] {-1, 1}) {
				BlockPos there = worldPosition.relative(side).above(dy);
				if (level.getBlockEntity(there) instanceof RailBlockEntity) {
					out.add(there);
				}
			}
		}
		return out;
	}

	@Override
	public void build(Circuit circuit) {
		node = circuit.node();
		double half = Haul.CONDUCTOR_OHMS_PER_METRE / 2;
		for (Direction face : Direction.values()) {
			// shrouded and flush: the track never shocks
			circuit.dcPort(worldPosition, face, node, half, false);
		}
		// its insulators leak a little to earth: and a stretch of unfed track, its joints far stiffer than the solver's own tiny leak, still solves
		circuit.resistor(node, 0, Haul.INSULATOR_OHMS);
		circuit.publish(worldPosition, node);
		for (BlockPos there : links()) {
			if (there.asLong() > worldPosition.asLong()) {
				circuit.span(worldPosition, there, Haul.CONDUCTOR_OHMS_PER_METRE * Math.sqrt(2));
			}
		}
		if (demand > 0 && level != null && level.getGameTime() < demandUntil) {
			// a constant-power load, sized from the last solve's volts, as Wildcatter's drives are
			double v = volts > Haul.TRACTION_VOLTS * Haul.FED_SHARE ? volts : Haul.TRACTION_VOLTS;
			circuit.resistor(node, 0, v * v / demand);
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = node < 0 ? 0 : circuit.dcVolts(node);
		solved = true;
		boolean fed = volts > Haul.TRACTION_VOLTS * Haul.FED_SHARE;
		if (fed != powered) {
			setPowered(fed);
			lightNeighbours();
		}
	}

	/** A mine light or block signal beside the rail follows whether it is fed. */
	private void lightNeighbours() {
		if (level == null) {
			return;
		}
		for (Direction direction : Direction.values()) {
			BlockPos beside = worldPosition.relative(direction);
			BlockState state = level.getBlockState(beside);
			if (state.getBlock() instanceof MineLightBlock) {
				MineLightBlock.refresh(level, beside, state);
			} else if (state.getBlock() instanceof BlockSignalBlock && state.getValue(BlockSignalBlock.LIT) != powered) {
				level.setBlock(beside, state.setValue(BlockSignalBlock.LIT, powered), 3);
			}
		}
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
