#!/usr/bin/env python3
"""The mine locomotives and cars at their real length: paints the 256 x 256 skins and prints the addBox lines for the model classes:
the trolley locomotive and the Granby car (haulage.md), the battery locomotive (battery-locomotive.md, L2) and the bottom-dump car
(bottom-dump-cars.md, L3).

Pixel space as Blockbench exports it: y runs down from the root, the wheels' bottoms at y = 24 are on the rail, the front is -z.
16 px are 1 m, so the locomotive's 48 px body is 3 m and the car's 32 px body is 2 m (haulage.md).

Skins are painted in Ludo's materials (tools/models/skin.py): run python3 tools/process_art.py first.

Run from the repo root: python3 tools/models/vehicles.py
"""
import random

from PIL import Image

from skin import sample

OUT = "common/src/main/resources/assets/tailings/textures/entity/"

YELLOW = (205, 158, 32)
BLACK = (34, 34, 36)
DARK = (58, 60, 64)
STEEL = (92, 96, 102)
RUST = (108, 72, 44)
ROCK = (86, 80, 72)
GLASS = (46, 70, 84)
LAMP = (255, 244, 190)
WORN = (120, 122, 118)
PLUG = (168, 40, 36)
# each colour's Ludo material and its shade (tools/models/skin.py): rail wheels, roof and couplers are blackened steel, the car's
# sides rusty plate; the lamp and the cab glass stay flat colours
MATERIAL = {YELLOW: ("paint_yellow", 1.0), BLACK: ("steel_dark", 0.6), DARK: ("steel_dark", 1.0), STEEL: ("steel_rust", 1.0),
            RUST: ("steel_rust", 0.8), ROCK: ("rock", 1.0), WORN: ("steel_worn", 0.85)}

# name, x0, y0, z0, sx, sy, sz, colour, pattern[, pivot]: a part with a pivot (x, y, z) turns about it (the doors), its box
# written relative to it
LOCOMOTIVE = [
    ("frame", -7, 19, -24, 14, 3, 48, DARK, "plate"),
    ("hood", -6, 9, -22, 12, 10, 28, YELLOW, "louvres"),
    ("cab", -7, 3, 6, 14, 16, 16, YELLOW, "cab"),
    ("roof", -8, 2, 5, 16, 1, 18, BLACK, "plate"),
    ("headlight", -2, 11, -23, 4, 3, 1, LAMP, "flat"),
    ("bumper_f", -7, 20, -25, 14, 2, 1, YELLOW, "hazard"),
    ("bumper_r", -7, 20, 24, 14, 2, 1, YELLOW, "hazard"),
    ("coupler", -1, 20, 25, 2, 2, 3, BLACK, "flat"),
    ("pinion", -1, 21, -3, 2, 3, 6, STEEL, "flat"),
    ("shoe", 7, 21, -2, 2, 2, 4, STEEL, "flat"),
    ("wheel_fl", -7, 20, -18, 1, 4, 4, BLACK, "flat"),
    ("wheel_fr", 6, 20, -18, 1, 4, 4, BLACK, "flat"),
    ("wheel_rl", -7, 20, 14, 1, 4, 4, BLACK, "flat"),
    ("wheel_rr", 6, 20, 14, 1, 4, 4, BLACK, "flat"),
]
CAR = [
    ("frame", -6, 20, -16, 12, 2, 32, BLACK, "plate"),
    ("floor", -8, 19, -16, 16, 1, 32, DARK, "plate"),
    ("side_l", -8, 5, -16, 1, 14, 32, STEEL, "ribs"),
    ("side_r", 7, 5, -16, 1, 14, 32, STEEL, "ribs"),
    ("end_f", -7, 5, -16, 14, 14, 1, STEEL, "plate"),
    ("end_b", -7, 5, 15, 14, 14, 1, STEEL, "plate"),
    ("load", -7, 6, -15, 14, 13, 30, ROCK, "rock"),
    ("coupler_f", -1, 20, -20, 2, 2, 4, BLACK, "flat"),
    ("coupler_b", -1, 20, 16, 2, 2, 4, BLACK, "flat"),
    ("dump_wheel", 8, 17, -3, 1, 3, 6, DARK, "flat"),
    ("wheel_fl", -7, 20, -13, 1, 4, 4, BLACK, "flat"),
    ("wheel_fr", 6, 20, -13, 1, 4, 4, BLACK, "flat"),
    ("wheel_rl", -7, 20, 9, 1, 4, 4, BLACK, "flat"),
    ("wheel_rr", 6, 20, 9, 1, 4, 4, BLACK, "flat"),
]

