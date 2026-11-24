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

## As built (L4, 2026-11-09, branch `l4-shaft`)
Decisions in [../questions/l4-shaft.md](../questions/l4-shaft.md) (21 entries; #1, #5 and #20 most need Tim's eye).
- **Code:** `Machine.HEADFRAME`, `HEADFRAME_BIN`, `HOIST_HOUSE`, `MEASURING_FLASK` (models in `tools/models/shaft.py`); the stages in
  `block.stage.Shaft` (`HOIST`, `FLASK`, `BIN`); `block.ShaftLayout` (where the pieces stand, NOT BUILT); `shaft.Hoisting` (the figures)
  and `shaft.Winder` (the skips in balance, their loads; saved on the hoist house), both plain Java and unit-tested (`HoistingTest`);
  `entity.ShaftSkipEntity` and `client.SkipRenderer` (the skip's own 2 x 4 x 2 model, its load, its rope to the sheave and the drum);
  the apron feeder's flask branch (`Dump.feedFlask`).
- **The layout,** in the headframe's own frame (facing north from its main cell): the shaft is x and z 2 to 6; skip compartments x 2-3
  and 5-6, z 3-4; the hoist house stands behind it (its own south) within 60 m in line with the shaft; the flask has a cell in the ring
  round the shaft, down to 200 m; the bin touches the headframe. The skip loads with its bottom at the flask's bottom and dumps 18 m
  above the collar, so the wind is the depth plus 18 m.
- **Skips:** a `skip` item clicked on the headframe hangs one in the next free compartment; a sneaking empty-hand hit takes it off.
- **As the spec, except:** one 10 s rest at the ends (both ends work at once), so a 150 m shaft lifts about 270-280 t/h, not 225
  (#5, #6); the headframe bin feeds belts only; the flask takes only what the feeder brings, a rock item at a time into its surge (#10);
  the motor draws its full rating while running (#8). Only SKIP mode.
- **Tests:** NeoForge `shaft_hoist` (150 m, 168 m of wind: three dumps, a measured cycle of 39.75 s against 40.1 s, 272 t/h, full rope
  speed, the skips in balance), `shaft_not_built` (a missing skip, a missing flask, a blocked compartment), `shaft_flask_feed` (a
  bottom-dump car into the pocket, the feeder to the flask, 3 t measured), `shaft_bin_full` (BIN FULL, then a dump and the SKIP DUMPED
  pulse), `shaft_power` (the brakes hold with no power and it winds on); the client film `shaft_hoist`.

## Still to build (planned)
- **L4b, the raise borer**: built in heavy-iron-fixes (below, "As built: the raise borer"). The plan was (`raise_borer`, 4 x 6 x 6, 400 kW at the medium-voltage tier, proposed): a `RaiseBorer` stage on the plant
  block entity. Placed on a level pad, it looks straight down up to 200 m for a drive (an air cell under at least 3 cells of solid rock
  over a TBM floor block); NO DRIVE BELOW otherwise. PILOT: one cell a 10 s down the shaft's centre column (saved progress), a 300 mm
  hole drawn as a thin column. REAM: once the pilot breaks through, the 5 x 5 rises from the drive's roof to the collar a layer a 60 s;
  each layer's blocks become items of their rock (the TBM's mined-rock rule) dropped onto the drive floor under the shaft, where a
  loader or a train takes them; lining as the TBM's (28 segments or cobble) on the shaft walls. Reads PILOT n m, REAMING n m, DONE; a
  scene `shaft_raise_bore` at a test depth of 20 m with `testTicks`. Its 400 kW is a Wildcatter `Motor` at the medium tier.
- **L4c, the cage**: built in heavy-iron-fixes, below; as planned here except where that says. The plan was (`cage`, 2 x 3 x 2): a second skip kind on compartment 1's rope, hung by clicking the headframe with a cage item;
  the hoist house's sneak-click sets SKIP or CAGE (only with the cage hung). In CAGE the skip on drum 0 runs empty as the counterweight;
  a player in the cage's box rides it (the cage's entity carries passengers as a vehicle does; collision of its floor); call buttons:
  a `shaft_call` block at each landing (the collar and the flask's level) and the cage's own, which set the winder's target end. The
  same speed profile at 4 m/s for people (proposed, a real man-riding limit). Scene `shaft_cage`: a mock player rides from the bottom
  to the top.
- **The shaft as the return airway** (with L1 ventilation): a fan on the headframe counts the shaft as the drive's exhaust.
- **Levels:** several flasks, one per level, the winder's bottom stop chosen by which flask is full; a PLC LEVEL point.

## As built: the cage (heavy-iron-fixes, 2026-11-22)
Episode 7 of Heavy Iron needs someone to ride the shaft (../questions/heavy-iron-fixes.md, 12-15). The raise borer followed (below).
- **Its own compartment beside the skips:** the shaft's south side, x 3-4, z 5-6 in the headframe's frame (the skips are z 3-4). A
  `cage` item clicked on the headframe hangs it at the collar; one a headframe. 2 x 3 x 2 m: a steel deck and roof, corner posts, mesh
  sides and a gate, the rope from its bail to the head sheave and the hoist's drum.
- **People and materials:** up to **4 people** ride it (they board with an empty hand; a gate holds them in while it moves; they step
  off at a landing by sneaking); its deck carries **9 stacks** (sneak with a stack to load it, with an empty hand to unload).
- **Landings:** the collar, and the bottom landing at the measuring flask's level (the skips' loading stop).
- **Running it:** a rider's boarding rings the bell to go to the other landing; sneaking with an empty hand on the headframe calls it
  to the collar, on the flask to the bottom. The bell rings **5 s** *(proposed: the onsetter's signal)*, then it winds on the skips'
  profile (0.75 m/s², creep at the ends) at the man-riding **4 m/s** (the spec's proposal). 40 m takes about 15.3 s plus the bell;
  150 m about 43 s.
- **Its own winder (2026-11-23, Tim):** the cage no longer rides on the skip hoist. A **cage winder** beside the hoist house winds it
  on its own 2.5 m drum and rope over its own 2.5 m sheave in the headframe, on its own 160 kW motor; the skips and the cage no longer
  wait for each other. See [cage-winder.md](cage-winder.md). (It was: it took the hoist once the skips were at rest, the skips waiting
  for it.)
- **Power:** the cage winder's own motor, from the medium voltage.
- **Call buttons (films-hi07):** any redstone signal reaching the headframe calls the cage to the collar, one reaching the measuring
  flask calls it to the bottom landing: a stone button on a block beside either is the landing's call button (NeoForge
  `shaft_cage_button`). No new block.
- **Not built:** several levels, the cage's own column check in NOT BUILT.
- **Tests:** `HoistingTest.theCageRidesAtFourMetresASecond`; NeoForge `shaft_cage` (a stack of rail and a rider down 40 m in the bell's
  5 s and 15.3 s at 4 m/s, the skips standing; called back up from the collar).

## As built: the raise borer (heavy-iron-fixes, 2026-11-23)
As L4b planned, except where listed (../questions/heavy-iron-fixes.md, 16-19). The figures are in `shaft.RaiseBoring` (`RaiseBoringTest`).
- **The part:** `raise_borer`, 4 x 6 x 6 (`tools/models/shaft.py`): its baseplate on the pad, two guide columns and the derrick head over
  the hole, the rotary drive head and the rod down the hole, the rod rack at its north end, the 400 kW hydraulic power pack at its
  south end; **400 kW** from the medium-voltage tier (the spec's proposal), its cable on the west face of its main cell.
- **Where the raise goes:** in the borer's own frame (facing north from its main cell) the 5 x 5 is x 0 to 4, z 1 to 5, the pilot down
  its centre (x 2, z 3). It looks down that column up to **200 m** for a drive: the first open cell under at least **3 m** of rock, with
  a floor within 6 m under it. NO DRIVE BELOW otherwise.
- **PILOT:** a metre each **10 s** to the drive (the 300 mm pilot is not drawn: the cuttings puff out at the collar). **REAM:** then a
  metre each **60 s** from the drive's roof up to the layer under the pad, **5 m** across (the spec's: real borers ream 1-6 m in a few
  metres a day, *scaled*). The pad's own metre under the borer is the collar slab, left for the player to take out once the borer is
  moved, as a real raise is finished under the machine's pad. A 20 m drive: 200 s of pilot and 18 m, 18 min, of reaming.
- **The cuttings:** each reamed layer's blocks fall into the drive as their own drops (stone as cobblestone), spread on the drive's
  floor under the raise, where a loader or a train takes them. Block entities and unbreakable blocks are left.
- **No lining:** the raise stands in its rock, as raise-bored shafts in competent rock usually do *(unverified)*; any open or flooded
  cell round a layer is grouted with cobblestone. (The spec said segments or cobble as the TBM's.)
- **Reads:** NO DRIVE BELOW, PILOT n of m, REAMING n of m, DONE; saved on the part.
- **Drawn in the hole (films-hi07):** while it drills, the drill string of 1.5 m rods and its bit go down the hole as far as the
  pilot has gone; while it reams, the string runs down to a 4.8 m reamer head under the cutting face, its cutters on top, rising as
  it cuts; both turn with the drive head's spindle at 8 rpm (proposed). The client is sent the borer's progress each metre and every
  2 s, and runs it on between.
- **Not built:** PLC points.
- **Tests:** `RaiseBoringTest`; NeoForge `shaft_raise_bore` (a 20 m drive: the 5 x 5 open for 18 m, the walls and the collar slab
  standing, 450 cobblestone in the drive).

## Drawn moving (films-hi07, 2026-11-23)
- **The head sheaves and the drums turn with the rope:** their spokes (and the drums' rope clamps and flange spokes) are drawn by
  `PlantRenderer`, turned as far as each skip has moved (the rope over a 2.9 m sheave, onto a 3 m drum); the static spokes are out of
  `shaft.py`. The cage's drum and sheave turn on its own winder (cage-winder.md, 2026-11-23).
- **The skip's dump reads:** while a skip pours (the first 4 s of the rest), its bottom door is drawn swung open and a stream of
  rock falls from it to the headframe bin's mouth, thinning as it empties; the heap in the skip sinks as it pours (synced on the skip:
  how much is left and where the bin's mouth is). The load still goes into the bin's count at the rest's end, as before.
