# Progress

Phases 0 through 3 are on `main` (pull requests 1 through 9). The realignment with Wildcatter (R) is current; R1 is done. Phase 4 has not started. The full status, including stand-ins, is [docs/ROADMAP.md](docs/ROADMAP.md). How the systems work is [docs/DESIGN.md](docs/DESIGN.md).

## Phase 0

Multi-loader skeleton, CC0, CI, and the plain-Java recovery ladder (25/50/75/95%). Merged in pull request 1.

## Phase 1

Hand-cranked stamp mill, hand screen, shaking table, and magnetic separator. Country rock, the 27 Wildcatter ores, and vanilla iron, copper, and gold have size items and mineral lists. Gravity and magnetic concentration pay 50%. Nine Tailings nuggets craft into one mineral item. Iron and gold use the vanilla nuggets. Merged in pull request 2.

## Phase 2

True-size crushing circuit and flotation bank on Wildcatter motors. Fines are the mill product. A full bank pays the 75% route for sulfides and for vanilla copper and gold. Lime is calcined calcite. Merged in pull request 4.

## Phase 3

Moving tunnel drill with chunk tickets, stone-default lining, quench tank, smooth 1-in-2 ramps, and a pause when the muck buffer is full. Haulage is a rectifier, a locomotive entity, cars stored on the rail, a dump, lights, and block signals. Textures and the locomotive and car models are in. Merged across pull requests 5 through 9. There is still no Field Manual.

## R1: safety fixes (2026-11-05)

The first wave of realigning Tailings with Wildcatter, after a review found it far from Wildcatter's patterns. R1 fixes only what damages worlds or leaks load:
- **Chunk tickets.** One `tailings:runner` ticket, refreshed by running machines and timing out on its own, replaces the global force-load.
- **The drill** no longer leaks an entity, and it no longer overwrites blocks when it moves: it stops as "blocked".
- **Trains** no longer do disk reads or block updates every tick, and keep their cars through a turnout. A turnout is now fed at all: the rectifier skipped switches.
- **Reagents** no longer jam the tank.
- **Plant machines and the drill** drop their contents however they are removed.

`./gradlew build` and `:neoforge:runGameTestServer` (15 scenes) pass.

## R2: specs in Wildcatter's format (approved by Tim 2026-11-05)

The three draft specs are rewritten in Wildcatter's format, and the drill-and-haulage spec is split in three, giving five specs:
- `crushing-circuit.md`;
- `flotation-bank.md`;
- `tunnel-boring-machine.md`;
- `haulage.md`;
- `dump-station.md`.

`docs/multiblocks/README.md` indexes them. The design choices made with Tim are in ROADMAP (R2).

The slurry the plant puts on Wildcatter's steel pipe needs a Wildcatter change, proposed in Wildcatter's ROADMAP Ideas. No code
changed.

## Live client scenes, and what they found (2026-11-06)

