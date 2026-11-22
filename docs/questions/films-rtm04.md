# Films for Rock to Metal, episode 4 (branch `films-rtm04`)

The `rtm04_*` client scenes (`Rtm04Scenes`, `TAILINGS_ONLY=rtm04`): the spiral bank, the scavenger spirals, the thickener and filter, the
gas-fired dryer, the three dry-mill separators and Wildcatter's chlorinator, every motor on one Wildcatter diesel set at the industrial
voltage. Decided while Tim was away (2026-11-22), each on the best recommendation. No game rule or main code changed.

## 1. One line, filmed new, instead of the old force-run `spirals` and `dry_mill` films
- **Found:** the plan's two [exists] shots are force-run and sped up (`fastCycle(20)` on the dryer and separators).
- **Done:** one line laid as dry-mill.md's scene lays it, run in real time on real power, and filmed in nine shots. The dryer stands south
  of the filter rather than east of it, because east of the filter it covers the dryer's own motor box on its west face.

## 2. The feed is staged
- The cyclone cluster's overflow is topped up with a heavy-mineral sand slurry: 3.5 % heavy minerals of the solids at 35 % solids,
  split 25/55/5/3 zircon, ilmenite, rutile and monazite as Wildcatter's heavy mineral sand (dry-mill.md, Sources). The cluster itself is
  not run. The scavenger's tails are drawn off as if to flotation. The gas and chlorine tanks are topped up as if piped. All said in
  the episode.

## 3. WET and NO GAS shown the in-game way
- **NO GAS:** a length of the gas line is taken out on camera (the dryer reads NO GAS at once) and put back (it dries again).
- **WET:** a bag of moist heavy mineral concentrate is offered to the rare-earth roll magnet by hand (a mannequin stands in for the
  player); the separator refuses it with its own WET message. The card keeps the message, because the separator's status line is
  overwritten on its next tick.

## 4. The zircon handoff is carried by hand
- **Found:** a hopper under the induced-roll magnet takes zircon nuggets before nine of them pack into a zircon concentrate item, and the
  chlorinator's hopper refuses nuggets, so a hopper chain would jam.
- **Done:** the scene waits for the first zircon concentrate item and moves it into the chest over the chlorinator's top hopper, a
  mannequin standing on the chlorinator for the player. The narration says it is carried. Wildcatter's `#wildcatter:zircon_feed` tag
  (answer 9 of 2026-11-10) takes it at 56.25 kg; the chlorinator chlorinates at 23.3 kg/h of zircon on camera.

## 5. Half-speed drawing once the chlorinator runs
- With Wildcatter's chlorinator running, the client draws at about half speed at 2880 x 1620 (the server ticks at 6-10 ms). The zircon
  film after 33.4 s and the set film are played at 2x in the episode, which restores real time, and labelled so. Not chased: it is
  Wildcatter's renderer, and the brief's "fix if small" is for Tailings features. **Worth a look by Tim** if other films hit it.

## 6. The hook to episode 5 reworded (needs Tim, wording only)
- **Found:** the plan's hook says copper, lead, zinc and nickel are "too light for gravity". Their sulfides are not light (the mod's
  own DMS figures put copper ore at 3.15 against granite's 2.65): they are too finely disseminated, so the ore is
  ground too fine for spirals to catch.
- **Done:** the narration says the grains are so fine that the ore must be ground very fine to free them, too fine for a spiral, so the
  plant makes them float. PLAN.md is not changed; Tim may want its hook line updated.

## 7. Scheelite, chromite and iron on the spirals
- The narration lists what the spiral bank takes (`Mineral.spirals()`): zircon, ilmenite, rutile, monazite, cassiterite, chromite,
  scheelite and iron ore. Only the first five go on through the dry mill (dry-mill.md, As built #4); the feed here carries four of them.
