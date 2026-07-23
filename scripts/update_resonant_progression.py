#!/usr/bin/env python3
"""Generate the cross-version Resonant cooking and equipment progression recipes."""

from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GEAR = ("sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots")
TIERS = ("verdant", "infernal", "aetheric", "echoing")
COOKING_TICKS = {"verdant": 200, "infernal": 400, "aetheric": 800, "echoing": 1600}
EXPERIENCE = {"verdant": 1.0, "infernal": 2.0, "aetheric": 3.0, "echoing": 4.0}
UPGRADES = {
    "infernal": ("verdant", "infernal_ascension_template"),
    "aetheric": ("infernal", "end_ascension_template"),
    "echoing": ("aetheric", "deep_convergence_template"),
}


def write(path: Path, value: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def shaped(tier: str, gear: str, result_key: str) -> dict:
    armor = {
        "helmet": ("III", "I I"),
        "chestplate": ("I I", "III", "III"),
        "leggings": ("III", "I I", "I I"),
        "boots": ("I I", "I I"),
    }
    tools = {
        "sword": ("I", "I", "S"),
        "pickaxe": ("III", " S ", " S "),
        "axe": ("II ", "IS ", " S "),
        "shovel": ("I", "S", "S"),
        "hoe": ("II ", " S ", " S "),
    }
    pattern = armor[gear] if gear in armor else tools[gear]
    key = {"I": {"item": f"velorise_emc:{tier}_resonant_ingot"}}
    if "S" in "".join(pattern):
        key["S"] = {"item": "minecraft:stick"}
    return {
        "type": "minecraft:crafting_shaped",
        "category": "equipment",
        "pattern": list(pattern),
        "key": key,
        "result": {result_key: f"velorise_emc:{tier}_resonant_{gear}", "count": 1},
    }


def smithing(tier: str, gear: str, result_key: str) -> dict:
    previous, template = UPGRADES[tier]
    return {
        "type": "minecraft:smithing_transform",
        "template": {"item": f"velorise_emc:{template}"},
        "base": {"item": f"velorise_emc:{previous}_resonant_{gear}"},
        "addition": {"item": f"velorise_emc:{tier}_resonant_ingot"},
        "result": {result_key: f"velorise_emc:{tier}_resonant_{gear}"},
    }


def cooking(tier: str, source: str, kind: str, neo: bool) -> dict:
    result: object = {"id": f"velorise_emc:{tier}_resonant_ingot"} if neo else f"velorise_emc:{tier}_resonant_ingot"
    return {
        "type": f"minecraft:{kind}",
        "category": "misc",
        "cookingtime": COOKING_TICKS[tier],
        "experience": EXPERIENCE[tier],
        "ingredient": {"item": f"velorise_emc:{source}"},
        "result": result,
    }


def main() -> None:
    targets = (
        (ROOT / "forge-1.20.1/src/main/resources/data/velorise_emc/recipes", "item", False),
        (ROOT / "neoforge-1.21.1/src/main/resources/data/velorise_emc/recipe", "id", True),
    )
    for recipe_root, result_key, neo in targets:
        for gear in GEAR:
            write(recipe_root / f"verdant_resonant_{gear}.json", shaped("verdant", gear, result_key))
            for tier in UPGRADES:
                write(recipe_root / f"{tier}_resonant_{gear}.json", smithing(tier, gear, result_key))
        for tier in TIERS:
            sources = [f"raw_{tier}_resonant", f"{tier}_resonant_ore"]
            if tier == "verdant":
                sources.append("deepslate_verdant_resonant_ore")
            for source in sources:
                for kind in ("smelting", "blasting"):
                    write(recipe_root / f"{tier}_resonant_ingot_from_{source}_{kind}.json", cooking(tier, source, kind, neo))


if __name__ == "__main__":
    main()
