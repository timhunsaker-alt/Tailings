#!/usr/bin/env python3
"""Builds Tailings' true-size part models with Wildcatter's model tool, and a preview of each in build/model-previews/.

Run from the Tailings repo root, with Wildcatter checked out beside it (../wildcatter): python3 tools/models/build.py [module ...]
The modules are crushing, flotation, dump, rectifier and belt; each lists its models in MODELS.

Wildcatter's tool writes into its own namespace, so this driver points it at Tailings' assets first. Its models are drawn in
Wildcatter's material textures, which ship in the Wildcatter jar: after export, every texture this mod does not have itself is
named in the wildcatter namespace.
"""
import importlib, json, os, sys

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.normpath(os.path.join(HERE, "..", ".."))
WILDCATTER_TOOLS = os.path.normpath(os.path.join(ROOT, "..", "wildcatter", "tools", "models"))
sys.path.insert(0, HERE)
sys.path.insert(1, WILDCATTER_TOOLS)

import mlib   # noqa: E402  (Wildcatter's)

mlib.NS = "tailings"
mlib.ROOT = ROOT
mlib.ASSETS = os.path.join(ROOT, "common/src/main/resources/assets/tailings")
import importlib.util   # noqa: E402

# Wildcatter's build.py has the same name as this file, so it is loaded from its path (for its preview colours)
_spec = importlib.util.spec_from_file_location("wildcatter_build", os.path.join(WILDCATTER_TOOLS, "build.py"))
wildcatter_build = importlib.util.module_from_spec(_spec)
_spec.loader.exec_module(wildcatter_build)

MODULES = ["crushing", "flotation", "gravity", "recovery", "dump", "rectifier", "belt", "tbm", "pit", "blast"]


def own_texture(ref):
	return os.path.exists(os.path.join(mlib.ASSETS, "textures", ref.split(":", 1)[1] + ".png"))


def renamespace(path):
	"""tailings:block/mat/... becomes wildcatter:block/mat/... unless Tailings has that texture itself."""
	with open(path) as f:
		data = json.load(f)
	textures = data.get("textures", {})
	changed = False
	for key, ref in textures.items():
		if isinstance(ref, str) and ref.startswith("tailings:") and not own_texture(ref):
			textures[key] = "wildcatter:" + ref.split(":", 1)[1]
			changed = True
	if changed:
		with open(path, "w") as f:
			json.dump(data, f, indent=1)


def fix_textures(name):
	folder = os.path.join(mlib.ASSETS, "models/block", name)
	for file in os.listdir(folder):
		if file.endswith(".json"):
			renamespace(os.path.join(folder, file))


def colours(model):
	"""Wildcatter's preview colours, read from Wildcatter's own copy of each material."""
	saved = mlib.ASSETS
	mlib.ASSETS = os.path.join(WILDCATTER_TOOLS, "..", "..", "common/src/main/resources/assets/wildcatter")
	try:
		return wildcatter_build.colours(model)
	finally:
		mlib.ASSETS = saved


def main(names):
	previews = os.path.join(ROOT, "build/model-previews")
	os.makedirs(previews, exist_ok=True)
	for name in names or MODULES:
		module = importlib.import_module(name)
		for model in module.MODELS:
			model.export(**getattr(model, "export_args", {}))
			fix_textures(model.name)
			model.preview(os.path.join(previews, model.name + ".png"), colours(model))
		if hasattr(module, "after"):
			module.after()


if __name__ == "__main__":
	main(sys.argv[1:])
