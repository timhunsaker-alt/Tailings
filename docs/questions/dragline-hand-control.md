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