A Fabric client game test now builds scenes in a real game and films the game window (Tim's request; part of R7 brought forward):
`parts`, `train` and `tbm`. The first run found four bugs the server scenes had missed, all fixed:
- `flotation_cell` and `dump_station` were still in the pickaxe and stone-tool tags, so since R3 those tags failed to load, taking vanilla's
  mining rules with them; R3's newer parts were not in them at all. `registers` now checks the tags load and every part is in them.
- on the flat the floor kept dropping past the target Y, a staircase of flat pieces (an old bug: the tests drove only a block or two).
- unfed track could not be solved (its joints are too stiff for the solver's tiny leak): each rail now has its insulators' 1 Mohm leak to
  earth. `traction_unfed` covers it.

## R8: sound (2026-11-06)

Tim asked for machine sounds. Sixteen new sounds from Ludo, two reused from Wildcatter ([docs/sound.md](docs/sound.md)). 49 game test scenes pass and the client scenes check each sound starts, with one steady loop per machine. Nobody has listened to them together yet, so the levels are a first guess.

## R7: testing and art (2026-11-06)

Every spec now has a filmed client scene. Three things turned up that the server scenes could not show, all fixed: a full train never left a moving gantry; the cab's screen had no slot outlines and ran off its panel; and the name of a missing part never reached the player. 48 game test scenes pass, and all the client scenes. The models stay as they are. R is done; what is left before phase 4 is a survival playthrough.

## R6c: the gantries (2026-11-06)

Pump, reel and conveyor gantries ride the twin track behind the machine, coupled in order. The conveyor loads the cars behind its tail, trains stop short of it, and a missing gantry or the cable's end stops the machine. 47 game test scenes pass; both drives are filmed with the gantries. R6 is complete; R7 is next.

## R6b: the contraption (2026-11-06)

The TBM's parts lift into one entity when it starts, glide into each cut, and line behind themselves, pitched on the decline. It sets down on the flat or parks on the decline; it can be dismantled and ridden. 40 game test scenes pass; the flat and decline drives are filmed.

## R6a: the TBM's real parts, as blocks (2026-11-06)

The one-block cab is gone; the machine is what tunnel-boring-machine.md draws:
- cutterhead, segment erector, main beam with the cab at its tail, track layer and cable junction, true-size parts with real models;
- the cab checks they stand in order and names what is missing, and draws 1 MW at 4.16 kV through the junction;
- each advance cuts the face, moves every part one block on, then places the ring behind the erector and the floor and twin track
  behind the track layer, with gravel ballast under all track;
- 1 in 2 only, and broken lining does not leak (Tim's answers);
- until R6b it drives flat only.

**Tests:** `TunnelDrillTest` rewritten (the two-phase advance, the decline coming down behind the machine, a waiting ring across a
save); game tests `drill`, `drill_pause`, `drill_blocked` rewritten for the whole machine; new `tbm_built`, `drill_retired`.

## R5b: the traction rectifier on Wildcatter's DC solver (2026-11-05); R5 done

The track carries real DC power, as haulage.md says (Tim's choice: a Tailings source on Wildcatter's solver):
- the traction rectifier is a 250 V source behind its 2 % internal resistance, with a DC breaker that trips at 1.25 times its
  rating for a second; its 480 V draw follows the trains;
- every rail and switch is a length of conductor rail on Wildcatter's circuit, so the volts fall along a long line;
- each locomotive is a DC load drawing what it uses through the rail under it, and its power, so its speed, follows the volts;
- lights and signals follow whether their rail is fed.

**Tests:** `TractionTest` (the rectifier's sizing); game tests `traction_dc` (a 14 t train climbs the decline at about 1.2 m/s while
the rectifier's amps rise), `traction_trip`, `traction_far`; `haul` reads the track's volts.

## R5a: trains as entities (2026-11-05)

Every locomotive and car is an entity, and trains glide along the rails as haulage.md says:
- the track is read from the rails themselves, so trains run the flat, through thrown switches, and down and up the 1-in-2 decline;
- speed follows power over force (2.06 m/s for a locomotive alone up the decline, 0.34 m/s loaded), 1 m/s down the rack, 3 m/s on
  the flat, 0.3 m/s² either way;
- place vehicles on the rail; cars couple within reach; run, hold and reverse the locomotive by hand; at the end of the track a
  train waits and reverses;
- signals, the dump station and the drill's loading point hold it; track broken under it derails it;
- old worlds' trains, stored on the rails, become entities;
- every rating is in `haul.Haul`.

**Tests:** `TrackTest`, `TractionTest`; game tests `haul`, `train_runs`, `train_turnout`, `train_decline`, `train_derail`,
`train_harm`, `train_migrate`, and `dump_station`, `dump_waits`, `train_tickets` rewritten for entities.

## R4c: the dump station (2026-11-05); R4 done

The dump station runs as dump-station.md says:
- the pocket takes each tipping car's whole load, up to one 10-car train; a car it cannot take holds the train (Tim's decision);
- the apron feeder draws a stack a second to the sorter;
- the sorter's sides are set by clicking them with a rock item, and one side as waste; each type goes into the ore bin on its side;
- each ore bin feeds the belt under it, into the crushing circuit.

From train to ingots the plant now runs as the three specs say.

**Tests:** `HaulTest` (the sorter's sides); game tests `dump_station` (both cars tip on the move; copper to its bin, cobble to
waste; a full copper bin reads FULL) and `dump_waits`; `haul` updated for the pocket.

## R4b: flotation to items (2026-11-05)

The flotation circuit runs as flotation-bank.md says, on steel pipe:
- the conditioning tank doses the reagents per tonne of feed (copper sulfate only for sphalerite);
- the bank, with its blower, floats 75 % of each floating mineral to concentrate and sends the rest to tails;
- the thickener and pressure filter turn the concentrate into items: nuggets, nine packed into the ladder's item, so a block of
  copper ore still pays 3 ingots;
- the tails go to a new `tailings_pond_placeholder` (Tim's decision) until Phase 4d.

R3's flotation shortcut, `Concentrator` and `PlantPlan` are gone.

**Tests:** `FlotationTest` (the split, the cake, reagents by the tonne); game tests `flotation` (3 ingots, the rest of the copper in
the pond) and `flotation_stops` (NO AIR, NO REAGENT, a full pond, tails with nowhere to go).

## R4a: the crushing circuit (2026-11-05)

The crushing circuit runs as crushing-circuit.md says, on Wildcatter v0.3.0's assayed slurry:
- every part runs on its own motor at its share of 50 t/h; no controller and no shortcut;
- dry rock moves in kg on belt runs, each driven from its head end (7.5 kW each 30 m), with lumps drawn on the belt;
- grizzly, jaw, screen and cone in closed circuit; sand onto the ball mill;
- the ball mill and cyclones at a 250 % circulating load, with water from Wildcatter pumps; mill discharge and fines slurry on
  steel pipe, each carrying the rock's mineral assay;
- the ore bin feeds the belt under it.

**Tests:**
- `CrushingRatesTest` (rates, Bond's law, drives, slurry water, rock loads, assays);
- game tests `crushing_circuit`, `bin_feeds`, `belt_drive` and a new `not_built`; every scene now has padding and its chunks
  loaded, as the true-size parts reach past the test's own chunk.

## R3: big parts and real models (2026-11-05)

Every static machine in the R2 specs is now a Wildcatter big part with a real model:
- crushing: grizzly, jaw, screen, cone, conveyor drive and belt runs, ball mill, cyclones;
- flotation: conditioning tank, flotation bank, blower, thickener, filter;
- dump station: ramp, pocket, apron feeder, ore sorter, ore bins;
- the traction rectifier.

Each part has:
- one item that fills its real size, with the ghost outline;
- pieces that break together;
- real collision;
- its motor box on the west face of its main cell.

The models come from `tools/models/`, using Wildcatter's tool and materials.

The plant keeps running on the old shortcut until R4 (Tim's choice). A line forms from parts joined by touching or driven belt runs.

**Tests:**
- `ModelLintTest` and `MachineTest`;
- game tests `part_break`, `not_built` and `terminal`;
- the crushing, flotation and haul scenes rebuilt on whole parts.

`./gradlew build` and 18 NeoForge scenes pass.

## R9: real vehicle length (2026-11-06)

Tim chose real length. `Haul.carOffset` and `tailOffset` replace the one spacing; `tools/models/vehicles.py` makes the boxes and skins. The longer locomotive left the first car outside the old five-row chute, which only the filmed `haul` showed, so the chute is 13 rows and `tbm_train_loads` covers it on the server. 50 NeoForge scenes pass; `train`, `dump`, `haul` and `hand` were filmed.

## R10: the playthrough, and G: the spiral bank (2026-11-06)

`Playthrough.java` plays the mine in a real world; `spirals` films the new part. Tim saw the first attempts die: the scene dug the pit under the player, who fell in, and the locked test window sat on the death screen. The player now goes to spectator first, and the scene fails if they die. The mean tick was 12 ms with six generators and the whole plant running. Bill of materials in `playthrough-bill.txt`: about 3,700 copper ingots, 2,100 iron, 1,000 steel and 1,300 plastic sheet, mostly cable and busway. 51 NeoForge scenes pass.

## Phase 4a to 4c (2026-11-06)

Spec: docs/multiblocks/full-recovery.md. `FullRecoveryRates` holds the shares and is unit-tested to 95 % and 4 items for both routes. NeoForge scenes `full_recovery`, `leach_no_acid`, `byproducts`, `heap_leach`: 55 pass. The filmed `recovery` scene and 4d follow Wildcatter v0.4.0.

## Phase 4d (2026-11-06)

Tails to Wildcatter's lined cell on v0.4.0, the pond retired as a legacy part, scenes `tails_to_cell` and `cell_breached`. The playthrough now covers the top rung, the cell and real pumped water.

It passed on 2026-11-06 in 15 minutes on Wildcatter v0.4.1, the release with the solver fix: 0 unsolved circuits, 0 trips, mean tick about 18 ms late in the run, 3 ingots at the main filter, 3 nuggets at the scavengers' filter, 5 at the cells, 3.3 t in the cell. On v0.4.0 the same run stalls, as two generators never close their breakers. One run on v0.4.1 died early when a creeper killed the player placing parts in the pit, so the playthrough now runs on peaceful; it says so in its staged list.

Three things it found that scenes did not: that solver failure (probably also the old "busway onto the rectifier" gap, which has the same symptom; not re-tested); the blower refusing a scavenger bank, hidden by force-running; and the water pump leaving the flotation set no margin to start the bank.

## Phase 4e: selective lead-zinc flotation (2026-11-07)

Spec: docs/multiblocks/selective-flotation.md. Tim said yes on 2026-11-06, and chose that a plain bank fed both loses the zinc it floats. `plant.Selective` holds the logic and is unit-tested; a slurry's assay carries the held-down sphalerite as its own entry, so no Wildcatter release was needed. New: `activation_tank`, `zinc_sulfate`, a BULK or SELECTIVE setting on the conditioning tank. NeoForge scenes `selective_float`, `selective_no_reagent`, `no_activation_tank`, `bulk_loses_zinc`, `bulk_one_mineral`; a force-run film `selective`.

The playthrough does not build this circuit. It was rerun on the change and pays the same on its copper feed.

## Wildcatter 13i: PLC signals (2026-11-07)

Wildcatter's Phase 13 ends here. Plant motors, the sorter's gates, mine switches, block signals and the TBM's cable junction are devices on Wildcatter's control cable (DESIGN.md, "PLC signals"). NeoForge scenes `plc_plant`, `plc_switch`, `plc_signal`, `plc_tbm_go`: 66 pass.

The playthrough now runs the mine from a PLC. A large CPU on a staged deck, with 2,076 cells of control cable to 37 devices, runs a generated structured-text program: it starts the eight generators, then each machine downstream first, one a second, then sends GO to the TBM. Nothing is started by hand. It passed on 2026-11-07 in 16 minutes on Wildcatter 0.5.1: 0 unsolved circuits, mean tick about 9 ms, 3 ingots at the main filter, 3 nuggets at the scavengers' filter, 5 at the cells, 3.2 t in the cell, the TBM 35 blocks down the decline.

Two things it found: a GO through the junction skipped the cab's own start (heading and origin), so the machine read NOT BUILT (fixed, `plc_tbm_go`); and Wildcatter 0.5.0's cable walk stopped after about 1,600 cables, reaching 26 of the 37 devices (fixed in Wildcatter 0.5.1).

## Open pit P1: loose ground, the haul truck, the truck dump, the wheel loader (2026-11-09)

Spec `docs/multiblocks/open-pit.md`, approved 2026-11-09; Tim's P1 answers: soil goes to dumps, and the truck dump is separate parts.
DESIGN.md, "The open pit (P1)".

- **The settle rule** (`mine.Settle`). The first way written, a 1-in-3 chance for a block to creep past a single step, measured 43
  degrees, and 1 in 2 the same; creeping every time flattened piles to 12. It is now a look-ahead: a block rolls on where resting
  would leave a step of 2, or 4 up in a run of 4 columns. 2,000 blocks tipped at one point stand at 36 degrees (`SettleTest`).
- **The truck** drives on the server from the rider's keys, by `pit.PitDrive`: loaded up a 10 % road it settles at 3.24 m/s (11.7
  km/h), as the spec's formula gives; the retarder holds it to 15 km/h down one (`PitDriveTest`). It steps up a block, and stops
  at a 2-block wall (`pit_truck_drive`).
- **The truck dump.** The bumper first stood 1 m: the model tool clips a collision box to the part's cells, and a truck steps up a
  block, so it drove over it into the hopper (found by `pit_hopper`). The wall is now 12 x 5 x 2 so the bumper's 1.5 m collides.
  `pit_hopper`: backed up with the movement keys, the truck stops at the bumper with its tail over the hopper; 54 t of cobble goes
  in as 20 bank blocks, sand is refused, a full hopper reads FULL, and the apron feeder draws it to the sorter's bin.
- **The loader** fills a 91 t truck from a sand face in 5 passes of 12 blocks (19.2 t a pass, the 13th block would pass its 20.4 t)
  (`pit_loader_soft`); reads "hard rock: blast it first" at stone (`pit_loader_hard`); digs ten blocks of muck to exactly 20.25 t
  (`pit_loader_muck`).
- NeoForge scenes: **74 pass** (66 before, 8 new).

- **The filmed scene** (`pit`, Fabric client, 2026-11-09). A player rides the loader and fills the truck with the jump key in five
  passes (0, 20.3, 40.5, 60.8, 81.0, 91 t), backs it to the bumper with the movement keys (it reaches 3 m/s in reverse and stops at
  z 16, its tail over the hopper), and tips it in: the feeder and sorter put 12 copper ore and 20 cobble in the bin. The second
  truck tips 40 t of sand on the ground, 25 blocks in a heap behind its tail. The screenshots found two things:
  - **The cabs were blind:** the window pixels were opaque, so from the seat the driver saw only the cab's inside. They are now clear
    (the cutout drops them), and from the loader's seat the plant and the truck show through.
  - **What a hopper leaves:** the hopper takes whole bank blocks of each ore, so after a 91 t tip 4.6 t stays in the body (2.0 t of
    copper ore, 2.6 t of cobble), and the body is drawn full. Tim chose (2026-11-09) that the hopper keeps it: what is under a block of
    each ore stays in the hopper and the next load makes it up, and the truck comes away empty (`pit_hopper` checks 55 t in, 1 t
    kept, then 1.7 t more makes the 21st block).
  - The scene itself first failed for its own reasons: it was built below a flat world's floor, it held entities that their chunks had
    reloaded as new ones, and its key presses were shorter than the server's tick. All three are fixed in the scene.

## Open pit P2a: survey, self-driving trucks, the self-running loader, diesel and the fuel bay (2026-11-09)

Spec `docs/multiblocks/open-pit.md` ("P2 decided with Tim", "P2a as planned"), numbers approved 2026-11-09. DESIGN.md, "The open
pit (P2a)".

- **What building it changed in the plan** (each in the spec, marked proposed where it adds a number):
  - **Turning round:** a truck reaching the stake before an end mostly faces the end, so it cannot back straight in. It now turns
    round in the open at that stake (about 25 m) until its nose points away from the end, then backs in, as on a dump's turning
    area. Laying the road out as a Y is no longer needed.
  - **Steering on a look-ahead point** 15 m on along its lane: steering for the far stake's lane point left trucks off their lane
    for most of a leg, and two trucks going opposite ways stopped for each other (found writing `pit_one_way`).
  - **One-way legs:** a truck waits 40 m short of the leg (not at the stake, where it blocked the truck coming off the leg); only a
    truck going its way is kept 50 m behind (an oncoming truck counted too, and both stopped); obstacles are looked for along the
    path it is steering for, as far as the next stake.
  - **Backing in** follows the line from the turn stake to the end (the first try came off the turn 2.4 m wide and caught the
    loader's side, and stopping there counted as arriving); stuck 5 m or more short of the stake for 30 s it stops and says so.
  - **At a DUMP POCKET** it backs on up to 3 m past the stake until the bumper stops it: with the stake 2.5 m short of the bumper
    the hopper was out of reach of its tail, and it tipped on the pad.
- **Diesel:** flat out the truck burns 201 L/h (`PitP2aTest`); 5 s of driving burns under 0.28 L, and parked with nobody in it
  nothing (`pit_fuel_burn`). A whole filmed haul cycle used 3 L of the truck's 1,100 and 6 L of the loader's 1,500.
- **The haul** (`pit_truck_haul`, server, about 2 minutes of game time): backs 40 m to the self-running loader, is loaded in five
  passes, climbs the 9 % ramp loaded at 5-6 m/s, turns round at the turn stake, backs to the bumper, tips 33 blocks into the hopper,
  and drives back.
- **The filmed scene** (`pit_haul`, Fabric client): the same cycle with the camera on the truck. Its stills show it backing beside
  the loader and loaded, on the ramp full, tipping over the bumper into the hopper, and going back empty past the stakes. The film's
  first run ended before the truck came back: the scene's own loop was too short (fixed).
- NeoForge scenes: **83 pass** (74 before, 9 new).

## Round wheels and the films again (2026-11-09)

Tim: "make the wheels round as possible and redo the videos, the screen was asleep". Each pit wheel is now its own group on its axle,
built of 1/16 m slices following the circle (`tools/models/pit_vehicles.py`), with hub and nuts, turned by the distance rolled
(`PitVehicleEntity.rolled`, from the synced speed on the client). The first film showed see-through gaps in the loader's wheel: the
slices share one texture rectangle and use different corners of it, and the unpainted corners were transparent; the rectangles are
now painted solid. Rail wheels: eight-sided at 4 px (`tools/models/vehicles.py`). Both pit scenes were filmed again with the display
kept awake (`caffeinate -dimsu`).

## Open pit P2b: the haul dispatch office on the PLC (2026-11-09)

Spec `docs/multiblocks/open-pit.md` ("Haul roads, surveying and dispatch", "P2b as built"). Built while Tim was away; the open
questions were decided on the best recommendation and logged in `docs/questions/p2b-dispatch.md`. DESIGN.md, "The open pit (P2b)".

- **The office** (`haul_dispatch`, 3 x 3 x 3): a `ControlDevice` with 43 points (5 a route for 8 routes, then FUEL LOW, BLOCKED,
  AUTO) and a 5 kW load on the low tier. The survey book adds a route to it; trucks on that route report once a second and read RUN.
- **PARK stakes work:** RUN off sends a route's trucks on to its PARK stake (or the next stake), where they stand with their engines
  off; RUN on sends them back to work. A stopped PLC parks the route; HAND runs it; with no power the trucks stop at their next stake.
- **What building it found in P2a:** on a straight road a truck turning round at the stake before an end comes out about 15 m to the
  side and backs in at an angle; it missed the end stake by more than 1.5 m and circled behind it for ever. A truck whose tail
  passes the end within 4 m of the line now stops there (`TruckRoute.PAST_END_ACROSS`). `pit_truck_haul` (a bent road) is unchanged.
- **Scenes:** `pit_dispatch` (spec: RUN on, the truck hauls and tips, LOADS pulses once, TRUCKS 5 mA, TONNES up; RUN off, it drives
  back to the PARK stake and stands with its engine off burning nothing, RUNNING off; RUN on, it goes back to work),
  `pit_dispatch_alarms` (FUEL LOW at 10 % and not at 50 %; BLOCKED after 30 s at an armour stand, clearing when it goes),
  `pit_dispatch_fallback` (the PLC stops writing RUN and the truck parks at its next stake; HAND runs it again; the office's power off
  stops it at its next stake with its loops at 0 mA; power back, it goes on). Unit tests `DispatchTest`.
- NeoForge scenes: **86 pass** (83 before, 3 new).

## The dispatch office filmed; ground tips come away clean (2026-11-09)

- **`pit_dispatch` (Fabric client, filmed):** the office in HAND runs the route; the truck hauls to the dump point and tips (LOADS 1,
  30 t in the hour); switched OFF it drives back and parks at the PARK stake (z 71, engine off); back in HAND it goes to work.
- **Found by the film:** after a ground tip the truck was drawn full, holding the 1.2 t under a whole block of sand. A ground tip now
  goes out to the nearest whole block or quarter and the rest spills, so the truck comes away empty (`pit_truck_tip`: 30 t of sand
  is 19 blocks). Logged in `docs/questions/lead-decisions.md`.

## TBM gaps: the gantries, the trains and the machine's loose ends (2026-11-09)

Tim was away and asked for all known gaps closed on the best recommendation; the choices are in
[docs/questions/tbm-gaps.md](docs/questions/tbm-gaps.md).
- **Gantries on wheels:** each runs on a `TrainPath` along each of its two rails; a rail gone under one stops the machine, and it goes
  back on by itself when the rail is back. On the grade they now stand on their rails, half a block lower than the course's line.
- **Every car loads:** the loading point reaches a whole 10-car train, so a train indexes forward through it.
- **The trailing cable** is drawn from the reel back to the junction, and bars on the reel turn as it pays out.
- **The transformer** (4.16 kV to 480 V, 150 kVA) is a real part of the junction's circuit; the pump and conveyor draw through it.
- **The quench tank** rides the pump gantry and is lost with it.
- **The cab's screen** stays open across the lift and set-down.
- **Recipes:** steel and Wildcatter's parts for the machine and its gantries.
- **Track too long** for Wildcatter's solver (over 4,000 nodes) is dead and says so; it used to keep its last volts silently.
- **Busway straight onto the rectifier** works on Wildcatter 0.5.1 (`rectifier_busway`): the R10 gap is closed.
- **Left:** per-part boxes for the running machine (a plan in the questions file); the legacy parts are kept, as no release has
  shipped yet.

**Tests:** `GantryWheelsTest`; NeoForge scenes `tbm_gantry_derail`, `tbm_train_fills`, `tbm_quench_gantry`, `tbm_screen_follows`,
`tbm_transformer`, `tbm_recipes`, `track_too_long`, `rectifier_busway`; `tbm_gantries_decline` now checks the pump gantry on its
rails. 91 NeoForge scenes pass. `TAILINGS_SCENES=a,b` runs only those scenes. The client films were not re-shot: `tbm`, `haul` and
`hand` show the changes.

## Plant gaps: the ore plant's and dump's known gaps closed (2026-11-09)

Tim (2026-11-09): "close all known gaps", decided on the best recommendation while he was away
([docs/questions/plant-gaps.md](docs/questions/plant-gaps.md)). Each closed gap has a NeoForge scene.
- **Belts have a transit time:** rock rides at the spec's 2 m/s from where it dropped on (`plant.BeltTravel`): a 20 m run delivers in
  10 s (`belt_transit`), and the lumps are drawn on the metres the rock is riding. A run whose head end is blocked stops.
- **Feed faces are checked:** the grizzly, cone and heap pad take rock over their top, the screen and mill over their north (feed) end;
  the jaw only from its grizzly; a part beside an output face is no chute (`feed_face`). The old layouts broke the spec in three
  places, all moved to belts: the screen's oversize went straight into a cone touching it (the R3 sketch, the crushing scenes), and the
  playthrough fed the screen through its west face and from under it. The playthrough's crushing layout is rebuilt (the undersize and
  the jaw's product onto a feed belt over the screen's north end; the oversize by belt over the cone, now on the floor; the cone's
  product round onto the jaw's belt), and a server scene builds it block for block (`playthrough_crushing`) so it is proved before the
  15-minute film. Its power drops were re-planned with it and checked cell by cell for clashes; the playthrough itself was not run.
- **The screen's gravel face:** a belt on the west face of its south end takes the 6-40 mm out as aggregate, and a hopper or chest at a
  belt's head end takes rock as items, so gravel ends as `gravel_*` items (`screen_gravel`).
- **Oversize on the pocket's grizzly:** one item in a hundred tipped stays on the bars; 32 choke it; a pickaxe breaks a piece through,
  a sneaking empty-hand click lifts them off (`pocket_oversize`; `dump_station` breaks its one piece through).
- **The train waits at the signal before the dump** while the pocket cannot take its load (`dump_signal`).
- **Bulk concentrate:** lead and zinc concentrate piped into one filter pay the lead and lose the bank's zinc (`bulk_concentrate`,
  `SelectiveTest`).
- **The ore sorter has a screen** on both loaders (`SorterMenu`, `SorterScreen`; `sorter_screen`); the playthrough sets its sorter
  through it.
- **Metal in kg:** the assay counts the vanilla metals in kg, an ingot 100 kg (`CrushingRatesTest`, `BeltTravelTest`); the flotation
  and recovery scenes feed real masses.
- Kept: concentrate as mineral only, and the `fines_*` items. The waste bin's way out is written in DESIGN.md.
- NeoForge scenes: **91 pass** (83 before, 8 new). Unit tests pass. The Fabric client scenes were not run (the lead films them).

## PLC 13i: the last signals (2026-11-09)

Tim (2026-11-09): "close all known gaps", decided on the best recommendation while he was away
([docs/questions/plc-13i.md](docs/questions/plc-13i.md), with two questions for him). The three 13i lines are gone from Known gaps.
Wildcatter 0.5.1's API only (its AO4 module's `commandMa`). DESIGN.md, "PLC signals".
- **The sorter's routing:** NORTH/EAST/SOUTH/WEST TAKES, a rock code a side on 4-20 mA (code *n* at 4 + *n*/4 mA; 0 nothing, 1-38
  `Rock`'s list, copper ore 10). In AUTO on a fresh loop it overrides the screen, which comes back when the PLC stops. Found on the
  way: a gate the PLC shut stayed shut when the PLC went quiet; gates are now refreshed every tick (`plc_sorter_route`).
- **The TBM's course:** TARGET Y (-64 to 320) and FLAT LENGTH (0-400 m) on the cable junction, with read-backs. It takes while the
  machine stands, before a GO in the same scan; a target that would move floor already cut is held (`plc_tbm_course`: a flat 3 m
  drive set by the PLC over the screen's 32, holding while it drives though the PLC asks for 400).
- **Slurry readings:** LEVEL (feed tank over its hold), FLOW (out by pipe, 0-60 L/s) and DENSITY (1.0-2.0 kg/L from the product's
  solids) on every slurry part with a motor; Wildcatter's transmitters read them too (`plc_slurry`: the cyclone's flow at the tank's
  31 L/s draw, 1.28 kg/L fines, the tank backing up to full).
- Plain Java in `plant.PlcScale` and `block.stage.SlurryGauge`; `PlcScaleTest` pins the rock codes.
- NeoForge scenes: **105 pass** (102 before, 3 new). Unit tests pass. The Fabric client scenes were not run (the lead films them).
