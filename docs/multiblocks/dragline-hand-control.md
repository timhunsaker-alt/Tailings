# The dragline by hand, and highwall and spoil failures (DRAFT, Wave B, 2026-11-19; not approved)

Follows [dragline.md](dragline.md) (approved 2026-11-09, built in P6) and closes two of its Left out lines: **hand control of swing,
drag and hoist**, and **highwall and spoil-pile failures**. Questions for Tim are at the end and in
[../questions/dragline-hand-control.md](../questions/dragline-hand-control.md). A figure marked *(proposed)* is a textbook value, a
scaled value or a proposal; *(unverified)* is from memory, to be checked before approval.

**In one look:**
- A player in the cab takes the levers by touching them: A/D swing the house, W/S drag in and pay out, jump hoists, sprint lowers.
  The bucket hangs where its two ropes put it, fills when dragged through the bank, and dumps when the drag is paid out with it hoisted.
  Getting out hands the machine back to its job, which carries on from where the bucket is.
- With the world rule `slope_failures` on, the dragline's highwall and its spoil pile can fail. The highwall is checked by a real
  slope formula (height, slope, ground, water, blast damage); the spoil by its height over a weak or wet floor. A slope going bad
  creeps first: survey prisms move, cracks open on the bench, loose ground ravels off the face; then it slides, as a fast flowing
  pour into the cut. A slide that takes the ground from under the tub wrecks the dragline.

## Real counterparts and sources
- **Operating a walking dragline:** the operator works drag and hoist on two hand levers and swing on a lever or foot pedals
  *(unverified: layouts differ by maker and age)*. The bucket is filled by dragging it toward the machine through the bank, carried by
  hoist with the drag held tight (the dump rope keeps it level), and dumped by paying out the drag so the dump rope goes slack and the
  bucket tips. The house's inertia is large: a good operator starts braking the swing well before the dump point. SME *Mining Engineering
  Handbook*, 3rd ed. (2011), dragline stripping.
- **Swing assist:** automated dragline swing, trialled in Australian coal from the late 1990s (CSIRO and CRC Mining, Corke and others),
  lets the operator teach dig and dump points and take the levers back at any time by moving them. That is this spec's AUTO and HAND.
- **Highwall stability:** Culmann's planar wedge (1866), in Taylor, *Fundamentals of Soil Mechanics* (1948) and Das, *Principles of
  Geotechnical Engineering*: the critical height of a slope at angle β in ground of cohesion c, friction angle φ and unit weight γ is
  H_c = (4c/γ) · sin β · cos φ / (1 − cos(β − φ)), the failure plane through the toe at (β + φ)/2. Wyllie and Mah, *Rock Slope
  Engineering* (4th ed., 2004): water pressure, blast damage and tension cracks. Read and Stacey, *Guidelines for Open Pit Slope Design*
  (CSIRO, 2009): monitoring, prisms, slope radar, trigger action response plans.
- **Spoil failures:** spoil piles stand at their angle of repose but fail on weak floors (clay, fireclay, a wet seam floor) and with
  water ponded at the toe, flattening to about 20-28° *(unverified)*; Bowen Basin strip mines, Simmons and McManus, "Spoil pile
  instabilities with reference to strip coal mines in Australia" *(unverified citation)*.
- **Warnings and prediction:** tension cracks open behind the crest before a slide; survey prisms read by a robotic total station show
  the slope's speed. Fukuzono's inverse-velocity method (1985): as a slope accelerates toward failure, 1/velocity falls in a near
  straight line to zero at the time of failure, so the trend predicts the slide.
- **Rules:** US 30 CFR 77 Subpart K (ground control at surface coal mines: highwalls examined, loose ground removed, no one under an
  unsafe highwall) and 30 CFR 56.3130-56.3200 (wall, bank and slope stability at surface metal and non-metal mines).

## Part 1: hand control

