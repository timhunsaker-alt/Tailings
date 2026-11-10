# Ludo prompts

The prompts behind the Ludo pictures in this folder, so a picture can be remade or a new one matched. Tailings' textures follow
Wildcatter's pipeline (its `art/source/PROMPTS.md`): Ludo pictures here, `tools/process_art.py` makes them into textures in
`art/final` and copies them into the mod, at the sizes Tailings' textures are (16 px blocks and items; 32 px vehicle materials).

## Art wave (2026-11-09)
Tim, 2026-11-09: "close all known gaps". Ludo `createImage`, model klimt, art_style `Photorealistic 3D` (as Wildcatter's), 2
variations each, the better kept (the letter after each name).

Blocks, `texture`, each "Seamless flat texture, <straight-on / top-down> orthographic view, even lighting: ..., no text":

| File | Prompt |
|---|---|
| muck (b) | freshly blasted muck in an open pit mine, broken angular grey-brown rock fragments of mixed sizes with dusty fines between them |
| stake_post (a) | rough sawn pale pine wood of a surveyor's grade stake, straight vertical grain, a few small knots and saw marks |
| stake_flag (a) | plain white plastic surveyor's flagging tape, slightly glossy vinyl with soft creases and faint wrinkles (dyed in each stake's colour by `tools/process_art.py`) |
| stamp_mill (b) | front of an old ore stamp mill battery, heavy dark timber frame posts and cross beams, vertical iron stamp stems with tappets, bolted iron straps, rock dust |
| hand_screen (a) | a miner's hand sorting screen, coarse woven steel wire mesh stretched in a worn oak plank frame, a little gravel caught in the mesh |
| shaking_table (a) | deck of a wet concentrating shaking table, pale green linoleum with long thin parallel wooden riffles, a film of water and bands of grey sand and dark heavy mineral |
| magnetic_separator (a) | housing of a drum magnetic separator, steel plate painted blue-grey with rows of rivets and an inspection hatch, worn paint and iron dust |

Vehicle materials (`vehicle/`), `texture`, the same opening; `tools/models/pit_vehicles.py` and `vehicles.py` paint the skins from
them (`tools/models/skin.py`). The loads in the truck, the bucket and the cars use `muck`.

| File | Prompt |
|---|---|
| paint_yellow (a) | worn yellow paint on the steel of a heavy mining machine, scuffs, a film of dust, small chips showing dark primer, plain, no seams |
| rubber_tread (a) | black rubber tread of a giant mining truck tyre, deep chevron tread blocks with grooves between, dusty with dried mud |
| steel_dark (a) | dark grey painted heavy steel chassis plate, grimy with oil stains and dust, a few weld lines, plain |
| steel_worn (b) | worn bare steel wear plate of a loader bucket, grey metal polished and scratched by rock, some light rust in the scratches |
| steel_rust (b) | side plate of an old underground mine car, weathered grey steel with brown-orange rust streaks and patches, dents |

Items, `item-icon` (returned on transparency), each "..., game item icon, plain white background, no text" unless a dark ground is
noted:

