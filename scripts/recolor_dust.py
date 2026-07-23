import os
import urllib.request
from PIL import Image

target_dir = os.path.join(os.path.dirname(__file__), "..", "shared-resources", "assets", "velorise_emc", "textures", "item")
os.makedirs(target_dir, exist_ok=True)

# Official Mojang Bedrock Samples Repository URL for Glowstone Dust (16x16)
GLOWSTONE_DUST_URL = "https://raw.githubusercontent.com/Mojang/bedrock-samples/main/resource_pack/textures/items/glowstone_dust.png"
base_dust_path = os.path.join(os.path.dirname(__file__), "vanilla_glowstone_dust.png")

def fetch_vanilla_glowstone_dust():
    if not os.path.exists(base_dust_path):
        print(f"Downloading official Mojang Glowstone Dust texture from:\n{GLOWSTONE_DUST_URL}")
        try:
            req = urllib.request.Request(GLOWSTONE_DUST_URL, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(req) as response, open(base_dust_path, 'wb') as out_file:
                out_file.write(response.read())
            print("Successfully downloaded official vanilla_glowstone_dust.png!")
        except Exception as e:
            print(f"Failed to download online glowstone dust texture: {e}")
            return None
    return base_dust_path

def recolor_alchemical_dust(src_img_path):
    """
    Recolors official 16x16 Mojang Glowstone Dust texture based on its crafting ingredients:
    - Nether Quartz (White / Cream Quartz crystal dust)
    - Philosopher's Stone (Glowing Crimson / Ruby Red Alchemical energy)
    """
    img = Image.open(src_img_path).convert("RGBA")
    pixels = img.load()
    width, height = img.size

    out_img = Image.new("RGBA", (width, height), (0, 0, 0, 0))
    out_pixels = out_img.load()

    # Colors reflecting Nether Quartz + Philosopher's Stone
    # Dark shadow pixels -> Deep Alchemical Crimson
    shadow_rgb = (130, 15, 30)
    # Midtone pixels -> Philosopher's Stone Ruby Red
    ruby_rgb = (220, 38, 38)
    # High luminance / highlight pixels -> Nether Quartz Pale Cream / White
    quartz_rgb = (250, 242, 235)

    for y in range(height):
        for x in range(width):
            r, g, b, a = pixels[x, y]
            if a == 0:
                continue

            # Calculate pixel luminance (0.0 to 1.0)
            lum = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0

            if lum < 0.45:
                # Blend shadow to ruby
                t = lum / 0.45
                nr = int(shadow_rgb[0] * (1 - t) + ruby_rgb[0] * t)
                ng = int(shadow_rgb[1] * (1 - t) + ruby_rgb[1] * t)
                nb = int(shadow_rgb[2] * (1 - t) + ruby_rgb[2] * t)
            else:
                # Blend ruby to quartz white highlight
                t = (lum - 0.45) / 0.55
                nr = int(ruby_rgb[0] * (1 - t) + quartz_rgb[0] * t)
                ng = int(ruby_rgb[1] * (1 - t) + quartz_rgb[1] * t)
                nb = int(ruby_rgb[2] * (1 - t) + quartz_rgb[2] * t)

            out_pixels[x, y] = (min(255, max(0, nr)), min(255, max(0, ng)), min(255, max(0, nb)), a)

    out_file = os.path.join(target_dir, "alchemical_dust.png")
    out_img.save(out_file, "PNG")
    print(f"Generated recolored alchemical_dust texture: {out_file}")

if __name__ == "__main__":
    src_file = fetch_vanilla_glowstone_dust()
    if src_file:
        recolor_alchemical_dust(src_file)
    else:
        print("Could not obtain Vanilla Glowstone Dust base file.")
