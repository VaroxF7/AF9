#!/usr/bin/env python3
"""Texture of Lapis Butene-II-Sulfonate: a slow, deep lapis-blue liquid with fine gold pyrite flecks that glint and drift,
like the specks in the stone. Built on litho_fluids.py's viscous wave.

Run from the repository root (needs Pillow):  python3 tools/textures/lapis_sulfonate.py

Writes fluid.lapis_butene_ii_sulfonate.png (+ .mcmeta): 32 frames of 16 x 16, tiles and loops. AF9Materials gives the
material customStill().disableColor(), so the texture is shown as it is.
"""
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import litho_fluids as lf

NAME = 'lapis_butene_ii_sulfonate'
LIGHT, DARK = 0x3d63c4, 0x14246e
GOLD = (236, 196, 92)
FLECKS = 7


def frame(f):
    img = lf.frame(NAME, 'viscous', LIGHT, DARK, f)
    px = img.load()
    for i in range(FLECKS):
        x = int(lf.hash2(i, 11, 3) * 16)
        y = int(lf.hash2(i, 12, 3) * 16)
        y = (y + f // 2) % 16  # sinks one tile per loop
        k = max(0.0, math.sin(lf.TAU * f / lf.FRAMES + lf.hash2(i, 13, 3) * 6.28)) ** 3  # glint
        base = px[x, y][:3]
        mix = 0.15 + 0.85 * k
        px[x, y] = tuple(int(base[c] + (GOLD[c] - base[c]) * mix) for c in range(3)) + (255,)
    return img


def main():
    out = lf.Image.new('RGBA', (16, 16 * lf.FRAMES))
    for f in range(lf.FRAMES):
        out.paste(frame(f), (0, 16 * f))
    path = os.path.join(lf.FLUIDS, 'fluid.%s.png' % NAME)
    out.save(path)
    with open(path + '.mcmeta', 'w', newline='\n') as meta:
        json.dump({'animation': {'frametime': 3, 'interpolate': True}}, meta, indent=2)
        meta.write('\n')
    print(path)


if __name__ == '__main__':
    main()
