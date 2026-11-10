# L2 and L3: battery locomotives and bottom-dump cars (2026-11-09), decided while Tim was away

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back".
Built on branch `l2-l3-trains` from the draft specs [battery-locomotive.md](../multiblocks/battery-locomotive.md) (L2) and
[bottom-dump-cars.md](../multiblocks/bottom-dump-cars.md) (L3), whose own choices are in `specs-tailings.md` (L2-1, L3-1). Format as in
`README.md` here. **#7, #12 and #16 most need your eye.**

## L2: the battery locomotive

### 1. One entity class or a new one?
- **Chosen:** a new entity type `battery_locomotive` on the trolley locomotive's class (`MineVehicleEntity`), as the spec says ("the
  trolley locomotive's entity with a battery in place of a shoe"). Couplings, signals, the dump, harm, derailing and chunk tickets are
  the same code; only where the power comes from differs.
- **Why:** one train code path; a battery locomotive and a trolley locomotive behave alike everywhere but the power.
- **Alternatives:** a subclass (cleaner types, but every `instanceof` check in the train code would need to know it).

### 2. Does a new battery locomotive come with a battery?
- **Chosen:** no. A placed battery locomotive reads NO BATTERY until it is right-clicked with a `loco_battery`. A crafted battery (no
  item data) holds 10 % of its usable charge, as a new pit machine holds 10 % diesel. Breaking the locomotive drops its battery as
  its own item, with its charge.
- **Why:** if the locomotive came with a battery, placing and breaking it would make batteries from nothing. The box is a separate
  part in real mines too (it is changed by crane).
- **Alternatives:** the locomotive comes with a full battery (easier first use, but a free battery each time it is placed); the
  locomotive's recipe takes a battery.

### 3. How the charge is counted
- **Chosen:** in usable watt-hours, 0 to 46,464 Wh (80 % of 132 V x 440 Ah = 58,080 Wh). The percentage shown, LOW BATTERY (under 20 %)
  and the PLC's 4-20 mA are of the usable charge. FLAT is 0 usable Wh.
- **Why:** the spec's "usable 46 kWh, under 20 % LOW, at the floor FLAT" reads most simply as shares of the usable charge.
- **Alternatives:** count the nameplate 58 kWh with a 20 % floor (same behaviour, the percentage would never show under 20 %).

### 4. Energy on the flat: 1.5 kWh a km, not 1.3
- **Chosen:** the battery gives the motors' output over the drive's 85 %: on the flat at 3 m/s a loaded 48 t train's motors give
  14.1 kW, so 16.6 kW from the battery, about **1.54 kWh a km**. The climb is as the spec says: about 7.9 kWh for 50 m of height, 15.7
  for 100 m (the game test measures 7.86 kWh per 50 m).
