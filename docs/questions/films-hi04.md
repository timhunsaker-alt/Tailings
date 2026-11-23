# Films for Heavy Iron episode 4, "Dozers and the dry stack" (branch `films-hi04`, 2026-11-23)

Decided while Tim was away, on the best recommendation; each is open for review. The series plan is
`videos/longform/heavy-iron/PLAN.md`; the episode is `videos/longform/heavy-iron/ep04/` (facts in its `ledger.md`).

## Decisions
1. **Two sites, one dozer.** The plan's "thin for a whole episode" is met by pairing the dozer's two jobs: a waste dump (the rope
   shovel's waste, the ultra truck, a 2 m berm, the pour down the face) and the plant's dry stack (thickener, press, load-out, a
   100 t truck and a spread lift). About 8 minutes, 37 paragraphs.
2. **The rope shovel's loading is episode 2's film**, `hi02_passes`, labelled "From episode 2". Re-filming the shovel would need
   episode 2's gas turbine yard again for one line; the ultra here is loaded off camera (360 t of waste rock) and says so.
3. **The dozer is driven by the scene's keys.** It has no autopilot in the game (a player drives it; the p4 doc says it does not run
   itself). The scene presses the same keys a player would (`testDrive`), and every dozer film's card says "driven by the scene's
   keys, as a player drives it"; the narration says it too. An autopilot for the dozer is a feature, not a film fix: needs Tim.
