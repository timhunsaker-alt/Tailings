"""The pit line's flotation and tails at true size (docs/multiblocks/pit-flotation.md, P7c), each part drawn facing north from its main
cell (x east, y up, z south):
- conditioning_tank_large, 8 x 9 x 8: an 8 m agitated tank on its pad, the bridge over it with the 90 kW agitator drive and the reagent
  hopper, fines slurry in at the north face and conditioned slurry out at the south;
- tank_cell, 7 x 7 x 7: one 160 m3 forced-air tank cell, about 6.5 m across: the tank, its froth and the froth launder round its lip,
  the launder's downcomer to the east face (the concentrate), the mechanism's beam and 160 kW drive over it, the air pipe in from the
  west, the walkway; feed in at the north face, the dart-valve box and the tails out at the south;
- column_cell, 4 x 14 x 4: a 4 m column cleaner on its legs and cone: the froth launder and wash-water ring at its top, the spargers
  low in it, the feed nozzle two thirds up its north side, the concentrate's downcomer down its south side, the 15 kW tails pump under
  its cone and the tails out of its east face;
- flotation_blower_large, 4 x 4 x 6: a multistage centrifugal blower on its base, its inlet filter house at the north end, the 400 kW
  motor at the south, the air out of its top to the east;
- tailings_thickener_large, 45 x 6 x 45: a 45 m high-rate thickener: the concrete tank wall, the slurry over its bed, the overflow
  launder round the rim, the feedwell, the half bridge from the east wall to the centre drive (55 kW), the feed pipe on its rack from
  the north face to the feedwell, the underflow out at the south and the overflow water out at the east. Its empty cells (the air over
  the tank and round its corners) share one empty model (build.py, share_empty).
"""
import math
import mlib
import tlib
from tlib import motor_box, disc

# vent.py adds the duct's own texture to tlib.TEX when every module is built in one run; these parts never use it
TEX = {k: v for k, v in tlib.TEX.items() if k != "duct"}


def model(name, w, h, d, particle="grey", textures=None):
	return mlib.Model(name, mlib.footprint_box(w, h, d, 0, 0), dict(textures or TEX), (textures or TEX)[particle])


def octo(M, axis, c1, c2, r, start, end, tex):
	"""A round body too wide for one turned slab, along x or z: an octagon of three boxes (as tlib.disc is along y)."""
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


# ---- the large conditioning tank --------------------------------------------------------------------------------------------------
CK = model("conditioning_tank_large", 8, 9, 8)
CK.box((0.0, 0.0, 0.0), (8.0, 0.3, 8.0), "concrete")
CK.ring("y", (4.0, 4.0), 3.9, 0.3, 8.0, 0.1, "grey", sides=32)                # the 8 m tank
disc(CK, 4.0, 4.0, 3.85, 7.6, 7.65, "dark")                                  # the slurry
CK.box((0.2, 8.0, 3.6), (7.8, 8.2, 4.4), "grating")                          # the bridge
CK.handrail([(0.2, 3.6), (7.8, 3.6)], 8.2, "hipaint", height=0.7)
CK.handrail([(0.2, 4.4), (7.8, 4.4)], 8.2, "hipaint", height=0.7)
CK.box((3.4, 8.2, 3.45), (4.6, 8.75, 4.55), "blue")                          # the 90 kW agitator drive
CK.box((3.75, 8.75, 3.75), (4.25, 8.95, 4.25), "grey")
CK.box((5.4, 7.7, 5.4), (6.8, 8.9, 6.8), "galv")                             # the reagent hopper
CK.box((5.9, 7.4, 5.9), (6.3, 7.7, 6.3), "steel")                            # its dosing feeder
motor_box(CK, 0.4, 0.3)
CK.pipe([(4.0, 1.0, 0.0), (4.0, 1.0, 0.2)], 12, "rubber")                    # fines slurry in, the north
CK.pipe([(4.0, 0.7, 7.8), (4.0, 0.7, 8.0)], 12, "rubber")                    # conditioned slurry out, the south
CK.collide((0.1, 0.0, 0.1), (7.9, 8.2, 7.9))

