# Changelog

All notable changes are the merged pull requests on `main`. The mod version is still 0.1.0. Dates are the merge dates in the repository history (2026-11-02 and 2026-11-03).

## [Unreleased]

### Open pit P6 — the walking dragline (2026-11-10)

Built while Tim was away on the approved spec `docs/multiblocks/dragline.md`; decisions in `docs/questions/p6-dragline.md`.

- **Walking Dragline:** build its parts (`dragline_tub`, `dragline_house`, two `dragline_shoe`, `dragline_mast`, five
  `dragline_boom_section`, `dragline_bucket`), couple a trailing cable to the tub, set its strip on the tub's panel (width, length, the
  seam's depth, the side to cast to) and press RUN: it lifts into one machine with a 75 m boom and a 33 m³ bucket. Each 55 s cycle it
  drags 25 bank blocks of overburden out of the strip, top down, and casts them beyond it, where the spoil settles; it leaves the seam
  for loaders and trucks, and stops at rock that needs blasting. When its reach is dug out it walks on 2 m a minute; at the strip's
  end it reads DONE. It draws 6 MW as the bucket fills, about 2.5 MW on average. STOP sets it back down as its parts.
- **Pit Switch House:** each outlet now carries its machine's PLC points too: RUN, RUNNING, DONE, CYCLES and POWER. The house's own
  points keep their numbers.
- **Trailing Cable:** a reel clicked on a pit machine (the drill, the hydraulic shovel, a lifted rope shovel or dragline) now couples it,
  adds 400 m, or (sneaking) takes the cable off.


### Open pit P7c — pit flotation and the tails (2026-11-10, decided while Tim was away)

- **A row of tank cells at 1,000 t/h:** the `conditioning_tank_large` conditions the pit line's fines slurry; each `tank_cell` floats its
  share of what floats (35, 15, 8, 5, 4, 3, 3, 2 % cell by cell, 75 % over eight) out of its east launder and passes the rest on out of
  its south face. A shorter row pays only what its cells float; a ninth cell adds nothing.
- **Column cleaners:** the `column_cell` cleans the row's concentrate for the existing concentrate thickener and pressure filter, which pay
  it as they pay the small bank.
- **Air:** the `flotation_blower_large` (400 kW, medium voltage) serves ten cells joined to it by air tubing; without it the cells read
  NO AIR.
- **The tails:** the `tailings_thickener_large` (45 m) sends 60 % underflow to a lined cell and its water back on a water pipe; TORQUE on
  the PLC.
- Nothing changes in existing worlds or the small plant. Decisions in `docs/questions/p7-pit-line.md`.

### Open pit P7b — the SAG mill line (2026-11-10, decided while Tim was away)

- **An SABC circuit at 1,000 t/h:** the `sag_mill` (9.75 m, two 4.1 MW pinion motors), the `pebble_crusher`, the `ball_mill_large`
  (7.3 m x 11 m, two 5.25 MW pinions), the `cyclone_feed_pump` (600 kW) and the `cyclone_cluster_large` (twelve 660 mm cyclones) turn
  the stockpile's crushed rock into fines slurry at 150 µm, carrying its assay, on steel pipe. All their motors at the medium-voltage tier.
- **The SAG's charge:** it grinds its full rate at its normal charge; overfed past 35 % filling it reads OVERLOAD and the belt weigher
  holds its feed 10 % back. With the pebble crusher off its pebbles go round uncrushed and it grinds 15 % slower.
- **PLC:** WEIGHT on each mill, PRESSURE on the cluster.
- **P7c (pit flotation) is planned, not built.** Nothing changes in existing worlds. Decisions in `docs/questions/p7-pit-line.md`.

### Open pit P7a — the primary gyratory, the coarse-ore stockpile and its reclaim (2026-11-10, decided while Tim was away)

- **The primary:** trucks tip into the `gyratory_pocket` (400 t) standing on the `gyratory_crusher` (450 kW at the medium-voltage tier),
  which crushes about 2,400 t/h into the `surge_pocket` (500 t) under it; the `primary_apron_feeder` (up to 3,000 t/h) puts it on a
  heavy belt. Every 200th hard bank block bridges the bowl until the `rock_breaker` on the pocket's rim breaks it, 30 s.
- **The heavy belt:** `heavy_belt_conveyor`, 1,400 mm at 4 m/s, 3,000 t/h, with `heavy_conveyor_drive`s of 355 kW: 75 kW a 30 m plus
  the lift; no steeper than 1 in 3.
- **The stockpile:** the `stockpile_head` drops crushed rock as `stockpile_rock` blocks by the settle rule; a 30 m pile holds about
  100,000 t. `reclaim_feeder`s under openings in a tunnel's roof draw the block over them; the slopes roll in at 37 degrees, so the
  pile drains as cones and the rest is dead stockpile (one feeder draws about a quarter).
- **The belt weigher:** reads t/h and pulses each 100 t; the feeders share its setpoint (1,000 t/h, a sneaking click or the PLC).
- **Nothing changes in existing worlds.** Decisions in `docs/questions/p7-pit-line.md`.

### Open pit P5 — the pit switch house, trailing cables, the ultra truck and the big shovels (2026-11-09)

Built while Tim was away on the draft spec; decisions in `docs/questions/p5-big-machines.md`.

- **Pit Switch House** (`pit_switch_house`): put it on the world's medium-voltage cable (its north face). Its four outlets each feed one
  machine through a **Trailing Cable** (`trailing_cable`, a 400 m reel): click the house with a reel, then the machine. Its panel opens
  and closes each outlet; an outlet only closes when its cable reaches a machine, and trips at 1.25 x its machine's peak held for a
  second. On the PLC: CLOSE, CLOSED, TRIPPED and AMPS for each outlet.
- **Electric machines:** the cable pays out as the machine drives and stops it at CABLE END; under 90 % volts at the machine it stops,
  LOW VOLTAGE. The **Blasthole Drill** has an Electric setting on its panel.
- **Ultra Haul Truck** (`ultra_haul_truck`): the 360 t class, 363 t in its body, 15 m long, climbing 10 % loaded at 11 km/h. It takes
  routes, tips into the pit hopper and works with dispatch as the 100 t truck does; it needs 40 m to turn and 2-block berms. The wheel
  loader cannot reach it.
- **Hydraulic Shovel** (`hydraulic_shovel`): a 27 t bucket; digs the face, swings to a truck either side and loads it in 13 passes; in
  AUTO it loads trucks on both sides in turn. Diesel, or electric on a trailing cable (its panel).
