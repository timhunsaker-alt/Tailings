package dev.wildcatter.tailings.block;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.DriveLoad;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.haul.Haul;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The traction rectifier (docs/multiblocks/haulage.md): 480 V three-phase in, 250 V DC out to the conductor rail, 300 kW. On
 * Wildcatter's DC solver it is a 250 V source behind its internal resistance, its minus on the running rails (earth), its plus on
 * every rail or switch touching it; the track carries it on. Its AC side is a Wildcatter variable load whose draw follows the DC
 * watts. Its DC breaker trips at 1.25 times its rated current held for 1 s; a click resets it.
 */
public class RectifierBlockEntity extends BlockEntity implements CircuitPart, Inspectable, BigPartEntity {
	private static final double POWER_FACTOR = 0.95;

	private boolean grown;
	private final DriveLoad ac = new DriveLoad(Voltage.INDUSTRIAL, POWER_FACTOR, 0);
	private boolean forced;
	private boolean acLive;
	private boolean tripped;
	private double over;
	private int plus = -1;
	private int branch = -1;
	private double volts;
	private double amps;

	public RectifierBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.RECTIFIER, pos, state);
	}

	public static void tick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, RectifierBlockEntity rectifier) {
		boolean big = rectifier.tooBig();
		if (big && !rectifier.warned && !level.isClientSide()) {
			// said once, to whoever is near, as it happens: the track is dead and nothing else says why
			for (net.minecraft.world.entity.player.Player player : level.players()) {
				if (player.blockPosition().closerThan(pos, 64)) {
					player.sendSystemMessage(Component.literal("Traction rectifier at " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + ": " + tooBigWarning()));
				}
			}
		}
		rectifier.warned = big;
	}

	private boolean warned;

	/** The game test runs it without building a grid: its DC side is on whatever the AC side does. */
	public void forceRunning() {
		forced = true;
	}

	public boolean powered() {
		return !tripped && (forced || acLive);
	}

	public boolean tripped() {
		return tripped;
	}

	public double volts() {
		return volts;
	}

	public double amps() {
		return amps;
	}

	/** Closes the tripped DC breaker again. */
	public boolean reset() {
		if (!tripped) {
			return false;
		}
		tripped = false;
		over = 0;
		setChanged();
		return true;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	/** Its AC terminal box is on the west face of its main cell, turned with it. */
	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && face == TailingsPartBlock.boxFace(getBlockState()) && voltage == ac.voltage() && role != Conductor.N;
	}

	private Circuit built;
	private long builtAt;

	/** True when its track's circuit has more nodes than Wildcatter solves: nothing on it is simulated (R5b). */
	public boolean tooBig() {
		return built != null && level != null && level.getGameTime() - builtAt <= 40 && built.nodes() > dev.wildcatter.Tuning.MAX_CIRCUIT_NODES;
	}

	/** What the player is told when its circuit is too big. */
	public static String tooBigWarning() {
		return "TRACK TOO LONG: this circuit has more than " + dev.wildcatter.Tuning.MAX_CIRCUIT_NODES
				+ " nodes (a node a metre of track), more than the grid solver takes, so it is dead; give the far end its own rectifier, on track not joined to this";
	}

	@Override
	public void build(Circuit circuit) {
		built = circuit;
		builtAt = level == null ? 0 : level.getGameTime();
		ac.setDemand(volts * amps / Haul.RECTIFIER_EFFICIENCY);
		ac.build(circuit, worldPosition, List.of(TailingsPartBlock.boxFace(getBlockState())));
		plus = circuit.node();
		branch = -1;
		if (powered()) {
			int inside = circuit.node();
			circuit.source(inside, 0, Haul.TRACTION_VOLTS);
			branch = circuit.resistor(inside, plus, Haul.RECTIFIER_OHMS);
		}
		List<BlockPos> cells = BigParts.cells(worldPosition, getBlockState());
		for (BlockPos cell : cells) {
			for (Direction face : Direction.values()) {
				BlockPos there = cell.relative(face);
				var block = level.getBlockState(there).getBlock();
				if (!cells.contains(there) && (block == TailingsBlocks.MINE_RAIL || block == TailingsBlocks.MINE_SWITCH)) {
					circuit.dcPort(cell, face, plus, Haul.CONDUCTOR_OHMS_PER_METRE / 2, false);
				}
			}
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		ac.apply(circuit, ticks);
		acLive = ac.volts() > 1 && !ac.undervoltage();
		if (branch < 0) {
			volts = 0;
			amps = 0;
			return;
		}
		volts = circuit.dcVolts(plus);
		amps = Math.abs(circuit.dcAmps(branch));
		if (amps > Haul.TRIP_SHARE * Haul.RECTIFIER_AMPS) {
			over += ticks / 20.0;
			if (over >= Haul.TRIP_SECONDS) {
				tripped = true;
				over = 0;
				setChanged();
			}
		} else {
			over = 0;
		}
	}

	@Override
	public Component inspect() {
		String dc = tooBig() ? tooBigWarning() : tripped ? "DC breaker TRIPPED on overload: click to reset"
				: powered() ? String.format("%.0f V DC, %.0f A, %.0f kW to the track", volts, amps, volts * amps / 1000)
				: "no 480 V in";
		return Component.literal("Traction rectifier (300 kW, 250 V DC): " + dc + "; AC " + ac.describe());
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
		output.putBoolean("tripped", tripped);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		tripped = input.getBooleanOr("tripped", false);
	}
}
