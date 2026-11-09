#!/usr/bin/env python3
"""The open pit's P3 blocks and items (docs/multiblocks/open-pit-p3-blasting.md): placeholder textures drawn by script, and their
block states, models and item definitions. Every texture is *_placeholder until the art comes.

- blasthole: a column block, rock with the 270 mm hole down its middle; its collar shows a coloured tag by state (white drilled,
  yellow primed, orange charged, grey stemmed, green logged, red misfired);
- blasting_machine: a box on a stand with a key switch, ARM and FIRE;
- items: cast_booster, detonator, blast_logger, blasting_key, blasthole_drill, emulsion_truck.

Run from the repo root: python3 tools/models/blast_assets.py
"""
import json
import os
import random

from PIL import Image

ROOT = "common/src/main/resources/assets/tailings/"
STAGES = {"drilled": (235, 235, 230), "primed": (232, 200, 40), "charged": (226, 120, 30), "stemmed": (128, 128, 128),
	"logged": (60, 170, 70), "misfired": (200, 40, 36)}


def noise(img, base, spread, rng):
	for y in range(img.height):
		for x in range(img.width):
			d = rng.randint(-spread, spread)
			img.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in base) + (255,))


def hole(img, centre=(7.5, 7.5), r=2.6, colour=(22, 22, 24)):
	for y in range(img.height):
		for x in range(img.width):
			if (x - centre[0]) ** 2 + (y - centre[1]) ** 2 <= r * r:
				img.putpixel((x, y), colour + (255,))


def save(img, path):
	os.makedirs(os.path.dirname(ROOT + path), exist_ok=True)
	img.save(ROOT + path)


def write(path, data):
	os.makedirs(os.path.dirname(ROOT + path), exist_ok=True)
	with open(ROOT + path, "w") as f:
		json.dump(data, f, indent=2)
		f.write("\n")


def blocks():
	rng = random.Random("blasthole")
	side = Image.new("RGBA", (16, 16))
	noise(side, (118, 116, 112), 10, rng)
	for y in range(16):   # the hole's line down the face, where the column runs
		side.putpixel((7, y), (52, 50, 48, 255))
		side.putpixel((8, y), (60, 58, 56, 255))
	save(side, "textures/block/blasthole_placeholder.png")
	top = Image.new("RGBA", (16, 16))
	noise(top, (118, 116, 112), 10, rng)
	hole(top)
	save(top, "textures/block/blasthole_top_placeholder.png")
	variants = {}
	for stage, colour in STAGES.items():
		collar = Image.new("RGBA", (16, 16))
		noise(collar, (132, 126, 116), 12, rng)   # the cuttings round the collar
		for y in range(16):
			for x in range(16):
				dd = (x - 7.5) ** 2 + (y - 7.5) ** 2
				if 3.2 ** 2 < dd <= 4.6 ** 2:
					collar.putpixel((x, y), colour + (255,))   # the state's tag, round the hole
		hole(collar)
		save(collar, f"textures/block/blasthole_collar_{stage}_placeholder.png")
		write(f"models/block/blasthole_collar_{stage}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
			"top": f"tailings:block/blasthole_collar_{stage}_placeholder", "bottom": "tailings:block/blasthole_top_placeholder",
			"side": "tailings:block/blasthole_placeholder"}})
		variants[f"collar=true,stage={stage}"] = {"model": f"tailings:block/blasthole_collar_{stage}"}
		variants[f"collar=false,stage={stage}"] = {"model": "tailings:block/blasthole"}
	write("models/block/blasthole.json", {"parent": "minecraft:block/cube_column", "textures": {
		"end": "tailings:block/blasthole_top_placeholder", "side": "tailings:block/blasthole_placeholder"}})
	write("blockstates/blasthole.json", {"variants": variants})
	write("items/blasthole.json", {"model": {"type": "minecraft:model", "model": "tailings:block/blasthole_collar_drilled"}})

	box = Image.new("RGBA", (16, 16))
	noise(box, (170, 40, 36), 6, rng)
	for x in range(3, 13):
		box.putpixel((x, 3), (40, 40, 40, 255))
	for (x, y, c) in ((5, 7, (240, 200, 40)), (10, 7, (220, 30, 30)), (5, 8, (240, 200, 40)), (10, 8, (220, 30, 30))):
		box.putpixel((x, y), c + (255,))   # ARM (amber) and FIRE (red)
	box.putpixel((7, 11), (200, 200, 200, 255))
	box.putpixel((8, 11), (200, 200, 200, 255))   # the key switch
	save(box, "textures/block/blasting_machine_placeholder.png")
	leg = Image.new("RGBA", (16, 16))
	noise(leg, (96, 100, 106), 6, rng)
	save(leg, "textures/block/blasting_machine_stand_placeholder.png")
	write("models/block/blasting_machine.json", {"parent": "minecraft:block/block", "textures": {
		"particle": "tailings:block/blasting_machine_placeholder", "box": "tailings:block/blasting_machine_placeholder",
		"stand": "tailings:block/blasting_machine_stand_placeholder"}, "elements": [
		{"from": [7, 0, 7], "to": [9, 6, 9], "faces": {f: {"texture": "#stand"} for f in ("north", "south", "east", "west", "up", "down")}},
		{"from": [4, 0, 4], "to": [12, 1, 12], "faces": {f: {"texture": "#stand"} for f in ("north", "south", "east", "west", "up", "down")}},
		{"from": [3, 6, 3], "to": [13, 12, 13], "faces": {f: {"texture": "#box"} for f in ("north", "south", "east", "west", "up", "down")}}]})
	write("blockstates/blasting_machine.json", {"variants": {"": {"model": "tailings:block/blasting_machine"}}})
	write("items/blasting_machine.json", {"model": {"type": "minecraft:model", "model": "tailings:block/blasting_machine"}})