### Taking the levers
- A player sits in the cab as now (front left of the house). **Touching any control key takes the machine into HAND at once,** in
  whatever it was doing: the ropes and the swing stop following the job and follow the keys from where they are. The status reads
  HAND. As real swing assist does, no button is needed.
- **Sneak** gets out, as in every Tailings vehicle. Getting out hands it back to **AUTO** with its job (below). **Sprint held 2 s with
  no other key** hands it back while staying seated (the action bar says AUTO).
- It must be running (RUN, lifted, power on its cable) for the keys to move anything; stopped, STOPPED, DONE or with no power the keys do
  nothing and the action bar says why. One player drives; another clicking the machine reads its readout, as now.
- The keys come from the server's copy of the rider's input (`getLastClientInput`), as the trucks and dozers are driven: **vanilla keys
  only, no new key bindings** (Q6).

### The controls
| Key | Lever | What it does |
|---|---|---|
| A / D | swing | turns the house left / right about the tub's centre, with inertia (below) |
| W | drag in | winds the drag rope in: the bucket comes toward the machine; through the bank it digs |
| S | drag out | pays the drag rope out: the bucket swings out toward plumb under the boom point; hoisted and loaded, it **dumps** |
| jump | hoist up | winds the hoist rope in: the bucket rises |
| sprint | hoist down | pays the hoist rope out: the bucket lowers; on the ground the rope goes slack |
| none | brakes | drag and hoist hold where they are; the swing brakes (it coasts to a stop) |