- **Rope Shovel**: build its six parts (`shovel_crawlers`, `shovel_house`, `shovel_gantry`, `shovel_boom`, `shovel_handle`,
  `shovel_dipper`), couple a trailing cable to the crawlers and press RUN on their panel: it lifts into one machine with a 100 t dipper,
  drawing 3.5 MW as it hoists, and loads an ultra in 4 passes. STOP sets it back down as its parts.
- **Art:** the trailing cable, ultra truck and hydraulic shovel icons are Ludo art. The haul trucks' empty bodies no longer show their
  tyres through the floor (the floor sits above the tyres, on rails), and the rope shovel's hazard stripes and louvres are coarse
  enough not to shimmer at range.

### Open pit P3 and P4 art (2026-11-09, decided while Tim was away)

- **Ludo art for the blasting and dry stack:** the drill, emulsion truck, cast booster, detonator, blast logger, blasting key and
  track dozer icons; the blasting machine and its stand; the blasthole column and its six collars, each still ringed in its
  state's colour (white drilled, yellow primed, orange charged, grey stemmed, green logged, red misfired); and dry tails. The drill
  and emulsion truck skins are painted from Ludo material swatches (a white paint added for the truck). No `*_placeholder`
  texture is left. Decisions in `docs/questions/art-p3.md`.

### Open pit P3 — drilling and blasting (2026-11-09)

Needs Wildcatter 0.6.0 (its ammonia plant makes the emulsion matrix).

- **Blasthole Drill** (`blasthole_drill`): a crawler rotary drill at its real size, 26 m to the mast top. Sneak-right-click it for its
  panel: rows and columns, burden, spacing, bench height, subdrill, staggered. GO walks the pattern and drills each hole, 1 m every
  10 s, in a serpentine; it stops and says why (DONE, OUT OF FUEL, BLOCKED, NOT LEVEL, not on broken ground). Drive it by hand and
  jump to drill one hole. Diesel, filled at a fuel bay.
- **Blastholes:** a drilled hole is a column of blocks, each remembering its rock. Its collar shows its state by colour. Put a
  **Cast Booster** and an **Electronic Detonator** in it (TNT makes both), click it with an empty hand to stem it with its cuttings
  (or gravel), wash it out with a water bucket, sneak-click to read it.
- **Emulsion Truck** (`emulsion_truck`) and **Matrix Fill Point** (`matrix_fill_point`): pipe Wildcatter's emulsion matrix to the
  fill point; a truck stopped beside it fills at 1 t/min. Jump in the truck charges every primed hole within its 12 m hose at
  20 kg/s, to the hole's depth less the stemming set on its panel.
- **Blast Logger**, **Blasting Machine** and **Blasting Key:** log each hole (its delay by row and place in the row), load the shot
  into the machine at least 50 m away, put the key in, ARM, FIRE: a 10 s siren, then the shot. On the PLC: ARM, FIRE, ARMED, FIRED,
  HOLES, MISFIRES.
- **The blast:** rock in each hole's box breaks to muck, 4 blocks for 3, the extra spilling off the free face; the bench floor is
  left flat. Short stemming blows out with flyrock; players and mobs within 50 m are hurt, more nearer. Holes with no booster or
  detonator, or too little charge, misfire and stand in the muck; the next shot waits 30 s and a loader will not dig into one. No
  shot fires with a machine or vehicle in the rock it breaks.

### Tim's answers built (2026-11-10, branch `answers`)

- **World rules:** what changes how an existing world plays is now a rule saved with the world, not a config file: the drill's NO
  VENTILATION interlock (`ventilation_interlock`), `stale_air`, `headframe_hopper`, `paste_unloaded_cure` and the new
  `dry_mill_all_hmc` are on in a new world; `paste_wall_breach` is off until set. A world saved before this version loads with every
  rule off. `/tailings rule` lists them; `/tailings rule <name> <true|false>` sets one (operators). The old
  `config/tailings-rules.properties` and `config/tailings-ventilation.properties` are read once, when a world's rules are made.
- **Paste fills ten times as fast:** a placed block of paste fill stands for 10 m³ of the line's paste, so a 360 m³ drive branch fills
  in about an hour; the cure is still a game day.
- **All heavy mineral concentrate through the dry mill** (`dry_mill_all_hmc`): a filter with a dryer touching it sends the spiral
  bank's share there too, paid at 1.0 a kg; the line still makes 4x. Off, or with no dryer, as before.
- **DMS floats on a belt:** a belt off the floats screen's north end takes the floats back to the plant as gravel with their rock, so
  the light ores (rock salt, potash, borax and the rest) can be recovered. With no belt, vanilla gravel as before.
- **X-ray sorter reverse mode:** past HIGH the sneaking click steps through the five thresholds in reverse, keeping what reads under
  the threshold (graphite, magnesite, borax, bauxite at LOW); the PLC's new REVERSE point (9) sets it in AUTO. A new sorter sorts as
  before.
- **PLC:** the tailings filter's CYCLE is a one-second bit at each cake drop (it was a pulse count); the paste mixer's LEVEL and
  DENSITY read its paste hopper.
- **Ore blocks only:** raw iron, copper and gold and every Wildcatter ore's mined drop are no longer plant feed.
- **Waits on Wildcatter:** the chlorinator taking Tailings' zircon concentrate (the request is in
  `docs/questions/answers-2026-11-10.md`); the medium from Wildcatter's ferrosilicon (0.7.0).
- **Tests:** `rules_new_world`, `rules_existing_world`, `rules_command`, `raw_ore_refused`, `vent_new_world`, `paste_pace`,
  `dry_mill_all_hmc`, `dms_floats_belt`, `xrt_reverse`, `gaps_press_cycle_bit`; the Fabric client film `dms_floats_belt`.

### gaps2: known gaps closed (2026-11-10, decided while Tim was away)

