#!/usr/bin/env python3
"""The Orbital Light Casing sprite (tools/textures/orbital_src/orbital_light_casing.png): the frame of the CosmicCore casing it
started from (orbital_light_base.png, see CREDITS-textures.md), recoloured teal, around AF9's own maze in a new glow colour.

Run from the repository root (needs Pillow):  python3 tools/textures/orbital_light_src.py
then tools/textures/orbital_casings.py for the animated strips and the connected-texture sheet (border 4 px, the 8 x 8 cell
inside repeats; every lit pixel has green > 150, which orbital_casings.py animates).
"""
import colorsys
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/orbital_src')

HUE = 0.50                      # teal (the base frame is blue-violet, ~0.64)
# the maze: 8 x 8 cell, 1 = dark. A spiral groove cut into the lit window, running in to a core and out through a notch.
MAZE = ['00000000',
        '01111110',
        '01000010',
        '01011010',
        '01010010',
        '01010110',
        '01000000',
        '00000000']
RECESS = (46, 48, 58, 255)      # the dark floor of the maze
GLOW = [(214, 255, 250), (140, 246, 240), (64, 214, 226), (34, 150, 190)]   # core -> rim


def recolour(px):
    r, g, b, a = px
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    r, g, b = colorsys.hsv_to_rgb(HUE, min(1.0, s * 1.5 + 0.12), min(1.0, v * 1.08))
    return (round(r * 255), round(g * 255), round(b * 255), a)


def lit(k, m):
    """Glow tone of a lit pixel: the rim of the cell darker, a bright edge on the groove's lower right."""
    if k in (0, 7) or m in (0, 7):
        return GLOW[2] + (255,)
    if MAZE[m - 1][k] == '1' or MAZE[m][k - 1] == '1':     # lit edge below / right of a groove
        return GLOW[0] + (255,)
    return GLOW[1] + (255,)


def main():
    base = Image.open(os.path.join(SRC, 'orbital_light_base.png')).convert('RGBA')
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            if 4 <= x <= 11 and 4 <= y <= 11:
                k, m = x - 4, y - 4
                im.putpixel((x, y), recolour(RECESS) if MAZE[m][k] == '1' else lit(k, m))
            else:
                im.putpixel((x, y), recolour(base.getpixel((x, y))))
    im.save(os.path.join(SRC, 'orbital_light_casing.png'))


if __name__ == '__main__':
    main()
