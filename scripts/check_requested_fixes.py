#!/usr/bin/env python3
"""Static regression checks for the July 2026 recipes, menus, visuals and progression fix set."""

from __future__ import annotations

import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
GEAR = ("sword", "pickaxe", "axe", "shovel", "hoe", "helmet", "chestplate", "leggings", "boots")
UPGRADES = {
    "infernal": ("verdant", "infernal_ascension_template"),
    "aetheric": ("infernal", "end_ascension_template"),
    "echoing": ("aetheric", "deep_convergence_template"),
}
ARCANE_RECIPES = {
    'dragonfire_scepter': ([' BD', ' CF', 'S  '], {'B': 'minecraft:dragon_breath', 'D': 'minecraft:dragon_head', 'C': 'velorise_emc:wireless_core', 'F': 'minecraft:fire_charge', 'S': 'minecraft:stick'}),
    'resonant_echo': ([' ER', ' CE', 'S  '], {'E': 'minecraft:echo_shard', 'R': 'minecraft:recovery_compass', 'C': 'velorise_emc:wireless_core', 'S': 'minecraft:stick'}),
    'inferno_rod': ([' RR', ' CR', 'F  '], {'R': 'minecraft:blaze_rod', 'C': 'velorise_emc:wireless_core', 'F': 'minecraft:fire_charge'}),
    'ghastfire_staff': ([' FG', ' CP', 'S  '], {'F': 'minecraft:fire_charge', 'G': 'minecraft:ghast_tear', 'C': 'velorise_emc:wireless_core', 'P': 'minecraft:gunpowder', 'S': 'minecraft:stick'}),
    'wither_scepter': ([' HH', ' CH', 'S  '], {'H': 'minecraft:wither_skeleton_skull', 'C': 'velorise_emc:wireless_core', 'S': 'minecraft:stick'}),
    'shulker_seeker': ([' EK', ' CP', 'S  '], {'E': 'minecraft:end_rod', 'K': 'minecraft:shulker_shell', 'C': 'velorise_emc:wireless_core', 'P': 'minecraft:popped_chorus_fruit', 'S': 'minecraft:stick'}),
    'guardian_lens': ([' PH', ' CN', 'S  '], {'P': 'minecraft:prismarine_crystals', 'H': 'minecraft:heart_of_the_sea', 'C': 'velorise_emc:wireless_core', 'N': 'minecraft:nautilus_shell', 'S': 'minecraft:stick'}),
    'evoker_fang_staff': ([' BT', ' CE', 'S  '], {'B': 'minecraft:bone_block', 'T': 'minecraft:totem_of_undying', 'C': 'velorise_emc:wireless_core', 'E': 'minecraft:emerald_block', 'S': 'minecraft:stick'}),
    'stormcaller': ([' LT', ' CK', 'S  '], {'L': 'minecraft:lightning_rod', 'T': 'minecraft:trident', 'C': 'velorise_emc:wireless_core', 'K': 'minecraft:copper_block', 'S': 'minecraft:stick'}),
    'frostbolt_wand': ([' NB', ' CA', 'S  '], {'N': 'minecraft:snowball', 'B': 'minecraft:blue_ice', 'C': 'velorise_emc:wireless_core', 'A': 'minecraft:amethyst_shard', 'S': 'minecraft:stick'}),
    'webcaster': ([' WF', ' CW', 'S  '], {'W': 'minecraft:cobweb', 'F': 'minecraft:fermented_spider_eye', 'C': 'velorise_emc:wireless_core', 'S': 'minecraft:stick'}),
}


