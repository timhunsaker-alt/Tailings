#!/usr/bin/env python3
"""The open pit's haul truck and wheel loader at their real size (docs/multiblocks/open-pit.md): paints their skins and writes
their model classes, client/HaulTruckModel.java and client/WheelLoaderModel.java.

Boxes are given in metres in the machine's own frame: x across (plus is its right), y up from the ground, z along (minus is the
front). They become entity-model pixels at 16 to the metre: model x is minus x (the renderer mirrors it back), model y is minus y.
A box in a group hangs from that group's hinge, so the renderer can turn it: the truck's body tips about its rear hinge, the loader's
arms lift about their pins. Skins are 1024 x 1024 (a 10 m body is 160 px long, too big for the mine car's 256 sheet).

Wheels are as round as the pixel grid allows: each tyre is a stack of 1/16 m slices, each as long as the circle is wide at its
height, with a steel hub and six wheel nuts on its outer face. Each wheel is its own group on its axle, so the renderer rolls it.

Skins are painted in Ludo's materials (tools/models/skin.py): run python3 tools/process_art.py first.

Run from the repo root: python3 tools/models/pit_vehicles.py
"""
import math
import random

from PIL import Image

from skin import sample

OUT_TEX = "common/src/main/resources/assets/tailings/textures/entity/"
OUT_JAVA = "common/src/main/java/dev/wildcatter/tailings/client/"
SHEET = 1024

YELLOW = (214, 166, 34)
BLACK = (30, 30, 32)
DARK = (56, 58, 62)
STEEL = (96, 100, 106)
ROCK = (92, 84, 74)
GLASS = (44, 68, 82)
RUST = (112, 74, 44)
# each colour's Ludo material (tools/models/skin.py); glass is see-through, so it has none
MATERIAL = {YELLOW: "paint_yellow", BLACK: "rubber_tread", DARK: "steel_dark", STEEL: "steel_worn", ROCK: "rock", RUST: "steel_rust"}

# name, group, (x0, x1), (y0, y1), (z0, z1), colour, pattern
TRUCK_HINGE = (0.0, 2.0, 4.6)
TRUCK = [
	("axle", None, (-1.45, 1.45), (1.1, 1.6), (2.1, 2.6), DARK, "flat"),
	("frame", None, (-0.9, 0.9), (1.3, 2.0), (-4.9, 4.0), DARK, "plate"),
	("hood", None, (-1.1, 1.1), (1.6, 3.0), (-5.0, -2.6), YELLOW, "louvres"),
	("grille", None, (-1.2, 1.2), (1.5, 3.0), (-5.1, -5.0), DARK, "louvres"),
	("bumper", None, (-2.0, 2.0), (1.2, 1.6), (-5.2, -4.9), YELLOW, "hazard"),
	("deck", None, (-3.1, 3.1), (3.0, 3.15), (-5.0, -2.4), DARK, "plate"),
	("cab", None, (-3.0, -1.3), (3.15, 5.0), (-4.6, -2.6), YELLOW, "cab"),
	# the body's floor clears the rear tyres' tops (2.7 m), so no tyre shows through an empty body; it rests on two rails on the frame
	("rail_l", "body", (-1.3, -0.9), (2.0, 2.75), (-2.4, 4.6), DARK, "plate"),
	("rail_r", "body", (0.9, 1.3), (2.0, 2.75), (-2.4, 4.6), DARK, "plate"),
	("floor", "body", (-2.75, 2.75), (2.75, 3.05), (-2.4, 4.9), YELLOW, "plate"),
	("side_l", "body", (-2.75, -2.55), (3.05, 4.6), (-2.4, 4.5), YELLOW, "ribs"),
	("side_r", "body", (2.55, 2.75), (3.05, 4.6), (-2.4, 4.5), YELLOW, "ribs"),
	("front_wall", "body", (-2.75, 2.75), (2.75, 5.0), (-2.6, -2.4), YELLOW, "plate"),
	("canopy", "body", (-3.1, 3.1), (5.0, 5.2), (-5.0, -2.4), YELLOW, "plate"),
	("load", "body", (-2.55, 2.55), (3.05, 4.4), (-2.4, 4.4), ROCK, "rock"),
]

# wheels: name, (x0, x1) across, centre height, centre along, radius, the side its hub faces (-1 left, 1 right, 0 an inner dual)
TRUCK_WHEELS = [
	("wheel_fl", (-3.1, -2.3), 1.35, -2.85, 1.35, -1),
	("wheel_fr", (2.3, 3.1), 1.35, -2.85, 1.35, 1),
	("wheel_rlo", (-3.1, -2.3), 1.35, 2.35, 1.35, -1),
	("wheel_rli", (-2.25, -1.45), 1.35, 2.35, 1.35, 0),
	("wheel_rri", (1.45, 2.25), 1.35, 2.35, 1.35, 0),
	("wheel_rro", (2.3, 3.1), 1.35, 2.35, 1.35, 1),
]

LOADER_HINGE = (0.0, 3.0, -3.8)
LOADER = [
	("front_frame", None, (-1.0, 1.0), (1.0, 2.2), (-4.5, 0.2), DARK, "plate"),
	("rear_frame", None, (-1.2, 1.2), (1.0, 3.4), (0.2, 7.0), YELLOW, "louvres"),
	("counterweight", None, (-1.4, 1.4), (1.0, 2.6), (7.0, 7.5), DARK, "hazard"),
	("fender_f", None, (-2.3, 2.3), (3.0, 3.2), (-4.1, -0.9), YELLOW, "plate"),
	("fender_r", None, (-2.3, 2.3), (3.0, 3.2), (1.4, 4.6), YELLOW, "plate"),
	("cab", None, (-1.1, 1.1), (3.4, 6.0), (-0.6, 1.6), YELLOW, "cab"),
	("boom_l", "arm", (-1.2, -0.8), (1.0, 3.2), (-7.0, -3.6), YELLOW, "plate"),
	("boom_r", "arm", (0.8, 1.2), (1.0, 3.2), (-7.0, -3.6), YELLOW, "plate"),
	("bucket", "arm", (-2.25, 2.25), (0.0, 1.8), (-7.5, -6.0), STEEL, "plate"),
	("bucket_load", "arm", (-2.1, 2.1), (1.3, 2.1), (-7.4, -6.1), ROCK, "rock"),
]


