import json
import os
from pathlib import Path

ROOT = Path(r"c:\Users\hunga\Documents\Coder\Minecraft\myMod\simple_emc")

lang_path = ROOT / "shared-resources/assets/velorise_emc/lang/en_us.json"
with open(lang_path, encoding="utf-8") as f:
    lang = json.load(f)

valid_mod_items = set()
for key in lang.keys():
    if key.startswith("item.velorise_emc."):
        valid_mod_items.add("velorise_emc:" + key[len("item.velorise_emc."):])
    elif key.startswith("block.velorise_emc."):
        valid_mod_items.add("velorise_emc:" + key[len("block.velorise_emc."):])

valid_mod_items.add("velorise_emc:wireless_core")
valid_mod_items.add("velorise_emc:wireless_core_t1")
valid_mod_items.add("velorise_emc:wireless_core_t2")
valid_mod_items.add("velorise_emc:wireless_core_t3")
valid_mod_items.add("velorise_emc:wireless_core_t4")

recipe_dirs = [
    ROOT / "forge-1.20.1/src/main/resources/data/velorise_emc/recipes",
    ROOT / "neoforge-1.21.1/src/main/resources/data/velorise_emc/recipe"
]

issues = []

for r_dir in recipe_dirs:
    if not r_dir.exists():
        continue
    for recipe_file in r_dir.glob("*.json"):
        with open(recipe_file, encoding="utf-8") as f:
            data = json.load(f)
        
        result = data.get("result")
        if isinstance(result, dict):
            res_id = result.get("item") or result.get("id")
            if res_id and res_id.startswith("velorise_emc:") and res_id not in valid_mod_items:
                issues.append(f"{recipe_file.relative_to(ROOT)}: invalid result item '{res_id}'")
        elif isinstance(result, str):
            if result.startswith("velorise_emc:") and result not in valid_mod_items:
                issues.append(f"{recipe_file.relative_to(ROOT)}: invalid result item '{result}'")

        key = data.get("key", {})
        for k_symbol, k_val in key.items():
            items = []
            if isinstance(k_val, dict):
                if "item" in k_val: items.append(k_val["item"])
            elif isinstance(k_val, list):
                for item_entry in k_val:
                    if isinstance(item_entry, dict) and "item" in item_entry:
                        items.append(item_entry["item"])
            for item in items:
                if item.startswith("velorise_emc:") and item not in valid_mod_items:
                    issues.append(f"{recipe_file.relative_to(ROOT)}: symbol '{k_symbol}' has invalid item '{item}'")

        ingredients = data.get("ingredients", [])
        for ing in ingredients:
            if isinstance(ing, dict) and "item" in ing:
                item = ing["item"]
                if item.startswith("velorise_emc:") and item not in valid_mod_items:
                    issues.append(f"{recipe_file.relative_to(ROOT)}: ingredient has invalid item '{item}'")

if issues:
    print(f"Found {len(issues)} recipe issues:")
    for issue in issues:
        print(" - " + issue)
else:
    print("ALL RECIPES AUDITED 100% CLEAN! No invalid item names found!")
