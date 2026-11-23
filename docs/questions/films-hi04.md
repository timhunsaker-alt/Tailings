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
8. **The cake's figure:** the narration says 15 t a cake (the game's batch); the first cycle after start-up holds less (its feed
   started part-way), so the drop is filmed at the end of a later, full cycle.
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

## Needs Tim

## Tests
