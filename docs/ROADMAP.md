# Tailings — Roadmap

Phases 0 through 3 are **done** and on `main`. **The realignment with Wildcatter (R) is CURRENT**, then Phase 4. One phase is the current target. "Done" here means the behavior is in the code, `./gradlew build` passes, and `:neoforge:runGameTestServer` passes. There is no Field Manual yet, and there is no Fabric client game-test task.

## Phase 0 — Set up — done

Pull request [#1](https://github.com/timhunsaker-alt/Tailings/pull/1).

- Multi-loader project (`common`, `fabric`, `neoforge`), its own jars, hard dependency on Wildcatter 0.2.0+.
- Plain-Java `recovery` package: 25%, 50%, 75%, 95%, and tests that no route pays more than the block holds.
- CC0, and CI that runs `./gradlew build` and `:neoforge:runGameTestServer`.

## Phase 1 — Rock and the starter mill — done

Pull request [#2](https://github.com/timhunsaker-alt/Tailings/pull/2).

- Size items for every feed: crushed, gravel, sand, and fines. Feeds are cobblestone, stone, granite, diorite, andesite, deepslate, tuff, sandstone, vanilla iron, copper, and gold, and the 27 Wildcatter ores.
- Hand stamp mill and hand screen change size and pay nothing. The shaking table (gravity) and the magnetic separator (iron) pay the 50% route.
- Nuggets from processing. Nine nuggets craft into one mineral item. Iron and gold use `minecraft:iron_nugget` and `minecraft:gold_nugget`. Copper has a Tailings nugget.
- Separate spiral and jig blocks were not built. The shaking table is the gravity machine.

## Phase 2 — The concentrator — done

Pull request [#4](https://github.com/timhunsaker-alt/Tailings/pull/4).

- True-size crushing circuit: grizzly, jaw crusher, 6 m belt, double-deck screen, cone crusher, ball mill (4.16 kV), cyclone cluster. Spec: [multiblocks/crushing-circuit.md](multiblocks/crushing-circuit.md).
- The ball mill turns sand into fines. Fines are items. The bank treats them as 90% liberated and still pays the even 75% ladder.
- Flotation bank: conditioning tank, four roughers, two scavengers, two cleaners, air blower, concentrate thickener, pressure filter. Spec: [multiblocks/flotation-bank.md](multiblocks/flotation-bank.md).
- The bank pays 75% for galena, sphalerite, pentlandite, molybdenite, cobaltite, acanthite, platinum, and vanilla copper and gold.
- Reagents are Tailings items. Lime is calcite smelted to quicklime, because Wildcatter has no lime. Xanthate, MIBC, and copper sulfate are crafted. Copper sulfate is dosed only when the feed holds sphalerite.
- The powered-machine crafting recipes are placeholders (eight iron ingots around one copper ingot). They are not true-size recipes.

## Phase 3 — The mine — done

Spec: [multiblocks/tunnel-drill-haulage.md](multiblocks/tunnel-drill-haulage.md). Pull requests [#5](https://github.com/timhunsaker-alt/Tailings/pull/5) (first drill), [#6](https://github.com/timhunsaker-alt/Tailings/pull/6) (haulage), [#7](https://github.com/timhunsaker-alt/Tailings/pull/7) (Tim's revisions), [#8](https://github.com/timhunsaker-alt/Tailings/pull/8) (block and item textures), [#9](https://github.com/timhunsaker-alt/Tailings/pull/9) (entity models and the remaining sprites).

What the revisions in pull request 7 delivered, and what the later art pull requests added:

- **Moving drill and chunk loaders.** The drill is an entity-owned moving assembly, not a static multiblock. Ticket force-loading keeps the drill and the trains running when players are away. NeoForge uses a `TicketController` (`tailings:runner`). Fabric uses `ServerLevel.setChunkForced`. Tickets drop when the machine stops or is removed.
- **The drill stops when the buffer is full**, so a train can keep the heading loaded. The pause does not latch the drill off. The next tick tries again. One face of muck is up to 64 items, which is also how many items move into a waiting car in one tick.
- **Smooth ramps.** Slope and rail beds are eight cuboid slices (2 pixels of run each), and the collision matches. A 1-in-2 pair is a lower half then an upper half. Walkway stairs and a landing every other block stay as they were.
- **Floor and bed.** Mined rock becomes the floor pieces. Gravel ballast is only on the flat. Cobble fills caves under the floor. The flat-rail model now uses the ballast texture under the rail overlay. The switch bed uses ballast too (2026-11-09).
- **Lining.** The default ring is cobble or any stone the drill has just mined or is holding (stone, cobble, deepslate, granite, diorite, andesite, tuff, sandstone). Crafted segments (4 gray concrete + 1 iron ingot = 4) are optional. They are used only when a full ring of 28 is in the segment slot.
- **Quench water.** The tank holds 256 m³. Pumped water fills it first. Excess is voided. A water bucket can still top the tank up. Lava is quenched from that tank, then cut as rock.
- **Trains.** Up to 10 cars. Each car holds 9 stacks (576 items), so one 64-item drill face fits in a car. The real Granby figure in the spec stays about 3 t. That number is not the inventory size. The dump pocket holds one 10-car train. The apron clears one stack (64 items) each second.
- **Power on the rail.** A fed conductor section is live along its whole length and is still safe to walk: no shock. Mine lights and signal lamps draw from a neighboring fed rail. The locomotive has a headlight. A thrown `mine_switch` sends a train clockwise, and the same signal check still applies.
- **Trains hurt players and mobs.** Default damage is 8. It is `config/tailings-train.properties` (`damage=true` or `damage=false`, and `amount=`), not a gamerule. Only the locomotive is an entity. Its hitbox covers the consist. Cars are data on the rail blocks, drawn by the locomotive's renderer.

Pull request 3, between the phases, is docs only: an unscheduled PLC idea. See Ideas below.

Ventilation (old 3c) was only a proposal then. It is built as L1 (below).

## Realignment with Wildcatter (R) — done

A review on 2026-11-05 found Tailings far from Wildcatter's patterns:
- **Machines:** footprint code of its own, not Wildcatter's big parts, and each machine a box of identical vanilla cubes.
- **The plant:** the process short-cuts the stages.
- **Art:** 16 px textures with no kept sources.
- **Safety:** bugs that leak chunk load or damage the world.

R brings it in line, one wave at a time.

**Decided with Tim on 2026-11-05:**
- **The drill:** moves like a Create contraption, or a real boring machine. While running, its parts lift into one moving entity that
  renders them, and they set back down as blocks when it stops.
- **Art:** keep the current art. Look at it in game and fix only what's needed.
- **Order:** safety fixes first, then the specs.
- **Git:** straight to `main` from `timhunsaker-alt`, as with Wildcatter.

**The waves:**
- **R1. Safety fixes — DONE (2026-11-05).**
  - chunk tickets that time out on their own;
  - no leaked drill entity;
  - the drill stops as "blocked" instead of overwriting;
  - no per-tick disk reads or light-block churn;
  - consists through turnouts, and the rectifier feeds switches;
  - the reagent jam;
  - contents drop however a machine is removed.
- **R2. Specs in Wildcatter's format — DONE (approved by Tim 2026-11-05).** The specs are listed in
  [multiblocks/README.md](multiblocks/README.md): crushing circuit, flotation bank, tunnel boring machine, haulage and dump station.
  The old drill-and-haulage spec is split into the last three. Decided with Tim on 2026-11-05:
  - **Plant flow:** belts between the dry stages; from the ball mill on, slurry on Wildcatter's steel pipe carrying its mineral
    assay. That needs a Wildcatter change, proposed in Wildcatter's ROADMAP Ideas ("Assayed slurry on the steel pipe").
  - **Rate:** the plant runs at the real 50 t/h.
  - **Flotation:** one `flotation_bank` part holds the eight cells, as Wildcatter's coke battery holds its ovens.
  - **The TBM:**
    - the full real machine as true-size big parts;
    - on the decline it stays a contraption, drawn pitched;
    - its back-up gear rides gantries on the twin track it lays.
  - **Trains:** they glide, and every car is an entity.
  - **The dump station:** real parts (ramp, pocket, apron feeder, sorter, bins).
  - **Belts:** laid like pipe, with a drive head.
- **R3. Big parts — DONE (2026-11-05).** every part in the specs on Wildcatter's big parts:
  - `BigParts` and the piece property;
  - shapes and the ghost outline;
  - terminal faces;
  - Python-generated piece models and ModelLint.

  It includes the new parts: belt and drive, flotation bank, dump ramp, pocket, feeder, sorter and bins.
- **R4. The real process — DONE (2026-11-05)**, per crushing-circuit.md, flotation-bank.md and dump-station.md: each stage hands off to the next, at
  the real rate; nothing runs unbuilt. Built on Wildcatter v0.3.0's assayed slurry. Decided with Tim on 2026-11-05: plant first,
  in three waves; a `tailings_pond_placeholder` takes the tails until 4d; a full pocket makes the train wait; belts draw their
  lumps.
  - **R4a. The crushing circuit — DONE (2026-11-05).** Belts with head drives, the grizzly, jaw, screen, cone, ball mill and
    cyclones each on their own motor at 50 t/h; mill discharge and fines slurry on steel pipe with their assay; the ore bin feeds
    its belt.
  - **R4b. Flotation to items — DONE (2026-11-05).** The conditioning tank, bank, blower, thickener and filter on pipe, at the
    75 % ladder; the filter pays nuggets packed into ingots; the tails to a `tailings_pond_placeholder`.
  - **R4c. The dump station — DONE (2026-11-05).** The pocket takes whole car loads up to one train; the apron feeder draws a
    stack a second to the sorter; the sorter sends each type out of the side set for it into the ore bin there; a loaded car over
    a full pocket holds the train.
- **R5. Haulage, per haulage.md — DONE (2026-11-05).** Decided with Tim on 2026-11-05: the rectifier is a Tailings DC source on Wildcatter's DC solver;
  two waves; couplings never break in R5; trains saved on rails become entities.
  - **R5a. Trains as entities — DONE (2026-11-05).** Every vehicle is an entity and trains glide along the rails, through switches
    and up and down the decline, at speeds from power over force; placing, coupling, run/hold/reverse, signals, the dump, the
    drill's loading point, derailing, harm, chunk tickets; the ratings in `haul.Haul`.
  - **R5b. The rectifier on Wildcatter's DC solver — DONE (2026-11-05).** A 250 V source behind 2 %, with its DC breaker (1.25 x
    for 1 s) and an AC draw that follows the trains; the rails as the conductor rail; locomotives as DC loads whose speed follows
    the track's volts.
- **R6. The TBM**, per tunnel-boring-machine.md. Decided with Tim on 2026-11-05: 1 in 2 only; broken lining does not leak; ballast on
  the decline too; three waves; riding is a seat in the cab.
  - **R6a. The real parts, as blocks — DONE (2026-11-06).** Cutterhead, segment erector, main beam with its cab, track layer and
    cable junction as true-size parts with models; the ring behind the erector, the floor and twin track on ballast behind the track
    layer; the cab's logic in `TbmCore`.
  - **R6b. The contraption — DONE (2026-11-06).** The parts lift into one gliding entity, pitched on the decline; they set down on
    the flat, or park on the decline; dismantling; the seat.
  - **R6c. The gantries — DONE (2026-11-06).** Pump, reel and conveyor on the twin track, coupled in order and towed along it; the conveyor's loading point, the cable end, a missing gantry stopping the machine.
- **R7. Testing and art — DONE (2026-11-06):** Fabric client game tests with screenshots and film for every spec:
  `TAILINGS_ONLY=parts,train,tbm,crushing,flotation,dump,haul,hand ./gradlew :fabric:runClientGameTest`, each scene filming the game
  window to `fabric/build/scene-videos`. `haul` runs the whole mine: the machine on a decline with a real train taking its muck to the
  dump. `hand` places, couples and breaks gantries and starts the machine with real input, then saves and quits in mid-drive and
  checks it goes on after reopening. The in-game look found one thing to fix, the cab's screen; the models stay as they are.

- **R8. Sound — DONE (2026-11-06, Tim's request):** a running loop for every powered plant part, the machine, the pump and conveyor gantries and the locomotive, and one-shots for tipping, loading, setting a ring and coupling ([sound.md](sound.md)). The client scenes fail if a machine's sound does not start.

- **R9. Vehicles at their real length — DONE (2026-11-06, Tim's decision):** a 3 m locomotive and 2 m cars on 0.5 m couplings, new models and skins from `tools/models/vehicles.py`, boxes along the track. The conveyor's chute reaches 13 rows, so a stopped train still loads its first four cars.

- **TBM gaps — DONE (2026-11-09, decided while Tim was away: [questions/tbm-gaps.md](questions/tbm-gaps.md)):** the gantries run on their own wheels on the twin track; every car of a train loads; the trailing cable is drawn and the reel turns; the reel gantry's transformer is on the circuit; the quench tank rides the pump gantry; the cab's screen stays open across the lift; real recipes for the machine and its gantries; track too long for the solver says so; busway straight onto the rectifier works on Wildcatter 0.5.1. Per-part boxes are left with a plan; the legacy parts are kept.

- **R10. The playthrough — DONE (2026-11-06, Tim's request):** `TAILINGS_ONLY=playthrough ./gradlew :fabric:runClientGameTest`, about 15 minutes, filmed. A generated survival world at a fixed seed; every part crafted through the game's own recipe matching and placed through the game's item use from a survival hand; six real generators, nothing force-run; the machine bores real rock; real ore by train to the dump, through crushing and flotation to ingots, at 11.5 game minutes. It writes the mine's bill of materials to `playthrough-bill.txt`. It found: the machine needs two gas sets and two substations (below); the old film layouts blocked three motor boxes; a live fault with busway straight onto the rectifier (Known gaps).
- **G. The gravity circuit — DONE (2026-11-06, as recommended, Tim):** a spiral bank on the cyclones' overflow pays the 2x route at plant scale, for what does not float, iron included ([gravity-circuit.md](multiblocks/gravity-circuit.md)). Jigs left out, with the reason.

**Open DECISIONs from the R2 specs (listed, not chosen):**
- Selective lead-then-zinc circuits: built in 4e (selective-flotation.md).

## Phase 4 — Full recovery — done

Spec: [full-recovery.md](multiblocks/full-recovery.md), decisions taken by Tim 2026-11-06. **All four waves are built** (2026-11-06). The playthrough now runs the top rung and the lined cell for real, on real rock, with nothing force-run; the heap and the by-product plant are proved by NeoForge scenes only, as copper is no by-product ore and the playthrough builds no heap. **4e, selective lead-zinc flotation, is built too** (Tim, 2026-11-06; spec: [selective-flotation.md](multiblocks/selective-flotation.md)): on a plain bank fed both, the zinc it floats is lost.

- **4a.** Scavenger and cleaner flotation beyond the fixed bank, and a regrind.
- **4b.** Leaching the flotation tails: tank leach with Wildcatter's sulfuric acid, and heap leach on a lined pad.
- **4c.** By-products, the 95% route: cadmium and indium from sphalerite, silver from galena, rhenium from molybdenite, cobalt from pentlandite, monazite rare earths from heavy mineral sand. The played multiple is 4x. The mass paid is 95% of what the block holds, not 100%. 95/25 is 3.8. The even 4x is the written exception in [VISION.md](VISION.md).
- **4d.** The lined tailings cell, shared with the Wildcatter uranium-mill idea. What the mill rejects has to go somewhere.

## Wildcatter 13i — PLC signals

Wildcatter's Phase 13 ends with Tailings: its plant motors, ore sorter gates, mine switches, block signals and the TBM's cable
junction are devices on Wildcatter's control cable (Wildcatter v0.5.1). See DESIGN.md, "PLC signals". NeoForge scenes
`plc_plant`, `plc_switch`, `plc_signal`, `plc_tbm_go`. The playthrough starts the mine from a PLC: a generated structured-text program
starts the machines downstream first, one a second, then sends GO to the TBM.

The last signals (2026-11-09, decided while Tim was away, [questions/plc-13i.md](questions/plc-13i.md)): the sorter's routing by a
rock code a side, the TBM's course (target Y and flat length) on its junction, and a slurry part's level, flow and density. NeoForge
scenes `plc_sorter_route`, `plc_tbm_course`, `plc_slurry`; `PlcScaleTest`.

## Open pit — P1: loose ground, the haul truck, the truck dump, the wheel loader (2026-11-09)

Spec [multiblocks/open-pit.md](multiblocks/open-pit.md), approved 2026-11-09, with Tim's P1 answers: soil goes to dumps, and the truck
dump is separate parts. Built: the settle rule (`mine.Settle`, 37 degrees), muck, real-tonne loads, the 100 t haul truck you drive,
the truck dump wall and 400 t pit hopper feeding the existing apron feeder, and the wheel loader on soft ground and muck. NeoForge
scenes `pit_settle`, `pit_truck_tip`, `pit_truck_body`, `pit_hopper`, `pit_truck_drive`, `pit_loader_soft`, `pit_loader_hard`,
`pit_loader_muck`; unit tests `SettleTest`, `PitDriveTest`; the filmed client scene `pit`.

## Open pit — P2a: survey, self-driving trucks, the self-running loader, fuel (2026-11-09)

Tim's P2 answers (2026-11-09): the loader can run itself as well as be driven; fuel burns by the engine's work; refuelling only at a
fuel bay; P2 in two waves. Built: survey stakes and the survey book, routes surveyed for grade, clearance and width, self-driving
haul trucks out and back with turning round and backing in, the self-running loader, diesel by work, and the fuel bay. NeoForge
scenes `pit_survey`, `pit_loader_auto`, `pit_fuel_burn`, `pit_fuel_bay`, `pit_out_of_fuel`, `pit_truck_obstacle`, `pit_truck_follow`,
`pit_one_way`, `pit_truck_haul`; unit tests `PitP2aTest`; the filmed client scene `pit_haul`.

## Open pit — P2b: the haul dispatch office on the PLC (2026-11-09)

Built while Tim was away, on the best recommendation (decisions in [questions/p2b-dispatch.md](questions/p2b-dispatch.md)): the
`haul_dispatch` office, a 3 x 3 x 3 `ControlDevice` on the control cable and a small load on the low tier, reaching trucks within
2 km. Up to 8 routes, added with the survey book; per route RUN, RUNNING, TRUCKS, LOADS, TONNES; for the fleet FUEL LOW and BLOCKED.
PARK stakes now work: RUN off sends a route's trucks to its PARK stake (or its next stake) with their engines off. NeoForge scenes
`pit_dispatch`, `pit_dispatch_alarms`, `pit_dispatch_fallback`; unit tests `DispatchTest`.

Next: P3 (the ammonia plant
in Wildcatter, the drill, the emulsion truck, the blast; on its own branch for Wildcatter 0.6.0), P4 (below), P5 (the big shovels and the ultra
truck), P6 (the dragline), P7 (the pit-sized plant).

## Open pit — P4: dry stack, the truck load-out, waste dumps, berms, the track dozer (2026-11-09)

Built while Tim was away on the draft spec [multiblocks/open-pit-p4-dry-stack.md](multiblocks/open-pit-p4-dry-stack.md), decisions in
[questions/p4-dry-stack.md](questions/p4-dry-stack.md). P4a: the `tailings_thickener` (60 % underflow, overflow water back on a water
pipe), the `tailings_filter` (a 15-minute batch press dropping 15 t of `dry_tails`), the truck load-out (`loadout_bin` on
`loadout_frame`, 2 t/s through its gate), LOADOUT routes driven through and round a balloon loop. P4b: dump points with crests,
berms and paddock dumps; NO BERM; a truck backed over a crest is wrecked; the `track_dozer` (CARRY, CUT to grade, RAISED; berms; hard
rock). NeoForge scenes `pit_thickener`, `pit_filter_cycle`, `pit_loadout`, `pit_loadout_haul`, `pit_berm`, `pit_no_berm`,
`pit_over_crest`, `pit_dump_advance`, `pit_dozer_cut`, `pit_dozer_berm`, `pit_dozer_hard`, `pit_dozer_push`; unit tests
`DryStackTest`; the client scene `pit_dry_stack` (written, not yet filmed).

## Plant gaps (2026-11-09, closed while Tim was away)

Tim (2026-11-09): "close all known gaps". Closed on the ore plant and dump, each with a NeoForge scene; decisions in
[questions/plant-gaps.md](questions/plant-gaps.md): belts' transit time at 2 m/s (`belt_transit`); feed faces checked and the old layouts
moved to them (`feed_face`, `crushing_circuit`, `playthrough_crushing`); the screen's gravel face and belts into hoppers or chests
(`screen_gravel`); oversize on the pocket's grizzly (`pocket_oversize`); the train waits at the signal before the dump (`dump_signal`);
lead and zinc concentrate mixed make a bulk cake (`bulk_concentrate`); the ore sorter's screen (`sorter_screen`); metal counted in kg in
the assay. Kept as they are: concentrate as mineral only, and the old `fines_*` items. The waste bin's way out is written, not built
into the playthrough.

## Later — L2 and L3: battery locomotives and bottom-dump cars (2026-11-09)

Built while Tim was away on the draft specs [multiblocks/battery-locomotive.md](multiblocks/battery-locomotive.md) and
[multiblocks/bottom-dump-cars.md](multiblocks/bottom-dump-cars.md), decisions in [questions/l2-l3-trains.md](questions/l2-l3-trains.md).
**L2:** the `battery_locomotive` (8 t, 2 x 22 kW, 58 kWh lead-acid, 46 kWh usable) runs on any rail, fed or not, on its
`loco_battery`; the `battery_charger` (20 kW) plugs into the world's three-phase 30 A receptacle as a Wildcatter load and charges the
battery it holds or a locomotive standing beside it; batteries swap by hand; LOW BATTERY and FLAT; PLC points CHARGING, FULL, CHARGE.
**L3:** the `bottom_dump_car` drops its load through its doors on the move over a `track_grizzly` beside a `dump_cam_rail`, into the
pocket under the track; mixed trains dump each kind at its own station. NeoForge scenes `battery_loco_run`, `battery_loco_climb`,
`battery_loco_flat`, `battery_charge`, `bottom_dump_on_move`, `bottom_dump_full`, `bottom_dump_mixed`, `train_models_l2_l3`; unit
tests `BatteryTest`; the client film `battery_bottom_dump`.
- **Left out, planned (the specs' "Left out"):** lithium packs and fast charging; regeneration down the decline; hydrogen from charging
  and the bay's ventilation (with L1, ventilation); battery life and cycles; battery-electric pit trucks and loaders (open-pit.md);
  doors jamming on sticky muck and frozen loads; oversize and a rock breaker on the track grizzly; rotary car dumpers. The shaft's
  measuring pocket (L4) is the next user of the bottom-dump station.

## Later — L1: ventilation of the drive (2026-11-09)

Built while Tim was away on the draft spec [multiblocks/ventilation.md](multiblocks/ventilation.md), decisions in
[questions/l1-ventilation.md](questions/l1-ventilation.md). The `vent_fan` (75 kW) blows through `vent_duct` laid like pipe, with
`booster_fan`s (30 kW) for long drives; the TBM's reel gantry takes a `vent_duct_cassette` and pays duct out as it advances; the cab,
the junction's AIR AT BACK-UP and the `gas_detector` read the air (10.8 m³/s, 0.3 m/s, needed) and RADON in tuff and sandstone.
**The interlock (NO VENTILATION) and stale air's effects are world rules, on in new worlds and off in existing ones** (Tim,
2026-11-10; questions #1, #2), so existing drives run as before. NeoForge scenes `vent_fresh`, `vent_cassette`, `vent_stale_player`,
`vent_booster`, `vent_default_off`, `vent_parts`, `vent_new_world`; unit tests `AirflowTest`.
- **Left out, planned (the spec's "Left out"):** a network solver (Hardy Cross), return airways, regulators and doors; a shaft as the
  return airway (with L4, shaft-and-hoist.md, Later); heat from the rock at depth and refrigeration; real gases (methane, blasting
  fumes, diesel particulate) and the diesel rule (0.06 m³/s a kW) for when a diesel machine goes below; the battery bay's hydrogen
  (L2).
## Later — L4: the shaft and hoist (2026-11-09)

Built while Tim was away on the draft spec [multiblocks/shaft-and-hoist.md](multiblocks/shaft-and-hoist.md), decisions in
[questions/l4-shaft.md](questions/l4-shaft.md), branch `l4-shaft`. The `headframe` (9 x 30 x 9) stands over a 5 x 5 shaft; the
`hoist_house` (10 x 8 x 12, a 450 kW motor at the medium-voltage tier) stands behind it within 60 m and winds two 3 t `skip`s in
balance at up to 10 m/s between the `measuring_flask` at the shaft bottom and the dump in the headframe, into the 100 t `headframe_bin`
beside it, which feeds a belt. The L3 bottom-dump station's apron feeder feeds the flask. About 270-280 t/h from 150 m. Reads STOPPED,
WINDING, WAITING FOR ORE, BIN FULL, NOT BUILT; PLC FILL (the bin) and SKIP DUMPED. NeoForge scenes `shaft_hoist`, `shaft_not_built`,
`shaft_flask_feed`, `shaft_bin_full`, `shaft_power`; unit tests `HoistingTest`; the client film `shaft_hoist`.
- **Still to build, planned (the spec's "Still to build"):** L4b the raise borer (pilot and ream, cuttings into the drive); L4c the cage
  on the second drum (CAGE mode, riding, two landings); the shaft as the drive's return airway (with L1 ventilation); then the spec's
  "Left out": Koepe hoists, rope stretch and inspection, overwind, sinking by drill and blast, several levels, lowering the TBM down
  the shaft.

## Later — L5: paste backfill (2026-11-10)

Built while Tim was away on the draft spec [multiblocks/paste-backfill.md](multiblocks/paste-backfill.md), decisions in
[questions/l5-paste.md](questions/l5-paste.md), branch `l5-paste`. The `paste_filter` (75 kW) takes thickened tails to an 80 % cake; the
`paste_mixer` (75 kW) trims it with thickened tails and binds it with 5 % `cement` (a 50 kg bag, a placeholder recipe) to 78 % paste; the
`paste_pump` (110 kW, 35 m³/h at 8 MPa) pushes it down a steel fill line to a `fill_outlet` in any closed underground void (up to 20,000
m³, its openings closed with `fill_bulkhead`s), where it lands as wet `paste_fill`, lowest first, and sets in a game day. A broken
bulkhead lets the wet paste run out. Reads NO FEED, NO CEMENT, NOT ENCLOSED, FULL, LINE PRESSURE; PLC FLOW and PRESSURE on the pump.
NeoForge scenes `paste_fill_void`, `paste_not_enclosed`, `paste_cure`, `paste_bulkhead_fail`, `paste_no_cement`, `paste_plant`,
`paste_line_pressure`, `paste_pace`; unit tests `PasteFillTest`, `FillVoidTest`; the client film `paste`. Since Tim's answers
(2026-11-10) a placed block stands for 10 m³ of paste: a 360 m³ branch fills in about an hour.
- **Left out, planned (the spec's "Left out"):** uncemented hydraulic and rock fill; fill strength and exposing a fill wall; boreholes
  and their wear; paste rheology; a real cement kiln (cement is a crafted placeholder).
## Later — L6: the dry mill (2026-11-10)

Built while Tim was away on the draft spec [multiblocks/dry-mill.md](multiblocks/dry-mill.md), decisions in
[questions/l6-dry-mill.md](questions/l6-dry-mill.md), branch `l6-dry-mill`. A `scavenger_spiral_bank` on the spirals' tails takes
another 45 % of ilmenite, rutile, zircon, monazite and cassiterite to heavy mineral concentrate; the filter keeps it apart and passes
it to a gas-fired `fluid_bed_dryer` touching it (or bags it, `heavy_mineral_concentrate`, 100 kg with its assay); the dryer feeds the
`rer_separator` (ilmenite), the `ht_roll_separator` (rutile, cassiterite) and the `irm_separator` (monazite, and zircon), each paying
its minerals at the top rung's rate: 50 + 45 = 95 %, the 4x, by the real route. The leach's exception stays for a plant without them.
Existing plants pay as before. Since Tim's answers (2026-11-10) the world rule `dry_mill_all_hmc` (on in new worlds) sends the spiral
bank's share through the dry mill too, where a dryer touches the filter. NeoForge scenes `dry_mill_split`, `dry_mill_ladder`,
`dry_mill_all_hmc`, `dry_mill_wet`, `dry_mill_no_dryer`, `dry_mill_no_gas`, `dry_mill_parts`; unit tests `DryMillTest`; the client
film `dry_mill`.
- **Waits on Wildcatter:** its chlorinator taking Tailings' `zircon_concentrate` (Tim said yes, L6-3; questions #14). Wildcatter has
  no tag or API for it; what it needs is in questions/answers-2026-11-10.md, "Wildcatter needs".
- **Left out, planned (the spec's "Left out"):** electrostatic plate separators, attritioning and acid washing; leucoxene and altered
  ilmenite, the TiO2 grade; a titanium pigment plant (Wildcatter Ideas); monazite's dust and radioactivity; misplacement and middlings.

## Later — L7: dense-medium separation (2026-11-10)

Built while Tim was away on the draft spec [multiblocks/dense-medium-separation.md](multiblocks/dense-medium-separation.md), decisions
in [questions/l7-dms.md](questions/l7-dms.md), branch `l7-dms`. On the belt off the double-deck screen's gravel face (already built),
a `deslime_screen` washes the fines off the gravel into a `dms_cyclone_module`, which splits it by each rock's density against a
ferrosilicon medium (default SG 2.75, 2.6-3.3): the floats leave through a `drain_rinse_screen` as vanilla gravel, a block for 1.9 t,
their minerals with them; the sinks go on through a second one to the cone. A `medium_recovery` makes up the ferrosilicon lost, 0.3 kg
a tonne; `ferrosilicon` is a 25 kg bag with a placeholder recipe. 2 % each way goes wrong. Existing plants run as before. NeoForge
scenes `dms_split`, `dms_density`, `dms_medium`, `dms_gravel_face`, `dms_parts`, `dms_floats_belt`; unit tests `DenseMediumTest`;
the client films `dms` and `dms_floats_belt`. Since Tim's answers (2026-11-10) the floats can ride a belt off the floats screen's north
end, back to the plant, so the light ores can be recovered.
- **Waits on Wildcatter 0.7.0:** the medium made from Wildcatter's ferrosilicon bag (FeSi 75 and iron to FeSi 15; Tim, #8); the
  placeholder recipe stays until Wildcatter's Phase 17 is released.
- **Left out, planned (the spec's "Left out"):** a DMS drum for coarser feed; real partition curves and near-gravity material; coal
  washing and diamond recovery (X-2); medium viscosity and contamination by slimes; a starting charge of medium; a screen for the
  setting and a PLC setpoint.
## Later — L8: the X-ray ore sorter (2026-11-10)

Built while Tim was away on the draft spec [multiblocks/xrt-ore-sorter.md](multiblocks/xrt-ore-sorter.md), decisions in
[questions/l8-xrt.md](questions/l8-xrt.md), branch `l8-xrt`. A 1.2 m `xrt_sorter` (4 x 5 x 8, 20 kW) reads each piece's effective
atomic number from the real formulas of the minerals its rock holds and splits crushed rock and gravel two ways: over the threshold
(five steps, set by a sneaking click or the PLC's 4-20 mA THRESHOLD) to product, under it to waste, with 3 % and 5 % misplaced; 250
t/h on crushed rock. Its own 160 kW `sorter_compressor` gives the ejectors air; without it, NO AIR, and it stops. PRODUCT and WASTE pulse
a tonne each. The dump sorter is unchanged. NeoForge scenes `xrt_split`, `xrt_threshold`, `xrt_no_air`, `xrt_plc`, `xrt_refuses`; unit
tests `XrtSortingTest`; the client film `xrt`. Since Tim's answers (2026-11-10) a reverse mode keeps what reads under the threshold
(the sneaking click, or the PLC's REVERSE); scene `xrt_reverse`.
- **Not built:** the spec's sizing screen (questions/l8-xrt.md, 8).
- **Left out, planned (the spec's "Left out"):** other sensors (optical, laser, NIR, X-ray fluorescence and luminescence for diamonds);
  a grade spread inside one rock; dust extraction.

## Tim's answers (2026-11-10)

Built on branch `answers` from [questions/answers-2026-11-10.md](questions/answers-2026-11-10.md), each entry marked there:
- **World rules (#5, #23):** `OptInRules`, saved with the world and set by `/tailings rule`, replace the two config files:
  `ventilation_interlock`, `stale_air`, `headframe_hopper`, `paste_unloaded_cure`, `dry_mill_all_hmc` on in new worlds,
  `paste_wall_breach` off; every rule off in a world saved before them. Scenes `rules_new_world`, `rules_existing_world`,
  `rules_command`, `vent_new_world`; the scenes of an older world run in their own batch with the rules off.
- **Paste pace (#6):** a placed block stands for 10 m³; scene `paste_pace`.
- **DMS floats on a belt (#7):** scene `dms_floats_belt`, client film `dms_floats_belt`. The ferrosilicon placeholder stays until
  Wildcatter 0.7.0.
- **XRT reverse (#8):** scene `xrt_reverse`.
- **All HMC through the dry mill (#9):** scene `dry_mill_all_hmc`. The chlorinator compat waits on Wildcatter (see "Wildcatter needs").
- **gaps2 #2, #3 (#14):** the press's CYCLE is a one-second bit; the paste mixer's gauges read its paste hopper. Scenes
  `gaps_press_cycle_bit`, `gaps_press_points`, `gaps_paste_gauges`.
- **Ore blocks only (#3):** raw iron, copper and gold and every Wildcatter ore's mined drop are no plant feed; scene `raw_ore_refused`.

## Later, not scheduled

- Ventilation of the drive: built as L1 (above).
- Magnetic separation for ilmenite, and electrostatic separation for rutile and zircon: built as L6, the dry mill (above). Magnetite for vanilla iron is already the phase 1 separator.
- Dense-medium separation: built as L7 (above). X-ray ore sorting as its own machine. The dump sorter is a stand-in, not that machine.
- Dense-medium separation, and X-ray ore sorting as its own machine (X-ray sorting built as L8, above). The dump sorter is a stand-in, not that machine, and stays.
- A shaft and hoist (headframe, skip) instead of the decline: built as L4 (above); the raise borer and the cage are still to build.
- Battery locomotives charged from Wildcatter receptacles, for unpowered track: built as L2 (above).
- Bottom-dump cars over a grizzly: built as L3 (above).
- Open-pit and strip mining: scheduled, waves P1-P7 (above).
- Paste backfill of old stopes with tailings: built as L5 (above).
- PLCs: moved to Wildcatter's Phase 13 (docs/multiblocks/plc.md there). Tailings' machines, belts, sorter, TBM and track take its signals as wave 13i.

## Known gaps and stand-ins

- **Ferrosilicon is a crafted placeholder (L7):** 4 bags of 25 kg from an iron ingot, a sand and a metallurgical coke. Tim
  (2026-11-10): make it from Wildcatter's ferrosilicon bag (FeSi 75 and iron to FeSi 15) once that is released; that waits for
  Wildcatter 0.7.0 (Phase 17) (questions/l7-dms.md, 8).
- **The DMS needs no starting charge of medium (L7):** only the 0.3 kg a tonne lost is drawn (questions/l7-dms.md, 9).
- **The medium is set by clicks (L7):** no screen and no PLC setpoint; the PLC reads it (questions/l7-dms.md, 10).
- **Floats on a belt are rock again (L7):** with a belt off the floats screen they go back to the plant as gravel with their rock;
  a belt that runs them straight back to the DMS only floats them again; the player routes them past it, to the cone or the mill
  (questions/l7-dms.md, 11).
- **DMS sinks go round again (L7):** through the cone, the screen and the DMS, losing 2 % a pass (questions/l7-dms.md, 5).
- **A flat 2 % misplacement (L7):** no partition curve; a whole block floats or sinks as one (questions/l7-dms.md, 6).
- **No new L7 sounds:** the screens use the vibrating screen, the module the slurry rush, the recovery Wildcatter's motor hum.

- **Cement is a crafted placeholder (L5):** a 50 kg bag from one quicklime and a clay ball, until a cement kiln exists
  (questions/l5-paste.md, 8).
- **Paste is marked tails slurry (L5):** Wildcatter 0.5.1 has no product for paste; it carries `tailings:solids` and `tailings:binder`
  in its assay. A `PASTE` product is proposed for Wildcatter.
- **A paste spill happens in one tick (L5):** a broken bulkhead (or, with `paste_wall_breach` on, a breached wall) lets the wet paste out at once, not over several
  ticks (questions/l5-paste.md, 14).
- **Paste fills at a game pace (L5):** a placed block stands for 10 m³ of the line's paste, so a 360 m³ branch fills in about an hour
  of play, not the real 11; cement per block of void is a tenth of the real (questions/l5-paste.md, 19).
- **No new L5 sounds:** the filter and pump use Wildcatter's motor hum, the mixer the agitator; paste sounds as mud.
- **World rules (Tim, 2026-11-10):** `ventilation_interlock`, `stale_air`, `headframe_hopper`, `paste_unloaded_cure` and
  `dry_mill_all_hmc` are on in new worlds and off in worlds saved before them; `paste_wall_breach` is off in every world until set
  (`/tailings rule`). A world is new when its game time is 0 and it holds no Tailings data when its rules are made; a world created
  before this version and never ticked would count as new. The old `config/tailings-rules.properties` and
  `config/tailings-ventilation.properties` are read once, as a seed, when a world's rules are made, then never again.

- **The duct is drawn a metre round (L1):** the real 1.4 m does not fit a block; its friction is worked at 1.4 m.
- **A duct line is one line (L1):** at a branch the farther end gets the air; the rest is not counted.
- **No new L1 sounds:** the fans use Wildcatter's fan loop.

- **Charging takes 2.6 real hours (L2):** built at the real 18 kW into a 46 kWh battery; whether to scale it is a question for Tim
  (questions/l2-l3-trains.md, 12).
- **The charger takes 20 kW on either standard (L2):** on an IEC world that is 30.4 A on a 32 A receptacle, over the 80 % continuous
  the 480 V case keeps (questions/l2-l3-trains.md, 9).
- **The track grizzly keeps the pocket's oversize (L3):** the spec says TBM muck has none (questions/l2-l3-trains.md, 16).
- **Bottom-dump doors snap (L3):** they are drawn open or shut, not swinging; the load drops in one tick rather than over the grizzly's
  4 m.
- **No new L2/L3 sounds:** the battery locomotive uses the trolley locomotive's loop, the doors `muck_tip`; the charger is silent.
- **The battery locomotive has no PLC points of its own (L2):** it has no control cable as it moves; the spec puts its points on the
  charger, which has them (CHARGING, FULL, CHARGE) (questions/gaps2.md, 8).
- **No raise borer and no cage yet (L4):** the shaft is dug by hand; the hoist only checks the skips' compartments are clear
  (questions/l4-shaft.md, 1, 2). Planned in shaft-and-hoist.md, "Still to build".
- **The hoist motor draws its full 450 kW while it runs (L4):** Wildcatter 0.5.1's motor has no part load, so the real lift (346 kW,
  less between winds) is not modelled (questions/l4-shaft.md, 8).
- **A 3 t skip against a 1,555 t car (L4):** the hoist is right in tonnes but takes over five hours to lift one car's 576 items
  (questions/l4-shaft.md, 20).
- **Only the scavengers' share goes through the dry mill (L6):** the filter still pays the spiral bank's 50 %; a real plant sends all
  its HMC through (questions/l6-dry-mill.md, 3).
- **The dry mill is 2 t/h (L6):** on heavy mineral sand ore the scavengers make about 7.5 t/h; the filter bags what the dryer cannot
  take (questions/l6-dry-mill.md, 9).
- **Dry HMC moves by touching (L6):** filter to dryer to each separator, no elevator or chute; the separators pay into their own
  stacks, with no product bins (questions/l6-dry-mill.md, 6, 7).
- **Each separator takes all of its mineral (L6):** no misplacement or middlings.
- **No new L6 sounds:** the dryer uses Wildcatter's fan, the separators its motor hum.
- **Every piece of one rock reads the same (L8):** the X-ray sorter's 3 % and 5 % errors stand in for the real spread inside a rock
  (questions/l8-xrt.md, 4).
- **Light ores need the X-ray sorter in reverse (L8):** graphite, magnesite, borax, bauxite, spodumene and bentonite read under quartz,
  so at a normal step they go to waste; LOW in reverse keeps them. The platinum reef's platinum cannot be seen (questions/l8-xrt.md, 5).
- **The X-ray sorter's air is a running compressor, not a pressure (L8):** its own 160 kW compressor touching it, not Wildcatter's
  instrument air, which has no flow model (questions/l8-xrt.md, 6).
- **No sizing screen (L8):** the sorter takes the game's crushed and gravel classes as they come (questions/l8-xrt.md, 8).
- **No X-ray sorter sounds of its own (L8):** it plays the dump sorter's air jets; its compressor Wildcatter's motor hum.
- **No hoist sound of its own (L4):** the hoist house uses Wildcatter's motor hum; the skip dumps silently.

- **Dry tails do not ride belts (P4):** belts carry rock by kind and size, and dry tails are not a rock kind. The filter drops its
  cake into a load-out bin under it, a container under its chute, or onto open ground (questions/p4-dry-stack.md, 4).
- **Thickened tails are marked tails slurry (P4):** Wildcatter 0.5.1 has no product for them, so the underflow is tails slurry whose
  assay carries its solids. Wildcatter's lined cell counts it as plain tails. A `THICKENED_TAILS` product is proposed for Wildcatter.
- **No new P4 sounds:** the press, thickener, gate and dozer reuse existing sounds; the hydraulic whine, the cake's thud and the
  track clatter are for the art wave.
- **Placeholder art (P4):** `dry_tails_placeholder` (block) and `track_dozer_placeholder` (the dozer's icon) are drawn by script; the
  dozer's entity skin is painted in Ludo's materials; the four parts use Wildcatter's materials.
- **The dozer is drawn level and its treads do not run (P4):** its idlers and sprockets turn as it moves; the tread boxes are still.
- **A load-out needs room to loop (P4):** a truck leaves a load-out round a balloon loop needing about 40 m beyond the gate and 25 m
  to its right; it halts and says so if it cannot loop within 3 minutes.
- **Loads are tipped where the truck stops (P4):** a DUMP POINT stake is moved by the player as the crest advances; nothing moves it.
- **Art (2026-11-09 art wave):** no `*_placeholder` texture is left. The pit machines', locomotive's and car's skins are painted by
  their generators in Ludo materials (`art/source/vehicle/`, `tools/models/skin.py`); muck, the survey stakes, the hand-line machines,
  the reagents, the by-products, the survey book and the vehicle icons are Ludo art (`art/source/PROMPTS.md`). Still vanilla:
  the rock-size items (crushed, gravel, sand, fines), nuggets, minerals and concentrates share one vanilla icon per kind (cobblestone,
  gravel, sand, clay, iron nugget, raw iron, gold nugget) across every rock; they were never on this list and are a wave of their own
  (`docs/questions/art-wave.md`, 5).
- **A pit machine is drawn level (P1).** It climbs a step a block at a time and is not pitched on a ramp.
- **A pit machine's box is a box (P1):** its real length and width turned with it, so at a diagonal it stands wider than it is.
- **The loader works where it stands (P1):** it digs the face in front of its bucket and dumps into a truck within 4 m of the bucket,
  without driving between; the driver moves it. A self-running loader stands where it was left too.
- **A truck's route is only as good as its stakes (P2a):** it drives straight between stakes and turns round in the open at the
  stake before an end (about 25 m). It does not find its own way round anything the survey missed between samples.
- **The dispatch office's model is a first pass (P2b):** a site office drawn in Wildcatter's materials (no new textures); its radio
  mast is short, kept inside its 3 x 3 x 3 cells.
- **A parked truck waits on the road (P2b):** it parks in its lane at the PARK stake, not off to the side in a parking bay, so park
  a route's trucks on a stake clear of the road or on a spur.
- **One office per route (P2b):** a route on two offices answers to the first whose radio reaches the truck. Trucks are not moved
  between routes by dispatch: a truck's route is still set with the survey book.
- **TONNES is the last hour of game time (P2b):** 72,000 ticks; it needs an hour's running to settle, and its loads are kept only
  while the office is loaded (it keeps its own chunk while it has routes).
- **Turning round on a straight road leaves a truck off the line (P2a, found in P2b):** after turning on full lock at the stake
  before an end it is some 15 m to one side and backs in at an angle. A truck whose tail passes the end within 4 m of the line now
  stops there (before, it circled behind the end for ever); it may tip up to a few metres past a DUMP POINT stake.

- **The old `dewatering_pump` is a legacy block (R6c):** no recipe, out of the creative tab, doing nothing. Kept until the release after the first one that ships it (no release yet; docs/questions/tbm-gaps.md 8).
- **The running machine collides as one box (R6b),** not the parts' exact shapes, and it does not turn on the grade: the box is its outline, raised at the rear when pitched. Left with a plan (docs/questions/tbm-gaps.md 7): the game collides an entity by one box, and the bore is the machine's own 8 x 8 section, so it differs from the parts only beside the main beam and in the corners on the grade.
- **The loading point reaches a train's length behind the conveyor (TBM gaps),** 29 rows, so every car loads; nothing is drawn over it (a real back-up is longer than the train, or has a shuttle belt).
- **The reel gantry's transformer is built at the cable junction (TBM gaps),** a real part of the circuit, but at the cable's fixed end: the trailing cable's own impedance is not modelled.
- **The machine's body follows the course's smooth floor, the gantries their rails (TBM gaps).** On the grade the two can differ by up to half a block at the joint behind the track layer; to be checked in the films.
- **Standing on the moving machine waits (decided with Tim, R6).** Riding is a seat in the cab.
- **The headlight's light is a vanilla light block.** The model lamp is full bright. The entity also places `minecraft:light` at level 15 on the block above the rail, and removes it when the train leaves. That block is what actually lights the tunnel.
- **The hand-line machines are plain cubes (2026-11-09 art wave).** Stamp mill, hand screen, shaking table and magnetic separator have their own Ludo textures now, but are still one textured cube each, not models; the crushing, flotation and dump parts are real models since R3.
- **The old `tailings_pond_placeholder` is a legacy part (4d):** no recipe, out of the creative tab; an old plant's pond goes on working. Wildcatter's lined cell takes tails now. A few scenes still end their line in it, as it can read the tails' assay and the tower does not keep one. Kept, with the other legacy parts, until a release has shipped them as legacy (docs/questions/tbm-gaps.md 8).
- **Still staged in the playthrough (R10, 4d, 13i):** the PLC's control deck, its 24 V steam set and the control cable to each machine, placed rather than crafted (not in the bill), digging the pit, the gas and diesel in the generators, the acid in its tank, the hole and the bucket of water under the pump, the raw materials, the forced chunks, and peaceful difficulty (a creeper in the pit killed the player during the placing in one run). The plant's water is real now, from the process pump and the cell's decant.
- **The scavengers' concentrate has its own thickener and filter in the playthrough (4d),** only as that was the shorter layout; a pipe to the main thickener works too.
- **Concentrate is mineral only (R4b; kept, plant-gaps decision 10).** The bank's concentrate carries the recovered mineral at 30 % solids; the gangue in a real concentrate, and the thickener's overflow water, are left out. Since the assay is in kg (plant-gaps) its mass is the real mineral's.
- **Fines items have no use now (R4b; kept, plant-gaps decision 11).** The plant makes fines slurry; the `fines_*` items stay registered for old worlds.
- **A train waits over the pocket where no signal guards the dump (plant-gaps).** With a block signal whose section holds the dump it waits at the signal.
- **The rock breaker is a pickaxe (plant-gaps).** Oversize on the pocket's grizzly is broken by a click with a pickaxe, or lifted off by hand; no powered breaker yet. The share, one item in a hundred, is proposed.
- **A blocked belt stops at once (plant-gaps).** A run whose head end cannot deliver stops where its rock is; there is no spill or chute switch to reset.
- **The sorter screen is drawn by code (plant-gaps):** flat panels, no texture, a "W" button a side.
- **Holds (R4a, proposed):** a dry part holds one 2.7 t block; a slurry part about 10 s of its flow; a belt can carry three times the plant rate; the grizzly passes 30 %, the screen 15 % of crushed and 40 % of gravel; the screen's gravel face takes 35 % of crushed (plant-gaps); an ingot counts 100 kg of metal in the assay (plant-gaps).
- **Old items (R3).** `flotation_cell` and `dump_station` are kept for one release as items that craft back into iron. No release yet, so still kept (docs/questions/tbm-gaps.md 8).
- **One node a metre of track (R5b).** Each rail is a node on Wildcatter's circuit, and Wildcatter solves at most 4,000 nodes in one circuit (grid included): a very long drive needs its own rectifier further in, on track not joined to the first. Past the limit the track is dead and says so: the rectifier's readout, a stopped train's status and a chat line to players near it ("TRACK TOO LONG"). A line split by unloaded chunks loses its far end until they load.
- **The client films ran before the world rules (answers):** the Fabric client scenes make a new world, so the rules are on in them;
  only `dms_floats_belt` was filmed since. A film whose TBM runs over a minute with no fan would now stop NO VENTILATION.
- **Train damage is a config file, not a gamerule.** `config/tailings-train.properties`. Missing file means damage is on and the amount is 8.
- **Couplings never break (R5a).** Decided with Tim: a loaded 10-car train would need about 215 kN on the 1-in-2 decline. Breaking couplings and cut cars rolling back wait.
- **Sound levels are not tuned by ear (R8).** The volumes in `sound.md` were set from the files' levels. The tests prove each sound starts, not how it sounds together. The films have no audio: `screencapture` can only record a microphone.
- **Silent for now (R8).** The traction rectifier, the small phase 1 machines, the reel gantry, and the wheels of an unpowered car. A plant part's sound does not follow its load.
- **What the playthrough still stages:** digging the pit, the gas and diesel in the generators, the mill's and cyclones' water, and handing over raw materials and reagents; the gensets and unit substations are set by helper, not by hand. Its chunks are force-loaded: the plant stops if its chunks unload (no ticket on plant parts). The individual film scenes still force-run their parts.
- **The playthrough's waste bin backs up in about 90 minutes of driving (R10).** 992 items in 11 game minutes against 8,192. The ways out are built (DESIGN.md, "Dump and signals"): a belt under the bin to a `heap_pad` (100 blocks, leached at a tenth of the plant's rate) or to hoppers and chests at its head end, which take the rock as items. The playthrough runs 11.5 minutes and builds neither; a real waste dump waits for open pit P4.
- **Selective flotation (4e) is not in the playthrough:** proved by unit tests, NeoForge scenes and a force-run film only. Its figures, 300 g/t of zinc sulfate above all, are proposed, and its recipes are placeholders.
- **Phase 4 parts' recipes are placeholders (4a-4c),** as the rest of the plant's: eight iron around a copper ingot.

Other stand-ins worth knowing:

- Powered-machine recipes are eight iron and one copper, for every plant part. The ramp, pocket and bin are iron around a hopper. The belt is leather over iron.
- Slope, stair, landing, and mine floor are not crafted. The drill cuts them from mined stone.
- The eight-slice ramp is a cuboid stand-in for a smooth mesh. Vanilla JSON models cannot be an arbitrary wedge.
- The dump's "sensor sorter" is a slot map, not an X-ray sorter.

## Decisions already made

1. Plain mining is 25% of the block. Tailings does not change Wildcatter's drop. Decided 2026-11-02.
2. The played steps are even 2x / 3x / 4x. 4x pays 95%, a written exception. Decided 2026-11-02.
3. Vanilla iron, copper, and gold are on the same ladder. One plain smelt is 1 ingot. Decided 2026-11-02.
4. The drill program is a GUI, with rail, gravel, cobble, segments, and a water bucket slot. Decided 2026-11-02.
5. Haulage is Tailings mine rail and mine cars, not vanilla minecarts. Decided 2026-11-02.
6. Chunk loading is a ticket on the drill and on each train. Decided 2026-11-03; since R1 (2026-11-05) it is one `tailings:runner` ticket that times out on its own.
7. Packaging is its own jar that requires Wildcatter. Decided 2026-11-02.
8. Only ore blocks feed the plant (Silk Touch, the TBM, blasting); raw ore items and mined drops get no plant value. Fortune and Silk
   Touch stay as vanilla has them. Diamonds stay vanilla. Decided 2026-11-10 (answers #3, #4).
9. A rule that changes how an existing world plays is a per-world rule, on in new worlds and off in existing ones, changeable by
   command. Decided 2026-11-10 (answers #23).

Still open, from the specs: which extra vanilla ores (redstone, lapis) join the ladder.
