package dev.wildcatter.tailings.plant;

import java.util.Locale;

/**
 * A copper smelter part's own state (docs/copper-smelter.md): the copper it holds to work, what it has plated toward its next cathode,
 * and its totals for the readout. Plain Java; saved as one string.
 */
public final class SmelterState {
	/** The ANODE CAST and CATHODE PULLED points stay on this long (1 s). */
	public static final int PULSE_TICKS = 20;

	private double copper;
	private double plated;
	private double fedKg;
	private double lostKg;
	private double slagKg;
	private double sulfurKg;
	private int made;
	private long madeAt = Long.MIN_VALUE / 2;
	private boolean gasBacked;

	/** kg of copper held, waiting to be worked. */
	public double copper() {
		return copper;
	}

	public void add(double kg) {
		copper += Math.max(0, kg);
	}

	/** Takes up to {@code kg} of the copper held; what it took. */
	public double take(double kg) {
		double got = Math.max(0, Math.min(kg, copper));
		copper -= got;
		if (copper < 1e-9) {
			copper = 0;
		}
		return got;
	}

	/** kg of copper plated toward the next cathode (the tankhouse). */
	public double plated() {
		return plated;
	}

	public void plate(double kg) {
		plated += kg;
	}

	public void unplate(double kg) {
		plated = Math.max(0, plated - kg);
	}

	/** Counts a pass: {@code fed} kg of copper worked, {@code lost} of it to slag, {@code slag} kg of slag, {@code sulfur} kg of S to gas. */
	public void count(double fed, double lost, double slag, double sulfur) {
		fedKg += fed;
		lostKg += lost;
		slagKg += slag;
		sulfurKg += sulfur;
	}

	/** Counts an anode cast or a cathode pulled at {@code now}. */
	public void made(long now) {
		made++;
		madeAt = now;
	}

	public boolean pulsing(long now) {
		return now - madeAt < PULSE_TICKS;
	}

	public void setGasBacked(boolean backed) {
		gasBacked = backed;
	}

	/** True while its off-gas has nowhere to go (not saved). */
	public boolean gasBacked() {
		return gasBacked;
	}

	public double fedKg() {
		return fedKg;
	}

	public double lostKg() {
		return lostKg;
	}

	public double slagKg() {
		return slagKg;
	}

	public double sulfurKg() {
		return sulfurKg;
	}

	public int madeCount() {
		return made;
	}

	public String save() {
		return String.format(Locale.ROOT, "%.6f;%.6f;%.4f;%.4f;%.4f;%.4f;%d", copper, plated, fedKg, lostKg, slagKg, sulfurKg, made);
	}

	public void load(String text) {
		String[] p = text == null || text.isEmpty() ? new String[0] : text.split(";");
		try {
			copper = p.length > 0 ? Double.parseDouble(p[0]) : 0;
			plated = p.length > 1 ? Double.parseDouble(p[1]) : 0;
			fedKg = p.length > 2 ? Double.parseDouble(p[2]) : 0;
			lostKg = p.length > 3 ? Double.parseDouble(p[3]) : 0;
			slagKg = p.length > 4 ? Double.parseDouble(p[4]) : 0;
			sulfurKg = p.length > 5 ? Double.parseDouble(p[5]) : 0;
			made = p.length > 6 ? Integer.parseInt(p[6]) : 0;
		} catch (NumberFormatException ex) {
			copper = 0;
		}
	}
}
