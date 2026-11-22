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

## 2. Reagents by hopper were mostly lost (fixed; needs Tim)
- **Found:** a hopper over a conditioning tank sets the first item of a reagent into an empty slot (counted, 1 kg) but then grows that
  stack in place without `setItem`, so the tank never counted the rest, and `trimReagents` then took those items away. A tank fed by
  hopper (the way flotation-bank.md says reagents go in) ran short at once: NO REAGENT with a hopper full of lime. The game tests fed
  the tank by `offer()`, which counts each item, so none caught it.
- **Done:** `PlantBlockEntity.reagents()` first counts any reagent items over the counted kilograms as new kilograms (the tank holds
  exactly one item per kilogram or part of one it has counted, so the surplus can only have come by hopper). New NeoForge scene
  `reagent_hopper`: 5 xanthate and 3 lime through a hopper read 5 and 3 kg. No rule changed; Tim may want to check older worlds, whose
  tanks will now count the items they hold.

## 3. The reagents made in the world, on camera
- On the small tank's top: a hopper into the tank, a furnace over it burning calcite to quicklime, and three vanilla crafters round it
  facing it (xanthate, frother, copper sulfate), each crafted by a button press. The crafters' ingredients are kept stocked (said).
- Off camera the rig runs like a level control: the furnace gets calcite while the tank has under 6 kg of lime, a crafter is pressed
  while the tank has under 3 kg of its reagent, so no reagent crowds the hopper. The pit tank's chest is refilled in dose proportion
  (20 lime, 2 xanthate, 1 frother) only when empty, for the same reason.

## 4. Faults shown the in-game way
- **NO AIR:** the blower's selector turned to OFF and back to HAND (small bank), and the large blower's (pit row: every cell's AIR point
  falls from 20 to 4 mA together).
- **NO REAGENT:** the furnace emptied and the tank's lime drawn down to 0.3 kg off camera (said), run out on camera, then calcite back
  in the furnace and conditioning again ten seconds later.

## 5. The pit row's layout and power
- The column cleaners stand side by side east of the concentrate header, not in line on it as p7_flotation lays them, because there
  the header covers their motor boxes. At the lean feed (0.4 % copper in the slurry) column 1 takes all the concentrate and column 2
  stands by; the card and narration say so.
- Seven sets: three cells (about 700 A) to a gas set's 800 A busway; the 400 kW blower on a fourth gas set stepped up to the medium
  voltage; diesel sets for the small motors. Every run is laid joined (the air tubing too, after run 4 drew it in loose pieces).
- The plan's "45 times the scale" is not used: the pit line is 20 times the small plant's feed (1,000 against 50 t/h), which the
  narration says.

## 6. The filter pays nuggets into the hopper
- The hopper under each filter takes the copper nuggets before nine pack into an ingot (as episode 4 found for zircon). The cards count
  copper paid in ingots' worth, nuggets and ingots together; the narration says the filter pays nuggets, nine to an ingot.

## 7. The small bank's froth is not animated
- Only the pit row's tank and column cells draw a moving froth. The bank's eight cells are one still model; the cold open and the froth
  shots use the pit row's cell 1, and the narration says the bank's cells are drawn, not animated. Adding froth to the bank is a
  renderer change for another day.

## 8. Half-speed drawing (run 4, not used)
- In run 4 the client drew at half speed from about seven minutes of filming on, whatever the camera looked at. Run 5, with the air
  tubing joined and five films, ran in real time throughout. Not chased further: every film used is real time.

## 9. Tests
- `build` passes. The full NeoForge suite passed with the thickener fix, and passed again with everything here; one run between them
  failed only `pit_dozer_cut` ("it should cut 20 m: at 15.5"), which nothing here touches and which passed on the rerun. **Worth a look
  by Tim** as a flaky scene.
