package dev.wildcatter.tailings.block;

import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.control.ControlDevice;
import dev.wildcatter.control.Remote;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.solver.Complex;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.pit.PitPower;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The pit switch house (docs/multiblocks/open-pit-p5-big-machines.md, P5a): a skid house on the pit's medium-voltage loop, the
 * world's medium voltage in on its north face, four breaker outlets on its south face. Each outlet is a breaker, a ground-check relay
 * and a trailing-cable coupler; a machine on the far end of its cable is a constant-power load behind the cable's resistance, all on
 * Wildcatter's solver, so a long cable to a big machine shows its drop.
 *
 * <p>Ground check: an outlet will not close until its cable reaches a machine that answers; a machine that stops answering (its cable
 * torn out, or it is gone) trips it. Each outlet's breaker trips at 1.25 x its machine's peak current held for 1 s; the incoming
 * breaker at 1.25 x the 2,000 A bus. A trip holds until it is reset on the panel. HAND: the panel opens and closes the outlets; OFF:
 * they are all held open; AUTO: each outlet follows its PLC CLOSE.
 */
public class PitSwitchHouseBlockEntity extends BlockEntity implements BigPartEntity, Inspectable, ControlDevice, CircuitPart, PitPanelMenu.Panel {
	/** A machine counts as on its cable while it has answered within this long. */
	public static final int ANSWER_TICKS = 40;
	private static final Conductor[] PHASES = {Conductor.A, Conductor.B, Conductor.C};
	/** An outlet's coupler on the south face, across the house (metres in its own frame) and up. */
	private static final double[] COUPLER_X = {0.45, 1.15, 1.85, 2.55};
	private static final double COUPLER_Y = 0.6;

	public enum State {
		OPEN, CLOSED, TRIPPED, GROUND_FAULT
	}

	/** One outlet: what is on its cable, what the machine last said, and what the solver measured. */
	public static final class Outlet {
		@Nullable UUID machine;
		String name = "";
		double cableM;
		boolean closed;
		boolean tripped;
		boolean groundFault;
		String why = "";
		long answered = -1000;
		double demand;
		double peak;
		double paid;
		double amps;
		double volts;
		double watts;
		double over;
		/** What a machine with PLC points on its cable last said (the dragline, P6): running, done, its cycles; not saved. */
		boolean plc;
		boolean running;
		boolean done;
		long cycles;
		final int[] far = new int[3];
		final int[] cable = {-1, -1, -1};
		final int[] star = {-1, -1, -1};
		int starNode = -1;

		public State state() {
			return groundFault ? State.GROUND_FAULT : tripped ? State.TRIPPED : closed ? State.CLOSED : State.OPEN;
		}
	}

	private final Outlet[] outlets = new Outlet[PitPower.OUTLETS];
	/** Each outlet's CLOSE, then each outlet's machine's RUN (P6). */
	private final Remote remote = new Remote(PitPower.OUTLETS * 2);
	private boolean grown;
	private boolean busTripped;
	private double busOver;
	private double busAmps;
	private double busVolts;
	private boolean forceSupply;
	/** Just loaded: its machines get a fresh {@link #ANSWER_TICKS} to answer before the ground check trips. */
	private boolean loaded;
	private final int[] line = new int[3];
	private final int[] bus = new int[3];
	private final int[] incoming = {-1, -1, -1};

