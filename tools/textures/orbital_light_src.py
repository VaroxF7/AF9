#!/usr/bin/env python3
"""AF9's own Orbital Light Casing sprite (tools/textures/orbital_src/orbital_light_casing.png), drawn from scratch.

Run from the repository root (needs Pillow):  python3 tools/textures/orbital_light_src.py
then tools/textures/orbital_casings.py for the animated strips and the connected-texture sheet.

Layout, for the connected-texture sheet (tx = ty = 4, see space_elevator.py): a 4 px border (poured-concrete steel frame:
black outline, lit bevel on top/left, shaded on bottom/right, a tie-rod slot centred on every edge, a bolt in every corner)
around an 8 x 8 cell that repeats across joined blocks (its grid starts right after the border, see orbital_casings.py): an octagonal lens with a dark mullion and a white-hot core that
fades to amber (every lit pixel has red > 200, which orbital_casings.py animates).
"""
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
OUT = os.path.join(ROOT, 'tools/textures/orbital_src/orbital_light_casing.png')

OUTLINE = (17, 18, 23, 255)
INNER = (28, 29, 36, 255)
LIT = (92, 96, 114, 255)         # bevel on top / left
PLATE = (62, 65, 79, 255)
SHADE = (40, 42, 52, 255)        # bevel on bottom / right
SLOT = (20, 21, 27, 255)
BOLT_HI = (122, 127, 148, 255)
BOLT_LO = (30, 31, 39, 255)
MULLION = (84, 50, 34, 255)
CORE = [(255, 246, 220, 255), (255, 222, 160, 255), (255, 186, 98, 255), (246, 150, 60, 255)]


def border(x, y):
    dx, dy = min(x, 15 - x), min(y, 15 - y)
    d = min(dx, dy)
    on_top_left = (y < 8 if dy <= dx else x < 8)           # which side's bevel this pixel belongs to
    if d == 0:
        return OUTLINE
    if d == 3:
        return INNER
    if dx < 4 and dy < 4:                                  # corner: a bolt on the plate
        if (dx, dy) == (2, 2):
            return BOLT_HI
        if (dx, dy) in ((1, 2), (2, 1), (2, 3), (3, 2), (1, 1)):
            return BOLT_LO if (dx, dy) != (1, 1) else (LIT if on_top_left else SHADE)
    along = x if dy < dx else y
    if d == 2 and along in (7, 8):                         # tie-rod slot, centred on the edge
        return SLOT
    if d == 1:
        return LIT if on_top_left else SHADE
    return PLATE


def cell(k, m):
    """The 8 x 8 lens at (k, m): a mullion ring, chamfered inside, then the glow."""
    if k in (0, 7) or m in (0, 7) or (k, m) in ((1, 1), (6, 1), (1, 6), (6, 6)):
        return MULLION
    ring = max(abs(k - 3.5), abs(m - 3.5))                 # 0.5 .. 1.5 inside the ring
    return CORE[int(ring - 0.5) + (1 if abs(k - 3.5) + abs(m - 3.5) > 2.5 else 0)]


def main():
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            if 4 <= x <= 11 and 4 <= y <= 11:
                im.putpixel((x, y), cell(x - 4, y - 4))
            else:
                im.putpixel((x, y), border(x, y))
    im.save(OUT)


if __name__ == '__main__':
    main()
