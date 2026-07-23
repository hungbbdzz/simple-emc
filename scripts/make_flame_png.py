import zlib
import struct
import os

width = 14
height = 14

# Define colors (100% transparent background)
T = (0, 0, 0, 0)
Y = (255, 235, 70, 255)   # Bright Yellow core
O = (255, 130, 20, 255)   # Vibrant Orange body
R = (225, 45, 15, 255)    # Flame tip Red

# 14x14 grid
grid = [[T]*14 for _ in range(14)]

# Left flame (cols 2,3,4, rows 4..13)
grid[4][3] = R
for y in range(5, 13):
    grid[y][3] = Y
    grid[y][2] = O
    grid[y][4] = O
grid[13][3] = O; grid[13][2] = R; grid[13][4] = R

# Center flame (cols 6,7,8, rows 2..13)
grid[2][7] = R
grid[3][7] = Y
for y in range(4, 13):
    grid[y][7] = Y
    grid[y][6] = O
    grid[y][8] = O
grid[13][7] = O; grid[13][6] = R; grid[13][8] = R

# Right flame (cols 10,11,12, rows 4..13)
grid[4][11] = R
for y in range(5, 13):
    grid[y][11] = Y
    grid[y][10] = O
    grid[y][12] = O
grid[13][11] = O; grid[13][10] = R; grid[13][12] = R

def make_png(w, h, grid_data):
    def chunk(c_type, data):
        c_len = len(data)
        crc = zlib.crc32(c_type + data) & 0xffffffff
        return struct.pack(">I", c_len) + c_type + data + struct.pack(">I", crc)

    header = b"\x89PNG\r\n\x1a\n"
    ihdr = chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))

    raw_data = bytearray()
    for row in grid_data:
        raw_data.append(0)
        for r, g, b, a in row:
            raw_data.extend([r, g, b, a])

    idat = chunk(b"IDAT", zlib.compress(bytes(raw_data)))
    iend = chunk(b"IEND", b"")
    return header + ihdr + idat + iend

png_bytes = make_png(14, 14, grid)

dest = r"shared-resources/assets/velorise_emc/textures/gui/burn_flame.png"
os.makedirs(os.path.dirname(dest), exist_ok=True)
with open(dest, "wb") as f:
    f.write(png_bytes)

print(f"Saved pure transparent burn_flame.png ({len(png_bytes)} bytes) successfully!")
