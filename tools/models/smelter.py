"""The copper smelter line at true size (docs/copper-smelter.md), each part drawn facing north from its main cell:
- concentrate_dryer, 4 x 5 x 16: a 2.2 m x 14 m rotary drum on two tyres and their rollers, the burner and feed chute at its north end,
  the discharge hood, exhaust fan and bag house at its south end, the girth gear and drive on the west side;
- flash_furnace, 7 x 12 x 16: the settler's refractory box on its concrete pad, the 3.5 m reaction shaft over its north end with the
  concentrate burner on top, the uptake at its south end, the matte and slag tapholes and launders, the process air main and its fan;
- copper_converter, 5 x 5 x 9: the 3.0 m x 6.1 m drum on its riding rings and rollers, the tuyere line and air header, the mouth and
  hood over it, the drive at its north end, its off-gas flue to the south;
- anode_furnace, 5 x 5 x 9: a smaller drum on rings, the burner at its north end, the gas lance and tuyeres, the spout to the wheel;
- anode_casting_wheel, 8 x 3 x 8: the round table of 16 copper moulds round its hub, the tundish and ladle, the take-off crane and
  the cooling spray hood;
- refinery_tankhouse, 9 x 3 x 24: four rows of 18 polymer-concrete cells, their anode and cathode plates, the copper bus bars
  between them, the walkway grating and the rectifier's DC bus at the north end.
"""
import tlib
from tlib import model, motor_box


def box_mv(M, y=0.4, z=0.2):
	"""A medium-voltage terminal box on the west face of the main cell."""
	M.box((0.0, y, z), (0.4, y + 0.8, z + 0.7), "grey")
	M.decal((-0.01, y + 0.1, z + 0.1), (0.0, y + 0.6, z + 0.6), "arcflash", "west", "grey")


def drum_z(M, cx, cy, r, z0, z1, tex, n=8):
	"""A horizontal drum along z, stepped to the circle (too wide for one turned slab)."""
	import math
	for i in range(n):
		x0 = cx - r + i * 2 * r / n
		x1 = x0 + 2 * r / n
		xm = max(abs(x0 - cx), abs(x1 - cx)) if (x0 - cx) * (x1 - cx) > 0 else 0.0
		h = math.sqrt(max(0.0, r * r - xm * xm))
		M.box((x0, cy - h, z0), (x1, cy + h, z1), tex)


def ring_z(M, cx, cy, r, z0, z1, tex):
	"""A riding ring or tyre round a drum: a square band a little wider than the drum."""
	M.box((cx - r, cy - r, z0), (cx + r, cy - r + 0.15, z1), tex)
	M.box((cx - r, cy + r - 0.15, z0), (cx + r, cy + r, z1), tex)
	M.box((cx - r, cy - r, z0), (cx - r + 0.15, cy + r, z1), tex)
	M.box((cx + r - 0.15, cy - r, z0), (cx + r, cy + r, z1), tex)


# ---- the concentrate dryer ---------------------------------------------------------------------------------------------------------
CD = model("concentrate_dryer", 4, 5, 16, "steel")
CD.box((0.0, 0.0, 0.0), (4.0, 0.25, 16.0), "concrete")
for z in (3.5, 11.5):
	CD.box((0.6, 0.25, z), (3.4, 1.0, z + 1.0), "concrete")                       # the roller piers
	CD.box((0.9, 1.0, z + 0.2), (1.3, 1.3, z + 0.8), "iron")                      # the rollers
	CD.box((2.7, 1.0, z + 0.2), (3.1, 1.3, z + 0.8), "iron")
	ring_z(CD, 2.0, 2.4, 1.25, z + 0.25, z + 0.75, "iron")                         # the tyres
