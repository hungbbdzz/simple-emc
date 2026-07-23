import os
import urllib.request
from PIL import Image

# Directories
script_dir = os.path.dirname(__file__)
base_dir = os.path.join(script_dir, "vanilla_bases")
os.makedirs(base_dir, exist_ok=True)

temp_dir = os.path.join(script_dir, "..", "shared-resources", "assets", "velorise_emc", "temp")
block_dir = os.path.join(temp_dir, "block")
item_dir = os.path.join(temp_dir, "item")
armor_dir = os.path.join(temp_dir, "models", "armor")

os.makedirs(block_dir, exist_ok=True)
os.makedirs(item_dir, exist_ok=True)
os.makedirs(armor_dir, exist_ok=True)

RAW_URL_BASE = "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/"

# Base texture definitions to download
BASE_FILES = {
    # Items
    "diamond.png": "items/diamond.png",
    "raw_gold.png": "items/raw_gold.png",
    "ingot.png": "items/iron_ingot.png",
    "netherite_ingot.png": "items/netherite_ingot.png",
    "sword.png": "items/diamond_sword.png",
    "pickaxe.png": "items/diamond_pickaxe.png",
    "axe.png": "items/diamond_axe.png",
    "shovel.png": "items/diamond_shovel.png",
    "hoe.png": "items/diamond_hoe.png",
    "helmet.png": "items/diamond_helmet.png",
    "chestplate.png": "items/diamond_chestplate.png",
    "leggings.png": "items/diamond_leggings.png",
    "boots.png": "items/diamond_boots.png",

    # Netherite Tools & Armor (for Sculk / Tier 4 base if needed)
    "netherite_sword.png": "items/netherite_sword.png",
    "netherite_pickaxe.png": "items/netherite_pickaxe.png",
    "netherite_axe.png": "items/netherite_axe.png",
    "netherite_shovel.png": "items/netherite_shovel.png",
    "netherite_hoe.png": "items/netherite_hoe.png",
    "netherite_helmet.png": "items/netherite_helmet.png",
    "netherite_chestplate.png": "items/netherite_chestplate.png",
    "netherite_leggings.png": "items/netherite_leggings.png",
    "netherite_boots.png": "items/netherite_boots.png",
    
    # Blocks
    "stone.png": "blocks/stone.png",
    "deepslate.png": "blocks/deepslate_top.png",
    "netherrack.png": "blocks/netherrack.png",
    "end_stone.png": "blocks/end_stone.png",
    "diamond_ore.png": "blocks/diamond_ore.png",
    "deepslate_diamond_ore.png": "blocks/deepslate_diamond_ore.png",
    "nether_gold_ore.png": "blocks/nether_gold_ore.png",
    "ancient_debris_side.png": "blocks/ancient_debris_side.png",
    "block.png": "blocks/diamond_block.png",

    # Armor Models
    "armor_layer_1.png": "models/armor/diamond_1.png",
    "armor_layer_2.png": "models/armor/diamond_2.png",
    "netherite_layer_1.png": "models/armor/netherite_1.png",
    "netherite_layer_2.png": "models/armor/netherite_2.png",
}

def fetch_bases():
    print("--- Checking & Downloading Vanilla Base Textures from Mojang Bedrock Samples ---")
    for local_name, subpath in BASE_FILES.items():
        dst_path = os.path.join(base_dir, local_name)
        if not os.path.exists(dst_path):
            url = RAW_URL_BASE + subpath
            print(f"Downloading: {url}")
            try:
                req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
                with urllib.request.urlopen(req) as response, open(dst_path, 'wb') as f:
                    f.write(response.read())
            except Exception as e:
                print(f"Error downloading {local_name}: {e}")

# 4 Tier Color Palettes
TIER_PALETTES = {
    "verdant_resonant": {
        "dark": (6, 78, 59),       # Deep Forest Jade Rim
        "base": (5, 150, 105),     # Rich Emerald Jade
        "main": (16, 185, 129),    # Vibrant Mint Emerald
        "highlight": (110, 231, 183), # Bright Mint Glow
        "handle": (120, 53, 15)    # Bronze Oak Handle
    },
    "infernal_resonant": {
        "dark": (30, 20, 25),      # Obsidian Charcoal Rim
        "base": (185, 28, 28),     # Deep Fiery Crimson
        "main": (220, 38, 38),     # Lava Ruby Red
        "highlight": (245, 158, 11), # Molten Lava Gold Glow
        "handle": (40, 30, 35)     # Dark Obsidian Slate Handle
    },
    "aetheric_resonant": {
        "dark": (59, 7, 100),
        "base": (107, 33, 168),
        "main": (147, 51, 234),
        "highlight": (250, 204, 21),  # Chorus Gold Accent
        "handle": (215, 210, 160)  # End Stone light bone handle
    },
    "echoing_resonant": { # Tier 4: Dark Sculk Abyss Aesthetic
        "dark": (3, 12, 18),
        "base": (8, 36, 46),
        "main": (13, 148, 136),
        "highlight": (0, 245, 212),  # Electric Sculk Soul Glow
        "handle": (20, 30, 38)  # Deepslate Dark Bone Handle
    }
}

