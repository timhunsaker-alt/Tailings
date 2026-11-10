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
- **Belts:** a run is the belts of one axis end to end, level or a block up or down. The `conveyor_drive` touching an end is its head; it holds the run's load (30 s of flow) and puts it into what the head end touches: ahead, under, ahead and under, beside, then over. It moves only with a running 7.5 kW drive for each 30 m of run (NO DRIVE).
  - **Transit time (plant-gaps):** rock rides at the belt's 2 m/s (`PlantRates.BELT_METRES_PER_SECOND`) from where it dropped on to the head end (`plant.BeltTravel`), so a 20 m run takes 10 s; the drive delivers only what has arrived ("carrying ... s from its head end"). A run whose head end is blocked stops, as a real belt trips on a full chute.
  - **Drawing:** each belt block draws lumps of the run's main rock for the kg riding on that metre, sliding at 2 m/s while the run moves (`client.BeltRenderer`); a stopped run's lumps stand still.
  - **Into a hopper or chest:** a vanilla container at the head end (under it, ahead, or ahead and under) takes the run's rock as whole items, one item for each 2.7 t of one rock and size: sized items (`tailings:gravel_granite`), or the rock's block for unbroken rock.
- **Feed faces (plant-gaps):** a part takes rock only through its feed face, from a belt's head end or a part's output face (`Stage.feeds`): the grizzly, cone and heap pad over their top; the screen and ball mill over the top of their north (feed) end, the first third of their length. The jaw takes rock only from its grizzly, and an ore bin only from its sorter; any dry part still takes rock by hand or hopper.
- **Where product goes:** out of a part's output face onto a belt, or into a part whose feed face that is (an ore bin over a grizzly's top). A part beside an output face is no chute.
- **The parts:**
  - grizzly: unbroken rock; 30 % passes the bars to a belt (or part) on its east face as crushed rock, the rest drops into the jaw under it (NOT BUILT without one);
  - jaw: unbroken to crushed, out of its south face;
  - screen (3 x the plant rate, for the circulating load): 15 % of crushed and 40 % of gravel pass 6 mm as sand, out of its east face; the oversize out of its south face to the cone. **Gravel face:** with a belt on the west face of its south (discharge) end, the 6-40 mm between the decks goes out there as aggregate instead of round to the cone: 35 % of crushed rock (proposed) and all of the cone's gravel that is not sand. What that belt has no room for goes round to the cone;
  - cone (2.5 x): crushed or gravel to gravel, out of its south face back to the screen;
  - ball mill: sand and the cyclone underflow (first), water from a Wildcatter pump; mill discharge at 70 % solids on steel pipe at the south face of its back bottom cells;
  - cyclone cluster: mill discharge in at its north face, water from a pump; at a 250 % circulating load a 3.5th of the solids leave as fines slurry at 35 % solids on its south face, the rest goes back into the mill it touches (NOT BUILT without one);
  - ore bin: 8,192 rock items, fed by hand or hopper, out onto the belt under it.
- **Assays:** the slurry carries Wildcatter's assay (v0.3.0), keyed by each mineral's item id: the kg of the whole mineral the rock holds per kg (`plant.Assays`). Crushing liberates and recovers nothing. The vanilla metals are counted in kg too (plant-gaps): an ingot is 100 kg of metal (`Mineral.INGOT_KG`, as Wildcatter counts an iron ingot), so copper ore's four ingots are 400 kg in its 2.7 t; the filter and the cells divide by it to pay ingots and nuggets (`FilterCake.addKg`).
- **Readouts:** NO FEED, NO WATER, NO DRIVE, NOT BUILT (names the part), FULL, "its output has nowhere to go".

**How flotation runs (R4b, docs/multiblocks/flotation-bank.md):** each part draws its feed off steel pipe at the north face of its front cells into a few seconds' buffer, and puts its product on pipe at the south face of its back cells (`block.stage.Flotation`).
- conditioning tank: fines slurry to conditioned slurry, dosing the reagents per tonne of solids; copper sulfate only when the assay holds sphalerite, or zinc sulfate in its place when set SELECTIVE (NO REAGENT names what is short);
- activation tank: a selective bank's tails to conditioned slurry, freeing the held-down sphalerite for a second bank (`plant.Selective`, selective-flotation.md);
- flotation bank: needs an air blower touching it, running (NO AIR); each floating mineral's 75 % (`RecoveryRoute.FLOTATION`) to concentrate slurry at 30 % solids out of its back cell at x 6, the rest of the feed to tails slurry at x 0;
- concentrate thickener: to thickened concentrate at 60 % solids;
- pressure filter: its cake (`plant.FilterCake`) pays a nugget for each nugget's worth, and nine of a mineral's nuggets are packed into the ladder's item (`Mineral.phaseOneProductId`) in its two output stacks, for a hopper under it or a hand (FULL: take the concentrate). **Bulk cake (plant-gaps):** the sphalerite a bank floats travels as `tailings:floated_sphalerite`; a filter whose feed holds it and galena together (lead and zinc concentrate piped into one thickener or filter) makes a bulk cake that pays the lead and loses that zinc to slag, as a bank fed both does, and reads "filtering a BULK cake". The scavengers' sphalerite is not a bank's and pays as before;
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

