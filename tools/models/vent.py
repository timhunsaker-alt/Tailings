"""The drive's ventilation at true size (docs/multiblocks/ventilation.md, L1), each drawn facing north from its own main cell:
- vent_fan, 3 x 3 x 5: a 1.8 m axial forcing fan on its skid, the inlet bell and screen at the north end, the lagged silencer, the fan
  casing with its flanges and the diffuser, and the outlet spigot on the south face where the duct joins; the motor box on the west face;
- booster_fan, 2 x 2 x 3: an in-line booster, its casing between two duct spigots, in at the north, out at the south;
- vent_duct_core and vent_duct_arm: the duct block's multipart pieces (a hanger collar and half a metre of round yellow duct toward
  north), turned by its blockstate as Wildcatter's pipe is.
"""
import json
import os

import mlib
import tlib
from tlib import model, motor_box

TEX = tlib.TEX
TEX["duct"] = "block/vent_duct"

# ---- the ventilation fan ----------------------------------------------------------------------------------------------------------
VF = model("vent_fan", 3, 3, 5, "grey")
VF.textures["duct"] = TEX["duct"]
VF.box((0.3, 0.0, 0.2), (2.7, 0.25, 4.8), "iron")                            # the skid
for z in (0.9, 2.4, 3.9):
	VF.box((0.6, 0.25, z), (2.4, 0.6, z + 0.25), "iron")                     # the cradles
VF.ring("z", (1.5, 1.5), 1.4, 0.0, 0.25, 0.1, "galv", sides=16)             # the inlet bell's lip
VF.ring("z", (1.5, 1.5), 1.2, 0.25, 0.6, 0.1, "galv", sides=16)             # the bell
VF.cylinder("z", (1.5, 1.5), 1.1, 0.05, 0.09, "grating", 16)                # the inlet screen
VF.fan("z", (1.5, 1.5), 0.85, 0.75, 0.06, "iron", blades=8)                 # the impeller, seen through the screen
VF.cylinder("z", (1.5, 1.5), 1.1, 0.6, 2.1, "lagging", 16)                  # the silencer
VF.flange("z", (1.5, 1.5), 1.15, 2.1, 0.08, "grey", bolts=12)
VF.cylinder("z", (1.5, 1.5), 0.95, 2.1, 3.5, "grey", 16)                    # the 1.8 m fan casing
VF.flange("z", (1.5, 1.5), 1.0, 3.5, 0.08, "grey", bolts=12)
VF.cylinder("z", (1.5, 1.5), 0.85, 3.5, 4.5, "grey", 16)                    # the diffuser
VF.cylinder("z", (1.5, 1.5), 0.48, 4.5, 5.0, "duct", 12)                    # the outlet spigot, where the duct joins
VF.decal((1.2, 2.45, 2.5), (1.8, 2.46, 3.1), "ratingplate", "up", "grey")   # its rating plate on top
VF.pipe([(0.3, 0.8, 0.55), (0.55, 0.8, 0.55), (0.55, 0.8, 2.6), (0.6, 0.8, 2.6)], 1, "rubber")   # the motor leads from the box into the casing
motor_box(VF, 0.5, 0.3, 0.6, 0.5)
VF.collide((0.3, 0.0, 0.0), (2.7, 2.7, 5.0))

# ---- the booster fan --------------------------------------------------------------------------------------------------------------
BF = model("booster_fan", 2, 2, 3, "grey")
BF.textures["duct"] = TEX["duct"]
BF.box((0.2, 0.0, 0.3), (1.8, 0.2, 2.7), "iron")                            # the skid
BF.box((0.5, 0.2, 0.8), (1.5, 0.35, 2.2), "iron")
BF.cylinder("z", (1.0, 1.0), 0.48, 0.0, 0.4, "duct", 12)                    # duct in, the north
BF.flange("z", (1.0, 1.0), 0.8, 0.4, 0.06, "grey", bolts=8)
BF.cylinder("z", (1.0, 1.0), 0.75, 0.4, 2.6, "grey", 16)                    # the casing
BF.fan("z", (1.0, 1.0), 0.65, 0.38, 0.04, "iron", blades=6)
BF.flange("z", (1.0, 1.0), 0.8, 2.6, 0.06, "grey", bolts=8)
BF.cylinder("z", (1.0, 1.0), 0.48, 2.6, 3.0, "duct", 12)                    # duct out, the south
motor_box(BF, 0.3, 0.3, 0.45, 0.4)
BF.collide((0.2, 0.0, 0.0), (1.8, 1.75, 3.0))

MODELS = [VF, BF]


def _single(name, build):
	"""A one-block model for a multipart piece, in this mod's own models/block."""
	m = mlib.Model(name, mlib.footprint_box(1, 1, 1, 0, 0), {"duct": TEX["duct"], "galv": TEX["galv"]}, TEX["duct"])
	build(m)
	m.export_single(name)
	src = os.path.join(mlib.ASSETS, "models/block", f"rotor_{name}.json")
	dst = os.path.join(mlib.ASSETS, "models/block", f"{name}.json")
	with open(src) as f:
		text = f.read().replace('"tailings:block/mat/', '"wildcatter:block/mat/')   # Wildcatter's materials ship in its jar
	with open(dst, "w") as f:
		f.write(text)
	os.remove(src)


def after():
	_single("vent_duct_arm", lambda m: m.cylinder("z", (0.5, 0.5), 0.47, 0.0, 0.5, "duct", 12, caps=False))
	def core(m):
		m.ring("z", (0.5, 0.5), 0.49, 0.45, 0.55, 0.04, "galv", sides=12)  # the hanger band
		m.cylinder("y", (0.5, 0.5), 0.03, 0.9, 1.0, "galv", 6)               # its wire up to the crown messenger
	_single("vent_duct_core", core)
	arms = [("north", {}), ("east", {"y": 90}), ("south", {"y": 180}), ("west", {"y": 270}), ("up", {"x": 270}), ("down", {"x": 90})]
	parts = [{"apply": {"model": "tailings:block/vent_duct_core"}}]
	for side, rot in arms:
		parts.append({"when": {side: "true"}, "apply": dict({"model": "tailings:block/vent_duct_arm"}, **rot)})
	with open(os.path.join(mlib.ASSETS, "blockstates", "vent_duct.json"), "w") as f:
		json.dump({"multipart": parts}, f, indent=1)
	# the item: a straight metre of duct along north-south
	with open(os.path.join(mlib.ASSETS, "models/block", "vent_duct_arm.json")) as f:
		arm = json.load(f)
	elements = list(arm["elements"])
	for e in arm["elements"]:
		e2 = json.loads(json.dumps(e))
		e2["from"][2], e2["to"][2] = 16 - e["to"][2], 16 - e["from"][2]
		if "rotation" in e2:
			e2["rotation"]["origin"][2] = 16 - e["rotation"]["origin"][2]
		elements.append(e2)
	item = {"parent": "minecraft:block/block", "textures": arm["textures"], "elements": elements}
	with open(os.path.join(mlib.ASSETS, "models/item", "vent_duct.json"), "w") as f:
		json.dump(item, f, indent=1)
	with open(os.path.join(mlib.ASSETS, "items", "vent_duct.json"), "w") as f:
		json.dump({"model": {"type": "minecraft:model", "model": "tailings:item/vent_duct"}}, f, indent=1)
