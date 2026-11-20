# Wave B: the dragline by hand, highwall and spoil failures — questions for Tim (2026-11-19)

Spec: [../multiblocks/dragline-hand-control.md](../multiblocks/dragline-hand-control.md) (DRAFT, not approved). Only the real design
choices are here, the recommended option first; everything else in the spec was decided on the best recommendation and says so.
Format as [README.md](README.md).

**Answered by Tim 2026-11-19:** all eight, each with the recommended option (the Chosen line of each entry). Built on branch
`spec-dragline-hand`; decisions taken while building it are under "Building it" at the end.

### 1. The world rule `slope_failures`: off everywhere, or on in new worlds?
- **Chosen:** off in every world, new ones included; turned on by `/tailings rule slope_failures true` or the World Rules screen.
- **Why:** a slide can wreck a 1,750 t machine and bury players; Tim asked for it opt-in. `paste_wall_breach` is the same kind of rule
  and is off everywhere.
- **Alternatives:** on in new worlds and off in existing ones (house rule 9, as `stale_air`): more players meet it, and a new player's
  first dragline could go down with its highwall.
- **Tim (2026-11-19):** off in every world.

### 2. How long a failing wall gives before it slides
- **Chosen:** from FS falling under 1, about one game day at 0.95, shortening to 2 min at 0.7; under 0.7 within 10 s. Cracks and moving
  prisms from FS 1.15 give warning before that.
- **Why:** long enough to see the cracks, read the prisms and walk the dragline off, short enough to happen while a player is there.
  Real highwalls give days to weeks.
- **Alternatives:** several game days (real pace; most slides would happen while no one is near); a slide as soon as FS falls under 1
  (no warning but cracks).
- **Tim (2026-11-19):** about a game day at FS 0.95 down to 2 min at 0.7.

### 3. A dragline caught in a slide
- **Chosen:** with a third or more of its tub's ground gone it is wrecked: it breaks where it lands into all eleven parts' items, its
  cable and reel, and its load, nothing lost. Under a third, it stops: GROUND GONE, until the ground is made up.
- **Why:** as a wrecked haul truck (its item and load, nothing lost); carrying the parts back and building it again is the cost.
- **Alternatives:** some parts lost (the boom sections break): harsher; never wrecked, only stopped (a slide costs only time).
- **Tim (2026-11-19):** wrecked, all its parts given back.

### 4. The job on cracks
- **Chosen:** a strip panel setting "Stop on cracks", on by default: in AUTO the dragline ends its cycle and stands, WARNING: CRACKS,
  when cracks open within its reach.
- **Why:** a job left running should not walk itself off a cliff while the player is away; a player who wants to push on turns it off.
- **Alternatives:** the setting off by default; no setting (the job always digs on, and only the survey station's alarm through the PLC
  stops it).
- **Tim (2026-11-19):** "Stop on cracks" on by default.

