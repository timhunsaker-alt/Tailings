# P7: the pit-sized plant line (2026-11-10), decided while Tim was away

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back".
Built on branch `p7-pit-line` (cut from `p5-big-machines`) from the draft specs
[primary-gyratory-stockpile.md](../multiblocks/primary-gyratory-stockpile.md) (P7a), [sag-mill-line.md](../multiblocks/sag-mill-line.md)
(P7b) and [pit-flotation.md](../multiblocks/pit-flotation.md) (P7c), whose own choices are in `specs-tailings.md` (P7-1 to P7-10). Format
as in `README.md` here. **#11, #14, #21 and #27 most need your eye; for P7c, #33, #36, #37 and #40.** Nothing here changes an existing world (#1).

## P7a: the primary gyratory, the coarse-ore stockpile and its reclaim

### 1. Does anything change for a plant or pit already built?
- **Chosen:** no. P7a is nine new parts, a heavy belt and the stockpile block. Three shared pieces of code were touched, each so that
  what exists behaves exactly as before: a belt run is now one kind of belt end to end (there was only one kind before, so every
  existing run is the same run); a haul truck's tip looks for the gyratory's dump pocket as well as the pit hopper; and the muck
  block's placing code takes the block to place (muck, as before, unless it is the stockpile). Muck itself does not roll: only the new
  stockpile block does (#11). The existing scenes for belts, the crushing circuit, the pit hopper, the settle rule, the dozer and the
  playthrough's crushing pass unchanged.
- **Why:** the rule for these waves: a built small plant pays and runs exactly as today; the pit line is new parts.
- **Alternatives:** none needed.

### 2. How the primary stands: a stack, 26 m deep
- **Chosen:** the parts stand one on another, as a real direct-tip gyratory station does: the dump pocket (14 x 8 x 10) on the
  gyratory's top, over its bowl; the gyratory (6 x 10 x 6) on the surge pocket (8 x 6 x 8); the primary apron feeder (3 x 2 x 12) under
  the surge pocket, touching it. The gyratory takes from the pocket standing on it and crushes into the surge pocket under it; NOT BUILT
  names a missing one. The rock breaker stands on the pocket's rim, touching it.
- **Why:** big parts cannot share cells, so the spec's "pocket round the bowl" is the pocket standing over the bowl, its floor's opening
  over the crusher. A real station is a concrete pit 25-30 m deep with the trucks tipping at the top and the apron feeder at the bottom.
- **Alternatives:** the pocket beside the crusher (a side-feed; not how gyratories are fed).

### 3. The pocket is the pit hopper's kind of part
- **Chosen:** the gyratory's dump pocket is the same kind of block entity as P1's pit hopper: 400 t, a truck's whole load or none, the
  part of a bank block under a whole one kept for the next load, soil refused ("not rock: tip it on a dump"), FULL makes the truck wait.
  A truck backed to it tips into it (the hopper search under the tail now finds either). The gyratory draws it a bank block at a time.
- **Why:** the spec says "as the pit hopper does"; one kind of pocket means the trucks, the dispatch office and the readouts need no new
  rules.
- **Alternatives:** a pocket of its own kind with a rock level (no gain).

### 4. Boulders: every 200th hard bank block
- **Chosen:** the gyratory counts the hard bank blocks it takes; every 200th is a boulder that bridges the bowl (P7-8). "Hard" is every
  rock and ore but tuff, sandstone and the soft ores a bucket digs (bauxite, phosphate, heavy mineral sand, bentonite, borax). The count
  is saved with the crusher. BRIDGED stops it until the rock breaker has worked 30 s on it.
- **Why:** 1 in 200 is the spec's figure; counting rather than rolling dice makes it the same in every world and testable, and over a
  shift it is the same rate (a few a shift at 1,000 t/h: 370 blocks an hour, so about 1.9 boulders an hour).
- **Real counterpart:** primaries bridge on slabs and boulders a few times a shift; a pedestal breaker clears them.
- **Alternatives:** a random 1 in 200 (more real, not testable); oversize by rock size from the blast (P3 has no fragmentation model).

### 5. The rock breaker works by itself
- **Chosen:** the breaker works on a boulder whenever its 75 kW motor runs; no seat. With its motor off (no power, or OFF on its HOA)
  the bowl stays bridged.
- **Why:** real breakers are often remote-operated from the crusher's control room; a seat would need a new riding entity for a
  30-second job. The spec allows either.
- **Alternatives:** a seat and a player's clicks (the spec's other option).