- **Why:** the spec's 1.3 kWh/km is haulage.md's 14 kW without the 85 % it applies to the climb; the code applies it to both.
- **Alternatives:** 1.3 kWh a km (drop the drive's losses on the flat only).

### 5. Speed and the rack
- **Chosen:** the trolley locomotive's rule (`Traction.limit`) at 44 kW: 0.20 m/s loaded up the 1-in-2, 0.55 empty, 1.2 alone, 3 m/s
  capped on the flat. Down the decline the rack brake holds it to 1 m/s and it draws nothing (no regeneration). On fed track it does
  not touch the conductor rail.
- **Why:** the spec's table; the rectifier's load should not see a locomotive with no shoe.
- **Alternatives:** none considered.

### 6. FLAT
- **Chosen:** at 0 Wh the motors give nothing; it slows at its usual 0.3 m/s² to a stop and is braked where it stands, on the flat or
  on the rack, and reads "FLAT: its battery is at its floor; braked where it stands until it is charged or swapped". It does not roll
  back down the decline (`battery_loco_flat`).
- **Why:** the spec ("it stops where it is, braked"); trains in this mod never roll under gravity.
- **Alternatives:** stop at once (a jolt from 3 m/s); roll back to the bottom (real for a locomotive with no brake, which none is).

### 7. The hands: the spec's sneak-click clashes with reversing
- **Chosen:** right-click with a `loco_battery` **swaps** batteries (the one in hand goes in, the old one comes out to the hand).
  Sneak-right-click with an empty hand **takes the battery out only when the locomotive is held and standing**; otherwise it reverses
  the train as on the trolley locomotive. Every click reads the charge (kWh, %, LOW or FLAT, CHARGING), its state and its cars.
- **Why:** the spec gives sneak-right-click to both taking the battery out and the readout, but sneak-right-click already reverses a
  locomotive. Taking the box out of a moving train makes no sense, so the held-and-standing case is free.
- **Alternatives:** a key item (Wildcatter's Selector Key) to take the battery out; take it out only with a battery swap.

### 8. The charger as a receptacle load
- **Chosen:** `battery_charger` (2 x 2 x 1) plugs into a receptacle touching any of its cells. It is Wildcatter 0.5.1's `DriveLoad`:
  a balanced three-phase constant-power load at power factor 0.95 drawing 20 kW, picking its load up at 10 % a second and stopping
  below 80 % of its volts. 90 % of what it draws goes into the battery: 18 kW, a flat battery full in 2.6 hours. Its volts come from
  the world's standard (Wildcatter's `Voltage.INDUSTRIAL`), never a number in Tailings.
- **Why:** the receptacle is only a junction in Wildcatter; a plug adapter is a load set against it, and the charger is the same.
  `DriveLoad` is the real shape of a rectifier front end and is already in the 0.5.1 API, so no newer Wildcatter is needed.
- **Alternatives:** a plain resistive load (as Wildcatter's plug adapter; no power factor); drawing through a plug adapter block
  (the charger would then be an FE machine, which Tailings has none of).

### 9. Which receptacles it fits
- **Chosen:** the industrial-tier three-phase 30 A class: NEMA L22-30 (480 V) and IEC 60309 red 32 A (400 V). It refuses the others and
  says WRONG RECEPTACLE, naming the one it is against; NOT PLUGGED IN with none.
- **Why:** the spec's "the world's three-phase 30 A receptacle: L22-30 or IEC 60309 red"; the industrial tier is what a mine runs.
- **Alternatives:** also NEMA L21-30 (208 V, the low tier: the same 20 kW would be 59 A, twice its rating); IEC red 63 A.
- **Question:** at 400 V the same 20 kW is 30.4 A on a 32 A receptacle, 95 % of its rating, more than the 80 % continuous the 480 V
  case keeps to. Should the charger take 80 % of whichever receptacle it is on (17.7 kW on IEC) instead of a flat 20 kW?

### 10. What the charger charges
- **Chosen:** the battery it holds first (right-click it with a battery; sneak-click with an empty hand takes it out; a click reads
  it); with none, a battery locomotive standing with any part of it within 3 m of the charger. It stops drawing when the battery is
  full. Broken, it drops the battery it held.
- **Why:** the spec's two ways; one charger, one battery at a time, as a real single-output charger.
- **Alternatives:** charge both at once (two outputs); charge the locomotive first.

### 11. The charger's PLC points
- **Chosen:** CHARGING, FULL and CHARGE (4-20 mA, empty to full), inputs only. With no battery the loop reads 4 mA (empty), not a
  failed loop. No HAND / OFF / AUTO: it has no outputs.
- **Why:** the spec's points; a selector would have nothing to select.
- **Alternatives:** a CHARGE ENABLE output with a selector, so a PLC can shed the bay's load.

### 12. Charging takes 2.6 real hours
- **Chosen:** built at the real rate: a flat battery takes 2.6 hours of play to fill. The game tests check the rate (18 kW into the
  battery), not a full charge.
- **Why:** the rule of the mod is real values; the two-battery swap is what makes it workable, as in real charging bays.
- **Alternatives:** scale charging time (as the TBM's tests do, never play). **Question:** this is the same kind of choice as the real
  residence times (Wildcatter `plant-crane-gaps.md` #11). A full battery runs about 30 km on the flat, so one battery a shift is
  realistic, but a player may find 2.6 hours long.

### 13. Recipes (placeholders, as the trolley locomotive's)
- **Chosen:** battery locomotive: iron around a steel ingot, a mine rail under. Traction battery: six Wildcatter lead ingots, two copper
  ingots and a steel ingot. Charger: steel, copper, redstone and Wildcatter three-phase cable. Bottom-dump car: the Granby car's with an
  iron trapdoor in place of the chest. Track grizzly: iron bars over iron and a rail over smooth stone. Cam rail: iron over smooth stone.
- **Why:** the spec ("the battery from Wildcatter's lead and copper"); Wildcatter 0.5.1 has lead and steel ingots.
- **Alternatives:** Wildcatter's own battery item if it gets one.

## L3: bottom-dump cars

### 14. How the station is laid out
- **Chosen:** the `track_grizzly` (1 x 1 x 4) is the block **under** the mine rail; the `dump_pocket` is under the grizzly; the
  `dump_cam_rail` (1 x 1 x 4) stands beside the rail or beside the grizzly. A bottom-dump car dumps when the rail cell it is on has a
  grizzly under it, a pocket under that, and a cam rail beside. The pocket is 3 m deep along the track if placed facing north; turned
  east or west its 4 m run along the grizzly (the scenes do that). A grizzly cell with no pocket under it dumps nothing.
- **Why:** the spec's "mine rail runs over it as over any rail"; the side-dump's pocket already sits under the rail, so the
  grizzly between them is the only change.
- **Alternatives:** the grizzly as a rail block itself (a new rail type the track code would have to know).

### 15. When the load falls, and the doors
- **Chosen:** the whole load drops the tick the car's rail cell is over the grizzly and the pocket can take it, at the train's speed
  (the `bottom_dump_on_move` train crosses at 2.7 m/s). The doors are open while the car is over a grizzly with a cam rail and empty,
  shut everywhere else (the closing ramp). A car the pocket cannot take does not open, and the train holds with it over the grizzly.
  The doors are synced and drawn swung down 60 degrees; they snap, they do not swing.
- **Why:** the spec ("all of it while the car is over the 4 m grizzly"), and the side dump does the same.
- **Alternatives:** drop the load over the grizzly's 4 m at the car's speed, a share a tick (looks better, no change in play).

### 16. Oversize: the spec says none, the pocket keeps it
- **Chosen:** the pocket under a track grizzly keeps the plant-gaps oversize rule the side-dump pocket has: one item in a hundred stays
  on the bars, 32 choke it, a pickaxe on the pocket breaks a piece through. The spec ("TBM muck has no oversize ... as today") was
  written before plant-gaps added oversize to the pocket.
- **Why:** one pocket, one rule; the two stations should not differ in a way the player cannot see.
- **Alternatives:** no oversize for bottom dumps, as the spec says (the track grizzly would let everything through). **Question:**
  which do you want? The scenes lift oversize off where a full train is dumped.

### 17. Mixed trains and the signal before the dump
- **Chosen:** each kind dumps only at its own station: Granby cars at a ramp, bottom-dump cars over a grizzly; each passes the other's
  station loaded. The signal before a dump counts only the cars of that station's kind against its pocket.
- **Why:** the spec's mixed trains; a train of Granby cars should not wait for a full grizzly pocket it will not use.
- **Alternatives:** none considered.

## Both

### 18. Art
- **Chosen:** the two vehicles' skins are painted by `tools/models/vehicles.py` from the art wave's Ludo swatches (as the locomotive's
  and car's): the battery box in worn steel, the doors in dark steel. Their item icons and the battery's are Ludo pictures (`item-icon`,
  Photorealistic 3D, 2 variations each, the first of each kept; 3 credits), processed by `tools/process_art.py`. The three parts are drawn
  in Wildcatter's materials by `tools/models/trains.py`. Nothing is a script-drawn placeholder.
- **Why:** the art wave's method (`art-wave.md` #1-2).
- **Alternatives:** none.

### 19. Sound
- **Chosen:** the battery locomotive uses the trolley locomotive's motor loop; the bottom-dump car's drop uses `muck_tip`; the
  charger is silent.
- **Why:** no new sounds without the sound wave; a charger's hum is faint.
- **Alternatives:** a charger hum and a door clang (for a sound wave).

### 20. The game tests' shortcuts
- **Chosen:** `battery_loco_climb` measures 3 m of height (of the 50 m the spec names) and scales it up; `battery_loco_run` dumps ten
  full cars with oversize lifted off the bars each tick (a rock breaker), since 5,760 items leave 58 pieces, more than choke it;
  `battery_charge` runs a real 500 kW diesel set into the receptacle through Wildcatter three-phase cable.
- **Why:** a 50 m climb at 0.2 m/s is 9 minutes of ticks.
- **Alternatives:** none.
