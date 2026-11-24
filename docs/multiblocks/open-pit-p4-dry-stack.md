# Open pit P4: the tailings thickener and filter, the truck load-out, waste dumps, berms, the track dozer

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** The choices are listed in
[../questions/specs-tailings.md](../questions/specs-tailings.md) (P4 section).

This fills in open-pit.md's P4 machines ("Tailings filter and truck load-out", "Waste dump", "Track dozer") so they can be coded
without questions. **Dry stacking** is the third place tails can go, beside Wildcatter's lined cell (4d) and, later, paste backfill
([paste-backfill.md](paste-backfill.md)).

**Real counterparts:**
- Filtered ("dry-stack") tailings: a high-rate thickener, large plate-and-frame filter presses with 2.5 x 2.5 m plates, a cake
  conveyor to a load-out or stacker, and truck haulage to a compacted stack (Karara, La Coipa).
- A truck load-out bin with a clamshell gate over a drive-through lane.
- End-tipped waste dumps with crest berms at mid-axle height (US 30 CFR 56.9300).
- A 70 t class crawler dozer (D10 class) with a semi-U blade.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

```
 flotation (or scavenger, leach) tails slurry, 35 % solids
   -> TAILINGS THICKENER (overflow water back to the plant) -> underflow 60 % solids
   -> TAILINGS FILTER (batch: fill, squeeze, blow, open) -> cake 15 % moisture -> belt
   -> TRUCK LOAD-OUT (400 t bin, clamshell gate) -> haul truck at a LOADOUT stake
   -> WASTE DUMP: back to the berm at a DUMP POINT stake, tip; the load settles down the face
 TRACK DOZER: pushes berms, cuts ramps to grade, pushes tipped loads over the crest
```

