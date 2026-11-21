# Films, batch 4 (2026-11-21): the TBM, rock to concentrate, the hoist

Small fixes made while filming three shorts (branch `films-batch4`), decided while Tim was away. Each is drawing or particles only;
no rate, recipe or rule changed.

### 1. The TBM's cutterhead turns while it cuts
- **Chosen:** `TbmRenderer` draws a wheel on the face in front of the cutterhead's shield while the machine cuts (`TbmEntity.cutting()`,
  the synced flag the cutter sound already used): a rim, six spokes, disc cutters staggered along them and a hub, turning at
  **6 rpm** *(proposed: 8 m hard-rock heads turn about 5 to 8 rpm)*. The square block model is left as it is behind it.
- **Why:** the head was a still plate; on camera nothing said the machine was cutting.
- **Alternatives:** turn the whole 8 x 8 cutterhead model (its corners would sweep outside the bore); a round bore (the spec leaves it
  out: "a square face stands in for a round bore").

### 2. Muck rides the TBM's conveyor gantry
- **Chosen:** `GantryRenderer` draws lumps of rock riding the conveyor gantry's belt to its tail while the machine drives, at **2 m/s**
  *(proposed: a 600 mm muck belt)*. The lumps are drawing only; the muck itself still goes from the buffer to the car as before.
- **Why:** the belt was still; "muck out on the belt" did not show.
- **Alternatives:** none simple (the buffer is not on the gantry).

### 3. The big mills turn
- **Chosen:** `PlantRenderer` turns a 16-sided shell just over the SAG mill's and the large ball mill's octagon shells (between the head
  flanges, the girth gear left to the model), with eight rows of liner bolt heads, at **75 % of critical speed**, 42.3 / sqrt(D):
  **10.2 rpm** for the 9.75 m SAG and **11.7 rpm** for the 7.3 m ball mill (sag-mill-line.md gives 75 % of critical).
- **Why:** the mills stood still while running.
- **Alternatives:** turn only the girth gear (too small to read on a phone).

### 4. Tank and column cells draw their froth
- **Chosen:** while a tank cell or column cell runs, a grey froth is drawn just over the model's dark surface, with bubbles riding out
  from the middle to the launder at the lip, growing as they go: a cycle every 30 s, about **0.1 m/s** in a tank cell *(proposed)*.
- **Why:** the model's froth was the slurry's dark colour; flotation had no froth on camera.
- **Alternatives:** froth particles (heavy on the client for a row of eight 160 m3 cells).

### 5. The X-ray sorter's pieces
- **Chosen:** pieces ride its 1.2 m belt at **3 m/s** (xrt-ore-sorter.md); off the head pulley those read as ore are blown on by the
  ejectors toward the south (product) chute with a puff at the manifold, and the rest drop and fall away east to the waste chute. The
  share drawn as ore is the share the sorter has sent to product (sent to the client every second when it moves by over 5 %).
- **Note:** as built (l8-xrt.md), the jets blow the **ore** to product and the waste drops; the short's caption says so.
- **Why:** the sorter showed no rock and no ejection.
- **Alternatives:** a fixed half-and-half (wrong on a mostly-waste feed).

### 6. A skip pours into the headframe bin
- **Chosen:** when a skip arrives at the dump with a load the bin has room for, a `muck_tip` sounds and rock pours from its bottom door
  toward the bin's top for the first **4 s** of the 10 s rest *(proposed)*; it then shows empty. Its 3 t still goes into the bin's count
  at the rest's end, as before.
- **Why:** the skip arrived, waited 10 s and left; its load vanished with nothing seen.
- **Alternatives:** move the dump itself to the start of the rest (a change to the hoist's timing and its PLC pulse).

### 7. Not done: TBM thrust
- The machine has no thrust figure (its readout gives status, floor, metres driven, cable, air). The TBM short's card shows the
  cutterhead's rpm, the advance (1 m in 20 s in stone, the game's rate), metres bored and rings set, and the drive's 1,000 kW.
  A thrust readout would need a thrust model (cutter count times load per disc); left for Tim.
