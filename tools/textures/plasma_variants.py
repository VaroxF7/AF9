#!/usr/bin/env python3
"""Candidate textures for the Idontknowium plasma (16 x 16, 32 looping frames, tileable).
Run from the repository root (needs Pillow):  python3 tools/textures/plasma_variants.py OUT_DIR
Writes OUT_DIR/<variant>.png (+ .mcmeta) and a preview sheet."""
import json
import math
import os
import sys

from PIL import Image

FRAMES = 32
TAU = 2 * math.pi


def lerp_ramp(stops, t):
    t = max(0.0, min(1.0, t))
    for (a, ca), (b, cb) in zip(stops, stops[1:]):
        if t <= b:
            f = (t - a) / (b - a)
            return [ca[i] + (cb[i] - ca[i]) * f for i in range(3)]
    return list(stops[-1][1])


def field(x, y, p, waves):
    u, v = TAU * x / 16, TAU * y / 16
    warp = 0.9 * math.sin(v + p) + 0.9 * math.sin(u - p)
    total = sum(math.sin(kx * (u + 0.5 * warp) + ky * (v - 0.5 * warp) + s * p + i) for i, (kx, ky, s) in enumerate(waves))
    return total / len(waves)


W = [(1, 0, 1), (0, 1, -1), (1, 1, 2), (2, -1, -2), (1, -2, 1), (3, 1, 1), (-2, 3, -1)]

ION = [(0, (4, 6, 30)), (0.4, (14, 50, 140)), (0.7, (30, 150, 255)), (0.9, (170, 230, 255)), (1, (255, 255, 255))]
ICE = [(0, (2, 14, 30)), (0.45, (10, 80, 120)), (0.75, (60, 210, 230)), (1, (235, 255, 255))]
VOID = [(0, (2, 8, 10)), (0.5, (6, 70, 70)), (0.8, (40, 220, 180)), (1, (220, 255, 240))]
STEEL = [(0, (10, 14, 24)), (0.5, (30, 50, 90)), (0.8, (70, 140, 220)), (1, (210, 235, 255))]


def ion_storm(x, y, p):
    """deep blue plasma with white-blue lightning filaments, no pink"""
    f = field(x, y, p, W)
    col = lerp_ramp(ION, max(0.0, 0.5 + 0.5 * f * 1.9) ** 1.3)
    r = abs(math.sin(2.5 * f * len(W) / 3 + p))
    if r > 0.96:
        k = (r - 0.96) / 0.04
        col = [150 + 105 * k, 215 + 40 * k, 255]
    return col


def cold_core(x, y, p):
    """cyan/white cells: worley-style plasma cells with bright borders, like a reactor core"""
    pts = []
    for i in range(3):
        for j in range(3):
            pts.append((i * 16 / 3 + 1.6 * math.sin(p + i * 1.7 + j), j * 16 / 3 + 1.6 * math.cos(p + j * 2.1 - i)))
    best = 9e9
    second = 9e9
    for px, py in pts:
        for ox in (-16, 0, 16):
            for oy in (-16, 0, 16):
                d = math.hypot(x - px - ox, y - py - oy)
                if d < best:
                    best, second = d, best
                elif d < second:
                    second = d
    edge = second - best
    t = 0.25 + 0.35 * math.exp(-best * 0.3) + (0.5 if edge < 1.0 else 0) * (1 - edge / 1.0 if edge < 1.0 else 0)
    return lerp_ramp(ICE, t)


def void_teal(x, y, p):
    """near-black with teal-green energy veins and white-hot nodes"""
    f = field(x, y, p, W)
    vein = math.exp(-abs(math.sin(f * 6.0 + p)) * 5)
    base = 0.12 + 0.25 * (0.5 + 0.5 * f)
    return lerp_ramp(VOID, base + 0.75 * vein)


def scan_steel(x, y, p):
    """industrial: steel-blue plasma crossed by scrolling scanlines and a bright coolant stream"""
    f = field(x, y, p, W)
    t = 0.3 + 0.35 * (0.5 + 0.5 * f)
    band = 0.5 + 0.5 * math.sin(TAU * (y / 4.0 - p / TAU * 1.0))
    t += 0.22 * band
    col = lerp_ramp(STEEL, t)
    if int(y + p / TAU * 16 * 2) % 8 == 0:
        col = [min(255, c + 45) for c in col]
    return col


VARIANTS = {'a_ion_storm': ion_storm, 'b_cold_core': cold_core, 'c_void_teal': void_teal, 'd_scan_steel': scan_steel}


def render(fn):
    sheet = Image.new('RGBA', (16, 16 * FRAMES), (0, 0, 0, 255))
    for f in range(FRAMES):
        p = TAU * f / FRAMES
        px = Image.new('RGBA', (16, 16), (0, 0, 0, 255))
        pp = px.load()
        for y in range(16):
            for x in range(16):
                c = fn(x, y, p)
                pp[x, y] = tuple(max(0, min(255, int(v))) for v in c) + (255,)
        sheet.paste(px, (0, 16 * f))
    return sheet


def main(out):
    os.makedirs(out, exist_ok=True)
    cell = 3 * 16 * 4
    prev = Image.new('RGB', (cell * len(VARIANTS) + 10 * (len(VARIANTS) + 1), cell * 2 + 30), (20, 20, 24))
    for n, (name, fn) in enumerate(VARIANTS.items()):
        s = render(fn)
        s.save(os.path.join(out, name + '.png'))
        with open(os.path.join(out, name + '.png.mcmeta'), 'w') as fh:
            json.dump({'animation': {'frametime': 2, 'interpolate': True}}, fh, indent=2)
        for row, fr in enumerate((0, 11)):
            tile = s.crop((0, 16 * fr, 16, 16 * fr + 16))
            big = Image.new('RGB', (48, 48))
            for i in range(3):
                for j in range(3):
                    big.paste(tile.convert('RGB'), (16 * i, 16 * j))
            prev.paste(big.resize((cell, cell), Image.NEAREST), (10 + n * (cell + 10), 10 + row * (cell + 10)))
    prev.save(os.path.join(out, 'preview.png'))


if __name__ == '__main__':
    main(sys.argv[1])