### Rates and limits *(proposed, the 24-34 m³ class; unverified against a sheet)*
| | |
|---|---|
| Swing | up to **9°/s** (1.5 rpm), speeding up and braking at **1°/s²**: a 90° swing takes about 19 s, as the job's 17 s out and 20 s back |
| Swing inertia | letting go of A/D brakes at 1°/s²: from full speed the house coasts 40° (Q8). Pressing the other way brakes twice as hard |
| Hoist | **2.5 m/s** empty, **1.5 m/s** loaded |
| Drag | **2.5 m/s** in or out, free; through the bank the bank holds it to **0.33 m/s** (below) |
| Hoist limit | the bucket stops **4 m below the boom point** (the hoist's limit switch: no two-blocking) |
| Drag limit | the bucket stops **15 m from the tub's centre** (it would hit the tub) |
| Reach | **65 m** from the centre, as the job; **35 m** below the tub |
| Power | the machine's generator sets give **6 MW** at most: drag through the bank 4.8 MW, hoist loaded 4.8 MW, hoist empty or drag free 1.2 MW, swing 1.2 MW speeding up; asking more than 6 MW slows every motion in proportion, so the outlet's breaker (7.5 MW for 1 s) is never tripped by the levers. 0.3 MW standing |

### Where the bucket hangs
- The bucket hangs in the boom's plane at the meeting of its two ropes: **the hoist rope from the boom point** (72 m out, 44 m up) and
  **the drag rope from the fairlead** at the house's front (8.5 m ahead of the centre, a metre up). With the drag paid out farther than
  it reaches, the drag rope is slack and the bucket hangs plumb under the point. Plain Java (`pit.DraglineRopes`), unit-tested.
- The bucket does not swing out of the plane like a pendulum when the house swings (left out). It does not go into blocks: lowered onto
  the ground, it rests there and the hoist goes slack; swung into a block above the ground it stops the swing (a thud, no damage).

### Digging
- With the bucket's sole on the ground and the hoist slack, **W drags it through the bank:** each metre it moves, it cuts the layer it
  sits in, 5 wide and 1 deep, along its path. The bucket is full at **25 bank blocks or 55 t** (as the job), which is 5 m of drag at
  0.33 m/s: **15 s at 4.8 MW**, the job's fill time.
- It digs what the job digs: soil and soft ground whole, muck by the quarter, grass and snow stripped with the topsoil. **Rock stops it**
  (the bucket skids; "hard: blast it first"), **ore stops it** (the seam is left for loaders and trucks, as the job; digging the seam is
  left out), and so does any machine's block.
- **Anywhere in reach**, not only in the set strip: a player may dig a ramp, a key cut or the box cut where they like.

### Carrying and dumping
- **Hoisted with the drag held** (no S), the bucket carries its load level.
- **S with the bucket clear of the ground and loaded dumps it:** the bucket tips and the load pours from where it hangs, through the
  pour service (`pit.Pouring`): 25 blocks fall in 1 s and set over 1.5 s, by the settle rule, where they land. On a truck's body it loads
  the truck to its mass, the rest spills (as a shovel). A load dumped on a player harms them as a slide does (below).
- **S with the bucket empty** lets it swing out toward plumb under the boom point (how an operator places the bucket far out). Throwing
  the bucket past the point is left out.
- A dump counts as a **cycle** (CYCLES pulse, bank m³), whoever dug it.

### What the job does when a player takes over, and when they give it back
- **Taking over:** the job stops planning; nothing is undone. A bucket half filled stays half filled; a swing under way coasts on the
  player's brakes, not the job's.
- **Giving back (AUTO):** the job carries on from the bucket and the swing as they are, at the job's rates: loaded (any load), it hoists,
  swings to the cast target and dumps; empty, it swings back to its next dig cell (found again from the world, top down, as now) and
  drags. What the player dug in the strip is not dug again; what they dumped in the strip is dug again (it is in the strip). The job's
  cycle is now driven through the same ropes and swing as the levers, with the same rates, so the hand and the job move one machine
  the same way; its 55 s cycle and its 6 MW / 1.2 MW draw are kept.
- **Walking stays the job's** (Q7): in HAND the machine does not walk; handed back with its reach dug out, it walks as now.
- **Readout in HAND (action bar):** HAND, swing angle, bucket out (m from the centre) and height (m over the tub), load t and %, hoist
  and drag speeds, kW, and the slope warning ahead if any (CRACKS, MOVING).
- **PLC:** a new point, **MACHINE HAND**, on while a player has the levers, added after every point the switch house already has, so no
  point's number changes. **MACHINE RUN off stops it in HAND too** (the brakes set, the action bar says "stopped by the plant"): a stop
  always wins, as every interlock. RUN on does not move the levers.

## Part 2: highwall and spoil failures (world rule `slope_failures`)

### The world rule
- `slope_failures`, an `OptInRules` world rule set by `/tailings rule slope_failures true`, shown on the World Rules screen.
  **Off in every world, new ones included** (Q1), as `paste_wall_breach`: a slide can wreck a 1,750 t machine, so a player turns it on.
- Off: nothing in this part runs; prisms and the survey station can still be built and read, and read STABLE.

### What is checked (Q5)
- **The dragline's highwall:** the strip's near edge, the side the dragline stands on, along the strip's length, in **sections of 10 m**.
- **The dragline's spoil:** the ground the dragline cast onto, in the same 10 m sections along the strip.
- Each strip is kept in a saved record (`SavedData` `Slopes`, by strip) from the moment its dragline digs, so its highwall and spoil
  are still checked after it walks on, is set down or reads DONE. A section is dropped when its cut is filled to the crest (the next
  strip's spoil covers it, as in real strip mining), or **7 game days** after its strip is DONE *(proposed)*.
- Nothing else is checked: shovel benches, truck dumps, vanilla cliffs and players' own holes never fail.
- A section is checked every **10 s** while its chunk is loaded; an unloaded section waits.

### The highwall's batter: a new panel setting
- The strip panel gets **Highwall**: vertical, 2 in 1 (63°), 1 in 1 (45°) or 1 in 2 (27°). The job digs the highwall at that slope, the
  width measured at the crest, so the face leans into the strip and the floor narrows; the dragline's stand-back (12.5 m from its
  centre to the crest) is kept.
- **Default:** 2 in 1 when `slope_failures` is on, vertical when it is off (so a world without the rule digs as it does now and
  `dragline_strip` still moves 1,500 m³). A saved job from before reads vertical.

### The highwall's factor of safety
For each section, from the world:
- **H**, the height: the bench top 3 m behind the crest less the top of whatever stands at the toe (the cut floor, or spoil or muck
  lying against it: a toe buttress lowers H).
- **β**, the face's slope: from the crest and toe columns.
- **c, φ, γ:** averaged by count over the blocks in the failure wedge (below):

| Ground | c (kPa) | φ | γ (kN/m³) | H_c vertical | 2 in 1 | 1 in 1 |
|---|---|---|---|---|---|---|
| Sand, red sand (damp) | 5 | 32° | 17 | 2 m | 6 m | 28 m |
| Gravel | 2 | 38° | 19 | 1 m | 3 m | 32 m |
| Dirt, coarse and rooted dirt, grass block | 20 | 25° | 18 | 7 m | 17 m | 47 m |
| Clay, mud | 30 | 18° | 19 | 9 m | 18 m | 39 m |
| Muck, spoil | 5 | 37° | 18 | 2 m | 8 m | 65 m |
| Stone and any rock (jointed rock mass) | 200 | 35° | 26 | 59 m | 187 m | stable |

  *(proposed: textbook mid-range values for each ground; a modded block by its tags, stone-like as rock, else as dirt.)*
- **H_c** by Culmann's formula; **FS = H_c / H** *(a simplification: real factors of safety are a ratio of strengths; this one reads
  the same at 1 and moves the same way)*. Plain Java (`mine.Slope`), unit-tested against the table.
