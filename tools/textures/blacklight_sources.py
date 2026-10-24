#!/usr/bin/env python3
"""The litho light-source blocks (KrF Excimer Laser, ArF Excimer Laser, EUV Light Source) restyled after GregTech's own
Blacklight (tools/textures/gt_src/blacklight.png, GregTech CEu Modern, LGPL-3.0, see CREDITS-textures.md): a dark tube with a
violet body. The palette is read from that sprite (violet body ramp, grey end caps, near-black housing); the blocks show
1 (KrF), 2 (ArF) or 3 (EUV) of its tubes lengthwise in a recessed dark housing. Version 1 of the line is GT's purple lamp.

Run from the repository root (needs Pillow):  python3 tools/textures/blacklight_sources.py
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/block')
SRC = Image.open(os.path.join(os.path.dirname(os.path.abspath(__file__)), 'gt_src/blacklight.png')).convert('RGBA')

# palette sampled from the Blacklight item: body highlight .. shade, cap light .. dark, housing
BODY = [(206, 50, 234), (175, 45, 218), (149, 38, 204), (124, 34, 189), (86, 43, 119)]
CAP = [(121, 115, 126), (83, 76, 92), (58, 52, 66)]
HOUSING = [(83, 76, 92), (58, 52, 66), (42, 36, 49), (30, 25, 35), (19, 15, 22)]
assert all(c in {p[:3] for p in SRC.getdata()} for c in BODY[:4] + CAP + HOUSING[:4]), 'palette drifted from the GT sprite'
HOT = (246, 214, 255)           # the tube's lit core line
HALO = (52, 22, 74)             # the slot floor beside a lit tube


def block(tubes):
    im = Image.new('RGBA', (16, 16), HOUSING[2] + (255,))
    px = im.load()
    for i in range(16):         # bevelled frame: light top/left, dark bottom/right
        px[i, 0] = px[0, i] = HOUSING[0]
        px[i, 1] = px[1, i] = HOUSING[1]
        px[i, 15] = px[15, i] = HOUSING[4]
        px[i, 14] = px[14, i] = HOUSING[3]
    for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):   # rivets
        px[x, y] = CAP[0]
    # recessed slot
    for y in range(3, 13):
        for x in range(3, 13):
            px[x, y] = HOUSING[4] if y in (3, 12) or x in (3, 12) else HOUSING[3]
    for rows in tubes:          # violet halo on the slot floor round each tube
        for y in range(rows[0] - 1, rows[-1] + 2):
            for x in range(4, 12):
                if px[x, y] == HOUSING[3] + (255,):
                    px[x, y] = HALO
    for rows in tubes:
        for j, y in enumerate(rows):
            for x in range(4, 12):
                px[x, y] = CAP[min(j, 2)] if x in (4, 11) else BODY[min(j, 3)]
        y = rows[min(1, len(rows) - 1)]
        for x in range(5, 11):  # lit core line
            px[x, y] = HOT
    return im


# 4 rows per tube (1 tube), 2 rows per tube (2), 1 row each (3); hot core on the middle of each tube
def make(n):
    if n == 1:
        t = [[5, 6, 7, 8]]
    elif n == 2:
        t = [[4, 5, 6], [8, 9, 10]]
    else:
        t = [[4, 5], [7, 8], [10, 11]]
    return block(t)


if __name__ == '__main__':
    for name, n in (('krf_excimer_laser', 1), ('arf_excimer_laser', 2), ('euv_light_source', 3)):
        make(n).save(os.path.join(OUT, name + '.png'))
        print('wrote', name)
