#!/usr/bin/env python3
"""The open pit's P3 machines at their real size (docs/multiblocks/open-pit-p3-blasting.md), with pit_vehicles.py's painter: the rotary
blasthole drill (client/BlastholeDrillModel.java) and the bulk emulsion truck (client/EmulsionTruckModel.java), with their
placeholder skins. Frames as in pit_vehicles.py: x across (plus is its right), y up, z along (minus is the front).

The drill's mast stands at its back end over the hole (1.5 m in from the back, z = 6); its rotary head runs down the mast as it
drills. The truck's hose hangs from the reel at its back while it pumps.

Run from the repo root: python3 tools/models/pit_blast_vehicles.py
"""
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import pit_vehicles  # noqa: E402
from pit_vehicles import paint, YELLOW, BLACK, DARK, STEEL  # noqa: E402

WHITE = (222, 222, 216)
RED = (168, 42, 38)

DRILL_HEAD = (0.0, 23.0, 6.0)
DRILL = [
	("mast", None, (-0.9, 0.9), (3.2, 26.0), (6.6, 7.2), YELLOW, "ribs"),
	("mast_l", None, (-0.9, -0.7), (3.2, 26.0), (4.8, 6.6), YELLOW, "plate"),
	("mast_r", None, (0.7, 0.9), (3.2, 26.0), (4.8, 6.6), YELLOW, "plate"),
	("pipe", None, (-0.15, 0.15), (0.0, 22.5), (5.85, 6.15), STEEL, "flat"),
	("track_l", None, (-3.5, -2.2), (0.0, 1.6), (-5.5, 4.5), BLACK, "tread"),
	("track_r", None, (2.2, 3.5), (0.0, 1.6), (-5.5, 4.5), BLACK, "tread"),
	("carbody", None, (-2.2, 2.2), (0.5, 1.6), (-4.0, 3.0), DARK, "plate"),
	("deck", None, (-3.5, 3.5), (1.6, 2.2), (-7.5, 7.5), YELLOW, "plate"),
	("house", None, (-3.3, 1.0), (2.2, 5.2), (-6.0, 2.5), YELLOW, "louvres"),
	("cab", None, (1.3, 3.4), (2.2, 5.6), (-7.4, -4.6), YELLOW, "cab"),
	("compressor", None, (-3.3, 0.8), (2.2, 4.0), (2.8, 4.5), DARK, "louvres"),
	("jack_f", None, (-0.4, 0.4), (0.0, 1.6), (-7.3, -6.6), STEEL, "plate"),
	("jack_rl", None, (-3.3, -2.6), (0.0, 1.6), (6.6, 7.3), STEEL, "plate"),
	("jack_rr", None, (2.6, 3.3), (0.0, 1.6), (6.6, 7.3), STEEL, "plate"),
	("mast_table", None, (-1.5, 1.5), (2.2, 3.2), (4.6, 7.4), DARK, "plate"),
	("crown", None, (-1.0, 1.0), (25.5, 26.0), (4.7, 7.3), DARK, "plate"),
	("dust", None, (1.0, 2.6), (2.2, 5.2), (3.0, 4.5), DARK, "plate"),
	("skirt", None, (-1.2, 1.2), (0.6, 1.6), (5.0, 7.0), BLACK, "flat"),
	("head", "head", (-0.6, 0.6), (22.5, 24.5), (5.4, 6.6), DARK, "plate"),
]

TRUCK = [
	("chassis", None, (-0.55, 0.55), (0.55, 1.0), (-4.1, 4.1), DARK, "plate"),
	("cab", None, (-1.25, 1.25), (1.0, 3.3), (-4.25, -2.3), WHITE, "cab"),
	("bumper", None, (-1.25, 1.25), (0.5, 0.8), (-4.3, -4.2), DARK, "hazard"),
	("tank", None, (-1.15, 1.15), (1.1, 3.1), (-2.1, 2.5), WHITE, "plate"),
	("stripe", None, (-1.17, 1.17), (1.9, 2.2), (-2.05, 2.45), RED, "flat"),
	("pump", None, (-1.1, 1.1), (1.0, 1.6), (2.6, 3.3), DARK, "louvres"),
	("reel", None, (-0.9, 0.9), (1.6, 3.0), (2.8, 4.2), RED, "plate"),
	("ladder", None, (1.15, 1.25), (1.0, 3.1), (1.6, 2.0), STEEL, "ribs"),
	("hose", "hose", (-0.08, 0.08), (0.1, 1.9), (4.2, 4.4), BLACK, "flat"),
]

TRUCK_WHEELS = [
	("wheel_fl", (-1.25, -0.9), 0.55, -3.1, 0.55, -1),
	("wheel_fr", (0.9, 1.25), 0.55, -3.1, 0.55, 1),
	("wheel_rl", (-1.25, -0.9), 0.55, 1.4, 0.55, -1),
	("wheel_rr", (0.9, 1.25), 0.55, 1.4, 0.55, 1),
	("wheel_tl", (-1.25, -0.9), 0.55, 2.6, 0.55, -1),
	("wheel_tr", (0.9, 1.25), 0.55, 2.6, 0.55, 1),
]

if __name__ == "__main__":
	pit_vehicles.SHEET = 2048   # a 26 m mast and a 15 m deck do not fit the trucks' 1024 sheet
	paint(DRILL, "blasthole_drill", DRILL_HEAD, "BlastholeDrillModel",
		"The rotary blasthole drill at its real size, 7 x 15 m on its crawlers and 26 m to the mast top; its rotary head runs down the mast as it drills.",
		["head"],
		"\t\tModelPart head = root().getChild(HEAD);\n"
		"\t\thead.y = -368F + state.lift * 256F;   // the head comes down 16 m as the hole deepens", ())
	pit_vehicles.SHEET = 1024
	paint(TRUCK, "emulsion_truck", (0.0, 1.9, 4.3), "EmulsionTruckModel",
		"The bulk emulsion truck at its real size, 8.5 x 2.5 x 3.8 m; its hose hangs from the reel while it pumps.", ["hose"],
		"\t\troot().getChild(HOSE).visible = state.lift > 0;", TRUCK_WHEELS)
