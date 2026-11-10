# The walking dragline (APPROVED by Tim 2026-11-09; built in P6, see "As built" at the end)

**Strip mining.** A walking dragline sits on top of a flat-lying deposit's overburden and digs a long cut ("strip") down to the
seam. It casts the overburden sideways into the previous, mined-out strip as a spoil pile. Loaders and trucks
([open-pit.md](open-pit.md)) then take the uncovered seam. The next strip's overburden goes into this strip's hole.

**Real counterparts:**
- Walking draglines of the 30-60 m³ class at phosphate mines (Florida), bauxite and coal strip mines.
- SME *Mining Engineering Handbook*, 3rd ed. (2011): dragline stripping, the cast and the extended bench.

**Decided with Tim (2026-11-09):** a dragline for strip mining. It digs the overburden off the existing veins' flat-lying
deposits, such as bauxite, phosphate rock and bentonite. The seam's own ore goes through the plant, as every load does.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked against a manufacturer's spec sheet or the SME handbook before approval.

```
 section across the strip, the dragline walking into the page:

                         boom 75 m at 32°
                      .---------------------------------------.
                     /                                         \  hoist ropes
   [house][A-frame] /                                           |
   ==== tub ====   /                                             [bucket 33 m³]
 ------------------ highwall top (the dragline's bench) ------+  |
   overburden                                                 |  v dig
 ------------------                                           |  .        spoil pile (cast, settles)
   SEAM (bauxite, phosphate...)                               |___________/\/\/\/\_______  <- last strip, mined out
```

## Size and rate (a mid-size walking dragline; checked 2026-11-09 against a 33.6 m³ machine on a 74.7 m boom and the 24-34 m³ class's sheets)
| | |
|---|---|
| Bucket | **33 m³**, about 55 t of rock a pass |
| Boom | **75 m** lattice at 32°: about 65 m reach out from the centre, point about 42 m up (the class: 75-101 m) |
| Dig depth | down to **35 m** below the tub *(unverified)* |
| Tub (base) | **15 m** across, the machine's whole weight on it while digging |
| Walking shoes | two, each **3 x 14 m**, one each side of the house *(unverified)* |
| House | **24 x 12 x 20 m** (W x H x L): hoist, drag and swing motors on their generator sets *(unverified)* |
| Operating mass | about **1,750 t** (the class: 1,750-1,990 t) |
| Power | electric, a trailing cable from the pit switch house at Wildcatter's medium-voltage tier: about **2.5 MW average, 6 MW peak** *(proposed)* |
| Cycle | **55 s**: drag in, hoist and swing 90°, dump, swing back *(real 50-60 s)* |
| Output | about **1,600 m³ of bank an hour** (a 33 m³ bucket of loose rock is about 25 m³ of bank; 65 cycles an hour) |
| Walking | **one step of 2 m a minute** *(real about 0.2 km/h)* |

## Parts (big parts, then a contraption)
| Part | Cells (W x H x L) | Basis |
|---|---|---|
| `dragline_tub` | 15 x 2 x 15 | The round base. Its main cell holds the machine's block entity: status, screen, cable |
| `dragline_house` | 24 x 12 x 20 | The revolving frame and machinery house, with the operator's cab at its front corner |
| `dragline_shoe` | 3 x 3 x 14, two | Walking shoes on cams, one each side |
| `dragline_mast` | 6 x 20 x 4 | The A-frame and mast holding the boom's suspension ropes |
| `dragline_boom` | 4 x 4 x 75 | The lattice boom, placed in sections of 7.5 m (ten `dragline_boom_section` items, one part) *(proposed: one 75 m item would be odd to hold)* |
| `dragline_bucket` | 5 x 4 x 6 | The 33 m³ bucket with its rigging |

- **Built and checked:** the tub reads NOT BUILT, naming the part, until the house sits on the tub, a shoe on each side, the
  mast on the house, the boom foot at the house's front, its sections in a line, and the bucket under the boom point. It reads
  NO CABLE until a trailing cable reaches it from a `pit_switch_house`.
- **The contraption:** started, its parts lift into one entity (multiblocks README, The contraption). The house, mast, boom and
  bucket swing about the tub's centre. The ropes are drawn as lines from the boom point to the bucket. Stopped, it sets down as
  blocks if the cells are free, as the TBM does on the flat.

## Running it
- **Its screen sets the job:**
  - **the strip:** a width (default 30 m), a length and the seam's top level. The dragline digs from the surface down to the seam;
  - **the cast:** the side to cast to, left or right of its walk. The spoil target is the previous strip's hole, or flat ground for
    the first ("box") cut;
  - **GO** and **STOP**, as the TBM's cab.
- **Each cycle** digs a bucket's worth (about 25 blocks of bank) from the strip within its reach, from the top down. It swings and
  dumps it at the cast target, where it lands by the settle rule (open-pit.md). So the spoil pile builds up as a real one does,
  at its angle of repose.
- **Walking:** when nothing more is in reach to the seam's top, it walks 2 m along the strip and digs again. At the strip's end
  it stops and reads DONE.
