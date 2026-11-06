# Progress

Phases 0 through 3 are on `main` (pull requests 1 through 9). The realignment with Wildcatter (R) is current; R1 is done. Phase 4 has not started. The full status, including stand-ins, is [docs/ROADMAP.md](docs/ROADMAP.md). How the systems work is [docs/DESIGN.md](docs/DESIGN.md).

## Phase 0

Multi-loader skeleton, CC0, CI, and the plain-Java recovery ladder (25/50/75/95%). Merged in pull request 1.

## Phase 1

Hand-cranked stamp mill, hand screen, shaking table, and magnetic separator. Country rock, the 27 Wildcatter ores, and vanilla iron, copper, and gold have size items and mineral lists. Gravity and magnetic concentration pay 50%. Nine Tailings nuggets craft into one mineral item. Iron and gold use the vanilla nuggets. Merged in pull request 2.

## Phase 2

True-size crushing circuit and flotation bank on Wildcatter motors. Fines are the mill product. A full bank pays the 75% route for sulfides and for vanilla copper and gold. Lime is calcined calcite. Merged in pull request 4.

## Phase 3

Moving tunnel drill with chunk tickets, stone-default lining, quench tank, smooth 1-in-2 ramps, and a pause when the muck buffer is full. Haulage is a rectifier, a locomotive entity, cars stored on the rail, a dump, lights, and block signals. Textures and the locomotive and car models are in. Merged across pull requests 5 through 9. There is still no Field Manual.

## R1: safety fixes (2026-11-05)

The first wave of realigning Tailings with Wildcatter, after a review found it far from Wildcatter's patterns. R1 fixes only what damages worlds or leaks load:
- **Chunk tickets.** One `tailings:runner` ticket, refreshed by running machines and timing out on its own, replaces the global force-load.
- **The drill** no longer leaks an entity, and it no longer overwrites blocks when it moves: it stops as "blocked".
- **Trains** no longer do disk reads or block updates every tick, and keep their cars through a turnout. A turnout is now fed at all: the rectifier skipped switches.
- **Reagents** no longer jam the tank.
- **Plant machines and the drill** drop their contents however they are removed.

`./gradlew build` and `:neoforge:runGameTestServer` (15 scenes) pass.

## R2: specs in Wildcatter's format (approved by Tim 2026-11-05)

The three draft specs are rewritten in Wildcatter's format, and the drill-and-haulage spec is split in three, giving five specs:
- `crushing-circuit.md`;
- `flotation-bank.md`;
- `tunnel-boring-machine.md`;
- `haulage.md`;
- `dump-station.md`.

`docs/multiblocks/README.md` indexes them. The design choices made with Tim are in ROADMAP (R2).

The slurry the plant puts on Wildcatter's steel pipe needs a Wildcatter change, proposed in Wildcatter's ROADMAP Ideas. No code
changed.

## R4b: flotation to items (2026-11-05)

The flotation circuit runs as flotation-bank.md says, on steel pipe:
- the conditioning tank doses the reagents per tonne of feed (copper sulfate only for sphalerite);
- the bank, with its blower, floats 75 % of each floating mineral to concentrate and sends the rest to tails;
- the thickener and pressure filter turn the concentrate into items: nuggets, nine packed into the ladder's item, so a block of
  copper ore still pays 3 ingots;
- the tails go to a new `tailings_pond_placeholder` (Tim's decision) until Phase 4d.

R3's flotation shortcut, `Concentrator` and `PlantPlan` are gone.

**Tests:** `FlotationTest` (the split, the cake, reagents by the tonne); game tests `flotation` (3 ingots, the rest of the copper in
the pond) and `flotation_stops` (NO AIR, NO REAGENT, a full pond, tails with nowhere to go).

## R4a: the crushing circuit (2026-11-05)

The crushing circuit runs as crushing-circuit.md says, on Wildcatter v0.3.0's assayed slurry:
- every part runs on its own motor at its share of 50 t/h; no controller and no shortcut;
- dry rock moves in kg on belt runs, each driven from its head end (7.5 kW each 30 m), with lumps drawn on the belt;
- grizzly, jaw, screen and cone in closed circuit; sand onto the ball mill;
- the ball mill and cyclones at a 250 % circulating load, with water from Wildcatter pumps; mill discharge and fines slurry on
  steel pipe, each carrying the rock's mineral assay;
- the ore bin feeds the belt under it.

**Tests:**
- `CrushingRatesTest` (rates, Bond's law, drives, slurry water, rock loads, assays);
- game tests `crushing_circuit`, `bin_feeds`, `belt_drive` and a new `not_built`; every scene now has padding and its chunks
  loaded, as the true-size parts reach past the test's own chunk.

## R3: big parts and real models (2026-11-05)

Every static machine in the R2 specs is now a Wildcatter big part with a real model:
- crushing: grizzly, jaw, screen, cone, conveyor drive and belt runs, ball mill, cyclones;
- flotation: conditioning tank, flotation bank, blower, thickener, filter;
- dump station: ramp, pocket, apron feeder, ore sorter, ore bins;
- the traction rectifier.

Each part has:
- one item that fills its real size, with the ghost outline;
- pieces that break together;
- real collision;
- its motor box on the west face of its main cell.

The models come from `tools/models/`, using Wildcatter's tool and materials.

The plant keeps running on the old shortcut until R4 (Tim's choice). A line forms from parts joined by touching or driven belt runs.

**Tests:**
- `ModelLintTest` and `MachineTest`;
- game tests `part_break`, `not_built` and `terminal`;
- the crushing, flotation and haul scenes rebuilt on whole parts.

`./gradlew build` and 18 NeoForge scenes pass.