def item(name, draw):
	img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
	draw(img, random.Random(name))
	save(img, f"textures/item/{name}_placeholder.png")
	write(f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"tailings:item/{name}_placeholder"}})
	write(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"tailings:item/{name}"}})


def rect(img, x0, y0, x1, y1, colour, rng=None, spread=0):
	for y in range(y0, y1 + 1):
		for x in range(x0, x1 + 1):
			d = rng.randint(-spread, spread) if rng else 0
			img.putpixel((x, y), tuple(max(0, min(255, c + d)) for c in colour) + (255,))


def booster(img, rng):
	rect(img, 5, 3, 10, 13, (190, 160, 70), rng, 8)          # a cast cylinder of TNT
	rect(img, 7, 2, 8, 3, (60, 60, 60))                       # the detonator well


def detonator(img, rng):
	rect(img, 7, 2, 8, 10, (170, 120, 60), rng, 6)            # the shell
	for i in range(10, 15):
		img.putpixel((7 + (i % 2), i), (220, 200, 40, 255))   # the downline


def logger(img, rng):
	rect(img, 4, 2, 11, 13, (230, 200, 40), rng, 4)
	rect(img, 5, 3, 10, 6, (40, 70, 60))                      # its screen
	for x, y in ((5, 9), (7, 9), (9, 9), (5, 11), (7, 11), (9, 11)):
		img.putpixel((x, y), (30, 30, 30, 255))


def key(img, rng):
	rect(img, 4, 5, 7, 8, (200, 160, 40), rng, 6)
	rect(img, 8, 6, 13, 7, (200, 160, 40), rng, 6)
	img.putpixel((12, 8), (200, 160, 40, 255))
	img.putpixel((10, 8), (200, 160, 40, 255))


def drill(img, rng):
	rect(img, 2, 11, 13, 13, (30, 30, 32))                    # crawlers
	rect(img, 2, 8, 13, 10, (214, 166, 34), rng, 6)           # deck
	rect(img, 11, 1, 12, 8, (214, 166, 34), rng, 6)           # mast
	rect(img, 3, 6, 5, 8, (214, 166, 34), rng, 6)             # cab
	rect(img, 3, 6, 4, 7, (44, 68, 82))


def truck(img, rng):
	rect(img, 2, 6, 4, 10, (222, 222, 216), rng, 4)           # cab
	rect(img, 2, 7, 3, 8, (44, 68, 82))
	rect(img, 5, 5, 13, 10, (222, 222, 216), rng, 4)          # tank
	rect(img, 5, 8, 13, 8, (168, 42, 38))                     # its stripe
	for x in (3, 9, 12):
		rect(img, x - 1, 11, x + 1, 12, (30, 30, 32))


if __name__ == "__main__":
	blocks()
	item("cast_booster", booster)
	item("detonator", detonator)
	item("blast_logger", logger)
	item("blasting_key", key)
	item("blasthole_drill", drill)
	item("emulsion_truck", truck)
	print("blast assets written")
