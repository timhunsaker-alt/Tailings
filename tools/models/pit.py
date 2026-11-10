"""The open pit's truck dump at true size (docs/multiblocks/open-pit.md, Truck dump), each part drawn facing north from its main cell:
- truck_dump_wall, 12 x 5 x 2: the concrete retaining wall, 3 m below the pad, with its 1.5 m bumper on the hopper's edge above it (two rows of cells, so a truck that steps up a block cannot climb it);
- pit_hopper, 10 x 6 x 6: the 400 t steel-lined concrete hopper below the wall's lip, its sloped liner falling to the feeder end;
- fuel_bay, 4 x 3 x 2: the diesel island (P2a): a 2,000 L day tank on saddles, the dispenser with its hose, bollards and a canopy;
- haul_dispatch, 3 x 3 x 3: the dispatch office (P2b): a skid-mounted site office with windows on three sides, its door,
  an air conditioner, the radio mast on its roof, and the power and control cable entry (the electrical box) on the west face.
- tailings_thickener, 15 x 5 x 15 (P4): the 15 m high-rate thickener on the plant's tails: its tank, feedwell, bridge and 11 kW rake
  drive, the tails in at the north, the underflow out at the south and the overflow launder's water pipe on the east;
- tailings_filter, 6 x 6 x 24 (P4): a plate-and-frame press with 2.5 m plates on a steel floor 4 m up, its cake chute under the middle;
- loadout_frame, 12 x 10 x 12 (P4): the load-out's legs and bracing over a 10 m wide, 9 m clear lane, its deck, and the clamshell gate
  and chute hanging under the deck's centre (collision only on the legs, deck and chute, so a truck drives through);
- loadout_bin, 10 x 7 x 10 (P4): the 400 t steel bin on the frame, its sloped bottom to the gate.
"""
import tlib
from tlib import model, motor_box, stepped

# ---- the truck dump wall ---------------------------------------------------------------------------------------------------------
W = model("truck_dump_wall", 12, 5, 2, "concrete")
W.box((0.0, 0.0, 0.0), (12.0, 3.0, 2.0), "concrete")                       # the retaining wall, its top the pad's level
W.box((0.0, 3.0, 0.0), (12.0, 4.5, 1.0), "concrete")                       # the bumper a reversing truck's wheels stop against
W.box((0.0, 4.5, 0.0), (12.0, 4.55, 1.0), "hipaint")                       # its painted top
for x in range(0, 12, 2):
	W.box((x + 0.1, 3.3, 1.0), (x + 0.9, 4.2, 1.02), "hipaint")            # the bumper's hazard bands, on the truck's side
W.collide((0.0, 0.0, 0.0), (12.0, 3.0, 2.0))
W.collide((0.0, 3.0, 0.0), (12.0, 4.5, 1.0))

# ---- the pit hopper --------------------------------------------------------------------------------------------------------------
H = model("pit_hopper", 10, 6, 6, "steel")
H.box((0.0, 0.0, 0.0), (10.0, 0.4, 6.0), "concrete")                       # the floor
H.box((0.0, 0.4, 0.0), (0.4, 6.0, 6.0), "concrete")                        # the walls
H.box((9.6, 0.4, 0.0), (10.0, 6.0, 6.0), "concrete")
H.box((0.4, 1.4, 0.0), (9.6, 6.0, 0.4), "concrete")                        # the north wall, open below for the feeder
H.box((0.4, 0.4, 5.6), (9.6, 6.0, 6.0), "concrete")
stepped(H, 0.4, 9.6, 5.0, 0.6, 5.6, 0.4, 9, 0.2, "steel")                 # the wear liner falling to the feeder end
H.box((0.0, 5.8, 0.0), (10.0, 6.0, 0.4), "galv")                           # the steel curb round the top
H.box((0.0, 5.8, 5.6), (10.0, 6.0, 6.0), "galv")
H.box((0.0, 5.8, 0.4), (0.4, 6.0, 5.6), "galv")
H.box((9.6, 5.8, 0.4), (10.0, 6.0, 5.6), "galv")
H.collide((0.0, 0.0, 0.0), (10.0, 0.4, 6.0))
H.collide((0.0, 0.4, 0.0), (0.4, 6.0, 6.0))
H.collide((9.6, 0.4, 0.0), (10.0, 6.0, 6.0))
H.collide((0.4, 1.4, 0.0), (9.6, 6.0, 0.4))
H.collide((0.4, 0.4, 5.6), (9.6, 6.0, 6.0))

