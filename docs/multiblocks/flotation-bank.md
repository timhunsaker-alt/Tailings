# The flotation bank (APPROVED by Tim 2026-11-05, R2)

A **rougher-scavenger-cleaner flotation circuit of mechanical cells**, the 3x route for the sulfide ores and for vanilla copper
and gold. The fines slurry comes from the crushing circuit's cyclone overflow on pipe. It leaves as:
- a filtered concentrate, as items;
- a final tail on pipe.

**Decided with Tim:**
- **2026-11-02:** the 3x step pays 75 % of the mineral (VISION.md).
- **2026-11-05 (R2):**
  - the slurry travels on Wildcatter's steel pipe with its assay;
  - the real 50 t/h;
  - the eight cells are **one `flotation_bank` part** holding them in its model, as Wildcatter's coke battery holds its 13 ovens.
    This replaces the old written exception for a bank of identical cells.

**Sources:**
- Wills & Finch (2016), chapter 12 (froth flotation).
- Outotec TankCell and Denver DR cell data sheets.
- Fuerstenau, Jameson & Yoon, *Froth Flotation: A Century of Innovation* (2007).

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal.

```
fines slurry -> CONDITIONING TANK -> conditioned slurry -> FLOTATION BANK ---------------------------> final tail (pipe)
                 ^ xanthate, MIBC,                         | inside it: 4 roughers -> rougher conc -> 2 cleaners
                   lime, copper sulfate                    |   rougher tail -> 2 scavengers; scavenger conc and cleaner
                   (hopper)                                |   tail back to the rougher feed
                                     AIR BLOWER (touching) ^
                                                           v concentrate slurry (pipe)
                                              CONCENTRATE THICKENER -> thickened concentrate (pipe) -> PRESSURE FILTER -> items
```

## Size: 50 t/h of solids
- **Feed:** 13.9 kg/s of solids at 35 % solids, about 31 L/s (111 m³/h) of slurry, from the crushing circuit.
- **Residence:** about 12 min in the roughers in a real plant. Here each part holds a few seconds of its rated flow and the rates
  are real, as every process part in Wildcatter does.
- **Concentrate mass:** set by each mineral's recovery and grade (below). For copper ore at the ores.md grade that is a few
  percent of the feed.

## Reagents (real doses, by hopper on the conditioning tank)
One item is 1 kg. The doses are per tonne of feed (2.7 t a block):

| Reagent | Dose | Per block | Notes |
|---|---|---|---|
| Potassium amyl xanthate (collector) | 50 g/t | 0.135 kg | every feed |
| MIBC (frother) | 20 g/t | 0.054 kg | every feed |
| Quicklime (pH 9-10, to depress pyrite) | 500 g/t | 1.35 kg | calcite burned in a furnace; Wildcatter has no lime |
| Copper sulfate (activator) | 300 g/t | 0.81 kg | only for feed that holds sphalerite |

The tank holds one item for each kg, or part of one, still to dose (R1).

## Recovery to concentrate
These are real figures; the game multiplies each by the crushing circuit's 90 % liberation at fines:

| Mineral | Real recovery | Concentrate grade |
|---|---|---|
| Galena | 90 % | 70 % Pb |
| Sphalerite | 88 % | 52 % Zn |
| Pentlandite | 85 % | 12 % Ni |
| Molybdenite | 85 % | 52 % Mo |
| Cobaltite, acanthite | 80 % | — |
| Platinum reef | 85 % | 100-200 g/t PGM |
| Vanilla copper (chalcopyrite) | 88 % | 28 % Cu |
| Vanilla gold (free gold reports to gravity first; the rest floats) | 85 % | — |

- **The 3x step:** at 90 % liberation and about 85 % flotation, about 77 % of the mineral in the rock reaches concentrate. The
  game pays the even **75 %**, the 3x step on the 25 % baseline (decided, VISION.md). A vanilla copper or gold ore gives 3 ingots
  instead of 1.
- **The bank's size is fixed:** recovery does not grow with a second bank. A second bank in parallel doubles capacity, not
  recovery. Phase 4a's regrind and extra cleaning, plus tails leaching, raise it toward 95 % (4x).

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `conditioning_tank` | 3 x 4 x 3 | A 3 m agitated conditioning tank, about 4 min at 111 m³/h: **15 kW at 480 V**. Fines slurry in, reagents by hopper on top, conditioned slurry out |
| `flotation_bank` | 7 x 4 x 13 *(proposed)* | **Eight 20 m³ forced-air tank cells**, each about 3.3 m across (TankCell e20 class), in two rows. The east row holds four roughers; the west row holds two scavengers and two cleaners. Their launders, pumps and level controls are in the one part.<br>• Agitators **8 x 45 kW = 360 kW at 480 V**, on one motor box.<br>• Conditioned slurry in; concentrate slurry and final tail out on pipe.<br>• Needs air from a blower it touches. |
| `air_blower` | 2 x 2 x 3 | A multistage centrifugal blower, **75 kW at 480 V**, about 1 m³/s at 50 kPa *(proposed)*, for up to eight cells. It touches the bank |
| `concentrate_thickener` | 8 x 3 x 8 | An 8 m high-rate thickener, **5.5 kW rake at 480 V**. Concentrate slurry in; thickened concentrate at 60 % solids out *(proposed)*. The overflow water is left out (below) |
| `pressure_filter` | 3 x 3 x 5 | A plate-and-frame pressure filter to 8 % moisture: **15 kW at 480 V**. Thickened concentrate in on pipe; the cake goes out as items to a hopper under it, or to an empty hand |
| `tailings_pond_placeholder` | 6 x 2 x 6 | *Legacy since Phase 4d: Wildcatter's lined cell takes the tails.* An earthen tailings pond with its berms (decided with Tim, 2026-11-05, R4). Tails slurry in on pipe at any side; it holds about a day of the plant's tails, **3,400 t** *(proposed)*. No motor. A placeholder until Phase 4d's lined cell |

