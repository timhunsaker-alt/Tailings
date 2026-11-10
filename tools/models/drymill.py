"""The dry mill at true size (docs/multiblocks/dry-mill.md, L6), each part drawn facing north from its main cell:
- scavenger_spiral_bank, 4 x 5 x 6: the spiral bank's twin on its tails, blue-framed to tell them apart;
- fluid_bed_dryer, 3 x 4 x 6: a long steel bed on legs under its hood, the feed hopper at the north end, the gas burner on the east
  side, the fluidising blower at the front and the exhaust cyclone and stack at the back;
- rer_separator, 2 x 3 x 3: a rare-earth roll magnet, one roll under its vibrating feeder, two product chutes;
- ht_roll_separator, 2 x 3 x 4: two high-tension rolls one above the other, the electrode and its high-voltage cabinet;
- irm_separator, 2 x 3 x 3: an induced-roll magnet, three rolls stacked in a closed frame, its magnet coil housing on the side.
"""
import tlib
from tlib import model, motor_box

# ---- the scavenger spiral bank -------------------------------------------------------------------------------------------------
SS = model("scavenger_spiral_bank", 4, 5, 6, "galv")
SS.box((0.0, 0.0, 0.0), (4.0, 0.25, 6.0), "concrete")
SS.box((0.2, 0.25, 1.2), (3.8, 0.7, 5.8), "galv")                           # the launders
for x in (0.15, 3.75):
	for z in (1.15, 3.35, 5.55):
		SS.box((x, 0.25, z), (x + 0.1, 4.2, z + 0.1), "blue")               # the frame's columns
SS.box((0.15, 4.2, 1.15), (3.85, 4.3, 5.65), "grating")                     # the top deck
for cx in (1.0, 2.0, 3.0):
	for cz in (1.9, 3.5, 5.0):
		SS.cylinder("y", (cx, cz), 0.07, 0.7, 4.2, "iron", 8)
		for level in range(5):
			y = 0.95 + level * 0.65
			SS.ring("y", (cx, cz), 0.42, y, y + 0.14, 0.14, "polymer", sides=10)
SS.cylinder("y", (2.0, 3.4), 0.5, 4.3, 4.9, "galv", 12)                     # the feed distributor
SS.cylinder("z", (1.0, 0.6), 0.32, 0.25, 0.95, "blue", 12)                  # the feed pump
SS.pipe([(1.0, 0.6, 0.95), (1.0, 4.6, 0.95), (1.0, 4.6, 3.4), (1.5, 4.6, 3.4)], 6, "rubber")
motor_box(SS, 0.4, 0.2)
SS.pipe([(1.5, 0.6, 0.0), (1.5, 0.6, 0.25)], 6, "rubber")                   # the spirals' tails in, the front
SS.pipe([(0.5, 0.45, 5.8), (0.5, 0.45, 6.0)], 6, "rubber")                  # the rest out, the back, west
SS.pipe([(3.5, 0.45, 5.8), (3.5, 0.45, 6.0)], 4, "rubber")                  # HMC out, the back, east
SS.collide((0.0, 0.0, 0.0), (4.0, 4.3, 6.0))

# ---- the fluid-bed dryer -------------------------------------------------------------------------------------------------------
FD = model("fluid_bed_dryer", 3, 4, 6, "steel")
FD.box((0.0, 0.0, 0.0), (3.0, 0.25, 6.0), "concrete")
for x in (0.35, 2.45):
	for z in (1.0, 3.0, 5.2):
		FD.box((x, 0.25, z), (x + 0.2, 1.2, z + 0.2), "iron")               # the legs
FD.box((0.3, 1.2, 0.9), (2.7, 1.5, 5.6), "iron")                            # the plenum under the air distributor
FD.box((0.3, 1.5, 0.9), (2.7, 2.6, 5.6), "steel")                           # the bed
FD.box((0.4, 2.6, 1.0), (2.6, 3.0, 5.5), "grey")                            # the hood
FD.box((0.9, 3.0, 1.0), (2.1, 3.7, 1.8), "galv")                            # the feed hopper, north end
FD.box((1.1, 3.7, 1.15), (1.9, 3.75, 1.65), "grating")
FD.box((2.7, 1.6, 1.2), (2.95, 2.2, 2.0), "hipaint")                        # the gas burner, east side
FD.pipe([(2.95, 1.9, 1.6), (3.0, 1.9, 1.6)], 2, "galv")                     # its gas line
FD.cylinder("z", (1.5, 0.75), 0.45, 0.1, 0.85, "blue", 12)                  # the fluidising blower
FD.pipe([(1.5, 0.75, 0.85), (1.5, 1.3, 0.85), (1.5, 1.3, 0.95)], 8, "galv")
FD.cylinder("y", (1.5, 5.75), 0.22, 2.0, 3.6, "galv", 10)                   # the exhaust cyclone
FD.cylinder("y", (1.5, 5.75), 0.12, 3.6, 3.95, "iron", 8)                   # its stack
FD.box((1.2, 1.3, 5.6), (1.8, 1.6, 6.0), "galv")                            # the dry HMC out, the back
motor_box(FD, 0.35, 0.15)
FD.collide((0.3, 0.0, 0.9), (2.7, 3.0, 5.6))