# ---- the fuel bay ----------------------------------------------------------------------------------------------------------------
F = model("fuel_bay", 4, 3, 2, "galv")
F.box((0.0, 0.0, 0.0), (4.0, 0.25, 2.0), "concrete")                       # the island
F.box((0.3, 0.25, 0.5), (0.5, 0.45, 1.5), "grey")                          # the tank's saddles
F.box((1.5, 0.25, 0.5), (1.7, 0.45, 1.5), "grey")
F.box((0.2, 0.45, 0.4), (1.8, 1.55, 1.6), "galv")                          # the 2,000 L day tank: 1.6 x 1.1 x 1.2 m
F.box((0.9, 1.55, 0.9), (1.1, 1.75, 1.1), "steel")                         # its vent and filler
F.box((1.8, 0.7, 0.95), (2.5, 0.8, 1.05), "steel")                         # the line to the dispenser
F.box((2.5, 0.25, 0.6), (3.3, 2.0, 1.4), "grey")                           # the dispenser
F.decal((2.6, 1.3, 1.401), (3.2, 1.8, 1.41), "gauge", "south", "grey")     # its meter
F.box((3.3, 1.2, 0.9), (3.45, 1.7, 1.1), "steel")                          # the nozzle on its holster
F.box((3.3, 1.0, 0.95), (3.4, 1.2, 1.05), "rubber")                        # and its hose
F.box((3.6, 0.25, 0.2), (3.8, 1.2, 0.4), "hipaint")                        # bollards
F.box((3.6, 0.25, 1.6), (3.8, 1.2, 1.8), "hipaint")
for x, z in ((0.05, 0.05), (3.8, 0.05), (0.05, 1.8), (3.8, 1.8)):
	F.box((x, 0.25, z), (x + 0.15, 2.85, z + 0.15), "galv")                # the canopy's posts
F.box((0.0, 2.85, 0.0), (4.0, 3.0, 2.0), "galv")                           # and its roof
F.collide((0.0, 0.0, 0.0), (4.0, 0.25, 2.0))
F.collide((0.2, 0.25, 0.4), (1.8, 1.55, 1.6))
F.collide((2.5, 0.25, 0.6), (3.3, 2.0, 1.4))
F.collide((0.0, 2.85, 0.0), (4.0, 3.0, 2.0))

# ---- the haul dispatch office ---------------------------------------------------------------------------------------------------
D = model("haul_dispatch", 3, 3, 3, "grey")
D.box((0.0, 0.0, 0.1), (3.0, 0.2, 0.3), "iron")                           # the skids
D.box((0.0, 0.0, 2.7), (3.0, 0.2, 2.9), "iron")
D.box((0.0, 0.2, 0.0), (3.0, 0.35, 3.0), "checker")                        # the floor
D.box((0.05, 0.35, 0.05), (2.95, 2.35, 2.95), "grey")                      # the walls
D.box((0.0, 2.35, 0.0), (3.0, 2.5, 3.0), "galv")                           # the roof
for x0, x1 in ((0.3, 1.3), (1.7, 2.7)):
	D.box((x0, 1.1, -0.01), (x1, 1.9, 0.05), "dark")                       # the windows over the pit, north
