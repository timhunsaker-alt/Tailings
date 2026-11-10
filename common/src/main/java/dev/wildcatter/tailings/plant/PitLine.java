package dev.wildcatter.tailings.plant;

/**
 * The pit-sized plant line's rates, holds and sizing (docs/multiblocks/primary-gyratory-stockpile.md, sag-mill-line.md; decisions in
 * docs/questions/p7-pit-line.md). A figure marked (proposed) is so marked in the specs. Plain Java: no voltage is written here; a motor's
 * tier is its {@link Machine.Supply}, read from Wildcatter's electrical standard.
 */
public final class PitLine {
	private PitLine() {
	}

	/** The line's rate: 1,000 t/h, 24,000 t a day, a mid-size copper concentrator (P7-2, proposed). */
	public static final double TONNES_PER_HOUR = 1000;
	public static final double KG_PER_SECOND = TONNES_PER_HOUR * 1000 / 3600;
	public static final double KG_PER_TICK = KG_PER_SECOND / 20;

	/** kg a tick of {@code tonnesPerHour}. */
	public static double kgPerTick(double tonnesPerHour) {
		return tonnesPerHour * 1000 / 3600 / 20;
	}

	// ---- P7a: the gyratory, its pockets and the stockpile -------------------------------------------------------------------------

	/** The 42-65 class gyratory crushes at about 2,400 t/h when fed (the bottom of 2,354-3,362 t/h at 152 mm open side). */
	public static final double GYRATORY_TPH = 2400;
	/** The direct-tip pocket over the bowl takes one 363 t truck load and more: 400 t (proposed), as the pit hopper. */
	public static final int POCKET_KG = 400_000;
	/** The surge pocket under the crusher: 500 t of crushed rock (proposed). */
	public static final double SURGE_KG = 500_000;
	/** One bank block of hard rock in 200 is a boulder that bridges the bowl (P7-8, proposed). */
	public static final int BOULDER_EVERY = 200;
	/** The pedestal rock breaker clears a bridge in 30 s (P7-8, proposed). */
	public static final int BREAK_SECONDS = 30;
	/** The 2.4 m primary apron feeder, up to 3,000 t/h (proposed). */
	public static final double PRIMARY_FEEDER_TPH = 3000;

	/** The 1,400 mm heavy belt: 4 m/s, up to 3,000 t/h (P7-9, proposed). */
	public static final double HEAVY_BELT_TPH = 3000;
	public static final double HEAVY_BELT_METRES_PER_SECOND = 4;
	/** kg on a metre of heavy belt carrying its full 3,000 t/h at 4 m/s: about 208 kg. */
	public static final double HEAVY_KG_PER_METRE = HEAVY_BELT_TPH * 1000 / 3600 / HEAVY_BELT_METRES_PER_SECOND;
	/** One heavy conveyor drive: a 355 kW motor (proposed). */
	public static final int HEAVY_DRIVE_WATTS = 355_000;
	/** A heavy belt run's flat power: 75 kW for each 30 m (proposed). */
	public static final double HEAVY_FLAT_WATTS_PER_30M = 75_000;
	/** The steepest a troughed belt carries crushed rock: about 18 degrees, 1 in 3. */
	public static final int HEAVY_MOST_RISES_IN_THREE = 1;
	/** Lift efficiency of a belt drive (proposed): m g h over 0.9. */
	public static final double LIFT_EFFICIENCY = 0.9;
	public static final double G = 9.81;

	/** Watts a heavy belt run of {@code metres} lifting {@code rise} m at {@code tonnesPerHour} takes: 75 kW a 30 m plus m g h / 0.9. */
	public static double heavyBeltWatts(int metres, int rise, double tonnesPerHour) {
		double flat = HEAVY_FLAT_WATTS_PER_30M * metres / 30.0;
		double lift = tonnesPerHour * 1000 / 3600 * G * Math.max(0, rise) / LIFT_EFFICIENCY;
		return flat + lift;
	}

	/** The 355 kW drives a heavy run needs at its full 3,000 t/h, rounded up, at least one. */
	public static int heavyDrivesFor(int metres, int rise) {
		return Math.max(1, (int) Math.ceil(heavyBeltWatts(metres, rise, HEAVY_BELT_TPH) / HEAVY_DRIVE_WATTS - 1e-9));
	}

	/** What a heavy run holds when full: its length at 208 kg a metre, at least 30 s of the line's rate. */
	public static double heavyBeltHoldKg(int metres) {
		return Math.max(30 * KG_PER_SECOND, metres * HEAVY_KG_PER_METRE);
	}

