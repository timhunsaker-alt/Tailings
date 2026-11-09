"""The open pit's truck dump at true size (docs/multiblocks/open-pit.md, Truck dump), each part drawn facing north from its main cell:
- truck_dump_wall, 12 x 5 x 2: the concrete retaining wall, 3 m below the pad, with its 1.5 m bumper on the hopper's edge above it (two rows of cells, so a truck that steps up a block cannot climb it);
- pit_hopper, 10 x 6 x 6: the 400 t steel-lined concrete hopper below the wall's lip, its sloped liner falling to the feeder end;
- fuel_bay, 4 x 3 x 2: the diesel island (P2a): a 2,000 L day tank on saddles, the dispenser with its hose, bollards and a canopy.
"""
import tlib
from tlib import model, stepped

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

MODELS = [W, H, F]
