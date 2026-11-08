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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The cable junction on Wildcatter's grid: a 4.16 kV load whose draw is the TBM's at the far end of its trailing cable (its drives
 * are variable-frequency: a Wildcatter {@link DriveLoad} at power factor 0.95, proposed). The machine says what it draws each tick.
 */
public class CableJunctionBlockEntity extends BlockEntity implements CircuitPart, Inspectable, dev.wildcatter.control.ControlDevice {
	private final dev.wildcatter.control.Remote remote = new dev.wildcatter.control.Remote(1);
	/** The machine on its cable, which says so each tick: the junction stands still, and is how a control cable reaches a machine that moves. */
	private TbmCore machine;
	private long machineTick = -100;
	private boolean goPending;

	/** The machine reports each tick; {@code press} is its own Go button, so a GO from here does what a hand on the cab does. */
	public void report(TbmCore core, Level at, java.util.function.IntConsumer press) {
		machine = core;
		machineTick = at.getGameTime();
		long now = at.getGameTime();
		// go on the command's rise, as its cab's button: a machine that stopped itself, at its target or for want of stock, stays stopped until told again
		if (goPending && !core.running()) {
			press.accept(10);
		} else if (!remote.hand() && !remote.on(0, now) && core.running()) {
			press.accept(10);   // a stop, or a PLC gone quiet
		}
		goPending = false;
	}

	private TbmCore machine() {
		return level != null && level.getGameTime() - machineTick <= 40 ? machine : null;
	}

	@Override
	public String deviceName() {
		return "TBM, through its cable junction";
	}

	@Override
	public java.util.List<Point> points() {
		return java.util.List.of(new Point("GO", true), new Point("RUNNING", false), new Point("AUTO", false), new Point("ADVANCE PULSES", false, Signal.PULSE), new Point("CABLE LEFT 4-20 mA, 400 m", false, Signal.MA), new Point("QUENCH TANK 4-20 mA", false, Signal.MA));
	}

	@Override
	public boolean readPoint(int point) {
		TbmCore core = machine();
		return point == 1 ? core != null && core.running() : point == 2 && remote.auto();
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == 0 && remote.command(0, on, gameTime)) {
			goPending = true;
		}
	}

	@Override
	public long readPulses(int point) {
		TbmCore core = machine();
		return core == null ? 0 : core.drill().advanceCount();
	}

	@Override
	public double readMa(int point) {
		TbmCore core = machine();
		if (core == null) {
			return 0;   // no machine reporting: an open loop, as a real one with no signal
		}
		return 4 + 16 * Math.min(1.03, point == 4 ? core.cableLeft() / 400.0 : core.tank() / (double) TbmCore.TANK_MAX);
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		setChanged();
		return remote.cycle();
	}

	@Override
	protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
		super.saveAdditional(output);
		remote.save(output);
	}

	@Override
	protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
		super.loadAdditional(input);
		remote.load(input);
	}

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
