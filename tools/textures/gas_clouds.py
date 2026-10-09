#!/usr/bin/env python3
"""The cloud blocks of the gas giants and the Sun (Zephyr, Kronos, Helios).

Run from the repository root (needs Pillow):  python3 tools/textures/gas_clouds.py

Writes af9-core/src/main/resources/assets/af9/textures/block/<name>.png (soft, translucent billows that tile) and the
blockstate, block model (translucent) and item model of each.
"""
import json
import math
import os
import random

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
RES = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9')
# name: (light colour, dark colour, alpha)
CLOUDS = {
    'amber_cloud': ((0xF2, 0xB9, 0x6B), (0xC9, 0x7A, 0x3A), 215),
    'rust_cloud': ((0xB5, 0x62, 0x34), (0x7A, 0x34, 0x1E), 235),
    'pale_cloud': ((0xF4, 0xEC, 0xD2), (0xCF, 0xC0, 0x9C), 215),
    'ochre_cloud': ((0xC9, 0xA2, 0x5E), (0x8E, 0x6B, 0x34), 235),
    'plasma_cloud': ((0xFF, 0xF0, 0x9A), (0xFF, 0xA8, 0x2E), 235),
    'core_plasma': ((0xFF, 0xB8, 0x3C), (0xE0, 0x4A, 0x10), 245),
}


def tile_noise(seed, size=16, cell=4):
    rnd = random.Random(seed)
    n = size // cell
    grid = [[rnd.random() for _ in range(n)] for _ in range(n)]

    def smooth(t):
        return t * t * (3 - 2 * t)

    def at(x, y):
        gx, gy = x / cell, y / cell
        x0, y0 = int(gx) % n, int(gy) % n
        x1, y1 = (x0 + 1) % n, (y0 + 1) % n
        fx, fy = smooth(gx - int(gx)), smooth(gy - int(gy))
        a = grid[y0][x0] * (1 - fx) + grid[y0][x1] * fx
        b = grid[y1][x0] * (1 - fx) + grid[y1][x1] * fx
        return a * (1 - fy) + b * fy
    return [[at(x, y) for x in range(size)] for y in range(size)]


def texture(name, light, dark, alpha):
    seed = sum(ord(c) for c in name)
    big, fine = tile_noise(seed, 16, 8), tile_noise(seed + 7, 16, 2)
    img = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            t = min(1.0, max(0.0, big[y][x] * 0.7 + fine[y][x] * 0.3))
            band = 0.5 + 0.5 * math.sin((y + big[y][x] * 5) * math.pi / 4)  # the bands of a gas giant
            t = t * 0.6 + band * 0.4
            c = tuple(int(dark[i] + (light[i] - dark[i]) * t) for i in range(3))
            img.putpixel((x, y), c + (alpha,))
    return img


for name, (light, dark, alpha) in CLOUDS.items():
    texture(name, light, dark, alpha).save(os.path.join(RES, 'textures/block', name + '.png'))
    with open(os.path.join(RES, 'blockstates', name + '.json'), 'w') as f:
        json.dump({'variants': {'': {'model': 'af9:block/' + name}}}, f, indent=2)
    with open(os.path.join(RES, 'models/block', name + '.json'), 'w') as f:
        json.dump({'parent': 'minecraft:block/cube_all', 'render_type': 'minecraft:translucent',
                   'textures': {'all': 'af9:block/' + name}}, f, indent=2)
    with open(os.path.join(RES, 'models/item', name + '.json'), 'w') as f:
        json.dump({'parent': 'af9:block/' + name}, f, indent=2)
