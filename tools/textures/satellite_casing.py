#!/usr/bin/env python3
"""The Planetary Satellite's casing and controller face (GTNH's Planetary Gas Siphon, GT5-Unofficial, LGPL-3.0).

Run from the repository root (needs Pillow):  python3 tools/textures/satellite_casing.py

The siphon's casing (gtnhintergalactic SiphonCasing.png) is recoloured through a gradient in the colours of
Enriched Naquadah Trinium Europium Duranide (GT: 0xc6b083 with 0x45063d): its blacks stay black, its purple plate
goes through the material's purple to its gold, and it joins its neighbours (LDLib's .mcmeta + `_ctm` sheet, the format of
space_elevator.py). The controller face is GTNH's own; while the machine works its orb breathes, a glint sweeps over it
and the marker blinks (an animated strip, 16 frames).
"""
import json
import math
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from space_elevator import ctm_sheet

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '../..')
GT5 = os.environ.get('GT5_SRC', '/tmp/GT5-Unofficial')
CASING_OUT = os.path.join(ROOT, 'af9-core/src/main/resources/assets/af9/textures/block/satellite_casing.png')
FACE_OUT = os.path.join(ROOT, 'kubejs/assets/gtceu/textures/block/multiblock/planetary_satellite')

STOPS = [(0.0, (6, 5, 10)), (0.30, (0x45, 0x06, 0x3D)), (1.0, (0xC6, 0xB0, 0x83))]


def ramp(t):
    t = max(0.0, min(1.0, t))
    for (t0, c0), (t1, c1) in zip(STOPS, STOPS[1:]):
        if t <= t1:
            f = (t - t0) / (t1 - t0)
            return tuple(round(a + (b - a) * f) for a, b in zip(c0, c1)) + (255,)
    return STOPS[-1][1] + (255,)


FRAMES = 16


def clamp(v):
    return max(0, min(255, round(v)))


def animate(face):
    """The face as a vertical strip of FRAMES frames: the orb pulses, a diagonal glint sweeps over it, red blinks."""
    strip = Image.new('RGBA', (16, 16 * FRAMES))
    for f in range(FRAMES):
        t = f / FRAMES
        pulse = 1.0 + 0.22 * math.sin(2 * math.pi * t)
        for y in range(16):
            for x in range(16):
                r, g, b, a = face.getpixel((x, y))
                if a == 0:
                    continue
                if (r > 1.6 * g and r > 1.6 * b) or (g > 1.6 * r and g > 1.6 * b):   # the marker (green while working) blinks
                    k = 0.55 + 0.75 * (1 if f % 8 < 4 else 0.35)
                    r, g, b = r * k, g * k, b * k
                else:
                    glint = max(0.0, 1 - abs((x + y) - (t * 40 - 8)) / 3.0)   # a diagonal band, left to right
                    k = pulse + 0.55 * glint
                    r, g, b = r * k, g * k, b * k
                strip.putpixel((x, f * 16 + y), (clamp(r), clamp(g), clamp(b), a))
    return strip


def main():
    src = Image.open(os.path.join(GT5, 'src/main/resources/assets/gtnhintergalactic/textures/blocks/gasSiphon/SiphonCasing.png')).convert('RGBA')
    out = Image.new('RGBA', src.size)
    for y in range(src.height):
        for x in range(src.width):
            r, g, b, a = src.getpixel((x, y))
            out.putpixel((x, y), ramp((0.3 * r + 0.59 * g + 0.11 * b) / 80.0))
    out.save(CASING_OUT)
    # connected textures: the sheet (GT's CTM reads `<name>_ctm.png`) and the .mcmeta that ties it to the sprite
    ctm_sheet(out, 3, 3).save(CASING_OUT.replace('.png', '_ctm.png'))
    with open(CASING_OUT + '.mcmeta', 'w', newline='\n') as f:
        f.write('{\n    "ldlib": {\n        "connection": "af9:block/satellite_casing_ctm"\n    }\n}\n')

    faces = os.path.join(GT5, 'src/main/resources/assets/gregtech/textures/blocks/iconsets')
    os.makedirs(FACE_OUT, exist_ok=True)
    for dst, name in (('overlay_front', ''), ('overlay_front_paused', ''), ('overlay_front_active', ''),
                      ('overlay_front_emissive', '_GLOW'), ('overlay_front_paused_emissive', '_GLOW'),
                      ('overlay_front_active_emissive', '_ACTIVE_GLOW')):
        face = Image.open(os.path.join(faces, 'OVERLAY_FRONT_PLANETARYSIPHON%s.png' % name)).convert('RGBA')
        path = os.path.join(FACE_OUT, dst + '.png')
        if 'active' in dst:
            animate(face).save(path)
            with open(path + '.mcmeta', 'w', newline='\n') as f:
                json.dump({'animation': {'frametime': 3, 'interpolate': True}}, f, indent=2)
                f.write('\n')
        else:
            face.save(path)


if __name__ == '__main__':
    main()
