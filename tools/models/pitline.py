"""The pit line's primary crushing and stockpile at true size (docs/multiblocks/primary-gyratory-stockpile.md, P7a), each part drawn facing
north from its main cell (x east, y up, z south):
- gyratory_crusher, 6 x 10 x 6: the 42-65 class gyratory on its foundation: the bottom shell and its discharge, the drive's pinion
  housing and the 450 kW motor on the west side, the concave shell, the top shell (the bowl) and the spider across its top, the mantle's
  head showing in the bowl;
- gyratory_pocket, 14 x 8 x 10: the concrete direct-tip pocket over the bowl, its floor's opening over the crusher (x 4-10, z 2-8), walls
  falling in toward the opening, its rim and the dump walls' faces on its east and west tipping sides;
- rock_breaker, 2 x 3 x 6: a pedestal rock breaker on its column, its boom reaching south over the bowl, the hammer at its tip;
- surge_pocket, 8 x 6 x 8: the steel-lined bin under the crusher, its hopper falling to the outlet over the apron feeder;
- primary_apron_feeder, 3 x 2 x 12: the 2.4 m apron of steel pans on its chain, the skirts, the head sprockets at the south end and its
  drive;
- heavy_conveyor_drive, 2 x 2 x 3: a head pulley 1.2 m across and its 355 kW motor and gearbox on a base frame;
- stockpile_head, 3 x 4 x 4: the head frame on its gallery over the pile, the head pulley, the chute dropping out of the south end's
  floor;
- reclaim_feeder, 2 x 2 x 6: a 1.5 m apron feeder under the tunnel roof's opening, its feed skirts at the north end;
- belt_weigher, 2 x 1 x 2: a weigh frame with its idlers on load cells beside the belt, its instrument box;
- heavy_belt_conveyor, one metre of 1,400 mm troughed belt on its idlers, drawn the full block wide.
"""
import json, os
import mlib
import tlib
from tlib import model, motor_box, disc, stepped


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


# ---- the gyratory crusher ---------------------------------------------------------------------------------------------------------
GY = model("gyratory_crusher", 6, 10, 6, "grey")
GY.box((0.0, 0.0, 0.0), (6.0, 0.6, 6.0), "concrete")                         # its foundation
for x, z in ((0.2, 0.2), (5.0, 0.2), (0.2, 5.0), (5.0, 5.0)):
	GY.box((x, 0.6, z), (x + 0.8, 1.6, z + 0.8), "concrete")                # the piers
disc(GY, 3.0, 3.0, 2.7, 1.6, 4.2, "steel")                                   # the bottom shell, round the discharge
GY.box((2.2, 0.6, 2.2), (3.8, 1.6, 3.8), "dark")                             # the discharge opening to the surge pocket
GY.box((1.3, 1.9, 2.6), (1.5, 2.9, 3.4), "blue")                             # the pinion housing's flange
GY.box((0.3, 1.9, 2.5), (1.3, 2.9, 3.5), "steel")                            # the countershaft box
octo(GY, "x", 2.4, 3.0, 0.45, 0.0, 0.3, "blue")                              # the coupling
GY.box((0.0, 1.6, 0.6), (0.3, 1.9, 2.4), "iron")                            # the motor's base
disc(GY, 3.0, 3.0, 2.5, 4.2, 7.4, "blue")                                    # the concave shell
for y in (4.6, 5.6, 6.6):
	GY.box((0.45, y, 2.8), (5.55, y + 0.15, 3.2), "steel")                   # its flanges, ribbed
	GY.box((2.8, y, 0.45), (3.2, y + 0.15, 5.55), "steel")
