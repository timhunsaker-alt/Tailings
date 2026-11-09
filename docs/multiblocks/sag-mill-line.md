# P7b: the SAG mill line (SAG mill, pebble crusher, ball mill, cyclones)

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** The choices are listed in
[../questions/specs-tailings.md](../questions/specs-tailings.md) (P7 section).

The grinding line of the pit-sized plant, at **1,000 t/h** (primary-gyratory-stockpile.md, "The line's rate"). It takes the
stockpile's crushed rock and makes fines slurry at 150 µm for the pit flotation row ([pit-flotation.md](pit-flotation.md)). It does
what the 50 t/h plant's jaw, screen, cone and ball mill do (crushing-circuit.md), in one big closed circuit: an **SABC** circuit
(SAG mill, ball mill, pebble crusher).

**Real counterparts:**
- A **32 ft x 15 ft (EGL) SAG mill** at about 8.2 MW: the size of the first gearless SAG mills, at Chuquicamata (1986).
- A **24 ft x 36 ft** overflow ball mill on a twin-pinion drive, **2 x 5.25 MW** *(the twin-pinion drive size Red Chris's SAG mill runs; the ball mill's size is scaled, proposed)*.
- A cone pebble crusher in the SAG's recycle, and a cluster of 660 mm hydrocyclones on a cyclone feed pump.
- Wills & Finch, *Wills' Mineral Processing Technology*, 8th ed.: autogenous and semi-autogenous grinding, the Bond work index.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

```
 SAG feed belt (1,000 t/h) + water -> SAG MILL -> trommel: -12 mm slurry -> CYCLONE FEED PUMP BOX
                                           | +12 mm pebbles (about 20 %)          ^           |
                                           v                                      |           v
                                 heavy belt -> PEBBLE CRUSHER -> back to the SAG feed belt    CYCLONE CLUSTER
                                                                                  |   underflow   | overflow 150 µm
                                                                     BALL MILL <--+               v
                                                                         | discharge (pipe)   fines slurry -> pit flotation
                                                                         +--> CYCLONE FEED PUMP BOX
```

## Size: 1,000 t/h
- **Power check (Bond's law, as crushing-circuit.md):** W = 10 Wi (1/√P80 − 1/√F80) kWh/t.
  - **Ball mill:** from the SAG's transfer size, T80 about 2 mm, to 150 µm at Wi 15: 150 x (1/√150 − 1/√2000) = **8.9 kWh/t**, so
    **8.9 MW** at 1,000 t/h. The 10.5 MW mill covers it.
  - **SAG mill:** real SAG mills take 5-12 kWh/t from a primary product to a few mm. At 1,000 t/h the 8.2 MW mill gives **8.2 kWh/t**.
  - Total grinding about **17 MW drawn**, **18.7 MW installed**, inside open-pit.md's "a SAG mill line draws 10-20 MW".
- **A block a time:** 1,000 t/h is a bank block (2.7 t) every 9.7 s, through the whole line.
- **Slurry *(proposed, typical values)*:** the SAG at **75 %** solids (0.33 L of water a kg); the ball mill discharge at 70 %; the cyclones
  at a **250 %** circulating load; the overflow at **35 %** solids (1.86 L a kg), **517 L/s** of water, about **0.62 m³/s** of slurry.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `sag_mill` | 11 x 11 x 14 | A **9.75 m (32 ft) x 4.6 m (15 ft EGL)** SAG mill on its trunnions, with its feed chute and the **trommel** on its discharge end. **2 x 4.1 MW** on a twin-pinion drive *(proposed: dual-pinion drives reach 9-12 MW)*: two motor boxes at the world's standard medium voltage on its west face. Charge about 28 % full with 12 % balls; 75 % of critical speed *(proposed)* |
| `pebble_crusher` | 4 x 5 x 4 | A cone crusher of the 300 kW class *(unverified)*, closed-side setting 12 mm, crushing the SAG's pebbles. Medium-voltage motor box *(proposed)* |
| `ball_mill_large` | 9 x 9 x 16 | A **7.3 m (24 ft) x 11 m (36 ft)** overflow ball mill, **2 x 5.25 MW** twin pinion *(proposed: about 10.5 MW, scaled from an 18 x 28 ft mill at 4.1 MW by D^2.5 L)*. Two medium-voltage motor boxes on its west face. Cyclone underflow in at its feed end; discharge out on pipe |
| `cyclone_feed_pump` | 4 x 4 x 5 | The sump and a **600 kW** slurry pump *(proposed: 1.0 m³/s of slurry at about 25 m head)*, on a medium-voltage motor. It takes the SAG's trommel undersize (touching) and the ball mill's discharge (pipe), and feeds the cluster by pipe. Water off a water pipe on any face |
| `cyclone_cluster_large` | 6 x 6 x 6 | **Twelve 660 mm hydrocyclones**, ten running and two spare, on a ring manifold, cut point **150 µm** *(proposed: about 360 m³/h each)*. Underflow by launder into the ball mill it touches; overflow out on pipe |
| `heavy_belt_conveyor`, `heavy_conveyor_drive` | as primary-gyratory-stockpile.md | The SAG feed belt; the pebble belt to the crusher and back |

## Nozzles and faces (each part drawn facing north; main cell at the north-west bottom corner)
- **SAG mill:** fed by the heavy belt delivering onto its feed chute (north end, top); water off a water pipe on any face; the trommel's
  undersize drops into the `cyclone_feed_pump` touching its south (discharge) face; the trommel's oversize (pebbles) out of its east
  face onto a heavy belt.
- **Pebble crusher:** fed by a belt over its top; its product out of its south face onto a belt back to the SAG feed belt.
- **Cyclone feed pump:** SAG undersize by touching; the ball mill's discharge in on pipe at its north face; water off a water pipe; out
  on pipe to the cluster from its south face.
- **Cyclone cluster:** feed in on pipe at its north face; overflow (fines slurry) out on pipe at its south face; underflow into the
  ball mill it touches at the mill's feed end.
- **Ball mill:** underflow in by touching; discharge out on pipe at the south face of its back cells.
- **Pipe size:** 0.62 m³/s of overflow and about 1 m³/s of cyclone feed are far over a small pipe: see Decision P7-6 (a large-bore
  slurry line in Wildcatter).

## How it runs
- **Each part runs its own motors** while it has feed and somewhere to put its product, as the 50 t/h plant's parts do; its rate follows
  its motors' output, up to its share of 1,000 t/h. There is no controller; the belt weigher's setpoint (P7a) sets the feed.
- **The SAG mill:** it turns the stockpile's crushed rock into **sand-size slurry** (the trommel's -12 mm) and **pebbles** (+12 mm,
  the hard, round middle sizes a SAG cannot break). Pebbles are **20 %** of new feed *(proposed: real 10-30 %)*, so about 200 t/h goes
  round the pebble crusher.
  - **Starting:** a 8.2 MW mill does not start across the line: its twin motors start through Wildcatter's soft starter or VFD
    (soft-starter.md, vfd.md), or the breaker trips *(proposed; real big mills start on a liquid resistance starter or a VFD)*.
  - **Overload:** fed faster than its motors grind, its charge rises; over **35 %** filling *(proposed)* it reads OVERLOAD and the
    belt weigher's setpoint falls back by 10 % until it clears *(real SAG control holds the mill's weight on its bearings)*.
