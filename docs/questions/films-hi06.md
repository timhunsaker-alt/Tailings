# Films for Heavy Iron episode 6, "The battery locomotive" (branch `films-hi06`, 2026-11-23)

Decided while Tim was away, on the best recommendation; each is open for review. The series plan is
`videos/longform/heavy-iron/PLAN.md`; the episode is `videos/longform/heavy-iron/ep06/` (facts in its `ledger.md`).

## Decisions
1. **The episode is the battery locomotive and its bottom-dump cars only** (title "The Battery Locomotive", family label "Underground ·
   rail haulage"). The game has no jumbo drill, LHD or underground truck, and nothing implies one. End card "Next: Shaft, hoist and
   ventilation".
2. **One site, a section of a mine:** a yard with the charging bay south of a stone ridge; one track through the portal, over a
   bottom-dump station (track grizzly, cam rail, pocket under the track), down a 10 m 1-in-2 rack decline to a lower level. The ridge's
   east side is cut away down to the track bed so the drive shows; labelled "Rock cut away".
3. **Real power:** a Wildcatter 500 kW diesel set, its industrial cable on the ground to the world's three-phase 30 A receptacle
   against the charger (NEMA L22-30 on this world's standard; the receptacle is never named on screen, and no voltage is written or
   said). The charger is a real receptacle load: 20.0 kW from the line, 18.0 kW into the battery (card). Fuel put in by the scene.
4. **Every film real time.** Between films the scene places, loads, holds and reverses the train (said in the narration and labels:
   "Cars loaded by the scene", "Battery swapped by the scene", "Battery set almost flat by the scene").
5. **The battery swap is done by the scene** with the same calls a player's clicks make (`takeBattery`, the charger's held box,
   `setBatteryWh`), since a scene cannot click; the box is seen to leave and return (fix 1 below).
6. **Scale:** a villager (no AI) held on the locomotive's new cab step by the scene; it reads as standing on the step beside the cab.
7. **A three-car train** (20 t loaded) rather than ten: it climbs at 0.5 m/s on camera; the narration gives the ten-car 0.20 m/s from
   the game test.