disc(GY, 3.0, 3.0, 2.95, 7.4, 9.2, "steel")                                  # the top shell, the bowl's rim
disc(GY, 3.0, 3.0, 1.2, 9.2, 9.25, "dark")                                   # the bowl's mouth, the 1,066 mm opening round the head
disc(GY, 3.0, 3.0, 0.9, 9.0, 9.4, "iron")                                    # the mantle's head
GY.box((0.1, 9.2, 2.6), (5.9, 9.8, 3.4), "blue")                             # the spider arm across the top
disc(GY, 3.0, 3.0, 0.7, 9.2, 10.0, "blue")                                   # the spider cap over the main shaft
GY.box((0.25, 9.8, 2.65), (5.75, 9.85, 3.35), "rubber")                      # the arm's liner
GY.box((5.0, 0.6, 0.3), (5.9, 1.6, 1.6), "grey")                             # the lube unit and its tank
GY.pipe([(5.4, 1.6, 1.0), (5.4, 3.0, 1.0), (4.6, 3.0, 1.0)], 2, "pipe_diesel")
motor_box(GY, 2.0, 0.6, 0.6, 0.5)
GY.collide((0.2, 0.0, 0.2), (5.8, 10.0, 5.8))

# ---- the gyratory's dump pocket ---------------------------------------------------------------------------------------------------
GP = model("gyratory_pocket", 14, 8, 10, "concrete")
OX0, OX1, OZ0, OZ1 = 4.5, 9.5, 2.5, 7.5                                      # the floor's opening, over the bowl
GP.box((0.0, 0.0, 0.0), (14.0, 1.0, OZ0), "concrete")                        # the floor round the opening
GP.box((0.0, 0.0, OZ1), (14.0, 1.0, 10.0), "concrete")
GP.box((0.0, 0.0, OZ0), (OX0, 1.0, OZ1), "concrete")
GP.box((OX1, 0.0, OZ0), (14.0, 1.0, OZ1), "concrete")
GP.box((OX0 - 0.15, 0.85, OZ0 - 0.15), (OX1 + 0.15, 1.0, OZ0), "steel")    # the opening's steel lip
GP.box((OX0 - 0.15, 0.85, OZ1), (OX1 + 0.15, 1.0, OZ1 + 0.15), "steel")
GP.box((0.0, 0.0, 0.0), (14.0, 8.0, 0.6), "concrete")                        # the north and south walls
GP.box((0.0, 0.0, 9.4), (14.0, 8.0, 10.0), "concrete")
for k in range(6):
	y0 = 1.0 + k * 1.1
	inset = 0.6 + (6 - k) * 0.55                                              # the east and west faces fall in toward the opening
	GP.box((0.0, y0, 0.6), (inset, y0 + 1.1, 9.4), "concrete")
	GP.box((14.0 - inset, y0, 0.6), (14.0, y0 + 1.1, 9.4), "concrete")
	GP.box((inset, y0, 0.6), (inset + 0.12, y0 + 1.1, 9.4), "steel")         # its wear liners
	GP.box((14.0 - inset - 0.12, y0, 0.6), (14.0 - inset, y0 + 1.1, 9.4), "steel")
GP.box((0.0, 7.6, 0.0), (14.0, 8.0, 0.6), "hipaint")                         # the rim's painted edges
GP.box((0.0, 7.6, 9.4), (14.0, 8.0, 10.0), "hipaint")
GP.collide((0.0, 0.0, 0.0), (14.0, 8.0, 0.6))
GP.collide((0.0, 0.0, 9.4), (14.0, 8.0, 10.0))
GP.collide((0.0, 0.0, 0.6), (OX0, 1.0, 9.4))
GP.collide((OX1, 0.0, 0.6), (14.0, 1.0, 9.4))
for k in range(6):
	y0 = 1.0 + k * 1.1
	inset = 0.6 + (6 - k) * 0.55
	GP.collide((0.0, y0, 0.6), (inset, y0 + 1.1, 9.4))
	GP.collide((14.0 - inset, y0, 0.6), (14.0, y0 + 1.1, 9.4))

