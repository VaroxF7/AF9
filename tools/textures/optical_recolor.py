#!/usr/bin/env python3
"""Recolours the two GTNH-derived optical parts (Optical CPU, Optical RAM) from amber to the dark-prism-glass look of the
Optically Reflective Wafer family: amber body -> deep violet-blue glass, green rim -> indigo, yellow sparks -> white-cyan.
Reads the originals from tools/textures/optical_src_gtnh (copied there on first run); keeps every animation frame.

    python3 tools/textures/optical_recolor.py     (from the repository root; needs Pillow)
"""
import colorsys
import os
import shutil

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
ITEM = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/item')
SRC = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'optical_src_gtnh')
NAMES = ['optical_cpu', 'optical_ram']
GLASS = [(6, 5, 16), (24, 22, 48), (44, 44, 92), (70, 82, 150), (112, 140, 214), (190, 222, 255)]


def ramp(v):
    v = max(0.0, min(1.0, v)) * (len(GLASS) - 1)
    i = min(int(v), len(GLASS) - 2)
    t = v - i
    return tuple(int(GLASS[i][k] + (GLASS[i + 1][k] - GLASS[i][k]) * t) for k in range(3))


def recolor(p):
    r, g, b, a = p
    if not a:
        return p
    h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
    if s < 0.18:                                  # greys: the casing, kept but cooled and darkened a touch
        c = ramp(v * 0.95)
        return c + (a,)
    if 0.17 < h < 0.45 and s > 0.5 and v < 0.7:   # the green rim -> deep indigo
        return ramp(0.12 + v * 0.5) + (a,)
    if 0.12 <= h < 0.2 and v > 0.78:               # yellow sparks and pins -> white-cyan
        return ramp(0.88 + 0.12 * (v - 0.78) / 0.22) + (a,)
    # amber body: its brightness picks the glass tone (a little contrast stretch)
    return ramp(min(1.0, (v ** 1.15) * 0.95)) + (a,)


def main():
    if not os.path.isdir(SRC):
        os.makedirs(SRC)
        for n in NAMES:
            shutil.copy(os.path.join(ITEM, n + '.png'), os.path.join(SRC, n + '.png'))
    for n in NAMES:
        im = Image.open(os.path.join(SRC, n + '.png')).convert('RGBA')
        out = Image.new('RGBA', im.size)
        for y in range(im.height):
            for x in range(im.width):
                out.putpixel((x, y), recolor(im.getpixel((x, y))))
        out.save(os.path.join(ITEM, n + '.png'))
        print('recoloured', n)


main()
