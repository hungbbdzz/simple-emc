import os
from PIL import Image

target_dirs = [
    os.path.join(os.path.dirname(__file__), "..", "shared-resources", "assets", "velorise_emc", "textures", "block"),
    os.path.join(os.path.dirname(__file__), "..", "forge-1.20.1", "build", "resources", "main", "assets", "velorise_emc", "textures", "block")
]

# Define 4-tier unified color palettes
# Format: (DarkRim 'D', DarkBase 'B', MainColor 'M', BrightHighlight 'H', CoreGlow 'G')
TIER_PALETTES = {
    1: {  # Tier I - Overworld Emerald Green
        'D': (10, 45, 20, 255),
        'B': (20, 83, 45, 255),
        'M': (34, 197, 94, 255),
        'H': (74, 222, 128, 255),
        'G': (187, 247, 208, 255)
    },
    2: {  # Tier II - Infernal Crimson / Lava Orange
        'D': (45, 10, 15, 255),
        'B': (127, 29, 29, 255),
        'M': (220, 38, 38, 255),
        'H': (249, 115, 22, 255),
        'G': (254, 215, 170, 255)
    },
    3: {  # Tier III - End Purple & Chorus Gold
        'D': (30, 10, 50, 255),
        'B': (88, 28, 135, 255),
        'M': (147, 51, 234, 255),
        'H': (192, 132, 252, 255),
        'G': (253, 224, 71, 255)
    },
    4: {  # Tier IV - Echo Sculk Teal & Aqua Soul
        'D': (6, 30, 40, 255),
        'B': (14, 116, 144, 255),
        'M': (6, 182, 212, 255),
        'H': (45, 212, 191, 255),
        'G': (207, 250, 254, 255)
    }
}

# 16x16 Unified Square Core Socket Matrix (No protruding dashes)
# . = Transparent
# D = Dark Outer Rim
# B = Dark Base Ring
# M = Main Color Ring
# H = Bright Highlight Ring
# G = Pure Core Glow Center
matrix_16x16 = [
    "................",
    "................",
    "..DDDDDDDDDDDD..",
    "..DBBBBBBBBBBD..",
    "..DBMMMMMMMMBD..",
    "..DBMHHHHHHMBD..",
    "..DBMHGGGGHMBD..",
    "..DBMHGGGGHMBD..",
    "..DBMHGGGGHMBD..",
    "..DBMHGGGGHMBD..",
    "..DBMHHHHHHMBD..",
    "..DBMMMMMMMMBD..",
    "..DBBBBBBBBBBD..",
    "..DDDDDDDDDDDD..",
    "................",
    "................"
]

def generate_sockets():
    for tier, palette in TIER_PALETTES.items():
        img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
        pixels = img.load()
        
        for y in range(16):
            row = matrix_16x16[y]
            for x in range(16):
                char = row[x]
                if char in palette:
                    pixels[x, y] = palette[char]
                    
        file_name = f"core_socket_t{tier}.png"
        for t_dir in target_dirs:
            if os.path.exists(t_dir):
                out_path = os.path.join(t_dir, file_name)
                img.save(out_path, "PNG")
                print(f"Generated unified socket texture (Tier {tier}): {out_path}")

if __name__ == "__main__":
    generate_sockets()
