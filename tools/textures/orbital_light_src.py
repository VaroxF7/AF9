#!/usr/bin/env python3
"""The Orbital Light Casing sprite (tools/textures/orbital_src/orbital_light_casing.png): the frame of the CosmicCore casing it
started from (orbital_light_base.png, see CREDITS-textures.md), recoloured to warm gunmetal, around the same lit-window glyph redrawn in gold and chrome.

Run from the repository root (needs Pillow):  python3 tools/textures/orbital_light_src.py
then tools/textures/orbital_casings.py for the animated strips and the connected-texture sheet (border 4 px, the 8 x 8 cell
inside repeats; the gold pixels are the only strongly red-over-blue ones, which orbital_casings.py animates).
"""
import colorsys
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/orbital_src')

HUE = 0.09                      # warm gunmetal, the frame colour of the Orbital Magnet Casing next to it (base frame: ~0.64)
DESAT = 0.35
# The glyph: the base casing's 8 x 8 lit-window glyph, redrawn. Lit tones as in the base: d bright, c mid (gold), i the inner
# ring (chrome here), k the core (gold); . the dark floor (the base's own dark pixels are kept where it was dark there too).
# A gold rail round the cell (rails of joined cells make a grid), a chrome ring inside it, a pair of gold sparks, and four gold bridges in
# a pinwheel that link rail, ring and core.
GLYPH = ['ccddddcc',
         'd..c...d',
         'd.iiii.d',
         'd.i.kicd',
         'dcik.i.d',
         'd.iiii.d',
         'd...c..d',
         'ccddddcc']
GOLD = [(255, 240, 156), (240, 192, 66), (196, 140, 40), (126, 86, 24)]       # bright .. shade
CHROME = [(255, 255, 255), (226, 230, 238), (180, 186, 200), (126, 132, 148)]
FLOOR = [(30, 31, 38), (36, 37, 45), (24, 25, 31)]


def recolour(px):
    r, g, b, a = px
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    r, g, b = colorsys.hsv_to_rgb(HUE, min(1.0, s * DESAT + 0.03), v)
    return (round(r * 255), round(g * 255), round(b * 255), a)


def lit(ch, k, m):
    """Tone of a lit pixel, bevelled bright upper left / shaded lower right, with a little grain."""
    if ch == 'i':                                          # chrome ring: bright top and left, shade bottom and right
        t = 0 if m == 2 else 3 if m == 5 else 1 if k == 2 else 2
        ramp = CHROME
    elif ch == 'k':                                        # core: a tiny gold lens
        t = 0
        ramp = GOLD
    else:                                                  # rails and bridges
        t = (0 if ch == 'd' else 1) + (1 if (k == 7 or m == 7) else 0)
        ramp = GOLD
    g = 1.0 + 0.04 * (((k * 7919 + m * 104729) % 13) / 6.0 - 1.0)
    return tuple(max(0, min(255, round(c * g))) for c in ramp[t])


def main():
    base = Image.open(os.path.join(SRC, 'orbital_light_base.png')).convert('RGBA')
    im = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            px = base.getpixel((x, y))
            if 4 <= x <= 11 and 4 <= y <= 11:
                k, m = x - 4, y - 4
                ch = GLYPH[m][k]
                if ch != '.':
                    im.putpixel((x, y), lit(ch, k, m) + (255,))
                    continue
                im.putpixel((x, y), FLOOR[(k * 3 + m * 5) % 3] + (255,))   # the dark floor, deeper than the base's so the metal reads
                continue
            im.putpixel((x, y), recolour(px))
    im.save(os.path.join(SRC, 'orbital_light_casing.png'))


if __name__ == '__main__':
    main()
