#!/usr/bin/env python3
"""The Planetary Satellite's three Neutron Ray Reflectors (GTCEu's Iridium Neutron Reflector, recoloured).

Run from the repository root (needs Pillow):  python3 tools/textures/ray_reflectors.py

The plate is GTCEu's `item/neutron_reflector.png` (GregTech CEu Modern, LGPL-3.0; the copy is in gt_src/): a grey
beveled square. Each tier keeps the shape and takes its own ramp of greys - cold silver, blue steel, violet - and
its own glow sweeping over it (a 16-frame vertical strip plus an .mcmeta, the format of satellite_casing.py): a
band crossing left to right for iridium, a diagonal one for osmiridium, and a top-to-bottom one that breathes for
unobtainium-vibranium.
"""
import json
import math
import os
import sys

from PIL import Image

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
SRC = os.path.join(ROOT, 'tools/textures/gt_src/neutron_reflector.png')
OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/item')

FRAMES = 16

# id, ramp stops (t, dark -> mid -> bright), sweep (x = across, xy = diagonal, y = down + breathing), frametime
TIERS = [
    ('iridium_ray_reflector',
     [(0.0, (18, 20, 26)), (0.5, (126, 136, 156)), (1.0, (240, 246, 255))], 'x', 4),
    ('osmiridium_ray_reflector',
     [(0.0, (6, 16, 32)), (0.5, (40, 92, 152)), (1.0, (190, 230, 255))], 'xy', 3),
    ('unobtainium_vibranium_ray_reflector',
     [(0.0, (18, 8, 36)), (0.5, (124, 60, 200)), (1.0, (236, 210, 255))], 'y', 3),
]


def ramp(stops):
    """A function over 0..1 that walks the stops (same shape as satellite_casing.py's)."""
    def at(t):
        t = max(0.0, min(1.0, t))
        for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
            if t <= t1:
                f = (t - t0) / (t1 - t0)
                return tuple(round(a + (b - a) * f) for a, b in zip(c0, c1))
        return stops[-1]
    return at


def clamp(v):
    return max(0, min(255, round(v)))


def base_plate(src, at):
    """The plate through the tier's ramp: the source is grey, so one luminance selects every colour."""
    out = Image.new('RGBA', src.size)
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            if a == 0:
                continue
            t = (0.3 * r + 0.59 * g + 0.11 * b) / 255.0
            out.putpixel((x, y), at(t) + (a,))
    return out


def animate(plate, sweep):
    """The plate as a vertical strip of FRAMES frames: a glow band crosses it, one way per tier."""
    strip = Image.new('RGBA', (16, 16 * FRAMES))
    w, h = plate.size
    for f in range(FRAMES):
        t = f / FRAMES
        travel = t * (w + h + 4) - 2        # the band runs off one side and enters at the other
        breath = 1.0 + 0.20 * math.sin(2 * math.pi * t) if sweep == 'y' else 1.0
        for y in range(h):
            for x in range(w):
                r, g, b, a = plate.getpixel((x, y))
                if a == 0:
                    continue
                if sweep == 'x':
                    d = abs(x - travel)
                elif sweep == 'xy':
                    d = abs((x + y) - travel)
                else:
                    d = abs(y - travel)
                glow = max(0.0, 1.0 - d / 3.0) * breath
                k = 1.0 + 1.15 * glow
                strip.putpixel((x, f * 16 + y), (clamp(r * k), clamp(g * k), clamp(b * k), a))
    return strip


def main():
    src = Image.open(SRC).convert('RGBA')
    for name, stops, sweep, frametime in TIERS:
        path = os.path.join(OUT, name + '.png')
        animate(base_plate(src, ramp(stops)), sweep).save(path)
        with open(path + '.mcmeta', 'w', newline='\n') as f:
            json.dump({'animation': {'frametime': frametime, 'interpolate': True}}, f, indent=2)
            f.write('\n')
        print('wrote', path)


if __name__ == '__main__':
    main()
