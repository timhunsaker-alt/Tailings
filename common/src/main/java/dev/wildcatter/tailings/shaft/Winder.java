package dev.wildcatter.tailings.shaft;

import dev.wildcatter.tailings.plant.RockLoad;

/**
 * A double-drum hoist's state (docs/multiblocks/shaft-and-hoist.md, "The hoist"): its two skips in balance, skip 0 on one drum and
 * skip 1 on the other, so as one rises the other falls. {@link #position} is skip 0's height above its loading stop, from 0 to the
 * travel; skip 1 stands at the travel less that. Each skip's load rides here, in kg. Plain Java, unit-tested.
 */
public final class Winder {
	/** The metres between the loading stop and the dump. */
	private double travel;
	/** Skip 0's height above the loading stop, m. */
	private double position;
	/** Rope speed, m/s, and which way skip 0 goes while winding (+1 up, -1 down, 0 at rest). */
	private double speed;
	private int direction;
	/** Seconds of rest still to run at the ends, loading and dumping. */
	private double rest;
	private final RockLoad[] loads = {new RockLoad(), new RockLoad()};
	private int dumps;
	/**
	 * The cage, in its own compartment beside the skips (heavy-iron-fixes): its floor's height above the bottom landing, the metres
	 * between the landings, its speed, which way it goes (0 at rest), the bell still to ring before it goes, and its trips.
	 */
	private double cageAt, cageTravel, cageSpeed, cageBell;
	private int cageDir, cageTrips;

	public double travel() {
		return travel;
	}

	public double position() {
		return position;
	}

	public double speed() {
		return speed;
	}

	public boolean winding() {
		return direction != 0;
	}

	public double rest() {
		return rest;
	}

	/** Skips dumped so far. */
	public int dumps() {
		return dumps;
	}

	public RockLoad load(int skip) {
		return loads[skip];
	}

	/** A skip's height above the loading stop, m. */
	public double height(int skip) {
		return skip == 0 ? position : travel - position;
	}

	/** At rest, the skip at the dump (0 or 1); skip 0 is at the top once it has wound up. */
	public int top() {
		return position >= travel / 2 ? 0 : 1;
	}

	public int bottom() {
		return 1 - top();
	}

	/**
	 * The shaft's travel, as the hoist finds it built. A change while at rest keeps the skips at their ends; a change while winding
	 * (a pocket moved under a running hoist) stops it at the nearer end.
	 */
	public void setTravel(double metres) {
		if (Math.abs(metres - travel) < 1e-9) {
			return;
		}
		boolean up = position >= travel / 2 && travel > 0;
		travel = Math.max(0, metres);
		position = up ? travel : 0;
		speed = 0;
		direction = 0;
	}

	/** Starts a wind: the skip at the bottom goes up. */
	public void start() {
		if (direction == 0 && travel > 0) {
			direction = position >= travel / 2 ? -1 : 1;
		}
	}

	/** The brakes set: the skips stop where they are (the motor stopped or lost its supply mid-wind). */
	public void brake() {
		speed = 0;
	}

	/**
	 * Winds on for {@code dt} seconds with the motor giving {@code strength} of its rating (its speed scales with it): up to rope speed,
	 * braked so it arrives at the creep speed, and the last metres at creep. Returns true on the tick it arrives; the rest starts.
	 */
	public boolean wind(double dt, double strength) {
		if (direction == 0) {
			return false;
		}
		if (strength <= 0) {
			brake();
			return false;
		}
		double target = direction > 0 ? travel : 0;
		double left = Math.abs(target - position);
		double top = Hoisting.ROPE_SPEED * Math.min(1, strength);
		double braking = Math.sqrt(2 * Hoisting.ACCEL * left);
		// up to speed, then braked along the curve that stops it at the end; the last metres at creep
		speed = braking <= Hoisting.CREEP ? Hoisting.CREEP : Math.min(Math.min(top, speed + Hoisting.ACCEL * dt), braking);
		double step = speed * dt;
		if (step >= left) {
			position = target;
			speed = 0;
			direction = 0;
			rest = Hoisting.REST_SECONDS;
			return true;
		}
		position += direction * step;
		return false;
	}

	/** Runs the rest on; true once it is over. */
	public boolean rested(double dt) {
		rest = Math.max(0, rest - dt);
		return rest <= 0;
	}

	/** The skip at the top dumps: its load, which the caller puts in the bin. */
	public RockLoad dump() {
		RockLoad out = loads[top()].take(Double.MAX_VALUE);
		dumps++;
		return out;
	}

	/** The skip at the bottom takes the flask's load. */
	public void loadBottom(RockLoad from) {
		loads[bottom()].add(from);
	}

