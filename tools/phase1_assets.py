#!/usr/bin/env python3
"""Write item models, language, recipes and loot for the phase 1 items. Ids come from the Java sources."""

import json
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COMMON = ROOT / "common/src/main/resources"
ROCK = (ROOT / "common/src/main/java/dev/wildcatter/tailings/rock/Rock.java").read_text()
MINERAL = (ROOT / "common/src/main/java/dev/wildcatter/tailings/rock/Mineral.java").read_text()

rocks = re.findall(r'\t[A-Z_]+\("([a-z_]+)", "([^"]+)"', ROCK)
nugget_crafts = re.findall(r'nugget\("([a-z_]+)"\), "([^"]+)"', MINERAL)
nugget_crafts += re.findall(r'"(tailings:[a-z_]+_nugget)", "([^"]+)"', MINERAL)

SIZES = [("crushed", "Crushed"), ("gravel", "Gravel"), ("sand", "Sand"), ("fines", "Fines")]
MACHINES = [
    ("stamp_mill", "Stamp Mill", "tailings:block/stamp_mill"),
    ("hand_screen", "Hand Screen", "tailings:block/hand_screen"),
    ("shaking_table", "Shaking Table", "tailings:block/shaking_table"),
    ("magnetic_separator", "Magnetic Separator", "tailings:block/magnetic_separator"),
]
ITEM_TEXTURE = {
    "crushed": "minecraft:block/cobblestone",
    "gravel": "minecraft:block/gravel",
    "sand": "minecraft:block/sand",
    "fines": "minecraft:block/clay",
    "nugget": "minecraft:item/iron_nugget",
    "mineral": "minecraft:item/raw_iron",
    "concentrate": "minecraft:item/gold_nugget",
}


def write(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2) + "\n")


def item_model(name, texture):
    write(COMMON / f"assets/tailings/models/item/{name}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": texture},
    })


def item_def(path, model):
    write(COMMON / f"assets/tailings/items/{path}.json", {
        "model": {"type": "minecraft:model", "model": model},
    })


lang = {"itemGroup.tailings": "Tailings"}

for name, title, texture in MACHINES:
    write(COMMON / f"assets/tailings/blockstates/{name}.json", {
        "variants": {"": {"model": f"tailings:block/{name}"}},
    })
    write(COMMON / f"assets/tailings/models/block/{name}.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": texture},
    })
    item_def(name, f"tailings:block/{name}")
    lang[f"block.tailings.{name}"] = title
    write(COMMON / f"data/tailings/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"tailings:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })

write(COMMON / "data/minecraft/tags/block/mineable/pickaxe.json", {
    "values": [f"tailings:{name}" for name, _, _ in MACHINES],
})
write(COMMON / "data/minecraft/tags/block/needs_stone_tool.json", {
    "values": [f"tailings:{name}" for name, _, _ in MACHINES],
})

for key, texture in ITEM_TEXTURE.items():
    if key in {"crushed", "gravel", "sand", "fines", "nugget", "mineral", "concentrate"}:
        item_model(key, texture)

for rock_id, rock_name in rocks:
    for size, label in SIZES:
        path = f"{size}_{rock_id}"
        item_def(path, f"tailings:item/{size}")
        lang[f"item.tailings.{path}"] = f"{label} {rock_name}"

seen = set()
for nugget_id, result in nugget_crafts:
    path = nugget_id.split(":", 1)[1] if ":" in nugget_id else nugget_id + "_nugget"
    if not path.startswith("tailings:") and ":" not in nugget_id:
        item_path = nugget_id + "_nugget" if nugget_id != path else path
    else:
        item_path = path if ":" not in path else path
    # nugget("cassiterite") -> cassiterite, result wildcatter:cassiterite
    if ":" in nugget_id:
        item_path = nugget_id.split(":", 1)[1]
        result_id = result
    else:
        item_path = nugget_id + "_nugget"
        result_id = result
    if item_path in seen:
        continue
    seen.add(item_path)
    item_def(item_path, "tailings:item/nugget")
    pretty = item_path.replace("_nugget", "").replace("_", " ").title() + " Nugget"
    lang[f"item.tailings.{item_path}"] = pretty
    write(COMMON / f"data/tailings/recipe/{item_path}_to_item.json", {
        "type": "minecraft:crafting_shapeless",
        "ingredients": [f"tailings:{item_path}"] * 9,
        "result": {"id": result_id, "count": 1},
    })

for path, title, model in [
    ("zircon", "Zircon", "mineral"),
    ("ilmenite", "Ilmenite", "mineral"),
    ("rutile", "Rutile", "mineral"),
    ("monazite", "Monazite", "mineral"),
    ("zircon_concentrate", "Zircon Concentrate", "concentrate"),
    ("ilmenite_concentrate", "Ilmenite Concentrate", "concentrate"),
    ("rutile_concentrate", "Rutile Concentrate", "concentrate"),
    ("monazite_concentrate", "Monazite Concentrate", "concentrate"),
    ("chromite_concentrate", "Chromite Concentrate", "concentrate"),
    ("scheelite_concentrate", "Scheelite Concentrate", "concentrate"),
]:
    item_def(path, f"tailings:item/{model}")
    lang[f"item.tailings.{path}"] = title

recipes = {
    "stamp_mill": ["CIC", "CSC", "CCC"],
    "hand_screen": ["S S", "III", "S S"],
    "shaking_table": ["PPP", " I ", "PPP"],
    "magnetic_separator": ["I I", " C ", "I I"],
}
keys = {
    "stamp_mill": {"C": "minecraft:cobblestone", "I": "minecraft:iron_ingot", "S": "minecraft:stick"},
    "hand_screen": {"S": "minecraft:stick", "I": "minecraft:iron_ingot"},
    "shaking_table": {"P": "minecraft:oak_planks", "I": "minecraft:iron_ingot"},
    "magnetic_separator": {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"},
}
for name, pattern in recipes.items():
    write(COMMON / f"data/tailings/recipe/{name}.json", {
        "type": "minecraft:crafting_shaped",
        "pattern": pattern,
        "key": keys[name],
        "result": {"id": f"tailings:{name}", "count": 1},
    })

write(COMMON / "assets/tailings/lang/en_us.json", lang)
print(f"wrote {len(lang)} lang entries")
