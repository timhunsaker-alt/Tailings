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

## Decided by Tim 2026-11-23 (branch `fix2-hi04`, off main 105dee5e)
Tim decided four game changes for episode 4; the spec note with each one's real-world counterpart is in
`docs/multiblocks/open-pit-p4-dry-stack.md`, "Tim's decisions of 2026-11-23". Tim's rule stands: "Let's keep fixing weak spots as we
find them."
1. **Dozer autopilot.** Two `DOZE AREA` survey stakes (a new stake kind, yellow flag) mark the area's corners; the survey book with
   those two stakes clicked on the dozer starts it, and again stops it; a player in the cab takes it over. It pushes the way it faced,
   squared, in lanes a blade wide: SPREAD to the far end at 3.5 km/h, anything left on the blade left at the end, blade raised, reverse
   straight back down its own lane, then over to the next lane (across and back), squaring up on its tracks. It waits for anything in its way (not spectators), stops out of fuel,
   stuck or at a drop behind it, and burns its own diesel (`TrackDozerEntity`, autopilot section).
2. **Compaction.** `dry_tails` is now `DryTailsBlock` with `compacted=false|true`: placed loose (paler, in clods), compacted by a pass
   of a moving dozer's tracks (darker, smooth, track marks). The slump rule: loose cake on loose cake at the top of an edge with a
   drop of 2 m or more beside it slides off and settles below at 37 degrees; anything else stands. Berms on rock, piles on ground and
   1 m lifts never move.
3. **Full batch press.** The fill holds at its 2-minute mark ("filling: waiting for a full batch") until the chambers hold 15 t; then
   the timed squeeze, blow and open; the whole 15 t drops at the end. The scene's own hold (`Hi04Scenes.holdPress`, its selector to
   OFF) is gone: the game does it. The client stops running the press's plates on while it is held (`press_held`).
4. **Bin level.** The load-out bin's static "full" fill came out of its model (`tools/models/pit.py`, only `loadout_bin` changed); it
   draws its dry tails at their level inside and a sight window on each wall with a mark every 100 t (`PlantRenderer.bin`); the
   server sends the level whenever it moves a 100th (`bin_fill`).
- **Narration rewritten where it was now wrong** (EN and ES, same voices and settings: Ludo `createSpeechPreset`, Brian, English
  "calm, measured documentary narration" / Spanish "Venezuelan Spanish accent, calm documentary"): P07 (no longer "It has no
  autopilot"), P28 (a cycle of at least 15 minutes, filling to a full batch), P29 (every cake 15 t; on the plant's 50 t/h about 18
  minutes a cycle), P31 (the whole cake at once, output in drops; the bin's level in its windows), P35 (loose and compacted, the slump;
  "compaction is not modelled" was wrong), and a new paragraph after it (the autopilot; Spanish paragraph 36, the old 36 and 37 moved
  to 37 and 38 with their takes renamed, not re-voiced).
- **Spanish:** the same lines translated and re-voiced (Brian, Spanish, "Venezuelan Spanish accent, calm documentary"); "claro y en
  terrones ... lo compacta" (heard "enterrones", "compactra") became "claro, en grumos ... lo deja compactado".
- **Garble check** (`garble_fix2.txt`, `es/garble_fix2.txt`): every re-voiced clip transcribed exactly or nearly; accepted after
  inspection: EN p07 "stack" 0.46, p35 "in" 0.47, p29_c2 "1" 0.46, p38_c2 "a" 0.42 and "and" 0.31 (exact transcripts); EN p38_c1
  "stakes" heard "steaks" (a homophone; re-voiced once, the same again); ES p29_c2 "las/la", p36_c2 "del/de" (match over 0.97).
  EN p38_c1 first take heard "a survey book" for "the survey book": re-voiced. The lane line was re-voiced in both languages once the
  pattern changed (below): "backs up its lane and moves over to the next" / "retrocede por su carril y pasa al siguiente".

### Fixed (fix 2)
1. **The autopilot's first takes stopped or stuck.** Backing diagonally to the next lane it climbed a staged pile and its drop check
   (2 m) stopped it; then, turning at a lane's start, a windrow in its swing stopped it for good. Now it backs straight down the lane
   it has just pushed clear, crosses over at the near end, pushes loose ground in its swing aside into the blade, counts a crest
   behind it only at the game's 3 m, and gives up turning after 10 s ("no room to turn"). `pit_dozer_auto` has a windrow where it
   turns.
2. **The press's drop is filmed from above** (run 4: high over the tail's end, looking south down the plates to the head) instead of
   from the side, where the trough's wall hid the falling cake and the lumps read as specks. The lumps are larger once clear of the
   plates and the cake now gathers in the trough under each opened chamber until the count (47.2 s into the take). A run-6 take from
   straight above the head (the scene's camera now) showed only a hairline gap between the plates and no falling cake: rejected.
3. **"Into the bin" is seen:** the bin's windows (`hi04_bin`, its level falling as the gate fills a truck), and its fill from above in
   the press film; the bin's model no longer looks full whatever it holds.
4. **The press film's card** no longer says "up to 15 t each" or "held by the scene".
5. **Slow films played at their game's speed:** two run-2 films (the machine loaded by other agents' builds) are played at clock /
   ticks (slump 1.43x, bin 1.25x) so the game's motion is real time; noted in NOTES.md.