8. **Real-world figures, each with a source (Tim's rule); see ledger.** Kept: the 8 t class's motors (2 x 11 or 2 x 15 kW) and 440 Ah,
   about 58 kWh (two makers' spec tables); real top speed 6.2-7.8 km/h; 80 % depth of discharge (US Bureau of Mines OFR 61(5)-78; ZVEI);
   spare boxes charging while one works (Wikipedia "Mine railway"; an anthracite haulage history); no trolley wire needed (USBM;
   Wikipedia); a Swedish iron mine's trains of 21 bottom-dump cars of 17 m³ emptied on the move (Mass Mining 2024, ACG); steel-wheel
   adhesion falls underground (same paper) and mine rack lines about 1 in 6 (Wikipedia "Rack railway", Morgan rack, 16 %). **Left out:**
   the PLAN's lithium monorail (the paper says 142 kWh, not 143.5; not needed), the 300-400 mm grizzly gaps (no source found; the game's
   300 mm is said as the game's), boxes changed by crane (not found), any real ruling grade figure.
9. **The game is stronger than the real class, said plainly:** "So the game gives it more power than a real one has" (2 x 22 kW against
   2 x 11-15), and "three metres a second is about eleven kilometres an hour; real eight-tonne battery locomotives list six to eight".
10. **The readout card as brief evidence from the start:** every shot cropped clear of the card except where a figure it shows is said
    (the charger's 18 kW, the set's 20 kW of 500, the battery, the climb speed and energy, the pocket's count): there the full frame
    plays 4-6 s first. The FLAT paragraph keeps the card, since it is about the card's state.
11. **Length:** 7:45 in English (8:35 in Spanish), a little short of the series' 8; the episode covers what the code does and stops there rather than pad.
12. **Rewordings for the voice:** "mines" heard "minds" four times, so "Real mines use them" became "Underground railways use them", "so
    a mine can see" became "so the control room can see", "A working mine does not wait" became "Real haulage does not wait", "Real
    mines do this" became "This is done"; "the bay's wall" (heard "base") became "the wall of the bay"; "CHARGING and FULL" (heard
    "charging in full") became "when it is charging and when it is full"; "lift it" (heard "lifted") became "move it up"; "no car dumps"
    (heard "card") became "nothing dumps"; "The rock it dumped" (heard "rocket") became "The rock that it dumped is still underground";
    "A charged box" (heard "charge box") became "A box that is already charged". No fact changed. Accepted after inspection: "wires"
    for "wire", "part way" for "partway", and single words at 0.44-0.50 with exact transcripts; "Three" at 0.21 and 0.26 in two takes of
    the speed line (sentence-initial, exact transcript).
13. **Music:** episode 1's Heavy Iron bed again (no new credits).
14. **Spanish:** "La locomotora a batería", Brian Spanish (Venezuelan, usted); new words under Heavy Iron in `es_glossary.md` (sala de
    carga, cajón, parrilla de vía, riel de leva, rampa de cierre, vagón de descarga por el fondo, cremallera, estribo, ...). Three clips
    re-voiced with the same words (p14, p30, p31); no rewording.

## Fixed (Tim's rule of 2026-11-23: fix weak spots, then film)
1. **A swapped-out battery box stayed drawn on the locomotive.** Whether a box is fitted is now synced to the client
   (`MineVehicleEntity` DATA_BATTERY), and the renderer draws a battery-out model (box, lid and lugs hidden) when it is out.
2. **The bottom-dump car's doors snapped open and shut.** They now swing down over 6 ticks and back (client side, drawn in 7 steps;
   `MineVehicleRenderer`, `BottomDumpCarModel.doors(float)`).
3. **Nothing was seen to fall when a car dumped.** A shower of the load's commonest rock now falls from under the car when it drops its
   load over the grizzly, and more while its doors hang open (`MineVehicleEntity.dropThrough`/`shower`, block particles; drawing only:
   the load goes into the pocket at once, as before).
4. **The rail over a track grizzly was a full ballast block,** hiding the bars and the pocket, so a dump looked like nothing at all.
   Mine rail has a new `bridge` state, set from the block under it (`MineRailBlock.BRIDGE`, `updateShape`, `getStateForPlacement`):
   on a grizzly it is drawn as rails, conductor and ties only (`mine_rail_flat_bridge`, `_rack_bridge`), with a matching open
   collision shape. (The blockstate JSON was edited directly: `tools/phase3_assets.py` is already behind it, e.g. the rack models.)
5. **No step for anyone to stand on:** the battery locomotive's model has a driver's step each side of the cab (`step_l`, `step_r`).
6. **A voltage in game text:** the traction battery's tooltip read "132 V 440 Ah"; it now reads its energy, "58 kWh".
7. **The charger's readout said "20 kW" on every receptacle:** it now gives its real rating, 80 % of the receptacle it is plugged into
   (17.7 kW on IEC 32 A).
8. **A public `MineVehicleEntity.reverse()`** (what a sneaking click does), used by the scene and by the click itself.
9. **The docs' unverified real-world figures** are checked: `battery-locomotive.md` and `bottom-dump-cars.md` gain a dated, sourced
   list (the motors corrected, the lithium figure corrected to 142 kWh, the grizzly gaps marked not found).

The trolley locomotive's sound borrowed by the battery locomotive is moot here: the episodes use no game audio.

## Weak on camera, left as is
- The mine rail is a full ballast block, so the track stands a metre proud of the yard's grass, like an embankment. *(2026-11-23: the scene's fault, fixed, "Fixed after release" 1.)*
- The dump itself is one tick: the load leaves the car at once as the doors open; the falling rock is drawn, not simulated. *(2026-11-23: the load now falls over about a second, "Fixed after release" 2.)*
- The pocket does not show its rock rising; the card gives its count. *(2026-11-23: the fill is drawn, "Fixed after release" 3.)*
- The aerial's far edge shows unrendered sections after about 45 s (not used).

## Needs Tim
- **(Accepted by Tim 2026-11-23: the locomotive stays as it is, power and speed.)** **The game's battery locomotive is about half as strong again as the real 8 t class** (2 x 22 kW against 2 x 11 or 2 x 15 kW in two
  makers' tables), and faster (3 m/s, 10.8 km/h, against 6.2-7.8 km/h). The battery (440 Ah, 58 kWh) is right. A rule change (it would
  move the climb test's 0.20 m/s to about 0.14), not a film fix; the episode says the game is stronger.
- **(Accepted 2026-11-23, checked in fix2-hi05-06: not wrong.)** **`Battery.java` still defines the battery as 132 V x 440 Ah** to get its 58,080 Wh. It is a battery rating, not a grid voltage, and
  is never shown now; say if it should be held as watt-hours instead.