- **Press and load-out PLC points:** the tailings filter adds CYCLE (a pulse each cake drop) and FULL; the load-out frame adds LEVEL
  (the bin's 400 t over 4-20 mA), FULL and LOADING. Added after the motor's six points, so existing wiring reads the same.
- **Paste filter and mixer gauges:** LEVEL, FLOW and DENSITY on their feed tanks, as every slurry part has; DENSITY reads a tails
  slurry's marked solids (thickened tails 1.61 kg/L, paste 1.97) rather than plain tails'.
- **Three fixes off by default,** as they would change existing worlds; each is a key in the new `config/tailings-rules.properties`:
  - `headframe_hopper=true`: with no belt under it, a hopper or chest under a headframe bin takes its rock as items, a block's worth
    (2.7 t) an item, at the belt's rate.
  - `paste_wall_breach=true`: removing any solid block beside wet paste, with open ground in its place, lets the paste run out as a
    broken bulkhead does.
  - `paste_unloaded_cure=true`: a chunk's wet paste makes up the time its chunk was unloaded on its first cure step back (from its
    first step with the rule on; earlier waiting is not jumped forward).
- **Checked:** the fuel bay fills its day tank by pipe; muck tipped onto wet paste rests on it (the Known gap that said otherwise was
  wrong).
- **Tests:** `gaps_fuel_bay_pipe`, `gaps_press_points`, `gaps_loadout_points`, `gaps_paste_gauges`, `gaps_headframe_hopper`,
  `gaps_paste_wall_breach`, `gaps_paste_catch_up`, `gaps_muck_on_paste`. Decisions in `docs/questions/gaps2.md`.

### L7: dense-medium separation (2026-11-10, decided while Tim was away)

- **Deslime screen, DMS cyclone module, drain-and-rinse screens, medium recovery:** on the belt off the double-deck screen's gravel
  face, gravel is washed of its fines and split in a ferrosilicon medium (SG 2.6-3.3, default 2.75, a sneaking click steps it). Rock
  lighter than the medium floats and leaves as vanilla gravel, a block for 1.9 t, taking its trace minerals with it; heavier rock sinks
  and goes on to the cone. 2 % goes the wrong way each way. Country rock floats (diorite and deepslate sink at 2.75); the metal ores
  sink; rock salt, potash, borax, sulfur, graphite, bauxite and bentonite float at any setting.
- **Ferrosilicon:** a 25 kg bag, made up by hopper on the medium recovery; 0.3 kg is lost a tonne; with none the module reads NO
  MEDIUM. A placeholder recipe: an iron ingot, a sand and a metallurgical coke make 4.
- **PLC:** the module reads DENSITY (4-20 mA) and pulses FLOATS each tonne.
- **Nothing changes in existing plants:** without a DMS module the screen's gravel face, the cone and the mill run as before.
- **Tests:** `dms_split`, `dms_density`, `dms_medium`, `dms_gravel_face`, `dms_parts`; `DenseMediumTest`. Decisions in
  `docs/questions/l7-dms.md`.
### L8: the X-ray ore sorter (2026-11-10, decided while Tim was away)

- **X-ray ore sorter:** a 1.2 m XRT sorter (4 x 5 x 8, 20 kW) on the dry end. Crushed rock or gravel comes in over its north end; each
  piece's effective atomic number, worked from the real formulas of the minerals its rock holds, is read against a threshold of five
  steps (a sneaking click, or the PLC's THRESHOLD loop); over it the ejectors blow it onto the product belt off its south face, under it
  it drops onto the waste belt off its east face. 3 % of ore is misplaced to waste and 5 % of waste to product. 250 t/h on crushed rock.
- **Sorter air compressor:** its own 160 kW screw compressor, touching it. Without it the sorter reads NO AIR and stops.
- **PLC:** THRESHOLD (4-20 mA), PRODUCT and WASTE (a pulse a tonne).
- **Nothing changes in existing worlds:** two new parts; the dump station's ore sorter is unchanged.
- **Tests:** `xrt_split`, `xrt_threshold`, `xrt_no_air`, `xrt_plc`, `xrt_refuses`; `XrtSortingTest`. Decisions in
  `docs/questions/l8-xrt.md`.

### L5: paste backfill (2026-11-10, decided while Tim was away)

- **Paste filter, mixer and pump:** a 75 kW vacuum disc filter takes a tailings thickener's underflow to an 80 % cake; the 75 kW
  paste mixer touching it trims the cake with thickened tails and binds it with 5 % cement to paste at 78 % solids; the 110 kW piston
  pump pushes it at up to 35 m³/h down a steel fill line to a fill outlet. About 34 m³ an hour from all of the plant's tails.
- **Fill outlet, bulkheads, paste:** put the outlet in any closed underground void (up to 20,000 m³; close its openings with fill
  bulkheads); the paste lands in its lowest cells as wet paste fill, soft to wade in, and sets to a mineable weak rock in a game day.
  Break a bulkhead while the paste behind it is wet and the paste runs out.
- **Cement:** a 50 kg bag from quicklime and a clay ball (a placeholder until a cement kiln); a bag a tonne of tails.
- **Reads:** NO FEED, NOT THICKENED, NO CEMENT, NOT ENCLOSED, FULL, LINE PRESSURE (past 8 MPa: about 1 km of line on the flat, less
  where the line falls). PLC: the pump's FLOW and PRESSURE.
- **Nothing changes in existing worlds.**
- **Tests:** `paste_fill_void`, `paste_not_enclosed`, `paste_cure`, `paste_bulkhead_fail`, `paste_no_cement`, `paste_plant`,
  `paste_line_pressure`; `PasteFillTest`, `FillVoidTest`; the client film `paste`. Decisions in `docs/questions/l5-paste.md`.
### L6: the dry mill (2026-11-10, decided while Tim was away)

- **Scavenger spiral bank:** on the spiral bank's tails, it takes another 45 % of the ilmenite, rutile, zircon, monazite and
  cassiterite as heavy mineral concentrate (HMC). The filter keeps that share apart: into a fluid-bed dryer touching it, or out as
  100 kg bags of Heavy Mineral Concentrate that keep their assay.
- **Fluid-bed dryer:** dries HMC on natural gas from a pipe at its east side (about 7.9 m³ a tonne), 2 t/h, and passes it to the
  separators touching it in a line: the rare-earth roll magnet takes the ilmenite, the high-tension roll the rutile and cassiterite,
  the induced-roll magnet the monazite and the zircon. Each pays its minerals at the top rung: with the spirals' half, 4x by the real
  route. Reads NO GAS, WET (moist HMC into a separator), FULL.
- **Nothing changes in existing plants:** without scavenger spirals the filter, leach and cells pay as before; the leach still takes
  the heavy minerals for a plant without the dry mill.
- **Tests:** `dry_mill_split`, `dry_mill_ladder`, `dry_mill_wet`, `dry_mill_no_dryer`, `dry_mill_no_gas`, `dry_mill_parts`;
  `DryMillTest`. Decisions in `docs/questions/l6-dry-mill.md`.

### L1: ventilation of the drive (2026-11-09, decided while Tim was away)

- **Ventilation fan and duct:** a 75 kW `vent_fan` at the portal blows through `vent_duct` (laid like pipe) to the drive; a 30 kW
  `booster_fan` in the line carries it past about 3 km. Click a duct for its air; the fan's readout gives its flow, pressure and the
  air at the duct's end. The drive needs 10.8 m³/s (0.3 m/s across the bore).
- **The TBM's duct cassette:** click the reel gantry with a Duct Cassette and the machine pays out 100 m of duct, a metre a block,
  at the crown just behind its back-up. Its screen, its readout and a new junction signal (AIR AT BACK-UP) read the air there.
- **Gas detector:** hold it in a drive to read the air, STALE AIR, and RADON in tuff and sandstone.
- **Nothing changes in existing worlds:** a TBM with no ventilation reads POOR AIR and keeps driving. The interlock (NO VENTILATION)
  and stale air's Mining Fatigue and Weakness are off unless `config/tailings-ventilation.properties` turns them on (`interlock=true`,
  `stale_air=true`).