- **What it digs:** soft ground directly, and muck. **Hard overburden must be blasted first**, as for a shovel (open-pit.md, The
  blast). The real practice of "cast blasting", throwing the overburden into the spoil with the blast itself, is left out.
- **The seam is not dug by the dragline:** it stops at the seam's top level. The seam is taken by loaders and trucks. *(Real
  draglines do sometimes dig the seam; that is a later choice.)*
- **Driving it by hand** (the operator sitting in the cab, swinging and dragging with the keys) is listed in Known gaps:
  *proposed*, the cab runs the set job, which is how a dragline spends most of its life.

## Readouts and PLC points
- **The cab's screen:** status (STOPPED, DIGGING, WALKING, DONE, NOT BUILT, NO CABLE, CABLE END, BLOCKED), cycles, bank m³
  moved, kW, cable paid out.
- **PLC points** (a Wildcatter `ControlDevice` at its tub): RUN (command), RUNNING, AUTO, CYCLES (pulse), POWER (4-20 mA), DONE.

## What can go wrong
- **The cable** runs out at 400 m: CABLE END.
- **Overload:** its switch house breaker trips at 1.25 times its 6 MW peak for 1 s, as Wildcatter breakers do.
- **The highwall:** the dragline stands back from the strip's edge, by its tub's radius plus 5 m *(proposed)*. A real highwall
  failure under the machine is left out.
- **Broken part:** breaking any piece breaks the part. Stopped as a contraption it drops nothing until set down or dismantled
  (sneak, empty hand, on the tub), when every part drops its item.

## Left out (to ROADMAP)
- Cast blasting.
- Hand control of swing, drag and hoist.
- Highwall and spoil-pile failures.
- Rehandle (the dragline digging its own spoil again).
- Bucket-wheel excavators and spreaders.

## Game tests
- `dragline_built`: NOT BUILT names a missing shoe; NO CABLE without a trailing cable.
- `dragline_cycle`: one cycle takes about 25 blocks of sand from the strip and lands them at the cast target, settled.
- `dragline_strip`: a 30 x 10 m strip 5 m deep over a bauxite seam is uncovered to the seam's top, and the seam is untouched.
- `dragline_walk`: it walks 2 m when its reach is dug out; it reads DONE at the strip's end.
- `dragline_hard`: a stone overburden it cannot dig reads BLOCKED: blast it first.

## As built (P6, 2026-11-10; decided while Tim was away, see [../questions/p6-dragline.md](../questions/p6-dragline.md))

Everything above is built, with these readings and additions (each a numbered decision there):
- **The boom** is five `dragline_boom_section` parts of 15 m, each two of the 7.5 m sections pinned (1); a cell is a whole metre. The
  parts stand as the rope shovel's: the tub, a shoe each side of it under the house, the house on the tub, the mast on its roof, and the
  boom sections and bucket laid on the ground ahead (2, `pit.DraglineLayout`). Every part fills its whole box (3).
- **Lifted**, it is one machine drawn by its own model, its box the house over the tub; the boom and mast do not collide (5, 6). It digs
  and casts within the spec's 65 m; the boom is drawn true, its point 72 m out and 44 m up (7).
- **Its screen** sets the strip from where it was lifted: width 10-40 m (default 30), length 10-400 m, the seam's depth under its tub
  (1-35 m, default 10) and the cast side (8). It stops at the seam's depth and at any ore block (9). A bucket is 25 bank blocks or 55 t
  (10), dug from the topmost layer down (11). The spoil goes to the lowest ground beyond the strip's far edge plus 6 m, leaned away
  from the cut, so the first strip's spoil lands on flat ground and the next's in this one's hole (12).
- **Its draw:** 6 MW for the 15 s the bucket fills, 1.2 MW the other 40 s (2.51 MW average), 2.5 MW walking, 0.3 MW standing in its job
  (13). It walks when its reach is dug out and the strip runs on beyond it (14); at CABLE END it waits for another reel (15), which can
  now be clicked onto any lifted pit machine, a P5 gap closed (16). STOP ends its cycle first and sets it down if it can (17).
- **Its PLC points** are on its switch house outlet, not its tub (the tub lifts and walks): MACHINE RUN, RUNNING, DONE, CYCLES and POWER
  for each outlet, after the house's own points; AUTO is the house's selector (18).
- **Its cab** can be sat in; the keys do nothing (19). Film: `dragline` (20).
- **Scenes:** `dragline_built` (NOT BUILT names the right shoe, NO CABLE, RUN lifts it, STOP sets it down with its cable and job),
  `dragline_cycle` (25 blocks of sand, cast and settled beyond the strip; 6,000 and 1,200 kW on its outlet), `dragline_strip` (30 x 10 m,
  5 m deep: 1,500 m³ in 60 cycles, the 600-block bauxite seam untouched, DONE), `dragline_walk` (three 2 m steps to reach the strip's
  far end, then DONE), `dragline_hard` (stone: BLOCKED; its top 2 m as muck: dug, then BLOCKED again), `dragline_plc`.
- **Not built:** the spec's Left out list (ROADMAP, Later); hand control (ROADMAP, Known gaps).