### 6. The heavy belt is its own block, drawn a metre wide
- **Chosen:** `heavy_belt_conveyor`: a 1,400 mm belt at 4 m/s, 3,000 t/h, laid as the 600 mm belt is; a run is one kind of belt only,
  driven by `heavy_conveyor_drive`s (355 kW, the medium-voltage tier). It is drawn the full block wide, as the 1.4 m vent duct is drawn
  a metre round. A run holds 208 kg a metre (its full rate at its speed), at least 30 s of the line.
- **Why:** P7-9; the real 1.4 m does not fit a block, and a two-block belt would be a part per metre.
- **Alternatives:** a belt two blocks wide (a big part a metre; hard to lay).

### 7. How many drives a heavy run needs, and its grade
- **Chosen:** 75 kW for each 30 m plus the lift, m g h over 0.9, at the run's full 3,000 t/h, in 355 kW drives rounded up (the spec's
  stockpile conveyor: 150 m and 30 m up is 650 kW, two drives). Short of that it reads NO DRIVE with the kW it needs. A run that changes
  height more than once in any three metres reads TOO STEEP and does not move: the spec's 18 degrees, 1 in 3.
- **Why:** the spec's rule; sizing at the full rate is how a real belt's drive is chosen.
- **Alternatives:** size by the rate it carries now (drives would come and go with the load).

### 8. Which tier each motor is on
- **Chosen:** the gyratory (450 kW) and the heavy drive (355 kW) at the medium-voltage tier; the primary apron feeder (150 kW), the
  reclaim feeders (55 kW) and the rock breaker (75 kW) at the industrial tier. Voltages come from Wildcatter's tiers, never a number.
  They start as Wildcatter's motors do (six times the current on a direct start).
- **Why:** real plants put motors over about 250 kW on medium voltage and the rest on low voltage.
- **Alternatives:** all at the industrial tier (a 450 kW motor at 480 V draws about 680 A).

### 9. The reclaim tunnel is not a part
- **Chosen:** there is no `reclaim_tunnel` part. The tunnel is built of any solid blocks (concrete, stone): the pile rests on anything
  solid, and an opening in its roof over a feeder's north (feed) end is the one place the pile comes down. The feeder looks up through
  the opening, up to 4 m, for the stockpile block over it.
- **Why:** a big part fills every cell of its box, so a 5 x 5 tunnel part would have no room inside for the feeders and the belt that
  must be in it. A player's concrete tunnel is what a real one is.
