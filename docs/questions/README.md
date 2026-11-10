# Questions for Tim (decided while he was away, 2026-11-09)

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back".
Each file here is one wave. Each entry is a decision taken on the best recommendation, with what was chosen, why, and the
alternatives, so it can be changed when Tim goes through them. Nothing here is final until he has.

Format of an entry:

### N. The question
- **Chosen:** what was built.
- **Why:** the reason, with the real-world counterpart.
- **Alternatives:** the other options and what they would change.

## Start here: what needs you (both mods)

These are the only items that wait on you; everything else is built and only needs a yes or a change.

1. **Wildcatter v0.6.0 GitHub release** (`lead-decisions.md` #3). Master is at 0.6.0 and pushed, but creating a public release was
   blocked without you. Once released, Tailings moves to 0.6.0 and the P3 blasting branch merges.
2. **Raw ore items** (`lead-decisions.md` #4, `specs-tailings.md` X-1, X-3): Fortune and copper multiply what the plant pays. The
   recommendation is ore blocks only (Silk Touch, the TBM, blasting). Not built.
3. **Diamonds on the ladder** (`specs-tailings.md` X-2): only by its real route (dense media, X-ray sorting), and only if you want it.
4. **Real residence times** (Wildcatter `plant-crane-gaps.md` #11): coking 18 h, digestion 1 h, precipitation 36 h as real holds.
   They change the game's pace, so they're not built. The plan is written.
5. **Guy wires** (Wildcatter `protection-gaps.md` #20): should an overloaded, unguyed pole warn, lean, or break? The plan is written.
6. **Wildcatter Phase 15, the oilfield** (branch `phase15`, `phase15-oilfield.md`, 25 entries): salt domes, drilling mud, the BOP and
   blowouts, built and tested (55 server scenes), but **not merged**. It changes existing worlds: domes move to a 192-block grid,
   so fields under existing wells can change, and plain-water mud is LOW GEL (land rigs drill at half rate until bentonite is
   mixed in). Say yes and it merges.
7. **A firebox on piped gas** (Wildcatter `phase14-gas-power.md`, last entry): a firebox counts gas in an early game unit (a m3
   burns in 0.4 s), so on real biogas or pipeline gas flows it never stays lit. The recommended fix gives the firebox one real
   heat rate, which changes gas wells under fireboxes.
8. **A pole transformer placed by command** (Wildcatter `protection-gaps.md` #21): turning it to face the pole changes which
   terminals are which in existing worlds.
9. **Ventilation rules** (`l1-ventilation.md` #1, #2): the NO VENTILATION interlock and stale air are built but off by default
   (a config file). Should the interlock default on for new worlds, and should it be a per-world rule instead of a file?
10. **Paste fill's pace** (`l5-paste.md` #19): at the real ~34 m³/h a 360 m³ drive branch takes about 11 hours of play. Scale it?
    And cement (#8) is a placeholder recipe until a cement kiln exists.
11. **The dry mill's share** (`l6-dry-mill.md` #3, #14): only the scavenger spirals' 45 % goes through the dry mill (the filter still
    pays the first spirals' 50 % as today). And should Wildcatter's chlorinator take Tailings' zircon concentrate?

12. **Dense-medium separation** (`l7-dms.md` #4, #5, #8): the light ores (salt, potash, borax) float at any setting and are lost; DMS
    sinks go round again and lose about 3 % of an ore block; ferrosilicon is a placeholder recipe until the ferroalloy plant.
13. **The XRT sorter** (`l8-xrt.md` #3, #5, #6): the default threshold (Zeff 12.0) sits just above deepslate (11.96) and below
    silver ore (12.08); light ores (graphite, bauxite, borax) read below quartz and go to waste unless a reverse mode is added; it has
    its own 160 kW compressor because Wildcatter's is a small instrument-air unit.
14. **The walking dragline** (branch `p5-big-machines`, `p6-dragline.md` #1, #3, #18): the boom is five 15 m parts, not ten of 7.5 m
    (a cell is a whole metre); its house fills its whole 5,760-cell box (a hollow shell would save about 15,000 block states); and its PLC
    points are on its switch house outlet, not its tub, since the tub lifts and walks.
15. **The pit line** (branch `p5-big-machines`, `p7-pit-line.md` #11, #14, #21, #27): three close reclaim feeders draw 41 % of a pile
    (one draws 26 %); the weigher's 50 t/h step; whether a big mill should refuse a direct start or just trip its breaker; whether
    Wildcatter should get a large-bore slurry pipe. P7c, pit flotation (#33, #36, #37, #40): turn the shared empty model on for the older
    big parts too (about 10,000 fewer files); the row's air on instrument tubing or a real air header; should the concentrate
    thickener refuse uncleaned concentrate; and Wildcatter's decant tower takes about 72 t/h against the line's 1,670 t/h, so raise its
    rate? P5, P6 and P7 (all of it) pass 205 server scenes together and merge to main after the Wildcatter v0.6.0 release.

## Every file, by wave

| Mod | File | Entries | What |
|---|---|---|---|
| Tailings | `lead-decisions.md` | 4 | the lead's own calls (ground tips, the dispatch film, the release, raw ores) |
| Tailings | `p2a-open-pit.md` | 5 | self-driving trucks: turning round, look-ahead, one-way holds, the dump pocket, a dry fuel bay |
| Tailings | `p2b-dispatch.md` | 12 | the haul dispatch office |
| Tailings | `p4-dry-stack.md` | 20 | the tailings filter, the load-out, dumps, berms, the track dozer |
| Tailings | `p3-blasting.md` (branch `p3-blasting`) | 19 | the blasthole drill, charging, the blasting machine, the shot |
| Tailings | `tbm-gaps.md` | 12 | the TBM, its gantries and the mine trains |
| Tailings | `plant-gaps.md` | 12 | belts, feed faces, the grizzly, the playthrough's plant |
| Tailings | `plc-13i.md` | 7 | the last PLC signals (sorter, TBM course, slurry) |
| Tailings | `art-wave.md` | 5 | the Ludo art and the vehicle skins |
| Tailings | `art-p3.md` (branch `p3-blasting`) | 7 | the blasting and dry-stack art |
| Tailings | `playthrough.md` | 4 | the playthrough fix: motors run on 60 s, failing fast |
| Tailings | `l2-l3-trains.md` | 20 | battery locomotives and their charger, bottom-dump cars over a track grizzly (merged; #7, #12, #16 need you) |
| Tailings | `l1-ventilation.md` (merged) | 19 | the drive's fan, duct, booster, the TBM's duct cassette, stale air (#1, #2 need you: the rules are off by default) |
| Tailings | `l4-shaft.md` (merged) | 21 | the shaft and hoist: headframe, hoist house, skips in balance, the measuring flask, the headframe bin (#1, #5, #20 need you) |
| Tailings | `l5-paste.md` (merged) | 22 | paste backfill: the paste filter, mixer and pump, fill bulkheads and outlet, curing, cement (#8, #14, #19 need you) |
| Tailings | `l6-dry-mill.md` (merged) | 16 | the dry mill: scavenger spirals, the fluid-bed dryer, the three separators, the HMC bag (#3, #9, #14 need you; #14 is the zircon compat, not built) |
| Tailings | `l7-dms.md` (merged) | 16 | dense-medium separation: deslime screen, DMS cyclone module, drain-and-rinse screens, medium recovery, ferrosilicon (#4, #5, #8 need you) |
| Tailings | `l8-xrt.md` (merged) | 16 | the X-ray ore sorter: reading a piece's atomic number, the threshold steps, its own compressor, light ores (#3, #5, #6 need you) |
| Tailings | `gaps2.md` (branch `gaps2`) | 8 | known gaps closed by adding: fuel bay pipe scene, press and load-out PLC points, paste filter and mixer gauges, the headframe bin into a hopper, a wall breached beside wet paste, paste curing while unloaded, muck on paste (#4, #5, #6 change existing worlds) |
| Tailings | `p5-big-machines.md` (branch `p5-big-machines`) | 35 | the switch house, trailing cables, the ultra truck, both shovels |
| Tailings | `p6-dragline.md` (branch `p5-big-machines`) | 20 | the walking dragline: its parts, strip, cast, walk, power, PLC points on its outlet |
| Tailings | `p7-pit-line.md` (branch `p5-big-machines`) | 44 | the gyratory and stockpile (P7a), the SAG line (P7b), pit flotation and the 45 m thickener (P7c) |
| Tailings | `specs-tailings.md` | 56 | the specs written for every remaining idea (P5 to P7, mills, shaft and hoist, ventilation, sorting) |
| Wildcatter | `ammonia-plant.md` | 14 | the ammonia plant (0.6.0) |
| Wildcatter | `phase14-gas-power.md` | 19 | Phase 14b-e: gas and power quick wins (and the solver's closing angle) |
| Wildcatter | `protection-gaps.md` | 28 | relays, switchgear, poles, service drops |
| Wildcatter | `plant-crane-gaps.md` | 16 | plants and cranes |
| Wildcatter | `art-wave.md` | 7 | the Ludo art |
| Wildcatter | `plc-gaps.md` | 12 | PLC gaps: cable cores, the 24 V supply, rack owners, ladder jumps, FBD wire routing, every signal run in the world |
| Wildcatter | `misc-gaps.md` | 9 | steam plant lines, the 1 MW alternator, land rig cables, coke oven gas derating, spool icons |
| Wildcatter | `phase15-oilfield.md` (branch `phase15`) | 25 | salt domes, drilling mud, the BOP and blowouts |
| Wildcatter | `specs-wildcatter.md` | 103 | the specs written for every unscheduled idea (see `docs/ideas-plan.md`) |

Films of the waves were sent to you in the session as they were made.
