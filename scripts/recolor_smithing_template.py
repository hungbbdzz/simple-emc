import os
import urllib.request
from PIL import Image

target_dir = os.path.join(os.path.dirname(__file__), "..", "shared-resources", "assets", "velorise_emc", "textures", "item")
os.makedirs(target_dir, exist_ok=True)

# Official Mojang Bedrock Samples Repository URL for Netherite Upgrade Smithing Template (16x16)
TEMPLATE_URL = "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/items/netherite_upgrade_smithing_template.png"
base_template_path = os.path.join(os.path.dirname(__file__), "vanilla_smithing_template.png")

def fetch_vanilla_template():
    if not os.path.exists(base_template_path):
        print(f"Downloading official Mojang Smithing Template texture from:\n{TEMPLATE_URL}")
        try:
            req = urllib.request.Request(TEMPLATE_URL, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req) as response, open(base_template_path, 'wb') as out_file:
                out_file.write(response.read())
            print("Successfully downloaded official vanilla_smithing_template.png!")
        except Exception as e:
            print(f"Failed to download online template texture: {e}")
            return None
    return base_template_path

def recolor_template(src_img_path, output_name, target_primary_rgb, target_accent_rgb):
    """
    Recolors the official 16x16 Mojang Smithing Template based on luminance & saturation.
    """
    img = Image.open(src_img_path).convert("RGBA")
    pixels = img.load()
    width, height = img.size

    out_img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    out_pixels = out_img.load()

    pr, pg, pb = target_primary_rgb
    ar, ag, ab = target_accent_rgb

    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue

            # Calculate pixel luminance (0.0 to 1.0)
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0

            # Determine if pixel is part of the center icon engraving (high red/warm saturation)
            is_engraving = (r > g + 15) and (r > b + 15)

            if is_engraving:
                # Recolor central emblem with bright accent color
                nr = min(255, max(0, int(ar * lum * 1.4)))
                ng = min(255, max(0, int(ag * lum * 1.4)))
                nb = min(255, max(0, int(ab * lum * 1.4)))
                out_pixels[x, y] = (nr, ng, nb, a)
            else:
                # Recolor base tablet stone with primary palette color
                nr = min(255, max(0, int(pr * lum * 1.2)))
                ng = min(255, max(0, int(pg * lum * 1.2)))
                nb = min(255, max(0, int(pb * lum * 1.2)))
                out_pixels[x, y] = (nr, ng, nb, a)

    out_file = os.path.join(target_dir, f"{output_name}.png")
    out_img.save(out_file, "PNG")
    print(f"Generated recolored template: {out_file}")

if __name__ == "__main__":
    src_file = fetch_vanilla_template()
    if src_file:
        # 0. Verdant Ascension Template (Stone Gray & Verdant Green Emblem)
        recolor_template(src_file, "verdant_ascension_template",
                         target_primary_rgb=(86, 96, 84),
                         target_accent_rgb=(70, 220, 118))

        # 1. Infernal Ascension Template (Nether Blackstone & Lava Orange Emblem)
        recolor_template(src_file, "infernal_ascension_template", 
                         target_primary_rgb=(65, 55, 68), 
                         target_accent_rgb=(255, 120, 20))

        # 2. End Ascension Template (End Stone & Ender Violet / Cyan Emblem)
        recolor_template(src_file, "end_ascension_template", 
                         target_primary_rgb=(215, 210, 160), 
                         target_accent_rgb=(168, 85, 247))

        # 3. Deep Convergence Template (Deepslate & Sculk Soul Aqua Teal Emblem)
        recolor_template(src_file, "deep_convergence_template", 
                         target_primary_rgb=(50, 60, 72), 
                         target_accent_rgb=(20, 184, 166))
    else:
        print("Could not obtain Vanilla template base file.")
