# Paste backfill of old workings with tailings

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed wave **L5**. The choices are listed
in [../questions/specs-tailings.md](../questions/specs-tailings.md) (Later section).

ROADMAP "Later": paste backfill of old stopes with tailings. Real underground mines pump about half their tails back underground as
**cemented paste fill**: thickened and filtered tails mixed with **3-7 % binder** to a paste of **70-85 % solids**, pumped through
steel pipe into mined-out stopes behind **bulkheads**, where it sets to a weak rock of about **0.5-1 MPa** at 28 days. It is the fourth
place tails can go: the lined cell (4d), dry stacking (P4), and now back into the ground.

**Tailings has no stopes** (its mine is TBM drives), so "old workings" are **any enclosed void underground** the player closes with
bulkheads: a worked-out drive branch, a natural cave, an old flat drive *(Decision L5-1)*.

**Real counterparts:**
- Paste fill plants at Canadian and Australian underground mines: a thickener, a disc filter, a continuous paste mixer with a cement
  silo, a positive-displacement piston pump, and steel fill lines down boreholes to the stopes.
- Shotcrete or timber fill bulkheads (barricades) closing each stope's drawpoints.
- ACG *Paste* conference proceedings.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

```
 tails slurry -> TAILINGS THICKENER (P4) -> underflow 60 % -> PASTE FILTER (disc) -> cake --+
                                     \-> underflow ---------------------------------------+-> PASTE MIXER <- cement (hopper)
                                                                                           |     paste at 78 % solids
                                                                                           v
                                          PASTE PUMP (piston) -> steel pipe -> FILL OUTLET in a void closed by FILL BULKHEADS
                                                                                           -> wet paste -> sets to paste fill
```

## Size
- **The plant's tails:** 50 t/h of solids. A paste at **78 %** solids and **2.0 t/m³** *(proposed)* carries 1.56 t of solids a m³: so
  all of the plant's tails make about **32 m³ of paste an hour**, about 0.5 m³ a minute.
- **Binder:** **5 % cement** by mass of solids *(proposed; real 3-7 %)*: **2.5 t an hour** at the full rate.
- **Strength:** at 5 % about **0.5-1 MPa** at 28 days, enough to stand as a wall when the next drive is cut beside it *(real)*.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `paste_filter` | 5 x 5 x 8 | A vacuum disc filter with its vacuum pump, to a cake of about **80 %** solids, **75 kW** *(proposed)*. Thickened tails in on pipe; cake out by touching the mixer |
| `paste_mixer` | 3 x 3 x 5 | A continuous twin-shaft mixer, **75 kW** *(proposed)*. Cake by touching, thickened tails on pipe to trim the solids, **cement** by a hopper on top. Paste out on pipe |
| `paste_pump` | 3 x 3 x 6 | A hydraulic piston pump, about **35 m³/h at 8 MPa**, **110 kW** *(proposed: 35 x 8,000 / 3,600 / 0.7)* |
| `fill_bulkhead` | 1 x 1 x 1 | One block of a **shotcrete bulkhead** wall; a wall of them closes a void's openings. Crafted from cement and gravel *(proposed)* |
| `fill_outlet` | 1 x 1 x 1 | The open end of the fill line inside the void, on Wildcatter steel pipe |
| `paste_fill` | a block, not an item | Placed paste: **wet** when placed, **set** after curing |

All motors at the world's standard industrial low voltage.