LOADER_WHEELS = [
	("wheel_fl", (-2.25, -1.25), 1.5, -2.5, 1.5, -1),
	("wheel_fr", (1.25, 2.25), 1.5, -2.5, 1.5, 1),
	("wheel_rl", (-2.25, -1.25), 1.5, 3.0, 1.5, -1),
	("wheel_rr", (1.25, 2.25), 1.5, 3.0, 1.5, 1),
]

# The track dozer (open-pit-p4-dry-stack.md): 8.8 x 4.9 x 4.6 m with its semi-U blade. It runs on tracks: each side a tread over an
# idler at the front and a sprocket at the rear (round, turned as it rolls), with its rollers in the track frame between them.
DOZER_HINGE = (0.0, 0.8, -1.0)
DOZER = [
	("track_frame_l", None, (-2.0, -1.3), (0.3, 1.2), (-2.6, 2.6), DARK, "plate"),
	("track_frame_r", None, (1.3, 2.0), (0.3, 1.2), (-2.6, 2.6), DARK, "plate"),
	("tread_top_l", None, (-2.1, -1.2), (1.2, 1.32), (-2.6, 2.6), BLACK, "tread"),
	("tread_top_r", None, (1.2, 2.1), (1.2, 1.32), (-2.6, 2.6), BLACK, "tread"),
	("tread_low_l", None, (-2.1, -1.2), (0.0, 0.14), (-2.6, 2.6), BLACK, "tread"),
	("tread_low_r", None, (1.2, 2.1), (0.0, 0.14), (-2.6, 2.6), BLACK, "tread"),
	("rollers_l", None, (-1.95, -1.35), (0.14, 0.4), (-1.8, 1.8), STEEL, "plate"),
	("rollers_r", None, (1.35, 1.95), (0.14, 0.4), (-1.8, 1.8), STEEL, "plate"),
	("fender_l", None, (-2.4, -1.2), (1.35, 1.5), (-2.9, 2.9), YELLOW, "plate"),
	("fender_r", None, (1.2, 2.4), (1.35, 1.5), (-2.9, 2.9), YELLOW, "plate"),
	("mainframe", None, (-1.2, 1.2), (0.6, 1.4), (-3.4, 3.0), DARK, "plate"),
	("hood", None, (-1.0, 1.0), (1.4, 2.8), (-3.6, -0.6), YELLOW, "louvres"),
	("grille", None, (-0.9, 0.9), (1.5, 2.7), (-3.7, -3.6), DARK, "louvres"),
	("stack", None, (-0.6, -0.4), (2.8, 3.6), (-2.6, -2.4), DARK, "flat"),
	("tank", None, (-1.3, 1.3), (1.4, 2.6), (1.0, 3.0), YELLOW, "plate"),
	("cab", None, (-1.1, 1.1), (1.6, 4.6), (-0.6, 1.0), YELLOW, "cab"),
	("drawbar", None, (-1.0, 1.0), (0.8, 1.6), (3.0, 3.6), DARK, "hazard"),
	("arm_l", "blade", (-2.35, -2.05), (0.6, 1.0), (-3.9, -1.0), DARK, "plate"),
	("arm_r", "blade", (2.05, 2.35), (0.6, 1.0), (-3.9, -1.0), DARK, "plate"),
	("tilt_ram", "blade", (-0.2, 0.2), (1.2, 1.6), (-3.9, -3.0), STEEL, "flat"),
	("blade", "blade", (-2.45, 2.45), (0.15, 2.1), (-4.4, -3.95), YELLOW, "ribs"),
	("edge", "blade", (-2.45, 2.45), (0.0, 0.15), (-4.45, -3.95), STEEL, "plate"),
	("blade_load", "blade", (-2.2, 2.2), (0.2, 1.6), (-5.4, -4.45), ROCK, "rock"),
]

# its idlers (front) and sprockets (rear), the round ends its treads run round
DOZER_WHEELS = [
	("idler_l", (-2.1, -1.2), 0.66, -2.6, 0.66, -1),
	("idler_r", (1.2, 2.1), 0.66, -2.6, 0.66, 1),
	("sprocket_l", (-2.1, -1.2), 0.66, 2.6, 0.66, -1),
	("sprocket_r", (1.2, 2.1), 0.66, 2.6, 0.66, 1),
]


# The 360 t ultra-class truck (open-pit-p5-big-machines.md, P5b): 15.08 m long, 9.53 m over the tyres and 9.76 m outside the body,
# 7.71 m to the canopy, its body's sides 7.0 m up (its loading height), on six 4 m tyres; its body tips about its rear hinge.
ULTRA_HINGE = (0.0, 3.0, 6.3)
ULTRA = [
	("frame", None, (-1.3, 1.3), (1.9, 2.9), (-7.2, 6.3), DARK, "plate"),
	("axle", None, (-3.3, 3.3), (1.6, 2.4), (2.8, 3.6), DARK, "flat"),
	("hood", None, (-1.6, 1.6), (2.4, 4.6), (-7.5, -3.9), YELLOW, "louvres"),
	("grille", None, (-1.7, 1.7), (2.2, 4.6), (-7.62, -7.5), DARK, "louvres"),
	("bumper", None, (-3.0, 3.0), (1.8, 2.4), (-7.7, -7.4), YELLOW, "hazard"),
	("deck", None, (-4.7, 4.7), (4.6, 4.8), (-7.5, -3.9), DARK, "plate"),
	("stairway", None, (1.9, 3.5), (2.0, 4.6), (-7.75, -7.55), DARK, "ribs"),
	("cab", None, (-4.5, -2.2), (4.8, 7.0), (-7.2, -4.4), YELLOW, "cab"),
	# the body's floor clears the rear tyres' tops (4.0 m), so no tyre shows through an empty body; it rests on two rails on the frame
	("rail_l", "body", (-1.6, -1.0), (2.9, 4.05), (-3.7, 6.3), DARK, "plate"),
	("rail_r", "body", (1.0, 1.6), (2.9, 4.05), (-3.7, 6.3), DARK, "plate"),
	("floor", "body", (-4.88, 4.88), (4.05, 4.45), (-3.7, 7.54), YELLOW, "plate"),
	("side_l", "body", (-4.88, -4.6), (4.45, 7.0), (-3.7, 7.2), YELLOW, "ribs"),
	("side_r", "body", (4.6, 4.88), (4.45, 7.0), (-3.7, 7.2), YELLOW, "ribs"),
	("front_wall", "body", (-4.88, 4.88), (4.05, 7.5), (-4.0, -3.7), YELLOW, "plate"),
	("canopy", "body", (-4.88, 4.88), (7.5, 7.71), (-7.54, -3.7), YELLOW, "plate"),
	("load", "body", (-4.6, 4.6), (4.45, 6.8), (-3.7, 7.0), ROCK, "rock"),
]

