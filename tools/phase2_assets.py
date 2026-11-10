#!/usr/bin/env python3
"""Models, language, recipes and loot for the phase 2 machines and reagents."""

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
COMMON = ROOT / "common/src/main/resources"

MACHINES = [
    ("grizzly", "Grizzly", "minecraft:block/iron_bars", 3 * 2 * 4),
    ("jaw_crusher", "Jaw Crusher", "minecraft:block/deepslate", 3 * 4 * 4),
    ("cone_crusher", "Cone Crusher", "minecraft:block/tuff", 3 * 4 * 3),
    ("double_deck_screen", "Double-Deck Screen", "minecraft:block/smooth_stone", 3 * 3 * 6),
    ("belt_conveyor", "Belt Conveyor", "minecraft:block/black_concrete", 1 * 1 * 6),
    ("ball_mill", "Ball Mill", "minecraft:block/gray_concrete", 4 * 4 * 6),
    ("cyclone_cluster", "Cyclone Cluster", "minecraft:block/orange_terracotta", 2 * 4 * 2),
    ("conditioning_tank", "Conditioning Tank", "minecraft:block/cauldron", 3 * 4 * 3),
    ("flotation_cell", "Flotation Cell", "minecraft:block/light_blue_concrete", 3 * 4 * 3),
    ("air_blower", "Air Blower", "minecraft:block/blue_terracotta", 2 * 2 * 3),
    ("concentrate_thickener", "Concentrate Thickener", "minecraft:block/brown_terracotta", 8 * 3 * 8),
    ("pressure_filter", "Pressure Filter", "minecraft:block/white_terracotta", 3 * 3 * 5),
]
REAGENTS = [
    ("lime", "Quicklime"),
    ("xanthate", "Potassium Amyl Xanthate"),
    ("mibc", "MIBC Frother"),
    ("copper_sulfate", "Copper Sulfate"),
]
FACINGS = ("north", "south", "east", "west")


def write(path: Path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    text = json.dumps(data, indent=2) + "\n"
    path.write_text(text)


lang_path = COMMON / "assets/tailings/lang/en_us.json"
lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}

for name, title, texture, cells in MACHINES:
    write(COMMON / f"assets/tailings/models/block/{name}.json", {
        "parent": "minecraft:block/cube_all",
        "textures": {"all": texture},
    })
    variants = {}
    for facing in FACINGS:
        for piece in range(cells):
            variants[f"facing={facing},piece={piece}"] = {"model": f"tailings:block/{name}"}
    write(COMMON / f"assets/tailings/blockstates/{name}.json", {"variants": variants})
    write(COMMON / f"assets/tailings/items/{name}.json", {
        "model": {"type": "minecraft:model", "model": f"tailings:block/{name}"},
    })
    lang[f"block.tailings.{name}"] = title
    write(COMMON / f"data/tailings/recipe/{name}.json", {
        "type": "minecraft:crafting_shaped",
        "pattern": ["III", "ICI", "III"],
        "key": {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"},
        "result": {"id": f"tailings:{name}", "count": 1},
    })

for name, title in REAGENTS:
    write(COMMON / f"assets/tailings/models/item/{name}.json", {
        "parent": "minecraft:item/generated",
        "textures": {"layer0": f"tailings:item/{name}"},   # Ludo icons (tools/process_art.py)
    })
    write(COMMON / f"assets/tailings/items/{name}.json", {
        "model": {"type": "minecraft:model", "model": f"tailings:item/{name}"},
    })
    lang[f"item.tailings.{name}"] = title

write(COMMON / "data/tailings/recipe/lime.json", {
    "type": "minecraft:smelting",
    "ingredient": "minecraft:calcite",
    "result": {"id": "tailings:lime"},
    "cookingtime": 200,
    "experience": 0.1,
})
write(COMMON / "data/tailings/recipe/copper_sulfate.json", {
    "type": "minecraft:crafting_shapeless",
    "ingredients": ["minecraft:copper_ingot", "wildcatter:sulfur"],
    "result": {"id": "tailings:copper_sulfate", "count": 4},
})
write(COMMON / "data/tailings/recipe/xanthate.json", {
    "type": "minecraft:crafting_shapeless",
    "ingredients": ["tailings:lime", "wildcatter:sulfur", "minecraft:sugar"],
    "result": {"id": "tailings:xanthate", "count": 4},
})
write(COMMON / "data/tailings/recipe/mibc.json", {
    "type": "minecraft:crafting_shapeless",
    "ingredients": ["minecraft:sugar", "minecraft:sugar"],
    "result": {"id": "tailings:mibc", "count": 4},
})

tag_path = COMMON / "data/minecraft/tags/block/mineable/pickaxe.json"
tag = json.loads(tag_path.read_text())
values = list(tag["values"])
for name, _, _, _ in MACHINES:
    block = f"tailings:{name}"
    if block not in values:
        values.append(block)
tag["values"] = values
write(tag_path, tag)
stone = COMMON / "data/minecraft/tags/block/needs_stone_tool.json"
stone_tag = json.loads(stone.read_text())
stone_values = list(stone_tag["values"])
for name, _, _, _ in MACHINES:
    block = f"tailings:{name}"
    if block not in stone_values:
        stone_values.append(block)
stone_tag["values"] = stone_values
write(stone, stone_tag)
write(lang_path, lang)
print(f"machines {len(MACHINES)}")
