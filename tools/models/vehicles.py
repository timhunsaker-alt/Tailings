#!/usr/bin/env python3
"""The mine locomotive and car at their real length: paints both 256 x 256 skins and prints the addBox lines for the model classes.

Pixel space as Blockbench exports it: y runs down from the root, the wheels' bottoms at y = 24 are on the rail, the front is -z.
16 px are 1 m, so the locomotive's 48 px body is 3 m and the car's 32 px body is 2 m (haulage.md).

Run from the repo root: python3 tools/models/vehicles.py
"""
import random

from PIL import Image

OUT = "common/src/main/resources/assets/tailings/textures/entity/"

YELLOW = (205, 158, 32)
BLACK = (34, 34, 36)
DARK = (58, 60, 64)
STEEL = (92, 96, 102)
RUST = (108, 72, 44)
ROCK = (86, 80, 72)
GLASS = (46, 70, 84)
LAMP = (255, 244, 190)

# name, x0, y0, z0, sx, sy, sz, colour, pattern
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


def shade(colour, rng, spread=7):
    d = rng.randint(-spread, spread)
    return tuple(max(0, min(255, c + d)) for c in colour)


def scale(colour, k):
    return tuple(int(c * k) for c in colour)


def face(img, x, y, w, h, colour, pattern, rng):
    """One face of a box. Its top left is (x, y) in the skin."""
    for j in range(h):
        for i in range(w):
            c = shade(colour, rng, 3)
            if pattern == "plate" and (i == 0 or j == 0 or i == w - 1 or j == h - 1) and w > 2 and h > 2:
                c = scale(colour, 0.78)
            elif pattern == "hazard" and (i + j) % 6 < 3:
                c = BLACK
            elif pattern == "ribs" and i % 8 == 3:
                c = scale(colour, 1.2)
            elif pattern == "ribs" and rng.random() < 0.06:
                c = RUST
            elif pattern == "rock":
                c = shade(colour if rng.random() > 0.1 else RUST, rng, 18)
            elif pattern == "louvres" and j % 3 == 1 and 1 < i < w - 2 and 1 < j < h - 2:
                c = BLACK
            elif pattern == "cab" and 2 < j < 9 and 2 < i < w - 3 and i % 8 != 7:
                c = GLASS if h > 2 else c
            img.putpixel((x + i, y + j), c)


def paint(boxes, name):
    rng = random.Random(name)
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    u = v = row = 0
    lines = []
    for part, x, y, z, sx, sy, sz, colour, pattern in boxes:
        w, h = 2 * (sx + sz), sy + sz
        if u + w > 256:
            u, v, row = 0, v + row + 1, 0
        # the box-UV cross: top and bottom over the four sides
        face(img, u + sz, v, sx, sz, scale(colour, 1.08), pattern if pattern in ("plate", "rock") else "flat", rng)
        face(img, u + sz + sx, v, sx, sz, scale(colour, 0.7), "flat", rng)
        face(img, u, v + sz, sz, sy, scale(colour, 0.88), pattern, rng)
        face(img, u + sz, v + sz, sx, sy, colour, pattern if pattern != "louvres" else "plate", rng)
        face(img, u + sz + sx, v + sz, sz, sy, scale(colour, 0.88), pattern, rng)
        face(img, u + sz + sx + sz, v + sz, sx, sy, scale(colour, 0.92), pattern if pattern != "louvres" else "plate", rng)
        if part.startswith("wheel_"):
            # as round as 4 px allows: the whole rectangle solid (the slices use parts of it), then four slices, 2-4-4-2 long
            face(img, u, v, w, h, colour, "flat", rng)
            boxes_ = "".join(f".addBox({x}F, {y + j}F, {z + (sz - n) // 2}F, {sx}F, 1F, {n}F)" for j, n in enumerate((2, 4, 4, 2)))
            lines.append(f'root.addOrReplaceChild("{part}", CubeListBuilder.create().texOffs({u}, {v}){boxes_}, PartPose.ZERO);')
        else:
            lines.append(f'root.addOrReplaceChild({part!r}, CubeListBuilder.create().texOffs({u}, {v})'
                         f'.addBox({x}F, {y}F, {z}F, {sx}F, {sy}F, {sz}F), PartPose.ZERO);'.replace("'", '"'))
        u += w + 1
        row = max(row, h)
    img.save(OUT + name + "_placeholder.png")   # drawn by script: a placeholder until the art comes from Ludo
    print(f"// {name}")
    print("\n".join(lines))


if __name__ == "__main__":
    paint(LOCOMOTIVE, "mine_locomotive")
    paint(CAR, "mine_car")