- **Tests:** `vent_fresh`, `vent_cassette`, `vent_stale_player`, `vent_booster`, `vent_default_off`, `vent_parts`; `AirflowTest`.
  Decisions in `docs/questions/l1-ventilation.md`.
### L4: the shaft and hoist (2026-11-09, decided while Tim was away)

- **Headframe, hoist house, skips:** a 30 m steel headframe over a 5 x 5 shaft and a hoist house behind it (a 450 kW double-drum hoist
  on the world's medium-voltage tier) wind two 3 t skips in balance at up to 10 m/s: the loaded one up to the dump in the headframe,
  the empty one down. Hang a skip by clicking the headframe with it. About 270 t/h from 150 m.
- **Measuring flask and headframe bin:** at the shaft bottom an apron feeder from a dump pocket (the L3 bottom-dump station) feeds the
  measuring flask, which measures each skip's 3 t; at the top the skips dump into a 100 t bin that feeds a belt to the plant.
- **Reads:** STOPPED (the brakes hold the skips without power), WINDING, WAITING FOR ORE, BIN FULL, NOT BUILT naming what is missing.
  PLC: the motor's points, FILL as the bin's level, SKIP DUMPED.
- **Tests:** `shaft_hoist`, `shaft_not_built`, `shaft_flask_feed`, `shaft_bin_full`, `shaft_power`; `HoistingTest`; the client film
  `shaft_hoist`. Decisions in `docs/questions/l4-shaft.md`. The raise borer and the cage are planned, not built.

### L2 and L3: battery locomotives and bottom-dump cars (2026-11-09, decided while Tim was away)

- **Battery locomotive:** an 8 t locomotive on its own 58 kWh traction battery runs on any mine rail, with or without a rectifier:
  0.20 m/s loaded up the decline, 3 m/s on the flat, about 8 kWh for 50 m of climb. LOW BATTERY under 20 %; FLAT at the floor, braked.
  A new one has no battery: right-click it with a Traction Battery; right-click with another to swap; sneak-click a held, standing
  locomotive to take its battery out.
- **Battery charger:** plugs into a three-phase 30 A receptacle (NEMA L22-30 or IEC 60309 red 32 A) and draws 20 kW from that
  circuit, charging the battery it holds or a battery locomotive standing beside it (a flat battery in about 2.6 hours). PLC points
  CHARGING, FULL and CHARGE (4-20 mA).
- **Bottom-dump cars:** the Granby car's size; they drop their load on the move over a new **track grizzly** (under the rails, over a
  dump pocket) beside a new **dump cam rail**. Granby cars pass a grizzly and bottom-dump cars pass a ramp without dumping, so one
  train can carry both.
- **Tests:** `battery_loco_run`, `battery_loco_climb`, `battery_loco_flat`, `battery_charge`, `bottom_dump_on_move`, `bottom_dump_full`,
  `bottom_dump_mixed`, `train_models_l2_l3`; `BatteryTest`. Decisions in `docs/questions/l2-l3-trains.md`.

### Playthrough fix: motors run on through gaps in their feed (2026-11-09, decided while Tim was away)

- **A plant part's started motor runs on, empty, for 60 s** after its work runs out (proposed). Since belts carry rock at 2 m/s, a
  motor that stopped the moment its hold emptied restarted direct on line on every few kilograms: in the playthrough the cone's
  4/0 cable burnt out at its motor box and the ball mill's 1 MW set tripped on stator overcurrent, both within 30 s of the first ore.
  It never starts without work, and a PLC's run or the hand switch still stops it at once.