The junction is also the reel gantry's transformer: three windings from the medium-voltage side to the industrial-voltage side, wye to wye with both neutrals grounded, the ratio from the world's standard, 150 kVA at 4 % impedance (proposed), and the pump's and conveyor's 82.5 kW a constant-power star at power factor 0.85 on its secondary. It is built at the junction because a circuit part is a block and the gantry moves. The conveyor moves no muck while the secondary is under 80 % of its volts.

Started, the machine lifts (R6b): `entity.TbmEntity` takes the `TbmCore` (saved and loaded across), the parts' cells (`block.TbmMachine`, the one layout the check, the lift and the renderer share) become air, quietly, and `client.TbmRenderer` draws each cell's own model, pitched about the cutterhead, the way Wildcatter's cranes draw a part in flight. The entity's position is a function of the core: the cutterhead's cell from the origin, along the bore by the glide's progress, on `TunnelDrill.floorExact`. Stopped on the flat with its cells free it sets them back down and hands the core to the new cab; on the decline it stays and its cab still works. `block.TbmCab` is the screen's view of either. Tests and films set `TbmCore.testTicks` rather than wait 20 s a block.

The back-up (R6c) is `entity.GantryEntity`, one class for three types, drawn from the states of `block.GantryPartBlock`, which has no item and is never placed; `client.PitchedCellsRenderer` draws gantries and machine alike. `block.TbmBackup` holds where each gantry belongs (`mine.TbmGantry` rows behind the cutterhead), what is wrong behind the machine, and tows them there each tick. Each gantry runs on its own wheels (`entity.GantryWheels`, a `haul.TrainPath` along each of its two rails, saved with it): the machine says how far along the bore its front is to stand, and the rails set where it stands, its height (a block under the rail's surface at its front edge, as on the flat) and its pitch. A rail gone or changed under it, or missing ahead or behind, takes it off the track: the machine reads "NOT BUILT: needs the track under the ..." and does not cut, and the gantry goes back on by itself when the rail is back. The pump gantry carries the quench tank's water (`water`, saved on it): the cab takes it each tick (`TbmBackup.loadTank`) and hands it back (`storeTank`); no pump gantry, no tank; a pump gantry broken takes its water with it. The reel gantry is told the junction and the metres paid out, and `client.GantryRenderer` draws the trailing cable a metre at a time along the floor back to the junction and turns bars on the reel's flanges. The cab's screen follows the machine across the lift and set-down (`DrillMenu.rebind`). The core keeps their UUIDs. Standing gantries mark the track columns they cover, and a train treats the nearest as the end of the way.

An advance has two phases (`TunnelDrill`): `step` probes and cuts the face; then, once the machine has glided one block on, `line` places the ring just behind the segment erector (`RING_BEHIND`) and the floor and twin track just behind the track layer (`FLOOR_BEHIND`). If something solid is in the way nothing moves and it stops as blocked; started again, it catches up first. Until the contraption (R6b) it drives flat only: a rigid machine of blocks cannot sit on a 1-in-2 grade.

The bore starts at the cutterhead: slice 0 is the rock ahead of it, and its floor is a block above the cutterhead's bottom. An old one-block `tunnel_drill` cab drops its stock and a main beam item, and goes. Broken lining does not leak.

Geometry (`mine.DriveCourse`): an 8×8 face, a 6×6 clear bore, 28 lining segments per ring (the perimeter of the 8×8, with the middle six floor cells left open), two rails per advance, six floor pieces, three blocks of probe ahead. Stone advances in 400 ticks (20 seconds). Deepslate is 600 ticks.

Heading is the way the machine was built. It drives a 1-in-2 decline (even slices are the upper half, odd slices are the lower half, and a pair drops one block), then flat once it reaches the target Y. The 1-in-1 option is gone (decided with Tim, R6); steep rails already laid still run, and an old program reads as 1 in 2. Floor columns, across the heading: slope, rail, stair or landing, stair or landing, rail, slope. On the flat the walkway is landings. Every track block, decline and flat, sits on gravel ballast (one gravel a block; the rail's `ballast` look). Caves under the floor are filled with cobble.

`SlopeRamp` builds eight boxes, 2 pixels of run each, low at the downhill end. Lower half rises 0 to 8 pixels, upper half 8 to 16, steep rises 2 pixels a slice up to 16. Collision uses the same boxes. The JSON models repeat that shape. They are cuboids, not a smooth mesh.

Lining: if the segment slot holds 28 crafted `lining` items, the ring uses those. Otherwise each cell is the mined stone, or cobble when the rock is not one of the floor stones. The drill holds back enough floor rock for the pieces plus a ring when it is not using segments.

Water: `DrillStock.TANK_MAX` is 256. `acceptWater` fills the tank and returns how much was voided. Lava is checked against the water already in the tank plus the water just pumped, then quench spends water. A water bucket in the bucket slot still adds one to the tank (on the pump gantry since the TBM gaps) and leaves an empty bucket; with no pump gantry it stays in the slot.

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
- **Speed** (`haul.Traction`): power over F = m g (sin θ + 0.01), capped at 3 m/s; held to 1 m/s down the rack, slowing for the decline before it reaches it; 0.3 m/s² either way. It stops for: an unfed rail (NO POWER), a grade without rack, a signal section held by another train, a cell another train is on, a car over a full dump pocket, a car with room at the drill's loading point while the drill has muck. The loading point reaches 29 rows behind the conveyor's tail, a whole 10-car train, so a train coming up stops with its first car in reach, fills it, moves up, and so on: every car loads. At the end of the track it stops, waits 5 s and reverses.
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
- **Tipping:** a loaded car passing over a pocket, with a dump ramp beside its rail, tips its whole load into the pocket on the move. The pocket holds one 10-car train (`Haul.POCKET_ITEMS`); a car it cannot take does not tip.
- **Where the train waits (plant-gaps):** a loaded train waits at the block signal whose section holds the dump (`SignalBlockEntity.dumpPocket`, found by following the section's rails) while the pocket cannot take the whole train's load or its grizzly is choked: "waiting at the signal before the dump". With no signal before the dump it waits with the car over the pocket, as before.
- **Oversize on the grizzly (plant-gaps):** one item in a hundred a car tips (`Haul.OVERSIZE_SHARE`, proposed) is over the pocket's 300 mm bars and stays on them, from the load's commonest rock. 32 pieces (`Haul.GRIZZLY_CHOKE`) choke the grizzly: no car tips until it is cleared. A click with a pickaxe breaks one piece through into the pocket (a rock breaker's work, a point of the pickaxe's wear); a sneaking empty-hand click lifts them all off to the player. The Power Meter or a click reads how many are on the bars.
- **Apron feeder:** touching the pocket, with the sorter at its tail end (NOT BUILT otherwise); one stack a second while its 11 kW motor runs.
- **Ore sorter:** its screen (`block.SorterMenu`, `client.SorterScreen`, plant-gaps), opened with an empty hand: a slot for each world side, north to west, round the sorter; a rock or ore item put on a slot sets that side to it (the item stays in hand), an empty hand clears it, a side's W button makes it the waste side, and a shift-click on a rock in the inventory sets the first free side. A click on a side with a rock item still sets it straight away. Each type goes out of its side, anything unset to waste, into the ore bin touching that side; a full bin stops that type (FULL names it).
- **The waste bin backs up (R10):** a bin holds 8,192 items. Its way out is a belt under it: to a heap pad (over its top; the heap holds 100 blocks and leaches at a tenth of the plant's rate), or to hoppers and chests at the belt's head end, which take the rock as items, or, in a later wave, to a waste dump (open pit P4). The playthrough runs 11.5 game minutes and fills about an eighth of it, so it builds none of these.
- **Ore bin:** 8,192 items, out of its bottom onto the belt under it (R4a).
- An old pocket's sorted slots come back into the pocket when its world loads. The recipes are iron around a hopper.

`block_signal` claims the next 16 rails in the direction it faces as one section. A train enters only when the signal allows it. The recipe is a redstone torch over an iron ingot. The facing face uses `signal_lamp_off` or `signal_lamp_on`. The other faces use `block_signal.png`.

`traction_rectifier` is a 2×3×2 big part, on Wildcatter's DC solver since R5b (docs/multiblocks/haulage.md): a 250 V source behind 4.2 mΩ (2 % at 300 kW), its minus on earth (the running rails), its plus on every rail or switch touching any of its cells. Each rail and switch is a node of conductor rail at 0.02 Ω/km (`RailBlockEntity` is a Wildcatter `CircuitPart`); rails a block up or down the grade are joined by a span. A locomotive draws through the rail under it, a constant-power load sized from the last solve's volts, and its motors give 74 kW times the square of the volts over 250. A rail over half the rating is fed: lights and block signals beside it light, or go dark. The AC side is a Wildcatter `DriveLoad` at 480 V on the west face of its main cell, drawing the DC watts over 97 %. Its DC breaker trips at 1,500 A held for 1 s; a click resets it. A circuit of more than Wildcatter's 4,000 nodes (`Tuning.MAX_CIRCUIT_NODES`; a node a metre of track) is not solved: rails and the rectifier in it read `tooBig()`, the track is dead, the rectifier's readout and a stopped train say "TRACK TOO LONG", and players within 64 m are told once. `forceRunning` (tests) runs it without a grid. The recipe is iron, redstone, and a copper block in the middle.

```
III
RCR
III
```

## PLC signals (Wildcatter 13i)

Wildcatter's PLCs (its `docs/multiblocks/plc.md`) reach Tailings by its multi-core control cable. Every device below has a keyed
HAND / OFF / AUTO selector (a click with Wildcatter's Selector Key): HAND works as it always has, OFF holds it, and AUTO does what the
PLC says while the PLC keeps saying it. A command not refreshed for Wildcatter's stale time counts as off, so a cut cable holds the
device. A PLC never overrides protection: a stalled motor, a tripped breaker and a full bin are what they were.
- **A plant part with a motor** (`PlantBlockEntity`): RUN in; RUNNING, STALLED and AUTO out; AMPS (4-20 mA over 150 % of full load)
  and FILL (4-20 mA over a belt's hold) as analog inputs. A conveyor drive's FILL is a belt scale. **The ore sorter** adds SHUT NORTH,
  EAST, SOUTH and WEST GATE: a shut gate sends that side's rock to waste. Parts with no motor have no signals; the belts themselves
  run on their drive.
- **The sorter's routing** (`plant.PlcScale`, `haul.Sorter.choose`): NORTH, EAST, SOUTH and WEST TAKES, one analog output a side
  carrying a rock code, code *n* at 4 + *n*/4 mA. 0 takes nothing of its own; 1 to 38 are `Rock`'s list in order: cobblestone,
  stone, granite, diorite, andesite, deepslate, tuff, sandstone (1-8), iron, copper and gold ore (9-11), then Wildcatter's ores:
  uraninite 12, pitchblende 13, sulfur 14, halite 15, sylvite 16, fluorite 17, heavy mineral sand 18, bauxite 19, galena 20,
  sphalerite 21, cassiterite 22, pentlandite 23, chromite 24, platinum 25, molybdenite 26, magnesite 27, dolomite 28, graphite 29,
  borax 30, spodumene 31, barite 32, bentonite 33, silver (acanthite) 34, scheelite 35, cobaltite 36, pyrolusite 37, phosphate 38.
  New rocks join at the end. In AUTO on a fresh loop a side takes its code's rock (its block item) over the screen's setting, which
  is kept and comes back when the PLC stops, in HAND or OFF, or on a code with no rock. The waste side stays the waste side; two sides
  on one rock, the first takes it. Gates and codes are refreshed every tick, so a quiet PLC opens its gates too.
- **A slurry part** (`block.stage.SlurryGauge`; the mill, cyclones, spiral bank, the tanks, banks, thickeners, filter, leach tank,
  electrowinning bank and heap pad) adds LEVEL, FLOW and DENSITY, 4-20 mA inputs. LEVEL is its feed tank (the inlet buffer it draws
  into off the pipe; the mill, cyclones and heap pad read their discharge sump) over the hold it works to, so a part backed up by a
  full outlet reads full. FLOW is what leaves it by pipe over the last second, 0-60 L/s. DENSITY is its feed tank's, 1.0-2.0 kg/L,
  from each product's solids at 2.7 t/m3: mill discharge 70 % (1.79), fines, conditioned, tails and leach slurry 35 % (1.28),
  concentrate 30 % (1.23), thickened concentrate 60 % (1.61), pregnant liquor as water. A transmitter clamped to it reads the same.
- **`mine_switch`:** THROW in; THROWN and AUTO out. In HAND a switch is thrown by hand (a click with an empty hand). A train's path
  through a switch that is thrown under it is broken, so it stops.
- **`block_signal`:** HOLD in, keeping the section closed even when it is clear; OCCUPIED and AUTO out.
- **`cable_junction`:** the TBM's fixed point, as the machine moves. GO in (a rising edge starts the machine as its Go button does);
  RUNNING and AUTO out; ADVANCE PULSES (one a block advanced) on a counter; CABLE LEFT (4-20 mA over 400 m) and QUENCH TANK as analog
  inputs. The machine reports to the junction its cable reaches. **Its course:** TARGET Y (-64 at 4 mA to 320 at 20) and FLAT LENGTH
  (0-400 m) as analog outputs, TARGET Y SET and FLAT LENGTH SET read back. The course takes only while the machine stands (a course
  and a GO in one scan: the course first); while it drives, the course it started on holds. A target that would move floor already
  cut is held (`PlcScale.allowedTarget`): any before the first advance, no higher than its floor while it declines, unchanged once
  it has levelled out. Heading and grade are not signals: it bores the way it was built, at 1 in 2.
- **Pull cords** are Wildcatter's `pull_cord`, wired to a PLC input like any button.

## The open pit (P1)

Spec: [multiblocks/open-pit.md](multiblocks/open-pit.md).

- **Loose ground:** `mine.Settle` is the settle rule in plain Java (unit-tested in `SettleTest`): a let-go block falls, then rolls
  toward a side where resting would leave a step of 2 or more, or 4 up in a run of 4 columns. Piles stand at 37 degrees (36 in the
  test). `pit.Loose` places muck, soil and tipped loads in the world by it, at once, with no falling entities.
- **Masses** (`pit.PitMass`): rock and ore 2.7 t a block; dirt and sand 1.6 t, gravel 1.9, clay 2.0, mud 1.8. A grass block digs as
  dirt. Soil is dirt, coarse and rooted dirt, sand, red sand, gravel, clay and mud: it goes to dumps, never to the plant.
- **Muck** (`block.MuckBlock`, `MuckBlockEntity`): holds quarters of a bank block of each item it was; a full block holds 3. It
  settles again when its ground goes. Broken by hand it drops a quarter at a time by chance; it has no loot table.
- **Driving** (`entity.PitVehicleEntity`, `pit.PitDrive`): the rider's keys come from the server's copy of their input
  (`ServerPlayer.getLastClientInput`), so the vehicle moves on the server and the client follows it. Forward and back drive or brake
  by the engine's power, left and right turn on its radius while it moves, jump works its body or bucket, sneak gets out. It steps
  up 1 block. Its box is its real length and width, turned with it; it will not turn into a wall.
- **The truck** (`HaulTruckEntity`): standing, jump raises the body over 13 s. At the top its load goes into a pit hopper under its
  tail (`DumpBlockEntity.tip`: whole bank blocks, all or none; soil refused; FULL) or out 1.5 m behind it by `Loose.tip` (soil as its
  own blocks, rock as muck at 4 quarters a bank block). On the ground a load goes out to the nearest whole block (soil) or quarter (rock) and the rest spills; the
  hopper keeps what is under a whole block of each ore and makes it up with the next load, so the truck comes away empty.
- **The loader** (`WheelLoaderEntity`): jump with the bucket empty digs up to 20.4 t from the face in front of the bucket (5 wide,
  6 high from its own floor, 3 deep, top down), soft ground or muck; a pass is 25 s. Jump with the bucket full dumps it into the
  nearest truck within 4 m of the bucket's lip; 15 s. Standing rock reads "hard rock: blast it first".
- **The pit hopper** is a `DumpBlockEntity` (as the rail dump pocket) with its own capacity, so the apron feeder, sorter and ore
  bins downstream work unchanged.
- **Sound:** the vehicles use Wildcatter's `diesel_idle` and `diesel_load` loops while their engines run; tipping and loading use
  `tailings:muck_tip` and `muck_load`.

## The open pit (P2a): survey, self-driving trucks, fuel

Spec: [multiblocks/open-pit.md](multiblocks/open-pit.md), "P2a as planned".

- **Survey** (`pit.Survey`, plain Java, unit-tested in `PitP2aTest`): a leg between two stakes is steep over 10 %, blocked where a
  solid block is in a 6.2 x 5.2 m truck's box along it, and one-way where 3 widths (18.6 m) are not clear. `block.SurveyStakeBlock`
  holds its stop kind (`pit.StopKind`) and facing in its block state and has no collision. `item.SurveyBookItem` keeps the route as
  block positions and the legs' problems in its custom data.
- **The autopilot** (`pit.Autopilot`, plain Java; `entity.TruckRoute` in the world): a truck with a route drives itself when nobody
  sits in it, through the same `PitDrive` physics a player's keys use (`PitVehicleEntity.autopilot` returns throttle, steer and
  jump). It steers for a point 15 m ahead on its lane (4 m right of the stake line on two-way legs), slows to 15 km/h for turns over
  30 degrees, stops for any player, mob or machine within its stopping distance plus 5 m along its path (as far as the next stake),
  keeps 50 m from a truck ahead going its way, and waits 40 m short of a one-way leg with a truck coming along it. At the stake
  before an end it turns round until its nose points away from the end, then backs in along the line, at a DUMP POCKET on to the
  bumper. Out and back between the ends; a FUEL stake when it is under 15 %.
- **The self-running loader** (`WheelLoaderEntity.setAuto`): at a LOAD stake within 16 m it digs whenever its bucket is empty and
  dumps into a stopped truck in reach.
- **Diesel** (`pit.PitFuel`): litres = work / (36 MJ/L x 0.38). Driving, the engine's output is the power at the wheels over 0.8,
  between 3 % idle and rated; the truck's hoist adds 30 %, a loader's pass 60 %. The engine is off with nobody in it, no route and no
  work. Tanks 1,100 L and 1,500 L; a new machine comes with 10 %. Dry, it will not drive, tip or dig.
- **The fuel bay** (`block.FuelBayBlockEntity`): a 2,000 L day tank filled by Wildcatter pipe (`Lines.pull`, diesel) on any face;
  it fills the nearest stopped machine within 8 m at 10 L/s.

## The open pit (P2b): the haul dispatch office

Spec: [multiblocks/open-pit.md](multiblocks/open-pit.md), "Haul roads, surveying and dispatch"; decisions taken while Tim was away are
in [questions/p2b-dispatch.md](questions/p2b-dispatch.md).

- **The office** (`haul_dispatch`, 3 x 3 x 3, `block.HaulDispatchBlockEntity`): a Wildcatter `ControlDevice` on the control cable, and
  a 5 kW single-phase load on Wildcatter's low tier (whatever the world's standard makes it), landed on its electrical box on the
  west face of its main cell. Under 85 % of the tier's nominal its radio is dead.
- **Routes:** the survey book clicked on the office adds the book's route as the next free route of 8; sneak-clicked, it takes that
  route off. A truck is on a route when its stakes are the same, either way along them. Once a second each truck on a route finds the
  office (`HaulDispatchBlockEntity.find`), reports whether it works, is under 15 % fuel or BLOCKED, and reads whether to run. A
  route no office has runs as in P2a.
- **Points** (`pit.Dispatch`, 43): for each route RUN (out), RUNNING, TRUCKS (4-20 mA over 0-16 trucks, 1 mA a truck), LOADS (a pulse
  counter, one a tipped load) and TONNES (4-20 mA over 0-2,000 t tipped in the last hour, 72,000 ticks); then FUEL LOW, BLOCKED and
  AUTO. With no power the loops read 0 mA, a failed loop.
- **The selector:** HAND runs every route, as with no office; OFF parks them all; AUTO runs a route while the PLC keeps writing its
  RUN, and parks it when RUN goes off or stops coming (Wildcatter's stale time: a stopped PLC or a cut cable).
- **Parking** (`entity.TruckRoute`): told to stop, a truck drives on to its route's PARK stake and stops there with its engine off
  (no diesel burnt); a route with no PARK stake stops at the next stake. A truck filling at LOAD leaves at once; one turning, backing
  or tipping at an end finishes first. A truck out of the office's radio (over 2 km, or the office unpowered) stops at its next stake
  and reads "no word from dispatch" until the radio comes back. RUN on, it goes on from the stake it parked at.
- **Chunks:** an office with routes keeps its own chunk working with the `tailings:runner` ticket; a parked truck keeps its chunks as
  a truck on a route does, so RUN reaches it.
- **Readouts:** right-click the office with an empty hand for its selector, power and each route's RUN or PARK, trucks, loads and
  tonnes in the last hour, and the fleet's alarms.

## The open pit (P3): drilling and blasting

Spec: [multiblocks/open-pit-p3-blasting.md](multiblocks/open-pit-p3-blasting.md) (a draft decided on recommendation); decisions taken
while building are in [questions/p3-blasting.md](questions/p3-blasting.md). The numbers are in `pit.Blasting` (unit-tested in
`BlastingTest`), the pattern in `pit.DrillPattern`, the shot in `pit.Shot`.

- **The bench and the hole:** a 14 m bench, 2 m subdrill, 16 m hole (one 16.8 m pass), 7 x 8 m staggered, 4 m stemming over a 12 m
  column: 69 kg a metre at 1.2 g/cm³, 828 kg, 0.39 kg/t of its 2,117 t box. Every one is a panel setting within the spec's limits.
- **The drill** (`entity.BlastholeDrillEntity`, `BlastholeDrillModel`): one machine at its real size (7 x 15 m, mast to 26 m), its
  hole under the mast 1.5 m in from its back end. Its panel (sneak-right-click) sets rows x columns, burden, spacing, bench, subdrill
  and staggered; GO squares it to the grid and walks the pattern in a serpentine: turn on the spot toward the next hole's stand, tram
  at 1.5 km/h, turn back square, 20 s on its jacks, drill 1 m every 10 s (15 s in deepslate). It stops and says why: DONE, OUT OF
  FUEL, BLOCKED, NOT LEVEL (over 1 block out under the crawlers), "not on broken ground" (muck at the collar). Driven by hand, jump
  drills one hole where it stands. Diesel by work: 70 % of 600 kW drilling, 40 % tramming, 1,500 L tank; it fuels at a fuel bay.
- **A hole** (`block.BlastholeBlock`, `BlastholeBlockEntity`): a column of `blasthole` blocks from the collar down, each remembering
  the block it replaced; the collar holds the hole: depth (SHORT where it met a cave, fluid, bedrock or a block entity), its row and
  place in the pattern, the pattern's free face, its cuttings (0.75 of its depth), booster, detonator ID, charge and when it was
  charged, stemming, delay, and a misfire. Its state (DRILLED, PRIMED, CHARGED, STEMMED, LOGGED, MISFIRED) is a block state, drawn as
  a coloured ring on the collar. On the collar: a booster or detonator primes it (before charging); an empty hand stems it with its
  cuttings, gravel 13 m a block; a water bucket washes it out (not once stemmed, unless it misfired), giving back the booster and
  detonator; sneak reads it. Mined by hand it drops the rock it replaced.
- **The emulsion truck** (`entity.EmulsionTruckEntity`, `EmulsionTruckModel`): 12 t of matrix, 8.5 x 2.5 x 3.8 m, 300 kW, 400 L. The
  matrix fill point (`matrix_fill_point`, 1 x 2 x 1, `block.MatrixFillPointBlockEntity`) draws Wildcatter's `EMULSION_MATRIX` by pipe on
  any face (a stock tank or IBC) and fills a stopped truck within 6 m at 1 t/min. Jump (or Charge on its panel), standing, charges every
  primed, uncharged hole within 12 m of its hose reel, nearest first, at 20 kg/s, to the hole's depth less the stemming set on its panel;
  unprimed holes are skipped and counted (NOT PRIMED). The pump runs the engine at 10 %; it reads EMPTY, NO HOLES IN REACH.
- **Boosters and detonators:** `cast_booster` (1 TNT makes 4) and `detonator` (TNT, copper, redstone make 8): items that do nothing
  on their own. **The logger** (`item.BlastLoggerItem`) logs a primed or charged hole: delay = row x row ms + hole x hole ms (65 and
  25, set on its panel), up to 200 holes, kept on the item.
- **The blasting machine** (`blasting_machine`, a block, `block.BlastingMachineBlockEntity`): the logger loads the shot (every hole
  50-500 m off, else TOO CLOSE or TOO FAR); the `blasting_key` goes in; an empty hand ARMs (it tests the shot: holes that answer, kg,
  tonnes, powder factor, what will not fire and what will blow out; armed for 30 s), then FIREs: a player in the danger zone asks a
  second FIRE; a 10 s siren at the shot; then the shot. With misfires it reads WAIT for 30 s. A machine, block entity or Tailings or
  Wildcatter vehicle in a hole's box stops ARM and FIRE. A `ControlDevice`: ARM and FIRE (rising edges, key in), ARMED, FIRED (pulses),
  HOLES (4-20 mA over 200), MISFIRES (pulses). It keeps the shot's chunks working while it fires.
- **The shot** (`pit.Shot`): its holes fire in delay order, a tick each 50 ms. A hole with no detonator, no booster, or under
  0.15 kg/t (its charge counting half until it has gassed 60 s) misfires, keeping its charge and standing. A hole breaks its box:
  burden deep toward its free face (its pattern's face if open ground stands half the bench high within burden + 2 m; else the nearest
  other side; else a box cut centred on it), spacing wide, collar to bench floor. Rock (a block a pickaxe digs, with a collision box,
  no block entity, not soft ground, not unbreakable) becomes muck holding 3 quarters; the 4th quarter of each goes to the swell, let go
  3 quarters a block by the settle rule from the face side's top (or over the shot in a box cut); at the end what is left goes as a part
  block, so every quarter is kept. The hole's own column below the floor is rock again. Stemming under 2 m blows out: the lower half of
  its box, and 6 flyrock; a front-row burden under half throws 3 forward, an overcharge (over 1.0 kg/t) 3. Flyrock is a quarter of
  muck landing 40-100 m off after its flight on a 45° arc, breaking glass and leaves, 10 damage where it lands. Players and mobs
  within 50 m of the nearest firing hole take 30 x (1 - r/50) explosion damage. No vanilla explosion, no drops. Dust (campfire smoke)
  over the shot for 20 s; one explosion sound.
- **The loader** stops before a pass that would take a misfired hole's block: "misfire in the muck: stop".
- **Sounds:** the drill's rotary head is `tailings:tbm_cutter`; the truck's pump Wildcatter's `motor_hum`; the siren vanilla's raid
  horn; the blast vanilla's explosion (decisions 15).

## The open pit (P4): dry stack, the load-out, dumps, berms and the dozer

Spec: [multiblocks/open-pit-p4-dry-stack.md](multiblocks/open-pit-p4-dry-stack.md) (draft, decided on recommendation); the choices made
building it are in [questions/p4-dry-stack.md](questions/p4-dry-stack.md). The figures are in `pit.DryStack` (unit-tested).

- **The tailings thickener** (`tailings_thickener`, 15 x 5 x 15, 11 kW, `block.stage.DryStackStages.THICKENER`): tails slurry in at
  the north face of its front cells; thickened tails out at the south face of its back cells at 60 % solids, on the pipe as tails
  slurry whose assay carries its solids (`tailings:solids`, Wildcatter 0.5.1 has no product for them); about 1.2 L of overflow water a
  kg of solids, sent once a second to the water users on a water pipe at its east face (lost with none, as built mills do). It holds
  20 t of underflow for the press.
- **The tailings filter** (`tailings_filter`, 6 x 6 x 24, 150 kW): a batch press on thickened tails (plain tails read NOT THICKENED).
  Each 15-minute cycle it fills its chambers to 15 t of solids over the first 7.5 minutes (filling, squeezing, blowing), then opens
  and drops the cake (a thud): into a `loadout_bin` under it, else as `dry_tails` items into a container under its chute, else as
  blocks onto open ground under the chute; with nowhere, it holds the cake and reads FULL. The filtrate, about 0.49 L a kg, goes back
  on the water pipe as the thickener's overflow does.
- **Dry tails** (`dry_tails`): a block that falls like sand, 1.8 t; carried in a truck by kg; tipped as itself; dug and pushed as soft
  ground; refused by the pit hopper.
- **The truck load-out**: `loadout_bin` (10 x 7 x 10, 400 t) on `loadout_frame` (12 x 10 x 12; legs over a 10 m wide, 9 m clear
  lane; the clamshell gate and chute under its deck; the gate's 7.5 kW hydraulics are its motor). The bin takes dry tails from a press
  over it, by hand and by hopper, and refuses anything else ("dry tails only"). The gate fills a haul truck stopped with its body's
  centre within 2 m of the lane's centre at 2 t/s, until the truck is full by mass or the bin is empty.
- **LOADOUT routes** (`entity.TruckRoute`): the LOADOUT stake stands at the mouth of the lane; a truck drives on through to the frame's
  gate (found within 16 m along the line) and stops there; filled, it drives out of the far side 15 m, round a balloon loop and back
  through the lane (give it about 40 m beyond and 25 m to its right). The leg next to a load-out is single file. It leaves after
  60 s with nothing more loaded once it holds some. Its loads count on its dispatch office as any tipped load does.
- **DUMP POINT, crest and berm** (`pit.Crest`): at the stake before a DUMP POINT a truck looks up to 10 m behind the stake for a crest
  (a drop of 3 m or more). None: a paddock dump, it tips at the stake on the flat. A crest with a berm (mid-axle high, 1 block for the
  100 t truck, spanning its tail within 3 m): it backs until its tail is 1 m short of the berm and tips over it; the load runs down the
  face by the settle rule, so a dump's crest advances as loads are tipped. A crest with no berm: it stops at that stake reading NO
  BERM (BLOCKED in dispatch after 30 s), and goes on once a berm is up.
- **Berms and going over the crest** (`entity.HaulTruckEntity`): reversing, a truck does not step up onto a ridge with a drop behind
  it, so a berm stops a truck driven by hand. A truck not on a route that backs its rear axle over a drop of 3 m or more tips back over
  the edge, falls, and is wrecked where it lands: its item and its load are left there, and its driver takes the fall.
- **The track dozer** (`track_dozer`, `entity.TrackDozerEntity`): 70 t, 447 kW, 8.8 x 4.9 x 4.6 m, a 1,200 L tank, driven by a player.
  Jump steps the blade CARRY, CUT, RAISED; sprint steps the setting for the blade's position (the grade in CUT, the berm's height in
  CARRY). CARRY takes the loose ground and muck in the blade's 5 x 2 front (and loose above it) into its 18-block load and spills the
  rest beside its ends; CUT also cuts the layer under its front, a block below its tracks, stepping down a block each 20, 12 or 10 m
  for 5, 8 or 10 % (level never steps); RAISED takes nothing. Pushing or cutting it goes 3.5 km/h and burns at 80 % power.
  Reversing with a load leaves it in front. At a crest it stops and pushes its load over. Along a crest (2 to 5 m to its side) with a
  load it drops a berm 1 or 2 high at a block a metre. Hard rock in its blade's rows stops it: "hard rock: blast it first".
- **Models:** the four parts are drawn in Wildcatter's materials (`tools/models/pit.py`); the dozer's model and skin come from
  `tools/models/pit_vehicles.py` in Ludo's materials, on tracks: treads over a round idler and sprocket a side, its blade group raised
  and lowered by the blade's position.

## Config

Only one file. It is not created for you.

`config/tailings-train.properties`

| Key | Default if missing | Meaning |
|---|---|---|
| `damage` | `true` | Whether the locomotive hurts living entities in the consist box |
| `amount` | `8` | Damage per hit. A bad number falls back to 8 |
| `truck_damage` | `8` | Damage per hit from a moving haul truck or wheel loader (over 1 m/s). `damage=false` turns it off too |

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
| `truck_dump_wall` | Smooth stone around an iron ingot (placeholder) | 12 x 5 x 2: 3 m retaining wall and a 1.5 m bumper a reversing truck stops against |
| `pit_hopper` | Iron, a hopper and smooth stone (placeholder) | 10 x 6 x 6, 400 t of bank blocks. A truck whose tail is over it tips in; the apron feeder touching it draws it out. It refuses soil |
| `muck` | None. A blast or a tipped load makes it | Broken rock: holds quarters of the bank blocks it was, falls and settles, dug by a bucket |
| `dry_tails` | None. The tailings filter makes it | The filter's cake, 1.8 t a block: falls like sand, tipped on dumps, dug and pushed as soft ground (P4) |

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
- **Blasting items (P3):** `cast_booster` (shapeless: 1 TNT makes 4), `detonator` (shapeless: TNT, copper ingot, redstone make 8),
  `blast_logger` (shapeless: copper ingot, redstone, 2 iron ingots), `blasting_key` (shapeless: iron nugget, redstone), and the
  `blasthole_drill` and `emulsion_truck` machine items (placeholder recipes).

Block items exist for every block above, including the ones with no crafting recipe.

## Entities

| Id | Class | Renderer | Role |
|---|---|---|---|
| `tunnel_drill` | `TunnelDrillEntity` | `NoopRenderer` on both loaders | Not spawned since R1; an old one discards itself. Kept registered for the contraption wave |
| `mine_locomotive` | `MineVehicleEntity` | `MineVehicleRenderer` | Drives its train: path, speed, direction, cars |
| `mine_car` | `MineVehicleEntity` | `MineVehicleRenderer` | A car; its load, and whether it is loaded (synced) |
| `mine_train` | `MineTrainEntity` | `NoopRenderer` | The block-stepped train before R5; an old one discards itself |
| `haul_truck` | `HaulTruckEntity` | `PitVehicleRenderer` (`HaulTruckModel`) | The 100 t truck: 91 t in its body by kilograms; driven, tips on jump |
| `wheel_loader` | `WheelLoaderEntity` | `PitVehicleRenderer` (`WheelLoaderModel`) | Digs soft ground or muck into its 20.4 t bucket and dumps it into a truck, on jump |
| `blasthole_drill` | `BlastholeDrillEntity` | `PitVehicleRenderer` (`BlastholeDrillModel`) | Drills a pattern of 270 mm holes to depth (P3); driven, drills one hole on jump |
| `emulsion_truck` | `EmulsionTruckEntity` | `PitVehicleRenderer` (`EmulsionTruckModel`) | 12 t of emulsion matrix; charges primed holes in its hose's reach on jump (P3) |
| `track_dozer` | `TrackDozerEntity` | `PitVehicleRenderer` (`TrackDozerModel`) | Pushes loose ground with its blade (CARRY, CUT, RAISED), cuts ramps to grade, pushes up berms (P4) |

Vehicles take no damage, except a sneaking empty-hand hit, which breaks them. They deal damage when the config allows it.
