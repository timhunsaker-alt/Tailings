# L8: the X-ray ore sorter (2026-11-10), decided while Tim was away

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back".
Built on branch `l8-xrt` from the draft spec [xrt-ore-sorter.md](../multiblocks/xrt-ore-sorter.md), whose own choice is L8-1 in
`specs-tailings.md` (the dump sorter stays; the XRT sorter is a separate two-way machine). Format as in `README.md` here. **#3, #5 and
#6 most need your eye.** Nothing here changes an existing world (#1).

### 1. Does anything change for an existing world or the dump sorter?
- **Chosen:** no. The XRT sorter and its compressor are two new parts; nothing existing calls them. The dump station's `ore_sorter`
  (its screen, its PLC gates and rock codes, its four sides) is not touched: its stage, `Sorter` and `SorterMenu` are unchanged. The
  only shared things are the plant part's block entity (the new sorter's state is a field that is null on every other part, saved
  only for the new sorter) and the `ore_sorter` sound, which the new sorter also plays. No setting was needed, unlike L1.
- **Why:** the rule for these waves, and L8-1 ("the dump sorter stays exactly as it is").
- **Alternatives:** none needed.

### 2. How it reads a piece: its effective atomic number, from the real formulas
- **Chosen:** each piece is the rock it came from. The sorter reads its **effective atomic number** (Zeff): each mineral's Zeff from
  its real chemical formula by the photoelectric power-law mean (Zeff = (Σ fᵢ Zᵢ^2.94)^(1/2.94), fᵢ each element's share of the
  electrons), the piece's minerals at their game grade mixed the same way, the rest of its 2.7 t taken as quartz. Quartz reads 11.6,
  galena 77, gold 79. Country rock reads 11.6-12.0; galena ore 45, sphalerite ore 25, cassiterite ore 23, molybdenum ore 13.8.
- **Why:** dual-energy X-ray transmission measures exactly this, a piece's mean atomic number ("atomic density"), which is what tells a
  sulfide or heavy oxide from a silicate. Working it from formulas means no invented table: every number comes from the periodic table
  and the game grades already in `Mineral`. `plant.XrtSorting` does it; `XrtSortingTest` checks quartz, galena and gold.
- **Real counterpart:** the effective-atomic-number rule (Mayneord and Spiers' exponent 2.94) used for X-ray contrast; XRT sorters'
  makers describe their reading as "atomic density".
- **Alternatives:** the spec's "the ore minerals' share at the game grade" by mass (simpler, but treats an 80 % graphite or borax
  rock as rich ore, which X-rays see as lighter than quartz); the DMS's mixed density (L7's rule; reads the wrong property for X-rays).