	/** A skip gone (broken off its rope): its load falls down the shaft and is lost. */
	public void lose(int skip) {
		loads[skip].take(Double.MAX_VALUE);
	}

	// ---- the cage -------------------------------------------------------------------------------------------------------------

	public double cageAt() {
		return cageAt;
	}

	public double cageTravel() {
		return cageTravel;
	}

	public double cageSpeed() {
		return cageSpeed;
	}

	public int cageTrips() {
		return cageTrips;
	}

	/** The cage is ringing to go or on its way: the skips wait for it, as it waits for them. */
	public boolean cageBusy() {
		return cageDir != 0;
	}

	public boolean cageRinging() {
		return cageDir != 0 && cageBell > 0;
	}

	/** At the top landing (the collar)? */
	public boolean cageUp() {
		return cageTravel > 0 && cageAt >= cageTravel - 1e-6;
	}

	public boolean cageAtLanding() {
		return cageDir == 0 && (cageAt <= 1e-6 || cageUp());
	}

	/** The landings as the hoist finds them; a change keeps the cage at its landing. */
	public void setCageTravel(double metres) {
		if (Math.abs(metres - cageTravel) < 1e-9) {
			return;
		}
		boolean up = cageTravel <= 0 || cageAt >= cageTravel / 2;   // a cage first found is where it was hung, at the collar
		cageTravel = Math.max(0, metres);
		cageAt = up ? cageTravel : 0;
		cageSpeed = 0;
		cageDir = 0;
	}

	/** Calls the cage to the top landing (true) or the bottom: the bell rings, then it goes. False when it is there or already going. */
	public boolean callCage(boolean top) {
		if (cageDir != 0 || cageTravel <= 0) {
			return false;
		}
		if (top ? cageUp() : cageAt <= 1e-6) {
			return false;
		}
		cageDir = top ? 1 : -1;
		cageBell = Hoisting.CAGE_BELL_SECONDS;
		return true;
	}

	/** Sends the cage to the other landing (a rider's bell). */
	public boolean sendCage() {
		return callCage(!cageUp());
	}

	/** Rings the bell on, then winds the cage for {@code dt} s at the man-riding speed; true on the tick it arrives at its landing. */
	public boolean windCage(double dt, double strength) {
		if (cageDir == 0) {
			return false;
		}
		if (cageBell > 0) {
			cageBell = Math.max(0, cageBell - dt);
			return false;
		}
		if (strength <= 0) {
			cageSpeed = 0;
			return false;
		}
		double target = cageDir > 0 ? cageTravel : 0;
		double left = Math.abs(target - cageAt);
		double top = Hoisting.CAGE_SPEED * Math.min(1, strength);
		double braking = Math.sqrt(2 * Hoisting.ACCEL * left);
		cageSpeed = braking <= Hoisting.CREEP ? Hoisting.CREEP : Math.min(Math.min(top, cageSpeed + Hoisting.ACCEL * dt), braking);
		double step = cageSpeed * dt;
		if (step >= left) {
			cageAt = target;
			cageSpeed = 0;
			cageDir = 0;
			cageTrips++;
			return true;
		}
		cageAt += cageDir * step;
		return false;
	}

	public String save() {
		return travel + ";" + position + ";" + speed + ";" + direction + ";" + rest + ";" + dumps + ";" + loads[0].save() + ";" + loads[1].save()
				+ ";" + cageAt + ";" + cageTravel + ";" + cageSpeed + ";" + cageBell + ";" + cageDir + ";" + cageTrips;
	}

	public void load(String text) {
		String[] p = text.split(";", -1);
		if (p.length < 8) {
			return;
		}
		try {
			travel = Double.parseDouble(p[0]);
			position = Double.parseDouble(p[1]);
			speed = Double.parseDouble(p[2]);
			direction = Integer.parseInt(p[3]);
			rest = Double.parseDouble(p[4]);
			dumps = Integer.parseInt(p[5]);
			loads[0] = new RockLoad();
			loads[0].load(p[6]);
			loads[1] = new RockLoad();
			loads[1].load(p[7]);
			if (p.length >= 14) {   // saved with a cage
				cageAt = Double.parseDouble(p[8]);
				cageTravel = Double.parseDouble(p[9]);
				cageSpeed = Double.parseDouble(p[10]);
				cageBell = Double.parseDouble(p[11]);
				cageDir = Integer.parseInt(p[12]);
				cageTrips = Integer.parseInt(p[13]);
			}
		} catch (NumberFormatException ex) {
			// a damaged save starts the hoist afresh
		}
	}
}
