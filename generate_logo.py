from PIL import Image, ImageDraw, ImageFilter
import math

# Create image
width, height = 512, 512
img = Image.new('RGBA', (width, height), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# Draw background circle (dark purple-blue gradient effect)
center = (width // 2, height // 2)
radius = 240

# Create gradient-like effect with multiple circles
for i in range(radius, 0, -5):
    alpha = int(255 * (1 - i / radius))
    color = (60 + int(40 * i / radius), 30, 100 + int(55 * i / radius), alpha)
    draw.ellipse([center[0]-i, center[1]-i, center[0]+i, center[1]+i], fill=color)

# Draw glowing core (bright cyan/white)
for i in range(80, 0, -2):
    alpha = int(200 * (1 - i / 80))
    color = (100 + int(155 * (1 - i / 80)), 200, 255, alpha)
    draw.ellipse([center[0]-i, center[1]-i, center[0]+i, center[1]+i], fill=color)

# Draw inner bright circle
draw.ellipse([center[0]-60, center[1]-60, center[0]+60, center[1]+60], fill=(150, 220, 255, 200))

# Draw alchemical symbol (transmutation cross inside)
arm_length = 50
thickness = 8

# Horizontal bar
draw.rectangle([center[0]-arm_length, center[1]-thickness//2, center[0]+arm_length, center[1]+thickness//2], fill=(255, 200, 100, 255))

# Vertical bar  
draw.rectangle([center[0]-thickness//2, center[1]-arm_length, center[0]+thickness//2, center[1]+arm_length], fill=(255, 200, 100, 255))

# Draw decorative circles around center
for angle in range(0, 360, 45):
    rad = math.radians(angle)
    x = center[0] + int(120 * math.cos(rad))
    y = center[1] + int(120 * math.sin(rad))
    draw.ellipse([x-15, y-15, x+15, y+15], fill=(200, 100, 255, 180), outline=(255, 200, 100, 255))

# Draw outer ring
draw.ellipse([center[0]-240, center[1]-240, center[0]+240, center[1]+240], outline=(255, 150, 100, 200), width=8)

# Apply slight blur for glow effect
img = img.filter(ImageFilter.GaussianBlur(radius=2))

# Save
img.save('simple_emc_logo.png')
print('Logo created successfully: simple_emc_logo.png')
