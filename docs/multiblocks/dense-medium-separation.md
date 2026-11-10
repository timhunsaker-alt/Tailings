# Dense-medium separation (a DMS cyclone plant ahead of the mill)

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Wave **L7**, built (see "As built"). The choices are listed
in [../questions/specs-tailings.md](../questions/specs-tailings.md) (Later section).

ROADMAP "Later": dense-medium separation. Crushed rock is mixed with a **heavy medium** (a slurry of fine ferrosilicon in water) at a
set density. Pieces lighter than the medium **float** and are rejected; heavier pieces **sink** and go on to the mill. It is
**pre-concentration**: it throws away barren rock before the expensive grinding. Real hard-rock DMS rejects **20-60 % of the mass
while keeping over 90 %** of the metal, where the ore is amenable.

**What it adds to the game:** the mill is the plant's bottleneck (50 t/h). A DMS plant on the gravel loop lets the player **reject
country rock before the mill** and spend the mill on ore. The cost is real: the floats leave **with their trace minerals**, so no
nuggets from what is rejected (Decision L7-1). Tim's rule "everything goes through the plant" still holds: the floats were crushed
and screened in the plant; they leave as **aggregate** (vanilla gravel), as real DMS floats are sold.

**Real counterparts:**
- DMS cyclone plants for pre-concentration of lead-zinc, tin and tungsten ores, and every coal washery and diamond plant.
- Ferrosilicon medium (SG 6.8, for media of 2.6-3.3); magnetite medium (SG 4.5-5) for lighter separations such as coal.
- Wills & Finch, *Wills' Mineral Processing Technology*, 8th ed., dense medium separation.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

```
 DOUBLE-DECK SCREEN gravel face (6-40 mm) -> DESLIME SCREEN (washes off -1 mm, back to the sand belt)
   -> DMS CYCLONE MODULE (mixing box, medium pump, 610 mm DMS cyclone, medium at SG 2.75)
        | sinks                                    | floats
        v                                          v
   DRAIN-AND-RINSE (sinks) -> belt -> CONE      DRAIN-AND-RINSE (floats) -> aggregate: gravel blocks
        | medium and rinse water                   | medium and rinse water
        +-------------> MEDIUM RECOVERY (magnetic separator, densifier) -> back to the mixing box
```

