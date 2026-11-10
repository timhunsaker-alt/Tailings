# Bottom-dump cars over a grizzly

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed wave **L3**. The choices are listed
in [../questions/specs-tailings.md](../questions/specs-tailings.md) (Later section).

ROADMAP "Later": bottom-dump cars over a grizzly. Today a train tips each Granby car **sideways** as a ramp lifts its dump wheel
(dump-station.md), into a pocket **beside** the track. A **bottom-dump car** opens doors in its floor as it rolls over a **track
grizzly**, and its load falls **straight down** through the rails into a pocket **under** the track. Real mines use them to unload a
long train on the move into a bin under the track, and they suit a pocket at the bottom of a shaft ([shaft-and-hoist.md](shaft-and-hoist.md)).

**Real counterparts:**
- Bottom-dump mine cars with clamshell or drop-bottom doors, opened on the move by a cam rail beside the track and closed by a
  closing ramp after the dump.
- A rail grizzly: steel bars on the bridge over the pocket, under the rails *(real gaps 300-400 mm)*, with a hydraulic rock breaker for
  oversize.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

## Parts
| Part | Size | Real-world basis |
|---|---|---|
| `bottom_dump_car` | 1 x 2 x 2 (as `mine_car`) | A bottom-dump car, **1.5 m³, 3 t** of rock, as the Granby car it replaces *(proposed: the same size so trains mix)*. Holds **9 stacks** (576 items), as the Granby car |
| `track_grizzly` | 1 x 1 x 4 | A rail bridge over a pocket with a **300 mm** bar grizzly between and under the rails; mine rail runs over it as over any rail |
| `dump_cam_rail` | 1 x 1 x 4, beside the grizzly | The cam rail that opens each car's doors as it passes, and the closing ramp at its far end |
| `dump_pocket` | as dump-station.md | The same pocket, set **under** the track grizzly instead of beside the track |

## How it runs
- **Dumping on the move:** a bottom-dump car rolling over a `track_grizzly` beside a `dump_cam_rail` drops its whole load into the
  pocket under the grizzly, at the train's speed *(proposed: all of it while the car is over the 4 m grizzly)*. The closing ramp shuts
  its doors as it leaves.
- **A full pocket:** a car whose load the pocket cannot take does not open; the train stops with that car over the grizzly, as the
  side-dump train waits (R4c).
- **Mixed trains:** Granby cars pass over a track grizzly without dumping; bottom-dump cars pass a side-dump ramp without tipping.
  A train can carry both and dump each at its own station.
- **Oversize:** **TBM muck has no oversize** *(real: a hard-rock TBM's muck is mostly under 100 mm)*, so every rock passes the bars,
  as today. Blasted rock is not hauled by train. *(Proposed: the grizzly's oversize and a rock breaker wait for a source of big rock
  underground.)*
- **Where it pays:** a pocket under the track takes less room than one beside it, and a dump at a shaft bottom (L4) needs one.
- **Downstream** as dump-station.md: the apron feeder, the sorter and the bins, or the shaft's measuring flask.
- **Reads:** the car's sneak-click reads its load and DOORS SHUT/OPEN. The pocket reads FULL as today.

## Left out (to ROADMAP)
- Doors jamming on sticky muck; frozen loads.
- Oversize on the grizzly and a rock breaker (above).
- Rotary car dumpers.

## Game tests to add
- `bottom_dump_on_move`: a train of bottom-dump cars dumps every car into a pocket under the track without stopping.
- `bottom_dump_full`: with the pocket full the train stops with the car over the grizzly, and goes on when the feeder makes room.
- `bottom_dump_mixed`: a mixed train dumps its Granby cars at a ramp and its bottom-dump cars over a grizzly.

## The wave plan
- **L3**: small, mostly models and a dump rule; before the shaft (L4).

## Sources
- SME *Mining Engineering Handbook*, 3rd ed. (2011), rail haulage: bottom-dump cars and cam-opened doors *(unverified detail)*.

## As built (L3, 2026-11-09, branch `l2-l3-trains`)
Decisions in [../questions/l2-l3-trains.md](../questions/l2-l3-trains.md) (#14-#20).
- **Code:** the `bottom_dump_car` entity type on `MineVehicleEntity` (its doors synced as `doorsOpen`); `track_grizzly` and
  `dump_cam_rail` (`Machine` parts, models in `tools/models/trains.py`); `DumpBlockEntity.grizzlyUnder` (rail on a grizzly, pocket
  under it, cam rail beside the rail or the grizzly); `SignalBlockEntity.dumpPocket(bottomDump)`; `client.BottomDumpCarModel`.
- **As the spec, except:** the pocket keeps the side dump's oversize rule (1 %, 32 choke it) though the spec says none (#16); the doors
  snap open and shut rather than swing; the pocket is turned east or west to run 4 m along the grizzly.
- **Tests:** NeoForge `bottom_dump_on_move` (three cars over the grizzly at 2.7 m/s, all dumped, doors open and shut),
  `bottom_dump_full`, `bottom_dump_mixed`, `train_models_l2_l3`; the client film `battery_bottom_dump`.
