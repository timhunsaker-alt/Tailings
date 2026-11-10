"""The paste fill plant at true size (docs/multiblocks/paste-backfill.md, L5), each part drawn facing north from its own main cell:
- paste_filter, 5 x 5 x 8: a vacuum disc filter: its slurry tub on a steel frame, the discs on one shaft along z (the cloth sectors
  half under the slurry), the shaft's drive at the south end, the vacuum receiver and pump beside it on the east, the filtrate line
  out of the east face and the cake chute falling out of the south face toward the mixer; thickened tails in at the north face;
- paste_mixer, 3 x 3 x 5: a continuous twin-shaft mixer: its trough on legs, the two shafts' gearbox and motor at the north end,
  the cement hopper on top with its screw, and the paste discharge at the south face;
- paste_pump, 3 x 3 x 6: a hydraulic twin-piston paste pump: its hopper with the S-valve at the north end, the two long material
  cylinders and hydraulic rams along z, the power pack and oil cooler, and the discharge spigot on the south face;
- fill_outlet: one block, the open end of the fill line: a pipe stub with a flange and a bell mouth, drawn down from its top.
Every motor box on the west face of the main cell.
"""
import json
import os

import mlib
import tlib
from tlib import model, motor_box, disc

TEX = tlib.TEX

# ---- the paste filter -------------------------------------------------------------------------------------------------------------
PF = model("paste_filter", 5, 5, 8, "grey")
PF.box((0.3, 0.0, 0.3), (4.7, 0.3, 7.7), "iron")                                # the base frame
for x in (0.4, 4.3):
	for z in (0.4, 3.8, 7.3):
		PF.box((x, 0.3, z), (x + 0.3, 1.6, z + 0.3), "iron")                    # the legs under the tub
PF.box((0.6, 1.6, 0.8), (3.4, 2.6, 7.0), "grey")                                # the slurry tub
PF.box((0.6, 2.6, 0.8), (3.4, 2.7, 7.0), "steel")                               # its rim
for z in [1.3 + 0.75 * i for i in range(7)]:
	PF.cylinder("z", (2.0, 3.0), 1.0, z, z + 0.2, "lagging", 16)             # the seven discs, their cloth sectors
	PF.ring("z", (2.0, 3.0), 1.0, z - 0.02, z + 0.22, 0.06, "steel", sides=16)
PF.cylinder("z", (2.0, 3.0), 0.18, 0.7, 7.4, "steel", 10)                       # the shaft with its filtrate ports
PF.box((1.4, 2.6, 7.2), (2.6, 3.4, 7.9), "blue")                                 # the shaft's gear drive at the south end
PF.box((0.6, 4.2, 1.0), (3.4, 4.4, 7.0), "galv")                                 # the hood over the discs
PF.cylinder("y", (4.2, 2.0), 0.55, 0.3, 3.6, "grey", 12)                        # the vacuum receiver on the east
PF.box((3.8, 0.3, 4.2), (4.7, 1.4, 5.8), "blue")                                 # the vacuum pump
PF.pipe([(4.2, 3.4, 2.0), (4.2, 3.9, 2.0), (2.0, 3.9, 2.0), (2.0, 3.9, 0.9)], 4, "steel")   # vacuum from the shaft to the receiver
PF.pipe([(4.2, 0.6, 2.0), (5.0, 0.6, 2.0)], 4, "steel")                         # the filtrate out of the east face
PF.pipe([(1.5, 0.6, 0.0), (1.5, 0.6, 0.6), (1.5, 1.8, 0.6)], 6, "steel")       # thickened tails in at the north face
tlib.stepped(PF, 0.8, 3.2, 2.0, 0.5, 7.0, 8.0, 3, 0.15, "steel")                # the cake chute out of the south face
PF.decal((1.6, 4.41, 3.6), (2.4, 4.42, 4.4), "ratingplate", "up", "galv")
motor_box(PF, 0.5, 0.3, 0.6, 0.5)
PF.collide((0.3, 0.0, 0.3), (4.7, 4.4, 7.7))

# ---- the paste mixer --------------------------------------------------------------------------------------------------------------
PM = model("paste_mixer", 3, 3, 5, "grey")
for x in (0.3, 2.4):
	for z in (0.6, 4.1):
		PM.box((x, 0.0, z), (x + 0.3, 1.0, z + 0.3), "iron")                    # the legs
PM.box((0.4, 1.0, 1.2), (2.6, 1.9, 4.6), "grey")                                # the twin trough
PM.box((0.4, 1.9, 1.2), (2.6, 2.0, 4.6), "steel")                               # its lid
for x in (1.0, 2.0):
	PM.cylinder("z", (x, 1.45), 0.08, 1.0, 4.7, "steel", 8)                     # the two paddle shafts' ends
