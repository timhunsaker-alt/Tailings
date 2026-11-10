# gaps2: known gaps closed by adding behaviour (2026-11-10), decided while Tim was away

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back".
Built on branch `gaps2` from `main`. The task: close the Known gaps in `ROADMAP.md` that can be closed purely by adding behaviour,
leaving the ones that wait on a design choice of yours. Format as in `README.md` here. **#4, #5 and #6 would change what an existing
world does, so they are built but off by default**, each behind a key in `config/tailings-rules.properties` (the mechanism
ventilation's rules use: `OptInRules`, read once a server run, missing file or key is off). The question on each is whether it should
default on. The rest only add.

### 1. The fuel bay's pipe intake has a scene (P2a)
- **Chosen:** no code change. `gaps_fuel_bay_pipe`: a Wildcatter stock tank of diesel on steel pipe to the bay's side; the bay's day
  tank fills from it through `Lines.pull` once a second, and the stock tank goes down by as much.
- **Why:** the Known gap was only that the intake had never been run in a scene; it works as written.
- **Alternatives:** none.

### 2. The press's and the load-out's own PLC points (P4)
- **Chosen:** the tailings filter (the press) adds, after the motor's six, 6 CYCLE (a pulse counter, a pulse each cake drop) and 7 FULL
  (on while it holds cake with nowhere to drop it). The load-out frame (it has the gate's motor; the bin has none) adds 6 LEVEL (4-20 mA
  over the bin's 400 t), 7 FULL (the bin's own FULL: within a block of 400 t) and 8 LOADING (on while the gate fills a truck).
- **Why:** the spec's points (`open-pit-p4-dry-stack.md`: RUN, RUNNING, CYCLE, FULL; LEVEL, FULL, LOADING). They are added after the
  existing six, so a PLC already wired to points 0-5 reads the same; the counter follows the X-ray sorter's PRODUCT and WASTE.
- **Alternatives:** CYCLE as an on-for-a-second bit like the hoist's SKIP DUMPED (a PLC must then count it itself); the load-out's
  points on the bin (it has no motor, so no selector or points today).
- **Tim (2026-11-10):** CYCLE becomes a one-second bit. Built: on for a second at each cake drop, as the hoist's SKIP DUMPED (answers-2026-11-10.md #14).

### 3. The paste filter's and mixer's slurry gauges (L5)
- **Chosen:** both join the slurry parts: LEVEL, FLOW and DENSITY as points 6-8, the inspection's "slurry level, flow, density", and a
  transmitter reads them. They read their feed tank of thickened tails (the filter's feed, the mixer's trim), over the same hold as the
  other slurry parts. DENSITY now uses the solids a tails slurry's assay is marked with (`tailings:solids`: thickened tails 60 %, 1.61
  kg/L; paste 78 %, 1.97) instead of plain tails' 35 %. The filter's FLOW reads 0: its cake goes to the mixer and its filtrate is
  water, so nothing leaves it on the slurry pipe.
- **Why:** l5-paste.md #18's alternative; every other slurry part has them. Without the marked solids the gauge would read thickened
  tails as plain tails.
- **Alternatives:** gauge the mixer's paste hopper instead of its trim (the more useful level for running the pump, but not what the
  other parts' LEVEL means). The marked-solids density applies to every gauged part: a part fed a thickened tails slurry would now read
  its real density. Nothing in the base plant puts marked tails into a gauged part, so no existing reading changes in practice.
- **Tim (2026-11-10):** the mixer's LEVEL reads its paste hopper. Built (its DENSITY reads the same hopper) (answers-2026-11-10.md #14).

### 4. The headframe bin feeds a hopper under it (L4): built, off by default
- **Chosen:** with `headframe_hopper=true` in `config/tailings-rules.properties`, and no belt under it, a headframe bin puts each whole block's worth (2.7 t) of one rock and size into a vanilla
  container (a hopper, a chest) under its bottom, as that rock's item (the block for unbroken rock, the crushed or gravel item
  otherwise), at the belt's rate (one item each 65 s). Less than a block of a rock stays in the bin. A belt under it still comes
  first.
- **Why:** l4-shaft.md #12's alternative, done the way a belt's head end already puts rock into hoppers and chests (R10), so the rule
  for rock becoming items is the same one.
- **Alternatives:** leave it belts only. **Existing worlds:** turned on, a hopper or chest already standing under a headframe bin, with
  no belt there, will start to fill with rock from the bin. It took nothing before, so this is the fix of a dead interaction; off (the
  default) nothing changes. **Should it default on?** Recommended yes: nothing worked there before.
