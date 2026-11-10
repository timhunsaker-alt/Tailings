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
