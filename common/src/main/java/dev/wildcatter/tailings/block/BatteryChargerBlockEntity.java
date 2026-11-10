package dev.wildcatter.tailings.block;

import dev.wildcatter.block.ReceptacleBlock;
import dev.wildcatter.circuit.Circuit;
import dev.wildcatter.circuit.CircuitManager;
import dev.wildcatter.circuit.CircuitPart;
import dev.wildcatter.circuit.Conductor;
import dev.wildcatter.circuit.DriveLoad;
import dev.wildcatter.control.ControlDevice;
import dev.wildcatter.energy.Inspectable;
import dev.wildcatter.energy.PlugType;
import dev.wildcatter.energy.Voltage;
import dev.wildcatter.multiblock.BigPartEntity;
import dev.wildcatter.multiblock.BigParts;
import dev.wildcatter.tailings.TailingsBlocks;
import dev.wildcatter.tailings.TailingsItems;
import dev.wildcatter.tailings.entity.MineVehicleEntity;
import dev.wildcatter.tailings.haul.Battery;
import dev.wildcatter.tailings.item.LocoBatteryItem;
import java.util.List;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/**
 * The battery charger of a charging bay (docs/multiblocks/battery-locomotive.md, L2): a 2 x 2 x 1 charger on a wall, its three-phase
 * 30 A plug in a Wildcatter receptacle touching it (the world's: NEMA L22-30, or IEC 60309 red 32 A on the industrial tier). It is a
 * receptacle load: a constant-power rectifier at power factor 0.95 drawing 20 kW (Wildcatter's {@link DriveLoad}, which picks up
 * its load at 10 % a second and stops below 80 % of its volts), on the circuit the receptacle is on, so that circuit's breaker sees
 * it. 90 % of what it draws goes into the battery. It charges the battery it holds, or else a battery locomotive standing with its
 * cab within 3 m of it. PLC points: CHARGING, FULL, and the charge on 4-20 mA.
 */
public class BatteryChargerBlockEntity extends BlockEntity implements BigPartEntity, Inspectable, CircuitPart, ControlDevice {
	public static final int CHARGING = 0;
	public static final int FULL = 1;
	public static final int CHARGE = 2;

	private boolean grown;
	/** The battery box it holds: usable watt-hours, or -1 with none. */
	private double heldWh = -1;
	private final DriveLoad line = new DriveLoad(Voltage.INDUSTRIAL, Battery.CHARGER_POWER_FACTOR, 0);
	/** Watts going into a battery on the last tick. */
	private double intoBattery;
	/** The share of the battery it is charging (or its own), for the readout and the 4-20 mA point; -1 with none. */
	private double reading = -1;
	private String status = "starting";
	/** Where its plug meets a receptacle: the cell and the face, or null. */
	private @Nullable BlockPos plugCell;
	private @Nullable Direction plugFace;
	private @Nullable String wrongReceptacle;

	public BatteryChargerBlockEntity(BlockPos pos, BlockState state) {
		super(TailingsBlocks.BATTERY_CHARGER_ENTITY, pos, state);
	}

	/** Its plug fits the world's three-phase 30 A receptacle on the industrial tier: NEMA L22-30 or IEC 60309 red 32 A. */
	public static boolean fits(PlugType type) {
		return type.threePhase() && type.tier() == Voltage.INDUSTRIAL && type.amps() >= 30 && type.amps() <= 32;
	}

	public double heldWh() {
		return heldWh;
	}

	public void setHeldWh(double wh) {
		heldWh = wh < 0 ? -1 : Math.min(Battery.USABLE_WH, wh);
		setChanged();
	}

	/** Watts it draws from the line, as the last solve found them. */
	public double lineWatts() {
		return line.watts();
	}

	public double intoBattery() {
		return intoBattery;
	}

	public String status() {
		return status;
	}

	public static void tick(Level level, BlockPos pos, BlockState state, BatteryChargerBlockEntity charger) {
		if (level instanceof ServerLevel server) {
			charger.serverTick(server);
		}
	}

	private void serverTick(ServerLevel server) {
		findPlug();
		MineVehicleEntity loco = heldWh >= 0 ? null : locomotiveInReach(server);
		boolean wants = heldWh >= 0 ? heldWh < Battery.USABLE_WH - 1e-6 : loco != null && loco.batteryWh() < Battery.USABLE_WH - 1e-6;
		double demand = wants && plugCell != null ? Battery.CHARGER_WATTS : 0;
		if (Math.abs(demand - line.demand()) > 1) {
			line.setDemand(demand);
			CircuitManager.markDirty(server);
		}
		double watts = wants && plugCell != null && !line.undervoltage() ? line.watts() : 0;
		intoBattery = watts * Battery.CHARGE_EFFICIENCY;
		double seconds = 1.0 / 20;
		if (heldWh >= 0) {
			if (intoBattery > 0) {
				heldWh = Math.min(Battery.USABLE_WH, heldWh + intoBattery * seconds / 3600);
				setChanged();
			}
			reading = heldWh;
		} else if (loco != null) {
			if (intoBattery > 0) {
				loco.charge(intoBattery, seconds);
			}
			reading = loco.batteryWh();
		} else {
			reading = -1;
		}
		if (plugCell == null) {
			status = wrongReceptacle != null ? "WRONG RECEPTACLE: its plug is three-phase 30 A, not " + wrongReceptacle
					: "NOT PLUGGED IN: set it against a three-phase 30 A receptacle";
		} else if (reading < 0) {
			status = "no battery: put a traction battery in it, or stand a battery locomotive with its cab within 3 m";
		} else if (!wants) {
			status = "FULL: " + LocoBatteryItem.describe(reading) + (loco != null ? " (the locomotive's)" : "");
		} else if (line.undervoltage() || line.volts() < 1) {
			status = "NO POWER: the receptacle is dead";
		} else {
			status = String.format(Locale.ROOT, "CHARGING%s at %.1f kW (%.1f kW from the line): %s", loco != null ? " the locomotive" : "",
					intoBattery / 1000, watts / 1000, LocoBatteryItem.describe(reading));
		}
	}