## How it runs
- **The void:** from the `fill_outlet`, the void is every open cell (air or water) it can reach underground, held in by solid blocks
  and bulkheads, up to **20,000 cells** *(proposed, as the lined cell's basin search)*. A void that reaches the surface (open sky) or
  over the limit reads **NOT ENCLOSED** and the pump will not start *(proposed: real fill would run out of an open drawpoint)*.
- **Filling:** paste comes out at the outlet and fills the void **from its lowest cells up**, a `paste_fill` (wet) block for each m³,
  displacing water. The outlet reads FULL when the void is full to its own level.
- **Curing:** wet paste sets after **one game day (20 minutes)** *(scaled; real 28 days to design strength)*. Wet paste is soft: it
  slows what walks in it, like mud, and it is not a block to stand on. Set paste is a solid block that can be mined: it drops nothing
  (its mass is tails), like settled tails.
- **A bulkhead broken** while the paste behind it is wet lets the wet paste run out through the gap by the settle rule (as muck
  falls), and the void stops counting as closed *(proposed: the real hazard of paste fill is a bulkhead failure)*. Set paste stays.
- **The pump** runs while it has paste, its line to the outlet is whole and the void is closed and not full. Its line pressure rises
  with the line's length: past about **1 km** of pipe *(proposed)* it reads LINE PRESSURE and stops.
- **Readouts:** the outlet reads the void's size, m³ filled, m³ wet and set. NO CEMENT, NO FEED, NOT ENCLOSED, FULL, LINE PRESSURE.
  **PLC points:** the pump's RUN, RUNNING, FLOW (4-20 mA), PRESSURE (4-20 mA).

## Cement
- Wildcatter and Tailings have **no cement** yet. Tailings already makes quicklime from calcite. Proposed: a `cement` item, crafted
  as a placeholder from quicklime and a clay ball until a real cement kiln exists (Decision L5-2). Real binders are Portland cement,
  often blended with slag or fly ash.

## Left out (to ROADMAP)
- Uncemented hydraulic fill and rock fill.
- Fill strength and wall exposure; stope design.
- Borehole lines and their wear; paste rheology in detail.
- Filling the open pit (real in-pit backfill is dumped, not pasted: the settle rule already fills a pit with tipped loads).

## Game tests to add
- `paste_fill_void`: a bulkheaded 10 x 6 x 6 drive branch fills from the bottom up and reads FULL.
- `paste_not_enclosed`: an outlet in a void open to the sky reads NOT ENCLOSED.
- `paste_cure`: wet paste sets after the cure time.
- `paste_bulkhead_fail`: breaking a bulkhead with wet paste behind it lets paste run out.
- `paste_no_cement`: the mixer stops NO CEMENT.

## The wave plan
- **L5**, after the shaft (L4): real paste goes down a shaft or borehole; here a steel pipe down the decline works too.

## Sources
- ACG and Turkish conference papers on cemented paste backfill: 70-85 % solids; binder about 3-11 %; 0.75-0.9 MPa at 6-7 % binder at 28
  days; a UWA dataset: 268 kPa at 5 % binder and 73 % solids, 954 kPa at 9 %.
- A scheelite mine (Mittersill) study: 5 % binder gave over 1 MPa at 28 days and met pumpability.
- Pump and plant sizes *(proposed)*.

## As built (L5, 2026-11-10, branch `l5-paste`)
Decisions in [../questions/l5-paste.md](../questions/l5-paste.md) (22 entries; #8, #14 and #19 need Tim).
- **Existing worlds are untouched:** every block, item and part is new (#22).
- **Code:** `paste.PasteFill` (the figures, unit-tested in `PasteFillTest`) and `paste.FillVoid` (the void scan, `FillVoidTest`);
  `Machine.PASTE_FILTER`, `PASTE_MIXER`, `PASTE_PUMP` on the plant's motors (`block.stage.Paste`); `block.FillOutletBlock` and its
  entity; `block.FillBulkheadBlock` (the spill); `block.PasteFillBlock` (wet and set, the cure); `tools/models/paste.py`.
- **As the spec, except:** the mixer trims the cake with 0.21 kg of thickened tails a kg of cake solids to exactly 78 % with its cement
  (#7); cement is a 50 kg bag, a bag a tonne of tails (#8); the pump pushes into the outlet it finds on its own trace of the line, up to
  1,500 m (#9); line pressure is friction (8 kPa/m) less the paste's weight in the fall (19.6 kPa/m) (#10); curing is eight steps of
  2.5 minutes (#12); only a broken bulkhead lets paste out, all at once (#14); the void's walls are any block, torches and rails
  included, and it needs no bulkhead if it is closed (#2).
- **Measured (PasteFillTest, the scenes):** a m³ of paste holds 1.49 t of tails and 74 kg of cement; the plant's 50 t/h of tails make
  33.6 m³/h; the filter's filtrate 0.42 L/kg; the pump needs 111 kW; 1,000 m on the flat is 8.0 MPa, 1,001 m reads LINE PRESSURE.
- **Tests:** NeoForge `paste_fill_void` (the spec's 10 x 6 x 6 branch fills layer by layer, 359 m³, and reads FULL),
  `paste_not_enclosed` (open to the sky; a bulkhead missing), `paste_cure`, `paste_bulkhead_fail` (71 m³ of wet paste runs out of
  the gap; the set layer stays; NOT ENCLOSED), `paste_no_cement`, `paste_plant` (thickened tails to wet paste in the void through all
  three parts; FLOW and PRESSURE on the PLC), `paste_line_pressure` (1,100 m of line); the client film `paste`
  (`TAILINGS_ONLY=paste ./gradlew :fabric:runClientGameTest`).
