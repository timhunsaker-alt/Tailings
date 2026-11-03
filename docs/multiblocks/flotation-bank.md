# The flotation cell bank (DRAFT, not approved)

A **rougher-scavenger-cleaner bank of mechanical flotation cells** for the sulfide ores, the 3x route. Ground fines come from
the cyclone overflow; a sulfide concentrate and a tail come out.

Sources:
- Wills & Finch (2016), chapter 12 (froth flotation).
- Outotec TankCell and Denver DR cell data sheets.
- Fuerstenau, Jameson & Yoon, *Froth Flotation: A Century of Innovation* (2007).

```
fines slurry -> CONDITIONING TANK -> ROUGHER CELLS (x4) -> rougher conc -> CLEANER CELLS (x2) -> final conc -> THICKENER -> FILTER
                 ^ xanthate, frother, lime      \ tail -> SCAVENGER CELLS (x2) -> scav conc back to rougher
                                                                   \ final tail -> tailings (leach in Phase 4b)
```

## Size
- Feed 14 kg/s of solids at 35 % solids, from the crushing circuit.
- Residence about 12 min in the roughers *(the game holds a few seconds, rates real, as in Wildcatter)*.

## Reagents (real doses)
- Potassium amyl xanthate collector: 50 g/t *(unverified)*.
- MIBC frother: 20 g/t.
- Lime to pH 9-10 to depress pyrite: 500 g/t, from Wildcatter's calcining (open: lime is not yet in Wildcatter).
- Copper sulfate to activate sphalerite: 300 g/t.

## Recovery to concentrate (real; game uses these times the liberation from the crushing circuit)
| Mineral | Real recovery | Concentrate grade |
|---|---|---|
| Galena | 90 % | 70 % Pb |
| Sphalerite | 88 % | 52 % Zn |
| Pentlandite | 85 % | 12 % Ni |
| Molybdenite | 85 % | 52 % Mo |
| Cobaltite, acanthite | 80 % | — |
| Platinum reef | 85 % | 100-200 g/t PGM |
| Vanilla copper (chalcopyrite) | 88 % | 28 % Cu |
| Vanilla gold (free gold reports to gravity first; the rest floats) | 85 % | — |

At 90 % liberation and about 85 % flotation, about 77 % of the mineral in the rock reaches concentrate: the **3x** step on the
25 % baseline (decided, VISION.md). For a vanilla copper or gold ore that is 3 ingots per ore instead of 1. Scavenger and cleaner banks (Phase 4a) plus tails leaching raise it toward 95 % (4x).

## Parts
| Part | Cells (W x H x L) | Real-world basis |
|---|---|---|
| `conditioning_tank` | 3 x 4 x 3 | 3 m agitated tank, 15 kW, 480 V. Reagents by hopper on top |
| `flotation_cell` | 3 x 4 x 3 | 20 m3 forced-air cell, 45 kW, 480 V, with a blower header. **Written exception:** a bank is identical cells in series, as at every real plant; recovery grows with cell count |
| `air_blower` | 2 x 2 x 3 | 75 kW, 480 V, feeds up to 8 cells |
| `concentrate_thickener` | 8 x 3 x 8 | 8 m high-rate thickener, 5.5 kW rake |
| `pressure_filter` | 3 x 3 x 5 | Plate filter to 8 % moisture; bags concentrate |

## Open questions
- Cell count per recovery step: fixed bank sizes, or recovery by count like the centrifuge cascade?
- Selective (Pb then Zn) circuits, or one bulk sulfide concentrate to start?
