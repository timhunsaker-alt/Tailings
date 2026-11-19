# Wave A (2026-11-19): decisions taken building Tim's 11-19 answers

Tim's answers of 2026-11-19 (recorded under each entry in the other files) asked for C4, C6, C13, C15, C23 and C25 to be built, with
the DMS medium from Wildcatter's ferrosilicon bag and the time scale (both answered on 2026-11-10), the dragline scenes that failed
together, the last three quest milestones, and the first public release. Each entry below is a call made on the best recommendation
while building them, as in the other files here: what was chosen, why, and the alternatives.

### 1. Copper's 14 ingots: the block keeps its 400 kg of copper (C4)
- **Chosen:** a copper ore block holds 14 ingots (`Mineral.COPPER`), and a copper ingot counts 400 / 14 kg (about 28.6) in a slurry's
  assay, so the block still carries the same 400 kg of copper in its 2.7 t. A copper nugget is a ninth of an ingot (it was a ninth of
  a quarter-block, which with 14 ingots would have made a nugget 0.39 ingot while nine of them still craft one). The hand line's 2x
  step pays 7 copper ingots.
- **Why:** raising the copper in the block to 1,400 kg would have changed its density and its atomic number, so the DMS would sink it
  at every setting and the X-ray sorter would read it as a heavier rock; the ladder is about what the plant pays, not what the rock is.
- **Alternatives:** 1,400 kg of copper in the block (changes DMS and X-ray results); a separate pay factor on the filter only (the hand
  line and the flotation cells would still pay 4).

### 2. The charger's 80 %: watts from the receptacle's rating (C6)
- **Chosen:** the charger draws 0.8 x root 3 x the receptacle's line-to-line volts x its amps, taken as watts, from Wildcatter's
  plug type of the receptacle it is plugged into (`Battery.chargerWatts`): 19.95 kW on NEMA L22-30 (480 V 30 A), 17.7 kW on IEC 60309
  red 32 A (400 V). 90 % of it goes into the battery, so a flat battery takes 2.6 h at 480 V and 2.9 h at 400 V.
- **Why:** Tim's figures (17.7 kW, 20 kW as before); no voltage is written in Tailings, so a new receptacle type would follow.
- **Alternatives:** 80 % of the apparent power times the 0.95 power factor (19.0 and 16.8 kW: slower than Tim's figures).

### 3. The cab's course while it drives, and while it stands (C13)
- **Chosen:** while the machine drives, Y-, Y+, L- and L+ do nothing and are greyed out on the cab screen, with a tooltip; standing,
  Y+ and Y- go through the PLC's rule (`PlcScale.allowedTarget`): before the first advance any target, while it declines no higher
  than its floor, once level the target it has.
- **Why:** "as the PLC is" (C13): the screen and the PLC now hold the course the same way, and floor already cut cannot be moved.
- **Alternatives:** lock only while driving and leave the standing screen free (it could still move floor already cut).

### 4. Which items are variants of one ore (C13)
- **Chosen:** a deepslate ore is its plain ore (`Sorter.variantOf`: `minecraft:deepslate_copper_ore` is `minecraft:copper_ore`), from
  the screen and from a PLC code alike; a side set to either takes both, and one ore has one side.
- **Why:** vanilla iron, copper and gold are the only ores with a deepslate variant; Wildcatter's ores have one block each.
- **Alternatives:** a tag per ore (`c:ores/copper`): would also take other mods' copper ores, which the plant does not feed.

### 5. The DMS medium recipe: 64 bags from one bulk bag
- **Chosen:** Wildcatter's 1 t bulk bag of ferrosilicon 75 (`#c:ferroalloys/ferrosilicon`), an iron block and four iron ingots
  (about 1.3 t of iron) craft 64 bags of 15 % Si (1.6 t). The old placeholder (iron, sand, coke) is gone.
- **Why:** a real bag diluted to 15 % Si makes 5 t, 200 bags, and a crafting table gives at most a stack; the blend's iron is right for
  the 64 bags, and the rest of the bag's silicon is not counted. 1.6 t lasts a DMS about 5,300 t of feed at 0.3 kg a tonne.
- **Alternatives:** the medium recovery taking the bulk bag and iron straight into its hopper (a real blend, a new hand use and a
  bigger hopper); keeping the placeholder beside it (two routes, one of them not real).

### 6. The paste cure at the world time scale
- **Chosen:** the cure's eight steps are the game day over the scale, rounded to ticks (`PasteFill.cureStepTicks`): 3,000 ticks a step
  at 1:1, 300 at 10:1, 60 at 50:1. Tailings' six rules are on Wildcatter's World Rules screen after Wildcatter's, preset to their
  new-world values; a new world's Tailings rules start from what was chosen there (or `newWorld.tailings.<rule>` in
  `wildcatter.properties`), else the default.
- **Why:** Wildcatter's time-scale spec lists the paste cure as a wait that scales; the screen is where Tim chose rules go.
- **Alternatives:** none built.

### 7. Milestones for the stamp mill, zircon and ferrosilicon quests
- **Chosen:** `tailings:stamp_mill_running` (the stamp mill crushing a piece), `tailings:dry_mill_running` (any dry mill separator
  separating) and `tailings:dms_running` (the DMS module separating), each added to its quest beside its items.
- **Why:** the zircon quest's work is the dry mill, and the ferrosilicon quest's the DMS; no new machine was needed.
- **Alternatives:** a zircon milestone only when zircon itself is paid (the last separator only), which few plants reach early.

### 8. The two dragline scenes that failed together: the scenes, not the game
- **Chosen:** `dragline_cycle` and `pit_pour_dragline` stand the dragline on a bench 96 m long instead of 90 m. The settle rule's rolls
  draw on the level's random source, so another scene running beside them changes them; now and then a block of the cast rolled past
  the 90 m edge and fell 170 m, out of the count, and the cycle scene then saw a second bucket dug. With the wider bench, 8 of 8 paired
  runs pass (about half failed before).
- **Why:** spoil rolling off a bench's edge is right; the scenes only needed room for it.
- **Alternatives:** giving the settle rule its own random source per machine (the scenes would be repeatable, but the rule would
  change for every machine).

### 9. The first release's version
- **Chosen:** 0.1.0, the version the CHANGELOG and `gradle.properties` have carried; the legacy blocks (`dewatering_pump`,
  `flotation_cell`, `dump_station`, `tailings_pond_placeholder`) ship in it and go in the release after (C26).
- **Why:** the first public release; 0.x as Wildcatter's are.
- **Alternatives:** 1.0.0 (says the mod is finished, which its Known gaps say it is not).
