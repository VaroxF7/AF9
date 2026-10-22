#!/usr/bin/env python3
"""XPS circuit board: GT's printed board shape (the wetware board), recoloured: a sand-and-dirt laminate traced in
sanguinite red, like the recipe (xps_alloy plates + sanguinite foil and wire).

Run from the repository root (needs Pillow). GT_SRC = a checkout of GregTech-Modern (LGPL-3.0, see CREDITS-textures.md):
  GT_SRC=/path/to/GregTech-Modern python3 tools/textures/xps_board.py
Writes af9-core/src/main/resources/assets/af9/textures/item/xps/xps_board.png (16 x 16).
"""
import colorsys
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(os.environ.get('GT_SRC', ''), 'src/main/resources/assets/gtceu/textures/item/wetware_printed_circuit_board.png')
OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/item/xps/xps_board.png')

# laminate: dark outline -> highlight (sand and dirt)
LAM = [(50, 40, 30), (96, 82, 62), (138, 124, 98), (172, 158, 130), (200, 188, 160)]
# traces: sanguinite, dark crimson -> bright blood red
TRACE = [(84, 8, 18), (130, 14, 28), (172, 22, 38), (206, 34, 48), (230, 62, 66)]


def ramp(pal, t):
    return pal[max(0, min(len(pal) - 1, int(t * len(pal))))]


def main():
    if not os.path.exists(SRC):
        sys.exit('set GT_SRC to a GregTech-Modern checkout (missing %s)' % SRC)
    src = Image.open(SRC).convert('RGBA')
    out = Image.new('RGBA', src.size, (0, 0, 0, 0))
    sp, op = src.load(), out.load()
    # brightness of every pixel, scaled per group (laminate / traces) so each uses its whole ramp
    groups = {False: [], True: []}
    pix = {}
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = sp[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            trace = 0.10 < h < 0.20 and s > 0.5 and v > 0.5  # the gold traces
            lum = 0.3 * r + 0.59 * g + 0.11 * b
            pix[(x, y)] = (trace, lum, a)
            groups[trace].append(lum)
    span = {k: (min(v), max(v)) for k, v in groups.items() if v}
    for (x, y), (trace, lum, a) in pix.items():
        lo, hi = span[trace]
        t = (lum - lo) / (hi - lo) if hi > lo else 0.5
        op[x, y] = ramp(TRACE if trace else LAM, min(0.999, t)) + (a,)
    out.save(OUT)


if __name__ == '__main__':
    main()
