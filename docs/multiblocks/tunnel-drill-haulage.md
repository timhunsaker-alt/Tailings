# The track-laying tunnel drill and twin-track haulage (DRAFT, not approved)

A **programmable tunnel-boring machine** that drives a decline down to a set Y level, then a flat drive, laying **two parallel
tracks** behind it. Mine cars run loaded out on one track and empty back on the other, between a loading station behind the
drill and a dump station at the plant's ore bin.

Real counterparts:
- Small hard-rock TBMs and the Robbins Main Beam TBM; the Rio Tinto / Sandvik Mobile Tunnel Miner for mine drives.
- Rail-haulage mines with twin track and passing loops (classic hard-rock practice; SME *Mining Engineering Handbook*, 3rd ed., 2011, haulage chapter).
- Rail mines keep grades under about 1-2 % for adhesion; a decline steeper than that uses rope haulage or a rack.

```
 surface:  DUMP STATION -> ore bin -> crushing circuit
              |  ||  (loaded track in, empty track out)
 decline:     \\  1-in-2 slopes under the tracks, a walkway between, 6 x 6 lined bore
               \\
 level Y:       ======== flat drive ======== LOADING STATION <- conveyor <- TUNNEL DRILL (cutterhead)
```

## The bore (decided with Tim, 2026-11-02; 6 x 6 lined bore)
The drill is a **tunnel boring machine (TBM)**. It cuts the rock, then builds a ring of **lining segments** around the bore as
it advances, as a real segmental-lining TBM does, so ground water and lava cannot get in. Inside the lining the drive is a
clear **6 blocks wide by 6 blocks high**. The cutterhead cuts an **8 x 8** face and the lining takes the outer ring of 1 block
(decided with Tim, 2026-11-02).

Real counterparts:
- Precast concrete segment rings erected inside the TBM's shield, gaskets sealing every joint (Herrenknecht and Robbins
  single-shield TBMs; BTS/ITA segmental lining guidance).
- Probe drilling and grouting ahead of the face in water-bearing ground.

The floor pattern, west to east (drawn facing the heading), on the lining's invert:

| Column | 1 | 2 | 3 | 4 | 5 | 6 |
|---|---|---|---|---|---|---|
| Floor | slope | slope + track | stair walkway | stair walkway | slope + track | slope |

Cross-section on the decline, looking down the heading (`L` lining segment, `/` slope block, `#` mine rail on a slope block,
`S` stair or landing, `.` open):
```
   L   1   2   3   4   5   6   L
 +---+---+---+---+---+---+---+---+
 | L | L | L | L | L | L | L | L |   y+6   crown (roof segments)
 | L | . | . | . | . | . | . | L |   y+5
 | L | . | . | . | . | . | . | L |   y+4
 | L | . | . | . | . | . | . | L |   y+3   5 blocks clear above the floor
 | L | . | . | . | . | . | . | L |   y+2   (cars are 2 high; a player 1.8)
 | L | . | . | . | . | . | . | L |   y+1
 | L | / | # | S | S | # | / | L |   y     floor (half-height pieces)
 +---+---+---+---+---+---+---+---+
 | L | L | L | L | L | L | L | L |   y-1   invert (floor segments)
```
Side view of a track column on the default 1-in-2 decline (each block of run drops half a block):
```
 L L L L L L L L L L          lining crown follows the grade in half-block steps
 ‾‾\__
       ‾‾\__                  # rail on a lower-half / upper-half slope pair
             ‾‾\__
 L L L L L L L L L L          invert
```

### The slope: 1 in 2 by default (decided 2026-11-02, replacing 1 in 1)
- **The pieces:** `tailings:slope` comes in a lower half (a wedge rising 0 to 0.5) and an upper half (0.5 to 1). Two in a
  row make one block of rise over two of run: a smooth 26.6° ramp. A `steep` variant (0 to 1 in one block) stays for
  1-in-1 declines if Tim wants a steep option in the GUI.
- **How it renders:** the slope and the rail bed are a real 1-in-2 wedge: eight thin slices, low at the downhill end and high
  at the uphill end, and the collision matches those slices so a player walks the ramp. (Vanilla models are cuboids, so the
  wedge is a framed slope of eight boxes, the same trick framed-slope mods use. Two halves in a row meet in the middle.)
  The lining crown still steps a full block every two advances; headroom over the floor stays 5 blocks.
- **The walkway (decided with Tim, 2026-11-02):** columns 3 and 4 are **real stair blocks with a landing every other block**:
  a stair (one block of rise), then a full-block landing, then a stair. That is one block of rise over two of run, the
  same 1 in 2 as the track, so the walkway meets the track's height at every landing. The player climbs the stairs
  normally and rests on each landing.

