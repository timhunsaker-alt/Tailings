"""The tunnel boring machine at true size (docs/multiblocks/tunnel-boring-machine.md), each part drawn facing north (the heading)
from its own main cell at the front-left-bottom corner:
- cutterhead, 8 x 8 x 1: the disc-cutter head in its shield, its cutters set in rings, the muck buckets round the rim, the centre cone;
- segment_erector, 8 x 8 x 2: the tail shield and the ring erector's arm holding a segment;
- main_beam, 2 x 2 x 8: the box beam with its thrust cylinders and grippers, the cutterhead drive at the front, the cab at its tail;
- track_layer, 6 x 1 x 3: the frame under the beam's rear that lays the floor and the twin track, its rail feed and ballast hopper;
- pump_gantry, 6 x 4 x 3: the back-up frame on its wheels, carrying the dewatering pump and the quench tank;
- reel_gantry, 6 x 4 x 4: the frame carrying the trailing cable reel and the transformer;
- conveyor_gantry, 6 x 4 x 8: the frame carrying the muck conveyor, rising to its tail over the loading point.
"""
import math

import tlib
from tlib import model

# ---- round parts: an 8 m circle drawn in level strips, so the head and the tail shield read round as blocks too -----------------
CX, CY = 4.0, 4.0
STRIP = 0.25


def _half(r, y0, y1):
	"""Half the chord of a circle of radius r (about CY) across the strip y0..y1, at its middle."""
	d = (y0 + y1) / 2 - CY
	return math.sqrt(r * r - d * d) if abs(d) < r else 0.0


def round_disc(M, r, z0, z1, tex):
	"""A solid disc of radius r about (CX, CY), from z0 to z1 along the heading."""
	n = int(round(8 / STRIP))
	for i in range(n):
		y0, y1 = i * STRIP, (i + 1) * STRIP
		h = _half(r, y0, y1)
		if h > 0.02:
			M.box((CX - h, y0, z0), (CX + h, y1, z1), tex)


def round_ring(M, outer, inner, z0, z1, tex, collide=False):
	"""A hoop between two radii about (CX, CY), from z0 to z1: in each strip the whole chord, or its two ends where it crosses the hole."""
	n = int(round(8 / STRIP))
	for i in range(n):
		y0, y1 = i * STRIP, (i + 1) * STRIP
		ho, hi = _half(outer, y0, y1), _half(inner, y0, y1)
		if ho <= 0.02:
			continue
		parts = [(CX - ho, CX + ho)] if hi <= 0.02 else [(CX - ho, CX - hi), (CX + hi, CX + ho)]
		for a, b in parts:
			M.box((a, y0, z0), (b, y1, z1), tex)
			if collide:
				M.collide((a, y0, z0), (b, y1, z1))


# ---- the cutterhead ---------------------------------------------------------------------------------------------------------------
# the head in its round shield; its wheel of spokes, disc cutters and muck buckets is drawn turning on the face by TbmRenderer
CH = model("cutterhead", 8, 8, 1, "steel")
round_disc(CH, 3.95, 0.35, 1.0, "grey")                                      # the shield and the head's back plate
round_disc(CH, 3.75, 0.1, 0.35, "dark")                                      # the face plate, behind the turning wheel
CH.box((3.6, 3.6, -0.15), (4.4, 4.4, 0.1), "iron")                          # the centre cone
for i in range(8):
	y0, y1 = float(i), float(i + 1)
	h = math.sqrt(max(0.0, 3.95 ** 2 - min((y0 - CY) ** 2, (y1 - CY) ** 2))) if not (y0 <= CY <= y1) else 3.95
	CH.collide((max(0.0, CX - h), y0, 0.0), (min(8.0, CX + h), y1, 1.0))

# ---- the segment erector ----------------------------------------------------------------------------------------------------------
SE = model("segment_erector", 8, 8, 2, "grey")
round_ring(SE, 3.95, 3.55, 0.0, 2.0, "grey", collide=True)                  # the round tail shield
round_ring(SE, 3.35, 3.0, 1.3, 1.7, "galv")                                 # the erector's slewing ring
SE.box((3.5, 3.5, 0.2), (4.5, 4.5, 1.8), "blue")                            # the erector's hub on the beam
SE.box((3.8, 4.5, 0.6), (4.2, 6.6, 1.0), "blue")                            # its arm
SE.box((2.6, 6.6, 0.5), (5.4, 7.0, 1.6), "concrete")                        # a lining segment on its grip
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

