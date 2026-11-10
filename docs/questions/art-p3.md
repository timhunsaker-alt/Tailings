# Open pit P3 and P4 art (2026-11-09): decisions taken while Tim was away

Tim (2026-11-09): "close all known gaps". The script-drawn `*_placeholder` textures of P3 (blasting) and P4 (dry stack) are now
Ludo art, made as the art wave made the rest (`docs/questions/art-wave.md`; prompts in `art/source/PROMPTS.md`). 12 Ludo jobs of 2
pictures each, 12 credits. Format as in `README.md` here.

### 1. The same method as the art wave
- **Chosen:** `Photorealistic 3D` (klimt), 2 variations, the better kept; `tools/process_art.py` box-filters them to 16 px (tiled
  blocks, cropped solid-pixel item icons); the two skins painted by their generator from 32 px swatches. Files lose `_placeholder`;
  the item and block models, `tools/models/blast_assets.py` and `PitVehicleRenderer` point at the new names.
- **Why:** one look across Tailings and Wildcatter; a regenerated model and its skin stay in step.
- **Alternatives:** none worth the mismatch.

### 2. One cuttings picture for the six blasthole collars
- **Chosen:** one Ludo picture of drill cuttings, top-down, with the hole drawn dark in its middle and a ring round it of the
  survey stakes' flagging tape (`stake_flag`), dyed in each state's colour: white drilled, yellow primed, orange charged, grey
  stemmed, green logged, red misfired, the colours `BlastholeBlock` and the CHANGELOG give.
- **Why:** the colour is the state's meaning, so the six must differ only in it (as art-wave decision 4, the stake flags). Reusing the
  tape picture costs nothing and the collar's tag looks like the stakes' flags, which is what a blaster ties there.
- **Alternatives:** six pictures (six different cuttings, colours drifting); a separate tag picture (a credit for no gain at 16 px).

### 3. The blasthole column: one rock picture, the hole drawn by script
- **Chosen:** one Ludo picture of grey bench rock for the column's side and end. The side has the hole's line down its middle, drawn
  as dark as the hole (a first try at 40 % darkening was lost among the rock's dark joints); the end has the hole.
- **Why:** a 270 mm hole is 4 px on a 16 px face: placed by script it always lines up from block to block and between the side and
  the end. A picture of a hole would sit wherever Ludo put it.
- **Alternatives:** cropping the rock picture to a larger scale (tried; the joints stayed); a picture of a drilled face.

### 4. The blasting machine's stand is a swatch, not a new picture
- **Chosen:** `vehicle/steel_dark` at 16 px.
- **Why:** the stand is a 2 px leg and a foot plate; dark painted steel is exactly what it is, and it is already Ludo art.
- **Alternatives:** a galvanised pole picture (a credit for 2 px).

### 5. The emulsion truck's paint: one white swatch, dyed red for the stripe and reel
- **Chosen:** a new Ludo swatch `vehicle/paint_white` (white paint with reddish dust); `paint_red` is it dyed in the truck's red by
  `process_art.py`. `pit_blast_vehicles.py` maps its WHITE and RED to them; the drill keeps the haul truck's yellow, tread and steel.
- **Why:** no existing swatch was white; the red parts are the same paint in another colour, so they wear alike.
- **Alternatives:** a separate red picture (would not match the white's wear).

### 6. The blasting machine picture is lettered
- **Chosen:** kept variation (b), whose ARM and FIRE letters are smallest. The prompt said "no text"; Ludo lettered both anyway.
- **Why:** at 16 px no letter survives the box filter: the face reads as a red box with two amber and two red buttons and a key.
- **Alternatives:** a retry with stronger wording (1 credit; the letters are not visible in the game, so not needed).

### 7. Left as they are
- The drill's icon (a tall mast over a wide crawler) and the emulsion truck's (long and low) fill only part of the 16 px square, as
  the haul truck's does; squaring keeps their shape. Read well enough in the contact sheet; better seen in game.
- The `tailings_pond_placeholder` block keeps its id: it is a legacy part's name, not a texture.
- The generator's Java comment said "1024 x 1024" for the drill's 2048 sheet; it now gives the real size.