ULTRA_WHEELS = [
	("wheel_fl", (-4.76, -3.3), 2.0, -4.3, 2.0, -1),
	("wheel_fr", (3.3, 4.76), 2.0, -4.3, 2.0, 1),
	("wheel_rlo", (-4.76, -3.3), 2.0, 3.2, 2.0, -1),
	("wheel_rli", (-3.2, -1.74), 2.0, 3.2, 2.0, 0),
	("wheel_rri", (1.74, 3.2), 2.0, 3.2, 2.0, 0),
	("wheel_rro", (3.3, 4.76), 2.0, 3.2, 2.0, 1),
]


# The 250 t class hydraulic front shovel (open-pit-p5-big-machines.md, P5c): on crawlers 6.0 m wide, its house 8.04 m wide over the
# walkways and 7.25 m to the cab roof; the house swings on its ring, and its front (boom, stick and the 15 m3 bucket) lifts about the
# boom foot.
HYD_HOUSE = (0.0, 1.8, 0.0)
HYD_FRONT = (0.0, 3.0, -2.6)
HYD = [
	("track_frame_l", None, (-3.0, -1.8), (0.4, 1.3), (-3.4, 3.4), DARK, "plate"),
	("track_frame_r", None, (1.8, 3.0), (0.4, 1.3), (-3.4, 3.4), DARK, "plate"),
	("tread_top_l", None, (-3.05, -1.75), (1.3, 1.45), (-3.4, 3.4), BLACK, "tread"),
	("tread_top_r", None, (1.75, 3.05), (1.3, 1.45), (-3.4, 3.4), BLACK, "tread"),
	("tread_low_l", None, (-3.05, -1.75), (0.0, 0.15), (-3.4, 3.4), BLACK, "tread"),
	("tread_low_r", None, (1.75, 3.05), (0.0, 0.15), (-3.4, 3.4), BLACK, "tread"),
	("carbody", None, (-1.8, 1.8), (0.6, 1.6), (-1.8, 1.8), DARK, "plate"),
	("swing_ring", None, (-1.6, 1.6), (1.6, 1.8), (-1.6, 1.6), STEEL, "flat"),
	("deck", "house", (-3.2, 3.2), (1.8, 2.4), (-2.6, 4.6), YELLOW, "plate"),
	("engine_house", "house", (-3.0, 3.0), (2.4, 5.0), (0.0, 4.4), YELLOW, "louvres"),
	("counterweight", "house", (-3.2, 3.2), (1.9, 4.0), (4.4, 5.6), DARK, "hazard"),
	("walkway_l", "house", (-4.02, -3.2), (2.2, 2.35), (-2.4, 4.4), DARK, "plate"),
	("walkway_r", "house", (3.2, 4.02), (2.2, 2.35), (-2.4, 4.4), DARK, "plate"),
	("cab_base", "house", (-3.0, -1.0), (2.4, 4.6), (-2.4, -0.2), YELLOW, "plate"),
	("cab", "house", (-3.0, -1.0), (4.6, 7.25), (-2.4, -0.2), YELLOW, "cab"),
	("boom", "front", (-0.8, 0.8), (3.0, 4.4), (-7.6, -2.4), YELLOW, "plate"),
	("boom_ram_l", "front", (-1.3, -0.9), (2.2, 3.4), (-5.0, -2.4), STEEL, "flat"),
	("boom_ram_r", "front", (0.9, 1.3), (2.2, 3.4), (-5.0, -2.4), STEEL, "flat"),
	("stick", "front", (-0.7, 0.7), (1.6, 5.4), (-8.4, -7.4), YELLOW, "plate"),
	("bucket", "front", (-2.15, 2.15), (0.0, 2.6), (-10.2, -8.0), STEEL, "plate"),
	("bucket_load", "front", (-1.9, 1.9), (2.2, 3.0), (-10.0, -8.2), ROCK, "rock"),
]

# its idlers (front) and sprockets (rear)
HYD_WHEELS = [
	("idler_l", (-3.05, -1.75), 0.72, -3.4, 0.72, -1),
	("idler_r", (1.75, 3.05), 0.72, -3.4, 0.72, 1),
	("sprocket_l", (-3.05, -1.75), 0.72, 3.4, 0.72, -1),
	("sprocket_r", (1.75, 3.05), 0.72, 3.4, 0.72, 1),
]