- **The playthrough fails fast:** a burnt power cable, a tripped generator, a machine dark for a minute, or a TBM it cannot find stops
  the run with the reason. It follows the TBM when it sets itself down as blocks at the end of its course (it read "machine not
  found"). `TAILINGS_FEED=1` stages ore in the ore bin for a quick diagnostic. Decisions in `docs/questions/playthrough.md`.

### PLC 13i: the last signals (2026-11-09, decided while Tim was away)

- **A PLC chooses which rock each side of the ore sorter takes:** one 4-20 mA output a side carrying a rock code (copper ore is code
  10, 6.5 mA; the list is in DESIGN.md). The screen's setting comes back when the PLC stops saying. A gate the PLC shut now opens
  when the PLC goes quiet (it stayed shut).
- **A PLC programs the TBM's course** through its cable junction: TARGET Y and FLAT LENGTH, with read-backs. The course takes while
  the machine stands, and never moves floor already cut.
- **Slurry parts read LEVEL, FLOW and DENSITY** (4-20 mA): the feed tank's level, the flow out by pipe (0-60 L/s), and the slurry's
  density from its solids (1.0-2.0 kg/L). Transmitters read them too.
- **Changed for existing PLC programs:** the ore sorter has four more points (14, was 10); slurry parts have three more (9, was 6).
  The old points keep their numbers.
- **Tests:** `plc_sorter_route`, `plc_tbm_course`, `plc_slurry`; `PlcScaleTest`.

### Open pit P4: dry stack, the truck load-out, waste dumps, berms, the track dozer (2026-11-09, decided while Tim was away)

- **New parts:** the tailings thickener (15 m, tails to 60 % solids, its overflow water back on a water pipe), the tailings filter (a
  plate press dropping 15 t of dry tails every 15 minutes), and the truck load-out: a 400 t bin on a frame over a drive-through lane,
  filling a truck under its gate at 2 t/s.
- **Dry tails**, a new block: the press's cake, 1.8 t a block, falling like sand. The pit hopper refuses it: it goes to the dump.
- **LOADOUT stakes work:** a self-driving truck drives through the load-out, is filled under the gate, loops round and hauls to its
  dump stop.
- **Dumps, crests and berms:** at a DUMP POINT on a crest a truck backs to the berm and tips over it, and the load runs down the face;
  with no berm it will not back up (NO BERM, and BLOCKED in dispatch). With no crest it tips on the flat. A berm stops a truck backed
  by hand; a truck backed over a crest with no berm goes over and is wrecked, leaving its item and load at the bottom.
- **The track dozer:** a 70 t crawler. Blade CARRY, CUT (to level, 5, 8 or 10 %) or RAISED; it pushes loose ground and muck, cuts
  ramps, pushes loads over a crest and pushes up berms along it. Hard rock stops it.
- **Tests:** 12 NeoForge scenes (`pit_thickener` to `pit_dozer_push`), `DryStackTest`; the client scene `pit_dry_stack`.

### The TBM's gaps (2026-11-09, decided while Tim was away)

- **The gantries run on their own wheels** on the twin track. Take a rail from under one and the machine stops, naming it; put it
  back and it goes on. On the decline they now stand on their rails, half a block lower than before.
- **Every car of a train loads:** the loading point reaches a whole 10-car train (29 rows behind the conveyor, was 13), and a train
  indexes forward through it a car at a time.
- **The trailing cable is drawn** from the reel gantry back to the cable junction, and the reel turns as it pays out.
- **The reel gantry's transformer is on the circuit:** the pump's and conveyor's current flows through a 4.16 kV to 480 V, 150 kVA
  transformer, shown on the junction's readout. The conveyor stops if its 480 V is out.
- **The quench tank is on the pump gantry.** Breaking the pump gantry loses its water. Changed for existing worlds: water in an old
  cab moves to its pump gantry.
- **The cab's screen stays open** when the machine lifts and sets down.
- **Real recipes** for the cutterhead, segment erector, main beam, track layer, cable junction and the three gantries, from steel and
  Wildcatter's parts. Changed for existing players: the old iron-and-copper recipes are gone.
- **Track too long for the solver** (over 4,000 nodes on one rectifier) is dead and says so: "TRACK TOO LONG".
- **Fixed by Wildcatter 0.4.1, now proved:** busway straight onto the traction rectifier's motor box.
- **Tests:** `tbm_gantry_derail`, `tbm_train_fills`, `tbm_quench_gantry`, `tbm_screen_follows`, `tbm_transformer`, `tbm_recipes`,
  `track_too_long`, `rectifier_busway`; `GantryWheelsTest`.

### Plant gaps closed (2026-11-09)

- **Belts take time:** rock rides a belt at 2 m/s, so a 20 m run takes 10 s, and the lumps are drawn where the rock is. A belt whose
  head end is blocked stops.
- **Parts take rock only through their feed face:** grizzly, cone and heap pad from a belt over their top; screen and ball mill over
  their north (feed) end; the jaw only from its grizzly. A part beside another's output face no longer takes it as a chute: lay a belt.
  Existing layouts that relied on that need a belt there.
- **Gravel aggregate:** a belt on the west face of the screen's south end takes the 6-40 mm gravel out; a hopper or chest at a belt's
  head end takes rock as items.
- **Oversize on the dump pocket's grizzly:** about one item in a hundred stays on the bars; 32 choke it and no car tips. Break pieces
  through with a pickaxe, or sneak-click with an empty hand to lift them off.
- **Trains wait at the signal before the dump** while its pocket is full, when a block signal guards the dump.
- **Bulk concentrate:** lead and zinc concentrate piped into one filter pay the lead only, the zinc going to slag.
- **The ore sorter has a screen:** click it with an empty hand; put a rock on a side's slot to set it, W makes it the waste side.
- **Assays count metal in kg:** an ingot is 100 kg of copper, iron or gold in a slurry's assay. Payouts are unchanged.

### Round wheels (2026-11-09, Tim's request)

- **The haul truck's and wheel loader's wheels are round**, as round as the pixel grid allows (a 1/16 m slice per row), with a steel
  hub and six wheel nuts, and they **roll** as the machine moves. The mine locomotive's and car's 0.25 m wheels are eight-sided, as
  round as 4 px allows.

### Open pit P2b — the haul dispatch office (2026-11-09)

- **Haul Dispatch Office** (`haul_dispatch`, 3 x 3 x 3): a site office on Wildcatter's control cable that runs the pit's trucks by
  radio within 2 km. Click it with a survey book to add the book's route (up to 8; sneak-click to take one off). Power it from the
  low tier (5 kW).
- **PLC points:** for each route RUN, RUNNING, TRUCKS (4-20 mA, 0-16), LOADS (a pulse a tipped load) and TONNES (4-20 mA, 0-2,000 t in
  the last hour); for the fleet FUEL LOW and BLOCKED. Its selector: HAND runs every route, OFF parks them, AUTO follows the PLC.
- **PARK stakes work:** RUN off sends a route's trucks to its PARK stake (or their next stake) to stand with their engines off; RUN
  on sends them back to work. A PLC that stops parks the route; an office with no power stops its trucks at their next stake.
- **Fix:** a self-driving truck backing in at an angle on a straight road no longer circles behind the end stake.

### Open pit P2a — survey stakes, self-driving trucks, the self-running loader, diesel and the fuel bay (2026-11-09)

- **Survey stakes and the survey book:** mark a haul road's stops on stakes (LOAD, DUMP POCKET, DUMP POINT, FUEL, PARK), click
  them in order with the book; each leg is surveyed for grade (10 % most), a truck's clearance, and width (narrow legs are one-way).
- **Self-driving haul trucks:** click a truck with the book. It runs out and back between the route's ends, turns round and backs
  in to the loader and to the truck dump's bumper, fills, tips, keeps right on two-way legs, waits for one-way legs, keeps 50 m from
  the truck ahead, stops for anything in its way (BLOCKED after 30 s), and stops at a FUEL stake when low. Sit in it to take over.
- **The loader runs itself:** click it with a book whose route has a LOAD stake within 16 m; it digs and loads trucks that pull in.
- **Diesel:** trucks and loaders burn it by the engine's work (about 200 L/h flat out, nothing parked) and stop when dry.
- **Fuel Bay:** a dispenser island with a 2,000 L day tank, filled by Wildcatter pipe; it fills a stopped machine within 8 m at
  600 L/min.

### Open pit P1 — the haul truck, the truck dump and the wheel loader (2026-11-09)

- **Haul truck** (100 t class, 91 t payload, true size): right-click to drive it with the movement keys, by its 765 kW against its
  load and the grade; jump, standing, tips the body. Its load is counted in real tonnes.
- **Truck dump:** a `truck_dump_wall` with a 1.5 m bumper and a 400 t `pit_hopper`. A truck backed to the bumper tips its rock into the
  hopper, and the apron feeder, sorter and ore bins take it on as from the rail dump pocket. Soil is refused: tip it on a dump.
- **Wheel loader:** jump digs up to 20.4 t of soft ground (dirt, sand, gravel, clay, mud, the soft Wildcatter ores) or muck from the
  face in front; jump again dumps it into a truck beside it. Five passes fill a truck. Standing rock it cannot dig.
- **Muck and loose ground:** broken rock as `muck` blocks that remember what they were; tipped loads land and settle downhill, piles
  standing at 37 degrees, and fill holes first.
- **Config:** `truck_damage` in `config/tailings-train.properties` (default 8).
- **Clear cab windows:** the truck's and loader's windows are see-through, so the driver can see out (they were opaque; found
  filming the `pit` scene).
- **A truck comes away from the hopper empty:** the hopper keeps what is under a whole block of each ore and makes it up with the
  next load.
- **Tests:** `pit_settle`, `pit_truck_tip`, `pit_truck_body`, `pit_hopper`, `pit_truck_drive`, `pit_loader_soft`, `pit_loader_hard`,
  `pit_loader_muck`; `SettleTest`, `PitDriveTest`.

### PLC signals, Wildcatter 13i (2026-11-07)

- **Requires Wildcatter 0.5.1,** with its PLCs. 0.5.0 cut a long control trunk short: the playthrough's 2,076 cables reached 26 of 37 devices.
- **On a PLC:** every plant part with a motor (run, running, stalled, amps, fill), the ore sorter's gates, mine switches, block signals, and the TBM through its cable junction. Each has HAND / OFF / AUTO; in HAND it works as before.
- **A mine switch can be thrown by hand,** and a train stops if a switch is thrown under its path.
- **Fixed before release, found by the playthrough:** a GO through the cable junction now does what the cab's Go does first (heading and origin); it read NOT BUILT.
- **Tests:** `plc_plant`, `plc_switch`, `plc_signal`, `plc_tbm_go`; the playthrough starts the mine from a PLC.

### Phase 4e — Selective lead-zinc flotation (2026-11-07)

- **A conditioning tank can run selective** (sneak-click with an empty hand): zinc sulfate holds the sphalerite down, a first bank floats the lead, and a new **activation tank** with copper sulfate frees the zinc for a second bank.
- **Changed for existing plants:** a plain bank fed both galena and sphalerite now loses the zinc it floats, as a bulk cake's zinc goes to slag in a lead furnace. This reaches the traces in country rock. Feed with only one of the two pays as before.
- **Tests:** `selective_float`, `selective_no_reagent`, `no_activation_tank`, `bulk_loses_zinc`, `bulk_one_mineral`, and a `selective` film.

### Wildcatter 0.4.1

- **Requires Wildcatter 0.4.1,** whose solver fix lets a generator close its breaker onto a dead bus with long busway. The playthrough stalled on 0.4.0.
- **The playthrough runs on peaceful,** after a creeper killed its player in the pit. Listed as staged.

## Phase 4d — The lined cell (2026-11-06)

- **Tails go to Wildcatter's lined cell** (v0.4.0), on a real pipe over its rim; the decant's water comes back to the mill. The old pond is a legacy part: no recipe, out of the creative tab.
- **The playthrough runs Phase 4:** real rock through the scavengers, leach and cells, tails in a lined cell, water from a process pump instead of staged. About 950 kW more, on two more generators.
- **Fixed, found by the playthrough:** a blower now serves the scavenger bank it touches. The bill of materials counts crafting batches properly.
- **Tests:** `tails_to_cell`, `cell_breached`.

## Phase 4a, 4b, 4c — Full recovery (2026-11-06)

- **Scavenger bank, regrind mill, cleaner bank:** 85 % of what floats.
- **Leach tank, CCD thickener, electrowinning bank:** 95 % and the even 4x. What does not float reaches 4x here too (Tim). Acid off Wildcatter's pipe.
- **Heap leach pad:** waste rock by belt, paying its traces at 50 %.
- **By-product plant:** cadmium, indium, rhenium, cobalt, rare earth oxide, and Wildcatter's silver.

## G — The gravity circuit (2026-11-06)

- **The spiral bank:** 4 x 5 x 6, 37 kW. On the cyclones' overflow it pays the 2x route at plant scale for everything that does not float, iron included. Copper, gold and the sulfides pass through to flotation.

## R10 — The playthrough (2026-11-06)

- **A filmed run from nothing in a real generated survival world,** real recipes, real placement, real generators, real rock to copper ingots.
- **Recipes:** `mine_switch` and `mine_light`.
- **Found:** what the machine really needs for power (tunnel-boring-machine.md), and a layout that can be wired (crushing-circuit.md).

## R9 — Real vehicle length (2026-11-06)

- **The locomotive is 3 m and the cars are 2 m,** on 0.5 m couplings. New models and skins; a vehicle's hit box now runs its whole length along the track.
- **Placing:** a car couples when placed or sneak-clicked within 3 m of its place behind the train.
- **The conveyor's chute reaches 13 rows,** so a stopped train still loads its first four cars.
- **Tests:** `tbm_train_loads`; the `haul` film found the first car standing past the old chute.

## R8 — Sound (2026-11-06)

- **Machines now sound:** crushers, screen, ball mill, belts, apron feeder, cyclones, agitators, filter, sorter, blower and thickener while they run with work to do; the machine's cutters and its pump and conveyor gantries while it drives; the locomotive, its pitch following its speed.
- **Events:** a car tipping, muck pouring into a car, each ring set, a coupling, the machine lifting and setting down.
- **Tests:** `plant_sound`; the client scenes check each machine's sound starts.

## R7 — Testing and art (2026-11-06)

- **A train behind a moving machine now turns back.** It followed the conveyor forever with full cars, since it never came to rest behind a gantry on the move. Found by filming the whole mine; `tbm_train_turns` covers it at the real advance rate.
- **The cab's screen:** slots are drawn, with ghosts of what each empty stock slot takes; the status names what is wrong ("NOT BUILT: needs the Pump Gantry coupled behind it"), which never reached the player before; nothing runs off the panel; the stop toggles show their state and say what they do; it is titled Tunnel Boring Machine.
- **Filmed client scenes:** `crushing`, `flotation`, `dump`, `haul` (the whole mine) and `hand` (real input, then a save and reload in mid-drive). `tbm_decline` is filmed in section.

## R6c — The gantries (2026-11-06)

- **Pump, reel and conveyor gantries:** placed on the twin track like a car, coupled in order with a sneak-click, and towed behind the machine, pitched to the floor under each.
- **The machine needs them:** a missing gantry, or missing track under one, reads NOT BUILT and names it. "Cable end" stops it.
- **Muck** now loads into cars just behind the conveyor's tail; a train waits there, and stops and turns back at the gantry.
- **Power:** the pump and conveyor add their draw at the cable junction.
- **The old dewatering pump is retired:** no recipe, out of the creative tab.
- **Tests:** `tbm_gantries`, `tbm_muck`, `tbm_cable_end`, `tbm_gantry_missing`, `tbm_uncoupled`, `tbm_gantries_decline`, `tbm_train_stops`.

## R6b — The contraption (2026-11-06)

- **The machine now moves as one.** Started, the TBM's parts lift into a single entity that cuts the face, glides into the cut at the advance rate, and lines and lays track behind itself.
- **On the decline** it follows the 1-in-2 grade, pitched. Stopped there it parks and its cab still works; stopped on the flat it sets back down as blocks, or says where it is blocked.
- **Dismantling:** sneak-click the stopped machine with an empty hand; every part and slot drops.
- **Riding:** the Ride button on the cab's screen seats you on the running machine.
- **Tests:** `tbm_lift`, `tbm_glide`, `tbm_setdown`, `tbm_park`, `tbm_dismantle`, `tbm_seat`; `drill`, `drill_pause`, `drill_blocked` and `tbm_built` rewritten; filmed client scenes `tbm` and `tbm_decline`.

## Live client tests and fixes (2026-11-06)

- **Fixed: mining rules.** A stale tag entry since R3 stopped the pickaxe and stone-tool tags loading, breaking mining rules for the whole game. All parts are now tagged.
- **Fixed: flat drives** no longer sink past the target Y.
- **Fixed: unfed track** no longer spams "could not solve the circuit".
- **Tests:** a Fabric client game test that films its scenes; `traction_unfed`.

## R6a — The TBM's real parts (2026-11-06)

- **The real machine:** cutterhead, segment erector, main beam with its cab, and track layer, each one item at true size with a real model, replace the one-block cab. Build them in order; the cab names anything missing.
- **Cable junction:** wire 4.16 kV to it within 16 blocks of the cab.
- **Lining and floor** go down behind the machine, as on a real TBM. Gravel ballast goes under all track, decline too.
- **1 in 2 only:** the steep option is gone.
- **Until the contraption wave** the machine drives flat only.
- **Old worlds:** an old cab drops its stock and a main beam item.
- **Tests:** `tbm_built`, `drill_retired`; the drill scenes rewritten.

## R5b — Traction power on Wildcatter's DC solver (2026-11-05)

- **The traction rectifier** is a real 250 V DC source on Wildcatter's circuit, with a DC breaker (trips at 1.25x for 1 s; click to reset). Its 480 V draw follows the trains.
- **The track is the conductor rail:** volts fall along a long line, so a long drive wants a second rectifier.
- **Locomotives draw real DC power,** and their speed follows the track's volts.
- **Lights and signals** go dark when their rail is not fed.
- **Tests:** game tests `traction_dc`, `traction_trip`, `traction_far`.

## R5a — Trains as entities (2026-11-05)

- **Every vehicle is an entity,** and trains glide along the track instead of jumping a block at a time.
- **Trains run the decline** both ways now, and follow thrown switches; cars follow the locomotive's path.
- **Speed follows power:** slow up the 1-in-2 decline when loaded, 1 m/s down it on the rack brake, 3 m/s on the flat.
- **Controls:** right-click the locomotive to run or hold it; sneak-right-click to reverse. Sneak-hit a vehicle with an empty hand to pick it up with its load.
- **End of track:** the train stops, waits 5 s and reverses.
- **Derailing:** track broken under a running train derails it and spills the loads at the gap.
- **Old worlds:** trains stored on the rails come back as entities.
- **Tests:** `TrackTest`, `TractionTest`; game tests `train_runs`, `train_turnout`, `train_decline`, `train_derail`, `train_harm`, `train_migrate`.

## R4c — The dump station (2026-11-05)

- **The pocket** takes each car's whole load, up to one train. When it is full, the car over it does not tip and the train waits.
- **The apron feeder** draws a stack a second onto the ore sorter.
- **The ore sorter:** click one of its sides with a rock or ore to send that type out of it; sneak-click a side with an empty hand to make it the waste side. Each type goes into the ore bin touching its side.
- **Old worlds:** a pocket's old sorted slots come back into the pocket.
- **Tests:** game tests `dump_station`, `dump_waits`.

## R4b — Flotation to items (2026-11-05)

- **Real flotation on steel pipe:** conditioning tank, flotation bank with its air blower, concentrate thickener and pressure filter, each on its own motor.
- **Reagents** are dosed per tonne of feed; copper sulfate only when the feed holds sphalerite.
- **The 75 % ladder** in the bank; the filter pays nuggets and packs nine into the ingot or mineral item, so a copper ore block still gives 3 ingots.
- **New part: `tailings_pond_placeholder`** takes the final tails until the lined cell (Phase 4d).
- **Removed:** R3's flotation shortcut.
- **Tests:** `FlotationTest`; game tests `flotation`, `flotation_stops`.

## R4a — The crushing circuit (2026-11-05)

- **Real flow:** the grizzly, jaw, screen, cone, ball mill and cyclones each run on their own motor at their share of 50 t/h. No more line forming or shortcut.
- **Belts** carry rock in kg from part to part. A `conveyor_drive` at the head end drives and holds each run; long runs need a drive for each 30 m. Belts show the rock they carry.
- **Slurry on Wildcatter's steel pipe** from the ball mill on: mill discharge to the cyclones, fines slurry out, each with the rock's mineral assay (Wildcatter 0.3.0).
- **Water** for the mill and cyclones from a Wildcatter pump.
- **The ore bin** holds 8,192 rock items and feeds the belt under it.
- **The screen's sand** comes out as items for a hopper when no belt is on its sand face (the hand line's 2x route).
- **Tests:** `CrushingRatesTest`; game tests `crushing_circuit`, `bin_feeds`, `belt_drive`, `not_built`.

