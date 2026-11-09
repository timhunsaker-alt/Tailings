# Selective lead-zinc flotation: Phase 4e (decided by Tim 2026-11-06; figures still proposed)

Tim said yes to selective lead-then-zinc flotation on 2026-11-06, closing the DECISION left open in flotation-bank.md.

**Sources:**
- Wills & Finch, *Wills' Mineral Processing Technology*, 8th ed. (2016), chapter 12: differential flotation of lead-zinc ores.
- Fuerstenau, Jameson & Yoon, *Froth Flotation: A Century of Innovation* (2007).

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal.

## Why it needs a decision
Today's filter sorts every mineral in a cake into its own item, so a plain bank on mixed feed already pays clean lead and clean zinc. A
real plant cannot do that, which is why it floats the two apart. Selective flotation only matters in play if the bulk route gets worse.

**Tim, 2026-11-06: on a plain bank, the zinc is lost.** A bulk cake smelts to lead and the zinc goes to slag, as in a real lead furnace.
The bank's 75 % of the zinc is gone; the scavengers and the leach still pay their shares, 20 % in all.

This also touches ordinary rock, which carries traces of both (DESIGN.md), so a plant on country rock pays a little less zinc unless it
runs selectively. Feed holding only one of the two never changes.

## The circuit
```
fines -> CONDITIONING TANK (selective) -> BANK 1 -> lead concentrate -> THICKENER -> FILTER -> lead
          ^ zinc sulfate, xanthate,          |
            MIBC, lime; no copper sulfate    v tails, zinc still depressed
                                       ACTIVATION TANK -> BANK 2 -> zinc concentrate -> THICKENER -> FILTER -> zinc
                                          ^ copper sulfate,  |
                                            lime, xanthate,  v tails
                                            MIBC          SCAVENGER BANK and on, as Phase 4
```

Real order: galena floats first while sphalerite is held down, then copper sulfate activates the sphalerite at a higher pH.

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `conditioning_tank`, existing | 3 x 4 x 3 | Gains a setting, BULK or SELECTIVE, clicked with an empty hand as the sorter is. SELECTIVE doses zinc sulfate in place of copper sulfate and holds sphalerite down |
| `activation_tank`, new | 3 x 4 x 3 | A second 3 m agitated tank, **15 kW at 480 V**, like the conditioning tank. Tails slurry in, conditioned slurry out, reagents by hopper on top |
| `flotation_bank`, a second one | 7 x 4 x 13 | Unchanged, with its own blower. Its own thickener and filter follow it |

- **New item:** `zinc_sulfate`, by hopper, **300 g/t** *(proposed; real plants use a few hundred g/t, often with cyanide or sulfite, left out here)*. Placeholder recipe: a zinc ingot and sulfur, as copper sulfate is made.
- **Second-stage doses:** the existing figures, on the tails: copper sulfate 300 g/t, lime 500 g/t, xanthate 50 g/t, MIBC 20 g/t.

## How it runs
- **SELECTIVE, short of zinc sulfate:** the tank stops, NO REAGENT (Zinc Sulfate). It never falls back to bulk unannounced.
- **Bank 1** floats everything that floats except the held-down sphalerite, at the 75 % rung. That includes copper or gold in the feed.
- **The activation tank** frees the sphalerite. **Bank 2** floats it at 75 %, and only it: the galena bank 1 left does not float a second time, or a block would pay over its rung.
- **The scavengers on** take what is left at their existing shares, so the ladder still comes to 95 %.
- **A selective tank's tails sent straight on, with no activation tank:** the held-down zinc goes to the cell and is lost.

## Power
The added parts come to about **470 kW**: bank 360, blower 75, tank 15, thickener 5.5, filter 15. That is about one more 1 MW set on a plant already near the top of its two.

## How it is built
- **No Wildcatter release needed:** a slurry's assay is keyed by strings, so held-down sphalerite travels as its own assay entry (`tailings:depressed_sphalerite`).
- **Model:** `activation_tank`, on a contact sheet before the commit, in Wildcatter's materials. The `zinc_sulfate` item has a Ludo icon since the 2026-11-09 art wave, as the other reagents do.
- **Scenes:** `selective_float` (both pay their full rung through two filters), `selective_no_depressant`, `no_activation_tank` (zinc lost), `bulk_loses_zinc`, `bulk_one_mineral` (feed with only one of the two pays as before).
- **Playthrough:** extended only if the drive meets real lead and zinc ore. Its copper feed proves the trace behaviour otherwise, and the report will say which.

## As built
- **The setting** is a sneaking click with an empty hand on the conditioning tank. It reads BULK or SELECTIVE, and a new tank is BULK.
- **Any mix counts as bulk,** down to the traces in country rock, and the rule lives in the bank: a bank fed plain galena and plain sphalerite together loses the sphalerite it floats. The scavengers do not have this rule, so their shares pay as before.
- **The tank has a fifth reagent slot,** so copper sulfate left in it cannot block zinc sulfate.
- **Proved by:** unit tests, five NeoForge scenes and a force-run film. The playthrough runs a plain bank on copper feed and did not build this circuit; it still pays the same.

## Left out (ROADMAP)
- Copper-then-moly: a different circuit, a bulk float first, then sodium hydrosulfide holds the copper down.
- Cyanide and sulfite as depressants; pH as a number.
- Zinc in the lead concentrate and lead in the zinc concentrate: real plants accept a few percent of each. Here each stage's cake is clean.
