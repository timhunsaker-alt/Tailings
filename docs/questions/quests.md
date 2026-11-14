# Tailings' quest chapter (2026-11-14)

Wildcatter 0.8.0's quest book (Wildcatter `docs/quests.md`) takes a chapter from any mod. Tailings ships
`data/tailings/wildcatter_quests/open_pit.json` and fires four milestones from its machines (`quest.Milestones`). Plan:
`/Users/timhunsaker/.claude/plans/elegant-swinging-starfish.md`, "Tailings chapter" (approved by Tim 2026-11-14). Format as in
`README.md` here. Each entry was **decided on recommendation; for Tim**: none needs him before it can stand, but each can be changed.

### 1. Grinding and flotation come after MV switchgear, not after Three-phase power
- **Chosen:** the quest `tailings:flotation` needs `tailings:crusher_line` and `wildcatter:switchgear`. The plan put "the 480 V crusher
  line and flotation" after Three-phase power.
- **Why:** the flotation bank, conditioning tank and blower are 480 V, but the only feed for them is fines slurry from the ball mill and
  cyclones, and the ball mill's 600 kW motor is on the medium-voltage tier. A player following the book could not run flotation on 480 V
  alone. The crusher line itself is all 480 V and stays after Three-phase (`wildcatter:motor`).
- **Alternatives:** keep the plan's order and say in the text that the mill needs medium voltage; or put the ball mill on 480 V (a game
  change, not a quest change).

### 2. The zircon compat is wired now
- **Chosen:** Tailings adds `tailings:zircon_concentrate` to `#wildcatter:zircon_feed` (`data/wildcatter/tags/item/zircon_feed.json`)
  and gives its kg file (`data/tailings/wildcatter/zircon_feed/zircon_concentrate.json`, 56.25 kg of ZrSiO4 an item). Scene
  `quest_zircon_feed`.
- **Why:** the plan's quest "zircon to Wildcatter's chlorinator" is only true if the chlorinator takes it. Tim said yes to this on
  2026-11-10 (answer 9, `l6-dry-mill.md` #14); it waited on Wildcatter, and Wildcatter 0.8.0 has the tag and kg files (`ZirconFeed`).
  56.25 kg is the figure `l6-dry-mill.md` #14 proposed. Data only; nothing in an existing world changes except that a chlorinator's
  hopper now takes the concentrate.
- **Alternatives:** leave the compat for its own change and word the quest as "zircon concentrate" only.

### 3. The chapter: "Mine and mill", `tailings:open_pit`, order 110
- **Chosen:** the plan's file name (so the id is `tailings:open_pit`), titled "Mine and mill" since it covers the mill as much as the pit;
  order 110 (the spec's example), after `wildcatter:getting_started`. Eleven quests: the plan's seven bullets, with the crusher line and
  flotation, the leach and zircon, and the shovel and dragline each split in two.
- **Why:** each half has its own prerequisites (entry 1; the leach needs sulfuric acid, zircon the chlorinator's chlor-alkali).
- **Alternatives:** title "Open pit"; one quest per plan bullet.

### 4. Tasks name one or two key parts, not every part
- **Chosen:** each quest's tasks are one to three items that mark the build (the jaw and cone; the ball mill and flotation bank; the
  switch house and a reel), plus the milestone where there is one. The dragline quest is its milestone alone. The text lists the rest.
- **Why:** an item task is met only while the player carries the item, and all of a quest's tasks must be met at once. Asking for the
  dragline's eleven parts at once would mean carrying them all before building; a running dragline already proves them. The same keeps an
  existing world's quests reachable (Wildcatter's decision 2).
- **Alternatives:** every part as a task (a checklist, and hard to finish once built).

### 5. Where each milestone fires
- **Chosen:**
  - `tailings:crusher_running`: a jaw or cone crusher on the tick it crushes (not the grizzly, gyratory or pebble crusher);
  - `tailings:flotation_running`: the small flotation bank on the tick it floats, and a tank cell of the pit row floating as cells 1-8
    (not the scavenger, cleaner or column cells);
  - `tailings:blast_fired`: the blasting machine when its shot is done, at the machine, so the player at the firing point gets it;
  - `tailings:dragline_running`: the dragline each tick it works a cycle or walks with power, at its position.
- **Why:** the places that already know the machine is doing its work; Wildcatter limits each position and key to once in 5 s.
- **Alternatives:** fire the crusher key from the gyratory too (its quest has no milestone now).

### 6. Ferrosilicon also needs Cables
- **Chosen:** `tailings:ferrosilicon` needs `wildcatter:coke` (the plan's "after Coke") and `wildcatter:cables`, and the crusher line.
- **Why:** ferrosilicon is made with metallurgical coke, but the DMS cyclone module it is for is crafted with an industrial cable.
- **Alternatives:** the ferrosilicon item alone, after Coke.

### 7. Blasting needs only Ammonia
- **Chosen:** `tailings:blasting` needs `wildcatter:ammonia` (the emulsion matrix), nothing from Tailings.
- **Why:** the blasthole drill, emulsion truck and blasting machine are diesel or hand-crafted and need no cable or medium voltage.

### 8. The rewards
- **Chosen:** iron ingots (4) for the stamp mill, belt conveyor (12) for the crusher line, lime and frother (8 each) for flotation,
  survey stakes (16) for the cabled pit, a blast logger for the first shot. Nothing for the quests past the plastic gate.
- **Why:** small, useful for the next step, and each made by hand (Wildcatter's check refuses anything only a machine makes; ferrosilicon
  is on its never list).
- **Alternatives:** more or fewer.
