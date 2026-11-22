# Films for Rock to Metal, episode 3 (branch `films-rtm03`)

The `rtm03_*` client scenes (`Rtm03Scenes`, `TAILINGS_ONLY=rtm03`): the dump station, the crushing circuit, dense-medium separation and
the X-ray sorter, every motor on a Wildcatter gas or diesel set at the industrial voltage. Decided while Tim was away (2026-11-21), each
on the best recommendation.

## 1. Oversize on the dump pocket's bars is now drawn (fixed, small)
- **Found:** the rail dump pocket counts the oversize left on its 300 mm bars (one item in a hundred, 32 choke it; dump-station.md,
  Oversize), but nothing showed it: the pocket looked empty while the train waited at CHOKED.
- **Done:** the pocket now tells the client what lies on its bars when that changes (`DumpBlockEntity.getUpdateTag`, `showBars`), and a
  new `client.DumpRenderer` draws each piece as a boulder in its own rock's texture on the free cells beside the track, heaped as they
  gather (up to 40). Registered on both loaders. No rule changed; saves are untouched.

## 2. The DMS density and the X-ray threshold are set on the parts, not from the PLC
- **Found:** the plan's shots set the medium's density "from 2.6 to 3.3 on the PLC" and step the X-ray threshold "on the PLC". The
  module's DENSITY point is an input to the PLC (it reads the setting); only the X-ray sorter's THRESHOLD and REVERSE are PLC outputs.
- **Done:** both are stepped the in-game way, the module's sneaking-click setting and the sorter's screen step, and the card shows the
  DENSITY and THRESHOLD loop currents. The narration says so. Making DENSITY a PLC output would be a feature, not a film fix: left as is.

## 3. Staged in the films (said in the episode)
- The crushing circuit's ore bin is topped up with copper ore and granite, a block at a time, as if from the dump.
- The DMS feed belt and the X-ray sorter's feed belt are loaded at their tails, a mix of rocks, as if from the screen's gravel face and a jaw.
- The DMS module's water is topped up as if piped. The products' chests are emptied as if hauled away.
- The second train is put on the track loaded, off camera. A mannequin with a pickaxe stands in for the player clearing the bars.

## 4. A belt's head-end chest stalled on a mixed load (fixed, small)
- **Found:** the X-ray sorter's product and waste belts each end over a chest, as the `xrt` film lays them. With five rocks on the
  belts the sorter went FULL within a minute and stayed there. A run dropping into a chest keeps part blocks at its head until it has a
  whole 2.7 t block of one rock and size to bag, and it allowed two blocks' worth **in all**: five rocks of a little over a tonne each
  filled that, none reached a whole block, and the run stopped for good.
- **Done:** the allowance is now two blocks **of each rock and size** (`Belts.containerRoom`). One rock on a belt behaves exactly as
  before. `build` and all 283 NeoForge game tests pass. This is a fix to a stall, not to a rule; flagged here so Tim sees it.