- **The pebble crusher** runs while it has pebbles; with it off, the pebbles go straight back to the SAG and the SAG's rate falls by
  **15 %** *(proposed: the real gain from a pebble crusher is 10-25 %)*.
- **The ball mill** takes the cyclones' underflow, as the small one does.
- **The cyclones** split at 150 µm: overflow to fines slurry, underflow back to the ball mill.
- **Liberation** at fines is the same **90 %** as the small plant's (crushing-circuit.md). The slurry carries its assay.
- **What each part reads when it stops:** NO FEED, NO WATER, NOT BUILT, OVERLOAD, "its output has nowhere to go", NOT STARTED (a mill
  motor not through a starter).
- **PLC points:** each mill's RUN, RUNNING, STALLED, AUTO, AMPS and a **WEIGHT** (4-20 mA over its rated charge); the pump's RUN,
  RUNNING, AMPS; the cluster's PRESSURE (4-20 mA, its feed pressure).
- **Removing a part** stops the flow through it; what it holds drops when it is broken (R1).

## Power
- **About 19 MW installed, 17-18 MW drawn**, with the crusher, feeders and belts of P7a about 1.5 MW more. A real line takes it from
  the grid at sub-transmission voltage through its own main substation. Here it is a Wildcatter load like any other: it needs a
  generating plant or a grid tie of that size (Wildcatter's gas turbines, combined cycle or steam plant) *(Decision P7-5)*.

## Left out (to ROADMAP)
- Liner and media wear, steel ball make-up, the tramp-iron magnet on the pebble belt.
- A real size distribution through the SAG (P80 only, as the small plant).
- A gearless (ring-motor) drive: real above about 18 MW; this mill is twin pinion.
- The mill's critical-speed and lifter design.

## Game tests to add
- `p7_sag_grind`: crushed rock on the feed belt at 1,000 t/h (test ticks short) comes out as fines slurry with its assay; pebbles go
  round the crusher.
- `p7_sag_overload`: fed at 1,500 t/h the SAG reads OVERLOAD and the setpoint falls back.
- `p7_sag_start`: the SAG's breaker trips started across the line; it starts through a VFD.
- `p7_pebble_off`: with the pebble crusher off the SAG's rate falls by 15 %.
- `p7_cyclones`: the cyclones' overflow is fines slurry at 35 % solids; the underflow goes to the ball mill.
- Client film `p7_grinding`: the pile, the SAG, the ball mill and the cyclones running.

## The wave plan
- **P7b**, after P7a (the stockpile) and a Wildcatter large-bore slurry line, if Tim takes Decision P7-6.

## Sources
- AG/SAG update survey (2015): the first gearless SAG mills were two **32' x 17' (15' EGL), 8,200 kW** mills for Chuquicamata (1986);
  dual-pinion drives limited to about 9-12 MW; gearless above about 18 MW.
- Kennecott Copperton fourth line (OneMine): a 34 ft x 15 ft SAG with two **18 ft x 28 ft** ball mills on **4.1 MW** synchronous motors.
- IMPC 2024 proceedings: Red Chris SABC, **2 x 5.25 MW** on the SAG, **2 x 6.7 MW** on the ball mill; a 2,150 t/h case sizing a 38 x 22 ft
  SAG at about 16 MW.
- Wills & Finch (as crushing-circuit.md): Bond's law; SAG specific energies *(proposed range)*.
