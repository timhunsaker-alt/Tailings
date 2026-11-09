# Open-pit mining: drills, blasting, loaders, haul trucks, haul roads and dumps (APPROVED by Tim 2026-11-09)

**Surface mining with drill, blast, load and haul.** Benches are drilled and blasted. Loaders and shovels load the broken rock
into haul trucks. The trucks carry it on surveyed haul roads to a dump pocket over the primary crusher. **Every load goes
through the plant, country rock as well as ore:** country rock pays its trace minerals as nuggets (crushing-circuit.md,
Nuggets). What the plant rejects is filtered dry, loaded back into trucks and tipped on a waste dump. The dump grows as real
falling blocks that settle at the lowest points. Strip mining with the dragline is in [dragline.md](dragline.md). The explosive
comes from an ammonia plant, proposed in Wildcatter: `wildcatter/docs/multiblocks/ammonia-plant.md`.

**Real counterparts:**
- Truck-and-shovel open pits: porphyry copper and molybdenum pits, bauxite strip mines (Weipa), limestone quarries.
- SME *Mining Engineering Handbook*, 3rd ed. (2011): surface mining, blasting, haulage and dump chapters.
- Dry-stack (filtered) tailings: the plant's tails filtered to a cake, hauled by truck and stacked, as at Karara and La Coipa.
- Autonomous haulage systems (AHS): driverless trucks on surveyed routes under a dispatch system, at Pilbara iron-ore mines.

**Decided with Tim (2026-11-09):**
- **Trucks:** both classes, a 100 t truck and a 360 t ultra-class truck.
- **Loaders:** all of them: a wheel loader, a hydraulic front shovel and an electric rope shovel.
- **Driving:** a player can drive any truck. A truck can also run by itself on a surveyed haul road, under dispatch on
  Wildcatter's PLC control cable.
- **Blasting:** hard rock is drilled and blasted. The explosive is a pumped **emulsion (slurry) explosive** made from the
  ammonium nitrate of a real ammonia plant, which is to be built.
- **Waste:** all rock goes through the crusher and the plant. The plant's rejects are hauled to a dump site. Tipped loads fall
  like gravel and settle at the lowest points, so dumps grow into real waste mountains.
- **Strip mining:** a walking dragline (dragline.md).
- **Ore bodies:** the existing veins. No new worldgen. A pit's value is its veins plus the trace-mineral nuggets in all the
  country rock it moves.

**Decided with Tim (2026-11-09, on this draft):**
- **The plant's size:** option (a), a pit-sized concentrator line, built after the trucks; option (b) until then (below).
- **Truck loads in real tonnes** (below).
- **The ammonia plant is Wildcatter's** (wildcatter/docs/multiblocks/ammonia-plant.md).
- **The 37° settle exception** (below).
- **Boosters and detonators are made with TNT** (vanilla TNT): real cast boosters of TNT and its mixes exist.

**Decided with Tim (2026-11-09, planning P1):**
- **Soil goes to dumps, not the plant.** Dirt, grass, sand, red sand, gravel, clay and mud are overburden: the pit's hopper refuses
  them ("not rock: tip it on a dump") and they are tipped on waste dumps, as real overburden is. Rock and ore all go through the
  plant. (Before this, soil reaching an ore bin was silently lost; that stays for other feeds.)
- **The dump pocket is separate real parts** (below), not one 16 x 8 x 12 part.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked against a manufacturer's spec sheet or the SME handbook before approval.

```
 BENCH: drill pattern -> charge (emulsion truck) -> stem -> fire        (hard rock; soft ground is dug as it is)
   |
   v  muck pile (broken rock, swollen 1.33x)
 LOADER / SHOVEL -> HAUL TRUCK -> surveyed haul road (max grade 10 %) -> DUMP POCKET -> primary crusher -> the plant
                                                                                                             |
                                       tails slurry -> TAILINGS FILTER -> dry tails -> TRUCK LOAD-OUT -> HAUL TRUCK
                                                                                                             |
                                                                    WASTE DUMP (tipped blocks settle downhill) <-
```

