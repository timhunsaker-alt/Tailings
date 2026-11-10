#!/usr/bin/env python3
"""The open pit's P3 blocks and items (docs/multiblocks/open-pit-p3-blasting.md): their block states, models and item definitions.
Their textures are Ludo art made by tools/process_art.py (art/source/PROMPTS.md), no longer drawn here.

- blasthole: a column block, rock with the 270 mm hole down its middle; its collar shows a coloured tag by state (white drilled,
  yellow primed, orange charged, grey stemmed, green logged, red misfired);
- blasting_machine: a box on a stand with a key switch, ARM and FIRE;
- items: cast_booster, detonator, blast_logger, blasting_key, blasthole_drill, emulsion_truck.

Run from the repo root: python3 tools/models/blast_assets.py
"""
import json
import os

ROOT = "common/src/main/resources/assets/tailings/"
STAGES = ("drilled", "primed", "charged", "stemmed", "logged", "misfired")


def write(path, data):
	os.makedirs(os.path.dirname(ROOT + path), exist_ok=True)
	with open(ROOT + path, "w") as f:
		json.dump(data, f, indent=2)
		f.write("\n")


def blocks():
	variants = {}
	for stage in STAGES:
		write(f"models/block/blasthole_collar_{stage}.json", {"parent": "minecraft:block/cube_bottom_top", "textures": {
			"top": f"tailings:block/blasthole_collar_{stage}", "bottom": "tailings:block/blasthole_top",
			"side": "tailings:block/blasthole"}})
		variants[f"collar=true,stage={stage}"] = {"model": f"tailings:block/blasthole_collar_{stage}"}
		variants[f"collar=false,stage={stage}"] = {"model": "tailings:block/blasthole"}
	write("models/block/blasthole.json", {"parent": "minecraft:block/cube_column", "textures": {
		"end": "tailings:block/blasthole_top", "side": "tailings:block/blasthole"}})
	write("blockstates/blasthole.json", {"variants": variants})
	write("items/blasthole.json", {"model": {"type": "minecraft:model", "model": "tailings:block/blasthole_collar_drilled"}})

	write("models/block/blasting_machine.json", {"parent": "minecraft:block/block", "textures": {
		"particle": "tailings:block/blasting_machine", "box": "tailings:block/blasting_machine",
		"stand": "tailings:block/blasting_machine_stand"}, "elements": [
		{"from": [7, 0, 7], "to": [9, 6, 9], "faces": {f: {"texture": "#stand"} for f in ("north", "south", "east", "west", "up", "down")}},
		{"from": [4, 0, 4], "to": [12, 1, 12], "faces": {f: {"texture": "#stand"} for f in ("north", "south", "east", "west", "up", "down")}},
		{"from": [3, 6, 3], "to": [13, 12, 13], "faces": {f: {"texture": "#box"} for f in ("north", "south", "east", "west", "up", "down")}}]})
	write("blockstates/blasting_machine.json", {"variants": {"": {"model": "tailings:block/blasting_machine"}}})
	write("items/blasting_machine.json", {"model": {"type": "minecraft:model", "model": "tailings:block/blasting_machine"}})


def item(name):
	write(f"models/item/{name}.json", {"parent": "minecraft:item/generated", "textures": {"layer0": f"tailings:item/{name}"}})
	write(f"items/{name}.json", {"model": {"type": "minecraft:model", "model": f"tailings:item/{name}"}})


if __name__ == "__main__":
	blocks()
	for name in ("cast_booster", "detonator", "blast_logger", "blasting_key", "blasthole_drill", "emulsion_truck"):
		item(name)
	print("blast assets written")
