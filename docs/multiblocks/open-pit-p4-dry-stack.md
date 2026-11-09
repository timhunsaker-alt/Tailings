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

- **An entity** like the loader (`PitVehicleEntity`), driven by a player. It does not run itself *(proposed; real autonomous
  dozers exist and are left out)*.
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
- Compaction and the stack's water content, stability and dust; rain on the stack.
- Flocculant and its make-up plant.
- A radial stacker conveyor building the stack without trucks.
- Rehandle and reclamation; topsoil.
- The dozer's ripper; an autonomous dozer.
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

## Sources
- R. Kaswalder, filter press paper (ACG Paste 2017 proceedings): one unit with 2,500 x 2,500 mm plates about 1,800 t/day (average
  filterability) to about 3,000 t/day.
- A 2.5 m press maker's sizing example (ACG proceedings): filling 2.0, compacting 3.5, blowing 2.0, non-process 8.0 min.
- UWA/ACG dry-stack design paper: cake moisture under 20 wt %.
- US 30 CFR 56.9300(b): berms at least mid-axle height of the largest self-propelled mobile equipment that usually travels the roadway.
- D10-class dealer and spec-database data: 70,171 kg, C27 at 600 hp (447 kW) net forward, semi-U blade about 18.5 m³, top speed 11.7-12.7 km/h
  forward (the sources disagree).
