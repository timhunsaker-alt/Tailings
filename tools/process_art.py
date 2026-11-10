#!/usr/bin/env python3
"""Turns the Ludo pictures in art/source into Tailings' textures (art/final), and copies them into the mod.

Wildcatter's pipeline, at Tailings' sizes (art/source/PROMPTS.md has the prompts):
- blocks (muck, the survey stake's post and flags, the four hand-line machines): the whole picture box-filtered to 16 px, a touch
  of contrast, then its edges cross-faded so it tiles. The stake flags are one Ludo picture of white flagging tape, dyed in each
  stake's colour (the colours the script-drawn flags had), so the seven read alike;
- items: Ludo draws them on transparency (or a plain ground, made transparent here), cropped to the item, squared and shrunk to 16 px;
- the blastholes (open pit P3): one Ludo picture of bench rock makes the column's side (the hole's dark line down it) and its end
  (the hole); one of drill cuttings makes the six collars, each ringed with the flagging tape dyed in its state's colour, as the
  stake flags are, round the hole. The blasting machine's stand is the dark steel vehicle material at 16 px;
- vehicle materials (art/source/vehicle/): 32 px tiles, 2 m at the skins' 16 px a metre, for tools/models/pit_vehicles.py and
  vehicles.py to paint the skins from. They are not copied into the mod: the skins are.

Run from the repo root with Pillow installed: python3 tools/process_art.py. Then re-run tools/models/pit_vehicles.py and
tools/models/vehicles.py for the skins.
"""
import os
import shutil

from PIL import Image, ImageDraw, ImageEnhance

SRC, OUT = "art/source", "art/final"
TEX = "common/src/main/resources/assets/tailings/textures"
BLOCK, ITEM, VEHICLE = 16, 16, 32

BLOCKS = {"muck": "muck", "stake_post": "survey_stake_post", "stamp_mill": "stamp_mill", "hand_screen": "hand_screen",
	"shaking_table": "shaking_table", "magnetic_separator": "magnetic_separator", "blasting_machine": "blasting_machine",
	"dry_tails": "dry_tails"}
# each stake's flag colour, as the placeholder flags had them (docs/multiblocks/open-pit.md)
FLAGS = {"load": (63, 173, 73), "loadout": (76, 136, 206), "dump_point": (155, 75, 45), "dump_pocket": (209, 69, 59),
	"park": (137, 137, 147), "fuel": (240, 210, 50), "waypoint": (225, 115, 25)}
ITEMS = ("survey_book", "haul_truck", "wheel_loader", "indium", "rhenium", "cobalt", "cadmium", "rare_earth_oxide",
	"copper_sulfate", "lime", "mibc", "xanthate", "zinc_sulfate", "blasthole_drill", "emulsion_truck", "cast_booster", "detonator",
	"blast_logger", "blasting_key", "track_dozer", "trailing_cable", "ultra_haul_truck", "hydraulic_shovel")
VEHICLE_MATS = ("paint_yellow", "rubber_tread", "steel_dark", "steel_worn", "steel_rust", "paint_white")
# each blasthole collar's tag colour by state, as the placeholder collars had them (BlastholeBlock)
COLLARS = {"drilled": (235, 235, 230), "primed": (232, 200, 40), "charged": (226, 120, 30), "stemmed": (128, 128, 128),
	"logged": (60, 170, 70), "misfired": (200, 40, 36)}
EMULSION_RED = (168, 42, 38)   # the emulsion truck's stripe and hose reel: its white paint dyed red


def tileable(img, band):
	"""Cross-fades each edge with the opposite one so the tile repeats without a seam (as Wildcatter's process_materials.py)."""
	w, h = img.size
	for axis in (0, 1):
		src = img.copy()
		px, o = src.load(), img.load()
		for a in range(h if axis == 0 else w):
			for i in range(band):
				k = 0.5 * (1 - i / band)
				p, q = ((i, a), (w - 1 - i, a)) if axis == 0 else ((a, i), (a, h - 1 - i))
				x, z = px[p], px[q]
				o[p] = tuple(int(c * (1 - k) + d * k) for c, d in zip(x, z))
				o[q] = tuple(int(d * (1 - k) + c * k) for c, d in zip(x, z))
	return img


