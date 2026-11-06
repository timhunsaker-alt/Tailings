# The tunnel boring machine (APPROVED by Tim 2026-11-05, R2)

A **programmable, track-laying tunnel boring machine (TBM)**. It drives a decline down to a set Y level, then a flat drive. It lines
the bore with a ring of segments as it goes, lays the floor and **two parallel tracks** behind it, and sends its muck back to a mine
car at its tail. Haulage, the trains and the dump are in [haulage.md](haulage.md) and [dump-station.md](dump-station.md).

**Real counterparts:**
- Small hard-rock TBMs: the Robbins Main Beam TBM, single-shield segmental TBMs (Herrenknecht, Robbins), and the Rio Tinto /
  Sandvik Mobile Tunnel Miner for mine drives.
- Their **back-up system**: gantries on rails behind the machine, carrying its transformer, cable reel, pumps and muck conveyor.

**Decided with Tim:**
- **2026-11-02:** the 6 x 6 lined bore, the 1-in-2 decline, the walkway, the pump and the cab's screen, below.
- **2026-11-03:**
  - the pump's surplus water is voided;
  - the TBM pauses when its muck buffer is full;
  - chunk tickets.
- **2026-11-05:**
  - **The contraption:** it moves like a Create contraption or a real boring machine. While it runs, its parts lift into one
    moving entity, and they set back down as blocks when it stops.
  - **The full real machine:** every part below is a true-size big part.
  - **On the decline it stays a contraption:** a rigid 8 m main beam cannot sit as blocks on a 1-in-2 grade. Stopped there, it
    parks as the entity, drawn pitched to the grade, still clickable and saved with the world. It sets down as blocks only on the
    flat, or when it is dismantled.
  - **The back-up gear rides gantries on the new rails:** the transformer and cable reel, the dewatering pump and the muck
    conveyor run on the twin track it has just laid, as a real TBM's back-up does. So they follow the grade and the turn onto the
    flat.
  - **Chunks:** the `tailings:runner` ticket (R1).

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* came from the
first draft and is still to be checked.

