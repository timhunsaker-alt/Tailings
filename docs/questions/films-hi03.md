# Films for Heavy Iron episode 3, "Drill and blast" (branch `films-hi03`, 2026-11-23)

Decided while Tim was away, on the best recommendation; each is open for review. The series plan is
`videos/longform/heavy-iron/PLAN.md`; the episode is `videos/longform/heavy-iron/ep03/` (facts in its `ledger.md`).

## Decisions
1. **Stemmed 6 m, so the zone is 50 m and the blast is filmed from 60-80 m outside it.** The default 4 m of stemming throws collar
   flyrock 124 m by Richards and Moore, held to the game's 100 m: a camera at 60-80 m would stand inside the zone. 6 m (within the
   20-30 hole diameters the p3 doc gives, *unverified*) brings the collar's reach to 43 m and the face's to 35 m, so ARM reads "flyrock
   to 43 m: clear 50 m". The narration says the default throws to the cap and that stemming is the lever. Every column is 10 m, 690 kg
   (0.33 kg/t).
2. **Three patterns, one shot each.** A shot can be filmed once, so the bench carries three identical 3 x 5 patterns 100 m apart: A is
   drilled (first two holes real time), charged (first stand real time), armed and fired with the camera walking out of the zone; B is
   fired for a real-time view from the pit floor; C is fired with the game slowed to a quarter (`tick rate 5`) and labelled. B and C
   were drilled at 40x and charged at 10x off camera; the narration says "a second pattern", "a third pattern".
3. **The camera stands in the zone on purpose.** To show the siren's countdown the camera's player must be in the zone and not a
   spectator: it is put in creative (invulnerable), 35 m behind A. FIRE asks a second press ("PLAYER IN DANGER ZONE"); the scene
   presses again; through the siren the player is told each second, and the camera walks out to 68 m by the shelter. The action bar is
   hidden with the HUD in films, so the card copies what the game shows the player (read from the HUD, word for word). Said on screen.
4. **No one hurt.** The villager for scale at the drill's mast is removed before anything is armed. The villager in A's shelter stands
   under its roof 59 m from the nearest hole, outside both the 43 m flyrock reach and the blast's 50 m harm (log: 20 of 20 health).
   No one is struck in any shot (strikes 0).
5. **Diesel, not the trailing cable.** The drill is placed whole and runs on its own diesel (the 271 class the doc is drawn from; P3's
   way); the truck is diesel. Nothing is force-run; their tanks are filled at the start. The electric drill off a switch house is left
   out: it would need episode 2's power yard for a 600 kW machine and adds nothing to the story.
6. **The rotary head turns (small fix, needs Tim).** The plan flagged that the drill's head did not spin. It now turns at 60 rpm while
   the hole deepens (`BlastholeDrillModel.setupAnim`, a `spin` clock in `PitVehicleRenderer.PitState`; the generator
   `tools/models/pit_blast_vehicles.py` writes the same line). 60 rpm is *proposed* (real rotary heads run about 0-150 rpm, unverified).
   Drawing only.
7. **Staged by the scene, said in the narration:** priming (booster and detonator), stemming with the cuttings, logging (row 65 ms,
   hole 25 ms) and loading the logger into the machine are done by the scene off camera; the readouts show their results.
8. **Speeds.** Every film is real time but `hi03_slow` (a quarter speed through the shot, labelled). The drill's 10 s a metre is the
   game's, about 8x a real drill's 43 m/h; the truck's 20 kg/s is 4x a real 300 kg/min; both said.
9. **Narration.** About 1,240 words in 38 paragraphs, Brian, English. Rewordings for the voice: "the rock it drilled out" (heard
   "rocket") became "what it drilled out"; "Press GO" (GO under 0.4 twice) became "One press of its start button"; "the game's default
   four metres" became "four metres ..., the usual setting in this game"; the end line became "Next episode: the dozers, and the dry
   stack". Accepted after inspection: "four" at 0.37 in P27 with an exact transcript (third take).
