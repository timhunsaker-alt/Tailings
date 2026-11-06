# Changelog

All notable changes are the merged pull requests on `main`. The mod version is still 0.1.0. Dates are the merge dates in the repository history (2026-11-02 and 2026-11-03).

## [Unreleased]

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
