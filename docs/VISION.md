# Tailings — Vision

## What it is
A Wildcatter add-on about **getting ore out of the ground and the metal out of the ore**, as real mines and mills do. Wildcatter
already has the 27 real ores (Wildcatter `docs/ores.md`), the grid, the steel pipe and the chemistry. Tailings adds:

tunnel drill → twin-track haulage → ore bin → crushing → grinding → classification → concentration → leaching →
concentrate to the smelter or plant → tailings to a lined cell

## How this fits Wildcatter's "no ore multiplication" rule
Wildcatter's VISION forbids ore multiplication ("no Mekanism clones"). Tailings keeps that rule. **It never makes mineral that was
not in the rock.** Every ore block already holds a fixed mass of mineral at its game grade (ores.md: about ten times a real
mined grade, capped at 80 %). The 2x/3x/4x is how much of that mass each route recovers, with real recovery figures:

| Route | What is lost | Recovery of the contained mineral | Multiple of plain mining |
|---|---|---|---|
| Plain mining (break the block, smelt or hand to a plant) | The fines, the mineral locked in rock lumps, and what hand-cobbing rejects | **25 %**, decided (hand-cobbing and direct smelting of lump ore historically recovered roughly 20-40 %) | 1x |
| Crushing and gravity or magnetic concentration | Slimes under about 20 µm and mineral still locked in middlings | **about 50 %** (gravity circuits 40-70 %; Wills' *Mineral Processing Technology*) | **2x** |
| Grinding to liberation and froth flotation | Unliberated and oxidised mineral in the flotation tails | **about 75 %** in the game *(real sulfide flotation 85-92 %; scaled so the ladder is even)* | **3x** |
| Flotation plus leaching the tails, regrind and scavenger cells, by-product recovery | Only what the real best plant loses | **about 95 %** (best real base-metal plants 92-97 %) | **4x** (3.8x real, a written exception) |

So the multiples are **recovery ratios**, not duplication. The game grade, not Tailings, sets the mass in the rock. The baseline
is **decided (Tim, 2026-11-02): plain mining counts as 25 % of the block's mineral.** The drop of a plain-mined Wildcatter ore
block stands for that 25 %. **Tailings changes nothing in Wildcatter** (confirmed with Tim, 2026-11-02): the drop stays
exactly as Wildcatter makes it, and Tailings reads one drop as 25 %. The other 75 % is still in the drop, locked in rock; processing the drops fully, through every
Tailings stage, is how a block reaches 4x (up to 95 % of its mineral, rounded to 4x as below).

**The 4x is a written exception (decided with Tim, 2026-11-02),** like Wildcatter's centrifuge cascade (1000x separative work)
and its small reactor core. Real figures on a 25 % baseline give about 2x, 3.4x and 3.8x. Tailings rounds the steps to an even
2x, 3x and 4x for play. Every step still comes from a real recovery stage, and no route ever returns more mineral than the
block holds at its ores.md grade.

Also allowed by Wildcatter's rules:
- **Common rock carries real trace minerals, recovered as nuggets** (decided with Tim, 2026-11-02). Granite, for example,
  really holds a little zircon, monazite and cassiterite. Processing it in a plant gives **ore nuggets** of those minerals at
  real crustal abundances times the same tenfold game factor (`multiblocks/crushing-circuit.md`, Nuggets). Nothing comes from
  nowhere: a block of granite is 2,700 kg of rock with a written mineral list.
- **Every machine is distinct real parts.** No "Tier 1–5 macerator". A bigger plant is a bigger crusher or more flotation cells
  in a real bank. (A flotation bank of identical cells is a written exception, like Wildcatter's centrifuge cascade, because a
  real bank is exactly that.)

## Vanilla ores (decided with Tim, 2026-11-02)
Tailings also processes vanilla ores: iron, copper and gold, and any others whose mineral fits a real recovery route (see
ROADMAP). **One vanilla ore smelted plainly gives 1 ingot, the 1x. Run through the full machine line it gives 4 ingots, the
4x**, by the same ladder: gravity or magnetic concentration 2 ingots, flotation 3, full recovery with leaching 4. The
ingot stands in for 25 % of the ore's metal, as a Wildcatter drop does for its mineral. Raw ore items (raw iron, raw copper,
raw gold) and the ore blocks both enter the line.

## Pillars (in priority order)
1. **The mine-to-mill chain.** Every feature serves rock out of the ground and mineral out of the rock.
2. **Realism.** Real machines, real power draw, real recoveries, real throughput, scaled only where play needs it, and every
   scaling is written down.
3. **The mine is gameplay.** Laying out a decline, a haulage loop and a plant is a core activity, not a chore.
4. **Consequence.** Carts collide, a mill overloads its breaker, tailings must go somewhere.

## Non-goals
- No ore multiplication beyond the mineral in the rock (above).
- No abstract energy. Everything draws watts from Wildcatter's grid.
- No magic conveyors. Rock moves by cart, chute, belt or slurry pipe.
- No metal refining of its own where Wildcatter plans the plant (aluminium, nickel, ferroalloys): Tailings delivers the
  concentrate to it.

## Progression
1. **Hand mine, hand crusher.** Cobble and gravel; the first sands; gravity concentrates (2x).
2. **The mill.** Jaw and cone crushers, screens, a ball mill; flotation (3x).
3. **The mine.** The track-laying drill and twin-track haulage.
4. **Full recovery.** Leaching of tails, scavenger and cleaner banks, by-products (4x), the tailings cell.

## Packaging (decided with Tim, 2026-11-02)
Tailings is **its own jar** (Fabric and NeoForge, like Wildcatter) and **requires Wildcatter**: it declares a hard dependency
(`depends` in `fabric.mod.json`, a required `[[dependencies]]` in `neoforge.mods.toml`), and the game will not load without it.

## Multiblock rules
Wildcatter's apply: distinct parts, one controller, a spec in `docs/multiblocks/<name>.md` before any code, true size
(1 block = 1 m), capacity by a bigger structure.