## R3 — Big parts and real models (2026-11-05)

- **Wildcatter's big parts:** every crushing, flotation and dump-station part, and the traction rectifier, now uses them. You get one item at its real size, the ghost outline, pieces that break together, real collision, and the motor box on the west face of the main cell.
- **New parts:**
  - the `flotation_bank` (eight cells in one part) replaces eight `flotation_cell`s;
  - `belt_conveyor` runs with a `conveyor_drive`;
  - the dump station's ramp, pocket, apron feeder, ore sorter and ore bin replace the one-block `dump_station`.
- **Models:** real ones, generated by `tools/models/` with Wildcatter's tool and drawn in Wildcatter's materials.
- **Trains** tip into a pocket under the track, beside a dump ramp, on the move.
- **Lines** form from parts joined by touching or driven belt runs. The old process shortcut runs until R4.
- **Old items:** `flotation_cell` and `dump_station` craft back into iron.
- **Tests:**
  - `ModelLintTest` and `MachineTest`;
  - game tests `part_break`, `not_built`, `terminal`.

## R1 — Safety fixes (2026-11-05)

The first wave of realigning Tailings with Wildcatter. Committed straight to `main`.

- **Chunk tickets.** A running drill and a locomotive refresh one Tailings ticket type, `tailings:runner`, on both loaders. It is saved with the world and times out 10 s after its last refresh. The Fabric global force-load and the NeoForge `TicketController` are gone, so a player's own force-loaded chunks are never touched, and a stopped or removed machine never leaves chunks loaded.
- **The drill** no longer spawns its invisible `TunnelDrillEntity`. One left in an older world discards itself. Before the cab and pump move, every cell they move into must be air, replaceable, or their own; otherwise the drill stops as "blocked" and moves nothing. When it starts again, the cab catches up with the face first.
- **Trains.** The harm config is read once per server run, not every tick. The headlight's light block moves only when the locomotive does. The consist is found by car index, so cars past a turnout or on a grade stay with the train. The rectifier now feeds `mine_switch` too: before, a fed train could not run through a turnout at all.
- **Reagents.** The conditioning tank takes out a reagent item for each kilogram dosed, so it no longer jams after 256 of each. A tank saved by an older build is trimmed when it loads.
- **Drops.** Plant machines and the drill cab drop their contents however they are removed, explosions included.
- Tests: `ReagentStockTest`; game tests `haul_turnout`, `train_tickets`, `drill_blocked`, `plant_drops`.

