# P5: big machines — decisions taken while building (2026-11-09)

Spec: [../multiblocks/open-pit-p5-big-machines.md](../multiblocks/open-pit-p5-big-machines.md) (a draft decided on recommendation;
its own choices are in [specs-tailings.md](specs-tailings.md), P5 section). These are the questions building it raised, decided on the
best recommendation while Tim was away. Nothing here is final until he has been through it.

## P5a: the pit switch house and trailing cables

### 1. What the switch house is in the world
- **Chosen:** a Tailings plant part on Wildcatter's big parts (`pit_switch_house`, 3 x 3 x 6, `Machine.PIT_SWITCH_HOUSE`) whose
  main cell is one block entity: a Wildcatter circuit part, a PLC device and a panel. The medium voltage lands on any cell of its north
  face; the four couplers are drawn on its south face.
- **Why:** the spec asks for "a Wildcatter circuit part (as the TBM's `cable_junction` is)"; the plant parts already carry ports on
  chosen faces of chosen cells (`hasPortAt`) and a PLC device, so it is the same shape as the haul dispatch office.
- **Alternatives:** a Wildcatter-side block (it would have to live in Wildcatter); four separate one-block outlets (not a real house).

### 2. The machine at the far end of the cable on the solver
- **Chosen:** each closed outlet adds the cable's three phases as resistors (0.1 ohm a km a phase, proposed) from the house's bus to
  the machine's end, and the machine as a constant-power star at power factor 0.95 at that end, sized from the volts it saw at the
  last solve. The volts, amps and kW the house shows are the solver's.
- **Why:** it is what Wildcatter's drives do (constant power from the line, impedance from the last volts), so the cable's drop, the
  current and the trip are real; Wildcatter's own `DriveLoad` was not used because it ramps its draw 10 % a second and sheds for an
  island's frequency, which would hide a 12 s hoist.
- **Alternatives:** `DriveLoad` per outlet (a ramped draw, frequency shedding); no cable resistance (no drop to see).

### 3. The cable's resistance: its whole length
- **Chosen:** the resistance is the whole cable's (every reel on the machine), paid out or not.
- **Why:** the current runs the cable's whole length, whether it lies on the ground or is still on the reel; this is why real pits
  do not hang more cable on a shovel than it needs.
- **Alternatives:** only what is paid out (the drop would grow as the machine drives away, which a real cable does not do).

### 4. How a breaker trips
- **Chosen:** definite time, as the spec: each outlet trips once its current has been over 1.25 x its machine's peak current (at the
  nominal volts, power factor 0.95) for 1 s of solves; never set over the outlet's 1,200 A. The incoming breaker trips at 2,500 A for
  1 s. A trip, and a ground fault, hold until **Reset trips** on the panel; the PLC cannot reset one.
- **Why:** "as Wildcatter breakers do" in the spec, kept to the one setting it names; a PLC never overrides protection (DESIGN.md,
  PLC signals).
- **Alternatives:** an inverse-time curve from Wildcatter's `TripCurve` (a more real relay, harder to read in a scene); a PLC RESET
  point.

### 5. Paying out the cable
- **Chosen:** the machine counts what it moves: moving away from the house (or across) pays out what it moved; moving back toward it
  reels in what it moved; never less than the straight line to the house. Coupled, it starts at the straight line.
- **Why:** the spec counts the path ("the real cable lies along the machine's track"); real shovels have the cable picked up and
  moved as they come back, which the reel-in stands for.
- **Alternatives:** the straight line only (a machine could wander forever); the path only, never reeled in (driving back and forth
  would use the cable up).

### 6. At CABLE END, and a torn cable
- **Chosen:** a move that would pay out past the cable's length is refused: the machine is put back where it was that tick, stops,
  and reads "CABLE END"; its outlet stays closed. A machine that jumps more than 8 m in a tick (teleported, carried) tears its cable
  out: its outlet trips on a ground fault and the cable is off it.
