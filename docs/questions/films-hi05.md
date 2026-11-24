# Films for Heavy Iron episode 5, "Tunnel boring machine" (branch `films-hi05`, 2026-11-23)

Decided while Tim was away, on the best recommendation; each is open for review. The series plan is
`videos/longform/heavy-iron/PLAN.md`; the episode is `videos/longform/heavy-iron/ep05/` (facts in its `ledger.md`).

## Decisions
1. **One site, a portal into a ridge.** The machine is built as blocks at a 12 m cut face of stone, heading north, its three gantries
   on the twin track behind and a muck train (a battery locomotive and two Granby cars) at the loading point. It bores in from there.
2. **Real power, the way the R10 playthrough found works:** two Wildcatter 1 MW gas sets, each through its own unit substation run
   backward, joined on the medium-voltage side and run to the cable junction. The cab is not forced (`Trailer.tbm` forces it; the scene
   turns that off). Gas is put in by the scene.
3. **Every film real time** (1 m in 20 s in stone). Between films the advance is run fast off camera (half a second a metre) and the
   muck it makes is taken off with it, so the drive is long enough for the back-up to be inside. Said in NOTES and the ledger; nothing
   narrated as time-lapse.
4. **The ventilation interlock is off** (`tailings rule ventilation_interlock false`): a short drive with no fan or duct; ventilation is
   episode 7. The narration says so, and that with it on the machine stops after a minute in poor air.
5. **The section:** after the inside shot, the ridge east of the bore is cut away, and the bore's east wall along the machine and the
   east half of the face ahead of it are cut as it goes, so the wheel, the erector, the track layer and the belt show. Labelled "Rock
   cut away to show the machine".
6. **Lit for the camera:** the inside shot of the finished drive is under night vision (the drive has no lights); labelled.
7. **Scale:** a villager (no AI) at the portal face beside the 8 m head. On the cab's seat a villager shows only the top of its head
   (Needs Tim, the seat); the film keeps it, held at the seat by the scene, and the narration says it is a fault.
