# Films for Rock to Metal episode 6, "Lead from Zinc" (branch `films-rtm06`)

Made 2026-11-22 while Tim was away. Each is my best recommendation; all are reversible. Item 1 needs Tim.

1. **Bug fixed (needs Tim to confirm): the fifth reagent slot was out of a hopper's reach.** `PlantBlockEntity.getSlotsForFace(UP)`
   offered slots 0 and 2-5, but the tanks keep reagents in 2-6 (`LAST_REAGENT_SLOT`, the slot added so copper sulfate cannot block
   zinc sulfate, selective-flotation.md "As built"). A conditioning tank already holding lime, xanthate, frother and copper sulfate
   (a bulk tank on zinc feed) could never take zinc sulfate by hopper when set to selective: it read NO REAGENT for good. A click by
   hand worked, since `offer` walks to slot 6. Fix: the top face offers slot 6 too. New NeoForge scene `reagent_hopper_fifth`.
2. **Small flotation bank froth.** The bank's eight cells were a still model, so the episode had nothing moving on its main machine.
   `PlantRenderer` now draws the tank cells' froth on each of the bank's eight cells (positions from `tools/models/flotation.py`),
   moving while the bank runs (`PlantMotion.rpm(FLOTATION_BANK)` = the froth's 2 rpm). New NeoForge scene `flotation_bank_froth_moves`.
3. **Lead-zinc feed is staged.** Galena and sphalerite are separate ore blocks in the game, so the feed is the fines of one block of
   each, mixed (galena 4.17 %, sphalerite 14.52 % of the slurry), topped up as if from the mill's cyclones. The narration says so.
4. **The plain (bulk) line** is episode 5's small-plant layout beside the selective line, fed the same feed, to show the zinc lost.
5. **The froth readout** on the cards is the game's own split (`Selective.bank`, the 75 % rung) applied to the feed the bank holds:
   the thickeners use their inlet slurry in the same tick it arrives, so their buffers read empty between ticks.
6. **The setting** is flipped by the scene (`toggleSelective`); in play it is a sneaking click with an empty hand. Said in the narration.
7. **Two thickeners and filters** each pay clean cakes (as the doc says); real concentrates carry a few percent of the other metal: said.
8. **Narration length:** 1,234 words, under the brief's 1,400-1,700; the episode runs 8:18 with the cards, inside 8-12 min. I did not
   pad it.

Tests: `build` passes; the full NeoForge suite passes (286 of 286, the two new scenes included).
