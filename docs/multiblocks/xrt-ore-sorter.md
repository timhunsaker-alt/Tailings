# X-ray transmission ore sorting (its own machine)

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed wave **L8**. The choices are listed
in [../questions/specs-tailings.md](../questions/specs-tailings.md) (Later section).

ROADMAP "Later": X-ray ore sorting as its own machine; "the dump sorter is a stand-in, not that machine". A real **XRT sorter**
spreads crushed rock in a single layer on a fast belt, shoots it through a **dual-energy X-ray line**, and judges each piece by its
mean **atomic density**. A bank of **air valves** blows the chosen pieces off the end of the belt into one chute; the rest fall into
the other. It sorts **ore from waste piece by piece**, which the dump's sorter (by rock type, at the click of a screen) does not.

**What it adds:** the same job as dense-medium separation (pre-concentration, rejecting waste before the mill), but **dry, coarse
(10-125 mm) and with no medium**, at a real throughput that suits the pit line. And it is the real machine behind the dump sorter's
"sensor" (Decision L8-1: the dump sorter stays as it is).

**Real counterparts:**
- XRT sorters with a **1.2 m** working width: **10-125 mm**, up to **250 t/h**, 152-192 ejectors (the 1.2 m class); a 2.4 m
  machine to 500 t/h.
- Ore sorting at tungsten, tin, base-metal and diamond mines.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

```
 crushed rock (jaw product, 40-150 mm; the gyratory's on the pit line)
   -> SIZING SCREEN (passes -125 mm to the sorter; +125 mm back to the crusher, -10 mm round the sorter)
   -> XRT SORTER: vibrating feeder -> 3 m/s belt -> X-ray line -> air ejectors
        | ejected: pieces over the threshold (product)        | not ejected (waste)
        v                                                     v
      belt to the plant                              waste: belt to a bin or truck load-out
 COMPRESSOR (air for the ejectors)
```

## How it judges a piece
- **Each piece** is one kg-lot of one rock off the belt *(the dry stages carry kg of each rock and size class)*. The sorter reads its
  **mean atomic density** from what the rock holds: country rock (Si, Al, O, light) is low; a piece carrying sulfides, oxides of
  heavy metals or native metal is high *(proposed: the ore minerals' share at the game grade decides, as in DMS)*.
- **The threshold** is set on the sorter's screen, in steps from LOW to HIGH *(proposed: 5 steps)*. Over it, a piece is ejected to
  **product**; under it, it drops to **waste**.
- **Errors:** **3 %** of ore pieces drop to waste and **5 %** of waste pieces are ejected with the product *(proposed: real sorters
  recover about 90-97 % of the metal)*.
- **What it cannot see:** a piece that is mostly gangue with a little sulfide. The game, which takes the block's mean, gives the same
  answer for every piece of one rock; the error rates above stand in for the real spread.
- **Country rock's trace minerals** in waste leave with it, as in DMS: no nuggets from what it rejects.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `sizing_screen` | 3 x 3 x 5 | A double-deck screen at 125 mm and 10 mm, **22 kW** *(proposed)*. +125 mm back to the crusher by belt; -10 mm round the sorter to the plant |
| `xrt_sorter` | 4 x 5 x 8 | A **1.2 m** XRT sorter: vibrating feeder, belt at 3 m/s, X-ray source and line-scan detector, **192 ejectors**. Up to **250 t/h** at 50-125 mm, about half that at 25-50 mm. About **20 kW** for the belt, feeder, source and electronics *(unverified)*. X-ray shielding round the source: no harm outside the cabinet |
| `sorter_compressor` | 2 x 2 x 3 | A screw compressor and air receiver for the ejectors, **160 kW** *(proposed)* |

All motors at the world's standard industrial low voltage.

## How it runs
- It runs while it has feed, air and room on both outputs; its rate is the feed's, up to its rating. Its rating at the game's crushed
  class (40-150 mm) is taken as **250 t/h** *(proposed)*.
- **Where:** on the 50 t/h plant, between the jaw and the screen (one sorter has five times the capacity); on the pit line, between
  the gyratory's surge pocket and the stockpile, **four** sorters for 1,000 t/h at full crushing rate *(proposed)*.
- **NO AIR:** with the compressor off it ejects nothing and everything goes to waste: the sorter stops and reads NO AIR *(proposed: so
  ore is never lost by a stopped compressor)*.
- **Reads:** NO FEED, NO AIR, FULL, "its output has nowhere to go". **PLC points:** RUN, RUNNING, THRESHOLD (analog out from the PLC,
  4-20 mA over the five steps), PRODUCT and WASTE (pulse a tonne each).

## Left out (to ROADMAP)
- Other sensors: colour (optical), laser, NIR, X-ray fluorescence and luminescence (the real diamond sorter; Decision X-2).
- A piece-by-piece grade distribution inside one rock type.
- Dust extraction round the sorter.

## Game tests to add
- `xrt_split`: andesite and galena-ore crushed rock in at the default threshold: about 97 % of the galena ore to product, about 95 % of
  the andesite to waste.
- `xrt_threshold`: at LOW nearly everything is product; at HIGH low-grade ore goes to waste.
- `xrt_no_air`: with the compressor off the sorter stops NO AIR.

## The wave plan
- **L8**, last of the Later waves: its use grows with the pit line (P7).

## Sources
- A sorter maker's 1.2 m and 2.4 m XRT product data: 1,200 mm working width, 10-125 mm, up to 250 t/h; 2,400 mm to 500 t/h; 152 or 192
  ejectors (indicative figures, "exact numbers on request").
- A Russian 1,200 mm XRT separator: up to 186 t/h at -100+50 mm, 93 at -50+25, 47 at -25+12, 33 at -12+6.
- A small XRT unit listing: 12 kW installed, 0.6 MPa air, 3.5 m³/min of air *(vendor, unverified)*.
