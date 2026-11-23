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
