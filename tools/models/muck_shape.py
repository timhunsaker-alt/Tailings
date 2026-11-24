"""Writes the muck and stockpile rock blockstates and their top models (Heavy Iron fix of 2026-11-23): a block of a pile's surface is
drawn rounded off toward its open sides, so a pile slumped to 37 degrees reads as a pile of broken rock, not a staircase of cubes.

A block with nothing over it (up) is drawn as a 3 x 3 grid of columns. A column touching n of the block's open sides (north, east,
south, west: air or anything with no collision beside it) stands one of HEIGHTS[n] pixels, picked at random for each block (the
game's own per-position pick of a weighted model), so the step edges of a pile break up instead of running in straight lines: the
middle 16, the middle of an open edge 5-11, the corner between two open sides 1-4. A block with something over it is a whole cube.
Its collision stays a whole cube (the settle rule and the machines that dig it work in cells); MuckBlock.shapeOf builds the boxes at
the lowest of each set for its occlusion, so light reaches the lowered parts and the faces beside them are drawn. Change HEIGHTS here
and MuckBlock.HEIGHTS (the lowest of each set) together.

    python3 tools/models/muck_shape.py
"""
import json
import os

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..", "common", "src", "main", "resources", "assets", "tailings")
HEIGHTS = [[16], [5, 7, 9, 11], [1, 2, 4]]
EDGES = [0, 5, 11, 16]
# the grid's columns: (i west to east, j north to south) -> the open sides they touch
SIDES = {0: "west", 2: "east"}
ROWS = {0: "north", 2: "south"}


def column(i, j, h, tex):
	x0, x1, z0, z1 = EDGES[i], EDGES[i + 1], EDGES[j], EDGES[j + 1]
	faces = {"up": {"texture": "#all"}, "down": {"texture": "#all", "cullface": "down"}}
	for face, edge in (("north", z0 == 0), ("south", z1 == 16), ("west", x0 == 0), ("east", x1 == 16)):
		f = {"texture": "#all"}
		if edge:
			f["cullface"] = face
		faces[face] = f
	return {"textures": {"all": tex, "particle": tex}, "elements": [{"from": [x0, 0, z0], "to": [x1, h, z1], "faces": faces}]}


def write(path, data):
	with open(path, "w") as f:
		json.dump(data, f, indent=2)
		f.write("\n")


def main():
	for block in ("muck", "stockpile_rock"):
		tex = f"tailings:block/{block}"
		parts = [{"when": {"up": "false"}, "apply": {"model": f"tailings:block/{block}"}}]
		for i in range(3):
			for j in range(3):
				touch = [s for s in (SIDES.get(i), ROWS.get(j)) if s]
				# every combination of this column's sides open or not
				for mask in range(1 << len(touch)):
					open_ = [touch[k] for k in range(len(touch)) if mask >> k & 1]
					apply = []
					for h in HEIGHTS[len(open_)]:
						name = f"{block}_top_{i}{j}_{h}"
						write(os.path.join(ROOT, "models", "block", name + ".json"), column(i, j, h, tex))
						apply.append({"model": f"tailings:block/{name}"})
					when = {"up": "true"}
					for s in touch:
						when[s] = "true" if s in open_ else "false"
					parts.append({"when": when, "apply": apply[0] if len(apply) == 1 else apply})
		write(os.path.join(ROOT, "blockstates", block + ".json"), {"multipart": parts})
		print("wrote", block, len(parts), "parts")


if __name__ == "__main__":
	main()
