# Tailings — Design

This is what the code on `main` does. The reasons for the recovery numbers are in [VISION.md](VISION.md). Machine specs that were written before the code are in [multiblocks/](multiblocks/). Where a spec and the code disagree, the code wins, and the gap is listed in [ROADMAP.md](ROADMAP.md).

Package root: `dev.wildcatter.tailings`. Mod id: `tailings`. Recovery, rock lists, milling, the plant math, and haulage rules are plain Java. World behavior is in `block`, `entity`, and `client`.

## Recovery ladder

`recovery.RecoveryRoute`. A later route replaces an earlier one. The fractions are not added. No route may pay more mineral than the block holds. `Recovery` enforces that, and the unit tests check it.

| Route | Percent of the block | Played multiple | Where it is paid |
|---|---|---|---|
| Plain mining and smelting | 25% | 1x | Vanilla or Wildcatter. Tailings does not change the drop. One drop is read as 25% |
| Gravity or magnetic concentration | 50% | 2x | Shaking table and magnetic separator, on sand |
| Flotation | 75% | 3x | A full flotation bank, on fines |
| Full recovery | 95% | 4x | Not implemented. 95/25 is 3.8. The even 4x is the written exception. The mass paid will still be 95%, not 100% |

Vanilla iron, copper, and gold hold four ingots in the rock list, so plain smelting is one ingot and the 50% route is two. The flotation bank pays three for copper and gold. Iron does not float. It is the magnetic mineral.

## Rock and the size ladder

`rock.Rock` is every feed. `rock.SizeClass` is `crushed`, `gravel`, `sand`, `fines`. The item id is `tailings:<size>_<rock>`, for example `tailings:sand_granite`. Fines are made only by the ball mill. The stamp mill and the jaw make crushed rock from a whole block. The hand screen, the double-deck screen, and the cone move crushed rock to gravel and gravel to sand. Sand does not become fines on a screen.

Country-rock traces are fixed kilograms, crustal abundance times the same tenfold game factor Wildcatter uses. Granite's zircon is 8 kg, matching the crushing-circuit spec. Ore blocks hold `Mineral.blockAmount()` (density in t/m³ × 1000 × the game grade). Vanilla metals hold 4 ingots.