## The tailings thickener (`tailings_thickener`)
| | |
|---|---|
| Real | a high-rate thickener with a rake and a flocculant feedwell |
| Size | **15 m** across, 15 x 5 x 15 cells *(proposed: 1,200 t/day at 0.15 m² a t/day is 180 m²; real high-rate units 0.05-0.25)* |
| Rake drive | **11 kW**, the world's standard industrial low voltage *(proposed)* |
| In | tails slurry on steel pipe at the north face of its front cells (the plant's 50 t/h at 35 % solids) |
| Out | thickened tails at **60 % solids** *(proposed, the concentrate thickener's)* on pipe at the south face of its back cells; **overflow water** on a water pipe at its east face |
| Flocculant | left out (real dose about 20 g/t) |

- **The overflow water is real:** 1.86 L/kg of feed water in less 0.67 L/kg left in the underflow, about **1.2 L a kg of tails**
  (16.5 L/s at 50 t/h) back on a water pipe. It closes the overflow-water gap for the tails as the cell's decant does.
- With no pipe on its water face it counts the water as lost, as built mills do.

## The tailings filter (`tailings_filter`)
| | |
|---|---|
| Real | a **plate-and-frame filter press with 2.5 x 2.5 m plates**, about **80 chambers** *(proposed count)*, its feed pump and air blow |
| Rate | **60 t/h of solids** (open-pit.md): one real 2.5 m press does **1,800-3,000 t/day (75-125 t/h)** by the slurry; 60 t/h is a short press *(proposed)* |
| Cycle | **15 minutes**: fill 2, squeeze 3.5, blow 2, open and drop 7.5 *(a 2.5 m press sizing example's 15.5 min)* |
| Batch | **15 t of solids a cycle** (60 t/h x 15 min), dropped as cake at the end of each cycle |
| Cake | **15 % moisture** (open-pit.md; real 12-20 %) |
| Power | **150 kW** (open-pit.md, *proposed*): the feed pump 110 kW, hydraulics and plate shifter 25 kW, the blow-air compressor 15 kW *(proposed split)*, the world's standard industrial low voltage |
| Size | **6 x 6 x 24** cells *(proposed)*: the press on a steel floor 4 m up, the cake chute and a belt under it |
| Filtrate | the water squeezed out, about **0.49 L a kg of solids** (from 60 % to 85 % solids) on a water pipe |

- **Batch, not continuous** *(proposed: it is how a press works, and the cake drop is the thing to see and hear)*. The 15 t drops
  onto the belt under its chute or into a part touching its bottom face. With nowhere to drop, it holds the cake and reads FULL; the
  thickener backs up behind it.
- **The cake is `dry_tails`:** carried in kg on the belt; **1 m³ at 1.8 t** as a placed block (open-pit.md). So one cycle is
  **8.3 blocks**, a 91 t truck **50 blocks**, a 363 t truck about **200**.
- **What the tails carry:** dry tails hold no recoverable mineral in the game. Their assay is kept only for the readout.
- **PLC points:** RUN, RUNNING, CYCLE (pulse each cake drop), FULL.
- **Sound:** a hydraulic whine through the squeeze, a thud at each drop *(new sounds)*.

## The truck load-out (`loadout_bin` on `loadout_frame`)
| Part | Cells (W x H x L) | Real component |
|---|---|---|
| `loadout_frame` | 12 x 10 x 12 | The steel legs and bracing over a **drive-through lane 10 m wide and 9 m clear** *(the ultra truck is 9.8 m over its body and loads at 7.0 m)* |
| `loadout_bin` | 10 x 7 x 10 | The steel bin on top of the frame: **400 t** of dry tails (open-pit.md); 400 t at 1.6 t/m³ loose is 250 m³ of its 700 *(proposed)* |
| `loadout_gate` | 3 x 2 x 3 | The clamshell gate and chute under the bin's centre, hydraulic, **7.5 kW** *(proposed)* |

- **Fed** by a belt delivering onto the bin's top, from the filter, and by any part with a dry output touching its top: the gravity
  circuit's coarse rejects as well (open-pit.md).
- **Loading:** a truck standing with its body under the gate (its body's centre within 2 m of the gate) is filled at **2 t/s**
  *(proposed: a 91 t truck in 46 s, a 363 t truck in 3 min)* until it is full by mass or the bin is empty. A self-driving truck at a
  **LOADOUT** stake drives through forward and stops with its body under the gate *(proposed: the stake marks the gate, not the
  tail, at a load-out)*; it leaves when full, or after 60 s with nothing more to load.
- **Takes dry tails only** for loading out. Rock reaching it is refused: "dry tails only".
- **PLC points:** LEVEL (4-20 mA), FULL, LOADING.

## Waste dumps
- **A dump** is ordinary ground the player chooses (open-pit.md). The truck tips at a **DUMP POINT** stake at the crest.
- **Tipping:** the truck backs to the stake, or to the berm behind it (below), raises its body and the load goes out behind it by
  the settle rule. A load tipped against a berm lands on and over the berm and runs down the face, so the crest advances by about
  the load's width each few loads, as a real end-tipped dump does.
- **Moving the crest:** a DUMP POINT stake is moved by the player as the dump advances *(proposed; real dumps move their tip points
  every shift)*. A stake with no drop of at least 3 m within 10 m behind it reads "no crest" on the book and a truck tips at it on
  the flat (a **paddock dump**, *real practice*).
- **Dry tails, rock as muck, and soil** all tip; each settles by the one rule. A dump of dry tails stands at the same 37°.
- **What a dump is worth:** nothing more is recovered from it. Muck tipped on a dump (soil from overburden, the pit's rejects in the
  future) can be dug again and taken to the plant if the player chooses *(rehandle)*.

## Berms
- **A berm is any ridge of blocks along the crest** at least **mid-axle height of the truck** that tips there (US 30 CFR 56.9300(b)
  requires mid-axle height of the largest vehicle on the road):
  - the 100 t truck: wheel about 2.7 m, axle 1.35 m: **1 block** *(rounded down to whole blocks, proposed)*;
  - the ultra truck: wheel about 4 m *(unverified)*, axle 2 m: **2 blocks**.
- **A reversing truck stops against a berm** within 3 m behind its stake: its tail stops 1 m short of the berm's crest line
  *(proposed)*, as at a bumper. It then tips over it.
- **No berm:**
  - **a self-driving truck** will not back to a crest that has no berm: it stops at the stake before and reads **NO BERM** (and
    BLOCKED in dispatch after 30 s) *(proposed: real autonomous trucks tip only at surveyed, bermed tip points)*;
  - **a player-driven truck** backing past the edge, its rear axle over a drop of 3 m or more, **goes over the crest**: it falls by
    gravity, lands, and is **wrecked** at the bottom: it breaks into its item and its load (tipped where it lands by the settle rule),
    and its driver takes the fall *(proposed: the item is kept so a mistake costs time, not the truck)*.
- **Making a berm:** the dozer pushes one up (below), or a player places blocks. A windrow of dry tails tipped along the crest from a
  truck driving slowly with its body up is left out *(proposed)*.
- **Survey:** the survey book reads "no berm" on a DUMP POINT leg without one *(proposed)*. Berms along haul roads are left out.

## The track dozer (`track_dozer`)
| | |
|---|---|
| Real | a **70 t** crawler dozer of the D10 class: **447-449 kW** net (600 hp), semi-U blade |
| Blade | **4.9 m wide, 2.1 m high, 18.5 m³** (open-pit.md); the game blade is **5 x 2 blocks** and carries **18 blocks** of loose ground or muck |
| Size L x W x H | about **8.8 x 4.9 x 4.6 m** with the blade *(unverified)* |
| Speed | **11.7-12.7 km/h** forward top (sources disagree), about **14.5-15.8** reverse; dozing at **3.5 km/h** *(proposed)* |
| Fuel | about **1,200 L** tank *(unverified)*; burns by work as `PitFuel`, dozing at 80 % of rated power *(proposed)* |
| Mass | 70.2 t |

- **An entity** like the loader (`PitVehicleEntity`), driven by a player, or running by itself in an area marked by two DOZE AREA
  stakes (Tim, 2026-11-23: below).
- **The blade has three positions**, stepped with jump:
  - **CARRY** (default): the blade at track level. Driving forward, it takes the loose ground and muck in the 5 x 2 cells in front of
    it, at and above its tracks, into the blade's load, up to 18 blocks. Past 18 the extra spills off the blade's ends as two windrows
    by the settle rule, as a real blade spills.
  - **CUT:** the blade 1 block below the tracks. Driving forward, it also cuts the layer under the blade's front: soft ground and
    muck. It steps down 1 block **each N metres** set on its screen: **level, 5 %, 8 % or 10 %** grade *(proposed: 1 block in 20, 12,
    10 m)*. So it cuts a ramp to a haul road's grade, and on level it cuts a bench flat.
  - **RAISED:** travelling; it takes nothing.
- **Dropping the load:** reversing with a load in CARRY leaves it in front where it stood, by the settle rule. Pushed over a crest,
  the load falls and runs down the face.
- **Berms:** in CARRY with a load, driving along a crest with the crest on its right or left (the blade angled, real) drops a **berm
  1 block high** along the crest at 1 block a metre, or **2 high** with the screen set to "berm 2 m" *(proposed)*, until the load is
  spent.
- **Hard rock:** standing hard rock in the blade's way stops it: "hard rock: blast it first". The rear ripper is left out
  (open-pit.md).
- **Weight:** it climbs at most a 1-block step, as the trucks, and a ramp of up to **40 %** *(proposed; real dozers climb about 60 %
  on dry ground)*.
- **Sound:** the diesel loops; a track clatter *(new)*.

## Left out (to ROADMAP)
- The stack's water content and dust; rain on the stack (compaction and a simple slump rule: Tim, 2026-11-23, below).
- Flocculant and its make-up plant.
- A radial stacker conveyor building the stack without trucks.
- Rehandle and reclamation; topsoil.
- The dozer's ripper (an autonomous dozer: Tim, 2026-11-23, below).
- Dump failure and crest collapse under a truck.

## Game tests to add
- `pit_thickener`: tails slurry in, 60 % underflow and overflow water out at the rates above.
- `pit_filter_cycle`: one cycle drops 15 t of `dry_tails` onto a belt after 15 minutes (test ticks set short); with nowhere to drop it
  reads FULL.
- `pit_loadout` (from open-pit.md): dry tails from the filter fill a 91 t truck under the gate in about 46 s; rock is refused.
- `pit_berm` (from open-pit.md): a self-driving truck backs to a 1-block berm and tips over it; with no berm it stops and reads NO
  BERM.
- `pit_over_crest`: a player-driven truck backed over a 5 m crest without a berm is wrecked at the bottom, dropping its item and
  load.
- `pit_dump_advance`: 10 loads tipped at one stake advance the crest and stand at 37°.
- `pit_dozer_cut`: the dozer in CUT at 10 % cuts a 20 m ramp down 2 blocks in sand.
- `pit_dozer_berm`: the dozer drops a 1-block berm along a crest.
- `pit_dozer_hard`: stone in front reads "hard rock: blast it first".
- Client film `pit_dry_stack`: filter, load-out, a self-driving truck to a bermed dump; the dozer pushes up the berm.

## The wave plan
- **P4a:** the tailings thickener, the filter, `dry_tails`, the load-out; the LOADOUT stake drives through.
- **P4b:** dumps, berms and NO BERM, going over the crest; the track dozer.

## As built (2026-11-09, decided while Tim was away: [../questions/p4-dry-stack.md](../questions/p4-dry-stack.md))
- **All of P4a and P4b is built** to the figures above, on Wildcatter 0.5.1. What building changed or added:
  - **The underflow is tails slurry marked with its solids** in its assay (no thickened-tails product in Wildcatter 0.5.1); the press
    refuses unmarked tails (NOT THICKENED). The thickener holds 20 t of underflow so it works on while the press stands open.
  - **The cake drops into a load-out bin under the press, a container under its chute, or onto open ground**; belts are left out.
  - **The gate is drawn in the frame:** two parts, `loadout_frame` and `loadout_bin`; the gate's 7.5 kW is the frame's motor.
  - **The LOADOUT stake stands at the lane's mouth**; the truck finds the frame within 16 m on along the line and stops with its body
    under the gate. Filled, it leaves out of the far side and round a balloon loop (about 40 m beyond the gate, 25 m to one side).
  - **A berm stops a truck driven by hand:** reversing, it does not step up onto a ridge with a drop behind it.
  - **The dozer's settings** are on the sprint key (no screen): the grade in CUT, the berm's height in CARRY. It turns on 4 m
    *(proposed)*. At a crest it stops and pushes its load over.
  - **Not built:** the press's and load-out's own PLC points; the new sounds; dry tails on belts.
- **Scenes:** `pit_thickener`, `pit_filter_cycle`, `pit_loadout`, `pit_loadout_haul`, `pit_berm`, `pit_no_berm`, `pit_over_crest`,
  `pit_dump_advance`, `pit_dozer_cut`, `pit_dozer_berm`, `pit_dozer_hard`, `pit_dozer_push`; the client film `pit_dry_stack` (written).

## As built: running tracks, the body on the ground, and spreading (heavy-iron-fixes, 2026-11-22)
Episode 4 of Heavy Iron films the dozer (../questions/heavy-iron-fixes.md, 6-8).
- **The tracks run:** a row of grousers along the ground run and the top run of each track (`tools/models/pit_vehicles.py`), drawn at a
  **0.25 m** pitch (the class's shoes are about 0.26 m, *unverified*), a pixel proud of the tread. As the dozer rolls the ground run
  slides back and the top run forward, a pitch and round again, at its own speed (the same distance its idlers and sprockets turn
  through), so the tracks are seen to run.
- **On the ground under it:** the body is drawn pitched by the ground under the front and rear of its tracks and rolled by the ground
  under its left and right tracks, each from the highest ground there (a track bridges a hollow), eased over a few frames as a block
  edge passes under it. Its box is still level.
- **Spreading (SPREAD, CARRY's third setting on the sprint key):** with a load, a metre at a time, each of the blade's five columns
  fills the ground its tracks stand on in front of the blade where that is open (a hollow, or its lift's edge over lower ground): the
  dozer lays its load as a **1 m lift** and drives on along it. Pushing a truck's tipped load across a lift spreads it into the lift.
  Real dry stacks are spread in lifts of about 0.3-0.5 m and compacted *(unverified)*; a block is the game's thinnest lift.
- **No compaction** (left out): the real one is a number of roller or track passes to a density; the game has no density for a
  block, nothing that a firmer stack would change, and dry tails is a plain falling block.
- **Game test:** `pit_dozer_spread` (18 blocks laid 5 wide at the lift's level from its edge, the dozer driving on along it).

## Tim's decisions of 2026-11-23: the dozer runs itself, cake is compacted, the press drops full batches, the bin shows its level
Decided by Tim for Heavy Iron episode 4 (`docs/questions/films-hi04.md`, "Decided by Tim 2026-11-23"). They replace "It does not run
itself", "No compaction" and the press's fill-over-the-pressing-time above.

### 1. Dozer autopilot (a marked area, a start/stop, its own diesel)
- **Real counterpart:** semi-autonomous dozing systems at large mines, such as Caterpillar's **Command for dozing**: a supervisor
  draws the area and the push direction, the dozer works passes across it by itself (blade down to push, back to the start, over to
  the next slot), and stops for anything in its way; an operator can take it over. (Named here only; the videos name no brand.)
- **The area: two `DOZE AREA` survey stakes** (a new kind on the survey stake, stepped with an empty hand after PARK, a yellow flag)
  at opposite corners mark a rectangle, as the trucks' and loaders' stakes mark their work. Click the two stakes into the survey book,
  then click the dozer with the book: it **starts** on the area. Click it again with the same book: it **stops**. A player sitting in
  its cab takes it over and it stops (it is still driven by hand as before).
- **How it works the area** *(proposed)*: it pushes the way it faced when started, squared to the nearest compass direction. The area
  is cut into lanes a blade wide (5 m) across that direction. Each lane: from the near end, blade set to **SPREAD**, it drives to the
  far end at dozing speed, taking loose cake and ground into its blade and laying it where the ground in front of the blade is open
  (a hollow, or a lift's edge inside the area), as SPREAD does by hand. At the far end it leaves anything still on the blade in front
  (as reversing in CARRY does), raises the blade, reverses straight back down the lane it has just pushed clear, and crosses over to
  the next lane's start, squaring up on its tracks; the lanes go across and back (0, 1, ..., last, ..., 1, 0). Its readout says "AUTO: lane n of m, pass p" and what it is doing.
- **It stops or waits:** for something in its way (a mob, a player not in spectator, another machine) within 3 m ahead of the blade
  or behind it while reversing it waits, "AUTO: waiting: something in its way"; hard rock or a crest ends the lane; out of fuel or
  stuck (no headway for 10 s) it stops: "AUTO STOPPED".
- **Power:** its own diesel, burnt by its work as when driven; it idles between moves while the autopilot runs.

### 2. Compaction (loose and compacted cake)
- **Real counterpart:** filtered tails are placed in thin lifts and compacted by rollers or dozer track passes to a target density;
  a stack of loose, uncompacted cake is weak and its faces slump, and a lift placed on an uncompacted one loads it before it can
  carry the weight. (Practice; lifts of about 0.3 m and the density target are *unverified* here.)
- **In the game** *(proposed, kept simple)*: `dry_tails` has two looks, **loose** (as the press, a truck, a pour or the dozer's
  blade places it: paler, in clods) and **compacted** (darker, smooth, with track marks). **A dozer driving over loose cake
  compacts it**, a pass under its tracks, by hand or on autopilot; nothing else does.
- **The slump rule:** loose cake standing on **loose** cake, at the top of an edge with a drop of 2 m or more beside it, slides off
  the edge and comes to rest below by the settle rule (37°). On compacted cake, or on anything else, it stands as before. So a load
  tipped at the edge of an uncompacted lift, or a second lift laid on an uncompacted one, slumps at the edge; on a compacted lift the
  same load stands. Lifts and piles less than 2 m high at an edge never move.

### 3. The press drops full batches
- **Real counterpart:** a plate press fills until its chambers are full (its feed pressure up, the feed flow down), then squeezes,
  blows and opens; the cake comes out in drops, one batch at a time, and a press on a short feed simply fills longer.
- **In the game:** the fill phase runs its 2 minutes and then **holds** ("filling: waiting for a full batch") until the chambers
  hold the full **15 t**; then the timed squeeze (3.5), blow (2) and open-and-drop (7.5). Only a full 15 t cake drops, at the end of
  its cycle, into the bin: the output comes in **pulses**, not a steady rate. At 60 t/h of thickened tails or more the cycle is its
  15 minutes; on the plant's 50 t/h it is about 18. (It was: filling over the whole pressing time and dropping whatever it held,
  12.5 t on the plant's feed.)

### 4. The bin's level drawn on the bin
- **Real counterpart:** bins carry level transmitters (the bin's LEVEL point, 4-20 mA, above) and often a level gauge or marks.
- **In the game:** the load-out bin draws the dry tails inside it at their level (the static full-looking fill in its model is
  gone), and a **sight window** on each wall with the fill risen in it and a mark every 100 t. The server sends the level whenever
  it moves by a 100th of the bin.

### Game tests
`pit_dozer_auto` (the autopilot works its lanes inside the stakes, spreads a tipped pile, compacts the lift, stops on the book
again), `pit_cake_slump` (a load at the edge of a loose lift slides off; on a compacted lift it stands; a dozer pass compacts),
`pit_filter_batch` (on a short feed the press holds at its fill and drops only a full 15 t), `pit_bin_level` (the bin sends its
level to the client as the cake drops and the truck loads).

## Sources
- R. Kaswalder, filter press paper (ACG Paste 2017 proceedings): one unit with 2,500 x 2,500 mm plates about 1,800 t/day (average
  filterability) to about 3,000 t/day.
- A 2.5 m press maker's sizing example (ACG proceedings): filling 2.0, compacting 3.5, blowing 2.0, non-process 8.0 min.
- UWA/ACG dry-stack design paper: cake moisture under 20 wt %.
- US 30 CFR 56.9300(b): berms at least mid-axle height of the largest self-propelled mobile equipment that usually travels the roadway.
- D10-class dealer and spec-database data: 70,171 kg, C27 at 600 hp (447 kW) net forward, semi-U blade about 18.5 m³, top speed 11.7-12.7 km/h
  forward (the sources disagree).
