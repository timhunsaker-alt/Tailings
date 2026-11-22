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

## Part 2: the copper smelter line (spec [`docs/copper-smelter.md`](../copper-smelter.md))

### 4. What scale?
- **Chosen:** 6 t/h of concentrate, 1.68 t/h of copper. The off-gas carries 0.53 kg/s of sulfur, 94 % of Wildcatter's 150 t/d acid
  plant, so one smelter fills one acid plant. The line counts copper, because the plant's concentrate assay carries only the mineral.
  Its gas, flux, slag and fuel are worked out from a real 28 % Cu, 32 % S concentrate.
- **Why:** a real smelter and its acid plant are sized together, and the acid plant is the part Wildcatter already has. The size
  also matches the 50 t/h plant on mixed ore. A line sized to the pit plant's concentrate thickener (43 t/h of copper, a large real
  smelter) would need about 25 of Wildcatter's acid plants and a 15 MW tankhouse.
- **Alternatives:** size it to the pit plant (one very large smelter and its acid plant, which Wildcatter would also need); size it
  to Wildcatter's 2 t/h nickel smelter (a third of this rate).

### 5. The filter's direct ingots: keep, label, or pay less?
- **Chosen:** label it as the early-game shortcut, and in a **new world** pay it at **85 %**. This is a new world rule,
  `copper_shortcut`, on in new worlds and off in existing ones, like `dry_mill_all_hmc`. A world saved before the rule pays exactly as
  before. The filter says "copper paid as ingots, the shortcut (85 %; a smelter line pays 97.5 %)". With a concentrate dryer touching
  it, the filter sends all its copper to the line instead.
- **Why:** the smelter can only pay what the concentrate holds, less its slag. If the shortcut kept 100 %, the line would pay
  less, so it would not be worth building. 85 % is the share a real mine is paid for the copper in concentrate it sells to a
  smelter: about 96.5 % of the copper is payable, less the treatment and refining charges. The shortcut stands for exactly that sale.
  Per ore block through flotation, the line pays 10.24 ingots' worth as cathodes against the shortcut's 8.9, plus about 1 t of
  acid.
- **Alternatives:** keep it at 100 % and have the line pay better only through its acid (nobody would build it for copper); pay
  the shortcut lower, such as two thirds (a stronger push, but further from a real sale); apply the cut to existing worlds too (it
  would silently lower what running plants pay). **This changes the copper ladder's flotation rung in new worlds from 10.5 to 8.9 at
  the filter. It is worth Tim's look.** Episodes 1-5 never narrate the per-block ladder; episodes 7 and 9 should use the new figures.

### 6. Build all six units now?
- **Chosen:** yes, all six in route order: dryer, flash furnace, converter, anode furnace, casting wheel and tankhouse. A cathode
  melts to two ingots in a **vanilla blast furnace** instead of a seventh machine.
- **Why:** episode 9 needs the whole route on film, and every unit uses the existing plant-part pattern, so each was small. The
  shaft furnace that melts cathode in a real plant is a melting pot with no new process, so the blast furnace stands in for it
  honestly.
- **Alternatives:** a cathode shaft furnace and casting machine (later, ROADMAP); cathodes as the end product (they are a real
  traded product, but the player needs ingots).

### 7. Air or oxygen in the flash furnace?
- **Chosen:** preheated air with gas burners, as the first flash furnaces ran: 1.0 GJ of gas a tonne of concentrate. No oxygen
  plant.
- **Why:** oxygen enrichment for 6 t/h needs about 1,100 Nm3/h of oxygen, eleven of Wildcatter's 100 Nm3/h VPSA units. An air
  furnace's off-gas is about 11 % SO2, which is the strength Wildcatter's burner gas already models. An oxygen furnace's 30-40 % gas
  would need a new product.
- **Alternatives:** oxygen enrichment from Wildcatter's VPSA, with double the rate, no burner gas and a richer off-gas (later).

### 8. The slag: cleaned, or to waste?
- **Chosen:** to waste, counted on the furnace's readout (tonnes, and the copper in it: 2.5 % of the copper smelted). The
  converter's slag goes back to the flash furnace by touching, as a real converter's slag does.
- **Why:** an electric slag-cleaning furnace would bring the loss down to about 1 %. It is cheap in code, but it is a seventh
  machine with its own power and model. It is listed as later.
- **Alternatives:** build the slag-cleaning furnace now; drop the slag as an item (granulated slag) to be trucked to a dump.

### 9. Continuous converter and anode furnace
- **Chosen:** both blow continuously. A real Peirce-Smith converter works a 6-10 hour batch: charge, slag blow, copper blow, pour.
  The anode furnace also works in batches (oxidise, then pole).
