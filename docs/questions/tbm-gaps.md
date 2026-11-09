# The TBM, its gantries and the mine trains — gaps closed while Tim was away (2026-11-09)

Tim (2026-11-09): "make decisions best recommendation and complete the rest of these two mods leaving questions for when I get back
... close all known gaps". These are the R5, R6, R9 and R10 lines of ROADMAP's Known gaps. Each entry is what was built, so it can be
changed when Tim goes through them (PROGRESS.md, "TBM gaps").

### 1. How the gantries ride the track
- **Chosen:** each gantry has wheels: a `TrainPath` along each of its two rails, laid from the rails as a train's path is
  (`entity.GantryWheels`). The machine tows it by saying how far along the bore its front is to stand; the rails set where it stands,
  its height and its pitch. A rail gone or changed under it, ahead of it or behind it takes it off the track, and the machine reads
  "NOT BUILT: needs the track under the Conveyor Gantry" and does not cut. Put the rail back and the gantry goes back on its wheels by
  itself, and the machine goes on. The old check of the course under each gantry is gone.
- **Why:** this is what a real back-up does: it is towed by the machine and runs on the track the machine lays. The gantry's place is
  still the machine's (it is coupled at a fixed distance), but where it stands now comes from the rails, not from the bore's course.
- **Alternatives:** let a derailed gantry drop off the rails and need re-railing by hand (a real derailment; harsher, and the spec says
  the machine "carries on once it is put right"); or let the gantries roll by their own drive and keep the distance with a coupling
  (they have no drive on a real machine).

### 2. Every car loads: the loading point the length of a train
- **Chosen:** the loading point reaches back 29 rows behind the conveyor's tail, a whole 10-car train at real length (it was 13).
  A train holds while a car in that reach has room and the machine has muck, so as it comes up it stops with its first car in reach,
  fills it, moves up, fills the next, and so on until its locomotive is at the gantry: it indexes forward a car at a time. The muck
  goes to the car with room nearest the conveyor.
- **Why:** a train standing behind the gantry can only move toward it, so the only way every car passes the loading point is for the
  point to be a train's length from the gantry. On a real machine the back-up is longer than the train and the train runs inside it
  under the conveyor, or a shuttle belt runs out over the standing train; the reach stands for that.
- **Alternatives:** let trains run under the gantries, inside the back-up, with the chute at the conveyor gantry's inby end (the
  real layout, but the gantries share the train's two tracks here, and the back-up would need to grow to a train's length); or draw
  a shuttle belt hung behind the conveyor gantry (not drawn now: a Known gap).

### 3. The trailing cable and the reel
- **Chosen:** the reel gantry draws its cable off the reel, down behind the gantry, then a segment a metre along the floor beside the
  left rail back to the cable junction (up to 160 m drawn), lying on whatever floor is there, so it climbs the decline. Two bars on
  each of the reel's flanges turn as the cable pays out, at the cable's 0.95 m radius on the drum. The cable is drawn black.
- **Why:** the simple segmented render the brief asked for; it shows the cable and the reel at work without new models.
- **Alternatives:** hang the cable on the tunnel wall on brackets (real practice, but needs a wall bracket block); a model for the
  reel that turns whole (needs the reel split out of the gantry's model).

### 4. The reel gantry's transformer as a circuit part
- **Chosen:** the transformer is built into the cable junction's circuit: three windings (Wildcatter's `Circuit.transformer`, wye to
  wye, both neutrals grounded) from the medium-voltage side to the industrial-voltage side, ratio from the world's standard, the
  150 kVA nameplate at 4 % impedance (proposed, typical of the size, X/R from Wildcatter's `Tuning`); the pump and conveyor a
  constant-power star on its secondary at power factor 0.85 (proposed, induction motors started across the line). Their current
  flows through it and shows on the junction's side; the junction's readout gives the secondary's volts, amps and kW. The conveyor
  does not move muck while the secondary is below 80 % of its volts.
- **Why:** a circuit part is a block and the gantry moves, so the part is at the cable's fixed end; electrically the same, as the
  trailing cable is not modelled either. Only Wildcatter 0.5.1 API is used.
- **Alternatives:** a transformer block the player sets at the junction (more to build, nothing more learnt); or model the trailing
  cable's impedance, 300 m of 4.16 kV cable, between the junction and the machine (a few volts' drop; later).