def is_wooden_handle(r, g, b):
    return (r > g) and (g > b) and (r < 180) and (g < 140) and (b < 100) and (r - b > 20)

def recolor_pixel_by_lum(r, g, b, palette):
    dk_r, dk_g, dk_b = palette["dark"]
    bs_r, bs_g, bs_b = palette["base"]
    mn_r, mn_g, mn_b = palette["main"]
    hl_r, hl_g, hl_b = palette["highlight"]

    lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0

    if lum < 0.35:
        t = lum / 0.35
        nr = int(dk_r * (1 - t) + bs_r * t)
        ng = int(dk_g * (1 - t) + bs_g * t)
        nb = int(dk_b * (1 - t) + bs_b * t)
    elif lum < 0.70:
        t = (lum - 0.35) / 0.35
        nr = int(bs_r * (1 - t) + mn_r * t)
        ng = int(bs_g * (1 - t) + mn_g * t)
        nb = int(bs_b * (1 - t) + mn_b * t)
    else:
        t = (lum - 0.70) / 0.30
        nr = int(mn_r * (1 - t) + hl_r * t)
        ng = int(mn_g * (1 - t) + hl_g * t)
        nb = int(mn_b * (1 - t) + hl_b * t)

    return (min(255, max(0, nr)), min(255, max(0, ng)), min(255, max(0, nb)))

def recolor_texture(src_path, dst_path, palette, preserve_handle=True):
    if not os.path.exists(src_path):
        return
    img = Image.open(src_path).convert("RGBA")
    pixels = img.load()
    w, h = img.size
    out_img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    out_pixels = out_img.load()

    custom_handle = palette.get("handle")

    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue

            if preserve_handle and is_wooden_handle(r, g, b):
                if custom_handle is not None:
                    # Apply custom handle color (e.g. Sculk slate / obsidian) with handle's original shading
                    lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                    hr, hg, hb = custom_handle
                    out_pixels[x, y] = (int(hr * lum * 1.3), int(hg * lum * 1.3), int(hb * lum * 1.3), a)
                else:
                    out_pixels[x, y] = (r, g, b, a)
                continue

            nr, ng, nb = recolor_pixel_by_lum(r, g, b, palette)
            out_pixels[x, y] = (nr, ng, nb, a)

    out_img.save(dst_path, "PNG")

def recolor_ore_vein_only(ore_path, bg_stone_path, dst_path, palette):
    """
    Recolors ONLY the mineral vein pixels while keeping 100% untouched background stone texture!
    """
    if not os.path.exists(ore_path) or not os.path.exists(bg_stone_path):
        return
    ore_img = Image.open(ore_path).convert("RGBA")
    stone_img = Image.open(bg_stone_path).convert("RGBA")
    
    ore_px = ore_img.load()
    stone_px = stone_img.load()
    w, h = ore_img.size

    out_img = Image.new("RGBA", (w, h), (0, 0, 0, 255))
    out_px = out_img.load()

    for y in range(h):
        for x in range(w):
            or_r, or_g, or_b, or_a = ore_px[x, y]
            st_r, st_g, st_b, st_a = stone_px[x, y]

            # Measure difference between ore pixel and background stone pixel
            diff = abs(or_r - st_r) + abs(or_g - st_g) + abs(or_b - st_b)

            if diff > 15:
                # Mineral vein pixel -> Recolor with Tier palette
                nr, ng, nb = recolor_pixel_by_lum(or_r, or_g, or_b, palette)
                out_px[x, y] = (nr, ng, nb, 255)
            else:
                # Background stone pixel -> Keep exact original stone texture!
                out_px[x, y] = (st_r, st_g, st_b, st_a)

    out_img.save(dst_path, "PNG")