def tile(name, size):
	img = Image.open(f"{SRC}/{name}.webp").convert("RGB").resize((size, size), Image.BOX)
	return tileable(ImageEnhance.Contrast(img).enhance(1.1), max(2, size // 8))


def dye(img, colour):
	"""White flagging tape in a colour: the tape's light and creases kept, its white made the colour."""
	px = img.load()
	for x in range(img.width):
		for y in range(img.height):
			v = sum(px[x, y]) / (3 * 255)
			px[x, y] = tuple(min(255, int(c * (0.35 + 0.75 * v))) for c in colour)
	return img


def icon(name):
	img = Image.open(f"{SRC}/{name}.webp").convert("RGBA")
	if img.getpixel((0, 0))[3] == 255:   # drawn on a plain ground: make it transparent
		for corner in ((0, 0), (img.width - 1, 0), (0, img.height - 1), (img.width - 1, img.height - 1)):
			ImageDraw.floodfill(img, corner, (0, 0, 0, 0), thresh=40)
	img = img.crop(img.getbbox())
	side = max(img.size)
	square = Image.new("RGBA", (side, side), (0, 0, 0, 0))
	square.paste(img, ((side - img.width) // 2, (side - img.height) // 2))
	small = square.resize((ITEM, ITEM), Image.BOX)
	# a 16 px icon keeps only solid pixels, as vanilla's do: no half-transparent fringe
	a = small.getchannel("A").point(lambda v: 255 if v >= 110 else 0)
	small.putalpha(a)
	return ImageEnhance.Contrast(small).enhance(1.12)


def hole(img, r=2.6, centre=(7.5, 7.5)):
	"""The 270 mm hole: near black at its middle, the rock's own colour darkened at its lip."""
	px = img.load()
	for x in range(img.width):
		for y in range(img.height):
			d = ((x - centre[0]) ** 2 + (y - centre[1]) ** 2) ** 0.5
			if d <= r:
				px[x, y] = (22, 22, 24, 255)
			elif d <= r + 0.9:
				px[x, y] = tuple(int(c * 0.55) for c in px[x, y][:3]) + (255,)
	return img


def ring(img, tape, inner=3.2, outer=4.6):
	"""The collar's tag round the hole, in the dyed tape."""
	px, tp = img.load(), tape.load()
	for x in range(img.width):
		for y in range(img.height):
			if inner ** 2 < (x - 7.5) ** 2 + (y - 7.5) ** 2 <= outer ** 2:
				px[x, y] = tp[x, y]
	return img


def blastholes(flag):
	rock = tile("bench_rock", BLOCK)
	side = rock.convert("RGBA")
	px = side.load()
	for y in range(BLOCK):   # the hole's line down the face, where the column runs
		for x in (6, 9):
			px[x, y] = tuple(int(c * 0.6) for c in px[x, y][:3]) + (255,)
		for x in (7, 8):
			px[x, y] = (22, 22, 24, 255)   # as dark as the hole at its end, so it reads apart from the rock's joints
	save(side, "block", "blasthole")
	save(hole(rock.copy().convert("RGBA")), "block", "blasthole_top")
	cuttings = tile("drill_cuttings", BLOCK).convert("RGBA")
	for stage, colour in COLLARS.items():
		save(hole(ring(cuttings.copy(), dye(flag.copy(), colour).convert("RGBA"))), "block", f"blasthole_collar_{stage}")
	save(tile("vehicle/steel_dark", BLOCK), "block", "blasting_machine_stand")


def save(img, kind, name, copy=True):
	os.makedirs(f"{OUT}/{kind}", exist_ok=True)
	img.convert("RGBA").save(f"{OUT}/{kind}/{name}.png")
	if copy:
		shutil.copy(f"{OUT}/{kind}/{name}.png", f"{TEX}/{kind}/{name}.png")


for src, name in BLOCKS.items():
	save(tile(src, BLOCK), "block", name)
flag = tile("stake_flag", BLOCK)
for role, colour in FLAGS.items():
	save(dye(flag.copy(), colour), "block", f"survey_stake_{role}")
blastholes(flag)
for name in ITEMS:
	save(icon(name), "item", name)
for name in VEHICLE_MATS:
	save(tile(f"vehicle/{name}", VEHICLE), "vehicle", name, copy=False)
save(dye(tile("vehicle/paint_white", VEHICLE), EMULSION_RED), "vehicle", "paint_red", copy=False)
save(tile("muck", VEHICLE), "vehicle", "rock", copy=False)   # the loads in the truck, the bucket and the cars are the pit's muck
print("art processed:", sum(len(fs) for _, _, fs in os.walk(OUT)), "textures")
