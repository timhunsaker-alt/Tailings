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
- From inside, the bore is an octagon of whole blocks (decision 11).
- The cutting is not drawn: the face's blocks vanish as the wheel reaches them (said in the narration).
- The section cut leaves a flat stone trench beside the ridge in the aerial.

## Needs Tim
- **A rider on the TBM's seat is drawn sunk into the cab's roof.** The server puts it at the roof (the scene's seat test: rider at y
  83.00, the roof at 83.00), but the client draws a villager about 1.6 m low, only the top of its head showing, whether riding or held
  there by the scene. A player riding was not checked. Two client runs did not find the cause (the renderer draws the cells at the
  entity's position with no offset; vanilla positionRider would subtract the rider's vehicle attachment, which is at the feet for a
  villager), so it is left for a proper look rather than guessed at. Episode 5 says so in the narration.
- **A launch frame at the portal** (optional): the first rings stand in the open at the portal, as a real machine's temporary launch
  rings do, but with nothing for them to push against. A reaction frame would be a new part.
- **Real-world power:** the game's 1 MW beam is about a third of a real 8 m main-beam drive (3,300 kW). A rule change, not a film fix.
- **The game bores close to a hundred times faster than a real machine** (3 m a minute against 40-60 m a day): a game-pace choice.