| File | Prompt |
|---|---|
| survey_book (b) | A single surveyor's field book: a small yellow waterproof hard cover notebook, closed, with a pencil clipped to it |
| haul_truck (a) | A single yellow 100 tonne rigid mining haul truck seen from the side at three-quarter view, big black tyres, dump body with a canopy over the cab, generic, no brand names |
| wheel_loader (a) | A single large yellow mining wheel loader seen from the side at three-quarter view, raised steel bucket on its arms, big black tyres, generic, no brand names |
| indium (a) | A single small cast ingot of soft bright silvery-white indium metal with a slightly bluish lustre (dark grey background) |
| rhenium (b) | A few small pressed pellets of dull dark grey rhenium metal powder |
| cobalt (a) | A single small piece of electrolytic cobalt metal, a lustrous steel-grey cathode chip with a faint bluish tint |
| cadmium (b) | A single short cast stick of soft silvery-grey cadmium metal, slightly dull |
| rare_earth_oxide (a) | A small heap of fine pale pinkish-beige mixed rare earth oxide powder (dark grey background) |
| copper_sulfate (a) | A small heap of bright blue copper sulfate pentahydrate crystals |
| lime (a) | A small brown paper sack of hydrated lime, top open showing fine white powder (dark grey background) |
| mibc (a) | A single small blue plastic jerrycan of clear liquid flotation frother (MIBC), screw cap, generic |
| xanthate (a) | A small heap of pale yellow sodium xanthate pellets, short cylindrical pellets (the item is potassium amyl xanthate, which looks the same) |
| zinc_sulfate (a) | A small heap of colourless to white zinc sulfate heptahydrate crystals (dark grey background) |

## Open pit P3 and P4 art (2026-11-09)
The same: Ludo `createImage`, model klimt, art_style `Photorealistic 3D`, 2 variations each, the better kept (decisions in
`docs/questions/art-p3.md`).

Blocks, `texture`, the same opening:

| File | Prompt |
|---|---|
| blasting_machine (b) | front panel of a rugged red steel blasting machine box, a key switch in the middle, an amber ARM button and a red FIRE button, small bolted corners, worn paint and dust (Ludo lettered the buttons anyway; at 16 px no letter survives) |
| bench_rock (a) | the hard grey rock of an open pit mine bench, massive fractured grey stone with fine cracks and a little dust, plain (the blasthole's side, with the hole's dark line down it, and its end, with the hole, by `tools/process_art.py`) |
| drill_cuttings (a) | fine grey-brown drill cuttings spread on an open pit bench, a layer of crushed rock chips and powder blown out of a blasthole (top-down; the six collars, each ringed with `stake_flag` dyed in its state's colour round the hole) |
| dry_tails (b) | filtered dry stacked mine tailings, compacted pale grey-beige silty sand filter cake, slightly cracked and crumbly, faint dozer track marks (top-down) |

The blasting machine's stand is `vehicle/steel_dark` at 16 px.

Vehicle materials (`vehicle/`), `texture`, the same opening; the emulsion truck's stripe and reel are this paint dyed red
(`paint_red`, by `tools/process_art.py`):

| File | Prompt |
|---|---|
| paint_white (a) | worn white paint on the steel of a mining explosives truck's tank, scuffs, a film of reddish dust, small chips showing grey primer, plain, no seams |

Items, `item-icon`, each "..., game item icon, plain white background, no text" (Ludo returned all of them on transparency):

| File | Prompt |
|---|---|
| blasthole_drill (a) | A single yellow rotary blasthole drill rig for open pit mining seen from the side at three-quarter view, on crawler tracks, tall lattice drill mast standing upright at its back end, small cab at the front, generic, no brand names |
| emulsion_truck (a) | A single white bulk emulsion explosives truck for mining seen from the side at three-quarter view, white tank body with a red stripe along it, red hose reel at the back, generic, no brand names |
| cast_booster (a) | A single cast explosive booster for blasting: a short pale yellow-tan cylinder in a thin cardboard sleeve, a small well in its top for the detonator, generic, no brand names |
| detonator (b) | A single electronic blasting detonator: a thin copper-coloured metal tube with a coil of bright yellow downline wire attached, generic, no brand names |
| blast_logger (a) | A single rugged handheld blast logger: a yellow rubber-armoured handheld unit with a small dark green screen and a black keypad, front view, generic, no brand names |
| blasting_key (a) | A single brass key for a blasting machine's key switch, a chunky brass key with a round bow and a red plastic tag on its ring, lying flat, generic, no brand names |
| track_dozer (a) | A single large yellow crawler track dozer seen from the side at three-quarter view, wide steel blade at the front, black steel tracks, cab on top, generic, no brand names |