# ---- the back-up gantries: frames on two pairs of wheels, straddling the twin track (columns 1 and 4 of the frame) -----------------
def gantry(name, length):
	G = model(name, 6, 4, length, "galv")
	for x in (1.5, 4.5):                                                     # a wheel set on each rail, front and rear
		for z in (0.5, length - 0.5):
			G.cylinder("x", (0.3, z), 0.28, x - 0.2, x + 0.2, "dark", 10)
		G.box((x - 0.12, 0.3, 0.3), (x + 0.12, 0.62, length - 0.3), "iron")  # the truck over them
	G.box((0.1, 0.62, 0.05), (5.9, 0.82, length - 0.05), "galv")             # the deck
	for x in (0.1, 5.65):
		for z in (0.05, length - 0.3):
			G.box((x, 0.82, z), (x + 0.25, 3.8, z + 0.25), "galv")           # a post
		G.box((x, 3.55, 0.05), (x + 0.25, 3.8, length - 0.05), "galv")       # a top rail
	G.box((0.1, 3.55, 0.05), (5.9, 3.8, 0.3), "galv")                        # the frame's ends
	G.box((0.1, 3.55, length - 0.3), (5.9, 3.8, length - 0.05), "galv")
	G.collide((0.0, 0.0, 0.0), (6.0, 3.8, float(length)))
	return G


PG = gantry("pump_gantry", 3)
PG.cylinder("z", (2.5, 2.5), 1.15, 0.4, 2.6, "blue", 16)                     # the quench tank
for z in (0.7, 2.3):
	PG.box((1.25, 0.82, z - 0.1), (3.75, 1.45, z + 0.1), "steel")            # its saddle
PG.cylinder("y", (4.6, 1.0), 0.4, 0.82, 2.0, "grey", 12)                    # the pump
PG.cylinder("y", (4.6, 1.0), 0.3, 2.0, 2.6, "blue", 12)                      # its motor
PG.cylinder("z", (4.6, 2.2), 0.12, 0.2, 2.8, "steel", 8)                     # the drain main
PG.cylinder("x", (2.2, 1.6), 0.1, 3.6, 4.6, "steel", 8)                      # from the tank to the main
PG.decal((0.36, 1.2, 0.3), (0.37, 1.9, 1.5), "nameplate", "west", "grey")

RG = gantry("reel_gantry", 4)
RG.cylinder("x", (2.5, 1.5), 1.2, 0.5, 0.75, "steel", 16)                   # the reel's flanges
RG.cylinder("x", (2.5, 1.5), 1.2, 3.0, 3.25, "steel", 16)
RG.cylinder("x", (2.5, 1.5), 0.95, 0.75, 3.0, "rubber", 16)                 # the cable wound on it
RG.cylinder("x", (2.5, 1.5), 0.15, 0.35, 3.4, "iron", 8)                    # the axle
RG.box((3.75, 0.82, 0.5), (5.45, 2.9, 3.4), "grey")                          # the transformer
for z in (0.8, 1.5, 2.2, 2.9):
	RG.box((3.65, 1.0, z), (3.75, 2.6, z + 0.25), "steel")                   # a cooling fin
for x in (4.15, 4.55, 4.95):
	RG.cylinder("y", (x, 1.9), 0.09, 2.9, 3.25, "concrete", 8)               # its bushings
RG.decal((5.45, 1.4, 1.0), (5.46, 2.3, 2.6), "nameplate", "east", "grey")

CG = gantry("conveyor_gantry", 8)
for i in range(8):
	y = 1.3 + 0.28 * i                                                       # the belt climbs to the tail
	CG.box((1.9, y, float(i)), (4.1, y + 0.3, i + 1.0), "steel")             # the stringers
	CG.box((2.2, y + 0.3, float(i)), (3.8, y + 0.36, i + 1.0), "rubber")     # the belt
	CG.box((2.05, y + 0.3, float(i)), (2.2, y + 0.55, i + 1.0), "iron")      # the skirts
	CG.box((3.8, y + 0.3, float(i)), (3.95, y + 0.55, i + 1.0), "iron")
	CG.box((2.1, 0.82, i + 0.4), (2.3, y, i + 0.6), "galv")                  # the legs under it
	CG.box((3.7, 0.82, i + 0.4), (3.9, y, i + 0.6), "galv")
CG.cylinder("x", (3.4, 7.8), 0.22, 1.9, 4.1, "dark", 10)                     # the head pulley at the tail
CG.box((2.2, 2.0, 7.65), (3.8, 3.35, 7.95), "steel")                         # the chute dropping to the car
CG.box((4.1, 3.1, 7.2), (4.6, 3.55, 7.9), "blue")                            # the belt's drive motor
CG.decal((4.1, 1.2, 1.0), (4.11, 1.6, 2.2), "nameplate", "east", "steel")

MODELS = [CH, SE, MB, TL, PG, RG, CG]
