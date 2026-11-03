# Changelog

All notable changes are the merged pull requests on `main`. The mod version is still 0.1.0. Dates are the merge dates in the repository history (2026-11-02 and 2026-11-03).

## [Unreleased]

Nothing on `main` after pull request 9.

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