# The 4100-class electric rope shovel, lifted and running (open-pit-p5-big-machines.md, P5d): two crawlers 14 m across and 12 m long
# under the car body; the house (12 x 15 m, the cab at its front left, the gantry on its roof) swings on the roller circle; the 20 m boom
# stands at 45 degrees from its foot; the dipper handle runs through the saddle block on the boom and is hoisted up and down with the
# dipper on its end. The hoist and suspension ropes are drawn by the renderer, as lines.
ROPE_HOUSE = (0.0, 3.0, 0.0)
ROPE_BOOM = (0.0, 5.0, -6.5)
ROPE_SADDLE = (0.0, 11.36, -12.86)
ROPE = [
	("crawler_l", None, (-7.0, -2.5), (0.4, 2.2), (-5.5, 5.5), DARK, "plate"),
	("crawler_r", None, (2.5, 7.0), (0.4, 2.2), (-5.5, 5.5), DARK, "plate"),
	("shoes_top_l", None, (-7.0, -2.5), (2.2, 2.6), (-5.7, 5.7), BLACK, "tread"),
	("shoes_top_r", None, (2.5, 7.0), (2.2, 2.6), (-5.7, 5.7), BLACK, "tread"),
	("shoes_low_l", None, (-7.0, -2.5), (0.0, 0.4), (-5.7, 5.7), BLACK, "tread"),
	("shoes_low_r", None, (2.5, 7.0), (0.0, 0.4), (-5.7, 5.7), BLACK, "tread"),
	("carbody", None, (-2.5, 2.5), (0.8, 2.6), (-3.0, 3.0), DARK, "plate"),
	("roller_circle", None, (-4.5, 4.5), (2.6, 3.0), (-4.5, 4.5), STEEL, "flat"),
	("frame", "house", (-6.0, 6.0), (3.0, 3.8), (-7.0, 8.0), DARK, "plate"),
	("machinery", "house", (-5.7, 5.7), (3.8, 10.5), (-4.5, 7.3), YELLOW, "louvres"),
	("roof", "house", (-5.9, 5.9), (10.5, 10.8), (-4.7, 7.5), DARK, "plate"),
	("counterweight", "house", (-5.5, 5.5), (3.8, 7.5), (7.3, 8.0), DARK, "hazard"),
	("cab_stand", "house", (-5.7, -2.5), (3.8, 6.5), (-7.0, -4.5), DARK, "plate"),
	("cab", "house", (-5.7, -2.5), (6.5, 9.8), (-7.0, -4.5), YELLOW, "cab"),
	("foot_brackets", "house", (-1.5, 1.5), (3.8, 5.6), (-7.0, -5.8), DARK, "plate"),
	("gantry_leg_l", "house", (-3.0, -2.4), (10.8, 16.8), (-4.0, -3.4), YELLOW, "plate"),
	("gantry_leg_r", "house", (2.4, 3.0), (10.8, 16.8), (-4.0, -3.4), YELLOW, "plate"),
	("gantry_strut_l", "house", (-3.0, -2.4), (10.8, 15.3), (-1.6, -1.0), YELLOW, "plate"),
	("gantry_strut_r", "house", (2.4, 3.0), (10.8, 15.3), (-1.6, -1.0), YELLOW, "plate"),
	("gantry_head", "house", (-3.0, 3.0), (16.2, 16.8), (-4.0, -2.2), YELLOW, "plate"),
	("chord_l", "boom", (-1.3, -0.5), (3.8, 6.2), (-26.5, -6.5), YELLOW, "plate"),
	("chord_r", "boom", (0.5, 1.3), (3.8, 6.2), (-26.5, -6.5), YELLOW, "plate"),
	("bracing", "boom", (-0.5, 0.5), (4.6, 5.4), (-26.5, -6.5), DARK, "ribs"),
	("point_sheaves", "boom", (-1.2, 1.2), (3.7, 6.3), (-28.0, -26.5), STEEL, "flat"),
	("saddle", "dipper", (-1.4, 1.4), (10.3, 12.4), (-13.6, -12.1), DARK, "plate"),
	("handle", "dipper", (-0.8, 0.8), (10.86, 11.86), (-24.9, -10.9), YELLOW, "plate"),
	("dipper", "dipper", (-2.3, 2.3), (9.0, 13.5), (-29.4, -24.9), STEEL, "plate"),
	("teeth", "dipper", (-2.2, 2.2), (8.6, 9.0), (-29.6, -28.8), DARK, "flat"),
	("dipper_load", "dipper", (-2.0, 2.0), (12.5, 13.6), (-29.1, -25.2), ROCK, "rock"),
]

ROPE_WHEELS = [
	("idler_l", (-7.0, -2.5), 1.0, -5.5, 1.0, -1),
	("idler_r", (2.5, 7.0), 1.0, -5.5, 1.0, 1),
	("tumbler_l", (-7.0, -2.5), 1.0, 5.5, 1.0, -1),
	("tumbler_r", (2.5, 7.0), 1.0, 5.5, 1.0, 1),
]


# The walking dragline, lifted and working (dragline.md, P6): its tub 15 m across on the ground; the house (24 x 20 m, 12 m high, the
# cab at its front left) swinging on the tub's roller path, its walking shoes hung from cams on its sides; the A-frame mast on its roof;
# the 75 m lattice boom at 32 degrees from its foot at the house's front, in ten 7.5 m sections; and the 33 m³ bucket, hung from the
# boom point and dragged in by the drag ropes, placed by the renderer. The hoist, drag and suspension ropes are drawn as lines.
DRAG_HOUSE = (0.0, 0.0, 0.0)
DRAG_BOOM = (0.0, 4.4, -8.5)
DRAG_SHOES = (0.0, 2.6, 0.0)
DRAG_BUCKET = (0.0, 0.0, 0.0)
DRAG = [
	("tub_a", None, (-2.8, 2.8), (0.0, 1.6), (-6.85, 6.85), DARK, "plate"),
	("tub_b", None, (-6.85, 6.85), (0.0, 1.6), (-2.8, 2.8), DARK, "plate"),
	("tub_c", None, (-4.8, 4.8), (0.0, 1.6), (-4.8, 4.8), DARK, "plate"),
	("roller_path", None, (-5.0, 5.0), (1.6, 2.2), (-5.0, 5.0), STEEL, "flat"),
	("frame", "house", (-12.0, 12.0), (2.2, 3.4), (-8.5, 11.5), DARK, "plate"),
	("machinery", "house", (-11.5, 11.5), (3.4, 14.6), (-5.5, 11.0), YELLOW, "louvres"),
	("roof", "house", (-11.7, 11.7), (14.6, 15.0), (-5.7, 11.2), DARK, "plate"),
	("cooler_l", "house", (-8.0, -4.0), (15.0, 15.8), (3.0, 7.0), STEEL, "ribs"),
	("cooler_r", "house", (4.0, 8.0), (15.0, 15.8), (3.0, 7.0), STEEL, "ribs"),
	("counterweight", "house", (-11.0, 11.0), (3.4, 8.0), (11.0, 11.5), DARK, "hazard"),
	("cab_stand", "house", (-11.5, -7.5), (3.4, 6.2), (-8.5, -5.5), DARK, "plate"),
	("cab", "house", (-11.5, -7.5), (6.2, 9.7), (-8.5, -5.5), YELLOW, "cab"),
	("fairlead", "house", (-1.2, 1.2), (3.4, 4.8), (-9.0, -7.5), STEEL, "flat"),
	("foot_brackets", "house", (-2.4, 2.4), (3.4, 5.4), (-8.6, -7.4), DARK, "plate"),
	("mast_leg_l", "house", (-3.0, -2.3), (15.0, 35.0), (-7.2, -6.5), YELLOW, "plate"),
	("mast_leg_r", "house", (2.3, 3.0), (15.0, 35.0), (-7.2, -6.5), YELLOW, "plate"),
	("mast_strut_l", "house", (-3.0, -2.3), (15.0, 31.0), (-3.5, -2.8), YELLOW, "plate"),
	("mast_strut_r", "house", (2.3, 3.0), (15.0, 31.0), (-3.5, -2.8), YELLOW, "plate"),
	("mast_tie_l", "house", (-3.0, -2.3), (30.5, 31.0), (-6.5, -3.5), YELLOW, "plate"),
	("mast_tie_r", "house", (2.3, 3.0), (30.5, 31.0), (-6.5, -3.5), YELLOW, "plate"),
	("mast_brace_1", "house", (-2.3, 2.3), (20.0, 20.4), (-7.1, -6.6), DARK, "flat"),
	("mast_brace_2", "house", (-2.3, 2.3), (26.0, 26.4), (-7.1, -6.6), DARK, "flat"),
	("mast_head", "house", (-3.0, 3.0), (34.2, 35.0), (-7.4, -5.4), YELLOW, "plate"),
	("equaliser", "house", (-1.5, 1.5), (33.4, 34.2), (-7.2, -5.8), STEEL, "flat"),
]
for side in (-1, 1):
	x0, x1 = (-10.5, -7.5) if side < 0 else (7.5, 10.5)
	n = "l" if side < 0 else "r"
	DRAG += [
		(f"shoe_pad_{n}", "shoes", (x0, x1), (0.0, 0.5), (-7.0, 7.0), DARK, "plate"),
		(f"shoe_side_{n}", "shoes", (x0 + 0.3, x1 - 0.3), (0.5, 2.0), (-6.0, 6.0), YELLOW, "plate"),
		(f"shoe_cam_{n}", "shoes", (x0 + 0.6, x1 - 0.6), (2.0, 3.0), (-1.5, 1.5), STEEL, "flat"),
	]