D.box((-0.01, 1.1, 1.4), (0.05, 1.9, 2.6), "dark")                         # west
D.box((2.95, 1.1, 0.4), (3.01, 1.9, 1.6), "dark")                          # east
D.box((1.8, 0.35, 2.95), (2.6, 2.1, 3.01), "blue")                         # the door, south
D.box((2.45, 1.15, 3.01), (2.52, 1.25, 3.05), "steel")                     # its handle
D.box((0.3, 0.6, 2.95), (1.1, 1.3, 3.0), "engine")                         # the air conditioner through the wall
D.box((0.35, 0.65, 3.0), (1.05, 1.25, 3.02), "louvre")
motor_box(D, y=0.6, z=0.2, h=0.7, w=0.6)                                   # the electrical box: low-tier power and the control cable
D.box((2.4, 2.5, 0.4), (2.6, 2.6, 0.6), "steel")                           # the radio mast's foot
D.box((2.47, 2.6, 0.47), (2.53, 3.0, 0.53), "galv")                        # its mast, to the top of its cells
D.decal((1.2, 1.95, -0.02), (1.8, 2.25, -0.01), "nameplate", "north", "grey")
D.collide((0.0, 0.0, 0.0), (3.0, 2.5, 3.0))

# ---- the tailings thickener (P4) -------------------------------------------------------------------------------------------------
from tlib import disc   # noqa: E402

TT = model("tailings_thickener", 15, 5, 15, "concrete")
disc(TT, 7.5, 7.5, 7.45, 0.0, 0.6, "concrete")               # the floor and its underflow cone
TT.ring("y", (7.5, 7.5), 7.45, 0.6, 4.4, 0.12, "concrete", sides=36)       # the 15 m tank wall
disc(TT, 7.5, 7.5, 7.3, 4.0, 4.05, "dark")                                  # the clear water over the bed
TT.ring("y", (7.5, 7.5), 7.2, 4.0, 4.3, 0.06, "galv", sides=36)            # the overflow launder round the rim
TT.cylinder("y", (7.5, 7.5), 1.0, 2.8, 4.4, "galv", 14)                     # the feedwell
TT.box((0.2, 4.4, 7.1), (14.8, 4.7, 7.9), "grating")                        # the bridge
TT.box((0.2, 4.7, 7.05), (14.8, 4.75, 7.1), "hipaint")                      # its handrails
TT.box((0.2, 4.7, 7.9), (14.8, 4.75, 7.95), "hipaint")
TT.box((6.9, 4.7, 6.9), (8.1, 5.0, 8.1), "blue")                            # the 11 kW rake drive
TT.pipe([(7.5, 1.0, 0.0), (7.5, 1.0, 0.4)], 8, "rubber")                    # tails slurry in, the north
TT.pipe([(7.5, 0.4, 14.6), (7.5, 0.4, 15.0)], 6, "rubber")                  # thickened tails out, the south
TT.pipe([(14.6, 3.8, 7.5), (15.0, 3.8, 7.5)], 8, "pipe_water")              # overflow water out, the east
motor_box(TT, 0.2, 0.3, 0.45, 0.4)
TT.collide((0.05, 0.0, 0.05), (14.95, 4.4, 14.95))

# ---- the tailings filter (P4) -----------------------------------------------------------------------------------------------------
TF = model("tailings_filter", 6, 6, 24, "grey")
for z in (0.3, 8.0, 16.0, 23.4):
	for x in (0.2, 5.5):
		TF.box((x, 0.0, z), (x + 0.3, 3.8, z + 0.3), "iron")                # the columns
TF.box((0.0, 3.8, 0.0), (6.0, 4.0, 24.0), "checker")                        # the steel floor
for x in (0.6, 5.1):
	TF.box((x, 4.0, 1.0), (x + 0.3, 5.9, 23.0), "iron")                     # the side beams the plates hang from
TF.box((0.9, 4.0, 1.0), (5.1, 5.9, 2.0), "blue")                            # the fixed head
TF.box((0.9, 4.0, 21.8), (5.1, 5.9, 22.6), "blue")                          # the moving tail
z = 2.1
while z < 21.6:
	TF.box((1.75, 4.1, z), (4.25, 5.8, z + 0.2), "polymer")                 # a 2.5 m filter plate
	z += 0.25
