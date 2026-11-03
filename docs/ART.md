# Art

Phase 3 art is in the repository. Phase 1 and phase 2 machines are still vanilla block textures. This page is how the files are laid out and how they were made, so the next pass does not wipe them.

## Where the files are

All runtime art is under `common/src/main/resources/assets/tailings/`.

| Path | What it is |
|---|---|
| `textures/block/*.png` | 16×16 block sprites, including the rail overlays, ballast, signal lamps, and mine light on/off |
| `textures/item/mine_car.png`, `textures/item/mine_locomotive.png` | 16×16 inventory icons |
| `textures/entity/mine_car.png`, `textures/entity/mine_locomotive.png` | 128×128 entity skins |
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

**Entity models** were built by hand in Blockbench (one locomotive, one Granby-style car) and exported as `createBodyLayer()` Java, not as JSON entity models. The cubes are in pixel space. Positive Y in that export is Minecraft's model space (down from a root 24 pixels above the wheels), which is the usual entity-model flip of a Blockbench file whose Y is up. The wheel bottoms sit on the rail. The locomotive's front is −Z, which is the direction of travel after the renderer yaws.

The Blockbench `.bbmodel` files were the authoring source for that export. They are not in the repository. The Java in `MineLocomotiveModel` and `MineCarModel` is what the game loads. If you change a cube, change those methods and keep the texture size at 128×128.

## Cutout overlays

`mine_rail`, `rack_rail`, `mine_switch`, and `mine_switch_thrown` are transparent. Minecraft 26.3 picks the chunk layer from the sprite, not from a per-block render-type registry. `ChunkSectionLayer.byTransparency` uses cutout when a sprite has transparent pixels and no translucent pixels, and translucent when any alpha is strictly between 0 and 255.

The shipped overlays are binary alpha (only 0 and 255), so both loaders bake those quads as cutout. If you regenerate one, threshold alpha below 128 to 0 and the rest to 255 before committing it. A single leftover alpha of 1 makes the whole quad translucent, and the rail will sort like glass instead of sitting on the floor.

The overlay is a thin element just above the bed (about 0.25 pixels). The bed stays solid: mined stone on a slope, `tailings:block/ballast` on flat rail, gravel on the switch. Holes in the overlay show that bed. Replacing the top face with the overlay, instead of stacking it, would show the world through the gaps.

## What is still a vanilla texture

- Stamp mill, hand screen, shaking table, magnetic separator.
- Every crushing and flotation block (grizzly, jaw, cone, screen, belt, ball mill, cyclones, tank, cell, blower, thickener, filter).
- The switch cube under the overlay (`minecraft:block/gravel`).
- Slope, stair, landing, and mine floor, on purpose: they use the mined stone (`minecraft:block/stone`, cobblestone, deepslate, granite, diorite, andesite, tuff, sandstone).
- The mine light **item** (`textures/block/mine_light.png`). The placed block uses `mine_light_on` and `mine_light_off`.
- The drill **entity** has no model. The drill **block** uses `tunnel_drill.png`.

## Regenerating

There is no script in the repo that calls Ludo or Blockbench. To replace a texture, export a PNG at the same size, quantize overlay alpha as above, and overwrite the file at the same path. Block models that already reference `tailings:block/<name>` pick the new sprite up. If you add a texture, point a model or blockstate at it. Unused PNGs are not loaded.

Do **not** re-run `tools/phase3_assets.py` to refresh art. That script is an early generator. It still writes slab and cube parents with vanilla placeholder textures, and it would overwrite the eight-slice slope and rail models. The committed JSON is the source of truth for block models.

To change the train's shape, edit `createBodyLayer()` in the model class (or re-export from Blockbench into that method). Keep part names the renderer looks up: `headlight` on the locomotive, `load` on the car. Register nothing new unless you add a layer. Both loaders already register `tailings:mine_locomotive` and `tailings:mine_car`, layer `main`.

After a model change, `./gradlew :common:test` runs the spacing tests, and `:neoforge:runGameTestServer` includes the `models` scene, which bakes both layers and checks that the headlight and load parts exist. That does not open a world view. Look at the train in a client before calling the art finished.
