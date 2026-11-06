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

## Realignment with Wildcatter (R) — CURRENT

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
- **R5. Haulage, per haulage.md.** Decided with Tim on 2026-11-05: the rectifier is a Tailings DC source on Wildcatter's DC solver;
  two waves; couplings never break in R5; trains saved on rails become entities.
  - **R5a. Trains as entities — DONE (2026-11-05).** Every vehicle is an entity and trains glide along the rails, through switches
    and up and down the decline, at speeds from power over force; placing, coupling, run/hold/reverse, signals, the dump, the
    drill's loading point, derailing, harm, chunk tickets; the ratings in `haul.Haul`.
  - **R5b. The rectifier on Wildcatter's DC solver.** A 250 V source, the rails as the conductor, locomotives as DC loads.
- **R6. The contraption TBM**, per tunnel-boring-machine.md.
- **R7. Testing and art:** Fabric client game tests with screenshots for every spec's game test, then the in-game art look and
  its fixes.

**Open DECISIONs from the R2 specs (listed, not chosen):**
- A plant-scale gravity circuit (spirals, jigs) for the 2x route (crushing-circuit.md).
- Selective lead-then-zinc circuits (flotation-bank.md).
- The traction rectifier's DC source: a voltage-regulated Wildcatter rectifier, or a Tailings source on Wildcatter's DC solver
  (haulage.md).
- Carried from the first draft: the 1-in-1 steep option, leaking broken lining (tunnel-boring-machine.md). Ballast on the decline:
  decided with Tim (2026-11-05), yes if it can be done, in R6.

## Phase 4 — Full recovery — next after R

Not started.

- **4a.** Scavenger and cleaner flotation beyond the fixed bank, and a regrind.
- **4b.** Leaching the flotation tails: tank leach with Wildcatter's sulfuric acid, and heap leach on a lined pad.
- **4c.** By-products, the 95% route: cadmium and indium from sphalerite, silver from galena, rhenium from molybdenite, cobalt from pentlandite, monazite rare earths from heavy mineral sand. The played multiple is 4x. The mass paid is 95% of what the block holds, not 100%. 95/25 is 3.8. The even 4x is the written exception in [VISION.md](VISION.md).
- **4d.** The lined tailings cell, shared with the Wildcatter uranium-mill idea. What the mill rejects has to go somewhere.

## Later, not scheduled

- Ventilation of the drive.
- Magnetic separation for ilmenite, and electrostatic separation for rutile and zircon. Magnetite for vanilla iron is already the phase 1 separator.
- Dense-medium separation, and X-ray ore sorting as its own machine. The dump sorter is a stand-in, not that machine.
- A shaft and hoist (headframe, skip) instead of the decline.
- Battery locomotives charged from Wildcatter receptacles, for unpowered track.
- Bottom-dump cars over a grizzly.
- Open-pit mining with haul trucks.
- Paste backfill of old stopes with tailings.
- In-game PLCs, modeled on a Siemens S7 and others: ladder logic, function blocks, and I/O modules, for the drill program, the dump sorter, train signals, and plant pumps. Wildcatter's controls stay fixed hardware (genset panel, MCC starters, substation relays). If both mods grow a programmable controller, the scan can live in Wildcatter as plain Java, the way its solver does, and Tailings would call it.

## Known gaps and stand-ins

These are in the current build on purpose, or because the art and tests have not caught up. They are not phase 4 work unless a later change says so.

