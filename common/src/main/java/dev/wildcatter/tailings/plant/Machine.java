package dev.wildcatter.tailings.plant;

/**
 * One true-size part of the plant or the dump station (docs/multiblocks/crushing-circuit.md, flotation-bank.md, dump-station.md).
 * Sizes are the spec's cells, W x H x L (1 block = 1 m). Watts are the real motor; 0 is unpowered steel. {@link Supply#MEDIUM} is
 * the 4.16 kV tier and {@link Supply#INDUSTRIAL} the 480 V tier, read from Wildcatter's tiers, never as a number.
 */
public enum Machine {
	GRIZZLY("grizzly", "Grizzly", 3, 2, 4, 0, Supply.NONE),
	JAW("jaw_crusher", "Jaw Crusher", 3, 4, 4, 110_000, Supply.INDUSTRIAL),
	CONE("cone_crusher", "Cone Crusher", 3, 4, 3, 132_000, Supply.INDUSTRIAL),
	SCREEN("double_deck_screen", "Double-Deck Screen", 3, 3, 6, 22_000, Supply.INDUSTRIAL),
	/** The head drive of a belt run; R3 rates it for one 30 m run, R4 sizes it by the run's length. */
	CONVEYOR_DRIVE("conveyor_drive", "Conveyor Drive", 1, 1, 2, 7_500, Supply.INDUSTRIAL),
	BALL_MILL("ball_mill", "Ball Mill", 4, 4, 6, 600_000, Supply.MEDIUM),
	CYCLONE("cyclone_cluster", "Cyclone Cluster", 2, 4, 2, 45_000, Supply.INDUSTRIAL),
	/** The 2x route at plant scale: 24 twin-start spirals on the cyclones' overflow, with a 37 kW feed pump (gravity-circuit.md). */
	SPIRAL_BANK("spiral_bank", "Spiral Bank", 4, 5, 6, 37_000, Supply.INDUSTRIAL),
	CONDITIONER("conditioning_tank", "Conditioning Tank", 3, 4, 3, 15_000, Supply.INDUSTRIAL),
	ACTIVATION_TANK("activation_tank", "Activation Tank", 3, 4, 3, 15_000, Supply.INDUSTRIAL),
	/** Eight 20 m3 cells, 8 x 45 kW of agitators on one motor box. */
	FLOTATION_BANK("flotation_bank", "Flotation Bank", 7, 4, 13, 360_000, Supply.INDUSTRIAL),
	BLOWER("air_blower", "Air Blower", 2, 2, 3, 75_000, Supply.INDUSTRIAL),
	THICKENER("concentrate_thickener", "Concentrate Thickener", 8, 3, 8, 5_500, Supply.INDUSTRIAL),
	FILTER("pressure_filter", "Pressure Filter", 3, 3, 5, 15_000, Supply.INDUSTRIAL),
	/** Phase 4 (full-recovery.md): four 20 m3 cells on the bank's tails, 4 x 45 kW. */
	SCAVENGER_BANK("scavenger_bank", "Scavenger Bank", 5, 4, 9, 180_000, Supply.INDUSTRIAL),
	REGRIND_MILL("regrind_mill", "Regrind Mill", 3, 3, 5, 150_000, Supply.INDUSTRIAL),
	CLEANER_BANK("cleaner_bank", "Cleaner Bank", 4, 3, 6, 44_000, Supply.INDUSTRIAL),
	LEACH_TANK("leach_tank", "Leach Tank", 5, 6, 5, 30_000, Supply.INDUSTRIAL),
	CCD_THICKENER("ccd_thickener", "CCD Thickener", 12, 4, 12, 7_500, Supply.INDUSTRIAL),
	ELECTROWINNING_BANK("electrowinning_bank", "Electrowinning Bank", 5, 3, 9, 400_000, Supply.INDUSTRIAL),
	/** Phase 4c: roasting, leaching and cementing out what concentrates carry (proposed: 250 kW). */
	BYPRODUCT_PLANT("byproduct_plant", "By-Product Plant", 5, 5, 8, 250_000, Supply.INDUSTRIAL),
	/** Phase 4b: a lined heap of waste rock under acid sprays, at a tenth of the plant's rate. Its pump is 15 kW (proposed). */
	HEAP_PAD("heap_pad", "Heap Leach Pad", 10, 3, 10, 15_000, Supply.INDUSTRIAL),
	/** The earthen pond from before Wildcatter's lined cell (Phase 4d): a legacy part, with no recipe and out of the creative tab; an old plant's pond goes on working. */
	TAILINGS_POND("tailings_pond_placeholder", "Tailings Pond (legacy)", 6, 2, 6, 0, Supply.NONE),
	DUMP_RAMP("dump_ramp", "Dump Ramp", 1, 1, 4, 0, Supply.NONE),
	DUMP_POCKET("dump_pocket", "Dump Pocket", 4, 3, 3, 0, Supply.NONE),
	APRON_FEEDER("apron_feeder", "Apron Feeder", 2, 1, 4, 11_000, Supply.INDUSTRIAL),
	ORE_SORTER("ore_sorter", "Ore Sorter", 3, 3, 3, 40_000, Supply.INDUSTRIAL),
	ORE_BIN("ore_bin", "Ore Bin", 3, 6, 3, 0, Supply.NONE),
	/** The truck dump's concrete retaining wall: 3 m of wall below the pad and its 1.5 m bumper above, in two rows (open-pit.md). */
	TRUCK_DUMP_WALL("truck_dump_wall", "Truck Dump Wall", 12, 5, 2, 0, Supply.NONE),
	/** The truck dump's 400 t hopper, drawn by an apron feeder touching it (open-pit.md). */
	PIT_HOPPER("pit_hopper", "Pit Hopper", 10, 6, 6, 0, Supply.NONE),
	/** The pit's diesel dispenser island and its 2,000 L day tank, fed by pipe (open-pit.md, P2a). */
	FUEL_BAY("fuel_bay", "Fuel Bay", 4, 3, 2, 0, Supply.NONE),
	/** The pit's haul dispatch office (open-pit.md, P2b): a small building on the control cable that runs the fleet by radio. Its load is on the low tier, not a motor. */
	HAUL_DISPATCH("haul_dispatch", "Haul Dispatch Office", 3, 3, 3, 0, Supply.NONE),
	/** The emulsion truck's filling point (open-pit-p3-blasting.md, P3): a standpipe and hose on Wildcatter pipe from the matrix tank. */
	MATRIX_FILL_POINT("matrix_fill_point", "Matrix Fill Point", 1, 2, 1, 0, Supply.NONE),
	/** The dry stack (open-pit-p4-dry-stack.md): a 15 m high-rate thickener on the plant's tails, its rake drive 11 kW (proposed). */
	TAILINGS_THICKENER("tailings_thickener", "Tailings Thickener", 15, 5, 15, 11_000, Supply.INDUSTRIAL),
	/** A plate-and-frame press with 2.5 m plates, 60 t/h in 15-minute batches, 150 kW (open-pit.md, proposed). */
	TAILINGS_FILTER("tailings_filter", "Tailings Filter", 6, 6, 24, 150_000, Supply.INDUSTRIAL),
	/** The load-out's legs over a 10 m wide, 9 m clear drive-through lane, with the clamshell gate under its deck: the gate's 7.5 kW hydraulics. */
	LOADOUT_FRAME("loadout_frame", "Load-Out Frame", 12, 10, 12, 7_500, Supply.INDUSTRIAL),
	/** The load-out's 400 t steel bin of dry tails, on the frame. */
	LOADOUT_BIN("loadout_bin", "Load-Out Bin", 10, 7, 10, 0, Supply.NONE),
	/**
	 * The pit switch house (open-pit-p5-big-machines.md, P5a): a skid house on the pit's medium-voltage loop, 3 x 3 x 6, the medium
	 * voltage in on its north face and four breaker outlets for trailing cables on its south. It is no motor: its machines' draw is theirs.
	 */
	PIT_SWITCH_HOUSE("pit_switch_house", "Pit Switch House", 3, 3, 6, 0, Supply.NONE),
	/**
	 * The electric rope shovel's six parts (open-pit-p5-big-machines.md, P5d), built as the dragline will be and lifted into one machine
	 * when it runs. Its power is its trailing cable's, not a motor box: the crawlers' main cell holds its reel and its panel.
	 */
	SHOVEL_CRAWLERS("shovel_crawlers", "Shovel Crawlers", 14, 3, 12, 0, Supply.NONE),
	/** The revolving frame and machinery house: the hoist, crowd and swing motors on their drives, the transformer, the cab. */
	SHOVEL_HOUSE("shovel_house", "Shovel House", 12, 9, 15, 0, Supply.NONE),
	/** The A-frame gantry on the house that holds the boom's suspension ropes. */
	SHOVEL_GANTRY("shovel_gantry", "Shovel Gantry", 6, 6, 3, 0, Supply.NONE),
	/** The 20 m boom, laid on the ground ahead of the house until the machine runs. */
	SHOVEL_BOOM("shovel_boom", "Shovel Boom", 3, 3, 20, 0, Supply.NONE),
	/** The dipper handle, crowded in and out through the boom's saddle block. */
	SHOVEL_HANDLE("shovel_handle", "Shovel Dipper Handle", 2, 2, 14, 0, Supply.NONE),
	/** The 55 m³ dipper with its door and teeth. */
	SHOVEL_DIPPER("shovel_dipper", "Shovel Dipper", 5, 5, 5, 0, Supply.NONE),
	/** A rail bridge over a dump pocket, its 300 mm bars between and under the rails (bottom-dump-cars.md, L3): mine rail runs on it. */
	TRACK_GRIZZLY("track_grizzly", "Track Grizzly", 1, 1, 4, 0, Supply.NONE),
	/** The cam rail beside a track grizzly that opens each bottom-dump car's doors, with its closing ramp at the far end (L3). */
	DUMP_CAM_RAIL("dump_cam_rail", "Dump Cam Rail", 1, 1, 4, 0, Supply.NONE),
	/** A traction battery charger on a wall, plugged into a three-phase 30 A receptacle, 20 kW (battery-locomotive.md, L2). */
	BATTERY_CHARGER("battery_charger", "Battery Charger", 2, 2, 1, 0, Supply.NONE),
	/** The drive's forcing fan at the portal (ventilation.md, L1): a 1.8 m axial fan with its silencer and inlet bell, 75 kW; its outlet on its south face. */
	VENT_FAN("vent_fan", "Ventilation Fan", 3, 3, 5, 75_000, Supply.INDUSTRIAL),
	/** An in-line booster in the duct for long drives (ventilation.md, L1), 30 kW (proposed): in at its north face, out at its south. */
	BOOSTER_FAN("booster_fan", "Booster Fan", 2, 2, 3, 30_000, Supply.INDUSTRIAL),
	/** A steel four-post headframe over a 5 x 5 shaft, its two head sheaves at the top and the skip dump scroll (shaft-and-hoist.md, L4; proposed: 30 m high). */
	HEADFRAME("headframe", "Headframe", 9, 30, 9, 0, Supply.NONE),
	/** The 100 t steel bin beside the headframe that the skips dump into, feeding a belt under it (L4, proposed). */
	HEADFRAME_BIN("headframe_bin", "Headframe Bin", 4, 5, 4, 0, Supply.NONE),
	/** The hoist house: a double-drum hoist, drums about 3 m across, on a 450 kW motor at the medium-voltage tier (L4, proposed). */
	HOIST_HOUSE("hoist_house", "Hoist House", 10, 8, 12, dev.wildcatter.tailings.shaft.Hoisting.MOTOR_WATTS, Supply.MEDIUM),
	/** The loading pocket's measuring flask beside the shaft bottom: it fills to one skip's 3 t and empties into the skip (L4). */
	MEASURING_FLASK("measuring_flask", "Measuring Flask", 2, 3, 2, 0, Supply.NONE),
	/** A vacuum disc filter with its vacuum pump, taking thickened tails to an 80 % cake for the paste mixer (paste-backfill.md, L5; proposed 75 kW). */
	PASTE_FILTER("paste_filter", "Paste Filter", 5, 5, 8, 75_000, Supply.INDUSTRIAL),
	/** A continuous twin-shaft paste mixer with its cement hopper on top (L5; proposed 75 kW). */
	PASTE_MIXER("paste_mixer", "Paste Mixer", 3, 3, 5, 75_000, Supply.INDUSTRIAL),
	/** A hydraulic piston pump, 35 m3/h at 8 MPa, pushing paste down the fill line (L5; 110 kW). */
	PASTE_PUMP("paste_pump", "Paste Pump", 3, 3, 6, dev.wildcatter.tailings.paste.PasteFill.PUMP_WATTS, Supply.INDUSTRIAL),
	/** The dry mill (dry-mill.md, L6): the spiral bank's twin on its tails, another 45 % of the heavy minerals to HMC; a 37 kW feed pump (proposed). */
	SCAVENGER_SPIRAL_BANK("scavenger_spiral_bank", "Scavenger Spiral Bank", 4, 5, 6, 37_000, Supply.INDUSTRIAL),
	/** A gas-fired fluid-bed dryer for HMC at about 120 °C, 0.3 GJ a tonne; its 30 kW fluidising blower is the motor (L6, proposed). */
	FLUID_BED_DRYER("fluid_bed_dryer", "Fluid-Bed Dryer", 3, 4, 6, 30_000, Supply.INDUSTRIAL),
	/** A 1.5 m rare-earth roll magnet, 5.5 kW: takes the ilmenite (L6, proposed). */
	RER_SEPARATOR("rer_separator", "Rare-Earth Roll Magnet", 2, 3, 3, 5_500, Supply.INDUSTRIAL),
	/** A 1.5 m high-tension roll separator with its own high-voltage supply, 7.5 kW: takes the conductors, rutile and cassiterite (L6, proposed). */
	HT_ROLL_SEPARATOR("ht_roll_separator", "High-Tension Roll Separator", 2, 3, 4, 7_500, Supply.INDUSTRIAL),
	/** A 1.5 m induced-roll magnet at high intensity, 11 kW: takes the monazite; the zircon passes (L6, proposed). */
	IRM_SEPARATOR("irm_separator", "Induced-Roll Magnet", 2, 3, 3, 11_000, Supply.INDUSTRIAL),
	/** Dense-medium separation (dense-medium-separation.md, L7): a wet vibrating screen at 1 mm with spray bars on the gravel, 11 kW (proposed). */
	DESLIME_SCREEN("deslime_screen", "Deslime Screen", 2, 2, 4, 11_000, Supply.INDUSTRIAL),
	/** The mixing box, a 55 kW medium pump and one 610 mm DMS cyclone on its stand, about 60 t/h of gravel (L7, unverified). */
	DMS_MODULE("dms_cyclone_module", "DMS Cyclone Module", 3, 6, 4, 55_000, Supply.INDUSTRIAL),
	/** A drain-and-rinse screen, 7.5 kW (L7, proposed): one on the module's north side for the floats, one on its south side for the sinks. */
	DRAIN_RINSE_SCREEN("drain_rinse_screen", "Drain-and-Rinse Screen", 2, 2, 5, 7_500, Supply.INDUSTRIAL),
	/** A wet drum magnetic separator and densifier that win the ferrosilicon back from the rinse water, its make-up hopper on top, 5.5 kW (L7, proposed). */
	MEDIUM_RECOVERY("medium_recovery", "Medium Recovery", 2, 3, 3, 5_500, Supply.INDUSTRIAL),
	/** A 1.2 m X-ray transmission ore sorter (xrt-ore-sorter.md, L8): feeder, 3 m/s belt, X-ray line and 192 air ejectors; 20 kW (unverified). */
	XRT_SORTER("xrt_sorter", "X-Ray Ore Sorter", 4, 5, 8, 20_000, Supply.INDUSTRIAL),
	/** The sorter's screw compressor and air receiver for its ejectors, 160 kW (L8, proposed). */
	SORTER_COMPRESSOR("sorter_compressor", "Sorter Air Compressor", 2, 2, 3, 160_000, Supply.INDUSTRIAL),
	/**
	 * The pit line's primary crusher (primary-gyratory-stockpile.md, P7a): a 42-65 class gyratory, 1,066 mm feed opening, about 120 t,
	 * crushing about 2,400 t/h when fed; its bowl at its top under the dump pocket, its 450 kW motor at the medium-voltage tier.
	 */
	GYRATORY_CRUSHER("gyratory_crusher", "Gyratory Crusher", 6, 10, 6, 450_000, Supply.MEDIUM),
	/** The concrete direct-tip pocket over the gyratory's bowl, holding a truck load and more, 400 t (P7a, proposed). */
	GYRATORY_POCKET("gyratory_pocket", "Gyratory Dump Pocket", 14, 8, 10, 0, Supply.NONE),
	/** A pedestal hydraulic rock breaker on the pocket's rim, 75 kW (P7a, proposed): it breaks a boulder that bridges the bowl. */
	ROCK_BREAKER("rock_breaker", "Rock Breaker", 2, 3, 6, 75_000, Supply.INDUSTRIAL),
	/** The steel-lined surge pocket under the gyratory, 500 t of crushed rock (P7a, proposed). */
	SURGE_POCKET("surge_pocket", "Surge Pocket", 8, 6, 8, 0, Supply.NONE),
	/** A 2.4 m apron feeder from the surge pocket onto the stockpile conveyor, up to 3,000 t/h, 150 kW (P7a, proposed). */
	PRIMARY_APRON_FEEDER("primary_apron_feeder", "Primary Apron Feeder", 3, 2, 12, 150_000, Supply.INDUSTRIAL),
	/** The head pulley and drive of a 1,400 mm heavy belt run, 355 kW at the medium-voltage tier (P7a, proposed). */
	HEAVY_CONVEYOR_DRIVE("heavy_conveyor_drive", "Heavy Conveyor Drive", 2, 2, 3, dev.wildcatter.tailings.plant.PitLine.HEAVY_DRIVE_WATTS, Supply.MEDIUM),
	/** The head frame and chute over the coarse-ore stockpile: it drops the belt's load onto the pile under it (P7a). */
	STOCKPILE_HEAD("stockpile_head", "Stockpile Head", 3, 4, 4, 0, Supply.NONE),
	/** A 1.5 m apron feeder in the reclaim tunnel under an opening in its roof, 500 t/h, 55 kW (P7a, proposed). */
	RECLAIM_FEEDER("reclaim_feeder", "Reclaim Feeder", 2, 2, 6, 55_000, Supply.INDUSTRIAL),
	/** A belt scale on the SAG feed belt: it reads t/h, and the reclaim feeders follow its setpoint (P7a). */
	BELT_WEIGHER("belt_weigher", "Belt Weigher", 2, 1, 2, 0, Supply.NONE),
	/**
	 * The 9.75 m x 4.6 m (32 ft x 15 ft EGL) SAG mill and its trommel (sag-mill-line.md, P7b): 8.2 MW on a twin-pinion drive, two
	 * 4.1 MW motors at the medium-voltage tier, their boxes on its west face. {@link #watts} is one motor.
	 */
	SAG_MILL("sag_mill", "SAG Mill", 11, 11, 14, dev.wildcatter.tailings.plant.PitLine.SAG_MOTOR_WATTS, Supply.MEDIUM),
	/** A 300 kW class cone crusher at a 12 mm closed side, crushing the SAG's pebbles (P7b, unverified). */
	PEBBLE_CRUSHER("pebble_crusher", "Pebble Crusher", 4, 5, 4, 300_000, Supply.MEDIUM),
	/** A 7.3 m x 11 m (24 ft x 36 ft) overflow ball mill on two 5.25 MW pinions (P7b, proposed). {@link #watts} is one motor. */
	BALL_MILL_LARGE("ball_mill_large", "Large Ball Mill", 9, 9, 16, dev.wildcatter.tailings.plant.PitLine.BALL_MILL_MOTOR_WATTS, Supply.MEDIUM),
	/** The cyclone feed sump and its 600 kW slurry pump, about 1 m³/s at 25 m head (P7b, proposed). */
	CYCLONE_FEED_PUMP("cyclone_feed_pump", "Cyclone Feed Pump", 4, 4, 5, 600_000, Supply.MEDIUM),
	/** Twelve 660 mm hydrocyclones on a ring manifold, ten running, cut at 150 µm; the pump drives them (P7b, proposed). */
	CYCLONE_CLUSTER_LARGE("cyclone_cluster_large", "Large Cyclone Cluster", 6, 6, 6, 0, Supply.NONE);

	/** Motors on the part: the two pinions of the big mills' twin-pinion drives, one for every other part. */
	public int motors() {
		return this == SAG_MILL || this == BALL_MILL_LARGE ? 2 : 1;
	}

	public enum Supply {
		NONE,
		INDUSTRIAL,
		MEDIUM
	}

	private final String id;
	private final String name;
	private final int width;
	private final int height;
	private final int depth;
	private final int watts;
	private final Supply supply;

	Machine(String id, String name, int width, int height, int depth, int watts, Supply supply) {
		this.id = id;
		this.name = name;
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.watts = watts;
		this.supply = supply;
	}

	public String id() {
		return id;
	}

	public String displayName() {
		return name;
	}

	public int width() {
		return width;
	}

	public int height() {
		return height;
	}

	public int depth() {
		return depth;
	}

	public int watts() {
		return watts;
	}

	public Supply supply() {
		return supply;
	}

	public int cells() {
		return width * height * depth;
	}
}
