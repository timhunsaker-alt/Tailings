"""The materials the vehicle skins are painted in: Ludo's swatches (art/source/vehicle/, made 32 px tiles by tools/process_art.py).

A skin pixel takes the swatch pixel at the same place on the sheet, so a material runs on unbroken across a face and its edges;
at 16 px a metre a 32 px tile is 2 m of paint, tread or rock. Each of the generators' colours names its material; the shading the
script gives a face (lighter tops, darker undersides, plate edges, ribs) multiplies the swatch.
"""
import os

from PIL import Image

ART = "art/final/vehicle"
_cache = {}


def swatch(name):
	if name not in _cache:
		path = f"{ART}/{name}.png"
		if not os.path.exists(path):
			raise SystemExit(f"{path} is missing: run python3 tools/process_art.py first")
		_cache[name] = Image.open(path).convert("RGB")
	return _cache[name]


def sample(name, x, y, k=1.0):
	"""The material's colour at sheet pixel (x, y), times k."""
	img = swatch(name)
	r, g, b = img.getpixel((x % img.width, y % img.height))
	return (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k)))