## Pull request 9 — Render mine trains and the remaining haulage textures

- Locomotive and mine-car entity models (128×128), registered on Fabric and NeoForge.
- `MineTrainRenderer` draws the locomotive and the cars stored on the rails behind it, 20 pixels apart, following the slope. A car's load cube is hidden when the car is empty. The headlight cube is full bright.
- Lit and unlit mine lights, the thrown switch overlay, and gravel replaced by `ballast` under flat rail.

## Pull request 8 — Phase 3 block and item textures

- Block and item sprites for the drill, pump, rectifier, dump, lining, signals, lights, and the rail overlays.
- `mine_rail`, `rack_rail`, and `mine_switch` are cutout overlays on the floor. Signal lamps follow the lit state. Slopes, stairs, landings, and mine floors stay on mined stone.

## Pull request 7 — Phase 3 revisions

The moving drill and the haulage revisions, on top of pull requests 5 and 6.

- The drill is a moving contraption. Chunk tickets keep the drill and trains loaded when no player is nearby.
- A full muck buffer pauses the drill until a train takes the rock. The drill does not latch off.
- Slopes and rail beds are eight-slice ramps, with matching collision. Stairs and landings stay on the walkway.
- Default lining is cobble or mined stone. Crafted segments are used only when a full ring is stocked.
- Pumped water fills a 256 m³ quench tank. Excess is voided. Buckets can still top the tank up.
- A car holds 9 stacks (576 items). A 10-car train fills the dump pocket. The apron clears one stack a second.
- A fed rail section is live along its whole length and still safe to walk. Lights, signal lamps, and the locomotive headlight draw from it. A thrown switch sends a train clockwise.
- The locomotive is an entity and hurts living things it touches. Damage defaults to 8, from `config/tailings-train.properties`, not a gamerule.