8. **Real-world figures, each with a source (Tim's rule):** an 8.02 m main-beam machine's spec table (WTC 2023 paper, a water-diversion
   tunnel): 0 to 6.87 rpm, 3,300 kW main drive, 51 disc cutters; a maker's release on a 7.9 m main-beam machine: 40 to 60 m a day,
   a record month of 1,423.5 m for the class; a 7.8 m rock machine under the Palisades about 150 m (500 ft) long with its gantries;
   a reel maker's tunnelling page: reels up to 500 m. Left out: mass (the 1,680 "tons" is ambiguous), segments a ring (no source),
   motor counts, and **no thrust figure**. The PLAN's "8 m heads at 5-8 rpm (proposed)" and "reels 300-500 m" are replaced by these.
9. **The game's speed said plainly:** 3 m a minute against a real 40 to 60 m a day, so the game bores close to a hundred times faster.
10. **The muck train is placed by the scene** at the loading point after each run-on (a battery locomotive with no battery fitted cannot
    index itself up behind the advancing back-up); labelled "Train placed by the scene". Its run is episode 6.
11. **The round bore reads as an octagon from inside** (an 8 m circle in whole blocks, three cells cut off each corner). The narration
    says so ("its corners stepped in: a circle, drawn in blocks") rather than calling it round.
12. **Rewordings for the voice:** "built as blocks" (built at 0.3) became "set up as blocks"; "A real machine tows" (heard "reel ...
    toes") became "A full-size machine tows"; "Real tunnelling reels" (heard "reel") became "In the real world, tunnelling reels";
    "turns at up to" (heard "turns it up to") became "can turn at nearly seven"; "round the edge" (0.3) became "all around the edge";
    "a car takes the muck" (heard "kicks") became "a car has room again"; "and here a villager" became "Here, a villager"; "at one in
    two" (heard "one and two") became "that drops one metre in every two"; "a lined tunnel" (heard "line") became "a tunnel lined with
    segments". Spanish: "Necesita mucha energía" voiced twice in one take; "Gira a seis revoluciones" voiced "gira gira" twice, so it
    became "La rueda da seis vueltas por minuto"; "sondea" (heard "sondé a") became "revisa"; "en plano" (heard "emplama") became
    "horizontal" in P29; "un vagón tiene espacio otra vez" (heard "que en espacio") became "vuelve a tener espacio". No fact changed.
    Accepted after inspection: homophones and spellings with exact sense (cutter head, deep slate, backup, tows/toes, 8x8) and single
    numbers or function words under 0.4 with exact transcripts (one, seven, six, and; vienen, 12).
13. **Music:** episode 1's Heavy Iron bed again (no new credits).
14. **Spanish:** "La tuneladora", Brian Spanish (Venezuelan, usted); new words under Heavy Iron in `es_glossary.md` (tuneladora, cabeza
    de corte, cortadores de disco, cangilones, erector de dovelas, dovela, anillo, viga principal, colocadora de vía, pórtico, tren de
    apoyo, carrete de cable, caja de empalme, escombro for the TBM's muck, frente, portal, galería, pizarra profunda, enclavamiento de
    aire).
15. **The readout card as brief evidence** (the coordinator's review): every shot is cropped clear of the card; where a figure the card
    shows is said (the draw, Go, the advance, the rpm) the full frame plays first for 3-6 s; the paragraph about the card itself keeps
    it. The corner map at each chapter change is the series' duty-cycle panel, not a leftover.

## Fixed (Tim's rule of 2026-11-23: fix weak spots, then film)
- **The parked TBM was drawn as square blocks.** The cutterhead and segment erector models are now drawn round in their own block
  models (`tools/models/tbm.py`: an 8 m disc and tail-shield hoop in level 0.25 m strips; the corner cells are empty), so the machine
  reads round set down as blocks and running alike. The running renderer no longer skips the corner cells (it would notch the round
  model); its collision boxes follow the circle in 1 m steps. Regenerated `cutterhead/*` and `segment_erector/*`.
- **The wheel was drawn over a still model.** The head's model carried disc cutters and muck buckets that stood still behind the turning
  spokes, so up close the face read as two layers. The model is now a plain dark face plate and centre cone; the renderer draws the
  spokes, the disc cutters and now the eight muck buckets on the rim, all turning together at 6 rpm (`client/TbmRenderer.java`).
  Checked close up in `hi05_wheel`.
- **The PLAN's real-world figures had no source.** Each narrated one now has one (decision 8); the rest are left out. The renderer's
  "5 to 8 rpm (proposed)" comment and the doc's "real reels hold 300-500 m" now cite their sources, and the doc gains a sourced list.
- **No thrust figure** anywhere.
- **The TBM ran force-run in every earlier scene** (`Trailer.tbm` forces the cab): here it runs on two gas sets through the junction.

## Weak on camera, left as is
- From inside, the bore is an octagon of whole blocks (decision 11). *(2026-11-23: its shoulders now drawn rounded, "Fixed after release" 2.)*
- The cutting is not drawn: the face's blocks vanish as the wheel reaches them (said in the narration). *(2026-11-23: chips and dust now drawn, "Fixed after release" 3; the blocks still vanish.)*
- The section cut leaves a flat stone trench beside the ridge in the aerial.

## Needs Tim
- **(Resolved 2026-11-23, "Fixed after release" 1: not a render fault, the section cut hid it.)** **A rider on the TBM's seat is drawn sunk into the cab's roof.** The server puts it at the roof (the scene's seat test: rider at y
  83.00, the roof at 83.00), but the client draws a villager about 1.6 m low, only the top of its head showing, whether riding or held
  there by the scene. A player riding was not checked. Two client runs did not find the cause (the renderer draws the cells at the
  entity's position with no offset; vanilla positionRider would subtract the rider's vehicle attachment, which is at the feet for a
  villager), so it is left for a proper look rather than guessed at. Episode 5 says so in the narration.
- **A launch frame at the portal** (optional): the first rings stand in the open at the portal, as a real machine's temporary launch
  rings do, but with nothing for them to push against. A reaction frame would be a new part.
- **Real-world power:** the game's 1 MW beam is about a third of a real 8 m main-beam drive (3,300 kW). A rule change, not a film fix.
- **(Accepted by Tim 2026-11-23: keep the fast boring speed.)** **The game bores close to a hundred times faster than a real machine** (3 m a minute against 40-60 m a day): a game-pace choice.

## Tests
`build` passes. The full NeoForge suite on this branch (the round models and shapes in): all 306 required tests passed. The client
scenes (TAILINGS_ONLY=hi05) ran six times; films kept from runs 3, 5 and 6 (`videos/footage/films-hi05/NOTES.md`).


## Fixed after release (2026-11-23)
Branch `fix-weak-spots-hi05` (off main 5f225735). Tim: "Let's keep fixing weak spots as we find them." Decided on the best
recommendation while Tim was away; each is open for review. Re-filmed (fix runs 2-4; run 1 ran the probe by mistake) and episode 5
rebuilt in English and Spanish, the narration unchanged but for paragraph 28. Footage notes: `videos/footage/films-hi05/NOTES.md`.

1. **The villager on the cab's seat was never drawn low: the section cut hid it.** A probe (`HI05_PROBE=1`, stills in the client run's
   `hi05probe/`) read the rider on both sides: server and client alike put it at y 83.0, the cab's roof (`TbmEntity.positionRider`,
   SEAT_UP 5), and from inside the bore, before the section is cut, a riding villager and one stood on the beam both show whole on the
   roof. In the film the camera was outside the cut-away east wall at about the roof's height, and the cut (only the wall's own column,
   across 7) left the ring's east shoulder segment and the crown's edge (across 5-6, up 6-7) between it and the seat: they hid all of
   the villager above its feet, which read as "the top of its head" sunk into the roof. So no renderer or attachment change: for the cab
   film the scene now also cuts that shoulder and crown along the machine (`Hi05Scenes.shoulder`), the villager rides the seat (no
   longer held there by the scene), and the camera is nearer and aimed north of the seat, so the villager stands clear of the
   readout card (fix run 2's take had it beside the card and was deleted). Re-filmed `hi05_cab` (fix run 4).
   **Narration:** paragraph 28 re-voiced (Brian; Spanish Brian, Venezuelan delivery): "Here the scene has put a villager on that seat. It
   rides up on the roof, where a player would sit as the machine bores." / "Aquí la escena puso un aldeano en ese asiento. Va arriba, en
   el techo, donde se sentaría un jugador mientras la máquina perfora." Garble check: English exact; Spanish first take heard "techo"
   as "pecho", re-voiced once, then exact but for "aldiano" for "aldeano" (a spelling, accepted as before). The cab's label became
   "Villager put on the seat by the scene" ("Aldeano puesto en el asiento por la escena"); the descriptions no longer call it a fault.
   A player riding was not checked on camera (the same seat code).
2. **The bore's corners are drawn rounded.** The ring's two upper corner segments (the crown's shoulders, `DriveCourse.roundedCorner`)
   are now placed as `lining[corner=true,facing=<outer side>]`: a quarter-round fillet of the bore's inside, from the wall's top to the
   crown's edge, in 2 px strips (`models/block/lining_corner.json`; its shape in 4 px strips). From inside, the bore reads as a
   round-cornered tunnel instead of a stepped octagon. Chosen because it fits inside the lining's own cells: anything rounder needs
   lining in the clear 6 x 6 the gantries pass through. Still one segment each; a ring cut from rock stays square; the walls and the
   floor's corners are unchanged (a real tunnel's invert is flat too). `TunnelDrillTest` checks the two shoulders. Re-filmed `hi05_bore`.
   Paragraph 20 ("its corners stepped in: a circle, drawn in blocks") still holds and is unchanged.
3. **The cutting is drawn.** While it cuts, the client throws chips and dust of the rock just ahead (stone where the section is open)
   off the face in front of the wheel, and chips fall at the bottom of the wheel where the buckets scoop (`TbmEntity.faceDust`, nine a
   tick; drawn only, no rule). Re-filmed `hi05_face` and `hi05_wheel` (the cold open and paragraph 16 show it too). Paragraph 17 is
   unchanged: the game still takes the face block by block.
4. **No soft crops.** The films were kept at 1920 x 1080, so the episode's crops (most 0.5-0.56 of the frame, to keep the readout card
   out) were 1.8-2x enlargements. Every hi05 film is now re-filmed and kept at its full 2880 x 1620 (CRF 17), so the same crops are
   1.2-1.5x; the hardest ones are filmed nearer as well (`hi05_power`, `hi05_face`, `hi05_wheel`, `hi05_cab`), and their crops widened
   where the card allows. The scenes kill dropped items before each film (the section cut left some on the pad).
