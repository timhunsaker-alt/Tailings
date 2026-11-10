# Open pit P3, drilling and blasting — decided while Tim was away (2026-11-09)

The spec ([../multiblocks/open-pit-p3-blasting.md](../multiblocks/open-pit-p3-blasting.md)) is a draft decided on recommendation
(its choices are in [specs-tailings.md](specs-tailings.md), P3). Building it raised the questions below; each was decided on the best
recommendation and is open for Tim. New numbers are marked *(proposed)* in the spec's "As built" section. PROGRESS.md, "Open pit P3".

### 1. Is the drill four placed parts, as P3-2 chose?
- **Chosen:** no, for now: the drill is **one machine at its real size** (7 x 15 m, 26 m to the mast top), placed from an item and
  driven like the haul truck, with the hole under its mast at the back end. It is drawn by the vehicle generator.
- **Why:** a contraption of four parts (build check, lift into one entity, set down) is the TBM's machinery again, a wave of its own;
  everything a drill does in the pit (walk a pattern, drill to depth, burn diesel by work, stop and say why) works the same either way,
  and the pit's other machines are already single entities. The written exception to "big parts" stands until Tim chooses.
- **Alternatives:** the four parts as P3-2 chose (crawlers, deck, mast, dust collector), built when the rope shovel's contraption is
  built in P5, sharing its code.

### 2. Which way does a pattern run?
- **Chosen:** square to the world's grid. GO turns the drill to the nearest of the four ways; its back (the mast end) faces the free
  face, row 1 is under the mast, and the rows run on the way it faces; along a row the holes go to its right.
- **Why:** each hole's box breaks square to the grid (P3-5), so a pattern at an angle would leave saw-toothed edges; real benches are
  laid out to the crest, and a block world's crests are square. With the mast to the face the machine stands on unbroken bench, back
  from the crest.
- **Alternatives:** any angle, with boxes rounded to blocks (ragged); or the mast end leading away from the face.

### 3. How are the drill's pattern, the truck's stemming and the logger's delays set?
- **Chosen:** one small panel for all three (`PitPanelMenu`): a minus and plus for each setting, a few action buttons (GO, STOP, Drill
  one; Charge all in reach; Clear) and the status. Sneak-right-click the drill or truck with an empty hand; sneak-use the logger.
