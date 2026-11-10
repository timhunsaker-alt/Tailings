# Plant gaps — decided while closing them (2026-11-09)

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods ... close all known gaps". These are the
ore plant's and dump's known gaps from ROADMAP.md, closed on branch `plant-gaps` (PROGRESS.md, "Plant gaps").

### 1. How fast a belt carries rock
- **Chosen:** the spec's 2 m/s (`belt_conveyor`: a 600 mm troughed belt at 2 m/s), from where the rock dropped on to the head end; a
  20 m run takes 10 s. A run whose head end cannot deliver stops where its rock is.
- **Why:** 2 m/s is the spec's figure and a usual speed for a small troughed belt. Real belts trip on a blocked chute rather than pile
  rock at their head, and a stopped belt cannot take more at its tail.
- **Alternatives:** 2.5-3 m/s (shorter waits, a faster belt for the same tonnage); keep the belt running and let the head pile up to
  the run's hold (it would deliver a burst when unblocked).

### 2. Which face of a part is its feed face
- **Chosen:** as the spec's "Nozzles and faces": grizzly, cone and heap pad over their top; screen and ball mill over the top of their
  north (feed) end, taken as the first third of their length (the first two rows of six); the jaw only from its grizzly; the ore bin
  only from its sorter. A part beside an output face takes nothing straight in.
- **Why:** the spec says "fed by a belt over its north (feed) end" and "over its top". The first third gives a belt's head a little room
  without letting rock in mid-screen.
- **Alternatives:** the north row only (strict, harder to lay); the whole top (looser). Keep chutes between touching parts (simpler
  layouts, but not what the spec draws).

### 3. The old layouts that broke the feed faces
- **Chosen:** moved to belts. The NeoForge `crushing_circuit` scene and the Fabric `crushing` film had a cone against the screen's south
  face and the jaw's belt feeding the screen from under it; the playthrough fed the screen through its west face and from under it, and
  had the same cone. All now go by belt; the playthrough's cone stands on the floor beside the screen, and its product comes round
  onto the jaw's belt. A server scene (`playthrough_crushing`) builds the playthrough's crushing layout block for block.
- **Why:** the spec never allowed them; the old R3 layout sketch's "cone touching the screen" was the only place it did, and the R4a
  gap list called them stand-ins.
