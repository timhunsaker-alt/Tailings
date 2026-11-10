"""The shaft and hoist at true size (docs/multiblocks/shaft-and-hoist.md, L4), each part drawn facing north from its own main cell:
- headframe, 9 x 30 x 9: a steel four-post headframe over a 5 x 5 shaft (x and z 2 to 7 m): its legs and girts, the shaft collar and its
  gate rails, the skips' steel guides down both compartments (x 2-4 and 5-7, z 3-5), the dump chute out of the north side at the dump,
  the sheave deck at 25.5 m and the two 3 m head sheaves, their ropes leaving to the south where the hoist house stands;
- headframe_bin, 4 x 5 x 4: the 100 t steel bin on legs that the skips dump into, its hopper falling to the outlet under it;
- hoist_house, 10 x 8 x 12: the hoist house, its north wall open over the drums where the ropes leave, the two 3 m drums on one shaft,
  the 450 kW motor at their east end and its terminal box on the west face of the main cell;
- measuring_flask, 2 x 3 x 2: the loading pocket's measuring flask, its feed chute on top and its hydraulic gate under it;
- skip, 2 x 4 x 2: a 3 t bottom-dump skip, never placed: the skip entity draws it, its bail, rope socket and guide shoes.
"""
import tlib
from tlib import model, motor_box, stepped

# ---- the headframe ----------------------------------------------------------------------------------------------------------------
HF = model("headframe", 9, 30, 9, "galv")
LEGS = ((0.3, 0.3), (8.1, 0.3), (0.3, 8.1), (8.1, 8.1))
for x, z in LEGS:
	HF.box((x - 0.2, 0.0, z - 0.2), (x + 0.8, 0.5, z + 0.8), "concrete")   # the leg's footing
	HF.box((x, 0.5, z), (x + 0.6, 25.5, z + 0.6), "galv")                    # the four posts
for y in (5.0, 10.0, 15.0, 20.0):
	HF.box((0.9, y, 0.4), (8.1, y + 0.35, 0.8), "galv")                       # the girts between them, every 5 m
	HF.box((0.9, y, 8.2), (8.1, y + 0.35, 8.6), "galv")
	HF.box((0.4, y, 0.9), (0.8, y + 0.35, 8.1), "galv")
	HF.box((8.2, y, 0.9), (8.6, y + 0.35, 8.1), "galv")
# the collar round the shaft, and its gate rails so nobody walks in
HF.box((1.0, 0.0, 1.0), (8.0, 0.4, 2.0), "concrete")
HF.box((1.0, 0.0, 7.0), (8.0, 0.4, 8.0), "concrete")
HF.box((1.0, 0.0, 2.0), (2.0, 0.4, 7.0), "concrete")
HF.box((7.0, 0.0, 2.0), (8.0, 0.4, 7.0), "concrete")
for frm, to in (((1.8, 0.4, 1.8), (7.2, 1.5, 1.95)), ((1.8, 0.4, 7.05), (7.2, 1.5, 7.2)), ((1.8, 0.4, 1.95), (1.95, 1.5, 7.05)), ((7.05, 0.4, 1.95), (7.2, 1.5, 7.05))):
	HF.box((frm[0], 1.35, frm[2]), (to[0], 1.5, to[2]), "hipaint")            # the rail's top bar
	HF.box((frm[0], 0.8, frm[2]), (to[0], 0.9, to[2]), "hipaint")             # its mid bar
	HF.collide(frm, to)
for x, z in ((1.8, 1.8), (7.05, 1.8), (1.8, 7.05), (7.05, 7.05)):
	HF.box((x, 0.4, z), (x + 0.15, 1.5, z + 0.15), "hipaint")                 # its posts
# the steel guides each skip's shoes run on, up both compartments to the dump, and the buntons that carry them
for x in (2.0, 3.88, 5.0, 6.88):
	HF.box((x, 0.4, 3.9), (x + 0.12, 24.0, 4.1), "steel")
for y in range(3, 24, 5):
	HF.box((2.0, y, 2.0), (7.0, y + 0.2, 2.15), "iron")                        # the buntons along the shaft's north and south walls
	HF.box((2.0, y, 6.85), (7.0, y + 0.2, 7.0), "iron")
	for x in (2.0, 3.88, 5.0, 6.88):
		HF.box((x, y, 2.15), (x + 0.12, y + 0.2, 3.9), "iron")                # and the dividers out to the guides
		HF.box((x, y, 4.1), (x + 0.12, y + 0.2, 6.85), "iron")
# the dump: a scroll in each compartment tips the skip's door open, and the chute takes the rock out of the north side
for x in (2.0, 5.0):
	HF.box((x + 0.1, 17.4, 3.0), (x + 1.9, 17.6, 3.2), "blue")                # the scroll that catches the door's roller
