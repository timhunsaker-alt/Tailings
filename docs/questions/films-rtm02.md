# Films, Rock to Metal episode 2 (2026-11-21): the SAG mill line on real power

Decided while Tim was away, while filming episode 2 (branch `films-rtm02`, TAILINGS_ONLY=rtm02).

### 1. The big mills' pinions move to the generator tier (accepted by Tim 2026-11-21)
- **Found:** the SAG mill and the large ball mill could not run on real power at all. At the medium-voltage tier one 4.1 MW SAG pinion
  draws about 710 A running and 4,300 A starting, one 5.25 MW ball-mill pinion about 910 A running. The largest MV cable is the
  500 kcmil, rated 450 A, and Wildcatter's wire heat burns a conductor at 1.34 times its rating held: a direct start burns it in about
  two seconds, a running SAG pinion in about a minute and a half. No soft starter or drive goes past 125 A at 4.16 kV, and there is no
  MV busway. (Decision #20 counted the 1,400 A of one cable for both pinions; one pinion is already too much.)
- **Chosen:** `Machine.Supply.GENERATOR` for `sag_mill` and `ball_mill_large`: their two motor boxes take the generator tier (13.8 kV on
  North American worlds, 11 kV IEC, read from the tier), fed on generator bus duct (1,200 A). A SAG pinion then draws about 215 A, a
  ball-mill pinion about 275 A. Every other part keeps its tier.
- **Why:** it is the smallest change that makes the line work, and it is what real mills of this size do: motors over a few megawatts
  are wound for 6.6-13.8 kV. A SAG line wired at 4.16 kV burned its cable on its first start, so no working build breaks.
- **What changes for a world:** a big mill with MV cable on its boxes now reads NO POWER until the cable is swapped for bus duct from a
  generator bus (a gas turbine's breaker, as the episode films it).
- **Alternatives:** a larger MV conductor in Wildcatter (parallel 500 kcmil runs, or an MV busway to 2,000 A), which keeps the mills at
  medium voltage but needs a Wildcatter release; or a third and fourth motor box on each mill.
- **Tests:** `MachineTest` reads the new tier; `p7b_parts` checks the boxes take the generator tier and not the medium one.

### 2. How the episode starts the mills
- **Chosen:** direct on line from a 16.5 MW gas turbine set per mill (the SAG on one, the ball mill on the other), one pinion and
  then the other: the second pinion's last length of bus duct is laid once the first is running, standing in for its breaker.
- **Why:** sag-mill-line.md asks for a soft starter or drive; none in Wildcatter is near the size (125 A at 4.16 kV). Real mills
  this size start on a liquid resistance starter or a drive (decision #21). The narration says the game starts them direct on line.

### 3. The filmed layout differs from the force-run `p7_line` scene
- **Chosen:** the cyclone cluster on the large ball mill's east face (on its west face it covered the mill's second motor box); the
  pebble return belt's drive west of the belt's head (its box had landed on its own belt); the line 4 m lower (the SAG on a 2 m base),
  so the 87 m SAG feed belt needs 326 kW and one 355 kW drive; the pebble crusher in a pit dug into the ground.
- **Why:** the old scene force-ran every motor, so none of this had to work on real power. No rule changed.
- **Each motor draws its rating while it runs** (Wildcatter's motor model), so the pebble belt's drive has a 1 MW gas set of its own: the
  crusher and two drives on one set tripped it on frequency.