# ---- the rock breaker -------------------------------------------------------------------------------------------------------------
RB = model("rock_breaker", 2, 3, 6, "hipaint")
RB.box((0.1, 0.0, 0.1), (1.9, 0.3, 1.9), "concrete")                         # its plinth
disc(RB, 1.0, 1.0, 0.55, 0.3, 1.6, "hipaint")                                 # the pedestal
disc(RB, 1.0, 1.0, 0.6, 1.6, 1.85, "iron")                                    # the slew ring
RB.box((0.6, 1.85, 0.6), (1.4, 2.4, 1.6), "hipaint")                          # the boom's foot
for k in range(6):
	z = 1.4 + k * 0.6
	y = 2.4 - k * 0.12
	RB.box((0.75, y, z), (1.25, y + 0.4, z + 0.65), "hipaint")              # the boom, reaching south
RB.box((0.8, 1.2, 4.9), (1.2, 2.0, 5.3), "hipaint")                          # the stick
RB.box((0.85, 0.3, 4.95), (1.15, 1.2, 5.25), "dark")                         # the hammer
RB.box((0.95, 0.0, 5.05), (1.05, 0.3, 5.15), "steel")                        # its moil point
RB.pipe([(1.3, 2.6, 1.2), (1.3, 2.6, 4.6)], 1, "rubber")                     # the hoses along the boom
RB.box((0.1, 0.3, 2.0), (1.9, 1.3, 3.2), "grey")                             # the power pack
motor_box(RB, 0.35, 0.25, 0.5, 0.45)
RB.collide((0.1, 0.0, 0.1), (1.9, 2.4, 1.9))
RB.collide((0.1, 0.3, 2.0), (1.9, 1.3, 3.2))

# ---- the surge pocket -------------------------------------------------------------------------------------------------------------
SP = model("surge_pocket", 8, 6, 8, "galv")
for x, z in ((0.1, 0.1), (7.4, 0.1), (0.1, 7.4), (7.4, 7.4)):
	SP.box((x, 0.0, z), (x + 0.5, 6.0, z + 0.5), "galv")                     # its columns
SP.box((0.1, 5.4, 0.1), (7.9, 6.0, 7.9), "steel")                            # the top ring under the crusher
for k in range(5):
	y0 = 0.4 + k * 1.0
	i = 2.8 - k * 0.6                                                           # the hopper's sides, falling to the outlet
	SP.box((i, y0, i), (8.0 - i, y0 + 1.0, i + 0.2), "steel")
	SP.box((i, y0, 7.8 - i), (8.0 - i, y0 + 1.0, 8.0 - i), "steel")
	SP.box((i, y0, i), (i + 0.2, y0 + 1.0, 8.0 - i), "steel")
	SP.box((7.8 - i, y0, i), (8.0 - i, y0 + 1.0, 8.0 - i), "steel")
SP.box((3.0, 0.0, 3.0), (5.0, 0.4, 5.0), "dark")                             # the outlet over the apron feeder
SP.ladder(0.9, 0.6, 0.0, 6.0, -1, "galv")
SP.collide((0.1, 0.0, 0.1), (7.9, 6.0, 7.9))

# ---- the primary apron feeder -----------------------------------------------------------------------------------------------------
PF = model("primary_apron_feeder", 3, 2, 12, "grey")
for z in (0.3, 4.0, 7.7, 11.2):
	PF.box((0.1, 0.0, z), (2.9, 0.6, z + 0.5), "concrete")                   # its piers
PF.box((0.2, 0.6, 0.2), (0.4, 1.2, 11.6), "blue")                            # the side frames
PF.box((2.6, 0.6, 0.2), (2.8, 1.2, 11.6), "blue")
for k in range(23):
	z = 0.3 + k * 0.5
	PF.box((0.4, 1.2, z), (2.6, 1.35, z + 0.46), "steel")                     # the pans
