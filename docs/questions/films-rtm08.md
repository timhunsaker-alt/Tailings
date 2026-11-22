# Films for Rock to Metal episode 8, "Where the Tails Go" (branch `films-rtm08`)

Made 2026-11-22 while Tim was away. Each is my best recommendation; all are reversible. Item 1 needs Tim.

1. **Needs Tim: Wildcatter's decant tower counts every tails slurry as 40 % solids** (`Tuning.CELL_SLURRY_SOLIDS`, its tick). The
   45 m thickener's underflow is marked 60 % solids (`DryStack.SOLIDS_KEY`), so a third of its solids go uncounted in the cell: at the
   pit's 1,000 t/h the cell settles about 667 t/h. The real fix is in Wildcatter (read `tailings:solids` when the slurry carries it,
   and take the water from the rest); not changed here. The episode narrates no settled rate.
2. **The by-product plant takes items from a hopper on its top** (episode 7 found only a hand could; episode 9 needs hopper feed), as
   the tankhouse takes its anodes: one line in `PlantBlockEntity.getSlotsForFace`. A hopper under it takes the outputs as before. NeoForge
   scene `byproduct_hopper` (fails without the change). Committed on its own.
3. **Staged, and said so in the episode:** the tails into each thickener (a flotation bank or a tank cell with its outlet filled, as if
   from the bank or the row); the water tanks the thickeners and the decant tower fill, drawn down each second as the plant would use
   the water; the load-out bin stocked to 100 t (a cycle drops about 12.5 t at the plant's 50 t/h, and the 91 t truck needs more); the
   dozer's blade loaded with dry tails; the mixer's cement chest; the fill line coiled on the ground for its length (640 m, 5.0 MPa);
   the lined cell part-filled (1,150 t).
4. **The pit is fed exactly 1,000 t/h** while filming (a tick's worth each tick); the 45 m thickener can take 1.2 times that.
5. **The paste fill is real time.** The game places a block of fill for every tenth of a cubic metre (`PasteFill.FILL_SCALE`), so the
   drive fills about six blocks a minute; the episode says so.
6. **Line pressure is said, not shown:** the pump reads 5.0 MPa on 640 m (friction 8 kPa/m less 19.6 kPa/m for the 6 m fall); LINE
   PRESSURE past about 1 km on the flat is the tested rule (`paste_line_pressure`), narrated as the rule.
7. **The legacy pond is narrated, not filmed** (older worlds only).
8. **Two films drew at half speed and play at 2x (real time):** `rtm08_drop` both times it was filmed (56 s by ticks, 112 s by the
   clock); other films in the same runs were real time, and a 6 GB client heap (env `CLIENT_XMX`, added to
   `fabric/build.gradle`, off unless set) did not change it. Cause not found.
9. **Narration length:** 1,188 words, under the brief's 1,400-1,700, as episodes 6 and 7 were.