## Where it sits
- **On the gravel (6-40 mm) face** of the double-deck screen, which crushing-circuit.md describes and R4a did not build ("No gravel
  face yet"). This wave builds that face. The **sinks** go on to the cone, as gravel does today; the **floats** leave the plant.
- **Sand (-6 mm) bypasses** the DMS and goes to the mill as before *(real: fines bypass a DMS)*. The crushed class (40-150 mm) goes to
  the cone as before. So the DMS sees only what the cone would have broken to gravel.

## What floats and what sinks
- **Each piece's density** is the mass-weighted density of the minerals its rock holds *(proposed)*: country rock is about
  **2.65-2.75** (quartz, feldspar; deepslate and diorite up to 2.9 *(unverified)*); an ore rock's gravel carries its ore mineral
  (chalcopyrite 4.2, galena 7.5, sphalerite 4.0, cassiterite 7.0 *(textbook densities)*) at the game grade, which lifts it: a 10 % sphalerite ore is about 2.80, a 10 % galena ore about 2.88 *(by the mixing rule)*.
- **The medium's density** is set on the module's screen: **2.6-3.3**, default **2.75** *(proposed)*. Pieces under it float. Set too high, low-grade ore floats with the waste; set too low, the denser country rocks (deepslate, diorite) sink with the ore. That trade is the real one.
- **Liberation at gravel size:** a piece of ore rock may be mostly gangue. The game takes the **block's** mean: a whole ore block's
  gravel sinks; country rock floats. So in practice **DMS rejects the country rock** and keeps the ores.
- **Efficiency:** **2 %** of sinks report to floats and 2 % of floats to sinks *(proposed: real DMS cyclones are sharp)*.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `deslime_screen` | 2 x 2 x 4 | A wet vibrating screen at 1 mm with spray bars, **11 kW** *(proposed)*. Its -1 mm goes back to the sand belt |
| `dms_cyclone_module` | 3 x 6 x 4 | The mixing box, a **55 kW** medium pump and one **610 mm** DMS cyclone on its stand, about **60 t/h** of feed *(unverified)* |
| `drain_rinse_screen` | 2 x 2 x 5, two | Drain-and-rinse screens for the sinks and the floats, **7.5 kW** each *(proposed)* |
| `medium_recovery` | 2 x 3 x 3 | A wet drum magnetic separator and a densifier that recovers the ferrosilicon from the rinse water, **5.5 kW** *(proposed)* |

All motors at the world's standard industrial low voltage. Water off a water pipe at the module.

## The medium
- **Ferrosilicon** (`ferrosilicon` item, 15 % Si milled grade). It is **lost at about 0.3 kg a tonne** of feed *(proposed; real
  losses 0.1-0.5 kg/t)*, made up by hopper on the medium recovery part. NO MEDIUM stops the module.
- **Where it comes from:** Wildcatter's planned ferroalloy plant (VISION: Tailings does not refine ferroalloys). Until it exists, a
  placeholder recipe *(Decision L7-2)*.

## How it runs
- It runs while it has gravel, water, medium and room on both product belts. Rate up to **60 t/h** *(proposed, more than the plant's
  share of gravel)*.
- **Floats** go out as **vanilla gravel**, 1 block for each 1.9 t *(open-pit.md's gravel mass)*, to a hopper, belt or bin touching
  the floats screen; they hold no mineral. **Sinks** go to the cone by belt.
- **Reads:** NO FEED, NO WATER, NO MEDIUM, FULL, "its output has nowhere to go". **PLC points:** RUN, RUNNING, DENSITY (4-20 mA over
  2.6-3.3, the medium's), FLOATS (pulse a tonne).

## Left out (to ROADMAP)
- A DMS drum for coarser feed; real partition curves and near-gravity material.
- Coal washing (vanilla coal has no place on the ladder) and diamond recovery (see the questions file, Decision X-2).
- Medium viscosity and contamination by slimes.

## Game tests to add
- `dms_split`: granite and copper-ore gravel in at SG 2.75: the granite floats out as gravel blocks, the copper ore sinks to the cone.
- `dms_density`: at SG 2.6 everything sinks; at 3.3 most country rock and little ore floats.
- `dms_medium`: ferrosilicon falls by 0.3 kg a tonne; with none it stops NO MEDIUM.
- `dms_gravel_face`: the screen's gravel face (crushing-circuit.md) sends gravel to a belt.

## The wave plan
- **L7**, with the screen's gravel face. After the dry mill, as it is the larger change to the plant's flow.

## As built (L7, 2026-11-10, branch `l7-dms`)
Decisions in [../questions/l7-dms.md](../questions/l7-dms.md) (16 entries; #4, #5 and #8 need Tim).
- **Existing worlds are untouched (#1):** four new parts and a new item; the screen's gravel face was already built in the plant-gaps
  wave and is used as it is (#2). No setting was needed.
- **Code:** `plant.DenseMedium` (mineral and host densities, the piece's density by the mixing rule, the split, the losses, the
  gravel; unit-tested in `DenseMediumTest`), `plant.DmsState` (the setting and the counts), `block.stage.DenseMediumStages` (the
  deslime screen, the module, the drain-and-rinse screens, the medium recovery), `Machine.DESLIME_SCREEN`, `DMS_MODULE`,
  `DRAIN_RINSE_SCREEN`, `MEDIUM_RECOVERY` on the plant's motors and PLC points, `TailingsItems.FERROSILICON`; `tools/models/dms.py`.
- **As the spec, except:** a piece's density by the real mixing rule, not mass-weighted (#3); the light industrial ores float at any
  setting (#4); the sinks come round through the cone and the DMS again, about 3 % of an ore block's mineral lost (#5); the floats
  screen is the one on the module's north side, the sinks screen the one on its south (#7); a 25 kg ferrosilicon bag, 4 from an iron
  ingot, a sand and a metallurgical coke (#8); no starting charge of medium (#9); the density set by a sneaking click, not a screen
  (#10); floats by hopper or hand, not by belt (#11).
- **Measured (DenseMediumTest, the scenes):** granite 2.65, copper ore 3.15, galena ore 3.14, sphalerite ore 3.72, tin ore 2.88; 2 t
  of granite and 1 t of copper ore at 2.75: 1.98 t floats (one gravel block and 80 kg waiting), 980 kg of the copper and 40 kg of the
  granite sink; 0.9 kg of ferrosilicon lost for 3 t.
- **Tests:** NeoForge `dms_split`, `dms_density` (2.6 and 3.3), `dms_medium`, `dms_gravel_face` (crushed copper ore through the screen's
  gravel face, the deslime screen and the module), `dms_parts`; unit tests `DenseMediumTest`, `MachineTest`; the client film `dms`.
- **Not built:** the spec's "Left out"; diamonds (X-2, #16).

## Sources
- *Recent developments in preconcentration using dense media separation* (a DMS plant maker): DMS at 0.5-250 mm; 20-60 % mass rejection at
  over 90 % recovery for amenable ores.
- *Dense Media Separation* (a metallurgy reference site): ferrosilicon SG 6.8 against magnetite 4.5; a typical medium at SG 2.65.
- A 2025 lead-zinc pre-concentration paper (Serbia): a 900 mm DMS cyclone at about 624 m³/h of pulp.
