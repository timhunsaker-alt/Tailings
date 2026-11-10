# P7: the pit-sized plant line (2026-11-10), decided while Tim was away

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back".
Built on branch `p7-pit-line` (cut from `p5-big-machines`) from the draft specs
[primary-gyratory-stockpile.md](../multiblocks/primary-gyratory-stockpile.md) (P7a), [sag-mill-line.md](../multiblocks/sag-mill-line.md)
(P7b) and [pit-flotation.md](../multiblocks/pit-flotation.md) (P7c), whose own choices are in `specs-tailings.md` (P7-1 to P7-10). Format
as in `README.md` here. **#11, #14 and the P7b entries marked "needs you" most need your eye.** Nothing here changes an existing world (#1).

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
