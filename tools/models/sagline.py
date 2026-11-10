"""The pit line's grinding at true size (docs/multiblocks/sag-mill-line.md, P7b), each part drawn facing north from its main cell
(x east, y up, z south):
- sag_mill, 11 x 11 x 14: the 9.75 m (32 ft) SAG mill on its trunnions, its 4.6 m shell and conical heads, the girth gear and the two
  4.1 MW pinion motors on the west side (their terminal boxes on the west face at z 0 and z 4), the feed chute over the north end and
  the trommel turning on the discharge (south) end;
- pebble_crusher, 4 x 5 x 4: a cone crusher on its base, its feed hopper over the top and its 300 kW motor;
- ball_mill_large, 9 x 9 x 16: the 7.3 m (24 ft) overflow ball mill, 11 m long, on its trunnions, the girth gear and two 5.25 MW
  pinion motors on the west side, the underflow launder in at the north end and the discharge out under the south end;
- cyclone_feed_pump, 4 x 4 x 5: the pump box (sump) at the north end, the 600 kW slurry pump and its motor, the discharge going out of
  the south face;
- cyclone_cluster_large, 6 x 6 x 6: twelve 660 mm cyclones round a ring manifold on a steel stand, the overflow launder round their
  tops and the underflow launder under them.
"""
import tlib
from tlib import model, motor_box, disc


def octo(M, axis, c1, c2, r, start, end, tex):
	"""A round body too wide for one turned slab, along z (or x): an octagon of three boxes."""
	a, b = r * 0.92, r * 0.38
	if axis == "x":
		y, z = c1, c2
		M.box((start, y - b, z - a), (end, y + b, z + a), tex)
		M.box((start, y - a, z - b), (end, y + a, z + b), tex)
		M.box((start, y - r * 0.65, z - r * 0.65), (end, y + r * 0.65, z + r * 0.65), tex)
	else:
		x, y = c1, c2
		M.box((x - b, y - a, start), (x + b, y + a, end), tex)
		M.box((x - a, y - b, start), (x + a, y + b, end), tex)
		M.box((x - r * 0.65, y - r * 0.65, start), (x + r * 0.65, y + r * 0.65, end), tex)


def mill(M, cx, cy, r, z0, z1, shell, cone_r, cone_len, w):
	"""A mill shell along z: the octagon body, its bolted flanges, and the conical heads stepping down to the trunnions."""
	octo(M, "z", cx, cy, r, z0, z1, shell)
	for z in (z0, z1 - 0.15):
		octo(M, "z", cx, cy, r + 0.08, z, z + 0.15, "iron")                  # the head flanges
	for k in range(3):                                                      # the heads, stepping down
		rr = r - (r - cone_r) * (k + 1) / 3
		octo(M, "z", cx, cy, rr, z0 - cone_len * (k + 1) / 3, z0 - cone_len * k / 3, shell)
		octo(M, "z", cx, cy, rr, z1 + cone_len * k / 3, z1 + cone_len * (k + 1) / 3, shell)


# ---- the SAG mill -----------------------------------------------------------------------------------------------------------------
SG = model("sag_mill", 11, 11, 14, "padgreen")
SG.box((0.0, 0.0, 0.0), (11.0, 0.6, 14.0), "concrete")                         # the foundation
SG.box((3.0, 0.6, 1.4), (8.0, 4.2, 3.0), "concrete")                           # the feed-end trunnion pier
SG.box((3.0, 0.6, 11.0), (8.0, 4.2, 12.4), "concrete")                         # the discharge-end pier
mill(SG, 5.5, 5.9, 4.85, 4.4, 9.0, "padgreen", 1.4, 1.4, 11)
octo(SG, "z", 5.5, 5.9, 1.2, 2.0, 3.0, "iron")                                 # the feed trunnion on its bearing
octo(SG, "z", 5.5, 5.9, 1.2, 10.4, 11.4, "iron")                               # the discharge trunnion
SG.box((4.6, 4.5, 2.1), (6.4, 5.0, 2.9), "steel")                              # the bearing caps
SG.box((4.6, 4.5, 10.5), (6.4, 5.0, 11.3), "steel")
SG.box((4.7, 7.3, 0.0), (6.3, 10.9, 2.4), "steel")                             # the feed chute, where the belt delivers
SG.box((4.9, 6.3, 1.4), (6.1, 7.3, 2.6), "rubber")                             # its spout into the trunnion
octo(SG, "z", 5.5, 5.9, 5.15, 8.3, 8.7, "iron")                                 # the girth gear
octo(SG, "z", 5.5, 5.9, 1.25, 11.4, 13.9, "galv")                               # the trommel, turning with the mill
for z in (11.8, 12.4, 13.0, 13.6):
	octo(SG, "z", 5.5, 5.9, 1.32, z, z + 0.1, "iron")                         # its screen panels' rings