5. **Tests:** `build` passes; the full NeoForge suite on this branch: all 306 required tests passed. The client scenes ran four
   times (fix runs 1-4), every kept film real time.
6. **The probe switch** (`HI05_PROBE`) stays in `Hi05Scenes` for the record; an empty value is off (fix run 1 ran it by mistake).


## Second pass (2026-11-23)
Branch `fix2-hi05-06` (off main 105dee5e). Tim: "Let's keep fixing weak spots as we find them." Decided on the best recommendation
while Tim was away; each is open for review. Narration unchanged in English and Spanish. Footage notes: `videos/footage/films-hi05/NOTES.md`.

### Fixed
1. **The bore's lower corners were square.** Where the floor is flat the drill now sets a cove in the open cell at each wall's foot as
   it lays the floor behind the track layer (`DriveCourse.roundedFoot`, `TunnelDrill.line`): `lining[corner=true,foot=true,facing=<the
   wall>]`, the shoulder's quarter-round fillet turned upside down (`models/block/lining_foot.json`, its collision in 4 px strips), so
   the bore reads round-cornered at the floor as at the crown. Only beside a crafted segment (a rock-cut ring stays square, as at the
   crown), only into air, and not on the decline (the floor slopes under the wall there; see Still weak). It is the drive's finish from
   no stock: the ring stays 18 segments and the cove drops nothing (`LiningBlock.getDrops`). It stays under the gantries' deck line
   except against the wall, where their frame posts stand in its lowest half metre; the trailing cable now lies on whatever shape is
   under its point (`GantryRenderer.floorAt`), so it rides over the cove rather than a cell up. The machine's clearance check is ahead
   of the floor, so a cove never blocks it. The section films cut the east cove away with the east wall (`Hi05Scenes.cutSection`).
   Tests: `TunnelDrillTest` (two coves, flat only, not segments); NeoForge `tbm_bore_feet` (bores eleven metres: a cove at each wall's
   foot beside the rings, facing the wall, inside its cell, dropping nothing; the ring still 18; none outside the ridge).
   Paragraph 20 ("six blocks wide and six high, its corners stepped in: a circle, drawn in blocks") still holds and is unchanged.

