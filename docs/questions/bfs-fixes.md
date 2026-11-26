# Built From Scratch quest fixes: judgement calls (2026-11-26, branch `bfs-fixes`)

From the quest check (`videos/longform/built-from-scratch/QUEST_CHECK.md`), approved by Tim with the episode order that follows the
book. Wildcatter's side is in its own `docs/questions/bfs-fixes.md`.

### 1. watch_the_wall: what it requires now
- **Chosen:** `stamp_mill` (the chapter's first quest) in place of `dragline`; the text says prisms on a pit's benches, and the alarm
  stops the dragline or shovel.
- **Why:** the survey station fires `slope_watched` with any three prisms in sight (`SurveyStationBlockEntity`, about line 79); its
  recipe is a spyglass, redstone and steel, the prisms glass, copper and a stick. Nothing from the dragline is used.
- **Alternatives:** `cabled_pit` or `blasting` (the pit is where a highwall stands, but neither is a real need).

### 2. The haul truck and loader
- **Chosen:** `tailings:haul_truck` and `tailings:wheel_loader` as item tasks of the gyratory, named in its text.
- **Why:** the dump pocket fills only from a haul truck tipping (`HaulTruckEntity.tip`); the loader fills the truck. Both are crafted from
  vanilla items. The ultra haul truck is the shovel's, so the plain truck stands for the gyratory.

### 3. Wildcatter machines in Tailings' quests
- **Chosen:** the submerged arc furnace as a task of `ferrosilicon` (the bag comes only from it; the quest already requires
  `wildcatter:coke`), and the nitric acid unit and emulsion blender as tasks of `blasting`, which also requires `wildcatter:diesel` now.
  The neutraliser is named in the text.
- **Why:** these are the quests that need them; Wildcatter's chapters never use them.

### 4. The check's machine table (`QuestReach.MACHINES`)
- Updated for Wildcatter 0.10.2: quicklime under casing_point, barite under choke_kill, the OBM package under oil_mud, calcined coke
  under aluminium (the quests that now teach each machine), and the ferrosilicon bag under `tailings:ferrosilicon`.
- The scene now runs in the full suite on this branch (it was a check-only scene on `bfs-check`).

### 5. Building against an unreleased Wildcatter
- `wildcatter_version=0.10.2` is not on Wildcatter's GitHub releases yet. Until it is, build with
  `-Pwildcatter_repo=file:/Volumes/KINGSTON/home/minecraft-mods/wildcatter-local-releases` (the jars are in its `v0.10.2/`).