SG.box((3.6, 0.6, 11.6), (7.4, 4.0, 14.0), "steel")                            # the undersize hopper under it, to the pump box
SG.box((8.6, 3.8, 12.2), (11.0, 4.4, 13.6), "steel")                           # the pebble chute out of the east face
for z in (0.4, 4.4):
	SG.box((0.2, 0.6, z), (2.4, 0.9, z + 2.4), "iron")                         # each pinion motor's base
	octo(SG, "z", 1.3, 1.9, 0.95, z + 0.2, z + 2.2, "blue")                     # a 4.1 MW motor
	SG.box((1.1, 1.6, z + 2.2), (1.5, 2.0, 8.5), "steel")                      # its shaft to the pinion
	SG.box((0.6, 1.0, 8.2), (2.2, 2.6, 8.8), "grey")                           # the pinion's guard at the girth gear
motor_box(SG, 0.9, 0.3, 0.7, 0.6)
SG.box((0.0, 0.9, 4.3), (0.3, 1.6, 4.9), "grey")                                # the second motor's terminal box
SG.decal((-0.01, 1.0, 4.4), (0.0, 1.5, 4.8), "danger", "west", "grey")
SG.collide((0.0, 0.0, 0.0), (11.0, 0.6, 14.0))
SG.collide((1.0, 0.6, 2.0), (10.0, 10.8, 12.0))
SG.collide((4.7, 7.3, 0.0), (6.3, 10.9, 2.0))

# ---- the pebble crusher -----------------------------------------------------------------------------------------------------------
PC = model("pebble_crusher", 4, 5, 4, "padgreen")
PC.box((0.0, 0.0, 0.0), (4.0, 0.8, 4.0), "concrete")
disc(PC, 2.0, 2.0, 1.6, 0.8, 2.4, "padgreen")                                    # the main frame
disc(PC, 2.0, 2.0, 1.75, 2.4, 2.7, "iron")                                       # the adjustment ring
disc(PC, 2.0, 2.0, 1.4, 2.7, 3.4, "padgreen")                                    # the bowl
disc(PC, 2.0, 2.0, 1.0, 3.4, 4.4, "steel")                                       # the feed hopper
disc(PC, 2.0, 2.0, 0.7, 4.4, 4.9, "galv")
PC.box((0.2, 0.8, 1.6), (0.9, 1.8, 2.4), "steel")                                # the countershaft box
PC.box((1.4, 0.0, 3.4), (2.6, 0.8, 4.0), "dark")                                 # the discharge, south
motor_box(PC, 0.9, 0.2, 0.5, 0.5)
PC.collide((0.2, 0.0, 0.2), (3.8, 4.9, 3.8))

# ---- the large ball mill ----------------------------------------------------------------------------------------------------------
BL = model("ball_mill_large", 9, 9, 16, "padgreen")
BL.box((0.0, 0.0, 0.0), (9.0, 0.6, 16.0), "concrete")
BL.box((2.5, 0.6, 1.2), (6.5, 3.4, 2.6), "concrete")                             # the feed-end pier
BL.box((2.5, 0.6, 13.4), (6.5, 3.4, 14.8), "concrete")                           # the discharge-end pier
mill(BL, 4.5, 4.8, 3.65, 3.6, 12.4, "padgreen", 1.0, 1.0, 9)
octo(BL, "z", 4.5, 4.8, 0.9, 1.6, 2.6, "iron")                                   # the trunnions
octo(BL, "z", 4.5, 4.8, 0.9, 13.4, 14.6, "iron")
BL.box((3.6, 5.8, 0.0), (5.4, 6.4, 2.0), "steel")                                # the underflow launder in at the north end
BL.box((3.9, 5.0, 1.0), (5.1, 5.8, 2.0), "rubber")
octo(BL, "z", 4.5, 4.8, 3.95, 11.4, 11.8, "iron")                                 # the girth gear
BL.box((3.5, 0.6, 14.8), (5.5, 3.6, 16.0), "steel")                              # the discharge box under the south end, to the pipe
for z in (0.4, 4.4):
	BL.box((0.2, 0.6, z), (2.4, 0.9, z + 2.4), "iron")
	octo(BL, "z", 1.3, 1.9, 0.95, z + 0.2, z + 2.2, "blue")                       # a 5.25 MW motor
	BL.box((1.1, 1.6, z + 2.2), (1.5, 2.0, 11.6), "steel")
	BL.box((0.4, 1.0, 11.3), (1.6, 2.6, 11.9), "grey")