- **No drill entity model.** `tunnel_drill` the entity is a `NoopRenderer`, and since R1 nothing spawns it. The machine you see is the moved blocks, which use `tunnel_drill.png`.
- **The headlight's light is a vanilla light block.** The model lamp is full bright. The entity also places `minecraft:light` at level 15 on the block above the rail, and removes it when the train leaves. That block is what actually lights the tunnel.
- **The mine light item icon is the old sprite.** Placed `mine_light` uses `mine_light_on` and `mine_light_off`. The item model still points at `textures/block/mine_light.png`.
- **The switch bed is still gravel.** Flat `mine_rail` uses `ballast` under the cutout overlay. `mine_switch` and `mine_switch_thrown` still use `minecraft:block/gravel` for the cube under the overlay.
- **The hand-line machines still use vanilla block textures.** Stamp mill, hand screen, shaking table, magnetic separator. The crushing, flotation and dump parts are real models since R3.
- **The tailings pond is a placeholder (R4b).** `tailings_pond_placeholder` holds 3,400 t of tails, then the bank stops. Its model is plain berms and its recipe is 8 clay. Phase 4d's lined cell replaces it.
- **Concentrate is mineral only (R4b).** The bank's concentrate carries the recovered mineral at 30 % solids; the gangue in a real concentrate, and the thickener's overflow water, are left out.
- **Fines items have no use now (R4b).** The plant makes fines slurry; the `fines_*` items stay registered for old worlds.
- **The sorter's screen is clicks, not a screen (R4c).** A side is set by clicking it with a rock item; the waste side by a sneaking empty-hand click.
- **The train waits over the pocket (R4c),** not at the signal before the ramp.
- **No oversize on the pocket's grizzly (R4c).** Every rock passes its bars.
- **Belts deliver at once (R4a).** A run's load reaches its head end without a transit time; the lumps are drawn moving.
- **Feed faces are not checked (R4a).** A belt's head end feeds whatever part it touches, and a part takes rock through any face, not only the spec's feed face. Parts touching an output face take the product straight in, as a chute.
- **No gravel face yet (R4a).** The screen's 6-40 mm gravel always goes round to the cone; the aggregate face in crushing-circuit.md is not built.
- **Ingots in the assay (R4a).** The vanilla metals are counted in ingots, so an ingot's worth of metal counts as 1 kg in the slurry assay.
- **Holds (R4a, proposed):** a dry part holds one 2.7 t block; a slurry part about 10 s of its flow; a belt can carry three times the plant rate; the grizzly passes 30 %, the screen 15 % of crushed and 40 % of gravel.
- **Old items (R3).** `flotation_cell` and `dump_station` are kept for one release as items that craft back into iron.
- **R3 models were not seen in a live client.** They were checked as previews (`build/model-previews/`) and by ModelLint. Screenshots come with R7.
- **Trains still run on the rectifier's on/off feed until R5b.** Full power on any fed rail; no voltage drop yet.
- **Train damage is a config file, not a gamerule.** `config/tailings-train.properties`. Missing file means damage is on and the amount is 8.
- **Couplings never break (R5a).** Decided with Tim: a loaded 10-car train would need about 215 kN on the 1-in-2 decline. Breaking couplings and cut cars rolling back wait.
- **Vehicles at the models' length (R5a).** Cars sit 1.25 m apart, the models' 20 px, not the real 2 m car and 3 m locomotive, until the art pass (R7).
- **Trains were not seen in a live client (R5a).** The game tests run the server; how the gliding looks waits for R7's screenshots.
- **The drill entity is not spawned (R1).** Until the contraption wave (R6), the drill is its blocks and its chunks are held by the `tailings:runner` ticket.
- **Entity and block models were not checked in a live client.** Unit tests check the 20-pixel spacing. A game test bakes the entity layers and checks that the headlight and load parts exist. There is no headed-client pass.

Other stand-ins worth knowing:

- Powered-machine recipes are eight iron and one copper, for every plant part. The ramp, pocket and bin are iron around a hopper. The belt is leather over iron.
- `mine_switch` and `mine_light` have no crafting recipe.
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

Still open, from the specs: whether Fortune and Silk Touch stay; which extra vanilla ores (redstone, lapis) join the ladder; whether the 1-in-1 steep grade stays in the GUI; whether broken lining leaks; whether gravel ballast belongs on the decline as well as the flat.