def require(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


def main() -> None:
    targets = (
        ("forge-1.20.1", "recipes", "item", False),
        ("neoforge-1.21.1", "recipe", "id", True),
    )
    for loader, folder, result_key, neo in targets:
        recipes = ROOT / loader / f"src/main/resources/data/velorise_emc/{folder}"
        for path in recipes.glob("*.json"):
            json.loads(path.read_text(encoding="utf-8"))
        for weapon, (pattern, ingredients) in ARCANE_RECIPES.items():
            value = json.loads((recipes / f"{weapon}.json").read_text(encoding="utf-8"))
            require(value["type"] == "minecraft:crafting_shaped", f"{loader}: {weapon} is not shaped")
            require(value["pattern"] == pattern, f"{loader}: wrong {weapon} layout")
            require(set(value["key"]) == set(ingredients), f"{loader}: wrong {weapon} ingredient symbols")
            for symbol, item_id in ingredients.items():
                require(value["key"][symbol]["item"] == item_id, f"{loader}: wrong {weapon} ingredient {symbol}")
            require(value["result"][result_key] == f"velorise_emc:{weapon}", f"{loader}: wrong {weapon} output")
        for gear in GEAR:
            verdant = json.loads((recipes / f"verdant_resonant_{gear}.json").read_text(encoding="utf-8"))
            require(verdant["type"] == "minecraft:crafting_shaped", f"{loader}: Verdant {gear} is not shaped")
            require(result_key in verdant["result"], f"{loader}: wrong Verdant result schema")
            for tier, (previous, template) in UPGRADES.items():
                value = json.loads((recipes / f"{tier}_resonant_{gear}.json").read_text(encoding="utf-8"))
                require(value["type"] == "minecraft:smithing_transform", f"{loader}: {tier} {gear} is not smithing")
                require(value["base"]["item"] == f"velorise_emc:{previous}_resonant_{gear}", f"{loader}: broken gear chain")
                require(value["template"]["item"] == f"velorise_emc:{template}", f"{loader}: wrong template")
                require(value["addition"]["item"] == f"velorise_emc:{tier}_resonant_ingot", f"{loader}: wrong addition")
        for tier in ("verdant", "infernal", "aetheric", "echoing"):
            for kind in ("smelting", "blasting"):
                path = recipes / f"{tier}_resonant_ingot_from_raw_{tier}_resonant_{kind}.json"
                value = json.loads(path.read_text(encoding="utf-8"))
                result = value["result"]
                require(isinstance(result, dict) == neo, f"{loader}: wrong cooking result schema in {path.name}")
                if neo:
                    require(result.get("id") == f"velorise_emc:{tier}_resonant_ingot", f"{loader}: wrong cooking output")

    active_roots = (
        ROOT / "common-core/src/main", ROOT / "common-gameplay/src/main",
        ROOT / "forge-1.20.1/src/main", ROOT / "neoforge-1.21.1/src/main", ROOT / "shared-resources",
    )
    for source_root in active_roots:
        for path in source_root.rglob("*"):
            if path.is_file() and path.suffix in {".java", ".json"}:
                text = path.read_text(encoding="utf-8")
                require("imbued_totem" not in text and "IMBUED_TOTEM" not in text, f"Totem reference remains: {path}")

    shared = ROOT / "common-gameplay/src/main/java/com/velorise/emc"
    require("player.distanceToSqr" in (shared / "AlchemicalAnvilMenu.java").read_text(), "Anvil validity override missing")
    jukebox = (shared / "ResonanceJukeboxScreen.java").read_text()
    require("Collected:" not in jukebox and "EmcTextFormat.compact" in jukebox, "Jukebox labels regressed")
    furnace = (shared / "ResonantFurnaceScreen.java").read_text()
    require("FURNACE_TEXTURE" in furnace and "statusText" not in furnace, "Furnace UI is not compact")
    for loader, _, _, _ in targets:
        base = ROOT / loader / "src/main/java/com/velorise/emc"
        portable = (base / "PortableWorkstationUi.java").read_text()
        require("PortableCraftingMenu" in portable and "return player.isAlive()" in portable, f"{loader}: portable validity missing")
        blocks = (base / "PortableWorkstationBlocks.java").read_text()
        require(blocks.count("PortableWorkstationUi.provider") == 3, f"{loader}: placed portable menus missing")
        rework = (base / "VanillaReworkEvents.java").read_text()
        require("isArcaneWeapon" in rework and "installedTier.level() != currentLevel + 1" in rework, f"{loader}: sequential arcane upgrades missing")
        require("return stationLevel == tier.level();" in rework
                and "Vanilla Anvils and lower/higher Alchemical tiers cannot" in rework,
                f"{loader}: exact Alchemical Anvil tier gate missing")
        require("onCoreMiningSpeed" not in rework and "miningSpeedMultiplier(held)" not in rework,
                f"{loader}: generic Core mining-speed hook still active")
        echobound_pickaxe = (base / "EchoboundPickaxeItem.java").read_text()
        require("getDestroySpeed" in echobound_pickaxe and "4096.0F" in echobound_pickaxe,
                f"{loader}: Echobound universal mining speed missing")
        weapon = (base / "ArcaneWeaponItem.java").read_text()
        require("advanceWebBurst" in weapon and "webBurstShots()" in weapon, f"{loader}: Webcaster burst missing")
        require("advanceFrostBurst" in weapon and "frostBurstUses(tier)" in weapon,
                f"{loader}: Frostbolt Core volley missing")
        require("double[] fangX = new double[count]" in weapon and "fangX[i], fangY[i], fangZ[i]" in weapon,
                f"{loader}: Evoker Fang waves must reuse the same positions")
        require("guardianEmcPerPulse" in weapon and "finishGuardianBeam" in weapon,
                f"{loader}: Guardian channel missing")
        jei = (base / "compat/jei/VeloriseEMCJeiPlugin.java").read_text()
        require("previousTier" in jei, f"{loader}: JEI higher-tier input missing")
        simple = (base / "VeloriseEMC.java").read_text()
        require("Math.max(0.20D, Math.min(2.0D, nextZoom))" in simple, f"{loader}: spyglass clamp missing")

    gameplay = (ROOT / "common-core/src/main/java/com/velorise/emc/common/GameplayConfig.java").read_text()
    require("webVenomDurationTicks = {120, 180, 240, 300}" in gameplay, "Webcaster venom duration curve regressed")
    require("webPoisonAmplifier = {0, 0, 0, 1}" in gameplay, "Webcaster poison strength curve regressed")
    require("CURRENT_VERSION = 23" in gameplay, "Gameplay schema was not advanced for the alpha.61 config cleanup")
    core_profile = (ROOT / "common-core/src/main/java/com/velorise/emc/common/WirelessCoreProfile.java").read_text()
    require("CONDENSATION_UNITS = 9" in core_profile, "Core condensation is not 9-to-1")
    for loader, folder, _, _ in targets:
        recipes = ROOT / loader / f"src/main/resources/data/velorise_emc/{folder}"
        for tier in ("i", "ii", "iii", "iv"):
            condensed = json.loads((recipes / f"condensed_core_{tier}.json").read_text())
            block = json.loads((recipes / f"condensed_core_block_{tier}.json").read_text())
            require(condensed["pattern"] == ["CCC", "CCC", "CCC"], f"{loader}: condensed Core {tier} is not 9-to-1")
            require(block["pattern"] == ["CCC", "CCC", "CCC"], f"{loader}: condensed Core block {tier} is not 9-to-1")
        source = ROOT / loader / "src/main/java/com/velorise/emc"
        require(not (source / "ArcaneTrophyDrops.java").exists(), f"{loader}: retired trophy drop handler remains")
        require((source / "ArcaneMilestoneTracker.java").exists(), f"{loader}: Arcane milestone tracker missing")
        emc = (source / "EMCRegistry.java").read_text()
        require("6_685_440L" in emc and "60_709_152L" in emc, f"{loader}: recursive Core EMC values regressed")

    require("TemporalClock" not in gameplay and "toolSpeedMultiplier" not in gameplay,
            "Retired Temporal Clock or generic Core tool speed remains in gameplay config")
    for loader, folder, _, _ in targets:
        recipes = ROOT / loader / f"src/main/resources/data/velorise_emc/{folder}"
        expected_anvils = {
            "alchemical_anvil_i": ("minecraft:anvil", "velorise_emc:wireless_core"),
            "alchemical_anvil_ii": ("velorise_emc:alchemical_anvil_i", "velorise_emc:wireless_core_t2"),
            "alchemical_anvil_iii": ("velorise_emc:alchemical_anvil_ii", "velorise_emc:wireless_core_t3"),
            "alchemical_anvil_iv": ("velorise_emc:alchemical_anvil_iii", "velorise_emc:wireless_core_t4"),
        }
        for name, pair in expected_anvils.items():
            recipe = json.loads((recipes / f"{name}.json").read_text())
            require(recipe["type"] == "minecraft:crafting_shapeless", f"{loader}: {name} is not shapeless")
            require(tuple(x["item"] for x in recipe["ingredients"]) == pair, f"{loader}: wrong {name} ingredients")

    hourglass_model = json.loads((ROOT / "shared-resources/assets/velorise_emc/models/block/alchemical_hourglass.json").read_text())
    hourglass_active = json.loads((ROOT / "shared-resources/assets/velorise_emc/models/block/alchemical_hourglass_active.json").read_text())
    require(len(hourglass_model.get("elements", [])) >= 12 and len(hourglass_active.get("elements", [])) >= 12,
            "Hourglass is not using the full 3D model")

    textures = ROOT / "shared-resources/assets/velorise_emc/textures/block"
    for roman in ("i", "ii", "iii", "iv"):
        require(Image.open(textures / f"alchemical_anvil_{roman}.png").size == (16, 16), f"Anvil {roman} is not 16x16")
    tier3 = Image.open(textures / "alchemical_anvil_iii.png").convert("RGB")
    tier4 = Image.open(textures / "alchemical_anvil_iv.png").convert("RGB")
    tier3_pixels = tier3.get_flattened_data()
    tier4_pixels = tier4.get_flattened_data()
    require(sum(b for _, _, b in tier3_pixels) > sum(g for _, g, _ in tier3_pixels), "Tier III is not purple")
    require(sum(g for _, g, _ in tier4_pixels) > sum(r for r, _, _ in tier4_pixels), "Tier IV is not deep-dark teal")
    item_textures = ROOT / "shared-resources/assets/velorise_emc/textures/item"
    for name in ("dragonfire_scepter", "ghastfire_staff", "wither_scepter", "shulker_seeker",
                 "guardian_lens", "alchemical_hourglass", "resonant_echo", "inferno_rod",
                 "evoker_fang_staff", "stormcaller", "frostbolt_wand", "webcaster"):
        require(Image.open(item_textures / f"{name}.png").size == (16, 16), f"{name} is not 16x16")
    require(not (item_textures / "gale_rod.png").exists(), "Gale Rod texture remains")
    require(not (ROOT / "shared-resources/assets/velorise_emc/models/item/gale_rod.json").exists(), "Gale Rod model remains")
    common = (ROOT / "common-core/src/main/java/com/velorise/emc/common/GameplayConfig.java").read_text()
    require("webBurstShots = 3" in common, "Webcaster is not a three-shot burst")
    require("frostBurstResetTicks = 40" in common, "Frostbolt burst reset window regressed")
    require("guardianEmcPerSecond = {24L, 48L, 96L, 192L}" in common
            and "guardianBeamDurationTicks = {120, 160, 200, 240}" in common,
            "Guardian channel balance regressed")
    require("echoDamage = {10.0F, 15.0F, 20.0F, 25.0F}" in common,
            "Resonant Echo damage curve regressed")
    require("witherBurstShots = 2" in common, "Wither Scepter two-cast burst regressed")
    require("ghastfireRadius = {3.0D, 4.0D, 5.0D, 6.0D}" in common,
            "Ghastfire radius scaling regressed")
    require("webDurationTicks = {300, 600, 900, 1_200}" in common,
            "Webcaster lifetime scaling regressed")
    require("shulkerDamage = {4.0F, 4.0F, 4.0F, 4.0F}" in common,
            "Shulker damage should stay fixed")
    equipment = (ROOT / "common-core/src/main/java/com/velorise/emc/common/ResonantEquipmentProfile.java").read_text()
    require("13.0F, 15.0F" in equipment, "Aetheric/Echoing damage cap regressed")
    require("aquaticWaterBreathingTicks = 1_200" in common, "Turtle helmet breathing is not 60 seconds")
    require("aquaticDolphinsGraceTicks = 600" in common, "Turtle helmet Dolphin's Grace is not 30 seconds")
    require("outputPerPlayer = {2_048L, 8_192L, 32_768L, 131_072L}" in common,
            "Resonance Spire reward output regressed")
    require("reserve = {1_228_800L, 4_915_200L, 19_660_800L, 78_643_200L}" in common,
            "Resonance Spire ten-minute reserve regressed")
    require("rechargeTicks = {12_000, 12_000, 12_000, 12_000}" in common,
            "Resonance Spire recharge cadence regressed")

    armor_dir = ROOT / "shared-resources/assets/velorise_emc/textures/models/armor"
    for tier in ("verdant", "infernal", "aetheric", "echoing"):
        for layer in (1, 2):
            require(Image.open(armor_dir / f"{tier}_resonant_layer_{layer}.png").size == (64, 32),
                    f"{tier} armor UV size changed")

    for loader, recipe_dir, _, _ in targets:
        recipe_root = ROOT / loader / "src/main/resources/data/velorise_emc" / recipe_dir
        require(not (recipe_root / "portable_stonecutter.json").exists(), f"{loader}: portable stonecutter is still craftable")
        require(not (recipe_root / "portable_loom.json").exists(), f"{loader}: portable loom is still craftable")
        require(not (recipe_root / "portable_cartography_table.json").exists(), f"{loader}: portable cartography table is still craftable")
        attraction = json.loads((recipe_root / "attraction_catalyst.json").read_text(encoding="utf-8"))
        mirror = json.loads((recipe_root / "magic_mirror.json").read_text(encoding="utf-8"))
        require(attraction["key"]["D"]["item"] == "minecraft:lapis_block", f"{loader}: Attraction Catalyst ingredient")
        require(mirror["key"]["B"]["item"] == "minecraft:glass", f"{loader}: Magic Mirror ingredient")

        source = ROOT / loader / "src/main/java/com/velorise/emc"
        emc = (source / "EMCRegistry.java").read_text(encoding="utf-8")
        require("RecipeType.SMITHING" in emc and "calculateCheapestInput(recipe.getIngredients()" in emc,
                f"{loader}: smithing EMC inputs are not accumulated")
        require("LEGACY_FALLBACK_MAP.getOrDefault(item, 1L)" not in emc,
                f"{loader}: synthetic 1 EMC fallback remains")
        require("if (legacyValue > 1L) map.put(item, legacyValue)" in emc,
                f"{loader}: unresolved Balanced values are unsafe")
        require("RESONANCE_SPIRE = BLOCKS.register(\"resonance_spire\"" in (source / "VeloriseEMC.java").read_text(),
                f"{loader}: Resonance Spire registry mismatch")
        rework = (source / "VanillaReworkEvents.java").read_text()
        require("canHarvestResonantOre" in rework and "Items.NETHERITE_PICKAXE" in rework,
                f"{loader}: resonant ore mining gates missing")

    active_text = "\n".join(
        file.read_text(encoding="utf-8")
        for root in active_roots for file in root.rglob("*")
        if file.is_file() and file.suffix in {".java", ".json"}
    )
    require("renewal_elixir" not in active_text.lower(), "Renewal Elixir remains in active sources")
    require("RedstoneControl" not in active_text, "Machine redstone mode remains")
    require("AURA_NODE" not in active_text, "Old Aura Node registry constant remains")
    for trophy_id in ("blaze_head", "ghast_head", "shulker_head", "guardian_head", "evoker_head",
                       "drowned_head", "stray_head", "spider_head", "warden_head"):
        require(trophy_id not in active_text, f"Retired custom trophy remains: {trophy_id}")

    for loader, advancement_dir in (("forge-1.20.1", "advancements"), ("neoforge-1.21.1", "advancement")):
        journey = ROOT / loader / "src/main/resources/data/velorise_emc" / advancement_dir / "journey"
        for advancement in ("verdant_resonance", "infernal_resonance", "aetheric_resonance",
                            "echoing_resonance", "echoing_armor_mastery"):
            require((journey / f"{advancement}.json").exists(), f"{loader}: missing {advancement} advancement")

    last_echo = (ROOT / "common-core/src/main/java/com/velorise/emc/common/LastEchoProfile.java").read_text()
    require("MIN_DISTANCE = 5_000" in last_echo and "MAX_DISTANCE = 10_000" in last_echo,
            "Last Echo outer-End annulus regressed")
    require("BASE_HEALTH = 4_000.0F" in last_echo and "CRYSTALS_BY_PHASE = {8, 6, 4, 0}" in last_echo,
            "Echobound Dragon phase balance regressed")
    require("HEART_EMC = 134_217_728L" in last_echo and "SCALE_EMC = 4_194_304L" in last_echo,
            "Last Echo reward EMC regressed")
    require("ECHOBOUND_DEFENSE = {12, 20, 16, 10}" in last_echo
            and "ECHOBOUND_INCOMING_DAMAGE_MULTIPLIER = 0.20F" in last_echo,
            "Echobound armor defense regressed")
    service = (ROOT / "common-gameplay/src/main/java/com/velorise/emc/LastEchoCitadelService.java").read_text()
    require("placeAirOnly" in service and "highSkyClear" in service,
            "Citadel mod-structure collision safeguards missing")
    require(Image.open(ROOT / "shared-resources/assets/velorise_emc/textures/entity/echobound_dragon.png").size == (64, 64),
            "Echobound Dragon entity UV is not 64x64")
    for name in ("eye_of_the_last_echo", "heart_of_the_last_echo", "echobound_dragon_scale"):
        require(Image.open(item_textures / f"{name}.png").size == (16, 16), f"{name} is not 16x16")
    for name in ("echobound_helmet", "echobound_chestplate", "echobound_leggings", "echobound_boots"):
        require(Image.open(item_textures / f"{name}.png").size == (16, 16), f"{name} is not 16x16")
    flight = (ROOT / "common-gameplay/src/main/java/com/velorise/emc/EchoboundArmorFlight.java").read_text()
    require("mayfly = true" in flight and "EchoboundFlightGranted" in flight,
            "Echobound full-set flight ownership missing")
    for loader, recipe_dir, advancement_dir in (
        ("forge-1.20.1", "recipes", "advancements"),
        ("neoforge-1.21.1", "recipe", "advancement"),
    ):
        data = ROOT / loader / "src/main/resources/data/velorise_emc"
        require((data / recipe_dir / "eye_of_the_last_echo.json").exists(), f"{loader}: Last Echo Eye recipe missing")
        for armor in ("helmet", "chestplate", "leggings", "boots"):
            recipe = json.loads((data / recipe_dir / f"echobound_{armor}.json").read_text())
            expected = "velorise_emc:heart_of_the_last_echo" if armor == "chestplate" else "velorise_emc:echobound_dragon_scale"
            require(recipe["addition"]["item"] == expected, f"{loader}: Echobound {armor} boss ingredient")
        require((data / advancement_dir / "last_echo/defeat_echobound_dragon.json").exists(),
                f"{loader}: Echobound Dragon advancement missing")
        require((data / advancement_dir / "last_echo/echobound_armor_mastery.json").exists(),
                f"{loader}: Echobound armor advancement missing")
        simple = (ROOT / loader / "src/main/java/com/velorise/emc/VeloriseEMC.java").read_text()
        require('literal("grant")' in simple and 'literal("place_citadel")' in simple and 'literal("summon_boss")' in simple,
                f"{loader}: Last Echo operator commands missing")
    for path in active_roots:
        for file in path.rglob("*"):
            if file.is_file() and file.suffix in {".java", ".json", ".md"}:
                require("gale_rod" not in file.read_text(encoding="utf-8").lower(), f"Gale Rod remains: {file}")
    print("Requested-fix static checks OK: progression, Last Echo, recipes, menus, balance and textures.")


if __name__ == "__main__":
    main()
