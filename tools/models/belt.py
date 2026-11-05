"""One metre of belt conveyor (docs/multiblocks/crushing-circuit.md): a 600 mm troughed belt on idlers in a 0.8 m frame, its top at
0.75 m. One look, along z; along x the blockstate turns it a quarter turn."""
import json, os
import mlib
import tlib

BC = mlib.Model("belt_conveyor", [(0, 0, 0)], dict(tlib.TEX), tlib.TEX["iron"])
for x in (0.1, 0.84):
	BC.box((x, 0.0, 0.1), (x + 0.06, 0.65, 0.18), "galv")                   # the legs
	BC.box((x, 0.0, 0.82), (x + 0.06, 0.65, 0.9), "galv")
	BC.box((x, 0.55, 0.0), (x + 0.06, 0.65, 1.0), "galv")                   # the stringers
BC.cylinder("x", (0.62, 0.5), 0.05, 0.16, 0.84, "steel", 8)                  # a carrying idler set
BC.box((0.2, 0.67, 0.0), (0.8, 0.72, 1.0), "rubber")                        # the belt's flat
BC.box((0.12, 0.72, 0.0), (0.2, 0.8, 1.0), "rubber")                        # its troughed edges
BC.box((0.8, 0.72, 0.0), (0.88, 0.8, 1.0), "rubber")
BC.box((0.2, 0.15, 0.0), (0.8, 0.18, 1.0), "rubber")                        # the return belt
BC.export_args = {"blockstate": False, "item": True}

MODELS = [BC]


def after():
	path = os.path.join(mlib.ASSETS, "blockstates", "belt_conveyor.json")
	variants = {"axis=z": {"model": "tailings:block/belt_conveyor/p0"}, "axis=x": {"model": "tailings:block/belt_conveyor/p0", "y": 90}}
	with open(path, "w") as f:
		json.dump({"variants": variants}, f, indent=1)
