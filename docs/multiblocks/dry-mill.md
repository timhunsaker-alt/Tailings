# The dry mill: magnetic separation for ilmenite, electrostatic separation for rutile and zircon

**DRAFT — decided on recommendation while Tim was away (2026-11-09); not approved.** Proposed wave **L6**. The choices are listed
in [../questions/specs-tailings.md](../questions/specs-tailings.md) (Later section).

ROADMAP "Later": magnetic separation for ilmenite, and electrostatic separation for rutile and zircon. In a real mineral sands
operation the wet plant's spirals make a **heavy mineral concentrate (HMC)** of mixed ilmenite, rutile, zircon and monazite. A
**dry mill** (mineral separation plant) dries it and splits it by **magnetism** and **conductivity**:
- **ilmenite** is magnetic (and conducts);
- **rutile** (and cassiterite) conducts but is not magnetic;
- **zircon** neither conducts nor is magnetic;
- **monazite** does not conduct but is weakly magnetic, so a high-intensity magnet takes it out of the zircon.

**What it adds to the game (Decision L6-1):** today the spiral bank (gravity-circuit.md) pays each heavy mineral as its own item at
50 %, and the leach takes them to 95 % as a **written game choice** (full-recovery.md: "real tin, zircon and ilmenite are not leached
from tails"). The dry mill gives these minerals their **real top rung**: a scavenger spiral bank on the spirals' tails and the dry
mill on the combined concentrate take them to **95 % (4x)** the way a real mineral sands plant does. The leach exception stays for a
plant without the dry mill.

**Real counterparts:**
- Mineral sands dry mills (Richards Bay, Jacinth-Ambrosia's feed to its separation plant): fluid-bed dryer, rare-earth roll magnets
  (RER), induced-roll magnets (IRM), high-tension (HT) roll and electrostatic plate separators.
- Wildcatter's own `mineral_separator` (zirconium.md) does the same job in one small part for the zirconium chain; this is the plant
  scale for Tailings' concentrate.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory
and is to be checked before approval.

```
 SPIRAL BANK -> concentrate slurry ---------------------------+
      | tails                                                 v
 SCAVENGER SPIRAL BANK -> concentrate slurry ----> HMC THICKENER/FILTER (existing) -> HMC (moist)
      | tails -> flotation as before                          |
                                                              v
                    FLUID-BED DRYER -> RARE-EARTH ROLL MAGNET -> magnetics: ILMENITE
                                             | non-magnetics
                                             v
                                       HT ROLL SEPARATOR -> conductors: RUTILE (and cassiterite)
                                             | non-conductors
                                             v
                                 INDUCED-ROLL MAGNET (high intensity) -> weakly magnetic: MONAZITE
                                             | non-magnetic
                                             v
                                           ZIRCON
```

## Size
- **Feed:** the spirals' concentrate. In the 50 t/h plant that is small: granite's zircon is 8 kg a block (DESIGN.md), so even a
  sandstone-rich feed gives under **1 t/h** of HMC. The dry mill is sized at **2 t/h** *(proposed)*, enough for Wildcatter's heavy
  mineral sand ore run through the plant at its grade.
- **Real capacities:** induced-roll magnets about **4 t/h a metre** of roll for ilmenite sands; HT rolls about **1-2 t/h a metre**
  *(vendor listing, unverified)*. So a 1.5 m roll of each is enough.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `scavenger_spiral_bank` | 4 x 5 x 6 | The spiral bank's twin (gravity-circuit.md), on the spirals' tails: 24 starts, **37 kW** pump *(proposed)*. Concentrate out as concentrate slurry; tails on to flotation |
| `fluid_bed_dryer` | 3 x 4 x 6 | A fluid-bed dryer at about **120 °C** *(proposed)*, fired on Wildcatter **pipeline gas** off a pipe, about **0.3 GJ a tonne** of HMC *(proposed)*, a **30 kW** fluidising blower. Moist HMC in by a hopper on top (the filter's cake) |
| `rer_separator` | 2 x 3 x 3 | A **1.5 m** rare-earth roll magnet, **5.5 kW** *(proposed)*. Takes ilmenite |
| `ht_roll_separator` | 2 x 3 x 4 | A **1.5 m** high-tension roll separator, rolls about 270 mm across, with its high-voltage supply, **7.5 kW** *(proposed)*. Takes the conductors: rutile and cassiterite |
| `irm_separator` | 2 x 3 x 3 | A **1.5 m** induced-roll magnet at high intensity, **11 kW** *(proposed)*. Takes monazite from the non-conductors; zircon passes |
| `product_bins` | as `ore_bin` (dump-station.md) | One bin a product, touching each separator's product face |

All motors at the world's standard industrial low voltage.

## How it runs
- **The scavenger spiral bank** takes the spirals' tails and recovers **another 45 %** of what the block held of each heavy mineral
  *(proposed: so 50 + 45 = 95 %)*, as concentrate slurry. Its concentrate joins the spiral bank's on the thickener.
- **The filter's cake** (moist HMC) goes into the dryer by hopper **instead of out as items** when a dryer touches the filter's
  output *(proposed)*. With no dryer, the filter pays the spiral bank's 50 % as items as it does today, and keeps the
  scavenger's share as a `heavy_mineral_concentrate` item (its assay kept) to be fed to a dryer later: the 4x is paid only through
  the dry mill (Decision L6-2).
- **The separators** pass the dry HMC down the line, each taking its mineral to its own product face. They pay the **top rung**: each
  kg won is paid at the ladder's even 4x **(1.11 a kg won, as `FullRecoveryRates.topRungPay` pays what does not float)**, as the
  mineral's item (`ilmenite_concentrate`, `rutile_concentrate`, tin ingots for cassiterite, `monazite_concentrate` → the existing
  rare-earth route, `zircon_concentrate`).
- **No mineral pays twice:** what the scavenger spirals take never reaches the leach; the leach still takes what is left.
- **Dry only:** wet HMC (not through the dryer) into a separator reads WET.
- **Mixed products** are not modelled: each separator takes all of its mineral *(proposed: real dry mills recycle middlings to reach
  their grade; the game counts the result)*.
- **Reads:** NO FEED, WET, NO GAS (the dryer), FULL, "its output has nowhere to go". **PLC points:** RUN, RUNNING on each.

## Wildcatter's zirconium chain
- `zircon_concentrate` from the dry mill is **the zircon Wildcatter's chlorinator wants**. Proposed: Wildcatter's chlorinator accepts
  Tailings' `zircon_concentrate` as zircon by kg (a Wildcatter compat entry), so a Tailings player skips Wildcatter's small
  `mineral_separator` *(Decision L6-3)*.

## Left out (to ROADMAP)
- Electrostatic plate separators, attritioning and acid washing of the HMC.
- Leucoxene and altered ilmenite grades; the TiO2 grade of the products.
- A titanium pigment plant (Wildcatter Ideas).
- Dust and radioactivity of monazite.

## Game tests to add
- `dry_mill_split`: an HMC of ilmenite, rutile, zircon and monazite comes out at four product faces, each its own mineral.
- `dry_mill_ladder`: a sandstone feed through spirals, scavenger spirals and the dry mill pays 95 % of its zircon and ilmenite (4x),
  and the leach after it finds none of them.
- `dry_mill_wet`: undried HMC into the RER reads WET.

## The wave plan
- **L6**, after the spirals (built) and the top rung (built). It needs no Wildcatter change, except the optional compat entry.

## Sources
- A magnet maker's note on rare-earth and induced-roll magnets: about 4 t/h a metre for ilmenite sands, 3-5 for chromite.
- An HT roll separator maker (AusIMM toolbox): rolls 270 mm across, 1,800 mm long. A vendor listing: 1.5 m HT rolls at 1-3 t/h
  *(unverified)*.
- Wildcatter ores.md: heavy mineral sand, 25 % zircon, 55 % ilmenite, 5 % rutile, 3 % monazite of its heavy minerals.