```
 side view, running north (heading to the left), on the flat:

   ┌cutterhead┐┌erector┐┌──────────── main beam (cab at its tail) ───────────┐
   |##########||#######||   ====================================  [cab]     |      ┌──gantry──┐┌──gantry───┐┌──gantry──┐
   |##########||#######||   ====================================             |      |  pump    || reel and  ||  muck    |
   |##########||#######||           [ track layer ]                          |      |  + tank  ||transformer|| conveyor |
 ==face=======ring=======floor + rails laid here==================================o==o==========o==o=========o==o======== -> mine car
    slice 0    1  2   3 (ring erected at the shield's tail)                        (the gantries ride the twin track)

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
  row make one block of rise over two of run: a smooth 26.6° ramp. A `steep` variant (0 to 1 in one block) stays only
  for rails an old world already laid (decided with Tim, R6: 1 in 2 only).
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
  written impossibility. The old "45° on a rack" game exception is dropped.
- **Length:** a 1-in-2 decline needs 2 blocks of run per block of depth, so reaching y -40 from y 64 takes about 208 blocks.

## The pump and keeping the drive dry (decided with Tim, 2026-11-02)
The machine's **dewatering pump** (on the `pump_gantry`, below) clears every liquid source block it meets in or beside the
bore, before the lining closes. The lining then keeps the drive dry. Flowing liquid is shut off with its source.

| Liquid | What the drill does | Cost |
|---|---|---|
| **Water** (source or flowing) | Pumps it into the drill's **water tank** (256 m3). That tank is what quenches lava. **Water past a full tank is voided** (revised with Tim, 2026-11-03; the tank is real, the surplus is not piped to surface) | Real lift work: E = ρ g V H / η. 1 m3 lifted 100 m at 70 % efficiency is 1.4 MJ (0.39 kWh). The pump draws its power from the trailing cable |
| **Lava** (source or flowing) | It cannot be pumped. The drill **quenches** it: water sprayed from its own tank turns a lava source to obsidian and flowing lava to cobble, which it then cuts as rock and sends to the cars (obsidian goes out as muck). A cutterhead cannot cut molten rock, so it waits while it quenches | 1 m3 of water per lava block *(proposed)* from the quench tank, plus 20 s of the pump's time. If there is no water the drill stops and says why |
| **Other mods' liquids** | Pumped like water if the loader says the fluid is not hot (temperature under 100 C); otherwise treated like lava; if it cannot tell, it stops and asks | As water or lava |
| **Caves and air gaps** | The track layer fills open space under the floor with cobble, and the lining closes the walls and roof as usual, so a cave is sealed off from the drive | Cobble from the inventory |

- **Where the water goes:** pumped source blocks fill the quench tank first. Anything the tank cannot hold is deleted.
  Quench water is that tank (buckets can still top it up). The power cost is still charged as if the water were lifted.
- **Probing:** each advance the drill checks 3 blocks ahead of the face for liquid (as real probe holes do) and pumps or
  quenches before cutting.
- **Pump size:** a 75 kW submersible-class dewatering pump at 480 V, about 30 L/s at 100 m head *(unverified)*, so a source block
  clears in about 35 s. Big inflows (an aquifer, an ocean above) can outrun it; the drill stops if water gains on it.

## Parts
### The machine (one contraption while it runs)
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `cutterhead` | 8 x 8 x 1 | A disc-cutter head in its shield, cutting the 8 x 8 face that the lining reduces to a 6 x 6 drive *(a square face stands in for a round bore)* |
| `segment_erector` | 8 x 8 x 2 | The tail shield and ring erector: it places one ring of 28 lining segments at its tail for each block of advance |
| `main_beam` | 2 x 2 x 8 | The thrust cylinders, grippers and cutterhead drive, with the **operator cab** at its tail (the controller). **1 MW at 4.16 kV** *(unverified, small TBM class)*. It runs down the middle of the 6 x 6 bore, 2 blocks above the floor |
| `track_layer` | 6 x 1 x 3 | Hangs under the rear of the main beam, 1 block above the floor. It lays the floor pattern under itself and the mine rail on columns 2 and 5, from the cab's inventory |

### The back-up (gantries on the twin track)
Each gantry is placed on the two tracks behind the machine, as a car is placed on the rail, and coupled to it:
- **Coupling:** sneak-click with an empty hand.
- **Order behind the machine:** pump, reel, muck conveyor. The conveyor is last, so the car it loads stands on clear track behind it.
- **Movement:** they are entities on the rail, like cars (haulage.md), towed by the machine.

| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `pump_gantry` | 6 x 4 x 3 | A gantry carrying the **dewatering pump**: 75 kW at 480 V, about 30 L/s at 100 m head *(unverified)*, with its **256 m³ water tank**, drain pipes and quench sprays (below) |
| `conveyor_gantry` | 6 x 4 x 8 | A gantry carrying the **muck conveyor**: a 600 mm belt from the cutterhead's muck ring back to the tail, **7.5 kW at 480 V**. It drops the muck into the mine car standing under its tail, at the loading point |
| `reel_gantry` | 6 x 4 x 4 | A gantry carrying the **trailing cable reel** and a **4.16 kV to 480 V, 150 kVA transformer** for the pump and conveyor *(proposed)*. The reel pays out 4.16 kV cable as the machine advances (below) |

**Power:**
- The reel's fixed end is wired to a Wildcatter 4.16 kV cable at the point where the machine was started. That point is a
  `cable_junction` block, a terminal box placed there *(proposed)*.
- The reel holds **300 m of cable** *(proposed; real reels hold 300-500 m)*. More cable items in the cab's cable slot add length.
- The main beam draws its 1 MW through the reel. The pump and conveyor draw through the gantry transformer.
- When the cable is all paid out, the machine stops: "cable end".

## The cab's screen and inventory (decided with Tim, 2026-11-02)
Right-click the operator cab (on the main beam, as blocks or as the moving contraption) to open its screen.
- **Program:** target Y; heading; flat-drive length; stop conditions (ore seen, cable end, liquid it cannot clear, inventory
  empty). The drill drives the decline down at 1 in 2 to the target Y, then runs flat.
- **Inventory slots**, which the drill uses as it advances:
  - **Mine rail**: two a block of advance.
  - **Gravel**: ballast under the track, one per track block *(proposed)*, on the decline and the flat (decided R6).
  - **Cobble**: fill for caves and gaps under the floor, and a fallback stock for the floor pieces.
- **The drill makes its own floor pieces** (decided with Tim, 2026-11-02): slope pieces, stairs and landings are cut by the
  track layer from cobble or any suitable rock it has just mined (stone, cobble, deepslate, granite, diorite, andesite, tuff,
  sandstone), one rock block a piece. The player never crafts them. It never uses ore, gravel, sand or dirt; those go to the
  cars. The pieces take the look of the rock they are cut from. If the mined rock runs short (in a cave or ore body) it draws
  on the cobble slot, and stops when that is empty too.
  - **Lining**: 28 blocks a ring. The default ring is cobble or any stone the drill has just mined or is holding (stone, cobble, deepslate, granite, diorite, andesite, tuff, sandstone). **Crafted lining segments** (4 gray concrete + 1 iron = 4) are an optional cleaner ring, used when a full ring of them is in the slot.
  - **Water** (a bucket slot): water buckets top up the quench tank on the pump gantry. The machine moves, so it has no pipe.
- The screen shows the floor-piece stock (mined rock held back for pieces), each slot's count and how many blocks of drive it lasts. When a slot runs out the drill stops and says why.
- **Readout:** depth, Y, blocks driven, power draw, cable left, water pumped.

## Building and starting it
- **Where:** the player builds the machine as blocks on level ground, facing the heading:
  - the cutterhead touching the erector;
  - the erector touching the main beam's front;
  - the track layer under the beam's rear.

  At least a few blocks of the player's own twin mine rail run back from the track layer; the gantries go on that rail, coupled in
  order.
- **Start:** the cab checks:
  - every part is present, touching in order and facing the heading;
  - the gantries are coupled;
  - the cable junction is wired.

  It reads what is missing: NOT BUILT (names the part), NO CABLE, and so on. Then the machine's parts **lift into one moving
  entity**. Their cells become air, and the entity draws their models in place.

## The contraption while it runs
- **Motion:** it glides forward continuously at the advance rate: 1 block per 20 s in stone, slower in deepslate *(proposed)*.
  As the cutterhead's face passes each slice, the machine:
  1. probes 3 blocks ahead and pumps or quenches (below);
  2. cuts the 8 x 8 face and sends each block cut to the muck buffer;
  3. erects the ring at the erector's tail;
  4. lays the floor and track under the track layer.
- **On the decline:** the entity is drawn pitched to the 1-in-2 grade, and its collision follows it.
- **Riding:** players and mobs can stand on it and ride it, as on a Create contraption. Its collision is the parts' real shapes.
- **The muck buffer:**
  - the main beam holds 18 stacks of muck;
  - the conveyor gantry hands them into the car under its tail, up to 64 items a tick;
  - with the buffer full, the machine **pauses**. It does not latch off; it goes on when a car takes the muck (decided 2026-11-03).
- **The gantries:** they are towed along the new track, so a gantry's wheels are always on rail the machine laid.
- **Chunks:** the entity and each gantry refresh the `tailings:runner` ticket on their own chunks and on the face's.
- **Saving:** the world saves the contraption as the entity, with every part's contents, the tank, the program and its progress.

## Stopping
- **On the flat:** it sets its parts back down as blocks where they are, and its cells must be free. If something is in the way
  (a block a player put in the bore, say), it stays a contraption and reads "can't set down: blocked at x, y, z".
- **On the decline:** it parks as the contraption (decided 2026-11-05). It is still clickable: its cab's screen works and its
  readout shows.
- **Starting again:** it starts from wherever it is, blocks or contraption.
- **Dismantling:** sneak and right-click the cab with an empty hand while it is stopped. Every part drops as its item and every
  slot's contents drop, blocks or contraption alike *(proposed)*.
- **Stops it reads:**
  - target reached;
  - ore seen;
  - cable end;
  - a liquid it cannot clear;
  - out of rail, lining, gravel, cobble or quench water;
  - buffer full (a pause);
  - blocked (a solid block in the way of the contraption that is not rock it can cut: another machine, say).

## Nozzles and faces
- **Motor boxes:** none on the machine itself. It is fed through the reel's trailing cable and the gantry transformer (above).
- **Cable junction:** a 4.16 kV Wildcatter cable lands on any face of the `cable_junction` block.
- **Water:** the quench tank fills from the pump, or from water buckets in the cab's water slot. There is no pipe nozzle: the
  machine moves.

## Removing a part
- **As blocks, on the flat:** breaking any part breaks that part and drops its item and contents (R1). The machine reads NOT BUILT
  until the part is back.
- **As a contraption:** its parts cannot be broken one by one; it is dismantled whole from the cab (above).
- **A gantry:** breaking it uncouples it and drops it. The machine stops, reading the missing gantry.

## Left out (ROADMAP)
- A round bore: the face is square.
- Cutter wear and disc changes; ground support beyond the segment lining; grouting; probe drilling as its own machine.
- Ventilation of the drive (Later, not scheduled).
- Huge inflows (tunnelling under an ocean): the machine stops if water gains on it.

## Questions from the first draft (all decided with Tim, 2026-11-05, R6)
- **1 in 2 only:** the 1-in-1 steep option leaves the screen; it would have brought back the rack exception. Steep rails already
  laid in an old world still run.
- **Broken lining does not leak:** a broken segment just opens the wall.
- **Gravel ballast on the decline too:** one gravel under every track block, decline and flat.

## Decided with Tim for R6 (2026-11-05)
- **Three waves:** R6a the parts as blocks, R6b the contraption, R6c the gantries.
- **Riding is a seat in the cab:** a Ride button on the cab's screen seats the player, who rides as in a minecart until they sneak.
  Standing on the moving machine (Create-style collision) is left for later (ROADMAP Known gaps).

## Added while planning R6
- **The `cable_junction`** is the machine's power point, and the cab counts the cable paid out against 300 m plus cable items.

## Added while building R6c
- **Rows behind the cutterhead:** the track layer lays the last rail at row 11; the pump gantry stands on rows 12 to 14, the reel on 15 to 18, the conveyor on 19 to 26. So the player's twin rail must run at least 16 rows back from the track layer before starting.
- **The loading point** is the five rows just behind the conveyor's tail, on both tracks. A train waits there while a car has room and the machine has muck.
- **A train stops short of a gantry** and turns back there, as at the end of the line.
- **Coupling** is from within 8 blocks of the gantry's place, in order, and snaps it into place. It works on the machine as blocks, running or parked.
- **Power:** 1 MW for the beam, 75 kW more while the pump pumps or quenches, 7.5 kW more while the conveyor moves muck, all through the junction.
- **A gantry gone, or the track under one,** reads NOT BUILT and names it. The machine keeps its Go and carries on once it is put right.

## Added while building R6b
- **Only the contraption drives.** Started and whole, with its junction live, the machine lifts before it cuts anything: the blocks never step.
- **Each advance cuts first, then glides** into the cut for the advance time (20 s a block in stone, 30 s in deepslate), and lines and floors behind itself when it arrives.
- **Pitch** comes from the floor's height under the cutterhead and 11 blocks behind it, so the machine eases onto the grade and off it.
- **The bore it moves through** must be clear of anything solid (columns 1 to 6, 2 to 6 up, along the beam and the track layer). If not it stops as blocked before cutting.
- **On the flat** means the floor is level under its whole length; until then it parks and its cab says so.
- **The seat** is on the cab's roof at the main beam's tail. The Ride button only works on the running or parked machine; setting down puts the rider off.

## Game test
`tbm` (Fabric client game test):
- **Setup:**
  - a stone test volume;
  - the machine built as blocks on the flat, with its three gantries on a short twin track;
  - a cable junction fed at 4.16 kV;
  - rail, cobble, lining and water in the cab.
- **Checks:**
  1. Started, the parts become one contraption: their cells are air and one TBM entity draws them.
  2. It drives 4 blocks: four rings of 28 segments, the floor pattern and two tracks behind it, with the gantries following on
     the new track.
  3. The muck reaches a car under the conveyor gantry's tail.
  4. A player standing on the beam rides forward with it.
  5. Stopped on the flat, it sets down as blocks. Restarted on a 1-in-2 program, it reaches the decline. Stopped there, it parks
     as the contraption and its cab screen still opens.
  6. Dismantled, it drops every part and the slots' contents.
- **Screenshot:** the machine running on the decline.
