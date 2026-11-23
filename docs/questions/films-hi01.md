# Films for Heavy Iron episode 1, "Walking dragline" (branch `films-hi01`, 2026-11-22)

Decided while Tim was away, on the best recommendation; each is open for review. The series plan is
`videos/longform/heavy-iron/PLAN.md`; the episode is `videos/longform/heavy-iron/ep01/`.

## Decisions
1. **Real power for a 6 MW machine.** No scene had fed a pit switch house from real generation (every dragline scene forced its
   supply). The dragline peaks at 6 MW, more than the 1 MW gas sets can carry, so the scene uses Wildcatter's 16.5 MW gas turbine set
   (its starter on a diesel set, as Rock to Metal episode 2), a unit auxiliary transformer down to the pit's medium voltage, MV
   cable to the pit switch house, and the dragline's trailing cable from outlet 1. The narration says the turbine stands in for the
   grid. Its purge and run-up are skipped off camera, as in episode 2. *Needs Tim:* none; a real mine would be on the grid.
2. **The job's bucket now cuts the bank as it drags (needs Tim).** The auto job took all 25 bank cells from the world at the start of
   the drag, so on camera the bank vanished and the bucket slid over air. Now the cells go over the 15 s drag, outermost first
   (`DraglineEntity.cutTo`, `flushCuts`; saved with the entity). The load, the cells, the cycle and the power are unchanged; a cut
   left when the drag ends early (hand takeover, stop, save) goes at once. The 17 dragline and slope NeoForge scenes pass.
3. **The strip is staged.** The strip behind and around the dragline, the last strip's hole and its spoil, and an older backfilled
   strip are laid by the scene; the dragline digs a 20 x 12 x 8 m block on camera. Said on screen ("Strip partly dug before
   filming") and in the narration.
4. **The job ran at ten times speed between films** (`fastCycles(10)`) to dig out its reach so it would walk; the step is filmed in
   real time and the narration says so.
5. **Slope films** use Wildcatter's time scale (10:1 for the highwall, 20:1 for the wreck), which shortens the failure clock only;
   labelled "Failure clock at 10x/20x". The wreck's dragline is on the same switch house (outlet 2), on real power.
6. **A villager for scale** (no AI, silent) at the tub's foot, and the ultra truck parked beside it, as SERIES_BRIEF asks; labelled.
7. **A music bed of its own** for Heavy Iron (`videos/longform/music/bed_heavy_iron.wav`, Ludo createMusic, 3 credits), so the second
   series does not reuse Rock to Metal's bed.
8. **The corner map draws the duty cycle** (drag, hoist and swing, dump, swing back; spoil as waste, the seam as product, the switch
   house as power, the walk), as SERIES_BRIEF asks.
9. **Plan, episode 6:** the code has no jumbo drill, LHD or underground truck (`tunnel-drill-haulage.md` only points to the TBM and
   haulage). PLAN.md proposes "The battery locomotive and bottom-dump cars" unless Tim wants those machines built first. *Needs Tim.*
10. **The MV feeder is one 500 kcmil cable (450 A).** It carries the dragline's cycle (938 A for 15 s, 177 A for 40 s: about 512 A
   RMS, under Wildcatter's burn rise), but held at 4.8 MW for two minutes (a scripted operator stuck at the hoist limit in take 3) it
   burned out after about 70 s, as Wildcatter's wire heat says it should. The scene's operator is fixed. A real 6 MW machine would be
   fed by parallel cables; Wildcatter's MV cable tops out at 450 A and a part has one 4.16 kV terminal cell, so parallel feeders
   cannot be landed. *Needs Tim:* whether the pit switch house (or MV cable) wants a 1,000 A-class feeder.

## Weak on camera, left as is
- The trailing cable is counted, not drawn, so no cable is seen between the switch house and the dragline (P5 design).
- The shoes' walk is short and slow to read from a distance; filmed close from the strip floor.
- The cast film's pour is hidden by the spoil ridge in front of the camera; the cold open uses the cycle film's dump instead.
- The walk is filmed from the strip floor: the house is seen moving 2 m, the shoes are hidden by the highwall.
- The auto cycle's swing between dig and dump is small here (about 20 degrees): the strip and the hole lie side by side. The hand film
  shows a long swing to the truck.

## Tests
`build` passes. The full NeoForge suite (301 scenes, 11.9 min) passes but for `pit_dozer_cut`, the known intermittent one
(ROADMAP: "fails now and then in the full test suite"); run alone it passes. The 17 dragline and slope scenes pass with the cut fix.
