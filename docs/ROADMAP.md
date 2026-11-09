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
- **Floor and bed.** Mined rock becomes the floor pieces. Gravel ballast is only on the flat. Cobble fills caves under the floor. The flat-rail model now uses the ballast texture under the rail overlay. The switch bed is still the gravel texture (see gaps).
- **Lining.** The default ring is cobble or any stone the drill has just mined or is holding (stone, cobble, deepslate, granite, diorite, andesite, tuff, sandstone). Crafted segments (4 gray concrete + 1 iron ingot = 4) are optional. They are used only when a full ring of 28 is in the segment slot.
- **Quench water.** The tank holds 256 m³. Pumped water fills it first. Excess is voided. A water bucket can still top the tank up. Lava is quenched from that tank, then cut as rock.
- **Trains.** Up to 10 cars. Each car holds 9 stacks (576 items), so one 64-item drill face fits in a car. The real Granby figure in the spec stays about 3 t. That number is not the inventory size. The dump pocket holds one 10-car train. The apron clears one stack (64 items) each second.
- **Power on the rail.** A fed conductor section is live along its whole length and is still safe to walk: no shock. Mine lights and signal lamps draw from a neighboring fed rail. The locomotive has a headlight. A thrown `mine_switch` sends a train clockwise, and the same signal check still applies.
- **Trains hurt players and mobs.** Default damage is 8. It is `config/tailings-train.properties` (`damage=true` or `damage=false`, and `amount=`), not a gamerule. Only the locomotive is an entity. Its hitbox covers the consist. Cars are data on the rail blocks, drawn by the locomotive's renderer.

Pull request 3, between the phases, is docs only: an unscheduled PLC idea. See Ideas below.

Ventilation (old 3c) was only a proposal. It is not in the game.

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

Next: **P2b**, the dispatch office on the PLC (RUN, PARK, LOADS, TONNES, FUEL LOW, BLOCKED). Then P3 (the ammonia plant
in Wildcatter, the drill, the emulsion truck, the blast), P4 (dry tails and dumps, the dozer), P5 (the big shovels and the ultra
truck), P6 (the dragline), P7 (the pit-sized plant).

## Plant gaps (2026-11-09, closed while Tim was away)

Tim (2026-11-09): "close all known gaps". Closed on the ore plant and dump, each with a NeoForge scene; decisions in
[questions/plant-gaps.md](questions/plant-gaps.md): belts' transit time at 2 m/s (`belt_transit`); feed faces checked and the old layouts
moved to them (`feed_face`, `crushing_circuit`, `playthrough_crushing`); the screen's gravel face and belts into hoppers or chests
(`screen_gravel`); oversize on the pocket's grizzly (`pocket_oversize`); the train waits at the signal before the dump (`dump_signal`);
lead and zinc concentrate mixed make a bulk cake (`bulk_concentrate`); the ore sorter's screen (`sorter_screen`); metal counted in kg in
the assay. Kept as they are: concentrate as mineral only, and the old `fines_*` items. The waste bin's way out is written, not built
into the playthrough.

## Later, not scheduled

- Ventilation of the drive.
- Magnetic separation for ilmenite, and electrostatic separation for rutile and zircon. Magnetite for vanilla iron is already the phase 1 separator.
- Dense-medium separation, and X-ray ore sorting as its own machine. The dump sorter is a stand-in, not that machine.
- A shaft and hoist (headframe, skip) instead of the decline.
- Battery locomotives charged from Wildcatter receptacles, for unpowered track.
- Bottom-dump cars over a grizzly.
- Open-pit and strip mining: scheduled, waves P1-P7 (above).
- Paste backfill of old stopes with tailings.
- PLCs: moved to Wildcatter's Phase 13 (docs/multiblocks/plc.md there). Tailings' machines, belts, sorter, TBM and track take its signals as wave 13i.

## Known gaps and stand-ins

- **The open pit's art is placeholder (P1):** `haul_truck_placeholder` and `wheel_loader_placeholder` skins, their item icons, and
  `muck_placeholder` are drawn by script.
