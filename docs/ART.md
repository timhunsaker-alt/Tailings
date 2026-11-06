# Art

Phase 3 art is in the repository. Phase 1 and phase 2 machines are still vanilla block textures. This page is how the files are laid out and how they were made, so the next pass does not wipe them.

## Where the files are

All runtime art is under `common/src/main/resources/assets/tailings/`.

| Path | What it is |
|---|---|
| `textures/block/*.png` | 16×16 block sprites, including the rail overlays, ballast, signal lamps, and mine light on/off |
| `textures/item/mine_car.png`, `textures/item/mine_locomotive.png` | 16×16 inventory icons |
| `textures/entity/mine_car_placeholder.png`, `textures/entity/mine_locomotive_placeholder.png` | 256×256 entity skins, script-drawn placeholders (tools/models/vehicles.py) |
| `models/block/*.json` | Block models. Slopes and sloped rails are element models (eight slices). Flat rail and the switch are a full cube plus a thin overlay |
| `models/item/mine_car.json`, `models/item/mine_locomotive.json` | `minecraft:item/generated` icons |
| `blockstates/*.json` | Variants. `mine_rail` picks slope, look, and rack. `block_signal` picks the lamp. `mine_light` picks on/off. `mine_switch` picks thrown |
| `items/*.json` | Minecraft 26.3 item definitions (`minecraft:model` pointing at a block or item model) |

Java, not JSON:

| Class | What it is |
|---|---|
| `common/.../client/MineLocomotiveModel.java` | `createBodyLayer()`, 128×128, root 24 pixels above the rail |
| `common/.../client/MineCarModel.java` | Same, with a `load` part the renderer hides when the car is empty |
| `common/.../client/MineTrainRenderer.java` | Both loaders. Cutout body, eyes render type on the headlight only |

Fabric registers the layers in `TailingsFabricClient`. NeoForge registers them on `EntityRenderersEvent.RegisterLayerDefinitions`.

## How this art was made