PM.box((0.5, 1.0, 0.3), (2.5, 1.9, 1.2), "blue")                                 # the gearbox at the north end
PM.cylinder("x", (1.45, 0.65), 0.35, 0.3, 1.4, "grey", 12)                      # its motor
PM.box((0.8, 2.0, 1.6), (2.2, 2.2, 3.0), "galv")                                 # the hopper's screw feeder
PM.box((0.7, 2.2, 1.5), (2.3, 2.95, 3.1), "galv")                                # the cement hopper on top
PM.box((0.7, 2.95, 1.5), (2.3, 3.0, 3.1), "grating")
PM.pipe([(1.5, 1.2, 4.6), (1.5, 1.2, 5.0)], 6, "steel")                         # the paste out of the south face
PM.pipe([(0.6, 0.5, 0.0), (0.6, 0.5, 0.25)], 4, "steel")                        # the trim in at the north face
PM.decal((1.2, 2.21, 3.2), (1.8, 2.22, 3.6), "ratingplate", "up", "galv")
motor_box(PM, 0.5, 0.3, 0.5, 0.4)
PM.collide((0.3, 0.0, 0.3), (2.7, 3.0, 4.8))

# ---- the paste pump ---------------------------------------------------------------------------------------------------------------
PP = model("paste_pump", 3, 3, 6, "grey")
PP.box((0.2, 0.0, 0.2), (2.8, 0.3, 5.8), "iron")                                 # the skid
PP.box((0.4, 0.3, 0.2), (2.6, 1.3, 1.3), "grey")                                 # the S-valve housing
PP.box((0.3, 1.3, 0.1), (2.7, 2.1, 1.3), "galv")                                 # the paste hopper on it
PP.box((0.3, 2.1, 0.1), (2.7, 2.15, 1.3), "grating")
for x in (0.9, 2.1):
	PP.cylinder("z", (x, 0.95), 0.3, 1.3, 3.6, "steel", 12)                     # the two material cylinders
	PP.flange("z", (x, 0.95), 0.36, 3.6, 0.08, "grey", bolts=8)
	PP.box((x - 0.35, 0.6, 3.68), (x + 0.35, 1.3, 3.9), "grey")                   # the water box between them and the rams
	PP.cylinder("z", (x, 0.95), 0.2, 3.9, 5.3, "blue", 10)                      # the hydraulic rams
PP.box((0.3, 1.4, 3.9), (2.7, 2.6, 5.7), "blue")                                 # the power pack: motor, pumps and oil tank
PP.box((0.4, 2.6, 4.2), (2.6, 2.9, 5.4), "grating")                              # the oil cooler on top
PP.pipe([(1.5, 0.55, 1.0), (1.5, 0.55, 6.0)], 6, "steel")                        # the discharge line out of the south face
PP.flange("z", (1.5, 0.55), 0.22, 5.85, 0.08, "steel", bolts=6)
PP.decal((1.2, 2.61, 3.95), (1.8, 2.62, 4.15), "ratingplate", "up", "blue")
motor_box(PP, 0.5, 0.3, 0.5, 0.4)
PP.collide((0.2, 0.0, 0.1), (2.8, 2.9, 5.8))

MODELS = [PF, PM, PP]


def after():
	"""The fill outlet: a one-block model in this mod's models/block, as the vent duct's pieces are."""
	m = mlib.Model("fill_outlet", mlib.footprint_box(1, 1, 1, 0, 0), {"steel": TEX["steel"], "grey": TEX["grey"], "iron": TEX["iron"]}, TEX["steel"])
	m.cylinder("y", (0.5, 0.5), 0.16, 0.45, 1.0, "steel", 10)       # the line coming down to it
	m.flange("y", (0.5, 0.5), 0.26, 0.42, 0.06, "grey", bolts=6)
	m.cylinder("y", (0.5, 0.5), 0.22, 0.12, 0.42, "iron", 10)      # the spool
	m.ring("y", (0.5, 0.5), 0.3, 0.0, 0.12, 0.06, "iron", sides=12)   # its open bell mouth
	m.export_single("fill_outlet")
	src = os.path.join(mlib.ASSETS, "models/block", "rotor_fill_outlet.json")
	dst = os.path.join(mlib.ASSETS, "models/block", "fill_outlet.json")
	with open(src) as f:
		text = f.read().replace('"tailings:block/mat/', '"wildcatter:block/mat/')
	with open(dst, "w") as f:
		f.write(text)
	os.remove(src)
	with open(os.path.join(mlib.ASSETS, "blockstates", "fill_outlet.json"), "w") as f:
		json.dump({"variants": {"": {"model": "tailings:block/fill_outlet"}}}, f, indent=1)
	with open(os.path.join(mlib.ASSETS, "items", "fill_outlet.json"), "w") as f:
		json.dump({"model": {"type": "minecraft:model", "model": "tailings:block/fill_outlet"}}, f, indent=1)