octo(PF, "x", 1.0, 0.5, 0.45, 0.4, 2.6, "iron")                              # the tail sprockets
octo(PF, "x", 1.0, 11.4, 0.5, 0.4, 2.6, "iron")                              # the head sprockets
PF.box((0.25, 1.35, 0.2), (0.4, 1.95, 9.5), "steel")                         # the skirts
PF.box((2.6, 1.35, 0.2), (2.75, 1.95, 9.5), "steel")
PF.box((2.8, 0.6, 10.6), (3.0, 1.4, 11.8), "grey")                           # the drive's gearbox
motor_box(PF, 0.7, 0.4, 0.5, 0.5)
PF.collide((0.1, 0.0, 0.2), (2.9, 1.35, 11.8))

# ---- the heavy conveyor drive -----------------------------------------------------------------------------------------------------
HD = model("heavy_conveyor_drive", 2, 2, 3, "grey")
HD.box((0.0, 0.0, 0.0), (2.0, 0.3, 3.0), "iron")                             # the base frame
octo(HD, "x", 1.2, 0.8, 0.6, 0.1, 1.9, "rubber")                              # the 1.2 m head pulley, lagged
HD.box((0.05, 0.3, 0.4), (0.15, 1.6, 1.2), "galv")                            # its bearings' stands
HD.box((1.85, 0.3, 0.4), (1.95, 1.6, 1.2), "galv")
HD.box((1.2, 0.3, 1.6), (1.95, 1.2, 2.2), "grey")                             # the gearbox
octo(HD, "z", 1.55, 0.75, 0.42, 2.2, 3.0, "blue")                              # the 355 kW motor
motor_box(HD, 0.4, 0.3, 0.6, 0.5)
HD.collide((0.0, 0.0, 0.0), (2.0, 1.8, 3.0))

# ---- the stockpile head -----------------------------------------------------------------------------------------------------------
SH = model("stockpile_head", 3, 4, 4, "galv")
for x, z in ((0.1, 0.1), (2.7, 0.1), (0.1, 3.7), (2.7, 3.7)):
	SH.box((x, 0.0, z), (x + 0.2, 3.0, z + 0.2), "galv")                     # the head frame's legs
SH.box((0.0, 2.9, 0.0), (3.0, 3.1, 4.0), "grating")                          # the gallery floor
SH.handrail([(0.1, 0.1), (2.9, 0.1), (2.9, 3.9), (0.1, 3.9), (0.1, 0.1)], 3.1, "hipaint")
octo(SH, "x", 3.6, 2.6, 0.45, 0.4, 2.6, "rubber")                             # the head pulley
SH.box((0.3, 3.1, 2.0), (0.4, 3.9, 3.2), "galv")                             # its stands
SH.box((2.6, 3.1, 2.0), (2.7, 3.9, 3.2), "galv")
SH.box((0.6, 3.6, 0.0), (2.4, 3.7, 2.6), "rubber")                           # the belt coming in over the top
SH.box((1.0, 0.0, 3.0), (1.1, 2.9, 3.9), "steel")                            # the chute, dropping out of the south end
SH.box((1.9, 0.0, 3.0), (2.0, 2.9, 3.9), "steel")
SH.box((1.0, 0.0, 3.85), (2.0, 2.9, 3.95), "steel")
SH.box((1.0, 2.0, 2.4), (2.0, 2.9, 3.0), "steel")                            # the hood over the pulley
SH.collide((0.0, 2.9, 0.0), (3.0, 3.1, 4.0))

# ---- the reclaim feeder -----------------------------------------------------------------------------------------------------------
RF = model("reclaim_feeder", 2, 2, 6, "grey")
RF.box((0.1, 0.0, 0.1), (1.9, 0.4, 5.9), "concrete")                         # its plinth on the tunnel floor
RF.box((0.15, 0.4, 0.1), (0.3, 1.1, 5.9), "blue")                             # the side frames
RF.box((1.7, 0.4, 0.1), (1.85, 1.1, 5.9), "blue")
for k in range(11):
	z = 0.2 + k * 0.5
	RF.box((0.3, 1.1, z), (1.7, 1.22, z + 0.46), "steel")                     # the pans
