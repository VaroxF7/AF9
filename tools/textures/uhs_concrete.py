#!/usr/bin/env python3
"""Ultra High Strength Concrete Floor (the Space Elevator's and the Dyson Swarm's): a poured slab, 16 x 16.

Run from the repository root (needs Pillow):  python3 tools/textures/uhs_concrete.py [--preview <file>]

Drawn here, not copied: fine-grained cool-grey concrete (three octaves of wrapping value noise, so it tiles), light and dark
aggregate flecks, a few pores and a hairline crack, a saw-cut control joint along the top and left edges (a dark line with a
lit lip below it, so a floor of blocks reads as a grid of slabs) and four form-tie dimples in the corners of each slab.
"""
import os
import random
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/block')
NAME = 'ultra_high_strength_concrete_floor'
N = 16


def smooth(t):
    return t * t * (3 - 2 * t)


def noise(rng, cells):
    """Value noise on a wrapping lattice of cells x cells, sampled at every pixel of the 16 x 16 face."""
    lattice = [[rng.random() for _ in range(cells)] for _ in range(cells)]
    out = [[0.0] * N for _ in range(N)]
    for y in range(N):
        for x in range(N):
            fx, fy = x * cells / N, y * cells / N
            x0, y0 = int(fx), int(fy)
            tx, ty = smooth(fx - x0), smooth(fy - y0)
            a, b = lattice[y0 % cells][x0 % cells], lattice[y0 % cells][(x0 + 1) % cells]
            c, d = lattice[(y0 + 1) % cells][x0 % cells], lattice[(y0 + 1) % cells][(x0 + 1) % cells]
            out[y][x] = (a * (1 - tx) + b * tx) * (1 - ty) + (c * (1 - tx) + d * tx) * ty
    return out


def shade(v):
    """A cool concrete grey, v in 0..1 (0.5 is the base)."""
    base = 124 + (v - 0.5) * 70
    return (round(base - 3), round(base), round(base + 4), 255)


def make():
    rng = random.Random(9)
    n1, n2, n3 = noise(rng, 2), noise(rng, 4), noise(rng, 8)
    v = [[(n1[y][x] * 0.35 + n2[y][x] * 0.35 + n3[y][x] * 0.3) for x in range(N)] for y in range(N)]
    im = Image.new('RGBA', (N, N))
    for y in range(N):
        for x in range(N):
            jitter = (rng.random() - 0.5) * 0.08                 # the fine grain
            im.putpixel((x, y), shade(v[y][x] + jitter))
    for _ in range(14):                                          # aggregate: small light and dark flecks
        x, y = rng.randrange(N), rng.randrange(N)
        im.putpixel((x, y), shade(rng.choice((0.25, 0.3, 0.78, 0.85))))
    for _ in range(4):                                           # pores
        x, y = rng.randrange(N), rng.randrange(N)
        im.putpixel((x, y), (74, 77, 82, 255))
    for x, y in ((9, 6), (10, 7), (10, 8), (11, 9), (11, 10)):   # a hairline crack
        im.putpixel((x, y), (82, 85, 90, 255))
    for i in range(N):                                           # the control joint and its lit lip
        im.putpixel((i, 0), (58, 61, 66, 255))
        im.putpixel((0, i), (58, 61, 66, 255))
        if i > 0:
            im.putpixel((i, 1), shade(0.88))
            im.putpixel((1, i), shade(0.88))
    for cx, cy in ((4, 4), (11, 4), (4, 11), (11, 11)):          # form-tie dimples
        im.putpixel((cx, cy), (66, 69, 74, 255))
        im.putpixel((cx + 1, cy + 1), shade(0.82))
    return im


def main():
    im = make()
    im.save(os.path.join(OUT, NAME + '.png'))
    if '--preview' in sys.argv:
        big = Image.new('RGBA', (N * 4, N * 4))
        for bx in range(4):
            for by in range(4):
                big.paste(im, (bx * N, by * N))
        big.resize((N * 4 * 6, N * 4 * 6), Image.NEAREST).save(sys.argv[sys.argv.index('--preview') + 1])


if __name__ == '__main__':
    main()
