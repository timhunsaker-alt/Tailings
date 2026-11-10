# L4: the shaft and hoist (2026-11-09), decided while Tim was away

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back".
Built on branch `l4-shaft` from the draft spec [shaft-and-hoist.md](../multiblocks/shaft-and-hoist.md), whose own choices are in
`specs-tailings.md` (L4-1, L4-2). Format as in `README.md` here. **#1, #5 and #20 most need your eye.** Nothing here changes an existing
world (#21).

### 1. How much of the spec is built
- **Chosen:** the core: the headframe, the hoist house (a real 450 kW motor on Wildcatter's circuit), two skips in balance running the
  shaft, the measuring flask at the bottom fed by the L3 bottom-dump station's apron feeder, and the headframe bin at the top feeding a
  belt. The raise borer, the cage and the shaft as a return airway are not built; each is planned in the spec's "Still to build".
- **Why:** the hoisting loop is what turns the shaft into a way out for rock, and everything else hangs off it. The raise borer is a
  whole machine (a 400 kW part, a pilot and reaming phases, cuttings into the drive); the cage needs player riding and landing calls.
- **Alternatives:** build the raise borer first (a shaft with nothing to hoist); a smaller version of all three.

### 2. Until the raise borer: the shaft is dug by hand
- **Chosen:** the hoist does not care how the shaft was made. It checks that both skip compartments are clear (nothing with
  collision) from the flask's bottom up to the collar; otherwise NOT BUILT: "the shaft is blocked at y N".
- **Why:** lets the hoist be used now, and the raise borer later only has to clear the same cells.
- **Alternatives:** require a shaft the raise borer made (no shaft until L4b); require lining.

### 3. The headframe is a full 9 x 30 x 9 box with the shaft inside it
- **Chosen:** the headframe's footprint is the whole box; the cells over the shaft (x and z 2 to 6 in its own frame) have no collision,
  so the skips run up through it to the dump. The collar has 1.5 m gate rails round the shaft (with collision), so a player cannot walk
  in.
- **Why:** Wildcatter's big parts take any footprint, but a box is what every other Tailings part is, and an empty-collision core
  costs nothing. The rails keep the shaft from being a death trap.
- **Alternatives:** a footprint with a hole (the shaft cells free for other blocks, which would then block the skips anyway).

### 4. Skips are entities, hung by clicking the headframe with a skip
- **Chosen:** a `skip` item (placeholder recipe) clicked on a headframe hangs a `ShaftSkipEntity` in the first free compartment (west,
  then east), at the dump. The hoist house finds them and sets their height every tick; their loads ride in the hoist's winder (saved
  with the hoist house). A sneaking empty-hand hit takes a skip off its rope as its item; a skip lost so loses its load ("down the
  shaft"). The skip is drawn from its own 2 x 4 x 2 block model, as the TBM's gantries are, with its rope up to the sheave and back to
  the drum.
- **Why:** the skip moves 150 m and more; only an entity can glide that. One owner of the state (the hoist) keeps the two skips in
  balance by construction.
- **Alternatives:** the skip as a big part lifted into a contraption each wind (the TBM's way; far more code for a part that is
  always moving); skips that come with the hoist (no NOT BUILT for a missing skip, which the spec tests).

### 5. The cycle: one 10 s rest, not 10 s at each end
- **Chosen:** at each end of a wind the hoist rests 10 s (proposed), in which the skip at the top dumps and the skip at the bottom loads
  at the same time. A 150 m wind is 28.3 s, so a cycle is 38.3 s and the hoist lifts **282 t/h**, not the spec's 225.
- **Why:** with skips in balance the two ends work at once; the spec's "10 s at each end, about 48 s" counted the rest twice. Real
  balanced-skip cycles add one rest of 8-15 s to the wind.
- **Alternatives:** a 20 s rest (the spec's 225 t/h).

### 6. What "150 m" means: the wind is the depth plus the dump height
- **Chosen:** the skip loads with its bottom at the flask's bottom and dumps with its bottom 18 m above the collar (proposed: the scroll
  in a 30 m headframe). A 150 m shaft is a 168 m wind: 40.1 s a cycle, 269 t/h (the server scene measures 39.75 s, 272 t/h).
- **Why:** a real skip is lifted into the headframe to dump.
- **Alternatives:** dump at the collar (a 30 m headframe with nothing in it).

### 7. The motion: trapezoid, braked on the stopping curve, the last metres at creep
- **Chosen:** 0.75 m/s² up to 10 m/s; it brakes on v = sqrt(2 a d) so it arrives exactly; under 0.5 m/s (proposed) it creeps in. A wind
  too short for full speed is a triangle.
- **Why:** real drum hoists follow a set speed profile with a creep into the dump and the loading position.
- **Alternatives:** a constant speed (unreal, and the skips would slam).

### 8. The motor on the circuit
- **Chosen:** a Wildcatter `Motor` of 450 kW at the world's medium-voltage tier (never a hard-coded voltage), its box on the west face
  of the hoist house's main cell as every Tailings motor. It is wanted while it winds and, as every plant motor since the playthrough
  fix, runs on 60 s through the rests. The rope speed is the motor's output times 10 m/s (a sagging supply winds slower). It draws its
  rating while running: Wildcatter 0.5.1's motor has no part load, so the lift's real 346 kW plus acceleration is not modelled.
- **Why:** the spec's 450 kW, on the same motor every plant part uses.
- **Alternatives:** a drive-fed motor (Wildcatter's drives) for the speed profile; a load that follows the winding power (needs a
  Wildcatter change).

### 9. No power: the brakes hold
- **Chosen:** if the motor stops mid-wind (no supply, OFF, a PLC stops it) the skips stop where they are (STOPPED: the brakes hold the
  skips) and wind on from there when it runs again.
- **Why:** real hoist brakes set on power loss.
- **Alternatives:** an emergency stop that lowers them to the nearest end.

### 10. The flask: a 2.7 t surge, measured out at 3 t
- **Chosen:** the apron feeder drops one rock item (a 2.7 t block) into the flask's surge whenever the surge is empty, once a second;
  the flask fills from it at 300 kg/s (proposed) until it holds exactly 3 t, which the bottom skip takes whole. It takes nothing by hand
  or hopper.
- **Why:** rock moves as items before the plant and as kilograms in it; the surge turns whole items into exact 3 t loads, as a real
  measuring flask does with a surge bin above it.
- **Alternatives:** a 2.7 t skip (one item a skip, an unreal payload); hand-fed flasks.

### 11. The apron feeder feeds a flask only where there is no sorter
- **Chosen:** a feeder with an ore sorter at its tail works exactly as before. With none, and a measuring flask touching it, it feeds the
  flask. Its NOT BUILT readout now names both ("an Ore Sorter (or, at a shaft bottom, a Measuring Flask)").
- **Why:** the hard rule: no existing dump station may change. A station with a sorter cannot see the new branch.
- **Alternatives:** a new feeder part for the shaft bottom (a second feeder model for the same machine).

### 12. The headframe bin holds kilograms and feeds a belt
- **Chosen:** 100 t of rock by rock and size (as the plant's belts carry it), out of its bottom onto a belt under it at the belt's rate.
  A hopper under it takes nothing. **Since gaps2** a hopper or chest under it (with no belt) takes its rock as items (gaps2.md #4).
- **Why:** the spec: "feeding a belt to the plant". The plant's belts already carry kilograms.
- **Alternatives:** an item bin like the ore bin (8,192 items), so hoppers could empty it.

### 13. Where the hoist house stands
- **Chosen:** behind the headframe: on its rope side (its own south), across from the shaft, within 60 m (proposed), in line: the hoist
  looks out of each of its four sides along its centre line, at each of its heights, for the headframe. Its own facing does not matter.
- **Why:** the ropes leave the sheaves to the south; a real hoist stands in line with its sheaves.
- **Alternatives:** require it to face the headframe (players place parts facing themselves, so it would often be wrong for no reason).

### 14. Where the flask stands
- **Chosen:** with any of its cells in the ring of cells round the shaft, anywhere down to 200 m below the collar (proposed); the first
  found from the top is the loading pocket. Only one level.
- **Why:** the spec's "beside the shaft" at the bottom; several levels are left out.
- **Alternatives:** a fixed side for the loading pocket.

### 15. The bin is any headframe bin touching the headframe
- **Chosen:** any side. The model's dump chute leaves the north side, so a bin there looks right.
- **Why:** a player who puts it elsewhere is not stopped for a cosmetic reason.
- **Alternatives:** the north side only.

### 16. The hoist waits for both ends
- **Chosen:** at rest it winds when the bottom skip is loaded and the top skip is empty: BIN FULL while the top skip cannot dump,
  WAITING FOR ORE while the flask is short of 3 t. The first skip loads straight away (no rest at start).
- **Why:** the spec's readouts; a hoist never winds a skip up into a full bin.
- **Alternatives:** wind empty skips anyway (a balanced hoist does not need to; pointless wear).

### 17. PLC points
- **Chosen:** the plant motor's six (RUN, RUNNING, STALLED, AUTO, AMPS, FILL), FILL being the headframe bin's level (4-20 mA), and
  SKIP DUMPED, on for a second after each dump (a PLC counter counts it).
- **Why:** the spec's RUN, RUNNING, SKIPS pulse and bin LEVEL, on the points every motor already has.
- **Alternatives:** a separate LEVEL point and a counter output.

### 18. Recipes (placeholders)
- **Chosen:** headframe: iron blocks and iron bars (`B B / IBI / B B`); headframe bin: iron around a hopper; hoist house: iron blocks
  round a copper block; measuring flask: iron and a hopper; skip: iron, iron bars and a hopper.
- **Why:** the same pattern as every placeholder recipe; the headframe and hoist cost more as the biggest parts.
- **Alternatives:** steel from Wildcatter once it has a steel item.

### 19. Art
- **Chosen:** all five models from `tools/models/shaft.py` in Wildcatter's Ludo materials (no new textures, no `_placeholder`); no Ludo
  credits spent. The rope is drawn in black concrete's texture; the load in the skip in gravel's. The hoist house sounds Wildcatter's
  motor hum.
- **Why:** the parts are steel, concrete and paint, which the materials already cover.
- **Alternatives:** a hoist sound of its own (a winding hum rising with speed) from Ludo.

### 20. The scale of a car against a skip
- **Chosen:** left as it is: a skip lifts 3 t; a mine car holds 576 items, 1,555 t at the plant's 2.7 t a block. At 270-280 t/h the
  hoist lifts a car in about 5.6 h and a 10-car train in 2.3 days, though it is still more than five times the plant's 50 t/h.
- **Why:** the hoist is right in tonnes; the car's 576 items are the old haulage scale (haulage.md's "real Granby car is about 3 t" is
  documentation only). Changing either changes existing worlds or the spec's real figures.
- **Alternatives:** a bigger skip for the game (say 30 t, the potash shaft's class); counting a car's item as less than a block. Your
  call: the shaft feeds the plant at five times its rate either way.

### 21. Existing worlds
- **Chosen:** nothing changes. The five new parts and the skip are new; the only shared code touched is the apron feeder's tail end
  (#11), which takes the new branch only where there is no sorter.
- **Why:** the hard rule.
- **Alternatives:** none needed.