- **Water** lowers it: water blocks within the wedge or within 8 m behind the crest, at or below the crest's level (a pond, a river,
  a flooded neighbour), or **rain** on the section for 10 min of game time and for one game day after: φ is taken as
  atan(0.7 · tan φ) *(proposed: about a pore pressure ratio of 0.3)*. A dirt wall 2 in 1 then stands 13 m, not 17. Draining the water
  (by bucket, pump or anything) brings it back.
- **Blast damage:** a blast (P3) with a hole within **15 m** of a section's crest halves c in that section for good *(proposed: the
  damage zone behind a production blast)*. A rock highwall vertical then stands 30 m, not 59: so a highwall in rock is blasted with
  care, or laid back. A blast within **100 m** takes **0.05 off FS** for one game hour (vibration) *(proposed)*. Presplit and wall
  control blasting, which would avoid the damage, are left out.

### The spoil's factor of safety
- The spoil pile already stands at the settle rule's 37°, its angle of repose, and on a firm dry floor it **never fails**.
- **Weak floor:** clay or mud among the cells under the pile's lowest layer within 10 m of its toe. **Wet toe:** water against the pile's
  toe (the old cut flooded), or rain as above.
- **Limit height** *(proposed)*: weak floor or wet toe **24 m**, both **12 m**. **FS = limit / H**, H the pile's crest over the lowest
  ground under it. Plain Java in `mine.Slope`, as the highwall's.

### Stages: STABLE, CREEP, FAILING, SLID
| FS | Stage | What the world shows |
|---|---|---|
| 1.3 or more | STABLE | nothing; prisms read under 0.1 mm/h (noise) |
| 1.0 to 1.3 | CREEP | prisms move steadily, up to **2 mm/h** at 1.0. Under **1.15**: **tension cracks** open on the bench and loose ground **ravels** off the face (dust, a muck quarter rolling down now and then, a creak) |
| under 1.0 | FAILING | the slide's clock starts (below); prisms accelerate (1/velocity falling in a straight line to zero at the slide); cracks widen; ravel and creaks come faster |
| | SLID | the wedge slides (below); the section is checked again from its new face |

