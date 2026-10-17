#!/usr/bin/env python3
"""The Rack Heat Vents (MK-I to MK-IV) for the Quantum Computer's racks: a vent grille in a steel frame, a stripe in
the colour of the tier. Original art; GTNH's rack vents are IC2's reactor vents, which this pack does not have.

Run from the repository root (needs Pillow):  python3 tools/textures/rack_vents.py
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/item')

FRAME = (58, 62, 70, 255)
FRAME_LIGHT = (104, 110, 122, 255)
FRAME_DARK = (30, 33, 40, 255)
SLOT = (14, 16, 20, 255)
# tier colours: air-blue, teal, amber, white-hot cyan
TIERS = [(86, 160, 220), (60, 200, 170), (240, 170, 60), (210, 245, 255)]


def vent(color):
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    px = img.load()
    for x in range(1, 15):
        for y in range(1, 15):
            px[x, y] = FRAME
    for i in range(1, 15):
        px[i, 1] = FRAME_LIGHT
        px[1, i] = FRAME_LIGHT
        px[i, 14] = FRAME_DARK
        px[14, i] = FRAME_DARK
    # the grille: five slots, the stripe above it
    for row in (5, 7, 9, 11, 13):
        for x in range(3, 13):
            px[x, row] = SLOT
    glow = color + (255,)
    shade = tuple(max(0, c - 70) for c in color) + (255,)
    for x in range(3, 13):
        px[x, 3] = glow
        px[x, 4] = shade
    # corner bolts
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
        px[x, y] = FRAME_LIGHT
    return img


for tier, color in enumerate(TIERS, start=1):
    vent(color).save(os.path.join(OUT, f'rack_heat_vent_mk{tier}.png'))
