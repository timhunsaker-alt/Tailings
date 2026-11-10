# Tailings specs — decided on recommendation while Tim was away (2026-11-09)

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back
... lets get all ideas done or planed out as much as possible without my input". These are the choices taken while writing the
draft specs for everything still unbuilt in Tailings. Each spec they belong to is marked DRAFT and is not approved. Format as
[README.md](README.md). Numbers are by section (P3-1, L6-2, ...) so the specs can point at them.

The specs:
- [open-pit-p3-blasting.md](../multiblocks/open-pit-p3-blasting.md) — P3
- [open-pit-p4-dry-stack.md](../multiblocks/open-pit-p4-dry-stack.md) — P4
- [open-pit-p5-big-machines.md](../multiblocks/open-pit-p5-big-machines.md) — P5
- [primary-gyratory-stockpile.md](../multiblocks/primary-gyratory-stockpile.md), [sag-mill-line.md](../multiblocks/sag-mill-line.md),
  [pit-flotation.md](../multiblocks/pit-flotation.md) — P7a, P7b, P7c
- [ventilation.md](../multiblocks/ventilation.md) L1, [battery-locomotive.md](../multiblocks/battery-locomotive.md) L2,
  [bottom-dump-cars.md](../multiblocks/bottom-dump-cars.md) L3, [shaft-and-hoist.md](../multiblocks/shaft-and-hoist.md) L4,
  [paste-backfill.md](../multiblocks/paste-backfill.md) L5, [dry-mill.md](../multiblocks/dry-mill.md) L6,
  [dense-medium-separation.md](../multiblocks/dense-medium-separation.md) L7, [xrt-ore-sorter.md](../multiblocks/xrt-ore-sorter.md) L8

## Proposed changes to approved specs (not made; for the lead to reconcile)
- **open-pit.md, Emulsion truck / Powder factor:** the example block becomes 7 x 8 x **14** m with a **16 m** hole (14 m bench, 2 m
  subdrill): 784 m³, 2,117 t, 828 kg, **0.39 kg/t** (was 14.5 m and 0.37) — P3-3.
- **open-pit.md, The blast, "Ground left":** "the floor at the subdrill depth" becomes "the floor at the **bench floor** (the hole's
  depth less its subdrill); the subdrill zone is fractured, not broken" — P3-6.
- **open-pit.md, Rotary blasthole drill:** "Electric off the pit cable ... or diesel" becomes **diesel in P3, electric from P5** — P3-1.
- **open-pit.md, Haul trucks, ultra column:** top speed **67.6 km/h** (was 64); fuel tank **3,785 L** (was about 4,000, proposed);
  length 15.08 m and height 7.71 m to the canopy are now checked (were unverified) — P5-3.
