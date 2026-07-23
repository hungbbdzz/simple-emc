#!/usr/bin/env python3
"""Static release guard for the current Velorise EMC alpha.61 source tree."""
from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
LOADERS = {
    "forge-1.20.1": {"recipe": "recipes", "loot": "loot_tables"},
    "neoforge-1.21.1": {"recipe": "recipe", "loot": "loot_table"},
}
TIERS = ("verdant", "infernal", "aetheric", "echoing")
GEAR = ("sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots")
COOKING_TICKS = {"verdant": 200, "infernal": 400, "aetheric": 800, "echoing": 1600}
RETIRED_IDS = {
    "alchemical_dust", "alchemical_shears", "tidal_heart", "deepbound_chest",
    "deepbound_barrel", "deepbound_shulker_box", "resonant_beacon",
    "restorative_hearth", "revealing_bell", "bottomless_bucket",
    "imbued_pickaxe", "resonant_smithing_forge", "verdant_ascension_template",
    "imbued_totem",
    "blaze_head", "ghast_head", "shulker_head", "guardian_head", "evoker_head",
    "drowned_head", "stray_head", "spider_head", "warden_head",
}


def read(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def load_json(path: Path):
    return json.loads(read(path))


def require(failures: list[str], path: Path) -> None:
    if not path.is_file():
        failures.append(f"missing required file: {path.relative_to(ROOT)}")


def marker(failures: list[str], path: Path, *values: str) -> None:
    require(failures, path)
    if not path.is_file():
        return
    text = read(path)
    for value in values:
        if value not in text:
            failures.append(f"{path.relative_to(ROOT)}: missing marker {value!r}")


def forbid(failures: list[str], path: Path, *values: str) -> None:
    if not path.is_file():
        return
    text = read(path)
    for value in values:
        if value in text:
            failures.append(f"{path.relative_to(ROOT)}: retired marker still present: {value!r}")


def registry_ids(entrypoint: Path, registry: str) -> list[str]:
    return re.findall(rf'{registry}\.register\("([^"]+)"', read(entrypoint))


def validate_json(failures: list[str]) -> None:
    for path in ROOT.rglob("*.json"):
        if ".git" in path.parts:
            continue
        try:
            load_json(path)
        except Exception as exc:
            failures.append(f"invalid JSON {path.relative_to(ROOT)}: {exc}")


def validate_versions(failures: list[str]) -> None:
    marker(failures, ROOT / "gradle.properties", "mod_version=1.9.0-alpha.61")
    marker(
        failures,
        ROOT / "common-core/src/main/java/com/velorise/emc/common/GameplayConfig.java",
        "CURRENT_VERSION = 23",
    )


def validate_registries_and_assets(failures: list[str]) -> None:
    parsed: dict[str, tuple[set[str], set[str]]] = {}
    for loader in LOADERS:
        simple = ROOT / loader / "src/main/java/com/velorise/emc/VeloriseEMC.java"
        require(failures, simple)
        if not simple.is_file():
            continue
        blocks = set(registry_ids(simple, "BLOCKS"))
        items = set(registry_ids(simple, "ITEMS"))
        parsed[loader] = (blocks, items)
        for retired in RETIRED_IDS:
            if retired in blocks or retired in items:
                failures.append(f"{loader}: retired registry ID still registered: {retired}")

    if len(parsed) == 2:
        forge_blocks, forge_items = parsed["forge-1.20.1"]
        neo_blocks, neo_items = parsed["neoforge-1.21.1"]
        if forge_blocks != neo_blocks:
            failures.append(f"loader block registries differ: {sorted(forge_blocks ^ neo_blocks)}")
        expected_item_delta = set()
        if forge_items ^ neo_items != expected_item_delta:
            failures.append(f"unexpected loader item registry difference: {sorted(forge_items ^ neo_items)}")

    lang_path = ROOT / "shared-resources/assets/velorise_emc/lang/en_us.json"
    require(failures, lang_path)
    lang = load_json(lang_path) if lang_path.is_file() else {}
    item_models = {p.stem for p in (ROOT / "shared-resources/assets/velorise_emc/models/item").glob("*.json")}
    blockstates = {p.stem for p in (ROOT / "shared-resources/assets/velorise_emc/blockstates").glob("*.json")}
    if parsed:
        blocks, items = parsed["neoforge-1.21.1"]
        for item_id in items:
            if item_id not in item_models:
                failures.append(f"missing item model: {item_id}")
            if f"item.velorise_emc.{item_id}" not in lang and f"block.velorise_emc.{item_id}" not in lang:
                failures.append(f"missing English item translation: {item_id}")
        for block_id in blocks:
            if block_id not in blockstates:
                failures.append(f"missing blockstate: {block_id}")
            if f"block.velorise_emc.{block_id}" not in lang:
                failures.append(f"missing English block translation: {block_id}")

    asset_root = ROOT / "shared-resources/assets/velorise_emc"
    for retired in RETIRED_IDS:
        for folder in ("models/item", "models/block", "blockstates", "textures/item", "textures/block"):
            for suffix in (".json", ".png"):
                path = asset_root / folder / f"{retired}{suffix}"
                if path.exists():
                    failures.append(f"retired asset still present: {path.relative_to(ROOT)}")



def validate_arcane_hand_models(failures: list[str]) -> None:
    model_root = ROOT / "shared-resources/assets/velorise_emc/models/item"
    base_path = model_root / "arcane_staff_handheld.json"
    require(failures, base_path)
    if base_path.is_file():
        base = load_json(base_path)
        if base.get("parent") != "minecraft:item/handheld":
            failures.append("arcane_staff_handheld.json: must inherit minecraft:item/handheld")
        display = base.get("display", {})
        expected_scales = {
            "thirdperson_righthand": [-0.85, 0.85, 0.85],
            "thirdperson_lefthand": [-0.85, 0.85, 0.85],
            "firstperson_righthand": [-0.68, 0.68, 0.68],
            "firstperson_lefthand": [-0.68, 0.68, 0.68],
        }
        for context, expected in expected_scales.items():
            transform = display.get(context)
            if not isinstance(transform, dict):
                failures.append(f"arcane_staff_handheld.json: missing {context} transform")
                continue
            if transform.get("scale") != expected:
                failures.append(
                    f"arcane_staff_handheld.json: {context} scale must be {expected} to mirror only held rendering"
                )

    staff_models = (
        "dragonfire_scepter", "resonant_echo", "inferno_rod", "ghastfire_staff",
        "wither_scepter", "shulker_seeker", "guardian_lens", "evoker_fang_staff",
        "stormcaller", "frostbolt_wand", "webcaster",
    )
    for staff in staff_models:
        path = model_root / f"{staff}.json"
        require(failures, path)
        if not path.is_file():
            continue
        model = load_json(path)
        if model.get("parent") != "velorise_emc:item/arcane_staff_handheld":
            failures.append(f"{path.relative_to(ROOT)}: Arcane Weapon must use shared mirrored hand model")
        if "display" in model:
            failures.append(f"{path.relative_to(ROOT)}: local display override would bypass shared hand rendering")


def validate_anvils(failures: list[str]) -> None:
    for roman, level in zip(("i", "ii", "iii", "iv"), range(1, 5)):
        for rel in (
            f"shared-resources/assets/velorise_emc/blockstates/alchemical_anvil_{roman}.json",
            f"shared-resources/assets/velorise_emc/models/block/alchemical_anvil_{roman}.json",
            f"shared-resources/assets/velorise_emc/models/item/alchemical_anvil_{roman}.json",
            f"shared-resources/assets/velorise_emc/textures/block/alchemical_anvil_{roman}.png",
        ):
            require(failures, ROOT / rel)
        for loader, dirs in LOADERS.items():
            require(
                failures,
                ROOT / loader / f"src/main/resources/data/velorise_emc/{dirs['loot']}/blocks/alchemical_anvil_{roman}.json",
            )
    for loader in LOADERS:
        base = ROOT / loader / "src/main/java/com/velorise/emc"
        marker(failures, base / "AlchemicalAnvilItem.java", "createStack(int level)", "roman(int level)")
        marker(
            failures,
            base / "VeloriseEMC.java",
            'register("alchemical_anvil_i"',
            'register("alchemical_anvil_ii"',
            'register("alchemical_anvil_iii"',
            'register("alchemical_anvil_iv"',
        )
        rework = base / "VanillaReworkEvents.java"
        marker(
            failures,
            rework,
            "return stationLevel == tier.level();",
            "AlchemicalAnvilItem.isAnvil(target)",
        )
        jei = base / "compat/jei/VeloriseEMCJeiPlugin.java"
        forbid(failures, jei, "AlchemicalAnvilItem.createStack(tier.level() - 1)", "ALCHEMICAL_ANVIL_ITEM")

    for loader, dirs in LOADERS.items():
        recipe_root = ROOT / loader / f"src/main/resources/data/velorise_emc/{dirs['recipe']}"
        previous = "minecraft:anvil"
        for roman, level in zip(("i", "ii", "iii", "iv"), range(1, 5)):
            recipe = recipe_root / f"alchemical_anvil_{roman}.json"
            require(failures, recipe)
            if recipe.is_file():
                text = recipe.read_text(encoding="utf-8")
                core_id = "velorise_emc:wireless_core" if level == 1 else f"velorise_emc:wireless_core_t{level}"
                if previous not in text or core_id not in text:
                    failures.append(f"{recipe.relative_to(ROOT)}: invalid exact-tier anvil progression ingredients")
            previous = f"velorise_emc:alchemical_anvil_{roman}"


def validate_ores_and_recipes(failures: list[str]) -> None:
    for loader, dirs in LOADERS.items():
        data = ROOT / loader / "src/main/resources/data/velorise_emc"
        recipe_root = data / dirs["recipe"]
        loot_root = data / dirs["loot"] / "blocks"
        ore_names = {
            "verdant": ("verdant_resonant_ore", "deepslate_verdant_resonant_ore"),
            "infernal": ("infernal_resonant_ore",),
            "aetheric": ("aetheric_resonant_ore",),
            "echoing": ("echoing_resonant_ore",),
        }
        for tier, ores in ore_names.items():
            for ore in ores:
                path = loot_root / f"{ore}.json"
                require(failures, path)
                if path.is_file():
                    obj = load_json(path)
                    text = json.dumps(obj)
                    if f"raw_{tier}_resonant" not in text:
                        failures.append(f"{path.relative_to(ROOT)}: must drop raw {tier}")
                    if '"max": 4' not in json.dumps(obj, separators=(",", ": ")):
                        failures.append(f"{path.relative_to(ROOT)}: Fortune cap must be 4")
                    if "minecraft:apply_bonus" not in text or "minecraft:limit_count" not in text:
                        failures.append(f"{path.relative_to(ROOT)}: missing Fortune 1-4 functions")

            sources = [f"raw_{tier}_resonant", f"{tier}_resonant_ore"]
            if tier == "verdant":
                sources.append("deepslate_verdant_resonant_ore")
            for source in sources:
                for kind in ("smelting", "blasting"):
                    path = recipe_root / f"{tier}_resonant_ingot_from_{source}_{kind}.json"
                    require(failures, path)
                    if not path.is_file():
                        continue
                    obj = load_json(path)
                    if obj.get("type") != f"minecraft:{kind}":
                        failures.append(f"{path.relative_to(ROOT)}: wrong cooking type")
                    if obj.get("cookingtime") != COOKING_TICKS[tier]:
                        failures.append(f"{path.relative_to(ROOT)}: expected cookingtime {COOKING_TICKS[tier]}")

            upgrade_sources = {
                "infernal": ("verdant", "infernal_ascension_template"),
                "aetheric": ("infernal", "end_ascension_template"),
                "echoing": ("aetheric", "deep_convergence_template"),
            }
            for gear in GEAR:
                path = recipe_root / f"{tier}_resonant_{gear}.json"
                require(failures, path)
                if path.is_file():
                    obj = load_json(path)
                    expected_type = "minecraft:crafting_shaped" if tier == "verdant" else "minecraft:smithing_transform"
                    if obj.get("type") != expected_type:
                        failures.append(f"{path.relative_to(ROOT)}: expected {expected_type}")
                    if tier != "verdant":
                        previous, template = upgrade_sources[tier]
                        if obj.get("base", {}).get("item") != f"velorise_emc:{previous}_resonant_{gear}":
                            failures.append(f"{path.relative_to(ROOT)}: must upgrade the previous equipment tier")
                        if obj.get("template", {}).get("item") != f"velorise_emc:{template}":
                            failures.append(f"{path.relative_to(ROOT)}: wrong ascension template")
                        if obj.get("addition", {}).get("item") != f"velorise_emc:{tier}_resonant_ingot":
                            failures.append(f"{path.relative_to(ROOT)}: wrong Resonant ingot addition")
                smithing = recipe_root / f"{tier}_resonant_{gear}_smithing.json"
                if smithing.exists():
                    failures.append(f"duplicate Resonant smithing recipe still present: {smithing.relative_to(ROOT)}")

    for loader in LOADERS:
        emc = ROOT / loader / "src/main/java/com/velorise/emc/EMCRegistry.java"
        for tier, raw, ingot in (
            ("VERDANT", "32_768L", "33_024L"),
            ("INFERNAL", "65_536L", "66_048L"),
            ("AETHERIC", "131_072L", "132_096L"),
            ("ECHOING", "262_144L", "264_192L"),
        ):
            marker(
                failures,
                emc,
                f"RAW_{tier}_RESONANT.get(), {raw}",
                f"{tier}_RESONANT_ORE_ITEM.get(), {raw}",
                f"{tier}_RESONANT_INGOT.get(), {ingot}",
            )


def validate_fuels(failures: list[str]) -> None:
    for loader, dirs in LOADERS.items():
        root = ROOT / loader / f"src/main/resources/data/velorise_emc/{dirs['recipe']}"
        recipes = (
            ("alchemical_coal", "minecraft:coal"),
            ("mobius_fuel", "velorise_emc:alchemical_coal"),
            ("aeternalis_fuel", "velorise_emc:mobius_fuel"),
        )
        for name, lower in recipes:
            path = root / f"{name}.json"
            require(failures, path)
            if not path.is_file():
                continue
            obj = load_json(path)
            ingredients = [entry.get("item") for entry in obj.get("ingredients", [])]
            if ingredients.count(lower) != 4 or ingredients.count("velorise_emc:philosophers_stone") != 1:
                failures.append(f"{path.relative_to(ROOT)}: expected four lower fuel plus Philosopher's Stone")
        stone = ROOT / loader / "src/main/java/com/velorise/emc/PhilosophersStoneItem.java"
        marker(failures, stone, "hasCraftingRemainingItem", "getCraftingRemainingItem")


def validate_removed_content(failures: list[str]) -> None:
    forbidden_classes = (
        "AlchemicalShearsItem.java", "DeepboundStorageBlock.java", "DeepboundStorageBlockEntity.java",
        "PortableDeepboundStorage.java", "RestorativeHearthBlock.java", "RestorativeHearthBlockEntity.java",
        "RevealingBellBlock.java", "RevealingBellBlockEntity.java",
        "TemporalClockProfile.java",
    )
    for loader in LOADERS:
        base = ROOT / loader / "src/main/java/com/velorise/emc"
        for name in forbidden_classes:
            if (base / name).exists():
                failures.append(f"retired class still present: {(base / name).relative_to(ROOT)}")
        simple = base / "VeloriseEMC.java"
        forbid(
            failures,
            simple,
            "ALCHEMICAL_DUST", "ALCHEMICAL_SHEARS", "TIDAL_HEART", "RESONANT_BEACON",
            "RESTORATIVE_HEARTH", "REVEALING_BELL", "BOTTOMLESS_BUCKET",
            "RESONANT_SMITHING_FORGE", "VERDANT_ASCENSION_TEMPLATE",
        )
        for java in base.rglob("*.java"):
            forbid(
                failures,
                java,
                "TEMPORAL_CLOCK", "TemporalClockProfile",
                "onCoreMiningSpeed", "miningSpeedMultiplier", "toolSpeedMultiplier",
            )


def validate_behavior_markers(failures: list[str]) -> None:
    marker(
        failures,
        ROOT / "common-gameplay/src/main/java/com/velorise/emc/PortableWorkstationItem.java",
        "fixed utility made with Core I", "never carry a level", "never scan nearby inventories",
    )
    for loader in LOADERS:
        marker(
            failures,
            ROOT / loader / "src/main/java/com/velorise/emc/PortableWorkstationUi.java",
            "PortableCraftingMenu", "PortableStonecutterMenu", "return player.isAlive()",
        )
    stonecutter = ROOT / "shared-resources/assets/velorise_emc/models/block/portable_stonecutter.json"
    require(failures, stonecutter)
    if stonecutter.is_file() and load_json(stonecutter).get("parent") != "minecraft:block/stonecutter":
        failures.append("portable_stonecutter model must inherit the Vanilla stonecutter to avoid the black blade")

    for loader in LOADERS:
        base = ROOT / loader / "src/main/java/com/velorise/emc"
        trans = base / "PassiveEmcTableBlockEntity.java"
        marker(failures, trans, "Passive EMC generation was removed")
        service = base / "PassiveEmcGenerationService.java"
        marker(failures, service, "tables no longer generate EMC", "Intentionally empty")
        rework = base / "VanillaReworkEvents.java"
        marker(
            failures,
            rework,
            "ATTRACTION_CATALYST", "EMC per use", "EMC per cast", "EMC per boost",
            "EMC per second", "Uncapped while in use", "Reload:",
        )
        cursed = base / "CursedIgniterItem.java"
        marker(failures, cursed, "CURSED_SOUL_FIRE", "SOUL_FIRE_FLAME")
        forbid(failures, cursed, "addCooldown")
        item_events = base / "VanillaItemUpgradeEvents.java"
        marker(failures, item_events, "Cursed Igniter is reloading", "addCooldown")
        if item_events.is_file() and re.search(r"@SubscribeEvent\s*@SubscribeEvent", read(item_events)):
            failures.append(f"{item_events.relative_to(ROOT)}: duplicated @SubscribeEvent annotation")

        table = base / "TransmutationTableBlock.java"
        marker(failures, table, "extends Block", "createBlockStateDefinition", "CORE_LEVEL")
        forbid(failures, table, "extends BaseEntityBlock", "getTicker(")

        simple = base / "VeloriseEMC.java"
        marker(failures, simple, "50%-500% zoom", "Math.max(0.20D, Math.min(2.0D, nextZoom))")
        forbid(failures, simple, "Math.max(0.0001D, farSightZoom")
        anvil_logic = rework
        marker(
            failures,
            anvil_logic,
            "MATTERBURST_TNT_ITEM", "MATTERBURST_MINECART_ITEM",
            "isAlreadyCoreUpgraded", "installedTier.level() != current.level() + 1",
        )
        forbid(failures, base / "SpecialUpgradeUtil.java", "IMBUED_TOTEM", "imbued_totem")
        forbid(failures, base / "VanillaItemUpgradeEvents.java", "ImbuedTotem", "imbued_totem")
        forbid(failures, rework, "IMBUED_TOTEM", "imbued_totem")

        if loader == "neoforge-1.21.1":
            forbid(failures, item_events, "setSecondsOnFire(")
            marker(failures, item_events, "igniteForSeconds(")
            forbid(failures, rework, "hasCustomHoverName()", "setHoverName(")
            marker(failures, rework, "DataComponents.CUSTOM_NAME")

    world_progress = ROOT / "common-gameplay/src/main/java/com/velorise/emc/WorldProgressScreen.java"
    marker(failures, world_progress, "for (Renderable renderable : this.renderables)")
    forbid(failures, world_progress, "super.render(graphics, mouseX, mouseY, partialTick)")



def validate_arcane_trophies_and_progression(failures: list[str]) -> None:
    marker(
        failures, ROOT / "common-core/src/main/java/com/velorise/emc/common/WirelessCoreProfile.java",
        "CONDENSATION_UNITS = 9",
    )
    for loader, dirs in LOADERS.items():
        recipe_root = ROOT / loader / f"src/main/resources/data/velorise_emc/{dirs['recipe']}"
        result_key = "item" if loader == "forge-1.20.1" else "id"
        for tier in ("i", "ii", "iii", "iv"):
            for name, ingredient in ((f"condensed_core_{tier}", "wireless_core" if tier == "i" else f"wireless_core_t{ {'ii':'2','iii':'3','iv':'4'}[tier] }"),
                                     (f"condensed_core_block_{tier}", f"condensed_core_{tier}")):
                path = recipe_root / f"{name}.json"
                require(failures, path)
                if not path.is_file():
                    continue
                obj = load_json(path)
                if obj.get("pattern") != ["CCC", "CCC", "CCC"]:
                    failures.append(f"{path.relative_to(ROOT)}: expected 3x3 condensation")
                if obj.get("key", {}).get("C", {}).get("item") != f"velorise_emc:{ingredient}":
                    failures.append(f"{path.relative_to(ROOT)}: wrong condensation ingredient")
                if obj.get("result", {}).get(result_key) != f"velorise_emc:{name}":
                    failures.append(f"{path.relative_to(ROOT)}: wrong condensation output")

        advancement_dir = "advancements" if loader == "forge-1.20.1" else "advancement"
        advancement_root = ROOT / loader / f"src/main/resources/data/velorise_emc/{advancement_dir}"
        all_advancements = {
            str(path.relative_to(advancement_root).with_suffix("")).replace("\\", "/")
            for path in advancement_root.rglob("*.json")
        }
        for tab in ("journey", "engineering", "world"):
            root = advancement_root / tab / "root.json"
            require(failures, root)
            if root.is_file() and "parent" in load_json(root):
                failures.append(f"{root.relative_to(ROOT)}: independent tab root must not declare a parent")
        for path in advancement_root.rglob("*.json"):
            obj = load_json(path)
            parent = obj.get("parent")
            if not parent or not parent.startswith("velorise_emc:"):
                continue
            parent_path = parent.split(":", 1)[1]
            if parent_path not in all_advancements:
                failures.append(f"{path.relative_to(ROOT)}: missing parent {parent}")

        arcane = {
            "dragonfire_scepter", "resonant_echo", "inferno_rod", "ghastfire_staff",
            "wither_scepter", "shulker_launcher", "guardian_lens", "evoker_fang_staff",
            "stormcaller", "frostbolt_wand", "webcaster", "arcane_arsenal",
        }
        for name in arcane:
            require(failures, advancement_root / "engineering" / f"{name}.json")

        arcane_easter = {
            "youre_fired", "well_done", "collateral_architecture", "eight_business_skulls",
            "skeleton_management", "ground_control", "air_mail", "dental_appointment",
            "highly_conductive", "fourcast", "cold_reception", "brain_freeze",
            "terms_and_conditions", "incognito_mode", "speak_up", "urban_renewal",
            "dragon_these_nuts", "elemental_disagreement", "crowd_control_department",
            "this_seems_personal",
        }
        for name in arcane_easter:
            require(failures, advancement_root / "engineering/easter" / f"{name}.json")
        for name in ("nine_to_one", "management_block", "overqualified", "full_promotion",
                     "core_values", "no_skipping_orientation"):
            require(failures, advancement_root / "journey" / f"{name}.json")
        for name in ("microwave_economics", "paid_by_the_second", "not_a_duplication_bug",
                     "industrial_revolution_again", "perfectly_balanced_invoice"):
            require(failures, advancement_root / "engineering" / f"{name}.json")
        require(failures, advancement_root / "world/one_boss_remaining.json")

    for loader in LOADERS:
        base = ROOT / loader / "src/main/java/com/velorise/emc"
        if (base / "ArcaneTrophyDrops.java").exists():
            failures.append(f"{loader}: retired ArcaneTrophyDrops.java remains")
        marker(
            failures, base / "ArcaneMilestoneTracker.java",
            "engineering/arcane_arsenal", "1 << 10",
        )
        marker(
            failures, base / "ArcaneAchievementTracker.java",
            "engineering/easter/highly_conductive", "engineering/easter/brain_freeze",
            "engineering/easter/this_seems_personal", "pulses >= 23",
        )
        marker(
            failures, base / "ArcaneWeaponEvents.java",
            "creeper.thunderHit", "markGhastfireArea", "markChargedCreeper",
        )


def validate_roadmap_inventory(failures: list[str]) -> None:
    roadmap = ROOT / "ROADMAP.md"
    marker(
        failures, roadmap,
        "1.9.0-alpha.61", "Complete block inventory",
        "Complete custom utility and weapon inventory", "Retired content",
    )
    if not roadmap.is_file():
        return
    text = read(roadmap)
    forge = ROOT / "forge-1.20.1/src/main/java/com/velorise/emc/VeloriseEMC.java"
    neo = ROOT / "neoforge-1.21.1/src/main/java/com/velorise/emc/VeloriseEMC.java"
    ids: set[str] = set()
    for simple in (forge, neo):
        if simple.is_file():
            ids.update(registry_ids(simple, "ITEMS"))
            ids.update(registry_ids(simple, "BLOCKS"))
    for registry_id in sorted(ids):
        if registry_id not in text:
            failures.append(f"ROADMAP.md: missing registered item/block ID: {registry_id}")

def validate_velorise_emc_references(failures: list[str]) -> None:
    for loader in LOADERS:
        java_root = ROOT / loader / "src/main/java"
        simple = java_root / "com/velorise/emc/VeloriseEMC.java"
        if not simple.is_file():
            continue
        declarations = set(re.findall(r"public\s+static\s+final\s+[^;=]+\s+([A-Z][A-Z0-9_]*)\s*=", read(simple)))
        for path in java_root.rglob("*.java"):
            text = read(path)
            for name in re.findall(r"VeloriseEMC\.([A-Z][A-Z0-9_]*)", text):
                if name not in declarations and name != "MODID":
                    failures.append(f"{path.relative_to(ROOT)}: unresolved VeloriseEMC.{name}")


def main() -> int:
    failures: list[str] = []
    validate_json(failures)
    validate_versions(failures)
    validate_registries_and_assets(failures)
    validate_arcane_hand_models(failures)
    validate_anvils(failures)
    validate_ores_and_recipes(failures)
    validate_fuels(failures)
    validate_removed_content(failures)
    validate_behavior_markers(failures)
    validate_arcane_trophies_and_progression(failures)
    validate_roadmap_inventory(failures)
    validate_velorise_emc_references(failures)

    failures = list(dict.fromkeys(failures))
    if failures:
        print("Source validation failed:")
        for failure in failures:
            print(f" - {failure}")
        return 1
    json_count = sum(1 for p in ROOT.rglob("*.json") if ".git" not in p.parts)
    print(f"Velorise EMC alpha.61 source validation OK: {json_count} JSON files checked.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
