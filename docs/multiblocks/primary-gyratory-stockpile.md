# P7a: the primary gyratory crusher, the coarse-ore stockpile and its reclaim

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Wave **P7a**, built (see "As built"). The choices are listed in
[../questions/specs-tailings.md](../questions/specs-tailings.md) (P7 section).

The first of the pit-sized line's three specs (open-pit.md, "The plant's size", decided (a)). Trucks tip straight into a gyratory
crusher. Its product goes up a wide belt to a **coarse-ore stockpile**, a real cone of crushed rock that the settle rule builds.
Feeders in a tunnel under the pile draw it out to the SAG mill line ([sag-mill-line.md](sag-mill-line.md)), whose fines go to the
pit flotation row ([pit-flotation.md](pit-flotation.md)).

**Real counterparts:**
- A **42-65 class primary gyratory crusher** (1,066 mm feed opening, 450 kW) in a direct-tip pocket: trucks tip from the side into
  the crusher's bowl.
- A surge pocket under the crusher and an apron feeder onto the stockpile conveyor.
- A conical coarse-ore stockpile over a reclaim tunnel with apron feeders, as at most large copper concentrators.
- Wills & Finch, *Wills' Mineral Processing Technology*, 8th ed.: gyratory crushers; SME *Mineral Processing Handbook*: stockpiles.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

```
 HAUL TRUCK -> backs to the TRUCK DUMP WALL -> GYRATORY CRUSHER (bowl) -> SURGE POCKET -> PRIMARY APRON FEEDER
   -> HEAVY BELT (1.4 m, up the stockpile conveyor) -> drops at the head, 30 m up -> COARSE-ORE STOCKPILE (a cone at 37°)
   -> RECLAIM FEEDERS in the tunnel under the pile (2 running, 1 spare) -> HEAVY BELT with BELT WEIGHER -> SAG MILL
```

## The line's rate: 1,000 t/h *(proposed)*
- **The line runs at 1,000 t/h, 24,000 t a day** *(proposed: a mid-size copper concentrator; one SAG mill and one ball mill)*.
  A bank block (2.7 t) every **9.7 s**; a 363 t truck every **22 min**, a 91 t truck every 5.5 min, keeps it full.
- **The gyratory is bigger than the line,** as real primaries are: it crushes at about **2,400 t/h when fed** and stands idle
  between trucks. Real primary crushers run at 50-65 % of the time; here about 40 %.
