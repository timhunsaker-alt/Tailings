# The crushing circuit (APPROVED by Tim 2026-11-05, R2)

A **two-stage crushing and screening circuit with a ball mill and cyclones**, as at a small base-metal concentrator. It takes
run-of-mine rock from the dump station's ore bins and makes flotation feed.
- The dry end moves rock by touching parts and belt conveyors.
- From the ball mill on, the ore is **slurry on Wildcatter's steel pipe**, carrying its mineral assay.

**Decided with Tim:**
- **2026-11-02:**
  - the nuggets rule (below);
  - vanilla iron, copper and gold join the same ladder.
- **2026-11-05 (R2):**
  - belts between the dry stages, slurry on pipe from the mill on;
  - the real 50 t/h;
  - belts are laid like pipe, with a drive head.

**Sources:**
- Wills & Finch, *Wills' Mineral Processing Technology*, 8th ed. (2016): crushers, screens, the Bond work index, hydrocyclones.
- Metso *Crushing and Screening Handbook*: C-series jaws, HP cones.
- SME *Mineral Processing Handbook* (1985).

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal.

```
ore bin -> belt -> GRIZZLY -> +150 mm -> JAW CRUSHER -> belt -> DOUBLE-DECK SCREEN -> -6 mm sand -> belt -> BALL MILL
                     \ -150 mm (bypass) ------------^           | +6 mm         ^                          | mill discharge
                                                                v               |                          v   (pipe)
                                                          CONE CRUSHER -> belt -+                   CYCLONE CLUSTER
                                                                                     underflow (touching) ^   | overflow
                                                                                                              v   (pipe)
                                                                                          fines slurry -> flotation bank
```