## Pull request 6 — Phase 3b haulage

Mine rail, traction rectifier, locomotive and car items, dump station, and block signals. Merged into the drill line and brought to `main` with pull request 7.

## Pull request 5 — Phase 3a tunnel drill

The first tunnel drill: an 8×8 cut, a 6×6 lined drive, the dewatering pump, and the floor pieces. The cab in that pull request was still a static multiblock. Pull request 7 replaced it with the moving contraption.

## Pull request 4 — Phase 2 concentrator

True-size crushing circuit and flotation bank on Wildcatter motors. Fines are the mill product. A full bank pays the 75% route for sulfides and for vanilla copper and gold. Lime is calcined calcite, because Wildcatter has no lime item.

## Pull request 3 — PLC note

Docs only. An unscheduled idea for in-game PLCs (ladder logic, function blocks, I/O), and a check that Wildcatter's controls are fixed hardware rather than a program the player writes.

## Pull request 2 — Phase 1 starter mill

Stamp mill, hand screen, shaking table, and magnetic separator. Size items and mineral lists for country rock, the 27 Wildcatter ores, and vanilla iron, copper, and gold. Gravity and magnetic concentration pay 50%. Nine nuggets craft into one mineral item.

## Pull request 1 — Phase 0 skeleton

Multi-loader Gradle project, CC0, CI (`./gradlew build` and `:neoforge:runGameTestServer`), and the plain-Java recovery ladder at 25/50/75/95%.

## Initial commit

Design docs: vision, roadmap, and the first machine specs, before any code.
