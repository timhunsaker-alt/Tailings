# Open pit P4, dry stack, dumps, berms and the dozer — decided while Tim was away (2026-11-09)

The draft spec ([../multiblocks/open-pit-p4-dry-stack.md](../multiblocks/open-pit-p4-dry-stack.md)) and its choices
([specs-tailings.md](specs-tailings.md), P4) were taken as written. These were open when it came to building, and were decided on the
best recommendation (Tim, 2026-11-09: "make decisions best recommendation and complete the rest of these two mods leaving questions
for when I get back"). PROGRESS.md, "Open pit P4", has what was built.

### 1. Thickened tails on the pipe, with no product for them
- **Chosen:** Wildcatter 0.5.1 has no thickened-tails product, so the thickener's underflow goes on the pipe as tails slurry whose
  assay carries its solids as its own entry (`tailings:solids` = 0.60), as the depressed sphalerite already does. The filter refuses
  tails without it: "NOT THICKENED: feed it from a Tailings Thickener's underflow".
- **Why:** P4 must build on the released Wildcatter; the assay already travels with the slurry through Wildcatter's pipe, so nothing
  else changes. Refusing raw tails keeps the spec's P4-1 (a thickener ahead of the press) honest.
- **Alternatives:** a `THICKENED_TAILS` product in Wildcatter (cleanest; proposed for its ROADMAP Ideas, for a later release); the
  thickener feeding a filter that touches it (a 15 m tank beside a press on a floor 4 m up does not lay out); the filter taking 35 %
  tails as written in the spec's alternative.
- **Left:** Wildcatter's lined cell takes the marked underflow as plain tails, counting its solids at its own figure (Known gaps).

### 2. The thickener's underflow store
- **Chosen:** the thickener holds up to 20 t of underflow slurry for the press *(proposed)*, so it keeps working through the 7.5
  minutes a press stands open.
- **Why:** a real thickener's bed is the buffer for a batch filter; with the plant's few seconds of hold the thickener and the bank
  behind it stopped every cycle.
- **Alternatives:** a surge tank part (another part for little play); the press taking feed during its open time (not how a press works).

### 3. What the cake weighs
- **Chosen:** a tonne of cake is counted as a tonne of solids, as the spec's figures are (15 t a cycle is 8.3 blocks; 91 t a truck
  is 50 blocks).
- **Why:** the spec's numbers come out; the 15 % moisture is in the readout and the filtrate, not in the truck's tonnes.
- **Alternatives:** cake mass = solids / 0.85 (a cycle 17.6 t, 9.8 blocks).

### 4. Where the cake drops
- **Chosen:** into a load-out bin touching the press's bottom, else as items into a container under its chute (the bottom cell
  nearest its middle), else as settled blocks onto open ground under the chute; with none, it holds the cake and reads FULL.
- **Why:** a press over its bin or a cake bunker is the common real layout; a loader can rehandle a cake pile (P2's loader digs dry
  tails).
- **Alternatives:** onto a belt (the spec). Belts carry rock by kind and size; dry tails are not a rock kind, and adding one touches
  every size item. Left out (Known gaps).

### 5. The load-out in two parts, not three
- **Chosen:** `loadout_frame` (12 x 10 x 12) carries the clamshell gate and its chute in its own model, under the deck's centre, with
  the gate's 7.5 kW hydraulics as the frame's motor; `loadout_bin` (10 x 7 x 10) stands on it.
- **Why:** the spec's 3 x 2 x 3 gate hangs inside the frame's cells; two parts cannot hold the same cells.
- **Alternatives:** a frame with a hole in its cells (true-size parts are boxes); the gate as part of the bin's cells (the bin would
  then reach down into the lane).

### 6. Where the LOADOUT stake stands
- **Chosen:** at the mouth of the lane, on the line through it. A truck at a LOADOUT drives on along the line to the load-out frame it
  finds within 16 m and stops with its body under that frame's gate; with no frame, at the stake.
- **Why:** the frame's cells fill the lane, so no stake can stand at the gate. The spec's rule, "the stake marks the gate", is kept as
  closely as blocks allow.
