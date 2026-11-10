package dev.wildcatter.tailings.plant;

import java.util.Locale;

/**
 * A DMS cyclone module's own state (dense-medium-separation.md, L7): the medium's density as set, what it has treated, floated and lost
 * of ferrosilicon, and when it last counted a tonne of floats (the FLOATS pulse). Plain Java; saved as one string.
 */
public final class DmsState {
	/** The FLOATS point stays on this long for each tonne floated (1 s). */
	public static final int PULSE_TICKS = 20;

	private double medium = DenseMedium.SG_DEFAULT;
	private double fedKg;
	private double floatsKg;
	private double fesiKg;
	private double towardPulse;
	private long pulsedAt = Long.MIN_VALUE / 2;

	public double medium() {
		return medium;
	}

	public void setMedium(double sg) {
		medium = DenseMedium.clamp(sg);
	}

	/** Moves the setting one step up, wrapping; returns the new density. */
	public double step() {
		medium = DenseMedium.step(medium);
		return medium;
	}

	/** Counts a pass: {@code fed} kg treated, {@code floats} kg of it floated, {@code fesi} kg of ferrosilicon lost, at {@code now}. */
	public void count(double fed, double floats, double fesi, long now) {
		fedKg += fed;
		floatsKg += floats;
		fesiKg += fesi;
		towardPulse += floats;
		if (towardPulse >= 1000) {
			towardPulse -= 1000 * Math.floor(towardPulse / 1000);
			pulsedAt = now;
		}
	}

	/** True for a second after each tonne floated. */
	public boolean pulsing(long now) {
		return now - pulsedAt < PULSE_TICKS;
	}

	public double fedKg() {
		return fedKg;
	}

	public double floatsKg() {
		return floatsKg;
	}

	public double fesiKg() {
		return fesiKg;
	}

	public String save() {
		return String.format(Locale.ROOT, "%.4f;%.4f;%.4f;%.6f;%.4f", medium, fedKg, floatsKg, fesiKg, towardPulse);
	}

	public void load(String text) {
		String[] parts = text == null ? new String[0] : text.split(";");
		try {
			medium = parts.length > 0 && !parts[0].isEmpty() ? DenseMedium.clamp(Double.parseDouble(parts[0])) : DenseMedium.SG_DEFAULT;
			fedKg = parts.length > 1 ? Double.parseDouble(parts[1]) : 0;
			floatsKg = parts.length > 2 ? Double.parseDouble(parts[2]) : 0;
			fesiKg = parts.length > 3 ? Double.parseDouble(parts[3]) : 0;
			towardPulse = parts.length > 4 ? Double.parseDouble(parts[4]) : 0;
		} catch (NumberFormatException ex) {
			medium = DenseMedium.SG_DEFAULT;
		}
	}
}
