# P6: the walking dragline — decisions taken while building (2026-11-10)

Spec: [../multiblocks/dragline.md](../multiblocks/dragline.md) (approved by Tim, 2026-11-09). These are the questions building it
raised that the spec leaves open, decided on the best recommendation while Tim was away. Nothing here is final until he has been
through it. It is built as P5's rope shovel was ([p5-big-machines.md](p5-big-machines.md), decisions 27-34): parts on the ground,
lifted into one machine when it runs, on its trailing cable from a pit switch house.

## The parts

### 1. The boom: five sections of 15 m, not ten of 7.5 m
- **Chosen:** `dragline_boom_section` is a 4 x 4 x 15 big part; five laid foot to point in a line make the 75 m boom. Each is two of the
  spec's 7.5 m sections pinned together. NOT BUILT names the missing one by its place ("the third Dragline Boom Section").
- **Why:** a cell is a whole metre, so a 7.5 m part cannot exist; 15 m keeps the boom exactly 75 m and still avoids the one 75 m item the
  spec ruled out. Real boom sections ship in lengths of 6-15 m and are pinned together on site.
- **Alternatives:** ten 4 x 4 x 8 sections (an 80 m boom laid out); sections of 7 and 8 m (two items); one 75 m part.

### 2. How the parts stand to be built
- **Chosen:** the tub on the ground; a walking shoe each side of it, outboard of the tub and under the house's sides; the house on the
  tub, a metre up (the tub's model draws its roller path and centre pintle in that metre); the mast on the house's roof at its front;
  the five boom sections laid on the ground ahead from the house's front, and the bucket beyond the boom's point, its mouth toward the
  machine (`pit.DraglineLayout`). 82 m from the tub's front to the bucket's teeth. All face the same way.
- **Why:** the rope shovel's layout (P5-27): a boom is laid out on the ground and raised by the mast's ropes. Putting the shoes under
  the house keeps the machine 24 m wide, as its house, so it stands back from the strip by the spec's tub radius plus 5 m without its
  shoes hanging over the cut.
- **Alternatives:** shoes outside the house (30 m wide; at 12.5 m from its walk line the shoes would overhang the strip).

### 3. The parts keep the spec's full boxes
- **Chosen:** every part fills its whole W x H x L box, as every Tailings part does: 8,262 cells in all, 5,760 of them the house. That
  is about 33,000 block states, cheap in 26.3 (a block's states share one neighbour table), and some 2 s more model baking at start-up.
  No existing block changes.
- **Why:** the spec's sizes are approved; a box is what Wildcatter's big parts and Tailings' model tool are built for.
- **Alternatives:** a hollow house (a shell of 1,800 cells: about 15,000 fewer states; needs a non-box footprint in `PlantMachineBlock`
  and the model tool). Recommended only if start-up time is noticed.
- **Tim (2026-11-10): "a hollow house; turn the shared empty model on for every big part." Built:** `Machine.hollow()` leaves the
  machinery house's closed inside out of the house's footprint (`Footprint.of`, the box's order kept): 22 x 9 x 15 = 2,970 cells, none
  of which drew anything, so the house is 2,790 pieces, not 5,760, and **11,880 fewer block states** (the dragline 5,292 cells in all,
  not 8,262). The other 581 empty cells (the open air in front of the machinery house and over
  its frame) stay, so the part still claims the whole box it is set down in. `tools/models/tlib.py` leaves out the same cells (`model(..., hollow=)`). With every big part sharing one empty
  model (p7-pit-line.md #33) the house is 2,209 piece models and one `empty.json`. Nothing looks different: every part rebuilt from its
  blockstate and pieces draws the same boxes in all four facings, and the previews are byte for byte the same.

### 4. Recipes and items
- **Chosen:** placeholder recipes from Wildcatter's steel and vanilla iron (the house takes Wildcatter's medium-voltage drive cells and
  drive transformer, as the rope shovel's). Each part's item is its own shrunk block model, so no icon is drawn and no Ludo art was used.
  The lifted machine has no item: broken by hand (sneak, empty hand) it gives back all eleven parts, its cable and its bucket's load.
- **Why:** as the rope shovel's (P5-33); real recipes come with the art wave.
- **Alternatives:** none needed now.

## Lifting it and drawing it

### 5. One machine, drawn by its own model
- **Chosen:** RUN on the tub's panel checks the parts, the cable and that the strip leaves room to cast, then the parts' cells go to air
  quietly and one `DraglineEntity` stands at the tub's centre, by the tub's name (its outlet keeps knowing it), with its cable and job.
  It is drawn with `DraglineModel` (the vehicle generator, Ludo's materials, a 2048 skin): the tub, the house swinging on it with the
  shoes on their cams, the mast, the boom at 32 degrees in ten 7.5 m lattice sections, and the bucket placed by the hoist and drag. The
  hoist, drag and suspension ropes are drawn as lines. Its box is the house over the tub, 24 x 20 x 15 m; the boom and mast stand above
  its box and do not collide.
- **Why:** the rope shovel's (P5-28). A box 72 m long for the boom would make the machine unable to stand beside anything.
- **Alternatives:** draw each cell's own block model (8,262 cells; heavy and the boom would stay lying down).

### 6. The boom's skin: shared patches
- **Chosen:** `tools/models/pit_vehicles.py` learnt an opt-in `share`: boxes of the same size, colour and pattern draw from one patch of
  the skin. Only the dragline uses it (40 chord boxes and 200 lacing bars); every other skin is byte for byte as before.
- **Why:** without it the 75 m boom and the 24 m house do not fit a 2048 sheet, and a 4096 sheet is 64 MB of video memory.
- **Alternatives:** a 2048 x 4096 sheet (32 MB); fewer, longer boxes for the boom (would not read as a lattice).

### 7. Its reach and the boom point
- **Chosen:** it digs and casts within the spec's 65 m of its centre; the boom is drawn true, 75 m at 32 degrees from its foot at the
  house's front (8.5 m ahead of the centre), so its point is 72 m out and 44 m up. The bucket hangs a little inside the point.
- **Why:** both are the spec's figures; a real dragline's dumping radius is a few metres inside its point.
- **Alternatives:** shorten the drawn boom to put its point at 65 m.

## Its job

### 8. The strip, in the machine's own frame
- **Chosen:** the strip starts where the machine was lifted: from its centre, ahead along the way its boom pointed when it was built
  (it walks that way), on its cast side from 12.5 m (its tub's radius plus 5 m) out across the strip's width. The panel sets the width
  (10-40 m by 5, default 30), the length (10-400 m by 10, default 100), the seam's depth under its tub (1-35 m, default 10) and the cast
  side (right by default). The settings can be changed while it runs.
- **Why:** the spec's screen ("a width, a length and the seam's top level"); a depth under the tub is how the player reads the seam's
  level from where the machine stands. 40 m is the widest strip that still leaves 6 m of room to cast within 65 m.
- **Alternatives:** the seam's absolute level (a number to look up with F3); a strip marked out with survey stakes.

### 9. What it digs, and what stops it
- **Chosen:** from 10 m above its tub (proposed) down to the seam's depth, in the strip and within reach: soil and soft ground whole;
  muck by the quarter (P5-22); grass, flowers and snow stripped with the topsoil. Down each column it stops at standing rock (hard),
  at any ore block (`*_ore`: the seam, left for loaders and trucks), and at another machine's block. When nothing it can dig is left
  in reach but rock was found: BLOCKED, "hard overburden: blast it first", trying again every 10 s.
- **Why:** the spec ("soft ground directly, and muck"; "the seam is not dug by the dragline"). Stopping at an ore block as well as the
  set depth means a seam set a metre too deep is still never cast onto the spoil.
- **Alternatives:** stop only at the set depth (a mistake on the panel would cast ore away).

### 10. The bucket: 25 bank blocks or 55 t
- **Chosen:** a bucket fills to 25 bank blocks (100 quarters) or 55 t, whichever comes first: 25 blocks of sand (40 t), about 20 bank
  blocks of blasted rock (55 t).
- **Why:** the spec's 33 m³ bucket, "about 25 m³ of bank", "about 55 t of rock a pass".
- **Alternatives:** by volume only (rock would fill to 67 t).

### 11. Top down, and where in the strip
- **Chosen:** each bucket starts at the topmost cell in reach (nearest the strip's start, then nearest its walk line) and takes the cells
  of that layer nearest it, then the next layer down.
- **Why:** "from the top down" (spec); starting at the strip's start lets it uncover the seam in order as it walks.
- **Alternatives:** dig one column to the seam at a time (would leave pits and walls).

### 12. Where the spoil goes
- **Chosen:** beyond the strip's far edge plus 6 m, within 64 m of its centre and from 20 m behind the strip's start to 20 m past its
  end: on the lowest ground there, a column 4 m farther out counting as a metre lower, nearest along to where it dug. It never casts on
  a machine's block, nor higher than 34 m over its tub (its boom point less 8 m): then BLOCKED, "no room to cast". The spoil lands by the
  settle rule, so the pile stands at the angle of repose. What found no room is cast again next cycle, without digging.
- **Why:** the spec: the previous strip's hole, or flat ground for the box cut. Lowest-first fills the old hole before building on flat
  ground; the lean outward keeps the pile's toe out of the cut, so nothing is dug twice (rehandle is left out, spec). Found by
  `dragline_strip`: with 4 m and no lean, 3 m³ of 1,500 rolled back in and was dug again.
- **Alternatives:** a fixed dump radius (a ridge that runs back into the cut); the player marks the spoil.

### 13. The cycle's split and its draw
- **Chosen:** 55 s: dragging the bucket in to fill it 15 s at 6 MW; hoisting and swinging out 17 s, casting 3 s and swinging back 20 s
  at 1.2 MW: 2.51 MW on average. Walking 2.5 MW (proposed: its walking cams run off the same generator sets); standing in its job
  (BLOCKED, CABLE END) 0.3 MW for its sets turning over (proposed); 0 stopped or DONE. `dragline_cycle` reads 6,000 and 1,200 kW on
  its outlet. Its breaker trips at 1.25 x 6 MW for 1 s, as the switch house's breakers do (877 A at 4.16 kV; 1,096 A pickup).
- **Why:** the spec's 2.5 MW average, 6 MW peak and 55 s cycle; peak while the bucket is dragged full through the bank, as the rope
  shovel's hoist (P5-29).
- **Alternatives:** a steady 2.5 MW (no peak for the breaker).

## Walking

### 14. When and how it walks
- **Chosen:** with nothing it can dig left in reach, it walks on if the strip runs on beyond its reach along the strip's far edge; else
  DONE. A step is 2 m a minute: the shoes swing out and down in the first quarter, the tub moves 2 m in the middle half, the shoes lift
  in the last quarter (drawn on the shoes' cams); the house swings back to point the boom along its walk first. A step needs its box
  clear 2 m ahead and ground under its centre, else BLOCKED, "the way ahead is blocked".
- **Why:** the spec's walk and step (real walking draglines walk about 0.2 km/h; 2 m a minute is 0.12 km/h).
- **Alternatives:** walk as soon as the nearest part of the strip is dug (it would walk away from cells still in reach).

### 15. At CABLE END
- **Chosen:** a step that would pay out past the cable's length stops where it is: CABLE END, trying again every 10 s; the outlet stays
  closed (P5-6). Another reel clicked on the lifted machine adds 400 m, and it walks on.
- **Why:** as every machine on a cable (P5-6).
- **Alternatives:** trip the outlet.

### 16. A reel on a lifted machine: a P5 gap closed
- **Chosen:** right-clicking a pit machine with a trailing cable reel now couples it, adds 400 m, or (sneaking) takes its cable off.
  `TrailingCableItem.onMachine` was written in P5 but never called, so no player could put a reel on the drill, the hydraulic shovel or
  a lifted rope shovel; the scenes coupled them by code.
- **Why:** P5-8 says the reel is clicked on the machine; the dragline needs it to walk past its first 400 m.
- **Alternatives:** none: it is a bug.

## Running it

### 17. STOP, RUN and DONE
- **Chosen:** STOP on its panel lets it finish its cycle or step, then stops it and sets its parts down where it stands, square to its
  walk, if every cell is free and its bucket is empty; else it stays STOPPED as one machine and says why. RUN starts it again. DONE
  stays DONE (a new strip: STOP, set down, set the job, RUN).
- **Why:** the spec ("GO and STOP, as the TBM's cab"; "stopped, it sets down as blocks if the cells are free"); a real operator ends
  the cycle before stopping.
- **Alternatives:** stop mid-cycle (the bucket would hang full over the spoil).

### 18. Its PLC points: on its switch house outlet
- **Chosen:** the spec's points are on the pit switch house, after the house's own points (whose numbers do not change): for each
  outlet, MACHINE RUN (command), MACHINE RUNNING, MACHINE DONE, MACHINE CYCLES (a pulse a cycle) and MACHINE POWER (4-20 mA over the
  machine's peak). AUTO is the house's selector. In AUTO a fresh RUN off stops the dragline at the end of its cycle (no setting down);
  RUN on starts it. The dragline answers on its cable's control pair each tick. `dragline_plc` checks them.
- **Why:** the spec puts a ControlDevice "at its tub", but the tub lifts and walks; the switch house is the fixed point its cable
  reaches, and real trailing cables carry pilot and control cores. This is P5-32's plan, built.
- **Alternatives:** a device on the standing tub only (its points would vanish when it runs); a separate control box on a cable.
  The rope shovel could answer on the same points (RUNNING, its PASSES); not wired yet (ROADMAP, Known gaps).

### 19. The cab
- **Chosen:** a player may sit in the cab (at the house's front left); the keys do nothing: the cab runs the set job. Sneak-click with
  an empty hand opens its panel; sneak-click with anything else, or click while someone is in the cab, prints its readout.
- **Why:** the spec: hand control of swing, drag and hoist is a Known gap.
- **Alternatives:** none now.

## The film

### 20. The `dragline` film
- **Chosen:** built from its parts on a bench of grass and dirt over a bauxite seam, beside the last strip's hole; coupled to a switch
  house; RUN lifts it. Its strip is 10 m wide and 70 m long, 3 m to the seam: the middle already uncovered, so it digs the near end,
  walks on to reach the far end, digs it, and reads DONE. Its cycles run ten times fast, so the film is about three minutes.
- **Why:** the spec's film (walking, digging a strip, casting spoil) in a watchable length.
- **Alternatives:** a whole 30 m strip at real time (hours).