- **Tim (2026-11-10):** a per-world rule, on in new worlds and off in existing ones. Built: the world rule `headframe_hopper` (answers-2026-11-10.md #23).

### 5. A stone wall breached beside wet paste lets it out (L5): built, off by default
- **Chosen:** with `paste_wall_breach=true`, when a solid block next to wet paste is removed by any means (mined, blown up, cut) and open ground (air, water,
  anything replaceable) takes its place, the wet paste spills out of that gap exactly as out of a broken bulkhead: the wet cells joined
  to it at or above the gap's floor run out, highest first, by the settle rule; set paste stays. It is found from the paste's own block
  updates (the block that was there, then the block that is there now), with no scan, and the spill runs once that update is done.
  A wall changed for another solid block, a torch or the outlet removed, and the paste moving itself let nothing out.
- **Why:** l5-paste.md #14's alternative, without the void scan it feared: the paste learns which neighbour went and what replaced it,
  so open cells inside the void (its unfilled top) are never mistaken for a breach.
- **Alternatives:** keep it bulkheads only. **Existing worlds:** mining or blasting into a void that holds wet paste now spills it,
  where before the wall could be opened with nothing coming out; off (the default) only a bulkhead spills, as before. **Should it
  default on?** It is the real hazard the spec describes; recommended on for new worlds, your call for existing ones. The spill still
  happens in one tick (l5-paste.md #14).
- **Tim (2026-11-10):** a per-world rule, off even in new worlds. Built: the world rule `paste_wall_breach`, off by default (answers-2026-11-10.md #23).

### 6. Paste cures while its chunk is unloaded, caught up on its return (L5): built, off by default
- **Chosen:** with `paste_unloaded_cure=true`, a per-dimension record (`paste.PasteClock`, saved with the world) of when each chunk's wet paste last stepped. A chunk's
  wet paste steps at least every 2.5 minutes while it is loaded, so a longer gap is time it was unloaded; each wet block in the chunk
  gets that time's whole steps on its first step back (within one step of the chunk's return). A day or more away and it is set.
- **Why:** the spec's "sets in one game day" held only while a player stayed near the stope; scheduled ticks keep their time left while
  their chunk is unloaded. One record per chunk, no block entity per cubic metre (l5-paste.md #12's reason for the scheduled steps).
- **Alternatives:** leave it (paste far from players stays wet indefinitely); cure by the clock with a block entity per cube (heavy).
  **Existing worlds:** a chunk with no record yet gets nothing made up; its record starts at its first step with the rule on. So paste
  already waiting in an unloaded chunk is not jumped forward; from then on it cures by the game's clock. Off (the default), paste waits
  with its chunk as before. **Should it default on?** Recommended yes: one game day is the spec's rule, and it only stops paste
  waiting for a player.
- **Tim (2026-11-10):** a per-world rule, on in new worlds and off in existing ones. Built: the world rule `paste_unloaded_cure` (answers-2026-11-10.md #23).

### 7. Muck tipped onto wet paste: it already rests on it (L5)
- **Chosen:** no code change; the Known gap was wrong. The game's free test (`FallingBlock.isFree`) is air, fire, liquid or a
  replaceable block, not "no collision", and paste is not replaceable, so the settle rule (`pit.Loose`) already rests muck and dry
  tails on wet paste, and settled muck stays on it. `gaps_muck_on_paste` keeps it so. l5-paste.md #15's second sentence is
  corrected.
- **Why:** checked by running the scene against the unchanged settle rule: it passed.
- **Alternatives:** none.

### 8. Gaps looked at and left
- **DMS floats on a belt (L7, l7-dms.md #11):** the floats would ride the belt as plant rock, keeping their minerals. The light ores
  (rock salt, potash, borax, graphite, bauxite) float at any setting, so a belt would let them go round to the plant and be recovered,
  which today they are not (l7-dms.md #4, which waits on you). That is balance, so it is left.
- **Battery locomotive PLC points (L2):** a locomotive is a moving entity with no control cable; the spec puts its points on the
  charger, which has them (CHARGING, FULL, CHARGE). Left; the Known gap is reworded to say so.
- **Every other Known gap** is a design choice waiting on you (charge times, paste pace, rules off by default, the dry mill's share,
  placeholder recipes and products, the scale of skips) or art and sound, and is left as it was.
