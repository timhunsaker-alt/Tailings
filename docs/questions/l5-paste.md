# L5: paste backfill (2026-11-10), decided while Tim was away

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back".
Built on branch `l5-paste` from the draft spec [paste-backfill.md](../multiblocks/paste-backfill.md), whose own choices are in
`specs-tailings.md` (L5-1 to L5-3). Format as in `README.md` here. **#8, #14 and #19 most need your eye.** Nothing here changes an
existing world (#22).

### 1. How much of the spec is built
- **Chosen:** all of its parts: the `paste_filter` (vacuum disc filter), the `paste_mixer` (twin-shaft, with a cement hopper), the
  `paste_pump` (piston pump), the `fill_bulkhead`, the `fill_outlet`, the `paste_fill` block (wet, then set) and the `cement` item, with
  every readout the spec lists (NO CEMENT, NO FEED, NOT ENCLOSED, FULL, LINE PRESSURE) and the pump's PLC points. Left out, as the spec
  leaves them out: hydraulic and rock fill, fill strength and exposure, boreholes and their wear, paste rheology.
- **Why:** the chain from the thickener's underflow to paste in the ground is only useful whole; each part is small once the void scan
  exists.
- **Alternatives:** the outlet and bulkheads alone, fed straight from the thickener (no filter, mixer or cement: not paste fill).

### 2. What counts as the void
- **Chosen:** from the outlet, every cell it reaches through air, water and paste; anything else is wall: rock, a fill bulkhead, but also
  a torch, a rail or a pipe. A void need not have any bulkheads: a closed natural cave fills as it is.
- **Why:** L5-1 ("any enclosed underground void"); paste cannot flow into a cell a block already holds, and a void with no opening needs
  no barricade. A rail or a lamp left in a real stope is buried by the fill; here it stays as a hole in the fill, which is harmless.
- **Alternatives:** count torches, rails and the like as open and bury them (they would be destroyed, so a player's things in a stope
  would be lost); require at least one bulkhead.

### 3. How NOT ENCLOSED is found
- **Chosen:** the void reads NOT ENCLOSED if any of its open cells is at or above the world's surface height at its column (nothing at
  all above it), if it runs into ground that is not loaded, or if it has more than 20,000 cells. The outlet reads its void every 5 s
  and when clicked.
- **Why:** the surface height is exact and does not wait on light updates; a cave under a lake reaches the air over the lake through
  the water, so it reads open too. Unloaded ground cannot be known, so it is not trusted.
- **Alternatives:** sky light 15 (lags behind block changes; a glass roof would read open).

### 4. The fill order and FULL
- **Chosen:** each 2.0 t of paste (a cubic metre) lands as a block of wet paste in the void's lowest open cell, and on one level the
  cell nearest the outlet first, displacing water. It fills only cells no higher than the outlet; FULL is when none is left.
- **Why:** the spec ("fills from its lowest cells up", "FULL when the void is full to its own level"); real paste self-levels and the
  fill line ends at the top of the stope.
- **Alternatives:** fill above the outlet too (a pumped line can push paste up; it would pressure the bulkheads, which are not modelled).

### 5. Paste on the pipe is marked tails slurry
- **Chosen:** paste goes on Wildcatter's steel pipe as tails slurry whose assay carries its solids (`tailings:solids`, 0.78) and its
  binder (`tailings:binder`, 0.05), as thickened tails do since P4. A `PASTE` product is proposed for Wildcatter.
- **Why:** Wildcatter 0.5.1 has no product for paste, and Tailings changes nothing in Wildcatter.
- **Alternatives:** a Wildcatter release with a paste product first (L5 would wait on it).

### 6. The paste filter
- **Chosen:** 50 t/h of solids (proposed: the plant's tails), thickened tails in at its north face (plain tails or paste read NOT
  THICKENED), to an 80 % cake held in its chute up to 5 t for the mixer touching it (FULL when it cannot pass it on); the filtrate,
  about 0.42 L a kg of solids, back on a water pipe at its east face as the thickener's overflow is (lost with none).