# the boom: ten 7.5 m sections, four chords each, and the lacing every 1.5 m; laid out ahead of its foot and turned up 32 degrees
for k in range(10):
	z1, z0 = -8.5 - 7.5 * k, -8.5 - 7.5 * (k + 1)
	for cx, cy, n in ((-2.0, 2.4, "bl"), (1.5, 2.4, "br"), (-2.0, 5.9, "tl"), (1.5, 5.9, "tr")):
		DRAG.append((f"chord_{n}_{k}", "boom", (cx, cx + 0.5), (cy, cy + 0.5), (z0, z1), YELLOW, "plate"))
	for j in range(5):
		z = z0 + 0.65 + 1.5 * j
		DRAG += [
			(f"lace_l_{k}_{j}", "boom", (-1.95, -1.55), (2.9, 5.9), (z, z + 0.2), DARK, "flat"),
			(f"lace_r_{k}_{j}", "boom", (1.55, 1.95), (2.9, 5.9), (z, z + 0.2), DARK, "flat"),
			(f"lace_b_{k}_{j}", "boom", (-1.5, 1.5), (2.45, 2.85), (z, z + 0.2), DARK, "flat"),
			(f"lace_t_{k}_{j}", "boom", (-1.5, 1.5), (5.95, 6.35), (z, z + 0.2), DARK, "flat"),
		]
DRAG += [
	("point_sheaves", "boom", (-1.5, 1.5), (2.0, 6.8), (-85.0, -83.5), STEEL, "flat"),
	# the bucket about its own origin, its mouth toward the machine (plus z), placed by the renderer
	("bucket_floor", "bucket", (-2.2, 2.2), (0.0, 0.4), (-2.8, 2.6), STEEL, "plate"),
	("bucket_side_l", "bucket", (-2.2, -1.8), (0.4, 3.0), (-2.8, 2.6), STEEL, "plate"),
	("bucket_side_r", "bucket", (1.8, 2.2), (0.4, 3.0), (-2.8, 2.6), STEEL, "plate"),
	("bucket_back", "bucket", (-1.8, 1.8), (0.4, 2.6), (-2.8, -2.4), STEEL, "plate"),
	("bucket_lip", "bucket", (-2.2, 2.2), (0.0, 0.6), (2.6, 3.0), DARK, "flat"),
	("bucket_teeth", "bucket", (-2.0, 2.0), (0.05, 0.4), (3.0, 3.4), DARK, "flat"),
	("bucket_arch", "bucket", (-2.2, 2.2), (3.0, 3.6), (2.0, 2.6), YELLOW, "plate"),
	("bucket_chain_l", "bucket", (-2.35, -2.25), (1.2, 3.8), (-0.2, 0.0), DARK, "flat"),
	("bucket_chain_r", "bucket", (2.25, 2.35), (1.2, 3.8), (-0.2, 0.0), DARK, "flat"),
	("spreader", "bucket", (-2.4, 2.4), (3.8, 4.0), (-0.4, 0.2), STEEL, "flat"),
	("bucket_load", "bucket", (-1.8, 1.8), (2.4, 3.2), (-2.4, 2.4), ROCK, "rock"),
]


def disc(radius_px):
	"""A disc's pixel rows about its axle: (the row's top, its half-length) for each row, as round as whole pixels allow."""
	rows = []
	n = int(math.ceil(radius_px))
	for j in range(-n, n):
		mid = j + 0.5
		if abs(mid) < radius_px:
			half = int(round((radius_px * radius_px - mid * mid) ** 0.5))
			if half > 0:
				rows.append((j, half))
	return rows