Side view of the walkway beside the track (`S` stair, `_` landing, `#` track on the slope pair):
```
 walkway:   S_            track:   ‾‾\__
              S_                         ‾‾\__
                S_                             ‾‾\__
            (stair, landing, stair...)   (smooth 26.6° ramp)
```
- **The rack stays, and it is now realistic.** 1 in 2 is a 50 % grade. Plain rail adhesion tops out near 5-7 % and most
  mine haulage keeps under 2 %, so no adhesion locomotive holds 50 %. The steepest rack railway, the Pilatusbahn, climbs 48 %
  with its Locher rack. So the 1-in-2 decline needs a rack, and with one it is close to a real record railway instead of a
  written impossibility. The old "45° on a rack" game exception is dropped; a 1-in-1 option would bring it back.
- **Length:** a 1-in-2 decline needs 2 blocks of run per block of depth, so reaching y -40 from y 64 takes about 208 blocks.

## The pump and keeping the drive dry (decided with Tim, 2026-11-02)
The drill's **dewatering pump** (part `dewatering_pump`, below) clears every liquid source block it meets in or beside the
bore, before the lining closes. The lining then keeps the drive dry. Flowing liquid is shut off with its source.

| Liquid | What the drill does | Cost |
|---|---|---|
| **Water** (source or flowing) | Pumps it into the drill's **water tank** (256 m3). That tank is what quenches lava. **Water past a full tank is voided** (revised with Tim, 2026-11-03; the tank is real, the surplus is not piped to surface) | Real lift work: E = ρ g V H / η. 1 m3 lifted 100 m at 70 % efficiency is 1.4 MJ (0.39 kWh). The pump draws its power from the trailing cable |
| **Lava** (source or flowing) | It cannot be pumped. The drill **quenches** it: water sprayed from its own tank turns a lava source to obsidian and flowing lava to cobble, which it then cuts as rock and sends to the cars (obsidian goes out as muck). A cutterhead cannot cut molten rock, so it waits while it quenches | 1 m3 of water per lava block *(proposed)* from the water slot or the discharge line, plus 20 s of the pump's time. If there is no water the drill stops and says why |
| **Other mods' liquids** | Pumped like water if the loader says the fluid is not hot (temperature under 100 C); otherwise treated like lava; if it cannot tell, it stops and asks | As water or lava |
| **Caves and air gaps** | The track layer fills open space under the floor with cobble, and the lining closes the walls and roof as usual, so a cave is sealed off from the drive | Cobble from the inventory |

- **Where the water goes:** pumped source blocks fill the quench tank first. Anything the tank cannot hold is deleted.
  Quench water is that tank (buckets can still top it up). The power cost is still charged as if the water were lifted.
- **Probing:** each advance the drill checks 3 blocks ahead of the face for liquid (as real probe holes do) and pumps or
  quenches before cutting.
- **Pump size:** a 75 kW submersible-class dewatering pump at 480 V, about 30 L/s at 100 m head *(unverified)*, so a source block
  clears in about 35 s. Big inflows (an aquifer, an ocean above) can outrun it; the drill stops if water gains on it.

## The drill
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `cutterhead` | 8 x 8 x 1 | Disc-cutter head in a shield, cutting the 8 x 8 face that the lining reduces to a 6 x 6 drive *(a square face is a Minecraft stand-in for a round TBM bore)* |
| `segment_erector` | 8 x 8 x 2 | Ring erector inside the tail shield: places one ring of lining segments each block of advance |
| `main_beam` | 2 x 2 x 8 | Thrust cylinders and grippers, with the operator cab (the controller); 4.16 kV, about 1 MW total *(unverified, small TBM class)* |
| `dewatering_pump` | 2 x 2 x 2 | 75 kW, 480 V dewatering pump with its water tank, drain pipes and quench sprays (above) |
| `muck_conveyor` | 1 x 1 x 8 | Belt from the head back to the loading station |
| `track_layer` | 6 x 1 x 3 | Places the floor pattern and the mine rail on columns 2 and 5, from the drill's inventory |
| `trailing_cable_reel` | 2 x 2 x 2 | 4.16 kV trailing cable from the nearest Wildcatter breaker; range set by cable length |

### The drill's GUI and inventory (decided with Tim, 2026-11-02)
Right-click the operator cab to open its screen.
- **Program:** target Y; heading; flat-drive length; stop conditions (ore seen, cable end, liquid it cannot clear, inventory
  empty). The drill drives the decline down at 1 in 2 (1 in 1 as an option) to the target Y, then runs flat.