	/** A stockpile block holds three quarters of a bank block, the swell of broken rock, as muck does: 2,025 kg. */
	public static final int STOCKPILE_QUARTERS = 3;
	public static final double STOCKPILE_BLOCK_KG = STOCKPILE_QUARTERS * PlantRates.BLOCK_KG / 4;
	/** The settle rule's long slope, 3 in 4: 36.9 degrees. */
	public static final double REPOSE_SLOPE = 0.75;

	/** The base radius of a cone pile {@code height} m high at the settle rule's 3 in 4. */
	public static double coneRadius(double height) {
		return height / REPOSE_SLOPE;
	}

	/** The volume of that cone, m³. */
	public static double coneVolume(double height) {
		double r = coneRadius(height);
		return Math.PI * r * r * height / 3;
	}

	/** The tonnes that cone holds at 2,025 kg a block. */
	public static double coneTonnes(double height) {
		return coneVolume(height) * STOCKPILE_BLOCK_KG / 1000;
	}

	/** A 1.5 m reclaim apron feeder, up to 500 t/h (proposed). */
	public static final double RECLAIM_FEEDER_TPH = 500;
	/** The belt weigher's RATE spans 0 to 2,000 t/h; its default setpoint is the line's 1,000 t/h. */
	public static final double WEIGHER_SPAN_TPH = 2000;
	public static final double WEIGHER_DEFAULT_TPH = TONNES_PER_HOUR;
	/** Its TOTAL pulses once each 100 t. */
	public static final double WEIGHER_PULSE_KG = 100_000;
	/** The setpoints its screen steps through, t/h: 50 is the small plant's rate, for P7a feeding it before the SAG line (P7-10). */
	public static final double[] SETPOINT_STEPS = {50, 250, 500, 750, 1000, 1250, 1500};

	/** The step after {@code now}, wrapping. */
	public static double nextSetpoint(double now) {
		for (double step : SETPOINT_STEPS) {
			if (step > now + 1e-6) {
				return step;
			}
		}
		return SETPOINT_STEPS[0];
	}

	/** Each running feeder's share of the setpoint: split evenly, never over its own 500 t/h. */
	public static double feederShareTph(double setpointTph, int feeders) {
		return feeders <= 0 ? 0 : Math.min(RECLAIM_FEEDER_TPH, setpointTph / feeders);
	}

	// ---- P7b: the SAG mill line ---------------------------------------------------------------------------------------------------

	/** The 32 ft x 15 ft SAG: 8.2 MW on two 4.1 MW pinions. */
	public static final int SAG_MOTOR_WATTS = 4_100_000;
	/** The 24 ft x 36 ft overflow ball mill: about 10.5 MW on two 5.25 MW pinions (proposed). */
	public static final int BALL_MILL_MOTOR_WATTS = 5_250_000;
	/** Pebbles, the +12 mm the SAG cannot break, are 20 % of what it grinds of crushed rock (proposed: real 10-30 %). */
	public static final double PEBBLE_SHARE = 0.20;
	/** With the pebble crusher off, the SAG's rate falls by 15 % (proposed: the real gain is 10-25 %). */
	public static final double NO_PEBBLE_CRUSHER = 0.85;
	/** The SAG grinds at 75 % solids; its trommel undersize goes to the pump box at that density. */
	public static final double SAG_SOLIDS = 0.75;
	/** The pebble crusher's rate, about twice its usual 200 t/h load (proposed: a 300 kW cone). */
	public static final double PEBBLE_CRUSHER_TPH = 400;

	/**
	 * The SAG's ore charge. Its inside, about 9.5 m by 4.6 m inside its liners, is about 326 m³; at its 28 % filling with 12 % balls the
	 * rock is 16 % of that at about 1.6 t a m³ of broken rock: about 85 t (proposed). OVERLOAD is the spec's 35 % filling: 23 % rock, so
	 * 85 x 23 / 16, about 122 t.
	 */
	public static final double SAG_INSIDE_M3 = Math.PI * 4.75 * 4.75 * 4.6;
	public static final double CHARGE_BULK_T_PER_M3 = 1.6;
	public static final double SAG_FILLING = 0.28, SAG_BALLS = 0.12, SAG_OVERLOAD_FILLING = 0.35;
	public static final double SAG_NORMAL_CHARGE_KG = SAG_INSIDE_M3 * (SAG_FILLING - SAG_BALLS) * CHARGE_BULK_T_PER_M3 * 1000;
	public static final double SAG_OVERLOAD_KG = SAG_NORMAL_CHARGE_KG * (SAG_OVERLOAD_FILLING - SAG_BALLS) / (SAG_FILLING - SAG_BALLS);
	/** It takes no more feed past its full 40 % (proposed): the feed belt stops. */
	public static final double SAG_MOST_CHARGE_KG = SAG_NORMAL_CHARGE_KG * (0.40 - SAG_BALLS) / (SAG_FILLING - SAG_BALLS);
	/** OVERLOAD pulls the belt weigher's setpoint back by 10 % until it clears (real SAG weight control). */
	public static final double OVERLOAD_FALLBACK = 0.10;