	public PitSwitchHouseBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.PIT_SWITCH_HOUSE_ENTITY, pos, state);
		for (int i = 0; i < outlets.length; i++) {
			outlets[i] = new Outlet();
		}
	}

	// ---- the outlets, for machines and tests --------------------------------------------------------------------------------------

	public Outlet outlet(int i) {
		return outlets[i];
	}

	public State state(int i) {
		return outlets[i].state();
	}

	/** The first outlet with nothing on its cable, or -1. */
	public int freeOutlet() {
		for (int i = 0; i < outlets.length; i++) {
			if (outlets[i].machine == null) {
				return i;
			}
		}
		return -1;
	}

	/** A cable from outlet {@code i} is coupled to {@code machine}; why not, or null. */
	public @Nullable String couple(int i, UUID machine, String name, double cableM) {
		Outlet o = outlets[i];
		if (o.machine != null && !o.machine.equals(machine)) {
			return "outlet " + (i + 1) + " already has a cable to " + o.name;
		}
		o.machine = machine;
		o.name = name;
		o.cableM = cableM;
		o.groundFault = false;
		o.answered = level == null ? -1000 : level.getGameTime();
		setChanged();
		CircuitManager.markDirty(level);
		return null;
	}

	/** The cable is taken off outlet {@code i}: it opens. */
	public void uncouple(int i) {
		Outlet o = outlets[i];
		o.machine = null;
		o.name = "";
		o.closed = false;
		o.groundFault = false;
		o.plc = o.running = o.done = false;
		o.amps = o.volts = o.watts = 0;
		setChanged();
		if (level != null) {
			CircuitManager.markDirty(level);
		}
	}

	/**
	 * A machine on outlet {@code i} answers, each tick: what it draws now and at its peak, the cable's length and how much is paid out.
	 * It reads back {@link #live} and {@link #volts}.
	 */
	public void report(int i, UUID machine, double demandWatts, double peakWatts, double cableM, double paid) {
		Outlet o = outlets[i];
		if (!machine.equals(o.machine)) {
			return;
		}
		o.answered = level == null ? 0 : level.getGameTime();
		o.demand = Math.max(0, demandWatts);
		o.peak = Math.max(1, peakWatts);
		o.cableM = cableM;
		o.paid = paid;
	}

	/**
	 * A machine with PLC points answers on its cable's control pair (the dragline, P6): whether it runs, whether its job is done, and the
	 * cycles it has worked, ever.
	 */
	public void reportPlc(int i, UUID machine, boolean running, boolean done, long cycles) {
		Outlet o = outlets[i];
		if (!machine.equals(o.machine)) {
			return;
		}
		o.plc = true;
		o.running = running;
		o.done = done;
		o.cycles = cycles;
	}

	/** The PLC's RUN for the machine on outlet {@code i}: on or off while the selector is AUTO and the command fresh; null otherwise. */
	public @Nullable Boolean runCommand(int i) {
		if (level == null || !remote.auto() || !remote.fresh(PitPower.OUTLETS + i, level.getGameTime())) {
			return null;
		}
		return remote.on(PitPower.OUTLETS + i, level.getGameTime());
	}

	/** The machine's cable has torn out or been broken: a ground fault, which trips the outlet, and the cable is off it. */
	public void torn(int i, String why) {
		uncouple(i);
		outlets[i].groundFault = true;
		outlets[i].why = why;
	}

	private boolean answering(Outlet o) {
		return o.machine != null && level != null && level.getGameTime() - o.answered <= ANSWER_TICKS;
	}

	/** Closes outlet {@code i}; why not, or null. */
	public @Nullable String close(int i) {
		Outlet o = outlets[i];
		if (o.tripped || o.groundFault) {
			return "outlet " + (i + 1) + " is " + o.state() + ": reset it first";
		}
		if (remote.hoa() == dev.wildcatter.control.Hoa.OFF) {
			return "the selector is OFF";
		}
		if (o.machine == null || !answering(o)) {
			return "outlet " + (i + 1) + ": GROUND CHECK: no machine on its cable";
		}
		if (busTripped) {
			return "the incoming breaker is TRIPPED: reset it first";
		}
		if (!o.closed) {
			o.closed = true;
			o.over = 0;
			setChanged();
			CircuitManager.markDirty(level);
		}
		return null;
	}

	public void open(int i) {
		if (outlets[i].closed) {
			outlets[i].closed = false;
			setChanged();
			CircuitManager.markDirty(level);
		}
	}

	/** Resets every trip and ground fault, and the incoming breaker; the outlets stay open. */
	public String reset() {
		for (Outlet o : outlets) {
			o.tripped = false;
			o.groundFault = false;
			o.over = 0;
			o.why = "";
		}
		busTripped = false;
		busOver = 0;
		setChanged();
		CircuitManager.markDirty(level);
		return "reset: every outlet open and ready to close";
	}

	/** True while the machine on outlet {@code i} has power: closed, fed, and at least 90 % of the nominal volts at its end. */
	public boolean live(int i) {
		Outlet o = outlets[i];
		return o.state() == State.CLOSED && fed() && PitPower.enough(o.volts, Voltage.MEDIUM.lineToLine());
	}

	/** Line-to-line volts at the far end of outlet {@code i}'s cable, at the last solve. */
	public double volts(int i) {
		return outlets[i].volts;
	}

	public double amps(int i) {
		return outlets[i].amps;
	}

	public double watts(int i) {
		return outlets[i].watts;
	}

	/** Why the machine on outlet {@code i} has no power, for its readout; empty when it has. */
	public String noPower(int i) {
		Outlet o = outlets[i];
		return switch (o.state()) {
			case GROUND_FAULT -> "GROUND FAULT at the switch house" + (o.why.isEmpty() ? "" : ": " + o.why);
			case TRIPPED -> "TRIPPED at the switch house";
			case OPEN -> "OUTLET " + (i + 1) + " OPEN at the switch house";
			case CLOSED -> !fed() ? "NO POWER: nothing on the switch house's " + Voltage.MEDIUM.label() + " in" + (busTripped ? " (incoming TRIPPED)" : "")
					: o.volts < 1 ? "OUTLET " + (i + 1) + " CLOSING"
					: !PitPower.enough(o.volts, Voltage.MEDIUM.lineToLine()) ? String.format(Locale.ROOT, "LOW VOLTAGE: %.0f V", o.volts) : "";
		};
	}

	/** True while the medium voltage is on its bus. */
	public boolean fed() {
		return !busTripped && busVolts > Voltage.MEDIUM.lineToLine() * 0.5;
	}

	public boolean busTripped() {
		return busTripped;
	}

	/** Test hook: an ideal medium-voltage supply on its incoming side, as if the pit's loop reached it. */
	public void forceSupply(boolean on) {
		forceSupply(on, 0.01);
	}

	/** Test hook: the same supply behind {@code ohms} a phase (a weak feed, for LOW VOLTAGE). */
	public void forceSupply(boolean on, double ohms) {
		forceSupply = on;
		supplyOhms = ohms;
		CircuitManager.markDirty(level);
	}

	private double supplyOhms = 0.01;

	// ---- where its couplers are -------------------------------------------------------------------------------------------------

	/** Outlet {@code i}'s coupler, in the world: on the south face of the house, turned with it. */
	public static Vec3 coupler(BlockPos main, BlockState state, int i) {
		return world(main, state, COUPLER_X[Math.max(0, Math.min(3, i))], COUPLER_Y, 6.0);
	}

	/** A point given in the part's own frame (metres from its main cell's north-west bottom corner), in the world. */
	public static Vec3 world(BlockPos main, BlockState state, double x, double y, double z) {
		double qx = x - 0.5, qz = z - 0.5;
		for (int t = 0; t < BigParts.turns(state); t++) {
			double tmp = qx;
			qx = -qz;
			qz = tmp;
		}
		return new Vec3(main.getX() + 0.5 + qx, main.getY() + y, main.getZ() + 0.5 + qz);
	}

	// ---- ticking ------------------------------------------------------------------------------------------------------------------

	public static void tick(Level level, BlockPos pos, BlockState state, PitSwitchHouseBlockEntity house) {
		if (!(level instanceof ServerLevel)) {
			return;
		}
		long now = level.getGameTime();
		if (house.loaded) {
			house.loaded = false;
			for (Outlet o : house.outlets) {
				o.answered = now;
			}
		}
		for (int i = 0; i < house.outlets.length; i++) {
			Outlet o = house.outlets[i];
			// ground check: a closed outlet whose machine has stopped answering trips
			if (o.closed && !house.answering(o)) {
				o.closed = false;
				o.groundFault = true;
				o.why = "its machine stopped answering on the cable";
				house.setChanged();
				CircuitManager.markDirty(level);
			}
			switch (house.remote.hoa()) {
				case OFF -> house.open(i);
				case AUTO -> {
					if (house.remote.on(i, now)) {
						if (!o.closed) {
							house.close(i);
						}
					} else {
						house.open(i);
					}
				}
				default -> {
				}
			}
		}
	}

	// ---- the circuit --------------------------------------------------------------------------------------------------------------

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	/** The medium voltage comes in on the north face's cells. */
	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return voltage == Voltage.MEDIUM && role != Conductor.N && role != Conductor.G && face == PartFrame.world(getBlockState(), Direction.NORTH)
				&& PartFrame.local(worldPosition, getBlockState(), cell)[2] == 0;
	}

	@Override
	public void build(Circuit circuit) {
		BlockState state = getBlockState();
		Direction north = PartFrame.world(state, Direction.NORTH);
		for (int k = 0; k < 3; k++) {
			line[k] = circuit.node();
			bus[k] = circuit.node();
			incoming[k] = busTripped ? -1 : circuit.resistor(line[k], bus[k], dev.wildcatter.Tuning.CONTACT_OHMS);
		}
		if (busTripped) {
			for (int k = 0; k < 3; k++) {
				circuit.resistor(bus[k], bus[(k + 1) % 3], 1e6);   // a dead bus still has its nodes tied, so it solves
			}
		}
		for (BlockPos cell : BigParts.cells(worldPosition, state)) {
			if (PartFrame.local(worldPosition, state, cell)[2] == 0) {
				for (int k = 0; k < 3; k++) {
					circuit.port(cell, north, PHASES[k], Voltage.MEDIUM, line[k], dev.wildcatter.Tuning.CONTACT_OHMS / 2, false);
				}
			}
		}
		if (forceSupply) {
			int neutral = circuit.node();
			circuit.ground(neutral);
			double phase = Voltage.MEDIUM.lineToLine() / Math.sqrt(3);
			for (int k = 0; k < 3; k++) {
				int internal = circuit.node();
				circuit.source(internal, neutral, phase, dev.wildcatter.circuit.ThreePhaseSource.DEGREES[k]);
				circuit.resistor(internal, line[k], supplyOhms);
			}
		}
		for (Outlet o : outlets) {
			java.util.Arrays.fill(o.cable, -1);
			java.util.Arrays.fill(o.star, -1);
			o.starNode = -1;
			if (o.state() != State.CLOSED) {
				continue;
			}
			double ohms = Math.max(1e-4, PitPower.cableOhms(o.cableM));
			for (int k = 0; k < 3; k++) {
				o.far[k] = circuit.node();
				o.cable[k] = circuit.resistor(bus[k], o.far[k], ohms);
			}
			if (o.demand > 1 && answering(o)) {
				// constant power, from the volts it saw last (as Wildcatter's drives)
				double nominal = Voltage.MEDIUM.lineToLine();
				double at = o.volts > nominal * 0.5 ? o.volts : nominal;
				double z = at * at / (o.demand / PitPower.POWER_FACTOR);
				o.starNode = circuit.node();
				for (int k = 0; k < 3; k++) {
					o.star[k] = circuit.impedance(o.far[k], o.starNode, z * PitPower.POWER_FACTOR, z * Math.sqrt(1 - PitPower.POWER_FACTOR * PitPower.POWER_FACTOR));
				}
			}
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		double seconds = ticks / 20.0;
		busVolts = (circuit.across(bus[0], bus[1]) + circuit.across(bus[1], bus[2]) + circuit.across(bus[2], bus[0])) / 3.0;
		busAmps = 0;
		boolean changed = false;
		for (Outlet o : outlets) {
			if (o.cable[0] < 0) {
				o.amps = o.volts = o.watts = 0;
				continue;
			}
			o.volts = (circuit.across(o.far[0], o.far[1]) + circuit.across(o.far[1], o.far[2]) + circuit.across(o.far[2], o.far[0])) / 3.0;
			o.amps = 0;
			o.watts = 0;
			for (int k = 0; k < 3; k++) {
				o.amps = Math.max(o.amps, circuit.currentPhasor(o.cable[k]).abs());
				if (o.star[k] >= 0) {
					Complex i = circuit.currentPhasor(o.star[k]);
					o.watts += circuit.acrossPhasor(o.far[k], o.starNode).times(i.conjugate()).re();
				}
			}
			busAmps += o.amps;
			o.over = PitPower.over(o.over, o.amps, PitPower.tripAmps(o.peak, Voltage.MEDIUM.lineToLine()), seconds);
			if (PitPower.trips(o.over)) {
				o.tripped = true;
				o.closed = false;
				o.why = String.format(Locale.ROOT, "%.0f A for %.1f s", o.amps, o.over);
				o.over = 0;
				changed = true;
			}
		}
		busOver = PitPower.over(busOver, busAmps, PitPower.busTripAmps(), seconds);
		if (PitPower.trips(busOver)) {
			busTripped = true;
			busOver = 0;
			changed = true;
		}
		if (changed) {
			setChanged();
			CircuitManager.markDirty(level);
		}
	}

	// ---- the PLC ------------------------------------------------------------------------------------------------------------------

	@Override
	public String deviceName() {
		return "Pit switch house";
	}

	@Override
	public List<Point> points() {
		List<Point> out = new ArrayList<>();
		for (int i = 1; i <= PitPower.OUTLETS; i++) {
			out.add(new Point("OUTLET " + i + " CLOSE", true));
			out.add(new Point("OUTLET " + i + " CLOSED", false));
			out.add(new Point("OUTLET " + i + " TRIPPED", false));
			out.add(new Point("OUTLET " + i + " AMPS 4-20 mA, 1,200 A", false, Signal.MA));
		}
		out.add(new Point("AUTO", false));
		// each outlet's machine, on its cable's control pair (P6): after the house's own points, so theirs keep their numbers
		for (int i = 1; i <= PitPower.OUTLETS; i++) {
			out.add(new Point("OUTLET " + i + " MACHINE RUN", true));
			out.add(new Point("OUTLET " + i + " MACHINE RUNNING", false));
			out.add(new Point("OUTLET " + i + " MACHINE DONE", false));
			out.add(new Point("OUTLET " + i + " MACHINE CYCLES", false, Signal.PULSE));
			out.add(new Point("OUTLET " + i + " MACHINE POWER 4-20 mA, its peak", false, Signal.MA));
		}
		return out;
	}

	/** The first of the machines' points, and how many each outlet has. */
	public static final int MACHINE_POINTS = PitPower.OUTLETS * 4 + 1;
	public static final int POINTS_EACH = 5;

	/** The outlet whose machine point {@code point} is, and which of its five (0 RUN, 1 RUNNING, 2 DONE, 3 CYCLES, 4 POWER); null for the house's own. */
	private static int @Nullable [] machinePoint(int point) {
		if (point < MACHINE_POINTS || point >= MACHINE_POINTS + PitPower.OUTLETS * POINTS_EACH) {
			return null;
		}
		return new int[] {(point - MACHINE_POINTS) / POINTS_EACH, (point - MACHINE_POINTS) % POINTS_EACH};
	}

	/** True while a machine answering with PLC points is on outlet {@code i}. */
	private boolean plcOn(Outlet o) {
		return o.plc && answering(o);
	}

	@Override
	public long readPulses(int point) {
		int[] m = machinePoint(point);
		return m != null && m[1] == 3 && plcOn(outlets[m[0]]) ? outlets[m[0]].cycles : 0;
	}

	@Override
	public boolean readPoint(int point) {
		int[] m = machinePoint(point);
		if (m != null) {
			Outlet o = outlets[m[0]];
			return plcOn(o) && (m[1] == 1 ? o.running : m[1] == 2 && o.done);
		}
		if (point == PitPower.OUTLETS * 4) {
			return remote.auto();
		}
		Outlet o = outlets[Math.min(PitPower.OUTLETS - 1, point / 4)];
		return switch (point % 4) {
			case 1 -> o.state() == State.CLOSED;
			case 2 -> o.tripped || o.groundFault;
			default -> false;
		};
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point < PitPower.OUTLETS * 4 && point % 4 == 0) {
			remote.command(point / 4, on, gameTime);
		}
		int[] m = machinePoint(point);
		if (m != null && m[1] == 0) {
			remote.command(PitPower.OUTLETS + m[0], on, gameTime);
		}
	}

	@Override
	public double readMa(int point) {
		if (point < PitPower.OUTLETS * 4 && point % 4 == 3) {
			return PitPower.ampsMa(outlets[point / 4].amps);
		}
		int[] m = machinePoint(point);
		if (m != null && m[1] == 4) {
			Outlet o = outlets[m[0]];
			return plcOn(o) ? 4 + 16 * Math.min(1.03, o.watts / Math.max(1, o.peak)) : 0;
		}
		return 0;
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		setChanged();
		return remote.cycle();
	}

	// ---- the panel ----------------------------------------------------------------------------------------------------------------

	@Override
	public PitPanelMenu.Kind kind() {
		return PitPanelMenu.Kind.SWITCH_HOUSE;
	}

	@Override
	public int setting(int i) {
		return i < outlets.length && outlets[i].state() == State.CLOSED ? 1 : 0;
	}

	@Override
	public void set(int i, int value, Player player) {
		if (i >= outlets.length) {
			return;
		}
		if (!remote.hand()) {
			player.sendOverlayMessage(Component.literal("Pit Switch House: the selector is " + remote.hoa().name() + ": HAND to work it here"));
			return;
		}
		String why = value == 1 ? close(i) : null;
		if (value == 0) {
			open(i);
		}
		player.sendOverlayMessage(Component.literal("Pit Switch House: " + (why != null ? why : "outlet " + (i + 1) + " " + outlets[i].state())));
	}

	@Override
	public void action(int a, Player player) {
		if (a == 0) {
			player.sendOverlayMessage(Component.literal("Pit Switch House: " + reset()));
		}
	}

	@Override
	public String status() {
		StringBuilder out = new StringBuilder(remote.hoa().name()).append(busTripped ? " IN TRIP" : fed() ? "" : " NO MV");
		for (int i = 0; i < outlets.length; i++) {
			Outlet o = outlets[i];
			out.append(" | ").append(i + 1).append(' ').append(o.machine == null ? "-" : shortName(o.name)).append(' ').append(switch (o.state()) {
				case OPEN -> "OPEN";
				case CLOSED -> String.format(Locale.ROOT, "%.0fA %.0fkW", o.amps, o.watts / 1000);
				case TRIPPED -> "TRIP";
				case GROUND_FAULT -> "GND";
			});
			if (o.machine != null) {
				out.append(String.format(Locale.ROOT, " %.0f/%.0fm", o.paid, o.cableM));
			}
		}
		return out.length() > PitPanelMenu.STATUS_CHARS ? out.substring(0, PitPanelMenu.STATUS_CHARS) : out.toString();
	}

	private static String shortName(String name) {
		String[] words = name.split(" ");
		return words.length == 0 ? name : words[words.length - 1].toUpperCase(Locale.ROOT);
	}

	@Override
	public boolean stillValid(Player player) {
		return !isRemoved() && player.distanceToSqr(Vec3.atCenterOf(worldPosition)) < 32 * 32;
	}

	// ---- the readout -------------------------------------------------------------------------------------------------------------

	@Override
	public Component inspect() {
		double nominal = Voltage.MEDIUM.lineToLine();
		StringBuilder out = new StringBuilder(String.format(Locale.ROOT, "Pit Switch House (%s in, %s): bus %.0f V, %.0f A of %.0f%s", Voltage.MEDIUM.label(),
				remote.hoa().name(), busVolts, busAmps, PitPower.BUS_AMPS, busTripped ? "; incoming breaker TRIPPED" : ""));
		for (int i = 0; i < outlets.length; i++) {
			Outlet o = outlets[i];
			out.append(String.format(Locale.ROOT, "\nOutlet %d: %s", i + 1, o.state()));
			if (o.machine == null) {
				out.append(", no cable");
				continue;
			}
			out.append(String.format(Locale.ROOT, ", %s; %.0f A, %.0f kW, %.0f V at the machine (%.0f %%); cable %.0f of %.0f m paid out; trips at %.0f A for 1 s%s",
					o.name, o.amps, o.watts / 1000, o.volts, nominal <= 0 ? 0 : o.volts / nominal * 100, o.paid, o.cableM, PitPower.tripAmps(o.peak, nominal),
					o.why.isEmpty() ? "" : " (" + o.why + ")"));
		}
		return Component.literal(out.toString());
	}

	// ---- saving ------------------------------------------------------------------------------------------------------------------

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
		output.putBoolean("bus_tripped", busTripped);
		remote.save(output);
		for (int i = 0; i < outlets.length; i++) {
			Outlet o = outlets[i];
			output.putString("machine" + i, o.machine == null ? "" : o.machine.toString());
			output.putString("name" + i, o.name);
			output.putDouble("cable" + i, o.cableM);
			output.putBoolean("closed" + i, o.closed);
			output.putBoolean("tripped" + i, o.tripped);
			output.putBoolean("fault" + i, o.groundFault);
			output.putString("why" + i, o.why);
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		busTripped = input.getBooleanOr("bus_tripped", false);
		remote.load(input);
		for (int i = 0; i < outlets.length; i++) {
			Outlet o = outlets[i];
			String id = input.getStringOr("machine" + i, "");
			o.machine = id.isEmpty() ? null : UUID.fromString(id);
			o.name = input.getStringOr("name" + i, "");
			o.cableM = input.getDoubleOr("cable" + i, 0);
			o.closed = input.getBooleanOr("closed" + i, false);
			o.tripped = input.getBooleanOr("tripped" + i, false);
			o.groundFault = input.getBooleanOr("fault" + i, false);
			o.why = input.getStringOr("why" + i, "");
		}
		loaded = true;
	}
}