- **Why:** the line runs at the plant's own pace without a crane, ladles and a cycle state machine. The readout says "blowing".
  Real continuous converting exists (flash converting), though not in a Peirce-Smith converter.
- **Alternatives:** batch cycles with ladles carried by a crane (later; filmable, but a real piece of work).

### 10. Units hand on by touching
- **Chosen:** filter to dryer, dryer to furnace, furnace to converter (and the converter's slag back), converter to anode furnace,
  anode furnace to casting wheel, all by touching, as the dry mill's dryer and separators do. Anodes go into the tankhouse by
  hopper or by hand, and cathodes come out into a hopper.
- **Why:** this is the repo's pattern for parts that a conveyor or a crane would link in a real plant.
- **Alternatives:** matte, blister and anode copper as items (ladles) carried by the player or a crane.

### 11. Motor sizes and tiers
- **Chosen:** dryer 75 kW, furnace 132 kW, anode furnace 55 kW and casting wheel 30 kW on the industrial tier; converter blower
  450 kW and tankhouse 490 kW (rectifier input and pumps) on the medium tier. All are read from Wildcatter's tiers, never as numbers.
  The tankhouse's rectifier is modelled as a motor load, as the electrowinning bank's already is.
- **Why:** these are the real sizes for this scale (spec, Parts). The two medium-voltage loads each start on a 1 MW gas set
  stepped up (scene `smelter_line`).
- **Alternatives:** the tankhouse on Wildcatter's own rectifier and DC bus (as its nickel tankhouse is): truer, but a much bigger
  build. **Question for Tim:** worth doing later?

### 12. Off-gas to Wildcatter's acid plant
- **Chosen:** the furnace and the converter put Wildcatter **burner gas** on the pipe, from the east face of the back cell's top
  layer. Wildcatter's acid plant converter takes it at its north face, so a smelter needs only the converter and absorbers, not
  the sulfur melter, furnace and boiler. A furnace whose gas cannot leave stops: OFF-GAS HAS NOWHERE TO GO (PLC point OFF-GAS
  BACKED UP). The scene `smelter_acid` pipes the furnace into a real acid plant converter, which makes acid from it.
- **Why:** Wildcatter has the acid plant, and its roasters already work this way ("a roaster must not vent"). Burner gas at
  11 % SO2 is the right strength for an air furnace.
- **Alternatives:** count the sulfur as acid inside the smelter (no Wildcatter tie-in); vent it with a stack figure (not real
  practice).

### 13. Items: anode 400 kg, cathode 57.1 kg
- **Chosen:** `copper_anode` is 400 kg, a whole ore block's copper (14 ingots), and real anodes are 350-400 kg. `copper_cathode`
  is 57.1 kg (two ingots), and real cathodes are 40-60 kg. Both textures are script-drawn **placeholders**
  (`copper_anode_placeholder`, `copper_cathode_placeholder`) and listed in ROADMAP Known gaps. No Ludo art was made this pass.
- **Alternatives:** Ludo icons (one try each) in the next art wave.

### 14. Not built: anode scrap and slimes
- **Chosen:** the tankhouse dissolves whole anodes. A real tankhouse sends back about 15 % of each anode as scrap, and its
  slimes carry the gold, silver and selenium. Both are listed as later.
- **Why:** Tailings' copper ore carries no precious metals today, so slimes would have nothing in them. Scrap would only send
  copper round a loop.

### 15. No Field Manual pages
- **Chosen:** Tailings has no Field Manual (ROADMAP: "There is no Field Manual yet"). Wildcatter's Manual is a fixed list with no
  hook for an add-on. The line is explained by its quest text, each part's readout and the spec.
- **Alternatives:** a Tailings field manual (its own book item); pages added to Wildcatter's Manual (a Wildcatter change).
  **Question for Tim.**

### 16. The quest
- **Chosen:** "Concentrate to metal" after Grind and float, requiring Wildcatter's acid plant quest. Its tasks are a flash furnace
  item and two new milestones: `smelter_running` (the furnace smelting) and `cathode_pulled` (the tankhouse pulling a cathode).
  The reward is 8 sand. The quest text says the line pays 97.5 % against the filter's 85 %.

### 17. Nothing in the client this pass
- **Chosen:** no client screenshot. The client lock was held by the filming agent (`rtm06`), so I did not take it. The models
  were checked from the model tool's previews (`build/model-previews/`), and the parts were checked by the NeoForge scenes.
  **Episode 9 should be screenshotted when the client is free.**
