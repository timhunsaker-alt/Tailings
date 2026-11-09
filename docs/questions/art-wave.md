# Art wave (2026-11-09): decisions taken while Tim was away

Tim (2026-11-09): "close all known gaps". Every `*_placeholder` texture, the reagents' borrowed vanilla icons and the hand-line
machines' vanilla block textures now come from Ludo pictures (`art/source/PROMPTS.md`). Format as in `README.md` here.

### 1. Which Ludo style, and how does Tailings process it?
- **Chosen:** Wildcatter's: `Photorealistic 3D` (model klimt), 2 variations, the better kept. A new `tools/process_art.py` box-filters
  the pictures to Tailings' sizes (16 px blocks and items, as the files they replace), keeps item icons to solid pixels as vanilla's
  are, and copies them into the mod.
- **Why:** Tailings' plant parts are drawn in Wildcatter's Ludo materials, and the two mods share one inventory; one look across both.
- **Alternatives:** a pixel-art style (`16-Bit Pixel`), which reads crisper at 16 px but would stand apart from Wildcatter.

### 2. How are the vehicle skins made from Ludo art, when Ludo cannot draw a UV sheet?
- **Chosen:** five 32 px Ludo material swatches (worn yellow paint, tyre tread, dark painted steel, worn bucket steel, rusty mine car
  plate) plus the muck for loads. `tools/models/pit_vehicles.py` and `vehicles.py` stay the only source: each face takes the swatch
  pixel at its place on the sheet, shaded as before (lighter tops, darker sides, plate edges, ribs, louvres, hazard stripes), and the
  cab windows stay see-through. The skins lose `_placeholder` (`haul_truck.png`, `wheel_loader.png`, `mine_locomotive.png`,
  `mine_car.png`); the boxes, UVs and Java models are unchanged apart from a comment.
- **Why:** the generators lay the UVs out; a picture cannot know them. Swatches keep a regenerated model and its skin in step.
- **Alternatives:** paint the skins by hand in Blockbench (lost on the next generator run), or ask Ludo per face (dozens of
  pictures, never matching at the seams).

### 3. The mine locomotive and car materials
- **Chosen:** the car's sides are the rusty plate, the locomotive yellow paint; rail wheels, couplers and roof are the dark steel at
  60 %, not tyre rubber.
- **Why:** rail wheels are steel; a tread pattern on them would be wrong.
- **Alternatives:** a separate Ludo wheel steel.

### 4. One flag picture for seven stakes
- **Chosen:** one Ludo picture of white flagging tape, dyed in each stake's colour (the colours the script flags had).
- **Why:** the colours are the stakes' meaning (load green, loadout blue, dump red, ...); one cloth keeps them alike but for colour.
- **Alternatives:** seven pictures in seven colours (seven different creases, and Ludo's colours would drift).

### 5. Not done in this wave
- The rock-size items (crushed, gravel, sand, fines), nuggets, minerals and concentrates use one vanilla icon per kind across every
  rock. They were not on the gaps list; a per-rock icon set is about 120 pictures (60 credits), a wave of its own. Now listed in
  ROADMAP's Known gaps.
- `tools/models/build.py` was not re-run: no part model changed. The skins' Java models were re-written by `pit_vehicles.py` (only
  their comment changed).
