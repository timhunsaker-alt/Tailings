# Multiblock specs

Every Tailings machine has a spec here **before any code is written**, as in Wildcatter. A spec has:
- the real-world counterpart;
- its size and rate;
- a parts table;
- a layered ASCII grid with its legend;
- the nozzles and terminals;
- how it forms and runs, and what each part reads when it stops;
- what removing a part does;
- what is left out;
- the game test.

**Status:**
- **APPROVED by Tim on 2026-11-05 (R2), in Wildcatter's format.**
- Approval covers the real-world numbers, the part lists and the behaviour.
- The ASCII grids are sketches; they are fixed with the models when each part is built (R3).
- Approval is not a go-ahead to build: each part waits for its R wave (ROADMAP.md).

| Spec | Built in | Replaces |
|---|---|---|
| [crushing-circuit.md](crushing-circuit.md) | R3, R4 | the box-of-cubes crushing line in one row; the item shortcut from grizzly to fines |
| [flotation-bank.md](flotation-bank.md) | R3, R4 | eight separate `flotation_cell` boxes |
| [tunnel-boring-machine.md](tunnel-boring-machine.md) | R6 | the one-block cab and 2 x 2 x 2 pump that step by deleting and re-placing blocks |
| [haulage.md](haulage.md) | R5, R6 | cars stored on rail blocks; the rectifier modelled as a motor |
| [dump-station.md](dump-station.md) | R3, R4 | the one-block `dump_station` with a slot-map sorter |
| [tunnel-drill-haulage.md](tunnel-drill-haulage.md) | | pointer only: split into the three specs above |

## Shared rules
Wildcatter's apply unchanged: its `docs/multiblocks/README.md`, "Shared rules" and "Big parts". In short:
- true scale (1 block = 1 m);
- one item per real component, filling its real size when placed, with a ghost outline;
- distinct parts with distinct block ids, and never tiled identical blocks;
- terminals on named faces;
- models from `tools/models/<part>.py`;
- breaking any piece breaks the part.

Tailings adds four rules of its own:
- **The contraption (the tunnel boring machine).** A machine that drives itself lifts its parts into one moving entity while it
  runs. The entity draws their models, carries their contents, and keeps its chunks with the `tailings:runner` ticket. When it
  stops, it sets its parts back down as blocks.
- **Entity trains.** Locomotives and cars are entities on the mine rail, not data on the rail blocks.
- **Belts are laid like pipe.** A belt conveyor is a run of `belt_conveyor` blocks of any length, as a Wildcatter pipe or cable run
  is. Its motor is on the `conveyor_drive` at its head end. A run is a conveyor, not a tiled machine.
- **Assayed slurry on Wildcatter's steel pipe.**
  - From the ball mill on, ore moves as slurry on Wildcatter's steel pipe. Each joined run carries an **assay**: kg of each
    mineral per kg of slurry, so a run of copper fines slurry knows its chalcopyrite.
  - This needs a Wildcatter change, proposed in Wildcatter's ROADMAP Ideas ("Assayed slurry on the steel pipe"). R4 waits for
    that Wildcatter release.

## Nozzles (every Tailings part, as in Wildcatter)
Each part is drawn facing north (x east, z south), with its main cell at the north-west bottom corner.
- **Motor box:** on the west face of the main cell.
- **Pipes:** in at the north face of the front cells and out of the south face of the back cells, at the bottom (y 0), unless a
  spec says otherwise.
- **Hoppers:** in from one on any top cell, and out to one under any bottom cell.
- **Belts:** a belt delivers onto the part whose top cell its head end sits over, and takes from a part whose discharge face it
  touches.
