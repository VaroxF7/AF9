#!/usr/bin/env python3
"""Ceramic Circuit Boards: GT's plastic board and printed plastic board redrawn as fired ceramic, plus the three substrates.

GT's silhouette and shading are kept; the colour is ivory ceramic (blank board) with copper traces (printed board), the
design changes a little: two clipped corners (a cut tile, not a moulded sheet) and four drilled holes. The substrates
(silicon carbide, alumina, beryllia) are the same tile in the ceramic's own colour with a strip of the metal foil it was
fired onto along the lower edge.

Run from the repository root (needs Pillow). GT_SRC = a checkout of GregTech-Modern (LGPL-3.0, see CREDITS-textures.md):
  GT_SRC=/path/to/GregTech-Modern python3 tools/textures/ceramic_boards.py
Writes kubejs/assets/gtceu/textures/item/plastic_circuit_board.png and plastic_printed_circuit_board.png (they replace
GT's), and af9-core/src/main/resources/assets/af9/textures/item/ceramic/*_substrate.png (16 x 16).
"""
import colorsys
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
GT_TEX = os.path.join(os.environ.get('GT_SRC', ''), 'src/main/resources/assets/gtceu/textures/item')
GT_OUT = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/item')
AF9_OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/item/ceramic')

# ceramic: dark outline -> highlight, ivory with a cool shadow
IVORY = [(74, 70, 72), (122, 118, 120), (170, 166, 164), (208, 203, 194), (238, 233, 220)]
# copper traces, dark -> bright
COPPER = [(86, 36, 18), (134, 62, 28), (184, 96, 44), (218, 134, 70), (242, 176, 112)]
# substrates: (ramp base colour, foil ramp)
SUBSTRATES = {
    'silicon_carbide': ((58, 68, 84), [(70, 72, 78), (104, 108, 116), (140, 144, 152), (176, 180, 188), (206, 210, 216)]),   # + molybdenum
    'alumina': ((233, 227, 214), [(96, 56, 36), (140, 86, 56), (184, 120, 82), (212, 152, 110), (236, 186, 146)]),         # + cupronickel
    'beryllia': ((244, 239, 228), [(52, 70, 96), (84, 108, 140), (118, 146, 180), (156, 184, 214), (196, 220, 240)]),      # + chromium
}
HOLES = [(6, 6), (10, 6), (6, 10), (9, 10)]


def ramp(pal, t):
    return pal[max(0, min(len(pal) - 1, int(t * len(pal))))]


def ramp_from(base):
    """Five tones of one colour, outline to highlight."""
    return [tuple(int(c * f) for c in base) for f in (0.38, 0.62, 0.84, 1.0, 1.12)]  # the last one clips at 255 below


def clip(rgb):
    return tuple(min(255, c) for c in rgb)


def load(name):
    path = os.path.join(GT_TEX, name + '.png')
    if not os.path.exists(path):
        sys.exit('set GT_SRC to a GregTech-Modern checkout (missing %s)' % path)
    return Image.open(path).convert('RGBA')


def clipped(src):
    """The silhouette with two corners cut off (the tile's cut corners)."""
    out = src.copy()
    px = out.load()
    for x, y in ((5, 2), (6, 2), (7, 2), (4, 3), (5, 3), (4, 4), (8, 14), (9, 14), (10, 14), (11, 13), (12, 13)):
        px[x, y] = (0, 0, 0, 0)
    return out


def brightness(src):
    px = src.load()
    lum = {}
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = px[x, y]
            if a:
                lum[(x, y)] = (0.3 * r + 0.59 * g + 0.11 * b, a)
    return lum


def paint(lum, pal, holes=True):
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    op = out.load()
    lo = min(v for v, _ in lum.values())
    hi = max(v for v, _ in lum.values())
    for (x, y), (v, a) in lum.items():
        op[x, y] = clip(ramp(pal, (v - lo) / (hi - lo) if hi > lo else 0.5)) + (a,)
    if holes:
        for hx, hy in HOLES:
            if (hx, hy) in lum:
                op[hx, hy] = pal[0] + (255,)
    return out


def main():
    os.makedirs(GT_OUT, exist_ok=True)
    os.makedirs(AF9_OUT, exist_ok=True)
    blank = clipped(load('plastic_circuit_board'))
    # the blank board
    paint(brightness(blank), IVORY).save(os.path.join(GT_OUT, 'plastic_circuit_board.png'))
    # the printed board: the same tile, GT's gold traces become copper
    printed = clipped(load('plastic_printed_circuit_board'))
    pp = printed.load()
    out = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    op = out.load()
    base, trace = {}, {}
    for y in range(16):
        for x in range(16):
            r, g, b, a = pp[x, y]
            if not a:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            lum = 0.3 * r + 0.59 * g + 0.11 * b
            (trace if 0.10 < h < 0.20 and s > 0.5 and v > 0.5 else base)[(x, y)] = (lum, a)
    for group, pal in ((base, IVORY), (trace, COPPER)):
        if not group:
            continue
        lo = min(v for v, _ in group.values())
        hi = max(v for v, _ in group.values())
        for (x, y), (v, a) in group.items():
            op[x, y] = clip(ramp(pal, min(0.999, (v - lo) / (hi - lo) if hi > lo else 0.5))) + (a,)
    for hx, hy in HOLES:
        if (hx, hy) in base:
            op[hx, hy] = IVORY[0] + (255,)
    out.save(os.path.join(GT_OUT, 'plastic_printed_circuit_board.png'))
    # the substrates: the blank tile in the ceramic's colour, the foil along the lower right edge
    lum = brightness(blank)
    for name, (colour, foil) in SUBSTRATES.items():
        img = paint(lum, ramp_from(colour))
        px = img.load()
        for (x, y) in lum:
            if x + y >= 19 and px[x, y][3]:
                px[x, y] = clip(ramp(foil, ((x + y) % 3 + 1) / 4)) + (255,)
        img.save(os.path.join(AF9_OUT, name + '_substrate.png'))


if __name__ == '__main__':
    main()