- **Why:** the spec's disc filter; the cake moves by touching as the spec says, the water the way P4's parts return it.
- **Alternatives:** the cake on a belt (belts carry rock by kind and size, and cake is not a rock kind: P4's dry tails have the same gap).

### 7. The mixer needs a trim of thickened tails
- **Chosen:** cake alone is too stiff, so the mixer adds thickened tails from the pipe at its north face, 0.21 kg a kg of cake solids, to
  bring the paste with its cement to exactly 78 % solids (`PasteFill.trimPerKgCake`); without it it reads NO FEED. So the thickener's
  underflow must reach both the filter and the mixer (one pipe can feed both).
- **Why:** the spec ("thickened tails on pipe to trim the solids"); real paste plants trim the cake with thickener underflow or water.
- **Alternatives:** trim with water from a water pipe (one pipe less to lay; the same paste); no trim (paste at 81 %).

### 8. Cement: a 50 kg bag, from quicklime and a clay ball
- **Chosen:** `cement` is a 50 kg bag (proposed; real bags are 25-50 kg), crafted shapeless from one quicklime and one clay ball, a
  placeholder until a cement kiln exists (L5-2). At 5 % of the tails' solids the mixer uses one bag a tonne of tails, 50 bags an hour at
  the plant's full rate. The hopper holds 40 bags (2 t) and takes them by a click or a hopper above it.
- **Why:** a bag a tonne is easy to reason about, and real paste plants store cement by the tonne; the recipe is the spec's.
- **Alternatives:** a 1 t bulk bag (fewer items, but the cheap recipe would make a tonne of cement from a clay ball); vanilla concrete
  powder as the binder (L5-2's alternative).

### 9. The pump pushes to the outlet it finds on its line
- **Chosen:** once a second the pump follows the steel pipe from its south face, up to 1,500 m, to the first fill outlet on it, and
  pushes paste straight into the outlet (it does not offer paste to the pipe, so nothing else on the line can draw it). It runs while it
  has paste, finds an outlet, the line is within its pressure, and the outlet's void is closed and not full. The pump traces the line
  itself because Wildcatter's line tracer stops at 512 pipes, short of the spec's 1 km.
- **Why:** a fill line is one line with one end; pushing lets the pump read the void's state, which a pulled line could not.
- **Alternatives:** the outlet draws from the pipe like any part (the pump could not stop on NOT ENCLOSED or FULL).

### 10. Line pressure
- **Chosen:** 8 kPa a metre of wall friction (proposed: so the pump's 8 MPa is reached at 1 km on the flat, as the spec has it), less
  the paste's own weight in the fall, 19.6 kPa a metre down (2.0 t/m³). Past 8 MPa it reads LINE PRESSURE. A line that falls far enough
  runs on its own weight (reads 0). The pump's PRESSURE point reads it while it pumps, 0 stopped.
- **Why:** real paste lines are sized by friction against the vertical drop (the "flow ratio" of line length to drop); 1-10 kPa/m is the
  real range of friction for paste.
- **Alternatives:** length only, as the spec's first draft (a deep borehole would be no help, which is wrong).

### 11. The pump's motor: 110 kW
- **Chosen:** 35 m³/h x 8 MPa / 0.7 is 111 kW; the motor is 110 kW, the standard frame, on the world's industrial tier. The filter and
  mixer are 75 kW each.
- **Why:** the spec's figures; motors come in standard frames.
- **Alternatives:** 132 kW (the next frame up, so it never runs past its rating).

### 12. Curing: eight steps of 2.5 minutes
- **Chosen:** wet paste sets in one game day (L5-3), in eight scheduled steps of 3,000 ticks; its step is in its block state, so paste
  moved by a spill keeps its age. While its chunk is not loaded it does not cure (the game holds a scheduled tick's time left);
  **since gaps2** it makes that time up when the chunk comes back (gaps2.md #6).
- **Why:** a single 24,000-tick tick would restart when the paste moves; steps cost about one block tick a second for a 20,000 m³ void.
- **Alternatives:** cure by the clock (a block entity per cubic metre, heavy for 20,000 of them); random ticks (no exact time).

### 13. What wet and set paste are like
- **Chosen:** wet paste has no collision: whatever walks into it sinks, slowed to half speed across and 40 % down; it does not suffocate.
  Set paste is a solid block, mined with a pickaxe or a shovel. Both drop nothing (their mass is tails, as the spec says) and are not
  items.
- **Why:** the spec ("slows what walks in it, like mud ... not a block to stand on"; "can be mined: it drops nothing").
- **Alternatives:** wet paste drowns (it is not water); set paste as hard as stone (real set fill is 0.5-1 MPa, far weaker than rock).

### 14. A broken bulkhead lets the paste out
- **Chosen:** when a fill bulkhead is removed by any means and wet paste touches the gap, every wet cell joined to it at or above the
  gap's floor (all of them, for a gap under the paste) runs out, highest first, and settles outside the gap by the settle rule; set
  paste stays. It happens at once, in one tick. Breaking a stone wall next to wet paste lets nothing out (the outlet then reads NOT
  ENCLOSED). **Since gaps2:** a stone (any solid) wall breached beside wet paste spills it too (gaps2.md #5).
- **Why:** the spec ("a bulkhead broken while the paste behind it is wet"); the real hazard is a barricade failing. Any block could be
  watched, but a wet void always has open cells beside its paste (its unfilled top), so telling a breach from them needs a scan.
- **Alternatives:** any wall block breached lets paste out (a void scan on every block broken next to paste); a spill spread over
  several ticks (smoother on a server for a very large void).

### 15. The settle rule and wet paste
- **Chosen:** spilled paste settles on paste (the rule's ground treats paste as solid). Muck and dry tails tipped onto wet paste rest
  on it: the game's free test is not about collision, and paste is not replaceable. (This said they fell into it and replaced it; the
  gaps2 wave checked it with a scene, `gaps_muck_on_paste`: they never did. gaps2.md #7.)
- **Why:** changing the shared rule would change how muck settles elsewhere; tipping onto wet fill is rare.
- **Alternatives:** make the shared rule see wet paste as solid (one line, touches the pit's settling).

### 16. Bulkheads: 4 cement and 5 gravel make 2
- **Chosen:** `fill_bulkhead`, a metre of shotcrete barricade: `GCG / CGC / GCG` (gravel, cement) makes 2: two bags, 100 kg, a cubic
  metre of wall.
- **Why:** a real shotcrete barricade about 0.3 m thick is roughly 120 kg of cement a square metre of opening.
- **Alternatives:** cheaper (a bag a block); timber bulkheads from planks (real for small openings).

### 17. Recipes of the parts (placeholders)
- **Chosen:** filter: steel, white wool (its cloth), a bucket, industrial cable; mixer: steel, a hopper, copper, cable; pump: steel, a
  piston, Wildcatter steel pipe, cable; outlet: steel pipe, iron and iron bars.
- **Why:** the same pattern as the other plant parts' placeholder recipes.
- **Alternatives:** none needed until the recipe pass.

### 18. PLC points
- **Chosen:** the pump has the plant motor's six points, then FLOW (4-20 mA over 0-40 m³/h) and PRESSURE (4-20 mA over 0-10 MPa). The
  filter and mixer have the motor's six only; their feed tanks are not on the slurry gauges. **Since gaps2** they have LEVEL, FLOW and
  DENSITY (gaps2.md #3).
- **Why:** the spec's pump points (RUN, RUNNING, FLOW, PRESSURE).
- **Alternatives:** LEVEL and DENSITY on the filter and mixer as the other slurry parts have.

### 19. The pace: a full stope takes days of game time
- **Chosen:** real rates. All of the plant's tails make about 34 m³ of paste an hour; the pump takes 35. A 360 m³ drive branch fills in
  about 11 hours of play, the largest void (20,000 m³) in about 600.
- **Why:** real paste fill runs for weeks to fill a stope; the spec's rates are real.
- **Alternatives:** count each placed block as more than a cubic metre, or a faster pump for the game. Your call: as built it is a
  long, background use of the tails, which is what it is in a real mine.

### 20. Paste is not stopped by what it buries
- **Chosen:** the outlet places paste only in air and water; it does not break blocks, kill mobs or move items in the void. A player
  or mob standing in a filling void wades in wet paste (#13).
- **Why:** destroying things silently in a player's world is worse than leaving them in the fill.
- **Alternatives:** paste pushes out items and mobs (more code, and still not real).

### 21. Art and sound
- **Chosen:** the three parts and the outlet drawn in Wildcatter's materials (`tools/models/paste.py`); four Ludo pictures (cement's
  icon, the bulkhead's shotcrete, wet and set paste; 4 credits), so no `_placeholder` texture. The filter and pump hum with
  Wildcatter's motor hum, the mixer with the agitator's loop; placed and spilt paste sounds as mud.
- **Why:** the art-wave practice; the parts are steel and paint, which the materials cover.
- **Alternatives:** a piston pump's thump from Ludo.

### 22. Existing worlds
- **Chosen:** nothing changes. Every block, item and part is new; the shared plant code (`PlantBlockEntity`) gained branches only for
  the three new parts (cement in the mixer's hopper, the pump's two points); the settle rule, the thickener and every other part are
  untouched.
- **Why:** the hard rule.
- **Alternatives:** none needed.
