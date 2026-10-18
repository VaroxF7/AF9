#!/usr/bin/env python3
"""The Orbital Light Casing sprite (tools/textures/orbital_src/orbital_light_casing.png): the frame of the CosmicCore casing it
started from (orbital_light_base.png, see CREDITS-textures.md), recoloured indigo, around AF9's own coil window: glowing windings held by gunmetal straps.

Run from the repository root (needs Pillow):  python3 tools/textures/orbital_light_src.py
then tools/textures/orbital_casings.py for the animated strips and the connected-texture sheet (border 4 px, the 8 x 8 cell
inside repeats; every lit winding pixel has green > 150, which orbital_casings.py animates).
"""
import colorsys
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/orbital_src')

HUE = 0.68                      # deep indigo frame (the base frame is blue-violet, ~0.64)
# the window: a superconducting coil. Horizontal windings (bright edge, body, shaded underside, a dark groove: repeats every
# 4 px) held by a gunmetal strap with two rivets (every 8 px); the windings darken beside the strap.
WINDING = [(206, 252, 255), (96, 214, 255), (46, 138, 232), (14, 22, 44)]
STRAP = [(74, 78, 98), (118, 124, 148), (58, 62, 80), (30, 32, 42)]
RECESS = (46, 48, 58, 255)


def recolour(px):
    r, g, b, a = px
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    r, g, b = colorsys.hsv_to_rgb(HUE, min(1.0, s * 1.5 + 0.12), min(1.0, v * 1.08))
    return (round(r * 255), round(g * 255), round(b * 255), a)


def cell(k, m):
    row = m % 4
    if k in (3, 4):                                        # the strap
        if m in (1, 5):                                    # rivets
            return STRAP[1] + (255,) if k == 3 else STRAP[0] + (255,)
        if row == 3:
            return STRAP[3] + (255,)
        return STRAP[1 if k == 3 else 2] + (255,) if row != 0 else STRAP[0] + (255,)
    r, g, b = WINDING[row]
    if k in (2, 5) and row != 3:                           # shadow beside the strap
        r, g, b = r * 0.55, g * 0.65, b * 0.8
    return (round(r), round(g), round(b), 255)


def main():
    base = Image.open(os.path.join(SRC, 'orbital_light_base.png')).convert('RGBA')
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            if 4 <= x <= 11 and 4 <= y <= 11:
                k, m = x - 4, y - 4
                im.putpixel((x, y), cell(k, m))
            else:
                im.putpixel((x, y), recolour(base.getpixel((x, y))))
    im.save(os.path.join(SRC, 'orbital_light_casing.png'))


if __name__ == '__main__':
    main()