### 5. The quench tank on the pump gantry
- **Chosen:** the water is the pump gantry's, saved on it. The cab takes it each tick for its work and hands it back. A bucket in the
  cab's water slot goes to the gantry's tank; with no pump gantry there is no tank, and the slot keeps its bucket. Breaking the pump
  gantry loses its water (a tank of water cannot drop as an item). A world saved with water in the cab moves it to the pump gantry the
  first time the two are seen together.
- **Why:** the spec puts the 256 m3 tank on the pump gantry.
- **Alternatives:** drop the water as buckets when it breaks (256 items; odd).

### 6. The cab's screen across the lift
- **Chosen:** the screen follows the machine: lifting rebinds every screen open on the cab to the running machine, and setting down
  back to the cab (`DrillMenu.rebind`). The slots and readout are of whichever carries the cab now.
- **Alternatives:** none worth having.

### 7. Per-part boxes for the running machine: left, narrowed
- **Chosen:** not built. Still one box, its outline, raised at the rear on the grade.
- **Why:** the game collides an entity by one box, so per-part boxes need child entities: a new entity type on both loaders, five
  more entities per machine moved and sent every tick, and riders kept in step across them. And the bore is the machine's own 8 x 8
  section, so the outline differs from the parts only beside the main beam (rows 3 to 10) and in the corners on the grade, where a
  player cannot stand anyway while it drives. The cost is far above the gain now.
- **Plan if wanted:** a `TbmPartEntity` (no model, a box per part: cutterhead and erector 8 x 8 x 3, the beam 2 x 3, the cab, the
  track layer 6 x 1 x 3), spawned on lift, set each tick from the machine's place and pitch, not saved (made again on load), clicks
  and hits passed to the machine. About a day, with a client film to check riding.
- **Alternatives:** build it now (above).

### 8. The old `dewatering_pump`, `flotation_cell`, `dump_station` and `tailings_pond_placeholder`
- **Chosen:** kept. They were kept "for one release", and there has been no release since: the mod is still 0.1.0 and the CHANGELOG
  has only [Unreleased]. Remove them in the release after the first one that ships them as legacy.
- **Why:** an old world with them loses blocks if they go before players have had a release to move off them.
- **Alternatives:** remove now (nobody plays a release yet, so no world breaks; Tim may prefer the cleanup).

### 9. Real recipes for the machine and its gantries
- **Chosen:** steel and Wildcatter's parts, in the spirit of the spec:
  - cutterhead: steel, two stator windings (its drive motors) and a variable-frequency drive;
  - segment erector: steel, two pistons (its rams) and a winding (its pump's motor);
  - main beam: steel, two pistons (thrust cylinders), a glass pane and a small CPU module (the cab);
  - track layer: steel, mine rail and a hopper (the ballast);
  - cable junction: steel, two MV cables and a copper ingot;
  - pump gantry: steel, a process water pump, a cauldron (the tank) and two minecarts (its wheels);
  - reel gantry: steel, an MV cable (the reel), two windings (the transformer) and two minecarts;
  - conveyor gantry: steel, a belt conveyor, a winding (the drive) and two minecarts.
- **Why:** these are the real parts: disc cutters on a steel head turned by drive motors on VFDs, hydraulic rams, and wheeled steel
  gantries. There is no bearing item in Wildcatter 0.5.1, so none is used. The playthrough crafts through recipes, so its bill of
  materials changes (more steel, a drive cabinet, MV cable).
- **Alternatives:** more of each (a 1 MW machine is far dearer than a grid of nine allows); a parts item ("disc cutter") of Tailings'
  own.

### 10. Track too long to solve
- **Chosen:** the long drive was not warned: Wildcatter only logs it and stops simulating the circuit, so the track kept its last
  volts. Now a rail or rectifier in a circuit over Wildcatter's node limit reads too big: the track is dead (trains stop), the
  rectifier's readout and a stopped train's status say "TRACK TOO LONG ... give the far end its own rectifier", and players within
  64 m are told once in chat when it happens.
- **Alternatives:** split the track's circuit for the player (Wildcatter's solver groups by connection; not Tailings' to change).

### 11. Busway straight onto the rectifier
- **Chosen:** re-tested on Wildcatter 0.5.1 with a server scene (`rectifier_busway`): a 500 kW set with 480 V busway straight onto
  the rectifier's motor box. See PROGRESS for the result.