### 3. The five threshold steps and the default
- **Chosen:** steps at Zeff **11.5 (LOW), 12.0, 13.0, 15.0, 20.0 (HIGH)**; a new sorter starts at **12.0**.
  - LOW is under every country rock: nearly everything is blown to product (the spec's `xrt_threshold`).
  - 12.0 is just over the heaviest country rock, deepslate (11.96): every country rock goes to waste, every ore heavier than quartz to
    product, down to silver ore (12.08, 0.3 % acanthite).
  - HIGH (20) sends the low-grade ores (molybdenum 13.8, cobalt 14.9, vanilla copper 15.0, vanilla iron 14.5, the salts) to waste and
    keeps the rich heavy ores (galena, sphalerite, cassiterite, chromite, scheelite, pitchblende, gold).
- **Why:** spaced where the real rocks fall, so each step changes what goes where.
- **Needs your eye:** the default sits only 0.04 over deepslate and 0.08 under silver ore. That is real (an XRT sorter on a 0.3 %
  silver ore would be set on a knife's edge, or not used), but it means a small change to a rock's trace list moves deepslate or silver
  across it. `XrtSortingTest` will fail if one does.
- **Alternatives:** steps in round units of grade; a default at 12.5 (more margin; silver ore then goes to waste at the default).

### 4. The errors: fixed shares, not chance
- **Chosen:** of each rock's kg over the threshold, exactly 3 % goes to waste; of each under it, exactly 5 % goes with the product.
- **Why:** the spec's figures. The game gives every piece of one rock the same reading, so the errors stand in for the real spread
  (the spec's "What it cannot see"); a fixed share keeps the tests exact and the plant's numbers steady.
- **Alternatives:** a random draw a piece (truer to a stream, noisier to read).

### 5. Light ores read under quartz: this sorter cannot upgrade them
- **Chosen:** graphite (7.8), magnesite (9.4), borax (9.6), bauxite (10.7), spodumene (11.3) and bentonite (11.4) read under quartz, so
  at every step but LOW they go to waste with the country rock. The platinum reef (63 g a tonne at the game grade) reads as country
  rock: its platinum cannot be seen.
- **Why:** true of X-ray transmission. Real XRT plants sort these the other way round (coal: eject the heavy ash pieces; magnesite:
  eject the heavy silicate), or use another sensor (optical, NIR), which the spec leaves out.
- **Needs your eye:** should the sorter have a second mode that **ejects the light pieces** as product, for these ores? It is real
  (the ejectors blow whichever stream is set) and a small change: a second toggle on the screen and a REVERSE point.
- **Alternatives:** a reverse mode (above); leave it, as built.
- **Tim (2026-11-10):** add a reverse mode (sneak-click or a PLC point) that keeps the low readers. Built: the sneaking click steps LOW to HIGH, then LOW to HIGH in reverse; the PLC's REVERSE (point 9) sets it in AUTO; a new sorter sorts as built (answers-2026-11-10.md #8).

### 6. Compressed air: its own compressor, not Wildcatter's
- **Chosen:** a `sorter_compressor` part, **2 x 2 x 3, 160 kW** on the industrial tier (the spec's figure): a screw compressor and air
  receiver that runs while it touches a sorter with feed. The sorter needs it touching and running.
- **Why:** Wildcatter's `air_compressor` (0.5.1) is instrument air: **7.5 kW**, a receiver between 6 and 8 bar that leaks down, read
  as a pressure through air tubing and a dryer by valve actuators that use no air. A real 1.2 m XRT sorter's 192 ejectors use about
  20-30 m³/min at 6-7 bar, which takes a screw compressor of 110-160 kW; Wildcatter's gives about 1 m³/min and has no flow to draw.
  Reading its pressure would let one 7.5 kW unit run four 250 t/h sorters.
- **Real counterpart:** XRT sorters are sold with their own compressor and receiver; plant air and instrument air are kept apart.
- **Needs your eye:** if Wildcatter grows a plant-air system with flow (a compressor rated in m³/min and users that draw it), the
  sorter should draw from it instead.
- **Alternatives:** Wildcatter's instrument air by tubing (cheap, wrong scale); a plant-air header part in Tailings.

### 7. With no air it stops
- **Chosen:** with its compressor missing or not running, the sorter reads **NO AIR** and holds its rock; nothing goes to waste.
- **Why:** the spec ("so ore is never lost by a stopped compressor"). A real sorter's feeder is interlocked to the air pressure.
- **Alternatives:** everything to waste (what a real sorter's chutes would do without the interlock).

### 8. No sizing screen
- **Chosen:** the spec's `sizing_screen` was not built. The sorter takes **crushed rock** (the game's 40-150 mm class) at its full
  250 t/h and **gravel** (6-40 mm) at half; it refuses sand, fines and unbroken rock.
- **Why:** the game's size classes already sit inside the sorter's 10-125 mm range (crushed rock about it, gravel at its fine end);
  a screen that split them again would have nothing of its own to do, and the double-deck screen already makes gravel.
- **Alternatives:** build the 22 kW sizing screen with a +125 mm return to the crusher (needs a new size class).

### 9. Its rating and motor
- **Chosen:** 250 t/h on crushed rock, 125 t/h on gravel, at its motor's output; **20 kW** for its belt, feeder, X-ray source and
  electronics, on the industrial tier (*unverified*, the spec's figure).
- **Why:** a maker's 1.2 m machine is rated up to 250 t/h at 50-125 mm; a Russian 1.2 m separator's table halves it at 25-50 mm. One
  sorter is five times the 50 t/h plant, so it is never the plant's bottleneck.
- **Alternatives:** the Russian table's 186 t/h at 50-100 mm.

### 10. Its faces
- **Chosen:** fed by a belt over its **north (feed) end**'s top (the first third, where the vibrating feeder is), a hopper or a hand;
  **product** out of its **south** face (the ejected pieces over the splitter), **waste** out of its **east** face; each onto a belt or a
  part's feed face. Both must have room, or it reads FULL; a face with nothing: "its output has nowhere to go".
- **Why:** the plant's rules for faces (crushing-circuit.md); real sorters split the two streams at the head pulley, the product
  thrown furthest.
- **Alternatives:** waste out of the bottom (would need a belt under the part).

### 11. Setting the threshold: a sneaking click, and the PLC
- **Chosen:** a sneaking click with an empty hand steps the screen's threshold, LOW to HIGH and round. The PLC's **THRESHOLD** point is
  an analog output, 4-20 mA over the five steps (4 mA a step); in AUTO, while the loop keeps coming, it sets the step over the
  screen's, which is kept; quiet, the screen's comes back.
- **Why:** the conditioner's selective switch is set the same way; the dump sorter's PLC rock codes fall back the same way.
- **Alternatives:** a screen of its own (a menu; more code for one setting).

### 12. PRODUCT and WASTE: pulse counters
- **Chosen:** two **pulse** inputs, a pulse a tonne each way, as a running count (Wildcatter's PULSE signal), kept through saves.
- **Why:** the spec ("pulse a tonne each"); a belt scale's totaliser gives a pulse a tonne the same way.
- **Alternatives:** 4-20 mA rates in t/h.

### 13. What sorting pays
- **Chosen:** sorting changes no rock: the product is the same crushed rock, and nothing pays more per tonne of ore. What it gives is
  fewer tonnes to mill: on a mix of ore and country rock, about 95 % of the country rock never reaches the mill. The 3 % of ore it
  drops to waste is lost with the waste, and the 5 % of country rock it keeps carries its traces on as now. So the ladder (2x, 3x,
  4x) is untouched.
- **Why:** pre-concentration is a throughput and cost gain, not a recovery gain; a real sorter loses a little metal to win mill room.
- **Alternatives:** none proposed.

### 14. Where it goes
- **Chosen:** anywhere on a belt line; nothing enforces a place. The spec's places (between the jaw and the screen on the 50 t/h plant;
  four of them on the pit line) are advice in the docs.
- **Alternatives:** none.

### 15. Recipes, art and sounds
- **Chosen:** placeholder recipes of Wildcatter steel and an industrial cable: the sorter with a lead ingot (its shielding), two
  observers (its sensor) and a belt conveyor; the compressor with a piston and an iron block. Both parts are drawn in Wildcatter's
  materials by `tools/models/xrt.py`; no texture was drawn by script and no Ludo art was used (no item needed an icon). The sorter
  plays the dump sorter's `ore_sorter` loop (air jets), the compressor Wildcatter's motor hum.
- **Alternatives:** real-part recipes once Wildcatter has an X-ray tube; a compressor sound of its own (the art wave's).

### 16. The X-rays
- **Chosen:** no harm outside the part: the cabinet is shielded, as the spec says; the model carries its warning signs.
- **Why:** real XRT sorters are shielded cabinets licensed as such; nothing leaks in normal running.
- **Alternatives:** a dose near an open cabinet (would need Wildcatter's radiation rules).
