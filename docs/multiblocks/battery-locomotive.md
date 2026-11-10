# Battery locomotives, charged from Wildcatter receptacles

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed wave **L2**. The choices are listed
in [../questions/specs-tailings.md](../questions/specs-tailings.md) (Later section).

ROADMAP "Later": battery locomotives charged from Wildcatter receptacles, for unpowered track. Today a train moves only on track fed
by a traction rectifier (haulage.md). A **battery locomotive** carries its own traction battery and runs on **any** mine rail,
powered or not: on a short branch, a new drive before its rectifier is in, or a mine with no rectifier at all. It is charged in a
**charging bay** from a Wildcatter three-phase receptacle, with a spare battery box charging while the other works, as real mines do.

**Real counterparts:**
- 8 t underground battery locomotives: **2 x 22 kW** traction motors on a **132 V, 440 Ah** traction battery *(a supplier's listing
  for the 8 t class, unverified)*: about **58 kWh**.
- Lead-acid traction batteries in steel boxes, changed by crane in a charging bay; lithium packs on newer machines.
- Wildcatter's three-phase receptacles (Wildcatter Phase 3: L21-30, L22-30, IEC 60309 red).

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

## Parts and items
| Part or item | Size | Real-world basis |
|---|---|---|
| `battery_locomotive` | 1 x 2 x 3 (as `mine_locomotive`) | An **8 t** battery locomotive, **2 x 22 kW**, a rack pinion for the decline as the trolley locomotive has, a headlight; **no collector shoe** |
| `loco_battery` (item) | carried in the locomotive | A **58 kWh** traction battery box, lead-acid, about **2 t** of the locomotive's 8 *(unverified)*. It keeps its charge as item data |
| `battery_charger` | 2 x 2 x 1 | A charger on a wall, with a **three-phase 30 A plug** into a Wildcatter receptacle *(the world's three-phase 30 A receptacle: L22-30 or IEC 60309 red)*, **20 kW** continuous *(80 % of the circuit, proposed)*. It holds one battery box |

## How it runs
- **The locomotive** is the trolley locomotive's entity (`mine_locomotive`) with a battery in place of a shoe: couplings, cars, run,
  hold, reverse, signals, the dump, harm and chunk tickets as haulage.md. Its motors give **44 kW** while the battery holds charge.
- **Speed follows power** as the trolley locomotive's (haulage.md), with 44 kW:

  | Train | Mass | Up the 1-in-2 decline, 44 kW | On the flat |
  |---|---|---|---|
  | Loaded, 10 cars | 48 t | 0.20 m/s | 3 m/s (capped) |
  | Empty, 10 cars | 18 t | 0.55 m/s | 3 m/s |
  | Alone | 8 t | 1.2 m/s | 3 m/s |

- **The battery's energy:** the motors draw their power over **85 %** *(proposed)* from the battery. A loaded 10-car train climbing
  100 m of height up the decline uses about **16 kWh** (48 t x g x 100 m, over 0.85); on the flat, about **14 kW** at 3 m/s, so about
  **1.3 kWh a km** *(proposed from haulage.md's 14 kW)*. Down the decline the rack brake holds it, as the trolley locomotive (no
  regeneration, *proposed*).
- **Usable charge** is **80 %** of 58 kWh, 46 kWh *(real: deep discharge shortens lead-acid life)*. Under 20 % it reads LOW BATTERY;
  at the floor it stops where it is, braked, and reads FLAT.
- **On fed track** it does not draw from the conductor rail (it has no shoe) *(proposed)*.
- **Charging:**
  - **In the locomotive:** a battery locomotive stopped with its cab within 3 m of a `battery_charger` *(proposed)*, the charger's
    plug in a live receptacle, charges at 20 kW less 10 % loss: **46 kWh in about 2.6 hours**.
  - **Swapping (the real practice):** sneak-right-click the locomotive with an empty hand to take its battery out; right-click with a
    `loco_battery` to put one in. A battery in a charger charges as above. Two batteries keep one locomotive working.
  - The charger is a Wildcatter receptacle load: it draws its 20 kW (and a real power factor of **0.95**, *proposed*) on the circuit the
    receptacle is on, and that receptacle's breaker trips as Wildcatter's do if a bigger charger were put on it.
- **Readouts:** sneak-right-click the locomotive for its charge (kWh, %), its state and its train. **PLC points** (on the charger):
  CHARGING, FULL, a 4-20 mA charge.
- **Recipe:** a placeholder, as the trolley locomotive's; the battery from Wildcatter's lead (if it has it) and copper *(proposed)*.

## Left out (to ROADMAP)
- Lithium packs and fast charging; regeneration on the decline.
- Battery gassing (hydrogen in the charging bay) and its ventilation.
- Battery life and cycles.
- Battery-electric pit trucks and loaders (open-pit.md, Left out: electric drive).

## Game tests to add
- `battery_loco_run`: a battery locomotive pulls 10 loaded cars on unpowered flat track to the dump.
- `battery_loco_climb`: up 50 m of decline it uses about 8 kWh and climbs at 0.20 m/s loaded.
- `battery_loco_flat`: run to the floor it stops FLAT and holds on the rack.
- `battery_charge`: a battery in a charger on a live 30 A receptacle charges at 20 kW; with the receptacle dead it does not.

## The wave plan
- **L2**: small, and useful at once to any mine with a branch.

## Sources
- Supplier listings for a Chinese 8 t class: 8 t, 2 x 22 kW, AC variable-frequency drive; 132 V, 440 Ah for the 8 t model *(unverified)*.
- AGH paper on a lithium monorail battery locomotive: a 143.5 kWh lithium system, 11 kW a drive, 88 kW in all (a different class, for
  comparison).
- Wildcatter ROADMAP Phase 3: three-phase receptacles and plugs (L21-30, L22-30, IEC 60309 red).

## As built (L2, 2026-11-09, branch `l2-l3-trains`)
Decisions in [../questions/l2-l3-trains.md](../questions/l2-l3-trains.md) (#1-#13, #18-#20).
- **Code:** `haul.Battery` (the numbers, unit-tested in `BatteryTest`); the `battery_locomotive` entity type on `MineVehicleEntity`;
  `item.LocoBatteryItem` (`loco_battery`, its usable Wh as item data, a charge bar and tooltip); `block.BatteryChargerBlockEntity`
  (`battery_charger`, a `Machine` part); `client.BatteryLocomotiveModel`.
- **As the spec, except:** a placed locomotive has no battery until one is put in (a crafted battery holds 10 %); the flat costs about
  1.5 kWh a km (the 85 % applied, #4); sneak-clicking takes the battery out only when the locomotive is held and standing, otherwise
  it reverses (#7); the charger is Wildcatter's `DriveLoad` at PF 0.95, so it picks up its 20 kW over about 10 s (#8); it fits NEMA
  L22-30 and IEC 60309 red 32 A (#9).
- **Measured in the game tests:** 0.20 m/s loaded up the 1-in-2 (0.204); 7.86 kWh per 50 m of height; 18.0 kW into the battery from
  20 kW drawn; FLAT holds on the rack.
- **Tests:** NeoForge `battery_loco_run`, `battery_loco_climb`, `battery_loco_flat`, `battery_charge` (a real 500 kW set feeding the
  receptacle; it also charges a battery locomotive standing beside it), `train_models_l2_l3`; the client film `battery_bottom_dump`.
