# Open pit P5: the pit switch house and trailing cables, the 360 t ultra truck, the hydraulic front shovel, the electric rope shovel

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved. Built (P5a-P5d): see "As built" at the end.** The choices are listed in
[../questions/specs-tailings.md](../questions/specs-tailings.md) (P5 section).

This fills in open-pit.md's P5 machines ("Pit power", "Haul trucks" for the ultra class, "Loaders" for the two shovels) so they can
be coded without questions. The dragline (P6) uses the same switch house and cable.

**Real counterparts:**
- A skid-mounted pit switch house on the pit's medium-voltage loop, with a breaker and ground-check relay for each machine's
  trailing cable.
- A 363 t ultra-class mechanical-drive haul truck (C175-20-class engine, 2,983 kW gross).
- A 250 t class hydraulic front shovel with a 15 m³ bucket (diesel, or electric off a trailing cable).
- A 4100-class electric rope shovel with a 55 m³ dipper, on a trailing cable at medium voltage.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

## The pit switch house (`pit_switch_house`)
| | |
|---|---|
| Real | a skid-mounted switch house moved round the pit by dozer, on the pit's medium-voltage loop |
| Size | **3 x 3 x 6** (open-pit.md, *proposed*): a steel skid house with four breaker cubicles and an incoming section |
| In | Wildcatter **medium-voltage cable** on its north face *(the world's standard medium voltage; real shovels and draglines take 4.16-7.2 kV)* |
| Out | **four outlets** on its south face, each a breaker, a ground-check relay and a trailing-cable coupler |
| Outlet rating | **1,200 A** each *(proposed: the dragline's 6 MW peak is about 830 A at 4.16 kV)*; the house's bus **2,000 A** *(proposed)* |
| Protection | each outlet's breaker trips at **1.25 x its machine's peak for 1 s**, as Wildcatter breakers do; the house's incoming breaker at 1.25 x 2,000 A |

- **A Wildcatter circuit part** (as the TBM's `cable_junction` is): one node on Wildcatter's solver, with each machine on an outlet a
  load at the far end of its cable (below).
- **Ground check** *(real: a pilot wire in the trailing cable proves the earth is whole)*: an outlet's breaker will not close until
  its cable reaches a machine. A cable that runs out or is broken trips it.
- **Its screen:** each outlet's machine, amps, kW, cable paid out, state (OPEN, CLOSED, TRIPPED, GROUND FAULT); a click on a cubicle
  opens or closes it; reset after a trip.
- **PLC points:** for each outlet CLOSE (command), CLOSED, TRIPPED, AMPS (4-20 mA over 1,200 A).

## Trailing cables
- **Connecting:** right-click the switch house's outlet with a `trailing_cable` item, then the machine's cable reel (its main cell).
  One cable item is **400 m** (open-pit.md) of medium-voltage mining cable on a reel *(real reels 300-500 m)*. Each extra cable
  item in the machine's cable slot adds 400 m *(proposed, as the TBM)*.
- **The cable is counted, not placed:** the machine counts the cable paid out as the **path length** it has driven from the switch
  house (the real cable lies along the machine's track). Past its length it stops: CABLE END.
- **Drawn** as a line lying on the ground from the switch house to the machine, draped over the terrain along its path, a few
  points a metre *(proposed; the TBM's cable is not drawn, a Known gap)*.
- **Voltage drop:** the cable is a resistance on Wildcatter's solver, about **0.1 Ω/km** a phase *(proposed: a 4/0 AWG class mining
  cable, unverified)*, so a long cable to a big machine shows a real drop.
- **Driving over it:** trucks, loaders and dozers may cross a cable *(proposed; real pits bridge it with cable crossings)*. Left out:
  cable damage.
- **Machines that take it:** the rope shovel (must), the dragline (must, P6), the blasthole drill (option: electric instead of
  diesel), the hydraulic shovel (option: its electric variant).

## The 360 t ultra-class truck (`ultra_haul_truck`)
| | | Source |
|---|---|---|
| Payload | **363 t** nominal, 134 bank blocks, 179 muck | open-pit.md |
| Gross | **623.7 t**; empty about **261 t** | spec sheet |
| Length | **15.08 m** | spec sheet |
| Width | **9.53 m** over the tyres, **9.76 m** outside the body | spec sheet |
| Height | **6.53 m** to the ROPS, **7.71 m** to the canopy, **15.70 m** with the body raised | spec sheet |
| Loading height | **7.0 m** empty | spec sheet |
| Engine | **2,983 kW** gross, **2,828 kW** net | spec sheet |
| Top speed | **67.6 km/h** loaded *(open-pit.md has 64: Decision P5-3)* | spec sheet |
| Fuel tank | **3,785 L** *(open-pit.md proposed about 4,000: Decision P5-3)* | spec sheet |
| Up 10 %, loaded | **11 km/h**: 2,828 kW x 0.8 / (623.7 t x g x 0.1195) | as open-pit.md |
| Wheel | about **4.0 m** *(unverified)* | |

- **The same entity class** as the 100 t truck (`HaulTruckEntity`), with these figures: driving, tipping, routes, fuel and dispatch
  as P1 and P2. Body raise **20 s** *(proposed; the 100 t is 13 s)*.
- **Survey:** its box is 9.8 m wide and 7.7 m high; its two-way roads are **30 m** (open-pit.md, 3 widths). A route for both
  classes is surveyed for the wider *(proposed)*.
- **Turning round** at the stake before an end needs about **40 m** of open ground *(proposed; the 100 t needs 25 m)*.
- **Loading:** the hydraulic shovel in 13 passes, the rope shovel in 4. The wheel loader can reach the 7.0 m loading height only
  just *(proposed: the loader's 6 m reach; the loader is refused for the ultra: "too high to load")*.
- **The berm** at its dump points is 2 blocks (open-pit-p4-dry-stack.md).
- **Damage** as the trucks (`truck_damage`).
- **Placing:** a placeholder recipe until the art wave.

## The hydraulic front shovel (`hydraulic_shovel`)
| | | Source |
|---|---|---|
| Class | **250 t class** front shovel, 258 t operating | spec sheet |
| Bucket | **15.0-16.5 m³** heaped; the game **15 m³, 27 t** (a loose 1.8 t/m³) | spec sheet; open-pit.md |
| Engine | **1,119 kW** diesel; the electric variant **860 kW** off a trailing cable | spec sheet |
| Upper width | **8.04 m**; undercarriage **6.0 m** wide | spec sheet |
| Max dump height | **10.35 m** | spec sheet |
| Digging reach | **14.06 m**; digs **3.72 m** below its tracks | spec sheet |
| Height | about **7.25 m** to the cab roof *(unverified, third-party)*; length about 15 m with the front *(unverified)* | |
| Cycle | **30 s** | open-pit.md |
| Fuel | **5,000 L** tank *(proposed)*; burns by work, a pass at 60 % of rated power *(as the loader)* | |
| Travel | **1.5 km/h** crawling | open-pit.md *(proposed)* |

- **An entity** (`PitVehicleEntity`), not a contraption *(proposed: it is one rigid machine that tracks and swings; it is drawn
  with its house swinging but moves as one box)*. It is about the loader's size class, not the rope shovel's.
- **Digging:** jump with the bucket empty takes up to 27 t from the face in front: **8 wide, 12 high from its floor, 3 deep**,
  top down *(proposed: its reach)*, soft ground or muck; standing hard rock reads "hard rock: blast it first". A MISFIRED hole stops
  it (open-pit-p3-blasting.md).
- **Loading:** it swings and dumps into a truck whose body is within **6 m** of the bucket's arc *(proposed)*, at either side.
- **AUTO** at a LOAD stake as the self-running loader (P2a), loading trucks that pull in on **either side**, alternately *(real
  double-side loading)*.
- **Electric variant:** a screen toggle when a trailing cable reaches it: it then draws 860 kW from the cable and no diesel.

## The electric rope shovel (`rope_shovel`)
| | | Source |
|---|---|---|
| Class | **4100 class** | |
| Dipper | **55 m³**, **100 t** (the class 53-61 m³, 100-109 t) | spec sheet |
| Crawlers and house | **14.4 x 15 m** | open-pit.md |
| Boom point | about **21 m** high *(unverified)*; boom about 20 m *(unverified)* | |
| Dump height | about **9.5 m** *(unverified)* | |
| Operating mass | about **1,400 t** *(unverified)* | |
| Power | electric, a trailing cable at the world's standard medium voltage; real 7.2 or 13.8 kV from a 3,750 kVA or larger transformer. About **1.5 MW average, 3.5 MW peak** *(proposed)* | open-pit.md |
| Cycle | **35 s** | open-pit.md |
| Travel | **1.5 km/h** *(proposed)* | |

### Parts (big parts, then a contraption, as the dragline)
| Part | Cells (W x H x L) | Real component |
|---|---|---|
| `shovel_crawlers` | 14 x 3 x 12 | The lower works: two crawler frames, the car body and the swing rack and roller circle. Its main cell holds the machine's block entity, screen and cable reel |
| `shovel_house` | 12 x 9 x 15 | The revolving frame and machinery house: the hoist, crowd and swing motors on their drives, the transformer and the cab at its front corner |
| `shovel_gantry` | 6 x 6 x 3 | The A-frame gantry on the house that holds the boom's suspension ropes |
| `shovel_boom` | 3 x 3 x 20 | The boom, its foot on the house's front and the sheave at its point *(proposed: one 20 m part, unlike the dragline's 75 m in sections)* |
| `shovel_handle` | 2 x 2 x 14 | The dipper handle through the boom's saddle block, crowded in and out |
| `shovel_dipper` | 5 x 5 x 5 | The 55 m³ dipper with its door and teeth |

- **Built and checked** as the dragline: NOT BUILT names the missing part; NO CABLE until a trailing cable from a switch house
  reaches it.
- **The contraption:** started, it lifts into one entity; the house, gantry, boom, handle and dipper swing about the crawlers'
  centre; the hoist ropes are drawn as lines from the boom point to the dipper. Stopped, it sets down if the cells are free.
- **Digging:** a pass takes up to **100 t** from the face in front: **10 wide, 15 high from its floor, 4 deep**, top down
  *(proposed: its reach)*, soft ground or muck only. It swings up to 90° and dumps into a truck under the dipper's arc, either side.
  The last pass into a truck is a part dipper *(real practice: no spill)*.
- **AUTO** as the hydraulic shovel, double-side loading: an ultra in **4 passes**, about **2 min 20 s** with spotting; a 100 t truck
  in 1 pass *(the real 4100 class is not matched to 100 t trucks; allowed)*.
- **Driving by hand:** a player in the cab moves it at 1.5 km/h with the keys and digs a pass with jump.
- **Power:** it draws its peak during a hoist and its average over the cycle, on Wildcatter's solver through the cable: 3.5 MW for
  the hoist part of each cycle (12 s), 0.6 MW the rest *(proposed: the average is about 1.5 MW)*. With too little voltage at the
  shovel (under 90 %) it stops: LOW VOLTAGE *(proposed)*.
- **Readouts and PLC points** (as the dragline): status (STOPPED, DIGGING, LOADING, WAITING FOR TRUCK, FACE OUT OF REACH, NOT
  BUILT, NO CABLE, CABLE END, LOW VOLTAGE), passes, tonnes, kW, cable paid out; RUN, RUNNING, AUTO, PASSES (pulse), POWER (4-20 mA).

## Left out (to ROADMAP)
- Electric-drive and trolley-assist ultra trucks (open-pit.md, Left out).
- Tyre heat and the ton-km-per-hour limit.
- Cable damage, cable crossings and the cable reel truck.
- Shovel tooth wear; the dipper's door trip as its own action.

## Game tests to add
- `pit_switch_house`: a rope shovel on an outlet draws its power; the breaker trips at 1.25 x peak held for 1 s; the outlet will not
  close with no cable to a machine.
- `pit_cable_end`: a drill on a 400 m cable stops at CABLE END after 400 m of path.
- `pit_ultra_haul`: an ultra truck on a route climbs a 10 % ramp at about 11 km/h loaded and tips 363 t into the pit hopper.
- `pit_hyd_shovel`: the hydraulic shovel fills an ultra in 13 passes from muck, double-side, in AUTO.
- `pit_rope_shovel_built`: NOT BUILT names a missing boom; NO CABLE without a cable.
- `pit_rope_shovel`: the rope shovel fills an ultra in 4 passes from muck and reads its power from the cable.
- Client film `pit_big`: a rope shovel and an ultra truck on a bench.

## The wave plan
- **P5a:** the pit switch house and trailing cables (the drill's electric option comes with it).
- **P5b:** the ultra-class truck.
- **P5c:** the hydraulic front shovel (diesel and electric).
- **P5d:** the rope shovel (parts, contraption, cable).

## Sources
- The 363 t truck's technical specification (12/2023) and dealer pages: 15,080 mm long; 9,529 mm over the tyres, 9,755 mm outside the
  body; 6,526 mm to the ROPS, 7,709 mm to the canopy, 15,701 mm raised; 6,998 mm loading height; C175-20, 2,983 kW gross, 2,828 kW net;
  67.6 km/h; 3,785 L fuel tank (4,542 L for the Tier 4 option).
- A 250 t class shovel maker's product pages: 1,119 kW (QSKTA50); 258 t; 15.0-16.5 m³ heaped; 10,350 mm dump height; 14,060 mm reach; 3,720 mm dig
  depth; 8,040 mm upper width; 6,000 mm undercarriage. Its electric variant: 860 kW.
- open-pit.md's sources for the 4100-class rope shovel.

## As built (P5a-P5d, 2026-11-09; decided while Tim was away, see [../questions/p5-big-machines.md](../questions/p5-big-machines.md))

Everything above is built, with these readings and additions (each a numbered decision there):
- **The switch house** is a plant part on Wildcatter's big parts with one block entity: a circuit part, a PLC device and a panel
  (decisions 1, 12). Each closed outlet puts its cable on the solver as a resistance a phase of its whole length and the machine as a
  constant-power star at 0.95 at the far end (2, 3). Trips are definite time, 1.25 x the machine's peak current for 1 s, capped at
  1,200 A, held until reset on the panel; the PLC cannot reset (4). The ground check needs the machine to have answered within 2 s, to
  close and to stay closed (7). Scenes feed it with an ideal supply hook; `pit_switch_house_fed` feeds one from a real set (13).
- **The cable** pays out along the path, reeling in as the machine comes back, never under the straight line (5); at CABLE END the move
  is refused and the outlet stays closed; a jump of over 8 m tears it out, a ground fault (6). A reel takes the house's first free
  outlet (8). It is drawn along the straight line, lying on the ground (9).
- **Electric machines** stop under 90 % at their end, LOW VOLTAGE, and their drives hold off 10 s before trying again (11, 30). The drill's
  electric option is a panel setting at its diesel's 600 kW (10).
- **The ultra** is its own entity type, a subclass of the 100 t truck with its figures (15), driving on the net 2,828 kW: 11.0 km/h up
  10 % loaded in `pit_ultra_haul` (16). Each truck surveys its route for its own box; lanes and turning room scale with it (17). The
  wheel loader refuses it, "too high to load" (18).
- **The shovels** share one class (21). Muck comes out of the face a quarter of a bank block at a time, so a bucket fills to its rating
  (22). A truck with under half a bucket of room is loaded and sent on; else the last pass is a part bucket (23): 13 passes of 27 t
  (351 t) for the hydraulic shovel, 3 x 99.9 t and a 62.8 t part dipper for the rope shovel. The figures the spec left open (dump arcs,
  dig and swing split, AUTO reach) are in decisions 24 and 31.
- **The hydraulic shovel's** electric variant is a panel setting; AUTO can be set from the panel (25).
- **The rope shovel's** parts are built with the crawlers and house standing and the boom, handle and dipper laid on the ground ahead
  (27); RUN lifts it into one machine drawn by its own model, STOP sets it down if the cells are free (28); it draws 3.5 MW hoisting
  and 0.6 MW the rest (29). Its PLC points are planned, not built (32, ROADMAP Known gaps).
- **Not built:** the rope shovel's PLC points (planned on its outlet); the spec's Left out list (ROADMAP, Later).