## Size: 50 t/h
- **Rock:** 50 t/h, **13.9 kg/s**, a small underground mine. A block of rock (2.7 t) passes in about 3 minutes 15 seconds.
- **Power check (Bond's law):** W = 10 Wi (1/√P80 − 1/√F80) kWh/t, P80 and F80 in µm.
  - Granite has Wi about 15 kWh/t. Cobble is taken as andesite-like, 18 *(proposed)*.
  - The mill's share, from 3 mm sand to 150 µm at Wi 15: W = 150 × (1/√150 − 1/√3000) = 9.5 kWh/t, so 475 kW at 50 t/h.
    The 600 kW mill covers it.
- **Slurry *(proposed, typical values)*:**
  - the mill discharges at 70 % solids, with 0.43 L of water a kg of ore (6.0 L/s);
  - the cyclones run with a 250 % circulating load;
  - the overflow is flotation feed at 35 % solids, with 1.86 L of water a kg of ore (25.8 L/s), off a water pipe at the mill and
    the cyclone pump box.

## The ladder of sizes
| Product | Size | Made by | Goes to |
|---|---|---|---|
| Crushed rock | 40-150 mm | jaw | the screen |
| Gravel | 6-40 mm | cone, screened | back to the cone with the oversize (closed circuit), or out as an aggregate item if a hopper or belt is on the gravel face *(proposed)* |
| Sand | 0.15-6 mm | screen undersize | the ball mill by belt, or the hand line's shaking table (the 2x route) |
| Fines | under 150 µm | ball mill and cyclone overflow | flotation, as fines slurry on pipe (the 3x route) |

- **Liberation:** the crushing circuit itself recovers nothing; it **liberates**. Liberation is about 60 % at sand and 90 % at fines
  *(proposed; real values depend on grain size)*. The ladder's recoveries (VISION.md) are already written against these.
- **What a belt or pipe carries:**
  - the dry stages carry kg of each rock and size class (for example "andesite, crushed");
  - the slurry carries kg of each mineral in its assay.
  - Items exist only where the player hands rock in or takes a product out.

## What each rock carries *(proposed: crustal abundance × 10, as Wildcatter's ores.md scales grades)*
| Rock | Main minerals | Recoverable trace (game) |
|---|---|---|
| Cobblestone / stone | quartz, feldspar | iron nuggets (magnetite), sphalerite and galena nuggets, rarely |
| Granite | quartz, K-feldspar, mica | zircon, monazite, cassiterite, a little molybdenite |
| Diorite | plagioclase, hornblende | magnetite, ilmenite |
| Andesite | plagioclase, pyroxene | copper sulfides (porphyry), trace gold |
| Deepslate | as stone, deeper | pentlandite, chromite traces |
| Tuff | volcanic glass | zeolite, a little uranium |
| Sandstone | quartz | heavy mineral sand, roll-front uranium |
| Wildcatter ores | as ores.md | their ore mineral at the ores.md game grade |
| Vanilla iron, copper, gold ores (and raw ore items) | magnetite or hematite; chalcopyrite; native gold | the metal for 1 to 4 ingots per ore, by the stage ladder (VISION.md) |

### Nuggets (decided with Tim, 2026-11-02)
Processing ordinary rock (cobble, stone, granite, diorite, andesite, deepslate, tuff, sandstone) yields **ore nuggets** of the
minerals each rock really holds, from the table above.
- **The item:** one nugget per mineral, such as `tailings:zircon_nugget`. Vanilla's iron and gold nuggets are used where they
  exist.
- **Size:** a nugget is 1/9 of that mineral's item by kg of mineral, as vanilla nuggets are 1/9 of an ingot. Nine nuggets craft one
  mineral item.
- **How many:** the plant keeps a running kg total of each trace mineral and puts out a nugget each time a nugget's worth is
  recovered. Recovery follows the same ladder as ores (gravity 50 %, flotation 75 %, full 95 %), so no rock ever yields more than
  its written mineral list.
- **Which stage:** the stage that recovers the mineral:
  - gravity for cassiterite, zircon, monazite and magnetite;
  - flotation for sphalerite and galena.

  The crushers alone give no nuggets.

## A layout that wires (found by the R10 playthrough)
A part's motor box needs its west cell free. So the screen stands clear of the jaw, with a short belt off the grizzly's east face; the cyclones stand on the mill's west face, not its east; the blower on the bank's west. `Playthrough.java` holds a full working layout.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `grizzly` | 3 x 2 x 4 | A static bar grizzly with a 150 mm gap, set over the jaw's feed opening. No motor. It passes −150 mm rock past the jaw onto the jaw's discharge belt, and drops +150 mm into the jaw it touches |
| `jaw_crusher` | 3 x 4 x 4 | A C100-class jaw: **110 kW at 480 V**, closed-side setting 100 mm. It touches the grizzly above its feed opening |
| `double_deck_screen` | 3 x 3 x 6 | A 1.8 x 4.8 m inclined screen with 40 mm and 6 mm decks: **22 kW at 480 V**. It discharges +6 mm off its top deck end and −6 mm sand from under it |
| `cone_crusher` | 3 x 4 x 3 | An HP200-class cone: **132 kW at 480 V**, closed-side setting 13 mm. It takes the screen's oversize by belt and returns its product to the screen by belt |
| `ball_mill` | 4 x 4 x 6 | A 3.2 m x 4.5 m overflow ball mill: **600 kW at 4.16 kV**. Sand in at its feed end by belt, water off a water pipe, and the cyclone underflow from the cluster it touches. Mill discharge slurry out on pipe |
| `cyclone_cluster` | 2 x 4 x 2 | Four 250 mm hydrocyclones with a 150 µm cut point *(proposed count)*, on a pump box with a **45 kW, 480 V** slurry pump. Mill discharge in on pipe, overflow (fines slurry) out on pipe, underflow back into the mill it touches |
| `belt_conveyor` | 1 x 1 x 1 per block, a run of any length | A 600 mm troughed belt at 2 m/s on idlers. Laid block by block like a pipe run, along one axis or up a grade of at most 1 in 3 *(proposed; real belts climb about 18°)* |
| `conveyor_drive` | 1 x 1 x 2 | The head pulley and drive of one run: **7.5 kW at 480 V for each 30 m of run, rounded up** *(proposed)*. A run with no drive, or with too little drive for its length, does not move |

**New pipe products (in the Wildcatter proposal):**
- `mill discharge`, in kg;
- `fines slurry`, in kg.

Each carries its assay.

## Layout (a sketch, facing north; fixed with the models in R3)
Plan, bottom layer (y 0). The circuit is a set of parts placed by the player, not one fixed structure. This is one layout that
works:
```
 x ->  0         5         10        15        20
 z 0   . . . = = = = = = = . . . . . . . . . . . .     = belt_conveyor (arrow shows travel)
   1   . . . = J J J . . = . . . . . . . . . . . .     D conveyor_drive at the head end of each run
   2   . . . = J J J . . = S S S . . . . . . . . .     (the grizzly sits on top of the jaw, y 4-5: see the elevation)
   3   . . . = J J J . . = S S S . . . . . . . . .     J jaw_crusher
   4   . . . D J J J . . D S S S = = = = D . . . .     S double_deck_screen
   5   . . . . . . . . . . S S S . . . . = . . . .     C cone_crusher
   6   . . . . . . . . . . S S S = C C C = . . . .     M ball_mill
   7   . . . . . . . . . . S S S D C C C . . . . .     Y cyclone_cluster (touching the mill's feed end)
   8   . . . . . . . . . . . = . . C C C . . . . .     ~ steel pipe (mill discharge, fines slurry)
   9   . . . . . . . . . . . = . . . . . . . . . .
  10   . . . . . . . . . . Y Y M M M M . . . . . .
  11   . . . . . . . . . . Y Y M M M M ~ ~ to flotation
 ...                         (the mill runs 6 cells south)
```
Elevation of the jaw and grizzly, looking west:
```
 y 6   = = = =          the feed belt from the ore bin delivers onto the grizzly
 y 5   G G G G          G grizzly (3 x 2 x 4) on top of the jaw's feed opening
 y 4   G G G G  =       -150 mm falls through the bars and out of its east face onto a belt down to the jaw's discharge belt
 y 3   J J J J
 y 0   J J J J  = = =   J jaw (3 x 4 x 4): +150 mm is crushed and leaves its south face onto the belt
```

## Nozzles and faces (each part drawn facing north; main cell at the north-west bottom corner)
- **Motor boxes:**
  - on the west face of every powered part's main cell;
  - the ball mill's at **4.16 kV**; every other one at **480 V**;
  - a `conveyor_drive`'s on its west face.
- **Grizzly:** fed from a belt over its top. It drops +150 mm into the jaw under it, and passes −150 mm out of its east face onto
  a belt.
- **Jaw:** crushed rock out of its south face onto a belt.
- **Screen:**
  - fed by a belt over its north (feed) end;
  - +6 mm out of its south face onto a belt;
  - −6 mm sand out of its east face onto a belt, or to a hopper under it.
- **Cone:** fed by a belt over its top; its product out of its south face onto a belt.
- **Ball mill:**
  - sand in by a belt over its north (feed) end;
  - water off a water pipe on any face;
  - mill discharge out on pipe at the south face of its back cells (z 5, bottom layer).
- **Cyclone cluster:**
  - mill discharge in on pipe at its north face;
  - water off a water pipe on any face;
  - fines slurry out on pipe at its south face;
  - its underflow goes into the ball mill it touches at the mill's feed end.

## How it runs
- **Every powered part** runs its own motor while it has work and somewhere to put its product. Its rate follows its motor's
  output, up to its share of 50 t/h. There is no controller: each part is its own, as in Wildcatter's uranium mill.
- **Belts:**
  - a belt moves only while its drive's motor runs;
  - it holds about 30 s of its run's flow *(proposed)* and stops taking when full;
  - it shows the lumps it carries.
- **The screen and cone** are a closed circuit:
  - the oversize goes round until it passes 6 mm;
  - a 6-40 mm gravel face with a hopper or belt on it takes gravel out as aggregate instead.
- **The ball mill** runs while it has sand and water and its discharge pipe has room. It takes the cyclones' underflow back first,
  as a real closed circuit does.
- **The cyclone cluster** splits the mill discharge at 150 µm: overflow to the fines slurry pipe, underflow back to the mill.
- **What each part reads when it stops:**
  - NO FEED;
  - NO WATER;
  - NOT BUILT (the part it must touch is missing or facing the wrong way);
  - NO DRIVE (a belt);
  - FULL;
  - "its output has nowhere to go".
- **Removing a part** stops the flow through it. What it holds stays in it until it is broken, and then drops (R1).

## Left out (ROADMAP)
- Real size distributions inside a size class (P80 only).
- Screen blinding, liner and media wear, steel ball consumption.
- A plant-scale gravity circuit: now [gravity-circuit.md](gravity-circuit.md).

## Game test
`crushingCircuit` (Fabric client game test):
- **Power:**
  - a genset at 4.16 kV for the ball mill;
  - a unit substation to 480 V for the other motors.
- **Feed:**
  - cobble and copper-ore blocks put into an ore bin by a hook;
  - water off a test-filled tank.
- **Checks:**
  1. The jaw, screen, cone and belts run, and the screen's sand face delivers sand to the mill.
  2. Mill discharge flows to the cyclones.
  3. Fines slurry reaches a stock tank on the overflow pipe, and its assay shows the copper ore's chalcopyrite.
  4. With the ball mill's breaker open, the mill stops and the screen's sand belt fills and stops (FULL).
- **Screenshot:** the circuit.