motor_box(BL, 0.9, 0.3, 0.7, 0.6)
BL.box((0.0, 0.9, 4.3), (0.3, 1.6, 4.9), "grey")
BL.decal((-0.01, 1.0, 4.4), (0.0, 1.5, 4.8), "danger", "west", "grey")
BL.collide((0.0, 0.0, 0.0), (9.0, 0.6, 16.0))
BL.collide((0.8, 0.6, 1.2), (8.2, 8.5, 15.0))

# ---- the cyclone feed pump --------------------------------------------------------------------------------------------------------
FP = model("cyclone_feed_pump", 4, 4, 5, "blue")
FP.box((0.0, 0.0, 0.0), (4.0, 0.3, 5.0), "concrete")
FP.box((0.3, 0.3, 0.0), (3.7, 3.6, 2.4), "galv")                                 # the pump box (sump), open on top
FP.box((0.4, 3.6, 0.1), (3.6, 3.7, 2.3), "grating")
octo(FP, "x", 1.3, 3.2, 0.9, 1.2, 2.6, "blue")                                    # the pump casing
FP.box((1.6, 0.3, 2.6), (2.4, 0.9, 4.6), "iron")                                 # the motor's base
octo(FP, "z", 2.0, 1.4, 0.55, 3.6, 4.8, "blue")                                   # the 600 kW motor
FP.box((1.85, 1.2, 3.0), (2.15, 1.6, 3.6), "steel")                              # the coupling
FP.box((1.65, 2.0, 2.95), (2.15, 3.65, 3.45), "rubber")                           # the 500 mm discharge, up...
octo(FP, "z", 1.9, 3.4, 0.27, 3.2, 5.0, "rubber")                                 # ...and out of the south face
motor_box(FP, 0.5, 0.2, 0.6, 0.5)
FP.collide((0.3, 0.0, 0.0), (3.7, 3.7, 4.8))

# ---- the large cyclone cluster ----------------------------------------------------------------------------------------------------
CC = model("cyclone_cluster_large", 6, 6, 6, "steel")
for x, z in ((0.2, 0.2), (5.4, 0.2), (0.2, 5.4), (5.4, 5.4)):
	CC.box((x, 0.0, z), (x + 0.4, 3.0, z + 0.4), "blue")                        # the stand's columns
CC.box((0.2, 2.9, 0.2), (5.8, 3.1, 5.8), "grating")                              # the cyclone floor
disc(CC, 3.0, 3.0, 2.2, 0.6, 1.0, "galv")                                        # the underflow launder, to the ball mill
CC.box((2.6, 0.0, 4.6), (3.4, 0.6, 6.0), "galv")                                 # and its chute out of the south side
disc(CC, 3.0, 3.0, 0.75, 3.1, 4.6, "steel")                                      # the ring manifold's distributor
import math
for k in range(12):
	a = 2 * math.pi * k / 12
	x, z = 3.0 + 2.0 * math.cos(a), 3.0 + 2.0 * math.sin(a)
	CC.cylinder("y", (x, z), 0.33, 3.9, 5.2, "rubber", 10)                      # a 660 mm cyclone's cylinder
	CC.cylinder("y", (x, z), 0.22, 2.6, 3.9, "rubber", 10)                      # its cone
	CC.cylinder("y", (x, z), 0.08, 1.0, 2.6, "rubber", 8)                       # its spigot to the underflow launder
	CC.box((min(x, 3.0) - 0.05, 4.5, min(z, 3.0) - 0.05), (max(x, 3.0) + 0.05, 4.6, max(z, 3.0) + 0.05), "steel")
CC.ring("y", (3.0, 3.0), 2.6, 5.3, 5.8, 0.3, "galv", sides=16)                   # the overflow launder round their tops
octo(CC, "z", 3.0, 4.0, 0.27, 0.0, 2.3, "rubber")                                  # the 500 mm feed in at the north face
octo(CC, "z", 3.0, 5.65, 0.27, 5.6, 6.0, "rubber")                                 # the overflow out at the south face
CC.handrail([(0.2, 0.2), (5.8, 0.2), (5.8, 5.8), (0.2, 5.8), (0.2, 0.2)], 3.1, "hipaint", height=1.0)
CC.collide((0.2, 0.0, 0.2), (5.8, 5.9, 5.8))

MODELS = [SG, PC, BL, FP, CC]