**Block and item textures** were generated with [Ludo.ai](https://ludo.ai), then saved as PNGs at the paths above. They are 16×16. Opaque machines are RGB. The rail, rack, switch, and item icons are RGBA.

**Entity textures** are the same pipeline at 128×128, painted to the UV layout of the Blockbench models (frame, cab, wheels, headlight, car body, load).

**Entity models** are at their real length since R9: `tools/models/vehicles.py` holds the boxes, paints the 256x256 skins and prints the `createBodyLayer()` lines, not JSON entity models. The cubes are in pixel space. Positive Y in that export is Minecraft's model space (down from a root 24 pixels above the wheels), which is the usual entity-model flip of a Blockbench file whose Y is up. The wheel bottoms sit on the rail. The locomotive's front is −Z, which is the direction of travel after the renderer yaws.

The Blockbench `.bbmodel` files were the authoring source for that export. They are not in the repository. The Java in `MineLocomotiveModel` and `MineCarModel` is what the game loads. If you change a cube, change those methods and keep the texture size at 128×128.

## Cutout overlays

`mine_rail`, `rack_rail`, `mine_switch`, and `mine_switch_thrown` are transparent. Minecraft 26.3 picks the chunk layer from the sprite, not from a per-block render-type registry. `ChunkSectionLayer.byTransparency` uses cutout when a sprite has transparent pixels and no translucent pixels, and translucent when any alpha is strictly between 0 and 255.

The shipped overlays are binary alpha (only 0 and 255), so both loaders bake those quads as cutout. If you regenerate one, threshold alpha below 128 to 0 and the rest to 255 before committing it. A single leftover alpha of 1 makes the whole quad translucent, and the rail will sort like glass instead of sitting on the floor.

The overlay is a thin element just above the bed (about 0.25 pixels). The bed stays solid: mined stone on a slope, `tailings:block/ballast` on flat rail, gravel on the switch. Holes in the overlay show that bed. Replacing the top face with the overlay, instead of stacking it, would show the world through the gaps.

## What is still a vanilla texture

- Stamp mill, hand screen, shaking table, magnetic separator.
- The switch cube under the overlay (`minecraft:block/gravel`).
- Slope, stair, landing, and mine floor, on purpose: they use the mined stone (`minecraft:block/stone`, cobblestone, deepslate, granite, diorite, andesite, tuff, sandstone).
- The mine light **item** (`textures/block/mine_light.png`). The placed block uses `mine_light_on` and `mine_light_off`.
- The old `tunnel_drill` **block**, a legacy item, uses `tunnel_drill.png`. The machine and gantries that replaced it are models from `tools/models/tbm.py`, drawn cell by cell by `TbmRenderer` and `GantryRenderer` while they move.

## Sounds

Rules and which machine makes which: [sound.md](sound.md). Sources are in `art/sound_source/`, made with Ludo `createSoundEffect`, one take each; `tools/process_sounds.py` makes the mono `.ogg`s and `sounds.json` (it needs `soundfile` and `numpy`). Do not edit `sounds.json` by hand.

| File | Loop | Duration | Prompt |
|---|---|---|---|
| jaw_crusher.wav | yes | 6 s | a large jaw rock crusher running, a slow heavy rhythmic steel crunch breaking rock, about two strokes a second, constant, industrial, no music |
| cone_crusher.wav | yes | 6 s | a cone rock crusher running, a deep continuous grinding rumble of rock being squeezed between steel, constant, industrial, no music |
| vibrating_screen.wav | yes | 5 s | a large vibrating double-deck mining screen, fast steel rattle and gravel dancing and sifting over wire mesh, constant, industrial, no music |
| ball_mill.wav | yes | 6 s | a huge ball mill rotating, steel balls and wet ore tumbling and cascading inside a turning steel drum, a deep constant roar with heavy thuds, industrial, no music |
| belt_conveyor.wav | yes | 5 s | a long rubber belt conveyor running with rock on it, rollers humming and squeaking softly, gravel shifting, constant steady rumble, industrial, no music |
| apron_feeder.wav | yes | 5 s | a steel apron pan feeder moving slowly, heavy overlapping steel pans clanking on chain and rollers with rock on top, constant slow rhythmic clank, industrial, no music |
| slurry_rush.wav | yes | 5 s | gritty mineral slurry rushing under pressure through steel hydrocyclones and pipes, a constant wet hissing swirl and rush, industrial, no music |
| agitator.wav | yes | 6 s | large flotation tank agitators turning in thick slurry, low motor drone with churning sloshing liquid and rising air bubbles, constant, industrial, no music |
| pressure_filter.wav | yes | 5 s | an industrial pressure filter running, steady hydraulic pump whine with wet hissing air and water being squeezed out, constant, no music |
| ore_sorter.wav | yes | 5 s | an ore sorting machine, rock clattering down steel chutes with quick sharp puffs of compressed air, constant busy, industrial, no music |
| tbm_cutter.wav | yes | 8 s | a tunnel boring machine cutting hard rock underground, disc cutters grinding and cracking stone in a huge deep continuous roar with a powerful electric motor drone, reverberant, constant, no music |
| mine_locomotive.wav | yes | 5 s | a small electric mine locomotive running on rail, steady traction motor whine and gear hum with soft steel wheels clacking on rail joints, constant, no music, no horn |
| muck_tip.mp3 | no | 3 s | a mine car tipping its load, a heavy rush of broken rock sliding out and crashing down into a steel pocket, ending in a few settling stones |
| muck_load.mp3 | no | 2 s | broken rock pouring off a conveyor into a steel mine car, a short rattling clatter of stones hitting steel |
| ring_set.mp3 | no | 2 s | a heavy concrete tunnel lining segment seated by a hydraulic erector, a short hydraulic hiss then a deep solid thud with a low underground echo |
| car_couple.mp3 | no | 1 s | heavy steel rail car coupling engaging, one sharp loud metallic clank with a short ringing rattle of the link and pin |

## Regenerating

There is no script in the repo that calls Ludo or Blockbench. To replace a texture, export a PNG at the same size, quantize overlay alpha as above, and overwrite the file at the same path. Block models that already reference `tailings:block/<name>` pick the new sprite up. If you add a texture, point a model or blockstate at it. Unused PNGs are not loaded.

Do **not** re-run `tools/phase2_assets.py` (it would write the old cube machines back) or `tools/phase3_assets.py` to refresh art. That script is an early generator. It still writes slab and cube parents with vanilla placeholder textures, and it would overwrite the eight-slice slope and rail models. The committed JSON is the source of truth for block models.

To change the train's shape, edit the boxes in `tools/models/vehicles.py`, run it, and paste what it prints into `createBodyLayer()`. Keep part names the renderer looks up: `headlight` on the locomotive, `load` on the car. Register nothing new unless you add a layer. Both loaders already register `tailings:mine_locomotive` and `tailings:mine_car`, layer `main`.

After a model change, `./gradlew :common:test` runs the spacing tests, and `:neoforge:runGameTestServer` includes the `models` scene, which bakes both layers and checks that the headlight and load parts exist. That does not open a world view. Look at the train in a client before calling the art finished.

## Machine models (R3)

Every true-size part is a real model, not a textured cube:
- the crushing circuit, the flotation bank, the dump station's parts, the traction rectifier and the belt.

How they are made:
- **The tool:** they come from Python scripts in `tools/models/` (`crushing.py`, `flotation.py`, `dump.py`, `rectifier.py`, `belt.py`), built with **Wildcatter's** model tool (`mlib`, `plantlib`), as Wildcatter's own parts are.
- **The dimensions:** each part is drawn in metres from its spec's real dimensions, facing north from its main cell, with its motor box on the west face.
- **Running it:** `python3 tools/models/build.py [module ...]` from the repo root, with Wildcatter checked out beside Tailings (`../wildcatter`). It writes per-piece block models, blockstates, item models and collision shapes into Tailings' assets, and three-view previews into `build/model-previews/`.
- **Textures:** the models use Wildcatter's material textures (`wildcatter:block/mat/...`), which ship in the Wildcatter jar. The driver renames each texture Tailings does not have itself into that namespace. Only materials in the Wildcatter 0.3.0 jar are used.
- **Checks:** `ModelLintTest` (`:common:test`) re-checks the boxes, finds every texture (Tailings' on disk, Wildcatter's in its jar), and checks every part's shape file.

The committed JSON is what the game loads. CI does not run the tool.

