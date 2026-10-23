#!/usr/bin/env python3
"""The Water Filter Casing: GT's Watertight Casing and its connected sheet (gt_src/, LGPL-3.0, CREDITS-textures.md) with a
diagonal wire mesh (dark openings between lit wires) in place of the plate field. The frame, bevels and rivets are kept, so
it joins with the watertight casing around it; the mesh is 16 px periodic, so it runs on seamlessly across blocks.

Run from the repository root (needs Pillow):  python3 tools/textures/water_filter_casing.py
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/gt_src')
ASSETS = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9')
BLOCK = os.path.join(ASSETS, 'textures/block')
NAME = 'water_filter_casing'
HOLE = (14, 30, 38)


def lum(p):
    return sum(p[:3]) // 3


def scale(c, f):
    return tuple(max(0, min(255, round(v * f))) for v in c[:3])


def wire(x, y, c):
    a, b = (x + y) % 4, (x - y) % 4
    if a == 0 or b == 0:
        return scale(c, 1.45)
    if a == 2 and b == 2:
        return HOLE
    if a == 2 or b == 2:
        return (22, 48, 58)
    return (16, 36, 46)


def field_mask(im):
    """The plate field: runs of 3+ mid-tone pixels, leaving the frame beams (the sprite's top/bottom 3 rows; the sheet's beam rows)."""
    w, h = im.size
    mask = [[False] * w for _ in range(h)]
    for y in range(h):
        if (w == 16 and (y < 3 or y > 12)) or (w == 32 and y in (16, 17, 18, 19, 28, 29, 30, 31)):
            continue
        x = 0
        while x < w:
            if 58 <= lum(im.getpixel((x, y))) <= 72:
                x2 = x
                while x2 < w and 58 <= lum(im.getpixel((x2, y))) <= 72:
                    x2 += 1
                if x2 - x >= 3:
                    for k in range(x, x2):
                        mask[y][k] = True
                x = x2
            else:
                x += 1
    return mask


def meshed(im):
    im = im.convert('RGBA')
    out = im.copy()
    mask = field_mask(im)
    for y in range(im.height):
        for x in range(im.width):
            if mask[y][x]:
                out.putpixel((x, y), wire(x % 16, y % 16, im.getpixel((x, y))) + (255,))
    return out


def dump(path, data):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, 'w') as f:
        json.dump(data, f, indent=2)
        f.write('\n')


def main():
    meshed(Image.open(os.path.join(SRC, 'watertight_casing.png'))).save(os.path.join(BLOCK, NAME + '.png'))
    meshed(Image.open(os.path.join(SRC, 'watertight_casing_ctm.png'))).save(os.path.join(BLOCK, NAME + '_ctm.png'))
    dump(os.path.join(BLOCK, NAME + '.png.mcmeta'), {'ldlib': {'connection': 'af9:block/%s_ctm' % NAME}})
    dump(os.path.join(ASSETS, 'blockstates/%s.json' % NAME), {'variants': {'': {'model': 'af9:block/%s' % NAME}}})
    dump(os.path.join(ASSETS, 'models/block/%s.json' % NAME),
         {'parent': 'minecraft:block/cube_all', 'textures': {'all': 'af9:block/%s' % NAME}})
    dump(os.path.join(ASSETS, 'models/item/%s.json' % NAME), {'parent': 'af9:block/%s' % NAME})
    dump(os.path.join(ROOT, 'af9-core/src/main/resources/data/af9/loot_tables/blocks/%s.json' % NAME),
         {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item',
          'name': 'af9:' + NAME}], 'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})


if __name__ == '__main__':
    main()