2. **The villager stood near the frame's edge and the head was cut off.** The portal film now ends back from the face (from over the
   beam to 4 m west, 12 m back, 4.5 m up off the cutterhead's cell, `Hi05Scenes.portalFilm`), so the whole 8 m head and the villager
   at its foot are both in the frame, the villager well inside it. Paragraph 3 plays it from 44 s on the full frame: the card's
   "Cutterhead 8 m across" is the figure said, so it stays as evidence; the hero sits left of the villager.
3. **The bore film's locomotive filled a corner.** The camera now runs along the east track above the train's height and stops at
   the portal (`Hi05Scenes.boreFilm`); paragraphs 33-34 crop right of the locomotive and below the card.
4. **The ending showed empty grass.** Paragraphs 35-36 use the aerial while the machine shows in the cut (from 21 s and 33 s), not its
   later turn over the trench and grass, and the outro is the train at the loading point (`hi05_train` from 36 s).
5. **Every hi05 film re-filmed** in one run (fix2 run 1), all real time; the rest framed as before, the west cove in sight at the far
   wall's foot in the section films. Episode 5 rebuilt in English and Spanish with the narration unchanged.

### Accepted by Tim 2026-11-23
- **The fast boring speed** (3 m a minute against a real 40-60 m a day): kept as a game-pace choice.

### Still weak
- On the decline the lower corners stay square: the floor slopes under the wall there, and a cove for the slope pieces would need its
  own shapes. The episode's films are all on the flat.
- The coves are drawn in 2 px strips (the shoulders' look), so close up they read as fine steps rather than a smooth curve.
- The gantries' corner posts stand in the cove's lowest half metre where they pass along the wall (a few pixels from the wall; not
  noticeable in the films).
- The section cut still leaves a flat stone trench beside the ridge in the aerial; the episode now uses only the part with the machine.
- The game's 1 MW beam against a real 3,300 kW drive is unchanged (a rule, not a film fix).