**The items the filter pays** are the same ones the ladder pays today (DESIGN.md):
- the ingot, where the concentrate is one that smelts (copper, gold, galena to lead, acanthite to silver);
- otherwise the Wildcatter mineral item.

Nuggets of trace sulfides from ordinary rock come out here too (crushing-circuit.md, Nuggets).

**New pipe products (in the Wildcatter proposal), all in kg and each carrying its assay:**
- `conditioned slurry`;
- `concentrate slurry`;
- `thickened concentrate`;
- `tails slurry`.

## Layout (a sketch, facing north; fixed with the models in R3)
Plan, bottom layer (y 0):
```
 x ->  0         5         10        15
 z 0   K K K ~ ~ ~ ~ . . . . . . . . . .     K conditioning_tank       ~ steel pipe
   1   K K K . . . ~ . . . . . . . . . .     B flotation_bank (7 x 4 x 13: cells drawn inside its model)
   2   K K K . . . ~ . . . . . . . . . .     A air_blower (touching the bank's west side)
   3   . . . . B B B B B B B . . . . . .     T concentrate_thickener
   4   . . A A B B B B B B B . . . . . .     F pressure_filter
   5   . . A A B B B B B B B . . . . . .
  ...          (the bank runs 13 cells south, to z 15)
  15   . . . . B B B B B B B ~ ~ T T T T T T T T
  16   . . . . . . . ~ . . . . . T T T T T T T T     concentrate slurry from the bank's south face to the thickener
  17   . . . . . . . ~ tails   . T T T T T T T T
  ...                            (the thickener is 8 x 8)
  23   . . . . . . . . . . . . . . . ~ F F F . .     thickened concentrate to the filter
```

## Nozzles and faces (each part drawn facing north; main cell at the north-west bottom corner)
- **Motor boxes:** on the west face of every part's main cell, all at **480 V**.
- **Conditioning tank:**
  - fines slurry in at the north face of its front cells;
  - conditioned slurry out at the south face of its back cells;
  - reagents by a hopper on any top cell.
- **Flotation bank:**
  - conditioned slurry in at the north face of its front cells;
  - concentrate slurry out of the south face of its back cells at x 6;
  - tails slurry out of the south face of its back cells at x 0.
  - The blower must touch its west side.
- **Thickener:** concentrate slurry in at the north face of its front cells; thickened concentrate out of the south face of its
  back cells.
- **Pressure filter:**
  - thickened concentrate in at the north face of its front cells;
  - cake out to a hopper under any bottom cell.

## How it runs
- **The conditioning tank** conditions while it has fines slurry, its outlet has room, and its reagents cover the feed's dose
  (copper sulfate only when the assay holds sphalerite). Short of a reagent, it stops: NO REAGENT (names it).
- **The flotation bank:**
  - it floats while its agitators run, the blower it touches runs, and both its outlets have room;
  - the concentrate slurry carries the recovered mineral (the 75 % ladder), and the tails slurry carries the rest of the feed.
- **The thickener** passes concentrate slurry to the filter as thickened concentrate.
- **The pressure filter** bags the cake into items each time an item's worth of mineral is in it, and holds two stacks at most.
- **The final tail:**
  - the tails slurry goes to whatever its pipe joins;
  - until Phase 4d's lined tailings cell, it runs to a `tailings_pond_placeholder` (decided with Tim, 2026-11-05);
  - with nowhere to go, the bank stops: "its output has nowhere to go".
- **What each part reads when it stops:**
  - NO FEED;
  - NO REAGENT;
  - NO AIR;
  - NOT BUILT;
  - FULL: take the concentrate;
  - "its output has nowhere to go".
- **Removing a part** stops the flow through it. What it holds stays until it is broken, then drops.

## Left out (ROADMAP)
- Copper-then-moly circuits. Selective lead-then-zinc is built: see selective-flotation.md.
- Regrind and extra cleaning (Phase 4a); leaching the tails (4b); by-products (4c); the lined tailings cell (4d).
- Thickener overflow water recycled to the plant; froth chemistry beyond the four reagents; pyrite as a by-product.

Decided with Tim on 2026-11-05 (R4): the final tails go to a `tailings_pond_placeholder` until Phase 4d.

## Added while building (R4b)
- **Inlet buffers:** each part draws its feed off the pipe into a few seconds' buffer, so it knows the feed's assay (for the copper
  sulfate dose, and the bank's split) before it works it.
- **The concentrate** is the recovered mineral at 30 % solids *(proposed)*; the gangue that real concentrate carries is left out.
- **The filter** pays a nugget for each nugget's worth of a mineral and packs each nine of a mineral's nuggets into the item the
  ladder pays (a copper ingot, a lead ingot, ...), so an ore block still pays exactly 3.

## Game test
`flotationBank` (Fabric client game test):
- **Power:** a unit substation at 480 V.
- **Feed:**
  - copper fines slurry by a hook into a test-filled stock tank on the tank's inlet;
  - the four reagents by hopper.
- **Checks:**
  1. The bank floats once the blower runs.
  2. With the blower's breaker open, it stops: NO AIR.
  3. The filter drops copper ingots into its hopper, 3 for each block of copper ore's worth of fines (the 3x step).
  4. The tails pipe carries the rest of the assay.
  5. Without copper sulfate, a sphalerite feed stops: NO REAGENT (Copper Sulfate).
- **Screenshot:** the bank.
