# Haulage: mine rail, trains and the traction rectifier (APPROVED by Tim 2026-11-05, R2)

**Twin-track rail haulage** between the tunnel boring machine's loading point and the dump station. Loaded trains run out on one
track and empty trains come back on the other. The track carries its own power and is safe to walk on. The machine is in
[tunnel-boring-machine.md](tunnel-boring-machine.md), the dump in [dump-station.md](dump-station.md).

**Real counterparts:**
- Rail-haulage mines with twin track (SME *Mining Engineering Handbook*, 3rd ed., 2011, haulage chapter).
- Granby side-dump cars on 900 mm gauge.
- **Ground-level power supply**, as Alstom's APS on the Bordeaux tramway: a conductor rail between the running rails, shrouded and
  flush, so the track is safe to step on.
- Rack railways for the decline: the Pilatusbahn's Locher rack climbs 48 %.

**Decided with Tim:**
- **2026-11-02:**
  - Tailings' own mine rail and cars, not vanilla minecarts;
  - 1-block gauge;
  - the safe powered track;
  - the 1.5 m³, 3 t Granby car;
  - 10 cars to a locomotive.
- **2026-11-03:**
  - a car holds 9 stacks;
  - fed track also feeds lights and signals;
  - a thrown switch sends a train clockwise;
  - trains hurt what they hit, set in `config/tailings-train.properties`;
  - chunk tickets.
- **2026-11-05:**
  - **every car is an entity:** trains glide smoothly as linked entities, like Create's trains, and are no longer data on the
    rail blocks;
  - the rectifier on Wildcatter's DC tier (R5): **a Tailings DC source on Wildcatter's DC solver** (decided with Tim, R5 plan);
  - **couplings never break in R5** (a loaded 10-car train needs about 215 kN at its first coupling to climb the 1-in-2 rack
    decline, more than a real mine-car coupling); breaking couplings wait, listed in ROADMAP Known gaps;
  - **trains placed before R5** (stored on the rail blocks) become their entities, with their loads, where they stand.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* came from the
first draft and is still to be checked.

```
 surface:  DUMP STATION (dump-station.md) -> ore bins -> belt -> crushing circuit
              |  ||   loaded track in, empty track out; a switch at each end loops trains from one track to the other
 decline:     \\  1-in-2 rack track on slope blocks, a walkway between
               \\
 level Y:       ======== flat drive ======== mine car under the conveyor gantry's tail (the loading point) <- TBM back-up
```

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `mine_rail` | 1 x 1 x 1 per track | Heavy 30 kg/m mine rail on steel ties, its own block (not vanilla rail), at a 1-block gauge (about 900 mm). It has a **shrouded, segmented conductor rail** set flush between the running rails, and a rack on the decline. The running rails are the return |
| `mine_switch` | 1 x 1 x 1 | A turnout. Thrown (by redstone or by hand), it sends a train down the clockwise branch |
| `traction_rectifier` | 2 x 3 x 2 | A mine traction substation: **480 V three-phase in, 250 V DC out** to the conductor rail, **300 kW** *(unverified)*. It is placed beside the track and feeds the track it touches and all track joined to it, switches included (R1) |
| `mine_locomotive` | 1 x 2 x 3 | An **8 t** mine locomotive with **two 37 kW** traction motors, a rack pinion, a collector shoe on the conductor rail, and a headlight *(unverified)* |
| `mine_car` | 1 x 2 x 2 | A Granby side-dump car, **1.5 m³, 3 t of rock**, about 1 t empty *(proposed)*. It carries any mix of rock and ore, each counted. It holds **9 stacks (576 items)** for play (decided 2026-11-03); the 3 t stays the real counterpart |
| `block_signal`, `mine_light` | 1 x 1 x 1 | A block signal and a tunnel lamp, lit from a neighbouring fed rail |

## The trains (entities, 2026-11-05)
- **What a train is:**
  - a `mine_locomotive` entity and up to 10 `mine_car` entities, linked by couplings 0.5 m long *(proposed)*;
  - each sits on the rail at its real length, and the cars follow the locomotive's path exactly, through curves, switches and
    the change of grade;
  - each car is drawn and hit where it is. Nothing of the train is stored on the rail blocks.
- **Placing and coupling:**
  - right-click a mine rail with a locomotive or car item to place it;
  - sneak and right-click with an empty hand to couple a car to the one in front;
  - the TBM's gantries couple the same way (tunnel-boring-machine.md).
- **Speed follows power** *(proposed, from the motors)*. With F = m g (sin θ + 0.01 rolling), the speed is P / F, capped at
  **3 m/s** on the flat:

  | Train | Mass | Up the 1-in-2 decline, 74 kW |
  |---|---|---|
  | Loaded, 10 cars | 48 t | 0.34 m/s |
  | Empty, 10 cars | 18 t | 0.92 m/s |
  | Locomotive alone | 8 t | 2.06 m/s |

  - **Down the decline:** speed is held to 1 m/s on the rack brake.
  - **On the flat:** a loaded train draws about 14 kW to roll at 3 m/s.
  - A train starts and stops at 0.3 m/s² *(proposed)*.
