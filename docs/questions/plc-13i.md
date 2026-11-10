# PLC 13i's last signals — decided while closing them (2026-11-09)

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back
... close all known gaps". These are the three 13i lines in ROADMAP.md's Known gaps, closed on branch `plc-13i` (PROGRESS.md, "PLC
13i: the last signals"). Wildcatter 0.5.1's API only: its AO4 module already writes 4-20 mA to a device's analog outputs
(`ControlDevice.commandMa`), and Wildcatter's plc.md 13i note says "sorter routing and rates need word signals, as AO does, not bits".

### 1. How a rock type travels on a signal
- **Chosen:** one analog output a side, `NORTH TAKES 4-20 mA, ROCK CODE` (and EAST, SOUTH, WEST): code *n* at 4 + *n*/4 mA, read to the
  nearest quarter milliamp. Code 0 (4 mA) is "nothing of its own" (its rock goes to waste, as a cleared side); code 1 is the first rock
  in `Rock`'s list, in its order: cobblestone 1, stone 2, granite 3, diorite 4, andesite 5, deepslate 6, tuff 7, sandstone 8, iron
  ore 9, copper ore 10, gold ore 11, uraninite 12 ... sphalerite 21 ... phosphate 38 (DESIGN.md has the list). A code names the rock's
  block item, as the screen sets it (copper ore is `minecraft:copper_ore`).
- **Why:** an AO4 channel is how Wildcatter's PLC writes a word to a device; one channel a side keeps four sides on one module. A
  fixed step of 0.25 mA (500 raw counts) is far wider than any error, and new rocks join at the end of the list without moving a code
  already in a program. `PlcScaleTest` pins the codes so a reordered `Rock` fails the build.
- **Alternatives:** a share of the span per code (16 mA over the 38 rocks: every new rock moves every code); BOOL-coded bits (6 DO
  points a side, 24 for the sorter); codes into a list the player builds on the sorter's screen (fewer codes, but a program then
  depends on a screen setting it cannot see).

### 2. When the PLC's choice holds, and what happens to the screen's
- **Chosen:** in AUTO, while the loop is fresh (Wildcatter's stale time, 10 ticks), each side takes the rock its code names, over what
  the screen set; the screen's setting is kept, not overwritten, and comes back when the PLC stops saying, in HAND or OFF, or on a code
  with no rock (past 38). The waste side stays the waste side. Two sides given one rock: the first (north, east, south, west) takes it.
  The inspect line marks a side the PLC sets: `west (PLC): minecraft:copper_ore`. A shut gate still sends a side's rock to waste.
- **Why:** as every 13i device: a command not refreshed counts as off, so a cut cable leaves the sorter as the player left it.
- **Alternatives:** write the PLC's choice into the screen's setting (survives the PLC, but a stale cable would leave the last choice
  for ever); refuse a code naming a rock another side takes.
- Fixed with it: a gate the PLC shut stayed shut when the PLC went quiet (it was set only when a command came). It is now refreshed
  every tick and opens when the command goes stale.

### 3. Which parts of the TBM's course are signals
- **Chosen:** TARGET Y and FLAT LENGTH, as two analog outputs on the cable junction; TARGET Y SET and FLAT LENGTH SET read back what
  the program holds. Heading and grade are not signals.
- **Why:** they are the cab screen's only course settings. The heading is the way the machine was built (Go sets it from its facing;
  it cannot turn), and the grade is 1 in 2 only (R6, decided with Tim). A heading point would be a point that can only be refused.
- **Alternatives:** a heading and grade point that read the machine's (inputs, no use to a program).

### 4. The course's mA ranges
- **Chosen:** TARGET Y over the overworld's height, -64 at 4 mA to 320 at 20 mA (24 blocks a milliamp, rounded to the block), held to
  the world's floor + 1; FLAT LENGTH 0 to 400 m (as CABLE LEFT's 400 m), rounded to the metre.