- **Alternatives:** stakes allowed inside part cells (they would replace the frame's cells).

### 7. How a truck leaves a load-out
- **Chosen:** filled, it drives out of the far side 15 m past the gate, then round a balloon loop (a quarter turn one way, three
  quarters the other, at full lock), which puts it back on the line heading back, and drives back through the lane. Give it about 40 m
  beyond the gate and 25 m to its right.
- **Why:** a route runs out and back; a U-turn puts the truck 25 m off the line, too far to get back into a 10 m lane. The balloon
  loop is how real drive-through loops are laid out.
- **Alternatives:** routes that run on through a load-out as a loop (a bigger change to routes); backing out (not a drive-through).

### 8. The leg into a load-out is single file
- **Chosen:** on the leg next to a LOADOUT end a truck keeps to the stake line both ways, not 4 m right of it.
- **Why:** the lane is 10 m for a 6.2 m truck; a truck in its two-way lane would hit a leg.
- **Alternatives:** leave the survey to call the leg one-way (it only does when the frame's legs fall inside the leg).

### 9. Counting a berm
- **Chosen:** a berm counts where a ridge at least mid-axle high (1 block for the 100 t truck) stands within 3 m behind the DUMP POINT
  stake across the truck's tail (on its centre line and 1 m in from each corner); a crest is a drop of 3 m or more within 10 m on any of
  those lines. The truck's tail stops 1 m short of the berm.
- **Why:** the spec's figures; a berm must span the tail to stop it.
- **Alternatives:** a berm anywhere on the centre line only (a single block would count).

### 10. A berm stops a truck driven by hand
- **Chosen:** reversing, a truck does not step up onto a ridge with a drop behind it, so a berm stops it as a bumper does; it still
  climbs a 1-block step anywhere else.
- **Why:** trucks climb 1-block steps, so a 1-block berm would not stop a player otherwise, and a berm is there to stop the truck.
- **Alternatives:** berms 2 blocks for every truck (more than mid-axle); berms only for self-driving trucks.

### 11. Going over the crest
- **Chosen:** a truck not on a route backing with its rear axle (2.5 m in from its tail) over a drop of 3 m or more tips back over
  the edge, falls, and is wrecked where it lands: its item and its load (tipped by the settle rule) there, and its driver takes the
  fall less 3 m as damage.
- **Why:** the spec (P4-4); keeping the item makes a mistake cost time, not the truck.
- **Alternatives:** the truck is lost.

### 12. The dozer's settings without a screen
- **Chosen:** jump steps the blade (CARRY, CUT, RAISED); sprint steps the setting that belongs to the blade's position: the grade in
  CUT (level, 5, 8, 10 %), the berm's height in CARRY (1 or 2 m). The cab's overlay says what is set.
- **Why:** a ridden entity has few keys; a screen for two settings would be a menu on both loaders for little gain.
- **Alternatives:** a screen (the spec's word); sneak-clicking from outside.

### 13. The dozer at a crest
- **Chosen:** when the ground falls away 2 blocks or more under the front of its tracks it stops there; with a load it pushes the load
  over first ("at the crest: pushed the load over").
- **Why:** a dozer pushes to the edge and backs off; driving over would need the truck's wreck for the dozer too.
- **Alternatives:** it can drive over and fall (and be wrecked).

### 14. The dozer's cut and spill
- **Chosen:** CUT's first cut is a block below its tracks, then a block lower each 20, 12 or 10 m driven (level never steps). When
  the blade is full, the extra spills beside the blade's ends, a metre ahead of it, as windrows.
- **Why:** the spec's figures; spilling ahead of the blade keeps the windrows from rolling back into the cut behind it.
- **Alternatives:** spill beside its tracks (refilled the ramp behind it).

### 15. Hard rock at the blade
- **Chosen:** a block in the blade's two rows that a pickaxe mines and that is not loose stops it: "hard rock: blast it first"; any
  other solid block stops it naming the block. Loose is soil, muck, dry tails and Wildcatter's soft ores.
- **Why:** the loader's rule.

### 16. Dry tails as a block
- **Chosen:** `dry_tails` falls like sand, is dug with a shovel, by the loader and the dozer, and tips as its own block. The pit
  hopper refuses it ("not rock: tip it on a dump").
- **Why:** it is filtered waste: it goes to the dump, not back through the plant.

### 17. PLC points for the press and the load-out
- **Chosen:** left out for now; the press, the thickener and the load-out frame have the standard motor points (RUN, RUNNING,
  STALLED, AUTO, AMPS, FILL). The spec's LEVEL, FULL, LOADING and CYCLE were a Known gap; **built in gaps2** (gaps2.md #2).
- **Why:** the plant's points are one shared list per motor; giving a part its own points is a change to that list for every part.

### 18. Sounds
- **Chosen:** reused: the pressure filter's loop for the press, the motor hum for the thickener, the muck tip at each cake drop and
  for a wreck, the muck pour while the gate loads, and the diesel loops for the dozer.
- **Why:** the task asked to reuse sounds; the spec's hydraulic whine and track clatter are new sounds for the art wave.

### 19. Waiting at a load-out
- **Chosen:** as at LOAD: a truck with some load leaves after 60 s with nothing more loaded; an empty truck waits for the bin.
- **Why:** the spec's 60 s; leaving empty would drive an empty truck to the dump.

### 20. The dozer's figures
- **Chosen:** 70.2 t, 447 kW, 12.2 km/h forward (the middle of 11.7-12.7), 15 km/h back, 3.5 km/h dozing, a 1,200 L tank, 80 % power
  while dozing, turning on 4 m *(proposed: a crawler turns nearly on the spot)*, 8.8 x 4.9 x 4.6 m.
- **Why:** the spec; the turning radius is new.