- **A pit machine is drawn level (P1).** It climbs a step a block at a time and is not pitched on a ramp.
- **A pit machine's box is a box (P1):** its real length and width turned with it, so at a diagonal it stands wider than it is.
- **The loader works where it stands (P1):** it digs the face in front of its bucket and dumps into a truck within 4 m of the bucket,
  without driving between; the driver moves it. A self-running loader stands where it was left too.
- **The open pit's P2a art is placeholder:** `survey_stake_*_placeholder` (the post and its flag colours) and
  `survey_book_placeholder` are drawn by script.
- **A truck's route is only as good as its stakes (P2a):** it drives straight between stakes and turns round in the open at the
  stake before an end (about 25 m). It does not find its own way round anything the survey missed between samples.
- **The fuel bay's pipe intake has no scene yet (P2a):** `pit_fuel_bay` fills its day tank by hand; filling by pipe uses Wildcatter's
  `Lines.pull` as the genset does.
- **PARK stakes do nothing until P2b** (the dispatch office sends trucks to them).
- **The sorter's routing is not on a signal (13i).** A PLC can shut a side's gate, sending it to waste, but not choose which rock a side takes: that is set on its screen.
- **The TBM's program is not on a signal (13i).** GO starts it; its course is set on its cab's screen.
- **A slurry part's level, flow and density have no reading yet (13i).** FILL reads what a dry part or belt holds.