- **Why:** the spec has both "past its length it stops: CABLE END" and "a cable that runs out or is broken trips it"; stopping first
  is the real operator's job, and the trip is kept for the cable that really is broken.
- **Alternatives:** trip at CABLE END too (harsher, and the machine would sit dead at the end of every bench).

### 7. The ground check
- **Chosen:** an outlet closes only while its machine has answered on the cable within 2 s; a closed outlet whose machine stops
  answering for 2 s trips on a ground fault. A house just loaded gives its machines 2 s to answer.
- **Why:** the pilot wire proves the cable reaches a machine (spec). A machine whose chunk unloads with its outlet closed reads as a
  lost pilot, which is what a real relay would see.
- **Alternatives:** check only on closing (a cable cut later would not trip).

### 8. Coupling a cable
- **Chosen:** a `trailing_cable` item (one 400 m reel) clicked on a switch house takes its first free outlet; clicked on a machine
  with a cable reel it couples that outlet to it (the machine must be within 400 m in a straight line). Another reel on a coupled
  machine adds 400 m; sneak-clicking a coupled machine with a reel takes its cable off and gives the reels back. A machine broken by
  hand gives its reels back too.
- **Why:** the spec's two clicks; the first free outlet saves a screen. The machine is the reel's end because the drill and shovels
  are entities (the rope shovel's crawlers hold its reel once it is lifted).
- **Alternatives:** choose the outlet by the face clicked; a screen to pick it.

### 9. Drawing the cable
- **Chosen:** a black rod from the outlet's coupler, a point a metre along the straight line to the machine's back, lying on whatever
  ground is there; the path is counted, not drawn.
- **Why:** the spec asks for a line draped over the terrain; drawing the driven path needs the path sent to every client, which can
  come later.
- **Alternatives:** draw the path (sync the breadcrumbs); draw nothing (as the TBM before its gaps wave).

### 10. The drill's electric option
- **Chosen:** an eighth setting on the drill's panel, **Electric** (0 diesel, 1 its cable), changed at any time; on its cable it draws
  what its diesel would have made (600 kW rated, proposed: the same as its diesel) and burns no diesel. With no cable it reads "NO
  CABLE"; it does everything else as before.
- **Why:** decision P3-1 ("diesel in P3, electric from P5" as a screen choice); real 271-class drills come either way.
- **Alternatives:** an electric drill as its own item (two machines to place); a bigger electric rating (unverified, so kept).

### 11. Any machine on its cable stops under 90 % volts
- **Chosen:** every electric machine (drill, hydraulic shovel, rope shovel) stops reading LOW VOLTAGE under 90 % of the nominal at its
  end of the cable.
- **Why:** P5-5 sets it for the rope shovel; the same drives would not run any better on the others.
- **Alternatives:** the rope shovel only.

### 12. The switch house's panel and PLC points
- **Chosen:** a right-click opens a small panel (the P3 panel): each outlet open or closed with minus and plus, **Reset trips**, and a
  status line of every outlet's machine, amps, kW and cable; sneak-right-click prints the full readout. PLC points per outlet CLOSE
  (command), CLOSED, TRIPPED (a trip or a ground fault), AMPS (4-20 mA over 1,200 A), then AUTO. The selector: HAND works from the
  panel, OFF holds every outlet open, AUTO follows each CLOSE.
- **Why:** the spec's screen and points, with the HAND / OFF / AUTO every Tailings PLC device has.
- **Alternatives:** a click on each cubicle door (needs the face and position clicked).

### 13. Feeding the house in the scenes
- **Chosen:** a test hook, `forceSupply`, puts an ideal medium-voltage source on the house's incoming side on the solver.
- **Why:** a real feed big enough for a rope shovel is a power plant; the hook keeps the cable's drop, the current and the trip on
  the solver (the haul dispatch office's `forcePowered` is the same idea).
- **Alternatives:** build a steam plant or gas turbines in each scene.

### 14. Recipes and the reel's icon
- **Chosen:** placeholder recipes from Wildcatter's parts (the house: steel, `cable_mv`, a redstone block, an iron door; a reel: eight
  `cable_mv` round black wool); the reel's icon is drawn by script (`trailing_cable_placeholder`).
- **Why:** the art wave and real recipes come later, as for every new part.
- **Alternatives:** none needed now.

## P5b: the 360 t ultra truck

### 15. One class with its own figures
- **Chosen:** `ultra_haul_truck` is its own entity type and item, `UltraHaulTruckEntity`, a subclass of the 100 t truck's class that
  only gives its figures: 15.08 x 9.76 x 7.71 m, 261 t empty, 363 t, 2,828 kW net, 67.6 km/h, a 3,785 L tank, its body up in 20 s,
  4 m tyres, a 7.0 m loading height. Driving, tipping, routes, fuel, dispatch, berms and going over a crest are the 100 t's code.
- **Why:** the spec says "the same entity class ... with these figures"; a subclass is that, and a second entity type is what lets the
  game give it its own size, item and model.
- **Alternatives:** one entity type with a class setting (one item placing two sizes; the box would have to change after spawning).

### 16. The engine figure it drives on
- **Chosen:** the net 2,828 kW, at the wheels x 0.8 as every pit machine; its fuel burns from the same work.
- **Why:** the spec's climb figure (11 km/h up 10 % loaded) is worked from the net figure; `pit_ultra_haul` measures 11.0 km/h.
- **Alternatives:** the gross 2,983 kW (about 11.6 km/h).

### 17. Routes sized by the truck on them
- **Chosen:** a truck surveys its route for its own box when it takes it (the ultra's 9.8 x 7.7 m); its lane is right of the stake line
  in proportion to its width (4 m for the 100 t, 6.3 m for the ultra); it needs twice its turning radius to turn round (25 m, 40 m:
  the ultra turns on 20 m, proposed); the berm it needs is mid-axle on its own tyres (2 blocks for the ultra). The survey book still
  shows the 100 t's survey.
- **Why:** "a route for both classes is surveyed for the wider" (spec): each truck surveying for itself gives the same answer for a
  mixed route and still lets a 100 t route through a tight cut work for the 100 t.
- **Alternatives:** the book surveys for the widest truck always (every old route through a tight cut would read blocked).

### 18. The wheel loader and the ultra
- **Chosen:** the loader refuses: "too high to load", whenever the truck under its bucket has a body over 6.0 m (its reach,
  proposed); the self-running loader says so and waits. The 100 t's loading height is set at 4.2 m.
- **Why:** decision P5-6; real 992-class loaders are not matched to 360 t trucks.
- **Alternatives:** allow it at 18 passes.

### 19. The ultra's skin and model
- **Chosen:** drawn by `tools/models/pit_vehicles.py` as the other trucks, painted in Ludo's materials (`skin.py`), on a 2048 sheet
  (its 15 m body does not fit 1024). Its item icon is drawn by script (`ultra_haul_truck_placeholder`). The body tips to 50 degrees
  (proposed).
- **Why:** the same generator gives the same round wheels and the same look as the 100 t; only the icon is new art.
- **Alternatives:** a Ludo icon now (an art wave).

### 20. A placeholder recipe
- **Chosen:** two 100 t trucks, steel ingots, a blast furnace and black wool.
- **Why:** the spec: "a placeholder recipe until the art wave".
- **Alternatives:** none needed now.

## P5c: the hydraulic front shovel

### 21. One shovel class for both shovels
- **Chosen:** a shared `PitShovelEntity` (dig a face, swing up to 90 degrees to a truck under its dump arc either side, dump; AUTO with
  double-side loading; a part bucket last) that the hydraulic shovel and the rope shovel each give their figures to. The wheel loader
  keeps its own code.
- **Why:** the spec gives both shovels the same loading ("AUTO as the hydraulic shovel, double-side loading"); the loader is left as it
  is so none of its scenes change.
- **Alternatives:** the shovels as kinds of loader (the loader's 4 m bucket reach and single side would leak into them).

### 22. The bucket fills to its rating: muck by the quarter
- **Chosen:** a shovel takes muck a quarter of a bank block at a time (the muck block keeps what is left), so a bucket fills to its
  27 t (40 quarters of rock) and a dipper to 99.9 t; soft ground still comes a whole block at a time, as the loader's.
- **Why:** whole muck blocks (2.0 t each) fill a 27 t bucket to 26.3 t, which makes 13 passes 342 t and an ultra needs 14; by the
  quarter it is the spec's 13 passes of 27 t.
- **Alternatives:** whole blocks (14 passes); count the bucket by volume (a different unit from the trucks').

### 23. When a truck is loaded
- **Chosen:** a truck with under half a bucket of room is loaded: the shovel tells it, it leaves its LOAD stake (a route truck no
  longer waits for its last tonne or the 60 s), and the shovel turns to the truck on the other side. A truck with half a bucket or more
  of room gets a part bucket of what it has room for.
- **Why:** real practice (the 10/10/20 payload policy: no pass that would overload a truck, no truck sent away badly short); it gives
  the spec's 13 passes (351 t of 363) and the rope shovel's 4 with a part dipper last.
- **Alternatives:** fill every truck to its last kilogram (a 14th, 12 t pass into an ultra).

### 24. The hydraulic shovel's figures not in the spec
- **Chosen:** its face is 8 wide, 12 high and 3 deep from its front (spec); it dumps about 10 m out from its centre to a truck whose
  body is within 6 m of that arc; a pass is 15 s digging and 15 s swinging, dumping and back (its 30 s cycle; proposed split); it
  burns at 60 % of its rated power through a pass, as the loader. All proposed.
- **Why:** from its 14.06 m reach and the loader's split; the 6 m is the spec's.
- **Alternatives:** a longer dig and a shorter swing (real cycles are about 40 % dig).

### 25. The electric variant
- **Chosen:** a panel setting, **Electric**: on, it takes the 860 kW spec of the same machine and draws through its trailing cable,
  burning no diesel; off, its 1,119 kW diesel. The panel also turns AUTO on and off where it stands (without a survey book).
- **Why:** P5-4: its real electric variant as a screen choice; AUTO from the panel lets a shovel work without a route, as a shovel at
  a face often does.
- **Alternatives:** a separate item for the electric shovel.

### 26. Its model
- **Chosen:** drawn by the vehicle generator in Ludo's materials (2048 sheet): crawlers with an idler and sprocket a side, the house
  that swings on its ring, and the front (boom, stick and bucket) that lifts about the boom foot. The generator learnt groups with
  their own hinges, hung from other groups. Its item icon is `hydraulic_shovel_placeholder`.
- **Why:** the same tool as the other pit machines; the nested groups let the front ride the swinging house.
- **Alternatives:** a contraption of parts (P5-4's alternative).

## P5d: the electric rope shovel

### 27. How the six parts are laid out to build
- **Chosen:** the crawlers and the house stand as they work (the house on the crawlers, its rear overhanging); the gantry on the house's
  roof; the boom, the dipper handle and the dipper lie on the ground ahead, the boom's foot at the house's front (`pit.RopeShovelLayout`).
  All six face the same way; the check reads each part's main cell where it should be and names the first one missing.
- **Why:** the spec's parts are a 3 x 3 x 20 boom and a 2 x 2 x 14 handle, which only fit their cells lying down; a real boom is laid out
  on the ground and raised by the gantry's ropes.
- **Alternatives:** parts at their working angle (a 20 m boom at 45 degrees is 14 x 14 cells, not the spec's 3 x 3 x 20).

### 28. Lifting it into one machine, and drawing it
- **Chosen:** RUN on the crawlers' panel checks the parts and the cable, then the parts' cells go to air, quietly, and one
  `RopeShovelEntity` stands in their place, at the crawlers' centre, with the crawlers' name (its outlet keeps knowing it) and its cable.
  It is drawn with its own model (`RopeShovelModel`, the vehicle generator in Ludo's materials): the crawlers, the house that swings, the
  boom raised at 45 degrees, the handle and dipper hoisted with each pass; the hoist and suspension ropes are drawn as lines. STOP sets
  the parts back down as blocks where it stands, square to the nearest way, if every cell is free (the ground ahead must be clear for the
  boom, handle and dipper, as for building); the new crawlers take its name and cable.
- **Why:** the spec ("started, it lifts into one entity ... stopped, it sets down if the cells are free"); drawing the lifted machine with
  one model rather than its 2,600 cells' block models keeps it cheap to draw and lets the boom stand up.
- **Alternatives:** draw each cell's own model (as the TBM does; heavy at this size, and the boom would stay lying down).

### 29. Its power by its cycle
- **Chosen:** on its cable it draws the hoist's 3.5 MW while it digs (12 s of each 35 s cycle) and 0.6 MW the rest of the time it works
  (swinging, dumping, tramming, standing in AUTO); 1.59 MW on average. It has no diesel. `pit_rope_shovel` reads 3,500 kW and 600 kW on its
  outlet.
- **Why:** the spec's figures (proposed), on the solver so the cable's drop and the breaker see them.
- **Alternatives:** a steady 1.5 MW (no hoist peak for the breaker to see).

### 30. LOW VOLTAGE holds for 10 s
- **Chosen:** under 90 % at its end of the cable an electric machine's drives stop, and stay stopped 10 s (proposed) before they try
  again.
- **Why:** with the drives stopped the drop goes and the volts come back, so without a hold the machine would hunt on and off every
  solve; real drives trip on undervoltage and restart after a delay.
- **Alternatives:** latch until the operator restarts it (a machine in AUTO would sit stopped).

### 31. Its figures not in the spec
- **Chosen:** its face is 10 wide, 15 high and 4 deep from the front of its crawlers (spec); it dumps about 18 m out from its centre (its
  boom point, proposed) to a truck within 6 m of that arc; 12 s digging and 23 s swinging, dumping and back; the box it moves as is its
  crawlers and house, 15 x 14.4 m and 21 m to the boom point; in AUTO it works trucks within 40 m.
- **Why:** from the spec's reach and cycle; the dump radius from a 20 m boom at 45 degrees.
- **Alternatives:** none needed now.

### 32. Its PLC points: planned, not built
- **Chosen:** the rope shovel has no PLC points yet; its readout and panel show status, passes, tonnes, kW and cable. The spec's points
  (RUN, RUNNING, AUTO, PASSES, POWER) are planned on its switch house outlet's cable, as the TBM's are on its cable junction (ROADMAP,
  Known gaps).
- **Why:** a machine that lifts and moves has no fixed block to land a control cable on; the switch house is the fixed point, but
  giving each outlet a machine's points needs a second device on the house, which is its own piece of work.
- **Alternatives:** points on the crawlers only while it stands as parts (they would vanish when it runs, which is when they matter).

### 33. Recipes and items
- **Chosen:** each part has a placeholder recipe from Wildcatter's steel (the house takes Wildcatter's medium-voltage drive cells and
  drive transformer; the crawlers a track dozer). The lifted machine has no item: broken by hand (sneak, empty hand) it gives back its
  six parts and its cable.
- **Why:** it is built from parts, as the TBM; the art wave and real recipes come later.
- **Alternatives:** none needed now.

### 34. The client film `pit_big`
- **Chosen:** a rope shovel built from its parts on a bench, coupled to a switch house; RUN lifts it; in AUTO it fills an ultra on its
  right in 4 passes from a muck face; the loaded ultra drives off. Its passes are sped up (fastPasses) so the film runs about two
  minutes.
- **Why:** the spec's film; the speed-up as the other films' (pit_blast's drill).
- **Alternatives:** film it at real time (about 2 min 20 s of loading alone).