- **The clock** *(proposed, Q2)*: from FS falling under 1, the slide comes after **one game day at 0.95**, shortening to **2 min at 0.7**
  (as the square of FS − 0.7 over 0.25); under 0.7 within **10 s** (a wall that cannot stand at all, as vertical sand, ravels down as
  it is dug). Each check takes the sooner of the time left and a fresh clock, so a wall dug deeper, wetted or blasted comes down sooner.
  FS back over 1 (water drained, the toe buttressed) stops the clock; over 1.15 the cracks stop growing. The clock scales with
  Wildcatter's time scale, as the paste cure does.
- **Tension cracks:** the top block of the bench, where the failure plane meets it (at H · (cot θ − cot β) behind the crest, θ the
  plane's angle), becomes a `tension_crack` (dirt with a dark crack through it; it drops dirt; on spoil, muck stays muck and the crack
  is drawn as dust and particles only), in a line along the section, growing a block a minute while CREEP or FAILING.

### The slide
- **The wedge:** every block of the section between the face and the failure plane (through the toe at θ = (β + φ)/2), over the section's
  10 m, the ends tapering 2 m into the next sections. For the spoil: the part of the pile above a line at 1 in 2 from its toe.
- **It runs as a pour:** each block of the wedge is let go loose (soil as itself, rock as muck at 4 quarters a bank block, the swell) and
  placed by the settle rule, through `pit.Pouring` at a faster run, **6 m/s** *(proposed; real slides run at 1-10 m/s)*, setting
  rim first over 2 s. So a slide looks like the flowing dump: one thick sheet running into the cut, then set. A spoil slide's blocks
  settle by a **wet settle rule**, a step of 2 to the next column or 2 up in a run of 4, so the slid spoil stands at about **1 in 2**
  (27°), as real failed spoil. Blocks left with nothing under them fall as the settle rule lets them.
- **Sound and dust:** `highwall_slide` (a long low rumble, 6-10 s), dust along the crest and the toe. Players within 200 m hear it.

### What a slide does
- **The dragline:** if the wedge takes the ground from under **a third or more of its tub's cells**, it goes down with the slide and is
  **wrecked** (Q3): it breaks where it lands into its eleven parts' items, its cable and reel, and its bucket's load (poured where it
  lands), as a wrecked haul truck breaks into its item and load. A player in its cab is thrown clear and harmed as below. **Less than a
  third:** it stops, reads GROUND GONE, and will not run or walk until the ground under its tub is made up (a dozer or by hand), then RUN.
  Its bucket, if lying in the cut under the slide, is **buried**: it reads BUCKET BURIED and must be dragged out (in HAND, or the job
  drags it out at its rate, the buried blocks counting as the next bucket).
- **Other machines:** a truck, dozer, loader or shovel on the wedge goes down with it and is wrecked if it lands buried over half its
  height (as a truck over a crest); in the run-out it is pushed and stopped, BLOCKED.