# the 8 t battery locomotive (L2): the trolley locomotive's frame, wheels, pinion and cab, a short hood for the drive and controller
# over its front, and the traction battery's steel box in the middle with its lid, lifting lugs and charging socket; no collector shoe
BATTERY_LOCOMOTIVE = [
    ("frame", -7, 19, -24, 14, 3, 48, DARK, "plate"),
    ("hood", -6, 11, -22, 12, 8, 8, YELLOW, "louvres"),
    ("battery", -7, 8, -14, 14, 11, 20, WORN, "ribs"),
    ("lid", -7, 7, -14, 14, 1, 20, BLACK, "plate"),
    ("lug_f", -1, 5, -12, 2, 2, 1, STEEL, "flat"),
    ("lug_r", -1, 5, 3, 2, 2, 1, STEEL, "flat"),
    ("socket", 7, 12, -6, 1, 3, 3, PLUG, "flat"),
    ("cab", -7, 3, 6, 14, 16, 16, YELLOW, "cab"),
    ("roof", -8, 2, 5, 16, 1, 18, BLACK, "plate"),
    ("headlight", -2, 13, -23, 4, 3, 1, LAMP, "flat"),
    ("bumper_f", -7, 20, -25, 14, 2, 1, YELLOW, "hazard"),
    ("bumper_r", -7, 20, 24, 14, 2, 1, YELLOW, "hazard"),
    ("coupler", -1, 20, 25, 2, 2, 3, BLACK, "flat"),
    ("pinion", -1, 21, -3, 2, 3, 6, STEEL, "flat"),
    ("wheel_fl", -7, 20, -18, 1, 4, 4, BLACK, "flat"),
    ("wheel_fr", 6, 20, -18, 1, 4, 4, BLACK, "flat"),
    ("wheel_rl", -7, 20, 14, 1, 4, 4, BLACK, "flat"),
    ("wheel_rr", 6, 20, 14, 1, 4, 4, BLACK, "flat"),
]
# the bottom-dump car (L3): the Granby car's body on two side sills, its floor two drop-bottom doors hinged along the sides that
# swing down to let the load fall through the rails, and a cam roller on its side for the cam rail that opens them
BOTTOM_DUMP_CAR = [
    ("sill_l", -8, 20, -16, 2, 2, 32, BLACK, "plate"),
    ("sill_r", 6, 20, -16, 2, 2, 32, BLACK, "plate"),
    ("side_l", -8, 5, -16, 1, 14, 32, STEEL, "ribs"),
    ("side_r", 7, 5, -16, 1, 14, 32, STEEL, "ribs"),
    ("end_f", -7, 5, -16, 14, 15, 1, STEEL, "plate"),
    ("end_b", -7, 5, 15, 14, 15, 1, STEEL, "plate"),
    ("door_l", 0, -0.5, -15, 7, 1, 30, DARK, "plate", (-7, 19.5, 0)),
    ("door_r", -7, -0.5, -15, 7, 1, 30, DARK, "plate", (7, 19.5, 0)),
    ("load", -7, 6, -15, 14, 13, 30, ROCK, "rock"),
    ("coupler_f", -1, 20, -20, 2, 2, 4, BLACK, "flat"),
    ("coupler_b", -1, 20, 16, 2, 2, 4, BLACK, "flat"),
    ("cam_roller", 8, 18, -2, 1, 2, 4, DARK, "flat"),
    ("wheel_fl", -7, 20, -13, 1, 4, 4, BLACK, "flat"),
    ("wheel_fr", 6, 20, -13, 1, 4, 4, BLACK, "flat"),
    ("wheel_rl", -7, 20, 9, 1, 4, 4, BLACK, "flat"),
    ("wheel_rr", 6, 20, 9, 1, 4, 4, BLACK, "flat"),
]