## The plant's size: the plant is 50 t/h; a pit moves thousands (decided: a, after the trucks)
The crushing circuit is sized for a small underground mine: **50 t/h, a block of rock every 3 min 15 s**
(crushing-circuit.md). One 360 t truck is 133 blocks of rock, **7.2 hours** of crushing. One 100 t truck is 2 hours. Real pits
feed a gyratory crusher at 2,000-10,000 t/h. The options:
- **(a) A pit-sized concentrator line:** a primary gyratory crusher under the dump pocket (about 3,000 t/h), a coarse-ore
  stockpile, and a SAG mill line (a few hundred to a few thousand t/h). Each gets its own spec. Real and big: a SAG mill line
  draws 10-20 MW.
- **(b) Keep the 50 t/h plant:** the dump pocket and a coarse-ore stockpile buffer the trucks, and the pit runs as fast as the
  plant. The 100 t truck is the right size for it.
- **(c) A written exception:** the existing plant runs at a game-scaled rate for pit rock.

**Decided (Tim, 2026-11-09): (a), built after the trucks, with (b) until then.** Until then the truck dump's 400 t hopper holds
8 hours of feed. The gyratory, the coarse-ore stockpile and reclaim, and the SAG mill line each get their own spec before P7.

## Rock, broken rock and dug ground
- **Soft ground** is dug as it stands: dirt, grass, sand, red sand, gravel, clay, mud, and the soft Wildcatter ores (bauxite,
  phosphate rock, heavy mineral sand, bentonite, borax ore). A loader's bucket takes it block by block.
- **Hard rock** must be blasted first: stone, deepslate, granite, diorite, andesite, tuff, sandstone, calcite, every other
  Wildcatter ore, and the vanilla ores. A loader cannot dig standing hard rock. It reads "hard rock: blast it first".
- **Muck (broken rock):** a blast turns each block in the shot into **muck**: a `muck` block with a block entity that holds what
  the block was (its rock or ore, counted as in a mine car). Real broken rock swells: **3 blocks of bank rock break to 4 blocks
  of muck** (swell 1.33, *proposed*; real hard rock 1.3-1.6). The extra muck spills forward off the face and settles (below).
  A muck block falls like gravel. When dug, it gives its contents to the bucket. It holds its contents in **quarters of a bank
  block**: a full muck block holds 3 quarters.
- **Masses** *(proposed, bank densities)*: rock and ore 2.7 t a block (`PlantRates.BLOCK_KG`, as the plant counts it); dirt,
  grass block, sand and red sand 1.6 t; gravel 1.9 t; clay 2.0 t; mud 1.8 t. A truck or bucket counts kilograms of each item.
- **Tipping on the ground:** soil lands as its own blocks; rock and ore land as muck, 4 quarters of muck for each bank block (the
  same swell).
- **What a truck carries** is counted in tonnes from what it loaded: a block of rock is 2.7 t (2.6-3.0 by rock, as in
  `rock.Rock`), a block of muck three-quarters of that. **Decided (Tim, 2026-11-09): real tonnes.** A 91 t truck carries about 34 blocks of bank
  rock (45 of muck); a 363 t truck about 134 (179 of muck). The truck is full by mass, not by stacks.

## The settle rule (muck piles, tipped loads, dumps)
One rule for every loose block Tailings places: muck spilled by a blast, a tipped truck load, a dragline's cast.
- A placed loose block falls straight down to the first solid block.
- It then looks along its four sides. It rolls one cell toward a side where resting would leave the ground too steep, and falls
  again: a step of **2 or more to the next column** (1 in 1, 45°, the steepest a block world holds), or **4 up in a run of 4
  columns**. It goes the steepest way, a tie by chance, and stops after 64 rolls.
- So a long slope stands at **3 in 4, 37°**, real broken rock's angle of repose. **Written exception (decided with Tim,
  2026-11-09):** the 37°. *As built (P1):* the first way proposed, a 1-in-3 chance for a block to creep on past a single step,
  measured 43° in the unit test, so the run-of-4 rule replaced it; 2,000 blocks tipped at one point stand at 36°
  (`SettleTest`).