What each country rock holds (`Rock`'s static list):

| Rock | Minerals (kilograms unless noted) |
|---|---|
| Cobblestone, stone | Iron 0.02, sphalerite 0.4, galena 0.3 |
| Granite | Zircon 8, monazite 0.6, cassiterite 0.675, molybdenite 0.068 |
| Diorite | Iron 0.05, ilmenite 5 |
| Andesite | Copper 0.04, gold 0.004 |
| Deepslate | Iron 0.02, sphalerite 0.4, galena 0.3, pentlandite 5, chromite 30 |
| Tuff | Uraninite 0.5 |
| Sandstone | Zircon 5.75, ilmenite 12.65, rutile 1.15, monazite 0.69, uraninite 0.2 |

Cobble and granite therefore do not make the same sand. Granite sand is a zircon feed. Cobble sand is a trace sulfide and iron feed. The crushing circuit keeps those rocks as separate items the whole way down the ladder, so the dump must not mix them if you want the right concentrate.

`rock.Separator` is only `GRAVITY` or `MAGNETIC`. Phase 1 pays a mineral only when the rock contains it and the machine matches:

- Magnetic separator: iron (vanilla iron ore, and the iron trace in country rock).
- Shaking table: copper, gold, zircon, ilmenite, rutile, monazite, cassiterite, chromite, acanthite, scheelite.

A concentrate that smelts is paid as the ingot (`phaseOneProductId`). Otherwise the machine pays `tailings:<mineral>_concentrate`. Those concentrate items are zircon, ilmenite, rutile, monazite, chromite, and scheelite. Cassiterite pays tin ingots, acanthite pays silver ingots, copper and gold pay their ingots.

Country rock does not emit a whole ingot from a trace. The kilograms go into `NuggetBank`. A nugget is one ninth of a mineral item, and a mineral item is one quarter of the block (the 25% yield). Nine nuggets, shapeless, craft the mineral item. Iron and gold nuggets are vanilla. Every other nugget is `tailings:<mineral>_nugget`.

`Mineral.floats()` is the 75% set: galena, sphalerite, pentlandite, molybdenite, cobaltite, acanthite, platinum, copper, gold. The bank pays three products for an ore or a vanilla metal, and nuggets for country rock.

## The concentrator

`plant.Machine` is one true-size part. 1 block is 1 metre. Watts are the motor. `INDUSTRIAL` is Wildcatter 480 V. `MEDIUM` is 4.16 kV. The ball mill is the only medium-voltage motor. The grizzly is unpowered steel.

Since R3 every part is a Wildcatter big part (`block.TailingsPartBlock` extends Wildcatter's `BigPartBlock`):
- **Placing and breaking:**
  - one item fills its real size, with Wildcatter's ghost outline and "needs W x D x H m of clear space";
  - breaking any piece breaks the whole part and drops the one item, and its contents (R1);
  - a click on any piece is the main cell's.
- **Collision:** read from `assets/tailings/shapes/<id>.json`, which the model tool writes.
- **Motor box:** on the west face of the main cell, turned with the part (`TailingsPartBlock.boxFace`); no other face takes a cable.

**How the crushing circuit runs (R4a, docs/multiblocks/crushing-circuit.md):**
- **No controller.** Each part's block entity runs its own `block.stage.Stage` (`Crushing`, `Grinding`, `Belts`): it runs its motor while it has feed, and works at its share of 50 t/h (`plant.PlantRates`) times its motor's output, only as fast as its product has somewhere to go.
- **Dry rock is kg, not items** (`plant.RockLoad`: kg of each rock and size class). One rock item is one 2.7 t block. Items exist only where the player hands rock in (a bin, the grizzly, any dry part by hand or hopper) or takes a product out (the screen's sand, as items, when no belt is on its sand face).
- **Belts:** a run is the belts of one axis end to end, level or a block up or down. The `conveyor_drive` touching an end is its head; it holds the run's load (30 s of flow) and puts it into what the head end touches: ahead, under, beside, then over. It moves only with a running 7.5 kW drive for each 30 m of run (NO DRIVE). Each belt block draws lumps of the run's main rock (`client.BeltRenderer`).
- **Where product goes:** out of a part's output face onto a belt, or straight into a part touching that face (a chute).
- **The parts:**
  - grizzly: unbroken rock; 30 % passes the bars to a belt (or part) on its east face as crushed rock, the rest drops into the jaw under it (NOT BUILT without one);
  - jaw: unbroken to crushed, out of its south face;
  - screen (3 x the plant rate, for the circulating load): 15 % of crushed and 40 % of gravel pass 6 mm as sand, out of its east face; the oversize out of its south face to the cone;
  - cone (2.5 x): crushed or gravel to gravel, out of its south face back to the screen;
  - ball mill: sand and the cyclone underflow (first), water from a Wildcatter pump; mill discharge at 70 % solids on steel pipe at the south face of its back bottom cells;
  - cyclone cluster: mill discharge in at its north face, water from a pump; at a 250 % circulating load a 3.5th of the solids leave as fines slurry at 35 % solids on its south face, the rest goes back into the mill it touches (NOT BUILT without one);
  - ore bin: 8,192 rock items, fed by hand or hopper, out onto the belt under it.
- **Assays:** the slurry carries Wildcatter's assay (v0.3.0), keyed by each mineral's item id: the whole mineral the rock holds per kg (`plant.Assays`). Crushing liberates and recovers nothing. For the vanilla metals, counted in ingots, an ingot's worth counts as 1 kg.
- **Readouts:** NO FEED, NO WATER, NO DRIVE, NOT BUILT (names the part), FULL, "its output has nowhere to go".

**How flotation runs (R4b, docs/multiblocks/flotation-bank.md):** each part draws its feed off steel pipe at the north face of its front cells into a few seconds' buffer, and puts its product on pipe at the south face of its back cells (`block.stage.Flotation`).
- conditioning tank: fines slurry to conditioned slurry, dosing the reagents per tonne of solids; copper sulfate only when the assay holds sphalerite (NO REAGENT names what is short);
- flotation bank: needs an air blower touching it, running (NO AIR); each floating mineral's 75 % (`RecoveryRoute.FLOTATION`) to concentrate slurry at 30 % solids out of its back cell at x 6, the rest of the feed to tails slurry at x 0;
- concentrate thickener: to thickened concentrate at 60 % solids;
- pressure filter: its cake (`plant.FilterCake`) pays a nugget for each nugget's worth, and nine of a mineral's nuggets are packed into the ladder's item (`Mineral.phaseOneProductId`) in its two output stacks, for a hopper under it or a hand (FULL: take the concentrate);
- tailings pond (placeholder): takes tails slurry off a pipe on any side, up to 3,400 t (FULL).

**Parts (`plant.Machine`):**
- **Crushing:** grizzly (3×2×4), jaw (3×4×4, 110 kW), screen (3×3×6, 22 kW), cone (3×4×3, 132 kW), conveyor drive (1×1×2, 7.5 kW), ball mill (4×4×6, 600 kW), cyclones (2×4×2, 45 kW).
- **Flotation:** conditioner (3×4×3, 15 kW), flotation bank (7×4×13, eight 20 m³ cells, 360 kW), blower (2×2×3, 75 kW), thickener (8×3×8, 5.5 kW), filter (3×3×5, 15 kW), tailings pond placeholder (6×2×6).
- **Dump station:** ramp (1×1×4), pocket (4×3×3), apron feeder (2×1×4, 11 kW), ore sorter (3×3×3, 40 kW), ore bin (3×6×3).
- **Belts:** `belt_conveyor` is one block a metre, laid along the way the player faces.

`plant.Reagent` doses are grams per tonne of a 2.7 t block, and one item is one kilogram. The tank keeps one item for each kilogram, or part of one, still to dose (`ReagentStock.items`) and takes the rest out as they are spent. Every reagent is required for every feed except copper sulfate, which is required only when the rock holds sphalerite.

| Reagent | Dose | Recipe |
|---|---|---|
| `lime` (quicklime) | 500 g/t | Smelt `minecraft:calcite`, 200 ticks |
| `xanthate` | 50 g/t | Shapeless: lime, `wildcatter:sulfur`, sugar → 4 |
| `mibc` | 20 g/t | Shapeless: two sugar → 4 |
| `copper_sulfate` | 300 g/t | Shapeless: copper ingot, `wildcatter:sulfur` → 4 |

**Recipes (placeholders):**
- every powered part: a shaped 3×3 of iron ingots with a copper ingot in the middle;
- the ramp, pocket and bin: iron around a hopper;
- the belt: three leather over two iron, giving 6.

**Old items:** `flotation_cell` and `dump_station` are kept for one release as plain items that craft back into 8 iron ingots.

## The tunnel boring machine

The machine is true-size big parts since R6a (`mine.TbmPart`, `block.TbmPartBlock`; docs/multiblocks/tunnel-boring-machine.md), laid out in the bore from the face back: `cutterhead` (8×8×1), `segment_erector` (8×8×2), `main_beam` (2×2×8, down the middle of the bore, the cab at its tail), and `track_layer` (6×1×3, under the beam's rear). The old 2×2×2 `dewatering_pump` stands against the beam's tail until the pump gantry (R6c). A one-block `cable_junction` takes 4.16 kV cable on any face.

`block.TbmCore` is the cab: its slots, program, stock, muck buffer, progress and advance cycle, apart from where the machine stands. `DrillBlockEntity`, at the main beam's main cell, carries it and is the machine's body as blocks: it checks every part stands where it should (NOT BUILT names what is missing), finds the junction within 16 blocks when it starts, and draws its 1 MW through it. The cable is the reel's 300 m plus a metre for each `cable_mv` item in the cable slot.

An advance has two phases (`TunnelDrill`): `step` probes and cuts the face; then, once the machine has moved one block on with its block entities (without neighbour updates or drops, so no part breaks), `line` places the ring just behind the segment erector (`RING_BEHIND`) and the floor and twin track just behind the track layer (`FLOOR_BEHIND`). If something solid is in the way nothing moves and it stops as blocked; started again, it catches up first. Until the contraption (R6b) it drives flat only: a rigid machine of blocks cannot sit on a 1-in-2 grade.

The bore starts at the cutterhead: slice 0 is the rock ahead of it, and its floor is a block above the cutterhead's bottom. An old one-block `tunnel_drill` cab drops its stock and a main beam item, and goes. Broken lining does not leak.

Geometry (`mine.DriveCourse`): an 8×8 face, a 6×6 clear bore, 28 lining segments per ring (the perimeter of the 8×8, with the middle six floor cells left open), two rails per advance, six floor pieces, three blocks of probe ahead. Stone advances in 400 ticks (20 seconds). Deepslate is 600 ticks.

Heading is the way the machine was built. It drives a 1-in-2 decline (even slices are the upper half, odd slices are the lower half, and a pair drops one block), then flat once it reaches the target Y. The 1-in-1 option is gone (decided with Tim, R6); steep rails already laid still run, and an old program reads as 1 in 2. Floor columns, across the heading: slope, rail, stair or landing, stair or landing, rail, slope. On the flat the walkway is landings. Every track block, decline and flat, sits on gravel ballast (one gravel a block; the rail's `ballast` look). Caves under the floor are filled with cobble.

`SlopeRamp` builds eight boxes, 2 pixels of run each, low at the downhill end. Lower half rises 0 to 8 pixels, upper half 8 to 16, steep rises 2 pixels a slice up to 16. Collision uses the same boxes. The JSON models repeat that shape. They are cuboids, not a smooth mesh.

Lining: if the segment slot holds 28 crafted `lining` items, the ring uses those. Otherwise each cell is the mined stone, or cobble when the rock is not one of the floor stones. The drill holds back enough floor rock for the pieces plus a ring when it is not using segments.

Water: `DrillStock.TANK_MAX` is 256. `acceptWater` fills the tank and returns how much was voided. Lava is checked against the water already in the tank plus the water just pumped, then quench spends water. A water bucket in the bucket slot still adds one to the tank and leaves an empty bucket.

Buffer: when the muck slots cannot take the face, the drill reports that it is paused because the buffer is full. It does not set the stopped latch and it does not clear `running`. The next tick tries again. A car on the track just behind the track layer, columns 2 and 5, takes up to 64 items a tick (`Haul.TRANSFER`), and a train waits there while the machine has muck for it, until the conveyor gantry (R6c) takes over.

Inventory, in order: mine rail, gravel, cobble, lining segments, water bucket, cable, then the muck slots. The screen is `client.DrillScreen`, opened by a click on the main beam. Buttons send menu ids for target Y, flat length, stop conditions and go.

Chunk tickets (`block.ChunkRunner`): while running, the drill refreshes a `tailings:runner` ticket every 20 ticks on three chunks: the cab, the face cell, and the cell `PROBE_BLOCKS` (3) ahead. The ticket type is persistent and times out 200 ticks after its last refresh, so nothing removes it by hand. While a locomotive ticks it refreshes the same ticket on the rail chunk and the chunk one block ahead. 

The parts' recipes are placeholders: eight iron ingots around a copper ingot; the junction is seven. The pump recipe is eight iron ingots around a water bucket.

## Haulage

`haul.Haul` is the numbers. A real Granby car is about 3 t (`REAL_CAR_KG`). That constant is documentation. The game car holds `CAR_STACKS` 9 times `STACK` 64 = 576 items of any mix. `MAX_CARS` is 10. The dump pocket holds one such train (`POCKET_ITEMS`). Each product bin and the waste bin hold 8192. The rectifier is 300 kW (`RECTIFIER_WATTS`), 480 V AC in. The DC side stays inside the block. There is no DC tier in the solver.

Speed: one block every 7 ticks on the flat, every 14 ticks on a grade (`Motion`). About 3 m/s on the flat, half of that on the decline.

### Rail

`mine_rail` is one block, with facing, grade (`flat`, `lower`, `upper`, `steep`), look (the mined stone), and rack. Rack is set on declines. The conductor is part of the block. `Conductor.live` is true for the whole section a rectifier is feeding, not only under the locomotive. Walking it does not shock. `MineLightBlock` and `BlockSignalBlock` refresh from a neighboring fed rail. A lit mine light is level 15. A lit signal is level 12.

`mine_switch` is its own block, not a subclass of the rail (a subclass could not add `thrown` in time for the default state). Thrown, travel leaves clockwise: north turns east, matching `Heading.turn()`. A train claims a block signal's section when it runs into it and leaves it when its last car is out. There is no crafting recipe for the switch or the light.

Rail recipe: shaped, iron and one copper ingot, two rails.

```
I I
ICI
I I
```

`I` iron ingot, `C` copper ingot.

### Locomotive and cars

Since R5a every vehicle is an entity (`entity.MineVehicleEntity`, types `mine_locomotive` and `mine_car`), and trains glide (docs/multiblocks/haulage.md). Nothing of a train is stored on the rails.
- **Placing:** a locomotive or car item on a mine rail or switch puts it there, facing along the rail the way the player looks. A car couples to the train whose next car place is within 3 m of it (`Haul.COUPLE_REACH`); sneak-right-click a car with an empty hand to couple it later. A train has up to 10 cars.
- **The track:** `haul.Track` reads the rails (through `block.RailView`): from a cell, the next is the rail beside it, level or a block up or down, whose surface meets it, so trains run the flat, the 1-in-2 decline and the joins both ways; a thrown switch sends a train from its toe out of its clockwise side, and back out of its toe from the branch. Each cell's centreline runs entry edge, centre, exit edge at the `RailSurface` heights.
- **The train:** the locomotive keeps the train's `haul.TrainPath` (the steps it covers, laid ahead and behind as it runs), its speed, direction and cars by UUID. Each tick it moves along the path and sets every car `Haul.SPACING` (the models' 20 px) behind the one ahead; positions, yaw and pitch sync as entity motion, so the client glides them.
- **Speed** (`haul.Traction`): power over F = m g (sin θ + 0.01), capped at 3 m/s; held to 1 m/s down the rack, slowing for the decline before it reaches it; 0.3 m/s² either way. It stops for: an unfed rail (NO POWER), a grade without rack, a signal section held by another train, a cell another train is on, a car over a full dump pocket, a car with room at the drill's loading point while the drill has muck. At the end of the track it stops, waits 5 s and reverses.
- **Hands:** right-click the locomotive to run or hold it; sneak-right-click to reverse it. Sneak-attack a vehicle with an empty hand to break it into its item and load.
- **Derail:** track broken under the train, or ahead closer than it can stop, derails it: it stops, reads DERAILED, and the cars at the gap drop their loads. It runs again once the track under it is whole.
- **Dump and drill:** a car over a dump pocket beside a ramp tips on the move (`DumpBlockEntity.under`); the drill loads the car standing on the rail behind its cab (`DrillBlockEntity.loadingAt`).
- **Drawing:** `client.MineVehicleRenderer` draws one vehicle per entity, turned and pitched as the track runs; a car's `load` cube shows while it is loaded (synced). The headlight part is submitted again with `RenderTypes.eyes` at full brightness, and the locomotive places a vanilla light block (level 15) in the air above it, moving it when it moves a block.
- **Harm:** a moving vehicle hurts what it hits. `TrainHarm` reads `config/tailings-train.properties` once, when the server starts: `damage=false` turns hits off, `amount=` sets the damage, 8 by default. There is no gamerule.
- **Chunks:** the locomotive refreshes `tailings:runner` on every chunk its train covers and the one ahead, about once a second; they time out on their own once it stops.
- **Old worlds:** a rail saved holding an old train turns it into its entities on its first tick (`RailBlockEntity.migrate`); an old `mine_train` entity discards itself.
- **Tuning:** every rating and proposed figure is in `haul.Haul`.

Locomotive recipe:

```
III
IFI
IRI
```

`F` furnace, `R` mine rail, `I` iron ingot. One locomotive.

Car recipe:

```
I I
ICI
IRI
```

`C` chest, `R` mine rail, `I` iron ingot. One car.

### Dump and signals

The dump station (R4c, docs/multiblocks/dump-station.md):
- **Tipping:** a loaded car passing over a pocket, with a dump ramp beside its rail, tips its whole load into the pocket on the move. The pocket holds one 10-car train (`Haul.POCKET_ITEMS`); a car it cannot take does not tip, and the train waits (`RailBlockEntity.stepOnce` does not move).
- **Apron feeder:** touching the pocket, with the sorter at its tail end (NOT BUILT otherwise); one stack a second while its 11 kW motor runs.
- **Ore sorter:** its screen (`haul.Sorter`) sets each world side, north to west, to one rock or ore item, and one side as waste: a click on a side with the item sets it, a sneaking empty-hand click sets the waste side. Each type goes out of its side, anything unset to waste, into the ore bin touching that side; a full bin stops that type (FULL names it).
- **Ore bin:** 8,192 items, out of its bottom onto the belt under it (R4a).
- An old pocket's sorted slots come back into the pocket when its world loads. The recipes are iron around a hopper.

`block_signal` claims the next 16 rails in the direction it faces as one section. A train enters only when the signal allows it. The recipe is a redstone torch over an iron ingot. The facing face uses `signal_lamp_off` or `signal_lamp_on`. The other faces use `block_signal.png`.

`traction_rectifier` is a 2×3×2 big part, on Wildcatter's DC solver since R5b (docs/multiblocks/haulage.md): a 250 V source behind 4.2 mΩ (2 % at 300 kW), its minus on earth (the running rails), its plus on every rail or switch touching any of its cells. Each rail and switch is a node of conductor rail at 0.02 Ω/km (`RailBlockEntity` is a Wildcatter `CircuitPart`); rails a block up or down the grade are joined by a span. A locomotive draws through the rail under it, a constant-power load sized from the last solve's volts, and its motors give 74 kW times the square of the volts over 250. A rail over half the rating is fed: lights and block signals beside it light, or go dark. The AC side is a Wildcatter `DriveLoad` at 480 V on the west face of its main cell, drawing the DC watts over 97 %. Its DC breaker trips at 1,500 A held for 1 s; a click resets it. `forceRunning` (tests) runs it without a grid. The recipe is iron, redstone, and a copper block in the middle.

```
III
RCR
III
```

## Config

Only one file. It is not created for you.

`config/tailings-train.properties`

| Key | Default if missing | Meaning |
|---|---|---|
| `damage` | `true` | Whether the locomotive hurts living entities in the consist box |
| `amount` | `8` | Damage per hit. A bad number falls back to 8 |

There is no other Tailings config, and no Tailings gamerule.

## Blocks

Blocks with no recipe are placed by the drill, or they exist for creative use only.

| Id | Recipe | Role |
|---|---|---|
| `stamp_mill` | Cobble around an iron ingot and a stick. See below | Hand crusher. Whole block to crushed. Pays nothing |
| `hand_screen` | Sticks and iron. See below | Crushed to gravel, gravel to sand. Pays nothing |
| `shaking_table` | Oak planks and an iron ingot. See below | Gravity, 50%, on sand |
| `magnetic_separator` | Iron and a copper ingot. See below | Magnetic, 50%, on sand |
| `grizzly` | Iron and copper placeholder | Unpowered feed |
| `jaw_crusher` | Iron and copper placeholder | Whole block to crushed. 110 kW |
| `cone_crusher` | Iron and copper placeholder | Crushed to gravel. 132 kW |
| `double_deck_screen` | Iron and copper placeholder | Crushed to gravel, gravel to sand. 22 kW |
| `belt_conveyor` | Iron and copper placeholder | 7.5 kW link in the circuit |
| `ball_mill` | Iron and copper placeholder | Sand to fines. 600 kW at 4.16 kV |
| `cyclone_cluster` | Iron and copper placeholder | 45 kW |
| `conditioning_tank` | Iron and copper placeholder | Flotation controller. 15 kW |
| `flotation_cell` | Iron and copper placeholder | One cell of the fixed bank. 45 kW |
| `air_blower` | Iron and copper placeholder | 75 kW |
| `concentrate_thickener` | Iron and copper placeholder | 5.5 kW |
| `pressure_filter` | Iron and copper placeholder | 15 kW |
| `lining` | 4 gray concrete and 1 iron ingot → 4 | Optional ring. Used only when 28 are stocked |
| `cutterhead`, `segment_erector`, `main_beam`, `track_layer` | 8 iron around a copper ingot (placeholder) | The TBM's parts |
| `cable_junction` | 7 iron around a copper ingot (placeholder) | Its power point |
| `tunnel_drill` | None since R6 | The old one-block cab; it retires itself |
| `dewatering_pump` | 8 iron around a water bucket | The pump multiblock |
| `mine_rail` | Iron and copper, 2 rails | Track, conductor, and rack |
| `traction_rectifier` | Iron, redstone, copper block | 300 kW, feeds joined rail |
| `mine_locomotive` | Item, not a block. See haulage | Spawns the train entity |
| `mine_car` | Item, not a block. See haulage | Marks a rail as a car |
| `dump_station` | 8 iron around a hopper | Pocket, sorter, bins |
| `block_signal` | Redstone torch over an iron ingot | 16-rail section |
| `mine_switch` | None | Clockwise turnout when thrown |
| `mine_light` | None | Level 15 when a fed rail is beside it |
| `slope` | None. The drill cuts it | Lower, upper, or steep, in the mined stone |
| `mine_stair` | None. The drill cuts it | Walkway stair |
| `mine_landing` | None. The drill cuts it | Walkway landing |
| `mine_floor` | None. The drill cuts it | Flat floor stone |

Stamp mill:

```
CIC
CSC
CCC
```

`C` cobblestone, `I` iron ingot, `S` stick.

Hand screen:

```
S S
III
S S
```

`S` stick, `I` iron ingot.

Shaking table:

```
PPP
 I
PPP
```

`P` oak planks, `I` iron ingot.

Magnetic separator:

```
I I
 C
I I
```

`I` iron ingot, `C` copper ingot.

## Items

Sized rock, nuggets, concentrates, and reagents are registered from the enums in `TailingsItems`. They are not a hand-written list.

- **Sized:** four sizes times every `Rock` (8 country rocks, 3 vanilla metals, 27 ores).
- **Nuggets:** one per mineral, except iron and gold, which use vanilla. Nine nuggets, shapeless, make the mineral item: a Wildcatter item, a vanilla ingot (copper), or a Tailings item for zircon, ilmenite, rutile, and monazite.
- **Concentrates:** `tailings:zircon_concentrate`, `ilmenite_concentrate`, `rutile_concentrate`, `monazite_concentrate`, `chromite_concentrate`, `scheelite_concentrate`. No crafting recipe. The 50% machines produce them.
- **Reagents:** `lime`, `xanthate`, `mibc`, `copper_sulfate`. Recipes are in the concentrator section.
- **Haulage items:** `mine_locomotive`, `mine_car`.

Block items exist for every block above, including the ones with no crafting recipe.

## Entities

| Id | Class | Renderer | Role |
|---|---|---|---|
| `tunnel_drill` | `TunnelDrillEntity` | `NoopRenderer` on both loaders | Not spawned since R1; an old one discards itself. Kept registered for the contraption wave |
| `mine_locomotive` | `MineVehicleEntity` | `MineVehicleRenderer` | Drives its train: path, speed, direction, cars |
| `mine_car` | `MineVehicleEntity` | `MineVehicleRenderer` | A car; its load, and whether it is loaded (synced) |
| `mine_train` | `MineTrainEntity` | `NoopRenderer` | The block-stepped train before R5; an old one discards itself |

Vehicles take no damage, except a sneaking empty-hand hit, which breaks them. They deal damage when the config allows it.
