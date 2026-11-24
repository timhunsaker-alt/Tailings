# Films for Heavy Iron episode 7, "Shaft, hoist and ventilation" (branch `films-hi07`, 2026-11-23)

Decided while Tim was away, on the best recommendation; each is open for review. The series plan is
`videos/longform/heavy-iron/PLAN.md`; the episode is `videos/longform/heavy-iron/ep07/` (facts in its `ledger.md`). The series finale.

## Decisions
1. **One small mine, not three sites.** The headframe over a 120 m shaft, the hoist house behind it, the bottom of the shaft with the
   rail level (track grizzly, pocket, apron feeder, measuring flask), a drive north from it to a face, and a vent raise bored on camera
   down to that drive with the fan at its collar. The episode follows the rock from the train (episode 6) up the shaft, and the air
   down the raise.
2. **Depth 120 m, not the plan's 150 m.** The world's floor is at y -64 and the yard is at y 78: 150 m would leave no room for the rail
   level. At 120 m the travel is 138 m, the skips still reach their 10 m/s, and the cycle is 37.1 s (291 t/h), measured on camera at
   26.8 s for the wind (`windSeconds(138)` = 27.1).
3. **The section:** the rock west of the shaft cut away from the surface to the bottom (x -24 to 1, the collar's 3 m lip kept under the
   headframe's legs), open to the sky, labelled "Rock cut away". The shaft, both skips, the cage and the rail level show from one side
   in daylight.
4. **The raise borer bores a vent raise, filmed; the main shaft is cut by the scene, said.** The borer is the game's way to make a
   shaft; a 120 m main shaft reamed on camera would take 2 h of game time for one shot. The narration says "The main shaft was cut by
   the scene before filming". The vent raise (110 m) is bored by the borer on real power: the pilot filmed at the game's rate, the last
   2 m into the drive's roof at the game's rate, the ream shown at 20 times the game's rate (labelled), the rest of it run on fast off
   camera. The borer is then lifted off by the scene, the collar slab taken out, and the fan, duct and cable laid (said).
5. **The reamer fitted at once:** the game puts the reamer on at breakthrough; the narration says a real crew fits it below.
6. **Ventilation through the raise:** the 75 kW vent fan at the raise's collar forces air down a duct in the raise and along the
   drive's crown to 3 m from the face (148 m of duct): 23.2 m³/s from the fan at 283 Pa, 22.8 m³/s at the face (card), against the
   game's 10.8. The stale-air effects are an opt-in rule, off by default: said so, not filmed.
7. **Real power.** Two Wildcatter 1 MW gas sets, each through a unit substation run backward, joined on the medium-voltage side, to the
   hoist house (450 kW) and, by a branch taken up after the raise, the raise borer (400 kW). A Wildcatter 500 kW diesel set for the
   fan (75 kW) and, by a cable down the raise's corner and along the drive, the apron feeder at the shaft bottom (11 kW). Gas and fuel
   put in by the scene. No voltage written or said.
8. **The headframe bin is drawn down by the scene** between shots (no belt under it here): otherwise it fills in 33 skips, about 20
   minutes, and the hoist stops BIN FULL.
9. **Lit for the camera underground:** light blocks in the drive's crown and up the raise's corners, and the camera sees with night
   vision in the three underground films (breakthrough, ream, face); labelled.
10. **Scale:** a villager (no AI) at the collar beside the call button, the camera rising from it up the 30 m headframe; the same
    villager put in the cage by the scene, riding 120 m down (35 s) and back up. The buttons are pressed by the scene (a scene cannot
    click), as a hand would. Before the cage films the scene empties the pocket and the flask's surge, so the skips finish their ore
    and stand (WAITING FOR ORE): a call made while they wind waits out the wind and its rest, too long for one shot.
11. **Real-world figures, each with a source (Tim's rule).** Kept: a Finnish zinc-copper mine's 1,450 m, 5 m shaft, 54 m headframe,
    21.5 t skips at 15.5 m/s, 275 t/h, a cage for 20 people at 12 m/s (the hoist maker's case sheet for the Pyhäsalmi mine; the maker
    is not named); raise boring, pilot holes of 280-450 mm, cuttings flushed up at the top, a 5.5 m ream at about 0.5 m an hour (Lyle,
    "Considerations for large-diameter raiseboring", Underground Mining Technology 2017, ACG); 0.3 m/s in working places (Queensland
    Coal Mining Safety and Health Regulation 2001, s 344(3)). **Left out:** the plan's 16 t double drum at 11 m/s to 1,260 m on 3 MW
    and the Spanish potash shaft (no source found that could be read), the diesel airflow rules (the drive burns no diesel).
12. **Not compared:** a bottom-dump car's 576 items with a 3 t skip (l4-shaft.md #20; the brief's rule).
13. **Music:** episode 1's Heavy Iron bed again (no new credits).
14. **End card:** "Next series: Built From Scratch" over "The third series"; the last paragraph looks back over the seven episodes.
15. **Length:** 7:38 in English, a little short of the series' 8 minutes, as episode 6 was; the episode covers what the code does
    and stops there rather than pad.
16. **Rewordings for the voice:** "The rock it cuts" (heard "rocket", as in episode 6) became "The cut rock"; "The Finnish shaft"
    (heard "finished") became "The shaft in Finland"; "the shaft, the hoist and the air" (heard "the hoist in the air" twice) became
    "the shaft, its hoist and its ventilation". No fact changed. Accepted after inspection: "head frame" for headframe, "brakes"
    heard "breaks", "quarters" at 0.01 in two takes with an exact transcript (whisper's split of "three quarters"), single function words
    under 0.5 with exact transcripts.

## Fixed (Tim's rule of 2026-11-23: fix weak spots, then film)
1. **Cage call buttons:** a redstone signal reaching the headframe calls the cage to the collar, one reaching the measuring flask
   calls it to the bottom landing (`PlantMachineBlock.neighborChanged`): a stone button on a block beside either is the landing's call
   button. No new block. NeoForge `shaft_cage_button` (pressed at the bottom, the cage comes down; at the collar, back up).
2. **The raise borer's pilot and reamer drawn in the hole:** the drill string of 1.5 m rods with its bit going down as the pilot goes,
   then a 4.8 m reamer head with its cutters under the cutting face, rising as it reams; the spindle under the drive head turning
   (`PlantRenderer.raiseTools`). The client is sent the borer's progress each metre and every 2 s (`PlantBlockEntity` update tag
   `raise`, `raise_fast`; `raiseTools(now)` runs it on between).
3. **The skip's dump reads:** while it pours, its bottom door is drawn swung open and a stream of rock falls from it to the bin's
   mouth, thinning as it empties; the heap in the skip sinks (`ShaftSkipEntity` DATA_POUR, DATA_POUR_TO; `SkipRenderer`).
4. **Sheaves, drums and fan blades did not turn** (checked in the first take: `PlantRenderer` drew none of them). Now the head
   sheaves' spokes and the drums' rope clamps and flange spokes turn as far as each rope moves (from the skips' heights on the
   client), and the vent fan's and booster's impellers turn at 1,480 rpm while they run (`PlantMotion.FAN_RPM`). The static spokes and
   impellers are out of `tools/models/shaft.py` and `vent.py` (regenerated; nothing else in the models changed).
5. **The raise borer's tools were culled from below:** the renderer's box reaches down its hole, and plant parts draw from 224 m.
6. **A call from the collar missed a cage more than 40 m down** (`PlantMachineBlock.callCage` looked 40 m up and down from the
   headframe; the flask's call looked the whole shaft). Both now look down the whole shaft, 200 m. `shaft_cage_button` runs at 60 m.

## Weak on camera, left as is
- The ream from below is a dark disc rising up a lit shaft; a section of the raise is not possible (the ream grouts any open cell round it). *(2026-11-23: the reamer was drawn inside the rock; fixed, "Fixed after release" 3.)*
- The flask is a block model: its 3 t shows on the card, not in the flask. *(2026-11-23: sight windows drawn, "Fixed after release" 2.)*
- The cage moves with the skips standing, so no drum turns for it. *(2026-11-23: its own winder; fix2-hi07 below.)*

## Needs Tim
- **A 150 m shaft does not fit under a yard at y 78** (the world's floor at y -64): the episode's shaft is 120 m. Deeper shafts need a
  higher yard.
- **The raise borer reams a metre a minute**, about 120 times a real 5.5 m reamer (Lyle 2017: 0.5 m an hour). Said in the episode as
  the game's scaling; a rule change only if Tim wants it slower.
- **The cage has no drum of its own** (it rides "beside the skips on the hoist's second rope", heavy-iron-fixes #13), so the drawing
  cannot turn a drum for it. *(Tim, 2026-11-23: add a cage winder; done, fix2-hi07 below. The 120 m shaft and the reamer's rate:
  accepted.)*

## Tests
`build` passes. The full NeoForge suite on this branch (all the fixes above in, and the new `shaft_cage_button`): all 307 required
tests passed. The client scenes (TAILINGS_ONLY=hi07) ran six times; films kept from runs 3 to 6 (`videos/footage/films-hi07/NOTES.md`).


## Fixed after release (2026-11-23)
Branch `fix-weak-spots-hi07` (off main 07a459a0). Tim: "Let's keep fixing weak spots as we find them." Decided on the best
recommendation while Tim was away; each is open for review. Seven films re-filmed in one client run (fix run, every film real time
except the ream at 20x, as before) and episode 7 rebuilt in English and Spanish with the narration unchanged. Footage notes:
`videos/footage/films-hi07/NOTES.md` ("Fix run").

1. **The pilot rod could not be seen going into the ground.** `Hi07Scenes.pilotFilm` now cuts the rock away east of the hole for the
   camera (a trench 3 m wide and 9 m deep, open to the pilot's column; the pad kept under the baseplate) and puts it back after the
   film; the camera comes down from the rig into the cut, where the drill string, its tool joints and the bit are seen going down a
   metre in 10 s. Labelled "Rock cut away". The pilot's string is now lit as the open cell under its bit, not the rock round it, so it
   reads in the cut and as it breaks into the drive (`PlantRenderer`).
2. **The flask's fill showed only on the card.** The measuring flask now has sight windows on its west, east and south faces (the
   north carries its load cell readout), drawn with its rock risen to its fill and a yellow mark at its 3 t (`PlantRenderer.flask`).
   The flask sends its fill and commonest rock to the client when it moves a 24th (`PlantBlockEntity.showFlask`, update tag
   `flask_fill`, `flask_rock`). On camera it empties into the skip at 22 s and fills again over 8 s. NeoForge `shaft_flask_feed` now
   checks the client is told it is full of granite.
3. **The ream from below was a dim disc, and mostly not even that.** The reamer was drawn rising through the layer it cuts, and the
   game takes that layer away only when its metre is done, so for most of each metre the plate was inside the rock and only its body
   showed. It is now drawn tight under the standing layer, its cutters biting 0.18 m in as the metre goes, and pulled up to the next
   layer over half a second when the layer comes away (`PlantBlockEntity.raiseTools`). Seen from the drive, its underside is lit as if
   from below (lit for the camera), with three yellow stabiliser arms that show it turning, gauge cutters round its rim, and chips
   falling from the rim while it cuts (drawn only). **One rule change, small:** each reamed layer's rock now falls from where it was
   cut, down the raise into the drive, rather than being put on the drive's floor (`Shaft.ream`); the same items land in the same
   drive. NeoForge `shaft_raise_bore` now also checks none of it is left in the raise. At breakthrough the roof's rock is seen to fall
   round the bit (particles only).
4. **The first raise borer shot carried the whole readout over the sky for 6 s.** It is now cropped clear of the card for its whole
   length (the 300 mm is the paragraph's figure on screen); every other evidence shot is 3 s of full frame, then the crop
   (`ep07/tools/shots.py` CARD): the ream, the fan, the face, the train, the flask and the wind.
5. **The shots up the raise and the shaft read dim.** Lights in all four corners of the raise every 3 m (were two corners every 4 m);
   the break filmed closer under the spot; the wind ridden 6.5 m from the skip (was 11 m), so it fills more of the grey section.
6. **The 4 m/s figure covered the cage.** The ride is filmed 6 m from the cage with it right of centre, and the cage shots are cropped
   from the right (x 0.3 to 0.97), so the figure, which always sits left of centre, has the left third to itself. The riding camera
   lags the cage as it starts down, so paragraph 26 cuts from the collar (1-8 s) to the cage mid-shaft (18 s on), where the figure
   comes up; paragraph 27 (the call button and the bell) is now the collar's call film from its start, and paragraph 28 the ride
   from 31 s to the bottom.
7. **The Spanish cut showed the English readouts (fan, shaft bottom) in full for 5-7 s.** With the evidence at 3 s they are brief, as
   the episode's description says ("Las lecturas del juego se muestran en inglés"). Game text not translated.
8. **The flask shots re-cut to the window:** paragraph 18 now ends on the flask filling (23.5 s on), paragraph 19 shows it full,
   emptying into the skip at 22 s and starting to fill.
9. **Length unchanged** (the narration is unchanged; no new voice).

### Needs Tim (unchanged)
- The 120 m shaft (150 m would not fit under a yard at y 78), the ream at a metre a minute (about 120 times a real 5.5 m reamer), and
  the cage riding on the hoist's second rope with no drum of its own: game rules, not film fixes.

### Still weak on camera
- The break: the 300 mm bit is small in a 4 m drive even from closer; it reads by the reamer appearing and the roof's rock falling.
- The pilot cut shows only the first 4 m of the 112 m pilot (35 s at the game's rate).
- The shaft section is a big grey wall; the skip and the cage are small in it on a phone even ridden closer.

### Tests
`build` passes; the full NeoForge suite on this branch (the flask sync, the reamer drawing and the falling cuttings in): all 307 required tests passed, first run. `shaft_flask_feed` and `shaft_raise_bore` gained the checks above.

## Second fix round (fix2-hi07, 2026-11-23)
Branch `fix2-hi07` (off main 105dee5e). Tim's rule: "Let's keep fixing weak spots as we find them."

### Decided by Tim 2026-11-23
1. **Add a cage winder.** Done: the cage no longer rides on the skip hoist. A separate single-drum service winder beside the hoist
   house (`cage_winder`, 6 x 6 x 8): its own 2.5 m drum, its own rope over its own 2.5 m sheave in the headframe, its own 160 kW motor
   from the same medium-voltage run as the hoist. Spec with the real counterpart and its sources first:
   `docs/multiblocks/cage-winder.md` (MDG 33's single-drum personnel winder: 4 m/s, a drum of about 2.5 m; the Hard Rock Miner's
   Handbook's 0.405 √H, 4.4 m/s over 120 m, and cage service at two thirds of the skips' speed; its power table's separate cage
   hoist). The cage's 4 m/s is kept, now sourced. The call buttons work as before. The skips and the cage no longer wait for each
   other.
2. **Keep the shaft at 120 m.** Accepted (was "Needs Tim").
3. **Keep the reamer's fast speed** (a metre a minute, said in the episode as the game's scaling). Accepted (was "Needs Tim").

### Fixed
1. **The cage winder** (above): `shaft.CageWinder`, `Shaft.CAGE_WINDER`, `ShaftLayout.findCage`, `Machine.CAGE_WINDER`; the model in
   `tools/models/shaft.py` (the cage winder; the cage sheave and the deck's rope opening in the headframe, the only change to the
   headframe's model); `PlantRenderer.cageDrum` and `cageSheave` turn with the cage, with light rope clamps and yellow marks so the
   turn reads; `SkipRenderer` runs the cage's rope round the top of its sheave to the drum. Recipe, loot, lang, pickaxe tag. NeoForge
   `shaft_cage_winder` (new), `shaft_cage` and `shaft_cage_button` now on the cage winder; `HoistingTest` extended.
2. **The narration said the cage takes the hoist and the skips wait for it.** Paragraph 27's last sentence re-voiced in both
   languages, same voice and settings (Ludo preset Brian; English "calm, measured documentary narration"; Spanish "Venezuelan Spanish
   accent, calm documentary"): "It has its own winder: one drum, one rope over its own sheave, and its own motor." / "Tiene su propio
   malacate: un tambor, un cable sobre su propia polea y su propio motor." Garble check: English exact (one "a" at 0.49), Spanish exact,
   no flags (`ep07/garble4.txt`, `ep07/es/garble3.txt`).
3. **The cage shots now show its winder.** New films `hi07_cagedrum` (the drum turning through the winder's open side as the cage is
   called up, the camera drawing back along the rope to the headframe) and `hi07_cagesheave` (the cage sheave turning, zoomed);
   paragraph 27 cuts from the drum to the sheave. A lower third "Cage winder · its own drum, rope and motor · 2.5 m drum · 160 kW" and a
   3 s evidence card (CAGE WINDER · 160 kW, rope speed, the drum's turns). The cage chapter no longer lights the hoist on the duty-cycle
   map; the map is off there, which also lets the cage's lower third show (it was dropped for room).
4. **The 300 mm bit was small at breakthrough.** The break is filmed zoomed (a 30 degree view from 4.5 m: 2.4 m of roof round the
   bit).
5. **The cut showed only the first 4 m of the 112 m pilot.** A second pilot film, `hi07_pilotdeep`: the rock cut away 92 m down
   beside the pilot's column (9 x 5 m, open to the sky), the camera going down the cut with the bit at 20 times the game's rate to 86 m,
   then the game's rate again; the rock put back. Labelled "Rock cut away · 20x the game's rate". Paragraph 9 opens on it.
6. **The skip and the cage were small on the grey section.** The wind and the ride re-filmed tighter: the camera 5 m from the skip
   (was 6.5) at a 55 degree view, 4.6 m from the cage at 62 degrees, both led by their speed so they stay framed at 10 m/s and 4 m/s.
7. **The site's other films re-taken with the cage winder in them** (wide, power, hoist, scale, dump), so it is there throughout.
   `hi07_sheaves` kept from the first take: paragraph 4 says "two head sheaves", and from that angle the cage sheave stands in front
   of them.
8. **Paragraph 9's pass down the pilot plays its readout in full** (5 s): PILOT 49 to 57 of 112 m counting down is the evidence for
   "a hundred and twelve metres down"; then the zoomed break, the bit through the roof and the reamer on. Paragraph 21 starts 2 s
   later in the new wind (6 s), past the ledge the camera sees first.
9. **Lengths kept:** English 7:38 (458.3 s; the new line is 0.6 s longer), Spanish 8:17 (497.3 s; its new line is 1.2 s longer, and
   paragraph 27's closing pad is 0.7 s shorter in the Spanish spec to hold it). Descriptions in both languages updated (the cage's own
   winder; one pass down the pilot at 20x; the rock cut away beside the pilot), chapter lists refreshed.

### Decided while Tim was away (open for review)
- **The cage winder stands beside the hoist house**, east of it (Tim: "in or beside the hoist house"), rather than as a third drum
  inside it: the hoist house's 10 m is taken by its two 3 m drums. It finds the headframe whose rope side it stands on within the
  rope's 60 m and 20 m either side of the shaft's line.
- **The skips no longer wait for the cage, nor it for them** (two winders, two ropes). The scenes no longer empty the pocket before the
  cage films; the skips are seen winding in them.
- **A cage winder's motor is 160 kW** (142 kW to lift the cage, a rated 1 t and its rope from 120 m at 4 m/s, with accelerating;
  `cage-winder.md`). The cage's mass (1.5 t), its rated load (1 t) and its rope (2.7 kg/m) are proposed figures, marked so.
- **Paragraph 30 kept** ("the medium voltage for the hoist and the raise borer"): the cage winder is a hoist on the same run; not
  re-voiced.
- **A world from before:** the hoist no longer runs the cage; with no cage winder built, calls do nothing.

### Still weak on camera
- The pass down the pilot is a drill string in a slot of grey rock: it reads by its tool joints sliding and the readout counting, not
  by the rock.
- The wind's camera passes the collar's 3 m rock lip for about 3 s (22-25 s of `hi07_wind`); paragraph 22 crosses it briefly.
- The cage at the bottom landing is not seen from the riding camera (kept 5 m up, clear of the ledge); paragraph 28 cuts before it.
- The cage drum's rope clamps are light on a dark drum but small from the north side; its yellow flange marks and the readout's turns
  carry the turning.
- `hi07_call` keeps the first take's angle, with a grey block close on the left as before.

### Tests (fix2-hi07)
`build` passes; the full NeoForge suite on this branch: all 308 required tests passed, first run (the new `shaft_cage_winder`: 60 m
down while the skips wind, the drum 7.64 turns, braked with no power, back up with the drum wound in; `shaft_cage` and
`shaft_cage_button` on the cage winder). `HoistingTest` adds the cage winder's lift power, the drum's turns and an old hoist save
still loading.
