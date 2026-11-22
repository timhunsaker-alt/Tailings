# The copper smelter line: concentrate to cathode to ingot

**DRAFT: decided on recommendation while Tim was asleep (2026-11-22); not approved.** Tim decided on 2026-11-22 to build a copper
smelter line in Tailings, so that Rock to Metal's episode 9, "Concentrate to Metal", ends on real smelting and refining rather than
on the pressure filter's direct payout. The choices are in [questions/copper-smelter.md](questions/copper-smelter.md) (#4 on).

Before this, neither mod had a copper smelter, converter, refinery or cathode. Wildcatter's ROADMAP lists "copper refining ... flash
smelting" as not built. The pressure filter paid concentrate copper straight out as nuggets packed into `minecraft:copper_ingot`.
That payout stays as an early-game shortcut (#5). This line is the full route, and it pays more copper from each ore.

**Real counterparts:**
- A **flash smelting furnace** on preheated, unenriched air, as the first flash furnaces ran (1949 on), with its reaction shaft,
  settler and uptake. It is small: 6 t/h of concentrate. Modern oxygen flash furnaces run 1,000 to 4,000 t/d.
- A **Peirce-Smith converter**, 3.0 m x 6.1 m (10 x 20 ft), the smallest common size, blowing air through the matte from a tuyere line.
- A **rotary anode furnace** for fire refining: air to burn out the sulfur, then natural gas to take out the oxygen ("poling").
- A **single anode casting wheel**, 16 moulds.
- An **electrorefining tankhouse**: anodes and stainless starter sheets in copper sulfate and sulfuric acid electrolyte, cells in
  series on one rectifier.
- Davenport, King, Schlesinger and Biswas, *Extractive Metallurgy of Copper*, 4th ed. (2002): flash smelting, converting, fire
  refining and electrorefining, including the per-tonne figures below.

A figure marked *(proposed)* is a textbook value, a scaled value, or a proposal. A figure marked *(unverified)* is from memory and is
to be checked before approval.

```
 thickened concentrate (pipe) -> PRESSURE FILTER --(touching)--> CONCENTRATE DRYER <- pipeline gas
                                                                      | dry concentrate (touching)
                    silica sand (hopper) + pipeline gas  ->  FLASH SMELTING FURNACE --- slag: counted, to waste (2.5 % of the copper)
                                                     ^            | matte, 55 % Cu (touching)   \__ off-gas (burner gas, 11 % SO2) --+
                              converter slag (4 % of its copper)  v                                                                 |
                    silica sand (hopper) ------------------>  PEIRCE-SMITH CONVERTER ------------ off-gas (burner gas) -------------+
                                                                  | blister copper, 98.5 % (touching)                               |
                                     pipeline gas  ------->  ANODE FURNACE                                                          v
                                                                  | anode copper, 99.5 % (touching)            Wildcatter's acid plant converter
                                                             ANODE CASTING WHEEL -> copper anodes, 400 kg (hopper)   -> sulfuric acid
                                                                                          v
                                                             ELECTROREFINING TANKHOUSE -> copper cathodes, 57 kg (hopper)
                                                                                          v
                                                             blast furnace: a cathode melts to 2 copper ingots
```

## Size: 6 t/h of concentrate, 1.68 t/h of copper
- **Sized to Wildcatter's acid plant.** A real smelter is sized together with its acid plant. Wildcatter's sulfur-burning acid plant
  is rated at 150 t/d of H2SO4, 0.57 kg/s of sulfur (acid-plant.md). This smelter's off-gas carries **0.53 kg/s** of sulfur, 94 % of
  that, so **one smelter fills one acid plant**: about **141 t/d of acid**.
- **The plant it serves.** The 50 t/h plant on copper ore makes up to about 5.5 t/h of copper at the game grade. Fed from mixed ore,
  as a plant usually is, it runs near this line's rate. The pit plant's one concentrate thickener (43 t/h) would fill several of
  these lines, as a real 1,000 t/h concentrator ships to a smelter several times this size.
- **The game's concentrate is its copper.** The plant's concentrate assay carries only the mineral (`PitFlotation.concentrateSolids`),
  so the line counts copper. For its gas, flux, slag and fuel it takes a real concentrate of **28 % Cu and 32 % S** (chalcopyrite
  with some bornite and pyrite; real concentrates run 20-35 % Cu): 3.57 kg of concentrate and **1.14 kg of sulfur a kg of copper**
  *(proposed)*.
- **Per ore block of copper ore** (flotation's 300 kg of copper, 10.5 ingots): the line pays **292.5 kg**, 10.24 ingots' worth as
  5.1 cathodes, and its off-gas makes about **1.05 t of acid** in Wildcatter's acid plant. The filter's direct payout pays 8.9 ingots
  in a new world (#5).

| Stream | At the rating | Figure |
|---|---|---|
| Dry concentrate | 6.0 t/h | 1,680 kg/h of Cu, 1,920 kg/h of S |
| Matte | 3.0 t/h at 55 % Cu, 22 % S | 0.40 kg of S a kg of Cu *(proposed: air flash furnaces made 45-60 % matte)* |
| Smelting slag | 3.6 t/h (0.6 t a t of concentrate) | 2.5 % of the copper fed, about 1.2 % Cu in the slag *(proposed: flash slag 1-2 % Cu before cleaning)* |
| Converter slag | back to the furnace | 4 % of the converter's copper *(proposed: converter slag 4-8 % Cu)* |
| Off-gas sulfur | furnace 1,248 kg/h, converter 672 kg/h | on the pipe as Wildcatter **burner gas**, at its 11 % SO2 (`AcidPlant.gasPerSulfur`): unenriched air flash furnaces and converters both make 10-14 % SO2 gas |
| Silica flux | 900 kg/h to the furnace, 252 kg/h to the converter | 0.15 t a t of concentrate; 0.15 kg a kg of copper *(proposed)*. A block of sand is 1.6 t |
| Pipeline gas | dryer 47, furnace 158, anode furnace 49 m3/h | 0.30 GJ/t of concentrate to dry 9 % to 0.3 % moisture; 1.0 GJ/t of concentrate for an air flash furnace's burners; 1.1 GJ/t of copper to fire refine (burner and poling gas) *(proposed)*. At Wildcatter's 38 MJ/m3 |
| Anodes | 4.1 an hour | 400 kg each (real anodes are 350-400 kg) |
| Cathodes | 28.7 an hour | 57.1 kg each, two ingots' copper (real cathodes are 40-60 kg on a 7-10 day cycle) |

## Parts
Voltages are the world's standard tiers, read from Wildcatter, never as numbers: "LV" is the industrial tier (480 V or 400 V), "MV"
the medium tier (4.16 kV or 3.3 kV).

| Part | Cells (W x H x L) | Real-world basis and values |
|---|---|---|
| `concentrate_dryer` | 4 x 5 x 16 | A **2.2 m x 14 m rotary drum dryer** on its tyres and rollers, a gas burner at its feed end and the exhaust fan and bag house at its discharge end. It dries filter cake from about 9 % moisture to 0.3 %, because a flash furnace burns its concentrate in suspension and wet concentrate will not burn. **75 kW at LV**: drum drive and exhaust fan *(proposed)*. Takes the cake from a pressure filter touching it; pipeline gas on the east face of its front cells |
| `flash_furnace` | 7 x 12 x 16 | A **flash smelting furnace**: a 3.5 m reaction shaft, 6 m tall, over a 12 m x 5 m settler, and the uptake at the back. Dry concentrate and silica burn in a jet of preheated air as they fall. The settler parts matte under slag. **132 kW at LV**: process air fan and the preheater's fan *(proposed: about 5,000 Nm3/h of air at 30 kPa)*. Dry concentrate from the dryer touching it; silica sand by a hopper on top; pipeline gas on the east face of its front cells; **off-gas out of the east face of its back cell, top layer** |
| `copper_converter` | 5 x 5 x 9 | A **Peirce-Smith converter, 3.0 m x 6.1 m**, on its riding rings, with its tuyere line, hood and the air main from its blower. It blows the matte's iron to slag and its sulfur to SO2, leaving blister copper. **450 kW at MV**: the converter blower *(proposed: about 250 Nm3/min at 120 kPa)*. Matte from the furnace touching it, and its slag back into that furnace; silica sand by a hopper on top; **off-gas out of the east face of its back cell, top layer** |
| `anode_furnace` | 5 x 5 x 9 | A **3.2 m x 7 m rotary anode furnace**: air through tuyeres to take the sulfur to about 0.002 %, then natural gas to take the oxygen to about 0.15 % *(textbook)*. **55 kW at LV**: the drive and the combustion fan *(proposed)*. Blister from the converter touching it; pipeline gas on the east face of its front cells |
| `anode_casting_wheel` | 8 x 3 x 8 | A **single casting wheel of 16 copper moulds**, 25 t/h, with its tundish, ladle and take-off crane, and the cooling sprays. **30 kW at LV**: the wheel drive and hydraulics *(proposed)*. Anode copper from the anode furnace touching it; **copper anodes** to a hopper under it or a hand |
| `refinery_tankhouse` | 9 x 3 x 24 | An **electrorefining tankhouse section: 72 cells in series**, in four rows of 18, at **20.5 kA** and **0.30 V a cell**: **21.6 V, 443 kW DC**. At 96 % current efficiency and Faraday's 1.186 g of copper an ampere-hour that is **1.68 t/h** (derived). **490 kW at MV**: the rectifier's input and the electrolyte circulation pumps *(proposed)*. Copper anodes by a hopper on top or a hand; **copper cathodes** to a hopper under it or a hand |

### Items
- `tailings:copper_anode`: 400 kg of anode copper, a whole ore block's copper (14 ingots). It stacks to 16.
- `tailings:copper_cathode`: 57.1 kg of cathode copper, two ingots' worth. **A blast furnace melts one to 2 copper ingots** (real
  cathode copper is melted in a shaft furnace and cast). It stacks to 64.

## Nozzles and faces (each part drawn facing north; main cell at the north-west bottom corner, its motor box on that cell's west face)
- The parts hand on by touching, as the dry mill does: filter to dryer, dryer to furnace, furnace to converter (and the converter's
  slag back), converter to anode furnace, anode furnace to casting wheel. In the real plant a pneumatic conveyor carries the dry
  concentrate, and ladles on an overhead crane carry the matte, blister and anode copper.
- **Pipeline gas** (or any natural gas on Wildcatter's pipe) in on the east face of the front cells (z 0) of the dryer, the furnace
  and the anode furnace.
- **Off-gas:** the furnace's and the converter's out of the east face of their back cell, top layer (the uptake's and the hood's flue), as Wildcatter **burner gas**.
  Pipe it to the front of a Wildcatter acid plant `converter` (acid-plant.md), which takes burner gas on its north face. The smelter's
  gas replaces the sulfur melter, furnace and boiler. A furnace or converter whose off-gas has nowhere to go stops: **OFF-GAS HAS
  NOWHERE TO GO**. A smelter must not vent, as Wildcatter's roasters do not.
- **Silica sand** (`minecraft:sand` or `minecraft:red_sand`) by a hopper on top of the furnace and the converter, 1.6 t a block,
  four blocks held.
- **Anodes** into the tankhouse by a hopper on its top; anodes out of the wheel and cathodes out of the tankhouse into a hopper under
  each, or a hand.

## How it runs
- **Each part runs its own motor** while it has work and room for its product, as every Tailings part does; its rate follows its
  motor's output, up to the line's 1.68 t/h of copper. There is no controller.
- **The pressure filter with a concentrate dryer touching it** sends its copper to the dryer, not to the ladder's nuggets; it waits
  (FULL) while the dryer's bin is full. Every other mineral in its cake is paid as before.
- **The dryer** holds 3 t of copper (about 11 t of concentrate) *(proposed)*. NO GAS below half its gas.
- **The furnace** needs flux and gas, and room for its off-gas: NO FLUX, NO GAS, OFF-GAS HAS NOWHERE TO GO, and "needs a converter
  touching it". It smelts into the converter's matte, up to 8 t of copper held there, and counts its slag (tonnes and the copper in
  it).
- **The converter** needs flux and room for its off-gas, and the furnace touching it for its slag; it blows into the anode furnace.
  A real converter works in batches (charge, slag blow, copper blow, pour: a 6-10 hour cycle); this one blows continuously, so the
  line runs smoothly at the plant's own pace *(#9)*.
- **The anode furnace** fire refines continuously too, on gas, into the casting wheel.
- **The casting wheel** casts a 400 kg anode each 58 s of its 25 t/h, into its two output stacks: FULL when they are full.
- **The tankhouse** takes up to 24 anodes (9.6 t) and plates their copper at its rating, paying a cathode for each 57.1 kg.
- **Readouts:** each part says what it is doing, what stops it, its copper held, and its totals: concentrate in, slag (t) and the
  copper lost in it, sulfur to the acid plant, gas burnt, anodes cast, cathodes pulled.
- **PLC points:** each part's RUN, RUNNING, STALLED, AUTO, AMPS and FILL (its copper over its hold), as every motor part has. The
  furnace and the converter add **OFF-GAS BACKED UP**; the casting wheel **ANODE CAST** and the tankhouse **CATHODE PULLED**, each on
  for a second.
- **Removing a part** stops what needs it. What a part holds is kept with the part, and lost if it is broken.

## Power
- **1.23 MW installed:** 292 kW at LV (dryer, furnace, anode furnace, casting wheel) and 940 kW at MV (converter, tankhouse). The
  converter's blower and the tankhouse's rectifier each want a 1 MW set or better, stepped up to MV. A real smelter of this size
  also runs an oxygen plant, which this one does without (#7).

## Pays better than the shortcut
- **The filter's direct payout is the early-game shortcut.** It stands for the concentrate sold to a smelter somewhere else: a
  concentrate sale pays for about 96.5 % of its copper, less treatment and refining charges, about **85 %** of the copper's value in
  all *(proposed)*. In a world made after this line (world rule `copper_shortcut`, on in new worlds, off in existing ones), the filter
  pays **85 %** of its copper this way. A world saved before it pays as it always did.
- **The smelter line pays 97.5 %:** only the smelting slag's 2.5 % is lost, because the converter's slag goes back and refining loses
  next to nothing. Per ore block through flotation: **10.24 ingots as cathodes against 8.9**, about 15 % more, and the acid besides.

## Built now, and later
- **Built now (all six parts, the route in order):** dryer, flash furnace, converter, anode furnace, casting wheel, tankhouse; the
  anode and cathode items; the cathode melted to ingots in a blast furnace; off-gas to Wildcatter's acid plant.
- **Later (to ROADMAP):**
  - an **electric slag-cleaning furnace** on the furnace slag: it would cut the 2.5 % loss to about 1 %. It is cheap, but a seventh
    machine. The slag is counted, not handled;
  - an **oxygen plant** and enriched air: double the furnace's rate, no burner gas, a richer off-gas;
  - **anode scrap** (real tankhouses send back about 15 % of each anode to the anode furnace) and **anode slimes** carrying the
    concentrate's gold, silver and selenium to a precious-metals plant; the electrolyte's bleed, heating and additives;
  - a **cathode shaft furnace and casting machine** in place of the blast furnace;
  - batch converting (the converter's cycle) and the crane and ladles between the furnaces;
  - the slag as an item (granulated slag) to a dump;
  - the electrowinning bank paying cathodes (real SX-EW makes cathode) rather than ingots.

## Game tests
- `smelter_dryer`, `smelter_furnace`, `smelter_converter`, `smelter_anode_furnace`, `smelter_casting`, `smelter_tankhouse`: each unit
  on its own, forced on, fed what it takes, giving what it gives; the stops (NO FLUX, NO GAS, OFF-GAS HAS NOWHERE TO GO).
- `smelter_shortcut`: the filter pays copper at 85 % with no dryer touching it, and sends it all to a dryer touching it.
- `smelter_line`: thickened concentrate to cathodes and ingots through all six parts, every motor on Wildcatter generator sets
  (the LV parts on one, the converter and tankhouse each on their own set stepped up to MV), the off-gas into a stock tank, and the
  copper balance checked: cathodes plus what is held plus the slag's copper equals the copper fed.
- Unit test `SmelterTest`: the chemistry and the rates.

## Sources
- Davenport et al., *Extractive Metallurgy of Copper*, 4th ed. (2002): flash furnace matte 50-70 % Cu and slag 1-2 % Cu before cleaning;
  Peirce-Smith converters, 4 x 11 m typical and smaller ones from 3 m; converter slag 4-8 % Cu returned to the smelting furnace; fire
  refining to 0.002 % S and about 0.15 % O; anodes 350-400 kg; electrorefining at 0.3 V a cell and 250-350 A/m², current efficiency
  93-98 % *(unverified page by page)*.
- Faraday's law: 63.546 / (2 x 26.8) = **1.186 g of copper an ampere-hour**.
- Wildcatter acid-plant.md: 150 t/d, burner gas at 11 % SO2, the converter's 1.2 x rated intake.
