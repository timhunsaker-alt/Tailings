package dev.wildcatter.tailings.block;

import dev.wildcatter.block.Assayed;
import dev.wildcatter.block.FluidNode;
import dev.wildcatter.block.Lines;
import dev.wildcatter.block.WaterUser;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.Motor;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.fluid.Product;
import dev.wildcatter.fluid.SlurryAssay;
import dev.wildcatter.fluid.SlurryHold;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.block.stage.Stage;
import dev.wildcatter.tailings.plant.FilterCake;
import dev.wildcatter.tailings.plant.Machine;
import dev.wildcatter.tailings.plant.PlantRates;
import dev.wildcatter.tailings.plant.Reagent;
import dev.wildcatter.tailings.plant.ReagentStock;
import dev.wildcatter.tailings.plant.RockLoad;
import dev.wildcatter.tailings.rock.Rock;
import dev.wildcatter.tailings.rock.SizeClass;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Prediction;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * The main cell of a plant or dump-station part (docs/multiblocks/crushing-circuit.md, flotation-bank.md). Each part runs on its
 * own: its {@link Stage} moves rock or slurry while its motor runs, it has feed, and its product has somewhere to go. Dry rock is
 * held in kg ({@link RockLoad}); slurry for the pipe in a Wildcatter {@link SlurryHold}, with its assay. Items exist only where the
 * player hands rock in or takes a product out.
 */