- **Inventory slots**, which the drill uses as it advances:
  - **Mine rail**: two a block of advance.
  - **Gravel**: ballast under the track on the flat drive, one per track block *(proposed)*.
  - **Cobble**: fill for caves and gaps under the floor, and a fallback stock for the floor pieces.
- **The drill makes its own floor pieces** (decided with Tim, 2026-11-02): slope pieces, stairs and landings are cut by the
  track layer from cobble or any suitable rock it has just mined (stone, cobble, deepslate, granite, diorite, andesite, tuff,
  sandstone), one rock block a piece. The player never crafts them. It never uses ore, gravel, sand or dirt; those go to the
  cars. The pieces take the look of the rock they are cut from. If the mined rock runs short (in a cave or ore body) it draws
  on the cobble slot, and stops when that is empty too.
  - **Lining**: 28 blocks a ring. The default ring is cobble or any stone the drill has just mined or is holding (stone, cobble, deepslate, granite, diorite, andesite, tuff, sandstone). **Crafted lining segments** (4 gray concrete + 1 iron = 4) are an optional cleaner ring, used when a full ring of them is in the slot.
  - **Water** (a tank slot): quench water for lava, filled from a pipe. It is separate from the pump's tank.
- The screen shows the floor-piece stock (mined rock held back for pieces), each slot's count and how many blocks of drive it lasts. When a slot runs out the drill stops and says why.
- **Readout:** depth, Y, blocks driven, power draw, cable left, water pumped.

### Running
- **Advance:** real small TBMs make 10-30 m a shift in hard rock. The game: 1 block per 20 s in stone, slower in deepslate
  *(proposed)*.
- **Rock out:** every block cut is a real run-of-mine rock item (cobble, granite, ore) sent to the cars, not dropped.
- **Ground:** the lining is the ground support; no roof bolts are needed.

## Haulage
- **Two tracks:** loaded out, empty in. No passing loops needed; switches at each station loop cars from one track to the other.
- **Cars:** Tailings' own mine cars, not vanilla minecarts (decided with Tim, 2026-11-02). See "The mine cars" below.
- **Grade:** 1 in 2 on the decline, held by the rack in the mine rail (see The slope).
- **Stations:** the loading station fills cars from the drill's conveyor; the dump station tips and sorts them into the ore bins.
- **Gauge:** 1 block (decided with Tim, 2026-11-02): each track is one block wide, about a 900 mm mine gauge.
- **Signals:** a block system so cars on one track keep their distance.

## The mine cars and the powered track (decided with Tim, 2026-11-02: custom cars, not vanilla minecarts; the track carries the power and is safe to walk on)
Real counterparts:
- Granby side-dump cars and bottom-dump cars on 600 mm or 900 mm gauge (SME *Mining Engineering Handbook*, haulage chapter).
- **Ground-level power supply**, as on Alstom's APS on the Bordeaux tramway: a conductor rail between the running rails, split
  into short sections that are switched live only while a vehicle covers them, so the rail is dead where anyone can step.
