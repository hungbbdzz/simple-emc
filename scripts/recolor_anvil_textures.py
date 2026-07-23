#!/usr/bin/env python3
"""Apply UV-safe, color-only contrast palettes to the four anvil textures."""

from pathlib import Path
from PIL import Image, ImageEnhance

ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "shared-resources/assets/velorise_emc/textures/block"


def palette(path: Path, shadow: tuple[int, int, int], highlight: tuple[int, int, int]) -> None:
    image = Image.open(path).convert("RGBA")
    output = Image.new("RGBA", image.size)
    for x in range(image.width):
        for y in range(image.height):
            r, g, b, a = image.getpixel((x, y))
            luminance = (0.2126 * r + 0.7152 * g + 0.0722 * b) / 255.0
            amount = max(0.0, min(1.0, luminance ** 0.78))
            rgb = tuple(round(lo + (hi - lo) * amount) for lo, hi in zip(shadow, highlight))
            output.putpixel((x, y), (*rgb, a))
    output.save(path)


def contrast(path: Path) -> None:
    image = Image.open(path).convert("RGBA")
    alpha = image.getchannel("A")
    rgb = image.convert("RGB")
    rgb = ImageEnhance.Contrast(rgb).enhance(1.28)
    rgb = ImageEnhance.Brightness(rgb).enhance(0.88)
    rgb.putalpha(alpha)
    rgb.save(path)


def main() -> None:
    contrast(TEXTURES / "alchemical_anvil_i.png")
    contrast(TEXTURES / "alchemical_anvil_ii.png")
    palette(TEXTURES / "alchemical_anvil_iii.png", (15, 7, 24), (184, 54, 229))
    palette(TEXTURES / "alchemical_anvil_iv.png", (3, 10, 14), (27, 112, 119))


if __name__ == "__main__":
    main()
