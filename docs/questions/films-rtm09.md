# Films for Rock to Metal episode 9, "Concentrate to Metal" (branch `films-rtm09`)

Made 2026-11-22 while Tim was away. Each is my best recommendation; all are reversible. Items 1 and 2 need Tim.

1. **Needs Tim: the by-product plant's cobalt share looks high.** `ByProducts.java:26` pays 0.30 kg of cobalt for each kg of
   pentlandite, so one pentlandite concentrate item (435 kg) gives about 13 cobalt items. Real pentlandite carries cobalt in the
   order of 1 %, and nickel concentrates well under that. Not changed, and not filmed: episode 9 narrates no by-product.
2. **Needs Tim: rhenium cannot be shown in a film.** Molybdenite's 0.005 kg a kg (`ByProducts.java:25`) makes a 10 kg rhenium item
   from about a hundred molybdenite concentrate items, one every 20 s: over half an hour. The grade is real; the item size makes it
   unfilmable. Not changed.
3. **Wildcatter's moly roaster hopper is not shown.** It is on Wildcatter master (095dcdc7) but not in 0.8.1, the release Tailings
   builds and runs against (`gradle.properties`), so the episode leaves it out rather than film a Wildcatter build no player has.
4. **The anode and cathode icons are Ludo pictures** (one try each; commit "Art: the copper anode and cathode icons…"). They were
   script-drawn placeholders, and the episode shows both in item frames.
5. **The tankhouse sits in a pit under the casting wheel**, so a single hopper between them drops each anode into its cells: the
   spec's "anodes to a hopper under it" and "anodes by a hopper on its top" met in one block. A real refinery moves anodes by crane;
   the episode says so. The rectifier sits against its north end, in the pit.
6. **Staged, and said so in the episode:** thickened concentrate on both filters' inlets at the line's rate plus 5 %, as if from the
   concentrate thickener; pipeline gas in three stock tanks; the final absorber's process water; the sand chests over the furnace's
   and converter's hoppers; cathodes put into the blast furnace by hand; the anode, cathode and ingot in item frames.
7. **The shortcut filter is fed at the same rate as the line**, so its ingots and the line's cathodes can be compared on film.
8. **No smoke on the furnaces.** Both send their gas by pipe to the acid plant (a smelter must not vent), so the line shows none;
   the episode makes that a point ("neither furnace has a chimney") rather than add particles.
9. **The line is filmed at its own pace, in real time.** The first anode is cast about a quarter of an hour after start-up; the films
   wait for it. A trial that topped the wheel up instead was dropped: the run's totals would have carried staged copper.
10. **Why long client films drew at half speed (episode 8's `rtm08_drop`, and this episode's second take):** the game drops to 10 fps
   after ten minutes with no input (`FramerateLimitTracker`, LONG_AFK), and a client game test waits a frame a tick. `Rtm09Scenes`
   sets the inactivity limit to "minimized" so the films stay real time. Other film classes could do the same; not changed here.
11. **Not shown: the by-product plant and Wildcatter's nickel route** (items 1-3). The episode names lead, zinc, tin and zircon over
   episode 6's footage instead, and spends its length on the smelter line.
12. **The episode:** `videos/longform/rock-to-metal/ep09/ep09.mp4`, 9:23, 1,374 words. Weak: the smelter parts have no moving parts or
   glow on camera (the cards carry the action); the shortcut film's card is wide, so its text is small at phone size; the casting
   wheel's card line was cut short after the cast and is cropped out in the edit.

Tests: `build` passes; the full NeoForge suite passes (301 of 301).