- **open-pit.md, Tailings filter:** a **tailings thickener** goes ahead of the filter — P4-1.
- **open-pit.md, Haul roads: LOADOUT stops:** at a load-out the stake marks the **gate**, and the truck drives through forward — P4-3.
- **open-pit.md, Proposed waves, P7:** P7 becomes three waves, P7a-c, and includes flotation and the tails at the pit's rate — P7-1.
- **full-recovery.md:** the leach's "written game choice" for heavy minerals stays, but the dry mill gives them a real route — L6-1.
- **crushing-circuit.md, the gravel face:** built with the DMS (L7-3). Found already built in the plant-gaps wave and used as it is
  (`l7-dms.md` #2): no change needed.
- **Rock / Feeds (code, not a spec):** raw ore items stop being plant feed — X-1.

## P3: drill, emulsion, blast

### P3-1. Is the blasthole drill diesel or electric in P3?
- **Chosen:** diesel in P3 (a C27-class engine, about 600 kW, *unverified*), burning by work as the trucks do; the 522 kW electric drive
  is a screen choice from P5 when a trailing cable reaches it.
- **Why:** the pit switch house and trailing cables are P5. A P3 drill that needs them could not run. Real 271-class drills are sold
  both ways.
- **Alternatives:** move the switch house into P3 (P3 grows by a wave); or electric only and P3 waits for P5.

### P3-2. Is the drill a contraption of parts or a single entity?
- **Chosen:** a contraption of four parts (crawlers, deck, mast, dust collector), as open-pit.md says ("Contraption, like the rope
  shovel").
- **Why:** it is a 26 m machine; the approved spec chose it.
- **Alternatives:** one entity like the loader (much simpler to code; would need a written exception to "big parts").

### P3-3. The default bench and hole
- **Chosen:** 14 m bench, 2 m subdrill, 16 m hole, 7 x 8 m staggered, 4 m stemming, 12 m charge, 828 kg, 0.39 kg/t.
- **Why:** whole blocks (open-pit.md's 14.5 + 1.5 is not), under the 16.8 m single pass, inside the rope shovel's 15 m reach. Burden 26
  hole diameters and spacing 30 are hard-rock textbook values; subdrill 0.29 of the burden.
- **Alternatives:** 15 m bench and 1 m subdrill (fills the rope shovel's reach, but subdrill under real practice); 12 m bench for the
  hydraulic shovel's reach.

### P3-4. Stemming 4 m, and when a hole blows out
- **Chosen:** keep the approved 4 m default; a hole with under **2 m** of stemming blows out.
- **Why:** real stemming for a 270 mm hole is 20-30 diameters (5.4-8 m), so 4 m is short; but a longer default would cut the charge
  and the powder factor under the approved example. 2 m makes "not stemmed" a clear mistake, not a tuning question.
- **Alternatives:** a 6 m default (charge 10 m, 690 kg, 0.33 kg/t), real-er; or a blowout threshold scaled to burden (0.7 B = 4.9 m).

### P3-5. How a hole breaks: its own box toward its free face
- **Chosen:** each hole breaks a box burden deep toward its free face, spacing wide, collar to bench floor; a shot with no free face
  (a box cut) still breaks and heaves its swell upward.
- **Why:** it is the simplest geometry that matches what a real row does and gives a clean bench edge; real box cuts heave.
- **Alternatives:** a radius round each hole (ragged edges, harder to plan); refuse a shot with no free face (real blasters do fire box
  cuts).

### P3-6. Where the floor is left
- **Chosen:** the break goes to the **bench floor**; the subdrill below is fractured, not broken, and the floor is flat at grade.
- **Why:** subdrill exists to pull the floor to grade; breaking it too would leave the next bench 2 m low each time. open-pit.md's
  wording ("the floor at the subdrill depth") reads the other way; a proposed change.
- **Alternatives:** break to the subdrill depth as written (each bench drops by the subdrill).

### P3-7. What a blast does to soft ground and to machines in the shot
- **Chosen:** soft ground in a hole's box is left as it is; a shot will not fire while any part, block entity or vehicle is inside the
  volume it breaks.
- **Why:** soft ground is already diggable; and the blast must not wreck builds silently. Real practice clears the bench.
- **Alternatives:** break soft ground to muck too (no gain); let the blast destroy what is in it (realistic, unforgiving).

### P3-8. The emulsion truck's pump rate
- **Chosen:** 1,200 kg/min (20 kg/s), scaled 4x from a real 300 kg/min MMU: a hole in 41 s.
- **Why:** at the real rate a 30-hole pattern takes 1.4 hours to charge; the drill is already scaled about 8x.
- **Alternatives:** the real 300 kg/min; 8x to match the drill.

### P3-9. How the emulsion truck is filled
- **Chosen:** a new small part, `matrix_fill_point`, on Wildcatter pipe from the matrix tank or IBC, filling at 1 t/min.
- **Why:** real trucks fill at a filling point; it mirrors the fuel bay.
- **Alternatives:** the truck fills from any adjacent Wildcatter tank directly (no new part).

### P3-10. Gassing sleep time
- **Chosen:** 60 s; fired sooner the hole counts half its charge.
- **Why:** real gassed emulsion needs time in the hole to reach its density; it makes "charge, then fire" a sequence. Real time is
  about 30-60 min *(unverified)*.
- **Alternatives:** no sleep time (simpler); a longer one.

### P3-11. Tying in and the firing point
- **Chosen:** a `blast_logger` item logs each hole and writes its delay (row 65 ms, hole 25 ms); the `blasting_machine` must stand 50-500
  m from every hole; ARM then FIRE, key in; a player in the danger zone needs a second FIRE press.
- **Why:** real electronic systems work exactly this way (logger, lead line, blasting machine); keeping the firing point out of the
  harm radius makes the safe way the easy way.
- **Alternatives:** the blasting machine fires any primed holes within range without logging (simpler, less real); refuse to fire with
  a player in the zone (safer, but a game cannot stop a player walking back in after the press).

### P3-12. Harm and flyrock
- **Chosen:** damage 30 x (1 − r/50) within 50 m (explosion damage, armour counts); flyrock only from blowouts (6 blocks), short burden
  (3) and overcharge; lands up to 100 m, 10 damage, breaks only glass and leaves.
- **Why:** a well-designed real blast throws little flyrock; most comes from those causes. The numbers make 20 m dangerous (18) and 40
  m a scratch (6), matching the approved test.
- **Alternatives:** flyrock from every hole (noisier, less real); a harm curve by charge weight (scaled distance) — more real, harder to
  read.

### P3-13. Misfires
- **Chosen:** no misfires by chance; a misfire is a hole logged without an answering detonator, without a booster, or undercharged. It
  keeps its charge; the next shot waits 30 s; a bucket stops at it; it is fired alone or washed out.
- **Why:** every misfire has a cause the player can find; digging into one is the real accident.
- **Alternatives:** a small random rate (real, but it punishes a correct pattern).

### P3-14. Booster and detonator recipes
- **Chosen:** booster: 1 TNT → 4 (400 g each); detonator: 1 TNT, 1 copper ingot, 1 redstone → 8. PETN in a real detonator is a written
  exception (TNT stands in), as open-pit.md already says.
- **Why:** Tim decided TNT; redstone stands for the electronics until Wildcatter has a circuit item.
- **Alternatives:** a Wildcatter electronics item if one exists; a PETN item made from Wildcatter's nitric acid (real, more chemistry).

### P3-15. Is one ammonia plant enough?
- **Chosen:** not changed here; flagged. One plant makes 46 kg/h of matrix, enough for about **124 t/h** of blasted rock at 0.37 kg/t:
  more than the 50 t/h plant, but the pit line (P7, 1,000 t/h) needs about **8 times** that, from 8 steam reformers' hydrogen.
- **Why:** the ammonia plant is approved at one reformer's size; scaling it is a Wildcatter question.
- **Alternatives:** a Wildcatter "large ammonia plant" spec at P7 time; or soft ground and fewer holes carry the rest.

## P4: dry stack, dumps, dozer

### P4-1. A thickener ahead of the filter
- **Chosen:** a new `tailings_thickener` (15 m, 11 kW) takes the 35 % tails to 60 % before the filter, its overflow water back to the
  plant.
- **Why:** real presses are fed from a thickener; filtering 35 % slurry would triple the press. It also returns the water.
- **Alternatives:** the filter takes 35 % tails straight (no new part, a written stand-in).

### P4-2. The filter runs in batches
- **Chosen:** a 15-minute cycle dropping 15 t of cake each time.
- **Why:** a filter press is a batch machine; the drop is the thing to see and hear. The rate is still 60 t/h.
- **Alternatives:** a continuous rate (simpler; a belt would smooth it anyway).

### P4-3. The load-out's size, rate and stake
- **Chosen:** a 12 x 10 x 12 frame with a lane 10 m wide and 9 m clear; 2 t/s; at a LOADOUT stake the truck drives through and stops
  with its body under the gate.
- **Why:** the ultra is 9.8 m over its body and loads at 7.0 m; drive-through is how real load-outs work; 2 t/s fills a 100 t truck in
  under a minute.
- **Alternatives:** a back-in load-out (as a dump); a slower gate.

### P4-4. Berms, and a truck at a crest without one
- **Chosen:** a berm is 1 block for the 100 t truck and 2 for the ultra (mid-axle, 30 CFR 56.9300); a self-driving truck will not back to
  an unbermed crest (NO BERM); a player-driven truck backed over a 3 m drop goes over and is wrecked, dropping its item and load.
- **Why:** the mid-axle rule is the real one; driverless trucks only tip at bermed points; keeping the item makes a mistake cost time,
  not a truck.
- **Alternatives:** the truck is lost (harsher); berms only decorative.

### P4-5. Tipping where there is no crest
- **Chosen:** a DUMP POINT with no 3 m drop behind it is a paddock dump: the truck tips on the flat.
- **Why:** real paddock dumping; lets a dump start on flat ground.
- **Alternatives:** require a crest.

### P4-6. The dozer
- **Chosen:** driven only; three blade positions (CARRY, CUT, RAISED); cut to grade at level, 5, 8 or 10 %; a berm mode; no ripper; no
  autonomy.
- **Why:** cutting ramps at a haul road's grade and pushing berms are what a pit needs a dozer for.
- **Alternatives:** blade only (no grade setting: players cut ramps by hand); an autonomous dozer (real, much more work).

Built on these choices (2026-11-09); what building it decided is in [p4-dry-stack.md](p4-dry-stack.md).

## P5: power and big machines

### P5-1. The switch house
- **Chosen:** no transformer: medium voltage in, four 1,200 A outlets at the same voltage, each with a breaker and a ground-check
  relay; the house's bus 2,000 A.
- **Why:** Wildcatter's medium-voltage tier is what the approved spec named; 1,200 A covers the dragline's 6 MW peak. Real pits often
  run 7.2 kV or more; that would need a Wildcatter transformer between.
- **Alternatives:** a switch house with a transformer from the world's distribution voltage (more real for big pits, a Wildcatter
  part).

### P5-2. The trailing cable
- **Chosen:** counted along the machine's path, 400 m an item, drawn lying on the ground, a resistance on the solver (about 0.1 Ω/km,
  *proposed*), crossable by vehicles.
- **Why:** like the TBM's, plus drawing it, which the TBM lacks.
- **Alternatives:** cable as placed blocks (real, but a 400 m line of blocks is a mess); cable damage when run over.

### P5-3. Correcting the ultra truck's figures
- **Chosen:** top speed 67.6 km/h and a 3,785 L tank from the spec sheet (open-pit.md has 64 and about 4,000).
- **Why:** checked against the manufacturer's sheet.
- **Alternatives:** keep the approved numbers.

### P5-4. The hydraulic shovel: entity or contraption, diesel or electric
- **Chosen:** one entity (like the loader), diesel 1,119 kW, with its real electric variant (860 kW) on a trailing cable as a screen
  choice.
- **Why:** it is one rigid machine; the electric variant uses P5's cable for free.
- **Alternatives:** a contraption of parts (more real art, more code).

### P5-5. The rope shovel's behaviour
- **Chosen:** six parts as a contraption; double-side loading in AUTO; a part dipper last; stops LOW VOLTAGE under 90 %.
- **Why:** real practice; the low-voltage stop makes the cable's drop matter.
- **Alternatives:** single-side loading; no voltage check.

### P5-6. Can the wheel loader load the ultra?
- **Chosen:** no: "too high to load" (6 m reach against a 7.0 m loading height).
- **Why:** real 992-class loaders are not matched to 360 t trucks; open-pit.md's table already says "not done in practice".
- **Alternatives:** allow it at 18 passes.

## P7: the pit-sized line

### P7-1. Does P7 include flotation and the tails?
- **Chosen:** yes: a third spec, P7c (pit-flotation.md): a row of eight 160 m³ tank cells, two column cleaners, a large blower, a 45 m
  tails thickener to Wildcatter's lined cell.
- **Why:** Tim named the gyratory, the stockpile and the SAG line; but 1,000 t/h of fines slurry would need 20 flotation banks, which is
  tiling. Without P7c the line's product has nowhere to go.
- **Alternatives:** feed the SAG line into many existing banks (tiling); or size the line at 50 t/h (pointless).

### P7-2. The line's rate and the crusher
- **Chosen:** 1,000 t/h (24,000 t/day): one 32 ft SAG (8.2 MW), one 24 x 36 ft ball mill (10.5 MW), a 42-65 class gyratory (450 kW,
  2,354-3,362 t/h).
- **Why:** the smallest real gyratory crushes over 2,000 t/h, and real primaries run at 50-65 % of the time; 1,000 t/h is a mid-size
  copper concentrator, a block every 9.7 s, a 360 t truck every 22 minutes. It lands inside open-pit.md's 10-20 MW.
- **Alternatives:** 500 t/h (a 28 ft SAG; the gyratory is then four times too big, and a large jaw would be real instead); 2,000 t/h
  (a 38 ft SAG at 16 MW; 40 MW in all).

### P7-3. No gravity stage on the pit line
- **Chosen:** the pit line floats what floats and sends the rest to tails; no spirals.
- **Why:** spirals at 1,000 t/h are 20 banks; real copper concentrators do not recover zircon or ilmenite.
- **Alternatives:** a large spiral plant spec (real in mineral sands, not in copper plants).

### P7-4. Where the pit line's tails go
- **Chosen:** a 45 m high-rate thickener, then Wildcatter's lined tailings cell; no dry stacking at this rate.
- **Why:** 17 of P4's filters and a truck fleet for the cake; real plants this size mostly use thickened tails in a storage facility.
- **Alternatives:** dry stack anyway; paste fill (L5) takes part.

### P7-5. Power
- **Chosen:** about 20 MW for P7a-b plus about 3 MW for P7c, as Wildcatter loads; twin-pinion mill drives with motors at the world's
  standard medium voltage; mills must start through a soft starter or VFD.
- **Why:** dual-pinion drives are real to about 9-12 MW; Wildcatter has soft starters and VFDs. Real mills this size usually take 11-13.8
  kV, which Wildcatter has as its generator tier.
- **Alternatives:** mill motors on Wildcatter's generator-voltage tier (more real, fewer amps: about 360 A instead of 1,200 for the SAG);
  a ring-motor gearless drive (real above 18 MW).

### P7-6. A large-bore slurry pipe
- **Chosen:** flagged for Wildcatter: the cyclone feed (about 1 m³/s) and overflow (0.62 m³/s) are far beyond a small steel pipe.
  Proposed: a Wildcatter large-bore (about 500-600 mm) slurry line with its own rating.
- **Why:** the plant's pipe is sized for 50 t/h.
- **Alternatives:** several pipes in parallel (tiling); let the existing pipe carry any flow (unreal).

### P7-7. The stockpile is real blocks
- **Chosen:** the head drops `stockpile_rock` (a muck variant holding crushed rock) by the settle rule; feeders draw the block above
  their opening; live and dead fall out of the rule.
- **Why:** it is the most visible thing in a real concentrator and the settle rule already does it; 25-35 % live comes from the rule, not a
  number.
- **Alternatives:** a stockpile part with a capacity (simpler, invisible).

### P7-8. Boulders in the gyratory
- **Chosen:** 1 bank block in 200 of hard rock bridges the bowl; a pedestal rock breaker clears it in 30 s.
- **Why:** real primaries jam a few times a shift; the rock breaker is a real and visible part.
- **Alternatives:** no oversize (as the rail grizzly today).

### P7-9. A new heavy belt
- **Chosen:** a separate `heavy_belt_conveyor` (1,400 mm, 4 m/s, 3,000 t/h) with its own drive rule (75 kW a 30 m plus lift).
- **Why:** real plants use different belts; one class at 3,000 t/h would let the 600 mm belt carry the pit.
- **Alternatives:** a belt width setting on the existing belt.

### P7-10. P7a before the SAG line
- **Chosen:** P7a's reclaim belt may feed the existing 50 t/h crushing circuit (the weigher's setpoint at 50 t/h) until P7b.
- **Why:** the stockpile and gyratory are useful at once.
- **Alternatives:** P7a waits for P7b.

## Later (L waves)

### L-0. The order of the Later waves
- **Chosen:** L1 ventilation, L2 battery locomotives, L3 bottom-dump cars, L4 shaft and hoist, L5 paste backfill, L6 dry mill, L7 dense-medium
  separation, L8 XRT sorter. All after P7 unless Tim pulls one forward.
- **Why:** ventilation changes the TBM players already use; L2-L3 are small; the shaft wants the bottom-dump cars; paste wants a shaft;
  the separation machines matter most once the plant is big.
- **Alternatives:** the dry mill first (it is self-contained and pays a real rung).

### L1-1. What ventilation does in play
- **Chosen:** the TBM stops NO VENTILATION without duct air within 30 m behind its back-up; a player in stale air gets Mining Fatigue I
  after 30 s and Weakness I after 2 min; RADON is read in tuff and sandstone drives; air need 0.3 m/s across the bore (10.8 m³/s).
- **Why:** the drive is electric, so the diesel rule does not apply; people, heat, dust and rock gases are what real drives ventilate
  for; the TBM gas interlock is real.
- **Alternatives:** readouts only (no effect on play); damage in stale air (harsh).

### L2-1. The battery locomotive
- **Chosen:** the 8 t class, 2 x 22 kW, 58 kWh lead-acid (46 usable), a 20 kW charger on the world's three-phase 30 A receptacle,
  battery swapping.
- **Why:** real class figures; Wildcatter already has the receptacles; swapping is the real practice.
- **Alternatives:** lithium (more energy, less real for the class); charging only in the locomotive.

### L3-1. Bottom-dump cars
- **Chosen:** the same size as the Granby car (1.5 m³, 9 stacks), dumping over a 4 m track grizzly beside a cam rail; still no
  oversize.
- **Why:** mixed trains; TBM muck has no real oversize.
- **Alternatives:** bigger cars; oversize with a rock breaker.

### L4-1. How the shaft is sunk
- **Chosen:** raise boring: a raise borer on the surface drills a pilot to an existing drive and reams up to 5 m; cuttings fall into the
  drive.
- **Why:** real, needs no underground blasting, and the TBM's drive is already there.
- **Alternatives:** drill-and-blast sinking from a stage (real, but underground blasting is out); a shaft boring machine (real, a big
  new machine).

### L4-2. The hoist's size
- **Chosen:** 3 t skips in balance, 10 m/s, a 450 kW double-drum hoist: about 225 t/h from 150 m; a cage on the second drum when set.
- **Why:** small-mine class; a fifth of it serves the 50 t/h plant.
- **Alternatives:** a Koepe hoist; bigger skips for the pit era.

### L5-1. What paste fill fills
- **Chosen:** any enclosed underground void closed with `fill_bulkhead` blocks, up to 20,000 cells; NOT ENCLOSED otherwise; a broken
  bulkhead lets wet paste out.
- **Why:** Tailings has no stopes; worked-out drives and caves are its old workings.
- **Alternatives:** fill only drives the TBM bored (knows its own volume).

### L5-2. Cement
- **Chosen:** a `cement` item, a placeholder recipe from quicklime and clay, until a real cement kiln exists (in Wildcatter or Tailings).
- **Why:** neither mod has cement; paste needs 5 %.
- **Alternatives:** vanilla concrete powder as the binder (it is cement and aggregate); a cement kiln spec first.

### L5-3. Curing time
- **Chosen:** one game day (20 minutes), scaled from 28 days.
- **Alternatives:** 3 game days; instant.

### L6-1. What the dry mill adds to the ladder
- **Chosen:** a scavenger spiral bank (another 45 %) and the dry mill take the heavy minerals (ilmenite, rutile, zircon, monazite,
  cassiterite) to 95 % (4x) by their real route; the leach's exception stays for a plant without it.
- **Why:** it replaces a written game choice with the real practice for those minerals.
- **Alternatives:** the dry mill only splits a mixed concentrate (no rung, little reason to build it); remove the leach exception
  outright (breaks built plants).

### L6-2. The scavenger spirals without a dryer
- **Chosen:** the filter keeps their share as a `heavy_mineral_concentrate` item (assay kept) until it is dried and separated.
- **Why:** the 4x is earned at the dry mill, not the filter.
- **Alternatives:** pay it at the filter at 2x.

### L6-3. Zircon to Wildcatter
- **Chosen:** propose a Wildcatter compat entry: its chlorinator accepts Tailings' `zircon_concentrate` as zircon by kg.
- **Alternatives:** convert by crafting; leave the two chains apart.

### L7-1. What DMS rejects
- **Chosen:** floats (pieces lighter than the medium, default SG 2.75) leave as vanilla gravel and carry their trace minerals away (no
  nuggets); sinks go on; 2 % misplacement each way.
- **Why:** real pre-concentration trades recovery for mill capacity; floats are sold as aggregate.
- **Alternatives:** floats keep paying nuggets (no trade, no reason not to build it).

### L7-2. Ferrosilicon
- **Chosen:** a `ferrosilicon` item, a placeholder recipe until Wildcatter's ferroalloy plant exists; lost at 0.3 kg/t.
- **Alternatives:** magnetite medium from the magnetic separator (real for coal, too light for SG 2.75 media in practice).

### L7-3. Where DMS goes
- **Chosen:** on the double-deck screen's gravel (6-40 mm) face, which this wave builds; sand bypasses.
- **Alternatives:** a DMS drum on the crushed class (coarser, a different machine).

### L8-1. The dump sorter and the XRT sorter
- **Chosen:** the dump sorter stays as it is (by type, a stand-in); the XRT sorter is a separate two-way machine (product or waste by a
  threshold), 250 t/h, with 3 % and 5 % errors, and needs its compressor.
- **Why:** the dump sorter is built and used; the real XRT splits two ways, not N.
- **Alternatives:** replace the dump sorter with XRT (players lose type sorting).

## Still open from ROADMAP

### X-1. Do Fortune and Silk Touch stay?
- **Chosen:** **both stay, untouched** (Tailings does not change drops, Decision 1). **Raw ore items stop being plant feed**: the plant
  takes the ore **block** (from Silk Touch, the TBM or a blast), and refuses raw iron, raw copper and raw gold: "already ore: smelt it".
- **Why:** today `Feeds` counts one raw iron as a whole iron-ore block, so Fortune III (about 2.2 raw a block) pays about 8.8 ingots a
  block through the plant, and plain copper (2-5 raw a block, 3.5 on average) about 14: more than the block holds, against VISION's "no
  ore multiplication". A raw item is the 25 % already won (its real counterpart is hand-cobbed lump ore sent straight to the smelter, not
  the mill). With blocks only: Fortune is the plain route's bonus, Silk Touch is the hand miner's way into the plant, and machine mining
  (TBM, blast) is the plant's real feed. Nothing pays over the block.
