#!/usr/bin/env python3
"""Energizing Orb Mk2: the texture of its base: dark plated steel with a cyan inlay and rivets (16 x 16).

Run from the repository root (needs Pillow):  python3 tools/textures/orb_mk2.py

Writes af9-core/src/main/resources/assets/af9/textures/block/energizing_orb_mk2.png
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/block/energizing_orb_mk2.png')

STEEL = (58, 64, 74)
STEEL_LIGHT = (96, 104, 118)
STEEL_DARK = (36, 40, 48)
INLAY = (40, 200, 235)
INLAY_DIM = (24, 120, 150)
RIVET = (150, 158, 170)


def main():
    img = Image.new('RGBA', (16, 16), STEEL + (255,))
    px = img.load()
    for i in range(16):
        # the bevel: light on the top and left edge, dark on the bottom and right
        px[i, 0] = STEEL_LIGHT + (255,)
        px[0, i] = STEEL_LIGHT + (255,)
        px[i, 15] = STEEL_DARK + (255,)
        px[15, i] = STEEL_DARK + (255,)
    # the inlay: a frame inside, with a gap in the middle of each side
    for i in range(3, 13):
        if 7 <= i <= 8:
            continue
        px[i, 3] = INLAY + (255,)
        px[i, 12] = INLAY + (255,)
        px[3, i] = INLAY + (255,)
        px[12, i] = INLAY + (255,)
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        px[x, y] = INLAY_DIM + (255,)
    # rivets in the corners
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        px[x, y] = RIVET + (255,)
    # the middle: a dim cross, the orb's seat
    for x, y in ((7, 7), (8, 7), (7, 8), (8, 8)):
        px[x, y] = INLAY + (255,)
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    img.save(OUT)
    print('orb mk2 texture written')


if __name__ == '__main__':
    main()
