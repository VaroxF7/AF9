#!/usr/bin/env python3
"""Anti-matter plasma: a black containment field with two counter-rotating streams, crimson and amber, that meet in
white pair-annihilation flashes. Deliberately warm so it cannot be mistaken for the blue/magenta Idontknowium plasma.

Run from the repository root (needs Pillow):  python3 tools/textures/antimatter.py

Writes into af9-core/src/main/resources/assets/gtceu:
  textures/block/fluids/fluid.anti_matter_plasma.png (+ .mcmeta): the plasma's texture (32 frames of 16 x 16, tiles and loops).
"""
import json
import math
import os

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
FLUIDS = os.path.join(ROOT, 'af9-core/src/main/resources/assets/gtceu/textures/block/fluids')
FRAMES = 32
TAU = 2 * math.pi

RAMP = [(0.0, (8, 2, 6)), (0.3, (70, 6, 22)), (0.55, (200, 24, 30)), (0.75, (255, 140, 20)), (0.92, (255, 224, 120)), (1.0, (255, 255, 235))]


def ramp(t):
    t = max(0.0, min(1.0, t))
    for (a, ca), (b, cb) in zip(RAMP, RAMP[1:]):
        if t <= b:
            f = (t - a) / (b - a)
            return [ca[i] + (cb[i] - ca[i]) * f for i in range(3)]
    return list(RAMP[-1][1])


def pixel(x, y, p):
    u, v = TAU * x / 16, TAU * y / 16
    # matter stream drifts one way, anti-matter stream the other; both wrap at 16 px
    a = math.sin(2 * u + v + 2 * p) + 0.6 * math.sin(v - 3 * u + p)
    b = math.sin(2 * u - v - 2 * p) + 0.6 * math.sin(v + 3 * u - p)
    sa, sb = max(0.0, a) / 1.6, max(0.0, b) / 1.6
    t = 0.12 + 0.45 * max(sa, sb) + 0.3 * sa * sb * 4
    # annihilation: both streams peak at once -> white flash
    meet = sa * sb
    if meet > 0.32:
        t = 0.9 + min(0.1, (meet - 0.32) * 0.6)
    return ramp(t)


def main():
    os.makedirs(FLUIDS, exist_ok=True)
    sheet = Image.new('RGBA', (16, 16 * FRAMES), (0, 0, 0, 255))
    px = sheet.load()
    for f in range(FRAMES):
        p = TAU * f / FRAMES
        for y in range(16):
            for x in range(16):
                px[x, 16 * f + y] = tuple(max(0, min(255, int(c))) for c in pixel(x, y, p)) + (255,)
    path = os.path.join(FLUIDS, 'fluid.anti_matter_plasma.png')
    sheet.save(path)
    with open(path + '.mcmeta', 'w', newline='\n') as out:
        json.dump({'animation': {'frametime': 2, 'interpolate': True}}, out, indent=2)
        out.write('\n')


if __name__ == '__main__':
    main()