drum_z(CD, 2.0, 2.4, 1.1, 1.5, 14.5, "grey")                                      # the drum, 2.2 m
CD.box((1.0, 3.5, 0.6), (3.0, 4.6, 1.5), "galv")                                  # the feed chute, north end
CD.box((0.8, 1.4, 0.2), (3.2, 3.4, 1.5), "hipaint")                              # the burner and its combustion chamber
CD.pipe([(3.2, 1.8, 0.4), (4.0, 1.8, 0.4)], 2, "galv")                            # its gas line, east side at the front
CD.box((0.6, 1.0, 14.5), (3.4, 3.8, 15.8), "galv")                                # the discharge hood
CD.box((2.6, 0.25, 15.0), (3.8, 4.8, 15.9), "steel")                              # the bag house
CD.cylinder("z", (1.2, 2.0), 0.5, 15.1, 15.9, "blue", 12)                         # the exhaust fan
CD.box((0.3, 1.6, 7.6), (0.6, 3.2, 8.4), "iron")                                  # the girth gear's guard
CD.box((0.3, 0.25, 7.6), (0.9, 1.2, 8.6), "blue")                                 # the drum drive
motor_box(CD, 0.4, 0.15)
CD.collide((0.6, 0.0, 0.2), (3.4, 3.8, 15.9))

# ---- the flash smelting furnace ----------------------------------------------------------------------------------------------------
FF = model("flash_furnace", 7, 12, 16, "iron")
FF.box((0.0, 0.0, 0.0), (7.0, 0.5, 16.0), "concrete")
FF.box((0.5, 0.5, 0.5), (6.5, 3.5, 15.5), "iron")                                 # the settler's steel shell
for z in range(1, 16, 2):
	FF.box((0.4, 0.5, z), (0.5, 3.6, z + 0.2), "dark")                            # its buckstays
	FF.box((6.5, 0.5, z), (6.6, 3.6, z + 0.2), "dark")
FF.box((0.7, 3.5, 0.7), (6.3, 3.9, 15.3), "concrete")                             # the settler's roof bricks
tlib.disc(FF, 3.5, 3.0, 1.75, 3.9, 9.9, "iron")                                    # the reaction shaft, 3.5 m
for y in (5.0, 7.0, 9.0):
	tlib.disc(FF, 3.5, 3.0, 1.85, y, y + 0.2, "dark")                              # its cooling jacket bands
FF.cylinder("y", (3.5, 3.0), 0.6, 9.9, 11.0, "hipaint", 12)                        # the concentrate burner
FF.box((2.6, 11.0, 2.1), (4.4, 11.9, 3.9), "galv")                                # the dry concentrate bin over it
FF.box((2.0, 3.9, 11.5), (5.0, 11.5, 14.5), "iron")                               # the uptake, south end
FF.box((2.2, 11.5, 11.8), (4.8, 11.9, 14.2), "dark")
FF.box((5.0, 10.0, 12.0), (7.0, 11.6, 16.0), "iron")                              # the off-gas flue, east out of its back cell at the top
FF.box((0.0, 0.8, 5.0), (0.5, 1.2, 9.0), "copper")                                # the matte launder, west side
FF.box((6.5, 1.6, 10.0), (7.0, 1.9, 14.0), "dark")                                # the slag launder, east side
FF.pipe([(6.0, 4.5, 0.4), (6.0, 4.5, 3.0), (5.3, 4.5, 3.0)], 8, "galv")         # the process air main to the burner
FF.cylinder("z", (6.0, 1.3), 0.6, 0.0, 0.5, "blue", 12)                           # the process air fan
FF.pipe([(6.6, 1.0, 0.3), (7.0, 1.0, 0.3)], 2, "galv")                            # the burners' gas, east side at the front
FF.box((0.5, 3.9, 0.5), (6.5, 4.0, 1.5), "grating")                               # the charging deck
FF.ladder(0.6, 1.6, 0.5, 3.9, 0, "galv")
motor_box(FF, 0.6, 0.6)
FF.collide((0.5, 0.0, 0.5), (6.5, 3.9, 15.5))