stepped(HF, 2.0, 7.0, 17.2, 15.6, 3.0, 0.0, 4, 0.25, "steel")              # the dump chute, falling out of the north side
HF.box((2.0, 15.6, -0.0), (2.15, 17.6, 3.0), "steel")                       # its cheek plates
HF.box((6.85, 15.6, 0.0), (7.0, 17.6, 3.0), "steel")
# the sheave deck, open over the compartments where the ropes come up
for frm, to in (((0.0, 25.5, 0.0), (9.0, 25.7, 3.0)), ((0.0, 25.5, 5.0), (9.0, 25.7, 9.0)), ((0.0, 25.5, 3.0), (2.0, 25.7, 5.0)),
		((4.0, 25.5, 3.0), (5.0, 25.7, 5.0)), ((7.0, 25.5, 3.0), (9.0, 25.7, 5.0))):
	HF.box(frm, to, "grating")
	HF.collide(frm, to)
HF.handrail([(0.1, 0.1), (8.9, 0.1), (8.9, 8.9), (0.1, 8.9), (0.1, 0.1)], 25.7, "hipaint")
# the two 3 m head sheaves, the ropes coming up the shaft at z 4 and leaving over their tops to the south
for x in (3.0, 6.0):
	HF.box((x - 0.55, 25.7, 4.9), (x - 0.3, 27.3, 6.1), "iron")               # the bearing pedestals
	HF.box((x + 0.3, 25.7, 4.9), (x + 0.55, 27.3, 6.1), "iron")
	HF.cylinder("x", (27.5, 5.5), 0.12, x - 0.55, x + 0.55, "steel", sides=8)   # the axle
	HF.ring("x", (27.5, 5.5), 1.45, x - 0.12, x + 0.12, 0.12, "blue", sides=16)   # the rim, with the rope's groove
	HF.fan("x", (27.5, 5.5), 1.4, x, 0.08, "blue", blades=6, width=0.12, hub=0.2, hub_tex="iron")   # its spokes
HF.collide((0.3, 0.0, 0.3), (0.9, 25.5, 0.9))
HF.collide((8.1, 0.0, 0.3), (8.7, 25.5, 0.9))
HF.collide((0.3, 0.0, 8.1), (0.9, 25.5, 8.7))
HF.collide((8.1, 0.0, 8.1), (8.7, 25.5, 8.7))
HF.collide((1.0, 0.0, 1.0), (8.0, 0.4, 2.0))
HF.collide((1.0, 0.0, 7.0), (8.0, 0.4, 8.0))
HF.collide((1.0, 0.0, 2.0), (2.0, 0.4, 7.0))
HF.collide((7.0, 0.0, 2.0), (8.0, 0.4, 7.0))

# ---- the headframe bin ------------------------------------------------------------------------------------------------------------
HB = model("headframe_bin", 4, 5, 4, "galv")
for x, z in ((0.1, 0.1), (3.5, 0.1), (0.1, 3.5), (3.5, 3.5)):
	HB.box((x, 0.0, z), (x + 0.4, 2.2, z + 0.4), "galv")                     # its legs
HB.box((1.4, 0.0, 1.4), (2.6, 0.6, 2.6), "steel")                          # the outlet and its gate, over the belt under it
HB.box((1.2, 0.6, 1.2), (2.8, 1.2, 2.8), "steel")                          # the hopper, falling to the outlet
HB.box((0.7, 1.2, 0.7), (3.3, 1.8, 3.3), "steel")
HB.box((0.3, 1.8, 0.3), (3.7, 2.4, 3.7), "steel")
HB.box((0.1, 2.4, 0.1), (3.9, 4.8, 3.9), "galv")                           # the bin
HB.box((0.0, 4.8, 0.0), (4.0, 5.0, 4.0), "iron")                           # its rim
HB.decal((1.2, 3.2, -0.01), (2.8, 4.2, 0.0), "ratingplate", "north", "galv")
HB.collide((0.1, 0.0, 0.1), (3.9, 5.0, 3.9))

# ---- the hoist house --------------------------------------------------------------------------------------------------------------
HH = model("hoist_house", 10, 8, 12, "galv")
HH.box((0.0, 0.0, 0.0), (10.0, 0.4, 12.0), "concrete")                     # the floor slab
HH.box((0.0, 0.4, 0.0), (10.0, 2.2, 0.2), "galv")                          # the north wall: low, open over the drums for the ropes
HH.box((0.0, 6.4, 0.0), (10.0, 7.6, 0.2), "galv")
for x in (0.0, 4.9, 9.8):
	HH.box((x, 2.2, 0.0), (x + 0.2, 6.4, 0.2), "galv")                     # its posts
