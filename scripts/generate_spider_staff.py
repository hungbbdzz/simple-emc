import zlib
import struct
import os

def create_png_rgba(width, height, pixel_data):
    """
    width: int
    height: int
    pixel_data: list of list of tuple (R, G, B, A) or list of tuple (R, G, B, A) length width*height
    """
    raw_data = bytearray()
    for y in range(height):
        raw_data.append(0) # Filter type 0 (None)
        for x in range(width):
            r, g, b, a = pixel_data[y * width + x]
            raw_data.extend([r, g, b, a])
    
    def make_chunk(chunk_type, data):
        length = len(data)
        chunk = struct.pack(">I", length) + chunk_type + data
        crc = zlib.crc32(chunk_type + data) & 0xffffffff
        chunk += struct.pack(">I", crc)
        return chunk

    png = bytearray(b"\x89PNG\r\n\x1a\n")
    # IHDR
    ihdr_data = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png.extend(make_chunk(b"IHDR", ihdr_data))
    # IDAT
    compressed_data = zlib.compress(bytes(raw_data), 9)
    png.extend(make_chunk(b"IDAT", compressed_data))
    # IEND
    png.extend(make_chunk(b"IEND", b""))
    return bytes(png)

HEX_COLORS = {
    ' ': (0, 0, 0, 0),             # Transparent
    
    # Staff Shaft & Chitin (Dark Purple Wood / Spider Bone)
    'K': (20, 16, 29, 255),        # #14101D Shadow Base / Outline
    'D': (37, 27, 46, 255),        # #251B2E Dark Chitin
    'M': (61, 45, 75, 255),        # #3D2D4B Medium Chitin
    'H': (89, 69, 107, 255),       # #59456B Chitin Highlight
    'L': (118, 93, 140, 255),      # #765D8C Light Chitin Highlight
    
    # Spider Orb / Gem / Eyes (Crimson / Ruby Glowing Core)
    'r': (90, 0, 18, 255),         # #5A0012 Deep Red Shadow
    'R': (168, 13, 38, 255),       # #A80D26 Crimson Red
    'G': (230, 25, 60, 255),       # #E6193C Glowing Ruby
    'E': (255, 30, 64, 255),       # #FF1E40 Spider Eye Red
    'S': (255, 89, 117, 255),      # #FF5975 Core Highlight
    'P': (255, 168, 182, 255),     # #FFA8B6 Core Spark
    
    # Venom Drip / Poison Gem Accents
    'v': (40, 160, 72, 255),       # #28A048 Dark Venom
    'V': (57, 211, 98, 255),       # #39D362 Glowing Venom
    'N': (118, 240, 155, 255),     # #76F09B Bright Venom Spark
    
    # Cobweb Threads & Accents
    'w': (176, 196, 222, 180),     # #B0C4DE Semitransparent Web
    'W': (200, 216, 232, 230),     # #C8D8E8 Soft Light Web
    'Y': (234, 244, 252, 255),     # #EAF4FC Bright Web Fiber
}

# 16x16 Art Layout (Rows 0 to 15, Cols 0 to 15)
# Design: A spider scepter with spider legs & web wrapped around a dark chitin handle, crowned by a crimson glowing spider gem with a drop of spider venom.
GRID = [
    "            D W ", # 0
    "          M D G S P ", # 1
    "         H E R G S D", # 2
    "        D D R P G M N", # 3
    "       W M K r D V v", # 4
    "      M H D Y M D   ", # 5
    "     D M H W        ", # 6
    "    K M H Y         ", # 7
    "    K M H W         ", # 8
    "   Y K M H          ", # 9
    "  W K M H           ", # 10
    " K D M              ", # 11
    "K D H               ", # 12
    "K D                 ", # 13
    "                    ", # 14
    "                    ", # 15
]

pixels = []
for y in range(16):
    row_str = GRID[y].ljust(16, ' ')
    for x in range(16):
        char = row_str[x]
        pixels.append(HEX_COLORS.get(char, (0, 0, 0, 0)))

png_bytes = create_png_rgba(16, 16, pixels)

base_dir = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
target_paths = [
    os.path.join(base_dir, "shared-resources", "assets", "velorise_emc", "textures", "item", "spider_staff.png"),
    os.path.join(base_dir, "art-reference", "spider_staff.png"),
]

for path in target_paths:
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(png_bytes)
    print(f"Wrote {len(png_bytes)} bytes to {path}")