- **Gantries keep their place by the machine's course (R6c),** set along the straight bore behind it, not run along the rail by their own wheels. The machine checks the track under them before each advance and stops if it is gone.
- **Only the first four cars load on a visit (R6c, R9).** A long train leaves with its far cars empty.
- **The trailing cable is not drawn (R6c),** and the reel does not turn. The cab counts what is paid out.
- **The transformer is a rating, not a circuit part (R6c).** The pump and conveyor add to what the junction draws at 4.16 kV.
- **The quench tank stays with the cab (R6c).** Its water is kept when a pump gantry is broken.
- **The old `dewatering_pump` is a legacy block (R6c):** no recipe, out of the creative tab, doing nothing.
- **The running machine collides as one box (R6b),** not the parts' exact shapes, and it does not turn on the grade: the box is its outline, raised at the rear when pitched.
- **The cab's screen closes when the machine lifts (R6b).** Open it again on the running machine.
- **TBM recipes are placeholders (R6a).** Eight iron around a copper ingot, as the plant's.
- **Standing on the moving machine waits (decided with Tim, R6).** Riding is a seat in the cab.
- **The headlight's light is a vanilla light block.** The model lamp is full bright. The entity also places `minecraft:light` at level 15 on the block above the rail, and removes it when the train leaves. That block is what actually lights the tunnel.
- **The mine light item icon is the old sprite.** Placed `mine_light` uses `mine_light_on` and `mine_light_off`. The item model still points at `textures/block/mine_light.png`.
- **The switch bed is still gravel.** Flat `mine_rail` uses `ballast` under the cutout overlay. `mine_switch` and `mine_switch_thrown` still use `minecraft:block/gravel` for the cube under the overlay.
- **The hand-line machines still use vanilla block textures.** Stamp mill, hand screen, shaking table, magnetic separator. The crushing, flotation and dump parts are real models since R3.
- **The old `tailings_pond_placeholder` is a legacy part (4d):** no recipe, out of the creative tab; an old plant's pond goes on working. Wildcatter's lined cell takes tails now. A few scenes still end their line in it, as it can read the tails' assay and the tower does not keep one.
- **Still staged in the playthrough (R10, 4d, 13i):** the PLC's control deck, its 24 V steam set and the control cable to each machine, placed rather than crafted (not in the bill), digging the pit, the gas and diesel in the generators, the acid in its tank, the hole and the bucket of water under the pump, the raw materials, the forced chunks, and peaceful difficulty (a creeper in the pit killed the player during the placing in one run). The plant's water is real now, from the process pump and the cell's decant.
- **The scavengers' concentrate has its own thickener and filter in the playthrough (4d),** only as that was the shorter layout; a pipe to the main thickener works too.
- **Concentrate is mineral only (R4b; kept, plant-gaps decision 10).** The bank's concentrate carries the recovered mineral at 30 % solids; the gangue in a real concentrate, and the thickener's overflow water, are left out. Since the assay is in kg (plant-gaps) its mass is the real mineral's.
- **Fines items have no use now (R4b; kept, plant-gaps decision 11).** The plant makes fines slurry; the `fines_*` items stay registered for old worlds.
- **A train waits over the pocket where no signal guards the dump (plant-gaps).** With a block signal whose section holds the dump it waits at the signal.
- **The rock breaker is a pickaxe (plant-gaps).** Oversize on the pocket's grizzly is broken by a click with a pickaxe, or lifted off by hand; no powered breaker yet. The share, one item in a hundred, is proposed.
- **A blocked belt stops at once (plant-gaps).** A run whose head end cannot deliver stops where its rock is; there is no spill or chute switch to reset.
- **The sorter screen is drawn by code (plant-gaps):** flat panels, no texture, a "W" button a side.
- **Holds (R4a, proposed):** a dry part holds one 2.7 t block; a slurry part about 10 s of its flow; a belt can carry three times the plant rate; the grizzly passes 30 %, the screen 15 % of crushed and 40 % of gravel; the screen's gravel face takes 35 % of crushed (plant-gaps); an ingot counts 100 kg of metal in the assay (plant-gaps).
- **Old items (R3).** `flotation_cell` and `dump_station` are kept for one release as items that craft back into iron.
- **One node a metre of track (R5b).** Each rail is a node on Wildcatter's circuit, and Wildcatter solves at most 4,000 nodes in one circuit (grid included): a very long drive needs its own rectifier further in. A line split by unloaded chunks loses its far end until they load.
- **Train damage is a config file, not a gamerule.** `config/tailings-train.properties`. Missing file means damage is on and the amount is 8.
- **Couplings never break (R5a).** Decided with Tim: a loaded 10-car train would need about 215 kN on the 1-in-2 decline. Breaking couplings and cut cars rolling back wait.
- **Sound levels are not tuned by ear (R8).** The volumes in `sound.md` were set from the files' levels. The tests prove each sound starts, not how it sounds together. The films have no audio: `screencapture` can only record a microphone.
- **Silent for now (R8).** The traction rectifier, the small phase 1 machines, the reel gantry, and the wheels of an unpowered car. A plant part's sound does not follow its load.
- **The locomotive and car skins are script-drawn placeholders (R9):** `mine_locomotive_placeholder`, `mine_car_placeholder`, from `tools/models/vehicles.py`, until they come from Ludo.
- **What the playthrough still stages:** digging the pit, the gas and diesel in the generators, the mill's and cyclones' water, and handing over raw materials and reagents; the gensets and unit substations are set by helper, not by hand. Its chunks are force-loaded: the plant stops if its chunks unload (no ticket on plant parts). The individual film scenes still force-run their parts.
- **A busway run straight onto the rectifier's motor box keeps its generator's breaker open (R10).** Sized cable works, and one train needs only about 110 A. Probably the solver failure fixed in Wildcatter 0.4.1, from the same symptom; not re-tested with the fix.
- **The playthrough's waste bin backs up in about 90 minutes of driving (R10).** 992 items in 11 game minutes against 8,192. The ways out are built (DESIGN.md, "Dump and signals"): a belt under the bin to a `heap_pad` (100 blocks, leached at a tenth of the plant's rate) or to hoppers and chests at its head end, which take the rock as items. The playthrough runs 11.5 minutes and builds neither; a real waste dump waits for open pit P4.
- **Selective flotation (4e) is not in the playthrough:** proved by unit tests, NeoForge scenes and a force-run film only. Its figures, 300 g/t of zinc sulfate above all, are proposed, and its recipes are placeholders.
- **The `zinc_sulfate` item borrows vanilla sugar's icon (4e),** as the other reagents borrow vanilla icons.
- **Phase 4 parts' recipes are placeholders (4a-4c),** as the rest of the plant's: eight iron around a copper ingot. **By-product item icons are script-drawn placeholders.**

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

Still open, from the specs: whether Fortune and Silk Touch stay; which extra vanilla ores (redstone, lapis) join the ladder.
