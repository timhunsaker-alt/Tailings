# Films for the Rock to Metal revision pass (branch `films-rev2`)

Copy of `videos/longform/rock-to-metal/rev2_decisions.md` (2026-11-22). Items 4 and 5 need Tim.


Each is my best recommendation; all are reversible.

1. **Builder:** rev2 features are opt-in spec keys, so the old specs build unchanged (checked: all five segment to the same
   lengths). `build_ep_pre_rev2.py` is the backup.
2. **Map placement:** top left (the footage's readout cards sit top right; subtitles bottom centre). Lower thirds wait for
   the map to go. Hero metric middle left. Slates lower left above the subtitle band.
3. **Recycle colour is violet**, not amber, so it is never confused with the accent that marks the current stage.
4. **Copper ending (needs Tim):** neither mod smelts copper; the pressure filter pays copper as nuggets packed into ingots.
   The roadmap scripts episode 9 honestly on that, naming the abstraction, and leaves a slot if Tim would rather build a
   smelter line first.
5. **Possible bug (needs Tim, untested):** the by-product plant matches pentlandite and molybdenite concentrate items that are
   never registered (`ByProducts.java:34-40`), so those routes look unreachable. Not fixed: outside this pass.
6. **Episode 4 re-film:** only the seven films with the concentrate thickener in view, from `Rtm04Scenes` unchanged, so every cut
   point in the spec holds. New films in `videos/footage/films-rev2/`; the originals stay.
7. **Episode 5 "(rou":** the line is the tank cell's own status, too long for the card. Fixed in the scene, not the mod:
   `Rtm05Scenes.frothCard` shows the status up to its bracket (the card title already says ROUGHER). Commit 07615742 on
   `films-rev2` (local). The mod's status string is unchanged.
8. **Re-voices kept to four:** ep02 cyclone line (the game splits a fixed share, no size cut) and ep02 teaser (ep03 is another
   plant); ep03 route line and ep03 liberation line (not modelled). Everything else was correct or already framed as real-world.
9. **Speeds:** checked every shot against the film notes. Ep01's 2x shots were already real time; ep03's set and density
   films were playing slow and now play at real time.
10. **Cold open 20.6 s in episode 1:** the two hook clips run 20.5 s with no gaps; kept rather than cut the second sentence.
11. **Episode 5 cold open** stays on the pit row's tank cell (the best froth shot) and is labelled as the pit row, since the
    episode then begins on the small plant.
