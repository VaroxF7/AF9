#!/usr/bin/env python3
"""The Orbital Light Casing sprite (tools/textures/orbital_src/orbital_light_casing.png): the frame of the CosmicCore casing it
started from (orbital_light_base.png, see CREDITS-textures.md), recoloured to warm gunmetal, around AF9's own magnet window: chrome and gold conductor tracks held by riveted straps.

Run from the repository root (needs Pillow):  python3 tools/textures/orbital_light_src.py
then tools/textures/orbital_casings.py for the animated strips and the connected-texture sheet (border 4 px, the 8 x 8 cell
inside repeats; the gold tracks are the only strongly red-over-blue pixels, which orbital_casings.py animates, which orbital_casings.py animates).
"""
import colorsys
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/orbital_src')

HUE = 0.09                      # warm gunmetal frame (the base frame is blue-violet, ~0.64), mostly desaturated
DESAT = 0.3
# the window: conductor tracks of a magnet. Per 8 rows: a chrome track (bright edge, body, shaded edge), a dark gap, a gold
# track (same), a dark gap; held by a dark strap with chrome rivets every 8 px; fine grain over all of it.
CHROME = [(236, 238, 242), (190, 194, 202), (112, 116, 126)]
GOLD = [(255, 236, 150), (226, 176, 58), (140, 92, 24)]
GAP = (22, 21, 24)
STRAP = [(118, 118, 124), (88, 88, 94), (62, 62, 68), (30, 30, 34)]


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
    row = m % 8
    if k in (3, 4):                                        # the strap, bevelled left-light / right-dark, with chrome rivets
        if m in (1, 5):
            return shade(CHROME[0] if k == 3 else CHROME[2], grain(k, m, 0.04))
        return shade(STRAP[0 if k == 3 else 1] if row not in (3, 7) else STRAP[3], grain(k, m, 0.05))
    if row in (3, 7):
        return shade(GAP, grain(k, m, 0.1))
    tone = (CHROME if row < 3 else GOLD)[row % 4]
    f = 0.68 if k in (2, 5) else 1.0                       # shadow beside the strap
    return shade(tone, f * grain(k, m, 0.05))


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