HH.box((0.0, 0.4, 11.8), (10.0, 7.6, 12.0), "galv")                        # the south wall
HH.box((0.0, 0.4, 0.2), (0.2, 7.6, 11.8), "galv")                          # the side walls
HH.box((9.8, 0.4, 0.2), (10.0, 7.6, 11.8), "galv")
HH.decal((-0.01, 3.5, 6.0), (0.0, 5.0, 9.0), "louvre", "west", "galv")
HH.decal((10.0, 3.5, 6.0), (10.01, 5.0, 9.0), "louvre", "east", "galv")
HH.box((-0.1, 7.6, -0.1), (10.1, 8.0, 12.1), "blue")                       # the roof
for x1, x2 in ((0.8, 4.3), (5.3, 8.8)):
	HH.ring("x", (3.4, 3.0), 1.4, x1, x2, 0.2, "iron", sides=16)            # the two 3 m drums, the ropes wound on them
	HH.ring("x", (3.4, 3.0), 1.7, x1 - 0.1, x1, 0.2, "blue", sides=16)     # their flanges
	HH.ring("x", (3.4, 3.0), 1.7, x2, x2 + 0.1, 0.2, "blue", sides=16)
HH.cylinder("x", (3.4, 3.0), 0.25, 0.4, 9.6, "steel", sides=8)             # the drum shaft
for x in (0.4, 4.6, 9.2):
	HH.box((x, 0.4, 2.5), (x + 0.4, 3.2, 3.5), "iron")                     # its bearing pedestals
HH.box((3.0, 0.4, 7.0), (7.0, 2.6, 10.0), "blue")                          # the 450 kW motor, behind the drums
HH.box((4.6, 1.2, 3.5), (5.4, 1.8, 7.0), "steel")                          # its gearbox shaft to the drums
HH.box((1.0, 0.4, 9.0), (2.2, 2.4, 11.4), "grey")                          # the hoist's control desk and its drive cabinets
HH.decal((1.2, 1.4, 8.99), (2.0, 2.2, 9.0), "controller", "north", "grey")
HH.box((7.6, 0.4, 10.6), (9.6, 2.6, 11.6), "grey")
HH.decal((7.8, 1.4, 10.59), (9.4, 2.4, 10.6), "gauge", "north", "grey")
motor_box(HH, y=1.0, z=0.4, h=0.8, w=0.7)
HH.collide((0.0, 0.0, 0.0), (10.0, 0.4, 12.0))
HH.collide((0.0, 0.4, 0.0), (10.0, 2.2, 0.2))
HH.collide((0.0, 0.4, 11.8), (10.0, 8.0, 12.0))
HH.collide((0.0, 0.4, 0.0), (0.2, 8.0, 12.0))
HH.collide((9.8, 0.4, 0.0), (10.0, 8.0, 12.0))
HH.collide((0.0, 7.6, 0.0), (10.0, 8.0, 12.0))

# ---- the measuring flask ----------------------------------------------------------------------------------------------------------
MF = model("measuring_flask", 2, 3, 2, "steel")
for x, z in ((0.1, 0.1), (1.7, 0.1), (0.1, 1.7), (1.7, 1.7)):
	MF.box((x, 0.0, z), (x + 0.2, 1.0, z + 0.2), "galv")                     # its frame
MF.box((0.7, 0.2, 0.7), (1.3, 0.8, 1.3), "steel")                          # the discharge and its gate
MF.box((0.3, 0.5, 0.75), (0.7, 0.65, 1.25), "blue")                        # the gate's hydraulic cylinder
MF.box((0.4, 0.8, 0.4), (1.6, 1.2, 1.6), "steel")                          # the flask's cone
MF.box((0.15, 1.2, 0.15), (1.85, 2.6, 1.85), "steel")                      # the flask: one skip's 3 t
MF.box((0.5, 2.6, 0.5), (1.5, 3.0, 1.5), "galv")                           # its feed chute from the feeder
MF.decal((0.5, 1.6, 0.14), (1.5, 2.3, 0.15), "gauge", "north", "steel")    # its load cell readout
MF.collide((0.1, 0.0, 0.1), (1.9, 3.0, 1.9))

# ---- the skip ---------------------------------------------------------------------------------------------------------------------
SK = model("skip", 2, 4, 2, "galv")
SK.box((0.25, 0.3, 0.25), (1.75, 3.3, 1.75), "galv")                       # the body
SK.box((0.2, 3.2, 0.2), (1.8, 3.35, 1.8), "iron")                          # its rim
SK.box((0.3, 0.0, 0.3), (1.7, 0.3, 1.7), "blue")                           # the bottom door
SK.box((0.0, 0.1, 0.85), (0.25, 0.25, 1.15), "iron")                       # the door's roller, that the scroll catches
for x in (0.05, 1.8):
	SK.box((x, 0.4, 0.8), (x + 0.15, 3.95, 1.2), "iron")                    # the bail's side straps
	SK.box((x - 0.05, 0.5, 0.85), (x + 0.2, 0.9, 1.15), "steel")            # the guide shoes on the guides
	SK.box((x - 0.05, 3.0, 0.85), (x + 0.2, 3.4, 1.15), "steel")
SK.box((0.05, 3.75, 0.8), (1.95, 3.95, 1.2), "iron")                       # the crosshead
SK.box((0.9, 3.95, 0.9), (1.1, 4.0, 1.1), "steel")                         # the rope socket
SK.collide((0.2, 0.0, 0.2), (1.8, 4.0, 1.8))

MODELS = [HF, HB, HH, MF, SK]
