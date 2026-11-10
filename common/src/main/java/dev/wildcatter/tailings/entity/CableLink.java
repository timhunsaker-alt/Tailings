package dev.wildcatter.tailings.entity;

import dev.wildcatter.energy.Voltage;
import dev.wildcatter.tailings.block.PitSwitchHouseBlockEntity;
import dev.wildcatter.tailings.pit.PitPower;
import java.util.Locale;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A machine's trailing cable (docs/multiblocks/open-pit-p5-big-machines.md, Trailing cables): from one outlet of a pit switch house to
 * the machine's cable reel. The cable is counted, not placed: what is paid out is the path the machine has driven from the house
 * (reeled in as it comes back, never less than the straight line). Past its length the machine stops, CABLE END. Each tick the machine
 * answers its outlet with what it draws, and reads back whether it has power.
 */
public final class CableLink {
	public enum Why {
		LIVE, NO_HOUSE, OPEN, LOW_VOLTAGE, CABLE_END
	}

	private final BlockPos house;
	private final int outlet;
	private double metres;
	private double paid;
	private boolean live;
	private double volts;
	private String why = "";
	private boolean end;

	public CableLink(BlockPos house, int outlet, double metres, double paid) {
		this.house = house.immutable();
		this.outlet = outlet;
		this.metres = metres;
		this.paid = paid;
	}

	public BlockPos house() {
		return house;
	}

	public int outlet() {
		return outlet;
	}

	/** The cable's length, metres. */
	public double metres() {
		return metres;
	}

	public void addMetres(double more) {
		metres += more;
	}

	/** Metres paid out along its path. */
	public double paid() {
		return paid;
	}

	/** Test hook. */
	public void setPaid(double metres) {
		paid = Math.max(0, metres);
	}

	/** It reached the end of its cable on the last move. */
	public boolean atEnd() {
		return end;
	}

	/** Power at the machine: its outlet closed and at least 90 % of the nominal volts at its end. */
	public boolean live() {
		return live && !end;
	}

	public double volts() {
		return volts;
	}

	/** Why it has no power, for its status; empty when it has. */
	public String why() {
		return end ? "CABLE END: " + Math.round(metres) + " m paid out" : live ? "" : why;
	}

	/** The switch house, if its chunk is loaded and it stands. */
	public @Nullable PitSwitchHouseBlockEntity house(ServerLevel level) {
		return level.isLoaded(house) && level.getBlockEntity(house) instanceof PitSwitchHouseBlockEntity h ? h : null;
	}

	/** Its outlet's coupler, where the cable leaves the house; the house's own corner if it is gone. */
	public Vec3 coupler(ServerLevel level) {
		var state = level.getBlockState(house);
		return state.getBlock() instanceof dev.wildcatter.tailings.block.PlantMachineBlock ? PitSwitchHouseBlockEntity.coupler(house, state, outlet) : Vec3.atCenterOf(house);
	}

	/**
	 * The machine moved from {@code before} to {@code now}: pays the cable out or in. True if that is within its length; false at CABLE
	 * END, and the caller puts the machine back. A jump of more than {@link PitPower#TORN_M} tears it out: {@code torn} is then set.
	 */
	public Move move(ServerLevel level, Vec3 before, Vec3 now) {
		Vec3 at = coupler(level);
		double moved = Math.hypot(now.x - before.x, now.z - before.z);
		if (moved > PitPower.TORN_M) {
			return Move.TORN;
		}
		double d0 = Math.hypot(before.x - at.x, before.z - at.z), d1 = Math.hypot(now.x - at.x, now.z - at.z);
		double next = PitPower.payOut(paid, d0, d1, moved);
		if (next > metres + 1e-6 && next > paid) {
			end = true;
			return Move.END;
		}
		end = next >= metres - 0.05;
		paid = next;
		return Move.OK;
	}

	public enum Move {
		OK, END, TORN
	}

	/** Answers its outlet: what it draws now and at its peak; reads back its power. */
	public void answer(ServerLevel level, UUID machine, double demandWatts, double peakWatts) {
		PitSwitchHouseBlockEntity h = house(level);
		if (h == null) {
			live = false;
			volts = 0;
			why = "NO POWER: no switch house at " + house.toShortString();
			return;
		}
		h.report(outlet, machine, demandWatts, peakWatts, metres, paid);
		live = h.live(outlet);
		volts = h.volts(outlet);
		why = live ? "" : h.noPower(outlet);
		if (why.isEmpty() && !live) {
			why = String.format(Locale.ROOT, "LOW VOLTAGE: %.0f of %.0f V", volts, Voltage.MEDIUM.lineToLine());
		}
	}

	public String describe() {
		return String.format(Locale.ROOT, "cable %.0f of %.0f m from outlet %d at %s; %s", paid, metres, outlet + 1, house.toShortString(),
				live() ? String.format(Locale.ROOT, "%.0f V", volts) : why());
	}

	public void save(ValueOutput output) {
		output.putLong("cable_house", house.asLong());
		output.putInt("cable_outlet", outlet);
		output.putDouble("cable_m", metres);
		output.putDouble("cable_paid", paid);
	}

	public static @Nullable CableLink load(ValueInput input) {
		long h = input.getLongOr("cable_house", Long.MIN_VALUE);
		if (h == Long.MIN_VALUE) {
			return null;
		}
		return new CableLink(BlockPos.of(h), input.getIntOr("cable_outlet", 0), input.getDoubleOr("cable_m", PitPower.CABLE_M), input.getDoubleOr("cable_paid", 0));
	}
}
