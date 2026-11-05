"""The dump station at true size (docs/multiblocks/dump-station.md), each part drawn facing north from its own main cell:
- dump_ramp, 1 x 1 x 4: the curved Granby dump ramp beside the track that lifts each car's dump wheel as it passes;
- dump_pocket, 4 x 3 x 3: the concrete pocket under the track, its 300 mm grizzly on top and its sloped floor to the feeder;
- apron_feeder, 2 x 1 x 4: overlapping steel pans between skirts, the head sprocket and the 11 kW drive;
- ore_sorter, 3 x 3 x 3: the sensor-based sorter's housing, its feed hood, the sensor and the air-jet manifold, outlet chutes;
- ore_bin, 3 x 6 x 3: a steel surge bin on four legs, its hopper bottom and its roof.
"""
import tlib
from tlib import model, motor_box, stepped

# ---- the dump ramp ----------------------------------------------------------------------------------------------------------------
DR = model("dump_ramp", 1, 1, 4, "iron")
DR.box((0.1, 0.0, 0.0), (0.9, 0.15, 4.0), "concrete")                      # its footing
for i, h in enumerate((0.25, 0.4, 0.55, 0.65, 0.65, 0.55, 0.4, 0.25)):       # the curved rail: up and back down
	DR.box((0.35, 0.15, i * 0.5), (0.65, h, i * 0.5 + 0.5), "iron")
DR.box((0.3, 0.15, 0.0), (0.7, 0.2, 4.0), "iron")                           # its sole plate
DR.collide((0.1, 0.0, 0.0), (0.9, 0.65, 4.0))

# ---- the dump pocket --------------------------------------------------------------------------------------------------------------
DP = model("dump_pocket", 4, 3, 3, "concrete")
DP.box((0.0, 0.0, 0.0), (4.0, 0.3, 3.0), "concrete")                       # the floor
DP.box((0.0, 0.3, 0.0), (0.3, 2.8, 3.0), "concrete")                        # the walls
DP.box((3.7, 0.3, 0.0), (4.0, 2.8, 3.0), "concrete")
DP.box((0.3, 0.3, 0.0), (3.7, 2.8, 0.3), "concrete")
DP.box((0.3, 1.0, 2.7), (3.7, 2.8, 3.0), "concrete")                        # the south wall, open below for the feeder
stepped(DP, 0.3, 3.7, 2.2, 0.5, 0.3, 2.7, 5, 0.1, "steel")                 # the sloped liner to the outlet
x = 0.3
while x < 3.7:
	DP.box((x, 2.8, 0.0), (x + 0.1, 3.0, 3.0), "iron")                      # the grizzly bars, 300 mm apart
	x += 0.4
DP.box((0.0, 2.8, 0.0), (0.3, 3.0, 3.0), "concrete")                        # the curb the bars rest on
DP.box((3.7, 2.8, 0.0), (4.0, 3.0, 3.0), "concrete")
DP.collide((0.0, 0.0, 0.0), (4.0, 0.3, 3.0))
DP.collide((0.0, 0.3, 0.0), (0.3, 3.0, 3.0))
DP.collide((3.7, 0.3, 0.0), (4.0, 3.0, 3.0))
DP.collide((0.3, 0.3, 0.0), (3.7, 3.0, 0.3))
DP.collide((0.3, 2.8, 0.3), (3.7, 3.0, 3.0))

# ---- the apron feeder -------------------------------------------------------------------------------------------------------------
AF = model("apron_feeder", 2, 1, 4, "grey")
AF.box((0.15, 0.0, 0.1), (0.3, 0.9, 3.9), "grey")                           # the skirts and frame
AF.box((1.7, 0.0, 0.1), (1.85, 0.9, 3.9), "grey")
z = 0.2
while z < 3.6:
	AF.box((0.3, 0.45, z), (1.7, 0.55, z + 0.24), "iron")                   # an overlapping pan
	z += 0.22
AF.cylinder("x", (0.45, 3.65), 0.3, 0.3, 1.7, "iron", 12)                    # the head sprocket shaft and wheels
AF.box((0.3, 0.05, 0.25), (0.65, 0.4, 0.75), "blue")                        # the 11 kW drive
motor_box(AF, 0.05, 0.85, 0.35, 0.3)
AF.collide((0.15, 0.0, 0.1), (1.85, 0.9, 3.9))

# ---- the ore sorter ---------------------------------------------------------------------------------------------------------------
OS = model("ore_sorter", 3, 3, 3, "grey")
OS.box((0.0, 0.0, 0.0), (3.0, 0.2, 3.0), "checker")
OS.box((0.3, 0.2, 0.3), (2.7, 2.4, 2.7), "grey")                            # the housing
OS.box((0.9, 2.4, 0.3), (2.1, 2.95, 1.3), "galv")                           # the feed hood
OS.box((0.6, 2.4, 1.5), (2.4, 2.8, 2.4), "blue")                            # the sensor and its source
OS.box((0.4, 1.2, 2.7), (2.6, 1.5, 2.8), "steel")                           # the air-jet manifold
for (x1, z1, x2, z2) in ((1.2, 2.7, 1.8, 3.0), (2.7, 1.2, 3.0, 1.8), (0.0, 1.2, 0.3, 1.8)):
	OS.box((x1, 0.4, z1), (x2, 1.0, z2), "galv")                            # the outlet chutes
OS.decal((2.7, 1.6, 0.6), (2.71, 2.1, 1.1), "controller", "east", "grey")   # its control panel
motor_box(OS, 0.4, 0.3)
OS.collide((0.3, 0.0, 0.3), (2.7, 2.95, 2.7))

# ---- the ore bin ------------------------------------------------------------------------------------------------------------------
OB = model("ore_bin", 3, 6, 3, "galv")
for x in (0.2, 2.6):
	for z in (0.2, 2.6):
		OB.box((x, 0.0, z), (x + 0.2, 2.2, z + 0.2), "iron")                # its legs
OB.box((0.1, 2.2, 0.1), (2.9, 5.6, 2.9), "galv")                            # the bin
for i, (a, y) in enumerate(((0.3, 2.0), (0.6, 1.75), (0.9, 1.5), (1.15, 1.25))):
	OB.box((a, y, a), (3.0 - a, y + 0.25, 3.0 - a), "galv")                 # its hopper bottom, stepped in
OB.box((1.2, 0.9, 1.2), (1.8, 1.25, 1.8), "steel")                          # the outlet gate
OB.box((0.05, 5.6, 0.05), (2.95, 5.75, 2.95), "grey")                       # its roof
OB.ladder(0.6, 2.9, 0.0, 5.6, 1, "galv")
OB.decal((0.09, 4.2, 1.1), (0.1, 4.7, 1.9), "nameplate", "west", "galv")
OB.collide((0.1, 2.2, 0.1), (2.9, 5.75, 2.9))
OB.collide((0.2, 0.0, 0.2), (2.8, 2.2, 2.8))

MODELS = [DR, DP, AF, OS, OB]