4. **Staged and said:** the windrow of waste along the crest (the berm's material) and the 18-block pile short of the crest, laid by
   the scene; the dump's talus of earlier tips; the stack's first 1 m lift; the tails fed into the thickener as from the plant; the
   load-out bin stocked to 100 t before the truck came; the villager (no AI).
5. **Real power:** the dozer and both trucks burn their own diesel (tanks filled at the start, never topped up while filming, so the
   card's kW is from the burn); the press and the load-out gate run on one 500 kW Wildcatter diesel set, the thickener on a second,
   as in Rock to Metal episode 8. Nothing force-run.
6. **The berm is 2 m, for the ultra:** the game's rule is the wheel's radius in whole blocks (2.0 m for the ultra), after the US rule
   of mid-axle height. The dozer's CARRY setting is put at "berm 2 m" with the test hook a player's sprint key steps to.
7. **The stack is spread, not bermed.** The 100 t truck's dump point on the stack is a paddock (no crest within 10 m), so the dry
   stack shows the other half of the dozer's work, SPREAD, laying the tipped load as a 1 m lift at the lift's edge. The berm is shown
   once, at the waste dump.
8. **The cake's figure:** the press fills a batch up to 15 t over its pressing time, and the staged feed is a little short of 60 t/h,
   so the filmed cake is 12.9 t (the first, part-fed cycle 6.5 t). The narration says "a cake holds up to fifteen tonnes ... full,
   that is sixty tonnes an hour", and the drop is filmed at the end of the second cycle.
10. **The thickener's card is cropped out** of its shot: its underflow line read 0 % (it looked at the pipe's side, emptied every
   tick); the 60 % is in the hero, from the code. A scene-card bug only, not the game's.
11. **Rewordings for the voice:** "The ridge it tips over, the berm" (heard "rigid") became "The berm along the edge"; "the load-out's
   gate" (heard "load-ounce") became "the gate of the load-out"; the cake line (decision 8). Spanish: "que da forma al lugar"
   (heard "queda a forma") became "que acomoda todo eso en su lugar"; "la pala de cables cargó" (heard "escargó", then "pana")
   became "vimos a la pala de cables llenar". Accepted after inspection: function words and numbers under 0.4 with exact transcripts
   (rock, ultra, five; y, 5, 2, sin).
12. **Music:** episode 1's Heavy Iron bed again (no new credits).
9. **Spanish:** "Topadoras y la pila seca", Brian Spanish (Venezuelan, usted); new glossary words under Heavy Iron in
   `es_glossary.md` (topadora de orugas, hoja, garras, zapata, corte, acarreo, esparcir, cordón, capa, descarga en patio, ...).

## Fixed (Tim's rule of 2026-11-23: fix weak spots, then film)
- **`pit_dozer_cut` was flaky** (ROADMAP, since 2026-11-18; it failed in episode 3's full run). Cause: cutting at 10 % the cut steps a
  block down every 10 m, but the dozer's 8.8 m box comes down into the last step only once its tail clears it, so for a few ticks the
  blade cuts two blocks under its tracks; the crest check under its nose (two blocks under the tracks) then saw its own cut as a crest
  and stopped it, "at the crest: pushed the load over", at 11-19 m. The timing of its fall made it intermittent. Now, cutting, the
  crest is looked for under its own cut's floor (`TrackDozerEntity.beforeMove`); a real crest is still found.
- **The tracks did not show running from the side.** Commit 42a6d3f5 slid the grousers along the ground and top runs, but from beside
  the machine the ground run is under the track and the top run under the fender: the track read as a still slab. Each shoe now has
  its end drawn on the track's outer side, a pixel proud, sliding with its run (`tools/models/pit_vehicles.py`, `TrackDozerModel`
  regenerated). The sprockets and idlers already turned.
- **The dozer did not run itself:** driven by the scene's keys and said so (decision 3).
- **Thin for an episode:** paired with the waste dump and the pour (decision 1).

## Weak on camera, left as is
- The dump's face and the stack are flat-shaded blocks: a vertical 4 m crest over a stepped talus, and a beige slab of dry tails.
- The dozer's blade load is drawn as one box; the berm, windrow and pile are whole muck blocks.
- The load-out film: the truck creeps 80 s to the gate (the episode starts it at the gate); the game ran at 96 % in that film, so it
  is played at 1.04x.
- The press's model does not open or move through its cycle; the cycle shows on its card and the drop only as the bin's figure.
- The closing aerial of the dry stack is far: the new lift's edge is small.
- In the press film the card says "15 t each" while the bin shows the first cake's 6.5 t (decision 8).

## Needs Tim
- **A dozer autopilot** (decision 3): a route for berms and spreading, as the trucks have, so a dozer can run itself. A feature.
- **Compaction** (heavy-iron-fixes 8, still left out): real stacks are spread thinner and compacted.
- **The press model** could open its plates and drop a visible cake (an art and renderer task).

## Tests
`build` passes. The full NeoForge suite on this branch (main 32067d03 merged in, with the dozer crest fix): all 306 required tests
passed, `pit_dozer_cut` among them (it had failed in episode 3's and earlier full runs). The client scenes (TAILINGS_ONLY=hi04) ran
four times; films kept from runs 3 and 4.

## Fixed after release (2026-11-23)
Branch `fix-weak-spots-hi04` (off main 7e6fe828). Tim: "Let's keep fixing weak spots as we find them." Decided on the best
recommendation while Tim was asleep; each is open for review. Re-filmed (fix runs 1 and 2) and episode 4 rebuilt in English and
Spanish with the same narration (shots swapped and in-points moved, nothing re-voiced). Footage notes: `videos/footage/films-hi04/NOTES.md`.

1. **The thickener card reads 60 % (scene readout bug).** The card read the underflow's solids off the thickener's outlet and its
   buffer. The press draws the outlet empty as fast as the thickener makes it (up to four ticks of flow a tick against about 1.2 kg
   a tick made), so the outlet was always empty when the card looked, and the buffer holds the 35 % feed, which carries no solids
   mark: 0 %. The game was right; the scene was reading the wrong place. Now the card reads the marked solids on the outlet when the
   cone holds some, else in the press's feed buffer, the same slurry piped up to it (`Hi04Scenes.solids`). `hi04_thickener`
   re-filmed: "Underflow: 60 % solids, piped up to the press" from the first frame, so the episode no longer crops the card out of
   paragraph 26; its hero (60 %) stays on "sixty percent".
2. **A full 15 t cake (the press held for a full cone).** The short cake was not the scene's feed: the thickener works the plant's
   50 t/h (`PlantRates.TONNES_PER_HOUR`, `Flotation.FEED_PER_TICK`), and the press is sized for 60 t/h (`DryStack.FILTER_KG_PER_HOUR`),
   so with no buffer a batch ends at 12.5 t (12.9 t filmed, with what the pipe held). Feeding the thickener more does nothing (it
   takes its rate, no more). Chosen: as a plant runs a press bigger than its feed, the scene holds the press (its selector to OFF)
   25 s into each cycle, after it has closed, until the thickener's cone holds its 20 t of underflow (`UNDERFLOW_HOLD`), then runs
   it again (`Hi04Scenes.holdPress`, about 7.5 minutes a batch). The filmed batch fills to 15.0 t; the cake drops 15.0 t into the
   bin (15.5 to 30.5 t). Said on screen: the press card's note "Held by the scene till the thickener's cone is full", the drop
   shot's label ("Real time · press held by the scene for a full cone"; Spanish "filtro detenido por la escena hasta llenar el
   cono"), and both descriptions. Rejected: topping the press's feed up with thickened tails from nowhere. The card's "Cakes
   dropped: n (15 t each)" now says "up to 15 t each" (the first, part-fed cake is in the bin). **Narration unchanged:** "A cake
   holds up to fifteen tonnes" is true and now matches the drop exactly; no re-voice needed.
3. **The press opens and drops its cake (renderer and model).** Small enough to do: the press's 78 plates, its moving tail and the
   ram's rod came out of the static model (`tools/models/pit.py`, rebuilt with `build.py pit` only; the other pit models came out
   byte-identical) and are drawn by `PlantRenderer.press`, timed by the cycle the server now sends every 2 s and at each drop
   (`PlantBlockEntity.getUpdateTag`, `pressSeconds`; the client runs it on while the motor runs). Closed through filling, squeezing
   and blowing; at 7:30 the tail draws back half a metre over 20 s, then the plates are shifted one at a time from the tail end to the
   head, one every 5.6 s, 2.5 s each, so the open gap travels down the pack; as each chamber opens its cake stands a second, breaks
   into lumps and falls through the floor; the last, by the head, falls just before the cycle ends, when the game counts the cake into
   the bin (bin figure and fall on the same words); the
   ram closes the pack again over the next cycle's first 20 s. For the cakes to fall, the floor is now open under the plates over a
   galvanised trough that steps down to the chute into the bin, and the side beams are top rails the plates hang from (they were 1.9 m
   walls that hid the plates). The timing is the game's own phases (`DryStack.FILL/SQUEEZE/BLOW_SECONDS`); a real shifter takes a few
   seconds a plate, as here. `hi04_press` re-filmed from above the press's floor and `hi04_drop` from just south-east of the head
   at the plates' height (the first takes looked up at its underside, where no plate showed).
4. **The load-out film is real time.** Re-filmed: 68.1 s by ticks and by the clock (the first take ran at 96 % and played at 1.04x).
   It also now starts 12 s before the truck stops (the first take's first 80 s were the truck creeping to the gate, off the edit);
   the shot plays at 1x with the stop on "stops with its body under the gate".
5. **The aerials are nearer and lower.** `hi04_dump`: 42 m out and 12 m up (was 70 m and 38 m), from over the dump's floor: the
   stepped talus, the 4 m face, the berm along the crest and the new tip read. `hi04_stackwide` (the closing shot and the end card):
   30 m out and 9 m up from north of the lift's edge (was 75 m and 40 m): the lift's 1 m edge, the strip the dozer spread and the
   dozer on it in front, the load-out, press and thickener beyond the road. The dump aerial also carries the cold open's title card
   background and paragraphs 2 and 20, which change with it.
6. **Shots swapped, narration kept.** Paragraph 26 shows the thickener's whole frame (no crop). Paragraph 31 is two shots of the
   new `hi04_drop`: zoomed on the plates by the head while the last plate shifts on "the press opens" and its cake falls on "the
   cake drops", then the full frame just as the card's bin figure jumps (15.5 to 30.5 t, "The cake dropped into the bin") on "into
   the bin" (Spanish "se abre", "cae en el silo", its own in-point). Paragraph 32 starts 4 s before the truck stops, on "stops with its body under the gate" /
   "se detiene", at 1x. Paragraphs 27-29 (the press), 36-37 and the end card (the stack) and 2, 20 and the title card (the dump) keep
   their in-points in the new films.
7. **Lengths unchanged:** English 492.5 s (8:13), Spanish 544.1 s (9:04); chapters, captions and srt unchanged. The descriptions
   add the press's hold to the list of what is labelled on screen. Thumbnails kept (they use `hi04_berm`, not replaced). The files in
   `videos/ready/youtube` and `youtube-es` were rewritten in place, so the links still match.

### Still weak
- The falling cakes are small on screen: a chamber opens 0.55 m between 1.7 m plates, so the lumps show only from near the head's
  side and for under a second each; most of their fall is inside the trough. The bin's level is not drawn, so "into the bin" shows
  only as the card's figure.
- The dump and the stack are still flat-shaded blocks; the nearer, lower aerials make the steps read, but a real stack's several
  lifts and a dump's long tipped face would read better with more lifts staged.
- The press held each batch is a scene control, not a game feature: a press on a thickener of the plant's rate makes 12.5 t cakes.

### Needs Tim
- **The press's size against the plant (a game rule).** The press is sized for 60 t/h and the plant makes 50 t/h, so without a hold
  every cake is 12.5 t. Either is defensible (a press is often sized above its feed); a "wait for a full batch" setting on the press,
  or matching the two figures, is a design call.
- **Bin level drawn** (a renderer for the load-out bin's contents), so the cake can be seen arriving.
- Still from before: the dozer autopilot, compaction.

### Tests
`build` passes. The full NeoForge suite on this branch: all 306 required tests passed (20 min, at low priority beside another
agent's filming). The client scenes ran three times: fix run 1 kept five films (its drop, low by the head, hid the cakes behind the
head and was filmed before the last cake was timed to the count: rejected); fix run 2's drop started too early (its wait timed out
with the press film skipped); fix run 3: `hi04_drop`. Rejected takes deleted.