def face(img, x, y, w, h, colour, pattern, rng, k=1.0):
	"""One face of a box, top left at (x, y) on the skin: its colour's material, shaded by k, with the pattern's details."""
	mat = MATERIAL[colour]
	# on a big face (3 m and more each way) fine stripes and louvres alias into moire at range: there the hazard stripes are 1 m
	# wide, and the louvres are a bank of coarser slats across the face's top third, the rest plain plate with panel seams
	coarse = min(w, h) >= 48
	for j in range(h):
		for i in range(w):
			X, Y = x + i, y + j
			c = sample(mat, X, Y, k)
			edge = i == 0 or j == 0 or i == w - 1 or j == h - 1
			if pattern == "plate" and edge and w > 2 and h > 2:
				c = sample(mat, X, Y, k * 0.78)
			elif pattern == "hazard" and (i + j) % (32 if coarse else 8) < (16 if coarse else 4):
				c = sample("steel_dark", X, Y, 0.55)
			elif pattern == "ribs" and i % 16 == 7:
				c = sample(mat, X, Y, k * 1.18)
			elif pattern == "ribs" and rng.random() < 0.04:
				c = sample("steel_rust", X, Y, k)
			elif pattern == "tread" and (j % 6 < 2):
				c = sample(mat, X, Y, k * 1.6)
			elif pattern == "louvres" and not coarse and j % 4 == 1 and 1 < i < w - 2 and 1 < j < h - 2:
				c = sample("steel_dark", X, Y, 0.35)
			elif pattern == "louvres" and coarse and 4 <= j < h // 3 and j % 8 < 3 and 4 <= i < w - 4:
				c = sample("steel_dark", X, Y, 0.45)
			elif pattern == "louvres" and coarse and (edge or i % 48 == 0):
				c = sample(mat, X, Y, k * 0.78)
			elif pattern == "cab" and h > 8 and 0.15 * h < j < 0.55 * h and 2 < i < w - 3:
				c = None
			# the windows are clear: the cutout drops these pixels, so from the seat the driver sees out
			img.putpixel((X, Y), GLASS + (0,) if c is None else c + (255,))


def px(v):
	return int(round(v * 16))


