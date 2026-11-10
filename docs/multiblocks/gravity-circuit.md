# The gravity circuit (recommended by Claude and taken by Tim 2026-11-06)

A **bank of spiral concentrators** on the cyclones' overflow, ahead of the conditioning tank. It is the plant's 2x route: the heavy
minerals that do not float go to concentrate at the ladder's 50 %, and everything else goes on to flotation.

Tim's answer to crushing-circuit.md's open DECISION was "whatever is recommended". The recommendation:
- **Spirals, not jigs.** In a hard-rock plant the feed has to be ground to free the mineral, and spirals are what treats ground feed
  (mineral sands, tin, tungsten, hematite). A jig needs coarse, already free mineral, such as alluvial gold or sand off the screen.
  The mod's Rock tracks no liberation by size, so a jig would do nothing a spiral does not. Jigs go to Left out.
- **After the cyclones, not before the mill:** the flow is already wet and ground, and the plant keeps one line.

**Sources:**
- Wills & Finch, *Wills' Mineral Processing Technology*, 8th ed. (2016), gravity concentration: spirals take about 2 t/h a start on
  feed under 1 mm, at 15 to 45 % solids.
- SME *Mineral Processing Handbook* (1985): spiral plants for heavy mineral sand and hematite.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal.

```
CYCLONE CLUSTER -> fines slurry -> SPIRAL BANK -> fines slurry -> CONDITIONING TANK -> ... (flotation-bank.md)
                                        \-> concentrate slurry -> CONCENTRATE THICKENER -> PRESSURE FILTER
```

## What it takes
Every mineral that does not float and has a gravity or magnetic route on the ladder: iron, cassiterite, zircon, ilmenite, rutile,
monazite, chromite, scheelite and the like (`Mineral.spirals()`). Hematite and magnetite are both heavy enough for spirals; iron has had
no plant route until now.

The minerals the bank floats (copper, gold, the sulfides) pass through untouched and pay flotation's 3x as before. **No mineral pays on
both.**

## Part
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `spiral_bank` | 4 x 5 x 6 | 24 twin-start spirals at 1 t/h a start for the plant's 50 t/h, in a steel frame over their launders, with a feed distributor on top *(proposed count)*. A **37 kW, 480 V** slurry pump lifts the feed to the distributor *(proposed)* |

## Nozzles and faces (drawn facing north; main cell at the north-west bottom corner)
- **Motor box:** on the west face of the main cell.
- **Fines slurry in:** on pipe at the north face of its front cells.
- **Out, at the south face of its back cells, bottom layer:**
  - concentrate slurry at its east cell (x 3);
  - fines slurry, the rest, at its west cell (x 0).

## How it runs
- **It runs** while it has feed and both outlets have room. It treats the plant's rate at its motor's strength, as the tank does.
- **Recovery:** the ladder's 50 % of each mineral it takes (`RecoveryRoute.GRAVITY_OR_MAGNETIC`), as concentrate slurry at 30 % solids
  *(proposed, the bank's figure)*. Its rate does not vary with feed grade.
- **The concentrate** joins the bank's on the thickener's inlet, or has its own thickener and filter. The filter pays it by the nuggets rule.
- **What it reads when it stops:** NO FEED, waiting for its motor, "its output has nowhere to go" naming the full pipe.

## Left out (ROADMAP)
- Jigs, shaking tables at plant scale and cleaning stages on the spirals.
- Wash water; a spiral's cut varying with feed density.
- Magnetic and electrostatic separation of the concentrate: built as L6, the dry mill (dry-mill.md).

## Game test
`spiral_bank` (NeoForge): cassiterite and galena slurry in, half the cassiterite to concentrate, all the galena on to the rest.