### 5. What can fail
- **Chosen:** only the dragline's own highwall and spoil, in saved 10 m sections along each strip it digs.
- **Why:** a known set of faces is cheap to check every 10 s and never surprises a player's builds; it is the spec's Left out line.
- **Alternatives:** every pit face and dump Tailings' machines make (shovel benches, truck dumps): more real, more to check and to
  tune; any face in the world (would turn vanilla cliffs and players' builds into hazards).
- **Tim (2026-11-19):** only the dragline's own highwall and spoil.

### 6. The levers' keys
- **Chosen:** vanilla keys, read on the server as the trucks are: W/S drag in and out, A/D swing, jump hoist up, sprint hoist down,
  sneak out; sprint held 2 s alone hands back to AUTO.
- **Why:** no new key bindings, no client packet, works on every keyboard layout the player already set; touching a key takes over,
  as real swing assist.
- **Alternatives:** new rebindable key bindings laid out as two levers and a swing pedal (closer to a real cab; needs a client key
  packet and a controls page).
- **Tim (2026-11-19):** vanilla keys.

### 7. Walking by hand
- **Chosen:** left out: in HAND it does not walk; handed back, the job walks it as now.
- **Why:** a step is 2 m a minute along the strip; the job already does it right, and there is no free vanilla key for it.
- **Alternatives:** a step key in HAND (a key chord, such as sprint and W with the bucket landed).
- **Tim (2026-11-19):** left out.

### 8. Swing inertia
- **Chosen:** real inertia: the house speeds up and brakes at 1°/s², so from full speed it coasts 40° after the key is let go; the
  other key brakes twice as hard.
- **Why:** braking early for the dump point is the dragline operator's skill, and the job's own 17-20 s swings come from the same rates.
- **Alternatives:** the swing stops when the key is let go (easier; hand casts would be faster than the job's).
- **Tim (2026-11-19):** real inertia.

## Building it (2026-11-19, branch `spec-dragline-hand`)

Calls taken while building Tim's answers, on the best recommendation, for review.

### 9. Handing back from the cab: jump and sprint together, not sprint alone
- **Chosen:** jump and sprint held together for 2 s (and no other key) hands it back to AUTO while seated; getting out still does.
- **Why:** sprint alone lowers the hoist, so holding it 2 s to lower the bucket handed the machine back by mistake. Hoisting up and
  down at once means nothing on the levers, so it is free for the hand-back.
- **Alternatives:** sprint alone with the hoist doing nothing while it is held (lowering would stop after 2 s); getting out only.

### 10. Handed back, the job carries on from the bucket's pose; its cycle is not re-driven through the ropes
- **Chosen:** loaded, the job picks its cast target and swings out from where the bucket hangs; empty, it swings back from there to
  its last dig point and starts its next cycle. Its own 55 s cycle and its draw stay as built in P6 (poses on a timeline).
- **Why:** the same result for the player (no jump of the bucket, nothing dug twice) without rebuilding a tested job; the hand's rope
  model (`pit.DraglineRopes`) and the job's timeline agree on the rates (a 90 degree swing about 19 s by hand, 17-20 s by the job).
- **Alternatives:** the job driving the same rope model (the spec's sentence; a larger change to P6's tested cycle).

### 11. A slide runs at the pour service's 2.4 m/s, not 6 m/s
- **Chosen:** a slide pours through the same `pit.Pouring` as a truck's load, leaving over 2 s and setting over 2 s, at the service's
  run speed. The slide's blocks are not drawn as the wet sheet (no machine owns them): they appear rim first with dust, a rumble and
  the slide's sound.
- **Why:** one pour service, and the slide's mass is kept exactly (the scenes count every block). A faster run would need a second speed
  in `Pour`; the sheet needs an entity to carry it to the client.
- **Alternatives:** a run speed per pour; a small "slide" entity to draw the sheet.

### 12. Left out of the slide for now: a buried bucket, trucks buried by the run-out, the 2 m taper at a section's ends
- **Chosen:** a section slides its own 10 m, line by line; players and mobs in the wedge or the run-out take 8 damage (and vanilla
  suffocation if buried); other vehicles fall by their own driving physics if the ground goes; the dragline's bucket is not buried.
- **Why:** the spec's parts that matter most (the wedge, the cut filling, the dragline wrecked or GROUND GONE, harm) are in; the rest
  each need their own scene. Listed in ROADMAP, Known gaps.
- **Alternatives:** build them now.

### 13. A slide reaches only the half of the tub ahead of the strip's start
- **Chosen:** the strip's frame starts at the dragline's centre where it was lifted, so section 1's slide can take ground only under
  the part of its tub ahead of that line. A 20 m vertical dirt wall at the strip's start takes about a fifth of the tub (GROUND GONE);
  the scene and film use a 30 m wall, whose plane reaches past the tub's centre, to wreck it. Further along the strip (after a walk)
  the tub is wholly beside the section.
- **Why:** the record keeps what the dragline digs; a slide behind the start would be a wall it never dug.
- **Alternatives:** sections that reach 10 m behind the start.

### 14. The survey station reads out in chat; no chart screen
- **Chosen:** clicked, the station lists each prism it sees (in line of sight, within 500 m): how far it has moved, mm/h, STEADY,
  CREEPING or ACCELERATING, and the predicted slide time once one accelerates. Its PLC points are as specified.
- **Why:** everything the chart would show, readable now; a screen with a chart is art and UI work for the next art wave.
- **Alternatives:** a screen with each prism's velocity chart over the last game day (spec).

### 15. The prism and the station are drawn from their Ludo icons, crossed
- **Chosen:** each is a cross model (as a flower) of its 16 px Ludo picture; the tension crack is dirt with a Ludo crack on top.
- **Why:** they read as the objects at once with no hand-built model; a real model is cheap to add later.
- **Alternatives:** box models in Wildcatter's materials.

### 16. "On the levers" asks for one bucket by hand, not ten
- **Chosen:** the quest's task is the milestone `tailings:dragline_hand_cast`, fired for each bucket cast by hand.
- **Why:** Wildcatter's milestone tasks tick once; there is no count on a milestone task.
- **Alternatives:** a count on milestone tasks in Wildcatter.

### 17. Dumped by hand over a truck, the load pours onto the ground under the bucket
- **Chosen:** a hand dump always pours where the bucket hangs, by the settle rule, onto whatever is below.
- **Why:** the spec's truck loading by hand needs the shovels' loading into a body; a dragline is not a loading tool in practice.
- **Alternatives:** load a truck under the bucket as the shovels do.

## Finishing it (2026-11-20, branch `finish-0.2`, 0.2.1)

The Wave B gaps closed for the trailer, on the best recommendation, for review. 11, 12 and 17 are superseded as below; 14 stands.

### 18. A slide runs block by block at 6 m/s, each block drawn on its way (supersedes 11)
- **Chosen:** each block of the wedge lets go (the toe and the middle first, the top and the ends within 1.5 s), speeds up at about
  5 m/s² down the failure plane to 6 m/s, runs down the face and out over the cut to the cell the settle rule gives it, and sets
  there. A small never-saved slide entity carries the blocks' ways to the client, which draws each block in its own model, tumbling;
  the pour service still sets every block, so nothing is lost and the scenes' counts hold.
- **Why:** the wall now visibly flows into the cut over several seconds instead of failing in one frame; one pour service still
  owns the blocks.
- **Alternatives:** the pour's wet sheet (a ground-hugging film; it does not show a face coming down); vanilla falling blocks (they
  place themselves and lose the settle rule and the count).

### 19. A failing bench creeps in three steps of a metre
- **Chosen:** at 35, 60 and 80 % of a failing highwall's clock, the slab of bench between its tension crack and its crest slides a
  metre toward the face over about half a second, with its prisms; the crack gapes a metre wider each time and the crest's block
  ravels down the face, coming to rest 3 m out from the toe (at the toe it would read as a buttress). Cracks open as fast as the clock
  runs out; a cracking or failing section is checked every 10 s divided by the time scale (at least once a second).
- **Why:** real failing walls move metres in their last hours; this makes the warning something you can see and film, at the world's
  time scale.
- **Alternatives:** prism readings only (invisible); a smooth sub-block shift (the world has no sub-block ground).

### 20. A prism is not ground
- **Chosen:** a slope's profile reads through survey prisms, so a prism standing on the bench never moves its crest.
- **Why:** a prism carried onto the bench's measuring column made the wall read as a different shape.

### 21. The 2 m taper (closes part of 12)
- **Chosen:** a section's slide reaches 2 m into each neighbouring section, the wedge reaching 2/3 then 1/3 as far behind the face.

### 22. A wrecked dragline tips into the cut before it breaks
- **Chosen:** with a third of its tub's ground gone it tips over the tub's edge toward the missing ground, to 70° over 3 s, sliding out
  and down into the cut, drawn as its model turning; its operator is thrown clear at once; then it breaks where it landed into all
  its parts, as before (Q3).
- **Alternatives:** falling blocks of its parts (it is an entity drawn by its model, not blocks).

### 23. A buried bucket (closes part of 12)
- **Chosen:** a slide's blocks coming to rest over the bucket lying in the cut bury it: BUCKET BURIED. In HAND it neither hoists nor
  swings until dragged out with W (full or not, it comes out); in AUTO the job stops its cycle and its next bucket is the slid ground
  over it, top down. Saved with the machine.

### 24. Machines caught in the run-out (closes part of 12)
- **Chosen:** as the run-out reaches a truck, dozer, loader or shovel: buried over half its height by the cells planned over it, it is
  wrecked (its item and load, nothing lost); otherwise it is pushed 2 m the way the slide runs, up onto the run-out, and stands BLOCKED
  for 30 s or until its driver drives. Not saved (a slide reloaded mid-run catches nothing more).
- **Alternatives:** BLOCKED until a player clears it (a self-driving fleet would stand forever).

### 25. Dumped by hand over a truck, the load goes into the truck (supersedes 17)
- **Chosen:** with the bucket over a haul truck's box, S loads the truck to its rated mass as a shovel does (the stream drawn into the
  body); what it cannot take spills beside it by the settle rule. A cycle and a hand cast either way.

### 26. The cab's readout is shorter
- **Chosen:** HAND | swing | m out and up | t and % | hoist and drag m/s | kW (and CRACKS, BURIED), so it reads whole on a phone.

### 27. No chart screen yet (14 stands)
- **Chosen:** the station still reads out in chat; the chart screen was optional and is left for the next art wave.
