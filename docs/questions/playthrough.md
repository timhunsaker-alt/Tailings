# Playthrough fix — decided while getting the filmed run to pay again (2026-11-09)

Tim (2026-11-09): "make decisions best recommendation". The playthrough on main (145f0d4) stalled: from its first ore the cone read
NO POWER and the ball mill's generator tripped, the plant backed up and the filter paid nothing; at 23.5 minutes the TBM read
"machine not found" and the train ran empty for the rest of the hour. Fixed on branch `worktree-agent-ae0e50cef61c0be63`.

What was wrong, found with a diagnostic run that stages ore in the ore bin (`TAILINGS_FEED=1`, below) and a check that every power
cable laid is still there:

- **The cone's 4/0 cable burnt out at its motor box, and the mill's 1 MW set tripped on stator overcurrent** (phase C at 2.7 x, I²t
  103 %), both within 30 s of the first ore. Not a wiring mistake and not Wildcatter's 4,000-node cap (the largest circuit in the
  playthrough is about 1,750 nodes). Since plant-gaps the belts carry rock at 2 m/s and drop it in small pieces, and a part's motor
  stopped the moment its hold emptied. The cone and the mill started direct on line on every few kilograms, six times their current
  each time, and the heat added up: the cable burnt, the generator tripped. The screen was caught the same way ("starting, inrush"
  at every reading).
- **The TBM was not lost.** At the end of its course (25 blocks down, 20 on the flat) it sets itself down as blocks
  (`TbmEntity.setDown`, as designed). The playthrough only looked for its cab where it started, so it read "machine not found". The
  train ran empty because the bore was finished: the plant, stalled, had not paid by then.
- The server tests' "4102 nodes" warning comes from a server scene, not the playthrough.

### 1. Motors run on through gaps in their feed
- **Chosen:** a mod fix in `PlantBlockEntity`: a started motor keeps running, empty, for `PlantRates.MOTOR_RUN_ON_SECONDS` = 60 s
  after its part's work runs out, then stops. It never starts without work, and the PLC's run (or the hand switch) still stops it at
  once. The number is marked proposed.
- **Why:** real plants run their crushers and mills through the gaps in the feed; NEMA MG 1 allows a large motor about two starts cold
  and one hot. Stopping on every empty hold short-cycled the motors, which is what burnt the cable and tripped the set. With the
  run-on, the same layout and the same generators run: cone 194 A, mill 98 A at 3.9 kV, mill set at 54 %.
- **Alternatives:** a 4.16 kV soft starter or drive for the mill and a bigger cable for the cone (works around the symptom in the
  playthrough only; any player's plant with belts would still short-cycle); run every motor whenever the PLC says run, work or not
  (more real for a PLC plant, but a part's motor would then never stop by itself off a PLC); a longer run-on (5 min) costs more fuel
  for an idle plant.

### 2. The playthrough fails fast
- **Chosen:** every 30 game seconds the playthrough checks that each power cable and busway cell it laid is still there, that no
  generator has tripped, and that no machine has read NO POWER or STALLED for two checks running (a minute). Any of these stops the
  run with the reason, the full status dump and a screenshot (`tailings-play-failed`), and the film is stopped and kept.
- **Why:** the stalled run used its full hour to show what was clear at minute 8. A burnt cable is removed from the world, so its
  absence is the plainest sign.
- **Alternatives:** fail on the first NO POWER reading (a generator's start or a breaker's re-close can read dark for a moment);
  check only the machines (would not name the burnt cell).

### 3. The TBM at the end of its course
- **Chosen:** the playthrough follows the TBM: the moving machine, or, when it stands as blocks, its cab within 16 blocks of where it
  was last seen. It keeps topping up the cab's slots there. Only if neither is found does the run fail ("the TBM is gone").
- **Why:** setting down on the flat is the machine's designed end of a course; it is not a fault.
- **Alternatives:** a longer flat so the bore runs the whole hour (the run pays well before the course ends once the plant runs).

### 4. A diagnostic feed
- **Chosen:** `TAILINGS_FEED=1` puts 256 of the bore's ore straight into the ore bin when the TBM gets its GO, and logs every status
  each 30 s. With `TAILINGS_GAME_MINUTES=6` it shows within five minutes whether the plant's power holds under load, instead of waiting
  eight minutes for the first train. It is off in the filmed run and logged as a diagnostic when on.
- **Why:** a full run takes about 15-25 minutes of real time and only one client may run on the machine at a time.
- **Alternatives:** a server scene with the playthrough's whole power layout (generators, substations, busway), the better long-term
  proof, but a large scene to keep in step with the playthrough.
