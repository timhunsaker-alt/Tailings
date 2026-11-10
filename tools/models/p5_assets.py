#!/usr/bin/env python3
"""The open pit's P5 items (docs/multiblocks/open-pit-p5-big-machines.md): their item definitions and models. Their icons are
Ludo art (art/source/PROMPTS.md), made by tools/process_art.py; this script draws no textures.

- trailing_cable: a steel reel of black mining cable, its coupler hanging off;
- ultra_haul_truck: the 360 t truck at three-quarter view;
- hydraulic_shovel: the front shovel on its crawlers, boom up, bucket out.

Run from the repo root: python3 tools/models/p5_assets.py
"""
import json
import os

ROOT = "common/src/main/resources/assets/tailings/"


def save_json(path, data):
	os.makedirs(os.path.dirname(ROOT + path), exist_ok=True)
	with open(ROOT + path, "w") as f:
		json.dump(data, f, indent=2)
		f.write("\n")


def item(name):
	save_json(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"tailings:item/{name}"}})
	save_json(f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"tailings:item/{name}"}})


if __name__ == "__main__":
	for name in ("trailing_cable", "ultra_haul_truck", "hydraulic_shovel"):
		item(name)
