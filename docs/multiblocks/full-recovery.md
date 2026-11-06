# Full recovery: Phase 4 (DRAFT, not yet approved by Tim)

Taking the ladder from flotation's 75 % (3x) to the 95 % the top rung pays (4x), and giving the tails somewhere to go. Tim said to go on to
Phase 4 on 2026-11-06. The choices below are recommendations; each marked **DECISION** waits for Tim.

**Sources:**
- Wills & Finch, *Wills' Mineral Processing Technology*, 8th ed. (2016): scavenging, regrind, cleaning.
- Schlesinger et al., *Extractive Metallurgy of Copper*, 6th ed. (2022): heap and tank leach, solvent extraction, electrowinning.
- ICOLD Bulletin 139 and the Global Industry Standard on Tailings Management (2020): lined cells, decant, seepage.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal.

## The ladder, for a mineral that floats
| Step | Part | Recovered, of what the block holds | Total | Played |
|---|---|---|---|---|
| Rougher, the existing bank | `flotation_bank` | 75 % | 75 % | 3x |
| 4a Scavenge, regrind, clean | `scavenger_bank`, `regrind_mill`, `cleaner_bank` | 10 % | 85 % | |
| 4b Leach the tails | `leach_tank`, `ccd_thickener`, `electrowinning_bank` | 10 % | **95 %** | **4x** |

- The steps are a real plant's. The exact shares are set so they add to the ladder's written 95 %. Nothing pays over what the block holds (VISION.md).
- Each step takes only what the one before left, so a plant can stop at any rung and still pay that rung.
- **Minerals that do not float** go 50 % on the spirals (gravity-circuit.md). **DECISION 1:** the recommendation is that they stop at 2x, since real tin,
  zircon and ilmenite are not leached from tails. VISION.md's table says 95 % is "only what the real best plant loses", and these are not recovered that way.
  The alternative is a share at 4b so every mineral can reach 4x.

```
BANK -> tails -> SCAVENGER BANK -> tails -> LEACH TANK -> CCD THICKENER -> washed tails -> LINED CELL
                    |  ^                       ^ acid        |
                    v  | cleaner tails                       v pregnant liquor
              REGRIND MILL -> CLEANER BANK                ELECTROWINNING BANK -> metal, raffinate back to the leach tank
                                  | concentrate
                                  v
                              THICKENER -> FILTER
```

## 4a. Scavenging, regrinding, cleaning
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `scavenger_bank` | 5 x 4 x 9 | Four 20 m³ tank cells, 4 x 45 kW at 480 V, on the bank's tails. Air off a blower it touches |
| `regrind_mill` | 3 x 3 x 5 | A 2.1 m ball mill, **150 kW at 480 V** *(proposed)*, regrinding scavenger concentrate to about 40 µm |
| `cleaner_bank` | 4 x 3 x 6 | Four 3 m³ cells, 4 x 11 kW at 480 V *(proposed)*. Its concentrate goes to the thickener |

Reagents are carried in the pulp from the conditioner *(a game choice, Left out below)*.

## 4b. Leaching the tails
| Part | Cells | Real-world basis |
|---|---|---|
| `leach_tank` | 5 x 6 x 5 | An agitated rubber-lined tank, **30 kW at 480 V** *(proposed)*. **20 kg of sulfuric acid a tonne of tails** *(proposed)*, off a Wildcatter pipe. NO ACID stops it |
| `ccd_thickener` | Wildcatter's size, 12 x 4 x 12 | Counter-current washing; washed tails to the cell, pregnant liquor on |
| `electrowinning_bank` | 5 x 3 x 9 | Cathode cells at about 2 kWh a kg of copper. **400 kW at 480 V through its own rectifier section** *(proposed)*. Pays through the filter's nugget rule |

**Heap leach on a lined pad** *(ROADMAP 4b)*: `heap_pad`, player-built from liner, fed by belt from the waste bin, sprayed with acid, draining to the
electrowinning bank. It is the first use for the waste bin. **DECISION 2:** the recommendation is to build it as its own wave after the tank route, paying only country-rock
copper and uranium nuggets at the 50 % rate.

## 4c. By-products
| From | By-product, per the concentrate's metal *(proposed, real concentrate grades)* | Real-world route |
|---|---|---|
| Sphalerite | cadmium 0.3 %, indium 0.01 % | Zinc plant residues |
| Galena | silver 0.5 % | Anode slimes, Parkes |
| Molybdenite | rhenium 0.05 % | Roaster flue dust |
| Pentlandite | cobalt 3 % | Nickel matte refining |
| Heavy mineral sand | monazite rare earths | Already a concentrate; a rare-earth oxide item |

- A **`byproduct_plant`** (5 x 5 x 8, 250 kW at 480 V *(proposed)*) takes those concentrates as items and returns them with the by-product nuggets, as Wildcatter's refineries take items.
- Real grades are too small to feel in play, so they are scaled ×10 as ores.md scales grades *(proposed)*.
- **DECISION 3:** the recommendation is new by-product items for what Wildcatter has no use for yet (cadmium, indium, rhenium, rare earth oxide), and Wildcatter's silver.

## 4d. The lined tailings cell (shared with Wildcatter)
Replaces `tailings_pond_placeholder`. It belongs in **Wildcatter**, because the uranium mill needs it too (Wildcatter ROADMAP Ideas): see Wildcatter's Phase 12.
- Player-built: a `cell_liner` floor and walls in any rectangle, one `decant_tower`. Holds about 2.2 t a block, so a plant's day needs about 1,500 blocks of volume.
- Tails slurry settles; **the decant returns water on a pipe** *(closing the Known gap on overflow water)*.
- A breach in the liner or an overtopped berm leaks: **DECISION 4:** the recommendation is a visible seep and the cell stopping, no contaminated ground, as Tim decided for broken lining in R6.

## Left out (ROADMAP)
- Selective lead-then-zinc flotation (still Tim's open DECISION, flotation-bank.md).
- Reagent doses at the scavengers and cleaners.
- Solvent extraction as its own machine (inside `electrowinning_bank`), acid mist, cathode stripping.
- Cyanide and thiosulfate gold leaching.

## Waves
Wildcatter Phase 12 first, as Tailings compiles against its release. Then 4a, 4b, 4c, 4d, each with its spec approved, models on a contact sheet, a NeoForge scene and the playthrough extended to it.
