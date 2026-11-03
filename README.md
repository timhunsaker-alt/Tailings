# Tailings

A Minecraft Java 26.3 add-on (Java 25) for **Wildcatter**, on **Fabric and NeoForge**: real mines and real mills. You drive a
tunnel with a track-laying drill, haul run-of-mine rock out on twin-track haulage in custom mine cars, and put it through a concentrator:
crushing, grinding, screening, gravity, magnetic and flotation separation, leaching and by-product recovery. A block of
Wildcatter ore gives up to **four times** what plain mining and smelting give, because each stage recovers mineral that the
simpler route throws away, as in a real mill. Every machine is a true-size multiblock of real parts, powered from the Wildcatter
grid at real voltages. What it is and why it is allowed is in `docs/VISION.md`. What comes next is in `docs/ROADMAP.md`. Each
machine's spec is in `docs/multiblocks/`.

## What's in it (planned; nothing is built yet)
- **Rock crushing by rock type:** cobblestone, granite, diorite, andesite, deepslate, tuff, sandstone and Wildcatter's ore
  blocks each crush to their own product (crushed rock → gravel → sand → fines), and each product carries the minerals its host
  rock really holds.
- **The crushing circuit:** a jaw crusher, a cone crusher, vibrating screens and a ball mill with cyclones
  (`docs/multiblocks/crushing-circuit.md`).
- **Concentration:** a flotation cell bank for the sulfides (galena, sphalerite, pentlandite, molybdenite, cobaltite,
  acanthite, platinum reef) (`docs/multiblocks/flotation-bank.md`); gravity (spirals, jigs, shaking tables) and magnetic
  separation later.
- **Leaching and by-products:** heap and tank leach of flotation tailings; cadmium and indium from sphalerite; silver from
  galena; rhenium from molybdenite; cobalt from pentlandite (ROADMAP).
- **The mine:** a programmable track-laying tunnel drill that bores a decline to a set Y level and then drives flat, and
  a lined 6 x 6 bore kept dry by its pump, 1-in-2 slopes under two tracks and a walkway between them, and trains of up to
  10 custom mine cars on powered track that is safe to walk on, carrying mixed loads that a dump station sorts into ore bins
  (`docs/multiblocks/tunnel-drill-haulage.md`).
- **Tailings:** what the mill rejects goes to a lined tailings cell, as in the Wildcatter ROADMAP idea.

## Needs
- Tailings is **its own jar** and **requires Wildcatter** 0.2.0 or newer on the same loader and Minecraft version; it will not
  load without it. It has no grid, ores or pipe of its own; it uses Wildcatter's.

## The code (planned)
One codebase, two loaders, laid out as Wildcatter is: `common/` (the recovery math, plants, blocks, items, the drill's
program), `fabric/` and `neoforge/` (entry points). The recovery math is plain Java with no Minecraft imports and its own unit
tests.

## Testing (planned)
`./gradlew build`, `./gradlew :common:test`, and `./gradlew :fabric:runClientGameTest` with one scene per machine, as in Wildcatter.
