#!/usr/bin/env python3
"""The glass tube mold and the filled one (Create's spout fills it, the press empties it): 16 x 16 item textures.

A steel block, rivets in the corners and a long channel the shape of a tube; filled, the channel holds molten glass.
Writes assets/af9/textures/item/glass_tube_mold.png and glass_tube_mold_filled.png.
"""
import random
from pathlib import Path

from PIL import Image

OUT = Path(__file__).resolve().parents[2] / 'af9-core/src/main/resources/assets/af9/textures/item'
BASE = [(0x7B, 0x80, 0x85), (0x6F, 0x73, 0x79), (0x63, 0x67, 0x6D)]
EDGE = (0x41, 0x45, 0x4A)
SHADE = (0x29, 0x2C, 0x31)
CHANNEL = (0x1E, 0x20, 0x24)
GLASS = [(0xB8, 0xE6, 0xF5), (0x8E, 0xC8, 0xE8), (0xD8, 0xF4, 0xFF)]


def make(filled):
    rng = random.Random(7)
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    for y in range(2, 14):
        for x in range(1, 15):
            c = BASE[rng.randrange(3)]
            if x in (1, 14) or y in (2, 13):
                c = EDGE if y in (2,) or x == 1 else SHADE
            im.putpixel((x, y), c + (255,))
    for x, y in ((2, 3), (13, 3), (2, 12), (13, 12)):
        im.putpixel((x, y), (0xA8, 0xAD, 0xB2, 255))
    # the channel: a tube lying across the block
    for x in range(3, 13):
        for y in (6, 7, 8, 9):
            if filled:
                c = GLASS[(x + y) % 3] if y in (7, 8) else GLASS[1]
            else:
                c = CHANNEL if y in (7, 8) else SHADE
            im.putpixel((x, y), c + (255,))
    return im


OUT.mkdir(parents=True, exist_ok=True)
make(False).save(OUT / 'glass_tube_mold.png')
make(True).save(OUT / 'glass_tube_mold_filled.png')
print('wrote', OUT)