def face(img, x, y, w, h, colour, pattern, rng, k=1.0):
    """One face of a box, top left at (x, y) on the skin: its colour's material, shaded by k, with the pattern's details."""
    mat, base = MATERIAL.get(colour, (None, 1.0))

    def at(i, j, kk, m=None):
        if (m or mat) is None:
            return tuple(min(255, int(c * kk)) for c in colour)
        return sample(m or mat, x + i, y + j, (base if m is None else 1.0) * kk)

    for j in range(h):
        for i in range(w):
            c = at(i, j, k)
            if pattern == "plate" and (i == 0 or j == 0 or i == w - 1 or j == h - 1) and w > 2 and h > 2:
                c = at(i, j, k * 0.78)
            elif pattern == "hazard" and (i + j) % 6 < 3:
                c = at(i, j, 0.55, "steel_dark")
            elif pattern == "ribs" and i % 8 == 3:
                c = at(i, j, k * 1.2)
            elif pattern == "ribs" and rng.random() < 0.06:
                c = at(i, j, k * 0.8, "steel_rust")
            elif pattern == "louvres" and j % 3 == 1 and 1 < i < w - 2 and 1 < j < h - 2:
                c = at(i, j, 0.35, "steel_dark")
            elif pattern == "cab" and 2 < j < 9 and 2 < i < w - 3 and i % 8 != 7 and h > 2:
                c = GLASS
            img.putpixel((x + i, y + j), c)


def paint(boxes, name):
    rng = random.Random(name)
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    u = v = row = 0
    lines = []
    for part, x, y, z, sx, sy, sz, colour, pattern, *pivot in boxes:
        sx, sy, sz = int(sx), int(sy), int(sz)
        w, h = 2 * (sx + sz), sy + sz
        if u + w > 256:
            u, v, row = 0, v + row + 1, 0
        # the box-UV cross: top and bottom over the four sides
        face(img, u + sz, v, sx, sz, colour, pattern if pattern in ("plate", "rock") else "flat", rng, 1.08)
        face(img, u + sz + sx, v, sx, sz, colour, "flat", rng, 0.7)
        face(img, u, v + sz, sz, sy, colour, pattern, rng, 0.88)
        face(img, u + sz, v + sz, sx, sy, colour, pattern if pattern != "louvres" else "plate", rng)
        face(img, u + sz + sx, v + sz, sz, sy, colour, pattern, rng, 0.88)
        face(img, u + sz + sx + sz, v + sz, sx, sy, colour, pattern if pattern != "louvres" else "plate", rng, 0.92)
        if part.startswith("wheel_"):
            # as round as 4 px allows: the whole rectangle solid (the slices use parts of it), then four slices, 2-4-4-2 long
            face(img, u, v, w, h, colour, "flat", rng)
            boxes_ = "".join(f".addBox({x}F, {y + j}F, {z + (sz - n) // 2}F, {sx}F, 1F, {n}F)" for j, n in enumerate((2, 4, 4, 2)))
            lines.append(f'root.addOrReplaceChild("{part}", CubeListBuilder.create().texOffs({u}, {v}){boxes_}, PartPose.ZERO);')
        else:
            pose = f"PartPose.offset({pivot[0][0]}F, {pivot[0][1]}F, {pivot[0][2]}F)" if pivot else "PartPose.ZERO"
            lines.append(f'root.addOrReplaceChild({part!r}, CubeListBuilder.create().texOffs({u}, {v})'
                         f'.addBox({x}F, {y}F, {z}F, {sx}F, {sy}F, {sz}F), {pose});'.replace("'", '"'))
        u += w + 1
        row = max(row, h)
    img.save(OUT + name + ".png")   # painted in the Ludo materials (art/final/vehicle)
    print(f"// {name}")
    print("\n".join(lines))


if __name__ == "__main__":
    paint(LOCOMOTIVE, "mine_locomotive")
    paint(CAR, "mine_car")
    paint(BATTERY_LOCOMOTIVE, "battery_locomotive")
    paint(BOTTOM_DUMP_CAR, "bottom_dump_car")
