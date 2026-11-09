"""The open pit's truck dump at true size (docs/multiblocks/open-pit.md, Truck dump), each part drawn facing north from its main cell:
- truck_dump_wall, 12 x 5 x 2: the concrete retaining wall, 3 m below the pad, with its 1.5 m bumper on the hopper's edge above it (two rows of cells, so a truck that steps up a block cannot climb it);
- pit_hopper, 10 x 6 x 6: the 400 t steel-lined concrete hopper below the wall's lip, its sloped liner falling to the feeder end.
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

MODELS = [W, H]