	/** A battery locomotive standing with its cab within reach of any of its cells, with a battery that is not full. */
	private @Nullable MineVehicleEntity locomotiveInReach(ServerLevel server) {
		AABB box = null;
		for (BlockPos cell : BigParts.cells(worldPosition, getBlockState())) {
			AABB one = new AABB(cell);
			box = box == null ? one : box.minmax(one);
		}
		if (box == null) {
			return null;
		}
		for (MineVehicleEntity loco : server.getEntitiesOfClass(MineVehicleEntity.class, box.inflate(Battery.CHARGE_REACH),
				e -> e.battery() && e.hasBattery() && e.standing() && e.isAlive())) {
			return loco;
		}
		return null;
	}

	/** The first face of its cells that meets a receptacle its plug fits. */
	private void findPlug() {
		plugCell = null;
		plugFace = null;
		wrongReceptacle = null;
		if (level == null) {
			return;
		}
		List<BlockPos> cells = BigParts.cells(worldPosition, getBlockState());
		for (BlockPos cell : cells) {
			for (Direction face : Direction.values()) {
				if (level.getBlockState(cell.relative(face)).getBlock() instanceof ReceptacleBlock receptacle) {
					if (fits(receptacle.type())) {
						plugCell = cell;
						plugFace = face;
						return;
					}
					wrongReceptacle = receptacle.type().label();
				}
			}
		}
	}

	// ---- hands --------------------------------------------------------------------------------------------------------------------

	/** A traction battery put in it; the one it held comes back to the player. */
	public boolean offer(Player player, InteractionHand hand, ItemStack stack) {
		if (stack.getItem() != TailingsItems.LOCO_BATTERY) {
			return false;
		}
		ItemStack old = heldWh >= 0 ? LocoBatteryItem.of(TailingsItems.LOCO_BATTERY, heldWh) : ItemStack.EMPTY;
		setHeldWh(LocoBatteryItem.charge(stack));
		stack.shrink(1);
		if (!old.isEmpty()) {
			if (stack.isEmpty()) {
				player.setItemInHand(hand, old);
			} else {
				player.getInventory().placeItemBackInInventory(old, net.minecraft.util.Prediction.SERVER_ONLY);
			}
		}
		return true;
	}

	/** Its battery, taken out to the player. */
	public void takeOut(Player player) {
		if (heldWh >= 0) {
			player.getInventory().placeItemBackInInventory(LocoBatteryItem.of(TailingsItems.LOCO_BATTERY, heldWh), net.minecraft.util.Prediction.SERVER_ONLY);
			setHeldWh(-1);
		}
	}

	@Override
	public void preRemoveSideEffects(BlockPos pos, BlockState state) {
		super.preRemoveSideEffects(pos, state);
		if (level != null && heldWh >= 0) {
			Containers.dropItemStack(level, pos.getX(), pos.getY(), pos.getZ(), LocoBatteryItem.of(TailingsItems.LOCO_BATTERY, heldWh));
			heldWh = -1;
		}
	}

	// ---- the circuit: a three-phase receptacle load ----------------------------------------------------------------------------------

	@Override
	public boolean hasPort(Direction face, Conductor role, Voltage voltage) {
		return false;
	}

	/** Its plug's pins, where its cell meets the receptacle: phases A, B and C on the industrial tier. */
	@Override
	public boolean hasPortAt(BlockPos cell, Direction face, Conductor role, Voltage voltage) {
		return cell.equals(plugCell) && face == plugFace && voltage == line.voltage() && (role == Conductor.A || role == Conductor.B || role == Conductor.C);
	}

	@Override
	public void build(Circuit circuit) {
		findPlug();
		line.build(circuit, plugCell != null ? plugCell : worldPosition, plugFace != null ? List.of(plugFace) : List.of());
	}

	@Override
	public void apply(Circuit circuit, int ticks) {
		line.apply(circuit, ticks);
	}

	// ---- PLC points -------------------------------------------------------------------------------------------------------------

	@Override
	public String deviceName() {
		return "Battery charger";
	}

	@Override
	public List<Point> points() {
		return List.of(new Point("CHARGING", false), new Point("FULL", false), new Point("CHARGE 4-20 mA, empty to full", false, Signal.MA));
	}

	@Override
	public boolean readPoint(int point) {
		return switch (point) {
			case CHARGING -> intoBattery > 100;
			case FULL -> reading >= Battery.USABLE_WH - 1e-6;
			default -> false;
		};
	}

	@Override
	public void commandPoint(int point, boolean on, long gameTime) {
		// it has no outputs
	}

	/** The charge of the battery it holds or charges; empty (4 mA) with none. */
	@Override
	public double readMa(int point) {
		return point == CHARGE ? Battery.chargeMa(Math.max(0, reading)) : 0;
	}

	// ---- the readout and saving -----------------------------------------------------------------------------------------------------

	@Override
	public Component inspect() {
		return Component.literal("Battery Charger (20 kW, three-phase 30 A plug): " + status + "; line " + line.describe());
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
		output.putDouble("held", heldWh);
	}

	@Override
	protected void loadAdditional(ValueInput input) {
		super.loadAdditional(input);
		grown = input.getBooleanOr("grown", false);
		heldWh = input.getDoubleOr("held", -1);
	}
}
