#!/usr/bin/env python3
"""The Orbital Light Casing sprite (tools/textures/orbital_src/orbital_light_casing.png): the frame of the CosmicCore casing it
started from (orbital_light_base.png, see CREDITS-textures.md), recoloured to cold gunmetal, around AF9's own coil window: worn steel windings with glowing emitter slits and riveted straps.

Run from the repository root (needs Pillow):  python3 tools/textures/orbital_light_src.py
then tools/textures/orbital_casings.py for the animated strips and the connected-texture sheet (border 4 px, the 8 x 8 cell
inside repeats; the emitter slits are the only blue-dominant pixels, which orbital_casings.py animates, which orbital_casings.py animates).
"""
import colorsys
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/orbital_src')

HUE = 0.60                      # cold gunmetal frame (the base frame is blue-violet, ~0.64), mostly desaturated
DESAT = 0.45
# the window: a coil bank in worn steel. Per 4 rows: a bevelled steel winding, a glowing emitter slit, a shadowed winding,
# a dark groove; a riveted steel strap every 8 px; fine grain over all of it so nothing reads as flat colour.
STEEL = [(169, 169, 172), (120, 121, 126), (74, 75, 80), (36, 36, 40)]
SLIT = (74, 206, 236)
STRAP = [(229, 229, 232), (197, 197, 200), (145, 146, 150), (97, 98, 102), (49, 49, 53)]


def grain(k, m, amp):
    h = (k * 73856093 ^ m * 19349663 ^ 0x5bd1e995) & 0xffff
    return 1.0 + amp * ((h % 100) / 50.0 - 1.0)


def recolour(px):
    r, g, b, a = px
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    r, g, b = colorsys.hsv_to_rgb(HUE, min(1.0, s * DESAT + 0.06), v * 0.95)
    return (round(r * 255), round(g * 255), round(b * 255), a)


def shade(rgb, f):
    return tuple(max(0, min(255, round(c * f))) for c in rgb) + (255,)


def cell(k, m):
    row = m % 4
    if k in (3, 4):                                        # the strap, bevelled left-light / right-dark, with rivets
        if m in (1, 5):
            return shade(STRAP[0] if k == 3 else STRAP[3], grain(k, m, 0.04))
        tone = {0: 1 if row else 0, 1: 2 if row != 3 else 3}[k - 3] if row != 3 else 4
        return shade(STRAP[tone], grain(k, m, 0.05))
    if row == 1:                                           # the emitter slit: hot in the middle of the cell, cooler to the strap
        f = 1.0 + 0.35 * (k in (0, 1, 6, 7)) * -1 + 0.2 * (k in (2, 5))
        return shade(SLIT, f * grain(k, m, 0.06))
    f = 0.62 if k in (2, 5) and row != 3 else 1.0           # shadow beside the strap
    return shade(STEEL[row], f * grain(k, m, 0.07))


def main():
    base = Image.open(os.path.join(SRC, 'orbital_light_base.png')).convert('RGBA')
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            if 4 <= x <= 11 and 4 <= y <= 11:
                im.putpixel((x, y), cell(x - 4, y - 4))
            else:
                im.putpixel((x, y), recolour(base.getpixel((x, y))))
    im.save(os.path.join(SRC, 'orbital_light_casing.png'))


if __name__ == '__main__':
    main()