10. **Spanish** (Venezuelan, usted, Brian Spanish): "Perforación y voladura", 44 voice calls; new words in the glossary (piedra for
   burden, taco, sobreperforación, explosor, proyección de roca ...); two lines refused by the voice service's moderation and reworded,
   no fact changed (heavy-iron/es_decisions.md, Episode 3).
11. **Music:** episode 1's Heavy Iron bed again (no new credits).

## Fixed (Tim's rule of 2026-11-23: fix weak spots, then re-film)
- **The rotary head did not turn** (PLAN.md): it now turns while it drills (decision 6), and the drill is filmed from the deck side,
  where the head shows between the mast's legs coming down (`hi03_drill`), with a close film of it turning (`hi03_head`, new). In run 1
  the camera stood behind the mast's back plate and the head never showed.
- **The siren film looked at bare bench:** the camera starts 12 m up and ends by the shelter, the muck rising over its roof.
- **The side shot ran along the face, far and flat:** B is filmed from the pit floor north-west of it, 64 m out, the face and the muck
  spilling toward the camera.
- **The pit ran 130 m to natural ground, and the granite bands striped the floors pink:** the pit is walled at 70 m; the bench top
  and the pit floor are plain stone with a little andesite and tuff.
- **The pattern was lost from 36 m up, and the truck small in its film:** both cameras closer.
- **The siren's message shrank the card:** it is split over two lines.
- **Tried and reverted:** drawing the truck's hose from the reel to the collar it charges (a synced collar and a rod in
  `PitVehicleRenderer`). Its ground run did not show and its first leg stood apart from the truck: the model's reel and the game's
  differ. Left as it was; the charge film is run 2's. Needs a proper look at the truck model's reel (small, but not tonight).

## Weak on camera, left as is
- The site is a plain bench and pit floor, with the seed's hills beyond; the fog line shows at the far end.
- The fragments are small at 60-80 m (as asked): specks over the shot, best in the slowed film.
- From behind the pattern (`hi03_siren`) the collars are too small to see at 35-68 m and the shot shows as the muck rising and specks;
  the face is seen in `hi03_blast` and `hi03_slow`.
- The muck is drawn as stepped blocks at 37 degrees; every hole breaks the same (fragmentation not modelled), and the throw forward
  is not modelled.
- The truck's hose hangs from its reel, not run into the hole (see Fixed, tried and reverted).

