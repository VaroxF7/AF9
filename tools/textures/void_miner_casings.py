#!/usr/bin/env python3
"""The casings of the Void Miner MK2 / MK3 structures (GTNH's: BartWorks' and GT++'s mining casings).

Run from the repository root (needs Pillow):  python3 tools/textures/void_miner_casings.py

GTNH's own art is in tools/textures/gtnh_void_miner/: the Mining Black Plutonium / Mining Neutronium casings and the
Black Plutonium item pipe casing are copied; the four bolted / rebolted casings are BartWorks' tinted per material at
runtime, so they are drawn here the same way: a plate in the material's colour, four bolts (bolted) or eight bolts
and a cross brace (rebolted).
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/gtnh_void_miner')
OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/block')


def shade(rgb, f):
    return tuple(max(0, min(255, round(c * f))) for c in rgb) + (255,)


def plate(rgb):
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            f = 1.0
            if x == 0 or y == 0:
                f = 1.18                       # lit edge
            elif x == 15 or y == 15:
                f = 0.72                       # shaded edge
            elif (x * 7 + y * 13) % 11 == 0:
                f = 0.93                       # a little grain
            im.putpixel((x, y), shade(rgb, f))
    return im


def bolt(im, cx, cy, rgb):
    for dx, dy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        im.putpixel((cx + dx, cy + dy), shade(rgb, 1.55))
    for dx, dy in ((-1, 0), (-1, 1), (2, 0), (2, 1), (0, -1), (1, -1), (0, 2), (1, 2)):
        im.putpixel((cx + dx, cy + dy), shade(rgb, 0.55))


def bolted(rgb):
    im = plate(rgb)
    for cx, cy in ((2, 2), (12, 2), (2, 12), (12, 12)):
        bolt(im, cx, cy, rgb)
    return im


def rebolted(rgb):
    im = plate(rgb)
    for i in range(3, 13):                     # the cross brace, a darker band with a lit upper edge
        for t in (-1, 0):
            for x, y in ((i, i + t), (15 - i, i + t)):
                if 0 < x < 15 and 0 < y < 15:
                    im.putpixel((x, y), shade(rgb, 0.62 if t == 0 else 1.25))
    for cx, cy in ((2, 2), (12, 2), (2, 12), (12, 12), (7, 1), (7, 13), (1, 7), (13, 7)):
        bolt(im, cx, cy, rgb)
    return im


def main():
    os.makedirs(OUT, exist_ok=True)
    for src, dst in (('MACHINE_CASING_MINING_BLACKPLUTONIUM', 'mining_black_plutonium_casing'),
                     ('MACHINE_CASING_MINING_NEUTRONIUM', 'mining_neutronium_casing'),
                     ('MACHINE_CASING_ITEM_PIPE_BLACK_PLUTONIUM', 'black_plutonium_item_pipe_casing')):
        Image.open(os.path.join(SRC, src + '.png')).convert('RGBA').save(os.path.join(OUT, dst + '.png'))
    naquadah, iridium = (0x3E, 0x5E, 0x4C), (0xD2, 0xD6, 0xE2)       # the materials' colours
    bolted(naquadah).save(os.path.join(OUT, 'bolted_naquadah_alloy_casing.png'))
    rebolted(naquadah).save(os.path.join(OUT, 'rebolted_naquadah_alloy_casing.png'))
    bolted(iridium).save(os.path.join(OUT, 'bolted_iridium_casing.png'))
    rebolted(iridium).save(os.path.join(OUT, 'rebolted_iridium_casing.png'))


if __name__ == '__main__':
    main()