- **Players and mobs:** caught in the wedge or the run-out: **8 damage** (as a truck's hit), more with the drop as vanilla fall damage;
  a block setting in their cell buries them, and vanilla suffocation does the rest until they dig out. A player in creative is not
  harmed.
- **Cables:** a trailing cable is counted, not placed (P5), so a slide does not cut it.
- **Ore:** ore blocks in the wedge slide as ore muck, so a slid seam is still plant feed, dug from the pile by loaders.

### Warnings: survey prisms and the survey station
- **`slope_prism`:** a small block (a survey prism on a short pole) placed on a bench or spoil crest. It belongs to the section under it
  (or reads NOT ON A SLOPE). Clicked with the existing **survey book** it reads: "Prism 3: 14 mm in the last hour, 6 mm/h and rising"
  and the stage as a crew would see it (STEADY, CREEPING, ACCELERATING), never FS: FS is hidden, as in real life. An op's
  `/tailings slope` prints FS for a section (for testing).
- **`survey_station`:** a robotic total station on a pillar, a block with a Wildcatter `ControlDevice`. It reads every prism it can see
  (in line of sight, within **500 m** *(proposed; real robotic stations read prisms to 1-2 km)*) each minute, and its screen lists them
  with their speed and a velocity chart of the last game day, and a **predicted slide time** from the inverse-velocity trend once a prism
  accelerates. **PLC points:** ALARM (on when any prism passes the set speed), WORST SPEED (4-20 mA over 0-50 mm/h), PRISMS (how many
  it reads), and ALARM SET (an analog output, 0-50 mm/h, default **10 mm/h** *(proposed; a common first alarm level)*). Wired to the
  switch house outlet's MACHINE RUN, the alarm stops the dragline: the real trigger action response plan.
- **The dragline itself** reads CRACKS on its cab readout and the action bar when a section within its reach has cracks.
- **The job on cracks** (Q4): a panel setting **Stop on cracks**, **on** by default: in AUTO the dragline finishes its cycle and stands,
  WARNING: CRACKS, when cracks open in a section within its reach; off, it digs on. In HAND the player decides.
- **What a player can do about it:** lay the highwall back, keep strips shallower, keep water off the bench and out of the cut, keep
  blasts away from the crest, leave spoil or muck against the toe as a buttress, stand the dragline farther back (a wider stand-back is a
  question for later; the strip's start sets it now), watch the prisms, and get out from under a moving wall.

## Blocks, items, recipes *(placeholder recipes until the art wave, as P5 and P6)*
| Block / item | Recipe *(proposed)* | Notes |
|---|---|---|
| `slope_prism` | glass pane, copper ingot, 2 sticks: 4 | survey prism on a pole; small model |
| `survey_station` | spyglass, Wildcatter steel plates, Wildcatter PLC I/O card, redstone | robotic total station on a pillar, with its solar panel |
| `tension_crack` | none: a failing slope makes it | drops dirt |
| strip panel | (existing) | new settings: Highwall, Stop on cracks |

No new dragline part, no voltage written anywhere (the dragline's power is in MW from its outlet, as now).

## Progression and quest book
- Hand control comes with the dragline (no new item). The prism and the station unlock with the dragline's quest, after the survey book
  (P2a) and the blasting machine (P3).
- **Quests** (in the dragline's chapter, after its milestone; quests.md format): **"On the levers"**, cast ten buckets by hand
  (milestone `tailings:dragline_hand_cast`); **"Watch the wall"**, place three prisms and a survey station reading them. Both show
  whether the rule is on or off.

## Sounds and art
- **Sounds:** `highwall_creak` (deep ground groan, CREEP and FAILING), `ravel` (a short trickle of stones), `highwall_slide` (long rumble),
  `survey_alarm` (the station's two-tone beep), `dragline_swing_brake` (an air brake's hiss when the swing brakes), the drag and hoist
  motors pitched by their load (from the existing dragline hum).
- **Art (Ludo):** `tension_crack` (dirt and grass top with a dark crack), `slope_prism` model and texture, `survey_station` model and
  texture, the station's screen chart. The slide uses the pour's wet sheet, in the slid ground's own texture.

## Game tests (NeoForge scenes) and unit tests
- **Unit:** `DraglineRopesTest` (the bucket at the ropes' meeting; plumb with the drag slack; the limits), `SlopeTest` (the table's H_c
  values, water, blast damage, the spoil limits), `SlopeClockTest` (one day at 0.95, two minutes at 0.7; the clock only shortens).
- `dragline_hand_take` (a fake player's input: W takes it into HAND mid-swing; the job's state is kept; MACHINE HAND on).
- `dragline_hand_dig` (drag through sand: 25 blocks in 15 s at 4.8 MW; rock skids; an ore block stops the bucket).
- `dragline_hand_dump` (hoisted and loaded, S pours the load where the bucket hangs, settled; a cycle counted).
- `dragline_hand_limits` (the hoist limit, the drag limit, the 6 MW share slows two motions).
- `dragline_hand_give_back` (getting out with a full bucket: the job casts it to its target; empty: it drags at its next cell).
- `dragline_hand_plc_stop` (MACHINE RUN off in HAND sets the brakes).
- `slope_rule_off` (rule off: a 10 m vertical sand highwall stands; prisms read STABLE).
- `slope_highwall_creep` (dirt, 2 in 1, dug from 12 to 16 m: CREEP, cracks at the plane's distance behind the crest, a prism's speed).
- `slope_highwall_slide` (dirt vertical 9 m: FS 0.78, FAILING, slides about 2 min later; the wedge's mass all in the cut, none lost).
- `slope_water` (a pond behind the crest turns a 2 in 1, 13 m dirt wall from CREEP, FS 1.28, to FAILING, 0.98; drained, the clock stops).
- `slope_blast` (a blast 10 m from a 40 m vertical rock wall halves c: it fails).
- `slope_buttress` (spoil against the toe lowers H: a failing wall goes back to CREEP).
- `slope_spoil` (a 15 m spoil pile on clay with water at its toe slides to about 1 in 2; on stone, dry, it stands).
- `slope_dragline_wrecked` (a slide under half its tub: the dragline breaks into its parts, cable and load; none lost).
- `slope_dragline_ground_gone` (a slide under a quarter of its tub: GROUND GONE; made up, RUN works).
- `slope_station_alarm` (a station reads an accelerating prism; ALARM over 10 mm/h stops the dragline through MACHINE RUN).
- `slope_saved` (a FAILING section saved and loaded keeps its clock).
- **Client scenes (filmed):** `dragline_hand` (a scripted operator digs, swings with the house coasting, and dumps), `highwall_slide`
  (cracks open behind the crest, prisms on the station's chart, the wall runs into the cut), `spoil_slide` (a pile on a wet clay
  floor slumps to 1 in 2), `dragline_wrecked` (a dragline digging a 20 m vertical dirt highwall: the plane reaches 12.7 m behind the crest, under most of its tub, and it goes down with it).

## Left out (to ROADMAP)
- Walking the dragline by hand (Q7); the bucket swinging out of the boom's plane; throwing the bucket past the boom point.
- Boom and rope damage: tightlining, the bucket striking the boom, rope wear.
- Digging the seam and rehandle by the job outside its strip.
- Failures of shovel benches, truck dumps and any face the dragline did not dig (Q5); wedge and toppling failures in rock (only
  Culmann's plane); a slope's factor from a full limit-equilibrium search.
- Presplit and wall-control blasting; horizontal drains and dewatering bores as their own machines; slope radar.
- A wider stand-back setting on the panel.

## Questions for Tim
Also in [../questions/dragline-hand-control.md](../questions/dragline-hand-control.md), recommended option first.
1. **The world rule's default:** off in every world (opt-in), or on in new worlds and off in existing ones (house rule 9)?
2. **How long a failing wall gives:** about a game day at FS 0.95 down to 2 min at 0.7; several game days (real pace); or a slide
   as soon as FS falls under 1?
3. **A dragline caught in a slide:** wrecked, all its parts given back as items where it lands; wrecked with some parts lost; or
   never wrecked, only stopped (GROUND GONE)?
4. **The job on cracks:** a "Stop on cracks" setting on by default; off by default; or no setting (it always digs on)?
5. **What can fail:** the dragline's own highwall and spoil only; or every pit face and dump too?
6. **The levers' keys:** vanilla keys (W/S drag, A/D swing, jump and sprint hoist); or new rebindable key bindings for two levers?
7. **Walking by hand:** left out (the job walks); or a step key in HAND?
8. **Swing inertia:** real inertia (the house coasts 40° from full speed); or the swing stops when the key is let go?
