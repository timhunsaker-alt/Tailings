# Full recovery: Phase 4 (decisions taken by Tim 2026-11-06; figures still proposed)

Taking the ladder from flotation's 75 % (3x) to the 95 % the top rung pays (4x), and giving the tails somewhere to go. Tim said to go on to
Phase 4 on 2026-11-06, and answered its four decisions the same day.

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
- **Minerals that do not float (Tim, 2026-11-06: reach 4x too):** 50 % on the spirals (gravity-circuit.md), then the leach takes them to 95 %. They have no 3x rung. This is a **written game choice**: real tin, zircon and ilmenite are not leached from tails, and this is the one place the ladder is more uniform than real plants.

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

**Heap leach on a lined pad** *(ROADMAP 4b)*: `heap_pad`, one 10 x 3 x 10 part with its liner in it, 15 kW for its pump, at a tenth of the plant's rate *(proposed)*, fed by belt from the waste bin, sprayed with acid, draining to the
electrowinning bank. It is the first use for the waste bin. **Tim, 2026-11-06:** its own wave after the tank route, paying country rock's nuggets at the 50 % rate.

## 4c. By-products
| From | By-product, per the concentrate's metal *(proposed, real concentrate grades)* | Real-world route |
|---|---|---|
| Sphalerite | cadmium 0.3 %, indium 0.01 % | Zinc plant residues |
| Galena | silver 0.5 % | Anode slimes, Parkes |
| Molybdenite | rhenium 0.05 % | Roaster flue dust |
| Pentlandite | cobalt 3 % | Nickel matte refining |
| Heavy mineral sand | monazite rare earths | Already a concentrate; a rare-earth oxide item |

- A **`byproduct_plant`** (5 x 5 x 8, 250 kW at 480 V *(proposed)*) takes the item the ladder pays for a mineral, 20 s an item *(proposed)*, and gives it back with what it carried. A by-product item is 10 kg *(proposed)*.
- Real grades are too small to feel in play, so they are scaled ×10 as ores.md scales grades *(proposed)*.
- **Tim, 2026-11-06:** new by-product items from one plant (cadmium, indium, rhenium, rare earth oxide), and Wildcatter's silver.

## 4d. The lined tailings cell (shared with Wildcatter)
Replaces `tailings_pond_placeholder`. It belongs in **Wildcatter**, because the uranium mill needs it too (Wildcatter ROADMAP Ideas): see Wildcatter's Phase 12.
- Player-built: a `cell_liner` floor and walls in any rectangle, one `decant_tower`. Holds about 2.2 t a block, so a plant's day needs about 1,500 blocks of volume.
- Tails slurry settles; **the decant returns water on a pipe** *(closing the Known gap on overflow water)*.
- **A breach or an overtopped cell (Tim, 2026-11-06):** a visible seep and the cell stops taking tails until it is repaired. No contaminated ground.

## As built
- **No new pipe products:** the scavengers send a poor concentrate on as `concentrate slurry`; the regrind mill's product is `conditioned slurry`, for the cleaners.
- **The top rung's even 4x** is paid at the cells (`FullRecoveryRates.topRungPay`): 1.5 per kg won for what floats, 1.11 for what does not. The heap takes that back, so it pays a true 50 %.
- **Pregnant liquor carries its mineral at up to 30 %** *(a game figure)*: the liquor's volume does not matter here.
- **The cells have no rectifier section of their own to wire:** they are a 400 kW motor load at 480 V, and raffinate is not returned.

- **A blower serves a scavenger bank as well as a flotation bank.** The playthrough found this: the blower read NOT BUILT, and the scene had hidden it by force-running.
- **4d, in the playthrough:** 8 x 8 cell, 2 high, 279 t. The tails come over the rim on steel pipe to the top of the tower, so the walls stay whole, and the water leaves the tower's side and goes back over the rim to the mill's header, which the process water pump also feeds.
- **Power, as played:** the top rung is about 950 kW on two 1 MW sets: the 400 kW cells on one, the rest and the 37 kW water pump on the other. The pump does not fit on the flotation set: with it, starting the 360 kW bank tripped the set at 56.1 Hz.

## Left out (ROADMAP)
- Selective lead-then-zinc flotation: its own wave, 4e (selective-flotation.md).
- Reagent doses at the scavengers and cleaners.
- Solvent extraction as its own machine (inside `electrowinning_bank`), acid mist, cathode stripping.
- Cyanide and thiosulfate gold leaching.

## Waves
Wildcatter Phase 12 first, as Tailings compiles against its release. Then 4a, 4b, 4c, 4d, each with its spec approved, models on a contact sheet, a NeoForge scene and the playthrough extended to it.
