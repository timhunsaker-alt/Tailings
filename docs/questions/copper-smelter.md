# The by-product plant's feed and the copper smelter line (branch `copper-smelter`)

Tim decided on 2026-11-22: fix the by-product plant's inputs if the bug is real, and build a copper smelter line, concentrate to
ingot, for episode 9 of Rock to Metal. Tim was asleep, so each entry below is my best recommendation, built, with its
alternatives. The spec is [`docs/copper-smelter.md`](../copper-smelter.md).

## Part 1: the by-product plant's inputs

### 1. Is the rev2 audit's by-product bug real?
- **Chosen:** yes. Confirmed with a NeoForge scene before any fix. A pressure filter fed one item's worth of pentlandite
  concentrate left 9 loose `tailings:pentlandite_nugget`, and the same happened with molybdenite. The filter packs nine nuggets into
  `Mineral.phaseOneProductId()`, which for a floating mineral with no ingot is `tailings:<mineral>_concentrate`. Those items were
  registered only for minerals with a gravity or magnetic separator, so the pack found no item and stopped. The by-product plant
  matches on that same id (`ByProducts.byItem`), so it could never be fed. Cobalt and rhenium could not be made. Cobaltite and platinum
  had the same gap.
- **Why:** the scenes `byproduct_pentlandite` and `byproduct_molybdenite` failed on main with "the filter packs PENTLANDITE into
  tailings:pentlandite_concentrate, which is not a registered item".
- **Alternatives:** none. It was a bug.

### 2. The fix: register the four missing concentrates
- **Chosen:** register `pentlandite_concentrate`, `molybdenite_concentrate`, `cobaltite_concentrate` and `platinum_concentrate`. These
  are the floating minerals with no ingot. The registration rule is now "a separator **or floats**, and no ingot". The filter now
  packs them, and the by-product plant takes pentlandite and molybdenite concentrate, as `ByProducts` always meant it to. Each
  concentrate of a Wildcatter mineral also crafts one for one into that mineral. It is the same mass: one concentrate item and one
  Wildcatter mineral item are both a quarter of a block. So pentlandite concentrate goes by hand to Wildcatter's fluid-bed roaster,
  which takes `wildcatter:pentlandite`. The route by pipe, thickened concentrate to the roaster, worked before and is unchanged.
- **Why:** this is the smallest change, and it keeps the concentrate a concentrate. The other way, matching the by-product plant on
  `wildcatter:pentlandite`, would also let a plain-mined drop in, and specs-tailings.md X-1 says a mined drop is no plant feed. A
  real nickel refinery takes cobalt out of the nickel concentrate, and a molybdenite roaster takes rhenium out of the concentrate's
  off-gas dust. In both cases the source is the concentrate, not the ore.
- **Alternatives:** match on the Wildcatter mineral item (one line, but drops count as concentrate); leave the filter's nuggets
  loose and have the plant take nuggets (nine clicks an item, unlike every other concentrate).

### 3. Scheelite (and chromite) packed to a dead-end item
- **Chosen:** keep `tailings:scheelite_concentrate`, and add the one-for-one recipe to `wildcatter:scheelite`. Chromite gets the same,
  and for chromite that fully fixes it: Wildcatter's chromate kiln takes `wildcatter:chromite`. **Scheelite still has no item route
  into Wildcatter:** its moly roaster takes scheelite and molybdenite only by pipe, as thickened concentrate on its east side (it dries
  the scheelite into a 1 t scheelite concentrate bag for the aluminothermic vessel). That pipe route works from a Tailings thickener
  today, because the assay carries `wildcatter:scheelite`. The scene `filter_scheelite` checks the filter's pay and the four recipes.
- **Why:** the item route needs a hopper on Wildcatter's roaster, which is a Wildcatter change, outside this branch. Episode 9 can
  show scheelite by pipe.
- **Alternatives:** have the filter bag scheelite into Wildcatter's `scheelite_concentrate_bag` per 1,000 kg (mass-exact, but the
  filter's cake is not the dried concentrate that bag stands for); give Wildcatter's moly roaster a hopper for its two minerals
  (**question for Tim**: worth doing in Wildcatter?).