# ---- the tank cell ----------------------------------------------------------------------------------------------------------------
TC = model("tank_cell", 7, 7, 7)
TC.box((0.0, 0.0, 0.0), (7.0, 0.3, 7.0), "concrete")
TC.ring("y", (3.5, 3.5), 3.2, 0.3, 5.9, 0.1, "grey", sides=28)               # the 160 m3 tank
disc(TC, 3.5, 3.5, 3.15, 5.6, 5.65, "dark")                                  # its froth
TC.ring("y", (3.5, 3.5), 3.35, 5.4, 6.0, 0.18, "galv", sides=28)             # the froth launder round its lip
TC.box((6.62, 0.3, 3.2), (6.95, 5.4, 3.8), "galv")                           # the launder's downcomer: concentrate out, the east
TC.pipe([(6.8, 0.6, 3.5), (7.0, 0.6, 3.5)], 8, "rubber")
TC.box((0.3, 6.0, 3.3), (6.7, 6.25, 3.7), "iron")                            # the mechanism's beam
TC.box((3.0, 6.25, 3.0), (4.0, 6.75, 4.0), "blue")                           # its 160 kW drive
TC.box((3.25, 6.75, 3.25), (3.75, 6.95, 3.75), "grey")
TC.box((0.3, 6.0, 2.5), (6.7, 6.1, 3.3), "grating")                          # the walkway beside the beam
TC.handrail([(0.3, 2.5), (6.7, 2.5)], 6.1, "hipaint", height=0.8)
TC.pipe([(0.0, 6.5, 3.85), (3.0, 6.5, 3.85)], 8, "galv")                     # the air in from the west, to the mechanism
TC.pipe([(3.5, 0.9, 0.0), (3.5, 0.9, 0.35)], 12, "rubber")                   # feed in, the north
TC.box((2.8, 0.3, 6.62), (4.2, 2.0, 7.0), "steel")                           # the dart-valve box
TC.pipe([(3.5, 0.8, 6.9), (3.5, 0.8, 7.0)], 12, "rubber")                    # tails out, the south
motor_box(TC, 0.4, 0.3)
TC.collide((0.2, 0.0, 0.2), (6.8, 6.1, 6.8))

# ---- the column cell --------------------------------------------------------------------------------------------------------------
CL = model("column_cell", 4, 14, 4)
CL.box((0.0, 0.0, 0.0), (4.0, 0.3, 4.0), "concrete")
for x, z in ((0.25, 0.25), (3.45, 0.25), (0.25, 3.45), (3.45, 3.45)):
	CL.box((x, 0.3, z), (x + 0.3, 1.6, z + 0.3), "iron")                     # its legs
CL.ring("y", (2.0, 2.0), 1.3, 0.9, 1.6, 0.1, "grey", sides=16)               # its cone
CL.ring("y", (2.0, 2.0), 1.9, 1.6, 13.0, 0.08, "grey", sides=20)             # the 4 m column
disc(CL, 2.0, 2.0, 1.85, 12.7, 12.75, "dark")                                # the froth
CL.ring("y", (2.0, 2.0), 1.95, 12.5, 13.2, 0.15, "galv", sides=20)           # the froth launder
CL.ring("y", (2.0, 2.0), 1.1, 13.0, 13.06, 0.05, "steel", sides=16)          # the wash-water ring over the froth
CL.box((0.15, 2.4, 1.95), (3.85, 2.5, 2.05), "steel")                        # the spargers
CL.box((1.95, 2.4, 0.15), (2.05, 2.5, 3.85), "steel")
CL.pipe([(0.0, 2.45, 1.0), (0.2, 2.45, 1.0)], 4, "galv")                     # the air in, the west
CL.pipe([(2.0, 9.0, 0.0), (2.0, 9.0, 0.2)], 8, "rubber")                     # the feed in, two thirds up, the north
CL.box((1.75, 0.3, 3.9), (2.25, 12.5, 4.0), "galv")                          # the concentrate's downcomer, the south
CL.box((1.6, 0.3, 1.6), (2.4, 0.85, 2.4), "blue")                            # the 15 kW tails pump under the cone
CL.pipe([(2.4, 0.6, 2.0), (4.0, 0.6, 2.0)], 6, "rubber")                     # its tails out, the east
CL.box((0.1, 13.2, 0.1), (3.9, 13.3, 1.0), "grating")                        # the top deck
CL.handrail([(0.1, 0.1), (3.9, 0.1)], 13.3, "hipaint", height=0.65)
motor_box(CL, 0.35, 0.3, 0.45, 0.4)
CL.collide((0.1, 0.0, 0.1), (3.9, 13.3, 3.9))

# ---- the large flotation blower ---------------------------------------------------------------------------------------------------
BW = model("flotation_blower_large", 4, 4, 6, "blue")
BW.box((0.0, 0.0, 0.0), (4.0, 0.3, 6.0), "concrete")
BW.box((0.3, 0.3, 0.3), (3.7, 0.6, 5.7), "iron")                             # its base
BW.box((0.6, 0.6, 0.0), (3.4, 3.0, 0.6), "filter")                           # the inlet filter house, the north
octo(BW, "z", 2.0, 1.9, 1.3, 0.6, 3.0, "blue")                               # the multistage casing
for z in (1.0, 1.6, 2.2):
	octo(BW, "z", 2.0, 1.9, 1.36, z, z + 0.1, "iron")                         # its stage joints
