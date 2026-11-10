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
 * <p>
 * The reel gantry's transformer is a real part of the same circuit: three windings (Wildcatter's {@link Circuit#transformer}, the
 * 150 kVA nameplate's impedance) from the medium-voltage side the cable brings in to an industrial-voltage secondary, its neutrals
 * grounded, and the pump's and conveyor's motors a load on that secondary. Their current flows through it and shows on the
 * junction's 4.16 kV side. The transformer is built here, at the cable's fixed end, because a circuit part is a block and the gantry
 * moves; the trailing cable between them is not modelled.
 */
public class CableJunctionBlockEntity extends BlockEntity implements CircuitPart, Inspectable, dev.wildcatter.control.ControlDevice {
	private final dev.wildcatter.control.Remote remote = new dev.wildcatter.control.Remote(3);
	/** The machine on its cable, which says so each tick: the junction stands still, and is how a control cable reaches a machine that moves. */
	private TbmCore machine;
	private long machineTick = -100;
	private boolean goPending;

	/** The machine reports each tick; {@code press} is its own Go button, so a GO from here does what a hand on the cab does. */
	public void report(TbmCore core, Level at, java.util.function.IntConsumer press) {
		machine = core;
		machineTick = at.getGameTime();
		long now = at.getGameTime();
		program(core, at, now);
		// go on the command's rise, as its cab's button: a machine that stopped itself, at its target or for want of stock, stays stopped until told again
		if (goPending && !core.running()) {
			press.accept(10);
		} else if (!remote.hand() && !remote.on(0, now) && core.running()) {
			press.accept(10);   // a stop, or a PLC gone quiet
		}
		goPending = false;
	}

	/**
	 * The course from the PLC, while the machine stands, in AUTO, on fresh loops: TARGET Y and FLAT LENGTH set the program as the cab's
	 * screen does, before a GO in the same scan starts it. While it drives, the course it started on holds. A target that would move floor
	 * already cut is held to what it may be (PlcScale.allowedTarget); heading and grade are not signals: it bores the way it was built, at
	 * 1 in 2.
	 */
	private void program(TbmCore core, Level at, long now) {
		if (core.running()) {
			return;
		}
		double target = remote.fraction(1, now, -1);
		if (target >= 0) {
			var drill = core.drill();
			core.program().setTargetY(dev.wildcatter.tailings.plant.PlcScale.allowedTarget(dev.wildcatter.tailings.plant.PlcScale.targetY(target), drill.advanceCount(),
					drill.declining(), drill.floorY(), core.program().targetY(), at.getMinY() + 1));
		}
		double flat = remote.fraction(2, now, -1);
		if (flat >= 0) {
			core.program().setFlatLength(dev.wildcatter.tailings.plant.PlcScale.flatLength(flat));
		}
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
		return java.util.List.of(new Point("GO", true), new Point("RUNNING", false), new Point("AUTO", false), new Point("ADVANCE PULSES", false, Signal.PULSE), new Point("CABLE LEFT 4-20 mA, 400 m", false, Signal.MA), new Point("QUENCH TANK 4-20 mA", false, Signal.MA),
				new Point("TARGET Y 4-20 mA, -64 to 320", true, Signal.MA), new Point("FLAT LENGTH 4-20 mA, 400 m", true, Signal.MA), new Point("TARGET Y SET 4-20 mA", false, Signal.MA),
				new Point("FLAT LENGTH SET 4-20 mA", false, Signal.MA), new Point("AIR AT BACK-UP 4-20 mA, 15 m3/s", false, Signal.MA));
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
		return switch (point) {
			case 4 -> 4 + 16 * Math.min(1.03, core.cableLeft() / 400.0);
			case 8 -> dev.wildcatter.tailings.plant.PlcScale.ma(core.program().targetY(), dev.wildcatter.tailings.plant.PlcScale.TARGET_Y_LOW, dev.wildcatter.tailings.plant.PlcScale.TARGET_Y_HIGH);
			case 9 -> dev.wildcatter.tailings.plant.PlcScale.ma(core.program().flatLength(), 0, dev.wildcatter.tailings.plant.PlcScale.FLAT_HIGH);
			case 10 -> dev.wildcatter.tailings.plant.PlcScale.ma(Math.max(0, core.air()), 0, PlantBlockEntity.VENT_FLOW_HIGH);
			default -> 4 + 16 * Math.min(1.03, core.tank() / (double) TbmCore.TANK_MAX);
		};
	}

	@Override
	public void commandMa(int point, double milliamps, long gameTime) {
		if (point == 6 || point == 7) {
			remote.commandMa(point - 5, milliamps, gameTime);
		}
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

	/** The reel gantry's transformer: 150 kVA (proposed, tunnel-boring-machine.md), 4 % impedance (proposed, typical of the size). */
	public static final double TRANSFORMER_VA = 150_000;
	private static final double TRANSFORMER_PERCENT_Z = 4.0;
	/** The pump's and conveyor's induction motors, started across the line (proposed). */
	private static final double AUX_POWER_FACTOR = 0.85;
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};
	private final int[] high = new int[3];
	private final int[] low = new int[3];
	private final int[] winding = {-1, -1, -1};
	private final int[] auxBranch = {-1, -1, -1};
	private int auxStar = -1;
	private double auxDemand;
	private double lowVolts;
	private double auxAmps;
	private double auxWatts;
	private double highAmps;

	public CableJunctionBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.CABLE_JUNCTION_ENTITY, pos, state);
	}

	/** The machine draws {@code watts} through the junction for the next second. */
	public void draw(double watts) {
		draw(watts, 0);
	}

	/** The beam draws {@code beam} watts at 4.16 kV and the pump and conveyor {@code aux} through the reel gantry's transformer, for the next second. */
	public void draw(double beam, double aux) {
		demand = Math.max(0, beam);
		auxDemand = Math.max(0, aux);
		until = level == null ? 0 : level.getGameTime() + 20;
	}

	/** True while the transformer's secondary holds at least 80 % of its rated volts: the pump and conveyor can run. */
	public boolean auxLive() {
		return lowVolts > Voltage.INDUSTRIAL.lineToLine() * 0.8;
	}

	/** The secondary's line-to-line volts, at the last solve. */
	public double transformerVolts() {
		return lowVolts;
	}

	/** The pump's and conveyor's current on the secondary, and the watts they take, at the last solve. */
	public double auxAmps() {
		return auxAmps;
	}

	public double auxWatts() {
		return auxWatts;
	}

	/** The transformer's primary current, on the 4.16 kV side. */
	public double transformerPrimaryAmps() {
		return highAmps;
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
		buildTransformer(circuit, drawing ? auxDemand : 0);
	}

	/** Wye-wye, both neutrals grounded, from the world's own medium and industrial voltages; its secondary load a constant-power star. */
	private void buildTransformer(Circuit circuit, double aux) {
		Voltage hv = Voltage.MEDIUM, lv = Voltage.INDUSTRIAL;
		double ratio = hv.nominal() / lv.nominal();
		double zBase = hv.nominal() * hv.nominal() / (TRANSFORMER_VA / 3);
		double reactance = TRANSFORMER_PERCENT_Z / 100.0 * zBase;
		double ohms = reactance / dev.wildcatter.Tuning.TRANSFORMER_X_OVER_R;
		int highNeutral = circuit.node(), lowNeutral = circuit.node();
		for (int k = 0; k < 3; k++) {
			high[k] = circuit.node();
			low[k] = circuit.node();
			winding[k] = circuit.transformer(high[k], highNeutral, low[k], lowNeutral, ratio, ohms, reactance);
		}
		circuit.ground(highNeutral);
		circuit.ground(lowNeutral);
		for (Direction face : Direction.values()) {
			for (int k = 0; k < 3; k++) {
				circuit.port(worldPosition, face, PHASES[k], hv, high[k], dev.wildcatter.Tuning.CONTACT_OHMS / 2, false);
			}
		}
		java.util.Arrays.fill(auxBranch, -1);
		auxStar = -1;
		if (aux > 1) {
			double at = lowVolts > lv.lineToLine() * 0.5 ? lowVolts : lv.lineToLine();   // constant power, from the volts it saw last
			double z = at * at / (aux / AUX_POWER_FACTOR);
			auxStar = circuit.node();
			for (int k = 0; k < 3; k++) {
				auxBranch[k] = circuit.impedance(low[k], auxStar, z * AUX_POWER_FACTOR, z * Math.sqrt(1 - AUX_POWER_FACTOR * AUX_POWER_FACTOR));
			}
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		load.apply(circuit, ticks);
		live = load.volts() > 1 && !load.undervoltage();
		lowVolts = (circuit.across(low[0], low[1]) + circuit.across(low[1], low[2]) + circuit.across(low[2], low[0])) / 3.0;
		highAmps = 0;
		for (int k = 0; k < 3; k++) {
			highAmps = Math.max(highAmps, circuit.transformerCurrent(winding[k]));
		}
		auxAmps = 0;
		auxWatts = 0;
		for (int k = 0; k < 3; k++) {
			if (auxBranch[k] >= 0) {
				dev.wildcatter.solver.Complex i = circuit.currentPhasor(auxBranch[k]);
				auxWatts += circuit.acrossPhasor(low[k], auxStar).times(i.conjugate()).re();
				auxAmps = Math.max(auxAmps, i.abs());
			}
		}
	}

	@Override
	public Component inspect() {
		return Component.literal("Cable junction (" + Voltage.MEDIUM.label() + " to the TBM's trailing cable): " + (live ? "live" : "no " + Voltage.MEDIUM.label() + " in") + "; " + load.describe()
				+ String.format("; reel gantry transformer (%s / %s, 150 kVA): %.0f V, %.0f A, %.0f kW to the pump and conveyor", Voltage.MEDIUM.label(), Voltage.INDUSTRIAL.label(), lowVolts, auxAmps, auxWatts / 1000));
	}
}