- **(Accepted by Tim 2026-11-23: the 1-in-2 grade stays.)** **A 1-in-2 decline for rail** is far steeper than any real mine railway (the TBM's grade); the episode says so.

## Tests
`build` passes. The full NeoForge suite on this branch (the bridge rail, the swinging doors, the rock shower and the battery-out
model in): all 306 required tests passed. The client scenes (TAILINGS_ONLY=hi06) ran three times; films kept
from runs 2 and 3 (`videos/footage/films-hi06/NOTES.md`).


## Fixed after release (2026-11-23)
Branch `fix-weak-spots-hi06` (off main 6a054d2e). Tim: "Let's keep fixing weak spots as we find them." Decided on the best
recommendation while Tim was away; each is open for review. All ten films re-filmed in one client run (fix run 1, every film real
time) and episode 6 rebuilt in English and Spanish with the narration unchanged. Footage notes: `videos/footage/films-hi06/NOTES.md`.

1. **The track stood a metre above the ground because of the scene, not the rail model.** The mine rail is meant to be the floor's
   own cell: the game counts it as floor (`WorldGround`, `Ground.FLOOR`) and the TBM lays it in the floor with gravel under it
   (`tunnel-boring-machine.md`, "Gravel ballast"). `Hi06Scenes` had laid it one cell up, on a gravel bed on top of the yard and the
   drive floor. Now the rail is in the floor (`Hi06Scenes.rail(z)`, gravel under it), so the rail head and the wheels are level with
   the grass and the drive's floor; the drive floor beside the decline is graded with slope blocks to match the rail; the dump station
   (track grizzly, pocket, cam rail) is one metre lower with it, the cam rail in the open floor beside the bridge rail, its top under
   the rail head; the drive's clear height over the rail stays 4 m; the static cameras are a metre lower. No change to the rail
   block or its model. A player who lays mine rail on top of grass still gets a raised bed, as before: the block is a floor block.
2. **The dump runs over about a second, the rule unchanged.** As before, the pocket takes a car's whole load or none, checked as the
   doors open; it now keeps room for that load (`DumpBlockEntity.reserve`) while it falls out over `Haul.DUMP_TICKS` (20 ticks,
   after 2 for the doors to start): most at first, the last trickling, the car's count dropping and the pocket's rising as it lands
   (`MineVehicleEntity.dumpThrough`, `DumpBlockEntity.receive`, `CarLoad.take`). The falling rock is drawn in step with what leaves
   the car. On camera each car empties in 1.1 s (576 to 0) at 2.6-3.0 m/s, about 3 m, inside the 4 m grizzly, and the cars overlap.
   Chosen at 1.1 s rather than 2 s so the whole load still drops while the car is over the grizzly at top speed, as narrated
   (paragraph 26). A car mid-dump is not held by the "pocket full" check and its load does not count again at the dump's signal;
   a car that stops feeding the pocket for a second gives up its room. The fall is not saved: a world saved mid-dump keeps the
   rest in the car. NeoForge `bottom_dump_on_move` now also checks the cars are seen part empty with their doors open.
3. **The pocket's fill is drawn.** The pocket sends its share full and commonest rock to the client when it moves a 64th
   (`DumpBlockEntity.showBars`), and `DumpRenderer` draws that rock over the pocket's stepped steel floor up to the level that
   volume reaches, with a slightly rough top (full is just under the bars). It shows through the bars in `hi06_dump` (empty steel
   steps at the start; copper ore over the lower steps at 1,728 items). This applies to every rail dump pocket, side dump included.
4. **The length stays** (Tim: do not pad it).
5. **Every film re-filmed and kept at full 2880 x 1620** (CRF 17, as episode 5's fix), since the raised track showed in all of them.
   The events landed within 0.3 s of the first takes (doors 12.3, 13.3, 14.1 s; FLAT at 19.8 s; the climb 0.5 m/s at 51.8 kW), so the
   in-points are unchanged.
6. **Tests:** `build` passes; the full NeoForge suite on this branch: all 306 required tests passed.


## Second pass (2026-11-23)
Branch `fix2-hi05-06` (off main 105dee5e). Tim: "Let's keep fixing weak spots as we find them." Decided on the best recommendation
while Tim was away; each is open for review. Narration unchanged in English and Spanish. Footage notes: `videos/footage/films-hi06/NOTES.md`.

### Fixed
1. **The dump shots cropped off the car tops to keep the readout card out of frame.** `hi06_dump` is re-filmed from back and up
   (`Hi06Scenes.dumpFilm`: from 5, 1.2 to 9.5, 3.2 off the grizzly's east side), so the train runs through the middle of the frame
   with the car tops below the card, and the pocket's front and its fill under the bars below. Paragraphs 24, 25 and 28 crop to the
   dump station with the cars whole; paragraph 27 (the pocket's 5,760, the card's figure) plays the full frame for 6 s first, then
   the same crop (rev2 evidence + then_crop). The events landed as in the earlier take (doors 12.3, 13.2, 14.1 s), so the in-points
   hold. Episode 6 rebuilt in English and Spanish.

### Accepted by Tim 2026-11-23
- **The battery locomotive stays as it is:** its 2 x 22 kW, its 3 m/s and the 1-in-2 grade (the episode already says the game is
  stronger and steeper than a real mine railway).
- **Battery V x Ah:** checked, not wrong. `Battery.java` holds 132 V x 440 Ah = 58,080 Wh; the makers' 8 t tables give 440 Ah at 110
  to 144 V, so 132 V sits inside that range and the 58 kWh (46 kWh usable at 80 %) in the episode and the card matches it. It is a
  battery rating, never shown on screen.

### Still weak
- The dump film is farther than before, so the pocket and the falling rock are smaller in the frame; the grizzly film stays the close
  view of the doors.
- In the grizzly film the far end of the track runs up toward the card, so the train's first seconds are cropped at the top; the dump
  itself (12-16 s) is whole.
