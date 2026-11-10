#!/usr/bin/env python3
"""The open pit's P5 item icons (docs/multiblocks/open-pit-p5-big-machines.md), drawn by script as placeholders until the art comes,
and their item definitions and models. Every texture is *_placeholder (PROGRESS.md, Known gaps).

- trailing_cable: a reel of black mining cable on a steel drum, its coupler hanging off;
- ultra_haul_truck: the 360 t truck from the side, yellow over big black tyres;
- hydraulic_shovel: the front shovel on its crawlers, boom up, bucket out.

Run from the repo root: python3 tools/models/p5_assets.py
"""
import json
import os
import random

from PIL import Image

ROOT = "common/src/main/resources/assets/tailings/"
YELLOW = (214, 166, 34)
DARK = (40, 40, 44)
STEEL = (120, 124, 130)
BLACK = (24, 24, 26)
ORANGE = (220, 120, 30)


def save(img, path):
	os.makedirs(os.path.dirname(ROOT + path), exist_ok=True)
	img.save(ROOT + path)


def put(img, x, y, c, rng, spread=10):
	if 0 <= x < img.width and 0 <= y < img.height:
		d = rng.randint(-spread, spread)
		img.putpixel((x, y), tuple(max(0, min(255, v + d)) for v in c) + (255,))


def rect(img, x0, y0, x1, y1, c, rng):
	for y in range(y0, y1 + 1):
		for x in range(x0, x1 + 1):
			put(img, x, y, c, rng)


def disc(img, cx, cy, r, c, rng):
	for y in range(img.height):
		for x in range(img.width):
			if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
				put(img, x, y, c, rng)


def item(name):
	save_json(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"tailings:item/{name}"}})
	save_json(f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"tailings:item/{name}_placeholder"}})


def save_json(path, data):
	os.makedirs(os.path.dirname(ROOT + path), exist_ok=True)
	with open(ROOT + path, "w") as f:
		json.dump(data, f, indent=2)
		f.write("\n")


def trailing_cable():
	rng = random.Random("trailing_cable")
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	disc(img, 7.5, 7.0, 6.5, STEEL, rng)            # the drum's flange
	disc(img, 7.5, 7.0, 5.0, BLACK, rng)            # the cable wound on it
	for r in (2.5, 4.0):
		for a in range(0, 360, 12):
			import math
			put(img, int(round(7.5 + r * math.cos(math.radians(a)))), int(round(7.0 + r * math.sin(math.radians(a)))), DARK, rng, 4)
	disc(img, 7.5, 7.0, 1.4, STEEL, rng)            # the hub
	rect(img, 12, 9, 13, 13, BLACK, rng)            # the tail hanging off
	rect(img, 11, 13, 14, 15, ORANGE, rng)          # its coupler
	save(img, "textures/item/trailing_cable_placeholder.png")
	item("trailing_cable")


def ultra_haul_truck():
	rng = random.Random("ultra_haul_truck")
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	rect(img, 1, 3, 14, 8, YELLOW, rng)             # the body
	rect(img, 0, 2, 4, 3, YELLOW, rng)              # its canopy over the cab
	rect(img, 1, 5, 3, 7, DARK, rng)                # the cab's window
	rect(img, 2, 9, 14, 9, DARK, rng)               # the frame
	disc(img, 4.0, 12.0, 3.2, BLACK, rng)           # two big tyres
	disc(img, 11.5, 12.0, 3.2, BLACK, rng)
	disc(img, 4.0, 12.0, 1.0, STEEL, rng)
	disc(img, 11.5, 12.0, 1.0, STEEL, rng)
	save(img, "textures/item/ultra_haul_truck_placeholder.png")
	item("ultra_haul_truck")


def hydraulic_shovel():
	rng = random.Random("hydraulic_shovel")
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	rect(img, 1, 12, 12, 15, DARK, rng)             # the crawlers
	rect(img, 2, 7, 10, 11, YELLOW, rng)            # the house
	rect(img, 8, 5, 10, 7, DARK, rng)               # the cab
	for i in range(6):
		rect(img, 10 + i // 2, 7 - i, 11 + i // 2, 8 - i, YELLOW, rng)   # the boom, up and out
	rect(img, 12, 2, 15, 6, STEEL, rng)             # the bucket
	rect(img, 15, 6, 15, 7, STEEL, rng)
	save(img, "textures/item/hydraulic_shovel_placeholder.png")
	item("hydraulic_shovel")


if __name__ == "__main__":
	trailing_cable()
	ultra_haul_truck()
	hydraulic_shovel()
