# L6: the dry mill (2026-11-10), decided while Tim was away

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back".
Built on branch `l6-dry-mill` from the draft spec [dry-mill.md](../multiblocks/dry-mill.md), whose own choices are in
`specs-tailings.md` (L6-1 to L6-3). Format as in `README.md` here. **#3, #9 and #14 most need your eye.** Nothing here changes an
existing world (#1).

### 1. Does anything change for a plant already built?
- **Chosen:** no. The new rungs come only from the new parts. A plant without a scavenger spiral bank sends no HMC down its pipes, so
  its filter, leach and cells pay exactly as before; the filter's HMC code does nothing while it holds none. The existing scenes
  (`spiral_bank`, `full_recovery`, the playthrough's plant) pass unchanged. No setting was needed, unlike L1.
- **Why:** the rule for these waves: a built plant pays exactly what it pays today.
- **Alternatives:** none needed.

### 2. How the scavenger spirals' share travels apart from the spirals' own
- **Chosen:** in the slurry's assay, under its own names: what the scavenger spirals take is `tailings:hmc_zircon` (and so on), what
  they leave is `tailings:scavenged_zircon`. The thickener carries both as it carries any entry; the filter keeps the HMC apart; the
  leach, a second set of spirals and the flotation banks know neither name, so they take nothing of them. Selective flotation already
  travels its held-down sphalerite this way.
- **Why:** both concentrates can share one thickener and filter, as the spec draws them, and no mineral pays twice: 50 + 45 = 95 %,
  and the last 5 % is lost to the tails, as in a real plant. Without the "scavenged" mark the leach would take 90 % of that 5 % again
  and a block would pay over 4x.
- **Alternatives:** a separate product on the pipe for HMC (needs a Wildcatter `Product`); a thickener and filter of its own for the
  scavengers (more parts, and the spec joins the two).

### 3. What goes through the dry mill: only the scavengers' share
- **Chosen:** the filter still pays the spiral bank's 50 % as items, as today, even with a dryer touching it. Only the scavenger
  spirals' 45 % is HMC: it goes to the dryer (or is bagged), and the separators pay it at the top rung's 1.11 a kg.
- **Why:** the spec says both "the filter's cake goes into the dryer" and "the separators pay 1.11 a kg won". Both together would pay
  the spirals' half at 1.11 too, over 4x. Keeping the spirals' half at the filter keeps every plant's 2x as it is and pays the 4x
  exactly.
- **Real counterpart:** a real mineral sands plant sends all its HMC, primary and scavenger, through the dry mill.
- **Alternatives:** the whole HMC through the dry mill, the separators paying the primary share at 1.0 and the scavenger share at 1.11
  (truer flow; the dry mill would then carry the 2x as well, and the filter's 2x would move for anyone who builds a dryer).
- **Tim (2026-11-10):** all HMC through the dry mill, as a per-world rule on in new worlds. Built: `dry_mill_all_hmc`; a filter with a dryer touching it sends the spiral bank's share too, paid at 1.0 a kg (the scavengers' at 1.11), so the line still makes 4x; with no dryer, or the rule off, as before (answers-2026-11-10.md #9).

### 4. Which minerals the dry mill takes
- **Chosen:** ilmenite, rutile, zircon, monazite and cassiterite (L6-1's list). Iron, chromite and scheelite, which the spirals also
  take, keep their leach exception; the scavenger spirals pass them untouched.
- **Why:** these five are what a mineral sands dry mill separates by magnetism and conductivity. Chromite (weakly magnetic) and
  scheelite are separated in their own plants, not a mineral sands mill.
- **Alternatives:** chromite with the induced-roll magnet (real in some plants); iron by a wet magnetic separator (a different
  machine).

### 5. How each mineral reports
- **Chosen:** the rare-earth roll magnet takes ilmenite; the high-tension roll takes rutile and cassiterite (conductors); the
  induced-roll magnet takes monazite at high intensity, and what it leaves, zircon, is its second product. Each takes all of its
  mineral (the spec's "mixed products are not modelled").
- **Why:** the real properties (dry-mill.md). Real mills recycle middlings to reach grade; the game counts the result.
- **Alternatives:** a real split with misplacement (a share of each mineral to the wrong product; then the 4x would need topping up).

### 6. How the parts pass dry HMC: by touching
- **Chosen:** the filter feeds a dryer touching it; the dryer a roll magnet touching it; the magnet a high-tension roll; that an
  induced roll. No belt or chute between them.
- **Why:** dry HMC is a few tonnes an hour of fine sand, carried by bucket elevators and chutes in a real mill; Tailings' belts carry
  rock by kind and size, not sand by assay. Touching is how the blower serves the bank today.
- **Alternatives:** belts carrying HMC (a new kind of load on belts); a bucket elevator part.

### 7. The products leave as items, not into product bins
- **Chosen:** each separator pays into four output stacks of its own, which a hopper under it or a hand takes, as the filter's two.
  No `product_bins` part was built.
- **Why:** the spec's bins would be ore bins that hold rock items; the products are concentrate items already, and a hopper and chest
  do the same job.
- **Alternatives:** a product bin a separator as the spec lists (one more part, and four of them in a line).

### 8. The items the separators pay
- **Chosen:** the ladder's items, by the nuggets rule: `ilmenite_concentrate`, `rutile_concentrate`, tin ingots for cassiterite,
  `monazite_concentrate` (on to the by-product plant's rare earths), `zircon_concentrate`. The same items the filter pays.
- **Why:** the spec; one item a mineral whichever rung paid it.
- **Alternatives:** new "dry mill grade" items (no use in either mod yet).

### 9. The size: 2 t/h a part, and HMC bagged when the dryer is full
- **Chosen:** the dryer and each separator treat 2 t/h of HMC (the spec's figure); the dryer's bin and each separator's hopper hold
  1 t. When the dryer's bin is full the filter bags the rest, 100 kg a bag, to be fed later.
- **Why:** on trace rock (granite, sandstone) a 50 t/h plant makes well under 1 t/h of HMC. On Wildcatter's heavy mineral sand (900 kg of
  heavy minerals in each 2.7 t the plant counts a block), the scavengers' share is about 7.5 t/h: the dry mill falls behind and the bags pile up, as a real
  HMC stockpile ahead of a dry mill does (real dry mills often run apart from the wet plant).
- **Real counterpart:** induced-roll magnets about 4 t/h a metre, HT rolls 1-2 t/h a metre; real mills run banks of separators.
- **Alternatives:** size the dry mill at the plant's rate (25 t/h: not a 1.5 m roll); allow several separators in parallel.

### 10. The dryer's gas
- **Chosen:** any natural gas on a pipe at its east side (pipeline gas, dried gas, gas, compressed gas, as Wildcatter's gas burners
  take), 0.3 GJ a tonne at Wildcatter's heating value (`Tuning.GAS_JOULES_PER_M3`, 38 MJ/m³): about 7.9 m³ a tonne, 16 m³ an hour at
  2 t/h. Under half the gas it needs: NO GAS. Its 30 kW blower is its motor on the world's industrial tier.
- **Why:** drying 5 % moisture off a tonne of sand and heating it to 120 °C is about 0.2 GJ, about 0.3 with a real dryer's losses.
- **Alternatives:** an electric dryer (real but rare at this size); steam off Wildcatter's boilers.

### 11. Wet HMC into a separator
- **Chosen:** a separator offered a bag of HMC refuses it and reads WET; a hopper cannot put one in.
- **Why:** the spec ("dry only"); moist grains stick and electrostatic separation needs dry feed.
- **Alternatives:** a separator that takes wet feed at a loss.

### 12. The HMC bag
- **Chosen:** `heavy_mineral_concentrate`, one item a 100 kg bag, holding whole kg of each mineral as item data (so bags of one feed
  stack); the fractions wait in the filter for the next bag. Its tooltip reads its assay. A bag from the creative tab holds nothing.
- **Why:** the spec's "kept with its assay"; whole kg keeps stacks usable.
- **Alternatives:** a bag a tonne (fewer items, slower to fill on trace rock); exact assays (never stack).

### 13. A scavenger spiral bank with no spiral bank before it
- **Chosen:** allowed. It takes 90 % of what reaches it, which the dry mill pays at 1.11: 4x, the cap. A second scavenger after it,
  or a spiral bank after it, takes nothing (#2).
- **Why:** it cannot know what came before it; the result never passes 4x.
- **Alternatives:** have it read NO FEED unless the feed is a spiral bank's tails (needs a mark on the spirals' tails, which would
  change today's slurries).

### 14. The zircon compat to Wildcatter (L6-3): proposed only, not built
- **Question for Tim:** should Wildcatter's chlorinator accept Tailings' `zircon_concentrate` as zircon by kg (a compat entry in
  Wildcatter), so a Tailings player skips Wildcatter's small `mineral_separator`?
- **Recommendation:** yes, as a tag in Wildcatter (`#wildcatter:zircon_feed`) holding its own zircon and, when Tailings is present,
  Tailings' `zircon_concentrate` at its item's 56.25 kg. Nothing was built: it changes Wildcatter.
- **Alternatives:** a crafting conversion in Tailings; leave the two chains apart.
- **Tim (2026-11-10):** yes, Wildcatter's chlorinator takes Tailings' zircon concentrate. Not buildable from Tailings' side (no Wildcatter tag or API takes it); what Wildcatter needs is written in (answers-2026-11-10.md "Wildcatter needs").

### 15. Recipes and art
- **Chosen:** placeholder recipes of Wildcatter steel, an industrial cable and a distinct centre each (a bucket, a blast furnace,
  an iron block, a copper block, an iron and redstone block), so no two collide. The five parts are drawn in Wildcatter's materials by
  `tools/models/drymill.py`; the HMC bag's icon is Ludo art (1 credit, `art/source/PROMPTS.md`). No `*_placeholder` texture was made.
- **Why:** the plant's recipes are all placeholders; distinct shapes avoid two parts sharing a pattern, as several Phase 4 parts do.
- **Alternatives:** real-part recipes (motors, rolls, magnets) once Wildcatter has the parts.

### 16. Sounds
- **Chosen:** the scavenger spirals use the spirals' slurry rush, the dryer Wildcatter's fan, the separators Wildcatter's motor hum. No
  new sounds.
- **Why:** an existing loop for each; new sounds are the art wave's.
- **Alternatives:** a roll separator's rattle and the dryer's burner roar (to record).
