# Open pit P3: the blasthole drill, the emulsion truck, boosters and detonators, the blast

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** The choices are listed in
[../questions/specs-tailings.md](../questions/specs-tailings.md) (P3 section).

This fills in open-pit.md's P3 machines ("Rotary blasthole drill", "Emulsion truck", "The blast") so they can be coded without
questions. Where it changes or narrows the approved spec, the change is listed in the questions file, not made in open-pit.md.

**Real counterparts:**
- A crawler rotary blasthole drill of the 271 class: 270 mm holes, single pass to 16.8 m.
- A bulk emulsion truck (mobile manufacturing unit, MMU) pumping matrix into the hole and gassing it there.
- Cast boosters of TNT and its mixes; electronic delay detonators with a logger and a blasting machine.
- SME *Mining Engineering Handbook*, 3rd ed. (2011), surface blast design; the ISEE *Blasters' Handbook* (18th ed.).

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

```
 DRILL walks the pattern -> blasthole columns (cuttings kept at the collar)
   -> drop a BOOSTER + DETONATOR in each hole (primed)
   -> EMULSION TRUCK pumps matrix to the set column, gassed in the hole (charged)
   -> stem the rest with the cuttings (stemmed)
   -> LOGGER: click each hole (delays), then the BLASTING MACHINE (logged)
   -> clear 50 m; key in; siren 10 s; FIRE -> rock to muck, 4 for 3, spilled off the free face
```

