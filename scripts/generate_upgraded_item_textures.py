import os
from PIL import Image

target_dir = os.path.join(os.path.dirname(__file__), "..", "shared-resources", "assets", "velorise_emc", "textures", "item")
os.makedirs(target_dir, exist_ok=True)

def create_pixel_art(name, pixel_matrix, color_map):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels = img.load()
    
    for y in range(16):
        row = pixel_matrix[y]
        for x in range(16):
            char = row[x]
            if char in color_map and color_map[char] is not None:
                pixels[x, y] = color_map[char]
                
    file_path = os.path.join(target_dir, f"{name}.png")
    img.save(file_path, "PNG")
    print(f"Generated 16x16 item texture: {file_path}")

# ==============================================================================
# VANILLA SMITHING TEMPLATE SHAPE BASE MATRIX (16x16)
# Standard Minecraft Smithing Template Tablet outline with central engraving
# ==============================================================================

# 1. INFERNAL ASCENSION TEMPLATE (Nether Blackstone + Fiery Crimson Flame Emblem)
infernal_template_matrix = [
    "................",
    "....HHHHHHHH....",
    "...HBBBBBBBBH...",
    "..HBSSSSSSSSBH..",
    "..HBSFFSFFSSBH..",
    "..HBSFFFFFSSBH..",
    "..HBSFFFFFSSBH..",
    "..HBSFFSFFSSBH..",
    "..HBSFFSFFSSBH..",
    "..HBSSSSSSSSBH..",
    "..HBBBBBBBBBBH..",
    "...HBBBBBBBBH...",
    "....HHHHHHHH....",
    "................",
    "................",
    "................"
]

infernal_template_colors = {
    '.': None,
    'H': (28, 24, 30, 255),     # Dark Blackstone Edge Highlight
    'B': (52, 47, 56, 255),     # Mid Blackstone Base Tablet
    'S': (85, 78, 92, 255),     # Inner Carved Plate Surface
    'F': (249, 115, 22, 255)    # Fiery Crimson/Lava Flame Rune
}

# 2. END ASCENSION TEMPLATE (End Stone + Violet Ender Eye Emblem)
end_template_matrix = [
    "................",
    "....HHHHHHHH....",
    "...HBBBBBBBBH...",
    "..HBSSSSSSSSBH..",
    "..HBSVVEVVVVSBH.",
    "..HBSVEVEVVSBH..",
    "..HBSVEEEEVSBH..",
    "..HBSVEVEVVSBH..",
    "..HBSVVEVVVVSBH.",
    "..HBSSSSSSSSBH..",
    "..HBBBBBBBBBBH..",
    "...HBBBBBBBBH...",
    "....HHHHHHHH....",
    "................",
    "................",
    "................"
]

end_template_colors = {
    '.': None,
    'H': (150, 145, 100, 255),  # Shadow End Stone Edge
    'B': (215, 210, 160, 255),  # Mid End Stone Base Tablet
    'S': (245, 240, 195, 255),  # Light End Stone Surface
    'V': (126, 34, 206, 255),   # Deep Ender Purple Rune
    'E': (34, 211, 238, 255)    # Cyan Ender Crystal Center
}

# 3. DEEP CONVERGENCE TEMPLATE (Deepslate + Soul Aqua Sculk Emblem)
deep_template_matrix = [
    "................",
    "....HHHHHHHH....",
    "...HBBBBBBBBH...",
    "..HBSSSSSSSSBH..",
    "..HBSAAASAAASBH.",
    "..HBSAAAAAAASBH.",
    "..HBSAAASAAASBH.",
    "..HBSAAAAAAASBH.",
    "..HBSAAASAAASBH.",
    "..HBSSSSSSSSBH..",
    "..HBBBBBBBBBBH..",
    "...HBBBBBBBBH...",
    "....HHHHHHHH....",
    "................",
    "................",
    "................"
]

deep_template_colors = {
    '.': None,
    'H': (18, 22, 28, 255),     # Dark Deepslate Shadow Edge
    'B': (45, 53, 63, 255),     # Mid Deepslate Base Tablet
    'S': (80, 92, 108, 255),    # Light Deepslate Surface
    'A': (45, 212, 191, 255)    # Sculk Soul Teal Emblem
}

# ==============================================================================
# UPGRADED MAGIC ITEMS (Inferno Rod, Dragonfire Scepter, Resonant Echo)
# ==============================================================================
inferno_rod_matrix = [
    "....FF..........",
    "...FFF..........",
    "..FFFFO.........",
    "..FFFFOO........",
    "...FFOOO........",
    "....OOOOO.......",
    "....OGGG........",
    "....GGGGG.......",
    ".....GGGGG......",
    "......GGGGG.....",
    ".......GGGGG....",
    "........GGGGG...",
    ".........GGGS...",
    "..........GSS...",
    "...........S....",
    "................"
]
inferno_rod_colors = {
    '.': None,
    'F': (255, 230, 80, 255),
    'O': (245, 120, 20, 255),
    'G': (255, 190, 40, 255),
    'S': (180, 50, 20, 255)
}

dragonfire_scepter_matrix = [
    ".......GG.......",
    ".......GG.......",
    "......CCCC......",
    "......CPPC......",
    ".....CPPMMC.....",
    "....CPMMMMMC....",
    "....CMMMMMMC....",
    "....CMMMMMMC....",
    "....CPMMMMMC....",
    ".....CPMMMC.....",
    "......CCCC......",
    ".......GG.......",
    ".......GG.......",
    "........G.......",
    "................",
    "................"
]
dragonfire_scepter_colors = {
    '.': None,
    'G': (255, 215, 0, 255),
    'C': (220, 235, 255, 220),
    'P': (235, 100, 255, 255),
    'M': (140, 30, 180, 255)
}

resonant_echo_matrix = [
    "...AAA..........",
    "..AAAAA.........",
    ".AAATTT.........",
    ".AAATTTA........",
    "..AATTTA........",
    "...AAA.DD.......",
    "......DDDD......",
    ".....DDDDDD.....",
    "......DDDDDD....",
    ".......DDDDDD...",
    "........DDDDDD..",
    ".........DDDD...",
    "..........DD....",
    "................",
    "................",
    "................"
]
resonant_echo_colors = {
    '.': None,
    'A': (150, 255, 235, 255),
    'T': (15, 200, 175, 255),
    'D': (35, 45, 60, 255)
}

if __name__ == "__main__":
    # Templates
    create_pixel_art("infernal_ascension_template", infernal_template_matrix, infernal_template_colors)
    create_pixel_art("end_ascension_template", end_template_matrix, end_template_colors)
    create_pixel_art("deep_convergence_template", deep_template_matrix, deep_template_colors)
    
    # Magic Items
    create_pixel_art("inferno_rod", inferno_rod_matrix, inferno_rod_colors)
    create_pixel_art("dragonfire_scepter", dragonfire_scepter_matrix, dragonfire_scepter_colors)
    create_pixel_art("resonant_echo", resonant_echo_matrix, resonant_echo_colors)
    print("All template and magic item textures created successfully!")