RF.box((0.15, 1.22, 0.0), (0.3, 2.0, 2.0), "steel")                           # the skirts under the roof's opening
RF.box((1.7, 1.22, 0.0), (1.85, 2.0, 2.0), "steel")
RF.box((0.15, 1.22, 0.0), (1.85, 2.0, 0.12), "steel")
octo(RF, "x", 0.8, 5.5, 0.35, 0.3, 1.7, "iron")                               # the head sprockets
RF.box((1.85, 0.4, 4.8), (2.0, 1.1, 5.8), "grey")                             # the drive
motor_box(RF, 0.45, 0.3, 0.45, 0.45)
RF.collide((0.1, 0.0, 0.1), (1.9, 1.22, 5.9))

# ---- the belt weigher -------------------------------------------------------------------------------------------------------------
BW = model("belt_weigher", 2, 1, 2, "galv")
BW.box((0.1, 0.0, 0.1), (1.9, 0.15, 1.9), "iron")                            # the weigh frame's base
for x in (0.2, 1.6):
	BW.box((x, 0.15, 0.3), (x + 0.2, 0.35, 0.5), "brass")                   # the load cells
	BW.box((x, 0.15, 1.5), (x + 0.2, 0.35, 1.7), "brass")
BW.box((0.2, 0.35, 0.2), (1.8, 0.5, 1.8), "galv")                            # the weigh frame
BW.cylinder("x", (0.6, 0.6), 0.08, 0.3, 1.7, "steel", 8)                    # its weigh idlers
BW.cylinder("x", (0.6, 1.4), 0.08, 0.3, 1.7, "steel", 8)
BW.box((0.7, 0.15, 0.7), (1.3, 0.85, 1.3), "grey")                           # the integrator's box
BW.decal((1.3, 0.35, 0.8), (1.31, 0.75, 1.2), "gauge", "east", "grey")
BW.collide((0.1, 0.0, 0.1), (1.9, 0.5, 1.9))

# ---- one metre of heavy belt -----------------------------------------------------------------------------------------------------
HB = mlib.Model("heavy_belt_conveyor", [(0, 0, 0)], dict(tlib.TEX), tlib.TEX["iron"])
for x in (0.0, 0.92):
	HB.box((x, 0.0, 0.1), (x + 0.08, 0.7, 0.2), "galv")                     # the legs
	HB.box((x, 0.0, 0.8), (x + 0.08, 0.7, 0.9), "galv")
	HB.box((x, 0.6, 0.0), (x + 0.08, 0.7, 1.0), "galv")                     # the stringers
HB.cylinder("x", (0.66, 0.5), 0.06, 0.1, 0.9, "steel", 8)                    # a carrying idler set
HB.box((0.18, 0.72, 0.0), (0.82, 0.78, 1.0), "rubber")                       # the belt's flat
HB.box((0.08, 0.76, 0.0), (0.18, 0.86, 1.0), "rubber")                       # its troughed wings
HB.box((0.82, 0.76, 0.0), (0.92, 0.86, 1.0), "rubber")
HB.box((0.1, 0.15, 0.0), (0.9, 0.19, 1.0), "rubber")                         # the return belt
HB.export_args = {"blockstate": False, "item": True}

MODELS = [GY, GP, RB, SP, PF, HD, SH, RF, BW, HB]


def after():
	path = os.path.join(mlib.ASSETS, "blockstates", "heavy_belt_conveyor.json")
	variants = {"axis=z": {"model": "tailings:block/heavy_belt_conveyor/p0"}, "axis=x": {"model": "tailings:block/heavy_belt_conveyor/p0", "y": 90}}
	with open(path, "w") as f:
		json.dump({"variants": variants}, f, indent=1)
