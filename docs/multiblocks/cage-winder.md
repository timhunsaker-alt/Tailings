# The cage winder (a single-drum service winder for the shaft's cage)

Tim, 2026-11-23: "Add a cage winder." Until now the cage rode on the skip hoist's rope with no drum of its own
(shaft-and-hoist.md, "As built: the cage"): the skips stood while it ran, and nothing turned for it. Real shafts with skips and a cage
run the cage on its own **service hoist**: its own drum, rope and motor, over its own sheave in the headframe. This is that part.
Written before it was built; "As built" at the end says what changed on the way.

## The real counterpart, and its figures
A **single-drum personnel winder**: one drum, one rope, one conveyance, no counterweight. NSW's drum winder guideline works one
through (MDG 33, *Guideline for Design, Commissioning and Maintenance of Drum Winders*, NSW Department of Mineral Resources, Issue A
Rev 4, 1998, Examples 4.1 and 4.5):

| | MDG 33's example | This mod's cage winder |
|---|---|---|
| Carries | 20 persons (88 kg each with their kit) | 4 people, or 9 stacks of materials |
| Depth | 400 m | the shaft's, collar to the flask's landing (120 m in episode 7) |
| Cage | 4,000 kg | 1,500 kg *(proposed: a 2 x 2 x 3 m cage for four)* |
| Rope | 36 mm, 5.49 kg/m | 26 mm, 2.7 kg/m *(proposed; a 26 mm six-strand rope weighs about that)* |
| Speed | **4 m/s**, creeping in and out of the guides at 1 m/s | **4 m/s** (kept), creep 0.5 m/s |
| Acceleration | 1.5 m/s² | 0.75 m/s² (the skips', kept) |
| Drum | about 2.5 m (30.3 rpm at 4 m/s) | **2.5 m**, single, on its own shaft |
| Power | gearbox rated for a 410 kW design duty | **160 kW** motor *(below)* |

Other sources for the figures:
- **Speed.** de la Vergne, *Hard Rock Miner's Handbook*, Edition 5 (Stantec, 2008), chapter 13, "Hoist Speed": the economic speed of
  a drum hoist is about 0.405 √H m/s (H the wind, m), and "the desirable speed for cage service is approximately 2/3 of the optimum
  speed calculated for a skip hoist". At 120 m that is 4.4 m/s; two thirds of the skips' optimum over their 138 m is 3.2 m/s. The
  cage's 4 m/s sits between the two, and is MDG 33's figure. Kept.
- **Why its own motor, and a big one for the load.** ABB's mine hoist pages: single-drum hoists are common for personnel and
  equipment and for shallow shafts, and "a single drum hoist requires a larger motor than the friction or double drum hoist functional
  equivalent", having no counterweight. The Handbook's power table for a 2,500 t/d mine (Table 23-2) lists a skip hoist of 3,500 hp
  and a separate cage hoist of 2,000 hp: the cage's own winder at about 57 % of the skip hoist.
- **Drum to rope.** The Handbook, chapter 13: guidelines put a drum at 80 times its rope (72:1 at lower speeds). 2.5 m on 26 mm is
  96:1.

## The motor
Nothing balances the cage, so the motor lifts the cage, its load and its hanging rope:
(1,500 + 1,000 payload + 135 m x 2.7 kg/m) x 9.81 x 4 m/s / 0.85 = **134 kW**, and 10 kW more while it accelerates at 0.75 m/s². A
**160 kW** motor (a standard rating, about 10 % over). That is 36 % of the skip hoist's 450 kW, a little under the Handbook's 57 %
(its cage carries far more people). The payload is the deck's rated 1 t *(proposed)*; four people are about 0.35 t.

## The part
- **`cage_winder`, 6 x 6 x 8** (`tools/models/shaft.py`): a small winder house, its north wall low and its roof open over the drum
  where the rope leaves; the 2.5 m drum on its shaft between two bearing pedestals, its brake, the 160 kW motor behind it on its
  gearbox, the control desk; the terminal box on the west face of the main cell. Medium-voltage tier, as the hoist house.
- **Where it stands:** behind the headframe on its rope side (the south, as the hoist house), within the hoist's 60 m rope reach,
  in or beside the hoist house's line: up to 20 m east or west of the shaft's centre line. It finds the headframe, its cage and the
  bottom landing (the measuring flask) as the hoist does.
- **Its sheave:** a **2.5 m** cage sheave in the headframe, under the skips' sheaves at the sheave deck, over the cage's compartment
  (x 4, its axle 26.95 m up, 7.25 m south in the headframe's frame), so the rope comes up the cage compartment's centre, over the
  sheave and down at a slant to the cage winder's drum. The deck is open where the rope comes through.
- **Drawn turning:** its drum's rope clamps and flange spokes, and the cage sheave's spokes, turned as far as the cage has moved
  (on the client, from the cage's height, as the skips' drums and sheaves are).

## Running it
- The same calls as before: a rider's bell, the landings' call buttons (a redstone signal reaching the headframe or the flask),
  sneaking with an empty hand on the headframe or the flask. The bell rings 5 s, then it winds at 4 m/s with the skips' acceleration
  and creep.
- **The skips no longer wait for the cage, nor it for them:** two winders, two ropes, two compartments.
- **Power:** its own 160 kW from the medium voltage, drawn only while it winds; the brakes hold the cage where it is with no power.
- **Reads:** NOT BUILT (no headframe in reach, no cage hung, no flask, the cage's compartment blocked), "CAGE at the collar", "CAGE: the
  bell, going down", "CAGE at 3.8 m/s, 54 m below the collar", "STOPPED: the brakes hold the cage (no power to its motor)".
- **With no cage winder** the cage hangs where it is and calls do nothing; the hoist house no longer runs it. A world saved before
  the cage winder (the cage's state was on the hoist) finds the cage at the collar when a cage winder is first built.

## Tests
- `HoistingTest`: the cage winder's figures (its lift power under its motor; the cage at 4 m/s; its drum's turns over a trip).
- NeoForge `shaft_cage_winder`: the cage winder winds the cage 60 m down and back up while the skips keep winding; its drum turns
  as far as the rope (60 m on a 2.5 m drum: 7.6 turns); with no power the brakes hold it.
- NeoForge `shaft_cage` and `shaft_cage_button`: as before, now on the cage winder.

## Sources
- NSW Department of Mineral Resources, MDG 33, *Guideline for Design, Commissioning and Maintenance of Drum Winders*, Issue A Rev 4
  (1998), Examples 4.1 (rope selection: 20 persons, 400 m, cage 4,000 kg, 36 mm rope) and 4.5 (duty cycle: 4 m/s, 1.5 m/s², creep
  1 m/s; 30.31 rpm; 410.4 kW design): https://www.resources.nsw.gov.au/sites/default/files/documents/mdg-33-part-2-of-2.pdf
- NSW Resources Regulator, *Technical reference guide: Powered winding systems, Part 3: Vertical shaft winders*, "Single drum
  personnel and material winders" ("typically slow speed winders where high volume is not needed").
- J. de la Vergne, *Hard Rock Miner's Handbook*, Edition 5, Stantec, 2008: chapter 13 "Hoist Speed" (0.405 √H; cage service at
  2/3 of the skip optimum; 80:1 drum to rope) and Table 23-2 (skip hoist 3,500 hp, cage hoist 2,000 hp).
- ABB, "Drum hoist" (mine hoist types): single-drum hoists for personnel and equipment, and their larger motor.

## As built (fix2-hi07, 2026-11-23)
As specified above, with these details:
- **Code:** `shaft.CageWinder` (the cage's state, moved out of the skips' `Winder`; a hoist saved with the cage's fields still loads),
  `Hoisting` CAGE_KG, CAGE_PAYLOAD_KG, CAGE_ROPE_KG_PER_M, CAGE_DRUM_DIAMETER, CAGE_SHEAVE_DIAMETER, CAGE_MOTOR_WATTS, `cageLiftWatts`
  (142 kW over 120 m at full load), `cageDrumDegrees`; `ShaftLayout.findCage` (the nearest headframe whose rope side it stands on, the
  flask's landing, the cage's compartment clear); `Shaft.CAGE_WINDER`; `Machine.CAGE_WINDER` (6 x 6 x 8, 160 kW, medium voltage).
- **Drawn:** the cage winder in `tools/models/shaft.py`; the 2.5 m cage sheave and the deck's rope opening in the headframe (the only
  change to the headframe's model); `PlantRenderer.cageDrum` and `cageSheave` turn with the cage's height on the client, with a
  yellow mark on the drum's flanges and on one spoke of the sheave so the turn reads; `SkipRenderer` runs the cage's rope up its
  compartment, round the top of its sheave and down to the drum.
- **Sound:** the mine hoist's loop while it winds.
- **Recipe:** iron blocks round a copper block, the top middle open.
- **Tests:** `HoistingTest.theCageRidesAtFourMetresASecond`, `theCageWinderLiftsItsCageOnItsOwnMotor`, `aSkipWinderSavedWithACageStillLoads`;
  NeoForge `shaft_cage`, `shaft_cage_button` (now on the cage winder) and the new `shaft_cage_winder` (60 m down with the skips winding
  at the same time, the drum 7.64 turns, braked with no power, back up with the drum wound in).
