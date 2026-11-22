# Films for Rock to Metal, episode 5 (branch `films-rtm05`)

The `rtm05_*` client scenes (`Rtm05Scenes`, `TAILINGS_ONLY=rtm05`): the 50 t/h flotation bank with its reagents made in the world,
its faults, its thickener and filter; the pit row of tank cells, column cleaners and the large blower, each cell's AIR point. Every
motor on Wildcatter generator sets. Decided while Tim was away (2026-11-22), each on the best recommendation.

## 1. The thickeners drawn whole (fixed; a separate commit)
- **Found (Tim, episode 4 at 250 s):** the concentrate thickener drew its contents as a black octagon of three boxes in Wildcatter's
  near-black oil texture, with no floor, so the grass showed between the octagon and the round wall.
- **Done:** a `pool()` helper in `tools/models/tlib.py` draws each thickener's inside row by row to its wall: a concrete floor, clear
  water out to the overflow launder at the rim, and the muddy slurry over the bed inside 80 % of the radius. The slurry and water use
  the game's own mud and clay textures. All four thickeners (`concentrate_thickener`, `ccd_thickener`, `tailings_thickener`,
  `tailings_thickener_large`) are rebuilt with it; their bridges and drives are unchanged above the surface. Their model files grow
  (the large one from about 3 MB to about 15 MB of JSON, before compression), since every row is a box in every cell it crosses.