- **Why:** the screen sets an absolute Y, so the signal does too, and a program can be written from a map. AO4's 32,000 counts make the
  steps far finer than a block.
- **Alternatives:** a depth below the machine's start (portable, but not what the screen shows); a flat length in the screen's 8 m
  steps.

### 5. When the PLC's course takes
- **Chosen:** only while the machine stands. A course set in the same scan as a GO is in the program before the GO starts it (the
  junction applies the course, then the GO). While it drives, the course it started on holds; a change takes when it next stands. A
  target that would move floor already cut is held: before the first advance any target; while the drive still declines, no higher
  than the floor it stands on (it levels out there); once it has levelled out, the target it has.
- **Why:** the floor of every slice is worked out from the target (`TunnelDrill.floorAt`), and the floor is laid 12 slices behind the
  face: a target changed after the machine levelled out, or raised above its floor, would lay the floor behind it at the wrong height.
- **Alternatives:** take it at any time, as the cab's screen does (question below).

### 6. What a slurry part reads, and where
- **Chosen:** every part with a motor that holds slurry (ball mill, cyclones, spiral bank, conditioning and activation tanks, flotation,
  scavenger and cleaner banks, regrind mill, concentrate and CCD thickeners, pressure filter, leach tank, electrowinning bank, heap pad)
  adds three analog inputs: LEVEL, FLOW and DENSITY, 4-20 mA. LEVEL and DENSITY read its feed tank, the inlet buffer it draws its feed
  into off the pipe; the mill, the cyclones and the heap pad have none and read their discharge sump. FLOW is what leaves it by pipe.
  A transmitter clamped to the part reads them too (Wildcatter's `Measurable` LEVEL %, FLOW L/s, DENSITY kg/L).
- **Why:** a level on the feed box is what shows a part backing up: when its outlet is full it stops drawing its feed down, and its
  feed tank fills. A flowmeter and a density gauge on the discharge are where a plant puts them; FLOW is the one hook every pipe draw
  passes (the part's `take`).
- **Alternatives:** read the outlet's level (it fills first, but holds two products on a bank); a feed flow on the inlet (each stage
  draws its own way).

### 7. The slurry ranges
- **Chosen:** LEVEL 0-100 % of the hold the part works to (10 s of the plant's flow at each inlet: about 400 kg of fines slurry; the
  mill's 10 s of discharge, the cyclones' 10 s of overflow); FLOW 0-60 L/s; DENSITY 1.0-2.0 kg/L. Density is worked from each product's
  solids by mass from the specs, with the solids at 2.7 t/m3 as a block is: mill discharge 70 % (1.79 kg/L), fines, conditioned and
  tails slurry and leach slurry 35 % (1.28), concentrate 30 % (1.23), thickened concentrate 60 % (1.61), pregnant liquor a solution
  read as water (1.00). An empty tank reads 1.0 (4 mA). Over and under range is held to 3.8-20.5 mA, as a transmitter's is (NE 43).
- **Why:** the specs give the flows (flotation feed 31 L/s, flotation-bank.md; the mill's discharge with its 250 % circulating load is
  39 L/s) and the solids. 60 L/s covers the largest with room; 1.0-2.0 kg/L is water to above the mill's discharge.
- **Alternatives:** flow in m3/h (0-200; the spec writes both, but Wildcatter's FLOW unit is L/s); a level against the specs' real tank
  volumes (a flotation bank's eight 20 m3 cells: the game's 10 s hold would read near 0 % always); concentrate's real higher density
  (chalcopyrite is 4.2 t/m3), which the game does not model.

## Questions for Tim

- **The cab's screen can still change the course while the machine drives,** and after it has levelled out (both move floor already
  cut, as 5 says). The PLC is held to 5; should the screen be too?
- **Deepslate ores:** a code names the plain ore's item (`minecraft:copper_ore`), as the screen does from what is in hand; a
  `deepslate_copper_ore` item is another item to both. Should a side take a rock whatever its block (any copper ore)?