- **Alternatives:** keep the playthrough as it was by loosening the faces (gap 2's alternative). The playthrough's power drops were
  re-planned with the layout and checked cell by cell, but the 15-minute run itself was not run: **the lead should run it once.**

### 4. The screen's gravel face
- **Chosen:** the west face of the screen's south (discharge) end. A belt there takes the 6-40 mm: 35 % of crushed rock *(proposed)*
  and all of the cone's product that is not sand; what that belt has no room for goes round to the cone. A hopper or chest at any
  belt's head end takes rock as items (2.7 t of one rock and size an item), so the gravel ends as `gravel_*` items.
- **Why:** the spec gives no face; the west of the discharge end is free of the motor box (west of the main cell, north end), the sand
  face (east) and the oversize face (south). The middle deck of a 40 mm / 6 mm screen discharges at that end. Items in a chest are how
  a player gets aggregate, and the same rule gives the waste bin a way out.
- **Alternatives:** a hopper under the face (hoppers only pull from above, so it would have to be a slot of its own); a share of 25-45 %.

### 5. Oversize on the dump pocket's 300 mm grizzly
- **Chosen:** one item in a hundred a car tips *(proposed)* stays on the bars, from the load's commonest rock. 32 pieces choke the
  grizzly: no car tips (the train waits). A click with a pickaxe breaks one piece through into the pocket and wears the pickaxe a point;
  a sneaking empty-hand click lifts them all off to the player.
- **Why:** the spec says "oversize stays on the grizzly; a player clears it by hand". TBM muck is mostly chips, so the share is small;
  slabs from the crown and broken segments are what real pockets catch. The pickaxe stands in for a rock breaker.
- **Alternatives:** a powered rock breaker part on the pocket's rim (real, a later wave); a share of 0.5-3 %; oversize returned to the
  car rather than the bars.

### 6. Where a loaded train waits for a full pocket
- **Chosen:** at the block signal whose section holds the dump, while the pocket cannot take the whole train's load or its grizzly is
  choked; it stops a little short of the section. With no signal before the dump it waits with the car over the pocket, as before.
- **Why:** the spec: "the train waits at the signal before the ramp". Waiting for room for the whole train means it never stands half
  tipped over the pocket.
- **Alternatives:** wait only for room for the first car (it would then stand over the pocket for the rest); a dedicated dump signal
  block.

### 7. A selective plant's concentrates mixed
- **Chosen:** a bank's floated sphalerite travels as `tailings:floated_sphalerite`; a filter whose feed holds it and galena together
  makes a bulk cake: the lead pays and that zinc is lost to slag, as on a bank fed both. The scavengers' sphalerite is not tagged and
  pays as before (selective-flotation.md's existing exception).
- **Why:** the 4e spec gives each bank its own thickener and filter, and Tim's rule is that a bulk cake smelts to lead. The tag keeps
  the scavengers' exception without tracing every kg.
- **Alternatives:** the rule in the thickener as well (same effect, earlier); lose all sphalerite in any cake holding galena (would
  break the scavengers' exception).

### 8. The ore sorter's screen
- **Chosen:** a real menu on both loaders, opened with an empty hand: a slot a side round the sorter showing the type it takes (put a
  rock on it to set, the item stays in hand; empty hand clears), a W button a side for the waste side, shift-click to set the first
  free side. A click on a side with a rock item still sets it straight away. The sneaking empty-hand click no longer sets the waste
  side (it opens the screen).
- **Why:** follows the drill cab's menu and screen. Ghost slots are how vanilla filters are set.
- **Alternatives:** a menu with real slots holding one sample item each (the player would lose the item to it).

### 9. The waste bin backing up
- **Chosen:** written, not built into the playthrough. The ways out exist now: a belt under the bin to a heap pad, or to hoppers and
  chests at its head end. DESIGN.md, "Dump and signals".
- **Why:** the playthrough fills about an eighth of the bin in its 11.5 minutes; a heap pad there also needs acid and power, more than
  a small change to the filmed layout. A real mine sends waste to a waste dump, which is open pit P4.
- **Alternatives:** add the heap pad to the playthrough (more parts, more power, no change to what the run proves).

### 10. Concentrate as mineral only
- **Chosen:** kept. The concentrate carries the recovered mineral at 30 % solids; gangue and the thickener's overflow water stay out.
- **Why:** no payout changes with gangue in it, and the assay being in kg (decision 12) already gives a concentrate its real mineral
  mass. Adding gangue would only matter for a smelter's grade, which Tailings has not got.
- **Alternatives:** carry a gangue share (about 25-50 % of a real concentrate) and an overflow water stream to the process water.

### 11. The `fines_*` items
- **Chosen:** kept as they are, registered for old worlds, no use.
- **Why:** removing them would turn old worlds' items to air; giving them a use (a hand-fed slurry) would be a new mechanic nobody asked for.
- **Alternatives:** let the conditioning tank take them by hand as fines slurry; drop them in a later release.

### 12. Ingots in the assay
- **Chosen:** the assay counts metal in kg: an ingot is 100 kg *(proposed; Wildcatter counts an iron ingot as 100 kg)*. Copper ore's
  four ingots are 400 kg in its 2.7 t. The filter and the cells divide by it to pay ingots and nuggets, so payouts do not change.
- **Why:** the assay is kg of mineral per kg everywhere else; ingots-as-kg made a vanilla metal's concentrate weigh almost nothing.
- **Alternatives:** a real copper block's mass (about 8.9 t a cubic metre, so 990 kg an ingot) or vanilla's ninth of a block; either
  changes no payout, only the masses shown.
