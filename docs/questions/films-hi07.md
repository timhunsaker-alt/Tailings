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
- The ream from below is a dark disc rising up a lit shaft; a section of the raise is not possible (the ream grouts any open cell round it).
- The flask is a block model: its 3 t shows on the card, not in the flask.
- The cage moves with the skips standing, so no drum turns for it.

## Needs Tim
- **A 150 m shaft does not fit under a yard at y 78** (the world's floor at y -64): the episode's shaft is 120 m. Deeper shafts need a
  higher yard.
- **The raise borer reams a metre a minute**, about 120 times a real 5.5 m reamer (Lyle 2017: 0.5 m an hour). Said in the episode as
  the game's scaling; a rule change only if Tim wants it slower.
- **The cage has no drum of its own** (it rides "beside the skips on the hoist's second rope", heavy-iron-fixes #13), so the drawing
  cannot turn a drum for it.

## Tests
`build` passes. The full NeoForge suite on this branch (all the fixes above in, and the new `shaft_cage_button`): all 307 required
tests passed. The client scenes (TAILINGS_ONLY=hi07) ran six times; films kept from runs 3 to 6 (`videos/footage/films-hi07/NOTES.md`).