BW.box((1.7, 0.6, 3.0), (2.3, 1.6, 3.6), "iron")                             # the coupling's stand
octo(BW, "z", 2.0, 1.9, 0.3, 3.0, 3.6, "steel")                              # the coupling
octo(BW, "z", 2.0, 1.75, 0.95, 3.6, 5.6, "blue")                             # the 400 kW motor
BW.box((2.5, 3.1, 1.3), (3.3, 3.85, 2.1), "galv")                            # the discharge, up...
BW.box((3.3, 3.25, 1.4), (4.0, 3.75, 2.0), "galv")                           # ...and out to the east, to the air line
motor_box(BW, 0.6, 0.8, 0.6, 0.5)
BW.collide((0.3, 0.0, 0.0), (3.7, 3.85, 5.7))

# ---- the large tailings thickener ---------------------------------------------------------------------------------------------------
THICK = {k: TEX[k] for k in ("concrete", "dark", "galv", "grating", "hipaint", "blue", "grey", "rubber", "iron", "danger", "pipe_water")}
TL = model("tailings_thickener_large", 45, 6, 45, "concrete", THICK)
TL.share_empty = True
C, R = 22.5, 22.5           # the centre and the tank's outside radius
INNER = 22.1                 # inside its wall
SURFACE = 4.55               # the slurry over the bed, under the launder


def chord(r, zc):
	"""Half the width of the circle of radius r on the row whose centre is zc, or 0."""
	d = zc - C
	return math.sqrt(r * r - d * d) if abs(d) < r else 0.0


for k in range(45):
	h = chord(INNER, k + 0.5)
	if h > 0:
		TL.box((C - h, SURFACE, k), (C + h, SURFACE + 0.05, k + 1), "dark")    # the slurry, a row at a time: one box to a cell
TL.ring("y", (C, C), R - 0.2, 0.0, 5.0, 0.4, "concrete", sides=96)           # the 45 m tank wall
TL.ring("y", (C, C), INNER - 0.4, 4.4, 4.9, 0.3, "galv", sides=96)           # the overflow launder inside the rim
TL.ring("y", (C, C), 2.5, 3.6, 5.2, 0.1, "galv", sides=16)                   # the feedwell
TL.box((21.6, 4.6, 21.6), (23.4, 5.6, 23.4), "blue")                         # the centre drive head, 55 kW
TL.box((22.1, 5.6, 22.1), (22.9, 5.95, 22.9), "grey")
TL.box((C, 5.0, 21.9), (44.8, 5.25, 23.1), "grating")                        # the half bridge from the east wall
for z in (21.9, 23.05):
	TL.box((C, 5.85, z), (44.8, 5.9, z + 0.05), "hipaint")                   # its handrails
	TL.box((C, 5.5, z), (44.8, 5.53, z + 0.05), "hipaint")
	for x in range(23, 45, 2):
		TL.box((x, 5.25, z), (x + 0.05, 5.9, z + 0.05), "hipaint")
TL.box((22.2, 5.15, 0.4), (22.8, 5.3, 20.0), "iron")                         # the feed pipe's rack, over the tank from the north
TL.pipe([(C, 0.6, 0.0), (C, 0.6, 0.15), (C, 5.5, 0.15), (C, 5.5, 20.0), (C, 5.2, 20.0)], 12, "rubber")   # tails in, the north
TL.pipe([(C, 0.5, 44.6), (C, 0.5, 45.0)], 12, "rubber")                      # thickened tails out, the south
TL.box((44.4, 4.3, 22.0), (45.0, 4.9, 23.0), "galv")                         # the overflow box, the east
TL.pipe([(44.8, 4.3, C), (44.8, 0.8, C), (45.0, 0.8, C)], 12, "pipe_water")   # overflow water out, the east
TL.box((0.05, 0.0, 0.35), (0.25, 0.3, 0.65), "galv")                         # the junction stand at the cable, the north-west corner
motor_box(TL, 0.3, 0.3, 0.45, 0.4)
for k in range(45):                                                          # the tank, row by row: its wall to 5 m, its slurry to the surface
	outer, inner = chord(R, k + 0.5), chord(INNER, k + 0.5)
	if outer <= 0:
		continue
	if inner <= 0:
		TL.collide((C - outer, 0.0, k), (C + outer, 5.0, k + 1))
		continue
	TL.collide((C - outer, 0.0, k), (C - inner, 5.0, k + 1))
	TL.collide((C + inner, 0.0, k), (C + outer, 5.0, k + 1))
	TL.collide((C - inner, 0.0, k), (C + inner, SURFACE, k + 1))
TL.collide((C, 5.0, 21.9), (44.8, 5.25, 23.1))

MODELS = [CK, TC, CL, BW, TL]