- **Where a train goes** *(proposed)*:
  - The locomotive drives in its set direction while its section is fed and the block ahead is clear.
  - **At the loading point** (a car under the conveyor gantry's tail) it stops each car under the tail in turn until the car is
    full, then creeps one car on.
  - **At the dump** each car tips on the move.
  - At the end of the track it waits, then reverses. Right-clicking the locomotive sets its direction and starts or holds it.
- **Signals:** a block system keeps trains one section apart. A section runs from one `block_signal` to the next.
- **Power off:** past the last fed section a train brakes to a stop and holds on the rack.
- **Couplings:** they do not break in R5 (decided with Tim). Later: one breaks if the train pulls past its rating, and the cut
  cars roll back down the decline unless their brakes are set.
- **Chunks:** a running locomotive refreshes the `tailings:runner` ticket on the chunks its train covers and the one ahead (R1).

## Power: the traction rectifier on Wildcatter's DC tier (R5)
- **Its AC side:** a Wildcatter 480 V load whose draw follows the trains.
- **Its DC side:** on Wildcatter's DC solver (`wildcatter/docs/multiblocks/dc-bus.md`):
  - it holds **250 V** with an internal resistance of about 2 % at its rating *(proposed)*;
  - each locomotive is a DC load drawing its tractive power through its shoe (296 A at 74 kW);
  - the conductor rail is a DC conductor, about **0.02 Ω/km** with its return *(proposed; real steel conductor rail is
    0.007-0.02 Ω/km)*. A long drive therefore needs a second rectifier further in.
- **Overload:** the rectifier's DC breaker trips at 1.25 times its rating for 1 s, as Wildcatter's rectifier does. Its readout
  shows the track's volts, amps and trains.
- **Safe track:**
  - a fed section is live along its whole length, so lights and signals can draw from it;
  - the conductor is shrouded and flush, and a player, mob or item on the track never gets a shock;
  - a train hurts a player or mob it hits, 8 damage by default (`config/tailings-train.properties`).
- **Decided with Tim (R5):** Wildcatter's rectifier holds a current setpoint (it was built for electrolysis), so Tailings builds its
  own **250 V voltage source** on Wildcatter's DC solver: an EMF behind its internal resistance, its minus on the running rails
  (earth), with its own DC breaker. Its AC side is a Wildcatter variable load whose draw follows the DC watts, at 97 %
  efficiency *(proposed)*.

## Nozzles and faces
- **Motor box (the rectifier's AC side):** on the west face of its main cell, at **480 V**.
- **The rectifier's DC side:** feeds every mine rail or switch touching any of its cells, and the track joined to it.
- **Lights and signals:** draw from a neighbouring fed rail.

## Added while planning R5
- **Where a train runs** is worked out from the rails themselves: from a rail, the next is the one beside it, level or a block up or
  down, whose rail surface meets it, so the flat drive, the 1-in-2 decline and the joins between them run both ways.
- **The end of track** is the end the locomotive sees within its braking distance: it stops, waits 5 s *(proposed)* and reverses.
  Track broken under a train, or ahead closer than it can stop, derails it.
- **Spacing (R9, Tim 2026-11-06):** real length: centres 3 m from the locomotive to its first car, 2.5 m from car to car. A car couples within 3 m of its place behind the train; placed by hand it can stand anywhere near that.

## Added while building R5b
- **The conductor rail on Wildcatter's solver:** each rail or switch is one node, joined to its neighbours at 0.02 ohm/km; rails a
  block up or down the grade are joined by a span. The running rails are the return (earth).
- **A locomotive's power** is what the track's volts give it: its 74 kW at 250 V, falling with the square of the volts *(proposed)*;
  it draws only what it uses (holding the grade, rolling, speeding up) through the rail under it.
- **Fed track:** a rail is fed while its volts are over half the rating *(proposed)*; lights and block signals beside it go dark when
  it is not.
- **The rectifier's AC side:** a Wildcatter variable load at power factor 0.95 *(proposed)*.

## Breaking and removing
- **Breaking a track piece** leaves the rest. A train reaching the gap derails: the cars at the gap stop and drop their loads as
  items, and the train reads DERAILED until it is put back on the rail *(proposed)*.
- **A car or locomotive** is broken by sneak-attacking it with an empty hand. It drops its item and its load.
- **Breaking the rectifier** leaves its track dead.

## Left out (ROADMAP)
- Battery locomotives for unpowered track.
- Bottom-dump cars.
- Shaft and hoist haulage.
- Automatic train dispatch beyond the shuttle above. In-game PLCs are in ROADMAP Later.

## Game test
`haulage` (Fabric client game test):
- **Setup:**
  - a loop of twin track with a switch at each end;
  - a traction rectifier fed at 480 V;
  - a locomotive and three cars, the first car loaded by a hook;
  - a dump station at one end.
- **Checks:**
  1. The train runs as four entities and keeps its spacing through the thrown switch.
  2. The rectifier's DC amps rise while it climbs a 1-in-2 test grade, and its speed there is about 1.2 m/s (14 t: the locomotive, three cars and one load).
  3. Each car tips into the dump on the move.
  4. A broken track piece ahead derails it.
  5. A player stepping on fed track takes no shock, and the train hurts a mob it hits.
- **Screenshot:** the train on the decline.