- Rack railways (the Pilatusbahn's Locher rack, 48 %) for the decline.

There is **no overhead wire and no shock hazard on the track.**

| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `mine_rail` | 1 x 1 x 1 per track | Heavy 30 kg/m mine rail on steel ties, its own block (not vanilla rail), 1 block gauge, with a **shrouded, segmented conductor rail** set flush between the running rails and a rack for the decline. A section is live only while a car or locomotive sits over it; the running rails are the return. Walking on it is always safe |
| `traction_rectifier` | 2 x 3 x 2 | Mine traction substation: 480 V 3φ AC in from the Wildcatter grid, 250 V DC out to the mine rail *(the DC side is worked out inside it, as the conversion plant's rectifier does; no DC tier in the solver)*, 300 kW *(unverified)*. Placed beside the track; feeds the track it touches and all track joined to it |
| `mine_locomotive` | 1 x 2 x 3 | 8 t mine locomotive, two 37 kW traction motors and a rack pinion, with a collector shoe under it on the conductor rail *(unverified)* |
| `mine_car` | 1 x 2 x 2 | Granby-type side-dump car, **1.5 m3, 3 t of rock** (approved by Tim, 2026-11-02). It carries any mix of rock and ore types, each counted in kg |

- **Capacity:** a car holds about 3,000 kg, about one block of ore (a uranium ore block is 2,300 kg; a rock-salt block 2,117 kg).
  It carries **any mix** of rock and ore types (decided with Tim, 2026-11-02), as real run-of-mine rock does; right-click shows
  its total load and the kg of each type.
- **Powered pickup:** the locomotive's shoe draws real current from the conductor rail under it: about 75 kW pulling a loaded
  train up the decline, less on the flat, a little at idle for its lights and compressor. The rectifier's breaker trips on
  overload, as any Wildcatter feeder does, and its readout shows the track's load. On unpowered track, or past the last powered
  section, the locomotive brakes to a stop and holds on the rack.
- **Safe track:** a section is switched live only under a locomotive; the conductor is shrouded and flush. A player, mob or item
  on the track never gets a shock. This is the one Wildcatter conductor that does not shock, and it is safe because its real
  counterpart is built to be.
- **Coupling and trains:** sneak-right-click with an empty hand couples a car to the one in front, up to **10 cars a
  locomotive** (approved by Tim, 2026-11-02): about 30 t of rock, 40 t with the cars, plus the 8 t locomotive. A train moves as one: it starts slowly (real tractive effort against the train's mass and the
  grade), keeps its spacing on curves, and stops as one. A car with no locomotive does not move by itself, except that it rolls
  downhill on the decline if its brake is off. A coupling breaks if the train pulls past its rating, and the cut cars roll
  back.
- **Train behavior:** the locomotive runs a set route between the loading and dump stations, out on the loaded track and back
  on the empty track. Block signals keep trains one block section apart. Speed is 3 m/s on the flat *(real mine locomotives
  run 2-4 m/s)*, slower on the decline.
- **Loading:** at the loading station a chute fills each car in turn from the drill's conveyor as the train creeps past.
- **Dump action:** at the dump station a curved dump ramp lifts each Granby car's wheel as it passes, tipping the body sideways
  onto the sorting grizzly without stopping the train, as real Granby cars do. A bottom-dump car (later) opens over it
  instead.

## The dump station and sorting (decided with Tim, 2026-11-02: cars carry mixed loads; sorting happens at the dump)
Real counterparts: a dump pocket over a grizzly, a picking belt, and sensor-based ore sorting (TOMRA X-ray transmission and
colour sorters, which reject waste rock from ore at mine sites).

| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `dump_pocket` | 4 x 3 x 3 | Concrete pocket under the dump ramp with a 300 mm grizzly; takes a car's load as it tips |
| `apron_feeder` | 2 x 1 x 4 | Apron feeder from the pocket to the sorter, 11 kW, 480 V |
| `ore_sorter` | 3 x 3 x 3 | Sensor-based sorter, 40 kW, 480 V *(unverified)*. Each rock type goes to its own output face |
| `ore_bin` | 3 x 6 x 3 | One steel bin per product (a waste-rock bin, one bin per ore type set in its GUI), feeding the crushing circuit or a hopper |

- **What it sorts:** every rock and ore type is counted in kg in the car, so the sorter sends each type's kg to its bin.
  Waste rock (cobble, granite, deepslate with no recoverable mineral set) goes to the waste bin, for backfill or the cobble slot.
- **Rate:** 50 t/h, matching the crushing circuit; a 10-car train of 30 t takes about 36 minutes to clear *(proposed; open
  whether a train waits or the pocket buffers it)*.
- **A full bin:** the sorter stops that output; when the pocket is full the next car does not tip and the train waits at the
  signal.
- **Rock types the crushing circuit treats differently** (granite vs cobble) stay separate, so each gets its own crushed products.

## Breaking and safety
- Breaking a track piece leaves the rest; a car at a gap derails and drops its load.
- The track never shocks (above). A player standing on the track in front of a moving train is struck by it, so players use the walkway
  between the tracks.

## Open questions
- Keep the 1-in-1 steep option in the GUI (which brings back the rack exception), or 1 in 2 only?
- Lining segment recipe and whether broken lining leaks.
- Sorter throughput: does the train wait to be sorted, or does the pocket buffer a whole train?
- Gravel under the track on the decline too, or only on the flat?
- Should the powered track also feed lights or pumps in the drive?
- ~~Chunk loading while the drill runs far from the player.~~ **Decided 2026-11-03:** the moving drill files a chunk ticket (NeoForge `TicketController`, Fabric the level force-load ticket) on its contraption entity for the cab chunk, the face, and the 3-block probe. The ticket drops when the drill stops. Trains get the same kind of ticket along their route.
- (Closed: caves, water and lava are handled by the lining and the pump; the remaining part is huge inflows, such as tunneling under an ocean.)
