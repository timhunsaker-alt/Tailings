# L7: dense-medium separation (2026-11-10), decided while Tim was away

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back".
Built on branch `l7-dms` from the draft spec [dense-medium-separation.md](../multiblocks/dense-medium-separation.md), whose own choices
are in `specs-tailings.md` (L7-1 to L7-3). Format as in `README.md` here. **#4, #5 and #8 most need your eye.** Nothing here changes
an existing world (#1).

### 1. Does anything change for a plant already built?
- **Chosen:** no. DMS is four new parts and a new item. A plant without a DMS cyclone module runs exactly as before: the screen, its
  gravel face, the cone, the belts and the mill are untouched, and the existing scenes (`screen_gravel`, `crushing_circuit`,
  `playthrough_crushing`, the playthrough's plant) pass unchanged. No setting was needed, unlike L1.
- **Why:** the rule for these waves: a built plant pays exactly what it pays today.
- **Alternatives:** none needed.

### 2. The double-deck screen's gravel face (L7-3): already built, used as it is
- **Chosen:** no new screen part. The gravel face was built in the plant-gaps wave (`plant-gaps.md` #4): a belt on the west face of
  the screen's south end takes the 6-40 mm, 35 % of crushed rock and all the cone's gravel that is not sand. DMS sits on that belt:
  its head end over the deslime screen's north end. Without a DMS the belt still ends in a chest as `gravel_*` items, as today.
- **Why:** L7-3 asked this wave to build the face; it exists and behaves as the spec describes, so building it again (or a second
  screen) would only change existing worlds.
- **Alternatives:** a new `dms_screen` part with its own face (needed only if the face had to change).

### 3. A piece's density
- **Chosen:** the mixing rule over the whole block: 1 / SG = sum of (mass share / SG) of each mineral the block holds (the plant counts
  a block as 2.7 t), the rest of the block its host rock. Textbook mineral densities (galena 7.58, sphalerite 4.05, chalcopyrite 4.2
  for the vanilla copper, hematite 5.26 for iron, cassiterite 6.99, ...); host rocks granite and andesite 2.65, stone and cobble 2.70,
  deepslate 2.80, diorite 2.85, tuff 2.40, sandstone 2.35 (porous), ores in quartz 2.65, the platinum reef's pyroxenite 3.2 and the
  nickel ore's norite 3.0 *(host values from textbook ranges, unverified)*. The game's copper ore is about 3.15, galena ore 3.14,
  sphalerite ore 3.72, tin ore 2.88, iron ore 2.96.
- **Why:** it is how a real piece's density adds up (volumes add, not masses). The spec wrote "mass-weighted"; its two examples did
  not agree with each other under either rule, so the real rule was used.
- **Real counterpart:** heavy-liquid sink-float tests, which plants run before building a DMS, measure exactly this.
- **Alternatives:** a density written per rock (simpler to read, but not tied to the minerals the rock holds).

### 4. What floats at the default 2.75, and the light ores (needs you)
- **Chosen:** at 2.75 granite, andesite, stone, cobble, tuff and sandstone float; diorite and deepslate sink with the ore; the metal
  ores sink. The light industrial ores (rock salt, potash, borax, native sulfur, graphite, bauxite, bentonite) **float at any
  setting** and are lost as gravel, as they would be in a real DMS. The module's status names the rock it is treating, its density
  and whether it floats, so a player sees it happen.
- **Why:** real DMS is used on dense ores (lead-zinc, tin, tungsten, spodumene, fluorspar, iron, chrome, diamonds) and never on salt or
  potash. Making those sink would be inventing physics.
- **Question:** is this trap acceptable, or should the module pass rocks it should never see (anything lighter than the medium's
  bottom, 2.6) straight through to the sinks? The player can already keep them off the DMS line with the ore sorter.
- **Alternatives:** pass-through for those rocks (unreal but forgiving); a warning on the module only.
- **Tim (2026-11-10):** floats ride a belt, so the light ores can be recovered. Built: see #11 (answers-2026-11-10.md #7).

### 5. The sinks go round again (needs you)
- **Chosen:** the sinks go out on a belt off the sinks screen's south end, to the cone as the spec says. The cone's gravel comes back
  over the screen, and what is still 6-40 mm comes back to the DMS, which treats it again. Each pass sends 2 % of the ore to the
  floats, so over the circuit about 5 % of the ore that reaches the gravel face is lost, about **3 % of an ore block's mineral**.
  Country rock is about 65 % rejected (what passes 6 mm before reaching the DMS goes on to the mill).
- **Why:** it is the closed circuit the plant already has; nothing in a rock load marks a piece as already separated.
- **Real counterpart:** real DMS sinks go to secondary crushing and the mill, not back to the DMS; real plants reject 20-60 % of the
  mass at over 90 % recovery, which this matches.
- **Alternatives:** mark the sinks so a second pass lets them through (needs a mark on rock loads, a change to every belt and
  part); send the sinks to the ball mill (the mill takes sand only).

### 6. Liberation and misplacement
- **Chosen:** a whole block's gravel floats or sinks as one (the spec's "block's mean"); 2 % of each side goes the wrong way, flat,
  whatever the margin between the piece and the medium.
- **Why:** the spec; a size class carries no distribution of piece grades to make a partition curve from.
- **Alternatives:** a real partition curve (an Ep of about 0.02-0.03 for a DMS cyclone) would send more of the rocks near the cut
  the wrong way, for example tin ore at 2.88 against a 2.85 medium.

### 7. How the parts join
- **Chosen:** gravel from a belt over the **deslime screen**'s north end; it washes off 3 % *(proposed)* as sand to a belt on its
  east face (back to the sand belt) and passes the rest into the **DMS cyclone module** it touches (on top of it, as the mixing box
  is fed in a real plant). The module splits the gravel into the **drain-and-rinse screen** on its **north** side (the floats) and the
  one on its **south** side (the sinks). The **medium recovery** touches it anywhere. The module also takes gravel by hand or hopper.
- **Why:** parts hand rock by touching, as the dry mill and the cyclones do; the two screens are one part, so which side they are on
  tells them apart (the cyclone's overflow and spigot ends in the model).
- **Alternatives:** belts between every part (more belts, nothing gained); two different screen parts.

### 8. Ferrosilicon (L7-2): a placeholder recipe (needs you)
- **Chosen:** a `ferrosilicon` item, a 25 kg bag of milled 15 % Si ferrosilicon. Placeholder recipe, shapeless: an iron ingot, a sand
  and Wildcatter's metallurgical coke make **4 bags** (100 kg: about the 85 kg of iron an ingot counts for, and the silicon). Its icon is
  Ludo art (1 credit).
- **Why:** Wildcatter's ferroalloy plant (its `ferroalloys.md`) is planned and makes FeSi 75 in 1 t bags; Tailings does not refine
  ferroalloys. When it exists, the recipe should become FeSi 75 diluted with steel scrap, or a direct conversion.
- **Question:** keep this recipe until then, or make it from Wildcatter's steel and silicon items when they exist?
- **Alternatives:** magnetite medium from the magnetic separator (real for coal; too light for a 2.75 medium in practice).
- **Tim (2026-11-10):** make the medium from Wildcatter's ferrosilicon bag (FeSi 75 + iron to FeSi 15) once Phase 17 is released; keep the placeholder until then. Kept; it waits for Wildcatter 0.7.0 (answers-2026-11-10.md #7).

### 9. Ferrosilicon losses, and no starting charge
- **Chosen:** 0.3 kg a tonne of feed *(proposed; real 0.1-0.5)* comes out of the medium recovery's hopper (1 t, 40 bags). With an
  empty hopper the module stops, NO MEDIUM. There is no starting charge: the medium in the circuit is taken as already made up.
- **Why:** a 60 t/h circuit loses about 18 kg an hour, a bag every 80 t or so; a real circuit holds several tonnes of ferrosilicon
  (about 30 % by volume at 2.75), which would be 100-odd bags before the first tonne.
- **Alternatives:** require a charge of a tonne (40 bags) before the first run, given back when the module is broken.

### 10. Setting the medium's density
- **Chosen:** a sneaking empty-hand click on the module steps it 0.05 up, 2.60 to 3.30, then back to 2.60. The PLC reads it on
  DENSITY (4-20 mA over 2.6-3.3). Saved with the module.
- **Why:** the spec's "set on the module's screen"; a click is the conditioning tank's way of setting its mode, and a menu with its own
  packets on both loaders is a lot of code for one number.
- **Alternatives:** a screen; a PLC setpoint (DENSITY SET 4-20 mA), as a real density controller runs.

### 11. The floats: vanilla gravel, by hopper or hand only
- **Chosen:** the floats screen pays a vanilla gravel block for each 1.9 t into its product slot; a hopper under it or a hand takes it.
  Two blocks' worth waiting and a full slot: FULL. Floats do not go on a belt.
- **Why:** the spec (1 block for each 1.9 t, open-pit.md's loose gravel); belts carry plant rock by kind and size, and gravel is not a
  plant rock. A block of rock (2.7 t) gives about 1.4 blocks of gravel: it bulks.
- **Alternatives:** floats on a belt as rock to a stockpile bin (a new kind of load).
- **Tim (2026-11-10):** floats ride a belt. Built: a belt off the floats screen's north end takes the floats as gravel with their rock, back to the plant; with no belt there, vanilla gravel as before (answers-2026-11-10.md #7).

### 12. The rates and the water
- **Chosen:** the module 60 t/h *(unverified, the spec)*, the deslime and both drain-and-rinse screens the same; 0.15 L of make-up
  water a kg at the module *(proposed)*, held 10 s (25 L), NO WATER without it.
- **Why:** on country rock the gravel face sends about 33 t/h at 50 t/h of feed. On ore, whose sinks come round again (#5), the
  gravel through the DMS can reach about 80 t/h; then the module's hold fills, the belts back up and the screen sends what the
  gravel face's belt has no room for round to the cone, as it does today, so the plant slows nowhere. Most of a DMS plant's water goes
  round through the medium recovery; only the make-up is drawn.
- **Alternatives:** a water draw on the screens too (real spray water, about 1 m³ a tonne, almost all returned).

### 13. Motors and voltage
- **Chosen:** deslime screen 11 kW, module 55 kW (its medium pump), drain-and-rinse screens 7.5 kW each, medium recovery 5.5 kW, all on
  the world's industrial tier read from Wildcatter's standard (480 V in a North American world), with the plant motor's PLC points.
  Each runs its own motor while it has work; the module waits for the medium recovery's.
- **Why:** the spec's figures; the plant's rule that every part runs its own motor.
- **Alternatives:** none.

### 14. PLC points
- **Chosen:** the module adds DENSITY (4-20 mA over SG 2.6-3.3, the setting) and FLOATS (on for a second for each tonne floated) to
  RUN, RUNNING, STALLED, AUTO, AMPS and FILL.
- **Why:** the spec.
- **Alternatives:** a measured density with drift (real gauges read the medium, which wanders with slimes; not modelled).

### 15. Recipes, models and sounds
- **Chosen:** placeholder recipes of Wildcatter steel and an industrial cable round a distinct centre each (water bucket and iron
  bars, hopper and cauldron, two iron bars, bucket and redstone block), so none collides. The four parts are drawn in Wildcatter's
  materials by `tools/models/dms.py`. The screens sound the vibrating screen, the module the slurry rush, the recovery Wildcatter's
  motor hum. No `*_placeholder` texture was made.
- **Why:** as L6.
- **Alternatives:** real-part recipes once Wildcatter has pumps and magnets as items.

### 16. Diamonds (X-2): not built
- **Chosen:** not built. DMS is the real route for diamonds; it waits on your answer to X-2 (`specs-tailings.md`).
- **Why:** the question is open.
- **Alternatives:** a kimberlite rock with diamonds as a trace, recovered off the DMS sinks by an X-ray sorter (L8's machine).