### Still weak (fix 2)
- **The falling cake still does not read**, from the side, from above the tail (run 4, used) or from straight above the head (run 6):
  a chamber opens about 0.5 m between 1.7 m plates, so the gap is a hairline on screen and the cake falls inside the trough. It needs
  an art change, not another camera: wider gaps between the shifted plates, or an open side to the trough so the fall can be seen.

## Fix 3 (branch `fix3-hi04`, off main 43ba8293, 2026-11-24): the cake seen falling
The art change, all three of the options at once (spec note with the real counterpart: `docs/multiblocks/open-pit-p4-dry-stack.md`,
"The press's opening drawn"). The press's figures, timing, batch and the game's count are unchanged.
1. **An open frame under the pack.** The trough's 3.2 m solid sides are cut away: skirted pans on posts hung from the floor, stepping
   down to the chute over the bin (pan tops 2.0, 1.6, 1.2 m, the chute 0.6), so the space under the plates is open from every side,
   as under a real side-bar or overhead-beam press. `tools/models/pit.py`; only `tailings_filter`'s model files changed on rebuild.
2. **A 0.8 m gap** (was 0.5): the tail draws back 0.8 m and each shifted plate moves 0.8 m; the tail is drawn 0.6 m thick so it still
   stops short of the cylinder's stand.
3. **Slabs, not specks:** each cake comes away in four slabs (about 1 x 0.8 m, drawn 0.12 m thick), the lower two first, tumbling as
   they fall the full height onto the pan; they gather there till the count. The schedule is `pit/PressOpening` (plain Java).
- **Tests:** the full NeoForge suite on this branch (2026-11-25): all 317 required tests passed. `PressOpeningTest` (new, 4 tests,
  pass); `ModelLintTest` passes; `pit_filter_batch` checks the drawn last cake has
  landed by the game's count.
- **Scene:** `hi04_drop`'s camera is now three-quarter from the east, level with the press's floor, between its columns, at the head
  end (`Hi04Scenes.DROP_CAM`). The head is at the **north** end (its feed pipe), the tail and ram at the south: the old comment had
  them the other way round. `HI04_DROP_PREVIEW=1` (with `HI04_DROP_CAM` to try another camera) films only the drop in a few minutes,
  the press fed and set near its cycle's end by the scene: a camera check, never a kept take.
- **Camera checks:** preview 1 (south-east) showed the open frame and the cake heaps on the pans clearly, but from the tail end;
  preview 2 (north-east) had a column and the thickened-tails pipe across the head-end chambers; preview 3 recorded the desktop, not
  the game (the recorder films the screen rectangle of the game window, and other windows were in front of it): deleted unwatched
  beyond the check.
- **2026-11-25:** the full suite of 2026-11-24 died unfinished (its log stops at 12:31, no result); nothing of this branch was left
  running. A filming run started at 10:32 ran as a preview by mistake (the client script passes `HI04_DROP_PREVIEW` empty, and the
  scene took any set value as on: now a blank value is off), and it too recorded the desktop instead of the game window. Deleted
  unwatched beyond the check. Filming needs the game window in front and the screen left alone for the run (about an hour).
- **The recorder reads the game's own frames (2026-11-25, asked by the coordinator: Tim uses the computer while scenes film).**
  `Filming` (fabric gametest) no longer films the screen. At the start of each client tick (a client game test draws one frame a
  tick) it copies the main render target (`gameRenderer.mainRenderTarget()`, the frame just drawn with its HUD and card) on the GPU
  into one of four reused read-back buffers (`copyTextureToBuffer`, MAP_READ | COPY_DST, as `Screenshot` does); when the copy is done
  the bytes go to a writer thread that pipes them as raw RGBA to ffmpeg (`-f rawvideo -pix_fmt rgba -s 1440x810 -r 20 -i -`, then
  `vflip`, a 2x nearest-neighbour scale and `fps=60`, x264 CRF 14), the same `build/scene-videos/<scene>.mov` at 2880x1620 and 60 fps
  as before (the game draws at the window's 1440x810; the screen recordings were that doubled on the Retina screen). Same API, same
  paths: no scene and no `keep.sh` changed. A film is game time: 20 frames a game second. It logs frames written, dropped and the
  real rate: the test films wrote 1,421 frames for 71.1 s of game time, none dropped, 19.8-20.0 frames a second by the clock.
  `TAILINGS_FILM_SCREEN=1` films the screen with `screencapture` as before. Checked: the test takes show the game while other windows
  covered it. (The client script now runs Gradle online: offline resolution had lost `lwjgl-freetype`.)
- **The slabs turn as they fall:** seen from the press's side the first test take showed the slabs edge-on (their faces point along
  the pack); each now also turns 60-90 degrees about the vertical once clear of the plates, as real cake pieces twist off the cloth,
  so its face shows from the side. The camera moved in to about 5 m (`DROP_CAM`).
- **Not done in this pass:** the real take of `hi04_drop` (and `press`, `thickener`, `bin`, which show the old solid trough), the
  in-points, the EN/ES rebuild and the contact sheets: filming needs the game window in front for about an hour.