def paint(boxes, name, hinge, cls, doc, groups, anim, wheels=(), sheet=SHEET, extra="", share=False):
	"""groups: names hung from the one hinge, or (name, hinge, parent) for a group with its own hinge, hung from its parent group
	(listed before it); a group's boxes are given in the machine's frame as any other. share: boxes of the same size, colour and
	pattern draw from one patch of the skin (the dragline's 40 boom chords and 200 lacing bars)."""
	shared = {}
	SHEET = sheet
	info = {}
	for g in groups:
		if isinstance(g, tuple):
			info[g[0]] = (g[1], g[2])
		else:
			info[g] = (hinge, None)
	groups = list(info)
	rng = random.Random(name)
	img = Image.new("RGBA", (SHEET, SHEET), (0, 0, 0, 0))
	u = v = row = 0
	root_lines, group_lines = [], {g: [] for g in groups}
	for part, group, (x0, x1), (y0, y1), (z0, z1), colour, pattern in boxes:
		# model space: x mirrored, y down
		mx0, my0, mz0 = px(-x1), px(-y1), px(z0)
		sx, sy, sz = px(x1 - x0), px(y1 - y0), px(z1 - z0)
		sx, sy, sz = max(sx, 1), max(sy, 1), max(sz, 1)
		w, h = 2 * (sx + sz), sy + sz
		key = (sx, sy, sz, colour, pattern)
		again = share and key in shared
		if again:
			bu, bv = shared[key]
		else:
			if u + w > SHEET:
				u, v, row = 0, v + row + 1, 0
			if v + h > SHEET:
				raise SystemExit(f"{name}: the skin is full at {part}")
			bu, bv = u, v
			shared[key] = (u, v)
		if group:
			gh = info[group][0]
			hx, hy, hz = px(-gh[0]), px(-gh[1]), px(gh[2])
			group_lines[group].append(f'\t\t{group}.addOrReplaceChild("{part}", CubeListBuilder.create().texOffs({bu}, {bv})'
							f'.addBox({mx0 - hx}F, {my0 - hy}F, {mz0 - hz}F, {sx}F, {sy}F, {sz}F), PartPose.ZERO);')
		else:
			root_lines.append(f'\t\troot.addOrReplaceChild("{part}", CubeListBuilder.create().texOffs({bu}, {bv})'
							f'.addBox({mx0}F, {my0}F, {mz0}F, {sx}F, {sy}F, {sz}F), PartPose.ZERO);')
		if again:
			continue
		face(img, u + sz, v, sx, sz, colour, pattern if pattern in ("plate", "rock") else "flat", rng, 1.08)
		face(img, u + sz + sx, v, sx, sz, colour, "flat", rng, 0.7)
		face(img, u, v + sz, sz, sy, colour, pattern, rng, 0.88)
		face(img, u + sz, v + sz, sx, sy, colour, pattern, rng)
		face(img, u + sz + sx, v + sz, sz, sy, colour, pattern, rng, 0.88)
		face(img, u + sz + sx + sz, v + sz, sx, sy, colour, pattern, rng, 0.92)
		u += w + 1
		row = max(row, h)
	# the wheels: a tread region, a hub region and a nut region, each shared by every slice (a slice uses the corner it needs)
	wheel_lines = []
	if wheels:
		biggest = max(px(w[4]) for w in wheels)
		width = max(px(w[1][1] - w[1][0]) for w in wheels)
		regions = {}
		for key, (sx, sy, sz), colour, pattern in (("tread", (width, 1, 2 * biggest), BLACK, "flat"), ("hub", (1, 1, 2 * biggest), STEEL, "flat"),
				("nut", (1, 2, 2), DARK, "flat")):
			w, h = 2 * (sx + sz), sy + sz
			if u + w > SHEET:
				u, v, row = 0, v + row + 1, 0
			# the whole rectangle, solid: a smaller slice lays its faces over other parts of it, which must not be see-through
			face(img, u, v, w, h, colour, "flat", rng, 0.9)
			for fx, fy, fw, fh in ((u + sz, v, sx, sz), (u + sz + sx, v, sx, sz), (u, v + sz, sz, sy), (u + sz, v + sz, sx, sy),
					(u + sz + sx, v + sz, sz, sy), (u + sz + sx + sz, v + sz, sx, sy)):
				face(img, fx, fy, fw, fh, colour, pattern, rng)
			# the tread's lugs across its rolling face, so a turning wheel shows it
			if key == "tread":
				for j in range(0, 2 * biggest, 5):
					for i in range(sx):
						img.putpixel((u + sz + i, v + j), sample(MATERIAL[BLACK], u + sz + i, v + j, 1.9) + (255,))
						img.putpixel((u + sz + i, v + j + 1), sample(MATERIAL[BLACK], u + sz + i, v + j + 1, 1.9) + (255,))
			regions[key] = (u, v)
			u += w + 1
			row = max(row, h)
		for wname, (x0, x1), cy, cz, r, hub_side in wheels:
			wheel_lines.append(f'\t\tPartDefinition {wname} = root.addOrReplaceChild("{wname}", CubeListBuilder.create(), PartPose.offset(0.0F, {-px(cy)}F, {px(cz)}F));')
			mx0, sx = px(-x1), px(x1 - x0)
			tu, tv = regions["tread"]
			for k, (top, half) in enumerate(disc(r * 16)):
				wheel_lines.append(f'\t\t{wname}.addOrReplaceChild("t{k}", CubeListBuilder.create().texOffs({tu}, {tv})'
						f'.addBox({mx0}F, {top}F, {-half}F, {sx}F, 1.0F, {2 * half}F), PartPose.ZERO);')
			if hub_side:
				# its outer face in model x, which is mirrored: a right-hand wheel's outer face is the low model x
				hx = mx0 - 1 if hub_side > 0 else mx0 + sx
				hu, hv = regions["hub"]
				for k, (top, half) in enumerate(disc(r * 16 * 0.45)):
					wheel_lines.append(f'\t\t{wname}.addOrReplaceChild("h{k}", CubeListBuilder.create().texOffs({hu}, {hv})'
							f'.addBox({hx}F, {top}F, {-half}F, 1.0F, 1.0F, {2 * half}F), PartPose.ZERO);')
				nu, nv = regions["nut"]
				nx = hx - 1 if hub_side > 0 else hx + 1
				for k in range(6):
					a = k * math.pi / 3
					ny, nz = int(round(r * 16 * 0.28 * math.sin(a))) - 1, int(round(r * 16 * 0.28 * math.cos(a))) - 1
					wheel_lines.append(f'\t\t{wname}.addOrReplaceChild("n{k}", CubeListBuilder.create().texOffs({nu}, {nv})'
							f'.addBox({nx}F, {ny}F, {nz}F, 1.0F, 2.0F, 2.0F), PartPose.ZERO);')
	img.save(OUT_TEX + name + ".png")   # painted in the Ludo materials (art/final/vehicle)
	body = list(root_lines)
	for g in groups:
		gh, parent = info[g]
		hx, hy, hz = px(-gh[0]), px(-gh[1]), px(gh[2])
		if parent:
			ph = info[parent][0]
			hx, hy, hz = hx - px(-ph[0]), hy - px(-ph[1]), hz - px(ph[2])
		body.append(f'\t\tPartDefinition {g} = {parent or "root"}.addOrReplaceChild("{g}", CubeListBuilder.create(), PartPose.offset({hx}F, {hy}F, {hz}F));')
		body.extend(group_lines[g])
	body.extend(wheel_lines)
	constants = "".join(f'\tpublic static final String {g.upper()} = "{g}";\n' for g in groups)
	java = f'''package dev.wildcatter.tailings.client;

import dev.wildcatter.tailings.Tailings;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/** {doc} Written by tools/models/pit_vehicles.py, with its {SHEET} x {SHEET} skin painted in Ludo's materials: change the script, not this file. */
public class {cls} extends EntityModel<PitVehicleRenderer.PitState> {{
	public static final ModelLayerLocation LAYER = new ModelLayerLocation(Tailings.id("{name}"), "main");
	/** Its wheels, each turning on its own axle as it rolls, and their radius in metres. */
	public static final String[] WHEELS = {{{", ".join(chr(34) + w[0] + chr(34) for w in wheels)}}};
	public static final float WHEEL_RADIUS = {wheels[0][4] if wheels else 1.0}F;
{constants}
	public {cls}(ModelPart root) {{
		super(root, id -> RenderTypes.entityCutout(id));
	}}

	public static LayerDefinition createBodyLayer() {{
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
{chr(10).join(body)}
		return LayerDefinition.create(mesh, {SHEET}, {SHEET});
	}}

{extra}	/** Set when the frame is drawn, from this machine's own state. */
	@Override
	public void setupAnim(PitVehicleRenderer.PitState state) {{
		super.setupAnim(state);
		for (String wheel : WHEELS) {{
			root().getChild(wheel).xRot = state.roll / WHEEL_RADIUS;   // turned through the distance rolled over the radius
		}}
{anim}
	}}
}}
'''
	with open(OUT_JAVA + cls + ".java", "w") as f:
		f.write(java)
	print(f"{cls}: {len(boxes)} boxes, skin rows to v = {v + row}")


def main(names):
	if not names or "haul_truck" in names:
		truck()
	if not names or "ultra_haul_truck" in names:
		ultra()
	if not names or "hydraulic_shovel" in names:
		hydraulic()
	if not names or "rope_shovel" in names:
		rope()
	if not names or "dragline" in names:
		dragline()
	if not names or "wheel_loader" in names:
		loader()
	if not names or "track_dozer" in names:
		dozer()


def dozer():
	paint(DOZER, "track_dozer", DOZER_HINGE, "TrackDozerModel",
		"The 70 t track dozer at its real size, 8.8 x 4.9 x 4.6 m; its blade and push arms rise and fall about their trunnions.", ["blade"],
		"\t\tModelPart blade = root().getChild(BLADE);\n"
		"\t\tblade.y += (0.5F - state.lift) * 24F;   // CUT a block lower than CARRY, RAISED a block and a half higher\n"
		"\t\tblade.getChild(\"blade_load\").visible = state.loaded;", DOZER_WHEELS)


