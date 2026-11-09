# Ventilation of the drive (main fan, vent duct, the TBM's duct cassette)

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed wave **L1**. The choices are listed
in [../questions/specs-tailings.md](../questions/specs-tailings.md) (Later section).

ROADMAP "Later": ventilation of the drive (old 3c, never built). A drive a TBM bores has fresh air only as far as something pushes it.
Real tunnels and mine drives are ventilated by a **forcing fan at the portal blowing through a flexible duct** to near the face, the
stale air returning along the bore. The TBM's back-up carries a **duct cassette** that pays out duct as it advances.

**Real counterparts:**
- Axial forcing fans of 1-2 m diameter at a tunnel portal, flexible PVC lay-flat duct of 1.0-1.4 m, duct cassettes of 100-200 m on
  the TBM back-up *(sizes proposed from practice, unverified)*.
- Airflow rules: **0.06 m³/s a kW of diesel** (Australia; Canada 0.045-0.092, Chile and South Africa 0.063); minimum velocities of
  **0.15-0.3 m/s** in working places; auxiliary air within about 5 m (UK) of a development face.
- McPherson, *Subsurface Ventilation Engineering* (2nd ed., 2009) *(unverified edition)*.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

## What needs air, here
- **The drive is all-electric:** the TBM, the trains and the gantries burn no diesel, so the 0.06 m³/s a kW rule does not apply. What
  real drives still ventilate for is **people, heat from the machines, dust from the cutterhead and rock gases** (CO2 from sulfide
  oxidation; **radon** where the rock holds uranium: tuff and sandstone in `Rock`).
- **The game's rule** *(proposed)*: the bore needs **0.3 m/s** of air across its 36 m² clear section, **10.8 m³/s**, from the portal to
  within **30 m behind the cutterhead** (the back-up's tail, where people work).
- **Diesel underground** (none today) would add 0.06 m³/s a kW, if a later wave brings a diesel loader below.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `vent_fan` | 3 x 3 x 5 | An axial forcing fan with its silencer and inlet bell, **1.8 m** across, **15 m³/s at up to 3 kPa**, **75 kW** *(proposed: 15 x 3,000 / 0.6)*, the world's standard industrial low voltage. Placed at the portal (in fresh air), its outlet on its south face |
| `vent_duct` | 1 x 1 x 1 a block, a run of any length | **1.4 m** flexible lay-flat duct hung from the crown, drawn round, **laid like pipe** (multiblocks README). Placed by hand on the surface run and in the bore |
| `duct_cassette` | on the `reel_gantry` (no cells of its own) | A cassette of **100 m** of duct *(proposed)* carried on the TBM's reel gantry, paying duct out as the machine advances. A `vent_duct_cassette` item in the cab's slots refills it; real crews change cassettes on the back-up |
| `booster_fan` | 2 x 2 x 3 | An in-line booster fan in the duct, **30 kW** *(proposed)*, for long drives |

## How it runs
- **The duct's resistance** *(proposed, Darcy-Weisbach, friction factor 0.02, with leakage)*: at 13-15 m³/s a 1.4 m duct carries air
  at 8.5-9.7 m/s, losing about **65 Pa a 100 m**, and leaks about **1 % a 100 m** *(proposed, good duct)*. One fan delivers 10.8 m³/s
  through about **3 km** of duct; past that, a booster fan in the line adds its 1.5 kPa *(proposed)*.
- **Air at the duct's end** is what the fan delivers less the leakage, at the pressure left. If it is at least 10.8 m³/s, the bore from
  the portal to the duct's end plus 30 m is **fresh**.
- **The TBM** pays out its cassette's duct as it advances, joined to the duct behind it. With no duct reaching within 30 m behind the
  back-up (the cassette empty, or no fan), it stops: **NO VENTILATION** *(proposed: real TBMs interlock their cutterhead on the gas
  monitors)*. Its screen reads the air at the back-up in m³/s.
- **Players in stale air** (in a drive further than 30 m past the last fresh point, or with the fan off) get **Mining Fatigue I** after 30
  s and **Weakness I** after 2 minutes, cleared within 10 s of fresh air *(proposed: a game rendering of heat and CO2)*. In tuff or
  sandstone drives the same stale air reads **RADON** on the cab and on a `gas_detector` item *(proposed)*.
- **Cave air:** a bore that breaks into a natural cave is still ventilated as the bore; the cave is not.
- **PLC points:** the fan's RUN, RUNNING, FLOW (4-20 mA over 15 m³/s); the TBM's junction adds AIR AT BACK-UP (4-20 mA).
- **Breaking a duct block** stops the air beyond it; the TBM stops after 60 s *(proposed: time for the crew to walk out)*.

## Left out (to ROADMAP)
- A full ventilation network solver (Hardy Cross); return airways; regulators and doors. A shaft as the return is in
  [shaft-and-hoist.md](shaft-and-hoist.md), Later.
- Heat from the rock at depth, refrigeration.
- Real gas: methane (not in hard-rock drives), blasting fumes (no underground blasting).
- Diesel particulate.

## Game tests to add
- `vent_fresh`: a fan and 200 m of duct keep a TBM running; with the fan off it stops NO VENTILATION.
- `vent_cassette`: the TBM pays out 100 m of duct and stops when the cassette is empty.
- `vent_stale_player`: a player 60 m past the duct's end gets Mining Fatigue after 30 s, cleared in fresh air.
- `vent_booster`: 3.5 km of duct needs a booster fan to deliver 10.8 m³/s.

## The wave plan
- **L1**, the first of the Later waves *(recommended order in the questions file)*: it changes the TBM players already use.

## Sources
- Compilation of national diesel airflow rules: Australia 0.06, Canada 0.045-0.092, Chile 0.063, South Africa 0.063, Spain 0.066 m³/s a kW;
  Colorado 0.047 with 0.15 m/s minimum in working places. Queensland guidance: 0.3 m/s minimum in roadways.
- UK practice: primary air within 5 m of a heading face (lecture notes on secondary ventilation, duct outlet within 3√S).
- Fan, duct and cassette sizes: *(proposed)* from tunnelling practice, to be checked against a fan maker's sheet.
