#!/usr/bin/env python3
"""Draws the animated nano particles: a small swarm of nanites orbiting a dark seed. Every grain runs on its own tilted ellipse
(whole turns per loop, so the strip loops without a seam), is bright cyan in front of the seed and dim blue-grey behind it, and now
and then a grain flares white. Original work, not taken from any other pack.
Run from the repository root (needs Pillow):  python3 tools/textures/nano_particles.py"""
import json
import math
import os

from PIL import Image

TEX = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../af9-core/src/main/resources/assets/af9/textures/item')
FRAMES = 16
FRAMETIME = 2

K = (14, 18, 24, 255)
D = (34, 42, 54, 255)
M = (62, 74, 90, 255)
FRONT = [(60, 220, 240, 255), (130, 240, 252, 255)]
BACK = [(36, 84, 112, 255), (56, 110, 140, 255)]
FLARE = (236, 252, 255, 255)
VIOLET = (150, 110, 230, 255)

# (orbit radius x, radius y, tilt in radians, start phase in turns, turns per loop (signed), colour index)
GRAINS = [
    (6.2, 2.4, 0.5, 0.00, 1, 0), (6.0, 2.2, 0.5, 0.50, 1, 1), (5.0, 1.8, 0.5, 0.25, 1, 0),
    (6.4, 3.0, -0.7, 0.10, -1, 1), (5.4, 2.6, -0.7, 0.60, -1, 0), (4.4, 2.0, -0.7, 0.85, -1, 1),
    (3.4, 1.4, 1.3, 0.30, 2, 0), (3.2, 1.2, 1.3, 0.80, 2, 1), (7.0, 3.4, 0.0, 0.45, 1, 0),
    (2.6, 1.0, -0.2, 0.05, 2, 1), (5.8, 1.4, 1.0, 0.70, -1, 0), (4.0, 3.2, 0.2, 0.95, 1, 1),
]


def frame(f):
    im = Image.new('RGBA', (16, 16), (0, 0, 0, 0))
    cx, cy = 7.5, 7.5
    dots = []
    for i, (rx, ry, tilt, ph, turns, ci) in enumerate(GRAINS):
        a = 2 * math.pi * (ph + turns * f / FRAMES)
        x, y = rx * math.cos(a), ry * math.sin(a)
        px = cx + x * math.cos(tilt) - y * math.sin(tilt)
        py = cy + x * math.sin(tilt) + y * math.cos(tilt)
        front = math.sin(a) > 0                      # the near half of the orbit passes in front of the seed
        dots.append((front, int(round(px)), int(round(py)), ci, i))
    # seed: a small dark core with a pulse in its centre
    for (x, y, c) in ((7, 6, M), (8, 6, D), (6, 7, D), (7, 7, M), (8, 7, M), (9, 7, D), (6, 8, D), (7, 8, D), (8, 8, M), (9, 8, D),
                      (7, 9, D), (8, 9, D)):
        im.putpixel((x, y), c)
    pulse = [(60, 220, 240, 255), (130, 240, 252, 255), FLARE, (130, 240, 252, 255)][(f // 2) % 4]
    im.putpixel((7, 7), pulse)
    im.putpixel((8, 8), (24, 120, 150, 255))
    for front in (False, True):                      # back grains first, front grains over the seed
        for fr, x, y, ci, i in dots:
            if fr != front or not (0 <= x < 16 and 0 <= y < 16):
                continue
            c = FRONT[ci] if fr else BACK[ci]
            if (f + i * 5) % FRAMES == 0:            # a grain flares
                c = FLARE
            elif i % 5 == 3 and fr:
                c = VIOLET
            im.putpixel((x, y), c)
    return im


def main():
    strip = Image.new('RGBA', (16, 16 * FRAMES), (0, 0, 0, 0))
    for f in range(FRAMES):
        strip.paste(frame(f), (0, f * 16))
    path = os.path.join(TEX, 'nano_particles.png')
    strip.save(path)
    with open(path + '.mcmeta', 'w') as fh:
        json.dump({'animation': {'frametime': FRAMETIME}}, fh)
    print('wrote nano_particles')
    return strip


if __name__ == '__main__':
    main()
