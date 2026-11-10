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
| Tailings | `p5-big-machines.md` (branch `p5-big-machines`) | 35 | the switch house, trailing cables, the ultra truck, both shovels |
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
