# Films for Rock to Metal episode 7, "The Last Quarter" (branch `films-rtm07`)

Made 2026-11-22 while Tim was away. Each is my best recommendation; all are reversible. Items 1 and 2 need Tim.

1. **Needs Tim: the cells' start trips a lone 1 MW gas set.** The electrowinning bank (400 kW, a motor load at the industrial voltage,
   started direct on line) tripped its own 1 MW gas set on frequency when it started (run 3: "TRIPPED on frequency (56.0 Hz)"). The
   playthrough ran the cells on a set of their own; here they need two gas sets in parallel. Real cells sit behind a rectifier that
   ramps its current, not a motor start; main's tankhouse now runs on Wildcatter's rectifier, and the electrowinning bank could do the
   same. Not changed: filmed on two sets, and the narration says the cells need two.
2. **Needs Tim: the by-product plant cannot be fed by hopper.** It counts as a paying part, so its top face offers no slots
   (`PlantBlockEntity.getSlotsForFace`): only a hand puts items in. Filmed with lead ingots put in by hand (said in the narration). On a
   500 kW diesel set east of the plant it stalled on its start (NO POWER); on a 1 MW gas set by busway it runs (243 kW).
3. **The copper ladder follows the world's rule.** The filmed world is new, so `copper_shortcut` is on: both filters pay 85 %. The
   episode narrates 8.9 + 1.2 + 2.1 = 12.2 ingots of a copper ore block's 14 (the line card's shares, 73 / 10 / 17 %, agree), not the
   roadmap's 10.5 / 11.9 / 14 (the rule-off figures). The cells are not filters, so the rule does not cut them.
4. **The generator-set trip was re-filmed and read off the readout** (roadmap). A spare 1 MW gas set with a flotation bank and five air
   blowers, a demonstration labelled as such: with one blower running the bank's start dips it to 57.8 Hz and it holds; with two it falls
   to 56.2 Hz (panel: "TRIPPED on frequency (55.7 Hz)"). The playthrough's 56.1 Hz is not narrated. That film was drawn at half speed
   (another agent's NeoForge suite was running) and plays at 2x, which is real time.
5. **The scavengers get their own thickener and filter**, so the card counts their copper apart; a real plant joins it to the main
   concentrate. Said in the narration.
6. **Froth on the scavenger and cleaner banks** (`PlantRenderer`), as episode 6 gave the flotation bank: nothing after the bank moved on
   camera. NeoForge scenes `scavenger_bank_froth_moves`, `cleaner_bank_froth_moves`.
7. **Left out, as briefed:** the by-product plant's nickel, molybdenum, rhenium and cobalt (episode 9). Cadmium and indium are named,
   not filmed. No cathodes: the cells pay nuggets and ingots.
8. **The heap's liquor joins the same cells** (a pipe to the cells' inlet). Its copper is a trace: andesite pays about a fifth of a
   nugget a block, at 5 t/h.
9. **Narration length:** 1,231 words, under the brief's 1,400-1,700, as episode 6 was; the episode runs 8:08 with the cards.
10. **Weak:** in the no-acid film the broken length of pipe is mostly hidden between the leach tank and its acid tank; the card carries
   the NO ACID reading.

Tests: `build` passes; the full NeoForge suite passes (300 of 300, the two new froth scenes included), on the branch with main 39a4b6f5 merged.
