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

MODULES = ["crushing", "flotation", "gravity", "recovery", "dump", "rectifier", "belt", "tbm", "pit", "blast", "trains", "shaft", "vent", "paste", "drymill", "dms", "xrt", "dragline", "pitline", "sagline", "pitflotation"]


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


def export_sharing_empty(model, **args):
	"""Exports a part whose empty pieces (no box of the drawing in their cell) all share one empty model, <part>/empty.json, in place
	of a p<k>.json each; their blockstate variants name it and turn nothing. Wildcatter's mlib writes a model per cell, so its writer
	is wrapped for this export: the 45 m thickener (P7c) is 12,150 cells, most of them the air over its tank. Every big part is
	exported this way (shares_empty); a one-cell model, or one whose caller writes its blockstate, is written exactly as mlib writes it."""
	folder = os.path.join(mlib.ASSETS, "models/block", model.name)
	blockstate = os.path.join(mlib.ASSETS, "blockstates", model.name + ".json")
	empty = set()
	textures = {}
	save = mlib._save
	if os.path.isdir(folder):
		# the piece models of an earlier export go first: a part with fewer pieces now (a hollow one) leaves none behind
		for file in os.listdir(folder):
			if file.startswith("p") and file[1:-5].isdigit() and file.endswith(".json"):
				os.remove(os.path.join(folder, file))

	def sharing(path, obj):
		name = os.path.basename(path)
		if os.path.dirname(path) == folder and name.startswith("p") and name[1:-5].isdigit() and name.endswith(".json"):
			textures.update(obj.get("textures", {}))
			if not obj.get("elements"):
				empty.add(int(name[1:-5]))
				if os.path.exists(path):
					os.remove(path)   # a piece model left by an earlier export
				return
		if path == blockstate and empty:
			# an empty piece looks the same every way round: one variant names it for all four facings, and the file is written
			# without indentation (it still holds four variants for each drawn piece)
			variants = {}
			for key, value in obj["variants"].items():
				piece = int(key.split("piece=")[1])
				if piece not in empty:
					variants[key] = value
				elif f"piece={piece}" not in variants:
					variants[f"piece={piece}"] = {"model": f"{mlib.NS}:block/{model.name}/empty"}
			save(os.path.join(folder, "empty.json"), {"textures": {"particle": textures["particle"]}, "elements": []})
			with open(path, "w") as f:
				json.dump({"variants": variants}, f, separators=(",", ":"))
			return
		save(path, obj)

	mlib._save = sharing
	try:
		count = model.export(**args)
	finally:
		mlib._save = save
	print(f"{model.name}: {len(empty)} empty pieces share {model.name}/empty")
	return count


def shares_empty(model):
	"""Every big part (more than one cell, its blockstate written by mlib) shares one empty model among its empty pieces (Tim,
	2026-11-10: p7-pit-line.md #33); a model may still set share_empty = False to be written exactly as mlib writes it."""
	args = getattr(model, "export_args", {})
	return getattr(model, "share_empty", True) and len(model.cells) > 1 and args.get("blockstate", True) is not False


def main(names):
	previews = os.path.join(ROOT, "build/model-previews")
	os.makedirs(previews, exist_ok=True)
	for name in names or MODULES:
		module = importlib.import_module(name)
		for model in module.MODELS:
			if shares_empty(model):
				export_sharing_empty(model, **getattr(model, "export_args", {}))
			else:
				model.export(**getattr(model, "export_args", {}))
			fix_textures(model.name)
			model.preview(os.path.join(previews, model.name + ".png"), colours(model))
		if hasattr(module, "after"):
			module.after()


if __name__ == "__main__":
	main(sys.argv[1:])