- **Why:** it needs no packets of its own (a menu's data slots and button clicks), works the same on both loaders, and covers the spec's
  "its screen" in each place.
- **Alternatives:** a separate screen for each with typed numbers (more code, nicer); settings by clicking with items (no screen).

### 4. How much stemming do a hole's cuttings make?
- **Chosen:** by the spec's own basis: the hole's volume less 25 % lost to the dust collector, bulked 1.0, is **0.75 of the hole's
  depth**, 12 m for a 16 m hole: more than any stemming setting. Gravel stems 13 m a block, as the spec says.
- **Why:** the spec's "enough for 3.4 m" does not follow from its basis (0.92 m³ less 25 % in a 0.057 m²-a-metre hole is 12 m). Real
  cuttings piles are plenty to stem with. Gravel is still needed for a hole whose cuttings are used up (washed out and charged again).
- **Alternatives:** keep 3.4 m (some other loss, about 80 % lost), so every hole needs gravel topping up.

### 5. How does a player stem a hole?
- **Chosen:** right-click the collar with an empty hand: it shovels in its own cuttings, up to the collar. Gravel tops it up 13 m a
  block. Sneak-right-click reads the hole.
- **Why:** the spec's "a stack of the hole's cuttings (on the collar, free)" with no cuttings item to carry; the cuttings stay at the
  collar, as real ones do.
- **Alternatives:** a cuttings item dropped by the drill (more inventory juggling).

### 6. Can a hole be primed after it is charged?
- **Chosen:** no: "the primer goes in first: wash the charge out to prime it". A misfired hole is the exception: a new booster and
  detonator go on its collar, and with both in it is no longer marked misfired, to be logged and fired alone (spec, Misfires).
- **Why:** a booster sits at the toe, under the column; it cannot get there through 12 m of emulsion.
- **Alternatives:** allow it (top priming: real, but less common, and it would make the order meaningless).

### 7. Which holes can the logger log?
- **Chosen:** any primed or charged hole, stemmed or not. ARM lists what will not fire and any hole stemmed under 2 m ("it will blow
  out").
- **Why:** the spec logs stemmed holes, but its own scene fires "an unstemmed hole [that] blows out"; that needs an unstemmed hole in
  the shot. ARM's test is the blaster's last check, as a real logger's is.
- **Alternatives:** log only stemmed holes (no blowouts possible except from short stemming).

### 8. A free face when a neighbour has broken first
- **Chosen:** a hole breaks toward its pattern's own free face whenever that face has open ground within burden + 2 m; only a hole
  with none there looks the other three ways (the nearest wins), and with none at all it is a box cut.
- **Why:** found building `pit_blast`: holes in the front row fire 25 ms apart, and the second found its broken neighbour 5 m away,
  nearer than the face, and broke sideways, leaving rock standing. Real holes in a row fire to the face.
- **Alternatives:** nearest face always (echelon firing, harder to plan).

### 9. What is the subdrill after the shot?
- **Chosen:** the hole's column below the bench floor turns back into the rock it replaced. Its box stops at the bench floor (P3-6).
- **Why:** "fractured, not broken": the floor stays flat at grade, and no blasthole blocks are left in the floor to confuse the next
  bench's drill.
- **Alternatives:** leave the subdrill as spent blastholes (a marker of where holes were).

### 10. What breaks in a hole's box?
- **Chosen:** a block a pickaxe digs, with a collision box, that is not soft ground, not unbreakable, not obsidian or reinforced
  deepslate, and has no block entity. Muck, fluid and soft ground are left. *(As built after the first full run: rock first broke
  whatever had a collision box, which would have turned planks or glass into "muck"; the pickaxe test keeps it to rock and stone.)*
- **Why:** the spec's list (hard rock and ore) as a rule; it must not grief what is not rock.
- **Alternatives:** an explicit list of rocks (would miss other mods' stones).

### 11. Machines in the shot
- **Chosen:** ARM and FIRE refuse while any block entity other than a blasthole or muck, or any Tailings or Wildcatter vehicle or
  contraption, is inside a hole's box: "MACHINE IN THE SHOT: ... : move it".
- **Why:** P3-7; a chest is protected as a machine is.
- **Alternatives:** refuse only for Tailings and Wildcatter blocks (a block with a block entity is never broken either way, so a
  player's chest would stand in the muck rather than stop the shot).

### 12. Flyrock in flight
- **Chosen:** no flying entity. Each piece's landing point (40-100 m off, forward ± 30° or any way for a blowout) and its flight time
  on a 45° arc are worked out when it is thrown; when the time is up it lands by the settle rule as muck of one quarter, breaking glass,
  panes and leaves it lands on, and anything within 1 m takes 10. A piece landing in an unloaded chunk is lost (noted).
- **Why:** a falling-block entity for its flight needs a renderer and loader wiring for a few frames of sight; the effect on the world
  is the same.
- **Alternatives:** a small flyrock entity drawn as a muck block (the spec's "falling entity for its flight only").

### 13. Overcharged flyrock
- **Chosen:** an overcharged hole (over 1.0 kg/t) throws **3** pieces forward *(proposed)*, as short burden does.
- **Why:** the spec names the cause but not the number.
- **Alternatives:** scale with the charge over 1.0 kg/t.

### 14. The firing point's controls
- **Chosen:** a plain block (not a big part): click with the logger to load, with the key to put it in; empty hand ARM, then FIRE;
  sneak with an empty hand takes the key out (or reads it). The danger-zone check skips spectators.
- **Why:** a blasting machine is a box a blaster carries; one cell is real size.
- **Alternatives:** a panel with ARM and FIRE buttons.

### 15. Sounds
- **Chosen:** no new sound files. The drill's rotary head is the TBM's cutter loop; the truck's pump is Wildcatter's motor hum; the
  siren is vanilla's raid horn, every 2 s for 10 s; the blast is vanilla's explosion, once, loud, at the shot.
- **Why:** the brief: reuse, and vanilla's explosion where a blast sound is needed.
- **Alternatives:** recorded drill, pump, siren and blast sounds in the art wave (`drill_rotary`, `blast` in the spec).

### 16. Camera shake
- **Chosen:** left out (Known gaps).
- **Why:** it is client-side camera work on both loaders for a small effect; the dust, the sound and the muck show the blast.
- **Alternatives:** a short shake for players within 200 m, as the spec proposes.

### 17. The drill's PLC points
- **Chosen:** left out in P3 (Known gaps): the drill is an entity, with no cell for a control cable to land on.
- **Why:** follows from decision 1; the parts version has its main cell.
- **Alternatives:** a radio point on the dispatch office, as the trucks report.

### 18. New figures *(proposed)*
- The drill: about **110 t**, turning on the spot at **9°/s**, by hand on a 5 m radius. The truck: about **14 t** empty, **40 km/h**,
  the pump at **10 %** of its engine. The fill point holds **1,000 kg** drawn ahead from its line. The flyrock lands **40-100 m** off.
- **Why:** the spec leaves them open; each is a round figure for the class.

### 19. Wildcatter 0.6.0
- **Chosen:** `wildcatter_version=0.6.0` (the ammonia plant's `EMULSION_MATRIX`), built against the local release folder until it is
  on GitHub.
- **Why:** the fill point takes Wildcatter's matrix by pipe; it does not exist before 0.6.0.
- **Alternatives:** none: P3c needs it.

### 20. The blasting machine keeps its own chunk running (found filming, by the lead)
- **Chosen:** from FIRE until the shot is done, the machine holds a runner ticket on its own chunk (as it already did on the holes').
- **Why:** the film showed a siren that never ended: the firing point stood 120 m from the bench, out of simulation distance while
  the camera watched the bench, so the machine never ticked and never fired. Real firing points stand well back, and the shot must
  not depend on where the player looks.
- **Alternatives:** require the player within simulation distance of the machine (the countdown would freeze when they walk off).

### 21. A self-running loader does not edge into its face (left as a gap)
- **Chosen:** not changed. In the film the loader is driven by hand and its driver edges forward when nothing is in reach. A
  self-running loader (LOAD stake) still reads "face worked out" once the toe is a few metres off.
- **Plan:** edge forward up to 5 m past its LOAD stake along its heading, a metre at a time, while the path is clear; then
  "face worked out". Recommended for P5 with the shovels, which work a face the same way.
- **Alternatives:** the player moves the LOAD stake (today's way).
