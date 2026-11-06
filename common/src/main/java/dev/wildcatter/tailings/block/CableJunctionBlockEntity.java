package dev.wildcatter.tailings.block;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.DriveLoad;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.tailings.TailingsBlocks;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The cable junction on Wildcatter's grid: a 4.16 kV load whose draw is the TBM's at the far end of its trailing cable (its drives
 * are variable-frequency: a Wildcatter {@link DriveLoad} at power factor 0.95, proposed). The machine says what it draws each tick.
 */
public class CableJunctionBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	private static final double POWER_FACTOR = 0.95;

	private final DriveLoad load = new DriveLoad(Voltage.MEDIUM, POWER_FACTOR, 0);
	private double demand;
	private long until;
	private boolean live;

	public CableJunctionBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.CABLE_JUNCTION_ENTITY, pos, state);
	}

	/** The machine draws {@code watts} through the junction for the next second. */
	public void draw(double watts) {
		demand = Math.max(0, watts);
		until = level == null ? 0 : level.getGameTime() + 20;
	}

	/** True while 4.16 kV comes in. */
	public boolean live() {
		return live;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.MEDIUM && role != Conductor.N && role != Conductor.G;
	}

	@Override
	public void build(Circuit circuit) {
		boolean drawing = level != null && level.getGameTime() < until;
		load.setDemand(drawing ? demand : 0);
		load.build(circuit, worldPosition, List.of(Direction.values()));
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		load.apply(circuit, ticks);
		live = load.volts() > 1 && !load.undervoltage();
	}

	@Override
	public Component inspect() {
		return Component.literal("Cable junction (4.16 kV to the TBM's trailing cable): " + (live ? "live" : "no 4.16 kV in") + "; " + load.describe());
	}
}
