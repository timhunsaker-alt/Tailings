# P7c: pit-scale flotation and tails (tank cells, column cleaners, the large thickeners)

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Wave **P7c**, **built 2026-11-10** on branch
`p7c-pit-flotation` (ROADMAP, P7c; what this spec left open is decided in `../questions/p7-pit-line.md` #33-#44). The choices are listed in
[../questions/specs-tailings.md](../questions/specs-tailings.md) (P7 section).

The pit-sized line's last stage. The SAG line ([sag-mill-line.md](sag-mill-line.md)) makes **1,000 t/h** of fines slurry, twenty times
what one `flotation_bank` (flotation-bank.md) takes. Twenty banks side by side would be tiling; real plants this size use a **row of
large tank cells**. Tim's decision (a) named the gyratory, the stockpile and the SAG line; flotation and the tails at this rate follow
from it, so they are specced here as the third part (Decision P7-1).

**Real counterparts:**
- A rougher-scavenger row of **160 m³ forced-air tank cells**, as at mid-size copper concentrators.
- **Column flotation cells** as cleaners.
- Large high-rate thickeners for the concentrate and the tails; a tailings pipeline to a storage facility.
- Wills & Finch, *Wills' Mineral Processing Technology*, 8th ed.: flotation machines and circuits.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

```
 fines slurry 1,000 t/h -> CONDITIONING TANK (large) -> TANK CELL x 8 (rougher 1-5, scavenger 6-8) -> tails
                                                             | concentrate                              |
                                                             v                                          v
                                                   COLUMN CELL x 2 (cleaners) -> concentrate   LARGE TAILINGS THICKENER
                                                             | cleaner tails back to cell 1         | water back; underflow
                                                             v                                      v
                                        CONCENTRATE THICKENER (existing) -> PRESSURE FILTER   Wildcatter's lined cell
```

## Size
- **Slurry:** 1,000 t/h of solids at 35 %: **2,230 m³/h** (370 m³/h of solids and 1,860 of water).
- **The row:** 8 cells x 160 m³ x 85 % live volume *(proposed)* is 1,090 m³: **29 minutes** of residence, inside a real
  rougher-scavenger's 20-35 min.
- **Recovery:** the same ladder as the bank: **75 %** of every mineral that floats (`RecoveryRoute.FLOTATION`), paid by the existing
  filter's nugget rule. No new rung. The minerals that do not float go to the tails: **a pit line has no gravity stage** (below).

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `conditioning_tank_large` | 8 x 9 x 8 | An **8 m** agitated tank, about 6 minutes at 2,230 m³/h *(proposed)*, **90 kW**; reagents by hopper as the small tank, dosed per tonne as the small plant *(the same g/t)* |
| `tank_cell` | 7 x 7 x 7 | One **160 m³ forced-air tank cell**, about **6.5 m** across *(unverified)*, its own **160 kW** mechanism *(unverified)*. Each cell is one real machine, placed in a row joined by pipe: feed in at its north face, tails out at its south face to the next, concentrate out of its east launder |
| `column_cell` | 4 x 14 x 4 | A **4 m x 14 m** column cell, cleaner, with its sparger and wash water *(proposed)*; air from the blower; **15 kW** for its tails pump *(proposed)* |
| `flotation_blower_large` | 4 x 4 x 6 | A multistage centrifugal blower for the row's air, about **400 kW** at the world's standard medium voltage *(proposed)* |
| `tailings_thickener_large` | 45 x 6 x 45 | A **45 m** high-rate thickener *(proposed: 24,000 t/day at about 0.07 m² a t/day)*, **55 kW** rake drive. Tails in; underflow at **60 %** solids out on pipe; overflow water back on a water pipe |

The concentrate goes to the existing `concentrate_thickener` and `pressure_filter` (flotation-bank.md) *(proposed: the concentrate is
a few per cent of the feed, so the small ones at their size take it; the filter pays the nuggets)*. Two or more of each if a rich ore
needs it.

## How it runs
- **The row works as one flow:** each cell takes its feed, floats its share of what floats, and passes the rest on. The row's 75 % is
  split over its eight cells, most in the first *(proposed: 35, 15, 8, 5, 4 % of what the block holds in the roughers; 3, 3, 2 in the
  scavengers)*. A row cut short pays only what its cells float. A ninth cell adds nothing.
- **The column cleaners** take the row's concentrate and give it back as cleaned concentrate; their tails go back to cell 1 *(the
  game pays at the filter, so the cleaners are a real step without a rung of their own, as the bank's cleaners are)*.
- **Air:** each cell and column needs the blower's air; NO AIR stops it.
- **The tails** go to the large thickener, its underflow by pipe to **Wildcatter's lined tailings cell** (tailings-cell.md), its water
  back to the plant. At 24,000 t a day a cell needs about **11,000 blocks of volume a day** (2.2 t a block): the pit line's tails need a
  very large cell, or several, as real ones do.
- **No dry stacking for the pit line** *(Decision P7-4)*: at 1,000 t/h it would take **17** of P4's 60 t/h filters and a fleet to haul
  the cake. Dry stacking stays the 50 t/h plant's.
- **No gravity stage:** at this rate the spirals would be 20 banks. A real copper concentrator does not recover zircon or ilmenite
  from its feed either *(Decision P7-3)*. A player who wants them puts a bank of spirals on a split stream.
- **Reads:** NO FEED, NO AIR, NO REAGENT, NOT BUILT, "its output has nowhere to go".
- **PLC points:** each cell's RUN, RUNNING, AIR (4-20 mA); the blower's RUN, RUNNING, AMPS; the thickener's TORQUE (4-20 mA, its rake).

## Left out (to ROADMAP)
- Froth level control and a real kinetic model per cell.
- Regrind of the rougher concentrate (the small plant's 4a has one).
- Pit-scale leach, by-products and selective flotation (the small plant's 4b-4e): the pit line pays the 3x rung.
- The tailings pipeline's pumping stations.

## Game tests to add
- `p7_tank_row`: copper fines slurry through eight cells: 75 % of the copper to the filter, split 35/15/8/5/4/3/3/2.
- `p7_row_short`: a row of four cells pays 63 %.
- `p7_no_air`: the blower off stops the row.
- `p7_tails`: the large thickener sends its underflow to a lined cell and its water back.

## The wave plan
- **P7c**, after P7b. With P7a-c built the pit line runs from truck to concentrate.

## Sources
- Wills & Finch (as flotation-bank.md): tank cells to 300 m³ and over, column cleaners, residence times.
- Cell mechanism powers, the cells' sizes and the thickener's unit area are *(unverified)* or *(proposed)* as marked.