public class PlantBlockEntity extends BlockEntity implements CircuitPart, Inspectable, WorldlyContainer, BigPartEntity, FluidNode, Assayed,
		WaterUser, dev.wildcatter.kinetic.Sounding, dev.wildcatter.control.ControlDevice, dev.wildcatter.control.Measurable {
	/** Its HAND / OFF / AUTO: HAND runs on its own work as it always has, OFF holds it, and AUTO runs it while a PLC keeps enabling it (Wildcatter's plc.md). */
	private final dev.wildcatter.control.Remote remote = new dev.wildcatter.control.Remote(9);
	private static final String[] SIDE_POINTS = {"NORTH", "EAST", "SOUTH", "WEST"};
	/** The sorter's points past the six every motor has: a gate a side, then a rock code a side. */
	private static final int GATES = 6, CODES = 10;

	private boolean plcAllows() {
		return remote.hand() || level != null && remote.on(0, level.getGameTime());
	}

	@Override
	public String deviceName() {
		return machine().name().replace('_', ' ');
	}

	/**
	 * Each motor-driven part: run, its feedback and its current. A sorter adds a diverter gate a side and the rock code each side takes;
	 * a part that holds slurry adds its level, flow and density. A part with no motor has no signals.
	 */
	@Override
	public java.util.List<Point> points() {
		if (motor == null) {
			return java.util.List.of();
		}
		java.util.List<Point> out = new java.util.ArrayList<>(java.util.List.of(new Point("RUN", true), new Point("RUNNING", false), new Point("STALLED", false), new Point("AUTO", false),
				new Point("AMPS 4-20 mA, 150 %", false, Signal.MA), new Point("FILL 4-20 mA", false, Signal.MA)));
		if (machine() == Machine.ORE_SORTER) {
			for (String side : SIDE_POINTS) {
				out.add(new Point("SHUT " + side + " GATE", true));
			}
			for (String side : SIDE_POINTS) {
				out.add(new Point(side + " TAKES 4-20 mA, ROCK CODE", true, Signal.MA));
			}
		} else if (machine() == Machine.XRT_SORTER) {
			// the X-ray sorter's threshold from the PLC, and a pulse a tonne each way (xrt-ore-sorter.md, L8)
			out.add(new Point("THRESHOLD 4-20 mA, 5 STEPS", true, Signal.MA));
			out.add(new Point("PRODUCT, A PULSE A TONNE", false, Signal.PULSE));
			out.add(new Point("WASTE, A PULSE A TONNE", false, Signal.PULSE));
			// reverse mode: keep what reads under the threshold (Tim, 2026-11-10, l8-xrt.md #5); in AUTO while it keeps coming
			out.add(new Point("REVERSE: KEEP UNDER THE THRESHOLD", true));
		} else if (machine() == Machine.VENT_FAN) {
			out.add(new Point(String.format("FLOW 4-20 mA, %.0f m3/s", VENT_FLOW_HIGH), false, Signal.MA));
		} else if (machine() == Machine.HOIST_HOUSE) {
			// the hoist's FILL is the headframe bin's level; a pulse each skip dumped (shaft-and-hoist.md, L4)
			out.add(new Point("SKIP DUMPED", false));
		} else if (machine() == Machine.TAILINGS_FILTER) {
			// the press's own points (open-pit-p4-dry-stack.md, P4; docs/questions/gaps2.md #2): CYCLE on for a second at each cake drop,
			// as the hoist's SKIP DUMPED (Tim, 2026-11-10), and FULL while its cake has nowhere to drop
			out.add(new Point("CYCLE, A SECOND A CAKE DROP", false));
			out.add(new Point("FULL", false));
		} else if (machine() == Machine.LOADOUT_FRAME) {
			// the load-out's own points (P4): the bin's level, the bin full, and the gate loading a truck
			out.add(new Point("LEVEL 4-20 mA", false, Signal.MA));
			out.add(new Point("FULL", false));
			out.add(new Point("LOADING", false));
		} else if (machine() == Machine.PASTE_PUMP) {
			// the paste pump's flow and line pressure (paste-backfill.md, L5)
			out.add(new Point(String.format("FLOW 4-20 mA, %.0f m3/h", PASTE_FLOW_HIGH), false, Signal.MA));
			out.add(new Point(String.format("PRESSURE 4-20 mA, %.0f MPa", PASTE_PRESSURE_HIGH / 1e6), false, Signal.MA));
		} else if (dms != null) {
			// the medium's density and a pulse a tonne floated (dense-medium-separation.md, L7)
			out.add(new Point(String.format(java.util.Locale.ROOT, "DENSITY 4-20 mA, SG %.1f-%.1f", dev.wildcatter.tailings.plant.DenseMedium.SG_LOW, dev.wildcatter.tailings.plant.DenseMedium.SG_HIGH), false, Signal.MA));
			out.add(new Point("FLOATS", false));
		} else if (dev.wildcatter.tailings.block.stage.SlurryGauge.gauged(machine())) {
			out.add(new Point("LEVEL 4-20 mA", false, Signal.MA));
			out.add(new Point(String.format("FLOW 4-20 mA, %.0f L/s", dev.wildcatter.tailings.plant.PlcScale.FLOW_HIGH), false, Signal.MA));
			out.add(new Point(String.format("DENSITY 4-20 mA, %.1f-%.1f kg/L", dev.wildcatter.tailings.plant.PlcScale.DENSITY_LOW, dev.wildcatter.tailings.plant.PlcScale.DENSITY_HIGH), false, Signal.MA));
		}
		return out;
	}

	@Override
	public boolean readPoint(int point) {
		if (machine() == Machine.TAILINGS_FILTER && point == 7) {
			return cakeOut > 0;
		}
		if (machine() == Machine.TAILINGS_FILTER && point == 6) {
			return level != null && level.getGameTime() - droppedAt < CYCLE_BIT_TICKS;
		}
		if (machine() == Machine.LOADOUT_FRAME && (point == 7 || point == 8)) {
			return point == 7 ? loadoutBinFull() : status().startsWith("loading");
		}
		return switch (point) {
			case 1 -> motor != null && motor.output() > 0;
			case 2 -> motor != null && motor.stalled();
			case 3 -> remote.auto();
			case 6 -> machine() == Machine.HOIST_HOUSE && dev.wildcatter.tailings.block.stage.Shaft.dumpedRecently(this);
			case 7 -> dms != null && level != null && dms.pulsing(level.getGameTime());
			default -> false;
		};
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		if (point == 0) {
			remote.command(0, on, gameTime);
		} else if (machine() == Machine.XRT_SORTER && point == 9) {
			remote.command(2, on, gameTime);
		} else if (machine() == Machine.ORE_SORTER && point >= GATES && point < CODES) {
			remote.command(point - GATES + 1, on, gameTime);
			sorter.setGate(point - GATES, remote.auto() && on);
		}
	}

	@Override
	public void commandMa(int point, double milliamps, long gameTime) {
		if (machine() == Machine.XRT_SORTER && point == 6) {
			remote.commandMa(1, milliamps, gameTime);
			return;
		}
		if (machine() == Machine.ORE_SORTER && point >= CODES && point < CODES + dev.wildcatter.tailings.haul.Sorter.SIDES) {
			remote.commandMa(point - CODES + 5, milliamps, gameTime);
			if (level != null) {
				routeBySignal(level.getGameTime());
			}
		}
	}

	/**
	 * The sorter's sides as its PLC says, each tick: a gate shut while its command keeps coming, and each side taking the rock its code
	 * names while in AUTO and the loop is fresh. Code 0 takes nothing of its own; a code with no rock, a stale loop or HAND or OFF leave
	 * the side as its screen set it.
	 */
	private void routeBySignal(long now) {
		for (int side = 0; side < dev.wildcatter.tailings.haul.Sorter.SIDES; side++) {
			sorter.setGate(side, remote.on(side + 1, now));
			double fraction = remote.fraction(side + 5, now, -1);
			int code = fraction < 0 ? -1 : dev.wildcatter.tailings.plant.PlcScale.code(4 + 16 * fraction);
			dev.wildcatter.tailings.rock.Rock rock = dev.wildcatter.tailings.plant.PlcScale.rock(code);
			if (code == 0) {
				sorter.choose(side, null);
			} else if (rock != null) {
				sorter.choose(side, Feeds.blockId(rock));
			} else {
				sorter.release(side);
			}
		}
	}

	@Override
	public double readMa(int point) {
		if (motor == null) {
			return 0;
		}
		if (machine() == Machine.PASTE_PUMP && (point == 6 || point == 7)) {
			dev.wildcatter.tailings.block.stage.Paste.Line line = dev.wildcatter.tailings.block.stage.Paste.line(this);
			return point == 6 ? dev.wildcatter.tailings.plant.PlcScale.ma(dev.wildcatter.tailings.block.stage.Paste.flowM3PerHour(this), 0, PASTE_FLOW_HIGH)
					: dev.wildcatter.tailings.plant.PlcScale.ma(line == null ? 0 : line.pressure, 0, PASTE_PRESSURE_HIGH);
		}
		if (point == 6 && machine() == Machine.LOADOUT_FRAME) {
			PlantBlockEntity bin = touching(Machine.LOADOUT_BIN);
			return 4 + 16 * Math.min(1.03, bin == null ? 0 : bin.dryKg / dev.wildcatter.tailings.pit.DryStack.BIN_KG);
		}
		if (point == 6 && dms != null) {
			return dev.wildcatter.tailings.plant.PlcScale.ma(dms.medium(), dev.wildcatter.tailings.plant.DenseMedium.SG_LOW, dev.wildcatter.tailings.plant.DenseMedium.SG_HIGH);
		}
		if (point == 6 && vent != null) {
			return dev.wildcatter.tailings.plant.PlcScale.ma(vent.air().fanFlow(), 0, VENT_FLOW_HIGH);
		}
		if (point >= 6 && dev.wildcatter.tailings.block.stage.SlurryGauge.gauged(machine())) {
			return switch (point) {
				case 6 -> 4 + 16 * Math.min(1.03, dev.wildcatter.tailings.block.stage.SlurryGauge.level(this));
				case 7 -> dev.wildcatter.tailings.plant.PlcScale.ma(flowLitres, 0, dev.wildcatter.tailings.plant.PlcScale.FLOW_HIGH);
				default -> dev.wildcatter.tailings.plant.PlcScale.ma(dev.wildcatter.tailings.block.stage.SlurryGauge.density(this), dev.wildcatter.tailings.plant.PlcScale.DENSITY_LOW,
						dev.wildcatter.tailings.plant.PlcScale.DENSITY_HIGH);
			};
		}
		if (point == 5 && machine() == Machine.HOIST_HOUSE) {
			return 4 + 16 * Math.min(1.03, dev.wildcatter.tailings.block.stage.Shaft.binLevel(this));
		}
		return 4 + 16 * Math.min(1.03, point == 4 ? motor.amps() / (motor.fullLoadAmps() * 1.5) : hold.total() / dev.wildcatter.tailings.plant.PlantRates.BELT_HOLD_KG);
	}

	/** True for a load-out frame whose bin on it holds no more than a block short of its 400 t, as the bin reads FULL. */
	private boolean loadoutBinFull() {
		PlantBlockEntity bin = touching(Machine.LOADOUT_BIN);
		return bin != null && bin.dryKg >= dev.wildcatter.tailings.pit.DryStack.BIN_KG - dev.wildcatter.tailings.pit.PitMass.DRY_TAILS_KG;
	}

	/** The X-ray sorter's PRODUCT and WASTE counts: a pulse a tonne. */
	@Override
	public long readPulses(int point) {
		if (xrt == null) {
			return 0;
		}
		return point == 7 ? xrt.productPulses() : point == 8 ? xrt.wastePulses() : 0;
	}

	/** An X-ray sorter's own state (xrt-ore-sorter.md, L8); null for every other part. */
	private final dev.wildcatter.tailings.plant.XrtSorting.State xrt;

	public dev.wildcatter.tailings.plant.XrtSorting.State xrt() {
		return xrt;
	}

	/** True while a PLC in AUTO sets the X-ray sorter's threshold. */
	public boolean xrtByPlc() {
		return level != null && remote.fraction(1, level.getGameTime(), -1) >= 0;
	}

	/** The X-ray sorter's threshold step now: the PLC's while its loop is fresh in AUTO, else its screen's. */
	public int xrtStep() {
		double fraction = level == null ? -1 : remote.fraction(1, level.getGameTime(), -1);
		return fraction < 0 ? xrt.step() : dev.wildcatter.tailings.plant.XrtSorting.stepOf(fraction);
	}

	/** True while a PLC in AUTO sets the X-ray sorter's REVERSE. */
	public boolean xrtReverseByPlc() {
		return level != null && remote.auto() && remote.fresh(2, level.getGameTime());
	}

	/** The X-ray sorter in reverse now: the PLC's REVERSE while it keeps coming in AUTO, else its screen's. */
	public boolean xrtReverse() {
		return xrtReverseByPlc() ? remote.on(2, level.getGameTime()) : xrt.reverse();
	}

	/** A sneaking click on the X-ray sorter steps its screen's threshold, LOW to HIGH, then LOW to HIGH in reverse, and round again. */
	public void stepThreshold() {
		xrt.nextStep();
		say("set to " + dev.wildcatter.tailings.plant.XrtSorting.describe(xrt.step(), xrt.reverse()) + (xrtByPlc() || xrtReverseByPlc() ? "; the PLC sets it while in AUTO" : ""));
		setChanged();
	}

	/** Litres a second of slurry leaving by pipe over the last second, and what has left in this one. */
	private double flowLitres;
	private double flowCounting;

	/** Its discharge flow, L/s, over the last second. */
	public double flow() {
		return flowLitres;
	}

	/** Litres a stage sent on by itself (the paste pump's push to its outlet), counted in its flow. */
	public void countFlow(double litres) {
		flowCounting += litres;
	}

	/** The paste pump's FLOW spans 0-40 m3/h and its PRESSURE 0-10 MPa (paste-backfill.md, L5). */
	public static final double PASTE_FLOW_HIGH = 40, PASTE_PRESSURE_HIGH = 10_000_000;

	@Override
	public String cycleHoa(BlockPos cell) {
		setChanged();
		return remote.cycle();
	}

	/** What it can report: its motor, and the rock it holds (on a conveyor drive, the belt's load: a belt scale). */
	@Override
	public double measure(Quantity quantity) {
		return switch (quantity) {
			case AMPS -> motor == null ? Double.NaN : motor.amps();
			case SPEED -> motor == null ? Double.NaN : motor.output() * 100;
			case WEIGHT -> hold.total();
			case LEVEL -> dev.wildcatter.tailings.block.stage.SlurryGauge.gauged(machine()) ? dev.wildcatter.tailings.block.stage.SlurryGauge.level(this) * 100
					: hold.total() / dev.wildcatter.tailings.plant.PlantRates.BELT_HOLD_KG * 100;
			case FLOW -> vent != null ? vent.air().fanFlow() : dev.wildcatter.tailings.block.stage.SlurryGauge.gauged(machine()) || machine() == Machine.PASTE_PUMP ? flowLitres : Double.NaN;
			case DENSITY -> dms != null ? dms.medium() : dev.wildcatter.tailings.block.stage.SlurryGauge.gauged(machine()) ? dev.wildcatter.tailings.block.stage.SlurryGauge.density(this) : Double.NaN;
			default -> Double.NaN;
		};
	}
	/** How long a part keeps sounding after its last work, so a stage that works in bursts does not flutter. */
	private static final int SOUND_HOLD_TICKS = 20;
	/** Ticks a started motor still runs on with no work ({@link dev.wildcatter.tailings.plant.PlantRates#MOTOR_RUN_ON_SECONDS}); not saved. */
	private int runOn;
	private boolean sounding;
	private int soundHold;
	private boolean grown;
	private final NonNullList<ItemStack> slots = NonNullList.withSize(7, ItemStack.EMPTY);
	/** The pressure filter's cake, paid out as nuggets. */
	private final FilterCake cake = new FilterCake();
	/**
	 * Heavy mineral concentrate (dry-mill.md, L6): a filter's scavenger share not yet bagged or sent to its dryer, a dryer's moist feed,
	 * a separator's dry feed. Empty in every part of a plant without the dry mill.
	 */
	private final dev.wildcatter.tailings.plant.HeavyMinerals hmc = new dev.wildcatter.tailings.plant.HeavyMinerals();
	/** A dryer's gas burnt so far, m³. */
	private double gasM3;
	private static final int LAST_REAGENT_SLOT = 6;
	private final ReagentStock reagents = new ReagentStock();
	private boolean selective;
	/** Dry rock waiting to be worked, or, in a conveyor drive, the load on its run. */
	private final RockLoad hold = new RockLoad();
	/** A conveyor drive's load, by where it rides along the run (the transit time to the head end). */
	private final dev.wildcatter.tailings.plant.BeltTravel travel = new dev.wildcatter.tailings.plant.BeltTravel();
	/** Rock worked but not yet paid out as an item (the screen's sand for its hopper). */
	private final RockLoad made = new RockLoad();
	/** What it puts on steel pipe. */
	private final SlurryHold out = new SlurryHold();
	/** The ball mill's cyclone underflow (dry kg with its assay, ground before new feed); a flotation part's inlet buffer; the pond's tails. */
	private final SlurryHold back = new SlurryHold();
	/** An ore bin's rock items, by charge, past the one in its intake slot. */
	private final java.util.Map<String, Integer> binned = new java.util.LinkedHashMap<>();
	/** An ore sorter's screen: the type each side takes, and the waste side. */
	private final dev.wildcatter.tailings.haul.Sorter sorter = new dev.wildcatter.tailings.haul.Sorter();
	private double water;
	/**
	 * The dry stack (open-pit-p4-dry-stack.md): a load-out bin's dry tails, or the cake in a tailings filter's chambers, kg; the
	 * filter's cake dropped and not yet placed; its time into its cycle in ticks; water waiting to go back on a water pipe, and the
	 * litres returned so far. Paste fill (paste-backfill.md, L5): {@code dryKg} is a paste filter's cake and a paste mixer's cement.
	 */
	private double dryKg;
	private double cakeOut;
	private double cycle;
	private double waterOut;
	private double waterReturned;
	private int cycles;
	/** When the press last dropped its cake, for its one-second CYCLE bit (not saved). */
	private long droppedAt = Long.MIN_VALUE / 2;
	/** A game test's shorter press cycle (not saved). */
	private int fast = 1;
	private Motor motor;
	/** A ventilation fan's duct line (ventilation.md); null for every other part. */
	private final @Nullable VentState vent;
	/** A DMS cyclone module's medium and counts (dense-medium-separation.md, L7); null for every other part. */
	private final dev.wildcatter.tailings.plant.@Nullable DmsState dms;
	/** A ventilation fan's FLOW signal spans 0-15 m³/s, its rating (ventilation.md). */
	public static final double VENT_FLOW_HIGH = 15;
	private boolean demand;
	private boolean forced;
	private boolean loading;
	private String status = "idle";
	/** A hoist house's drums, skips and their loads (shaft-and-hoist.md, L4). */
	private final dev.wildcatter.tailings.shaft.Winder winder = new dev.wildcatter.tailings.shaft.Winder();

	public dev.wildcatter.tailings.shaft.Winder winder() {
		return winder;
	}

	/** A conveyor drive's last scan of its run; a hoist house's last look at its shaft (not saved). */
	public @Nullable Object run;

	private static final List<Product> SLURRIES = List.of(Product.LEACH_SLURRY, Product.PREGNANT_LIQUOR, Product.MILL_DISCHARGE, Product.FINES_SLURRY, Product.CONDITIONED_SLURRY,
			Product.CONCENTRATE_SLURRY, Product.THICKENED_CONCENTRATE, Product.TAILS_SLURRY);

	public PlantBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.PLANT, pos, state);
		Machine machine = machine();
		if (machine.watts() > 0) {
			Voltage voltage = machine.supply() == Machine.Supply.MEDIUM ? Voltage.MEDIUM : Voltage.INDUSTRIAL;
			motor = new Motor(true, voltage, machine.watts());
		}
		vent = machine == Machine.VENT_FAN ? new VentState() : null;
		dms = machine == Machine.DMS_MODULE ? new dev.wildcatter.tailings.plant.DmsState() : null;
		xrt = machine == Machine.XRT_SORTER ? new dev.wildcatter.tailings.plant.XrtSorting.State() : null;
	}

	/** A DMS cyclone module's medium and counts, or null. */
	public dev.wildcatter.tailings.plant.@Nullable DmsState dms() {
		return dms;
	}

	/** A ventilation fan's duct line, or null. */
	public @Nullable VentState vent() {
		return vent;
	}

	public Machine machine() {
		return ((PlantMachineBlock) getBlockState().getBlock()).machine();
	}

	public Stage stage() {
		return Stage.of(machine());
	}

	public static void tick(Level level, BlockPos pos, BlockState state, PlantBlockEntity plant) {
		plant.serverTick();
	}

	private void serverTick() {
		demand = false;
		if (level.getGameTime() % 20 == 0) {
			flowLitres = flowCounting;
			flowCounting = 0;
		}
		if (machine() == Machine.ORE_SORTER) {
			routeBySignal(level.getGameTime());
		}
		intake();
		stage().tick(this);
		runOn = demand ? dev.wildcatter.tailings.plant.PlantRates.MOTOR_RUN_ON_SECONDS * 20 : Math.max(0, runOn - 1);
		soundHold = running() && demand ? SOUND_HOLD_TICKS : Math.max(0, soundHold - 1);
		boolean now = soundHold > 0;
		if (now != sounding) {
			sounding = now;
			level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), net.minecraft.world.level.block.Block.UPDATE_CLIENTS);
		}
	}

	@Override
	public net.minecraft.sounds.SoundEvent loop() {
		return sounding ? dev.wildcatter.tailings.TailingsSounds.loop(machine()) : null;
	}

	@Override
	public float soundPitch() {
		return 1F;
	}

	@Override
	public float soundVolume() {
		return sounding ? dev.wildcatter.tailings.TailingsSounds.volume(machine()) : 0;
	}

	@Override
	public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
		return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
	}

	/** All the client needs of a part: whether to sound. */
	@Override
	public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
		net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
		tag.putBoolean("sounding", sounding);
		return tag;
	}

	/** A hopper's item in the intake slot goes into the hold (or an ore bin's store) as soon as there is room. */
	private void intake() {
		ItemStack in = slots.get(0);
		if (machine() == Machine.PASTE_MIXER) {
			if (cement(in) && dryKg + dev.wildcatter.tailings.paste.PasteFill.CEMENT_BAG_KG <= dev.wildcatter.tailings.paste.PasteFill.CEMENT_HOLD_KG) {
				in.shrink(1);
				dryKg += dev.wildcatter.tailings.paste.PasteFill.CEMENT_BAG_KG;
				setChanged();
			}
			return;
		}
		if (machine() == Machine.FLUID_BED_DRYER) {
			if (hmcBag(in)) {
				binHmc(in);
			}
			return;
		}
		if (machine() == Machine.MEDIUM_RECOVERY) {
			if (ferrosilicon(in) && dryKg + dev.wildcatter.tailings.plant.DenseMedium.FESI_BAG_KG <= dev.wildcatter.tailings.plant.DenseMedium.FESI_HOLD_KG + 1e-6) {
				in.shrink(1);
				dryKg += dev.wildcatter.tailings.plant.DenseMedium.FESI_BAG_KG;
				setChanged();
			}
			return;
		}
		if (machine() == Machine.LOADOUT_BIN) {
			if (dryTails(in) && dryKg + dev.wildcatter.tailings.pit.PitMass.DRY_TAILS_KG <= dev.wildcatter.tailings.pit.DryStack.BIN_KG) {
				in.shrink(1);
				dryKg += dev.wildcatter.tailings.pit.PitMass.DRY_TAILS_KG;
				setChanged();
			}
			return;
		}
		Feeds.Charge charge = Feeds.parse(in);
		if (charge == null) {
			return;
		}
		if (machine() == Machine.ORE_BIN) {
			if (binCount() <= PlantRates.BIN_ITEMS) {
				binned.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(in.getItem()).toString(), in.getCount(), Integer::sum);
				slots.set(0, ItemStack.EMPTY);
				setChanged();
			}
		} else if (accepts(charge.rock(), charge.size()) && room() >= PlantRates.BLOCK_KG) {
			in.shrink(1);
			hold.add(charge.rock(), charge.size(), PlantRates.BLOCK_KG);
			setChanged();
		}
	}

	// ---- what stages use ---------------------------------------------------------------------------------------------------------

	/** The fraction of its rated rate the part runs at now: its motor's output (an unpowered part runs whenever it has work). */
	public double strength() {
		if (forced || motor == null) {
			return 1;
		}
		return motor.output();
	}

	public boolean running() {
		return forced || (motor != null && motor.output() > 0);
	}

	/** It has work: its motor should run. */
	public void want() {
		demand = true;
	}

	public void say(String status) {
		if (!status.equals(this.status)) {
			this.status = status;
			setChanged();
		}
	}

	/** True one tick in 20, spread over parts by position: for scans that need not run every tick. */
	public boolean scanDue() {
		return level != null && (level.getGameTime() + (worldPosition.asLong() & 15)) % 20 == 0;
	}

	public RockLoad hold() {
		return hold;
	}

	public RockLoad made() {
		return made;
	}

	public SlurryHold out() {
		return out;
	}

	public SlurryHold back() {
		return back;
	}

	public double water() {
		return water;
	}

	public double dryKg() {
		return dryKg;
	}

	public void setDryKg(double kg) {
		dryKg = Math.max(0, kg);
		setChanged();
	}

	public double cakeOut() {
		return cakeOut;
	}

	public void setCakeOut(double kg) {
		cakeOut = Math.max(0, kg);
		setChanged();
	}

	/** A filter's time into its cycle, in ticks. */
	public double cycle() {
		return cycle;
	}

	public void setCycle(double ticks) {
		cycle = ticks;
		setChanged();
	}

	/** Cake drops so far. */
	public int cycles() {
		return cycles;
	}

	public void countCycle() {
		cycles++;
		droppedAt = level == null ? 0 : level.getGameTime();
		setChanged();
	}

	/** The tailings filter's CYCLE stays on this long after a drop: a second, as the hoist's SKIP DUMPED. */
	public static final int CYCLE_BIT_TICKS = 20;

	/** A game test's shorter press cycle: the cycle runs {@code divisor} times as fast. */
	public void fastCycle(int divisor) {
		fast = Math.max(1, divisor);
	}

	public int fast() {
		return fast;
	}

	/** Water squeezed or settled out, to go back on a water pipe. */
	public void overflow(double litres) {
		waterOut += litres;
	}

	public double waterOut() {
		return waterOut;
	}

	public void returned(double litres, boolean lost) {
		waterOut = Math.max(0, waterOut - litres);
		waterReturned += lost ? 0 : litres;
		waterLost += lost ? litres : 0;
		setChanged();
	}

	/** Litres with no water pipe or user to take them, counted as lost (not saved). */
	public double waterLost() {
		return waterLost;
	}

	private double waterLost;

	/** Litres sent back on the water pipe so far. */
	public double waterReturned() {
		return waterReturned;
	}

	public void useWater(double litres) {
		water = Math.max(0, water - litres);
		setChanged();
	}

	public List<BlockPos> cells() {
		return BigParts.cells(worldPosition, getBlockState());
	}

	/** The faces of the cells that match {@code where} (own frame) on {@code localSide}. */
	public List<Lines.Face> faces(Predicate<int[]> where, Direction localSide) {
		return PartFrame.faces(worldPosition, getBlockState(), where, localSide);
	}

	/** Every outward face on {@code localSide}. */
	public List<Lines.Face> side(Direction localSide) {
		return PartFrame.side(worldPosition, getBlockState(), localSide);
	}

	/** How many kg of dry rock it can still take. */
	public double room() {
		return Math.max(0, stage().holdKg() - hold.total());
	}

	public boolean accepts(Rock rock, @Nullable SizeClass size) {
		return stage().accepts(rock, size);
	}

	/** Takes rock into its hold; the caller checked {@link #room} and {@link #accepts}. A conveyor drive takes it onto its run's tail end. */
	public void receive(RockLoad load) {
		receive(load, stage().dropAt(this, null));
	}

	/** Takes rock into its hold; on a conveyor drive's run it drops {@code toGo} metres short of the head end and rides from there. */
	public void receive(RockLoad load, double toGo) {
		if (machine() == Machine.CONVEYOR_DRIVE) {
			travel.match(hold.total());
			travel.add(load.total(), toGo);
		}
		hold.add(load);
		setChanged();
	}

	/** A conveyor drive's load, along its run. */
	public dev.wildcatter.tailings.plant.BeltTravel travel() {
		return travel;
	}

	/** A load-out frame's gate: the centre of its lane, at its floor (open-pit-p4-dry-stack.md). */
	public net.minecraft.world.phys.Vec3 loadoutGate() {
		List<BlockPos> cells = cells();
		double cx = 0, cz = 0;
		int lowest = Integer.MAX_VALUE;
		for (BlockPos c : cells) {
			cx += c.getX() + 0.5;
			cz += c.getZ() + 0.5;
			lowest = Math.min(lowest, c.getY());
		}
		return new net.minecraft.world.phys.Vec3(cx / cells.size(), lowest, cz / cells.size());
	}

	/** A part of kind {@code machine} touching this one, or null. */
	public @Nullable PlantBlockEntity touching(Machine machine) {
		List<BlockPos> mine = cells();
		for (BlockPos cell : mine) {
			for (Direction side : Direction.values()) {
				BlockPos next = cell.relative(side);
				if (!mine.contains(next) && BigParts.entity(level, next) instanceof PlantBlockEntity part && part.machine() == machine) {
					return part;
				}
			}
		}
		return null;
	}

	/** A part of kind {@code machine} right under this one's bottom, or null. */
	public @Nullable PlantBlockEntity under(Machine machine) {
		List<BlockPos> mine = cells();
		for (BlockPos cell : mine) {
			BlockPos below = cell.below();
			if (!mine.contains(below) && BigParts.entity(level, below) instanceof PlantBlockEntity part && part.machine() == machine) {
				return part;
			}
		}
		return null;
	}

	/** Where rock goes out of these faces: a belt run's load, or a part that takes it. */
	public interface Sink {
		/** How many kg of {@code what} it can take now (0 if it does not take all of it). */
		double room(RockLoad what);

		void put(RockLoad load);
	}

	/** The first place out of {@code faces} that takes rock: a moving belt, or a part whose feed face touches that face. */
	public @Nullable Sink sink(List<Lines.Face> faces) {
		for (Lines.Face face : faces) {
			Sink there = sinkAt(face.pipe(), face.side(), this);
			if (there != null) {
				return there;
			}
		}
		return null;
	}

	/**
	 * What takes rock at {@code pos}, coming in moving {@code into} (DOWN: dropped onto it from above), never {@code except}: a belt's
	 * run (through its drive) or a part, only through its feed face (crushing-circuit.md, "Nozzles and faces").
	 */
	public @Nullable Sink sinkAt(BlockPos pos, Direction into, @Nullable PlantBlockEntity except) {
		if (level.getBlockEntity(pos) instanceof BeltBlockEntity belt) {
			BlockPos at = belt.drive();
			if (at != null && level.getBlockEntity(at) instanceof PlantBlockEntity drive && drive != except) {
				double toGo = drive.stage().dropAt(drive, pos);
				return new Sink() {
					@Override
					public double room(RockLoad what) {
						return belt.moving() ? Math.max(0, PlantRates.BELT_HOLD_KG - drive.hold.total()) : 0;
					}

					@Override
					public void put(RockLoad load) {
						drive.receive(load, toGo);
					}
				};
			}
			return null;
		}
		if (BigParts.entity(level, pos) instanceof PlantBlockEntity part && part != except && part != this) {
			if (!part.stage().feeds(part, PartFrame.local(part.worldPosition, part.getBlockState(), pos), part.local(into.getOpposite()))) {
				return null;
			}
			return new Sink() {
				@Override
				public double room(RockLoad what) {
					boolean[] ok = {true};
					what.forEach((rock, size, kg) -> ok[0] &= part.accepts(rock, size));
					return ok[0] ? part.room() : 0;
				}

				@Override
				public void put(RockLoad load) {
					part.receive(load);
				}
			};
		}
		return null;
	}

	/** Puts a made item in the product slot, for a hopper under the part or a hand; false if it does not fit. */
	public boolean emit(ItemStack stack) {
		if (stack.isEmpty() || !roomFor(stack)) {
			return false;
		}
		add(stack);
		setChanged();
		return true;
	}

	public boolean roomForItem(ItemStack stack) {
		return roomFor(stack);
	}

	/** An ore bin's items, intake slot first: the count of each rock item id it holds. */
	public java.util.Map<String, Integer> binned() {
		return binned;
	}

	public int binCount() {
		int n = slots.get(0).getCount();
		for (int c : binned.values()) {
			n += c;
		}
		return n;
	}

	public NonNullList<ItemStack> slots() {
		return slots;
	}

	/** The game test turns the motors on without building a grid, or off again. */
	public void forceRunning() {
		forced = true;
	}

	public void stopForced() {
		forced = false;
	}

	public String status() {
		return status;
	}

	public ItemStack product() {
		return slots.get(1);
	}

	// ---- hands and hoppers -------------------------------------------------------------------------------------------------------

	private static boolean cement(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() == dev.wildcatter.tailings.TailingsItems.CEMENT;
	}

	private static boolean ferrosilicon(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() == dev.wildcatter.tailings.TailingsItems.FERROSILICON;
	}

	/** Steps a DMS cyclone module's medium one setting up, wrapping (a sneaking click); returns its status. */
	public String stepMedium() {
		if (dms != null) {
			say(String.format(java.util.Locale.ROOT, "the medium set to SG %.2f", dms.step()));
		}
		return status;
	}

	private static boolean dryTails(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() == TailingsBlocks.DRY_TAILS.asItem();
	}

	public boolean offer(ItemStack stack) {
		if (machine() == Machine.PASTE_MIXER) {
			// the mixer's hopper takes cement only, a 50 kg bag a click (paste-backfill.md, L5)
			if (!cement(stack)) {
				return false;
			}
			if (dryKg + dev.wildcatter.tailings.paste.PasteFill.CEMENT_BAG_KG > dev.wildcatter.tailings.paste.PasteFill.CEMENT_HOLD_KG) {
				say("the cement hopper is full");
				return false;
			}
			stack.shrink(1);
			dryKg += dev.wildcatter.tailings.paste.PasteFill.CEMENT_BAG_KG;
			setChanged();
			return true;
		}
		if (machine() == Machine.MEDIUM_RECOVERY) {
			// its make-up hopper takes ferrosilicon only, a 25 kg bag a click (dense-medium-separation.md, L7)
			if (!ferrosilicon(stack)) {
				return false;
			}
			if (dryKg + dev.wildcatter.tailings.plant.DenseMedium.FESI_BAG_KG > dev.wildcatter.tailings.plant.DenseMedium.FESI_HOLD_KG + 1e-6) {
				say("the ferrosilicon hopper is full");
				return false;
			}
			stack.shrink(1);
			dryKg += dev.wildcatter.tailings.plant.DenseMedium.FESI_BAG_KG;
			setChanged();
			return true;
		}
		if (hmcBag(stack) && machine() == Machine.FLUID_BED_DRYER) {
			if (!binHmc(stack)) {
				say(dev.wildcatter.tailings.item.HeavyMineralConcentrateItem.contents(stack).isEmpty() ? "a bag with no assay: nothing to dry" : "FULL: its bin holds " + (int) hmc.total() + " kg");
				return false;
			}
			return true;
		}
		if (hmcBag(stack) && dev.wildcatter.tailings.plant.DryMill.separates(machine())) {
			// dry-mill.md: only dried HMC separates; moist grains stick to the rolls
			say("WET: dry the concentrate in a fluid-bed dryer first");
			return false;
		}
		if (machine() == Machine.LOADOUT_BIN) {
			// the load-out takes dry tails only (open-pit-p4-dry-stack.md)
			if (!dryTails(stack)) {
				say("dry tails only: " + stack.getHoverName().getString() + " refused");
				return false;
			}
			if (dryKg + dev.wildcatter.tailings.pit.PitMass.DRY_TAILS_KG > dev.wildcatter.tailings.pit.DryStack.BIN_KG) {
				say("FULL");
				return false;
			}
			stack.shrink(1);
			dryKg += dev.wildcatter.tailings.pit.PitMass.DRY_TAILS_KG;
			setChanged();
			return true;
		}
		Reagent reagent = reagent(stack);
		if (reagent != null && dosed()) {
			for (int slot = 2; slot <= LAST_REAGENT_SLOT; slot++) {
				ItemStack there = slots.get(slot);
				if (there.isEmpty()) {
					setItem(slot, stack.split(1));
					return true;
				}
				if (ItemStack.isSameItemSameComponents(there, stack) && there.getCount() < there.getMaxStackSize()) {
					there.grow(1);
					stack.shrink(1);
					reagents.addKilogram(reagent);
					setChanged();
					return true;
				}
			}
			return false;
		}
		if (machine() == Machine.BYPRODUCT_PLANT) {
			if (!canPlaceItem(0, stack) || !(slots.get(0).isEmpty() || ItemStack.isSameItemSameComponents(slots.get(0), stack) && slots.get(0).getCount() < 64)) {
				return false;
			}
			if (slots.get(0).isEmpty()) {
				slots.set(0, stack.split(1));
			} else {
				slots.get(0).grow(1);
				stack.shrink(1);
			}
			setChanged();
			return true;
		}
		Feeds.Charge charge = Feeds.parse(stack);
		if (charge == null) {
			return false;
		}
		if (machine() == Machine.ORE_BIN) {
			if (binCount() >= PlantRates.BIN_ITEMS) {
				return false;
			}
			String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
			binned.merge(id, 1, Integer::sum);
			stack.shrink(1);
			setChanged();
			return true;
		}
		if (!accepts(charge.rock(), charge.size()) || room() < PlantRates.BLOCK_KG) {
			return false;
		}
		stack.shrink(1);
		hold.add(charge.rock(), charge.size(), PlantRates.BLOCK_KG);
		setChanged();
		return true;
	}

	/** The by-product plant's kg of each by-product made and not yet a whole item, and its work on the item at hand, in ticks. */
	private final java.util.Map<String, Double> byproducts = new java.util.TreeMap<>();
	private int working;

	public java.util.Map<String, Double> byproducts() {
		return byproducts;
	}

	public int working(int by) {
		working += by;
		if (by < 0) {
			working = 0;
		}
		return working;
	}

	/** The parts that pay out by the nuggets rule, with two output stacks. */
	private boolean pays() {
		return machine() == Machine.FILTER || machine() == Machine.ELECTROWINNING_BANK || machine() == Machine.BYPRODUCT_PLANT || dev.wildcatter.tailings.plant.DryMill.separates(machine());
	}

	/** The stacks it pays into: two, or a separator's four. */
	private int[] outputs() {
		return dev.wildcatter.tailings.plant.DryMill.separates(machine()) ? dev.wildcatter.tailings.block.stage.DryMillStages.OUTPUT_SLOTS : pays() ? new int[] {1, 2} : new int[] {1};
	}

	public boolean give(Player player) {
		boolean gave = false;
		for (int slot : outputs()) {
			ItemStack stack = slots.get(slot);
			if (!stack.isEmpty()) {
				player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
				slots.set(slot, ItemStack.EMPTY);
				gave = true;
			}
		}
		if (gave) {
			setChanged();
		}
		return gave;
	}

	public dev.wildcatter.tailings.haul.Sorter sorter() {
		return sorter;
	}

	private static final String[] SIDE_NAMES = {"north", "east", "south", "west"};

	/** A click on a sorter's side with a rock item sets that side to take it (a shortcut for its screen); false for anything else. */
	public boolean setSide(Direction face, ItemStack stack) {
		if (machine() != Machine.ORE_SORTER || !face.getAxis().isHorizontal() || Feeds.parse(stack) == null) {
			return false;
		}
		setSide((face.get2DDataValue() + 2) % 4, net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getHoverName().getString());
		return true;
	}

	/** Sets a sorter's side (0 north to 3 west) to take {@code itemId}, from its screen. */
	public void setSide(int side, String itemId, String name) {
		sorter.set(side, itemId);
		say("the " + SIDE_NAMES[side] + " side takes " + name);
		setChanged();
	}

	/** Clears a sorter's side: it takes nothing of its own. */
	public void clearSide(int side) {
		sorter.clear(side);
		say("the " + SIDE_NAMES[side] + " side takes nothing");
		setChanged();
	}

	/** Makes a sorter's side the waste side. */
	public void setWaste(int side) {
		sorter.setWaste(side);
		say("the " + SIDE_NAMES[side] + " side takes the waste");
		setChanged();
	}

	/** Sets the sorter's side facing {@code face} as the waste side. */
	public boolean setWasteSide(Direction face) {
		if (machine() != Machine.ORE_SORTER || !face.getAxis().isHorizontal()) {
			return false;
		}
		setWaste((face.get2DDataValue() + 2) % 4);
		return true;
	}

	/** A conditioning tank's setting: selective holds the sphalerite down with zinc sulfate (selective-flotation.md). */
	public boolean selective() {
		return selective;
	}

	public void toggleSelective() {
		selective = !selective;
		setChanged();
	}

	/** The parts that take reagents by hopper or hand. */
	private boolean dosed() {
		return machine() == Machine.CONDITIONER || machine() == Machine.ACTIVATION_TANK;
	}

	public ReagentStock reagents() {
		return reagents;
	}

	public FilterCake cake() {
		return cake;
	}

	public dev.wildcatter.tailings.plant.HeavyMinerals hmc() {
		return hmc;
	}

	/** A dryer's burner took {@code m3} of gas. */
	public void gasBurnt(double m3) {
		gasM3 += m3;
	}

	private static boolean hmcBag(ItemStack stack) {
		return !stack.isEmpty() && stack.getItem() == dev.wildcatter.tailings.TailingsItems.HEAVY_MINERAL_CONCENTRATE;
	}

	/** A dryer takes a bag of HMC into its bin while one fits; false if it does not. */
	private boolean binHmc(ItemStack stack) {
		dev.wildcatter.tailings.plant.HeavyMinerals bag = dev.wildcatter.tailings.item.HeavyMineralConcentrateItem.contents(stack);
		if (bag.isEmpty() || hmc.total() + bag.total() > dev.wildcatter.tailings.plant.DryMill.HOLD_KG) {
			return false;
		}
		hmc.add(bag);
		stack.shrink(1);
		setChanged();
		return true;
	}

	/** Puts {@code stack} into the first of {@code into} that takes it whole; false if none does. */
	public boolean emitTo(int[] into, ItemStack stack) {
		for (int slot : into) {
			ItemStack there = slots.get(slot);
			if (there.isEmpty()) {
				slots.set(slot, stack.copy());
				setChanged();
				return true;
			}
			if (ItemStack.isSameItemSameComponents(there, stack) && there.getCount() + stack.getCount() <= there.getMaxStackSize()) {
				there.grow(stack.getCount());
				setChanged();
				return true;
			}
		}
		return false;
	}

	/** How many of {@code like} are in these slots. */
	public int countIn(int[] in, ItemStack like) {
		int n = 0;
		for (int slot : in) {
			if (ItemStack.isSameItemSameComponents(slots.get(slot), like)) {
				n += slots.get(slot).getCount();
			}
		}
		return n;
	}

	/** Takes {@code count} of {@code like} out of these slots. */
	public void removeFrom(int[] in, ItemStack like, int count) {
		for (int slot : in) {
			if (count > 0 && ItemStack.isSameItemSameComponents(slots.get(slot), like)) {
				int take = Math.min(count, slots.get(slot).getCount());
				slots.get(slot).shrink(take);
				count -= take;
			}
		}
		setChanged();
	}

	/** Takes out the reagent items whose kilogram has been dosed, so the slots never fill with spent reagent. */
	public void trimReagents() {
		for (Reagent reagent : Reagent.values()) {
			int extra = -reagents.items(reagent);
			for (int slot = 2; slot <= LAST_REAGENT_SLOT; slot++) {
				if (reagent(slots.get(slot)) == reagent) {
					extra += slots.get(slot).getCount();
				}
			}
			for (int slot = 2; slot <= LAST_REAGENT_SLOT && extra > 0; slot++) {
				ItemStack there = slots.get(slot);
				if (reagent(there) == reagent) {
					int take = Math.min(extra, there.getCount());
					there.shrink(take);
					extra -= take;
				}
			}
		}
	}

	private static @Nullable Reagent reagent(ItemStack stack) {
		if (stack.isEmpty()) {
			return null;
		}
		String id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
		for (Reagent reagent : Reagent.values()) {
			if (reagent.itemId().equals(id)) {
				return reagent;
			}
		}
		return null;
	}

	private boolean roomFor(ItemStack stack) {
		ItemStack there = slots.get(1);
		if (there.isEmpty()) {
			return true;
		}
		return ItemStack.isSameItemSameComponents(there, stack) && there.getCount() + stack.getCount() <= there.getMaxStackSize();
	}

	private void add(ItemStack stack) {
		ItemStack there = slots.get(1);
		if (there.isEmpty()) {
			slots.set(1, stack);
		} else if (ItemStack.isSameItemSameComponents(there, stack)) {
			there.grow(stack.getCount());
		}
	}

	// ---- steel pipe and water ----------------------------------------------------------------------------------------------------

	/** {@code side} (in the world) seen in the part's own frame. */
	private Direction local(Direction side) {
		Direction out = side;
		for (int i = 0; side.getAxis().isHorizontal() && i < BigParts.turns(getBlockState()); i++) {
			out = out.getCounterClockWise();
		}
		return out;
	}

	@Override
	public @Nullable Product offers(BlockPos cell, Direction side) {
		return stage().outlet(this, PartFrame.local(worldPosition, getBlockState(), cell), local(side));
	}

	@Override
	public double available(BlockPos cell, Direction side, Product product, Pull pull) {
		return offers(cell, side) == product && pull != Pull.FLARE ? out.held(product) : 0;
	}

	@Override
	public double take(BlockPos cell, Direction side, Product product, double max, Pull pull) {
		if (available(cell, side, product, pull) <= 0) {
			return 0;
		}
		double got = out.give(product, max);
		flowCounting += got / dev.wildcatter.tailings.plant.PlcScale.density(product);
		setChanged();
		return got;
	}

	@Override
	public SlurryAssay assayOf(Product product) {
		return out.assayOf(product);
	}

	@Override
	public double waterLevel() {
		double hold = stage().waterHold();
		return hold <= 0 ? 1 : water / hold;
	}

	@Override
	public int addWater(int litres) {
		int fits = (int) Math.max(0, Math.min(litres, stage().waterHold() - water));
		water += fits;
		if (fits > 0) {
			setChanged();
		}
		return fits;
	}

	// ---- the motor ---------------------------------------------------------------------------------------------------------------

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	/** The motor box is on the west face of the main cell, turned with the part. */
	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return motor != null && cell.equals(worldPosition) && face == TailingsPartBlock.boxFace(getBlockState()) && voltage == motor.voltage()
				&& role != Conductor.N;
	}

	@Override
	public void build(Circuit circuit) {
		if (motor != null) {
			motor.build(circuit, worldPosition, List.of(TailingsPartBlock.boxFace(getBlockState())));
		}
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		if (motor != null) {
			// a started motor runs on through a gap in its feed instead of stopping and starting again on the next few kilograms
			boolean runningOn = runOn > 0 && motor.state() != dev.wildcatter.circuit.Motor.State.STOPPED;
			motor.apply(circuit, ticks, (demand || runningOn) && plcAllows());
		}
	}

	@Override
	public Component inspect() {
		String power = motor == null ? "no motor" : motor.describe();
		StringBuilder held = new StringBuilder();
		if (!hold.isEmpty()) {
			held.append(String.format("; holds %.0f kg of rock", hold.total()));
		}
		for (Product p : SLURRIES) {
			for (SlurryHold h : List.of(back, out)) {
				if (h.held(p) > 0.5 && (h == out || machine() != Machine.BALL_MILL)) {
					held.append(String.format("; %.0f kg of %s (%s)", h.held(p), p.label(), h.assayOf(p).describe()));
				}
			}
		}
		if (motor != null && dev.wildcatter.tailings.block.stage.SlurryGauge.gauged(machine())) {
			held.append(String.format("; slurry level %.0f %%, flow %.1f L/s, density %.2f kg/L", dev.wildcatter.tailings.block.stage.SlurryGauge.level(this) * 100, flowLitres,
					dev.wildcatter.tailings.block.stage.SlurryGauge.density(this)));
		}
		if (stage().waterHold() > 0) {
			held.append(String.format("; water %.0f/%.0f L", water, stage().waterHold()));
		}
		if (machine() == Machine.LOADOUT_BIN) {
			held.append(String.format("; holds %.1f of %.0f t of dry tails", dryKg / 1000, dev.wildcatter.tailings.pit.DryStack.BIN_KG / 1000));
		}
		if (machine() == Machine.TAILINGS_FILTER) {
			held.append(String.format("; %.1f t of cake in the press, %d drops", dryKg / 1000, cycles));
			if (cakeOut > 0) {
				held.append(String.format("; %.1f t of cake waiting to drop", cakeOut / 1000));
			}
		}
		if (waterReturned > 0) {
			held.append(String.format("; %.0f L of water returned", waterReturned));
		}
		if (!hmc.isEmpty()) {
			held.append(String.format("; %.0f kg of %s HMC (%s)", hmc.total(), dev.wildcatter.tailings.plant.DryMill.separates(machine()) ? "dry" : "moist", hmc.describe()));
		}
		if (gasM3 > 0) {
			held.append(String.format("; %.2f m3 of gas burnt", gasM3));
		}
		if (machine() == Machine.CONDITIONER) {
			held.append(selective ? "; SELECTIVE: holds the zinc down" : "; BULK");
		}
		if (dms != null) {
			held.append(String.format(java.util.Locale.ROOT, "; medium at SG %.2f; %.1f t treated, %.1f t floated, %.2f kg of ferrosilicon lost", dms.medium(), dms.fedKg() / 1000,
					dms.floatsKg() / 1000, dms.fesiKg()));
		}
		if (machine() == Machine.MEDIUM_RECOVERY) {
			held.append(String.format(java.util.Locale.ROOT, "; %.1f kg of ferrosilicon in its hopper", dryKg));
		}
		if (machine() == Machine.PASTE_FILTER) {
			held.append(String.format("; %.1f t of cake", dryKg / 1000));
		}
		if (machine() == Machine.PASTE_MIXER) {
			held.append(String.format("; %.0f bags of cement (%.0f kg)", Math.floor(dryKg / dev.wildcatter.tailings.paste.PasteFill.CEMENT_BAG_KG), dryKg));
		}
		if (machine() == Machine.PASTE_PUMP && dev.wildcatter.tailings.block.stage.Paste.line(this) instanceof dev.wildcatter.tailings.block.stage.Paste.Line line) {
			held.append(String.format("; %d m of fill line%s", line.pipes, line.outlet == null ? ", no outlet" : ""));
		}
		if (machine() == Machine.HOIST_HOUSE) {
			held.append(String.format("; %d skips dumped (%.0f t); skips at %.0f and %.0f m of %.0f m", winder.dumps(), winder.dumps() * dev.wildcatter.tailings.shaft.Hoisting.SKIP_KG / 1000,
					winder.height(0), winder.height(1), winder.travel()));
		}
		if (xrt != null) {
			held.append("; ").append(dev.wildcatter.tailings.plant.XrtSorting.describe(xrtStep(), xrtReverse())).append(xrtByPlc() || xrtReverseByPlc() ? " (PLC)" : "")
					.append(String.format("; %.1f t to product, %.1f t to waste", xrt.productKg() / 1000, xrt.wasteKg() / 1000));
		}
		if (machine() == Machine.ORE_BIN) {
			held.append("; ").append(binCount()).append(" of ").append(PlantRates.BIN_ITEMS).append(" items");
		}
		if (machine() == Machine.ORE_SORTER) {
			String[] names = {"north", "east", "south", "west"};
			for (int side = 0; side < names.length; side++) {
				String takes = sorter.takes(side);
				String set = side == sorter.waste() ? (takes == null ? "waste" : takes + " and waste") : takes;
				if (sorter.chosen(side)) {
					held.append("; ").append(names[side]).append(" (PLC): ").append(set == null ? "nothing" : set);
				} else if (set != null) {
					held.append("; ").append(names[side]).append(": ").append(set);
				}
			}
		}
		return Component.literal(machine().displayName() + " (" + machine().watts() / 1000.0 + " kW): " + status + held + "; " + power);
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
		ContainerHelper.saveAllItems(output, slots);
		output.putString("cake", cake.save());
		output.putString("hmc", hmc.save());
		output.putDouble("gas_m3", gasM3);
		output.putString("reagents", reagents.save());
		output.putString("status", status);
		output.putString("hold", hold.save());
		output.putString("travel", travel.save());
		output.putString("made", made.save());
		output.putString("out", out.save());
		StringBuilder made = new StringBuilder();
		byproducts.forEach((id, kg) -> made.append(id).append('=').append(kg).append(';'));
		output.putString("byproducts", made.toString());
		output.putString("back", back.save());
		output.putDouble("water", water);
		output.putDouble("dry_kg", dryKg);
		output.putDouble("cake_out", cakeOut);
		output.putDouble("cycle", cycle);
		output.putDouble("water_out", waterOut);
		output.putDouble("water_returned", waterReturned);
		output.putInt("cycles", cycles);
		output.putString("sort", sorter.save());
		if (machine() == Machine.HOIST_HOUSE) {
			output.putString("winder", winder.save());
		}
		remote.save(output);
		output.putBoolean("selective", selective);
		StringBuilder bin = new StringBuilder();
		binned.forEach((id, n) -> bin.append(bin.length() == 0 ? "" : ",").append(id).append('=').append(n));
		output.putString("binned", bin.toString());
		if (vent != null) {
			vent.save(output);
		}
		if (dms != null) {
			output.putString("dms", dms.save());
		}
		if (xrt != null) {
			output.putString("xrt", xrt.save());
		}
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		sounding = input.getBooleanOr("sounding", false);
		grown = input.getBooleanOr("grown", false);
		slots.clear();
		loading = true;
		ContainerHelper.loadAllItems(input, slots);
		loading = false;
		remote.load(input);
		cake.load(input.getStringOr("cake", ""));
		hmc.load(input.getStringOr("hmc", ""));
		gasM3 = input.getDoubleOr("gas_m3", 0);
		reagents.load(input.getStringOr("reagents", ""));
		// a tank saved by an older build may hold spent reagent items
		trimReagents();
		status = input.getStringOr("status", "idle");
		hold.load(input.getStringOr("hold", ""));
		// a run saved before belts had a transit time: its load is at the head end
		travel.load(input.getStringOr("travel", ""));
		travel.match(hold.total());
		made.load(input.getStringOr("made", ""));
		out.load(input.getStringOr("out", ""));
		byproducts.clear();
		for (String one : input.getStringOr("byproducts", "").split(";")) {
			String[] pair = one.split("=");
			if (pair.length == 2) {
				byproducts.put(pair[0], Double.parseDouble(pair[1]));
			}
		}
		back.load(input.getStringOr("back", ""));
		water = input.getDoubleOr("water", 0);
		dryKg = input.getDoubleOr("dry_kg", 0);
		cakeOut = input.getDoubleOr("cake_out", 0);
		cycle = input.getDoubleOr("cycle", 0);
		waterOut = input.getDoubleOr("water_out", 0);
		waterReturned = input.getDoubleOr("water_returned", 0);
		cycles = input.getIntOr("cycles", 0);
		sorter.load(input.getStringOr("sort", ""));
		winder.load(input.getStringOr("winder", ""));
		selective = input.getBooleanOr("selective", false);
		if (vent != null) {
			vent.load(input);
		}
		if (dms != null) {
			dms.load(input.getStringOr("dms", ""));
		}
		if (xrt != null) {
			xrt.load(input.getStringOr("xrt", ""));
		}
		binned.clear();
		for (String part : input.getStringOr("binned", "").split(",")) {
			int eq = part.lastIndexOf('=');
			if (eq > 0) {
				try {
					binned.put(part.substring(0, eq), Integer.parseInt(part.substring(eq + 1)));
				} catch (NumberFormatException ex) {
					// left out
				}
			}
		}
	}

	// ---- the container: slot 0 the intake, slot 1 the product, 2-5 reagents ------------------------------------------------------

	@Override
	public int getContainerSize() {
		return slots.size();
	}

	@Override
	public boolean isEmpty() {
		return slots.stream().allMatch(ItemStack::isEmpty);
	}

	@Override
	public ItemStack getItem(int slot) {
		return slots.get(slot);
	}

	@Override
	public ItemStack removeItem(int slot, int count) {
		ItemStack out = ContainerHelper.removeItem(slots, slot, count);
		setChanged();
		return out;
	}

	@Override
	public ItemStack removeItemNoUpdate(int slot) {
		return ContainerHelper.takeItem(slots, slot);
	}

	@Override
	public void setItem(int slot, ItemStack stack) {
		slots.set(slot, stack);
		if (!loading && slot >= 2 && dosed()) {
			Reagent reagent = reagent(stack);
			if (reagent != null) {
				for (int i = 0; i < stack.getCount(); i++) {
					reagents.addKilogram(reagent);
				}
			}
		}
		setChanged();
	}

	@Override
	public boolean stillValid(Player player) {
		return Container.stillValidBlockEntity(this, player);
	}

	/** Breaking the part, any way at all, drops what it holds as items: its slots, and an ore bin's rock. */
	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && vent != null) {
			VentNetwork.forget(level, pos);
		}
		if (level != null) {
			Containers.dropContents(level, pos, this);
			binned.forEach((id, n) -> {
				for (int left = n; left > 0; left -= 64) {
					Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), Feeds.stack(id, Math.min(64, left)));
				}
			});
			binned.clear();
			if (machine() == Machine.PASTE_MIXER) {
				// the mixer's hopper: its cement back as bags, not dry tails
				int bags = (int) Math.floor(dryKg / dev.wildcatter.tailings.paste.PasteFill.CEMENT_BAG_KG);
				for (int left = bags; left > 0; left -= 64) {
					Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(dev.wildcatter.tailings.TailingsItems.CEMENT, Math.min(64, left)));
				}
				dryKg = 0;
			}
			if (machine() == Machine.MEDIUM_RECOVERY) {
				// the make-up hopper: its ferrosilicon back as whole bags
				int bags = (int) Math.floor(dryKg / dev.wildcatter.tailings.plant.DenseMedium.FESI_BAG_KG + 1e-6);
				for (int left = bags; left > 0; left -= 64) {
					Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(dev.wildcatter.tailings.TailingsItems.FERROSILICON, Math.min(64, left)));
				}
				dryKg = 0;
			}
			// a load-out bin's dry tails, as items, up to a few stacks: the rest is spilt
			int blocks = (int) Math.min(64 * 9, (dryKg + cakeOut) / dev.wildcatter.tailings.pit.PitMass.DRY_TAILS_KG);
			for (int left = blocks; left > 0; left -= 64) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), new ItemStack(TailingsBlocks.DRY_TAILS, Math.min(64, left)));
			}
			dryKg = 0;
			cakeOut = 0;
			// its heavy mineral concentrate, in whole bags: the rest is spilt
			for (dev.wildcatter.tailings.plant.HeavyMinerals bag = dev.wildcatter.tailings.plant.DryMill.bagged(hmc); bag != null; bag = dev.wildcatter.tailings.plant.DryMill.bagged(hmc)) {
				Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), dev.wildcatter.tailings.item.HeavyMineralConcentrateItem.of(dev.wildcatter.tailings.TailingsItems.HEAVY_MINERAL_CONCENTRATE, bag));
			}
			hmc.clear();
		}
	}

	@Override
	public void clearContent() {
		slots.clear();
	}

	@Override
	public int[] getSlotsForFace(Direction side) {
		if (pays()) {
			return side == Direction.UP ? new int[0] : outputs();
		}
		return side == Direction.UP ? new int[] {0, 2, 3, 4, 5} : new int[] {1};
	}

	@Override
	public boolean canPlaceItem(int slot, ItemStack stack) {
		if (slot == 0 && machine() == Machine.BYPRODUCT_PLANT) {
			return dev.wildcatter.tailings.plant.ByProducts.byItem(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).toString()) != null;
		}
		if (slot == 0 && machine() == Machine.LOADOUT_BIN) {
			return dryTails(stack);
		}
		if (slot == 0 && machine() == Machine.PASTE_MIXER) {
			return cement(stack);
		}
		if (slot == 0 && machine() == Machine.MEDIUM_RECOVERY) {
			return ferrosilicon(stack);
		}
		if (slot == 0 && machine() == Machine.FLUID_BED_DRYER) {
			return hmcBag(stack) && slots.get(0).isEmpty();
		}
		if (slot == 0) {
			Feeds.Charge charge = Feeds.parse(stack);
			if (charge == null) {
				return false;
			}
			return switch (machine()) {
				case ORE_BIN -> binCount() < PlantRates.BIN_ITEMS;
				case CONDITIONER, ACTIVATION_TANK, FILTER, ELECTROWINNING_BANK -> false;
				default -> accepts(charge.rock(), charge.size());
			};
		}
		return slot >= 2 && dosed() && reagent(stack) != null;
	}

	@Override
	public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
		return side == Direction.UP && canPlaceItem(slot, stack);
	}

	@Override
	public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
		return (slot == 1 || slot == 2 && pays() || slot >= 3 && slot <= 4 && dev.wildcatter.tailings.plant.DryMill.separates(machine())) && side != Direction.UP;
	}
}