- **The stockpile** is what lets the trucks (a shift's work) feed the mill (all day), which is the whole reason it exists.
- The rate is the basis for every part in P7. Its consequences (power, the emulsion it needs, the tails) are in the questions file.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `gyratory_crusher` | 6 x 10 x 6 | A **42-65 class** gyratory: **1,066 mm** feed opening, **450 kW**, about **120 t**; **2,354-3,362 t/h** at a 152 mm open-side setting. Its spider and bowl are its top 2 layers; the mantle, eccentric and drive below; the motor box at its west face. Medium-voltage motor *(the world's standard medium voltage)* |
| `gyratory_pocket` | 14 x 8 x 10 | The concrete direct-tip pocket round the crusher's bowl: the **dump wall** on its two tipping sides (the P1 `truck_dump_wall` stands on its rim, one each side), and the rock breaker's pedestal. It holds **one truck load over the bowl**, 400 t *(proposed)* |
| `rock_breaker` | 2 x 3 x 6 | A pedestal hydraulic rock breaker on the pocket's rim, **75 kW** *(proposed)*, which breaks oversize jammed in the bowl (below) |
| `surge_pocket` | 8 x 6 x 8 | The steel-lined bin under the crusher: **500 t** of crushed rock *(proposed: about 1.5 ultra loads)* |
| `primary_apron_feeder` | 3 x 2 x 12 | A **2.4 m** apron feeder from the surge pocket onto the stockpile conveyor, up to **3,000 t/h**, **150 kW** *(proposed)* |
| `heavy_belt_conveyor` | 2 x 1 x 1 a block, a run of any length | A **1,400 mm** troughed belt at **4 m/s**, up to **3,000 t/h** *(proposed)*, laid like pipe as the 600 mm belt is (multiblocks README), up to an 18° grade *(1 in 3)* |
| `heavy_conveyor_drive` | 2 x 2 x 3 | The head pulley and drive: **75 kW a 30 m of run on the flat plus the lift power** at the run's rate (m g h, at 90 %) *(proposed)*. The stockpile conveyor, 150 m long and 30 m up at 3,000 t/h: 375 + 272 = **650 kW**, so **two 355 kW drives** *(proposed)* |
| `stockpile_head` | 3 x 4 x 4 | The head frame and chute at the top of the stockpile conveyor, on a steel gallery; it drops the belt's load onto the pile under it |
| `reclaim_tunnel` | 5 x 5 x 1 a block, laid like a belt | A corrugated steel or concrete tunnel under the pile, **5 m** square inside *(proposed)*; it holds up the pile above it |
| `reclaim_feeder` | 2 x 2 x 6 | A **1.5 m** apron feeder under an opening in the tunnel's roof, **500 t/h**, **55 kW** *(proposed)*. Variable speed: the SAG's feed controller sets it |
| `belt_weigher` | 2 x 1 x 2 | A belt scale on the SAG feed belt: it reads t/h, and the reclaim feeders follow its setpoint *(real SAG feed control)* |

## How it runs
### The crusher
- **Tipping:** a truck backs to a `truck_dump_wall` on the pocket's rim (a DUMP POCKET stake, as at the pit hopper) and tips into
  the bowl. The pocket takes a whole load if it has room for it, all or none, as the pit hopper does; otherwise FULL and the truck
  waits.
- **It takes rock, ore and muck; it refuses soil** (open-pit.md: soil goes to dumps).
- **It crushes** while its motor runs and the surge pocket has room: at its motor's output, up to **2,400 t/h** *(proposed, the
  bottom of the 42-65's range at 152 mm)*. Its product is the **crushed** size class, about **P80 150 mm**, carried in kg of each
  rock as the dry stages carry it.
- **Oversize:** **1 bank block in 200** of hard rock is a boulder that jams the bowl *(proposed: real primaries see a few a shift)*;
  the crusher reads BRIDGED and stops crushing until the rock breaker breaks it: **30 s** *(proposed)*, automatic while the breaker
  has power, or a player in its seat. The 100 t truck's and the ultra's muck alike.
- **The motor:** 450 kW at the world's standard medium voltage on its own motor box (its west face). Its starting current and
  its breaker as Wildcatter's motors.

### The stockpile
- **The pile is real blocks:** the stockpile head drops the belt's load at its chute, and it lands by **the settle rule** (open-pit.md)
  as `stockpile_rock` blocks: a muck block that holds **crushed** rock *(proposed: the muck block entity with the size class; 3
  quarters of a bank block each, the same swell)*. So the pile grows as a cone at **37°**.
- **How big:** a cone 30 m high (the head's height) at 37° is **40 m** across the base radius and holds about **50,000 m³**, about
  **100,000 t** at 2.0 t a block (3 quarters of 2.7 t): **4.2 days** of the line *(real piles hold 1-3 days live)*. The pile is ground: its size is what the player builds.
- **Live and dead:** each reclaim feeder draws **the block directly above its opening**; blocks above fall into the gap and the
  slopes roll in by the settle rule. So the pile drains a cone over each feeder, and what lies outside those cones at 37° stays: the
  **dead** stockpile. With three feeders in a line along the tunnel, about **25-35 %** is live *(real conical piles: 20-30 % live with
  one feeder; this falls out of the rule and is checked in the test)*.
- **The tunnel** must run under the pile with the feeders under its middle. A tunnel block holds up whatever is above it; a feeder's
  opening is the one place the pile can come down.
- **Dozing the dead pile in** (a dozer pushes the dead slopes over the feeders) is real and allowed; the track dozer works on
  `stockpile_rock` like muck.

### Reclaim
- **Reclaim feeders** run while their motors run and the belt under them has room, each up to 500 t/h, together at the rate the
  belt weigher's setpoint asks: **1,000 t/h** by default, set on its screen or by the PLC *(real SAG feed control)*.
- **No pile over a feeder:** NO FEED ("draw-down: pile is hung" when the blocks above stand but cannot reach the opening, which the
  settle rule never allows, so in practice "empty").
- **The SAG feed belt** carries the reclaim to the SAG mill's feed chute (sag-mill-line.md).

### Readouts and PLC points
- **Gyratory:** RUN, RUNNING, BRIDGED, AMPS (4-20 mA), the pocket's LEVEL and FULL.
- **Surge pocket and primary feeder:** LEVEL, RUN, RUNNING.
- **Belt weigher:** RATE (4-20 mA over 2,000 t/h), SETPOINT (analog out from the PLC), TOTAL (pulse each 100 t).
- **Reclaim feeders:** RUN, RUNNING, SPEED (4-20 mA).
- **The pile:** the stockpile head reads the pile's height under it and the tonnes in it *(counted as it is built and drawn)*.

## Removing a part
- Breaking the crusher, a pocket or a feeder breaks the part and drops its contents' items (R1). The pile's blocks stay. A broken
  `reclaim_tunnel` block lets the pile fall into the tunnel by the settle rule, which is what really happens.

## Left out (to ROADMAP)
- Mantle and concave wear, and the setting closing with wear.
- Dust collection and the stockpile's dome or cover.
- A stacker-reclaimer on rails (longitudinal piles) instead of a cone.
- In-pit crushing and conveying (open-pit.md, Left out).
- Tramp metal from a truck (real primaries have a tramp-iron magnet after the crusher).

## Game tests to add
- `p7_gyratory_tip`: a 363 t load tips into the pocket and is crushed at 2,400 t/h into the surge pocket; soil is refused.
- `p7_gyratory_bridge`: a boulder bridges the bowl; the rock breaker clears it in 30 s.
- `p7_stockpile_cone`: 2,000 blocks dropped at the head stand at 37° (as `pit_settle`).
- `p7_stockpile_live`: three feeders draw a 30 m pile until NO FEED; between 20 and 40 % of it is drawn.
- `p7_reclaim_rate`: the belt weigher holds 1,000 t/h with two feeders; with one it reads 500 t/h.
- Client film `p7_primary`: an ultra truck tips into the gyratory; the pile grows.

## The wave plan
- P7 is three specs and three waves: **P7a** this spec; **P7b** the SAG mill line; **P7c** pit flotation and the tails.
- P7a can run on its own: until P7b, the reclaim belt may feed the existing 50 t/h crushing circuit's grizzly *(the feeders follow the
  belt weigher's setpoint, set to 50 t/h)*.

## As built (P7a, 2026-11-10, branch `p7-pit-line`)
Decisions in [../questions/p7-pit-line.md](../questions/p7-pit-line.md) (#1-#19; #11 and #14 need Tim).
- **Existing worlds are untouched (#1):** new parts, a heavy belt and the stockpile block. A belt run is now one kind of belt; a truck
  finds the gyratory's pocket as well as the pit hopper; muck does not roll.
- **Code:** `plant.PitLine` (the line's rates, the heavy belt's drives, the cone, the feeders' share; unit-tested in `PitLineTest`),
  `plant.PitState` (the boulder count, the weigher's setpoint and counts, the tonnes stacked and drawn), `block.stage.PitLineStages`
  (gyratory, rock breaker, surge pocket, primary apron feeder, stockpile head, reclaim feeder, belt weigher), `Belts.HEAVY_DRIVE`,
  `block.HeavyBeltBlock`, `block.StockpileRockBlock`; `Machine.GYRATORY_CRUSHER` to `BELT_WEIGHER`; `tools/models/pitline.py`.
- **As the spec, except:** the pocket stands on the gyratory, over its bowl, and the stack is pocket, gyratory, surge pocket, apron
  feeder, 26 m deep (#2); the pocket is the pit hopper's kind (#3); every 200th hard block is a boulder, by count (#4); the breaker works
  by itself while its motor runs, no seat (#5); the heavy belt is drawn a metre wide (#6); there is no `reclaim_tunnel` part: the tunnel
  is any solid blocks with an opening in its roof over each feeder (#9); the head counts tonnes stacked and the feeders tonnes drawn, not
  the pile itself (#13); the weigher counts what the feeders put on its run (#14); the PLC points as #17.
- **Measured (the scenes):** an ultra's 363 t of granite is 134 bank blocks in the pocket and 1.2 t kept; the gyratory crushes at
  2,400 t/h; a boulder holds it 30 s; a 2,000-block pile stands about 10 m at 37 degrees; **one feeder under its middle draws 20 % of it,
  three 6 m apart 33 %** (#11; 26 and 41 % before Tim's answer of 2026-11-10 made the draw-down stand at 45 degrees); two feeders at the default setpoint run at 500 t/h each and the weigher reads 1,000 t/h, one alone reads
  500; a 150 m flat heavy run needs two drives.
- **Tests:** NeoForge `p7_gyratory_tip`, `p7_gyratory_bridge`, `p7_stockpile_cone`, `p7_stockpile_live`, `p7_stockpile_one`,
  `p7_reclaim_rate`, `p7_heavy_belt`, `p7_primary_line`, `p7a_parts`; unit tests `PitLineTest`, `MachineTest`; the client film
  `p7_line` (with P7b).
- **Not built:** the spec's "Left out".

## Sources
- A current primary gyratory crusher maker's brochure (2018): the 42-65, 120 t, 1,066 mm feed opening, 450 kW, 2,354-3,362
  t/h at 152 mm open-side setting; the 60-89, 750 kW, up to 12,500 t/h. Capacities for a typical feed, SG 2.75, maximum eccentricity.
- Live-to-total fractions of conical stockpiles, apron feeder sizes, belt and drive sizing: textbook values *(proposed)*.