# ---- the separators ------------------------------------------------------------------------------------------------------------


def frame(M, w, h, d, tex="blue"):
	M.box((0.0, 0.0, 0.0), (w, 0.2, d), "concrete")
	for x in (0.1, w - 0.2):
		for z in (0.1, d - 0.2):
			M.box((x, 0.2, z), (x + 0.1, h - 0.3, z + 0.1), tex)
	for z in (0.1, d - 0.2):
		M.box((0.1, h - 0.3, z), (w - 0.1, h - 0.2, z + 0.1), tex)         # the top beams, open over the feeder
	for x in (0.1, w - 0.2):
		M.box((x, h - 0.3, 0.2), (x + 0.1, h - 0.2, d - 0.2), tex)


def feeder(M, w, d0, d1, top):
	"""The vibrating feeder's hopper on top, that spreads the HMC across the roll."""
	M.box((0.3, top - 0.6, d0), (w - 0.3, top, d1), "galv")
	M.box((0.4, top - 0.75, d0 + 0.1), (w - 0.4, top - 0.6, d1 - 0.1), "steel")


RE = model("rer_separator", 2, 3, 3, "blue")
frame(RE, 2, 3, 3)
feeder(RE, 2, 0.6, 1.6, 2.7)
RE.cylinder("x", (1.5, 1.4), 0.25, 0.25, 1.75, "dark", 14)                  # the rare-earth roll
RE.cylinder("x", (1.5, 1.4), 0.05, 0.15, 1.85, "iron", 6)                   # its shaft
RE.box((0.3, 0.3, 1.6), (1.7, 1.1, 2.1), "galv")                            # the magnetics chute
RE.box((0.3, 0.3, 0.6), (1.7, 1.0, 1.1), "galv")                            # the non-magnetics chute
RE.cylinder("x", (0.6, 2.5), 0.2, 1.75, 1.95, "blue", 10)                   # the roll's drive
motor_box(RE, 0.3, 0.15, 0.5, 0.4)
RE.collide((0.1, 0.0, 0.1), (1.9, 2.8, 2.9))

HT = model("ht_roll_separator", 2, 3, 4, "grey")
frame(HT, 2, 3, 4, "grey")
feeder(HT, 2, 0.4, 1.4, 2.75)
for y, z in ((1.75, 1.2), (0.95, 2.2)):
	HT.cylinder("x", (y, z), 0.135, 0.25, 1.75, "steel", 14)               # the 270 mm rolls
	HT.box((0.25, y + 0.05, z + 0.25), (1.75, y + 0.12, z + 0.32), "copper")   # the electrode beside each
HT.box((0.3, 0.2, 2.8), (1.7, 0.7, 3.6), "galv")                            # the conductors' chute
HT.box((0.3, 0.2, 0.3), (1.7, 0.6, 0.9), "galv")                            # the non-conductors' chute
HT.box((1.25, 1.6, 3.2), (1.85, 2.5, 3.85), "hipaint")                      # the high-voltage supply cabinet
HT.decal((1.3, 2.0, 3.86), (1.8, 2.4, 3.87), "danger", "south", "hipaint")
motor_box(HT, 0.3, 0.15, 0.5, 0.4)
HT.collide((0.1, 0.0, 0.1), (1.9, 2.8, 3.9))

IR = model("irm_separator", 2, 3, 3, "blue")
frame(IR, 2, 3, 3)
feeder(IR, 2, 0.5, 1.4, 2.75)
IR.box((0.25, 0.4, 0.4), (1.75, 2.0, 1.9), "steel")                         # the closed magnet frame
for k, y in enumerate((1.7, 1.2, 0.7)):
	IR.cylinder("x", (y, 1.95 + 0.12 * k), 0.12, 0.3, 1.7, "dark", 12)     # the induced rolls
IR.box((1.75, 0.5, 0.6), (1.95, 1.9, 1.7), "copper")                        # the coil housing
IR.box((0.3, 0.2, 2.2), (1.7, 0.6, 2.8), "galv")                            # the products' chutes
motor_box(IR, 0.3, 0.15, 0.5, 0.4)
IR.collide((0.1, 0.0, 0.1), (1.9, 2.8, 2.9))

MODELS = [SS, FD, RE, HT, IR]
