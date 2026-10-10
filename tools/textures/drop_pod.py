#!/usr/bin/env python3
"""Draws the drop pod's texture (assets/af9/textures/entity/drop_pod.png, 128 x 192).

The boxes and their offsets are the table of af9-core's DropPodModel.createLayer: change one and change the other. Every box is
unfolded the way vanilla's cubes are (top and bottom over the four sides) and painted here by hand: riveted steel panels, a
white nose cone with an orange band, hazard stripes on the door and the restraint, soot under the thrusters.
Run:  python tools/textures/drop_pod.py
"""
import random
from pathlib import Path

from PIL import Image, ImageDraw

OUT = Path(__file__).resolve().parents[2] / 'af9-core/src/main/resources/assets/af9/textures/entity/drop_pod.png'
W, H = 128, 192

STEEL = (118, 124, 132)
STEEL_DARK = (78, 83, 90)
STEEL_LIGHT = (154, 160, 168)
WHITE = (226, 228, 230)
ORANGE = (222, 112, 36)
YELLOW = (240, 196, 36)
BLACK = (26, 26, 28)
SOOT = (44, 40, 38)
GLOW = (255, 150, 40)

# name: (w, h, d, u, v)
BOXES = {
    'base': (26, 3, 26, 0, 0),
    'canopy': (26, 3, 26, 0, 30),
    'back': (26, 34, 3, 0, 60),
    'side_left': (3, 22, 20, 60, 60),
    'side_right': (3, 22, 20, 0, 100),
    'nose1': (20, 6, 20, 48, 100),
    'nose2': (14, 6, 14, 56, 164),
    'nose3': (8, 8, 8, 84, 128),
    'post': (2, 35, 2, 0, 144),
    'door': (20, 28, 2, 10, 144),
    'restraint': (22, 3, 3, 56, 144),
    'thruster': (6, 5, 6, 56, 152),
}

rng = random.Random(9)
img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
draw = ImageDraw.Draw(img)


def faces(box):
    """The six faces of a box on the sheet: name -> (x, y, w, h)."""
    w, h, d, u, v = BOXES[box]
    return {
        'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d),
        'west': (u, v + d, d, h), 'north': (u + d, v + d, w, h),
        'east': (u + d + w, v + d, d, h), 'south': (u + 2 * d + w, v + d, w, h),
    }


def fill(rect, base, noise=10):
    x, y, w, h = rect
    for i in range(w):
        for j in range(h):
            n = rng.randint(-noise, noise)
            img.putpixel((x + i, y + j), tuple(max(0, min(255, c + n)) for c in base) + (255,))


def border(rect, color):
    x, y, w, h = rect
    draw.rectangle([x, y, x + w - 1, y + h - 1], outline=color + (255,))


def rivets(rect, step=6):
    x, y, w, h = rect
    for i in range(2, w - 1, step):
        for j in (1, h - 2):
            if 0 <= j < h: img.putpixel((x + i, y + j), STEEL_LIGHT + (255,))
    for j in range(2, h - 1, step):
        for i in (1, w - 2):
            if 0 <= i < w: img.putpixel((x + i, y + j), STEEL_LIGHT + (255,))


def hazard(rect, band=4):
    x, y, w, h = rect
    for i in range(w):
        for j in range(h):
            color = YELLOW if ((i + j) // band) % 2 == 0 else BLACK
            img.putpixel((x + i, y + j), color + (255,))


def paint_all(box, base, noise=8, edge=STEEL_DARK):
    for name, rect in faces(box).items():
        fill(rect, base, noise)
        border(rect, edge)
        if rect[2] > 8 and rect[3] > 8: rivets(rect)


# the hull: steel, the canopy and the nose in white
for box in ('base', 'back', 'side_left', 'side_right', 'post'):
    paint_all(box, STEEL)
paint_all('canopy', WHITE, 6, STEEL)
paint_all('nose1', WHITE, 6, STEEL)
paint_all('nose2', ORANGE, 8, (150, 70, 20))
paint_all('nose3', WHITE, 6, STEEL)

# the underside of the base and the thrusters: soot and glow
fill(faces('base')['bottom'], SOOT, 6)
paint_all('thruster', STEEL_DARK, 8, BLACK)
fill(faces('thruster')['bottom'], GLOW, 14)
x, y, w, h = faces('thruster')['bottom']
draw.rectangle([x + 1, y + 1, x + w - 2, y + h - 2], outline=(255, 220, 120, 255))

# the door: panelled, with hazard stripes along its outer face (south, +z) and a window slit
paint_all('door', STEEL_LIGHT, 6)
hazard(faces('door')['south'])
sx, sy, sw, sh = faces('door')['south']
draw.rectangle([sx + 4, sy + 4, sx + sw - 5, sy + 10], fill=(40, 70, 96, 255), outline=BLACK + (255,))
border(faces('door')['south'], BLACK)

# the restraint bar: hazard stripes all round
for rect in faces('restraint').values():
    hazard(rect, 3)

# a darker stripe at the foot of the hull and a lighter one at the top of the walls
for box in ('back', 'side_left', 'side_right'):
    for name in ('north', 'south', 'west', 'east'):
        x, y, w, h = faces(box)[name]
        draw.line([x, y + h - 2, x + w - 1, y + h - 2], fill=STEEL_DARK + (255,))
        draw.line([x, y + 1, x + w - 1, y + 1], fill=STEEL_LIGHT + (255,))

OUT.parent.mkdir(parents=True, exist_ok=True)
img.save(OUT)
print('wrote', OUT)
