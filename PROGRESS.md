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

