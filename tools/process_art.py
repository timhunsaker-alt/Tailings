#!/usr/bin/env python3
"""Turns the Ludo pictures in art/source into Tailings' textures (art/final), and copies them into the mod.

Wildcatter's pipeline, at Tailings' sizes (art/source/PROMPTS.md has the prompts):
- blocks (muck, the survey stake's post and flags, the four hand-line machines): the whole picture box-filtered to 16 px, a touch
  of contrast, then its edges cross-faded so it tiles. The stake flags are one Ludo picture of white flagging tape, dyed in each
  stake's colour (the colours the script-drawn flags had), so the seven read alike;
- items: Ludo draws them on transparency (or a plain ground, made transparent here), cropped to the item, squared and shrunk to 16 px;
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
	"shaking_table": "shaking_table", "magnetic_separator": "magnetic_separator", "vent_duct": "vent_duct",
	"fill_bulkhead": "fill_bulkhead", "paste_fill_wet": "paste_fill_wet", "paste_fill_set": "paste_fill_set"}
# each stake's flag colour, as the placeholder flags had them (docs/multiblocks/open-pit.md)
FLAGS = {"load": (63, 173, 73), "loadout": (76, 136, 206), "dump_point": (155, 75, 45), "dump_pocket": (209, 69, 59),
	"park": (137, 137, 147), "fuel": (240, 210, 50), "waypoint": (225, 115, 25)}
ITEMS = ("survey_book", "haul_truck", "wheel_loader", "indium", "rhenium", "cobalt", "cadmium", "rare_earth_oxide",
	"copper_sulfate", "lime", "mibc", "xanthate", "zinc_sulfate", "battery_locomotive", "bottom_dump_car", "loco_battery",
	"vent_duct_cassette", "gas_detector", "cement", "heavy_mineral_concentrate")
VEHICLE_MATS = ("paint_yellow", "rubber_tread", "steel_dark", "steel_worn", "steel_rust")


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


def save(img, kind, name, copy=True):
	os.makedirs(f"{OUT}/{kind}", exist_ok=True)
	img.convert("RGBA").save(f"{OUT}/{kind}/{name}.png")
	if copy:
		shutil.copy(f"{OUT}/{kind}/{name}.png", f"{TEX}/{kind}/{name}.png")


for src, name in BLOCKS.items():
	img = tile(src, BLOCK)
	if src == "paste_fill_wet":   # wet paste reads darker than set paste: the water in it
		img = ImageEnhance.Brightness(img).enhance(0.78)
	save(img, "block", name)
flag = tile("stake_flag", BLOCK)
for role, colour in FLAGS.items():
	save(dye(flag.copy(), colour), "block", f"survey_stake_{role}")
for name in ITEMS:
	save(icon(name), "item", name)
for name in VEHICLE_MATS:
	save(tile(f"vehicle/{name}", VEHICLE), "vehicle", name, copy=False)
save(tile("muck", VEHICLE), "vehicle", "rock", copy=False)   # the loads in the truck, the bucket and the cars are the pit's muck
print("art processed:", sum(len(fs) for _, _, fs in os.walk(OUT)), "textures")
