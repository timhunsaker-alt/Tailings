"""The tunnel boring machine at true size (docs/multiblocks/tunnel-boring-machine.md), each part drawn facing north (the heading)
from its own main cell at the front-left-bottom corner:
- cutterhead, 8 x 8 x 1: the disc-cutter head in its shield, its cutters set in rings, the muck buckets round the rim, the centre cone;
- segment_erector, 8 x 8 x 2: the tail shield and the ring erector's arm holding a segment;
- main_beam, 2 x 2 x 8: the box beam with its thrust cylinders and grippers, the cutterhead drive at the front, the cab at its tail;
- track_layer, 6 x 1 x 3: the frame under the beam's rear that lays the floor and the twin track, its rail feed and ballast hopper.
"""
import math

import tlib
from tlib import model

# ---- the cutterhead ---------------------------------------------------------------------------------------------------------------
CH = model("cutterhead", 8, 8, 1, "steel")
CH.box((0.1, 0.1, 0.35), (7.9, 7.9, 1.0), "grey")                           # the shield and the head's back plate
CH.box((0.3, 0.3, 0.1), (7.7, 7.7, 0.35), "steel")                          # the face plate
for ring, count in ((1.0, 6), (2.0, 12), (3.1, 18)):
	for i in range(count):
		a = 2 * math.pi * i / count + ring
		x, y = 4.0 + ring * math.cos(a), 4.0 + ring * math.sin(a)
		CH.box((x - 0.14, y - 0.14, -0.05), (x + 0.14, y + 0.14, 0.1), "iron")   # a disc cutter in its housing
for i in range(8):
	a = 2 * math.pi * i / 8 + 0.2
	x, y = 4.0 + 3.6 * math.cos(a), 4.0 + 3.6 * math.sin(a)
	CH.box((x - 0.3, y - 0.3, 0.0), (x + 0.3, y + 0.3, 0.35), "dark")      # a muck bucket at the rim
CH.box((3.6, 3.6, -0.15), (4.4, 4.4, 0.1), "iron")                          # the centre cone
CH.collide((0.0, 0.0, 0.0), (8.0, 8.0, 1.0))

# ---- the segment erector ----------------------------------------------------------------------------------------------------------
SE = model("segment_erector", 8, 8, 2, "grey")
SE.box((0.0, 0.0, 0.0), (8.0, 0.4, 2.0), "grey")                            # the tail shield: floor, roof and walls
SE.box((0.0, 7.6, 0.0), (8.0, 8.0, 2.0), "grey")
SE.box((0.0, 0.4, 0.0), (0.4, 7.6, 2.0), "grey")
SE.box((7.6, 0.4, 0.0), (8.0, 7.6, 2.0), "grey")
for (x1, y1, x2, y2) in ((0.6, 0.6, 7.4, 1.0), (0.6, 7.0, 7.4, 7.4), (0.6, 1.0, 1.0, 7.0), (7.0, 1.0, 7.4, 7.0)):
	SE.box((x1, y1, 1.3), (x2, y2, 1.7), "galv")                            # the erector's slewing ring
SE.box((3.5, 3.5, 0.2), (4.5, 4.5, 1.8), "blue")                            # the erector's hub on the beam
SE.box((3.8, 4.5, 0.6), (4.2, 6.6, 1.0), "blue")                            # its arm
SE.box((2.6, 6.6, 0.5), (5.4, 7.0, 1.6), "concrete")                        # a lining segment on its grip
SE.collide((0.0, 0.0, 0.0), (8.0, 0.4, 2.0))
SE.collide((0.0, 7.6, 0.0), (8.0, 8.0, 2.0))
SE.collide((0.0, 0.4, 0.0), (0.4, 7.6, 2.0))
SE.collide((7.6, 0.4, 0.0), (8.0, 7.6, 2.0))
SE.collide((3.5, 3.5, 0.2), (4.5, 4.5, 1.8))

# ---- the main beam ----------------------------------------------------------------------------------------------------------------
MB = model("main_beam", 2, 2, 8, "blue")
MB.box((0.25, 0.25, 0.0), (1.75, 1.75, 6.0), "blue")                        # the box beam
MB.box((0.1, 0.1, 0.0), (1.9, 1.9, 1.2), "grey")                            # the cutterhead drive housing
for x in (0.15, 1.65):
	MB.box((x, 0.6, 0.3), (x + 0.2, 1.4, 0.9), "blue")                      # a drive motor
for (y, x) in ((0.35, 0.05), (0.35, 1.75), (1.45, 0.05), (1.45, 1.75)):
	MB.cylinder("z", (x + 0.1, y + 0.1), 0.1, 1.2, 4.2, "steel", 8)          # a thrust cylinder
MB.box((0.0, 0.5, 4.2), (0.25, 1.5, 5.6), "iron")                           # the grippers
MB.box((1.75, 0.5, 4.2), (2.0, 1.5, 5.6), "iron")
MB.box((0.1, 0.0, 6.0), (1.9, 2.0, 8.0), "grey")                            # the operator cab at the tail
MB.box((0.2, 1.1, 7.95), (1.8, 1.8, 8.0), "dark")                           # its rear window
MB.decal((0.4, 0.4, 8.0), (1.6, 0.9, 8.01), "controller", "south", "grey")
MB.decal((1.9, 0.9, 6.3), (1.91, 1.6, 7.6), "nameplate", "east", "grey")
MB.collide((0.1, 0.0, 0.0), (1.9, 2.0, 8.0))

# ---- the track layer --------------------------------------------------------------------------------------------------------------
TL = model("track_layer", 6, 1, 3, "galv")
TL.box((0.1, 0.55, 0.1), (5.9, 0.85, 2.9), "galv")                          # its frame
for x in (0.5, 5.2):
	TL.box((x, 0.85, 0.3), (x + 0.3, 1.0, 2.7), "galv")                     # the hangers to the beam
for x in (1.2, 4.2):
	TL.box((x, 0.15, 0.6), (x + 0.6, 0.55, 2.4), "iron")                    # a rail feed and its rollers
	TL.cylinder("x", (0.3, 2.6), 0.18, x, x + 0.6, "rubber", 8)              # its drive roller
TL.box((2.2, 0.2, 0.4), (3.8, 0.55, 1.6), "dark")                           # the ballast hopper
TL.box((2.4, 0.0, 1.6), (3.6, 0.2, 2.6), "steel")                           # the floor piece shoe
TL.collide((0.1, 0.15, 0.1), (5.9, 1.0, 2.9))

MODELS = [CH, SE, MB, TL]
