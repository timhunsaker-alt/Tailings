package dev.wildcatter.tailings.shaft;

/**
 * The cage winder's state (docs/multiblocks/cage-winder.md): a single-drum service winder, the cage on its own rope in its own
 * compartment, between the collar and the bottom landing. {@link #at} is the cage's floor above the bottom landing, from 0 to the
 * travel; the rope paid out over the drum is the travel less that. Plain Java, unit-tested. (Until 2026-11-23 this rode on the skip
 * hoist's {@link Winder}.)
 */
public final class CageWinder {
	/** The cage's floor above the bottom landing, the metres between the landings, its speed, the bell still to ring, its trips. */
	private double at, travel, speed, bell;
	/** Which way it goes: +1 up, -1 down, 0 standing at a landing (or braked between them with a call in hand). */
	private int dir, trips;

	public double at() {
		return at;
	}

	public double travel() {
		return travel;
	}

	public double speed() {
		return speed;
	}

	public int trips() {
		return trips;
	}

	/** The cage is ringing to go or on its way. */
	public boolean busy() {
		return dir != 0;
	}

	public boolean ringing() {
		return dir != 0 && bell > 0;
	}

	/** At the top landing (the collar)? */
	public boolean up() {
		return travel > 0 && at >= travel - 1e-6;
	}

	public boolean atLanding() {
		return dir == 0 && (at <= 1e-6 || up());
	}

	/** The rope paid out over the drum, m: the cage's depth below the collar. */
	public double rope() {
		return travel - at;
	}

	/** How far the drum has turned from the cage at the collar, degrees. */
	public double drumDegrees() {
		return Hoisting.cageDrumDegrees(rope());
	}

	/** The landings as the winder finds them; a change keeps the cage at its landing, the nearer one if it was between them. */
	public void setTravel(double metres) {
		if (Math.abs(metres - travel) < 1e-9) {
			return;
		}
		boolean top = travel <= 0 || at >= travel / 2;   // a cage first found is where it was hung, at the collar
		travel = Math.max(0, metres);
		at = top ? travel : 0;
		speed = 0;
		dir = 0;
	}

	/** Calls the cage to the top landing (true) or the bottom: the bell rings, then it goes. False when it is there or already going. */
	public boolean call(boolean top) {
		if (dir != 0 || travel <= 0) {
			return false;
		}
		if (top ? up() : at <= 1e-6) {
			return false;
		}
		dir = top ? 1 : -1;
		bell = Hoisting.CAGE_BELL_SECONDS;
		return true;
	}

	/** Sends the cage to the other landing (a rider's bell). */
	public boolean send() {
		return call(!up());
	}

	/** The brakes set: the cage stops where it is (no power to the motor). */
	public void brake() {
		speed = 0;
	}

	/**
	 * Rings the bell on, then winds the cage for {@code dt} s at the man-riding speed with the motor giving {@code strength} of its
	 * rating: up to speed, braked so it arrives at the creep, and the last metres at creep. True on the tick it arrives.
	 */
	public boolean wind(double dt, double strength) {
		if (dir == 0) {
			return false;
		}
		if (bell > 0) {
			bell = Math.max(0, bell - dt);
			return false;
		}
		if (strength <= 0) {
			brake();
			return false;
		}
		double target = dir > 0 ? travel : 0;
		double left = Math.abs(target - at);
		double top = Hoisting.CAGE_SPEED * Math.min(1, strength);
		double braking = Math.sqrt(2 * Hoisting.ACCEL * left);
		speed = braking <= Hoisting.CREEP ? Hoisting.CREEP : Math.min(Math.min(top, speed + Hoisting.ACCEL * dt), braking);
		double step = speed * dt;
		if (step >= left) {
			at = target;
			speed = 0;
			dir = 0;
			trips++;
			return true;
		}
		at += dir * step;
		return false;
	}

	public String save() {
		return at + ";" + travel + ";" + speed + ";" + bell + ";" + dir + ";" + trips;
	}

	public void load(String text) {
		String[] p = text.split(";", -1);
		if (p.length < 6) {
			return;
		}
		try {
			at = Double.parseDouble(p[0]);
			travel = Double.parseDouble(p[1]);
			speed = Double.parseDouble(p[2]);
			bell = Double.parseDouble(p[3]);
			dir = Integer.parseInt(p[4]);
			trips = Integer.parseInt(p[5]);
		} catch (NumberFormatException ex) {
			// a damaged save starts the winder afresh
		}
	}
}