def generate_all():
    fetch_bases()
    print("\n--- Generating Full Resonant Tier Assets (Ores, Blocks, Raw, Ingots, Tools, Armor) ---")

    for prefix, palette in TIER_PALETTES.items():
        print(f"\nProcessing Tier: {prefix}")

        # 1. Blocks & Ores
        stone_base = os.path.join(base_dir, "stone.png")
        deepslate_base = os.path.join(base_dir, "deepslate.png")
        netherrack_base = os.path.join(base_dir, "netherrack.png")
        end_stone_base = os.path.join(base_dir, "end_stone.png")
        dia_ore_base = os.path.join(base_dir, "diamond_ore.png")
        deepslate_dia_ore_base = os.path.join(base_dir, "deepslate_diamond_ore.png")

        if prefix == "verdant_resonant":
            # Tier 1: Overworld Stone Ore + Deepslate Ore (Vein-only recolor, stone untouched!)
            recolor_ore_vein_only(dia_ore_base, stone_base, os.path.join(block_dir, f"{prefix}_ore.png"), palette)
            recolor_ore_vein_only(deepslate_dia_ore_base, deepslate_base, os.path.join(block_dir, f"deepslate_{prefix}_ore.png"), palette)
            
        elif prefix == "infernal_resonant":
            # Tier 2: Netherrack Base + Fiery Lava/Crimson Veins (Ancient Debris / Nether Gold pattern)
            nether_gold_base = os.path.join(base_dir, "nether_gold_ore.png")
            recolor_ore_vein_only(nether_gold_base, netherrack_base, os.path.join(block_dir, f"{prefix}_ore.png"), palette)

        elif prefix == "aetheric_resonant":
            # Tier 3: End Stone Base + Ender Violet Crystal Veins
            recolor_ore_vein_only(dia_ore_base, stone_base, os.path.join(block_dir, f"{prefix}_ore.png"), palette)
            # Merge vein onto End Stone
            ore_temp = Image.open(os.path.join(block_dir, f"{prefix}_ore.png")).convert("RGBA")
            end_bg = Image.open(end_stone_base).convert("RGBA")
            o_px, e_px = ore_temp.load(), end_bg.load()
            st_px = Image.open(stone_base).convert("RGBA").load()
            for y in range(16):
                for x in range(16):
                    sr, sg, sb, _ = st_px[x, y]
                    r, g, b, _ = o_px[x, y]
                    if abs(r - sr) + abs(g - sg) + abs(b - sb) <= 15:
                        o_px[x, y] = e_px[x, y]
            ore_temp.save(os.path.join(block_dir, f"{prefix}_ore.png"), "PNG")

        elif prefix == "echoing_resonant":
            # Tier 4: Deepslate Base + Sculk Soul Teal Veins
            recolor_ore_vein_only(deepslate_dia_ore_base, deepslate_base, os.path.join(block_dir, f"deepslate_{prefix}_ore.png"), palette)
            # Copy to regular echoing_resonant_ore.png as well for consistency
            recolor_ore_vein_only(dia_ore_base, stone_base, os.path.join(block_dir, f"{prefix}_ore.png"), palette)

        # Storage Block
        recolor_texture(os.path.join(base_dir, "block.png"), os.path.join(block_dir, f"{prefix}_block.png"), palette, False)

        # 2. Items
        raw_base = os.path.join(base_dir, "raw_gold.png")
        ingot_base = os.path.join(base_dir, "netherite_ingot.png" if prefix == "echoing_resonant" else "ingot.png")
        
        recolor_texture(raw_base, os.path.join(item_dir, f"raw_{prefix}.png"), palette, False)
        recolor_texture(ingot_base, os.path.join(item_dir, f"{prefix}_ingot.png"), palette, False)

        # Tools & Armor (Use Netherite base for Tier 4 Sculk to ensure unique dark aesthetic!)
        tool_base_prefix = "netherite_" if prefix == "echoing_resonant" else ""
        
        for tool in ["sword", "pickaxe", "axe", "shovel", "hoe"]:
            tool_file = f"{tool_base_prefix}{tool}.png"
            recolor_texture(os.path.join(base_dir, tool_file), os.path.join(item_dir, f"{prefix}_{tool}.png"), palette, True)

        for piece in ["helmet", "chestplate", "leggings", "boots"]:
            piece_file = f"{tool_base_prefix}{piece}.png"
            recolor_texture(os.path.join(base_dir, piece_file), os.path.join(item_dir, f"{prefix}_{piece}.png"), palette, False)

        # Armor Models 3D
        arm_base = "netherite_" if prefix == "echoing_resonant" else "armor_"
        recolor_texture(os.path.join(base_dir, f"{arm_base}layer_1.png"), os.path.join(armor_dir, f"{prefix}_layer_1.png"), palette, False)
        recolor_texture(os.path.join(base_dir, f"{arm_base}layer_2.png"), os.path.join(armor_dir, f"{prefix}_layer_2.png"), palette, False)

    print("\nSUCCESS: All Resonant Tier textures generated successfully with exact stone preservation & Sculk aesthetic!")

if __name__ == "__main__":
    generate_all()
