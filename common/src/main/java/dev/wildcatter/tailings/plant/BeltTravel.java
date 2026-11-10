package dev.wildcatter.tailings.plant;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a belt run's load is along the run (docs/multiblocks/crushing-circuit.md, `belt_conveyor`: a troughed belt at 2 m/s). Each
 * parcel of rock rides from the point it dropped on toward the head end, and the drive delivers only what has reached it, so a run's
 * transit time is its length over the belt's speed. The rock itself (what it is and what size) is the drive's {@link RockLoad}; this
 * keeps only kg and metres. Plain Java.
 */
public final class BeltTravel {
	/** Parcels dropped on within this many metres of each other ride as one, so a long run keeps a short list. */
	private static final double MERGE_METRES = 0.5;

	/** One parcel: its kg and the metres it still has to ride to the head end (0 or less: there). */
	private record Parcel(double kg, double toGo) {
	}

	/** Head end first: the parcel nearest the head is first. */
	private final List<Parcel> parcels = new ArrayList<>();

	/** Rock dropped on {@code toGo} metres short of the head end. */
	public void add(double kg, double toGo) {
		if (kg <= 0) {
			return;
		}
		double at = Math.max(0, toGo);
		for (int i = 0; i < parcels.size(); i++) {
			Parcel p = parcels.get(i);
			if (Math.abs(p.toGo() - at) <= MERGE_METRES) {
				// it joins the parcel riding there, at that parcel's place: rock fed on steadily still rides on
				parcels.set(i, new Parcel(p.kg() + kg, p.toGo()));
				return;
			}
			if (p.toGo() > at) {
				parcels.add(i, new Parcel(kg, at));
				return;
			}
		}
		parcels.add(new Parcel(kg, at));
	}

	/** The belt runs {@code metres} on: every parcel comes that much nearer the head end. */
	public void advance(double metres) {
		for (int i = 0; i < parcels.size(); i++) {
			Parcel p = parcels.get(i);
			parcels.set(i, new Parcel(p.kg(), p.toGo() - metres));
		}
		// parcels that arrived together are one
		for (int i = parcels.size() - 1; i > 0; i--) {
			if (parcels.get(i).toGo() <= 0 && parcels.get(i - 1).toGo() <= 0) {
				Parcel a = parcels.get(i - 1), b = parcels.remove(i);
				parcels.set(i - 1, new Parcel(a.kg() + b.kg(), Math.max(a.toGo(), b.toGo())));
			}
		}
	}

	/** Kg that has reached the head end and can be delivered. */
	public double ready() {
		double kg = 0;
		for (Parcel p : parcels) {
			if (p.toGo() <= 1e-9) {
				kg += p.kg();
			}
		}
		return kg;
	}

	/** Takes up to {@code kg} of what has reached the head end; returns what it took. */
	public double take(double kg) {
		double took = 0;
		for (int i = 0; i < parcels.size() && took < kg; i++) {
			Parcel p = parcels.get(i);
			if (p.toGo() > 1e-9) {
				break;
			}
			double some = Math.min(p.kg(), kg - took);
			took += some;
			if (some >= p.kg() - 1e-9) {
				parcels.remove(i--);
			} else {
				parcels.set(i, new Parcel(p.kg() - some, p.toGo()));
			}
		}
		return took;
	}

	public double total() {
		double kg = 0;
		for (Parcel p : parcels) {
			kg += p.kg();
		}
		return kg;
	}

	/** Kg riding between {@code from} and {@code to} metres short of the head end; what has reached the head counts in its first metre. */
	public double between(double from, double to) {
		double kg = 0;
		for (Parcel p : parcels) {
			double at = Math.max(0, p.toGo());
			if (at >= from && (at < to || at == 0 && from == 0)) {
				kg += p.kg();
			}
		}
		return kg;
	}

	/** Seconds a parcel dropped on the tail of a {@code metres} run takes to reach its head end at {@code metresPerSecond}. */
	public static double transitSeconds(int metres, double metresPerSecond) {
		return metres / metresPerSecond;
	}

	/** Brings the parcels' total to {@code kg} (the drive's load is the truth): rock added outside the belt is at the head end. */
	public void match(double kg) {
		double have = total();
		if (kg > have + 1e-6) {
			add(kg - have, 0);
		} else if (kg < have - 1e-6) {
			double extra = have - kg;
			for (int i = parcels.size() - 1; i >= 0 && extra > 1e-9; i--) {
				Parcel p = parcels.get(i);
				double some = Math.min(p.kg(), extra);
				extra -= some;
				if (some >= p.kg() - 1e-9) {
					parcels.remove(i);
				} else {
					parcels.set(i, new Parcel(p.kg() - some, p.toGo()));
				}
			}
		}
	}

	public boolean isEmpty() {
		return parcels.isEmpty();
	}

	/** Saved as {@code kg@toGo;kg@toGo}. */
	public String save() {
		StringBuilder out = new StringBuilder();
		for (Parcel p : parcels) {
			out.append(out.length() == 0 ? "" : ";").append(p.kg()).append('@').append(p.toGo());
		}
		return out.toString();
	}

	public void load(String text) {
		parcels.clear();
		if (text == null || text.isBlank()) {
			return;
		}
		for (String part : text.split(";")) {
			int at = part.indexOf('@');
			if (at > 0) {
				try {
					add(Double.parseDouble(part.substring(0, at)), Double.parseDouble(part.substring(at + 1)));
				} catch (NumberFormatException ex) {
					// left out
				}
			}
		}
	}
}
