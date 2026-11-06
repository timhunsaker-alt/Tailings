# The dump station and ore sorting (APPROVED by Tim 2026-11-05, R2)

Where trains tip their mixed loads. A **dump pocket under a tipping ramp** takes the rock, and an **apron feeder** draws it out to a
**sensor-based ore sorter**. The sorter sends each rock and ore type to its own **ore bin**, and each bin feeds a belt to the
crushing circuit. The trains are in [haulage.md](haulage.md), the plant in [crushing-circuit.md](crushing-circuit.md).

**Real counterparts:**
- A concrete dump pocket over a 300 mm grizzly, under a curved Granby dump ramp.
- An apron feeder.
- Sensor-based ore sorting: X-ray transmission and colour sorters that reject waste rock at mine sites.
- Steel surge bins.

**Decided with Tim:**
- **2026-11-02:** cars carry mixed loads, and sorting happens at the dump.
- **2026-11-03:** the pocket holds one 10-car train, and the feeder clears a stack a second.
- **2026-11-05:**
  - the station is **real parts**, each a true-size big part;
  - the plant runs at the real 50 t/h.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* came from the
first draft and is still to be checked.

```
 train on the dump ramp -> tips each car on the move -> DUMP POCKET (300 mm grizzly) -> APRON FEEDER -> ORE SORTER
                                                                                                        | one face a product
                                                                       ORE BIN (waste) <- ORE BIN (copper ore) <- ...
                                                                            |                |
                                                                       belt to backfill   belt to the crushing circuit
```

## Size
- **Rate:** 50 t/h, matching the crushing circuit.
- **The pocket:** holds one 10-car train (decided 2026-11-03).
- **The feeder:** in game items, it clears one stack (64 items) a second (decided 2026-11-03).
- **Decided with Tim, 2026-11-05 (R4):** the pocket buffers a whole train, and when it is full the next car does not tip and the
  train waits. A 10-car train of 30 t takes about 36 minutes to clear at 50 t/h.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `dump_ramp` | 1 x 1 x 4 | A curved Granby dump ramp beside the track over the pocket *(proposed part)*. It lifts each car's dump wheel as the car passes, so the car tips sideways into the pocket without the train stopping |
| `dump_pocket` | 4 x 3 x 3 | A concrete pocket with a 300 mm grizzly, set under the track and the ramp. Oversize stays on the grizzly; a player clears it by hand *(proposed)* |
| `apron_feeder` | 2 x 1 x 4 | An apron feeder from the pocket's bottom to the sorter: **11 kW at 480 V** |
| `ore_sorter` | 3 x 3 x 3 | A sensor-based sorter: **40 kW at 480 V** *(unverified)*. It sends each rock and ore type to the face its screen sets: north, east, south or west (one is the feed side), plus "waste" |
| `ore_bin` | 3 x 6 x 3 | A steel surge bin, one product a bin, set in its screen. It holds **8,192 items** (decided as the game size; the real bin is about 50 m³). It feeds a belt or a hopper from under its bottom cells |

## Layout (a sketch, facing north; fixed with the models in R3)
Plan, track level (y 3):
```
 x ->  0   1   2   3   4   5
 z 0   =   =   =   =   =   =      = mine rail (loaded track runs west to east over the pocket)
   1   R   R   R   R   .   .      R dump_ramp beside the track
```
Below the track (y 0 to 2), and the sorter and bins beside the pocket:
```
 x ->  0   1   2   3   4   5   6   7   8   9
 z 0   P   P   P   P   .   .   .   .   .   .      P dump_pocket (4 x 3 x 3, y 0-2)
   1   P   P   P   P   .   .   .   .   .   .      A apron_feeder (from the pocket's south side)
   2   P   P   P   P   .   .   .   .   .   .      O ore_sorter
   3   .   A   A   .   .   .   .   .   .   .      B ore_bin (one a product, each against a sorter face)
   4   .   A   A   .   .   .   .   .   .   .
   5   .   A   A   .   .   .   .   .   .   .
   6   .   A   A   O   O   O   B   B   B   .
   7   .   .   .   O   O   O   B   B   B   .
   8   .   .   .   O   O   O   B   B   B   .
   9   .   .   .   B   B   B   .   .   .   .
  10   .   .   .   B   B   B   .   .   .   .
  11   .   .   .   B   B   B   .   .   .   .
```

## Nozzles and faces (each part drawn facing north; main cell at the north-west bottom corner)
- **Motor boxes:** on the west face of the feeder's and the sorter's main cells, at **480 V**.
- **The pocket:**
  - takes what a tipping car drops from above;
  - feeds the apron feeder touching its south side.
- **The apron feeder:** discharges onto the sorter its tail end touches.
- **The ore sorter:** each output face passes its product to the ore bin it touches; "waste" goes to the bin set as waste.
- **The ore bin:** out of its bottom to a belt or a hopper under it.

## How it runs
- **Tipping:** a car passing over the ramp tips its whole load into the pocket while the pocket has room. When the pocket is full,
  the next car does not tip, and the train waits at the signal before the ramp.
- **The feeder:** draws rock to the sorter while its motor runs and the sorter has room.
- **The sorter:**
  - it runs while its motor runs;
  - it sends each type by its setting;
  - a type with no bin set goes to the waste bin;
  - when the bin for a type is full, it stops that type. When every bin it needs is full, it stops: FULL.
- **The bins** feed their belts as the plant takes rock.
- **What each part reads when it stops:**
  - NO FEED;
  - NOT BUILT (a part it must touch is missing);
  - FULL;
  - "its output has nowhere to go".
- **Removing a part** stops the flow through it. What it holds stays until it is broken, then drops (R1).
- **Rock types stay separate.** The crushing circuit treats each rock type differently (granite sand is a zircon feed, cobble sand
  a trace sulfide feed), so a mixed bin gives a mixed product.

## Added while building (R4c)
- **The sorter's screen:** right-click a side of the sorter with a rock or ore item to set that side to that type; sneak-right-click
  a side with an empty hand to set it as the waste side. The Power Meter or an empty-hand click reads the settings.
- **Where the train waits:** with the car over the full pocket, not at the signal before the ramp.
- **The sorter** passes up to a stack a second, as the feeder does, and holds two stacks on its belt.

## Left out (ROADMAP)
- A real X-ray ore sorter's grade cut, which splits ore from waste within one rock type. This sorter sorts by type only.
- Bottom-dump cars over a grizzly.
- Dense-medium separation.
- Oversize staying on the pocket's 300 mm grizzly for a player to clear (proposed in the parts table): every rock passes the bars.

## Game test
`dumpStation` (Fabric client game test):
- **Setup:**
  - a short loaded track over the ramp and pocket;
  - a locomotive and two cars loaded by a hook with cobble and copper ore;
  - the feeder, sorter and two bins (copper ore; waste) at 480 V.
- **Checks:**
  1. Both cars tip on the move.
  2. The copper ore reaches its bin and the cobble the waste bin.
  3. With the copper bin full, copper waits in the pocket and the sorter reads FULL for it.
- **Screenshot:** the station.
