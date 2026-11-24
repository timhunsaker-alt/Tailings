# Films for Heavy Iron episode 2, "Rope shovel and ultra truck" (branch `films-hi02`, 2026-11-22)

Decided while Tim was away, on the best recommendation; each is open for review. The series plan is
`videos/longform/heavy-iron/PLAN.md`; the episode is `videos/longform/heavy-iron/ep02/`.

## Decisions
1. **The rope shovel's dig cycle, drawn as a real one works (needs Tim).** Asked for by the lead: the boom was "fixed at 45°" and the
   handle did not crowd. A real electric rope shovel's boom *is* fixed in service, held by its suspension ropes at about 45°, so the
   boom stays as it is (one constant, `RopeShovelModel.BOOM_ANGLE`; nothing to make data-driven while there is one rope shovel).
   What was missing, and is now drawn:
   - the handle crowds out and retracts through the saddle block and pivots on it, solved each frame so the teeth follow a path: up
     the face from its toe (16.5 m out) to its top (22 m out, 13 m up) through the dig, out over the truck (21 m, 11.5 m) on the
     swing, back down to the toe on the return (`RopeShovelModel.pose`);
   - the dipper's angle follows the handle through the dig: hanging nearly straight down at the toe, tipped up at the top;
   - the door, a plate under the dipper hinged at its inner edge, trips open over the truck (the load drawn in the dipper goes as it
     opens) and swings shut as the dipper comes back down;
   - the hoist ropes follow the dipper's bail with the crowd (`PitVehicleRenderer.ropes`).
   The handle is drawn at 0.6 of its modelled length (the dipper rides 4.8 m in), so the crowd stays within a real handle's travel and
   its back end does not run through the house. The entity syncs where it is in its pass (`PitShovelEntity.cycle()`: 0-1 the dig, 1-2
   the swing, dump and back), so the drawing follows the server's 12 s dig and 23 s swing exactly. The generator
   (`tools/models/pit_vehicles.py`) has the same code and a `door` box; the skin was not repainted (the door uses the dipper's paint).
2. **The rope shovel digs where its dipper is drawn (needs Tim).** Its face started 7.5 m ahead of its centre, at the crawlers' front,
   where no dipper can reach; the drawn dipper reached the floor about 16 m out. The face now starts 16.5 m out
   (`RopeShovelEntity.faceAhead`; proposed, about the class's floor-level cutting radius, unverified). Width, height, depth, mass a
   pass and timing are unchanged. `pit_rope_shovel` lays its muck face there (it was laid where the boom had lain).
3. **The shovels cut the face over the dig (needs Tim).** A pass took all its cells from the world at the start of the dig, so the
   face vanished at once. Now they go over the 12 s dig, lowest first (`PitShovelEntity.cutTo`, `flushCuts`, saved with the entity),
   as the dragline's do since episode 1. The load is counted at the start, as before; a dig cut short (stopped, saved, a new pass) cuts
   what is left at once. The hydraulic shovel shares the class and the change.
4. **Real power, sized so the film is not a burnout.** The shovel draws 3.5 MW for the 12 s hoist and 0.6 MW the rest of each 35 s
   pass (1.59 MW average). On the episode 1 yard (16.5 MW gas turbine set, unit auxiliary transformer, one 500 kcmil MV cable of
   450 A to the pit switch house) the hoist is about 530 A: 1.18 times the cable's rating, a heat rise of 1.39 against Wildcatter's burn
   rise of 1.8 (`WireHeat`), so the cable never burns even if the hoist were held; over a pass it settles near half its rated rise.
   One cable is enough, and the narration and the power card say how it is fed. (Episode 1's dragline burned the same cable at 4.8 MW
   held for two minutes: 739 A.) The turbine stands in for the grid and its run-up is skipped off camera, as in episode 1.
5. **The muck pile is staged.** Blasting is episode 3, so the scene lays a muck pile (cobblestone and andesite, three quarters a block)
   against the pit's north bench. Said on screen. It is waste rock and goes to a waste dump; the narration says ore would go to the
   crusher.
6. **The site.** A pit 12 m deep, a 10 % ramp 120 m long and 40 m wide (two lanes of ultras: a 20 m ramp let the truck's keep-right
   lane run it into the wall in run 1), the dump's crest 60 m past the ramp's top with a 2 m berm (the ultra's mid-axle,
   `DryStack.bermBlocks`). The truck drives itself on a six-stake route; nothing teleports it.
7. **The dispatch office's 5 kW is on its test feed** (`forcePowered`). It is not a motor, and wiring a low-voltage feed for it would
   add a transformer and panel to the yard for no film. Not said on screen; noted here and in the ledger.
8. **Villagers for scale** (no AI, silent): one at the shovel's crawler, one at the ultra's front wheel, one on its deck; a 100 t truck
   parked beside the ultra for the scale shot, removed before the ultra takes its route. Labelled.
10. **A 2 m berm stops an ultra backing in (bug fix, needs Tim).** A truck backing toward a crest finds a berm "just behind its
   tail" and stops stepping up onto it (`HaulTruckEntity.beforeMove`). The check looked for a ridge one block high only, so the ultra's
   own berm (2 blocks, `DryStack.bermBlocks` for its 4 m tyres) was never seen. In run 2 the ultra, backing in at an angle from its
   turn, went past the end of a short berm and over the crest. Now the check takes a ridge up to the truck's berm height; the scene's
   berm runs the whole crest and its turn stake is 40 m back, so the truck backs in square. `pit_berm`, `pit_no_berm`,
   `pit_over_crest`, `pit_ultra_haul`, `pit_dump_advance` and `pit_truck_haul` pass.
9. **One truck.** The shovel waits with a full dipper while the truck is away; the narration says a real pit matches trucks to its
   shovel. A second truck would show double-side loading but doubles the takes' risk; the narration describes it instead.

11. **Narration.** 1,175 words in 36 paragraphs, Brian, English; Spanish in Venezuelan usted register, Brian Spanish (as episode 1).
   Four passes "a little over two minutes" (4 x 35 s), not the first draft's two and a half. The dispatch office's name is said
   "the dispatch office" (the voice said "haul" as "hall" twice).
12. **Music:** episode 1's Heavy Iron bed again (no new credits).
13. **Thumbnails:** `hi02_passes` at 35 s, the dipper's door open over the ultra and a villager at the crawlers, cropped below the card;
   "100 TONNES A PASS" / "100 TONELADAS POR PASADA" (heavy-iron/tools/make_thumbs.py, ep 2 added).

## Weak on camera, left as is
- `hi02_swing` is from low by the crawlers and the house hides the dipper; it is used once, under "it digs only loose ground".
- `hi02_dig`: from the west the dipper is seen rising beside the pile, the cut itself behind its near side. The passes film (from the
  north-east) carries the dig.
- The power card's 3,500 kW comes a few seconds after the narration says it (the film's dig starts 4 s into the paragraph).
- The trailing cable is drawn as a straight line from the switch house on the rim down to the shovel.
- The handle is drawn shorter than its modelled box (decision 1).
- Muck cells vanish as the dipper rises through them; the dipper is drawn but the cut is block by block.

## Tests
`build` passes. The full NeoForge suite (12.7 min) passed but for `quest_shovel_milestone`, whose muck face was laid where the old
face started; moved to the new face (16.5 m out), it and `pit_rope_shovel` pass. The pit scenes (63), the berm and haul scenes and the
shovel scenes pass with every change.

## Fixed after release (2026-11-23)
Branch `fix-weak-spots-hi02` (off main 9a266274). Tim: "Let's keep fixing weak spots as we find them." Decided on the best
recommendation while Tim was asleep; each is open for review. Re-filmed (run 2; run 1 rejected and deleted) and episode 2 rebuilt in
English and Spanish with the same narration (shots swapped only, nothing re-voiced). Footage notes: `videos/footage/films-hi02/NOTES.md`.

1. **The trailing cable lies on the ground (renderer bug fix).** The cable was not drawn as a straight line by design: its points were
   placed relative to `entity.getX/Y/Z(partialTick)`, which in this game version is the position *plus the box's size times the
   argument*, not a lerp. On a rope shovel (a box tens of metres across) that moved the whole cable by up to its box size every frame,
   and the ground search (2 m up, 3 m down from the last point) never found the pit floor from the rim, so it ran through the air.
   Now the cable is placed from `entity.getPosition(partialTick)` and finds the ground under each metre by climbing up to 16 m (a berm,
   a bench) or dropping up to 64 m (off a crest): it runs across the grass from the switch house, hangs down the bench face and lies
   along the pit floor to the reel (`PitVehicleRenderer.cable`, `groundAt`). Renderer only; the model is unchanged. Chosen over a
   catenary between supports: a real shovel's cable lies on the ground, with stands only where it crosses a haul road, and this pit
   has none. The dozer's ground tilt (`PitVehicleRenderer.ground`, heavy-iron-fixes) read its ground through the same call and was
   sampled up to its box width off; fixed the same way. Its tilt was not re-checked on camera (episode 4's films will show it).
2. **`hi02_swing` re-filmed** from beyond the truck, east and a little south, looking back at the shovel: the dipper swings out of
   the face towards the camera, over the truck, the door trips and the load pours, then it swings back. Used under "It digs only
   loose ground" (paragraph 10) at 12 s, now on the swing and the dump. A second angle (high to the south-east) was filmed after the
   truck had gone and was deleted.
3. **`hi02_dig` re-filmed** from the west, 6 m over the rim beside the pile, where the dipper is seen crowding into the face and coming
   up through it, and the cut shows in the face. It now carries paragraph 8 (the ropes, the hoist and the crowd; it was the passes
   film) and paragraph 34 (the face height), and the end card. A second film, `hi02_dig_b`, from high behind the machine to its
   south-west, shows the pit, the pile, the shovel and the ultra; it carries paragraph 4 ("This pit is twelve metres deep... muck
   pile laid before filming", label kept). Run 1's two over-the-shoulder dig views (the gantry filled the frame) were rejected.
   **The cut follows the dipper's teeth (needs Tim).** The cells a pass takes went at a steady count over the 12 s dig while the
   teeth come up eased in and out, so cells vanished above the teeth early in the dig. Now the rope shovel cuts a cell when its
   teeth come up past its middle, by the same heights and easing its drawing uses (0.8 m at the toe to 13 m at the top;
   `RopeShovelEntity.teethUp`, `PitShovelEntity.cutTo`). The load is counted at the start as before; the hydraulic shovel keeps the
   steady count. Small, and seen working in the new dig films.
4. **The dispatch office runs on real power** (replaces decision 7). Its 5 kW comes off the pit's supply: a unit substation on the
   MV net in the yard steps down to the industrial voltage, a 305 m feeder (2 AWG) runs buried 3 m down round the pit and under the
   haul road beyond the ramp's top, and a dry-type transformer at the office's box steps down to the low voltage, its low side against
   the office (`Hi02Scenes.officeFeed`; no `forcePowered`). Chosen over a generator at the office: the yard already has the supply,
   and a feeder from it is what a real pit does. The dry-type is seen beside the office in `hi02_dispatch`; its card now reads
   "Radio: on, 2 km reach; power from the yard". Not narrated (nothing in the narration said otherwise). The dispatch film was
   re-filmed with the same beats; its in-points are unchanged.
5. **The power card matches the narration.** Paragraph 21 now starts in the re-filmed `hi02_power` at 39.0 s, 5.4 s before the dig
   ends: the card reads 3,500 kW / 531 A under "three and a half megawatts" (2.9 s in) and drops to 600 kW / 88 A just before "six
   hundred kilowatts" (6.2 s in). Spanish says the same words at the same times (2.1 s and 5.5 s), so one in-point serves both.
   Paragraph 22 ("The hoist pulls more than that, but only for twelve seconds") moved to 25.0 s, where the next dig and its
   3,500 kW start just as "only for twelve seconds" is said.
6. **The 10 fps idle fix** (`inactivityFpsLimit` MINIMIZED, from `Rtm09Scenes`) copied into every film scene class on main that
   lacked it: `Rtm01Scenes`-`Rtm08Scenes`, `PitShorts`, `PlcBelts`, `PlcTrailer`, `Trailer`, `SlopeScenes`, `Batch4Scenes`,
   `PourScene`. Three lines each, nothing else.
7. **Films replaced:** parts, dig, dig_b (new), swing, power, dispatch, scale_shovel and wide (the cable or the office on camera).
   Kept: scale_truck, spot, passes, haul, dump. Every replaced film is real time (ticks = clock within 0.4 s; run 2).
8. **Lengths unchanged:** English 491.1 s (8:11), Spanish 531.8 s (8:51); chapters, captions, srt and descriptions unchanged.
   Thumbnails regenerated (same frame of `hi02_passes`). The files in `videos/ready/youtube` and `youtube-es` were replaced in place
   or re-linked, so the links still match.

### Still weak
- Paragraph 34 ("Its reach sets the shape of the pit"), on the dig film's second dig, catches the pass where the face is nearly
  worked out: the card reads "Dipper: 1.4 t of waste rock". True, and nothing narrated contradicts it, but a full dipper would read
  better; a longer muck pile would fix it at the next re-film.
- The cable hangs straight down the bench face from the crest; a real one would usually come down a ramp or over a cable stand.
- The handle is drawn shorter than its modelled box (decision 1), as before.

### Tests
`build` passes. The full NeoForge suite (17 min) ran twice on this branch; each time one required test failed and each was flaky,
not this change: `pit_switch_house` (tripped 17 ticks after its overload, against 18-40; 19-21 ticks on three later runs) in the
first, `pit_dozer_cut` ("it should cut 20 m", stopped at the crest on tick 1602) in the second; on the same code it then failed
once and passed three times alone. Neither touches the shovel's cut or the renderer. `pit_rope_shovel` passes (362.5 t in four
passes, 3,500 kW peak). Worth a look (needs Tim): `pit_dozer_cut` is not deterministic.

## Second cleanup (2026-11-23, branch `fix2-hi02-03`)
Branch `fix2-hi02-03` (off main 105dee5e). Tim: "Let's keep fixing weak spots as we find them." Episode 2 re-filmed whole (one run) and
rebuilt in English and Spanish with the same narration (nothing re-voiced). Footage notes: `videos/footage/films-hi02/NOTES.md`.

### Fixed
1. **The trailing cable hangs off the crest instead of dropping straight down the bench face.** Its run is still laid metre by metre
   on the ground, but where the ground drops more than 1.5 m in a metre (a crest) it now hangs from the lip in a curve, steepest at
   the lip and flat where it lands, and touches down on the floor beyond the toe: 0.8 m out for every metre of drop plus 2 (12 m out
   off this 12 m bench; about 2 m a metre at its steepest, where it was 12 m in one). A face it climbs is the same curve the other way;
   a low berm it lies over as before. `pit/CableDrape` (plain Java), used by `PitVehicleRenderer.cable`; drawing only. The dragline's
   cable is drawn by the same code. Chosen over routing the cable down the ramp: a trailing cable is laid where the machine has
   walked, and the game counts that path but does not keep it. NeoForge scene `pit_cable_drape`.
2. **The muck pile reads as a pile** (shared with episode 3, see films-hi03.md): a muck block on a pile's surface is drawn rounded off
   toward its open sides, at heights picked per block, so a pile's step edges break up. The staged pile here and every tipped load
   show it.
3. **The readout card is brief evidence (rev2).** Every shot on a carded film is cropped clear of the card except where a figure on it
   is said: there the full frame plays first (3 s; 7 s for P21's 3,500 then 600 kW), then the crop. P35 (about the card itself) keeps
   it. **The 1.4 t dipper card** (P34, "Its reach sets the shape of the pit") is gone: P34 is cropped throughout, the dipper in the
   face without the card.

### Accepted by Tim 2026-11-23
- **The cut follows the dipper's teeth** (decision "Fixed after release" 3, `RopeShovelEntity.teethUp`): accepted as built.

### Still weak
- The handle is drawn shorter than its modelled box (decision 1), as before.
- The cropped shots are 1.5x enlargements of 1080p films (as episodes 5-7's).
