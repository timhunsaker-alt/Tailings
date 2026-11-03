# The crushing circuit (DRAFT, not approved)

A **two-stage crushing and screening circuit** with a ball mill, like a small base-metal concentrator. It turns run-of-mine rock
from the haulage into flotation feed, and it sorts every rock type into its own sized products.

Sources:
- Wills & Finch, *Wills' Mineral Processing Technology*, 8th ed. (2016): crushers, screens, Bond work index.
- Metso *Crushing and Screening Handbook*: C-series jaws, HP cone crushers.
- SME *Mineral Processing Handbook* (1985).

A figure marked *(unverified)* is a textbook value, a scaled value, or a proposal.

```
ROM rock (ore bin) -> GRIZZLY -> JAW CRUSHER -> belt -> DOUBLE-DECK SCREEN -> +40 mm -> CONE CRUSHER -> back to screen
                        \ -150 mm bypass                    \ 6-40 mm gravel     \ -6 mm sand -> BALL MILL + CYCLONE -> flotation feed
```

## Size
- **Rock:** 50 t/h *(proposed)*, about 14 kg/s, a small underground mine.
- **Power:** Bond's law, W = 10 Wi (1/√P80 − 1/√F80) kWh/t. Granite Wi about 15 kWh/t; cobble taken as andesite-like 18
  *(unverified)*. From 600 mm to 150 µm takes about 12-15 kWh/t, so about 0.7 MW at 50 t/h.

## The ladder of sizes
| Product | Size | Made by | Gives |
|---|---|---|---|
| Crushed rock | 40-150 mm | jaw | aggregate; nothing recovered yet |
| Gravel | 6-40 mm | cone, screened | vanilla gravel from cobble; a "crushed X" item for other rocks |
| Sand | 0.15-6 mm | cone, screened | vanilla sand from sandstone or cobble; gravity feed (2x route) |
| Fines | under 150 µm | ball mill, cyclone overflow | flotation feed (3x route) |

## What each rock carries *(proposed: crustal abundance × 10, as ores.md scales grades)*
| Rock | Main minerals | Recoverable trace (game) |
|---|---|---|
| Cobblestone / stone | quartz, feldspar | iron nuggets (magnetite), sphalerite and galena nuggets, rarely |
| Granite | quartz, K-feldspar, mica | zircon, monazite, cassiterite, a little molybdenite |
| Diorite | plagioclase, hornblende | magnetite, ilmenite |
| Andesite | plagioclase, pyroxene | copper sulfides (porphyry), trace gold |
| Deepslate | as stone, deeper | pentlandite, chromite traces |
| Tuff | volcanic glass | zeolite, a little uranium |
| Sandstone | quartz | heavy mineral sand, roll-front uranium |
| Wildcatter ores | as ores.md | their ore mineral at the ores.md game grade |
| Vanilla iron, copper, gold ores (and raw ore items) | magnetite or hematite; chalcopyrite; native gold | the metal for 1 to 4 ingots per ore, by the stage ladder (VISION.md) |

## Nuggets (decided with Tim, 2026-11-02)
Processing ordinary blocks (cobble, stone, granite, diorite, andesite, deepslate, tuff, sandstone) yields **ore nuggets**:
small amounts of the minerals each rock really holds, from the table above.
- **The item:** one nugget per mineral, e.g. `tailings:zircon_nugget`, `tailings:cassiterite_nugget`; vanilla's iron and gold
  nuggets are used where they exist.
- **Size:** a nugget is **1/9 of that mineral's Wildcatter item** (by kg of mineral), as vanilla nuggets are 1/9 of an ingot.
- **Combining:** 9 nuggets craft into one mineral item in a crafting grid; the item then goes to its smelting or plant route as
  any mineral item does (smelting for galena, cassiterite, sphalerite, acanthite; the Wildcatter plants for the rest).
- **How many:** the plant keeps a running kg total of each trace mineral across all the rock it treats, and puts out a
  nugget each time a nugget's worth is recovered. Recovery is the same ladder as ores (gravity 50 %, flotation 75 %, full
  95 %), so no rock ever yields more than its written mineral list. Example *(unverified)*: granite at 10x crustal zircon is
  about 0.3 % zircon, 8 kg a block; at 50 % that is about 4 kg a block.
- **Which stage:** nuggets come out of the stage that recovers that mineral: gravity for cassiterite, zircon, monazite and
  magnetite; flotation for sphalerite and galena. The crushers alone give no nuggets.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `grizzly` | 3 x 2 x 4 | Static bar grizzly, 150 mm gap, over the jaw's feed |
| `jaw_crusher` | 3 x 4 x 4 | C100 class: 110 kW, 480 V, closed side setting 100 mm *(unverified)* |
| `cone_crusher` | 3 x 4 x 3 | HP200 class: 132 kW, 480 V, closed side 13 mm *(unverified)* |
| `double_deck_screen` | 3 x 3 x 6 | 1.8 x 4.8 m inclined screen, 40 mm and 6 mm decks, 22 kW |
| `belt_conveyor` | 1 x 1 x n | 600 mm belt, 2 m/s, 7.5 kW per 30 m *(proposed)* |
| `ball_mill` | 4 x 4 x 6 | 3.2 m x 4.5 m overflow ball mill, 600 kW, 4.16 kV |
| `cyclone_cluster` | 2 x 4 x 2 | 250 mm hydrocyclones, cut point 150 µm, fed by a 45 kW slurry pump |

## Recovery
The crushing circuit itself recovers nothing. It **liberates**: it sets what fraction of the mineral is free for the next stage.
Liberation at sand size about 60 %, at fines 90 % *(proposed; real values depend on grain size)*.

## Open questions
- Does vanilla gravel and sand count as Tailings products, or do all rocks get "crushed X" items?
- Hand-crank starter crusher for Phase 1b: what real tool (a stamp mill?).