	/**
	 * What the SAG grinds a tick: its rated rate times its motors' output and the pebble crusher's factor, times how full its charge is
	 * against its normal charge (a lightly charged mill grinds less), never more than it holds.
	 */
	public static double sagGrindKg(double chargeKg, double strength, boolean pebbleCrusher) {
		double rated = KG_PER_TICK * strength * (pebbleCrusher ? 1 : NO_PEBBLE_CRUSHER);
		return Math.min(chargeKg, rated * Math.min(1, chargeKg / SAG_NORMAL_CHARGE_KG));
	}

	/** The cyclones' circulating load on the line, 250 %, as the small plant's: the ball mill takes 2.5 t for each t of new feed. */
	public static final double CIRCULATING_LOAD = PlantRates.CIRCULATING_LOAD;
	/** The ball mill discharges at 70 %, the overflow leaves at 35 %, as the small plant's. */
	public static final double MILL_SOLIDS = PlantRates.MILL_SOLIDS;
	public static final double OVERFLOW_SOLIDS = PlantRates.OVERFLOW_SOLIDS;
	/** The ball mill grinds the underflow: 2,500 t/h at the line's rate. */
	public static final double BALL_MILL_TPH = TONNES_PER_HOUR * CIRCULATING_LOAD;
	/** The cyclone feed: what the pump box sends, 3,500 t/h of solids. */
	public static final double CYCLONE_FEED_TPH = TONNES_PER_HOUR * (1 + CIRCULATING_LOAD);

	/**
	 * The cyclone feed's solids by mass, so that the underflow leaves at the mill's 70 % and the overflow at 35 % with no water of
	 * their own: (1 + CL) kg of solids carry CL kg of underflow's water and 1 kg of overflow's. About 54 %, inside real cyclone feeds' 50-60 %.
	 */
	public static double cycloneFeedSolids() {
		double water = CIRCULATING_LOAD * PlantRates.waterPerKg(MILL_SOLIDS) + PlantRates.waterPerKg(OVERFLOW_SOLIDS);
		double solids = 1 + CIRCULATING_LOAD;
		return solids / (solids + water);
	}

	/** Litres the pump box adds to each kg of solids that comes to it at {@code inSolids}, to make the cyclone feed. */
	public static double pumpWaterPerKg(double inSolids) {
		return Math.max(0, PlantRates.waterPerKg(cycloneFeedSolids()) - PlantRates.waterPerKg(inSolids));
	}

	/** The fresh water the line takes, L/s at its rate: the SAG's and the pump box's; it all leaves in the overflow. */
	public static double freshWaterLitresPerSecond() {
		return KG_PER_SECOND * PlantRates.waterPerKg(OVERFLOW_SOLIDS);
	}

	/** Solids' density in the slurry, t/m³ (2.7 as the plant counts a block). */
	public static final double SOLIDS_DENSITY = 2.7;

	/** m³/s of overflow slurry at the line's rate: its solids' volume and its water. */
	public static double overflowM3PerSecond() {
		return KG_PER_SECOND / 1000 / SOLIDS_DENSITY + freshWaterLitresPerSecond() / 1000;
	}

	/** Bond's law for the ball mill: from the SAG's transfer size, T80 2 mm, to 150 µm at Wi 15: about 8.9 kWh/t. */
	public static double ballMillKwhPerTonne() {
		return PlantRates.bondKwhPerTonne(15, 150, 2000);
	}

	/** A mill's power scaled from one of known size: power goes with D^2.5 times L (Bond, at the same filling and speed). */
	public static double scaledMillWatts(double knownWatts, double knownDiameter, double knownLength, double diameter, double length) {
		return knownWatts * Math.pow(diameter / knownDiameter, 2.5) * (length / knownLength);
	}

	/** Slurry parts on the line hold about 10 s of their flow, as the small plant's. */
	public static final double SLURRY_HOLD_SECONDS = PlantRates.SLURRY_HOLD_SECONDS;
}
