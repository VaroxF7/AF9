#!/usr/bin/env python3
"""High-grade solder: a proper look for the plain-white auto texture. Cool silver with a satin diagonal
shine, faint brushed streaks and a whisper of bismuth iridescence (SAC-InBi solder: Sn9BiInAg).

Run from the repository root (needs Pillow):  python3 tools/textures/solder.py

Writes into af9-core/src/main/resources/assets/gtceu:
  textures/item/material_sets/solder/{dust,dust_small,dust_tiny,ingot,ingot_hot,nugget}_overlay.png
  (16 x 16, static) + empty.png, models/item/material_sets/solder/{same}.json
  (the icon set of AF9Materials: shapes the set lacks fall back to its parent, SHINY).
"""
import json
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
RES = os.path.join(ROOT, 'af9-core/src/main/resources/assets/gtceu')

# cool silver solder
OUT = (69, 75, 84, 255)
SHD = (124, 133, 146, 255)
MID = (188, 197, 209, 255)
LIT = (221, 228, 236, 255)
HI = (244, 248, 252, 255)
# a whisper of bismuth temper colour
PINK = (247, 184, 216, 255)
CYAN = (169, 228, 245, 255)
GOLD = (245, 223, 160, 255)
VIOLET = (201, 184, 245, 255)

# the same bar fresh out of the EBF
HOT_OUT = (74, 44, 26, 255)
HOT_SHD = (138, 74, 42, 255)
HOT_MID = (216, 138, 74, 255)
HOT_LIT = (245, 192, 128, 255)
HOT_HI = (255, 240, 208, 255)
GLOW = (255, 208, 112, 255)


def shade(pal, light):
    """0..1 (dark..bright) into the palette's four tones."""
    tones = [pal[3], pal[2], pal[1], pal[0]]
    return tones[max(0, min(3, int(light * 4)))]


def paint(rows, pal, accents=(), streaks=False, ridge=True):
    """A 16 x 16 sprite: rows are (y, x0, x1); light falls from the top left."""
    img = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    filled = set()
    for y, x0, x1 in rows:
        for x in range(x0, x1 + 1):
            filled.add((x, y))
    if not filled:
        return img
    top = min(y for _, y in filled)
    left = min(x for x, _ in filled)
    px = img.load()
    for x, y in filled:
        # bright at the top left, falling off down and right
        light = 1.0 - 0.62 * (y - top) / 9.0 - 0.30 * (x - left) / 9.0
        col = shade(pal, light)
        # satin grain: a seeded dither so the faces are not flat
        if (x * 5 + y * 11) % 7 in (0, 1):
            col = shade(pal, light - 0.22)
        px[x, y] = col
    if streaks:
        # brushed lines down the face, one light damar beside a dark one
        for x, y in filled:
            if y > top + 2:
                if x % 4 == 2:
                    px[x, y] = shade(pal, 0.30)
                elif x % 4 == 0:
                    px[x, y] = shade(pal, 0.78)
    # dark outline wherever the shape meets air
    for x, y in filled:
        if any((x + dx, y + dy) not in filled for dx in (-1, 0, 1) for dy in (-1, 0, 1)):
            px[x, y] = pal[0]
    if ridge:
        # light catches the ridge: the topmost pixel of every column
        by_col = {}
        for x, y in filled:
            by_col.setdefault(x, []).append(y)
        for x, ys in by_col.items():
            y = min(ys)
            px[x, y] = pal[3] if (x + y) % 2 == 0 else pal[2]
    for x, y, col in accents:
        px[x, y] = col
    return img


DUST = [(6, 7, 8), (7, 6, 9), (8, 5, 10), (9, 5, 10), (10, 4, 11), (11, 4, 11), (12, 3, 12), (13, 3, 12)]
DUST_SMALL = [(9, 7, 8), (10, 6, 9), (11, 6, 9), (12, 5, 10), (13, 5, 10)]
DUST_TINY = [(11, 7, 8), (12, 6, 9), (13, 6, 9)]
INGOT = [(5, 3, 12), (6, 2, 13), (7, 2, 13), (8, 2, 13), (9, 2, 13), (10, 3, 12)]
NUGGET = [(8, 6, 9), (9, 5, 10), (10, 5, 10), (11, 6, 9)]

PAL = (OUT, SHD, MID, LIT, HI)
HOT = (HOT_OUT, HOT_SHD, HOT_MID, HOT_LIT, HOT_HI)


def main():
    items = os.path.join(RES, 'textures/item/material_sets/solder')
    models = os.path.join(RES, 'models/item/material_sets/solder')
    for folder in (items, models):
        os.makedirs(folder, exist_ok=True)
    cold = {'pal': PAL, 'streaks': False}
    shapes = {
        'dust': (DUST, dict(cold, accents=[(5, 10, CYAN), (9, 8, PINK), (7, 12, VIOLET)])),
        'dust_small': (DUST_SMALL, dict(cold, accents=[(6, 11, CYAN), (8, 10, GOLD)])),
        'dust_tiny': (DUST_TINY, dict(cold, accents=[(7, 12, PINK)])),
        'ingot': (INGOT, dict(cold, streaks=True, ridge=False,
                              accents=[(9, 5, CYAN), (6, 5, HI), (7, 6, PINK), (10, 9, GOLD)])),
        'ingot_hot': (INGOT, {'pal': HOT, 'streaks': True, 'ridge': False,
                             'accents': [(6, 5, HOT_HI), (9, 5, GLOW), (5, 9, GLOW)]}),
        'nugget': (NUGGET, dict(cold, accents=[(8, 9, CYAN)])),
    }
    for name, (rows, kw) in shapes.items():
        paint(rows, kw['pal'], accents=kw.get('accents', ()),
              streaks=kw.get('streaks', False), ridge=kw.get('ridge', True)
              ).save(os.path.join(items, name + '_overlay.png'))
        with open(os.path.join(models, name + '.json'), 'w', newline='\n') as out:
            json.dump({'parent': 'item/generated', 'textures': {
                'layer0': 'gtceu:item/material_sets/solder/empty',
                'layer1': 'gtceu:item/material_sets/solder/empty',
                'layer2': 'gtceu:item/material_sets/solder/%s_overlay' % name}}, out, indent=2)
            out.write('\n')
    Image.new('RGBA', (16, 16), (0, 0, 0, 0)).save(os.path.join(items, 'empty.png'))
    print('solder textures written')


if __name__ == '__main__':
    main()
