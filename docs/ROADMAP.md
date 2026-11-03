# Tailings — Roadmap

Exactly one phase is marked **CURRENT**. Each phase is done when `./gradlew runClientGameTest` passes with new coverage for the
phase, PROGRESS.md is updated, and the Field Manual has a page for each new system.

## Phase 0 — Set up — CURRENT
- README, VISION.md, ROADMAP.md and the first specs (this draft). Tim settles the open questions below.
- A multi-loader project like Wildcatter's (common, fabric, neoforge): its own jars, each with a hard dependency on Wildcatter 0.2.0+.
- A plain-Java `recovery` package: the mineral mass in a block, each stage's recovery, and tests that no route returns more
  than the block holds.

## Phase 1 — Rock and the ladder of sizes
- 1a: crushed products for each rock type: crushed rock → gravel → sand → fines, each with its mineral list (granite, diorite,
  andesite, deepslate, tuff, sandstone, cobblestone, and the 27 Wildcatter ores).
- 1b: a hand-fed starter jaw crusher and a screen; the 1x baseline (a plain drop is 25 % of the block's mineral) in the
  recovery package; ore nuggets from ordinary rock and how they combine.
- 1c: gravity concentration (spiral, jig, shaking table) for cassiterite, heavy mineral sand, scheelite, chromite, silver,
  and vanilla gold; magnetic separation for vanilla iron (magnetite) (2x).
- 1d: vanilla ores: iron, copper and gold (ore blocks and raw ore items) on the same 1x/2x/3x/4x ladder; one plain smelt is
  1 ingot, the full line 4 ingots. Others that fit (for example redstone, lapis) are judged ore by ore.

## Phase 2 — The concentrator
- 2a: the crushing circuit at true size (`multiblocks/crushing-circuit.md`).
- 2b: the ball mill and cyclones, grinding to liberation.
- 2c: the flotation cell bank (`multiblocks/flotation-bank.md`) with reagents from Wildcatter's chemicals (3x).
- 2d: thickeners and filters; concentrate in bags or drums.

## Phase 3 — The mine
- 3a: the track-laying TBM (`multiblocks/tunnel-drill-haulage.md`): an 8 x 8 cut, a 6 x 6 drive inside segment lining that keeps liquids
  out; the floor pattern slope | slope + track | stair walkway | stair walkway | slope + track | slope (stairs with a landing every other block); 1-in-2 half slopes by default; the
  dewatering pump that clears water into its tank (voided in code) and quenches lava; the GUI program (target Y, then flat) and its inventory of mine rail,
  gravel, cobble, lining segments and water.
- 3b: twin-track haulage on Tailings' own mine rail: 1 block gauge; custom Granby side-dump mine cars (about 3 t, mixed loads), a mine locomotive
  drawing from the powered mine rail (a shrouded, segmented conductor rail, safe to walk on; no overhead wire) fed from a
  480 V AC to 250 V DC traction rectifier on the Wildcatter grid, a rack for the 1-in-2 decline, trains of up to 10 cars, a dump station that sorts each type into its ore bin, coupled trains, loading and dump stations,
  block signals. Custom cars, not vanilla minecarts (decided with Tim, 2026-11-02).
- 3c: ventilation *(proposed)*.

## Phase 4 — Full recovery
- 4a: scavenger and cleaner flotation, regrind.
- 4b: leaching of flotation tails (tank leach with Wildcatter's sulfuric acid; heap leach on a lined pad).
- 4c: by-products: cadmium and indium from sphalerite; silver from galena; rhenium from molybdenite roasting; cobalt from
  pentlandite; monazite rare earths from heavy mineral sand (4x).
- 4d: the lined tailings cell, shared with Wildcatter's uranium mill (Wildcatter ROADMAP idea).

## Ideas (not scheduled)
- Magnetic separation for ilmenite, and electrostatic separation (rutile, zircon). (Magnetite for vanilla iron is in 1c.)
- Dense-medium separation, ore sorting by X-ray.
- A shaft and hoist (headframe, skip) as the late alternative to the decline.
- Battery locomotives charged from Wildcatter receptacles, for unpowered track.
- Bottom-dump cars over a grizzly.
- Open-pit mining with haul trucks.
- Paste backfill of old stopes with tailings.
- In-game PLCs, modeled on real ones (a Siemens S7 and others). The player programs them in the world, in the real forms:
  ladder logic, function blocks, and I/O modules. Uses: the TBM's program, the dump sorter, train block signals, and plant
  pumps. Wildcatter's controls are fixed hardware, not a program the player writes: the genset control panel (governor and
  synchronizer), MCC starters with overload relays, and the substation's 50/51 and under-frequency relays (Wildcatter
  `docs/multiblocks/genset.md`, `motor-control-center.md`, `substation.md`). Its "controller" is the block that owns a
  multiblock, not a PLC. Those stay as they are. If both mods grow a programmable controller, the scan (ladder and function
  blocks, no Minecraft imports) can be a plain-Java library in Wildcatter, the way its solver is, and Tailings would call it
  for the mine and the plant.

## Open questions (for Tim)
1. ~~1x baseline~~: **decided 2026-11-02:** plain mining counts as 25 % of the block's mineral; processing the drops fully
   reaches 4x (VISION.md). **Tailings changes nothing in Wildcatter** (confirmed 2026-11-02): its drop stays the same and
   Tailings reads one drop as 25 %.
2. **Do Fortune and Silk Touch stay?** Fortune is a kind of multiplication. Keep it, cap it, or replace it with the plant.
3. ~~Even or real steps~~: **decided 2026-11-02:** even 2x/3x/4x; 4x is a written exception (VISION.md).
4. **Which rock types** get mineral lists, and from what source (crustal abundance × 10)?
5. ~~Vanilla ores~~: **decided 2026-11-02:** yes. One vanilla ore smelted plainly is 1 ingot (1x); the full line gives 4
   ingots (4x), on the same ladder (VISION.md).
6. ~~Drill program~~: **decided 2026-11-02:** a GUI, with an inventory of mine rail, gravel and cobble.
7. ~~Carts~~: **decided 2026-11-02:** Tailings' own mine cars on mine rail, not vanilla minecarts (`multiblocks/tunnel-drill-haulage.md`).
8. **Chunk loading** for a drill that runs far from the player.
9. ~~Packaging~~: **decided 2026-11-02:** its own jar that requires Wildcatter (VISION.md).