## Tests
`build` passes. The full NeoForge suite (with `build`, 26 min) failed one test, `pit_dozer_cut` ("it should cut 20 m: at 15.5 ...
at the crest"), the known flaky one (ROADMAP, since 2026-11-18; this branch touches no dozer code). Run again with no change: all 306
required tests passed. The client scenes (TAILINGS_ONLY=hi03) ran three times end to end.

## Second cleanup (2026-11-23, branch `fix2-hi02-03`)
Branch `fix2-hi02-03` (off main 105dee5e). Tim: "Let's keep fixing weak spots as we find them." Episode 3 re-filmed whole and rebuilt
in English and Spanish; P30-P32 re-voiced (they described the camera in the zone), the rest of the narration unchanged. Footage
notes: `videos/footage/films-hi03/NOTES.md`.

### Fixed
1. **The drill has an air hose.** The compressor's main air hose runs from the compressor's top up the front of the mast's leg to a
   clamp 14 m up, then hangs in a loop down and back up into the rotary head. Its 16 m loop hangs lower as the head comes down the
   mast (its bottom from 11 m with the head at the top to the compressor's top, 4.6 m, with it at the bottom). `pit/DrillHose`, drawn
   by `PitVehicleRenderer` (drawing only). Seen in `hi03_drill`, `hi03_head` and `hi03_tram`. NeoForge scene `pit_drill_hose`.
2. **The blast fragments read at range.** Drawn 0.5-1.1 m (were 0.3-0.7 m; real flyrock is mostly 0.1-0.5 m), each with a trail of
   three smaller chips behind it along its arc (`Blasting.FRAGMENT_MIN_M`/`MAX_M`, `FlyrockRenderer`). Drawing only: the count (8 off
   the face, 5 out of each collar), the arcs, the strikes and the reach are unchanged. `pit_blast_flyrock` now checks every fragment
   is drawn 0.5 m or more.
3. **The muck pile reads as a pile, not a staircase.** A muck (or stockpile rock) block with nothing over it is drawn as a 3 x 3 grid
   of columns, lowered toward its open sides: an open edge's middle 5-11 px, a corner between two open sides 1-4 px, each picked at
   random per block (weighted models), so the step edges of a 37 degree pile break up instead of running in straight lines, and the
   heave over each hole is a lump, not a tower. Its collision stays a whole cube, so the settle rule, the buckets, the wheels and
   every count work in cells as before; its occlusion and light follow the drawn shape. `MuckBlock` (block state `up` and the four
   sides, set in its tick and on its neighbours' changes), `tools/models/muck_shape.py` (writes the models). NeoForge scene
   `pit_muck_shape`.
4. **The siren is filmed from outside the zone (Tim's decision: keep the 50 m zone, re-film the siren shot with the camera outside
   it).** The camera stands 67-69 m behind pattern A, west of its shelter, where it sees the villager under the roof and the bench
   beyond; FIRE with nobody in the zone starts the 10 s siren at once (FIRE 6.0 s, FIRING 16.0 s, FIRED 20.6 s). The card reads the
   machine, "Zone to clear: 50 m; no one in it" and the camera's distance. **Re-voiced, English and Spanish** (the old lines described
   the camera in the zone and walking out): P30 "Here the camera stands sixty-eight metres behind the pattern, outside the zone. FIRE
   is pressed. With nobody in the zone, the siren starts at once." P31 "A siren sounds for ten seconds. Had a player been in the zone,
   the machine would have stopped and asked for a second press, and told them every second to clear the zone or take cover." (now
   over the blasting machine in its shelter) P32 "The villager in the shelter stands under its roof, fifty-nine metres from the
   nearest hole." P33 unchanged. Label "Camera outside the zone". Transcribed: every word as written.
5. **The readout card is brief evidence (rev2):** every carded shot cropped clear of it except where a figure on it is said (3 s of
   the full frame first; 4 s for ARM's readout).

### Accepted by Tim 2026-11-23
- **The rotary head turns at 60 rpm** (decision 6): accepted.
- **6 m of stemming** (decision 1; the collar's flyrock to 43 m, the zone the blast's 50 m): accepted.
- **The 50 m blast zone** kept (decision 4 above).

6. **Lengths:** English 523.8 s (8:44, was 525.7), Spanish 576.1 s (9:36, was 579.0); captions, chapters and the descriptions
   updated (the description said the siren was shown telling a player in the zone, and the camera in the zone labelled).
   Thumbnails regenerated (the same frame of the new `hi03_blast`). `videos/ready/youtube` and `youtube-es` links checked.

### Still weak
- The muck pile still shows its layers: its edges are rounded and broken up, but a 37 degree pile of whole cells still reads as
  terraced from close in. Smoothing it further needs partial-height muck (a change to the settle rule), not only drawing.
- The emulsion truck's hose still hangs from its reel, not run into the hole (tried and reverted in the first pass).
- P35 says the fragments are "drawn a little larger than real flyrock"; they are now about twice real size. Not wrong, so not
  re-voiced.
- The cropped shots are 1.5x enlargements of 1080p films; the drill's crops lose the top of its mast.
- P29 and P31 are both on the blasting machine in its shelter (P31 moved there: the siren film is only 16 s before the shot).

### Tests
`build` and the full NeoForge suite: all 310 required tests passed (38.5 min), first run. The client scenes (TAILINGS_ONLY=hi03)
ran end to end twice.