- **Alternatives:** (a) count a raw item as a quarter of a block (it then pays 1 ingot through the plant: no gain, but no refusal); (b)
  disable Fortune on ores (changes vanilla, against Decision 1); (c) leave it (Fortune and copper multiply the ladder).

### X-2. Which extra vanilla ores join the ladder (redstone, lapis)?
- **Chosen:** **neither.** Recommended instead, later and only if Tim wants more: **diamond**, by its real route (DMS on the gravel face,
  then X-ray luminescence sorting), after L7-L8.
- **Why:** redstone has no real mineral (it would need an invented recovery route); lapis lazuli is a gem rock that real mines quarry
  and hand-sort, not mill. Diamond recovery is a real milling route (crush, DMS, X-ray), but diamonds are vanilla's scarcest valuable, so
  it is Tim's call. Coal stays out: a washery cleans coal but does not multiply it, and the ladder does not fit.
- **Alternatives:** redstone with a written game route (on the cinnabar model, a sulfide that floats); lapis by gravity (lazurite is light,
  it would not concentrate).

### X-3. Copper's ladder against vanilla's drop
- **Chosen:** flagged with X-1: plain mining of vanilla copper gives 2-5 raw (3.5 average), so the 1x is 3.5 ingots, not 1 (Decision 3
  assumed one). Recommend copper's block amount become **14 ingots** (3.5 x 4), paid through copper nuggets, so the ladder is 3.5 / 7 /
  10.5 / 14.
- **Why:** with raw items refused (X-1), a copper-ore block through the plant otherwise pays no more than mining it plainly.
- **Alternatives:** keep 4 (the plant is pointless for vanilla copper ore, useful only for its nuggets); 8 (half-way).