def rope():
	paint(ROPE, "rope_shovel", ROPE_HOUSE, "RopeShovelModel",
		"The 4100-class electric rope shovel at its real size: crawlers 14 x 12 m, the house 12 x 15 m swinging on them, the 20 m boom at 45"
		+ " degrees and the dipper on its handle, hoisted up and down.",
		[("house", ROPE_HOUSE, None), ("boom", ROPE_BOOM, "house"), ("dipper", ROPE_SADDLE, "house")],
		"\t\tModelPart house = root().getChild(HOUSE);\n"
		"\t\thouse.yRot = (float) Math.toRadians(state.swing);   // plus swings its front to its right\n"
		"\t\thouse.getChild(BOOM).xRot = -BOOM_ANGLE;   // the boom stands at 45 degrees\n"
		"\t\tModelPart dipper = house.getChild(DIPPER);\n"
		"\t\tdipper.xRot = handleAngle(state.lift);\n"
		"\t\tdipper.getChild(\"dipper_load\").visible = state.loaded;", ROPE_WHEELS, 2048,
		"\t/** The boom's angle up from level, radians. */\n"
		"\tpublic static final float BOOM_ANGLE = (float) Math.toRadians(45);\n\n"
		"\t/** The dipper handle's angle down from level, radians, for the hoist from 0 (in the face) to 1 (up over a truck). */\n"
		"\tpublic static float handleAngle(float lift) {\n"
		"\t\treturn 0.7F - lift * 0.75F;\n"
		"\t}\n\n")


def dragline():
	# its skin is packed in shelves: the tallest boxes first, or a 75 m boom and a 24 m house do not fit one 2048 sheet
	tall = sorted(DRAG, key=lambda b: -(px(b[3][1] - b[3][0]) + px(b[4][1] - b[4][0])))
	paint(tall, "dragline", DRAG_HOUSE, "DraglineModel",
		"The walking dragline at its real size: its tub 15 m across, the house 24 x 20 m swinging on it with its walking shoes, the mast,"
		+ " the 75 m boom at 32 degrees and the 33 m\u00b3 bucket, placed under the boom by the hoist and drag.",
		[("house", DRAG_HOUSE, None), ("shoes", DRAG_SHOES, "house"), ("boom", DRAG_BOOM, "house"), ("bucket", DRAG_BUCKET, "house")],
		"\t\tModelPart house = root().getChild(HOUSE);\n"
		"\t\thouse.yRot = (float) Math.toRadians(state.swing);   // plus swings its front to its right\n"
		"\t\thouse.getChild(BOOM).xRot = -BOOM_ANGLE;   // the boom stands at 32 degrees\n"
		"\t\tModelPart shoes = house.getChild(SHOES);\n"
		"\t\tshoes.y -= shoeLift(state.walk) * 16F;\n"
		"\t\tshoes.z -= shoeAhead(state.walk) * 16F;\n"
		"\t\tModelPart bucket = house.getChild(BUCKET);\n"
		"\t\tbucket.y = -state.bucketUp * 16F;\n"
		"\t\tbucket.z = -state.bucketOut * 16F;\n"
		"\t\tbucket.getChild(\"bucket_load\").visible = state.loaded;", (), 2048,
		"\t/** The boom's angle up from level, radians: 32 degrees. */\n"
		"\tpublic static final float BOOM_ANGLE = (float) Math.toRadians(32);\n\n"
		"\t/** The shoes' lift off the ground through a step, metres: hung 0.5 m up while it digs, down on the ground while it moves. */\n"
		"\tpublic static float shoeLift(float walk) {\n"
		"\t\treturn walk < 0.25F ? 0.5F * (1 - walk / 0.25F) : walk < 0.75F ? 0F : 0.5F * (walk - 0.75F) / 0.25F;\n"
		"\t}\n\n"
		"\t/** The shoes ahead of the tub through a step, metres: out 2 m and set down, then the tub moves up to them. */\n"
		"\tpublic static float shoeAhead(float walk) {\n"
		"\t\treturn walk < 0.25F ? 2F * walk / 0.25F : walk < 0.75F ? 2F * (1 - (walk - 0.25F) / 0.5F) : 0F;\n"
		"\t}\n\n", share=True)


def hydraulic():
	paint(HYD, "hydraulic_shovel", HYD_HOUSE, "HydraulicShovelModel",
		"The 250 t class hydraulic front shovel at its real size, about 15 x 8.04 x 7.25 m; its house swings and its front lifts.",
		[("house", HYD_HOUSE, None), ("front", HYD_FRONT, "house")],
		"\t\tModelPart house = root().getChild(HOUSE);\n"
		"\t\thouse.yRot = (float) Math.toRadians(state.swing);   // plus swings its front to its right\n"
		"\t\tModelPart front = house.getChild(FRONT);\n"
		"\t\tfront.xRot = -state.lift * 0.6F;   // the boom lifts the bucket out of the face and over a truck\n"
		"\t\tfront.getChild(\"bucket_load\").visible = state.loaded;", HYD_WHEELS, 2048)


def ultra():
	paint(ULTRA, "ultra_haul_truck", ULTRA_HINGE, "UltraHaulTruckModel",
		"The 360 t ultra-class haul truck at its real size, 15.08 x 9.76 x 7.71 m; its body tips about its rear hinge.", ["body"],
		"\t\tModelPart body = root().getChild(BODY);\n"
		"\t\tbody.xRot = -state.lift * 0.87F;   // the body tips to 50 degrees (proposed)\n"
		"\t\tbody.getChild(\"load\").visible = state.loaded;", ULTRA_WHEELS, 2048)


def truck():
	paint(TRUCK, "haul_truck", TRUCK_HINGE, "HaulTruckModel",
		"The 100 t class haul truck at its real size, 10.0 x 6.2 x 5.2 m; its body tips about its rear hinge.", ["body"],
		"\t\tModelPart body = root().getChild(BODY);\n"
		"\t\tbody.xRot = -state.lift * 0.84F;   // the body tips to 48 degrees (proposed)\n"
		"\t\tbody.getChild(\"load\").visible = state.loaded;", TRUCK_WHEELS)


def loader():
	paint(LOADER, "wheel_loader", LOADER_HINGE, "WheelLoaderModel",
		"The wheel loader at its real size, 15 x 4.5 x 6 m; its arms and bucket lift about the boom pins.", ["arm"],
		"\t\tModelPart arm = root().getChild(ARM);\n"
		"\t\tarm.xRot = -state.lift * 0.9F;   // the arms lift the bucket over a truck's side\n"
		"\t\tarm.getChild(\"bucket_load\").visible = state.loaded;", LOADER_WHEELS)


if __name__ == "__main__":
	import sys
	main(sys.argv[1:])
