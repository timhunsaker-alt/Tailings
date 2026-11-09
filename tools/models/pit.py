"""The open pit's truck dump at true size (docs/multiblocks/open-pit.md, Truck dump), each part drawn facing north from its main cell:
- truck_dump_wall, 12 x 5 x 2: the concrete retaining wall, 3 m below the pad, with its 1.5 m bumper on the hopper's edge above it (two rows of cells, so a truck that steps up a block cannot climb it);
- pit_hopper, 10 x 6 x 6: the 400 t steel-lined concrete hopper below the wall's lip, its sloped liner falling to the feeder end;
- fuel_bay, 4 x 3 x 2: the diesel island (P2a): a 2,000 L day tank on saddles, the dispenser with its hose, bollards and a canopy;
- haul_dispatch, 3 x 3 x 3: the dispatch office (P2b): a skid-mounted site office with windows on three sides, its door,
  an air conditioner, the radio mast on its roof, and the power and control cable entry (the electrical box) on the west face.
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

MODELS = [W, H, F, D]