- **Alternatives:** a tunnel part with a hollow inside (Wildcatter's big parts are solid boxes); a roof-only part.

### 10. The stockpile block
- **Chosen:** `stockpile_rock`: a muck block that holds crushed rock, three quarters of a bank block (2,025 kg), with muck's block
  entity. A shovel digs it; broken by hand it drops crushed rock items, a quarter at a time by chance, as muck drops its rock. Its
  texture is drawn by script, `stockpile_rock_placeholder` (muck's art, lighter and greyer).
- **Why:** P7-7; the same swell as a blast's muck, so the pile's tonnes come out of its blocks.
- **Alternatives:** muck itself (it would not be crushed, and would not roll, #11).

### 11. The pile drains as cones: how much is live (needs you)
- **Chosen:** a stockpile block on the pile's top rolls by the settle rule when it stands too steep, and when a cell of the pile is
  emptied the tops of the eight columns round it look again. So when a feeder draws the block over its opening the column falls into the
  gap and the slopes roll in at 37 degrees, as the spec says. **Measured** on a 2,000-block pile, about 10 m high: one feeder under its
  middle draws **26 %**; three feeders 6 m apart in a line draw **41 %**. The spec predicted 25-35 % with three.
- **Why:** the one-feeder figure is the real one (conical piles are 20-30 % live with one feeder) and is what a 37-degree draw-down cone
  in a 37-degree pile gives on paper (25 %). Three feeders 6 m apart under a pile only 13 m in radius cover much of it; under the spec's
  30 m pile (40 m radius) the same three would draw a smaller share.
- **Question:** is 41 % with three close feeders acceptable? Real plants space feeders to get 30-35 %; a player who spaces them wider
  gets more, which is right.
- **Alternatives:** muck rolling too (would change existing worlds' muck heaps); a live share written as a number (P7-7 rejected it).
- **Tim (2026-11-10): "tune three close feeders down to the spec's 25-35 %." Built:** the draw-down has its own row of the settle
  rule (`Settle.DRAW_DOWN_RISE`), used only by a stockpile block on the pile's top rolling as a feeder draws under it: its slopes stand
  at 1 in 1 over a run (45 degrees, not the pile's 3 in 4) and a step of 2 may stand beside the drawn column, as real draw-down cones
  stand steeper than the angle of repose. The pile is built, and a block falling into the drawn column rests, by the rule as before;
  muck is unchanged. **Measured** on the same 2,000-block pile: three feeders 6 m apart draw **33 %** (32.6-34.5 % over eight seeded
  runs of an offline model of the scene, which gave the old 26 and 41 % too); one feeder **20 %** (19.8-20.8 %), the low end of real
  piles' 20-30 % with one feeder. No row of the rule kept one feeder at 26 % with three under 35 %: every draw-down slope tried left
  three feeders at about 1.55 times one (the cones' overlap is geometry), so 26 % for one meant 40 % for three. The steeper slope
  was chosen over a 63-degree lip with a 37-degree run (one 22 %, three 34-35 %, too near the spec's edge). `p7_stockpile_live` now
  asks 25-35 %, `p7_stockpile_one` 17-30 %; `PitLineTest` checks the rule.

### 12. The stockpile head
- **Chosen:** a heavy belt delivers over its top; it drops the load through its chute, under the middle of its south end, as stockpile
  blocks by the settle rule: a whole block of one rock when it has 2,025 kg of it, else, with two blocks' worth of mixed rock, a mixed
  block of quarters. It reads how far the pile is under its chute and the tonnes it has stacked; FULL when the pile reaches its chute.
- **Why:** blocks of one rock keep the pile's contents exact; mixed blocks only when a feed of many rocks would otherwise wait.
- **Alternatives:** always mixed blocks (the pile would read as a blend).

### 13. The head counts tonnes stacked, not tonnes in the pile
- **Chosen:** the head counts what it has stacked; each reclaim feeder counts what it has drawn. Neither counts the pile itself.
- **Why:** a 30 m pile is 50,000 blocks; counting it would be a scan of a 40 m radius. The spec allowed "counted as it is built and drawn".
- **Alternatives:** a periodic scan, or a pile registry the head and feeders share.

### 14. The belt weigher and the feeders' share (needs you)
- **Chosen:** the weigher sits beside or under a heavy belt. Its rate is what the reclaim feeders put on its run each second (not a
  scale at its own place on the belt), its total pulses each 100 t. Its setpoint, 1,000 t/h by default, steps 50, 250, 500, 750, 1,000,
  1,250, 1,500 by a sneaking click, or comes from the PLC (SETPOINT, 4-20 mA over 0-2,000 t/h) while in AUTO. The feeders drawing for it
  share it evenly, each up to its own 500 t/h: two at 1,000 t/h run at 500 each; one runs at 500. A feeder on a run with no weigher runs
  at its full 500 t/h. While the mill its run feeds reads OVERLOAD (P7b) the setpoint is held 10 % back.
- **Why:** real SAG feed control is a belt scale's PID trimming the feeders' speeds; an even split is what that settles to, and counting
  at the feeders gives the same tonnes without a transit delay.
- **Question:** the 50 t/h step is for P7a feeding the small plant before P7b (P7-10); keep it?
- **Alternatives:** a PID on the scale's own reading (slower to settle, the same result); feeders each set by hand.

### 15. What the gyratory makes
- **Chosen:** the crushed class (P80 about 150 mm), as the jaw's, kg for kg of each rock; at about 2,400 t/h while fed and its motor runs,
  and only as fast as the surge pocket has room.
- **Why:** the spec; the game's size classes have no coarser class for a primary's product.
- **Alternatives:** a coarser "primary" class (every later part would have to learn it).

### 16. Where the feeders put their rock
- **Chosen:** the primary apron feeder and the reclaim feeders put their rock onto a belt beyond their south face or under their south
  end, as the head of a real apron feeder discharges.
- **Why:** both are real ways a feeder's head chute meets a belt.
- **Alternatives:** the south face only.

### 17. The PLC points
- **Chosen:** every motor has the plant's points (RUN, RUNNING, STALLED, AUTO, AMPS, FILL). The gyratory's FILL is its pocket's level,
  and it adds BRIDGED and POCKET FULL (another 363 t would not fit). A reclaim feeder adds SPEED (4-20 mA over 500 t/h). The weigher,
  which has no motor, has RATE, SETPOINT and TOTAL only. The surge pocket's LEVEL is its readout, not a point.
- **Why:** the spec's lists, on the points the plant already has.
- **Alternatives:** a separate LEVEL point on the surge pocket (it has no motor box to carry the signal).

### 18. Tests run faster
- **Chosen:** the draw-down scenes run the reclaim feeders and the heavy belt 20 times as fast (a test-only setting, as the dry mill's
  and DMS's scenes do); the 600 mm belt has no such setting, so no existing scene changes.
- **Why:** at the real rate the 2,000-block draw-down takes 40 minutes of play.
- **Alternatives:** smaller piles (the live share is a property of the pile's shape).

### 19. Recipes and art
- **Chosen:** placeholder recipes, as the rest of the plant's (iron blocks round a cone crusher for the gyratory, gray concrete round a
  hopper for the pocket, and so on). The parts are models in Wildcatter's materials (`tools/models/pitline.py`); no Ludo art was spent
  on P7a; the stockpile block's texture is a script-drawn placeholder (#10), listed under Known gaps.
- **Why:** the powered parts' recipes are placeholders plant-wide (ROADMAP, Known gaps).
- **Alternatives:** Ludo art for the stockpile block's texture (about 1 credit).

## P7b: the SAG mill line

### 20. Twin-pinion drives: two motors, two boxes
- **Chosen:** the SAG (2 x 4.1 MW) and the large ball mill (2 x 5.25 MW) each have two motors at the medium-voltage tier, their terminal
  boxes on the west face at the main cell and 4 m south of it. Each pinion gives half the mill's power: on one motor the mill turns at
  half its rate. Each reads its own volts, amps and watts in the readout; AMPS on the PLC is the first pinion's.
- **Why:** P7-5 and the spec: dual-pinion drives are real to about 9-12 MW; two boxes is what a real twin-pinion mill has.
- **Alternatives:** one 8.2 MW motor box (one cable carrying about 1,400 A at 4.16 kV); motors at the generator tier (P7-5's alternative).

### 21. Starting a big mill (needs you)
- **Chosen:** no rule of its own. The mills' motors start as Wildcatter's motors do: direct on line they draw six times their full-load
  current, about 4,300 A for each 4.1 MW motor at 4.16 kV, which trips a breaker set for the running current; through a soft starter
  they draw less, and on a VFD they never draw locked-rotor current. There is no NOT STARTED interlock and no `p7_sag_start` scene: it
  needs a supply of over 8 MW, which no scene builds yet.
- **Why:** the physics is already Wildcatter's; a second rule on top would disagree with it. Real big mills start on a liquid resistance
  starter or a VFD for exactly this reason.
- **Question:** should a mill refuse a direct start outright (NOT STARTED), as the spec proposed, or is the breaker tripping enough?
- **Alternatives:** an interlock that reads whether a starter or drive feeds the motor (Wildcatter's motor knows only "drive-fed").

### 22. The SAG's charge, OVERLOAD and the weigher
- **Chosen:** the SAG's hold is its rock charge. Its normal charge is about 83 t (16 % of its 326 m³ inside, at 28 % filling with 12 %
  balls, at 1.6 t a m³ of broken rock); it grinds its full 1,000 t/h at that charge and less below it, in proportion. Over the spec's
  35 % filling (about 120 t) it reads OVERLOAD and keeps grinding; at 40 % (about 160 t) its feed belt stops. While it reads OVERLOAD the
  belt weigher on its feed belt holds its setpoint 10 % back, and gives it back once the mill has cleared.
- **Why:** real SAG control holds the mill's weight on its bearings by trimming the feed; a rate that follows the charge makes the
  charge settle where feed and grind meet, as a real mill's does.
- **Alternatives:** a fixed rate with OVERLOAD a stall (a mill does not stop when overfed); a fallback that steps down 10 % each 10 s
  (harsher; the same end).

### 23. Pebbles and the pebble crusher
- **Chosen:** 20 % of the crushed rock the SAG grinds leaves as pebbles (still the crushed class) out of its east face onto a belt; the
  pebble crusher makes them gravel; the SAG takes gravel and grinds all of it to undersize. With the pebble crusher's motor stopped the
  pebbles go round it uncrushed (BYPASSED). The SAG grinds 15 % slower unless its pebble belt delivers into a pebble crusher that is
  running. Getting the crushed pebbles back onto the SAG's feed belt is the player's belt work.
- **Why:** the spec's figures; a bypass chute round the pebble crusher is real.
- **Alternatives:** pebbles as a size class of their own (every part would have to know it).

### 24. The pump takes the ball mill's discharge on its sides as well
- **Chosen:** the cyclone feed pump takes the ball mill's discharge on pipe at its north face or either side.
- **Why:** the spec puts the SAG against the pump's north face (the undersize by touching) and the pipe on the same face; with a part as
  wide as the SAG there, the pipe needs another face.
- **Alternatives:** the SAG touching the pump's side instead (the spec's drawing has the pump at the SAG's discharge end).

### 25. The water
- **Chosen:** the SAG takes water to grind at 75 % solids; the pump box takes the rest to make a cyclone feed of about 54 % solids, so the
  underflow leaves at the ball mill's 70 % and the overflow at 35 % with no water of their own. All of the line's 516 L/s of fresh water
  leaves in the overflow; the ball mill takes none. NO WATER on the SAG and the pump.
- **Why:** it closes the mass balance the spec gives (75 %, 70 %, 35 %, 250 % circulating load); real cyclone feeds are 50-60 % solids.
- **Alternatives:** water at the cyclones, as the small plant's cluster takes it.

### 26. The cyclone cluster has no motor
- **Chosen:** the twelve 660 mm cyclones are driven by the pump's 600 kW; the cluster has no motor box. It is rated 20 % over the design
  3,500 t/h of feed (ten running, two spare) and its PLC point is PRESSURE (4-20 mA over 0-200 kPa), 90 kPa at the design feed and going
  with the feed squared. The pump is rated the same.
- **Why:** a cyclone has no moving parts; its feed pressure is what a real plant reads.
- **Alternatives:** a gauge on the pump instead.

### 27. The large-bore pipe (P7-6, needs you)
- **Chosen:** the line runs on Wildcatter's steel pipe as it is. Wildcatter 0.6.0's pipe holds nothing and sets no flow limit, so one
  pipe carries the cyclone feed's 1 m³/s; the parts' 500 mm nozzles are drawn in their models. A large-bore slurry line with its own
  rating stays proposed for Wildcatter (P7-6).
- **Why:** nothing in Wildcatter stops it, and a new Wildcatter pipe is outside this branch.
- **Question:** build Wildcatter's large-bore slurry line (and a rating for the small pipe), or leave the pipe unrated?
- **Alternatives:** several small pipes in parallel (tiling; and still unrated).

### 28. Liberation and the assay
- **Chosen:** as the small plant: the slurry carries its assay from the SAG on, and the flotation stage treats fines as 90 % liberated.
- **Why:** the spec says the same 90 %.
- **Alternatives:** none.

### 29. Where the overflow goes before P7c
- **Chosen:** the cluster's fines slurry goes out on pipe to whatever takes fines slurry: today the 50 t/h plant's conditioning tank and
  bank. One bank takes a twentieth of the line; when nothing draws the overflow the cluster's hold fills and the whole line backs up and
  slows, as a real one would.
- **Why:** P7c is the line's own flotation; until it is built the line runs only as fast as what it feeds.
- **Alternatives:** a tails pond the line can dump to (it would throw away the ore).

### 30. The PLC points
- **Chosen:** each mill has the plant's motor points and WEIGHT (4-20 mA): the SAG's charge over its 40 % filling, the ball mill's
  underflow waiting over 10 s of its feed. The pump has the plant's motor points; the cluster PRESSURE only (#26).
- **Why:** the spec's lists.
- **Alternatives:** none.

### 31. Art, sound and recipes
- **Chosen:** five models in Wildcatter's materials (`tools/models/sagline.py`); the mills sound the ball mill (louder), the pebble crusher
  the cone crusher, the pump Wildcatter's motor hum. Placeholder recipes (iron blocks round a ball mill for each mill; the small cyclone
  clusters round an iron block for the large one). No Ludo credits spent on P7.
- **Why:** as every plant wave; the parts have no icons of their own to make (their items are their models).
- **Alternatives:** Ludo icons for the stockpile block (#10).

## P7c: pit flotation and the tails

### 32. P7c is not built: a written plan instead
- **Chosen:** P7a and P7b are built, tested, documented and filmed; P7c (the tank-cell row, the column cleaners, the large blower and the
  45 m tails thickener) is left with a plan in ROADMAP ("P7c, the plan").
- **Why:** Tim's instruction for a session that cannot do all of P7. Two things in P7c want their own care: the 45 m thickener is 12,150
  cells, about 50 MB of piece models at one per cell, which wants a change to the model tool first; and the row's recovery split must pay
  through the existing filter without changing what the small bank pays.
- **Alternatives:** a half-built P7c (ruled out by Tim).
- **Since:** P7c was built on branch `p7c-pit-flotation` (cut from `p5-big-machines`); #33-#44 below.

## P7c, as built (2026-11-10, branch `p7c-pit-flotation`)

### 33. The model tool: empty cells share one model (needs you)
- **Chosen:** Tailings' driver (`tools/models/build.py`, not Wildcatter's `mlib`) exports a model that sets `share_empty = True` with
  Wildcatter's writer wrapped: a piece with nothing drawn in its cell gets no `p<k>.json`; one `<part>/empty.json` stands for all of
  them, its blockstate names it once per piece (`piece=k`, every facing, no turn) and the blockstate is written without indentation.
  Only the `tailings_thickener_large` sets it: 10,005 of its 12,150 pieces are empty, so it is 2,146 piece models (about 3.8 MB) and a
  1.4 MB blockstate, not about 50 MB. Every other model is written exactly as before: all 20 existing modules were regenerated before and
  after the change and the output compared, byte for byte identical.
- **Why:** the plan's step 1; doing it in Tailings' driver leaves Wildcatter's tool and its parts alone, and opt-in keeps every
  existing asset unchanged.
- **Question:** turn it on for the existing big parts too? The dragline house has 3,551 empty pieces, the headframe 1,715, the load-out
  frame 1,024, the shovel house 943, the SAG 866: it would delete about 10,000 small files and change no look or behaviour.
- **Found on the way:** `vent.py` adds its duct texture to the shared texture table when every module is built in one run, so a full
  run writes a `duct` entry into later modules' models that the committed files (each built alone) lack. Harmless (an unused texture);
  the new module keeps its own table. Fixed on main (6678e33): `vent.py` now copies the table, and a full regeneration matches git.
- **Alternatives:** change `mlib` in Wildcatter (the brief said not to); a smaller thickener (the spec's 45 m is the real size).
- **Tim (2026-11-10): "turn the shared empty model on for every big part." Built:** `build.py` exports every big part (more than one
  cell, its blockstate written by `mlib`) this way; a model may still set `share_empty = False`. The export now clears a part's old
  piece models first, so a part with fewer pieces leaves none behind. Regenerated: 13,604 piece models deleted, 67 `empty.json` added,
  67 blockstates (written compact) and the dragline house's shape file rewritten: 31,644 model and blockstate files to 18,107, 96.2 to
  63.7 MB. Every one of the 67 parts, rebuilt from its blockstate and piece models in all four facings, draws the same boxes before and
  after, and all 103 previews in `build/model-previews` are byte for byte the same. With the hollow dragline house (p6-dragline.md #3).

### 34. How the 45 m thickener is drawn
- **Chosen:** the concrete wall (96 plates), the overflow launder inside its rim, the slurry's surface drawn a row at a time (one box a
  cell, so no cell holds overlapping boxes), the feedwell, the centre drive, the half bridge from the east wall, the feed pipe on its
  rack from the north face to the feedwell, the underflow nozzle south and the overflow water east. The bed, the rakes and the floor
  are under the surface and not drawn. You can walk on the slurry (its collision is the surface), as on the P4 thickener.
- **Why:** what shows from outside, in as few pieces as draw it.
- **Alternatives:** rake arms turning (a rotor of 22 m; Wildcatter's rotors are a block across).

### 35. The row's split, cell by cell
- **Chosen:** each tank cell floats its share of the block's floating mineral, 35, 15, 8, 5, 4, 3, 3, 2 %, as the fraction of what
  reaches it (35 % of 100, then 15 of 65, and so on). What a cell passes on travels in the tails under a marked name, `tailings:rowN/<id>`
  after N cells, so the next cell knows its share from the slurry itself however the pipes run: a row of four pays 63 %, a ninth cell
  floats nothing ("nothing left in them that a cell floats"), two rows side by side each pay 75 %, and the bank or scavengers of the
  small plant do not float the marked names again. Gangue and the selective names pass untouched. The large thickener takes the marks
  off, so the lined cell holds plain names. As at the bank, sphalerite floated from feed that also holds galena is lost (bulk) and the
  zinc concentrate keeps its own name. The concentrate carries plain names, so the existing filter pays it by its unchanged rule.
- **Why:** the plan's split, the rung kept (the shares add to `RecoveryRoute.FLOTATION`'s 75 %, a unit test), and no cell needs to
  know its place in a row of pipes.
- **Alternatives:** each cell finding its place by following the pipe north (breaks with a split stream or a header); a fixed 75 % at
  the last cell (a short row would pay nothing).

### 36. The air: Wildcatter's air tubing as the air line (needs you)
- **Chosen:** a tank cell or column cell has air when a running `flotation_blower_large` touches it or is joined to it by Wildcatter's
  air tubing. One blower serves ten cells (the row's eight and two columns, proposed); the eleventh and on read NO AIR until another
  blower is on the line. No blower reads NOT BUILT; a stopped one NO AIR. The line is looked at once a second.
- **Why:** a row of 7 m cells with pipe between cannot all touch one blower; real rows have an air header along them. The air tubing is
  Wildcatter's only air line.
- **Question:** the tubing is drawn as instrument tubing, not a 600 mm air header. Keep it, or add an air header to Wildcatter (or a
  Tailings duct, as the vent duct)?
- **Alternatives:** the blower touching each cell (impossible in a row); cells passing air to the cells they touch (they do not touch).

### 37. The column cleaners (needs you)
- **Chosen:** the cells' launders give a rougher concentrate at 20 % solids (proposed); a column cell takes it (north face, up to 30 t/h
  of solids, proposed) and gives it at the bank's 30 % out of its south face, every kg of mineral; its tails out of its east face are
  the water it gave up, with no mineral, to be piped back to cell 1 as the plan says. Concentrate already at 30 % passes through. The
  columns need air.
- **Why:** the game pays at the filter (#4 of the plan): a cleaner that sent mineral back to cell 1 would pay over the rung (the loop
  floats it again) or under it. Water only keeps the 75 % exact.
- **Question:** the columns are not required: the concentrate thickener takes rougher concentrate too and pays the same, only with
  half as much slurry again to thicken. Should the thickener refuse concentrate that has not been cleaned?
- **Alternatives:** cleaners that reject a share of mineral with a recirculating load (pays about 73 % at 90 % column recovery).

### 38. The row's rates
- **Chosen:** the large tank, each cell and the large thickener are rated 20 % over the line's 1,000 t/h of fines slurry at 35 %
  (47.6 kg a tick), as the large cyclones are; each holds 10 s at its inlet and outlet. Every cell takes the whole row's flow, as a real
  cell in series does. Eight 160 m³ cells hold about 29 minutes of the line.
- **Why:** the spec's sizes; 20 % over keeps a row from becoming the line's choke with the columns' water in it.
- **Alternatives:** exactly the line's rate (the columns' water returning would throttle it).

### 39. The large conditioning tank
- **Chosen:** as the small tank: reagents by hopper (lime, xanthate, MIBC; copper sulfate with sphalerite), dosed per tonne at the same
  g/t, so it takes twenty times as many reagent items an hour. It has no selective mode: the pit line pays the bulk 3x rung only.
- **Why:** the spec (selective flotation is the small plant's 4b-4e, left out of the pit line).
- **Alternatives:** a selective switch (a later wave with a pit-scale activation tank).

### 40. The large thickener, its tails and Wildcatter's lined cell (needs you)
- **Chosen:** the P4 tailings thickener's stage, made a function of its rate and holds (the P4 thickener calls it with its old numbers
  and works as before; `pit_thickener` passes), called at the line's rate: tails in at its north face, 60 % underflow at its south
  face, the overflow water back on a water pipe at its east face. TORQUE (4-20 mA) is its rake's: a fifth of its rating turning in
  its bed, rising to the full rating as the underflow backs up, 4 mA stopped.
- **Question:** Wildcatter's decant tower takes 20 kg of slurry a tick (about 72 t/h); the line's underflow is about 1,670 t/h, so a
  pit line needs over 20 lined cells to keep its thickener from backing up. Raise the tower's rate in Wildcatter (or let a cell have
  several spigots)? The tower also counts slurry as 40 % solids where the underflow is 60 % (as with P4 today).
- **Why:** the plan said to reuse P4's stage; the tower's rate is Wildcatter's.
- **Alternatives:** a tailings pipeline part with its own sink (the spec's Left out: pumping stations).

### 41. The PLC points
- **Chosen:** every new part has the plant's motor points (RUN, RUNNING, STALLED, AUTO, AMPS, FILL). A tank cell and a column add AIR
  (20 mA with air, 4 mA without); the large thickener adds TORQUE. The blower has the motor points only (its RUN, RUNNING, AMPS). The
  new parts have no LEVEL, FLOW and DENSITY gauges.
- **Why:** the spec's list.
- **Alternatives:** AIR as a flow in m³/min (the game has no air flow to read).

### 42. Motors and tiers
- **Chosen:** the blower's 400 kW at the medium-voltage tier; the tank (90 kW), each cell (160 kW), each column (15 kW) and the
  thickener (55 kW) at the industrial tier. Tiers from Wildcatter, never a number. A row of eight is 1.28 MW.
- **Why:** #8's rule: over about 250 kW on medium voltage.
- **Alternatives:** none.

### 43. Art, sound and recipes
- **Chosen:** five models in Wildcatter's materials (`tools/models/pitflotation.py`); no texture drawn, no Ludo credits. The tank and
  cells sound the agitator, the column, blower and thickener the motor hum. Placeholder recipes: conditioning tanks round an iron block;
  a flotation bank in iron; a cleaner bank between iron blocks; air blowers round an iron block; tailings thickeners round an iron block.
- **Why:** as every plant wave.
- **Alternatives:** none.

### 44. Tests and the film
- **Chosen:** NeoForge scenes `p7_tank_row` (eight cells: each cell's share to 1 g, three nuggets of a little over four at the filter,
  a quarter in the tails), `p7_row_short` (four cells, 63 %, two nuggets), `p7_no_air`, `p7_tails` (underflow into a lined cell with
  plain names, water back to a mill, TORQUE) and `p7c_parts`; unit tests `PitFlotationTest`. The Fabric client film `p7_flotation`
  shows the tank, the row floating, the two columns, the concentrate thickener and filter, the 45 m thickener and its lined cell. The
  full film from truck to concentrate (the plan's last line) is `p7_line` followed by `p7_flotation`: one scene of both would hold over
  200 m of plant in the client at once.
- **Alternatives:** one long film (too slow to load in the test client).
