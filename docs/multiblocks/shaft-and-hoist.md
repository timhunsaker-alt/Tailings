# A shaft and hoist (raise-bored shaft, headframe, double-drum hoist, skips, cage)

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed wave **L4**. The choices are listed
in [../questions/specs-tailings.md](../questions/specs-tailings.md) (Later section).

ROADMAP "Later": a shaft and hoist (headframe, skip) instead of the decline. Today all rock climbs the TBM's 1-in-2 rack decline by
train. A **vertical shaft** with **skips in balance** lifts rock in a fraction of the time and power: a loaded 10-car train needs about
74 kW for 0.34 m/s up the decline (haulage.md); a skip lifts its load straight up at 10 m/s.

**Real counterparts:**
- Raise boring: a raise borer on the surface drills a pilot hole down to an existing drive, then pulls a reamer head up through it,
  the cuttings falling into the drive. Used for shafts, ore passes and ventilation raises.
- A steel headframe over the shaft with its sheaves, a **double-drum hoist** in a hoist house, two **skips** in balance (one up, one
  down), a **loading pocket** with a measuring flask at the bottom, a skip dump and **bin** at the top.
- A cage for people in its own compartment *(on the same hoist in small mines)*.
- SME *Mining Engineering Handbook*, 3rd ed. (2011), hoisting chapter.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

```
 surface:   HOIST HOUSE (double drum, motor)  ----ropes---->  HEADFRAME (sheaves; skip dump -> HEADFRAME BIN -> belt to the plant)
                                                                  ||
 shaft:     5 x 5 m clear, lined; two skip compartments and a cage   ||  skip up / skip down (in balance)
                                                                  ||
 bottom:    train -> UNDERGROUND DUMP POCKET -> apron feeder -> MEASURING FLASK (3 t) -> loads the skip at the bottom
```

## Sinking it: raise boring (Decision L4-1)
- **The order:** the TBM drives its decline and flat drive first, as today. Then a **raise borer** on the surface, directly over the
  drive, drills a **pilot hole** down to it and **reams back up** to the shaft's full size. The cuttings fall into the drive, where a
  train or a loader takes them *(real raise boring)*.
- **Size:** a **5 m** reamed shaft *(proposed: real raise borers ream 1-6 m)*, a **5 x 5** block shaft in the game, lined with segments
  or cobble as the TBM lines.
- **Rates *(scaled)*:** pilot hole **1 m every 10 s**; reaming **1 m every 60 s** *(proposed; real reaming makes a few m a day)*.
  A 150 m shaft takes about 25 min of pilot and 2.5 h of reaming.
- **The raise borer** (`raise_borer`, 4 x 6 x 6, **400 kW** at the world's standard medium voltage *(proposed)*): placed on a level
  pad, it reads NO DRIVE BELOW until a drive lies under it within its depth (up to **200 m**, *proposed*).
- **Why not sink from the top:** real shaft sinking is drill-and-blast from a sinking stage; Tailings has no underground blasting
  *(left out)*.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `headframe` | 9 x 30 x 9 | A steel A-frame headframe over the shaft with its two sheaves, the skip dump scroll and the bin under it *(proposed: 30 m high)* |
| `headframe_bin` | 4 x 5 x 4 | A **100 t** steel bin under the skip dump *(proposed)*, feeding a belt to the plant |
| `hoist_house` | 10 x 8 x 12 | The hoist house with a **double-drum hoist**, drums about **3 m** across *(proposed)*, and its motor: **450 kW** at the world's standard medium voltage *(proposed, below)* |
| `skip` | 2 x 4 x 2, two | A **3 t** bottom-dump skip *(proposed)* on its rope, running in guides in its compartment |
| `cage` | 2 x 3 x 2 | A cage for 4 people *(proposed)*, on the hoist's second drum in place of one skip when set to CAGE |
| `measuring_flask` | 2 x 3 x 2 | The loading pocket's flask at the shaft bottom: it fills to **3 t** and empties into the skip |
| `underground_dump_pocket` | as `dump_pocket` (dump-station.md) | The train's dump at the shaft bottom, its apron feeder to the flask |

## The hoist
- **Rope speed:** **10 m/s** *(proposed; real small shafts 8-12 m/s)*, accelerating and braking at **0.75 m/s²** *(proposed)*.
- **Power:** with the skips in balance, the motor lifts only the payload and the losses: 3 t x 9.81 x 10 m/s / 0.85 = **350 kW**, plus
  acceleration: a **450 kW** motor *(proposed)*.
- **A cycle** for 150 m: about **28 s** of winding (13 s to reach 10 m/s, 13 s to brake, under 2 s at full speed), plus **10 s** at
  each end to load and dump *(proposed)*: about **48 s**, so **225 t/h**. The 50 t/h plant needs a fifth of that; the TBM's trains feed it.
- **Rope and brakes:** the hoist's brakes hold the skips when it stops or loses power *(real)*. Overwind: the skips are stopped at the
  ends of their travel by the hoist's limits; an overwind is left out.

## How it runs
- **Built and checked:** the hoist house reads NOT BUILT until the headframe stands over a shaft, its ropes reach the house (within
  **60 m**, *proposed*, in a line), both skips (or a skip and the cage) hang in the shaft, and the flask stands at the bottom beside the
  shaft.
- **SKIP mode:** while the flask has 3 t and the headframe bin has room, it winds: the full skip up and the empty one down, dumps at the
  top, loads at the bottom. A train tips into the underground dump pocket as at the surface dump.
- **CAGE mode:** a player in the cage rides it; buttons in the cage and at each landing call it to the surface or a level *(proposed:
  two landings, the top and the bottom)*. The skip on the other drum balances it, empty.
- **A shaft as the return airway:** a fan on the headframe can pull the drive's stale air up the shaft (ventilation.md, Left out
  there) *(proposed for a later wave)*.
- **Reads:** STOPPED, WINDING, WAITING FOR ORE, BIN FULL, NOT BUILT. **PLC points:** RUN, RUNNING, SKIPS (pulse each skip dumped), the
  headframe bin's LEVEL.

## Left out (to ROADMAP)
- Koepe (friction) hoists, rope stretch and rope inspection, overwind.
- Drill-and-blast shaft sinking; shafts deeper than the world allows.
- Several levels with several loading pockets.
- Lowering the TBM's parts down the shaft (real practice when a TBM starts underground).

## Game tests to add
- `shaft_raise_bore`: a raise borer over a drive drills its pilot and reams a 5 x 5 shaft; the cuttings land in the drive.
- `shaft_hoist`: two skips in balance lift ore 150 m at about 225 t/h into the headframe bin.
- `shaft_cage`: a player rides the cage from the bottom to the top.
- `shaft_not_built`: a missing skip reads NOT BUILT.

## The wave plan
- **L4**, after ventilation (L1), battery locomotives (L2) and bottom-dump cars (L3): the bottom-dump car is the natural way to tip
  into the underground pocket.

## Sources
- Hoist Magazine: a double-drum hoist for 16 t (36,000 lb) at 11 m/s (2,200 ft/min) to 1,260 m on a 3 MW (4,000 hp) motor.
- A Moroccan shaft (3.2 m): a 14.5 t skip at 10.5 m/s, 250 t/h from 670 m, a four-rope Koepe winder on a 900 kW DC motor.
- A Spanish potash shaft: 30 t skips at 16.5 m/s, 570 t/h from 680 m, a 3,100 kW motor.
- Raise boring sizes and rates *(unverified)*.