TF.cylinder("z", (3.0, 5.0), 0.25, 22.6, 23.8, "steel", 10)                 # the hydraulic closing ram
TF.box((1.0, 0.6, 9.0), (5.0, 3.8, 15.0), "galv")                           # the cake chute under the press
TF.box((1.6, 0.0, 9.6), (4.4, 0.6, 14.4), "galv")
TF.pipe([(3.0, 4.6, 0.0), (3.0, 4.6, 1.0)], 6, "rubber")                    # thickened tails in, the north
TF.pipe([(5.6, 4.3, 12.0), (6.0, 4.3, 12.0)], 4, "pipe_water")             # filtrate out, the east
motor_box(TF, 0.4, 0.4, 0.6, 0.5)
TF.collide((0.0, 3.8, 0.0), (6.0, 6.0, 24.0))
TF.collide((1.0, 0.0, 9.0), (5.0, 3.8, 15.0))
for z in (0.3, 8.0, 16.0, 23.4):
	for x in (0.2, 5.5):
		TF.collide((x, 0.0, z), (x + 0.3, 3.8, z + 0.3))

# ---- the truck load-out (P4) ------------------------------------------------------------------------------------------------------
LF = model("loadout_frame", 12, 10, 12, "steel")
for x in (0.0, 11.0):
	for z in (0.0, 5.5, 11.0):
		LF.box((x, 0.0, z), (x + 1.0, 9.0, z + 1.0), "steel")               # the legs, outside the 10 m lane
	LF.box((x + 0.3, 3.0, 1.0), (x + 0.7, 3.4, 11.0), "steel")              # the side bracing
	LF.box((x + 0.3, 6.5, 1.0), (x + 0.7, 6.9, 11.0), "steel")
	LF.box((x, 0.0, 0.0), (x + 1.0, 0.3, 12.0), "concrete")                 # the footings
LF.box((0.0, 9.0, 0.0), (12.0, 10.0, 12.0), "grating")                      # the deck the bin stands on
LF.box((0.0, 8.8, 0.0), (12.0, 9.0, 12.0), "hipaint")
LF.box((4.5, 7.6, 4.5), (7.5, 9.0, 7.5), "galv")                            # the chute under the bin's centre
LF.box((5.0, 7.0, 5.0), (7.0, 7.6, 7.0), "blue")                            # the clamshell gate
LF.box((4.6, 7.2, 5.6), (5.0, 8.4, 6.4), "steel")                           # its 7.5 kW hydraulic rams
LF.box((7.0, 7.2, 5.6), (7.4, 8.4, 6.4), "steel")
for z in (0.05, 11.85):
	LF.box((1.0, 8.5, z), (11.0, 8.8, z + 0.1), "hipaint")                  # the clearance bars: 9 m
motor_box(LF, 0.5, 0.2, 0.6, 0.5)
for x in (0.0, 11.0):
	for z in (0.0, 5.5, 11.0):
		LF.collide((x, 0.0, z), (x + 1.0, 9.0, z + 1.0))
LF.collide((0.0, 8.8, 0.0), (12.0, 10.0, 12.0))
LF.collide((4.5, 7.0, 4.5), (7.5, 8.8, 7.5))

LB = model("loadout_bin", 10, 7, 10, "steel")
LB.box((0.0, 2.0, 0.0), (10.0, 7.0, 0.3), "steel")                          # the walls
LB.box((0.0, 2.0, 9.7), (10.0, 7.0, 10.0), "steel")
LB.box((0.0, 2.0, 0.3), (0.3, 7.0, 9.7), "steel")
LB.box((9.7, 2.0, 0.3), (10.0, 7.0, 9.7), "steel")
for i in range(4):
	a = 0.3 + i * 1.0
	LB.box((a, 2.0 - i * 0.5, a), (10.0 - a, 2.5 - i * 0.5, 10.0 - a), "steel")   # the sloped bottom, stepping to the gate
for x in (0.0, 9.6):
	for z in (0.0, 9.6):
		LB.box((x, 0.0, z), (x + 0.4, 2.0, z + 0.4), "steel")               # its seats on the deck
LB.box((0.3, 6.0, 0.3), (9.7, 6.5, 9.7), "polymer")                         # the dry tails in it
LB.box((0.0, 6.9, 4.7), (10.0, 7.0, 5.3), "grating")                        # the walkway over it
LB.decal((4.0, 4.0, -0.01), (6.0, 5.0, 0.0), "nameplate", "north", "steel")
LB.collide((0.0, 0.0, 0.0), (10.0, 7.0, 10.0))

MODELS = [W, H, F, D, TT, TF, LF, LB]
