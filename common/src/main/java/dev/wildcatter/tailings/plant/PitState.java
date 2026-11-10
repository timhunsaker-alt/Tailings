package dev.wildcatter.tailings.plant;

import java.util.HashMap;
import java.util.Map;

/**
 * What a pit-line part keeps between ticks (primary-gyratory-stockpile.md, sag-mill-line.md): the gyratory's boulder count and a bridge
 * being broken; the belt weigher's setpoint, its rate and totals and the feeders following it; the stockpile head's tonnes stacked; the
 * SAG's overload. Each part uses its own fields; the rest stay at zero. Plain Java.
 */
public final class PitState {
	/** Gyratory: hard bank blocks since the last boulder, whether one bridges the bowl now, and the breaker's ticks on it. */
	private int sinceBoulder;
	private boolean bridged;
	private int breakTicks;
	private int boulders;

	/** Belt weigher: its own setpoint (t/h), kg counted onto its belt this second and last, its total kg, pulses sent. */
	private double setpoint = PitLine.WEIGHER_DEFAULT_TPH;
	private double counting;
	private double lastSecondKg;
	private double totalKg;
	/** The setpoint the PLC sent while in AUTO; below 0, none. Not saved. */
	private double plcSetpoint = -1;
	/** Feeders that drew for this weigher, by position, with the game time they last did. Not saved. */
	private final Map<Long, Long> feeders = new HashMap<>();
	/** True while the mill its belt feeds reads OVERLOAD (not saved: the mill says so each tick). */
	private long overloadSeen = Long.MIN_VALUE;

	/** Stockpile head: kg stacked onto the pile so far, and blocks dropped. */
	private double stackedKg;
	private int dropped;

	/** Reclaim feeder: kg drawn so far. */
	private double drawnKg;

	public int sinceBoulder() {
		return sinceBoulder;
	}

	/** A test sets how many hard blocks have gone by since the last boulder. */
	public void setSinceBoulder(int n) {
		sinceBoulder = n;
	}

	/**
	 * Counts one bank block into the crusher; a hard rock's every {@link PitLine#BOULDER_EVERY}th bridges the bowl. Returns true when
	 * this one does.
	 */
	public boolean countBlock(boolean hard) {
		if (!hard) {
			return false;
		}
		sinceBoulder++;
		if (sinceBoulder >= PitLine.BOULDER_EVERY) {
			sinceBoulder = 0;
			bridged = true;
			breakTicks = 0;
			boulders++;
			return true;
		}
		return false;
	}

	public boolean bridged() {
		return bridged;
	}

	public int boulders() {
		return boulders;
	}

	public int breakTicks() {
		return breakTicks;
	}

	/** The breaker worked on the boulder a tick; true when it is broken. */
	public boolean breakTick() {
		if (!bridged) {
			return false;
		}
		breakTicks++;
		if (breakTicks >= PitLine.BREAK_SECONDS * 20) {
			bridged = false;
			breakTicks = 0;
			return true;
		}
		return false;
	}

	// ---- the belt weigher ----------------------------------------------------------------------------------------------------------

	public double ownSetpoint() {
		return setpoint;
	}

	public void setSetpoint(double tph) {
		setpoint = Math.max(0, Math.min(PitLine.WEIGHER_SPAN_TPH, tph));
	}

	public void setPlcSetpoint(double tph) {
		plcSetpoint = tph;
	}

	public boolean byPlc() {
		return plcSetpoint >= 0;
	}

	/** The setpoint the feeders follow: the PLC's while it sends one, else its own; 10 % less while the mill reads OVERLOAD. */
	public double setpoint(long now) {
		double base = plcSetpoint >= 0 ? plcSetpoint : setpoint;
		return overloaded(now) ? base * (1 - PitLine.OVERLOAD_FALLBACK) : base;
	}

	public void sawOverload(long now) {
		overloadSeen = now;
	}

	public boolean overloaded(long now) {
		return overloadSeen != Long.MIN_VALUE && now - overloadSeen <= 40;
	}

	/** A feeder at {@code at} drew for this weigher at {@code now}. */
	public void feederRan(long at, long now) {
		feeders.put(at, now);
	}

	/** Feeders that drew in the last two seconds. */
	public int feeders(long now) {
		feeders.values().removeIf(t -> now - t > 40);
		return feeders.size();
	}

	public void weigh(double kg) {
		counting += kg;
		totalKg += kg;
	}

	/** Each second: the kg counted in it become the rate. */
	public void second() {
		lastSecondKg = counting;
		counting = 0;
	}

	public double rateTph() {
		return lastSecondKg * 3600 / 1000;
	}

	public double totalKg() {
		return totalKg;
	}

	public long pulses() {
		return (long) Math.floor(totalKg / PitLine.WEIGHER_PULSE_KG);
	}

	// ---- the stockpile head and the feeders -----------------------------------------------------------------------------------------

	public void stacked(double kg) {
		stackedKg += kg;
		dropped++;
	}

	public double stackedKg() {
		return stackedKg;
	}

	public int dropped() {
		return dropped;
	}

	public void drew(double kg) {
		drawnKg += kg;
	}

	public double drawnKg() {
		return drawnKg;
	}

	public String save() {
		return sinceBoulder + ";" + (bridged ? 1 : 0) + ";" + breakTicks + ";" + boulders + ";" + setpoint + ";" + totalKg + ";" + stackedKg + ";" + dropped + ";" + drawnKg;
	}

	public void load(String text) {
		String[] f = text == null ? new String[0] : text.split(";");
		try {
			sinceBoulder = f.length > 0 && !f[0].isEmpty() ? Integer.parseInt(f[0]) : 0;
			bridged = f.length > 1 && "1".equals(f[1]);
			breakTicks = f.length > 2 ? Integer.parseInt(f[2]) : 0;
			boulders = f.length > 3 ? Integer.parseInt(f[3]) : 0;
			setpoint = f.length > 4 ? Double.parseDouble(f[4]) : PitLine.WEIGHER_DEFAULT_TPH;
			totalKg = f.length > 5 ? Double.parseDouble(f[5]) : 0;
			stackedKg = f.length > 6 ? Double.parseDouble(f[6]) : 0;
			dropped = f.length > 7 ? Integer.parseInt(f[7]) : 0;
			drawnKg = f.length > 8 ? Double.parseDouble(f[8]) : 0;
		} catch (NumberFormatException ex) {
			// a damaged save keeps the defaults
		}
	}
}