- **Into water or lava** it falls through and displaces the fluid. A dump can fill a flooded pit (real in-pit backfill).
- **Load:** a tipped 360 t load is about 200 blocks of dry tails. Settling is worked out on the server and placed at once,
  with a short falling animation for the client. No falling-block entities *(proposed, for the server's sake)*.
- The rule lives in plain Java (`mine/Settle`), with unit tests.

## Machines

### Haul trucks (entities, true size)
| | `haul_truck` (100 t class) | `ultra_haul_truck` (360 t class) |
|---|---|---|
| Payload | **91 t** (target payload) | **363 t** (nominal) |
| Size L x W x H | **10.0 x 6.2 x 5.2 m**, about 10 m high with the body raised *(height to the cab unverified)* | **15.1 x 9.5 x 7.7 m**: 9.5 m over the tyres; length and height *(unverified)* |
| Empty mass | about 70 t *(unverified)* | about 260 t (624 t rated gross less the payload) |
| Engine | **765 kW** gross diesel, mechanical drive | **2,983 kW** gross diesel |
| Fuel | Wildcatter diesel, about 1,100 L tank, about 100 L/h working *(proposed)* | about 4,000 L tank, about 350 L/h *(proposed)* |
| Top speed | 60 km/h on the flat, empty | 64 km/h |
| Up a 10 % haul road, loaded | about 12 km/h *(proposed, from P = m g v (sin θ + rolling 0.02), 80 % at the wheels)* | about 12 km/h |

- **Speed follows power**, as the trains do: v = P η / (m g (sin θ + rolling)), η 0.8 at the wheels *(proposed)*, with a
  retarder holding a loaded truck to 15 km/h down a 10 % grade *(proposed)*.
- **The body** holds any mix of rock, ore, muck contents and dry tails, counted by item and tonne. It tips to dump: the load
  goes out behind the truck by the settle rule, or into a dump pocket's hopper.
- **Driving:** right-click to sit in the cab and drive with the movement keys. A truck hurts a player or mob it hits, by the
  trains' config (`config/tailings-train.properties`, a new `truck_damage` line). Sneak-attack with an empty hand to break it
  into its item and its load.
- **Running by itself:** see Haul roads and dispatch.
- **Placing:** right-click the ground with the truck item. Crafted from Wildcatter parts (placeholder recipes until the art wave).

### Loaders
| Part | Size (W x L x H) | Bucket | Power | Cycle | Fills a 100 t truck in | Fills a 360 t truck in |
|---|---|---|---|---|---|---|
| `wheel_loader` | 4.5 x 16 x 6 m *(unverified)* | **12 m³**, 20.4 t (high-lift rating; the class takes 11.5-24.5 m³ buckets) | **607 kW** net diesel | 40 s | 5 passes | 18 (not done in practice) |
| `hydraulic_shovel` (front shovel, about 250 t) | 7 x 15 x 8 m *(unverified)* | **15 m³**, about 27 t *(unverified)* | about 1,100 kW diesel *(unverified)* | 30 s | 4 passes | 13 |
| `rope_shovel` (electric) | 14.4 x 15 m crawlers and house, about 21 m to the boom point *(height unverified)* | **55 m³** dipper, **100 t** (the class: 53-61 m³, 100-109 t) | electric off the pit cable; real ones take 7.2 or 13.8 kV from at least a 3,750 kVA transformer. About 1.5 MW average, 3.5 MW peak *(proposed)* | 35 s | 1 pass | **4 passes** |

- **Operation:** a player sits in the cab. A bucket pass digs the bucket's volume from the face in front: soft ground block by
  block, or muck. It then swings and dumps into the truck standing under the bucket. In AUTO (a button on the cab's screen),
  it keeps loading whatever truck stands in its loading spot until the face in reach is gone *(proposed)*.
- **Reach:** the face up to the bucket's height: about 6 m for the loader, 12 m for the hydraulic shovel, 15 m for the rope
  shovel *(proposed; a bench is cut to the shovel's reach)*.
- **The wheel loader** also loads a truck from a stockpile, and moves on wheels at up to 20 km/h. **The shovels** crawl on
  tracks at about 1.5 km/h *(proposed)*.
- **The rope shovel** is a big contraption, like the TBM: its parts (crawlers, revolving frame and machinery house, boom,
  dipper handle and dipper) are placed as big parts, checked, and lift into one entity while it moves or swings
  (multiblocks README, The contraption). It draws power through a trailing cable from a pit switch house.

### Rotary blasthole drill (`blasthole_drill`)
- **Real:** a crawler-mounted rotary drill with a mast, drilling **270 mm** holes (the class: 171-270 mm, 311 mm on the larger
  rigs) to the bench depth plus 1.5 m of subdrill, **up to 16.8 m in a single pass**. Electric off the pit cable, a **522 kW** motor
  (746 kW option), or diesel.
- **Size:** 7 x 15 x 26 m with the mast up *(unverified)*. Contraption, like the rope shovel.
- **Its screen sets a pattern:** rows and columns, burden and spacing (proposed default **7 x 8 m**), and depth (the bench height
  plus subdrill). It walks the pattern and drills each hole. A drilled hole is a column of `blasthole` blocks with its
  cuttings beside the collar, which serve as stemming.
- **Rate:** **1 m every 10 s** *(scaled; a real rotary drill makes about 30-60 m/h)*.
- PLC points: RUN, RUNNING, HOLES DRILLED (pulse).

### Emulsion truck (`emulsion_truck`, a bulk explosives truck)
- **Real:** a bulk truck carrying emulsion matrix (an ammonium-nitrate oxidiser, not yet an explosive), diesel and a gassing
  solution. It pumps the matrix down the hole through a hose and sensitises it in the hole by gassing.
- **Carries:** 12 t of emulsion matrix *(proposed)*, filled from the ammonia plant's emulsion matrix (ammonia-plant.md).
- **Charging:** parked by a hole, it charges the hole to its set column (default: the hole's depth less 4 m of stemming). At
  **1.2 g/cm³** a 270 mm hole holds **69 kg a metre** *(from the volume; proposed density)*. A 16 m hole, 12 m charged, holds
  about 820 kg.
- **Powder factor:** a 7 x 8 x 14.5 m block is 812 m³, 2,190 t. 820 kg is **0.37 kg/t** *(real hard-rock open pits run about
  0.2-0.5 kg/t)*.
- **Initiation:** each hole needs a `cast_booster` and a `detonator` dropped in before charging (items). The holes are tied in to a
  `blasting_machine` with a delay chosen per row (25 ms default). **Both are made with TNT (decided with Tim, 2026-11-09):**
  - `cast_booster`: a 400 g cast of TNT *(real boosters are TNT, pentolite or TNT mixes, 340-450 g)*: one TNT makes 4
    *(proposed)*;
  - `detonator`: an electronic delay detonator. Its base charge is really PETN; TNT stands in for it, written as an exception.
    One TNT, copper wire and a circuit makes 8 *(proposed)*.
- **Stemming:** each hole is filled to its collar with its cuttings or 4 gravel *(proposed)*. A hole not stemmed blows out: half
  the break and a crater of flyrock *(proposed)*.

### The blast
- **Fired** from the `blasting_machine` (a key and a FIRE button, or the PLC's FIRE point).
- **What breaks:** each charged, primed, stemmed hole breaks its burden x spacing x depth to muck. A hole with less than **0.15
  kg/t** fails and breaks nothing *(proposed)*. Muck spills forward off the free face by the settle rule.
- **Danger:** players and mobs within **50 m** of a firing hole take damage, more nearer, and blocks of flyrock land up to 100 m
  off *(scaled; a real blast clears people to 500 m)*. No vanilla explosion: no crater and no item loss. A siren sounds for 10 s
  before the shot *(proposed)*.
- **Ground left:** the floor at the subdrill depth is left flat as the next bench's floor.

### Track dozer (`track_dozer`)
- **Real:** a **70 t** crawler dozer with a semi-U blade **4.9 m wide, 2.1 m high, 18.5 m³**, **449 kW** net diesel.
- **Driven by a player.** Its blade cuts the layer level with its tracks: soft ground and muck, one block deep, pushed ahead as a
  pile up to the blade's 2.1 m height (a 5 x 2 block blade). It cuts ramps and benches and pushes a pile over a dump crest (settle rule).
- Left out: the rear ripper for soft rock.

### Pit power: the pit switch house and trailing cable
- **Real:** a skid-mounted switch house on the pit's power line, with a breaker for each machine's trailing cable.
- `pit_switch_house` (3 x 3 x 6, *proposed*): a Wildcatter circuit part on Wildcatter's medium-voltage cable. **Four outlets**,
  each a breaker and a trailing cable up to **400 m** *(proposed; the TBM's is 300 m)*, to the rope shovel, the drill and the
  dragline. Moving past the cable's length stops the machine ("cable end").
- **Voltage:** Wildcatter's medium-voltage tier. Real shovels and draglines run at 4.16-7.2 kV.

### Truck dump (`truck_dump_wall` and `pit_hopper`, split into parts with Tim, 2026-11-09)
- **Real:** a truck dump: a concrete retaining wall with a bumper on its lip that a truck backs to, and a steel-lined hopper below
  the lip that it tips into, over the feeder to the primary crusher.
- `truck_dump_wall`: **12 x 5 x 2 cells** *(proposed)*: 3 m of concrete wall below the pad and a **1.5 m bumper** on its top edge
  (about half the truck's 2.7 m wheel). A reversing truck stops against the bumper: it steps up 1 block, not 1.5. *(As built, P1:
  the bumper first stood 1 m in a 4-row part, and a truck stepped over it; the bumper's top half needs a fifth row of cells.)*
- `pit_hopper`: **10 x 6 x 6 m** *(proposed)*, below and behind the wall, its top at the wall's top. It holds **400 t** (one
  ultra-class load and a margin; 400 t of broken rock at about 1.7 t/m³ is 235 m³ of its 360). A truck whose tail is over it tips
  into it.
- **Downstream:** the hopper is drawn by the existing apron feeder touching it, then the ore sorter and ore bins, as the rail dump
  pocket is (dump-station.md), and so to the jaw crusher at its rate (or, by Decision 1, the gyratory).
- It takes whole bank blocks: a load's rock and ore go in as items (2.7 t each). What is under a whole block of each ore the
  hopper keeps, and the next load makes it up; the truck comes away empty (decided with Tim, 2026-11-09). It refuses soil.
- Reads FULL when a truck could not tip its load into it, and a self-driving truck waits.
- **The coarse-ore stockpile** moves to P7 with the gyratory: at 50 t/h a 400 t hopper already holds 8 hours of feed.
- PLC points: LEVEL (4-20 mA), FULL.

### Tailings filter and truck load-out (dry stack)
- `tailings_filter`: a large plate-and-frame filter press. It takes the tails slurry (the flotation bank's tails, which today go to
  the tailings pond placeholder) and gives **dry tails at 15 % moisture** *(proposed; real dry-stack cake is 12-20 %)*. Rate:
  **60 t/h of solids** *(proposed, more than the plant's 50 t/h)*, about 150 kW *(proposed)*. Its size and grid come with the
  model.
- **Dry tails** are `dry_tails` blocks when placed: 1 m³ at **1.8 t** *(proposed; real dry stacks are 1.6-1.9 t/m³)*. So a 91 t
  load is 50 blocks and a 363 t load about 200.
- `truck_loadout`: a steel bin of 400 t on legs, with a chute over a drive-through lane. A truck under the chute is filled. It is fed
  by belt from the filter, and also takes the gravity circuit's coarse rejects.
- The tailings pond placeholder stays for slurry. The lined tailings cell (ROADMAP 4d) is still to come.

### Waste dump
- A **dump site** is ordinary ground the player chooses. A surveyed **dump point** (below) at a crest tells a truck where to tip.
- A truck backs to the crest and tips: the load goes out behind it by the settle rule, so the dump's face advances out from the
  crest as tipped loads roll down it, as a real end-tipped dump does.
- **A safety berm** (a windrow of dry tails 1 block high) along the crest stops a reversing truck *(proposed: the dozer pushes
  one up; a truck backing over a crest without a berm rolls off and is wrecked)*.

## Haul roads, surveying and dispatch
- **Survey stakes** (`survey_stake`, a block): a player places stakes along a road and names its stops on the stake's screen:
  LOAD (by a loader), DUMP POCKET, LOADOUT, DUMP POINT, FUEL, PARK. A **survey book** item links stakes in order, by clicking them,
  into a route.
- **The surveyed route** is the line through its stakes. The survey checks it and writes problems on the book:
  - **grade:** not over **10 %** between stakes *(real haul roads 8-10 %)*;
  - **width:** at least **3 truck widths** for two-way traffic (18 m for the 100 t truck, 30 m for the ultra) *(SME rule)*.
    Narrower roads are one-way, and a truck waits at the stake before one for an oncoming truck to clear;
  - **clear:** no solid block in a truck's way, along the line and its width.
- **Self-driving trucks:** a truck given a route (click it with the survey book) drives itself: to LOAD, waits in the loader's
  spot, then to its dump stop, tips, and back. It slows for curves and grades by power, keeps 50 m from the truck ahead
  *(proposed)*, and stops for any player, mob or vehicle in its way (real AHS obstacle detection). It goes to FUEL when its tank is
  under 15 %, and to PARK when told to stop.
- **The dispatch office** (`haul_dispatch`, a 3 x 3 x 3 building, *proposed*) is a Wildcatter `ControlDevice` on the control
  cable, so a PLC runs the fleet. It reaches trucks within **2 km** by radio *(proposed)*. Its points, for each route (up to 8):
  - RUN (command): trucks on the route run; off sends them to PARK;
  - RUNNING, TRUCKS (count, 4-20 mA over **0-16 trucks**, 1 mA a truck *(proposed)*), LOADS (pulse each tipped load), TONNES
    (4-20 mA over **0-2,000 t** tipped in the last hour *(proposed)*);
  - for the whole fleet: FUEL LOW (any truck under 15 %), BLOCKED (a truck stopped for an obstacle more than 30 s).
- **Fuel bay** (`fuel_bay`, *proposed*): a diesel dispenser fed by Wildcatter pipe from a tank. A truck stopped at a FUEL stake fills.
- **A player can take over** any self-driving truck by sitting in it. It drops off its route until it is given one again.

### P2 decided with Tim (2026-11-09)
- **Loading:** both. A player in the wheel loader fills a truck at a LOAD stake, or the loader runs itself: a loader set to a
  LOAD stake (click it with the survey book) digs the face in front and fills each truck that pulls in, so a fleet runs with
  nobody there.
- **Fuel burns by engine work:** litres = the engine's work / (36 MJ/L x 0.38) *(diesel's 36 MJ/L is Wildcatter's
  `LIQUID_FUEL_JOULES_PER_L`; 0.38 engine efficiency proposed)*: about 200 L/h for the truck flat out, near nothing parked. The
  loader burns the same way.
- **Refuelling only at a fuel bay** (no buckets).
- **Two waves:** P2a stakes, the survey book, routes, self-driving trucks, the self-running loader, fuel and the fuel bay; P2b
  the dispatch office on the PLC.

### P2a as planned
- **`survey_stake`** (a block): right-click with an empty hand steps its stop kind: none (a waypoint), LOAD, DUMP POCKET,
  LOADOUT, DUMP POINT, FUEL, PARK; sneak-right-click reads it. It faces the way the player placed it.
- **The survey book** (`survey_book`): click stakes in order to make a route (up to 32 stakes); sneak-use in the air clears it.
  Each click surveys the leg to the stake before and writes what is wrong in the chat and on the book:
  - grade over **10 %**;
  - not clear: a solid block in a truck's box (6.2 m wide, 5.2 m high) along the leg;
  - narrow: clear less than **3 truck widths** (18.6 m), so the leg is one-way (below).
  A route with a problem is still saved; a truck given it stops at the leg's start and reads the problem.
- **A route runs out and back** between its two end stakes, its ends being its stops (LOAD at one end, DUMP POCKET, LOADOUT or
  DUMP POINT at the other). **A truck backs into an end stop** from the stake before it, as a truck backs up to a loader or a
  dump: it stops at that stake, turns round there until its nose points away from the end (as on a dump's turning area: give it
  about 25 m of open ground), then reverses to the end stake. It drives out forward. Stakes between are waypoints, or FUEL and PARK stops it takes when it needs them.
- **Driving itself:** a truck clicked with a book takes its route (a player sitting in it takes it off). It steers for the next
  stake, slows to **15 km/h** *(proposed)* for a turn of more than 30 degrees at a stake, keeps **50 m** from a truck ahead on its
  route *(spec)*, and stops for any player, mob or vehicle within its stopping distance plus 5 m ahead *(proposed)*; stopped more
  than 30 s, it reads BLOCKED. Before a one-way leg it waits **40 m** short of the leg's start *(proposed after approval, to keep the junction clear)* while a truck is on the leg coming the other way.
- **At LOAD** it waits under the loader until it is full, or until 60 s *(proposed)* pass with nothing loaded once it has a load.
  **At its dump stop** it tips (into a hopper under its tail, or out behind it), then goes back.
- **The self-running loader:** set to a LOAD stake within 16 m *(proposed)*, it digs a pass whenever its bucket is empty and holds
  the full bucket ready, as a loader does between trucks, and dumps when a truck is in reach; it stands where it is (moving it is the driver's job). It reads
  "face worked out" when nothing is left in reach.
- **Fuel:** the truck's tank **1,100 L** *(spec, proposed)*, the loader's **1,500 L** *(proposed)*; a new machine comes with
  **10 %** *(proposed)*, enough to reach a fuel bay. The engine's work is its power at the wheels over 0.8, plus the body hoist at
  **30 %** of rated power while it moves and the loader's passes at **60 %** *(proposed)*; it idles at **3 %** *(proposed)* with
  a driver in or while it runs a route, and is off otherwise. Out of fuel it stops and reads "out of fuel". A self-driving truck
  under **15 %** *(spec)* stops at the next FUEL stake on its route.
- **`fuel_bay`** (a part, 4 x 3 x 2 m *(proposed)*: a dispenser island with a 2,000 L day tank *(proposed)*) takes diesel by
  Wildcatter pipe on any face and fills a stopped truck or loader within 8 m *(proposed)* at **600 L/min** *(proposed: a
  fast-fill nozzle)*.
- **Readouts:** sneak-right-click a truck or loader for its fuel, route, stop and state.
- **Stakes don't block:** a truck drives over a stake. **An end stake marks where the truck's tail stops**; at a DUMP POCKET it
  backs on up to 3 m past the stake *(proposed)* until the bumper stops it, so the hopper is under its tail. On a two-way leg a truck keeps **4 m right** of the stake line *(proposed: one lane each way)*.
- **PARK** stakes wait for P2b's dispatch (RUN off sends trucks to PARK); in P2a a truck passes them. *(P2b: done, below.)*
- **Chunks:** a truck on a route refreshes `tailings:runner` on its chunks and the next stake's.
- **Game tests:** `pit_survey` (a 3-stake route; a 15 % leg and a blocked leg are written on the book), `pit_truck_haul` (spec: a
  truck on a route loads at a self-running loader, climbs a 10 % ramp, backs to the bumper, tips into the hopper and comes
  back), `pit_truck_obstacle`, `pit_truck_follow`, `pit_one_way`, `pit_fuel_burn`, `pit_fuel_bay`, `pit_out_of_fuel`,
  `pit_loader_auto`; the client film `pit_haul`.

### P2b as built (2026-11-09, decided while Tim was away: docs/questions/p2b-dispatch.md)
- **A route is registered** by clicking the office with the survey book (the next free route of 8); sneak-clicking takes it off. A
  truck is on a route when its stakes are the same stakes, either way along them.
- **Its selector** (Wildcatter's HAND / OFF / AUTO): HAND runs every route, as with no office; OFF parks them all; AUTO follows each
  route's RUN while the PLC keeps writing it. **A PLC that stops** (RUN not refreshed for Wildcatter's stale time) parks the route
  *(proposed: the safe state is trucks parked, not trucks running nobody can stop)*.
- **Parking:** RUN off, a truck drives on along its route to the PARK stake and stops there with its engine off. If the PARK stake
  is behind it, it finishes its trip (a loaded truck tips first) and parks on the way back. A truck filling at LOAD leaves at once.
  A route with no PARK stake stops at the next stake.
- **No radio** (the office unpowered, or the truck over 2 km off): the truck stops at its next stake and reads "no word from
  dispatch" *(proposed: real autonomous trucks stop when they lose their dispatch link)*.
- **Power:** the office is a **5 kW** *(proposed)* single-phase load on Wildcatter's low tier; under 85 % of nominal its radio is
  dead and its loops read 0 mA (a failed loop).
- **Points:** 43: five a route (RUN, RUNNING, TRUCKS, LOADS, TONNES), then FUEL LOW, BLOCKED and AUTO. RUNNING is on while a truck
  on the route is working (not parked, halted or dry).
- **Readout:** right-click the office with an empty hand: its selector, power, and each route's RUN or PARK, trucks, loads and
  tonnes in the last hour.
- **Game tests:** `pit_dispatch` (spec), `pit_dispatch_alarms` (FUEL LOW, BLOCKED), `pit_dispatch_fallback` (the PLC stops; HAND;
  no power).

## Chunks
A running truck, loader, shovel, drill or dragline refreshes the `tailings:runner` ticket on its chunks and the one ahead (R1).
A self-driving fleet keeps its route's chunks loaded only while trucks are on them.

## Left out (to ROADMAP)
- Trolley assist (overhead lines on the ramp).
- Grade control (ore and waste sent to different places): Tim's rule sends everything to the plant.
- In-pit crushing and conveying.
- Graders and water trucks; tyre wear; road condition.
- Pit slope failure and wall stability.
- Reclamation and topsoil.
- Electric drive for the haul trucks.

## Game tests (each wave adds its own)
- `pit_settle`: 200 blocks tipped at one point stand at no more than 1 in 1, average near 37°, and fill a hole beside the tip first.
- `pit_truck_haul`: a 100 t truck on a 3-stake route loads at a loader, climbs a 10 % ramp, tips into the dump pocket and comes back.
- `pit_truck_drive`: a player-driven truck tips at a crest; the load settles downhill.
- `pit_loader_soft`: a wheel loader fills a truck from a sand face in 5 passes.
- `pit_loader_hard`: a loader at a stone face reads "hard rock: blast it first".
- `pit_drill_pattern`: a 3 x 3 pattern drills 9 holes to depth.
- `pit_blast`: a charged, primed, stemmed 3 x 3 pattern breaks to muck at 4 for 3; an unstemmed hole blows out; a player 20 m off
  is hurt.
- `pit_dispatch`: a PLC turns a route's RUN on and off; trucks run and park; LOADS pulses.
- `pit_loadout`: dry tails from the filter fill a truck under the load-out.
- `pit_berm`: a self-driving truck stops at a berm and tips.

## Proposed waves (after approval)
- **P1:** the settle rule, muck, the 100 t truck (driven), the truck dump (wall and hopper), the wheel loader on soft ground.
- **P2a:** survey stakes, the survey book, routes, self-driving trucks, the self-running loader, fuel, the fuel bay.
- **P2b:** the dispatch office on the PLC.
- **P3:** the ammonia plant (a Wildcatter wave), the drill, the emulsion truck, TNT boosters and detonators, the blast.
- **P4:** the tailings filter, the load-out, waste dumps, berms, the track dozer.
- **P5:** the pit switch house, the ultra-class truck, the hydraulic shovel, the rope shovel.
- **P6:** the dragline (dragline.md).
- **P7:** the pit-sized line (decided): gyratory crusher, coarse-ore stockpile and reclaim, SAG mill line, each specced first.

## Sources for the figures
Checked 2026-11-09 against manufacturers' and dealers' published specifications for current machines of each class: a 91 t
mechanical truck (C32 engine), a 363 t truck (C175-20 engine), a 992-class wheel loader, a 4100-class electric rope shovel, a
271-class rotary blasthole drill and a D10-class dozer. Brand names stay out of the game. Figures still marked *(unverified)*
were not in those sheets.