# ---- the Peirce-Smith converter ---------------------------------------------------------------------------------------------------
PS = model("copper_converter", 5, 5, 9, "iron")
PS.box((0.0, 0.0, 0.0), (5.0, 0.3, 9.0), "concrete")
for z in (1.6, 6.4):
	PS.box((0.8, 0.3, z), (4.2, 0.8, z + 1.0), "concrete")                        # the roller piers
	ring_z(PS, 2.5, 2.2, 1.65, z + 0.2, z + 0.8, "iron")                           # the riding rings
drum_z(PS, 2.5, 2.2, 1.5, 1.4, 7.7, "iron", 10)                                   # the drum, 3.0 m x 6.1 m
for z in (1.4, 7.6):
	PS.box((1.2, 0.9, z - 0.05), (3.8, 3.5, z + 0.05), "dark")                     # its end plates
PS.pipe([(4.1, 2.0, 1.6), (4.1, 2.0, 7.4)], 8, "galv")                            # the tuyere line's air header
for z in (2.2, 3.0, 3.8, 4.6, 5.4, 6.2, 7.0):
	PS.pipe([(4.1, 2.0, z), (3.95, 2.0, z)], 2, "iron")                           # the tuyeres
PS.box((1.8, 3.6, 3.6), (3.2, 4.0, 5.2), "dark")                                  # the mouth
PS.box((1.4, 4.0, 3.2), (3.6, 4.9, 5.6), "galv")                                  # the hood
PS.box((3.6, 4.2, 4.0), (5.0, 4.8, 5.0), "galv")                                  # the off-gas flue
PS.box((3.6, 4.2, 5.0), (4.4, 4.8, 9.0), "galv")                                  # the flue on to the back cell
PS.box((4.4, 4.2, 8.0), (5.0, 4.8, 9.0), "galv")                                  # its outlet, east of the back cell at the top
PS.box((1.6, 0.3, 0.2), (3.4, 2.0, 1.3), "blue")                                  # the drive and its gear box, north end
PS.pipe([(4.6, 0.6, 0.6), (4.6, 2.0, 0.6), (4.6, 2.0, 1.6), (4.1, 2.0, 1.6)], 8, "galv")   # the air main from the blower
box_mv(PS)
PS.collide((0.8, 0.0, 0.2), (4.2, 4.9, 8.8))

# ---- the anode furnace ------------------------------------------------------------------------------------------------------------
AF = model("anode_furnace", 5, 5, 9, "iron")
AF.box((0.0, 0.0, 0.0), (5.0, 0.3, 9.0), "concrete")
for z in (1.8, 6.2):
	AF.box((0.8, 0.3, z), (4.2, 0.8, z + 1.0), "concrete")
	ring_z(AF, 2.5, 2.2, 1.75, z + 0.2, z + 0.8, "iron")
drum_z(AF, 2.5, 2.2, 1.6, 1.2, 8.2, "iron", 10)                                   # the drum, 3.2 m x 7 m
AF.box((1.9, 1.4, 0.3), (3.1, 3.0, 1.2), "hipaint")                              # the burner, north end
AF.pipe([(3.1, 1.8, 0.5), (5.0, 1.8, 0.5)], 2, "galv")                            # its gas, east side at the front
AF.pipe([(4.2, 2.6, 2.0), (4.2, 2.6, 7.0)], 4, "galv")                            # the poling gas and air lance header
AF.box((2.0, 3.8, 3.8), (3.0, 4.3, 5.0), "dark")                                  # the mouth
AF.box((1.6, 4.3, 3.4), (3.4, 4.9, 5.4), "galv")                                  # its hood
AF.box((2.2, 0.9, 8.2), (2.8, 1.3, 9.0), "copper")                                # the spout to the wheel, south end
AF.box((0.6, 0.3, 4.0), (1.2, 1.2, 5.0), "blue")                                  # the drive
motor_box(AF, 0.4, 0.3)
AF.collide((0.8, 0.0, 0.3), (4.2, 4.9, 8.8))

