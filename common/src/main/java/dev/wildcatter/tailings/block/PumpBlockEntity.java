package dev.wildcatter.tailings.block;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.tailings.TailingsBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** The 75 kW, 480 V dewatering pump. The cab will not advance unless this motor is running. */
public class PumpBlockEntity extends BlockEntity implements CircuitPart, Inspectable {
	public static final int WATTS = 75_000;

	private final Motor motor = new Motor(true, Voltage.INDUSTRIAL, WATTS);
	private boolean demand;
	private boolean forced;

	public PumpBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.PUMP, pos, state);
	}

	public boolean powered() {
		return forced || motor.output() > 0;
	}

	public void setDemand(boolean demand) {
		this.demand = demand;
	}

	public void forceRunning() {
		forced = true;
	}

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.INDUSTRIAL && role != Conductor.N && role != Conductor.G;
	}

	@Override
	public void build(Circuit circuit) {
		motor.build(circuit, worldPosition);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		motor.apply(circuit, ticks, demand);
	}

	@Override
	public Component inspect() {
		return Component.literal("Dewatering pump (75 kW): " + motor.describe());
	}
}