## The bench and the pattern (defaults)
| | Default | Basis |
|---|---|---|
| Bench height | **14 m** | the rope shovel's 15 m reach (open-pit.md, Loaders); a whole number of blocks *(proposed)* |
| Subdrill | **2 m** | real 0.2-0.5 x burden; 2 m is 0.29 x 7 m *(proposed)* |
| Hole | **270 mm**, **16 m** deep (single pass, under the class's 16.8 m) | 271-class drill (Sources) |
| Burden x spacing | **7 x 8 m** | 26 and 30 hole diameters, hard rock *(proposed; the textbook range is 25-40 d)* |
| Stemming | **4 m** | the approved default *(proposed)*; textbook 20-30 d is 5.4-8 m: see Decision P3-4 |
| Charge column | **12 m**, at 69 kg/m, **828 kg** | 1.2 g/cm³ in a 270 mm hole |
| Rock broken a hole | 7 x 8 x 14 m = **784 m³ bank, 2,117 t** | 2.7 t/m³ (`PlantRates.BLOCK_KG`) |
| Powder factor | **0.39 kg/t** | real hard-rock pits run 0.2-0.5 kg/t |

The screen lets a player change every one of these within: bench 6-15 m; subdrill 0-3 m; burden 4-10 m; spacing 4-12 m;
stemming 2-8 m *(proposed limits)*. The hole depth is bench plus subdrill and may not exceed 16.8 m (one pass).

## The rotary blasthole drill (`blasthole_drill`)

### Real figures
| | |
|---|---|
| Hole | 171-270 mm (270 mm is the game's only bit) |
| Single pass | **16.8 m**; 32 m with rod changes (left out: single pass only) |
| Weight on bit | **34 t** (75,000 lb) |
| Power | **diesel in P3** *(decided: the pit switch house comes in P5)*: a C27-class engine, about **600 kW** *(unverified)*. Electric, **522 kW** off a trailing cable, from P5 (a screen choice once a cable reaches it) |
| Drilling rate | real about **43 m/h** scheduled for a 270 mm bit (AusIMM comparison); the game **1 m every 10 s** (360 m/h, *scaled about 8x*, as approved) |
| In deepslate | **1 m every 15 s** *(proposed, the TBM's 600/400 ratio)* |
| Set-up at each hole | levelling jacks down, mast plumb: **20 s** *(scaled; real 1-2 min)* |
| Tramming | **1.5 km/h** *(proposed, the shovels' crawl)* |
| Size | about 7 x 15 m on its crawlers, **26 m** to the mast top *(unverified)* |
| Fuel | tank **1,500 L** *(proposed, the loader's)*; burns by work as `PitFuel`: drilling at **70 %** of rated power, tramming at **40 %**, idle 3 % *(proposed)* |

### Parts (big parts, then a contraption, like the TBM and the rope shovel)
| Part | Cells (W x H x L) | Real component |
|---|---|---|
| `drill_crawlers` | 7 x 2 x 10 | The two crawler tracks on their frame, with the three levelling jacks. Its main cell holds the machine's block entity: status, screen, pattern, fuel |
| `drill_deck` | 7 x 4 x 15 | The main frame with the engine (or the electric motor), the screw compressor for the flushing air, the hydraulic pumps, the operator's cab at the front corner, and the cable reel stand for P5 |
| `drill_mast` | 3 x 20 x 3 | The mast with the rotary head, the pull-down chains, the drill pipe and the 270 mm tricone bit, standing at the deck's back end over the hole |
| `drill_dust_collector` | 2 x 3 x 3 | The dust collector and the deck bushing's skirt round the collar *(real; it is what makes the cuttings pile)* |

- **Built and checked** as the dragline is: the crawlers read NOT BUILT, naming the part, until the deck sits on them, the mast
  stands at the deck's back end and the dust collector sits beside the mast's foot.
- **The contraption:** started, it lifts into one entity (multiblocks README, The contraption). It is drawn with the mast up.
  Stopped, it sets down if the cells are free.
- **The hole is under the mast's foot**, 1 m behind the deck. Its position is what the pattern counts.

### Running a pattern
- **Its screen sets** the pattern's origin (the first hole: the block the mast stands over when GO is pressed), the direction of its
  rows (the way the drill faces), **rows x columns** (up to 10 x 20, *proposed*), burden, spacing, bench height and subdrill. The
  rows run back from the free face: row 1 is the front row. Set as **staggered** (each other row offset by half a spacing, *real
  practice*) or square *(staggered is the default)*.
- **GO** walks the pattern row by row, a serpentine: tram to the hole's spot, jacks down, drill, jacks up, on to the next.
  Each hole takes `depth x rate + 20 s + tramming`: about **3 min 20 s** for a default hole 8 m from the last.
- **A drilled hole** is a column of `blasthole` blocks from the collar down to its depth. Each `blasthole` block **remembers the
  rock or ore it replaced** (so the break counts every block of the shot, and nothing is lost). The collar block carries the hole's
  state (below) and its cuttings: **0.92 m³** for 16 m *(the hole's volume)*, drawn as a small cone at the collar, enough for
  **3.4 m** of stemming *(proposed: 0.92 m³ less 25 % lost to the dust collector, bulked 1.0)*. Short of the set stemming, the player
  tops it up with gravel (1 block = 13 m of hole, *volume*).
- **What a hole goes through:** stone, ores, soil. A hole that meets air (a cave), water, lava, bedrock or a block entity stops
  there and is marked SHORT, with its depth. Water in a hole is fine (emulsion is water-resistant, the reason it is chosen).
- **It stops** and reads why: DONE, NOT BUILT, OUT OF FUEL, CABLE END (P5), BLOCKED (something in the way of tramming: it does not
  drive round), NOT LEVEL (the ground under the crawlers is more than 1 block out of level).
- **Drilling on a muck pile** is refused ("not on broken ground"); real drills stand on the bench's solid floor.
- **PLC points** (a Wildcatter `ControlDevice` at the crawlers' main cell): RUN, RUNNING, HOLES DRILLED (pulse), DONE, FUEL LOW.
- **Driving by hand:** a player in the cab drives it with the keys like a pit vehicle (1.5 km/h) and drills one hole where it stands
  with jump. The pattern is for most of its life *(proposed, as the dragline)*.
- **Sound:** the diesel loops (Wildcatter's), and a new `drill_rotary` loop while drilling.

## The hole's states
`DRILLED` → `PRIMED` → `CHARGED` → `STEMMED` → `LOGGED` → fired, or `MISFIRED`.

| State | How | Reads (sneak-right-click the collar) |
|---|---|---|
| DRILLED | the drill | depth, rock at the toe |
| PRIMED | right-click the collar with a `cast_booster`, then a `detonator` (either order; both needed) | "primed: booster, detonator #ID" |
| CHARGED | the emulsion truck | kg, column, "sleeping" until the matrix has gassed (below) |
| STEMMED | right-click with a stack of the hole's cuttings (on the collar, free) or gravel | stemming m |
| LOGGED | the logger | its delay in ms |

- A hole charged without a primer reads **"no primer: it will not fire"**. Real: a column of emulsion needs a booster to initiate.
- A hole can be **unloaded** before it is stemmed: a water bucket on the collar **washes out** the emulsion *(real practice for a
  misfire)*; the booster and detonator come back as items.
- **The collar's look:** a coloured tag on the collar for each state *(a placeholder art)*: white drilled, yellow primed, orange charged,
  grey stemmed, green logged, red misfired.

## Boosters and detonators (decided by Tim: made with TNT)
| Item | Real | Recipe *(proposed)* |
|---|---|---|
| `cast_booster` | A **400 g** cast primer (real boosters are TNT, pentolite or TNT mixes, 340-450 g) | 1 vanilla TNT → 4 (shapeless) |
| `detonator` | An electronic delay detonator, programmable 0-15,000 ms *(unverified range)*, on a 20 m downline. Its base charge is really PETN: TNT stands in, **a written exception** | 1 TNT, 1 copper ingot, 1 redstone (the electronics, placeholder) → 8 |
| `blast_logger` | A hand-held logger that reads each detonator's ID and writes its delay | 1 copper ingot, 1 redstone, 2 iron ingots (placeholder) |
| `blasting_machine` | The blasting machine: a box with a key switch, ARM and FIRE, on the surface lead line | a block; placeholder recipe as the plant's |
| `blasting_key` | Its key; the machine does nothing without it | 1 iron nugget, 1 redstone (placeholder) |

- **They do nothing on their own.** A booster or detonator is not a block and does not explode in a fire or when dropped
  *(proposed: real boosters need a detonator; real detonators are a small hazard left out)*.

## The emulsion truck (`emulsion_truck`, a bulk explosives truck)
| | | |
|---|---|---|
| Carries | **12 t of emulsion matrix** | open-pit.md *(proposed; real MMUs carry 8-28 t)* |
| Matrix density | **1.35 g/cm³** before gassing *(unverified)*; **1.2 g/cm³** gassed in the hole | gassing makes the bubbles that sensitise it |
| So the tank | **8.9 m³** of matrix | 12 t / 1.35 |
| Pump | real **up to 300 kg/min** (a 300 kg/min-class MMU); the game **1,200 kg/min, 20 kg/s** *(scaled 4x, as the drill is)* | |
| Hose reach | **12 m** from the truck's hose reel at its back *(proposed)* | |
| Gassing solution | carried inside, counted with the matrix *(proposed)* | |
| Sleep time | **60 s** after charging before the gassed column is at full strength *(scaled; real gassing takes 30-60 min, unverified)* | fired sooner, the hole counts **half** its kg *(proposed)* |
| Truck | a 4x4 or 6x4 road chassis, **8.5 x 2.5 x 3.8 m**, about **300 kW** diesel *(proposed)*; tank 400 L | |

- **An entity like the haul truck** (`PitVehicleEntity`), driven by a player, with the same diesel by work, refuelled at a fuel bay.
  It does not run itself in P3 *(proposed; a self-running charge crew is left out)*.
- **Filling:** real trucks are filled by hose at a filling point beside the matrix tanks. A `matrix_fill_point` (1 x 2 x 1, a
  standpipe and hose, *proposed*) takes **emulsion matrix** (ammonia-plant.md) by Wildcatter pipe on any face, from a tank or IBC,
  and fills a truck stopped within 6 m at **1 t/min** *(proposed)*, as the fuel bay fills diesel.
- **Charging:** with jump, standing, it charges every **primed, uncharged** hole within its hose reach, nearest first, to each
  hole's set column (the hole's depth less its stemming). Each kg counts to the hole. A hole not primed is skipped, and the cab
  reads how many.
- **Its screen:** the column to charge (default: depth less 4 m), and "charge all in reach". It reads the matrix left.
- **Sound:** the diesel loops and a pump loop *(proposed: Wildcatter's pump loop)*.

## Tying in and firing
1. **Log:** a player with the `blast_logger` right-clicks each stemmed hole. The logger gives it a delay: **row x row delay + hole
   in row x hole delay**, defaults **row 65 ms, hole 25 ms** *(proposed: real surface patterns use 17-42 ms between holes and
   42-100 ms between rows, unverified)*. Its screen sets the two delays. It holds up to **200 holes** *(proposed)*.
2. **Hook up:** right-click the `blasting_machine` with the logger to load the shot. The machine must be **at least 50 m** from
   every hole in it, and within **500 m** of them (the lead line, real lengths 300-1,000 m, *proposed*). Closer: "too close: firing
   point inside the danger zone".
3. **Fire:** with the key in, **ARM** (it tests the shot: each hole's detonator answers, and it reads how many holes, kg, tonnes,
   powder factor and any hole that will not fire), then **FIRE** within 30 s of arming. A **siren sounds for 10 s** at the shot, then it
   fires. Pressing FIRE with a player inside the danger zone (below) asks a second press: "PLAYER IN DANGER ZONE" *(proposed: real
   practice is a clearance and guard posts; a game cannot stop a player walking back in)*.
- **PLC points** (a `ControlDevice`): ARM (command), FIRE (command, rising edge, only when armed and the key is in), ARMED,
  FIRED (pulse), HOLES (4-20 mA over 200), MISFIRES (count). A PLC can never fire a machine without its key.
- **A shot refuses to fire** while any Wildcatter or Tailings part, block entity or vehicle is inside the volume it breaks: "machine
  in the shot: move it" *(proposed: keeps the blast from wrecking builds; real practice clears the bench)*.

## The blast (how a charged, primed, stemmed pattern breaks)
**The free face.** For each hole, its free face is the side (of the four) where open air stands at least half the bench height
within **burden + 2 m** of the hole, at mid-bench *(proposed)*. The front row has one; the rows behind face the row in front, which
fires first (the delays), as real rows do. A shot with no free face anywhere is a **box cut**: it still breaks, but the swell heaves up
over the shot instead of spilling forward *(real box cuts heave)*.

**Each hole breaks its own block** *(proposed, the game's geometry)*: a box **burden** deep toward its free face, **spacing** wide
centred on the hole, from the collar's level down to the **bench floor** (the hole's depth less its subdrill). The subdrill zone is
fractured but not broken: it is what leaves the next bench's floor flat at grade (Decision P3-6). Boxes of neighbouring holes meet; a
block in two boxes breaks once.

| What is in the box | Becomes |
|---|---|
| Hard rock and ore (stone, deepslate, granite, diorite, andesite, tuff, sandstone, calcite, Wildcatter and vanilla ores) | **muck**, holding 3 quarters of what it was |
| The `blasthole` blocks | muck of the rock each replaced |
| Soft ground (dirt, sand, gravel, clay, mud, soft Wildcatter ores) | **left as it is**: already diggable *(proposed)* |
| Muck already there | left as it is |
| Water, lava, air | left; muck falls through fluid by the settle rule |
| Bedrock, obsidian, reinforced deepslate, anything unbreakable | left; the hole's box stops at it *(proposed)* |

- **Swell: 4 for 3.** Every 3 bank blocks broken give 1 more muck block (open-pit.md), placed by the settle rule from the top of the
  box's free-face side, so it spills forward off the face and down. In a box cut it is placed on top of the shot.
- **A hole fails** and breaks nothing if its charge is under **0.15 kg/t** of its box (open-pit.md, *proposed*). It reads
  UNDERCHARGED and its charge stays in the hole (a misfire, below).
- **Overcharged:** over **1.0 kg/t** *(proposed)* the hole breaks as normal and throws **flyrock** (below), as a real overcharged hole
  does.
- **Order:** the holes fire in delay order, spread over the ticks of the shot (each 50 ms is one tick), so the front row spills
  first. Dust (a cloud of campfire-smoke particles over the shot for 20 s, *proposed*) and one `tailings:blast` sound.

## Blowouts, flyrock and harm
- **A blowout:** a hole whose stemming is under **2 m** *(proposed)* vents up the collar. It breaks **half its box** (the top half
  stays standing) and throws **6 blocks of flyrock** *(proposed)*.
- **Short burden:** a front-row hole with less than **half its burden** of rock to the free face (an overhang cut back, a cave)
  throws **3 blocks of flyrock forward** *(proposed: the real cause of most flyrock)*.
- **A good hole throws none.**
- **Flyrock** is a muck block of one quarter of the hole's rock, thrown on a ballistic arc to land up to **100 m** off
  (open-pit.md, *scaled; real blasts clear people to 500 m*). It is a falling entity for its flight only. It lands by the settle rule
  and does not break blocks, except glass, panes and leaves *(proposed)*. Anything it lands on takes **10 damage** *(proposed)*.
- **Harm:** players and mobs within **50 m** of any firing hole take **30 x (1 − r / 50)** damage, r the distance in metres to the
  nearest hole *(proposed)*: 18 at 20 m, 6 at 40 m. It is explosion damage, so armour and Blast Protection count. Vehicles and
  machines outside the shot are not damaged *(proposed; left out)*.
- **No vanilla explosion:** no crater, no item loss, no block drops (open-pit.md).
- **Ground vibration and air blast** are left out; players within 200 m see a short camera shake *(proposed)*.

## Misfires
- **A hole misfires** when it is logged but its detonator did not answer the test (no detonator, a detonator dropped into a hole
  never logged), when it has no booster, or when it is UNDERCHARGED. **Electronic detonators do not fail by chance in the game**
  *(proposed: real rates are very low; every misfire is a cause the player can find)*.
- **What it leaves:** a MISFIRED hole keeps its charge. If its neighbours broke round it, its column stays as `blasthole` blocks
  standing in the muck, marked red.
- **The rule after a shot:** the blasting machine reads MISFIRES and the danger zone stays closed for **30 s** *(scaled; real codes
  set 5-30 min, unverified)*: a second shot cannot be armed, and the machine reads WAIT.
- **Digging into a misfire:** a loader or shovel bucket that would take a MISFIRED hole's block stops: "misfire in the muck: stop"
  *(proposed: the real accident is a bucket hitting live explosive)*. The hole is cleared by **re-priming and firing it alone**
  (a booster and detonator dropped on the collar again, logged, fired) or by **washing it out** with water buckets (one bucket a hole).

## What can go wrong (readouts)
- Drill: as above. Emulsion truck: EMPTY, NO HOLES IN REACH, NOT PRIMED (count). Blasting machine: NO KEY, NOT LOGGED, TOO CLOSE,
  TOO FAR, MACHINE IN THE SHOT, ARMED, WAIT (after a misfire), FIRED (holes, tonnes broken).

## Left out (to ROADMAP)
- Multi-pass drilling and angle holes; presplit and wall-control blasting.
- Real ground vibration (PPV) and air-overpressure limits.
- Detonator failure by chance; static and lightning hazards.
- Prill and ANFO (ammonia-plant.md, Left out).
- A self-running emulsion truck under dispatch.
- Fragmentation size (every hole makes the same muck); boulders and secondary breaking.

## Game tests to add
- `pit_drill_pattern` (from open-pit.md): a 3 x 3 staggered pattern drills 9 holes, each 16 m deep, each `blasthole` block
  remembering its rock.
- `pit_drill_short`: a hole over a cave stops SHORT at the cave.
- `pit_charge`: the emulsion truck charges 3 primed holes to 12 m, 828 kg each, at 20 kg/s, and skips an unprimed one.
- `pit_blast` (from open-pit.md): a charged, primed, stemmed 3 x 3 pattern breaks to muck at 4 for 3, spilled off the free face;
  the bench floor is left flat at 14 m; an unstemmed hole blows out with flyrock; a player 20 m off takes 18 damage.
- `pit_blast_boxcut`: with no free face the swell heaves up over the shot.
- `pit_misfire`: a hole with no booster misfires; the next shot waits 30 s; a loader stops at it; washing it out clears it.
- `pit_blast_guard`: a firing point 40 m away reads TOO CLOSE; a truck in the shot blocks firing.
- `pit_blast_plc`: a PLC arms and fires with the key in, and cannot without it.
- Client film `pit_blast`: drill, charge, stem, log and fire a 3 x 4 pattern; load the muck.

## The wave plan
- **P3a (Wildcatter):** the ammonia plant to emulsion matrix in an IBC (ammonia-plant.md). Tailings compiles against that release.
- **P3b:** the blasthole drill (parts, contraption, pattern, diesel), the `blasthole` block and its states.
- **P3c:** boosters, detonators, the logger, the blasting machine, the emulsion truck and the matrix fill point, the blast, flyrock,
  harm and misfires.
- Each with models on a contact sheet, NeoForge scenes, and the client film at the end of P3c.

## Sources
- Atlas Copco/Epiroc PV-271 product page: 270 mm single pass, 16.8 m single pass, 32 m max, 34 t (75,000 lb) bit load.
- AusIMM, *Rotary blast hole drills, a comparison* (Large Open Pit Mining Conference, Newman): 0.012 m/s scheduled penetration for a
  270 mm bit (43 m/h).
- BME E-244 HC surface MMU product page: up to 300 kg/min delivery. US patent 6,165,297: MMU loading rates 70-1,000 kg/min.
- Engineering News (2026-10-16), Enaex Africa: MMUs up to 28 t.
- Mining lecture notes (stemming 20-35 d, burden 25-40 d, bottom charge 0.3-0.5 of burden plus subdrill): rules of thumb, *(proposed)*
  where used.
- Delay timing and misfire waiting times were not found in a primary source: *(unverified)* where used.