# ---- the anode casting wheel ------------------------------------------------------------------------------------------------------
CW = model("anode_casting_wheel", 8, 3, 8, "grey")
CW.box((0.0, 0.0, 0.0), (8.0, 0.3, 8.0), "concrete")
tlib.disc(CW, 4.0, 4.0, 3.4, 0.3, 1.0, "iron")                                     # the wheel's table
CW.cylinder("y", (4.0, 4.0), 0.6, 1.0, 1.6, "blue", 12)                           # its hub and drive
import math
for k in range(16):
	a = 2 * math.pi * k / 16
	x, z = 4.0 + 2.6 * math.cos(a), 4.0 + 2.6 * math.sin(a)
	CW.box((x - 0.35, 1.0, z - 0.35), (x + 0.35, 1.12, z + 0.35), "copper")       # the moulds, an anode in each
CW.box((3.3, 1.0, 0.2), (4.7, 2.0, 1.0), "hipaint")                              # the tundish and casting ladle, north side
CW.box((3.6, 1.6, 1.0), (4.4, 1.8, 1.6), "copper")                                # the pour
CW.box((0.4, 1.0, 3.0), (1.2, 2.8, 5.0), "galv")                                  # the cooling spray hood
CW.box((6.8, 0.3, 3.5), (7.3, 2.9, 4.0), "padgreen")                              # the take-off crane's post
CW.box((5.4, 2.6, 3.6), (7.3, 2.9, 3.9), "padgreen")                              # its jib
CW.box((6.6, 0.3, 5.6), (7.8, 0.9, 7.6), "copper")                                # the anode rack
motor_box(CW, 0.4, 0.2)
CW.collide((0.4, 0.0, 0.2), (7.6, 1.2, 7.8))

# ---- the electrorefining tankhouse ------------------------------------------------------------------------------------------------
TH = model("refinery_tankhouse", 9, 3, 24, "polymer")
TH.box((0.0, 0.0, 0.0), (9.0, 0.3, 24.0), "concrete")
rows = (0.6, 2.6, 5.0, 7.0)
for x0 in rows:
	for c in range(18):
		z0 = 2.0 + c * 1.2
		TH.box((x0, 0.3, z0), (x0 + 1.4, 1.3, z0 + 1.1), "polymer")               # a cell
		TH.box((x0 + 0.1, 1.3, z0 + 0.15), (x0 + 1.3, 1.35, z0 + 0.95), "dark")   # its electrolyte
		for p in range(3):
			TH.box((x0 + 0.15, 1.35, z0 + 0.25 + p * 0.25), (x0 + 1.25, 1.9, z0 + 0.3 + p * 0.25), "copper" if p % 2 else "steel")
	TH.box((x0 - 0.05, 1.3, 2.0), (x0 + 0.05, 1.45, 23.6), "copper")              # the bus bar along the row
TH.box((4.0, 0.3, 1.0), (5.0, 0.4, 23.8), "grating")                              # the walkway between the pairs of rows
TH.box((0.4, 0.3, 0.2), (8.6, 2.6, 1.6), "grey")                                  # the rectifier's DC bus and its enclosure, north end
TH.decal((3.5, 1.2, 0.19), (5.5, 2.2, 0.2), "danger", "north", "grey")
for x0 in rows:
	TH.box((x0 + 0.5, 1.6, 1.6), (x0 + 0.9, 2.0, 2.0), "copper")                  # the bus from the rectifier to each row
TH.pipe([(8.7, 0.6, 2.0), (8.7, 0.6, 23.5)], 4, "polymer")                         # the electrolyte header
box_mv(TH, 0.4, 0.2)
TH.collide((0.4, 0.0, 0.2), (8.6, 1.9, 23.8))

MODELS = [CD, FF, PS, AF, CW, TH]
