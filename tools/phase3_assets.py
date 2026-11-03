#!/usr/bin/env python3
"""Blockstates, models, recipes and loot for the tunnel drill."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "common/src/main/resources/assets/tailings"
DATA = ROOT / "common/src/main/resources/data"

LOOKS = {
    "stone": "minecraft:block/stone",
    "cobble": "minecraft:block/cobblestone",
    "deepslate": "minecraft:block/deepslate",
    "granite": "minecraft:block/granite",
    "diorite": "minecraft:block/diorite",
    "andesite": "minecraft:block/andesite",
    "tuff": "minecraft:block/tuff",
    "sandstone": "minecraft:block/sandstone",
}
FACINGS = {"north": 0, "east": 90, "south": 180, "west": 270}
BLOCKS = [
    "lining", "slope", "mine_stair", "mine_landing", "mine_floor", "mine_rail",
    "tunnel_drill", "dewatering_pump",
]


def write(path: Path, payload: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(payload, indent=2) + "\n")


def cube(texture: str) -> dict:
    return {"parent": "minecraft:block/cube_all", "textures": {"all": texture}}


def slab(texture: str, top: str | None = None) -> dict:
    face = top or texture
    return {"parent": "minecraft:block/slab", "textures": {"bottom": texture, "top": face, "side": texture}}


def slab_top(texture: str, top: str | None = None) -> dict:
    face = top or texture
    return {"parent": "minecraft:block/slab_top", "textures": {"bottom": texture, "top": face, "side": texture}}


def stairs(texture: str) -> dict:
    return {"parent": "minecraft:block/stairs", "textures": {"bottom": texture, "top": texture, "side": texture}}


def column(side: str, end: str) -> dict:
    return {"parent": "minecraft:block/cube_column", "textures": {"side": side, "end": end}}


def item_model(model: str) -> dict:
    return {"model": {"type": "minecraft:model", "model": model}}


def variant(model: str, y: int = 0) -> dict:
    entry = {"model": model}
    if y:
        entry["y"] = y
    return entry


# Lining, cab, pump.
write(ASSETS / "models/block/lining.json", cube("minecraft:block/gray_concrete"))
write(ASSETS / "models/block/tunnel_drill.json", cube("minecraft:block/deepslate_bricks"))
write(ASSETS / "models/block/dewatering_pump.json", cube("minecraft:block/copper_block"))
write(ASSETS / "blockstates/lining.json", {"variants": {"": {"model": "tailings:block/lining"}}})

drill_variants = {}
pump_variants = {}
for facing, y in FACINGS.items():
    drill_variants[f"facing={facing}"] = variant("tailings:block/tunnel_drill", y)
    for piece in range(8):
        pump_variants[f"facing={facing},piece={piece}"] = variant("tailings:block/dewatering_pump", y)
write(ASSETS / "blockstates/tunnel_drill.json", {"variants": drill_variants})
write(ASSETS / "blockstates/dewatering_pump.json", {"variants": pump_variants})

slope_variants = {}
stair_variants = {}
landing_variants = {}
floor_variants = {}
rail_variants = {}
for look, texture in LOOKS.items():
    write(ASSETS / f"models/block/slope_lower_{look}.json", slab(texture))
    write(ASSETS / f"models/block/slope_upper_{look}.json", slab_top(texture))
    write(ASSETS / f"models/block/slope_steep_{look}.json", cube(texture))
    write(ASSETS / f"models/block/mine_stair_{look}.json", stairs(texture))
    write(ASSETS / f"models/block/mine_landing_{look}.json", cube(texture))
    write(ASSETS / f"models/block/mine_floor_{look}.json", cube(texture))
    write(ASSETS / f"models/block/mine_rail_lower_{look}.json", slab(texture, "minecraft:block/iron_block"))
    write(ASSETS / f"models/block/mine_rail_upper_{look}.json", slab_top(texture, "minecraft:block/iron_block"))
    write(ASSETS / f"models/block/mine_rail_steep_{look}.json", column(texture, "minecraft:block/iron_block"))
    landing_variants[f"look={look}"] = {"model": f"tailings:block/mine_landing_{look}"}
    floor_variants[f"look={look}"] = {"model": f"tailings:block/mine_floor_{look}"}
    for facing, y in FACINGS.items():
        stair_variants[f"facing={facing},look={look}"] = variant(f"tailings:block/mine_stair_{look}", y)
        for half in ("lower", "upper", "steep"):
            slope_variants[f"facing={facing},half={half},look={look}"] = variant(f"tailings:block/slope_{half}_{look}", y)
        for grade in ("lower", "upper", "steep"):
            for rack in ("false", "true"):
                rail_variants[f"facing={facing},grade={grade},look={look},rack={rack}"] = variant(
                    f"tailings:block/mine_rail_{grade}_{look}", y)
        for rack in ("false", "true"):
            rail_variants[f"facing={facing},grade=flat,look={look},rack={rack}"] = variant("tailings:block/mine_rail_flat", y)

write(ASSETS / "models/block/mine_rail_flat.json", column("minecraft:block/gravel", "minecraft:block/iron_block"))
write(ASSETS / "blockstates/slope.json", {"variants": slope_variants})
write(ASSETS / "blockstates/mine_stair.json", {"variants": stair_variants})
write(ASSETS / "blockstates/mine_landing.json", {"variants": landing_variants})
write(ASSETS / "blockstates/mine_floor.json", {"variants": floor_variants})
write(ASSETS / "blockstates/mine_rail.json", {"variants": rail_variants})

for name, model in (
    ("lining", "tailings:block/lining"),
    ("slope", "tailings:block/slope_steep_stone"),
    ("mine_stair", "tailings:block/mine_stair_stone"),
    ("mine_landing", "tailings:block/mine_landing_stone"),
    ("mine_floor", "tailings:block/mine_floor_stone"),
    ("mine_rail", "tailings:block/mine_rail_flat"),
    ("tunnel_drill", "tailings:block/tunnel_drill"),
    ("dewatering_pump", "tailings:block/dewatering_pump"),
):
    write(ASSETS / f"items/{name}.json", item_model(model))
    write(DATA / f"tailings/loot_table/blocks/{name}.json", {
        "type": "minecraft:block",
        "pools": [{
            "rolls": 1,
            "entries": [{"type": "minecraft:item", "name": f"tailings:{name}"}],
            "conditions": [{"condition": "minecraft:survives_explosion"}],
        }],
    })

write(DATA / "tailings/recipe/lining.json", {
    "type": "minecraft:crafting_shapeless",
    "ingredients": ["minecraft:gray_concrete", "minecraft:gray_concrete", "minecraft:gray_concrete", "minecraft:gray_concrete", "minecraft:iron_ingot"],
    "result": {"id": "tailings:lining", "count": 4},
})
write(DATA / "tailings/recipe/mine_rail.json", {
    "type": "minecraft:crafting_shaped",
    "pattern": ["I I", "ICI", "I I"],
    "key": {"I": "minecraft:iron_ingot", "C": "minecraft:copper_ingot"},
    "result": {"id": "tailings:mine_rail", "count": 2},
})
write(DATA / "tailings/recipe/tunnel_drill.json", {
    "type": "minecraft:crafting_shaped",
    "pattern": ["III", "IBI", "III"],
    "key": {"I": "minecraft:iron_ingot", "B": "minecraft:copper_block"},
    "result": {"id": "tailings:tunnel_drill", "count": 1},
})
write(DATA / "tailings/recipe/dewatering_pump.json", {
    "type": "minecraft:crafting_shaped",
    "pattern": ["III", "IWI", "III"],
    "key": {"I": "minecraft:iron_ingot", "W": "minecraft:water_bucket"},
    "result": {"id": "tailings:dewatering_pump", "count": 1},
})

for tag in ("mineable/pickaxe", "needs_stone_tool"):
    path = DATA / "minecraft/tags/block" / f"{tag}.json"
    payload = json.loads(path.read_text())
    for name in BLOCKS:
        value = f"tailings:{name}"
        if value not in payload["values"]:
            payload["values"].append(value)
    write(path, payload)

lang_path = ASSETS / "lang/en_us.json"
lang = json.loads(lang_path.read_text())
lang.update({
    "block.tailings.lining": "Lining Segment",
    "block.tailings.slope": "Slope",
    "block.tailings.mine_stair": "Mine Stair",
    "block.tailings.mine_landing": "Mine Landing",
    "block.tailings.mine_floor": "Mine Floor",
    "block.tailings.mine_rail": "Mine Rail",
    "block.tailings.tunnel_drill": "Tunnel Drill",
    "block.tailings.dewatering_pump": "Dewatering Pump",
})
write(lang_path, lang)
print("phase 3 assets written")
