package dev.wildcatter.tailings.block;

import dev.wildcatter.Tuning;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.control.ControlDevice;
import dev.wildcatter.control.Remote;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.pit.Dispatch;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The haul dispatch office (docs/multiblocks/open-pit.md, P2b): a small building on the control cable that runs the pit's trucks by
 * radio, up to 2 km off. A survey book clicked on it adds the book's route as the next of its 8 routes; a truck whose route is the
 * same stakes is on it. For each route a PLC has RUN (off sends its trucks to the route's PARK stake), RUNNING, TRUCKS, LOADS and
 * TONNES; for the fleet FUEL LOW and BLOCKED.
 *
 * <p>Its selector: HAND runs every route, as with no office; OFF parks them all; AUTO runs a route while the PLC keeps commanding
 * its RUN, and parks it when the commands stop (a stopped PLC, a cut cable). With no power its radio is dead and its trucks stop
 * at their next stake until it comes back.
 */
public class HaulDispatchBlockEntity extends BlockEntity implements BigPartEntity, Inspectable, ControlDevice, CircuitPart {
	/** The offices loaded in each level, by main cell, for trucks to find. */
	private static final Map<ServerLevel, Set<BlockPos>> OFFICES = new WeakHashMap<>();

	/** A truck's last word: when, and whether it was working, low on fuel or blocked. */
	private record Report(long seen, boolean working, boolean fuelLow, boolean blocked) {
	}

	/** What a truck finds for its route: the office and the route's number, and whether the radio reaches it (why not, if not). */
	public record Link(HaulDispatchBlockEntity office, int route, @Nullable String noContact) {
		public boolean contact() {
			return noContact == null;
		}
	}

	private final Remote remote = new Remote(Dispatch.ROUTES);
	private final List<List<BlockPos>> routes = new ArrayList<>();
	private final List<Map<UUID, Report>> reports = new ArrayList<>();
	private final Dispatch.Hour[] hours = new Dispatch.Hour[Dispatch.ROUTES];
	private final long[] loads = new long[Dispatch.ROUTES];
	private boolean grown;
	private int hot = -1, neutral = -1;
	private double volts;
	private boolean forcePowered;
	private int refresh;

	public HaulDispatchBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.HAUL_DISPATCH_ENTITY, pos, state);
		for (int i = 0; i < Dispatch.ROUTES; i++) {
			routes.add(new ArrayList<>());
			reports.add(new HashMap<>());
			hours[i] = new Dispatch.Hour();
		}
	}

	// ---- finding an office -------------------------------------------------------------------------------------------------------

	/** The office with this route, preferring one whose radio reaches {@code at}; null if no office has it. */
	public static @Nullable Link find(ServerLevel level, List<BlockPos> stakes, Vec3 at) {
		Link best = null;
		for (BlockPos pos : List.copyOf(OFFICES.getOrDefault(level, Set.of()))) {
			if (!(level.getBlockEntity(pos) instanceof HaulDispatchBlockEntity office) || office.isRemoved()) {
				OFFICES.getOrDefault(level, new HashSet<>()).remove(pos);
				continue;
			}
			int route = office.routeOf(stakes);
			if (route < 0) {
				continue;
			}
			String why = null;
			if (!office.powered()) {
				why = "no word from dispatch: the office has no power";
			} else if (Math.hypot(at.x - pos.getX() - 0.5, at.z - pos.getZ() - 0.5) > Dispatch.RADIO_M) {
				why = "no word from dispatch: over 2 km from the office";
			}
			Link link = new Link(office, route, why);
			if (link.contact()) {
				return link;
			}
			if (best == null) {
				best = link;
			}
		}
		return best;
	}

	/** The route that is these stakes, either way along them, or -1. */
	public int routeOf(List<BlockPos> stakes) {
		if (stakes.isEmpty()) {
			return -1;
		}
		List<BlockPos> reversed = new ArrayList<>(stakes);
		java.util.Collections.reverse(reversed);
		for (int i = 0; i < Dispatch.ROUTES; i++) {
			List<BlockPos> route = routes.get(i);
			if (!route.isEmpty() && (route.equals(stakes) || route.equals(reversed))) {
				return i;
			}
		}
		return -1;
	}

	// ---- routes ------------------------------------------------------------------------------------------------------------------

	/** Adds the survey book's route as the next free route; what to tell the player. */
	public String addRoute(List<BlockPos> stakes) {
		if (stakes.size() < 3) {
			return "the survey book has no route of 3 stakes or more";
		}
		int already = routeOf(stakes);
		if (already >= 0) {
			return "that is already route " + (already + 1);
		}
		for (int i = 0; i < Dispatch.ROUTES; i++) {
			if (routes.get(i).isEmpty()) {
				routes.get(i).addAll(stakes);
				reports.get(i).clear();
				hours[i].clear();
				setChanged();
				return "route " + (i + 1) + ": " + stakes.size() + " stakes, " + stakes.get(0).toShortString() + " to " + stakes.get(stakes.size() - 1).toShortString();
			}
		}
		return "it runs " + Dispatch.ROUTES + " routes: sneak-click with a route's book to take one off";
	}

	/** Takes the survey book's route off; what to tell the player. */
	public String removeRoute(List<BlockPos> stakes) {
		int route = routeOf(stakes);
		if (route < 0) {
			return "the survey book's route is not one of its routes";
		}
		routes.get(route).clear();
		reports.get(route).clear();
		hours[route].clear();
		setChanged();
		return "route " + (route + 1) + " taken off: its trucks run on their own";
	}

	public List<BlockPos> route(int route) {
		return List.copyOf(routes.get(route));
	}

	/** Whether a route's trucks are to run now: HAND runs, OFF parks, AUTO follows the PLC's RUN while it keeps coming. */
	public boolean runs(int route) {
		if (remote.hand()) {
			return true;
		}
		return level != null && remote.on(route, level.getGameTime());
	}

	/** Why a route is parked, for its trucks to read. */
	public String parkedWhy(int route) {
		if (remote.hoa() == dev.wildcatter.control.Hoa.OFF) {
			return "dispatch OFF";
		}
		return level != null && remote.fresh(route, level.getGameTime()) ? "RUN off" : "no RUN from the PLC";
	}

	/** A truck on a route says how it is, once a second. */
	public void report(int route, UUID truck, boolean working, boolean fuelLow, boolean blocked) {
		if (level != null) {
			reports.get(route).put(truck, new Report(level.getGameTime(), working, fuelLow, blocked));
		}
	}

	/** A truck on a route tipped a load. */
	public void tipped(int route, long kg) {
		if (level != null && kg > 0) {
			loads[route]++;
			hours[route].add(level.getGameTime(), kg);
			setChanged();
		}
	}

	// ---- what it knows -----------------------------------------------------------------------------------------------------------

	private List<Report> fresh(int route) {
		long now = level == null ? 0 : level.getGameTime();
		List<Report> out = new ArrayList<>();
		for (Report r : reports.get(route).values()) {
			if (now - r.seen() <= Dispatch.REPORT_FRESH_TICKS) {
				out.add(r);
			}
		}
		return out;
	}

	public int trucks(int route) {
		return fresh(route).size();
	}

	public boolean running(int route) {
		return fresh(route).stream().anyMatch(Report::working);
	}

	public long loads(int route) {
		return loads[route];
	}

	public double tonnesLastHour(int route) {
		return level == null ? 0 : hours[route].tonnes(level.getGameTime());
	}

	public boolean fuelLow() {
		for (int i = 0; i < Dispatch.ROUTES; i++) {
			if (fresh(i).stream().anyMatch(Report::fuelLow)) {
				return true;
			}
		}
		return false;
	}

	public boolean blocked() {
		for (int i = 0; i < Dispatch.ROUTES; i++) {
			if (fresh(i).stream().anyMatch(Report::blocked)) {
				return true;
			}
		}
		return false;
	}

	public boolean powered() {
		return forcePowered || volts >= Voltage.LOW.nominal() * Dispatch.DROPOUT;
	}

	/** Test hook: as if fed from the low tier. */
	public void forcePowered(boolean on) {
		forcePowered = on;
	}

	// ---- ticking -----------------------------------------------------------------------------------------------------------------

	public static void tick(Level level, BlockPos pos, BlockState state, HaulDispatchBlockEntity office) {
		if (level instanceof ServerLevel server) {
			OFFICES.computeIfAbsent(server, l -> new HashSet<>()).add(pos.immutable());
			// with routes to run, it keeps its own chunk working, as a running machine does
			if (office.refresh-- <= 0 && office.routes.stream().anyMatch(r -> !r.isEmpty())) {
				office.refresh = ChunkRunner.REFRESH_TICKS;
				ChunkRunner.refresh(server, pos);
			}
		}
	}

	/** Trucks find it as soon as it is in the world, not only once it has ticked (its chunk may not tick yet). */
	@Override
	public void clearRemoved() {
		super.clearRemoved();
		if (level instanceof ServerLevel server) {
			OFFICES.computeIfAbsent(server, l -> new HashSet<>()).add(worldPosition.immutable());
		}
	}

	@Override
	public void setRemoved() {
		if (level instanceof ServerLevel server) {
			Set<BlockPos> here = OFFICES.get(server);
			if (here != null) {
				here.remove(worldPosition);
			}
		}
		super.setRemoved();
	}

	// ---- the control cable -------------------------------------------------------------------------------------------------------

	@Override
	public String deviceName() {
		return "Haul dispatch";
	}

	@Override
	public List<Point> points() {
		List<Point> out = new ArrayList<>();
		for (int i = 1; i <= Dispatch.ROUTES; i++) {
			out.add(new Point("ROUTE " + i + " RUN", true));
			out.add(new Point("ROUTE " + i + " RUNNING", false));
			out.add(new Point("ROUTE " + i + " TRUCKS 4-20 mA, 0-" + Dispatch.TRUCKS_SPAN, false, Signal.MA));
			out.add(new Point("ROUTE " + i + " LOADS", false, Signal.PULSE));
			out.add(new Point(String.format(Locale.ROOT, "ROUTE %d TONNES 4-20 mA, 0-%,.0f t/h", i, Dispatch.TONNES_SPAN), false, Signal.MA));
		}
		out.add(new Point("FUEL LOW", false));
		out.add(new Point("BLOCKED", false));
		out.add(new Point("AUTO", false));
		return out;
	}

	@Override
	public boolean readPoint(int point) {
		if (point == Dispatch.AUTO) {
			return remote.auto();
		}
		if (!powered()) {
			return false;
		}
		if (point == Dispatch.FUEL_LOW) {
			return fuelLow();
		}
		if (point == Dispatch.BLOCKED) {
			return blocked();
		}
		int route = Dispatch.routeOf(point);
		return route >= 0 && Dispatch.kindOf(point) == Dispatch.RUNNING && running(route);
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		int route = Dispatch.routeOf(point);
		if (route >= 0 && Dispatch.kindOf(point) == Dispatch.RUN) {
			remote.command(route, on, gameTime);
		}
	}

	/** Its loops are powered by the office: with no power they read 0 mA, a failed loop. */
	@Override
	public double readMa(int point) {
		int route = Dispatch.routeOf(point);
		if (route < 0 || !powered()) {
			return 0;
		}
		return switch (Dispatch.kindOf(point)) {
			case Dispatch.TRUCKS -> Dispatch.trucksMa(trucks(route));
			case Dispatch.TONNES -> Dispatch.tonnesMa(tonnesLastHour(route));
			default -> 0;
		};
	}

	@Override
	public long readPulses(int point) {
		int route = Dispatch.routeOf(point);
		return route >= 0 && Dispatch.kindOf(point) == Dispatch.LOADS ? loads[route] : 0;
	}

	@Override
	public String cycleHoa(BlockPos cell) {
		setChanged();
		return remote.cycle();
	}

	// ---- power: a small single-phase load on the low tier -------------------------------------------------------------------------

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(worldPosition) && face == TailingsPartBlock.boxFace(getBlockState()) && voltage == Voltage.LOW && role != Conductor.G;
	}

	@Override
	public void build(Circuit circuit) {
		hot = circuit.node();
		neutral = circuit.node();
		double nominal = Voltage.LOW.nominal();
		circuit.resistor(hot, neutral, nominal * nominal / Dispatch.OFFICE_WATTS);
		Direction face = TailingsPartBlock.boxFace(getBlockState());
		circuit.hotPort(worldPosition, face, Voltage.LOW, hot, Tuning.CONTACT_OHMS / 2, false);
		circuit.port(worldPosition, face, Conductor.N, Voltage.LOW, neutral, Tuning.CONTACT_OHMS / 2, false);
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		volts = hot < 0 ? 0 : Math.abs(circuit.across(hot, neutral));
	}

	// ---- the readout -------------------------------------------------------------------------------------------------------------

	@Override
	public Component inspect() {
		StringBuilder out = new StringBuilder(String.format(Locale.ROOT, "Haul Dispatch Office (%.0f kW, %s): %s; %s", Dispatch.OFFICE_WATTS / 1000, Voltage.LOW.label(),
				remote.hoa().name(), powered() ? "radio on" : "NO POWER: radio dead, its trucks stop at their next stake"));
		boolean any = false;
		for (int i = 0; i < Dispatch.ROUTES; i++) {
			List<BlockPos> route = routes.get(i);
			if (route.isEmpty()) {
				continue;
			}
			any = true;
			out.append(String.format(Locale.ROOT, "\nRoute %d (%d stakes): %s, %s; %d trucks; %d loads; %,.0f t in the last hour", i + 1, route.size(),
					runs(i) ? "RUN" : "PARK (" + parkedWhy(i) + ")", running(i) ? "running" : "not running", trucks(i), loads[i], tonnesLastHour(i)));
		}
		if (!any) {
			out.append("\nNo routes: click it with a survey book to add the book's route");
		}
		if (fuelLow()) {
			out.append("\nFUEL LOW: a truck is under 15 %");
		}
		if (blocked()) {
			out.append("\nBLOCKED: a truck has stood for an obstacle over 30 s");
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
		remote.save(output);
		for (int i = 0; i < Dispatch.ROUTES; i++) {
			output.putString("route" + i, String.join(",", routes.get(i).stream().map(p -> Long.toString(p.asLong())).toList()));
			output.putLong("loads" + i, loads[i]);
			output.putString("hour" + i, hours[i].save());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		remote.load(input);
		for (int i = 0; i < Dispatch.ROUTES; i++) {
			routes.get(i).clear();
			String saved = input.getStringOr("route" + i, "");
			if (!saved.isEmpty()) {
				for (String part : saved.split(",")) {
					try {
						routes.get(i).add(BlockPos.of(Long.parseLong(part)));
					} catch (NumberFormatException ignored) {
						// a bad entry is dropped
					}
				}
			}
			loads[i] = input.getLongOr("loads" + i, 0);
			hours[i].load(input.getStringOr("hour" + i, ""));
		}
	}
}
